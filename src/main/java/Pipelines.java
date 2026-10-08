import java.io.*;
import java.util.*;

public class Pipelines {

    // 这里只负责快速判断命令中是否出现管道符；具体拆分由 parse() 完成。
    public boolean isPipeline(String command) {
        return command.contains("|");
    }

    // 解析整条管道，并根据是否包含 builtin 选择不同的执行策略。
    public void execute(
            String command,
            Map<String, CommandHandler> commands,
            PrintStream outputStream,
            PrintStream errorStream
    ) throws IOException, InterruptedException {

        // 每个内部 List 表示一个 stage，例如：
        // echo hello | cat | wc -c -> [[echo, hello], [cat], [wc, -c]]
        List<List<String>> pipeline = parse(command);

        // 管道任意一段为空都属于无效输入，例如：echo hello | | cat。
        if (pipeline.stream().anyMatch(List::isEmpty)) {
            errorStream.println("Invalid pipeline");
            return;
        }

        // 纯 external pipeline 可以直接交给 ProcessBuilder.startPipeline()，
        // 让操作系统负责连接各个子进程的 stdin 和 stdout。
        boolean allExternal = pipeline.stream()
                .noneMatch(cmd -> commands.containsKey(cmd.getFirst()));

        if (allExternal) {
            executeExternalPipeline(pipeline, outputStream);
        } else {
            executeMixedPipeline(pipeline, commands, outputStream, errorStream);
        }
    }

    // 执行 External | External | External 这一类纯外部命令管道。
    private void executeExternalPipeline(
            List<List<String>> pipeline,
            PrintStream outputStream
    ) throws IOException, InterruptedException {

        List<ProcessBuilder> builders = new ArrayList<>();

        // 每一个 pipeline stage 对应一个 ProcessBuilder。
        for (List<String> command : pipeline) {
            ProcessBuilder pb = new ProcessBuilder(command);
            // stderr 不进入 pipeline，直接显示在当前终端。
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            builders.add(pb);
        }

        // startPipeline() 会自动把前一个进程的 stdout 连接到后一个进程的 stdin。
        List<Process> processes = ProcessBuilder.startPipeline(builders);

        try {
            // First command receives EOF instead of terminal input.
            processes.getFirst().getOutputStream().close();

            Process last = processes.getLast();

            // Stream output immediately.
            // 持续读取最后一个进程的 stdout，避免输出塞满系统 pipe buffer。
            try (InputStream stdout = last.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = stdout.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }
            }

            // Wait for the last command only.
            // 最后一段结束后即可进入 finally，清理可能仍在运行的上游进程。
            last.waitFor();

        } finally {
            // Stop upstream processes that may still be running.
            // 这也负责处理 yes | head -n 5 这类下游提前结束的情况。
            for (Process process : processes) {
                if (process.isAlive()) {
                    process.destroy();
                }
            }
        }
    }

    // 执行包含 builtin 的混合管道，例如 Builtin | External | Builtin。
    private void executeMixedPipeline(
            List<List<String>> pipeline,
            Map<String, CommandHandler> commands,
            PrintStream outputStream,
            PrintStream errorStream
    ) throws IOException, InterruptedException {

        // mixed pipeline 当前采用逐段缓冲：上一段完整输出成为下一段完整输入。
        byte[] previousOutput = new byte[0];

        for (int i = 0; i < pipeline.size(); i++) {

            List<String> command = pipeline.get(i);
            boolean isLast = i == pipeline.size() - 1;

            // 当前 stage 从上一段已经收集完成的 byte[] 中读取 stdin。
            InputStream stdin = new ByteArrayInputStream(previousOutput);
            // 非最后一段的 stdout 暂存在内存中，稍后交给下一段。
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            // 最后一段直接输出到 Shell；中间段则写入 buffer。
            PrintStream stdout = isLast
                    ? outputStream
                    : new PrintStream(buffer);

            try {
                // command 的第一个 token 能在 commands 中找到时，它就是 builtin。
                if (commands.containsKey(command.getFirst())) {

                    executeBuiltin(
                            command,
                            stdin,
                            stdout,
                            errorStream,
                            commands
                    );

                } else {

                    executeExternal(
                            command,
                            stdin,
                            stdout
                    );
                }

                stdout.flush();

                // 保存本段结果，作为下一个 stage 的 stdin。
                if (!isLast) {
                    previousOutput = buffer.toByteArray();
                }

            } finally {
                // 只能关闭内部创建的 PrintStream，不能关闭调用方传入的 outputStream。
                if (!isLast) {
                    stdout.close();
                }
            }
        }
    }

    private void executeBuiltin(
            List<String> command,
            InputStream inputStream,
            PrintStream outputStream,
            PrintStream errorStream,
            Map<String, CommandHandler> commands
    ) {

        // 根据命令名取得 Shell 注册的 builtin handler。
        CommandHandler handler = commands.get(command.getFirst());

        if (handler == null) {
            return;
        }

        // 第一个 token 是命令名，其余 token 重新组合成 builtin 参数字符串。
        String arguments = String.join(
                " ",
                command.subList(1, command.size())
        );

        handler.execute(
                arguments,
                inputStream,
                outputStream,
                errorStream
        );
    }

    private void executeExternal(
            List<String> command,
            InputStream inputStream,
            OutputStream outputStream
    ) throws IOException, InterruptedException {

        // mixed pipeline 中的单个 external stage 不能使用 inheritIO，
        // 因为它的 stdin/stdout 需要连接前后两个 stage 的内存流。
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = pb.start();

        // 单独使用 writer thread 向子进程 stdin 写数据；主线程同时读取 stdout，
        // 避免任意一侧的系统 pipe buffer 写满后双方互相等待。
        Thread writer = new Thread(() -> {
            try (OutputStream stdin = process.getOutputStream()) {
                inputStream.transferTo(stdin);
            } catch (IOException ignored) {
                // Process may exit before reading all input.
            }
        });

        writer.start();

        // 当前线程持续排空子进程 stdout，并写入下一段使用的 outputStream。
        try (InputStream stdout = process.getInputStream()) {
            stdout.transferTo(outputStream);
            outputStream.flush();
        }

        // 子进程结束后，再确认负责 stdin 的 writer thread 已经退出。
        process.waitFor();
        writer.join();
    }

    // 把原始命令拆成多个 stage，并复用 Quoting parser 处理每段的参数。
    private List<List<String>> parse(String command) {

        List<List<String>> pipeline = new ArrayList<>();

        // 当前正则要求 | 两侧至少各有一个空白字符。
        String[] parts = command.split("\\s+\\|\\s+", -1);

        for (String part : parts) {
            // 每个 stage 使用独立的 Redirection 解析上下文。
            pipeline.add(
                    Quoting.parse(part, new Redirection())
            );
        }

        return pipeline;
    }
}

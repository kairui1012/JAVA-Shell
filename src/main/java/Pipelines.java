import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Pipelines {

    public boolean isPipeline(String command) {
        return command.contains("|");
    }

    public void execute(
            String command,
            Map<String, CommandHandler> commands,
            InputStream inputStream,
            PrintStream outputStream,
            PrintStream errorStream
    ) throws IOException, InterruptedException {

        List<List<String>> pipelineCommands = parse(command);

        if (pipelineCommands.stream().anyMatch(List::isEmpty)) {
            errorStream.println("Invalid pipeline");
            return;
        }

        List<Thread> threads = new ArrayList<>();

        InputStream currentInput = inputStream;

        for (int i = 0; i < pipelineCommands.size(); i++) {

            List<String> currentCommand = pipelineCommands.get(i);

            boolean isFirst = i == 0;
            boolean isLast = i == pipelineCommands.size() - 1;

            // 当前命令的输出
            OutputStream currentOutput;

            // 下一条命令的输入
            InputStream nextInput = null;

            if (isLast) {
                currentOutput = outputStream;
            } else {
                PipedInputStream pipeInput = new PipedInputStream(8192);

                currentOutput = new PipedOutputStream(pipeInput);
                nextInput = pipeInput;
            }

            // 这里创建 Thread 执行当前 Command
            InputStream finalInput = isFirst
                    ? InputStream.nullInputStream()
                    : currentInput;
            OutputStream finalOutput = currentOutput;

            Thread commandThread = new Thread(() -> {

                boolean isBuiltin =
                        commands.containsKey(currentCommand.getFirst());

                try {
                    if (isBuiltin) {

                        PrintStream builtinOutput =
                                new PrintStream(finalOutput, true);

                        executeBuiltin(
                                currentCommand,
                                finalInput,
                                builtinOutput,
                                errorStream,
                                commands
                        );

                        builtinOutput.flush();
                        builtinOutput.checkError();

                    } else {

                        executeExternal(
                                currentCommand,
                                finalInput,
                                finalOutput
                        );
                    }

                } catch (IOException e) {
                    errorStream.println(e.getMessage());

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();

                } finally {

                    // 当前命令结束后，不再需要它消费的输入管道。
                    // 关闭读端可以解除上游阻塞在 pipe buffer 上的写操作。
                    if (!isFirst) {
                        closeQuietly(finalInput);
                    }

                    // 不是最后一个命令才关闭输出管道
                    if (!isLast) {
                        closeQuietly(finalOutput);
                    }
                }
            });

            threads.add(commandThread);
            commandThread.start();

            // 当前命令的 stdout → 下一条命令的 stdin
            currentInput = nextInput;
        }

        // 所有 Command 启动后，才统一等待
        for (Thread thread : threads) {
            thread.join();
        }
    }


    private List<List<String>> parse(String command) {
        List<List<String>> pipelineCommands = new ArrayList<>();
        List<String> currentCommand = new ArrayList<>();

        for (String part : command.trim().split("\\s+")) {
            if (part.equals("|")) {
                pipelineCommands.add(currentCommand);
                currentCommand = new ArrayList<>();
            } else {
                currentCommand.add(part);
            }
        }

        pipelineCommands.add(currentCommand);
        return pipelineCommands;
    }

    private void executeBuiltin(
            List<String> command,
            InputStream inputStream,
            PrintStream outputStream,
            PrintStream errorStream,
            Map<String, CommandHandler> commands
    ) {
        CommandHandler handler = commands.get(command.getFirst());

        if (handler == null) {
            return;
        }

        String arguments = String.join(
                " ",
                command.subList(1, command.size())
        );

        handler.execute(arguments, inputStream, outputStream, errorStream);
    }

    private void executeExternal(
            List<String> command,
            InputStream inputStream,
            OutputStream outputStream
    ) throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = pb.start();

        Thread inputThread = new Thread(() -> {
            try (OutputStream processInput = process.getOutputStream()) {
                inputStream.transferTo(processInput);
            } catch (IOException ignored) {
                // The process may exit before consuming all pipeline input.
            }
        });

        Thread outputThread = new Thread(() -> {
            try (InputStream processOutput = process.getInputStream()) {
                processOutput.transferTo(outputStream);
                outputStream.flush();
            } catch (IOException ignored) {
                // The downstream command has stopped reading. Terminate this
                // process so it cannot remain blocked while writing stdout.
                terminateProcess(process);
            }
        });

        inputThread.start();
        outputThread.start();

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            terminateProcess(process);
            throw e;
        } finally {
            // The stage owns inputStream. Interrupt the pump here and let the
            // stage's finally block close the pipeline input after this method returns.
            inputThread.interrupt();
        }

        inputThread.join();
        outputThread.join();
    }

    private void terminateProcess(Process process) {
        if (!process.isAlive()) {
            return;
        }

        process.destroy();

        try {
            if (!process.waitFor(200, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    private void closeQuietly(Closeable stream) {
        try {
            stream.close();
        } catch (IOException ignored) {
            // Pipeline shutdown should continue even if a stream is already closed.
        }
    }
}

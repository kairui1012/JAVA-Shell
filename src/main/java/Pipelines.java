import java.io.*;
import java.util.*;

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

        List<List<String>> pipeline = parse(command);

        if (pipeline.stream().anyMatch(List::isEmpty)) {
            errorStream.println("Invalid pipeline");
            return;
        }

        boolean allExternal = pipeline.stream()
                .noneMatch(cmd -> commands.containsKey(cmd.getFirst()));

        if (allExternal) {
            executeExternalPipeline(pipeline, outputStream);
        } else {
            executeMixedPipeline(pipeline, commands, outputStream, errorStream);
        }
    }

    // External | External | External
    private void executeExternalPipeline(
            List<List<String>> pipeline,
            PrintStream outputStream
    ) throws IOException, InterruptedException {

        List<ProcessBuilder> builders = new ArrayList<>();

        for (List<String> command : pipeline) {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            builders.add(pb);
        }

        List<Process> processes = ProcessBuilder.startPipeline(builders);

        try {
            // First command receives EOF instead of terminal input.
            processes.getFirst().getOutputStream().close();

            Process last = processes.getLast();

            // Stream output immediately.
            try (InputStream stdout = last.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = stdout.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }
            }

            // Wait for the last command only.
            last.waitFor();

        } finally {
            // Stop upstream processes that may still be running.
            for (Process process : processes) {
                if (process.isAlive()) {
                    process.destroy();
                }
            }
        }
    }

    // Builtin | External | Builtin
    private void executeMixedPipeline(
            List<List<String>> pipeline,
            Map<String, CommandHandler> commands,
            PrintStream outputStream,
            PrintStream errorStream
    ) throws IOException, InterruptedException {

        byte[] previousOutput = new byte[0];

        for (int i = 0; i < pipeline.size(); i++) {

            List<String> command = pipeline.get(i);
            boolean isLast = i == pipeline.size() - 1;

            InputStream stdin = new ByteArrayInputStream(previousOutput);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            PrintStream stdout = isLast
                    ? outputStream
                    : new PrintStream(buffer);

            try {
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

                if (!isLast) {
                    previousOutput = buffer.toByteArray();
                }

            } finally {
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

        CommandHandler handler = commands.get(command.getFirst());

        if (handler == null) {
            return;
        }

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

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = pb.start();

        Thread writer = new Thread(() -> {
            try (OutputStream stdin = process.getOutputStream()) {
                inputStream.transferTo(stdin);
            } catch (IOException ignored) {
                // Process may exit before reading all input.
            }
        });

        writer.start();

        try (InputStream stdout = process.getInputStream()) {
            stdout.transferTo(outputStream);
            outputStream.flush();
        }

        process.waitFor();
        writer.join();
    }

    private List<List<String>> parse(String command) {

        List<List<String>> pipeline = new ArrayList<>();

        String[] parts = command.split("\\s+\\|\\s+", -1);

        for (String part : parts) {
            pipeline.add(
                    Quoting.parse(part, new Redirection())
            );
        }

        return pipeline;
    }
}
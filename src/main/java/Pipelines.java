import java.io.*;
import java.util.*;
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

            OutputStream currentOutput;
            InputStream nextInput = null;

            if (isLast) {
                currentOutput = outputStream;
            } else {
                PipedInputStream pipeInput = new PipedInputStream(8192);
                currentOutput = new PipedOutputStream(pipeInput);
                nextInput = pipeInput;
            }

            InputStream finalInput = isFirst
                    ? InputStream.nullInputStream()
                    : currentInput;

            OutputStream finalOutput = currentOutput;

            Thread commandThread = new Thread(() -> {

                try {
                    if (commands.containsKey(currentCommand.getFirst())) {

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
                    if (!isFirst) {
                        closeQuietly(finalInput);
                    }

                    if (!isLast) {
                        closeQuietly(finalOutput);
                    }
                }
            });

            threads.add(commandThread);
            commandThread.start();

            currentInput = nextInput;
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }

    private List<List<String>> parse(String command) {

        List<List<String>> pipelineCommands = new ArrayList<>();

        // Split pipeline stages, then reuse the existing quoting parser.
        // This version expects spaces around the pipe operator.
        String[] parts = command.split("\\s+\\|\\s+", -1);

        for (String part : parts) {

            List<String> parsedCommand = Quoting.parse(
                    part,
                    new Redirection()
            );

            pipelineCommands.add(parsedCommand);
        }

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

        // Upstream stdout -> process stdin
        Thread inputThread = new Thread(() -> {
            try (OutputStream processInput = process.getOutputStream()) {

                inputStream.transferTo(processInput);

            } catch (IOException ignored) {
                // The process may stop reading before input is exhausted.
            }
        });

        // Process stdout -> downstream stdin
        Thread outputThread = new Thread(() -> {
            try (InputStream processOutput = process.getInputStream()) {

                processOutput.transferTo(outputStream);
                outputStream.flush();

            } catch (IOException e) {
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
            // Stream may already be closed.
        }
    }
}
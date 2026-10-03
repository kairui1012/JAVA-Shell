import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Shell {

    @FunctionalInterface
    private interface CommandHandler {
        // Returns true to continue the shell or false to exit.
        boolean execute(String arguments, PrintStream outputStream, PrintStream errorStream);
    }

    public void run() throws Exception {
        // Split PATH into directories used to locate external programs.
        String systemPath = System.getenv("PATH");
        String[] directories = systemPath.split(
                Pattern.quote(File.pathSeparator)
        );

        Scanner scanner = new Scanner(System.in);
        HashMap<String, CommandHandler> commands = new HashMap<>();
        Navigation navigation = new Navigation();

        // Register commands that are handled directly by this shell.
        commands.put("exit", (arguments, outputStream, errorStream) -> exit());
        commands.put("echo", this::echo);
        commands.put("type", (arguments, outputStream, errorStream) -> type(arguments, commands, directories, outputStream));
        commands.put("pwd", (arguments, outputStream, errorStream) -> navigation.pwd(outputStream));
        commands.put("cd", (arguments, outputStream, errorStream) -> navigation.cd(arguments));

        while (true) {
            System.out.print("$ ");

            // Reads the full command entered by the user.
            String command = scanner.nextLine();

            Redirection redirection = new Redirection();

            // Split the input while preserving quoted arguments.
            List<String> parsedCommand = Quoting.parse(command, redirection);

            if (parsedCommand.isEmpty()) {
                continue;
            }

            String commandName = parsedCommand.getFirst();

            List<String> parsedArguments = parsedCommand.subList(
                    1,
                    parsedCommand.size()
            );

            String parsedArgumentLine = String.join(" ", parsedArguments);

            List<String> processCommand = new ArrayList<>();


            // A handler returns true to continue the shell and false to exit.
            CommandHandler handler = commands.get(commandName);


            if (handler != null) {
                PrintStream outputStream = System.out;
                PrintStream errorStream = System.err;
                try {
                    if (redirection.isRedirectionRequired()
                            && redirection.hasOutputFile()) {
                        outputStream = new PrintStream(
                                redirection.getOutputFile()
                        );
                    }

                    if (redirection.isErrorRedirectionRequired()
                            && redirection.hasErrorFile()) {
                        errorStream = new PrintStream(
                                redirection.getErrorFile()
                        );
                    }

                    if (!handler.execute(parsedArgumentLine, outputStream, errorStream)) {
                        break;
                    }

                } finally {

                    if (outputStream != System.out) {
                        outputStream.close();
                    }

                    if (errorStream != System.err) {
                        errorStream.close();
                    }
                }

                continue;
            }
            // Search each PATH directory for an executable with this name.
            for (String directory : directories) {
                Path candidate = Path.of(directory, commandName);

                if (Files.isRegularFile(candidate)
                        && Files.isExecutable(candidate)) {
                    processCommand.add(commandName);
                    processCommand.addAll(parsedArguments);
                    break;
                }
            }

            if (processCommand.isEmpty()) {
                System.out.println(commandName + ": command not found");
            } else {
                ProcessBuilder pb = new ProcessBuilder(processCommand);
                // Connect the child process to this shell's input and output.
                pb.inheritIO();

                if (redirection.isRedirectionRequired()
                        && redirection.hasOutputFile()) {
                    pb.redirectOutput(
                            ProcessBuilder.Redirect.to(
                                    new File(redirection.getOutputFile())
                            )
                    );
                }

                if (redirection.isErrorRedirectionRequired()
                        && redirection.hasErrorFile()) {
                    pb.redirectError(
                            ProcessBuilder.Redirect.to(
                                    new File(redirection.getErrorFile())
                            )
                    );
                }

                try (Process process = pb.start()) {
                    process.waitFor();
                }
            }
        }
    }

    private boolean exit() {
        return false;
    }

    private boolean echo(String arguments, PrintStream outputStream,PrintStream errorStream ) {
        outputStream.println(arguments);
        return true;
    }

    private boolean type(
            String arguments,
            HashMap<String, CommandHandler> commands,
            String[] directories,
            PrintStream outputStream
    ) {
        String target = arguments.trim();

        // A command can either be a shell built-in or an external executable.
        if (commands.containsKey(target)) {
            outputStream.println(target + " is a shell builtin");
        } else {
            boolean found = false;

            for (String directory : directories) {
                Path candidate = Path.of(directory, target);

                if (Files.isRegularFile(candidate)
                        && Files.isExecutable(candidate)) {
                    outputStream.println(target + " is " + candidate);
                    found = true;
                    break;
                }
            }

            if (!found) {
                outputStream.println(target + ": not found");
            }
        }

        return true;
    }

}

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Shell {

    @FunctionalInterface
    private interface CommandHandler {
        // Returns true to continue the shell or false to exit.
        boolean execute(String arguments);
    }

    String currentDirectory = System.getProperty("user.dir");


    public void run() throws Exception {
        String systemPath = System.getenv("PATH");
        String[] directories = systemPath.split(
                Pattern.quote(File.pathSeparator)
        );



        Scanner scanner = new Scanner(System.in);
        HashMap<String, CommandHandler> commands = new HashMap<>();

        commands.put("exit", arguments -> exit());
        commands.put("echo", this::echo);
        commands.put("type", arguments -> type(arguments, commands, directories));
        commands.put("pwd", arguments -> pwd(currentDirectory));
        commands.put("cd", this::cd);


        while (true) {
            System.out.print("$ ");

            // Reads the full command entered by the user.
            String command = scanner.nextLine();

            // Splits the input at the first space into a command name and its arguments.
            String[] commandParts = command.split(" ", 2);

            // The first part identifies which command handler to execute.
            String commandName = commandParts[0];

            // Uses the remaining text as arguments, or an empty string when none are provided.
            String arguments = commandParts.length > 1 ? commandParts[1] : "";

            List<String> processCommand = new ArrayList<>();
            List<String> parsedArguments = getStrings(arguments);
            String parsedArgumentLine = String.join(" ", parsedArguments);

            // A handler returns true to continue the shell and false to exit.
            CommandHandler handler = commands.get(commandName);
            if (handler != null) {
                if (!handler.execute(parsedArgumentLine)) {
                    break;
                }
                continue;
            }

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
                pb.inheritIO();

                try (Process process = pb.start()) {
                    process.waitFor();
                }
            }
        }
    }

    private List<String> getStrings(String arguments) {
        List<String> parsedArguments = new ArrayList<>();
        boolean insideSingleQuote = false;
        StringBuilder currentArgument = new StringBuilder();

        for (int i = 0; i < arguments.length(); i++) {
            char currentChar = arguments.charAt(i);

            if (currentChar == '\'') {
                insideSingleQuote = !insideSingleQuote;
                continue;
            }

            if (currentChar == ' ' && !insideSingleQuote) {
                if (!currentArgument.isEmpty()) {
                    parsedArguments.add(currentArgument.toString());
                    currentArgument.setLength(0);
                }
                continue;
            }

            currentArgument.append(currentChar);
        }

        if (!currentArgument.isEmpty()) {
            parsedArguments.add(currentArgument.toString());
        }
        return parsedArguments;
    }


    private boolean exit() {
        return false;
    }

    private boolean echo(String arguments) {
        System.out.println(arguments);
        return true;
    }

    private boolean type(
            String arguments,
            HashMap<String, CommandHandler> commands,
            String[] directories
    ) {
        String target = arguments.trim();

        if (commands.containsKey(target)) {
            System.out.println(target + " is a shell builtin");
        } else {
            boolean found = false;

            for (String directory : directories) {
                Path candidate = Path.of(directory, target);

                if (Files.isRegularFile(candidate)
                        && Files.isExecutable(candidate)) {
                    System.out.println(target + " is " + candidate);
                    found = true;
                    break;
                }
            }

            if (!found) {
                System.out.println(target + ": not found");
            }
        }

        return true;
    }

    private boolean pwd(String currentDirectory) {
        System.out.println(currentDirectory);
        return true;
    }

    private boolean cd(String arguments) {

        String input = arguments.trim();
        String homeDirectory = System.getenv("HOME");

        if (homeDirectory == null || homeDirectory.isBlank()) {
            homeDirectory = System.getProperty("user.home");
        }


        if (input.isEmpty()) {
            currentDirectory = System.getProperty("user.home");
        }
        else if (input.equals("~") || input.startsWith("~/")) {

            Path path = Path.of(homeDirectory);

            if (input.startsWith("~/")) {
                path = path.resolve(input.substring(2)).normalize();
            }

            if (!Files.isDirectory(path)) {
                System.out.println("cd: no such file or directory: " + arguments);
                return false;
            }

            currentDirectory = path.toString();
        }
        else {
            Path path = Path.of(currentDirectory)
                    .resolve(input)
                    .normalize();

            if (!Files.isDirectory(path)) {
                System.out.println("cd: no such file or directory: " + arguments);
            }
            else {
                currentDirectory = path.toString();
            }
        }

        return true;
    }

}

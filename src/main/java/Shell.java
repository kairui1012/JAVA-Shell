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

            List<String> parsedCommand = getStrings(command);

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
        List<String> result = new ArrayList<>();
        boolean insideSingleQuote = false;
        boolean insideDoubleQuote = false;
        boolean escapeNextCharacter = false;
        StringBuilder currentArgument = new StringBuilder();

        for (int i = 0; i < arguments.length(); i++) {
            char currentChar = arguments.charAt(i);

            if (escapeNextCharacter) {
                currentArgument.append(currentChar);
                escapeNextCharacter = false;
                continue;
            }

            if (currentChar == '\\') {

                if (insideSingleQuote) {
                    // 单引号内：反斜杠是普通字符
                    currentArgument.append(currentChar);
                    continue;
                }
                else if (insideDoubleQuote) {
                    // 双引号内：检查下一个字符
                    if (i + 1 < arguments.length()) {
                        char nextCharacter = arguments.charAt(i + 1);
                        if (nextCharacter == '\\'
                                || nextCharacter == '"'
                                || nextCharacter == '$'
                                || nextCharacter == '`') {
                            escapeNextCharacter = true;
                            continue;
                        }
                    }
                } else {
                    // 所有引号外：保护任意下一个字符
                    escapeNextCharacter = true;
                    continue;
                }
            }

            if (currentChar == '\'' && !insideDoubleQuote) {
                insideSingleQuote = !insideSingleQuote;
                continue;
            }

            if (currentChar == '"' && !insideSingleQuote) {
                insideDoubleQuote = !insideDoubleQuote;
                continue;
            }

            if (currentChar == ' ' && !insideSingleQuote  && !insideDoubleQuote) {
                if (!currentArgument.isEmpty()) {
                    result.add(currentArgument.toString());
                    currentArgument.setLength(0);
                }
                continue;
            }

            currentArgument.append(currentChar);

        }

        if (!currentArgument.isEmpty()) {
            result.add(currentArgument.toString());
        }
        return result;
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

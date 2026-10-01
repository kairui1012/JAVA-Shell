import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws Exception {

        Set<String> builtins = Set.of("echo", "exit", "type", "pwd", "cd");
        // Retrieves the system PATH, for example:
        // /opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin
        String systemPath = System.getenv("PATH");

        // Splits the directories using the current operating system's PATH separator
        String[] directories = systemPath.split(
                Pattern.quote(File.pathSeparator)
        );

        Scanner scanner = new Scanner(System.in);

        while(true){
            // TODO: Uncomment the code below to pass the first stage
            System.out.print("$ ");

            // Captures the user's command in the "command" variable
            String command = scanner.nextLine();

            if (command.equals("exit")) {
                break;
            }

            if (command.startsWith("echo ")) {
                System.out.println(command.substring(5));
                continue;
            }

            if (command.startsWith("type ")) {
                // Extracts the command name after "type"; for example, "type echo" yields "echo"
                String target = command.substring(5).trim();

                // Checks whether the command is a shell builtin first
                if (builtins.contains(target)) {
                    System.out.println(target + " is a shell builtin");
                } else {

                    boolean found = false;

                    // Searches for the command in each directory in PATH order
                    for (String directory : directories) {
                        Path candidate = Path.of(directory, target);

                        // The candidate must be a regular file that the current user can execute
                        if (Files.isRegularFile(candidate)
                                && Files.isExecutable(candidate)) {

                            System.out.println(target + " is " + candidate);

                            found = true;

                            // The shell uses only the first matching executable in PATH
                            break;
                        }
                    }

                    // The command was not found after searching every directory in PATH
                    if (!found) {
                        System.out.println(target + ": not found");
                    }
                }

                // The type command is complete; continue with the shell's next iteration
                continue;
            }

            if (command.equals("pwd")) {
                System.out.println(System.getProperty("user.dir"));
                continue;
            }

            // Prepares the executable path and arguments for ProcessBuilder
            List<String> processCommand = new ArrayList<>();

            // Splits the input into the command name and its arguments
            String[] parts = command.trim().split("\\s+");
            String commandName = parts[0];

            // Searches each PATH directory for an executable command
            for (String directory : directories) {
                Path candidate = Path.of(directory, commandName);

                if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {

                    // Places the executable first, followed by its arguments
                    processCommand.add(commandName);
                    processCommand.addAll(Arrays.asList(parts).subList(1, parts.length));
                    break;
                }
            }

            // Prints the "<command>: command not found" message
            if (processCommand.isEmpty()){
                System.out.println(commandName + ": command not found");
            }
            else {
                ProcessBuilder pb = new ProcessBuilder(processCommand);

                // Connects the child process directly to the shell's terminal
                pb.inheritIO();
                try (Process process = pb.start()) {
                    // Waits for the command to finish before showing the next prompt
                    process.waitFor();
                }
            }
        }
    }
}

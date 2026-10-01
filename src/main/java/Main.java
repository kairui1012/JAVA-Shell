import java.util.Scanner;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws Exception {
        Set<String> builtins = Set.of("echo", "exit", "type", "pwd", "cd");
        while(true){
            // TODO: Uncomment the code below to pass the first stage
            System.out.print("$ ");

            // Captures the user's command in the "command" variable
            Scanner scanner = new Scanner(System.in);
            String command = scanner.nextLine();

            if (command.equals("exit")) {
                break;
            }

            if (command.startsWith("echo ")) {
                System.out.println(command.substring(5));
                continue;
            }

            if (command.startsWith("type ")) {
                String target = command.substring(5);
                if (builtins.contains(target)){
                    System.out.println(target+" is a shell builtin");
                }
                else {
                    System.out.println(target+": not found");
                }
                continue;
            }

            // Prints the "<command>: command not found" message
            System.out.println(command + ": command not found");

        }
    }
}

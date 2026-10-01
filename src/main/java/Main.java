import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        while(true){
            // TODO: Uncomment the code below to pass the first stage
            System.out.print("$ ");

            // Captures the user's command in the "command" variable
            Scanner scanner = new Scanner(System.in);
            String command = scanner.nextLine();

            if (command.equals("exit")) {
                break;
            }

            // Prints the "<command>: command not found" message
            System.out.println(command + ": command not found");

        }
    }
}

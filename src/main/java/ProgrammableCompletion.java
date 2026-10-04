import java.io.PrintStream;

public class ProgrammableCompletion {
    public boolean complete(String arguments, PrintStream errorStream) {
        String[] parts = arguments.split("\\s+");
        if (parts.length >= 2 && parts[0].equals("-p")) {
            String command = parts[1];

            errorStream.println(
                    "complete: " + command + ": no completion specification"
            );
        }
        return true;
    }
}

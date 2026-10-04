import java.io.PrintStream;
import java.util.HashMap;

public class ProgrammableCompletion {
    public boolean complete(String arguments, PrintStream outputStream, PrintStream errorStream,HashMap<String,String> commandCompleters) {
        String[] parts = arguments.split("\\s+");

        if (parts.length >= 3 && parts[0].equals("-C")) {
            String command = parts[2];
            String completer = parts[1];
            commandCompleters.put(command,completer);
        }
        else if (parts.length >= 1 && parts[0].equals("-p")) {
            if (parts.length == 1)
            {
                commandCompleters.forEach((command, completer) -> {
                    outputStream.println(
                            "complete -C '" + completer + "' " + command
                    );
                });
            }

            if (parts.length > 1){
                String command = parts[1];
                if (commandCompleters.containsKey(command)){
                    outputStream.println(
                            "complete -C '" + commandCompleters.get(command) + "' " + command
                    );
                }
                else
                {
                    errorStream.println(
                            "complete: " + command + ": no completion specification"
                    );
                }
            }
        }
        return true;
    }
}

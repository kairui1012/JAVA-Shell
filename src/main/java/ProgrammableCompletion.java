import java.io.PrintStream;
import java.util.HashMap;

public class ProgrammableCompletion {

    public boolean complete(
            String arguments,
            PrintStream outputStream,
            PrintStream errorStream,
            HashMap<String, String> completerCommandsByTarget
    ) {
        String[] argumentParts = arguments.split("\\s+");

        if (argumentParts.length >= 3 && argumentParts[0].equals("-C")) {
            String targetCommand = argumentParts[2];
            String completerCommand = argumentParts[1];

            // Register which external completer command belongs to the target command.
            completerCommandsByTarget.put(targetCommand, completerCommand);
        } else if (argumentParts.length >= 1 && argumentParts[0].equals("-p")) {
            if (argumentParts.length == 1) {
                // Print every registered target-command and completer-command pair.
                completerCommandsByTarget.forEach((targetCommand, completerCommand) -> {
                    outputStream.println(
                            "complete -C '" + completerCommand + "' " + targetCommand
                    );
                });
            }

            if (argumentParts.length > 1) {
                String targetCommand = argumentParts[1];

                if (completerCommandsByTarget.containsKey(targetCommand)) {
                    outputStream.println(
                            "complete -C '"
                                    + completerCommandsByTarget.get(targetCommand)
                                    + "' "
                                    + targetCommand
                    );
                } else {
                    errorStream.println(
                            "complete: " + targetCommand + ": no completion specification"
                    );
                }
            }
        }
        return true;
    }
}

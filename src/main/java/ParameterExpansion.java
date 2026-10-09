import java.io.PrintStream;
import java.util.Map;

public class ParameterExpansion {

    public static boolean declare(
            String arguments,
            PrintStream outputStream,
            PrintStream errorStream,
            Map<String, String> variables
    ) {
        String[] parts = arguments.trim().split("\\s+", 2);

        if (parts[0].equals("-p")) {

            if (parts.length < 2 || parts[1].isBlank()) {
                errorStream.println("declare: missing variable name");
                return true;
            }

            String variableName = parts[1].trim();

            if (!variables.containsKey(variableName)) {
                errorStream.println(
                        "declare: " + variableName + ": not found"
                );
                return true;
            }

            outputStream.println(
                    "declare -- " + variableName + "=\"" +
                            variables.get(variableName) + "\""
            );
        }

        return true;
    }
}

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
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
        else if (arguments.contains("=")) {

            List<String> leftVariable = new ArrayList<>();
            List<String> rightValue = new ArrayList<>();

            // Split at the first '='
            String[] assignment = arguments.split("=", 2);

            leftVariable.add(assignment[0].trim());
            rightValue.add(assignment[1]);

            if (leftVariable.getFirst().isEmpty()
                    || Character.isDigit(leftVariable.getFirst().charAt(0))) {

                errorStream.println(
                        "declare: `" + arguments + "': not a valid identifier"
                );
                return true;
            }

            variables.put(leftVariable.getFirst(), rightValue.getFirst());
        }


        return true;
    }
}
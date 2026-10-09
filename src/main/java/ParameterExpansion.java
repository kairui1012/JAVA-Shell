import java.io.PrintStream;

public class ParameterExpansion {

    public static boolean declare(String arguments, PrintStream outputStream, PrintStream errorStream) {
        String[] parts = arguments.trim().split("\\s+", 2);

        boolean found = false;
        String option = parts[0];
        if (option.equals("-p")){
            if (!found){
                outputStream.println("declare: "+parts[1]+": not found");
            }
        }
        return true;
    }
}

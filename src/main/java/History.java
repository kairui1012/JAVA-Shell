import java.io.PrintStream;
import java.util.HashMap;

public class History {
    public boolean history(String arguments, PrintStream outputStream, HashMap<Integer, String> historyHashMap) {

        if (!arguments.isBlank()) {
            int limit = Integer.parseInt(arguments.trim());
            int start = Math.max(1, historyHashMap.size() - limit + 1);

            for (int i = start; i <= historyHashMap.size(); i++) {
                outputStream.printf("%5d  %s%n", i, historyHashMap.get(i));
            }
            return true;
        }

        for (int i = 1; i <= historyHashMap.size(); i++) {
            outputStream.printf("%5d  %s%n", i, historyHashMap.get(i));
        }
        return true;
    }
}

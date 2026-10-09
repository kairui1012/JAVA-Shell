import java.io.PrintStream;
import java.util.HashMap;

public class History {
    public boolean history(PrintStream outputStream, HashMap<Integer, String> historyHashMap) {
        for (int i = 1; i <= historyHashMap.size(); i++) {
            outputStream.printf("%5d  %s%n", i, historyHashMap.get(i));
        }
        return true;
    }
}

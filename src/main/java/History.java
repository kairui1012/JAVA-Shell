import java.io.PrintStream;
import java.util.ListIterator;

public class History {
    public boolean history(
            String arguments,
            PrintStream outputStream,
            org.jline.reader.History lineHistory
    ) {
        int start = lineHistory.first();

        if (!arguments.isBlank()) {
            int limit = Integer.parseInt(arguments.trim());
            start = Math.max(lineHistory.first(), lineHistory.last() - limit + 1);
        }

        ListIterator<org.jline.reader.History.Entry> entries = lineHistory.iterator(start);

        while (entries.hasNext()) {
            org.jline.reader.History.Entry entry = entries.next();
            outputStream.printf("%5d  %s%n", entry.index() + 1, entry.line());
        }

        return true;
    }
}

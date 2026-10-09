import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ListIterator;

public class History {
    public boolean history(
            String arguments,
            PrintStream outputStream,
            org.jline.reader.History lineHistory
    ) {
        int start = lineHistory.first();

        if (!arguments.isBlank()) {

            String[] parts = arguments.trim().split("\\s+", 2);

            if (parts[0].equals("-r")) {
                // parts[1] 就是文件路径
                String filePath = parts[1];
                List<String> lines = null;
                try {
                    lines = Files.readAllLines(Path.of(filePath));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                for (String line : lines) {
                    lineHistory.add(line);
                }
            }
            else if (parts[0].equals("-r")){

            }


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

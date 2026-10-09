import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.ListIterator;

public class History {

    private int lastAppendedIndex = -1;

    public boolean history(
            String arguments,
            PrintStream outputStream,
            PrintStream errorStream,
            org.jline.reader.History lineHistory
    ) {

        int start = lineHistory.first();

        if (!arguments.isBlank()) {

            String[] parts = arguments.trim().split("\\s+", 2);

            String option = parts[0];

            // Handle file operations: -r, -w, -a
            if (option.equals("-r")
                    || option.equals("-w")
                    || option.equals("-a")) {

                // Check whether a file path was provided
                if (parts.length < 2 || parts[1].isBlank()) {
                    errorStream.println("history: missing file path");
                    return true;
                }

                String filePath = parts[1];
                Path path = Path.of(filePath);

                // ==========================
                // Read history from file
                // ==========================
                if (option.equals("-r")) {

                    try {
                        List<String> lines = Files.readAllLines(path);

                        for (String line : lines) {
                            lineHistory.add(line);
                        }

                    } catch (IOException e) {
                        errorStream.println("history: " + e.getMessage());
                    }

                    return true;
                }

                // ==========================
                // Write history to file
                // ==========================
                else if (option.equals("-w")) {

                    StringBuilder content = new StringBuilder();

                    for (org.jline.reader.History.Entry entry : lineHistory) {
                        content.append(entry.line()).append("\n");
                    }

                    try {
                        Files.writeString(path, content.toString());

                    } catch (IOException e) {
                        errorStream.println("history: " + e.getMessage());
                    }

                    return true;
                }

                // ==========================
                // Append new history to file
                // ==========================
                else if (option.equals("-a")) {

                    StringBuilder content = new StringBuilder();

                    for (org.jline.reader.History.Entry entry : lineHistory) {

                        if (entry.index() > lastAppendedIndex) {
                            content.append(entry.line()).append("\n");
                        }
                    }

                    try {
                        Files.writeString(
                                path,
                                content.toString(),
                                StandardOpenOption.CREATE,
                                StandardOpenOption.APPEND
                        );

                        // Update only after successful write
                        lastAppendedIndex = lineHistory.last();

                    } catch (IOException e) {
                        errorStream.println("history: " + e.getMessage());
                    }

                    return true;
                }
            }

            // ==========================
            // History with numeric limit
            // ==========================
            try {

                int limit = Integer.parseInt(arguments.trim());

                if (limit <= 0) {
                    return true;
                }

                start = Math.max(
                        lineHistory.first(),
                        lineHistory.last() - limit + 1
                );

            } catch (NumberFormatException e) {
                errorStream.println("history: numeric argument required");
                return true;
            }
        }

        // ==========================
        // Display history
        // ==========================
        ListIterator<org.jline.reader.History.Entry> entries =
                lineHistory.iterator(start);

        while (entries.hasNext()) {

            org.jline.reader.History.Entry entry = entries.next();

            outputStream.printf(
                    "%5d  %s%n",
                    entry.index() + 1,
                    entry.line()
            );
        }

        return true;
    }
}
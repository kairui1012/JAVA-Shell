import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class FileCompletion {

    public Result findMatches(String buffer) {
        int lastSpace = buffer.lastIndexOf(' ');

        String commandPart = buffer.substring(0, lastSpace + 1);
        String pathInput = buffer.substring(lastSpace + 1);

        int lastSlash = pathInput.lastIndexOf('/');

        String parent;
        String prefix;

        if (lastSlash == -1) {
            parent = "";
            prefix = pathInput;
        } else {
            parent = pathInput.substring(0, lastSlash + 1);
            prefix = pathInput.substring(lastSlash + 1);
        }

        File directory;

        if (parent.isEmpty()) {
            directory = new File(".");
        } else {
            directory = new File(parent);
        }

        List<String> matches = new ArrayList<>();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith(prefix)) {
                    String match = parent + file.getName();

                    if (file.isDirectory()) {
                        match += "/";
                    }

                    matches.add(match);
                }
            }
        }

        return new Result(commandPart, pathInput, matches);
    }

    public record Result(
            String commandPart,
            String pathInput,
            List<String> matches
    ) {
    }
}

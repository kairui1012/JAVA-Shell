import org.jline.reader.impl.completer.StringsCompleter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CommandCompletion {

    private final Set<String> candidates;

    public CommandCompletion(Set<String> builtInCommands, String[] directories) {
        candidates = new HashSet<>(builtInCommands);

        // Add executable filenames found in every directory listed in PATH.
        for (String directory : directories) {
            File dir = new File(directory);
            File[] files = dir.listFiles();

            // Ignore PATH entries that cannot be read or are not directories.
            if (files == null) {
                continue;
            }

            for (File file : files) {
                if (file.isFile() && file.canExecute()) {
                    candidates.add(file.getName());
                }
            }
        }
    }

    public StringsCompleter createCompleter() {
        return new StringsCompleter(candidates);
    }

    public List<String> findMatches(String input) {
        List<String> matches = new ArrayList<>();

        for (String candidate : candidates) {
            if (candidate.startsWith(input)) {
                matches.add(candidate);
            }
        }

        return matches;
    }
}

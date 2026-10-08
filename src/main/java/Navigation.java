import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Navigation {

    private final InheritableThreadLocal<String> currentDirectory =
            new InheritableThreadLocal<>() {
                @Override
                protected String initialValue() {
                    return System.getProperty("user.dir");
                }
            };

    public boolean pwd(PrintStream outputStream) {
        outputStream.println(currentDirectory.get());
        return true;
    }

    public boolean cd(String arguments) {
        String input = arguments.trim();
        String homeDirectory = System.getenv("HOME");

        if (homeDirectory == null || homeDirectory.isBlank()) {
            homeDirectory = System.getProperty("user.home");
        }

        if (input.isEmpty()) {
            currentDirectory.set(System.getProperty("user.home"));
        } else if (input.equals("~") || input.startsWith("~/")) {
            Path path = Path.of(homeDirectory);

            if (input.startsWith("~/")) {
                path = path.resolve(input.substring(2)).normalize();
            }

            if (!Files.isDirectory(path)) {
                System.out.println("cd: no such file or directory: " + arguments);
                return false;
            }

            currentDirectory.set(path.toString());
        } else {
            Path path = Path.of(currentDirectory.get())
                    .resolve(input)
                    .normalize();

            if (!Files.isDirectory(path)) {
                System.out.println("cd: no such file or directory: " + arguments);
            } else {
                currentDirectory.set(path.toString());
            }
        }

        return true;
    }
}

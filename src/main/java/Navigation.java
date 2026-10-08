import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Navigation {

    private String currentDirectory = System.getProperty("user.dir");

    public boolean pwd(PrintStream outputStream) {
        outputStream.println(currentDirectory);
        return true;
    }

    public boolean cd(String arguments) {
        String input = arguments.trim();
        String homeDirectory = System.getenv("HOME");

        if (homeDirectory == null || homeDirectory.isBlank()) {
            homeDirectory = System.getProperty("user.home");
        }

        if (input.isEmpty()) {
            currentDirectory = System.getProperty("user.home");
        } else if (input.equals("~") || input.startsWith("~/")) {
            Path path = Path.of(homeDirectory);

            if (input.startsWith("~/")) {
                path = path.resolve(input.substring(2)).normalize();
            }

            if (!Files.isDirectory(path)) {
                System.out.println("cd: no such file or directory: " + arguments);
                return false;
            }

            currentDirectory = path.toString();
        } else {
            Path path = Path.of(currentDirectory)
                    .resolve(input)
                    .normalize();

            if (!Files.isDirectory(path)) {
                System.out.println("cd: no such file or directory: " + arguments);
            } else {
                currentDirectory = path.toString();
            }
        }

        return true;
    }
}

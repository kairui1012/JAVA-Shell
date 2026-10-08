import java.io.InputStream;
import java.io.PrintStream;

@FunctionalInterface
public interface CommandHandler {

    // Returns true to continue the shell or false to exit.
    boolean execute(
            String arguments,
            InputStream inputStream,
            PrintStream outputStream,
            PrintStream errorStream
    );
}

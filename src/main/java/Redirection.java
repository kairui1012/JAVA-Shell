import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public final class Redirection {

    private String outputFile;
    private boolean redirectionRequired = false;
    private String errorFile;
    private boolean errorRedirectionRequired = false;
    private boolean append = false;

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public boolean hasOutputFile() {
        return outputFile != null;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setRedirectionRequired(boolean redirectionRequired) {
        this.redirectionRequired = redirectionRequired;
    }

    public boolean isRedirectionRequired() {
        return redirectionRequired;
    }

    public void setErrorFile(String errorFile) {
        this.errorFile = errorFile;
    }

    public boolean hasErrorFile() {
        return errorFile != null;
    }

    public String getErrorFile() {
        return errorFile;
    }

    public void setErrorRedirectionRequired(boolean errorRedirectionRequired) {
        this.errorRedirectionRequired = errorRedirectionRequired;
    }

    public boolean isErrorRedirectionRequired() {
        return errorRedirectionRequired;
    }

    public void setAppend(boolean append) {
        this.append = append;
    }

    public boolean isAppend() {
        return append;
    }

    public void startOutputRedirection(boolean append) {
        setAppend(append);
        setRedirectionRequired(true);
    }

    public void startErrorRedirection(boolean append) {
        setAppend(append);
        setErrorRedirectionRequired(true);
    }

    public PrintStream openOutputStream(
            PrintStream defaultOutputStream
    ) throws FileNotFoundException {
        if (isRedirectionRequired() && hasOutputFile()) {
            return new PrintStream(
                    new FileOutputStream(getOutputFile(), isAppend())
            );
        }

        return defaultOutputStream;
    }

    public PrintStream openErrorStream(
            PrintStream defaultErrorStream
    ) throws FileNotFoundException {
        if (isErrorRedirectionRequired() && hasErrorFile()) {
            return new PrintStream(
                    new FileOutputStream(getErrorFile(), isAppend())
            );
        }

        return defaultErrorStream;
    }

    public void applyTo(ProcessBuilder processBuilder) {
        if (isRedirectionRequired() && hasOutputFile()) {
            File file = new File(getOutputFile());

            if (isAppend()) {
                processBuilder.redirectOutput(
                        ProcessBuilder.Redirect.appendTo(file)
                );
            } else {
                processBuilder.redirectOutput(
                        ProcessBuilder.Redirect.to(file)
                );
            }
        }

        if (isErrorRedirectionRequired() && hasErrorFile()) {
            File file = new File(getErrorFile());

            if (isAppend()) {
                processBuilder.redirectError(
                        ProcessBuilder.Redirect.appendTo(file)
                );
            } else {
                processBuilder.redirectError(
                        ProcessBuilder.Redirect.to(file)
                );
            }
        }
    }
}

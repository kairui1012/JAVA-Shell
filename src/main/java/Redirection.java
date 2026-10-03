public final class Redirection {

    private String outputFile;
    private boolean redirectionRequired = false;
    private String errorFile;
    private boolean errorRedirectionRequired = false;

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
}

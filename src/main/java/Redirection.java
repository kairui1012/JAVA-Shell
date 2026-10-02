public final class Redirection {

    private String outputFile;
    private boolean redirectionRequired = false;

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
}

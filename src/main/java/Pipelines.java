import java.io.IOException;
import java.util.List;

public class Pipelines {

    public void execute(
            List<String> commandLeft,
            List<String> commandRight
    ) throws IOException {

        ProcessBuilder leftBuilder =
                new ProcessBuilder(commandLeft);

        ProcessBuilder rightBuilder =
                new ProcessBuilder(commandRight);

        rightBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        rightBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

        List<Process> processes =
                ProcessBuilder.startPipeline(
                        List.of(leftBuilder, rightBuilder)
                );

        Process leftProcess = processes.get(0);
        Process rightProcess = processes.get(1);

        try {
            rightProcess.waitFor();

            if (leftProcess.isAlive()) {
                leftProcess.destroy();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
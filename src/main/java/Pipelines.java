import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class Pipelines {

    public void execute(List<String> commandLeft, List<String> commandRight) throws IOException {
        ProcessBuilder leftBuilder =
                new ProcessBuilder(commandLeft);

        ProcessBuilder rightBuilder =
                new ProcessBuilder(commandRight);

        // 右边命令直接输出到当前 shell
        rightBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        rightBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process leftProcess = leftBuilder.start();
        Process rightProcess = rightBuilder.start();

        InputStream leftProcessOutput = leftProcess.getInputStream();

        OutputStream rightProcessInput = rightProcess.getOutputStream();

        Thread pipeThread = new Thread(() -> {
            try {
                leftProcessOutput.transferTo(rightProcessInput);
                rightProcessInput.close();
            } catch (IOException e) {
                // 暂时处理
            }
        });

        pipeThread.start();


        try {
            rightProcess.waitFor();

            if (leftProcess.isAlive()) {
                leftProcess.destroy();
            }

            pipeThread.join();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

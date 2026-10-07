import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class Pipelines {

    public void execute(List<String> commandLeft, List<String> commandRight) throws IOException {
        Process leftProcess =
                new ProcessBuilder(commandLeft).start();

        Process rightProcess =
                new ProcessBuilder(commandRight).start();

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


        InputStream rightProcessOutput =
                rightProcess.getInputStream();

        rightProcessOutput.transferTo(System.out);
    }
}

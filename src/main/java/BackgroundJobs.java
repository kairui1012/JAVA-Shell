import java.io.PrintStream;
import java.util.HashMap;
import java.util.stream.Stream;

public class BackgroundJobs {

    public boolean jobs(
            String arguments,
            PrintStream outputStream,
            HashMap<Integer, Process> backgroundJobsMap,
            HashMap<Integer, String> backgroundCommandsMap
    ) {

        int max = backgroundJobsMap.keySet()
                .stream()
                .max(Integer::compareTo)
                .orElse(0);

        backgroundJobsMap.forEach((jobId, process) -> {

            if (process.isAlive()) {
                if (jobId.equals(max)){
                    outputStream.printf(
                            "[%d]+  %-24s%s &%n",
                            jobId,
                            "Running",
                            backgroundCommandsMap.get(jobId)
                    );
                } else if (jobId.equals(max - 1)) {
                    outputStream.printf(
                            "[%d]-  %-24s%s &%n",
                            jobId,
                            "Running",
                            backgroundCommandsMap.get(jobId)
                    );
                }
                else {
                    outputStream.printf(
                            "[%d]   %-24s%s &%n",
                            jobId,
                            "Running",
                            backgroundCommandsMap.get(jobId)
                    );
                }
            }


        });

        return true;
    }
}
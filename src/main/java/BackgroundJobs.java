import java.io.PrintStream;
import java.util.HashMap;

public class BackgroundJobs {

    public boolean jobs(
            String arguments,
            PrintStream outputStream,
            HashMap<Integer, Process> backgroundJobsMap,
            HashMap<Integer, String> backgroundCommandsMap
    ) {

        backgroundJobsMap.forEach((jobId, process) -> {

            if (process.isAlive()) {
                outputStream.printf(
                        "[%d]+  %-24s%s &%n",
                        jobId,
                        "Running",
                        backgroundCommandsMap.get(jobId)
                );
            }

        });

        return true;
    }
}
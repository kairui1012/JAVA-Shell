import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BackgroundJobs {

    public boolean jobs(
            String arguments,
            PrintStream outputStream,
            HashMap<Integer, Process> backgroundJobsMap,
            HashMap<Integer, String> backgroundCommandsMap
    ) {

        List<Integer> doneJobsList = new ArrayList<>();

        List<Integer> runningJobIds = backgroundJobsMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue().isAlive())
                .map(entry -> entry.getKey())
                .sorted()
                .toList();

        int latestJobId = runningJobIds.isEmpty()
                ? -1
                : runningJobIds.getLast();

        int previousJobId = runningJobIds.size() < 2
                ? -1
                : runningJobIds.get(runningJobIds.size() - 2);

        backgroundJobsMap.forEach((jobId, process) -> {

            if (!process.isAlive()) {
                outputStream.printf(
                        "[%d]   %-24s%s%n",
                        jobId,
                        "Done",
                        backgroundCommandsMap.get(jobId)
                );
                doneJobsList.add(jobId);
            }
            else {
                if (jobId == latestJobId) {
                    outputStream.printf(
                            "[%d]+  %-24s%s &%n",
                            jobId,
                            "Running",
                            backgroundCommandsMap.get(jobId)
                    );

                } else if (jobId == previousJobId) {
                    outputStream.printf(
                            "[%d]-  %-24s%s &%n",
                            jobId,
                            "Running",
                            backgroundCommandsMap.get(jobId)
                    );

                } else {
                    outputStream.printf(
                            "[%d]   %-24s%s &%n",
                            jobId,
                            "Running",
                            backgroundCommandsMap.get(jobId)
                    );
                }
            }
        });

        for (int id : doneJobsList){
            backgroundJobsMap.remove(id);
            backgroundCommandsMap.remove(id);
        }


        return true;
    }
}
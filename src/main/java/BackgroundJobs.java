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

            String status;
            String marker;
            String suffix;

            if (process.isAlive()) {
                status = "Running";
                suffix = " &";
            } else {
                status = "Done";
                suffix = "";
                doneJobsList.add(jobId);
            }

            if (jobId == latestJobId) {
                marker = "+";
            } else if (jobId == previousJobId) {
                marker = "-";
            } else {
                marker = " ";
            }

            outputStream.printf(
                    "[%d]%s  %-24s%s%s%n",
                    jobId,
                    marker,
                    status,
                    backgroundCommandsMap.get(jobId),
                    suffix
            );
        });

        for (int id : doneJobsList) {
            backgroundJobsMap.remove(id);
            backgroundCommandsMap.remove(id);
        }

        return true;
    }
}
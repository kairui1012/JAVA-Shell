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

        // First reap finished jobs
        reapFinishedJobs(
                outputStream,
                backgroundJobsMap,
                backgroundCommandsMap
        );

        // Recalculate markers after removing completed jobs
        List<Integer> jobIds = backgroundJobsMap.keySet()
                .stream()
                .sorted()
                .toList();

        int latestJobId = jobIds.isEmpty()
                ? -1
                : jobIds.getLast();

        int previousJobId = jobIds.size() < 2
                ? -1
                : jobIds.get(jobIds.size() - 2);

        for (int jobId : jobIds) {

            Process process = backgroundJobsMap.get(jobId);

            if (!process.isAlive()) {
                continue;
            }

            String marker;

            if (jobId == latestJobId) {
                marker = "+";
            } else if (jobId == previousJobId) {
                marker = "-";
            } else {
                marker = " ";
            }

            outputStream.printf(
                    "[%d]%s  %-24s%s &%n",
                    jobId,
                    marker,
                    "Running",
                    backgroundCommandsMap.get(jobId)
            );
        }

        return true;
    }

    public void reapFinishedJobs(
            PrintStream outputStream,
            HashMap<Integer, Process> backgroundJobsMap,
            HashMap<Integer, String> backgroundCommandsMap
    ) {

        List<Integer> doneJobsList = new ArrayList<>();

        List<Integer> jobIds = backgroundJobsMap.keySet()
                .stream()
                .sorted()
                .toList();

        int latestJobId = jobIds.isEmpty()
                ? -1
                : jobIds.getLast();

        int previousJobId = jobIds.size() < 2
                ? -1
                : jobIds.get(jobIds.size() - 2);

        for (int jobId : jobIds) {

            Process process = backgroundJobsMap.get(jobId);

            if (process.isAlive()) {
                continue;
            }

            String marker;

            if (jobId == latestJobId) {
                marker = "+";
            } else if (jobId == previousJobId) {
                marker = "-";
            } else {
                marker = " ";
            }

            outputStream.printf(
                    "[%d]%s  %-24s%s%n",
                    jobId,
                    marker,
                    "Done",
                    backgroundCommandsMap.get(jobId)
            );

            doneJobsList.add(jobId);
        }

        for (int id : doneJobsList) {
            backgroundJobsMap.remove(id);
            backgroundCommandsMap.remove(id);
        }
    }
}
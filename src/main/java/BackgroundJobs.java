import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BackgroundJobs {

    public boolean jobs(
            PrintStream outputStream,
            HashMap<Integer, Process> backgroundJobsMap,
            HashMap<Integer, String> backgroundCommandsMap
    ) {

        printJobsAndRemoveFinished(
                outputStream,
                backgroundJobsMap,
                backgroundCommandsMap,
                true
        );

        return true;
    }

    public void reapFinishedJobs(
            PrintStream outputStream,
            HashMap<Integer, Process> backgroundJobsMap,
            HashMap<Integer, String> backgroundCommandsMap
    ) {

        printJobsAndRemoveFinished(
                outputStream,
                backgroundJobsMap,
                backgroundCommandsMap,
                false
        );
    }

    private void printJobsAndRemoveFinished(
            PrintStream outputStream,
            HashMap<Integer, Process> backgroundJobsMap,
            HashMap<Integer, String> backgroundCommandsMap,
            boolean includeRunningJobs
    ) {

        List<Integer> jobIds = backgroundJobsMap.keySet()
                .stream()
                .sorted()
                .toList();

        List<Integer> doneJobsList = new ArrayList<>();

        int latestJobId = jobIds.isEmpty()
                ? -1
                : jobIds.getLast();

        int previousJobId = jobIds.size() < 2
                ? -1
                : jobIds.get(jobIds.size() - 2);

        for (int jobId : jobIds) {

            Process process = backgroundJobsMap.get(jobId);
            boolean isRunning = process.isAlive();

            if (isRunning && !includeRunningJobs) {
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
                    "[%d]%s  %-24s%s%s%n",
                    jobId,
                    marker,
                    isRunning ? "Running" : "Done",
                    backgroundCommandsMap.get(jobId),
                    isRunning ? " &" : ""
            );

            if (!isRunning) {
                doneJobsList.add(jobId);
            }
        }

        for (int id : doneJobsList) {
            backgroundJobsMap.remove(id);
            backgroundCommandsMap.remove(id);
        }
    }
}

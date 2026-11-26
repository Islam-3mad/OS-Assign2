import java.util.*;

public class SJF{

    private static int findShortestProcess(List<Process> processes,
                                           int currentTime,
                                           Process lastExecuted) {

        int shortest = -1;
        if (lastExecuted != null &&
            lastExecuted.arrivalTime <= currentTime &&
            lastExecuted.remainingTime > 0)
     {
            shortest = processes.indexOf(lastExecuted);
        }

        for (int i = 0; i < processes.size(); i++)
        {
            Process p = processes.get(i);
            if (p.arrivalTime <= currentTime && p.remainingTime > 0) {
                if (shortest == -1 ||
                    p.remainingTime < processes.get(shortest).remainingTime) {
                    shortest = i;
                }
            }
        }
        return shortest;
    }
    public static void run(List<Process> processes) {
        int n = processes.size();
        int completed = 0;
        Process lastExecuted = null;
        while (completed < n) 
        {
            int shortest = findShortestProcess(
                processes, Main.currentTime, lastExecuted
            );
            if (shortest == -1) {
                Main.timeline.add(
                    "Time " + Main.currentTime + " | CPU Idle"
                );
                Main.currentTime++;
                lastExecuted = null;
                continue;
            }
            Process current = processes.get(shortest);
            if (lastExecuted != null && lastExecuted != current) {
                Main.applyContextSwitch(
                    lastExecuted,
                    null,
                    Collections.emptyList(),
                    null
                );
                shortest = findShortestProcess(
                    processes, Main.currentTime, lastExecuted
                );
                current = processes.get(shortest);
            }
            if (current.startTime == -1) {
                current.startTime = Main.currentTime;
            }
            Main.timeline.add(
                "Time " + Main.currentTime + " | Running: " + current.id
            );
            current.remainingTime--;
            Main.currentTime++;
            lastExecuted = current;
            if (current.remainingTime == 0) {
                current.finishTime = Main.currentTime;
                completed++;
            }
        }
    }
}

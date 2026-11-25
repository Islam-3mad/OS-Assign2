import java.util.*;

public class ACSTier2 {
    private List<Process> queue = new ArrayList<>();
    private Process running = null;

    public void add(Process p, int currentTime) {
        p.tier2EnterTime = currentTime;
        queue.add(p);
    }

    public List<Process> getQueue() {
        return queue;
    }

    public boolean isEmpty() {
        return queue.isEmpty() && running == null;
    }

    public boolean isRunning() {
        return running != null;
    }

    public Process pickNext(int currentTime) {
        if (running != null) return running;
        if (queue.isEmpty()) return null;

        Process best = queue.get(0);
        for (Process p : queue) {
            if (p.effectivePriority < best.effectivePriority) best = p;
        }
        queue.remove(best);
        running = best;
        if (best.startTime == -1) best.startTime = currentTime;
        return best;
    }

    public boolean runOneUnit(int currentTime) {
        if (running == null) return false;
        running.remainingTime--;
        if (running.remainingTime == 0) {
            running.finishTime = currentTime + 1;
            running = null;
            return true;
        }
        return false;
    }
}
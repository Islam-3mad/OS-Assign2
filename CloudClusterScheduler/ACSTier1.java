import java.util.*;

public class ACSTier1 {

    public enum Result { IDLE, RUNNING, FINISHED, DEMOTED }

    private final LinkedList<Process> queue = new LinkedList<>();
    private final int quantum;
    private Process running = null;
    private int usedInQuantum = 0;
    private ACSTier2 tier2;
    private AgingManager aging;
    private List<Process> pending;

    public ACSTier1(int quantum) {
        if (quantum <= 0) {
            throw new IllegalArgumentException("quantum must be greater than 0");
        }
        this.quantum = quantum;
    }
    public void add(Process p) {
        queue.addLast(p);
    }

    public boolean isEmpty() {
        return queue.isEmpty() && running == null;
    }

    public boolean isRunning() {
        return running != null;
    }

    public Process getRunning() {
        return running;
    }
    public Process pickNext() {
        if (running != null) return running;
        if (queue.isEmpty()) return null;
        running = queue.pollFirst();
        usedInQuantum = 0;
        return running;
    }
    public Result runOneUnit(int currentTime, ACSTier2 tier2) {
        if (running == null) return Result.IDLE;

        if (running.startTime == -1) {
            running.startTime = currentTime;
        }

        running.remainingTime--;
        usedInQuantum++;
        int endTime = currentTime + 1;

        if (running.remainingTime == 0) {
            running.finishTime = endTime;
            running = null;
            return Result.FINISHED;
        }
        if (usedInQuantum == quantum) {
            Process p = running;
            running = null;
            tier2.add(p, endTime);
            return Result.DEMOTED;
        }

        return Result.RUNNING;
    }

    public List<Process> getQueue() {
        return new ArrayList<>(queue);
    }
    public List<Process> getDisplayQueue() {
        List<Process> list = new ArrayList<>();
        if (running != null) list.add(running);
        list.addAll(queue);
        return list;
    }

    public static void run(List<Process> processes, int quantum) {
        new ACSTier1(quantum).simulate(processes);
    }

    private void simulate(List<Process> processes) {
        tier2 = new ACSTier2();
        aging = new AgingManager();
        pending = new ArrayList<>(processes);
        pending.sort(Comparator.comparingInt(p -> p.arrivalTime)); 

        int n = processes.size();
        int done = 0;
        Process last = null;
        admit();

        while (done < n) {
            Process next;
            boolean inTier1;

            if (!isEmpty()) {
                next = pickNext();
                inTier1 = true;
            } else if (!tier2.getQueue().isEmpty()) {
                aging.applyAging(tier2, Main.currentTime);

                Map<Process, Integer> saved = new HashMap<>();
                for (Process p : tier2.getQueue()) saved.put(p, p.startTime);
                next = tier2.pickNext(Main.currentTime);
                next.startTime = saved.get(next);
                inTier1 = false;
            } else {
                logState("CPU idle", null);
                advance();
                last = null;
                continue;
            }

          
            if (last != null && last != next) {
                String msg = (last.remainingTime > 0)
                        ? "Context switch saves " + last.id + " and loads " + next.id
                        : "Context switch loads " + next.id;
                for (int i = 0; i < Main.contextSwitchTime; i++) {
                    logState(msg, inTier1 ? null : next);
                    advance();
                }
            }

            if (inTier1) {
             
                Result r;
                do {
                    logState("Running " + next.id + " (Tier 1)", null);
                    r = runOneUnit(Main.currentTime, tier2);
                    advance();
                } while (r == Result.RUNNING);

                if (r == Result.FINISHED) done++;
            } else {
             
                boolean finished;
                do {
                    logState("Running " + next.id + " (Tier 2)", next);
                    if (next.startTime == -1) next.startTime = Main.currentTime;
                    finished = tier2.runOneUnit(Main.currentTime);
                    advance();
                } while (!finished);
                done++;
            }
            last = next;
        }

        Main.logStep("All processes finished", new ArrayList<>(), new ArrayList<>());
    }

    private void logState(String event, Process tier2Running) {
        aging.applyAging(tier2, Main.currentTime);
        List<Process> t2 = new ArrayList<>();
        if (tier2Running != null) t2.add(tier2Running);
        t2.addAll(tier2.getQueue());
        Main.logStep(event, getDisplayQueue(), t2);
    }
    private void advance() {
        Main.currentTime++;
        admit();
    }

    private void admit() {
        while (!pending.isEmpty() && pending.get(0).arrivalTime <= Main.currentTime) {
            add(pending.remove(0));
        }
    }
}

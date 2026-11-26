public class Process {
    public String id;
    public int arrivalTime;
    public int burstTime;
    public int remainingTime;
    public int initialPriority;
    public int effectivePriority;

    // Metrics
    public int startTime = -1;
    public int finishTime = -1;
    public int waitingTime = 0;
    public int turnaroundTime = 0;
    public int responseTime = 0;

    // Used for Aging calculation in Tier 2
    public int tier2EnterTime = -1;

    public Process(String id, int arrivalTime, int burstTime, int initialPriority) {
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.initialPriority = initialPriority;
        this.effectivePriority = initialPriority;
    }

    @Override
    public String toString() {
        return id + " (remaining: " + remainingTime + ", Priority: " + effectivePriority + ")";
    }
}
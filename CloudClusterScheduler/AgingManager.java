import java.util.*;

/**
 * Member 4 - ACS Tier 3 (Aging Manager).
 *
 * Prevents starvation in ACS Tier 2 (Non-Preemptive Priority Queue) by tracking
 * each waiting process's Tier 2 waiting duration and increasing its priority
 * (lowering its numerical effectivePriority) over time.
 */
public class AgingManager {
    public static final int DEFAULT_AGING_INTERVAL = 4;
    public static final int DEFAULT_MIN_PRIORITY = 0;

    private final int agingInterval;
    private final int minPriority;

    public AgingManager() {
        this(DEFAULT_AGING_INTERVAL, DEFAULT_MIN_PRIORITY);
    }

    public AgingManager(int agingInterval) {
        this(agingInterval, DEFAULT_MIN_PRIORITY);
    }

    public AgingManager(int agingInterval, int minPriority) {
        if (agingInterval <= 0) {
            throw new IllegalArgumentException("agingInterval must be greater than 0");
        }
        this.agingInterval = agingInterval;
        this.minPriority = minPriority;
    }

    public int getAgingInterval() {
        return agingInterval;
    }

    public int getMinPriority() {
        return minPriority;
    }

    /**
     * Calculates how long a process has been waiting in Tier 2 up to currentTime.
     */
    public static int getTier2WaitTime(Process p, int currentTime) {
        if (p == null || p.tier2EnterTime < 0 || currentTime <= p.tier2EnterTime) {
            return 0;
        }
        return currentTime - p.tier2EnterTime;
    }

    /**
     * Computes the priority boost: floor(waitTime / agingInterval).
     */
    public int computeAgingBoost(Process p, int currentTime) {
        return getTier2WaitTime(p, currentTime) / agingInterval;
    }

    /**
     * Computes effectivePriority = max(minPriority, initialPriority - boost).
     * Recomputing from initialPriority keeps aging idempotent across ticks.
     */
    public int computeEffectivePriority(Process p, int currentTime) {
        if (p == null) {
            throw new IllegalArgumentException("process must not be null");
        }
        int boost = computeAgingBoost(p, currentTime);
        return Math.max(minPriority, p.initialPriority - boost);
    }

    /**
     * Updates effectivePriority for all waiting processes in Tier 2 and returns
     * formatted aging trace lines.
     */
    public List<String> applyAging(ACSTier2 tier2, int currentTime) {
        if (tier2 == null) {
            return Collections.emptyList();
        }
        return applyAging(tier2.getQueue(), currentTime);
    }

    /**
     * Updates effectivePriority for all processes in the given Tier 2 queue and
     * returns formatted aging trace lines.
     */
    public List<String> applyAging(List<Process> tier2Queue, int currentTime) {
        if (tier2Queue == null || tier2Queue.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> logs = new ArrayList<>();
        for (Process p : tier2Queue) {
            if (p == null) {
                continue;
            }
            int waitTime = getTier2WaitTime(p, currentTime);
            int boost = waitTime / agingInterval;
            int updatedPriority = Math.max(minPriority, p.initialPriority - boost);
            p.effectivePriority = updatedPriority;

            if (boost > 0) {
                logs.add(String.format(
                    "%s: Waited from T=%d to T=%d (%d units). Aging: floor(%d/%d) = %d. Effective Priority = %d - %d = %d",
                    p.id, p.tier2EnterTime, currentTime, waitTime,
                    waitTime, agingInterval, boost,
                    p.initialPriority, boost, updatedPriority
                ));
            } else {
                logs.add(String.format(
                    "%s: Waited from T=%d to T=%d (%d units). Effective Priority = %d",
                    p.id, p.tier2EnterTime, currentTime, waitTime, updatedPriority
                ));
            }
        }
        return logs;
    }

    /**
     * Applies aging to Tier 2 and returns a map of Process ID to updated
     * effectivePriority in queue order.
     */
    public Map<String, Integer> updateAndReturnPriorities(ACSTier2 tier2, int currentTime) {
        if (tier2 == null) {
            return Collections.emptyMap();
        }
        return updateAndReturnPriorities(tier2.getQueue(), currentTime);
    }

    /**
     * Applies aging to the given Tier 2 queue and returns a map of Process ID to
     * updated effectivePriority in queue order.
     */
    public Map<String, Integer> updateAndReturnPriorities(List<Process> tier2Queue, int currentTime) {
        Map<String, Integer> updatedPriorities = new LinkedHashMap<>();
        if (tier2Queue == null || tier2Queue.isEmpty()) {
            return updatedPriorities;
        }
        applyAging(tier2Queue, currentTime);
        for (Process p : tier2Queue) {
            if (p != null) {
                updatedPriorities.put(p.id, p.effectivePriority);
            }
        }
        return updatedPriorities;
    }

    /**
     * Applies aging and returns a new list ordered by effectivePriority ascending,
     * breaking ties by earliest Tier 2 entry time (longest wait first), then arrivalTime.
     */
    public List<Process> getAgedOrder(List<Process> tier2Queue, int currentTime) {
        if (tier2Queue == null || tier2Queue.isEmpty()) {
            return Collections.emptyList();
        }
        applyAging(tier2Queue, currentTime);
        List<Process> ordered = new ArrayList<>();
        for (Process p : tier2Queue) {
            if (p != null) {
                ordered.add(p);
            }
        }
        ordered.sort((a, b) -> {
            int byPriority = Integer.compare(a.effectivePriority, b.effectivePriority);
            if (byPriority != 0) {
                return byPriority;
            }
            int byTier2Enter = Integer.compare(a.tier2EnterTime, b.tier2EnterTime);
            if (byTier2Enter != 0) {
                return byTier2Enter;
            }
            int byArrival = Integer.compare(a.arrivalTime, b.arrivalTime);
            if (byArrival != 0) {
                return byArrival;
            }
            return a.id.compareTo(b.id);
        });
        return ordered;
    }

    public static List<String> updateTier2Priorities(ACSTier2 tier2, int currentTime) {
        return new AgingManager().applyAging(tier2, currentTime);
    }

    public static List<String> updateTier2Priorities(List<Process> tier2Queue, int currentTime) {
        return new AgingManager().applyAging(tier2Queue, currentTime);
    }
}

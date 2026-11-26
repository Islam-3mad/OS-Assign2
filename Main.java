import java.util.*;

public class Main {
    // 1. Simulation Clock, Context Switching, and Timeline Management
    public static int currentTime = 0;
    public static int contextSwitchTime = 0;
    public static List<String> timeline = new ArrayList<>();

    public static void logStep(String event, List<Process> tier1, List<Process> tier2) {
        String t1 = (tier1 == null || tier1.isEmpty()) ? "[Empty]" : tier1.toString();
        String t2 = (tier2 == null) ? "" : " | Tier 2: " + (tier2.isEmpty() ? "[Empty]" : tier2.toString());
        timeline.add(String.format("Time %-3d | %-35s | Tier 1: %s%s", currentTime, event, t1, t2));
    }

    public static void applyContextSwitch(Process from, Process to, List<Process> tier1, List<Process> tier2) {
        for (int i = 0; i < contextSwitchTime; i++) {
            String msg = (from != null && to != null)
                    ? "Context switch saves " + from.id + " and loads " + to.id
                    : (to != null ? "Context switch loads " + to.id : "Context switch");
            logStep(msg, tier1, tier2);
            currentTime++;
        }
    }

    // 2. Metrics and Averages Calculation
    public static void printMetrics(List<Process> processes) {
        double totalWT = 0, totalTAT = 0, totalRT = 0;

        System.out.println("\nProcess | Arrival | Finish | Turnaround | Waiting | Response");
        System.out.println("------------------------------------------------------------");
        for (Process p : processes) {
            p.turnaroundTime = p.finishTime - p.arrivalTime;
            p.waitingTime = p.turnaroundTime - p.burstTime;
            p.responseTime = (p.startTime != -1) ? (p.startTime - p.arrivalTime) : 0;

            totalTAT += p.turnaroundTime;
            totalWT += p.waitingTime;
            totalRT += p.responseTime;

            System.out.printf("%-7s | %-7d | %-6d | %-10d | %-7d | %-8d%n",
                    p.id, p.arrivalTime, p.finishTime, p.turnaroundTime, p.waitingTime, p.responseTime);
        }

        int n = processes.size();
        System.out.println("------------------------------------------------------------");
        System.out.printf("Average Turnaround Time: %.2f%n", totalTAT / n);
        System.out.printf("Average Waiting Time   : %.2f%n", totalWT / n);
        System.out.printf("Average Response Time  : %.2f%n", totalRT / n);
    }

    public static void printTimeline() {
        System.out.println("\n=== Timeline ===");
        for (String step : timeline) {
            System.out.println(step);
        }
    }

    // 3. User Input, Algorithm Selection, and Team Integration
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        List<Process> processes = new ArrayList<>();

        System.out.print("Enter number of processes: ");
        int n = sc.nextInt();
        for (int i = 0; i < n; i++) {
            System.out.print("ID, Arrival, Burst, Priority: ");
            processes.add(new Process(sc.next(), sc.nextInt(), sc.nextInt(), sc.nextInt()));
        }

        System.out.print("Enter Context Switch time (CS): ");
        contextSwitchTime = sc.nextInt();

        System.out.println("Choose Algorithm: 1) Preemptive SJF   2) ACS");
        int choice = sc.nextInt();

        if (choice == 1) {
              SJF.run(processes);
            // Call Member 1's Preemptive SJF implementation here
            // SJF.run(processes);
        } else {
            System.out.print("Enter Quantum (Q): ");
            int quantum = sc.nextInt();
            
            // Call Members 2, 3, and 4's ACS implementation here
            // ACS.run(processes, quantum);
             ACSTier1.run(processes, quantum);
        }

        printTimeline();
        printMetrics(processes);
    }
}

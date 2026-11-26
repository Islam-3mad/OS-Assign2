
import java.util.*;

public class ACSTier1 {

    private Queue<Process> queue = new LinkedList<>();

    
    public void add(Process p) {
        queue.add(p);
    }


    public Process pollNext() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }


    public List<Process> getQueue() {
        return new ArrayList<>(queue);
    }

 
    public int executeNextQuantum(
            int quantum,
            int currentTime,
            ACSTier2 tier2) {

        Process p = pollNext();

       
        if (p == null) {
            return currentTime;
        }

        int executedTime = 0;

        
        if (p.startTime == -1) {
            p.startTime = currentTime;
        }

        while (executedTime < quantum && p.remainingTime > 0) {

            p.remainingTime--;

            currentTime++;
            executedTime++;
        }

   
        if (p.remainingTime == 0) {

            p.finishTime = currentTime;

        } else {

           
            demoteIfNeeded(
                    p,
                    executedTime,
                    quantum,
                    tier2,
                    currentTime
            );
        }

        return currentTime;
    }

    
    public void demoteIfNeeded(
            Process p,
            int executedTime,
            int quantum,
            ACSTier2 tier2,
            int currentTime) {

        if (executedTime == quantum && p.remainingTime > 0) {
            tier2.add(p, currentTime);
        }
    }

    
}

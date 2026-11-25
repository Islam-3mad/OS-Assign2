import java.util.*;

/**
 * Verification tests for Member 4 (AgingManager):
 * - Assignment PDF Test Case 2 exact aging milestones (T=11 and T=15)
 * - Step-by-step idempotency across consecutive clock ticks
 * - Long-wait starvation prevention and priority floor clamping
 * - Post-aging priority ties (different initial priorities and floor ties)
 * - Edge cases and custom configuration
 */
public class AgingManagerTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            failed++;
            System.err.println("FAIL: " + message + " | expected=" + expected + ", actual=" + actual);
        } else {
            passed++;
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            failed++;
            System.err.println("FAIL: " + message);
        } else {
            passed++;
        }
    }

    private static void testPdfTestCase2() {
        Process p1 = new Process("P1", 0, 5, 4);
        Process p2 = new Process("P2", 1, 6, 2);
        Process p3 = new Process("P3", 4, 4, 3);

        p1.remainingTime = 2;
        p2.remainingTime = 3;
        p3.remainingTime = 1;

        ACSTier2 tier2 = new ACSTier2();
        tier2.add(p1, 3);
        tier2.add(p2, 7);
        tier2.add(p3, 11);

        AgingManager aging = new AgingManager();
        Map<String, Integer> at11 = aging.updateAndReturnPriorities(tier2, 11);

        assertEquals(8, AgingManager.getTier2WaitTime(p1, 11), "TestCase2 T=11 P1 wait time");
        assertEquals(2, at11.get("P1"), "TestCase2 T=11 P1 effectivePriority");
        assertEquals(4, AgingManager.getTier2WaitTime(p2, 11), "TestCase2 T=11 P2 wait time");
        assertEquals(1, at11.get("P2"), "TestCase2 T=11 P2 effectivePriority");
        assertEquals(0, AgingManager.getTier2WaitTime(p3, 11), "TestCase2 T=11 P3 wait time");
        assertEquals(3, at11.get("P3"), "TestCase2 T=11 P3 effectivePriority");

        Process pickedAt11 = tier2.pickNext(11);
        assertEquals("P2", pickedAt11.id, "TestCase2 T=11 Tier2 picks P2");

        // Finish P2 (3 remaining units: T=11, 12, 13 -> finishes at 14; CS=1 loads next at 15)
        assertTrue(!tier2.runOneUnit(11), "P2 unit 1 not finished");
        assertTrue(!tier2.runOneUnit(12), "P2 unit 2 not finished");
        assertTrue(tier2.runOneUnit(13), "P2 finishes after 3rd unit");

        Map<String, Integer> at15 = aging.updateAndReturnPriorities(tier2, 15);
        assertEquals(12, AgingManager.getTier2WaitTime(p1, 15), "TestCase2 T=15 P1 wait time");
        assertEquals(1, at15.get("P1"), "TestCase2 T=15 P1 effectivePriority");
        assertEquals(4, AgingManager.getTier2WaitTime(p3, 15), "TestCase2 T=15 P3 wait time");
        assertEquals(2, at15.get("P3"), "TestCase2 T=15 P3 effectivePriority");

        Process pickedAt15 = tier2.pickNext(15);
        assertEquals("P1", pickedAt15.id, "TestCase2 T=15 Tier2 picks P1");
    }

    private static void testStepByStepIdempotency() {
        Process p1 = new Process("P1", 0, 5, 4);
        Process p2 = new Process("P2", 1, 6, 2);
        Process p3 = new Process("P3", 4, 4, 3);

        ACSTier2 tier2 = new ACSTier2();
        tier2.add(p1, 3);

        AgingManager aging = new AgingManager();
        for (int t = 3; t <= 15; t++) {
            if (t == 7) {
                tier2.add(p2, 7);
            }
            if (t == 11) {
                tier2.add(p3, 11);
            }
            aging.applyAging(tier2, t);
            if (t == 11) {
                assertEquals(2, p1.effectivePriority, "Idempotent T=11 P1 effectivePriority");
                assertEquals(1, p2.effectivePriority, "Idempotent T=11 P2 effectivePriority");
                assertEquals(3, p3.effectivePriority, "Idempotent T=11 P3 effectivePriority");
            }
            if (t == 15) {
                assertEquals(1, p1.effectivePriority, "Idempotent T=15 P1 effectivePriority");
                assertEquals(0, p2.effectivePriority, "Idempotent T=15 P2 effectivePriority");
                assertEquals(2, p3.effectivePriority, "Idempotent T=15 P3 effectivePriority");
            }
        }
    }

    private static void testLongWaitPreventsStarvation() {
        AgingManager aging = new AgingManager();
        ACSTier2 tier2 = new ACSTier2();

        Process lowPriority = new Process("P_LOW", 0, 10, 10);
        tier2.add(lowPriority, 0);

        // Simulate steady arrivals of priority-2 jobs every 4 ticks until P_LOW ages below priority 2
        int currentTime = 0;
        boolean lowPrioritySelected = false;
        for (int batch = 1; batch <= 12; batch++) {
            Process highPriority = new Process("P_HIGH_" + batch, currentTime, 4, 2);
            tier2.add(highPriority, currentTime);

            aging.applyAging(tier2, currentTime);
            Process chosen = tier2.pickNext(currentTime);
            if ("P_LOW".equals(chosen.id)) {
                lowPrioritySelected = true;
                assertEquals(32, currentTime, "P_LOW reaches priority 2 at T=32 and wins tie by earlier entry");
                assertEquals(2, chosen.effectivePriority, "P_LOW effectivePriority at selection");
                break;
            }
            for (int u = 0; u < 4; u++) {
                tier2.runOneUnit(currentTime + u);
            }
            currentTime += 4;
        }
        assertTrue(lowPrioritySelected, "Long-waiting low-priority process must not starve");

        // Verify a long-waiting process strictly overtakes a higher-initial-priority process
        Process longWaiter = new Process("P_LONG", 0, 5, 10);
        longWaiter.tier2EnterTime = 4;
        Process freshHigh = new Process("P_FRESH_HIGH", 38, 5, 2);
        freshHigh.tier2EnterTime = 40;
        List<Process> reversedOrder = Arrays.asList(freshHigh, longWaiter);
        aging.applyAging(reversedOrder, 40);
        assertEquals(1, longWaiter.effectivePriority, "P_LONG aged from 10 to 1 after 36 units");
        assertEquals(2, freshHigh.effectivePriority, "P_FRESH_HIGH stays at 2 after 0 units");
        assertEquals("P_LONG", aging.getAgedOrder(reversedOrder, 40).get(0).id,
            "P_LONG strictly overtakes P_FRESH_HIGH regardless of queue position");

        // Verify extreme wait clamps at minPriority (0) and never turns negative
        Process extremeWait = new Process("P_EXTREME", 0, 5, 6);
        extremeWait.tier2EnterTime = 0;
        List<Process> list = Collections.singletonList(extremeWait);
        aging.applyAging(list, 100);
        assertEquals(0, extremeWait.effectivePriority, "Extreme wait clamps at DEFAULT_MIN_PRIORITY (0)");
    }

    private static void testPriorityTiesAfterAging() {
        AgingManager aging = new AgingManager();

        // Case A: P_OLDER (initial 4, entered T=3) vs P_NEWER (initial 3, entered T=7) at T=11 -> both become 2
        Process older = new Process("P_OLDER", 0, 4, 4);
        Process newer = new Process("P_NEWER", 2, 4, 3);

        ACSTier2 tier2A = new ACSTier2();
        tier2A.add(older, 3);
        tier2A.add(newer, 7);

        aging.applyAging(tier2A, 11);
        assertEquals(2, older.effectivePriority, "Tie Case A older effectivePriority");
        assertEquals(2, newer.effectivePriority, "Tie Case A newer effectivePriority");

        List<Process> orderedA = aging.getAgedOrder(tier2A.getQueue(), 11);
        assertEquals("P_OLDER", orderedA.get(0).id, "Tie Case A getAgedOrder favors longer Tier 2 waiter");
        assertEquals("P_OLDER", tier2A.pickNext(11).id, "Tie Case A ACSTier2.pickNext favors P_OLDER");

        // Case B: Two processes both hit floor (0) after very long wait -> earlier Tier 2 entry wins
        Process floorFirst = new Process("P_FLOOR_1", 1, 3, 5);
        Process floorSecond = new Process("P_FLOOR_2", 0, 3, 3);

        ACSTier2 tier2B = new ACSTier2();
        tier2B.add(floorFirst, 2);
        tier2B.add(floorSecond, 10);

        aging.applyAging(tier2B, 40);
        assertEquals(0, floorFirst.effectivePriority, "Tie Case B floorFirst clamped to 0");
        assertEquals(0, floorSecond.effectivePriority, "Tie Case B floorSecond clamped to 0");

        List<Process> orderedB = aging.getAgedOrder(tier2B.getQueue(), 40);
        assertEquals("P_FLOOR_1", orderedB.get(0).id, "Tie Case B getAgedOrder breaks floor tie by tier2EnterTime");
        assertEquals("P_FLOOR_1", tier2B.pickNext(40).id, "Tie Case B ACSTier2.pickNext picks earlier waiter");
    }

    private static void testEdgeCasesAndCustomConfig() {
        AgingManager defaultAging = new AgingManager();
        assertTrue(defaultAging.applyAging((ACSTier2) null, 10).isEmpty(), "Null ACSTier2 returns empty logs");
        assertTrue(defaultAging.applyAging(Collections.emptyList(), 10).isEmpty(), "Empty list returns empty logs");
        assertTrue(defaultAging.updateAndReturnPriorities((ACSTier2) null, 10).isEmpty(), "Null ACSTier2 returns empty map");

        Process notInTier2 = new Process("P_NONE", 0, 5, 4);
        assertEquals(0, AgingManager.getTier2WaitTime(notInTier2, 10), "tier2EnterTime=-1 yields 0 wait time");
        assertEquals(4, defaultAging.computeEffectivePriority(notInTier2, 10), "Unentered process keeps initialPriority");

        AgingManager customAging = new AgingManager(2, 1);
        Process customProc = new Process("P_CUSTOM", 0, 5, 4);
        customProc.tier2EnterTime = 2;
        assertEquals(2, customAging.computeEffectivePriority(customProc, 6), "Custom interval=2 wait=4 gives 4-2=2");
        assertEquals(1, customAging.computeEffectivePriority(customProc, 20), "Custom minPriority=1 clamps at 1");

        boolean threw = false;
        try {
            new AgingManager(0, 0);
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        assertTrue(threw, "agingInterval <= 0 throws IllegalArgumentException");
    }

    public static void main(String[] args) {
        testPdfTestCase2();
        testStepByStepIdempotency();
        testLongWaitPreventsStarvation();
        testPriorityTiesAfterAging();
        testEdgeCasesAndCustomConfig();

        System.out.println("AgingManagerTest Summary: " + passed + " passed, " + failed + " failed.");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

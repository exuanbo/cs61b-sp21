package timingtest;

import deque.ArrayDeque;
import deque.Deque;
import edu.princeton.cs.algs4.Stopwatch;

public class Timing {
    public static void addFirst(Deque<Integer> deque) {
        ArrayDeque<Integer> Ns = new ArrayDeque<>();
        ArrayDeque<Double> times = new ArrayDeque<>();
        ArrayDeque<Integer> opCounts = new ArrayDeque<>();
        int dequeLength = 1000;
        for (int p = 0; p < 15; p++) {
            if (p > 0) {
                dequeLength *= 2;
            }
            Ns.addLast(dequeLength);
            Stopwatch sw = new Stopwatch();
            for (int i = 0; i < dequeLength; i++) {
                deque.addFirst(1);
            }
            double timeInSeconds = sw.elapsedTime();
            times.addLast(timeInSeconds);
            opCounts.addLast(dequeLength);
        }
        printTable(String.format("%s addFirst", deque.getClass()), Ns, times, opCounts);
    }

    private static void printTable(String opName, ArrayDeque<Integer> Ns, ArrayDeque<Double> times, ArrayDeque<Integer> opCounts) {
        System.out.println(opName + "\n");
        System.out.printf("%12s %12s %12s %12s\n", "N", "time (s)", "# ops", "microsec/op");
        System.out.print("------------------------------------------------------------\n");
        for (int i = 0; i < Ns.size(); i += 1) {
            int N = Ns.get(i);
            double time = times.get(i);
            int opCount = opCounts.get(i);
            double timePerOp = time / opCount * 1e6;
            System.out.printf("%12d %12.2f %12d %12.2f\n", N, time, opCount, timePerOp);
        }
    }
}

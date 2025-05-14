package MonteCarloPI;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Random;

public class MonteCarloPi {
    static final long NUM_POINTS = 50_000_000L;
    static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // Without Threads
        System.out.println("Single threaded calculation started: ");
        long startTime = System.nanoTime();
        double piWithoutThreads = estimatePiWithoutThreads(NUM_POINTS);
        long endTime = System.nanoTime();
        System.out.println("Monte Carlo Pi Approximation (single thread): " + piWithoutThreads);
        System.out.println("Time taken (single threads): " + (endTime - startTime) / 1_000_000 + " ms");

        // With Threads
        System.out.printf("Multi threaded calculation started: (your device has %d logical threads)\n", NUM_THREADS);
        startTime = System.nanoTime();
        double piWithThreads = estimatePiWithThreads(NUM_POINTS, NUM_THREADS);
        endTime = System.nanoTime();
        System.out.println("Monte Carlo Pi Approximation (Multi-threaded): " + piWithThreads);
        System.out.println("Time taken (Multi-threaded): " + (endTime - startTime) / 1_000_000 + " ms");
    }

    // Monte Carlo Pi Approximation without threads
    public static double estimatePiWithoutThreads(long numPoints) {
        Random rand = new Random();
        long pointsInsideCircle = 0;

        for (long i = 0; i < numPoints; i++) {
            double x = rand.nextDouble() * 2 - 1; // [-1, 1]
            double y = rand.nextDouble() * 2 - 1; // [-1, 1]

            if (x * x + y * y <= 1) {
                pointsInsideCircle++;
            }
        }

        return 4.0 * pointsInsideCircle / numPoints;
    }

    // Monte Carlo Pi Approximation with threads
    public static double estimatePiWithThreads(long numPoints, int numThreads)
            throws InterruptedException, ExecutionException {

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicLong totalPointsInsideCircle = new AtomicLong(0);
        long pointsPerThread = numPoints / numThreads;

        // Submit tasks to executor
        for (int i = 0; i < numThreads; i++) {
            final long threadPoints = (i == numThreads - 1) ?
                    numPoints - (pointsPerThread * (numThreads - 1)) :
                    pointsPerThread;

            executor.submit(() -> {
                Random rand = new Random();
                long localPointsInside = 0;

                for (long j = 0; j < threadPoints; j++) {
                    double x = rand.nextDouble() * 2 - 1;
                    double y = rand.nextDouble() * 2 - 1;

                    if (x * x + y * y <= 1) {
                        localPointsInside++;
                    }
                }

                totalPointsInsideCircle.addAndGet(localPointsInside);
            });
        }

        // Shutdown executor and wait for termination
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        return 4.0 * totalPointsInsideCircle.get() / numPoints;
    }
}
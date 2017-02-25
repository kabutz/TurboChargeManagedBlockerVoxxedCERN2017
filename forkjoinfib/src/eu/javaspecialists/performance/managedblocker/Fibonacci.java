package eu.javaspecialists.performance.managedblocker;

import eu.javaspecialists.performance.math.*;

import java.util.*;
import java.util.concurrent.*;

public class Fibonacci {
    public BigInteger f(int n) {
        Map<Integer, ForkJoinTask<BigInteger>> cache = new ConcurrentHashMap<>();
        cache.put(0, ForkJoinTask.adapt(() ->BigInteger.ZERO).fork());
        cache.put(1, ForkJoinTask.adapt(() -> BigInteger.ONE).fork());
        return f(n, cache);
    }

    public BigInteger f(int n, Map<Integer, ForkJoinTask<BigInteger>> cache) {
        ForkJoinTask<BigInteger> computation = new RecursiveTask<BigInteger>() {
            @Override
            protected BigInteger compute() {
                int half = (n + 1) / 2;
                ForkJoinTask<BigInteger> f0_task = new RecursiveTask<BigInteger>() {
                    protected BigInteger compute() {
                        return f(half - 1, cache);
                    }
                }.fork();
                BigInteger f1 = f(half, cache);
                BigInteger f0 = f0_task.join();

                BigInteger result = (n % 2 == 1)
                        ? f0.multiply(f0).add(f1.multiply(f1))
                        : f0.shiftLeft(1).add(f1).multiply(f1);

                return result;
            }
        };

        ForkJoinTask<BigInteger> alreadyComputing = cache.putIfAbsent(n, computation);

        if (alreadyComputing == null) {
            return computation.invoke();
        }
        else {
            try {
                FJTBlocker blocker = new FJTBlocker(alreadyComputing);
                ForkJoinPool.managedBlock(blocker);
                return blocker.result;
            } catch (InterruptedException e) {
                throw new CancellationException("interrupted");
            }
        }
    }

    private class FJTBlocker implements ForkJoinPool.ManagedBlocker {
        private final ForkJoinTask<BigInteger> alreadyComputing;
        private BigInteger result;

        private FJTBlocker(ForkJoinTask<BigInteger> alreadyComputing) {
            this.alreadyComputing = alreadyComputing;
        }

        public boolean block() throws InterruptedException {
            result = alreadyComputing.join();
            return true;
        }

        public boolean isReleasable() {
            if (alreadyComputing.isDone()) {
                result = alreadyComputing.join();
                return true;
            } else {
                return false;
            }
        }
    }
}
package eu.javaspecialists.performance.managedblocker;

import eu.javaspecialists.performance.math.*;

import java.util.*;
import java.util.concurrent.*;

public class Fibonacci {
    public BigInteger f(int n) {
        Map<Integer, BigInteger> cache = new ConcurrentHashMap<>();
        cache.put(0, BigInteger.ZERO);
        cache.put(1, BigInteger.ONE);
        return f(n, cache);
    }
    public BigInteger f(int n, Map<Integer, BigInteger> cache) {
        BigInteger result = cache.get(n);
        if (result== null) {

            int half = (n + 1) / 2;

            ForkJoinTask<BigInteger> f0_task = new RecursiveTask<BigInteger>() {
                protected BigInteger compute() {
                    return f(half - 1, cache);
                }
            };
            f0_task.fork();
            BigInteger f1 = f(half, cache);
            BigInteger f0 = f0_task.join();

            long time = n > 1000 ? System.currentTimeMillis() : 0;
            try {
                if (n % 2 == 1) {
                    result = f0.multiply(f0).add(f1.multiply(f1));
                } else {
                    result = f0.shiftLeft(1).add(f1).multiply(f1);
                }
            } finally {
                time = n > 1000 ? System.currentTimeMillis() - time : 0;
                if (time > 20) {
                    System.out.println("f(" + n + ") = " + time);
                }
            }
            cache.put(n, result);
        }
        return result;
    }
}

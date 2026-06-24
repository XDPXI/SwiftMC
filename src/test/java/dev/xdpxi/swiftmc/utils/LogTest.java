package dev.xdpxi.swiftmc.utils;

import dev.xdpxi.swiftmc.Main;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogTest {

    @BeforeAll
    static void setConfig() {
        Main.config = new Config();
    }

    @Test
    void concurrentLogging_fromManyThreads_doesNotThrow() throws InterruptedException {
        int threadCount = 8;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicBoolean failed = new AtomicBoolean(false);

        for (int i = 0; i < threadCount; i++) {
            int idx = i;
            Thread t = new Thread(() -> {
                try {
                    for (int j = 0; j < 50; j++) {
                        Log.info("thread %d message %d", idx, j);
                    }
                } catch (Throwable e) {
                    failed.set(true);
                } finally {
                    latch.countDown();
                }
            });
            t.start();
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "logging threads did not finish in time");
        assertFalse(failed.get(), "a logging call threw an exception");
    }

    @Test
    void info_returnsWithoutBlockingOnDiskIO() {
        long start = System.nanoTime();
        for (int i = 0; i < 2000; i++) {
            Log.info("perf check message %d", i);
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertTrue(elapsedMs < 2000, "logging 2000 messages took " + elapsedMs + "ms, expected non-blocking enqueue");
    }
}

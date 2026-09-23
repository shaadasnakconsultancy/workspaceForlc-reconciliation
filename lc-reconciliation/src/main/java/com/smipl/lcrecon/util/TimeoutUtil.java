package com.smipl.lcrecon.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Runs a blocking, non-interruptible operation (PDFBox parsing, javax.mail Transport.send, etc.)
 * on a short-lived daemon thread and enforces a hard wall-clock timeout on it.
 * <p>
 * Background: several third-party calls in this codebase (PDFBox text extraction, SMTP send via
 * javax.mail) can block indefinitely on malformed input or an unreachable/firewalled host, and
 * do NOT respond to Thread.interrupt(). Since job execution runs on a small fixed thread pool
 * (see JobManager, pool size 3), a single such hang permanently consumes a worker thread and the
 * job sits at "RUNNING" forever with no error logged. Wrapping the call here guarantees the
 * calling (job-runner) thread always regains control after the timeout, even though the runaway
 * worker thread itself may still be blocked in the background (it is abandoned as a daemon thread
 * and will be reclaimed by the JVM/GC once the underlying call eventually returns or the process
 * restarts).
 */
public final class TimeoutUtil {
    private static final Logger logger = LoggerFactory.getLogger(TimeoutUtil.class);

    private TimeoutUtil() {
    }

    /**
     * Runs {@code task} with a hard timeout. If the task does not complete in time, logs a
     * warning and returns {@code fallback} instead of blocking the caller any further.
     *
     * @param task       the blocking operation to run
     * @param timeout    timeout value
     * @param unit       timeout unit
     * @param fallback   value to return if the task times out or throws
     * @param taskName   short label used in log messages (e.g. "Invoice HSN extraction")
     */
    public static <T> T runWithTimeout(Callable<T> task, long timeout, TimeUnit unit, T fallback, String taskName) {
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "timeout-guard-" + taskName.replaceAll("\\s+", "-"));
            t.setDaemon(true); // never blocks JVM/Tomcat shutdown even if the task itself never returns
            return t;
        });
        Future<T> future = executor.submit(task);
        try {
            return future.get(timeout, unit);
        } catch (TimeoutException te) {
            logger.warn("{} timed out after {} {} - continuing without result (fallback applied)",
                    taskName, timeout, unit.name().toLowerCase());
            future.cancel(true); // best-effort; underlying blocking call may not actually stop
            return fallback;
        } catch (Exception e) {
            logger.error("{} failed: {}", taskName, e.getMessage());
            return fallback;
        } finally {
            executor.shutdownNow();
        }
    }
}

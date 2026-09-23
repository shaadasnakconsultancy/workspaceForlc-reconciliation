package com.smipl.lcrecon.job;

import com.smipl.lcrecon.dao.JobDao;
import com.smipl.lcrecon.dao.SettingsDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class JobManager {
    private static final Logger logger = LoggerFactory.getLogger(JobManager.class);

    private static final int WORKER_THREADS = 3;
    /** Fallback wall-clock limit per job when job_timeout_minutes is not configured. */
    private static final int DEFAULT_JOB_TIMEOUT_MINUTES = 120;

    private final ExecutorService executor;
    /** Separate from the worker pool so the watchdog can still fire when all workers are busy. */
    private final ScheduledExecutorService watchdog;
    private final ConcurrentHashMap<Long, JobHandle> runningJobs = new ConcurrentHashMap<>();

    /**
     * Everything needed to stop a job. The worker thread is recorded so an abort can interrupt it
     * directly, and {@code abortRequested} is a flag the runner polls - a library that swallows
     * InterruptedException clears the interrupt bit, but it cannot clear this.
     */
    private static class JobHandle {
        volatile Future<?> future;
        volatile Thread workerThread;
        volatile ScheduledFuture<?> timeoutTask;
        final AtomicBoolean abortRequested = new AtomicBoolean(false);
    }

    public JobManager() {
        this.executor = Executors.newFixedThreadPool(WORKER_THREADS, namedFactory("lc-job-worker"));
        this.watchdog = Executors.newScheduledThreadPool(1, namedFactory("lc-job-watchdog"));
        logger.info("JobManager initialized with {} worker threads", WORKER_THREADS);
    }

    private static ThreadFactory namedFactory(final String prefix) {
        return new ThreadFactory() {
            private int count = 0;
            @Override
            public synchronized Thread newThread(Runnable r) {
                Thread t = new Thread(r, prefix + "-" + (++count));
                t.setDaemon(true); // never block Tomcat shutdown on a wedged job
                return t;
            }
        };
    }

    public void submitJob(long jobId, ServletContext servletContext) {
        logger.info("Submitting reconciliation job: {}", jobId);

        final JobHandle handle = new JobHandle();
        runningJobs.put(jobId, handle);

        final ReconciliationJobRunner runner = new ReconciliationJobRunner(jobId, servletContext);
        handle.future = executor.submit(new Runnable() {
            @Override
            public void run() {
                handle.workerThread = Thread.currentThread();
                try {
                    runner.run();
                } catch (Throwable t) {
                    logger.error("Job {} terminated unexpectedly", jobId, t);
                } finally {
                    // Cleanup lives here rather than in a second pooled task. The old code submitted
                    // a watcher into this same pool, so every job consumed 2 of 3 threads and two
                    // stuck jobs starved the pool completely.
                    cancelTimeout(handle);
                    runningJobs.remove(jobId);
                }
            }
        });

        scheduleTimeout(jobId, servletContext, handle);
    }

    /**
     * Hard wall-clock limit. Without this a job that wedges in a way no timeout covers would hold
     * its worker thread until Tomcat restarts.
     */
    private void scheduleTimeout(final long jobId, final ServletContext ctx, final JobHandle handle) {
        final int minutes = resolveTimeoutMinutes(ctx);
        handle.timeoutTask = watchdog.schedule(new Runnable() {
            @Override
            public void run() {
                if (!runningJobs.containsKey(jobId)) return; // already finished
                logger.warn("Job {} exceeded the {}-minute limit; forcing abort", jobId, minutes);
                try {
                    JobDao jobDao = (JobDao) ctx.getAttribute("jobDao");
                    if (jobDao != null) {
                        jobDao.addLog(jobId, "ERROR", "Job exceeded the " + minutes
                                + "-minute limit and was stopped automatically.");
                        jobDao.updateCompleted(jobId, "FAILED", null, null, null, null,
                                "Timed out after " + minutes + " minutes");
                    }
                } catch (Exception e) {
                    logger.error("Failed to record timeout for job {}", jobId, e);
                }
                stop(jobId);
            }
        }, minutes, TimeUnit.MINUTES);
    }

    private int resolveTimeoutMinutes(ServletContext ctx) {
        try {
            SettingsDao settingsDao = (SettingsDao) ctx.getAttribute("settingsDao");
            if (settingsDao != null) {
                String value = settingsDao.getSetting("job_timeout_minutes");
                if (value != null && !value.trim().isEmpty()) {
                    int minutes = Integer.parseInt(value.trim());
                    if (minutes > 0) return minutes;
                }
            }
        } catch (Exception e) {
            logger.warn("Could not read job_timeout_minutes, using default: {}", e.getMessage());
        }
        return DEFAULT_JOB_TIMEOUT_MINUTES;
    }

    /**
     * Ask a job to stop. Returns true when the job was running and a stop was issued.
     *
     * The runner polls {@link #isAbortRequested} at each step, so a cooperative abort happens as
     * soon as the current step ends. The interrupt handles threads parked in a sleep or wait; a
     * thread blocked in a socket read ignores it, which is why every outbound call now carries a
     * timeout - the read gives up on its own and the runner then sees the abort flag.
     */
    public boolean abortJob(long jobId) {
        JobHandle handle = runningJobs.get(jobId);
        if (handle == null) {
            logger.warn("Job {} not found in running jobs or already completed", jobId);
            return false;
        }
        logger.info("Abort requested for job {}", jobId);
        return stop(jobId);
    }

    private boolean stop(long jobId) {
        JobHandle handle = runningJobs.get(jobId);
        if (handle == null) return false;

        handle.abortRequested.set(true);

        Thread worker = handle.workerThread;
        if (worker != null) {
            worker.interrupt();
        }
        if (handle.future != null) {
            handle.future.cancel(true);
        }
        cancelTimeout(handle);

        // Drop it from the registry immediately so the UI and isJobRunning() reflect the abort even
        // if the worker takes until its current socket timeout to actually unwind.
        runningJobs.remove(jobId);
        return true;
    }

    private void cancelTimeout(JobHandle handle) {
        ScheduledFuture<?> timeoutTask = handle.timeoutTask;
        if (timeoutTask != null) {
            timeoutTask.cancel(false);
        }
    }

    /** Polled by the runner between steps so an abort is honoured even if the interrupt was eaten. */
    public boolean isAbortRequested(long jobId) {
        JobHandle handle = runningJobs.get(jobId);
        return handle == null || handle.abortRequested.get();
    }

    public boolean isJobRunning(long jobId) {
        JobHandle handle = runningJobs.get(jobId);
        return handle != null && handle.future != null && !handle.future.isDone();
    }

    public void shutdown() {
        logger.info("Shutting down JobManager...");
        watchdog.shutdownNow();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("JobManager shut down");
    }
}

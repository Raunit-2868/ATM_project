package com.atm.service;

import com.atm.util.AppLogger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SessionManager - Manages user session with a 3-minute inactivity timeout.
 *
 * Uses a ScheduledThreadPoolExecutor running on a dedicated daemon thread.
 * The countdown can be reset on any user activity (resetTimer).
 * On expiry, fires the onSessionExpired callback (must dispatch to EDT if touching UI).
 *
 * Thread-safety: uses AtomicInteger/AtomicBoolean to eliminate race conditions.
 */
public class SessionManager {

    public static final int SESSION_TIMEOUT_SECONDS = 180; // 3 minutes

    private final ScheduledExecutorService scheduler;
    private volatile ScheduledFuture<?> countdownTask;

    private final Runnable onSessionExpired;
    private volatile Runnable onTickCallback;

    private final AtomicInteger remainingSeconds = new AtomicInteger(SESSION_TIMEOUT_SECONDS);
    private final AtomicBoolean active = new AtomicBoolean(false);

    public SessionManager(Runnable onSessionExpired) {
        this.onSessionExpired = onSessionExpired;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ATM-SessionTimer");
            t.setDaemon(true);
            return t;
        });
    }

    /** Starts the session countdown. */
    public void start() {
        active.set(true);
        remainingSeconds.set(SESSION_TIMEOUT_SECONDS);
        scheduleCountdown();
        AppLogger.info("Session started. Timeout: " + SESSION_TIMEOUT_SECONDS + "s");
    }

    /**
     * Resets the countdown back to the full timeout.
     * Call this on any user interaction to extend the session.
     */
    public synchronized void resetTimer() {
        if (!active.get()) return;
        cancelCurrentTask();
        remainingSeconds.set(SESSION_TIMEOUT_SECONDS);
        scheduleCountdown();
    }

    /** Permanently stops the session timer (call on logout or app close). */
    public void stop() {
        active.set(false);
        cancelCurrentTask();
        scheduler.shutdownNow();
        AppLogger.info("Session stopped.");
    }

    /** Returns seconds remaining before automatic logout. */
    public int getRemainingSeconds() {
        return remainingSeconds.get();
    }

    /**
     * Sets a callback that fires once per second with the remaining countdown.
     * Must dispatch UI updates to the EDT inside this callback.
     */
    public void setOnTickCallback(Runnable onTickCallback) {
        this.onTickCallback = onTickCallback;
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void cancelCurrentTask() {
        ScheduledFuture<?> task = countdownTask;
        if (task != null && !task.isDone()) {
            task.cancel(false);
        }
        // Purge cancelled tasks if the underlying impl supports it
        if (scheduler instanceof ScheduledThreadPoolExecutor stpe) {
            stpe.purge();
        }
    }

    private synchronized void scheduleCountdown() {
        countdownTask = scheduler.scheduleAtFixedRate(() -> {
            if (!active.get()) return;

            int secs = remainingSeconds.decrementAndGet();

            Runnable tick = onTickCallback;
            if (tick != null) {
                try { tick.run(); } catch (Exception ignored) {}
            }

            if (secs <= 0) {
                AppLogger.warn("Session expired due to inactivity.");
                active.set(false);
                cancelCurrentTask();
                try { onSessionExpired.run(); } catch (Exception ignored) {}
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
}

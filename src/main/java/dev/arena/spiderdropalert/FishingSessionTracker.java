package dev.arena.spiderdropalert;

/** Session-only efficiency and Great White festival statistics derived from local catch messages. */
final class FishingSessionTracker {
    static final long FESTIVAL_WINDOW_MILLIS = 61L * 60L * 1000L;
    private static final long MILLIS_PER_HOUR = 60L * 60L * 1000L;

    private long sessionStartedAt;
    private long trackedCatchCount;
    private long festivalStartedAt;
    private long currentFestivalGreatWhiteCount;
    private long bestFestivalGreatWhiteCount;

    void startSession(long now) {
        if (sessionStartedAt <= 0L) {
            sessionStartedAt = safeTimestamp(now);
        }
    }

    void recordTrackedCatch(long now) {
        startSession(now);
        trackedCatchCount = increment(trackedCatchCount);
    }

    void resetSession() {
        sessionStartedAt = 0L;
        trackedCatchCount = 0L;
    }

    long getSessionStartedAt() {
        return sessionStartedAt;
    }

    long getTrackedCatchCount() {
        return trackedCatchCount;
    }

    long getSessionElapsedMillis(long now) {
        if (sessionStartedAt <= 0L || now <= sessionStartedAt) {
            return 0L;
        }
        return now - sessionStartedAt;
    }

    double getTrackedCatchesPerHour(long now) {
        long elapsed = getSessionElapsedMillis(now);
        if (elapsed <= 0L || trackedCatchCount <= 0L) {
            return 0.0D;
        }
        return trackedCatchCount * (double) MILLIS_PER_HOUR / elapsed;
    }

    boolean recordGreatWhiteCatch(long now) {
        long safeNow = safeTimestamp(now);
        if (festivalStartedAt <= 0L || safeNow < festivalStartedAt
                || safeNow - festivalStartedAt >= FESTIVAL_WINDOW_MILLIS) {
            festivalStartedAt = safeNow;
            currentFestivalGreatWhiteCount = 0L;
        }
        currentFestivalGreatWhiteCount = increment(currentFestivalGreatWhiteCount);
        if (currentFestivalGreatWhiteCount > bestFestivalGreatWhiteCount) {
            bestFestivalGreatWhiteCount = currentFestivalGreatWhiteCount;
            return true;
        }
        return false;
    }

    long getCurrentFestivalGreatWhiteCount(long now) {
        if (festivalStartedAt <= 0L || now < festivalStartedAt
                || now - festivalStartedAt >= FESTIVAL_WINDOW_MILLIS) {
            return 0L;
        }
        return currentFestivalGreatWhiteCount;
    }

    void endFestival() {
        festivalStartedAt = 0L;
        currentFestivalGreatWhiteCount = 0L;
    }

    long getBestFestivalGreatWhiteCount() {
        return bestFestivalGreatWhiteCount;
    }

    void loadBestFestivalGreatWhiteCount(long count) {
        bestFestivalGreatWhiteCount = Math.max(0L, count);
    }

    void clear() {
        resetSession();
        festivalStartedAt = 0L;
        currentFestivalGreatWhiteCount = 0L;
        bestFestivalGreatWhiteCount = 0L;
    }

    private static long safeTimestamp(long now) {
        return Math.max(1L, now);
    }

    private static long increment(long value) {
        return value == Long.MAX_VALUE ? Long.MAX_VALUE : value + 1L;
    }
}

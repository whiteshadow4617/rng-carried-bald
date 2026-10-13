package dev.arena.spiderdropalert;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FishingSessionTrackerTest {
    @Test
    public void tracksEligibleCreatureCatchesAndComputesHourlyRate() {
        FishingSessionTracker tracker = new FishingSessionTracker();
        tracker.startSession(1000L);
        tracker.recordTrackedCatch(1000L);
        tracker.recordTrackedCatch(1801000L);

        assertEquals(2L, tracker.getTrackedCatchCount());
        assertEquals(1800000L, tracker.getSessionElapsedMillis(1801000L));
        assertEquals(4.0D, tracker.getTrackedCatchesPerHour(1801000L), 0.0001D);

        tracker.resetSession();
        assertEquals(0L, tracker.getTrackedCatchCount());
        assertEquals(0L, tracker.getSessionElapsedMillis(1801000L));
    }

    @Test
    public void keepsOnlyGreatWhiteFestivalCountAndRetainsBestAcrossFestivalWindows() {
        FishingSessionTracker tracker = new FishingSessionTracker();
        assertTrue(tracker.recordGreatWhiteCatch(1000L));
        assertEquals(1L, tracker.getCurrentFestivalGreatWhiteCount(1000L));
        assertTrue(tracker.recordGreatWhiteCatch(2000L));
        assertEquals(2L, tracker.getCurrentFestivalGreatWhiteCount(2000L));
        assertEquals(2L, tracker.getBestFestivalGreatWhiteCount());

        long afterWindow = 1000L + FishingSessionTracker.FESTIVAL_WINDOW_MILLIS;
        assertEquals(0L, tracker.getCurrentFestivalGreatWhiteCount(afterWindow));
        assertFalse(tracker.recordGreatWhiteCatch(afterWindow));
        assertEquals(1L, tracker.getCurrentFestivalGreatWhiteCount(afterWindow));
        assertEquals(2L, tracker.getBestFestivalGreatWhiteCount());
        tracker.endFestival();
        assertEquals(0L, tracker.getCurrentFestivalGreatWhiteCount(afterWindow));
        assertEquals(2L, tracker.getBestFestivalGreatWhiteCount());

        tracker.clear();
        assertEquals(0L, tracker.getCurrentFestivalGreatWhiteCount(afterWindow));
        assertEquals(0L, tracker.getBestFestivalGreatWhiteCount());
    }
}

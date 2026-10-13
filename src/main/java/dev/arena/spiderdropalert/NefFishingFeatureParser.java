package dev.arena.spiderdropalert;

import java.util.Locale;

/**
 * Chat and server-text recognizers for the Fakepixel fishing features adapted from
 * Not Enough Fakepixel by 2pb (LGPL-3.0-or-later).
 */
final class NefFishingFeatureParser {
    private NefFishingFeatureParser() {
    }

    static String detectLegendaryCatch(String message) {
        if (message == null) {
            return null;
        }
        LegendaryCreature trackedCreature = DropMessageParser.detectLegendaryCreature(message);
        if (trackedCreature != null) {
            return trackedCreature.displayName;
        }

        String plain = normalize(message);
        String upper = plain.toUpperCase(Locale.ROOT);
        if (upper.contains("THE WATER HYDRA HAS COME")) {
            return "Water Hydra";
        }
        if (upper.contains("AN ABYSSAL MINER BREAKS")) {
            return "Abyssal Miner";
        }
        if (upper.contains("THE SPIRIT OF A LONG LOST PHANTOM")) {
            return "Phantom Fisher";
        }
        if (upper.contains("THIS CAN'T BE! THE MANIFESTATION OF DEATH HIMSELF!")) {
            return "Grim Reaper";
        }
        if (upper.contains("WOAH! A PLHLEGBLAST APPEARED.")) {
            return "Plhlegblast";
        }
        if (upper.contains("WHAT IS THIS CREATURE!?")) {
            return "Yeti";
        }
        if (upper.contains("A REINDRAKE FORMS FROM THE DEPTHS.")) {
            return "Reindrake";
        }
        if (upper.contains("HIDE NO LONGER, A GREAT WHITE SHARK")) {
            return "Great White Shark";
        }
        if (upper.contains("YOU HEAR A MASSIVE RUMBLE AS")) {
            return "Thunder";
        }
        if (upper.contains("YOU HAVE ANGERED A LEGENDARY CREATURE.")) {
            return "Lord Jawbus";
        }
        return null;
    }

    static String detectTrophyFish(String message) {
        if (message == null) {
            return null;
        }
        String plain = normalize(message);
        String upper = plain.toUpperCase(Locale.ROOT);
        int marker = upper.indexOf("TROPHY FISH!");
        if (marker < 0) {
            return null;
        }
        String fish = plain.substring(marker + "TROPHY FISH!".length()).trim();
        fish = fish.replaceFirst("(?i)^You\\s+caught\\s+a\\s+", "")
                .replaceFirst("^[!:>\\-\\s]+", "")
                .replaceAll("\\s+", " ").trim();
        return fish.length() == 0 ? null : fish;
    }

    static boolean isFakepixelServerAddress(String serverAddress) {
        return serverAddress != null && serverAddress.toLowerCase(Locale.ROOT).contains("fakepixel");
    }

    static boolean isTrophyFishingArea(String scoreboardText) {
        if (scoreboardText == null) {
            return false;
        }
        String upper = normalize(scoreboardText).toUpperCase(Locale.ROOT);
        return upper.contains("CRIMSON ISLE")
                || upper.contains("CRIMSON FIELDS")
                || upper.contains("SCARLETON")
                || upper.contains("RUINS OF ASHFANG")
                || upper.contains("BLAZING VOLCANO");
    }

    private static String normalize(String message) {
        return SpiderDropAlert.stripFormattingCodes(message)
                .replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
    }
}

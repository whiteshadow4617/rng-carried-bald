package dev.arena.spiderdropalert;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure parsers for Slayer rewards and Fishing catch announcements. */
final class DropMessageParser {
    private static final Pattern RARITY_LABEL = Pattern.compile(
            "(?i)(?:(?:VERY|CRAZY|INCREDIBLY)\\s+)?RARE\\s+DROP|INSANE\\s+DROP");
    private static final Pattern MAGIC_FIND_SUFFIX = Pattern.compile(
            "(?i)\\s*\\([^)]*MAGIC\\s+FIND[^)]*\\)\\s*$");
    private static final Pattern STACK_SUFFIX = Pattern.compile(
            "(?i)\\s*(?:\\(\\s*(?:X\\s*([\\d,]+)|([\\d,]+)\\s*[X\\x{00D7}])\\s*\\)"
                    + "|(?:X|\\x{00D7})\\s*([\\d,]+)|([\\d,]+)\\s*[X\\x{00D7}])\\s*$");
    private static final Pattern PARENTHESIZED_COUNT_PREFIX = Pattern.compile(
            "(?i)^\\s*\\(\\s*([\\d,]+)\\s*\\)\\s*[X\\x{00D7}]\\s*");
    private static final Pattern STACK_PREFIX = Pattern.compile(
            "(?i)^\\s*(?:X\\s*([\\d,]+)|([\\d,]+)\\s*[X\\x{00D7}])\\s+");

    private DropMessageParser() {
    }

    static String normalizeItemName(String item) {
        if (item == null) {
            return "";
        }
        String normalized = SpiderDropAlert.stripFormattingCodes(item).trim();
        Matcher suffix = STACK_SUFFIX.matcher(normalized);
        if (suffix.find()) {
            normalized = normalized.substring(0, suffix.start());
        }
        Matcher prefix = STACK_PREFIX.matcher(normalized);
        if (prefix.find()) {
            normalized = normalized.substring(prefix.end());
        }
        normalized = stripOuterBrackets(normalized.trim())
                .replaceFirst("[!.,;:]+$", "").trim();
        return normalized.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    static String extractDropItem(String message) {
        String item = getDropPayload(message);
        boolean changed;
        do {
            changed = false;
            String unwrapped = stripOuterBrackets(item);
            if (!unwrapped.equals(item)) {
                item = unwrapped;
                changed = true;
                continue;
            }

            Matcher suffix = STACK_SUFFIX.matcher(item);
            if (suffix.find()) {
                item = item.substring(0, suffix.start()).trim();
                changed = true;
                continue;
            }
            Matcher parenthesizedPrefix = PARENTHESIZED_COUNT_PREFIX.matcher(item);
            if (parenthesizedPrefix.find()) {
                item = item.substring(parenthesizedPrefix.end()).trim();
                changed = true;
                continue;
            }
            Matcher prefix = STACK_PREFIX.matcher(item);
            if (prefix.find()) {
                item = item.substring(prefix.end()).trim();
                changed = true;
            }
        } while (changed && item.length() > 0);
        return stripOuterBrackets(item.trim());
    }

    static long extractDropQuantity(String message) {
        String item = getDropPayload(message);
        long quantity = 1L;
        boolean foundQuantity = false;
        while (item.length() > 0) {
            String unwrapped = stripOuterBrackets(item);
            if (!unwrapped.equals(item)) {
                item = unwrapped;
                continue;
            }

            Matcher suffix = STACK_SUFFIX.matcher(item);
            if (suffix.find()) {
                long parsedQuantity = getStackCount(suffix);
                if (!foundQuantity || parsedQuantity > quantity) {
                    quantity = parsedQuantity;
                }
                foundQuantity = true;
                item = item.substring(0, suffix.start()).trim();
                continue;
            }
            Matcher parenthesizedPrefix = PARENTHESIZED_COUNT_PREFIX.matcher(item);
            if (parenthesizedPrefix.find()) {
                long parsedQuantity = getStackCount(parenthesizedPrefix);
                if (!foundQuantity || parsedQuantity > quantity) {
                    quantity = parsedQuantity;
                }
                foundQuantity = true;
                item = item.substring(parenthesizedPrefix.end()).trim();
                continue;
            }
            Matcher prefix = STACK_PREFIX.matcher(item);
            if (prefix.find()) {
                long parsedQuantity = getStackCount(prefix);
                if (!foundQuantity || parsedQuantity > quantity) {
                    quantity = parsedQuantity;
                }
                foundQuantity = true;
                item = item.substring(prefix.end()).trim();
                continue;
            }
            break;
        }
        return quantity;
    }

    static LegendaryCreature detectLegendaryCreature(String message) {
        if (message == null) {
            return null;
        }
        String plainMessage = SpiderDropAlert.stripFormattingCodes(message)
                .replace('\n', ' ').replace('\r', ' ').trim().replaceAll("\\s+", " ");
        String upper = plainMessage.toUpperCase(Locale.ROOT);
        for (LegendaryCreature creature : LegendaryCreature.values()) {
            for (String chatPrefix : creature.chatPrefixes) {
                if (upper.startsWith(chatPrefix)) {
                    return creature;
                }
            }
        }
        return null;
    }

    static SlayerType detectSlayer(String message) {
        if (message == null) {
            return null;
        }
        String plainMessage = SpiderDropAlert.stripFormattingCodes(message).trim();
        if (!RARITY_LABEL.matcher(plainMessage).lookingAt()) {
            return null;
        }
        String upper = plainMessage.toUpperCase(Locale.ROOT);
        for (SlayerType slayer : SlayerType.values()) {
            for (String marker : slayer.markers) {
                if (upper.contains(marker)) {
                    return slayer;
                }
            }
        }
        return null;
    }

    static SlayerType detectSlayerBossSpawnMessage(String message) {
        if (message == null) {
            return null;
        }
        String upper = SpiderDropAlert.stripFormattingCodes(message).trim().toUpperCase(Locale.ROOT);
        if (!upper.startsWith("[BOSS]")) {
            return null;
        }
        return detectSlayerBossName(upper);
    }

    static SlayerType detectSlayerBossName(String displayName) {
        if (displayName == null) {
            return null;
        }
        String upper = SpiderDropAlert.stripFormattingCodes(displayName).trim().toUpperCase(Locale.ROOT);
        if (upper.contains("TARANTULA BROODFATHER")
                || upper.contains("PRIMORDIAL BROODFATHER")) {
            return SlayerType.SPIDER;
        }
        if (upper.contains("REVENANT HORROR") || upper.contains("ATONED HORROR")) {
            return SlayerType.REVENANT;
        }
        if (upper.contains("INFERNO DEMONLORD")) {
            return SlayerType.BLAZE;
        }
        if (upper.contains("SVEN PACKMASTER")) {
            return SlayerType.WOLF;
        }
        if (upper.contains("VOIDGLOOM SERAPH")) {
            return SlayerType.ENDERMAN;
        }
        return null;
    }

    static boolean isSlayerQuestCompleteMessage(String message) {
        return message != null && SpiderDropAlert.stripFormattingCodes(message).trim()
                .toUpperCase(Locale.ROOT).startsWith("SLAYER QUEST COMPLETE");
    }

    static boolean isSpiderSlayerRareDrop(String message) {
        return detectSlayer(message) == SlayerType.SPIDER;
    }

    private static String getDropPayload(String message) {
        if (message == null) {
            return "";
        }
        Matcher label = RARITY_LABEL.matcher(message);
        String item = label.find() ? message.substring(label.end()).trim() : message.trim();
        item = item.replaceFirst("^[!:>\\-\\s]+", "");
        item = stripOuterBrackets(item);

        Matcher magicFind = MAGIC_FIND_SUFFIX.matcher(item);
        if (magicFind.find()) {
            item = item.substring(0, magicFind.start()).trim();
        }
        return stripOuterBrackets(item.trim());
    }

    private static String stripOuterBrackets(String item) {
        String result = item == null ? "" : item.trim();
        boolean stripped;
        do {
            stripped = false;
            if (isWrappedBy(result, '[', ']') || isWrappedBy(result, '(', ')')) {
                result = result.substring(1, result.length() - 1).trim();
                stripped = true;
            }
        } while (stripped);
        return result;
    }

    private static boolean isWrappedBy(String value, char opening, char closing) {
        if (value == null || value.length() < 2
                || value.charAt(0) != opening || value.charAt(value.length() - 1) != closing) {
            return false;
        }
        int depth = 0;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == opening) {
                depth++;
            } else if (current == closing) {
                depth--;
                if (depth < 0 || (depth == 0 && i < value.length() - 1)) {
                    return false;
                }
            }
        }
        return depth == 0;
    }

    private static long getStackCount(Matcher matcher) {
        for (int group = 1; group <= matcher.groupCount(); group++) {
            String count = matcher.group(group);
            if (count != null) {
                try {
                    return Math.max(1L, Long.parseLong(count.replace(",", "")));
                } catch (NumberFormatException ignored) {
                    return 1L;
                }
            }
        }
        return 1L;
    }
}

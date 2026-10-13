package dev.arena.spiderdropalert;

import java.util.Locale;

/** Wolf Slayer Power Orbs and the existing Totem/SOS timers used by the Deployables panel. */
enum DeployableItem {
    RADIANT_POWER_ORB("Radiant Power Orb", DeployableGroup.POWER_ORBS, 30),
    MANA_FLUX_POWER_ORB("Mana Flux Power Orb", DeployableGroup.POWER_ORBS, 30),
    OVERFLUX_POWER_ORB("Overflux Power Orb", DeployableGroup.POWER_ORBS, 60),
    PLASMAFLUX_POWER_ORB("Plasmaflux Power Orb", DeployableGroup.POWER_ORBS, 60),
    SOS_FLARE("SOS Flare", DeployableGroup.SOS_FLARE, 180),
    TOTEM_OF_CORRUPTION("Totem of Corruption", DeployableGroup.TOTEM_OF_CORRUPTION, 120);

    private final String displayName;
    private final DeployableGroup group;
    private final int durationSeconds;
    private final String normalizedName;

    DeployableItem(String displayName, DeployableGroup group, int durationSeconds) {
        this.displayName = displayName;
        this.group = group;
        this.durationSeconds = durationSeconds;
        this.normalizedName = normalize(displayName);
    }

    String getDisplayName() {
        return displayName;
    }

    DeployableGroup getGroup() {
        return group;
    }

    int getDurationSeconds() {
        return durationSeconds;
    }

    static DeployableItem fromDisplayName(String itemName) {
        if (itemName == null) {
            return null;
        }
        String normalized = normalize(itemName);
        for (DeployableItem item : values()) {
            if (normalized.contains(item.normalizedName)) {
                return item;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        return SpiderDropAlert.stripFormattingCodes(value)
                .toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }
}

enum DeployableGroup {
    POWER_ORBS("Power Orbs", "Orbs", "deployables.powerOrbs.enabled",
            "deployables.plasmaflux.enabled"),
    SOS_FLARE("SOS Flare", "SOS", "deployables.sosFlare.enabled", null),
    TOTEM_OF_CORRUPTION("Totem of Corruption", "Totem", "deployables.totem.enabled", null);

    private final String displayName;
    private final String compactName;
    private final String settingsKey;
    private final String legacySettingsKey;

    DeployableGroup(String displayName, String compactName, String settingsKey,
                    String legacySettingsKey) {
        this.displayName = displayName;
        this.compactName = compactName;
        this.settingsKey = settingsKey;
        this.legacySettingsKey = legacySettingsKey;
    }

    String getDisplayName() {
        return displayName;
    }

    String getCompactName() {
        return compactName;
    }

    String getSettingsKey() {
        return settingsKey;
    }

    String getLegacySettingsKey() {
        return legacySettingsKey;
    }
}

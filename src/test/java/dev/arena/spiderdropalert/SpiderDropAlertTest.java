package dev.arena.spiderdropalert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import net.minecraft.util.BlockPos;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class SpiderDropAlertTest {
    @Test
    public void exposesTheRenamedProductAndNewVersionWithoutChangingModId() {
        assertEquals("RNG Carried Bald", SpiderDropAlert.MOD_NAME);
        assertEquals("spiderdropalert", SpiderDropAlert.MOD_ID);
        assertEquals("1.0.65", SpiderDropAlert.VERSION);
    }

    @Test
    public void trackerGuiCommandSupportsShortAlias() {
        assertTrue(new TrackerGuiCommand(null).getCommandAliases().contains("tg"));
    }

    @Test
    public void hudPanelBackgroundOpacityIsIndependentClampedAndPersisted() throws Exception {
        File settingsFile = File.createTempFile("spider-drop-opacity-", ".properties");
        try {
            SpiderDropAlert mod = new SpiderDropAlert(settingsFile);
            for (int panel = SpiderDropAlert.HUD_PANEL_SLAYER;
                 panel <= SpiderDropAlert.HUD_PANEL_DEPLOYABLES; panel++) {
                assertEquals(35, mod.getHudPanelBackgroundOpacity(panel));
            }

            mod.setHudPanelBackgroundOpacity(SpiderDropAlert.HUD_PANEL_SLAYER, -10);
            assertEquals(1, mod.getHudPanelBackgroundOpacity(SpiderDropAlert.HUD_PANEL_SLAYER));
            mod.setHudPanelBackgroundOpacity(SpiderDropAlert.HUD_PANEL_FISHING, 41);
            mod.setHudPanelBackgroundOpacity(SpiderDropAlert.HUD_PANEL_CREATURE_HP, 99);
            mod.setHudPanelBackgroundOpacity(SpiderDropAlert.HUD_PANEL_DEPLOYABLES, 150);
            mod.saveSettings();

            Properties legacySettings = new Properties();
            try (FileInputStream input = new FileInputStream(settingsFile)) {
                legacySettings.load(input);
            }
            legacySettings.setProperty("tracker.backgroundOpacityPercent", "0");
            try (FileOutputStream output = new FileOutputStream(settingsFile)) {
                legacySettings.store(output, "legacy zero-opacity migration test");
            }

            SpiderDropAlert loaded = new SpiderDropAlert(settingsFile);
            assertEquals(1, loaded.getHudPanelBackgroundOpacity(SpiderDropAlert.HUD_PANEL_SLAYER));
            assertEquals(41, loaded.getHudPanelBackgroundOpacity(SpiderDropAlert.HUD_PANEL_FISHING));
            assertEquals(99, loaded.getHudPanelBackgroundOpacity(SpiderDropAlert.HUD_PANEL_CREATURE_HP));
            assertEquals(100, loaded.getHudPanelBackgroundOpacity(SpiderDropAlert.HUD_PANEL_DEPLOYABLES));
        } finally {
            settingsFile.delete();
        }
    }

    @Test
    public void opacitySliderValuesMapToTranslucentBackgroundAlpha() {
        assertEquals(0x030A0D14, HudPanelRenderer.panelBackgroundColor(0));
        assertEquals(0x030A0D14, HudPanelRenderer.panelBackgroundColor(1));
        assertEquals(0x590A0D14, HudPanelRenderer.panelBackgroundColor(35));
        assertEquals(0xFF0A0D14, HudPanelRenderer.panelBackgroundColor(100));
        assertEquals(0xFF0A0D14, HudPanelRenderer.panelBackgroundColor(150));
    }

    @Test
    public void detectsNormalAndMasterModeFloorSevenScoreboardsOnly() {
        assertTrue(M7OverlayRenderer.isFloorSevenScoreboardText(
                "The Catacombs - Master Mode Floor VII"));
        assertTrue(M7OverlayRenderer.isFloorSevenScoreboardText("Catacombs (M7)"));
        assertTrue(M7OverlayRenderer.isFloorSevenScoreboardText("[M7] Catacombs"));
        assertTrue(M7OverlayRenderer.isFloorSevenScoreboardText("MMF7"));
        assertTrue(M7OverlayRenderer.isFloorSevenScoreboardText("MM - Floor 7"));
        assertTrue(M7OverlayRenderer.isFloorSevenScoreboardText("Master Mode - F7"));
        assertTrue(M7OverlayRenderer.isFloorSevenScoreboardText("Master Mode - F 7"));
        assertTrue(M7OverlayRenderer.isFloorSevenScoreboardText("Master Mode - Floor VII"));
        assertTrue(M7OverlayRenderer.isFloorSevenScoreboardText("The Catacombs - Floor VII"));
        assertTrue(M7OverlayRenderer.isFloorSevenScoreboardText("The Catacombs - F7"));
        assertTrue(M7OverlayRenderer.isFloorSevenScoreboardText("Catacombs - F 7"));
        assertFalse(M7OverlayRenderer.isFloorSevenScoreboardText("The Catacombs - Floor VI"));
        assertTrue(M7OverlayRenderer.isM7ScoreboardText("Master Mode - Floor VII"));
        assertTrue(M7OverlayRenderer.isM7ScoreboardText("Catacombs (M7)"));
        assertFalse(M7OverlayRenderer.isM7ScoreboardText("The Catacombs - Floor VII"));
        assertFalse(M7OverlayRenderer.isFloorSevenScoreboardText("Master Mode Floor VI"));
        assertFalse(M7OverlayRenderer.isFloorSevenScoreboardText("Floor VII"));
        assertFalse(M7OverlayRenderer.isFloorSevenScoreboardText("F7 Queue"));
        assertFalse(M7OverlayRenderer.isFloorSevenScoreboardText(null));
    }

    @Test
    public void detectsFakepixelDungeonSidebarWithMultipleM7DragonHealthLines() {
        String fakepixelM7 = "SKYBLOCK 10/10/26 dung-5 Dungeon Power Dragon 628.3M "
                + "Ice Dragon 763.7M Dungeon Cleared 100%";
        assertTrue(M7OverlayRenderer.isFloorSevenScoreboardText(fakepixelM7));
        assertTrue(M7OverlayRenderer.isM7ScoreboardText(fakepixelM7));
        assertTrue(M7OverlayRenderer.isFakepixelM7DragonScoreboardText(fakepixelM7));
        assertTrue(M7OverlayRenderer.isFloorSevenScoreboardText(
                "SKYBLOCK 10/10/26 dung-5 Dungeon Power Dragon 628.3M"));
        assertTrue(M7OverlayRenderer.isM7ScoreboardText(
                "SKYBLOCK 10/10/26 dung-5 Dungeon Power Dragon 628.3M"));
        assertFalse(M7OverlayRenderer.isFloorSevenScoreboardText(
                "SKYBLOCK Dungeon Power Dragon 628.3M"));
        assertFalse(M7OverlayRenderer.isM7ScoreboardText(
                "SKYBLOCK Power Dragon 628.3M Ice Dragon 763.7M"));
    }

    @Test
    public void classifiesTheFourM7BossesAndDragonEntityHitboxes() {
        assertEquals(M7OverlayRenderer.M7HitboxTarget.MAXOR,
                M7OverlayRenderer.classifyM7HitboxTarget("\u00A7c[BOSS] Maxor", false));
        assertEquals(M7OverlayRenderer.M7HitboxTarget.STORM,
                M7OverlayRenderer.classifyM7HitboxTarget("Storm", false));
        assertEquals(M7OverlayRenderer.M7HitboxTarget.GOLDOR,
                M7OverlayRenderer.classifyM7HitboxTarget("Goldor", false));
        assertEquals(M7OverlayRenderer.M7HitboxTarget.NECRON,
                M7OverlayRenderer.classifyM7HitboxTarget("Necron", false));
        assertEquals(M7OverlayRenderer.M7HitboxTarget.MAXOR,
                M7OverlayRenderer.classifyM7HitboxTarget("[BOSS] Maxor the Wither", false));
        assertEquals(M7OverlayRenderer.M7HitboxTarget.STORM,
                M7OverlayRenderer.classifyM7HitboxTarget("\\u00A7c[BOSS] Storm - Phase 2", false));
        assertEquals(M7OverlayRenderer.M7HitboxTarget.GOLDOR,
                M7OverlayRenderer.classifyM7HitboxTarget("Goldor's Wither", false));
        assertEquals(M7OverlayRenderer.M7HitboxTarget.NECRON,
                M7OverlayRenderer.classifyM7HitboxTarget("Necron (Invulnerable)", false));
        assertNull(M7OverlayRenderer.classifyM7HitboxTarget("Wither", false));
        assertEquals(M7OverlayRenderer.M7HitboxTarget.DRAGON,
                M7OverlayRenderer.classifyM7HitboxTarget("Ender Dragon", true));
        assertEquals(M7OverlayRenderer.M7HitboxTarget.DRAGON,
                M7OverlayRenderer.classifyM7HitboxTarget("Apex Dragon", false));
        assertEquals(M7OverlayRenderer.M7HitboxTarget.DRAGON,
                M7OverlayRenderer.classifyM7HitboxTarget("Dragon", false));
        assertEquals(M7OverlayRenderer.M7HitboxTarget.DRAGON,
                M7OverlayRenderer.classifyM7HitboxTarget("Ender Dragon", false));
        assertNull(M7OverlayRenderer.classifyM7HitboxTarget("Wither Skeleton", false));
    }

    @Test
    public void featureSwitchesRemainIndependentAndPersist() throws Exception {
        File settingsFile = File.createTempFile("spider-drop-feature-switches-", ".properties");
        try {
            SpiderDropAlert mod = new SpiderDropAlert(settingsFile);
            mod.startDeployableTimer(DeployableItem.TOTEM_OF_CORRUPTION, 100L);
            mod.startDeployableTimer(DeployableItem.SOS_FLARE, 100L);
            mod.startDeployableTimer(DeployableItem.RADIANT_POWER_ORB, 100L);
            mod.startDeployableTimer(DeployableItem.MANA_FLUX_POWER_ORB, 100L);
            mod.startDeployableTimer(DeployableItem.OVERFLUX_POWER_ORB, 100L);
            mod.startDeployableTimer(DeployableItem.PLASMAFLUX_POWER_ORB, 100L);

            assertTrue(mod.isM7BossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget.MAXOR));
            assertTrue(mod.isM7BossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget.STORM));
            assertTrue(mod.isM7BossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget.GOLDOR));
            assertTrue(mod.isM7BossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget.NECRON));
            assertTrue(mod.isM7DragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.POWER));
            assertTrue(mod.isM7DragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.APEX));
            assertTrue(mod.isM7DragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.SOUL));
            assertTrue(mod.isM7DragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.ICE));
            assertTrue(mod.isM7DragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.FLAME));
            assertTrue(mod.isM7StatueZonesEnabled());
            assertTrue(mod.isM7AreasPersistAfterDragonSpawn());
            assertTrue(mod.isM7DragonKillAlertEnabled());
            mod.toggleM7BossHitbox(M7OverlayRenderer.M7HitboxTarget.MAXOR);
            mod.toggleM7DragonHitbox(M7OverlayRenderer.M7DragonStatue.ICE);
            mod.toggleM7StatueZones();
            mod.toggleM7AreasPersistAfterDragonSpawn();
            mod.toggleM7DragonKillAlert();

            mod.toggleM7Overlay();
            mod.toggleDeployableGroup(DeployableGroup.SOS_FLARE);
            mod.toggleDeployableGroup(DeployableGroup.POWER_ORBS);

            assertFalse(mod.isM7OverlayEnabled());
            assertFalse(mod.isM7BossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget.MAXOR));
            assertTrue(mod.isM7BossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget.STORM));
            assertFalse(mod.isM7DragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.ICE));
            assertTrue(mod.isM7DragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.POWER));
            assertFalse(mod.isM7StatueZonesEnabled());
            assertFalse(mod.isM7AreasPersistAfterDragonSpawn());
            assertFalse(mod.isM7DragonKillAlertEnabled());
            assertTrue(mod.isDeployableGroupEnabled(DeployableGroup.TOTEM_OF_CORRUPTION));
            assertFalse(mod.isDeployableGroupEnabled(DeployableGroup.SOS_FLARE));
            assertFalse(mod.isDeployableGroupEnabled(DeployableGroup.POWER_ORBS));
            assertTrue(mod.isTrackerEnabled());
            assertTrue(mod.isDeployablesTimerPanelEnabled());
            assertEquals(120, mod.getDeployableRemainingSeconds(
                    DeployableItem.TOTEM_OF_CORRUPTION, 100L));
            assertEquals(0, mod.getDeployableRemainingSeconds(DeployableItem.SOS_FLARE, 100L));
            assertEquals(0, mod.getDeployableRemainingSeconds(
                    DeployableItem.RADIANT_POWER_ORB, 100L));
            assertEquals(0, mod.getDeployableRemainingSeconds(
                    DeployableItem.MANA_FLUX_POWER_ORB, 100L));
            assertEquals(0, mod.getDeployableRemainingSeconds(
                    DeployableItem.OVERFLUX_POWER_ORB, 100L));
            assertEquals(0, mod.getDeployableRemainingSeconds(
                    DeployableItem.PLASMAFLUX_POWER_ORB, 100L));
            assertTrue(mod.isDeployablesTimerPanelVisible(100L));

            mod.toggleDeployableGroup(DeployableGroup.TOTEM_OF_CORRUPTION);
            assertFalse(mod.isDeployablesTimerPanelVisible(100L));

            SpiderDropAlert loaded = new SpiderDropAlert(settingsFile);
            assertFalse(loaded.isM7OverlayEnabled());
            assertFalse(loaded.isM7BossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget.MAXOR));
            assertTrue(loaded.isM7BossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget.STORM));
            assertFalse(loaded.isM7DragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.ICE));
            assertTrue(loaded.isM7DragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.POWER));
            assertFalse(loaded.isM7StatueZonesEnabled());
            assertFalse(loaded.isM7AreasPersistAfterDragonSpawn());
            assertFalse(loaded.isM7DragonKillAlertEnabled());
            for (DeployableGroup group : DeployableGroup.values()) {
                assertFalse(loaded.isDeployableGroupEnabled(group));
            }
            assertTrue(loaded.isTrackerEnabled());
            assertTrue(loaded.isDeployablesTimerPanelEnabled());
        } finally {
            settingsFile.delete();
        }
    }

    @Test
    public void mapsM7DragonsAndUsesNef124RelicAreaGeometry() {
        assertEquals(5, M7OverlayRenderer.M7DragonStatue.values().length);
        assertEquals(M7OverlayRenderer.M7DragonStatue.APEX,
                M7OverlayRenderer.dragonStatueForName("Apex Dragon"));
        assertEquals(M7OverlayRenderer.M7DragonStatue.FLAME,
                M7OverlayRenderer.dragonStatueForName("Flame Dragon"));
        assertEquals(M7OverlayRenderer.M7DragonStatue.ICE,
                M7OverlayRenderer.dragonStatueForName("Ice Dragon"));
        assertEquals(M7OverlayRenderer.M7DragonStatue.POWER,
                M7OverlayRenderer.dragonStatueForName("Power Dragon"));
        assertEquals(M7OverlayRenderer.M7DragonStatue.SOUL,
                M7OverlayRenderer.dragonStatueForName("Soul Dragon"));
        assertEquals(M7OverlayRenderer.M7DragonStatue.POWER,
                M7OverlayRenderer.nearestM7DragonStatue(27.0D, 59.0D));

        assertEquals(5, M7OverlayRenderer.M7DragonKillArea.values().length);
        M7OverlayRenderer.M7DragonKillArea red = M7OverlayRenderer.M7DragonKillArea.RED_RELIC;
        assertEquals(32.0D, red.bounds.minX, 0.0D);
        assertEquals(15.0D, red.bounds.minY, 0.0D);
        assertEquals(53.0D, red.bounds.minZ, 0.0D);
        assertEquals(64.0D, red.bounds.maxX, 0.0D);
        assertEquals(37.0D, red.bounds.maxY, 0.0D);
        assertEquals(77.0D, red.bounds.maxZ, 0.0D);
        assertEquals(0xFFFF0000, red.color);

        M7OverlayRenderer.M7DragonKillArea green = M7OverlayRenderer.M7DragonKillArea.GREEN_RELIC;
        assertEquals(32.0D, green.bounds.minX, 0.0D);
        assertEquals(91.0D, green.bounds.minZ, 0.0D);
        assertEquals(64.0D, green.bounds.maxX, 0.0D);
        assertEquals(115.0D, green.bounds.maxZ, 0.0D);
        assertEquals(0xFF00FF00, green.color);

        M7OverlayRenderer.M7DragonKillArea blue = M7OverlayRenderer.M7DragonKillArea.BLUE_RELIC;
        assertEquals(64.0D, blue.bounds.minX, 0.0D);
        assertEquals(91.0D, blue.bounds.minZ, 0.0D);
        assertEquals(96.0D, blue.bounds.maxX, 0.0D);
        assertEquals(115.0D, blue.bounds.maxZ, 0.0D);
        assertEquals(0xFF00FFFF, blue.color);

        M7OverlayRenderer.M7DragonKillArea orange = M7OverlayRenderer.M7DragonKillArea.ORANGE_RELIC;
        assertEquals(64.0D, orange.bounds.minX, 0.0D);
        assertEquals(53.0D, orange.bounds.minZ, 0.0D);
        assertEquals(96.0D, orange.bounds.maxX, 0.0D);
        assertEquals(77.0D, orange.bounds.maxZ, 0.0D);
        assertEquals(0xFFFFC800, orange.color);

        M7OverlayRenderer.M7DragonKillArea purple = M7OverlayRenderer.M7DragonKillArea.PURPLE_RELIC;
        assertEquals(53.0D, purple.bounds.minX, 0.0D);
        assertEquals(104.0D, purple.bounds.minZ, 0.0D);
        assertEquals(77.0D, purple.bounds.maxX, 0.0D);
        assertEquals(136.0D, purple.bounds.maxZ, 0.0D);
        assertEquals(0xFFFFAFAF, purple.color);
    }

    @Test
    public void m7DragonAreaPersistenceToggleControlsSpawnVisibility() {
        // ON is the default behavior: keep the areas after dragons spawn.
        assertTrue(M7OverlayRenderer.shouldRenderM7DragonKillAreas(true, true,
                true, true, true));
        // OFF hides areas after a dragon appears, but shows them before the spawn.
        assertTrue(M7OverlayRenderer.shouldRenderM7DragonKillAreas(true, true,
                false, true, false));
        assertFalse(M7OverlayRenderer.shouldRenderM7DragonKillAreas(true, true,
                true, true, false));
        assertFalse(M7OverlayRenderer.shouldRenderM7DragonKillAreas(false, true,
                false, true, true));
        assertFalse(M7OverlayRenderer.shouldRenderM7DragonKillAreas(true, false,
                false, true, true));
        assertFalse(M7OverlayRenderer.shouldRenderM7DragonKillAreas(true, true,
                false, false, true));
    }

    @Test
    public void recognizesNefM7FinalPhaseChatCue() {
        assertTrue(M7OverlayRenderer.isM7FinalPhaseMessage(
                "\u00A7cThe Catacombs... are no more."));
        assertTrue(M7OverlayRenderer.isM7FinalPhaseMessage("The Catacombs... are no more"));
        assertFalse(M7OverlayRenderer.isM7FinalPhaseMessage("The Catacombs - Floor VII"));
        assertFalse(M7OverlayRenderer.isM7FinalPhaseMessage(null));
    }

    @Test
    public void extractsItemAndRemovesMagicFindSuffix() {
        assertEquals("Tarantula Talisman", DropMessageParser.extractDropItem(
                "VERY RARE DROP! Tarantula Talisman (+25% Magic Find)"));
    }

    @Test
    public void preservesParenthesesThatArePartOfTheItemName() {
        assertEquals("Enchanted Book (Bane of Arthropods VI)", DropMessageParser.extractDropItem(
                "RARE DROP! Enchanted Book (Bane of Arthropods VI) (x1) (+25% Magic Find)"));
    }

    @Test
    public void recognizesSpiderRewardsButNotOtherRareDrops() {
        assertTrue(DropMessageParser.isSpiderSlayerRareDrop("VERY RARE DROP! Tarantula Talisman"));
        assertTrue(DropMessageParser.isSpiderSlayerRareDrop("RARE DROP! Spider Slayer reward: Unknown Gem"));
        assertFalse(DropMessageParser.isSpiderSlayerRareDrop("RARE DROP! Enchanted Ender Armor"));
        assertTrue(DropMessageParser.isSpiderSlayerRareDrop("RARE DROP! Toxic Arrow Poison"));
        assertTrue(DropMessageParser.isSpiderSlayerRareDrop(
                "RARE DROP! (1x Tarantula Silk) (+157% Magic Find!)"));
        assertTrue(DropMessageParser.isSpiderSlayerRareDrop(
                "RARE DROP! Spider Slayer reward: Toxic Arrow Poison"));
        assertFalse(DropMessageParser.isSpiderSlayerRareDrop(
                "[MVP+] Player: RARE DROP! (1x Tarantula Silk) (+157% Magic Find!)"));
    }

    @Test
    public void detectsAllFiveSupportedSlayerTypes() {
        SlayerType[] orderedTypes = SlayerType.values();
        assertEquals(5, orderedTypes.length);
        assertEquals(SlayerType.SPIDER, orderedTypes[0]);
        assertEquals(SlayerType.REVENANT, orderedTypes[1]);
        assertEquals(SlayerType.BLAZE, orderedTypes[2]);
        assertEquals(SlayerType.WOLF, orderedTypes[3]);
        assertEquals(SlayerType.ENDERMAN, orderedTypes[4]);
        assertEquals(SlayerType.SPIDER,
                DropMessageParser.detectSlayer("RARE DROP! (64x Toxic Arrow Poison)"));
        assertEquals(SlayerType.REVENANT,
                DropMessageParser.detectSlayer("RARE DROP! (1x Scythe Blade)"));
        assertEquals(SlayerType.BLAZE,
                DropMessageParser.detectSlayer("RARE DROP! (1x Archfiend Dice)"));
        assertEquals(SlayerType.WOLF,
                DropMessageParser.detectSlayer("RARE DROP! (1x Overflux Capacitor)"));
        assertEquals(SlayerType.ENDERMAN,
                DropMessageParser.detectSlayer("RARE DROP! (1x Judgement Core)"));
        assertNull(DropMessageParser.detectSlayer("RARE DROP! Summoning Eye"));
        assertNull(DropMessageParser.detectSlayer("RARE DROP! Paragon Shard"));
        assertNull(DropMessageParser.detectSlayer(
                "[MVP+] Player: RARE DROP! (1x Judgement Core)"));
        assertNull(DropMessageParser.detectSlayer(
                "[MVP+] Player: RARE DROP! (1x Scythe Blade)"));
        assertNull(DropMessageParser.detectSlayer(
                "[MVP+] Player: VERY RARE DROP! Tarantula Talisman"));
    }

    @Test
    public void detectsSpawnMessagesForAllFiveSlayerBossesAndRejectsPlayerLines() {
        assertEquals(SlayerType.SPIDER,
                DropMessageParser.detectSlayerBossSpawnMessage(
                        "[BOSS] Tarantula Broodfather: Welcome to my den."));
        assertEquals(SlayerType.SPIDER,
                DropMessageParser.detectSlayerBossName("[Lv 750] Primordial Broodfather"));
        assertEquals(SlayerType.REVENANT,
                DropMessageParser.detectSlayerBossSpawnMessage("[BOSS] Atoned Horror: Rise."));
        assertEquals(SlayerType.BLAZE,
                DropMessageParser.detectSlayerBossSpawnMessage(
                        "\u00A7c[BOSS] Inferno Demonlord: Burn."));
        assertEquals(SlayerType.WOLF,
                DropMessageParser.detectSlayerBossSpawnMessage("[BOSS] Sven Packmaster: Howl."));
        assertEquals(SlayerType.ENDERMAN,
                DropMessageParser.detectSlayerBossSpawnMessage("[BOSS] Voidgloom Seraph: ..."));
        assertNull(DropMessageParser.detectSlayerBossSpawnMessage(
                "[MVP+] Player: [BOSS] Tarantula Broodfather: copied text"));
        assertNull(DropMessageParser.detectSlayerBossName("Tarantula Vermin"));
        assertTrue(DropMessageParser.isSlayerQuestCompleteMessage("SLAYER QUEST COMPLETE!"));
        assertTrue(DropMessageParser.isSlayerQuestCompleteMessage(
                "\u00A7aSLAYER QUEST COMPLETE! You have slain the boss."));
        assertFalse(DropMessageParser.isSlayerQuestCompleteMessage(
                "[MVP+] Player: SLAYER QUEST COMPLETE!"));
    }

    @Test
    public void fishingTimerTracksBobberCastUntilReelIn() {
        SpiderDropAlert mod = new SpiderDropAlert();
        assertFalse(mod.hasFishingTimer());

        mod.updateFishingTimerState(true, 1000L);
        assertTrue(mod.hasFishingTimer());
        assertTrue(mod.isFishingTimerActive());
        assertEquals(5000L, mod.getFishingTimerElapsedMillis(6000L));

        // A bobber that remains out must not restart the stopwatch on later ticks.
        mod.updateFishingTimerState(true, 6000L);
        assertEquals(7500L, mod.getFishingTimerElapsedMillis(8500L));

        mod.updateFishingTimerState(false, 8500L);
        assertFalse(mod.isFishingTimerActive());
        assertEquals(7500L, mod.getFishingTimerElapsedMillis(20000L));

        mod.updateFishingTimerState(true, 21000L);
        assertTrue(mod.isFishingTimerActive());
        assertEquals("00:05", SpiderDropAlert.formatTimerDuration(5000L));
        assertEquals("01:00", SpiderDropAlert.formatTimerDuration(60000L));
        assertEquals("1:00:00", SpiderDropAlert.formatTimerDuration(3600000L));
    }

    @Test
    public void splashToPullTimerRunsFromBobberSplashUntilTheDetectedDip() {
        SpiderDropAlert mod = new SpiderDropAlert();
        mod.updateSplashToPullTimerState(true, 55, 10.0D, 1000L);
        assertFalse(mod.hasSplashToPullTimer());
        assertFalse(mod.startSplashToPullTimer(55, 2199L));

        mod.updateSplashToPullTimerState(true, 55, 10.0D, 2200L);
        assertTrue(mod.startSplashToPullTimer(55, 2200L));
        assertTrue(mod.isSplashToPullTimerActive());

        mod.updateSplashToPullTimerState(true, 55, 9.94D, 2300L);
        assertTrue(mod.isSplashToPullTimerActive());
        assertEquals(100L, mod.getSplashToPullTimerElapsedMillis(2300L));

        mod.updateSplashToPullTimerState(true, 55, 9.88D, 2400L);
        assertFalse(mod.isSplashToPullTimerActive());
        assertTrue(mod.hasSplashToPullTimer());
        assertEquals(200L, mod.getSplashToPullTimerElapsedMillis(3000L));
        assertEquals("1.25s", SpiderDropAlert.formatSplashToPullDuration(1250L));
    }

    @Test
    public void splashTimerAcceptsOnlyNearbySplashSoundsAndResetsForANewBobber() {
        assertTrue(SpiderDropAlert.isFishingSplashSound("random.splash"));
        assertTrue(SpiderDropAlert.isFishingSplashSound("minecraft:entity.generic.SPLASH"));
        assertFalse(SpiderDropAlert.isFishingSplashSound("random.pop"));
        assertTrue(SpiderDropAlert.isFishingSplashNearBobber(
                "random.splash", 3.0D, 64.0D, 0.0D, 0.0D, 64.0D, 0.0D));
        assertFalse(SpiderDropAlert.isFishingSplashNearBobber(
                "random.splash", 4.0D, 64.0D, 0.0D, 0.0D, 64.0D, 0.0D));

        SpiderDropAlert mod = new SpiderDropAlert();
        mod.updateSplashToPullTimerState(true, 55, 10.0D, 1000L);
        mod.updateSplashToPullTimerState(true, 55, 10.0D, 2200L);
        assertTrue(mod.startSplashToPullTimer(55, 2200L));
        mod.updateSplashToPullTimerState(true, 56, 10.0D, 2400L);
        assertFalse(mod.hasSplashToPullTimer());
        assertFalse(mod.startSplashToPullTimer(55, 2400L));
    }

    @Test
    public void bobberPullThresholdRemainsAvailableToTheSplashStopwatch() {
        assertFalse(SpiderDropAlert.isBobberPulledDown(-0.20D, 1199L));
        assertFalse(SpiderDropAlert.isBobberPulledDown(-0.119D, 1200L));
        assertFalse(SpiderDropAlert.isBobberPulledDown(-0.035D, 1200L));
        assertFalse(SpiderDropAlert.isBobberPulledDown(0.0D, 1200L));
        assertTrue(SpiderDropAlert.isBobberPulledDown(-0.12D, 1200L));
    }

    @Test
    public void fakepixelFishingNotifiersRecognizeLegendaryAndTrophyCatchMessages() {
        assertEquals("The Loch Emperor", NefFishingFeatureParser.detectLegendaryCatch(
                "The Sea Emperor arises from the deep!"));
        assertEquals("Water Hydra", NefFishingFeatureParser.detectLegendaryCatch(
                "The Water Hydra has come to test your strength"));
        assertEquals("Alligator", NefFishingFeatureParser.detectLegendaryCatch(
                "A long snout breaks the surface of the water"));
        assertEquals("Blue Ringed Octopus", NefFishingFeatureParser.detectLegendaryCatch(
                "A garish set of tentacles arise from the depths"));
        assertEquals("Fiery Scuttler", NefFishingFeatureParser.detectLegendaryCatch(
                "A fiery scuttler inconspicuously waddles up to you"));
        assertEquals("Puddle Jumper", NefFishingFeatureParser.detectLegendaryCatch(
                "A puddle jumper is preparing for liftoff"));
        assertEquals("Silkbreeze", NefFishingFeatureParser.detectLegendaryCatch(
                "Something zips through the air"));
        assertEquals("Yeti", NefFishingFeatureParser.detectLegendaryCatch(
                "What is this creature!?"));
        assertEquals("Grim Reaper", NefFishingFeatureParser.detectLegendaryCatch(
                "This can't be! The manifestation of death himself!"));
        assertEquals("Lord Jawbus", NefFishingFeatureParser.detectLegendaryCatch(
                "You have angered a legendary creature."));
        assertEquals("Golden Fish", NefFishingFeatureParser.detectTrophyFish(
                "\u00A76TROPHY FISH! You caught a Golden Fish"));
        assertNull(NefFishingFeatureParser.detectTrophyFish("You caught a Golden Fish"));
        assertTrue(NefFishingFeatureParser.isFakepixelServerAddress("mc.fakepixel.fun"));
        assertFalse(NefFishingFeatureParser.isFakepixelServerAddress("mc.hypixel.net"));
        assertTrue(NefFishingFeatureParser.isTrophyFishingArea("Crimson Isle / Scarleton"));
        assertFalse(NefFishingFeatureParser.isTrophyFishingArea("The Park"));

        assertEquals(LegendaryCreature.THE_LOCH_EMPEROR,
                DropMessageParser.detectLegendaryCreature("The Sea Emperor arises"));
        assertEquals(LegendaryCreature.WATER_HYDRA,
                DropMessageParser.detectLegendaryCreature("The Water Hydra has come"));
    }

    @Test
    public void fakepixelNotifierSettingsPersistWithoutRemovedFishingAlertSettings() throws Exception {
        File settingsFile = File.createTempFile("spider-drop-fakepixel-fishing-", ".properties");
        try {
            SpiderDropAlert mod = new SpiderDropAlert(settingsFile);
            mod.toggleFishingLegendaryNotifier();
            mod.toggleFishingTrophyFishNotifier();
            assertTrue(mod.isFishingLegendaryNotifierEnabled());
            assertTrue(mod.isFishingTrophyFishNotifierEnabled());

            SpiderDropAlert loaded = new SpiderDropAlert(settingsFile);
            assertTrue(loaded.isFishingLegendaryNotifierEnabled());
            assertTrue(loaded.isFishingTrophyFishNotifierEnabled());

            Properties saved = new Properties();
            try (FileInputStream input = new FileInputStream(settingsFile)) {
                saved.load(input);
            }
            assertNull(saved.getProperty("fishing.hookTimer.enabled"));
            assertNull(saved.getProperty("fishing.biteAlert.enabled"));
            assertNull(saved.getProperty("fishing.countdown.enabled"));
            assertNull(saved.getProperty("fishing.countdown.showEta"));
            assertNull(saved.getProperty("fishing.countdown.showIncoming"));
            assertNull(saved.getProperty("fishing.countdown.slugMode"));
            assertNull(saved.getProperty("fishing.countdown.biteAlertSizePercent"));
        } finally {
            settingsFile.delete();
        }
    }

    @Test
    public void parsesTotemRemainingTimeAndFormatsDeployableCountdowns() {
        assertEquals(299, SpiderDropAlert.parseDeployableTimerSeconds("Remaining: 4m 59s"));
        assertEquals(59, SpiderDropAlert.parseDeployableTimerSeconds("Remaining: 59s"));
        assertEquals(-1, SpiderDropAlert.parseDeployableTimerSeconds("Remaining: unknown"));
        assertEquals("3m 00s", SpiderDropAlert.formatDeployableTimer(180));
        assertEquals("09s", SpiderDropAlert.formatDeployableTimer(9));
    }

    @Test
    public void totemItemUseStartsFallbackCountdownEvenWithoutVisibleArmorStands() {
        assertTrue(SpiderDropAlert.isTotemOfCorruptionItemName("Totem of Corruption"));
        assertTrue(SpiderDropAlert.isTotemOfCorruptionItemName("\u00A7dTOTEM OF CORRUPTION"));
        assertFalse(SpiderDropAlert.isTotemOfCorruptionItemName("SOS Flare"));
        assertTrue(SpiderDropAlert.isSosFlareItemName("SOS Flare"));
        assertTrue(SpiderDropAlert.isSosFlareItemName("\u00A7cSOS FLARE"));
        assertFalse(SpiderDropAlert.isSosFlareItemName("Totem of Corruption"));

        SpiderDropAlert mod = new SpiderDropAlert();
        mod.startTotemFallbackTimer(1000L);
        assertEquals(120, mod.getTotemRemainingSeconds(1000L));
        assertEquals(119, mod.getTotemRemainingSeconds(2000L));
        assertTrue(mod.isDeployablesTimerPanelVisible(1000L));

        mod.updateTotemTimerState("Remaining: 1m 58s", 2500L);
        assertEquals(118, mod.getTotemRemainingSeconds(2500L));
    }

    @Test
    public void totemAndSosTimersCountDownFromServerTextAndPlacement() {
        SpiderDropAlert mod = new SpiderDropAlert();
        mod.updateTotemTimerState("Remaining: 1m 59s", 1000L);
        assertEquals(119, mod.getTotemRemainingSeconds(1000L));
        assertEquals(117, mod.getTotemRemainingSeconds(3000L));

        mod.startSosFlareTimer(10000L);
        assertEquals(180, mod.getSosFlareRemainingSeconds(10000L));
        assertEquals(179, mod.getSosFlareRemainingSeconds(11000L));
        assertEquals(1, mod.getSosFlareRemainingSeconds(189001L));
        assertEquals(0, mod.getSosFlareRemainingSeconds(190000L));
        assertTrue(mod.isDeployablesTimerPanelVisible(189999L));
    }

    @Test
    public void plasmafluxPowerOrbStartsSixtySecondDeployableCountdown() {
        assertTrue(SpiderDropAlert.isPlasmafluxPowerOrbItemName("Plasmaflux Power Orb"));
        assertTrue(SpiderDropAlert.isPlasmafluxPowerOrbItemName("\u00A7dPLASMAFLUX POWER ORB"));
        assertFalse(SpiderDropAlert.isPlasmafluxPowerOrbItemName("Overflux Power Orb"));
        assertFalse(SpiderDropAlert.isPlasmafluxPowerOrbItemName("Plasma Nucleus"));

        SpiderDropAlert mod = new SpiderDropAlert();
        mod.startPlasmafluxTimer(10000L);
        assertEquals(60, mod.getPlasmafluxRemainingSeconds(10000L));
        assertEquals(59, mod.getPlasmafluxRemainingSeconds(11000L));
        assertEquals(1, mod.getPlasmafluxRemainingSeconds(69999L));
        assertTrue(mod.isDeployablesTimerPanelVisible(69999L));
        assertEquals(0, mod.getPlasmafluxRemainingSeconds(70000L));
        assertFalse(mod.isDeployablesTimerPanelVisible(70000L));
    }

    @Test
    public void wolfSlayerPowerOrbCatalogIncludesOnlyTheFourSupportedOrbs() {
        String[] names = {
                "Radiant Power Orb", "Mana Flux Power Orb", "Overflux Power Orb",
                "Plasmaflux Power Orb", "SOS Flare", "Totem of Corruption"
        };
        int[] durations = {30, 30, 60, 60, 180, 120};

        assertEquals(6, DeployableItem.values().length);
        for (int i = 0; i < names.length; i++) {
            DeployableItem item = DeployableItem.values()[i];
            assertEquals(names[i], item.getDisplayName());
            assertEquals(durations[i], item.getDurationSeconds());
            assertEquals(item, DeployableItem.fromDisplayName(names[i]));
        }
        assertEquals(DeployableGroup.POWER_ORBS,
                DeployableItem.RADIANT_POWER_ORB.getGroup());
        assertEquals(DeployableGroup.POWER_ORBS,
                DeployableItem.MANA_FLUX_POWER_ORB.getGroup());
        assertEquals(DeployableGroup.POWER_ORBS,
                DeployableItem.OVERFLUX_POWER_ORB.getGroup());
        assertEquals(DeployableGroup.POWER_ORBS,
                DeployableItem.PLASMAFLUX_POWER_ORB.getGroup());
        assertEquals(DeployableItem.RADIANT_POWER_ORB,
                DeployableItem.fromDisplayName("\u00A7dRADIANT POWER ORB"));
        assertEquals(DeployableItem.MANA_FLUX_POWER_ORB,
                DeployableItem.fromDisplayName("\u00A7bMana Flux Power Orb"));
        assertNull(DeployableItem.fromDisplayName("Warning Flare"));
        assertNull(DeployableItem.fromDisplayName("Small Pocket Black Hole"));
        assertNull(DeployableItem.fromDisplayName("Dwarven Lantern"));
        assertNull(DeployableItem.fromDisplayName("Will-o'-wisp"));
        assertNull(DeployableItem.fromDisplayName("Umbrella"));
        assertNull(DeployableItem.fromDisplayName("Plasma Nucleus"));
        assertNull(DeployableItem.fromDisplayName(null));
    }

    @Test
    public void everySupportedDeployableTimerCountsDownToItsExpiry() {
        SpiderDropAlert mod = new SpiderDropAlert();
        long start = 10000L;
        for (DeployableItem item : DeployableItem.values()) {
            mod.startDeployableTimer(item, start);
            assertEquals(item.getDurationSeconds(),
                    mod.getDeployableRemainingSeconds(item, start));
            assertEquals(Math.max(1, item.getDurationSeconds() - 1),
                    mod.getDeployableRemainingSeconds(item, start + 1000L));
            assertEquals(1, mod.getDeployableRemainingSeconds(item,
                    start + item.getDurationSeconds() * 1000L - 1L));
            assertEquals(0, mod.getDeployableRemainingSeconds(item,
                    start + item.getDurationSeconds() * 1000L));
            start += 1000000L;
        }
    }

    @Test
    public void legacyPlasmafluxSwitchMigratesToThePowerOrbGroup() throws Exception {
        File settingsFile = File.createTempFile("spider-drop-deployable-migration-", ".properties");
        try {
            Properties legacySettings = new Properties();
            legacySettings.setProperty("deployables.totem.enabled", "false");
            legacySettings.setProperty("deployables.sosFlare.enabled", "false");
            legacySettings.setProperty("deployables.plasmaflux.enabled", "false");
            try (FileOutputStream output = new FileOutputStream(settingsFile)) {
                legacySettings.store(output, "legacy deployable settings");
            }

            SpiderDropAlert loaded = new SpiderDropAlert(settingsFile);
            assertFalse(loaded.isDeployableGroupEnabled(DeployableGroup.TOTEM_OF_CORRUPTION));
            assertFalse(loaded.isDeployableGroupEnabled(DeployableGroup.SOS_FLARE));
            assertFalse(loaded.isDeployableGroupEnabled(DeployableGroup.POWER_ORBS));

            loaded.saveSettings();
            Properties migratedSettings = new Properties();
            try (FileInputStream input = new FileInputStream(settingsFile)) {
                migratedSettings.load(input);
            }
            assertEquals("false", migratedSettings.getProperty("deployables.totem.enabled"));
            assertEquals("false", migratedSettings.getProperty("deployables.sosFlare.enabled"));
            assertEquals("false", migratedSettings.getProperty("deployables.powerOrbs.enabled"));
        } finally {
            settingsFile.delete();
        }
    }

    private static String repeat(char c, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(c);
        }
        return builder.toString();
    }

    @Test
    public void quickNoteSearchMatchesHeadingsAndBodyText() throws Exception {
        File noteFile = File.createTempFile("spider-drop-notesearch-", ".txt");
        noteFile.delete();
        QuickNoteStore store = new QuickNoteStore(noteFile);
        store.setHeading(0, "Diana prices");
        store.setPage(0, "griffin feathers up");
        store.appendPage();
        store.setHeading(1, "Dungeon routes");
        store.setPage(1, "learn M7 crystals");
        store.appendPage();
        store.setPage(2, "buy booster cookies");

        // An empty query lists every page, so the sidebar has something to show by default.
        assertEquals(3, store.findPages("").size());
        assertEquals(3, store.findPages(null).size());

        // Headings match, case-insensitively.
        assertEquals(1, store.findPages("diana").size());
        assertEquals(0, (int) store.findPages("DIANA").get(0));

        // Body text matches too, not just headings.
        assertEquals(1, store.findPages("crystals").size());
        assertEquals(1, (int) store.findPages("crystals").get(0));

        // Several pages can match one query: "er" hits feather(s) and boost(er).
        assertEquals(2, store.findPages("er").size());
        assertTrue(store.findPages("er").contains(0));
        assertTrue(store.findPages("er").contains(2));

        // Nothing found is an empty list, never null.
        assertTrue(store.findPages("nothing here").isEmpty());

        // The sidebar preview prefers the heading, then the first real line, then a placeholder.
        assertEquals("Diana prices", store.getPreview(0));
        store.setHeading(0, "");
        assertEquals("griffin feathers up", store.getPreview(0));
        store.setPage(0, "");
        assertEquals("(empty)", store.getPreview(0));
    }

    @Test
    public void quickNoteHeadingsSurviveSavingAndReloading() throws Exception {
        File noteFile = File.createTempFile("spider-drop-notehead-", ".txt");
        noteFile.delete();
        QuickNoteStore store = new QuickNoteStore(noteFile);
        store.setHeading(0, "Diana prices");
        store.setPage(0, "griffin feathers up");
        store.appendPage();
        store.setHeading(1, "Dungeons");
        store.setPage(1, "learn M7 routes");
        store.save();

        QuickNoteStore reloaded = new QuickNoteStore(noteFile);
        assertEquals(2, reloaded.getPageCount());
        assertEquals("Diana prices", reloaded.getHeading(0));
        assertEquals("griffin feathers up", reloaded.getPage(0));
        assertEquals("Dungeons", reloaded.getHeading(1));
        assertEquals("learn M7 routes", reloaded.getPage(1));

        // A page can be renamed, and cleared back to untitled.
        reloaded.setHeading(1, "F7 routes");
        assertEquals("F7 routes", reloaded.getHeading(1));
        reloaded.setHeading(1, "");
        assertEquals("", reloaded.getHeading(1));

        // A note line that looks like the heading marker is not read back as a heading.
        reloaded.setPage(0, QuickNoteStore.PAGE_TITLE_PREFIX + "not a heading");
        reloaded.save();
        QuickNoteStore escaped = new QuickNoteStore(noteFile);
        assertEquals("Diana prices", escaped.getHeading(0));
        assertEquals(QuickNoteStore.PAGE_TITLE_PREFIX + "not a heading", escaped.getPage(0));

        // Headings fold to one line and stay short.
        assertEquals("a b", QuickNoteStore.limitHeading("a\nb"));
        assertEquals(QuickNoteStore.MAX_HEADING_LENGTH,
                QuickNoteStore.limitHeading(repeat('h', 200)).length());
    }

    @Test
    public void quickNoteStoreKeepsPagesAndSavesThemAsPlainText() throws Exception {
        File noteFile = File.createTempFile("spider-drop-notes-", ".txt");
        noteFile.delete();
        QuickNoteStore store = new QuickNoteStore(noteFile);

        assertEquals("a fresh store has one blank page", 1, store.getPageCount());
        assertEquals("", store.getPage(0));

        store.setPage(0, "buy booster cookies");
        int second = store.appendPage();
        assertTrue(second > 0);
        store.setPage(second, "diana mayor soon\ncheck prices");
        assertEquals(2, store.getPageCount());
        store.save();
        assertTrue("the file is written", noteFile.isFile());

        QuickNoteStore reloaded = new QuickNoteStore(noteFile);
        assertEquals(2, reloaded.getPageCount());
        assertEquals("buy booster cookies", reloaded.getPage(0));
        assertEquals("diana mayor soon\ncheck prices", reloaded.getPage(1));
    }

    @Test
    public void quickNoteStoreHandlesDeletingAndMarkerText() throws Exception {
        File noteFile = File.createTempFile("spider-drop-notes2-", ".txt");
        noteFile.delete();
        QuickNoteStore store = new QuickNoteStore(noteFile);
        store.setPage(0, "first");
        store.appendPage();
        store.setPage(1, "second");
        store.appendPage();
        store.setPage(2, "third");

        assertTrue(store.deletePage(1));
        assertEquals(2, store.getPageCount());
        assertEquals("first", store.getPage(0));
        assertEquals("third", store.getPage(1));

        // A note that looks like the page marker survives the round trip.
        store.setPage(1, "keep\n" + QuickNoteStore.PAGE_MARKER + "\nend");
        store.save();
        QuickNoteStore reloaded = new QuickNoteStore(noteFile);
        assertEquals(2, reloaded.getPageCount());
        assertEquals("keep\n" + QuickNoteStore.PAGE_MARKER + "\nend", reloaded.getPage(1));

        // The last remaining page is cleared rather than removed.
        assertTrue(reloaded.deletePage(0));
        assertTrue(reloaded.deletePage(0));
        assertEquals(1, reloaded.getPageCount());
        assertEquals("", reloaded.getPage(0));

        // A page is capped so typing stays responsive.
        assertEquals(QuickNoteStore.MAX_PAGE_LENGTH,
                QuickNoteStore.limit(repeat('x', QuickNoteStore.MAX_PAGE_LENGTH + 50)).length());
    }

    @Test
    public void dragonKillAlertMatchesNamedDragonsAndTheirStatues() {
        // A dragon with a readable name matches by name, wherever it happens to be.
        assertEquals(M7OverlayRenderer.M7DragonStatue.POWER,
                SpiderDropAlert.statueForDragon("Power Dragon", 0, 0, 0, false));
        assertEquals(M7OverlayRenderer.M7DragonStatue.FLAME,
                SpiderDropAlert.statueForDragon("flame dragon", 0, 0, 0, false));

        // A dragon entity with no readable name falls back to the statue it is parked at.
        BlockPos apex = M7OverlayRenderer.M7DragonStatue.APEX.anchor;
        assertEquals(M7OverlayRenderer.M7DragonStatue.APEX,
                SpiderDropAlert.statueForDragon(null, apex.getX() + 0.5, apex.getY(),
                        apex.getZ() + 0.5, true));
        // ...but only when it is actually at a statue.
        assertNull(SpiderDropAlert.statueForDragon(null, 400, 14, 400, true));
        // A named non-dragon entity is never treated as one.
        assertNull(SpiderDropAlert.statueForDragon("Some Zombie", apex.getX() + 0.5,
                apex.getY(), apex.getZ() + 0.5, false));
    }

    @Test
    public void dragonKillAlertFiresOnlyCloseToThePlayerAtItsOwnStatue() {
        BlockPos power = M7OverlayRenderer.M7DragonStatue.POWER.anchor;
        double px = power.getX() + 0.5;
        double py = power.getY();
        double pz = power.getZ() + 0.5;

        List<SpiderDropAlert.DragonSighting> atStatue = new ArrayList<SpiderDropAlert.DragonSighting>();
        atStatue.add(new SpiderDropAlert.DragonSighting(M7OverlayRenderer.M7DragonStatue.POWER,
                px + 4, py, pz + 3));
        assertEquals("close to the player and at its own statue",
                M7OverlayRenderer.M7DragonStatue.POWER,
                SpiderDropAlert.pickDragonKillTarget(px, py, pz, atStatue));

        // Same dragon, but the player is far away.
        assertNull(SpiderDropAlert.pickDragonKillTarget(px + 40, py, pz, atStatue));

        // Close to the player, but parked at someone else's statue.
        List<SpiderDropAlert.DragonSighting> wrongStatue =
                new ArrayList<SpiderDropAlert.DragonSighting>();
        wrongStatue.add(new SpiderDropAlert.DragonSighting(M7OverlayRenderer.M7DragonStatue.POWER,
                M7OverlayRenderer.M7DragonStatue.ICE.anchor.getX() + 0.5, py,
                M7OverlayRenderer.M7DragonStatue.ICE.anchor.getZ() + 0.5));
        assertNull(SpiderDropAlert.pickDragonKillTarget(
                M7OverlayRenderer.M7DragonStatue.ICE.anchor.getX() + 0.5, py,
                M7OverlayRenderer.M7DragonStatue.ICE.anchor.getZ() + 0.5, wrongStatue));

        // Two dragons in range: the one nearer the player is called out.
        BlockPos apex = M7OverlayRenderer.M7DragonStatue.APEX.anchor;
        List<SpiderDropAlert.DragonSighting> both =
                new ArrayList<SpiderDropAlert.DragonSighting>();
        both.add(new SpiderDropAlert.DragonSighting(M7OverlayRenderer.M7DragonStatue.POWER,
                px + 10, py, pz));
        both.add(new SpiderDropAlert.DragonSighting(M7OverlayRenderer.M7DragonStatue.APEX,
                apex.getX() + 0.5, py, apex.getZ() + 0.5));
        assertEquals(M7OverlayRenderer.M7DragonStatue.POWER,
                SpiderDropAlert.pickDragonKillTarget(px, py, pz, both));
    }

    @Test
    public void dragonKillAlertTitleUsesEachDragonsColour() {
        assertEquals("\u00A7c", SpiderDropAlert.dragonAlertColorCode(
                M7OverlayRenderer.M7DragonStatue.POWER));
        assertEquals("\u00A7a", SpiderDropAlert.dragonAlertColorCode(
                M7OverlayRenderer.M7DragonStatue.APEX));
        assertEquals("\u00A7d", SpiderDropAlert.dragonAlertColorCode(
                M7OverlayRenderer.M7DragonStatue.SOUL));
        assertEquals("\u00A7b", SpiderDropAlert.dragonAlertColorCode(
                M7OverlayRenderer.M7DragonStatue.ICE));
        assertEquals("\u00A76", SpiderDropAlert.dragonAlertColorCode(
                M7OverlayRenderer.M7DragonStatue.FLAME));
    }

    @Test
    public void layoutEditorCornerDragMapsPixelsToPanelSize() {
        // Two pixels of corner travel is one 1% step, in either axis, across the 1%-100% range.
        assertEquals(50, HudLayoutEditorScreen.resizedPercent(50, 0));
        assertEquals(50, HudLayoutEditorScreen.resizedPercent(50, 1));
        assertEquals(51, HudLayoutEditorScreen.resizedPercent(50, 2));
        assertEquals(85, HudLayoutEditorScreen.resizedPercent(70, 30));
        // The range clamps at 100% up and 1% down, and starts at 100% of the normal size.
        assertEquals(100, HudLayoutEditorScreen.resizedPercent(100, 2));
        assertEquals(100, HudLayoutEditorScreen.resizedPercent(70, 500));
        assertEquals(1, HudLayoutEditorScreen.resizedPercent(70, -500));
        assertEquals(45, HudLayoutEditorScreen.resizedPercent(100, -110));
        // Sizes far below the old 70% floor are reachable now.
        assertEquals(25, HudLayoutEditorScreen.resizedPercent(100, -150));
        // No travel means no change, so a drag that returns to its start leaves the size alone.
        assertEquals(73, HudLayoutEditorScreen.resizedPercent(73, 0));
    }

    @Test
    public void allCustomHudPanelsHideWhileTabChatOrLayoutEditorIsActive() {
        assertTrue(SpiderDropAlert.shouldRenderCustomHudPanels(false, false, false));
        assertFalse(SpiderDropAlert.shouldRenderCustomHudPanels(true, false, false));
        assertFalse(SpiderDropAlert.shouldRenderCustomHudPanels(false, true, false));
        assertFalse(SpiderDropAlert.shouldRenderCustomHudPanels(false, false, true));
        assertFalse(SpiderDropAlert.shouldRenderCustomHudPanels(true, true, true));
    }

    @Test
    public void jawbusTitleTriggersOnlyOnTheLocalLordJawbusChatLine() {
        assertTrue(SpiderDropAlert.isLordJawbusAlertMessage(
                "You have angered a legendary creature... Lord Jawbus has arrived."));
        assertTrue(SpiderDropAlert.isLordJawbusAlertMessage(
                "\u00A7c\u00A7lYou have angered a legendary creature... Lord Jawbus has arrived."));
        assertFalse(SpiderDropAlert.isLordJawbusAlertMessage(
                "You have angered a legendary creature."));
        assertFalse(SpiderDropAlert.isLordJawbusAlertMessage(
                "[MVP+] Fisher: You have angered a legendary creature... Lord Jawbus has arrived."));
        assertFalse(SpiderDropAlert.isLordJawbusAlertMessage(
                "Party > [MVP+] Fisher: --> A Lord Jawbus has spawned <--"));
        assertFalse(SpiderDropAlert.isLordJawbusAlertMessage(
                "Party > [MVP+] Fisher: DOUBLE HOOK: I caught a Lord Jawbus!"));
        assertFalse(SpiderDropAlert.isLordJawbusAlertMessage(
                "Party > [MVP+] Fisher: Lord Jawbus is a rare sea creature."));
    }

    @Test
    public void nearbyCreatureHpParsesLegendaryAndMythicNameplates() {
        SpiderDropAlert.NearbyCreatureHpEntry jawbus = SpiderDropAlert.parseNearbyCreatureHpLabel(
                "\u00A7eSea Creatures HP\n\u00A77[Lvl 600] \u00A7cLord Jawbus \u00A7a76M/100M\u00A7c\u2764 >");
        assertEquals("Lord Jawbus", jawbus.creatureName);
        assertEquals(600, jawbus.level);
        assertEquals("76M", jawbus.currentHealth);
        assertEquals("100M", jawbus.maxHealth);
        assertEquals("[Lvl 600] Lord Jawbus 76M/100M\u2764",
                SpiderDropAlert.stripFormattingCodes(jawbus.toHudLabel()));

        SpiderDropAlert.NearbyCreatureHpEntry thunder = SpiderDropAlert.parseNearbyCreatureHpLabel(
                "[Lvl 400] Thunder 34M/35M\u2764 >");
        assertEquals("Thunder", thunder.creatureName);
        assertEquals("34M", thunder.currentHealth);
        assertEquals("35M", thunder.maxHealth);

        SpiderDropAlert.NearbyCreatureHpEntry reindrake = SpiderDropAlert.parseNearbyCreatureHpLabel(
                "[Lvl 100] Reindrake 543/2,500\u2764");
        assertEquals("Reindrake", reindrake.creatureName);
        assertEquals(100, reindrake.level);
        assertEquals("543", reindrake.currentHealth);
        assertEquals("2,500", reindrake.maxHealth);
        assertEquals("Titanoboa", SpiderDropAlert.parseNearbyCreatureHpLabel(
                "[Lvl 240] Titanoboa 45M/45M\u2764").creatureName);
        assertEquals("Frog Prince", SpiderDropAlert.parseNearbyCreatureHpLabel(
                "[Lvl 132] Frog Prince 10M/10M\u2764").creatureName);
        SpiderDropAlert.NearbyCreatureHpEntry seaEmperor =
                SpiderDropAlert.parseNearbyCreatureHpLabel(
                        "[Lvl 200] Sea Emperor 4.8M/5M\u2764");
        assertEquals("Sea Emperor", seaEmperor.creatureName);
        assertEquals("4.8M", seaEmperor.currentHealth);
        assertEquals("The Sea Emperor", SpiderDropAlert.parseNearbyCreatureHpLabel(
                "[Lvl 200] The Sea Emperor 4.8M/5M\u2764").creatureName);

        assertNull(SpiderDropAlert.parseNearbyCreatureHpLabel(
                "[Lvl 100] Sea Walker 1,000/1,500\u2764"));
        assertEquals("1.5M", SpiderDropAlert.formatNearbyCreatureHpValue(1500000.0D));
        assertEquals("2,500", SpiderDropAlert.formatNearbyCreatureHpValue(2500.0D));
    }

    @Test
    public void nearbyCreatureHpRecognizesEveryLegendaryNameplateWithRarityPrefix() {
        String[] legendaryCreatures = {
                "Water Hydra", "The Loch Emperor", "Alligator", "Abyssal Miner",
                "Blue Ringed Octopus", "Fiery Scuttler", "Great White Shark",
                "Phantom Fisher", "Puddle Jumper", "Silkbreeze", "Thunder", "Yeti"
        };
        for (String creatureName : legendaryCreatures) {
            SpiderDropAlert.NearbyCreatureHpEntry entry =
                    SpiderDropAlert.parseNearbyCreatureHpLabel(
                            "[LEGENDARY] [Lvl 150] " + creatureName + " 1M/1M\u2764");
            assertTrue("Should accept " + creatureName, entry != null);
        }
    }

    @Test
    public void nearbyCreatureHpTogglePositionAndScalePersistIndependently() throws Exception {
        File settingsFile = File.createTempFile("spider-drop-creature-hp-", ".properties");
        try {
            SpiderDropAlert mod = new SpiderDropAlert(settingsFile);
            assertTrue(mod.isNearbyCreatureHpEnabled());
            mod.moveNearbyCreatureHpPanelTo(120, 90, 854, 480);
            mod.adjustNearbyCreatureHpSize(20, 854, 480);
            mod.toggleNearbyCreatureHp();
            assertFalse(mod.isNearbyCreatureHpEnabled());
            assertTrue(mod.isTrackerEnabled());
            assertFalse(mod.isFishingMobCounterEnabled());

            SpiderDropAlert loaded = new SpiderDropAlert(settingsFile);
            assertFalse(loaded.isNearbyCreatureHpEnabled());
            assertEquals(120, loaded.getNearbyCreatureHpX());
            assertEquals(90, loaded.getNearbyCreatureHpY());
            assertEquals(100, loaded.getNearbyCreatureHpSizePercent());
            assertTrue(loaded.isTrackerEnabled());

            loaded.adjustNearbyCreatureHpSize(500, 854, 480);
            assertEquals(SpiderDropAlert.MAX_HUD_SIZE_PERCENT,
                    loaded.getNearbyCreatureHpSizePercent());
            loaded.adjustNearbyCreatureHpSize(-500, 854, 480);
            assertEquals(SpiderDropAlert.MIN_HUD_SIZE_PERCENT,
                    loaded.getNearbyCreatureHpSizePercent());
            // The range reaches small panels that the old 70% floor could not express.
            loaded.adjustNearbyCreatureHpSize(24, 854, 480);
            assertEquals(25, loaded.getNearbyCreatureHpSizePercent());
            loaded.moveNearbyCreatureHpPanelTo(854, 480, 854, 480);
            assertEquals(854 - loaded.getRenderedNearbyCreatureHpPanelWidth(854),
                    loaded.getNearbyCreatureHpX());
            assertEquals(480 - loaded.getRenderedNearbyCreatureHpPanelHeight(),
                    loaded.getNearbyCreatureHpY());
        } finally {
            settingsFile.delete();
        }
    }

    @Test
    public void deployablesPanelSettingsPersistIndependently() throws Exception {
        File settingsFile = File.createTempFile("spider-drop-deployables-", ".properties");
        try {
            SpiderDropAlert mod = new SpiderDropAlert(settingsFile);
            mod.moveDeployablesTimerPanelTo(120, 90, 854, 480);
            mod.adjustDeployablesTimerPanelSize(20, 854, 480);
            mod.toggleJawbusAlert();
            assertFalse(mod.isJawbusAlertEnabled());
            assertTrue(mod.isTrackerEnabled());
            assertFalse(mod.isFishingMobCounterEnabled());

            SpiderDropAlert loaded = new SpiderDropAlert(settingsFile);
            assertEquals(120, loaded.getDeployablesTimerPanelX());
            assertEquals(90, loaded.getDeployablesTimerPanelY());
            assertEquals(100, loaded.getDeployablesTimerPanelSizePercent());
            assertFalse(loaded.isJawbusAlertEnabled());
            assertTrue(loaded.isDeployablesTimerPanelEnabled());

            loaded.toggleDeployablesTimerPanel();
            assertFalse(loaded.isDeployablesTimerPanelEnabled());
            assertTrue(loaded.isTrackerEnabled());
        } finally {
            settingsFile.delete();
        }
    }

    @Test
    public void slayerBossTimerRunsFromSpawnUntilQuestCompletion() {
        SpiderDropAlert mod = new SpiderDropAlert();
        mod.startSlayerBossTimer(SlayerType.SPIDER, 1000L);
        assertTrue(mod.hasSlayerBossTimer());
        assertTrue(mod.isSlayerBossTimerActive());
        assertEquals(4000L, mod.getSlayerBossTimerElapsedMillis(5000L));

        // Duplicate boss-dialogue lines for the same boss must not reset the start time.
        mod.startSlayerBossTimer(SlayerType.SPIDER, 5000L);
        assertEquals(7000L, mod.getSlayerBossTimerElapsedMillis(8000L));

        mod.completeSlayerBossTimer(8200L);
        assertFalse(mod.isSlayerBossTimerActive());
        assertEquals(7200L, mod.getSlayerBossTimerElapsedMillis(30000L));

        mod.startSlayerBossTimer(SlayerType.ENDERMAN, 40000L);
        assertEquals(SlayerType.SPIDER, mod.getActiveSlayer());
        assertEquals(5000L, mod.getSlayerBossTimerElapsedMillis(45000L));
    }

    @Test
    public void fishingBoxAppearsWhenEnabledAndCanShowOnlyTimerBeforeCatch() throws Exception {
        File settingsFile = File.createTempFile("spider-drop-timer-", ".properties");
        try {
            SpiderDropAlert mod = new SpiderDropAlert(settingsFile);
            mod.toggleFishingMobCounter();
            assertTrue(mod.isFishingSectionVisible());
            assertEquals(0L, mod.getTotalLegendaryCreatureCount());
            mod.updateFishingTimerState(true, 1000L);
            assertTrue(mod.isFishingSectionVisible());
            mod.clearFishingHistory();
            assertTrue(mod.isFishingSectionVisible());
            mod.toggleFishingMobCounter();
            assertFalse(mod.isFishingSectionVisible());
        } finally {
            settingsFile.delete();
        }
    }

    @Test
    public void slayerAndFishingHudTogglesOperateIndependently() throws Exception {
        File settingsFile = File.createTempFile("spider-drop-toggle-", ".properties");
        try {
            SpiderDropAlert mod = new SpiderDropAlert(settingsFile);
            assertTrue(mod.isTrackerEnabled());
            assertFalse(mod.isFishingMobCounterEnabled());

            mod.toggleFishingMobCounter();
            assertTrue(mod.isFishingMobCounterEnabled());
            assertTrue(mod.isFishingSectionVisible());
            assertTrue(mod.isTrackerEnabled());

            mod.toggleTracker();
            assertFalse(mod.isTrackerEnabled());
            assertTrue(mod.isFishingSectionVisible());

            mod.updateFishingTimerState(true, 1000L);
            mod.toggleTracker();
            assertTrue(mod.isTrackerEnabled());
            assertTrue(mod.isFishingTimerActive());

            mod.toggleFishingMobCounter();
            assertTrue(mod.isTrackerEnabled());
            assertFalse(mod.isFishingSectionVisible());
            assertFalse(mod.isFishingTimerActive());
        } finally {
            settingsFile.delete();
        }
    }

    @Test
    public void clearsOnlyTheSelectedSlayerHistory() {
        SpiderDropAlert mod = new SpiderDropAlert();
        mod.recordDrop(SlayerType.SPIDER, "Tarantula Silk", 64L);
        mod.recordDrop(SlayerType.ENDERMAN, "Judgement Core", 1L);

        assertEquals(SlayerType.ENDERMAN, mod.getActiveSlayer());
        assertEquals(1, mod.getDropHistorySize(SlayerType.SPIDER));
        assertEquals(1, mod.getDropHistorySize(SlayerType.ENDERMAN));

        mod.clearHistory(SlayerType.ENDERMAN);

        assertEquals(0, mod.getDropHistorySize(SlayerType.ENDERMAN));
        assertEquals(1, mod.getDropHistorySize(SlayerType.SPIDER));
        assertEquals(SlayerType.ENDERMAN, mod.getActiveSlayer());
    }

    @Test
    public void recognizesAllLegendaryCreatureAnnouncementMarkersOnlyAtLineStart() {
        assertEquals(13, LegendaryCreature.values().length);
        for (LegendaryCreature creature : LegendaryCreature.values()) {
            assertEquals(creature, DropMessageParser.detectLegendaryCreature(creature.chatPrefix + "."));
        }
        assertEquals(LegendaryCreature.THE_LOCH_EMPEROR,
                DropMessageParser.detectLegendaryCreature(
                        "The Sea Emperor arises from the depths."));
        assertEquals(LegendaryCreature.THE_LOCH_EMPEROR,
                DropMessageParser.detectLegendaryCreature(
                        "\u00A7eThe Sea Emperor arises from the depths."));
        assertEquals(LegendaryCreature.THE_LOCH_EMPEROR,
                DropMessageParser.detectLegendaryCreature(
                        "Sea Emperor arises from the depths!"));
        assertEquals(LegendaryCreature.THE_LOCH_EMPEROR,
                DropMessageParser.detectLegendaryCreature(
                        "A Sea Emperor has appeared!"));
        assertNull(DropMessageParser.detectLegendaryCreature(
                "[MVP+] Player: The Sea Emperor arises from the depths."));
        assertEquals(LegendaryCreature.WATER_HYDRA,
                DropMessageParser.detectLegendaryCreature(
                        "\u00A7eThe Water Hydra has come to test your strength."));
        assertEquals(LegendaryCreature.LORD_JAWBUS,
                DropMessageParser.detectLegendaryCreature(
                        "You have angered a legendary creature... Lord Jawbus has arrived."));
        assertNull(DropMessageParser.detectLegendaryCreature(
                "You have angered a legendary creature... another creature has arrived."));
        assertEquals("Water Hydra-1", SpiderDropAlert.formatLegendaryCreatureLabel(
                LegendaryCreature.WATER_HYDRA, 1L));
        assertNull(DropMessageParser.detectLegendaryCreature(
                "[MVP+] Player: The Water Hydra has come to test your strength."));
        assertNull(DropMessageParser.detectLegendaryCreature("You caught a Sea Walker!"));
    }

    @Test
    public void feasibleFeeshFeaturesUseOnlySupportedLocalLegendaryAnnouncements() throws Exception {
        File settingsFile = File.createTempFile("spider-drop-feesh-extras-", ".properties");
        try {
            SpiderDropAlert mod = new SpiderDropAlert(settingsFile);
            assertFalse(mod.isFishingEfficiencyTrackerEnabled());
            assertFalse(mod.isFishingFestivalTrackerEnabled());
            assertFalse(mod.isFishingCreatureAlertEnabled());
            assertFalse(mod.isFishingRareCreatureHighlightEnabled());
            assertFalse(mod.isFishingSectionVisible());

            mod.toggleFishingEfficiencyTracker();
            mod.toggleFishingFestivalTracker();
            mod.toggleFishingCreatureAlert();
            mod.toggleFishingRareCreatureHighlight();
            assertTrue(mod.isFishingSectionVisible());
            assertTrue(mod.shouldRenderFishingRareCreatureHighlights());

            mod.updateFishingTimerState(true, 1000L);
            assertTrue(mod.processFishingAnnouncement(
                    LegendaryCreature.WATER_HYDRA.chatPrefix + ".", 1000L));
            assertEquals(1L, mod.getFishingEfficiencyCatchCount());
            assertEquals(0L, mod.getTotalLegendaryCreatureCount());
            assertEquals(0L, mod.getFishingFestivalGreatWhiteBest());
            assertFalse(mod.processFishingAnnouncement(
                    "[MVP+] Player: " + LegendaryCreature.WATER_HYDRA.chatPrefix, 2000L));
            assertFalse(mod.processFishingAnnouncement("You caught a Sea Walker!", 3000L));

            assertTrue(mod.processFishingAnnouncement(
                    LegendaryCreature.GREAT_WHITE_SHARK.chatPrefix + ".", 4000L));
            assertEquals(2L, mod.getFishingEfficiencyCatchCount());
            assertEquals(1L, mod.getFishingFestivalGreatWhiteCount(4000L));
            assertEquals(1L, mod.getFishingFestivalGreatWhiteBest());

            mod.toggleFishingMobCounter();
            assertTrue(mod.processFishingAnnouncement(
                    LegendaryCreature.THE_LOCH_EMPEROR.chatPrefix + ".", 5000L));
            assertEquals(1L, mod.getLegendaryCreatureCount(LegendaryCreature.THE_LOCH_EMPEROR));
            assertTrue(mod.isFishingMobCounterEnabled());
            assertTrue(mod.isFishingEfficiencyTrackerEnabled());
            assertTrue(mod.isFishingFestivalTrackerEnabled());
            assertTrue(mod.isFishingCreatureAlertEnabled());
            assertTrue(mod.isFishingRareCreatureHighlightEnabled());

            SpiderDropAlert loaded = new SpiderDropAlert(settingsFile);
            assertEquals(1L, loaded.getFishingFestivalGreatWhiteBest());
            assertTrue(loaded.isFishingEfficiencyTrackerEnabled());
            assertTrue(loaded.isFishingFestivalTrackerEnabled());
            assertTrue(loaded.isFishingCreatureAlertEnabled());
            assertTrue(loaded.isFishingRareCreatureHighlightEnabled());

            mod.clearFishingHistory();
            assertEquals(0L, mod.getFishingEfficiencyCatchCount());
            assertEquals(0L, mod.getFishingFestivalGreatWhiteBest());
            assertTrue(mod.isFishingSectionVisible());
        } finally {
            settingsFile.delete();
        }
    }

    @Test
    public void fishingFestivalEndDetectionRequiresTheLocalSystemLine() {
        String endLine = "FISHING FESTIVAL The festival has concluded! Time to dry off and repair your rods!";
        assertTrue(SpiderDropAlert.isFishingFestivalEndMessage(endLine));
        assertTrue(SpiderDropAlert.isFishingFestivalEndMessage("\u00A7e" + endLine));
        assertFalse(SpiderDropAlert.isFishingFestivalEndMessage("[MVP+] Player: " + endLine));
        assertFalse(SpiderDropAlert.isFishingFestivalEndMessage("The festival has concluded!"));
        assertTrue(SpiderDropAlert.shouldShowGenericFishingCreatureAlert(
                LegendaryCreature.GREAT_WHITE_SHARK));
        assertFalse(SpiderDropAlert.shouldShowGenericFishingCreatureAlert(
                LegendaryCreature.LORD_JAWBUS));
        assertFalse(SpiderDropAlert.shouldShowGenericFishingCreatureAlert(null));
    }

    @Test
    public void rareCreatureOutlinesUseTheExistingLegendaryAndMythicNameplateFilter() {
        assertTrue(SpiderDropAlert.parseNearbyCreatureHpLabel("[Lv180] Great White Shark LEGENDARY") != null);
        assertTrue(SpiderDropAlert.parseNearbyCreatureHpLabel("[Lv600] Lord Jawbus MYTHIC") != null);
        assertNull(SpiderDropAlert.parseNearbyCreatureHpLabel("[Lv6] Nurse Shark UNCOMMON"));
        assertNull(SpiderDropAlert.parseNearbyCreatureHpLabel("A random zombie"));
    }

    @Test
    public void totalsOnlyLegendaryCatchesAndFishingClearIsIndependent() throws Exception {
        File settingsFile = File.createTempFile("spider-drop-alert-test-", ".properties");
        try {
            SpiderDropAlert mod = new SpiderDropAlert(settingsFile);
            assertEquals(0, mod.getCaughtLegendaryCreatureCount());
            assertFalse(mod.isFishingSectionVisible());
            mod.toggleFishingMobCounter();
            assertTrue(mod.isFishingMobCounterEnabled());
            assertTrue(mod.isFishingSectionVisible());
            mod.recordDrop(SlayerType.SPIDER, "Tarantula Silk", 64L);
            mod.recordLegendaryCreature(LegendaryCreature.THE_LOCH_EMPEROR);
            mod.recordLegendaryCreature(LegendaryCreature.THE_LOCH_EMPEROR);
            mod.recordLegendaryCreature(LegendaryCreature.THE_LOCH_EMPEROR);
            for (int i = 0; i < 4; i++) {
                mod.recordLegendaryCreature(LegendaryCreature.WATER_HYDRA);
            }
            for (int i = 0; i < 47; i++) {
                mod.recordLegendaryCreature(LegendaryCreature.YETI);
            }

            assertEquals(3L, mod.getLegendaryCreatureCount(
                    LegendaryCreature.THE_LOCH_EMPEROR));
            assertEquals(4L, mod.getLegendaryCreatureCount(
                    LegendaryCreature.WATER_HYDRA));
            assertEquals(54L, mod.getTotalLegendaryCreatureCount());
            assertEquals(3, mod.getCaughtLegendaryCreatureCount());
            assertTrue(mod.isFishingSectionVisible());

            mod.recordLegendaryCreature(LegendaryCreature.LORD_JAWBUS);
            assertEquals(1L, mod.getLegendaryCreatureCount(
                    LegendaryCreature.LORD_JAWBUS));
            assertEquals(55L, mod.getTotalLegendaryCreatureCount());
            assertEquals(4, mod.getCaughtLegendaryCreatureCount());

            mod.clearHistory(SlayerType.SPIDER);
            assertEquals(0, mod.getDropHistorySize(SlayerType.SPIDER));
            assertEquals(55L, mod.getTotalLegendaryCreatureCount());

            mod.recordDrop(SlayerType.SPIDER, "Tarantula Silk", 1L);
            mod.clearFishingHistory();
            assertEquals(0L, mod.getTotalLegendaryCreatureCount());
            assertEquals(0, mod.getCaughtLegendaryCreatureCount());
            assertTrue(mod.isFishingSectionVisible());
            mod.toggleFishingMobCounter();
            assertFalse(mod.isFishingSectionVisible());
            assertEquals(1, mod.getDropHistorySize(SlayerType.SPIDER));
        } finally {
            settingsFile.delete();
        }
    }

    @Test
    public void fishingBoxHasIndependentPositionAndUsesSpaceBesideOrBelowTracker() throws Exception {
        File wideSettings = File.createTempFile("spider-drop-wide-", ".properties");
        File narrowSettings = File.createTempFile("spider-drop-narrow-", ".properties");
        try {
            SpiderDropAlert wide = new SpiderDropAlert(wideSettings);
            wide.clampFishingTrackerPosition(854, 480);
            assertEquals(262, wide.getFishingX());
            assertEquals(60, wide.getFishingY());

            wide.moveFishingTrackerTo(500, 120, 854, 480);
            assertEquals(500, wide.getFishingX());
            assertEquals(120, wide.getFishingY());
            assertEquals(10, wide.getTrackerX());
            assertEquals(60, wide.getTrackerY());

            SpiderDropAlert narrow = new SpiderDropAlert(narrowSettings);
            narrow.clampFishingTrackerPosition(420, 300);
            assertEquals(10, narrow.getFishingX());
            assertEquals(136, narrow.getFishingY());
        } finally {
            wideSettings.delete();
            narrowSettings.delete();
        }
    }

    @Test
    public void normalizesNamesForManualPrices() {
        assertEquals("tarantula talisman", DropMessageParser.normalizeItemName("  Tarantula   Talisman  "));
        assertEquals("tarantula talisman", DropMessageParser.normalizeItemName("\u00A76Tarantula Talisman"));
        assertEquals("tarantula silk", DropMessageParser.normalizeItemName("64x Tarantula Silk"));
        assertEquals("tarantula silk", DropMessageParser.normalizeItemName("Tarantula Silk (x64)"));
        assertEquals("1,000,000", SpiderDropAlert.formatPrice(1000000L));
    }


    @Test
    public void extractsStackQuantityAndCleansItemName() {
        assertEquals(64L, DropMessageParser.extractDropQuantity("RARE DROP! Tarantula Silk (64x)"));
        assertEquals("Tarantula Silk", DropMessageParser.extractDropItem("RARE DROP! Tarantula Silk (64x)"));
        assertEquals(20L, DropMessageParser.extractDropQuantity(
                "VERY RARE DROP! 20x Tarantula Silk (+25% Magic Find)"));
        assertEquals("Tarantula Silk", DropMessageParser.extractDropItem(
                "VERY RARE DROP! 20x Tarantula Silk (+25% Magic Find)"));
    }

    @Test
    public void extractsParenthesizedToxicArrowPoisonStacks() {
        String message = "RARE DROP! 1x (64x Toxic Arrow Poison)";
        assertEquals("Toxic Arrow Poison", DropMessageParser.extractDropItem(message));
        assertEquals(64L, DropMessageParser.extractDropQuantity(message));
        assertEquals("64x Toxic Arrow Poison(7.04k)",
                SpiderDropAlert.formatDropLabel("Toxic Arrow Poison", 64L,
                        PriceSetCommand.parsePrice("110")));
        assertEquals("64x Toxic Arrow Poison(7.04m)",
                SpiderDropAlert.formatDropLabel("Toxic Arrow Poison", 64L,
                        PriceSetCommand.parsePrice("110k")));
    }

    @Test
    public void normalizesParenthesizedCountAndItemFormatting() {
        String message = "RARE DROP! (322) x(Toxic Arrow Poison)";
        assertEquals(322L, DropMessageParser.extractDropQuantity(message));
        assertEquals("Toxic Arrow Poison", DropMessageParser.extractDropItem(message));
        assertEquals("434x Toxic Arrow Poison(83m)",
                SpiderDropAlert.formatDropLabel("Toxic Arrow Poison", 434L, 191244L));
    }

    @Test
    public void computesAndFormatsStackTotals() {
        long unitPrice = PriceSetCommand.parsePrice("1,9k");
        assertEquals(1900L, unitPrice);
        assertEquals(38000L, SpiderDropAlert.calculateTotalPrice(unitPrice, 20L));
        assertEquals(128L, SpiderDropAlert.addDropQuantities(64L, 64L));
        assertEquals("38k", SpiderDropAlert.formatCompactPrice(
                SpiderDropAlert.calculateTotalPrice(unitPrice, 20L)));
        assertEquals("1x Tarantula Silk(1.9k)",
                SpiderDropAlert.formatDropLabel("Tarantula Silk", 1L, unitPrice));
        assertEquals("2x Tarantula Silk(2 coins)",
                SpiderDropAlert.formatDropLabel("Tarantula Silk", 2L, 1L));
        assertEquals("20x Tarantula Silk(38k)",
                SpiderDropAlert.formatDropLabel("Tarantula Silk", 20L, unitPrice));
        assertEquals("64x Tarantula Silk(121.6k)",
                SpiderDropAlert.formatDropLabel("Tarantula Silk", 64L, unitPrice));
        assertEquals("64x Tarantula Silk(price not set)",
                SpiderDropAlert.formatDropLabel("Tarantula Silk", 64L, null));
    }

    @Test
    public void totalsVisiblePricedDropValuesAndIgnoresUnpricedRows() {
        long total = 0L;
        total = SpiderDropAlert.addPricedDropValue(total, 1900L, 20L);
        total = SpiderDropAlert.addPricedDropValue(total, null, 8L);
        total = SpiderDropAlert.addPricedDropValue(total, 110000L, 64L);
        assertEquals(7078000L, total);
        assertEquals("7.08m", SpiderDropAlert.formatCompactPrice(total));
    }

    @Test
    public void totalProfitMatchesSumOfExampleRowValues() {
        long total = 0L;
        total = SpiderDropAlert.addPricedDropValue(total, 320000L, 1L);
        total = SpiderDropAlert.addPricedDropValue(total, 499000L, 1L);
        assertEquals(819000L, total);
        assertEquals("819k", SpiderDropAlert.formatCompactPrice(total));
    }

    @Test
    public void supportsSetPriceAsAlias() {
        SpiderDropAlert mod = new SpiderDropAlert();
        PriceSetCommand primary = new PriceSetCommand(mod);
        PriceSetCommand alias = new PriceSetCommand(mod, "setprice");
        assertEquals("priceset", primary.getCommandName());
        assertEquals("setprice", alias.getCommandName());
        assertTrue(alias.getCommandUsage(null).startsWith("/setprice "));
    }

    @Test
    public void parsesCompactManualPrices() {
        assertEquals(110000L, PriceSetCommand.parsePrice("110k"));
        assertEquals(1500000L, PriceSetCommand.parsePrice("1.5m"));
        assertEquals(1900L, PriceSetCommand.parsePrice("1,9k"));
        assertEquals(1000000000L, PriceSetCommand.parsePrice("1b"));
        assertEquals(1234567L, PriceSetCommand.parsePrice("1,234,567"));
    }
}

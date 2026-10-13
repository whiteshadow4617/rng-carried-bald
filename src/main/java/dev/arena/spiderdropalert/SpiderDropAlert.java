package dev.arena.spiderdropalert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

/** Client-side SkyBlock tracker with Slayer/Fishing HUDs and M7 world overlays. */
@Mod(
        modid = SpiderDropAlert.MOD_ID,
        name = SpiderDropAlert.MOD_NAME,
        version = SpiderDropAlert.VERSION,
        acceptedMinecraftVersions = "[1.8.9]",
        clientSideOnly = true
)
public final class SpiderDropAlert {
    public static final String MOD_ID = "spiderdropalert";
    public static final String MOD_NAME = "RNG Carried Bald";
    public static final String VERSION = "1.0.65";

    static final int HUD_PANEL_SLAYER = 0;
    static final int HUD_PANEL_FISHING = 1;
    static final int HUD_PANEL_CREATURE_HP = 2;
    static final int HUD_PANEL_DEPLOYABLES = 3;

    /** HUD panel size range in percent, shared by every panel: 1% up to 100% of its normal size. */
    static final int MIN_HUD_SIZE_PERCENT = 1;
    static final int MAX_HUD_SIZE_PERCENT = 100;
    static final int MIN_HUD_BACKGROUND_OPACITY_PERCENT = 1;
    static final int MAX_HUD_BACKGROUND_OPACITY_PERCENT = 100;
    private static final int DEFAULT_HUD_BACKGROUND_OPACITY_PERCENT = 35;

    static final int PANEL_WIDTH = 240;
    static final int HEADER_HEIGHT = 18;
    static final int ITEM_ONLY_ENTRY_HEIGHT = 12;
    private static final int MAX_TRACKED_DROPS = 4;
    private static final long DUPLICATE_DROP_WINDOW_MS = 750L;
    private static final String PRICE_PROPERTY_PREFIX = "price.";
    private static final String LEGENDARY_COUNT_PROPERTY_PREFIX = "fishing.creature.";
    private static final String LEGACY_LAST_PRICE_ITEM_PROPERTY = "price.lastItem";
    private static final String FISHING_FESTIVAL_BEST_PROPERTY = "fishing.festival.greatWhiteBest";
    private static final long BOBBER_PULL_ARM_DELAY_MS = 1200L;
    private static final long BOBBER_PULL_SAMPLE_WINDOW_MS = 350L;
    private static final double BOBBER_PULL_DOWN_THRESHOLD = -0.12D;
    private static final double BOBBER_PULL_DISPLACEMENT_EPSILON = 0.000001D;
    private static final double FISHING_SPLASH_SOUND_RANGE_BLOCKS = 3.5D;
    private static final long DEPLOYABLE_SCAN_INTERVAL_MS = 1000L;
    private static final long DEPLOYABLE_INTERACTION_DEDUPE_WINDOW_MS = 500L;
    /** How close a dragon has to be to you before the kill alert can fire. */
    static final double DRAGON_KILL_ALERT_PLAYER_RANGE = 20.0D;
    /** How close that dragon has to be to its own statue anchor. */
    static final double DRAGON_KILL_ALERT_STATUE_RANGE = 15.0D;
    /** The title stays armed this long after the last sighting, so it cannot flicker. */
    private static final long DRAGON_KILL_ALERT_HOLD_MS = 900L;
    /** Re-issues the title no faster than this while the same dragon stays in range. */
    private static final long DRAGON_KILL_ALERT_RETITLE_MS = 1200L;
    private static final long JAWBUS_ALERT_DEDUPE_WINDOW_MS = 3000L;
    static final int DEPLOYABLE_PANEL_WIDTH = 270;
    private static final int NEARBY_CREATURE_HP_PANEL_WIDTH = 340;
    private static final int MAX_NEARBY_CREATURE_HP_ROWS = 8;
    private static final double NEARBY_CREATURE_HP_RANGE_BLOCKS = 64.0D;
    private static final long NEARBY_CREATURE_HP_SCAN_INTERVAL_MS = 500L;
    private static final String NEARBY_CREATURE_HP_PROPERTY_PREFIX = "nearbyCreatureHp.";
    private static final String[] TRACKED_NEARBY_SEA_CREATURES = {
            "The Loch Emperor", "Sea Emperor", "The Sea Emperor", "Loch Emperor",
            "Water Hydra", "Alligator", "Abyssal Miner",
            "Blue Ringed Octopus", "Fiery Scuttler", "Great White Shark", "Phantom Fisher",
            "Puddle Jumper", "Silkbreeze", "Thunder", "Yeti", "Lord Jawbus",
            "Nessie", "Grim Reaper", "Reindrake", "Ragnarok", "Plhlegblast",
            "Wiki Tiki", "Giant Isopod", "Titanoboa", "Frog Prince"
    };

    private static final Pattern DEPLOYABLE_REMAINING_TIME = Pattern.compile(
            "(?i)(?:(\\d+)\\s*m\\s*)?(\\d+)\\s*s");
    private static final Pattern NEARBY_CREATURE_LEVEL = Pattern.compile(
            "(?i)\\[\\s*LVL?\\s*(\\d+)\\s*\\]");
    private static final Pattern NEARBY_CREATURE_HEALTH = Pattern.compile(
            "(?i)([0-9][0-9,]*(?:\\.[0-9]+)?\\s*[KMBT]?)\\s*/\\s*"
                    + "([0-9][0-9,]*(?:\\.[0-9]+)?\\s*[KMBT]?)\\s*[\\u2764\\u2665]?");

    private final Map<SlayerType, List<DropEntry>> dropHistory =
            new EnumMap<SlayerType, List<DropEntry>>(SlayerType.class);
    private final Map<String, Long> manualPrices = new HashMap<String, Long>();
    private final Map<LegendaryCreature, Long> legendaryCreatureCounts =
            new EnumMap<LegendaryCreature, Long>(LegendaryCreature.class);
    private final List<NearbyCreatureHpEntry> nearbyCreatureHpEntries =
            new ArrayList<NearbyCreatureHpEntry>();
    private SlayerType activeSlayer = SlayerType.SPIDER;
    private String lastProcessedDropKey = "";
    private long lastProcessedDropAt;
    private String lastProcessedFishingCreature = "";
    private long lastProcessedFishingAt;

    private boolean fishingBobberWasActive;
    private boolean fishingTimerActive;
    private boolean fishingTimerHasValue;
    private long fishingTimerStartedAt;
    private long lastFishingDurationMillis;

    private boolean splashToPullTimerActive;
    private boolean splashToPullTimerHasValue;
    private long splashToPullTimerStartedAt;
    private long lastSplashToPullDurationMillis;
    private int splashToPullBobberEntityId = -1;
    private double previousSplashToPullBobberY;
    private double splashToPullSampleWindowStartY;
    private long splashToPullSampleWindowStartedAt;
    private long splashToPullBobberObservedAt;

    private boolean slayerBossTimerActive;
    private boolean slayerBossTimerHasValue;
    private SlayerType slayerBossTimerType;
    private long slayerBossTimerStartedAt;
    private long lastSlayerBossDurationMillis;

    private boolean jawbusAlertEnabled = true;
    private boolean m7DragonKillAlertEnabled = true;
    private boolean fishingCreatureAlertEnabled;
    private boolean fishingLegendaryNotifierEnabled;
    private boolean fishingTrophyFishNotifierEnabled;
    private boolean fishingEfficiencyTrackerEnabled;
    private boolean fishingFestivalTrackerEnabled;
    private boolean fishingRareCreatureHighlightEnabled;
    private long lastJawbusAlertAt;
    private long dragonKillAlertUntil;
    private long dragonKillAlertTitleAt;
    private M7OverlayRenderer.M7DragonStatue dragonKillAlertTarget;
    private final FishingSessionTracker fishingSessionTracker = new FishingSessionTracker();
    private World lastFishingSessionWorld;

    private boolean deployablesTimerPanelEnabled = true;
    private final EnumMap<DeployableGroup, Boolean> deployableGroupEnabled =
            new EnumMap<DeployableGroup, Boolean>(DeployableGroup.class);
    private final EnumMap<DeployableItem, Long> deployableExpiresAt =
            new EnumMap<DeployableItem, Long>(DeployableItem.class);
    private final EnumMap<DeployableItem, Long> lastDeployableInteractionAt =
            new EnumMap<DeployableItem, Long>(DeployableItem.class);
    private boolean nearbyCreatureHpEnabled = true;
    private boolean m7OverlayEnabled = true;
    private boolean m7MaxorEnabled = true;
    private boolean m7StormEnabled = true;
    private boolean m7GoldorEnabled = true;
    private boolean m7NecronEnabled = true;
    private boolean m7PowerDragonEnabled = true;
    private boolean m7ApexDragonEnabled = true;
    private boolean m7SoulDragonEnabled = true;
    private boolean m7IceDragonEnabled = true;
    private boolean m7FlameDragonEnabled = true;
    private boolean m7StatueZonesEnabled = true;
    private boolean m7AreasPersistAfterDragonSpawn = true;
    private final M7OverlayRenderer m7OverlayRenderer = new M7OverlayRenderer(this);
    private final HudPanelRenderer hudPanelRenderer = new HudPanelRenderer(this);
    private int nearbyCreatureHpX = 10;
    private int nearbyCreatureHpY = 145;
    private boolean nearbyCreatureHpPositionConfigured;
    private int nearbyCreatureHpSizePercent = 100;
    private long nextNearbyCreatureHpScanAt;
    private World lastNearbyCreatureHpWorld;
    private int deployablesTimerPanelX = 10;
    private int deployablesTimerPanelY = 200;
    private boolean deployablesTimerPanelPositionConfigured;
    private int deployablesTimerPanelSizePercent = 100;
    private int totemRemainingSeconds = -1;
    private long totemTimerObservedAt;
    private long nextDeployableScanAt;
    private World lastDeployablesWorld;

    private boolean trackerEnabled = true;
    private boolean fishingMobCounterEnabled;
    private int trackerX = 10;
    private int trackerY = 60;
    private int fishingX = 270;
    private int fishingY = 60;
    private boolean fishingPositionConfigured;
    private int trackerSizePercent = 100;
    private int trackerBackgroundOpacityPercent = DEFAULT_HUD_BACKGROUND_OPACITY_PERCENT;
    private int fishingBackgroundOpacityPercent = DEFAULT_HUD_BACKGROUND_OPACITY_PERCENT;
    private int nearbyCreatureHpBackgroundOpacityPercent = DEFAULT_HUD_BACKGROUND_OPACITY_PERCENT;
    private int deployablesBackgroundOpacityPercent = DEFAULT_HUD_BACKGROUND_OPACITY_PERCENT;
    private boolean trackerGuiPending;
    private boolean quickNotePending;
    private QuickNoteStore quickNoteStore;
    private File settingsFile;

    public SpiderDropAlert() {
    }

    SpiderDropAlert(File settingsFile) {
        this.settingsFile = settingsFile;
        loadSettings();
    }

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        loadSettings();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(m7OverlayRenderer);
        FMLCommonHandler.instance().bus().register(this);
        ClientCommandHandler.instance.registerCommand(new PriceSetCommand(this));
        ClientCommandHandler.instance.registerCommand(new PriceSetCommand(this, "setprice"));
        ClientCommandHandler.instance.registerCommand(new SpiderTrackerCommand(this));
        ClientCommandHandler.instance.registerCommand(new TrackerGuiCommand(this));
        ClientCommandHandler.instance.registerCommand(new QuickNoteCommand(this));
    }

    /** Opens the settings screen after the chat screen has finished closing. */
    void requestTrackerGui() {
        trackerGuiPending = true;
    }

    /** Opens the quick note pad after the chat screen has finished closing. */
    void requestQuickNote() {
        quickNotePending = true;
    }

    QuickNoteStore getQuickNoteStore() {
        if (quickNoteStore == null) {
            quickNoteStore = new QuickNoteStore(getQuickNoteFile());
        }
        return quickNoteStore;
    }

    int getQuickNotePageCount() {
        return getQuickNoteStore().getPageCount();
    }

    void resetQuickNotes() {
        getQuickNoteStore().resetAll();
        getQuickNoteStore().save();
    }

    private File getQuickNoteFile() {
        File settings = getSettingsFile();
        File folder = settings.getParentFile();
        return new File(folder == null ? new File(".") : folder, "spiderdropalert-notes.txt");
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        updateFishingTimerFromClient();
        long now = System.currentTimeMillis();
        updateDeployableTimersFromClient(now);
        updateNearbyCreatureHpFromClient(now);
        updateDragonKillAlertFromClient(now);
        if (quickNotePending) {
            quickNotePending = false;
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft != null) {
                minecraft.displayGuiScreen(new QuickNoteScreen(this, getQuickNoteStore(),
                        minecraft.currentScreen));
            }
        }
        if (!trackerGuiPending) {
            return;
        }
        trackerGuiPending = false;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null) {
            minecraft.displayGuiScreen(new TrackerConfigScreen(this,
                    minecraft.currentScreen));
        }
    }

    /** Starts a Slayer timer when a supported boss entity enters the client world. */
    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event == null || event.world == null || !event.world.isRemote || event.entity == null) {
            return;
        }
        if (!trackerEnabled || !(event.entity instanceof EntityLivingBase)) {
            return;
        }
        String displayName = event.entity.getDisplayName().getUnformattedText();
        SlayerType boss = DropMessageParser.detectSlayerBossName(displayName);
        if (boss != null) {
            startSlayerBossTimer(boss, System.currentTimeMillis());
        }
    }

    /** Tracks local use of supported deployables; it never automates gameplay. */
    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event == null || event.world == null || !event.world.isRemote
                || (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_AIR
                && event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.thePlayer == null
                || event.entityPlayer != minecraft.thePlayer) {
            return;
        }
        ItemStack heldItem = event.entityPlayer.getHeldItem();
        if (heldItem == null) {
            return;
        }
        DeployableItem deployable = DeployableItem.fromDisplayName(heldItem.getDisplayName());
        if (deployable == null || !isDeployableGroupEnabled(deployable.getGroup())) {
            return;
        }

        long now = System.currentTimeMillis();
        Long lastInteraction = lastDeployableInteractionAt.get(deployable);
        if (lastInteraction != null && now >= lastInteraction.longValue()
                && now - lastInteraction.longValue() < DEPLOYABLE_INTERACTION_DEDUPE_WINDOW_MS) {
            return;
        }
        lastDeployableInteractionAt.put(deployable, now);
        // Start on the local right-click: Hypixel may not expose a consistent projectile
        // or placed-entity event for every deployable type.
        startDeployableTimer(deployable, now);
    }

    /** Watches incoming chat only; it never sends messages or automates gameplay. */
    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.message == null) {
            return;
        }

        String message = event.message.getUnformattedText();
        if (message == null) {
            return;
        }
        message = message.replace('\n', ' ').trim();

        long now = System.currentTimeMillis();
        boolean fakepixelSystemMessage = event.type == 1 && isFakepixelServer();
        if (fakepixelSystemMessage && fishingLegendaryNotifierEnabled) {
            String legendaryCatch = NefFishingFeatureParser.detectLegendaryCatch(message);
            if (legendaryCatch != null) {
                showFakepixelLegendaryCatchTitle(legendaryCatch);
            }
        }
        if (fakepixelSystemMessage && fishingTrophyFishNotifierEnabled) {
            String trophyFish = NefFishingFeatureParser.detectTrophyFish(message);
            if (trophyFish != null) {
                showFakepixelTrophyFishTitle(trophyFish);
            }
        }
        if (isFishingFestivalEndMessage(message)) {
            fishingSessionTracker.endFestival();
        }
        if (event.type == 1 && jawbusAlertEnabled && isLordJawbusAlertMessage(message)
                && (lastJawbusAlertAt == 0L || now - lastJawbusAlertAt >= JAWBUS_ALERT_DEDUPE_WINDOW_MS)) {
            showLordJawbusTitle();
            lastJawbusAlertAt = now;
        }
        if (isSosFlareRemovedMessage(message)) {
            resetDeployableTimer(DeployableItem.SOS_FLARE);
        }
        if (trackerEnabled) {
            SlayerType spawnedBoss = DropMessageParser.detectSlayerBossSpawnMessage(message);
            if (spawnedBoss != null) {
                startSlayerBossTimer(spawnedBoss, now);
            }
            if (DropMessageParser.isSlayerQuestCompleteMessage(message)) {
                completeSlayerBossTimer(now);
            }
        }

        processFishingAnnouncement(message, now);

        if (!trackerEnabled) {
            return;
        }

        SlayerType detectedSlayer = DropMessageParser.detectSlayer(message);
        if (detectedSlayer == null) {
            return;
        }

        String item = DropMessageParser.extractDropItem(message);
        if (item.length() == 0) {
            item = "Unknown item";
        }
        long quantity = DropMessageParser.extractDropQuantity(message);
        String dropKey = detectedSlayer.name() + "|" + DropMessageParser.normalizeItemName(item) + "|" + quantity;
        long elapsed = now - lastProcessedDropAt;
        if (dropKey.equals(lastProcessedDropKey) && elapsed >= 0L
                && elapsed <= DUPLICATE_DROP_WINDOW_MS) {
            return;
        }
        lastProcessedDropKey = dropKey;
        lastProcessedDropAt = now;

        recordDrop(detectedSlayer, item, quantity);
    }

    boolean processFishingAnnouncement(String message, long now) {
        if (!fishingMobCounterEnabled && !fishingEfficiencyTrackerEnabled
                && !fishingFestivalTrackerEnabled && !fishingCreatureAlertEnabled) {
            return false;
        }
        LegendaryCreature creature = DropMessageParser.detectLegendaryCreature(message);
        if (creature == null) {
            return false;
        }

        String creatureKey = creature.name();
        long elapsed = now - lastProcessedFishingAt;
        if (creatureKey.equals(lastProcessedFishingCreature) && elapsed >= 0L
                && elapsed <= DUPLICATE_DROP_WINDOW_MS) {
            return false;
        }
        lastProcessedFishingCreature = creatureKey;
        lastProcessedFishingAt = now;

        if (fishingMobCounterEnabled) {
            recordLegendaryCreature(creature);
        }
        if (fishingEfficiencyTrackerEnabled) {
            fishingSessionTracker.recordTrackedCatch(now);
        }
        long previousFestivalBest = fishingSessionTracker.getBestFestivalGreatWhiteCount();
        if (fishingFestivalTrackerEnabled && creature == LegendaryCreature.GREAT_WHITE_SHARK) {
            fishingSessionTracker.recordGreatWhiteCatch(now);
        }
        boolean festivalBestChanged = fishingSessionTracker.getBestFestivalGreatWhiteCount()
                > previousFestivalBest;

        if (fishingCreatureAlertEnabled && shouldShowGenericFishingCreatureAlert(creature)
                && !(isFakepixelServer() && fishingLegendaryNotifierEnabled)) {
            showFishingCreatureTitle(creature);
        }
        if (fishingMobCounterEnabled || festivalBestChanged) {
            saveSettings();
        }
        return true;
    }

    private void showFishingCreatureTitle(LegendaryCreature creature) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.ingameGUI != null) {
            minecraft.ingameGUI.displayTitle("\u00A7b\u00A7lSEA CREATURE",
                    "\u00A7e" + creature.displayName, 1, 45, 10);
        }
        if (minecraft != null && minecraft.thePlayer != null) {
            minecraft.thePlayer.playSound("random.orb", 0.8F, 1.15F);
        }
    }

    void recordDrop(SlayerType slayer, String item, long quantity) {
        if (slayer == null) {
            return;
        }
        activeSlayer = slayer;
        addOrStackDrop(slayer, item, quantity);
    }

    private void addOrStackDrop(SlayerType slayer, String item, long quantity) {
        List<DropEntry> history = getDropHistory(slayer);
        String key = DropMessageParser.normalizeItemName(item);
        for (int i = 0; i < history.size(); i++) {
            DropEntry existing = history.get(i);
            if (DropMessageParser.normalizeItemName(existing.item).equals(key)) {
                long combinedQuantity = addDropQuantities(existing.quantity, quantity);
                history.remove(i);
                history.add(0, new DropEntry(item, combinedQuantity));
                return;
            }
        }

        history.add(0, new DropEntry(item, quantity));
        while (history.size() > MAX_TRACKED_DROPS) {
            history.remove(history.size() - 1);
        }
    }

    List<DropEntry> getDropHistory(SlayerType slayer) {
        List<DropEntry> history = dropHistory.get(slayer);
        if (history == null) {
            history = new ArrayList<DropEntry>();
            dropHistory.put(slayer, history);
        }
        return history;
    }

    int getDropHistorySize(SlayerType slayer) {
        List<DropEntry> history = dropHistory.get(slayer);
        return history == null ? 0 : history.size();
    }

    void recordLegendaryCreature(LegendaryCreature creature) {
        if (creature == null) {
            return;
        }
        legendaryCreatureCounts.put(creature,
                addDropQuantities(getLegendaryCreatureCount(creature), 1L));
    }

    long getLegendaryCreatureCount(LegendaryCreature creature) {
        Long count = legendaryCreatureCounts.get(creature);
        return count == null ? 0L : count;
    }

    long getTotalLegendaryCreatureCount() {
        long total = 0L;
        for (Long count : legendaryCreatureCounts.values()) {
            total = addDropQuantities(total, count == null ? 0L : count);
        }
        return total;
    }

    int getCaughtLegendaryCreatureCount() {
        int caught = 0;
        for (LegendaryCreature creature : LegendaryCreature.values()) {
            if (getLegendaryCreatureCount(creature) > 0L) {
                caught++;
            }
        }
        return caught;
    }

    boolean isFishingSectionVisible() {
        return fishingMobCounterEnabled || fishingEfficiencyTrackerEnabled
                || fishingFestivalTrackerEnabled;
    }

    boolean hasFishingTimer() {
        return fishingTimerHasValue;
    }

    boolean isFishingTimerActive() {
        return fishingTimerActive;
    }

    long getFishingTimerElapsedMillis(long now) {
        if (!fishingTimerHasValue) {
            return 0L;
        }
        return fishingTimerActive
                ? Math.max(0L, now - fishingTimerStartedAt) : lastFishingDurationMillis;
    }

    void updateFishingTimerState(boolean bobberActive, long now) {
        if (bobberActive && fishingEfficiencyTrackerEnabled) {
            fishingSessionTracker.startSession(now);
        }
        if (bobberActive && !fishingBobberWasActive) {
            fishingTimerStartedAt = now;
            fishingTimerActive = true;
            fishingTimerHasValue = true;
        } else if (!bobberActive && fishingBobberWasActive && fishingTimerActive) {
            lastFishingDurationMillis = Math.max(0L, now - fishingTimerStartedAt);
            fishingTimerActive = false;
        }
        fishingBobberWasActive = bobberActive;
    }

    private void updateFishingTimerFromClient() {
        Minecraft minecraft = Minecraft.getMinecraft();
        World world = minecraft == null ? null : minecraft.theWorld;
        if (world != lastFishingSessionWorld) {
            lastFishingSessionWorld = world;
            fishingSessionTracker.resetSession();
        }
        boolean localBobberPresent = minecraft != null && minecraft.thePlayer != null
                && minecraft.thePlayer.fishEntity != null;
        boolean bobberActive = fishingMobCounterEnabled && localBobberPresent;
        updateFishingTimerState(bobberActive, System.currentTimeMillis());
        if (fishingEfficiencyTrackerEnabled && localBobberPresent) {
            fishingSessionTracker.startSession(System.currentTimeMillis());
        }
    }

    private boolean isFakepixelServer() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft != null && minecraft.getCurrentServerData() != null
                && NefFishingFeatureParser.isFakepixelServerAddress(
                minecraft.getCurrentServerData().serverIP);
    }

    private boolean isFakepixelTrophyFishingArea() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.theWorld == null) {
            return false;
        }
        Scoreboard scoreboard = minecraft.theWorld.getScoreboard();
        ScoreObjective sidebar = scoreboard == null ? null : scoreboard.getObjectiveInDisplaySlot(1);
        if (sidebar == null) {
            return false;
        }
        StringBuilder lines = new StringBuilder();
        for (Score score : scoreboard.getSortedScores(sidebar)) {
            String playerName = score.getPlayerName();
            ScorePlayerTeam team = scoreboard.getPlayersTeam(playerName);
            lines.append(ScorePlayerTeam.formatPlayerName(team, playerName)).append(' ');
        }
        return NefFishingFeatureParser.isTrophyFishingArea(lines.toString());
    }

    private void showFakepixelLegendaryCatchTitle(String creatureName) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.ingameGUI != null) {
            minecraft.ingameGUI.displayTitle("\u00A76Legendary Catch!", "\u00A7e" + creatureName,
                    2, 70, 2);
        }
    }

    private void showFakepixelTrophyFishTitle(String fishName) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.ingameGUI != null) {
            minecraft.ingameGUI.displayTitle("TROPHY FISH", fishName, 1, 20, 1);
        }
    }

    @SubscribeEvent
    public void onFishingSplashSound(PlaySoundEvent event) {
        if (!fishingMobCounterEnabled || event == null || event.sound == null
                || !isFishingSplashSound(event.name)) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityFishHook hook = minecraft == null || minecraft.thePlayer == null
                ? null : minecraft.thePlayer.fishEntity;
        if (hook == null || hook.getEntityId() != splashToPullBobberEntityId) {
            return;
        }
        if (!isFishingSplashNearBobber(event.name,
                event.sound.getXPosF(), event.sound.getYPosF(), event.sound.getZPosF(),
                hook.posX, hook.posY, hook.posZ)) {
            return;
        }
        startSplashToPullTimer(hook.getEntityId(), System.currentTimeMillis());
    }

    void updateSplashToPullTimerState(boolean bobberActive, int entityId,
                                      double bobberY, long now) {
        if (!bobberActive) {
            splashToPullBobberEntityId = -1;
            previousSplashToPullBobberY = 0.0D;
            splashToPullSampleWindowStartY = 0.0D;
            splashToPullSampleWindowStartedAt = 0L;
            splashToPullBobberObservedAt = 0L;
            if (splashToPullTimerActive) {
                splashToPullTimerActive = false;
                splashToPullTimerHasValue = false;
                splashToPullTimerStartedAt = 0L;
                lastSplashToPullDurationMillis = 0L;
            }
            return;
        }

        if (splashToPullBobberEntityId != entityId) {
            splashToPullBobberEntityId = entityId;
            previousSplashToPullBobberY = bobberY;
            splashToPullSampleWindowStartY = bobberY;
            splashToPullSampleWindowStartedAt = now;
            splashToPullBobberObservedAt = now;
            splashToPullTimerActive = false;
            splashToPullTimerHasValue = false;
            splashToPullTimerStartedAt = 0L;
            lastSplashToPullDurationMillis = 0L;
            return;
        }

        double verticalChange = bobberY - previousSplashToPullBobberY;
        long elapsedSinceCast = Math.max(0L, now - splashToPullBobberObservedAt);
        long sampleWindowElapsed = now - splashToPullSampleWindowStartedAt;
        double windowVerticalChange = bobberY - splashToPullSampleWindowStartY;
        if (sampleWindowElapsed < 0L || sampleWindowElapsed > BOBBER_PULL_SAMPLE_WINDOW_MS) {
            splashToPullSampleWindowStartY = previousSplashToPullBobberY;
            splashToPullSampleWindowStartedAt = now;
            windowVerticalChange = verticalChange;
        }
        previousSplashToPullBobberY = bobberY;

        boolean pulled = isBobberPulledDown(verticalChange, elapsedSinceCast)
                || isBobberPulledDown(windowVerticalChange, elapsedSinceCast);
        if (pulled && splashToPullTimerActive) {
            lastSplashToPullDurationMillis = Math.max(0L, now - splashToPullTimerStartedAt);
            splashToPullTimerActive = false;
            splashToPullTimerHasValue = true;
        }
    }

    boolean startSplashToPullTimer(int entityId, long now) {
        if (entityId < 0 || entityId != splashToPullBobberEntityId
                || now - splashToPullBobberObservedAt < BOBBER_PULL_ARM_DELAY_MS
                || splashToPullTimerActive || splashToPullTimerHasValue) {
            return false;
        }
        splashToPullTimerStartedAt = now;
        lastSplashToPullDurationMillis = 0L;
        splashToPullTimerActive = true;
        splashToPullTimerHasValue = true;
        return true;
    }

    boolean hasSplashToPullTimer() {
        return splashToPullTimerHasValue;
    }

    boolean isSplashToPullTimerActive() {
        return splashToPullTimerActive;
    }

    long getSplashToPullTimerElapsedMillis(long now) {
        if (!splashToPullTimerHasValue) {
            return 0L;
        }
        return splashToPullTimerActive
                ? Math.max(0L, now - splashToPullTimerStartedAt)
                : lastSplashToPullDurationMillis;
    }

    private void updateDeployableTimersFromClient(long now) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.thePlayer == null || minecraft.theWorld == null) {
            resetDeployableTrackingState();
            lastDeployablesWorld = null;
            return;
        }

        if (lastDeployablesWorld != minecraft.theWorld) {
            resetDeployableTrackingState();
            lastDeployablesWorld = minecraft.theWorld;
            nextDeployableScanAt = 0L;
        }
        if (now < nextDeployableScanAt) {
            return;
        }
        nextDeployableScanAt = now + DEPLOYABLE_SCAN_INTERVAL_MS;
        if (isDeployableGroupEnabled(DeployableGroup.TOTEM_OF_CORRUPTION)) {
            updateTotemRemainingTime(minecraft, now);
        } else {
            resetTotemRemainingTime();
        }
        for (DeployableItem item : DeployableItem.values()) {
            if (item != DeployableItem.TOTEM_OF_CORRUPTION
                    && getDeployableRemainingSeconds(item, now) <= 0) {
                deployableExpiresAt.remove(item);
            }
        }
    }

    private void updateNearbyCreatureHpFromClient(long now) {
        if (!nearbyCreatureHpEnabled) {
            nearbyCreatureHpEntries.clear();
            nextNearbyCreatureHpScanAt = 0L;
            lastNearbyCreatureHpWorld = null;
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.theWorld == null || minecraft.thePlayer == null) {
            nearbyCreatureHpEntries.clear();
            nextNearbyCreatureHpScanAt = 0L;
            lastNearbyCreatureHpWorld = null;
            return;
        }
        if (lastNearbyCreatureHpWorld != minecraft.theWorld) {
            nearbyCreatureHpEntries.clear();
            lastNearbyCreatureHpWorld = minecraft.theWorld;
            nextNearbyCreatureHpScanAt = 0L;
        }
        if (now < nextNearbyCreatureHpScanAt) {
            return;
        }
        nextNearbyCreatureHpScanAt = now + NEARBY_CREATURE_HP_SCAN_INTERVAL_MS;

        List<NearbyCreatureHpEntry> detected = new ArrayList<NearbyCreatureHpEntry>();
        double maxDistanceSq = NEARBY_CREATURE_HP_RANGE_BLOCKS * NEARBY_CREATURE_HP_RANGE_BLOCKS;
        for (Object loadedEntity : minecraft.theWorld.loadedEntityList) {
            if (!(loadedEntity instanceof EntityLivingBase)) {
                continue;
            }
            EntityLivingBase living = (EntityLivingBase) loadedEntity;
            if (living == minecraft.thePlayer || living.isDead) {
                continue;
            }
            double distanceSq = living.getDistanceSqToEntity(minecraft.thePlayer);
            if (distanceSq > maxDistanceSq) {
                continue;
            }
            String displayName = living.getDisplayName() == null ? ""
                    : living.getDisplayName().getUnformattedText();
            NearbyCreatureHpEntry parsed = parseNearbyCreatureHpLabel(
                    displayName, living.getEntityId(), distanceSq);
            String customName = living.getCustomNameTag();
            if (customName != null && customName.length() > 0
                    && (parsed == null || parsed.currentHealth.length() == 0
                    || parsed.maxHealth.length() == 0)) {
                NearbyCreatureHpEntry customParsed = parseNearbyCreatureHpLabel(
                        customName, living.getEntityId(), distanceSq);
                if (customParsed != null) {
                    parsed = customParsed;
                }
            }
            if (parsed == null) {
                continue;
            }
            if (parsed.currentHealth.length() == 0 || parsed.maxHealth.length() == 0) {
                if (living instanceof EntityArmorStand) {
                    continue;
                }
                float health = living.getHealth();
                float maxHealth = living.getMaxHealth();
                if (Float.isNaN(health) || Float.isInfinite(health)
                        || Float.isNaN(maxHealth) || Float.isInfinite(maxHealth) || maxHealth <= 0.0F) {
                    continue;
                }
                parsed = new NearbyCreatureHpEntry(parsed.creatureName, parsed.level,
                        formatNearbyCreatureHpValue(health), formatNearbyCreatureHpValue(maxHealth),
                        living.getEntityId(), distanceSq);
            }
            detected.add(parsed);
        }
        Collections.sort(detected, new Comparator<NearbyCreatureHpEntry>() {
            @Override
            public int compare(NearbyCreatureHpEntry first, NearbyCreatureHpEntry second) {
                return Double.compare(first.distanceSq, second.distanceSq);
            }
        });
        if (detected.size() > MAX_NEARBY_CREATURE_HP_ROWS) {
            detected = new ArrayList<NearbyCreatureHpEntry>(
                    detected.subList(0, MAX_NEARBY_CREATURE_HP_ROWS));
        }
        nearbyCreatureHpEntries.clear();
        nearbyCreatureHpEntries.addAll(detected);
    }

    private void updateTotemRemainingTime(Minecraft minecraft, long now) {
        EntityArmorStand ownerStand = null;
        String playerName = minecraft.thePlayer.getName();
        for (Object loadedEntity : minecraft.theWorld.loadedEntityList) {
            if (!(loadedEntity instanceof EntityArmorStand)) {
                continue;
            }
            EntityArmorStand armorStand = (EntityArmorStand) loadedEntity;
            String name = stripFormattingCodes(armorStand.getCustomNameTag());
            if (name.contains("Owner:") && name.toLowerCase(Locale.ROOT)
                    .contains(playerName.toLowerCase(Locale.ROOT))) {
                ownerStand = armorStand;
                break;
            }
        }
        if (ownerStand == null) {
            preserveTotemFallbackUntilExpiry(now);
            return;
        }

        Entity possibleTotem = minecraft.theWorld.getEntityByID(ownerStand.getEntityId() - 2);
        Entity possibleRemaining = minecraft.theWorld.getEntityByID(ownerStand.getEntityId() - 1);
        if (!(possibleTotem instanceof EntityArmorStand)
                || !(possibleRemaining instanceof EntityArmorStand)) {
            preserveTotemFallbackUntilExpiry(now);
            return;
        }
        String totemName = stripFormattingCodes(
                ((EntityArmorStand) possibleTotem).getCustomNameTag()).trim();
        String remainingName = stripFormattingCodes(
                ((EntityArmorStand) possibleRemaining).getCustomNameTag()).trim();
        if (!"Totem of Corruption".equalsIgnoreCase(totemName)
                || !remainingName.toLowerCase(Locale.ROOT).contains("remaining:")) {
            preserveTotemFallbackUntilExpiry(now);
            return;
        }

        updateTotemTimerState(remainingName, now);
    }

    private void preserveTotemFallbackUntilExpiry(long now) {
        if (getTotemRemainingSeconds(now) <= 0) {
            resetTotemRemainingTime();
        }
    }

    void startTotemFallbackTimer(long now) {
        totemRemainingSeconds = DeployableItem.TOTEM_OF_CORRUPTION.getDurationSeconds();
        totemTimerObservedAt = now;
    }

    void updateTotemTimerState(String remainingName, long now) {
        int seconds = parseDeployableTimerSeconds(remainingName);
        if (seconds <= 0) {
            resetTotemRemainingTime();
            return;
        }
        totemRemainingSeconds = seconds;
        totemTimerObservedAt = now;
    }

    private void resetTotemRemainingTime() {
        totemRemainingSeconds = -1;
        totemTimerObservedAt = 0L;
    }

    private void resetDeployableTimer(DeployableItem item) {
        if (item == null) {
            return;
        }
        if (item == DeployableItem.TOTEM_OF_CORRUPTION) {
            resetTotemRemainingTime();
        } else {
            deployableExpiresAt.remove(item);
        }
    }

    private void resetDeployableGroupTimers(DeployableGroup group) {
        for (DeployableItem item : DeployableItem.values()) {
            if (item.getGroup() == group) {
                resetDeployableTimer(item);
            }
        }
    }

    private void resetDeployableTrackingState() {
        resetTotemRemainingTime();
        deployableExpiresAt.clear();
        lastDeployableInteractionAt.clear();
    }

    /** A dragon seen this tick together with the statue it belongs to. */
    static final class DragonSighting {
        final M7OverlayRenderer.M7DragonStatue statue;
        final double x;
        final double y;
        final double z;

        DragonSighting(M7OverlayRenderer.M7DragonStatue statue, double x, double y, double z) {
            this.statue = statue;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /**
     * Works out which statue a dragon entity belongs to. A name that matches one of the five M7
     * dragons wins; a dragon entity with no readable name falls back to the statue anchor it is
     * parked at, so the alert still works whichever way the server exposes it.
     */
    static M7OverlayRenderer.M7DragonStatue statueForDragon(String entityName, double x, double y,
                                                            double z, boolean dragonEntity) {
        if (entityName != null) {
            for (M7OverlayRenderer.M7DragonStatue statue
                    : M7OverlayRenderer.M7DragonStatue.values()) {
                if (entityName.equalsIgnoreCase(statue.dragonName)) {
                    return statue;
                }
            }
        }
        if (!dragonEntity) {
            return null;
        }
        M7OverlayRenderer.M7DragonStatue nearest = null;
        double bestDistance = DRAGON_KILL_ALERT_STATUE_RANGE * DRAGON_KILL_ALERT_STATUE_RANGE;
        for (M7OverlayRenderer.M7DragonStatue statue
                : M7OverlayRenderer.M7DragonStatue.values()) {
            double distance = distanceSqToAnchor(statue, x, y, z);
            if (distance <= bestDistance) {
                bestDistance = distance;
                nearest = statue;
            }
        }
        return nearest;
    }

    /**
     * The dragon to call out: one that is both close to the player and sitting at its own statue.
     * When several qualify the closest to the player wins, so the call-out follows the real threat.
     */
    static M7OverlayRenderer.M7DragonStatue pickDragonKillTarget(double playerX, double playerY,
                                                                 double playerZ,
                                                                 List<DragonSighting> sightings) {
        M7OverlayRenderer.M7DragonStatue best = null;
        double bestDistance = DRAGON_KILL_ALERT_PLAYER_RANGE * DRAGON_KILL_ALERT_PLAYER_RANGE;
        for (DragonSighting sighting : sightings) {
            double dx = sighting.x - playerX;
            double dy = sighting.y - playerY;
            double dz = sighting.z - playerZ;
            double toPlayer = dx * dx + dy * dy + dz * dz;
            if (toPlayer > DRAGON_KILL_ALERT_PLAYER_RANGE
                    * DRAGON_KILL_ALERT_PLAYER_RANGE) {
                continue;
            }
            if (distanceSqToAnchor(sighting.statue, sighting.x, sighting.y, sighting.z)
                    > DRAGON_KILL_ALERT_STATUE_RANGE * DRAGON_KILL_ALERT_STATUE_RANGE) {
                continue;
            }
            if (toPlayer <= bestDistance) {
                bestDistance = toPlayer;
                best = sighting.statue;
            }
        }
        return best;
    }

    private static double distanceSqToAnchor(M7OverlayRenderer.M7DragonStatue statue,
                                             double x, double y, double z) {
        double dx = statue.anchor.getX() + 0.5D - x;
        double dy = statue.anchor.getY() - y;
        double dz = statue.anchor.getZ() + 0.5D - z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Calls out the dragon that is close to you and parked at its own statue, and keeps the title
     * up while that stays true. Only runs during a Floor VII / M7 run, so it cannot fire anywhere
     * else a dragon might exist.
     */
    private void updateDragonKillAlertFromClient(long now) {
        if (!m7DragonKillAlertEnabled || !isFloorSevenScoreboardDetected()) {
            dragonKillAlertTarget = null;
            dragonKillAlertUntil = 0L;
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.theWorld == null || minecraft.thePlayer == null) {
            return;
        }
        List<DragonSighting> sightings = new ArrayList<DragonSighting>();
        for (Object loadedEntity : minecraft.theWorld.loadedEntityList) {
            if (!(loadedEntity instanceof Entity)) {
                continue;
            }
            Entity entity = (Entity) loadedEntity;
            boolean isDragonEntity = entity instanceof EntityDragon;
            String name = entity instanceof EntityLivingBase ? entity.getName() : null;
            M7OverlayRenderer.M7DragonStatue statue = statueForDragon(name, entity.posX,
                    entity.posY, entity.posZ, isDragonEntity);
            if (statue != null) {
                sightings.add(new DragonSighting(statue, entity.posX, entity.posY, entity.posZ));
            }
        }
        M7OverlayRenderer.M7DragonStatue target = pickDragonKillTarget(minecraft.thePlayer.posX,
                minecraft.thePlayer.posY, minecraft.thePlayer.posZ, sightings);
        if (target == null) {
            if (now > dragonKillAlertUntil) {
                dragonKillAlertTarget = null;
            }
            return;
        }
        dragonKillAlertUntil = now + DRAGON_KILL_ALERT_HOLD_MS;
        if (target != dragonKillAlertTarget || now - dragonKillAlertTitleAt
                >= DRAGON_KILL_ALERT_RETITLE_MS) {
            showDragonKillTitle(target);
            dragonKillAlertTarget = target;
            dragonKillAlertTitleAt = now;
        }
    }

    /** The title is drawn in the colour of that dragon's area outline. */
    private void showDragonKillTitle(M7OverlayRenderer.M7DragonStatue statue) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.ingameGUI == null) {
            return;
        }
        String name = statue.dragonName.replace(" Dragon", "").toUpperCase(Locale.ROOT);
        minecraft.ingameGUI.displayTitle(dragonAlertColorCode(statue) + "\u00A7lKILL " + name
                + " DRAGON", "", 2, 18, 4);
    }

    static String dragonAlertColorCode(M7OverlayRenderer.M7DragonStatue statue) {
        switch (statue) {
            case POWER: return "\u00A7c";
            case APEX: return "\u00A7a";
            case SOUL: return "\u00A7d";
            case ICE: return "\u00A7b";
            case FLAME: return "\u00A76";
            default: return "\u00A7f";
        }
    }

    boolean isM7DragonKillAlertEnabled() {
        return m7DragonKillAlertEnabled;
    }

    void toggleM7DragonKillAlert() {
        m7DragonKillAlertEnabled = !m7DragonKillAlertEnabled;
        saveSettings();
    }

    private void showLordJawbusTitle() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.ingameGUI != null) {
            minecraft.ingameGUI.displayTitle("\u00A7c\u00A7lLORD JAWBUS", "", 1, 45, 1);
        }
    }

    static boolean isLordJawbusAlertMessage(String message) {
        if (message == null) {
            return false;
        }
        String plain = stripFormattingCodes(message).replace('\n', ' ').trim()
                .replaceAll("\\s+", " ");
        String upper = plain.toUpperCase(Locale.ROOT);
        return upper.startsWith("YOU HAVE ANGERED A LEGENDARY CREATURE... LORD JAWBUS HAS ARRIVED");
    }

    static boolean isFishingFestivalEndMessage(String message) {
        if (message == null) {
            return false;
        }
        String plain = stripFormattingCodes(message).replace('\n', ' ').replace('\r', ' ')
                .trim().replaceAll("\\s+", " ");
        return plain.equalsIgnoreCase(
                "FISHING FESTIVAL The festival has concluded! Time to dry off and repair your rods!");
    }

    static boolean shouldShowGenericFishingCreatureAlert(LegendaryCreature creature) {
        return creature != null && creature != LegendaryCreature.LORD_JAWBUS;
    }

    static boolean isSosFlareRemovedMessage(String message) {
        return isFlareDisappearedMessage(message) || isPreviousSosFlareRemovedMessage(message);
    }

    static boolean isFlareDisappearedMessage(String message) {
        if (message == null) {
            return false;
        }
        String upper = stripFormattingCodes(message).trim().toUpperCase(Locale.ROOT);
        return upper.contains("YOUR FLARE DISAPPEARED BECAUSE YOU WERE TOO FAR AWAY");
    }

    static boolean isPreviousSosFlareRemovedMessage(String message) {
        if (message == null) {
            return false;
        }
        String upper = stripFormattingCodes(message).trim().toUpperCase(Locale.ROOT);
        return upper.contains("YOUR PREVIOUS SOS FLARE WAS REMOVED");
    }

    static boolean isTotemOfCorruptionItemName(String itemName) {
        return DeployableItem.fromDisplayName(itemName) == DeployableItem.TOTEM_OF_CORRUPTION;
    }

    static boolean isSosFlareItemName(String itemName) {
        return DeployableItem.fromDisplayName(itemName) == DeployableItem.SOS_FLARE;
    }

    static boolean isPlasmafluxPowerOrbItemName(String itemName) {
        return DeployableItem.fromDisplayName(itemName) == DeployableItem.PLASMAFLUX_POWER_ORB;
    }

    static int parseDeployableTimerSeconds(String displayName) {
        if (displayName == null) {
            return -1;
        }
        Matcher matcher = DEPLOYABLE_REMAINING_TIME.matcher(stripFormattingCodes(displayName));
        if (!matcher.find()) {
            return -1;
        }
        try {
            long minutes = matcher.group(1) == null ? 0L : Long.parseLong(matcher.group(1));
            long seconds = Long.parseLong(matcher.group(2));
            long total = minutes * 60L + seconds;
            return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    static String formatDeployableTimer(int totalSeconds) {
        int seconds = Math.max(0, totalSeconds);
        if (seconds >= 60) {
            return (seconds / 60) + "m " + String.format(Locale.ROOT, "%02d", seconds % 60) + "s";
        }
        return String.format(Locale.ROOT, "%02d", seconds) + "s";
    }

    void startDeployableTimer(DeployableItem item, long now) {
        if (item == null || !isDeployableGroupEnabled(item.getGroup())) {
            return;
        }
        if (item == DeployableItem.TOTEM_OF_CORRUPTION) {
            startTotemFallbackTimer(now);
            return;
        }
        deployableExpiresAt.put(item, now + item.getDurationSeconds() * 1000L);
    }

    int getDeployableRemainingSeconds(DeployableItem item, long now) {
        if (item == null || !isDeployableGroupEnabled(item.getGroup())) {
            return 0;
        }
        if (item == DeployableItem.TOTEM_OF_CORRUPTION) {
            return getTotemRemainingSeconds(now);
        }
        Long expiresAt = deployableExpiresAt.get(item);
        if (expiresAt == null || expiresAt.longValue() <= now) {
            return 0;
        }
        long remainingMillis = expiresAt.longValue() - now;
        long remainingSeconds = (remainingMillis + 999L) / 1000L;
        return (int) Math.min(item.getDurationSeconds(), remainingSeconds);
    }

    List<DeployableItem> getActiveDeployableItems(long now) {
        List<DeployableItem> active = new ArrayList<DeployableItem>();
        for (DeployableItem item : DeployableItem.values()) {
            if (getDeployableRemainingSeconds(item, now) > 0) {
                active.add(item);
            }
        }
        return Collections.unmodifiableList(active);
    }

    int getTotemRemainingSeconds(long now) {
        if (!isDeployableGroupEnabled(DeployableGroup.TOTEM_OF_CORRUPTION)
                || totemRemainingSeconds <= 0 || now < totemTimerObservedAt) {
            return 0;
        }
        long elapsedSeconds = (now - totemTimerObservedAt) / 1000L;
        return (int) Math.max(0L, totemRemainingSeconds - elapsedSeconds);
    }

    int getSosFlareRemainingSeconds(long now) {
        return getDeployableRemainingSeconds(DeployableItem.SOS_FLARE, now);
    }

    int getPlasmafluxRemainingSeconds(long now) {
        return getDeployableRemainingSeconds(DeployableItem.PLASMAFLUX_POWER_ORB, now);
    }

    void startSosFlareTimer(long now) {
        startDeployableTimer(DeployableItem.SOS_FLARE, now);
    }

    void startPlasmafluxTimer(long now) {
        startDeployableTimer(DeployableItem.PLASMAFLUX_POWER_ORB, now);
    }

    boolean isDeployablesTimerPanelVisible() {
        return isDeployablesTimerPanelVisible(System.currentTimeMillis());
    }

    boolean isDeployablesTimerPanelVisible(long now) {
        return deployablesTimerPanelEnabled && !getActiveDeployableItems(now).isEmpty();
    }

    boolean isDeployablesTimerPanelEnabled() {
        return deployablesTimerPanelEnabled;
    }

    boolean isDeployableGroupEnabled(DeployableGroup group) {
        if (group == null) {
            return false;
        }
        Boolean enabled = deployableGroupEnabled.get(group);
        return enabled == null || enabled.booleanValue();
    }

    void toggleDeployableGroup(DeployableGroup group) {
        if (group == null) {
            return;
        }
        boolean enabled = !isDeployableGroupEnabled(group);
        deployableGroupEnabled.put(group, Boolean.valueOf(enabled));
        if (!enabled) {
            resetDeployableGroupTimers(group);
        }
        saveSettings();
    }

    void toggleTotemTimer() {
        toggleDeployableGroup(DeployableGroup.TOTEM_OF_CORRUPTION);
    }

    void toggleSosFlareTimer() {
        toggleDeployableGroup(DeployableGroup.SOS_FLARE);
    }

    void togglePlasmafluxTimer() {
        toggleDeployableGroup(DeployableGroup.POWER_ORBS);
    }

    boolean isM7OverlayEnabled() {
        return m7OverlayEnabled;
    }

    boolean isFloorSevenScoreboardDetected() {
        return m7OverlayRenderer.isFloorSevenScoreboardDetected();
    }

    boolean shouldRenderWorldOverlay() {
        Minecraft minecraft = Minecraft.getMinecraft();
        boolean chatOpen = minecraft != null && minecraft.currentScreen instanceof GuiChat;
        return m7OverlayEnabled && shouldRenderCustomHudPanels(
                isPlayerListKeyDown(), chatOpen, isLayoutEditorOpen());
    }

    void toggleM7Overlay() {
        m7OverlayEnabled = !m7OverlayEnabled;
        saveSettings();
    }

    boolean isM7BossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget target) {
        if (target == null) {
            return false;
        }
        switch (target) {
            case MAXOR:
                return m7MaxorEnabled;
            case STORM:
                return m7StormEnabled;
            case GOLDOR:
                return m7GoldorEnabled;
            case NECRON:
                return m7NecronEnabled;
            default:
                return false;
        }
    }

    void toggleM7BossHitbox(M7OverlayRenderer.M7HitboxTarget target) {
        if (target == null) {
            return;
        }
        switch (target) {
            case MAXOR:
                m7MaxorEnabled = !m7MaxorEnabled;
                break;
            case STORM:
                m7StormEnabled = !m7StormEnabled;
                break;
            case GOLDOR:
                m7GoldorEnabled = !m7GoldorEnabled;
                break;
            case NECRON:
                m7NecronEnabled = !m7NecronEnabled;
                break;
            default:
                return;
        }
        saveSettings();
    }

    boolean isM7DragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue statue) {
        if (statue == null) {
            return false;
        }
        switch (statue) {
            case POWER:
                return m7PowerDragonEnabled;
            case APEX:
                return m7ApexDragonEnabled;
            case SOUL:
                return m7SoulDragonEnabled;
            case ICE:
                return m7IceDragonEnabled;
            case FLAME:
                return m7FlameDragonEnabled;
            default:
                return false;
        }
    }

    void toggleM7DragonHitbox(M7OverlayRenderer.M7DragonStatue statue) {
        if (statue == null) {
            return;
        }
        switch (statue) {
            case POWER:
                m7PowerDragonEnabled = !m7PowerDragonEnabled;
                break;
            case APEX:
                m7ApexDragonEnabled = !m7ApexDragonEnabled;
                break;
            case SOUL:
                m7SoulDragonEnabled = !m7SoulDragonEnabled;
                break;
            case ICE:
                m7IceDragonEnabled = !m7IceDragonEnabled;
                break;
            case FLAME:
                m7FlameDragonEnabled = !m7FlameDragonEnabled;
                break;
            default:
                return;
        }
        saveSettings();
    }

    boolean isM7StatueZonesEnabled() {
        return m7StatueZonesEnabled;
    }

    void toggleM7StatueZones() {
        m7StatueZonesEnabled = !m7StatueZonesEnabled;
        saveSettings();
    }

    boolean isM7AreasPersistAfterDragonSpawn() {
        return m7AreasPersistAfterDragonSpawn;
    }

    void toggleM7AreasPersistAfterDragonSpawn() {
        m7AreasPersistAfterDragonSpawn = !m7AreasPersistAfterDragonSpawn;
        saveSettings();
    }

    boolean isJawbusAlertEnabled() {
        return jawbusAlertEnabled;
    }

    void toggleJawbusAlert() {
        jawbusAlertEnabled = !jawbusAlertEnabled;
        saveSettings();
    }

    boolean isFishingCreatureAlertEnabled() {
        return fishingCreatureAlertEnabled;
    }

    void toggleFishingCreatureAlert() {
        fishingCreatureAlertEnabled = !fishingCreatureAlertEnabled;
        saveSettings();
    }

    boolean isFishingEfficiencyTrackerEnabled() {
        return fishingEfficiencyTrackerEnabled;
    }

    void toggleFishingEfficiencyTracker() {
        fishingEfficiencyTrackerEnabled = !fishingEfficiencyTrackerEnabled;
        fishingSessionTracker.resetSession();
        if (fishingEfficiencyTrackerEnabled && fishingBobberWasActive) {
            fishingSessionTracker.startSession(System.currentTimeMillis());
        }
        saveSettings();
    }

    long getFishingEfficiencyCatchCount() {
        return fishingSessionTracker.getTrackedCatchCount();
    }

    long getFishingEfficiencyElapsedMillis(long now) {
        return fishingSessionTracker.getSessionElapsedMillis(now);
    }

    double getFishingEfficiencyCatchesPerHour(long now) {
        return fishingSessionTracker.getTrackedCatchesPerHour(now);
    }

    boolean isFishingFestivalTrackerEnabled() {
        return fishingFestivalTrackerEnabled;
    }

    void toggleFishingFestivalTracker() {
        fishingFestivalTrackerEnabled = !fishingFestivalTrackerEnabled;
        saveSettings();
    }

    long getFishingFestivalGreatWhiteCount(long now) {
        return fishingSessionTracker.getCurrentFestivalGreatWhiteCount(now);
    }

    long getFishingFestivalGreatWhiteBest() {
        return fishingSessionTracker.getBestFestivalGreatWhiteCount();
    }

    boolean isFishingRareCreatureHighlightEnabled() {
        return fishingRareCreatureHighlightEnabled;
    }

    boolean shouldRenderFishingRareCreatureHighlights() {
        Minecraft minecraft = Minecraft.getMinecraft();
        boolean chatOpen = minecraft != null && minecraft.currentScreen instanceof GuiChat;
        return fishingRareCreatureHighlightEnabled && shouldRenderCustomHudPanels(
                isPlayerListKeyDown(), chatOpen, isLayoutEditorOpen());
    }

    void toggleFishingRareCreatureHighlight() {
        fishingRareCreatureHighlightEnabled = !fishingRareCreatureHighlightEnabled;
        saveSettings();
    }

    void toggleDeployablesTimerPanel() {
        deployablesTimerPanelEnabled = !deployablesTimerPanelEnabled;
        saveSettings();
    }

    int getDeployablesTimerPanelX() {
        return deployablesTimerPanelX;
    }

    int getDeployablesTimerPanelY() {
        return deployablesTimerPanelY;
    }

    int getDeployablesTimerPanelSizePercent() {
        return deployablesTimerPanelSizePercent;
    }

    float getDeployablesTimerPanelScale() {
        return deployablesTimerPanelSizePercent / 100.0F;
    }

    int getRenderedDeployablesTimerPanelWidth(int screenWidth) {
        return Math.round(Math.min(DEPLOYABLE_PANEL_WIDTH, Math.max(80, screenWidth - 20))
                * getDeployablesTimerPanelScale());
    }

    int getRenderedDeployablesTimerPanelHeight() {
        int rowCount = Math.max(1, getActiveDeployableItems(System.currentTimeMillis()).size());
        return Math.round(getDeployablesTimerPanelHeight(rowCount) * getDeployablesTimerPanelScale());
    }

    static int getDeployablesTimerPanelHeight(int rowCount) {
        return HEADER_HEIGHT + 6 + Math.max(0, rowCount) * ITEM_ONLY_ENTRY_HEIGHT + 7;
    }

    void moveDeployablesTimerPanelTo(int x, int y, int screenWidth, int screenHeight) {
        int renderedWidth = getRenderedDeployablesTimerPanelWidth(screenWidth);
        int renderedHeight = getRenderedDeployablesTimerPanelHeight();
        deployablesTimerPanelX = clamp(x, 0, Math.max(0, screenWidth - renderedWidth));
        deployablesTimerPanelY = clamp(y, 0, Math.max(0, screenHeight - renderedHeight));
        deployablesTimerPanelPositionConfigured = true;
    }

    void clampDeployablesTimerPanelPosition(int screenWidth, int screenHeight) {
        boolean wasConfigured = deployablesTimerPanelPositionConfigured;
        int oldX = deployablesTimerPanelX;
        int oldY = deployablesTimerPanelY;
        if (!deployablesTimerPanelPositionConfigured) {
            deployablesTimerPanelX = 10;
            deployablesTimerPanelY = Math.max(0, screenHeight / 2 - 20);
            deployablesTimerPanelPositionConfigured = true;
        }
        moveDeployablesTimerPanelTo(deployablesTimerPanelX, deployablesTimerPanelY,
                screenWidth, screenHeight);
        if (!wasConfigured || oldX != deployablesTimerPanelX || oldY != deployablesTimerPanelY) {
            saveSettings();
        }
    }

    void resetDeployablesTimerPanelPosition(int screenWidth, int screenHeight) {
        deployablesTimerPanelPositionConfigured = false;
        clampDeployablesTimerPanelPosition(screenWidth, screenHeight);
        saveSettings();
    }

    void adjustDeployablesTimerPanelSize(int delta, int screenWidth, int screenHeight) {
        deployablesTimerPanelSizePercent = clamp(deployablesTimerPanelSizePercent + delta, MIN_HUD_SIZE_PERCENT, MAX_HUD_SIZE_PERCENT);
        clampDeployablesTimerPanelPosition(screenWidth, screenHeight);
        saveSettings();
    }

    void resetDeployablesTimerPanelSize(int screenWidth, int screenHeight) {
        deployablesTimerPanelSizePercent = 100;
        clampDeployablesTimerPanelPosition(screenWidth, screenHeight);
        saveSettings();
    }


    /** Turns only the local camera. The saved pitch and yaw are not changed. */
    void turnCamera(float pitch, float yaw) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.thePlayer == null) {
            return;
        }
        EntityPlayerSP player = minecraft.thePlayer;
        player.rotationPitch = pitch;
        player.prevRotationPitch = pitch;
        player.rotationYaw = yaw;
        player.prevRotationYaw = yaw;
        player.rotationYawHead = yaw;
        player.prevRotationYawHead = yaw;
    }

    boolean isNearbyCreatureHpEnabled() {
        return nearbyCreatureHpEnabled;
    }

    boolean isNearbyCreatureHpVisible() {
        return nearbyCreatureHpEnabled && !nearbyCreatureHpEntries.isEmpty();
    }

    List<NearbyCreatureHpEntry> getNearbyCreatureHpEntries() {
        return Collections.unmodifiableList(nearbyCreatureHpEntries);
    }

    void toggleNearbyCreatureHp() {
        nearbyCreatureHpEnabled = !nearbyCreatureHpEnabled;
        nextNearbyCreatureHpScanAt = 0L;
        if (!nearbyCreatureHpEnabled) {
            nearbyCreatureHpEntries.clear();
        }
        saveSettings();
    }

    int getNearbyCreatureHpX() {
        return nearbyCreatureHpX;
    }

    int getNearbyCreatureHpY() {
        return nearbyCreatureHpY;
    }

    int getNearbyCreatureHpSizePercent() {
        return nearbyCreatureHpSizePercent;
    }

    float getNearbyCreatureHpScale() {
        return nearbyCreatureHpSizePercent / 100.0F;
    }

    int getRenderedNearbyCreatureHpPanelWidth(int screenWidth) {
        return Math.round(getNearbyCreatureHpPanelWidth(screenWidth) * getNearbyCreatureHpScale());
    }

    int getRenderedNearbyCreatureHpPanelHeight() {
        return Math.round(getNearbyCreatureHpPanelHeight() * getNearbyCreatureHpScale());
    }

    int getNearbyCreatureHpPanelWidth(int screenWidth) {
        int availableWidth = (int) (Math.max(100, screenWidth - 8) / getNearbyCreatureHpScale());
        return Math.min(NEARBY_CREATURE_HP_PANEL_WIDTH, Math.max(100, availableWidth));
    }

    int getNearbyCreatureHpPanelHeight() {
        int rows = nearbyCreatureHpEntries.isEmpty() ? 3
                : Math.min(MAX_NEARBY_CREATURE_HP_ROWS, nearbyCreatureHpEntries.size());
        return HEADER_HEIGHT + 6 + rows * ITEM_ONLY_ENTRY_HEIGHT + 4;
    }

    void moveNearbyCreatureHpPanelTo(int x, int y, int screenWidth, int screenHeight) {
        int renderedWidth = getRenderedNearbyCreatureHpPanelWidth(screenWidth);
        int renderedHeight = getRenderedNearbyCreatureHpPanelHeight();
        nearbyCreatureHpX = clamp(x, 0, Math.max(0, screenWidth - renderedWidth));
        nearbyCreatureHpY = clamp(y, 0, Math.max(0, screenHeight - renderedHeight));
        nearbyCreatureHpPositionConfigured = true;
    }

    void clampNearbyCreatureHpPosition(int screenWidth, int screenHeight) {
        boolean wasConfigured = nearbyCreatureHpPositionConfigured;
        int oldX = nearbyCreatureHpX;
        int oldY = nearbyCreatureHpY;
        if (!nearbyCreatureHpPositionConfigured) {
            nearbyCreatureHpX = 10;
            nearbyCreatureHpY = Math.min(145, Math.max(0, screenHeight / 3));
            nearbyCreatureHpPositionConfigured = true;
        }
        moveNearbyCreatureHpPanelTo(nearbyCreatureHpX, nearbyCreatureHpY,
                screenWidth, screenHeight);
        if (!wasConfigured || oldX != nearbyCreatureHpX || oldY != nearbyCreatureHpY) {
            saveSettings();
        }
    }

    void resetNearbyCreatureHpPosition(int screenWidth, int screenHeight) {
        nearbyCreatureHpPositionConfigured = false;
        clampNearbyCreatureHpPosition(screenWidth, screenHeight);
        saveSettings();
    }

    void adjustNearbyCreatureHpSize(int delta, int screenWidth, int screenHeight) {
        nearbyCreatureHpSizePercent = clamp(nearbyCreatureHpSizePercent + delta, MIN_HUD_SIZE_PERCENT, MAX_HUD_SIZE_PERCENT);
        clampNearbyCreatureHpPosition(screenWidth, screenHeight);
        saveSettings();
    }

    void resetNearbyCreatureHpSize(int screenWidth, int screenHeight) {
        nearbyCreatureHpSizePercent = 100;
        clampNearbyCreatureHpPosition(screenWidth, screenHeight);
        saveSettings();
    }

    static boolean isFishingSplashSound(String soundName) {
        return soundName != null && soundName.toLowerCase(Locale.US).contains("splash");
    }

    static boolean isFishingSplashNearBobber(String soundName,
                                             double soundX, double soundY, double soundZ,
                                             double bobberX, double bobberY, double bobberZ) {
        if (!isFishingSplashSound(soundName)) {
            return false;
        }
        double dx = soundX - bobberX;
        double dy = soundY - bobberY;
        double dz = soundZ - bobberZ;
        double maxDistanceSq = FISHING_SPLASH_SOUND_RANGE_BLOCKS
                * FISHING_SPLASH_SOUND_RANGE_BLOCKS;
        return dx * dx + dy * dy + dz * dz <= maxDistanceSq;
    }

    static String formatSplashToPullDuration(long elapsedMillis) {
        return String.format(Locale.US, "%.2fs", Math.max(0L, elapsedMillis) / 1000.0D);
    }

    static boolean isBobberPulledDown(double verticalChange, long elapsedSinceCastMillis) {
        return elapsedSinceCastMillis >= BOBBER_PULL_ARM_DELAY_MS
                && verticalChange <= BOBBER_PULL_DOWN_THRESHOLD
                        + BOBBER_PULL_DISPLACEMENT_EPSILON;
    }

    void startSlayerBossTimer(SlayerType slayer, long now) {
        if (slayer == null) {
            return;
        }
        if (slayerBossTimerActive && slayerBossTimerType == slayer) {
            return;
        }
        slayerBossTimerType = slayer;
        slayerBossTimerStartedAt = now;
        slayerBossTimerActive = true;
        slayerBossTimerHasValue = true;
    }

    void completeSlayerBossTimer(long now) {
        if (!slayerBossTimerActive) {
            return;
        }
        lastSlayerBossDurationMillis = Math.max(0L, now - slayerBossTimerStartedAt);
        slayerBossTimerActive = false;
    }

    boolean hasSlayerBossTimer() {
        return slayerBossTimerHasValue;
    }

    boolean isSlayerBossTimerActive() {
        return slayerBossTimerActive;
    }

    long getSlayerBossTimerElapsedMillis(long now) {
        if (!slayerBossTimerHasValue) {
            return 0L;
        }
        return slayerBossTimerActive
                ? Math.max(0L, now - slayerBossTimerStartedAt) : lastSlayerBossDurationMillis;
    }

    private void resetFishingTimer() {
        fishingBobberWasActive = false;
        fishingTimerActive = false;
        fishingTimerHasValue = false;
        fishingTimerStartedAt = 0L;
        lastFishingDurationMillis = 0L;
        resetSplashToPullTimer();
    }

    private void resetSplashToPullTimer() {
        splashToPullTimerActive = false;
        splashToPullTimerHasValue = false;
        splashToPullTimerStartedAt = 0L;
        lastSplashToPullDurationMillis = 0L;
        splashToPullBobberEntityId = -1;
        previousSplashToPullBobberY = 0.0D;
        splashToPullSampleWindowStartY = 0.0D;
        splashToPullSampleWindowStartedAt = 0L;
        splashToPullBobberObservedAt = 0L;
    }

    private void resetSlayerBossTimer() {
        slayerBossTimerActive = false;
        slayerBossTimerHasValue = false;
        slayerBossTimerType = null;
        slayerBossTimerStartedAt = 0L;
        lastSlayerBossDurationMillis = 0L;
    }

    static String formatTimerDuration(long elapsedMillis) {
        long totalSeconds = Math.max(0L, elapsedMillis) / 1000L;
        long seconds = totalSeconds % 60L;
        long minutes = (totalSeconds / 60L) % 60L;
        long hours = totalSeconds / 3600L;
        return hours > 0L
                ? String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
                : String.format(Locale.US, "%02d:%02d", totalSeconds / 60L, seconds);
    }

    static String formatLegendaryCreatureLabel(LegendaryCreature creature, long count) {
        return creature.displayName + "-" + formatPrice(Math.max(0L, count));
    }

    static NearbyCreatureHpEntry parseNearbyCreatureHpLabel(String displayName) {
        return parseNearbyCreatureHpLabel(displayName, -1, Double.MAX_VALUE);
    }

    private static NearbyCreatureHpEntry parseNearbyCreatureHpLabel(String displayName,
                                                                     int entityId,
                                                                     double distanceSq) {
        if (displayName == null || displayName.length() == 0) {
            return null;
        }
        String plain = stripFormattingCodes(displayName).replace('\r', ' ')
                .replace('\n', ' ').replace('\u00A0', ' ')
                .replaceAll("(?i)SEA\\s+CREATURES\\s+HP", " ")
                .replaceAll("\\s+", " ").trim();
        if (plain.length() == 0) {
            return null;
        }

        String currentHealth = "";
        String maxHealth = "";
        String creaturePart = plain;
        Matcher health = NEARBY_CREATURE_HEALTH.matcher(plain);
        if (health.find()) {
            currentHealth = health.group(1).replaceAll("\\s+", "");
            maxHealth = health.group(2).replaceAll("\\s+", "");
            creaturePart = plain.substring(0, health.start()).trim();
        }

        int level = -1;
        Matcher levelMatcher = NEARBY_CREATURE_LEVEL.matcher(creaturePart);
        if (levelMatcher.find()) {
            try {
                level = Integer.parseInt(levelMatcher.group(1));
            } catch (NumberFormatException ignored) {
                level = -1;
            }
            creaturePart = creaturePart.substring(0, levelMatcher.start()) + " "
                    + creaturePart.substring(levelMatcher.end());
        }
        creaturePart = creaturePart.replaceAll("(?i)SEA\\s+CREATURES\\s+HP", " ")
                .replaceAll("[>\\u00BB\\u203A\\u27A4\\u2192]+", " ").trim();
        String creatureName = canonicalNearbySeaCreatureName(creaturePart);
        if (creatureName == null) {
            return null;
        }
        return new NearbyCreatureHpEntry(creatureName, level, currentHealth, maxHealth,
                entityId, distanceSq);
    }

    private static String canonicalNearbySeaCreatureName(String name) {
        if (name == null) {
            return null;
        }
        String normalized = normalizeNearbyCreatureName(name);
        for (String trackedName : TRACKED_NEARBY_SEA_CREATURES) {
            if (normalized.equals(normalizeNearbyCreatureName(trackedName))) {
                return trackedName;
            }
        }
        for (String trackedName : TRACKED_NEARBY_SEA_CREATURES) {
            String candidate = normalizeNearbyCreatureName(trackedName);
            if (normalized.startsWith(candidate + " ") || normalized.endsWith(" " + candidate)
                    || normalized.contains(" " + candidate + " ")) {
                return trackedName;
            }
        }
        return null;
    }

    private static String normalizeNearbyCreatureName(String name) {
        return stripFormattingCodes(name).replaceAll("[^A-Za-z0-9]+", " ")
                .trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    static String formatNearbyCreatureHpValue(double value) {
        double safeValue = Double.isNaN(value) || Double.isInfinite(value) ? 0.0D : Math.max(0.0D, value);
        if (safeValue >= 1000000000.0D) {
            return formatNearbyCreatureHpSuffix(safeValue, 1000000000.0D, "B");
        }
        if (safeValue >= 1000000.0D) {
            return formatNearbyCreatureHpSuffix(safeValue, 1000000.0D, "M");
        }
        if (safeValue >= 1000.0D) {
            return NumberFormat.getIntegerInstance(Locale.US).format(Math.round(safeValue));
        }
        return NumberFormat.getIntegerInstance(Locale.US).format(Math.round(safeValue));
    }

    private static String formatNearbyCreatureHpSuffix(double value, double divisor, String suffix) {
        String amount = BigDecimal.valueOf(value).divide(BigDecimal.valueOf(divisor),
                1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        return amount + suffix;
    }

    static long addDropQuantities(long existing, long added) {
        long safeExisting = Math.max(0L, existing);
        long safeAdded = Math.max(0L, added);
        return safeExisting > Long.MAX_VALUE - safeAdded
                ? Long.MAX_VALUE : safeExisting + safeAdded;
    }

    static long addPricedDropValue(long runningTotal, Long unitPrice, long quantity) {
        long safeTotal = Math.max(0L, runningTotal);
        if (unitPrice == null) {
            return safeTotal;
        }
        return addDropQuantities(safeTotal, calculateTotalPrice(unitPrice, quantity));
    }

    /** Renders the persistent tracker panel instead of a transient drop popup. */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || event.resolution == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        boolean chatOpen = minecraft != null && minecraft.currentScreen instanceof GuiChat;
        boolean layoutEditorOpen = minecraft != null
                && minecraft.currentScreen instanceof HudLayoutEditorScreen;
        int screenWidth = event.resolution.getScaledWidth();
        int screenHeight = event.resolution.getScaledHeight();
        if (!shouldRenderCustomHudPanels(isPlayerListKeyDown(), chatOpen, layoutEditorOpen)) {
            return;
        }
        if (trackerEnabled) {
            hudPanelRenderer.drawTrackerPanel(screenWidth, screenHeight);
        }
        if (isFishingSectionVisible()) {
            clampFishingTrackerPosition(screenWidth, screenHeight);
            hudPanelRenderer.drawFishingTrackerPanel(screenWidth, screenHeight);
        }
        if (isDeployablesTimerPanelVisible()) {
            clampDeployablesTimerPanelPosition(screenWidth, screenHeight);
            hudPanelRenderer.drawDeployablesTimerPanel(screenWidth, screenHeight);
        }
        if (isNearbyCreatureHpVisible()) {
            clampNearbyCreatureHpPosition(screenWidth, screenHeight);
            hudPanelRenderer.drawNearbyCreatureHpPanel(screenWidth, screenHeight);
        }
    }

    static boolean shouldRenderCustomHudPanels(boolean playerListKeyDown) {
        return !playerListKeyDown;
    }

    static boolean shouldRenderCustomHudPanels(boolean playerListKeyDown, boolean chatOpen,
                                               boolean layoutEditorOpen) {
        return !playerListKeyDown && !chatOpen && !layoutEditorOpen;
    }

    private boolean isPlayerListKeyDown() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.gameSettings == null
                || minecraft.gameSettings.keyBindPlayerList == null) {
            return false;
        }
        // Vanilla renders the tab list off the key binding's own state, so mirror that first and
        // fall back to the raw key state for clients that keep the raw state in sync differently.
        if (minecraft.gameSettings.keyBindPlayerList.isKeyDown()) {
            return true;
        }
        int keyCode = minecraft.gameSettings.keyBindPlayerList.getKeyCode();
        return keyCode > 0 && Keyboard.isKeyDown(keyCode);
    }

    private boolean isLayoutEditorOpen() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft != null && minecraft.currentScreen instanceof HudLayoutEditorScreen;
    }

    boolean isTrackerHeaderAt(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        float scale = getTrackerScale();
        int panelWidth = Math.round(getPanelWidth(screenWidth) * scale);
        int panelHeight = Math.round(getPanelHeight() * scale);
        int x = clamp(trackerX, 0, Math.max(0, screenWidth - panelWidth));
        int y = clamp(trackerY, 0, Math.max(0, screenHeight - panelHeight));
        int headerHeight = Math.round(HEADER_HEIGHT * scale);
        return mouseX >= x && mouseX <= x + panelWidth
                && mouseY >= y && mouseY <= y + headerHeight;
    }

    void moveTrackerTo(int x, int y, int screenWidth, int screenHeight) {
        float scale = getTrackerScale();
        int panelWidth = Math.round(getPanelWidth(screenWidth) * scale);
        int panelHeight = Math.round(getPanelHeight() * scale);
        trackerX = clamp(x, 0, Math.max(0, screenWidth - panelWidth));
        trackerY = clamp(y, 0, Math.max(0, screenHeight - panelHeight));
    }

    boolean isFishingHeaderAt(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        float scale = getTrackerScale();
        int panelWidth = Math.round(getPanelWidth(screenWidth) * scale);
        int panelHeight = Math.round(getFishingPanelHeight() * scale);
        int x = clamp(fishingX, 0, Math.max(0, screenWidth - panelWidth));
        int y = clamp(fishingY, 0, Math.max(0, screenHeight - panelHeight));
        int headerHeight = Math.round(HEADER_HEIGHT * scale);
        return mouseX >= x && mouseX <= x + panelWidth
                && mouseY >= y && mouseY <= y + headerHeight;
    }

    void moveFishingTrackerTo(int x, int y, int screenWidth, int screenHeight) {
        float scale = getTrackerScale();
        int panelWidth = Math.round(getPanelWidth(screenWidth) * scale);
        int panelHeight = Math.round(getFishingPanelHeight() * scale);
        fishingX = clamp(x, 0, Math.max(0, screenWidth - panelWidth));
        fishingY = clamp(y, 0, Math.max(0, screenHeight - panelHeight));
        fishingPositionConfigured = true;
    }

    void nudgeTracker(int dx, int dy, int screenWidth, int screenHeight) {
        moveTrackerTo(trackerX + dx, trackerY + dy, screenWidth, screenHeight);
        saveSettings();
    }

    void resetTrackerPosition() {
        trackerX = 10;
        trackerY = 60;
        saveSettings();
    }

    void resetFishingPosition(int screenWidth, int screenHeight) {
        fishingPositionConfigured = false;
        setDefaultFishingPosition(screenWidth, screenHeight);
        fishingPositionConfigured = true;
        moveFishingTrackerTo(fishingX, fishingY, screenWidth, screenHeight);
        saveSettings();
    }

    int getTrackerSizePercent() {
        return trackerSizePercent;
    }

    int getRenderedTrackerWidth(int screenWidth) {
        return Math.round(getPanelWidth(screenWidth) * getTrackerScale());
    }

    int getRenderedTrackerHeight() {
        return Math.round(getPanelHeight() * getTrackerScale());
    }

    int getFishingX() {
        return fishingX;
    }

    int getFishingY() {
        return fishingY;
    }

    int getRenderedFishingTrackerWidth(int screenWidth) {
        return Math.round(getPanelWidth(screenWidth) * getTrackerScale());
    }

    int getRenderedFishingTrackerHeight() {
        return Math.round(getFishingPanelHeight() * getTrackerScale());
    }

    void clampTrackerPosition(int screenWidth, int screenHeight) {
        int oldX = trackerX;
        int oldY = trackerY;
        moveTrackerTo(trackerX, trackerY, screenWidth, screenHeight);
        if (trackerX != oldX || trackerY != oldY) {
            saveSettings();
        }
    }

    void clampFishingTrackerPosition(int screenWidth, int screenHeight) {
        boolean wasConfigured = fishingPositionConfigured;
        int oldX = fishingX;
        int oldY = fishingY;
        if (!fishingPositionConfigured) {
            setDefaultFishingPosition(screenWidth, screenHeight);
            fishingPositionConfigured = true;
        }
        moveFishingTrackerTo(fishingX, fishingY, screenWidth, screenHeight);
        if (!wasConfigured || fishingX != oldX || fishingY != oldY) {
            saveSettings();
        }
    }

    void adjustTrackerSize(int delta, int screenWidth, int screenHeight) {
        trackerSizePercent = clamp(trackerSizePercent + delta, MIN_HUD_SIZE_PERCENT, MAX_HUD_SIZE_PERCENT);
        moveTrackerTo(trackerX, trackerY, screenWidth, screenHeight);
        clampFishingTrackerPosition(screenWidth, screenHeight);
        saveSettings();
    }

    void resetTrackerSize(int screenWidth, int screenHeight) {
        trackerSizePercent = 100;
        moveTrackerTo(trackerX, trackerY, screenWidth, screenHeight);
        clampFishingTrackerPosition(screenWidth, screenHeight);
        saveSettings();
    }

    float getTrackerScale() {
        return trackerSizePercent / 100.0F;
    }

    int getPanelWidth(int screenWidth) {
        int availableWidth = (int) (Math.max(100, screenWidth - 8) / getTrackerScale());
        return Math.min(PANEL_WIDTH, Math.max(100, availableWidth));
    }

    int getPanelHeight() {
        int height = HEADER_HEIGHT + 6;
        List<DropEntry> activeDrops = getDropHistory(activeSlayer);
        if (activeDrops.isEmpty()) {
            height += 18;
        } else {
            height += ITEM_ONLY_ENTRY_HEIGHT * activeDrops.size();
        }
        if (slayerBossTimerHasValue) {
            height += ITEM_ONLY_ENTRY_HEIGHT;
        }
        return height + 22;
    }

    int getFishingPanelHeight() {
        int caughtCount = fishingMobCounterEnabled ? getCaughtLegendaryCreatureCount() : 0;
        int displayedRows = 0;
        if (fishingMobCounterEnabled) {
            displayedRows += caughtCount + (fishingTimerHasValue ? 1 : 0)
                    + (splashToPullTimerHasValue ? 1 : 0);
            if (caughtCount == 0 && !fishingTimerHasValue) {
                displayedRows++;
            }
        }
        if (fishingEfficiencyTrackerEnabled) {
            displayedRows += fishingSessionTracker.getTrackedCatchCount() > 0L ? 2 : 1;
        }
        if (fishingFestivalTrackerEnabled) {
            long festivalCount = fishingSessionTracker.getCurrentFestivalGreatWhiteCount(
                    System.currentTimeMillis());
            long personalBest = fishingSessionTracker.getBestFestivalGreatWhiteCount();
            displayedRows += (festivalCount > 0L ? 1 : 0) + (personalBest > 0L ? 1 : 0);
            if (festivalCount == 0L && personalBest == 0L) {
                displayedRows++;
            }
        }
        if (displayedRows == 0) {
            displayedRows = 1;
        }
        boolean hasCreatureTotal = fishingMobCounterEnabled && getTotalLegendaryCreatureCount() > 0L;
        int footerHeight = hasCreatureTotal ? 22 : 6;
        return HEADER_HEIGHT + 6 + ITEM_ONLY_ENTRY_HEIGHT * displayedRows + footerHeight;
    }

    private void setDefaultFishingPosition(int screenWidth, int screenHeight) {
        int fishingWidth = getRenderedFishingTrackerWidth(screenWidth);
        int fishingHeight = getRenderedFishingTrackerHeight();
        int mainX = clamp(trackerX, 0,
                Math.max(0, screenWidth - getRenderedTrackerWidth(screenWidth)));
        int mainY = clamp(trackerY, 0,
                Math.max(0, screenHeight - getRenderedTrackerHeight()));
        int mainWidth = getRenderedTrackerWidth(screenWidth);
        int mainHeight = getRenderedTrackerHeight();
        if (mainX + mainWidth + 12 + fishingWidth <= screenWidth) {
            fishingX = mainX + mainWidth + 12;
            fishingY = mainY;
        } else {
            fishingX = clamp(mainX, 0, Math.max(0, screenWidth - fishingWidth));
            int belowMain = mainY + mainHeight + 12;
            fishingY = clamp(belowMain, 0, Math.max(0, screenHeight - fishingHeight));
        }
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    int getHudPanelBackgroundOpacity(int panel) {
        switch (panel) {
            case HUD_PANEL_SLAYER:
                return trackerBackgroundOpacityPercent;
            case HUD_PANEL_FISHING:
                return fishingBackgroundOpacityPercent;
            case HUD_PANEL_CREATURE_HP:
                return nearbyCreatureHpBackgroundOpacityPercent;
            case HUD_PANEL_DEPLOYABLES:
                return deployablesBackgroundOpacityPercent;
            default:
                throw new IllegalArgumentException("Unknown HUD panel: " + panel);
        }
    }

    void setHudPanelBackgroundOpacity(int panel, int opacityPercent) {
        int safeOpacity = clamp(opacityPercent, MIN_HUD_BACKGROUND_OPACITY_PERCENT,
                MAX_HUD_BACKGROUND_OPACITY_PERCENT);
        switch (panel) {
            case HUD_PANEL_SLAYER:
                trackerBackgroundOpacityPercent = safeOpacity;
                break;
            case HUD_PANEL_FISHING:
                fishingBackgroundOpacityPercent = safeOpacity;
                break;
            case HUD_PANEL_CREATURE_HP:
                nearbyCreatureHpBackgroundOpacityPercent = safeOpacity;
                break;
            case HUD_PANEL_DEPLOYABLES:
                deployablesBackgroundOpacityPercent = safeOpacity;
                break;
            default:
                throw new IllegalArgumentException("Unknown HUD panel: " + panel);
        }
    }

    boolean isTrackerEnabled() {
        return trackerEnabled;
    }

    SlayerType getActiveSlayer() {
        return activeSlayer;
    }

    void toggleTracker() {
        trackerEnabled = !trackerEnabled;
        if (!trackerEnabled) {
            completeSlayerBossTimer(System.currentTimeMillis());
        }
        saveSettings();
    }

    boolean isFishingMobCounterEnabled() {
        return fishingMobCounterEnabled;
    }

    boolean isFishingLegendaryNotifierEnabled() {
        return fishingLegendaryNotifierEnabled;
    }

    void toggleFishingLegendaryNotifier() {
        fishingLegendaryNotifierEnabled = !fishingLegendaryNotifierEnabled;
        saveSettings();
    }

    boolean isFishingTrophyFishNotifierEnabled() {
        return fishingTrophyFishNotifierEnabled;
    }

    void toggleFishingTrophyFishNotifier() {
        fishingTrophyFishNotifierEnabled = !fishingTrophyFishNotifierEnabled;
        saveSettings();
    }

    void toggleFishingMobCounter() {
        if (fishingMobCounterEnabled) {
            updateFishingTimerState(false, System.currentTimeMillis());
            resetSplashToPullTimer();
        }
        fishingMobCounterEnabled = !fishingMobCounterEnabled;
        saveSettings();
    }

    void clearHistory() {
        for (List<DropEntry> history : dropHistory.values()) {
            history.clear();
        }
        legendaryCreatureCounts.clear();
        lastProcessedDropKey = "";
        lastProcessedDropAt = 0L;
        lastProcessedFishingCreature = "";
        lastProcessedFishingAt = 0L;
        fishingSessionTracker.clear();
        resetFishingTimer();
        resetSlayerBossTimer();
        saveSettings();
    }

    void clearFishingHistory() {
        legendaryCreatureCounts.clear();
        lastProcessedFishingCreature = "";
        lastProcessedFishingAt = 0L;
        fishingSessionTracker.clear();
        resetFishingTimer();
        saveSettings();
    }

    void clearHistory(SlayerType slayer) {
        if (slayer == null) {
            return;
        }
        List<DropEntry> history = dropHistory.get(slayer);
        if (history != null) {
            history.clear();
        }
        if (lastProcessedDropKey.startsWith(slayer.name() + "|")) {
            lastProcessedDropKey = "";
            lastProcessedDropAt = 0L;
        }
        if (slayerBossTimerType == slayer) {
            resetSlayerBossTimer();
        }
    }

    void setItemPrice(String item, long price) {
        String cleanItem = DropMessageParser.extractDropItem(stripFormattingCodes(item).trim());
        String key = DropMessageParser.normalizeItemName(cleanItem);
        if (key.length() == 0 || price < 0L) {
            return;
        }
        manualPrices.put(key, price);
        saveSettings();
    }

    String getHeldItemName() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.thePlayer == null) {
            return "";
        }
        net.minecraft.item.ItemStack held = minecraft.thePlayer.getHeldItem();
        if (held == null) {
            return "";
        }
        return DropMessageParser.extractDropItem(stripFormattingCodes(held.getDisplayName()).trim());
    }

    Long getItemPrice(String item) {
        return manualPrices.get(DropMessageParser.normalizeItemName(item));
    }

    int getTrackerX() {
        return trackerX;
    }

    int getTrackerY() {
        return trackerY;
    }

    void saveSettings() {
        File file = getSettingsFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return;
        }

        Properties properties = new Properties();
        properties.setProperty("tracker.enabled", Boolean.toString(trackerEnabled));
        properties.setProperty("fishing.mobCounter.enabled", Boolean.toString(fishingMobCounterEnabled));
        properties.setProperty("tracker.x", Integer.toString(trackerX));
        properties.setProperty("tracker.y", Integer.toString(trackerY));
        if (fishingPositionConfigured) {
            properties.setProperty("fishing.x", Integer.toString(fishingX));
            properties.setProperty("fishing.y", Integer.toString(fishingY));
        }
        properties.setProperty("tracker.sizePercent", Integer.toString(trackerSizePercent));
        properties.setProperty("tracker.backgroundOpacityPercent",
                Integer.toString(trackerBackgroundOpacityPercent));
        properties.setProperty("fishing.backgroundOpacityPercent",
                Integer.toString(fishingBackgroundOpacityPercent));
        properties.setProperty("nearbyCreatureHp.backgroundOpacityPercent",
                Integer.toString(nearbyCreatureHpBackgroundOpacityPercent));
        properties.setProperty("deployables.timerPanel.backgroundOpacityPercent",
                Integer.toString(deployablesBackgroundOpacityPercent));
        properties.setProperty("fishing.legendaryNotifier.enabled",
                Boolean.toString(fishingLegendaryNotifierEnabled));
        properties.setProperty("fishing.trophyFishNotifier.enabled",
                Boolean.toString(fishingTrophyFishNotifierEnabled));
        properties.setProperty("fishing.jawbusAlert.enabled", Boolean.toString(jawbusAlertEnabled));
        properties.setProperty("dungeon.m7DragonKillAlert.enabled",
                Boolean.toString(m7DragonKillAlertEnabled));
        properties.setProperty("fishing.creatureAlert.enabled", Boolean.toString(fishingCreatureAlertEnabled));
        properties.setProperty("fishing.efficiency.enabled", Boolean.toString(fishingEfficiencyTrackerEnabled));
        properties.setProperty("fishing.festivalTracker.enabled", Boolean.toString(fishingFestivalTrackerEnabled));
        properties.setProperty("fishing.rareCreatureHighlight.enabled",
                Boolean.toString(fishingRareCreatureHighlightEnabled));
        if (fishingSessionTracker.getBestFestivalGreatWhiteCount() > 0L) {
            properties.setProperty(FISHING_FESTIVAL_BEST_PROPERTY,
                    Long.toString(fishingSessionTracker.getBestFestivalGreatWhiteCount()));
        }
        properties.setProperty("deployables.timerPanel.enabled",
                Boolean.toString(deployablesTimerPanelEnabled));
        for (DeployableGroup group : DeployableGroup.values()) {
            properties.setProperty(group.getSettingsKey(),
                    Boolean.toString(isDeployableGroupEnabled(group)));
        }
        properties.setProperty("dungeon.m7Overlay.enabled", Boolean.toString(m7OverlayEnabled));
        properties.setProperty("dungeon.boss.maxor.enabled", Boolean.toString(m7MaxorEnabled));
        properties.setProperty("dungeon.boss.storm.enabled", Boolean.toString(m7StormEnabled));
        properties.setProperty("dungeon.boss.goldor.enabled", Boolean.toString(m7GoldorEnabled));
        properties.setProperty("dungeon.boss.necron.enabled", Boolean.toString(m7NecronEnabled));
        properties.setProperty("dungeon.dragon.power.enabled", Boolean.toString(m7PowerDragonEnabled));
        properties.setProperty("dungeon.dragon.apex.enabled", Boolean.toString(m7ApexDragonEnabled));
        properties.setProperty("dungeon.dragon.soul.enabled", Boolean.toString(m7SoulDragonEnabled));
        properties.setProperty("dungeon.dragon.ice.enabled", Boolean.toString(m7IceDragonEnabled));
        properties.setProperty("dungeon.dragon.flame.enabled", Boolean.toString(m7FlameDragonEnabled));
        properties.setProperty("dungeon.statueZones.enabled", Boolean.toString(m7StatueZonesEnabled));
        properties.setProperty("dungeon.statueZones.keepAfterDragonSpawn.enabled",
                Boolean.toString(m7AreasPersistAfterDragonSpawn));
        properties.setProperty(NEARBY_CREATURE_HP_PROPERTY_PREFIX + "enabled",
                Boolean.toString(nearbyCreatureHpEnabled));
        properties.setProperty(NEARBY_CREATURE_HP_PROPERTY_PREFIX + "sizePercent",
                Integer.toString(nearbyCreatureHpSizePercent));
        if (nearbyCreatureHpPositionConfigured) {
            properties.setProperty(NEARBY_CREATURE_HP_PROPERTY_PREFIX + "x",
                    Integer.toString(nearbyCreatureHpX));
            properties.setProperty(NEARBY_CREATURE_HP_PROPERTY_PREFIX + "y",
                    Integer.toString(nearbyCreatureHpY));
        }
        properties.setProperty("deployables.timerPanel.sizePercent",
                Integer.toString(deployablesTimerPanelSizePercent));
        if (deployablesTimerPanelPositionConfigured) {
            properties.setProperty("deployables.timerPanel.x", Integer.toString(deployablesTimerPanelX));
            properties.setProperty("deployables.timerPanel.y", Integer.toString(deployablesTimerPanelY));
        }
        for (Map.Entry<String, Long> entry : manualPrices.entrySet()) {
            properties.setProperty(PRICE_PROPERTY_PREFIX + entry.getKey(),
                    Long.toString(entry.getValue()));
        }
        for (Map.Entry<LegendaryCreature, Long> entry : legendaryCreatureCounts.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0L) {
                properties.setProperty(LEGENDARY_COUNT_PROPERTY_PREFIX + entry.getKey().name(),
                        Long.toString(entry.getValue()));
            }
        }

        try (FileOutputStream output = new FileOutputStream(file)) {
            properties.store(output, "RNG Carried Bald settings");
        } catch (IOException ignored) {
            // A settings write failure should not interrupt the client.
        }
    }

    private void loadSettings() {
        File file = getSettingsFile();
        if (!file.isFile()) {
            return;
        }

        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(file)) {
            properties.load(input);
        } catch (IOException ignored) {
            return;
        }

        trackerEnabled = Boolean.parseBoolean(properties.getProperty("tracker.enabled", "true"));
        fishingMobCounterEnabled = Boolean.parseBoolean(
                properties.getProperty("fishing.mobCounter.enabled", "false"));
        trackerX = parseInt(properties.getProperty("tracker.x"), trackerX);
        trackerY = parseInt(properties.getProperty("tracker.y"), trackerY);
        String fishingXValue = properties.getProperty("fishing.x");
        String fishingYValue = properties.getProperty("fishing.y");
        if (fishingXValue != null && fishingYValue != null) {
            fishingX = parseInt(fishingXValue, fishingX);
            fishingY = parseInt(fishingYValue, fishingY);
            fishingPositionConfigured = true;
        }
        trackerSizePercent = clamp(parseInt(properties.getProperty("tracker.sizePercent"),
                trackerSizePercent), MIN_HUD_SIZE_PERCENT, MAX_HUD_SIZE_PERCENT);
        trackerBackgroundOpacityPercent = clamp(parseInt(properties.getProperty(
                "tracker.backgroundOpacityPercent", Integer.toString(trackerBackgroundOpacityPercent)),
                trackerBackgroundOpacityPercent), MIN_HUD_BACKGROUND_OPACITY_PERCENT,
                MAX_HUD_BACKGROUND_OPACITY_PERCENT);
        fishingBackgroundOpacityPercent = clamp(parseInt(properties.getProperty(
                "fishing.backgroundOpacityPercent", Integer.toString(fishingBackgroundOpacityPercent)),
                fishingBackgroundOpacityPercent), MIN_HUD_BACKGROUND_OPACITY_PERCENT,
                MAX_HUD_BACKGROUND_OPACITY_PERCENT);
        nearbyCreatureHpBackgroundOpacityPercent = clamp(parseInt(properties.getProperty(
                "nearbyCreatureHp.backgroundOpacityPercent",
                Integer.toString(nearbyCreatureHpBackgroundOpacityPercent)),
                nearbyCreatureHpBackgroundOpacityPercent), MIN_HUD_BACKGROUND_OPACITY_PERCENT,
                MAX_HUD_BACKGROUND_OPACITY_PERCENT);
        deployablesBackgroundOpacityPercent = clamp(parseInt(properties.getProperty(
                "deployables.timerPanel.backgroundOpacityPercent",
                Integer.toString(deployablesBackgroundOpacityPercent)),
                deployablesBackgroundOpacityPercent), MIN_HUD_BACKGROUND_OPACITY_PERCENT,
                MAX_HUD_BACKGROUND_OPACITY_PERCENT);
        fishingLegendaryNotifierEnabled = Boolean.parseBoolean(properties.getProperty(
                "fishing.legendaryNotifier.enabled", "false"));
        fishingTrophyFishNotifierEnabled = Boolean.parseBoolean(properties.getProperty(
                "fishing.trophyFishNotifier.enabled", "false"));
        jawbusAlertEnabled = Boolean.parseBoolean(
                properties.getProperty("fishing.jawbusAlert.enabled", "true"));
        m7DragonKillAlertEnabled = Boolean.parseBoolean(
                properties.getProperty("dungeon.m7DragonKillAlert.enabled", "true"));
        fishingCreatureAlertEnabled = Boolean.parseBoolean(
                properties.getProperty("fishing.creatureAlert.enabled", "false"));
        fishingEfficiencyTrackerEnabled = Boolean.parseBoolean(
                properties.getProperty("fishing.efficiency.enabled", "false"));
        fishingFestivalTrackerEnabled = Boolean.parseBoolean(
                properties.getProperty("fishing.festivalTracker.enabled", "false"));
        fishingRareCreatureHighlightEnabled = Boolean.parseBoolean(
                properties.getProperty("fishing.rareCreatureHighlight.enabled", "false"));
        try {
            fishingSessionTracker.loadBestFestivalGreatWhiteCount(Long.parseLong(
                    properties.getProperty(FISHING_FESTIVAL_BEST_PROPERTY, "0")));
        } catch (NumberFormatException ignored) {
            fishingSessionTracker.loadBestFestivalGreatWhiteCount(0L);
        }
        deployablesTimerPanelEnabled = Boolean.parseBoolean(
                properties.getProperty("deployables.timerPanel.enabled", "true"));
        for (DeployableGroup group : DeployableGroup.values()) {
            String enabledValue = properties.getProperty(group.getSettingsKey());
            if (enabledValue == null && group.getLegacySettingsKey() != null) {
                enabledValue = properties.getProperty(group.getLegacySettingsKey());
            }
            deployableGroupEnabled.put(group, Boolean.valueOf(
                    enabledValue == null || Boolean.parseBoolean(enabledValue)));
        }
        m7OverlayEnabled = Boolean.parseBoolean(
                properties.getProperty("dungeon.m7Overlay.enabled", "true"));
        m7MaxorEnabled = Boolean.parseBoolean(properties.getProperty("dungeon.boss.maxor.enabled", "true"));
        m7StormEnabled = Boolean.parseBoolean(properties.getProperty("dungeon.boss.storm.enabled", "true"));
        m7GoldorEnabled = Boolean.parseBoolean(properties.getProperty("dungeon.boss.goldor.enabled", "true"));
        m7NecronEnabled = Boolean.parseBoolean(properties.getProperty("dungeon.boss.necron.enabled", "true"));
        m7PowerDragonEnabled = Boolean.parseBoolean(
                properties.getProperty("dungeon.dragon.power.enabled", "true"));
        m7ApexDragonEnabled = Boolean.parseBoolean(
                properties.getProperty("dungeon.dragon.apex.enabled", "true"));
        m7SoulDragonEnabled = Boolean.parseBoolean(
                properties.getProperty("dungeon.dragon.soul.enabled", "true"));
        m7IceDragonEnabled = Boolean.parseBoolean(
                properties.getProperty("dungeon.dragon.ice.enabled", "true"));
        m7FlameDragonEnabled = Boolean.parseBoolean(
                properties.getProperty("dungeon.dragon.flame.enabled", "true"));
        m7StatueZonesEnabled = Boolean.parseBoolean(
                properties.getProperty("dungeon.statueZones.enabled", "true"));
        m7AreasPersistAfterDragonSpawn = Boolean.parseBoolean(
                properties.getProperty("dungeon.statueZones.keepAfterDragonSpawn.enabled", "true"));
        nearbyCreatureHpEnabled = Boolean.parseBoolean(properties.getProperty(
                NEARBY_CREATURE_HP_PROPERTY_PREFIX + "enabled", "true"));
        nearbyCreatureHpSizePercent = clamp(parseInt(properties.getProperty(
                NEARBY_CREATURE_HP_PROPERTY_PREFIX + "sizePercent"), nearbyCreatureHpSizePercent),
                MIN_HUD_SIZE_PERCENT, MAX_HUD_SIZE_PERCENT);
        String nearbyCreatureHpXValue = properties.getProperty(NEARBY_CREATURE_HP_PROPERTY_PREFIX + "x");
        String nearbyCreatureHpYValue = properties.getProperty(NEARBY_CREATURE_HP_PROPERTY_PREFIX + "y");
        if (nearbyCreatureHpXValue != null && nearbyCreatureHpYValue != null) {
            nearbyCreatureHpX = parseInt(nearbyCreatureHpXValue, nearbyCreatureHpX);
            nearbyCreatureHpY = parseInt(nearbyCreatureHpYValue, nearbyCreatureHpY);
            nearbyCreatureHpPositionConfigured = true;
        }
        deployablesTimerPanelSizePercent = clamp(parseInt(properties.getProperty(
                "deployables.timerPanel.sizePercent"), deployablesTimerPanelSizePercent),
                MIN_HUD_SIZE_PERCENT, MAX_HUD_SIZE_PERCENT);
        String deployablesXValue = properties.getProperty("deployables.timerPanel.x");
        String deployablesYValue = properties.getProperty("deployables.timerPanel.y");
        if (deployablesXValue != null && deployablesYValue != null) {
            deployablesTimerPanelX = parseInt(deployablesXValue, deployablesTimerPanelX);
            deployablesTimerPanelY = parseInt(deployablesYValue, deployablesTimerPanelY);
            deployablesTimerPanelPositionConfigured = true;
        }

        for (String propertyName : properties.stringPropertyNames()) {
            if (propertyName.startsWith(PRICE_PROPERTY_PREFIX)
                    && !LEGACY_LAST_PRICE_ITEM_PROPERTY.equals(propertyName)) {
                String item = DropMessageParser.normalizeItemName(propertyName.substring(PRICE_PROPERTY_PREFIX.length()));
                try {
                    long price = Long.parseLong(properties.getProperty(propertyName));
                    if (item.length() > 0 && price >= 0L) {
                        manualPrices.put(item, price);
                    }
                } catch (NumberFormatException ignored) {
                    // Skip malformed price entries and keep loading other settings.
                }
            } else if (propertyName.startsWith(LEGENDARY_COUNT_PROPERTY_PREFIX)) {
                String creatureName = propertyName.substring(LEGENDARY_COUNT_PROPERTY_PREFIX.length());
                try {
                    LegendaryCreature creature = LegendaryCreature.valueOf(creatureName);
                    long count = Long.parseLong(properties.getProperty(propertyName));
                    if (count > 0L) {
                        legendaryCreatureCounts.put(creature, count);
                    }
                } catch (IllegalArgumentException ignored) {
                    // Skip unknown creature names or malformed counts.
                }
            }
        }
    }

    private File getSettingsFile() {
        if (settingsFile == null) {
            Minecraft minecraft = Minecraft.getMinecraft();
            File gameDir = minecraft == null || minecraft.mcDataDir == null
                    ? new File(".") : minecraft.mcDataDir;
            settingsFile = new File(new File(gameDir, "config"), "spiderdropalert.properties");
        }
        return settingsFile;
    }

    private static float parseFloat(String value, float fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    static String stripFormattingCodes(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder clean = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == '\u00A7' && i + 1 < text.length()) {
                i++;
            } else {
                clean.append(current);
            }
        }
        return clean.toString();
    }

    static String formatPrice(long price) {
        return NumberFormat.getIntegerInstance(Locale.US).format(price);
    }

    static long calculateTotalPrice(long unitPrice, long quantity) {
        if (unitPrice <= 0L || quantity <= 0L) {
            return 0L;
        }
        return unitPrice > Long.MAX_VALUE / quantity ? Long.MAX_VALUE : unitPrice * quantity;
    }

    static String formatDropLabel(String item, long quantity, Long unitPrice) {
        long safeQuantity = Math.max(1L, quantity);
        String quantityPrefix = NumberFormat.getIntegerInstance(Locale.US).format(safeQuantity) + "x ";
        return quantityPrefix + item + "(" + getDropValue(safeQuantity, unitPrice) + ")";
    }

    static String getDropValue(long quantity, Long unitPrice) {
        return unitPrice == null ? "price not set"
                : formatCompactPrice(calculateTotalPrice(unitPrice, quantity));
    }

    long getVisibleTotalProfit() {
        long total = 0L;
        for (DropEntry entry : getDropHistory(activeSlayer)) {
            total = addPricedDropValue(total, getItemPrice(entry.item), entry.quantity);
        }
        return total;
    }

    static String formatCompactPrice(long price) {
        if (price < 1000L) {
            return formatPrice(price) + (price == 1L ? " coin" : " coins");
        }
        long divisor;
        String suffix;
        if (price >= 1000000000L) {
            divisor = 1000000000L;
            suffix = "b";
        } else if (price >= 1000000L) {
            divisor = 1000000L;
            suffix = "m";
        } else {
            divisor = 1000L;
            suffix = "k";
        }
        String amount = BigDecimal.valueOf(price)
                .divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
        return amount + suffix;
    }

    static final class NearbyCreatureHpEntry {
        final String creatureName;
        final int level;
        final String currentHealth;
        final String maxHealth;
        final int entityId;
        final double distanceSq;

        NearbyCreatureHpEntry(String creatureName, int level, String currentHealth,
                              String maxHealth, int entityId, double distanceSq) {
            this.creatureName = creatureName;
            this.level = level;
            this.currentHealth = currentHealth == null ? "" : currentHealth;
            this.maxHealth = maxHealth == null ? "" : maxHealth;
            this.entityId = entityId;
            this.distanceSq = distanceSq;
        }

        String toHudLabel() {
            String levelText = level >= 0 ? "\u00A77[Lvl " + level + "] " : "";
            String healthText = currentHealth.length() > 0 && maxHealth.length() > 0
                    ? " \u00A7a" + currentHealth + "/" + maxHealth + "\u00A7c\u2764" : "";
            return levelText + "\u00A7c" + creatureName + healthText;
        }
    }

    static final class DropEntry {
        final String item;
        final long quantity;

        DropEntry(String item, long quantity) {
            this.item = item;
            this.quantity = Math.max(1L, quantity);
        }
    }
}

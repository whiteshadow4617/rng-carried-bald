package dev.arena.spiderdropalert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/** Owns Floor 7/M7 entity tracking, floor detection, and world-space outlines. */
public final class M7OverlayRenderer {
    private static final long ENTITY_CACHE_REFRESH_MS = 250L;
    private static final long SCOREBOARD_CACHE_REFRESH_MS = 500L;
    private static final Pattern FAKEPIXEL_DUNGEON_RUN_ID_PATTERN = Pattern.compile("DUNG([0-9]+)");

    private final SpiderDropAlert mod;
    private final Map<Integer, M7DragonStatue> dragonTypesByEntityId =
            new HashMap<Integer, M7DragonStatue>();
    private final List<M7Outline> cachedM7Outlines = new ArrayList<M7Outline>();
    private final List<Entity> cachedRareCreatureEntities = new ArrayList<Entity>();
    private final int[] drawnM7EntityIds = new int[64];
    private int drawnM7EntityIdCount;
    private World cachedWorld;
    private long lastEntityCacheRefreshAt;
    private long lastScoreboardRefreshAt;
    private boolean m7TargetCacheReady;
    private boolean rareCreatureCacheReady;
    private boolean floorSevenScoreboardDetected;
    private boolean m7ScoreboardDetected;
    private boolean m7FinalPhaseSeen;
    private boolean m7DragonSpawnedAfterFinalPhase;
    private String fakepixelM7DungeonRunId;

    private static final class M7Candidate {
        final Entity entity;
        final String name;
        final M7HitboxTarget target;

        M7Candidate(Entity entity, String name, M7HitboxTarget target) {
            this.entity = entity;
            this.name = name;
            this.target = target;
        }
    }

    private static final class M7Outline {
        final Entity source;
        final Entity body;
        final M7HitboxTarget bossTarget;
        final M7DragonStatue dragonStatue;

        M7Outline(Entity source, Entity body, M7HitboxTarget bossTarget,
                  M7DragonStatue dragonStatue) {
            this.source = source;
            this.body = body;
            this.bossTarget = bossTarget;
            this.dragonStatue = dragonStatue;
        }
    }

    boolean isFloorSevenScoreboardDetected() {
        return floorSevenScoreboardDetected;
    }

    M7OverlayRenderer(SpiderDropAlert mod) {
        this.mod = mod;
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event == null || event.world == null || !event.world.isRemote
                || !(event.entity instanceof EntityDragon)) {
            return;
        }

        if (m7FinalPhaseSeen) {
            m7DragonSpawnedAfterFinalPhase = true;
        }

        Entity dragon = event.entity;
        M7DragonStatue statue = dragonStatueForName(getEntityName(dragon));
        if (statue == null) {
            statue = nearestM7DragonStatue(dragon.posX, dragon.posZ);
        }
        if (statue != null) {
            dragonTypesByEntityId.put(dragon.getEntityId(), statue);
        }
    }

    /** Uses the NEF 1.2.4 final-phase chat cue to start showing M7 relic areas. */
    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (event == null || event.message == null
                || !isM7FinalPhaseMessage(event.message.getUnformattedText())) {
            return;
        }
        if (!m7FinalPhaseSeen) {
            m7DragonSpawnedAfterFinalPhase = hasDragonLoadedInCurrentWorld();
        }
        m7FinalPhaseSeen = true;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (event == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.theWorld == null || minecraft.thePlayer == null) {
            resetWorldState();
            return;
        }

        World world = minecraft.theWorld;
        long now = System.currentTimeMillis();
        if (cachedWorld != world) {
            resetWorldState();
            cachedWorld = world;
        }
        if (now - lastScoreboardRefreshAt >= SCOREBOARD_CACHE_REFRESH_MS) {
            // Keep detection independent from the overlay toggle so the Dungeon page can explain
            // why outlines are not currently being drawn, without scanning the sidebar every frame.
            boolean wasM7ScoreboardDetected = m7ScoreboardDetected;
            String scoreboardText = getSidebarScoreboardText(minecraft);
            String fakepixelRunId = fakepixelDungeonRunId(scoreboardText);
            boolean fakepixelDragonSidebar = isFakepixelM7DragonScoreboardText(scoreboardText);
            if (fakepixelDragonSidebar) {
                if (fakepixelM7DungeonRunId != null
                        && !fakepixelM7DungeonRunId.equals(fakepixelRunId)) {
                    resetM7DragonPhaseState();
                }
                fakepixelM7DungeonRunId = fakepixelRunId;
                if (!m7FinalPhaseSeen) {
                    m7DragonSpawnedAfterFinalPhase = true;
                }
                // Fakepixel uses dragon health lines instead of NEF's Hypixel phase chat cue.
                m7FinalPhaseSeen = true;
            } else if (fakepixelM7DungeonRunId != null
                    && !fakepixelM7DungeonRunId.equals(fakepixelRunId)) {
                fakepixelM7DungeonRunId = null;
                resetM7DragonPhaseState();
            }
            boolean fakepixelM7RunActive = fakepixelM7DungeonRunId != null
                    && fakepixelM7DungeonRunId.equals(fakepixelRunId);
            floorSevenScoreboardDetected = isFloorSevenScoreboardText(scoreboardText)
                    || fakepixelM7RunActive;
            m7ScoreboardDetected = isM7ScoreboardText(scoreboardText) || fakepixelM7RunActive;
            if (wasM7ScoreboardDetected && !m7ScoreboardDetected) {
                resetM7DragonPhaseState();
            }
            lastScoreboardRefreshAt = now;
        }

        boolean renderM7InThisWorld = mod.shouldRenderWorldOverlay() && floorSevenScoreboardDetected;
        boolean renderCreatureHighlights = mod.shouldRenderFishingRareCreatureHighlights();
        if (!renderM7InThisWorld) {
            clearM7TargetCache();
        }
        if (!renderCreatureHighlights) {
            clearRareCreatureCache();
        }
        if (!renderM7InThisWorld && !renderCreatureHighlights) {
            return;
        }

        if (now - lastEntityCacheRefreshAt >= ENTITY_CACHE_REFRESH_MS
                || (renderM7InThisWorld && !m7TargetCacheReady)
                || (renderCreatureHighlights && !rareCreatureCacheReady)) {
            refreshEntityCaches(minecraft, now, renderM7InThisWorld, renderCreatureHighlights);
        }

        Entity camera = minecraft.getRenderViewEntity();
        if (camera == null) {
            return;
        }

        double cameraX = interpolate(camera.lastTickPosX, camera.posX, event.partialTicks);
        double cameraY = interpolate(camera.lastTickPosY, camera.posY, event.partialTicks);
        double cameraZ = interpolate(camera.lastTickPosZ, camera.posZ, event.partialTicks);

        if (renderCreatureHighlights) {
            for (Entity entity : cachedRareCreatureEntities) {
                renderRareSeaCreatureOutline(entity, minecraft, event.partialTicks,
                        cameraX, cameraY, cameraZ);
            }
        }
        if (renderM7InThisWorld) {
            drawCachedM7Outlines(event.partialTicks, cameraX, cameraY, cameraZ);
        }
        boolean showM7DragonKillAreas = shouldRenderM7DragonKillAreas(m7ScoreboardDetected,
                m7FinalPhaseSeen, m7DragonSpawnedAfterFinalPhase,
                mod.isM7StatueZonesEnabled(), mod.isM7AreasPersistAfterDragonSpawn());
        if (renderM7InThisWorld && showM7DragonKillAreas) {
            for (M7DragonKillArea area : M7DragonKillArea.values()) {
                drawOutlinedBox(area.bounds.offset(-cameraX, -cameraY, -cameraZ),
                        area.color, false);
            }
        }
    }

    private void refreshEntityCaches(Minecraft minecraft, long now, boolean includeM7,
                                     boolean includeRareCreatures) {
        if (includeM7) {
            cachedM7Outlines.clear();
        }
        if (includeRareCreatures) {
            cachedRareCreatureEntities.clear();
        }

        List<EntityLivingBase> livingCandidates = new ArrayList<EntityLivingBase>();
        List<EntityDragon> dragonCandidates = new ArrayList<EntityDragon>();
        List<M7Candidate> m7Candidates = new ArrayList<M7Candidate>();
        for (Entity entity : minecraft.theWorld.loadedEntityList) {
            if (entity == null || entity.isDead) {
                continue;
            }
            boolean armorStand = entity instanceof EntityArmorStand;
            if (includeRareCreatures && entity instanceof EntityLivingBase
                    && !armorStand && entity != minecraft.thePlayer
                    && entity.getDistanceSqToEntity(minecraft.thePlayer) <= 64.0D * 64.0D) {
                if (SpiderDropAlert.parseNearbyCreatureHpLabel(getEntityName(entity)) != null) {
                    cachedRareCreatureEntities.add(entity);
                }
            }
            if (!includeM7) {
                continue;
            }

            if (entity instanceof EntityDragon) {
                dragonCandidates.add((EntityDragon) entity);
                if (m7FinalPhaseSeen) {
                    m7DragonSpawnedAfterFinalPhase = true;
                }
            } else if (entity instanceof EntityLivingBase && !armorStand
                    && !(entity instanceof EntityPlayer)) {
                livingCandidates.add((EntityLivingBase) entity);
            }

            String customName = entity.getCustomNameTag();
            boolean inspectName = armorStand || entity instanceof EntityDragon
                    || entity instanceof EntityWither
                    || (customName != null && customName.length() > 0);
            if (!inspectName) {
                continue;
            }
            String name = getEntityName(entity);
            M7HitboxTarget target = classifyM7HitboxTarget(name, entity instanceof EntityDragon);
            if (target != null) {
                m7Candidates.add(new M7Candidate(entity, name, target));
            }
        }

        if (includeM7) {
            for (M7Candidate candidate : m7Candidates) {
                Entity entity = candidate.entity;
                if (entity instanceof EntityArmorStand) {
                    if (candidate.target == M7HitboxTarget.DRAGON) {
                        M7DragonStatue statue = dragonStatueForName(candidate.name);
                        if (statue == null) {
                            statue = nearestM7DragonStatue(entity.posX, entity.posZ);
                        }
                        if (statue != null && mod.isM7DragonHitboxEnabled(statue)) {
                            EntityDragon dragon = findNearbyDragon(entity, dragonCandidates);
                            cachedM7Outlines.add(new M7Outline(entity, dragon, null, statue));
                        }
                    } else if (mod.isM7BossHitboxEnabled(candidate.target)) {
                        Entity body = findNearbyBossBody(entity, livingCandidates);
                        cachedM7Outlines.add(new M7Outline(entity, body, candidate.target, null));
                    }
                    continue;
                }

                if (candidate.target == M7HitboxTarget.DRAGON) {
                    M7DragonStatue statue = dragonStatueForName(candidate.name);
                    if (statue == null) {
                        statue = dragonTypesByEntityId.get(entity.getEntityId());
                    }
                    if (statue == null) {
                        statue = nearestM7DragonStatue(entity.posX, entity.posZ);
                    }
                    if (statue != null && mod.isM7DragonHitboxEnabled(statue)) {
                        cachedM7Outlines.add(new M7Outline(entity, entity, null, statue));
                    }
                } else if (mod.isM7BossHitboxEnabled(candidate.target)) {
                    cachedM7Outlines.add(new M7Outline(entity, entity, candidate.target, null));
                }
            }
            m7TargetCacheReady = true;
        } else {
            clearM7TargetCache();
        }
        rareCreatureCacheReady = includeRareCreatures;
        lastEntityCacheRefreshAt = now;
    }

    private void drawCachedM7Outlines(float partialTicks, double cameraX, double cameraY,
                                      double cameraZ) {
        drawnM7EntityIdCount = 0;
        for (M7Outline outline : cachedM7Outlines) {
            if (outline.source == null || outline.source.isDead) {
                continue;
            }
            Entity body = outline.body;
            if (body != null && body.isDead) {
                body = null;
            }
            Entity dedupeEntity = body == null ? outline.source : body;
            int entityId = dedupeEntity.getEntityId();
            boolean alreadyDrawn = false;
            for (int i = 0; i < drawnM7EntityIdCount; i++) {
                if (drawnM7EntityIds[i] == entityId) {
                    alreadyDrawn = true;
                    break;
                }
            }
            if (alreadyDrawn) {
                continue;
            }
            if (drawnM7EntityIdCount < drawnM7EntityIds.length) {
                drawnM7EntityIds[drawnM7EntityIdCount++] = entityId;
            }
            if (outline.bossTarget != null) {
                if (body != null) {
                    drawEntityBox(body, outline.bossTarget.color, partialTicks,
                            cameraX, cameraY, cameraZ);
                } else {
                    drawApproximateBossBox(outline.source, outline.bossTarget.color,
                            partialTicks, cameraX, cameraY, cameraZ);
                }
            } else if (outline.dragonStatue != null) {
                if (body != null) {
                    drawEntityBox(body, outline.dragonStatue.color, partialTicks,
                            cameraX, cameraY, cameraZ);
                } else {
                    drawApproximateDragonBox(outline.source, outline.dragonStatue.color,
                            partialTicks, cameraX, cameraY, cameraZ);
                }
            }
        }
    }

    private void clearM7TargetCache() {
        cachedM7Outlines.clear();
        m7TargetCacheReady = false;
    }

    private void clearRareCreatureCache() {
        cachedRareCreatureEntities.clear();
        rareCreatureCacheReady = false;
    }

    private void renderRareSeaCreatureOutline(Entity entity, Minecraft minecraft, float partialTicks,
                                               double cameraX, double cameraY, double cameraZ) {
        if (!(entity instanceof EntityLivingBase) || entity instanceof EntityArmorStand
                || entity == minecraft.thePlayer || entity.isDead
                || entity.getDistanceSqToEntity(minecraft.thePlayer) > 64.0D * 64.0D) {
            return;
        }
        AxisAlignedBB entityBox = entity.getEntityBoundingBox();
        if (entityBox == null) {
            return;
        }
        double entityX = interpolate(entity.lastTickPosX, entity.posX, partialTicks);
        double entityY = interpolate(entity.lastTickPosY, entity.posY, partialTicks);
        double entityZ = interpolate(entity.lastTickPosZ, entity.posZ, partialTicks);
        AxisAlignedBB renderBox = entityBox.offset(entityX - entity.posX - cameraX,
                entityY - entity.posY - cameraY, entityZ - entity.posZ - cameraZ);
        drawOutlinedBox(renderBox, 0xFFFFD45C, false);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event != null && event.world != null && event.world.isRemote) {
            resetWorldState();
            floorSevenScoreboardDetected = false;
        }
    }

    private void renderEntityHitbox(Entity entity, List<Entity> loadedEntities, float partialTicks,
                                    double cameraX, double cameraY, double cameraZ) {
        if (entity == null || entity.isDead) {
            return;
        }

        String name = getEntityName(entity);
        boolean dragonEntity = entity instanceof EntityDragon;
        M7HitboxTarget target = classifyM7HitboxTarget(name, dragonEntity);
        if (entity instanceof EntityArmorStand) {
            // Hypixel sometimes puts the boss name on a separate invisible armor-stand
            // nameplate while the actual Wither entity is sent with a generic name.
            // Use the nameplate to identify the nearby body, not the tiny stand hitbox.
            if (target == null) {
                return;
            }
            if (target == M7HitboxTarget.DRAGON) {
                M7DragonStatue statue = dragonStatueForName(name);
                if (statue == null) {
                    statue = nearestM7DragonStatue(entity.posX, entity.posZ);
                }
                EntityDragon dragon = findNearbyDragon(entity, loadedEntities);
                if (statue != null && mod.isM7DragonHitboxEnabled(statue)) {
                    if (dragon != null) {
                        drawEntityBox(dragon, statue.color, partialTicks, cameraX, cameraY, cameraZ);
                    } else {
                        drawApproximateDragonBox(entity, statue.color, partialTicks,
                                cameraX, cameraY, cameraZ);
                    }
                }
            } else if (mod.isM7BossHitboxEnabled(target)) {
                Entity bossBody = findNearbyBossBody(entity, loadedEntities);
                if (bossBody != null) {
                    drawEntityBox(bossBody, target.color, partialTicks, cameraX, cameraY, cameraZ);
                } else {
                    // Some servers send only the named armor-stand label for a boss. Keep a
                    // visible approximate outline at that label instead of silently showing nothing.
                    drawApproximateBossBox(entity, target.color, partialTicks,
                            cameraX, cameraY, cameraZ);
                }
            }
            return;
        }
        if (target == null) {
            return;
        }

        if (target == M7HitboxTarget.DRAGON) {
            M7DragonStatue statue = dragonStatueForName(name);
            if (statue == null) {
                statue = dragonTypesByEntityId.get(entity.getEntityId());
            }
            if (statue == null) {
                statue = nearestM7DragonStatue(entity.posX, entity.posZ);
            }
            if (statue == null || !mod.isM7DragonHitboxEnabled(statue)) {
                return;
            }
            drawEntityBox(entity, statue.color, partialTicks, cameraX, cameraY, cameraZ);
        } else if (mod.isM7BossHitboxEnabled(target)) {
            drawEntityBox(entity, target.color, partialTicks, cameraX, cameraY, cameraZ);
        }
    }

    private Entity findNearbyBossBody(Entity nameplate, List<? extends Entity> loadedEntities) {
        EntityWither nearestWither = null;
        double nearestWitherDistanceSq = Double.MAX_VALUE;
        EntityLivingBase nearestLiving = null;
        double nearestLivingDistanceSq = Double.MAX_VALUE;
        for (Entity candidate : loadedEntities) {
            if (candidate == null || candidate == nameplate || candidate.isDead
                    || candidate instanceof EntityArmorStand || candidate instanceof EntityPlayer
                    || candidate instanceof EntityDragon || !(candidate instanceof EntityLivingBase)) {
                continue;
            }
            double dx = candidate.posX - nameplate.posX;
            double dy = candidate.posY - nameplate.posY;
            double dz = candidate.posZ - nameplate.posZ;
            double horizontalDistanceSq = dx * dx + dz * dz;
            // Nameplates can drift above or away from their entity as bosses move; search
            // the full nearby arena rather than requiring the label to sit on the body.
            if (horizontalDistanceSq > 256.0D || Math.abs(dy) > 16.0D) {
                continue;
            }
            double distanceSq = horizontalDistanceSq + dy * dy;
            if (candidate instanceof EntityWither) {
                if (distanceSq < nearestWitherDistanceSq) {
                    nearestWither = (EntityWither) candidate;
                    nearestWitherDistanceSq = distanceSq;
                }
            } else if (horizontalDistanceSq <= 64.0D && distanceSq < nearestLivingDistanceSq) {
                nearestLiving = (EntityLivingBase) candidate;
                nearestLivingDistanceSq = distanceSq;
            }
        }
        return nearestWither != null ? nearestWither : nearestLiving;
    }

    private EntityDragon findNearbyDragon(Entity nameplate, List<? extends Entity> loadedEntities) {
        EntityDragon nearest = null;
        double nearestDistanceSq = Double.MAX_VALUE;
        for (Entity candidate : loadedEntities) {
            if (!(candidate instanceof EntityDragon) || candidate.isDead) {
                continue;
            }
            double dx = candidate.posX - nameplate.posX;
            double dy = candidate.posY - nameplate.posY;
            double dz = candidate.posZ - nameplate.posZ;
            double horizontalDistanceSq = dx * dx + dz * dz;
            double distanceSq = horizontalDistanceSq + dy * dy;
            if (horizontalDistanceSq <= 576.0D && Math.abs(dy) <= 32.0D
                    && distanceSq < nearestDistanceSq) {
                nearest = (EntityDragon) candidate;
                nearestDistanceSq = distanceSq;
            }
        }
        return nearest;
    }

    private static void drawApproximateDragonBox(Entity nameplate, int color, float partialTicks,
                                                  double cameraX, double cameraY, double cameraZ) {
        double x = interpolate(nameplate.lastTickPosX, nameplate.posX, partialTicks);
        double y = interpolate(nameplate.lastTickPosY, nameplate.posY, partialTicks);
        double z = interpolate(nameplate.lastTickPosZ, nameplate.posZ, partialTicks);
        AxisAlignedBB approximate = new AxisAlignedBB(x - 8.0D, y - 8.0D, z - 8.0D,
                x + 8.0D, y + 0.5D, z + 8.0D).offset(-cameraX, -cameraY, -cameraZ);
        drawOutlinedBox(approximate, color);
    }

    private static void drawApproximateBossBox(Entity nameplate, int color, float partialTicks,
                                                double cameraX, double cameraY, double cameraZ) {
        double x = interpolate(nameplate.lastTickPosX, nameplate.posX, partialTicks);
        double y = interpolate(nameplate.lastTickPosY, nameplate.posY, partialTicks);
        double z = interpolate(nameplate.lastTickPosZ, nameplate.posZ, partialTicks);
        AxisAlignedBB approximate = new AxisAlignedBB(x - 1.25D, y - 3.25D, z - 1.25D,
                x + 1.25D, y + 0.25D, z + 1.25D).offset(-cameraX, -cameraY, -cameraZ);
        drawOutlinedBox(approximate, color);
    }

    private static void drawEntityBox(Entity entity, int color, float partialTicks,
                                      double cameraX, double cameraY, double cameraZ) {
        AxisAlignedBB entityBox = entity.getEntityBoundingBox();
        if (entityBox == null) {
            return;
        }

        double entityX = interpolate(entity.lastTickPosX, entity.posX, partialTicks);
        double entityY = interpolate(entity.lastTickPosY, entity.posY, partialTicks);
        double entityZ = interpolate(entity.lastTickPosZ, entity.posZ, partialTicks);
        AxisAlignedBB renderBox = entityBox.offset(entityX - entity.posX - cameraX,
                entityY - entity.posY - cameraY, entityZ - entity.posZ - cameraZ);
        drawOutlinedBox(renderBox, color);
    }

    private void resetWorldState() {
        dragonTypesByEntityId.clear();
        clearM7TargetCache();
        clearRareCreatureCache();
        cachedWorld = null;
        lastEntityCacheRefreshAt = 0L;
        lastScoreboardRefreshAt = 0L;
        floorSevenScoreboardDetected = false;
        m7ScoreboardDetected = false;
        fakepixelM7DungeonRunId = null;
        resetM7DragonPhaseState();
    }

    private void resetM7DragonPhaseState() {
        m7FinalPhaseSeen = false;
        m7DragonSpawnedAfterFinalPhase = false;
    }

    private boolean hasDragonLoadedInCurrentWorld() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.theWorld == null) {
            return false;
        }
        for (Entity entity : minecraft.theWorld.loadedEntityList) {
            if (entity instanceof EntityDragon) {
                return true;
            }
        }
        return false;
    }

    private static double interpolate(double previous, double current, float partialTicks) {
        return previous + (current - previous) * partialTicks;
    }

    static boolean isFloorSevenScoreboardText(String text) {
        if (text == null) {
            return false;
        }
        String normalized = SpiderDropAlert.stripFormattingCodes(text).toUpperCase(Locale.ROOT);
        String compact = normalized.replaceAll("[^A-Z0-9]+", "");
        boolean explicitM7 = normalized.matches("(?s).*\\bM\\s*7\\b.*")
                || normalized.matches("(?s).*\\bM\\s*VII\\b.*")
                || normalized.matches("(?s).*\\bMM\\s*7\\b.*")
                || compact.contains("M7") || compact.contains("MMF7");
        boolean floorSeven = normalized.contains("FLOOR VII") || normalized.contains("FLOOR 7")
                || normalized.matches("(?s).*\\bF\\s*7\\b.*")
                || normalized.matches("(?s).*\\bF\\s*VII\\b.*")
                || compact.contains("F7") || compact.contains("FLOORVII")
                || compact.contains("FLOOR7") || compact.contains("MASTERMODEVII")
                || compact.contains("MASTERMODE7");
        boolean masterMode = normalized.contains("MASTER MODE")
                || normalized.matches("(?s).*\\bMM\\b.*")
                || compact.contains("MASTERMODE") || compact.contains("MMF7");
        boolean catacombs = compact.contains("CATACOMBS");
        boolean fakepixelM7 = hasFakepixelM7DragonScoreboard(normalized, compact);
        // Normal F7 sidebars use Catacombs + Floor VII/F7; Master Mode can identify itself
        // independently. Fakepixel uses a generic Dungeon/dung-* sidebar with dragon HP lines.
        return explicitM7 || fakepixelM7 || (floorSeven && (masterMode || catacombs));
    }

    static boolean isM7ScoreboardText(String text) {
        if (text == null) {
            return false;
        }
        String normalized = SpiderDropAlert.stripFormattingCodes(text).toUpperCase(Locale.ROOT);
        String compact = normalized.replaceAll("[^A-Z0-9]+", "");
        boolean explicitM7 = normalized.matches("(?s).*\\bM\\s*7\\b.*")
                || normalized.matches("(?s).*\\bM\\s*VII\\b.*")
                || normalized.matches("(?s).*\\bMM\\s*7\\b.*")
                || compact.contains("M7") || compact.contains("MMF7");
        boolean floorSeven = normalized.contains("FLOOR VII") || normalized.contains("FLOOR 7")
                || normalized.matches("(?s).*\\bF\\s*7\\b.*")
                || normalized.matches("(?s).*\\bF\\s*VII\\b.*")
                || compact.contains("F7") || compact.contains("FLOORVII")
                || compact.contains("FLOOR7") || compact.contains("MASTERMODEVII")
                || compact.contains("MASTERMODE7");
        boolean masterMode = normalized.contains("MASTER MODE")
                || normalized.matches("(?s).*\\bMM\\b.*")
                || compact.contains("MASTERMODE") || compact.contains("MMF7");
        boolean fakepixelM7 = hasFakepixelM7DragonScoreboard(normalized, compact);
        return explicitM7 || fakepixelM7 || (masterMode && floorSeven);
    }

    static boolean isFakepixelM7DragonScoreboardText(String text) {
        if (text == null) {
            return false;
        }
        String normalized = SpiderDropAlert.stripFormattingCodes(text).toUpperCase(Locale.ROOT);
        String compact = normalized.replaceAll("[^A-Z0-9]+", "");
        return hasFakepixelM7DragonScoreboard(normalized, compact);
    }

    private static boolean hasFakepixelM7DragonScoreboard(String normalized, String compact) {
        boolean dungeonRun = compact.matches("(?s).*DUNG[0-9]+.*");
        if (!compact.contains("SKYBLOCK") || !dungeonRun) {
            return false;
        }
        for (M7DragonStatue statue : M7DragonStatue.values()) {
            if (normalized.contains(statue.dragonName.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String fakepixelDungeonRunId(String text) {
        if (text == null) {
            return null;
        }
        String compact = SpiderDropAlert.stripFormattingCodes(text)
                .toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "");
        Matcher matcher = FAKEPIXEL_DUNGEON_RUN_ID_PATTERN.matcher(compact);
        return matcher.find() ? matcher.group(1) : null;
    }

    static boolean isM7FinalPhaseMessage(String message) {
        if (message == null) {
            return false;
        }
        String normalized = SpiderDropAlert.stripFormattingCodes(message)
                .toUpperCase(Locale.ROOT);
        return normalized.contains("THE CATACOMBS... ARE NO MORE");
    }

    static boolean shouldRenderM7DragonKillAreas(boolean m7Detected,
                                                  boolean finalPhaseSeen,
                                                  boolean dragonSpawned,
                                                  boolean featureEnabled,
                                                  boolean persistAfterDragonSpawn) {
        return m7Detected && finalPhaseSeen && featureEnabled
                && (persistAfterDragonSpawn || !dragonSpawned);
    }

    private String getSidebarScoreboardText(Minecraft minecraft) {
        if (minecraft == null || minecraft.theWorld == null) {
            return "";
        }
        Scoreboard scoreboard = minecraft.theWorld.getScoreboard();
        if (scoreboard == null) {
            return "";
        }
        ScoreObjective sidebar = scoreboard.getObjectiveInDisplaySlot(1);
        if (sidebar == null) {
            return "";
        }

        StringBuilder text = new StringBuilder(sidebar.getDisplayName());
        for (Score score : scoreboard.getSortedScores(sidebar)) {
            String playerName = score.getPlayerName();
            if (playerName == null || playerName.startsWith("#")) {
                continue;
            }
            ScorePlayerTeam team = scoreboard.getPlayersTeam(playerName);
            text.append(' ').append(ScorePlayerTeam.formatPlayerName(team, playerName));
        }
        return text.toString();
    }

    static M7HitboxTarget classifyM7HitboxTarget(String displayName, boolean dragonEntity) {
        String normalized = SpiderDropAlert.stripFormattingCodes(displayName == null ? "" : displayName)
                .toUpperCase(Locale.ROOT);
        if (normalized.contains("MAXOR")) {
            return M7HitboxTarget.MAXOR;
        }
        if (normalized.contains("STORM")) {
            return M7HitboxTarget.STORM;
        }
        if (normalized.contains("GOLDOR")) {
            return M7HitboxTarget.GOLDOR;
        }
        if (normalized.contains("NECRON")) {
            return M7HitboxTarget.NECRON;
        }
        if (dragonEntity || normalized.contains("DRAGON")) {
            return M7HitboxTarget.DRAGON;
        }
        return null;
    }

    static M7DragonStatue dragonStatueForName(String displayName) {
        String normalized = SpiderDropAlert.stripFormattingCodes(displayName == null ? "" : displayName)
                .toUpperCase(Locale.ROOT);
        for (M7DragonStatue statue : M7DragonStatue.values()) {
            if (normalized.contains(statue.dragonName.toUpperCase(Locale.ROOT))) {
                return statue;
            }
        }
        return null;
    }

    static M7DragonStatue nearestM7DragonStatue(double x, double z) {
        M7DragonStatue nearest = null;
        double nearestDistanceSq = Double.MAX_VALUE;
        for (M7DragonStatue statue : M7DragonStatue.values()) {
            double dx = x - statue.anchor.getX();
            double dz = z - statue.anchor.getZ();
            double distanceSq = dx * dx + dz * dz;
            if (distanceSq < nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                nearest = statue;
            }
        }
        return nearest;
    }

    private static String getEntityName(Entity entity) {
        if (entity == null) {
            return "";
        }
        StringBuilder name = new StringBuilder();
        if (entity.getDisplayName() != null) {
            name.append(entity.getDisplayName().getUnformattedText());
        }
        String customName = entity.getCustomNameTag();
        if (customName != null && customName.length() > 0) {
            name.append(' ').append(customName);
        }
        return name.toString();
    }

    private static void drawOutlinedBox(AxisAlignedBB box, int color) {
        drawOutlinedBox(box, color, true);
    }

    private static void drawOutlinedBox(AxisAlignedBB box, int color, boolean throughWalls) {
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableCull();
        if (throughWalls) {
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
        } else {
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
        }
        GL11.glLineWidth(2.0F);
        GL11.glColor4f(red, green, blue, 0.95F);
        GL11.glBegin(GL11.GL_LINES);
        drawBoxEdge(box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ);
        drawBoxEdge(box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ);
        drawBoxEdge(box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ);
        drawBoxEdge(box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ);
        drawBoxEdge(box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ);
        drawBoxEdge(box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ);
        drawBoxEdge(box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ);
        drawBoxEdge(box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ);
        drawBoxEdge(box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ);
        drawBoxEdge(box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ);
        drawBoxEdge(box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ);
        drawBoxEdge(box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ);
        GL11.glEnd();
        GL11.glLineWidth(1.0F);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private static void drawBoxEdge(double x1, double y1, double z1,
                                    double x2, double y2, double z2) {
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x2, y2, z2);
    }

    enum M7HitboxTarget {
        MAXOR(0xFFFF4545),
        STORM(0xFF9E62FF),
        GOLDOR(0xFFFFD34D),
        NECRON(0xFFDD59E8),
        DRAGON(0xFFF4F4F4);

        final int color;

        M7HitboxTarget(int color) {
            this.color = color;
        }
    }

    /** M7 relic-area box geometry adapted from NEF 1.2.4 DragonCloseAlert. */
    enum M7DragonKillArea {
        RED_RELIC(new BlockPos(43, 6, 64), -11, 21, -11, 13, 0xFFFF0000),
        GREEN_RELIC(new BlockPos(43, 6, 102), -11, 21, -11, 13, 0xFF00FF00),
        BLUE_RELIC(new BlockPos(85, 6, 102), -21, 11, -11, 13, 0xFF00FFFF),
        ORANGE_RELIC(new BlockPos(85, 6, 64), -21, 11, -11, 13, 0xFFFFC800),
        PURPLE_RELIC(new BlockPos(64, 6, 125), -11, 13, -21, 11, 0xFFFFAFAF);

        final AxisAlignedBB bounds;
        final int color;

        M7DragonKillArea(BlockPos orbPosition, int minXOffset, int maxXOffset,
                         int minZOffset, int maxZOffset, int color) {
            BlockPos areaOrigin = orbPosition.add(0, 20, 0);
            this.bounds = new AxisAlignedBB(
                    areaOrigin.getX() + minXOffset, areaOrigin.getY() - 11,
                    areaOrigin.getZ() + minZOffset,
                    areaOrigin.getX() + maxXOffset, areaOrigin.getY() + 11,
                    areaOrigin.getZ() + maxZOffset);
            this.color = color;
        }
    }

    enum M7DragonStatue {
        POWER("Power Dragon", new BlockPos(27, 14, 59), 0xFFFF4545),
        APEX("Apex Dragon", new BlockPos(27, 14, 94), 0xFF78EB4F),
        SOUL("Soul Dragon", new BlockPos(56, 14, 125), 0xFFB35CFF),
        ICE("Ice Dragon", new BlockPos(84, 14, 94), 0xFF45C9FF),
        FLAME("Flame Dragon", new BlockPos(85, 14, 56), 0xFFFF9138);

        final String dragonName;
        final BlockPos anchor;
        final int color;

        M7DragonStatue(String dragonName, BlockPos anchor, int color) {
            this.dragonName = dragonName;
            this.anchor = anchor;
            this.color = color;
        }
    }
}

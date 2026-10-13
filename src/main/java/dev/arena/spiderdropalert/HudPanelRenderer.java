package dev.arena.spiderdropalert;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

/** Draws HUD panels from the state managed by {@link SpiderDropAlert}. */
final class HudPanelRenderer {
    private final SpiderDropAlert mod;

    HudPanelRenderer(SpiderDropAlert mod) {
        this.mod = mod;
    }

    static int panelBackgroundColor(int opacityPercent) {
        int opacity = SpiderDropAlert.clamp(opacityPercent,
                SpiderDropAlert.MIN_HUD_BACKGROUND_OPACITY_PERCENT,
                SpiderDropAlert.MAX_HUD_BACKGROUND_OPACITY_PERCENT);
        int alpha = Math.round(opacity * 2.55F);
        return (alpha << 24) | 0x0A0D14;
    }

    static void drawPanelBackground(int x, int y, int panelWidth, int panelHeight,
                                    int opacityPercent) {
        if (panelWidth <= 0 || panelHeight <= 0) {
            return;
        }
        Gui.drawRect(x, y, x + panelWidth, y + panelHeight,
                panelBackgroundColor(opacityPercent));
    }

    void drawTrackerPanel(int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.fontRendererObj == null) {
            return;
        }

        FontRenderer font = minecraft.fontRendererObj;
        float scale = mod.getTrackerScale();
        int panelWidth = mod.getPanelWidth(screenWidth);
        int panelHeight = mod.getPanelHeight();
        int renderedWidth = Math.round(panelWidth * scale);
        int renderedHeight = Math.round(panelHeight * scale);
        int x = SpiderDropAlert.clamp(mod.getTrackerX(), 0, Math.max(0, screenWidth - renderedWidth));
        int y = SpiderDropAlert.clamp(mod.getTrackerY(), 0, Math.max(0, screenHeight - renderedHeight));
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, 0.0F);
        GlStateManager.scale(scale, scale, 1.0F);
        drawPanelBackground(0, 0, panelWidth, panelHeight,
                mod.getHudPanelBackgroundOpacity(SpiderDropAlert.HUD_PANEL_SLAYER));

        List<SpiderDropAlert.DropEntry> activeDrops = mod.getDropHistory(mod.getActiveSlayer());
        font.drawStringWithShadow(font.trimStringToWidth("RNG carried Bald", panelWidth - 12),
                6, 5, 0xFFFFFF);

        int rowY = SpiderDropAlert.HEADER_HEIGHT + 2;
        if (activeDrops.isEmpty()) {
            String waiting = "Waiting for drops...";
            font.drawStringWithShadow(font.trimStringToWidth(waiting, panelWidth - 12),
                    6, SpiderDropAlert.HEADER_HEIGHT + 5, 0xBBBBBB);
            rowY = SpiderDropAlert.HEADER_HEIGHT + 17;
        } else {
            for (int i = 0; i < activeDrops.size(); i++) {
                SpiderDropAlert.DropEntry entry = activeDrops.get(i);
                Long unitPrice = mod.getItemPrice(entry.item);
                String totalValue = SpiderDropAlert.getDropValue(entry.quantity, unitPrice);
                String quantityPrefix = NumberFormat.getIntegerInstance(Locale.US).format(entry.quantity) + "x ";
                String valueSuffix = "(" + totalValue + ")";
                int nameWidth = Math.max(0, panelWidth - 12
                        - font.getStringWidth(quantityPrefix) - font.getStringWidth(valueSuffix));
                String itemLine = quantityPrefix
                        + font.trimStringToWidth(entry.item, nameWidth) + valueSuffix;

                font.drawStringWithShadow(font.trimStringToWidth(itemLine, panelWidth - 12),
                        6, rowY, 0xFFFFFF);
                rowY += SpiderDropAlert.ITEM_ONLY_ENTRY_HEIGHT;
            }
        }

        rowY += 3;
        Gui.drawRect(6, rowY, panelWidth - 6, rowY + 1, 0x88FFFF99);
        rowY += 5;
        String profitLine = "Total profit = " + SpiderDropAlert.formatCompactPrice(mod.getVisibleTotalProfit());
        font.drawStringWithShadow(font.trimStringToWidth(profitLine, panelWidth - 12),
                6, rowY, 0xFFFF99);
        if (mod.hasSlayerBossTimer()) {
            String timerLine = "Boss timer - " + SpiderDropAlert.formatTimerDuration(
                    mod.getSlayerBossTimerElapsedMillis(System.currentTimeMillis()));
            font.drawStringWithShadow(font.trimStringToWidth(timerLine, panelWidth - 12),
                    6, rowY + SpiderDropAlert.ITEM_ONLY_ENTRY_HEIGHT, 0xE8E6EB);
        }

        GlStateManager.popMatrix();
    }

    void drawFishingTrackerPanel(int screenWidth, int screenHeight) {
        if (!mod.isFishingSectionVisible()) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.fontRendererObj == null) {
            return;
        }

        FontRenderer font = minecraft.fontRendererObj;
        float scale = mod.getTrackerScale();
        int panelWidth = mod.getPanelWidth(screenWidth);
        int panelHeight = mod.getFishingPanelHeight();
        int renderedWidth = Math.round(panelWidth * scale);
        int renderedHeight = Math.round(panelHeight * scale);
        int x = SpiderDropAlert.clamp(mod.getFishingX(), 0, Math.max(0, screenWidth - renderedWidth));
        int y = SpiderDropAlert.clamp(mod.getFishingY(), 0, Math.max(0, screenHeight - renderedHeight));
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, 0.0F);
        GlStateManager.scale(scale, scale, 1.0F);
        drawPanelBackground(0, 0, panelWidth, panelHeight,
                mod.getHudPanelBackgroundOpacity(SpiderDropAlert.HUD_PANEL_FISHING));

        font.drawStringWithShadow("Fishing", 6, 5, 0xFFFFFF);
        int rowY = SpiderDropAlert.HEADER_HEIGHT + 2;
        long now = System.currentTimeMillis();
        if (mod.isFishingMobCounterEnabled()) {
            if (mod.hasFishingTimer()) {
                String timerLine = "Fishing timer - " + SpiderDropAlert.formatTimerDuration(
                        mod.getFishingTimerElapsedMillis(now));
                font.drawStringWithShadow(font.trimStringToWidth(timerLine, panelWidth - 12),
                        6, rowY, 0xE8E6EB);
                rowY += SpiderDropAlert.ITEM_ONLY_ENTRY_HEIGHT;
            }
            if (mod.hasSplashToPullTimer()) {
                String splashToPullLine = "Splash -> pull - "
                        + SpiderDropAlert.formatSplashToPullDuration(
                        mod.getSplashToPullTimerElapsedMillis(now));
                font.drawStringWithShadow(font.trimStringToWidth(splashToPullLine, panelWidth - 12),
                        6, rowY, 0xE8E6EB);
                rowY += SpiderDropAlert.ITEM_ONLY_ENTRY_HEIGHT;
            }
            for (LegendaryCreature creature : LegendaryCreature.values()) {
                long count = mod.getLegendaryCreatureCount(creature);
                if (count <= 0L) {
                    continue;
                }
                String creatureLine = SpiderDropAlert.formatLegendaryCreatureLabel(creature, count);
                font.drawStringWithShadow(font.trimStringToWidth(creatureLine, panelWidth - 12),
                        6, rowY, 0xE8E6EB);
                rowY += SpiderDropAlert.ITEM_ONLY_ENTRY_HEIGHT;
            }
            if (mod.getTotalLegendaryCreatureCount() == 0L && !mod.hasFishingTimer()) {
                font.drawStringWithShadow("Waiting for catches...", 6, rowY, 0xBBBBBB);
                rowY += SpiderDropAlert.ITEM_ONLY_ENTRY_HEIGHT;
            }
        }

        if (mod.isFishingEfficiencyTrackerEnabled()) {
            long tracked = mod.getFishingEfficiencyCatchCount();
            if (tracked <= 0L) {
                font.drawStringWithShadow("Waiting for tracked catches...", 6, rowY, 0xBBBBBB);
                rowY += SpiderDropAlert.ITEM_ONLY_ENTRY_HEIGHT;
            } else {
                String countLine = "Tracked creatures - " + SpiderDropAlert.formatPrice(tracked);
                font.drawStringWithShadow(font.trimStringToWidth(countLine, panelWidth - 12),
                        6, rowY, 0xE8E6EB);
                rowY += SpiderDropAlert.ITEM_ONLY_ENTRY_HEIGHT;
                long elapsed = mod.getFishingEfficiencyElapsedMillis(now);
                String rateLine = elapsed < 60000L ? "Tracked / hour - warming up"
                        : "Tracked / hour - " + String.format(Locale.US, "%.1f",
                        mod.getFishingEfficiencyCatchesPerHour(now));
                font.drawStringWithShadow(font.trimStringToWidth(rateLine, panelWidth - 12),
                        6, rowY, 0xE8E6EB);
                rowY += SpiderDropAlert.ITEM_ONLY_ENTRY_HEIGHT;
            }
        }

        if (mod.isFishingFestivalTrackerEnabled()) {
            long festivalCount = mod.getFishingFestivalGreatWhiteCount(now);
            long personalBest = mod.getFishingFestivalGreatWhiteBest();
            if (festivalCount > 0L) {
                String festivalLine = "Great White festival - "
                        + SpiderDropAlert.formatPrice(festivalCount);
                font.drawStringWithShadow(font.trimStringToWidth(festivalLine, panelWidth - 12),
                        6, rowY, 0xE8E6EB);
                rowY += SpiderDropAlert.ITEM_ONLY_ENTRY_HEIGHT;
            }
            if (personalBest > 0L) {
                String bestLine = "Great White PB - " + SpiderDropAlert.formatPrice(personalBest);
                font.drawStringWithShadow(font.trimStringToWidth(bestLine, panelWidth - 12),
                        6, rowY, 0xE8E6EB);
                rowY += SpiderDropAlert.ITEM_ONLY_ENTRY_HEIGHT;
            }
            if (festivalCount == 0L && personalBest == 0L) {
                font.drawStringWithShadow("Waiting for a Great White...", 6, rowY, 0xBBBBBB);
                rowY += SpiderDropAlert.ITEM_ONLY_ENTRY_HEIGHT;
            }
        }

        long totalCreatureCount = mod.getTotalLegendaryCreatureCount();
        if (mod.isFishingMobCounterEnabled() && totalCreatureCount > 0L) {
            rowY += 3;
            Gui.drawRect(6, rowY, panelWidth - 6, rowY + 1, 0x88FFFF99);
            rowY += 5;
            String totalCreatureLine = "Total creature - "
                    + SpiderDropAlert.formatPrice(totalCreatureCount);
            font.drawStringWithShadow(font.trimStringToWidth(totalCreatureLine, panelWidth - 12),
                    6, rowY, 0xFFFF99);
        }
        GlStateManager.popMatrix();
    }

    void drawDeployablesTimerPanel(int screenWidth, int screenHeight) {
        long now = System.currentTimeMillis();
        List<DeployableItem> activeItems = mod.getActiveDeployableItems(now);
        if (!mod.isDeployablesTimerPanelEnabled() || activeItems.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.fontRendererObj == null) {
            return;
        }

        FontRenderer font = minecraft.fontRendererObj;
        float scale = mod.getDeployablesTimerPanelScale();
        int panelWidth = Math.min(SpiderDropAlert.DEPLOYABLE_PANEL_WIDTH, Math.max(80, screenWidth - 20));
        int panelHeight = SpiderDropAlert.getDeployablesTimerPanelHeight(activeItems.size());
        int renderedWidth = Math.round(panelWidth * scale);
        int renderedHeight = Math.round(panelHeight * scale);
        int x = SpiderDropAlert.clamp(mod.getDeployablesTimerPanelX(), 0, Math.max(0, screenWidth - renderedWidth));
        int y = SpiderDropAlert.clamp(mod.getDeployablesTimerPanelY(), 0, Math.max(0, screenHeight - renderedHeight));
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, 0.0F);
        GlStateManager.scale(scale, scale, 1.0F);
        drawPanelBackground(0, 0, panelWidth, panelHeight,
                mod.getHudPanelBackgroundOpacity(SpiderDropAlert.HUD_PANEL_DEPLOYABLES));

        font.drawStringWithShadow("Deployables", 6, 5, 0xFFFFFF);
        int rowY = SpiderDropAlert.HEADER_HEIGHT + 2;
        for (DeployableItem item : activeItems) {
            int seconds = mod.getDeployableRemainingSeconds(item, now);
            if (seconds <= 0) {
                continue;
            }
            int color = seconds <= 10 ? 0xFF5555 : 0xE8E6EB;
            String line = item.getDisplayName() + ": "
                    + SpiderDropAlert.formatDeployableTimer(seconds);
            font.drawStringWithShadow(font.trimStringToWidth(line, panelWidth - 12), 6, rowY, color);
            rowY += SpiderDropAlert.ITEM_ONLY_ENTRY_HEIGHT;
        }
        GlStateManager.popMatrix();
    }

    private static void outlineCell(int x, int y, int w, int h) {
        Gui.drawRect(x, y, x + w, y + 1, 0xFF6E6E76);
        Gui.drawRect(x, y + h - 1, x + w, y + h, 0xFF6E6E76);
        Gui.drawRect(x, y, x + 1, y + h, 0xFF6E6E76);
        Gui.drawRect(x + w - 1, y, x + w, y + h, 0xFF6E6E76);
    }

    private static void drawCell(int x, int y, int w, int h) {
        Gui.drawRect(x, y, x + w, y + h, 0xFF141416);
        Gui.drawRect(x, y, x + w, y + 1, 0xFF6E6E76);
        Gui.drawRect(x, y + h - 1, x + w, y + h, 0xFF6E6E76);
        Gui.drawRect(x, y, x + 1, y + h, 0xFF6E6E76);
        Gui.drawRect(x + w - 1, y, x + w, y + h, 0xFF6E6E76);
    }

    private static void drawCentered(FontRenderer font, String text, int centerX, int y,
                                     int color) {
        font.drawStringWithShadow(text, centerX - font.getStringWidth(text) / 2, y, color);
    }

    void drawNearbyCreatureHpPanel(int screenWidth, int screenHeight) {
        if (!mod.isNearbyCreatureHpVisible() || mod.getNearbyCreatureHpEntries().isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.fontRendererObj == null) {
            return;
        }

        FontRenderer font = minecraft.fontRendererObj;
        float scale = mod.getNearbyCreatureHpScale();
        int panelWidth = mod.getNearbyCreatureHpPanelWidth(screenWidth);
        int panelHeight = mod.getNearbyCreatureHpPanelHeight();
        int renderedWidth = Math.round(panelWidth * scale);
        int renderedHeight = Math.round(panelHeight * scale);
        int x = SpiderDropAlert.clamp(mod.getNearbyCreatureHpX(), 0, Math.max(0, screenWidth - renderedWidth));
        int y = SpiderDropAlert.clamp(mod.getNearbyCreatureHpY(), 0, Math.max(0, screenHeight - renderedHeight));
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, 0.0F);
        GlStateManager.scale(scale, scale, 1.0F);
        drawPanelBackground(0, 0, panelWidth, panelHeight,
                mod.getHudPanelBackgroundOpacity(SpiderDropAlert.HUD_PANEL_CREATURE_HP));

        font.drawStringWithShadow(font.trimStringToWidth("\u00A7e\u00A7lSea Creatures HP",
                panelWidth - 12), 6, 5, 0xFFFFFF);
        int rowY = SpiderDropAlert.HEADER_HEIGHT + 2;
        for (SpiderDropAlert.NearbyCreatureHpEntry entry : mod.getNearbyCreatureHpEntries()) {
            font.drawStringWithShadow(font.trimStringToWidth(entry.toHudLabel(), panelWidth - 12),
                    6, rowY, 0xFFFFFF);
            rowY += SpiderDropAlert.ITEM_ONLY_ENTRY_HEIGHT;
        }
        GlStateManager.popMatrix();
    }

}

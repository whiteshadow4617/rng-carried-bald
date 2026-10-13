package dev.arena.spiderdropalert;

import java.io.IOException;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/** Full-screen editor that lets each HUD panel be positioned and resized in one view. */
final class HudLayoutEditorScreen extends GuiScreen {
    private static final int PANEL_SLAYER = 0;
    private static final int PANEL_FISHING = 1;
    private static final int PANEL_CREATURE_HP = 2;
    private static final int PANEL_DEPLOYABLES = 3;
    static final int PANEL_COUNT = 4;
    private static final int BUTTON_DONE = 0;
    private static final int RESIZE_HANDLE_SIZE = 11;
    /** Pixels the corner must travel for one 1% size step, so the drag feels smooth. */
    private static final int RESIZE_PIXELS_PER_STEP = 2;
    private static final int BUTTON_HEIGHT = 20;
    private static final String[] PANEL_NAMES = {
            "Slayer", "Fishing", "Sea Creature HP", "Deployables"
    };

    /** The panel labels the editor shows, in panel order. */
    static String[] panelNames() {
        return PANEL_NAMES.clone();
    }

    private final SpiderDropAlert mod;
    private final GuiScreen parent;
    private int selectedPanel = PANEL_SLAYER;
    private int draggingPanel = -1;
    private boolean resizingPanel;
    private int dragOffsetX;
    private int dragOffsetY;
    private int resizeStartX;
    private int resizeStartY;
    private int resizeStartPercent;

    HudLayoutEditorScreen(SpiderDropAlert mod, GuiScreen parent) {
        this.mod = mod;
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(BUTTON_DONE, width - 72, 7, 64, BUTTON_HEIGHT, "Done"));

        mod.clampTrackerPosition(width, height);
        mod.clampFishingTrackerPosition(width, height);
        mod.clampNearbyCreatureHpPosition(width, height);
        mod.clampDeployablesTimerPanelPosition(width, height);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        Gui.drawRect(0, 0, width, height, 0x55000000);
        fontRendererObj.drawStringWithShadow("HUD Layout Editor", 10, 8, 0xFFFFFFFF);
        fontRendererObj.drawStringWithShadow(
                "Drag a box to move it; drag its corner handle to resize.",
                10, 22, 0xFFD7D4DE);
        fontRendererObj.drawStringWithShadow(
                "1 Slayer   2 Fishing   3 Sea Creature HP   4 Deployables",
                10, 35, 0xFFAAA6B2);

        for (int panel = 0; panel < PANEL_COUNT; panel++) {
            if (panel != selectedPanel) {
                drawPanel(panel, false);
            }
        }
        drawPanel(selectedPanel, true);

        if (resizingPanel && draggingPanel >= 0) {
            String sizeText = "Size " + getPanelSizePercent(draggingPanel) + "%";
            int textWidth = fontRendererObj.getStringWidth(sizeText);
            int readoutX = Math.max(2, Math.min(width - textWidth - 8, mouseX + 10));
            int readoutY = Math.max(2, mouseY - 16);
            Gui.drawRect(readoutX - 4, readoutY - 3, readoutX + textWidth + 4, readoutY + 11,
                    0xE0101418);
            fontRendererObj.drawStringWithShadow(sizeText, readoutX, readoutY, 0xFFFFFFFF);
        }

        BoxBounds selectedBounds = getPanelBounds(selectedPanel);
        String status = PANEL_NAMES[selectedPanel] + "   X " + selectedBounds.x
                + "   Y " + selectedBounds.y + "   Size " + getPanelSizePercent(selectedPanel) + "%";
        fontRendererObj.drawStringWithShadow(fontRendererObj.trimStringToWidth(status,
                Math.max(0, width - 20)), 10, height - 13, 0xFFFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawPanel(int panel, boolean selected) {
        BoxBounds bounds = getPanelBounds(panel);
        int opacity = mod.getHudPanelBackgroundOpacity(getHudPanelType(panel));
        int edge = selected ? 0xFF35E3FF : 0xCC70A5C4;
        int edgeWidth = selected ? 2 : 1;

        Gui.drawRect(bounds.x, bounds.y, bounds.x + bounds.width,
                bounds.y + bounds.height, HudPanelRenderer.panelBackgroundColor(opacity));
        Gui.drawRect(bounds.x, bounds.y, bounds.x + bounds.width,
                bounds.y + edgeWidth, edge);
        Gui.drawRect(bounds.x, bounds.y + bounds.height - edgeWidth,
                bounds.x + bounds.width, bounds.y + bounds.height, edge);
        Gui.drawRect(bounds.x, bounds.y, bounds.x + edgeWidth,
                bounds.y + bounds.height, edge);
        Gui.drawRect(bounds.x + bounds.width - edgeWidth, bounds.y,
                bounds.x + bounds.width, bounds.y + bounds.height, edge);

        String label = (panel + 1) + " " + PANEL_NAMES[panel];
        int availableLabelWidth = Math.max(0, bounds.width - 8);
        if (bounds.width >= 38 && bounds.height >= fontRendererObj.FONT_HEIGHT + 4) {
            fontRendererObj.drawStringWithShadow(fontRendererObj.trimStringToWidth(label,
                    availableLabelWidth), bounds.x + 4, bounds.y + 3,
                    selected ? 0xFFFFFFFF : 0xFFE1E8EE);
        } else {
            Gui.drawRect(bounds.x, bounds.y, bounds.x + Math.min(13, bounds.width),
                    bounds.y + Math.min(12, bounds.height), edge);
            if (bounds.width >= 7 && bounds.height >= 8) {
                fontRendererObj.drawStringWithShadow(Integer.toString(panel + 1),
                        bounds.x + 3, bounds.y + 2, 0xFFFFFFFF);
            }
        }

        if (bounds.width >= 82 && bounds.height >= 28) {
            String position = "X " + bounds.x + "  Y " + bounds.y;
            fontRendererObj.drawStringWithShadow(fontRendererObj.trimStringToWidth(position,
                    Math.max(0, bounds.width - 8)), bounds.x + 4, bounds.y + 14, 0xFFD0D4DC);
        }

        int handle = Math.max(5, Math.min(RESIZE_HANDLE_SIZE,
                Math.min(bounds.width, bounds.height)));
        int handleColor = selected ? 0xFFFFFFFF : 0xFFB8D7E8;
        Gui.drawRect(bounds.x + bounds.width - handle,
                bounds.y + bounds.height - handle,
                bounds.x + bounds.width, bounds.y + bounds.height, handleColor);
        if (handle >= 7) {
            Gui.drawRect(bounds.x + bounds.width - handle + 2,
                    bounds.y + bounds.height - handle + 2,
                    bounds.x + bounds.width - 2,
                    bounds.y + bounds.height - 2, 0xFF344451);
            // three grip dots, so the corner reads as a drag handle
            for (int i = 1; i <= 3; i++) {
                int dotX = bounds.x + bounds.width - 2 - i * 3;
                int dotY = bounds.y + bounds.height - 2 - (4 - i) * 3;
                Gui.drawRect(dotX, dotY, dotX + 2, dotY + 2, 0xFFFFFFFF);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mc.currentScreen != this || mouseButton != 0) {
            return;
        }

        for (int panel = PANEL_COUNT - 1; panel >= 0; panel--) {
            BoxBounds bounds = getPanelBounds(panel);
            if (!isInsideExpandedBounds(bounds, mouseX, mouseY)) {
                continue;
            }

            selectedPanel = panel;
            draggingPanel = panel;
            int handle = Math.max(5, Math.min(RESIZE_HANDLE_SIZE,
                    Math.min(bounds.width, bounds.height)));
            // The grab area reaches a little past the drawn corner so it is easy to catch.
            boolean hitHandle = mouseX >= bounds.x + bounds.width - handle - 5
                    && mouseY >= bounds.y + bounds.height - handle - 5;
            resizingPanel = hitHandle;
            if (resizingPanel) {
                resizeStartX = mouseX;
                resizeStartY = mouseY;
                resizeStartPercent = getPanelSizePercent(panel);
            } else {
                dragOffsetX = mouseX - bounds.x;
                dragOffsetY = mouseY - bounds.y;
            }
            return;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton,
                                  long timeSinceLastClick) {
        if (draggingPanel >= 0 && clickedMouseButton == 0) {
            if (resizingPanel) {
                resizePanelFromMouse(mouseX, mouseY);
            } else {
                movePanelTo(draggingPanel, mouseX - dragOffsetX, mouseY - dragOffsetY);
            }
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (draggingPanel >= 0) {
            if (!resizingPanel) {
                movePanelTo(draggingPanel, mouseX - dragOffsetX, mouseY - dragOffsetY);
            }
            mod.saveSettings();
        }
        draggingPanel = -1;
        resizingPanel = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            closeEditor();
            return;
        }
        if (typedChar >= '1' && typedChar <= '4') {
            selectedPanel = typedChar - '1';
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BUTTON_DONE) {
            closeEditor();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void closeEditor() {
        mod.saveSettings();
        mc.displayGuiScreen(parent);
    }

    private boolean isInsideExpandedBounds(BoxBounds bounds, int x, int y) {
        return x >= bounds.x - 4 && x <= bounds.x + bounds.width + 4
                && y >= bounds.y - 4 && y <= bounds.y + bounds.height + 4;
    }

    /**
     * Turns the corner drag into a size change. Pull the corner right or down to grow the box and
     * push it back to shrink; whichever axis the mouse travelled furthest decides, so diagonal
     * drags work too. The size is measured from the value the drag started on, so dragging out and
     * back returns the box to exactly where it began.
     */
    private void resizePanelFromMouse(int mouseX, int mouseY) {
        int dx = mouseX - resizeStartX;
        int dy = mouseY - resizeStartY;
        int travel = Math.abs(dx) >= Math.abs(dy) ? dx : dy;
        int target = resizedPercent(resizeStartPercent, travel);
        int current = getPanelSizePercent(draggingPanel);
        if (target == current) {
            return;
        }
        adjustPanelSize(draggingPanel, target - current);
        // Keep the box anchored by its top-left corner while it grows or shrinks.
        BoxBounds bounds = getPanelBounds(draggingPanel);
        movePanelTo(draggingPanel, bounds.x, bounds.y);
    }

    /**
     * The size a corner drag lands on: every full {@link #RESIZE_PIXELS_PER_STEP} pixels of travel is
     * a 1% step, clamped to the shared 1%-100% range. Kept static so the maths is unit-testable.
     */
    static int resizedPercent(int startPercent, int travelPixels) {
        // Truncating division gives a one-pixel dead zone, so hand shake cannot nudge the size.
        int steps = travelPixels / RESIZE_PIXELS_PER_STEP;
        return Math.min(SpiderDropAlert.MAX_HUD_SIZE_PERCENT,
                Math.max(SpiderDropAlert.MIN_HUD_SIZE_PERCENT, startPercent + steps));
    }

    private void adjustPanelSize(int panel, int delta) {
        switch (panel) {
            case PANEL_SLAYER:
            case PANEL_FISHING:
                mod.adjustTrackerSize(delta, width, height);
                break;
            case PANEL_CREATURE_HP:
                mod.adjustNearbyCreatureHpSize(delta, width, height);
                break;
            case PANEL_DEPLOYABLES:
                mod.adjustDeployablesTimerPanelSize(delta, width, height);
                break;
            default:
                throw new IllegalArgumentException("Unknown HUD panel: " + panel);
        }
    }

    private int getPanelSizePercent(int panel) {
        switch (panel) {
            case PANEL_SLAYER:
            case PANEL_FISHING:
                return mod.getTrackerSizePercent();
            case PANEL_CREATURE_HP:
                return mod.getNearbyCreatureHpSizePercent();
            case PANEL_DEPLOYABLES:
                return mod.getDeployablesTimerPanelSizePercent();
            default:
                throw new IllegalArgumentException("Unknown HUD panel: " + panel);
        }
    }

    private int getHudPanelType(int panel) {
        switch (panel) {
            case PANEL_SLAYER:
                return SpiderDropAlert.HUD_PANEL_SLAYER;
            case PANEL_FISHING:
                return SpiderDropAlert.HUD_PANEL_FISHING;
            case PANEL_CREATURE_HP:
                return SpiderDropAlert.HUD_PANEL_CREATURE_HP;
            case PANEL_DEPLOYABLES:
                return SpiderDropAlert.HUD_PANEL_DEPLOYABLES;
            default:
                throw new IllegalArgumentException("Unknown HUD panel: " + panel);
        }
    }

    private int getPanelX(int panel) {
        switch (panel) {
            case PANEL_SLAYER:
                return mod.getTrackerX();
            case PANEL_FISHING:
                return mod.getFishingX();
            case PANEL_CREATURE_HP:
                return mod.getNearbyCreatureHpX();
            case PANEL_DEPLOYABLES:
                return mod.getDeployablesTimerPanelX();
            default:
                throw new IllegalArgumentException("Unknown HUD panel: " + panel);
        }
    }

    private int getPanelY(int panel) {
        switch (panel) {
            case PANEL_SLAYER:
                return mod.getTrackerY();
            case PANEL_FISHING:
                return mod.getFishingY();
            case PANEL_CREATURE_HP:
                return mod.getNearbyCreatureHpY();
            case PANEL_DEPLOYABLES:
                return mod.getDeployablesTimerPanelY();
            default:
                throw new IllegalArgumentException("Unknown HUD panel: " + panel);
        }
    }

    private int getPanelWidth(int panel) {
        switch (panel) {
            case PANEL_SLAYER:
                return mod.getRenderedTrackerWidth(width);
            case PANEL_FISHING:
                return mod.getRenderedFishingTrackerWidth(width);
            case PANEL_CREATURE_HP:
                return mod.getRenderedNearbyCreatureHpPanelWidth(width);
            case PANEL_DEPLOYABLES:
                return mod.getRenderedDeployablesTimerPanelWidth(width);
            default:
                throw new IllegalArgumentException("Unknown HUD panel: " + panel);
        }
    }

    private int getPanelHeight(int panel) {
        switch (panel) {
            case PANEL_SLAYER:
                return mod.getRenderedTrackerHeight();
            case PANEL_FISHING:
                return mod.getRenderedFishingTrackerHeight();
            case PANEL_CREATURE_HP:
                return mod.getRenderedNearbyCreatureHpPanelHeight();
            case PANEL_DEPLOYABLES:
                return mod.getRenderedDeployablesTimerPanelHeight();
            default:
                throw new IllegalArgumentException("Unknown HUD panel: " + panel);
        }
    }

    private BoxBounds getPanelBounds(int panel) {
        return new BoxBounds(getPanelX(panel), getPanelY(panel),
                Math.max(1, getPanelWidth(panel)), Math.max(1, getPanelHeight(panel)));
    }

    private void movePanelTo(int panel, int x, int y) {
        BoxBounds bounds = getPanelBounds(panel);
        int safeX = SpiderDropAlert.clamp(x, 0, Math.max(0, width - bounds.width));
        int safeY = SpiderDropAlert.clamp(y, 0, Math.max(0, height - bounds.height));
        switch (panel) {
            case PANEL_SLAYER:
                mod.moveTrackerTo(safeX, safeY, width, height);
                break;
            case PANEL_FISHING:
                mod.moveFishingTrackerTo(safeX, safeY, width, height);
                break;
            case PANEL_CREATURE_HP:
                mod.moveNearbyCreatureHpPanelTo(safeX, safeY, width, height);
                break;
            case PANEL_DEPLOYABLES:
                mod.moveDeployablesTimerPanelTo(safeX, safeY, width, height);
                break;
            default:
                throw new IllegalArgumentException("Unknown HUD panel: " + panel);
        }
    }

    private static final class BoxBounds {
        final int x;
        final int y;
        final int width;
        final int height;

        BoxBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}

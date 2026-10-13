package dev.arena.spiderdropalert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/**
 * Settings window styled after the NotEnoughFakepixel layout: a collapsible category tree on the
 * left and feature cards with toggle switches on the right. Every option the mod had is reachable
 * from here; the tree groups them as Quality of Life (deployables, appearance, layout), Dungeons
 * (boss/dragon hitboxes and dragon areas), Fishing and Slayer.
 */
public final class TrackerConfigScreen extends GuiScreen {

    // ---------------------------------------------------------------- colours
    private static final int COLOR_WINDOW = 0xF01B1B1D;
    private static final int COLOR_WINDOW_BORDER = 0xFF8E8E96;
    private static final int COLOR_TITLE_STRIP = 0xFF2A2A2E;
    private static final int COLOR_TITLE_TEXT = 0xFFF2F2F4;
    private static final int COLOR_HEADING = 0xFFB18CF7;
    private static final int COLOR_ROW_PARENT = 0xFFC3C3C8;
    private static final int COLOR_ROW_CHILD = 0xFF9A9AA2;
    private static final int COLOR_ROW_SELECTED = 0xFF35D3D3;
    private static final int COLOR_LIST_BORDER = 0xFF6E6E76;
    private static final int COLOR_LIST_BG = 0xFF141416;
    private static final int COLOR_CARD_BG = 0xFF232326;
    private static final int COLOR_CARD_BORDER = 0xFF4A4A50;
    private static final int COLOR_CARD_TITLE = 0xFFE8E8EB;
    private static final int COLOR_CARD_DESC = 0xFFB4B4BB;
    private static final int COLOR_BUTTON_BG = 0xFF3A3A40;
    private static final int COLOR_BUTTON_BG_HOVER = 0xFF4A4A52;
    private static final int COLOR_BUTTON_BORDER = 0xFF63636C;
    private static final int COLOR_BUTTON_TEXT = 0xFFEDEDF0;
    private static final int COLOR_SWITCH_TRACK = 0xFF2C2C30;
    private static final int COLOR_SWITCH_BORDER = 0xFF5E5E66;
    private static final int COLOR_SWITCH_ON = 0xFF35C435;
    private static final int COLOR_SWITCH_KNOB = 0xFFE9F6E9;
    private static final int COLOR_SWITCH_KNOB_OFF = 0xFF8E8E94;
    private static final int COLOR_SLIDER_TRACK = 0xFF3A3A40;
    private static final int COLOR_SLIDER_FILL = 0xFF58D5C8;
    private static final int COLOR_SLIDER_KNOB = 0xFFEDEDF0;
    private static final int COLOR_NOTES = 0xFF9A9AA2;

    // ---------------------------------------------------------------- pages
    private static final int PAGE_ABOUT = 0;
    private static final int PAGE_DEPLOYABLES = 1;
    private static final int PAGE_APPEARANCE = 2;
    private static final int PAGE_LAYOUT = 3;
    private static final int PAGE_BOSS = 4;
    private static final int PAGE_DRAGONS = 5;
    private static final int PAGE_AREAS = 6;
    private static final int PAGE_FISHING_MOB = 7;
    private static final int PAGE_FISHING_NOTIF = 8;
    private static final int PAGE_FISHING_HP = 9;
    private static final int PAGE_SLAYER = 10;
    private static final int PAGE_SLAYER_HISTORY = 11;
    private static final int PAGE_QUICK_NOTE = 12;

    // ---------------------------------------------------------------- control ids
    private static final int TOGGLE_TRACKER = 100;
    private static final int TOGGLE_DEPLOYABLES_PANEL = 101;
    private static final int TOGGLE_TOTEM = 102;
    private static final int TOGGLE_SOS = 103;
    private static final int TOGGLE_ORBS = 104;
    private static final int TOGGLE_MOB_COUNTER = 110;
    private static final int TOGGLE_EFFICIENCY = 111;
    private static final int TOGGLE_FESTIVAL = 112;
    private static final int TOGGLE_LEGENDARY = 113;
    private static final int TOGGLE_TROPHY = 114;
    private static final int TOGGLE_RARE_HIGHLIGHT = 115;
    private static final int TOGGLE_CREATURE_ALERT = 116;
    private static final int TOGGLE_JAWBUS = 117;
    private static final int TOGGLE_CREATURE_HP = 118;
    private static final int TOGGLE_M7_OVERLAY = 120;
    private static final int TOGGLE_M7_MAXOR = 121;
    private static final int TOGGLE_M7_STORM = 122;
    private static final int TOGGLE_M7_GOLDOR = 123;
    private static final int TOGGLE_M7_NECRON = 124;
    private static final int TOGGLE_M7_APEX = 130;
    private static final int TOGGLE_M7_FLAME = 131;
    private static final int TOGGLE_M7_ICE = 132;
    private static final int TOGGLE_M7_POWER = 133;
    private static final int TOGGLE_M7_SOUL = 134;
    private static final int TOGGLE_M7_AREAS = 140;
    private static final int TOGGLE_M7_PERSIST = 141;
    private static final int TOGGLE_M7_KILL_ALERT = 142;

    private static final int BTN_DEPLOY_APPLY = 203;
    private static final int BTN_DEPLOY_POS_RESET = 204;
    private static final int BTN_HP_APPLY = 213;
    private static final int BTN_HP_POS_RESET = 214;
    private static final int BTN_FISH_APPLY = 220;
    private static final int BTN_FISH_POS_RESET = 221;
    private static final int BTN_FISH_CLEAR = 222;
    private static final int BTN_TRACKER_APPLY = 233;
    private static final int BTN_TRACKER_POS_RESET = 234;
    private static final int BTN_OPEN_LAYOUT = 240;
    private static final int BTN_OPEN_QUICK_NOTE = 241;
    private static final int BTN_CLEAR_QUICK_NOTE = 242;
    private static final int BTN_CLEAR_SPIDER = 250;
    private static final int BTN_CLEAR_REVENANT = 251;
    private static final int BTN_CLEAR_BLAZE = 252;
    private static final int BTN_CLEAR_WOLF = 253;
    private static final int BTN_CLEAR_ENDERMAN = 254;
    private static final int BTN_CONFIRM_YES = 260;
    private static final int BTN_CONFIRM_CANCEL = 261;
    private static final int SLIDER_BASE = 300;
    private static final int OPACITY_SLIDER_COUNT = 4;

    private static final int CARD_TOGGLES = 0;
    private static final int CARD_BUTTONS = 1;
    private static final int CARD_SLIDER = 2;
    private static final int CARD_FIELDS = 3;
    private static final int CARD_NOTES = 4;

    private static final int POS_NONE = -1;
    private static final int POS_TRACKER = 0;
    private static final int POS_FISHING = 1;
    private static final int POS_DEPLOYABLES = 2;
    private static final int POS_CREATURE_HP = 3;

    // ---------------------------------------------------------------- rows / cards
    private static final class Row {
        final String label;
        final int page;
        final int depth;
        final int group;
        final boolean parent;

        Row(String label, int page, int depth, int group, boolean parent) {
            this.label = label;
            this.page = page;
            this.depth = depth;
            this.group = group;
            this.parent = parent;
        }
    }

    private static final class Card {
        final String title;
        final String description;
        final int kind;
        final int[] controls;

        Card(String title, String description, int kind, int[] controls) {
            this.title = title;
            this.description = description;
            this.kind = kind;
            this.controls = controls;
        }
    }

    private static final class Hit {
        final int id;
        final int x;
        final int y;
        final int width;
        final int height;

        Hit(int id, int x, int y, int width, int height) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    private final SpiderDropAlert mod;
    private final GuiScreen parent;
    private int selectedPage = PAGE_DEPLOYABLES;
    private final boolean[] expanded = new boolean[] {true, true, true, true};
    private final List<Row> rows = new ArrayList<Row>();
    private final List<Card> cards = new ArrayList<Card>();
    private final List<Hit> hits = new ArrayList<Hit>();
    private final List<int[]> rowBounds = new ArrayList<int[]>();
    private final int[] sliderBounds = new int[OPACITY_SLIDER_COUNT * 4];
    private GuiTextField xField;
    private GuiTextField yField;
    private int draggingSlider = -1;
    private int draggingScrollbar = 0;
    private int contentScroll;
    private int confirmAction = -1;
    private String confirmMessage = "";

    TrackerConfigScreen(SpiderDropAlert mod, GuiScreen parent) {
        this.mod = mod;
        this.parent = parent;
    }

    // ---------------------------------------------------------------- setup
    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        xField = new GuiTextField(0, fontRendererObj, 0, 0, 40, 14);
        yField = new GuiTextField(1, fontRendererObj, 0, 0, 40, 14);
        xField.setMaxStringLength(5);
        yField.setMaxStringLength(5);
        rebuildRows();
        rebuildCards();
        refreshPositionFields();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        mod.saveSettings();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void addGroup(String label, int group, String[] children, int[] pages) {
        rows.add(new Row(label, -1, 0, group, true));
        if (!expanded[group]) {
            return;
        }
        for (int i = 0; i < children.length; i++) {
            rows.add(new Row(children[i], pages[i], 1, group, false));
        }
    }

    private void rebuildRows() {
        rows.clear();
        rows.add(new Row("About", PAGE_ABOUT, 0, -1, false));
        addGroup("Quality of Life", 0,
                new String[] {"Deployables", "Quick Note", "Appearance", "Layout"},
                new int[] {PAGE_DEPLOYABLES, PAGE_QUICK_NOTE, PAGE_APPEARANCE, PAGE_LAYOUT});
        addGroup("Dungeons", 1, new String[] {"Boss Hitboxes", "Dragon Hitboxes", "Dragon Areas"},
                new int[] {PAGE_BOSS, PAGE_DRAGONS, PAGE_AREAS});
        addGroup("Fishing", 2, new String[] {"Mob Counter", "Catch Notifications", "Sea Creature HP"},
                new int[] {PAGE_FISHING_MOB, PAGE_FISHING_NOTIF, PAGE_FISHING_HP});
        addGroup("Slayer", 3, new String[] {"Tracker", "History"},
                new int[] {PAGE_SLAYER, PAGE_SLAYER_HISTORY});
    }

    private void rebuildCards() {
        cards.clear();
        if (selectedPage == PAGE_ABOUT) {
            cards.add(new Card("Commands", null, CARD_NOTES, new int[0]));
            cards.add(new Card("Version", "RNG Carried Bald " + SpiderDropAlert.VERSION
                    + " for Minecraft 1.8.9 Forge.", CARD_NOTES, new int[0]));
        } else if (selectedPage == PAGE_DEPLOYABLES) {
            cards.add(new Card("Deployables timer",
                    "Countdowns for Totem of Corruption, SOS Flare and the four Wolf Slayer "
                            + "power orbs.",
                    CARD_TOGGLES, new int[] {TOGGLE_DEPLOYABLES_PANEL}));
            cards.add(new Card("Totem of Corruption", "Countdown while a totem is active.",
                    CARD_TOGGLES, new int[] {TOGGLE_TOTEM}));
            cards.add(new Card("SOS Flare", "Countdown after a flare is placed.",
                    CARD_TOGGLES, new int[] {TOGGLE_SOS}));
            cards.add(new Card("Power orbs",
                    "Radiant, Mana Flux, Overflux and Plasmaflux orb timers.",
                    CARD_TOGGLES, new int[] {TOGGLE_ORBS}));
            cards.add(new Card("Panel position", "Exact screen X and Y, or Reset.",
                    CARD_FIELDS, new int[0]));
        } else if (selectedPage == PAGE_QUICK_NOTE) {
            cards.add(new Card("Quick Note",
                    "Pages of notes you type in game. Open the pad with /qn or the button below; "
                            + "every page saves itself as you write.",
                    CARD_BUTTONS, new int[] {BTN_OPEN_QUICK_NOTE}));
            cards.add(new Card("Stored notes", null, CARD_NOTES, new int[0]));
            cards.add(new Card("Clear all pages",
                    "Empties every page and leaves one blank page behind.",
                    CARD_BUTTONS, new int[] {BTN_CLEAR_QUICK_NOTE}));
        } else if (selectedPage == PAGE_APPEARANCE) {
            cards.add(new Card("Background opacity",
                    "1% - 100% for each panel. Transparent panels read as text over the world.",
                    CARD_SLIDER, new int[] {SLIDER_BASE, SLIDER_BASE + 1, SLIDER_BASE + 2,
                            SLIDER_BASE + 3}));
        } else if (selectedPage == PAGE_LAYOUT) {
            cards.add(new Card("Full-screen editor",
                    "Arrange all four HUD boxes on one screen. Drag a box to move it, drag its "
                            + "corner handle to change its size.",
                    CARD_BUTTONS, new int[] {BTN_OPEN_LAYOUT}));
        } else if (selectedPage == PAGE_BOSS) {
            cards.add(new Card("M7 overlay",
                    "Master switch for every Floor VII boss and dragon outline.",
                    CARD_TOGGLES, new int[] {TOGGLE_M7_OVERLAY}));
            cards.add(new Card("Boss hitboxes", "Maxor, Storm, Goldor and Necron.",
                    CARD_TOGGLES, new int[] {TOGGLE_M7_MAXOR, TOGGLE_M7_STORM, TOGGLE_M7_GOLDOR,
                            TOGGLE_M7_NECRON}));
        } else if (selectedPage == PAGE_DRAGONS) {
            cards.add(new Card("Dragon hitboxes",
                    "The five M7 dragon statues: Apex, Flame, Ice, Power and Soul.",
                    CARD_TOGGLES, new int[] {TOGGLE_M7_APEX, TOGGLE_M7_FLAME, TOGGLE_M7_ICE,
                            TOGGLE_M7_POWER, TOGGLE_M7_SOUL}));
        } else if (selectedPage == PAGE_AREAS) {
            cards.add(new Card("Dragon areas",
                    "Relic-area outlines during the M7 final phase.",
                    CARD_TOGGLES, new int[] {TOGGLE_M7_AREAS}));
            cards.add(new Card("Kill alert",
                    "Shows KILL <DRAGON> when a dragon is within 20 blocks of you and 15 of its "
                            + "own statue.",
                    CARD_TOGGLES, new int[] {TOGGLE_M7_KILL_ALERT}));
            cards.add(new Card("Keep areas after dragon spawn",
                    "ON keeps the areas visible after the first final-phase dragon spawns.",
                    CARD_TOGGLES, new int[] {TOGGLE_M7_PERSIST}));
            cards.add(new Card("Status", null, CARD_NOTES, new int[0]));
        } else if (selectedPage == PAGE_FISHING_MOB) {
            cards.add(new Card("Mob counter",
                    "Catches, session efficiency and the Great White Festival personal best.",
                    CARD_TOGGLES, new int[] {TOGGLE_MOB_COUNTER}));
            cards.add(new Card("Catch efficiency", "Catches per hour for the session.",
                    CARD_TOGGLES, new int[] {TOGGLE_EFFICIENCY}));
            cards.add(new Card("Festival personal best", "Best Great White Shark catch.",
                    CARD_TOGGLES, new int[] {TOGGLE_FESTIVAL}));
            cards.add(new Card("Panel position", "Exact screen X and Y, or Reset.",
                    CARD_FIELDS, new int[0]));
            cards.add(new Card("History", "Resets the fishing counter and session statistics.",
                    CARD_BUTTONS, new int[] {BTN_FISH_CLEAR}));
        } else if (selectedPage == PAGE_FISHING_NOTIF) {
            cards.add(new Card("Legendary sea creature notifier",
                    "Fakepixel CATCH! lines only.", CARD_TOGGLES, new int[] {TOGGLE_LEGENDARY}));
            cards.add(new Card("Trophy fish notifier", "Fakepixel trophy catches only.",
                    CARD_TOGGLES, new int[] {TOGGLE_TROPHY}));
            cards.add(new Card("Rare creature highlight", "Outlines tracked rare creatures.",
                    CARD_TOGGLES, new int[] {TOGGLE_RARE_HIGHLIGHT}));
            cards.add(new Card("Rare creature alert",
                    "Plays a vanilla sound when a tracked creature appears.",
                    CARD_TOGGLES, new int[] {TOGGLE_CREATURE_ALERT}));
            cards.add(new Card("Lord Jawbus alert", "Local title when you anger Lord Jawbus.",
                    CARD_TOGGLES, new int[] {TOGGLE_JAWBUS}));
        } else if (selectedPage == PAGE_FISHING_HP) {
            cards.add(new Card("Sea creature HP",
                    "Shows the health of nearby sea creatures from their nameplates.",
                    CARD_TOGGLES, new int[] {TOGGLE_CREATURE_HP}));
            cards.add(new Card("Panel position", "Exact screen X and Y, or Reset.",
                    CARD_FIELDS, new int[0]));
        } else if (selectedPage == PAGE_SLAYER) {
            cards.add(new Card("Slayer tracker",
                    "Drop history with priced profit for the active Slayer.",
                    CARD_TOGGLES, new int[] {TOGGLE_TRACKER}));
            cards.add(new Card("Panel position", "Exact screen X and Y, or Reset.",
                    CARD_FIELDS, new int[0]));
        } else if (selectedPage == PAGE_SLAYER_HISTORY) {
            cards.add(new Card("Clear Slayer history",
                    "Pick a Slayer to reset its drops and total.",
                    CARD_BUTTONS, new int[] {BTN_CLEAR_SPIDER, BTN_CLEAR_REVENANT,
                            BTN_CLEAR_BLAZE, BTN_CLEAR_WOLF, BTN_CLEAR_ENDERMAN}));
        }
    }

    private int positionKindForPage(int page) {
        if (page == PAGE_DEPLOYABLES) {
            return POS_DEPLOYABLES;
        }
        if (page == PAGE_FISHING_MOB) {
            return POS_FISHING;
        }
        if (page == PAGE_FISHING_HP) {
            return POS_CREATURE_HP;
        }
        if (page == PAGE_SLAYER) {
            return POS_TRACKER;
        }
        return POS_NONE;
    }

    private void refreshPositionFields() {
        if (xField == null) {
            return;
        }
        int kind = positionKindForPage(selectedPage);
        if (kind == POS_TRACKER) {
            xField.setText(Integer.toString(mod.getTrackerX()));
            yField.setText(Integer.toString(mod.getTrackerY()));
        } else if (kind == POS_FISHING) {
            xField.setText(Integer.toString(mod.getFishingX()));
            yField.setText(Integer.toString(mod.getFishingY()));
        } else if (kind == POS_DEPLOYABLES) {
            xField.setText(Integer.toString(mod.getDeployablesTimerPanelX()));
            yField.setText(Integer.toString(mod.getDeployablesTimerPanelY()));
        } else if (kind == POS_CREATURE_HP) {
            xField.setText(Integer.toString(mod.getNearbyCreatureHpX()));
            yField.setText(Integer.toString(mod.getNearbyCreatureHpY()));
        }
    }

    private void selectPage(int page) {
        if (page == selectedPage) {
            return;
        }
        selectedPage = page;
        contentScroll = 0;
        rebuildCards();
        refreshPositionFields();
    }

    // ---------------------------------------------------------------- geometry
    private int windowWidth() {
        return Math.min(500, width - 16);
    }

    private int windowHeight() {
        return Math.min(266, height - 16);
    }

    private int windowX() {
        return (width - windowWidth()) / 2;
    }

    private int windowY() {
        return (height - windowHeight()) / 2;
    }

    private int contentX() {
        return windowX() + windowWidth() - 10 - contentWidth();
    }

    private int contentWidth() {
        return windowWidth() - 20 - sidebarWidth() - 12;
    }

    private int sidebarWidth() {
        return Math.max(120, Math.min(150, windowWidth() / 3));
    }

    private int sidebarX() {
        return windowX() + 10;
    }

    private int sidebarTop() {
        return windowY() + 34;
    }

    private int sidebarBottom() {
        return windowY() + windowHeight() - 10;
    }

    private int contentTop() {
        return windowY() + 34;
    }

    private int contentBottom() {
        return windowY() + windowHeight() - 10;
    }

    private int rowHeight() {
        return 12;
    }

    // ---------------------------------------------------------------- drawing
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        hits.clear();
        rowBounds.clear();
        for (int i = 0; i < sliderBounds.length; i++) {
            sliderBounds[i] = 0;
        }

        int x = windowX();
        int y = windowY();
        int w = windowWidth();
        int h = windowHeight();
        Gui.drawRect(x, y, x + w, y + h, COLOR_WINDOW);
        Gui.drawRect(x, y, x + w, y + 16, COLOR_TITLE_STRIP);
        drawCenteredString(fontRendererObj, SpiderDropAlert.MOD_NAME, x + w / 2, y + 4,
                COLOR_TITLE_TEXT);
        drawBorder(x, y, w, h, COLOR_WINDOW_BORDER);

        drawSidebar(mouseX, mouseY);
        drawContent(mouseX, mouseY);

        if (confirmAction >= 0) {
            drawConfirmDialog(mouseX, mouseY);
        }
    }

    private void drawSidebar(int mouseX, int mouseY) {
        int x = sidebarX();
        int top = sidebarTop();
        int bottom = sidebarBottom();
        int w = sidebarWidth();
        drawCenteredString(fontRendererObj, "Categories", x + w / 2, windowY() + 21, COLOR_HEADING);

        Gui.drawRect(x, top, x + w, bottom, COLOR_LIST_BG);
        drawBorder(x, top, w, bottom - top, COLOR_LIST_BORDER);

        int listTop = top + 3;
        int rowH = rowHeight();
        int scroll = sidebarScroll();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int rowY = listTop + i * rowH - scroll;
            rowBounds.add(new int[] {x + 1, rowY, w - 12, rowH});
            if (rowY + rowH < top || rowY > bottom) {
                continue;
            }
            boolean selected = row.page >= 0 && row.page == selectedPage;
            int color = selected ? COLOR_ROW_SELECTED : (row.parent ? COLOR_ROW_PARENT : COLOR_ROW_CHILD);
            int textX = x + 8 + row.depth * 10;
            if (row.parent) {
                String arrow = expanded[row.group] ? "v" : ">";
                fontRendererObj.drawStringWithShadow(arrow, textX - 8, rowY + 2, 0xFFEDEDF0);
            }
            String label = fontRendererObj.trimStringToWidth(row.label,
                    w - (textX - x) - 10);
            fontRendererObj.drawStringWithShadow(label, textX, rowY + 2, color);
            if (selected) {
                Gui.drawRect(textX, rowY + 11, textX + fontRendererObj.getStringWidth(label),
                        rowY + 12, COLOR_ROW_SELECTED);
            }
        }
        drawScrollbar(x + w - 9, top + 3, 8, bottom - top - 6, sidebarScroll(), rows.size() * rowH,
                bottom - top - 6);
    }

    private int sidebarScroll() {
        // The tree is sized to fit the list box, so it only clips on very small windows.
        int visible = sidebarBottom() - sidebarTop() - 6;
        return Math.max(0, rows.size() * rowHeight() - visible);
    }

    private void drawContent(int mouseX, int mouseY) {
        String heading = pageHeading(selectedPage);
        String subtitle = pageSubtitle(selectedPage);
        fontRendererObj.drawStringWithShadow(heading, contentX(), windowY() + 21, COLOR_CARD_TITLE);
        if (subtitle != null) {
            fontRendererObj.drawStringWithShadow(subtitle, contentX(), windowY() + 32,
                    COLOR_CARD_DESC);
        }

        int top = contentTop() + (subtitle != null ? 10 : 0);
        int bottom = contentBottom();
        int x = contentX();
        int w = contentWidth();

        int scroll = clampScroll(mouseX, mouseY);
        int y = top + 4 - scroll;
        int scaleFactor = scaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scaleFactor, (this.height - bottom) * scaleFactor, w * scaleFactor,
                (bottom - top) * scaleFactor);
        for (Card card : cards) {
            int cardHeight = measureCard(card, w);
            drawCard(card, x, y, w, cardHeight, mouseX, mouseY);
            y += cardHeight + 5;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        int total = 0;
        for (Card card : cards) {
            total += measureCard(card, w) + 5;
        }
        drawScrollbar(x + w - 8, top + 2, 6, bottom - top - 4, scroll, total, bottom - top - 4);
    }

    private int clampScroll(int mouseX, int mouseY) {
        int top = contentTop();
        int bottom = contentBottom();
        int w = contentWidth();
        int total = 0;
        for (Card card : cards) {
            total += measureCard(card, w) + 5;
        }
        int max = Math.max(0, total - (bottom - top - 4));
        contentScroll = Math.max(0, Math.min(max, contentScroll));
        return contentScroll;
    }

    private String pageHeading(int page) {
        switch (page) {
            case PAGE_ABOUT: return "About";
            case PAGE_DEPLOYABLES: return "Deployables";
            case PAGE_APPEARANCE: return "Appearance";
            case PAGE_LAYOUT: return "Layout";
            case PAGE_BOSS: return "Boss Hitboxes";
            case PAGE_DRAGONS: return "Dragon Hitboxes";
            case PAGE_AREAS: return "Dragon Areas";
            case PAGE_FISHING_MOB: return "Mob Counter";
            case PAGE_FISHING_NOTIF: return "Catch Notifications";
            case PAGE_FISHING_HP: return "Sea Creature HP";
            case PAGE_SLAYER: return "Tracker";
            case PAGE_SLAYER_HISTORY: return "History";
            case PAGE_QUICK_NOTE: return "Quick Note";
            default: return "Settings";
        }
    }

    private String pageSubtitle(int page) {
        switch (page) {
            case PAGE_ABOUT: return "Client-side Forge 1.8.9 mod.";
            case PAGE_DEPLOYABLES:
            case PAGE_APPEARANCE:
            case PAGE_LAYOUT:
            case PAGE_QUICK_NOTE: return "Quality of Life settings.";
            case PAGE_BOSS:
            case PAGE_DRAGONS:
            case PAGE_AREAS: return "Dungeons settings.";
            case PAGE_FISHING_MOB:
            case PAGE_FISHING_NOTIF:
            case PAGE_FISHING_HP: return "Fishing settings.";
            case PAGE_SLAYER:
            case PAGE_SLAYER_HISTORY: return "Slayer settings.";
            default: return null;
        }
    }

    private int measureCard(Card card, int cardWidth) {
        int titleLines = card.title == null ? 0
                : Math.max(1, fontRendererObj.listFormattedStringToWidth(card.title, titleColumnWidth(cardWidth)).size());
        int descLines = card.description == null ? 0
                : fontRendererObj.listFormattedStringToWidth(card.description, descColumnWidth(cardWidth)).size();
        int height = 6 + Math.max(titleLines, descLines) * 10;
        if (card.kind == CARD_TOGGLES) {
            height += card.controls.length * 13;
        } else if (card.kind == CARD_BUTTONS) {
            height += 16 + 2;
        } else if (card.kind == CARD_SLIDER) {
            height += card.controls.length * 13;
        } else if (card.kind == CARD_FIELDS) {
            height += 18;
        } else if (card.kind == CARD_NOTES) {
            height += notesFor(selectedPage, card.title).size() * 10 + 2;
        }
        return height + 5;
    }

    private int titleColumnWidth(int cardWidth) {
        return Math.max(60, cardWidth / 3);
    }

    private int descColumnWidth(int cardWidth) {
        return Math.max(60, cardWidth - titleColumnWidth(cardWidth) - 14);
    }

    private void drawCard(Card card, int x, int y, int w, int h, int mouseX, int mouseY) {
        Gui.drawRect(x, y, x + w, y + h, COLOR_CARD_BG);
        drawBorder(x, y, w, h, COLOR_CARD_BORDER);
        int titleWidth = titleColumnWidth(w);
        int descX = x + titleWidth + 4;
        int textY = y + 6;
        if (card.title != null) {
            fontRendererObj.drawSplitString(card.title, x + 5, textY, titleWidth - 6,
                    COLOR_CARD_TITLE);
        }
        if (card.description != null) {
            int descLines = fontRendererObj.listFormattedStringToWidth(card.description,
                    descColumnWidth(w)).size();
            fontRendererObj.drawSplitString(card.description, descX, textY,
                    descColumnWidth(w), COLOR_CARD_DESC);
            textY += descLines * 10;
        } else {
            textY += 10;
        }
        int controlY = textY + 1;
        if (card.kind == CARD_TOGGLES) {
            if (card.controls.length == 1) {
                // The card title already names the feature, so the switch sits under it on its own.
                drawSwitch(card.controls[0], x + 5, controlY, mouseX, mouseY);
            } else {
                for (int i = 0; i < card.controls.length; i++) {
                    int id = card.controls[i];
                    int rowY = controlY + i * 13;
                    fontRendererObj.drawStringWithShadow(labelFor(id), x + 5, rowY + 3,
                            COLOR_CARD_TITLE);
                    drawSwitch(x + w - 46, rowY + 1, mouseX, mouseY, id);
                }
            }
        } else if (card.kind == CARD_BUTTONS) {
            int buttonY = controlY + 2;
            int buttonX = x + 5;
            for (int i = 0; i < card.controls.length; i++) {
                int id = card.controls[i];
                String label = buttonLabel(id);
                int buttonWidth = fontRendererObj.getStringWidth(label) + 10;
                if (i > 0) {
                    buttonWidth += 2;
                }
                drawButton(id, buttonX, buttonY, Math.min(buttonWidth, x + w - 5 - buttonX), 14,
                        label, mouseX, mouseY);
                buttonX += Math.min(buttonWidth, x + w - 5 - buttonX) + 3;
            }
        } else if (card.kind == CARD_SLIDER) {
            int sliderValueY = y + 6 + fontRendererObj.listFormattedStringToWidth(
                    card.description == null ? "" : card.description, descColumnWidth(w)).size() * 10;
            for (int i = 0; i < card.controls.length; i++) {
                int panel = card.controls[i] - SLIDER_BASE;
                int rowY = sliderValueY + i * 13;
                int value = mod.getHudPanelBackgroundOpacity(panel);
                fontRendererObj.drawStringWithShadow(opacityLabel(panel) + "  " + value + "%",
                        x + 5, rowY + 3, COLOR_CARD_TITLE);
                int trackX = x + Math.max(110, w / 2);
                int trackWidth = x + w - 8 - trackX;
                drawSlider(panel, trackX, rowY + 5, trackWidth);
            }
        } else if (card.kind == CARD_FIELDS) {
            int fieldY = controlY + 2;
            int fieldWidth = 38;
            xField.xPosition = x + 15;
            xField.yPosition = fieldY;
            xField.width = fieldWidth;
            xField.height = 14;
            yField.xPosition = x + 15 + fieldWidth + 12;
            yField.yPosition = fieldY;
            yField.width = fieldWidth;
            yField.height = 14;
            fontRendererObj.drawStringWithShadow("X", x + 5, fieldY + 4, COLOR_NOTES);
            fontRendererObj.drawStringWithShadow("Y", x + 15 + fieldWidth + 2, fieldY + 4,
                    COLOR_NOTES);
            xField.drawTextBox();
            yField.drawTextBox();
            int buttonX = yField.xPosition + fieldWidth + 8;
            drawButton(applyButtonForPage(selectedPage), buttonX, fieldY + 1, 34, 13, "Apply",
                    mouseX, mouseY);
            buttonX += 37;
            drawButton(resetButtonForPage(selectedPage), buttonX, fieldY + 1, 34, 13, "Reset",
                    mouseX, mouseY);
        } else if (card.kind == CARD_NOTES) {
            int noteY = controlY + 1;
            List<String> lines = notesFor(selectedPage, card.title);
            for (String line : lines) {
                fontRendererObj.drawStringWithShadow(fontRendererObj.trimStringToWidth(line, w - 12),
                        x + 5, noteY, COLOR_NOTES);
                noteY += 10;
            }
            hits.add(new Hit(-1, x, y, w, Math.max(h, noteY - y + 3)));
        } else {
            hits.add(new Hit(-1, x, y, w, h));
        }
    }

    private List<String> notesFor(int page, String title) {
        List<String> lines = new ArrayList<String>();
        if (page == PAGE_ABOUT) {
            if ("Commands".equals(title)) {
                lines.add("/tg or /trackergui - open this window");
                lines.add("/qn or /quicknote - open the quick note pad");
                lines.add("/priceset <price> or /setprice <price> - price the held item");
                lines.add("/priceset <item name> <price> - price a named item");
                lines.add("All HUDs read local client data only; missing fields show N/A.");
            } else {
                lines.add("Forge 11.15.1.2318 on Minecraft 1.8.9.");
            }
        } else if (page == PAGE_QUICK_NOTE) {
            lines.add("Pages: " + mod.getQuickNotePageCount()
                    + "  (up to " + QuickNoteStore.MAX_PAGES + ")");
            int listed = Math.min(4, mod.getQuickNoteStore().getPageCount());
            for (int i = 0; i < listed; i++) {
                String heading = mod.getQuickNoteStore().getHeading(i);
                lines.add((i + 1) + ". " + (heading.isEmpty() ? "(untitled)" : heading));
            }
            if (mod.getQuickNoteStore().getPageCount() > listed) {
                lines.add("...");
            }
            lines.add("Click Heading in the pad to name a page; it saves with the note.");
        } else if (page == PAGE_AREAS) {
            lines.add("Floor VII detected: "
                    + (mod.isFloorSevenScoreboardDetected() ? "YES" : "not right now"));
            lines.add("Areas follow the NotEnoughFakepixel relic positions.");
        }
        return lines;
    }

    private void drawSwitch(int id, int x, int y, int mouseX, int mouseY) {
        int w = 38;
        int h = 11;
        boolean on = isOn(id);
        Gui.drawRect(x, y, x + w, y + h, COLOR_SWITCH_TRACK);
        drawBorder(x, y, w, h, COLOR_SWITCH_BORDER);
        int knobSize = h - 4;
        int knobX = on ? x + w - knobSize - 2 : x + 2;
        if (on) {
            Gui.drawRect(x + 1, y + 1, knobX, y + h - 1, COLOR_SWITCH_ON);
        }
        Gui.drawRect(knobX, y + 2, knobX + knobSize, y + 2 + knobSize,
                on ? COLOR_SWITCH_KNOB : COLOR_SWITCH_KNOB_OFF);
        hits.add(new Hit(id, x, y, w, h));
    }

    private void drawButton(int id, int x, int y, int w, int h, String label, int mouseX,
                            int mouseY) {
        if (id < 0 || w <= 0) {
            return;
        }
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        Gui.drawRect(x, y, x + w, y + h, hovered ? COLOR_BUTTON_BG_HOVER : COLOR_BUTTON_BG);
        drawBorder(x, y, w, h, COLOR_BUTTON_BORDER);
        fontRendererObj.drawStringWithShadow(fontRendererObj.trimStringToWidth(label, w - 6),
                x + 4, y + (h - 8) / 2, COLOR_BUTTON_TEXT);
        hits.add(new Hit(id, x, y, w, h));
    }

    private void drawSlider(int panel, int trackX, int trackY, int trackWidth) {
        int opacity = mod.getHudPanelBackgroundOpacity(panel);
        Gui.drawRect(trackX, trackY, trackX + trackWidth, trackY + 4, COLOR_SLIDER_TRACK);
        int filled = Math.round(trackWidth * opacity / 100.0F);
        if (filled > 0) {
            Gui.drawRect(trackX, trackY, trackX + filled, trackY + 4, COLOR_SLIDER_FILL);
        }
        Gui.drawRect(trackX + filled - 1, trackY - 2, trackX + filled + 2, trackY + 6,
                COLOR_SLIDER_KNOB);
        int index = panel * 4;
        sliderBounds[index] = trackX;
        sliderBounds[index + 1] = trackY - 3;
        sliderBounds[index + 2] = trackWidth;
        sliderBounds[index + 3] = 10;
    }

    private void drawScrollbar(int x, int y, int w, int height, int scroll, int total,
                               int visible) {
        Gui.drawRect(x, y, x + w, y + height, 0xFF232326);
        if (total <= visible || total <= 0) {
            return;
        }
        int thumbHeight = Math.max(12, height * visible / total);
        int thumbY = y + (height - thumbHeight) * scroll / Math.max(1, total - visible);
        Gui.drawRect(x + 1, thumbY, x + w - 1, thumbY + thumbHeight, 0xFF6E6E76);
    }

    private void drawBorder(int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        Gui.drawRect(x, y, x + w, y + 1, color);
        Gui.drawRect(x, y + h - 1, x + w, y + h, color);
        Gui.drawRect(x, y, x + 1, y + h, color);
        Gui.drawRect(x + w - 1, y, x + w, y + h, color);
    }

    private void drawConfirmDialog(int mouseX, int mouseY) {
        Gui.drawRect(0, 0, width, height, 0x90000000);
        int boxWidth = 220;
        int boxHeight = 70;
        int x = (width - boxWidth) / 2;
        int y = (height - boxHeight) / 2;
        Gui.drawRect(x, y, x + boxWidth, y + boxHeight, COLOR_WINDOW);
        drawBorder(x, y, boxWidth, boxHeight, COLOR_WINDOW_BORDER);
        fontRendererObj.drawSplitString(confirmMessage, x + 8, y + 10, boxWidth - 16,
                COLOR_CARD_TEXT_SUPPORT);
        int buttonY = y + boxHeight - 24;
        drawButton(BTN_CONFIRM_YES, x + 14, buttonY, 88, 16, "Yes, clear", mouseX, mouseY);
        drawButton(BTN_CONFIRM_CANCEL, x + boxWidth - 102, buttonY, 88, 16, "Cancel", mouseX,
                mouseY);
    }

    private static final int COLOR_CARD_TEXT_SUPPORT = 0xFFE8E8EB;

    // ---------------------------------------------------------------- labels / state
    private String labelFor(int id) {
        switch (id) {
            case TOGGLE_TRACKER: return "Slayer tracker";
            case TOGGLE_DEPLOYABLES_PANEL: return "Deployables timer";
            case TOGGLE_TOTEM: return "Totem of Corruption";
            case TOGGLE_SOS: return "SOS Flare";
            case TOGGLE_ORBS: return "Power orbs";
            case TOGGLE_MOB_COUNTER: return "Mob counter";
            case TOGGLE_EFFICIENCY: return "Catch efficiency";
            case TOGGLE_FESTIVAL: return "Festival personal best";
            case TOGGLE_LEGENDARY: return "Legendary notifier";
            case TOGGLE_TROPHY: return "Trophy fish notifier";
            case TOGGLE_RARE_HIGHLIGHT: return "Rare creature highlight";
            case TOGGLE_CREATURE_ALERT: return "Rare creature alert";
            case TOGGLE_JAWBUS: return "Lord Jawbus alert";
            case TOGGLE_CREATURE_HP: return "Sea creature HP";
            case TOGGLE_M7_OVERLAY: return "M7 overlay";
            case TOGGLE_M7_MAXOR: return "Maxor";
            case TOGGLE_M7_STORM: return "Storm";
            case TOGGLE_M7_GOLDOR: return "Goldor";
            case TOGGLE_M7_NECRON: return "Necron";
            case TOGGLE_M7_APEX: return "Apex dragon";
            case TOGGLE_M7_FLAME: return "Flame dragon";
            case TOGGLE_M7_ICE: return "Ice dragon";
            case TOGGLE_M7_POWER: return "Power dragon";
            case TOGGLE_M7_SOUL: return "Soul dragon";
            case TOGGLE_M7_AREAS: return "Dragon areas";
            case TOGGLE_M7_PERSIST: return "Keep areas after spawn";
            case TOGGLE_M7_KILL_ALERT: return "Kill alert";
            default: return "";
        }
    }

    private String opacityLabel(int panel) {
        switch (panel) {
            case SpiderDropAlert.HUD_PANEL_SLAYER: return "Slayer tracker";
            case SpiderDropAlert.HUD_PANEL_FISHING: return "Fishing counter";
            case SpiderDropAlert.HUD_PANEL_CREATURE_HP: return "Sea Creature HP";
            case SpiderDropAlert.HUD_PANEL_DEPLOYABLES: return "Deployables";
            default: return "Panel";
        }
    }

    private String buttonLabel(int id) {
        switch (id) {
            case BTN_DEPLOY_APPLY:
            case BTN_HP_APPLY:
            case BTN_FISH_APPLY:
            case BTN_TRACKER_APPLY: return "Apply";
            case BTN_DEPLOY_POS_RESET:
            case BTN_HP_POS_RESET:
            case BTN_FISH_POS_RESET:
            case BTN_TRACKER_POS_RESET: return "Reset position";
            case BTN_OPEN_LAYOUT: return "Open editor";
            case BTN_OPEN_QUICK_NOTE: return "Open notes";
            case BTN_CLEAR_QUICK_NOTE: return "Clear all pages";
            case BTN_FISH_CLEAR: return "Clear fishing history";
            case BTN_CLEAR_SPIDER: return "Spider";
            case BTN_CLEAR_REVENANT: return "Revenant";
            case BTN_CLEAR_BLAZE: return "Blaze";
            case BTN_CLEAR_WOLF: return "Wolf";
            case BTN_CLEAR_ENDERMAN: return "Enderman";
            default: return "";
        }
    }

    private int applyButtonForPage(int page) {
        int kind = positionKindForPage(page);
        if (kind == POS_TRACKER) {
            return BTN_TRACKER_APPLY;
        }
        if (kind == POS_FISHING) {
            return BTN_FISH_APPLY;
        }
        if (kind == POS_DEPLOYABLES) {
            return BTN_DEPLOY_APPLY;
        }
        if (kind == POS_CREATURE_HP) {
            return BTN_HP_APPLY;
        }
        return -1;
    }

    private int resetButtonForPage(int page) {
        int kind = positionKindForPage(page);
        if (kind == POS_TRACKER) {
            return BTN_TRACKER_POS_RESET;
        }
        if (kind == POS_FISHING) {
            return BTN_FISH_POS_RESET;
        }
        if (kind == POS_DEPLOYABLES) {
            return BTN_DEPLOY_POS_RESET;
        }
        if (kind == POS_CREATURE_HP) {
            return BTN_HP_POS_RESET;
        }
        return -1;
    }

    private boolean isOn(int id) {
        switch (id) {
            case TOGGLE_TRACKER: return mod.isTrackerEnabled();
            case TOGGLE_DEPLOYABLES_PANEL: return mod.isDeployablesTimerPanelEnabled();
            case TOGGLE_TOTEM: return mod.isDeployableGroupEnabled(DeployableGroup.TOTEM_OF_CORRUPTION);
            case TOGGLE_SOS: return mod.isDeployableGroupEnabled(DeployableGroup.SOS_FLARE);
            case TOGGLE_ORBS: return mod.isDeployableGroupEnabled(DeployableGroup.POWER_ORBS);
            case TOGGLE_MOB_COUNTER: return mod.isFishingMobCounterEnabled();
            case TOGGLE_EFFICIENCY: return mod.isFishingEfficiencyTrackerEnabled();
            case TOGGLE_FESTIVAL: return mod.isFishingFestivalTrackerEnabled();
            case TOGGLE_LEGENDARY: return mod.isFishingLegendaryNotifierEnabled();
            case TOGGLE_TROPHY: return mod.isFishingTrophyFishNotifierEnabled();
            case TOGGLE_RARE_HIGHLIGHT: return mod.isFishingRareCreatureHighlightEnabled();
            case TOGGLE_CREATURE_ALERT: return mod.isFishingCreatureAlertEnabled();
            case TOGGLE_JAWBUS: return mod.isJawbusAlertEnabled();
            case TOGGLE_CREATURE_HP: return mod.isNearbyCreatureHpEnabled();
            case TOGGLE_M7_OVERLAY: return mod.isM7OverlayEnabled();
            case TOGGLE_M7_MAXOR: return mod.isM7BossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget.MAXOR);
            case TOGGLE_M7_STORM: return mod.isM7BossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget.STORM);
            case TOGGLE_M7_GOLDOR: return mod.isM7BossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget.GOLDOR);
            case TOGGLE_M7_NECRON: return mod.isM7BossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget.NECRON);
            case TOGGLE_M7_APEX: return mod.isM7DragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.APEX);
            case TOGGLE_M7_FLAME: return mod.isM7DragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.FLAME);
            case TOGGLE_M7_ICE: return mod.isM7DragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.ICE);
            case TOGGLE_M7_POWER: return mod.isM7DragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.POWER);
            case TOGGLE_M7_SOUL: return mod.isM7DragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.SOUL);
            case TOGGLE_M7_AREAS: return mod.isM7StatueZonesEnabled();
            case TOGGLE_M7_PERSIST: return mod.isM7AreasPersistAfterDragonSpawn();
            case TOGGLE_M7_KILL_ALERT: return mod.isM7DragonKillAlertEnabled();
            default: return false;
        }
    }

    private void flip(int id) {
        switch (id) {
            case TOGGLE_TRACKER: mod.toggleTracker(); break;
            case TOGGLE_DEPLOYABLES_PANEL: mod.toggleDeployablesTimerPanel(); break;
            case TOGGLE_TOTEM: mod.toggleDeployableGroup(DeployableGroup.TOTEM_OF_CORRUPTION); break;
            case TOGGLE_SOS: mod.toggleDeployableGroup(DeployableGroup.SOS_FLARE); break;
            case TOGGLE_ORBS: mod.toggleDeployableGroup(DeployableGroup.POWER_ORBS); break;
            case TOGGLE_MOB_COUNTER: mod.toggleFishingMobCounter(); break;
            case TOGGLE_EFFICIENCY: mod.toggleFishingEfficiencyTracker(); break;
            case TOGGLE_FESTIVAL: mod.toggleFishingFestivalTracker(); break;
            case TOGGLE_LEGENDARY: mod.toggleFishingLegendaryNotifier(); break;
            case TOGGLE_TROPHY: mod.toggleFishingTrophyFishNotifier(); break;
            case TOGGLE_RARE_HIGHLIGHT: mod.toggleFishingRareCreatureHighlight(); break;
            case TOGGLE_CREATURE_ALERT: mod.toggleFishingCreatureAlert(); break;
            case TOGGLE_JAWBUS: mod.toggleJawbusAlert(); break;
            case TOGGLE_CREATURE_HP: mod.toggleNearbyCreatureHp(); break;
            case TOGGLE_M7_OVERLAY: mod.toggleM7Overlay(); break;
            case TOGGLE_M7_MAXOR: mod.toggleM7BossHitbox(M7OverlayRenderer.M7HitboxTarget.MAXOR); break;
            case TOGGLE_M7_STORM: mod.toggleM7BossHitbox(M7OverlayRenderer.M7HitboxTarget.STORM); break;
            case TOGGLE_M7_GOLDOR: mod.toggleM7BossHitbox(M7OverlayRenderer.M7HitboxTarget.GOLDOR); break;
            case TOGGLE_M7_NECRON: mod.toggleM7BossHitbox(M7OverlayRenderer.M7HitboxTarget.NECRON); break;
            case TOGGLE_M7_APEX: mod.toggleM7DragonHitbox(M7OverlayRenderer.M7DragonStatue.APEX); break;
            case TOGGLE_M7_FLAME: mod.toggleM7DragonHitbox(M7OverlayRenderer.M7DragonStatue.FLAME); break;
            case TOGGLE_M7_ICE: mod.toggleM7DragonHitbox(M7OverlayRenderer.M7DragonStatue.ICE); break;
            case TOGGLE_M7_POWER: mod.toggleM7DragonHitbox(M7OverlayRenderer.M7DragonStatue.POWER); break;
            case TOGGLE_M7_SOUL: mod.toggleM7DragonHitbox(M7OverlayRenderer.M7DragonStatue.SOUL); break;
            case TOGGLE_M7_AREAS: mod.toggleM7StatueZones(); break;
            case TOGGLE_M7_PERSIST: mod.toggleM7AreasPersistAfterDragonSpawn(); break;
            case TOGGLE_M7_KILL_ALERT: mod.toggleM7DragonKillAlert(); break;
            default: break;
        }
        mod.saveSettings();
    }

    private int scaleFactor() {
        int factor = new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor();
        return Math.max(1, factor);
    }

    // ---------------------------------------------------------------- input
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (confirmAction >= 0) {
            for (Hit hit : hits) {
                if (!hit.contains(mouseX, mouseY)) {
                    continue;
                }
                if (hit.id == BTN_CONFIRM_YES) {
                    runAction(confirmAction);
                    confirmAction = -1;
                    return;
                }
                if (hit.id == BTN_CONFIRM_CANCEL) {
                    confirmAction = -1;
                    return;
                }
            }
            return;
        }
        if (mouseButton == 0) {
            for (int i = 0; i < rowBounds.size() && i < rows.size(); i++) {
                int[] bounds = rowBounds.get(i);
                if (mouseX >= bounds[0] && mouseX <= bounds[0] + bounds[2]
                        && mouseY >= bounds[1] && mouseY <= bounds[1] + bounds[3]) {
                    Row row = rows.get(i);
                    if (row.parent) {
                        expanded[row.group] = !expanded[row.group];
                        rebuildRows();
                    } else {
                        selectPage(row.page);
                    }
                    return;
                }
            }
            for (int panel = 0; panel < OPACITY_SLIDER_COUNT; panel++) {
                int index = panel * 4;
                if (sliderBounds[index + 2] > 0
                        && mouseX >= sliderBounds[index] - 2
                        && mouseX <= sliderBounds[index] + sliderBounds[index + 2] + 2
                        && mouseY >= sliderBounds[index + 1]
                        && mouseY <= sliderBounds[index + 1] + sliderBounds[index + 3]) {
                    draggingSlider = panel;
                    updateSliderFromMouse(panel, mouseX);
                    return;
                }
            }
            boolean fieldsVisible = positionKindForPage(selectedPage) != POS_NONE;
            if (fieldsVisible && isInside(xField, mouseX, mouseY)) {
                xField.setFocused(true);
                yField.setFocused(false);
                xField.mouseClicked(mouseX, mouseY, mouseButton);
                return;
            }
            if (fieldsVisible && isInside(yField, mouseX, mouseY)) {
                yField.setFocused(true);
                xField.setFocused(false);
                yField.mouseClicked(mouseX, mouseY, mouseButton);
                return;
            }
            if (xField != null) {
                xField.setFocused(false);
            }
            if (yField != null) {
                yField.setFocused(false);
            }
            for (Hit hit : hits) {
                if (hit.id >= 0 && hit.contains(mouseX, mouseY)) {
                    if (hit.id >= SLIDER_BASE && hit.id < SLIDER_BASE + OPACITY_SLIDER_COUNT) {
                        draggingSlider = hit.id - SLIDER_BASE;
                        updateSliderFromMouse(draggingSlider, mouseX);
                        return;
                    }
                    if (hit.id >= TOGGLE_TRACKER && hit.id < SLIDER_BASE) {
                        if (isToggleId(hit.id)) {
                            flip(hit.id);
                        } else {
                            handleButton(hit.id);
                        }
                    }
                    return;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean isToggleId(int id) {
        return id >= TOGGLE_TRACKER && id <= TOGGLE_M7_PERSIST;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggingSlider = -1;
        draggingScrollbar = 0;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int mouseX = Mouse.getEventX() * width / mc.displayWidth;
            int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
            if (mouseX >= sidebarX() && mouseX <= sidebarX() + sidebarWidth()) {
                // The sidebar fits its rows; nothing to scroll there for now.
            }
            if (mouseX >= contentX() && mouseX <= contentX() + contentWidth()) {
                contentScroll -= wheel / 120 * 12;
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton,
                                  long timeSinceLastClick) {
        if (draggingSlider >= 0) {
            updateSliderFromMouse(draggingSlider, mouseX);
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    private void updateSliderFromMouse(int panel, int mouseX) {
        int index = panel * 4;
        int trackX = sliderBounds[index];
        int trackWidth = sliderBounds[index + 2];
        if (trackWidth <= 0) {
            return;
        }
        float ratio = (mouseX - trackX) / (float) trackWidth;
        int opacity = Math.round(Math.max(0F, Math.min(1F, ratio)) * 100F);
        mod.setHudPanelBackgroundOpacity(panel, opacity);
        mod.saveSettings();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (confirmAction >= 0 && keyCode == Keyboard.KEY_ESCAPE) {
            confirmAction = -1;
            return;
        }
        boolean fieldsVisible = positionKindForPage(selectedPage) != POS_NONE;
        if (xField != null && fieldsVisible) {
            xField.textboxKeyTyped(typedChar, keyCode);
        }
        if (yField != null && fieldsVisible) {
            yField.textboxKeyTyped(typedChar, keyCode);
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        boolean fieldsVisible = positionKindForPage(selectedPage) != POS_NONE;
        if (xField != null) {
            xField.setVisible(fieldsVisible);
            xField.updateCursorCounter();
        }
        if (yField != null) {
            yField.setVisible(fieldsVisible);
            yField.updateCursorCounter();
        }
    }

    // ---------------------------------------------------------------- actions
    private void handleButton(int id) {
        switch (id) {
            case BTN_DEPLOY_APPLY:
            case BTN_HP_APPLY:
            case BTN_FISH_APPLY:
            case BTN_TRACKER_APPLY:
                applyPosition(); break;
            case BTN_DEPLOY_POS_RESET:
                mod.resetDeployablesTimerPanelPosition(width, height); break;
            case BTN_HP_POS_RESET:
                mod.resetNearbyCreatureHpPosition(width, height); break;
            case BTN_FISH_POS_RESET:
                mod.resetFishingPosition(width, height); break;
            case BTN_TRACKER_POS_RESET:
                mod.resetTrackerPosition(); break;
            case BTN_OPEN_LAYOUT:
                mc.displayGuiScreen(new HudLayoutEditorScreen(mod, this)); break;
            case BTN_OPEN_QUICK_NOTE:
                mod.requestQuickNote(); break;
            case BTN_CLEAR_QUICK_NOTE:
                askConfirm(BTN_CLEAR_QUICK_NOTE, "Clear every quick note page?"); break;
            case BTN_FISH_CLEAR:
                askConfirm(BTN_FISH_CLEAR, "Clear the fishing history and session stats?");
                break;
            case BTN_CLEAR_SPIDER:
                askConfirm(BTN_CLEAR_SPIDER, "Clear the Spider Slayer history?"); break;
            case BTN_CLEAR_REVENANT:
                askConfirm(BTN_CLEAR_REVENANT, "Clear the Revenant Slayer history?"); break;
            case BTN_CLEAR_BLAZE:
                askConfirm(BTN_CLEAR_BLAZE, "Clear the Blaze Slayer history?"); break;
            case BTN_CLEAR_WOLF:
                askConfirm(BTN_CLEAR_WOLF, "Clear the Wolf Slayer history?"); break;
            case BTN_CLEAR_ENDERMAN:
                askConfirm(BTN_CLEAR_ENDERMAN, "Clear the Enderman Slayer history?"); break;
            default:
                break;
        }
        mod.saveSettings();
    }

    private void askConfirm(int action, String message) {
        confirmAction = action;
        confirmMessage = message;
    }

    private void runAction(int action) {
        switch (action) {
            case BTN_FISH_CLEAR: mod.clearFishingHistory(); break;
            case BTN_CLEAR_QUICK_NOTE: mod.resetQuickNotes(); break;
            case BTN_CLEAR_SPIDER: mod.clearHistory(SlayerType.SPIDER); break;
            case BTN_CLEAR_REVENANT: mod.clearHistory(SlayerType.REVENANT); break;
            case BTN_CLEAR_BLAZE: mod.clearHistory(SlayerType.BLAZE); break;
            case BTN_CLEAR_WOLF: mod.clearHistory(SlayerType.WOLF); break;
            case BTN_CLEAR_ENDERMAN: mod.clearHistory(SlayerType.ENDERMAN); break;
            default: break;
        }
        mod.saveSettings();
    }

    private void applyPosition() {
        int x = parseInt(xField.getText());
        int y = parseInt(yField.getText());
        if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE) {
            return;
        }
        int kind = positionKindForPage(selectedPage);
        if (kind == POS_TRACKER) {
            mod.moveTrackerTo(x, y, width, height);
        } else if (kind == POS_FISHING) {
            mod.moveFishingTrackerTo(x, y, width, height);
        } else if (kind == POS_DEPLOYABLES) {
            mod.moveDeployablesTimerPanelTo(x, y, width, height);
        } else if (kind == POS_CREATURE_HP) {
            mod.moveNearbyCreatureHpPanelTo(x, y, width, height);
        }
        refreshPositionFields();
        mod.saveSettings();
    }

    private static boolean isInside(GuiTextField field, int mouseX, int mouseY) {
        return field != null && mouseX >= field.xPosition && mouseX <= field.xPosition + field.width
                && mouseY >= field.yPosition && mouseY <= field.yPosition + field.height;
    }

    private static int parseInt(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (RuntimeException ex) {
            return Integer.MIN_VALUE;
        }
    }
}

package dev.arena.spiderdropalert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * The /qn quick note pad: pages of text typed in game. Every edit marks the page dirty and a tick
 * timer writes the file a moment later, so notes save themselves while you type and on close.
 */
final class QuickNoteScreen extends GuiScreen {
    private static final int BUTTON_PREV = 0;
    private static final int BUTTON_NEXT = 1;
    private static final int BUTTON_NEW_PAGE = 2;
    private static final int BUTTON_DELETE_PAGE = 3;
    private static final int BUTTON_DONE = 4;

    private static final int COLOR_PANEL = 0xF01B1B1D;
    private static final int COLOR_BORDER = 0xFF8E8E96;
    private static final int COLOR_BOX = 0xFF141416;
    private static final int COLOR_BOX_BORDER = 0xFF6E6E76;
    private static final int COLOR_TITLE = 0xFFF2F2F4;
    private static final int COLOR_TEXT = 0xFFE8E8EB;
    private static final int COLOR_HINT = 0xFF9A9AA2;
    private static final int COLOR_SAVED = 0xFF8FD68F;
    private static final int COLOR_PENDING = 0xFFE8C46A;
    private static final int COLOR_CURSOR = 0xFFFFFFFF;
    private static final int COLOR_SIDEBAR = 0xFF141416;
    private static final int COLOR_SIDEBAR_BORDER = 0xFF6E6E76;
    private static final int COLOR_ROW = 0xFF9A9AA2;
    private static final int COLOR_ROW_SELECTED = 0xFF35D3D3;
    private static final int COLOR_ROW_COUNT = 0xFF7F7F88;
    private static final int SIDEBAR_WIDTH = 132;
    private static final long AUTOSAVE_DELAY_MS = 700L;

    private final SpiderDropAlert mod;
    private final QuickNoteStore store;
    private final GuiScreen parent;
    private GuiTextField headingField;
    private GuiTextField searchField;
    private final List<Integer> searchHits = new ArrayList<Integer>();
    private int searchScroll;
    private String lastQuery = null;
    private int page;
    private int cursor;
    private int scrollLine;
    private boolean dirty;
    private long lastEditAt;
    private String wrappedSource;
    private int wrappedWidth;
    private final List<int[]> lines = new ArrayList<int[]>();

    QuickNoteScreen(SpiderDropAlert mod, QuickNoteStore store, GuiScreen parent) {
        this.mod = mod;
        this.store = store;
        this.parent = parent;
        // Open on the last page with the cursor at the end, ready to keep writing.
        this.page = Math.max(0, store.getPageCount() - 1);
        this.cursor = store.getPage(this.page).length();
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int buttonWidth = 62;
        int gap = 4;
        int buttonY = panelY() + panelHeight() - 24;
        int x = panelX() + 10;
        buttonList.add(new GuiButton(BUTTON_PREV, x, buttonY, buttonWidth, 18, "< Prev"));
        buttonList.add(new GuiButton(BUTTON_NEXT, x + buttonWidth + gap, buttonY, buttonWidth, 18,
                "Next >"));
        buttonList.add(new GuiButton(BUTTON_DELETE_PAGE, x + (buttonWidth + gap) * 2, buttonY,
                buttonWidth, 18, "Delete"));
        int doneX = panelX() + panelWidth() - 10 - buttonWidth;
        buttonList.add(new GuiButton(BUTTON_DONE, doneX, buttonY, buttonWidth, 18, "Done"));
        // The new page button sits in the header, where the old page counter used to be.
        int newPageWidth = 78;
        buttonList.add(new GuiButton(BUTTON_NEW_PAGE, panelX() + panelWidth() - 12 - newPageWidth,
                panelY() + 5, newPageWidth, 16, "+ New page"));
        headingField = new GuiTextField(0, fontRendererObj, headingFieldX(), headingLabelY(),
                headingFieldWidth(), 14);
        headingField.setMaxStringLength(QuickNoteStore.MAX_HEADING_LENGTH);
        headingField.setText(store.getHeading(page));
        searchField = new GuiTextField(1, fontRendererObj, sidebarX() + 4, panelY() + 25,
                SIDEBAR_WIDTH - 8, 14);
        searchField.setMaxStringLength(40);
        searchField.setText(lastQuery == null ? "" : lastQuery);
        refreshSearch();
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (buttonList == null || buttonList.isEmpty()) {
            return;
        }
        for (Object object : buttonList) {
            GuiButton button = (GuiButton) object;
            if (button.id == BUTTON_PREV) {
                button.enabled = page > 0;
            } else if (button.id == BUTTON_NEXT) {
                button.enabled = page < store.getPageCount() - 1;
            } else if (button.id == BUTTON_NEW_PAGE) {
                button.enabled = store.getPageCount() < QuickNoteStore.MAX_PAGES;
            } else if (button.id == BUTTON_DELETE_PAGE) {
                button.enabled = !store.getPage(page).isEmpty() || store.getPageCount() > 1;
            }
        }
    }

    // ---------------------------------------------------------------- geometry
    private int panelWidth() {
        return Math.min(568, width - 20);
    }

    private int sidebarX() {
        return panelX() + 10;
    }

    private int sidebarY() {
        return panelY() + 42;
    }

    private int sidebarBottom() {
        // Stops above the status line and button row.
        return panelY() + panelHeight() - 44;
    }

    private int contentLeft() {
        return sidebarX() + SIDEBAR_WIDTH + 10;
    }

    private int panelHeight() {
        return Math.min(252, height - 40);
    }

    private int panelX() {
        return (width - panelWidth()) / 2;
    }

    private int panelY() {
        return (height - panelHeight()) / 2;
    }

    private int boxX() {
        return contentLeft();
    }

    private int boxWidth() {
        return panelX() + panelWidth() - 12 - boxX();
    }

    private int headingLabelY() {
        return panelY() + 26;
    }

    private int headingFieldX() {
        return contentLeft() + 58;
    }

    private int headingFieldWidth() {
        return panelX() + panelWidth() - 12 - headingFieldX();
    }

    private int boxY() {
        return panelY() + 46;
    }

    private int boxHeight() {
        return panelHeight() - 90;
    }

    private int lineHeight() {
        return fontRendererObj.FONT_HEIGHT + 2;
    }

    private int visibleLines() {
        return Math.max(1, (boxHeight() - 6) / lineHeight());
    }

    private String pageText() {
        return store.getPage(page);
    }

    // ---------------------------------------------------------------- drawing
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        Gui.drawRect(panelX(), panelY(), panelX() + panelWidth(), panelY() + panelHeight(),
                COLOR_PANEL);
        drawBorder(panelX(), panelY(), panelWidth(), panelHeight(), COLOR_BORDER);
        fontRendererObj.drawStringWithShadow("Quick Note", panelX() + 12, panelY() + 10,
                COLOR_TITLE);
        drawSidebar();

        // The label sits in the note column, right beside the field that shows "(optional)"; the
        // left column holds the search box alone.
        fontRendererObj.drawStringWithShadow("Heading", contentLeft(), headingLabelY() + 3,
                COLOR_HINT);
        if (headingField != null) {
            headingField.xPosition = headingFieldX();
            headingField.yPosition = headingLabelY();
            headingField.width = headingFieldWidth();
            headingField.drawTextBox();
            if (!headingField.isFocused() && headingField.getText().isEmpty()) {
                fontRendererObj.drawStringWithShadow("(optional)",
                        headingFieldX() + 5, headingLabelY() + 3, COLOR_HINT);
            }
        }

        Gui.drawRect(boxX(), boxY(), boxX() + boxWidth(), boxY() + boxHeight(), COLOR_BOX);
        drawBorder(boxX(), boxY(), boxWidth(), boxHeight(), COLOR_BOX_BORDER);

        String text = pageText();
        rebuildLines(text);
        int visible = visibleLines();
        int maxScroll = Math.max(0, lines.size() - visible);
        scrollLine = Math.max(0, Math.min(maxScroll, scrollLine));
        for (int i = 0; i < visible; i++) {
            int lineIndex = scrollLine + i;
            if (lineIndex >= lines.size()) {
                break;
            }
            int[] line = lines.get(lineIndex);
            String label = text.substring(line[0], line[1]);
            fontRendererObj.drawStringWithShadow(label, boxX() + 4,
                    boxY() + 4 + i * lineHeight(), COLOR_TEXT);
        }
        drawCursor(text);

        String hint = dirty ? "saving..." : "saved automatically";
        fontRendererObj.drawStringWithShadow(hint, panelX() + 12,
                panelY() + panelHeight() - 37, dirty ? COLOR_PENDING : COLOR_SAVED);
        String keys = "Enter new line  -  search on the left  -  Esc closes";
        fontRendererObj.drawStringWithShadow(keys,
                panelX() + panelWidth() - 12 - fontRendererObj.getStringWidth(keys),
                panelY() + panelHeight() - 37, COLOR_HINT);

        super.drawScreen(mouseX, mouseY, partialTicks);
        updateButtonStates();
    }

    /** The page list on the left, with the search field that filters it. */
    private void drawSidebar() {
        Gui.drawRect(sidebarX(), sidebarY(), sidebarX() + SIDEBAR_WIDTH, sidebarBottom(),
                COLOR_SIDEBAR);
        drawBorder(sidebarX(), sidebarY(), SIDEBAR_WIDTH, sidebarBottom() - sidebarY(),
                COLOR_SIDEBAR_BORDER);
        if (searchField != null) {
            searchField.xPosition = sidebarX() + 4;
            searchField.yPosition = panelY() + 25;
            searchField.width = SIDEBAR_WIDTH - 8;
            searchField.drawTextBox();
            if (!searchField.isFocused() && searchField.getText().isEmpty()) {
                fontRendererObj.drawStringWithShadow("Search notes",
                        sidebarX() + 9, panelY() + 29, COLOR_ROW_COUNT);
            }
        }

        int rowHeight = lineHeight();
        int visible = Math.max(1, (sidebarBottom() - sidebarY() - 16) / rowHeight);
        int maxScroll = Math.max(0, searchHits.size() - visible);
        searchScroll = Math.max(0, Math.min(maxScroll, searchScroll));
        for (int i = 0; i < visible; i++) {
            int slot = searchScroll + i;
            if (slot >= searchHits.size()) {
                break;
            }
            int pageIndex = searchHits.get(slot);
            int rowY = sidebarY() + 3 + i * rowHeight;
            boolean selected = pageIndex == page;
            String label = (pageIndex + 1) + ". " + store.getPreview(pageIndex);
            String clipped = fontRendererObj.trimStringToWidth(label, SIDEBAR_WIDTH - 12);
            if (selected) {
                Gui.drawRect(sidebarX() + 1, rowY - 1, sidebarX() + SIDEBAR_WIDTH - 1,
                        rowY + rowHeight - 1, 0x5535D3D3);
            }
            fontRendererObj.drawStringWithShadow(clipped, sidebarX() + 5, rowY,
                    selected ? COLOR_ROW_SELECTED : COLOR_ROW);
        }

        String count = searchField != null && !searchField.getText().trim().isEmpty()
                ? searchHits.size() + " of " + store.getPageCount() + " pages"
                : store.getPageCount() + " pages";
        fontRendererObj.drawStringWithShadow(fontRendererObj.trimStringToWidth(count,
                SIDEBAR_WIDTH - 10), sidebarX() + 5, sidebarBottom() - 10, COLOR_ROW_COUNT);
    }

    private void refreshSearch() {
        String query = searchField == null ? "" : searchField.getText();
        if (query.equals(lastQuery)) {
            return;
        }
        lastQuery = query;
        searchHits.clear();
        searchHits.addAll(store.findPages(query));
        searchScroll = 0;
    }

    private void jumpToPage(int pageIndex) {
        saveNow();
        page = Math.max(0, Math.min(store.getPageCount() - 1, pageIndex));
        cursor = pageText().length();
        scrollLine = 0;
        wrappedSource = null;
        if (headingField != null) {
            headingField.setText(store.getHeading(page));
            headingField.setFocused(false);
        }
        ensureCursorVisible();
        updateButtonStates();
    }

    private void drawCursor(String text) {
        if ((System.currentTimeMillis() / 500L) % 2L != 0L) {
            return;
        }
        int lineIndex = cursorLine(text);
        if (lineIndex < scrollLine || lineIndex >= scrollLine + visibleLines()) {
            return;
        }
        int[] line = lines.get(lineIndex);
        int offset = fontRendererObj.getStringWidth(text.substring(line[0],
                Math.max(line[0], Math.min(cursor, line[1]))));
        int cursorX = boxX() + 4 + offset;
        int cursorY = boxY() + 4 + (lineIndex - scrollLine) * lineHeight();
        Gui.drawRect(cursorX, cursorY - 1, cursorX + 1, cursorY + fontRendererObj.FONT_HEIGHT,
                COLOR_CURSOR);
    }

    private void drawBorder(int x, int y, int w, int h, int color) {
        Gui.drawRect(x, y, x + w, y + 1, color);
        Gui.drawRect(x, y + h - 1, x + w, y + h, color);
        Gui.drawRect(x, y, x + 1, y + h, color);
        Gui.drawRect(x + w - 1, y, x + w, y + h, color);
    }

    /** Wraps the page into display lines, each an inclusive-start / exclusive-end index pair. */
    private void rebuildLines(String text) {
        if (text.equals(wrappedSource) && boxWidth() == wrappedWidth && !lines.isEmpty()) {
            return;
        }
        wrappedSource = text;
        wrappedWidth = boxWidth();
        lines.clear();
        int maxWidth = Math.max(20, boxWidth() - 8);
        StringBuilder current = new StringBuilder();
        int lineStart = 0;
        int lastSpace = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                lines.add(new int[] {lineStart, i});
                current.setLength(0);
                lineStart = i + 1;
                lastSpace = -1;
                continue;
            }
            current.append(c);
            if (c == ' ') {
                lastSpace = i;
            }
            if (current.length() > 1
                    && fontRendererObj.getStringWidth(current.toString()) > maxWidth) {
                if (lastSpace > lineStart) {
                    lines.add(new int[] {lineStart, lastSpace});
                    lineStart = lastSpace + 1;
                } else {
                    lines.add(new int[] {lineStart, i});
                    lineStart = i;
                }
                current.setLength(0);
                lastSpace = -1;
                i = lineStart - 1;
            }
        }
        lines.add(new int[] {lineStart, text.length()});
    }

    private int cursorLine(String text) {
        for (int i = 0; i < lines.size(); i++) {
            if (cursor <= lines.get(i)[1]) {
                return i;
            }
        }
        return Math.max(0, lines.size() - 1);
    }

    private void ensureCursorVisible() {
        int line = cursorLine(pageText());
        if (line < scrollLine) {
            scrollLine = line;
        } else if (line >= scrollLine + visibleLines()) {
            scrollLine = line - visibleLines() + 1;
        }
    }

    // ---------------------------------------------------------------- editing
    private void insert(String addition) {
        String text = pageText();
        if (text.length() + addition.length() > QuickNoteStore.MAX_PAGE_LENGTH) {
            return;
        }
        setText(text.substring(0, cursor) + addition + text.substring(cursor));
        cursor += addition.length();
    }

    private void setText(String text) {
        store.setPage(page, text);
        if (cursor > text.length()) {
            cursor = text.length();
        }
        wrappedSource = null;
        dirty = true;
        lastEditAt = System.currentTimeMillis();
        ensureCursorVisible();
    }

    private void saveNow() {
        syncHeading();
        store.save();
        dirty = false;
    }

    /** Copies the heading text into the store when it differs. */
    private void syncHeading() {
        if (headingField == null) {
            return;
        }
        if (!headingField.getText().equals(store.getHeading(page))) {
            store.setHeading(page, headingField.getText());
            dirty = true;
        }
    }

    private void markDirty() {
        dirty = true;
        lastEditAt = System.currentTimeMillis();
    }

    private void goToPage(int target) {
        saveNow();
        page = Math.max(0, Math.min(store.getPageCount() - 1, target));
        cursor = pageText().length();
        scrollLine = 0;
        wrappedSource = null;
        if (headingField != null) {
            headingField.setText(store.getHeading(page));
            headingField.setFocused(false);
        }
        ensureCursorVisible();
        updateButtonStates();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        String text = pageText();
        if (searchField != null && searchField.isFocused()) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                searchField.setFocused(false);
                return;
            }
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_TAB) {
                searchField.setFocused(false);
                return;
            }
            searchField.textboxKeyTyped(typedChar, keyCode);
            refreshSearch();
            return;
        }
        if (headingField != null && headingField.isFocused() && keyCode != Keyboard.KEY_ESCAPE) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_TAB) {
                // Enter or Tab drops out of the heading and into the note itself.
                store.setHeading(page, headingField.getText());
                markDirty();
                headingField.setFocused(false);
                return;
            }
            headingField.textboxKeyTyped(typedChar, keyCode);
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            saveNow();
            mc.displayGuiScreen(parent);
            return;
        }
        if (keyCode == Keyboard.KEY_BACK) {
            if (cursor > 0) {
                setText(text.substring(0, cursor - 1) + text.substring(cursor));
                cursor--;
                ensureCursorVisible();
            }
            return;
        }
        if (keyCode == Keyboard.KEY_DELETE) {
            if (cursor < text.length()) {
                setText(text.substring(0, cursor) + text.substring(cursor + 1));
            }
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN) {
            insert("\n");
            return;
        }
        if (keyCode == Keyboard.KEY_TAB) {
            insert("  ");
            return;
        }
        if (keyCode == Keyboard.KEY_LEFT) {
            if (cursor > 0) {
                cursor--;
                ensureCursorVisible();
            }
            return;
        }
        if (keyCode == Keyboard.KEY_RIGHT) {
            if (cursor < text.length()) {
                cursor++;
                ensureCursorVisible();
            }
            return;
        }
        if (keyCode == Keyboard.KEY_UP || keyCode == Keyboard.KEY_DOWN) {
            moveCursorVertically(keyCode == Keyboard.KEY_UP ? -1 : 1);
            return;
        }
        if (typedChar >= ' ' && typedChar != 127 && typedChar != 167) {
            insert(String.valueOf(typedChar));
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void moveCursorVertically(int delta) {
        String text = pageText();
        rebuildLines(text);
        int line = cursorLine(text);
        int target = Math.max(0, Math.min(lines.size() - 1, line + delta));
        if (target == line) {
            return;
        }
        int[] source = lines.get(line);
        int desired = fontRendererObj.getStringWidth(text.substring(source[0],
                Math.max(source[0], Math.min(cursor, source[1]))));
        int[] destination = lines.get(target);
        int best = destination[0];
        for (int i = destination[0]; i <= destination[1]; i++) {
            best = i;
            if (fontRendererObj.getStringWidth(text.substring(destination[0], i)) >= desired) {
                break;
            }
        }
        cursor = best;
        ensureCursorVisible();
    }

    // ---------------------------------------------------------------- mouse
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && searchField != null
                && mouseX >= searchField.xPosition
                && mouseX <= searchField.xPosition + searchField.width
                && mouseY >= searchField.yPosition
                && mouseY <= searchField.yPosition + searchField.height) {
            searchField.setFocused(true);
            if (headingField != null) {
                headingField.setFocused(false);
            }
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }
        if (searchField != null) {
            searchField.setFocused(false);
        }
        if (mouseButton == 0 && mouseX >= sidebarX() && mouseX <= sidebarX() + SIDEBAR_WIDTH
                && mouseY >= sidebarY() && mouseY <= sidebarBottom()) {
            int rowHeight = lineHeight();
            int slot = searchScroll + Math.max(0, (mouseY - sidebarY() - 3) / rowHeight);
            if (slot >= 0 && slot < searchHits.size()) {
                jumpToPage(searchHits.get(slot));
            }
            return;
        }
        if (mouseButton == 0 && headingField != null
                && mouseX >= headingField.xPosition
                && mouseX <= headingField.xPosition + headingField.width
                && mouseY >= headingField.yPosition
                && mouseY <= headingField.yPosition + headingField.height) {
            headingField.setFocused(true);
            headingField.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }
        if (headingField != null) {
            headingField.setFocused(false);
        }
        if (mouseButton == 0 && mouseX >= boxX() && mouseX <= boxX() + boxWidth()
                && mouseY >= boxY() && mouseY <= boxY() + boxHeight()) {
            String text = pageText();
            rebuildLines(text);
            int lineIndex = scrollLine + Math.max(0, (mouseY - boxY() - 4) / lineHeight());
            if (lineIndex < lines.size()) {
                int[] line = lines.get(lineIndex);
                int target = line[0];
                for (int i = line[0]; i <= line[1]; i++) {
                    target = i;
                    if (boxX() + 4 + fontRendererObj.getStringWidth(
                            text.substring(line[0], i)) >= mouseX) {
                        break;
                    }
                }
                cursor = Math.max(line[0], Math.min(line[1], target));
                ensureCursorVisible();
            }
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        if (mouseX >= sidebarX() && mouseX <= sidebarX() + SIDEBAR_WIDTH) {
            int visible = Math.max(1, (sidebarBottom() - sidebarY() - 16) / lineHeight());
            int maxListScroll = Math.max(0, searchHits.size() - visible);
            searchScroll = Math.max(0, Math.min(maxListScroll, searchScroll - wheel / 120 * 2));
            return;
        }
        int maxScroll = Math.max(0, lines.size() - visibleLines());
        scrollLine = Math.max(0, Math.min(maxScroll, scrollLine - wheel / 120 * 2));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BUTTON_PREV:
                goToPage(page - 1);
                break;
            case BUTTON_NEXT:
                goToPage(page + 1);
                break;
            case BUTTON_NEW_PAGE:
                saveNow();
                page = store.appendPage();
                lastQuery = null;
                cursor = 0;
                scrollLine = 0;
                wrappedSource = null;
                if (headingField != null) {
                    headingField.setText(store.getHeading(page));
                    headingField.setFocused(false);
                }
                markDirty();
                updateButtonStates();
                break;
            case BUTTON_DELETE_PAGE:
                saveNow();
                store.deletePage(page);
                page = Math.max(0, Math.min(store.getPageCount() - 1, page));
                cursor = pageText().length();
                scrollLine = 0;
                wrappedSource = null;
                if (headingField != null) {
                    headingField.setText(store.getHeading(page));
                    headingField.setFocused(false);
                }
                markDirty();
                updateButtonStates();
                break;
            case BUTTON_DONE:
                saveNow();
                mc.displayGuiScreen(parent);
                break;
            default:
                break;
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (searchField != null) {
            searchField.updateCursorCounter();
            refreshSearch();
        }
        if (headingField != null) {
            headingField.updateCursorCounter();
            if (headingField.isFocused()
                    && !headingField.getText().equals(store.getHeading(page))) {
                store.setHeading(page, headingField.getText());
                markDirty();
            }
        }
        if (dirty && System.currentTimeMillis() - lastEditAt >= AUTOSAVE_DELAY_MS) {
            saveNow();
        }
    }

    @Override
    public void onGuiClosed() {
        saveNow();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

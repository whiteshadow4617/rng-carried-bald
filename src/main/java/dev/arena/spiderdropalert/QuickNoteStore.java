package dev.arena.spiderdropalert;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The pages behind the /qn quick note. Stored as plain UTF-8 text in the mod's config folder, one
 * page per block of lines with a marker line between them, so the file stays readable and editable
 * outside the game.
 */
final class QuickNoteStore {
    /** A line holding exactly this text starts the next page. */
    static final String PAGE_MARKER = "%%PAGE%%";
    /** A line starting with this starts a page's heading, e.g. {@code %%TITLE%% Diana prices}. */
    static final String PAGE_TITLE_PREFIX = "%%TITLE%% ";
    static final int MAX_PAGES = 99;
    /** Keeps a page small enough that wrapping and redrawing stay instant while typing. */
    static final int MAX_PAGE_LENGTH = 2000;
    /** Headings are a single short line. */
    static final int MAX_HEADING_LENGTH = 48;

    /** One note page: an optional heading plus its body text. */
    static final class NotePage {
        String heading = "";
        String body = "";

        NotePage() {
        }

        NotePage(String heading, String body) {
            this.heading = heading == null ? "" : heading;
            this.body = body == null ? "" : body;
        }
    }

    private final File file;
    private final List<NotePage> pages = new ArrayList<NotePage>();

    QuickNoteStore(File file) {
        this.file = file;
        load();
    }

    File getFile() {
        return file;
    }

    List<NotePage> getPages() {
        return Collections.unmodifiableList(pages);
    }

    int getPageCount() {
        return pages.size();
    }

    String getPage(int index) {
        if (index < 0 || index >= pages.size()) {
            return "";
        }
        return pages.get(index).body;
    }

    /**
     * Page indexes whose heading or text contains the query, case-insensitively. An empty query
     * matches every page, so the same code path drives the default list and the search results.
     */
    List<Integer> findPages(String query) {
        List<Integer> hits = new ArrayList<Integer>();
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ENGLISH);
        for (int i = 0; i < pages.size(); i++) {
            NotePage page = pages.get(i);
            if (needle.isEmpty()
                    || page.heading.toLowerCase(Locale.ENGLISH).contains(needle)
                    || page.body.toLowerCase(Locale.ENGLISH).contains(needle)) {
                hits.add(i);
            }
        }
        return hits;
    }

    /** A one-line description of a page for the sidebar list. */
    String getPreview(int index) {
        if (index < 0 || index >= pages.size()) {
            return "";
        }
        NotePage page = pages.get(index);
        if (!page.heading.isEmpty()) {
            return page.heading;
        }
        for (String line : page.body.split("\n", -1)) {
            if (!line.trim().isEmpty()) {
                return line.trim();
            }
        }
        return "(empty)";
    }

    String getHeading(int index) {
        if (index < 0 || index >= pages.size()) {
            return "";
        }
        return pages.get(index).heading;
    }

    void setHeading(int index, String heading) {
        if (index < 0 || index >= pages.size()) {
            return;
        }
        pages.get(index).heading = limitHeading(heading);
    }

    /** A heading with line breaks folded to spaces, capped so the field stays a single line. */
    static String limitHeading(String heading) {
        if (heading == null) {
            return "";
        }
        String flattened = heading.replace('\n', ' ').replace('\r', ' ');
        return flattened.length() <= MAX_HEADING_LENGTH
                ? flattened : flattened.substring(0, MAX_HEADING_LENGTH);
    }

    void setPage(int index, String text) {
        if (index < 0 || index >= pages.size()) {
            return;
        }
        pages.get(index).body = limit(text);
    }

    /** Adds an empty page and returns its index, or the last page when the limit is reached. */
    int appendPage() {
        if (pages.size() >= MAX_PAGES) {
            return pages.size() - 1;
        }
        pages.add(new NotePage());
        return pages.size() - 1;
    }

    /** Removes a page. The only remaining page is cleared instead, so one always stays. */
    boolean deletePage(int index) {
        if (index < 0 || index >= pages.size()) {
            return false;
        }
        if (pages.size() <= 1) {
            pages.get(0).heading = "";
            pages.get(0).body = "";
            return true;
        }
        pages.remove(index);
        return true;
    }

    void resetAll() {
        pages.clear();
        pages.add(new NotePage());
    }

    static String limit(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= MAX_PAGE_LENGTH ? text : text.substring(0, MAX_PAGE_LENGTH);
    }

    void load() {
        pages.clear();
        if (file == null || !file.isFile()) {
            pages.add(new NotePage());
            return;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            NotePage page = new NotePage();
            String line;
            while ((line = reader.readLine()) != null) {
                if (PAGE_MARKER.equals(line)) {
                    pages.add(page);
                    page = new NotePage();
                } else if ((PAGE_MARKER + " ").equals(line)) {
                    // Older files escaped a note line that looked like the page marker this way.
                    appendBody(page, PAGE_MARKER);
                } else if (line.startsWith(PAGE_TITLE_PREFIX)) {
                    page.heading = limitHeading(unescape(line.substring(PAGE_TITLE_PREFIX.length())));
                } else {
                    appendBody(page, unescape(line));
                }
            }
            pages.add(page);
        } catch (IOException ex) {
            pages.clear();
            pages.add(new NotePage());
        } finally {
            closeQuietly(reader);
        }
        while (pages.size() > MAX_PAGES) {
            pages.remove(pages.size() - 1);
        }
        if (pages.isEmpty()) {
            pages.add(new NotePage());
        }
        for (NotePage page : pages) {
            page.body = limit(page.body);
            page.heading = limitHeading(page.heading);
        }
    }

    void save() {
        if (file == null) {
            return;
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            parent.mkdirs();
        }
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            for (int i = 0; i < pages.size(); i++) {
                if (i > 0) {
                    writer.write(PAGE_MARKER);
                    writer.newLine();
                }
                NotePage page = pages.get(i);
                if (!page.heading.isEmpty()) {
                    writer.write(PAGE_TITLE_PREFIX + page.heading);
                    writer.newLine();
                }
                for (String line : page.body.split("\n", -1)) {
                    // Lines that would look like a marker are escaped with an extra leading %.
                    writer.write(escape(line));
                    writer.newLine();
                }
            }
            writer.flush();
        } catch (IOException ex) {
            // Best effort: a failed write leaves the previous file in place.
        } finally {
            closeQuietly(writer);
        }
    }

    private static void appendBody(NotePage page, String line) {
        if (page.body.length() > 0) {
            page.body += "\n";
        }
        page.body += line;
    }

    /** A body line that would read as a marker gets one extra leading %, undone by {@link #unescape}. */
    static String escape(String line) {
        return line.startsWith("%%") ? "%" + line : line;
    }

    static String unescape(String line) {
        return line.startsWith("%%%") ? line.substring(1) : line;
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
            // Nothing useful to do while closing.
        }
    }
}

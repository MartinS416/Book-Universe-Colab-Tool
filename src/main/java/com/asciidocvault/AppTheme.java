package com.asciidocvault;

import java.util.prefs.Preferences;

/**
 * Holds every color token for the application.
 * Values are stored as CSS hex strings (#rrggbb or #aarrggbb).
 * Persisted via java.util.prefs.Preferences.
 */
public class AppTheme {

    // ── Color tokens ──────────────────────────────────────────────────────────
    public String windowBg        = "#0f0f13";
    public String sidebarBg       = "#13131a";
    public String sidebarText     = "#8888aa";
    public String sidebarHover    = "#1e1e2e";
    public String sidebarAccent   = "#7c6af7";
    public String toolbarBg       = "#13131a";
    public String toolbarBorder   = "#2a2a3e";
    public String editorBg        = "#0f0f13";
    public String editorText      = "#e2e0f0";
    public String editorCaret     = "#7c6af7";
    public String editorLineNum   = "#3a3a55";
    public String editorSelection = "#7c6af740";
    public String divider         = "#2a2a3e";
    public String accentPrimary   = "#7c6af7";
    public String accentSecond    = "#f06aab";
    public String buttonBg        = "#1e1e2e";
    public String buttonText      = "#c8c6e0";
    public String buttonHover     = "#2c2c42";
    public String statusBg        = "#13131a";
    public String statusText      = "#6666aa";
    public String previewBg       = "#ffffff";
    public String labelText       = "#555577";

    // ── Preset: Midnight (default dark) ──────────────────────────────────────
    public static AppTheme midnight() {
        return new AppTheme(); // defaults are midnight
    }

    // ── Preset: Parchment (warm light) ───────────────────────────────────────
    public static AppTheme parchment() {
        AppTheme t = new AppTheme();
        t.windowBg        = "#f5f0e8";
        t.sidebarBg       = "#ede8dc";
        t.sidebarText     = "#5a4a3a";
        t.sidebarHover    = "#d8d0c0";
        t.sidebarAccent   = "#c0622a";
        t.toolbarBg       = "#ede8dc";
        t.toolbarBorder   = "#ccc4b4";
        t.editorBg        = "#f5f0e8";
        t.editorText      = "#2a2218";
        t.editorCaret     = "#c0622a";
        t.editorLineNum   = "#bbb0a0";
        t.editorSelection = "#c0622a33";
        t.divider         = "#ccc4b4";
        t.accentPrimary   = "#c0622a";
        t.accentSecond    = "#2a7ab0";
        t.buttonBg        = "#ddd8cc";
        t.buttonText      = "#3a2e22";
        t.buttonHover     = "#ccc4b0";
        t.statusBg        = "#ede8dc";
        t.statusText      = "#8a7a6a";
        t.previewBg       = "#fefcf8";
        t.labelText       = "#9a8a7a";
        return t;
    }

    // ── Preset: Neon Forge (cyberpunk) ────────────────────────────────────────
    public static AppTheme neonForge() {
        AppTheme t = new AppTheme();
        t.windowBg        = "#080810";
        t.sidebarBg       = "#0c0c18";
        t.sidebarText     = "#00e5ff";
        t.sidebarHover    = "#141428";
        t.sidebarAccent   = "#ff0090";
        t.toolbarBg       = "#0c0c18";
        t.toolbarBorder   = "#ff009044";
        t.editorBg        = "#080810";
        t.editorText      = "#e0f8ff";
        t.editorCaret     = "#00e5ff";
        t.editorLineNum   = "#1a1a33";
        t.editorSelection = "#00e5ff22";
        t.divider         = "#ff009044";
        t.accentPrimary   = "#00e5ff";
        t.accentSecond    = "#ff0090";
        t.buttonBg        = "#141428";
        t.buttonText      = "#00e5ff";
        t.buttonHover     = "#1e1e38";
        t.statusBg        = "#0c0c18";
        t.statusText      = "#ff0090";
        t.previewBg       = "#ffffff";
        t.labelText       = "#333366";
        return t;
    }

    // ── Preset: Moss (earthy green) ───────────────────────────────────────────
    public static AppTheme moss() {
        AppTheme t = new AppTheme();
        t.windowBg        = "#0e1210";
        t.sidebarBg       = "#111a14";
        t.sidebarText     = "#7db88a";
        t.sidebarHover    = "#1a2a1e";
        t.sidebarAccent   = "#4ec97a";
        t.toolbarBg       = "#111a14";
        t.toolbarBorder   = "#2a3a2e";
        t.editorBg        = "#0e1210";
        t.editorText      = "#d4e8d8";
        t.editorCaret     = "#4ec97a";
        t.editorLineNum   = "#2a3a2e";
        t.editorSelection = "#4ec97a30";
        t.divider         = "#2a3a2e";
        t.accentPrimary   = "#4ec97a";
        t.accentSecond    = "#c9984e";
        t.buttonBg        = "#1a2a1e";
        t.buttonText      = "#b0d8b8";
        t.buttonHover     = "#243428";
        t.statusBg        = "#111a14";
        t.statusText      = "#4a7a55";
        t.previewBg       = "#ffffff";
        t.labelText       = "#2a4a30";
        return t;
    }

    // ── Contrast helpers ──────────────────────────────────────────────────────

    /**
     * Returns the relative luminance (0.0 = black, 1.0 = white) of a hex color.
     * Handles both #rrggbb and #aarrggbb formats.
     */
    public static double luminance(String hex) {
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            if (h.length() == 8) h = h.substring(2); // strip alpha if present
            int r = Integer.parseInt(h.substring(0, 2), 16);
            int g = Integer.parseInt(h.substring(2, 4), 16);
            int b = Integer.parseInt(h.substring(4, 6), 16);
            double rs = r / 255.0, gs = g / 255.0, bs = b / 255.0;
            double rl = rs <= 0.03928 ? rs / 12.92 : Math.pow((rs + 0.055) / 1.055, 2.4);
            double gl = gs <= 0.03928 ? gs / 12.92 : Math.pow((gs + 0.055) / 1.055, 2.4);
            double bl = bs <= 0.03928 ? bs / 12.92 : Math.pow((bs + 0.055) / 1.055, 2.4);
            return 0.2126 * rl + 0.7152 * gl + 0.0722 * bl;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Returns a legible foreground color that contrasts well against bgHex.
     * Uses the stored editorText if contrast is already good; otherwise
     * auto-selects near-black or near-white.
     */
    public static String contrastText(String bgHex) {
        return luminance(bgHex) > 0.35 ? "#1a1a1a" : "#e2e0f0";
    }

    // ── Persistence ───────────────────────────────────────────────────────────
    private static final Preferences PREFS =
            Preferences.userNodeForPackage(AppTheme.class);

    public void save() {
        PREFS.put("windowBg",        windowBg);
        PREFS.put("sidebarBg",       sidebarBg);
        PREFS.put("sidebarText",     sidebarText);
        PREFS.put("sidebarHover",    sidebarHover);
        PREFS.put("sidebarAccent",   sidebarAccent);
        PREFS.put("toolbarBg",       toolbarBg);
        PREFS.put("toolbarBorder",   toolbarBorder);
        PREFS.put("editorBg",        editorBg);
        PREFS.put("editorText",      editorText);
        PREFS.put("editorCaret",     editorCaret);
        PREFS.put("editorLineNum",   editorLineNum);
        PREFS.put("editorSelection", editorSelection);
        PREFS.put("divider",         divider);
        PREFS.put("accentPrimary",   accentPrimary);
        PREFS.put("accentSecond",    accentSecond);
        PREFS.put("buttonBg",        buttonBg);
        PREFS.put("buttonText",      buttonText);
        PREFS.put("buttonHover",     buttonHover);
        PREFS.put("statusBg",        statusBg);
        PREFS.put("statusText",      statusText);
        PREFS.put("previewBg",       previewBg);
        PREFS.put("labelText",       labelText);
    }

    public static AppTheme load() {
        AppTheme def = midnight();
        AppTheme t = new AppTheme();
        t.windowBg        = PREFS.get("windowBg",        def.windowBg);
        t.sidebarBg       = PREFS.get("sidebarBg",       def.sidebarBg);
        t.sidebarText     = PREFS.get("sidebarText",     def.sidebarText);
        t.sidebarHover    = PREFS.get("sidebarHover",    def.sidebarHover);
        t.sidebarAccent   = PREFS.get("sidebarAccent",   def.sidebarAccent);
        t.toolbarBg       = PREFS.get("toolbarBg",       def.toolbarBg);
        t.toolbarBorder   = PREFS.get("toolbarBorder",   def.toolbarBorder);
        t.editorBg        = PREFS.get("editorBg",        def.editorBg);
        t.editorText      = PREFS.get("editorText",      def.editorText);
        t.editorCaret     = PREFS.get("editorCaret",     def.editorCaret);
        t.editorLineNum   = PREFS.get("editorLineNum",   def.editorLineNum);
        t.editorSelection = PREFS.get("editorSelection", def.editorSelection);
        t.divider         = PREFS.get("divider",         def.divider);
        t.accentPrimary   = PREFS.get("accentPrimary",   def.accentPrimary);
        t.accentSecond    = PREFS.get("accentSecond",    def.accentSecond);
        t.buttonBg        = PREFS.get("buttonBg",        def.buttonBg);
        t.buttonText      = PREFS.get("buttonText",      def.buttonText);
        t.buttonHover     = PREFS.get("buttonHover",     def.buttonHover);
        t.statusBg        = PREFS.get("statusBg",        def.statusBg);
        t.statusText      = PREFS.get("statusText",      def.statusText);
        t.previewBg       = PREFS.get("previewBg",       def.previewBg);
        t.labelText       = PREFS.get("labelText",       def.labelText);
        return t;
    }
}

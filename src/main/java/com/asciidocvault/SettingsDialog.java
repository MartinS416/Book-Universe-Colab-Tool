package com.asciidocvault;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Modal settings dialog — fully themed.
 * Every surface colour is derived from the working AppTheme so the dialog
 * always matches whatever preset or custom colours are currently active.
 */
public class SettingsDialog extends Dialog<AppTheme> {

    private AppTheme working;
    private final Runnable livePreviewCallback;

    // Keep a reference to the root VBox so we can re-style it in-place
    private VBox contentRoot;

    public SettingsDialog(AppTheme current, Runnable livePreviewCallback) {
        this.working              = copy(current);
        this.livePreviewCallback  = livePreviewCallback;

        setTitle("Theme & Colors");
        setHeaderText(null);

        getDialogPane().setPrefWidth(660);
        getDialogPane().setPrefHeight(720);

        contentRoot = buildContentRoot();
        getDialogPane().setContent(buildScrollPane(contentRoot));

        ButtonType saveType   = new ButtonType("Save",   ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(saveType, cancelType);

        setResultConverter(bt -> bt == saveType ? working : null);

        applyDialogStyle();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Content builders
    // ─────────────────────────────────────────────────────────────────────────

    private ScrollPane buildScrollPane(VBox content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent; -fx-background:transparent;");
        return scroll;
    }

    private VBox buildContentRoot() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));

        root.getChildren().add(buildPresetsSection());
        root.getChildren().add(buildGroup("Window & Layout", new String[][]{
                {"windowBg",  "Window background"},
                {"divider",   "Split-pane divider"},
        }));
        root.getChildren().add(buildGroup("Sidebar / Explorer", new String[][]{
                {"sidebarBg",    "Sidebar background"},
                {"sidebarText",  "Sidebar text"},
                {"sidebarHover", "Sidebar hover"},
                {"sidebarAccent","Sidebar accent / icon"},
        }));
        root.getChildren().add(buildGroup("Toolbar", new String[][]{
                {"toolbarBg",     "Toolbar background"},
                {"toolbarBorder", "Toolbar bottom border"},
                {"buttonBg",      "Button background"},
                {"buttonText",    "Button text"},
                {"buttonHover",   "Button hover"},
        }));
        root.getChildren().add(buildGroup("Editor", new String[][]{
                {"editorBg",        "Editor background"},
                {"editorText",      "Editor text"},
                {"editorCaret",     "Caret / cursor"},
                {"editorLineNum",   "Line number gutter"},
                {"editorSelection", "Selection highlight"},
        }));
        root.getChildren().add(buildGroup("Accents", new String[][]{
                {"accentPrimary", "Primary accent"},
                {"accentSecond",  "Secondary accent"},
        }));
        root.getChildren().add(buildGroup("Status Bar & Labels", new String[][]{
                {"statusBg",   "Status bar background"},
                {"statusText", "Status bar text"},
                {"labelText",  "Section label text"},
        }));
        root.getChildren().add(buildGroup("Preview Pane", new String[][]{
                {"previewBg", "Preview background"},
        }));

        return root;
    }

    // ── Presets ───────────────────────────────────────────────────────────────

    private VBox buildPresetsSection() {
        Label title = sectionLabel("Presets");

        Button midnight  = presetButton("🌙 Midnight",   AppTheme.midnight());
        Button parchment = presetButton("📜 Parchment",  AppTheme.parchment());
        Button neon      = presetButton("⚡ Neon Forge",  AppTheme.neonForge());
        Button moss      = presetButton("🌿 Moss",        AppTheme.moss());

        HBox row = new HBox(10, midnight, parchment, neon, moss);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(8, title, row);
        box.setPadding(new Insets(0, 0, 4, 0));
        return box;
    }

    private Button presetButton(String label, AppTheme preset) {
        Button b = new Button(label);
        b.setOnAction(e -> {
            working = copy(preset);
            livePreviewCallback.run();
            // Rebuild entire content with fresh working theme
            contentRoot.getChildren().clear();
            VBox fresh = buildContentRoot();
            contentRoot.getChildren().addAll(fresh.getChildren());
            applyDialogStyle();
        });
        return b;
    }

    // ── Group builder ─────────────────────────────────────────────────────────

    private VBox buildGroup(String groupName, String[][] tokens) {
        Label title = sectionLabel(groupName);
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        grid.setPadding(new Insets(8, 0, 8, 12));

        int row = 0;
        for (String[] token : tokens) {
            String field      = token[0];
            String labelText  = token[1];
            String currentHex = getField(field);

            Label lbl = new Label(labelText);
            lbl.setMinWidth(190);

            // Swatch
            Rectangle swatch = new Rectangle(22, 22);
            swatch.setArcWidth(5);
            swatch.setArcHeight(5);
            try {
                swatch.setFill(Color.web(stripAlpha(currentHex)));
            } catch (Exception ignored) {
                swatch.setFill(Color.GRAY);
            }

            // Color picker
            ColorPicker picker = new ColorPicker();
            try {
                picker.setValue(Color.web(stripAlpha(currentHex)));
            } catch (Exception ignored) {
                picker.setValue(Color.GRAY);
            }

            // Hex text field
            TextField hexField = new TextField(currentHex);
            hexField.setPrefWidth(105);

            // Wire: picker → swatch + field + working
            picker.setOnAction(e -> {
                String hex = toHex(picker.getValue());
                setField(field, hex);
                swatch.setFill(picker.getValue());
                hexField.setText(hex);
                livePreviewCallback.run();
            });

            // Wire: hex field → picker + swatch + working
            hexField.setOnAction(e -> {
                try {
                    String txt = hexField.getText().trim();
                    if (!txt.startsWith("#")) txt = "#" + txt;
                    Color c = Color.web(stripAlpha(txt));
                    picker.setValue(c);
                    swatch.setFill(c);
                    setField(field, txt);
                    livePreviewCallback.run();
                } catch (Exception ignored) {}
            });

            grid.add(lbl,      0, row);
            grid.add(swatch,   1, row);
            grid.add(picker,   2, row);
            grid.add(hexField, 3, row);
            row++;
        }

        VBox box = new VBox(6, title, grid);
        box.setPadding(new Insets(12));
        return box;
    }

    // ── Full dialog theming ───────────────────────────────────────────────────

    /**
     * Applies working theme colours to every surface in the dialog —
     * DialogPane, scroll pane, group boxes, labels, text fields, buttons.
     * Uses contrastText() so text is always legible regardless of bg.
     */
    private void applyDialogStyle() {
        AppTheme t = working;

        // Derived colours
        String dialogBg   = t.windowBg;
        String dialogText = AppTheme.contrastText(dialogBg);
        String groupBg    = blend(dialogBg, dialogText, 0.05);  // subtle card surface
        String inputBg    = blend(dialogBg, dialogText, 0.08);
        String inputText  = AppTheme.contrastText(inputBg);
        String borderCol  = blend(dialogBg, dialogText, 0.18);
        String mutedText  = blend(dialogText, dialogBg, 0.45);

        // DialogPane itself
        getDialogPane().setStyle(
                "-fx-background-color:" + dialogBg + ";" +
                        "-fx-font-family:'Segoe UI','SF Pro Text',sans-serif;");

        // Button bar (Save / Cancel row at the bottom)
        Node buttonBar = getDialogPane().lookup(".button-bar");
        if (buttonBar != null) {
            buttonBar.setStyle("-fx-background-color:" + groupBg + ";");
        }
        // Style the Save and Cancel buttons
        getDialogPane().lookupAll(".button").forEach(node -> {
            if (node instanceof Button btn) {
                String bg   = btn.getText().equals("Save") ? t.accentPrimary : inputBg;
                String fg   = btn.getText().equals("Save")
                        ? AppTheme.contrastText(t.accentPrimary) : inputText;
                btn.setStyle(
                        "-fx-background-color:" + bg + ";" +
                                "-fx-text-fill:" + fg + ";" +
                                "-fx-background-radius:4;" +
                                "-fx-border-radius:4;" +
                                "-fx-font-size:12px;" +
                                "-fx-padding:5 16 5 16;" +
                                "-fx-cursor:hand;");
            }
        });

        // ScrollPane
        Node scroll = getDialogPane().lookup(".scroll-pane");
        if (scroll != null) {
            scroll.setStyle(
                    "-fx-background-color:" + dialogBg + ";" +
                            "-fx-background:" + dialogBg + ";");
        }
        // ScrollPane viewport
        getDialogPane().lookupAll(".viewport").forEach(n ->
                n.setStyle("-fx-background-color:" + dialogBg + ";"));
        // Scrollbars
        getDialogPane().lookupAll(".scroll-bar").forEach(n ->
                n.setStyle("-fx-background-color:" + dialogBg + ";"));
        getDialogPane().lookupAll(".scroll-bar > .thumb").forEach(n ->
                n.setStyle("-fx-background-color:" + borderCol + "; -fx-background-radius:4;"));
        getDialogPane().lookupAll(".scroll-bar > .track").forEach(n ->
                n.setStyle("-fx-background-color:transparent;"));
        getDialogPane().lookupAll(".scroll-bar > .increment-button," +
                ".scroll-bar > .decrement-button").forEach(n ->
                n.setStyle("-fx-background-color:transparent; -fx-padding:2;"));

        // Content root VBox
        if (contentRoot != null) {
            contentRoot.setStyle("-fx-background-color:" + dialogBg + ";");

            // Walk every child and apply per-type styles
            applyToAllChildren(contentRoot, dialogBg, dialogText, groupBg,
                    inputBg, inputText, borderCol, mutedText, t);
        }
    }

    /**
     * Recursively walks the node tree and applies theme-aware inline styles
     * based on node type.
     */
    private void applyToAllChildren(
            javafx.scene.Parent parent,
            String dialogBg, String dialogText,
            String groupBg, String inputBg, String inputText,
            String borderCol, String mutedText,
            AppTheme t) {

        for (Node node : parent.getChildrenUnmodifiable()) {

            if (node instanceof VBox vbox) {
                // Group card boxes
                if (vbox != contentRoot) {
                    vbox.setStyle(
                            "-fx-background-color:" + groupBg + ";" +
                                    "-fx-background-radius:6;" +
                                    "-fx-border-color:" + borderCol + ";" +
                                    "-fx-border-radius:6;" +
                                    "-fx-border-width:1;");
                }
                applyToAllChildren(vbox, dialogBg, dialogText, groupBg,
                        inputBg, inputText, borderCol, mutedText, t);

            } else if (node instanceof HBox hbox) {
                hbox.setStyle("-fx-background-color:transparent;");
                applyToAllChildren(hbox, dialogBg, dialogText, groupBg,
                        inputBg, inputText, borderCol, mutedText, t);

            } else if (node instanceof GridPane grid) {
                grid.setStyle("-fx-background-color:transparent;");
                applyToAllChildren(grid, dialogBg, dialogText, groupBg,
                        inputBg, inputText, borderCol, mutedText, t);

            } else if (node instanceof Label lbl) {
                // Section header labels vs token labels
                String text = lbl.getText() != null ? lbl.getText() : "";
                boolean isSection = text.equals(text.toUpperCase()) && text.length() > 2
                        && !text.contains(" ");
                // More reliable: section labels have no minWidth set (minWidth=0 default)
                if (lbl.getMinWidth() > 50) {
                    // token row label
                    lbl.setStyle("-fx-text-fill:" + dialogText + ";" +
                            "-fx-font-size:13px;");
                } else {
                    // section / group heading label
                    lbl.setStyle("-fx-text-fill:" + mutedText + ";" +
                            "-fx-font-size:10px;" +
                            "-fx-font-weight:bold;" +
                            "-fx-letter-spacing:1px;");
                }

            } else if (node instanceof TextField tf) {
                tf.setStyle(
                        "-fx-background-color:" + inputBg + ";" +
                                "-fx-text-fill:" + inputText + ";" +
                                "-fx-border-color:" + borderCol + ";" +
                                "-fx-border-width:1;" +
                                "-fx-border-radius:4;" +
                                "-fx-background-radius:4;" +
                                "-fx-font-size:12px;" +
                                "-fx-font-family:'JetBrains Mono','Consolas',monospace;" +
                                "-fx-padding:4 8 4 8;");

            } else if (node instanceof ColorPicker cp) {
                cp.setStyle(
                        "-fx-background-color:" + inputBg + ";" +
                                "-fx-text-fill:" + inputText + ";" +
                                "-fx-border-color:" + borderCol + ";" +
                                "-fx-border-width:1;" +
                                "-fx-border-radius:4;" +
                                "-fx-background-radius:4;" +
                                "-fx-color-rect-width:18px;" +
                                "-fx-color-rect-height:18px;");

            } else if (node instanceof Button btn) {
                // Preset buttons
                String btnBg  = blend(t.accentPrimary, dialogBg, 0.15);
                String btnFg  = AppTheme.contrastText(btnBg);
                btn.setStyle(
                        "-fx-background-color:" + btnBg + ";" +
                                "-fx-text-fill:" + btnFg + ";" +
                                "-fx-border-color:" + t.accentPrimary + "66;" +
                                "-fx-border-width:1;" +
                                "-fx-border-radius:4;" +
                                "-fx-background-radius:4;" +
                                "-fx-font-size:12px;" +
                                "-fx-cursor:hand;" +
                                "-fx-padding:5 12 5 12;");
                btn.setOnMouseEntered(e -> btn.setStyle(
                        "-fx-background-color:" + t.accentPrimary + ";" +
                                "-fx-text-fill:" + AppTheme.contrastText(t.accentPrimary) + ";" +
                                "-fx-border-color:" + t.accentPrimary + ";" +
                                "-fx-border-width:1;" +
                                "-fx-border-radius:4;" +
                                "-fx-background-radius:4;" +
                                "-fx-font-size:12px;" +
                                "-fx-cursor:hand;" +
                                "-fx-padding:5 12 5 12;"));
                // Re-apply base on exit by re-running applyDialogStyle lazily
                btn.setOnMouseExited(e -> btn.setStyle(
                        "-fx-background-color:" + btnBg + ";" +
                                "-fx-text-fill:" + btnFg + ";" +
                                "-fx-border-color:" + t.accentPrimary + "66;" +
                                "-fx-border-width:1;" +
                                "-fx-border-radius:4;" +
                                "-fx-background-radius:4;" +
                                "-fx-font-size:12px;" +
                                "-fx-cursor:hand;" +
                                "-fx-padding:5 12 5 12;"));

            } else if (node instanceof javafx.scene.Parent p) {
                applyToAllChildren(p, dialogBg, dialogText, groupBg,
                        inputBg, inputText, borderCol, mutedText, t);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Label sectionLabel(String text) {
        return new Label(text.toUpperCase());
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int)(c.getRed()   * 255),
                (int)(c.getGreen() * 255),
                (int)(c.getBlue()  * 255));
    }

    /** Strip 8-char alpha prefix so Color.web() can parse it. */
    private static String stripAlpha(String hex) {
        if (hex == null) return "#888888";
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        return h.length() == 8 ? "#" + h.substring(2) : "#" + h;
    }

    /**
     * Linear blend: factor=0 → pure a, factor=1 → pure b.
     * Used to derive subtle card and border colours from the base bg.
     */
    private static String blend(String hexA, String hexB, double factor) {
        try {
            Color a = Color.web(stripAlpha(hexA));
            Color b = Color.web(stripAlpha(hexB));
            double r = a.getRed()   + (b.getRed()   - a.getRed())   * factor;
            double g = a.getGreen() + (b.getGreen() - a.getGreen()) * factor;
            double bl = a.getBlue() + (b.getBlue()  - a.getBlue())  * factor;
            return String.format("#%02x%02x%02x",
                    (int)(Math.min(1, Math.max(0, r))  * 255),
                    (int)(Math.min(1, Math.max(0, g))  * 255),
                    (int)(Math.min(1, Math.max(0, bl)) * 255));
        } catch (Exception e) {
            return hexA;
        }
    }

    private String getField(String name) {
        return switch (name) {
            case "windowBg"        -> working.windowBg;
            case "sidebarBg"       -> working.sidebarBg;
            case "sidebarText"     -> working.sidebarText;
            case "sidebarHover"    -> working.sidebarHover;
            case "sidebarAccent"   -> working.sidebarAccent;
            case "toolbarBg"       -> working.toolbarBg;
            case "toolbarBorder"   -> working.toolbarBorder;
            case "editorBg"        -> working.editorBg;
            case "editorText"      -> working.editorText;
            case "editorCaret"     -> working.editorCaret;
            case "editorLineNum"   -> working.editorLineNum;
            case "editorSelection" -> working.editorSelection;
            case "divider"         -> working.divider;
            case "accentPrimary"   -> working.accentPrimary;
            case "accentSecond"    -> working.accentSecond;
            case "buttonBg"        -> working.buttonBg;
            case "buttonText"      -> working.buttonText;
            case "buttonHover"     -> working.buttonHover;
            case "statusBg"        -> working.statusBg;
            case "statusText"      -> working.statusText;
            case "previewBg"       -> working.previewBg;
            case "labelText"       -> working.labelText;
            default -> "#888888";
        };
    }

    private void setField(String name, String value) {
        switch (name) {
            case "windowBg"        -> working.windowBg        = value;
            case "sidebarBg"       -> working.sidebarBg       = value;
            case "sidebarText"     -> working.sidebarText     = value;
            case "sidebarHover"    -> working.sidebarHover    = value;
            case "sidebarAccent"   -> working.sidebarAccent   = value;
            case "toolbarBg"       -> working.toolbarBg       = value;
            case "toolbarBorder"   -> working.toolbarBorder   = value;
            case "editorBg"        -> working.editorBg        = value;
            case "editorText"      -> working.editorText      = value;
            case "editorCaret"     -> working.editorCaret     = value;
            case "editorLineNum"   -> working.editorLineNum   = value;
            case "editorSelection" -> working.editorSelection = value;
            case "divider"         -> working.divider         = value;
            case "accentPrimary"   -> working.accentPrimary   = value;
            case "accentSecond"    -> working.accentSecond    = value;
            case "buttonBg"        -> working.buttonBg        = value;
            case "buttonText"      -> working.buttonText      = value;
            case "buttonHover"     -> working.buttonHover     = value;
            case "statusBg"        -> working.statusBg        = value;
            case "statusText"      -> working.statusText      = value;
            case "previewBg"       -> working.previewBg       = value;
            case "labelText"       -> working.labelText       = value;
        }
    }

    private AppTheme copy(AppTheme src) {
        AppTheme t = new AppTheme();
        t.windowBg        = src.windowBg;
        t.sidebarBg       = src.sidebarBg;
        t.sidebarText     = src.sidebarText;
        t.sidebarHover    = src.sidebarHover;
        t.sidebarAccent   = src.sidebarAccent;
        t.toolbarBg       = src.toolbarBg;
        t.toolbarBorder   = src.toolbarBorder;
        t.editorBg        = src.editorBg;
        t.editorText      = src.editorText;
        t.editorCaret     = src.editorCaret;
        t.editorLineNum   = src.editorLineNum;
        t.editorSelection = src.editorSelection;
        t.divider         = src.divider;
        t.accentPrimary   = src.accentPrimary;
        t.accentSecond    = src.accentSecond;
        t.buttonBg        = src.buttonBg;
        t.buttonText      = src.buttonText;
        t.buttonHover     = src.buttonHover;
        t.statusBg        = src.statusBg;
        t.statusText      = src.statusText;
        t.previewBg       = src.previewBg;
        t.labelText       = src.labelText;
        return t;
    }
}
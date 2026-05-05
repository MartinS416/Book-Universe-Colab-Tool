package com.asciidocvault;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;

/**
 * Main UI controller — fully theme-driven.
 *
 * KEY FIX: lookup() calls (e.g. TextArea ".content", split-pane dividers)
 * only work AFTER the scene has been rendered. We defer those via
 * Platform.runLater() inside applyTheme(), guarded by null-checks.
 */
public class MainController {

    // ── Asciidoctor singleton ─────────────────────────────────────────────────
    private final Asciidoctor asciidoctor = Asciidoctor.Factory.create();

    // ── Theme ─────────────────────────────────────────────────────────────────
    private AppTheme theme = AppTheme.load();

    // ── Root & skinnable nodes ────────────────────────────────────────────────
    private SplitPane mainSplit;
    private SplitPane editorPreviewSplit;

    private VBox leftPane;
    private VBox centrePane;
    private VBox rightPane;

    private TreeView<File> fileTree;
    private Button         openDirBtn;
    private Label          explorerLabel;

    private TextArea editor;
    private ToolBar  editorToolbar;
    private Label    editorSectionLabel;
    private Label    statusLabel;
    private Label    statusDot;
    private HBox     statusBar;

    private Label   previewLabel;
    private WebView preview;

    // Toolbar buttons kept for re-styling
    private Button newBtn, openBtn, saveBtn, saveAsBtn, renderBtn, settingsBtn;

    // ── State ─────────────────────────────────────────────────────────────────
    private File    currentFile;
    private boolean dirty = false;

    // ── Debounce ──────────────────────────────────────────────────────────────
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "adoc-render");
                t.setDaemon(true);
                return t;
            });
    private ScheduledFuture<?> pendingRender;

    // ─────────────────────────────────────────────────────────────────────────
    public Parent buildUI() {
        buildLeftPane();
        buildCentrePane();
        buildRightPane();

        editorPreviewSplit = new SplitPane(centrePane, rightPane);
        editorPreviewSplit.setDividerPositions(0.55);

        mainSplit = new SplitPane(leftPane, editorPreviewSplit);
        mainSplit.setDividerPositions(0.18);

        // Apply the parts of the theme that DON'T need lookup() (safe before show)
        applyThemeEarly();

        // After the scene is rendered, apply the parts that need lookup()
        mainSplit.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(this::applyThemeLate);
            }
        });

        editor.setText(STARTER_DOC);
        return mainSplit;
    }

    // ── Pane builders ─────────────────────────────────────────────────────────

    private void buildLeftPane() {
        explorerLabel = new Label("EXPLORER");

        openDirBtn = new Button("⊞  Open Folder");
        openDirBtn.setMaxWidth(Double.MAX_VALUE);
        openDirBtn.setOnAction(e -> chooseDirectory());

        fileTree = new TreeView<>();
        fileTree.setShowRoot(true);
        fileTree.setCellFactory(tv -> new ThemedFileCell());
        // File opening is handled via onMouseClicked in ThemedFileCell

        VBox.setVgrow(fileTree, Priority.ALWAYS);
        leftPane = new VBox(6, explorerLabel, openDirBtn, fileTree);
        leftPane.setPadding(new Insets(10, 8, 10, 8));
    }

    private void buildCentrePane() {
        newBtn      = toolBtn("✦ New",      e -> newDocument());
        openBtn     = toolBtn("⊙ Open…",    e -> openFileDialog());
        saveBtn     = toolBtn("↓ Save",     e -> saveFile());
        saveAsBtn   = toolBtn("↓ Save As…", e -> saveFileAs());
        renderBtn   = toolBtn("⟳ Render",   e -> renderNow());
        settingsBtn = toolBtn("⚙ Theme",    e -> openSettings());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        editorToolbar = new ToolBar(
                newBtn, openBtn,
                new Separator(),
                saveBtn, saveAsBtn,
                new Separator(),
                renderBtn,
                spacer,
                settingsBtn);

        editorSectionLabel = new Label("EDITOR");

        editor = new TextArea();
        editor.setWrapText(true);
        editor.textProperty().addListener((obs, o, n) -> {
            dirty = true;
            updateStatus();
            scheduleRender(n);
        });

        statusDot   = new Label("●");
        statusLabel = new Label("Untitled");
        HBox statusInner = new HBox(6, statusDot, statusLabel);
        statusInner.setAlignment(Pos.CENTER_LEFT);

        statusBar = new HBox(statusInner);
        statusBar.setPadding(new Insets(4, 12, 4, 12));
        statusBar.setAlignment(Pos.CENTER_LEFT);

        VBox.setVgrow(editor, Priority.ALWAYS);
        centrePane = new VBox(editorToolbar, editorSectionLabel, editor, statusBar);
    }

    private void buildRightPane() {
        previewLabel = new Label("PREVIEW");
        preview = new WebView();
        preview.setContextMenuEnabled(false);

        VBox.setVgrow(preview, Priority.ALWAYS);
        rightPane = new VBox(previewLabel, preview);
    }

    // ── Theme: safe before scene is shown ─────────────────────────────────────

    /**
     * Only styles nodes that don't require CSS lookup() — safe to call
     * before the scene is attached / displayed.
     */
    private void applyThemeEarly() {
        AppTheme t = theme;

        // Splits
        mainSplit.setStyle("-fx-background-color:" + t.windowBg + ";");
        editorPreviewSplit.setStyle("-fx-background-color:" + t.windowBg + ";");

        // Left pane
        leftPane.setStyle("-fx-background-color:" + t.sidebarBg + ";");
        explorerLabel.setStyle(labelStyle(t));
        styleOpenDirBtn(t);

        fileTree.setStyle(
                "-fx-background-color:" + t.sidebarBg + ";" +
                        "-fx-border-color:transparent;");

        // Toolbar
        editorToolbar.setStyle(toolbarStyle(t));
        for (Button b : new Button[]{newBtn, openBtn, saveBtn, saveAsBtn, renderBtn}) {
            styleToolbarBtn(b, t);
        }
        settingsBtn.setStyle(settingsBtnStyle(t));
        settingsBtn.setOnMouseEntered(e -> settingsBtn.setStyle(settingsBtnStyleHover(t)));
        settingsBtn.setOnMouseExited (e -> settingsBtn.setStyle(settingsBtnStyle(t)));

        editorSectionLabel.setStyle(sectionLabelStyle(t, t.editorBg));

        // Editor (no lookup needed — just the outer node)
        editor.setStyle(editorStyle(t));

        // Status bar
        statusBar.setStyle("-fx-background-color:" + t.statusBg + ";");
        statusLabel.setStyle(
                "-fx-text-fill:" + t.statusText + ";" +
                        "-fx-font-size:11px;" +
                        "-fx-font-family:'Segoe UI',sans-serif;");
        statusDot.setStyle(
                "-fx-text-fill:" + (dirty ? t.accentSecond : t.accentPrimary) + ";" +
                        "-fx-font-size:9px;");

        // Preview pane
        previewLabel.setStyle(sectionLabelStyle(t, t.editorBg));
        rightPane.setStyle("-fx-background-color:" + t.editorBg + ";");
    }

    /**
     * Styles that require lookup() — only safe AFTER the scene is shown
     * and the CSS skin pass has completed.
     */
    private void applyThemeLate() {
        AppTheme t = theme;

        // TextArea inner content background
        if (editor.lookup(".content") != null) {
            editor.lookup(".content").setStyle(
                    "-fx-background-color:" + t.editorBg + ";");
        }

        // TextArea scroll bars
        editor.lookupAll(".scroll-bar").forEach(n ->
                n.setStyle("-fx-background-color:" + t.editorBg + ";"));
        editor.lookupAll(".scroll-bar > .track").forEach(n ->
                n.setStyle("-fx-background-color:" + t.editorBg + ";"));
        editor.lookupAll(".scroll-bar > .thumb").forEach(n ->
                n.setStyle("-fx-background-color:" + t.divider + "; -fx-background-radius:4;"));

        // SplitPane dividers
        mainSplit.lookupAll(".split-pane-divider").forEach(n ->
                n.setStyle("-fx-background-color:" + t.divider + "; -fx-padding:0 1 0 1;"));
        editorPreviewSplit.lookupAll(".split-pane-divider").forEach(n ->
                n.setStyle("-fx-background-color:" + t.divider + "; -fx-padding:0 1 0 1;"));

        // Toolbar separators
        editorToolbar.lookupAll(".separator").forEach(n ->
                n.setStyle("-fx-background-color:" + t.toolbarBorder + ";"));

        // FileTree rows
        fileTree.refresh();
    }

    // ── Public re-skin entry (called after settings save) ─────────────────────

    public void applyTheme() {
        applyThemeEarly();
        // Run late part after next layout pass
        Platform.runLater(this::applyThemeLate);
    }

    // ── Style helpers ─────────────────────────────────────────────────────────

    private String editorStyle(AppTheme t) {
        // Auto-pick text color from background luminance (WCAG contrast ratio >= 3:1).
        // Honour the user's chosen editorText if it already contrasts well enough;
        // otherwise fall back to near-black or near-white automatically.
        double bgLum        = AppTheme.luminance(t.editorBg);
        double txtLum       = AppTheme.luminance(t.editorText);
        double contrast     = (Math.max(bgLum, txtLum) + 0.05) /
                (Math.min(bgLum, txtLum) + 0.05);
        String resolvedText = contrast >= 3.0 ? t.editorText : AppTheme.contrastText(t.editorBg);
        return "-fx-background-color:" + t.editorBg + ";" +
                "-fx-text-fill:" + resolvedText + ";" +
                "-fx-font-family:'JetBrains Mono','Cascadia Code','Consolas',monospace;" +
                "-fx-font-size:14px;" +
                "-fx-highlight-fill:" + t.editorSelection + ";" +
                "-fx-highlight-text-fill:" + resolvedText + ";" +
                "-fx-border-color:transparent;" +
                "-fx-control-inner-background:" + t.editorBg + ";";
    }

    private String toolbarStyle(AppTheme t) {
        return "-fx-background-color:" + t.toolbarBg + ";" +
                "-fx-border-color:transparent transparent " + t.toolbarBorder + " transparent;" +
                "-fx-border-width:0 0 1 0;" +
                "-fx-padding:4 8 4 8;";
    }

    private String labelStyle(AppTheme t) {
        return "-fx-text-fill:" + t.labelText + ";" +
                "-fx-font-size:10px; -fx-font-weight:bold;" +
                "-fx-font-family:'Segoe UI','SF Pro Text',sans-serif;";
    }

    private String sectionLabelStyle(AppTheme t, String bg) {
        return "-fx-text-fill:" + t.labelText + ";" +
                "-fx-font-size:10px; -fx-font-weight:bold;" +
                "-fx-padding:4 0 2 12;" +
                "-fx-font-family:'Segoe UI','SF Pro Text',sans-serif;" +
                "-fx-background-color:" + bg + ";";
    }

    private String btnBase(AppTheme t) {
        return "-fx-background-color:" + t.buttonBg + ";" +
                "-fx-text-fill:" + t.buttonText + ";" +
                "-fx-font-size:12px;" +
                "-fx-font-family:'Segoe UI',sans-serif;" +
                "-fx-cursor:hand;" +
                "-fx-padding:4 10 4 10;" +
                "-fx-background-radius:4;";
    }

    private String btnHover(AppTheme t) {
        return "-fx-background-color:" + t.buttonHover + ";" +
                "-fx-text-fill:" + t.accentPrimary + ";" +
                "-fx-font-size:12px;" +
                "-fx-font-family:'Segoe UI',sans-serif;" +
                "-fx-cursor:hand;" +
                "-fx-padding:4 10 4 10;" +
                "-fx-background-radius:4;";
    }

    private String settingsBtnStyle(AppTheme t) {
        return "-fx-background-color:" + t.accentPrimary + "22;" +
                "-fx-text-fill:" + t.accentPrimary + ";" +
                "-fx-font-size:12px;" +
                "-fx-font-family:'Segoe UI',sans-serif;" +
                "-fx-cursor:hand;" +
                "-fx-padding:4 12 4 12;" +
                "-fx-background-radius:4;" +
                "-fx-border-radius:4;" +
                "-fx-border-color:" + t.accentPrimary + "55;" +
                "-fx-border-width:1;";
    }

    private String settingsBtnStyleHover(AppTheme t) {
        return "-fx-background-color:" + t.accentPrimary + "44;" +
                "-fx-text-fill:" + t.accentPrimary + ";" +
                "-fx-font-size:12px;" +
                "-fx-font-family:'Segoe UI',sans-serif;" +
                "-fx-cursor:hand;" +
                "-fx-padding:4 12 4 12;" +
                "-fx-background-radius:4;" +
                "-fx-border-radius:4;" +
                "-fx-border-color:" + t.accentPrimary + ";" +
                "-fx-border-width:1;";
    }

    private void styleToolbarBtn(Button b, AppTheme t) {
        b.setStyle(btnBase(t));
        b.setOnMouseEntered(e -> b.setStyle(btnHover(t)));
        b.setOnMouseExited (e -> b.setStyle(btnBase(t)));
    }

    private void styleOpenDirBtn(AppTheme t) {
        String base  = "-fx-background-color:" + t.buttonBg + ";" +
                "-fx-text-fill:" + t.sidebarText + ";" +
                "-fx-font-size:12px; -fx-font-family:'Segoe UI',sans-serif;" +
                "-fx-cursor:hand; -fx-padding:5 10 5 10;" +
                "-fx-background-radius:4; -fx-border-radius:4;" +
                "-fx-border-color:" + t.sidebarAccent + "44; -fx-border-width:1;";
        String hover = "-fx-background-color:" + t.buttonHover + ";" +
                "-fx-text-fill:" + t.sidebarAccent + ";" +
                "-fx-font-size:12px; -fx-font-family:'Segoe UI',sans-serif;" +
                "-fx-cursor:hand; -fx-padding:5 10 5 10;" +
                "-fx-background-radius:4; -fx-border-radius:4;" +
                "-fx-border-color:" + t.sidebarAccent + "; -fx-border-width:1;";
        openDirBtn.setStyle(base);
        openDirBtn.setOnMouseEntered(e -> openDirBtn.setStyle(hover));
        openDirBtn.setOnMouseExited (e -> openDirBtn.setStyle(base));
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    private void openSettings() {
        SettingsDialog dlg = new SettingsDialog(theme, () -> {});
        dlg.initOwner(getStage());
        dlg.showAndWait().ifPresent(newTheme -> {
            theme = newTheme;
            theme.save();
            applyTheme();
        });
    }

    // ── File tree cell ────────────────────────────────────────────────────────

    private class ThemedFileCell extends TreeCell<File> {
        @Override
        protected void updateItem(File file, boolean empty) {
            super.updateItem(file, empty);
            if (empty || file == null) {
                setText(null);
                setGraphic(null);
                setOnMouseClicked(null);
                setOnMouseEntered(null);
                setOnMouseExited(null);
                setStyle("-fx-background-color:transparent;");
                return;
            }

            String icon = file.isDirectory() ? "▸ " : "  ";
            setText(icon + file.getName());

            // Build styles each time so they always reflect current theme
            String base = "-fx-text-fill:" + theme.sidebarText + ";" +
                    "-fx-font-family:'Segoe UI',sans-serif;" +
                    "-fx-font-size:13px; -fx-padding:2 4 2 4;" +
                    "-fx-background-color:transparent;";
            String hoverStyle = "-fx-background-color:" + theme.sidebarHover + ";" +
                    "-fx-text-fill:" + theme.sidebarText + ";" +
                    "-fx-font-family:'Segoe UI',sans-serif;" +
                    "-fx-font-size:13px; -fx-padding:2 4 2 4;";
            String selected = "-fx-background-color:" + theme.accentPrimary + "33;" +
                    "-fx-text-fill:" + theme.sidebarAccent + ";" +
                    "-fx-font-family:'Segoe UI',sans-serif;" +
                    "-fx-font-size:13px; -fx-padding:2 4 2 4;";

            // Highlight the currently open file
            boolean isCurrent = currentFile != null
                    && currentFile.equals(file);
            setStyle(isCurrent ? selected : base);

            setOnMouseEntered(e -> {
                if (!isCurrent) setStyle(hoverStyle);
            });
            setOnMouseExited(e -> {
                boolean stillCurrent = currentFile != null && currentFile.equals(file);
                setStyle(stillCurrent ? selected : base);
            });

            // Single click on a file opens it in the editor.
            // Directories use the tree's built-in expand/collapse — no override needed.
            setOnMouseClicked(e -> {
                if (file.isFile()) {
                    openFile(file);
                    fileTree.refresh();
                }
            });
        }
    }

    // ── File operations ───────────────────────────────────────────────────────

    private void chooseDirectory() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Open Workspace Folder");
        File dir = dc.showDialog(getStage());
        if (dir != null) populateTree(dir);
    }

    private void populateTree(File root) {
        TreeItem<File> item = buildTreeItem(root);
        item.setExpanded(true);
        fileTree.setRoot(item);
    }

    private TreeItem<File> buildTreeItem(File file) {
        TreeItem<File> item = new TreeItem<>(file);
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                // Sort: directories first, then files, both alphabetically
                java.util.Arrays.sort(children, (a, b) -> {
                    if (a.isDirectory() && !b.isDirectory()) return -1;
                    if (!a.isDirectory() && b.isDirectory()) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                });
                for (File child : children) {
                    if (!child.getName().startsWith(".")) {
                        TreeItem<File> childItem = buildTreeItem(child);
                        // Expand all directories by default
                        if (child.isDirectory()) childItem.setExpanded(true);
                        item.getChildren().add(childItem);
                    }
                }
            }
        }
        return item;
    }

    private void openFile(File file) {
        // Auto-save current file before switching
        if (dirty && currentFile != null) {
            writeFile(currentFile);
        }
        try {
            currentFile = file;
            dirty = false;
            editor.setText(Files.readString(file.toPath(), StandardCharsets.UTF_8));
            updateStatus();
            fileTree.refresh();
        } catch (IOException ex) {
            showError("Cannot open file", ex.getMessage());
        }
    }

    private void openFileDialog() {
        if (!confirmDiscardIfDirty()) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Open AsciiDoc File");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("AsciiDoc",
                        "*.adoc", "*.asciidoc", "*.asc", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File f = fc.showOpenDialog(getStage());
        if (f != null) openFile(f);
    }

    private void newDocument() {
        if (!confirmDiscardIfDirty()) return;
        currentFile = null;
        dirty = false;
        editor.setText(STARTER_DOC);
        updateStatus();
    }

    private void saveFile() {
        if (currentFile == null) { saveFileAs(); return; }
        writeFile(currentFile);
    }

    private void saveFileAs() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save AsciiDoc File");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("AsciiDoc Files", "*.adoc"));
        if (currentFile != null) {
            fc.setInitialDirectory(currentFile.getParentFile());
            fc.setInitialFileName(currentFile.getName());
        } else {
            fc.setInitialFileName("document.adoc");
        }
        File f = fc.showSaveDialog(getStage());
        if (f != null) { currentFile = f; writeFile(f); }
    }

    private void writeFile(File f) {
        try {
            Files.writeString(f.toPath(), editor.getText(), StandardCharsets.UTF_8);
            dirty = false;
            updateStatus();
        } catch (IOException ex) {
            showError("Cannot save file", ex.getMessage());
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private void scheduleRender(String text) {
        if (pendingRender != null) pendingRender.cancel(false);
        pendingRender = scheduler.schedule(
                () -> Platform.runLater(() -> renderToPreview(text)),
                400, TimeUnit.MILLISECONDS);
    }

    private void renderNow() { renderToPreview(editor.getText()); }

    private void renderToPreview(String adocText) {
        try {
            String html = asciidoctor.convert(adocText,
                    Options.builder().headerFooter(true).safe(SafeMode.UNSAFE).build());
            String styled = html.replace("</head>",
                    "<style>body{background:" + theme.previewBg +
                            "; padding: 2em;}</style></head>");
            preview.getEngine().loadContent(styled);
        } catch (Exception ex) {
            preview.getEngine().loadContent(
                    "<html><body style='background:" + theme.previewBg +
                            "'><pre style='color:red'>" + escapeHtml(ex.getMessage()) +
                            "</pre></body></html>");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void updateStatus() {
        String name = currentFile != null ? currentFile.getName() : "Untitled";
        statusLabel.setText(name + (dirty ? "  —  unsaved" : "  —  saved"));
        statusDot.setStyle("-fx-text-fill:" +
                (dirty ? theme.accentSecond : theme.accentPrimary) +
                "; -fx-font-size:9px;");
    }

    private boolean confirmDiscardIfDirty() {
        if (!dirty) return true;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Discard unsaved changes?", ButtonType.YES, ButtonType.CANCEL);
        a.setTitle("Unsaved Changes");
        return a.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle(title);
        a.showAndWait();
    }

    private Stage getStage() {
        return editor.getScene() != null ? (Stage) editor.getScene().getWindow() : null;
    }

    private static Button toolBtn(String text,
                                  javafx.event.EventHandler<javafx.event.ActionEvent> h) {
        Button b = new Button(text);
        b.setOnAction(h);
        return b;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ── Starter doc ───────────────────────────────────────────────────────────
    private static final String STARTER_DOC =
            "= My AsciiDoc Document\n"
                    + "Author Name <author@example.com>\n"
                    + ":toc:\n"
                    + ":icons: font\n"
                    + "\n"
                    + "== Introduction\n"
                    + "\n"
                    + "Welcome to your *AsciiDoc Workspace*!\n"
                    + "The preview updates automatically as you type.\n"
                    + "\n"
                    + "== Features\n"
                    + "\n"
                    + "* Live preview pane\n"
                    + "* File explorer sidebar\n"
                    + "* Full color theme customisation — click ⚙ Theme\n"
                    + "\n"
                    + "== Code Example\n"
                    + "\n"
                    + "[source,java]\n"
                    + "----\n"
                    + "public class Hello {\n"
                    + "    public static void main(String[] args) {\n"
                    + "        System.out.println(\"Hello, AsciiDoc!\");\n"
                    + "    }\n"
                    + "}\n"
                    + "----\n"
                    + "\n"
                    + "== Table\n"
                    + "\n"
                    + "|===\n"
                    + "| Name  | Value\n"
                    + "\n"
                    + "| Alpha | 1\n"
                    + "| Beta  | 2\n"
                    + "|===\n";
}
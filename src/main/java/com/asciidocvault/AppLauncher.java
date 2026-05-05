package com.asciidocvault;

/**
 * Plain main-class entry point that delegates to MainApp.
 * Required so the fat-jar manifest does not reference a JavaFX Application
 * directly (which causes a runtime error when the JavaFX module is not on
 * the module-path).
 */
public class AppLauncher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}

package norivensuu.autoupdater;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.JSONUtil;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ThemeManager {

    private JSONObject themesJSON;
    private List<String> themeKeys;
    private int currentIndex; // Index of the currently active theme

    /**
     * Loads the theme configuration from a JSON file.
     *
     * @param configFilePath full path to the themes.json file.
     */
    public ThemeManager(String configFilePath) {
        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            createDefaultThemesConfig(configFile);
        }
        loadThemes(configFile);
        // Build the list of theme keys (excluding non-theme keys like "lastTheme")
        themeKeys = new ArrayList<>();
        for (Object keyObj : themesJSON.keySet()) {
            String key = (String) keyObj;
            if (!"lastTheme".equals(key)) {
                themeKeys.add(key);
            }
        }
        // Determine the last theme from the config; if none, default to the first theme.
        String lastTheme = (String) themesJSON.get("lastTheme");
        if (lastTheme != null && themeKeys.contains(lastTheme)) {
            currentIndex = themeKeys.indexOf(lastTheme);
        } else {
            currentIndex = 0;
        }
    }

    /**
     * Creates a default themes configuration and writes it to file.
     *
     * @param file The configuration file to create.
     */
    private void createDefaultThemesConfig(File file) {
        // Ensure parent directory exists.
        file.getParentFile().mkdirs();
        JSONObject defaultThemes = new JSONObject();

        JSONObject light = new JSONObject();
        light.put("background", "#ffffff");
        light.put("foreground", "#000000");
        light.put("buttonBackground", "#e0e0e0");
        light.put("buttonForeground", "#000000");
        light.put("toggleIcon", "/icon_dark.png");

        JSONObject dark = new JSONObject();
        dark.put("background", "#2b2b2b");
        dark.put("foreground", "#f5f5f5");
        dark.put("buttonBackground", "#3c3f41");
        dark.put("buttonForeground", "#f5f5f5");
        dark.put("toggleIcon", "/icon_light.png");

        // Save the themes and set a default last theme.
        defaultThemes.put("light", light);
        defaultThemes.put("dark", dark);
        defaultThemes.put("lastTheme", "dark");

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(defaultThemes.toJSONString(JSONStyle.NO_COMPRESS));
            System.out.println("Created default themes.json at " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the themes JSON from the given file.
     *
     * @param file The configuration file to load.
     */
    private void loadThemes(File file) {
        try (FileReader reader = new FileReader(file)) {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(reader);
            themesJSON = (JSONObject) obj;
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            // Fallback: define two default themes if loading fails
            themesJSON = new JSONObject();

            JSONObject light = new JSONObject();
            light.put("background", "#ffffff");
            light.put("foreground", "#000000");
            light.put("buttonBackground", "#e0e0e0");
            light.put("buttonForeground", "#000000");
            light.put("toggleIcon", "/icon_dark.png");
            themesJSON.put("light", light);

            JSONObject dark = new JSONObject();
            dark.put("background", "#2b2b2b");
            dark.put("foreground", "#f5f5f5");
            dark.put("buttonBackground", "#3c3f41");
            dark.put("buttonForeground", "#f5f5f5");
            dark.put("toggleIcon", "/icon_light.png");
            themesJSON.put("dark", dark);
            themesJSON.put("lastTheme", "dark");
        }
    }

    /**
     * Returns the key for the currently active theme.
     */
    public String getCurrentThemeKey() {
        return themeKeys.get(currentIndex);
    }

    /**
     * Cycles to the next theme in the configuration.
     */
    public void toggleTheme(File file) {
        file.getParentFile().mkdirs();

        currentIndex = (currentIndex + 1) % themeKeys.size();
        // Update the "lastTheme" value in the JSON to preserve the theme choice.
        themesJSON.put("lastTheme", getCurrentThemeKey());

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(themesJSON.toJSONString(JSONStyle.NO_COMPRESS));
            System.out.println("Created default themes.json at " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Color getBackgroundColor() {
        return hexToColor(getColorValue("background"));
    }

    public Color getForegroundColor() {
        return hexToColor(getColorValue("foreground"));
    }

    public Color getButtonBackgroundColor() {
        return hexToColor(getColorValue("buttonBackground"));
    }

    public Color getButtonForegroundColor() {
        return hexToColor(getColorValue("buttonForeground"));
    }

    /**
     * Returns the icon path for the next theme in the cycle.
     * This is used for the toggle button so that it indicates which theme will appear next.
     */
    public String getToggleIconPath() {
        int nextIndex = (currentIndex + 1) % themeKeys.size();
        String nextThemeKey = themeKeys.get(nextIndex);
        JSONObject nextTheme = (JSONObject) themesJSON.get(nextThemeKey);
        return (String) nextTheme.get("toggleIcon");
    }

    private String getColorValue(String key) {
        String themeKey = getCurrentThemeKey();
        JSONObject theme = (JSONObject) themesJSON.get(themeKey);
        return (String) theme.get(key);
    }

    private Color hexToColor(String hex) {
        return Color.decode(hex);
    }

    /**
     * Recursively applies the current theme's colors to the component and its children.
     *
     * @param comp The component to update.
     */
    public void applyTheme(Component comp) {
        if (comp instanceof JPanel || comp instanceof JFrame) {
            comp.setBackground(getBackgroundColor());
            comp.setForeground(getForegroundColor());
        } else if (comp instanceof JScrollPane) {
            // Special treatment for scroll panes: update both the scroll pane and its viewport.
            JScrollPane scrollPane = (JScrollPane) comp;
            scrollPane.setBackground(getBackgroundColor());
            if (scrollPane.getViewport() != null) {
                scrollPane.getViewport().setBackground(getBackgroundColor());
            }
        } else if (comp instanceof JLabel || comp instanceof JTextArea) {
            comp.setForeground(getForegroundColor());
        } else if (comp instanceof JButton) {
            comp.setBackground(getButtonBackgroundColor());
            comp.setForeground(getButtonForegroundColor());
        }

        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                applyTheme(child);
            }
        }
    }
}
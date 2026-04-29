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
    private File configFile;

    /**
     * Loads the theme configuration from a JSON file.
     *
     * @param configFilePath full path to the themes.json file.
     */
    public ThemeManager(String configFilePath) {
        configFile = new File(configFilePath);
        if (!configFile.exists()) {
            createDefaultThemesConfig(configFile);
        }
        loadThemes(configFile);

        themeKeys = new ArrayList<>();
        for (Object keyObj : themesJSON.keySet()) {
            String key = (String) keyObj;
            if (!"lastTheme".equals(key)) {
                themeKeys.add(key);
            }
        }

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
        file.getParentFile().mkdirs();
        JSONObject defaultThemes = new JSONObject();

        JSONObject light = new JSONObject();
        light.put("background", "#f3f6f8");
        light.put("foreground", "#17202a");
        light.put("mutedForeground", "#637083");
        light.put("surface", "#ffffff");
        light.put("surfaceAlt", "#e8eef3");
        light.put("accent", "#18a999");
        light.put("danger", "#d84c5f");
        light.put("buttonBackground", "#ffffff");
        light.put("buttonForeground", "#17202a");
        light.put("toggleIcon", "/icon_dark.png");

        JSONObject dark = new JSONObject();
        dark.put("background", "#111820");
        dark.put("foreground", "#f4f7fb");
        dark.put("mutedForeground", "#aab7c7");
        dark.put("surface", "#192330");
        dark.put("surfaceAlt", "#0c1219");
        dark.put("accent", "#4fd1c5");
        dark.put("danger", "#ff6b81");
        dark.put("buttonBackground", "#192330");
        dark.put("buttonForeground", "#f4f7fb");
        dark.put("toggleIcon", "/icon_light.png");

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
            themesJSON = new JSONObject();

            JSONObject light = new JSONObject();
            light.put("background", "#f3f6f8");
            light.put("foreground", "#17202a");
            light.put("mutedForeground", "#637083");
            light.put("surface", "#ffffff");
            light.put("surfaceAlt", "#e8eef3");
            light.put("accent", "#18a999");
            light.put("danger", "#d84c5f");
            light.put("buttonBackground", "#ffffff");
            light.put("buttonForeground", "#17202a");
            light.put("toggleIcon", "/icon_dark.png");
            themesJSON.put("light", light);

            JSONObject dark = new JSONObject();
            dark.put("background", "#111820");
            dark.put("foreground", "#f4f7fb");
            dark.put("mutedForeground", "#aab7c7");
            dark.put("surface", "#192330");
            dark.put("surfaceAlt", "#0c1219");
            dark.put("accent", "#4fd1c5");
            dark.put("danger", "#ff6b81");
            dark.put("buttonBackground", "#192330");
            dark.put("buttonForeground", "#f4f7fb");
            dark.put("toggleIcon", "/icon_light.png");
            themesJSON.put("dark", dark);
            themesJSON.put("lastTheme", "dark");

            createDefaultThemesConfig(file);
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

    public Color getMutedForegroundColor() {
        return hexToColor(getColorValue("mutedForeground", isDarkTheme() ? "#aab7c7" : "#637083"));
    }

    public Color getSurfaceColor() {
        return hexToColor(getColorValue("surface", isDarkTheme() ? "#192330" : "#ffffff"));
    }

    public Color getSurfaceAltColor() {
        return hexToColor(getColorValue("surfaceAlt", isDarkTheme() ? "#0c1219" : "#e8eef3"));
    }

    public Color getAccentColor() {
        return hexToColor(getColorValue("accent", isDarkTheme() ? "#4fd1c5" : "#18a999"));
    }

    public Color getDangerColor() {
        return hexToColor(getColorValue("danger", isDarkTheme() ? "#ff6b81" : "#d84c5f"));
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
        return getColorValue(key, "#ffffff");
    }

    private String getColorValue(String key, String fallback) {
        String themeKey = getCurrentThemeKey();
        JSONObject theme = (JSONObject) themesJSON.get(themeKey);
        Object value = theme.get(key);

        if (value == null) createDefaultThemesConfig(configFile); // TODO Replace this config file restoration fallback with dynamic config population. Overwise the 1% of people who'll ever modify the config to add custom themes would suffer from config completely resetting every time the default keys change.

        return value == null ? fallback : (String) value;
    }

    private boolean isDarkTheme() {
        return "dark".equals(getCurrentThemeKey());
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
        if (comp instanceof RoundedPanel) {
            comp.setBackground(getSurfaceColor());
            comp.setForeground(getForegroundColor());
        } else if (comp instanceof JPanel || comp instanceof JFrame) {
            comp.setBackground(getBackgroundColor());
            comp.setForeground(getForegroundColor());
        } else if (comp instanceof JScrollPane) {
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

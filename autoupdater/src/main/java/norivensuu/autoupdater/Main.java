package norivensuu.autoupdater;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.mozilla.universalchardet.UniversalDetector;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * UpdaterState is a simple container representing one state of the updater.
 */
class UpdaterState {
    private String id;
    private String headerText;
    private String gifResource;

    public UpdaterState(String id, String headerText, String gifResource) {
        this.id = id;
        this.headerText = headerText;
        this.gifResource = gifResource;
    }

    public String getId() {
        return id;
    }

    public String getHeaderText() {
        return headerText;
    }

    public String getGifResource() {
        return gifResource;
    }

    public void setHeaderText(String headerText) {
        this.headerText = headerText;
    }

    public void setGifResource(String gifResource) {
        this.gifResource = gifResource;
    }
}

/**
 * StateRecorder is responsible for managing and changing the current visual state.
 */
class StateRecorder {
    private final UpdaterFrame frame;
    private final Map<String, UpdaterState> stateMap = new HashMap<>();
    private UpdaterState currentState;

    public StateRecorder(UpdaterFrame frame) {
        this.frame = frame;
        registerDefaultStates();
    }

    private void registerDefaultStates() {
        registerState(new UpdaterState("idle", "<html><h1>Mod Updater</h1></html>", "/idle.gif"));
        registerState(new UpdaterState("download", "<html><h1>Downloading Release...</h1></html>", "/download.gif"));
        registerState(new UpdaterState("unpack", "<html><h1>Unpacking Archive...</h1></html>", "/unpack.gif"));
        registerState(new UpdaterState("update", "<html><h1>Updating Mods...</h1></html>", "/update.gif"));
        registerState(new UpdaterState("complete", "<html><h1>Update Complete!</h1></html>", "/complete.gif"));
        registerState(new UpdaterState("error", "<html><h1>Error Occurred!</h1></html>", "/error.gif"));
        changeState("idle");
    }

    public void registerState(UpdaterState state) {
        stateMap.put(state.getId(), state);
    }

    public void changeState(String stateId) {
        UpdaterState state = stateMap.get(stateId);
        if (state != null) {
            currentState = state;
            applyState(currentState);
        } else {
            System.err.println("State '" + stateId + "' not registered!");
        }
    }

    public UpdaterState getCurrentState() {
        return currentState;
    }

    private void applyState(UpdaterState state) {
        // Update the UI using the UpdaterFrame.
        frame.updateHeader(state.getHeaderText());
        frame.updateGif(state.getGifResource());
    }
}

/**
 * Main entry-point for the updater application.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UpdaterFrame frame = new UpdaterFrame();
            frame.setVisible(true);
        });
    }
}

/**
 * UpdaterFrame creates the main window.
 * It contains a dedicated animated GIF (as a GUI element), a header label, a log area, and a progress bar.
 */
class UpdaterFrame extends JFrame {
    private final JTextArea logArea;
    private final JProgressBar progressBar;
    private final JLabel gifDisplayLabel;
    private final JLabel headerLabel;
    private final JLabel subHeaderLabel;
    private final JLabel statusPill;
    private final StateRecorder stateRecorder;
    private Point dragOffset;

    // Theme Manager for cycling themes.
    private final ThemeManager themeManager;

    // Theme toggle button.
    private final JButton themeToggleButton;
    private final JButton exitButton;

    public UpdaterFrame() {
        setUndecorated(true);

        setTitle("Mod Updater");
        setSize(760, 560);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(55, 69, 86), 1));

        JPanel contentPane = new JPanel(new BorderLayout(18, 18));
        contentPane.setBorder(new EmptyBorder(18, 20, 20, 20));
        setContentPane(contentPane);

        String configDirectory = new File(new File(System.getProperty("user.dir")).getParentFile(), "config").getAbsolutePath() + File.separator;
        String configFilePath = configDirectory + "didimisssomething-themes.json";
        themeManager = new ThemeManager(configFilePath);

        JPanel topPanel = new JPanel(new BorderLayout(14, 0));
        topPanel.setOpaque(false);
        installWindowDrag(topPanel);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 2));
        titlePanel.setOpaque(false);
        headerLabel = new JLabel("Mod Updater");
        headerLabel.setFont(new Font("Inter", Font.BOLD, 30));
        subHeaderLabel = new JLabel("Preparing your Minecraft client");
        subHeaderLabel.setFont(new Font("Inter", Font.PLAIN, 13));
        titlePanel.add(headerLabel);
        titlePanel.add(subHeaderLabel);
        topPanel.add(titlePanel, BorderLayout.CENTER);

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controlsPanel.setOpaque(false);

        themeToggleButton = new JButton();
        themeToggleButton.setPreferredSize(new Dimension(38, 38));
        themeToggleButton.setBorder(new EmptyBorder(8, 8, 8, 8));
        themeToggleButton.setToolTipText("Switch theme");
        updateThemeToggleButtonIcon();
        themeToggleButton.setFocusPainted(false);
        themeToggleButton.setContentAreaFilled(false);
        themeToggleButton.addActionListener(e -> {
            themeManager.toggleTheme(new File(configFilePath));
            applyThemeToEverything();
            updateThemeToggleButtonIcon();
        });
        controlsPanel.add(themeToggleButton);

        exitButton = new JButton("X");
        exitButton.setPreferredSize(new Dimension(38, 38));
        exitButton.setFont(new Font("Inter", Font.BOLD, 14));
        exitButton.setToolTipText("Close updater");
        exitButton.setContentAreaFilled(false);
        exitButton.setBorder(new EmptyBorder(8, 8, 8, 8));
        exitButton.setFocusPainted(false);
        exitButton.addActionListener(e -> System.exit(0));
        controlsPanel.add(exitButton);

        topPanel.add(controlsPanel, BorderLayout.EAST);

        contentPane.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(18, 0));
        centerPanel.setOpaque(false);

        RoundedPanel animationPanel = new RoundedPanel(18);
        animationPanel.setLayout(new BorderLayout(0, 12));
        animationPanel.setBorder(new EmptyBorder(18, 18, 18, 18));
        gifDisplayLabel = new JLabel(new ImageIcon(getClass().getResource("/idle.gif")));
        gifDisplayLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gifDisplayLabel.setPreferredSize(new Dimension(310, 310));
        animationPanel.add(gifDisplayLabel, BorderLayout.CENTER);

        statusPill = new JLabel("IDLE", SwingConstants.CENTER);
        statusPill.setFont(new Font("Inter", Font.BOLD, 12));
        statusPill.setBorder(new EmptyBorder(8, 14, 8, 14));
        animationPanel.add(statusPill, BorderLayout.SOUTH);
        centerPanel.add(animationPanel, BorderLayout.WEST);

        RoundedPanel logPanel = new RoundedPanel(18);
        logPanel.setLayout(new BorderLayout(0, 12));
        logPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel logTitle = new JLabel("Update Log");
        logTitle.setFont(new Font("Inter", Font.BOLD, 16));
        logPanel.add(logTitle, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(360, 310));
        logPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(logPanel, BorderLayout.CENTER);
        contentPane.add(centerPanel, BorderLayout.CENTER);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(100, 18));
        progressBar.setBorder(BorderFactory.createEmptyBorder());
        progressBar.setFont(new Font("Inter", Font.BOLD, 11));
        progressBar.setUI(new RoundedProgressBarUI());
        contentPane.add(progressBar, BorderLayout.SOUTH);

        stateRecorder = new StateRecorder(this);
        applyThemeToEverything();

        UpdaterWorker worker = new UpdaterWorker(this, stateRecorder);
        worker.execute();
    }

    /**
     * Updates the toggle button icon using the next theme's toggle icon from the configuration.
     */
    private void updateThemeToggleButtonIcon() {
        String iconPath = themeManager.getToggleIconPath();
        themeToggleButton.setIcon(new ImageIcon(getClass().getResource(iconPath)));
    }

    private void applyThemeToEverything() {
        themeManager.applyTheme(this.getContentPane());
        themeManager.applyTheme(this);
        themeManager.applyTheme(getRootPane());

        Color background = themeManager.getBackgroundColor();
        Color surface = themeManager.getSurfaceColor();
        Color surfaceAlt = themeManager.getSurfaceAltColor();
        Color foreground = themeManager.getForegroundColor();
        Color muted = themeManager.getMutedForegroundColor();
        Color accent = themeManager.getAccentColor();

        getContentPane().setBackground(background);
        headerLabel.setForeground(foreground);
        subHeaderLabel.setForeground(muted);
        statusPill.setForeground(background);
        statusPill.setBackground(accent);
        statusPill.setOpaque(true);

        logArea.setBackground(surfaceAlt);
        logArea.setForeground(foreground);
        logArea.setCaretColor(accent);
        progressBar.setBackground(surfaceAlt);
        progressBar.setForeground(accent);

        themeToggleButton.setBackground(surface);
        themeToggleButton.setForeground(foreground);
        exitButton.setBackground(surface);
        exitButton.setForeground(themeManager.getDangerColor());

        repaint();
    }

    public void updateHeader(String text) {
        SwingUtilities.invokeLater(() -> {
            String cleanText = text
                    .replace("<html>", "")
                    .replace("</html>", "")
                    .replace("<h1>", "")
                    .replace("</h1>", "");
            headerLabel.setText(cleanText);
            statusPill.setText(cleanText.toUpperCase(Locale.ROOT));
            subHeaderLabel.setText(getStateSubtitle(cleanText));
        });
    }

    public void updateGif(String resourcePath) {
        SwingUtilities.invokeLater(() -> {
            ImageIcon icon = new ImageIcon(getClass().getResource(resourcePath));
            gifDisplayLabel.setIcon(icon);
        });
    }

    public void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void updateProgress(int value) {
        SwingUtilities.invokeLater(() -> progressBar.setValue(value));
    }

    public void setProgressIndeterminate(boolean indeterminate) {
        SwingUtilities.invokeLater(() -> progressBar.setIndeterminate(indeterminate));
    }

    private String getStateSubtitle(String title) {
        return switch (title) {
            case "Downloading Release..." -> "Fetching the newest modpack package";
            case "Unpacking Archive..." -> "Opening the archive and sorting files";
            case "Updating Mods..." -> "Moving mods and configs into place";
            case "Update Complete!" -> "Ready for launch, restart Minecraft";
            case "Error Occurred!" -> "Something failed, check the log";
            default -> "Preparing your Minecraft client";
        };
    }

    private void installWindowDrag(Component component) {
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point screenPoint = e.getLocationOnScreen();
                setLocation(screenPoint.x - dragOffset.x, screenPoint.y - dragOffset.y);
            }
        };
        component.addMouseListener(adapter);
        component.addMouseMotionListener(adapter);
    }
}

class RoundedPanel extends JPanel {
    private final int radius;

    public RoundedPanel(int radius) {
        this.radius = radius;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(getBackground());
        g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        g.dispose();
        super.paintComponent(graphics);
    }
}

class RoundedProgressBarUI extends BasicProgressBarUI {
    @Override
    protected void paintDeterminate(Graphics graphics, JComponent component) {
        Insets insets = progressBar.getInsets();
        int width = progressBar.getWidth() - insets.left - insets.right;
        int height = progressBar.getHeight() - insets.top - insets.bottom;
        int amountFull = getAmountFull(insets, width, height);

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(progressBar.getBackground());
        g.fillRoundRect(insets.left, insets.top, width, height, height, height);
        g.setColor(progressBar.getForeground());
        g.fillRoundRect(insets.left, insets.top, amountFull, height, height, height);
        g.dispose();

        if (progressBar.isStringPainted()) {
            paintString(graphics, insets.left, insets.top, width, height, amountFull, insets);
        }
    }

    @Override
    protected void paintIndeterminate(Graphics graphics, JComponent component) {
        paintDeterminate(graphics, component);
    }
}

/**
 * UpdaterWorker performs the update tasks in a background thread.
 * It supports both GitHub and GitLab by checking the API URL.
 * It uses the StateRecorder to update the GUI state.
 */
class UpdaterWorker extends SwingWorker<Void, String> {

    private final UpdaterFrame frame;
    private final StateRecorder stateRecorder;

    public final Config config = new Config();
    private final Map<String, Object> repositoryApiUrls;

    public UpdaterWorker(UpdaterFrame frame, StateRecorder stateRecorder) {
        this.frame = frame;
        this.stateRecorder = stateRecorder;

        log("Initializing autoupdater...");
        if (!new File(System.getProperty("user.dir")).getName().equals("didimisssomething")) {
            log("Incorrect initial folder... Move this jar to didimisssomething/ folder!");
            stateRecorder.changeState("error");
        }

        config.load();

        this.repositoryApiUrls = config.getRepositoryApiUrls();
    }

    public class Config {
        private final File CONFIG_FILE = new File(new File(System.getProperty("user.dir")).getParentFile().getAbsolutePath() + "/config/didimisssomething.json");
        private Map<String, Object> configData = new HashMap<>();

        public void load() {
            try {
                if (CONFIG_FILE.exists()) {
                    String content = FileUtils.readFileToString(CONFIG_FILE, StandardCharsets.UTF_8);
                    JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);

                    JSONObject json = (JSONObject) parser.parse(content);
                    configData.putAll(json);
                } else {
                    CONFIG_FILE.getParentFile().mkdirs();
                    save();
                }
            } catch (Exception e) {
                configData = new JSONObject();
            }
        }

        private void save() {
            try {
                FileUtils.writeStringToFile(CONFIG_FILE, JSONObject.toJSONString(configData, JSONStyle.LT_COMPRESS), StandardCharsets.UTF_8);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public <T> T get(String key, T defaultValue) {
            if (!configData.containsKey(key)) {
                configData.put(key, defaultValue);
                save();
                return defaultValue;
            }

            Object value = configData.get(key);

            if (defaultValue instanceof String) {
                return (T) String.valueOf(value);
            }

            return (T) value;
        }

        public Map<String, Object> getRepositoryApiUrls() {
            return get("repositoryApiUrls", Map.of(
                    "PLACE_YOUR_API_URL_IN_HERE", "PLACE_YOUR_GITHUB_TOKEN_IN_HERE",
                    "PLACE_YOUR_MIRROR_API_URL_IN_HERE", "PLACE_YOUR_GITLAB_TOKEN_IN_HERE"
            ));
        }
    }

    @Override
    protected Void doInBackground() throws Exception {
        File logFile = new File(System.getProperty("user.dir"), "log.txt");
        if (logFile.exists()) logFile.delete();

        File releaseFile = new File(System.getProperty("user.dir"), "/release.txt");
        if (!releaseFile.exists()) releaseFile.createNewFile();

        Map<String, String> installedVersions = new HashMap<>();

        List<String> releasesLines = Files.readAllLines(releaseFile.toPath());

        for (String line : releasesLines) {
            if (line.startsWith("latest-release;")) {
                installedVersions.put("latest-release", line.split(";")[1].trim());
            } else if (!line.isBlank() && line.contains(";")) {
                String[] parts = line.split(";");
                if (parts.length >= 2) {
                    installedVersions.put(parts[0].trim(), parts[1].trim());
                }
            }
        }

        log("Starting update process...");

        String installedMain = installedVersions.get("latest-release");

        File downloadFile = null;
        String latestRelease = null;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        var entryList = new ArrayList<>(repositoryApiUrls.entrySet());

        for (int i = 0; i < entryList.size(); i++) {
            var entry = entryList.get(i);
            String apiURL = entry.getKey();
            String token = entry.getValue().toString();

            if (!URI.create(apiURL).isAbsolute()) {
                continue;
            }

            boolean isGitLab = apiURL.contains("gitlab.com");
            boolean isLastEntry = (i == entryList.size() - 1);

            try {
                latestRelease = isGitLab
                        ? getTheLatestGitLabRelease(apiURL, token)
                        : getTheLatestRelease(apiURL, token);
                log("Latest release: " + latestRelease);

                frame.setTitle("Release " + latestRelease);

                if (installedMain != null && !installedMain.equals(latestRelease)) {
                    stateRecorder.changeState("download");
                    downloadFile = new File("downloads/latest-release.zip");

                    downloadFile.getParentFile().mkdirs();
                    log("Downloading release archive from: " + apiURL);

                    File finalDownloadFile = downloadFile;
                    Future<File> future = executor.submit(() -> isGitLab
                            ? downloadTheLatestGitLabRelease(apiURL, finalDownloadFile, token)
                            : downloadTheLatestRelease(apiURL, finalDownloadFile, token));

                    if (isLastEntry) {
                        downloadFile = future.get();
                    } else {
                        downloadFile = future.get(1, TimeUnit.MINUTES);
                    }
                    break;
                }

            } catch (TimeoutException e) {
                log("Download timed out for " + apiURL + ". Trying next source...");
            } catch (Exception e) {
                log("Error downloading from " + apiURL + ": " + e.getMessage());
            }
        }
        executor.shutdown();
        File unpackDir = new File("downloads/unpacked/latest-release/");
        if (installedMain != null && !installedMain.equals(latestRelease)) {
            if (downloadFile == null || latestRelease == null) {
                log("Something went wrong with your downloadFile or latestRelease string and they were not initialized.");
                stateRecorder.changeState("error");
                cancel(true);
                return null;
            }

            log("Download complete: " + downloadFile.getAbsolutePath());
            frame.updateProgress(30);

            frame.setTitle("Release " + latestRelease);

            stateRecorder.changeState("unpack");
            if (unpackDir.exists()) {
                FileUtils.deleteDirectory(unpackDir);
            }
            unpackDir.mkdirs();
            log("Unpacking archive...");
            if (unpackZip(downloadFile, unpackDir.getAbsolutePath())) {
                log("Archive unpacked successfully.");
            } else {
                log("Failed to unpack archive.");
                stateRecorder.changeState("error");
                cancel(true);
                return null;
            }
            frame.updateProgress(60);
        }

        File baseDir = new File(System.getProperty("user.dir")).getParentFile();
        File didimisssomethingDir = new File(System.getProperty("user.dir"));
        File modsDir = new File(baseDir, "mods");
        File prevModsDir = new File(didimisssomethingDir, "mods-previous");
        if (prevModsDir.exists()) {
            FileUtils.deleteDirectory(prevModsDir);
        }
        prevModsDir.mkdirs();
        log("Backing up current mods...");
        if (modsDir.exists()) {
            FileUtils.copyDirectory(modsDir, prevModsDir);
        } else {
            log("Mods directory not found. Skipping backup.");
        }
        frame.updateProgress(70);

        File referenceFile = new File(didimisssomethingDir, "reference.txt");
        if (referenceFile.exists()) {
            log("Deleting previously installed mods...");

            byte[] fileBytes = Files.readAllBytes(referenceFile.toPath());
            UniversalDetector detector =
                    new UniversalDetector(null);
            detector.handleData(fileBytes, 0, fileBytes.length);
            detector.dataEnd();

            String encoding = detector.getDetectedCharset();
            if (encoding == null) {
                log("Unable to detect encoding. Falling back to UTF-8");
                encoding = "UTF-8";
            }
            Charset charset = Charset.forName(encoding);
            List<String> lines = Files.readAllLines(referenceFile.toPath(), charset);

            for (String line : lines) {
                File file = new File(line.trim());
                if (file.exists()) {
                    FileUtils.forceDelete(file);
                    log("Deleted: " + file.getAbsolutePath());
                }
            }
        }
        frame.updateProgress(80);

        File additionalModsDir = new File(baseDir, "mods-additional");
        if (additionalModsDir.exists()) {
            log("Restoring additional mods...");
            FileUtils.copyDirectory(additionalModsDir, modsDir);
        }

        StringBuilder releaseInfo = new StringBuilder("latest-release;" + latestRelease);
        Map<String, List<Object>> ids = new HashMap<>();

        stateRecorder.changeState("update");
        log("Populating mods and configs from the unpacked release...");
        if (unpackDir.exists() && unpackDir.isDirectory()) {
            referenceFile.delete();
            referenceFile.createNewFile();
            Files.walk(unpackDir.toPath())
                    .filter(Files::isRegularFile)
                    .forEach(path -> {

                        File sourceFile = path.toFile();
                        Path relative = unpackDir.toPath().relativize(path);
                        File absoluteParent = relative.toFile();
                        while (absoluteParent.getParentFile() != null) {
                            absoluteParent = absoluteParent.getParentFile();
                        }
                        File destFile = new File(baseDir, relative.toString().replace(absoluteParent.getName() + "\\", ""));

                        destFile.getParentFile().mkdirs();

//                        log(String.format("%s", jarContainsMeta(sourceFile)));

                        if (config.get("useModsMetadataToUpdate", false) && sourceFile.getName().endsWith(".jar") && jarContainsMeta(sourceFile)) {
                            var json = readMetaFromJar(sourceFile);

                            try {
                                assert json != null;
                                Map<String, Object> modRepositoryApiUrls = (Map<String, Object>) json.getOrDefault("modRepositoryApiUrls", Map.of());

                                String jsonId = json.get("id").toString();
                                String installedVersion = installedVersions.get(jsonId);

                                File modDownloadFile = null;
                                String modLatestRelease = null;
                                var modEntryList = new ArrayList<>(modRepositoryApiUrls.entrySet());

                                for (Map.Entry<String, Object> entry : modEntryList) {
                                    String apiURL = entry.getKey();
                                    String token = entry.getValue().toString();
                                    boolean isGitLab = apiURL.contains("gitlab.com");

                                    modLatestRelease = isGitLab
                                            ? getTheLatestGitLabRelease(apiURL, token)
                                            : getTheLatestRelease(apiURL, token);
                                    log(json.get("id") + " latest release: " + modLatestRelease);

                                    ids.put(jsonId, List.of(modLatestRelease, apiURL, token));
                                }

                                modDownloadFile = new File(String.format("downloads/%s", json.get("id")));

                                File dir = modDownloadFile;

                                File[] jars = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".jar"));

                                if (jars != null && jars.length > 0) {
                                    modDownloadFile = jars[0];
                                } else {
                                    log("No .jar found in " + dir.getAbsolutePath());
                                }

                                if (installedVersion != null && installedVersion.equals(modLatestRelease)) {
                                    log(jsonId + " is up to date. Skipping download. " + modDownloadFile);
                                } else {
                                    log(jsonId + " changed. Downloading new version...");

                                    ExecutorService modExecutor = Executors.newSingleThreadExecutor();

                                    for (int i = 0; i < modEntryList.size(); i++) {
                                        var entry = modEntryList.get(i);

                                        String apiURL = entry.getKey();
                                        String token = entry.getValue().toString();

                                        if (!URI.create(apiURL).isAbsolute()) {
                                            continue;
                                        }
                                        boolean isGitLab = apiURL.contains("gitlab.com");
                                        boolean isLastEntry = (i == modEntryList.size() - 1);

                                        try {
                                            stateRecorder.changeState("download");

                                            modDownloadFile = new File(String.format("downloads/%s", json.get("id")));
                                            modDownloadFile.getParentFile().mkdirs();
                                            log(String.format("Downloading %s archive from: ", json.get("id")) + apiURL);

                                            File finalDownloadFile = modDownloadFile;
                                            Future<File> future = modExecutor.submit(() -> isGitLab
                                                    ? downloadTheLatestGitLabReleaseAssets(apiURL, finalDownloadFile, token)
                                                    : downloadTheLatestReleaseAssets(apiURL, finalDownloadFile, token));

                                            if (isLastEntry) {
                                                modDownloadFile = future.get();
                                            } else {
                                                modDownloadFile = future.get(1, TimeUnit.MINUTES);
                                            }
                                            break;

                                        } catch (TimeoutException e) {
                                            log("Download timed out for " + apiURL + ". Trying next source...");
                                        } catch (Exception e) {
                                            log("Error downloading from " + apiURL + ": " + e.getMessage());
                                        }
                                    }
                                    modExecutor.shutdown();
                                }

                                sourceFile = modDownloadFile;

                                if (sourceFile != null) {
                                    destFile = new File(baseDir, "mods/" + sourceFile.getName());

                                    destFile.getParentFile().mkdirs();
                                }

                                assert modDownloadFile != null;
                                modDownloadFile.getParentFile().delete();
                            }
                            catch (ClassCastException | AssertionError e) {
                                log(e.getMessage());
                            }
                        }

                        try {
                            assert sourceFile != null;
                            FileUtils.copyFile(sourceFile, destFile);

                            Files.writeString(referenceFile.toPath(),
                                    destFile.getAbsolutePath() + System.lineSeparator(),
                                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            log("Copied: " + destFile.getAbsolutePath());
                        } catch (IOException ex) {
                            log("Error copying " + destFile.getAbsolutePath() + ": " + ex.getMessage());
                        }
                    });
        } else {
            log("Unpacked release directory not found.");
        }

        for (var entry : ids.entrySet()) {
            releaseInfo.append("\n").append(entry.getKey()).append(";").append(entry.getValue().getFirst()).append(";").append(entry.getValue().get(1)).append(";").append(entry.getValue().get(2));
        }

        Files.write(releaseFile.toPath(), releaseInfo.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        log("Saved release info to: " + releaseFile.getAbsolutePath());

        frame.updateProgress(100);
        stateRecorder.changeState("complete");
        log("Update complete. Please restart the game.");

        log("Closing the autoupdater in 5 seconds!");
        Thread.sleep(5000);
        System.exit(0);

        return null;
    }

    private File findMainJar(String name, File dir) throws IOException {
        try (Stream<Path> stream = Files.walk(dir.toPath())) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> f.getName().startsWith(name))
                    .filter(f -> f.getName().endsWith(".jar"))
                    .filter(f -> !f.getName().endsWith("-sources.jar"))
                    .findFirst()
                    .orElse(null);
        }
    }

    private boolean jarContainsMeta(File jarFile) {
        try (ZipFile zip = new ZipFile(jarFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
//                log(entry.toString());
                if (!entry.isDirectory() && entry.getName().endsWith("didimisssomething.meta.json")) {
                    return true;
                }
            }
        } catch (IOException e) {
            log("Error reading jar: " + jarFile.getAbsolutePath());
        }
        return false;
    }

    private JSONObject readMetaFromJar(File jarFile) {
        JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);

        try (ZipFile zip = new ZipFile(jarFile)) {
            ZipEntry entry = zip.getEntry("didimisssomething.meta.json");

            if (entry == null) return null;

            try (InputStream is = zip.getInputStream(entry)) {
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return (JSONObject) parser.parse(json);
            }

        } catch (IOException | ParseException e) {
            log("Failed to parse meta in " + jarFile.getAbsolutePath() + ": " + e.getMessage());
            return null;
        }
    }

    public void log(String... messages) {
        File logFile = new File(System.getProperty("user.dir"), "log.txt");
        publish(logFile, messages);
    }

    public void publish(File logFile, String... messages) {
        publish(messages);

        try {
            for (String message : messages) {
                Files.writeString(logFile.toPath(),
                        message.replace(new File(System.getProperty("user.dir")).getParentFile().getAbsolutePath(), "(minecraft dir)") + System.lineSeparator(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            stateRecorder.changeState("error");
            publish(e.getMessage());
        }
    }

    @Override
    protected void process(List<String> chunks) {
        for (String message : chunks) {
            frame.appendLog(message);
        }
    }

    @Override
    protected void done() {
        try {
            get();
        } catch (InterruptedException | ExecutionException e) {
            frame.appendLog("Error during update: " + Arrays.toString(e.getStackTrace()));
            stateRecorder.changeState("error");
        }
    }

    public String getTheLatestRelease(String apiUrl, String githubToken) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
            connection.setRequestMethod("GET");
            if (githubToken != null && !githubToken.isBlank()) {
                connection.setRequestProperty("Authorization", "Bearer " + githubToken);
            }
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            InputStream inputStream = connection.getInputStream();
            String response = new String(inputStream.readAllBytes());
            JSONParser parser = new JSONParser();
            JSONObject release = (JSONObject) parser.parse(response);

            return release.getAsString("tag_name");

        } catch (IOException | ParseException e) {
            log(e.getMessage());
        }
        return null;
    }

    public File downloadTheLatestRelease(String apiUrl, File destination, String githubToken) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "token " + githubToken);
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            InputStream inputStream = connection.getInputStream();
            String response = new String(inputStream.readAllBytes());
            JSONParser parser = new JSONParser();
            JSONObject release = (JSONObject) parser.parse(response);

            String zipballUrl = release.getAsString("zipball_url");

            return downloadURL(zipballUrl, destination, githubToken);

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public File downloadTheLatestReleaseAssets(String apiUrl, File destinationDir, String githubToken) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            if (githubToken != null && !githubToken.isBlank()) {
                connection.setRequestProperty("Authorization", "Bearer " + githubToken);
            }

            int code = connection.getResponseCode();

            InputStream stream = (code >= 200 && code < 300)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String response = new String(stream.readAllBytes());

            JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
            JSONObject release = (JSONObject) parser.parse(response);

            JSONArray assets = (JSONArray) release.get("assets");

            if (assets == null || assets.isEmpty()) {
                log("No assets found in release.");
                return null;
            }

            JSONObject selectedAsset = null;

            for (Object obj : assets) {
                JSONObject asset = (JSONObject) obj;
                String name = asset.getAsString("name");

                if (name == null) continue;

                log("Found asset: " + name);

                if (name.endsWith(".jar") && !name.endsWith("-sources.jar")) {
                    selectedAsset = asset;
                    break;
                }
            }

            if (selectedAsset == null) {
                selectedAsset = (JSONObject) assets.get(0);
            }

            String downloadUrl = selectedAsset.getAsString("browser_download_url");
            String fileName = selectedAsset.getAsString("name");

            if (downloadUrl == null) {
                log("No download URL found for asset.");
                return null;
            }

            downloadUrl = downloadUrl.replace("%2B", "+");
            downloadUrl = downloadUrl.replace("%5B", "[");
            downloadUrl = downloadUrl.replace("%5D", "]");

            File destination = new File(destinationDir, fileName);
            destination.getParentFile().mkdirs();

            if (!destinationDir.exists()) {
                destinationDir.mkdirs();
            }

            log("Downloading asset: " + fileName);

            return downloadURL(downloadUrl, destination, githubToken);

        } catch (IOException | ParseException e) {
            log("Error downloading release assets: " + e.getMessage());
        }

        return null;
    }

    public String getTheLatestGitLabRelease(String apiURL, String gitlabToken) {
        try {
            URL url = URI.create(apiURL).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("PRIVATE-TOKEN", gitlabToken);
            connection.setRequestMethod("GET");

            InputStream responseStream = connection.getInputStream();
            String json = IOUtils.toString(responseStream, StandardCharsets.UTF_8);
            responseStream.close();

            JSONArray releases = (JSONArray) new JSONParser().parse(json);
            if (!releases.isEmpty()) {
                JSONObject latestRelease = (JSONObject) releases.get(0);
                return (String) latestRelease.get("tag_name");
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public File downloadTheLatestGitLabRelease(String url, File destination, String gitlabToken) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestProperty("PRIVATE-TOKEN", gitlabToken);
            connection.setRequestMethod("GET");

            InputStream inputStream = connection.getInputStream();
            String response = new String(inputStream.readAllBytes());

            String zipURL = "";
            JSONArray releases = (JSONArray) new JSONParser().parse(response);
            if (!releases.isEmpty()) {
                JSONObject latestRelease = (JSONObject) releases.getFirst();

                JSONObject assets = (JSONObject) latestRelease.get("assets");
                JSONArray sources = (JSONArray) assets.get("sources");
                for (Object sourceObject : sources) {
                    JSONObject source = (JSONObject) sourceObject;

                    if (source.get("format").equals("zip")) {
                        zipURL = (String) source.get("url");
                    }
                }
            }

            return downloadURL(zipURL, destination, gitlabToken);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public File downloadTheLatestGitLabReleaseAssets(String url, File destination, String gitlabToken) { // TODO Sorry, not implemented yet
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestProperty("PRIVATE-TOKEN", gitlabToken);
            connection.setRequestMethod("GET");

            InputStream inputStream = connection.getInputStream();
            String response = new String(inputStream.readAllBytes());

            String zipURL = "";
            JSONArray releases = (JSONArray) new JSONParser().parse(response);
            if (!releases.isEmpty()) {
                JSONObject latestRelease = (JSONObject) releases.getFirst();

                JSONObject assets = (JSONObject) latestRelease.get("assets");
                JSONArray sources = (JSONArray) assets.get("sources");
                for (Object sourceObject : sources) {
                    JSONObject source = (JSONObject) sourceObject;

                    if (source.get("format").equals("zip")) {
                        zipURL = (String) source.get("url");
                    }
                }
            }

            return downloadURL(zipURL, destination, gitlabToken);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public File downloadURL(String url, File destination, String token) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestMethod("GET");

            if (url.contains("gitlab.com")) {
                connection.setRequestProperty("PRIVATE-TOKEN", token);
            }
            else {
                connection.setRequestProperty("Authorization", "Bearer " + token);
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            }

            int contentLength = connection.getContentLength();
            if (contentLength <= 0) {
                frame.setProgressIndeterminate(true);
            }

            InputStream inputStream = connection.getInputStream();
            FileOutputStream outputStream = new FileOutputStream(destination);

            byte[] buffer = new byte[4096];
            int bytesRead;
            int totalRead = 0;
            double prevMegabytes = 0;
            String defaultTitle = frame.getTitle();
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
                double megabytes = (int) ((int) (totalRead / 1024d) / 1024d * 10d) / 10d;
                if (megabytes != prevMegabytes) {
                    log("Total megabytes read: " + megabytes);
                    prevMegabytes = megabytes;
                }
                frame.setTitle(defaultTitle + " " + (int) (totalRead / 1024d * 10d) / 10d + "kb");
                if (contentLength > 0) {
                    int progress = (int) (100L * totalRead / contentLength);
                    frame.updateProgress(Math.min(progress, 100));
                }
            }
            outputStream.close();
            inputStream.close();
            frame.setProgressIndeterminate(false);

            log("Downloaded to: ", destination.getAbsolutePath());
            return destination;
        } catch (IOException e) {
            log(String.format("Download failed: %s", Arrays.toString(e.getStackTrace())));
        }
        return null;
    }

    public static boolean unpackZip(File file, String outputDir) {
        try (ZipFile zipFile = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File entryDestination = new File(outputDir,  entry.getName());
                if (entry.isDirectory()) {
                    entryDestination.mkdirs();
                } else {
                    entryDestination.getParentFile().mkdirs();
                    try (InputStream in = zipFile.getInputStream(entry);
                         OutputStream out = new FileOutputStream(entryDestination)) {
                        IOUtils.copy(in, out);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}

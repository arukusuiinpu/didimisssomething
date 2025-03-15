package norivensuu.didimisssomething;

import net.fabricmc.api.ModInitializer;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

public class DidIMissSomething implements ModInitializer {
	public static final String MOD_ID = "didimisssomething";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		LOGGER.info("Haiiii my little meow meows");

        downloadURL("https://disk.yandex.ru/i/32uxyfj8_iDhwQ.jpg", "niko.jpg");
    }

    public static void downloadURL(String url, String saveLocation) {
        try {
            URL fileUrl = URI.create(url).toURL();
            File destination = new File(saveLocation);

            FileUtils.copyURLToFile(fileUrl, destination);

            System.out.println("Download complete: " + destination.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Download failed: " + e.getMessage());
        }
    }
}
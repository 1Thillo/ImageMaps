package net.craftcitizen.imagemaps;

import net.craftcitizen.imagemaps.util.MessageLevel;
import net.craftcitizen.imagemaps.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;

public class ImageMapDownloadCommand extends ImageMapSubCommand {

    public ImageMapDownloadCommand(ImageMaps plugin) {
        super("imagemaps.download", plugin, true);
    }

    @Override
    protected void execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendMessage(sender, MessageLevel.WARNING,
                                    "You must specify a file name and a download link.");
            return;
        }

        String filename = args[1];
        String url = args[2];

        if (filename.contains("/") || filename.contains("\\") || filename.contains(":")) {
            MessageUtil.sendMessage(sender, MessageLevel.WARNING, "Filename contains illegal character.");
            return;
        }

        getPlugin().getServer().getAsyncScheduler().runNow(getPlugin(), task -> download(sender, url, filename));
    }

    private void download(CommandSender sender, String input, String filename) {
        try {
            URL srcURL = new URI(input).toURL();

            if (!srcURL.getProtocol().startsWith("http")) {
                MessageUtil.sendMessage(sender, MessageLevel.WARNING, "Download URL is not valid.");
                return;
            }

            URLConnection connection = srcURL.openConnection();

            if (!(connection instanceof HttpURLConnection http)) {
                MessageUtil.sendMessage(sender, MessageLevel.WARNING, "Download URL is not valid.");
                return;
            }

            http.setRequestProperty("User-Agent", "ImageMaps/2");

            if (http.getResponseCode() != 200) {
                MessageUtil.sendMessage(sender, MessageLevel.WARNING,
                                        String.format("Download failed, HTTP Error code %d.",
                                                      http.getResponseCode()));
                return;
            }

            String mimeType = http.getHeaderField("Content-type");

            if (mimeType == null || !mimeType.startsWith("image/")) {
                MessageUtil.sendMessage(sender, MessageLevel.WARNING,
                                        String.format("Download is a %s file, not an image.", mimeType));
                return;
            }

            try (InputStream str = http.getInputStream()) {
                BufferedImage image = ImageIO.read(str);

                if (image == null) {
                    MessageUtil.sendMessage(sender, MessageLevel.WARNING, "Downloaded file is not an image!");
                    return;
                }

                File outFile = new File(getPlugin().getImagesDir(), filename);
                boolean fileExisted = outFile.exists();
                ImageIO.write(image, "PNG", outFile);
                image.flush();

                if (fileExisted) {
                    MessageUtil.sendMessage(sender, MessageLevel.WARNING, "File already exists, overwriting!");
                    getPlugin().getServer().getGlobalRegionScheduler()
                               .run(getPlugin(), task -> getPlugin().reloadImage(filename));
                }
            }
            catch (IllegalArgumentException e) {
                MessageUtil.sendMessage(sender, MessageLevel.WARNING, "Received no data");
                return;
            }

            MessageUtil.sendMessage(sender, MessageLevel.NORMAL, "Download complete.");
        }
        catch (URISyntaxException | IllegalArgumentException e) {
            MessageUtil.sendMessage(sender, MessageLevel.WARNING, "Malformatted URL");
        }
        catch (IOException e) {
            MessageUtil.sendMessage(sender, MessageLevel.ERROR, "An IO Exception happened, see server log");
            getPlugin().getLogger().log(Level.SEVERE, "Failed to download " + filename, e);
        }
    }

    @Override
    public void help(CommandSender sender) {
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL, "Downloads an image from an URL.");
        MessageUtil.sendMessage(sender, MessageLevel.INFO, "Usage: /imagemap download <filename> <sourceURL>");
    }
}

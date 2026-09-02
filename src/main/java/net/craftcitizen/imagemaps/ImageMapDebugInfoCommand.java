package net.craftcitizen.imagemaps;

import net.craftcitizen.imagemaps.util.MessageLevel;
import net.craftcitizen.imagemaps.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import javax.imageio.ImageIO;

public class ImageMapDebugInfoCommand extends ImageMapSubCommand {

    public ImageMapDebugInfoCommand(ImageMaps plugin) {
        super("imagemaps.admin", plugin, true);
    }

    @Override
    protected void execute(CommandSender sender, Command cmd, String label, String[] args) {
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                "ImageMaps Version " + getPlugin().getPluginMeta().getVersion());
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL, "Server: " + getPlugin().getServer().getVersion());
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL, "OS: " + System.getProperty("os.name"));
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL, "Java: " + System.getProperty("java.version"));
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL, "ImageIO Params:");
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                "Formats: " + String.join(", ", ImageIO.getReaderFormatNames()));
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                "Suffixes: " + String.join(", ", ImageIO.getReaderFileSuffixes()));
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                "MIME: " + String.join(", ", ImageIO.getReaderMIMETypes()));
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL, "Uses Cache: " + ImageIO.getUseCache());
    }

    @Override
    public void help(CommandSender sender) {
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL, "Prints some debug output.");
    }
}

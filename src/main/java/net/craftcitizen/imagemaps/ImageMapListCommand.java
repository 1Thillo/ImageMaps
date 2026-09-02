package net.craftcitizen.imagemaps;

import net.craftcitizen.imagemaps.util.MessageLevel;
import net.craftcitizen.imagemaps.util.MessageUtil;
import net.craftcitizen.imagemaps.util.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ImageMapListCommand extends ImageMapSubCommand {

    public ImageMapListCommand(ImageMaps plugin) {
        super("imagemaps.list", plugin, true);
    }

    @Override
    protected void execute(CommandSender sender, Command cmd, String label, String[] args) {
        String[] fileList = getPlugin().listImages();
        long page = args.length >= 2 ? Utils.parseIntegerOrDefault(args[1], 1) - 1 : 0;
        int numPages = Math.max(1, (int) Math.ceil((double) fileList.length / Utils.ELEMENTS_PER_PAGE));

        MessageUtil.sendMessage(sender, MessageLevel.INFO,
                                String.format("## Image List Page %d of %d ##", page + 1, numPages));

        boolean even = false;

        for (String filename : Utils.paginate(fileList, page)) {
            Component message = Component.text(filename, even ? NamedTextColor.GRAY : NamedTextColor.WHITE)
                                         .append(action(" [Info]", NamedTextColor.GOLD, "info", filename))
                                         .append(action(" [Reload]", NamedTextColor.GOLD, "reload", filename))
                                         .append(action(" [Place]", NamedTextColor.GOLD, "place", filename))
                                         .append(action(" [Delete]", NamedTextColor.RED, "delete", filename));

            MessageUtil.sendMessage(sender, MessageLevel.NORMAL, message);
            even = !even;
        }

        Component navigation = Component.text(String.format("<< Page %d", Math.max(page, 1)))
                                        .clickEvent(ClickEvent.runCommand("/imagemap list " + Math.max(page, 1)))
                                        .append(Component.text(" | "))
                                        .append(Component.text(String.format("Page %d >>",
                                                                             Math.min(page + 2, numPages)))
                                                         .clickEvent(ClickEvent.runCommand("/imagemap list "
                                                                                           + Math.min(page + 2,
                                                                                                      numPages))));

        MessageUtil.sendMessage(sender, MessageLevel.INFO, navigation);
    }

    static Component action(String label, NamedTextColor color, String subCommand, String filename) {
        return Component.text(label, color)
                        .clickEvent(ClickEvent.runCommand(String.format("/imagemap %s \"%s\"", subCommand, filename)));
    }

    @Override
    public void help(CommandSender sender) {
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL, "Lists all files in the images folder.");
        MessageUtil.sendMessage(sender, MessageLevel.INFO, "Usage: /imagemap list [page]");
    }
}

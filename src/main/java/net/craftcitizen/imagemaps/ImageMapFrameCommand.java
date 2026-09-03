package net.craftcitizen.imagemaps;

import net.craftcitizen.imagemaps.util.MessageLevel;
import net.craftcitizen.imagemaps.util.MessageUtil;
import net.craftcitizen.imagemaps.util.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Changes the item frame properties of the image a player is looking at.
 * <p>
 * The wooden hoe cannot do this reliably: a plain right click on a frame is answered by the client with an
 * interaction on the block behind it, so the server never learns about it, and a frame that has been set to fixed
 * refuses every interaction from then on, which makes it impossible to undo. A command has neither problem, and it
 * can change all frames of an image at once instead of one at a time.
 */
public class ImageMapFrameCommand extends ImageMapSubCommand {
    private static final List<String> PROPERTIES = Arrays.asList("invisible", "fixed", "rotatable");
    private static final List<String> VALUES = Arrays.asList("true", "false");
    private static final double REACH = 6.0D;

    public ImageMapFrameCommand(ImageMaps plugin) {
        super("imagemaps.frame", plugin, false);
    }

    @Override
    protected void execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 3) {
            help(sender);
            return;
        }

        String property = args[1].toLowerCase(Locale.ROOT);

        if (!PROPERTIES.contains(property)) {
            MessageUtil.sendMessage(sender, MessageLevel.WARNING,
                                    "Unknown property, use invisible, fixed or rotatable.");
            return;
        }

        String permission = "imagemaps.frame." + property;

        if (!sender.hasPermission(permission)) {
            MessageUtil.sendMessage(sender, MessageLevel.WARNING,
                                    "You do not have permission to change this property.");
            return;
        }

        boolean value = Boolean.parseBoolean(args[2]);
        Player player = (Player) sender;
        RayTraceResult hit = player.rayTraceEntities((int) REACH);

        if (hit == null || !(hit.getHitEntity() instanceof ItemFrame frame)) {
            MessageUtil.sendMessage(sender, MessageLevel.WARNING, "You are not looking at an item frame.");
            return;
        }

        Collection<ItemFrame> frames = getPlugin().getImageFrames(frame);

        for (ItemFrame target : frames)
            switch (property) {
                case "fixed" -> target.setFixed(value);
                case "rotatable" -> getPlugin().setRotatable(target, value);
                default -> target.setVisible(!value);
            }

        MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                String.format("Set %s to %s on %d frame(s).", property, value, frames.size()));
    }

    @Override
    public void help(CommandSender sender) {
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                "Changes the item frames of the image you are looking at.");
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                "Applies to every frame of that image, not just the one you aim at.");
        MessageUtil.sendMessage(sender, MessageLevel.INFO,
                                "Usage: /imagemap frame <invisible|fixed|rotatable> <true|false>");
    }

    @Override
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2)
            return Utils.getMatches(args[1], PROPERTIES);
        if (args.length == 3)
            return Utils.getMatches(args[2], VALUES);

        return Collections.emptyList();
    }
}

package net.craftcitizen.imagemaps.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/**
 * Sends prefixed, colour coded messages to a {@link CommandSender}.
 * <p>
 * Replaces the CLCore/BungeeCord based messaging of previous versions, the BungeeCord chat API no longer
 * ships with the Paper API.
 */
public final class MessageUtil {
    private static final Component PREFIX = Component.text("[", NamedTextColor.GRAY)
                                                     .append(Component.text("ImageMaps", NamedTextColor.AQUA))
                                                     .append(Component.text("] ", NamedTextColor.GRAY));

    private MessageUtil() {
    }

    public static void sendMessage(CommandSender sender, MessageLevel level, String message) {
        sendMessage(sender, level, Component.text(message));
    }

    public static void sendMessage(CommandSender sender, MessageLevel level, Component message) {
        sender.sendMessage(PREFIX.append(message.colorIfAbsent(level.getColor())));
    }
}

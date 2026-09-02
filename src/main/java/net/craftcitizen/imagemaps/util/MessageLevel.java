package net.craftcitizen.imagemaps.util;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum MessageLevel {
    NORMAL(NamedTextColor.GRAY),
    INFO(NamedTextColor.DARK_AQUA),
    WARNING(NamedTextColor.YELLOW),
    ERROR(NamedTextColor.RED),
    SEVERE(NamedTextColor.DARK_RED);

    private final TextColor color;

    MessageLevel(TextColor color) {
        this.color = color;
    }

    public TextColor getColor() {
        return color;
    }
}

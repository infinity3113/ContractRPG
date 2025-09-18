package com.infinity3113.contractrpg.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class MessageUtils {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();

    public static String parse(String message) {
        if (message == null) return "";
        Component component = miniMessage.deserialize(message);
        return legacySerializer.serialize(component);
    }

    public static void sendMessage(Player player, String message) {
        player.sendMessage(parse(message));
    }

    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null) return;
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(parse(message)));
    }
}
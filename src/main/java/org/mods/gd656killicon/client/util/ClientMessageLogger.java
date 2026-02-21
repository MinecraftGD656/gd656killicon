package org.mods.gd656killicon.client.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientMessageLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("gd656killicon-client");
    private static final MutableComponent PREFIX = Component.translatable("gd656killicon.prefix").withStyle(ChatFormatting.GOLD);

    public static void info(String key, Object... args) {
        LOGGER.info(Component.translatable(key, args).getString());
    }

    public static void warn(String key, Object... args) {
        LOGGER.warn(Component.translatable(key, args).getString());
    }

    public static void error(String key, Object... args) {
        LOGGER.error(Component.translatable(key, args).getString());
    }

    public static void success(String key, Object... args) {
        LOGGER.info(Component.translatable(key, args).getString());
    }

    public static void chatInfo(String key, Object... args) {
        logToChat(key, ChatFormatting.AQUA, args);
    }

    public static void chatWarn(String key, Object... args) {
        logToChat(key, ChatFormatting.YELLOW, args);
    }

    public static void chatError(String key, Object... args) {
        logToChat(key, ChatFormatting.RED, args);
    }

    public static void chatSuccess(String key, Object... args) {
        logToChat(key, ChatFormatting.GREEN, args);
    }

    public static void chatLiteral(String message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(PREFIX.copy().append(Component.literal(message).withStyle(ChatFormatting.AQUA)));
        }
    }

    public static void chatEmpty() {
        logToChat("", ChatFormatting.WHITE);
    }

    private static void logToChat(String key, ChatFormatting color, Object... args) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(PREFIX.copy().append(Component.translatable(key, args).withStyle(color)));
        }
    }
}

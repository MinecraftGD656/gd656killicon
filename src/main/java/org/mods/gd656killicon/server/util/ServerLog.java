package org.mods.gd656killicon.server.util;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.mods.gd656killicon.Gd656killicon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerLog {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gd656killicon.MODID);
    private static final String PREFIX_KEY = "gd656killicon.server.prefix";

    public static void info(String message, Object... args) {
        LOGGER.info(String.format(message, args));
    }

    public static void error(String message, Object... args) {
        LOGGER.error(String.format(message, args));
    }

    public static void sendSuccess(CommandSourceStack source, String key, Object... args) {
        MutableComponent prefix = Component.translatable(PREFIX_KEY).withStyle(ChatFormatting.YELLOW);
        MutableComponent message = Component.translatable(key, args).withStyle(ChatFormatting.GREEN);
        source.sendSuccess(() -> prefix.append(message), true);
    }

    public static void sendError(CommandSourceStack source, String key, Object... args) {
        MutableComponent prefix = Component.translatable(PREFIX_KEY).withStyle(ChatFormatting.YELLOW);
        MutableComponent message = Component.translatable(key, args).withStyle(ChatFormatting.RED);
        source.sendFailure(prefix.append(message));
    }
}

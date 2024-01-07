package me.hypherionmc.mmode.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class FormattingUtils {

    public static MutableComponent format(String value) {
        return Component.translatable(convertFormattingCodes(value));
    }

    private static String convertFormattingCodes(String input) {
        return input.replaceAll("§([0-9a-fklmnor])", "\u00A7$1");
    }

}

package com.quattage.experimental_tables.util;

import com.quattage.experimental_tables.ExperimentalTables;

import net.minecraft.text.Text;

public class util {
    public static Text newKey(String key) {
        return Text.translatable(ExperimentalTables.MOD_ID + "." + key);
    }
}

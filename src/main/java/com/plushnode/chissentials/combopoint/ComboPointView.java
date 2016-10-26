package com.plushnode.chissentials.combopoint;

import org.bukkit.ChatColor;

import java.util.Collections;

public class ComboPointView {
    static final ChatColor ComboColor = ChatColor.GOLD;
    static final String ComboString = "- ";

    static String formatComboMessage(int amount) {
        return ComboColor + String.join("", Collections.nCopies(amount, ComboString));
    }
}

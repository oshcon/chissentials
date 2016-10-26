package com.plushnode.chissentials.ability;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface SwingDamageAbility {
    double getSwingDamage(Player player, Entity target);
}

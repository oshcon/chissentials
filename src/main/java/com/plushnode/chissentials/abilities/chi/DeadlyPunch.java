package com.plushnode.chissentials.abilities.chi;

import com.plushnode.chissentials.ChissentialsPlugin;
import com.plushnode.chissentials.ability.SwingDamageAbility;
import com.plushnode.chissentials.combopoint.ComboPointManager;
import com.plushnode.chissentials.config.Configurable;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;


public class DeadlyPunch extends ChiAbility implements AddonAbility, SwingDamageAbility {
    private static final long DEFAULT_COOLDOWN = 1500;
    private static final double DEFAULT_MAX_DAMAGE = 8.0;

    private static boolean enabled = true;
    private static long cooldown = DEFAULT_COOLDOWN;
    private static double maxDamage = DEFAULT_MAX_DAMAGE;

    public DeadlyPunch(Player player) {
        super(player);

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return;

        int comboPoints = ComboPointManager.get().getComboPoints(player);
        if (comboPoints <= 0) return;

        bPlayer.addCooldown(this);
    }

    @Override
    public void progress() {

    }

    @Override
    public void remove() {
        super.remove();
    }

    @Override
    public double getSwingDamage(Player player, Entity target) {
        int comboPoints = ComboPointManager.get().spendAllComboPoints(player);
        if (comboPoints <= 0) return 0.0;

        return ComboPointManager.getComboPercent(comboPoints) * maxDamage;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public String getName() {
        return "DeadlyPunch";
    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public void load() {

    }

    @Override
    public void stop() {

    }

    @Override
    public String getAuthor() {
        return ChissentialsPlugin.developer;
    }

    @Override
    public String getVersion() {
        return ChissentialsPlugin.version;
    }

    public static class Config extends Configurable {
        public Config(ChissentialsPlugin plugin) {
            super(plugin);

            onConfigReload();
        }

        @Override
        public void onConfigReload() {
            enabled = this.config.getBoolean("Chi.DeadlyPunch.Enabled", true);
            cooldown = this.config.getLong("Chi.DeadlyPunch.Cooldown", DEFAULT_COOLDOWN);
            maxDamage = this.config.getDouble("Chi.DeadlyPunch.MaxDamage", DEFAULT_MAX_DAMAGE);
        }
    }
}

package com.plushnode.chissentials.abilities.chi;

import com.plushnode.chissentials.ChissentialsPlugin;
import com.plushnode.chissentials.ability.SwingDamageAbility;
import com.plushnode.chissentials.combopoint.ComboPointManager;
import com.plushnode.chissentials.config.Configurable;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.util.ActionBar;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class KickPunch extends ChiAbility implements AddonAbility, SwingDamageAbility {
    private static final long DEFAULT_COOLDOWN = 6000;
    private static final double DEFAULT_DAMAGE = 3.0;
    private static final int DEFAULT_COMBO_GEN = 1;

    private static final long Duration = 150;

    private static boolean enabled = true;
    private static long cooldown = DEFAULT_COOLDOWN;
    private static double damage = DEFAULT_DAMAGE;
    private static int comboGeneration = DEFAULT_COMBO_GEN;

    private ArmorStand vehicle;
    private boolean isSprinting;

    public KickPunch(Player player) {
        super(player);

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return;

        bPlayer.addCooldown(this);

        Location vehicleLocation = player.getLocation().clone().add(0, 0.5, 0);

        vehicle = player.getWorld().spawn(vehicleLocation, ArmorStand.class);
        vehicle.setBasePlate(false);
        vehicle.setVisible(false);
        vehicle.setGravity(false);
        vehicle.getLocation().setDirection(player.getLocation().getDirection().clone());
        vehicle.setMarker(true);
        vehicle.setPassenger(player);

        this.isSprinting = player.isSprinting();

        ComboPointManager.get().addComboPoints(player, comboGeneration);

        this.start();
    }

    @Override
    public void progress() {
        ActionBar.sendActionBar("", player);
        if (System.currentTimeMillis() >= this.getStartTime() + Duration || player.isDead() || !player.isOnline()) {
            Entity passenger = vehicle.getPassenger();

            if (passenger != null)
                passenger.leaveVehicle();

            vehicle.remove();

            if (!player.isDead() && player.isOnline()) {
                player.setSprinting(isSprinting);
            }

            remove();
        }
    }

    public double getSwingDamage() {
        return damage;
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
        return "KickPunch";
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
            enabled = this.config.getBoolean("Chi.KickPunch.Enabled", true);
            cooldown = this.config.getLong("Chi.KickPunch.Cooldown", DEFAULT_COOLDOWN);
            damage = this.config.getDouble("Chi.KickPunch.Damage", DEFAULT_DAMAGE);
            comboGeneration = this.config.getInt("Chi.KickPunch.ComboPointsGenerated", DEFAULT_COMBO_GEN);
        }
    }
}

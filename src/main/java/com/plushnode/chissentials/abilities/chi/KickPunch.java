package com.plushnode.chissentials.abilities.chi;

import com.plushnode.chissentials.ChissentialsPlugin;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.util.ActionBar;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class KickPunch extends ChiAbility implements AddonAbility {
    private static final long Duration = 150;
    private ArmorStand vehicle;

    public KickPunch(Player player) {
        super(player);

        Location vehicleLocation = player.getLocation().clone().add(0, 0.5, 0);

        vehicle = player.getWorld().spawn(vehicleLocation, ArmorStand.class);
        vehicle.setBasePlate(false);
        vehicle.setVisible(false);
        vehicle.setGravity(false);
        vehicle.getLocation().setDirection(player.getLocation().getDirection().clone());
        vehicle.setMarker(true);
        vehicle.setPassenger(player);

        this.start();
    }

    @Override
    public void progress() {
        ActionBar.sendActionBar("", player);
        if (System.currentTimeMillis() >= this.startTime + Duration) {
            Entity passenger = vehicle.getPassenger();
            if (passenger != null)
                passenger.leaveVehicle();
            vehicle.remove();
            remove();
        }
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
        return 0;
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
}

package com.plushnode.chissentials.abilities.chi;

import com.plushnode.chissentials.ChissentialsPlugin;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class LegSweep extends ChiAbility implements AddonAbility {
    private static final long SweepDuration = 500;
    private static final double Radius = 3.0;

    private static final int UpdateCount = 12;
    private static final long UpdateDelay = SweepDuration / UpdateCount;

    private double theta = 0;
    private double yaw;
    private long lastUpdate = 0;

    public LegSweep(Player player) {
        super(player);

        yaw = player.getLocation().getYaw();

        this.start();
    }

    @Override
    public void progress() {
        long time = System.currentTimeMillis();

        if (time >= lastUpdate + UpdateDelay) {
            for (double range = 0; range < 1; range += 0.3) {
                double angle = theta + Math.toRadians(yaw);
                double phi = Math.sin(angle);

                Vector offset = new Vector(Math.cos(angle), (phi + 1) / 8, Math.sin(angle)).multiply(Radius * range);

                Location current = player.getLocation().clone().add(offset);

                ParticleEffect.CLOUD.display(0.0f, 0.0f, 0.0f, 0.0f, 3, current, ChissentialsPlugin.PARTICLE_RANGE);
            }

            this.theta += (Math.PI * 2) / UpdateCount;
            this.lastUpdate = time;
        }

        if (this.theta >= Math.PI * 2) {
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
        return "LegSweep";
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

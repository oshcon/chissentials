package com.plushnode.chissentials.abilities.chi;

import com.plushnode.chissentials.ChissentialsPlugin;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;

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

        yaw = Math.toRadians(player.getLocation().getYaw());

        this.start();
    }

    @Override
    public void progress() {
        long time = System.currentTimeMillis();

        if (time >= lastUpdate + UpdateDelay) {
            double angle = theta + yaw;
            double phi = Math.sin(angle);

            angle = normalizeAngle(angle);

            for (double range = 0; range < 1; range += 0.3) {
                Location current = getAngleLocation(angle, Radius * range).add(0, (phi + 1) / 8, 0);

                ParticleEffect.CLOUD.display(0.0f, 0.0f, 0.0f, 0.0f, 3, current, ChissentialsPlugin.PARTICLE_RANGE);
            }

            Collection<Entity> entities = player.getWorld().getNearbyEntities(player.getLocation(), Radius, 1.5, Radius);

            for (Entity entity : entities) {
                if (entity == player) continue;
                if (!(entity instanceof LivingEntity)) continue;

                Vector entityPos = entity.getLocation().toVector().setY(0);
                Vector playerPos = player.getLocation().toVector().setY(0);

                if (entityPos.distanceSquared(playerPos) > Radius * Radius) continue;

                Vector toEntity = entityPos.subtract(playerPos);
                double entityAngle = Math.atan2(toEntity.getZ(), toEntity.getX());
                entityAngle = normalizeAngle(entityAngle);

                if ((entityAngle >= angle) && (entityAngle <= angle + (Math.PI * 2) / UpdateCount)) {
                    // todo: stun here instead of damage
                    ((LivingEntity)entity).damage(30);
                }
            }

            this.theta += (Math.PI * 2) / UpdateCount;
            this.lastUpdate = time;
        }

        if (this.theta >= Math.PI * 2) {
            remove();
        }
    }

    private double normalizeAngle(double angle) {
        while (angle < 0) angle += Math.PI * 2;
        while (angle > Math.PI * 2) angle -= Math.PI * 2;
        return angle;
    }

    private Location getAngleLocation(double angle, double radius) {
        Vector offset = new Vector(Math.cos(angle), 0, Math.sin(angle)).multiply(radius);
        return player.getLocation().clone().add(offset);
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

package com.plushnode.chissentials.abilities.chi;

import com.plushnode.chissentials.ChissentialsPlugin;
import com.plushnode.chissentials.StunManager;
import com.plushnode.chissentials.config.Configurable;
import com.projectkorra.projectkorra.BendingPlayer;
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
    private static final long DEFAULT_COOLDOWN = 12000;
    private static final long DEFAULT_STUN_DURATION = 1000;
    private static final long DEFAULT_SWEEP_DURATION = 500;
    private static final double DEFAULT_SWEEP_RADIUS = 3.0;
    private static final int UpdateCount = 12;

    private static boolean enabled = true;
    private static long cooldown = DEFAULT_COOLDOWN;
    private static long stunDuration = DEFAULT_STUN_DURATION;
    private static long sweepDuration = DEFAULT_SWEEP_DURATION;
    private static double radius = DEFAULT_SWEEP_RADIUS;
    private static long updateDelay = sweepDuration / UpdateCount;

    private double theta = 0;
    private double yaw;
    private long lastUpdate = 0;

    public LegSweep(Player player) {
        super(player);

        yaw = Math.toRadians(player.getLocation().getYaw());

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return;

        bPlayer.addCooldown(this);

        this.start();
    }

    @Override
    public void progress() {
        long time = System.currentTimeMillis();

        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (time >= lastUpdate + updateDelay) {
            double angle = theta + yaw;
            double phi = Math.sin(angle);

            angle = normalizeAngle(angle);

            for (double range = 0; range < 1; range += 0.3) {
                Location current = getAngleLocation(angle, radius * range).add(0, (phi + 1) / 8, 0);

                ParticleEffect.CLOUD.display(0.0f, 0.0f, 0.0f, 0.0f, 3, current, ChissentialsPlugin.PARTICLE_RANGE);
            }

            Collection<Entity> entities = player.getWorld().getNearbyEntities(player.getLocation(), radius, 1.5, radius);

            for (Entity entity : entities) {
                if (entity == player) continue;
                if (!(entity instanceof LivingEntity)) continue;

                Vector entityPos = entity.getLocation().toVector().setY(0);
                Vector playerPos = player.getLocation().toVector().setY(0);

                if (entityPos.distanceSquared(playerPos) > radius * radius) continue;

                Vector toEntity = entityPos.subtract(playerPos);
                double entityAngle = Math.atan2(toEntity.getZ(), toEntity.getX());
                entityAngle = normalizeAngle(entityAngle);

                if ((entityAngle >= angle) && (entityAngle <= angle + (Math.PI * 2) / UpdateCount)) {
                    StunManager.get().stun(entity, stunDuration);
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

    public static class Config extends Configurable {
        public Config(ChissentialsPlugin plugin) {
            super(plugin);

            onConfigReload();
        }

        @Override
        public void onConfigReload() {
            enabled = this.config.getBoolean("Chi.LegSweep.Enabled", true);
            cooldown = this.config.getLong("Chi.LegSweep.Cooldown", DEFAULT_COOLDOWN);
            stunDuration = this.config.getLong("Chi.LegSweep.StunDuration", DEFAULT_STUN_DURATION);
            sweepDuration = this.config.getLong("Chi.LegSweep.SweepDuration", DEFAULT_SWEEP_DURATION);
            radius = this.config.getDouble("Chi.LegSweep.SweepRadius", DEFAULT_SWEEP_RADIUS);

            updateDelay = sweepDuration / UpdateCount;
        }
    }
}

package com.plushnode.chissentials.abilities.chi;

import com.plushnode.chissentials.ChissentialsPlugin;
import com.plushnode.chissentials.config.Configurable;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.Collection;

public class Karma extends ChiAbility implements AddonAbility, Listener {
    private static final double DEFAULT_RANGE = 30.0;
    private static final long DEFAULT_DURATION = 6000;
    private static final long DEFAULT_COOLDOWN = 40000;

    private static final long DisplayDelay = 200;

    private static boolean enabled = true;
    private static long cooldown = DEFAULT_COOLDOWN;
    private static double range = DEFAULT_RANGE;
    private static double duration = DEFAULT_DURATION;

    private LivingEntity target;
    private long lastDisplay = 0;

    public Karma(Player player) {
        super(player);

        this.target = getTargetedEntity(range, 1.5);

        if (this.target == null) return;

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return;

        bPlayer.addCooldown(this);
        ChissentialsPlugin.plugin.getServer().getPluginManager().registerEvents(this, ChissentialsPlugin.plugin);

        this.start();
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() != player) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) return;

        event.setCancelled(true);

        target.damage(event.getDamage());
    }

    @Override
    public void progress() {
        long time = System.currentTimeMillis();

        if (player.isDead() || !player.isOnline() || !target.isValid()) {
            remove();
            return;
        }

        if (time >= lastDisplay + DisplayDelay) {
            Location playerLoc = player.getLocation().clone().add(0, 2.5, 0);
            Location targetLoc = target.getLocation().clone().add(0, 2.5, 0);

            ParticleEffect.SMOKE.display(0.1f, 0.0f, 0.1f, 0.0f, 3, targetLoc, ChissentialsPlugin.PARTICLE_RANGE);
            ParticleEffect.SPELL.display(0.1f, 0.0f, 0.1f, 0.0f, 3, playerLoc, ChissentialsPlugin.PARTICLE_RANGE);

            lastDisplay = time;
        }

        if (time >= this.startTime + duration) {
            remove();
        }
    }

    private LivingEntity getTargetedEntity(double range, double selectRadius) {
        final Collection<Entity> entities = player.getWorld().getNearbyEntities(player.getLocation(), range, range, range);
        final Location origin = player.getEyeLocation();
        final Vector direction = origin.getDirection().clone().normalize();

        LivingEntity targetEntity = null;
        double longestRangeSq = (range + 1) * (range + 1);

        for (Entity entity : entities) {
            final Location loc = entity.getLocation();

            if (loc.distanceSquared(origin) > longestRangeSq) continue;
            if (GeneralMethods.getDistanceFromLine(direction, origin, loc) > selectRadius) continue;
            if (!(entity instanceof LivingEntity)) continue;
            if (entity == player) continue;
            if (loc.distanceSquared(origin.clone().add(direction)) >= loc.distanceSquared(origin.clone().add(direction.clone().multiply(-1)))) continue;

            targetEntity = (LivingEntity)entity;
            longestRangeSq = loc.distanceSquared(origin);
        }

        if (targetEntity != null) {
            if (GeneralMethods.isObstructed(origin, targetEntity.getLocation())) {
                targetEntity = null;
            }
        }

        return targetEntity;
    }

    @Override
    public void remove() {
        EntityDamageEvent.getHandlerList().unregister(this);
        super.remove();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isSneakAbility() {
        return true;
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
        return "Karma";
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
            enabled = this.config.getBoolean("Chi.Karma.Enabled", true);
            cooldown = this.config.getLong("Chi.Karma.Cooldown", DEFAULT_COOLDOWN);
            range = this.config.getDouble("Chi.Karma.Range", DEFAULT_RANGE);
            duration = this.config.getLong("Chi.Karma.Duration", DEFAULT_DURATION);
        }
    }
}

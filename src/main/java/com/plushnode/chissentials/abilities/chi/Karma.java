package com.plushnode.chissentials.abilities.chi;

import com.plushnode.chissentials.ChissentialsPlugin;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
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
    private double range = 30.0;
    private double duration = 6000;
    private LivingEntity target;

    public Karma(Player player) {
        super(player);

        this.target = getTargetedEntity(range, 3.0);

        if (this.target == null) {
            System.out.println("No target for karma");
            return;
        }

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
        System.out.println("Removing karma");
        EntityDamageEvent.getHandlerList().unregister(this);
        super.remove();
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
        return 0;
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
}

package com.plushnode.chissentials.abilities.chi;

import com.plushnode.chissentials.ChissentialsPlugin;
import com.plushnode.chissentials.combopoint.ComboPointManager;
import com.plushnode.chissentials.config.Configurable;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.event.AbilityStartEvent;
import com.projectkorra.projectkorra.event.EntityBendingDeathEvent;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.Optional;

public class Ambush extends ChiAbility implements AddonAbility, Listener {
    private static final long DEFAULT_COOLDOWN = 25000;
    private static final double DEFAULT_DAMAGE = 5.0;
    private static final int DEFAULT_COMBO_GEN = 2;
    private static final long DEFAULT_STEALTH_DURATION = 6000;
    private static final double DEFAULT_NEARBY_RANGE = 10.0;

    private static boolean enabled = true;
    private static long cooldown = DEFAULT_COOLDOWN;
    private static double damage = DEFAULT_DAMAGE;
    private static int comboGeneration = DEFAULT_COMBO_GEN;
    private static long stealthDuration = DEFAULT_STEALTH_DURATION;
    private static double nearbyRange = DEFAULT_NEARBY_RANGE;

    private boolean removeNextTick = false;
    private Location beginLocation;
    private boolean vanishParticlesDisplayed = false;

    public Ambush(Player player) {
        super(player);

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return;

        if (CoreAbility.hasAbility(player, Ambush.class)) return;
        if (nearbyRange > 0 && hasEntitiesNearby()) return;

        bPlayer.addCooldown(this);

        int duration = (int)Math.ceil((stealthDuration / 1000.0) * 20.0);

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 1));

        beginLocation = player.getLocation().clone().add(player.getLocation().getDirection());

        ChissentialsPlugin.plugin.getServer().getPluginManager().registerEvents(this, ChissentialsPlugin.plugin);

        this.start();
    }

    private boolean hasEntitiesNearby() {
        Collection<Entity> entities = player.getWorld().getNearbyEntities(player.getLocation(), nearbyRange, nearbyRange, nearbyRange);
        Optional<Entity> found = entities.stream().filter(entity -> (entity instanceof LivingEntity) && entity != player).findAny();

        return found.isPresent();
    }

    @Override
    public void progress() {
        long time = System.currentTimeMillis();

        if (!vanishParticlesDisplayed) {
            // Render vertical part of smoke
            ParticleEffect.SMOKE_LARGE.display(0.5f, 1.0f, 0.5f, 0.0f, 60, beginLocation.clone().add(0, 1, 0), ChissentialsPlugin.PARTICLE_RANGE);
            // Render wide bottom part of smoke
            ParticleEffect.SMOKE_LARGE.display(1.0f, 0.5f, 1.0f, 0.0f, 120, beginLocation, ChissentialsPlugin.PARTICLE_RANGE);

            vanishParticlesDisplayed = true;
        }

        if (time > getStartTime() + stealthDuration || removeNextTick) {
            remove();
        }
    }

    @EventHandler (priority = EventPriority.LOW)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() == this.player) {
            // Destroy ability because the player was attacked.
            remove();
            return;
        }

        if (event.getDamager() != this.player) return;

        if (!(event.getEntity() instanceof LivingEntity)) {
            remove();
            return;
        }

        event.setCancelled(true);
        LivingEntity entity = (LivingEntity)event.getEntity();

        double newHealth = Math.max(entity.getHealth() - damage, 0);

        if(newHealth <= 0 && !entity.isDead()) {
            EntityBendingDeathEvent deathEvent = new EntityBendingDeathEvent(entity, damage, this);
            Bukkit.getServer().getPluginManager().callEvent(deathEvent);
        }

        Location location = entity.getLocation();
        ParticleEffect.CRIT.display(1.0f, 1.0f, 1.0f, 0.0f, 30, location, ChissentialsPlugin.PARTICLE_RANGE);
        // Call the non-entity version so another EntityDamageByEntityEvent isn't fired.
        entity.damage(damage);

        ComboPointManager.get().addComboPoints(this.player, comboGeneration);
        remove();
    }

    @EventHandler (priority = EventPriority.LOW)
    public void onPlayerSwing(PlayerAnimationEvent event) {
        if (event.getPlayer() != this.player) return;

        event.setCancelled(true);

        // PlayerAnimationEvent and EntityDamageByEntityEvent will both happen on the same tick.
        // Scheduling the ability to be removed on the next tick will allow the other event to fire if
        // this is actually an attack.
        removeNextTick = true;
    }

    @EventHandler (priority = EventPriority.LOW)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() == this.player) {
            // Destroy ability because the player took damage.
            remove();
        }
    }

    @EventHandler (priority = EventPriority.LOW)
    public void onAbilityStart(AbilityStartEvent event) {
        if (event.getAbility() == this) return;

        if (event.getAbility().getPlayer() == this.player) {
            // Destroy this ability because the player casted while stealthing.
            remove();
        }
    }

    @Override
    public void remove() {
        player.removePotionEffect(PotionEffectType.INVISIBILITY);

        EntityDamageByEntityEvent.getHandlerList().unregister(this);
        PlayerAnimationEvent.getHandlerList().unregister(this);
        EntityDamageEvent.getHandlerList().unregister(this);
        AbilityStartEvent.getHandlerList().unregister(this);

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
        return "Ambush";
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

    @Override
    public String getDescription() {
        return ChissentialsPlugin.plugin.getConfig().getString("Abilities.Chi." + this.getName() + ".Description");
    }

    public static class Config extends Configurable {
        public Config(ChissentialsPlugin plugin) {
            super(plugin);

            onConfigReload();
        }

        @Override
        public void onConfigReload() {
            enabled = this.config.getBoolean("Abilities.Chi.Ambush.Enabled", true);
            cooldown = this.config.getLong("Abilities.Chi.Ambush.Cooldown", DEFAULT_COOLDOWN);
            damage = this.config.getDouble("Abilities.Chi.Ambush.Damage", DEFAULT_DAMAGE);
            comboGeneration = this.config.getInt("Abilities.Chi.Ambush.ComboPointsGenerated", DEFAULT_COMBO_GEN);
            stealthDuration = this.config.getLong("Abilities.Chi.Ambush.StealthDuration", DEFAULT_STEALTH_DURATION);
            nearbyRange = this.config.getDouble("Abilities.Chi.Ambush.NearbyRange", DEFAULT_NEARBY_RANGE);
        }
    }
}

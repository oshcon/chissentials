package com.plushnode.chissentials.abilities.chi;

import com.plushnode.chissentials.ChissentialsPlugin;
import com.plushnode.chissentials.combopoint.ComboPointManager;
import com.plushnode.chissentials.config.Configurable;
import com.plushnode.chissentials.util.ArrowUtil;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.event.AbilityStartEvent;
import com.projectkorra.projectkorra.event.EntityBendingDeathEvent;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class Ambush extends ChiAbility implements AddonAbility, Listener {
    private static final double SOUND_RANGE = 7;

    private static final long DEFAULT_COOLDOWN = 25000;
    private static final double DEFAULT_DAMAGE = 5.0;
    private static final int DEFAULT_COMBO_GEN = 0;
    private static final long DEFAULT_STEALTH_DURATION = 8000;
    private static final double DEFAULT_VISION_RANGE = 10.0;
    private static final double DEFAULT_VISION_ANGLE = 70.0;

    private static boolean enabled = true;
    private static long cooldown = DEFAULT_COOLDOWN;
    private static double damage = DEFAULT_DAMAGE;
    private static int comboGeneration = DEFAULT_COMBO_GEN;
    private static long stealthDuration = DEFAULT_STEALTH_DURATION;
    private static double visionRange = DEFAULT_VISION_RANGE;
    private static double visionAngle = DEFAULT_VISION_ANGLE;
    private static boolean hideSprintParticles = false;
    private static boolean hideWalkParticles = false;

    private boolean removeNextTick = false;
    private Location beginLocation;
    private ParticleRenderer renderer;

    public Ambush(Player player) {
        super(player);

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return;

        if (CoreAbility.hasAbility(player, Ambush.class)) return;
        if (visionRange > 0 && hasEntitiesNearby()) return;

        bPlayer.addCooldown(this);

        int duration = (int)Math.ceil((stealthDuration / 1000.0) * 20.0);

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 1));

        ArrowUtil.clear(player);

        beginLocation = player.getLocation().clone();

        ChissentialsPlugin.plugin.getServer().getPluginManager().registerEvents(this, ChissentialsPlugin.plugin);

        if (player.hasPermission("Chissentials.Ambush.Rainbow")) {
            this.renderer = new RainbowRenderer();
        } else {
            this.renderer = new SmokeRenderer();
        }

        this.start();
    }

    @Override
    public void progress() {
        long time = System.currentTimeMillis();

        renderer.render();

        Collection<Entity> entities = player.getWorld().getNearbyEntities(player.getLocation(), 100, 100, 100);
        for (Entity entity : entities) {
            if (entity instanceof Player) {
                Player p = (Player) entity;

                if (!hideWalkParticles || (!hideSprintParticles && player.isSprinting())) {
                    p.showPlayer(player);
                } else {
                    double distanceSq = p.getLocation().distanceSquared(player.getLocation());

                    if (distanceSq <= SOUND_RANGE * SOUND_RANGE) {
                        p.showPlayer(player);
                    } else {
                        p.hidePlayer(player);
                    }
                }
            }
        }

        if (time > getStartTime() + stealthDuration || removeNextTick) {
            remove();
        }
    }

    private boolean inEntityVisibilityCone(LivingEntity entity) {
        if (entity.getWorld() != this.player.getWorld()) {
            return false;
        }

        if (entity instanceof Player) {
            Player enemy = (Player)entity;

            GameMode mode = enemy.getGameMode();
            if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
                return false;
            }
        }

        boolean footObstruction = GeneralMethods.isObstructed(entity.getEyeLocation().clone(), this.player.getLocation().clone());
        boolean obstructed = footObstruction && GeneralMethods.isObstructed(entity.getEyeLocation().clone(), this.player.getEyeLocation().clone());

        if (obstructed) {
            return false;
        }

        Vector viewDirection = entity.getLocation().getDirection().clone();
        Vector toPlayer = this.player.getLocation().toVector().subtract(entity.getLocation().toVector());
        double angle = viewDirection.angle(toPlayer);

        return Math.toDegrees(angle) <= visionAngle;
    }

    private boolean hasEntitiesNearby() {
        Collection<Entity> entities = player.getWorld().getNearbyEntities(player.getLocation(), visionRange, visionRange, visionRange);

        for (Entity entity : entities) {
            if (entity == player) continue;
            if (!(entity instanceof LivingEntity)) continue;
            if (entity instanceof ArmorStand) continue;
            if (entity.getLocation().distanceSquared(this.player.getLocation()) > visionRange * visionRange) continue;

            if (inEntityVisibilityCone((LivingEntity)entity)) {
                return true;
            }
        }

        return false;
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

    @EventHandler (priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.isCancelled()) return;
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

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showPlayer(player);
        }

        final boolean wasSprinting = player.isSprinting();

        // There's a bug introduced in 1.11 that doesn't properly synchronize metadata with clients.
        // Flip sprinting to try to update metadata.
        new BukkitRunnable() {
            @Override
            public void run() {
                player.setSprinting(!wasSprinting);
                player.setSprinting(wasSprinting);
            }
        }.runTaskLater(ChissentialsPlugin.plugin, 1);

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
            visionRange = this.config.getDouble("Abilities.Chi.Ambush.VisionRange", DEFAULT_VISION_RANGE);
            visionAngle = this.config.getDouble("Abilities.Chi.Ambush.VisionAngle", DEFAULT_VISION_ANGLE);
            hideSprintParticles = this.config.getBoolean("Abilities.Chi.Ambush.HideSprintParticles", false);
            hideWalkParticles = this.config.getBoolean("Abilities.Chi.Ambush.HideParticles", false);

            if (visionRange == DEFAULT_VISION_RANGE) {
                // Check the old config name
                visionRange = this.config.getDouble("Abilities.Chi.Ambush.NearbyRange", DEFAULT_VISION_RANGE);
            }
        }
    }

    private interface ParticleRenderer {
        void render();
    }

    private class SmokeRenderer implements ParticleRenderer {
        private boolean displayed = false;

        @Override
        public void render() {
            if (!displayed) {
                // Render vertical part of smoke
                ParticleEffect.SMOKE_LARGE.display(0.5f, 1.0f, 0.5f, 0.0f, 60, beginLocation.clone().add(0, 1, 0), ChissentialsPlugin.PARTICLE_RANGE);
                // Render wide bottom part of smoke
                ParticleEffect.SMOKE_LARGE.display(1.0f, 0.5f, 1.0f, 0.0f, 120, beginLocation, ChissentialsPlugin.PARTICLE_RANGE);

                displayed = true;
            }
        }
    }

    private class RainbowRenderer implements ParticleRenderer {
        private int vanishParticlesDisplayed = 0;
        private long lastParticleTime = 0;
        private List<RainbowParticle> rainbowParticles = new ArrayList<>();

        @Override
        public void render() {
            long time = System.currentTimeMillis();

            if (time >= lastParticleTime + 100 && vanishParticlesDisplayed < 3) {
                if (rainbowParticles.isEmpty()) {
                    // Render top part
                    renderRandomRainbow(beginLocation.clone().add(0, 1.5, 0), 1.0f, 2.0f, 1.0f, 120);
                    // Render bottom part
                    renderRandomRainbow(beginLocation.clone().add(0, 0.5, 0), 2.5f, 0.75f, 2.5f, 240);
                }

                for (RainbowParticle particle : rainbowParticles) {
                    Location particleLocation = particle.location;
                    Color color = particle.color;

                    ParticleEffect.REDSTONE.display(color.getRed(), color.getGreen(), color.getBlue(), 0.005f, 0, particleLocation, ChissentialsPlugin.PARTICLE_RANGE);
                }

                ++vanishParticlesDisplayed;
                lastParticleTime = time;
            }
        }

        private void renderRandomRainbow(Location location, float xSpread, float ySpread, float zSpread, int count) {
            Random r = new Random();

            for (int i = 0; i < count; ++i) {
                int rgb = Color.HSBtoRGB(r.nextFloat(), 0.75f, 0.75f);
                Color color = new Color(rgb);

                Location particleLocation = location.clone().add(randomBinomial(r) * xSpread, randomBinomial(r) * ySpread, randomBinomial(r) * zSpread);

                rainbowParticles.add(new RainbowParticle(particleLocation, color));
            }
        }

        private float randomBinomial(Random random) {
            return random.nextFloat() - random.nextFloat();
        }
    }

    private static class RainbowParticle {
        Location location;
        Color color;

        RainbowParticle(Location location, Color color) {
            this.location = location;
            this.color = color;
        }
    }
}

package com.plushnode.chissentials.abilities.chi;

import com.plushnode.chissentials.ChissentialsPlugin;
import com.plushnode.chissentials.combopoint.ComboPointManager;
import com.plushnode.chissentials.config.Configurable;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.event.AbilityStartEvent;
import com.projectkorra.projectkorra.event.EntityBendingDeathEvent;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.ReflectionHandler;
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
import org.bukkit.util.Vector;

import java.lang.reflect.*;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ambush extends ChiAbility implements AddonAbility, Listener {
    private static Method getHandle = null, getDataWatcher = null, setData = null;
    private static Constructor<?> DataWatcherObjectConstructor = null;
    private static Field IntegerSerializer = null;
    private static int mcVersion = 0;

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

    private boolean removeNextTick = false;
    private Location beginLocation;
    private boolean vanishParticlesDisplayed = false;

    public Ambush(Player player) {
        super(player);

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return;

        if (CoreAbility.hasAbility(player, Ambush.class)) return;
        if (visionRange > 0 && hasEntitiesNearby()) return;

        bPlayer.addCooldown(this);

        int duration = (int)Math.ceil((stealthDuration / 1000.0) * 20.0);

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 1));
        removeArrows();

        beginLocation = player.getLocation().clone().add(player.getLocation().getDirection());

        ChissentialsPlugin.plugin.getServer().getPluginManager().registerEvents(this, ChissentialsPlugin.plugin);

        this.start();
    }

    private void removeArrows() {
        if (getHandle == null || getDataWatcher == null || setData == null) {
            Class<?> CraftEntity = null, Entity = null, DataWatcher = null;
            Class<?> DataWatcherObject = null, DataWatcherSerializer = null, DataWatcherRegistry = null;

            if (mcVersion == 0) {
                mcVersion = Integer.parseInt(ReflectionHandler.PackageType.getServerVersion().split("_")[1]);
            }

            CraftEntity = getNMSClass("org.bukkit.craftbukkit.%s.entity.CraftEntity");
            Entity = getNMSClass("net.minecraft.server.%s.Entity");
            DataWatcher = getNMSClass("net.minecraft.server.%s.DataWatcher");
            DataWatcherObject = getNMSClass("net.minecraft.server.%s.DataWatcherObject");
            DataWatcherSerializer = getNMSClass("net.minecraft.server.%s.DataWatcherSerializer");
            DataWatcherRegistry = getNMSClass("net.minecraft.server.%s.DataWatcherRegistry");

            if (CraftEntity == null || Entity == null || DataWatcher == null
                    || DataWatcherObject == null || DataWatcherSerializer == null || DataWatcherRegistry == null)
            {
                return;
            }

            try {
                getHandle = CraftEntity.getDeclaredMethod("getHandle");
                getDataWatcher = Entity.getDeclaredMethod("getDataWatcher");

                IntegerSerializer = DataWatcherRegistry.getField("b");

                setData = DataWatcher.getDeclaredMethod("set", DataWatcherObject.asSubclass(Object.class), Object.class);

                DataWatcherObjectConstructor = DataWatcherObject.getConstructor(int.class, DataWatcherSerializer.asSubclass(Object.class));
            } catch (NoSuchMethodException|NoSuchFieldException e) {
                e.printStackTrace();
            }
        }

        try {
            Object entity = getHandle.invoke(player);
            Object dataWatcher = getDataWatcher.invoke(entity);

            Object intSerializer = IntegerSerializer.get(null);

            int metaIndex = 9;
            if (mcVersion > 9) {
                metaIndex = 10;
            }
            Object watcherObject = DataWatcherObjectConstructor.newInstance(metaIndex, intSerializer);

            setData.invoke(dataWatcher, watcherObject, 0);
        } catch (IllegalAccessException|InvocationTargetException|InstantiationException e) {
            e.printStackTrace();
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

            if (visionRange == DEFAULT_VISION_RANGE) {
                // Check the old config name
                visionRange = this.config.getDouble("Abilities.Chi.Ambush.NearbyRange", DEFAULT_VISION_RANGE);
            }
        }
    }

    private static Class<?> getNMSClass(String nmsClass) {
        String version = null;

        Pattern pattern = Pattern.compile("net\\.minecraft\\.(?:server)?\\.(v(?:\\d+_)+R\\d)");
        for (Package p : Package.getPackages()) {
            String name = p.getName();
            Matcher m = pattern.matcher(name);
            if (m.matches()) {
                version = m.group(1);
            }
        }

        if (version == null) return null;

        try {
            return Class.forName(String.format(nmsClass, version));
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}

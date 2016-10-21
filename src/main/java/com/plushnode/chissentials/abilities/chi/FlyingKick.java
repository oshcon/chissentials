package com.plushnode.chissentials.abilities.chi;

import com.plushnode.chissentials.ChissentialsPlugin;
import com.plushnode.chissentials.config.Configurable;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.util.ActionBar;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.util.Vector;

import java.util.Collection;

public class FlyingKick extends ChiAbility implements AddonAbility, Listener {
    private enum State { Movement, GroundWait, ExplosionStart, ExplosionMid, ExplosionEnd }

    private static final long DEFAULT_COOLDOWN = 16000;
    private static final double DEFAULT_SPEED = 20.0;
    private static final long DEFAULT_DURATION = 2000;
    private static final double DEFAULT_EXPLOSION_RADIUS = 3.5;
    private static final long DEFAULT_GROUND_WAIT_TIME = 1000;
    private static final double DEFAULT_DAMAGE = 20.0;

    private static final long ExplosionTransitionDelay = 100;
    private static final int ExplosionParticleCount = 24;

    private static boolean enabled = true;
    private static long cooldown = DEFAULT_COOLDOWN;
    // Blocks per second
    private static double speed = DEFAULT_SPEED;
    private static long duration = DEFAULT_DURATION;
    private static double ExplosionRadius = DEFAULT_EXPLOSION_RADIUS;
    // Maximum amount of time to wait for player to hit ground after stopping movement early
    private static long PlayerGroundWaitTime = DEFAULT_GROUND_WAIT_TIME;
    private static double damage = DEFAULT_DAMAGE;

    private ArmorStand vehicle;
    private Vector direction;
    private State state = State.Movement;
    private long stateStart;
    private Location explosionLocation;
    private int startY;
    private Location lastHeightChange = null;

    public FlyingKick(Player player) {
        super(player);

        if (!getAbilities(player, FlyingKick.class).isEmpty()) {
            return;
        }

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        bPlayer.addCooldown(this);

        Location vehicleLocation = player.getLocation().clone();
        vehicle = player.getWorld().spawn(vehicleLocation, ArmorStand.class);
        vehicle.setBasePlate(false);
        vehicle.setVisible(false);
        vehicle.setGravity(true);
        vehicle.getLocation().setDirection(player.getLocation().getDirection());
        vehicle.setMarker(true);
        vehicle.setPassenger(player);

        this.direction = player.getLocation().getDirection().clone().setY(0).normalize();
        this.startY = player.getLocation().getBlockY();
        this.stateStart = this.startTime;

        ChissentialsPlugin.plugin.getServer().getPluginManager().registerEvents(this, ChissentialsPlugin.plugin);
        this.start();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerSwing(PlayerAnimationEvent event) {
        if (event.getPlayer() == this.player && state == State.Movement) {
            event.setCancelled(true);
            destroyVehicle();
            transitionState(State.GroundWait);
        }
    }

    private void renderExplosion(double radius, double y) {
        for (double theta = 0.0; theta <= Math.PI * 2; theta += ((Math.PI * 2) / ExplosionParticleCount)) {
            Vector offset = new Vector(Math.cos(theta), y, Math.sin(theta)).multiply(radius);
            Location current = this.explosionLocation.clone().add(offset);

            ParticleEffect.CLOUD.display(0.5f, 0.0f, 0.5f, 0.0f, 3, current, ChissentialsPlugin.PARTICLE_RANGE);
        }
    }

    private void transitionState(State newState) {
        long time = System.currentTimeMillis();

        this.state = newState;
        this.stateStart = time;

        switch (state) {
            case ExplosionStart:
            {
                this.explosionLocation = player.getWorld().getHighestBlockAt(player.getLocation()).getLocation().clone();

                renderExplosion(ExplosionRadius * 0.3, 0);

                Collection<Entity> nearbyEntities = player.getWorld().getNearbyEntities(player.getLocation(), ExplosionRadius + 1, 1.5, ExplosionRadius + 1);
                for (Entity entity : nearbyEntities) {
                    if (entity == player) continue;
                    if (!(entity instanceof LivingEntity)) continue;

                    if (entity.getLocation().distanceSquared(this.explosionLocation) <= ExplosionRadius * ExplosionRadius) {
                        ((LivingEntity) entity).damage(damage);
                    }
                }
            }
            break;
            case ExplosionMid:
            {
                renderExplosion(ExplosionRadius * 0.6, 0.05);
            }
            break;
            case ExplosionEnd:
            {
                renderExplosion(ExplosionRadius, 0.1);
            }
            break;
        }
    }

    @Override
    public void progress() {
        long time = System.currentTimeMillis();

        switch (state) {
            case Movement:
            {
                moveVehicle();

                if (time >= this.startTime + duration) {
                    destroy();
                }
            }
            break;
            case GroundWait:
            {
                if (player.isOnGround()) {
                    transitionState(State.ExplosionStart);
                } else {
                    if (time >= stateStart + PlayerGroundWaitTime) {
                        // Cancel the explosion if player takes too long to hit the ground.
                        destroy();
                    }
                }
            }
            break;
            case ExplosionStart:
            {
                if (time >= stateStart + ExplosionTransitionDelay) {
                    transitionState(State.ExplosionMid);
                }
            }
            break;
            case ExplosionMid:
            {
                if (time >= stateStart + ExplosionTransitionDelay) {
                    transitionState(State.ExplosionEnd);
                }
            }
            break;
            case ExplosionEnd:
            {
                if (time >= stateStart + ExplosionTransitionDelay) {
                    destroy();
                }
            }
            break;
        }
    }

    private void moveVehicle() {
        Vector delta = this.direction.clone().multiply(speed / 20.0);

        vehicle.setVelocity(delta);
        ActionBar.sendActionBar("", player);

        if (vehicle.getPassenger() != player) {
            vehicle.setPassenger(player);
        }

        Location nextLocation = vehicle.getLocation().clone().add(delta);
        Location aboveNextLocation = nextLocation.clone().add(0, 1, 0);

        if (!isPassableLocation(aboveNextLocation)) {
            vehicle.setVelocity(new Vector(0, 0, 0));
        } else if (!isPassableLocation(nextLocation)) {
            if (isPassableLocation(aboveNextLocation.clone().add(0, 1, 0)) && isPassableLocation(vehicle.getLocation().clone().add(0, 2, 0))) {
                vehicle.setVelocity(delta.clone().add(new Vector(0, 2, 0)).normalize());
                lastHeightChange = vehicle.getLocation().clone().add(delta);
            } else {
                vehicle.setVelocity(new Vector(0, 0, 0));
            }
        }

        if (vehicle.getLocation().getBlockY() > startY && isPassableLocation(vehicle.getLocation().clone().subtract(0, 1, 0))) {
            // Only fall if the player didn't immediately climb.
            if (lastHeightChange == null || lastHeightChange.distanceSquared(vehicle.getLocation()) >= 1) {
                vehicle.setVelocity(new Vector(0, -0.5, 0));
                lastHeightChange = null;
            }
        }
    }

    private boolean isPassableLocation(Location location) {
        Block block = location.getBlock();
        return block.getType() == Material.AIR || block.isLiquid();
    }

    private void destroyVehicle() {
        if (vehicle == null) return;

        Entity passenger = vehicle.getPassenger();
        if (passenger != null) {
            passenger.leaveVehicle();
        }
        vehicle.remove();
    }

    private void destroy() {
        destroyVehicle();

        PlayerAnimationEvent.getHandlerList().unregister(this);
        remove();
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
        return "FlyingKick";
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
            enabled = this.config.getBoolean("Chi.FlyingKick.Enabled", true);
            cooldown = this.config.getLong("Chi.FlyingKick.Cooldown", DEFAULT_COOLDOWN);
            speed = this.config.getDouble("Chi.FlyingKick.Speed", DEFAULT_SPEED);
            duration = this.config.getLong("Chi.FlyingKick.Duration", DEFAULT_DURATION);
            ExplosionRadius = this.config.getDouble("Chi.FlyingKick.ExplosionRadius", DEFAULT_EXPLOSION_RADIUS);
            PlayerGroundWaitTime = this.config.getLong("Chi.FlyingKick.GroundWaitTime", DEFAULT_GROUND_WAIT_TIME);
            damage = this.config.getDouble("Chi.FlyingKick.Damage", DEFAULT_DAMAGE);
        }
    }
}

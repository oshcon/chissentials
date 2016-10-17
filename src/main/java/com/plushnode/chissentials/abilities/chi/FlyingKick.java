package com.plushnode.chissentials.abilities.chi;

import com.plushnode.chissentials.ChissentialsPlugin;
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
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.util.Vector;

import java.util.Collection;

public class FlyingKick extends ChiAbility implements AddonAbility, Listener {
    private enum State { Movement, GroundWait, ExplosionStart, ExplosionMid, ExplosionEnd }

    private static final long ExplosionTransitionDelay = 100;
    private static final int ExplosionParticleCount = 24;
    private static final double speed = 1.0; // Blocks per second
    private static final long duration = 2000;
    private static final double ExplosionRadius = 3.5;
    // Maximum amount of time to wait for player to hit ground after stopping movement early
    private static final long PlayerGroundWaitTime = 1000;
    private static double damage = 20.0;

    private ArmorStand vehicle;
    private Vector direction;
    private State state = State.Movement;
    private long stateStart;
    private Location explosionLocation;
    private int startY;
    private Location lastHeightChange = null;

    public FlyingKick(Player player) {
        super(player);

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

        stateStart = this.startTime;
        bPlayer.addCooldown(this);

        Location vehicleLocation = player.getLocation().clone();
        this.direction = player.getLocation().getDirection().clone().setY(0).normalize();
        this.startY = player.getLocation().getBlockY();

        vehicle = player.getWorld().spawn(vehicleLocation, ArmorStand.class);
        vehicle.setBasePlate(false);
        vehicle.setVisible(false);
        vehicle.setGravity(true);
        vehicle.getLocation().setDirection(player.getLocation().getDirection());
        vehicle.setMarker(true);
        vehicle.setPassenger(player);


        ChissentialsPlugin.plugin.getServer().getPluginManager().registerEvents(this, ChissentialsPlugin.plugin);
        this.start();
    }

    @EventHandler
    public void onPlayerSwing(PlayerAnimationEvent event) {
        if (event.getPlayer() == this.player) {
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

                Collection<Entity> nearbyEntities = player.getWorld().getNearbyEntities(player.getLocation(), ExplosionRadius + 1, 6, ExplosionRadius + 1);
                for (Entity entity : nearbyEntities) {
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
        Vector delta = this.direction.clone().multiply(speed);

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
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return 4000;
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
}

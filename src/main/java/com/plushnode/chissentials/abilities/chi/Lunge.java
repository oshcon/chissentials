package com.plushnode.chissentials.abilities.chi;

import com.plushnode.chissentials.ChissentialsPlugin;
import com.plushnode.chissentials.config.Configurable;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.awt.*;
import java.util.Random;

public class Lunge extends ChiAbility implements AddonAbility {
    private static final long DEFAULT_COOLDOWN = 6000;
    private static final double DEFAULT_SPEED = 10.0;
    private static final double DEFAULT_ANGLE = 20.0;
    private static final long DEFAULT_RAINBOW_CYCLE_DURATION = 600;

    private static final long ParticleUpdateDelay = 25;
    private static final long Timeout = 4000;
    private static final long LaunchTime = 250;
    private static final Random rand = new Random();

    private static boolean enabled = true;
    private static boolean displayParticles = true;
    private static long cooldown = DEFAULT_COOLDOWN;
    private static double speed = DEFAULT_SPEED;
    private static double angle = DEFAULT_ANGLE;
    private static long rainbowCycleDuration = DEFAULT_RAINBOW_CYCLE_DURATION;

    private boolean rainbowParticles;


    private long lastParticleDisplay = 0;

    public Lunge(Player player) {
        super(player);

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return;
        if (!player.isOnGround()) return;

        bPlayer.addCooldown(this);

        double theta = Math.toRadians(angle);
        Vector direction = player.getLocation().getDirection().clone().setY(0).normalize();
        player.setVelocity(direction.clone().setY(Math.sin(theta)).multiply(speed / 20.0));
        
        rainbowParticles = player.hasPermission("Chissentials.Lunge.Rainbow");

        this.start();
    }

    @Override
    public void progress() {
        long time = System.currentTimeMillis();

        if (time >= getStartTime() + Timeout || player.isDead() || !player.isOnline() || (player.isOnGround() && time > getStartTime() + LaunchTime)) {
            remove();
            return;
        }

        if (displayParticles && time >= lastParticleDisplay + ParticleUpdateDelay) {
            final Location location = player.getLocation().clone().add(0, 1.5, 0);

            if (!rainbowParticles) {
                final float horizontalWiggle = 0.3f;
                final float verticalWiggle = 0.5f;
                int amount = 12;

                ParticleEffect.CRIT.display(horizontalWiggle, verticalWiggle, horizontalWiggle, 0.0f, amount, location, ChissentialsPlugin.PARTICLE_RANGE);
            } else {
                final float horizontalWiggle = 0.6f;
                final float verticalWiggle = 0.75f;
                int amount = 16;
                final long duration = time - getStartTime();

                float hue = (duration % rainbowCycleDuration) / (float)rainbowCycleDuration;
                int rgb = Color.HSBtoRGB(hue, 0.75f, 0.75f);
                Color color = new Color(rgb);

                for (int i = 0; i < amount; ++i) {
                    Vector offset = new Vector(
                            (rand.nextFloat() - 0.5f) * horizontalWiggle * 2,
                            (rand.nextFloat() - 0.5f) * verticalWiggle * 2,
                            (rand.nextFloat() - 0.5f) * horizontalWiggle * 2
                    );
                    ParticleEffect.REDSTONE.display(color.getRed(), color.getGreen(), color.getBlue(), 0.005f, 0, location.clone().add(offset), ChissentialsPlugin.PARTICLE_RANGE);
                }
            }

            lastParticleDisplay = time;
        }
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
        return "Lunge";
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
            enabled = this.config.getBoolean("Abilities.Chi.Lunge.Enabled", true);
            cooldown = this.config.getLong("Abilities.Chi.Lunge.Cooldown", DEFAULT_COOLDOWN);
            angle = this.config.getDouble("Abilities.Chi.Lunge.Angle", DEFAULT_ANGLE);
            speed = this.config.getDouble("Abilities.Chi.Lunge.Speed", DEFAULT_SPEED);
            displayParticles = this.config.getBoolean("Abilities.Chi.Lunge.DisplayParticles", true);
            rainbowCycleDuration = this.config.getLong("Abilities.Chi.Lunge.RainbowCycleDuration", DEFAULT_RAINBOW_CYCLE_DURATION);
        }
    }
}

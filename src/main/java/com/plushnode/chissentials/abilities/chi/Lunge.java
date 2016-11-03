package com.plushnode.chissentials.abilities.chi;

import com.plushnode.chissentials.ChissentialsPlugin;
import com.plushnode.chissentials.ability.SwingDamageAbility;
import com.plushnode.chissentials.combopoint.ComboPointManager;
import com.plushnode.chissentials.config.Configurable;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.util.ActionBar;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Lunge extends ChiAbility implements AddonAbility {
    private static final long DEFAULT_COOLDOWN = 6000;
    private static final double DEFAULT_SPEED = 10.0;
    private static final double DEFAULT_ANGLE = 20.0;

    private static final long ParticleUpdateDelay = 25;
    private static final long Timeout = 4000;
    private static final long LaunchTime = 250;

    private static boolean enabled = true;
    private static boolean displayParticles = true;
    private static long cooldown = DEFAULT_COOLDOWN;
    private static double speed = DEFAULT_SPEED;
    private static double angle = DEFAULT_ANGLE;

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
            ParticleEffect.CRIT.display(0.5f, 0.5f, 0.5f, 0.0f, 12, player.getLocation().add(0, 1.5, 0), ChissentialsPlugin.PARTICLE_RANGE);

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

    public static class Config extends Configurable {
        public Config(ChissentialsPlugin plugin) {
            super(plugin);

            onConfigReload();
        }

        @Override
        public void onConfigReload() {
            enabled = this.config.getBoolean("Chi.Lunge.Enabled", true);
            cooldown = this.config.getLong("Chi.Lunge.Cooldown", DEFAULT_COOLDOWN);
            angle = this.config.getDouble("Chi.Lunge.Angle", DEFAULT_ANGLE);
            speed = this.config.getDouble("Chi.Lunge.Speed", DEFAULT_SPEED);
            displayParticles = this.config.getBoolean("Chi.Lunge.DisplayParticles", true);
        }
    }
}

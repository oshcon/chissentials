package com.plushnode.chissentials.abilities.chi.passives;

import com.plushnode.chissentials.ChissentialsPlugin;
import com.plushnode.chissentials.PrecisionSpot;
import com.plushnode.chissentials.PrecisionSpot.PrecisionArea;
import com.plushnode.chissentials.StunManager;
import com.plushnode.chissentials.collision.AABB;
import com.plushnode.chissentials.collision.Ray;
import com.plushnode.chissentials.config.Configurable;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.util.ParticleEffect;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Precision extends ChiAbility implements AddonAbility, Listener {
    private static final long DEFAULT_DURATION = 2000L;
    private static final long DEFAULT_COOLDOWN = 10000L;

    private static boolean enabled = true;
    private static long duration = DEFAULT_DURATION;
    private static long cooldown = DEFAULT_COOLDOWN;

    private Map<Player, List<PrecisionSpot.PrecisionArea>> areas = new HashMap<>();

    public Precision(Player player) {
        super(player);

        Precision current = CoreAbility.getAbility(player, Precision.class);
        if (current == null) {
            ChissentialsPlugin.plugin.getServer().getPluginManager().registerEvents(this, ChissentialsPlugin.plugin);

            start();
        }
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        List<Player> visualList = Collections.singletonList(this.player);
        Vector up = new Vector(0, 1, 0);

        Iterator<Map.Entry<Player, List<PrecisionArea>>> iterator;

        for (iterator = this.areas.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<Player, List<PrecisionSpot.PrecisionArea>> entry = iterator.next();
            Player target = entry.getKey();

            if (target.isDead() || !target.isOnline() || target.getGameMode() != GameMode.SURVIVAL) {
                iterator.remove();
                continue;
            }

            for (PrecisionSpot.PrecisionArea area : entry.getValue()) {
                Vector toPlayer = this.player.getLocation().toVector().subtract(target.getLocation().toVector()).normalize();

                Vector targetPos = target.getLocation().toVector();
                Vector visualBase = targetPos.clone().add(toPlayer);
                Vector front = player.getLocation().getDirection();

                boolean inFront = toPlayer.dot(target.getLocation().getDirection()) > 0;
                Vector right = (inFront ? front.crossProduct(up).multiply(-1.0) : front.crossProduct(up)).normalize();

                // Create the precision spot for the current player then display it near the target so it's always aligned.
                PrecisionSpot playerSpot = PrecisionSpot.getPrecisionSpot(this.player.getLocation(), target.isSneaking(), area, up, right);
                Vector mid = playerSpot.getBounds().mid().subtract(this.player.getLocation().toVector());

                ParticleEffect.CRIT.display(0.0F, 0.0F, 0.0F, 0.0F, 1, visualBase.clone().add(mid).toLocation(player.getWorld()), visualList);
            }
        }
    }

    @Override
    public void remove() {
        EntityDamageByEntityEvent.getHandlerList().unregister(this);
        super.remove();
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;

        Entity damagerEntity = event.getDamager();
        Entity targetEntity = event.getEntity();

        if (damagerEntity != this.player) return;
        if (!(targetEntity instanceof Player)) return;

        Player damager = (Player)damagerEntity;
        Player target = (Player)targetEntity;

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(damager);
        if (bPlayer == null || !bPlayer.hasElement(Element.CHI)) {
            return;
        }

        if (!bPlayer.canBendIgnoreBindsCooldowns(this)) return;
        if (bPlayer.isOnCooldown(this)) return;

        Ray ray = new Ray(damager.getEyeLocation().toVector(), damager.getEyeLocation().getDirection());

        List<PrecisionSpot> precisionSpots = getPrecisionSpots(target);

        double closestDist = Double.MAX_VALUE;
        PrecisionSpot closest = null;

        for (PrecisionSpot spot : precisionSpots) {
            AABB bounds = spot.getBounds();

            Optional<Double> result = bounds.intersects(ray);
            if (result.isPresent()) {
                double dist = result.get();

                if (dist < closestDist) {
                    closest = spot;
                    closestDist = dist;
                }
            }
        }

        if (closest != null) {
            List<PrecisionArea> targetAreas = getAreas(target);
            if (!targetAreas.contains(closest.getArea())) {
                targetAreas.add(closest.getArea());
            }

            if (targetAreas.size() >= 4) {
                targetAreas.clear();

                StunManager.get().stun(target, duration);
                bPlayer.addCooldown(this);
            }
        }
    }

    private static List<PrecisionSpot> getPrecisionSpots(Player player) {
        List<PrecisionSpot> spots = new ArrayList<>();

        Location location = player.getLocation();
        Vector front = location.getDirection();

        Vector up = new Vector(0, 1, 0);
        Vector right = front.crossProduct(up).normalize();

        boolean sneaking = player.isSneaking();

        spots.add(PrecisionSpot.getPrecisionSpot(location, sneaking, PrecisionSpot.PrecisionArea.KneeLeft, up, right));
        spots.add(PrecisionSpot.getPrecisionSpot(location, sneaking, PrecisionSpot.PrecisionArea.KneeRight, up, right));
        spots.add(PrecisionSpot.getPrecisionSpot(location, sneaking, PrecisionSpot.PrecisionArea.ShoulderLeft, up, right));
        spots.add(PrecisionSpot.getPrecisionSpot(location, sneaking, PrecisionSpot.PrecisionArea.ShoulderRight, up, right));

        return spots;
    }

    private List<PrecisionSpot.PrecisionArea> getAreas(Player player) {
        List<PrecisionSpot.PrecisionArea> playerAreas = this.areas.get(player);

        if (playerAreas == null) {
            playerAreas = new ArrayList<>();
            this.areas.put(player, playerAreas);
        }

        return playerAreas;
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
    public boolean isHiddenAbility() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public String getName() {
        return "Precision";
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
    public String getAuthor()
    {
        return ChissentialsPlugin.developer;
    }

    @Override
    public String getVersion()
    {
        return ChissentialsPlugin.version;
    }

    // Handle Precision passive here since ProjectKorra doesn't work correctly with addon passives.
    public static void createPassiveTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.isDead() || player.getGameMode() == GameMode.SPECTATOR) continue;

                    BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

                    if (bPlayer != null && bPlayer.hasElement(Element.CHI) && !CoreAbility.hasAbility(player, Precision.class)) {
                        if (bPlayer.canBendIgnoreBindsCooldowns(CoreAbility.getAbility("Precision"))) {
                            new Precision(player);
                        }
                    }
                }
            }
        }.runTaskTimer(ChissentialsPlugin.plugin, 20, 20);
    }

    public static class Config extends Configurable {
        public Config(ChissentialsPlugin plugin) {
            super(plugin);

            onConfigReload();
        }

        @Override
        public void onConfigReload() {
            enabled = this.config.getBoolean("Abilities.Chi.Precision.Enabled", true);
            cooldown = this.config.getLong("Abilities.Chi.Precision.Cooldown", DEFAULT_COOLDOWN);
            duration = this.config.getLong("Abilities.Chi.Precision.Duration", DEFAULT_DURATION);
        }
    }
}

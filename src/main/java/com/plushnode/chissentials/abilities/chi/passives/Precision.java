package com.plushnode.chissentials.abilities.chi.passives;

import com.plushnode.chissentials.ChissentialsPlugin;
import com.plushnode.chissentials.PrecisionSpot;
import com.plushnode.chissentials.PrecisionSpot.PrecisionArea;
import com.plushnode.chissentials.collision.AABB;
import com.plushnode.chissentials.collision.Ray;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.util.ParticleEffect;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

public class Precision extends ChiAbility implements AddonAbility, Listener {
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

        for (Map.Entry<Player, List<PrecisionSpot.PrecisionArea>> entry : this.areas.entrySet()) {
            Player target = entry.getKey();

            for (PrecisionSpot.PrecisionArea area : entry.getValue()) {
                PrecisionSpot spot = PrecisionSpot.getPrecisionSpot(target, area);

                Vector toPlayer = this.player.getLocation().toVector().subtract(target.getLocation().toVector()).normalize();
                ParticleEffect.CRIT.display(spot.getBounds().mid().add(toPlayer.clone().multiply(1.0D)).toLocation(target.getWorld()), 0.0F, 0.0F, 0.0F, 0.0F, 1);
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
        Entity damagerEntity = event.getDamager();
        Entity targetEntity = event.getEntity();

        if (!(damagerEntity instanceof Player)) return;
        if (!(targetEntity instanceof Player)) return;

        Player damager = (Player)damagerEntity;
        Player target = (Player)targetEntity;

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(damager);
        if (bPlayer == null || !bPlayer.hasElement(Element.CHI)) {
            return;
        }

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
                System.out.println("Hit precision spot");
                targetAreas.add(closest.getArea());
            }

            if (targetAreas.size() >= 4) {
                target.setNoDamageTicks(0);
                target.damage(6.0D);
                targetAreas.clear();

                player.sendMessage("All areas hit");
            }
        }
    }

    private static List<PrecisionSpot> getPrecisionSpots(Player player) {
        List<PrecisionSpot> spots = new ArrayList<>();

        Location location = player.getLocation();
        Vector front = location.getDirection();

        Vector up = new Vector(0, 1, 0);
        Vector right = front.crossProduct(up).normalize();

        spots.add(PrecisionSpot.getPrecisionSpot(player, PrecisionSpot.PrecisionArea.KneeLeft, up, right));
        spots.add(PrecisionSpot.getPrecisionSpot(player, PrecisionSpot.PrecisionArea.KneeRight, up, right));
        spots.add(PrecisionSpot.getPrecisionSpot(player, PrecisionSpot.PrecisionArea.ShoulderLeft, up, right));
        spots.add(PrecisionSpot.getPrecisionSpot(player, PrecisionSpot.PrecisionArea.ShoulderRight, up, right));

        return spots;
    }

    private List<PrecisionSpot.PrecisionArea> getAreas(Player player) {
        List<PrecisionSpot.PrecisionArea> playerAreas = this.areas.getOrDefault(player, new ArrayList<>());

        this.areas.put(player, playerAreas);

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
    public long getCooldown() {
        return 0L;
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
}

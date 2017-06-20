package com.plushnode.chissentials.listeners;

import com.plushnode.chissentials.ChissentialsPlugin;
import com.plushnode.chissentials.abilities.chi.*;
import com.plushnode.chissentials.ability.SwingDamageAbility;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class EntityListener implements Listener {
    private ChissentialsPlugin plugin;

    public EntityListener(ChissentialsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerSwing(PlayerAnimationEvent event) {
        if (event.isCancelled()) return;

        BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(event.getPlayer());
        if (bendingPlayer == null) return;

        CoreAbility boundAbility = bendingPlayer.getBoundAbility();
        if (boundAbility == null) return;

        if (!bendingPlayer.canBend(boundAbility)) return;

        if (boundAbility.getClass().equals(FlyingKick.class)) {
            new FlyingKick(event.getPlayer());
        } else if (boundAbility.getClass().equals(LegSweep.class)) {
            new LegSweep(event.getPlayer());
        } else if (boundAbility.getClass().equals(Lunge.class)) {
            new Lunge(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        if (event.isCancelled()) return;

        BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(event.getPlayer());
        if (bendingPlayer == null) return;

        CoreAbility boundAbility = bendingPlayer.getBoundAbility();
        if (boundAbility == null) return;

        if (!bendingPlayer.canBend(boundAbility)) return;

        if (boundAbility.getClass().equals(Karma.class)) {
            new Karma(event.getPlayer());
        } else if (boundAbility.getClass().equals(Ambush.class)) {
            new Ambush(event.getPlayer());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player)) return;

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer((Player)event.getEntity());
        if (bPlayer == null) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && bPlayer.hasElement(Element.CHI)) {
            if (bPlayer.isOnCooldown("Lunge")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamager() instanceof Player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        Player damager = (Player)event.getDamager();

        BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(damager);
        if (bendingPlayer == null) return;

        CoreAbility boundAbility = bendingPlayer.getBoundAbility();
        if (boundAbility == null) return;

        if (!bendingPlayer.canBend(boundAbility)) return;

        if (boundAbility.getClass().equals(KickPunch.class)) {
            new KickPunch(damager);
        } else if (boundAbility.getClass().equals(FlyingKick.class)) {
            new FlyingKick(damager);
        } else if (boundAbility.getClass().equals(LegSweep.class)) {
            new LegSweep(damager);
        } else if (boundAbility.getClass().equals(DeadlyPunch.class)) {
            new DeadlyPunch(damager);
        }

        if (boundAbility instanceof SwingDamageAbility) {
            double newDamage = ((SwingDamageAbility) boundAbility).getSwingDamage(damager, event.getEntity());

            event.setDamage(newDamage);
        }
    }
}

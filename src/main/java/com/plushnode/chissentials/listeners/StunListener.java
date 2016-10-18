package com.plushnode.chissentials.listeners;

import com.plushnode.chissentials.StunManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;

public class StunListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (StunManager.get().isStunned(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityChangeBlockEvent(EntityChangeBlockEvent event) {
        if (StunManager.get().isStunned(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityExplodeEvent(EntityExplodeEvent event) {
        if (StunManager.get().isStunned(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityInteractEvent(EntityInteractEvent event) {
        if (StunManager.get().isStunned(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityProjectileLaunchEvent(ProjectileLaunchEvent event) {
        if (StunManager.get().isStunned(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityShootBowEvent(EntityShootBowEvent event) {
        if (StunManager.get().isStunned(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntitySlimeSplitEvent(SlimeSplitEvent event) {
        if (StunManager.get().isStunned(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityTarget(EntityTargetEvent event) {
        if (StunManager.get().isStunned(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityTargetLiving(EntityTargetLivingEntityEvent event) {
        if (StunManager.get().isStunned(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityTeleportEvent(EntityTeleportEvent event) {
        if (StunManager.get().isStunned(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {
        if (StunManager.get().isStunned(event.getDamager())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteraction(PlayerInteractEvent event) {
        if (StunManager.get().isStunned(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (StunManager.get().isStunned(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (StunManager.get().isStunned(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (StunManager.get().isStunned(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerSwing(PlayerAnimationEvent event) {
        if (StunManager.get().isStunned(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}

package com.plushnode.chissentials.listeners;

import com.plushnode.chissentials.ChissentialsPlugin;
import com.plushnode.chissentials.config.ConfigManager;
import com.projectkorra.projectkorra.event.BendingReloadEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

public class BendingListener implements Listener {
    private ChissentialsPlugin plugin;

    public BendingListener(ChissentialsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBendingReload(BendingReloadEvent event) {
        final CommandSender sender = event.getSender();

        new BukkitRunnable() {
            public void run() {
                plugin.reloadConfig();
                ConfigManager.get().onConfigReload(event);
                plugin.registerAbilities();
                sender.sendMessage(ChatColor.GOLD + "Chissentials config reloaded.");
            }
        }.runTaskLater(plugin, 1);
    }
}
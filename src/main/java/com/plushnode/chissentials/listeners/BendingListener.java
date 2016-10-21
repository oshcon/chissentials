package com.plushnode.chissentials.listeners;

import com.plushnode.chissentials.ChissentialsPlugin;
import com.plushnode.chissentials.config.ConfigManager;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.event.BendingReloadEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class BendingListener implements Listener {
    private ChissentialsPlugin plugin;

    public BendingListener(ChissentialsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBendingReload(BendingReloadEvent event) {
        final CommandSender sender = event.getSender();

        Map<String, HashMap<Integer, String>> playersTemp = new HashMap<>();

        for (BendingPlayer bPlayer : BendingPlayer.getPlayers().values()) {
            // Deep copy current abilities for every player
            playersTemp.put(bPlayer.getName(), new HashMap<>(bPlayer.getAbilities()));
        }

        final Map<String, HashMap<Integer, String>> players = new HashMap<>(playersTemp);

        new BukkitRunnable() {
            public void run() {
                plugin.reloadConfig();
                ConfigManager.get().onConfigReload(event);
                plugin.registerAbilities();
                sender.sendMessage(ChatColor.GOLD + "Chissentials config reloaded.");

                // Deep copy all of the slots from before reload into the new slots
                for (Map.Entry<String, HashMap<Integer, String>> entry : players.entrySet()) {
                    String playerName = entry.getKey();
                    HashMap<Integer, String> slots = entry.getValue();

                    BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(playerName);
                    if (bPlayer == null) continue;

                    HashMap<Integer, String> playerAbilities = bPlayer.getAbilities();

                    for (Map.Entry<Integer, String> slotEntry : slots.entrySet()) {
                        int slot = slotEntry.getKey();
                        String abilityName = slotEntry.getValue();

                        if (CoreAbility.getAbility(abilityName) == null) continue;

                        playerAbilities.put(slot, abilityName);
                    }
                }
            }
        }.runTaskLater(plugin, 5);
    }
}

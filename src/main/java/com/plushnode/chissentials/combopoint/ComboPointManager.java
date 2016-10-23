package com.plushnode.chissentials.combopoint;

import com.projectkorra.projectkorra.util.ActionBar;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ComboPointManager extends BukkitRunnable {
    private static final ChatColor ComboColor = ChatColor.GOLD;
    private static final String ComboString = "- ";
    private static final int MaxComboPoints = 5;
    private static ComboPointManager instance;

    private Map<Player, Integer> playerComboPoints;

    private ComboPointManager() {
        this.playerComboPoints = new HashMap<>();
    }

    public static ComboPointManager get() {
        if (instance == null) {
            instance = new ComboPointManager();
        }

        return instance;
    }

    // Returns current / MaxComboPoints
    public static double getComboPercent(int current) {
        return current / (double)MaxComboPoints;
    }

    public int getComboPoints(Player player) {
        Integer current = playerComboPoints.get(player);
        if (current == null) {
            return 0;
        }
        return current;
    }

    public void addComboPoints(Player player, int amount) {
        Integer current = playerComboPoints.get(player);

        if (current == null) {
            current = 0;
        }

        setComboPoints(player, current + amount);
    }

    // Returns true if the combo points were spent
    public boolean spendComboPoints(Player player, int amount) {
        Integer current = playerComboPoints.get(player);

        if (current == null || current < amount) {
            return false;
        }

        setComboPoints(player, current - amount);
        return true;
    }

    public int spendAllComboPoints(Player player) {
        Integer current = playerComboPoints.get(player);

        if (current == null) {
            return 0;
        }

        setComboPoints(player, 0);
        return current;
    }

    private void setComboPoints(Player player, int amount) {
        amount = Math.max(Math.min(MaxComboPoints, amount), 0);

        if (amount > 0) {
            playerComboPoints.put(player, amount);
        } else {
            playerComboPoints.remove(player);
        }
    }

    @Override
    public void run() {
        Iterator<Map.Entry<Player, Integer>> iter = playerComboPoints.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<Player, Integer> entry = iter.next();
            Player player = entry.getKey();
            Integer amount = entry.getValue();

            if (player.isDead() || !player.isOnline()) {
                System.out.println("Clearing combo points for " + player.getName());
                iter.remove();
                continue;
            }

            if (amount > 0) {
                ActionBar.sendActionBar(ComboColor + formatComboMessage(amount), player);
            }
        }
    }

    private String formatComboMessage(int amount) {
        return String.join("", Collections.nCopies(amount, ComboString));
    }
}

package com.plushnode.chissentials;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class StunManager extends BukkitRunnable {
    private static final double DistanceThresholdSq = 0.5 * 0.5;
    private static StunManager instance = null;
    private Map<Entity, StunInfo> stunned = new HashMap<>();

    private StunManager() {

    }

    public static StunManager get() {
        if (instance == null) {
            instance = new StunManager();
        }

        return instance;
    }


    public void stun(Entity entity, Long duration) {
        StunInfo info = new StunInfo();
        info.end = System.currentTimeMillis() + duration;
        info.location = entity.getLocation().clone();

        stunned.put(entity, info);
    }

    public void unstun(Entity entity) {
        stunned.remove(entity);
    }

    public boolean isStunned(Entity entity) {
        long time = System.currentTimeMillis();
        StunInfo info = stunned.get(entity);

        if (info == null) return false;

        if (time >= info.end) {
            unstun(entity);
            return false;
        }

        return true;
    }

    @Override
    public void run() {
        long time = System.currentTimeMillis();
        Iterator<Map.Entry<Entity, StunInfo>> iter = stunned.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<Entity, StunInfo> entry = iter.next();
            Entity entity = entry.getKey();
            StunInfo info = entry.getValue();

            if (time >= info.end) {
                iter.remove();
                continue;
            }

            if (entity.getLocation().distanceSquared(info.location) > DistanceThresholdSq) {
                entity.teleport(info.location);
            }
        }
    }

    private class StunInfo {
        Long end;
        Location location;
    }
}

package com.plushnode.chissentials;

import com.plushnode.chissentials.abilities.chi.*;
import com.plushnode.chissentials.abilities.chi.passives.Precision;
import com.plushnode.chissentials.combopoint.ComboPointManager;
import com.plushnode.chissentials.listeners.BendingListener;
import com.plushnode.chissentials.listeners.EntityListener;
import com.plushnode.chissentials.listeners.StunListener;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Logger;

public class ChissentialsPlugin extends JavaPlugin {
    public static final double PARTICLE_RANGE = 257;
    public static ChissentialsPlugin plugin;
    public static String developer;
    public static String version;

    private Logger logger = getLogger();

    @Override
    public void onEnable() {
        plugin = this;

        saveDefaultConfig();

        developer = String.join(", ", getDescription().getAuthors());
        version = getDescription().getVersion();

        this.getServer().getPluginManager().registerEvents(new EntityListener(this), this);
        this.getServer().getPluginManager().registerEvents(new StunListener(), this);
        this.getServer().getPluginManager().registerEvents(new BendingListener(this), this);

        StunManager.get().runTaskTimer(this, 0L, 5L);
        ComboPointManager.get().runTaskTimer(this, 0L, 5L);

        new FlyingKick.Config(this);
        new Karma.Config(this);
        new KickPunch.Config(this);
        new LegSweep.Config(this);
        new Ambush.Config(this);
        new DeadlyPunch.Config(this);
        new Lunge.Config(this);
        new Precision.Config(this);

        Precision.createPassiveTask();

        // Register after loading the config so enabled will be set correctly
        registerAbilities();
    }

    public void registerAbilities() {
        logger.info("Registering Chissentials abilities with ProjectKorra.");
        CoreAbility.registerPluginAbilities(this, "com.plushnode.chissentials.abilities");

        // Add a fake combo to JedCore combos list so Precision appears as a cooldown.
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Class<?> JCMethods = Class.forName("com.jedk1.jedcore.JCMethods");

                    Method getCombos = JCMethods.getMethod("getCombos");
                    List<String> combos = (List<String>)getCombos.invoke(null);

                    if (combos != null) {
                        combos.add("Precision");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.runTaskLater(this, 20);
    }

    @Override
    public void onDisable() {
        
    }
}
package com.plushnode.chissentials;

import com.plushnode.chissentials.abilities.chi.*;
import com.plushnode.chissentials.abilities.chi.passives.Precision;
import com.plushnode.chissentials.combopoint.ComboPointManager;
import com.plushnode.chissentials.listeners.BendingListener;
import com.plushnode.chissentials.listeners.EntityListener;
import com.plushnode.chissentials.listeners.StunListener;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.plugin.java.JavaPlugin;

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

        Precision.createPassiveTask();

        // Register after loading the config so enabled will be set correctly
        registerAbilities();
    }

    public void registerAbilities() {
        logger.info("Registering Chissentials abilities with ProjectKorra.");
        CoreAbility.registerPluginAbilities(this, "com.plushnode.chissentials.abilities");
    }

    @Override
    public void onDisable() {
        
    }
}
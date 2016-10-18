package com.plushnode.chissentials;

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

        developer = String.join(", ", getDescription().getAuthors());
        version = getDescription().getVersion();

        logger.info("Registering Chissentials abilities with ProjectKorra.");
        CoreAbility.registerPluginAbilities(this, "com.plushnode.chissentials.abilities");

        this.getServer().getPluginManager().registerEvents(new EntityListener(this), this);
        this.getServer().getPluginManager().registerEvents(new StunListener(), this);

        StunManager.get().runTaskTimer(this, 0L, 5L);
    }

    @Override
    public void onDisable() {
        
    }
}
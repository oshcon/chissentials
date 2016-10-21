package com.plushnode.chissentials.config;

import com.plushnode.chissentials.ChissentialsPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Observable;
import java.util.Observer;

public abstract class Configurable implements Observer {
    private ChissentialsPlugin plugin;
    protected FileConfiguration config;

    public Configurable(ChissentialsPlugin plugin) {
        this.plugin = plugin;
        this.config = this.plugin.getConfig();
        ConfigManager.get().addObserver(this);
    }

    public void destroy() {
        ConfigManager.get().deleteObserver(this);
    }

    @Override
    public final void update(Observable o, Object arg) {
        this.config = this.plugin.getConfig();
        onConfigReload();
    }

    public abstract void onConfigReload();
}

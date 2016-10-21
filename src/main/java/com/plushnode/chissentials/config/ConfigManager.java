package com.plushnode.chissentials.config;

import com.projectkorra.projectkorra.event.BendingReloadEvent;

import java.util.Observable;

public class ConfigManager extends Observable {
    private static ConfigManager instance;

    private ConfigManager() {

    }

    public static ConfigManager get() {
        if (instance == null)
            instance = new ConfigManager();
        return instance;
    }

    public void onConfigReload(BendingReloadEvent event) {
        setChanged();
        notifyObservers();
    }
}

package com.plushnode.chissentials.util;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ArrowUtil {
    private static Method getHandle = null, getDataWatcher = null, setData = null;
    private static Constructor<?> DataWatcherObjectConstructor = null;
    private static Field IntegerSerializer = null;

    public static void clear(Player player) {
        if (getHandle == null || getDataWatcher == null || setData == null) {
            Class<?> CraftEntity = null, Entity = null, DataWatcher = null;
            Class<?> DataWatcherObject = null, DataWatcherSerializer = null, DataWatcherRegistry = null;

            CraftEntity = ReflectionUtil.getNMSClass("org.bukkit.craftbukkit.%s.entity.CraftEntity");
            Entity = ReflectionUtil.getNMSClass("net.minecraft.server.%s.Entity");
            DataWatcher = ReflectionUtil.getNMSClass("net.minecraft.server.%s.DataWatcher");
            DataWatcherObject = ReflectionUtil.getNMSClass("net.minecraft.server.%s.DataWatcherObject");
            DataWatcherSerializer = ReflectionUtil.getNMSClass("net.minecraft.server.%s.DataWatcherSerializer");
            DataWatcherRegistry = ReflectionUtil.getNMSClass("net.minecraft.server.%s.DataWatcherRegistry");

            if (CraftEntity == null || Entity == null || DataWatcher == null
                    || DataWatcherObject == null || DataWatcherSerializer == null || DataWatcherRegistry == null)
            {
                return;
            }

            try {
                getHandle = CraftEntity.getDeclaredMethod("getHandle");
                getDataWatcher = Entity.getDeclaredMethod("getDataWatcher");

                IntegerSerializer = DataWatcherRegistry.getField("b");

                setData = DataWatcher.getDeclaredMethod("set", DataWatcherObject.asSubclass(Object.class), Object.class);

                DataWatcherObjectConstructor = DataWatcherObject.getConstructor(int.class, DataWatcherSerializer.asSubclass(Object.class));
            } catch (NoSuchMethodException|NoSuchFieldException e) {
                e.printStackTrace();
            }
        }

        try {
            Object entity = getHandle.invoke(player);
            Object dataWatcher = getDataWatcher.invoke(entity);

            Object intSerializer = IntegerSerializer.get(null);

            int metaIndex = 9;
            if (ReflectionUtil.getServerVersion() > 9) {
                metaIndex = 10;
            }
            Object watcherObject = DataWatcherObjectConstructor.newInstance(metaIndex, intSerializer);

            setData.invoke(dataWatcher, watcherObject, 0);
        } catch (IllegalAccessException|InvocationTargetException |InstantiationException e) {
            e.printStackTrace();
        }
    }
}

package com.plushnode.chissentials.util;

import com.projectkorra.projectkorra.util.ReflectionHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReflectionUtil {
    private static Integer serverVersion = null;
    private static String nmsVersion = null;

    public static int getServerVersion() {
        if (serverVersion == null)
            serverVersion = Integer.parseInt(ReflectionHandler.PackageType.getServerVersion().split("_")[1]);
        return serverVersion;
    }

    public static Class<?> getNMSClass(String nmsClass) {
        if (nmsVersion == null) {
            Pattern pattern = Pattern.compile("net\\.minecraft\\.(?:server)?\\.(v(?:\\d+_)+R\\d)");
            for (Package p : Package.getPackages()) {
                String name = p.getName();
                Matcher m = pattern.matcher(name);
                if (m.matches()) {
                    nmsVersion = m.group(1);
                }
            }
        }

        if (nmsVersion == null) return null;

        try {
            return Class.forName(String.format(nmsClass, nmsVersion));
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}

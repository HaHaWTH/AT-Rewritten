package org.bukkit.craftbukkit.v1_19_R1.util;

import org.bukkit.util.CachedServerIcon;

public class CraftIconCache implements CachedServerIcon {
    public final String value;

    public String getData() { return value; } // Paper
    public CraftIconCache(final String value) {
        this.value = value;
    }
}

package io.github.niestrat99.advancedteleport.hooks;


import org.bukkit.World;
import org.bukkit.Location;

public abstract class ClaimPlugin {

    public abstract boolean canUse(World world);

    public abstract boolean isClaimed(Location location);
}

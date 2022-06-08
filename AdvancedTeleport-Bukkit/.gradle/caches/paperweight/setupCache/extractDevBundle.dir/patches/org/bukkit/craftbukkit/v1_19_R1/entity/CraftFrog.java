package org.bukkit.craftbukkit.v1_19_R1.entity;

import com.google.common.base.Preconditions;
import net.minecraft.world.entity.animal.frog.Frog;
import org.bukkit.Registry;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.craftbukkit.v1_19_R1.util.CraftNamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public class CraftFrog extends CraftAnimals implements org.bukkit.entity.Frog {

    public CraftFrog(CraftServer server, Frog entity) {
        super(server, entity);
    }

    @Override
    public Frog getHandle() {
        return (Frog) entity;
    }

    @Override
    public String toString() {
        return "CraftFrog";
    }

    @Override
    public EntityType getType() {
        return EntityType.FROG;
    }

    @Override
    public Entity getTongueTarget() {
        return this.getHandle().getTongueTarget().map(net.minecraft.world.entity.Entity::getBukkitEntity).orElse(null);
    }

    @Override
    public void setTongueTarget(Entity target) {
        if (target == null) {
            this.getHandle().eraseTongueTarget();
        } else {
            this.getHandle().setTongueTarget(((CraftEntity) target).getHandle());
        }
    }

    @Override
    public Variant getVariant() {
        return Registry.FROG_VARIANT.get(CraftNamespacedKey.fromMinecraft(net.minecraft.core.Registry.FROG_VARIANT.getKey(this.getHandle().getVariant())));
    }

    @Override
    public void setVariant(Variant variant) {
        Preconditions.checkArgument(variant != null, "variant");

        this.getHandle().setVariant(net.minecraft.core.Registry.FROG_VARIANT.get(CraftNamespacedKey.toMinecraft(variant.getKey())));
    }
}

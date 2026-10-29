package ladysnake.requiem.common.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

public class CuredVillagerEntity extends Villager {
    public CuredVillagerEntity(EntityType<? extends CuredVillagerEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected Component getTypeName() {
        return this.getVillagerData().profession().value().name();
    }
}

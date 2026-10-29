package ladysnake.requiem.common.block;

import ladysnake.requiem.Requiem;
import ladysnake.requiem.common.block.obelisk.RunestoneBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class RequiemBlockEntities {
    public static final BlockEntityType<RunestoneBlockEntity> RUNIC_OBSIDIAN = BlockEntityType.Builder.of(RunestoneBlockEntity::new,
        RequiemBlocks.TACHYLITE_RUNESTONE,
        RequiemBlocks.RUNIC_TACHYLITE_ATTRITION,
        RequiemBlocks.RUNIC_TACHYLITE_EMANCIPATION,
        RequiemBlocks.RUNIC_TACHYLITE_PENANCE,
        RequiemBlocks.RUNIC_TACHYLITE_RECLAMATION,
        RequiemBlocks.RIFT_RUNE
    ).build(null);

    public static void register(RegisterEvent.RegisterHelper<BlockEntityType<?>> helper) {
        helper.register(Requiem.id("runic_obsidian"), RUNIC_OBSIDIAN);
    }

    public static void init() {
        // Registered via RegisterEvent
    }
}

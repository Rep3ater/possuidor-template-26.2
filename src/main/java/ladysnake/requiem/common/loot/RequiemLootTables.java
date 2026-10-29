package ladysnake.requiem.common.loot;

import ladysnake.requiem.Requiem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class RequiemLootTables {
    public static final LootContextParamSet POSSESSION = new LootContextParamSet.Builder()
        .required(LootContextParams.THIS_ENTITY)
        .required(LootContextParams.ORIGIN)
        .build();

    public static final LootItemConditionType BOUND_SHELL_CONDITION = new LootItemConditionType(BoundShellLootCondition.CODEC);
    public static final LootItemConditionType HOST_CONDITION = new LootItemConditionType(HostLootCondition.HOST_CODEC);
    public static final LootItemConditionType POSSESSOR_CONDITION = new LootItemConditionType(HostLootCondition.POSSESSOR_CODEC);
    public static final LootItemConditionType RIFT_MORTICIAN_CONDITION = new LootItemConditionType(RiftMorticianLootCondition.CODEC);

    public static void register(RegisterEvent.RegisterHelper<LootItemConditionType> helper) {
        helper.register(Requiem.id("rift_mortician"), RIFT_MORTICIAN_CONDITION);
        helper.register(Requiem.id("bound_shell"), BOUND_SHELL_CONDITION);
        helper.register(Requiem.id("host"), HOST_CONDITION);
        helper.register(Requiem.id("possessor"), POSSESSOR_CONDITION);
    }

    public static void init() {
        // Registered via RegisterEvent
    }
}

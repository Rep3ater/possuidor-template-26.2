/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses>.
 */
package ladysnake.requiem.common.loot;

import com.mojang.serialization.MapCodec;
import ladysnake.requiem.Requiem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class RequiemLootTables {
    public static final LootContextParamSet POSSESSION = new LootContextParamSet.Builder()
        .required(LootContextParams.THIS_ENTITY)
        .required(LootContextParams.ORIGIN)
        .build();

    public static final MapCodec<BoundShellLootCondition> BOUND_SHELL_CONDITION = BoundShellLootCondition.CODEC;
    public static final MapCodec<HostLootCondition> HOST_CONDITION = HostLootCondition.HOST_CODEC;
    public static final MapCodec<HostLootCondition> POSSESSOR_CONDITION = HostLootCondition.POSSESSOR_CODEC;
    public static final MapCodec<RiftMorticianLootCondition> RIFT_MORTICIAN_CONDITION = RiftMorticianLootCondition.CODEC;

    @SuppressWarnings("unchecked")
    public static void register(RegisterEvent.RegisterHelper<MapCodec<? extends LootItemCondition>> helper) {
        helper.register(Requiem.id("rift_mortician"), (MapCodec) RIFT_MORTICIAN_CONDITION);
        helper.register(Requiem.id("bound_shell"), (MapCodec) BOUND_SHELL_CONDITION);
        helper.register(Requiem.id("host"), (MapCodec) HOST_CONDITION);
        helper.register(Requiem.id("possessor"), (MapCodec) POSSESSOR_CONDITION);
    }

    public static void init() {
        // Registered via RegisterEvent
    }
}

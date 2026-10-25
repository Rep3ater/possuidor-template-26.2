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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package ladysnake.requiem.common.entity.ai.brain;

import ladysnake.requiem.Requiem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Optional;

public final class PandemoniumMemoryModules {
    public static final MemoryModuleType<Integer> GO_HOME_ATTEMPTS = new MemoryModuleType<>(Optional.empty());
    public static final MemoryModuleType<List<LivingEntity>> VISIBLE_HOSTILES = new MemoryModuleType<>(Optional.empty());

    public static void register(RegisterEvent.RegisterHelper<MemoryModuleType<?>> helper) {
        helper.register(Requiem.id("pathfinding_failures"), GO_HOME_ATTEMPTS);
        helper.register(Requiem.id("visible_hostiles"), VISIBLE_HOSTILES);
    }
}

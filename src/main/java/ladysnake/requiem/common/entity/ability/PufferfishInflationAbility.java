/*
 * Requiem
 * Copyright (C) 2019-2023 Ladysnake
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
package ladysnake.requiem.common.entity.ability;

import ladysnake.requiem.core.entity.ability.IndirectAbilityBase;
import ladysnake.requiem.mixin.common.possession.gameplay.ability.PufferfishEntityAccessor;
import net.minecraft.world.entity.animal.Pufferfish;

/**
 * Depends on {@link ladysnake.requiem.mixin.common.possession.gameplay.ability.PufferfishEntityMixin} to work
 */
public class PufferfishInflationAbility extends IndirectAbilityBase<Pufferfish> {
    public PufferfishInflationAbility(Pufferfish owner) {
        super(owner, 0);
    }

    @Override
    protected boolean run() {
        if (!this.owner.level().isClientSide()) {
            PufferfishEntityAccessor pufferfish = (PufferfishEntityAccessor) this.owner;
            if (pufferfish.getInflateTicks() == 0) {
                pufferfish.setInflateTicks(1);
                pufferfish.setDeflateTicks(0);
            } else {
                pufferfish.setInflateTicks(0);
                pufferfish.setDeflateTicks(60);
            }
        }
        return true;
    }
}

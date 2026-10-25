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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ladysnake.requiem.common.entity.ability;

import ladysnake.requiem.core.entity.ability.IndirectAbilityBase;
import net.minecraft.world.entity.monster.Creeper;

public class CreeperPrimingAbility extends IndirectAbilityBase<Creeper> {
    public CreeperPrimingAbility(Creeper owner) {
        super(owner, 0);
    }

    @Override
    public boolean run() {
        if (!this.owner.level().isClientSide()) {
            this.owner.setSwellDir(this.owner.getSwellDir() > 0 ? -1 : 1);
        }
        return true;
    }
}

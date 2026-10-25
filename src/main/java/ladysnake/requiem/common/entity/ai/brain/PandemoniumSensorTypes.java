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
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class PandemoniumSensorTypes {
    public static final SensorType<PlayerHostileSensor> CLOSEST_PLAYER_HOSTILE = new SensorType<>(PlayerHostileSensor::new);

    public static void register(RegisterEvent.RegisterHelper<SensorType<?>> helper) {
        helper.register(Requiem.id("player_hostile"), CLOSEST_PLAYER_HOSTILE);
    }
}

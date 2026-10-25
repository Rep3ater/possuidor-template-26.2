/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; If not, see <https://www.gnu.org/licenses>.
 */
package ladysnake.requiem.api.v1.entity;

import com.demonwav.mcdev.annotations.CheckEnv;
import com.demonwav.mcdev.annotations.Env;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import dev.onyxstudios.cca.api.v3.component.tick.CommonTickingComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import javax.annotation.CheckForNull;

/**
 * A {@link MovementAlterer} alters the movement of an {@link net.minecraft.entity.Entity}
 * according to a {@link MovementConfig}.
 */
public interface MovementAlterer extends CommonTickingComponent, AutoSyncedComponent {
    ComponentKey<MovementAlterer> KEY = ComponentRegistry.getOrCreate(Identifier.fromNamespaceAndPath("requiem", "movement_alterer"), MovementAlterer.class);

    static MovementAlterer get(Player player) {
        return KEY.get(player);
    }

    void setConfig(@CheckForNull MovementConfig config);

    void applyConfig();

    /**
     * Called at the end of {@code LocalPlayer#tickNewAi()}
     */
    @CheckEnv(Env.CLIENT)
    void alterControls();

    /**
     * Gets the acceleration that this entity has underwater.
     *
     * @param baseAcceleration the default acceleration computed in {@link net.minecraft.entity.LivingEntity#travel(Vec3)}
     * @return the modified acceleration
     */
    float getSwimmingAcceleration(float baseAcceleration);

    double getSwimmingUpwardsVelocity(double baseUpwardsVelocity);

    boolean canClimbWalls();

    boolean isNoClipping();

    void updateSwimming();

    boolean disablesSwimming();

    void hugWall(boolean hugging);
}


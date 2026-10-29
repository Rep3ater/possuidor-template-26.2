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
package ladysnake.requiem.common.entity.ability;

import ladysnake.requiem.common.sound.RequiemSoundEvents;
import ladysnake.requiem.core.entity.ability.DirectAbilityBase;
import ladysnake.requiem.core.remnant.VagrantInteraction;
import ladysnake.requiem.core.remnant.VagrantInteractionRegistryImpl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class VagrantPossessAbility extends DirectAbilityBase<Player, LivingEntity> {

    public static final int POSSESSION_RANGE = 5;
    public static final int POSSESSION_COOLDOWN = 8;

    private @Nullable LivingEntity target;
    private @Nullable VagrantInteraction interaction;

    public VagrantPossessAbility(Player owner) {
        super(owner, POSSESSION_COOLDOWN, POSSESSION_RANGE, LivingEntity.class);
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        if (super.canTarget(target)) {
            this.interaction = VagrantInteractionRegistryImpl.INSTANCE.getAction(target, this.owner);
        } else this.interaction = null;
        return this.interaction != null;
    }

    @Override
    protected boolean run(LivingEntity target) {
        this.target = target;
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), RequiemSoundEvents.EFFECT_POSSESSION_ATTEMPT, SoundSource.PLAYERS, 2, 0.6f);
        this.beginCooldown();
        return true;
    }

    @Override
    protected void onCooldownEnd() {
        if (this.interaction != null && this.target != null && !this.target.isRemoved() && this.target.isAlive()) {
            this.interaction.action().accept(this.target, this.owner);
        }
        this.interaction = null;
        this.target = null;
    }

    @Override
    public ResourceLocation getIconTexture() {
        return this.interaction != null ? this.interaction.icon() : VagrantInteractionRegistryImpl.POSSESSION_ICON;
    }
}

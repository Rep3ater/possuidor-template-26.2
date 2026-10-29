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
 *
 * Linking this mod statically or dynamically with other
 * modules is making a combined work based on this mod.
 * Thus, the terms and conditions of the GNU General Public License cover the whole combination.
 *
 * In addition, as a special exception, the copyright holders of
 * this mod give you permission to combine this mod
 * with free software programs or libraries that are released under the GNU LGPL
 * and with code included in the standard release of Minecraft under All Rights Reserved (or
 * modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the GNU GPL for this mod
 * and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of this mod are not obligated to grant
 * this special exception for their modified versions; it is their choice whether to do so.
 * The GNU General Public License gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version which carries forward this exception.
 */
package ladysnake.requiem.core.ability;

import com.demonwav.mcdev.annotations.CheckEnv;
import com.demonwav.mcdev.annotations.Env;
import com.google.common.base.Preconditions;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import ladysnake.requiem.api.v1.entity.ability.AbilityType;
import ladysnake.requiem.api.v1.entity.ability.MobAbilityConfig;
import ladysnake.requiem.api.v1.entity.ability.MobAbilityController;
import ladysnake.requiem.api.v1.internal.DummyMobAbilityController;
import ladysnake.requiem.core.RequiemCoreNetworking;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;

public class PlayerAbilityController implements MobAbilityController, AutoSyncedComponent {

    private final MobAbilityController soulAbilities;
    private final Player player;
    private final EnumMap<AbilityType, WeakReference<Entity>> targets = new EnumMap<>(AbilityType.class);
    private AbilityType[] sortedAbilities = AbilityType.values();

    private MobAbilityController delegate = DummyMobAbilityController.INSTANCE;

    public PlayerAbilityController(Player player, MobAbilityConfig<Player> soulConfig) {
        this.soulAbilities = new ImmutableMobAbilityController<>(player, soulConfig);
        this.player = player;
    }

    public static PlayerAbilityController get(Player player) {
        return (PlayerAbilityController) MobAbilityController.get(player);
    }

    public void resetAbilities(boolean incorporeal) {
        this.setDelegate(incorporeal ? this.soulAbilities : DummyMobAbilityController.INSTANCE);
    }

    public void usePossessedAbilities(Mob possessed) {
        this.setDelegate(MobAbilityController.get(possessed));
    }

    @CheckEnv(Env.CLIENT)
    public AbilityType[] getSortedAbilities() {
        return sortedAbilities;
    }

    @CheckEnv(Env.CLIENT)
    public void tryTarget(AbilityType type, Entity target) {
        if (this.canTarget(type, target)) {
            targets.put(type, new WeakReference<>(target));
        }
    }

    @CheckEnv(Env.CLIENT)
    public void clearTargets() {
        this.targets.clear();
    }

    @CheckEnv(Env.CLIENT)
    public @Nullable Entity getTargetedEntity(AbilityType type) {
        WeakReference<Entity> ref = targets.get(type);
        return ref == null ? null : ref.get();
    }

    @CheckEnv(Env.CLIENT)
    public InteractionResult useDirectAbility(AbilityType type) {
        Entity targetedEntity = this.getTargetedEntity(type);

        if (targetedEntity != null) {
            InteractionResult result = this.useDirect(type, targetedEntity);
            if (result.consumesAction()) {
                RequiemCoreNetworking.sendAbilityUseMessage(type, targetedEntity);
            }
            return result;
        }

        return InteractionResult.FAIL;
    }

    private void setDelegate(MobAbilityController delegate) {
        Preconditions.checkNotNull(delegate);
        this.delegate = delegate;

        this.targets.clear();
        this.sortAbilities();
        KEY.sync(this.player);
    }

    private void sortAbilities() {
        AbilityType[] a = this.sortedAbilities.clone();
        Arrays.sort(a, Comparator.comparingDouble(this::getRange));
        this.sortedAbilities = a;
    }

    @Override
    public ResourceLocation getIconTexture(AbilityType type) {
        return this.delegate.getIconTexture(type);
    }

    @Override
    public double getRange(AbilityType type) {
        return delegate.getRange(type);
    }

    @Override
    public boolean canTarget(AbilityType type, Entity target) {
        return delegate.canTarget(type, target);
    }

    @Override
    public InteractionResult useDirect(AbilityType type, Entity target) {
        return delegate.useDirect(type, target);
    }

    @Override
    public boolean useIndirect(AbilityType type) {
        return delegate.useIndirect(type);
    }

    @Override
    public float getCooldownProgress(AbilityType type) {
        return delegate.getCooldownProgress(type);
    }

    @Override
    public void tick() {
        delegate.tick();
    }

    @Override
    public boolean shouldSyncWith(ServerPlayer player) {
        return player == this.player;
    }

    @Override
    public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        boolean hasData = this.delegate != DummyMobAbilityController.INSTANCE;
        buf.writeBoolean(hasData);
        if (hasData) {
            this.delegate.writeSyncPacket(buf, recipient);
        }
    }

    @Override
    public void applySyncPacket(RegistryFriendlyByteBuf buf) {
        if (buf.isReadable()) {
            boolean hasData = buf.readBoolean();
            if (hasData && buf.isReadable()) {
                this.delegate.applySyncPacket(buf);
            }
        }
    }
}

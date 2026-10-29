/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU License
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
package ladysnake.requiem.core;

import ladysnake.requiem.api.v1.entity.ability.AbilityType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.PacketDistributor;

public final class RequiemCoreNetworking {
    public static final ResourceLocation USE_DIRECT_ABILITY = RequiemCore.id("direct_ability");
    public static final ResourceLocation HUGGING_WALL = RequiemCore.id("hugging_wall");
    public static final ResourceLocation CONSUME_RESURRECTION_ITEM = RequiemCore.id("consume_resurrection_item");

    public record UseDirectAbilityPayload(AbilityType abilityType, int entityId) implements CustomPacketPayload {
        public static final Type<UseDirectAbilityPayload> TYPE = new Type<>(USE_DIRECT_ABILITY);
        public static final StreamCodec<RegistryFriendlyByteBuf, UseDirectAbilityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(AbilityType::valueOf, AbilityType::name),
            UseDirectAbilityPayload::abilityType,
            ByteBufCodecs.VAR_INT,
            UseDirectAbilityPayload::entityId,
            UseDirectAbilityPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record HugWallPayload(boolean hugging) implements CustomPacketPayload {
        public static final Type<HugWallPayload> TYPE = new Type<>(HUGGING_WALL);
        public static final StreamCodec<RegistryFriendlyByteBuf, HugWallPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            HugWallPayload::hugging,
            HugWallPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ConsumeResurrectionItemPayload(int entityId, ItemStack stack) implements CustomPacketPayload {
        public static final Type<ConsumeResurrectionItemPayload> TYPE = new Type<>(CONSUME_RESURRECTION_ITEM);
        public static final StreamCodec<RegistryFriendlyByteBuf, ConsumeResurrectionItemPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ConsumeResurrectionItemPayload::entityId,
            ItemStack.OPTIONAL_STREAM_CODEC,
            ConsumeResurrectionItemPayload::stack,
            ConsumeResurrectionItemPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void sendAbilityUseMessage(AbilityType type, Entity entity) {
        PacketDistributor.sendToServer(new UseDirectAbilityPayload(type, entity.getId()));
    }

    public static void sendHugWallMessage(boolean hugging) {
        PacketDistributor.sendToServer(new HugWallPayload(hugging));
    }

    public static void sendItemConsumptionPacket(Entity user, ItemStack stack) {
        sendToAllTrackingIncluding(user, new ConsumeResurrectionItemPayload(user.getId(), stack));
    }

    public static void sendToAllTrackingIncluding(Entity tracked, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(tracked, payload);
    }

    public static void sendToAllTrackingIncluding(Entity tracked, Packet<? super ClientGamePacketListener> message) {
        if (tracked.level() instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().broadcastAndSend(tracked, message);
        }
    }
}

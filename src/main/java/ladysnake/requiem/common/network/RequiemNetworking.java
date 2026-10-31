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
package ladysnake.requiem.common.network;

import ladysnake.requiem.Requiem;
import ladysnake.requiem.api.v1.block.ObeliskDescriptor;
import ladysnake.requiem.api.v1.entity.ability.AbilityType;
import ladysnake.requiem.api.v1.remnant.RemnantType;
import ladysnake.requiem.api.v1.util.SubDataManager;
import ladysnake.requiem.api.v1.util.SubDataManagerHelper;
import ladysnake.requiem.common.block.obelisk.RunestoneBlockEntity;
import ladysnake.requiem.common.remnant.RemnantTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;

import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

import static io.netty.buffer.Unpooled.buffer;

public final class RequiemNetworking {
    // Server -> Client
    public static final ResourceLocation ANCHOR_DAMAGE = Requiem.id("anchor_damage");
    public static final ResourceLocation DATA_SYNC = Requiem.id("data_sync");
    public static final ResourceLocation OPUS_USE = Requiem.id("opus_use");
    public static final ResourceLocation ETHEREAL_ANIMATION = Requiem.id("ethereal_animation");
    public static final ResourceLocation BODY_CURE = Requiem.id("body_cure");
    public static final ResourceLocation OBELISK_POWER_UPDATE = Requiem.id("obelisk_power_update");
    public static final ResourceLocation RIFT_WITNESSED = Requiem.id("rift_witnessed");
    public static final ResourceLocation OPEN_DIALOGUE = Requiem.id("open_dialogue");
    public static final ResourceLocation SHELL_PROFILE_SYNC = Requiem.id("shell_profile_sync");
    public static final ResourceLocation LOCKI_STATE = Requiem.id("locki_state");
    public static final ResourceLocation HOMUNCULUS_BRAIN_OPEN = Requiem.id("homunculus_brain_open");
    public static final ResourceLocation PARMENIDES_SWITCH = Requiem.id("parmenides_switch");
    public static final ResourceLocation PARMENIDES_STATE = Requiem.id("parmenides_state");
    public static final ResourceLocation OUT_OF_BODY_BODY = Requiem.id("out_of_body_body");

    // Client -> Server
    public static final ResourceLocation HOMUNCULUS_BRAIN_SAVE = Requiem.id("homunculus_brain_save");

    // Client -> Server
    public static final ResourceLocation DIALOGUE_ACTION = Requiem.id("dialogue_action");
    public static final ResourceLocation ETHEREAL_FRACTURE = Requiem.id("ethereal_fracture");
    public static final ResourceLocation OPEN_CRAFTING_MENU = Requiem.id("open_crafting");
    public static final ResourceLocation USE_INDIRECT_ABILITY = Requiem.id("indirect_ability");
    public static final ResourceLocation USE_RIFT = Requiem.id("use_rift");
    public static final ResourceLocation CONSTRUCT_ACTION = Requiem.id("construct_action");
    public static final ResourceLocation LOCKI_PLANT_CHOICE = Requiem.id("locki_plant_choice");

    public record LockiStatePayload(int playerEntityId, String mode, int cameraEntityId, int cameraMode, int rotStage) implements CustomPacketPayload {
        public static final Type<LockiStatePayload> TYPE = new Type<>(LOCKI_STATE);
        public static final StreamCodec<RegistryFriendlyByteBuf, LockiStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LockiStatePayload::playerEntityId,
            ByteBufCodecs.STRING_UTF8, LockiStatePayload::mode,
            ByteBufCodecs.VAR_INT, LockiStatePayload::cameraEntityId,
            ByteBufCodecs.VAR_INT, LockiStatePayload::cameraMode,
            ByteBufCodecs.VAR_INT, LockiStatePayload::rotStage,
            LockiStatePayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record HomunculusBrainOpenPayload(int entityId, String program) implements CustomPacketPayload {
        public static final Type<HomunculusBrainOpenPayload> TYPE = new Type<>(HOMUNCULUS_BRAIN_OPEN);
        public static final StreamCodec<RegistryFriendlyByteBuf, HomunculusBrainOpenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HomunculusBrainOpenPayload::entityId,
            ByteBufCodecs.STRING_UTF8, HomunculusBrainOpenPayload::program,
            HomunculusBrainOpenPayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record HomunculusBrainSavePayload(int entityId, String program) implements CustomPacketPayload {
        public static final Type<HomunculusBrainSavePayload> TYPE = new Type<>(HOMUNCULUS_BRAIN_SAVE);
        public static final StreamCodec<RegistryFriendlyByteBuf, HomunculusBrainSavePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HomunculusBrainSavePayload::entityId,
            ByteBufCodecs.STRING_UTF8, HomunculusBrainSavePayload::program,
            HomunculusBrainSavePayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record LockiPlantChoicePayload(int choice) implements CustomPacketPayload {
        public static final Type<LockiPlantChoicePayload> TYPE = new Type<>(LOCKI_PLANT_CHOICE);
        public static final StreamCodec<RegistryFriendlyByteBuf, LockiPlantChoicePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LockiPlantChoicePayload::choice,
            LockiPlantChoicePayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ConstructActionPayload(int actionId) implements CustomPacketPayload {
        public static final Type<ConstructActionPayload> TYPE = new Type<>(CONSTRUCT_ACTION);
        public static final StreamCodec<RegistryFriendlyByteBuf, ConstructActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ConstructActionPayload::actionId,
            ConstructActionPayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record AnchorDamagePayload(boolean dead) implements CustomPacketPayload {
        public static final Type<AnchorDamagePayload> TYPE = new Type<>(ANCHOR_DAMAGE);
        public static final StreamCodec<RegistryFriendlyByteBuf, AnchorDamagePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, AnchorDamagePayload::dead,
            AnchorDamagePayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record DataSyncPayload(byte[] data) implements CustomPacketPayload {
        public static final Type<DataSyncPayload> TYPE = new Type<>(DATA_SYNC);
        public static final StreamCodec<RegistryFriendlyByteBuf, DataSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE_ARRAY, DataSyncPayload::data,
            DataSyncPayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record OpusUsePayload(int rawId, boolean showBook) implements CustomPacketPayload {
        public static final Type<OpusUsePayload> TYPE = new Type<>(OPUS_USE);
        public static final StreamCodec<RegistryFriendlyByteBuf, OpusUsePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpusUsePayload::rawId,
            ByteBufCodecs.BOOL, OpusUsePayload::showBook,
            OpusUsePayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record EtherealAnimationPayload() implements CustomPacketPayload {
        public static final Type<EtherealAnimationPayload> TYPE = new Type<>(ETHEREAL_ANIMATION);
        public static final StreamCodec<RegistryFriendlyByteBuf, EtherealAnimationPayload> STREAM_CODEC = StreamCodec.unit(new EtherealAnimationPayload());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record BodyCurePayload(int entityId) implements CustomPacketPayload {
        public static final Type<BodyCurePayload> TYPE = new Type<>(BODY_CURE);
        public static final StreamCodec<RegistryFriendlyByteBuf, BodyCurePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BodyCurePayload::entityId,
            BodyCurePayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ObeliskPowerUpdatePayload(BlockPos pos, int coreWidth, int coreHeight, float powerRate) implements CustomPacketPayload {
        public static final Type<ObeliskPowerUpdatePayload> TYPE = new Type<>(OBELISK_POWER_UPDATE);
        public static final StreamCodec<RegistryFriendlyByteBuf, ObeliskPowerUpdatePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ObeliskPowerUpdatePayload::pos,
            ByteBufCodecs.VAR_INT, ObeliskPowerUpdatePayload::coreWidth,
            ByteBufCodecs.VAR_INT, ObeliskPowerUpdatePayload::coreHeight,
            ByteBufCodecs.FLOAT, ObeliskPowerUpdatePayload::powerRate,
            ObeliskPowerUpdatePayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record RiftWitnessedPayload(Component obeliskName) implements CustomPacketPayload {
        public static final Type<RiftWitnessedPayload> TYPE = new Type<>(RIFT_WITNESSED);
        public static final StreamCodec<RegistryFriendlyByteBuf, RiftWitnessedPayload> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, RiftWitnessedPayload::obeliskName,
            RiftWitnessedPayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record DialogueActionPayload(int choice) implements CustomPacketPayload {
        public static final Type<DialogueActionPayload> TYPE = new Type<>(DIALOGUE_ACTION);
        public static final StreamCodec<RegistryFriendlyByteBuf, DialogueActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DialogueActionPayload::choice,
            DialogueActionPayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record EtherealFracturePayload() implements CustomPacketPayload {
        public static final Type<EtherealFracturePayload> TYPE = new Type<>(ETHEREAL_FRACTURE);
        public static final StreamCodec<RegistryFriendlyByteBuf, EtherealFracturePayload> STREAM_CODEC = StreamCodec.unit(new EtherealFracturePayload());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ParmenidesSwitchPayload() implements CustomPacketPayload {
        public static final Type<ParmenidesSwitchPayload> TYPE = new Type<>(PARMENIDES_SWITCH);
        public static final StreamCodec<RegistryFriendlyByteBuf, ParmenidesSwitchPayload> STREAM_CODEC = StreamCodec.unit(new ParmenidesSwitchPayload());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ParmenidesStatePayload(int entityId, boolean active) implements CustomPacketPayload {
        public static final Type<ParmenidesStatePayload> TYPE = new Type<>(PARMENIDES_STATE);
        public static final StreamCodec<RegistryFriendlyByteBuf, ParmenidesStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ParmenidesStatePayload::entityId,
            ByteBufCodecs.BOOL, ParmenidesStatePayload::active,
            ParmenidesStatePayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record OutOfBodyBodyPayload(int entityId, boolean active) implements CustomPacketPayload {
        public static final Type<OutOfBodyBodyPayload> TYPE = new Type<>(OUT_OF_BODY_BODY);
        public static final StreamCodec<RegistryFriendlyByteBuf, OutOfBodyBodyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OutOfBodyBodyPayload::entityId,
            ByteBufCodecs.BOOL, OutOfBodyBodyPayload::active,
            OutOfBodyBodyPayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record OpenCraftingPayload() implements CustomPacketPayload {
        public static final Type<OpenCraftingPayload> TYPE = new Type<>(OPEN_CRAFTING_MENU);
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenCraftingPayload> STREAM_CODEC = StreamCodec.unit(new OpenCraftingPayload());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record UseIndirectAbilityPayload(AbilityType abilityType) implements CustomPacketPayload {
        public static final Type<UseIndirectAbilityPayload> TYPE = new Type<>(USE_INDIRECT_ABILITY);
        public static final StreamCodec<RegistryFriendlyByteBuf, UseIndirectAbilityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(AbilityType::valueOf, AbilityType::name), UseIndirectAbilityPayload::abilityType,
            UseIndirectAbilityPayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record UseRiftPayload(ObeliskDescriptor target) implements CustomPacketPayload {
        public static final Type<UseRiftPayload> TYPE = new Type<>(USE_RIFT);
        public static final StreamCodec<RegistryFriendlyByteBuf, UseRiftPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodecWithRegistries(ObeliskDescriptor.CODEC), UseRiftPayload::target,
            UseRiftPayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendLockiPlantChoice(int choice) {
        PacketDistributor.sendToServer(new LockiPlantChoicePayload(choice));
    }

    public static void sendHomunculusBrainOpen(ServerPlayer player, int entityId, String program) {
        PacketDistributor.sendToPlayer(player, new HomunculusBrainOpenPayload(entityId, program == null ? "" : program));
    }

    public static void sendHomunculusBrain(int entityId, String program) {
        PacketDistributor.sendToServer(new HomunculusBrainSavePayload(entityId, program == null ? "" : program));
    }

    public static void sendTo(ServerPlayer player, Packet<?> message) {
        if (player.connection != null) {
            player.connection.send(message);
        }
    }

    public static Packet<?> createOpusUsePacket(RemnantType chosenType, boolean showBook) {
        return new ClientboundCustomPayloadPacket(new OpusUsePayload(RemnantTypes.getRawId(chosenType), showBook));
    }

    public static Packet<?> createDataSyncMessage(SubDataManagerHelper helper) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(buffer(), net.minecraft.core.RegistryAccess.EMPTY);
        List<SubDataManager<?>> managers = helper.streamDataManagers().toList();
        buf.writeVarInt(managers.size());
        for (SubDataManager<?> manager : managers) {
            Requiem.LOGGER.info("[Requiem] Synchronizing data for {} ({})", manager.getFabricId(), manager);
            buf.writeResourceLocation(manager.getFabricId());
            manager.toPacket(buf);
        }
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return new ClientboundCustomPayloadPacket(new DataSyncPayload(bytes));
    }

    public static void sendDialogueActionMessage(int choice) {
        PacketDistributor.sendToServer(new DialogueActionPayload(choice));
    }

    public static void sendRiftUseMessage(ObeliskDescriptor target) {
        PacketDistributor.sendToServer(new UseRiftPayload(target));
    }

    public static void sendIndirectAbilityUseMessage(AbilityType type) {
        PacketDistributor.sendToServer(new UseIndirectAbilityPayload(type));
    }

    public static void sendSupercrafterMessage() {
        PacketDistributor.sendToServer(new OpenCraftingPayload());
    }

    public static void sendEtherealAnimationMessage(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new EtherealAnimationPayload());
    }

    public static void sendBodyCureMessage(LivingEntity entity) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new BodyCurePayload(entity.getId()));
    }

    public record OpenDialoguePayload(String dialogueId) implements CustomPacketPayload {
        public static final Type<OpenDialoguePayload> TYPE = new Type<>(OPEN_DIALOGUE);
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenDialoguePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenDialoguePayload::dialogueId,
            OpenDialoguePayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void sendOpenDialogueMessage(ServerPlayer player, String dialogueId) {
        PacketDistributor.sendToPlayer(player, new OpenDialoguePayload(dialogueId));
    }

    public static void sendAnchorDamageMessage(ServerPlayer player, boolean dead) {
        PacketDistributor.sendToPlayer(player, new AnchorDamagePayload(dead));
    }

    public static void sendObeliskPowerUpdateMessage(RunestoneBlockEntity runestone) {
        if (runestone.getLevel() instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersTrackingChunk(
                serverLevel,
                new ChunkPos(runestone.getBlockPos()),
                new ObeliskPowerUpdatePayload(runestone.getBlockPos(), runestone.getCoreWidth(), runestone.getCoreHeight(), runestone.getPowerRate())
            );
        }
    }

    public static void sendRiftWitnessedMessage(ServerPlayer player, Component obeliskName) {
        PacketDistributor.sendToPlayer(player, new RiftWitnessedPayload(obeliskName));
    }

    public record ShellProfileSyncPayload(int entityId, com.mojang.authlib.GameProfile profile) implements CustomPacketPayload {
        public static final Type<ShellProfileSyncPayload> TYPE = new Type<>(SHELL_PROFILE_SYNC);
        public static final StreamCodec<RegistryFriendlyByteBuf, ShellProfileSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ShellProfileSyncPayload::entityId,
            ByteBufCodecs.GAME_PROFILE.cast(), ShellProfileSyncPayload::profile,
            ShellProfileSyncPayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void sendShellProfileSyncMessage(ServerPlayer player, int entityId, com.mojang.authlib.GameProfile profile) {
        PacketDistributor.sendToPlayer(player, new ShellProfileSyncPayload(entityId, profile));
    }
}

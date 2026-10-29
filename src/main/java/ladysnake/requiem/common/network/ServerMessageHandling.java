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

import ladysnake.requiem.api.v1.entity.MovementAlterer;
import ladysnake.requiem.api.v1.entity.ability.MobAbilityController;
import ladysnake.requiem.api.v1.event.requiem.InitiateFractureCallback;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.common.screen.RiftScreenHandler;
import ladysnake.requiem.common.tag.RequiemEntityTypeTags;
import ladysnake.requiem.core.RequiemCoreNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ServerMessageHandling {

    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0.0");

        ladysnake.requiem.common.sharedbody.SharedBodyNetworking.register(registrar);

        // Core payloads
        registrar.playToServer(
            RequiemCoreNetworking.UseDirectAbilityPayload.TYPE,
            RequiemCoreNetworking.UseDirectAbilityPayload.STREAM_CODEC,
            ServerMessageHandling::handleUseDirectAbility
        );
        registrar.playToServer(
            RequiemCoreNetworking.HugWallPayload.TYPE,
            RequiemCoreNetworking.HugWallPayload.STREAM_CODEC,
            ServerMessageHandling::handleHugWall
        );
        registrar.playToClient(
            RequiemCoreNetworking.ConsumeResurrectionItemPayload.TYPE,
            RequiemCoreNetworking.ConsumeResurrectionItemPayload.STREAM_CODEC,
            ServerMessageHandling::handleConsumeResurrectionItem
        );

        // Requiem payloads
        registrar.playToServer(
            RequiemNetworking.UseIndirectAbilityPayload.TYPE,
            RequiemNetworking.UseIndirectAbilityPayload.STREAM_CODEC,
            ServerMessageHandling::handleUseIndirectAbility
        );
        registrar.playToServer(
            RequiemNetworking.EtherealFracturePayload.TYPE,
            RequiemNetworking.EtherealFracturePayload.STREAM_CODEC,
            ServerMessageHandling::handleEtherealFracture
        );
        registrar.playToServer(
            RequiemNetworking.OpenCraftingPayload.TYPE,
            RequiemNetworking.OpenCraftingPayload.STREAM_CODEC,
            ServerMessageHandling::handleOpenCrafting
        );
        registrar.playToServer(
            RequiemNetworking.UseRiftPayload.TYPE,
            RequiemNetworking.UseRiftPayload.STREAM_CODEC,
            ServerMessageHandling::handleUseRift
        );
        registrar.playToServer(
            RequiemNetworking.DialogueActionPayload.TYPE,
            RequiemNetworking.DialogueActionPayload.STREAM_CODEC,
            ServerMessageHandling::handleDialogueAction
        );
        registrar.playToServer(
            RequiemNetworking.ConstructActionPayload.TYPE,
            RequiemNetworking.ConstructActionPayload.STREAM_CODEC,
            ServerMessageHandling::handleConstructAction
        );
        registrar.playToServer(
            RequiemNetworking.LockiPlantChoicePayload.TYPE,
            RequiemNetworking.LockiPlantChoicePayload.STREAM_CODEC,
            ServerMessageHandling::handleLockiPlantChoice
        );
        registrar.playToServer(
            RequiemNetworking.HomunculusBrainSavePayload.TYPE,
            RequiemNetworking.HomunculusBrainSavePayload.STREAM_CODEC,
            ServerMessageHandling::handleHomunculusBrainSave
        );

        // Clientbound codecs are declared here, but actual client handlers are registered
        // in RegisterClientPayloadHandlersEvent to keep client-only classes off the server.
        registerClientboundPayloads(registrar);
    }

    private static void handleConsumeResurrectionItem(RequiemCoreNetworking.ConsumeResurrectionItemPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {});
    }

    private static void handleAnchorDamage(RequiemNetworking.AnchorDamagePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {});
    }

    private static void handleDataSync(RequiemNetworking.DataSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {});
    }

    private static void handleOpusUse(RequiemNetworking.OpusUsePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {});
    }

    private static void handleEtherealAnimation(RequiemNetworking.EtherealAnimationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {});
    }

    private static void handleBodyCure(RequiemNetworking.BodyCurePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {});
    }

    private static void handleObeliskPowerUpdate(RequiemNetworking.ObeliskPowerUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {});
    }

    private static void handleRiftWitnessed(RequiemNetworking.RiftWitnessedPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {});
    }

    public static void init() {
        // Event bus handles registration
    }

    private static void registerClientboundPayloads(PayloadRegistrar registrar) {
        // Keep the payload IDs/codecs available to the physical server. The client installs
        // the real handlers via RegisterClientPayloadHandlersEvent.
        registrar.playToClient(RequiemNetworking.AnchorDamagePayload.TYPE, RequiemNetworking.AnchorDamagePayload.STREAM_CODEC);
        registrar.playToClient(RequiemNetworking.DataSyncPayload.TYPE, RequiemNetworking.DataSyncPayload.STREAM_CODEC);
        registrar.playToClient(RequiemNetworking.OpusUsePayload.TYPE, RequiemNetworking.OpusUsePayload.STREAM_CODEC);
        registrar.playToClient(RequiemNetworking.EtherealAnimationPayload.TYPE, RequiemNetworking.EtherealAnimationPayload.STREAM_CODEC);
        registrar.playToClient(RequiemNetworking.BodyCurePayload.TYPE, RequiemNetworking.BodyCurePayload.STREAM_CODEC);
        registrar.playToClient(RequiemNetworking.ObeliskPowerUpdatePayload.TYPE, RequiemNetworking.ObeliskPowerUpdatePayload.STREAM_CODEC);
        registrar.playToClient(RequiemNetworking.RiftWitnessedPayload.TYPE, RequiemNetworking.RiftWitnessedPayload.STREAM_CODEC);
        registrar.playToClient(RequiemNetworking.OpenDialoguePayload.TYPE, RequiemNetworking.OpenDialoguePayload.STREAM_CODEC);
        registrar.playToClient(RequiemNetworking.ShellProfileSyncPayload.TYPE, RequiemNetworking.ShellProfileSyncPayload.STREAM_CODEC);
        registrar.playToClient(RequiemNetworking.LockiStatePayload.TYPE, RequiemNetworking.LockiStatePayload.STREAM_CODEC);
        registrar.playToClient(RequiemNetworking.HomunculusBrainOpenPayload.TYPE, RequiemNetworking.HomunculusBrainOpenPayload.STREAM_CODEC);
    }



    private static void handleUseDirectAbility(RequiemCoreNetworking.UseDirectAbilityPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            MobAbilityController abilityController = MobAbilityController.get(player);
            Entity targetedEntity = player.level().getEntity(payload.entityId());

            if (targetedEntity != null && (abilityController.getRange(payload.abilityType()) + 3) > targetedEntity.distanceTo(player)) {
                abilityController.useDirect(payload.abilityType(), targetedEntity);
            }

            MobAbilityController.KEY.sync(player);
        }
    }

    private static void handleUseIndirectAbility(RequiemNetworking.UseIndirectAbilityPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            MobAbilityController.get(player).useIndirect(payload.abilityType());
        }
    }

    private static void handleEtherealFracture(RequiemNetworking.EtherealFracturePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            InitiateFractureCallback.EVENT.invoker().performFracture(player);
        }
    }

    private static void handleHugWall(RequiemCoreNetworking.HugWallPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            MovementAlterer.get(player).hugWall(payload.hugging());
        }
    }

    private static void handleOpenCrafting(RequiemNetworking.OpenCraftingPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            Mob possessed = PossessionComponent.get(player).getHost();
            if (possessed != null && possessed.getType().builtInRegistryHolder().is(RequiemEntityTypeTags.SUPERCRAFTERS)) {
                player.openMenu(Blocks.CRAFTING_TABLE.defaultBlockState().getMenuProvider(player.level(), player.blockPosition()));
            }
        }
    }

    private static void handleUseRift(RequiemNetworking.UseRiftPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            if (player.containerMenu instanceof RiftScreenHandler riftScreenHandler) {
                riftScreenHandler.useRift(player, payload.target());
            }
        }
    }

    private static void handleDialogueAction(RequiemNetworking.DialogueActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ladysnake.requiem.api.v1.remnant.RemnantComponent remnant = ladysnake.requiem.api.v1.remnant.RemnantComponent.get(player);
                ladysnake.requiem.api.v1.remnant.RemnantType type = (payload.choice() == 0)
                    ? ladysnake.requiem.common.remnant.RemnantTypes.REMNANT
                    : ladysnake.requiem.common.remnant.RemnantTypes.MORTAL;
                remnant.become(type, true);
                ladysnake.requiem.api.v1.remnant.DeathSuspender suspender = ladysnake.requiem.api.v1.remnant.DeathSuspender.get(player);
                if (suspender.isLifeTransient()) {
                    suspender.resumeDeath();
                }
            }
        });
    }

    private static void handleLockiPlantChoice(RequiemNetworking.LockiPlantChoicePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ladysnake.requiem.common.locki.LockiSpiritSystem.handlePlantChoice(player, payload.choice());
            }
        });
    }

    private static void handleHomunculusBrainSave(RequiemNetworking.HomunculusBrainSavePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ladysnake.requiem.common.locki.MarbasAbilitySystem.applyBrainProgram(player, payload.entityId(), payload.program());
            }
        });
    }

    private static void handleConstructAction(RequiemNetworking.ConstructActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Mob host = ladysnake.requiem.api.v1.possession.PossessionComponent.get(player).getHost();
                if (host instanceof ladysnake.requiem.common.construct.entity.RoboticConstructEntity construct) {
                    construct.performConstructAction(payload.actionId());
                }
            }
        });
    }
}

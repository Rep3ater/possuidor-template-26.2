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
package ladysnake.requiem.common.possession.item;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.api.v1.util.MoreCodecs;
import ladysnake.requiem.common.RequiemRegistries;
import ladysnake.requiem.core.data.LazyEntityPredicate;
import ladysnake.requiem.core.util.serde.PolymorphicCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public record PossessionItemOverrideWrapper(
    int priority,
    boolean enabled,
    Optional<Component> tooltip,
    LazyEntityPredicate mob,
    PossessionItemOverride override
) implements Comparable<PossessionItemOverrideWrapper> {
    public static final int CURRENT_SCHEMA_VERSION = 0;

    // the fun bit is that V0 is both a schema version and an override type, so we need to flatten some stuff here
    public static final Codec<PossessionItemOverrideWrapper> CODEC_V0 = RecordCodecBuilder.create((instance) -> instance.group(
        Codec.INT.optionalFieldOf("priority", 100).forGetter(PossessionItemOverrideWrapper::priority),
        Codec.BOOL.optionalFieldOf("enabled", true).forGetter(PossessionItemOverrideWrapper::enabled),
        MoreCodecs.text(MoreCodecs.DYNAMIC_JSON).optionalFieldOf("tooltip").forGetter(w -> w.tooltip),
        OldPossessionItemOverride.Requirements.codec(MoreCodecs.DYNAMIC_JSON).fieldOf("requirements").forGetter(o -> ((OldPossessionItemOverride) o.override).requirements()),
        Codec.INT.optionalFieldOf("use_time", 0).forGetter(w -> ((OldPossessionItemOverride) w.override).useTime()),
        OldPossessionItemOverride.Result.CODEC.fieldOf("result").forGetter(w -> ((OldPossessionItemOverride) w.override).result())
    ).apply(instance, (p, e, t, req, u, res) -> new PossessionItemOverrideWrapper(p, e, t, req.possessed, new OldPossessionItemOverride(req, u, res))));

    public static final Codec<PossessionItemOverrideWrapper> CODEC_V1 = codecV1(MoreCodecs.DYNAMIC_JSON);

    public static final Codec<PossessionItemOverrideWrapper> CODEC = PolymorphicCodecBuilder.create("schema_version", Codec.INT, (PossessionItemOverrideWrapper o) -> CURRENT_SCHEMA_VERSION)
        .with(0, CODEC_V0)
        .with(1, CODEC_V1)
        .build()
        .xmap(PossessionItemOverrideWrapper::initNow, Function.identity());

    // The compressed NBT codec used by RegistryFriendlyByteBuf#encode fails on nulls, so we cannot use regular JSON objects
    public static final Codec<PossessionItemOverrideWrapper> NETWORK_CODEC = codecV1(MoreCodecs.STRING_JSON);

    private static Codec<PossessionItemOverrideWrapper> codecV1(Codec<JsonElement> jsonCodec) {
        return RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("priority", 100).forGetter(PossessionItemOverrideWrapper::priority),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(PossessionItemOverrideWrapper::enabled),
            MoreCodecs.text(jsonCodec).optionalFieldOf("tooltip").forGetter(o -> o.tooltip),
            LazyEntityPredicate.codec(jsonCodec).fieldOf("mob").forGetter(o -> o.mob),
            overrideCodecV1(jsonCodec).fieldOf("override").forGetter(w -> w.override)
        ).apply(instance, PossessionItemOverrideWrapper::new));
    }

    private static Codec<PossessionItemOverride> overrideCodecV1(Codec<JsonElement> jsonCodec) {
        // I promise I will make this a registry at some point
        return PolymorphicCodecBuilder.create("type", ResourceLocation.CODEC, PossessionItemOverride::getType)
            .with(OldPossessionItemOverride.ID, OldPossessionItemOverride.codec(jsonCodec))
            .with(DietItemOverride.ID, DietItemOverride.codec(jsonCodec))
            .with(HealingItemOverride.ID, HealingItemOverride.codec(jsonCodec))
            .with(CureItemOverride.ID, CureItemOverride.codec(jsonCodec))
            .build();
    }

    public static Optional<InteractionResult> tryUseOverride(Level world, Player player, ItemStack heldStack, InteractionHand hand) {
        OverridableItemStack.get(heldStack).requiem$clearOverriddenUseTime();

        Mob possessedEntity = PossessionComponent.getHost(player);
        if (possessedEntity != null) {
            return PossessionItemOverrideWrapper.findOverride(world, player, possessedEntity, heldStack)
                .map(override -> override.use(player, possessedEntity, heldStack, world, hand))
                .filter(res -> res != InteractionResult.PASS);
        }

        return Optional.empty();
    }

    public static Optional<InteractionResult> tryFinishUsingOverride(Level world, Player player, ItemStack heldStack, InteractionHand hand) {
        OverridableItemStack.get(heldStack).requiem$clearOverriddenUseTime();

        Mob possessedEntity = PossessionComponent.getHost(player);
        if (possessedEntity != null) {
            return PossessionItemOverrideWrapper.findOverride(world, player, possessedEntity, heldStack)
                .map(override -> override.finishUsing(player, possessedEntity, heldStack, world, hand))
                .filter(res -> res != InteractionResult.PASS);
        }

        return Optional.empty();
    }

    public static Optional<InstancedItemOverride> findOverride(Level world, Player player, Mob possessedEntity, ItemStack heldStack) {
        Optional<InstancedItemOverride> fallback = Optional.empty();
        var regOpt = world.registryAccess().lookup(RequiemRegistries.MOB_ITEM_OVERRIDE_KEY);
        if (regOpt.isEmpty()) {
            return fallback;
        }
        for (PossessionItemOverrideWrapper wrapper : regOpt.get().listElements().map(Holder::value).sorted().toList()) {
            Optional<InstancedItemOverride> tested = wrapper.test(player, possessedEntity, heldStack);
            if (tested.isPresent()) {
                if (tested.get().shortCircuits()) {
                    return tested;
                } else if (fallback.isEmpty()) {
                    fallback = tested;
                }
            }
        }
        return fallback;
    }

    public static List<Component> buildTooltip(Level world, Player player, Mob possessedEntity, ItemStack heldStack) {
        List<Component> lines = new ArrayList<>();
        var regOpt = world.registryAccess().lookup(RequiemRegistries.MOB_ITEM_OVERRIDE_KEY);
        if (regOpt.isEmpty()) {
            return lines;
        }
        for (PossessionItemOverrideWrapper wrapper : regOpt.get().listElements().map(Holder::value).sorted().toList()) {
            Optional<InstancedItemOverride> tested = wrapper.test(player, possessedEntity, heldStack);
            if (tested.isPresent()) {
                InstancedItemOverride override = tested.get();
                if (override.shortCircuits()) {
                    return wrapper.tooltip.flatMap(override::tweakTooltip).map(Collections::singletonList).orElse(Collections.emptyList());
                } else {
                    wrapper.tooltip.flatMap(override::tweakTooltip).ifPresent(lines::add);
                }
            }
        }
        return lines;
    }

    public Optional<InstancedItemOverride> test(Player player, Mob host, ItemStack stack) {
        return this.enabled() && this.mob.test(host) ? this.override.test(player, host, stack) : Optional.empty();
    }

    public PossessionItemOverrideWrapper initNow() {
        this.mob.initNow();
        this.override.initNow();
        return this;
    }

    @Override
    public int compareTo(@NotNull PossessionItemOverrideWrapper o) {
        return o.priority - this.priority;
    }
}

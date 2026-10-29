package io.github.ladysnake.pal;

import io.github.ladysnake.pal.impl.VanillaAbilityTracker;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForgeMod;

public final class VanillaAbilities {
    public static final ResourceLocation CREATIVE_FLIGHT_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("requiem", "pal_allow_flying");

    public static final PlayerAbility ALLOW_FLYING = Pal.registerAbility(
        ResourceLocation.withDefaultNamespace("allow_flying"),
        (ability, player) -> new VanillaAbilityTracker(
            ability,
            player,
            abilities -> abilities.mayfly,
            (abilities, enabled) -> {
                AttributeInstance flightAttr = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
                if (flightAttr != null) {
                    if (enabled) {
                        if (!flightAttr.hasModifier(CREATIVE_FLIGHT_MODIFIER_ID)) {
                            flightAttr.addTransientModifier(new AttributeModifier(
                                CREATIVE_FLIGHT_MODIFIER_ID,
                                1.0,
                                AttributeModifier.Operation.ADD_VALUE
                            ));
                        }
                    } else {
                        flightAttr.removeModifier(CREATIVE_FLIGHT_MODIFIER_ID);
                    }
                }
                boolean mayFlyNow = enabled || player.isCreative() || player.isSpectator();
                abilities.mayfly = mayFlyNow;
                if (!mayFlyNow) {
                    abilities.flying = false;
                }
            }
        )
    );

    /**
     * Alias provided for downstream flight-limiting lookups.
     */
    public static final PlayerAbility LIMIT_FLIGHT = ALLOW_FLYING;

    public static final PlayerAbility FLYING = Pal.registerAbility(
        ResourceLocation.withDefaultNamespace("flying"),
        createTrackerFactory(
            abilities -> abilities.flying,
            (abilities, enabled) -> abilities.flying = enabled
        )
    );

    public static final PlayerAbility INVULNERABLE = Pal.registerAbility(
        ResourceLocation.withDefaultNamespace("invulnerable"),
        (ability, player) -> new VanillaAbilityTracker(
            ability,
            player,
            abilities -> abilities.invulnerable,
            (abilities, enabled) -> abilities.invulnerable = enabled || player.isCreative() || player.isSpectator()
        )
    );

    public static final PlayerAbility CREATIVE_MODE = Pal.registerAbility(
        ResourceLocation.withDefaultNamespace("creative_mode"),
        (ability, player) -> new VanillaAbilityTracker(
            ability,
            player,
            abilities -> abilities.instabuild,
            (abilities, enabled) -> abilities.instabuild = enabled || player.isCreative()
        )
    );

    public static final PlayerAbility LIMIT_WORLD_MODIFICATIONS = Pal.registerAbility(
        ResourceLocation.fromNamespaceAndPath("pal", "limit_world_modifications"),
        (ability, player) -> new VanillaAbilityTracker(
            ability,
            player,
            abilities -> !abilities.mayBuild,
            (abilities, limited) -> abilities.mayBuild = !limited && !player.isSpectator()
        )
    );

    private VanillaAbilities() {
    }

    public static void init() {
        // Trigger static field registration
    }

    private static java.util.function.BiFunction<PlayerAbility, Player, AbilityTracker> createTrackerFactory(
        Predicate<Abilities> getter,
        BiConsumer<Abilities, Boolean> setter
    ) {
        return (ability, player) -> new VanillaAbilityTracker(ability, player, getter, setter);
    }
}

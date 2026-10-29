package io.github.ladysnake.pal;

import io.github.ladysnake.pal.impl.PalInternals;
import java.util.Objects;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class Pal {
    private Pal() {
    }

    public static AbilitySource getAbilitySource(String namespace, String name) {
        return getAbilitySource(ResourceLocation.fromNamespaceAndPath(namespace, name));
    }

    public static AbilitySource getAbilitySource(ResourceLocation abilitySourceId) {
        return getAbilitySource(abilitySourceId, AbilitySource.DEFAULT);
    }

    public static AbilitySource getAbilitySource(String namespace, String name, int priority) {
        return getAbilitySource(ResourceLocation.fromNamespaceAndPath(namespace, name), priority);
    }

    public static AbilitySource getAbilitySource(ResourceLocation abilitySourceId, int priority) {
        return PalInternals.getSource(Objects.requireNonNull(abilitySourceId, "abilitySourceId"), priority);
    }

    public static void grantAbility(Player player, PlayerAbility ability, AbilitySource abilitySource) {
        abilitySource.grantTo(player, ability);
    }

    public static void revokeAbility(Player player, PlayerAbility ability, AbilitySource abilitySource) {
        abilitySource.revokeFrom(player, ability);
    }

    public static void revokeAllAbilities(Player player, AbilitySource abilitySource) {
        abilitySource.revokeAllFrom(player);
    }

    public static boolean providesExternally(Player player, PlayerAbility ability) {
        return ability.isEnabledFor(player);
    }

    public static PlayerAbility registerAbility(String namespace, String name, BiFunction<PlayerAbility, Player, AbilityTracker> factory) {
        return registerAbility(ResourceLocation.fromNamespaceAndPath(namespace, name), factory);
    }

    public static PlayerAbility registerAbility(ResourceLocation abilityId, BiFunction<PlayerAbility, Player, AbilityTracker> factory) {
        return PalInternals.registerAbility(Objects.requireNonNull(abilityId, "abilityId"), Objects.requireNonNull(factory, "factory"));
    }

    @Nullable
    public static PlayerAbility getRegisteredAbility(ResourceLocation abilityId) {
        return PalInternals.getAbility(abilityId);
    }
}

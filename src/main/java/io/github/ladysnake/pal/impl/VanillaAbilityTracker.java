package io.github.ladysnake.pal.impl;

import io.github.ladysnake.pal.PlayerAbility;
import io.github.ladysnake.pal.SimpleAbilityTracker;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;

public class VanillaAbilityTracker extends SimpleAbilityTracker {
    private final Predicate<Abilities> getter;
    private final BiConsumer<Abilities, Boolean> setter;

    public VanillaAbilityTracker(
        PlayerAbility ability,
        Player player,
        Predicate<Abilities> getter,
        BiConsumer<Abilities, Boolean> setter
    ) {
        super(ability, player, true, true);
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    protected void updateBackingAttribute(boolean enabled) {
        this.setter.accept(this.player.getAbilities(), enabled);
    }

    @Override
    protected void sync() {
        if (this.player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            serverPlayer.onUpdateAbilities();
        }
        super.sync();
    }

    @Override
    public boolean isEnabled() {
        if (this.player.level().isClientSide()) {
            return super.isEnabled() || this.getter.test(this.player.getAbilities());
        }
        return super.isEnabled();
    }
}

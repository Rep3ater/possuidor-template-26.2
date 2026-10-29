package io.github.ladysnake.pal;

import io.github.ladysnake.pal.impl.PalInternals;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class SimpleAbilityTracker implements AbilityTracker {
    protected final Player player;
    protected final PlayerAbility ability;
    protected final SortedSet<AbilitySource> abilitySources = new TreeSet<>();
    private final boolean shouldSave;
    private final boolean shouldSync;

    public SimpleAbilityTracker(PlayerAbility ability, Player player) {
        this(ability, player, true, false);
    }

    public SimpleAbilityTracker(PlayerAbility ability, Player player, boolean saving, boolean synced) {
        this.ability = ability;
        this.player = player;
        this.shouldSave = saving;
        this.shouldSync = synced;
    }

    @Override
    public void addSource(AbilitySource abilitySource) {
        boolean wasEnabled = this.isEnabled();
        if (this.abilitySources.add(abilitySource)) {
            this.updateState(wasEnabled);
        }
    }

    @Override
    public void removeSource(AbilitySource abilitySource) {
        boolean wasEnabled = this.isEnabled();
        if (this.abilitySources.remove(abilitySource)) {
            this.updateState(wasEnabled);
        }
    }

    protected void updateState(boolean wasEnabled) {
        boolean enabled = this.isEnabled();
        this.updateBackingAttribute(enabled);
        if (this.shouldSync) {
            this.sync();
        }
        if (wasEnabled != enabled) {
            this.ability.getUpdatedEvent().invoker().onAbilityUpdated(this.player, enabled);
        }
    }

    protected void updateBackingAttribute(boolean enabled) {
    }

    protected void sync() {
        PalInternals.syncAbility(this.player);
    }

    @Override
    public boolean isGrantedBy(AbilitySource abilitySource) {
        return this.abilitySources.contains(abilitySource);
    }

    @Nullable
    @Override
    public AbilitySource getActiveSource() {
        for (AbilitySource source : this.abilitySources) {
            if (PlayerAbilityEnableCallback.EVENT.invoker().allow(this.player, this.ability, source)) {
                return source;
            }
        }
        return null;
    }

    @Override
    public void refresh(boolean sync) {
        this.updateBackingAttribute(this.isEnabled());
        if (sync && (this.shouldSync || this.ability == VanillaAbilities.ALLOW_FLYING || this.ability == VanillaAbilities.FLYING || this.ability == VanillaAbilities.INVULNERABLE || this.ability == VanillaAbilities.CREATIVE_MODE)) {
            this.sync();
        }
    }

    @Override
    public boolean isEnabled() {
        if (this.player.level().isClientSide()) {
            PalInternals.PalPlayerComponent comp = PalInternals.PAL_COMPONENT.getNullable(this.player);
            if (comp != null && comp.isSyncedEnabled(this.ability)) {
                return true;
            }
        }
        return this.getActiveSource() != null;
    }

    @Override
    public void save(CompoundTag tag) {
        if (!this.shouldSave || this.abilitySources.isEmpty()) {
            return;
        }
        ListTag list = new ListTag();
        for (AbilitySource source : this.abilitySources) {
            list.add(StringTag.valueOf(source.getId().toString()));
        }
        tag.put("ability_sources", list);
    }

    @Override
    public void load(CompoundTag tag) {
        if (!this.shouldSave) {
            return;
        }
        this.abilitySources.clear();
        ListTag list = tag.getList("ability_sources", Tag.TAG_STRING);
        for (Tag element : list) {
            String str = element.getAsString();
            ResourceLocation id = ResourceLocation.tryParse(str);
            if (id != null) {
                this.abilitySources.add(Pal.getAbilitySource(id));
            }
        }
    }
}

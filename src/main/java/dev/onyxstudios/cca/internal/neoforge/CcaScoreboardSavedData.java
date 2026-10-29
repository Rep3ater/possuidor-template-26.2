package dev.onyxstudios.cca.internal.neoforge;

import dev.onyxstudios.cca.internal.base.AbstractComponentContainer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.Objects;

public final class CcaScoreboardSavedData extends SavedData {
    private final ServerLevel level;
    private final AbstractComponentContainer container;

    public CcaScoreboardSavedData(@Nullable ServerLevel level) {
        this.level = Objects.requireNonNull(level, "ServerLevel required for CcaScoreboardSavedData");
        this.container = CcaNeoForgeEngine.createScoreboardContainer(level.getScoreboard(), level.getServer());
    }

    public static CcaScoreboardSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(
                () -> new CcaScoreboardSavedData(level),
                (tag, registries) -> load(level, tag)
            ),
            "requiem_cca_scoreboard"
        );
    }

    private static CcaScoreboardSavedData load(ServerLevel level, CompoundTag tag) {
        CcaScoreboardSavedData data = new CcaScoreboardSavedData(level);
        data.container.fromTag(tag, level.registryAccess());
        return data;
    }

    public AbstractComponentContainer getContainer() {
        return this.container;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        this.container.toTag(tag, this.level.registryAccess());
        return tag;
    }

    @Override
    public boolean isDirty() {
        return true;
    }
}

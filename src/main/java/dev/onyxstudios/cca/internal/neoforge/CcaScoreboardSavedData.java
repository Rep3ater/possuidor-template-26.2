package dev.onyxstudios.cca.internal.neoforge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.onyxstudios.cca.internal.base.AbstractComponentContainer;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class CcaScoreboardSavedData extends SavedData {
    public static final SavedDataType<CcaScoreboardSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("requiem", "cca_scoreboard"),
        CcaScoreboardSavedData::new,
        CcaScoreboardSavedData::makeCodec
    );

    private final ServerLevel level;
    private final AbstractComponentContainer container;

    public CcaScoreboardSavedData(@Nullable ServerLevel level) {
        this.level = Objects.requireNonNull(level, "ServerLevel required for CcaScoreboardSavedData");
        this.container = CcaNeoForgeEngine.createScoreboardContainer(level.getScoreboard(), level.getServer());
    }

    public AbstractComponentContainer getContainer() {
        return this.container;
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    private static Codec<CcaScoreboardSavedData> makeCodec(@Nullable ServerLevel level) {
        return CompoundTag.CODEC.flatXmap(
            tag -> {
                CcaScoreboardSavedData data = new CcaScoreboardSavedData(level);
                data.container.fromTag(tag, data.level.registryAccess());
                return DataResult.success(data);
            },
            data -> {
                CompoundTag tag = new CompoundTag();
                data.container.toTag(tag, data.level.registryAccess());
                return DataResult.success(tag);
            }
        );
    }
}

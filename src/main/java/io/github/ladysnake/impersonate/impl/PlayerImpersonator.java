package io.github.ladysnake.impersonate.impl;

import com.mojang.authlib.GameProfile;
import dev.onyxstudios.cca.api.v3.component.CopyableComponent;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import io.github.ladysnake.impersonate.Impersonate;
import io.github.ladysnake.impersonate.Impersonator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerImpersonator implements Impersonator, AutoSyncedComponent, CopyableComponent<PlayerImpersonator> {
    @NotNull
    private final Player player;
    private final Map<Identifier, GameProfile> stackedImpersonations = new LinkedHashMap<>();
    @Nullable
    private GameProfile impersonatedProfile;
    @Nullable
    private GameProfile editedProfile;

    public PlayerImpersonator(@NotNull Player player) {
        this.player = Objects.requireNonNull(player, "player");
    }

    @Override
    public void impersonate(@NotNull Identifier key, @NotNull GameProfile profile) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(profile, "profile");
        this.stackedImpersonations.put(key, profile);
        this.setImpersonatedProfile(profile);
    }

    @Override
    public void stopImpersonations() {
        this.stackedImpersonations.clear();
        this.resetImpersonation();
    }

    @Nullable
    @Override
    public GameProfile stopImpersonation(@NotNull Identifier key) {
        if (this.isImpersonating()) {
            GameProfile ret = this.stackedImpersonations.remove(key);
            this.resetImpersonation();
            return ret;
        }
        return null;
    }

    private void resetImpersonation() {
        this.setImpersonatedProfile(this.getActiveImpersonation());
    }

    @Nullable
    private GameProfile getActiveImpersonation() {
        GameProfile active = null;
        for (GameProfile gameProfile : this.stackedImpersonations.values()) {
            active = gameProfile;
        }
        return active;
    }

    private void setImpersonatedProfile(@Nullable GameProfile profile) {
        if (!Objects.equals(this.impersonatedProfile, profile)) {
            this.impersonatedProfile = profile;
            GameProfile actual = this.getActualProfile();
            this.editedProfile = profile == null
                ? null
                : new GameProfile(actual.id(), profile.name(), profile.properties());

            if (this.player instanceof ServerPlayer serverPlayer) {
                serverPlayer.refreshDisplayName();
                serverPlayer.refreshTabListName();
                Impersonate.IMPERSONATION.sync(this.player);
            }
        }
    }

    @Override
    public boolean isImpersonating() {
        return this.impersonatedProfile != null;
    }

    @Nullable
    @Override
    public GameProfile getImpersonatedProfile() {
        return this.impersonatedProfile;
    }

    @Nullable
    @Override
    public GameProfile getImpersonatedProfile(@NotNull Identifier key) {
        return this.stackedImpersonations.get(key);
    }

    @NotNull
    @Override
    public GameProfile getActualProfile() {
        return this.player.getGameProfile();
    }

    @NotNull
    @Override
    public GameProfile getEditedProfile() {
        return this.editedProfile == null ? this.getActualProfile() : this.editedProfile;
    }

    @Override
    public void copyFrom(PlayerImpersonator other) {
        this.stopImpersonations();
        this.stackedImpersonations.putAll(other.stackedImpersonations);
        this.resetImpersonation();
    }

    @Override
    public boolean shouldSyncWith(ServerPlayer player) {
        return true;
    }

    @Override
    public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        GameProfile profile = this.getImpersonatedProfile();
        buf.writeBoolean(profile != null);
        if (profile != null) {
            ByteBufCodecs.GAME_PROFILE.encode(buf, profile);
        }
    }

    @Override
    public void applySyncPacket(RegistryFriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            GameProfile profile = ByteBufCodecs.GAME_PROFILE.decode(buf);
            this.impersonatedProfile = profile;
            GameProfile actual = this.getActualProfile();
            this.editedProfile = new GameProfile(actual.id(), profile.name(), profile.properties());
        } else {
            this.impersonatedProfile = null;
            this.editedProfile = null;
        }
        this.player.refreshDisplayName();
    }

    @Override
    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        this.readFromNbt(tag);
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        this.stackedImpersonations.clear();
        ListTag list = tag.getListOrEmpty("impersonations");
        for (Tag elem : list) {
            if (elem instanceof CompoundTag entry) {
                Identifier key = Identifier.tryParse(entry.getStringOr("impersonation_key", ""));
                if (key != null) {
                    entry.read(ExtraCodecs.STORED_GAME_PROFILE).ifPresent(profile ->
                        this.stackedImpersonations.put(key, profile)
                    );
                }
            }
        }
        this.resetImpersonation();
    }

    @Override
    public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        this.writeToNbt(tag);
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        if (this.stackedImpersonations.isEmpty()) {
            return;
        }
        ListTag list = new ListTag();
        for (Map.Entry<Identifier, GameProfile> entry : this.stackedImpersonations.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("impersonation_key", entry.getKey().toString());
            entryTag.store(ExtraCodecs.STORED_GAME_PROFILE, entry.getValue());
            list.add(entryTag);
        }
        tag.put("impersonations", list);
    }
}

package io.github.ladysnake.impersonate;

import com.mojang.authlib.GameProfile;
import dev.onyxstudios.cca.api.v3.component.Component;
import javax.annotation.Nullable;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public interface Impersonator extends Component {
    static Impersonator get(@NotNull Player player) {
        return Impersonate.IMPERSONATION.get(player);
    }

    void impersonate(@NotNull Identifier key, @NotNull GameProfile profile);

    void stopImpersonations();

    @Nullable
    GameProfile stopImpersonation(@NotNull Identifier key);

    boolean isImpersonating();

    @Nullable
    GameProfile getImpersonatedProfile();

    @Nullable
    GameProfile getImpersonatedProfile(@NotNull Identifier key);

    @NotNull
    GameProfile getActualProfile();

    @NotNull
    GameProfile getEditedProfile();
}

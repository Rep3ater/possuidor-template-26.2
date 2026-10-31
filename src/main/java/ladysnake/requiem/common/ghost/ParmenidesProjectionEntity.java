package ladysnake.requiem.common.ghost;

import ladysnake.requiem.common.entity.PlayerShellEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

/** Player-rendered, non-possessable visual body used in a remote dimension. */
public final class ParmenidesProjectionEntity extends PlayerShellEntity {
    public ParmenidesProjectionEntity(EntityType<? extends Player> type, ServerLevel level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        setInvulnerable(false);
        getAbilities().invulnerable = false;
    }
}

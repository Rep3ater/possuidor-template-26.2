package ladysnake.requiem.common.entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
/** Independent registered body for the Suffering-transmuted immortal homunculus. */
public class ImmortalHomunculusEntity extends PathfinderMob {
    public ImmortalHomunculusEntity(EntityType<? extends ImmortalHomunculusEntity> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,100.0).add(Attributes.MOVEMENT_SPEED,0.20)
            .add(Attributes.ATTACK_DAMAGE,20.0).add(Attributes.FOLLOW_RANGE,32.0);
    }
    @Override protected void registerGoals() {
        // MarbasAbilitySystem owns the immortal entity's aggressive behavior.
    }
}
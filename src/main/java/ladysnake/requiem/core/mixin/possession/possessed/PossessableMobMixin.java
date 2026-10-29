package ladysnake.requiem.core.mixin.possession.possessed;

import ladysnake.requiem.api.v1.possession.Possessable;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.client.ClientPossessionHelper;
import ladysnake.requiem.core.RequiemCore;
import ladysnake.requiem.core.entity.EntityAiToggle;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class PossessableMobMixin implements Possessable {
    @Unique
    @Nullable
    private Player requiem$possessor;

    @Override
    @Nullable
    public Player getPossessor() {
        return this.requiem$possessor;
    }

    @Override
    public boolean isBeingPossessed() {
        return this.requiem$possessor != null;
    }

    @Override
    public boolean canBePossessedBy(Player player) {
        return this.requiem$possessor == null && ((Mob) (Object) this).isAlive();
    }

    @Override
    public void setPossessor(@Nullable Player possessor) {
        this.requiem$possessor = possessor;
        Mob self = (Mob) (Object) this;
        if (!self.level().isClientSide()) {
            EntityAiToggle.get(self).toggleAi(RequiemCore.POSSESSION_MECHANISM_ID, possessor != null, false);
            if (possessor != null) {
                self.getNavigation().stop();
                self.setTarget(null);
            }
        }
        this.onPossessorSet(possessor);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void requiem$onTick(CallbackInfo ci) {
        if (this.requiem$possessor != null) {
            if (this.requiem$possessor.isRemoved()) {
                PossessionComponent.get(this.requiem$possessor).stopPossessing();
                this.requiem$possessor = null;
                return;
            }
            Mob self = (Mob) (Object) this;
            if (self.isDeadOrDying() || !self.isAlive()) {
                Player possessor = this.requiem$possessor;
                this.requiem$possessor = null;
                if (possessor instanceof net.minecraft.server.level.ServerPlayer sp) {
                    ladysnake.requiem.api.v1.event.requiem.PossessionEvents.HOST_DEATH.invoker().onHostDeath(
                        sp,
                        self,
                        self.getLastDamageSource() != null ? self.getLastDamageSource() : self.damageSources().generic()
                    );
                }
                PossessionComponent.get(possessor).stopPossessing();
                possessor.setHealth(possessor.getMaxHealth());
                ladysnake.requiem.api.v1.remnant.RemnantComponent.get(possessor).setVagrant(true);
                return;
            }
            self.setYRot(this.requiem$possessor.getYRot());
            self.setXRot(this.requiem$possessor.getXRot());
            self.setYHeadRot(this.requiem$possessor.getYHeadRot());
            self.yHeadRotO = this.requiem$possessor.yHeadRotO;
            self.yBodyRot = this.requiem$possessor.yBodyRot;
            self.yBodyRotO = this.requiem$possessor.yBodyRotO;
            self.setSwimming(this.requiem$possessor.isSwimming());
            self.fallDistance = 0;
            self.setDeltaMovement(this.requiem$possessor.getDeltaMovement());
            self.setPos(this.requiem$possessor.getX(), this.requiem$possessor.getY(), this.requiem$possessor.getZ());
            self.xo = this.requiem$possessor.xo;
            self.yo = this.requiem$possessor.yo;
            self.zo = this.requiem$possessor.zo;
            self.xOld = this.requiem$possessor.xOld;
            self.yOld = this.requiem$possessor.yOld;
            self.zOld = this.requiem$possessor.zOld;
            self.yRotO = this.requiem$possessor.yRotO;
            self.xRotO = this.requiem$possessor.xRotO;
            self.walkAnimation.setSpeed(this.requiem$possessor.walkAnimation.speed());
            self.setOnGround(this.requiem$possessor.onGround());
            self.setShiftKeyDown(this.requiem$possessor.isShiftKeyDown());
            self.setAggressive(this.requiem$possessor.isUsingItem());
            if (this.requiem$possessor.hurtTime > 0) {
                self.hurtTime = this.requiem$possessor.hurtTime;
                self.hurtDuration = this.requiem$possessor.hurtDuration;
            } else if (self.hurtTime > 0) {
                this.requiem$possessor.hurtTime = self.hurtTime;
                this.requiem$possessor.hurtDuration = self.hurtDuration;
            }

            if (!self.level().isClientSide()) {
                this.requiem$possessor.setAbsorptionAmount(self.getAbsorptionAmount());
                if (self.getRemainingFireTicks() > 0) {
                    this.requiem$possessor.setRemainingFireTicks(Math.max(this.requiem$possessor.getRemainingFireTicks(), self.getRemainingFireTicks()));
                } else if (this.requiem$possessor.getRemainingFireTicks() > 0) {
                    self.setRemainingFireTicks(this.requiem$possessor.getRemainingFireTicks());
                }
                this.requiem$possessor.setAirSupply(self.getAirSupply());
                if (Math.abs(this.requiem$possessor.getHealth() - self.getHealth()) > 0.05F) {
                    float newHealth = Math.min(self.getHealth(), this.requiem$possessor.getMaxHealth());
                    if (newHealth > 0.0F) {
                        this.requiem$possessor.setHealth(newHealth);
                    }
                }
            } else {
                self.setItemSlot(EquipmentSlot.MAINHAND, this.requiem$possessor.getMainHandItem());
                self.setItemSlot(EquipmentSlot.OFFHAND, this.requiem$possessor.getOffhandItem());
                if (this.requiem$possessor.swinging) {
                    self.swinging = true;
                    self.swingTime = this.requiem$possessor.swingTime;
                    self.swingingArm = this.requiem$possessor.swingingArm;
                }
            }
        }
    }

    @Inject(method = "isNoAi", at = @At("HEAD"), cancellable = true)
    private void requiem$disableAiWhenPossessed(CallbackInfoReturnable<Boolean> cir) {
        if (this.requiem$possessor != null) {
            cir.setReturnValue(true);
        }
    }
}

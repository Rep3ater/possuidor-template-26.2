package ladysnake.requiem.common.block.obelisk;

import ladysnake.requiem.Requiem;
import ladysnake.requiem.api.v1.block.ObeliskRune;
import ladysnake.requiem.api.v1.block.VagrantTargetableBlock;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.api.v1.remnant.RiftTracker;
import ladysnake.requiem.common.advancement.RequiemStats;
import ladysnake.requiem.common.entity.effect.AttritionStatusEffect;
import ladysnake.requiem.common.entity.effect.RequiemStatusEffects;
import ladysnake.requiem.common.screen.RiftScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class RiftRunestoneBlock extends InertRunestoneBlock implements ObeliskRune, VagrantTargetableBlock {
    public static final Identifier RIFT_ICON_ID = Requiem.id("textures/gui/rift_icon.png");

    public RiftRunestoneBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    public static Optional<Vec3> findRespawnPosition(EntityType<?> type, CollisionGetter world, BlockPos targetObelisk) {
        return RespawnAnchorBlock.findStandUpPosition(type, world, targetObelisk);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(ACTIVATED) || !RemnantComponent.get(player).getRemnantType().isDemon()) {
            return InteractionResult.PASS;
        }
        if (!(ObeliskMatcher.findObeliskOrigin(world, pos).map(world::getBlockEntity).orElse(null) instanceof RunestoneBlockEntity controller)) return InteractionResult.PASS;

        controller.getDescriptorRecord().ifPresent(record -> RiftTracker.KEY.get(player).addRift(record));

        if (!RemnantComponent.isIncorporeal(player) || !this.canBeUsedByVagrant(player)) {
            return InteractionResult.PASS;
        } else {
            if (world.isClientSide()) {
                return InteractionResult.SUCCESS;
            } else {
                if (controller.isPowered()) {
                    player.openMenu(this.getMenuProvider(state, world, controller.getBlockPos()));
                    player.awardStat(RequiemStats.INTERACT_WITH_RIFT);
                }

                return InteractionResult.CONSUME;
            }
        }
    }

    @Override
    protected @Nullable MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof RunestoneBlockEntity controller) {
            return new RiftScreenHandlerFactory(controller.getDescriptor().orElseThrow(), controller::canBeUsedBy);
        }
        return null;
    }

    @Override
    protected boolean shouldHaveBlockEntity(BlockState state) {
        return true;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public void applyEffect(ServerPlayer target, int runeLevel, int obeliskWidth) {
    }

    @Override
    public Identifier getTargetedIcon() {
        return RIFT_ICON_ID;
    }

    @Override
    public boolean canBeUsedByVagrant(Player player) {
        MobEffectInstance statusEffect = player.getEffect(RequiemStatusEffects.ATTRITION);
        return statusEffect == null || statusEffect.getAmplifier() < AttritionStatusEffect.MAX_LEVEL;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (itemStack.has(DataComponents.CUSTOM_NAME)) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof RunestoneBlockEntity runestone) {
                runestone.setCustomName(itemStack.getHoverName());
            }
        }
    }
}

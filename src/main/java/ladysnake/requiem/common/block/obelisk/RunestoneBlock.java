package ladysnake.requiem.common.block.obelisk;

import ladysnake.requiem.api.v1.block.ObeliskEffectRune;
import ladysnake.requiem.common.entity.SoulEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;
import java.util.function.Supplier;

public class RunestoneBlock extends InertRunestoneBlock implements ObeliskEffectRune {
    public static Optional<Block> getByEffect(Holder<MobEffect> effect) {
        Identifier id = effect.unwrapKey().map(ResourceKey::identifier).orElse(null);
        return Optional.ofNullable(id).flatMap(i ->
            BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath(i.getNamespace(), "tachylite/runic/" + i.getPath())));
    }

    private final Supplier<Holder<MobEffect>> effect;
    private final int maxLevel;

    public RunestoneBlock(BlockBehaviour.Properties settings, Supplier<Holder<MobEffect>> effect, int maxLevel) {
        super(settings);
        this.effect = effect;
        this.maxLevel = maxLevel;
    }

    @Override
    public Holder<MobEffect> getEffect() {
        return effect.get();
    }

    @Override
    public int getMaxLevel() {
        return this.maxLevel;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext ctx && ctx.getEntity() instanceof SoulEntity) {
            return Shapes.empty();
        }
        return super.getCollisionShape(state, world, pos, context);
    }
}

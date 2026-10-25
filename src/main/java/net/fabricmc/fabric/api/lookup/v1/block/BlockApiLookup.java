package net.fabricmc.fabric.api.lookup.v1.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface BlockApiLookup<A, C> {
    static <A, C> BlockApiLookup<A, C> get(Identifier id, Class<A> apiClass, Class<C> contextClass) {
        return BlockApiLookupRegistry.get(id);
    }

    @Nullable
    A find(Level level, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity blockEntity, C context);

    default @Nullable A find(Level level, BlockPos pos, C context) {
        return this.find(level, pos, level.getBlockState(pos), level.getBlockEntity(pos), context);
    }

    void registerForBlocks(BlockApiProvider<A, C> provider, Block... blocks);

    void registerSelf(Block... blocks);

    @FunctionalInterface
    interface BlockApiProvider<A, C> {
        @Nullable
        A find(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, C context);
    }
}

package net.fabricmc.fabric.api.lookup.v1.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockApiLookupRegistry {
    private static final Map<ResourceLocation, BlockApiLookup<?, ?>> LOOKUPS = new ConcurrentHashMap<>();

    private BlockApiLookupRegistry() {
    }

    @SuppressWarnings("unchecked")
    public static <A, C> BlockApiLookup<A, C> get(ResourceLocation id) {
        return (BlockApiLookup<A, C>) LOOKUPS.computeIfAbsent(id, DefaultBlockApiLookup::new);
    }

    private static final class DefaultBlockApiLookup<A, C> implements BlockApiLookup<A, C> {
        private final ResourceLocation id;
        private final Map<Block, BlockApiProvider<A, C>> providers = new ConcurrentHashMap<>();

        DefaultBlockApiLookup(ResourceLocation id) {
            this.id = id;
        }

        @Override
        public @Nullable A find(Level level, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity blockEntity, C context) {
            BlockState actualState = state != null ? state : level.getBlockState(pos);
            Block block = actualState.getBlock();
            BlockApiProvider<A, C> provider = this.providers.get(block);
            if (provider != null) {
                A result = provider.find(level, pos, actualState, blockEntity, context);
                if (result != null) {
                    return result;
                }
            }
            if (blockEntity != null) {
                try {
                    return (A) blockEntity;
                } catch (ClassCastException ignored) {
                }
            }
            try {
                return (A) block;
            } catch (ClassCastException ignored) {
            }
            return null;
        }

        @Override
        public void registerForBlocks(BlockApiProvider<A, C> provider, Block... blocks) {
            for (Block block : blocks) {
                this.providers.put(block, provider);
            }
        }

        @Override
        public void registerSelf(Block... blocks) {
            this.registerForBlocks((level, pos, state, blockEntity, context) -> {
                if (blockEntity != null) {
                    try {
                        return (A) blockEntity;
                    } catch (ClassCastException ignored) {
                    }
                }
                try {
                    return (A) state.getBlock();
                } catch (ClassCastException ignored) {
                }
                return null;
            }, blocks);
        }
    }
}

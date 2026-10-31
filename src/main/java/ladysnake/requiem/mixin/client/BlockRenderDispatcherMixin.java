package ladysnake.requiem.mixin.client;

import ladysnake.requiem.client.ParmenidesClientState;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Recolors common Overworld terrain with Nether terrain models while the Parmenides preview is held. */
@Mixin(BlockRenderDispatcher.class)
public abstract class BlockRenderDispatcherMixin {
    @Inject(method = "getBlockModel", at = @At("HEAD"), cancellable = true)
    private void requiem$previewNetherTerrain(BlockState state, CallbackInfoReturnable<BakedModel> cir) {
        if (!ParmenidesClientState.shouldPreviewNether() || !requiem$isTerrain(state)) return;
        cir.setReturnValue(((BlockRenderDispatcher) (Object) this).getBlockModel(Blocks.NETHERRACK.defaultBlockState()));
    }

    private static boolean requiem$isTerrain(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.COBBLESTONE)
            || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)
            || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)
            || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL)
            || state.is(Blocks.CLAY) || state.is(Blocks.TUFF) || state.is(Blocks.CALCITE);
    }
}

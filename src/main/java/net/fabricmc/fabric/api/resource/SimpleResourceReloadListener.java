package net.fabricmc.fabric.api.resource;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public interface SimpleResourceReloadListener<T> extends PreparableReloadListener {
    ResourceLocation getFabricId();

    CompletableFuture<T> load(ResourceManager resourceManager, Executor backgroundExecutor);

    CompletableFuture<Void> apply(T data, ResourceManager resourceManager, Executor gameExecutor);

    @Override
    default CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier preparationBarrier,
                                           ResourceManager resourceManager,
                                           net.minecraft.util.profiling.ProfilerFiller preparationsProfiler,
                                           net.minecraft.util.profiling.ProfilerFiller reloadProfiler,
                                           Executor backgroundExecutor,
                                           Executor gameExecutor) {
        return this.load(resourceManager, backgroundExecutor)
            .thenCompose(preparationBarrier::wait)
            .thenCompose(data -> this.apply(data, resourceManager, gameExecutor));
    }
}

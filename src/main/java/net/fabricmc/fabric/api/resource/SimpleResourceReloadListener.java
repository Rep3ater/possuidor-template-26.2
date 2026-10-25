package net.fabricmc.fabric.api.resource;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public interface SimpleResourceReloadListener<T> extends PreparableReloadListener {
    Identifier getFabricId();

    CompletableFuture<T> load(ResourceManager resourceManager, Executor backgroundExecutor);

    CompletableFuture<Void> apply(T data, ResourceManager resourceManager, Executor gameExecutor);

    @Override
    default CompletableFuture<Void> reload(PreparableReloadListener.SharedState currentReload, Executor taskExecutor, PreparableReloadListener.PreparationBarrier preparationBarrier, Executor reloadExecutor) {
        ResourceManager resourceManager = currentReload.resourceManager();
        return this.load(resourceManager, taskExecutor)
            .thenCompose(preparationBarrier::wait)
            .thenCompose(data -> this.apply(data, resourceManager, reloadExecutor));
    }
}

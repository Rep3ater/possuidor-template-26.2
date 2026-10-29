package dev.onyxstudios.cca.api.v3.component;

import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public interface ComponentRegistryV3 {
    ComponentRegistryV3 INSTANCE = new ComponentRegistryV3() {
        @Override
        public <C extends Component> ComponentKey<C> getOrCreate(ResourceLocation componentId, Class<C> componentClass) {
            return ComponentRegistry.getOrCreate(componentId, componentClass);
        }

        @Nullable
        @Override
        public ComponentKey<?> get(ResourceLocation componentId) {
            return ComponentRegistry.get(componentId);
        }

        @Override
        public Stream<ComponentKey<?>> stream() {
            return ComponentRegistry.stream();
        }
    };

    <C extends Component> ComponentKey<C> getOrCreate(ResourceLocation componentId, Class<C> componentClass);

    @Nullable
    ComponentKey<?> get(ResourceLocation componentId);

    Stream<ComponentKey<?>> stream();
}

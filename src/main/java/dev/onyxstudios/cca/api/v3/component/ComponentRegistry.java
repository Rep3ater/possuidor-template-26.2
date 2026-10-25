package dev.onyxstudios.cca.api.v3.component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.Identifier;

public final class ComponentRegistry {
    private static final Map<Identifier, ComponentKey<?>> KEYS = new ConcurrentHashMap<>();

    private ComponentRegistry() {
    }

    @SuppressWarnings("unchecked")
    public static <C extends Component> ComponentKey<C> getOrCreate(Identifier componentId, Class<C> componentClass) {
        Objects.requireNonNull(componentId, "componentId");
        Objects.requireNonNull(componentClass, "componentClass");
        ComponentKey<?> existing = KEYS.computeIfAbsent(componentId, id -> new ComponentKey<>(id, componentClass));
        if (existing.getComponentClass() != componentClass) {
            throw new IllegalStateException("Registered component key " + componentId + " twice with 2 different classes: "
                + existing.getComponentClass().getName() + ", " + componentClass.getName());
        }
        return (ComponentKey<C>) existing;
    }

    @Nullable
    public static ComponentKey<?> get(Identifier componentId) {
        return KEYS.get(componentId);
    }

    public static Stream<ComponentKey<?>> stream() {
        return KEYS.values().stream();
    }
}

package net.fabricmc.fabric.api.event;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

public final class EventFactory {
    private EventFactory() {
    }

    public static <T> Event<T> createArrayBacked(Class<? super T> type, Function<T[], T> invokerFactory) {
        return new ArrayBackedEvent<>(type, invokerFactory);
    }

    public static <T> Event<T> createArrayBacked(Class<T> type, T emptyInvoker, Function<T[], T> invokerFactory) {
        return createArrayBacked(type, listeners -> {
            if (listeners.length == 0) {
                return emptyInvoker;
            }
            return invokerFactory.apply(listeners);
        });
    }

    public static <T> Event<T> createWithPhases(Class<? super T> type, Function<T[], T> invokerFactory, ResourceLocation... defaultPhases) {
        ArrayBackedEvent<T> event = new ArrayBackedEvent<>(type, invokerFactory);
        for (int i = 0; i < defaultPhases.length; i++) {
            event.ensurePhase(defaultPhases[i]);
            if (i > 0) {
                event.addPhaseOrdering(defaultPhases[i - 1], defaultPhases[i]);
            }
        }
        return event;
    }

    public static String getHandlerName(Object handler) {
        return handler.getClass().getName();
    }

    public static boolean isProfilingEnabled() {
        return false;
    }

    private static final class ArrayBackedEvent<T> extends Event<T> {
        private final Class<? super T> type;
        private final Function<T[], T> invokerFactory;
        private final Object lock = new Object();
        private final Map<ResourceLocation, List<T>> phaseListeners = new LinkedHashMap<>();
        private final Map<ResourceLocation, Set<ResourceLocation>> phaseAfter = new LinkedHashMap<>();
        private List<ResourceLocation> sortedPhases = List.of(DEFAULT_PHASE);

        @SuppressWarnings("unchecked")
        ArrayBackedEvent(Class<? super T> type, Function<T[], T> invokerFactory) {
            this.type = Objects.requireNonNull(type, "type");
            this.invokerFactory = Objects.requireNonNull(invokerFactory, "invokerFactory");
            this.phaseListeners.put(DEFAULT_PHASE, new ArrayList<>());
            this.phaseAfter.put(DEFAULT_PHASE, new LinkedHashSet<>());
            T[] empty = (T[]) Array.newInstance(type, 0);
            this.invoker = invokerFactory.apply(empty);
        }

        void ensurePhase(ResourceLocation phase) {
            Objects.requireNonNull(phase, "phase");
            synchronized (this.lock) {
                if (!this.phaseListeners.containsKey(phase)) {
                    this.phaseListeners.put(phase, new ArrayList<>());
                    this.phaseAfter.put(phase, new LinkedHashSet<>());
                    this.rebuildPhaseOrderLocked();
                }
            }
        }

        @Override
        public void register(T listener) {
            this.register(DEFAULT_PHASE, listener);
        }

        @Override
        public void register(ResourceLocation phase, T listener) {
            Objects.requireNonNull(phase, "phase");
            Objects.requireNonNull(listener, "listener");
            synchronized (this.lock) {
                this.ensurePhase(phase);
                this.phaseListeners.get(phase).add(listener);
                this.rebuildInvokerLocked();
            }
        }

        @Override
        public void addPhaseOrdering(ResourceLocation firstPhase, ResourceLocation secondPhase) {
            Objects.requireNonNull(firstPhase, "firstPhase");
            Objects.requireNonNull(secondPhase, "secondPhase");
            if (firstPhase.equals(secondPhase)) {
                return;
            }
            synchronized (this.lock) {
                this.ensurePhase(firstPhase);
                this.ensurePhase(secondPhase);
                if (this.phaseAfter.get(secondPhase).add(firstPhase)) {
                    this.rebuildPhaseOrderLocked();
                    this.rebuildInvokerLocked();
                }
            }
        }

        private void rebuildPhaseOrderLocked() {
            List<ResourceLocation> result = new ArrayList<>(this.phaseListeners.size());
            Set<ResourceLocation> visited = new LinkedHashSet<>();
            Set<ResourceLocation> visiting = new LinkedHashSet<>();
            for (ResourceLocation phase : this.phaseListeners.keySet()) {
                this.visitPhase(phase, visited, visiting, result);
            }
            this.sortedPhases = result;
        }

        private void visitPhase(ResourceLocation phase, Set<ResourceLocation> visited, Set<ResourceLocation> visiting, List<ResourceLocation> result) {
            if (visited.contains(phase)) {
                return;
            }
            if (!visiting.add(phase)) {
                return;
            }
            Set<ResourceLocation> deps = this.phaseAfter.get(phase);
            if (deps != null) {
                for (ResourceLocation dep : deps) {
                    this.visitPhase(dep, visited, visiting, result);
                }
            }
            visiting.remove(phase);
            visited.add(phase);
            result.add(phase);
        }

        @SuppressWarnings("unchecked")
        private void rebuildInvokerLocked() {
            int total = 0;
            for (ResourceLocation phase : this.sortedPhases) {
                total += this.phaseListeners.get(phase).size();
            }
            T[] array = (T[]) Array.newInstance(this.type, total);
            int idx = 0;
            for (ResourceLocation phase : this.sortedPhases) {
                for (T listener : this.phaseListeners.get(phase)) {
                    array[idx++] = listener;
                }
            }
            this.invoker = this.invokerFactory.apply(array);
        }
    }
}

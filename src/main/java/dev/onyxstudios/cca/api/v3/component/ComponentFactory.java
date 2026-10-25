package dev.onyxstudios.cca.api.v3.component;

@FunctionalInterface
public interface ComponentFactory<T, C extends Component> {
    C createComponent(T provider);
}

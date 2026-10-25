package dev.onyxstudios.cca.api.v3.entity;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentFactory;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public interface EntityComponentFactoryRegistry {
    <E extends Entity, C extends Component> void registerFor(Class<E> target, ComponentKey<C> key, ComponentFactory<? super E, ? extends C> factory);

    default <C extends Component> void registerForPlayers(ComponentKey<C> key, ComponentFactory<? super Player, ? extends C> factory) {
        this.registerForPlayers(key, factory, RespawnCopyStrategy.LOSSLESS_ONLY);
    }

    <C extends Component> void registerForPlayers(ComponentKey<C> key, ComponentFactory<? super Player, ? extends C> factory, RespawnCopyStrategy<? super C> respawnStrategy);

    <E extends Entity, C extends Component> Registration<E, C> beginRegistration(Class<E> target, ComponentKey<C> key);

    interface Registration<E extends Entity, C extends Component> {
        Registration<E, C> filter(Predicate<Class<? extends E>> test);

        Registration<E, C> respawnStrategy(RespawnCopyStrategy<? super C> strategy);

        Registration<E, C> after(ComponentKey<?>... keys);

        <I extends C> Registration<E, I> impl(Class<I> impl);

        void end(ComponentFactory<? super E, ? extends C> factory);
    }
}

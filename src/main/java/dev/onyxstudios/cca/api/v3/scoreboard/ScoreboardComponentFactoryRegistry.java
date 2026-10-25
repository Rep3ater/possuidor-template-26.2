package dev.onyxstudios.cca.api.v3.scoreboard;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;

public interface ScoreboardComponentFactoryRegistry {
    <C extends Component> void registerScoreboardComponent(ComponentKey<C> key, ScoreboardComponentFactory<? extends C> factory);

    default <C extends Component, F extends C> void registerScoreboardComponent(ComponentKey<C> key, Class<F> impl, ScoreboardComponentFactory<? extends F> factory) {
        this.registerScoreboardComponent(key, factory);
    }
}

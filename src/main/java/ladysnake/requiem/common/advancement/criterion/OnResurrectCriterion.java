package ladysnake.requiem.common.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public class OnResurrectCriterion extends SimpleCriterionTrigger<OnResurrectCriterion.Conditions> {
    private final ResourceLocation id;

    public OnResurrectCriterion(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public void handle(ServerPlayer player, Entity body) {
        this.trigger(player, conditions -> conditions.test(player, body));
    }

    public record Conditions(Optional<ContextAwarePredicate> player, Optional<EntityPredicate> entity) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                EntityPredicate.CODEC.optionalFieldOf("body").forGetter(Conditions::entity)
            ).apply(i, Conditions::new)
        );

        public boolean test(ServerPlayer player, Entity body) {
            return !this.entity.isPresent() || this.entity.get().matches(player, body);
        }
    }
}

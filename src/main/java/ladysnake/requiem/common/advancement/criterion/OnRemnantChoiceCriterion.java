package ladysnake.requiem.common.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ladysnake.requiem.api.v1.remnant.RemnantType;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class OnRemnantChoiceCriterion extends SimpleCriterionTrigger<OnRemnantChoiceCriterion.Conditions> {
    private final ResourceLocation id;

    public OnRemnantChoiceCriterion(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public void handle(ServerPlayer player, RemnantType chosenType) {
        this.trigger(player, conditions -> conditions.test(chosenType));
    }

    public record Conditions(Optional<ContextAwarePredicate> player, RemnantTypePredicate predicate) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                RemnantTypePredicate.CODEC.optionalFieldOf("remnant_type", RemnantTypePredicate.ANY).forGetter(Conditions::predicate)
            ).apply(i, Conditions::new)
        );

        public boolean test(RemnantType type) {
            return this.predicate.matches(type);
        }
    }
}

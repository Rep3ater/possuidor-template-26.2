package ladysnake.requiem.common.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootContext;

import java.util.Optional;

public class OnPossessionCriterion extends SimpleCriterionTrigger<OnPossessionCriterion.Conditions> {
    private final Identifier id;

    public OnPossessionCriterion(Identifier id) {
        this.id = id;
    }

    public Identifier getId() {
        return id;
    }

    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public void handle(ServerPlayer possessor, LivingEntity possessed) {
        LootContext lootContext = EntityPredicate.createContext(possessor, possessed);
        this.trigger(possessor, conditions -> conditions.test(lootContext));
    }

    public record Conditions(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> entity) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(Conditions::entity)
            ).apply(i, Conditions::new)
        );

        public boolean test(LootContext ctx) {
            return !this.entity.isPresent() || this.entity.get().matches(ctx);
        }
    }
}

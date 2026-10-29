package ladysnake.requiem.common.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootContext;

import java.util.Optional;

public class TransformedPossessedCriterion extends SimpleCriterionTrigger<TransformedPossessedCriterion.Conditions> {
    private final ResourceLocation id;

    public TransformedPossessedCriterion(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public void handle(ServerPlayer player, LivingEntity before, LivingEntity after, boolean cure) {
        this.trigger(player, conditions -> conditions.test(player, before, after, cure));
    }

    public record Conditions(
        Optional<ContextAwarePredicate> player,
        Optional<ContextAwarePredicate> before,
        Optional<ContextAwarePredicate> after,
        Optional<Boolean> cure
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("before").forGetter(Conditions::before),
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("after").forGetter(Conditions::after),
                Codec.BOOL.optionalFieldOf("cure").forGetter(Conditions::cure)
            ).apply(i, Conditions::new)
        );

        public boolean test(ServerPlayer player, LivingEntity before, LivingEntity after, boolean cure) {
            LootContext beforeCtx = EntityPredicate.createContext(player, before);
            LootContext afterCtx = EntityPredicate.createContext(player, after);
            return (!this.before.isPresent() || this.before.get().matches(beforeCtx))
                && (!this.after.isPresent() || this.after.get().matches(afterCtx))
                && (this.cure.isEmpty() || this.cure.get() == cure);
        }
    }
}

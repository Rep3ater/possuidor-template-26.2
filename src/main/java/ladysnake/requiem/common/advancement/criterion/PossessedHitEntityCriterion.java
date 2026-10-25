package ladysnake.requiem.common.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.DamagePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

import java.util.Optional;

public class PossessedHitEntityCriterion extends SimpleCriterionTrigger<PossessedHitEntityCriterion.Conditions> {
    private final Identifier id;

    public PossessedHitEntityCriterion(Identifier id) {
        this.id = id;
    }

    public Identifier getId() {
        return this.id;
    }

    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public void handle(ServerPlayer player, Entity possessed, Entity entity, DamageSource source, float dealt, float taken, boolean blocked) {
        this.trigger(player, conditions -> conditions.test(player, source, dealt, taken, blocked, possessed, entity));
    }

    public record Conditions(
        Optional<ContextAwarePredicate> player,
        Optional<DamagePredicate> damage,
        Optional<ContextAwarePredicate> possessed,
        Optional<ContextAwarePredicate> entity
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                DamagePredicate.CODEC.optionalFieldOf("damage").forGetter(Conditions::damage),
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("possessed").forGetter(Conditions::possessed),
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(Conditions::entity)
            ).apply(i, Conditions::new)
        );

        public boolean test(ServerPlayer player, DamageSource source, float dealt, float taken, boolean blocked, Entity possessed, Entity target) {
            if (this.damage.isPresent() && !this.damage.get().matches(player, source, dealt, taken, blocked)) {
                return false;
            }
            LootContext possessedCtx = EntityPredicate.createContext(player, possessed);
            LootContext targetCtx = EntityPredicate.createContext(player, target);
            return (!this.possessed.isPresent() || this.possessed.get().matches(possessedCtx))
                && (!this.entity.isPresent() || this.entity.get().matches(targetCtx));
        }
    }
}

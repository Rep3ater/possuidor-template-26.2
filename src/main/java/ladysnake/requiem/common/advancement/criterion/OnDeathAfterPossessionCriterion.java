package ladysnake.requiem.common.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.DamageSourcePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

import java.util.Optional;

public class OnDeathAfterPossessionCriterion extends SimpleCriterionTrigger<OnDeathAfterPossessionCriterion.Conditions> {
    private final ResourceLocation id;

    public OnDeathAfterPossessionCriterion(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public void handle(ServerPlayer player, Entity entity, DamageSource deathCause) {
        this.trigger(player, conditions -> conditions.test(player, entity, deathCause));
    }

    public record Conditions(
        Optional<ContextAwarePredicate> player,
        Optional<ContextAwarePredicate> entity,
        Optional<DamageSourcePredicate> killingBlow,
        Optional<Boolean> seppukku
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(Conditions::entity),
                DamageSourcePredicate.CODEC.optionalFieldOf("killing_blow").forGetter(Conditions::killingBlow),
                Codec.BOOL.optionalFieldOf("seppukku").forGetter(Conditions::seppukku)
            ).apply(i, Conditions::new)
        );

        public boolean test(ServerPlayer player, Entity entity, DamageSource deathCause) {
            LootContext lootContext = EntityPredicate.createContext(player, entity);
            if (this.killingBlow.isPresent() && !this.killingBlow.get().matches(player, deathCause)) {
                return false;
            }
            if (this.entity.isPresent() && !this.entity.get().matches(lootContext)) {
                return false;
            }
            return this.seppukku.isEmpty() || this.seppukku.get() == (deathCause.getEntity() == entity);
        }
    }
}

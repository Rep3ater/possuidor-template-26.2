package ladysnake.requiem.common.advancement.criterion;

import ladysnake.requiem.Requiem;
import net.minecraft.advancements.CriterionTrigger;
import net.neoforged.neoforge.registries.RegisterEvent;

public class RequiemCriteria {
    public static final OnResurrectCriterion PLAYER_RESURRECTED_AS_ENTITY = new OnResurrectCriterion(Requiem.id("player_resurrected_as_entity"));
    public static final OnRemnantChoiceCriterion MADE_REMNANT_CHOICE = new OnRemnantChoiceCriterion(Requiem.id("made_remnant_choice"));
    public static final OnPossessionCriterion PLAYER_POSSESSED_ENTITY = new OnPossessionCriterion(Requiem.id("player_possessed_entity"));
    public static final OnDeathAfterPossessionCriterion DEATH_AFTER_POSSESSION = new OnDeathAfterPossessionCriterion(Requiem.id("entity_died_after_possession"));
    public static final PossessedHitEntityCriterion POSSESSED_HIT_ENTITY = new PossessedHitEntityCriterion(Requiem.id("possessed_hit_entity"));
    public static final TransformedPossessedCriterion TRANSFORMED_POSSESSED_ENTITY = new TransformedPossessedCriterion(Requiem.id("transformed_possessed_entity"));
    public static final UsedRequiemTotemCriterion USED_TOTEM = new UsedRequiemTotemCriterion(Requiem.id("used_totem"));

    public static void register(RegisterEvent.RegisterHelper<CriterionTrigger<?>> helper) {
        helper.register(Requiem.id("player_resurrected_as_entity"), PLAYER_RESURRECTED_AS_ENTITY);
        helper.register(Requiem.id("made_remnant_choice"), MADE_REMNANT_CHOICE);
        helper.register(Requiem.id("player_possessed_entity"), PLAYER_POSSESSED_ENTITY);
        helper.register(Requiem.id("entity_died_after_possession"), DEATH_AFTER_POSSESSION);
        helper.register(Requiem.id("possessed_hit_entity"), POSSESSED_HIT_ENTITY);
        helper.register(Requiem.id("transformed_possessed_entity"), TRANSFORMED_POSSESSED_ENTITY);
        helper.register(Requiem.id("used_totem"), USED_TOTEM);
    }

    public static void init() {
        // Registered via RegisterEvent
    }
}

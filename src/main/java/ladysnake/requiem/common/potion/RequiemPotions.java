package ladysnake.requiem.common.potion;

import ladysnake.requiem.Requiem;
import ladysnake.requiem.common.entity.effect.RequiemStatusEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class RequiemPotions {
    public static final int SHARED_BODY_DURATION = 20 * 60 * 3;

    public static final DeferredHolder<Potion, Potion> SHARED_BODY = DeferredHolder.create(Registries.POTION, Requiem.id("shared_body"));

    private RequiemPotions() {}

    public static void register(RegisterEvent.RegisterHelper<Potion> helper) {
        helper.register(Requiem.id("shared_body"), new Potion("shared_body", new MobEffectInstance(RequiemStatusEffects.SHARED_BODY, SHARED_BODY_DURATION)));
    }

    /**
     * Brewed from an awkward potion and an echo shard. Splash and lingering variants follow from the usual
     * gunpowder / dragon's breath recipes.
     */
    public static void onRegisterBrewingRecipes(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addMix(Potions.AWKWARD, Items.ECHO_SHARD, SHARED_BODY);
    }
}

/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses>.
 *
 * Linking this mod statically or dynamically with other
 * modules is making a combined work based on this mod.
 * Thus, the terms and conditions of the GNU General Public License cover the whole combination.
 *
 * In addition, as a special exception, the copyright holders of
 * this mod give you permission to combine this mod
 * with free software programs or libraries that are released under the GNU LGPL
 * and with code included in the standard release of Minecraft under All Rights Reserved (or
 * modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the GNU GPL for this mod
 * and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of this mod are not obligated to grant
 * this special exception for their modified versions; it is their choice whether to do so.
 * The GNU General Public License gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version which carries forward this exception.
 */
package ladysnake.requiem.common.entity.effect;

import ladysnake.requiem.Requiem;
import ladysnake.requiem.common.entity.RequiemEntityAttributes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class RequiemStatusEffects {
    public static final DeferredHolder<MobEffect, MobEffect> ATTRITION = DeferredHolder.create(Registries.MOB_EFFECT, Requiem.id("attrition"));
    public static final DeferredHolder<MobEffect, MobEffect> EMANCIPATION = DeferredHolder.create(Registries.MOB_EFFECT, Requiem.id("emancipation"));
    public static final DeferredHolder<MobEffect, MobEffect> RECLAMATION = DeferredHolder.create(Registries.MOB_EFFECT, Requiem.id("reclamation"));
    public static final DeferredHolder<MobEffect, MobEffect> PENANCE = DeferredHolder.create(Registries.MOB_EFFECT, Requiem.id("penance"));
    public static final DeferredHolder<MobEffect, MobEffect> SHARED_BODY = DeferredHolder.create(Registries.MOB_EFFECT, Requiem.id("shared_body"));
    public static final DeferredHolder<MobEffect, MobEffect> WHITE_VISION = DeferredHolder.create(Registries.MOB_EFFECT, Requiem.id("white_vision"));
    public static final DeferredHolder<MobEffect, MobEffect> NUCLEUS_TRANSITION = DeferredHolder.create(Registries.MOB_EFFECT, Requiem.id("nucleus_transition"));
    public static final DeferredHolder<MobEffect, MobEffect> ASH_LUNG = DeferredHolder.create(Registries.MOB_EFFECT, Requiem.id("ash_lung"));
    public static final DeferredHolder<MobEffect, MobEffect> BRAIN_DAMAGE = DeferredHolder.create(Registries.MOB_EFFECT, Requiem.id("brain_damage"));
    public static final DeferredHolder<MobEffect, MobEffect> HOMUNCULUS_MENTAL_HEALTH = DeferredHolder.create(Registries.MOB_EFFECT, Requiem.id("homunculus_mental_health"));

    public static void register(RegisterEvent.RegisterHelper<MobEffect> helper) {
        helper.register(Requiem.id("attrition"), new AttritionStatusEffect(MobEffectCategory.HARMFUL, 0xAA3322)
            .addAttributeModifier(Attributes.MAX_HEALTH, Requiem.id("attrition.max_health"), -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(RequiemEntityAttributes.SOUL_OFFENSE, Requiem.id("attrition.soul_offense"), -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        helper.register(Requiem.id("emancipation"), new EmancipationStatusEffect(MobEffectCategory.BENEFICIAL, 0x7799FF));
        helper.register(Requiem.id("reclamation"), new ReclamationStatusEffect(MobEffectCategory.BENEFICIAL, 0xFFDF00));
        helper.register(Requiem.id("penance"), new PenanceStatusEffect(MobEffectCategory.HARMFUL, 0xB6FF00));
        helper.register(Requiem.id("shared_body"), new SharedBodyStatusEffect(MobEffectCategory.NEUTRAL, 0x8E7CFF));
        helper.register(Requiem.id("white_vision"), new WhiteVisionStatusEffect());
        helper.register(Requiem.id("nucleus_transition"), new NucleusTransitionStatusEffect());
        helper.register(Requiem.id("ash_lung"), new CorruptionMarkerStatusEffect(0xB5A58A));
        helper.register(Requiem.id("brain_damage"), new CorruptionMarkerStatusEffect(0xB47AC7));
        helper.register(Requiem.id("homunculus_mental_health"), new CorruptionMarkerStatusEffect(0xE6D75C));
    }

    public static void postRegister() {
        PenanceStatusEffect.registerCallbacks();
        ReclamationStatusEffect.registerEventHandlers();
    }

    public static void init() {
        // Registered via RegisterEvent
    }
}

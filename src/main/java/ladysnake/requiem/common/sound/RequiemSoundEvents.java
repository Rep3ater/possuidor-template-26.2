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
package ladysnake.requiem.common.sound;

import ladysnake.requiem.Requiem;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class RequiemSoundEvents {
    public static final SoundEvent BLOCK_OBELISK_ACTIVATE = SoundEvent.createVariableRangeEvent(Requiem.id("block.obelisk.activate"));
    public static final SoundEvent BLOCK_OBELISK_AMBIENT = SoundEvent.createVariableRangeEvent(Requiem.id("block.obelisk.ambient"));
    public static final SoundEvent BLOCK_OBELISK_CHARGE = SoundEvent.createVariableRangeEvent(Requiem.id("block.obelisk.charge"));
    public static final SoundEvent BLOCK_OBELISK_DEACTIVATE = SoundEvent.createVariableRangeEvent(Requiem.id("block.obelisk.deactivate"));
    public static final SoundEvent BLOCK_RUNESTONE_CARVE = SoundEvent.createVariableRangeEvent(Requiem.id("block.runestone.carve"));
    public static final SoundEvent BLOCK_RUNESTONE_CLEAR = SoundEvent.createVariableRangeEvent(Requiem.id("block.runestone.clear"));
    public static final SoundEvent EFFECT_BECOME_MORTAL = SoundEvent.createVariableRangeEvent(Requiem.id("effect.become.mortal"));
    public static final SoundEvent EFFECT_BECOME_REMNANT = SoundEvent.createVariableRangeEvent(Requiem.id("effect.become.remnant"));
    public static final SoundEvent EFFECT_DISSOCIATE = SoundEvent.createVariableRangeEvent(Requiem.id("effect.dissociate"));
    public static final SoundEvent EFFECT_PHASE = SoundEvent.createVariableRangeEvent(Requiem.id("effect.phase"));
    public static final SoundEvent EFFECT_POSSESSION_ATTEMPT = SoundEvent.createVariableRangeEvent(Requiem.id("effect.possession.attempt"));
    public static final SoundEvent EFFECT_RECLAMATION_CLEAR = SoundEvent.createVariableRangeEvent(Requiem.id("effect.reclamation.clear"));
    public static final SoundEvent EFFECT_TIME_STOP = SoundEvent.createVariableRangeEvent(Requiem.id("effect.time.stop"));
    public static final SoundEvent ENTITY_MORTICIAN_AMBIENT = SoundEvent.createVariableRangeEvent(Requiem.id("entity.mortician.ambient"));
    public static final SoundEvent ENTITY_MORTICIAN_CAST_SPELL = SoundEvent.createVariableRangeEvent(Requiem.id("entity.mortician.cast_spell"));
    public static final SoundEvent ENTITY_MORTICIAN_PREPARE_ATTACK = SoundEvent.createVariableRangeEvent(Requiem.id("entity.mortician.prepare_attack"));
    public static final SoundEvent ENTITY_MORTICIAN_DEATH = SoundEvent.createVariableRangeEvent(Requiem.id("entity.mortician.death"));
    public static final SoundEvent ENTITY_MORTICIAN_NO = SoundEvent.createVariableRangeEvent(Requiem.id("entity.mortician.no"));
    public static final SoundEvent ENTITY_MORTICIAN_TRADE = SoundEvent.createVariableRangeEvent(Requiem.id("entity.mortician.trade"));
    public static final SoundEvent ENTITY_MORTICIAN_YES = SoundEvent.createVariableRangeEvent(Requiem.id("entity.mortician.yes"));
    public static final SoundEvent ENTITY_MORTICIAN_HURT = SoundEvent.createVariableRangeEvent(Requiem.id("entity.mortician.hurt"));
    public static final SoundEvent ENTITY_SOUL_TELEPORT = SoundEvent.createVariableRangeEvent(Requiem.id("entity.soul.teleport"));
    public static final SoundEvent ENTITY_SOUL_DISINTEGRATES = SoundEvent.createVariableRangeEvent(Requiem.id("entity.soul.disintegrate"));
    public static final SoundEvent ENTITY_OBELISK_SOUL_DISINTEGRATES = SoundEvent.createVariableRangeEvent(Requiem.id("entity.obelisk_soul.disintegrate"));
    public static final SoundEvent ITEM_OPUS_USE = SoundEvent.createVariableRangeEvent(Requiem.id("item.opus.use"));
    public static final SoundEvent ITEM_EMPTY_VESSEL_USE = SoundEvent.createVariableRangeEvent(Requiem.id("item.empty_vessel.use"));
    public static final SoundEvent ITEM_FILLED_VESSEL_USE = SoundEvent.createVariableRangeEvent(Requiem.id("item.filled_vessel.use"));
    public static final SoundEvent[] ANIMALESE_LETTERS = new SoundEvent[26];
    public static final SoundEvent ANIMALESE_GENERAL = SoundEvent.createVariableRangeEvent(Requiem.id("speech.animalese.general"));

    static {
        for (int i = 0; i < 26; i++) {
            char c = (char) ('a' + i);
            ANIMALESE_LETTERS[i] = SoundEvent.createVariableRangeEvent(Requiem.id("speech.animalese." + c));
        }
    }

    public static void register(RegisterEvent.RegisterHelper<SoundEvent> helper) {
        helper.register(Requiem.id("block.obelisk.activate"), BLOCK_OBELISK_ACTIVATE);
        helper.register(Requiem.id("block.obelisk.ambient"), BLOCK_OBELISK_AMBIENT);
        helper.register(Requiem.id("block.obelisk.charge"), BLOCK_OBELISK_CHARGE);
        helper.register(Requiem.id("block.obelisk.deactivate"), BLOCK_OBELISK_DEACTIVATE);
        helper.register(Requiem.id("block.runestone.carve"), BLOCK_RUNESTONE_CARVE);
        helper.register(Requiem.id("block.runestone.clear"), BLOCK_RUNESTONE_CLEAR);
        helper.register(Requiem.id("effect.become.mortal"), EFFECT_BECOME_MORTAL);
        helper.register(Requiem.id("effect.become.remnant"), EFFECT_BECOME_REMNANT);
        helper.register(Requiem.id("effect.dissociate"), EFFECT_DISSOCIATE);
        helper.register(Requiem.id("effect.phase"), EFFECT_PHASE);
        helper.register(Requiem.id("effect.possession.attempt"), EFFECT_POSSESSION_ATTEMPT);
        helper.register(Requiem.id("effect.reclamation.clear"), EFFECT_RECLAMATION_CLEAR);
        helper.register(Requiem.id("effect.time.stop"), EFFECT_TIME_STOP);
        helper.register(Requiem.id("entity.mortician.ambient"), ENTITY_MORTICIAN_AMBIENT);
        helper.register(Requiem.id("entity.mortician.cast_spell"), ENTITY_MORTICIAN_CAST_SPELL);
        helper.register(Requiem.id("entity.mortician.prepare_attack"), ENTITY_MORTICIAN_PREPARE_ATTACK);
        helper.register(Requiem.id("entity.mortician.death"), ENTITY_MORTICIAN_DEATH);
        helper.register(Requiem.id("entity.mortician.no"), ENTITY_MORTICIAN_NO);
        helper.register(Requiem.id("entity.mortician.trade"), ENTITY_MORTICIAN_TRADE);
        helper.register(Requiem.id("entity.mortician.yes"), ENTITY_MORTICIAN_YES);
        helper.register(Requiem.id("entity.mortician.hurt"), ENTITY_MORTICIAN_HURT);
        helper.register(Requiem.id("entity.soul.teleport"), ENTITY_SOUL_TELEPORT);
        helper.register(Requiem.id("entity.soul.disintegrate"), ENTITY_SOUL_DISINTEGRATES);
        helper.register(Requiem.id("entity.obelisk_soul.disintegrate"), ENTITY_OBELISK_SOUL_DISINTEGRATES);
        helper.register(Requiem.id("item.opus.use"), ITEM_OPUS_USE);
        helper.register(Requiem.id("item.empty_vessel.use"), ITEM_EMPTY_VESSEL_USE);
        helper.register(Requiem.id("item.filled_vessel.use"), ITEM_FILLED_VESSEL_USE);
        for (int i = 0; i < 26; i++) {
            char c = (char) ('a' + i);
            helper.register(Requiem.id("speech.animalese." + c), ANIMALESE_LETTERS[i]);
        }
        helper.register(Requiem.id("speech.animalese.general"), ANIMALESE_GENERAL);
    }

    public static void init() {
        // NO-OP
    }
}

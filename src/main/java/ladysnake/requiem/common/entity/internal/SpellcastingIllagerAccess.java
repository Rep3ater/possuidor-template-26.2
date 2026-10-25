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
 */
package ladysnake.requiem.common.entity.internal;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;

public abstract class SpellcastingIllagerAccess extends SpellcasterIllager {
    public static final SpellcasterIllager.IllagerSpell SPELL_NONE = IllagerSpell.NONE;
    public static final SpellcasterIllager.IllagerSpell SPELL_FANGS = IllagerSpell.FANGS;
    public static final Method CAST_SPELL_GOAL$CAST_SPELL;

    static {
        try {
            Method m = SpellcasterUseSpellGoal.class.getDeclaredMethod("performSpellCasting");
            m.setAccessible(true);
            CAST_SPELL_GOAL$CAST_SPELL = m;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not find performSpellCasting method", e);
        }
    }

    private SpellcastingIllagerAccess(EntityType<? extends SpellcasterIllager> entityType, Level world) {
        super(entityType, world);
    }
}

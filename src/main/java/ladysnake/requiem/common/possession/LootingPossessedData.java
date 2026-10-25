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
package ladysnake.requiem.common.possession;

import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.common.loot.RequiemLootTables;
import ladysnake.requiem.common.particle.RequiemParticleTypes;
import ladysnake.requiem.core.possession.PossessedDataBase;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class LootingPossessedData extends PossessedDataBase {
    private boolean previouslyPossessed;

    public LootingPossessedData(Entity holder) {
        super(holder);
    }

    @Override
    public void giftFirstPossessionLoot(Player player) {
        if (!this.previouslyPossessed) {
            this.dropLoot(player);
            this.previouslyPossessed = true;
        }
    }

    @Override
    public void clientTick() {
        if (this.wasConvertedUnderPossession()) {
            Entity camera = Minecraft.getInstance().getCameraEntity();
            Level world = holder.level();
            if (camera != null && RemnantComponent.isIncorporeal(camera)) {
                if (world.getRandom().nextFloat() > 0.9f) {
                    for (int i = 0; i < world.getRandom().nextInt(3); i++) {
                        double vx = world.getRandom().nextGaussian() * 0.03D;
                        double vy = world.getRandom().nextGaussian() * 0.03D;
                        double vz = world.getRandom().nextGaussian() * 0.03D;
                        world.addParticle(RequiemParticleTypes.ATTUNED, holder.getX(0.5D), holder.getY(0.5D), holder.getZ(0.5D), vx, vy, vz);
                    }
                }
            }
        }
    }

    protected void dropLoot(Player player) {
        Identifier identifier = this.getLootTable();
        ServerLevel world = (ServerLevel) this.holder.level();
        LootTable lootTable = world.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, identifier));
        LootParams params = new LootParams.Builder(world)
            .withParameter(LootContextParams.THIS_ENTITY, this.holder)
            .withParameter(LootContextParams.ORIGIN, this.holder.position())
            .withLuck(player.getLuck())
            .create(RequiemLootTables.POSSESSION);
        lootTable.getRandomItems(params, player.getInventory()::placeItemBackInInventory);
    }

    private Identifier getLootTable() {
        Identifier identifier = BuiltInRegistries.ENTITY_TYPE.getKey(this.holder.getType());
        return Identifier.fromNamespaceAndPath(identifier.getNamespace(), "requiem/possession/" + identifier.getPath());
    }

    @Override
    protected void onPossessed() {
        this.previouslyPossessed = true;
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        super.readFromNbt(tag);

        if (tag.contains("previously_possessed")) {
            this.previouslyPossessed = tag.getBooleanOr("previously_possessed", false);
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        super.writeToNbt(tag);

        if (this.previouslyPossessed) {
            tag.putBoolean("previously_possessed", true);
        }
    }
}

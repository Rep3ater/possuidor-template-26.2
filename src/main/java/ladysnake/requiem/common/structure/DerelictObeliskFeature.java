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
package ladysnake.requiem.common.structure;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class DerelictObeliskFeature extends Structure {
    public static final MapCodec<DerelictObeliskFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        settingsCodec(instance),
        StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool)
    ).apply(instance, DerelictObeliskFeature::new));

    private final Holder<StructureTemplatePool> startPool;

    public DerelictObeliskFeature(StructureSettings settings, Holder<StructureTemplatePool> startPool) {
        super(settings);
        this.startPool = startPool;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.x * 16;
        int z = chunkPos.z * 16;

        BlockPos centerPos = new BlockPos(x, 0, z);

        return Optional.of(new GenerationStub(centerPos, structurePieces -> {
            WorldgenRandom chunkRandom = context.random();
            StructurePoolElement spawnedStructure = this.startPool.value().getRandomTemplate(chunkRandom);

            if (spawnedStructure != EmptyPoolElement.INSTANCE) {
                Rotation rotation = Util.getRandom(Rotation.values(), chunkRandom);
                BlockPos startPos = chunkPos.getWorldPosition();
                StructureTemplateManager structureManager = context.structureTemplateManager();
                PoolElementStructurePiece piece = new PoolElementStructurePiece(
                    structureManager,
                    spawnedStructure,
                    startPos,
                    spawnedStructure.getGroundLevelDelta(),
                    rotation,
                    spawnedStructure.getBoundingBox(structureManager, startPos, rotation),
                    LiquidSettings.APPLY_WATERLOGGING
                );
                BoundingBox boundingBox = piece.getBoundingBox();
                OptionalInt floorY = getFloorHeight(context.random(), context.randomState(), context.chunkGenerator(), boundingBox, context.heightAccessor());

                if (floorY.isEmpty()) return;

                int lowering = boundingBox.minY() + piece.getGroundLevelDelta();
                piece.move(0, floorY.getAsInt() - lowering, 0);
                structurePieces.addPiece(piece);

                Vec3i structureCenter = structurePieces.build().pieces().get(0).getBoundingBox().getCenter();
                int xOffset = centerPos.getX() - structureCenter.getX();
                int zOffset = centerPos.getZ() - structureCenter.getZ();
                for (StructurePiece structurePiece : structurePieces.build().pieces()) {
                    structurePiece.move(xOffset, 0, zOffset);
                }
            }
        }));
    }

    @Override
    public StructureType<?> type() {
        return RequiemStructures.DERELICT_OBELISK;
    }

    static OptionalInt getFloorHeight(RandomSource random, RandomState randomState, ChunkGenerator chunkGenerator, BoundingBox box, LevelHeightAccessor world) {
        int maxY = Mth.nextInt(random, 60, 100);

        List<BlockPos> corners = ImmutableList.of(
            new BlockPos(box.minX(), 0, box.minZ()),
            new BlockPos(box.maxX(), 0, box.minZ()),
            new BlockPos(box.minX(), 0, box.maxZ()),
            new BlockPos(box.maxX(), 0, box.maxZ())
        );
        List<NoiseColumn> cornerColumns = corners.stream().map(pos -> chunkGenerator.getBaseColumn(pos.getX(), pos.getZ(), world, randomState)).toList();
        Heightmap.Types heightmapType = Heightmap.Types.OCEAN_FLOOR_WG;

        for (int y = maxY; y > 15; --y) {
            int validCorners = 0;

            for (NoiseColumn cornerColumn : cornerColumns) {
                BlockState blockState = cornerColumn.getBlock(y);
                if (heightmapType.isOpaque().test(blockState)) {
                    ++validCorners;
                }
            }

            if (validCorners >= 3) {
                validCorners = 0;

                for (NoiseColumn cornerColumn : cornerColumns) {
                    BlockState blockState = cornerColumn.getBlock(y + box.getYSpan() - 1);
                    if (blockState.isAir()) {
                        ++validCorners;
                        if (validCorners == 2) {
                            return OptionalInt.of(y + 1);
                        }
                    }
                }
            }
        }

        return OptionalInt.empty();
    }
}

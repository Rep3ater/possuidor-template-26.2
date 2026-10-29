package ladysnake.requiem.common.construct;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class ConstructKinematicsAnalyzer {
    public static final int MAX_VOXELS = 512;
    public static final int MAX_RADIUS = 12;

    public static ConstructVoxelModel analyze(ServerLevel level, BlockPos heartPos, float scale) {
        ConstructVoxelModel model = new ConstructVoxelModel();
        model.setScale(scale);

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        visited.add(heartPos);
        queue.add(heartPos);

        while (!queue.isEmpty() && visited.size() <= MAX_VOXELS) {
            BlockPos current = queue.poll();
            BlockPos relPos = current.subtract(heartPos);

            BlockState state = level.getBlockState(current);
            if (!state.isAir()) {
                ConstructPart part = deducePart(relPos, state);
                ActuatorType actuator = deduceActuator(state, part);
                Direction facing = getFacing(state);

                float hardness = state.getDestroySpeed(level, current);
                float maxHp = calculateVoxelHealth(hardness, current.equals(heartPos));

                ConstructVoxel voxel = new ConstructVoxel(relPos, state, part, actuator, facing, maxHp);
                if (level.getBlockEntity(current) instanceof net.minecraft.world.level.block.entity.DispenserBlockEntity dispenser) {
                    List<net.minecraft.world.item.ItemStack> items = new java.util.ArrayList<>();
                    for (int slot = 0; slot < dispenser.getContainerSize(); slot++) {
                        net.minecraft.world.item.ItemStack stack = dispenser.getItem(slot);
                        if (!stack.isEmpty()) {
                            items.add(stack.copy());
                        }
                    }
                    voxel.setInventory(items);
                }
                model.addVoxel(voxel);
            }

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    if (Math.abs(neighbor.getX() - heartPos.getX()) <= MAX_RADIUS &&
                        Math.abs(neighbor.getY() - heartPos.getY()) <= MAX_RADIUS &&
                        Math.abs(neighbor.getZ() - heartPos.getZ()) <= MAX_RADIUS) {
                        
                        BlockState neighborState = level.getBlockState(neighbor);
                        if (!neighborState.isAir() && !neighborState.is(Blocks.BEDROCK)) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        return model;
    }

    private static ConstructPart deducePart(BlockPos rel, BlockState state) {
        Block block = state.getBlock();

        // 1. Skulls & Heads: ALWAYS Head!
        if (block instanceof AbstractSkullBlock || block instanceof SkullBlock || 
            block instanceof CarvedPumpkinBlock || block == Blocks.BEACON) {
            return ConstructPart.HEAD;
        }

        // 2. Weapon & Cannon components: Dispensers, Droppers, Lightning Rods, End Rods
        if (block instanceof DispenserBlock || block instanceof DropperBlock || 
            block instanceof LightningRodBlock || block == Blocks.END_ROD) {
            return rel.getX() < 0 ? ConstructPart.LEFT_ARM : ConstructPart.RIGHT_ARM;
        }

        // 3. Heavy Footwear/Legs bias: Hoppers, Anvils, Chains at bottom
        if (rel.getY() <= 0 && (block instanceof HopperBlock || block instanceof AnvilBlock || block instanceof ChainBlock)) {
            return rel.getX() < 0 ? ConstructPart.LEFT_LEG : ConstructPart.RIGHT_LEG;
        }

        int rx = rel.getX();
        int ry = rel.getY();
        int rz = rel.getZ();

        // Legs: below the core heart
        if (ry < 0) {
            if (rx < 0) {
                return ConstructPart.LEFT_LEG;
            } else if (rx > 0) {
                return ConstructPart.RIGHT_LEG;
            } else {
                return rz < 0 ? ConstructPart.LEFT_LEG : ConstructPart.RIGHT_LEG;
            }
        }

        // Arms: extended out horizontally
        if (Math.abs(rx) >= 2 || (ry >= 0 && Math.abs(rx) >= 1 && (block instanceof PistonBaseBlock))) {
            return rx < 0 ? ConstructPart.LEFT_ARM : ConstructPart.RIGHT_ARM;
        }

        // Head: elevated above torso in center column
        if (ry >= 2 && Math.abs(rx) <= 1 && Math.abs(rz) <= 1) {
            return ConstructPart.HEAD;
        }

        // Default: Torso / Core
        return ConstructPart.TORSO;
    }

    private static ActuatorType deduceActuator(BlockState state, ConstructPart part) {
        Block block = state.getBlock();

        if (state.is(ladysnake.requiem.common.block.RequiemBlocks.KEY_TRIGGER_R)) {
            return ActuatorType.KEY_R_TRIGGER;
        }
        if (state.is(ladysnake.requiem.common.block.RequiemBlocks.KEY_TRIGGER_V)) {
            return ActuatorType.KEY_V_TRIGGER;
        }
        if (block instanceof DispenserBlock || block instanceof DropperBlock) {
            return ActuatorType.DISPENSER_CANNON;
        }
        if (block == Blocks.REDSTONE_BLOCK || block == Blocks.OBSERVER || block == Blocks.TARGET) {
            return ActuatorType.REDSTONE_PULSE;
        }
        if (block instanceof PistonBaseBlock || state.is(Blocks.PISTON) || state.is(Blocks.STICKY_PISTON)) {
            if (part == ConstructPart.LEFT_ARM || part == ConstructPart.RIGHT_ARM) {
                return ActuatorType.PISTON_THRUST;
            } else {
                return ActuatorType.HYDRAULIC_JUMP;
            }
        }
        return ActuatorType.NONE;
    }

    private static Direction getFacing(BlockState state) {
        if (state.hasProperty(PistonBaseBlock.FACING)) {
            return state.getValue(PistonBaseBlock.FACING);
        }
        if (state.hasProperty(DirectionalBlock.FACING)) {
            return state.getValue(DirectionalBlock.FACING);
        }
        return Direction.NORTH;
    }

    private static float calculateVoxelHealth(float destroySpeed, boolean isHeart) {
        if (isHeart) {
            return 80.0f;
        }
        if (destroySpeed < 0) {
            return 100.0f;
        }
        return Math.max(10.0f, Math.min(100.0f, destroySpeed * 6.0f));
    }
}

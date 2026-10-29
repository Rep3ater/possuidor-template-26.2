package ladysnake.requiem.common.construct;

import ladysnake.requiem.Requiem;
import ladysnake.requiem.common.block.RequiemBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

public class ConstructWorkshopManager {
    public static final ResourceKey<Level> WORKSHOP_DIMENSION = ResourceKey.create(
        Registries.DIMENSION,
        Requiem.id("construct_workshop")
    );

    public static BlockPos getWorkshopCenter(ServerPlayer player) {
        int sectorIndex = Math.abs(player.getUUID().hashCode() % 1000);
        return new BlockPos(sectorIndex * 256, 64, 0);
    }

    public static void enterWorkshop(ServerPlayer player, BlockPos returnPos, ResourceKey<Level> returnDimension, boolean isMini) {
        if (!(player.level() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (server == null) return;
        ServerLevel workshopLevel = server.getLevel(WORKSHOP_DIMENSION);
        if (workshopLevel == null) {
            player.sendSystemMessage(Component.literal("Workshop dimension could not be loaded."));
            return;
        }

        BlockPos center = getWorkshopCenter(player);
        setupVoidHeart(workshopLevel, center, isMini);

        // Store return information on player's persistent data
        player.getPersistentData().putInt("requiem:construct_ret_x", returnPos.getX());
        player.getPersistentData().putInt("requiem:construct_ret_y", returnPos.getY());
        player.getPersistentData().putInt("requiem:construct_ret_z", returnPos.getZ());
        player.getPersistentData().putString("requiem:construct_ret_dim", returnDimension.location().toString());
        player.getPersistentData().putBoolean("requiem:construct_is_mini", isMini);

        // Enable flight so player can construct freely in 3D void
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();

        // Teleport player floating in front of the heart
        double spawnX = center.getX() + 0.5;
        double spawnY = center.getY() + 1.0;
        double spawnZ = center.getZ() - 3.0;
        player.teleportTo(workshopLevel, spawnX, spawnY, spawnZ, Set.of(), 0.0f, 0.0f);
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();

        workshopLevel.playSound(null, spawnX, spawnY, spawnZ, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.8f, 1.2f);
        player.sendSystemMessage(Component.literal("§6Construct Workshop: §fFlight enabled! Build around the floating Heart."));
        player.sendSystemMessage(Component.literal("§7Shift-Right-Click the Heart when ready to awaken your construct."));
    }

    public static void enterWorkshopForDisassembly(ServerPlayer player, BlockPos returnPos, ResourceKey<Level> returnDimension, ConstructVoxelModel model, boolean isMini) {
        if (!(player.level() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (server == null) return;
        ServerLevel workshopLevel = server.getLevel(WORKSHOP_DIMENSION);
        if (workshopLevel == null) return;

        BlockPos center = getWorkshopCenter(player);
        clearWorkshopArea(workshopLevel, center.above());

        // Place Heart
        BlockPos heartPos = center.above();
        workshopLevel.setBlock(heartPos, isMini ? RequiemBlocks.SMALL_CONSTRUCT_HEART.defaultBlockState() : RequiemBlocks.CONSTRUCT_HEART.defaultBlockState(), 3);

        // Reconstruct all blocks in the workshop around the heart
        for (ConstructVoxel voxel : model.getVoxels().values()) {
            if (!voxel.isDestroyed() && !voxel.getRelPos().equals(BlockPos.ZERO)) {
                BlockPos worldPos = heartPos.offset(voxel.getRelPos());
                workshopLevel.setBlock(worldPos, voxel.getState(), 3);
                if (workshopLevel.getBlockEntity(worldPos) instanceof net.minecraft.world.level.block.entity.DispenserBlockEntity dispenser) {
                    java.util.List<net.minecraft.world.item.ItemStack> items = voxel.getInventory();
                    for (int i = 0; i < items.size() && i < dispenser.getContainerSize(); i++) {
                        dispenser.setItem(i, items.get(i).copy());
                    }
                }
            }
        }

        player.getPersistentData().putInt("requiem:construct_ret_x", returnPos.getX());
        player.getPersistentData().putInt("requiem:construct_ret_y", returnPos.getY());
        player.getPersistentData().putInt("requiem:construct_ret_z", returnPos.getZ());
        player.getPersistentData().putString("requiem:construct_ret_dim", returnDimension.location().toString());
        player.getPersistentData().putBoolean("requiem:construct_is_mini", isMini);

        double spawnX = center.getX() + 0.5;
        double spawnY = center.getY() + 1.0;
        double spawnZ = center.getZ() - 3.0;
        player.teleportTo(workshopLevel, spawnX, spawnY, spawnZ, Set.of(), 0.0f, 0.0f, false);
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();

        workshopLevel.playSound(null, spawnX, spawnY, spawnZ, SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8f, 1.0f);
        player.sendSystemMessage(Component.literal("§aRobot disassembled into workshop! Modify or add blocks, then Shift-Right-Click the Heart to re-awaken."));
    }

    public static void exitWorkshop(ServerPlayer player, BlockPos heartPos, boolean awaken) {
        if (!(player.level() instanceof ServerLevel currentLevel)) return;
        MinecraftServer server = currentLevel.getServer();
        if (server == null) return;

        int retX = player.getPersistentData().getInt("requiem:construct_ret_x");
        int retY = (player.getPersistentData().contains("requiem:construct_ret_y", net.minecraft.nbt.Tag.TAG_INT) ? player.getPersistentData().getInt("requiem:construct_ret_y") : 64);
        int retZ = player.getPersistentData().getInt("requiem:construct_ret_z");
        String dimStr = (player.getPersistentData().contains("requiem:construct_ret_dim", net.minecraft.nbt.Tag.TAG_STRING) ? player.getPersistentData().getString("requiem:construct_ret_dim") : "minecraft:overworld");
        boolean isMini = player.getPersistentData().getBoolean("requiem:construct_is_mini");

        ResourceKey<Level> returnDim = ResourceKey.create(
            Registries.DIMENSION,
            net.minecraft.resources.ResourceLocation.parse(dimStr)
        );

        ServerLevel returnLevel = server.getLevel(returnDim);
        if (returnLevel == null) {
            returnLevel = server.overworld();
        }

        ConstructVoxelModel model = null;
        if (awaken) {
            float scale = isMini ? (1.0f / 16.0f) : 1.0f;
            model = ConstructKinematicsAnalyzer.analyze(currentLevel, heartPos, scale);
            clearWorkshopArea(currentLevel, heartPos);
        }

        // Restore normal flight if survival
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }

        BlockPos anchorPos = new BlockPos(retX, retY, retZ);
        double spawnX = retX + 0.5;
        double spawnY = retY;
        double spawnZ = retZ + 0.5;

        player.teleportTo(returnLevel, spawnX, spawnY + 0.5, spawnZ - 1.5, Set.of(), 0.0f, 0.0f, false);
        returnLevel.playSound(null, spawnX, spawnY, spawnZ, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);

        if (awaken && model != null && !model.isEmpty()) {
            ladysnake.requiem.common.construct.entity.RoboticConstructEntity construct = 
                new ladysnake.requiem.common.construct.entity.RoboticConstructEntity(
                    ladysnake.requiem.common.entity.RequiemEntities.ROBOTIC_CONSTRUCT,
                    returnLevel
                );
            construct.setPos(spawnX, spawnY, spawnZ);
            construct.setVoxelModel(model);
            returnLevel.addFreshEntity(construct);

            // Remove placed anchor block
            returnLevel.removeBlock(anchorPos, false);
            player.sendSystemMessage(Component.literal(isMini ? "§aThe Micro Robot awakens! You can now possess it." : "§aThe Robotic Mecha awakens! You can now possess it."));
        }
    }

    private static void setupVoidHeart(ServerLevel level, BlockPos center, boolean isMini) {
        clearWorkshopArea(level, center.above());
        // Place ONLY the floating Heart! No ground platform.
        level.setBlock(center.above(), isMini ? RequiemBlocks.SMALL_CONSTRUCT_HEART.defaultBlockState() : RequiemBlocks.CONSTRUCT_HEART.defaultBlockState(), 3);
    }

    private static void clearWorkshopArea(ServerLevel level, BlockPos heartPos) {
        int r = ConstructKinematicsAnalyzer.MAX_RADIUS;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos p = heartPos.offset(x, y, z);
                    if (!level.getBlockState(p).isAir() && !level.getBlockState(p).is(Blocks.BEDROCK)) {
                        level.removeBlock(p, false);
                    }
                }
            }
        }
    }
}

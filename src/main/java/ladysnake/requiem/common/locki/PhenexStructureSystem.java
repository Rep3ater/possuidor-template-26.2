package ladysnake.requiem.common.locki;

import com.mojang.authlib.GameProfile;
import ladysnake.requiem.common.block.RequiemBlocks;
import ladysnake.requiem.common.entity.PlayerShellEntity;
import ladysnake.requiem.common.entity.RequiemEntities;
import ladysnake.requiem.common.network.RequiemNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Tracks who built the 3x3 ritual and turns named dropped items into player shells. */
public final class PhenexStructureSystem {
    private static final Set<String> PHENEX_PLACED = new HashSet<>();
    private PhenexStructureSystem() {}

    private static String key(ServerLevel level, BlockPos pos) {
        return level.dimension().location() + "|" + pos.asLong();
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)
                || !PhenexAbilitySystem.isPhenex(player)) return;
        BlockPos placed = event.getPos().immutable();
        PHENEX_PLACED.add(key(level, placed));
        tryComplete(level, placed, player);
    }

    private static void tryComplete(ServerLevel level, BlockPos placed, ServerPlayer builder) {
        // Rows: iron-chain-iron / chain-redstone-chain / iron-chain-iron. This is a horizontal
        // 3x3 ritual square. Every part must have been placed by a tagged Phenex this session.
        for (int ox = -1; ox <= 0; ox++) for (int oz = -1; oz <= 0; oz++) {
            BlockPos origin = placed.offset(ox, 0, oz);
            BlockPos[] cells = new BlockPos[] {
                origin, origin.east(), origin.east(2),
                origin.south(), origin.south().east(), origin.south().east(2),
                origin.south(2), origin.south(2).east(), origin.south(2).east(2)
            };
            BlockState[] expected = new BlockState[] {
                Blocks.IRON_BLOCK.defaultBlockState(), Blocks.CHAIN.defaultBlockState(), Blocks.IRON_BLOCK.defaultBlockState(),
                Blocks.CHAIN.defaultBlockState(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Blocks.CHAIN.defaultBlockState(),
                Blocks.IRON_BLOCK.defaultBlockState(), Blocks.CHAIN.defaultBlockState(), Blocks.IRON_BLOCK.defaultBlockState()
            };
            boolean valid = true;
            for (int i = 0; i < cells.length; i++) {
                if (!PHENEX_PLACED.contains(key(level, cells[i])) || !level.getBlockState(cells[i]).is(expected[i].getBlock())) {
                    valid = false; break;
                }
            }
            if (!valid) continue;
            for (BlockPos cell : cells) {
                PHENEX_PLACED.remove(key(level, cell));
                if (!cell.equals(cells[4])) level.setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
            }
            level.setBlock(cells[4], RequiemBlocks.CLONING_MACHINE.defaultBlockState(), 3);
            builder.sendSystemMessage(net.minecraft.network.chat.Component.literal("§dThe ritual square transforms into a Cloning Machine. Drop a custom-named item on it to create that player's shell."));
            return;
        }
    }

    public static void onItemToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        ItemEntity dropped = event.getEntity();
        ItemStack stack = dropped.getItem();
        if (stack.isEmpty() || !stack.has(DataComponents.CUSTOM_NAME)) return;
        String requestedName = stack.getHoverName().getString().trim();
        if (requestedName.isBlank()) return;
        BlockPos machine = BlockPos.containing(dropped.position());
        BlockPos found = null;
        for (BlockPos pos : BlockPos.betweenClosed(machine.offset(-2, -2, -2), machine.offset(2, 2, 2))) {
            if (level.getBlockState(pos).is(RequiemBlocks.CLONING_MACHINE)) { found = pos.immutable(); break; }
        }
        if (found == null) return;

        ServerPlayer online = level.getServer().getPlayerList().getPlayerByName(requestedName);
        UUID uuid = online != null ? online.getUUID() : UUID.nameUUIDFromBytes(("OfflinePlayer:" + requestedName).getBytes(StandardCharsets.UTF_8));
        GameProfile profile = online != null ? online.getGameProfile() : new GameProfile(uuid, requestedName);
        PlayerShellEntity shell = RequiemEntities.PLAYER_SHELL.create(level);
        if (shell == null) return;
        shell.setDisplayProfile(profile);
        shell.setPos(found.getX() + 0.5, found.getY() + 1.0, found.getZ() + 0.5);
        shell.setYRot(player.getYRot());
        shell.setXRot(0.0F);
        if (level.addFreshEntity(shell)) {
            dropped.discard();
            for (ServerPlayer viewer : level.getServer().getPlayerList().getPlayers()) {
                if (viewer.level() == level) RequiemNetworking.sendShellProfileSyncMessage(viewer, shell.getId(), profile);
            }
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Cloning Machine created a shell for " + requestedName + "."));
        }
    }
}

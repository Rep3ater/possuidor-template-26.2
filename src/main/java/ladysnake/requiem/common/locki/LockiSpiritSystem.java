package ladysnake.requiem.common.locki;

import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.common.entity.effect.AttritionStatusEffect;
import ladysnake.requiem.common.entity.effect.RequiemStatusEffects;
import ladysnake.requiem.common.item.LockiAppleItem;
import ladysnake.requiem.common.item.RequiemItems;
import ladysnake.requiem.common.remnant.RemnantTypes;
import ladysnake.requiem.common.network.RequiemNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Server-authoritative gameplay for players tagged "locki". */
public final class LockiSpiritSystem {
    public static final String TAG = "locki";
    public static final String MODE_NONE = "none";
    public static final String MODE_SPIRIT = "spirit";
    public static final String MODE_ROOTS = "roots";
    public static final String MODE_CONVERSATION = "conversation";
    public static final String MODE_APPLE = "apple";

    private static final String DATA_PREFIX = "requiem:locki_";
    private static final String MODE = DATA_PREFIX + "mode";
    private static final String SAVED_FLYING_SPEED = DATA_PREFIX + "saved_flying_speed";
    private static final String HAS_SAVED_FLYING_SPEED = DATA_PREFIX + "has_saved_flying_speed";
    private static final ResourceLocation ASTRAL_LAND_SLOWDOWN = ResourceLocation.fromNamespaceAndPath("requiem", "locki_astral_land_slowdown");
    private static final ResourceLocation ASTRAL_ROOT_SPEED = ResourceLocation.fromNamespaceAndPath("requiem", "locki_astral_root_speed");
    private static final String TARGET_X = DATA_PREFIX + "target_x";
    private static final String TARGET_Y = DATA_PREFIX + "target_y";
    private static final String TARGET_Z = DATA_PREFIX + "target_z";
    private static final String TARGET_IS_TREE = DATA_PREFIX + "target_is_tree";
    private static final String TREE_X = DATA_PREFIX + "tree_x";
    private static final String TREE_Y = DATA_PREFIX + "tree_y";
    private static final String TREE_Z = DATA_PREFIX + "tree_z";
    private static final String TREE_DIM = DATA_PREFIX + "tree_dimension";
    private static final String TREE_SAPLING = DATA_PREFIX + "tree_sapling";
    private static final String APPLE_UUID = DATA_PREFIX + "apple_uuid";
    private static final String APPLE_AGE = DATA_PREFIX + "apple_age";
    private static final String APPLE_DROPPED_AT = DATA_PREFIX + "apple_dropped_at";
    private static final String APPLE_HAS_BEEN_CARRIED = DATA_PREFIX + "apple_has_been_carried";
    private static final String APPLE_X = DATA_PREFIX + "apple_x";
    private static final String APPLE_Y = DATA_PREFIX + "apple_y";
    private static final String APPLE_Z = DATA_PREFIX + "apple_z";
    private static final String APPLE_DIM = DATA_PREFIX + "apple_dimension";
    private static final String APPLE_DROP_X = DATA_PREFIX + "apple_drop_x";
    private static final String APPLE_DROP_Y = DATA_PREFIX + "apple_drop_y";
    private static final String APPLE_DROP_Z = DATA_PREFIX + "apple_drop_z";
    private static final String APPLE_DROP_DIM = DATA_PREFIX + "apple_drop_dimension";
    private static final String ROT_STAGE = DATA_PREFIX + "rot_stage";
    private static final String APPLIED_ATTRITION = DATA_PREFIX + "applied_attrition";
    private static final String SYNC_MODE = DATA_PREFIX + "sync_mode";
    private static final String SYNC_TARGET = DATA_PREFIX + "sync_target";
    private static final String SYNC_CAMERA = DATA_PREFIX + "sync_camera";
    private static final String SYNC_ROT = DATA_PREFIX + "sync_rot";

    public static final int TICKS_PER_DAY = 24_000;
    private static final long ROTTING_AWAY_AT = 14L * TICKS_PER_DAY;
    private static final long DROP_TIMEOUT = 30L * 20L;
    private static final Map<UUID, NetworkCache> NETWORKS = new HashMap<>();

    private LockiSpiritSystem() {}

    /** Requiem normally enables ghost flight; these Locki states intentionally suspend it. */
    public static boolean shouldSuppressFlight(Player player) {
        if (player == null || !player.entityTags().contains(TAG)) return false;
        String mode = player.getPersistentData().getString(MODE).orElse(MODE_NONE);
        return MODE_ROOTS.equals(mode) || MODE_CONVERSATION.equals(mode) || MODE_APPLE.equals(mode);
    }

    public static boolean isAstralVisionActive(Player player) {
        if (player == null || !player.entityTags().contains(TAG)) return false;
        String mode = player.getPersistentData().getString(MODE).orElse(MODE_NONE);
        return MODE_SPIRIT.equals(mode) || MODE_ROOTS.equals(mode);
    }

    private record NetworkCache(Level level, BlockPos center, long tick, List<LockiRootNetwork.PlantNode> plants,
                                List<LockiRootNetwork.Segment> segments) {}
    private record AppleTarget(ItemEntity itemEntity, ServerPlayer holder, boolean inMainHand, ItemStack stack) {}

    public static void tick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        CompoundTag data = serverPlayer.getPersistentData();
        // Force one initial state sync after joining/rejoining a world; the persistent data survives reconnects.
        if (serverPlayer.tickCount <= 2) data.remove(SYNC_MODE);
        String mode = data.getString(MODE).orElse(MODE_NONE);
        boolean tagged = player.entityTags().contains(TAG);

        if (!tagged) {
            applyMovementMode(serverPlayer, MODE_NONE);
            if (!MODE_NONE.equals(mode) && !mode.isBlank()) {
                if (MODE_APPLE.equals(mode)) releaseApple(serverPlayer, false, true);
                setMode(serverPlayer, MODE_NONE);
                resetPhysicalState(serverPlayer);
            }
            syncState(serverPlayer);
            return;
        }

        if (MODE_APPLE.equals(mode)) {
            applyMovementMode(serverPlayer, MODE_NONE);
            tickAppleBody(serverPlayer);
            syncState(serverPlayer);
            return;
        }

        if (!RemnantComponent.isIncorporeal(serverPlayer)) {
            applyMovementMode(serverPlayer, MODE_NONE);
            if (!MODE_NONE.equals(mode)) setMode(serverPlayer, MODE_NONE);
            resetPhysicalState(serverPlayer);
            syncState(serverPlayer);
            return;
        }

        if (MODE_NONE.equals(mode) || mode.isBlank()) {
            setMode(serverPlayer, MODE_SPIRIT);
            mode = MODE_SPIRIT;
        }

        applyMovementMode(serverPlayer, mode);
        if (MODE_ROOTS.equals(mode)) {
            tickRootDiving(serverPlayer);
        } else if (MODE_CONVERSATION.equals(mode)) {
            serverPlayer.setInvisible(true);
            serverPlayer.noPhysics = true;
            serverPlayer.getAbilities().flying = false;
            serverPlayer.getAbilities().mayfly = false;
            serverPlayer.setDeltaMovement(Vec3.ZERO);
        } else {
            // The Locki spirit is an invisible viewpoint, not a translucent player model.
            serverPlayer.setInvisible(MODE_SPIRIT.equals(mode));
            if (!serverPlayer.level().isClientSide() && serverPlayer.tickCount % 8 == 0 && serverPlayer.isShiftKeyDown()) {
                NetworkCache cache = getNetwork(serverPlayer);
                LockiRootNetwork.Segment nearest = LockiRootNetwork.nearestSegment(cache.segments(), serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ());
                if (nearest != null) {
                    Vec3 projected = nearest.project(serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ());
                    double dx = serverPlayer.getX() - projected.x;
                    double dy = serverPlayer.getY() - projected.y;
                    double dz = serverPlayer.getZ() - projected.z;
                    if (dx * dx + dy * dy + dz * dz <= 0.49) {
                        setMode(serverPlayer, MODE_ROOTS);
                        serverPlayer.setPos(projected.x, projected.y, projected.z);
                        serverPlayer.setInvisible(true);
                        serverPlayer.setPose(net.minecraft.world.entity.Pose.SWIMMING);
                        serverPlayer.getAbilities().flying = false;
                        serverPlayer.getAbilities().mayfly = false;
                        serverPlayer.onUpdateAbilities();
                    }
                }
            }
        }
        syncState(serverPlayer);
    }

    private static void applyMovementMode(ServerPlayer player, String mode) {
        CompoundTag data = player.getPersistentData();
        boolean needLandSlow = MODE_SPIRIT.equals(mode);
        boolean needRootBoost = MODE_ROOTS.equals(mode);

        // Incorporeal movement normally uses flight controls, so also scale the actual flying speed.
        // Save the player's pre-Locki value once and restore it when leaving the spirit state.
        if (needLandSlow) {
            if (!data.getBoolean(HAS_SAVED_FLYING_SPEED).orElse(false)) {
                data.putDouble(SAVED_FLYING_SPEED, player.getAbilities().getFlyingSpeed());
                data.putBoolean(HAS_SAVED_FLYING_SPEED, true);
            }
            if (Math.abs(player.getAbilities().getFlyingSpeed() - 0.0075F) > 0.0001F) {
                player.getAbilities().setFlyingSpeed(0.0075F);
                player.onUpdateAbilities();
            }
        } else if (data.getBoolean(HAS_SAVED_FLYING_SPEED).orElse(false)) {
            float original = data.getDouble(SAVED_FLYING_SPEED).orElse(0.05D).floatValue();
            player.getAbilities().setFlyingSpeed(original);
            player.onUpdateAbilities();
            data.remove(SAVED_FLYING_SPEED);
            data.remove(HAS_SAVED_FLYING_SPEED);
        }

        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        boolean hasLandSlow = speed.hasModifier(ASTRAL_LAND_SLOWDOWN);
        boolean hasRootBoost = speed.hasModifier(ASTRAL_ROOT_SPEED);
        if (hasLandSlow == needLandSlow && hasRootBoost == needRootBoost) return;
        speed.removeModifier(ASTRAL_LAND_SLOWDOWN);
        speed.removeModifier(ASTRAL_ROOT_SPEED);
        if (needLandSlow) {
            // Astral ground is deliberately lethargic: drifting rather than ordinary walking.
            speed.addTransientModifier(new AttributeModifier(ASTRAL_LAND_SLOWDOWN, -0.82,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        } else if (needRootBoost) {
            // Rootways are the fast-travel network through the realm.
            speed.addTransientModifier(new AttributeModifier(ASTRAL_ROOT_SPEED, 3.5,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    private static NetworkCache getNetwork(ServerPlayer player) {
        NetworkCache cache = NETWORKS.get(player.getUUID());
        BlockPos pos = player.blockPosition();
        if (cache == null || cache.level() != player.level() || player.tickCount - cache.tick() >= 20
                || cache.center().distSqr(pos) > 36.0) {
            List<LockiRootNetwork.PlantNode> plants = LockiRootNetwork.findPlants(player.level(), pos, LockiRootNetwork.DEFAULT_RADIUS);
            List<LockiRootNetwork.Segment> segments = LockiRootNetwork.build(player.level(), plants, LockiRootNetwork.DEFAULT_RADIUS);
            cache = new NetworkCache(player.level(), pos.immutable(), player.tickCount, plants, segments);
            NETWORKS.put(player.getUUID(), cache);
        }
        return cache;
    }

    private static void tickRootDiving(ServerPlayer player) {
        player.setInvisible(true);
        player.setPose(net.minecraft.world.entity.Pose.SWIMMING);
        player.noPhysics = true;
        if (player.getAbilities().flying || player.getAbilities().mayfly) {
            player.getAbilities().flying = false;
            player.getAbilities().mayfly = false;
            player.onUpdateAbilities();
        }
        NetworkCache cache = getNetwork(player);
        if (cache.segments().isEmpty()) {
            leaveRoots(player);
            return;
        }

        LockiRootNetwork.Segment nearest = LockiRootNetwork.nearestSegment(cache.segments(), player.getX(), player.getY(), player.getZ());
        if (nearest == null) {
            leaveRoots(player);
            return;
        }
        // Even if a sharp input or client prediction moves the avatar away from the root,
        // snap back to the nearest graph point instead of letting it escape into free movement.
        Vec3 point = nearest.project(player.getX(), player.getY(), player.getZ());
        // Hard 3D rail constraint: off-axis input is discarded and only the current root segment
        // can carry the player. Junctions are the only places where a branch can be taken.
        player.setPos(point.x, point.y, point.z);
        player.resetFallDistance();

        LockiRootNetwork.PlantNode plant = LockiRootNetwork.nearestPlant(cache.plants(), player.getX(), player.getY(), player.getZ());
        if (plant != null) {
            Vec3 p = plant.anchor();
            double dx = player.getX() - p.x;
            double dy = player.getY() - p.y;
            double dz = player.getZ() - p.z;
            if (dx * dx + dy * dy + dz * dz < 1.35 * 1.35) {
                startPlantConversation(player, plant);
            }
        }
    }

    private static void startPlantConversation(ServerPlayer player, LockiRootNetwork.PlantNode plant) {
        CompoundTag data = player.getPersistentData();
        data.putInt(TARGET_X, plant.pos().getX());
        data.putInt(TARGET_Y, plant.pos().getY());
        data.putInt(TARGET_Z, plant.pos().getZ());
        data.putBoolean(TARGET_IS_TREE, plant.tree());
        if (plant.tree()) {
            data.putInt(TREE_X, plant.pos().getX());
            data.putInt(TREE_Y, plant.pos().getY());
            data.putInt(TREE_Z, plant.pos().getZ());
            data.putString(TREE_DIM, player.level().dimension().identifier().toString());
            data.putString(TREE_SAPLING, BuiltInRegistries.BLOCK.getKey(LockiRootNetwork.getMatchingSapling(player.level().getBlockState(plant.pos()).getBlock())).toString());
        }
        setMode(player, MODE_CONVERSATION);
        player.setInvisible(true);
        player.setDeltaMovement(Vec3.ZERO);
        syncState(player);
    }

    public static void handlePlantChoice(ServerPlayer player, int choice) {
        if (!player.entityTags().contains(TAG) || !MODE_CONVERSATION.equals(player.getPersistentData().getString(MODE).orElse(MODE_NONE))) return;
        if (choice != 1) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("The plant closes its mind. You are expelled into the spirit realm."));
            setMode(player, MODE_SPIRIT);
            resetPhysicalState(player);
            syncState(player);
            return;
        }

        CompoundTag data = player.getPersistentData();
        BlockPos plantPos = new BlockPos(data.getInt(TARGET_X).orElse(0), data.getInt(TARGET_Y).orElse(64), data.getInt(TARGET_Z).orElse(0));
        boolean tree = data.getBoolean(TARGET_IS_TREE).orElse(false);
        if (!(player.level() instanceof ServerLevel level)
                || (tree && !level.getBlockState(plantPos).is(net.minecraft.tags.BlockTags.LOGS))
                || (!tree && !LockiRootNetwork.isPlant(level.getBlockState(plantPos)))) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("The plant's spirit has vanished. You are expelled into the spirit realm."));
            setMode(player, MODE_SPIRIT);
            resetPhysicalState(player);
            syncState(player);
            return;
        }

        // Convincing a plant is a meaningful attempt rather than a guaranteed button press.
        if (player.getRandom().nextFloat() >= 0.70F) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("The plant refuses to lend you one of its children. You are expelled into the spirit realm."));
            setMode(player, MODE_SPIRIT);
            resetPhysicalState(player);
            syncState(player);
            return;
        }
        if (!tree) {
            // Other bodies can have their own outcomes later; for now only trees can host Locki.
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("The plant accepts your plea, but this kind of body cannot hold you yet. The roots draw you back into the spirit realm."));
            setMode(player, MODE_SPIRIT);
            resetPhysicalState(player);
            syncState(player);
            return;
        }

        BlockPos treePos = plantPos;
        BlockPos fruitPos = findLowerLeaves(level, treePos);
        ItemStack apple = makeAppleStack(player.getUUID(), 0, data);
        ItemEntity appleEntity = new ItemEntity(level, fruitPos.getX() + 0.5, fruitPos.getY() + 0.35, fruitPos.getZ() + 0.5, apple);
        appleEntity.setDefaultPickUpDelay();
        // It visibly sprouts among the leaves and stays there until a player actually picks it up.
        appleEntity.setNoGravity(true);
        appleEntity.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(appleEntity);

        data.putString(APPLE_UUID, appleEntity.getUUID().toString());
        data.putLong(APPLE_AGE, 0L);
        data.putLong(APPLE_DROPPED_AT, -1L);
        data.putBoolean(APPLE_HAS_BEEN_CARRIED, false);
        data.putInt(ROT_STAGE, 0);
        data.putInt(APPLIED_ATTRITION, 0);
        data.putDouble(APPLE_X, appleEntity.getX());
        data.putDouble(APPLE_Y, appleEntity.getY());
        data.putDouble(APPLE_Z, appleEntity.getZ());
        data.putString(APPLE_DIM, level.dimension().identifier().toString());
        data.remove(APPLE_DROP_X);
        data.remove(APPLE_DROP_Y);
        data.remove(APPLE_DROP_Z);
        data.remove(APPLE_DROP_DIM);
        setMode(player, MODE_APPLE);
        player.setInvisible(true);
        player.noPhysics = false;
        player.setPose(net.minecraft.world.entity.Pose.STANDING);
        player.setPos(appleEntity.getX(), appleEntity.getY(), appleEntity.getZ());
        player.getAbilities().flying = false;
        player.getAbilities().mayfly = false;
        player.onUpdateAbilities();
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("The tree accepts you. You awaken as a small green apple among its lower leaves."));
        syncState(player);
    }

    private static BlockPos findLowerLeaves(ServerLevel level, BlockPos trunkBase) {
        BlockPos best = trunkBase.above(3);
        double bestScore = Double.MAX_VALUE;
        for (int x = trunkBase.getX() - 4; x <= trunkBase.getX() + 4; x++) {
            for (int z = trunkBase.getZ() - 4; z <= trunkBase.getZ() + 4; z++) {
                for (int y = trunkBase.getY() + 2; y <= trunkBase.getY() + 8; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).is(net.minecraft.tags.BlockTags.LEAVES)) continue;
                    double dx = x - trunkBase.getX();
                    double dz = z - trunkBase.getZ();
                    double dy = y - trunkBase.getY();
                    double score = dx * dx + dz * dz + dy * 0.18;
                    if (score < bestScore) { bestScore = score; best = pos; }
                }
            }
        }
        return best;
    }

    private static ItemStack makeAppleStack(UUID owner, int stage, CompoundTag playerData) {
        ItemStack stack = new ItemStack(RequiemItems.getLockiApple(stage));
        CompoundTag itemData = new CompoundTag();
        itemData.putString("locki:owner", owner.toString());
        itemData.putInt("locki:stage", stage);
        itemData.putInt("locki:tree_x", playerData.getInt(TREE_X).orElse(0));
        itemData.putInt("locki:tree_y", playerData.getInt(TREE_Y).orElse(64));
        itemData.putInt("locki:tree_z", playerData.getInt(TREE_Z).orElse(0));
        itemData.putString("locki:tree_dimension", playerData.getString(TREE_DIM).orElse("minecraft:overworld"));
        itemData.putString("locki:tree_sapling", playerData.getString(TREE_SAPLING).orElse("minecraft:oak_sapling"));
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.of(itemData));
        // Deliberately omit a custom name: this must look and read as an ordinary apple.
        return stack;
    }

    private static void tickAppleBody(ServerPlayer player) {
        player.setInvisible(true);
        if (player.getAbilities().flying || player.getAbilities().mayfly) {
            player.getAbilities().flying = false;
            player.getAbilities().mayfly = false;
            player.onUpdateAbilities();
        }
        CompoundTag data = player.getPersistentData();
        long age = data.getLong(APPLE_AGE).orElse(0L) + 1L;
        data.putLong(APPLE_AGE, age);

        AppleTarget target = findAppleTarget(player);
        if (target != null) {
            if (target.itemEntity() != null) {
                data.putDouble(APPLE_X, target.itemEntity().getX());
                data.putDouble(APPLE_Y, target.itemEntity().getY());
                data.putDouble(APPLE_Z, target.itemEntity().getZ());
                data.putString(APPLE_DIM, target.itemEntity().level().dimension().identifier().toString());
            } else if (target.holder() != null) {
                data.putDouble(APPLE_X, target.holder().getX());
                data.putDouble(APPLE_Y, target.holder().getY());
                data.putDouble(APPLE_Z, target.holder().getZ());
                data.putString(APPLE_DIM, target.holder().level().dimension().identifier().toString());
            }
            if (target.holder() != null) {
                data.putBoolean(APPLE_HAS_BEEN_CARRIED, true);
                if (target.holder().level() instanceof ServerLevel holderLevel) {
                    if (player.level() != holderLevel) {
                        player.teleportTo(holderLevel, target.holder().getX(), target.holder().getY(), target.holder().getZ(), Set.of(), target.holder().getYRot(), target.holder().getXRot(), false);
                    } else {
                        player.setPos(target.holder().getX(), target.holder().getY(), target.holder().getZ());
                    }
                }
            } else if (target.itemEntity() != null) {
                // A fruit that has already been picked up begins to fade if it is abandoned on the ground.
                if (data.getBoolean(APPLE_HAS_BEEN_CARRIED).orElse(false)) {
                    long droppedAt = data.getLong(APPLE_DROPPED_AT).orElse(-1L);
                    if (droppedAt < 0L) {
                        droppedAt = player.level().getGameTime();
                        data.putLong(APPLE_DROPPED_AT, droppedAt);
                    }
                    // Preserve the last physical drop site separately from the moving carrier
                    // position. Eating after picking the apple back up still births Locki at the
                    // spot where the fruit was dropped, not inside the eater's current body.
                    data.putDouble(APPLE_DROP_X, target.itemEntity().getX());
                    data.putDouble(APPLE_DROP_Y, target.itemEntity().getY());
                    data.putDouble(APPLE_DROP_Z, target.itemEntity().getZ());
                    data.putString(APPLE_DROP_DIM, target.itemEntity().level().dimension().identifier().toString());
                    if (player.level().getGameTime() - droppedAt >= DROP_TIMEOUT) {
                        releaseApple(player, false, false);
                        syncState(player);
                        return;
                    }
                } else {
                    // The fruit initially hangs in the tree indefinitely, waiting for the first carrier.
                    data.putLong(APPLE_DROPPED_AT, -1L);
                }
                if (player.level() == target.itemEntity().level()) {
                    player.setPos(target.itemEntity().getX(), target.itemEntity().getY(), target.itemEntity().getZ());
                }
            }
        } else {
            long droppedAt = data.getLong(APPLE_DROPPED_AT).orElse(-1L);
            if (droppedAt < 0L) {
                data.putLong(APPLE_DROPPED_AT, player.level().getGameTime());
                droppedAt = player.level().getGameTime();
            }
            if (player.level().getGameTime() - droppedAt >= DROP_TIMEOUT) {
                releaseApple(player, false, false);
                syncState(player);
                return;
            }
        }

        int stage = (int) Math.min(4L, age < TICKS_PER_DAY ? 0L : 1L + ((age - TICKS_PER_DAY) / (3L * TICKS_PER_DAY)));
        int oldStage = data.getInt(ROT_STAGE).orElse(0);
        if (stage != oldStage) {
            data.putInt(ROT_STAGE, stage);
            updateAppleStage(player, stage);
            int applied = data.getInt(APPLIED_ATTRITION).orElse(0);
            while (applied < Math.min(3, stage)) {
                AttritionStatusEffect.apply(player, 1, (int) Math.min(Integer.MAX_VALUE, ROTTING_AWAY_AT));
                applied++;
            }
            data.putInt(APPLIED_ATTRITION, applied);
        }

        if (age >= ROTTING_AWAY_AT) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("locki rotted away"));
            releaseApple(player, true, true);
            syncState(player);
        }
    }

    private static AppleTarget findAppleTarget(ServerPlayer locki) {
        UUID owner = locki.getUUID();
        MinecraftServer server = locki.level().getServer();
        if (server == null) return null;
        for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
            for (ItemStack stack : candidate.getInventory().getNonEquipmentItems()) {
                if (isOwnedApple(stack, owner)) return new AppleTarget(null, candidate, stack == candidate.getMainHandItem(), stack);
            }
            ItemStack offhand = candidate.getOffhandItem();
            if (isOwnedApple(offhand, owner)) return new AppleTarget(null, candidate, false, offhand);
        }

        String uuidString = locki.getPersistentData().getString(APPLE_UUID).orElse("");
        if (!uuidString.isBlank()) {
            try {
                UUID id = UUID.fromString(uuidString);
                for (ServerLevel level : server.getAllLevels()) {
                    Entity entity = level.getEntity(id);
                    if (entity instanceof ItemEntity item && isOwnedApple(item.getItem(), owner)) return new AppleTarget(item, null, false, item.getItem());
                }
            } catch (IllegalArgumentException ignored) {}
        }
        // A player may drop the apple, causing Minecraft to replace the item entity UUID.
        for (ItemEntity item : locki.level().getEntitiesOfClass(ItemEntity.class, locki.getBoundingBox().inflate(64.0), e -> isOwnedApple(e.getItem(), owner))) {
            return new AppleTarget(item, null, false, item.getItem());
        }
        return null;
    }

    private static boolean isOwnedApple(ItemStack stack, UUID owner) {
        if (stack.isEmpty() || !(stack.getItem() instanceof LockiAppleItem)) return false;
        String id = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            .copyTag().getString("locki:owner").orElse("");
        return owner.toString().equals(id);
    }

    private static void updateAppleStage(ServerPlayer owner, int stage) {
        MinecraftServer server = owner.level().getServer();
        if (server == null) return;
        UUID uuid = owner.getUUID();
        CompoundTag data = owner.getPersistentData();
        for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
            for (int i = 0; i < candidate.getInventory().getNonEquipmentItems().size(); i++) {
                ItemStack stack = candidate.getInventory().getNonEquipmentItems().get(i);
                if (isOwnedApple(stack, uuid)) candidate.getInventory().getNonEquipmentItems().set(i, makeAppleStack(uuid, stage, data));
            }
            ItemStack offhand = candidate.getOffhandItem();
            if (isOwnedApple(offhand, uuid)) candidate.setItemSlot(EquipmentSlot.OFFHAND, makeAppleStack(uuid, stage, data));
        }
        AppleTarget target = findAppleTarget(owner);
        if (target != null && target.itemEntity() != null) target.itemEntity().setItem(makeAppleStack(uuid, stage, data));
    }

    private static void releaseApple(ServerPlayer player, boolean oldAge, boolean removeFruit) {
        CompoundTag data = player.getPersistentData();
        AppleTarget target = findAppleTarget(player);
        double x = data.getDouble(APPLE_X).orElse(player.getX());
        double y = data.getDouble(APPLE_Y).orElse(player.getY());
        double z = data.getDouble(APPLE_Z).orElse(player.getZ());
        if (target != null) {
            if (target.itemEntity() != null) {
                x = target.itemEntity().getX(); y = target.itemEntity().getY(); z = target.itemEntity().getZ();
                if (removeFruit) target.itemEntity().discard();
                else {
                    target.itemEntity().setItem(new ItemStack(Items.APPLE));
                    target.itemEntity().setNoGravity(false);
                }
            }
        }
        MinecraftServer server = player.level().getServer();
        if (server != null) {
            for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
                for (int i = 0; i < candidate.getInventory().getNonEquipmentItems().size(); i++) {
                    ItemStack stack = candidate.getInventory().getNonEquipmentItems().get(i);
                    if (!isOwnedApple(stack, player.getUUID())) continue;
                    candidate.getInventory().getNonEquipmentItems().set(i, removeFruit ? ItemStack.EMPTY : new ItemStack(Items.APPLE));
                }
                ItemStack offhand = candidate.getOffhandItem();
                if (isOwnedApple(offhand, player.getUUID())) {
                    candidate.setItemSlot(EquipmentSlot.OFFHAND, removeFruit ? ItemStack.EMPTY : new ItemStack(Items.APPLE));
                }
            }
        }
        if (removeFruit) {
            spawnOriginSapling(player);
        }
        data.remove(APPLE_UUID);
        data.remove(APPLE_AGE);
        data.remove(APPLE_DROPPED_AT);
        data.remove(APPLE_HAS_BEEN_CARRIED);
        data.remove(APPLE_DIM);
        data.remove(APPLE_DROP_X);
        data.remove(APPLE_DROP_Y);
        data.remove(APPLE_DROP_Z);
        data.remove(APPLE_DROP_DIM);
        data.putInt(ROT_STAGE, 0);
        data.putInt(APPLIED_ATTRITION, 0);
        player.removeEffect(RequiemStatusEffects.ATTRITION);
        setMode(player, MODE_SPIRIT);
        resetPhysicalState(player);
    }

    private static void removeOwnedApplesNear(ServerLevel level, UUID owner, double x, double y, double z) {
        AABB search = new AABB(x - 32.0, Math.max(level.getMinY(), y - 16.0), z - 32.0,
            x + 32.0, Math.min(level.getMaxY(), y + 16.0), z + 32.0);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, search,
                entity -> isOwnedApple(entity.getItem(), owner))) {
            item.discard();
        }
    }

    /** Called by LockiAppleItem after the eater completes the normal apple eating animation. */
    public static void onAppleEaten(ItemStack stack, net.minecraft.world.entity.LivingEntity eater) {
        if (!(eater.level() instanceof ServerLevel eaterLevel)) return;
        String ownerId = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            .copyTag().getString("locki:owner").orElse("");
        UUID ownerUuid;
        try {
            ownerUuid = UUID.fromString(ownerId);
        } catch (IllegalArgumentException invalidUuid) {
            return;
        }
        MinecraftServer server = eaterLevel.getServer();
        if (server == null) return;
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerUuid);
        if (owner == null || !MODE_APPLE.equals(owner.getPersistentData().getString(MODE).orElse(MODE_NONE))) return;

        CompoundTag data = owner.getPersistentData();
        String destinationDimension = data.getString(APPLE_DROP_DIM)
            .orElseGet(() -> data.getString(APPLE_DIM).orElse(eaterLevel.dimension().identifier().toString()));
        boolean hasDropSite = data.contains(APPLE_DROP_X) && data.contains(APPLE_DROP_Y) && data.contains(APPLE_DROP_Z);
        double x = hasDropSite ? data.getDouble(APPLE_DROP_X).orElse(owner.getX()) : data.getDouble(APPLE_X).orElse(owner.getX());
        double y = hasDropSite ? data.getDouble(APPLE_DROP_Y).orElse(owner.getY()) : data.getDouble(APPLE_Y).orElse(owner.getY());
        double z = hasDropSite ? data.getDouble(APPLE_DROP_Z).orElse(owner.getZ()) : data.getDouble(APPLE_Z).orElse(owner.getZ());
        ResourceKey<Level> destinationKey;
        try {
            destinationKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(destinationDimension));
        } catch (Exception ignored) {
            destinationKey = eaterLevel.dimension();
        }
        ServerLevel destination = server.getLevel(destinationKey);
        if (destination == null) destination = eaterLevel;

        // Delete remaining copies near every persisted location before clearing the apple state.
        // A carried apple may be hundreds of blocks from the soul's original tree, so using only
        // the owner's current bounding box can leave an edible duplicate behind.
        for (ServerLevel level : server.getAllLevels()) {
            removeOwnedApplesNear(level, ownerUuid, data.getDouble(APPLE_X).orElse(owner.getX()),
                data.getDouble(APPLE_Y).orElse(owner.getY()), data.getDouble(APPLE_Z).orElse(owner.getZ()));
            if (hasDropSite && destination.dimension().equals(level.dimension())) {
                removeOwnedApplesNear(level, ownerUuid, x, y, z);
            }
            for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
                if (candidate.level() == level) {
                    removeOwnedApplesNear(level, ownerUuid, candidate.getX(), candidate.getY(), candidate.getZ());
                }
            }
        }
        for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
            for (int i = 0; i < candidate.getInventory().getNonEquipmentItems().size(); i++) {
                ItemStack candidateStack = candidate.getInventory().getNonEquipmentItems().get(i);
                if (candidateStack != stack && isOwnedApple(candidateStack, ownerUuid)) {
                    candidate.getInventory().getNonEquipmentItems().set(i, ItemStack.EMPTY);
                }
            }
            ItemStack offhand = candidate.getOffhandItem();
            if (offhand != stack && isOwnedApple(offhand, ownerUuid)) {
                candidate.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            }
        }

        spawnOriginSapling(owner);
        data.remove(APPLE_UUID);
        data.remove(APPLE_AGE);
        data.remove(APPLE_DROPPED_AT);
        data.remove(APPLE_HAS_BEEN_CARRIED);
        data.remove(APPLE_DIM);
        data.remove(APPLE_DROP_X);
        data.remove(APPLE_DROP_Y);
        data.remove(APPLE_DROP_Z);
        data.remove(APPLE_DROP_DIM);
        data.putInt(ROT_STAGE, 0);
        data.putInt(APPLIED_ATTRITION, 0);
        owner.removeEffect(RequiemStatusEffects.ATTRITION);

        // Becoming mortal recreates the tangible player body. Teleport after the state change so
        // body-regeneration hooks cannot send the newly born player back to the tree.
        RemnantComponent.get(owner).become(RemnantTypes.MORTAL, true);
        if (owner.level() != destination) {
            owner.teleportTo(destination, x, y, z, Set.of(), owner.getYRot(), owner.getXRot(), false);
        } else {
            owner.connection.teleport(x, y, z, owner.getYRot(), owner.getXRot());
        }
        setMode(owner, MODE_NONE);
        resetPhysicalState(owner);
        owner.setDeltaMovement(Vec3.ZERO);
        owner.resetFallDistance();
        owner.sendSystemMessage(net.minecraft.network.chat.Component.literal("You are born from the apple, at the place it was last dropped."));
        syncState(owner);
    }

    public static void onDeath(ServerPlayer player) {
        if (MODE_APPLE.equals(player.getPersistentData().getString(MODE).orElse(MODE_NONE))) {
            releaseApple(player, true, true);
            syncState(player);
        }
    }

    private static void spawnOriginSapling(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        String dimension = data.getString(TREE_DIM).orElse(player.level().dimension().identifier().toString());
        ResourceKey<Level> dimensionKey;
        try {
            dimensionKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimension));
        } catch (Exception ignored) {
            dimensionKey = player.level().dimension();
        }
        ServerLevel level = player.level().getServer() == null ? null : player.level().getServer().getLevel(dimensionKey);
        if (level == null) level = (player.level() instanceof ServerLevel currentLevel ? currentLevel : null);

        BlockPos source = new BlockPos(data.getInt(TREE_X).orElse(player.blockPosition().getX()),
            data.getInt(TREE_Y).orElse(player.blockPosition().getY()), data.getInt(TREE_Z).orElse(player.blockPosition().getZ()));
        String saplingName = data.getString(TREE_SAPLING).orElse("minecraft:oak_sapling");
        Block sapling;
        try {
            sapling = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(saplingName))
                .filter(block -> block instanceof SaplingBlock || block instanceof net.minecraft.world.level.block.BushBlock)
                .orElse(net.minecraft.world.level.block.Blocks.OAK_SAPLING);
        } catch (Exception ignored) {
            sapling = net.minecraft.world.level.block.Blocks.OAK_SAPLING;
        }
        BlockState saplingState = sapling.defaultBlockState();
        // The original trunk occupies its old position; find a nearby viable planting spot.
        for (int radius = 0; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    BlockPos candidate = new BlockPos(source.getX() + dx, source.getY(), source.getZ() + dz);
                    if (candidate.getY() >= level.getMaxY() - 1) continue;
                    if (level.getBlockState(candidate).isAir() && saplingState.canSurvive(level, candidate)) {
                        level.setBlock(candidate, saplingState, 3);
                        return;
                    }
                }
            }
        }
    }

    private static void leaveRoots(ServerPlayer player) {
        setMode(player, MODE_SPIRIT);
        resetPhysicalState(player);
    }

    private static void resetPhysicalState(ServerPlayer player) {
        String mode = player.getPersistentData().getString(MODE).orElse(MODE_NONE);
        player.setInvisible(MODE_SPIRIT.equals(mode) || MODE_ROOTS.equals(mode)
            || MODE_CONVERSATION.equals(mode) || MODE_APPLE.equals(mode));
        if (!player.isPassenger()) player.setPose(net.minecraft.world.entity.Pose.STANDING);
        if (!ladysnake.requiem.api.v1.entity.MovementAlterer.get(player).isNoClipping()) player.noPhysics = false;
        // Preserve vanilla creative/spectator flight; Locki only suspends it while a special body/view is active.
        if (player.isCreative() || player.isSpectator() || RemnantComponent.isIncorporeal(player)) {
            player.getAbilities().mayfly = true;
        }
        player.onUpdateAbilities();
    }

    private static void setMode(ServerPlayer player, String mode) {
        player.getPersistentData().putString(MODE, mode);
        boolean hidden = MODE_SPIRIT.equals(mode) || MODE_ROOTS.equals(mode)
            || MODE_APPLE.equals(mode) || MODE_CONVERSATION.equals(mode);
        if (!MODE_ROOTS.equals(mode) && !MODE_APPLE.equals(mode) && !MODE_CONVERSATION.equals(mode)) {
            if (!ladysnake.requiem.api.v1.entity.MovementAlterer.get(player).isNoClipping()) player.noPhysics = false;
        }
        player.setInvisible(hidden);
    }

    private static void syncState(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        String mode = data.getString(MODE).orElse(MODE_NONE);
        int cameraMode = 0;
        int cameraEntityId = player.getId();
        int rot = data.getInt(ROT_STAGE).orElse(0);
        if (MODE_ROOTS.equals(mode)) {
            cameraMode = 1;
        } else if (MODE_APPLE.equals(mode)) {
            AppleTarget target = findAppleTarget(player);
            if (target != null && target.holder() != null) {
                cameraEntityId = target.holder().getId();
                cameraMode = target.inMainHand() ? 2 : 3;
            } else if (target != null && target.itemEntity() != null) {
                cameraEntityId = target.itemEntity().getId();
                cameraMode = 4;
            } else {
                cameraMode = 4;
            }
        }
        String previousMode = data.getString(SYNC_MODE).orElse("");
        int previousTarget = data.getInt(SYNC_TARGET).orElse(Integer.MIN_VALUE);
        int previousCamera = data.getInt(SYNC_CAMERA).orElse(Integer.MIN_VALUE);
        int previousRot = data.getInt(SYNC_ROT).orElse(Integer.MIN_VALUE);
        if (!mode.equals(previousMode) || cameraEntityId != previousTarget || cameraMode != previousCamera || rot != previousRot) {
            data.putString(SYNC_MODE, mode);
            data.putInt(SYNC_TARGET, cameraEntityId);
            data.putInt(SYNC_CAMERA, cameraMode);
            data.putInt(SYNC_ROT, rot);
            RequiemNetworking.LockiStatePayload payload = new RequiemNetworking.LockiStatePayload(player.getId(), mode, cameraEntityId, cameraMode, rot);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload);
        }
    }

    public static void clearNetwork(UUID id) {
        NETWORKS.remove(id);
    }
}

package ladysnake.requiem.common.locki;

import ladysnake.requiem.Requiem;
import ladysnake.requiem.common.item.RequiemItems;
import ladysnake.requiem.common.entity.RequiemEntities;
import ladysnake.requiem.common.entity.effect.RequiemStatusEffects;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

import java.util.Set;
import java.util.UUID;

/** Fixed arena dimension and return-session bookkeeping for the unnamed Nucleus boss encounter. */
public final class NucleusManager {
    public static final ResourceKey<Level> NUCLEUS_DIMENSION = ResourceKey.create(Registries.DIMENSION, Requiem.id("nucleus"));
    private static final String BOSS_KEY = "requiem:nucleus_boss";
    private static final String BOSS_OWNER = "requiem:nucleus_boss_owner";
    private static final String PENDING_INVITE = "requiem:nucleus_pending_invite";
    private static final String SESSION_OWNER = "requiem:nucleus_session_owner";
    private static final String SESSION_DEV = "requiem:nucleus_session_developer";
    private static final String RETURN_X = "requiem:nucleus_return_x";
    private static final String RETURN_Y = "requiem:nucleus_return_y";
    private static final String RETURN_Z = "requiem:nucleus_return_z";
    private static final String RETURN_YAW = "requiem:nucleus_return_yaw";
    private static final String RETURN_PITCH = "requiem:nucleus_return_pitch";
    private static final String RETURN_DIM = "requiem:nucleus_return_dimension";
    private static final String HEAD_OWNER = "requiem:nucleus_owner";

    private NucleusManager() {}

    public static boolean tryEnter(ServerPlayer phoenix, BlockPos clicked) {
        if (!PhenexAbilitySystem.isPhenex(phoenix) || !PhenexAbilitySystem.isAbilityActive(phoenix, "roubar")) return false;
        if (phoenix.getXRot() < 89.0F) return false;
        if (!(phoenix.level() instanceof ServerLevel source)) return false;
        ServerLevel nucleus = source.getServer().getLevel(NUCLEUS_DIMENSION);
        if (nucleus == null) {
            phoenix.sendSystemMessage(Component.literal("Nucleus is not available: the dimension data may not have loaded."));
            return true;
        }
        String owner = phoenix.getUUID().toString();
        saveReturn(phoenix, owner);
        ensureArena(nucleus, phoenix);
        phoenix.addEffect(new MobEffectInstance(RequiemStatusEffects.NUCLEUS_TRANSITION, 35, 0, false, false, false));
        phoenix.teleportTo(nucleus, 0.5, 80.0, 0.5, Set.of(), -90.0F, 0.0F, false);
        phoenix.playSound(SoundEvents.PORTAL_TRAVEL, 0.8F, 0.7F);
        phoenix.sendSystemMessage(Component.literal("§eYellow light swallows the world. You have entered Nucleus."));
        inviteDevelopers(source.getServer(), phoenix);
        return true;
    }

    private static void ensureArena(ServerLevel level, ServerPlayer phoenix) {
        buildFireArenaIfNeeded(level);
        BlockPos phoenixMarker = new BlockPos(0, 79, 0);
        BlockPos devMarker = new BlockPos(100, 79, 0);
        level.setBlock(phoenixMarker, Blocks.CONCRETE.pick(DyeColor.MAGENTA).defaultBlockState(), 3);
        level.setBlock(devMarker, Blocks.CONCRETE.pick(DyeColor.YELLOW).defaultBlockState(), 3);

        // The sovereign is played by a consenting developer, not by a mob entity.
        // Clean up the Warden placeholder left behind by older versions.
        AABB oldBossArea = new AABB(94, 80, -6, 106, 88, 6);
        for (Mob oldBoss : level.getEntitiesOfClass(Mob.class, oldBossArea,
                entity -> entity.getPersistentData().getBoolean(BOSS_KEY).orElse(false))) {
            oldBoss.discard();
        }
    }

    /** Builds a small one-time blackstone/magma arena when the void dimension is first entered. */
    private static void buildFireArenaIfNeeded(ServerLevel level) {
        BlockPos marker = new BlockPos(-16, 79, -16);
        if (!level.getBlockState(marker).isAir()) return;

        for (int x = -16; x <= 116; x++) {
            for (int z = -16; z <= 16; z++) {
                BlockPos pos = new BlockPos(x, 79, z);
                boolean lavaChannel = (x == 26 || x == 52 || x == 78)
                    && Math.abs(z) < 15 && Math.floorMod(z, 7) != 0;
                net.minecraft.world.level.block.state.BlockState floor;
                if (lavaChannel) {
                    floor = Blocks.LAVA.defaultBlockState();
                } else if (x == -16 || x == 116 || Math.abs(z) == 16) {
                    floor = Blocks.BASALT.defaultBlockState();
                } else if (Math.floorMod(x * 31 + z * 17, 13) == 0) {
                    floor = Blocks.MAGMA_BLOCK.defaultBlockState();
                } else {
                    floor = Blocks.BLACKSTONE.defaultBlockState();
                }
                level.setBlock(pos, floor, 3);
            }
        }
    }

    private static void inviteDevelopers(MinecraftServer server, ServerPlayer phoenix) {
        for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
            if (candidate == phoenix || !isDeveloper(candidate)) continue;
            candidate.getPersistentData().putString(PENDING_INVITE, phoenix.getUUID().toString());
            candidate.sendSystemMessage(Component.literal("§dNucleus arena invitation from " + phoenix.getGameProfile().name() + ". Type `join` to accept."));
        }
    }

    public static boolean isDeveloper(Player player) {
        return player.entityTags().contains("dev") || player.entityTags().contains("DEV")
            || player.entityTags().contains("Developer") || player.entityTags().contains("tagDeveloper");
    }

    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String pending = player.getPersistentData().getString(PENDING_INVITE).orElse("");
        String message = event.getRawText().trim();
        if (!pending.isBlank() && (message.equalsIgnoreCase("join") || message.equalsIgnoreCase("accept"))) {
            event.setCanceled(true);
            joinArena(player, pending);
            return;
        }
        if (!pending.isBlank() && message.equalsIgnoreCase("decline")) {
            event.setCanceled(true);
            player.getPersistentData().remove(PENDING_INVITE);
            player.sendSystemMessage(Component.literal("Nucleus invitation declined."));
        }
    }

    private static void joinArena(ServerPlayer developer, String ownerString) {
        MinecraftServer server = developer.level().getServer();
        if (server == null) return;
        ServerPlayer phoenix = null;
        try { phoenix = server.getPlayerList().getPlayer(UUID.fromString(ownerString)); } catch (IllegalArgumentException ignored) { }
        if (phoenix == null || phoenix.level().dimension() != NUCLEUS_DIMENSION) {
            developer.getPersistentData().remove(PENDING_INVITE);
            developer.sendSystemMessage(Component.literal("That Nucleus encounter is no longer active."));
            return;
        }
        if (!isDeveloper(developer)) {
            developer.getPersistentData().remove(PENDING_INVITE);
            developer.sendSystemMessage(Component.literal("Only a tagged developer can accept this Nucleus role."));
            return;
        }
        String assignedDeveloper = phoenix.getPersistentData().getString(SESSION_DEV).orElse("");
        if (!assignedDeveloper.isBlank() && !assignedDeveloper.equals(developer.getUUID().toString())) {
            developer.getPersistentData().remove(PENDING_INVITE);
            developer.sendSystemMessage(Component.literal("Another developer has already accepted this Nucleus encounter."));
            return;
        }
        if (!developer.getPersistentData().getString(SESSION_OWNER).orElse("").isBlank()) {
            developer.sendSystemMessage(Component.literal("You already have an active Nucleus session."));
            return;
        }
        phoenix.getPersistentData().putString(SESSION_DEV, developer.getUUID().toString());
        saveReturn(developer, ownerString);
        ServerLevel nucleus = server.getLevel(NUCLEUS_DIMENSION);
        if (nucleus == null) return;
        developer.getPersistentData().remove(PENDING_INVITE);
        // Consent turns the developer's own player into the encounter's sovereign.
        // No Warden, husk or other boss entity is spawned at this marker.
        developer.getPersistentData().putBoolean(BOSS_KEY, true);
        developer.getPersistentData().putString(BOSS_OWNER, ownerString);
        developer.addTag("requiem_nucleus_player_boss");
        developer.teleportTo(nucleus, 100.5, 80.0, 4.5, Set.of(), 90.0F, 0.0F, false);
        developer.sendSystemMessage(Component.literal("You accepted the Nucleus sovereign role. You are the boss player; your inventory is preserved. Claim your opponent's Suffering Head by defeating Phenex."));
        phoenix.sendSystemMessage(Component.literal(developer.getGameProfile().name() + " accepted the Nucleus sovereign role."));
    }

    private static void saveReturn(ServerPlayer player, String owner) {
        CompoundTag data = player.getPersistentData();
        data.putString(SESSION_OWNER, owner);
        data.putString(RETURN_DIM, player.level().dimension().location().toString());
        data.putDouble(RETURN_X, player.getX());
        data.putDouble(RETURN_Y, player.getY());
        data.putDouble(RETURN_Z, player.getZ());
        data.putFloat(RETURN_YAW, player.getYRot());
        data.putFloat(RETURN_PITCH, player.getXRot());
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.getPersistentData().getBoolean(BOSS_KEY).orElse(false)) return;
        Entity killer = event.getSource().getEntity();
        String owner = entity.getPersistentData().getString(BOSS_OWNER).orElse("");
        if (!(killer instanceof ServerPlayer phenex) || !PhenexAbilitySystem.isPhenex(phenex)
                || !phenex.getUUID().toString().equals(owner)) {
            return;
        }

        PhenexAbilitySystem.addCorruption(phenex, 1);
        phenex.sendSystemMessage(Component.literal("§7The Nucleus sovereign falls. Ash settles in your lungs. Claim the Suffering Head to leave."));

        // The developer is the boss player. Reward the victorious Phenex without replacing the
        // developer's own inventory drops or treating the player as a disposable mob entity.
        if (entity instanceof ServerPlayer developer && entity.level() instanceof ServerLevel level) {
            ItemEntity drop = new ItemEntity(level, developer.getX(), developer.getY() + 0.3, developer.getZ(),
                makeOwnedSufferingHead(owner));
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
            developer.getPersistentData().remove(BOSS_KEY);
            developer.getPersistentData().remove(BOSS_OWNER);
            developer.removeTag("requiem_nucleus_player_boss");
            developer.sendSystemMessage(Component.literal("You were defeated in the Nucleus encounter. Your opponent's Suffering Head has dropped; normal player drops remain unchanged."));
        }
    }

    private static ItemStack makeOwnedSufferingHead(String owner) {
        ItemStack head = new ItemStack(RequiemItems.SUFFERING_HEAD);
        CustomData.update(DataComponents.CUSTOM_DATA, head, tag -> tag.putString(HEAD_OWNER, owner));
        return head;
    }

    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (event.getEntity().getPersistentData().getBoolean(BOSS_KEY).orElse(false)) {
            event.setCanceled(true);
        }
    }

    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.getPersistentData().getBoolean(BOSS_KEY).orElse(false)) return;
        // Player-boss rewards are created by onLivingDeath; never clear a developer's inventory drops.
        if (entity instanceof ServerPlayer) return;
        event.getDrops().clear();
        String owner = entity.getPersistentData().getString(BOSS_OWNER).orElse("");
        event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), makeOwnedSufferingHead(owner)));
    }

    /** Captures the owner before the pickup empties the ItemEntity, then returns the whole encounter. */
    public static void onPickupPre(ItemEntityPickupEvent.Pre event) {
        ItemStack stack = event.getItemEntity().getItem();
        if (!stack.is(RequiemItems.SUFFERING_HEAD)) return;
        String owner = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(HEAD_OWNER).orElse("");
        if (owner.isBlank()) return;
        if (!event.getPlayer().getUUID().toString().equals(owner)) {
            event.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
            return;
        }
        MinecraftServer server = event.getPlayer().level().getServer();
        if (server != null) server.execute(() -> returnSession(server, owner));
    }

    private static void returnSession(MinecraftServer server, String owner) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!owner.equals(player.getPersistentData().getString(SESSION_OWNER).orElse(""))) continue;
            CompoundTag data = player.getPersistentData();
            ResourceLocation id = ResourceLocation.tryParse(data.getString(RETURN_DIM).orElse("minecraft:overworld"));
            if (id == null) id = ResourceLocation.withDefaultNamespace("overworld");
            ServerLevel target = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
            if (target == null) target = server.overworld();
            double x = data.getDouble(RETURN_X).orElse(0.0);
            double y = data.getDouble(RETURN_Y).orElse(80.0);
            double z = data.getDouble(RETURN_Z).orElse(0.0);
            float yaw = data.getFloat(RETURN_YAW).orElse(0.0F);
            float pitch = data.getFloat(RETURN_PITCH).orElse(0.0F);
            player.teleportTo(target, x, y, z, Set.of(), yaw, pitch, false);
            data.remove(SESSION_OWNER);
            data.remove(SESSION_DEV);
            data.remove(RETURN_DIM);
            data.remove(RETURN_X);
            data.remove(RETURN_Y);
            data.remove(RETURN_Z);
            data.remove(RETURN_YAW);
            data.remove(RETURN_PITCH);
            player.sendSystemMessage(Component.literal("§eThe Suffering Head is claimed. You return to where you entered Nucleus."));
        }
    }
}

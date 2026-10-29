package ladysnake.requiem.common.locki;

import ladysnake.requiem.common.item.RequiemItems;
import ladysnake.requiem.common.entity.RequiemEntities;
import ladysnake.requiem.common.entity.HomunculusEntity;
import ladysnake.requiem.common.entity.ImmortalHomunculusEntity;
import ladysnake.requiem.common.network.RequiemNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Locale;

/** Marbas' embryo, homunculus brain, cauldron rituals, and immortal transmutation. */
public final class MarbasAbilitySystem {
    public static final String TAG = "marbas";
    public static final String HOMUNCULUS = "requiem:homunculus";
    public static final String BRAIN = "requiem:homunculus_brain";
    private static final String TRANSFERRED_BRAIN = "requiem:homunculus_souled";
    private static final String ATTACK_COOLDOWN_UNTIL = "requiem:homunculus_attack_cooldown_until";
    private static final String SAY_COOLDOWN_UNTIL = "requiem:homunculus_say_cooldown_until";
    private static final String STORED_ENTITY_NBT = "requiem:homunculus_full_entity_nbt";
    private static final String STORED_ENTITY_TYPE = "requiem:homunculus_body_type";
    public static final String BORN_AT = "requiem:homunculus_born_at";
    public static final String HOME = "requiem:homunculus_home";
    public static final String OWNER = "requiem:homunculus_owner";
    public static final String BERSERK = "requiem:suffering_berserk";
    private static final String NODE = "requiem:homunculus_node";
    private static final String TARGET_ID = "requiem:homunculus_target_id";
    private static final String STUN_UNTIL = "requiem:homunculus_stun_until";
    private static final String FED_UNTIL = "requiem:homunculus_fed_until";
    private static final String TRAITS = "requiem:homunculus_traits";
    private static final String EMBRYO_MARKER = "requiem:homunculus_embryo_marker";
    private static final String EMBRYO_STARTED_AT = "requiem:homunculus_embryo_started_at";
    private static final String EMBRYO_CLOCK_DAY_TIME = "requiem:homunculus_embryo_uses_day_time";
    private static final long EMBRYO_GESTATION_TICKS = 3L * 24_000L;

    private MarbasAbilitySystem() {}

    public static boolean isMarbas(Player player) {
        return player != null && (player.entityTags().contains(TAG) || player.entityTags().contains("Marbas"));
    }

    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (!isMarbas(player)) return;
        String phrase = event.getRawText().trim().toLowerCase(Locale.ROOT);
        if (!phrase.equals("embryo") && !phrase.equals("fetus")) return;
        event.setCanceled(true);
        ItemStack head = findNamedPlayerHead(player);
        if (head.isEmpty()) {
            player.sendSystemMessage(Component.literal("Hold a player head named `Embryo` or `Fetus`, then say that word again."));
            return;
        }
        if (!player.getAbilities().instabuild) head.shrink(1);
        ItemStack embryo = new ItemStack(RequiemItems.EMBRYO);
        embryo.set(DataComponents.CUSTOM_NAME, Component.literal(phrase.equals("fetus") ? "Fetus" : "Embryo"));
        CustomData.update(DataComponents.CUSTOM_DATA, embryo, tag -> {
            tag.putString(BRAIN, "return home");
            tag.putString(OWNER, player.getUUID().toString());
            tag.putString("requiem:donor_head_name", "Embryo");
        });
        if (!player.getInventory().add(embryo)) player.drop(embryo, false);
        player.sendSystemMessage(Component.literal("A named Embryo has been prepared. Place it in a water-filled cauldron to grow a homunculus."));
    }

    private static ItemStack findNamedPlayerHead(Player player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.is(Items.PLAYER_HEAD)) continue;
            String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            if (name.contains("embryo") || name.contains("fetus")) return stack;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Re-place a harvested homunculus on the clicked block face. The item snapshot is consumed
     * only after the entity has been successfully restored and added to the world.
     */
    public static boolean tryPlaceStoredHomunculus(ServerPlayer player, ItemStack stack, BlockPos placementPos) {
        if (!isMarbas(player) || !(player.level() instanceof ServerLevel level)
                || !stack.is(RequiemItems.HOMUNCULUS)) {
            return false;
        }

        CompoundTag itemData = readItemData(stack);
        CompoundTag savedEntity = itemData.getCompound(STORED_ENTITY_NBT).orElse(null);
        if (savedEntity == null) return false;

        Entity created = RequiemEntities.HOMUNCULUS.create(level, EntitySpawnReason.TRIGGERED);
        if (!(created instanceof Mob restored)) {
            player.sendSystemMessage(Component.literal("The stored homunculus body could not be reconstructed."));
            return true;
        }

        try {
            restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), savedEntity.copy()));
        } catch (RuntimeException invalidSnapshot) {
            player.sendSystemMessage(Component.literal("The stored homunculus data could not be read. The item was not consumed."));
            return true;
        }

        restored.setPos(placementPos.getX() + 0.5D, placementPos.getY(), placementPos.getZ() + 0.5D);
        restored.setDeltaMovement(Vec3.ZERO);
        restored.setPersistenceRequired();
        if (!level.noCollision(restored)) {
            player.sendSystemMessage(Component.literal("There is not enough room to place the homunculus. The item was not consumed."));
            return true;
        }
        if (!level.addFreshEntity(restored)) {
            player.sendSystemMessage(Component.literal("The homunculus could not be placed. The item was not consumed."));
            return true;
        }

        if (!player.getAbilities().instabuild) stack.shrink(1);
        playSoulTransferEffect(level, restored, true);
        player.sendSystemMessage(Component.literal("The homunculus has been placed with its previous body data and brain progress intact."));
        return true;
    }

    /** Called from the right-click-block event before ordinary incorporeal restrictions. */
    public static boolean onUseBlock(ServerPlayer player, ItemStack stack, BlockPos pos, boolean shift) {
        if (!isMarbas(player) || !(player.level() instanceof ServerLevel level)) return false;
        BlockState clicked = level.getBlockState(pos);
        if (!clicked.is(Blocks.WATER_CAULDRON)) return false;

        if (stack.is(RequiemItems.EMBRYO)) {
            if (findEmbryoMarker(level, pos) != null) {
                player.sendSystemMessage(Component.literal("This cauldron already contains an embryo."));
                return true;
            }
            ItemStack embryoStack = stack.copy();
            embryoStack.setCount(1);
            ItemEntity marker = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5, embryoStack);
            marker.setNoGravity(true);
            marker.setDeltaMovement(Vec3.ZERO);
            marker.setPickUpDelay(32767);
            marker.setUnlimitedLifetime();
            marker.getPersistentData().putBoolean(EMBRYO_MARKER, true);
            // Day time advances when players sleep, unlike gameTime (which counts server ticks).
            marker.getPersistentData().putLong(EMBRYO_STARTED_AT, level.getOverworldClockTime());
            marker.getPersistentData().putBoolean(EMBRYO_CLOCK_DAY_TIME, true);
            marker.getPersistentData().putString(OWNER, readItemData(stack).getString(OWNER).orElse(player.getUUID().toString()));
            if (!level.addFreshEntity(marker)) {
                player.sendSystemMessage(Component.literal("The embryo could not be placed in the cauldron."));
                return true;
            }
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.sendSystemMessage(Component.literal("The embryo is incubating in the water cauldron. It needs three in-game days before sulfur can complete the homunculus."));
            return true;
        }

        // A harvested homunculus carries a full entity snapshot. Restore it before checking the
        // head-fusion recipes so simply placing it back preserves health, attributes, inventory,
        // custom name, brain program/progress, traits, owner, home, and other persistent NBT.
        if (stack.is(RequiemItems.HOMUNCULUS)) {
            CompoundTag itemData = readItemData(stack);
            CompoundTag savedEntity = itemData.getCompound(STORED_ENTITY_NBT).orElse(null);
            if (savedEntity != null) {
                Entity created = RequiemEntities.HOMUNCULUS.create(level, EntitySpawnReason.TRIGGERED);
                if (!(created instanceof Mob restored)) {
                    player.sendSystemMessage(Component.literal("The stored homunculus body could not be reconstructed."));
                    return true;
                }
                try {
                    restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), savedEntity.copy()));
                } catch (RuntimeException invalidSnapshot) {
                    player.sendSystemMessage(Component.literal("The stored homunculus data could not be read. The item was not consumed."));
                    return true;
                }
                restored.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                restored.setDeltaMovement(Vec3.ZERO);
                restored.setPersistenceRequired();
                if (!level.addFreshEntity(restored)) {
                    player.sendSystemMessage(Component.literal("The homunculus could not be placed. The item was not consumed."));
                    return true;
                }
                if (!player.getAbilities().instabuild) stack.shrink(1);
                level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
                playSoulTransferEffect(level, restored, true);
                player.sendSystemMessage(Component.literal("The homunculus has been reconstituted with its previous body data and brain progress intact."));
                return true;
            }
        }

        if (stack.is(RequiemItems.HOMUNCULUS) && player.getOffhandItem().is(RequiemItems.SUFFERING_HEAD)) {
            Mob mob = growHomunculus(level, pos, player, "", "suffering");
            if (mob != null) mob.getPersistentData().putBoolean(BERSERK, true);
            if (!player.getAbilities().instabuild) { stack.shrink(1); player.getOffhandItem().shrink(1); }
            level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
            player.sendSystemMessage(Component.literal("The Suffering Head fuses with the homunculus. It is immortal, unstable, and will ignore its programmed brain."));
            return true;
        }

        if (stack.is(RequiemItems.HOMUNCULUS) && isMobHead(player.getOffhandItem())) {
            String headName = player.getOffhandItem().getHoverName().getString();
            String traits = traitsForHead(player.getOffhandItem());
            String program = readItemData(stack).getString(BRAIN).orElse("return home");
            Mob mob = growHomunculus(level, pos, player, program, traits);
            if (mob != null) mob.getPersistentData().putString(TRAITS, traits);
            if (!player.getAbilities().instabuild) { stack.shrink(1); player.getOffhandItem().shrink(1); }
            level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
            player.sendSystemMessage(Component.literal("The homunculus inherits traits from " + headName + "."));
            return true;
        }

        if (isSulfurIngredient(stack)) {
            ItemEntity embryoMarker = findEmbryoMarker(level, pos);
            if (embryoMarker != null) {
                CompoundTag incubationData = embryoMarker.getPersistentData();
                if (!incubationData.getBoolean(EMBRYO_CLOCK_DAY_TIME).orElse(false)) {
                    // Migrate embryos created by the prior build, which measured only server ticks
                    // and therefore ignored most of the day skipped when players slept.
                    long legacyStarted = incubationData.getLong(EMBRYO_STARTED_AT).orElse(level.getGameTime());
                    long legacyElapsed = Math.max(0L, level.getGameTime() - legacyStarted);
                    incubationData.putLong(EMBRYO_STARTED_AT, level.getOverworldClockTime() - legacyElapsed);
                    incubationData.putBoolean(EMBRYO_CLOCK_DAY_TIME, true);
                }
                long started = incubationData.getLong(EMBRYO_STARTED_AT).orElse(level.getOverworldClockTime());
                long elapsed = Math.max(0L, level.getOverworldClockTime() - started);
                if (elapsed < EMBRYO_GESTATION_TICKS) {
                    long remainingDays = (EMBRYO_GESTATION_TICKS - elapsed + 23_999L) / 24_000L;
                    player.sendSystemMessage(Component.literal("The embryo is still developing. Wait " + remainingDays + " more in-game day(s) before adding sulfur."));
                    return true;
                }
                ItemStack embryoStack = embryoMarker.getItem();
                CompoundTag embryoData = readItemData(embryoStack);
                Mob homunculus = growHomunculus(level, pos, player,
                    embryoData.getString(BRAIN).orElse("return home"),
                    embryoData.getString(TRAITS).orElse(""));
                if (homunculus == null) {
                    player.sendSystemMessage(Component.literal("The homunculus could not be formed; the embryo remains intact."));
                    return true;
                }
                String originalOwner = embryoMarker.getPersistentData().getString(OWNER).orElse(player.getUUID().toString());
                homunculus.getPersistentData().putString(OWNER, originalOwner);
                embryoMarker.discard();
                consumeSulfurIngredient(player, stack);
                level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
                player.sendSystemMessage(Component.literal("Sulfur completes the incubation. The homunculus awakens."));
                // Successful incubation opens the program editor immediately.
                RequiemNetworking.sendHomunculusBrainOpen(player, homunculus.getId(),
                    homunculus.getPersistentData().getString(BRAIN).orElse("return home"));
                return true;
            }

            Mob homunculus = level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(5.0),
                    mob -> hasProgrammableBrain(mob) && !(mob instanceof ImmortalHomunculusEntity))
                .stream().findFirst().orElse(null);
            if (homunculus == null) {
                player.sendSystemMessage(Component.literal("No programmable brain is nearby. Incubate an Embryo in water, or use sulfur on a homunculus / body carrying a transferred brain to edit its commands."));
                return true;
            }
            String program = homunculus.getPersistentData().getString(BRAIN).orElse("return home");
            consumeSulfurIngredient(player, stack);
            RequiemNetworking.sendHomunculusBrainOpen(player, homunculus.getId(), program);
            return true;
        }
        return false;
    }

    private static CompoundTag readItemData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    /** Match the special bucket by path so both configured namespaces work. */
    public static boolean isSulfurBucket(ItemStack stack) {
        return stack != null && !stack.isEmpty()
            && "sulfur_cube_bucket".equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
    }

    private static boolean isSulfurIngredient(ItemStack stack) {
        return stack.is(RequiemItems.SULFUR_CUBE) || isSulfurBucket(stack);
    }

    /**
     * Sulfur cube buckets behave like a container: on a successful ritual the sulfur is consumed
     * and an empty vanilla bucket is returned. On an incubation-time error nothing is consumed.
     */
    private static void consumeSulfurIngredient(ServerPlayer player, ItemStack stack) {
        if (player.getAbilities().instabuild) return;
        if (isSulfurBucket(stack)) {
            stack.shrink(1);
            ItemStack emptyBucket = new ItemStack(Items.BUCKET);
            if (!player.getInventory().add(emptyBucket)) player.drop(emptyBucket, false);
            return;
        }
        if (stack.is(RequiemItems.SULFUR_CUBE)) stack.shrink(1);
    }

    private static ItemEntity findEmbryoMarker(ServerLevel level, BlockPos cauldron) {
        AABB area = new AABB(cauldron).inflate(0.45, 0.8, 0.45);
        return level.getEntitiesOfClass(ItemEntity.class, area, item ->
            item.getPersistentData().getBoolean(EMBRYO_MARKER).orElse(false)
                && item.getItem().is(RequiemItems.EMBRYO))
            .stream().findFirst().orElse(null);
    }

    private static Mob growHomunculus(ServerLevel level, BlockPos cauldron, ServerPlayer owner, String brain, String traits) {
        // Both bodies are registered Requiem entities, not vanilla Husk/Wolf entities.
        boolean immortal = traits != null && traits.toLowerCase(Locale.ROOT).contains("suffering");
        Entity created = (immortal ? RequiemEntities.IMMORTAL_HOMUNCULUS : RequiemEntities.HOMUNCULUS)
            .create(level, EntitySpawnReason.TRIGGERED);
        if (!(created instanceof Mob mob)) return null;
        mob.setPos(cauldron.getX() + 0.5, cauldron.getY() + 1.0, cauldron.getZ() + 0.5);
        mob.setCustomName(Component.literal(immortal ? "Immortal Homunculus" : "Homunculus"));
        mob.setCustomNameVisible(true);
        mob.setPersistenceRequired();
        CompoundTag data = mob.getPersistentData();
        data.putBoolean(HOMUNCULUS, true);
        data.putBoolean(BERSERK, immortal);
        data.putString(BRAIN, immortal ? "" : (brain == null || brain.isBlank() ? "return home" : brain));
        data.putString(TRAITS, traits == null ? "" : traits);
        data.putString(OWNER, owner.getUUID().toString());
        data.putLong(BORN_AT, level.getGameTime());
        data.putString(HOME, cauldron.getX() + " " + (cauldron.getY() + 1) + " " + cauldron.getZ());
        var health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && traits != null && traits.contains("health")) health.setBaseValue(80.0);
        mob.setHealth(mob.getMaxHealth());
        if (!level.addFreshEntity(mob)) return null;
        playSoulTransferEffect(level, mob, true);
        return mob;
    }

    /** Gives soul/body transitions a readable particle cue and a restrained chime. */
    private static void playSoulTransferEffect(ServerLevel level, Entity entity, boolean restored) {
        double x = entity.getX();
        double y = entity.getY() + entity.getBbHeight() * 0.55D;
        double z = entity.getZ();
        level.sendParticles(ParticleTypes.SOUL, x, y, z, restored ? 24 : 12, 0.35, 0.45, 0.35, 0.04);
        level.sendParticles(restored ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.POOF,
            x, y, z, restored ? 18 : 10, 0.28, 0.38, 0.28, 0.06);
        level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME,
            SoundSource.NEUTRAL, 0.75F, restored ? 0.82F : 0.62F);
    }

    /** Replace the normal entity with the immortal type, keeping its origin and program metadata. */
    private static Mob transmuteToImmortal(ServerLevel level, Mob original) {
        if (original instanceof ImmortalHomunculusEntity) return original;
        Entity created = RequiemEntities.IMMORTAL_HOMUNCULUS.create(level, EntitySpawnReason.TRIGGERED);
        if (!(created instanceof Mob immortal)) return null;
        immortal.setPos(original.getX(), original.getY(), original.getZ());
        immortal.setYRot(original.getYRot());
        immortal.setXRot(original.getXRot());
        immortal.setYBodyRot(original.yBodyRot);
        immortal.setCustomName(Component.literal("Immortal Homunculus"));
        immortal.setCustomNameVisible(true);
        immortal.setPersistenceRequired();

        CompoundTag oldData = original.getPersistentData();
        CompoundTag data = immortal.getPersistentData();
        data.putBoolean(HOMUNCULUS, true);
        data.putBoolean(BERSERK, true);
        data.putString(BRAIN, "");
        data.putString(TRAITS, oldData.getString(TRAITS).orElse(""));
        data.putString(OWNER, oldData.getString(OWNER).orElse(""));
        data.putString(HOME, oldData.getString(HOME).orElse(
            original.blockPosition().getX() + " " + original.blockPosition().getY() + " " + original.blockPosition().getZ()));
        data.putLong(BORN_AT, oldData.getLong(BORN_AT).orElse(level.getGameTime()));
        data.putLong(STUN_UNTIL, 0L);
        if (!level.addFreshEntity(immortal)) return null;
        original.discard();
        return immortal;
    }

    public static boolean onEntityInteract(Player player, Entity target, ItemStack held, boolean shift) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(player.level() instanceof ServerLevel level)) return false;
        if (shift && target instanceof Mob homunculus && isHomunculus(homunculus)
                && held.is(RequiemItems.SUFFERING_HEAD)) {
            if (homunculus instanceof ImmortalHomunculusEntity) {
                serverPlayer.sendSystemMessage(Component.literal("This homunculus has already been transmuted."));
                return true;
            }
            Mob immortal = transmuteToImmortal(level, homunculus);
            if (immortal == null) {
                serverPlayer.sendSystemMessage(Component.literal("The transmutation failed; the head was not consumed."));
                return true;
            }
            if (!serverPlayer.getAbilities().instabuild) held.shrink(1);
            serverPlayer.sendSystemMessage(Component.literal("The Suffering Head transmutates the homunculus into a separate immortal creature."));
            return true;
        }
        if (target instanceof Mob homunculus && isHomunculus(homunculus) && held.is(Items.GOLD_INGOT)
                && homunculus.getPersistentData().getString(TRAITS).orElse("").contains("trade_gold")) {
            if (!serverPlayer.getAbilities().instabuild) held.shrink(1);
            ItemStack trade = switch (serverPlayer.getRandom().nextInt(5)) {
                case 0 -> new ItemStack(Items.QUARTZ, 2 + serverPlayer.getRandom().nextInt(4));
                case 1 -> new ItemStack(Items.BLACKSTONE, 2 + serverPlayer.getRandom().nextInt(4));
                case 2 -> new ItemStack(Items.STRING, 2 + serverPlayer.getRandom().nextInt(4));
                case 3 -> new ItemStack(Items.SOUL_SAND, 1 + serverPlayer.getRandom().nextInt(3));
                default -> new ItemStack(Items.IRON_NUGGET, 3 + serverPlayer.getRandom().nextInt(6));
            };
            homunculus.spawnAtLocation(level, trade);
            serverPlayer.sendSystemMessage(Component.literal("The homunculus barters for gold."));
            return true;
        }
        if (!isMarbas(player)) return false;
        if (held.is(RequiemItems.VESSEL_SHARD) && target instanceof Mob mob && isHomunculus(mob)) {
            long born = mob.getPersistentData().getLong(BORN_AT).orElse(level.getGameTime());
            int sleeps = player.getPersistentData().getInt("requiem:marbas_sleep_count").orElse(0);
            if (level.getGameTime() - born > 48000L && sleeps < 2) {
                player.sendSystemMessage(Component.literal("The homunculus soul has settled. It can be extracted during its first two days, or after two nights of sleep."));
                return true;
            }
            ItemStack fetus = new ItemStack(RequiemItems.EMBRYO);
            fetus.set(DataComponents.CUSTOM_NAME, Component.literal("Fetus"));
            CompoundTag data = mob.getPersistentData().copy();
            CustomData.update(DataComponents.CUSTOM_DATA, fetus, tag -> {
                tag.putString(BRAIN, data.getString(BRAIN).orElse("return home"));
                tag.putString(HOME, data.getString(HOME).orElse(
                    mob.blockPosition().getX() + " " + mob.blockPosition().getY() + " " + mob.blockPosition().getZ()));
                tag.putInt(NODE, Math.max(0, data.getInt(NODE).orElse(0)));
                tag.putString(TRAITS, data.getString(TRAITS).orElse(""));
                tag.putString(OWNER, data.getString(OWNER).orElse(serverPlayer.getUUID().toString()));
            });
            if (!serverPlayer.getInventory().add(fetus)) serverPlayer.drop(fetus, false);
            mob.discard();
            if (!serverPlayer.getAbilities().instabuild) held.shrink(1);
            serverPlayer.sendSystemMessage(Component.literal("The homunculus returns to a transferable fetus. Its soul/program is preserved."));
            return true;
        }
        if (held.is(RequiemItems.EMBRYO) && target instanceof Mob body) {
            if (body instanceof ImmortalHomunculusEntity) {
                serverPlayer.sendSystemMessage(Component.literal("The immortal homunculus cannot accept a transferable brain."));
                return true;
            }
            CompoundTag data = readItemData(held);
            CompoundTag bodyData = body.getPersistentData();
            bodyData.putString(BRAIN, data.getString(BRAIN).orElse("return home"));
            bodyData.putInt(NODE, Math.max(0, data.getInt(NODE).orElse(0)));
            if (isHomunculus(body)) {
                // A native homunculus keeps its own body and traits; only its program changes.
                bodyData.remove(TRANSFERRED_BRAIN);
            } else {
                bodyData.putBoolean(TRANSFERRED_BRAIN, true);
                bodyData.putString(HOME, data.getString(HOME).orElse(
                    body.blockPosition().getX() + " " + body.blockPosition().getY() + " " + body.blockPosition().getZ()));
                // A transplanted brain replaces behaviour, not the body's attributes or traits.
                bodyData.remove("requiem:injected_soul");
                bodyData.remove("requiem:soul_owner");
                activateTransferredBrain(body);
            }
            if (!serverPlayer.getAbilities().instabuild) held.shrink(1);
            playSoulTransferEffect(level, body, true);
            serverPlayer.sendSystemMessage(Component.literal("The homunculus brain program now controls " + body.getName().getString() + ". Its body attributes remain unchanged."));
            return true;
        }
        if (target instanceof Mob mob && isHomunculus(mob) && held.is(Items.BONE)) {
            // No vanilla wolf taming mechanic is applied.
            return true;
        }
        if (shift && target instanceof Mob mob && isHomunculus(mob)) {
            if (mob instanceof ImmortalHomunculusEntity) {
                serverPlayer.sendSystemMessage(Component.literal("The immortal homunculus cannot be harvested back into an embryo."));
                return true;
            }
            ItemStack body = new ItemStack(RequiemItems.HOMUNCULUS);
            body.set(DataComponents.CUSTOM_NAME, Component.literal("Homunculus"));
            CompoundTag data = mob.getPersistentData().copy();
            // saveWithoutId retains native entity data and PersistentData, but not the old runtime UUID.
            TagValueOutput snapshotOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            mob.saveWithoutId(snapshotOutput);
            CompoundTag fullEntitySnapshot = snapshotOutput.buildResult();
            CustomData.update(DataComponents.CUSTOM_DATA, body, tag -> {
                tag.putString(BRAIN, data.getString(BRAIN).orElse("return home"));
                tag.putString(TRAITS, data.getString(TRAITS).orElse(""));
                tag.putBoolean(BERSERK, data.getBoolean(BERSERK).orElse(false));
                tag.putString(STORED_ENTITY_TYPE, BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString());
                tag.put(STORED_ENTITY_NBT, fullEntitySnapshot.copy());
            });
            playSoulTransferEffect(level, mob, false);
            if (!serverPlayer.getInventory().add(body)) serverPlayer.drop(body, false);
            mob.discard();
            serverPlayer.sendSystemMessage(Component.literal("Homunculus collected. Place this body in a water cauldron to restore its saved brain, traits, attributes and other entity data."));
            return true;
        }
        return false;
    }

    public static void applyBrainProgram(ServerPlayer player, int entityId, String program) {
        Entity entity = player.level().getEntity(entityId);
        if (!(entity instanceof Mob mob) || !hasProgrammableBrain(mob)
                || mob instanceof ImmortalHomunculusEntity || mob.distanceTo(player) > 12.0) {
            player.sendSystemMessage(Component.literal("The homunculus or brain-carrying body is unavailable or cannot use a programmable brain."));
            return;
        }
        String safe = program == null ? "" : program.trim();
        if (safe.length() > 1024) safe = safe.substring(0, 1024);
        String normalized = safe.isBlank() ? "return home" : safe;
        CompoundTag mobData = mob.getPersistentData();
        String previousProgram = mobData.getString(BRAIN).orElse("return home");
        if (normalized.equals(previousProgram)) {
            player.sendSystemMessage(Component.literal("The brain program is unchanged; its current task progress is preserved."));
            return;
        }
        int previousNode = Math.max(0, mobData.getInt(NODE).orElse(0));
        int newNodeCount = Math.max(1, normalized.split(";").length);
        mobData.putString(BRAIN, normalized);
        mobData.putInt(NODE, Math.min(previousNode, newNodeCount - 1));
        int damage = Math.min(10, player.getPersistentData().getInt("requiem:marbas_brain_damage").orElse(0) + 1);
        player.getPersistentData().putInt("requiem:marbas_brain_damage", damage);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            ladysnake.requiem.common.entity.effect.RequiemStatusEffects.BRAIN_DAMAGE, 1200, damage - 1, false, false, false));
        player.sendSystemMessage(Component.literal("The homunculus brain/soul has been rewritten. You feel the process strain your own mind."));
    }

    /** Counts completed nights of actual sleep, rather than merely clicking a bed. */
    public static void onPlayerTick(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        // Older builds could store more than two nights; normalize that data on login/tick.
        int storedSleepCount = data.getInt("requiem:marbas_sleep_count").orElse(0);
        int normalizedSleepCount = Math.max(0, Math.min(2, storedSleepCount));
        if (normalizedSleepCount != storedSleepCount) {
            data.putInt("requiem:marbas_sleep_count", normalizedSleepCount);
        }
        long now = player.level().getGameTime();
        boolean wasSleeping = data.getBoolean("requiem:marbas_was_sleeping").orElse(false);
        if (player.isSleeping()) {
            if (!wasSleeping) {
                data.putBoolean("requiem:marbas_was_sleeping", true);
                data.putLong("requiem:marbas_sleep_started", now);
            }
        } else if (wasSleeping) {
            long started = data.getLong("requiem:marbas_sleep_started").orElse(now);
            data.remove("requiem:marbas_was_sleeping");
            data.remove("requiem:marbas_sleep_started");
            if (now - started >= 100L) {
                int previousCount = data.getInt("requiem:marbas_sleep_count").orElse(0);
                int count = Math.min(2, previousCount + 1);
                if (count > previousCount) {
                    data.putInt("requiem:marbas_sleep_count", count);
                    if (isMarbas(player)) {
                        player.sendSystemMessage(Component.literal("A completed night's sleep has settled your homunculus research (" + count + "/2 nights)."));
                    }
                }
            }
        }
    }

    public static void tick(ServerLevel level, BlockPos center) {
        long now = level.getGameTime();
        AABB activeArea = new AABB(center.getX() - 128.0, level.getMinY(), center.getZ() - 128.0,
            center.getX() + 128.0, level.getMaxY(), center.getZ() + 128.0);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, activeArea, MarbasAbilitySystem::hasProgrammableBrain)) {
            try {
                if (mob.getPersistentData().getLong("requiem:homunculus_last_tick").orElse(Long.MIN_VALUE) == now) continue;
                mob.getPersistentData().putLong("requiem:homunculus_last_tick", now);
                if (isHomunculus(mob)) {
                    int mentalLevel = mob.getPersistentData().getBoolean(BERSERK).orElse(false) ? 10 : 1;
                    mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        ladysnake.requiem.common.entity.effect.RequiemStatusEffects.HOMUNCULUS_MENTAL_HEALTH,
                        40, mentalLevel - 1, false, false, false));
                    applyInheritedTraits(level, mob, now);
                    if (mob.getPersistentData().getBoolean(BERSERK).orElse(false)) {
                        tickBerserk(level, mob, now);
                    } else {
                        runBrain(level, mob, now);
                    }
                } else {
                    // A transplanted brain runs the command program only; no homunculus traits or stats are copied.
                    runBrain(level, mob, now);
                }
            } catch (RuntimeException ignored) {
                // Bad user-authored node text must never crash the server tick.
            }
        }
    }

    private static boolean isHomunculus(Mob mob) {
        return mob.getPersistentData().getBoolean(HOMUNCULUS).orElse(false);
    }

    public static boolean hasTransferredBrain(Mob mob) {
        return mob.getPersistentData().getBoolean(TRANSFERRED_BRAIN).orElse(false);
    }

    private static boolean hasProgrammableBrain(Mob mob) {
        return isHomunculus(mob) || hasTransferredBrain(mob);
    }

    /** Disable the target body's ordinary and previously injected goals without changing attributes. */
    public static void activateTransferredBrain(Mob mob) {
        if (!hasTransferredBrain(mob)) return;
        mob.getPersistentData().remove("requiem:injected_soul");
        mob.getPersistentData().remove("requiem:soul_owner");
        mob.goalSelector.removeAllGoals(goal -> true);
        mob.targetSelector.removeAllGoals(goal -> true);
        mob.setTarget(null);
        mob.getNavigation().stop();
    }

    private static float bodyAttackDamage(Mob mob) {
        var attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        // Most passive bodies expose no attack-damage attribute; allow the AI to act using a
        // minimal fallback while never overwriting the body's attribute values.
        return attack == null ? 1.0F : (float) Math.max(0.0D, attack.getValue());
    }

    private static void applyInheritedTraits(ServerLevel level, Mob mob, long now) {
        String traits = mob.getPersistentData().getString(TRAITS).orElse("").toLowerCase(Locale.ROOT);
        if (traits.contains("fire_resistance")) mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 40, 0, false, false, false));
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || target == mob) return;
        if (traits.contains("fire")) target.igniteForSeconds(3.0F);
        if (traits.contains("wither")) target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 60, 0, false, false, true));
        if (traits.contains("explode") && mob.distanceToSqr(target) < 5.0 && now % 20 == 0
                && mob.getPersistentData().getLong("requiem:homunculus_trait_explosion_cooldown").orElse(0L) <= now) {
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(3.5), e -> e != mob && e.isAlive())) {
                victim.hurt(level.damageSources().explosion(mob, mob), 12.0F);
            }
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, mob.getX(), mob.getY() + 0.8, mob.getZ(), 1, 0, 0, 0, 0);
            mob.getPersistentData().putLong("requiem:homunculus_trait_explosion_cooldown", now + 200L);
        }
    }

    private static void runBrain(ServerLevel level, Mob mob, long now) {
        String program = mob.getPersistentData().getString(BRAIN).orElse("return home");
        String[] nodes = program.split(";");
        if (nodes.length == 0) return;
        int node = Math.max(0, Math.min(nodes.length - 1, mob.getPersistentData().getInt(NODE).orElse(0)));
        String rawCommand = nodes[node].trim();
        String command = rawCommand.toLowerCase(Locale.ROOT);
        if (command.isBlank()) { nextNode(mob, nodes.length); return; }
        if (command.startsWith("say:") || command.startsWith("say ")) {
            int separator = rawCommand.indexOf(':') >= 0 ? rawCommand.indexOf(':') : rawCommand.indexOf(' ');
            String message = separator >= 0 ? rawCommand.substring(separator + 1).trim() : "";
            if (message.isBlank()) {
                nextNode(mob, nodes.length);
                return;
            }
            CompoundTag data = mob.getPersistentData();
            if (now >= data.getLong(SAY_COOLDOWN_UNTIL).orElse(0L)) {
                Component speech = Component.literal("<" + mob.getName().getString() + "> " + message);
                for (ServerPlayer listener : level.players()) {
                    if (listener.distanceToSqr(mob) <= 48.0D * 48.0D) {
                        listener.sendSystemMessage(speech);
                    }
                }
                level.sendParticles(ParticleTypes.NOTE, mob.getX(), mob.getY() + mob.getBbHeight() + 0.12D,
                    mob.getZ(), 1, 0.18, 0.04, 0.18, 1.0D);
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.NEUTRAL, 0.35F, 1.3F);
                data.putLong(SAY_COOLDOWN_UNTIL, now + 100L);
            }
            nextNode(mob, nodes.length);
            return;
        }
        if (command.startsWith("goto ")) {
            int[] xyz = parseXYZ(command.substring(5));
            if (xyz == null) { nextNode(mob, nodes.length); return; }
            mob.getNavigation().moveTo(xyz[0] + 0.5, xyz[1], xyz[2] + 0.5, 1.0);
            if (mob.distanceToSqr(xyz[0] + 0.5, xyz[1], xyz[2] + 0.5) < 4.0) nextNode(mob, nodes.length);
            return;
        }
        if (command.equals("return home") || command.equals("home")) {
            int[] xyz = parseXYZ(mob.getPersistentData().getString(HOME).orElse("0 64 0"));
            if (xyz == null) { nextNode(mob, nodes.length); return; }
            mob.getNavigation().moveTo(xyz[0] + 0.5, xyz[1], xyz[2] + 0.5, 1.0);
            if (mob.distanceToSqr(xyz[0] + 0.5, xyz[1], xyz[2] + 0.5) < 4.0) nextNode(mob, nodes.length);
            return;
        }
        if (command.startsWith("find item")) {
            String filter = command.contains(":") ? command.substring(command.indexOf(':') + 1).trim() : "";
            ItemEntity item = level.getEntitiesOfClass(ItemEntity.class, mob.getBoundingBox().inflate(18.0), e -> !e.getItem().isEmpty()
                && (filter.isBlank() || BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).toString().equalsIgnoreCase(filter)
                    || e.getItem().getHoverName().getString().equalsIgnoreCase(filter))).stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(mob), b.distanceToSqr(mob))).orElse(null);
            if (item == null) { mob.getNavigation().stop(); return; }
            mob.getPersistentData().putInt(TARGET_ID, item.getId());
            mob.getNavigation().moveTo(item, 1.0);
            nextNode(mob, nodes.length);
            return;
        }
        if (command.equals("take item") || command.equals("take")
                || command.equals("pickup") || command.equals("pickup item")
                || command.startsWith("pickup:") || command.startsWith("pickup ")) {
            Entity targetItem = level.getEntity(mob.getPersistentData().getInt(TARGET_ID).orElse(-1));
            String filter = "";
            if (command.startsWith("pickup:")) filter = command.substring("pickup:".length()).trim();
            else if (command.startsWith("pickup ") && !command.equals("pickup item")) filter = command.substring("pickup ".length()).trim();
            final String itemFilter = filter;
            ItemEntity item = targetItem instanceof ItemEntity found ? found : null;
            if (item == null || item.getItem().isEmpty()
                    || (!itemFilter.isBlank() && !matchesItemFilter(item.getItem(), itemFilter))) {
                item = level.getEntitiesOfClass(ItemEntity.class, mob.getBoundingBox().inflate(18.0), e ->
                    !e.getItem().isEmpty() && (itemFilter.isBlank() || matchesItemFilter(e.getItem(), itemFilter)))
                    .stream().min((a, b) -> Double.compare(a.distanceToSqr(mob), b.distanceToSqr(mob))).orElse(null);
                if (item == null) { mob.getNavigation().stop(); nextNode(mob, nodes.length); return; }
                mob.getPersistentData().putInt(TARGET_ID, item.getId());
            }
            if (mob.distanceToSqr(item) >= 2.25) { mob.getNavigation().moveTo(item, 1.0); return; }
            ItemStack held = mob.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!held.isEmpty()) mob.spawnAtLocation(level, held.copy());
            mob.setItemSlot(EquipmentSlot.MAINHAND, item.getItem().copy());
            item.discard();
            mob.getNavigation().stop();
            nextNode(mob, nodes.length);
            return;
        }
        if (command.equals("drop item") || command.equals("drop")) {
            ItemStack held = mob.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!held.isEmpty()) {
                mob.spawnAtLocation(level, held.copy());
                mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
            nextNode(mob, nodes.length);
            return;
        }
        if (command.equals("store chest") || command.startsWith("store chest")) {
            ItemStack held = mob.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!held.isEmpty()) {
                BlockPos origin = mob.blockPosition();
                ItemStack remaining = held.copy();
                for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-3, -2, -3), origin.offset(3, 2, 3))) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof Container container && !remaining.isEmpty()) remaining = insertIntoContainer(container, remaining);
                    if (remaining.isEmpty()) break;
                }
                mob.setItemSlot(EquipmentSlot.MAINHAND, remaining);
                if (!remaining.isEmpty()) mob.getNavigation().stop();
            }
            nextNode(mob, nodes.length);
            return;
        }
        if (command.startsWith("find entity")) {
            String targetName = command.contains(":") ? command.substring(command.indexOf(':') + 1).trim()
                : (command.startsWith("find entity ") ? command.substring("find entity ".length()).trim() : "");
            LivingEntity target = level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(24.0), e -> e != mob && !e.isDeadOrDying()
                && (targetName.isBlank() || e.getName().getString().equalsIgnoreCase(targetName))).stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(mob), b.distanceToSqr(mob))).orElse(null);
            if (target == null) { mob.getNavigation().stop(); return; }
            mob.getPersistentData().putInt(TARGET_ID, target.getId());
            mob.getNavigation().moveTo(target, 1.0);
            nextNode(mob, nodes.length);
            return;
        }
        if (command.equals("kill entity") || command.equals("kill") || command.startsWith("kill:") || command.startsWith("kill ")) {
            Entity target = level.getEntity(mob.getPersistentData().getInt(TARGET_ID).orElse(-1));
            String filter = command.startsWith("kill:") ? command.substring("kill:".length()).trim()
                : (command.startsWith("kill ") && !command.equals("kill entity") ? command.substring("kill ".length()).trim() : "");
            LivingEntity living = target instanceof LivingEntity found ? found : null;
            if (living == null || !living.isAlive() || living == mob
                    || (!filter.isBlank() && !matchesEntityFilter(living, filter))) {
                living = level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(24.0), e ->
                    e != mob && e.isAlive() && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
                        && (filter.isBlank() || matchesEntityFilter(e, filter)))
                    .stream().min((a, b) -> Double.compare(a.distanceToSqr(mob), b.distanceToSqr(mob))).orElse(null);
                if (living == null) { mob.getNavigation().stop(); nextNode(mob, nodes.length); return; }
                mob.getPersistentData().putInt(TARGET_ID, living.getId());
            }
            mob.setTarget(living);
            mob.getNavigation().moveTo(living, 1.0);
            if (mob.distanceToSqr(living) < 3.0
                    && now >= mob.getPersistentData().getLong(ATTACK_COOLDOWN_UNTIL).orElse(0L)) {
                float damage = bodyAttackDamage(mob);
                if (damage > 0.0F) living.hurt(level.damageSources().mobAttack(mob), damage);
                mob.getPersistentData().putLong(ATTACK_COOLDOWN_UNTIL, now + 20L);
            }
            return;
        }
        if (command.equals("wait") || command.equals("guard")) { mob.getNavigation().stop(); return; }
        nextNode(mob, nodes.length);
    }

    private static boolean matchesItemFilter(ItemStack stack, String filter) {
        String normalized = filter.toLowerCase(Locale.ROOT);
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);
        return id.equals(normalized) || id.endsWith(":" + normalized)
            || stack.getHoverName().getString().equalsIgnoreCase(filter);
    }

    private static boolean matchesEntityFilter(LivingEntity entity, String filter) {
        String normalized = filter.toLowerCase(Locale.ROOT);
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().toLowerCase(Locale.ROOT);
        return id.equals(normalized) || id.endsWith(":" + normalized)
            || entity.getName().getString().equalsIgnoreCase(filter);
    }

    private static int[] parseXYZ(String text) {
        String[] p = text.trim().split("\\s+");
        if (p.length != 3) return null;
        try { return new int[] {Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])}; }
        catch (NumberFormatException ignored) { return null; }
    }

    /** Inserts a stack into the first empty container slots using the 26.2 Container API.
     * Any overflow remains in the homunculus hand instead of being silently discarded.
     */
    private static ItemStack insertIntoContainer(Container container, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack remaining = stack.copy();
        boolean changed = false;
        for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
            if (!container.canPlaceItem(slot, remaining)) continue;
            ItemStack existing = container.getItem(slot);
            if (existing.isEmpty()) {
                int movedCount = Math.min(remaining.getCount(), container.getMaxStackSize(remaining));
                ItemStack moved = remaining.copy();
                moved.setCount(movedCount);
                container.setItem(slot, moved);
                remaining.shrink(movedCount);
                changed = true;
            } else if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                int maximum = Math.min(container.getMaxStackSize(remaining), existing.getMaxStackSize());
                int capacity = maximum - existing.getCount();
                if (capacity > 0) {
                    int movedCount = Math.min(capacity, remaining.getCount());
                    existing.grow(movedCount);
                    remaining.shrink(movedCount);
                    changed = true;
                }
            }
        }
        if (changed) container.setChanged();
        return remaining;
    }

    private static void nextNode(Mob mob, int nodeCount) {
        int current = mob.getPersistentData().getInt(NODE).orElse(0);
        mob.getPersistentData().putInt(NODE, (current + 1) % Math.max(1, nodeCount));
    }

    private static void tickBerserk(ServerLevel level, Mob mob, long now) {
        mob.setInvulnerable(true);
        if (mob.isInLava()) {
            mob.getPersistentData().putLong(STUN_UNTIL, now + 2L);
            mob.setGlowingTag(true);
            mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 5, 5, false, false, false));
            Vec3 v = mob.getDeltaMovement();
            mob.setDeltaMovement(v.x + (mob.getRandom().nextDouble() - 0.5) * 0.15, v.y + (mob.getRandom().nextDouble() - 0.5) * 0.1, v.z + (mob.getRandom().nextDouble() - 0.5) * 0.15);
            return;
        }
        mob.setGlowingTag(false);
        if (now < mob.getPersistentData().getLong(STUN_UNTIL).orElse(0L)) {
            mob.getNavigation().stop(); mob.setTarget(null); return;
        }
        if (now < mob.getPersistentData().getLong(FED_UNTIL).orElse(0L)) {
            mob.getNavigation().stop(); mob.setTarget(null); return;
        }
        LivingEntity target = level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(28.0), e -> e != mob && e.isAlive()
            && !(e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator()))).stream()
            .min((a, b) -> Double.compare(a.distanceToSqr(mob), b.distanceToSqr(mob))).orElse(null);
        if (target == null) return;
        var speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(target instanceof Player p && p.isSprinting() ? 0.13 : 0.10);
        var attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) attack.setBaseValue(20.0);
        mob.setTarget(target);
        mob.getNavigation().moveTo(target, 1.0);
        if (mob.distanceToSqr(target) < 3.1 && now % 10 == 0) {
            target.hurt(level.damageSources().mobAttack(mob), 20.0F);
            mob.getPersistentData().putLong(FED_UNTIL, now + 1200L);
        }
    }

    public static boolean onBerserkDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ImmortalHomunculusEntity mob)) return false;
        // Cancel ordinary damage; lava briefly interrupts the immortal hunt.
        event.setCanceled(true);
        if (event.getSource().is(DamageTypes.LAVA)) {
            mob.getPersistentData().putLong(STUN_UNTIL, mob.level().getGameTime() + 100L);
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
        return true;
    }

    /** Player attacks alone cannot stun it; lava is the only stun source. */
    public static boolean onAttackEntity(Player attacker, Entity target) {
        return false;
    }

    private static boolean isMobHead(ItemStack stack) {
        return stack.is(Items.ZOMBIE_HEAD) || stack.is(Items.SKELETON_SKULL) || stack.is(Items.WITHER_SKELETON_SKULL)
            || stack.is(Items.CREEPER_HEAD) || stack.is(Items.PIGLIN_HEAD) || stack.is(Items.DRAGON_HEAD) || stack.is(Items.PLAYER_HEAD);
    }

    private static String traitsForHead(ItemStack stack) {
        if (stack.is(Items.CREEPER_HEAD)) return "explode; health";
        if (stack.is(Items.PIGLIN_HEAD)) return "trade_gold; health";
        if (stack.is(Items.WITHER_SKELETON_SKULL)) return "wither; health";
        if (stack.is(Items.SKELETON_SKULL)) return "ranged; health";
        if (stack.is(Items.ZOMBIE_HEAD)) return "health; fire_resistance";
        if (stack.is(Items.DRAGON_HEAD)) return "fire; health";
        return "humanlike; health";
    }
}

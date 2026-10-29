package ladysnake.requiem.common.locki;

import ladysnake.requiem.api.v1.event.requiem.SoulCaptureEvents;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.common.entity.PlayerShellEntity;
import ladysnake.requiem.common.entity.effect.AttritionStatusEffect;
import ladysnake.requiem.common.item.EmptySoulVesselItem;
import ladysnake.requiem.common.item.FilledSoulVesselItem;
import ladysnake.requiem.common.item.RequiemItems;
import ladysnake.requiem.core.entity.SoulHolderComponent;
import ladysnake.requiem.core.util.NbtCompat;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Server-authoritative implementation of Phenex's powers and ritual tools. */
public final class PhenexAbilitySystem {
    public static final String TAG = "phenex";
    private static final String ACTIVE_ABILITY = "requiem:phenex_active_ability";
    private static final String PENDING_TARGET = "requiem:phenex_pending_target";
    private static final String CORRUPTION = "requiem:phenex_corruption";
    private static final String SCROLL_RECORDING = "requiem:phenex_scroll_recording";
    private static final String SCROLL_SCRIPT = "requiem:scroll_script";
    private static final String LOCATOR_ACTIVE = "requiem:soul_locator_active";
    private static final String LOCATOR_TARGET = "requiem:soul_locator_target";
    private static final String LOCATOR_PENDING = "requiem:soul_locator_pending";
    private static final String COUGH_UNTIL = "requiem:phenex_cough_until";
    private PhenexAbilitySystem() {}

    public static boolean isPhenex(Player player) {
        return player != null && (player.getTags().contains(TAG) || player.getTags().contains("Phenex"));
    }

    public static boolean isAbilityActive(Player player, String ability) {
        if (player == null || player.level().isClientSide()) return false;
        long coughUntil = player.getPersistentData().getLong(COUGH_UNTIL);
        if (player.level().getGameTime() < coughUntil) return false;
        return ability.equals(player.getPersistentData().getString(ACTIVE_ABILITY));
    }

    public static boolean isCoughing(Player player) {
        return player != null && player.level().getGameTime() < player.getPersistentData().getLong(COUGH_UNTIL);
    }

    /** Power words and scroll recording share one parser so scrolls can replay the same phrases. */
    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String message = event.getRawText().trim();
        CompoundTag data = player.getPersistentData();

        if (isPhenex(player) && data.getBoolean(LOCATOR_PENDING)) {
            event.setCanceled(true);
            if (message.equalsIgnoreCase("cancel")) {
                data.remove(LOCATOR_PENDING);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Soul Locator target selection cancelled."));
                return;
            }
            Entity target = findEntity(player, message);
            if (target == null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("No loaded entity/player matched that name or UUID. Try again, or type `cancel`."));
                return;
            }
            data.putString(LOCATOR_TARGET, target.getUUID().toString());
            data.remove(LOCATOR_PENDING);
            data.putBoolean(LOCATOR_ACTIVE, true);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Soul Locator locked onto " + target.getName().getString() + ". It will guide you to their soul when it is present."));
            return;
        }
        if (isPhenex(player) && data.getBoolean(SCROLL_RECORDING)) {
            event.setCanceled(true);
            if (message.equalsIgnoreCase("end scroll") || message.equalsIgnoreCase("finish scroll")) {
                data.remove(SCROLL_RECORDING);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Scroll recording finished."));
            } else if (message.equalsIgnoreCase("cancel scroll")) {
                data.remove(SCROLL_RECORDING);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Scroll recording cancelled."));
            } else {
                ItemStack scroll = findScroll(player);
                if (scroll.isEmpty()) {
                    data.remove(SCROLL_RECORDING);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Recording stopped: hold your Phenex Scroll."));
                } else {
                    appendScrollLine(scroll, message);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Recorded line " + scrollLineCount(scroll) + "."));
                }
            }
            return;
        }
        if (!isPhenex(player)) return;

        // Accept target-bearing commands in one chat line as well as the two-step prompt.
        String lowerMessage = message.toLowerCase(Locale.ROOT);
        String directAction = null;
        String directTarget = null;
        if (lowerMessage.startsWith("fire! ")) {
            directAction = "fire";
            directTarget = message.substring("fire! ".length()).trim();
        } else if (lowerMessage.startsWith("seize ")) {
            directAction = "seize";
            directTarget = message.substring("seize ".length()).trim();
        }
        if (directAction != null) {
            event.setCanceled(true);
            if (directTarget.matches("-?\\d+\\s+-?\\d+\\s+-?\\d+")) {
                directTarget = "location " + directTarget;
            }
            if (directTarget.isBlank()) {
                beginTargetSelection(player, directAction);
            } else {
                resolvePendingTarget(player, directAction, directTarget);
            }
            return;
        }

        String pending = data.getString(PENDING_TARGET);
        if (!pending.isBlank()) {
            event.setCanceled(true);
            if (message.equalsIgnoreCase("cancel")) {
                data.remove(PENDING_TARGET);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Target selection cancelled."));
            } else {
                resolvePendingTarget(player, pending, message);
            }
            return;
        }

        String phrase = normalizePhrase(message);
        if (isCoughing(player) && isPowerPhrase(phrase)) {
            event.setCanceled(true);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7* coughs — Phenex's abilities are temporarily unavailable *"));
            return;
        }
        if (isPowerPhrase(phrase) && maybeAbilityCough(player)) {
            event.setCanceled(true);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7* Phenex coughs violently; the ability sputters out *"));
            return;
        }
        runPhrase(player, phrase, true);
    }

    private static String normalizePhrase(String message) {
        String phrase = message.trim().toLowerCase(Locale.ROOT);
        if (phrase.equals("fire")) return "fire!";
        if (phrase.equals("roubar")) return "roubar!";
        return phrase;
    }

    private static boolean isPowerPhrase(String phrase) {
        return phrase.equals("roubar!") || phrase.equals("corromper") || phrase.equals("fire!") || phrase.equals("seize")
            || phrase.equals("give me a scroll") || phrase.equals("give me a soul locator");
    }

    private static void runPhrase(ServerPlayer player, String phrase, boolean allowPending) {
        CompoundTag data = player.getPersistentData();
        switch (phrase) {
            case "roubar!" -> {
                String active = data.getString(ACTIVE_ABILITY);
                if ("roubar".equals(active)) {
                    data.remove(ACTIVE_ABILITY);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Soul stealing disabled."));
                } else {
                    data.putString(ACTIVE_ABILITY, "roubar");
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Soul stealing enabled. Hold right-click on a living target to steal its soul."));
                }
            }
            case "corromper" -> {
                String active = data.getString(ACTIVE_ABILITY);
                if ("corromper".equals(active)) {
                    data.remove(ACTIVE_ABILITY);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Corruption disabled."));
                } else if (active.isBlank() || "none".equals(active)) {
                    data.putString(ACTIVE_ABILITY, "corromper");
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Corruption enabled. Melee hits add one level of Attrition."));
                } else {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Disable your current ability before enabling corruption."));
                }
            }
            case "fire!" -> {
                if (allowPending) beginTargetSelection(player, "fire");
                else player.getPersistentData().putString(PENDING_TARGET, "fire");
            }
            case "seize" -> {
                if (allowPending) beginTargetSelection(player, "seize");
                else player.getPersistentData().putString(PENDING_TARGET, "seize");
            }
            case "give me a scroll" -> {
                if (!findItem(player, RequiemItems.PHENEX_SCROLL)) {
                    ItemStack scroll = new ItemStack(RequiemItems.PHENEX_SCROLL);
                    if (!player.getInventory().add(scroll)) player.drop(scroll, false);
                }
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Phenex Scroll granted. Shift-right-click it, enter one phrase per chat message, then type `end scroll`."));
            }
            case "give me a soul locator" -> {
                ItemStack locator = new ItemStack(RequiemItems.SOUL_LOCATOR);
                if (!player.getInventory().add(locator)) player.drop(locator, false);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Soul Locator granted."));
            }
            default -> { }
        }
    }

    private static void beginTargetSelection(ServerPlayer player, String action) {
        player.getPersistentData().putString(PENDING_TARGET, action);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            ("fire".equals(action) ? "Fire" : "Extinguish") + " target: send `entity <name-or-UUID>` or `location <x> <y> <z>`. Send `cancel` to abort."));
    }

    /** Returns true when an item right-click was consumed. */
    public static boolean onUseItem(ServerPlayer player, ItemStack stack, boolean shiftDown) {
        if (isPhenex(player) && isCoughing(player)
                && (stack.is(RequiemItems.PHENEX_SCROLL) || stack.is(RequiemItems.SOUL_LOCATOR))) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7* coughs — Phenex's abilities are temporarily unavailable *"));
            return true;
        }
        if (stack.is(RequiemItems.PHENEX_SCROLL) && isPhenex(player)) {
            if (shiftDown) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(SCROLL_SCRIPT));
                player.getPersistentData().putBoolean(SCROLL_RECORDING, true);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Recording scroll. Type one Phenex phrase per chat message; type `end scroll` when done."));
            } else {
                executeScroll(player, stack);
            }
            return true;
        }
        if (stack.is(RequiemItems.SOUL_LOCATOR) && isPhenex(player)) {
            CompoundTag data = player.getPersistentData();
            if (shiftDown || data.getString(LOCATOR_TARGET).isBlank()) {
                data.putBoolean(LOCATOR_PENDING, true);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Soul Locator target: type an entity/player name or UUID in chat. Type `cancel` to abort."));
                return true;
            }
            boolean active = data.getBoolean(LOCATOR_ACTIVE);
            data.putBoolean(LOCATOR_ACTIVE, !active);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(!active
                ? "Soul Locator activated. It is tracking your selected soul target; use it again to stop."
                : "Soul Locator deactivated."));
            return true;
        }
        return false;
    }

    public static boolean onUseBlock(ServerPlayer player, ItemStack stack, BlockPos pos, boolean shiftDown) {
        if (stack.is(RequiemItems.PHENEX_SCROLL) && isPhenex(player) && shiftDown) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(SCROLL_SCRIPT));
            player.getPersistentData().putBoolean(SCROLL_RECORDING, true);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Recording scroll. Type one Phenex phrase per chat message; type `end scroll` when done."));
            return true;
        }
        // The Corrupt Attrition Vessel paired with a repeater is the locator ritual.
        if (isPhenex(player) && shiftDown && stack.is(RequiemItems.ICHOR_VESSEL_ATTRITION)
                && player.level().getBlockState(pos).is(Blocks.REPEATER)) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            ItemStack locator = new ItemStack(RequiemItems.SOUL_LOCATOR);
            if (!player.getInventory().add(locator)) player.drop(locator, false);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("The repeater resonates. You receive a Soul Locator."));
            return true;
        }
        return false;
    }

    private static void executeScroll(ServerPlayer player, ItemStack scroll) {
        String script = scroll.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(SCROLL_SCRIPT);
        if (script.isBlank()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("This scroll has no recorded phrases. Shift-right-click to record it."));
            return;
        }
        String[] lines = script.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String normalized = normalizePhrase(line);
            if (isCoughing(player)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7* coughs — the remaining scroll commands cannot execute *"));
                break;
            }
            if (isPowerPhrase(normalized) && maybeAbilityCough(player)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7* Phenex coughs ash; the remaining scroll commands cannot execute *"));
                break;
            }
            if (normalized.equals("fire!") || normalized.equals("seize")) {
                if (i + 1 < lines.length && (lines[i + 1].trim().toLowerCase(Locale.ROOT).startsWith("entity ")
                        || lines[i + 1].trim().toLowerCase(Locale.ROOT).startsWith("location "))) {
                    resolvePendingTarget(player, normalized.equals("seize") ? "seize" : "fire", lines[++i].trim());
                } else {
                    runPhrase(player, normalized, false);
                }
            } else {
                runPhrase(player, normalized, false);
            }
        }
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Scroll executed: " + lines.length + " lines."));
    }

    private static void appendScrollLine(ItemStack scroll, String line) {
        CustomData.update(DataComponents.CUSTOM_DATA, scroll, tag -> {
            String old = tag.getString(SCROLL_SCRIPT);
            tag.putString(SCROLL_SCRIPT, old.isBlank() ? line : old + "\n" + line);
        });
    }

    private static int scrollLineCount(ItemStack scroll) {
        String script = scroll.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(SCROLL_SCRIPT);
        return script.isBlank() ? 0 : script.split("\\R").length;
    }

    private static ItemStack findScroll(Player player) {
        if (player.getMainHandItem().is(RequiemItems.PHENEX_SCROLL)) return player.getMainHandItem();
        if (player.getOffhandItem().is(RequiemItems.PHENEX_SCROLL)) return player.getOffhandItem();
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) if (stack.is(RequiemItems.PHENEX_SCROLL)) return stack;
        return ItemStack.EMPTY;
    }

    private static boolean findItem(Player player, net.minecraft.world.item.Item item) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) if (stack.is(item)) return true;
        return false;
    }

    /** Returns true when the right-click was consumed by Roubar. */
    public static boolean onEntityInteract(Player user, Entity target) {
        if (!(user instanceof ServerPlayer phenex) || user.level().isClientSide()
                || !isPhenex(user) || !isAbilityActive(user, "roubar")) return false;
        if (target == user) {
            phenex.sendSystemMessage(net.minecraft.network.chat.Component.literal("You cannot steal your own soul."));
            return true;
        }
        if (target instanceof PlayerShellEntity) {
            phenex.sendSystemMessage(net.minecraft.network.chat.Component.literal("That shell has no independently stealable living soul."));
            return true;
        }
        if (target instanceof ServerPlayer victim) {
            PossessionComponent possession = PossessionComponent.get(victim);
            boolean extracted = false;
            if (possession.isPossessionOngoing()) {
                possession.stopPossessing();
                extracted = RemnantComponent.get(victim).setVagrant(true);
            } else if (!RemnantComponent.isVagrant(victim)) {
                extracted = RemnantComponent.get(victim).splitPlayer(true).isPresent();
            }
            phenex.sendSystemMessage(net.minecraft.network.chat.Component.literal(extracted
                ? "You forcibly tore the soul from " + victim.getGameProfile().name() + "."
                : "That player's soul is already outside its body, or cannot be extracted."));
            return true;
        }
        if (target instanceof LivingEntity livingTarget) {
            Optional<SoulCaptureEvents.CaptureType> captureType = EmptySoulVesselItem.checkCapturePreconditions(phenex, livingTarget);
            if (captureType.isEmpty() || !(livingTarget instanceof Mob)) {
                phenex.sendSystemMessage(net.minecraft.network.chat.Component.literal("That target's soul cannot be stolen."));
                return true;
            }
            ItemStack vessel = FilledSoulVesselItem.forEntityType(livingTarget.getType());
            UUID recordUuid = captureType.get() == SoulCaptureEvents.CaptureType.NORMAL
                ? EmptySoulVesselItem.setupRecord(livingTarget).getUuid() : UUID.randomUUID();
            CustomData.update(DataComponents.CUSTOM_DATA, vessel, tag -> {
                CompoundTag fragment = tag.getCompound(FilledSoulVesselItem.SOUL_FRAGMENT_NBT).orElseGet(CompoundTag::new);
                NbtCompat.putUuid(fragment, "uuid", recordUuid);
                fragment.putString("type", BuiltInRegistries.ENTITY_TYPE.getKey(livingTarget.getType()).toString());
                tag.put(FilledSoulVesselItem.SOUL_FRAGMENT_NBT, fragment);
            });
            if (captureType.get() == SoulCaptureEvents.CaptureType.NORMAL) SoulHolderComponent.get(livingTarget).removeSoul();
            else livingTarget.hurt(livingTarget.damageSources().magic(), EmptySoulVesselItem.SOUL_AGGREGATE_STEALING_DAMAGE);
            if (!phenex.getInventory().add(vessel)) phenex.drop(vessel, false);
            phenex.sendSystemMessage(net.minecraft.network.chat.Component.literal("Soul stolen: " + livingTarget.getName().getString() + "."));
            return true;
        }
        phenex.sendSystemMessage(net.minecraft.network.chat.Component.literal("Roubar needs a living target."));
        return true;
    }

    /** Corromper's melee hit effect. */
    public static void onAttack(Player attacker, Entity target) {
        if (!(attacker instanceof ServerPlayer) || attacker.level().isClientSide()
                || !isPhenex(attacker) || !isAbilityActive(attacker, "corromper")) return;
        if (target instanceof LivingEntity livingTarget && target != attacker) AttritionStatusEffect.apply(livingTarget, 1);
    }

    private static void resolvePendingTarget(ServerPlayer player, String action, String input) {
        CompoundTag data = player.getPersistentData();
        boolean extinguish = "seize".equals(action);
        String lower = input.toLowerCase(Locale.ROOT);
        if (lower.startsWith("location ")) {
            String[] parts = input.trim().split("\\s+");
            if (parts.length != 4) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Use: location <x> <y> <z>."));
                return;
            }
            try {
                BlockPos pos = new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                applyAtLocation(player, pos, extinguish);
                data.remove(PENDING_TARGET);
            } catch (NumberFormatException invalid) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Coordinates must be whole numbers."));
            }
            return;
        }
        if (lower.startsWith("entity ")) {
            String selector = input.substring("entity ".length()).trim();
            Entity entity = findEntity(player, selector);
            if (entity == null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("No loaded entity or online player matched that name/UUID."));
                return;
            }
            if (extinguish) entity.clearFire(); else entity.igniteForSeconds(10.0F);
            data.remove(PENDING_TARGET);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal((extinguish ? "Extinguished " : "Set on fire: ") + entity.getName().getString() + "."));
            return;
        }
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Use `entity <name-or-UUID>` or `location <x> <y> <z>`."));
    }

    private static Entity findEntity(ServerPlayer player, String selector) {
        MinecraftServer server = player.level().getServer();
        if (server == null || selector.isBlank()) return null;
        try {
            UUID uuid = UUID.fromString(selector);
            for (ServerLevel level : server.getAllLevels()) {
                Entity byUuid = level.getEntity(uuid);
                if (byUuid != null) return byUuid;
            }
        } catch (IllegalArgumentException ignored) { }
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            if (online.getGameProfile().name().equalsIgnoreCase(selector)) return online;
        }
        AABB search = player.getBoundingBox().inflate(256.0);
        for (Entity entity : player.level().getEntitiesOfClass(Entity.class, search,
                candidate -> candidate.getName().getString().equalsIgnoreCase(selector))) return entity;
        return null;
    }

    private static void applyAtLocation(ServerPlayer player, BlockPos requested, boolean extinguish) {
        ServerLevel level = (ServerLevel) player.level();
        if (!level.getWorldBorder().isWithinBounds(requested.getX() + 0.5, requested.getZ() + 0.5)
                || requested.getY() < level.getMinBuildHeight() || requested.getY() >= level.getMaxBuildHeight()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("That location is outside the world limits."));
            return;
        }
        BlockPos pos = requested;
        if (!extinguish) {
            if (!level.getBlockState(pos).isAir()) pos = pos.above();
            if (pos.getY() >= level.getMaxBuildHeight() || !level.getBlockState(pos).isAir()
                    || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("There is no safe surface for fire at that location."));
                return;
            }
            level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
        } else {
            if (level.getBlockState(pos).is(Blocks.FIRE) || level.getBlockState(pos).is(Blocks.SOUL_FIRE)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal((extinguish ? "Fire cleared at " : "Fire ignited at ") + pos.getX() + " " + pos.getY() + " " + pos.getZ() + "."));
    }

    /** Toggle the tracker / run cough timers each server tick. */
    public static void tick(ServerPlayer player) {
        if (!isPhenex(player)) return;
        long time = player.level().getGameTime();
        int corruption = getCorruption(player);
        if (corruption > 0) {
            // Refresh the marker effect so White Vision can read the precise ash-lung level.
            player.addEffect(new MobEffectInstance(
                ladysnake.requiem.common.entity.effect.RequiemStatusEffects.ASH_LUNG,
                40, corruption - 1, false, false, false));
            if (!isCoughing(player)) {
                // Natural coughing is deliberately far less common than an ability-triggered cough.
                int naturalDenominator = Math.max(600, 6000 / corruption);
                if (player.getRandom().nextInt(naturalDenominator) == 0) cough(player);
            }
        } else {
            player.removeEffect(ladysnake.requiem.common.entity.effect.RequiemStatusEffects.ASH_LUNG);
        }
        if (player.getPersistentData().getBoolean(LOCATOR_ACTIVE) && time % 10 == 0) {
            guideToSoul(player);
        }
    }

    private static boolean maybeAbilityCough(ServerPlayer player) {
        int corruption = getCorruption(player);
        if (corruption <= 0) return false;
        int denominator = Math.max(2, (int) Math.round(25.0 - (corruption - 1) * (23.0 / 9.0)));
        if (player.getRandom().nextInt(denominator) == 0) {
            cough(player);
            return true;
        }
        return false;
    }

    private static void cough(ServerPlayer player) {
        int corruption = getCorruption(player);
        long until = player.level().getGameTime() + 30L;
        player.getPersistentData().putLong(COUGH_UNTIL, until);
        player.hurt(player.damageSources().magic(), 1.0F + corruption * 0.15F);
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, corruption >= 7 ? 1 : 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 70, 0, false, true, false));
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ASH, player.getX() - player.getLookAngle().x * 0.25, player.getY() + 1.48,
                player.getZ() - player.getLookAngle().z * 0.25, 7, 0.035, 0.035, 0.035, 0.01);
        }
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7* Phenex coughs ash; her powers falter *"));
    }

    private static void guideToSoul(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        String targetId = player.getPersistentData().getString(LOCATOR_TARGET);
        Entity target = null;
        try {
            UUID targetUuid = UUID.fromString(targetId);
            for (ServerLevel candidateLevel : server.getAllLevels()) {
                target = candidateLevel.getEntity(targetUuid);
                if (target != null) break;
            }
        } catch (IllegalArgumentException ignored) { }
        if (target == null) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Soul Locator: selected soul is not currently loaded. Shift-right-click to choose another target."));
            return;
        }
        if (target.level() != player.level()) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Soul Locator: " + target.getName().getString() + " is in another dimension (" + target.level().dimension().location() + ")."));
            return;
        }
        if (target instanceof ServerPlayer targetPlayer && !RemnantComponent.isIncorporeal(targetPlayer)
                && !LockiSpiritSystem.isAstralVisionActive(targetPlayer)) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Soul Locator: " + target.getName().getString() + " is currently embodied; their soul is not outside the body."));
            return;
        }
        Vec3 delta = target.position().subtract(player.position());
        double distance = Math.sqrt(delta.lengthSqr());
        double yaw = Math.toDegrees(Math.atan2(delta.z, delta.x)) - player.getYRot();
        yaw = ((yaw + 540.0) % 360.0) - 180.0;
        String direction = Math.abs(yaw) < 25 ? "ahead" : Math.abs(yaw) > 155 ? "behind" : yaw < 0 ? "to your left" : "to your right";
        player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Soul Locator: " + direction + " — " + target.getName().getString() + " (" + Math.round(distance) + " blocks)"));
    }

    /** Current Phenex corruption stacks from 0–10. */
    public static int getCorruption(Player player) {
        return Math.max(0, Math.min(10, player.getPersistentData().getInt(CORRUPTION)));
    }
    public static int addCorruption(Player player, int amount) {
        int next = Math.max(0, Math.min(10, getCorruption(player) + amount));
        player.getPersistentData().putInt(CORRUPTION, next);
        return next;
    }
}

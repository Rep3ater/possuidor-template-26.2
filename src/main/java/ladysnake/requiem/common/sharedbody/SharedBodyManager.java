package ladysnake.requiem.common.sharedbody;

import ladysnake.requiem.Requiem;
import ladysnake.requiem.api.v1.possession.Possessable;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.common.entity.effect.RequiemStatusEffects;
import ladysnake.requiem.core.entity.EntityAiToggle;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side logic of the Shared Body status effect.
 *
 * <p>A player who splashes a mob with the effect becomes that mob's <em>controller</em>. As long as the effect lasts
 * and the controller is in their body (i.e. not a vagrant soul), the mob copies the controller's inputs: look direction,
 * movement, jumping, sneaking, sprinting, attacking and using. Mobs also gain a few species-specific abilities.
 *
 * <p>The controller's identity is stored on the mob as a scoreboard tag, so it survives chunk unloads and restarts.
 */
public final class SharedBodyManager {
    private static final String TAG_PREFIX = "requiem_shared_body:";
    private static final ResourceLocation AI_INHIBITOR = Requiem.id("shared_body");

    private static final double REACH = 3.5;
    private static final int ATTACK_COOLDOWN = 12;
    private static final int USE_COOLDOWN = 40;
    private static final double FLY_ACCELERATION = 0.025;

    private static final Map<UUID, ControlledMob> CONTROLLED = new HashMap<>();
    private static final Map<UUID, HeldKeys> HELD_KEYS = new HashMap<>();
    private static final Map<UUID, List<Integer>> LAST_SYNCED = new HashMap<>();

    private SharedBodyManager() {}

    public static void init() {
        NeoForge.EVENT_BUS.register(SharedBodyManager.class);
    }

    public static void setHeldKeys(ServerPlayer player, boolean attack, boolean use) {
        HELD_KEYS.put(player.getUUID(), new HeldKeys(attack, use));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Events
    // ------------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) return;
        if (event.getEffectInstance().getEffect().value() != RequiemStatusEffects.SHARED_BODY.value()) return;

        if (event.getEffectSource() instanceof ServerPlayer thrower) {
            bind(mob, thrower.getUUID());
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) return;
        for (String tag : new ArrayList<>(mob.entityTags())) {
            if (!tag.startsWith(TAG_PREFIX)) continue;
            UUID controller = parseController(tag);
            if (controller != null && mob.hasEffect(RequiemStatusEffects.SHARED_BODY)) {
                CONTROLLED.put(mob.getUUID(), new ControlledMob(mob, controller));
            } else {
                mob.removeTag(tag);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        HELD_KEYS.remove(id);
        LAST_SYNCED.remove(id);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CONTROLLED.clear();
        HELD_KEYS.clear();
        LAST_SYNCED.clear();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (CONTROLLED.isEmpty() && LAST_SYNCED.isEmpty()) return;

        Map<UUID, List<Integer>> drivenByController = new HashMap<>();
        Iterator<ControlledMob> it = CONTROLLED.values().iterator();
        while (it.hasNext()) {
            ControlledMob entry = it.next();
            Mob mob = entry.mob;

            if (mob.isRemoved()) {
                // Dead or unloaded; if it was merely unloaded, the tag stays and EntityJoinLevelEvent picks it up again
                it.remove();
                continue;
            }
            if (!mob.isAlive() || !mob.hasEffect(RequiemStatusEffects.SHARED_BODY)) {
                release(entry);
                clearTags(mob);
                it.remove();
                continue;
            }

            ServerPlayer controller = event.getServer().getPlayerList().getPlayer(entry.controller);
            if (controller == null || !canControl(controller, mob)) {
                suspend(entry);
                continue;
            }

            drive(entry, controller);
            if (mob.level() == controller.level()) {
                drivenByController.computeIfAbsent(entry.controller, k -> new ArrayList<>()).add(mob.getId());
            }
        }

        syncClients(event, drivenByController);
    }

    private static void syncClients(ServerTickEvent.Post event, Map<UUID, List<Integer>> driven) {
        Iterator<Map.Entry<UUID, List<Integer>>> stale = LAST_SYNCED.entrySet().iterator();
        while (stale.hasNext()) {
            Map.Entry<UUID, List<Integer>> e = stale.next();
            if (!driven.containsKey(e.getKey())) {
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(e.getKey());
                if (player != null) SharedBodyNetworking.sendSync(player, List.of());
                stale.remove();
            }
        }
        for (Map.Entry<UUID, List<Integer>> e : driven.entrySet()) {
            List<Integer> ids = e.getValue();
            ids.sort(Integer::compare);
            if (!ids.equals(LAST_SYNCED.get(e.getKey()))) {
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(e.getKey());
                if (player != null) SharedBodyNetworking.sendSync(player, List.copyOf(ids));
                LAST_SYNCED.put(e.getKey(), ids);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Binding
    // ------------------------------------------------------------------------------------------------------------

    private static void bind(Mob mob, UUID controller) {
        if (mob.getUUID().equals(controller)) return;
        clearTags(mob);
        mob.addTag(TAG_PREFIX + controller);
        ControlledMob previous = CONTROLLED.put(mob.getUUID(), new ControlledMob(mob, controller));
        if (previous != null) release(previous);
    }

    private static void clearTags(Mob mob) {
        for (String tag : new ArrayList<>(mob.entityTags())) {
            if (tag.startsWith(TAG_PREFIX)) mob.removeTag(tag);
        }
    }

    private static UUID parseController(String tag) {
        try {
            return UUID.fromString(tag.substring(TAG_PREFIX.length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Whether the controller is currently able to puppet {@code mob}. Being a vagrant soul (out of body) suspends control.
     */
    private static boolean canControl(ServerPlayer controller, Mob mob) {
        if (!controller.isAlive() || controller.isSpectator() || RemnantComponent.isVagrant(controller)) return false;
        // A mob inhabited by a soul is driven by its possessor instead
        return !(mob instanceof Possessable possessable && possessable.isBeingPossessed());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Driving
    // ------------------------------------------------------------------------------------------------------------

    private static void drive(ControlledMob entry, ServerPlayer controller) {
        Mob mob = entry.mob;
        if (!(mob.level() instanceof ServerLevel level)) return;

        if (!entry.driving) {
            entry.driving = true;
            EntityAiToggle.get(mob).toggleAi(AI_INHIBITOR, true, false);
            mob.getNavigation().stop();
            mob.setTarget(null);
        }

        Input input = controller.getLastClientInput();
        HeldKeys keys = HELD_KEYS.getOrDefault(controller.getUUID(), HeldKeys.NONE);

        // Look
        mob.setYRot(controller.getYRot());
        mob.setXRot(controller.getXRot());
        mob.setYHeadRot(controller.getYHeadRot());
        mob.yBodyRot = controller.getYRot();

        // Sneak / sprint
        boolean creeper = mob instanceof Creeper;
        mob.setShiftKeyDown(input.shift());
        float forward = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
        float strafe = (input.left() ? 1 : 0) - (input.right() ? 1 : 0);
        mob.setSprinting(input.sprint() && forward > 0 && !input.shift());

        // Move & jump
        if (isFlyer(mob)) {
            flyerMovement(mob, input, forward, strafe);
        } else {
            if (forward != 0 || strafe != 0) {
                mob.getMoveControl().strafe(forward, strafe);
            }
            if (input.jump()) {
                mob.getJumpControl().jump();
            }
        }

        // Creepers detonate while their controller sneaks
        if (mob instanceof Creeper c) {
            c.setSwellDir(input.shift() ? 1 : -1);
        }

        // Attack / use
        if (entry.attackCooldown > 0) entry.attackCooldown--;
        if (entry.useCooldown > 0) entry.useCooldown--;

        if (keys.attack() && !creeper) {
            attack(entry, level, controller);
        } else {
            resetMining(entry, level);
        }
        if (keys.use()) {
            use(entry, level);
        }
    }

    private static boolean isFlyer(Mob mob) {
        return mob.getMoveControl() instanceof FlyingMoveControl
            || mob instanceof Ghast || mob instanceof Blaze || mob instanceof Vex
            || mob instanceof Phantom || mob instanceof Bat;
    }

    private static void flyerMovement(Mob mob, Input input, float forward, float strafe) {
        float vertical = (input.jump() ? 1 : 0) - (input.shift() ? 1 : 0);
        Vec3 motion = mob.getDeltaMovement();
        if (!mob.isNoGravity()) {
            // Cancel gravity so flying mobs can hover
            motion = motion.add(0, mob.getGravity(), 0);
        }
        if (forward != 0 || strafe != 0 || vertical != 0) {
            Vec3 look = mob.getLookAngle();
            float yawRad = mob.getYRot() * Mth.DEG_TO_RAD;
            Vec3 left = new Vec3(Mth.cos(yawRad), 0, Mth.sin(yawRad));
            Vec3 accel = look.scale(forward).add(left.scale(strafe)).add(0, vertical, 0);
            if (accel.lengthSqr() > 1.0E-4) {
                motion = motion.add(accel.normalize().scale(FLY_ACCELERATION));
            }
        }
        mob.setDeltaMovement(motion);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Attack
    // ------------------------------------------------------------------------------------------------------------

    private static void attack(ControlledMob entry, ServerLevel level, ServerPlayer controller) {
        Mob mob = entry.mob;
        Vec3 eye = mob.getEyePosition();
        Vec3 look = mob.getLookAngle();
        Vec3 end = eye.add(look.scale(REACH));

        BlockHitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mob));
        double blockDistSqr = blockHit.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : eye.distanceToSqr(blockHit.getLocation());

        AABB searchBox = mob.getBoundingBox().expandTowards(look.scale(REACH)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
            mob, eye, end, searchBox,
            e -> isValidTarget(entry, e, controller),
            REACH * REACH
        );

        if (entityHit != null && eye.distanceToSqr(entityHit.getLocation()) <= blockDistSqr) {
            resetMining(entry, level);
            if (entry.attackCooldown <= 0) {
                entry.attackCooldown = ATTACK_COOLDOWN;
                mob.swing(InteractionHand.MAIN_HAND);
                Entity target = entityHit.getEntity();
                if (mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                    mob.doHurtTarget(level, target);
                } else {
                    target.hurtServer(level, level.damageSources().mobAttack(mob), 1.0F);
                }
            }
        } else if (blockHit.getType() == HitResult.Type.BLOCK && canBreakBlocks(mob)) {
            mine(entry, level, controller, blockHit.getBlockPos());
        } else {
            resetMining(entry, level);
            if (entry.attackCooldown <= 0) {
                entry.attackCooldown = ATTACK_COOLDOWN;
                mob.swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    private static boolean isValidTarget(ControlledMob entry, Entity target, ServerPlayer controller) {
        if (target == entry.mob || target == controller || target.isSpectator() || !target.isPickable()) return false;
        // No friendly fire between mobs sharing the controller's body
        ControlledMob other = CONTROLLED.get(target.getUUID());
        return other == null || !other.controller.equals(entry.controller);
    }

    /**
     * Humanoid mobs are dexterous enough to dig, like a husk or zombie would with their hands.
     */
    private static boolean canBreakBlocks(Mob mob) {
        return mob instanceof Zombie || mob instanceof AbstractSkeleton || mob instanceof AbstractIllager
            || mob instanceof AbstractPiglin || mob instanceof Witch || mob instanceof Villager;
    }

    private static void mine(ControlledMob entry, ServerLevel level, ServerPlayer controller, BlockPos pos) {
        Mob mob = entry.mob;
        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        if (state.isAir() || hardness < 0 || !controller.mayInteract(level, pos) || !controller.getAbilities().mayBuild) {
            resetMining(entry, level);
            return;
        }

        if (!pos.equals(entry.miningPos)) {
            resetMining(entry, level);
            entry.miningPos = pos.immutable();
        }

        if (mob.tickCount % 6 == 0) {
            mob.swing(InteractionHand.MAIN_HAND);
        }

        // Bare-handed digging: 30 ticks per hardness point, or 100 if the block needs a proper tool
        float ticksNeeded = hardness * (state.requiresCorrectToolForDrops() ? 100.0F : 30.0F);
        entry.miningProgress += ticksNeeded <= 0 ? 1.0F : 1.0F / ticksNeeded;

        if (entry.miningProgress >= 1.0F) {
            level.destroyBlockProgress(mob.getId(), pos, -1);
            level.destroyBlock(pos, true, mob, 512);
            entry.miningPos = null;
            entry.miningProgress = 0;
            entry.miningStage = -1;
        } else {
            int stage = (int) (entry.miningProgress * 10.0F);
            if (stage != entry.miningStage) {
                entry.miningStage = stage;
                level.destroyBlockProgress(mob.getId(), pos, stage);
            }
        }
    }

    private static void resetMining(ControlledMob entry, ServerLevel level) {
        if (entry.miningPos != null) {
            level.destroyBlockProgress(entry.mob.getId(), entry.miningPos, -1);
        }
        entry.miningPos = null;
        entry.miningProgress = 0;
        entry.miningStage = -1;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Use (species-specific abilities)
    // ------------------------------------------------------------------------------------------------------------

    private static void use(ControlledMob entry, ServerLevel level) {
        Mob mob = entry.mob;
        if (entry.useCooldown > 0) return;

        Vec3 look = mob.getLookAngle();
        Vec3 eye = mob.getEyePosition();

        if (mob instanceof Blaze) {
            entry.useCooldown = 10;
            SmallFireball fireball = new SmallFireball(level, mob, look);
            fireball.setPos(eye.x + look.x * 1.2, eye.y - 0.2 + look.y * 1.2, eye.z + look.z * 1.2);
            level.addFreshEntity(fireball);
            mob.swing(InteractionHand.MAIN_HAND);
        } else if (mob instanceof Ghast) {
            entry.useCooldown = USE_COOLDOWN;
            LargeFireball fireball = new LargeFireball(level, mob, look, 1);
            fireball.setPos(eye.x + look.x * 2.5, eye.y + look.y * 2.5, eye.z + look.z * 2.5);
            level.addFreshEntity(fireball);
        } else if (mob instanceof AbstractSkeleton) {
            entry.useCooldown = 20;
            Arrow arrow = new Arrow(level, mob, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
            arrow.setPos(eye.x + look.x * 0.5, eye.y - 0.1 + look.y * 0.5, eye.z + look.z * 0.5);
            arrow.shoot(look.x, look.y, look.z, 1.8F, 1.0F);
            level.addFreshEntity(arrow);
            mob.swing(InteractionHand.MAIN_HAND);
        } else if (mob instanceof EnderMan) {
            entry.useCooldown = 30;
            double range = 32.0;
            BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
            Vec3 dest = hit.getType() == HitResult.Type.MISS ? hit.getLocation() : hit.getLocation().subtract(look.scale(0.5));
            mob.randomTeleport(dest.x, dest.y, dest.z, true);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Release
    // ------------------------------------------------------------------------------------------------------------

    /** Hands the mob back to its own AI without forgetting who its controller is. */
    private static void suspend(ControlledMob entry) {
        if (entry.driving) {
            release(entry);
        }
    }

    /** Undoes every temporary change made while driving. */
    private static void release(ControlledMob entry) {
        Mob mob = entry.mob;
        if (mob.level() instanceof ServerLevel level) {
            resetMining(entry, level);
        }
        if (entry.driving) {
            entry.driving = false;
            if (!mob.isRemoved()) {
                EntityAiToggle.get(mob).toggleAi(AI_INHIBITOR, false, false);
            }
        }
        if (mob instanceof Creeper c) {
            c.setSwellDir(-1);
        }
        mob.setShiftKeyDown(false);
        mob.setSprinting(false);
    }

    private record HeldKeys(boolean attack, boolean use) {
        static final HeldKeys NONE = new HeldKeys(false, false);
    }

    private static final class ControlledMob {
        final Mob mob;
        final UUID controller;
        boolean driving;
        int attackCooldown;
        int useCooldown;
        BlockPos miningPos;
        float miningProgress;
        int miningStage = -1;

        ControlledMob(Mob mob, UUID controller) {
            this.mob = mob;
            this.controller = controller;
        }
    }
}

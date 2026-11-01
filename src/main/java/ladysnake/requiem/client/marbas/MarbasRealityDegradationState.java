package ladysnake.requiem.client.marbas;

import ladysnake.requiem.client.RequiemClient;
import ladysnake.requiem.client.SpeechBubbleClientState;
import ladysnake.requiem.common.entity.effect.RequiemStatusEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-authoritative reality degradation system for Marbas.
 * Scales with Brain Damage (up to Level 20+), reproducing authentic vanilla glitches,
 * shifting entity models, underground void plunging, sudden night flashes, fake entities,
 * multilingual eerie speech bubbles (EN, PT, ES) with minor psychological effects,
 * wide Animalese pitch variation, and peripheral vignette flashes.
 */
public final class MarbasRealityDegradationState {

    public enum SpeechEffect {
        NONE,
        LOOK_BEHIND,
        WAKE_UP_SWEETHEART,
        DONT_BLINK,
        THE_FLOOR_IS_GONE,
        DO_YOU_HEAR_THEM,
        ITS_COLD_OUT_HERE,
        THEY_ARE_WATCHING,
        RUN,
        EVERYTHING_IS_BURNING,
        LET_ME_IN,
        DONT_LOOK_UP,
        BEHIND_THE_WALL
    }

    public record EerieSpeech(String en, String pt, String es, SpeechEffect effect) {
        public String getText(RandomSource random) {
            // 1 in 5 (20%) chance for Portuguese, 1 in 5 (20%) chance for Spanish, otherwise English (60%)
            int roll = random.nextInt(5);
            if (roll == 0) return pt;
            if (roll == 1) return es;
            return en;
        }
    }

    // 32 multilingual speeches with 12 distinct psychological/jumpscare effects
    private static final EerieSpeech[] SPEECHES = new EerieSpeech[] {
        // --- 12 Speeches with Minor Effects ---
        new EerieSpeech("look behind you", "olhe para trás", "mira detrás de ti", SpeechEffect.LOOK_BEHIND),
        new EerieSpeech("wake up sweetheart", "acorde, querido", "despierta, cariño", SpeechEffect.WAKE_UP_SWEETHEART),
        new EerieSpeech("don't blink", "não pisque", "no parpadees", SpeechEffect.DONT_BLINK),
        new EerieSpeech("the floor is gone", "o chão sumiu", "el suelo desapareció", SpeechEffect.THE_FLOOR_IS_GONE),
        new EerieSpeech("do you hear them?", "você consegue ouvi-los?", "¿puedes oírlos?", SpeechEffect.DO_YOU_HEAR_THEM),
        new EerieSpeech("it's cold out here", "está tão frio aqui fora", "hace mucho frío aquí afuera", SpeechEffect.ITS_COLD_OUT_HERE),
        new EerieSpeech("they are watching", "eles estão observando", "ellos están observando", SpeechEffect.THEY_ARE_WATCHING),
        new EerieSpeech("run", "corra", "corre", SpeechEffect.RUN),
        new EerieSpeech("everything is burning", "tudo está queimando", "todo se está quemando", SpeechEffect.EVERYTHING_IS_BURNING),
        new EerieSpeech("let me in", "deixe-me entrar", "déjame entrar", SpeechEffect.LET_ME_IN),
        new EerieSpeech("don't look up", "não olhe para cima", "no mires hacia arriba", SpeechEffect.DONT_LOOK_UP),
        new EerieSpeech("someone is behind the wall", "alguém está atrás da parede", "alguien está detrás de la pared", SpeechEffect.BEHIND_THE_WALL),

        // --- 20 Additional Atmospheric Whispers ---
        new EerieSpeech("you are barely alive", "você mal está vivo", "apenas estás vivo", SpeechEffect.NONE),
        new EerieSpeech("can you see us?", "você pode nos ver?", "¿puedes vernos?", SpeechEffect.NONE),
        new EerieSpeech("your mind is slipping", "sua mente está se esvaindo", "tu mente se está desvaneciendo", SpeechEffect.NONE),
        new EerieSpeech("wake up", "acorde", "despierta", SpeechEffect.NONE),
        new EerieSpeech("is this real?", "isso é real?", "¿es esto real?", SpeechEffect.NONE),
        new EerieSpeech("we are not here", "nós não estamos aqui", "no estamos aquí", SpeechEffect.NONE),
        new EerieSpeech("it's too late", "é tarde demais", "ya es demasiado tarde", SpeechEffect.NONE),
        new EerieSpeech("why are you still here?", "por que você ainda está aqui?", "¿por qué sigues aquí?", SpeechEffect.NONE),
        new EerieSpeech("you are losing yourself", "você está se perdendo", "te estás perdiendo a ti mismo", SpeechEffect.NONE),
        new EerieSpeech("this place isn't safe", "este lugar não é seguro", "este lugar no es seguro", SpeechEffect.NONE),
        new EerieSpeech("who is controlling you?", "quem está te controlando?", "¿quién te está controlando?", SpeechEffect.NONE),
        new EerieSpeech("it hurts to remember", "dói lembrar", "duele recordar", SpeechEffect.NONE),
        new EerieSpeech("they lied to you", "eles mentiram para você", "te mintieron", SpeechEffect.NONE),
        new EerieSpeech("open your eyes", "abra seus olhos", "abre tus ojos", SpeechEffect.NONE),
        new EerieSpeech("do not trust what you see", "não confie no que você vê", "no confíes en lo que ves", SpeechEffect.NONE),
        new EerieSpeech("are you the one who died?", "foi você quem morreu?", "¿fuiste tú quien murió?", SpeechEffect.NONE),
        new EerieSpeech("the shadows are moving", "as sombras estão se movendo", "las sombras se estão moviendo", SpeechEffect.NONE),
        new EerieSpeech("stop resisting", "pare de resistir", "deja de resistirte", SpeechEffect.NONE),
        new EerieSpeech("can you feel your hands?", "consegue sentir suas mãos?", "¿puedes sentir tus manos?", SpeechEffect.NONE),
        new EerieSpeech("it's getting dark", "está ficando escuro", "se está oscureciendo", SpeechEffect.NONE)
    };

    // Varying outline colors from dye themes (Red, Purple, Magenta, Cyan, Yellow, Pink, Light Blue, Lime, White, Orange, Gray)
    private static final int[] BUBBLE_COLORS = new int[] {
        0xFFC92828, // Red
        0xFF8932B8, // Purple
        0xFFC74EBD, // Magenta
        0xFF169C9C, // Cyan
        0xFFFED83D, // Yellow
        0xFFF38BAA, // Pink
        0xFF3AB3DA, // Light Blue
        0xFF80C71F, // Lime
        0xFFFFFFFF, // White
        0xFFF9801D, // Orange
        0xFF585F63  // Gray
    };

    // --- State collections ---
    private static final Set<Long> GLITCHED_CHUNKS = ConcurrentHashMap.newKeySet();
    private static int chunkGlitchTimer = 0;
    private static int exposedCaveCutoffY = Integer.MIN_VALUE;

    private static final Set<BlockPos> GHOST_BLOCKS = ConcurrentHashMap.newKeySet();
    private static final Map<BlockPos, Integer> GHOST_BLOCK_TIMERS = new ConcurrentHashMap<>();

    private static final Set<Integer> UNDERWATER_HIDDEN_ENTITIES = ConcurrentHashMap.newKeySet();
    private static final Map<Integer, Integer> UNDERWATER_TIMERS = new ConcurrentHashMap<>();

    private static final Set<Integer> TRANSLUCENT_GLITCH_ENTITIES = ConcurrentHashMap.newKeySet();
    private static final Map<Integer, Integer> TRANSLUCENT_TIMERS = new ConcurrentHashMap<>();

    private static final Map<Integer, Float> FLOATING_CREATURES = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> FLOATING_TIMERS = new ConcurrentHashMap<>();

    // Void falling hallucination & Underground camera plunge (rare event)
    private static int voidFallTimer = 0;
    private static int voidFallCooldown = 600;

    // Entity Model Shifting Glitch
    private static int modelShiftTimer = 0;
    private static Vec3 modelShiftOffset = Vec3.ZERO;

    // Sudden Night Flash Glitch (rare event)
    private static int nightFlashTimer = 0;
    private static int nightFlashCooldown = 500;
    private static long originalDayTime = -1L;

    // Fake Entities Glitch
    private static final Map<Integer, Integer> FAKE_ENTITIES = new ConcurrentHashMap<>();

    // Special Minor Effects State
    private static int behindCreeperId = 0;
    private static int behindCreeperTimeout = 0;
    private static int behindCreeperFuse = -1;

    private static int dontBlinkEndermanId = 0;
    private static int dontBlinkEndermanTimer = 0;

    private static int pendingHeartbeatTicks = 0;

    // Peripheral Vignette Flash (1 single tick, border only)
    private static int vignetteFlashTicks = 0;

    // Delayed / phantom audio
    private static int nextGlitchCooldown = 60;
    private static boolean delayNextFootstep = false;
    private static int delayedFootstepCooldown = 0;
    private static int stillTicks = 0;
    private static int whisperCooldown = 120; // Occasional whisper interval

    private record ScheduledSound(long playTick, SoundType soundType, BlockPos pos) {}
    private static final List<ScheduledSound> SCHEDULED_SOUNDS = new ArrayList<>();

    private MarbasRealityDegradationState() {}

    private static void markBlockDirty(Minecraft mc, BlockPos pos) {
        if (mc.levelRenderer != null) {
            mc.levelRenderer.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
        }
    }

    public static int getBrainDamageLevel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        MobEffectInstance effect = mc.player.getEffect(RequiemStatusEffects.BRAIN_DAMAGE);
        return effect != null ? effect.getAmplifier() + 1 : 0;
    }

    public static boolean isGlitching() {
        return getBrainDamageLevel() > 0;
    }

    public static int getImpactFrameTicks() {
        return vignetteFlashTicks;
    }

    public static void triggerImpactFrame() {
        // Ultra-fast single tick (1 frame) vignette edge flash
        vignetteFlashTicks = 1;
    }

    public static void renderImpactFrame(GuiGraphics graphics) {
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        // High-contrast stark dark vignette on the borders only: screen center stays completely visible
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 0.90F);
        graphics.blit(RequiemClient.SPECTRE_VIGNETTE, 0, 0, w, h, 0.0F, 0.0F, 256, 256, 256, 256);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
    }

    public static float getCameraFallOffset(float partialTick) {
        if (voidFallTimer <= 0) return 0.0F;
        float progress = (70 - voidFallTimer) + partialTick;
        if (voidFallTimer < 14) {
            return Math.max(0.0F, voidFallTimer * 2.2F);
        }
        return Math.min(36.0F, progress * progress * 0.045F);
    }

    public static boolean isModelShiftingActive() {
        return modelShiftTimer > 0;
    }

    public static Vec3 getModelShiftOffset() {
        return modelShiftOffset;
    }

    // --- Hook Queries ---

    public static boolean shouldHideSection(BlockPos origin) {
        int level = getBrainDamageLevel();
        if (level <= 0) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        // Void Fall Hallucination (MC-228420)
        if (voidFallTimer > 0) {
            double distSqr = origin.distToCenterSqr(mc.player.position());
            if (voidFallTimer < 25 && distSqr > 32 * 32) return false;
            if (voidFallTimer < 15 && distSqr > 16 * 16) return false;
            return distSqr <= 72 * 72;
        }

        // Invisible Chunks / Exposed Caves (MC-129)
        if (chunkGlitchTimer > 0 && !GLITCHED_CHUNKS.isEmpty()) {
            long key = ChunkPos.asLong(origin.getX() >> 4, origin.getZ() >> 4);
            if (GLITCHED_CHUNKS.contains(key)) {
                if (exposedCaveCutoffY != Integer.MIN_VALUE) {
                    return origin.getY() >= exposedCaveCutoffY;
                }
                return true;
            }
        }

        return false;
    }

    public static boolean shouldHideBlock(BlockPos pos) {
        return getBrainDamageLevel() > 0 && GHOST_BLOCKS.contains(pos);
    }

    public static boolean shouldHideEntity(Entity entity) {
        return getBrainDamageLevel() > 0 && UNDERWATER_HIDDEN_ENTITIES.contains(entity.getId());
    }

    public static boolean isTranslucentGlitch(Entity entity) {
        return getBrainDamageLevel() > 0 && TRANSLUCENT_GLITCH_ENTITIES.contains(entity.getId());
    }

    public static boolean onPlayerFootstep(Player player, BlockPos pos, BlockState state) {
        int level = getBrainDamageLevel();
        if (level <= 0) return false;

        if (delayNextFootstep) {
            delayNextFootstep = false;
            SoundType soundType = state.getSoundType();
            long playAt = (player.level() != null ? player.level().getGameTime() : 0) + 12 + player.getRandom().nextInt(14);
            SCHEDULED_SOUNDS.add(new ScheduledSound(playAt, soundType, pos.immutable()));
            return true;
        }
        return false;
    }

    // --- Main Tick ---

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            clearAll();
            return;
        }

        int level = getBrainDamageLevel();
        if (level <= 0) {
            clearAll();
            return;
        }

        long gameTime = mc.level.getGameTime();

        // 0. Vignette frame countdown (ultra-fast single tick)
        if (vignetteFlashTicks > 0) {
            vignetteFlashTicks--;
        }

        // Decrement rare effect cooldowns
        if (voidFallCooldown > 0) voidFallCooldown--;
        if (nightFlashCooldown > 0) nightFlashCooldown--;

        // Pending heartbeat for "wake up sweetheart"
        if (pendingHeartbeatTicks > 0) {
            if (--pendingHeartbeatTicks == 0) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WARDEN_HEARTBEAT, 1.4F, 0.65F));
                triggerImpactFrame();
            }
        }

        // 1. Process Void Fall
        if (voidFallTimer > 0) {
            voidFallTimer--;
            if (voidFallTimer % 18 == 0) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ELYTRA_FLYING, 0.45F, 0.55F));
            }
        }

        // 2. Process Entity Model Shifting
        if (modelShiftTimer > 0) {
            modelShiftTimer--;
        }

        // 3. Process Sudden Night Flash
        if (nightFlashTimer > 0) {
            nightFlashTimer--;
            boolean isNight = (nightFlashTimer / 5) % 2 == 0;
            mc.level.setDayTime(isNight ? 18000L : (originalDayTime >= 0 ? originalDayTime : 6000L));
            if (nightFlashTimer == 0 && originalDayTime >= 0) {
                mc.level.setDayTime(originalDayTime);
                originalDayTime = -1L;
            }
        }

        // 4. Process Fake Entities
        Iterator<Map.Entry<Integer, Integer>> fakeIt = FAKE_ENTITIES.entrySet().iterator();
        while (fakeIt.hasNext()) {
            Map.Entry<Integer, Integer> entry = fakeIt.next();
            int entityId = entry.getKey();
            int remaining = entry.getValue() - 1;
            Entity fake = mc.level.getEntity(entityId);
            if (remaining <= 0 || fake == null || (fake != null && fake.distanceToSqr(mc.player) < 2.5 * 2.5)) {
                fakeIt.remove();
                if (fake != null) {
                    fake.discard();
                    mc.level.removeEntity(entityId, Entity.RemovalReason.DISCARDED);
                }
            } else {
                entry.setValue(remaining);
                Vec3 toPlayer = mc.player.getEyePosition().subtract(fake.getEyePosition());
                double dist = Math.sqrt(toPlayer.x * toPlayer.x + toPlayer.z * toPlayer.z);
                float yaw = (float) (Mth.atan2(toPlayer.z, toPlayer.x) * (180.0F / (float) Math.PI)) - 90.0F;
                float pitch = (float) (-(Mth.atan2(toPlayer.y, dist) * (180.0F / (float) Math.PI)));
                fake.setYRot(yaw);
                fake.setXRot(pitch);
                if (fake instanceof LivingEntity living) {
                    living.yBodyRot = yaw;
                    living.yHeadRot = yaw;
                }
            }
        }

        // 5. Process "Look Behind You" Fake Creeper
        if (behindCreeperId != 0) {
            Entity cEntity = mc.level.getEntity(behindCreeperId);
            if (cEntity instanceof Creeper creeper && creeper.isAlive() && behindCreeperTimeout-- > 0) {
                Vec3 toPlayer = mc.player.getEyePosition().subtract(creeper.getEyePosition());
                float yaw = (float) (Mth.atan2(toPlayer.z, toPlayer.x) * (180.0F / (float) Math.PI)) - 90.0F;
                creeper.setYRot(yaw);
                creeper.yHeadRot = yaw;
                creeper.yBodyRot = yaw;

                Vec3 playerLook = mc.player.getLookAngle().normalize();
                Vec3 toCreeper = creeper.getEyePosition().subtract(mc.player.getEyePosition()).normalize();
                double dot = playerLook.dot(toCreeper);

                if (behindCreeperFuse < 0) {
                    // When player turns around to look at the creeper
                    if (dot > 0.40) {
                        behindCreeperFuse = 22; // 1.1s fuse
                        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.CREEPER_PRIMED, 1.0F, 0.9F));
                        creeper.setSwellDir(1);
                    }
                } else {
                    behindCreeperFuse--;
                    if (behindCreeperFuse <= 0) {
                        // Harmless client-side explosion jumpscare
                        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.0F));
                        mc.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, creeper.getX(), creeper.getY() + 0.5, creeper.getZ(), 0, 0, 0);
                        for (int p = 0; p < 16; p++) {
                            mc.level.addParticle(ParticleTypes.LARGE_SMOKE,
                                creeper.getX() + (mc.player.getRandom().nextDouble() - 0.5) * 1.5,
                                creeper.getY() + mc.player.getRandom().nextDouble() * 1.2,
                                creeper.getZ() + (mc.player.getRandom().nextDouble() - 0.5) * 1.5,
                                (mc.player.getRandom().nextDouble() - 0.5) * 0.1, 0.05, (mc.player.getRandom().nextDouble() - 0.5) * 0.1);
                        }
                        triggerImpactFrame();
                        creeper.discard();
                        mc.level.removeEntity(behindCreeperId, Entity.RemovalReason.DISCARDED);
                        behindCreeperId = 0;
                        behindCreeperFuse = -1;
                    }
                }
            } else {
                if (cEntity != null) {
                    cEntity.discard();
                    mc.level.removeEntity(behindCreeperId, Entity.RemovalReason.DISCARDED);
                }
                behindCreeperId = 0;
                behindCreeperFuse = -1;
            }
        }

        // 6. Process "Don't Blink" Enderman
        if (dontBlinkEndermanId != 0) {
            Entity eEntity = mc.level.getEntity(dontBlinkEndermanId);
            if (eEntity instanceof EnderMan enderman && enderman.isAlive() && dontBlinkEndermanTimer-- > 0) {
                Vec3 toPlayer = mc.player.getEyePosition().subtract(enderman.getEyePosition());
                float yaw = (float) (Mth.atan2(toPlayer.z, toPlayer.x) * (180.0F / (float) Math.PI)) - 90.0F;
                enderman.setYRot(yaw);
                enderman.yHeadRot = yaw;
                enderman.yBodyRot = yaw;

                Vec3 playerLook = mc.player.getLookAngle().normalize();
                Vec3 toEnderman = enderman.getEyePosition().subtract(mc.player.getEyePosition()).normalize();
                double dot = playerLook.dot(toEnderman);

                // Vanishes when player looks away
                if (dot < 0.35) {
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.75F));
                    for (int p = 0; p < 18; p++) {
                        mc.level.addParticle(ParticleTypes.PORTAL,
                            enderman.getX() + (mc.player.getRandom().nextDouble() - 0.5) * 1.5,
                            enderman.getY() + mc.player.getRandom().nextDouble() * 2.5,
                            enderman.getZ() + (mc.player.getRandom().nextDouble() - 0.5) * 1.5,
                            (mc.player.getRandom().nextDouble() - 0.5) * 0.2, (mc.player.getRandom().nextDouble() - 0.5) * 0.2, (mc.player.getRandom().nextDouble() - 0.5) * 0.2);
                    }
                    enderman.discard();
                    mc.level.removeEntity(dontBlinkEndermanId, Entity.RemovalReason.DISCARDED);
                    dontBlinkEndermanId = 0;
                }
            } else {
                if (eEntity != null) {
                    eEntity.discard();
                    mc.level.removeEntity(dontBlinkEndermanId, Entity.RemovalReason.DISCARDED);
                }
                dontBlinkEndermanId = 0;
            }
        }

        // 7. Level 15-20: Occasional Eerie Whispers with varying colors, Animal Crossing pitch & minor effects
        if (level >= 15) {
            if (--whisperCooldown <= 0) {
                whisperCooldown = 130 + mc.player.getRandom().nextInt(110); // Occasional (every 6.5 - 12 seconds)
                AABB search = mc.player.getBoundingBox().inflate(16.0);
                List<LivingEntity> nearby = mc.level.getEntitiesOfClass(LivingEntity.class, search, e -> e != mc.player);
                if (!nearby.isEmpty()) {
                    LivingEntity speaker = nearby.get(mc.player.getRandom().nextInt(nearby.size()));
                    triggerEerieSpeech(speaker.getId(), mc.player.getRandom());
                } else if (mc.player.getRandom().nextBoolean()) {
                    triggerFakeEntity(level, mc.player.getRandom());
                }
            }
        }

        // 8. Process Chunk Glitches
        if (chunkGlitchTimer > 0) {
            chunkGlitchTimer--;
            if (chunkGlitchTimer == 0) {
                GLITCHED_CHUNKS.clear();
                exposedCaveCutoffY = Integer.MIN_VALUE;
            }
        }

        // 9. Process Ghost Blocks
        Iterator<Map.Entry<BlockPos, Integer>> ghostIt = GHOST_BLOCK_TIMERS.entrySet().iterator();
        while (ghostIt.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = ghostIt.next();
            BlockPos pos = entry.getKey();
            int remaining = entry.getValue() - 1;

            boolean lookedAway = isLookingAway(mc.player, pos);
            if (remaining <= 0 || (lookedAway && remaining < 60)) {
                ghostIt.remove();
                GHOST_BLOCKS.remove(pos);
                markBlockDirty(mc, pos);
            } else {
                entry.setValue(remaining);
            }
        }

        // 10. Process Underwater Hidden Entities
        Iterator<Map.Entry<Integer, Integer>> waterIt = UNDERWATER_TIMERS.entrySet().iterator();
        while (waterIt.hasNext()) {
            Map.Entry<Integer, Integer> entry = waterIt.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                waterIt.remove();
                UNDERWATER_HIDDEN_ENTITIES.remove(entry.getKey());
            } else {
                entry.setValue(remaining);
            }
        }

        // 11. Process Translucent Entities
        Iterator<Map.Entry<Integer, Integer>> transIt = TRANSLUCENT_TIMERS.entrySet().iterator();
        while (transIt.hasNext()) {
            Map.Entry<Integer, Integer> entry = transIt.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                transIt.remove();
                TRANSLUCENT_GLITCH_ENTITIES.remove(entry.getKey());
            } else {
                entry.setValue(remaining);
            }
        }

        // 12. Process Flying Squids / Aquatic Creatures
        Iterator<Map.Entry<Integer, Integer>> floatIt = FLOATING_TIMERS.entrySet().iterator();
        while (floatIt.hasNext()) {
            Map.Entry<Integer, Integer> entry = floatIt.next();
            int entityId = entry.getKey();
            int remaining = entry.getValue() - 1;
            Entity entity = mc.level.getEntity(entityId);
            if (entity != null && remaining > 0) {
                entity.setPos(entity.getX(), entity.getY() + 0.08, entity.getZ());
                entity.setDeltaMovement(0, 0.08, 0);
                entity.setOnGround(false);
                entry.setValue(remaining);
            } else {
                floatIt.remove();
                Float originalY = FLOATING_CREATURES.remove(entityId);
                if (entity != null && originalY != null) {
                    entity.setPos(entity.getX(), originalY, entity.getZ());
                    entity.setDeltaMovement(Vec3.ZERO);
                }
            }
        }

        // 13. Process Scheduled Delayed Sounds
        Iterator<ScheduledSound> soundIt = SCHEDULED_SOUNDS.iterator();
        while (soundIt.hasNext()) {
            ScheduledSound s = soundIt.next();
            if (gameTime >= s.playTick()) {
                soundIt.remove();
                mc.level.playSound(mc.player, s.pos(), s.soundType().getStepSound(),
                    SoundSource.PLAYERS, s.soundType().getVolume() * 0.35F, s.soundType().getPitch());
            }
        }

        // 14. Phantom footsteps while stationary
        if (mc.player.getDeltaMovement().lengthSqr() < 0.001) {
            stillTicks++;
            int footstepFrequency = Math.max(10, 45 - level * 2);
            if (stillTicks > 20 && stillTicks % footstepFrequency == 0 && mc.player.getRandom().nextInt(Math.max(2, 6 - level / 4)) == 0) {
                BlockPos under = mc.player.blockPosition().below();
                BlockState state = mc.level.getBlockState(under);
                SoundType type = state.getSoundType();
                mc.level.playSound(mc.player, mc.player.getX() - 0.6, mc.player.getY(), mc.player.getZ() - 0.6,
                    type.getStepSound(), SoundSource.PLAYERS, type.getVolume() * 0.3F, type.getPitch() * 0.9F);
            }
        } else {
            stillTicks = 0;
        }

        // 15. Glitch Scheduler: scales with Brain Damage level (1 to 20+)
        if (--nextGlitchCooldown <= 0) {
            int count = 1;
            if (level >= 19) count = 4;
            else if (level >= 13) count = 3;
            else if (level >= 8) count = 2;

            for (int i = 0; i < count; i++) {
                triggerRandomGlitch(level);
            }

            int minCooldown = Math.max(8, 200 - level * 10);
            int spread = Math.max(6, 100 - level * 5);
            nextGlitchCooldown = minCooldown + mc.player.getRandom().nextInt(spread);
        }
    }

    public static void triggerEerieSpeech(int speakerEntityId, RandomSource random) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        EerieSpeech speech = SPEECHES[random.nextInt(SPEECHES.length)];
        String text = speech.getText(random);
        int color = BUBBLE_COLORS[random.nextInt(BUBBLE_COLORS.length)];

        // Wide Animalese pitch variation: from ultra-low demonic drone (0.08F) to uncanny high screech (1.75F)
        float pitch = 0.08F + random.nextFloat() * 1.67F;

        SpeechBubbleClientState.show(speakerEntityId, text, color, pitch);
        executeSpeechEffect(speech.effect(), random);
    }

    private static void executeSpeechEffect(SpeechEffect effect, RandomSource random) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        switch (effect) {
            case LOOK_BEHIND -> {
                // Spawns a silent fake Creeper behind the player that hisses and explodes when the player looks at it
                Vec3 look = mc.player.getLookAngle();
                Vec3 flatLook = new Vec3(look.x, 0, look.z).normalize();
                double bx = mc.player.getX() - flatLook.x * 3.2;
                double by = mc.player.getY();
                double bz = mc.player.getZ() - flatLook.z * 3.2;

                Creeper creeper = EntityType.CREEPER.create(mc.level);
                if (creeper != null) {
                    if (behindCreeperId != 0) {
                        Entity old = mc.level.getEntity(behindCreeperId);
                        if (old != null) old.discard();
                        mc.level.removeEntity(behindCreeperId, Entity.RemovalReason.DISCARDED);
                    }
                    int fakeId = -9990 - random.nextInt(100);
                    creeper.setId(fakeId);
                    creeper.setPos(bx, by, bz);
                    creeper.setYRot(mc.player.getYRot());
                    creeper.yHeadRot = creeper.getYRot();
                    creeper.yBodyRot = creeper.getYRot();
                    creeper.setNoAi(true);
                    creeper.setSilent(true);
                    mc.level.addEntity(creeper);

                    behindCreeperId = fakeId;
                    behindCreeperTimeout = 160;
                    behindCreeperFuse = -1;
                }
            }
            case WAKE_UP_SWEETHEART -> {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WARDEN_HEARTBEAT, 1.4F, 0.7F));
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_BREATH, 1.1F, 0.8F));
                pendingHeartbeatTicks = 12;
                triggerImpactFrame();
            }
            case DONT_BLINK -> {
                Vec3 front = mc.player.position().add(mc.player.getLookAngle().scale(3.8));
                EnderMan enderman = EntityType.ENDERMAN.create(mc.level);
                if (enderman != null) {
                    if (dontBlinkEndermanId != 0) {
                        Entity old = mc.level.getEntity(dontBlinkEndermanId);
                        if (old != null) old.discard();
                        mc.level.removeEntity(dontBlinkEndermanId, Entity.RemovalReason.DISCARDED);
                    }
                    int fakeId = -9980 - random.nextInt(100);
                    enderman.setId(fakeId);
                    enderman.setPos(front.x, mc.player.getY(), front.z);
                    enderman.setYRot(mc.player.getYRot() + 180.0F);
                    enderman.yHeadRot = enderman.getYRot();
                    enderman.yBodyRot = enderman.getYRot();
                    enderman.setNoAi(true);
                    mc.level.addEntity(enderman);

                    dontBlinkEndermanId = fakeId;
                    dontBlinkEndermanTimer = 120;
                }
            }
            case THE_FLOOR_IS_GONE -> {
                BlockPos under = mc.player.blockPosition().below();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos p = under.offset(dx, 0, dz);
                        if (!mc.level.getBlockState(p).isAir()) {
                            GHOST_BLOCKS.add(p);
                            GHOST_BLOCK_TIMERS.put(p, 65 + random.nextInt(20));
                            markBlockDirty(mc, p);
                        }
                    }
                }
                triggerImpactFrame();
            }
            case DO_YOU_HEAR_THEM -> {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMBIENT_CAVE.value(), 1.0F, 0.55F));
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.65F, 0.4F));
            }
            case ITS_COLD_OUT_HERE -> {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_HURT_FREEZE, 1.0F, 0.8F));
                for (int i = 0; i < 20; i++) {
                    double ox = (random.nextDouble() - 0.5) * 2.0;
                    double oy = random.nextDouble() * 2.0;
                    double oz = (random.nextDouble() - 0.5) * 2.0;
                    mc.level.addParticle(ParticleTypes.SNOWFLAKE, mc.player.getX() + ox, mc.player.getY() + oy, mc.player.getZ() + oz, 0, -0.02, 0);
                }
            }
            case THEY_ARE_WATCHING -> {
                for (int i = 0; i < 3; i++) {
                    double angle = Math.toRadians(mc.player.getYRot() + i * 120.0);
                    double dist = 4.8;
                    double x = mc.player.getX() - Math.sin(angle) * dist;
                    double z = mc.player.getZ() + Math.cos(angle) * dist;
                    double y = mc.player.getY();

                    Mob watcher = EntityType.SKELETON.create(mc.level);
                    if (watcher != null) {
                        int fakeId = -3500 - random.nextInt(5000);
                        watcher.setId(fakeId);
                        watcher.setPos(x, y, z);
                        watcher.setNoAi(true);
                        watcher.setSilent(true);
                        mc.level.addEntity(watcher);
                        FAKE_ENTITIES.put(fakeId, 70);
                    }
                }
                triggerImpactFrame();
            }
            case RUN -> {
                Vec3 flatLook = new Vec3(mc.player.getLookAngle().x, 0, mc.player.getLookAngle().z).normalize();
                BlockPos playerBase = mc.player.blockPosition().below();
                BlockState underState = mc.level.getBlockState(playerBase);
                SoundType soundType = underState.isAir() ? SoundType.STONE : underState.getSoundType();
                long now = mc.level.getGameTime();
                for (int step = 0; step < 4; step++) {
                    double distBehind = 6.0 - step * 1.4;
                    BlockPos stepPos = mc.player.blockPosition().offset(
                        (int) (-flatLook.x * distBehind),
                        -1,
                        (int) (-flatLook.z * distBehind)
                    );
                    SCHEDULED_SOUNDS.add(new ScheduledSound(now + 4 + step * 4, soundType, stepPos));
                }
            }
            case EVERYTHING_IS_BURNING -> {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.FIRE_AMBIENT, 1.2F, 0.9F));
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.GENERIC_EXTINGUISH_FIRE, 0.7F, 1.2F));
                for (int i = 0; i < 28; i++) {
                    double ox = (random.nextDouble() - 0.5) * 2.5;
                    double oy = random.nextDouble() * 2.0;
                    double oz = (random.nextDouble() - 0.5) * 2.5;
                    mc.level.addParticle(ParticleTypes.FLAME, mc.player.getX() + ox, mc.player.getY() + oy, mc.player.getZ() + oz, 0, 0.04, 0);
                    mc.level.addParticle(ParticleTypes.SMOKE, mc.player.getX() + ox, mc.player.getY() + oy, mc.player.getZ() + oz, 0, 0.02, 0);
                }
                triggerImpactFrame();
            }
            case LET_ME_IN -> {
                Vec3 look = mc.player.getLookAngle();
                Vec3 earPos = mc.player.getEyePosition().add(-look.z * 0.8, 0, look.x * 0.8);
                mc.level.playLocalSound(earPos.x, earPos.y, earPos.z, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 1.4F, 0.85F, false);
                mc.level.playLocalSound(earPos.x, earPos.y, earPos.z, SoundEvents.WOOD_BREAK, SoundSource.HOSTILE, 0.8F, 0.9F, false);
                triggerImpactFrame();
            }
            case DONT_LOOK_UP -> {
                Vec3 above = mc.player.getEyePosition().add(0, 9.0, 0);
                mc.level.playLocalSound(above.x, above.y, above.z, SoundEvents.PHANTOM_SWOOP, SoundSource.HOSTILE, 1.3F, 0.7F, false);
            }
            case BEHIND_THE_WALL -> {
                Vec3 front = mc.player.getEyePosition().add(mc.player.getLookAngle().scale(1.8));
                mc.level.playLocalSound(front.x, front.y, front.z, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.HOSTILE, 1.1F, 0.6F, false);
            }
            case NONE -> {
                // Subtle ambient breath or presence
                if (random.nextInt(3) == 0) {
                    triggerImpactFrame();
                }
            }
        }
    }

    private static void triggerRandomGlitch(int level) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        RandomSource random = mc.player.getRandom();

        List<Integer> availableBugs = new ArrayList<>();
        // Bug 1: Delayed footstep / audio
        availableBugs.add(1);
        // Bug 2: Ghost block
        availableBugs.add(2);

        if (level >= 2) {
            // Bug 3: Chunks disappear / exposed caves
            availableBugs.add(3);
            // Bug 4: Underwater mob invisible
            availableBugs.add(4);
        }

        if (level >= 4) {
            // Bug 5: Flying squids / creatures
            availableBugs.add(5);
            // Bug 6: Transparency ordering glitch
            availableBugs.add(6);
            // Bug 7: Fake entity apparition
            availableBugs.add(7);
        }

        if (level >= 6) {
            // Bug 8: Entity model shifting (including player in F5)
            availableBugs.add(8);
        }

        // Day/Night and Void Fall are rare climatic events
        if (level >= 8 && nightFlashCooldown <= 0 && random.nextInt(12) == 0) {
            availableBugs.add(9);
        }

        if (level >= 10 && voidFallCooldown <= 0 && random.nextInt(16) == 0) {
            availableBugs.add(10);
        }

        int choice = availableBugs.get(random.nextInt(availableBugs.size()));

        switch (choice) {
            case 1 -> triggerDelayedSound(level);
            case 2 -> triggerGhostBlock(level);
            case 3 -> triggerChunkGlitch(level);
            case 4 -> triggerUnderwaterGlitch(level);
            case 5 -> triggerFlyingCreature(level);
            case 6 -> triggerTranslucencyGlitch(level);
            case 7 -> triggerFakeEntity(level, random);
            case 8 -> triggerModelShift(level, random);
            case 9 -> triggerNightFlash(level, random);
            case 10 -> triggerVoidFall(level);
        }
    }

    private static void triggerDelayedSound(int level) {
        delayNextFootstep = true;
        delayedFootstepCooldown = 60;
        triggerImpactFrame();
    }

    private static void triggerGhostBlock(int level) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        BlockPos playerPos = mc.player.blockPosition();
        int range = 2 + mc.player.getRandom().nextInt(5);
        int blocksToGlitch = level >= 10 ? 1 + mc.player.getRandom().nextInt(3) : 1;

        for (int b = 0; b < blocksToGlitch; b++) {
            for (int i = 0; i < 15; i++) {
                BlockPos candidate = playerPos.offset(
                    mc.player.getRandom().nextInt(range * 2 + 1) - range,
                    mc.player.getRandom().nextInt(3) - 1,
                    mc.player.getRandom().nextInt(range * 2 + 1) - range
                );
                BlockState state = mc.level.getBlockState(candidate);
                if (!state.isAir()) {
                    GHOST_BLOCKS.add(candidate);
                    GHOST_BLOCK_TIMERS.put(candidate, 80 + mc.player.getRandom().nextInt(60));
                    markBlockDirty(mc, candidate);
                    break;
                }
            }
        }
        triggerImpactFrame();
    }

    private static void triggerChunkGlitch(int level) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int playerChunkX = mc.player.chunkPosition().x;
        int playerChunkZ = mc.player.chunkPosition().z;

        GLITCHED_CHUNKS.clear();
        int dx = mc.player.getRandom().nextInt(3) - 1;
        int dz = mc.player.getRandom().nextInt(3) - 1;
        if (dx == 0 && dz == 0) dx = 1;

        GLITCHED_CHUNKS.add(ChunkPos.asLong(playerChunkX + dx, playerChunkZ + dz));
        if (level >= 6) {
            GLITCHED_CHUNKS.add(ChunkPos.asLong(playerChunkX + dx * 2, playerChunkZ + dz * 2));
        }
        if (level >= 12) {
            GLITCHED_CHUNKS.add(ChunkPos.asLong(playerChunkX - dz, playerChunkZ + dx));
        }

        if (mc.player.getRandom().nextBoolean()) {
            exposedCaveCutoffY = mc.player.getBlockY() - 3;
        } else {
            exposedCaveCutoffY = Integer.MIN_VALUE;
        }

        chunkGlitchTimer = 60 + mc.player.getRandom().nextInt(50);
        triggerImpactFrame();
    }

    private static void triggerUnderwaterGlitch(int level) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        AABB search = mc.player.getBoundingBox().inflate(32.0);
        List<LivingEntity> aquatic = mc.level.getEntitiesOfClass(LivingEntity.class, search,
            e -> e.isInWater() || e instanceof WaterAnimal);

        if (!aquatic.isEmpty()) {
            LivingEntity target = aquatic.get(mc.player.getRandom().nextInt(aquatic.size()));
            UNDERWATER_HIDDEN_ENTITIES.add(target.getId());
            UNDERWATER_TIMERS.put(target.getId(), 70 + mc.player.getRandom().nextInt(50));
            triggerImpactFrame();
        }
    }

    private static void triggerFlyingCreature(int level) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        AABB search = mc.player.getBoundingBox().inflate(36.0);
        List<LivingEntity> candidates = mc.level.getEntitiesOfClass(LivingEntity.class, search,
            e -> e instanceof Squid || e instanceof WaterAnimal || e.isInWater());

        if (!candidates.isEmpty()) {
            LivingEntity creature = candidates.get(mc.player.getRandom().nextInt(candidates.size()));
            FLOATING_CREATURES.put(creature.getId(), (float) creature.getY());
            FLOATING_TIMERS.put(creature.getId(), 75 + mc.player.getRandom().nextInt(45));
            triggerImpactFrame();
        }
    }

    private static void triggerTranslucencyGlitch(int level) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        AABB search = mc.player.getBoundingBox().inflate(24.0);
        List<LivingEntity> mobs = mc.level.getEntitiesOfClass(LivingEntity.class, search, e -> e != mc.player);

        if (!mobs.isEmpty()) {
            LivingEntity target = mobs.get(mc.player.getRandom().nextInt(mobs.size()));
            TRANSLUCENT_GLITCH_ENTITIES.add(target.getId());
            TRANSLUCENT_TIMERS.put(target.getId(), 80 + mc.player.getRandom().nextInt(60));
            triggerImpactFrame();
        }
    }

    private static void triggerFakeEntity(int level, RandomSource random) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        EntityType<?>[] candidates = new EntityType<?>[] {
            EntityType.ZOMBIE, EntityType.PIG, EntityType.SKELETON, EntityType.CREEPER, EntityType.VILLAGER
        };
        EntityType<?> chosen = candidates[random.nextInt(candidates.length)];

        Entity fake = chosen.create(mc.level);
        if (fake instanceof Mob mob) {
            double angle = Math.toRadians(mc.player.getYRot() + (random.nextBoolean() ? 35 : -35) + (random.nextDouble() - 0.5) * 20);
            double dist = 4.5 + random.nextDouble() * 3.5;
            double x = mc.player.getX() - Math.sin(angle) * dist;
            double z = mc.player.getZ() + Math.cos(angle) * dist;
            double y = mc.player.getY();

            int fakeId = -3000 - random.nextInt(10000);
            mob.setId(fakeId);
            mob.setPos(x, y, z);
            mob.setYRot(mc.player.getYRot() + 180.0F);
            mob.setXRot(0.0F);
            mob.yHeadRot = mob.getYRot();
            mob.setNoAi(true);

            mc.level.addEntity(mob);
            FAKE_ENTITIES.put(fakeId, 65 + random.nextInt(40));

            // On level 15-20, fake entities occasionally speak with multilingual speeches, varying colors and pitch
            if (level >= 15 && random.nextBoolean()) {
                triggerEerieSpeech(fakeId, random);
            }

            triggerImpactFrame();
        }
    }

    private static void triggerModelShift(int level, RandomSource random) {
        modelShiftTimer = 65 + random.nextInt(45);
        double sx = (random.nextDouble() - 0.5) * 3.2;
        double sy = (random.nextDouble() - 0.2) * 1.8;
        double sz = (random.nextDouble() - 0.5) * 3.2;
        modelShiftOffset = new Vec3(sx, sy, sz);
        triggerImpactFrame();
    }

    private static void triggerNightFlash(int level, RandomSource random) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        if (originalDayTime < 0) {
            originalDayTime = mc.level.getDayTime();
        }
        nightFlashTimer = 35 + random.nextInt(25);
        nightFlashCooldown = 700 + random.nextInt(500); // 35-60s cooldown between night flashes
        triggerImpactFrame();
    }

    private static void triggerVoidFall(int level) {
        voidFallTimer = 70; // 3.5 seconds
        voidFallCooldown = 800 + Minecraft.getInstance().player.getRandom().nextInt(600); // 40-70s cooldown between void falls
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ELYTRA_FLYING, 0.45F, 0.5F));
        }
        triggerImpactFrame();
    }

    private static boolean isLookingAway(Player player, BlockPos pos) {
        Vec3 look = player.getLookAngle();
        Vec3 toBlock = Vec3.atCenterOf(pos).subtract(player.getEyePosition()).normalize();
        double dot = look.dot(toBlock);
        return dot < 0.45;
    }

    public static void clearAll() {
        GLITCHED_CHUNKS.clear();
        chunkGlitchTimer = 0;
        exposedCaveCutoffY = Integer.MIN_VALUE;

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.level != null) {
            if (!GHOST_BLOCKS.isEmpty()) {
                for (BlockPos pos : GHOST_BLOCKS) markBlockDirty(mc, pos);
            }
            if (originalDayTime >= 0) {
                mc.level.setDayTime(originalDayTime);
                originalDayTime = -1L;
            }
            for (int fakeId : FAKE_ENTITIES.keySet()) {
                Entity fake = mc.level.getEntity(fakeId);
                if (fake != null) fake.discard();
                mc.level.removeEntity(fakeId, Entity.RemovalReason.DISCARDED);
            }
            if (behindCreeperId != 0) {
                Entity creeper = mc.level.getEntity(behindCreeperId);
                if (creeper != null) creeper.discard();
                mc.level.removeEntity(behindCreeperId, Entity.RemovalReason.DISCARDED);
            }
            if (dontBlinkEndermanId != 0) {
                Entity enderman = mc.level.getEntity(dontBlinkEndermanId);
                if (enderman != null) enderman.discard();
                mc.level.removeEntity(dontBlinkEndermanId, Entity.RemovalReason.DISCARDED);
            }
        }
        GHOST_BLOCKS.clear();
        GHOST_BLOCK_TIMERS.clear();

        UNDERWATER_HIDDEN_ENTITIES.clear();
        UNDERWATER_TIMERS.clear();
        TRANSLUCENT_GLITCH_ENTITIES.clear();
        TRANSLUCENT_TIMERS.clear();
        FLOATING_CREATURES.clear();
        FLOATING_TIMERS.clear();
        FAKE_ENTITIES.clear();
        SCHEDULED_SOUNDS.clear();
        behindCreeperId = 0;
        behindCreeperTimeout = 0;
        behindCreeperFuse = -1;
        dontBlinkEndermanId = 0;
        dontBlinkEndermanTimer = 0;
        pendingHeartbeatTicks = 0;
        voidFallTimer = 0;
        voidFallCooldown = 600;
        modelShiftTimer = 0;
        modelShiftOffset = Vec3.ZERO;
        nightFlashTimer = 0;
        nightFlashCooldown = 500;
        originalDayTime = -1L;
        vignetteFlashTicks = 0;
        delayNextFootstep = false;
        stillTicks = 0;
        whisperCooldown = 120;
    }
}

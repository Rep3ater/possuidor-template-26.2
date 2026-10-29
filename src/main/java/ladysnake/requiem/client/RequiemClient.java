package ladysnake.requiem.client;

import ladysnake.requiem.Requiem;
import com.mojang.blaze3d.platform.InputConstants;
import ladysnake.requiem.api.v1.entity.ability.AbilityType;
import ladysnake.requiem.api.v1.entity.ability.MobAbilityController;
import ladysnake.requiem.api.v1.entity.MovementConfig;
import ladysnake.requiem.api.v1.entity.MovementRegistry;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.client.particle.GenericRequiemParticle;
import ladysnake.requiem.client.particle.GhostParticle;
import ladysnake.requiem.client.render.entity.MorticianEntityRenderer;
import ladysnake.requiem.client.render.entity.PlayerShellEntityRenderer;
import ladysnake.requiem.core.possession.PossessionComponentImpl;
import ladysnake.requiem.common.entity.RequiemEntities;
import ladysnake.requiem.common.entity.effect.RequiemStatusEffects;
import ladysnake.requiem.common.network.RequiemNetworking;
import ladysnake.requiem.common.particle.RequiemParticleTypes;
import ladysnake.requiem.common.particle.WispTrailParticleEffect;
import ladysnake.requiem.common.sharedbody.SharedBodyClientState;
import ladysnake.requiem.common.sharedbody.SharedBodyNetworking;
import ladysnake.requiem.core.ability.PlayerAbilityController;
import ladysnake.requiem.core.entity.SoulHolderComponent;
import ladysnake.requiem.mixin.client.GameRendererAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.HuskRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.PiglinRenderer;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

public final class RequiemClient {

    public static final Identifier SPECTRE_POST_EFFECT = Identifier.fromNamespaceAndPath("requiem", "spectre");
    public static final Identifier LOCKI_SPIRIT_POST_EFFECT = Identifier.fromNamespaceAndPath("requiem", "locki_spirit");
    public static final Identifier LOCKI_APPLE_POST_EFFECT = Identifier.fromNamespaceAndPath("requiem", "locki_apple_vision");
    public static final Identifier LOCKI_WHITE_VISION_POST_EFFECT = Identifier.fromNamespaceAndPath("requiem", "locki_white_vision");
    public static final Identifier NUCLEUS_POST_EFFECT = Identifier.fromNamespaceAndPath("requiem", "nucleus_transition");
    public static final ContextKey<Integer> LOCKI_ROT_STAGE = new ContextKey<>(Identifier.fromNamespaceAndPath("requiem", "locki_rot_stage"));
    public static final ContextKey<Boolean> HAS_ATTRITION = new ContextKey<>(Identifier.fromNamespaceAndPath("requiem", "has_attrition"));
    public static final ContextKey<Integer> CORRUPTION_LEVEL = new ContextKey<>(Identifier.fromNamespaceAndPath("requiem", "corruption_level"));
    private static final Identifier VIGNETTE_LOCATION = Identifier.withDefaultNamespace("textures/misc/vignette.png");
    public static final ContextKey<Boolean> IS_LOCAL_HOST = new ContextKey<>(Identifier.fromNamespaceAndPath("requiem", "is_local_host"));

    public static final PlayerSkin SOUL_SKIN = new PlayerSkin(
        new ClientAsset.ResourceTexture(Requiem.id("entity/soul"), Requiem.id("textures/entity/soul.png")),
        null,
        null,
        PlayerModelType.WIDE,
        true
    );

    /** Opacity of a mob driven through the Shared Body effect, so that it never fully blocks the player's view. */
    public static final ContextKey<Float> SHARED_BODY_ALPHA = new ContextKey<>(Identifier.fromNamespaceAndPath("requiem", "shared_body_alpha"));
    public static final ContextKey<Float> GHOST_ALPHA = new ContextKey<>(Identifier.fromNamespaceAndPath("requiem", "ghost_alpha"));
    private static final float SHARED_BODY_MIN_ALPHA = 0.1F;
    /** Angular distance (radians) over which a driven mob fades back to full opacity, measured from the screen center. */
    private static final double SHARED_BODY_FADE_RADIANS = 0.35;

    public static KeyMapping DISSOCIATION_KEY;
    public static KeyMapping CONSTRUCT_ACTION_KEY;
    public static KeyMapping CONSTRUCT_SECONDARY_KEY;

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(RequiemClient::onRegisterRenderers);
        modEventBus.addListener(RequiemClient::onRegisterKeyMappings);
        modEventBus.addListener(RequiemClient::onRegisterRenderStateModifiers);
        modEventBus.addListener(RequiemClient::onRegisterParticleProviders);
        NeoForge.EVENT_BUS.addListener(RequiemClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(RequiemClient::onRenderPlayerPre);
        NeoForge.EVENT_BUS.addListener(RequiemClient::onRenderLivingPre);
        NeoForge.EVENT_BUS.addListener(RequiemClient::onRenderHand);
        NeoForge.EVENT_BUS.addListener(RequiemClient::onRenderGuiPost);
        NeoForge.EVENT_BUS.addListener(RequiemClient::onInteractionKeyMapping);
        NeoForge.EVENT_BUS.addListener(RequiemClient::onLoggingOut);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RequiemEntities.CURED_VILLAGER, VillagerRenderer::new);
        event.registerEntityRenderer(RequiemEntities.CURED_PIGLIN, context -> new PiglinRenderer(context, ModelLayers.PIGLIN, ModelLayers.PIGLIN_BABY, ModelLayers.PIGLIN_ARMOR, ModelLayers.PIGLIN_BABY_ARMOR));
        event.registerEntityRenderer(RequiemEntities.CURED_PIGLIN_BRUTE, context -> new PiglinRenderer(context, ModelLayers.PIGLIN_BRUTE, ModelLayers.PIGLIN_BRUTE, ModelLayers.PIGLIN_BRUTE_ARMOR, ModelLayers.PIGLIN_BRUTE_ARMOR));
        event.registerEntityRenderer(RequiemEntities.MORTICIAN, MorticianEntityRenderer::new);
        event.registerEntityRenderer(RequiemEntities.NUCLEUS_SOVEREIGN, HuskRenderer::new);
        event.registerEntityRenderer(RequiemEntities.RELEASED_SOUL, NoopRenderer::new);
        event.registerEntityRenderer(RequiemEntities.OBELISK_SOUL, NoopRenderer::new);

        EntityType<? extends AbstractClientPlayer> shellType = (EntityType) RequiemEntities.PLAYER_SHELL;
        event.registerEntityRenderer(shellType, PlayerShellEntityRenderer::new);
        event.registerEntityRenderer(RequiemEntities.ROBOTIC_CONSTRUCT, ladysnake.requiem.client.render.entity.RoboticConstructRenderer::new);
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        DISSOCIATION_KEY = new KeyMapping(
            "key.requiem.dissociation",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_COMMA,
            KeyMapping.Category.GAMEPLAY
        );
        event.register(DISSOCIATION_KEY);

        CONSTRUCT_ACTION_KEY = new KeyMapping(
            "key.requiem.construct_action",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KeyMapping.Category.GAMEPLAY
        );
        event.register(CONSTRUCT_ACTION_KEY);

        CONSTRUCT_SECONDARY_KEY = new KeyMapping(
            "key.requiem.construct_secondary",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KeyMapping.Category.GAMEPLAY
        );
        event.register(CONSTRUCT_SECONDARY_KEY);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onRegisterRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier((Class) LivingEntityRenderer.class, (entity, state) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && entity instanceof LivingEntity living) {
                if (SharedBodyClientState.isControlled(living.getId())) {
                    float alpha = sharedBodyAlpha(mc.player, living);
                    if (alpha < 0.999F) {
                        ((EntityRenderState) state).setRenderData(SHARED_BODY_ALPHA, alpha);
                    }
                }
                if (living instanceof Player) {
                    int rotStage = ladysnake.requiem.client.locki.LockiClientState.rotStage(living.getId());
                    if (rotStage > 0) ((EntityRenderState) state).setRenderData(LOCKI_ROT_STAGE, rotStage);
                }
                int corruptionLevel = 0;
                var attrition = living.getEffect(RequiemStatusEffects.ATTRITION);
                if (attrition != null) corruptionLevel = Math.max(corruptionLevel, attrition.getAmplifier() + 1);
                var ashLung = living.getEffect(RequiemStatusEffects.ASH_LUNG);
                if (ashLung != null) corruptionLevel = Math.max(corruptionLevel, ashLung.getAmplifier() + 1);
                var brainDamage = living.getEffect(RequiemStatusEffects.BRAIN_DAMAGE);
                if (brainDamage != null) corruptionLevel = Math.max(corruptionLevel, brainDamage.getAmplifier() + 1);
                var homunculusMind = living.getEffect(RequiemStatusEffects.HOMUNCULUS_MENTAL_HEALTH);
                if (homunculusMind != null) corruptionLevel = Math.max(corruptionLevel, homunculusMind.getAmplifier() + 1);
                if (corruptionLevel > 0) {
                    ((EntityRenderState) state).setRenderData(HAS_ATTRITION, living.hasEffect(RequiemStatusEffects.ATTRITION));
                    ((EntityRenderState) state).setRenderData(CORRUPTION_LEVEL, Math.min(10, corruptionLevel));
                }
                Mob host = PossessionComponent.getHost(mc.player);
                if (host != null && host.getId() == living.getId()) {
                    ((EntityRenderState) state).setRenderData(IS_LOCAL_HOST, true);
                }
            }
        });

        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState renderState) {
                if (avatar instanceof Player player && ladysnake.requiem.client.locki.LockiClientState.isInvisibleMode(player.getId())) {
                    // Locki's spirit/root/apple forms have no renderable player body at all: no
                    // armor layers, player shadow, or translucent avatar. Environmental root
                    // visuals are separate from the hidden entity.
                    renderState.headEquipment = ItemStack.EMPTY;
                    renderState.chestEquipment = ItemStack.EMPTY;
                    renderState.legsEquipment = ItemStack.EMPTY;
                    renderState.feetEquipment = ItemStack.EMPTY;
                    renderState.isInvisible = true;
                    renderState.isInvisibleToPlayer = true;
                } else if (avatar instanceof Player player && RemnantComponent.isIncorporeal(player)) {
                    ((EntityRenderState) renderState).setRenderData(GHOST_ALPHA, 0.10F);
                    renderState.headEquipment = ItemStack.EMPTY;
                    renderState.chestEquipment = ItemStack.EMPTY;
                    renderState.legsEquipment = ItemStack.EMPTY;
                    renderState.feetEquipment = ItemStack.EMPTY;

                    renderState.isInvisible = false;
                    renderState.isInvisibleToPlayer = false;

                    boolean isFlying = player.getAbilities().flying || !player.onGround();
                    Vec3 vel = player.getDeltaMovement();
                    double hSpeedSqr = vel.x * vel.x + vel.z * vel.z;
                    boolean isMoving = hSpeedSqr > 0.0005 || Math.abs(vel.y) > 0.03;

                    if (isFlying) {
                        // Suppress ground-walking leg flailing in mid-air
                        renderState.walkAnimationSpeed = 0.0F;
                        renderState.walkAnimationPos = 0.0F;

                        if (isMoving) {
                            // Ethereal forward glide pose (gliding like a phantom/wraith)
                            renderState.isFallFlying = true;
                            renderState.fallFlyingTimeInTicks = 12.0F;
                            renderState.shouldApplyFlyingYRot = false;
                        } else {
                            // Upright hovering stance
                            renderState.isFallFlying = false;
                            renderState.fallFlyingTimeInTicks = 0.0F;
                        }
                    }
                }
            }
        });
    }

    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(RequiemParticleTypes.GHOST, GhostParticle.Provider::new);
        event.registerSpriteSet(RequiemParticleTypes.ATTRITION, GenericRequiemParticle.SimpleProvider::new);
        event.registerSpriteSet(RequiemParticleTypes.ATTUNED, GenericRequiemParticle.SimpleProvider::new);
        event.registerSpriteSet(RequiemParticleTypes.CURE, GenericRequiemParticle.SimpleProvider::new);
        event.registerSpriteSet(RequiemParticleTypes.PENANCE, GenericRequiemParticle.SimpleProvider::new);
        event.registerSpriteSet(RequiemParticleTypes.OBELISK_SOUL, GenericRequiemParticle.SimpleProvider::new);
        event.registerSpriteSet(RequiemParticleTypes.SOUL_TRAIL, GenericRequiemParticle.WispTrailProvider::new);
        event.registerSpriteSet(RequiemParticleTypes.ENTITY_DUST, GenericRequiemParticle.EntityDustProvider::new);
    }

    public static void onRenderPlayerPre(RenderPlayerEvent.Pre<?> event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(event.getRenderState().id);
            if (entity instanceof Player player) {
                if (ladysnake.requiem.client.locki.LockiClientState.isInvisibleMode(player.getId())) {
                    event.setCanceled(true);
                } else if (PossessionComponent.get(player).isPossessionOngoing()) {
                    event.setCanceled(true);
                } else if (RemnantComponent.isIncorporeal(player)) {
                    // Stealth ghost form: No noisy render-frame particle spam so ghosts can spy
                }
            }
        }
    }

    public static void onRenderHand(RenderHandEvent event) {
        // The local Locki avatar is not the fruit's carrier, so its first-person arms would be misleading.
        if (ladysnake.requiem.client.locki.LockiClientState.isLockiModeActive()
                && "apple".equals(ladysnake.requiem.client.locki.LockiClientState.mode())
                && ladysnake.requiem.client.locki.LockiClientState.cameraMode() == 2) {
            event.setCanceled(true);
        }
    }

    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?, ?> event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.getCameraType().isFirstPerson()) {
            Boolean isHost = event.getRenderState().getRenderData(IS_LOCAL_HOST);
            if (Boolean.TRUE.equals(isHost)) {
                event.setCanceled(true);
            }
        }
    }

    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && RemnantComponent.isIncorporeal(mc.player)
                && !(ladysnake.requiem.client.locki.LockiClientState.isLockiModeActive()
                && !ladysnake.requiem.client.locki.LockiClientState.isSpiritVision())) {
            GuiGraphicsExtractor graphics = event.getGuiGraphics();
            long time = System.currentTimeMillis();
            // Subtle ethereal pulse between 0.35 and 0.60
            float pulse = 0.45F + 0.15F * Mth.sin((float) (time % 4000L) / 4000.0F * Mth.TWO_PI);
            int color = ARGB.colorFromFloat(pulse, 0.0F, 0.75F, 1.0F);
            graphics.blit(
                RenderPipelines.VIGNETTE,
                VIGNETTE_LOCATION,
                0,
                0,
                0.0F,
                0.0F,
                graphics.guiWidth(),
                graphics.guiHeight(),
                graphics.guiWidth(),
                graphics.guiHeight(),
                color
            );

            // Draw possession crosshair target icon
            Entity target = mc.crosshairPickEntity;
            if (target instanceof Mob mob) {
                boolean soulless = SoulHolderComponent.isSoulless(mob);
                boolean inRange = mc.player != null && mc.player.distanceToSqr(mob) <= 25.0D;
                int centerX = graphics.guiWidth() / 2;
                int centerY = graphics.guiHeight() / 2;

                if (soulless) {
                    Identifier icon = mob.getType() == EntityTypes.ENDERMAN
                        ? Identifier.fromNamespaceAndPath("requiem", "textures/gui/enderjacking_icon.png")
                        : Identifier.fromNamespaceAndPath("requiem", "textures/gui/possession_icon.png");

                    // The texture is 16x16 composed of two 16x8 frames: v=0 (idle), v=8 (active in range)
                    float vOffset = inRange ? 8.0F : 0.0F;
                    graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        icon,
                        centerX - 8,
                        centerY - 4,
                        0.0F,
                        vOffset,
                        16,
                        8,
                        16,
                        16
                    );
                } else {
                    // Living mob that cannot be possessed directly without soul extraction
                    Identifier lockIcon = Identifier.fromNamespaceAndPath("requiem", "textures/gui/locked_slot.png");
                    graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        lockIcon,
                        centerX - 8,
                        centerY - 8,
                        0.0F,
                        0.0F,
                        16,
                        16,
                        16,
                        16
                    );
                }
            }
        }
    }

    public static void onInteractionKeyMapping(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        PossessionComponent possession = PossessionComponent.get(mc.player);
        if (!possession.isPossessionOngoing()) return;

        if (event.isAttack()) {
            PlayerAbilityController controller = PlayerAbilityController.get(mc.player);
            InteractionResult directResult = controller.useDirectAbility(AbilityType.ATTACK);
            if (directResult.consumesAction()) {
                event.setCanceled(true);
                return;
            }
            MobAbilityController mobController = MobAbilityController.get(mc.player);
            if (mobController.useIndirect(AbilityType.ATTACK)) {
                RequiemNetworking.sendIndirectAbilityUseMessage(AbilityType.ATTACK);
                event.setCanceled(true);
            }
        } else if (event.isUseItem()) {
            ItemStack mainHand = mc.player.getMainHandItem();
            if (!mainHand.isEmpty() && (mainHand.is(Items.BOW) || mainHand.is(Items.CROSSBOW) || mainHand.is(Items.TRIDENT))) {
                return;
            }
            PlayerAbilityController controller = PlayerAbilityController.get(mc.player);
            InteractionResult directResult = controller.useDirectAbility(AbilityType.INTERACT);
            if (directResult.consumesAction()) {
                event.setCanceled(true);
                return;
            }
            MobAbilityController mobController = MobAbilityController.get(mc.player);
            if (mobController.useIndirect(AbilityType.INTERACT)) {
                RequiemNetworking.sendIndirectAbilityUseMessage(AbilityType.INTERACT);
                event.setCanceled(true);
            }
        }
    }

    /**
     * Computes how opaque a driven mob should be. The closer the mob is to the center of the screen, the more
     * transparent it gets, fading in a gradient back to full opacity towards the edges of the view.
     */
    private static float sharedBodyAlpha(Player viewer, LivingEntity mob) {
        Vec3 toMob = mob.getBoundingBox().getCenter().subtract(viewer.getEyePosition());
        double distance = toMob.length();
        if (distance < 1.0E-3) {
            return SHARED_BODY_MIN_ALPHA;
        }
        double cos = viewer.getViewVector(1.0F).dot(toMob) / distance;
        if (cos <= 0.0) {
            return 1.0F;
        }
        double angle = Math.acos(Math.min(1.0, cos));
        // A big or close mob covers more of the screen, so it keeps its transparency further from the center
        double angularRadius = Math.atan2(Math.max(mob.getBbWidth(), mob.getBbHeight()) * 0.5, distance);
        double t = Mth.clamp((angle - angularRadius * 0.6) / SHARED_BODY_FADE_RADIANS, 0.0, 1.0);
        t = t * t * (3.0 - 2.0 * t);
        return (float) (SHARED_BODY_MIN_ALPHA + (1.0F - SHARED_BODY_MIN_ALPHA) * t);
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SharedBodyClientState.reset();
        ladysnake.requiem.client.locki.LockiClientState.reset();
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ladysnake.requiem.client.locki.LockiClientState.tick();
        if (mc.level != null && mc.player != null) {
            // Let the server know which keys are held, so controlled mobs can mimic attacking and using
            boolean inGame = mc.mouseHandler.isMouseGrabbed();
            boolean attackHeld = inGame && mc.options.keyAttack.isDown();
            boolean useHeld = inGame && mc.options.keyUse.isDown();
            SharedBodyClientState.updateHeldKeys(attackHeld, useHeld,
                () -> ClientPacketDistributor.sendToServer(new SharedBodyNetworking.InputPayload(attackHeld, useHeld)));
            // Keep creative flight enabled client-side only when incorporeal
            if (RemnantComponent.isIncorporeal(mc.player)) {
                if (!mc.player.isCreative() && !mc.player.isSpectator()) {
                    if (ladysnake.requiem.client.locki.LockiClientState.shouldSuppressFlight()) {
                        mc.player.getAbilities().flying = false;
                        mc.player.getAbilities().mayfly = false;
                    } else {
                        mc.player.getAbilities().mayfly = true;
                    }
                }
            } else {
                PossessionComponent pc = PossessionComponent.get(mc.player);
                if (pc.isPossessionOngoing()) {
                    Mob host = pc.getHost();
                    boolean hostFlies = PossessionComponentImpl.canHostFly(host);
                    if (!mc.player.isCreative() && !mc.player.isSpectator()) {
                        mc.player.getAbilities().mayfly = hostFlies;
                        if (!hostFlies) {
                            mc.player.getAbilities().flying = false;
                        }
                    }
                } else if (!RemnantComponent.isIncorporeal(mc.player) && !mc.player.isCreative() && !mc.player.isSpectator()) {
                    if (mc.player.getAbilities().mayfly) {
                        mc.player.getAbilities().mayfly = false;
                        mc.player.getAbilities().flying = false;
                    }
                }
            }

            // Locki's plant/fruit viewpoints take priority over the generic out-of-body shader.
            Identifier current = mc.gameRenderer.currentPostEffect();
            Identifier lockiEffect = ladysnake.requiem.client.locki.LockiClientState.desiredPostEffect();
            if (mc.player.hasEffect(RequiemStatusEffects.NUCLEUS_TRANSITION)) {
                if (!NUCLEUS_POST_EFFECT.equals(current)) ((GameRendererAccessor) mc.gameRenderer).requiem$setPostEffect(NUCLEUS_POST_EFFECT);
            } else if (ladysnake.requiem.client.locki.LockiClientState.isLockiModeActive()) {
                if (lockiEffect != null && !lockiEffect.equals(current)) {
                    ((GameRendererAccessor) mc.gameRenderer).requiem$setPostEffect(lockiEffect);
                } else if (lockiEffect == null && current != null) {
                    mc.gameRenderer.clearPostEffect();
                }
            } else if (RemnantComponent.isIncorporeal(mc.player) && !ladysnake.requiem.common.RequiemConfig.graphics.reducedEffects) {
                if (!SPECTRE_POST_EFFECT.equals(current)) {
                    ((GameRendererAccessor) mc.gameRenderer).requiem$setPostEffect(SPECTRE_POST_EFFECT);
                }
            } else if (SPECTRE_POST_EFFECT.equals(current) || LOCKI_SPIRIT_POST_EFFECT.equals(current)
                    || LOCKI_APPLE_POST_EFFECT.equals(current) || LOCKI_WHITE_VISION_POST_EFFECT.equals(current)
                    || NUCLEUS_POST_EFFECT.equals(current)) {
                mc.gameRenderer.clearPostEffect();
            }

            // Spawn faint soul particles around incorporeal players
            for (Player p : mc.level.players()) {
                if (RemnantComponent.isIncorporeal(p)
                        && !ladysnake.requiem.client.locki.LockiClientState.isInvisibleMode(p.getId())) {
                    RandomSource rand = p.getRandom();
                    Vec3 vel = p.getDeltaMovement();
                    boolean isMoving = (vel.x * vel.x + vel.z * vel.z) > 0.002 || Math.abs(vel.y) > 0.04;
                    // Faint, rare particles so ghosts can stealthily spy on players without giving away position
                    float spawnChance = isMoving ? 0.03F : 0.005F;
                    if (rand.nextFloat() < spawnChance * ladysnake.requiem.common.RequiemConfig.graphics.particleDensity) {
                        mc.level.addParticle(
                            RequiemParticleTypes.GHOST,
                            p.getRandomX(0.5),
                            p.getRandomY(),
                            p.getRandomZ(0.5),
                            (rand.nextDouble() - 0.5) * 0.02,
                            rand.nextDouble() * 0.02,
                            (rand.nextDouble() - 0.5) * 0.02
                        );
                    }
                }
            }

            // Check dissociation key press
            if (DISSOCIATION_KEY != null && DISSOCIATION_KEY.consumeClick()) {
                ClientPacketDistributor.sendToServer(new RequiemNetworking.EtherealFracturePayload());
            }
            if (CONSTRUCT_ACTION_KEY != null && CONSTRUCT_ACTION_KEY.consumeClick()) {
                ClientPacketDistributor.sendToServer(new RequiemNetworking.ConstructActionPayload(1));
            }
            if (CONSTRUCT_SECONDARY_KEY != null && CONSTRUCT_SECONDARY_KEY.consumeClick()) {
                ClientPacketDistributor.sendToServer(new RequiemNetworking.ConstructActionPayload(2));
            }
        }
    }
}

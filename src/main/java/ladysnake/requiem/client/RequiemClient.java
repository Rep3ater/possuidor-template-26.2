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
import ladysnake.requiem.client.render.entity.HomunculusEntityRenderer;
import ladysnake.requiem.client.render.entity.ImmortalHomunculusEntityRenderer;
import ladysnake.requiem.client.model.HomunculusModel;
import ladysnake.requiem.client.model.ImmortalHomunculusModel;
import ladysnake.requiem.core.possession.PossessionComponentImpl;
import ladysnake.requiem.common.entity.RequiemEntities;
import ladysnake.requiem.common.entity.effect.RequiemStatusEffects;
import ladysnake.requiem.common.network.RequiemNetworking;
import ladysnake.requiem.client.network.ClientMessageHandling;
import ladysnake.requiem.common.particle.RequiemParticleTypes;
import ladysnake.requiem.common.particle.WispTrailParticleEffect;
import ladysnake.requiem.common.sharedbody.SharedBodyClientState;
import ladysnake.requiem.common.sharedbody.SharedBodyNetworking;
import ladysnake.requiem.core.ability.PlayerAbilityController;
import ladysnake.requiem.core.entity.SoulHolderComponent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.PiglinRenderer;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
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
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class RequiemClient {

    public static final ResourceLocation SPECTRE_POST_EFFECT = ResourceLocation.fromNamespaceAndPath("requiem", "spectre");
    public static final ResourceLocation LOCKI_SPIRIT_POST_EFFECT = ResourceLocation.fromNamespaceAndPath("requiem", "locki_spirit");
    public static final ResourceLocation LOCKI_APPLE_POST_EFFECT = ResourceLocation.fromNamespaceAndPath("requiem", "locki_apple_vision");
    public static final ResourceLocation LOCKI_WHITE_VISION_POST_EFFECT = ResourceLocation.fromNamespaceAndPath("requiem", "locki_white_vision");
    public static final ResourceLocation NUCLEUS_POST_EFFECT = ResourceLocation.fromNamespaceAndPath("requiem", "nucleus_transition");

    /** Opacity of a mob driven through Shared Body, so it never fully blocks the player's view. */
    private static final float SHARED_BODY_MIN_ALPHA = 0.18F;
    /** Angular distance (radians) over which a driven mob fades back to full opacity. */
    private static final double SHARED_BODY_FADE_RADIANS = Math.toRadians(24.0);

    private static ResourceLocation activePostEffect;
    private static boolean parmenidesPreviewKeyDown;
    private static int parmenidesPreviewRequestCooldown;
    private static int parmenidesPreviewRequestedChunkX = Integer.MIN_VALUE;
    private static int parmenidesPreviewRequestedChunkZ = Integer.MIN_VALUE;
    private static int parmenidesPreviewDimension = -1;

    public static ResourceLocation activePostEffect() {
        return activePostEffect;
    }

    public static void applyPostEffect(ResourceLocation effect) {
        Minecraft mc = Minecraft.getInstance();
        if (effect == null) {
            if (activePostEffect != null) {
                mc.gameRenderer.shutdownEffect();
                activePostEffect = null;
            }
        } else if (!effect.equals(activePostEffect)) {
            mc.gameRenderer.loadEffect(effect);
            activePostEffect = effect;
        }
    }

    public static KeyMapping DISSOCIATION_KEY;
    public static KeyMapping CONSTRUCT_ACTION_KEY;
    public static KeyMapping CONSTRUCT_SECONDARY_KEY;

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ClientMessageHandling::registerPayloadHandlers);
        modEventBus.addListener(RequiemClient::onRegisterRenderers);
        modEventBus.addListener(RequiemClient::onRegisterLayerDefinitions);
        modEventBus.addListener(RequiemClient::onRegisterKeyMappings);
        modEventBus.addListener(RequiemClient::onRegisterParticleProviders);
        NeoForge.EVENT_BUS.addListener(RequiemClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(RequiemClient::onRenderPlayerPre);
        NeoForge.EVENT_BUS.addListener(ladysnake.requiem.client.render.ParmenidesChunkPreviewRenderer::render);
        NeoForge.EVENT_BUS.addListener(RequiemClient::onRenderLivingPre);
        NeoForge.EVENT_BUS.addListener(RequiemClient::onRenderHand);
        NeoForge.EVENT_BUS.addListener(RequiemClient::onRenderGuiPost);
        NeoForge.EVENT_BUS.addListener(RequiemClient::onInteractionKeyMapping);
        NeoForge.EVENT_BUS.addListener(RequiemClient::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(ladysnake.requiem.client.render.SpeechBubbleRenderer::renderWorldBubbles);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RequiemEntities.CURED_VILLAGER, VillagerRenderer::new);
        event.registerEntityRenderer(RequiemEntities.CURED_PIGLIN, context -> new PiglinRenderer(context, ModelLayers.PIGLIN, ModelLayers.PIGLIN_INNER_ARMOR, ModelLayers.PIGLIN_OUTER_ARMOR, false));
        event.registerEntityRenderer(RequiemEntities.CURED_PIGLIN_BRUTE, context -> new PiglinRenderer(context, ModelLayers.PIGLIN_BRUTE, ModelLayers.PIGLIN_BRUTE_INNER_ARMOR, ModelLayers.PIGLIN_BRUTE_OUTER_ARMOR, false));
        event.registerEntityRenderer(RequiemEntities.MORTICIAN, MorticianEntityRenderer::new);
        event.registerEntityRenderer(RequiemEntities.RELEASED_SOUL, NoopRenderer::new);
        event.registerEntityRenderer(RequiemEntities.OBELISK_SOUL, NoopRenderer::new);

        EntityType<? extends AbstractClientPlayer> shellType = (EntityType) RequiemEntities.PLAYER_SHELL;
        event.registerEntityRenderer(shellType, PlayerShellEntityRenderer::new);
        event.registerEntityRenderer(RequiemEntities.ROBOTIC_CONSTRUCT, ladysnake.requiem.client.render.entity.RoboticConstructRenderer::new);
        event.registerEntityRenderer(RequiemEntities.HOMUNCULUS, HomunculusEntityRenderer::new);
        event.registerEntityRenderer(RequiemEntities.IMMORTAL_HOMUNCULUS, ImmortalHomunculusEntityRenderer::new);
    }

    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(HomunculusModel.LAYER_LOCATION, HomunculusModel::createBodyLayer);
        event.registerLayerDefinition(ImmortalHomunculusModel.LAYER_LOCATION, ImmortalHomunculusModel::createBodyLayer);
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        DISSOCIATION_KEY = new KeyMapping(
            "key.requiem.dissociation",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_COMMA,
            "key.categories.gameplay"
        );
        event.register(DISSOCIATION_KEY);

        CONSTRUCT_ACTION_KEY = new KeyMapping(
            "key.requiem.construct_action",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.gameplay"
        );
        event.register(CONSTRUCT_ACTION_KEY);

        CONSTRUCT_SECONDARY_KEY = new KeyMapping(
            "key.requiem.construct_secondary",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.gameplay"
        );
        event.register(CONSTRUCT_SECONDARY_KEY);
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

    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (ladysnake.requiem.client.locki.LockiClientState.isInvisibleMode(player.getId())
                || PossessionComponent.get(player).isPossessionOngoing()) {
            event.setCanceled(true);
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

    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.getCameraType().isFirstPerson() || mc.player == null) {
            return;
        }

        // In ordinary possession the camera is attached to the host mob. Hide that host's
        // model in first person as well as the Shared Body controlled entity.
        PossessionComponent possession = PossessionComponent.get(mc.player);
        Mob possessedHost = possession.isPossessionOngoing() ? possession.getHost() : null;
        if (SharedBodyClientState.isControlled(event.getEntity().getId())
                || event.getEntity() == possessedHost) {
            event.setCanceled(true);
        }
    }

    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        ladysnake.requiem.client.render.SpeechBubbleRenderer.renderHudBubble(event.getGuiGraphics());
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !RemnantComponent.isIncorporeal(mc.player)
                || (ladysnake.requiem.client.locki.LockiClientState.isLockiModeActive()
                && !ladysnake.requiem.client.locki.LockiClientState.isSpiritVision())) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Entity target = mc.crosshairPickEntity;
        if (!(target instanceof Mob mob)) {
            return;
        }

        boolean soulless = SoulHolderComponent.isSoulless(mob);
        boolean inRange = mc.player.distanceToSqr(mob) <= 25.0D;
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        if (soulless) {
            ResourceLocation icon = mob.getType() == EntityType.ENDERMAN
                ? ResourceLocation.fromNamespaceAndPath("requiem", "textures/gui/enderjacking_icon.png")
                : ResourceLocation.fromNamespaceAndPath("requiem", "textures/gui/possession_icon.png");
            float vOffset = inRange ? 8.0F : 0.0F;
            graphics.blit(icon, centerX - 8, centerY - 4, 0.0F, vOffset, 16, 8, 16, 16);
        } else {
            ResourceLocation lockIcon = ResourceLocation.fromNamespaceAndPath("requiem", "textures/gui/locked_slot.png");
            graphics.blit(lockIcon, centerX - 8, centerY - 8, 0.0F, 0.0F, 16, 16, 16, 16);
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
        ladysnake.requiem.client.SpeechLanguageSync.reset();
        ladysnake.requiem.client.SpeechBubbleClientState.clear();
        SharedBodyClientState.reset();
        parmenidesPreviewKeyDown = false;
        parmenidesPreviewRequestCooldown = 0;
        parmenidesPreviewRequestedChunkX = Integer.MIN_VALUE;
        parmenidesPreviewRequestedChunkZ = Integer.MIN_VALUE;
        parmenidesPreviewDimension = -1;
        ParmenidesClientState.clearPreview();
        activePostEffect = null;
        ladysnake.requiem.client.locki.LockiClientState.reset();
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        ladysnake.requiem.client.SpeechBubbleClientState.tick();
        Minecraft mc = Minecraft.getInstance();
        ladysnake.requiem.client.SpeechLanguageSync.tick(mc);
        ladysnake.requiem.client.locki.LockiClientState.tick();
        if (mc.level != null && mc.player != null) {
            boolean previewHeld = mc.options.keyShift.isDown() && ParmenidesClientState.isParmenides(mc.player)
                && (mc.level.dimension() == net.minecraft.world.level.Level.OVERWORLD
                    || mc.level.dimension() == net.minecraft.world.level.Level.NETHER);
            if (previewHeld) {
                int chunkX = mc.player.chunkPosition().x;
                int chunkZ = mc.player.chunkPosition().z;
                int dimension = mc.level.dimension() == net.minecraft.world.level.Level.NETHER ? 1 : 0;
                boolean changedChunk = chunkX != parmenidesPreviewRequestedChunkX
                    || chunkZ != parmenidesPreviewRequestedChunkZ
                    || dimension != parmenidesPreviewDimension;
                if (!parmenidesPreviewKeyDown) {
                    ParmenidesClientState.beginPreview(mc.level.getGameTime(), chunkX, chunkZ, dimension == 1);
                    PacketDistributor.sendToServer(new RequiemNetworking.ParmenidesPreviewRequestPayload(true, chunkX, chunkZ));
                    parmenidesPreviewRequestCooldown = 20;
                } else if (changedChunk) {
                    if (dimension != parmenidesPreviewDimension) {
                        ParmenidesClientState.beginPreview(mc.level.getGameTime(), chunkX, chunkZ, dimension == 1);
                    } else {
                        ParmenidesClientState.movePreviewCenter(chunkX, chunkZ, dimension == 1);
                    }
                    PacketDistributor.sendToServer(new RequiemNetworking.ParmenidesPreviewRequestPayload(true, chunkX, chunkZ));
                    parmenidesPreviewRequestCooldown = 20;
                }
                parmenidesPreviewRequestedChunkX = chunkX;
                parmenidesPreviewRequestedChunkZ = chunkZ;
                parmenidesPreviewDimension = dimension;
                parmenidesPreviewKeyDown = true;
            } else {
                if (parmenidesPreviewKeyDown) {
                    PacketDistributor.sendToServer(new RequiemNetworking.ParmenidesPreviewRequestPayload(false, 0, 0));
                }
                parmenidesPreviewKeyDown = false;
                parmenidesPreviewRequestCooldown = 0;
                parmenidesPreviewRequestedChunkX = Integer.MIN_VALUE;
                parmenidesPreviewRequestedChunkZ = Integer.MIN_VALUE;
                parmenidesPreviewDimension = -1;
                ParmenidesClientState.clearPreview();
            }
            ParmenidesClientState.refreshHiddenTerrainIfNeeded();
            // Let the server know which keys are held, so controlled mobs can mimic attacking and using
            boolean inGame = mc.mouseHandler.isMouseGrabbed();
            boolean attackHeld = inGame && mc.options.keyAttack.isDown();
            boolean useHeld = inGame && mc.options.keyUse.isDown();
            SharedBodyClientState.updateHeldKeys(attackHeld, useHeld,
                () -> PacketDistributor.sendToServer(new SharedBodyNetworking.InputPayload(attackHeld, useHeld)));
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
            ResourceLocation current = activePostEffect;
            ResourceLocation lockiEffect = ladysnake.requiem.client.locki.LockiClientState.desiredPostEffect();
            if (mc.player.hasEffect(RequiemStatusEffects.NUCLEUS_TRANSITION)) {
                if (!NUCLEUS_POST_EFFECT.equals(current)) applyPostEffect(NUCLEUS_POST_EFFECT);
            } else if (ladysnake.requiem.client.locki.LockiClientState.isLockiModeActive()) {
                if (lockiEffect != null && !lockiEffect.equals(current)) {
                    applyPostEffect(lockiEffect);
                } else if (lockiEffect == null && current != null) {
                    applyPostEffect(null);
                }
            } else if (RemnantComponent.isIncorporeal(mc.player) && !ladysnake.requiem.common.RequiemConfig.graphics.reducedEffects) {
                if (!SPECTRE_POST_EFFECT.equals(current)) {
                    applyPostEffect(SPECTRE_POST_EFFECT);
                }
            } else if (SPECTRE_POST_EFFECT.equals(current) || LOCKI_SPIRIT_POST_EFFECT.equals(current)
                    || LOCKI_APPLE_POST_EFFECT.equals(current) || LOCKI_WHITE_VISION_POST_EFFECT.equals(current)
                    || NUCLEUS_POST_EFFECT.equals(current)) {
                applyPostEffect(null);
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
                PacketDistributor.sendToServer(new RequiemNetworking.EtherealFracturePayload());
            }
            if (CONSTRUCT_ACTION_KEY != null && CONSTRUCT_ACTION_KEY.consumeClick()) {
                PacketDistributor.sendToServer(new RequiemNetworking.ConstructActionPayload(1));
            }
            if (CONSTRUCT_SECONDARY_KEY != null && CONSTRUCT_SECONDARY_KEY.consumeClick()) {
                PacketDistributor.sendToServer(new RequiemNetworking.ConstructActionPayload(2));
            }
        }
    }
}

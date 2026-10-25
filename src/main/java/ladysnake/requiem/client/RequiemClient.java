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
import ladysnake.requiem.common.network.RequiemNetworking;
import ladysnake.requiem.common.particle.RequiemParticleTypes;
import ladysnake.requiem.common.particle.WispTrailParticleEffect;
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
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

public final class RequiemClient {

    public static final Identifier SPECTRE_POST_EFFECT = Identifier.fromNamespaceAndPath("requiem", "spectre");
    private static final Identifier VIGNETTE_LOCATION = Identifier.withDefaultNamespace("textures/misc/vignette.png");
    public static final ContextKey<Boolean> IS_LOCAL_HOST = new ContextKey<>(Identifier.fromNamespaceAndPath("requiem", "is_local_host"));

    public static final PlayerSkin SOUL_SKIN = new PlayerSkin(
        new ClientAsset.ResourceTexture(Requiem.id("entity/soul"), Requiem.id("textures/entity/soul.png")),
        null,
        null,
        PlayerModelType.WIDE,
        true
    );

    public static KeyMapping DISSOCIATION_KEY;

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(RequiemClient::onRegisterRenderers);
        modEventBus.addListener(RequiemClient::onRegisterKeyMappings);
        modEventBus.addListener(RequiemClient::onRegisterRenderStateModifiers);
        modEventBus.addListener(RequiemClient::onRegisterParticleProviders);
        NeoForge.EVENT_BUS.addListener(RequiemClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(RequiemClient::onRenderPlayerPre);
        NeoForge.EVENT_BUS.addListener(RequiemClient::onRenderLivingPre);
        NeoForge.EVENT_BUS.addListener(RequiemClient::onRenderGuiPost);
        NeoForge.EVENT_BUS.addListener(RequiemClient::onInteractionKeyMapping);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RequiemEntities.CURED_VILLAGER, VillagerRenderer::new);
        event.registerEntityRenderer(RequiemEntities.CURED_PIGLIN, context -> new PiglinRenderer(context, ModelLayers.PIGLIN, ModelLayers.PIGLIN_BABY, ModelLayers.PIGLIN_ARMOR, ModelLayers.PIGLIN_BABY_ARMOR));
        event.registerEntityRenderer(RequiemEntities.CURED_PIGLIN_BRUTE, context -> new PiglinRenderer(context, ModelLayers.PIGLIN_BRUTE, ModelLayers.PIGLIN_BRUTE, ModelLayers.PIGLIN_BRUTE_ARMOR, ModelLayers.PIGLIN_BRUTE_ARMOR));
        event.registerEntityRenderer(RequiemEntities.MORTICIAN, MorticianEntityRenderer::new);
        event.registerEntityRenderer(RequiemEntities.RELEASED_SOUL, NoopRenderer::new);
        event.registerEntityRenderer(RequiemEntities.OBELISK_SOUL, NoopRenderer::new);

        EntityType<? extends AbstractClientPlayer> shellType = (EntityType) RequiemEntities.PLAYER_SHELL;
        event.registerEntityRenderer(shellType, PlayerShellEntityRenderer::new);
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        DISSOCIATION_KEY = new KeyMapping(
            "key.requiem.dissociation",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_COMMA,
            KeyMapping.Category.GAMEPLAY
        );
        event.register(DISSOCIATION_KEY);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onRegisterRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier((Class) LivingEntityRenderer.class, (entity, state) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && entity instanceof LivingEntity living) {
                Mob host = PossessionComponent.getHost(mc.player);
                if (host != null && host.getId() == living.getId()) {
                    ((EntityRenderState) state).setRenderData(IS_LOCAL_HOST, true);
                }
            }
        });

        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState renderState) {
                if (avatar instanceof Player player && RemnantComponent.isIncorporeal(player)) {
                    renderState.skin = SOUL_SKIN;
                    renderState.headEquipment = ItemStack.EMPTY;
                    renderState.chestEquipment = ItemStack.EMPTY;
                    renderState.legsEquipment = ItemStack.EMPTY;
                    renderState.feetEquipment = ItemStack.EMPTY;
                    renderState.showCape = false;
                    renderState.showJacket = false;
                    renderState.showLeftPants = false;
                    renderState.showRightPants = false;
                    renderState.showLeftSleeve = false;
                    renderState.showRightSleeve = false;
                    renderState.showHat = false;

                    // Enable spectral ghost translucency (renders with entityTranslucent shader)
                    renderState.isInvisible = true;
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
                if (PossessionComponent.get(player).isPossessionOngoing()) {
                    event.setCanceled(true);
                } else if (RemnantComponent.isIncorporeal(player)) {
                    Vec3 vel = player.getDeltaMovement();
                    boolean isMoving = (vel.x * vel.x + vel.z * vel.z) > 0.001 || Math.abs(vel.y) > 0.03;
                    int count = isMoving ? 3 : 1;
                    for (int i = 0; i < count; i++) {
                        mc.level.addParticle(
                            RequiemParticleTypes.GHOST,
                            player.getX() + (mc.level.getRandom().nextDouble() - 0.5) * player.getBbWidth() * 0.8,
                            player.getY() + mc.level.getRandom().nextDouble() * player.getBbHeight() * 0.8,
                            player.getZ() + (mc.level.getRandom().nextDouble() - 0.5) * player.getBbWidth() * 0.8,
                            -vel.x * 0.15, 0.01 - vel.y * 0.1, -vel.z * 0.15
                        );
                    }
                    if (isMoving && mc.level.getRandom().nextFloat() < 0.4F) {
                        mc.level.addParticle(
                            new WispTrailParticleEffect(0.3F, 0.85F, 1.0F, -0.05F, -0.02F, 0.0F),
                            player.getX() - vel.x * 0.4,
                            player.getY() + 0.6 - vel.y * 0.4,
                            player.getZ() - vel.z * 0.4,
                            0, 0, 0
                        );
                    }
                }
            }
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
        if (mc.player != null && RemnantComponent.isIncorporeal(mc.player)) {
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

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            // Keep creative flight enabled client-side only when incorporeal
            if (RemnantComponent.isIncorporeal(mc.player)) {
                if (!mc.player.isCreative() && !mc.player.isSpectator()) {
                    mc.player.getAbilities().mayfly = true;
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

            // Sync out-of-body shader post effect
            Identifier current = mc.gameRenderer.currentPostEffect();
            if (RemnantComponent.isIncorporeal(mc.player)) {
                if (!SPECTRE_POST_EFFECT.equals(current)) {
                    ((GameRendererAccessor) mc.gameRenderer).requiem$setPostEffect(SPECTRE_POST_EFFECT);
                }
            } else if (SPECTRE_POST_EFFECT.equals(current)) {
                mc.gameRenderer.clearPostEffect();
            }

            // Spawn soul / ghost particles around incorporeal players
            for (Player p : mc.level.players()) {
                if (RemnantComponent.isIncorporeal(p)) {
                    RandomSource rand = p.getRandom();
                    mc.level.addParticle(
                        RequiemParticleTypes.GHOST,
                        p.getRandomX(0.5),
                        p.getRandomY(),
                        p.getRandomZ(0.5),
                        (rand.nextDouble() - 0.5) * 0.05,
                        rand.nextDouble() * 0.05,
                        (rand.nextDouble() - 0.5) * 0.05
                    );
                }
            }

            // Check dissociation key press
            if (DISSOCIATION_KEY != null && DISSOCIATION_KEY.consumeClick()) {
                ClientPacketDistributor.sendToServer(new RequiemNetworking.EtherealFracturePayload());
            }
        }
    }
}

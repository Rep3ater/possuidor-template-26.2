/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses>.
 */
package ladysnake.requiem.common.entity;

import com.google.common.base.Preconditions;
import ladysnake.requiem.api.v1.block.ObeliskDescriptor;
import ladysnake.requiem.api.v1.possession.Possessable;
import ladysnake.requiem.api.v1.record.GlobalRecord;
import ladysnake.requiem.api.v1.record.GlobalRecordKeeper;
import ladysnake.requiem.api.v1.record.RecordPointer;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.common.RequiemRecordTypes;
import ladysnake.requiem.common.enchantment.RequiemEnchantments;
import ladysnake.requiem.common.entity.ai.FreeFromMortailCoilGoal;
import ladysnake.requiem.common.entity.ai.MorticianLookAtTargetGoal;
import ladysnake.requiem.common.entity.ai.MoveBackToObeliskGoal;
import ladysnake.requiem.common.entity.ai.StealSoulGoal;
import ladysnake.requiem.common.item.FilledSoulVesselItem;
import ladysnake.requiem.common.item.RequiemItems;
import ladysnake.requiem.common.network.RequiemNetworking;
import ladysnake.requiem.common.particle.RequiemParticleTypes;
import ladysnake.requiem.common.remnant.RemnantTypes;
import ladysnake.requiem.common.sound.RequiemSoundEvents;
import ladysnake.requiem.core.record.EntityPositionClerk;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MorticianEntity extends AbstractVillager implements NeutralMob {
    public static final int MAX_LINK_DISTANCE = 20;

    @FunctionalInterface
    public interface TradeFactory {
        @Nullable MerchantOffer create(Entity entity, RandomSource random);
    }

    public static final TradeFactory[] TRADES = new TradeFactory[]{
        (entity, random) -> new RemnantTradeOffer(
            new MerchantOffer(new ItemCost(Items.GOLD_INGOT, 32), Optional.of(new ItemCost(Items.NETHERITE_INGOT)), new ItemStack(RequiemItems.SEALED_REMNANT_VESSEL), 1, 1, 0.05F),
            new MerchantOffer(new ItemCost(RequiemItems.EMPTY_SOUL_VESSEL), Optional.of(new ItemCost(Items.NETHERITE_INGOT, 1)), new ItemStack(RequiemItems.SEALED_REMNANT_VESSEL), 1, 1, 0.05F),
            true
        ),
        (entity, random) -> new MerchantOffer(new ItemCost(RequiemItems.SHATTERED_SOUL_VESSEL), Optional.of(new ItemCost(Items.GOLD_INGOT)), new ItemStack(RequiemItems.EMPTY_SOUL_VESSEL), 10, 1, 0.05F),
        (entity, random) -> {
            ItemStack bookIn = new ItemStack(Items.ENCHANTED_BOOK);
            ItemStack bookOut = new ItemStack(Items.ENCHANTED_BOOK);
            try {
                var enchReg = entity.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var humanity = enchReg.getOrThrow(RequiemEnchantments.HUMANITY);
                bookIn = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(humanity, 1));
                bookOut = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(humanity, 2));
            } catch (Exception ignored) {}
            return new MerchantOffer(new ItemCost(Items.ENCHANTED_BOOK.builtInRegistryHolder(), 1, DataComponentPredicate.allOf(bookIn.getComponents())), Optional.of(new ItemCost(Items.GOLD_INGOT, 20)), bookOut, 5, 1, 0.05F);
        },
        (entity, random) -> new MerchantOffer(new ItemCost(FilledSoulVesselItem.forEntityType(entity.level().dimension() == Level.NETHER ? EntityType.PIGLIN : EntityType.VILLAGER).getItem()), Optional.of(new ItemCost(Items.GOLD_INGOT, 5)), new ItemStack(RequiemItems.ICHOR_VESSEL_EMANCIPATION), 10, 1, 0.05F),
        (entity, random) -> new MerchantOffer(new ItemCost(FilledSoulVesselItem.forEntityType(EntityType.AXOLOTL).getItem()), Optional.of(new ItemCost(Items.GOLD_INGOT, 5)), new ItemStack(RequiemItems.ICHOR_VESSEL_RECLAMATION), 10, 1, 0.05F),
        (entity, random) -> new MerchantOffer(new ItemCost(FilledSoulVesselItem.forEntityType(EntityType.GHAST).getItem()), Optional.of(new ItemCost(Items.GOLD_INGOT, 5)), new ItemStack(RequiemItems.ICHOR_VESSEL_ATTRITION), 10, 1, 0.05F),
        (entity, random) -> new MerchantOffer(new ItemCost(FilledSoulVesselItem.forEntityType(EntityType.PILLAGER).getItem()), Optional.of(new ItemCost(Items.GOLD_INGOT, 5)), new ItemStack(RequiemItems.ICHOR_VESSEL_PENANCE), 10, 1, 0.05F)
    };

    private static final UniformInt ANGER_TIME_RANGE = UniformInt.of(20, 39);
    public static final EntityDataAccessor<Boolean> OBELISK_PROJECTION = SynchedEntityData.defineId(MorticianEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> FADING_TICKS = SynchedEntityData.defineId(MorticianEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Byte> SPELL = SynchedEntityData.defineId(MorticianEntity.class, EntityDataSerializers.BYTE);
    public static final EntityDataAccessor<Long> DATA_ANGER_END_TIME = SynchedEntityData.defineId(MorticianEntity.class, EntityDataSerializers.LONG);
    public static final int DESPAWN_DELAY = 20 * 30;

    public static AttributeSupplier.Builder createMorticianAttributes() {
        return Mob.createMobAttributes().add(RequiemEntityAttributes.SOUL_OFFENSE, 30);
    }

    private boolean checkLegacyData;
    private @Nullable UUID linkedObelisk;
    private @Nullable UUID persistentAngerTarget;
    private HurtByTargetGoal revengeGoal;
    private final List<UUID> capturedSouls = new ArrayList<>();

    public MorticianEntity(EntityType<? extends MorticianEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OBELISK_PROJECTION, false);
        builder.define(FADING_TICKS, 0);
        builder.define(SPELL, (byte) 0);
        builder.define(DATA_ANGER_END_TIME, 0L);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(1, new MorticianLookAtTargetGoal(this));
        this.goalSelector.addGoal(2, new LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(3, new StealSoulGoal(this));
        this.goalSelector.addGoal(4, new FreeFromMortailCoilGoal(this));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.35D));
        this.goalSelector.addGoal(6, new MoveBackToObeliskGoal(this, 0.35D, false));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.35D));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));

        this.revengeGoal = new HurtByTargetGoal(this);
        this.targetSelector.addGoal(1, revengeGoal);
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
    }

    public void addCapturedSoul(GlobalRecord soulRecord) {
        this.capturedSouls.add(soulRecord.getUuid());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.isSpellcasting()) {
            float theta = this.yBodyRot * (float) (Math.PI / 180.0) + Mth.cos((float) this.tickCount * 0.6662F) * 0.25F;
            float x = Mth.cos(theta);
            float z = Mth.sin(theta);
            this.level().addParticle(RequiemParticleTypes.PENANCE, this.getX() + (double) x * 0.6, this.getY() + this.getBbHeight() * 0.92, this.getZ() + (double) z * 0.6, 1, 1, 1);
            this.level().addParticle(RequiemParticleTypes.PENANCE, this.getX() - (double) x * 0.6, this.getY() + this.getBbHeight() * 0.92, this.getZ() - (double) z * 0.6, 1, 1, 1);
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target instanceof Possessable possessable && possessable.isBeingPossessed()) {
            super.setTarget(possessable.getPossessor());
        } else {
            super.setTarget(target);
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (this.level().getDifficulty() != Difficulty.PEACEFUL
            && target instanceof ServerPlayer player
            && !player.isSpectator()
            && !player.isCreative()) {
            return true;
        }
        return super.canAttack(target);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader world) {
        return world.getBlockState(pos.below()).is(BlockTags.SOUL_SPEED_BLOCKS) ? 4 : super.getWalkTargetValue(pos, world);
    }

    @Override
    public long getPersistentAngerEndTime() {
        return this.entityData.get(DATA_ANGER_END_TIME);
    }

    @Override
    public void setPersistentAngerEndTime(long endTime) {
        this.entityData.set(DATA_ANGER_END_TIME, endTime);
    }

    @Override
    public @Nullable UUID getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable UUID persistentAngerTarget) {
        this.persistentAngerTarget = persistentAngerTarget;
    }

    @Override
    public void startPersistentAngerTimer() {
        this.setPersistentAngerEndTime(this.level().getGameTime() + ANGER_TIME_RANGE.sample(this.random));
    }

    public void setSpellcasting(boolean spellcasting) {
        this.getEntityData().set(SPELL, (byte) (spellcasting ? 1 : 0));
    }

    public boolean isSpellcasting() {
        return this.getEntityData().get(SPELL) != 0;
    }

    private void setFadingTicks(int fadingTicks) {
        this.getEntityData().set(FADING_TICKS, fadingTicks);
    }

    private int getFadingTicks() {
        return this.getEntityData().get(FADING_TICKS);
    }

    public float getFadingAmount() {
        return (float) this.getFadingTicks() / DESPAWN_DELAY;
    }

    private void setObeliskProjection(boolean projection) {
        this.getEntityData().set(OBELISK_PROJECTION, projection);
    }

    public boolean isObeliskProjection() {
        return this.getEntityData().get(OBELISK_PROJECTION);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel world, AgeableMob entity) {
        return null;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel sw && this.hasInvalidLinkedObelisk()) {
            MorticianSpawner.streamSpawnableObelisks(sw.getServer())
                .filter(r -> r.get(RequiemRecordTypes.OBELISK_REF)
                    .filter(obelisk -> obelisk.dimension() == this.level().dimension())
                    .filter(obelisk -> obelisk.pos().closerThan(this.blockPosition(), MAX_LINK_DISTANCE))
                    .isPresent())
                .min(Comparator.comparing(r -> r.get(RequiemRecordTypes.OBELISK_REF).orElseThrow().pos().distSqr(this.blockPosition())))
                .ifPresentOrElse(
                    r -> {
                        this.setFadingTicks(0);
                        this.linkWith(r);
                    },
                    () -> {
                        this.setFadingTicks(this.getFadingTicks() + 1);
                        if (this.getFadingTicks() >= DESPAWN_DELAY) {
                            this.level().broadcastEntityEvent(this, EntityEvent.DEATH);
                            this.discard();
                        }
                    }
                );

            if (!this.isRemoved()) {
                this.updatePersistentAnger((ServerLevel) this.level(), true);
            }
        }
    }

    @Override
    public void stopBeingAngry() {
        NeutralMob.super.stopBeingAngry();
        if (this.revengeGoal != null) {
            this.revengeGoal.stop();
        }
    }

    private boolean hasInvalidLinkedObelisk() {
        return this.linkedObelisk != null && GlobalRecordKeeper.get(this.level()).getRecord(this.linkedObelisk).flatMap(r -> r.get(RequiemRecordTypes.RIFT_OBELISK)).isEmpty();
    }

    @Override
    public InteractionResult mobInteract(Player customer, InteractionHand hand) {
        ItemStack itemStack = customer.getItemInHand(hand);
        if (!itemStack.is(Items.VILLAGER_SPAWN_EGG) && this.isAlive() && !this.isTrading() && !this.isBaby()) {
            if (hand == InteractionHand.MAIN_HAND) {
                customer.awardStat(Stats.TALKED_TO_VILLAGER);
            }

            if (!this.level().isClientSide() && !this.getOffers().isEmpty()) {
                this.prepareOffersFor(customer);
                this.setTradingPlayer(customer);
                this.openTradingScreen(customer, this.getDisplayName(), 1);
            }

            return InteractionResult.SUCCESS;
        } else {
            return super.mobInteract(customer, hand);
        }
    }

    private void prepareOffersFor(Player customer) {
        for (MerchantOffer offer : this.getOffers()) {
            if (offer instanceof RemnantTradeOffer demonTradeOffer) {
                demonTradeOffer.setRemnant(RemnantComponent.get(customer).getRemnantType().isDemon());
                demonTradeOffer.setTempDisabled(RemnantComponent.isVagrant(customer));
            }
        }
    }

    public void linkWith(GlobalRecord r) {
        Preconditions.checkState(r.get(RequiemRecordTypes.OBELISK_REF).isPresent());
        RecordPointer thisRef = new RecordPointer(EntityPositionClerk.get(this).getOrCreateRecord());
        r.put(RequiemRecordTypes.PROJECTED_MORTICIAN, thisRef);
        this.setLinkedObelisk(r.getUuid());
    }

    private void setLinkedObelisk(UUID linkedRecordId) {
        this.linkedObelisk = linkedRecordId;
        this.setObeliskProjection(true);
    }

    @Override
    protected void updateTrades() {
        MerchantOffers offers = this.getOffers();
        for (TradeFactory factory : TRADES) {
            MerchantOffer offer = factory.create(this, this.random);
            if (offer != null) {
                offers.add(offer);
            }
        }
    }

    public @Nullable ObeliskDescriptor getHome() {
        if (this.linkedObelisk == null) return null;
        return GlobalRecordKeeper.get(this.level()).getRecord(this.linkedObelisk).flatMap(r -> r.get(RequiemRecordTypes.OBELISK_REF)).orElse(null);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag input) {
        super.readAdditionalSaveData(input);

        if (input.hasUUID("linked_obelisk")) {
            UUID uuid = input.getUUID("linked_obelisk");
            this.setLinkedObelisk(uuid);
            this.setObeliskProjection(true);
            this.checkLegacyData = true;
        } else {
            this.setObeliskProjection(false);
        }

        this.setFadingTicks(input.getInt("fading_ticks"));

        UUIDUtil.CODEC.listOf().parse(NbtOps.INSTANCE, input.get("captured_souls")).result().ifPresent(list -> {
            this.capturedSouls.clear();
            this.capturedSouls.addAll(list);
        });

        this.readPersistentAngerSaveData(this.level(), input);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag output) {
        super.addAdditionalSaveData(output);
        if (this.linkedObelisk != null) output.putUUID("linked_obelisk", this.linkedObelisk);
        output.putInt("fading_ticks", this.getFadingTicks());
        UUIDUtil.CODEC.listOf().encodeStart(NbtOps.INSTANCE, this.capturedSouls).result()
            .ifPresent(tag -> output.put("captured_souls", tag));
        this.addPersistentAngerSaveData(output);
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        if (offer instanceof RemnantTradeOffer demonTradeOffer && demonTradeOffer.demonCustomer && this.getTradingPlayer() instanceof ServerPlayer player) {
            RemnantComponent.get(player).become(RemnantTypes.MORTAL, true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), RequiemSoundEvents.ITEM_OPUS_USE, player.getSoundSource(), 1.4F, 0.1F);
            RequiemNetworking.sendTo(player, RequiemNetworking.createOpusUsePacket(RemnantTypes.MORTAL, false));
        }
        if (offer.shouldRewardExp()) {
            int i = 3 + this.random.nextInt(4);
            this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5D, this.getZ(), i));
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!this.level().isClientSide() && this.isDeadOrDying()) {
            for (UUID capturedSoul : this.capturedSouls) {
                FilledSoulVesselItem.releaseSoul(this, capturedSoul);
            }
        }

        if (this.getTradingPlayer() instanceof ServerPlayer sp && sp.containerMenu instanceof MerchantMenu) {
            sp.closeContainer();
        }

        super.remove(reason);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isTrading() ? RequiemSoundEvents.ENTITY_MORTICIAN_TRADE : RequiemSoundEvents.ENTITY_MORTICIAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return RequiemSoundEvents.ENTITY_MORTICIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return RequiemSoundEvents.ENTITY_MORTICIAN_DEATH;
    }

    @Override
    protected SoundEvent getTradeUpdatedSound(boolean sold) {
        return sold ? RequiemSoundEvents.ENTITY_MORTICIAN_YES : RequiemSoundEvents.ENTITY_MORTICIAN_NO;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return RequiemSoundEvents.ENTITY_MORTICIAN_YES;
    }

    public SoundEvent getCastSpellSound() {
        return RequiemSoundEvents.ENTITY_MORTICIAN_CAST_SPELL;
    }
}

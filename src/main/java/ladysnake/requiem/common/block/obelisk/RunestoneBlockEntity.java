package ladysnake.requiem.common.block.obelisk;

import com.google.common.base.Preconditions;
import com.mojang.datafixers.util.Unit;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import ladysnake.requiem.api.v1.block.ObeliskDescriptor;
import ladysnake.requiem.api.v1.block.ObeliskRune;
import ladysnake.requiem.api.v1.record.GlobalRecord;
import ladysnake.requiem.api.v1.record.GlobalRecordKeeper;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.common.RequiemRecordTypes;
import ladysnake.requiem.common.block.RequiemBlockEntities;
import ladysnake.requiem.common.block.RequiemBlocks;
import ladysnake.requiem.common.network.RequiemNetworking;
import ladysnake.requiem.common.particle.RequiemParticleTypes;
import ladysnake.requiem.common.sound.RequiemSoundEvents;
import ladysnake.requiem.core.util.NbtCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RunestoneBlockEntity extends BaseRunestoneBlockEntity {
    public static final int POWER_ATTEMPTS = 6;

    final Object2IntMap<ObeliskRune> levels = new Object2IntOpenHashMap<>();
    private @Nullable UUID recordUuid;
    private int obeliskCoreWidth = 0;
    private int obeliskCoreHeight = 0;

    public RunestoneBlockEntity(BlockPos pos, BlockState state) {
        super(RequiemBlockEntities.RUNIC_OBSIDIAN, pos, state);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, RunestoneBlockEntity be) {
        assert !world.isClientSide();

        if ((world.getGameTime() + pos.hashCode()) % 80L == 0L) {
            be.update((ServerLevel) world);
        }
    }

    @VisibleForTesting
    void update(ServerLevel world) {
        BlockState state = this.getBlockState();
        if (!state.getValue(InertRunestoneBlock.HEAD)) {
            world.removeBlockEntity(this.worldPosition);
            this.onDestroyed();
            return;
        }

        this.refreshStructure(state);
        this.updatePower(world);

        if (!this.levels.isEmpty() && this.isPowered()) {
            this.applyPlayerEffects(world, this.worldPosition);
            world.playSound(null, this.worldPosition, RequiemSoundEvents.BLOCK_OBELISK_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.4F);
        }
    }

    private static int getRange(int obeliskWidth) {
        return obeliskWidth * 5;
    }

    private static Vec3 getObeliskCenter(BlockPos origin, int coreWidth) {
        return new Vec3(
            Mth.lerp(0.5, origin.getX(), origin.getX() + coreWidth - 1),
            origin.getY() - 2,
            Mth.lerp(0.5, origin.getZ(), origin.getZ() + coreWidth - 1)
        );
    }

    public static void updateObeliskPower(Level world, BlockPos controllerPos, int coreWidth, int coreHeight, float powerRate) {
        BlockPos pos2 = controllerPos.offset(coreWidth - 1, coreHeight - 1, coreWidth - 1);
        for (BlockPos pos : BlockPos.betweenClosed(controllerPos, pos2)) {
            if (world.getBlockEntity(pos) instanceof BaseRunestoneBlockEntity runestone) {
                runestone.setPowerRate(powerRate);
            }
        }
    }

    private void applyPlayerEffects(Level world, BlockPos pos) {
        Preconditions.checkState(this.obeliskCoreWidth > 0);
        Preconditions.checkState(this.obeliskCoreHeight > 0);

        double range = this.obeliskCoreWidth * 10 + 10;
        AABB box = AABB.encapsulatingFullBlocks(pos, pos.offset(this.obeliskCoreWidth - 1, obeliskCoreHeight - 1, this.obeliskCoreWidth - 1)).inflate(range);

        List<Player> players = world.getEntitiesOfClass(Player.class, box, EntitySelector.NO_SPECTATORS);

        for (Player player : players) {
            if (RemnantComponent.get(player).getRemnantType().isDemon()) {
                for (Object2IntMap.Entry<ObeliskRune> effect : this.levels.object2IntEntrySet()) {
                    effect.getKey().applyEffect((ServerPlayer) player, effect.getIntValue(), this.obeliskCoreWidth);
                }
            }
        }
    }

    private void updatePower(ServerLevel world) {
        Vec3 center = getObeliskCenter(this.worldPosition, this.obeliskCoreWidth);
        double range = getRange(this.obeliskCoreWidth);
        BlockPos.MutableBlockPos checked = new BlockPos.MutableBlockPos();
        int successes = 0;

        for (int attempt = 0; attempt < RunestoneBlockEntity.POWER_ATTEMPTS; attempt++) {
            double x = center.x + world.getRandom().nextDouble() * range * 2 - range;
            double z = center.z + world.getRandom().nextDouble() * range * 2 - range;
            checked.set(Math.round(x), center.y, Math.round(z));
            BlockState state = world.getBlockState(checked);

            while (!state.blocksMotion()) {
                checked.move(Direction.DOWN);
                state = world.getBlockState(checked);
            }

            if (state.is(BlockTags.SOUL_SPEED_BLOCKS)) {
                spawnSoul(world, center, Vec3.atCenterOf(checked));
                successes++;
            }
        }

        updateObeliskPower(world, this.getBlockPos(), this.obeliskCoreWidth, this.obeliskCoreHeight, (float) successes / POWER_ATTEMPTS);
        RequiemNetworking.sendObeliskPowerUpdateMessage(this);
    }

    private static void spawnSoul(ServerLevel world, Vec3 center, Vec3 particleSrc) {
        Vec3 toObelisk = center.subtract(particleSrc).normalize();
        world.sendParticles(RequiemParticleTypes.OBELISK_SOUL, particleSrc.x, particleSrc.y, particleSrc.z, 0, toObelisk.x, 1, toObelisk.z, 0.1);
    }

    public int getCoreWidth() {
        return this.obeliskCoreWidth;
    }

    public int getCoreHeight() {
        return this.obeliskCoreHeight;
    }

    public Optional<GlobalRecord> getDescriptorRecord() {
        if (this.level == null) return Optional.empty();
        return Optional.ofNullable(recordUuid).flatMap(GlobalRecordKeeper.get(this.level)::getRecord);
    }

    public Optional<ObeliskDescriptor> getDescriptor() {
        if (this.level != null && this.recordUuid != null) {
            return this.getDescriptorRecord().flatMap(r -> r.get(RequiemRecordTypes.OBELISK_REF));
        }

        return Optional.empty();
    }

    private void refreshStructure(BlockState state) {
        assert this.level != null;
        this.levels.clear();
        this.obeliskCoreWidth = 0;
        this.obeliskCoreHeight = 0;

        ObeliskMatcher.matchObelisk(this.level, this.worldPosition).result()
            .ifPresentOrElse(
                match -> {
                    Object2IntMap<ObeliskRune> runes = match.collectRunes();
                    this.obeliskCoreWidth = match.coreWidth();
                    this.obeliskCoreHeight = match.coreHeight();
                    this.levels.putAll(runes);
                    Optional<Component> customName = match.names().stream().unordered().findAny();
                    if (this.recordUuid == null && runes.containsKey(RequiemBlocks.RIFT_RUNE)) {
                        GlobalRecordKeeper.get(this.level).stream()
                            .filter(r -> r.get(RequiemRecordTypes.OBELISK_REF)
                                .map(ObeliskDescriptor::pos)
                                .filter(this.worldPosition::equals).isPresent())
                            .forEach(GlobalRecord::invalidate);
                        GlobalRecord record = GlobalRecordKeeper.get(this.level).createRecord();
                        record.put(RequiemRecordTypes.OBELISK_REF, new ObeliskDescriptor(
                            this.level.dimension(),
                            this.getBlockPos(),
                            this.obeliskCoreWidth,
                            this.obeliskCoreHeight,
                            customName.or(this::generateName)
                        ));
                        record.put(RequiemRecordTypes.RIFT_OBELISK, Unit.INSTANCE);
                        this.recordUuid = record.getUuid();
                    }
                },
                () -> this.level.scheduleTick(this.worldPosition, state.getBlock(), 0)
            );
    }

    public void initialize(ObeliskMatch match) {
        this.refreshStructure(this.getBlockState());
    }

    private Optional<Component> generateName() {
        return Optional.empty();
    }

    public boolean canBeUsedBy(Player player) {
        if (player.level().getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return player.distanceToSqr(Vec3.atCenterOf(this.worldPosition)) <= 64.0;
        }
    }

    public void onDestroyed() {
        if (this.recordUuid != null && this.getLevel() != null) {
            GlobalRecordKeeper.get(this.getLevel()).getRecord(this.recordUuid).ifPresent(GlobalRecord::invalidate);
            this.recordUuid = null;
        }

        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, RequiemSoundEvents.BLOCK_OBELISK_DEACTIVATE, SoundSource.BLOCKS, 1, 0.3f);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag input, HolderLookup.Provider registries) {
        super.loadAdditional(input, registries);
        if (input.hasUUID("linked_record")) this.recordUuid = input.getUUID("linked_record");
    }

    @Override
    protected void saveAdditional(CompoundTag output, HolderLookup.Provider registries) {
        super.saveAdditional(output, registries);
        if (this.recordUuid != null) output.putUUID("linked_record", this.recordUuid);
    }
}

package ladysnake.requiem.common.block.obelisk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public abstract class BaseRunestoneBlockEntity extends BlockEntity {
    private static final int POWER_TRANSITION_TIME = 10;
    @Nullable
    protected Component customName;
    private long lastPowerUpdateTime;
    private float previousPowerRate;
    private float powerRate;

    public BaseRunestoneBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    public void setCustomName(@Nullable Component customName) {
        this.customName = customName;
    }

    public Optional<Component> getCustomName() {
        return Optional.ofNullable(this.customName);
    }

    public float getPowerRate(float tickDelta) {
        assert level != null;
        return Mth.lerp(Mth.clamp(((level.getGameTime() - lastPowerUpdateTime) + tickDelta) / POWER_TRANSITION_TIME, 0f, 1f), previousPowerRate, powerRate);
    }

    public float getPowerRate() {
        return powerRate;
    }

    public boolean isPowered() {
        return this.powerRate > 0.5f;
    }

    public void setPowerRate(float powerRate) {
        this.previousPowerRate = this.powerRate;
        this.powerRate = powerRate;
        this.lastPowerUpdateTime = this.getLevel() == null ? 0 : this.getLevel().getGameTime();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("custom_name", ComponentSerialization.CODEC).ifPresent(c -> this.customName = c);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("custom_name", ComponentSerialization.CODEC, this.customName);
    }
}

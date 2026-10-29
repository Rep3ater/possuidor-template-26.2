package ladysnake.requiem.common.construct;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class ConstructVoxel {
    private final BlockPos relPos;
    private final BlockState state;
    private final ConstructPart part;
    private final ActuatorType actuator;
    private final Direction facing;
    private float health;
    private final float maxHealth;
    private final List<ItemStack> inventory = new ArrayList<>();

    public ConstructVoxel(BlockPos relPos, BlockState state, ConstructPart part, ActuatorType actuator, Direction facing, float maxHealth) {
        this(relPos, state, part, actuator, facing, maxHealth, maxHealth);
    }

    public ConstructVoxel(BlockPos relPos, BlockState state, ConstructPart part, ActuatorType actuator, Direction facing, float health, float maxHealth) {
        this.relPos = relPos;
        this.state = state;
        this.part = part;
        this.actuator = actuator;
        this.facing = facing != null ? facing : Direction.NORTH;
        this.maxHealth = Math.max(1.0f, maxHealth);
        this.health = Math.min(this.maxHealth, Math.max(0.0f, health));
    }

    public BlockPos getRelPos() {
        return relPos;
    }

    public BlockState getState() {
        return state;
    }

    public ConstructPart getPart() {
        return part;
    }

    public ActuatorType getActuator() {
        return actuator;
    }

    public Direction getFacing() {
        return facing;
    }

    public float getHealth() {
        return health;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public boolean isDestroyed() {
        return health <= 0.0f;
    }

    public float damage(float amount) {
        float applied = Math.min(this.health, amount);
        this.health -= applied;
        return applied;
    }

    public List<ItemStack> getInventory() {
        return inventory;
    }

    public void setInventory(List<ItemStack> items) {
        this.inventory.clear();
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                this.inventory.add(item.copy());
            }
        }
    }

    public ItemStack popItem() {
        for (int i = 0; i < this.inventory.size(); i++) {
            ItemStack stack = this.inventory.get(i);
            if (!stack.isEmpty()) {
                ItemStack single = stack.split(1);
                if (stack.isEmpty()) {
                    this.inventory.remove(i);
                }
                return single;
            }
        }
        return ItemStack.EMPTY;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("rx", relPos.getX());
        tag.putInt("ry", relPos.getY());
        tag.putInt("rz", relPos.getZ());
        tag.putInt("stateId", Block.getId(state));
        tag.putByte("part", (byte) part.ordinal());
        tag.putByte("actuator", (byte) actuator.ordinal());
        tag.putByte("facing", (byte) facing.ordinal());
        tag.putFloat("hp", health);
        tag.putFloat("maxHp", maxHealth);

        if (!this.inventory.isEmpty()) {
            ListTag invTag = new ListTag();
            for (ItemStack stack : this.inventory) {
                if (!stack.isEmpty()) {
                    CompoundTag itemTag = new CompoundTag();
                    itemTag.putString("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                    itemTag.putInt("count", stack.getCount());
                    invTag.add(itemTag);
                }
            }
            tag.put("inv", invTag);
        }

        return tag;
    }

    public static ConstructVoxel fromNbt(CompoundTag tag) {
        int rx = tag.getInt("rx").orElse(0);
        int ry = tag.getInt("ry").orElse(0);
        int rz = tag.getInt("rz").orElse(0);
        BlockPos pos = new BlockPos(rx, ry, rz);
        BlockState state = Block.stateById(tag.getInt("stateId").orElse(0));
        if (state.isAir()) {
            state = Blocks.IRON_BLOCK.defaultBlockState();
        }
        ConstructPart part = ConstructPart.fromOrdinal(tag.getByte("part").orElse((byte) 0));
        ActuatorType actuator = ActuatorType.fromOrdinal(tag.getByte("actuator").orElse((byte) 0));
        Direction facing = Direction.from3DDataValue(tag.getByte("facing").orElse((byte) 2));
        float hp = tag.getFloat("hp").orElse(2.0f);
        float maxHp = tag.getFloat("maxHp").orElse(Math.max(hp, 2.0f));

        ConstructVoxel voxel = new ConstructVoxel(pos, state, part, actuator, facing, hp, maxHp);
        tag.getList("inv").ifPresent(listTag -> {
            for (int i = 0; i < listTag.size(); i++) {
                if (listTag.get(i) instanceof CompoundTag itemTag) {
                    String id = itemTag.getString("id").orElse("");
                    int count = itemTag.getInt("count").orElse(1);
                    Item item = BuiltInRegistries.ITEM.get(Identifier.tryParse(id)).map(net.minecraft.core.Holder::value).orElse(Items.AIR);
                    if (item != Items.AIR) {
                        voxel.inventory.add(new ItemStack(item, count));
                    }
                }
            }
        });
        return voxel;
    }

    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeBlockPos(relPos);
        buf.writeVarInt(Block.getId(state));
        buf.writeByte(part.ordinal());
        buf.writeByte(actuator.ordinal());
        buf.writeByte(facing.ordinal());
        buf.writeFloat(health);
        buf.writeFloat(maxHealth);
        buf.writeVarInt(inventory.size());
        for (ItemStack stack : inventory) {
            buf.writeVarInt(BuiltInRegistries.ITEM.getId(stack.getItem()));
            buf.writeVarInt(stack.getCount());
        }
    }

    public static ConstructVoxel readFromBuf(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockState state = Block.stateById(buf.readVarInt());
        ConstructPart part = ConstructPart.fromOrdinal(buf.readByte());
        ActuatorType actuator = ActuatorType.fromOrdinal(buf.readByte());
        Direction facing = Direction.from3DDataValue(buf.readByte());
        float hp = buf.readFloat();
        float maxHp = buf.readFloat();
        ConstructVoxel voxel = new ConstructVoxel(pos, state, part, actuator, facing, hp, maxHp);
        int invSize = buf.readVarInt();
        for (int i = 0; i < invSize; i++) {
            Item item = BuiltInRegistries.ITEM.byId(buf.readVarInt());
            int count = buf.readVarInt();
            if (item != null && item != Items.AIR) {
                voxel.inventory.add(new ItemStack(item, count));
            }
        }
        return voxel;
    }
}

package ladysnake.requiem.common.construct;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class ConstructVoxelModel {
    private final Map<BlockPos, ConstructVoxel> voxels = new LinkedHashMap<>();
    private float scale = 1.0f;

    public ConstructVoxelModel() {}

    public ConstructVoxelModel(Collection<ConstructVoxel> voxelCollection) {
        for (ConstructVoxel voxel : voxelCollection) {
            voxels.put(voxel.getRelPos(), voxel);
        }
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = Math.max(0.01f, scale);
    }

    public boolean isMiniature() {
        return scale < 0.5f;
    }

    public void addVoxel(ConstructVoxel voxel) {
        voxels.put(voxel.getRelPos(), voxel);
    }

    public void updateVoxelBlockState(BlockPos relPos, net.minecraft.world.level.block.state.BlockState newState) {
        ConstructVoxel old = voxels.get(relPos);
        if (old != null) {
            ConstructVoxel updated = new ConstructVoxel(old.getRelPos(), newState, old.getPart(), old.getActuator(), old.getFacing(), old.getHealth(), old.getMaxHealth());
            updated.setInventory(old.getInventory());
            voxels.put(relPos, updated);
        }
    }

    public int getNonDestroyedCount() {
        int count = 0;
        for (ConstructVoxel v : voxels.values()) {
            if (!v.isDestroyed()) count++;
        }
        return count;
    }

    public Map<BlockPos, ConstructVoxel> getVoxels() {
        return Collections.unmodifiableMap(voxels);
    }

    public int getVoxelCount() {
        return voxels.size();
    }

    public boolean isEmpty() {
        return voxels.isEmpty();
    }

    public ConstructVoxel getVoxel(BlockPos relPos) {
        return voxels.get(relPos);
    }

    public float getTotalHealth() {
        float sum = 0.0f;
        for (ConstructVoxel v : voxels.values()) {
            sum += v.getHealth();
        }
        return sum;
    }

    public float getMaxTotalHealth() {
        float sum = 0.0f;
        for (ConstructVoxel v : voxels.values()) {
            sum += v.getMaxHealth();
        }
        return sum;
    }

    public float getGroundOffset() {
        if (voxels.isEmpty()) return 0.0f;
        int minY = Integer.MAX_VALUE;
        for (BlockPos pos : voxels.keySet()) {
            minY = Math.min(minY, pos.getY());
        }
        return -minY;
    }

    public boolean hasIntactLegs() {
        boolean leftLeg = false;
        boolean rightLeg = false;
        for (ConstructVoxel v : voxels.values()) {
            if (!v.isDestroyed()) {
                if (v.getPart() == ConstructPart.LEFT_LEG) leftLeg = true;
                if (v.getPart() == ConstructPart.RIGHT_LEG) rightLeg = true;
            }
        }
        return leftLeg || rightLeg;
    }

    public boolean hasBothLegs() {
        boolean leftLeg = false;
        boolean rightLeg = false;
        for (ConstructVoxel v : voxels.values()) {
            if (!v.isDestroyed()) {
                if (v.getPart() == ConstructPart.LEFT_LEG) leftLeg = true;
                if (v.getPart() == ConstructPart.RIGHT_LEG) rightLeg = true;
            }
        }
        return leftLeg && rightLeg;
    }

    public boolean hasIntactArm(boolean left) {
        ConstructPart target = left ? ConstructPart.LEFT_ARM : ConstructPart.RIGHT_ARM;
        for (ConstructVoxel v : voxels.values()) {
            if (!v.isDestroyed() && v.getPart() == target) {
                return true;
            }
        }
        return false;
    }

    public boolean hasActuator(ActuatorType type) {
        for (ConstructVoxel v : voxels.values()) {
            if (!v.isDestroyed() && v.getActuator() == type) {
                return true;
            }
        }
        return false;
    }

    public List<ConstructVoxel> getActuatorVoxels(ActuatorType type) {
        List<ConstructVoxel> list = new ArrayList<>();
        for (ConstructVoxel v : voxels.values()) {
            if (!v.isDestroyed() && v.getActuator() == type) {
                list.add(v);
            }
        }
        return list;
    }

    public boolean hasArmActuator(ActuatorType type) {
        for (ConstructVoxel v : voxels.values()) {
            if (!v.isDestroyed() && v.getActuator() == type &&
               (v.getPart() == ConstructPart.LEFT_ARM || v.getPart() == ConstructPart.RIGHT_ARM)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasLegActuator(ActuatorType type) {
        for (ConstructVoxel v : voxels.values()) {
            if (!v.isDestroyed() && v.getActuator() == type &&
               (v.getPart() == ConstructPart.LEFT_LEG || v.getPart() == ConstructPart.RIGHT_LEG)) {
                return true;
            }
        }
        return false;
    }

    public double getCalculatedMovementSpeed() {
        if (voxels.isEmpty()) return 0.25;
        double base = isMiniature() ? 0.32 : 0.28;
        if (!hasIntactLegs()) {
            return base * 0.3;
        }
        if (!hasBothLegs()) {
            return base * 0.6;
        }
        return base;
    }

    public double getCalculatedArmor() {
        float hardnessSum = 0.0f;
        for (ConstructVoxel v : voxels.values()) {
            if (!v.isDestroyed()) {
                hardnessSum += Math.min(20.0f, v.getState().getDestroySpeed(null, null));
            }
        }
        return Math.min(30.0, 4.0 + (hardnessSum * 0.15));
    }

    public double getCalculatedAttackDamage() {
        double damage = isMiniature() ? 2.5 : 4.0;
        if (hasArmActuator(ActuatorType.PISTON_THRUST)) {
            damage += isMiniature() ? 5.0 : 8.0;
        } else if (hasIntactArm(true) || hasIntactArm(false)) {
            damage += 3.0;
        }
        return damage;
    }

    public AABB calculateBoundingBox() {
        if (voxels.isEmpty()) {
            return new AABB(-0.5 * scale, 0, -0.5 * scale, 0.5 * scale, 1.8 * scale, 0.5 * scale);
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : voxels.keySet()) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        double widthX = (maxX - minX + 1.0) * scale;
        double widthZ = (maxZ - minZ + 1.0) * scale;
        double width = Math.max(0.4, Math.max(widthX, widthZ) * 0.85);
        double height = Math.max(0.6, (maxY - minY + 1.0) * scale);

        return new AABB(-width / 2.0, 0, -width / 2.0, width / 2.0, height, width / 2.0);
    }

    public ConstructVoxel damageClosestVoxel(Vec3 hitDir, float damageAmount) {
        if (voxels.isEmpty()) return null;

        ConstructVoxel best = null;
        double bestDot = -Double.MAX_VALUE;

        for (ConstructVoxel v : voxels.values()) {
            if (v.isDestroyed()) continue;
            BlockPos p = v.getRelPos();
            double dot = (p.getX() * hitDir.x) + (p.getY() * hitDir.y) + (p.getZ() * hitDir.z);
            if (dot > bestDot) {
                bestDot = dot;
                best = v;
            }
        }

        if (best == null) {
            for (ConstructVoxel v : voxels.values()) {
                if (!v.isDestroyed()) {
                    best = v;
                    break;
                }
            }
        }

        if (best != null) {
            best.damage(damageAmount);
            if (best.isDestroyed()) {
                voxels.remove(best.getRelPos());
            }
        }
        return best;
    }

    public List<ConstructVoxel> checkConnectivityAndSever() {
        if (voxels.isEmpty()) return Collections.emptyList();

        BlockPos rootPos = BlockPos.ZERO;
        if (!voxels.containsKey(rootPos)) {
            for (ConstructVoxel v : voxels.values()) {
                if (v.getPart() == ConstructPart.TORSO && !v.isDestroyed()) {
                    rootPos = v.getRelPos();
                    break;
                }
            }
        }

        if (!voxels.containsKey(rootPos)) {
            List<ConstructVoxel> severed = new ArrayList<>(voxels.values());
            voxels.clear();
            return severed;
        }

        Set<BlockPos> connected = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(rootPos);
        connected.add(rootPos);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (voxels.containsKey(neighbor) && !connected.contains(neighbor)) {
                    connected.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        List<ConstructVoxel> severed = new ArrayList<>();
        Iterator<Map.Entry<BlockPos, ConstructVoxel>> iterator = voxels.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, ConstructVoxel> entry = iterator.next();
            if (!connected.contains(entry.getKey())) {
                severed.add(entry.getValue());
                iterator.remove();
            }
        }

        return severed;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (ConstructVoxel voxel : voxels.values()) {
            list.add(voxel.toNbt());
        }
        tag.put("voxels", list);
        tag.putFloat("scale", scale);
        return tag;
    }

    public static ConstructVoxelModel fromNbt(CompoundTag tag) {
        ConstructVoxelModel model = new ConstructVoxelModel();
        if (tag != null) {
            model.setScale(tag.getFloat("scale"));
            ListTag list = tag.getList("voxels", net.minecraft.nbt.Tag.TAG_COMPOUND);
            {
                for (int i = 0; i < list.size(); i++) {
                    model.addVoxel(ConstructVoxel.fromNbt(list.getCompound(i)));
                }
            });
        }
        return model;
    }

    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeFloat(scale);
        buf.writeVarInt(voxels.size());
        for (ConstructVoxel voxel : voxels.values()) {
            voxel.writeToBuf(buf);
        }
    }

    public static ConstructVoxelModel readFromBuf(FriendlyByteBuf buf) {
        ConstructVoxelModel model = new ConstructVoxelModel();
        model.setScale(buf.readFloat());
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            model.addVoxel(ConstructVoxel.readFromBuf(buf));
        }
        return model;
    }
}

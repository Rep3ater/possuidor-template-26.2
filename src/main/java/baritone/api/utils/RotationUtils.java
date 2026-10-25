package baritone.api.utils;

import net.minecraft.world.phys.Vec3;

public class RotationUtils {
    public static float[] calcRotation(Vec3 from, Vec3 to) {
        double diffX = to.x - from.x;
        double diffY = to.y - from.y;
        double diffZ = to.z - from.z;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(diffY, dist) * 180.0 / Math.PI);
        return new float[]{yaw, pitch};
    }

    public static float[] calcRotationFromVec3d(Vec3 from, Vec3 to) {
        return calcRotation(from, to);
    }
}
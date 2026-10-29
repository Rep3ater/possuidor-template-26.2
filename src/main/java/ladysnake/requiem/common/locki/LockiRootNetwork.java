package ladysnake.requiem.common.locki;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministic, virtual root paths shared by the server movement code and client visuals. */
public final class LockiRootNetwork {
    public static final int DEFAULT_RADIUS = 18;
    private LockiRootNetwork() {}

    public record PlantNode(BlockPos pos, boolean tree) {
        public Vec3 anchor() {
            // The base of a tree log and the plant block are both at the soil's surface height.
            return new Vec3(pos.getX() + 0.5, pos.getY() + 0.035, pos.getZ() + 0.5);
        }
    }

    public record Segment(Vec3 start, Vec3 end, PlantNode first, PlantNode second) {
        /** Projects to the nearest point in 3D, so vertical roots are real movement paths. */
        public Vec3 project(double x, double y, double z) {
            double dx = end.x - start.x;
            double dy = end.y - start.y;
            double dz = end.z - start.z;
            double denom = dx * dx + dy * dy + dz * dz;
            double t = denom < 1.0E-8 ? 0.0
                : ((x - start.x) * dx + (y - start.y) * dy + (z - start.z) * dz) / denom;
            t = Math.max(0.0, Math.min(1.0, t));
            return new Vec3(start.x + dx * t, start.y + dy * t, start.z + dz * t);
        }

        /** Backwards-compatible surface projection for visual ranking only. */
        public Vec3 project(double x, double z) {
            double dx = end.x - start.x;
            double dz = end.z - start.z;
            double denom = dx * dx + dz * dz;
            double t = denom < 1.0E-8 ? 0.0 : ((x - start.x) * dx + (z - start.z) * dz) / denom;
            t = Math.max(0.0, Math.min(1.0, t));
            return new Vec3(start.x + dx * t, start.y + (end.y - start.y) * t, start.z + dz * t);
        }

        public double distanceSquared(double x, double y, double z) {
            Vec3 p = project(x, y, z);
            double dx = x - p.x;
            double dy = y - p.y;
            double dz = z - p.z;
            return dx * dx + dy * dy + dz * dz;
        }

        public double distanceSquared(double x, double z) {
            Vec3 p = project(x, z);
            double dx = x - p.x;
            double dz = z - p.z;
            return dx * dx + dz * dz;
        }
    }

    public static List<PlantNode> findPlants(Level level, BlockPos center, int radius) {
        Map<Long, PlantNode> found = new HashMap<>();
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - 24);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + 8);
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.hasChunkAt(pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    if (state.is(BlockTags.LOGS)) {
                        if (!level.getBlockState(pos.below()).is(BlockTags.LOGS)) {
                            // Deduplicate 2x2 trunk bases and nearby same-tree trunk blocks.
                            boolean duplicate = false;
                            for (int ox = -1; ox <= 1 && !duplicate; ox++) {
                                for (int oz = -1; oz <= 1; oz++) {
                                    PlantNode other = found.get(new BlockPos(x + ox, y, z + oz).asLong());
                                    if (other != null && other.tree()) { duplicate = true; break; }
                                }
                            }
                            if (!duplicate) found.put(pos.asLong(), new PlantNode(pos.immutable(), true));
                        }
                    } else if (state.getBlock() instanceof CropBlock || state.getBlock() instanceof BushBlock) {
                        found.put(pos.asLong(), new PlantNode(pos.immutable(), false));
                    }
                }
            }
        }
        List<PlantNode> result = new ArrayList<>(found.values());
        // Dense crop farms can contain thousands of candidates. Keeping the nearest 160 makes
        // graph construction bounded without changing which plants are prioritized for Locki.
        result.sort(Comparator.comparingDouble((PlantNode n) -> n.anchor().distanceToSqr(Vec3.atCenterOf(center)))
            .thenComparingInt(n -> n.pos().getX()).thenComparingInt(n -> n.pos().getZ()).thenComparingInt(n -> n.pos().getY()));
        if (result.size() > 160) result = new ArrayList<>(result.subList(0, 160));
        return result;
    }

    public static List<Segment> build(Level level, BlockPos center, int radius) {
        return build(level, findPlants(level, center, radius), radius);
    }

    /** Build with the live level so every root follows the terrain surface, not underground Y offsets. */
    public static List<Segment> build(Level level, List<PlantNode> nodes, int radius) {
        return buildInternal(nodes, radius, (x, z, fallbackY) -> surfaceY(level, x, z, fallbackY));
    }

    /** Backwards-compatible builder. Its interpolated paths also stay on the plant surface plane. */
    public static List<Segment> build(List<PlantNode> nodes, int radius) {
        return buildInternal(nodes, radius, (x, z, fallbackY) -> fallbackY);
    }

    @FunctionalInterface
    private interface SurfaceLookup {
        double y(double x, double z, double fallbackY);
    }

    private static double surfaceY(Level level, double x, double z, double fallbackY) {
        if (level == null) return fallbackY;
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int referenceY = Math.max(level.getMinBuildHeight(), Math.min(level.getMaxBuildHeight() - 1, (int) Math.floor(fallbackY)));
        BlockPos probe = new BlockPos(blockX, referenceY, blockZ);
        if (!level.hasChunkAt(probe)) return fallbackY;

        // Do not use a heightmap here: the heightmap can select a high tree trunk at the same
        // X/Z column, and the old anchor offset placed roots inside terrain on some blocks.
        // Scan down from just above the interpolated point, ignore vegetation and logs, then
        // anchor one block above the first sturdy terrain block. The same lookup powers both
        // traversal and rendering, so the visible roots and the walkable route agree.
        int startY = Math.min(level.getMaxBuildHeight() - 1, Math.max(referenceY + 3,
            level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ)));
        for (int y = startY; y >= Math.max(level.getMinBuildHeight(), referenceY - 32); y--) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)
                    || state.getBlock() instanceof BushBlock || state.getBlock() instanceof CropBlock
                    || state.getCollisionShape(level, pos).isEmpty()) {
                continue;
            }
            if (state.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP)) {
                return Math.max(level.getMinBuildHeight() + 0.05, Math.min(level.getMaxBuildHeight() - 1.0, y + 1.035));
            }
        }
        // Safe fallback for liquids or void terrain where no sturdy surface was found.
        return Math.max(level.getMinBuildHeight() + 0.05, Math.min(level.getMaxBuildHeight() - 1.0, fallbackY));
    }

    private static List<Segment> buildInternal(List<PlantNode> nodes, int radius, SurfaceLookup surface) {
        if (nodes.size() < 2) return List.of();

        record Edge(int a, int b, double distance) {}
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            Vec3 a = nodes.get(i).anchor();
            for (int j = i + 1; j < nodes.size(); j++) {
                Vec3 b = nodes.get(j).anchor();
                double dx = a.x - b.x;
                double dz = a.z - b.z;
                edges.add(new Edge(i, j, dx * dx + dz * dz));
            }
        }
        edges.sort(Comparator.comparingDouble(Edge::distance));
        int[] parent = new int[nodes.size()];
        for (int i = 0; i < parent.length; i++) parent[i] = i;
        Set<Long> selected = new HashSet<>();
        List<Edge> chosen = new ArrayList<>();
        for (Edge edge : edges) {
            int ra = root(parent, edge.a());
            int rb = root(parent, edge.b());
            if (ra != rb) {
                parent[ra] = rb;
                chosen.add(edge);
                selected.add((((long) edge.a()) << 32) | (edge.b() & 0xffffffffL));
            }
        }
        int[] extraCount = new int[nodes.size()];
        for (Edge edge : edges) {
            if (edge.distance() > 9.0 * 9.0) continue;
            long key = (((long) edge.a()) << 32) | (edge.b() & 0xffffffffL);
            if (selected.contains(key) || extraCount[edge.a()] >= 2 || extraCount[edge.b()] >= 2) continue;
            chosen.add(edge);
            selected.add(key);
            extraCount[edge.a()]++;
            extraCount[edge.b()]++;
        }

        // Route every branch in short surface-following segments. This shared graph drives both
        // server movement and client rendering, so neither can produce buried or floating roots.
        List<Segment> segments = new ArrayList<>();
        for (Edge edge : chosen) {
            PlantNode a = nodes.get(edge.a());
            PlantNode b = nodes.get(edge.b());
            Vec3 aPos = a.anchor();
            Vec3 bPos = b.anchor();
            double dx = bPos.x - aPos.x;
            double dz = bPos.z - aPos.z;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            int samples = Math.max(1, Math.min(64, (int) Math.ceil(horizontal / 0.85)));
            Vec3 previous = new Vec3(aPos.x, surface.y(aPos.x, aPos.z, aPos.y), aPos.z);
            Vec3 branchOrigin = null;
            for (int i = 1; i <= samples; i++) {
                double t = i / (double) samples;
                double x = aPos.x + dx * t;
                double z = aPos.z + dz * t;
                double interpolatedY = aPos.y + (bPos.y - aPos.y) * t;
                Vec3 current = new Vec3(x, surface.y(x, z, interpolatedY), z);
                segments.add(new Segment(previous, current, a, b));
                if (branchOrigin == null && horizontal > 4.0 && t >= 0.36) branchOrigin = current;
                previous = current;
            }

            // Short lateral rootlets branch on the surface too; the old implementation sent
            // their endpoints several blocks down into the dirt, which made most roots invisible.
            if (branchOrigin != null) {
                double sign = ((a.pos().getX() * 31L + b.pos().getZ() * 17L) & 1L) == 0L ? 1.0 : -1.0;
                double sideX = horizontal < 1.0E-4 ? sign : -dz / horizontal * sign;
                double sideZ = horizontal < 1.0E-4 ? 0.0 : dx / horizontal * sign;
                double length = 1.0 + (Math.abs(a.pos().getX() * 13L + b.pos().getZ() * 7L) % 12L) * 0.12;
                double endX = branchOrigin.x + sideX * length;
                double endZ = branchOrigin.z + sideZ * length;
                double endY = surface.y(endX, endZ, branchOrigin.y);
                segments.add(new Segment(branchOrigin, new Vec3(endX, endY, endZ), a, b));
            }
        }
        return segments;
    }

    private static int root(int[] parent, int value) {
        while (parent[value] != value) {
            parent[value] = parent[parent[value]];
            value = parent[value];
        }
        return value;
    }

    public static Segment nearestSegment(List<Segment> segments, double x, double y, double z) {
        Segment best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Segment segment : segments) {
            double d2 = segment.distanceSquared(x, y, z);
            if (d2 < bestDistance) {
                bestDistance = d2;
                best = segment;
            }
        }
        return best;
    }

    public static Segment nearestSegment(List<Segment> segments, double x, double z) {
        Segment best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Segment segment : segments) {
            double d2 = segment.distanceSquared(x, z);
            if (d2 < bestDistance) { bestDistance = d2; best = segment; }
        }
        return best;
    }

    public static PlantNode nearestPlant(List<PlantNode> plants, double x, double y, double z) {
        PlantNode best = null;
        double bestDistance = Double.MAX_VALUE;
        for (PlantNode plant : plants) {
            Vec3 p = plant.anchor();
            double dx = x - p.x;
            double dy = y - p.y;
            double dz = z - p.z;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < bestDistance) { bestDistance = d2; best = plant; }
        }
        return best;
    }

    public static Block getMatchingSapling(Block logBlock) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(logBlock);
        String path = key.getPath();
        if (path.contains("mangrove")) return net.minecraft.world.level.block.Blocks.MANGROVE_PROPAGULE;
        if (path.contains("crimson")) return net.minecraft.world.level.block.Blocks.CRIMSON_FUNGUS;
        if (path.contains("warped")) return net.minecraft.world.level.block.Blocks.WARPED_FUNGUS;
        String saplingPath;
        if (path.endsWith("_log")) saplingPath = path.substring(0, path.length() - 4) + "_sapling";
        else if (path.endsWith("_wood")) saplingPath = path.substring(0, path.length() - 5) + "_sapling";
        else if (path.endsWith("_stem")) saplingPath = path.substring(0, path.length() - 5) + "_fungus";
        else saplingPath = path + "_sapling";
        ResourceLocation saplingId = ResourceLocation.fromNamespaceAndPath(key.getNamespace(), saplingPath);
        return BuiltInRegistries.BLOCK.getOptional(saplingId)
            .filter(block -> block instanceof net.minecraft.world.level.block.SaplingBlock || block instanceof BushBlock)
            .orElse(net.minecraft.world.level.block.Blocks.OAK_SAPLING);
    }

    public static boolean isPlant(BlockState state) {
        return state.is(BlockTags.LOGS) || state.getBlock() instanceof CropBlock || state.getBlock() instanceof BushBlock;
    }

    public static boolean isPlayerNearSegment(Entity entity, List<Segment> segments, double maxDistance) {
        Segment nearest = nearestSegment(segments, entity.getX(), entity.getY(), entity.getZ());
        return nearest != null && nearest.distanceSquared(entity.getX(), entity.getY(), entity.getZ()) <= maxDistance * maxDistance;
    }
}

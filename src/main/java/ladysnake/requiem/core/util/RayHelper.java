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
 *
 * Linking this mod statically or dynamically with other
 * modules is making a combined work based on this mod.
 * Thus, the terms and conditions of the GNU General Public License cover the whole combination.
 *
 * In addition, as a special exception, the copyright holders of
 * this mod give you permission to combine this mod
 * with free software programs or libraries that are released under the GNU LGPL
 * and with code included in the standard release of Minecraft under All Rights Reserved (or
 * modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the GNU GPL for this mod
 * and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of this mod are not obligated to grant
 * this special exception for their modified versions; it is their choice whether to do so.
 * The GNU General Public License gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version which carries forward this exception.
 */
package ladysnake.requiem.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Some raytracing utilities
 *
 * @author UpcraftLP
 */
public final class RayHelper {

    /**
     * Raytraces using an entity's position as source, and its look vector as direction
     *
     * @param entity        the raytrace source
     * @param range         the maximum range of the raytrace
     * @param shapeType     <b>COLLIDER</b> for collision raytracing, <b>OUTLINE</b> for tracing the block outline shape (render bounding box)
     * @param fluidHandling how to handle fluids
     * @param tickDeltaTime the delta tick time (partial render tick)
     */
    @Nonnull
    public static HitResult raycastEntity(Entity entity, double range, ClipContext.Block shapeType, ClipContext.Fluid fluidHandling, float tickDeltaTime) {
        Vec3 startPoint = entity.getEyePosition(tickDeltaTime);
        Vec3 lookVec = entity.getViewVector(tickDeltaTime);
        Vec3 endPoint = startPoint.add(lookVec.x * range, lookVec.y * range, lookVec.z * range);
        return raycast(entity.level(), entity, startPoint, endPoint, shapeType, fluidHandling);
    }

    /**
     *
     * @param world         the world
     * @param source        the entity to be used for determining block bounding boxes
     * @param start         the start point
     * @param end           the end point, if no result was found
     * @param shapeType     <b>COLLIDER</b> for collision raytracing, <b>OUTLINE</b> for tracing the block outline shape (render bounding box)
     * @param fluidHandling how to handle fluids
     */
    @Nonnull
    public static HitResult raycast(Level world, Entity source, Vec3 start, Vec3 end, ClipContext.Block shapeType, ClipContext.Fluid fluidHandling) {
        return world.clip(new ClipContext(start, end, shapeType, fluidHandling, source));
    }

    /**
     * Finds a suitable blink position for an entity based on its look
     *
     * @param entity    the source of the raytrace
     * @param deltaTime the delta tick time (partial render tick)
     * @param range     the maximum range that the entity can target
     * @return the position targeted by <code>entity</code>
     */
    public static Vec3 findBlinkPos(Entity entity, float deltaTime, double range) {
        Level world = entity.level();
        HitResult trace = raycastEntity(entity, range, ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY, deltaTime);
        boolean secondPass;
        if (trace.getType() == HitResult.Type.MISS) {
            trace = raycast(world, entity, trace.getLocation(), trace.getLocation().subtract(0, 1, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY);
            secondPass = false;
        } else {
            secondPass = true;
        }
        Vec3 pos = trace.getLocation();
        if (trace.getType() == HitResult.Type.BLOCK) {
            BlockHitResult result = (BlockHitResult) trace;
            switch (result.getDirection()) {
                case DOWN -> {
                    pos = pos.subtract(0, entity.getBbHeight(), 0);
                    secondPass = false;
                }
                case UP -> secondPass = false;
                default -> {
                    Vec3 entityPos = entity.getEyePosition(deltaTime);
                    Vec3 toTarget = pos.subtract(entityPos);
                    if (pos.y - (int) pos.y >= 0.5D) {
                        BlockPos testPos = switch (result.getDirection()) {
                            case EAST -> BlockPos.containing(pos.x - 1, pos.y + 1, pos.z);
                            case WEST, NORTH -> BlockPos.containing(pos.x, pos.y + 1, pos.z);
                            case SOUTH -> BlockPos.containing(pos.x, pos.y + 1, pos.z - 1);
                            default -> //should never happen, but better safe than sorry
                                throw new RaytraceException("hit result had wrong value: " + result.getDirection());
                        };
                        if (!world.noCollision(entity, entity.getBoundingBox().move(testPos.getX() - entity.getX(), testPos.getY() - entity.getY(), testPos.getZ() - entity.getZ()))) {
                            toTarget = toTarget.scale(Math.max((toTarget.length() + 0.8D) / toTarget.length(), 1.0D));
                            pos = new Vec3(entityPos.x + toTarget.x, testPos.getY() + 0.1D, entityPos.z + toTarget.z);
                            HitResult result1 = raycast(world, entity, pos, pos.subtract(0.0D, 1.0D, 0.0D), ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY);
                            pos = result1.getLocation();
                            secondPass = false;
                        }
                    }
                    if (secondPass) {
                        toTarget = toTarget.scale((toTarget.length() - (entity.getBbWidth() * 1.3F)) / toTarget.length());
                        pos = entityPos.add(toTarget);
                        HitResult result1 = raycast(world, entity, pos, pos.subtract(0.0D, 1.0D, 0.0D), ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY);
                        pos = result1.getLocation();
                    }
                }
            }
        }
        if (secondPass) {
            Vec3 tempPos = pos.subtract(0.0D, 0.0001D, 0.0D);
            HitResult flagcast = raycast(world, entity, tempPos, pos.add(0.0D, entity.getBbHeight(), 0.0D), ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY);
            if (flagcast.getLocation().y - tempPos.y < entity.getBbHeight()) {
                pos = flagcast.getLocation().subtract(0, entity.getBbHeight(), 0);
            }
        }
        return pos;
    }

    @Nullable
    public static Entity getTargetedEntity(LivingEntity watcher) {
        EntityHitResult result = raycast(watcher);
        return result != null ? result.getEntity() : null;
    }

    @Nullable
    public static EntityHitResult raycast(LivingEntity watcher) {
        return raycast(watcher, watcher.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE));
    }

    @Nullable
    public static EntityHitResult raycast(LivingEntity watcher, double distance) {
        Vec3 eyes = watcher.getEyePosition(1F);
        Vec3 look = watcher.getViewVector(1.0F);
        Vec3 end = eyes.add(look.x * distance, look.y * distance, look.z * distance);
        AABB box = watcher.getBoundingBox().expandTowards(look.scale(distance)).inflate(1.0D, 1.0D, 1.0D);
        return raycast(watcher, eyes, end, box, e -> !e.isSpectator() && e.isPickable(), distance * distance);
    }

    /**
     * @see net.minecraft.world.entity.projectile.ProjectileUtil#getEntityHitResult(Entity, Vec3, Vec3, AABB, Predicate, double)
     */
    @Nullable
    public static EntityHitResult raycast(Entity watcher, Vec3 startPoint, Vec3 endPoint, AABB box, Predicate<Entity> predicate, double range) {
        Level world = watcher.level();
        double r = range;
        Entity target = null;
        Vec3 pos = null;

        for (Entity entity : world.getEntities(watcher, box, predicate)) {
            AABB bb = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> hitPosition = bb.clip(startPoint, endPoint);
            if (bb.contains(startPoint)) {
                if (r >= 0.0D) {
                    target = entity;
                    pos = hitPosition.orElse(startPoint);
                    r = 0.0D;
                }
            } else if (hitPosition.isPresent()) {
                Vec3 hitPos = hitPosition.get();
                double distanceSq = startPoint.distanceToSqr(hitPos);
                if (distanceSq < r || r == 0.0D) {
                    if (entity.getRootVehicle() == watcher.getRootVehicle()) {
                        if (r == 0.0D) {
                            target = entity;
                            pos = hitPos;
                        }
                    } else {
                        target = entity;
                        pos = hitPos;
                        r = distanceSq;
                    }
                }
            }
        }

        if (target == null) {
            return null;
        }

        return new EntityHitResult(target, pos);
    }

    public static class RaytraceException extends RuntimeException {
        public RaytraceException(String message) {
            super(message);
        }
    }
}

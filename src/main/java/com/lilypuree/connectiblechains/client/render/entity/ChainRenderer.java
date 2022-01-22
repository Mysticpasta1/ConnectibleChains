/*
 * Copyright (C) 2022 legoatoom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.lilypuree.connectiblechains.client.render.entity;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.util.Helper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.phys.Vec3;

import static com.lilypuree.connectiblechains.util.Helper.drip2;
import static com.lilypuree.connectiblechains.util.Helper.drip2prime;

public class ChainRenderer {

    private final Object2ObjectOpenHashMap<BakeKey, ChainModel> models = new Object2ObjectOpenHashMap<>(256);
    private static final float CHAIN_SCALE = 1f;
    private static final float CHAIN_SIZE = CHAIN_SCALE * 3/16f;
    private static final int MAX_SEGMENTS = 2048;

    public static class BakeKey {
        private final int hash;
        public BakeKey(Vec3 srcPos, Vec3 dstPos) {
            float dY = (float) (srcPos.y - dstPos.y);
            float dXZ = Helper.distanceBetween(
                    new Vector3f((float) srcPos.x, 0, (float) srcPos.z),
                    new Vector3f((float) dstPos.x, 0, (float) dstPos.z));

            int hash = Float.floatToIntBits(dY);
            hash = 31 * hash + Float.floatToIntBits(dXZ);
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BakeKey bakeKey = (BakeKey) o;
            return hash == bakeKey.hash;
        }
    }

    public void renderBaked(VertexConsumer buffer, PoseStack matrices, BakeKey key, Vector3f chainVec, int blockLight0, int blockLight1, int skyLight0, int skyLight1) {
        ChainModel model;
        if(models.containsKey(key)) {
            model = models.get(key);
        } else {
            model = buildModel(chainVec);
            models.put(key, model);
        }
        model.render(buffer, matrices, blockLight0, blockLight1, skyLight0, skyLight1);
    }

    public void render(VertexConsumer buffer, PoseStack matrices, Vector3f chainVec, int blockLight0, int blockLight1, int skyLight0, int skyLight1) {
        ChainModel model = buildModel(chainVec);
        model.render(buffer, matrices, blockLight0, blockLight1, skyLight0, skyLight1);
    }

    private ChainModel buildModel(Vector3f chainVec) {
        float desiredSegmentLength = 1f / ConnectibleChains.runtimeConfig.getQuality();
        int initialCapacity = (int) (2f * Helper.lengthOf(chainVec) / desiredSegmentLength);
        ChainModel.Builder builder = ChainModel.builder(initialCapacity);

        if(chainVec.x() == 0 && chainVec.z() == 0) {
            buildFaceVertical(builder, chainVec, 45, 0);
            buildFaceVertical(builder, chainVec, -45, 3);
        } else {
            buildFace(builder, chainVec, 45, 0);
            buildFace(builder, chainVec, -45, 3);
        }

        return builder.build();
    }

    private void buildFaceVertical(ChainModel.Builder builder, Vector3f v, float angle, int uvu) {
        float actualSegmentLength = 1f / ConnectibleChains.runtimeConfig.getQuality();
        Vector3f normal = new Vector3f((float)Math.cos(Math.toRadians(angle)), 0, (float)Math.sin(Math.toRadians(angle)));
        normal.mul(CHAIN_SIZE);

        Vector3f vert00 = new Vector3f(-normal.x()/2, 0, -normal.z()/2), vert01 = vert00.copy();
        vert01.add(normal);
        Vector3f vert10 = new Vector3f(-normal.x()/2, 0, -normal.z()/2), vert11 = vert10.copy();
        vert11.add(normal);

        float uvv0 = 0, uvv1 = 0;
        boolean lastIter_ = false;
        for (int segment = 0; segment < MAX_SEGMENTS; segment++) {
            if(vert00.y() + actualSegmentLength >= v.y()) {
                lastIter_ = true;
                actualSegmentLength = v.y() - vert00.y();
            }

            vert10.add(0, actualSegmentLength, 0);
            vert11.add(0, actualSegmentLength, 0);

            uvv1 += actualSegmentLength / CHAIN_SCALE;

            builder.vertex(vert00).uv(uvu/16f, uvv0).next();
            builder.vertex(vert01).uv((uvu+3)/16f, uvv0).next();
            builder.vertex(vert11).uv((uvu+3)/16f, uvv1).next();
            builder.vertex(vert10).uv(uvu/16f, uvv1).next();

            if(lastIter_) break;

            uvv0 = uvv1;

            vert00.load(vert10);
            vert01.load(vert11);
        }
    }

    private void buildFace(ChainModel.Builder builder, Vector3f v, float angle, int uvu) {
        float actualSegmentLength, desiredSegmentLength = 1f / ConnectibleChains.runtimeConfig.getQuality();
        float distance = Helper.lengthOf(v), distanceXZ = (float) Math.sqrt(v.x()*v.x() + v.z()*v.z());
        // Original code used total distance between start and end instead of horizontal distance
        // That changed the look of chains when there was a big height difference, but it looks better.
        float wrongDistanceFactor = distance/distanceXZ;

        Vector3f vert00 = new Vector3f(), vert01 = new Vector3f(), vert11 = new Vector3f(), vert10 = new Vector3f();
        Vector3f normal = new Vector3f(), rotAxis = new Vector3f();

        float uvv0, uvv1 = 0, gradient, x, y;
        Vector3f point0 = new Vector3f(), point1 = new Vector3f();
        Quaternion rotator;

        // All of this setup can probably go, but I can't figure out
        // how to integrate it into the loop :shrug:
        point0.set(0, (float) drip2(0, distance, v.y()), 0);
        gradient = (float) drip2prime(0, distance, v.y());
        normal.set(-gradient, Math.abs(distanceXZ / distance), 0);
        normal.normalize();

        x = estimateDeltaX(desiredSegmentLength, gradient);
        gradient = (float) drip2prime(x*wrongDistanceFactor, distance, v.y());
        y = (float) drip2(x*wrongDistanceFactor, distance, v.y());
        point1.set(x, y, 0);

        rotAxis.set(point1.x() - point0.x(), point1.y() - point0.y(), point1.z() - point0.z());
        rotAxis.normalize();
        rotator = rotAxis.rotationDegrees(angle);

        normal.transform(rotator);
        normal.mul(CHAIN_SIZE);
        vert10.set(point0.x() - normal.x()/2, point0.y() - normal.y()/2, point0.z() - normal.z()/2);
        vert11.load(vert10);
        vert11.add(normal);

        actualSegmentLength = Helper.distanceBetween(point0, point1);

        boolean lastIter_ = false;
        for (int segment = 0; segment < MAX_SEGMENTS; segment++) {
            rotAxis.set(point1.x() - point0.x(), point1.y() - point0.y(), point1.z() - point0.z());
            rotAxis.normalize();
            rotator = rotAxis.rotationDegrees(angle);

            // This normal is orthogonal to the face normal
            normal.set(-gradient, Math.abs(distanceXZ / distance), 0);
            normal.normalize();
            normal.transform(rotator);
            normal.mul(CHAIN_SIZE);

            vert00.load(vert10);
            vert01.load(vert11);

            vert10.set(point1.x() - normal.x()/2, point1.y() - normal.y()/2, point1.z() - normal.z()/2);
            vert11.load(vert10);
            vert11.add(normal);

            uvv0 = uvv1;
            uvv1 = uvv0 + actualSegmentLength / CHAIN_SCALE;

            builder.vertex(vert00).uv(uvu/16f, uvv0).next();
            builder.vertex(vert01).uv((uvu+3)/16f, uvv0).next();
            builder.vertex(vert11).uv((uvu+3)/16f, uvv1).next();
            builder.vertex(vert10).uv(uvu/16f, uvv1).next();

            if(lastIter_) break;

            point0.load(point1);

            x += estimateDeltaX(desiredSegmentLength, gradient);
            if(x >= distanceXZ) {
                lastIter_ = true;
                x = distanceXZ;
            }

            gradient = (float) drip2prime(x*wrongDistanceFactor, distance, v.y());
            y = (float) drip2(x*wrongDistanceFactor, distance, v.y());
            point1.set(x, y, 0);

            actualSegmentLength = Helper.distanceBetween(point0, point1);
        }
    }

    /**
     * Estimate Δx based on current gradient to get segments with equal length
     * k ... Gradient
     * T ... Tangent
     * s ... Segment Length
     *
     * T = (1, k)
     *
     * Δx = (s * T / |T|).x
     * Δx = s * T.x / |T|
     * Δx = s * 1 / |T|
     * Δx = s / |T|
     * Δx = s / √(1^2 + k^2)
     * Δx = s / √(1 + k^2)
     * @param s the desired segment length
     * @param k the gradient
     * @return Δx
     */
    private float estimateDeltaX(float s, float k) {
        return (float) (s / Math.sqrt(1 + k*k));
    }

    public void purge() {
        models.clear();
    }
}
package ladysnake.requiem.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;

import java.util.function.BooleanSupplier;

/** Applies an entity-scoped alpha multiplier to model vertices while retaining the original render types. */
public final class OutOfBodyAlphaBufferSource implements MultiBufferSource {
    private final MultiBufferSource delegate;
    private final BooleanSupplier active;
    private final float alpha;

    public OutOfBodyAlphaBufferSource(MultiBufferSource delegate, BooleanSupplier active, float alpha) {
        this.delegate = delegate;
        this.active = active;
        this.alpha = alpha;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        VertexConsumer consumer = delegate.getBuffer(renderType);
        return active.getAsBoolean() ? new AlphaVertexConsumer(consumer, alpha) : consumer;
    }

    private static final class AlphaVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float alpha;

        private AlphaVertexConsumer(VertexConsumer delegate, float alpha) {
            this.delegate = delegate;
            this.alpha = alpha;
        }

        @Override public VertexConsumer addVertex(float x, float y, float z) { delegate.addVertex(x, y, z); return this; }

        @Override public VertexConsumer setColor(int red, int green, int blue, int sourceAlpha) {
            delegate.setColor(red, green, blue, Math.round(sourceAlpha * alpha));
            return this;
        }

        @Override public VertexConsumer setColor(int color) {
            int a = FastColor.ARGB32.alpha(color);
            int r = FastColor.ARGB32.red(color);
            int g = FastColor.ARGB32.green(color);
            int b = FastColor.ARGB32.blue(color);
            delegate.setColor(r, g, b, Math.round(a * alpha));
            return this;
        }

        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
            int a = FastColor.ARGB32.alpha(color);
            int r = FastColor.ARGB32.red(color);
            int g = FastColor.ARGB32.green(color);
            int b = FastColor.ARGB32.blue(color);
            int scaledColor = FastColor.ARGB32.color(Math.round(a * alpha), r, g, b);
            delegate.addVertex(x, y, z, scaledColor, u, v, overlay, light, normalX, normalY, normalZ);
        }

        @Override public VertexConsumer setUv(float u, float v) { delegate.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { delegate.setNormal(x, y, z); return this; }
    }
}

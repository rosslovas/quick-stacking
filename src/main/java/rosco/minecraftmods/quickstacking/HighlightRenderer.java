package rosco.minecraftmods.quickstacking;

import java.util.ArrayList;

import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class HighlightRenderer {

    private static final Object _lock = new Object();

    private static ArrayList<Highlight> _highlights = new ArrayList<>();

    public static void addHighlights(PacketByteBuf buf) {
        synchronized (_lock) {
            var firstDelay = -1.0f;
            var len = buf.readInt();
            for (var i = 0; i < len; ++i) {
                var pos = buf.readBlockPos();
                int type = buf.readInt();
                var delay = buf.readFloat();
                if (firstDelay < 0.0f) {
                    firstDelay = delay;
                }
                _highlights.add(new Highlight(pos, type, delay - firstDelay));
            }
        }
    }

    private static float lastRenderTime = 0.0f;

    public static void render(WorldRenderContext context) {
        var time = context.world().getTime() + context.tickDelta();
        var delta = time - lastRenderTime;
        lastRenderTime = time;
        if (_highlights.isEmpty()) {
            return;
        }

        var cameraPos = context.camera().getPos();
        var sorted = new ArrayList<Highlight>(_highlights.size());
        var first = true;
        ArrayList<Highlight> highlights;
        synchronized (_lock) {
            for (var highlight : _highlights) {
                if (first) {
                    highlight.update(cameraPos, delta);
                    if (highlight.expiry > 0.0) {
                        sorted.add(highlight);
                    }
                    first = false;
                    continue;
                }

                highlight.update(cameraPos, delta);
                var distanceSquared = highlight.distanceSquared;
                var needToAdd = true;
                for (var i = 0; i < sorted.size(); ++i) {
                    if (sorted.get(i).distanceSquared < distanceSquared) {
                        needToAdd = false;
                        if (highlight.expiry > 0.0) {
                            sorted.add(i, highlight);
                        }
                        break;
                    }
                }

                if (needToAdd) {
                    if (highlight.expiry > 0.0) {
                        sorted.add(highlight);
                    }
                }
            }

            _highlights = sorted;
            highlights = new ArrayList<>(sorted);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();

        var tessellator = Tessellator.getInstance();
        BufferBuilder buffer;

        for (var highlight : highlights) {
            if (highlight.expiry > 6.0f) {
                continue;
            }
            buffer = tessellator.getBuffer();
            buffer.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            renderBlockBounding(buffer, highlight);
            tessellator.draw();
        }

        RenderSystem.enableTexture();
    }

    static void renderBlockBounding(BufferBuilder buffer, Highlight pos) {
        var red = 0xff;
        var green = 0xff;
        var blue = 0xff;
        var alpha = pos.alpha;

        var x = pos.x;
        var y = pos.y;
        var z = pos.z;
        var xExtent = pos.xExtent;
        var zExtent = pos.zExtent;

        buffer.vertex(x, y + 1.01f, z).color(red, green, blue, alpha).next();
        buffer.vertex(x + xExtent, y + 1.01f, z).color(red, green, blue, alpha).next();
        buffer.vertex(x, y, z).color(red, green, blue, alpha).next();
        buffer.vertex(x + xExtent, y, z).color(red, green, blue, alpha).next();
        buffer.vertex(x + xExtent, y, z + zExtent).color(red, green, blue, alpha).next();
        buffer.vertex(x + xExtent, y + 1.01f, z).color(red, green, blue, alpha).next();
        buffer.vertex(x + xExtent, y + 1.01f, z + zExtent).color(red, green, blue, alpha).next();
        buffer.vertex(x, y + 1.01f, z).color(red, green, blue, alpha).next();
        buffer.vertex(x, y + 1.01f, z + zExtent).color(red, green, blue, alpha).next();
        buffer.vertex(x, y, z).color(red, green, blue, alpha).next();
        buffer.vertex(x, y, z + zExtent).color(red, green, blue, alpha).next();
        buffer.vertex(x + xExtent, y, z + zExtent).color(red, green, blue, alpha).next();
        buffer.vertex(x, y + 1.01f, z + zExtent).color(red, green, blue, alpha).next();
        buffer.vertex(x + xExtent, y + 1.01f, z + zExtent).color(red, green, blue, alpha).next();
    }

    private static class Highlight {

        public final double ox;
        public final double oy;
        public final double oz;
        public final double xExtent;
        public final double zExtent;

        public double x;
        public double y;
        public double z;
        public int alpha;

        public float expiry;
        public double distanceSquared = 0.0;

        public Highlight(BlockPos pos, int type, float delay) {
            this.ox = pos.getX() - 0.005f;
            this.oy = pos.getY() - 0.005f;
            this.oz = pos.getZ() - 0.005f;
            this.x = 0.0;
            this.y = 0.0;
            this.z = 0.0;
            this.xExtent = (type == 1) ? 2.01f : 1.01f;
            this.zExtent = (type == 2) ? 2.01f : 1.01f;
            this.expiry = 6.0f + delay;
            this.alpha = 0;
        }

        public void update(Vec3d cameraPos, float delta) {
            x = ox - cameraPos.x;
            y = oy - cameraPos.y;
            z = oz - cameraPos.z;
            expiry -= delta;
            alpha = (int) (expiry * 9.0f);

            var dx = x + 0.505f;
            var dy = y + 0.505f;
            var dz = z + 0.505f;
            distanceSquared = dx * dx + dy * dy + dz * dz;
        }
    }
}

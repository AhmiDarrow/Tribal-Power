package tk.darrow.tribalpower.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import tk.darrow.tribalpower.blockentity.SpiritCisternBlockEntity;

/**
 * The Spirit Cistern shows what it holds: the fluid stands in its four crystal windows, as high as the tank is
 * full, in the fluid's own colour and texture.
 */
public class SpiritCisternRenderer implements BlockEntityRenderer<SpiritCisternBlockEntity> {
    // the windows of the model: x/z span across a face, the depth between the wood core and the pane's outer skin
    private static final float LOW = 5.5F / 16, HIGH = 9.5F / 16, EDGE0 = 5.1F / 16, EDGE1 = 10.9F / 16;
    private static final float IN = 2.45F / 16, OUT = 3.9F / 16;   // depth from the block's face inward

    public SpiritCisternRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(SpiritCisternBlockEntity cistern, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        FluidStack fluid = cistern.tank.getFluid();
        if (fluid.isEmpty() || cistern.tank.getCapacity() <= 0) return;
        float fill = Math.min(1.0F, (float) fluid.getAmount() / cistern.tank.getCapacity());
        var extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(extensions.getStillTexture(fluid));
        int colour = extensions.getTintColor(fluid);
        float top = LOW + (HIGH - LOW) * fill;
        VertexConsumer vc = buffers.getBuffer(Sheets.translucentCullBlockSheet());
        // north and south windows, then west and east
        box(pose, vc, sprite, colour, light, EDGE0, LOW, IN, EDGE1, top, OUT);
        box(pose, vc, sprite, colour, light, EDGE0, LOW, 1 - OUT, EDGE1, top, 1 - IN);
        box(pose, vc, sprite, colour, light, IN, LOW, EDGE0, OUT, top, EDGE1);
        box(pose, vc, sprite, colour, light, 1 - OUT, LOW, EDGE0, 1 - IN, top, EDGE1);
    }

    private static void box(PoseStack pose, VertexConsumer vc, TextureAtlasSprite sprite, int colour, int light,
                            float x0, float y0, float z0, float x1, float y1, float z1) {
        int r = colour >> 16 & 255, g = colour >> 8 & 255, b = colour & 255, a = colour >>> 24 == 0 ? 255 : colour >>> 24;
        var m = pose.last();
        float u0 = sprite.getU0(), v0 = sprite.getV0(), u1 = sprite.getU(0.5F), v1 = sprite.getV(0.5F);
        // top
        quad(vc, m, r, g, b, a, light, u0, v0, u1, v1, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        // north (-z) and south (+z)
        quad(vc, m, r, g, b, a, light, u0, v0, u1, v1, 0, 0, -1, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0);
        quad(vc, m, r, g, b, a, light, u0, v0, u1, v1, 0, 0, 1, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1);
        // west (-x) and east (+x)
        quad(vc, m, r, g, b, a, light, u0, v0, u1, v1, -1, 0, 0, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1);
        quad(vc, m, r, g, b, a, light, u0, v0, u1, v1, 1, 0, 0, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose m, int r, int g, int b, int a, int light,
                             float u0, float v0, float u1, float v1, float nx, float ny, float nz, float... p) {
        float[][] uv = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
        for (int i = 0; i < 4; i++)
            vc.addVertex(m, p[i * 3], p[i * 3 + 1], p[i * 3 + 2]).setColor(r, g, b, a).setUv(uv[i][0], uv[i][1])
                    .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(light).setNormal(m, nx, ny, nz);
    }
}

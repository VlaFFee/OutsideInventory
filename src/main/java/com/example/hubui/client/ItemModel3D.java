package com.example.hubui.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * Предмет как настоящая 3D-модель с произвольным поворотом (плоские предметы получают толщину,
 * блоки вращаются как кубы). Используется в подсказках и в окне чтения.
 */
@OnlyIn(Dist.CLIENT)
public final class ItemModel3D {

    /** (cx, cy) - центр в текущей системе координат GUI, size - размер в пикселях, z - глубина. */
    public static void render(GuiGraphics g, ItemStack stack, float cx, float cy, float size,
                              float yawDeg, float pitchDeg, float z) {
        if (stack.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ItemRenderer ir = mc.getItemRenderer();
        BakedModel model = ir.getModel(stack, mc.level, mc.player, 0);

        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx, cy, z);
        ps.mulPoseMatrix(new Matrix4f().scaling(1f, -1f, 1f));
        ps.scale(size, size, size);
        ps.mulPose(Axis.XP.rotationDegrees(pitchDeg));
        ps.mulPose(Axis.YP.rotationDegrees(yawDeg));
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();
        ir.render(stack, ItemDisplayContext.NONE, false, ps, g.bufferSource(),
                15728880, OverlayTexture.NO_OVERLAY, model);
        g.flush();
        ps.popPose();
    }

    private ItemModel3D() {}
}

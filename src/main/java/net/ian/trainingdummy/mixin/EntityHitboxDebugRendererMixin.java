package net.ian.trainingdummy.mixin;

import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxDebugRendererMixin {

    @Inject(method = "showHitboxes", at = @At("TAIL"))
    private void trainingdummy$renderMultipartHitboxes(
        Entity entity,
        float partialTick,
        boolean isServerEntity,
        CallbackInfo ci
    ) {
        if (entity.isMultipartEntity() && !(entity instanceof EnderDragon)) {
            PartEntity<?>[] parts = entity.getParts();
            if (parts == null) {
                return;
            }
            int color = isServerEntity
                ? ARGB.colorFromFloat(1.0F, 0.25F, 1.0F, 0.0F)
                : ARGB.colorFromFloat(1.0F, 0.25F, 1.0F, 0.25F);
            GizmoStyle style = GizmoStyle.stroke(color);
            for (PartEntity<?> part : parts) {
                Vec3 pos = part.position();
                Vec3 prevPos = new Vec3(part.xOld, part.yOld, part.zOld);
                Vec3 delta = pos.subtract(prevPos).scale(partialTick - 1.0F);
                Gizmos.cuboid(part.getBoundingBox().move(delta), style);
            }
        }
    }
}

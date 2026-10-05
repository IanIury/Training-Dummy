package net.ian.trainingdummy.client.render

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation

object DummyGlowRenderType : RenderStateShard("dummy_glow_type", {}, {}) {

    fun outlineNoXray(texture: ResourceLocation): RenderType {
        return RenderType.create(
            "dummy_outline_no_xray",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_OUTLINE_SHADER) // Shader de contorno idêntico ao Vanilla
                .setTextureState(TextureStateShard(texture, false, false))
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .setDepthTestState(LEQUAL_DEPTH_TEST) // Esconde atrás das paredes
                .createCompositeState(false)
        )
    }

    fun getGlowOutline(texture: ResourceLocation): RenderType {
        return RenderType.create(
            "dummy_glow_outline",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                .setTextureState(TextureStateShard(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .setDepthTestState(LEQUAL_DEPTH_TEST) // Bloqueado por paredes!
                .createCompositeState(false)
        )
    }
}
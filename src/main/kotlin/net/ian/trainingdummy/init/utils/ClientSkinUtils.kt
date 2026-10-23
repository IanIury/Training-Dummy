package net.ian.trainingdummy.init.utils

import com.mojang.authlib.GameProfile
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.world.entity.player.PlayerSkin
import net.minecraft.world.item.component.ResolvableProfile

object ClientSkinUtils {

    val defaultPlayerSkin: PlayerSkin = DefaultPlayerSkin.getDefaultSkin()

    fun getPlayerSkin(profile: ResolvableProfile?): PlayerSkin? {
        if (profile == null) return null
        val minecraft = Minecraft.getInstance()
        return minecraft.playerSkinRenderCache().getOrDefault(profile).playerSkin()
    }

    fun getPlayerSkin(profile: GameProfile?): PlayerSkin? {
        if (profile == null) return null
        return getPlayerSkin(ResolvableProfile.createResolved(profile))
    }
}

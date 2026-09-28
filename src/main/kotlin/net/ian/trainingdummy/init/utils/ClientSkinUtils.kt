package net.ian.trainingdummy.init.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.PlayerSkin
import net.minecraft.resources.ResourceLocation
import com.mojang.authlib.GameProfile
import java.util.UUID

object ClientSkinUtils {

    val deafultPlayerSkin = Minecraft.getInstance().skinManager.getInsecureSkin(GameProfile(UUID.randomUUID(), "Dummy"))

    fun getPlayerSkin(profile: GameProfile?): PlayerSkin? {
        if(profile==null){ return null }
        val minecraft = Minecraft.getInstance()
        return minecraft.skinManager.getInsecureSkin(profile)
    }
}
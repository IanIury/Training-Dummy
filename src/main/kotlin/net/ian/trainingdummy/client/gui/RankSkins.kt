package net.ian.trainingdummy.client.gui

import com.mojang.authlib.GameProfile
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.PlayerSkin
import net.minecraft.world.item.component.ResolvableProfile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolve a skin (rosto) de um jogador apenas pelo UUID, inclusive de jogadores offline:
 * 1. jogador online -> skin da lista de jogadores;
 * 2. caso contrário, consulta o PlayerSkinRenderCache (que resolve assincronamente via SkinManager/ProfileResolver);
 * 3. enquanto isso, usa a skin padrão derivada do UUID.
 */
object RankSkins {

    private val profiles = ConcurrentHashMap<UUID, ResolvableProfile>()

    fun skin(uuid: UUID): PlayerSkin {
        val minecraft = Minecraft.getInstance()
        minecraft.connection?.getPlayerInfo(uuid)?.let { return it.skin }

        val resolvable = profiles.computeIfAbsent(uuid) {
            ResolvableProfile.createResolved(GameProfile(it, ""))
        }
        return minecraft.playerSkinRenderCache().getOrDefault(resolvable).playerSkin()
    }
}

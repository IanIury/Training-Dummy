package net.ian.trainingdummy.client.gui

import com.mojang.authlib.GameProfile
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.client.resources.PlayerSkin
import net.minecraft.world.level.block.entity.SkullBlockEntity
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolve a skin (rosto) de um jogador apenas pelo UUID, inclusive de jogadores offline:
 * 1. jogador online -> skin da lista de jogadores;
 * 2. caso contrário, busca o perfil completo (com texturas) de forma assíncrona e guarda em cache;
 * 3. enquanto isso, usa a skin padrão derivada do UUID.
 */
object RankSkins {

    private val profiles = ConcurrentHashMap<UUID, GameProfile>()
    private val requested = ConcurrentHashMap.newKeySet<UUID>()

    fun skin(uuid: UUID): PlayerSkin {
        val minecraft = Minecraft.getInstance()
        minecraft.connection?.getPlayerInfo(uuid)?.let { return it.skin }

        profiles[uuid]?.let { return minecraft.skinManager.getInsecureSkin(it) }

        if (requested.add(uuid)) {
            SkullBlockEntity.fetchGameProfile(uuid).thenAccept { optional ->
                optional.ifPresent { profile -> profiles[uuid] = profile }
            }
        }
        return DefaultPlayerSkin.get(uuid)
    }
}

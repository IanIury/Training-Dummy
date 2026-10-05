package net.ian.trainingdummy.init.enums

import com.mojang.serialization.Codec
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.util.ByIdMap
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs

enum class TrainingModos {
    DEFAULT, // SINGLE_HIT
    DPS,
    ACCUMULATED,
    COMPARISON;


    companion object {

        fun fromString(name: String): TrainingModos {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DEFAULT
        }

        // --- CODEC (para salvar no NBT / Disco) ---
        val CODEC: Codec<TrainingModos> = Codec.STRING.xmap(
            { string -> fromString(string) },
            { enumValue -> enumValue.name }
        )

        private val BY_ID = ByIdMap.continuous(
            { it.ordinal },
            entries.toTypedArray(),
            ByIdMap.OutOfBoundsStrategy.ZERO
        )

        // --- STREAM_CODEC (para envio via REDE) ---


        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, TrainingModos> =
            NeoForgeStreamCodecs.enumCodec(TrainingModos::class.java)

    }

    fun next(): TrainingModos {
        return entries[(this.ordinal + 1) % entries.size]
    }

    fun previous(): TrainingModos {
        return entries[(this.ordinal - 1 + entries.size) % entries.size]
    }

    fun nameD(): String {return name.replace('_',' ').lowercase().replaceFirstChar { it.uppercase() }}

}
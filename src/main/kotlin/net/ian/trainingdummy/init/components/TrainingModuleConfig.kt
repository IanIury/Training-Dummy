package net.ian.trainingdummy.init.components

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.ian.trainingdummy.init.enums.TrainingModos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec

data class TrainingModuleConfig(

    var trainingModo : TrainingModos = TrainingModos.DEFAULT,

    ) {

    companion object {

        val CODEC: Codec<TrainingModuleConfig> = RecordCodecBuilder.create { instance ->
            instance.group(
                TrainingModos.CODEC.fieldOf("trainingModo").forGetter { it.trainingModo }
            ).apply(instance, ::TrainingModuleConfig)
        }

        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, TrainingModuleConfig> = StreamCodec.composite(
            TrainingModos.STREAM_CODEC, TrainingModuleConfig::trainingModo,
            ::TrainingModuleConfig
        )

    }

}
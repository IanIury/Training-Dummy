package net.ian.trainingdummy.init.components

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.ian.trainingdummy.init.enums.TrainingModos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.syncher.EntityDataSerializer

data class TrainingModuleConfig(

    val trainingModo : TrainingModos = TrainingModos.DEFAULT,

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

        val TRAINING_MODULE_CONFIG: EntityDataSerializer<TrainingModuleConfig> =
            EntityDataSerializer.forValueType(STREAM_CODEC)

    }

}
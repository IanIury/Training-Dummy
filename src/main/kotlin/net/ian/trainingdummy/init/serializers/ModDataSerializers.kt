package net.ian.trainingdummy.init.serializers

import net.ian.trainingdummy.event.utils.DamageData
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.world.item.component.ResolvableProfile
import java.util.Optional

object ModDataSerializers {



    // Define o StreamCodec para ler e escrever o objeto no ByteBuf
    val DAMAGE_DATA_CODEC: StreamCodec<RegistryFriendlyByteBuf, DamageData> = StreamCodec.of(
        { buffer, value ->
            buffer.writeFloat(value.originalDamage)
            buffer.writeFloat(value.newDamage)
            buffer.writeFloat(value.shieldDamage)
            buffer.writeFloat(value.blockedDamage)

            buffer.writeFloat(value.damageMultiplier)
            buffer.writeFloat(value.vanillaMultiplier)
            buffer.writeBoolean(value.isCriticalHit)
            buffer.writeBoolean(value.isVanillaCritical)

            buffer.writeFloat(value.invulnerability)
            buffer.writeFloat(value.armor)
            buffer.writeFloat(value.enchantments)
            buffer.writeFloat(value.mobEffects)
            buffer.writeFloat(value.absorption)
            buffer.writeFloat(value.innateResistance)
        },
        { buffer ->
            DamageData(
                originalDamage = buffer.readFloat(),
                newDamage = buffer.readFloat(),
                shieldDamage = buffer.readFloat(),
                blockedDamage = buffer.readFloat(),
                damageMultiplier = buffer.readFloat(),
                vanillaMultiplier = buffer.readFloat(),
                isCriticalHit = buffer.readBoolean(),
                isVanillaCritical = buffer.readBoolean(),
                invulnerability = buffer.readFloat(),
                armor = buffer.readFloat(),
                enchantments = buffer.readFloat(),
                mobEffects = buffer.readFloat(),
                absorption = buffer.readFloat(),
                innateResistance = buffer.readFloat()
            )
        }
    )



    // Cria a instância do EntityDataSerializer
    val DAMAGE_DATA: EntityDataSerializer<DamageData> = EntityDataSerializer.forValueType(DAMAGE_DATA_CODEC)
    val OPTIONAL_RESOLVABLE_PROFILE : EntityDataSerializer<Optional<ResolvableProfile>> = EntityDataSerializer.forValueType(
        ByteBufCodecs.optional(ResolvableProfile.STREAM_CODEC)
    )

}
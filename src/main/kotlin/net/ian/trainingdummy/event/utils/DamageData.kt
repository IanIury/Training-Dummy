package net.ian.trainingdummy.event.utils


import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.syncher.EntityDataSerializer


data class DamageData(

    val originalDamage : Float = 0f,
    val newDamage : Float = 0f,
    val shieldDamage : Float = 0f,
    val blockedDamage : Float = 0f,

    val damageMultiplier: Float = 1f,
    val vanillaMultiplier: Float = 1f,
    val isCriticalHit: Boolean = false,
    val isVanillaCritical: Boolean = false,

    val invulnerability: Float = 0f,
    val armor: Float = 0f,
    val enchantments: Float = 0f,
    val mobEffects: Float = 0f,
    val absorption: Float = 0f,
    val innateResistance: Float = 0f
)

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
}
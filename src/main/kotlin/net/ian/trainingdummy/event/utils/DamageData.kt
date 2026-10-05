package net.ian.trainingdummy.event.utils


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
){
    fun isReductions() : Boolean{return (invulnerability > 0.0f) || (armor > 0.0f) || (enchantments > 0.0f) || (mobEffects > 0.0f) || (absorption > 0.0f) || (innateResistance > 0.0f) }
    operator fun plus(other: DamageData): DamageData {
        return DamageData(
            originalDamage = this.originalDamage + other.originalDamage,
            newDamage = this.newDamage + other.newDamage,
            shieldDamage = this.shieldDamage + other.shieldDamage,
            blockedDamage = this.blockedDamage + other.blockedDamage,

            // Mantém os valores do novo golpe ou a média se preferir
            damageMultiplier = other.damageMultiplier,
            vanillaMultiplier = other.vanillaMultiplier,
            isCriticalHit = this.isCriticalHit || other.isCriticalHit,
            isVanillaCritical = this.isVanillaCritical || other.isVanillaCritical,

            invulnerability = this.invulnerability + other.invulnerability,
            armor = this.armor + other.armor,
            enchantments = this.enchantments + other.enchantments,
            mobEffects = this.mobEffects + other.mobEffects,
            absorption = this.absorption + other.absorption,
            innateResistance = this.innateResistance + other.innateResistance
        )
    }
    operator fun minus(other: DamageData): DamageData {
        return DamageData(
            originalDamage = (this.originalDamage - other.originalDamage).coerceAtLeast(0f),
            newDamage = (this.newDamage - other.newDamage).coerceAtLeast(0f),
            shieldDamage = (this.shieldDamage - other.shieldDamage).coerceAtLeast(0f),
            blockedDamage = (this.blockedDamage - other.blockedDamage).coerceAtLeast(0f),

            damageMultiplier = this.damageMultiplier,
            vanillaMultiplier = this.vanillaMultiplier,
            isCriticalHit = this.isCriticalHit && !other.isCriticalHit,
            isVanillaCritical = this.isVanillaCritical && !other.isVanillaCritical,

            invulnerability = (this.invulnerability - other.invulnerability).coerceAtLeast(0f),
            armor = (this.armor - other.armor).coerceAtLeast(0f),
            enchantments = (this.enchantments - other.enchantments).coerceAtLeast(0f),
            mobEffects = (this.mobEffects - other.mobEffects).coerceAtLeast(0f),
            absorption = (this.absorption - other.absorption).coerceAtLeast(0f),
            innateResistance = (this.innateResistance - other.innateResistance).coerceAtLeast(0f)
        )
    }
}


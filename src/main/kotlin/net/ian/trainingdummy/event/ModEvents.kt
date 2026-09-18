package net.ian.trainingdummy.event

import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.entity.DummyEntity
import net.ian.trainingdummy.event.utils.DamageData
import net.minecraft.util.Mth
import net.minecraft.world.item.MaceItem
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

@EventBusSubscriber(modid = TrainingDummy.ID)
object ModEvents {

    @SubscribeEvent
    fun registerPayloads(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar("1.0.0")
    }

    @SubscribeEvent
    fun onDamage(event: LivingDamageEvent.Post){

        if (event.entity.level().isClientSide) return

        //val player : Player = event.source.entity as? Player ?: return
        val vitima : DummyEntity = event.entity as? DummyEntity ?: return

        vitima.damageDataOLD = vitima.damageData;

        val damaData = vitima.damageData.copy(
            newDamage = event.newDamage,
            originalDamage = event.originalDamage,
            blockedDamage = event.blockedDamage,
            shieldDamage = event.shieldDamage,

            invulnerability = event.getReduction(Reduction.INVULNERABILITY),
            armor = event.getReduction(Reduction.ARMOR),
            enchantments = event.getReduction(Reduction.ENCHANTMENTS),
            mobEffects = event.getReduction(Reduction.MOB_EFFECTS),
            absorption = event.getReduction(Reduction.ABSORPTION),
            innateResistance = event.getReduction(Reduction.INNATE_RESISTANCE)
        )

        vitima.entityData.set(DummyEntity.DAMAGE_DATA,damaData)

        hitAnimation(event)



    }

    @SubscribeEvent
    fun onCrit(event : CriticalHitEvent){

        if (event.entity.level().isClientSide) return

        //val player : Player = event.entity
        val vitima : DummyEntity = event.target as? DummyEntity ?: return

        vitima.damageData = DamageData(
            damageMultiplier = event.damageMultiplier,
            vanillaMultiplier = event.vanillaMultiplier,
            isCriticalHit = event.isCriticalHit,
            isVanillaCritical = event.isVanillaCritical
        )

    }


    private fun hitAnimation(event: LivingDamageEvent.Post){

        val dummy : DummyEntity = event.entity as? DummyEntity ?: return
        val attacker = event.source.entity
        if (attacker != null) {

            if(attacker.weaponItem?.item is MaceItem){
                dummy.maceAnimation.maceDamage = event.newDamage
                dummy.maceAnimation.trigger()
            }else{
                dummy.maceAnimation.maceDamage = -1.0f
                dummy.maceAnimation.trigger()
            }

            val dx = attacker.x - dummy.x
            val dz = attacker.z - dummy.z
            val attackAngle = Mth.atan2(dz, dx) * (180.0f / Math.PI.toFloat()) - 90.0f
            val relativeAngle = Mth.wrapDegrees(attackAngle - dummy.yRot)
            val rad = Math.toRadians(relativeAngle)

            dummy.hitAnimation.hitPitch = -Mth.cos(rad.toFloat())
            dummy.hitAnimation.hitRoll = Mth.sin(rad.toFloat())
        } else {
            dummy.hitAnimation.hitPitch = -1.0f
            dummy.hitAnimation.hitRoll = 0.0f
        }
        dummy.hitAnimation.trigger()
    }


}
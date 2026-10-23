package net.ian.trainingdummy.entity.entityrender

import net.ian.trainingdummy.entity.DummyEntity
import net.ian.trainingdummy.init.enums.TrainingModos
import net.ian.trainingdummy.init.enums.WindowShapeType
import net.minecraft.client.renderer.entity.state.HumanoidRenderState
import net.minecraft.resources.Identifier
import java.util.UUID

class DummyRenderState : HumanoidRenderState() {
    var dummyEntity: DummyEntity? = null
    var dummyId: Int = 0
    var dummyUuid: UUID? = null

    // Hit & Wobble Animations
    var isHitAnimationStarted: Boolean = false
    var hitTimeMs: Float = 0f
    var hitPitch: Float = 0f
    var hitRoll: Float = 0f
    var offsetPositionX: Float = 0f
    var offsetPositionZ: Float = 0f

    // Spawn Animation
    var isSpawnAnimationStarted: Boolean = false
    var spawnTimeMs: Float = 0f

    // Mace Animation
    var isMaceAnimationStarted: Boolean = false
    var maceTimeMs: Float = 0f
    var maceIntensity: Float = 0f

    // Visuals & GUI
    var isGui: Boolean = false
    var isJeb: Boolean = false
    var customNameString: String? = null
    var skinTexture: Identifier? = null
    var dummyTexture: Identifier = Identifier.fromNamespaceAndPath("trainingdummy", "textures/entity/training_dummy.png")

    // Modes & Window settings
    var trainingMode: TrainingModos = TrainingModos.DEFAULT
    var windowShape: WindowShapeType = WindowShapeType.ROUNDED
    var detailedDamage: Boolean = false
    var dps: Float = 0f
    var maxDamage: Float = 0f
    var totalDamage: Float = 0f
    var hitCount: Int = 0
}

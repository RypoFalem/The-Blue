package io.github.rypofalem.the_blue.entities

import io.github.rypofalem.the_blue.TheBlueMod
import javax.annotation.Nullable
import net.minecraft.client.model.ModelPart
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.client.render.entity.{EntityRenderDispatcher, MobEntityRenderer}
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.ai.control.MoveControl
import net.minecraft.entity.ai.goal.{FleeEntityGoal, LookAroundGoal, LookAtEntityGoal, MeleeAttackGoal, MoveIntoWaterGoal, RevengeGoal, SwimAroundGoal}
import net.minecraft.entity.ai.pathing.SwimNavigation
import net.minecraft.entity.{Entity, EntityData, EntityDimensions, EntityPose, EntityType, MovementType, SpawnReason}
import net.minecraft.entity.attribute.{DefaultAttributeContainer, EntityAttributes}
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.mob.{GuardianEntity, MobEntity, WaterCreatureEntity}
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item.Settings
import net.minecraft.item.SpawnEggItem
import net.minecraft.nbt.CompoundTag
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Identifier
import net.minecraft.util.math.{MathHelper, Vec3d}
import net.minecraft.world.{LocalDifficulty, World, WorldAccess}

object Merfolk{
  def createAttributes: DefaultAttributeContainer = {
    MobEntity.createMobAttributes().
      add(EntityAttributes.GENERIC_MAX_HEALTH, 10).
      add(EntityAttributes.GENERIC_MOVEMENT_SPEED, .15f).
      add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1).
      build()
  }
}

class Merfolk(typee: EntityType[Merfolk], world: World) extends WaterCreatureEntity(typee, world) {
  moveControl = new MerfolkMoveControl(this)

  @Nullable override def initialize(world: WorldAccess,
                                    difficulty: LocalDifficulty,
                                    spawnReason: SpawnReason,
                                    @Nullable entityData: EntityData,
                                    @Nullable entityTag: CompoundTag): EntityData = {
    this.pitch = 0.0F
    super.initialize(world, difficulty, spawnReason, entityData, entityTag)
  }

  override def tickWaterBreathingAir(air: Int): Unit = {} // don't drown in air

  override def tryAttack(target: Entity): Boolean = {
    if (target.damage(
      DamageSource.mob(this), getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getValue.toInt.toFloat)
    ) {
      dealDamage(this, target)
      playSound(SoundEvents.ENTITY_DOLPHIN_ATTACK, 1.0F, 1.0F)
      true
    } else false
  }

  override def travel(movementInput: Vec3d): Unit = {
    if (canMoveVoluntarily && isTouchingWater) {
      updateVelocity(getMovementSpeed, movementInput)
      move(MovementType.SELF, getVelocity)
      setVelocity(getVelocity.multiply(.9))
      if (getTarget == null) setVelocity(getVelocity.add(0, -.005, 0))
    }
    else super.travel(movementInput)
  }

  override protected def initGoals(): Unit = {
    goalSelector.add(0, new MoveIntoWaterGoal(this))
    goalSelector.add(4, new SwimAroundGoal(this, 1, 10))
    goalSelector.add(4, new LookAroundGoal(this))
    goalSelector.add(5, new LookAtEntityGoal(this, classOf[PlayerEntity], 6F))
    goalSelector.add(6, new MeleeAttackGoal(this, 1.2, true))
    goalSelector.add(9, new FleeEntityGoal(this, classOf[GuardianEntity], 8F, 1, 1))
    targetSelector.add(1,
      new RevengeGoal(this, classOf[GuardianEntity]).setGroupRevenge(classOf[GuardianEntity]))
  }

  override protected def createNavigation(world: World) = new SwimNavigation(this, world)

  override protected def getActiveEyeHeight(pose: EntityPose, dimensions: EntityDimensions): Float =
    if (isBaby) 0.7F else 1.4F

  private class MerfolkMoveControl(val merfolk: Merfolk) extends MoveControl(merfolk) {
    override def tick(): Unit = {
      if (merfolk.isTouchingWater) merfolk.setVelocity(merfolk.getVelocity.add(0.0D, 0.005D, 0.0D))
      if (state == MoveControl.State.MOVE_TO && !merfolk.getNavigation.isIdle) {
        val dX = targetX - merfolk.getX
        val dY = targetY - merfolk.getY
        val dZ = targetZ - merfolk.getZ
        val dToTargetSquared = dX * dX + dY * dY + dZ * dZ
        if (dToTargetSquared < 2.5E-7D) entity.setForwardSpeed(0.0F)
        else {
          val h = (MathHelper.atan2(dZ, dX) * 57.2957763671875D).toFloat - 90.0F
          merfolk.yaw = this.changeAngle(merfolk.yaw, h, 15F)
          merfolk.bodyYaw = merfolk.yaw
          merfolk.headYaw = merfolk.yaw
          val effectiveSpeed = (speed * merfolk.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).getValue).toFloat
          if (merfolk.isTouchingWater) {
            merfolk.setMovementSpeed(effectiveSpeed)
            val pitchTarget = MathHelper.clamp(
              MathHelper.wrapDegrees(MathHelper.atan2(dY, MathHelper.sqrt(dX * dX + dZ * dZ)).toFloat * -57.3f),
              -85, 85)
            merfolk.pitch = this.changeAngle(merfolk.pitch, pitchTarget, 8)
            val toDegree = 0.017453292F
            val forwardRatio = MathHelper.cos(merfolk.pitch * toDegree)
            val upwardRatio = MathHelper.sin(merfolk.pitch * toDegree)
            merfolk.forwardSpeed = forwardRatio * effectiveSpeed
            merfolk.upwardSpeed = -upwardRatio * effectiveSpeed
          }
          else merfolk.setMovementSpeed(effectiveSpeed * .5f)
        }
      }
      else {
        merfolk.setMovementSpeed(0.0F)
        merfolk.setSidewaysSpeed(0.0F)
        merfolk.setUpwardSpeed(0.0F)
        merfolk.setForwardSpeed(0.0F)
      }
    }
  }

}

class MerfolkRenderer(erd: EntityRenderDispatcher)
  extends MobEntityRenderer[Merfolk, MerfolkModel[Merfolk]](erd, new MerfolkModel[Merfolk], .3125f) {
  override def getTexture(entity: Merfolk): Identifier =
    new Identifier(TheBlueMod.modid, "textures/entity/merfolk.png")
}

class MerfolkModel[T <: Merfolk] extends EntityModel[T] {
  textureWidth = 64
  textureHeight = 64

  val headbone = new ModelPart(this)
  headbone.setPivot(0.0F, 6.5F, 0.0F)
  headbone.setTextureOffset(0, 0).
    addCuboid(-4.0F, -10.5F, -4.0F, 8, 10, 8, 0.0F, false)

  val nosebone = new ModelPart(this)
  nosebone.setPivot(0.0F, -2.5F, -5.0F)
  headbone.addChild(nosebone)
  nosebone.setTextureOffset(24, 0).
    addCuboid(-1.0F, -1.0F, -1.0F, 2, 4, 2, 0.0F, false)

  val headfin = new ModelPart(this)
  headfin.setPivot(0.0F, -10.0F, 0.0F)
  headbone.addChild(headfin)
  headfin.setTextureOffset(46, 48).
    addCuboid(-0.5F, -3.5F, -2.0F, 1, 8, 8, 0.0F, false)

  val rightsidefin = new ModelPart(this)
  rightsidefin.setPivot(-4.0F, -4F, -1.0F)
  headbone.addChild(rightsidefin)
  rightsidefin.setTextureOffset(47, 55).
    addCuboid(-3.0F, -0.25F, -2.0F, 4, 1, 4, 0.0F, false)

  val leftsidefin = new ModelPart(this)
  leftsidefin.setPivot(4.0F, -4F, -1.0F)
  headbone.addChild(leftsidefin)
  leftsidefin.setTextureOffset(47, 55).
    addCuboid(-1.0F, -0.25F, -2.0F, 4, 1, 4, 0.0F, true)

  val torsobone = new ModelPart(this)
  torsobone.setPivot(0.0F, 23.0F, 0.0F)
  torsobone.setTextureOffset(16, 20).
    addCuboid(-4.0F, -17.0F, -3.0F, 8, 17, 6, 0.5F, false)

  val leftarm = new ModelPart(this)
  leftarm.setPivot(4.5F, -16.0F, -0.5F)
  torsobone.addChild(leftarm)
  leftarm.setTextureOffset(0, 27).
    addCuboid(-0.5F, -1.0F, -1.5F, 3, 8, 3, 0.0F, false)

  val rightarm = new ModelPart(this)
  rightarm.setPivot(-4.5F, -16.0F, -0.5F)
  torsobone.addChild(rightarm)
  rightarm.setTextureOffset(0, 27).
    addCuboid(-2.5F, -1.0F, -1.5F, 3, 8, 3, 0.0F, true)

  val tailbone = new ModelPart(this)
  tailbone.setPivot(0.0F, 22.0F, 2.0F)
  tailbone.setTextureOffset(33, 0).
    addCuboid(-4.0F, -1.0F, 0.0F, 8, 3, 7, -0.5F, false)

  val finbone = new ModelPart(this)
  finbone.setPivot(0.0F, 5.5F, 6.0F)
  tailbone.addChild(finbone)
  finbone.setTextureOffset(33, 13).
    addCuboid(-5.0F, -5.5F, -1.0F, 10, 1, 4, 0.0F, false)

  override def render(mat: MatrixStack,
                      vc: VertexConsumer,
                      light: Int,
                      overlay: Int,
                      red: Float,
                      green: Float,
                      blue: Float,
                      alpha: Float): Unit = {
    headbone.render(mat, vc, light, overlay, red, green, blue, alpha)
    torsobone.render(mat, vc, light, overlay, red, green, blue, alpha)
    tailbone.render(mat, vc, light, overlay, red, green, blue, alpha)
  }

  override def setAngles(entity: T,
                         limbAngle: Float,
                         limbDistance: Float,
                         customAngle: Float,
                         headYaw: Float,
                         headPitch: Float): Unit = {
    headbone.yaw = toDegrees(headYaw)
    headbone.pitch = toDegrees(headPitch)
    val cosAngle = -0.1F * MathHelper.cos(customAngle * 0.3F)
    tailbone.pitch = cosAngle
    finbone.pitch = 2 * cosAngle
    leftsidefin.roll =.1f + cosAngle
    rightsidefin.roll = -.1f - cosAngle
    rightarm.pitch = MathHelper.sin(limbAngle * .3f) * .33f
    leftarm.pitch = MathHelper.cos(limbAngle * .3f) * .33f
  }

  def toDegrees(rad: Float): Float = rad * 0.017453292F
}

final class MerfolkEgg(set: Settings)
  extends SpawnEggItem(TheBlueMod.merfolkType, 0x3d828c, 0x386784, set) {}

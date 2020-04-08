package io.github.rypofalem.the_blue.entities

import io.github.rypofalem.the_blue.TheBlueMod
import net.minecraft.client.model.ModelPart
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.client.render.entity.{EntityRenderDispatcher, MobEntityRenderer}
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.ai.goal._
import net.minecraft.entity.ai.pathing.SwimNavigation
import net.minecraft.entity.mob.{GuardianEntity, WaterCreatureEntity}
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.{Entity, EntityDimensions, EntityPose, EntityType}
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import net.minecraft.world.World

class Merfolk(typee: EntityType[Merfolk], world: World) extends WaterCreatureEntity(typee, world) {

  override protected def initGoals(): Unit = {
    this.goalSelector.add(0, new BreatheAirGoal(this))
    this.goalSelector.add(0, new MoveIntoWaterGoal(this))
    this.goalSelector.add(4, new SwimAroundGoal(this, 1, 10))
    this.goalSelector.add(4, new LookAroundGoal(this))
    this.goalSelector.add(5, new LookAtEntityGoal(this, classOf[PlayerEntity], 6F))
    this.goalSelector.add(6, new MeleeAttackGoal(this, 1.2, true))
    this.goalSelector.add(8, new ChaseBoatGoal(this))
    this.goalSelector.add(9, new FleeEntityGoal(this, classOf[GuardianEntity], 8F, 1, 1))
    this.targetSelector.add(1, new RevengeGoal(this, classOf[GuardianEntity]).setGroupRevenge(classOf[GuardianEntity]))
  }

  override def tickWaterBreathingAir(air: Int): Unit = {} // don't drown in air

  override protected def createNavigation(world: World) = new SwimNavigation(this, world)

  override protected def getActiveEyeHeight(pose: EntityPose, dimensions: EntityDimensions): Float =
    if (this.isBaby) 0.7F else 1.2F

}

class MerfolkRenderer(erd: EntityRenderDispatcher) extends MobEntityRenderer[Merfolk, MerfolkModel[Merfolk]](erd, new MerfolkModel[Merfolk], .3125f) {
  override def getTexture(entity: Merfolk): Identifier = new Identifier(TheBlueMod.modid, "textures/entity/merfolk.png")
}

class MerfolkModel[T <: Entity] extends EntityModel[T] {
  textureWidth = 64
  textureHeight = 64

  val headbone = new ModelPart(this)
  headbone.setPivot(0.0F, 6.5F, 0.0F)
  headbone.setTextureOffset(0, 0).addCuboid(-4.0F, -10.5F, -4.0F, 8, 10, 8, 0.0F, false)

  val nosebone = new ModelPart(this)
  nosebone.setPivot(0.0F, -2.5F, -5.0F)
  headbone.addChild(nosebone)
  nosebone.setTextureOffset(24, 0).addCuboid(-1.0F, -1.0F, -1.0F, 2, 4, 2, 0.0F, false)

  val headfin = new ModelPart(this)
  headfin.setPivot(0.0F, -10.0F, 0.0F)
  headbone.addChild(headfin)
  headfin.setTextureOffset(46, 48).addCuboid(-0.5F, -3.5F, -2.0F, 1, 8, 8, 0.0F, false)

  val rightsidefin = new ModelPart(this)
  rightsidefin.setPivot(-4.0F, -4.25F, -1.0F)
  headbone.addChild(rightsidefin)
  rightsidefin.setTextureOffset(47, 55).addCuboid(-3.0F, -0.25F, -2.0F, 4, 1, 4, 0.0F, false)

  val leftsidefin = new ModelPart(this)
  leftsidefin.setPivot(4.0F, -4.25F, -1.0F)
  headbone.addChild(leftsidefin)
  leftsidefin.setTextureOffset(47, 55).addCuboid(-1.0F, -0.25F, -2.0F, 4, 1, 4, 0.0F, true)

  val torsobone = new ModelPart(this)
  torsobone.setPivot(0.0F, 23.0F, 0.0F)
  torsobone.setTextureOffset(16, 20).addCuboid(-4.0F, -17.0F, -3.0F, 8, 17, 6, 0.5F, false)

  val leftarm = new ModelPart(this)
  leftarm.setPivot(4.5F, -16.0F, -0.5F)
  torsobone.addChild(leftarm)
  leftarm.setTextureOffset(0, 27).addCuboid(-0.5F, -1.0F, -1.5F, 3, 8, 3, 0.0F, false)

  val rightarm = new ModelPart(this)
  rightarm.setPivot(-4.5F, -16.0F, -0.5F)
  torsobone.addChild(rightarm)
  rightarm.setTextureOffset(0, 27).addCuboid(-2.5F, -1.0F, -1.5F, 3, 8, 3, 0.0F, true)

  val tailbone = new ModelPart(this)
  tailbone.setPivot(0.0F, 22.0F, 2.0F)
  tailbone.setTextureOffset(33, 0).addCuboid(-4.0F, -1.0F, 0.0F, 8, 3, 7, -0.5F, false)

  val finbone = new ModelPart(this)
  finbone.setPivot(0.0F, 5.5F, 6.0F)
  tailbone.addChild(finbone)
  finbone.setTextureOffset(33, 13).addCuboid(-5.0F, -5.5F, -1.0F, 10, 1, 4, 0.0F, false)

  override def render(mat: MatrixStack, vc: VertexConsumer, light: Int, overlay: Int, red: Float, green: Float, blue: Float, alpha: Float): Unit = {
    headbone.render(mat, vc, light, overlay, red, green, blue, alpha)
    torsobone.render(mat, vc, light, overlay, red, green, blue, alpha)
    tailbone.render(mat, vc, light, overlay, red, green, blue, alpha)
  }

  override def setAngles(entity: T, limbAngle: Float, limbDistance: Float, customAngle: Float, headYaw: Float, headPitch: Float): Unit = {
    headbone.yaw = toDegrees(headYaw)
    headbone.pitch = toDegrees(headPitch)
    val cosAngle = MathHelper.cos(customAngle * 0.3F)
    tailbone.pitch = -0.1F * cosAngle
    finbone.pitch = -0.2F * cosAngle
    leftsidefin.roll = -0.1F * cosAngle
    rightsidefin.roll = 0.1F * cosAngle
    rightarm.pitch = MathHelper.sin(limbAngle * .3f) * .33f
    leftarm.pitch = MathHelper.cos(limbAngle * .3f) * .33f
  }

  def toDegrees(rad: Float): Float = rad * 0.017453292F
}

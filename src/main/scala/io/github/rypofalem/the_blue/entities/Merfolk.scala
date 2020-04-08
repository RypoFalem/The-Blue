package io.github.rypofalem.the_blue.entities

import io.github.rypofalem.the_blue.TheBlueMod
import net.minecraft.client.model.ModelPart
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.client.render.entity.{EntityRenderDispatcher, MobEntityRenderer}
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.passive.{AbstractTraderEntity, PassiveEntity}
import net.minecraft.entity.{Entity, EntityDimensions, EntityPose, EntityType}
import net.minecraft.util.Identifier
import net.minecraft.village.TradeOffer
import net.minecraft.world.World

class Merfolk(typee:EntityType[Merfolk], world:World) extends AbstractTraderEntity(typee,world) {

  override def afterUsing(offer: TradeOffer): Unit = {}

  override def fillRecipes(): Unit = {}

  override def createChild(mate: PassiveEntity): PassiveEntity = {
    val world = getEntityWorld
    if(world.isClient) return null
    val baby = new Merfolk(getType.asInstanceOf[EntityType[Merfolk]], world)
    baby.setPos(getX,getY,getZ)
    baby
  }

  override protected def getActiveEyeHeight(pose: EntityPose, dimensions: EntityDimensions): Float =
    if (this.isBaby) 0.7F else 1.2F

}

class MerfolkRenderer(erd:EntityRenderDispatcher) extends MobEntityRenderer[Merfolk, MerfolkModel[Merfolk]](erd,new MerfolkModel[Merfolk],.3125f){
  override def getTexture(entity: Merfolk): Identifier = new Identifier(TheBlueMod.modid, "textures/entity/merfolk.png")
}

class MerfolkModel[T<:Entity] extends EntityModel[T]{


  textureWidth = 64
  textureHeight = 64

  val headbone = new ModelPart(this)
  headbone.setPivot(0.0F, 11.5F, 0.0F)
  headbone.setTextureOffset(0,0).addCuboid(-4.0F, -10.5F, -4.0F, 8, 10, 8, 0.0F, false)

  val nosebone = new ModelPart(this)
  nosebone.setPivot(0.0F, -1.5F, -5.0F)
  headbone.addChild(nosebone)
  nosebone.setTextureOffset(24, 0).addCuboid(-1.0F, -2.0F, -1.0F, 2, 4, 2, 0.0F, false)

  val headfin = new ModelPart(this)
  headfin.setPivot(0.0F, -9.0F, 0.0F)
  headbone.addChild(headfin)
  headfin.setTextureOffset(46,48).addCuboid(-0.5F, -4.5F, -2.0F, 1, 8, 8, 0.0F, false)

  val torsobone = new ModelPart(this)
  torsobone.setPivot(0.0F, 18.0F, 0.0F)
  torsobone.setTextureOffset(16,20).addCuboid(-4.0F, -7.0F, -3.0F, 8, 12, 6, 0.5F, false)

  val tailbone = new ModelPart(this)
  tailbone.setPivot(0.0F, 23.0F, 2.0F)
  tailbone.setTextureOffset(33,0).addCuboid( -4.0F, -2.0F, 0.0F, 8, 3, 7, -0.5F, false)

  val finbone = new ModelPart(this)
  finbone.setPivot(0.0F, -0.5F, 6.0F)
  tailbone.addChild(finbone)
  finbone.setTextureOffset(40,13).addCuboid(-5.0F, -0.5F, -1.0F, 10, 1, 4, 0.0F, false)

  override def render(mat: MatrixStack, vc: VertexConsumer, light: Int, overlay: Int, red: Float, green: Float, blue: Float, alpha: Float): Unit = {
    headbone.render(mat, vc, light, overlay, red, green, blue, alpha)
    torsobone.render(mat, vc, light, overlay, red, green, blue, alpha)
    tailbone.render(mat, vc, light, overlay, red, green, blue, alpha)
  }

  override def setAngles(entity: T, limbAngle: Float, limbDistance: Float, customAngle: Float, headYaw: Float, headPitch: Float): Unit = {
    headbone.yaw = headYaw
    headbone.pitch = headPitch
  }

}

package io.github.rypofalem.the_blue.blocks.tiles

import java.util.Random

import io.github.rypofalem.the_blue.TheBlueMod
import io.github.rypofalem.the_blue.inventory.SimpleInventory
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.minecraft.block.{Block, BlockEntityProvider, BlockState, Waterloggable}
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.{BlockEntityRenderDispatcher, BlockEntityRenderer}
import net.minecraft.client.render.model.json.ModelTransformation
import net.minecraft.client.util.math.{MatrixStack, Vector3d, Vector3f}
import net.minecraft.entity.{EntityContext, ExperienceOrbEntity, ItemEntity}
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.fluid.{FluidState, Fluids}
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.{ItemPlacementContext, ItemStack, Items}
import net.minecraft.loot.LootTables
import net.minecraft.loot.context.{LootContext, LootContextParameters, LootContextTypes}
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.StateManager
import net.minecraft.state.property.{BooleanProperty, Properties}
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.{ActionResult, Hand, Tickable}
import net.minecraft.util.math.{BlockPos, Direction}
import net.minecraft.util.shape.VoxelShape
import net.minecraft.world.chunk.{ChunkStatus, WorldChunk}
import net.minecraft.world.{BlockView, IWorld, World}

import scala.jdk.CollectionConverters._

class FishingNetTile extends BlockEntity(TheBlueMod.fishingNetTileType)
  with Tickable with SimpleInventory with SidedInventory with BlockEntityClientSerializable{

  // Time in ticks between fish catching
  // Assumption: minWaitTime < maxWaitTime, this is never enforced
  val minWaitTime:Int = 60 * 20 * 5 // 5 minutes in ticks
  val maxWaitTime:Int = 60 * 20 * 15 // 15 minutes in ticks

  override def getInvSize:Int = 10
  val resetDistance:Int = 15 // must be in range {0 < resetDistance < 16} for resetNearbyTimers
  protected[the_blue] var ticksTilNextFish:Int = calcTicksTilNextFish(new Random())
  protected var luck:Float = 0
  // keep track of the number of slots filled so we don't have to loop through inventory on client render thread
  protected[the_blue] var filledSlots:Int = 0

  protected def calcTicksTilNextFish:Int = calcTicksTilNextFish(getWorld.random)
  protected def calcTicksTilNextFish(rand:Random):Int =
    20 //TODO debug minWaitTime + rand.nextInt(maxWaitTime-minWaitTime) + 1 - timeBonus
  protected def timeBonus:Int = (60 * 20 * luck).toInt // one minute of ticks per unit of luck

  override def tick(): Unit = {
    markDirty() // we mark dirty on every tick because the countdown changes every tick
    ticksTilNextFish -= 1 // countdown
    if(ticksTilNextFish > 0) return // do nothing if countdown not ready
    catchFish() // try to catch a fish
    resetFishingTimer() // reset timer
    resetNearbyNetTimers() // reset timers of nets that are too close as punishment
  }

  // Causes the block to fill the first empty slot with an item from the fishing loot table
  // it then resets the timers of all nearby blocks
  protected[the_blue] def catchFish():Unit = {
    if(world.isClient) return // only works serverside
    if(!world.getBlockState(pos).get(FishingNetBlock.WATERLOGGED)) return // don't catch fish without water!
    for{
      // find the first empty Item index, which is Int component of the tuple
      emptyItem:(ItemStack, Int) <- items.zipWithIndex.find{ case (stack, _) => stack.isEmpty }
      loot = getRandomFishingLoot
      if !loot.isEmpty
    } {
      items(emptyItem._2) = loot
      filledSlots += 1
      sync()
    }
  }

  // reset the timers of nets that are too close as punishment
  protected[the_blue] def resetNearbyNetTimers():Unit = {
    val chunkX = pos.getX >> 4
    val chunkZ = pos.getZ >> 4
    for{
      // 3x3 chunk selection centered on the our chunk
      x <- (chunkX-1) to (chunkX+1)
      z <- (chunkZ-1) to (chunkZ+1)
      tile <- world.getChunk(x, z, ChunkStatus.FULL, true).asInstanceOf[WorldChunk].
        getBlockEntities.values.asScala

      if tile.isInstanceOf[FishingNetTile] &&
        tile != this &&
        tile.getPos.getManhattanDistance(this.getPos) < resetDistance
      tooCloseNet = tile.asInstanceOf[FishingNetTile]
    } tooCloseNet.timerPunishment(20*60*5) // 5 minutes
  }

  // punish the player by extending the timer
  // called when the player checks an empty net
  // called on too-close nets when a net catches a fish
  protected[the_blue] def timerPunishment(ticks:Int):Unit =
    ticksTilNextFish = math.min(ticks+ticksTilNextFish, maxWaitTime)

  // Returns an item from the fishing loot table based on this block's properties
  // The item may be "empty"
  // ItemStack will always be empty if called client side
  def getRandomFishingLoot:ItemStack = {
    if(world.isClient) return ItemStack.EMPTY
    val itemList = getWorld.getServer.getLootManager.getSupplier(LootTables.FISHING_GAMEPLAY).getDrops(getLootContext)
    if(itemList.isEmpty) ItemStack.EMPTY
    else itemList.get(0)
  }

  // Build and return a fishing LootContext based on this block's properties
  def getLootContext: LootContext= {
    new LootContext.Builder(world.asInstanceOf[ServerWorld])
      .setRandom(world.getRandom)
      .put(LootContextParameters.POSITION, getPos)
      .put(LootContextParameters.TOOL, new ItemStack(Items.FISHING_ROD))
      .setLuck(luck)
      .build(LootContextTypes.FISHING)
  }

  protected[the_blue] def resetFishingTimer():Unit = ticksTilNextFish = calcTicksTilNextFish

  override def toTag(tag: CompoundTag): CompoundTag = {
    super.toTag(tag)
    tag.putFloat("luck", luck)
    tag.putInt("ticksTilNextFish", ticksTilNextFish)
    tag.putInt("filledSlots", filledSlots)
    SimpleInventory.toTag(tag, items, setIfEmpty = true)
    tag
  }

  override def fromTag(tag: CompoundTag): Unit = {
    super.fromTag(tag)
    luck = tag.getFloat("luck")
    ticksTilNextFish = tag.getInt("ticksTilNextFish")
    filledSlots = tag.getInt("filledSlots")
    SimpleInventory.fromTag(tag, items)
  }

  // sync server -> clients todo: maybe not all tags need to be synced
  override def fromClientTag(tag: CompoundTag): Unit = fromTag(tag)
  override def toClientTag(tag: CompoundTag): CompoundTag = toTag(tag)

  def getFilledSlots:Int = filledSlots

  // restrict inputting and outputting items from all sides
  override def getInvAvailableSlots(side: Direction): Array[Int] = Array.empty[Int]
  override def canInsertInvStack(slot: Int, stack: ItemStack, dir: Direction): Boolean = false
  override def canExtractInvStack(slot: Int, stack: ItemStack, dir: Direction): Boolean = false
}


// TODO render items in block
class FishingNetBlock(settings:Block.Settings) extends Block(settings) with BlockEntityProvider with Waterloggable{
  setDefaultState(getStateManager.getDefaultState.`with`(FishingNetBlock.WATERLOGGED, java.lang.Boolean.FALSE))
  override def createBlockEntity(blockView:BlockView) = new FishingNetTile

  override def isTranslucent(state: BlockState, view: BlockView, pos: BlockPos): Boolean = true

  override def onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand,
                     hit: BlockHitResult):ActionResult = {
    // do nothing if clientside
    if(world.isClient) return ActionResult.SUCCESS

    // go through the net's inventory and drop all items and xp for the player
    val net = world.getBlockEntity(pos).asInstanceOf[FishingNetTile]
    var lootCount:Int = 0

    // a location in the center of the hit side, .2 blocks extended outwards
    val dropLocation:Vector3d = {
      val side = hit.getSide
      def offset(d:Double):Double = d * 0.7 + .5
      new Vector3d(offset(side.getOffsetX) + pos.getX,
        offset(side.getOffsetY) + pos.getY,
        offset(side.getOffsetZ) + pos.getZ)
    }
    for{
      slot <- 0 until net.getInvSize
      item = net.getInvStack(slot)
      if !item.isEmpty
    } {
        net.setInvStack(slot, ItemStack.EMPTY)
        lootCount += 1
        val worldItem = new ItemEntity(world, dropLocation.x, dropLocation.y, dropLocation.z, item)
        worldItem.setPickupDelay(0)
        world.spawnEntity(worldItem)
    }

    if(lootCount > 0){
      net.sync()
      dropExperience(world, pos, lootCount*3)
    }
    else net.timerPunishment(20*60) // add 1 minute to the timer if the player checked an empty net

    net.filledSlots = 0

    ActionResult.SUCCESS
  }


  override protected def appendProperties(builder: StateManager.Builder[Block, BlockState]): Unit = {
    builder.add(FishingNetBlock.WATERLOGGED)
  }

  override def getFluidState(state: BlockState): FluidState =
    if (state.get(FishingNetBlock.WATERLOGGED)) Fluids.WATER.getStill(false)
    else super.getFluidState(state)

  override def onBlockAdded(state: BlockState, world: World, pos: BlockPos, oldState: BlockState, moved: Boolean): Unit =
    if (!world.isClient) world.getBlockTickScheduler.schedule(pos, this, 1)

  override def getOutlineShape(state: BlockState, view: BlockView, pos: BlockPos, context: EntityContext): VoxelShape =
    FishingNetBlock.SHAPE


  override def getStateForNeighborUpdate(state: BlockState, facing: Direction, neighborState: BlockState, world: IWorld,
                                          pos: BlockPos, neighborPos: BlockPos): BlockState = {
    if (state.get(FishingNetBlock.WATERLOGGED))
      world.getFluidTickScheduler.schedule(pos, Fluids.WATER, Fluids.WATER.getTickRate(world))
    if (!world.isClient) world.getBlockTickScheduler.schedule(pos, this, 1)
    state
  }

  override def getPlacementState(ctx: ItemPlacementContext): BlockState =
    getDefaultState.`with`[java.lang.Boolean,java.lang.Boolean](
      FishingNetBlock.WATERLOGGED,
      ctx.getWorld.getFluidState(ctx.getBlockPos).getFluid == Fluids.WATER
    )
}

object FishingNetBlock{
  val WATERLOGGED:BooleanProperty = Properties.WATERLOGGED
  val SHAPE:VoxelShape = Block.createCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D)
}

class FishingNetRenderer(dispatcher:BlockEntityRenderDispatcher)
  extends BlockEntityRenderer[FishingNetTile](dispatcher){
  val fullCircle:Float = 2f * math.Pi.toFloat
  val quarterCircle:Float = math.Pi.toFloat / 2f
  val radius:Float = 4f / 16f // 4 pixels

  override def render(net: FishingNetTile, tickDelta: Float, matrices: MatrixStack,
                      vertexConsumers: VertexConsumerProvider, light: Int, overlay: Int): Unit = {
    val time:Float = net.getWorld.getTime + tickDelta
    val itemCount = net.filledSlots
    val separationRadians = fullCircle / itemCount // how many radians apart items are


    for(i <- 0 until itemCount){
      val stack = net.getInvStack(i)
      val rotation = separationRadians * i +
        time / 15
      matrices.push()
      // move to center of block and then outward in a circle
      matrices.translate(
        radius*math.sin(rotation) + .5f,
        .3f, // items are tall
        radius*math.cos(rotation) + .5f)
      // rotate to match it's position, then an extra quarter circle so that it's edge points to the center
      matrices.multiply(Vector3f.POSITIVE_Y.getRadialQuaternion(rotation + quarterCircle) )
      MinecraftClient.getInstance.getItemRenderer.
        renderItem(stack, ModelTransformation.Mode.GROUND, light, overlay, matrices, vertexConsumers)
      matrices.pop()
    }
  }
}

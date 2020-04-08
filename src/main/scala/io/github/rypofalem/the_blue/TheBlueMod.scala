package io.github.rypofalem.the_blue

import io.github.rypofalem.the_blue.TheBlueMod.{fishingNetBlock, fishingNetTileType}
import io.github.rypofalem.the_blue.blocks.tiles.{FishingNetBlock, FishingNetItem, FishingNetRenderer, FishingNetTile}
import io.github.rypofalem.the_blue.entities.{Merfolk, MerfolkRenderer}
import net.fabricmc.api.{ClientModInitializer, ModInitializer}
import net.fabricmc.fabric.api.block.FabricBlockSettings
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder
import net.fabricmc.fabric.api.client.rendereregistry.v1.{BlockEntityRendererRegistry, EntityRendererRegistry}
import net.fabricmc.fabric.api.entity.FabricEntityTypeBuilder
import net.minecraft.block.entity.{BlockEntity, BlockEntityType}
import net.minecraft.block.{Block, Material}
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.entity.EntityRenderDispatcher
import net.minecraft.entity.{Entity, EntityCategory, EntityDimensions, EntityType}
import net.minecraft.item.{BlockItem, Item, ItemGroup, ItemStack}
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import net.minecraft.world.World

// todo dolphin saddle
object TheBlueMod extends ModInitializer {

  val modid: String = "the_blue"
  val itemGroup: ItemGroup = FabricItemGroupBuilder.build(
    new Identifier(modid, "the_blue"), () => new ItemStack(fishingNetBlock))


  val fishingNetBlock: FishingNetBlock =
    new FishingNetBlock(FabricBlockSettings.of(Material.WOOL).nonOpaque().sounds(BlockSoundGroup.WOOL).build)
  val fishingNetTileType: BlockEntityType[FishingNetTile] =
    BlockEntityType.Builder.create(() => new FishingNetTile, fishingNetBlock).build(null)
  val fishingNetItem: BlockItem = new FishingNetItem(fishingNetBlock, new Item.Settings().group(itemGroup))

  val kelpStringItem: Item = new Item(new Item.Settings().group(itemGroup))

  val merfolkType: EntityType[Merfolk] = FabricEntityTypeBuilder.
    create[Merfolk](EntityCategory.WATER_CREATURE, (t: EntityType[Merfolk], w: World) => new Merfolk(t, w)).
    size(EntityDimensions.fixed(.625f, 1.875f)).
    build()

  override def onInitialize(): Unit = {
    registerBlock("fishingnet", fishingNetBlock)
    registerItem("fishingnet", fishingNetItem)
    registerTile("fishingnet", fishingNetTileType)
    registerItem("kelp_string", kelpStringItem)
    registerEntity("merfolk", merfolkType)
  }

  private def registerEntity[T <: Entity](name: String, entityType: EntityType[T]): EntityType[T] =
    Registry.register(Registry.ENTITY_TYPE, new Identifier(modid, name), entityType)

  private def registerTile[A <: BlockEntity](name: String, tileType: BlockEntityType[A]): BlockEntityType[A] =
    Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(modid, name), tileType)

  private def registerBlock(name: String, block: Block): Block =
    Registry.register(Registry.BLOCK, new Identifier(modid, name), fishingNetBlock)

  private def registerItem(name: String, item: Item): Item =
    Registry.register(Registry.ITEM, new Identifier(modid, name), item)
}

object TheBlueClient extends ClientModInitializer {
  override def onInitializeClient(): Unit = {
    BlockRenderLayerMap.INSTANCE.putBlock(fishingNetBlock, RenderLayer.getTranslucent)
    BlockEntityRendererRegistry.INSTANCE.register[FishingNetTile](fishingNetTileType, x => new FishingNetRenderer(x))
    EntityRendererRegistry.INSTANCE.register(
      TheBlueMod.merfolkType, { (erd: EntityRenderDispatcher, _) => new MerfolkRenderer(erd) })
  }
}
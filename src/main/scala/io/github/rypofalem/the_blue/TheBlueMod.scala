package io.github.rypofalem.the_blue


import io.github.rypofalem.the_blue.blocks.tiles.{FishingNetBlock, FishingNetRenderer, FishingNetTile}
import net.fabricmc.api.{ClientModInitializer, ModInitializer}
import net.fabricmc.fabric.api.block.FabricBlockSettings
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder
import net.minecraft.block.{Block, Material}
import net.minecraft.block.entity.{BlockEntity, BlockEntityType}
import net.minecraft.client.render.RenderLayer
import net.minecraft.item.{BlockItem, Item, ItemGroup, ItemStack}
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

// todo dolphin saddle
object TheBlueMod extends ModInitializer with ClientModInitializer{

  val modid:String = "the_blue"
  val itemGroup:ItemGroup = FabricItemGroupBuilder.build(
    new Identifier(modid, "the_blue"), () => new ItemStack(fishingNetBlock))


  val fishingNetBlock:Block =
    new FishingNetBlock(FabricBlockSettings.of(Material.WOOL).nonOpaque().sounds(BlockSoundGroup.WOOL).build)
  val fishingNetTileType:BlockEntityType[FishingNetTile] =
    BlockEntityType.Builder.create(() => new FishingNetTile, fishingNetBlock).build(null)
  val fishingNetItem:BlockItem = new BlockItem(fishingNetBlock, new Item.Settings().group(itemGroup))

  override def onInitialize(): Unit = {
    registerBlock("fishingnet", fishingNetBlock)
    registerBlockItem("fishingnet", fishingNetItem)
    registerTile("fishingnet", fishingNetTileType)
  }

  override def onInitializeClient(): Unit = {
    BlockRenderLayerMap.INSTANCE.putBlock(fishingNetBlock, RenderLayer.getTranslucent)
    import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry
    BlockEntityRendererRegistry.INSTANCE.register[FishingNetTile](fishingNetTileType, x => new FishingNetRenderer(x) )
  }

  private def registerTile[A<:BlockEntity](name:String, tileType:BlockEntityType[A] ): BlockEntityType[A] = {
    Registry.register(
      Registry.BLOCK_ENTITY_TYPE,
      new Identifier(modid, name),
      tileType)
  }

  private def registerBlock(name:String, block:Block): Block ={
    Registry.register(Registry.BLOCK,
      new Identifier(modid, name), fishingNetBlock)
  }

  private def registerBlockItem(name:String, blockItem: BlockItem):Item = {
    Registry.register(Registry.ITEM, new Identifier (modid, name), blockItem)
  }
}

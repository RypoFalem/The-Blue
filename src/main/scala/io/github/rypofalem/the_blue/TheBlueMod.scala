package io.github.rypofalem.the_blue

import io.github.rypofalem.the_blue.blocks.tiles.{FishingNetBlock, FishingNetTile}
import net.fabricmc.api.{ClientModInitializer, ModInitializer}
import net.fabricmc.fabric.api.block.FabricBlockSettings
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.minecraft.block.{Block, Material}
import net.minecraft.block.entity.{BlockEntity, BlockEntityType}
import net.minecraft.client.render.RenderLayer
import net.minecraft.item.{BlockItem, Item, ItemGroup}
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

// todo translation
// todo dolphin saddle
object TheBlueMod extends ModInitializer with ClientModInitializer{

  val modid:String = "the_blue"
  val fishingNetBlock:Block = new FishingNetBlock(FabricBlockSettings.of(Material.WOOL).nonOpaque().sounds(BlockSoundGroup.WOOL).build)
  val fishingNetTileType:BlockEntityType[FishingNetTile] =
    BlockEntityType.Builder.create(() => new FishingNetTile, fishingNetBlock).build(null)

  override def onInitialize(): Unit = {
    registerBlock("fishingnet", fishingNetBlock)
    registerBlockItem("fishingnet", fishingNetBlock)
    registerTile("fishingnet", fishingNetTileType)
  }

  override def onInitializeClient(): Unit = {
    BlockRenderLayerMap.INSTANCE.putBlock(fishingNetBlock, RenderLayer.getTranslucent)
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

  private def registerBlockItem(name:String, block:Block):Item = {
    Registry.register(
      Registry.ITEM,
      new Identifier (modid, name),
      new BlockItem (block, new Item.Settings().group(ItemGroup.MISC)))
  }
}

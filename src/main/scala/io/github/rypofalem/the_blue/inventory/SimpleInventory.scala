package io.github.rypofalem.the_blue.inventory

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.{CompoundTag, ListTag}

import scala.collection.immutable.HashMap

// A simple Inventory trait for easy reusable implementation
// concrete classes and objects still need to implement getInvSize:Int and markDirty():Unit
// todo handle overriding getInvMaxStackAmount:Int by ensuring that is enforced
trait SimpleInventory extends Inventory {
  protected val items: Array[ItemStack] = new Array[ItemStack](size) map { _ => ItemStack.EMPTY }

  override def isEmpty: Boolean = items.find(!_.isEmpty) match {
    case Some(_) => false // if we found an item that is not empty, return false
    case _ => true // all slots contained an item, return true
  }

  // return a copy of the item in the slot
  // to modify the slot use setInvStack
  // todo Will only sending copies cause issues?
  // todo It seems dangerous to send callers mutable ItemStacks without knowing when to mark dirty in case they change it
  override def getStack(slot: Int): ItemStack = items(slot).copy()

  // empty the item slot and return the entire stack
  override def removeStack(slot: Int): ItemStack = {
    markDirty()
    val item = getStack(slot)
    items(slot) = ItemStack.EMPTY
    item
  }

  // take a number of items from the given slot
  // if the slot doesn't contain enough items, take the whole itemstack instead
  override def removeStack(slot: Int, amount: Int): ItemStack = {
    markDirty()
    val item = getStack(slot)
    if (item.getCount > amount) {
      // reduce the itemstack count and return a new itemstack with that amount
      item.setCount(item.getCount - amount)
      new ItemStack(item.getItem, amount)
    } else removeStack(slot)
  }

  override def setStack(slot: Int, stack: ItemStack): Unit = {
    markDirty()
    items(slot) = stack
  }

  override def canPlayerUse(player: PlayerEntity): Boolean = true

  override def clear(): Unit = {
    markDirty()
    for (i <- items.indices) items(i) = ItemStack.EMPTY
  }

  // computes a string listing all non-empty items with their amounts
  // example output: {(bow,1), (saddle,1), (lily_pad,1), (pufferfish,1), (cod,6)}
  def inventoryToString: String = {
    val builder = new StringBuilder("{")
    val iterator = getItemCounts.iterator
    if (iterator.hasNext) builder.append(iterator.next().toString)
    while (iterator.hasNext) builder.append(s", ${iterator.next().toString}")
    builder.append("}")
    builder.result()
  }

  // get a map of item types and the number of that item present in the inventory
  def getItemCounts: Map[Item, Int] = {
    items.foldLeft(new HashMap[Item, Int]()) {
      (countMap, itemStack) =>
        if (itemStack.isEmpty) countMap
        else countMap.updated(itemStack.getItem, countMap.getOrElse(itemStack.getItem, 0) + itemStack.getCount)
    }
  }
}

object SimpleInventory {
  def fromTag(tag: CompoundTag, items: Array[ItemStack]): Array[ItemStack] = {
    val listTag = tag.getList("Items", 10)
    for {
      listTagIndex <- 0 until listTag.size
      compoundTag = listTag.getCompound(listTagIndex)
      slot = compoundTag.getByte("Slot") & 255
      if slot >= 0 && slot < items.length
    } items(slot) = ItemStack.fromTag(compoundTag)
    items
  }

  def toTag(tag: CompoundTag, stacks: Array[ItemStack], setIfEmpty: Boolean): CompoundTag = {
    val listTag = new ListTag
    for {
      slot <- stacks.indices
      itemStack = stacks(slot)
      if !itemStack.isEmpty
    } {
      val compoundTag = new CompoundTag
      compoundTag.putByte("Slot", slot.toByte)
      itemStack.toTag(compoundTag)
      listTag.add(compoundTag)
    }
    if (!listTag.isEmpty || setIfEmpty) tag.put("Items", listTag)
    tag
  }
}

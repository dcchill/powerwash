/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.powerwash.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

import net.mcreator.powerwash.item.PowerwasherItem;
import net.mcreator.powerwash.PowerwashMod;

public class PowerwashModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(PowerwashMod.MODID);
	public static final DeferredItem<Item> POWERWASHER = REGISTRY.register("powerwasher", PowerwasherItem::new);
	public static final DeferredItem<Item> GRIME = block(PowerwashModBlocks.GRIME);

	// Start of user code block custom items
	// End of user code block custom items
	private static DeferredItem<Item> block(DeferredHolder<Block, Block> block) {
		return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
	}
}

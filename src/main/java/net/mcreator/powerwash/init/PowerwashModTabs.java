/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.powerwash.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

import net.mcreator.powerwash.PowerwashMod;

public class PowerwashModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PowerwashMod.MODID);
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> POWERWASH = REGISTRY.register("powerwash",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.powerwash.powerwash")).icon(() -> new ItemStack(PowerwashModItems.POWERWASHER.get())).displayItems((parameters, tabData) -> {
				tabData.accept(PowerwashModItems.POWERWASHER_WIDE.get());
				tabData.accept(PowerwashModItems.POWERWASHER_MED.get());
			}).build());
}
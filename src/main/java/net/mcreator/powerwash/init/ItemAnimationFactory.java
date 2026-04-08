package net.mcreator.powerwash.init;

import software.bernie.geckolib.animatable.GeoItem;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;

import net.mcreator.powerwash.item.PowerwasherWideItem;
import net.mcreator.powerwash.item.PowerwasherMedItem;
import net.mcreator.powerwash.item.PowerwasherItem;

@EventBusSubscriber
public class ItemAnimationFactory {
	@SubscribeEvent
	public static void animatedItems(PlayerTickEvent.Post event) {
		String animation = "";
		ItemStack mainhandItem = event.getEntity().getMainHandItem().copy();
		ItemStack offhandItem = event.getEntity().getOffhandItem().copy();
		if (mainhandItem.getItem() instanceof GeoItem || offhandItem.getItem() instanceof GeoItem) {
			if (mainhandItem.getItem() instanceof PowerwasherItem animatable) {
				animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
				if (!animation.isEmpty()) {
					CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), tag -> tag.putString("geckoAnim", ""));
					if (event.getEntity().level().isClientSide()) {
						((PowerwasherItem) event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
					}
				}
			}
			if (offhandItem.getItem() instanceof PowerwasherItem animatable) {
				animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
				if (!animation.isEmpty()) {
					CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), tag -> tag.putString("geckoAnim", ""));
					if (event.getEntity().level().isClientSide()) {
						((PowerwasherItem) event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
					}
				}
			}
			if (mainhandItem.getItem() instanceof PowerwasherWideItem animatable) {
				animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
				if (!animation.isEmpty()) {
					CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), tag -> tag.putString("geckoAnim", ""));
					if (event.getEntity().level().isClientSide()) {
						((PowerwasherWideItem) event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
					}
				}
			}
			if (offhandItem.getItem() instanceof PowerwasherWideItem animatable) {
				animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
				if (!animation.isEmpty()) {
					CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), tag -> tag.putString("geckoAnim", ""));
					if (event.getEntity().level().isClientSide()) {
						((PowerwasherWideItem) event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
					}
				}
			}
			if (mainhandItem.getItem() instanceof PowerwasherMedItem animatable) {
				animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
				if (!animation.isEmpty()) {
					CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), tag -> tag.putString("geckoAnim", ""));
					if (event.getEntity().level().isClientSide()) {
						((PowerwasherMedItem) event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
					}
				}
			}
			if (offhandItem.getItem() instanceof PowerwasherMedItem animatable) {
				animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
				if (!animation.isEmpty()) {
					CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), tag -> tag.putString("geckoAnim", ""));
					if (event.getEntity().level().isClientSide()) {
						((PowerwasherMedItem) event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
					}
				}
			}
		}
	}
}
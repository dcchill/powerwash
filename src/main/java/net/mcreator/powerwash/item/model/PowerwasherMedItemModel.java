package net.mcreator.powerwash.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import net.mcreator.powerwash.item.PowerwasherMedItem;

public class PowerwasherMedItemModel extends GeoModel<PowerwasherMedItem> {
	@Override
	public ResourceLocation getAnimationResource(PowerwasherMedItem animatable) {
		return ResourceLocation.parse("powerwash:animations/powerwashermed.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(PowerwasherMedItem animatable) {
		return ResourceLocation.parse("powerwash:geo/powerwashermed.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(PowerwasherMedItem animatable) {
		return ResourceLocation.parse("powerwash:textures/item/powerwash_texture.png");
	}
}
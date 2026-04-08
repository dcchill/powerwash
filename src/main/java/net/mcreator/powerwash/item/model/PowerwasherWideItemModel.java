package net.mcreator.powerwash.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import net.mcreator.powerwash.item.PowerwasherWideItem;

public class PowerwasherWideItemModel extends GeoModel<PowerwasherWideItem> {
	@Override
	public ResourceLocation getAnimationResource(PowerwasherWideItem animatable) {
		return ResourceLocation.parse("powerwash:animations/powerwasherwide.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(PowerwasherWideItem animatable) {
		return ResourceLocation.parse("powerwash:geo/powerwasherwide.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(PowerwasherWideItem animatable) {
		return ResourceLocation.parse("powerwash:textures/item/powerwash_texture.png");
	}
}
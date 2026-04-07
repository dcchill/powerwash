package net.mcreator.powerwash.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import net.mcreator.powerwash.item.PowerwasherItem;

public class PowerwasherItemModel extends GeoModel<PowerwasherItem> {
	@Override
	public ResourceLocation getAnimationResource(PowerwasherItem animatable) {
		return ResourceLocation.parse("powerwash:animations/.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(PowerwasherItem animatable) {
		return ResourceLocation.parse("powerwash:geo/");
	}

	@Override
	public ResourceLocation getTextureResource(PowerwasherItem animatable) {
		return ResourceLocation.parse("powerwash:textures/item/minecraft:amethyst_shard.png");
	}
}
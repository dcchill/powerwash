package net.mcreator.powerwash.item;

import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.GeoItem;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;

import net.mcreator.powerwash.item.renderer.PowerwasherMedItemRenderer;

import java.util.function.Consumer;

public class PowerwasherMedItem extends PowerwasherItem implements GeoItem {
	private static final int MED_MAX_WATER = 10000;
	private static final int MED_WATER_PER_TICK = 1;
	private static final int MED_MAX_RANGE = 6;
	private static final int MED_SPRAY_WIDTH = 4;
	private static final int MED_SPRAY_HEIGHT = 2;

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public PowerwasherMedItem() {
		super();
	}

	@Override
	protected int getMaxWater() { return MED_MAX_WATER; }
	@Override
	protected int getWaterPerTick() { return MED_WATER_PER_TICK; }
	@Override
	protected int getMaxRange() { return MED_MAX_RANGE; }
	@Override
	protected int getSprayWidth() { return MED_SPRAY_WIDTH; }
	@Override
	protected int getSprayHeight() { return MED_SPRAY_HEIGHT; }

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return false;
	}

	@Override
	public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
		consumer.accept(new GeoRenderProvider() {
			private PowerwasherMedItemRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
				if (this.renderer == null)
					this.renderer = new PowerwasherMedItemRenderer();
				return this.renderer;
			}
		});
	}

	private PlayState idlePredicate(AnimationState event) {
		if (this.animationprocedure.equals("empty")) {
			event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
			return PlayState.CONTINUE;
		}
		return PlayState.STOP;
	}

	String prevAnim = "empty";

	private PlayState procedurePredicate(AnimationState event) {
		if (!this.animationprocedure.equals("empty") && event.getController().getAnimationState() == AnimationController.State.STOPPED || (!this.animationprocedure.equals(prevAnim) && !this.animationprocedure.equals("empty"))) {
			if (!this.animationprocedure.equals(prevAnim))
				event.getController().forceAnimationReset();
			event.getController().setAnimation(RawAnimation.begin().thenPlay(this.animationprocedure));
			if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
				this.animationprocedure = "empty";
				event.getController().forceAnimationReset();
			}
		} else if (this.animationprocedure.equals("empty")) {
			prevAnim = "empty";
			return PlayState.STOP;
		}
		prevAnim = this.animationprocedure;
		return PlayState.CONTINUE;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar data) {
		AnimationController procedureController = new AnimationController(this, "procedureController", 0, this::procedurePredicate);
		data.add(procedureController);
		AnimationController idleController = new AnimationController(this, "idleController", 0, this::idlePredicate);
		data.add(idleController);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}

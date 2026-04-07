package net.mcreator.powerwash.item;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.util.Mth;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.component.DataComponents;

import net.mcreator.powerwash.item.renderer.PowerwasherItemRenderer;
import net.mcreator.powerwash.world.DirtyBlockManager;

import java.util.function.Consumer;

public class PowerwasherItem extends Item implements GeoItem {
	private static final String WATER_KEY = "Water";
	private static final int MAX_WATER = 1000;
	private static final int WATER_PER_TICK = 1;
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	public String animationprocedure = "empty";

	public PowerwasherItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null || !player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}

		ItemStack stack = context.getItemInHand();
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		FluidState fluidState = level.getFluidState(pos);
		if (fluidState.is(FluidTags.WATER) && fluidState.isSource()) {
			if (!level.isClientSide()) {
				level.removeBlock(pos, false);
				setWater(stack, MAX_WATER);
			}
			return InteractionResult.sidedSuccess(level.isClientSide());
		}

		return InteractionResult.PASS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isShiftKeyDown()) {
			return InteractionResultHolder.pass(stack);
		}
		if (getWater(stack) <= 0) {
			return InteractionResultHolder.fail(stack);
		}
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 72000;
	}

	@Override
	public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
		if (getWater(stack) <= 0) {
			livingEntity.stopUsingItem();
			return;
		}

		Vec3 eye = livingEntity.getEyePosition();
		Vec3 look = livingEntity.getLookAngle();
		Vec3 end = eye.add(look.scale(8.0));
		BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, livingEntity));

		spawnSpray(level, eye, look, hit.getLocation());

		if (!level.isClientSide() && hit.getType() == HitResult.Type.BLOCK) {
			boolean cleaned = DirtyBlockManager.cleanPixel(hit.getBlockPos(), hit.getDirection(), hit.getLocation());
			if (cleaned || remainingUseDuration % 2 == 0) {
				consumeWater(stack, WATER_PER_TICK);
			}
		}
	}

	private static void spawnSpray(Level level, Vec3 start, Vec3 direction, Vec3 hitLocation) {
		Vec3 delta = hitLocation.subtract(start);
		double length = delta.length();
		if (length <= 0.001) {
			return;
		}
		Vec3 normalized = delta.normalize();
		for (int i = 0; i < 10; i++) {
			double progress = (i + 1) / 10.0;
			Vec3 point = start.add(normalized.scale(length * progress));
			if (level instanceof ServerLevel serverLevel) {
				serverLevel.sendParticles(ParticleTypes.SPLASH, point.x, point.y, point.z, 1, 0.03, 0.03, 0.03, 0.01);
			} else {
				level.addParticle(ParticleTypes.SPLASH, point.x, point.y, point.z, direction.x * 0.02, direction.y * 0.02, direction.z * 0.02);
			}
		}
	}

	private static int getWater(ItemStack stack) {
		CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		if (!customData.copyTag().contains(WATER_KEY)) {
			setWater(stack, MAX_WATER);
			return MAX_WATER;
		}
		return customData.copyTag().getInt(WATER_KEY);
	}

	private static void setWater(ItemStack stack, int value) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(WATER_KEY, Mth.clamp(value, 0, MAX_WATER)));
	}

	private static void consumeWater(ItemStack stack, int amount) {
		setWater(stack, getWater(stack) - amount);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return getWater(stack) < MAX_WATER;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13.0F * ((float) getWater(stack) / (float) MAX_WATER));
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0x3A86FF;
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return false;
	}

	@Override
	public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
		consumer.accept(new GeoRenderProvider() {
			private PowerwasherItemRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
				if (this.renderer == null)
					this.renderer = new PowerwasherItemRenderer();
				return this.renderer;
			}
		});
	}

	private PlayState idlePredicate(AnimationState event) {
		if (this.animationprocedure.equals("empty")) {
			event.getController().setAnimation(RawAnimation.begin().thenLoop(""));
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

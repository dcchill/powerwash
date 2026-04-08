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
import net.minecraft.core.Direction;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.mcreator.powerwash.PowerwashMod;
import net.mcreator.powerwash.item.renderer.PowerwasherItemRenderer;
import net.mcreator.powerwash.init.PowerwashModDataComponents;
import net.mcreator.powerwash.world.DirtyBlockManager;

import java.util.function.Consumer;

public class PowerwasherItem extends Item implements GeoItem {
	protected static final int MAX_WATER = 10000;
	protected static final int WATER_PER_TICK = 1;
	protected static final int MAX_RANGE = 8;
	protected static final int SPRAY_WIDTH = 2;
	protected static final int SPRAY_HEIGHT = 2;

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	// Overridable getters for subclassing
	protected int getMaxWater() { return MAX_WATER; }
	protected int getWaterPerTick() { return WATER_PER_TICK; }
	protected int getMaxRange() { return MAX_RANGE; }
	protected int getSprayWidth() { return SPRAY_WIDTH; }
	protected int getSprayHeight() { return SPRAY_HEIGHT; }
	public String animationprocedure = "empty";

	public PowerwasherItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		PowerwashMod.LOGGER.info("[Powerwasher] useOn called. Shift: {}", player != null && player.isShiftKeyDown());
		
		if (player == null || !player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}

		ItemStack stack = context.getItemInHand();
		Level level = context.getLevel();
		BlockPos hitPos = context.getClickedPos();
		FluidState fluidState = level.getFluidState(hitPos);

		// If hit pos is empty (e.g., hit block under water), check the clicked face direction
		if (fluidState.isEmpty()) {
			hitPos = context.getClickedPos().relative(context.getClickedFace());
			fluidState = level.getFluidState(hitPos);
		}

		if (fluidState.is(FluidTags.WATER)) {
			if (!level.isClientSide()) {
				level.setBlock(hitPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
				setWater(stack, getMaxWater());
				PowerwashMod.LOGGER.info("[Powerwasher] Refilled successfully!");
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
		Vec3 end = eye.add(look.scale(getMaxRange()));
		BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, livingEntity));

		spawnSpray(level, hit.getLocation());

		if (!level.isClientSide() && hit.getType() == HitResult.Type.BLOCK) {
			ServerLevel serverLevel = (ServerLevel) level;
			boolean anyCleaned = cleanArea(hit, serverLevel);
			if (anyCleaned || remainingUseDuration % 2 == 0) {
				consumeWater(stack, getWaterPerTick());
			}
		}
	}

	protected boolean cleanArea(BlockHitResult hit, ServerLevel serverLevel) {
		BlockPos pos = hit.getBlockPos();
		Direction face = hit.getDirection();
		Vec3 hitLocation = hit.getLocation();
		int width = getSprayWidth();
		int height = getSprayHeight();
		boolean anyCleaned = false;

		// Calculate the starting pixel from the hit location
		int startPixelX;
		int startPixelY;

		double localX = Mth.clamp(hitLocation.x - pos.getX(), 0.0, 0.9999);
		double localY = Mth.clamp(hitLocation.y - pos.getY(), 0.0, 0.9999);
		double localZ = Mth.clamp(hitLocation.z - pos.getZ(), 0.0, 0.9999);

		switch (face) {
			case UP, DOWN -> {
				startPixelX = (int) (localX * 16);
				startPixelY = (int) (localZ * 16);
			}
			case NORTH, SOUTH -> {
				startPixelX = (int) (localX * 16);
				startPixelY = (int) (localY * 16);
			}
			case WEST, EAST -> {
				startPixelX = (int) (localZ * 16);
				startPixelY = (int) (localY * 16);
			}
			default -> {
				startPixelX = 0;
				startPixelY = 0;
			}
		}

		// Center the spray around the hit point
		int halfW = width / 2;
		int halfH = height / 2;

		for (int dy = 0; dy < height; dy++) {
			for (int dx = 0; dx < width; dx++) {
				int px = Mth.clamp(startPixelX - halfW + dx, 0, 15);
				int py = Mth.clamp(startPixelY - halfH + dy, 0, 15);
				int pixelIndex = py * 16 + px;
				if (DirtyBlockManager.cleanPixelByIndex(pos, face, pixelIndex, serverLevel)) {
					anyCleaned = true;
				}
			}
		}

		return anyCleaned;
	}

	private static void spawnSpray(Level level, Vec3 hitLocation) {
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.SPLASH, hitLocation.x, hitLocation.y, hitLocation.z, 5, 0.1, 0.1, 0.1, 0.05);
		} else {
			level.addParticle(ParticleTypes.SPLASH, hitLocation.x, hitLocation.y, hitLocation.z, 0, 0.1, 0);
		}
	}

	private int getWater(ItemStack stack) {
		return stack.getOrDefault(PowerwashModDataComponents.WATER.get(), getMaxWater());
	}

	private void setWater(ItemStack stack, int value) {
		stack.set(PowerwashModDataComponents.WATER.get(), Mth.clamp(value, 0, getMaxWater()));
	}

	private void consumeWater(ItemStack stack, int amount) {
		setWater(stack, getWater(stack) - amount);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return getWater(stack) < getMaxWater();
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13.0F * ((float) getWater(stack) / (float) getMaxWater()));
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

package net.mcreator.powerwash.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;

import net.mcreator.powerwash.PowerwashMod;
import net.mcreator.powerwash.world.DirtyBlockManager;

public class GrimeBlock extends Block {
	public GrimeBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.GRAVEL).strength(1f, 10f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 15;
	}

	@Override
	public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (!level.isClientSide()) {
			BlockPos targetPos = null;
			Direction clickedFace = null;

			// Calculate the direction from the player's eyes to the Grime block
			Vec3 eyePos = placer.getEyePosition();
			Vec3 toGrime = Vec3.atCenterOf(pos).subtract(eyePos).normalize();

			double bestDot = -1.0;

			// Find the solid neighbor that is most aligned with the direction the player is facing/placing
			for (Direction dir : Direction.values()) {
				BlockPos neighbor = pos.relative(dir);
				if (!level.getBlockState(neighbor).isAir()) {
					Vec3 dirVec = Vec3.atLowerCornerOf(dir.getNormal());
					// We want the neighbor that is in the direction of 'toGrime'
					double dot = dirVec.dot(toGrime);
					if (dot > bestDot) {
						bestDot = dot;
						targetPos = neighbor;
						clickedFace = dir.getOpposite();
					}
				}
			}

			// Fallback: If dot product failed (e.g. player inside block), find closest solid neighbor
			if (targetPos == null) {
				double bestDist = Double.MAX_VALUE;
				for (Direction dir : Direction.values()) {
					BlockPos neighbor = pos.relative(dir);
					if (!level.getBlockState(neighbor).isAir()) {
						double dist = eyePos.distanceToSqr(Vec3.atCenterOf(neighbor));
						if (dist < bestDist) {
							bestDist = dist;
							targetPos = neighbor;
						}
					}
				}
				// Recalculate face based on closest neighbor
				if (targetPos != null) {
					clickedFace = Direction.getNearest(
						pos.getX() - targetPos.getX(),
						pos.getY() - targetPos.getY(),
						pos.getZ() - targetPos.getZ()
					);
				}
			}

			if (targetPos == null) {
				level.removeBlock(pos, false);
				PowerwashMod.LOGGER.warn("[GrimeBlock] No target found for Grime at {}, removing.", pos);
				return;
			}

			PowerwashMod.LOGGER.info("[GrimeBlock] Placed at {} → target {} face={}", pos, targetPos, clickedFace);

			DirtyBlockManager.setDirtiest(targetPos, clickedFace, (ServerLevel) level);
			level.removeBlock(pos, false);
		}
	}
}

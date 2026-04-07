package net.mcreator.powerwash.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DirtyBlockManager {
	private static final int PIXELS = 16;
	private static final int PIXEL_COUNT = PIXELS * PIXELS;
	private static final Map<Long, DirtyBlockState> DIRTY_BLOCKS = new ConcurrentHashMap<>();

	private DirtyBlockManager() {
	}

	public static void setDirtiest(BlockPos pos) {
		DIRTY_BLOCKS.put(pos.asLong(), DirtyBlockState.full());
	}

	public static boolean isDirty(BlockPos pos) {
		DirtyBlockState state = DIRTY_BLOCKS.get(pos.asLong());
		return state != null && !state.isClean();
	}

	public static boolean cleanPixel(BlockPos pos, Direction face, Vec3 hitLocation) {
		DirtyBlockState state = DIRTY_BLOCKS.get(pos.asLong());
		if (state == null) {
			return false;
		}
		int pixelIndex = state.getPixelIndex(face, hitLocation, pos);
		boolean changed = state.clean(face, pixelIndex);
		if (state.isClean()) {
			DIRTY_BLOCKS.remove(pos.asLong());
		}
		return changed;
	}

	public static float getDirtyRatio(BlockPos pos) {
		DirtyBlockState state = DIRTY_BLOCKS.get(pos.asLong());
		if (state == null) {
			return 0.0F;
		}
		return state.getDirtyRatio();
	}

	private static final class DirtyBlockState {
		private final boolean[][] facePixels = new boolean[6][PIXEL_COUNT];

		static DirtyBlockState full() {
			DirtyBlockState state = new DirtyBlockState();
			for (int face = 0; face < state.facePixels.length; face++) {
				for (int i = 0; i < PIXEL_COUNT; i++) {
					state.facePixels[face][i] = true;
				}
			}
			return state;
		}

		boolean clean(Direction face, int pixelIndex) {
			boolean[] pixels = facePixels[face.get3DDataValue()];
			if (!pixels[pixelIndex]) {
				return false;
			}
			pixels[pixelIndex] = false;
			return true;
		}

		int getPixelIndex(Direction face, Vec3 hitLocation, BlockPos blockPos) {
			double localX = hitLocation.x - blockPos.getX();
			double localY = hitLocation.y - blockPos.getY();
			double localZ = hitLocation.z - blockPos.getZ();

			double u;
			double v;
			switch (face) {
				case DOWN -> {
					u = localX;
					v = 1.0 - localZ;
				}
				case UP -> {
					u = localX;
					v = localZ;
				}
				case NORTH -> {
					u = 1.0 - localX;
					v = 1.0 - localY;
				}
				case SOUTH -> {
					u = localX;
					v = 1.0 - localY;
				}
				case WEST -> {
					u = localZ;
					v = 1.0 - localY;
				}
				default -> {
					u = 1.0 - localZ;
					v = 1.0 - localY;
				}
			}

			int px = net.minecraft.util.Mth.clamp((int) (u * PIXELS), 0, PIXELS - 1);
			int py = net.minecraft.util.Mth.clamp((int) (v * PIXELS), 0, PIXELS - 1);
			return py * PIXELS + px;
		}

		boolean isClean() {
			for (boolean[] pixels : facePixels) {
				for (boolean pixel : pixels) {
					if (pixel) {
						return false;
					}
				}
			}
			return true;
		}

		float getDirtyRatio() {
			int dirtyPixels = 0;
			for (boolean[] pixels : facePixels) {
				for (boolean pixel : pixels) {
					if (pixel) {
						dirtyPixels++;
					}
				}
			}
			return (float) dirtyPixels / (float) (6 * PIXEL_COUNT);
		}
	}
}

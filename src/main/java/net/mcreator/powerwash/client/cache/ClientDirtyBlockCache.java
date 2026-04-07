package net.mcreator.powerwash.client.cache;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientDirtyBlockCache {
	private static final int PIXELS = 16;
	private static final int PIXEL_COUNT = PIXELS * PIXELS;
	private static final Map<Long, DirtyBlockState> DIRTY_BLOCKS = new ConcurrentHashMap<>();

	private ClientDirtyBlockCache() {
	}

	public static void setDirtiest(BlockPos pos) {
		DIRTY_BLOCKS.put(pos.asLong(), DirtyBlockState.full());
	}

	public static void cleanPixel(BlockPos pos, Direction face, int pixelIndex) {
		DirtyBlockState state = DIRTY_BLOCKS.get(pos.asLong());
		if (state == null) return;
		state.clean(face, pixelIndex);
		if (state.isClean()) {
			DIRTY_BLOCKS.remove(pos.asLong());
		}
	}

	public static void setDirtyPixel(BlockPos pos, Direction face, int pixelIndex) {
		DIRTY_BLOCKS.computeIfAbsent(pos.asLong(), k -> new DirtyBlockState()).setDirty(face, pixelIndex);
	}

	public static boolean isDirty(BlockPos pos) {
		DirtyBlockState state = DIRTY_BLOCKS.get(pos.asLong());
		return state != null && !state.isClean();
	}

	public static void removeBlock(BlockPos pos) {
		DIRTY_BLOCKS.remove(pos.asLong());
	}

	public static Map<Long, DirtyBlockState> getDirtyBlocks() {
		return DIRTY_BLOCKS;
	}

	public static final class DirtyBlockState {
		private final boolean[][] facePixels = new boolean[6][PIXEL_COUNT];

		public static DirtyBlockState full() {
			DirtyBlockState state = new DirtyBlockState();
			for (int face = 0; face < state.facePixels.length; face++) {
				for (int i = 0; i < PIXEL_COUNT; i++) {
					state.facePixels[face][i] = true;
				}
			}
			return state;
		}

		public void clean(Direction face, int pixelIndex) {
			boolean[] pixels = facePixels[face.get3DDataValue()];
			if (pixelIndex >= 0 && pixelIndex < PIXEL_COUNT) {
				pixels[pixelIndex] = false;
			}
		}

		public void setDirty(Direction face, int pixelIndex) {
			boolean[] pixels = facePixels[face.get3DDataValue()];
			if (pixelIndex >= 0 && pixelIndex < PIXEL_COUNT) {
				pixels[pixelIndex] = true;
			}
		}

		public boolean isPixelDirty(Direction face, int pixelIndex) {
			return facePixels[face.get3DDataValue()][pixelIndex];
		}

		public boolean isClean() {
			for (boolean[] pixels : facePixels) {
				for (boolean pixel : pixels) {
					if (pixel) return false;
				}
			}
			return true;
		}

		public int getDirtyPixelCount(Direction face) {
			int count = 0;
			for (boolean pixel : facePixels[face.get3DDataValue()]) {
				if (pixel) count++;
			}
			return count;
		}

		public float getDirtyRatio(Direction face) {
			return (float) getDirtyPixelCount(face) / (float) PIXEL_COUNT;
		}

		public boolean[][] getFacePixels() {
			return facePixels;
		}
	}
}

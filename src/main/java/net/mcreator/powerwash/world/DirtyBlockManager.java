package net.mcreator.powerwash.world;

import net.mcreator.powerwash.PowerwashMod;
import net.mcreator.powerwash.network.DirtyBlockSyncPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

	public static void setDirtiest(BlockPos pos, Direction clickedFace) {
		DIRTY_BLOCKS.put(pos.asLong(), DirtyBlockState.full(clickedFace));
	}

	public static void setDirtiest(BlockPos pos, Direction clickedFace, ServerLevel level) {
		setDirtiest(pos, clickedFace);
		syncFullBlock(pos, clickedFace, level);
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

	public static boolean cleanPixel(BlockPos pos, Direction face, Vec3 hitLocation, ServerLevel level) {
		DirtyBlockState state = DIRTY_BLOCKS.get(pos.asLong());
		if (state == null) {
			return false;
		}
		int pixelIndex = state.getPixelIndex(face, hitLocation, pos);
		int px = pixelIndex % PIXELS;
		int py = pixelIndex / PIXELS;

		// Clean 2x2 area
		boolean anyChanged = false;
		for (int dy = 0; dy < 2; dy++) {
			for (int dx = 0; dx < 2; dx++) {
				int cx = Math.min(px + dx, PIXELS - 1);
				int cy = Math.min(py + dy, PIXELS - 1);
				int idx = cy * PIXELS + cx;
				if (state.clean(face, idx)) {
					anyChanged = true;
					syncPixel(pos, face, (short) idx, level);
				}
			}
		}

		if (state.isClean()) {
			DIRTY_BLOCKS.remove(pos.asLong());
		}
		return anyChanged;
	}

	public static float getDirtyRatio(BlockPos pos) {
		DirtyBlockState state = DIRTY_BLOCKS.get(pos.asLong());
		if (state == null) {
			return 0.0F;
		}
		return state.getDirtyRatio();
	}

	private static void syncPixel(BlockPos pos, Direction face, short pixelIndex, ServerLevel level) {
		DirtyBlockSyncPayload payload = new DirtyBlockSyncPayload(pos.asLong(), (byte) face.get3DDataValue(), pixelIndex, true);
		for (ServerPlayer player : level.players()) {
			if (player.blockPosition().distSqr(pos) < 256.0) {
				player.connection.send(payload);
			}
		}
	}

	private static void syncFullBlock(BlockPos pos, Direction clickedFace, ServerLevel level) {
		for (ServerPlayer player : level.players()) {
			if (player.blockPosition().distSqr(pos) < 256.0) {
				for (int i = 0; i < PIXEL_COUNT; i++) {
					player.connection.send(new DirtyBlockSyncPayload(pos.asLong(), (byte) clickedFace.get3DDataValue(), (short) i, false));
				}
			}
		}
	}

	private static final class DirtyBlockState {
		private final boolean[][] facePixels = new boolean[6][PIXEL_COUNT];
		private final Direction clickedFace;

		static DirtyBlockState full() {
			return new DirtyBlockState(Direction.UP);
		}

		static DirtyBlockState full(Direction face) {
			return new DirtyBlockState(face);
		}

		private DirtyBlockState(Direction face) {
			this.clickedFace = face;
			boolean[] pixels = facePixels[face.get3DDataValue()];
			for (int i = 0; i < PIXEL_COUNT; i++) {
				pixels[i] = true;
			}
		}

		public Direction getClickedFace() {
			return clickedFace;
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
			double localX = net.minecraft.util.Mth.clamp(hitLocation.x - blockPos.getX(), 0.0, 0.9999);
			double localY = net.minecraft.util.Mth.clamp(hitLocation.y - blockPos.getY(), 0.0, 0.9999);
			double localZ = net.minecraft.util.Mth.clamp(hitLocation.z - blockPos.getZ(), 0.0, 0.9999);

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
					v = localY;
				}
				case SOUTH -> {
					u = localX;
					v = localY;
				}
				case WEST -> {
					u = localZ;
					v = localY;
				}
				default -> {
					u = 1.0 - localZ;
					v = localY;
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

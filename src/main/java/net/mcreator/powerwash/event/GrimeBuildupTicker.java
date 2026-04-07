package net.mcreator.powerwash.event;

import net.mcreator.powerwash.PowerwashMod;
import net.mcreator.powerwash.world.DirtyBlockManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Random;

@EventBusSubscriber(modid = PowerwashMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class GrimeBuildupTicker {
	
	private static final Random RANDOM = new Random();
	private static int tickCounter = 0;
	private static final int CHECK_INTERVAL = 20; // Check every 20 ticks (1 second)
	private static final int BLOCKS_PER_CHECK = 200; // Check 200 blocks per second
	private static final double GRIME_CHANCE = 0.20; // 20% chance per checked block

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		var players = event.getServer().getPlayerList().getPlayers();
		if (players.isEmpty()) return;

		tickCounter++;
		if (tickCounter < CHECK_INTERVAL) return;
		tickCounter = 0;

		// Pick a random player to process around
		ServerPlayer player = players.get(RANDOM.nextInt(players.size()));
		ServerLevel level = (ServerLevel) player.level();
		
		// Get a random chunk near the player
		LevelChunk chunk = level.getChunk(player.getBlockX() >> 4, player.getBlockZ() >> 4);
		
		// Check random blocks within that chunk
		for (int i = 0; i < BLOCKS_PER_CHECK; i++) {
			int x = chunk.getPos().getMinBlockX() + RANDOM.nextInt(16);
			int z = chunk.getPos().getMinBlockZ() + RANDOM.nextInt(16);
			int y = RANDOM.nextInt(256); // Check full height range
			
			BlockPos pos = new BlockPos(x, y, z);
			BlockState state = level.getBlockState(pos);
			
			// 1. Check if block is Stone (you can switch this back to your tag later)
			if (state.is(Blocks.STONE)) {
				
				// 2. Check if block was placed by a player
				if (GrimePlacementTracker.isPlayerPlaced(pos)) {
					
					// 3. Check if block is exposed to air (has at least one air neighbor)
					if (isExposedToAir(level, pos)) {
						
						// Apply grime
						if (RANDOM.nextDouble() < GRIME_CHANCE) {
							if (!DirtyBlockManager.isDirty(pos)) {
								DirtyBlockManager.setDirtiest(pos, getBestFace(level, pos), level);
								PowerwashMod.LOGGER.info("[GrimeTicker] Slowly placed grime on player-placed Stone at {}", pos);
							}
						}
					}
				}
			}
		}
	}

	private static boolean isExposedToAir(ServerLevel level, BlockPos pos) {
		for (Direction dir : Direction.values()) {
			if (level.getBlockState(pos.relative(dir)).isAir()) {
				return true;
			}
		}
		return false;
	}

	private static Direction getBestFace(ServerLevel level, BlockPos pos) {
		// Find a face exposed to air for the grime to appear on
		for (Direction dir : Direction.values()) {
			if (level.getBlockState(pos.relative(dir)).isAir()) {
				return dir;
			}
		}
		return Direction.UP;
	}
}

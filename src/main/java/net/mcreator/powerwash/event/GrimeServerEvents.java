package net.mcreator.powerwash.event;

import net.mcreator.powerwash.PowerwashMod;
import net.mcreator.powerwash.network.GrimeClearPayload;
import net.mcreator.powerwash.world.DirtyBlockManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = PowerwashMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class GrimeServerEvents {

	@SubscribeEvent
	public static void onBlockBreak(BlockEvent.BreakEvent event) {
		if (event.getLevel() instanceof ServerLevel serverLevel) {
			BlockPos pos = event.getPos();
			if (DirtyBlockManager.isDirty(pos)) {
				GrimeClearPayload payload = new GrimeClearPayload(pos.asLong());
				for (ServerPlayer player : serverLevel.players()) {
					if (player.blockPosition().distSqr(pos) < 256.0) {
						player.connection.send(payload);
					}
				}
				PowerwashMod.LOGGER.info("[GrimeServer] Cleared grime at {} on break", pos);
			}
		}
	}
}

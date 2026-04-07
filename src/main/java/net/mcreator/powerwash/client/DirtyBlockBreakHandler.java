package net.mcreator.powerwash.client;

import net.mcreator.powerwash.PowerwashMod;
import net.mcreator.powerwash.client.cache.ClientDirtyBlockCache;

import net.minecraft.core.BlockPos;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = PowerwashMod.MODID, value = net.neoforged.api.distmarker.Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class DirtyBlockBreakHandler {

	@SubscribeEvent
	public static void onBlockBreak(BlockEvent.BreakEvent event) {
		if (event.getLevel().isClientSide()) {
			BlockPos pos = event.getPos();
			if (ClientDirtyBlockCache.isDirty(pos)) {
				ClientDirtyBlockCache.removeBlock(pos);
				PowerwashMod.LOGGER.info("[DirtyBlockBreak] Removed dirty overlay at {}", pos);
			}
		}
	}
}

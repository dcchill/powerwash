package net.mcreator.powerwash.event;

import net.mcreator.powerwash.PowerwashMod;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = PowerwashMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class GrimePlacementTracker {

    // Thread-safe set to track player-placed blocks
    public static final Set<BlockPos> PLAYER_PLACED_BLOCKS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        // Only track blocks placed by players (living entities)
        if (event.getEntity() != null && event.getEntity().isAlive()) {
            BlockPos pos = event.getPos();
            PLAYER_PLACED_BLOCKS.add(pos.immutable());
            PowerwashMod.LOGGER.debug("[GrimeTracker] Tracked player-placed block at {}", pos);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockPos pos = event.getPos();
        PLAYER_PLACED_BLOCKS.remove(pos);
    }

    public static boolean isPlayerPlaced(BlockPos pos) {
        return PLAYER_PLACED_BLOCKS.contains(pos);
    }
}

/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.powerwash.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

import net.minecraft.world.level.block.Block;

import net.mcreator.powerwash.block.GrimeBlock;
import net.mcreator.powerwash.PowerwashMod;

public class PowerwashModBlocks {
	public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(PowerwashMod.MODID);
	public static final DeferredBlock<Block> GRIME = REGISTRY.register("grime", GrimeBlock::new);
	// Start of user code block custom blocks
	// End of user code block custom blocks
}
package net.mcreator.powerwash.init;

import net.mcreator.powerwash.PowerwashMod;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;

import com.mojang.serialization.Codec;

import java.util.function.Supplier;

public class PowerwashModDataComponents {
	public static final DeferredRegister<DataComponentType<?>> REGISTRY = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "powerwash");
	
	public static final Supplier<DataComponentType<Integer>> WATER = REGISTRY.register(
		"water",
		() -> DataComponentType.<Integer>builder()
			.persistent(Codec.INT)
			.networkSynchronized(ByteBufCodecs.INT)
			.build()
	);
}

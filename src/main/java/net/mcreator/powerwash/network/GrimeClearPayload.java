package net.mcreator.powerwash.network;

import net.mcreator.powerwash.PowerwashMod;
import net.mcreator.powerwash.client.cache.ClientDirtyBlockCache;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record GrimeClearPayload(long blockPos) implements CustomPacketPayload {

	public static final Type<GrimeClearPayload> TYPE = new Type<>(
		ResourceLocation.fromNamespaceAndPath(PowerwashMod.MODID, "grime_clear")
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, GrimeClearPayload> STREAM_CODEC = new StreamCodec<>() {
		@Override
		public GrimeClearPayload decode(RegistryFriendlyByteBuf buf) {
			return new GrimeClearPayload(buf.readLong());
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buf, GrimeClearPayload payload) {
			buf.writeLong(payload.blockPos);
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public BlockPos getBlockPos() {
		return BlockPos.of(blockPos);
	}

	public static void handle(GrimeClearPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().level().isClientSide()) {
				ClientDirtyBlockCache.removeBlock(payload.getBlockPos());
			}
		});
	}
}

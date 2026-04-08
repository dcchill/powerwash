package net.mcreator.powerwash.network;

import net.mcreator.powerwash.PowerwashMod;
import net.mcreator.powerwash.client.cache.ClientDirtyBlockCache;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DirtyBlockSyncPayload(long blockPos, byte face, short pixelIndex, boolean clean) implements CustomPacketPayload {

	public static final Type<DirtyBlockSyncPayload> TYPE = new Type<>(
		ResourceLocation.fromNamespaceAndPath(PowerwashMod.MODID, "dirty_block_sync")
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, DirtyBlockSyncPayload> STREAM_CODEC = new StreamCodec<>() {
		@Override
		public DirtyBlockSyncPayload decode(RegistryFriendlyByteBuf buf) {
			return new DirtyBlockSyncPayload(buf.readLong(), buf.readByte(), buf.readShort(), buf.readBoolean());
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buf, DirtyBlockSyncPayload payload) {
			buf.writeLong(payload.blockPos);
			buf.writeByte(payload.face);
			buf.writeShort(payload.pixelIndex);
			buf.writeBoolean(payload.clean());
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public BlockPos getBlockPos() {
		return BlockPos.of(blockPos);
	}

	public Direction getFace() {
		return Direction.from3DDataValue(face);
	}

	public static void handle(DirtyBlockSyncPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player() != null && context.player().level().isClientSide()) {
				PowerwashMod.LOGGER.info("[DirtyBlockSyncPayload] Received on client: pos={}, face={}, pixelIndex={}, clean={}", 
					payload.getBlockPos(), payload.getFace(), payload.pixelIndex(), payload.clean());
				if (payload.clean()) {
					ClientDirtyBlockCache.cleanPixel(payload.getBlockPos(), payload.getFace(), payload.pixelIndex());
				} else if (payload.pixelIndex() == -1) {
					// Full face sync - mark all pixels dirty on the target face
					ClientDirtyBlockCache.setDirtiestFace(payload.getBlockPos(), payload.getFace());
					PowerwashMod.LOGGER.info("[DirtyBlockSyncPayload] Set dirtiest face {} for block {}", payload.getFace(), payload.getBlockPos());
				} else {
					ClientDirtyBlockCache.setDirtyPixel(payload.getBlockPos(), payload.getFace(), payload.pixelIndex());
				}
			} else {
				PowerwashMod.LOGGER.warn("[DirtyBlockSyncPayload] Ignored on server side or null player. isClientSide={}", 
					context.player() != null ? context.player().level().isClientSide() : "null");
			}
		});
	}
}

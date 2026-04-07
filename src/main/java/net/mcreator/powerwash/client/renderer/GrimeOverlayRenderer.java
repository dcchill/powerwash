package net.mcreator.powerwash.client.renderer;

import net.mcreator.powerwash.PowerwashMod;
import net.mcreator.powerwash.client.cache.ClientDirtyBlockCache;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = PowerwashMod.MODID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public class GrimeOverlayRenderer {

	private static final ResourceLocation GRIME_TEXTURE = ResourceLocation.fromNamespaceAndPath("powerwash", "textures/block/grime.png");
	private static final float GRIME_ALPHA = 0.7f;
	private static final float PIXEL_SIZE = 1.0f / 16.0f;
	private static final float OVERLAY_OFFSET = 0.002f;

	private static int renderCallCount = 0;
	private static int lastDirtyCount = 0;

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
			return;
		}

		renderCallCount++;

		Level level = Minecraft.getInstance().level;
		if (level == null) return;

		var dirtyBlocks = ClientDirtyBlockCache.getDirtyBlocks();
		if (dirtyBlocks.isEmpty()) {
			if (renderCallCount % 200 == 0 && lastDirtyCount != 0) {
				PowerwashMod.LOGGER.info("[GrimeOverlay] No dirty blocks in cache (tick {}).", renderCallCount);
			}
			lastDirtyCount = 0;
			return;
		}

		if (dirtyBlocks.size() != lastDirtyCount) {
			lastDirtyCount = dirtyBlocks.size();
			PowerwashMod.LOGGER.info("[GrimeOverlay] Rendering {} dirty blocks (tick {}).", dirtyBlocks.size(), renderCallCount);
		}

		Camera camera = event.getCamera();
		PoseStack poseStack = event.getPoseStack();
		Frustum frustum = event.getFrustum();

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.setShaderColor(1f, 1f, 1f, GRIME_ALPHA);
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, GRIME_TEXTURE);

		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

		double camX = camera.getPosition().x;
		double camY = camera.getPosition().y;
		double camZ = camera.getPosition().z;

		int quadsDrawn = 0;

		for (var entry : dirtyBlocks.entrySet()) {
			BlockPos pos = BlockPos.of(entry.getKey());
			ClientDirtyBlockCache.DirtyBlockState state = entry.getValue();

			AABB blockBox = new AABB(pos);
			if (!frustum.isVisible(blockBox)) {
				continue;
			}

			try {
				quadsDrawn += renderBlockGrime(state, buffer, camX, camY, camZ, pos);
			} catch (Exception e) {
				PowerwashMod.LOGGER.error("[GrimeOverlay] Render error: {}", e.getMessage());
			}
		}

		if (renderCallCount % 100 == 0) {
			PowerwashMod.LOGGER.info("[GrimeOverlay] Drew {} quads across {} blocks", quadsDrawn, dirtyBlocks.size());
		}

		try {
			var mesh = buffer.build();
			if (mesh != null) {
				com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(mesh);
			}
		} catch (Exception e) {
			if (quadsDrawn > 0) {
				PowerwashMod.LOGGER.error("[GrimeOverlay] Buffer upload error: {}", e.getMessage());
			}
		}

		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private static int renderBlockGrime(ClientDirtyBlockCache.DirtyBlockState state, BufferBuilder buffer,
			double camX, double camY, double camZ, BlockPos pos) {
		int count = 0;
		for (Direction face : Direction.values()) {
			int dirtyCount = state.getDirtyPixelCount(face);
			if (dirtyCount == 0) continue;
			count += renderFaceGrime(face, state.getFacePixels()[face.get3DDataValue()], buffer, camX, camY, camZ, pos);
		}
		return count;
	}

	private static int renderFaceGrime(Direction face, boolean[] pixels, BufferBuilder buffer,
			double camX, double camY, double camZ, BlockPos pos) {
		int count = 0;
		for (int v = 0; v < 16; v++) {
			for (int u = 0; u < 16; u++) {
				if (!pixels[v * 16 + u]) continue;
				count++;
				drawPixelQuad(face, u, v, buffer, camX, camY, camZ, pos);
			}
		}
		return count;
	}

	private static void drawPixelQuad(Direction face, int u, int v, BufferBuilder buffer,
			double camX, double camY, double camZ, BlockPos pos) {
		float u0 = u * PIXEL_SIZE;
		float u1 = (u + 1) * PIXEL_SIZE;
		float v0 = v * PIXEL_SIZE;
		float v1 = (v + 1) * PIXEL_SIZE;

		double bx = pos.getX();
		double by = pos.getY();
		double bz = pos.getZ();

		switch (face) {
			case DOWN -> {
				// Y=0, XZ plane
				buffer.addVertex((float)(bx + u0 - camX), (float)(by - OVERLAY_OFFSET - camY), (float)(bz + v0 - camZ)).setUv(u0, v0).setColor(1f, 1f, 1f, GRIME_ALPHA);
				buffer.addVertex((float)(bx + u1 - camX), (float)(by - OVERLAY_OFFSET - camY), (float)(bz + v0 - camZ)).setUv(u1, v0).setColor(1f, 1f, 1f, GRIME_ALPHA);
				buffer.addVertex((float)(bx + u1 - camX), (float)(by - OVERLAY_OFFSET - camY), (float)(bz + v1 - camZ)).setUv(u1, v1).setColor(1f, 1f, 1f, GRIME_ALPHA);
				buffer.addVertex((float)(bx + u0 - camX), (float)(by - OVERLAY_OFFSET - camY), (float)(bz + v1 - camZ)).setUv(u0, v1).setColor(1f, 1f, 1f, GRIME_ALPHA);
			}
			case UP -> {
				// Y=1, XZ plane
				buffer.addVertex((float)(bx + u0 - camX), (float)(by + 1 + OVERLAY_OFFSET - camY), (float)(bz + v0 - camZ)).setUv(u0, v0).setColor(1f, 1f, 1f, GRIME_ALPHA);
				buffer.addVertex((float)(bx + u1 - camX), (float)(by + 1 + OVERLAY_OFFSET - camY), (float)(bz + v0 - camZ)).setUv(u1, v0).setColor(1f, 1f, 1f, GRIME_ALPHA);
				buffer.addVertex((float)(bx + u1 - camX), (float)(by + 1 + OVERLAY_OFFSET - camY), (float)(bz + v1 - camZ)).setUv(u1, v1).setColor(1f, 1f, 1f, GRIME_ALPHA);
				buffer.addVertex((float)(bx + u0 - camX), (float)(by + 1 + OVERLAY_OFFSET - camY), (float)(bz + v1 - camZ)).setUv(u0, v1).setColor(1f, 1f, 1f, GRIME_ALPHA);
			}
			case NORTH -> {
				// Z=0, XY plane
				buffer.addVertex((float)(bx + 1 - u1 - camX), (float)(by + v0 - camY), (float)(bz - OVERLAY_OFFSET - camZ)).setUv(u0, v0).setColor(1f, 1f, 1f, GRIME_ALPHA);
				buffer.addVertex((float)(bx + 1 - u0 - camX), (float)(by + v0 - camY), (float)(bz - OVERLAY_OFFSET - camZ)).setUv(u1, v0).setColor(1f, 1f, 1f, GRIME_ALPHA);
				buffer.addVertex((float)(bx + 1 - u0 - camX), (float)(by + v1 - camY), (float)(bz - OVERLAY_OFFSET - camZ)).setUv(u1, v1).setColor(1f, 1f, 1f, GRIME_ALPHA);
				buffer.addVertex((float)(bx + 1 - u1 - camX), (float)(by + v1 - camY), (float)(bz - OVERLAY_OFFSET - camZ)).setUv(u0, v1).setColor(1f, 1f, 1f, GRIME_ALPHA);
			}
			case SOUTH -> {
				// Z=1, XY plane
				buffer.addVertex((float)(bx + u0 - camX), (float)(by + v0 - camY), (float)(bz + 1 + OVERLAY_OFFSET - camZ)).setUv(u0, v0).setColor(1f, 1f, 1f, GRIME_ALPHA);
				buffer.addVertex((float)(bx + u1 - camX), (float)(by + v0 - camY), (float)(bz + 1 + OVERLAY_OFFSET - camZ)).setUv(u1, v0).setColor(1f, 1f, 1f, GRIME_ALPHA);
				buffer.addVertex((float)(bx + u1 - camX), (float)(by + v1 - camY), (float)(bz + 1 + OVERLAY_OFFSET - camZ)).setUv(u1, v1).setColor(1f, 1f, 1f, GRIME_ALPHA);
				buffer.addVertex((float)(bx + u0 - camX), (float)(by + v1 - camY), (float)(bz + 1 + OVERLAY_OFFSET - camZ)).setUv(u0, v1).setColor(1f, 1f, 1f, GRIME_ALPHA);
			}
			case WEST -> {
				// X=0, ZY plane
				buffer.addVertex((float)(bx - OVERLAY_OFFSET - camX), (float)(by + v0 - camY), (float)(bz + u0 - camZ)).setUv(u0, v0).setColor(1f, 1f, 1f, GRIME_ALPHA);
				buffer.addVertex((float)(bx - OVERLAY_OFFSET - camX), (float)(by + v0 - camY), (float)(bz + u1 - camZ)).setUv(u1, v0).setColor(1f, 1f, 1f, GRIME_ALPHA);
				buffer.addVertex((float)(bx - OVERLAY_OFFSET - camX), (float)(by + v1 - camY), (float)(bz + u1 - camZ)).setUv(u1, v1).setColor(1f, 1f, 1f, GRIME_ALPHA);
				buffer.addVertex((float)(bx - OVERLAY_OFFSET - camX), (float)(by + v1 - camY), (float)(bz + u0 - camZ)).setUv(u0, v1).setColor(1f, 1f, 1f, GRIME_ALPHA);
			}
			case EAST -> {
				// X=1, ZY plane
				buffer.addVertex((float)(bx + 1 + OVERLAY_OFFSET - camX), (float)(by + v0 - camY), (float)(bz + 1 - u1 - camZ)).setUv(u0, v0).setColor(1f, 1f, 1f, GRIME_ALPHA);
				buffer.addVertex((float)(bx + 1 + OVERLAY_OFFSET - camX), (float)(by + v0 - camY), (float)(bz + 1 - u0 - camZ)).setUv(u1, v0).setColor(1f, 1f, 1f, GRIME_ALPHA);
				buffer.addVertex((float)(bx + 1 + OVERLAY_OFFSET - camX), (float)(by + v1 - camY), (float)(bz + 1 - u0 - camZ)).setUv(u1, v1).setColor(1f, 1f, 1f, GRIME_ALPHA);
				buffer.addVertex((float)(bx + 1 + OVERLAY_OFFSET - camX), (float)(by + v1 - camY), (float)(bz + 1 - u1 - camZ)).setUv(u0, v1).setColor(1f, 1f, 1f, GRIME_ALPHA);
			}
		}
	}
}

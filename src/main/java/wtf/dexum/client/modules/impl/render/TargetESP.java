package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.Dexum;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.animations.base.Easing;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.base.theme.Theme;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.ModeSetting;
import wtf.dexum.client.modules.impl.combat.Aura;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.base.color.ColorUtil;
import wtf.dexum.utility.render.display.shader.DrawUtil;

import java.util.*;

@ModuleAnnotation(
        name = "TargetESP",
        category = Category.RENDER,
        description = "Выделяет цель"
)
public class TargetESP extends Module {
   public static final TargetESP INSTANCE = new TargetESP();
   private static final Identifier GLOW_TEXTURE = Identifier.of("dexum", "icons/glow.png");
   private static final Identifier BLOOM_TEXTURE = Identifier.of("dexum", "icons/bloom.png");
   private final ModeSetting mode = new ModeSetting("Мод", new String[]{
           "Маркер", "Призраки", "Призраки 1", "Призраки 2", "Призраки 3", "Призраки 4",
           "Сканер", "Призрачные орбиты", "Кристаллы", "Молнии", "Атомы"
   });
   private final Animation animation = new Animation(400L, Easing.CUBIC_OUT);
   private final Animation animation2 = new Animation(250L, Easing.CUBIC_OUT);
   private Entity lastTarget;
   private float rotationAngle;
   private float rotationSpeed;
   private boolean isReversing;
   private float animationNurik;
   private long currentTime = System.currentTimeMillis();
   private final long timestamp4 = System.currentTimeMillis();
   private long timestamp5 = System.nanoTime();
   private float value23;

   private static final int ORBIT_PARTICLE_COUNT = 3;
   private static final float ORBIT_BASE_RADIUS = 0.4f;
   private static final float ORBIT_BASE_MUL = 0.1f;
   private static final float ORBIT_SPEED = 15.0f;
   private static final int ORBIT_TRAIL_LENGTH = 40;
   private static final float[] SCALE_CACHE = new float[101];
   static {
      for (int k = 0; k <= 100; k++) SCALE_CACHE[k] = Math.max(0.28f * (k / 100f), 0.15f);
   }

   private final Vec3d[] orbitPositions = new Vec3d[ORBIT_PARTICLE_COUNT];
   private final Vec3d[] orbitMotions = new Vec3d[ORBIT_PARTICLE_COUNT];
   private final List<Vec3d>[] orbitTrails = new List[ORBIT_PARTICLE_COUNT];
   private float movingAngle;
   private long lastOrbitTime;
   private final Animation orbitShrinkAnim = new Animation(300L, Easing.CUBIC_OUT);

   private float crystalMoving;

   private final List<Deque<GhostPoint>> ghostTrails = Arrays.asList(new ArrayDeque<>(), new ArrayDeque<>(), new ArrayDeque<>());
   private long ghostStartTime = System.currentTimeMillis();
   private float colorInterpAnim;
   private Vec3d anchorPosition;
   private float anchorHeight = 1.8F;

   private final Random lightningRandom = new Random();
   private final List<LightningBolt> lightningBolts = new ArrayList<>();
   private long lastLightningTime;
   private static final int MAX_LIGHTNING_BOLTS = 6;
   private static final long LIGHTNING_INTERVAL = 80;

   public TargetESP() {
      for (int i = 0; i < ORBIT_PARTICLE_COUNT; i++) {
         orbitTrails[i] = new ArrayList<>();
         orbitMotions[i] = Vec3d.ZERO;
      }
   }

   @EventTarget
   private void onRenderWorldLast(EventRender3D e) {
      Entity target = Aura.INSTANCE.getTarget();
      if (target != null) {
         if (lastTarget != target) {
            for (int i = 0; i < ORBIT_PARTICLE_COUNT; i++) {
               orbitPositions[i] = null;
               orbitMotions[i] = Vec3d.ZERO;
               orbitTrails[i].clear();
            }
            ghostStartTime = System.currentTimeMillis();
         }
         lastTarget = target;
         animation.update(true);
         animation2.update(true);
         anchorPosition = new Vec3d(target.getX(), target.getY(), target.getZ());
         anchorHeight = target.getHeight();
         if (target instanceof LivingEntity living) {
            colorInterpAnim = lerp(colorInterpAnim, Math.min(1.0F, living.hurtTime / 5.0F), 0.2F);
         }
      } else {
         animation.update(false);
         animation2.update(false);
         colorInterpAnim = lerp(colorInterpAnim, 0.0F, 0.16F);
         if (animation.getValue() == 0.0F) {
            lastTarget = null;
            clearGhostTrails();
            anchorPosition = null;
            anchorHeight = 1.8F;
         }
      }

      if (lastTarget != null && animation.getValue() > 0.01F) {
         switch (mode.getValue().toString()) {
            case "Маркер": renderMarker(e); break;
            case "Призраки": drawSpiritsTrack(e); break;
            case "Призраки 1": drawSpirits(e); break;
            case "Призраки 2": renderNursultan(e); break;
            case "Призраки 3": drawGhosts3(e); break;
            case "Призраки 4": drawGhosts4(e); break;
            case "Сканер": renderScanner(e); break;
            case "Призрачные орбиты": drawGhostOrbits(e); break;
            case "Кристаллы": renderCrystals(e); break;
            case "Молнии": renderLightning(e); break;
            case "Атомы": renderAtoms(e); break;
         }
      }
   }

   private float lerp(float from, float to, float speed) {
      return from + (to - from) * MathHelper.clamp(speed, 0, 1);
   }

   private int interpolateColor(int c1, int c2, float r) {
      r = MathHelper.clamp(r, 0, 1);
      int a = (int)(((c1>>24)&0xFF) + (((c2>>24)&0xFF) - ((c1>>24)&0xFF)) * r);
      int rr = (int)(((c1>>16)&0xFF) + (((c2>>16)&0xFF) - ((c1>>16)&0xFF)) * r);
      int g = (int)(((c1>>8)&0xFF) + (((c2>>8)&0xFF) - ((c1>>8)&0xFF)) * r);
      int b = (int)((c1&0xFF) + ((c2&0xFF) - (c1&0xFF)) * r);
      return (a<<24) | (rr<<16) | (g<<8) | b;
   }

   private int setAlpha(int color, int alpha) {
      return (color & 0x00FFFFFF) | (MathHelper.clamp(alpha, 0, 255) << 24);
   }

   private void clearGhostTrails() {
      for (Deque<GhostPoint> trail : ghostTrails) trail.clear();
   }

   private void addTexturedQuad(BufferBuilder buf, Matrix4f mat, float half, int argb) {
      int a = (argb>>24)&0xFF, r = (argb>>16)&0xFF, g = (argb>>8)&0xFF, b = argb&0xFF;
      buf.vertex(mat, -half, -half, 0).texture(0,1).color(r,g,b,a);
      buf.vertex(mat, half, -half, 0).texture(1,1).color(r,g,b,a);
      buf.vertex(mat, half, half, 0).texture(1,0).color(r,g,b,a);
      buf.vertex(mat, -half, half, 0).texture(0,0).color(r,g,b,a);
   }

   // ---------- МАРКЕР ----------
   private void renderMarker(EventRender3D e) {
      Vec3d camPos = mc.gameRenderer.getCamera().getPos();
      double tickDelta = e.getPartialTicks();
      MatrixStack matrices = e.getMatrix();
      double x = MathHelper.lerp(tickDelta, lastTarget.lastRenderX, lastTarget.getX());
      double y = MathHelper.lerp(tickDelta, lastTarget.lastRenderY, lastTarget.getY()) + lastTarget.getHeight() / 2.0;
      double z = MathHelper.lerp(tickDelta, lastTarget.lastRenderZ, lastTarget.getZ());
      matrices.push();
      matrices.translate(x - camPos.x, y - camPos.y, z - camPos.z);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
      float scale = 0.15F * animation.getValue();
      matrices.scale(-scale, -scale, scale);
      updateRotation();
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationAngle));
      Identifier textureId = Identifier.of("dexum", "icons/marker.png");
      float alpha = animation.getValue();
      float size = 12.0F;
      Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
      ColorRGBA color = theme.getColor().withAlpha((int)(alpha * 255));
      DrawUtil.drawTexture(matrices, textureId, 0.0F - size / 2.0F, 0.0F - size / 2.0F, size, size, color);
      matrices.pop();
   }

   @Native
   private void updateRotation() {
      if (!isReversing) {
         rotationSpeed += 0.01F;
         if (rotationSpeed > 2.3F) { rotationSpeed = 2.3F; isReversing = true; }
      } else {
         rotationSpeed -= 0.01F;
         if (rotationSpeed < -2.3F) { rotationSpeed = -2.3F; isReversing = false; }
      }
      rotationAngle += rotationSpeed;
      rotationAngle %= 360.0F;
   }

   // ---------- ПРИЗРАКИ (основной) ----------
   private void drawSpiritsTrack(EventRender3D e) {
      Aura aura = Aura.INSTANCE;
      animation2.update(aura.getTarget() != null && aura.isEnabled());
      if (animation2.getValue() == 0.0F) return;
      MatrixStack matrices = e.getMatrix();
      if (aura.getTarget() != null) lastTarget = aura.getTarget();
      if (lastTarget == null) return;

      long now = System.currentTimeMillis();
      animationNurik += (float)(now - currentTime) / 120.0F;
      currentTime = now;

      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(770, 1, 0, 1);
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);

      double x = interpolate(lastTarget.getX(), lastTarget.lastRenderX, e.getPartialTicks()) - mc.gameRenderer.getCamera().getPos().x;
      double y = interpolate(lastTarget.getY(), lastTarget.lastRenderY, e.getPartialTicks()) - mc.gameRenderer.getCamera().getPos().y;
      double z = interpolate(lastTarget.getZ(), lastTarget.lastRenderZ, e.getPartialTicks()) - mc.gameRenderer.getCamera().getPos().z;

      int n2 = 3, n3 = 12, n4 = 3 * n2;
      matrices.push();
      BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      for (int i = 0; i < n4; i += n2) {
         for (int j = 0; j < n3; j++) {
            Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
            ColorRGBA color = theme.getColor();
            float f2 = animationNurik + j * 0.1F;
            int n5 = (int) Math.pow(i, 2);
            matrices.push();
            matrices.translate(x + 0.8F * MathHelper.sin(f2 + n5),
                    y + 0.5 + 0.3F * MathHelper.sin(animationNurik + j * 0.2F) + 0.2F * i,
                    z + 0.8F * MathHelper.cos(f2 - n5));
            matrices.scale(animation2.getValue() * (0.005F + j / 2000.0F),
                    animation2.getValue() * (0.005F + j / 2000.0F),
                    animation2.getValue() * (0.005F + j / 2000.0F));
            matrices.multiply(mc.gameRenderer.getCamera().getRotation());
            int n7 = -25, n8 = 50;
            int rgba = color.withAlpha((int)(animation2.getValue() * 600)).getRGB();
            buffer.vertex(matrices.peek().getPositionMatrix(), n7, n7 + n8, 0).texture(0,1).color(rgba);
            buffer.vertex(matrices.peek().getPositionMatrix(), n7 + n8, n7 + n8, 0).texture(1,1).color(rgba);
            buffer.vertex(matrices.peek().getPositionMatrix(), n7 + n8, n7, 0).texture(1,0).color(rgba);
            buffer.vertex(matrices.peek().getPositionMatrix(), n7, n7, 0).texture(0,0).color(rgba);
            matrices.pop();
         }
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      matrices.pop();
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
      RenderSystem.blendFunc(770, 771);
      RenderSystem.enableCull();
   }

   private double interpolate(double cur, double old, double scale) {
      return old + (cur - old) * scale;
   }

   // ---------- ПРИЗРАКИ 1 ----------
   private void drawSpirits(EventRender3D e) {
      MatrixStack matrices = e.getMatrix();
      Vec3d camPos = mc.gameRenderer.getCamera().getPos();
      double x = interpolate(lastTarget.getX(), lastTarget.lastRenderX, e.getPartialTicks()) - camPos.x;
      double y = interpolate(lastTarget.getY(), lastTarget.lastRenderY, e.getPartialTicks()) - camPos.y + lastTarget.getHeight() / 2.0;
      double z = interpolate(lastTarget.getZ(), lastTarget.lastRenderZ, e.getPartialTicks()) - camPos.z;

      float hurtTime = lastTarget instanceof LivingEntity living ? (living.hurtTime - (living.hurtTime != 0 ? e.getPartialTicks() : 0)) / 10.0F : 0;
      float animValue = -0.15F * animation2.getValue() + 0.65F;
      long time = (long)((System.currentTimeMillis() - timestamp4) / 2.0F);
      long nano = System.nanoTime();
      value23 += hurtTime * (nano - timestamp5) / 2000000.0F;
      timestamp5 = nano;

      matrices.push();
      matrices.translate(x, y, z);
      matrices.scale(1.5F, 1.5F, 1.5F);

      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(770, 1, 0, 1);
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);

      BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      for (int layer = 0; layer < 3; layer++) {
         for (int i = 0; i < 14; i++) {
            matrices.push();
            float progress = i / 13.0F;
            float size = (0.55F * (1 - progress) + 0.2F * progress) * animation2.getValue();
            double angle = (0.2F * (time + value23 - i * 7.0F) / 15.0F);
            boolean firstHalf = progress < 0.5F;
            float wave = firstHalf ? progress * 2 : (1 - progress) * 2;
            double amp = Math.sin(wave * Math.PI) * 2;
            Random rnd = new Random(i * 12345L);
            double offX = (rnd.nextDouble() - 0.5) * amp;
            double offY = (rnd.nextDouble() - 0.5) * amp;
            double offZ = (rnd.nextDouble() - 0.5) * amp;
            double aX = offX * animation2.getValue() - offX;
            double aY = offY * animation2.getValue() - offY;
            double aZ = offZ * animation2.getValue() - offZ;
            double posX = -Math.sin(angle) * animValue;
            double posZ = -Math.cos(angle) * animValue;
            switch (layer) {
               case 0 -> { aY += i * 0.02; matrices.translate(posX + aX, posZ + aY, -posZ + aZ); }
               case 1 -> { aY -= i * 0.02; matrices.translate(-posX + aX, posX + aY, -posZ + aZ); }
               case 2 -> matrices.translate(-posX + aX, -posX + aY, posZ + aZ);
            }
            float ps = size * 0.5F;
            Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
            ColorRGBA c = theme.getColor().withAlpha((int)(600 * animation2.getValue()));
            matrices.multiply(mc.gameRenderer.getCamera().getRotation());
            buffer.vertex(matrices.peek().getPositionMatrix(), -ps, -ps, 0).texture(1,1).color(c.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), ps, -ps, 0).texture(0,1).color(c.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), ps, ps, 0).texture(0,0).color(c.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), -ps, ps, 0).texture(1,0).color(c.getRGB());
            matrices.pop();
         }
      }
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.enableDepthTest(); RenderSystem.depthMask(true); RenderSystem.disableBlend();
      RenderSystem.blendFunc(770, 771); RenderSystem.enableCull();
      matrices.pop();
   }

   // ---------- ПРИЗРАКИ 2 (Нурсултан) ----------
   private void renderNursultan(EventRender3D e) {
      MatrixStack matrices = e.getMatrix();
      Vec3d camPos = mc.gameRenderer.getCamera().getPos();
      double x = interpolate(lastTarget.getX(), lastTarget.lastRenderX, e.getPartialTicks()) - camPos.x;
      double y = interpolate(lastTarget.getY(), lastTarget.lastRenderY, e.getPartialTicks()) - camPos.y + lastTarget.getHeight() / 2.0;
      double z = interpolate(lastTarget.getZ(), lastTarget.lastRenderZ, e.getPartialTicks()) - camPos.z;
      float time = (System.currentTimeMillis() - timestamp4) / 1100.0F;
      float rotation = time * 360.0F;
      float radius = 0.5F;

      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
      RenderSystem.enableBlend(); RenderSystem.blendFuncSeparate(770, 1, 0, 1);
      RenderSystem.disableCull(); RenderSystem.disableDepthTest(); RenderSystem.depthMask(false);

      BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      for (int layer = 0; layer < 4; layer++) {
         float layerOff = (layer - 1) * 0.4F;
         float prev = -1;
         for (float i = 0; i < 130; i++) {
            float angle = rotation + i + layerOff * 360;
            double rad = Math.toRadians(-angle);
            double yOff = Math.sin(rad + 2) * layerOff;
            float size = radius * (i / 140.0F);
            float finalSize = prev >= 0 ? (prev + size) / 2 : size;
            prev = size;
            finalSize *= animation2.getValue();
            float alpha = MathHelper.clamp(finalSize, 0, 1);
            Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
            ColorRGBA c = theme.getColor().withAlpha((int)(600 * animation2.getValue() * alpha));
            matrices.push();
            matrices.translate(x, y + yOff, z);
            matrices.multiply(mc.gameRenderer.getCamera().getRotation());
            float half = finalSize / 2;
            double cosA = Math.cos(rad) * radius - half;
            double sinA = Math.sin(rad) * radius - half;
            buffer.vertex(matrices.peek().getPositionMatrix(), (float)cosA, -half, (float)sinA).texture(0,1).color(c.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float)(cosA+finalSize), -half, (float)sinA).texture(1,1).color(c.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float)(cosA+finalSize), half, (float)sinA).texture(1,0).color(c.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float)cosA, half, (float)sinA).texture(0,0).color(c.getRGB());
            matrices.pop();
         }
      }
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.enableDepthTest(); RenderSystem.depthMask(true); RenderSystem.disableBlend();
      RenderSystem.blendFunc(770, 771); RenderSystem.enableCull();
   }

   // ---------- ПРИЗРАКИ 3 ----------
   private void drawGhosts3(EventRender3D e) {
      if (lastTarget == null) return;
      float fadeAnim = animation.getValue();
      if (fadeAnim <= 0.0f) return;
      MatrixStack ms = e.getMatrix();
      Camera camera = mc.gameRenderer.getCamera();
      float tickDelta = e.getPartialTicks();
      float moving = ((mc.player != null ? mc.player.age : 0) + tickDelta) * 13.0f;
      Vec3d targetPos = new Vec3d(
              MathHelper.lerp(tickDelta, lastTarget.lastRenderX, lastTarget.getX()),
              MathHelper.lerp(tickDelta, lastTarget.lastRenderY, lastTarget.getY()),
              MathHelper.lerp(tickDelta, lastTarget.lastRenderZ, lastTarget.getZ())
      );
      float width = lastTarget.getWidth() * 1.5f;
      float entityHeight = lastTarget.getHeight();
      Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
      ColorRGBA themeColor = theme.getColor();
      int glowArgb = themeColor.withAlpha((int) (fadeAnim * 255.0F * 0.08F)).getRGB();
      int coreArgb = themeColor.withAlpha((int) (fadeAnim * 255.0F)).getRGB();

      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(770, 1, 0, 1);
      RenderSystem.enableDepthTest();
      if (mc.world != null && mc.player != null) {
         Vec3d targetEye = targetPos.add(0.0, entityHeight * 0.85, 0.0);
         if (mc.world.raycast(new RaycastContext(
                 camera.getPos(),
                 targetEye,
                 RaycastContext.ShapeType.COLLIDER,
                 RaycastContext.FluidHandling.NONE,
                 mc.player
         )).getType() != HitResult.Type.MISS) {
            RenderSystem.disableDepthTest();
         }
      }
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);
      RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      int step = 2, wormTick = 0, wormCD = 0;
      for (int i = 0; i < 360; i += step) {
         float size = 0.13f + 0.005f * wormTick, bigSize = 0.7f + 0.005f * wormTick;
         if (wormCD > 0) { wormCD -= step; continue; }
         if ((wormTick += step) > 50) { wormCD = 100; wormTick = 0; continue; }
         float val = Math.max(0.5f, 1.2f - 0.5f * fadeAnim);
         float sin = (float) (Math.sin(Math.toRadians(i + moving)) * width * val);
         float cos = (float) (Math.cos(Math.toRadians(i + moving)) * width * val);
         float yAnim = (float) Math.sin(Math.toRadians(i / 2.0f + moving / 5.0f));
         ms.push();
         ms.translate(targetPos.x + sin - camera.getPos().x, targetPos.y + (entityHeight / 1.5f) + (entityHeight / 3.0f) * yAnim - camera.getPos().y, targetPos.z + cos - camera.getPos().z);
         ms.multiply(camera.getRotation());
         Matrix4f matrix = ms.peek().getPositionMatrix();
         float glowHalf = bigSize / 2.0f;
         addTexturedQuad(builder, matrix, glowHalf, glowArgb);
         float coreHalf = size / 2.0f;
         addTexturedQuad(builder, matrix, coreHalf, coreArgb);
         ms.pop();
      }
      BufferRenderer.drawWithGlobalProgram(builder.end());
      RenderSystem.enableCull();
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
   }

   // ---------- ПРИЗРАКИ 4 ----------
   private void drawGhosts4(EventRender3D e) {
      float anim = animation.getValue();
      if (anim <= 0.01F) return;
      Vec3d renderBasePos = resolveRenderBasePosition(e.getPartialTicks());
      if (renderBasePos == null) return;
      float dynamicScale = animation2.getValue() * (0.45F + anim * 0.55F);
      dynamicScale = Math.max(0.05F, dynamicScale);
      spawnGhostTrails(renderBasePos, anchorHeight);
      drawGhostTrails(e.getMatrix(), anim, dynamicScale);
   }

   private Vec3d resolveRenderBasePosition(float partialTicks) {
      if (lastTarget != null) {
         anchorHeight = lastTarget.getHeight();
         anchorPosition = new Vec3d(
                 MathHelper.lerp(partialTicks, lastTarget.lastRenderX, lastTarget.getX()),
                 MathHelper.lerp(partialTicks, lastTarget.lastRenderY, lastTarget.getY()),
                 MathHelper.lerp(partialTicks, lastTarget.lastRenderZ, lastTarget.getZ())
         );
         return anchorPosition;
      }
      return anchorPosition;
   }

   private void spawnGhostTrails(Vec3d basePos, float entityHeight) {
      int trailLength = 65;
      double rotationSpeed = 0.0050, orbitRadius = 0.65;
      long time = System.currentTimeMillis() - ghostStartTime;
      for (int i = 0; i < ghostTrails.size(); i++) {
         Deque<GhostPoint> trail = ghostTrails.get(i);
         trail.clear();
         for (int j = 0; j < trailLength; j++) {
            double ageOffset = j * 10.0;
            double angle = (time - ageOffset) * rotationSpeed + (i * 2.0) * Math.PI / ghostTrails.size();
            double dx = Math.cos(angle) * orbitRadius, dz = Math.sin(angle) * orbitRadius;
            double yo = Math.sin((time + i * 759.0 - ageOffset) * 0.003) * 0.6;
            trail.addLast(new GhostPoint(new Vec3d(basePos.x + dx + 0.1, basePos.y + entityHeight * 0.5 + yo + 0.25, basePos.z + dz), 0.5F));
         }
      }
   }

   private void drawGhostTrails(MatrixStack matrix, float anim, float dynamicScale) {
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d camPos = camera.getPos();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(770, 1, 0, 1);
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
      int baseColor = interpolateColor(theme.getColor().withAlpha(255).getRGB(), 0xFFFF1414, colorInterpAnim);
      for (Deque<GhostPoint> trail : ghostTrails) {
         int index = 0;
         for (GhostPoint point : trail) {
            float tailFactor = 1.0F - (float) index / trail.size();
            float ghostSize = point.size * dynamicScale * (1.1F - (float) index / trail.size());
            if (ghostSize <= 0.001F) { index++; continue; }
            int alpha = (int) (255.0F * anim * tailFactor);
            if (alpha <= 0) { index++; continue; }
            int color = ColorUtil.multAlpha(baseColor, alpha / 255.0f);
            int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF, a = (color >> 24) & 0xFF;
            double dx = point.position.x - camPos.x, dy = point.position.y - camPos.y, dz = point.position.z - camPos.z;
            matrix.push();
            matrix.translate(dx, dy, dz);
            matrix.multiply(camera.getRotation());
            Matrix4f mat = matrix.peek().getPositionMatrix();
            buffer.vertex(mat, 0.0F, 0.0F, 0.0F).texture(0.0F, 0.0F).color(r, g, b, a);
            buffer.vertex(mat, ghostSize, 0.0F, 0.0F).texture(1.0F, 0.0F).color(r, g, b, a);
            buffer.vertex(mat, ghostSize, ghostSize, 0.0F).texture(1.0F, 1.0F).color(r, g, b, a);
            buffer.vertex(mat, 0.0F, ghostSize, 0.0F).texture(0.0F, 1.0F).color(r, g, b, a);
            matrix.pop();
            index++;
         }
      }
      BuiltBuffer built = buffer.endNullable();
      if (built != null) BufferRenderer.drawWithGlobalProgram(built);
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
   }

   // ---------- СКАНЕР ----------
   private void renderScanner(EventRender3D e) {
      if (lastTarget == null) return;
      float alpha = animation2.getValue();
      if (alpha <= 0.0F) return;

      MatrixStack matrices = e.getMatrix();
      Vec3d camPos = mc.gameRenderer.getCamera().getPos();
      Camera camera = mc.gameRenderer.getCamera();
      float tickDelta = e.getPartialTicks();

      double x = interpolate(lastTarget.getX(), lastTarget.lastRenderX, tickDelta) - camPos.x;
      double y = interpolate(lastTarget.getY(), lastTarget.lastRenderY, tickDelta) - camPos.y;
      double z = interpolate(lastTarget.getZ(), lastTarget.lastRenderZ, tickDelta) - camPos.z;
      float height = lastTarget.getHeight();
      float radius = 0.4f;

      long time = System.currentTimeMillis();
      float rawPos = (time * 0.001f) % 2.0f;
      if (rawPos > 1.0f) rawPos = 2.0f - rawPos;
      float scannerY = height * rawPos;

      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(770, 1);
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);

      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

      Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
      int r = theme.getColor().getRed();
      int g = theme.getColor().getGreen();
      int b = theme.getColor().getBlue();
      int a = (int)(alpha * 255);

      int segments = 60;
      for (int i = 0; i <= segments; i++) {
         double angle = Math.toRadians(i * (360.0 / segments));
         float cos = (float) Math.cos(angle);
         float sin = (float) Math.sin(angle);
         double px = x + cos * radius;
         double pz = z + sin * radius;
         double py = y + scannerY;

         matrices.push();
         matrices.translate(px, py, pz);
         matrices.multiply(camera.getRotation());
         Matrix4f mat = matrices.peek().getPositionMatrix();
         buffer.vertex(mat, 0, 0, 0).color(r, g, b, a);
         matrices.pop();
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
      RenderSystem.blendFunc(770, 771);
      RenderSystem.enableCull();
   }

   // ---------- ПРИЗРАЧНЫЕ ОРБИТЫ ----------
   private void drawGhostOrbits(EventRender3D e) {
      if (lastTarget == null) return;
      MatrixStack matrices = e.getMatrix();
      Vec3d camPos = mc.gameRenderer.getCamera().getPos();
      float delta = e.getPartialTicks();
      Camera camera = mc.gameRenderer.getCamera();
      double tx = interpolate(lastTarget.getX(), lastTarget.lastRenderX, delta);
      double ty = interpolate(lastTarget.getY(), lastTarget.lastRenderY, delta);
      double tz = interpolate(lastTarget.getZ(), lastTarget.lastRenderZ, delta);
      Vec3d center = new Vec3d(tx, ty + lastTarget.getHeight() / 2.0, tz);

      long now = System.currentTimeMillis();
      if (lastOrbitTime == 0) lastOrbitTime = now;
      float dt = now - lastOrbitTime;
      lastOrbitTime = now;
      float fps = 500.0f / Math.max(mc.getCurrentFps(), 10);
      movingAngle += (20 * dt / 16.667f) * (ORBIT_SPEED / 55.0f);

      boolean hurt = lastTarget instanceof LivingEntity living && living.hurtTime > 7;
      orbitShrinkAnim.update(hurt);
      float shrink = orbitShrinkAnim.getValue();

      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
      RenderSystem.enableBlend(); RenderSystem.blendFunc(770, 1);
      RenderSystem.disableCull(); RenderSystem.disableDepthTest(); RenderSystem.depthMask(false);

      BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
      ColorRGBA base = theme.getColor();

      for (int i = 0; i < ORBIT_PARTICLE_COUNT; i++) {
         float off = i * 360f / ORBIT_PARTICLE_COUNT;
         float ang = movingAngle + off;
         double rad = Math.toRadians(ang);
         float orbR = ORBIT_BASE_RADIUS - shrink * ORBIT_BASE_RADIUS;
         float ox = (float)(Math.sin(rad) * orbR);
         float oz = (float)(Math.cos(rad) * orbR);
         double oy = 0.3 * Math.sin(Math.toRadians(movingAngle / (i + 1.0f)));
         Vec3d targetPos = center.add(ox, oy, oz);

         if (orbitPositions[i] == null || orbitPositions[i].distanceTo(targetPos) > 10) {
            orbitPositions[i] = targetPos;
            orbitMotions[i] = Vec3d.ZERO;
         }
         float mul = ORBIT_BASE_MUL * fps;
         Vec3d diff = targetPos.subtract(orbitPositions[i]);
         orbitMotions[i] = diff.multiply(mul);
         orbitPositions[i] = orbitPositions[i].add(orbitMotions[i]);

         if (orbitTrails[i].isEmpty() || orbitTrails[i].get(0).distanceTo(orbitPositions[i]) > 0.01) {
            orbitTrails[i].add(0, orbitPositions[i]);
            while (orbitTrails[i].size() > ORBIT_TRAIL_LENGTH) orbitTrails[i].remove(orbitTrails[i].size()-1);
         }

         for (int j = 0; j < orbitTrails[i].size(); j++) {
            Vec3d p = orbitTrails[i].get(j);
            float off2 = 1.0f - (float)j / ORBIT_TRAIL_LENGTH;
            matrices.push();
            matrices.translate(p.x - camPos.x, p.y - camPos.y, p.z - camPos.z);
            matrices.multiply(camera.getRotation());
            Matrix4f mat = matrices.peek().getPositionMatrix();
            float opacity = (float)Math.pow(off2, 1.8) * animation2.getValue() * 0.7f;
            int color = base.withAlpha((int)(opacity * 255)).getRGB();
            float scale = SCALE_CACHE[Math.min((int)(off2 * 100), 100)] * 0.8f;
            addTexturedQuad(buffer, mat, scale, color);
            matrices.pop();
         }
         if (!orbitTrails[i].isEmpty()) {
            Vec3d head = orbitTrails[i].get(0);
            matrices.push();
            matrices.translate(head.x - camPos.x, head.y - camPos.y, head.z - camPos.z);
            matrices.multiply(camera.getRotation());
            Matrix4f mat = matrices.peek().getPositionMatrix();
            float headScale = 0.35f * animation2.getValue();
            int headColor = base.withAlpha((int)(120 * animation2.getValue())).getRGB();
            addTexturedQuad(buffer, mat, headScale, headColor);
            matrices.pop();
         }
      }
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.enableDepthTest(); RenderSystem.depthMask(true); RenderSystem.disableBlend();
      RenderSystem.blendFunc(770, 771); RenderSystem.enableCull();
   }

   // ---------- КРИСТАЛЛЫ ----------
   private void renderCrystals(EventRender3D e) {
      float alpha = animation2.getValue();
      if (alpha <= 0.0F) return;
      if (lastTarget == null) return;
      if (mc.player == null) return;
      MatrixStack matrices = e.getMatrix();
      Vec3d camPos = mc.gameRenderer.getCamera().getPos();
      float tickDelta = e.getPartialTicks();
      double tx = interpolate(lastTarget.getX(), lastTarget.lastRenderX, tickDelta);
      double ty = interpolate(lastTarget.getY(), lastTarget.lastRenderY, tickDelta);
      double tz = interpolate(lastTarget.getZ(), lastTarget.lastRenderZ, tickDelta);
      crystalMoving += 1.0f;
      float entityHeight = lastTarget.getHeight(), entityWidth = lastTarget.getWidth();
      float width = entityWidth * 1.5f;
      Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
      ColorRGBA themeColor = theme.getColor();
      int colorRGB = themeColor.getRGB() & 0x00FFFFFF;
      int cr = (colorRGB >> 16) & 0xFF, cg = (colorRGB >> 8) & 0xFF, cb = colorRGB & 0xFF;
      matrices.push();
      matrices.translate(tx - camPos.x, ty - camPos.y, tz - camPos.z);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(770, 1);
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder crystalBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      int crystalAlpha = Math.min(255, (int) (alpha * 255));
      int color = (crystalAlpha << 24) | (cr << 16) | (cg << 8) | cb;
      float cw = 0.085f, ch = 0.22f;
      for (int i = 0; i < 360; i += 19) {
         float val = 1.2f - 0.5f * alpha;
         float angleDeg = i + crystalMoving * 0.3f;
         float angleRad = (float) Math.toRadians(angleDeg);
         float sin = (float) (Math.sin(angleRad) * width * val);
         float cos = (float) (Math.cos(angleRad) * width * val);
         float heightPrc = ((i / 20.0f) * 0.6180339f) % 1.0f;
         float crystalY = entityHeight * heightPrc;
         matrices.push();
         matrices.translate(sin, crystalY, cos);
         Vector3f dir = new Vector3f(-sin, 0, -cos).normalize();
         Quaternionf rotation = new Quaternionf().rotationTo(new Vector3f(0, 1, 0), dir);
         matrices.multiply(rotation);
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         float[] ex = {cw, 0, -cw, 0}, ez = {0, cw, 0, -cw};
         for (int j = 0; j < 4; j++) {
            int next = (j + 1) % 4;
            crystalBuffer.vertex(matrix, 0, ch, 0).color(color);
            crystalBuffer.vertex(matrix, ex[j], 0, ez[j]).color(color);
            crystalBuffer.vertex(matrix, ex[next], 0, ez[next]).color(color);
         }
         for (int j = 0; j < 4; j++) {
            int next = (j + 1) % 4;
            crystalBuffer.vertex(matrix, 0, -ch, 0).color(color);
            crystalBuffer.vertex(matrix, ex[next], 0, ez[next]).color(color);
            crystalBuffer.vertex(matrix, ex[j], 0, ez[j]).color(color);
         }
         matrices.pop();
      }
      BufferRenderer.drawWithGlobalProgram(crystalBuffer.end());
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
      BufferBuilder glowBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      Camera camera = mc.gameRenderer.getCamera();
      for (int i = 0; i < 360; i += 19) {
         float val = 1.2f - 0.5f * alpha;
         float angleDeg = i + crystalMoving * 0.3f;
         float angleRad = (float) Math.toRadians(angleDeg);
         float sin = (float) (Math.sin(angleRad) * width * val);
         float cos = (float) (Math.cos(angleRad) * width * val);
         float heightPrc = ((i / 20.0f) * 0.6180339f) % 1.0f;
         float crystalY = entityHeight * heightPrc;
         matrices.push();
         matrices.translate(sin, crystalY, cos);
         matrices.multiply(camera.getRotation());
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         float glowSize = 0.3f * alpha;
         int glowAlpha = Math.min(255, (int) (alpha * 200));
         int glowColor = (glowAlpha << 24) | (cr << 16) | (cg << 8) | cb;
         glowBuffer.vertex(matrix, -glowSize, glowSize, 0).texture(0f, 1f).color(glowColor);
         glowBuffer.vertex(matrix, glowSize, glowSize, 0).texture(1f, 1f).color(glowColor);
         glowBuffer.vertex(matrix, glowSize, -glowSize, 0).texture(1f, 0f).color(glowColor);
         glowBuffer.vertex(matrix, -glowSize, -glowSize, 0).texture(0f, 0f).color(glowColor);
         matrices.pop();
      }
      BufferRenderer.drawWithGlobalProgram(glowBuffer.end());
      matrices.pop();
      RenderSystem.enableCull();
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
   }

   // ---------- МОЛНИИ ----------
   private void renderLightning(EventRender3D e) {
      if (lastTarget == null) return;
      float alpha = animation2.getValue();
      if (alpha <= 0.0F) { lightningBolts.clear(); return; }
      long now = System.currentTimeMillis();
      if (lightningBolts.size() < MAX_LIGHTNING_BOLTS && now - lastLightningTime > LIGHTNING_INTERVAL) {
         lastLightningTime = now;
         Vec3d targetCenter = new Vec3d(lastTarget.getX(), lastTarget.getY() + lastTarget.getHeight() / 2.0, lastTarget.getZ());
         double angle = lightningRandom.nextDouble() * Math.PI * 2;
         double radius = lastTarget.getWidth() * 1.2;
         Vec3d start = targetCenter.add(Math.cos(angle) * radius, (lightningRandom.nextDouble() - 0.5) * lastTarget.getHeight(), Math.sin(angle) * radius);
         double outAngle = lightningRandom.nextDouble() * Math.PI * 2;
         double outRadius = radius + 0.5 + lightningRandom.nextDouble() * 1.5;
         Vec3d end = targetCenter.add(Math.cos(outAngle) * outRadius, (lightningRandom.nextDouble() - 0.5) * lastTarget.getHeight() * 1.5, Math.sin(outAngle) * outRadius);
         lightningBolts.add(new LightningBolt(start, end, now, 100 + lightningRandom.nextInt(150), 5 + lightningRandom.nextInt(8), (lightningRandom.nextFloat() - 0.5f) * 0.4f));
      }
      lightningBolts.removeIf(bolt -> now - bolt.spawnTime > bolt.duration);
      if (lightningBolts.isEmpty()) return;
      MatrixStack matrices = e.getMatrix();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(770, 1);
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      for (LightningBolt bolt : lightningBolts) {
         float lifeProgress = (float)(now - bolt.spawnTime) / bolt.duration;
         float boltAlpha = alpha * (1.0f - lifeProgress) * 0.9f;
         if (boltAlpha <= 0.01f) continue;
         Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
         int cr = theme.getColor().getRed(), cg = theme.getColor().getGreen(), cb = theme.getColor().getBlue();
         int color1 = ((int)(boltAlpha * 255) << 24) | (cr << 16) | (cg << 8) | cb;
         int color2 = ((int)(boltAlpha * 0.3f * 255) << 24) | (cr << 16) | (cg << 8) | cb;
         int segments = bolt.segments;
         Vec3d prevPoint = null;
         float prevWidth = 0.02f * alpha;
         for (int i = 0; i <= segments; i++) {
            float progress = (float) i / segments;
            Vec3d point = bolt.start.add((bolt.end.x - bolt.start.x) * progress, (bolt.end.y - bolt.start.y) * progress, (bolt.end.z - bolt.start.z) * progress);
            double offX = (lightningRandom.nextDouble() - 0.5) * bolt.offset * (1.0 - Math.abs(progress - 0.5) * 2.0);
            double offY = (lightningRandom.nextDouble() - 0.5) * bolt.offset * (1.0 - Math.abs(progress - 0.5) * 2.0);
            double offZ = (lightningRandom.nextDouble() - 0.5) * bolt.offset * (1.0 - Math.abs(progress - 0.5) * 2.0);
            point = point.add(offX, offY, offZ);
            if (prevPoint != null) {
               Vec3d dir = point.subtract(prevPoint).normalize();
               Vec3d perp = new Vec3d(-dir.z, 0, dir.x).normalize();
               float width = prevWidth * (1.0f - progress * 0.7f);
               Vec3d p1 = prevPoint.add(perp.x * width, perp.y * width, perp.z * width).subtract(mc.gameRenderer.getCamera().getPos());
               Vec3d p2 = prevPoint.subtract(perp.x * width, perp.y * width, perp.z * width).subtract(mc.gameRenderer.getCamera().getPos());
               Vec3d p3 = point.add(perp.x * width * 0.5f, perp.y * width * 0.5f, perp.z * width * 0.5f).subtract(mc.gameRenderer.getCamera().getPos());
               Vec3d p4 = point.subtract(perp.x * width * 0.5f, perp.y * width * 0.5f, perp.z * width * 0.5f).subtract(mc.gameRenderer.getCamera().getPos());
               Matrix4f mat = matrices.peek().getPositionMatrix();
               buffer.vertex(mat, (float)p1.x, (float)p1.y, (float)p1.z).color(color1);
               buffer.vertex(mat, (float)p2.x, (float)p2.y, (float)p2.z).color(color1);
               buffer.vertex(mat, (float)p3.x, (float)p3.y, (float)p3.z).color(color2);
               buffer.vertex(mat, (float)p4.x, (float)p4.y, (float)p4.z).color(color2);
            }
            prevPoint = point;
         }
      }
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
   }

   // ---------- АТОМЫ ----------
   private void renderAtoms(EventRender3D e) {
      if (lastTarget == null) return;
      float alpha = animation2.getValue();
      if (alpha <= 0.0F) return;

      MatrixStack matrices = e.getMatrix();
      Vec3d camPos = mc.gameRenderer.getCamera().getPos();
      float tickDelta = e.getPartialTicks();

      double tx = interpolate(lastTarget.getX(), lastTarget.lastRenderX, tickDelta) - camPos.x;
      double ty = interpolate(lastTarget.getY(), lastTarget.lastRenderY, tickDelta) - camPos.y;
      double tz = interpolate(lastTarget.getZ(), lastTarget.lastRenderZ, tickDelta) - camPos.z;

      float time = (System.currentTimeMillis() - timestamp4) / 1000.0f;

      Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
      int color1 = theme.getColor().withAlpha(255).getRGB();
      int color2 = interpolateColor(color1, 0xFFFFFFFF, 0.45f);

      shedevrotargetespAtoms(matrices, new Vec3d(tx, ty, tz), lastTarget.getHeight(), time, alpha, color1, color2);
   }

   private void shedevrotargetespAtoms(MatrixStack ms, Vec3d pos, float height, float time, float anim, int color1, int color2) {
      ms.push();
      ms.translate(pos.x, pos.y + height / 2.0, pos.z);

      float scale = 0.7f * anim;
      ms.scale(scale, scale, scale);

      RenderSystem.enableBlend();
      RenderSystem.disableCull();
      RenderSystem.blendFunc(770, 1);
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

      float radius = 1.0f;
      int segments = 64;

      for (int ring = 0; ring < 3; ring++) {
         ms.push();
         ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(ring * 60.0f));
         ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 60.0f + ring * 120.0f));

         // Кольцо
         BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
         Matrix4f matrix = ms.peek().getPositionMatrix();
         for (int i = 0; i <= segments; i++) {
            float angle = (float)(2 * Math.PI * i / segments);
            float x = (float)Math.cos(angle) * radius;
            float z = (float)Math.sin(angle) * radius;
            float progress = (float) i / segments;
            int segColor = setAlpha(interpolateColor(color1, color2, progress), (int)(200 * anim));
            bb.vertex(matrix, x, 0, z).color(segColor);
         }
         BufferRenderer.drawWithGlobalProgram(bb.end());

         // Частицы на кольце
         for (int p = 0; p < 6; p++) {
            float pAngle = (float)(2 * Math.PI * p / 6) + time * 2.0f;
            float px = (float)Math.cos(pAngle) * radius;
            float pz = (float)Math.sin(pAngle) * radius;
            ms.push();
            ms.translate(px, 0, pz);
            int pColor = setAlpha(interpolateColor(color1, color2, (float) p / 6), (int)(255 * anim));
            drawAtomSphere(ms, 0.08f, pColor);
            ms.pop();
         }

         ms.pop();
      }

      // Центральное свечение
      renderAtomGlow(ms, color1, anim * 0.5f);

      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      RenderSystem.blendFunc(770, 771);
      ms.pop();
   }

   private void drawAtomSphere(MatrixStack ms, float r, int color) {
      int a = (color >> 24) & 0xFF, rc = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
      float[][] v = {{0,r,0},{r,0,0},{0,0,r},{-r,0,0},{0,0,-r},{0,-r,0}};
      int[][] f = {{0,1,2},{0,2,3},{0,3,4},{0,4,1},{5,2,1},{5,3,2},{5,4,3},{5,1,4}};
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      Matrix4f matrix = ms.peek().getPositionMatrix();
      for (int[] face : f) {
         for (int idx : face) {
            bb.vertex(matrix, v[idx][0], v[idx][1], v[idx][2]).color(rc, g, b, a);
         }
      }
      BufferRenderer.drawWithGlobalProgram(bb.end());
   }

   private void renderAtomGlow(MatrixStack ms, int color, float alpha) {
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
      BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      ms.push();
      ms.multiply(mc.gameRenderer.getCamera().getRotation());
      Matrix4f mat = ms.peek().getPositionMatrix();
      int a = (int)(200 * alpha);
      int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
      float s = 1.5f;
      bb.vertex(mat, -s, -s, 0).texture(0f, 0f).color(r, g, b, a);
      bb.vertex(mat,  s, -s, 0).texture(1f, 0f).color(r, g, b, a);
      bb.vertex(mat,  s,  s, 0).texture(1f, 1f).color(r, g, b, a);
      bb.vertex(mat, -s,  s, 0).texture(0f, 1f).color(r, g, b, a);
      BufferRenderer.drawWithGlobalProgram(bb.end());
      ms.pop();
   }

   // ---------- ВСПОМОГАТЕЛЬНЫЕ КЛАССЫ ----------
   private static class GhostPoint {
      final Vec3d position;
      final float size;
      GhostPoint(Vec3d p, float s) { position = p; size = s; }
   }

   private static class LightningBolt {
      final Vec3d start, end;
      final long spawnTime, duration;
      final int segments;
      final float offset;
      LightningBolt(Vec3d s, Vec3d e, long st, long d, int seg, float off) {
         start = s; end = e; spawnTime = st; duration = d; segments = seg; offset = off;
      }
   }
}
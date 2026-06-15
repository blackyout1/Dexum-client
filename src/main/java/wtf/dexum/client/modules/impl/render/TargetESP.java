package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Random;

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
           "Маркер", "Призраки", "Призраки 1", "Призраки 2", "Призраки 3", "Призраки 4", "Круг", "Призрачные орбиты", "Кристаллы"
   });
   private final Animation animation;
   private final Animation animation2;
   private Entity lastTarget;
   private boolean textureLoaded;
   private float rotationAngle;
   private float rotationSpeed;
   private boolean isReversing;
   private float animationNurik;
   private long currentTime;
   private final long timestamp4;
   private long timestamp5;
   private float value23;

   private static final int ORBIT_PARTICLE_COUNT = 3;
   private static final float ORBIT_BASE_RADIUS = 0.4f;
   private static final float ORBIT_BASE_MUL = 0.1f;
   private static final float ORBIT_SPEED = 15.0f;
   private static final int ORBIT_TRAIL_LENGTH = 40;

   private static final float[] SCALE_CACHE = new float[101];

   static {
      for (int k = 0; k <= 100; k++) {
         SCALE_CACHE[k] = Math.max(0.28f * (k / 100f), 0.15f);
      }
   }

   private final Vec3d[] orbitPositions = new Vec3d[ORBIT_PARTICLE_COUNT];
   private final Vec3d[] orbitMotions = new Vec3d[ORBIT_PARTICLE_COUNT];
   private final List<Vec3d>[] orbitTrails = new List[ORBIT_PARTICLE_COUNT];
   private float movingAngle = 0;
   private long lastOrbitTime = 0;
   private final Animation orbitShrinkAnim = new Animation(300L, Easing.CUBIC_OUT);

   private float crystalMoving = 0;

   private final List<Deque<GhostPoint>> ghostTrails = Arrays.asList(
           new ArrayDeque<>(),
           new ArrayDeque<>(),
           new ArrayDeque<>()
   );
   private long ghostStartTime = System.currentTimeMillis();
   private float colorInterpAnim = 0.0F;
   private Vec3d anchorPosition = null;
   private float anchorHeight = 1.8F;

   public TargetESP() {
      this.animation = new Animation(400L, Easing.CUBIC_OUT);
      this.animation2 = new Animation(250L, Easing.CUBIC_OUT);
      this.lastTarget = null;
      this.textureLoaded = false;
      this.rotationAngle = 0.0F;
      this.rotationSpeed = 0.0F;
      this.isReversing = false;
      this.animationNurik = 0.0F;
      this.currentTime = System.currentTimeMillis();
      this.timestamp4 = System.currentTimeMillis();
      this.timestamp5 = System.nanoTime();
      this.value23 = 0.0F;

      for (int i = 0; i < ORBIT_PARTICLE_COUNT; i++) {
         this.orbitTrails[i] = new ArrayList<>();
         this.orbitMotions[i] = Vec3d.ZERO;
      }
   }

   public void onEnable() {
      super.onEnable();
   }

   @EventTarget
   private void onRenderWorldLast(EventRender3D e) {
      Entity target = Aura.INSTANCE.getTarget();
      if (target != null) {
         if (this.lastTarget != target) {
            for (int i = 0; i < ORBIT_PARTICLE_COUNT; i++) {
               orbitPositions[i] = null;
               orbitMotions[i] = Vec3d.ZERO;
               orbitTrails[i].clear();
            }
            this.ghostStartTime = System.currentTimeMillis();
         }
         this.lastTarget = target;
         this.animation.update(true);
         this.animation2.update(true);
         this.anchorPosition = new Vec3d(target.getX(), target.getY(), target.getZ());
         this.anchorHeight = target.getHeight();
         if (target instanceof LivingEntity living) {
            this.colorInterpAnim = lerp(this.colorInterpAnim, Math.min(1.0F, living.hurtTime / 5.0F), 0.2F);
         }
      } else {
         this.animation.update(false);
         this.animation2.update(false);
         this.colorInterpAnim = lerp(this.colorInterpAnim, 0.0F, 0.16F);
         if (this.animation.getValue() == 0.0F) {
            this.lastTarget = null;
            this.clearGhostTrails();
            this.anchorPosition = null;
            this.anchorHeight = 1.8F;
         }
      }

      if (this.lastTarget != null && this.animation.getValue() > 0.01F) {
         String currentMode = this.mode.getValue().toString();
         switch (currentMode) {
            case "Маркер":
               this.renderMarker(e);
               break;
            case "Призраки":
               this.drawSpiritsTrack(e);
               break;
            case "Призраки 1":
               this.drawSpirits(e);
               break;
            case "Призраки 2":
               this.renderNursultan(e);
               break;
            case "Призраки 3":
               this.drawGhosts3(e);
               break;
            case "Призраки 4":
               this.drawGhosts4(e);
               break;
            case "Круг":
               this.drawCircle(e);
               break;
            case "Призрачные орбиты":
               this.drawGhostOrbits(e);
               break;
            case "Кристаллы":
               this.renderCrystals(e);
               break;
         }
      }
   }

   private void renderMarker(EventRender3D e) {
      if (!this.textureLoaded) {
         this.textureLoaded = true;
      }

      Vec3d camPos = mc.gameRenderer.getCamera().getPos();
      double tickDelta = (double) e.getPartialTicks();
      MatrixStack matrices = e.getMatrix();
      double x = MathHelper.lerp(tickDelta, this.lastTarget.lastRenderX, this.lastTarget.getX());
      double y = MathHelper.lerp(tickDelta, this.lastTarget.lastRenderY, this.lastTarget.getY()) + (double) this.lastTarget.getHeight() / 2.0D;
      double z = MathHelper.lerp(tickDelta, this.lastTarget.lastRenderZ, this.lastTarget.getZ());
      matrices.push();
      matrices.translate(x - camPos.x, y - camPos.y, z - camPos.z);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
      float scale = 0.15F * this.animation.getValue();
      matrices.scale(-scale, -scale, scale);
      this.updateRotation();
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.rotationAngle));

      Identifier textureId = Identifier.of("dexum", "icons/marker.png");
      float alpha = this.animation.getValue();
      float size = 12.0F;
      Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
      ColorRGBA color = theme.getColor().withAlpha((int) (alpha * 255.0F));
      DrawUtil.drawTexture(matrices, textureId, 0.0F - size / 2.0F, 0.0F - size / 2.0F, size, size, color);
      matrices.pop();
   }

   @Native
   private void updateRotation() {
      if (!this.isReversing) {
         this.rotationSpeed += 0.01F;
         if ((double) this.rotationSpeed > 2.3D) {
            this.rotationSpeed = 2.3F;
            this.isReversing = true;
         }
      } else {
         this.rotationSpeed -= 0.01F;
         if ((double) this.rotationSpeed < -2.3D) {
            this.rotationSpeed = -2.3F;
            this.isReversing = false;
         }
      }
      this.rotationAngle += this.rotationSpeed;
      this.rotationAngle %= 360.0F;
   }

   public static double interpolate(double current, double old, double scale) {
      return old + (current - old) * scale;
   }

   private void drawSpirits(EventRender3D e) {
      MatrixStack matrices = e.getMatrix();
      Vec3d camPos = mc.gameRenderer.getCamera().getPos();

      double x = interpolate(this.lastTarget.getX(), this.lastTarget.lastRenderX, (double) e.getPartialTicks()) - camPos.x;
      double y = interpolate(this.lastTarget.getY(), this.lastTarget.lastRenderY, (double) e.getPartialTicks()) - camPos.y + (double) this.lastTarget.getHeight() / 2.0;
      double z = interpolate(this.lastTarget.getZ(), this.lastTarget.lastRenderZ, (double) e.getPartialTicks()) - camPos.z;

      float hurtTime = 0.0F;
      if (this.lastTarget instanceof LivingEntity living) {
         hurtTime = ((float) living.hurtTime - (living.hurtTime != 0 ? e.getPartialTicks() : 0.0F)) / 10.0F;
      }

      float animValue = -0.15F * this.animation2.getValue() + 0.65F;
      long time = (long) ((float) (System.currentTimeMillis() - this.timestamp4) / 2.0F);
      long nanoTime = System.nanoTime();
      float deltaTime = (float) (nanoTime - this.timestamp5) / 2000000.0F;
      this.timestamp5 = nanoTime;
      this.value23 += hurtTime * deltaTime;

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
            float progress = (float) i / 13.0F;
            float size = (0.55F * (1.0F - progress) + 0.2F * progress) * this.animation2.getValue();
            double angle = (double) (0.2F * ((float) time + this.value23 - (float) i * 7.0F) / 15.0F);

            boolean firstHalf = progress < 0.5F;
            float wave = firstHalf ? progress * 2.0F : (1.0F - progress) * 2.0F;
            double amplitude = Math.sin((double) wave * Math.PI) * 2.0;

            Random random = new Random((long) i * 12345L);
            double offsetX = (random.nextDouble() - 0.5) * amplitude;
            double offsetY = (random.nextDouble() - 0.5) * amplitude;
            double offsetZ = (random.nextDouble() - 0.5) * amplitude;

            double animOffsetX = offsetX * (double) this.animation2.getValue() - offsetX;
            double animOffsetY = offsetY * (double) this.animation2.getValue() - offsetY;
            double animOffsetZ = offsetZ * (double) this.animation2.getValue() - offsetZ;

            double posX = -Math.sin(angle) * (double) animValue;
            double posZ = -Math.cos(angle) * (double) animValue;

            switch (layer) {
               case 0:
                  animOffsetY += (double) i * 0.02;
                  matrices.translate(posX + animOffsetX, posZ + animOffsetY, -posZ + animOffsetZ);
                  break;
               case 1:
                  animOffsetY -= (double) i * 0.02;
                  matrices.translate(-posX + animOffsetX, posX + animOffsetY, -posZ + animOffsetZ);
                  break;
               case 2:
                  matrices.translate(-posX + animOffsetX, -posX + animOffsetY, posZ + animOffsetZ);
            }

            float particleSize = size * 0.5F;
            Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
            ColorRGBA color = theme.getColor().withAlpha((int) (600.0F * this.animation2.getValue()));

            matrices.multiply(mc.gameRenderer.getCamera().getRotation());

            buffer.vertex(matrices.peek().getPositionMatrix(), -particleSize, -particleSize, 0.0F).texture(1.0F, 1.0F).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), particleSize, -particleSize, 0.0F).texture(0.0F, 1.0F).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), particleSize, particleSize, 0.0F).texture(0.0F, 0.0F).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), -particleSize, particleSize, 0.0F).texture(1.0F, 0.0F).color(color.getRGB());

            matrices.pop();
         }
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
      RenderSystem.blendFunc(770, 771);
      RenderSystem.enableCull();
      matrices.pop();
   }

   private void renderNursultan(EventRender3D e) {
      MatrixStack matrices = e.getMatrix();
      Vec3d camPos = mc.gameRenderer.getCamera().getPos();

      double x = interpolate(this.lastTarget.getX(), this.lastTarget.lastRenderX, (double) e.getPartialTicks()) - camPos.x;
      double y = interpolate(this.lastTarget.getY(), this.lastTarget.lastRenderY, (double) e.getPartialTicks()) - camPos.y + (double) this.lastTarget.getHeight() / 2.0;
      double z = interpolate(this.lastTarget.getZ(), this.lastTarget.lastRenderZ, (double) e.getPartialTicks()) - camPos.z;

      float time = (float) (System.currentTimeMillis() - this.timestamp4) / 1100.0F;
      float rotSpd = 360.0F;
      float rotation = time * rotSpd;
      float radius = 0.5F;

      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(770, 1, 0, 1);
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);

      BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      for (int layer = 0; layer < 4; layer++) {
         float layerOffset = (float) (layer - 1) * 0.4F;
         float prevSize = -1.0F;
         for (float i = 0.0F; i < 130.0F; i++) {
            float angle = rotation + i + layerOffset * 360.0F;
            double radians = Math.toRadians(-angle);
            double yOffset = Math.sin(radians + 2.0) * (double) layerOffset;
            float size = radius * (i / 140.0F);
            float finalSize = prevSize >= 0.0F ? (prevSize + size) / 2.0F : size;
            prevSize = size;
            finalSize *= this.animation2.getValue();

            float alpha = MathHelper.clamp(finalSize, 0.0F, 1.0F);
            Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
            ColorRGBA color = theme.getColor().withAlpha((int) (600.0 * this.animation2.getValue() * alpha));

            matrices.push();
            matrices.translate(x, y + yOffset, z);
            matrices.multiply(mc.gameRenderer.getCamera().getRotation());

            float halfSize = finalSize / 2.0F;
            double cosAngle = Math.cos(radians) * (double) radius - (double) halfSize;
            double sinAngle = Math.sin(radians) * (double) radius - (double) halfSize;

            buffer.vertex(matrices.peek().getPositionMatrix(), (float) cosAngle, -halfSize, (float) sinAngle).texture(0.0F, 1.0F).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) (cosAngle + finalSize), -halfSize, (float) sinAngle).texture(1.0F, 1.0F).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) (cosAngle + finalSize), halfSize, (float) sinAngle).texture(1.0F, 0.0F).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) cosAngle, halfSize, (float) sinAngle).texture(0.0F, 0.0F).color(color.getRGB());

            matrices.pop();
         }
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
      RenderSystem.blendFunc(770, 771);
      RenderSystem.enableCull();
   }

   private void drawCircle(EventRender3D e) {
      MatrixStack matrices = e.getMatrix();
      Vec3d camPos = mc.gameRenderer.getCamera().getPos();

      double x = interpolate(this.lastTarget.getX(), this.lastTarget.lastRenderX, (double) e.getPartialTicks()) - camPos.x;
      double y = interpolate(this.lastTarget.getY(), this.lastTarget.lastRenderY, (double) e.getPartialTicks()) - camPos.y;
      double z = interpolate(this.lastTarget.getZ(), this.lastTarget.lastRenderZ, (double) e.getPartialTicks()) - camPos.z;

      float height = this.lastTarget.getHeight();
      short period = 1500;
      double time = (double) (System.currentTimeMillis() % (long) period);
      boolean ascending = time > (double) (period / 2);
      float progress = (float) (time / (double) ((float) period / 2.0F));

      if (ascending) progress -= 1.0F;
      else progress = 1.0F - progress;

      progress = (double) progress < 0.5 ? 2.0F * progress * progress :
              (float) (1.0 - Math.pow((double) (-2.0F * progress + 2.0F), 2.0) / 2.0);

      double yOffset = (double) (height / 2.0F * ((double) progress > 0.5 ? 1.0F - progress : progress) * (float) (ascending ? -1 : 1));

      matrices.push();
      matrices.translate(x, y + (double) (height * progress) + yOffset, z);

      float hurtTime = 0.0F;
      if (this.lastTarget instanceof LivingEntity living) {
         hurtTime = ((float) living.hurtTime - (living.hurtTime != 0 ? e.getPartialTicks() : 0.0F)) / 10.0F;
      }

      long timeMs = (long) ((float) (System.currentTimeMillis() - this.timestamp4) / 2.5F);
      long nanoTime = System.nanoTime();
      float deltaTime = (float) (nanoTime - this.timestamp5) / 2000000.0F;
      this.timestamp5 = nanoTime;
      this.value23 += hurtTime * deltaTime;

      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(770, 1, 0, 1);
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);

      BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      for (int layer = 0; layer < 4; layer++) {
         for (int i = 0; i < 15; i++) {
            matrices.push();
            float particleProgress = (float) i / 14.0F;
            float size = (0.5F * (1.0F - particleProgress) + 0.5F * particleProgress) * this.animation2.getValue();
            float angle = 0.2F * ((float) timeMs + this.value23 - (float) i * 3.5F) / 15.0F;

            boolean firstHalf = particleProgress < 0.5F;
            float wave = firstHalf ? particleProgress * 2.0F : (1.0F - particleProgress) * 2.0F;
            double amplitude = Math.sin((double) wave * Math.PI) * 2.0;

            Random random = new Random((long) i * 12345L);
            double offsetX = (random.nextDouble() - 0.5) * amplitude;
            double offsetY = (random.nextDouble() - 0.5) * amplitude;
            double offsetZ = (random.nextDouble() - 0.5) * amplitude;

            double animOffsetX = offsetX * (double) this.animation2.getValue() - offsetX;
            double animOffsetY = offsetY * (double) this.animation2.getValue() - offsetY;
            double animOffsetZ = offsetZ * (double) this.animation2.getValue() - offsetZ;

            double radius = 0.7;
            switch (layer) {
               case 0:
                  matrices.translate(Math.cos((double) angle) * radius + animOffsetX, animOffsetY, Math.sin((double) angle) * radius + animOffsetZ);
                  break;
               case 1:
                  matrices.translate(-Math.sin((double) angle) * radius + animOffsetX, animOffsetY, Math.cos((double) angle) * radius + animOffsetZ);
                  break;
               case 2:
                  matrices.translate(-Math.cos((double) angle) * radius + animOffsetX, animOffsetY, -Math.sin((double) angle) * radius + animOffsetZ);
                  break;
               case 3:
                  matrices.translate(Math.sin((double) angle) * radius + animOffsetX, animOffsetY, -Math.cos((double) angle) * radius + animOffsetZ);
            }

            float particleSize = size * 0.5F;
            Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
            ColorRGBA color = theme.getColor().withAlpha((int) (400.0F * this.animation2.getValue()));

            matrices.multiply(mc.gameRenderer.getCamera().getRotation());

            buffer.vertex(matrices.peek().getPositionMatrix(), -particleSize, -particleSize, 0.0F).texture(1.0F, 1.0F).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), particleSize, -particleSize, 0.0F).texture(0.0F, 1.0F).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), particleSize, particleSize, 0.0F).texture(0.0F, 0.0F).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), -particleSize, particleSize, 0.0F).texture(1.0F, 0.0F).color(color.getRGB());

            matrices.pop();
         }
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
      RenderSystem.blendFunc(770, 771);
      RenderSystem.enableCull();
      matrices.pop();
   }

   private void drawSpiritsTrack(EventRender3D event3D) {
      Aura aura = Aura.INSTANCE;
      this.animation2.update(aura.getTarget() != null && aura.isEnabled());
      if ((double) this.animation2.getValue() != 0.0D) {
         MatrixStack e = event3D.getMatrix();
         if (aura.getTarget() != null) this.lastTarget = aura.getTarget();

         if (this.lastTarget != null) {
            long currentTimeMs = System.currentTimeMillis();
            this.animationNurik += (float) (currentTimeMs - this.currentTime) / 120.0F;
            this.currentTime = currentTimeMs;

            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(770, 1, 0, 1);
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            double x = interpolate(this.lastTarget.getX(), this.lastTarget.lastRenderX, (double) event3D.getPartialTicks()) - mc.gameRenderer.getCamera().getPos().getX();
            double y = interpolate(this.lastTarget.getY(), this.lastTarget.lastRenderY, (double) event3D.getPartialTicks()) - mc.gameRenderer.getCamera().getPos().getY();
            double z = interpolate(this.lastTarget.getZ(), this.lastTarget.lastRenderZ, (double) event3D.getPartialTicks()) - mc.gameRenderer.getCamera().getPos().getZ();
            int n2 = 3, n3 = 12, n4 = 3 * n2;
            e.push();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (int i = 0; i < n4; i += n2) {
               for (int j = 0; j < n3; ++j) {
                  Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
                  ColorRGBA color = theme.getColor();
                  float f2 = this.animationNurik + (float) j * 0.1F;
                  int n5 = (int) Math.pow((double) i, 2.0D);
                  e.push();
                  e.translate(x + (double) (0.8F * MathHelper.sin(f2 + (float) n5)), y + 0.5 + (double) (0.3F * MathHelper.sin(this.animationNurik + (float) j * 0.2F)) + (double) (0.2F * (float) i), z + (double) (0.8F * MathHelper.cos(f2 - (float) n5)));
                  e.scale(this.animation2.getValue() * (0.005F + (float) j / 2000.0F), this.animation2.getValue() * (0.005F + (float) j / 2000.0F), this.animation2.getValue() * (0.005F + (float) j / 2000.0F));
                  e.multiply(mc.gameRenderer.getCamera().getRotation());
                  int n7 = -25, n8 = 50;
                  int rgba = color.withAlpha((int) (this.animation2.getValue() * 600.0F)).getRGB();
                  buffer.vertex(e.peek().getPositionMatrix(), (float) n7, (float) (n7 + n8), 0.0F).texture(0.0F, 1.0F).color(rgba);
                  buffer.vertex(e.peek().getPositionMatrix(), (float) (n7 + n8), (float) (n7 + n8), 0.0F).texture(1.0F, 1.0F).color(rgba);
                  buffer.vertex(e.peek().getPositionMatrix(), (float) (n7 + n8), (float) n7, 0.0F).texture(1.0F, 0.0F).color(rgba);
                  buffer.vertex(e.peek().getPositionMatrix(), (float) n7, (float) n7, 0.0F).texture(0.0F, 0.0F).color(rgba);
                  e.pop();
               }
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            e.pop();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.blendFunc(770, 771);
            RenderSystem.enableCull();
         }
      }
   }

   private void drawGhostOrbits(EventRender3D e) {
      if (this.lastTarget == null) return;

      MatrixStack matrices = e.getMatrix();
      Vec3d camPos = mc.gameRenderer.getCamera().getPos();
      float delta = e.getPartialTicks();
      Camera camera = mc.gameRenderer.getCamera();

      double tx = interpolate(this.lastTarget.getX(), this.lastTarget.lastRenderX, delta);
      double ty = interpolate(this.lastTarget.getY(), this.lastTarget.lastRenderY, delta);
      double tz = interpolate(this.lastTarget.getZ(), this.lastTarget.lastRenderZ, delta);
      Vec3d targetCenter = new Vec3d(tx, ty + this.lastTarget.getHeight() / 2.0, tz);

      long now = System.currentTimeMillis();
      if (lastOrbitTime == 0) lastOrbitTime = now;
      float dtMs = now - lastOrbitTime;
      lastOrbitTime = now;

      float fpsFactor = 500 / Math.max(mc.getCurrentFps(), 10);
      movingAngle += (20.0f * dtMs / 16.667f) * (ORBIT_SPEED / 55.0f);

      boolean isHurt = false;
      if (this.lastTarget instanceof LivingEntity living) isHurt = living.hurtTime > 7;
      orbitShrinkAnim.update(isHurt);
      float shrinkValue = orbitShrinkAnim.getValue();

      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(770, 1);
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);

      BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
      ColorRGBA baseColor = theme.getColor();

      for (int i = 0; i < ORBIT_PARTICLE_COUNT; i++) {
         float angleOffset = i * 360f / ORBIT_PARTICLE_COUNT;
         float currentAngle = movingAngle + angleOffset;
         double radian = Math.toRadians(currentAngle);

         float orbitRadius = ORBIT_BASE_RADIUS - shrinkValue * ORBIT_BASE_RADIUS;
         float ox = (float) Math.sin(radian) * orbitRadius;
         float oz = (float) Math.cos(radian) * orbitRadius;
         double oy = 0.3 * Math.sin(Math.toRadians(movingAngle / (i + 1.0f)));

         Vec3d targetGhostPos = targetCenter.add(ox, oy, oz);

         if (orbitPositions[i] == null || orbitPositions[i].distanceTo(targetGhostPos) > 10) {
            orbitPositions[i] = targetGhostPos;
            orbitMotions[i] = Vec3d.ZERO;
         }

         float mul = ORBIT_BASE_MUL * fpsFactor;
         Vec3d diff = targetGhostPos.subtract(orbitPositions[i]);
         orbitMotions[i] = diff.multiply(mul, mul, mul);
         orbitPositions[i] = orbitPositions[i].add(orbitMotions[i]);

         if (orbitTrails[i].isEmpty() || orbitTrails[i].get(0).distanceTo(orbitPositions[i]) > 0.01) {
            orbitTrails[i].add(0, orbitPositions[i]);
            while (orbitTrails[i].size() > ORBIT_TRAIL_LENGTH) orbitTrails[i].remove(orbitTrails[i].size() - 1);
         }

         for (int j = 0; j < orbitTrails[i].size(); j++) {
            Vec3d p = orbitTrails[i].get(j);
            float offset = 1.0f - (float) j / ORBIT_TRAIL_LENGTH;

            matrices.push();
            matrices.translate(p.x - camPos.x, p.y - camPos.y, p.z - camPos.z);
            matrices.multiply(camera.getRotation());
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            float opacity = (float) Math.pow(offset, 1.8) * this.animation2.getValue() * 0.7f;
            int color = baseColor.withAlpha((int)(opacity * 255)).getRGB();
            float scale = SCALE_CACHE[Math.min((int) (offset * 100), 100)] * 0.8f;

            buffer.vertex(matrix, -scale, scale, 0).texture(0f, 1f).color(color);
            buffer.vertex(matrix, scale, scale, 0).texture(1f, 1f).color(color);
            buffer.vertex(matrix, scale, -scale, 0).texture(1f, 0f).color(color);
            buffer.vertex(matrix, -scale, -scale, 0).texture(0f, 0f).color(color);
            matrices.pop();
         }

         if (!orbitTrails[i].isEmpty()) {
            Vec3d head = orbitTrails[i].get(0);
            matrices.push();
            matrices.translate(head.x - camPos.x, head.y - camPos.y, head.z - camPos.z);
            matrices.multiply(camera.getRotation());
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            float headScale = 0.35f * this.animation2.getValue();
            int headColor = baseColor.withAlpha((int)(120 * this.animation2.getValue())).getRGB();

            buffer.vertex(matrix, -headScale, headScale, 0).texture(0f, 1f).color(headColor);
            buffer.vertex(matrix, headScale, headScale, 0).texture(1f, 1f).color(headColor);
            buffer.vertex(matrix, headScale, -headScale, 0).texture(1f, 0f).color(headColor);
            buffer.vertex(matrix, -headScale, -headScale, 0).texture(0f, 0f).color(headColor);
            matrices.pop();
         }
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
      RenderSystem.blendFunc(770, 771);
      RenderSystem.enableCull();
   }

   private void renderCrystals(EventRender3D e) {
      float alpha = this.animation2.getValue();
      if (alpha <= 0.0F) return;
      if (this.lastTarget == null) return;
      if (mc.player == null) return;

      MatrixStack matrices = e.getMatrix();
      Vec3d camPos = mc.gameRenderer.getCamera().getPos();
      float tickDelta = e.getPartialTicks();

      double tx = interpolate(this.lastTarget.getX(), this.lastTarget.lastRenderX, tickDelta);
      double ty = interpolate(this.lastTarget.getY(), this.lastTarget.lastRenderY, tickDelta);
      double tz = interpolate(this.lastTarget.getZ(), this.lastTarget.lastRenderZ, tickDelta);

      crystalMoving += 1.0f;

      float entityHeight = this.lastTarget.getHeight();
      float entityWidth = this.lastTarget.getWidth();
      float width = entityWidth * 1.5f;

      Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
      ColorRGBA themeColor = theme.getColor();
      int colorRGB = themeColor.getRGB() & 0x00FFFFFF;
      int cr = (colorRGB >> 16) & 0xFF;
      int cg = (colorRGB >> 8) & 0xFF;
      int cb = colorRGB & 0xFF;

      matrices.push();
      matrices.translate(tx - camPos.x, ty - camPos.y, tz - camPos.z);

      RenderSystem.enableBlend();

      RenderSystem.blendFunc(770, 1);
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);

      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

      BufferBuilder crystalBuffer = Tessellator.getInstance().begin(
              VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

      int crystalAlpha = Math.min(255, (int) (alpha * 255));
      int color = (crystalAlpha << 24) | (cr << 16) | (cg << 8) | cb;

      int cTop = color;
      int cSide1 = color;
      int cSide2 = color;
      int cBot = color;

      float cw = 0.085f;
      float ch = 0.22f;

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

         float[] ex = {cw, 0, -cw, 0};
         float[] ez = {0, cw, 0, -cw};

         for (int j = 0; j < 4; j++) {
            int next = (j + 1) % 4;
            int fc = (j % 2 == 0) ? cTop : cSide1;
            crystalBuffer.vertex(matrix, 0, ch, 0).color(fc);
            crystalBuffer.vertex(matrix, ex[j], 0, ez[j]).color(fc);
            crystalBuffer.vertex(matrix, ex[next], 0, ez[next]).color(fc);
         }

         for (int j = 0; j < 4; j++) {
            int next = (j + 1) % 4;
            int fc = (j % 2 == 0) ? cBot : cSide2;
            crystalBuffer.vertex(matrix, 0, -ch, 0).color(fc);
            crystalBuffer.vertex(matrix, ex[next], 0, ez[next]).color(fc);
            crystalBuffer.vertex(matrix, ex[j], 0, ez[j]).color(fc);
         }

         matrices.pop();
      }

      BufferRenderer.drawWithGlobalProgram(crystalBuffer.end());
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, GLOW_TEXTURE);

      BufferBuilder glowBuffer = Tessellator.getInstance().begin(
              VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

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
         int glowAlpha = (int) Math.min(255, alpha * 200);
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

   private ColorRGBA blendColors(ColorRGBA c1, ColorRGBA c2, float ratio) {
      int r = (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
      int g = (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
      int b = (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
      int a = (int) (c1.getAlpha() * (1 - ratio) + c2.getAlpha() * ratio);
      return new ColorRGBA(r, g, b, a);
   }

   private void drawGhosts3(EventRender3D e) {
      if (this.lastTarget == null) return;

      float fadeAnim = this.animation.getValue();
      if (fadeAnim <= 0.0f) return;

      MatrixStack ms = e.getMatrix();
      Camera camera = mc.gameRenderer.getCamera();
      float tickDelta = e.getPartialTicks();
      float moving = ((mc.player != null ? mc.player.age : 0) + tickDelta) * 13.0f;

      Vec3d targetPos = new Vec3d(
              MathHelper.lerp(tickDelta, this.lastTarget.lastRenderX, this.lastTarget.getX()),
              MathHelper.lerp(tickDelta, this.lastTarget.lastRenderY, this.lastTarget.getY()),
              MathHelper.lerp(tickDelta, this.lastTarget.lastRenderZ, this.lastTarget.getZ())
      );

      float width = this.lastTarget.getWidth() * 1.5f;
      float entityHeight = this.lastTarget.getHeight();
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

      int step = 2;
      int wormTick = 0;
      int wormCD = 0;

      for (int i = 0; i < 360; i += step) {
         float size = 0.13f + 0.005f * wormTick;
         float bigSize = 0.7f + 0.005f * wormTick;

         if (wormCD > 0) {
            wormCD -= step;
            continue;
         }

         if ((wormTick += step) > 50) {
            wormCD = 100;
            wormTick = 0;
            continue;
         }

         float val = Math.max(0.5f, 1.2f - 0.5f * fadeAnim);
         float sin = (float) (Math.sin(Math.toRadians(i + moving)) * width * val);
         float cos = (float) (Math.cos(Math.toRadians(i + moving)) * width * val);
         float yAnim = (float) Math.sin(Math.toRadians(i / 2.0f + moving / 5.0f));

         ms.push();
         ms.translate(
                 targetPos.x + sin - camera.getPos().x,
                 targetPos.y + (entityHeight / 1.5f) + (entityHeight / 3.0f) * yAnim - camera.getPos().y,
                 targetPos.z + cos - camera.getPos().z
         );
         ms.multiply(camera.getRotation());

         Matrix4f matrix = ms.peek().getPositionMatrix();
         float glowHalf = bigSize / 2.0f;
         this.addTexturedQuad(builder, matrix, glowHalf, glowArgb);
         float coreHalf = size / 2.0f;
         this.addTexturedQuad(builder, matrix, coreHalf, coreArgb);

         ms.pop();
      }

      BufferRenderer.drawWithGlobalProgram(builder.end());

      RenderSystem.enableCull();
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
   }

   private void addTexturedQuad(BufferBuilder builder, Matrix4f matrix, float half, int argb) {
      int a = (argb >> 24) & 0xFF;
      int r = (argb >> 16) & 0xFF;
      int g = (argb >> 8) & 0xFF;
      int b = argb & 0xFF;
      builder.vertex(matrix, -half, -half, 0.0F).texture(0.0F, 1.0F).color(r, g, b, a);
      builder.vertex(matrix, half, -half, 0.0F).texture(1.0F, 1.0F).color(r, g, b, a);
      builder.vertex(matrix, half, half, 0.0F).texture(1.0F, 0.0F).color(r, g, b, a);
      builder.vertex(matrix, -half, half, 0.0F).texture(0.0F, 0.0F).color(r, g, b, a);
   }

   private void drawGhosts4(EventRender3D e) {
      float anim = this.animation.getValue();
      if (anim <= 0.01F) return;

      Vec3d renderBasePos = this.resolveRenderBasePosition(e.getPartialTicks());
      if (renderBasePos == null) return;

      float dynamicScale = this.animation2.getValue() * (0.45F + anim * 0.55F);
      dynamicScale = Math.max(0.05F, dynamicScale);

      this.spawnGhostTrails(renderBasePos, this.anchorHeight);
      this.drawGhostTrails(e.getMatrix(), anim, dynamicScale);
   }

   private Vec3d resolveRenderBasePosition(float partialTicks) {
      if (this.lastTarget != null) {
         this.anchorHeight = this.lastTarget.getHeight();
         this.anchorPosition = new Vec3d(
                 MathHelper.lerp(partialTicks, this.lastTarget.lastRenderX, this.lastTarget.getX()),
                 MathHelper.lerp(partialTicks, this.lastTarget.lastRenderY, this.lastTarget.getY()),
                 MathHelper.lerp(partialTicks, this.lastTarget.lastRenderZ, this.lastTarget.getZ())
         );
         return this.anchorPosition;
      }
      return this.anchorPosition;
   }

   private void spawnGhostTrails(Vec3d basePos, float entityHeight) {
      int trailLength = 65;
      double rotationSpeed = 0.0050;
      double orbitRadius = 0.65;
      long time = System.currentTimeMillis() - this.ghostStartTime;

      double baseX = basePos.x;
      double baseY = basePos.y - 0.25;
      double baseZ = basePos.z;

      for (int i = 0; i < this.ghostTrails.size(); i++) {
         Deque<GhostPoint> trail = this.ghostTrails.get(i);
         trail.clear();

         for (int j = 0; j < trailLength; j++) {
            double ageOffset = j * 10.0;
            double angle = (time - ageOffset) * rotationSpeed + (i * 2.0) * Math.PI / this.ghostTrails.size();
            double dx = Math.cos(angle) * orbitRadius;
            double dz = Math.sin(angle) * orbitRadius;
            double yo = Math.sin((time + i * 759.0 - ageOffset) * 0.003) * 0.6;

            trail.addLast(new GhostPoint(
                    new Vec3d(baseX + dx + 0.1, baseY + entityHeight * 0.5 + yo + 0.25, baseZ + dz),
                    0.5F
            ));
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
      int baseColor = this.interpolateColor(theme.getColor().withAlpha(255).getRGB(), 0xFFFF1414, this.colorInterpAnim);

      for (Deque<GhostPoint> trail : this.ghostTrails) {
         int index = 0;
         int trailLength = Math.max(1, trail.size());

         for (GhostPoint point : trail) {
            float tailFactor = 1.0F - (float) index / trailLength;
            float ghostSize = point.size * dynamicScale * (1.1F - (float) index / trailLength);
            if (ghostSize <= 0.001F) {
               index++;
               continue;
            }

            int alpha = (int) (255.0F * anim * tailFactor);
            if (alpha <= 0) {
               index++;
               continue;
            }

            int color = ColorUtil.multAlpha(baseColor, alpha / 255.0f);
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            int a = (color >> 24) & 0xFF;

            double dx = point.position.x - camPos.x;
            double dy = point.position.y - camPos.y;
            double dz = point.position.z - camPos.z;

            matrix.push();
            matrix.translate(dx, dy, dz);
            matrix.multiply(camera.getRotation());
            matrix.translate(-ghostSize / 2.0F, -ghostSize / 2.0F, 0.0F);

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
      if (built != null) {
         BufferRenderer.drawWithGlobalProgram(built);
      }

      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
   }

   private void clearGhostTrails() {
      for (Deque<GhostPoint> trail : this.ghostTrails) {
         trail.clear();
      }
   }

   private float lerp(float from, float to, float speed) {
      return from + (to - from) * MathHelper.clamp(speed, 0.0F, 1.0F);
   }

   private int interpolateColor(int color1, int color2, float ratio) {
      ratio = MathHelper.clamp(ratio, 0.0F, 1.0F);
      int a1 = (color1 >> 24) & 0xFF;
      int r1 = (color1 >> 16) & 0xFF;
      int g1 = (color1 >> 8) & 0xFF;
      int b1 = color1 & 0xFF;

      int a2 = (color2 >> 24) & 0xFF;
      int r2 = (color2 >> 16) & 0xFF;
      int g2 = (color2 >> 8) & 0xFF;
      int b2 = color2 & 0xFF;

      int a = (int) (a1 + (a2 - a1) * ratio);
      int r = (int) (r1 + (r2 - r1) * ratio);
      int g = (int) (g1 + (g2 - g1) * ratio);
      int b = (int) (b1 + (b2 - b1) * ratio);
      return (a << 24) | (r << 16) | (g << 8) | b;
   }

   private static final class GhostPoint {
      private final Vec3d position;
      private final float size;

      private GhostPoint(Vec3d position, float size) {
         this.position = position;
         this.size = size;
      }
   }
}
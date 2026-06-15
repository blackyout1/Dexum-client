package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext.ShapeType;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.render.EventRender2D;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.base.font.Font;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.utility.game.player.PlayerIntersectionUtil;
import wtf.dexum.utility.game.player.RaytracingUtil;
import wtf.dexum.utility.math.ProjectionUtil;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;
import wtf.dexum.utility.render.level.Render3DUtil;

@ModuleAnnotation(
   name = "Predictions",
   category = Category.RENDER,
   description = "Показывает куда упадет предмет"
)
public final class Predictions extends Module {
   private final List<Predictions.Point> points = new ArrayList();
   public static Predictions INSTANCE = new Predictions();

   private Predictions() {
   }

   @EventTarget
   public void onDraw(EventRender2D e) {
      Iterator var2 = this.points.iterator();

      while(var2.hasNext()) {
         Predictions.Point point = (Predictions.Point)var2.next();
         Vec3d vec3d = ProjectionUtil.worldSpaceToScreenSpace(point.pos);
         int ticks = point.ticks;
         if (ProjectionUtil.canSee(point.pos)) {
            Font font = Fonts.REGULAR.getFont(7.0F);
            double time = (double)(ticks * 50) / 1000.0D;
            Object[] var10001 = new Object[]{time};
            String text = String.format("%.1f", var10001) + " сек";
            float textWidth = font.width(text);
            float padding = 2.0F;
            float centerX = (float)vec3d.getX();
            float centerY = (float)vec3d.getY();
            float totalHeight = font.height();
            float rectX = centerX - textWidth / 2.0F;
            float rectY = centerY - totalHeight / 2.0F;
            float textY = rectY + 5.0F;
            DrawUtil.drawRoundedRect(e.getContext().getMatrices(), rectX - 8.0F, textY - 2.0F, textWidth + 16.0F, totalHeight + 5.0F, BorderRadius.ZERO, new ColorRGBA(0, 0, 0, 120));
            e.getContext().getMatrices().push();
            e.getContext().getMatrices().translate(rectX - 6.0F, textY - 1.0F, 0.0F);
            e.getContext().getMatrices().scale(0.5F, 0.5F, 1.0F);
            e.getContext().drawItem(point.stack(), 0, 0);
            e.getContext().getMatrices().scale(1.0F, 1.0F, 1.0F);
            e.getContext().getMatrices().translate(-(rectX - 7.0F), -(textY - 1.0F), 0.0F);
            e.getContext().getMatrices().pop();
            e.getContext().drawText(font, text.replace(",", "."), rectX + 5.0F, textY + 0.5F, ColorRGBA.WHITE);
         }
      }

   }

   @EventTarget
   public void onWorldRender(EventRender3D e) {
      this.points.clear();
      this.getProjectiles().forEach((entity) -> {
         Vec3d motion = entity.getVelocity();
         Vec3d pos = entity.getPos();
         int ticks = 0;

         for(int i = 0; i < 300; ++i) {
            Vec3d prevPos = pos;
            pos = pos.add(motion);
            motion = this.calculateMotion(entity, prevPos, motion);
            HitResult result = RaytracingUtil.raycast(prevPos, pos, ShapeType.COLLIDER, entity);
            if (!result.getType().equals(Type.MISS)) {
               pos = result.getPos();
            }

            Render3DUtil.drawLine(prevPos, pos, Dexum.getInstance().getThemeManager().getClientColor(i).mulAlpha(MathHelper.clamp((float)i / 25.0F, 0.0F, 1.0F)).getRGB(), 2.0F, false);

            int glowColor = Dexum.getInstance().getThemeManager().getClientColor(i).mulAlpha(MathHelper.clamp((float)i / 50.0F, 0.0F, 0.3F)).getRGB();
            Render3DUtil.drawLine(prevPos, pos, glowColor, 4.5F, false);
            Render3DUtil.drawLine(prevPos, pos, glowColor, 7.0F, false);

            final Vec3d finalPrevPos = prevPos;
            final Vec3d finalPos = pos;
            boolean inEntity = PlayerIntersectionUtil.streamEntities().filter((ent) -> {
               boolean var10000;
               if (ent instanceof LivingEntity) {
                  LivingEntity living = (LivingEntity)ent;
                  if (living != mc.player && living.isAlive()) {
                     var10000 = true;
                     return var10000;
                  }
               }

               var10000 = false;
               return var10000;
            }).anyMatch((ent) -> {
               return ent.getBoundingBox().expand(0.25D).intersects(finalPrevPos, finalPos);
            });
            if (result.getType().equals(Type.BLOCK) || pos.y < -128.0D || inEntity || result.getType().equals(Type.ENTITY)) {
               this.BreakingBad(entity, pos, ticks);
               break;
            }

            ++ticks;
         }

      });
   }

   public List<Entity> getProjectiles() {
      return PlayerIntersectionUtil.streamEntities().filter((e) -> {
         return (e instanceof PersistentProjectileEntity || e instanceof ThrownItemEntity || e instanceof ItemEntity) && !this.visible(e);
      }).toList();
   }

   public Vec3d calculateMotion(Entity entity, Vec3d prevPos, Vec3d motion) {
      boolean isInWater = ((ClientWorld)Objects.requireNonNull(mc.world)).getBlockState(BlockPos.ofFloored(prevPos)).getFluidState().isIn(FluidTags.WATER);

      float multiply;
      if (entity instanceof TridentEntity) {
         multiply = 0.99F;
      } else if (entity instanceof PersistentProjectileEntity) {
         multiply = isInWater ? 0.6F : 0.99F;
      } else {
         multiply = isInWater ? 0.8F : 0.99F;
      }

      return motion.multiply((double)multiply).add(0.0D, -entity.getFinalGravity(), 0.0D);
   }

   private void BreakingBad(Entity entity, Vec3d pos, int ticks) {
      if (entity instanceof ItemEntity item) {
         this.points.add(new Predictions.Point(item.getStack(), pos, ticks));
      } else if (entity instanceof ThrownItemEntity thrown) {
         this.points.add(new Predictions.Point(thrown.getStack(), pos, ticks));
      } else if (entity instanceof PersistentProjectileEntity persistent) {
         this.points.add(new Predictions.Point(persistent.getItemStack(), pos, ticks));
      }
   }

   private boolean visible(Entity entity) {
      boolean posChange = entity.getX() == entity.prevX && entity.getY() == entity.prevY && entity.getZ() == entity.prevZ;
      boolean itemEntityCheck = entity instanceof ItemEntity && (entity.isOnGround() || PlayerIntersectionUtil.isBoxInBlock(entity.getBoundingBox().expand(2.0D), Blocks.WATER));
      return posChange || itemEntityCheck;
   }

   private static record Point(ItemStack stack, Vec3d pos, int ticks) {
      private Point(ItemStack stack, Vec3d pos, int ticks) {
         this.stack = stack;
         this.pos = pos;
         this.ticks = ticks;
      }

      public ItemStack stack() {
         return this.stack;
      }

      public Vec3d pos() {
         return this.pos;
      }

      public int ticks() {
         return this.ticks;
      }
   }
}
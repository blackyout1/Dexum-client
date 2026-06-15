package wtf.dexum.utility.render.particles;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.level.Render3DUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ParticleEngine {
    private static final List<Particle> particles = new ArrayList<>();

    public static void addParticle(Vec3d pos, Vec3d velocity, ColorRGBA color, float size, long lifetime) {
        particles.add(new Particle(pos, velocity, color, size, lifetime));
    }

    public static void update() {
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle p = iterator.next();
            if (p.isDead()) {
                iterator.remove();
                continue;
            }
            p.update();
        }
    }

    public static void render3D(MatrixStack matrices) {
        for (Particle p : particles) {
            p.render3D(matrices);
        }
    }

    public static class Particle {
        public Vec3d pos;
        public Vec3d velocity;
        public ColorRGBA color;
        public float size;
        public long spawnTime;
        public long lifetime;

        public Particle(Vec3d pos, Vec3d velocity, ColorRGBA color, float size, long lifetime) {
            this.pos = pos;
            this.velocity = velocity;
            this.color = color;
            this.size = size;
            this.spawnTime = System.currentTimeMillis();
            this.lifetime = lifetime;
        }

        public void update() {
            pos = pos.add(velocity);
            velocity = velocity.multiply(0.95);
        }

        public boolean isDead() {
            return System.currentTimeMillis() - spawnTime > lifetime;
        }

        public void render3D(MatrixStack matrices) {
            float ageProgress = 1.0f - ((float) (System.currentTimeMillis() - spawnTime) / lifetime);
            ColorRGBA renderColor = color.withAlpha((int)(255 * ageProgress));

            Render3DUtil.QUAD.add(new Render3DUtil.Quad(
                new Vec3d(pos.x - size, pos.y, pos.z - size),
                new Vec3d(pos.x + size, pos.y, pos.z - size),
                new Vec3d(pos.x + size, pos.y, pos.z + size),
                new Vec3d(pos.x - size, pos.y, pos.z + size),
                renderColor.getRGB()
            ));
        }
    }
}
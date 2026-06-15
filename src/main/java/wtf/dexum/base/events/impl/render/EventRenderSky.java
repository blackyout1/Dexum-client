package wtf.dexum.base.events.impl.render;

import com.darkmagician6.eventapi.events.Event;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class EventRenderSky implements Event {
    private final MatrixStack matrices;
    private final Matrix4f projectionMatrix;
    private final float tickDelta;

    public EventRenderSky(MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta) {
        this.matrices = matrices;
        this.projectionMatrix = projectionMatrix;
        this.tickDelta = tickDelta;
    }

    public MatrixStack getMatrices() {
        return matrices;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public float getTickDelta() {
        return tickDelta;
    }
}
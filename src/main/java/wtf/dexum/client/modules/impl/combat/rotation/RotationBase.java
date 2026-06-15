package wtf.dexum.client.modules.impl.combat.rotation;

import wtf.dexum.utility.game.player.rotation.Rotation;
import wtf.dexum.utility.interfaces.IMinecraft;

import java.security.SecureRandom;

public abstract class RotationBase implements IMinecraft {
    protected final SecureRandom rng = new SecureRandom();
    protected float lastYaw;
    protected float lastPitch;
    protected boolean noCircling;

    public abstract void update(Rotation targetAngle, boolean elytraVisual);

    public float getYaw() {
        return lastYaw;
    }

    public float getPitch() {
        return lastPitch;
    }

    public void setYaw(float yaw) {
        this.lastYaw = yaw;
    }

    public void setPitch(float pitch) {
        this.lastPitch = pitch;
    }

    public void setNoCircling(boolean noCircling) {
        this.noCircling = noCircling;
    }
}
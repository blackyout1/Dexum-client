package wtf.dexum.utility.render.shaders;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.BufferBuilder;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import wtf.dexum.utility.interfaces.IMinecraft;

import java.nio.FloatBuffer;

public abstract class Shader implements IMinecraft {
    private int program;

    public Shader() {
        int vertexShader = compileShader(GL20.GL_VERTEX_SHADER, getVertexCode());
        int fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, getCode());

        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);

        GL20.glBindAttribLocation(program, 0, "Position");

        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {

        }
    }

    private int compileShader(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {

        }
        return shader;
    }

    public void start() {
        GL20.glUseProgram(program);
    }

    public void finish() {
        GL20.glUseProgram(0);
    }

    public void drawQuads() {
        int vao = GL30.glGenVertexArrays();
        int vbo = GL15.glGenBuffers();

        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(8);
        vertexBuffer.put(new float[]{-1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f}).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0);

        GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);

        GL20.glDisableVertexAttribArray(0);
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        GL15.glDeleteBuffers(vbo);
        GL30.glDeleteVertexArrays(vao);
    }

    public int getUniform(String name) {
        return GL20.glGetUniformLocation(program, name);
    }

    public void setFloat(String name, float... values) {
        int location = getUniform(name);
        if (location == -1) return;
        switch (values.length) {
            case 1 -> GL20.glUniform1f(location, values[0]);
            case 2 -> GL20.glUniform2f(location, values[0], values[1]);
            case 3 -> GL20.glUniform3f(location, values[0], values[1], values[2]);
            case 4 -> GL20.glUniform4f(location, values[0], values[1], values[2], values[3]);
        }
    }

    public abstract String getCode();

    public String getVertexCode() {
        return """
                #version 150
                in vec2 Position;
                out vec2 texCoord;
                out vec3 rawPos;

                void main() {
                    texCoord = Position * 0.5 + 0.5;
                    rawPos = vec3(Position, 1.0);
                    gl_Position = vec4(Position, 0.0, 1.0);
                }
                """;
    }

    public boolean isValid() {
        return program != 0;
    }
}
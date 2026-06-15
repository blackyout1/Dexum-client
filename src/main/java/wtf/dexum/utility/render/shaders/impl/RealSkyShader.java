package wtf.dexum.utility.render.shaders.impl;

import wtf.dexum.utility.render.shaders.Shader;

public class RealSkyShader extends Shader {

    private static RealSkyShader INSTANCE;

    public static RealSkyShader getInstance() {
        if (INSTANCE == null) INSTANCE = new RealSkyShader();
        return INSTANCE;
    }

    @Override
    public String getCode() {
        return """
                #version 120
                uniform float time;
                uniform float aspect;
                uniform float fovTan;
                uniform vec3 look;
                uniform vec3 up;
                uniform vec3 left;
                uniform vec3 sunDir;
                uniform float cloudiness;
                uniform float haze;

                #define PI 3.1415926535

                float hash(vec2 p) {
                    p = fract(p * vec2(123.34, 456.21));
                    p += dot(p, p + 45.32);
                    return fract(p.x * p.y);
                }

                float noise(vec2 p) {
                    vec2 i = floor(p);
                    vec2 f = fract(p);
                    float a = hash(i);
                    float b = hash(i + vec2(1.0, 0.0));
                    float c = hash(i + vec2(0.0, 1.0));
                    float d = hash(i + vec2(1.0, 1.0));
                    vec2 u = f * f * (3.0 - 2.0 * f);
                    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
                }

                float fbm(vec2 p) {
                    float v = 0.0;
                    float a = 0.5;
                    for (int i = 0; i < 5; i++) {
                        v += a * noise(p);
                        p *= 2.0;
                        a *= 0.5;
                    }
                    return v;
                }

                void main() {
                    vec2 screen = gl_TexCoord[0].st;
                    vec2 ndc = screen * 2.0 - 1.0;
                    vec3 dir = normalize(look - left * ndc.x * aspect * fovTan + up * ndc.y * fovTan);

                    float sunLimit = 0.9995;
                    float sunIntensity = 10.0;
                    float distToSun = dot(dir, normalize(sunDir));

                    vec3 skyColor = mix(vec3(0.3, 0.6, 1.0), vec3(0.05, 0.1, 0.2), clamp(dir.y, 0.0, 1.0));

                    float scatter = pow(max(0.0, distToSun), 8.0) * haze;
                    skyColor += vec3(1.0, 0.9, 0.7) * scatter;

                    if (distToSun > sunLimit) {
                        float sun = smoothstep(sunLimit, sunLimit + 0.0001, distToSun);
                        skyColor = mix(skyColor, vec3(1.0, 1.0, 0.8) * sunIntensity, sun);
                    }

                    if (dir.y > 0.0) {
                        vec2 cloudPos = dir.xz / dir.y + time * 0.05;
                        float cloud = fbm(cloudPos * 0.5);
                        cloud = smoothstep(1.0 - cloudiness, 1.1 - cloudiness, cloud);
                        skyColor = mix(skyColor, vec3(0.95), cloud * dir.y);
                    }

                    gl_FragColor = vec4(skyColor, 1.0);
                }
                """;
    }
}
#version 150

#moj_import <dexum:common.glsl>

in vec2 FragCoord;
in vec4 FragColor;

uniform vec2 Size;
uniform vec4 Radius;
uniform float Smoothness;
uniform float InnerShadowDepth;

out vec4 fragColor;

void main() {
    vec2 center = Size * 0.5;
    vec2 fragPos = center - (FragCoord * Size);
    float dist = rdist(fragPos, center - 1.0, Radius);
    
    // Основная альфа для формы
    float alpha = 1.0 - smoothstep(1.0 - Smoothness, 1.0, dist);
    
    if (alpha < 0.01) {
        discard;
    }
    
    // Внутренняя тень: затемняем пиксели близкие к краю ВНУТРИ формы
    // dist < 0 внутри формы, dist > 0 снаружи
    // Нужно затемнить когда dist близок к 0 изнутри (от -InnerShadowDepth до 0)
    float shadowIntensity = smoothstep(-InnerShadowDepth, 0.0, dist);
    
    // Затемняем на 15% у края (shadowIntensity=0 у края, =1 в центре)
    vec3 shadowColor = FragColor.rgb * 0.85;
    vec3 finalColor = mix(shadowColor, FragColor.rgb, shadowIntensity);
    
    fragColor = vec4(finalColor, FragColor.a * alpha);
}

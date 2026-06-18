#version 150

uniform sampler2D DiffuseSampler;

uniform float time;
uniform float Blend;
uniform float TintStrength;
uniform float DesaturationStrength;
uniform float PulseStrength;

in vec2 texCoord;
out vec4 fragColor;

float luma(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    vec4 original = texture(DiffuseSampler, texCoord);

    float pulse = 0.5 + 0.5 * sin(time * 4.2 + texCoord.x * 0.16 + texCoord.y * 0.08);
    float tint = TintStrength * (0.96 + pulse * PulseStrength);
    float originalLuma = luma(original.rgb);
    vec3 color = original.rgb;
    color.r = min(1.0, color.r + tint * (0.22 + originalLuma * 0.18));
    color.g = min(1.0, color.g + tint * 0.025);
    color.b = min(1.0, color.b + tint * 0.012);

    float liftedLuma = luma(color);
    if (liftedLuma < originalLuma) {
        color += vec3(originalLuma - liftedLuma);
    }

    vec3 finalColor = mix(original.rgb, color, Blend);

    fragColor = vec4(clamp(finalColor, 0.0, 1.0), original.a);
}

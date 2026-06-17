#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

void main(){
    vec4 center = texture(DiffuseSampler, texCoord);
    vec4 left = texture(DiffuseSampler, texCoord - vec2(oneTexel.x * 4.0, 0.0));
    vec4 right = texture(DiffuseSampler, texCoord + vec2(oneTexel.x * 4.0, 0.0));
    vec4 up = texture(DiffuseSampler, texCoord - vec2(0.0, oneTexel.y * 4.0));
    vec4 down = texture(DiffuseSampler, texCoord + vec2(0.0, oneTexel.y * 4.0));
    vec4 nearLeft = texture(DiffuseSampler, texCoord - vec2(oneTexel.x * 2.0, 0.0));
    vec4 nearRight = texture(DiffuseSampler, texCoord + vec2(oneTexel.x * 2.0, 0.0));
    vec4 nearUp = texture(DiffuseSampler, texCoord - vec2(0.0, oneTexel.y * 2.0));
    vec4 nearDown = texture(DiffuseSampler, texCoord + vec2(0.0, oneTexel.y * 2.0));
    float leftDiff = max(abs(center.a - left.a), abs(center.a - nearLeft.a));
    float rightDiff = max(abs(center.a - right.a), abs(center.a - nearRight.a));
    float upDiff = max(abs(center.a - up.a), abs(center.a - nearUp.a));
    float downDiff = max(abs(center.a - down.a), abs(center.a - nearDown.a));
    float total = clamp(leftDiff + rightDiff + upDiff + downDiff, 0.0, 1.0);
    vec3 outColor = center.rgb * center.a
            + left.rgb * left.a
            + right.rgb * right.a
            + up.rgb * up.a
            + down.rgb * down.a
            + nearLeft.rgb * nearLeft.a
            + nearRight.rgb * nearRight.a
            + nearUp.rgb * nearUp.a
            + nearDown.rgb * nearDown.a;
    fragColor = vec4(outColor * 0.11111111, total);
}

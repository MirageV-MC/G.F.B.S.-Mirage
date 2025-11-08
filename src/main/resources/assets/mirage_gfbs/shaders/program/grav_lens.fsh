#version 150

in vec2 texcoord;
out vec4 fragColor;

uniform sampler2D DiffuseSampler; // minecraft:main
uniform vec2 InSize;              // 屏幕尺寸

const float EVENT_HORIZON_RADIUS = 0.20;
const float LENS_STRENGTH        = 0.30;
const float HARD_EDGE_STRENGTH   = 0.95;

void main() {
    vec2 uv = texcoord;
    vec2 center = vec2(0.5, 0.5);

    vec2 offset = uv - center;
    float aspect = InSize.x / InSize.y;
    offset.x *= aspect;
    float r = length(offset);

    vec4 color = texture(DiffuseSampler, uv);

    if (r < EVENT_HORIZON_RADIUS) {
        float nr = r / EVENT_HORIZON_RADIUS;
        float distortion = LENS_STRENGTH * (1.0 - nr);
        distortion = pow(distortion, 1.5);

        vec2 dir = normalize(offset);
        vec2 tangent = vec2(-dir.y, dir.x);
        vec2 displacedOffset = offset + tangent * distortion;
        displacedOffset.x /= aspect;

        vec2 newUV = center + displacedOffset;
        vec4 lensedColor = texture(DiffuseSampler, newUV);

        float mixFactor = smoothstep(0.0, HARD_EDGE_STRENGTH, 1.0 - nr);
        color = mix(color, lensedColor, mixFactor);

        float darkness = smoothstep(0.0, 0.15, nr);
        color.rgb *= darkness;
    }

    fragColor = color;
}

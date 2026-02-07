#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform mat4 InvViewProj;

uniform vec2 InSize;

uniform float Enable;

uniform vec3 Color;
uniform vec3 LampPos;
uniform vec3 LampDir;
uniform vec3 CameraPos;

uniform float AngleCos;
uniform float MaxDist;
uniform float Intensity;
uniform float Softness;

uniform float SpecularStrength;
uniform float Shininess;

in vec2 texCoord;

out vec4 fragColor;

vec3 reconstructWorldPos(vec2 uv, float depth01) {
    float z = depth01 * 2.0 - 1.0;
    vec4 ndc = vec4(uv * 2.0 - 1.0, z, 1.0);
    vec4 world = InvViewProj * ndc;
    return world.xyz / max(world.w, 1e-6);
}

void main() {
    vec4 base = texture(DiffuseSampler, texCoord);

    if (Enable < 0.5) {
        fragColor = base;
        return;
    }

    float d = texture(DepthSampler, texCoord).r;
    if (d >= 1.0) {
        fragColor = base;
        return;
    }

    vec3 worldPos = reconstructWorldPos(texCoord, d);

    vec3 vFromLamp = worldPos - LampPos;
    float dist = length(vFromLamp);
    if (dist <= 0.0001 || dist > MaxDist) {
        fragColor = base;
        return;
    }

    vec3 dirN = normalize(LampDir);
    vec3 vN = vFromLamp / dist;

    float c = dot(vN, dirN);
    if (c < AngleCos) {
        fragColor = base;
        return;
    }

    float edge0 = AngleCos;
    float edge1 = min(1.0, AngleCos + max(0.0001, Softness));
    float cone = smoothstep(edge0, edge1, c);
    float atten = max(0.0, 1.0 - dist / MaxDist);
    float strength = cone * atten * Intensity;

    vec3 lightRgb = Color * strength;

    vec3 lit = base.rgb * (1.0 + strength * 2.0);
    vec3 outRgb = lit + lightRgb;

    outRgb = outRgb / (outRgb + vec3(1.0));

    fragColor = vec4(outRgb, base.a);
}

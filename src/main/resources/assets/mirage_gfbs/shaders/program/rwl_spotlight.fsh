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

// 用深度邻域重建屏幕空间法线（世界空间）
vec3 reconstructNormal(vec2 uv) {
    vec2 px = vec2(1.0 / InSize.x, 0.0);
    vec2 py = vec2(0.0, 1.0 / InSize.y);

    float dC = texture(DepthSampler, uv).r;
    float dX = texture(DepthSampler, uv + px).r;
    float dY = texture(DepthSampler, uv + py).r;

    // 避免天空/无几何
    if (dC >= 1.0 || dX >= 1.0 || dY >= 1.0) return vec3(0.0, 1.0, 0.0);

    vec3 pC = reconstructWorldPos(uv, dC);
    vec3 pX = reconstructWorldPos(uv + px, dX);
    vec3 pY = reconstructWorldPos(uv + py, dY);

    vec3 dx = pX - pC;
    vec3 dy = pY - pC;

    vec3 n = normalize(cross(dx, dy));
    // cross 的方向不固定，翻到面向相机一侧更稳定
    vec3 v = normalize(CameraPos - pC);
    if (dot(n, v) < 0.0) n = -n;
    return n;
}

float smoothCone(float cosTheta) {
    float edge0 = AngleCos;
    float edge1 = min(1.0, AngleCos + max(0.0001, Softness));
    return smoothstep(edge0, edge1, cosTheta);
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

    // 聚光锥测试（发射方向与点方向的夹角）
    float c = dot(vN, dirN);
    if (c < AngleCos) {
        fragColor = base;
        return;
    }

    float cone = smoothCone(c);
    float atten = max(0.0, 1.0 - dist / MaxDist);
    float strength = cone * atten * Intensity;

    // ===== Roblox 风格：漫反射 + 高光 =====
    vec3 N = reconstructNormal(texCoord);

    // 光线方向：从点指向灯（注意与 vFromLamp 相反）
    vec3 L = normalize(LampPos - worldPos);
    float NdotL = max(dot(N, L), 0.0);

    // 视线方向
    vec3 V = normalize(CameraPos - worldPos);
    // 半角向量（Blinn-Phong）
    vec3 H = normalize(L + V);
    float NdotH = max(dot(N, H), 0.0);

    float diffuse = NdotL;
    float spec = pow(NdotH, Shininess) * SpecularStrength;

    vec3 lightRgb = Color * (diffuse + spec) * strength;

    // “真正像照明”：
    // 1) 先提升亮度（让表面变亮）
    vec3 lit = base.rgb * (1.0 + strength * 1.8);

    // 2) 再叠加彩色灯光（可明显染色）
    vec3 outRgb = lit + lightRgb;

    // 轻度 tone map，防止炸白
    outRgb = outRgb / (outRgb + vec3(1.0));

    fragColor = vec4(outRgb, base.a);
}

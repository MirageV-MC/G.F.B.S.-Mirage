#version 150

in vec4 vColor;
in vec2 vParam;
in vec3 vViewPos;

uniform vec4 ColorModulator;
uniform float GameTime;

uniform float EdgeSoft0;
uniform float EdgeSoft1;
uniform float LongPow;
uniform float FogDensity;
uniform float CoreU;
uniform float CoreBoost;
uniform float NoiseScale;
uniform float NoiseSpeed;
uniform float NoiseAmount;
uniform float ViewFadeDist;

out vec4 fragColor;

void main() {
    // 颜色：来自 BlockEntity 顶点色 * ColorModulator
    vec3 baseColor = vColor.rgb * ColorModulator.rgb;

    // 0近端 -> 1远端
    float t = clamp(vParam.y, 0.0, 1.0);

    // 关键：沿长度渐隐，保证 t=1 时严格为 0
    // LongPow=1：线性；>1：更“灯头集中”；<1：更均匀
    float fade = pow(1.0 - t, max(0.0001, LongPow));

    // additive（SRC_ALPHA, ONE）下：最终贡献是 src.rgb * src.a
    // 所以：rgb 不要再乘 fade，否则会变成 fade^2
    float alpha = vColor.a * ColorModulator.a * fade;

    // 亮度倍率：复用 FogDensity（你 json 里默认 1.55，就当强度）
    vec3 rgb = baseColor * max(0.0, FogDensity);

    fragColor = vec4(rgb, clamp(alpha, 0.0, 1.0));
}

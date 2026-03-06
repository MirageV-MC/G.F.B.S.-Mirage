#version 400

uniform sampler2D DepthSampler;
uniform sampler2D TextureSampler;
uniform sampler2D RenderTargetSampler;

uniform vec2 screenSize;
uniform mat4 projectionMatrix;
uniform mat4 modelViewMatrix;
uniform vec3 cameraPos;
uniform vec3 entityPos;
uniform float time;
uniform float scale;
uniform float eventHorizonScale;
uniform float accretionDiskOpacity;
uniform float diskInnerExpansion;
uniform float diskOuterExpansion;
uniform int isDespawning;

in vec2 texCoord;
out vec4 fragColor;

const float BLACKHOLE_SIZE = 2;
const float DISK_SCALE = 1.5;
const float BLACKHOLE_CORE_SIZE = 0.11;
const float ACCRETION_DISK_WIDTH_INNER = 0.03;
const float ACCRETION_DISK_WIDTH_OUTER = 0.02;
const float ACCRETION_DISK_INNER = 0.13 * DISK_SCALE;
const float ACCRETION_DISK_OUTER = 1.0 * DISK_SCALE;
const float THICKNESS_FALLOFF_POWER = 2.0;

const float NOISE_THICKNESS_MIN = 0.2;
const float NOISE_THICKNESS_MAX = 1.3;
const float NOISE_CONTRAST = 1.2;
const float NOISE_CUTOFF = 0.02;

const float RAMP_POS1 = 0.2;
const float RAMP_POS2 = 0.65;
const float RAMP_POS3 = 1;
const vec3 RAMP_COL1 = vec3(0.963, 0.783, 0.677);
const vec3 RAMP_COL2 = vec3(0.3, 0.15, 0.09);
const vec3 RAMP_COL3 = vec3(0.0, 0.0, 0.0);

const float RAMP_EMISSION = 1.25;
const vec3 EMISSION_BIAS = vec3(0.00);

const float ENV_INTENSITY = 0.9;
const float STEP_SIZE = 0.02;
const float NOISE_FACTOR = 0.5;
const float POWER = 0.024;
const int ITER_COUNT = 128;

#define PI 3.141592
#define TAU 6.283185

float remapClamp(float x, float a, float b, float c, float d)
{
    float t = clamp((x - a) / max(b - a, 1e-6), 0.0, 1.0);
    return mix(c, d, t);
}

float lengthSqrt(vec3 v)
{
    return sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
}

mat2 Rot(float a)
{
    float s = sin(a), c = cos(a);
    return mat2(c, -s, s, c);
}

mat3 rotateAxis(vec3 axis, float angle)
{
    axis = normalize(axis);
    float s = sin(angle);
    float c = cos(angle);
    float oc = 1.0 - c;

    return mat3(
    oc * axis.x * axis.x + c, oc * axis.x * axis.y - axis.z * s, oc * axis.z * axis.x + axis.y * s,
    oc * axis.x * axis.y + axis.z * s, oc * axis.y * axis.y + c, oc * axis.y * axis.z - axis.x * s,
    oc * axis.z * axis.x - axis.y * s, oc * axis.y * axis.z + axis.x * s, oc * axis.z * axis.z + c);
}

vec3 sampleDeepNoise(vec2 uv)
{
    return texture(TextureSampler, uv).rgb;
}

vec3 CatmulRom(in float T, vec3 D, vec3 C, vec3 B, vec3 A)
{
    return 0.5 * ((2.0 * B) + (-A + C) * T + (2.0 * A - 5.0 * B + 4.0 * C - D) * T * T + (-A + 3.0 * B - 3.0 * C + D) * T * T * T);
}

vec3 ColorRamp_BSpline(float T, vec4 A, vec4 B, vec4 C)
{
    float AB = B.w - A.w;
    float BC = C.w - B.w;

    float iAB = clamp((T - A.w) / AB, 0.0, 1.0);
    float iBC = clamp((T - B.w) / BC, 0.0, 1.0);

    vec3 p = vec3(1.0 - iAB, iAB - iBC, iBC);

    vec3 cA = CatmulRom(p.x, A.xyz, A.xyz, B.xyz, C.xyz);
    vec3 cB = CatmulRom(p.y, A.xyz, B.xyz, C.xyz, C.xyz);
    vec3 cC = C.xyz;

    if(T < B.w)return cA;
    if(T < C.w)return cB;

    return cC;
}

float smoothRange(float value, float inMin, float inMax, float outMin, float outMax)
{
    float t = clamp((value - inMin) / (inMax - inMin), 0.0, 1.0);
    float smoothT = t * t * (3.0 - 2.0 * t);
    return mix(outMin, outMax, smoothT);
}

float whiteNoise2D(vec2 uv)
{
    return fract(sin(dot(uv, vec2(12.9898, 78.233))) * 43758.5453123);
}

vec3 sampleEnvCube(vec3 dir)
{
    vec2 envUV = vec2(
    0.5 + atan(dir.z, dir.x) / (2.0 * PI),
    0.5 - asin(clamp(dir.y, -1.0, 1.0)) / PI
    );
    return texture(RenderTargetSampler, envUV).rgb;
}

vec3 clipToView(vec2 uv, float depth)
{
    vec4 clipPos = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 viewPos = inverse(projectionMatrix) * clipPos;
    return viewPos.xyz / viewPos.w;
}

vec3 viewToWorld(vec3 viewPos)
{
    vec4 worldPos = inverse(modelViewMatrix) * vec4(viewPos, 1.0);
    return worldPos.xyz + cameraPos;
}

float worldToDepth(vec3 worldPos)
{
    vec4 viewPos = modelViewMatrix * vec4(worldPos - cameraPos, 1.0);
    vec4 clipPos = projectionMatrix * viewPos;
    float ndcDepth = clipPos.z / clipPos.w;
    return ndcDepth * 0.5 + 0.5;
}

vec2 raySphereIntersect(vec3 ro, vec3 rd, vec3 center, float radius)
{
    vec3 oc = ro - center;
    float b = dot(oc, rd);
    float c = dot(oc, oc) - radius * radius;
    float h = b * b - c;

    if(h < 0.0) return vec2(-1.0);

    float sqrtH = sqrt(h);
    float t1 = -b - sqrtH;
    float t2 = -b + sqrtH;

    if(t1 < 0.0) t1 = 0.0;

    return vec2(t1, t2);
}

struct RenderResult
{
    vec3 color;
    float alpha;
    float hitDistance;
};

RenderResult renderBlackhole(vec3 localRo, vec3 localRd, vec2 screenUV, float ehScale, float innerExp, float outerExp, bool despawning)
{
    vec3 rayPos = localRo.xzy;
    vec3 rayDir = localRd.xzy;

    float noiseAmp = 0.03;
    float noiseWhite = whiteNoise2D(screenUV) * noiseAmp;
    vec3 jitter = rayDir * noiseWhite;
    rayPos = rayPos - jitter;

    vec4 rampA = vec4(RAMP_COL1, RAMP_POS1);
    vec4 rampB = vec4(RAMP_COL2, RAMP_POS2);
    vec4 rampC = vec4(RAMP_COL3, RAMP_POS3);

    vec3 col = vec3(0.0);
    float alpha = 0.0;
    float hitDistance = -1.0;
    float traveledDistance = 0.0;

    float coreSize = BLACKHOLE_CORE_SIZE * ehScale;
    float diskInner = ACCRETION_DISK_INNER * innerExp;
    float diskOuter = ACCRETION_DISK_OUTER * outerExp;

    for(int i = 0; i < ITER_COUNT; ++i)
    {
        vec3 rNorm = normalize(rayPos);
        float rLen = length(rayPos);
        float steerMag = (STEP_SIZE * POWER) / (rLen * rLen);
        float range = despawning ? 0.0 : remapClamp(rLen, -100.0, 0.5, 0.0, 4.0);
        vec3 steer = rNorm * (steerMag * range);
        vec3 steeredDir = normalize(rayDir - steer);

        vec3 advance = rayDir * STEP_SIZE;
        rayPos += advance;
        traveledDistance += STEP_SIZE;

        float xyLen = lengthSqrt(rayPos * vec3(1, 1, 0));
        float rLenNow = length(rayPos);

        float insideCore = despawning ? 0.0 : step(rLenNow, coreSize);

        if(insideCore < 0.5 && xyLen < diskInner)
        {
            rayDir = steeredDir;
            continue;
        }

        float radialNorm = clamp((xyLen - diskInner) / max(diskOuter - diskInner, 0.001), 0.0, 1.0);
        float thicknessFactor = 1.0 - pow(radialNorm, THICKNESS_FALLOFF_POWER);
        float dynamicWidth = mix(ACCRETION_DISK_WIDTH_OUTER, ACCRETION_DISK_WIDTH_INNER, thicknessFactor);

        float rotPhase = (xyLen * 4.270) - (time * 0.2);
        if(despawning) {
            rotPhase += time * 0.5;
        }
        vec3 uvAxis = vec3(0, 0, 1);
        vec3 uvRot = rayPos * (rotateAxis(uvAxis, rotPhase));
        vec2 uv = uvRot.xy * 2.0;

        vec3 noiseDeep = sampleDeepNoise(uv * 0.5);
        float noiseLuminance = dot(noiseDeep, vec3(0.299, 0.587, 0.114));

        if(noiseLuminance < NOISE_CUTOFF)
        {
            rayDir = steeredDir;
            continue;
        }

        noiseLuminance = pow(noiseLuminance, NOISE_CONTRAST);

        float noiseThicknessMod = mix(NOISE_THICKNESS_MIN, NOISE_THICKNESS_MAX, noiseLuminance);
        float modulatedWidth = dynamicWidth * noiseThicknessMod;
        float zAbs = abs(rayPos.z);
        float thicknessFalloff = smoothstep(modulatedWidth, 0.0, zAbs);
        float rampInput = xyLen;

        vec3 baseCol = ColorRamp_BSpline(rampInput, rampA, rampB, rampC);
        vec3 emissiveCol = (baseCol * 2.0) + vec3(0.14, 0.129, 0.09) * 0.4;

        vec3 shadedCol = mix(emissiveCol, vec3(0.0), insideCore);

        float aRadial = smoothRange(xyLen, diskOuter, diskInner, 0.0, 1.0);
        float alphaLocal = thicknessFalloff * aRadial * accretionDiskOpacity;
        alphaLocal = mix(alphaLocal, 1.0, insideCore);

        if(hitDistance < 0.0 && alphaLocal > 0.01) hitDistance = traveledDistance;

        float oneMinusA = 1.0 - alpha;
        float weight = oneMinusA * alphaLocal;
        vec3 newColor = mix(col, shadedCol, weight);
        float newAlpha = mix(alpha, 1.0, alphaLocal);

        rayDir = steeredDir;
        alpha = newAlpha;
        col = newColor;

        if(alpha > 0.99) break;
    }

    RenderResult result;
    result.color = col;
    result.alpha = alpha;
    result.hitDistance = hitDistance;
    return result;
}

void main()
{
    float sceneDepth = texture(DepthSampler, texCoord).r;

    vec3 viewPos = clipToView(texCoord, 0.0);
    vec3 rayOrigin = cameraPos;
    vec3 rayEnd = viewToWorld(viewPos);
    vec3 rayDir = normalize(rayEnd - rayOrigin);

    float sceneDistance = 1000.0;
    if(sceneDepth < 1.0)
    {
        vec3 sceneWorldPos = viewToWorld(clipToView(texCoord, sceneDepth));
        sceneDistance = distance(rayOrigin, sceneWorldPos);
    }

    float effectiveScale = scale;
    float ehScale = eventHorizonScale;
    float innerExp = diskInnerExpansion;
    float outerExp = diskOuterExpansion;
    bool despawning = isDespawning == 1;

    float renderRadius = effectiveScale * outerExp;
    vec2 sphereT = raySphereIntersect(rayOrigin, rayDir, entityPos, renderRadius);

    if(sphereT.y < 0.0 || sphereT.x >= sceneDistance) discard;

    vec3 marchStart;
    if(sphereT.x <= 0.0) marchStart = rayOrigin;
    else marchStart = rayOrigin + rayDir * sphereT.x;

    vec3 localRo = (marchStart - entityPos) / max(effectiveScale, 0.001);
    vec3 localRd = rayDir;

    RenderResult result = renderBlackhole(localRo, localRd, texCoord, ehScale, innerExp, outerExp, despawning);

    if(result.alpha > 0.01 && result.hitDistance > 0.0)
    {
        vec3 hitPos = marchStart + localRd * result.hitDistance * effectiveScale;
        float objectDepth = worldToDepth(hitPos);

        if(objectDepth >= sceneDepth) discard;

        gl_FragDepth = objectDepth;
        fragColor = vec4(result.color, result.alpha);
    }
    else discard;
}

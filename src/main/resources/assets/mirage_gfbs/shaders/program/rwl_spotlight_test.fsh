#version 150

uniform sampler2D DiffuseSampler;

uniform float Spot1_Enable;
uniform vec3 Spot1_Color;
uniform vec3 Spot1_Pos;
uniform vec3 Spot1_Dir;
uniform float Spot1_AngleCos;
uniform float Spot1_MaxDist;
uniform float Spot1_Intensity;
uniform float Spot1_Softness;

uniform float Spot2_Enable;
uniform vec3 Spot2_Color;
uniform vec3 Spot2_Pos;
uniform vec3 Spot2_Dir;
uniform float Spot2_AngleCos;
uniform float Spot2_MaxDist;
uniform float Spot2_Intensity;
uniform float Spot2_Softness;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 base = texture(DiffuseSampler, texCoord);
    
    vec3 finalColor = base.rgb;
    
    if (Spot1_Enable > 0.5) {
        vec2 screenPos = texCoord * 2.0 - 1.0;
        vec3 toLight = Spot1_Pos;
        float dist = length(toLight);
        
        if (dist < Spot1_MaxDist) {
            vec3 L = normalize(toLight);
            float spotCos = dot(-L, normalize(Spot1_Dir));
            
            float spotFactor = smoothstep(Spot1_AngleCos, Spot1_AngleCos + Spot1_Softness, spotCos);
            float attenuation = 1.0 - smoothstep(Spot1_MaxDist * 0.5, Spot1_MaxDist, dist);
            
            float light = spotFactor * attenuation * Spot1_Intensity;
            finalColor += Spot1_Color * light;
        }
    }
    
    if (Spot2_Enable > 0.5) {
        vec2 screenPos = texCoord * 2.0 - 1.0;
        vec3 toLight = Spot2_Pos;
        float dist = length(toLight);
        
        if (dist < Spot2_MaxDist) {
            vec3 L = normalize(toLight);
            float spotCos = dot(-L, normalize(Spot2_Dir));
            
            float spotFactor = smoothstep(Spot2_AngleCos, Spot2_AngleCos + Spot2_Softness, spotCos);
            float attenuation = 1.0 - smoothstep(Spot2_MaxDist * 0.5, Spot2_MaxDist, dist);
            
            float light = spotFactor * attenuation * Spot2_Intensity;
            finalColor += Spot2_Color * light;
        }
    }
    
    fragColor = vec4(finalColor, base.a);
}

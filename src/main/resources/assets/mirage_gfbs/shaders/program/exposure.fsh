#version 150

uniform sampler2D DiffuseSampler;
uniform float Exposure;

in vec2 texCoord;
out vec4 FragColor;

void main() {
    vec4 c = texture(DiffuseSampler, texCoord);

    float e = max(0.0, Exposure);
    vec3 outRgb = vec3(1.0) - exp(-c.rgb * (1.0 + e));

    FragColor = vec4(outRgb, c.a);
}

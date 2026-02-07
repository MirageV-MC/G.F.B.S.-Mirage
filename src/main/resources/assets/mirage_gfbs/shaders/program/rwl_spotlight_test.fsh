#version 150

uniform sampler2D DiffuseSampler;
uniform float Enable;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 base = texture(DiffuseSampler, texCoord);

    if (Enable > 0.5) {
        fragColor = vec4(1.0, 0.0, 0.0, 1.0);
    } else {
        fragColor = base;
    }
}

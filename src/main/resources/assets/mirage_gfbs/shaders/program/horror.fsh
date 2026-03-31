#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 FragColor;

void main() {
    FragColor = texture(DiffuseSampler, texCoord);
}

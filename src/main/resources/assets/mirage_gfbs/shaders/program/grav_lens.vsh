#version 150

in vec2 Position;
out vec2 texcoord;

void main() {
    gl_Position = vec4(Position, 0.0, 1.0);
    texcoord = (Position + 1.0) * 0.5;
}

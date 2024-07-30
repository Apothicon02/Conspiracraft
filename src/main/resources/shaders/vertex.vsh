#version 400 core

in vec3 position;
in vec2 texCoord;

out vec3 color;
out vec2 fragTexCoord;

uniform mat4 transformationMatrix;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;

void main() {
    fragTexCoord = texCoord;
    //gl_Position = projectionMatrix * viewMatrix * transformationMatrix * vec4(position, 1.0);
    gl_Position = projectionMatrix * viewMatrix * transformationMatrix * vec4(texCoord, 0.0, 1.0);
}
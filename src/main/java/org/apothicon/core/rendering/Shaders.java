package org.apothicon.core.rendering;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;
import org.lwjgl.system.MemoryStack;

import java.util.HashMap;
import java.util.Map;

public class Shaders {
    private final int programID;
    private int vertexShaderID, fragmentShaderID;

    private final Map<String, Integer> uniforms;

    public Shaders() throws Exception {
        programID = GL40.glCreateProgram();
        if (programID == 0) {
            throw new Exception("Could not create shader");
        }

        uniforms = new HashMap<>();
    }

    public void createUniforms(String uniformName) throws Exception {
        int uniformLocation = GL40.glGetUniformLocation(programID, uniformName);
        if (uniformLocation < 0) {
            throw new Exception("Could not find uniform" + uniformName);
        }
        uniforms.put(uniformName, uniformLocation);
    }

    public void setUniform(String uniformName, Matrix4f value) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            GL40.glUniformMatrix4fv(uniforms.get(uniformName), false,
                    value.get(stack.mallocFloat(16)));
        }
    }

    public void setUniform(String uniformName, Vector4f value) {
        GL30.glUniform4f(uniforms.get(uniformName), value.x, value.y, value.z, value.w);
    }

    public void setUniform(String uniformName, Vector3f value) {
        GL30.glUniform3f(uniforms.get(uniformName), value.x, value.y, value.z);
    }

    public void setUniform(String uniformName, boolean value) {
        GL30.glUniform1f(uniforms.get(uniformName), value ? 1 : 0);
    }

    public void setUniform(String uniformName, int value) {
        GL40.glUniform1i(uniforms.get(uniformName), value);
    }

    public void setUniform(String uniformName, float value) {
        GL40.glUniform1f(uniforms.get(uniformName), value);
    }

    public void createVertexShader(String shaderCode) throws Exception {
        vertexShaderID = createShader(shaderCode, GL40.GL_VERTEX_SHADER);
    }

    public void createFragmentShader(String shaderCode) throws Exception {
        fragmentShaderID = createShader(shaderCode, GL40.GL_FRAGMENT_SHADER);
    }

    public int createShader(String shaderCode, int shaderType) throws Exception {
        int shaderID = GL40.glCreateShader(shaderType);
        if (shaderID == 0) {
            throw new Exception("Error creating shader. Type: " + shaderType);
        }

        GL40.glShaderSource(shaderID, shaderCode);
        GL40.glCompileShader(shaderID);

        if (GL40.glGetShaderi(shaderID, GL40.GL_COMPILE_STATUS) == 0) {
            throw new Exception("Error compiling shader code: TYPE: " + shaderType
                    + " Info " + GL40.glGetShaderInfoLog(shaderID, 1024));
        }

        GL40.glAttachShader(programID, shaderID);

        return shaderID;
    }

    public void link() throws Exception {
        GL40.glLinkProgram(programID);
        if (GL40.glGetProgrami(programID, GL40.GL_LINK_STATUS) == 0) {
            throw new Exception("Error linking shader code "
                    + " Info " + GL40.glGetProgramInfoLog(programID, 1024));
        }

        if (vertexShaderID != 0) {
            GL40.glDetachShader(programID, vertexShaderID);
        }

        if (fragmentShaderID != 0) {
            GL40.glDetachShader(programID, fragmentShaderID);
        }

        GL40.glValidateProgram(programID);
        if (GL40.glGetProgrami(programID, GL40.GL_VALIDATE_STATUS) == 0) {
            throw new Exception("Unable to validate shader code: " + GL40.glGetProgramInfoLog(programID, 1024));
        }
    }

    public void bind() {
        GL40.glUseProgram(programID);
    }

    public void unbind() {
        GL40.glUseProgram(0);
    }

    public void cleanup() {
        unbind();
        if (programID != 0) {
            GL40.glDeleteProgram(programID);
        }
    }
}

package org.conspiracraft.engine;

import org.lwjgl.opengl.GL43;

public class ComputeProgram {
    public final int programId;
    private int computeShaderId;

    public ComputeProgram() throws Exception {
        programId = GL43.glCreateProgram();
        if (programId == 0) {
            throw new Exception("Could not create Shader");
        }
    }

    public void createComputeShader(String shaderCode) throws Exception {
        computeShaderId = createShader(shaderCode, GL43.GL_COMPUTE_SHADER);
    }

    protected int createShader(String shaderCode, int shaderType) throws Exception {
        int shaderId = GL43.glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new Exception("Error creating shader. Type: " + shaderType);
        }

        GL43.glShaderSource(shaderId, shaderCode);
        GL43.glCompileShader(shaderId);

        if (GL43.glGetShaderi(shaderId, GL43.GL_COMPILE_STATUS) == 0) {
            throw new Exception("Error compiling Shader code: " + GL43.glGetShaderInfoLog(shaderId, 1024));
        }

        GL43.glAttachShader(programId, shaderId);

        return shaderId;
    }

    public void link() throws Exception {
        GL43.glLinkProgram(programId);
        if (GL43.glGetProgrami(programId, GL43.GL_LINK_STATUS) == 0) {
            throw new Exception("Error linking Shader code: " + GL43.glGetProgramInfoLog(programId, 1024));
        }

        if (computeShaderId != 0) {
            GL43.glDetachShader(programId, computeShaderId);
        }

        GL43.glValidateProgram(programId);
        if (GL43.glGetProgrami(programId, GL43.GL_VALIDATE_STATUS) == 0) {
            System.err.println("Warning validating Shader code: " + GL43.glGetProgramInfoLog(programId, 1024));
        }

    }

    public void bind() {
        GL43.glUseProgram(programId);
    }

    public void unbind() {
        GL43.glUseProgram(0);
    }

    public void cleanup() {
        unbind();
        if (programId != 0) {
            GL43.glDeleteProgram(programId);
        }
    }
}

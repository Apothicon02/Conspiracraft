package org.conspiracraft.engine;

import org.lwjgl.opengl.GL40;

public class ShaderProgram {
    public final int programId;
    private int vertexShaderId;
    private int fragmentShaderId;

    public ShaderProgram() throws Exception {
        programId = GL40.glCreateProgram();
        if (programId == 0) {
            throw new Exception("Could not create Shader");
        }
    }

    public void createVertexShader(String shaderCode) throws Exception {
        vertexShaderId = createShader(shaderCode, GL40.GL_VERTEX_SHADER);
    }

    public void createFragmentShader(String shaderCode) throws Exception {
        fragmentShaderId = createShader(shaderCode, GL40.GL_FRAGMENT_SHADER);
    }

    protected int createShader(String shaderCode, int shaderType) throws Exception {
        int shaderId = GL40.glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new Exception("Error creating shader. Type: " + shaderType);
        }

        GL40.glShaderSource(shaderId, shaderCode);
        GL40.glCompileShader(shaderId);

        if (GL40.glGetShaderi(shaderId, GL40.GL_COMPILE_STATUS) == 0) {
            throw new Exception("Error compiling Shader code: " + GL40.glGetShaderInfoLog(shaderId, 1024));
        }

        GL40.glAttachShader(programId, shaderId);

        return shaderId;
    }

    public void link() throws Exception {
        GL40.glLinkProgram(programId);
        if (GL40.glGetProgrami(programId, GL40.GL_LINK_STATUS) == 0) {
            throw new Exception("Error linking Shader code: " + GL40.glGetProgramInfoLog(programId, 1024));
        }

        if (vertexShaderId != 0) {
            GL40.glDetachShader(programId, vertexShaderId);
        }
        if (fragmentShaderId != 0) {
            GL40.glDetachShader(programId, fragmentShaderId);
        }

        GL40.glValidateProgram(programId);
        if (GL40.glGetProgrami(programId, GL40.GL_VALIDATE_STATUS) == 0) {
            System.err.println("Warning validating Shader code: " + GL40.glGetProgramInfoLog(programId, 1024));
        }

    }

    public void bind() {
        GL40.glUseProgram(programId);
    }

    public void unbind() {
        GL40.glUseProgram(0);
    }

    public void cleanup() {
        unbind();
        if (programId != 0) {
            GL40.glDeleteProgram(programId);
        }
    }
}

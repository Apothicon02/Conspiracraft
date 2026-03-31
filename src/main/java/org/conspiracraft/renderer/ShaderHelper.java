package org.conspiracraft.renderer;

import org.conspiracraft.Utils;
import org.lwjgl.util.shaderc.Shaderc;

import java.nio.ByteBuffer;

public class ShaderHelper {
    public static ByteBuffer compileGLSLString(String[] paths, int stage) {
        StringBuilder glsl = new StringBuilder("#version 450 \n");
        String shaderName = "";
        for (String str : paths) {
            shaderName += str;
            glsl.append(Utils.readFile("assets/base/shader/" + str));
        }
        long compiler = Shaderc.shaderc_compiler_initialize();
        if (compiler == 0) {throw new IllegalStateException("Failed to create shaderc compiler");}
        long result = Shaderc.shaderc_compile_into_spv(compiler, glsl, stage, shaderName, "main", 0);
        if (result == 0) throw new RuntimeException(shaderName + " compile returned null.");
        long status = Shaderc.shaderc_result_get_compilation_status(result);
        if (status != Shaderc.shaderc_compilation_status_success) {
            throw new RuntimeException("Shader compile error:\n" + Shaderc.shaderc_result_get_error_message(result));
        }

        ByteBuffer spirv = Shaderc.shaderc_result_get_bytes(result);
        Shaderc.shaderc_result_release(result);
        Shaderc.shaderc_compiler_release(compiler);
        return spirv;
    }
}

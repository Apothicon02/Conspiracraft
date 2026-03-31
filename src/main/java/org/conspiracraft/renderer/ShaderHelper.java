package org.conspiracraft.renderer;

import org.conspiracraft.Utils;
import org.conspiracraft.Window;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK13.*;

public class ShaderHelper {
    public static long createShaderModule(ByteBuffer buffer) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(buffer);

            LongBuffer pShaderModule = stack.mallocLong(1);

            int err = vkCreateShaderModule(Window.device, createInfo, null, pShaderModule);
            if (err != VK_SUCCESS) {
                throw new RuntimeException("Failed to create shader module: " + err);
            }

            return pShaderModule.get(0);
        }
    }

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

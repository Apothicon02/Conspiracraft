package org.conspiracraft.renderer;

import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL45.GL_ZERO_TO_ONE;
import static org.lwjgl.opengl.GL45.glClipControl;

public class OpenGL {
    public static void initGL() {
        //long glInitStarted = System.currentTimeMillis();
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_GEQUAL);
        glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE);
        glFrontFace(GL_CW);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_FRONT);
        createGLDebugger();
        //System.out.print("Took "+String.format("%.2f", (System.currentTimeMillis()-glInitStarted)/1000.f)+"s to init OpenGL.\n");
    }

    public static void createGLDebugger() {
        glEnable(GL_DEBUG_OUTPUT);
        glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
        glDebugMessageCallback((source, type, id, severity, length, message, userParam) -> {
            String msg = decode(source, type, id, severity, length, message, userParam);
            if (msg != null) {
                System.out.println(msg);
            }
        }, 0);
    }

    public static String decode(int source, int type, int id, int severity, int length, long message, long userParam) {
        if(id == 131169 || id == 131185 || id == 131218 || id == 131204) return null; //ignore useless stuff
        String sourceTxt = switch (source) {
            case GL_DEBUG_SOURCE_API -> "API";
            case GL_DEBUG_SOURCE_WINDOW_SYSTEM -> "Window";
            case GL_DEBUG_SOURCE_SHADER_COMPILER -> "Shader";
            case GL_DEBUG_SOURCE_THIRD_PARTY -> "Third Party";
            case GL_DEBUG_SOURCE_APPLICATION -> "App";
            case GL_DEBUG_SOURCE_OTHER -> "Other";
            default -> "Unknown";
        };
        String typeTxt = switch (type) {
            case GL_DEBUG_TYPE_ERROR -> "Error";
            case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> "Deprecated";
            case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> "Undefined";
            case GL_DEBUG_TYPE_PORTABILITY -> "Portability";
            case GL_DEBUG_TYPE_PERFORMANCE -> "Performance";
            case GL_DEBUG_TYPE_MARKER -> "Marker";
            case GL_DEBUG_TYPE_PUSH_GROUP -> "Push Group";
            case GL_DEBUG_TYPE_POP_GROUP -> "Pop Group";
            case GL_DEBUG_TYPE_OTHER -> "Other";
            default -> "Unknown";
        };
        String severityTxt = switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH -> "High";
            case GL_DEBUG_SEVERITY_MEDIUM -> "Medium";
            case GL_DEBUG_SEVERITY_LOW -> "Low";
            case GL_DEBUG_SEVERITY_NOTIFICATION -> "Notification";
            default -> "Unknown";
        };
        return  "Source: " + sourceTxt + ", Type: " + typeTxt + ", Id: " + id + ", Severity: " + severityTxt + ", " + MemoryUtil.memUTF8(message, length);
    }
}

package org.conspiracraft.renderer;

import org.joml.Vector3f;

public record Vertex(Vector3f pos, Vector3f normal) {
    public static final int size = 4*(3+3);
}

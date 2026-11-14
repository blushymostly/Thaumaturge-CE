package com.leclowndu93150.thaumaturge.client.screen.research.detail;

public record Rect(int x, int y, int width, int height) {
    public boolean contains(double mouseX, double mouseY) {
        return inside(x, y, width, height, mouseX, mouseY);
    }

    public static boolean inside(int x, int y, int width, int height, double mouseX, double mouseY) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }
}

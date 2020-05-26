package ru.spbau.team.vnc;

import java.awt.*;

public class Parameters implements Cloneable {

    private final int framebufferWidth;
    private final int framebufferHeight;
    private PixelFormat pixelFormat;
    private final String name;

    public Parameters(int framebufferWidth, int framebufferHeight, PixelFormat pixelFormat, String name) {
        this.framebufferWidth = framebufferWidth;
        this.framebufferHeight = framebufferHeight;
        this.pixelFormat = pixelFormat;
        this.name = name;
    }

    public static Parameters getDefaultWithName(String name) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return new Parameters(screenSize.width, screenSize.height, PixelFormat.getDefault(), name);
    }

    @Override
    public Parameters clone() {
        try {
            return (Parameters) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getFramebufferWidth() {
        return framebufferWidth;
    }

    public int getFramebufferHeight() {
        return framebufferHeight;
    }

    public PixelFormat getPixelFormat() {
        return pixelFormat;
    }

    public void setPixelFormat(PixelFormat newPixelFormat) {
        pixelFormat = newPixelFormat;
    }

    public String getName() {
        return name;
    }
}

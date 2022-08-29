package za.madtek.arcaderacer;

import za.madtek.arcaderacer.vulkan.RenderSystem;

public class Globals {

    public static final String TITLE = "Arcade Racer - Vulkan";
    public static int WIDTH = 1600;
    public static int HEIGHT = 900;

    public static final boolean ENABLE_VALIDATION_LAYERS = false;

    public static ArcadeRacer game;
    public static Window window;
    public static RenderSystem renderSystem;
}

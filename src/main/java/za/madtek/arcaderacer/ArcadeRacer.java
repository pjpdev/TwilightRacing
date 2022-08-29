package za.madtek.arcaderacer;

import za.madtek.arcaderacer.vulkan.RenderSystem;

public class ArcadeRacer {
    private Window window;
    private RenderSystem renderSystem;

    private boolean running;

    public ArcadeRacer() {
        window = new Window(Globals.WIDTH, Globals.HEIGHT, false, false, Globals.TITLE);
        renderSystem = new RenderSystem();
    }

    public void run() {
        try {
            init();
            mainLoop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            free();
        }
    }

    private void init() {
        Globals.game = this;

        window.init();
        Globals.window = window;

        renderSystem.init();
        Globals.renderSystem = renderSystem;
    }

    private void mainLoop() {
        running = true;

        while (running) {
            if (window.isResized()) {
                // TODO: process resize messages.
                Globals.WIDTH = window.getWidth();
                Globals.HEIGHT = window.getHeight();
                window.setResized(false);
            }

            window.update();

            if (window.shouldClose()) running = false;
        }
    }

    private void free() {
        renderSystem.free();
        window.free();
    }

    public static void main(String[] args) {
        new ArcadeRacer().run();
    }
}

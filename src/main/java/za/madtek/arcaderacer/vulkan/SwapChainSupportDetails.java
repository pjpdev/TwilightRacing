package za.madtek.arcaderacer.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

import java.nio.IntBuffer;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;

public class SwapChainSupportDetails {

    private VkSurfaceCapabilitiesKHR capabilities;
    private VkSurfaceFormatKHR.Buffer formats;
    private IntBuffer presentModes;

    protected void querySupport(VkPhysicalDevice physicalDevice, long surface, MemoryStack stack) {
        capabilities = VkSurfaceCapabilitiesKHR.calloc(stack);
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, capabilities);

        IntBuffer count = stack.ints(0);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, count, null);
        if (count.get(0) != 0) {
            formats = VkSurfaceFormatKHR.malloc(count.get(0), stack);
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, count, formats);
        }

        vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, count, null);
        if (count.get(0) != 0) {
            presentModes = stack.mallocInt(count.get(0));
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, count, presentModes);
        }
    }

    protected boolean isAdequate() {
        return formats.hasRemaining() && presentModes.hasRemaining();
    }

    public VkSurfaceCapabilitiesKHR getCapabilities() {
        return capabilities;
    }

    public VkSurfaceFormatKHR.Buffer getFormats() {
        return formats;
    }

    public IntBuffer getPresentModes() {
        return presentModes;
    }
}

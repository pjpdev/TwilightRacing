package za.madtek.arcaderacer.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.IntBuffer;
import java.util.stream.IntStream;

import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_TRUE;

public class QueueFamilyIndices {
    private Integer graphicsFamily;
    private Integer presentFamily;

    public boolean isComplete() {
        return graphicsFamily != null && presentFamily != null;
    }

    public int[] unique() {
        return IntStream.of(graphicsFamily, presentFamily).distinct().toArray();
    }

    public Integer getGraphicsFamily() {
        return graphicsFamily;
    }

    public Integer getPresentFamily() {
        return presentFamily;
    }

    public static QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device, long surface) {
        QueueFamilyIndices indices = new QueueFamilyIndices();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer queueFamilyCount = stack.ints(0);
            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);

            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

            IntBuffer presentSupport = stack.ints(VK_FALSE);
            for (int i = 0; i < queueFamilies.capacity() || !indices.isComplete(); i++) {
                if ((queueFamilies.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0)
                    indices.graphicsFamily = i;

                vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, presentSupport);
                if (presentSupport.get(0) == VK_TRUE)
                    indices.presentFamily = i;
            }
        }

        return indices;
    }
}

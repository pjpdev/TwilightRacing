package za.madtek.arcaderacer.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import za.madtek.arcaderacer.Globals;
import za.madtek.arcaderacer.math.MathUtil;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.VK10.*;

public class SwapChain {

    private long swapChain;
    private List<Long> images;
    private int imageFormat;
    private VkExtent2D extent;

    protected void init(VkPhysicalDevice physicalDevice, VkDevice device, long surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SwapChainSupportDetails supportDetails = new SwapChainSupportDetails();
            supportDetails.querySupport(physicalDevice, surface, stack);
            VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(supportDetails.getFormats());
            int presentMode = chooseSwapPresentMode(supportDetails.getPresentModes());
            VkExtent2D pExtent = chooseSwapExtent(supportDetails.getCapabilities());

            int imageCount = supportDetails.getCapabilities().minImageCount() + 1;
            if (supportDetails.getCapabilities().maxImageCount() > 0 && imageCount > supportDetails.getCapabilities().maxImageCount())
                imageCount = supportDetails.getCapabilities().maxImageCount();

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(surface);
            createInfo.minImageCount(imageCount);
            createInfo.imageFormat(surfaceFormat.format());
            createInfo.imageColorSpace(surfaceFormat.colorSpace());
            createInfo.imageExtent(pExtent);
            createInfo.imageArrayLayers(1);
            createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

            QueueFamilyIndices indices = QueueFamilyIndices.findQueueFamilies(physicalDevice, surface);
            if (!indices.getGraphicsFamily().equals(indices.getPresentFamily())) {
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                createInfo.pQueueFamilyIndices(stack.ints(indices.getGraphicsFamily(), indices.getPresentFamily()));
            } else {
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }

            createInfo.preTransform(supportDetails.getCapabilities().currentTransform());
            createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode);
            createInfo.clipped(true);
            createInfo.oldSwapchain(VK_NULL_HANDLE);

            LongBuffer pSwapChain = stack.longs(VK_NULL_HANDLE);
            if (vkCreateSwapchainKHR(device, createInfo, null, pSwapChain) != VK_SUCCESS)
                throw new RuntimeException("Failed to create swap chain");

            swapChain = pSwapChain.get(0);

            IntBuffer swapChainImageCount = stack.ints(0);
            vkGetSwapchainImagesKHR(device, swapChain, swapChainImageCount, null);

            LongBuffer pSwapChainImages = stack.mallocLong(swapChainImageCount.get(0));
            vkGetSwapchainImagesKHR(device, swapChain, swapChainImageCount, pSwapChainImages);

            images = new ArrayList<>(swapChainImageCount.get(0));
            for (int i = 0; i < pSwapChainImages.capacity(); i++) {
                images.add(pSwapChainImages.get(i));
            }

            imageFormat = surfaceFormat.format();
            extent = VkExtent2D.create().set(pExtent);
        }
    }

    protected void free(VkDevice device) {
        vkDestroySwapchainKHR(device, swapChain, null);
    }

    private VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats) {
        return availableFormats.stream()
                .filter(availableFormat -> availableFormat.format() == VK_FORMAT_B8G8R8A8_SRGB)
                .filter(availableFormat -> availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                .findAny().orElse(availableFormats.get(0));
    }

    private int chooseSwapPresentMode(IntBuffer availablePresentModes) {
        // TODO: Vsync stuffs...

        for (int i = 0; i < availablePresentModes.capacity(); i++) {
            if (availablePresentModes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR)
                return availablePresentModes.get(i);
        }

        return VK_PRESENT_MODE_FIFO_KHR;
        //return VK_PRESENT_MODE_IMMEDIATE_KHR;
    }

    private VkExtent2D chooseSwapExtent(VkSurfaceCapabilitiesKHR capabilities) {
        if (capabilities.currentExtent().width() != MathUtil.UINT32_MAX) {
            return capabilities.currentExtent();
        }

        VkExtent2D actualExtent = VkExtent2D.malloc().set(Globals.WIDTH, Globals.HEIGHT);
        VkExtent2D minExtent = capabilities.minImageExtent();
        VkExtent2D maxExtent = capabilities.maxImageExtent();
        actualExtent.width(MathUtil.clamp(minExtent.width(), maxExtent.width(), actualExtent.width()));
        actualExtent.height(MathUtil.clamp(minExtent.height(), maxExtent.height(), actualExtent.height()));

        return actualExtent;
    }
}

package za.madtek.arcaderacer.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Set;
import java.util.stream.Collectors;

import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class ValidationLayers {

    private long debugMessenger;
    private final Set<String> validationLayers = Set.of("VK_LAYER_KHRONOS_validation");

    private int debugCallback(int messageSeverity, int messageType, long pCallbackData, long pUserData) {
        VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
        System.err.println("Validation Layer: " + callbackData.pMessageString());

        return VK_FALSE;
    }

    private int createDebugUtilsMessengerEXT(VkInstance instance, VkDebugUtilsMessengerCreateInfoEXT createInfo,
                                                    VkAllocationCallbacks allocationCallbacks, LongBuffer pDebugMessenger) {
        if (vkGetInstanceProcAddr(instance, "vkCreateDebugUtilsMessengerEXT") != NULL) {
            return vkCreateDebugUtilsMessengerEXT(instance, createInfo, allocationCallbacks, pDebugMessenger);
        }

        return VK_ERROR_EXTENSION_NOT_PRESENT;
    }

    protected boolean checkValidationLayerSupport() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer layerCount = stack.ints(0);
            vkEnumerateInstanceLayerProperties(layerCount, null);

            VkLayerProperties.Buffer availableLayers = VkLayerProperties.calloc(layerCount.get(0), stack);
            vkEnumerateInstanceLayerProperties(layerCount, availableLayers);

            Set<String> availableLayerNames = availableLayers.stream()
                    .map(VkLayerProperties::layerNameString)
                    .collect(Collectors.toSet());

            return availableLayerNames.containsAll(validationLayers);
        }
    }

    protected void populateDebugMessengerCreateInfo(VkDebugUtilsMessengerCreateInfoEXT createInfo) {
        createInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
        createInfo.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT |
                VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT);
        createInfo.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
        createInfo.pfnUserCallback(this::debugCallback);
    }

    protected PointerBuffer getAsPointerBuffer() {
        MemoryStack stack = stackGet();

        PointerBuffer pValidationLayers = stack.mallocPointer(validationLayers.size());
        validationLayers.stream().map(stack::UTF8).forEach(pValidationLayers::put);

        return pValidationLayers.rewind();
    }

    protected void setupDebugMessenger(VkInstance instance) {
        try (MemoryStack stack = stackPush()) {
            VkDebugUtilsMessengerCreateInfoEXT createInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
            populateDebugMessengerCreateInfo(createInfo);

            LongBuffer pDebugMessenger = stack.longs(VK_NULL_HANDLE);

            if (createDebugUtilsMessengerEXT(instance, createInfo, null, pDebugMessenger) != VK_SUCCESS)
                throw new RuntimeException("Failed to setup debug messenger.");

            debugMessenger = pDebugMessenger.get(0);
        }
    }

    protected void free(VkInstance instance) {
        if (vkGetInstanceProcAddr(instance, "vkDestroyDebugUtilsMessengerEXT") != NULL) {
            vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
        }
    }
}

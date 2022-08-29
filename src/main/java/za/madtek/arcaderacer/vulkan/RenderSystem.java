package za.madtek.arcaderacer.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import za.madtek.arcaderacer.Globals;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Set;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;

public class RenderSystem {

    private static final Set<String> DEVICE_EXTENSIONS = Set.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME);

    private ValidationLayers validationLayers;

    private VkInstance instance;
    private VkDevice device;
    private VkPhysicalDevice physicalDevice;

    private long surface;
    private VkQueue graphicsQueue;
    private VkQueue presentQueue;
    private long commandPool;
    private SwapChain swapChain;

    public RenderSystem() {
        if (Globals.ENABLE_VALIDATION_LAYERS)
            validationLayers = new ValidationLayers();

        swapChain = new SwapChain();
    }

    public void init() {
        createInstance();

        if (Globals.ENABLE_VALIDATION_LAYERS)
            validationLayers.setupDebugMessenger(instance);

        createSurface();
        pickPhysicalDevice();
        createLogicalDevice();
        createCommandPool();
        createSwapChainObjects();
    }

    public void free() {
        if (device != null) {
            swapChain.free(device);

            vkDestroyCommandPool(device, commandPool, null);
            vkDestroyDevice(device, null);
        }

        vkDestroySurfaceKHR(instance, surface, null);

        if (Globals.ENABLE_VALIDATION_LAYERS)
            validationLayers.free(instance);

        if (instance != null)
            vkDestroyInstance(instance, null);
    }

    public VkDevice getDevice() {
        return device;
    }

    public VkPhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    public long getSurface() {
        return surface;
    }

    private void createInstance() {
        if (Globals.ENABLE_VALIDATION_LAYERS && !validationLayers.checkValidationLayerSupport())
            throw new RuntimeException("Validation layers requested, but not available.");

        try (MemoryStack stack = stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);
            appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(stack.UTF8Safe(Globals.TITLE));
            appInfo.applicationVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.pEngineName(stack.UTF8Safe("No Engine"));
            appInfo.engineVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.apiVersion(VK_API_VERSION_1_0);

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            createInfo.pApplicationInfo(appInfo);
            createInfo.ppEnabledExtensionNames(getRequiredExtensions());

            if (Globals.ENABLE_VALIDATION_LAYERS) {
                createInfo.ppEnabledLayerNames(validationLayers.getAsPointerBuffer());

                VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
                validationLayers.populateDebugMessengerCreateInfo(debugCreateInfo);
                createInfo.pNext(debugCreateInfo.address());
            }

            PointerBuffer pInstance = stack.mallocPointer(1);

            if (vkCreateInstance(createInfo, null, pInstance) != VK_SUCCESS)
                throw new RuntimeException("Failed to create Vulkan instance");

            instance = new VkInstance(pInstance.get(0), createInfo);
        }
    }

    private PointerBuffer getRequiredExtensions() {
        PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();

        if (Globals.ENABLE_VALIDATION_LAYERS) {
            MemoryStack stack = MemoryStack.stackGet();
            PointerBuffer extensions = stack.mallocPointer(glfwExtensions.capacity() + 1);
            extensions.put(glfwExtensions);
            extensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));
            return extensions.rewind();
        }

        return glfwExtensions;
    }

    private void createSurface() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pSurface = stack.mallocLong(1);

            if (glfwCreateWindowSurface(instance, Globals.window.getHandle(), null, pSurface) != VK_SUCCESS)
                throw new RuntimeException("Failed to create surface");

            surface = pSurface.get(0);
        }
    }

    private void pickPhysicalDevice() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer deviceCount = stack.ints(0);

            vkEnumeratePhysicalDevices(instance, deviceCount, null);
            if (deviceCount.get(0) == 0)
                throw new RuntimeException("Failed to find GPUs with Vulkan Support!");

            PointerBuffer ppPhysicalDevices = stack.mallocPointer(deviceCount.get(0));
            vkEnumeratePhysicalDevices(instance, deviceCount, ppPhysicalDevices);
            for (int i = 0; i < ppPhysicalDevices.capacity(); i++) {
                VkPhysicalDevice device = new VkPhysicalDevice(ppPhysicalDevices.get(i), instance);
                if (isDeviceSuitable(device)) {
                    physicalDevice = device;
                    break;
                }
            }

            if (physicalDevice == null)
                throw new RuntimeException("Failed to find suitable GPU");
        }
    }

    private boolean isDeviceSuitable(VkPhysicalDevice device) {
        QueueFamilyIndices indices = QueueFamilyIndices.findQueueFamilies(device, surface);

        boolean extensionsSupported = checkDeviceExtensionSupport(device);
        boolean swapChainAdequate = false;
        boolean anisotropySupported = false;

        if (extensionsSupported) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                SwapChainSupportDetails swapChainSupportDetails = new SwapChainSupportDetails();
                swapChainSupportDetails.querySupport(device, surface, stack);
                swapChainAdequate = swapChainSupportDetails.isAdequate();

                VkPhysicalDeviceFeatures supportedFeatures = VkPhysicalDeviceFeatures.malloc(stack);
                vkGetPhysicalDeviceFeatures(device, supportedFeatures);
                anisotropySupported = supportedFeatures.samplerAnisotropy();
            }
        }

        return indices.isComplete() && extensionsSupported && swapChainAdequate && anisotropySupported;
    }

    private boolean checkDeviceExtensionSupport(VkPhysicalDevice device) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer extensionCount = stack.ints(0);
            vkEnumerateDeviceExtensionProperties(device, (String) null, extensionCount, null);

            VkExtensionProperties.Buffer extensionProperties = VkExtensionProperties.calloc(extensionCount.get(0), stack);
            vkEnumerateDeviceExtensionProperties(device, (String) null, extensionCount, extensionProperties);

            return extensionProperties.stream()
                    .map(VkExtensionProperties::extensionNameString)
                    .collect(Collectors.toSet())
                    .containsAll(DEVICE_EXTENSIONS);
        }
    }

    private void createLogicalDevice() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            QueueFamilyIndices indices = QueueFamilyIndices.findQueueFamilies(physicalDevice, surface);

            int[] uniqueQueueFamilies = indices.unique();

            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueQueueFamilies.length, stack);

            for (int i = 0; i < uniqueQueueFamilies.length; i++) {
                VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get(i);
                queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueCreateInfo.queueFamilyIndex(uniqueQueueFamilies[i]);
                queueCreateInfo.pQueuePriorities(stack.floats(1.0f));
            }

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);
            deviceFeatures.samplerAnisotropy(true);

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            createInfo.pEnabledFeatures(deviceFeatures);

            PointerBuffer extensionsBuffer = stack.mallocPointer(DEVICE_EXTENSIONS.size());

            DEVICE_EXTENSIONS.stream()
                    .map(stack::UTF8)
                    .forEach(extensionsBuffer::put);

            createInfo.ppEnabledExtensionNames(extensionsBuffer.rewind());

            if (Globals.ENABLE_VALIDATION_LAYERS) {
                createInfo.ppEnabledLayerNames(validationLayers.getAsPointerBuffer());
            }

            PointerBuffer pDevice = stack.pointers(VK_NULL_HANDLE);
            if (vkCreateDevice(physicalDevice, createInfo, null, pDevice) != VK_SUCCESS)
                throw new RuntimeException("Failed to create logical device");

            device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);

            PointerBuffer pGraphicsQueue = stack.pointers(VK_NULL_HANDLE);
            vkGetDeviceQueue(device, indices.getGraphicsFamily(), 0, pGraphicsQueue);
            graphicsQueue = new VkQueue(pGraphicsQueue.get(0), device);

            PointerBuffer pPResentQueue = stack.pointers(VK_NULL_HANDLE);
            vkGetDeviceQueue(device, indices.getPresentFamily(), 0, pPResentQueue);
            presentQueue = new VkQueue(pGraphicsQueue.get(0), device);
        }
    }

    private void createCommandPool() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            QueueFamilyIndices indices = QueueFamilyIndices.findQueueFamilies(physicalDevice, surface);

            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
            poolInfo.queueFamilyIndex(indices.getGraphicsFamily());

            LongBuffer pCommandPool = stack.longs(VK_NULL_HANDLE);
            if (vkCreateCommandPool(device, poolInfo, null, pCommandPool) != VK_SUCCESS)
                throw new RuntimeException("Failed to create command pool");
            commandPool = pCommandPool.get(0);
        }
    }

    private void createSwapChainObjects() {
        swapChain.init(physicalDevice, device, surface);
    }
}

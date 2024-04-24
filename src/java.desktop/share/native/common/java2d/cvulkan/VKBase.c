/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2024, JetBrains s.r.o.. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include <malloc.h>
#include <Trace.h>
#include "jvm_md.h"
#include "VKBase.h"
#include "VKVertex.h"
#include "CArrayUtil.h"
#include <vulkan/vulkan.h>
#include <dlfcn.h>
#include <string.h>

#define VULKAN_DLL JNI_LIB_NAME("vulkan")
#define VULKAN_1_DLL VERSIONED_JNI_LIB_NAME("vulkan", "1")
static const uint32_t REQUIRED_VULKAN_VERSION = VK_MAKE_API_VERSION(0, 1, 2, 0);


#define MAX_ENABLED_LAYERS 5
#define MAX_ENABLED_EXTENSIONS 5
#define VALIDATION_LAYER_NAME "VK_LAYER_KHRONOS_validation"
#define COUNT_OF(x) (sizeof(x)/sizeof(x[0]))
#if defined(VK_USE_PLATFORM_WAYLAND_KHR)
extern struct wl_display *wl_display;
#endif

static jboolean verbose;
static jint requestedDeviceNumber = -1;
static VKGraphicsEnvironment* geInstance = NULL;
static void* pVulkanLib = NULL;
#define DEBUG
#define INCLUDE_BYTECODE
#define SHADER_ENTRY(NAME, TYPE) static uint32_t NAME ## _ ## TYPE ## _data[] = {
#define BYTECODE_END };
#include "vulkan/shader_list.h"
#undef INCLUDE_BYTECODE
#undef SHADER_ENTRY
#undef BYTECODE_END

#define DEF_VK_PROC_RET_IF_ERR(INST, NAME, RETVAL) PFN_ ## NAME NAME = (PFN_ ## NAME ) vulkanLibProc(INST, #NAME); \
    if (NAME == NULL) {                                                                        \
        J2dRlsTraceLn1(J2D_TRACE_ERROR, "Required api is not supported. %s is missing.", #NAME)\
        vulkanLibClose();                                                                      \
        return RETVAL;                                                                         \
    }


#define VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(SNAME, NAME) do {                                    \
    SNAME->NAME = (PFN_ ## NAME ) vulkanLibProc( SNAME->vkInstance, #NAME);                    \
    if (SNAME->NAME == NULL) {                                                                 \
        J2dRlsTraceLn1(J2D_TRACE_ERROR, "Required api is not supported. %s is missing.", #NAME)\
        vulkanLibClose();                                                                      \
        return NULL;                                                                           \
    }                                                                                          \
} while (0)

static void vulkanLibClose() {
    if (pVulkanLib != NULL) {
        if (geInstance != NULL) {
            if (geInstance->layers != NULL) {
                free(geInstance->layers);
            }
            if (geInstance->extensions != NULL) {
                free(geInstance->extensions);
            }
            ARRAY_FREE(geInstance->physicalDevices);
            if (geInstance->devices != NULL) {
                PFN_vkDestroyDevice vkDestroyDevice = vulkanLibProc(geInstance->vkInstance, "vkDestroyDevice");

                for (uint32_t i = 0; i < ARRAY_SIZE(geInstance->devices); i++) {
                    if (geInstance->devices[i].enabledExtensions != NULL) {
                        free(geInstance->devices[i].enabledExtensions);
                    }
                    if (geInstance->devices[i].enabledLayers != NULL) {
                        free(geInstance->devices[i].enabledLayers);
                    }
                    if (geInstance->devices[i].name != NULL) {
                        free(geInstance->devices[i].name);
                    }
                    if (vkDestroyDevice != NULL && geInstance->devices[i].device != NULL) {
                        vkDestroyDevice(geInstance->devices[i].device, NULL);
                    }
                }
                free(geInstance->devices);
            }

            if (geInstance->vkInstance != NULL) {
                PFN_vkDestroyInstance vkDestroyInstance = vulkanLibProc(geInstance->vkInstance, "vkDestroyInstance");
                if (vkDestroyInstance != NULL) {
                    vkDestroyInstance(geInstance->vkInstance, NULL);
                }
            }
            free(geInstance);
            geInstance = NULL;
        }
        dlclose(pVulkanLib);
        pVulkanLib = NULL;
    }
}

void* vulkanLibProc(VkInstance vkInstance, char* procName) {
    static PFN_vkGetInstanceProcAddr vkGetInstanceProcAddr = NULL;
    if (pVulkanLib == NULL) {
        pVulkanLib = dlopen(VULKAN_DLL, RTLD_NOW);
        if (pVulkanLib == NULL) {
            pVulkanLib = dlopen(VULKAN_1_DLL, RTLD_NOW);
        }
        if (pVulkanLib == NULL) {
            J2dRlsTrace1(J2D_TRACE_ERROR, "Failed to load %s\n", VULKAN_DLL)
            return NULL;
        }
        if (!vkGetInstanceProcAddr) {
            vkGetInstanceProcAddr = (PFN_vkGetInstanceProcAddr) dlsym(pVulkanLib, "vkGetInstanceProcAddr");
            if (vkGetInstanceProcAddr == NULL) {
                J2dRlsTrace1(J2D_TRACE_ERROR,
                            "Failed to get proc address of vkGetInstanceProcAddr from %s\n", VULKAN_DLL)
                return NULL;
            }
        }
    }

    void* vkProc = vkGetInstanceProcAddr(vkInstance, procName);

    if (vkProc == NULL) {
        J2dRlsTrace1(J2D_TRACE_ERROR, "%s is not supported\n", procName)
        return NULL;
    }
    return vkProc;
}


jboolean VK_Init(jboolean verb, jint requestedDevice) {
    verbose = verb;
    if (VKGE_graphics_environment() == NULL) {
        return JNI_FALSE;
    }
    if (!VK_FindDevices()) {
        return JNI_FALSE;
    }
    return VK_CreateLogicalDevice(requestedDevice);
}

static const char* physicalDeviceTypeString(VkPhysicalDeviceType type)
{
    switch (type)
    {
#define STR(r) case VK_PHYSICAL_DEVICE_TYPE_ ##r: return #r
        STR(OTHER);
        STR(INTEGRATED_GPU);
        STR(DISCRETE_GPU);
        STR(VIRTUAL_GPU);
        STR(CPU);
#undef STR
        default: return "UNKNOWN_DEVICE_TYPE";
    }
}

VKGraphicsEnvironment* VKGE_graphics_environment() {
    if (geInstance == NULL) {

        DEF_VK_PROC_RET_IF_ERR(VK_NULL_HANDLE, vkEnumerateInstanceVersion, NULL);
        DEF_VK_PROC_RET_IF_ERR(VK_NULL_HANDLE, vkEnumerateInstanceExtensionProperties, NULL);
        DEF_VK_PROC_RET_IF_ERR(VK_NULL_HANDLE, vkEnumerateInstanceLayerProperties, NULL);
        DEF_VK_PROC_RET_IF_ERR(VK_NULL_HANDLE, vkCreateInstance, NULL);

        uint32_t apiVersion = 0;

        if (vkEnumerateInstanceVersion(&apiVersion) != VK_SUCCESS) {
            J2dRlsTrace(J2D_TRACE_ERROR, "Vulkan: unable to enumerate Vulkan instance version\n")
            vulkanLibClose();
            return NULL;
        }

        J2dRlsTrace3(J2D_TRACE_INFO, "Vulkan: Available (%d.%d.%d)\n",
                     VK_API_VERSION_MAJOR(apiVersion),
                     VK_API_VERSION_MINOR(apiVersion),
                     VK_API_VERSION_PATCH(apiVersion))

        if (apiVersion < REQUIRED_VULKAN_VERSION) {
            J2dRlsTrace3(J2D_TRACE_ERROR, "Vulkan: Unsupported version. Required at least (%d.%d.%d)\n",
                         VK_API_VERSION_MAJOR(REQUIRED_VULKAN_VERSION),
                         VK_API_VERSION_MINOR(REQUIRED_VULKAN_VERSION),
                         VK_API_VERSION_PATCH(REQUIRED_VULKAN_VERSION))
            vulkanLibClose();
            return NULL;
        }

        geInstance = (VKGraphicsEnvironment*)malloc(sizeof(VKGraphicsEnvironment));
        if (geInstance == NULL) {
            J2dRlsTrace(J2D_TRACE_ERROR, "Vulkan: Cannot allocate VKGraphicsEnvironment\n")
            vulkanLibClose();
            return NULL;
        }

        *geInstance = (VKGraphicsEnvironment) {};
        uint32_t extensionsCount;
        // Get the number of extensions and layers
        if (vkEnumerateInstanceExtensionProperties(NULL,
                                                   &extensionsCount,
                                                   NULL) != VK_SUCCESS)
        {
            J2dRlsTrace(J2D_TRACE_ERROR, "Vulkan: vkEnumerateInstanceExtensionProperties fails\n")
            vulkanLibClose();
            return NULL;
        }

        geInstance->extensions = ARRAY_ALLOC(VkExtensionProperties, extensionsCount);

        if (geInstance->extensions == NULL) {
            J2dRlsTrace(J2D_TRACE_ERROR, "Vulkan: Cannot allocate VkExtensionProperties\n")
            vulkanLibClose();
            return NULL;
        }

        if (vkEnumerateInstanceExtensionProperties(NULL, &extensionsCount,
                                                   geInstance->extensions) != VK_SUCCESS)
        {
            J2dRlsTrace(J2D_TRACE_ERROR, "Vulkan: vkEnumerateInstanceExtensionProperties fails\n")
            vulkanLibClose();
            return NULL;
        }

        ARRAY_SIZE(geInstance->extensions) = extensionsCount;

        uint32_t layersCount;
        if (vkEnumerateInstanceLayerProperties(&layersCount, NULL) != VK_SUCCESS)
        {
            J2dRlsTrace(J2D_TRACE_ERROR, "Vulkan: vkEnumerateInstanceLayerProperties fails\n")
            vulkanLibClose();
            return NULL;
        }

        geInstance->layers = ARRAY_ALLOC(VkLayerProperties, layersCount);
        if (geInstance->layers == NULL) {
            J2dRlsTrace(J2D_TRACE_ERROR, "Vulkan: Cannot allocate VkLayerProperties\n")
            vulkanLibClose();
            return NULL;
        }

        if (vkEnumerateInstanceLayerProperties(&layersCount,
                                               geInstance->layers) != VK_SUCCESS)
        {
            J2dRlsTrace(J2D_TRACE_ERROR, "Vulkan: vkEnumerateInstanceLayerProperties fails\n")
            vulkanLibClose();
            return NULL;
        }

        ARRAY_SIZE(geInstance->layers) = layersCount;
        J2dRlsTrace(J2D_TRACE_VERBOSE, "    Supported instance layers:\n")
        for (uint32_t i = 0; i < layersCount; i++) {
            J2dRlsTrace1(J2D_TRACE_VERBOSE, "        %s\n", (char *) geInstance->layers[i].layerName)
        }

        J2dRlsTrace(J2D_TRACE_VERBOSE, "    Supported instance extensions:\n")
        for (uint32_t i = 0; i < extensionsCount; i++) {
            J2dRlsTrace1(J2D_TRACE_VERBOSE, "        %s\n", (char *) geInstance->extensions[i].extensionName)
        }

        char* enabledLayers[MAX_ENABLED_LAYERS];
        uint32_t enabledLayersCount = 0;
        char* enabledExtensions[MAX_ENABLED_EXTENSIONS];
        uint32_t enabledExtensionsCount = 0;
        void *pNext = NULL;
#if defined(VK_USE_PLATFORM_WAYLAND_KHR)
        enabledExtensions[enabledExtensionsCount++] = VK_KHR_WAYLAND_SURFACE_EXTENSION_NAME;
#endif
        enabledExtensions[enabledExtensionsCount++] = VK_KHR_SURFACE_EXTENSION_NAME;

        // Check required layers & extensions.
        for (uint32_t i = 0; i < enabledExtensionsCount; i++) {
            int notFound = 1;
            for (uint32_t j = 0; j < extensionsCount; j++) {
                if (strcmp((char *) geInstance->extensions[j].extensionName, enabledExtensions[i]) == 0) {
                    notFound = 0;
                    break;
                }
            }
            if (notFound) {
                J2dRlsTrace1(J2D_TRACE_ERROR, "Vulkan: Required extension %s not found\n", enabledExtensions[i])
                vulkanLibClose();
                return NULL;
            }
        }

        // Configure validation
#ifdef DEBUG
        VkValidationFeatureEnableEXT enables[] = {
//                VK_VALIDATION_FEATURE_ENABLE_GPU_ASSISTED_EXT,
//                VK_VALIDATION_FEATURE_ENABLE_GPU_ASSISTED_RESERVE_BINDING_SLOT_EXT,
                VK_VALIDATION_FEATURE_ENABLE_BEST_PRACTICES_EXT,
//                VK_VALIDATION_FEATURE_ENABLE_DEBUG_PRINTF_EXT,
                VK_VALIDATION_FEATURE_ENABLE_SYNCHRONIZATION_VALIDATION_EXT
        };

        VkValidationFeaturesEXT features = {};
        features.sType = VK_STRUCTURE_TYPE_VALIDATION_FEATURES_EXT;
        features.enabledValidationFeatureCount = COUNT_OF(enables);
        features.pEnabledValidationFeatures = enables;

       // Includes the validation features into the instance creation process

        int foundDebugLayer = 0;
        for (uint32_t i = 0; i < layersCount; i++) {
            if (strcmp((char *) geInstance->layers[i].layerName, VALIDATION_LAYER_NAME) == 0) {
                foundDebugLayer = 1;
                break;
            }
            J2dRlsTrace1(J2D_TRACE_VERBOSE, "        %s\n", (char *) geInstance->layers[i].layerName)
        }
        int foundDebugExt = 0;
        for (uint32_t i = 0; i < extensionsCount; i++) {
            if (strcmp((char *) geInstance->extensions[i].extensionName, VK_EXT_DEBUG_UTILS_EXTENSION_NAME) == 0) {
                foundDebugExt = 1;
                break;
            }
        }

        if (foundDebugLayer && foundDebugExt) {
            enabledLayers[enabledLayersCount++] = VALIDATION_LAYER_NAME;
            enabledExtensions[enabledExtensionsCount++] = VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
            pNext = &features;
        } else {
            J2dRlsTrace2(J2D_TRACE_WARNING, "Vulkan: %s and %s are not supported\n",
                       VALIDATION_LAYER_NAME, VK_EXT_DEBUG_UTILS_EXTENSION_NAME)
        }
#endif
        VkApplicationInfo applicationInfo = {
                .sType = VK_STRUCTURE_TYPE_APPLICATION_INFO,
                .pNext = NULL,
                .pApplicationName = "OpenJDK",
                .applicationVersion = 0,
                .pEngineName = "OpenJDK",
                .engineVersion = 0,
                .apiVersion = REQUIRED_VULKAN_VERSION
        };

        VkInstanceCreateInfo instanceCreateInfo = {
                .sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO,
                .pNext =pNext,
                .flags = 0,
                .pApplicationInfo = &applicationInfo,
                .enabledLayerCount = enabledLayersCount,
                .ppEnabledLayerNames = (const char *const *) enabledLayers,
                .enabledExtensionCount = enabledExtensionsCount,
                .ppEnabledExtensionNames = (const char *const *) enabledExtensions
        };

        if (vkCreateInstance(&instanceCreateInfo, NULL, &geInstance->vkInstance) != VK_SUCCESS) {
            J2dRlsTrace(J2D_TRACE_ERROR, "Vulkan: Failed to create Vulkan instance\n")
            vulkanLibClose();
            return NULL;
        } else {
            J2dRlsTrace(J2D_TRACE_INFO, "Vulkan: Instance Created\n")
        }

        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkEnumeratePhysicalDevices);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkGetPhysicalDeviceFeatures2);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkGetPhysicalDeviceProperties2);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkGetPhysicalDeviceQueueFamilyProperties);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkEnumerateDeviceLayerProperties);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkEnumerateDeviceExtensionProperties);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCreateShaderModule);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCreatePipelineLayout);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCreateGraphicsPipelines);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkDestroyShaderModule);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkGetPhysicalDeviceSurfaceCapabilitiesKHR);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkGetPhysicalDeviceSurfaceFormatsKHR);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkGetPhysicalDeviceSurfacePresentModesKHR);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCreateSwapchainKHR);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkGetSwapchainImagesKHR);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCreateImageView);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCreateFramebuffer);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCreateCommandPool);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkAllocateCommandBuffers);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCreateSemaphore);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCreateFence);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkGetDeviceQueue);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkWaitForFences);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkResetFences);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkAcquireNextImageKHR);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkResetCommandBuffer);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkQueueSubmit);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkQueuePresentKHR);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkBeginCommandBuffer);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCmdBeginRenderPass);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCmdBindPipeline);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCmdSetViewport);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCmdSetScissor);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCmdDraw);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkEndCommandBuffer);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCmdEndRenderPass);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCreateImage);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCreateSampler);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkAllocateMemory);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkGetPhysicalDeviceMemoryProperties);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkBindImageMemory);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCreateDescriptorSetLayout);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkUpdateDescriptorSets);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCreateDescriptorPool);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkAllocateDescriptorSets);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCmdBindDescriptorSets);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkGetImageMemoryRequirements);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCreateBuffer);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkGetBufferMemoryRequirements);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkBindBufferMemory);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkMapMemory);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkUnmapMemory);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCmdBindVertexBuffers);

#if defined(VK_USE_PLATFORM_WAYLAND_KHR)
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkGetPhysicalDeviceWaylandPresentationSupportKHR);
        VKGE_INIT_VK_PROC_RET_NULL_IF_ERR(geInstance, vkCreateWaylandSurfaceKHR);
#endif

    }
    return geInstance;
}

VkShaderModule createShaderModule(VkDevice device, uint32_t* shader, uint32_t sz) {
    VkShaderModuleCreateInfo createInfo = {};
    createInfo.sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
    createInfo.codeSize = sz;
    createInfo.pCode = (uint32_t*)shader;
    VkShaderModule shaderModule;
    if (geInstance->vkCreateShaderModule(device, &createInfo, NULL, &shaderModule) != VK_SUCCESS) {
        J2dRlsTrace(J2D_TRACE_ERROR, "failed to create shader module\n")
        return VK_NULL_HANDLE;
    }
    return shaderModule;
}


jboolean VK_FindDevices() {
    uint32_t physicalDevicesCount;
    if (geInstance->vkEnumeratePhysicalDevices(geInstance->vkInstance,
                                               &physicalDevicesCount,
                                               NULL) != VK_SUCCESS)
    {
        J2dRlsTrace(J2D_TRACE_ERROR, "Vulkan: vkEnumeratePhysicalDevices fails\n")
        vulkanLibClose();
        return JNI_FALSE;
    }

    if (physicalDevicesCount == 0) {
        J2dRlsTrace(J2D_TRACE_ERROR, "Vulkan: Failed to find GPUs with Vulkan support\n")
        vulkanLibClose();
        return JNI_FALSE;
    } else {
        J2dRlsTrace1(J2D_TRACE_INFO, "Vulkan: Found %d physical devices:\n", physicalDevicesCount)
    }

    geInstance->physicalDevices = ARRAY_ALLOC(VkPhysicalDevice, physicalDevicesCount);
    if (geInstance->physicalDevices == NULL) {
        J2dRlsTrace(J2D_TRACE_ERROR, "Vulkan: Cannot allocate VkPhysicalDevice\n")
        vulkanLibClose();
        return JNI_FALSE;
    }

    if (geInstance->vkEnumeratePhysicalDevices(
            geInstance->vkInstance,
            &physicalDevicesCount,
            geInstance->physicalDevices) != VK_SUCCESS)
    {
        J2dRlsTrace(J2D_TRACE_ERROR, "Vulkan: vkEnumeratePhysicalDevices fails\n")
        vulkanLibClose();
        return JNI_FALSE;
    }

    geInstance->devices = ARRAY_ALLOC(VKLogicalDevice, physicalDevicesCount);
    if (geInstance->devices == NULL) {
        J2dRlsTrace(J2D_TRACE_ERROR, "Vulkan: Cannot allocate VKLogicalDevice\n")
        vulkanLibClose();
        return JNI_FALSE;
    }

    for (uint32_t i = 0; i < physicalDevicesCount; i++) {
        VkPhysicalDeviceVulkan12Features device12Features = {
                .sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES,
                .pNext = NULL
        };

        VkPhysicalDeviceFeatures2 deviceFeatures2 = {
                .sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2,
                .pNext = &device12Features
        };

        geInstance->vkGetPhysicalDeviceFeatures2(geInstance->physicalDevices[i], &deviceFeatures2);

        VkPhysicalDeviceProperties2 deviceProperties2 = {.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2};
        geInstance->vkGetPhysicalDeviceProperties2(geInstance->physicalDevices[i], &deviceProperties2);
        J2dRlsTrace5(J2D_TRACE_INFO, "\t- %s (%d.%d.%d, %s) ",
                     (const char *) deviceProperties2.properties.deviceName,
                     VK_API_VERSION_MAJOR(deviceProperties2.properties.apiVersion),
                     VK_API_VERSION_MINOR(deviceProperties2.properties.apiVersion),
                     VK_API_VERSION_PATCH(deviceProperties2.properties.apiVersion),
                     physicalDeviceTypeString(deviceProperties2.properties.deviceType))

        if (!deviceFeatures2.features.logicOp) {
            J2dRlsTrace(J2D_TRACE_INFO, " - hasLogicOp not supported, skipped \n")
            continue;
        }

        if (!device12Features.timelineSemaphore) {
            J2dRlsTrace(J2D_TRACE_INFO, " - hasTimelineSemaphore not supported, skipped \n")
            continue;
        }
        J2dRlsTrace(J2D_TRACE_INFO, "\n")

        uint32_t queueFamilyCount = 0;
        geInstance->vkGetPhysicalDeviceQueueFamilyProperties(
                geInstance->physicalDevices[i], &queueFamilyCount, NULL);

        VkQueueFamilyProperties *queueFamilies = (VkQueueFamilyProperties*)calloc(queueFamilyCount,
                                                                                  sizeof(VkQueueFamilyProperties));
        if (queueFamilies == NULL) {
            J2dRlsTrace(J2D_TRACE_ERROR, "Vulkan: Cannot allocate VkQueueFamilyProperties\n")
            vulkanLibClose();
            return JNI_FALSE;
        }

        geInstance->vkGetPhysicalDeviceQueueFamilyProperties(
                geInstance->physicalDevices[i], &queueFamilyCount, queueFamilies);
        int64_t queueFamily = -1;
        for (uint32_t j = 0; j < queueFamilyCount; j++) {
#if defined(VK_USE_PLATFORM_WAYLAND_KHR)
            VkBool32 presentationSupported =
                geInstance->vkGetPhysicalDeviceWaylandPresentationSupportKHR(
                    geInstance->physicalDevices[i], j, wl_display);
#endif
            char logFlags[5] = {
                    queueFamilies[j].queueFlags & VK_QUEUE_GRAPHICS_BIT ? 'G' : '-',
                    queueFamilies[j].queueFlags & VK_QUEUE_COMPUTE_BIT ? 'C' : '-',
                    queueFamilies[j].queueFlags & VK_QUEUE_TRANSFER_BIT ? 'T' : '-',
                    queueFamilies[j].queueFlags & VK_QUEUE_SPARSE_BINDING_BIT ? 'S' : '-',
#if defined(VK_USE_PLATFORM_WAYLAND_KHR)
                    presentationSupported ? 'P' : '-'
#else
                    '-'
#endif
            };
            J2dRlsTrace3(J2D_TRACE_INFO, "    %d queues in family (%.*s)\n", queueFamilies[j].queueCount, 5,
                         logFlags)

            // TODO use compute workloads? Separate transfer-only DMA queue?
            if (queueFamily == -1 && (queueFamilies[j].queueFlags & VK_QUEUE_GRAPHICS_BIT)
                #if defined(VK_USE_PLATFORM_WAYLAND_KHR)
                && presentationSupported
#endif
                    ) {
                queueFamily = j;
            }
        }
        free(queueFamilies);
        if (queueFamily == -1) {
            J2dRlsTrace(J2D_TRACE_INFO, "    --------------------- Suitable queue not found, skipped \n")
            continue;
        }

        uint32_t layerCount;
        geInstance->vkEnumerateDeviceLayerProperties(geInstance->physicalDevices[i], &layerCount, NULL);
        VkLayerProperties *layers = (VkLayerProperties *) calloc(layerCount, sizeof(VkLayerProperties));
        if (layers == NULL) {
            J2dRlsTrace(J2D_TRACE_ERROR, "Vulkan: Cannot allocate VkLayerProperties\n")
            vulkanLibClose();
            return JNI_FALSE;
        }

        geInstance->vkEnumerateDeviceLayerProperties(geInstance->physicalDevices[i], &layerCount, layers);
        J2dRlsTrace(J2D_TRACE_VERBOSE, "    Supported device layers:\n")
        for (uint32_t j = 0; j < layerCount; j++) {
            J2dRlsTrace1(J2D_TRACE_VERBOSE, "        %s\n", (char *) layers[j].layerName)
        }

        uint32_t extensionCount;
        geInstance->vkEnumerateDeviceExtensionProperties(geInstance->physicalDevices[i], NULL, &extensionCount, NULL);
        VkExtensionProperties *extensions = (VkExtensionProperties *) calloc(
                extensionCount, sizeof(VkExtensionProperties));
        if (extensions == NULL) {
            J2dRlsTrace(J2D_TRACE_ERROR, "Vulkan: Cannot allocate VkExtensionProperties\n")
            vulkanLibClose();
            return JNI_FALSE;
        }

        geInstance->vkEnumerateDeviceExtensionProperties(
                geInstance->physicalDevices[i], NULL, &extensionCount, extensions);
        J2dRlsTrace(J2D_TRACE_VERBOSE, "    Supported device extensions:\n")
        VkBool32 hasSwapChain = VK_FALSE;
        for (uint32_t j = 0; j < extensionCount; j++) {
            J2dRlsTrace1(J2D_TRACE_VERBOSE, "        %s\n", (char *) extensions[j].extensionName)
            hasSwapChain = hasSwapChain ||
                           strcmp(VK_KHR_SWAPCHAIN_EXTENSION_NAME, extensions[j].extensionName) == 0;
        }
        free(extensions);
        J2dRlsTrace(J2D_TRACE_VERBOSE, "    Found:\n")
        if (hasSwapChain) {
            J2dRlsTrace(J2D_TRACE_VERBOSE, "    VK_KHR_SWAPCHAIN_EXTENSION_NAME\n")
        }

        if (!hasSwapChain) {
            J2dRlsTrace(J2D_TRACE_INFO,
                        "    --------------------- Required VK_KHR_SWAPCHAIN_EXTENSION_NAME not found, skipped \n")
            continue;
        }

        pchar* deviceEnabledLayers = ARRAY_ALLOC(pchar, MAX_ENABLED_LAYERS);
        if (deviceEnabledLayers == NULL) {
            J2dRlsTrace(J2D_TRACE_ERROR, "Vulkan: Cannot allocate deviceEnabledLayers array\n")
            vulkanLibClose();
            return JNI_FALSE;
        }

        pchar* deviceEnabledExtensions = ARRAY_ALLOC(pchar, MAX_ENABLED_EXTENSIONS);
        if (deviceEnabledExtensions == NULL) {
            J2dRlsTrace(J2D_TRACE_ERROR, "Vulkan: Cannot allocate deviceEnabledExtensions array\n")
            vulkanLibClose();
            return JNI_FALSE;
        }

        ARRAY_PUSH_BACK(&deviceEnabledExtensions, VK_KHR_SWAPCHAIN_EXTENSION_NAME);

        // Validation layer
#ifdef DEBUG
        int validationLayerNotSupported = 1;
            for (uint32_t j = 0; j < layerCount; j++) {
                if (strcmp(VALIDATION_LAYER_NAME, layers[j].layerName) == 0) {
                    validationLayerNotSupported = 0;
                    ARRAY_PUSH_BACK(&deviceEnabledLayers, VALIDATION_LAYER_NAME);
                    break;
                }
            }
            if (validationLayerNotSupported) {
                J2dRlsTrace1(J2D_TRACE_INFO, "    %s device layer is not supported\n", VALIDATION_LAYER_NAME)
            }
#endif
        free(layers);
        ARRAY_PUSH_BACK(&geInstance->devices,
                ((VKLogicalDevice) {
                .device = VK_NULL_HANDLE,
                .physicalDevice = geInstance->physicalDevices[i],
                .queueFamily = queueFamily,
                .enabledLayers = deviceEnabledLayers,
                .enabledExtensions = deviceEnabledExtensions,
        }));

        geInstance->devices[i].name = strdup(deviceProperties2.properties.deviceName);
        if (geInstance->devices[i].name == NULL) {
            J2dRlsTrace(J2D_TRACE_ERROR, "Vulkan: Cannot duplicate deviceName\n")
            vulkanLibClose();
            return JNI_FALSE;
        }

    }
    if (ARRAY_SIZE(geInstance->devices) == 0) {
        J2dRlsTrace(J2D_TRACE_ERROR, "No compatible device found\n")
        vulkanLibClose();
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

jboolean VK_CreateLogicalDevice(jint requestedDevice) {
    requestedDeviceNumber = requestedDevice;

    if (geInstance == NULL) {
        J2dRlsTrace(J2D_TRACE_ERROR, "Vulkan: VKGraphicsEnvironment is not initialized\n")
        return JNI_FALSE;
    }

    DEF_VK_PROC_RET_IF_ERR(geInstance->vkInstance, vkCreateDevice, JNI_FALSE)
    DEF_VK_PROC_RET_IF_ERR(geInstance->vkInstance, vkCreatePipelineCache, JNI_FALSE)
    DEF_VK_PROC_RET_IF_ERR(geInstance->vkInstance, vkCreateRenderPass, JNI_FALSE)

    requestedDeviceNumber = (requestedDeviceNumber == -1) ? 0 : requestedDeviceNumber;

    if (requestedDeviceNumber < 0 || (uint32_t)requestedDeviceNumber >= ARRAY_SIZE(geInstance->devices)) {
        if (verbose) {
            fprintf(stderr, "  Requested device number (%d) not found, fallback to 0\n", requestedDeviceNumber);
        }
        requestedDeviceNumber = 0;
    }
    geInstance->enabledDeviceNum = requestedDeviceNumber;
    if (verbose) {
        for (uint32_t i = 0; i < ARRAY_SIZE(geInstance->devices); i++) {
            fprintf(stderr, " %c%d: %s\n", i == geInstance->enabledDeviceNum ? '*' : ' ',
                    i, geInstance->devices[i].name);
        }
        fprintf(stderr, "\n");
    }

    VKLogicalDevice* logicalDevice = &geInstance->devices[geInstance->enabledDeviceNum];
    float queuePriority = 1.0f;
    VkDeviceQueueCreateInfo queueCreateInfo = {
            .sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO,
            .queueFamilyIndex = logicalDevice->queueFamily,  // obtained separately
            .queueCount = 1,
            .pQueuePriorities = &queuePriority
    };

    VkPhysicalDeviceFeatures features10 = { .logicOp = VK_TRUE };

    VkDeviceCreateInfo createInfo = {
        .sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO,
        .pNext = NULL,
        .flags = 0,
        .queueCreateInfoCount = 1,
        .pQueueCreateInfos = &queueCreateInfo,
        .enabledLayerCount = ARRAY_SIZE(logicalDevice->enabledLayers),
        .ppEnabledLayerNames = (const char *const *) logicalDevice->enabledLayers,
        .enabledExtensionCount = ARRAY_SIZE(logicalDevice->enabledExtensions),
        .ppEnabledExtensionNames = (const char *const *) logicalDevice->enabledExtensions,
        .pEnabledFeatures = &features10
    };

    if (vkCreateDevice(logicalDevice->physicalDevice, &createInfo, NULL, &logicalDevice->device) != VK_SUCCESS)
    {
        J2dRlsTrace1(J2D_TRACE_ERROR, "Cannot create device:\n    %s\n",
                     geInstance->devices[geInstance->enabledDeviceNum].name)
        vulkanLibClose();
        return JNI_FALSE;
    }
    VkDevice device = logicalDevice->device;
    J2dRlsTrace1(J2D_TRACE_INFO, "Logical device (%s) created\n", logicalDevice->name)

    VkPipelineCacheCreateInfo pipelineCacheCreateInfo = {};
    pipelineCacheCreateInfo.sType = VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO;
    if (vkCreatePipelineCache(device, &pipelineCacheCreateInfo, NULL, &logicalDevice->pipelineCache) != VK_SUCCESS)
    {
        J2dRlsTrace(J2D_TRACE_INFO, "Cannot create pipeline cache for device")
        return JNI_FALSE;
    }

    VkAttachmentDescription colorAttachment = {
            .format = VK_FORMAT_B8G8R8A8_UNORM, //TODO: swapChain colorFormat
            .samples = VK_SAMPLE_COUNT_1_BIT,
            .loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR,
            .storeOp = VK_ATTACHMENT_STORE_OP_STORE,
            .stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE,
            .stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE,
            .initialLayout = VK_IMAGE_LAYOUT_UNDEFINED,
            .finalLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
    };

    VkAttachmentReference colorReference = {
            .attachment = 0,
            .layout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL
    };

    VkSubpassDescription subpassDescription = {
            .pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS,
            .colorAttachmentCount = 1,
            .pColorAttachments = &colorReference
    };

    // Subpass dependencies for layout transitions
    VkSubpassDependency dependency = {
            .srcSubpass = VK_SUBPASS_EXTERNAL,
            .dstSubpass = 0,
            .srcStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
            .dstStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
            .srcAccessMask = 0,
            .dstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
    };

    VkRenderPassCreateInfo renderPassInfo = {
            .sType = VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO,
            .attachmentCount = 1,
            .pAttachments = &colorAttachment,
            .subpassCount = 1,
            .pSubpasses = &subpassDescription,
            .dependencyCount = 1,
            .pDependencies = &dependency
    };

    if (vkCreateRenderPass(device, &renderPassInfo, NULL, &logicalDevice->renderPass) != VK_SUCCESS)
    {
        J2dRlsTrace(J2D_TRACE_INFO, "Cannot create render pass for device")
        return JNI_FALSE;
    }

    // Create graphics pipeline
    VkShaderModule vertShaderModule = createShaderModule(device, blit_vert_data, sizeof (blit_vert_data));
    VkShaderModule fragShaderModule = createShaderModule(device, blit_frag_data, sizeof (blit_frag_data));

    VkPipelineShaderStageCreateInfo vertShaderStageInfo = {
            .sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO,
            .stage = VK_SHADER_STAGE_VERTEX_BIT,
            .module = vertShaderModule,
            .pName = "main"
    };

    VkPipelineShaderStageCreateInfo fragShaderStageInfo = {
            .sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO,
            .stage = VK_SHADER_STAGE_FRAGMENT_BIT,
            .module = fragShaderModule,
            .pName = "main"
    };

    VkPipelineShaderStageCreateInfo shaderStages[] = {vertShaderStageInfo, fragShaderStageInfo};

    VkPipelineVertexInputStateCreateInfo vertexInputInfo = {
            .sType = VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO,
            .vertexBindingDescriptionCount = 1,
            .vertexAttributeDescriptionCount = VKVertex_AttributeDescriptionsSize(),
            .pVertexBindingDescriptions = VKVertex_GetBindingDescription(),
            .pVertexAttributeDescriptions = VKVertex_GetAttributeDescriptions()
    };

    VkPipelineInputAssemblyStateCreateInfo inputAssembly = {
            .sType = VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO,
            .topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP,
            .primitiveRestartEnable = VK_FALSE
    };

    VkPipelineViewportStateCreateInfo viewportState = {
            .sType = VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO,
            .viewportCount = 1,
            .scissorCount = 1
    };

    VkPipelineRasterizationStateCreateInfo rasterizer = {
            .sType = VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO,
            .depthClampEnable = VK_FALSE,
            .rasterizerDiscardEnable = VK_FALSE,
            .polygonMode = VK_POLYGON_MODE_FILL,
            .lineWidth = 1.0f,
            .cullMode = VK_CULL_MODE_NONE,
            .depthBiasEnable = VK_FALSE,
            .depthBiasConstantFactor = 0.0f,
            .depthBiasClamp = 0.0f,
            .depthBiasSlopeFactor = 0.0f
    };

    VkPipelineMultisampleStateCreateInfo multisampling = {
            .sType = VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO,
            .sampleShadingEnable = VK_FALSE,
            .rasterizationSamples = VK_SAMPLE_COUNT_1_BIT
    };

    VkPipelineColorBlendAttachmentState colorBlendAttachment = {
            .colorWriteMask = VK_COLOR_COMPONENT_R_BIT |
                              VK_COLOR_COMPONENT_G_BIT |
                              VK_COLOR_COMPONENT_B_BIT |
                              VK_COLOR_COMPONENT_A_BIT,
            .blendEnable = VK_FALSE
    };

    VkPipelineColorBlendStateCreateInfo colorBlending = {
            .sType = VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO,
            .logicOpEnable = VK_FALSE,
            .logicOp = VK_LOGIC_OP_COPY,
            .attachmentCount = 1,
            .pAttachments = &colorBlendAttachment,
            .blendConstants[0] = 0.0f,
            .blendConstants[1] = 0.0f,
            .blendConstants[2] = 0.0f,
            .blendConstants[3] = 0.0f
    };

    VkDynamicState dynamicStates[] = {
            VK_DYNAMIC_STATE_VIEWPORT,
            VK_DYNAMIC_STATE_SCISSOR
    };

    VkPipelineDynamicStateCreateInfo dynamicState = {
            .sType = VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO,
            .dynamicStateCount = 2,
            .pDynamicStates = dynamicStates
    };

    VkDescriptorSetLayoutBinding samplerLayoutBinding = {
            .binding = 0,
            .descriptorCount = 1,
            .descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
            .pImmutableSamplers = NULL,
            .stageFlags = VK_SHADER_STAGE_FRAGMENT_BIT
    };

    VkDescriptorSetLayoutCreateInfo layoutInfo = {
            .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO,
            .bindingCount = 1,
            .pBindings = &samplerLayoutBinding
    };

    if (geInstance->vkCreateDescriptorSetLayout(device, &layoutInfo, NULL, &logicalDevice->descriptorSetLayout) != VK_SUCCESS) {
        J2dRlsTrace(J2D_TRACE_INFO,  "failed to create descriptor set layout!");
        return JNI_FALSE;
    }

    VkPipelineLayoutCreateInfo pipelineLayoutInfo = {
            .sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO,
            .setLayoutCount = 1,
            .pSetLayouts = &logicalDevice->descriptorSetLayout,
            .pushConstantRangeCount = 0
    };

    if (geInstance->vkCreatePipelineLayout(device, &pipelineLayoutInfo, NULL, &logicalDevice->pipelineLayout) != VK_SUCCESS) {
        J2dRlsTrace(J2D_TRACE_INFO, "failed to create pipeline layout!\n")
        return JNI_FALSE;
    }

    VkGraphicsPipelineCreateInfo pipelineInfo = {
            .sType = VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO,
            .stageCount = 2,
            .pStages = shaderStages,
            .pVertexInputState = &vertexInputInfo,
            .pInputAssemblyState = &inputAssembly,
            .pViewportState = &viewportState,
            .pRasterizationState = &rasterizer,
            .pMultisampleState = &multisampling,
            .pColorBlendState = &colorBlending,
            .pDynamicState = &dynamicState,
            .layout = logicalDevice->pipelineLayout,
            .renderPass = logicalDevice->renderPass,
            .subpass = 0,
            .basePipelineHandle = VK_NULL_HANDLE,
            .basePipelineIndex = -1
    };

    if (geInstance->vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, 1, &pipelineInfo, NULL,
                                              &logicalDevice->graphicsPipeline) != VK_SUCCESS)
    {
        J2dRlsTrace(J2D_TRACE_INFO, "failed to create graphics pipeline!\n")
        return JNI_FALSE;
    }
    geInstance->vkDestroyShaderModule(device, fragShaderModule, NULL);
    geInstance->vkDestroyShaderModule(device, vertShaderModule, NULL);

    // Create command pull
    VkCommandPoolCreateInfo poolInfo = {
            .sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO,
            .flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT,
            .queueFamilyIndex = logicalDevice->queueFamily
    };
    if (geInstance->vkCreateCommandPool(device, &poolInfo, NULL, &logicalDevice->commandPool) != VK_SUCCESS) {
        J2dRlsTraceLn(J2D_TRACE_INFO, "failed to create command pool!")
        return JNI_FALSE;
    }

    // Create command buffer
    VkCommandBufferAllocateInfo allocInfo = {
            .sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO,
            .commandPool = logicalDevice->commandPool,
            .level = VK_COMMAND_BUFFER_LEVEL_PRIMARY,
            .commandBufferCount = 1
    };

    if (geInstance->vkAllocateCommandBuffers(device, &allocInfo, &logicalDevice->commandBuffer) != VK_SUCCESS) {
        J2dRlsTraceLn(J2D_TRACE_INFO, "failed to allocate command buffers!");
        return JNI_FALSE;
    }

    // Create semaphores
    VkSemaphoreCreateInfo semaphoreInfo = {
            .sType = VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO
    };

    VkFenceCreateInfo fenceInfo = {
            .sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO,
            .flags = VK_FENCE_CREATE_SIGNALED_BIT
    };

    if (geInstance->vkCreateSemaphore(device, &semaphoreInfo, NULL, &logicalDevice->imageAvailableSemaphore) != VK_SUCCESS ||
        geInstance->vkCreateSemaphore(device, &semaphoreInfo, NULL, &logicalDevice->renderFinishedSemaphore) != VK_SUCCESS ||
        geInstance->vkCreateFence(device, &fenceInfo, NULL, &logicalDevice->inFlightFence) != VK_SUCCESS)
    {
        J2dRlsTraceLn(J2D_TRACE_INFO, "failed to create semaphores!");
        return JNI_FALSE;
    }

    geInstance->vkGetDeviceQueue(device, logicalDevice->queueFamily, 0, &logicalDevice->queue);
    if (logicalDevice->queue == NULL) {
        J2dRlsTraceLn(J2D_TRACE_INFO, "failed to get device queue!");
        return JNI_FALSE;
    }

    VkSamplerCreateInfo samplerInfo = {
            .sType = VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO,
            .magFilter = VK_FILTER_LINEAR,
            .minFilter = VK_FILTER_LINEAR,

            .addressModeU = VK_SAMPLER_ADDRESS_MODE_REPEAT,
            .addressModeV = VK_SAMPLER_ADDRESS_MODE_REPEAT,
            .addressModeW = VK_SAMPLER_ADDRESS_MODE_REPEAT,

            .anisotropyEnable = VK_FALSE,
            .maxAnisotropy = 1.0f,

            .compareEnable = VK_FALSE,
            .compareOp = VK_COMPARE_OP_ALWAYS,
            .mipmapMode = VK_SAMPLER_MIPMAP_MODE_LINEAR,
            .mipLodBias = 0.0f,
            .minLod = 0.0f,
            .maxLod = 0.0f
    };

    if (geInstance->vkCreateSampler(device, &samplerInfo, NULL, &logicalDevice->textureSampler) != VK_SUCCESS) {
        J2dRlsTraceLn(J2D_TRACE_INFO, "failed to create texture sampler!");
        return JNI_FALSE;
    }

    VkDescriptorPoolSize poolSize = {
            .type = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
            .descriptorCount = 1
    };

    VkDescriptorPoolCreateInfo descrPoolInfo = {
            .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO,
            .poolSizeCount = 1,
            .pPoolSizes = &poolSize,
            .maxSets = 1
    };

    if (geInstance->vkCreateDescriptorPool(device, &descrPoolInfo, NULL, &logicalDevice->descriptorPool) != VK_SUCCESS) {
        J2dRlsTraceLn(J2D_TRACE_INFO, "failed to create descriptor pool!")
        return JNI_FALSE;
    }

    VkDescriptorSetAllocateInfo descrAllocInfo = {
            .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO,
            .descriptorPool = logicalDevice->descriptorPool,
            .descriptorSetCount = 1,
            .pSetLayouts = &logicalDevice->descriptorSetLayout
    };

    if (geInstance->vkAllocateDescriptorSets(device, &descrAllocInfo, &logicalDevice->descriptorSets) != VK_SUCCESS) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "failed to allocate descriptor sets!");
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

 VkResult VK_FindMemoryType(VkPhysicalDevice physicalDevice, uint32_t typeFilter,
                                  VkMemoryPropertyFlags properties, uint32_t* pMemoryType) {
    VkPhysicalDeviceMemoryProperties memProperties;
    VKGraphicsEnvironment* ge = VKGE_graphics_environment();
    ge->vkGetPhysicalDeviceMemoryProperties(physicalDevice, &memProperties);

    for (uint32_t i = 0; i < memProperties.memoryTypeCount; i++) {
        if ((typeFilter & (1 << i)) && (memProperties.memoryTypes[i].propertyFlags & properties) == properties) {
            *pMemoryType = i;
            return VK_SUCCESS;
        }
    }

     return VK_ERROR_UNKNOWN;
}

VkResult VK_CreateBuffer(VkDeviceSize size, VkBufferUsageFlags usage,
                         VkMemoryPropertyFlags properties,
                         VkBuffer* buffer, VkDeviceMemory* bufferMemory)
{
    VKGraphicsEnvironment* ge = VKGE_graphics_environment();
    VKLogicalDevice* logicalDevice = &ge->devices[ge->enabledDeviceNum];

    VkBufferCreateInfo bufferInfo = {
            .sType = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO,
            .size = size,
            .usage = usage,
            .sharingMode = VK_SHARING_MODE_EXCLUSIVE
    };

    if (ge->vkCreateBuffer(logicalDevice->device, &bufferInfo, NULL, buffer) != VK_SUCCESS) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "failed to allocate descriptor sets!")
        return VK_ERROR_UNKNOWN;
    }


    VkMemoryRequirements memRequirements;
    ge->vkGetBufferMemoryRequirements(logicalDevice->device, *buffer, &memRequirements);

    uint32_t memoryType;

    if (VK_FindMemoryType(logicalDevice->physicalDevice,
                          memRequirements.memoryTypeBits,
                          properties, &memoryType) != VK_SUCCESS)
    {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "failed to find memory!")
        return VK_ERROR_UNKNOWN;
    }

    VkMemoryAllocateInfo allocInfo = {
            .sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO,
            .allocationSize = memRequirements.size,
            .memoryTypeIndex = memoryType
    };

    if (ge->vkAllocateMemory(logicalDevice->device, &allocInfo, NULL, bufferMemory) != VK_SUCCESS) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "failed to allocate buffer memory!");
        return VK_ERROR_UNKNOWN;
    }

    if (ge->vkBindBufferMemory(logicalDevice->device, *buffer, *bufferMemory, 0) != VK_SUCCESS) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "failed to bind buffer memory!");
        return VK_ERROR_UNKNOWN;
    }
    return VK_SUCCESS;
}

VkResult VK_CreateImage(uint32_t width, uint32_t height,
                        VkFormat format, VkImageTiling tiling,
                        VkImageUsageFlags usage,
                        VkMemoryPropertyFlags properties,
                        VkImage* image, VkDeviceMemory* imageMemory)
{
    VKGraphicsEnvironment* ge = VKGE_graphics_environment();
    VKLogicalDevice* logicalDevice = &ge->devices[ge->enabledDeviceNum];

    VkImageCreateInfo imageInfo = {
            .sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO,
            .imageType = VK_IMAGE_TYPE_2D,
            .extent.width = width,
            .extent.height = height,
            .extent.depth = 1,
            .mipLevels = 1,
            .arrayLayers = 1,
            .format = format,
            .tiling = tiling,
            .initialLayout = VK_IMAGE_LAYOUT_UNDEFINED,
            .usage = usage,
            .samples = VK_SAMPLE_COUNT_1_BIT,
            .sharingMode = VK_SHARING_MODE_EXCLUSIVE
    };

    if (ge->vkCreateImage(logicalDevice->device, &imageInfo, NULL, image) != VK_SUCCESS) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "Cannot create surface image");
        return VK_ERROR_UNKNOWN;
    }

    VkMemoryRequirements memRequirements;
    ge->vkGetImageMemoryRequirements(logicalDevice->device, *image, &memRequirements);

    uint32_t memoryType;
    if (VK_FindMemoryType(logicalDevice->physicalDevice,
                          memRequirements.memoryTypeBits,
                          properties, &memoryType) != VK_SUCCESS)
    {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "Failed to find memory")
        return VK_ERROR_UNKNOWN;
    }

    VkMemoryAllocateInfo allocInfo = {
            .sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO,
            .allocationSize = memRequirements.size,
            .memoryTypeIndex = memoryType
    };

    if (ge->vkAllocateMemory(logicalDevice->device, &allocInfo, NULL, imageMemory) != VK_SUCCESS) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "Failed to allocate image memory");
        return VK_ERROR_UNKNOWN;
    }

    ge->vkBindImageMemory(logicalDevice->device, *image, *imageMemory, 0);
    return VK_SUCCESS;
}

JNIEXPORT void JNICALL JNI_OnUnload(__attribute__((unused)) JavaVM *vm, __attribute__((unused)) void *reserved) {
    vulkanLibClose();
}

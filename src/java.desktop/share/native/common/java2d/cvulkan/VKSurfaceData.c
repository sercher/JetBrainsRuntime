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

#ifndef HEADLESS

#include <stdlib.h>

#include "jlong.h"
#include "jni_util.h"
#include "SurfaceData.h"
#include "VKSurfaceData.h"
#include <Trace.h>

/**
 * These are shorthand names for the surface type constants defined in
 * VKSurfaceData.java.
 */
#define VKSD_UNDEFINED       sun_java2d_pipe_hw_AccelSurface_UNDEFINED
#define VKSD_WINDOW          sun_java2d_pipe_hw_AccelSurface_WINDOW
#define VKSD_TEXTURE         sun_java2d_pipe_hw_AccelSurface_TEXTURE
#define VKSD_RT_TEXTURE      sun_java2d_pipe_hw_AccelSurface_RT_TEXTURE

void VKSD_InitWindowSurface(VKWinSDOps *vkwinsdo) {
    VKGraphicsEnvironment* ge = VKGE_graphics_environment();
    VKLogicalDevice* logicalDevice = &ge->devices[ge->enabledDeviceNum];
    VkPhysicalDevice physicalDevice = logicalDevice->physicalDevice;

    if (vkwinsdo->swapchainKhr == VK_NULL_HANDLE) {
        ge->vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, vkwinsdo->surface, &vkwinsdo->capabilitiesKhr);
        ge->vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, vkwinsdo->surface, &vkwinsdo->formatsKhrCount, NULL);
        if (vkwinsdo->formatsKhrCount == 0) {
            J2dRlsTrace(J2D_TRACE_ERROR, "No formats for swapchain found\n");
            return;
        }
        vkwinsdo->formatsKhr = calloc(vkwinsdo->formatsKhrCount, sizeof(VkSurfaceFormatKHR));
        ge->vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, vkwinsdo->surface, &vkwinsdo->formatsKhrCount,
                                                 vkwinsdo->formatsKhr);

        ge->vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, vkwinsdo->surface,
                                                      &vkwinsdo->presentModeKhrCount, NULL);

        if (vkwinsdo->presentModeKhrCount == 0) {
            J2dRlsTrace(J2D_TRACE_ERROR, "No present modes found\n");
            return;
        }

        vkwinsdo->presentModesKhr = calloc(vkwinsdo->presentModeKhrCount, sizeof(VkPresentModeKHR));
        ge->vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, vkwinsdo->surface, &vkwinsdo->presentModeKhrCount,
                                                      vkwinsdo->presentModesKhr);

        VkExtent2D extent = {
                (uint32_t) (vkwinsdo->vksdOps.width),
                (uint32_t) (vkwinsdo->vksdOps.height)
        };

        uint32_t imageCount = vkwinsdo->capabilitiesKhr.minImageCount + 1;
        if (vkwinsdo->capabilitiesKhr.maxImageCount > 0 && imageCount > vkwinsdo->capabilitiesKhr.maxImageCount) {
            imageCount = vkwinsdo->capabilitiesKhr.maxImageCount;
        }
        VkSwapchainCreateInfoKHR createInfoKhr = {};
        createInfoKhr.sType = VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
        createInfoKhr.surface = vkwinsdo->surface;
        createInfoKhr.minImageCount = imageCount;
        createInfoKhr.imageFormat = vkwinsdo->formatsKhr[0].format;
        createInfoKhr.imageColorSpace = vkwinsdo->formatsKhr[0].colorSpace;
        createInfoKhr.imageExtent = extent;
        createInfoKhr.imageArrayLayers = 1;
        createInfoKhr.imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
        createInfoKhr.imageSharingMode = VK_SHARING_MODE_EXCLUSIVE;
        createInfoKhr.queueFamilyIndexCount = 0;
        createInfoKhr.pQueueFamilyIndices = NULL;

        createInfoKhr.preTransform = vkwinsdo->capabilitiesKhr.currentTransform;
        createInfoKhr.compositeAlpha = VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
        createInfoKhr.presentMode = VK_PRESENT_MODE_FIFO_KHR;
        createInfoKhr.clipped = VK_TRUE;

        if (ge->vkCreateSwapchainKHR(logicalDevice->device, &createInfoKhr, NULL, &vkwinsdo->swapchainKhr) != VK_SUCCESS) {
            J2dRlsTrace(J2D_TRACE_ERROR, "Cannot create swapchain\n");
            return;
        }

        if (ge->vkGetSwapchainImagesKHR(logicalDevice->device, vkwinsdo->swapchainKhr, &vkwinsdo->swapChainImagesCount,
                                        NULL) != VK_SUCCESS) {
            J2dRlsTrace(J2D_TRACE_ERROR, "Cannot get swapchain images\n");
            return;
        }

        if (vkwinsdo->swapChainImagesCount == 0) {
            J2dRlsTrace(J2D_TRACE_ERROR, "No swapchain images found\n");
            return;
        }
        vkwinsdo->swapChainImages = calloc(vkwinsdo->swapChainImagesCount, sizeof(VkImage));
        if (ge->vkGetSwapchainImagesKHR(logicalDevice->device, vkwinsdo->swapchainKhr, &vkwinsdo->swapChainImagesCount,
                                        vkwinsdo->swapChainImages) != VK_SUCCESS) {
            J2dRlsTrace(J2D_TRACE_ERROR, "Cannot get swapchain images\n");
            return;
        }
        vkwinsdo->swapChainImageFormat = vkwinsdo->formatsKhr[0].format;
        vkwinsdo->swapChainExtent = extent;

// Create image views
        vkwinsdo->swapChainImageViewsCount = vkwinsdo->swapChainImagesCount;
        vkwinsdo->swapChainImageViews = calloc(vkwinsdo->swapChainImageViewsCount, sizeof(VkImageView));
        for (uint32_t i = 0; i < vkwinsdo->swapChainImagesCount; i++) {
            VkImageViewCreateInfo createInfo = {};
            createInfo.sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
            createInfo.image = vkwinsdo->swapChainImages[i];
            createInfo.viewType = VK_IMAGE_VIEW_TYPE_2D;
            createInfo.format = vkwinsdo->swapChainImageFormat;
            createInfo.components.r = VK_COMPONENT_SWIZZLE_IDENTITY;
            createInfo.components.g = VK_COMPONENT_SWIZZLE_IDENTITY;
            createInfo.components.b = VK_COMPONENT_SWIZZLE_IDENTITY;
            createInfo.components.a = VK_COMPONENT_SWIZZLE_IDENTITY;
            createInfo.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
            createInfo.subresourceRange.baseMipLevel = 0;
            createInfo.subresourceRange.levelCount = 1;
            createInfo.subresourceRange.baseArrayLayer = 0;
            createInfo.subresourceRange.layerCount = 1;
            if (ge->vkCreateImageView(logicalDevice->device, &createInfo, NULL, &vkwinsdo->swapChainImageViews[i]) !=
                VK_SUCCESS) {
                J2dRlsTrace(J2D_TRACE_ERROR, "Cannot get swapchain images\n");
                return;
            }
        }
        // Create frame buffers
        vkwinsdo->swapChainFramebuffersCount = vkwinsdo->swapChainImageViewsCount;
        vkwinsdo->swapChainFramebuffers = calloc(vkwinsdo->swapChainFramebuffersCount, sizeof(VkFramebuffer));
        for (size_t i = 0; i < vkwinsdo->swapChainFramebuffersCount; i++) {
            VkImageView attachments[] = {
                    vkwinsdo->swapChainImageViews[i]
            };

            VkFramebufferCreateInfo framebufferInfo = {};
            framebufferInfo.sType = VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO;
            framebufferInfo.renderPass = logicalDevice->renderPass;
            framebufferInfo.attachmentCount = 1;
            framebufferInfo.pAttachments = attachments;
            framebufferInfo.width = vkwinsdo->swapChainExtent.width;
            framebufferInfo.height = vkwinsdo->swapChainExtent.height;
            framebufferInfo.layers = 1;

            if (ge->vkCreateFramebuffer(logicalDevice->device, &framebufferInfo, NULL,
                                        &vkwinsdo->swapChainFramebuffers[i]) != VK_SUCCESS)
            {
                J2dRlsTraceLn(J2D_TRACE_ERROR, "failed to create framebuffer!");
                return;
            }
        }
    }
}

#endif /* !HEADLESS */

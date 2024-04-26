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

#include "jlong.h"
#include "SurfaceData.h"
#include "VKSurfaceData.h"
#include "VKVertex.h"
#include <Trace.h>

void VKSD_InitImageSurface(VKSDOps *vksdo) {
    if (vksdo->image != VK_NULL_HANDLE) {
        return;
    }

    VKGraphicsEnvironment* ge = VKGE_graphics_environment();
    VKLogicalDevice* logicalDevice = &ge->devices[ge->enabledDeviceNum];

    if (VK_CreateImage(vksdo->width, vksdo->height, VK_FORMAT_R8G8B8A8_UNORM, VK_IMAGE_TILING_LINEAR,
                VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                &vksdo->image, &vksdo->imageMemory) != VK_SUCCESS)
    {
        J2dRlsTrace(J2D_TRACE_ERROR, "Cannot create image\n");
        return;
    }

    VkImageViewCreateInfo viewInfo = {
            .sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO,
            .image = vksdo->image,
            .viewType = VK_IMAGE_VIEW_TYPE_2D,
            .format = VK_FORMAT_R8G8B8A8_UNORM,
            .subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT,
            .subresourceRange.baseMipLevel = 0,
            .subresourceRange.levelCount = 1,
            .subresourceRange.baseArrayLayer = 0,
            .subresourceRange.layerCount = 1
    };

    if (ge->vkCreateImageView(logicalDevice->device, &viewInfo, NULL, &vksdo->imageView) != VK_SUCCESS) {
        J2dRlsTrace(J2D_TRACE_ERROR, "Cannot surface image view\n");
        return;
    }

    VkDescriptorImageInfo imageInfo = {
            .imageLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
            .imageView = vksdo->imageView,
            .sampler = logicalDevice->textureSampler
    };

    VkWriteDescriptorSet descriptorWrites = {
            .sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET,
            .dstSet = logicalDevice->descriptorSets,
            .dstBinding = 0,
            .dstArrayElement = 0,
            .descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
            .descriptorCount = 1,
            .pImageInfo = &imageInfo
    };

    ge->vkUpdateDescriptorSets(logicalDevice->device, 1, &descriptorWrites, 0, NULL);
    VKVertex* vertices = ARRAY_ALLOC(VKVertex, 4);
    ARRAY_PUSH_BACK(&vertices, ((VKVertex){-1.0f, -1.0f, 0.0f, 0.0f}));
    ARRAY_PUSH_BACK(&vertices, ((VKVertex){1.0f, -1.0f, 1.0f, 0.0f}));
    ARRAY_PUSH_BACK(&vertices, ((VKVertex){-1.0f, 1.0f, 0.0f, 1.0f}));
    ARRAY_PUSH_BACK(&vertices, ((VKVertex){1.0f, 1.0f, 1.0f, 1.0f}));

    if (VKVertex_CreateVertexBufferFromArray(vertices, &vksdo->blitVertexBuffer, &vksdo->blitVertexBufferMemory) != VK_SUCCESS) {
        J2dRlsTrace(J2D_TRACE_ERROR, "Cannot create vertex buffer\n")
        return;
    }
    ARRAY_FREE(vertices);
}

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
        VkSwapchainCreateInfoKHR createInfoKhr = {
                .sType = VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR,
                .surface = vkwinsdo->surface,
                .minImageCount = imageCount,
                .imageFormat = vkwinsdo->formatsKhr[0].format,
                .imageColorSpace = vkwinsdo->formatsKhr[0].colorSpace,
                .imageExtent = extent,
                .imageArrayLayers = 1,
                .imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
                .imageSharingMode = VK_SHARING_MODE_EXCLUSIVE,
                .queueFamilyIndexCount = 0,
                .pQueueFamilyIndices = NULL,
                .preTransform = vkwinsdo->capabilitiesKhr.currentTransform,
                .compositeAlpha = VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR,
                .presentMode = VK_PRESENT_MODE_FIFO_KHR,
                .clipped = VK_TRUE
        };

        if (ge->vkCreateSwapchainKHR(logicalDevice->device, &createInfoKhr, NULL, &vkwinsdo->swapchainKhr) != VK_SUCCESS) {
            J2dRlsTrace(J2D_TRACE_ERROR, "Cannot create swapchain\n");
            return;
        }

        uint32_t swapChainImagesCount;
        if (ge->vkGetSwapchainImagesKHR(logicalDevice->device, vkwinsdo->swapchainKhr, &swapChainImagesCount,
                                        NULL) != VK_SUCCESS) {
            J2dRlsTrace(J2D_TRACE_ERROR, "Cannot get swapchain images\n");
            return;
        }

        if (swapChainImagesCount == 0) {
            J2dRlsTrace(J2D_TRACE_ERROR, "No swapchain images found\n");
            return;
        }
        vkwinsdo->swapChainImages = ARRAY_ALLOC(VkImage, swapChainImagesCount);

        if (ge->vkGetSwapchainImagesKHR(logicalDevice->device, vkwinsdo->swapchainKhr, &swapChainImagesCount,
                                        vkwinsdo->swapChainImages) != VK_SUCCESS) {
            J2dRlsTrace(J2D_TRACE_ERROR, "Cannot get swapchain images\n");
            return;
        }
        ARRAY_SIZE(vkwinsdo->swapChainImages) = swapChainImagesCount;

        vkwinsdo->swapChainImageFormat = vkwinsdo->formatsKhr[0].format;
        vkwinsdo->swapChainExtent = extent;

// Create image views
        vkwinsdo->swapChainImageViews = ARRAY_ALLOC(VkImageView,  swapChainImagesCount);
        ARRAY_SIZE(vkwinsdo->swapChainImageViews) = swapChainImagesCount;
        for (uint32_t i = 0; i < swapChainImagesCount; i++) {
            VkImageViewCreateInfo createInfo = {
                    .sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO,
                    .image = vkwinsdo->swapChainImages[i],
                    .viewType = VK_IMAGE_VIEW_TYPE_2D,
                    .format = vkwinsdo->swapChainImageFormat,
                    .components.r = VK_COMPONENT_SWIZZLE_IDENTITY,
                    .components.g = VK_COMPONENT_SWIZZLE_IDENTITY,
                    .components.b = VK_COMPONENT_SWIZZLE_IDENTITY,
                    .components.a = VK_COMPONENT_SWIZZLE_IDENTITY,
                    .subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT,
                    .subresourceRange.baseMipLevel = 0,
                    .subresourceRange.levelCount = 1,
                    .subresourceRange.baseArrayLayer = 0,
                    .subresourceRange.layerCount = 1
            };
            if (ge->vkCreateImageView(logicalDevice->device, &createInfo, NULL, &vkwinsdo->swapChainImageViews[i]) !=
                VK_SUCCESS) {
                J2dRlsTrace(J2D_TRACE_ERROR, "Cannot get swapchain images\n");
                return;
            }
        }
        // Create frame buffers
        vkwinsdo->swapChainFramebuffers = ARRAY_ALLOC(VkFramebuffer, swapChainImagesCount);
        ARRAY_SIZE(vkwinsdo->swapChainFramebuffers) = swapChainImagesCount;
        for (size_t i = 0; i < swapChainImagesCount; i++) {
            VkImageView attachments[] = {
                    vkwinsdo->swapChainImageViews[i]
            };

            VkFramebufferCreateInfo framebufferInfo = {
                    .sType = VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO,
                    .renderPass = logicalDevice->renderPass,
                    .attachmentCount = 1,
                    .pAttachments = attachments,
                    .width = vkwinsdo->swapChainExtent.width,
                    .height = vkwinsdo->swapChainExtent.height,
                    .layers = 1
            };

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

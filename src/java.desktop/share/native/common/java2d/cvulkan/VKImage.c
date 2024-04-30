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

#include <Trace.h>
#include "VKBase.h"
#include "VKBuffer.h"
#include "VKImage.h"


VKImage* VKImage_Create(uint32_t width, uint32_t height,
                        VkFormat format, VkImageTiling tiling,
                        VkImageUsageFlags usage,
                        VkMemoryPropertyFlags properties)
{
    VKGraphicsEnvironment* ge = VKGE_graphics_environment();
    VKLogicalDevice* logicalDevice = &ge->devices[ge->enabledDeviceNum];
    VKImage* image = malloc(sizeof (VKImage));
    image->format = format;

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

    if (ge->vkCreateImage(logicalDevice->device, &imageInfo, NULL, &image->image) != VK_SUCCESS) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "Cannot create surface image");
        return NULL;
    }

    VkMemoryRequirements memRequirements;
    ge->vkGetImageMemoryRequirements(logicalDevice->device, image->image, &memRequirements);

    uint32_t memoryType;
    if (VKBuffer_FindMemoryType(logicalDevice->physicalDevice,
                                memRequirements.memoryTypeBits,
                                properties, &memoryType) != VK_SUCCESS)
    {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "Failed to find memory")
        return NULL;
    }

    VkMemoryAllocateInfo allocInfo = {
            .sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO,
            .allocationSize = memRequirements.size,
            .memoryTypeIndex = memoryType
    };

    if (ge->vkAllocateMemory(logicalDevice->device, &allocInfo, NULL, &image->memory) != VK_SUCCESS) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "Failed to allocate image memory");
        return NULL;
    }

    ge->vkBindImageMemory(logicalDevice->device, image->image, image->memory, 0);

    VkImageViewCreateInfo viewInfo = {
            .sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO,
            .image = image->image,
            .viewType = VK_IMAGE_VIEW_TYPE_2D,
            .format = VK_FORMAT_R8G8B8A8_UNORM,
            .subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT,
            .subresourceRange.baseMipLevel = 0,
            .subresourceRange.levelCount = 1,
            .subresourceRange.baseArrayLayer = 0,
            .subresourceRange.layerCount = 1
    };

    if (ge->vkCreateImageView(logicalDevice->device, &viewInfo, NULL, &image->view) != VK_SUCCESS) {
        J2dRlsTrace(J2D_TRACE_ERROR, "Cannot surface image view\n");
        return NULL;
    }

    return image;
}

void VKImage_free(VKImage* image) {
    VKGraphicsEnvironment* ge = VKGE_graphics_environment();
    VKLogicalDevice* logicalDevice = &ge->devices[ge->enabledDeviceNum];

    if (!image) return;

    if (image->view != VK_NULL_HANDLE) {
        ge->vkDestroyImageView(logicalDevice->device, image->view, NULL);
    }
    if (image->image != VK_NULL_HANDLE) {
        ge->vkDestroyImage(logicalDevice->device, image->image, NULL);
    }
    if (image->memory != VK_NULL_HANDLE) {
        ge->vkFreeMemory(logicalDevice->device, image->memory, NULL);
    }
    free(image);
}
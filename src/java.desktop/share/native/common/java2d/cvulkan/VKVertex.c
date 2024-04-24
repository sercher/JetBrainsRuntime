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

#include <string.h>
#include <Trace.h>
#include "CArrayUtil.h"
#include "VKVertex.h"
#include "VKBase.h"

static VkVertexInputAttributeDescription attributeDescriptions [] = {
        {
                .binding = 0,
                .location = 0,
                .format = VK_FORMAT_R32G32_SFLOAT,
                .offset = offsetof(VKVertex, px)
        },
        {
                .binding = 0,
                .location = 1,
                .format = VK_FORMAT_R32G32_SFLOAT,
                .offset = offsetof(VKVertex, u)
        }
};

VkVertexInputBindingDescription* VKVertex_GetBindingDescription() {
    static VkVertexInputBindingDescription bindingDescription = {
            .binding = 0,
            .stride = sizeof(VKVertex),
            .inputRate = VK_VERTEX_INPUT_RATE_VERTEX
    };
    return &bindingDescription;
}

VkVertexInputAttributeDescription* VKVertex_GetAttributeDescriptions() {
    return attributeDescriptions;
}

uint32_t VKVertex_AttributeDescriptionsSize() {
    return SARRAY_COUNT_OF(attributeDescriptions);
}

VkResult VKVertex_CreateVertexBufferFromArray(VKVertex* vertices, VkBuffer* pVertexBuffer, VkDeviceMemory* pVertexBufferMemory) {
    VKGraphicsEnvironment* ge = VKGE_graphics_environment();
    VKLogicalDevice* logicalDevice = &ge->devices[ge->enabledDeviceNum];

    VkDeviceSize bufferSize = ARRAY_SIZE(vertices)*sizeof (vertices[0]);

    VK_CreateBuffer(bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT | VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT |
                    VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pVertexBuffer, pVertexBufferMemory);

    void* data;
    if (ge->vkMapMemory(logicalDevice->device, *pVertexBufferMemory, 0, bufferSize, 0, &data) != VK_SUCCESS) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "failed to map memory!");
        return VK_ERROR_UNKNOWN;
    }
    memcpy(data, vertices, bufferSize);
    ge->vkUnmapMemory(logicalDevice->device, *pVertexBufferMemory);

    return VK_SUCCESS;
}

#endif /* !HEADLESS */
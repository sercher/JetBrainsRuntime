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

#include <jlong.h>
#include "Trace.h"
#include "VKVertex.h"
#include "VKRenderer.h"

#define INCLUDE_BYTECODE
#define SHADER_ENTRY(NAME, TYPE) static uint32_t NAME ## _ ## TYPE ## _data[] = {
#define BYTECODE_END };
#include "vulkan/shader_list.h"
#undef INCLUDE_BYTECODE
#undef SHADER_ENTRY
#undef BYTECODE_END


VkShaderModule createShaderModule(VkDevice device, uint32_t* shader, uint32_t sz) {
    VKGraphicsEnvironment* ge = VKGE_graphics_environment();
    VkShaderModuleCreateInfo createInfo = {};
    createInfo.sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
    createInfo.codeSize = sz;
    createInfo.pCode = (uint32_t*)shader;
    VkShaderModule shaderModule;
    if (ge->vkCreateShaderModule(device, &createInfo, NULL, &shaderModule) != VK_SUCCESS) {
        J2dRlsTrace(J2D_TRACE_ERROR, "failed to create shader module\n")
        return VK_NULL_HANDLE;
    }
    return shaderModule;
}

jboolean VKRenderer_CreateBlitFrameBufferRenderer() {
    VKGraphicsEnvironment* ge = VKGE_graphics_environment();
    VKLogicalDevice* logicalDevice = &ge->devices[ge->enabledDeviceNum];
    if (logicalDevice->blitFrameBufferRenderer != NULL) {
        return JNI_TRUE;
    }
    logicalDevice->blitFrameBufferRenderer = malloc(sizeof (VKRenderer ));

    VkDevice device = logicalDevice->device;
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

    if (ge->vkCreateRenderPass(logicalDevice->device, &renderPassInfo, NULL, &logicalDevice->blitFrameBufferRenderer->renderPass) != VK_SUCCESS)
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
    VKVertexDescr vertexDescr = VKVertex_GetTxVertexDescr();
    VkPipelineVertexInputStateCreateInfo vertexInputInfo = {
            .sType = VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO,
            .vertexBindingDescriptionCount = vertexDescr.bindingDescriptionCount,
            .vertexAttributeDescriptionCount = vertexDescr.attributeDescriptionCount,
            .pVertexBindingDescriptions = vertexDescr.bindingDescriptions,
            .pVertexAttributeDescriptions = vertexDescr.attributeDescriptions
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

    if (ge->vkCreateDescriptorSetLayout(device, &layoutInfo, NULL, &logicalDevice->blitFrameBufferRenderer->descriptorSetLayout) != VK_SUCCESS) {
        J2dRlsTrace(J2D_TRACE_INFO,  "failed to create descriptor set layout!");
        return JNI_FALSE;
    }

    VkPipelineLayoutCreateInfo pipelineLayoutInfo = {
            .sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO,
            .setLayoutCount = 1,
            .pSetLayouts = &logicalDevice->blitFrameBufferRenderer->descriptorSetLayout,
            .pushConstantRangeCount = 0
    };

    if (ge->vkCreatePipelineLayout(device, &pipelineLayoutInfo, NULL, &logicalDevice->blitFrameBufferRenderer->pipelineLayout) != VK_SUCCESS) {
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
            .layout = logicalDevice->blitFrameBufferRenderer->pipelineLayout,
            .renderPass = logicalDevice->blitFrameBufferRenderer->renderPass,
            .subpass = 0,
            .basePipelineHandle = VK_NULL_HANDLE,
            .basePipelineIndex = -1
    };

    if (ge->vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, 1, &pipelineInfo, NULL,
                                              &logicalDevice->blitFrameBufferRenderer->graphicsPipeline) != VK_SUCCESS)
    {
        J2dRlsTrace(J2D_TRACE_INFO, "failed to create graphics pipeline!\n")
        return JNI_FALSE;
    }
    ge->vkDestroyShaderModule(device, fragShaderModule, NULL);
    ge->vkDestroyShaderModule(device, vertShaderModule, NULL);

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

    if (ge->vkCreateSampler(device, &samplerInfo, NULL, &logicalDevice->textureSampler) != VK_SUCCESS) {
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

    if (ge->vkCreateDescriptorPool(device, &descrPoolInfo, NULL, &logicalDevice->blitFrameBufferRenderer->descriptorPool) != VK_SUCCESS) {
        J2dRlsTraceLn(J2D_TRACE_INFO, "failed to create descriptor pool!")
        return JNI_FALSE;
    }

    VkDescriptorSetAllocateInfo descrAllocInfo = {
            .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO,
            .descriptorPool = logicalDevice->blitFrameBufferRenderer->descriptorPool,
            .descriptorSetCount = 1,
            .pSetLayouts = &logicalDevice->blitFrameBufferRenderer->descriptorSetLayout
    };

    if (ge->vkAllocateDescriptorSets(device, &descrAllocInfo, &logicalDevice->blitFrameBufferRenderer->descriptorSets) != VK_SUCCESS) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "failed to allocate descriptor sets!");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

void VKRenderer_BlitFrameBuffer(VkFramebuffer frameBuffer,
                                VkBuffer vertexBuffer,
                                uint32_t vertexNum,
                                uint32_t width,
                                uint32_t height)
{
    VKGraphicsEnvironment* ge = VKGE_graphics_environment();
    VKLogicalDevice* logicalDevice = &ge->devices[ge->enabledDeviceNum];
    
    VkCommandBufferBeginInfo beginInfo = {};
    beginInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;

    if (ge->vkBeginCommandBuffer(logicalDevice->commandBuffer, &beginInfo) != VK_SUCCESS) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "failed to begin recording command buffer!");
        return;
    }

    VkClearValue clearColor = {{{0.0f, 0.0f, 0.0f, 1.0f}}};
    VkRenderPassBeginInfo renderPassInfo = {
            .sType = VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO,
            .renderPass = logicalDevice->blitFrameBufferRenderer->renderPass,
            .framebuffer = frameBuffer,
            .renderArea.offset = (VkOffset2D){0, 0},
            .renderArea.extent = (VkExtent2D){width, height},
            .clearValueCount = 1,
            .pClearValues = &clearColor
    };

    ge->vkCmdBeginRenderPass(logicalDevice->commandBuffer, &renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
    ge->vkCmdBindPipeline(logicalDevice->commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                          logicalDevice->blitFrameBufferRenderer->graphicsPipeline);

    VkBuffer vertexBuffers[] = {vertexBuffer};
    VkDeviceSize offsets[] = {0};
    ge->vkCmdBindVertexBuffers(logicalDevice->commandBuffer, 0, 1, vertexBuffers, offsets);
    VkViewport viewport = {
            .x = 0.0f,
            .y = 0.0f,
            .width = (float)width,
            .height = (float)height,
            .minDepth = 0.0f,
            .maxDepth = 1.0f
    };

    ge->vkCmdSetViewport(logicalDevice->commandBuffer, 0, 1, &viewport);

    VkRect2D scissor = {
            .offset = (VkOffset2D){0, 0},
            .extent = (VkExtent2D){width, height},
    };

    ge->vkCmdSetScissor(logicalDevice->commandBuffer, 0, 1, &scissor);
    ge->vkCmdBindDescriptorSets(logicalDevice->commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                logicalDevice->blitFrameBufferRenderer->pipelineLayout, 0, 1, &logicalDevice->blitFrameBufferRenderer->descriptorSets, 0, NULL);
    ge->vkCmdDraw(logicalDevice->commandBuffer, vertexNum, 1, 0, 0);

    ge->vkCmdEndRenderPass(logicalDevice->commandBuffer);

    if (ge->vkEndCommandBuffer(logicalDevice->commandBuffer) != VK_SUCCESS) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "failed to record command buffer!")
        return;
    }

    VkSemaphore waitSemaphores[] = {logicalDevice->imageAvailableSemaphore};
    VkPipelineStageFlags waitStages[] = {VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT};
    VkSemaphore signalSemaphores[] = {logicalDevice->renderFinishedSemaphore};

    VkSubmitInfo submitInfo = {
            .sType = VK_STRUCTURE_TYPE_SUBMIT_INFO,
            .waitSemaphoreCount = 1,
            .pWaitSemaphores = waitSemaphores,
            .pWaitDstStageMask = waitStages,
            .commandBufferCount = 1,
            .pCommandBuffers = &logicalDevice->commandBuffer,
            .signalSemaphoreCount = 1,
            .pSignalSemaphores = signalSemaphores
    };

    if (ge->vkQueueSubmit(logicalDevice->queue, 1, &submitInfo, logicalDevice->inFlightFence) != VK_SUCCESS) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,"failed to submit draw command buffer!")
        return;
    }
}

void
VKRenderer_FillRect(jint x, jint y, jint w, jint h)
{
    J2dTraceLn4(J2D_TRACE_INFO, "VKRenderer_FillRect %d %d %d %d", x, y, w, h);

    if (w <= 0 || h <= 0) {
        return;
    }
}

jboolean VK_CreateLogicalDeviceRenderers() {
    return VKRenderer_CreateBlitFrameBufferRenderer();
}
#endif /* !HEADLESS */

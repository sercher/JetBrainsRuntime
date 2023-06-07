/* Simple Plugin API */
/* SPDX-FileCopyrightText: Copyright © 2018 Wim Taymans */
/* SPDX-License-Identifier: MIT */

#ifndef SPA_VIDEO_MJPG_UTILS_H
#define SPA_VIDEO_MJPG_UTILS_H

#ifdef __cplusplus
extern "C" {
#endif

/**
 * \addtogroup spa_param
 * \{
 */

#include <spa/pod/parser.h>
#include <spa/pod/builder.h>
#include <spa/param/video/mjpg.h>

static inline int
spa_format_video_mjpg_parse(const struct spa_pod *format,
                struct spa_video_info_mjpg *info)
{
    return spa_pod_parse_object(format,
            SPA_TYPE_OBJECT_Format, NULL,
            SPA_FORMAT_VIDEO_size,        SPA_POD_OPT_Rectangle(&info->size),
            SPA_FORMAT_VIDEO_framerate,    SPA_POD_OPT_Fraction(&info->framerate),
            SPA_FORMAT_VIDEO_maxFramerate,    SPA_POD_OPT_Fraction(&info->max_framerate));
}

static inline struct spa_pod *
spa_format_video_mjpg_build(struct spa_pod_builder *builder, uint32_t id,
               struct spa_video_info_mjpg *info)
{
    struct spa_pod_frame f;
    spa_pod_builder_push_object(builder, &f, SPA_TYPE_OBJECT_Format, id);
    spa_pod_builder_add(builder,
            SPA_FORMAT_mediaType,        SPA_POD_Id(SPA_MEDIA_TYPE_video),
            SPA_FORMAT_mediaSubtype,    SPA_POD_Id(SPA_MEDIA_SUBTYPE_mjpg),
            0);
    if (info->size.width != 0 && info->size.height != 0)
        spa_pod_builder_add(builder,
            SPA_FORMAT_VIDEO_size,        SPA_POD_Rectangle(&info->size), 0);
    if (info->framerate.denom != 0)
        spa_pod_builder_add(builder,
            SPA_FORMAT_VIDEO_framerate,    SPA_POD_Fraction(&info->framerate), 0);
    if (info->max_framerate.denom != 0)
        spa_pod_builder_add(builder,
            SPA_FORMAT_VIDEO_maxFramerate,    SPA_POD_Fraction(info->max_framerate), 0);
    return (struct spa_pod*)spa_pod_builder_pop(builder, &f);
}

/**
 * \}
 */

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* SPA_VIDEO_MJPG_UTILS_H */

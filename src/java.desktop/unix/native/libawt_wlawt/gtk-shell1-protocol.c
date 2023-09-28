/* Generated by wayland-scanner 1.19.0 */

#include <stdlib.h>
#include <stdint.h>
#include "wayland-util.h"

#ifndef __has_attribute
# define __has_attribute(x) 0  /* Compatibility with non-clang compilers. */
#endif

#if (__has_attribute(visibility) || defined(__GNUC__) && __GNUC__ >= 4)
#define WL_PRIVATE __attribute__ ((visibility("hidden")))
#else
#define WL_PRIVATE
#endif

extern const struct wl_interface gtk_surface1_interface;
extern const struct wl_interface wl_seat_interface;
extern const struct wl_interface wl_surface_interface;

static const struct wl_interface *gtk_types[] = {
	NULL,
	NULL,
	NULL,
	NULL,
	NULL,
	NULL,
	&gtk_surface1_interface,
	&wl_surface_interface,
	&gtk_surface1_interface,
	NULL,
	&wl_seat_interface,
	NULL,
};

static const struct wl_message gtk_shell1_requests[] = {
	{ "get_gtk_surface", "no", gtk_types + 6 },
	{ "set_startup_id", "?s", gtk_types + 0 },
	{ "system_bell", "?o", gtk_types + 8 },
	{ "notify_launch", "3s", gtk_types + 0 },
};

static const struct wl_message gtk_shell1_events[] = {
	{ "capabilities", "u", gtk_types + 0 },
};

WL_PRIVATE const struct wl_interface gtk_shell1_interface = {
	"gtk_shell1", 5,
	4, gtk_shell1_requests,
	1, gtk_shell1_events,
};

static const struct wl_message gtk_surface1_requests[] = {
	{ "set_dbus_properties", "?s?s?s?s?s?s", gtk_types + 0 },
	{ "set_modal", "", gtk_types + 0 },
	{ "unset_modal", "", gtk_types + 0 },
	{ "present", "u", gtk_types + 0 },
	{ "request_focus", "3?s", gtk_types + 0 },
	{ "release", "4", gtk_types + 0 },
	{ "titlebar_gesture", "5uou", gtk_types + 9 },
};

static const struct wl_message gtk_surface1_events[] = {
	{ "configure", "a", gtk_types + 0 },
	{ "configure_edges", "2a", gtk_types + 0 },
};

WL_PRIVATE const struct wl_interface gtk_surface1_interface = {
	"gtk_surface1", 5,
	7, gtk_surface1_requests,
	2, gtk_surface1_events,
};


/* == Start of generated table == */
/*
 * The following tables are generated by running:
 *
 *   ./gen-emoji-table.py emoji-data.txt
 *
 * on file with this header:
 *
 * # emoji-data-14.0.0.txt
 * # Date: 2021-08-26, 17:22:22 GMT
 * # © 2021 Unicode®, Inc.
 * # Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in the U.S. and other countries.
 * # For terms of use, see http://www.unicode.org/terms_of_use.html
 * #
 * # Emoji Data for UTS #51
 * # Used with Emoji Version 14.0 and subsequent minor revisions (if any)
 * #
 * # For documentation and usage, see http://www.unicode.org/reports/tr51
 */

#ifndef HB_UNICODE_EMOJI_TABLE_HH
#define HB_UNICODE_EMOJI_TABLE_HH

#include "hb-unicode.hh"

static const uint8_t
_hb_emoji_u8[544] =
{
   16, 17, 17, 17, 50, 20, 21, 17, 17, 17, 17, 17, 17, 17, 17, 17,
   17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17,
   17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17,
   17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17,118,152,
    0,  0,  1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
    2,  3,  0,  0,  4,  0,  5,  0,  0,  0,  0,  0,  6,  0,  7,  8,
    0,  0,  0,  9,  0,  0, 10, 11, 12, 13, 14, 13, 15, 16, 17,  0,
    0,  0,  0,  0, 18,  0,  0,  0,  0,  0,  0,  0, 19, 20,  0,  0,
   21,  0,  0,  0,  0,  0,  0,  0,  0,  0, 22,  0,  0,  0,  0,  0,
   13, 13, 13, 13, 23, 24, 25, 26, 27, 28, 13, 13, 13, 13, 13, 29,
   13, 13, 13, 13, 30, 31, 13, 13, 13, 32, 13, 13,  0, 33,  0, 34,
   35, 36, 37, 13, 38, 39, 13, 13, 13, 13, 13, 13,  0,  0,  0,  0,
   13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 30,
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 66,  0,  0,
    0,  0,  0,  0,  0,  0,  0, 16,  0,  2,  0,  0,  0,  0,  0,  0,
    0,  0,  0,  0,  4,  0,  0,  2,  0,  0,240,  3,  0,  6,  0,  0,
    0,  0,  0, 12,  0,  1,  0,  0,  0,  1,  0,  0,  0,  0,  0,  0,
    0,128,  0,  0,  0,254, 15,  7,  4,  0,  0,  0,  0,  0,  0,  0,
    0,  0,  0,  0,  0, 12, 64,  0,  1,  0,  0,  0,  0,  0,  0,120,
  191,255,247,255,255,255,255,255,255,255,255,255,255,255,255,255,
   63,  0,255,255,255,255,255,255, 63,255, 87, 32,  2,  1, 24,  0,
  144, 80,184,  0,248,  0,  0,  0,  0,  0,224,  0,  2,  0,  1,128,
    0,  0,  0,  0,  0,  0, 48,  0,224,  0,  0, 24,  0,  0,  0,  0,
    0,  0, 33,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  1, 32,
    0,  0,128,  2,  0,  0,  0,  0,  0,224,  0,  0,  0,128,  0,  0,
    0,  0,  0,  0,  0,240,  3,192,  0, 64,254,  7,  0,224,255,255,
  255,255,255,255, 63,  0,  0,  0,254,255,  0,  4,  0,128,252,247,
    0,254,255,255,255,255,255,255,255,255,255,255,255,255,255,  7,
  255,255,255,255,255,255,255, 63,192,255,255,255,255,255,255,255,
  255,255,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,240,255,
    0,  0,224,255,255,255,255,255,  0,240,  0,  0,  0,  0,  0,  0,
    0,255,  0,252,  0,  0,  0,  0,  0,255,  0,  0,  0,192,255,255,
    0,240,255,255,255,255,255,247,191,255,255,255,255,255,255,255,
};

static inline unsigned
_hb_emoji_b4 (const uint8_t* a, unsigned i)
{
  return (a[i>>1]>>((i&1u)<<2))&15u;
}
static inline unsigned
_hb_emoji_b1 (const uint8_t* a, unsigned i)
{
  return (a[i>>3]>>((i&7u)<<0))&1u;
}
static inline uint_fast8_t
_hb_emoji_is_Extended_Pictographic (unsigned u)
{
  return u<131070u?_hb_emoji_b1(224+_hb_emoji_u8,((_hb_emoji_u8[64+(((_hb_emoji_b4(_hb_emoji_u8,u>>6>>4))<<4)+((u>>6)&15u))])<<6)+((u)&63u)):0;
}


#endif /* HB_UNICODE_EMOJI_TABLE_HH */

/* == End of generated table == */

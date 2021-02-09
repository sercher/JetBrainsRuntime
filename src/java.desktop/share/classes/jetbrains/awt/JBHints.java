/*
 * Copyright 2000-2021 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.awt;

import java.awt.*;

public class JBHints {

    public static final RenderingHints.Key SUBPIXEL_GLYPH_RESOLUTION = new RenderingHints.Key(0) {
        @Override
        public boolean isCompatibleValue(Object val) {
            if (val instanceof Dimension) {
                Dimension resolution = (Dimension) val;
                /* Subpixel resolution must be in [1; 16] range, as both
                 * x and y resolution are treated as a single byte internally */
                return resolution.width >= 1 && resolution.width <= 16 &&
                        resolution.height >= 1 && resolution.height <= 16;
            }
            return false;
        }
    };
}

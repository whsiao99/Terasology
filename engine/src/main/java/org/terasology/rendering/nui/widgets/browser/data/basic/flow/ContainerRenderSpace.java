/*
 * Copyright 2015 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.nui.widgets.browser.data.basic.flow;

import org.joml.Rectanglei;
import org.terasology.rendering.nui.widgets.browser.ui.style.ParagraphRenderStyle;

public interface ContainerRenderSpace {
    int getContainerWidth();

    int getNextWidthChange(int y);

    Rectanglei addLeftFloat(int y, int width, int height);

    Rectanglei addRightFloat(int y, int width, int height);

    int getNextClearYPosition(ParagraphRenderStyle.ClearStyle clearStyle);

    int getWidthForVerticalPosition(int y);

    int getAdvanceForVerticalPosition(int y);
}
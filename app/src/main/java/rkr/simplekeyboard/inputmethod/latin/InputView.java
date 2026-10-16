/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2018 Raimondas Rimkus
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

package rkr.simplekeyboard.inputmethod.latin;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public final class InputView extends FrameLayout {
    public InputView(final Context context, final AttributeSet attrs) {
        super(context, attrs, 0);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        final int screenWidth = getResources().getDisplayMetrics().widthPixels;
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(screenWidth, MeasureSpec.EXACTLY),
                heightMeasureSpec);
        setMeasuredDimension(screenWidth, getMeasuredHeight());
    }

    @Override
    protected void onLayout(final boolean changed, final int left, final int top,
            final int right, final int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        final int parentWidth = right - left;
        final int parentHeight = bottom - top;
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            final int childWidth = child.getMeasuredWidth();
            final int childHeight = child.getMeasuredHeight();
            int bottomMargin = 0;
            if (child.getLayoutParams() instanceof MarginLayoutParams) {
                bottomMargin = ((MarginLayoutParams) child.getLayoutParams()).bottomMargin;
            }
            final int childLeft = Math.max(0, (parentWidth - childWidth) / 2);
            final int childTop = parentHeight - childHeight - bottomMargin;
            child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        }
    }
}

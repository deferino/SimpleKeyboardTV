/*
 * Copyright (C) 2026 Simple Keyboard TV Contributors
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

package rkr.simplekeyboard.inputmethod.keyboard;

import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Spatial 2D row/column navigator for Android TV D-pad remote controls.
 */
public final class DpadNavigator {

    private DpadNavigator() {
        // Utility class.
    }

    public static Key findDefaultKey(final Keyboard keyboard) {
        if (keyboard == null) {
            return null;
        }
        final List<List<Key>> rows = buildRows(keyboard);
        if (rows.isEmpty() || rows.get(0).isEmpty()) {
            return null;
        }
        return rows.get(0).get(0);
    }

    public static Key findClosestKey(final Keyboard keyboard, final int targetX, final int targetY) {
        if (keyboard == null) {
            return null;
        }
        Key bestKey = null;
        long bestDistSq = Long.MAX_VALUE;
        for (final Key key : keyboard.getSortedKeys()) {
            if (key.isSpacer()) {
                continue;
            }
            final long cx = key.getX() + key.getWidth() / 2;
            final long cy = key.getY() + key.getHeight() / 2;
            final long dx = cx - targetX;
            final long dy = cy - targetY;
            final long distSq = dx * dx + dy * dy;
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestKey = key;
            }
        }
        return bestKey;
    }

    public static Key findNextKey(final Keyboard keyboard, final Key currentKey,
            final int keyCode, final int preferredColumnX) {
        if (keyboard == null) {
            return null;
        }
        final List<List<Key>> rows = buildRows(keyboard);
        if (rows.isEmpty()) {
            return null;
        }
        if (currentKey == null) {
            return rows.get(0).get(0);
        }

        int currentRowIdx = -1;
        int currentColIdx = -1;
        for (int r = 0; r < rows.size(); r++) {
            final List<Key> row = rows.get(r);
            final int c = row.indexOf(currentKey);
            if (c >= 0) {
                currentRowIdx = r;
                currentColIdx = c;
                break;
            }
        }

        if (currentRowIdx < 0) {
            final int cx = currentKey.getX() + currentKey.getWidth() / 2;
            final int cy = currentKey.getY() + currentKey.getHeight() / 2;
            final Key fallback = findClosestKey(keyboard, cx, cy);
            return fallback != null ? fallback : rows.get(0).get(0);
        }

        final List<Key> currentRow = rows.get(currentRowIdx);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT: {
                final int nextCol = (currentColIdx - 1 + currentRow.size()) % currentRow.size();
                return currentRow.get(nextCol);
            }
            case KeyEvent.KEYCODE_DPAD_RIGHT: {
                final int nextCol = (currentColIdx + 1) % currentRow.size();
                return currentRow.get(nextCol);
            }
            case KeyEvent.KEYCODE_DPAD_UP: {
                final int nextRowIdx = (currentRowIdx - 1 + rows.size()) % rows.size();
                final int targetX = preferredColumnX >= 0
                        ? preferredColumnX
                        : currentKey.getX() + currentKey.getWidth() / 2;
                return findBestKeyInRow(rows.get(nextRowIdx), targetX);
            }
            case KeyEvent.KEYCODE_DPAD_DOWN: {
                final int nextRowIdx = (currentRowIdx + 1) % rows.size();
                final int targetX = preferredColumnX >= 0
                        ? preferredColumnX
                        : currentKey.getX() + currentKey.getWidth() / 2;
                return findBestKeyInRow(rows.get(nextRowIdx), targetX);
            }
            default:
                return currentKey;
        }
    }

    private static Key findBestKeyInRow(final List<Key> row, final int targetX) {
        Key bestKey = row.get(0);
        int bestSpanDist = Integer.MAX_VALUE;
        int bestCenterDist = Integer.MAX_VALUE;
        for (final Key key : row) {
            final int left = key.getX();
            final int right = left + key.getWidth();
            final int spanDist;
            if (targetX < left) {
                spanDist = left - targetX;
            } else if (targetX > right) {
                spanDist = targetX - right;
            } else {
                spanDist = 0;
            }
            final int center = left + key.getWidth() / 2;
            final int centerDist = Math.abs(center - targetX);
            if (spanDist < bestSpanDist
                    || (spanDist == bestSpanDist && centerDist < bestCenterDist)) {
                bestSpanDist = spanDist;
                bestCenterDist = centerDist;
                bestKey = key;
            }
        }
        return bestKey;
    }

    private static List<List<Key>> buildRows(final Keyboard keyboard) {
        final List<Key> validKeys = new ArrayList<>();
        for (final Key key : keyboard.getSortedKeys()) {
            if (!key.isSpacer()) {
                validKeys.add(key);
            }
        }
        Collections.sort(validKeys, new Comparator<Key>() {
            @Override
            public int compare(final Key a, final Key b) {
                final int cyA = a.getY() + a.getHeight() / 2;
                final int cyB = b.getY() + b.getHeight() / 2;
                if (cyA != cyB) {
                    return Integer.compare(cyA, cyB);
                }
                final int cxA = a.getX() + a.getWidth() / 2;
                final int cxB = b.getX() + b.getWidth() / 2;
                return Integer.compare(cxA, cxB);
            }
        });

        final List<List<Key>> rows = new ArrayList<>();
        for (final Key key : validKeys) {
            final int cy = key.getY() + key.getHeight() / 2;
            List<Key> matchedRow = null;
            for (final List<Key> row : rows) {
                final Key sample = row.get(0);
                final int rowCy = sample.getY() + sample.getHeight() / 2;
                final int threshold = Math.max(1, Math.min(key.getHeight(), sample.getHeight()) / 2);
                if (Math.abs(cy - rowCy) < threshold) {
                    matchedRow = row;
                    break;
                }
            }
            if (matchedRow == null) {
                matchedRow = new ArrayList<>();
                rows.add(matchedRow);
            }
            matchedRow.add(key);
        }

        for (final List<Key> row : rows) {
            Collections.sort(row, new Comparator<Key>() {
                @Override
                public int compare(final Key a, final Key b) {
                    final int cxA = a.getX() + a.getWidth() / 2;
                    final int cxB = b.getX() + b.getWidth() / 2;
                    return Integer.compare(cxA, cxB);
                }
            });
        }
        return rows;
    }
}

package com.holub.life.mouse;

import java.awt.Graphics;

/**
 * 보드 위에서 마우스가 하는 일 (State 패턴의 State). 좌표는 픽셀이 아니라 셀 단위
 * moved(), exited()는 미리보기를 다시 그려야 하면 true를 돌려줌
 */
public interface MouseMode {
    void clicked(int row, int column);

    default void rightClicked(int row, int column) {
        clicked(row, column);
    }

    default boolean moved(int row, int column) {
        return false;
    }

    default boolean exited() {
        return false;
    }

    default void drawPreview(Graphics g, int pixelsPerCell) {
    }
}

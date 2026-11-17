package com.holub.life.mouse;

import java.awt.Graphics;

/**
 * 지금 마우스 모드를 들고 있다가 마우스 입력을 넘기는 곳 (State 패턴의 Context)
 * Universe는 픽셀을 셀 좌표로 바꿔 여기에 넘기기만 하고, 모드가 무엇인지는 모름
 */
public class MouseController {
    private final MouseMode defaultMode;
    private MouseMode mode;
    private Runnable modeListener = () -> {
    };

    public MouseController(MouseMode defaultMode) {
        this.defaultMode = defaultMode;
        this.mode = defaultMode;
    }

    public MouseMode mode() {
        return mode;
    }

    public void setMode(MouseMode mode) {
        this.mode = mode;
        modeListener.run();
    }

    public void resetMode() {
        setMode(defaultMode);
    }

    // 모드가 바뀔 때 부름 (화면이 이전 모드의 미리보기를 지우도록)
    public void setModeListener(Runnable listener) {
        this.modeListener = listener;
    }

    public void clicked(int row, int column) {
        mode.clicked(row, column);
    }

    public void rightClicked(int row, int column) {
        mode.rightClicked(row, column);
    }

    public boolean moved(int row, int column) {
        return mode.moved(row, column);
    }

    public boolean exited() {
        return mode.exited();
    }

    public void drawPreview(Graphics g, int pixelsPerCell) {
        mode.drawPreview(g, pixelsPerCell);
    }
}

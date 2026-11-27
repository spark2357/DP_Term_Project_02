package com.holub.life.mouse;

import com.holub.life.game.LifeGame;

/**
 * 기본 모드: 클릭한 셀을 토글 (원본처럼 어느 버튼이든)
 */
public class ToggleMode implements MouseMode {
    private final LifeGame game;

    public ToggleMode(LifeGame game) {
        this.game = game;
    }

    @Override
    public void clicked(int row, int column) {
        game.toggle(row, column);
    }
}

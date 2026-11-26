package com.holub.life.mouse;

import com.holub.life.LifeGame;

/**
 * 기본 모드: 클릭한 셀을 토글 (원본처럼 어느 버튼이든)
 */
// [수정] 2026-11-23: 클릭한 셀을 펜 색으로 칠함. 같은 색이면 꺼지고(토글), 다른 색이면 색만 바뀜. 펜이 기본색(빨강)이면 원래 토글과 같음
public class ToggleMode implements MouseMode {
    private final LifeGame game;
    private final Pen pen;

    public ToggleMode(LifeGame game, Pen pen) {
        this.game = game;
        this.pen = pen;
    }

    @Override
    public void clicked(int row, int column) {
        game.brush(row, column, pen.getColor());
    }
}

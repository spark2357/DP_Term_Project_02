package com.holub.life.mouse;

import com.holub.life.LifeGame;

// [수정] 2026-11-27: 칠하기 모드. ToggleMode에 있던 칠하기를 이곳으로 옮김. 클릭한 셀을 펜 색으로 칠함.
// 같은 색이면 꺼지고, 다른 색이면 색만 바뀜. 펜이 기본색(빨강)이면 원래 클릭(ToggleMode)과 같음
public class BrushMode implements MouseMode{
    private final LifeGame game;
    private final Pen pen;

    public BrushMode(LifeGame game, Pen pen){
        this.game = game;
        this.pen = pen;
    }

    @Override
    public void clicked(int row, int column){
        game.brush(row, column, pen.getColor());
    }
}

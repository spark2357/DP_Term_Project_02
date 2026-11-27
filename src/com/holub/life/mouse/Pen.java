package com.holub.life.mouse;

import com.holub.life.model.CellColor;
import java.util.Objects;

// [수정] 2026-11-23: 셀 색 기능. 지금 펜 색. 칠하기 모드와 Stamp 모드가 같이 읽어서, 두 모드가 서로를 몰라도 됨
public class Pen{
    private CellColor color;

    public Pen(CellColor color){
        this.color = Objects.requireNonNull(color);
    }

    public void setColor(CellColor color){
        this.color = Objects.requireNonNull(color);
    }

    public CellColor getColor(){
        return color;
    }
}

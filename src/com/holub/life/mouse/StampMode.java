package com.holub.life.mouse;

import com.holub.life.model.CellPattern;
import com.holub.life.game.LifeGame;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

/**
 * 스탬프 모드: 왼쪽 클릭은 마우스가 가리키는 셀을 왼쪽 위로 해서 패턴을 찍고, 오른쪽 클릭은 패턴을 90도 돌림
 * 마우스가 보드 위에 있으면 찍힐 자리를 미리 보여줌
 */
public class StampMode implements MouseMode {
    // 불투명 색: 실행 중에는 바뀐 블록만 다시 그린 뒤 미리보기를 또 그리므로, 반투명이면 겹쳐 그릴수록 진해짐
    private static final Color PREVIEW_CELL = new Color(130, 180, 255);
    private static final Color PREVIEW_BORDER = new Color(0, 120, 255);

    private final LifeGame game;
    private final Pen pen;
    private CellPattern pattern;
    private Point hover;            // 마우스가 가리키는 셀 (x = 열, y = 행). 보드 밖이면 null

    public StampMode(LifeGame game, CellPattern pattern, Pen pen) {
        this.game = game;
        this.pen = pen;
        this.pattern = pattern;
    }

    public CellPattern pattern() {
        return pattern;
    }

    @Override
    public void clicked(int row, int column) {
        game.stamp(pattern, row, column, pen.getColor());
    }

    @Override
    public void rightClicked(int row, int column) {
        pattern = pattern.rotatedClockwise();
    }

    @Override
    public boolean moved(int row, int column) {
        Point here = new Point(column, row);
        if (here.equals(hover)) {
            return false;
        }
        hover = here;
        return true;
    }

    @Override
    public boolean exited() {
        boolean wasShown = hover != null;
        hover = null;
        return wasShown;
    }

    @Override
    public void drawPreview(Graphics g, int pixelsPerCell) {
        if (hover == null) {
            return;
        }
        int left = hover.x * pixelsPerCell;
        int top = hover.y * pixelsPerCell;

        Graphics preview = g.create();
        preview.setColor(PREVIEW_CELL);
        for (Point p : pattern.cells()) {
            preview.fillRect(left + p.x * pixelsPerCell, top + p.y * pixelsPerCell, pixelsPerCell, pixelsPerCell);
        }
        preview.setColor(PREVIEW_BORDER);
        preview.drawRect(left, top, pattern.width() * pixelsPerCell - 1, pattern.height() * pixelsPerCell - 1);
        preview.dispose();
    }
}

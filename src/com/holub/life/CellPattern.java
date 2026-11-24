package com.holub.life;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;

/**
 * 보드 위치와 상관없는 패턴 모양. width x height 사각형 안에서 산 칸의 좌표(왼쪽 위가 0,0, x = 열, y = 행)
 * 사각형 안의 나머지 칸은 죽은 칸이며, Stamp는 이 사각형 전체를 덮어씀
 */
public record CellPattern(int width, int height, Set<Point> cells) {
    public CellPattern {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("negative size: " + width + "x" + height);
        }
        Set<Point> copy = new HashSet<>();
        for (Point p : cells) {
            if (p.x < 0 || p.x >= width || p.y < 0 || p.y >= height) {
                throw new IllegalArgumentException("cell " + p + " is outside " + width + "x" + height);
            }
            copy.add(new Point(p));
        }
        cells = Set.copyOf(copy);
    }

    // 시계 방향으로 90도 돌린 패턴. 너비와 높이가 바뀜
    public CellPattern rotatedClockwise() {
        Set<Point> rotated = new HashSet<>();
        for (Point p : cells) {
            rotated.add(new Point(height - 1 - p.y, p.x));
        }
        return new CellPattern(height, width, rotated);
    }

    @Override
    public Set<Point> cells() {
        Set<Point> copy = new HashSet<>();
        for (Point p : cells) {
            copy.add(new Point(p));
        }
        return copy;
    }
}

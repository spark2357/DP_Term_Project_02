package com.holub.life.support;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;

/**
 * 보드 단위 테스트에서 셀 좌표를 읽기 쉽게 쓰기 위한 헬퍼
 */
public final class Cells {
	private Cells() {
	}

	// 예: Cells.fromRowColumn("7,2", "7,3", "7,4") → 7행의 2, 3, 4열
	public static Set<Point> fromRowColumn(String... rowCommaColumn) {
		Set<Point> cells = new HashSet<>();
		for (String text : rowCommaColumn) {
			String[] parts = text.split(",");
			int row = Integer.parseInt(parts[0].trim());
			int column = Integer.parseInt(parts[1].trim());
			cells.add(new Point(column, row));
		}
		return cells;
	}

	// 패턴을 (dRow, dColumn)만큼 이동. 보드 밖으로 나간 셀은 버림
	public static Set<Point> shift(Set<Point> pattern, int dRow, int dColumn) {
		Set<Point> moved = new HashSet<>();
		for (Point cell : pattern) {
			int row = cell.y + dRow;
			int column = cell.x + dColumn;
			if (row >= 0 && row < LifeTestDriver.SIZE && column >= 0 && column < LifeTestDriver.SIZE) {
				moved.add(new Point(column, row));
			}
		}
		return moved;
	}

	// 패턴을 (startRow, startColumn)부터 8칸 간격으로 보드 전체에 64개 놓음. 보드 밖으로 나가는 셀은 버림
	// 시작 위치를 0~7로 바꿔 64번 부르면 64x64의 모든 위치를 한 번씩 덮음
	public static Set<Point> repeatEvery8(Set<Point> pattern, int startRow, int startColumn) {
		Set<Point> all = new HashSet<>();
		for (int row = startRow; row < LifeTestDriver.SIZE; row += 8) {
			for (int column = startColumn; column < LifeTestDriver.SIZE; column += 8) {
				all.addAll(shift(pattern, row, column));
			}
		}
		return all;
	}
}

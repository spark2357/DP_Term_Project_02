package com.holub.life.support;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;

/**
 * 테스트용 정답 계산기: 다음 세대에 살아 있을 셀을 B3/S23 규칙으로 계산해, 테스트의 기대값(정답)으로 씀
 * 원본처럼 블록으로 나누거나 계산을 건너뛰지 않고, 64x64 모든 칸의 이웃 8칸을 직접 셈 (느리지만 단순해서 틀리기 어려움)
 * 보드 밖은 죽은 셀로 셈 (원본의 Cell.DUMMY와 같음)
 * 원본 결과와 세대마다 비교해 블록 경계 처리 오류를 찾는 데 씀. 결과가 맞는지만 확인하고, 계산이 얼마나 줄었는지는 확인하지 않음
 */
public final class ExpectedGenerationCalculator {
	private static final int SIZE = LifeTestDriver.SIZE;

	private ExpectedGenerationCalculator() {
	}

	// live: 지금 살아 있는 셀 좌표 Point(x=열, y=행). 반환값: 다음 세대에 살아 있는 셀 좌표
	public static Set<Point> nextGeneration(Set<Point> live) {
		Set<Point> next = new HashSet<>();
		for (int row = 0; row < SIZE; row++) {
			for (int column = 0; column < SIZE; column++) {
				int neighbors = countLiveNeighbors(live, row, column);
				boolean alive = live.contains(new Point(column, row));

				if (neighbors == 3 || (alive && neighbors == 2)) {
					next.add(new Point(column, row));
				}
			}
		}
		return next;
	}

	private static int countLiveNeighbors(Set<Point> live, int row, int column) {
		int count = 0;
		for (int dRow = -1; dRow <= 1; dRow++) {
			for (int dColumn = -1; dColumn <= 1; dColumn++) {
				if (dRow == 0 && dColumn == 0) {
					continue;	// 자기 자신은 세지 않음
				}
				// 보드 밖 좌표는 live에 들어 있지 않으므로 자동으로 죽은 셀로 셈
				if (live.contains(new Point(column + dColumn, row + dRow))) {
					count++;
				}
			}
		}
		return count;
	}
}

package com.holub.life;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

/**
 * Board.stamp 단위 테스트: 패턴의 왼쪽 위를 (행, 열)에 맞추어 찍음
 *
 *   메서드
 *   - stamp(pattern, 행, 열) : 패턴 사각형 안은 패턴대로(죽은 칸 포함) 덮어쓰고, 밖의 셀은 그대로 둠.
 *                              보드 밖으로 나가는 부분은 잘라냄
 *
 * [테스트 시나리오]
 *   1. 덮어쓰기: 사각형 안의 기존 셀은 패턴대로 바뀌고, 사각형 밖의 셀은 남음. 찍은 패턴은 다음 세대로 진행됨
 *   2. 잘라내기: 보드 가장자리를 넘는 부분만 버리고 나머지는 찍힘
 *
 * [커버리지]
 *   - BoardStampTest.java 테스트 대상: Board.stamp
 *   - 테스트 제외 대상
 *     - 블록 경계에 걸친 패턴의 진행: stamp는 Load와 같은 transfer(LOAD) 경로를 써서 Store/Load 테스트에서 확인됨
 */
@Tag("extension")
class BoardStampTest {
	// x = 3, y = 3 사각형 안의 glider
	private static final CellPattern GLIDER = new CellPattern(3, 3,
			Cells.fromRowColumn("0,1", "1,2", "2,0", "2,1", "2,2"));

	//  1. 덮어쓰기

	@Test
	@DisplayName("stamp(): 사각형 안의 기존 셀은 패턴대로 바뀌고(죽은 칸도 덮어씀), 밖의 셀은 그대로 남는다")
	void overwritesOnlyInsideTheRectangle() {
		// Given: 사각형 안의 (10,10)은 패턴에서 죽은 칸, (9,9)는 사각형 밖
		Board board = new Board();
		board.toggle(10, 10);
		board.toggle(9, 9);

		// When
		board.stamp(GLIDER, 10, 10);

		// Then
		Set<Point> expected = Cells.fromRowColumn("9,9", "10,11", "11,12", "12,10", "12,11", "12,12");
		assertEquals(expected, board.liveCells());

		// Then: 찍은 glider는 다음 세대로 진행됨 (9,9는 이웃이 없어 죽음)
		board.step();
		assertEquals(Cells.fromRowColumn("11,10", "11,12", "12,11", "12,12", "13,11"), board.liveCells());
	}

	//  2. 잘라내기

	@Test
	@DisplayName("stamp(): 오른쪽 아래 구석에 찍으면 보드 밖으로 나가는 부분만 버린다")
	void clipsAtTheEdge() {
		// Given
		Board board = new Board();
		int last = board.widthInCells() - 1;

		// When: 왼쪽 위가 (63, 62)이면 첫 행(0행)의 (63, 63)만 보드 안
		board.stamp(GLIDER, last, last - 1);

		// Then
		assertEquals(Set.of(new Point(last, last)), board.liveCells());
	}
}

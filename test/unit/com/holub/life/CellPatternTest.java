package com.holub.life;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * CellPattern 단위 테스트: 스탬프로 찍을 패턴 (크기와 산 셀 좌표를 가진 값 객체)
 *
 *   메서드
 *   - 생성자            : 크기가 음수이거나 셀이 크기 밖이면 IllegalArgumentException
 *   - rotatedClockwise() : 시계 방향 90도 회전. 가로와 세로가 바뀜
 *   - cells()            : 셀 좌표의 복사본
 *
 * [테스트 시나리오]
 *   1. 잘못된 패턴: 음수 크기, 크기 밖의 셀은 만들 수 없음
 *   2. 회전: 90도 회전하면 크기가 바뀌고 셀이 정해진 자리로 감. 네 번 돌리면 처음과 같음
 *   3. 복사: cells()로 받은 집합이나 만들 때 넘긴 집합을 바꿔도 패턴은 그대로
 *
 * [커버리지]
 *   - CellPatternTest.java 테스트 대상: 생성자, rotatedClockwise, cells
 *   - 테스트 제외 대상
 *     - 보드에 찍기: BoardStampTest에서 확인
 */
@Tag("extension")
class CellPatternTest {

	@Test
	@DisplayName("생성자: 크기가 음수이면 만들 수 없다")
	void rejectsNegativeSize() {
		assertThrows(IllegalArgumentException.class, () -> new CellPattern(-1, 3, Set.of()));
	}

	@Test
	@DisplayName("생성자: 크기 밖에 있는 셀이 있으면 만들 수 없다")
	void rejectsCellOutsideTheSize() {
		// Given: 3x3 패턴에 (3, 0) 셀
		Set<Point> cells = Set.of(new Point(3, 0));

		// When, Then
		assertThrows(IllegalArgumentException.class, () -> new CellPattern(3, 3, cells));
	}

	@Test
	@DisplayName("rotatedClockwise(): 시계 방향으로 90도 돌리면 가로와 세로가 바뀌고 셀이 정해진 자리로 간다")
	void rotatesClockwise() {
		// Given: 3x1 가로 막대의 왼쪽 끝 하나만 있는 패턴 (0행 0열)
		CellPattern pattern = new CellPattern(3, 1, Cells.fromRowColumn("0,0"));

		// When
		CellPattern rotated = pattern.rotatedClockwise();

		// Then: 1x3 세로 막대가 되고, 왼쪽 끝은 맨 위로 감
		assertEquals(1, rotated.width());
		assertEquals(3, rotated.height());
		assertEquals(Cells.fromRowColumn("0,0"), rotated.cells());
	}

	@Test
	@DisplayName("rotatedClockwise(): glider를 네 번 돌리면 처음 모양으로 돌아온다")
	void fourRotationsGiveTheOriginal() {
		// Given
		CellPattern glider = new CellPattern(3, 3, Cells.fromRowColumn("0,1", "1,2", "2,0", "2,1", "2,2"));

		// When
		CellPattern rotated = glider.rotatedClockwise().rotatedClockwise().rotatedClockwise().rotatedClockwise();

		// Then
		assertEquals(glider, rotated);
	}

	@Test
	@DisplayName("cells(): 받은 집합이나 만들 때 넘긴 집합을 바꿔도 패턴은 바뀌지 않는다")
	void cellsAreCopied() {
		// Given
		Set<Point> source = new java.util.HashSet<>(Cells.fromRowColumn("0,0"));
		CellPattern pattern = new CellPattern(2, 2, source);

		// When
		source.add(new Point(1, 1));
		pattern.cells().add(new Point(1, 0));

		// Then
		assertEquals(Cells.fromRowColumn("0,0"), pattern.cells());
	}
}

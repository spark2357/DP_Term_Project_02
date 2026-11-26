package com.holub.life.unit;

import com.holub.life.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * LiveCells 단위 테스트: 살아 있는 셀들과 그 색을 묶은 불변 값 객체
 *
 *   - LiveCells(map)   : 위치별 색으로 만듦
 *   - of(positions)    : 색 정보가 없는 위치 목록은 모두 기본색
 *   - isAlive, colorAt : 죽은 자리의 색을 물으면 예외
 *   - equals           : 위치와 색이 모두 같아야 같음
 *   - 불변성            : 넘긴 Map이나 Point, positions()/toMap()으로 받은 것을 고쳐도 바뀌지 않음
 *
 * [커버리지]
 *   - LiveCellsTest.java 테스트 대상: 생성자, of, isAlive, colorAt, positions, toMap, size, isEmpty, equals
 */
class LiveCellsTest {

	private static final CellColor BLUE = new CellColor(0, 0, 255);

	@Test
	@DisplayName("LiveCells(map): 위치별 색을 그대로 담는다")
	void constructorKeepsColors() {
		LiveCells cells = new LiveCells(Map.of(new Point(0, 0), BLUE, new Point(5, 5), CellColor.DEFAULT));

		assertEquals(2, cells.size());
		assertEquals(BLUE, cells.colorAt(new Point(0, 0)));
		assertEquals(CellColor.DEFAULT, cells.colorAt(new Point(5, 5)));
	}

	@Test
	@DisplayName("of(): 위치만 주면 모두 기본색으로 살아 있다")
	void ofUsesDefaultColor() {
		LiveCells cells = LiveCells.of(Set.of(new Point(1, 2), new Point(3, 4)));

		assertEquals(Set.of(new Point(1, 2), new Point(3, 4)), cells.positions());
		assertEquals(CellColor.DEFAULT, cells.colorAt(new Point(1, 2)));
		assertEquals(CellColor.DEFAULT, cells.colorAt(new Point(3, 4)));
	}

	@Test
	@DisplayName("colorAt(): 죽은 자리의 색을 물으면 IllegalArgumentException")
	void colorAtDeadCellThrows() {
		LiveCells cells = LiveCells.of(Set.of(new Point(1, 1)));

		assertFalse(cells.isAlive(new Point(2, 2)));
		assertThrows(IllegalArgumentException.class, () -> cells.colorAt(new Point(2, 2)));
	}

	@Test
	@DisplayName("빈 Map으로 만들면 비어 있다")
	void emptyMapIsEmpty() {
		LiveCells cells = new LiveCells(Map.of());

		assertTrue(cells.isEmpty());
		assertEquals(LiveCells.of(Set.of()), cells);
	}

	@Test
	@DisplayName("equals(): 위치가 같아도 색이 다르면 다르다")
	void equalsComparesColors() {
		LiveCells red = LiveCells.of(Set.of(new Point(1, 1)));
		LiveCells sameRed = new LiveCells(Map.of(new Point(1, 1), CellColor.DEFAULT));
		LiveCells blue = new LiveCells(Map.of(new Point(1, 1), BLUE));

		assertEquals(red, sameRed);
		assertEquals(red.hashCode(), sameRed.hashCode());
		assertNotEquals(red, blue);
	}

	@Test
	@DisplayName("불변성: 만들 때 넘긴 Map이나 Point를 나중에 고쳐도 LiveCells는 그대로다")
	void constructorCopiesInput() {
		// Given
		Point p = new Point(1, 1);
		Map<Point, CellColor> input = new HashMap<>();
		input.put(p, BLUE);
		LiveCells cells = new LiveCells(input);

		// When
		p.move(9, 9);
		input.put(new Point(2, 2), BLUE);

		// Then
		assertEquals(Set.of(new Point(1, 1)), cells.positions());
	}

	@Test
	@DisplayName("불변성: positions()나 toMap()으로 받은 것을 고쳐도 LiveCells는 그대로다")
	void returnedCollectionsAreCopies() {
		// Given
		LiveCells cells = new LiveCells(Map.of(new Point(1, 1), BLUE));

		// When
		cells.positions().clear();
		Map<Point, CellColor> map = cells.toMap();
		map.remove(new Point(1, 1));
		map.put(new Point(2, 2), BLUE);

		// Then
		assertEquals(Map.of(new Point(1, 1), BLUE), cells.toMap());
	}
}

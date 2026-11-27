package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Direction 단위 테스트: 블록 가장자리의 8방향을 비트로 모은 값 (블록 경계 변경 표시에 씀)
 *
 *   메서드
 *   - add(d), has(d), the(d), clear(), equals(d)
 *   - 상수 NORTH ~ SOUTHWEST, ALL, NONE : 바꿀 수 없음
 *
 * [테스트 시나리오]
 *   1. 모으기: 방향을 더하면 그 방향을 가짐. 더하지 않은 방향은 없음
 *   2. ALL과 NONE: ALL은 8방향을 모두 가지고, NONE은 아무 방향도 없음
 *   3. 지우기: clear()하면 NONE과 같아짐
 *   4. 상수 보호: 상수를 바꾸려 하면 UnsupportedOperationException
 *   5. 복사: 복사한 Direction을 바꿔도 원래 값은 그대로
 *
 * [커버리지]
 *   - DirectionTest.java 테스트 대상: add, has, the, clear, equals, 복사 생성자, 상수
 */
@Tag("original")
class DirectionTest {

	private static final Direction[] EIGHT = {
		Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
		Direction.NORTHEAST, Direction.NORTHWEST, Direction.SOUTHEAST, Direction.SOUTHWEST
	};

	@Test
	@DisplayName("add(), has(): 더한 방향만 가지고 있다")
	void hasOnlyAddedDirections() {
		// Given
		Direction d = new Direction();

		// When
		d.add(Direction.NORTH);
		d.add(Direction.SOUTHEAST);

		// Then
		assertTrue(d.has(Direction.NORTH));
		assertTrue(d.has(Direction.SOUTHEAST));
		assertFalse(d.has(Direction.WEST));
	}

	@Test
	@DisplayName("has(): ALL은 8방향을 모두 가지고, NONE은 아무 방향도 없다")
	void allHasEveryDirectionAndNoneHasNothing() {
		for (Direction each : EIGHT) {
			assertTrue(Direction.ALL.has(each));
			assertFalse(Direction.NONE.has(each));
		}
	}

	@Test
	@DisplayName("clear(): 지우면 NONE과 같아진다")
	void clearMakesItNone() {
		// Given
		Direction d = new Direction();
		d.add(Direction.EAST);

		// When
		d.clear();

		// Then
		assertTrue(d.equals(Direction.NONE));
	}

	@Test
	@DisplayName("add(), clear(): NORTH, ALL 같은 상수는 바꿀 수 없다")
	void constantsCannotBeChanged() {
		assertThrows(UnsupportedOperationException.class, () -> Direction.NORTH.add(Direction.SOUTH));
		assertThrows(UnsupportedOperationException.class, () -> Direction.ALL.clear());
	}

	@Test
	@DisplayName("Direction(Direction): 복사한 값을 바꿔도 원래 값은 그대로다")
	void copyIsIndependent() {
		// Given
		Direction original = new Direction();
		original.add(Direction.NORTH);
		Direction copy = new Direction(original);

		// When
		copy.add(Direction.SOUTH);

		// Then
		assertFalse(original.has(Direction.SOUTH));
		assertTrue(copy.has(Direction.NORTH));
	}
}

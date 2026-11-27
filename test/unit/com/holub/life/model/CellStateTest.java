package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * CellState 단위 테스트: 셀 상태(State 패턴의 State)와 상태 사이의 전이
 *
 *   상태
 *   - Dead.INSTANCE           : 죽음. 색이 없음
 *   - new Alive(색)           : 삶. 반드시 색이 있음
 *
 *   전이
 *   - brushedWith(색)  : 칠하기. 죽음 → 그 색으로 삶, 다른 색으로 삶 → 그 색으로 바뀜, 같은 색으로 삶 → 죽음
 *
 * [테스트 시나리오]
 *   1. 상태와 색이 어긋날 수 없음: 죽음에는 색이 없고, 색 없는 삶은 만들 수 없음
 *   2. 칠하기
 *
 * [커버리지]
 *   - CellStateTest.java 테스트 대상: Dead.INSTANCE, new Alive(), brushedWith
 *   - 보드 위에서의 동작(세대 진행, 물려받기)은 CellColorBoardTest에서 확인
 */
// [수정] 2026-11-27: CellState.DEAD는 Dead.INSTANCE, CellState.alive(색)은 new Alive(색)으로 바뀜 (CellState가 구현체를 모르게 함)
@Tag("extension")
class CellStateTest {
	private static final CellColor BLUE = new CellColor(0, 0, 255);
	private static final CellColor YELLOW = new CellColor(255, 255, 0);

	//  1. 상태와 색이 어긋날 수 없음

	@Test
	@DisplayName("isAlive(), getColor(): Dead.INSTANCE는 죽어 있고 색이 없다. new Alive(색)은 살아 있고 그 색을 가진다")
	void aliveAndColorComeTogether() {
		assertFalse(Dead.INSTANCE.isAlive());
		assertEquals(Optional.empty(), Dead.INSTANCE.getColor());

		assertTrue(new Alive(BLUE).isAlive());
		assertEquals(Optional.of(BLUE), new Alive(BLUE).getColor());
	}

	@Test
	@DisplayName("new Alive(): 색 없이(null) 산 상태는 만들 수 없다")
	void aliveWithoutColorIsRejected() {
		assertThrows(NullPointerException.class, () -> new Alive(null));
	}

	//  2. 칠하기

	@Test
	@DisplayName("brushedWith(): 죽은 셀은 그 색으로 살고, 다른 색 셀은 색만 바뀌고, 같은 색 셀은 죽는다")
	void brushingDependsOnCurrentState() {
		assertEquals(Optional.of(BLUE), Dead.INSTANCE.brushedWith(BLUE).getColor());
		assertEquals(Optional.of(YELLOW), new Alive(BLUE).brushedWith(YELLOW).getColor());
		assertSame(Dead.INSTANCE, new Alive(BLUE).brushedWith(BLUE));
	}

	@Test
	@DisplayName("brushedWith(): 새 상태를 돌려주고, 원래 상태는 바뀌지 않는다")
	void brushingReturnsNewStateAndKeepsOriginal(){
		// Given
		CellState blue = new Alive(BLUE);

		// When
		CellState yellow = blue.brushedWith(YELLOW);
		CellState born = Dead.INSTANCE.brushedWith(BLUE);

		// Then
		assertEquals(Optional.of(YELLOW), yellow.getColor());
		assertEquals(Optional.of(BLUE), blue.getColor(), "칠하기 전 산 상태");
		assertTrue(born.isAlive());
		assertFalse(Dead.INSTANCE.isAlive(), "칠하기 전 죽은 상태");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("anyStates")
	@DisplayName("brushedWith(): 색 없이(null) 칠하면 죽은 셀이든 산 셀이든 NullPointerException")
	void brushingWithNullIsRejected(CellState state){
		assertThrows(NullPointerException.class, () -> state.brushedWith(null));
	}

	private static Stream<CellState> anyStates(){
		return Stream.of(Dead.INSTANCE, new Alive(BLUE));
	}
}

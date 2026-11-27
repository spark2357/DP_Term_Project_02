package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * BoardHistory 단위 테스트: Undo/Redo 기록 (Memento 패턴의 Caretaker)
 *
 *   메서드
 *   - record(before)  : 바뀌기 전 스냅샷을 undo에 쌓고 redo를 비움
 *   - undo(current)   : undo에서 꺼내 돌려주고, current는 redo에 쌓음
 *   - redo(current)   : redo에서 꺼내 돌려주고, current는 undo에 쌓음
 *
 * [테스트 시나리오]
 *   1. 빈 기록: Undo/Redo할 것이 없으면 빈 Optional
 *   2. 순서: 마지막에 기록한 것부터 되돌리고, 되돌린 순서의 반대로 다시 실행함
 *   3. 새 기록: Undo한 뒤 새로 기록하면 redo가 비워짐
 *   4. 개수 제한: 넘치면 가장 오래된 기록부터 버림
 *
 * [커버리지]
 *   - 스냅샷의 내용은 보지 않으므로 같은 객체가 돌아오는지(assertSame)로 확인함
 *   - 기록이 비었는지는 undo()/redo()가 빈 Optional을 돌려주는지로 확인함 (비었을 때는 아무것도 바뀌지 않음)
 *   - 보드에 적용하는 것은 통합 테스트 LifeGameUndoTest, UndoRedoUseCaseTest에서 확인
 */
@Tag("extension")
class BoardHistoryTest {
	// 보드 A → B → C로 바뀐 기록. 스냅샷은 서로 다른 객체이기만 하면 됨
	// [수정] 2026-11-27: 진짜 Board 대신 값 객체 BoardSnapshot을 직접 만듦 (BoardHistory만 확인하는 단위 테스트)
	private final BoardSnapshot a = snapshot();
	private final BoardSnapshot b = snapshot();
	private final BoardSnapshot c = snapshot();

	//  1. 빈 기록

	@Test
	@DisplayName("undo(), redo(): 기록이 없으면 빈 Optional이고, 넘긴 스냅샷은 쌓이지 않는다")
	void emptyHistoryReturnsNothing() {
		// Given
		BoardHistory history = new BoardHistory(100);

		// When
		Optional<BoardSnapshot> undone = history.undo(a);
		Optional<BoardSnapshot> redone = history.redo(a);

		// Then: Undo가 a를 redo에 쌓았다면 Redo가 a를 돌려줬을 것
		assertTrue(undone.isEmpty());
		assertTrue(redone.isEmpty());
		assertTrue(history.undo(a).isEmpty());
	}

	//  2. 순서

	@Test
	@DisplayName("undo(): 마지막에 기록한 보드부터 되돌리고, redo()는 되돌린 순서의 반대로 다시 실행한다")
	void undoAndRedoInOrder() {
		// Given: A → B → C
		BoardHistory history = historyOfABC();

		// When, Then: Undo 두 번 (C → B → A)
		assertSame(b, history.undo(c).orElseThrow());
		assertSame(a, history.undo(b).orElseThrow());
		assertTrue(history.undo(a).isEmpty());

		// When, Then: Redo 두 번 (A → B → C)
		assertSame(b, history.redo(a).orElseThrow());
		assertSame(c, history.redo(b).orElseThrow());
		assertTrue(history.redo(c).isEmpty());

		// Then: 다시 Undo할 수 있음
		assertSame(b, history.undo(c).orElseThrow());
	}

	//  3. 새 기록

	@Test
	@DisplayName("record(): Undo한 뒤 새로 기록하면 redo가 비워진다")
	void recordClearsRedo() {
		// Given: A → B → C에서 Undo 한 번 (지금 B)
		BoardHistory history = historyOfABC();
		history.undo(c);

		// When: B에서 다른 편집
		history.record(b);

		// Then
		assertTrue(history.redo(b).isEmpty());
	}

	//  4. 개수 제한

	@Test
	@DisplayName("record(): 제한을 넘으면 가장 오래된 기록부터 버린다")
	void dropsOldestOverLimit() {
		// Given: 최대 2개
		BoardHistory history = new BoardHistory(2);

		// When: A → B → C → (지금) 세 번 기록
		history.record(a);
		history.record(b);
		history.record(c);

		// Then: C, B만 남고 A는 버려짐
		BoardSnapshot now = snapshot();
		assertSame(c, history.undo(now).orElseThrow());
		assertSame(b, history.undo(c).orElseThrow());
		assertTrue(history.undo(b).isEmpty());
	}

	@Test
	@DisplayName("BoardHistory(): 제한이 0 이하이면 IllegalArgumentException")
	void limitMustBePositive() {
		assertThrows(IllegalArgumentException.class, () -> new BoardHistory(0));
	}

	// ---- 헬퍼 메서드

	private static BoardSnapshot snapshot(){
		return new BoardSnapshot(new LiveCells(Map.of()));
	}

	// A → B → C로 바뀐 기록 (지금 보드는 C)
	private BoardHistory historyOfABC() {
		BoardHistory history = new BoardHistory(100);
		history.record(a);
		history.record(b);
		return history;
	}
}

package com.holub.life.unit;

import com.holub.life.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.util.Collection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Resident 단위 테스트: 보드의 셀 하나 (Composite 패턴의 Leaf)
 *
 *   필드
 *   - amAlive      : 현재 살아 있는지
 *   - willBeAlive  : 다음 세대에 살아 있을지 (figureNextState()가 계산한 값, 보드 전체 계산 후 반영)
 *   - BORDER_COLOR, LIVE_COLOR, DEAD_COLOR : 그리기 색상
 *   메서드
 *   - figureNextState(이웃 8개) : B3/S23 규칙으로 willBeAlive를 계산. 바뀔 셀이면 true
 *   - transition()              : amAlive = willBeAlive 로 적용. 상태가 바뀌었으면 true를 반환해야 함
 *   - isAlive()                 : amAlive 반환
 *   - userClicked(점, 영역)     : amAlive를 뒤집음 (좌표는 쓰지 않음)
 *   - clear()                   : amAlive, willBeAlive 모두 false
 *   - isDisruptiveTo()          : 바뀔 셀이면 Direction.ALL, 아니면 Direction.NONE
 *   - transfer(memento, 좌표, LOAD/STORE) : 파일에서 상태를 읽거나(LOAD) 산 셀을 기록(STORE)
 *   - createMemento()           : 항상 UnsupportedOperationException (memento는 Neighborhood가 만듦)
 *   - redraw(), edge(), create(), widthInCells() : 그리기, 이웃 조회, 복제, Cell 가로 크기(Resident는 Leaf라서 1)
 *   - verify(), isStable()      : private 보조 메서드
 *
 * [테스트 시나리오]
 *   1. B3/S23 규칙: 죽은 셀과 산 셀 각각에 대해 이웃 0~8개를 모두 넣어 탄생, 생존, 사망을 확인
 *   2. 두 단계 갱신: figureNextState()만으로는 상태가 바뀌지 않고 transition()에서 바뀜
 *   3. transition()의 반환값: 바뀐 셀만 true여야 하지만 원본은 반대로 반환함 (결함: 블록 건너뛰기 최적화가 동작하지 않는 원인)
 *   4. 보드 밖: Cell.DUMMY가 죽은 이웃으로 세어짐
 *   5. Clear: 계산해 둔 다음 상태까지 지워짐
 *   6. Store/Load: 산 셀만 기록되고(memento 전체를 확인), Load한 셀은 안정 상태로 들어감 (원본 동작)
 *
 * [커버리지]
 *   - ResidentTest.java 테스트 대상: figureNextState, transition(상태 변화와 반환값), isAlive, userClicked, clear, isDisruptiveTo, transfer
 *   - 테스트 제외 대상
 *     - redraw(): 화면 그리기라 유닛 테스트에서 제외
 *     - edge(), create(), widthInCells(), createMemento(): 한 줄짜리 고정 반환이라 생략
 */
class ResidentTest {

	@Test
	@DisplayName("figureNextState(), transition(): 죽은 셀은 이웃이 정확히 3개일 때만 태어난다")
	void deadCellIsBornOnlyWithThreeNeighbors() {
		for (int liveNeighbors = 0; liveNeighbors <= 8; liveNeighbors++) {
			// Given
			Resident cell = new Resident();

			// When
			figureNextState(cell, neighbors(liveNeighbors));
			cell.transition();

			// Then
			boolean expected = (liveNeighbors == 3);
			assertEquals(expected, cell.isAlive(), "liveNeighbors " + liveNeighbors + "개");
		}
	}

	@Test
	@DisplayName("figureNextState(), transition(): 산 셀은 이웃이 2~3개면 살고, 나머지는 죽는다")
	void liveCellSurvivesOnlyWithTwoOrThreeNeighbors() {
		for (int liveNeighbors = 0; liveNeighbors <= 8; liveNeighbors++) {
			// Given
			Resident cell = liveCell();

			// When
			figureNextState(cell, neighbors(liveNeighbors));
			cell.transition();

			// Then
			boolean expected = (liveNeighbors == 2 || liveNeighbors == 3);
			assertEquals(expected, cell.isAlive(), "liveNeighbors " + liveNeighbors + "개");
		}
	}

	@Test
	@DisplayName("figureNextState(), transition(): 계산만으로는 상태가 바뀌지 않고, transition()에서 바뀐다")
	void stateChangesOnlyOnTransition() {
		// Given
		Resident cell = new Resident();

		// When: 다음 상태만 계산
		figureNextState(cell, neighbors(3));

		// Then: 아직 죽어 있음
		assertFalse(cell.isAlive(), "figureNextState() 직후");

		// When: 다음 상태 적용
		cell.transition();

		// Then: 이제 살아 있음
		assertTrue(cell.isAlive(), "transition() 직후");
	}

	@Test
	@DisplayName("transition() (결함): 상태가 바뀐 셀만 true를 반환해야 한다 (원본: 반대로 반환해 블록 건너뛰기 최적화가 동작하지 않음)")
	void transitionReturnsTrueOnlyWhenChanged() {
		// Given: 그대로 죽어 있을 셀과, 태어날 셀
		Resident unchanged = new Resident();
		figureNextState(unchanged, neighbors(0));

		Resident changed = new Resident();
		figureNextState(changed, neighbors(3));

		// When
		boolean unchangedResult = unchanged.transition();
		boolean changedResult = changed.transition();

		// Then
		// 원본은 바꾸기 전의 isStable()을 반환해서 결과가 반대로 나옴 (Resident.java:81)
		// Neighborhood.transition()은 이 값으로 바뀐 가장자리를 표시하므로,
		// 바뀌지 않은 셀 때문에 매번 모든 가장자리가 표시되어 블록 건너뛰기가 동작하지 않음
		assertFalse(unchangedResult, "바뀌지 않은 셀");
		assertTrue(changedResult, "태어난 셀");
	}

	@Test
	@DisplayName("figureNextState(): 보드 밖을 나타내는 Cell.DUMMY는 항상 죽은 이웃으로 계산한다")
	void dummyCountsAsDeadNeighbor() {
		// Given: 산 이웃 2개 + DUMMY 6개
		Resident cell = new Resident();
		Cell[] around = {
			liveCell(), liveCell(),
			Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY
		};

		// When
		figureNextState(cell, around);
		cell.transition();
	
		// Then: DUMMY는 liveNeighbors가 아님
		assertFalse(Cell.DUMMY.isAlive());
		assertFalse(cell.isAlive());
	}

	@Test
	@DisplayName("clear(): 미리 계산해 둔 다음 상태까지 지워서 초기화한다")
	void clearAlsoForgetsNextState() {
		// Given: 다음 세대에 태어나야하는 셀
		Resident cell = new Resident();
		figureNextState(cell, neighbors(3));

		// When
		cell.clear();
		cell.transition();

		// Then
		assertFalse(cell.isAlive());
	}

	@Test
	@DisplayName("transfer(LOAD), isDisruptiveTo() (원본 동작): Load한 셀은 변화가 없는 셀이라 Go 전에는 이웃을 깨우지 않는다. Neighborhood.transfer()에서 문제 발생")
	void loadedCellIsNotDisruptive() {
		// Given: (0,0)이 살아 있다고 기록된 memento
		Cell.Memento memento = emptyMemento();
		memento.markAsAlive(new Point(0, 0));

		Resident clicked = new Resident();
		Resident loaded = new Resident();

		// When: 하나는 클릭으로, 하나는 Load로 살림
		clicked.userClicked(new Point(0, 0), new Rectangle(0, 0, 8, 8));
		loaded.transfer(memento, new Point(0, 0), Cell.LOAD);

		// Then: 둘 다 살아 있지만, 클릭한 셀은 ALL, Load한 셀은 NONE을 반환
		// Direction.equals(Direction)은 equals(Object)를 재정의하지 않으므로 assertEquals 대신 직접 호출
		assertTrue(clicked.isAlive());
		assertTrue(loaded.isAlive());
		assertTrue(clicked.isDisruptiveTo().equals(Direction.ALL));
		assertTrue(loaded.isDisruptiveTo().equals(Direction.NONE));
	}

	@Test
	@DisplayName("transfer(STORE): 산 셀만 memento에 기록한다")
	void storeRecordsOnlyLiveCells() throws Exception {
		// Given
		Cell.Memento memento = emptyMemento();
		Resident live = liveCell();
		Resident dead = new Resident();

		// When
		live.transfer(memento, new Point(3, 5), Cell.STORE);
		dead.transfer(memento, new Point(4, 5), Cell.STORE);

		// Then: memento에 기록된 좌표는 산 셀의 (3,5) 하나뿐
		Collection stored = storedPoints(memento);
		assertEquals(1, stored.size());
		assertTrue(stored.contains(new Point(3, 5)));
	}

	private static Resident liveCell() {
		Resident cell = new Resident();
		cell.userClicked(new Point(0, 0), new Rectangle(0, 0, 8, 8));
		return cell;
	}

	// 이웃 8개: 앞의 liveCount개는 산 셀, 나머지는 죽은 셀
	private static Cell[] neighbors(int liveCount) {
		Cell[] around = new Cell[8];
		for (int i = 0; i < 8; i++) {
			if (i < liveCount) {
				around[i] = liveCell();
			} else {
				around[i] = new Resident();
			}
		}
		return around;
	}

	private static boolean figureNextState(Resident cell, Cell[] around) {
		return cell.figureNextState(around[0], around[1], around[2], around[3],
									around[4], around[5], around[6], around[7]);
	}

	// Resident는 memento를 만들 수 없으므로 Neighborhood에서 생성
	private static Cell.Memento emptyMemento() {
		return (Cell.Memento) new Neighborhood(8, new Resident()).createMemento();
	}

	// memento에는 기록된 좌표 개수를 묻는 메서드가 없으므로, 저장한 내용을 다시 읽어 좌표 목록을 꺼냄
	private static Collection storedPoints(Cell.Memento memento) throws Exception {
		ByteArrayOutputStream file = new ByteArrayOutputStream();
		memento.flush(file);

		ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(file.toByteArray()));
		return (Collection) in.readObject();
	}
}

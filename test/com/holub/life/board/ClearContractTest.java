package com.holub.life.board;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Clear Contract 테스트: Grid → Clear가 보드를 완전히 비우고, 그 뒤에도 게임이 정상으로 진행되는지 검증 (회귀)
 * 추상 클래스이며, 하위 클래스가 newDriver()로 검증할 구현을 정함 (원본: OriginalClearTest)
 *
 *   사용하는 LifeTestDriver 메서드
 *   - click(행, 열), tick(), liveCells()
 *   - clear() : 원본에서는 Neighborhood.clear() → 모든 Resident.clear()
 *
 * [테스트 시나리오]
 *   1. 셀 지우기: 여러 블록과 보드 꼭짓점에 놓은 셀이 모두 지워짐
 *   2. 진행 중에 Clear: 패턴이 움직이던 보드를 Clear하면, 그 뒤 세대를 진행해도 아무것도 생기지 않음
 *      (미리 계산해 둔 다음 상태와 블록의 활성 표시가 남지 않음)
 *   3. Clear 후 다시 그리기: 진행하던 보드를 Clear하고 새로 놓은 패턴이 정답 계산 결과대로 진화함
 *      (블록 경계를 지나는 glider로, 지워진 가장자리 표시가 다시 제대로 켜지는지 확인)
 *   4. 빈 보드 Clear: 아무 변화 없음
 *
 * [커버리지]
 *   - ClearContractTest.java 테스트 대상: Clear 뒤의 보드 상태와 이후 세대 진행
 *   - 테스트 제외 대상
 *     - 블록 내부 표시(activeEdges 등)를 직접 확인하는 것: 원본 구조에 의존하므로 NeighborhoodTest에서 확인
 *     - Clear가 시계를 멈추지 않는 원본 동작: 시계가 있어야 하므로 통합 테스트에서 확인
 */
abstract class ClearContractTest {
	protected LifeTestDriver driver;

	abstract LifeTestDriver newDriver();

	@BeforeEach
	void setUp() {
		driver = newDriver();
	}

	@Test
	@DisplayName("clear(): 여러 블록과 보드 꼭짓점에 놓은 셀이 모두 지워진다")
	void clearRemovesAllCells() {
		// Given
		place(driver, Cells.fromRowColumn("0,0", "0,63", "63,0", "63,63", "7,7", "8,8", "30,30", "30,31", "45,12"));

		// When
		driver.clear();

		// Then
		assertEquals(Cells.fromRowColumn(), driver.liveCells());
	}

	@Test
	@DisplayName("clear(), tick(): 패턴이 움직이던 보드를 Clear하면 이후 세대를 진행해도 아무것도 생기지 않는다")
	void nothingAppearsAfterClearWhileRunning() {
		// Given: blinker, toad, glider를 놓고 몇 세대 진행
		place(driver, Cells.fromRowColumn("3,2", "3,3", "3,4"));
		place(driver, Cells.fromRowColumn("12,3", "12,4", "12,5", "13,2", "13,3", "13,4"));
		place(driver, Cells.fromRowColumn("20,21", "21,22", "22,20", "22,21", "22,22"));
		ticks(3);

		// When
		driver.clear();

		// Then: 바로 비어 있고, 세대를 진행해도 계속 비어 있음
		assertEquals(Cells.fromRowColumn(), driver.liveCells(), "Clear 직후");
		ticks(3);
		assertEquals(Cells.fromRowColumn(), driver.liveCells(), "Clear 뒤 3세대");
	}

	@Test
	@DisplayName("clear(), tick(): Clear한 뒤 새로 놓은 패턴은 정답 계산 결과대로 진화한다")
	void newPatternAfterClearEvolvesCorrectly() {
		// Given: 무작위 패턴을 10세대 진행한 보드를 Clear
		Random random = new Random(2026);
		Set<Point> soup = new HashSet<>();
		for (int row = 0; row < LifeTestDriver.SIZE; row++) {
			for (int column = 0; column < LifeTestDriver.SIZE; column++) {
				if (random.nextInt(100) < 30) {
					soup.add(new Point(column, row));
				}
			}
		}
		place(driver, soup);
		ticks(10);
		driver.clear();

		// When: 블록 경계와 꼭짓점을 지나가는 glider를 새로 놓음
		Set<Point> glider = Cells.shift(Cells.fromRowColumn("0,1", "1,2", "2,0", "2,1", "2,2"), 2, 2);
		place(driver, glider);

		// Then: 60세대 동안 세대마다 정답 계산 결과와 같음
		Set<Point> expected = glider;
		for (int generation = 1; generation <= 60; generation++) {
			driver.tick();
			expected = ExpectedGenerationCalculator.nextGeneration(expected);
			assertEquals(expected, driver.liveCells(), generation + "세대");
		}
	}

	@Test
	@DisplayName("clear(): 빈 보드를 Clear해도 아무 변화가 없다")
	void clearOnEmptyBoardChangesNothing() {
		// Given: 아무 셀도 놓지 않음

		// When
		driver.clear();
		ticks(2);

		// Then
		assertEquals(Cells.fromRowColumn(), driver.liveCells());
	}

	// ---- 헬퍼 메서드

	private void ticks(int count) {
		for (int i = 0; i < count; i++) {
			driver.tick();
		}
	}

	private static void place(LifeTestDriver board, Set<Point> cells) {
		for (Point cell : cells) {
			board.click(cell.y, cell.x);
		}
	}
}

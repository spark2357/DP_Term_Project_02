package com.holub.life;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Click Contract 테스트 (UC1 셀 상태 토글): 셀을 클릭하면 그 셀만 뒤집히고, 다음 세대 계산에 반영되는지 검증 (회귀)
 * 추상 클래스이며, 하위 클래스가 newDriver()로 검증할 구현을 정함 (원본: OriginalClickTest, 현재: BoardClickTest)
 *
 *   사용하는 LifeTestDriver 메서드
 *   - click(행, 열) : 원본에서는 Universe의 마우스 처리 → Neighborhood.userClicked()
 *   - tick(), liveCells()
 *
 * [테스트 시나리오]
 *   1. 토글: 죽은 셀을 클릭하면 살고, 한 번 더 클릭하면 다시 죽음
 *   2. 위치: 보드 꼭짓점과 블록 경계의 셀도 정확히 그 칸이 뒤집힘
 *   3. 다른 셀: 클릭한 셀 말고는 바뀌지 않음
 *   4. 멈춘 보드에 클릭: 변화가 없어 계산을 건너뛰던 블록도, 클릭한 뒤에는 다음 세대에 정상으로 계산됨
 *      (블록 안쪽과 블록 경계에 걸친 blinker)
 *
 * [커버리지]
 *   - ClickContractTest.java 테스트 대상: 클릭 뒤의 보드 상태와 이후 세대 진행
 *   - 테스트 제외 대상
 *     - 픽셀 좌표를 칸 좌표로 바꾸는 것, 화면 다시 그리기: 화면(Universe)이 필요하므로 시연으로 확인
 *     - 마우스 모드(토글/스탬프) 전환: 기능 확장 테스트에서 확인
 */
@Tag("original")
abstract class ClickContractTest {
	protected LifeTestDriver driver;

	abstract LifeTestDriver newDriver();

	@BeforeEach
	void setUp() {
		driver = newDriver();
	}

	@Test
	@DisplayName("click(): 죽은 셀을 클릭하면 살고, 한 번 더 클릭하면 다시 죽는다")
	void clickingTwiceTogglesBack() {
		// When, Then
		driver.click(10, 20);
		assertEquals(Cells.fromRowColumn("10,20"), driver.liveCells(), "한 번 클릭");

		driver.click(10, 20);
		assertEquals(Cells.fromRowColumn(), driver.liveCells(), "두 번 클릭");
	}

	@Test
	@DisplayName("click(): 보드 꼭짓점과 블록 경계의 셀도 정확히 그 칸이 뒤집힌다")
	void clickSelectsTheRightCellAtCornersAndBlockEdges() {
		// Given: 보드 꼭짓점 4개와 블록 경계(7|8행, 7|8열)의 셀
		Set<Point> cells = Cells.fromRowColumn("0,0", "0,63", "63,0", "63,63", "7,7", "7,8", "8,7", "8,8");

		// When
		place(cells);

		// Then
		assertEquals(cells, driver.liveCells());
	}

	@Test
	@DisplayName("click(): 클릭한 셀 말고는 바뀌지 않는다")
	void clickChangesOnlyThatCell() {
		// Given
		Set<Point> before = Cells.fromRowColumn("30,30", "30,31", "31,30", "31,31");
		place(before);

		// When
		driver.click(40, 40);

		// Then
		Set<Point> expected = new HashSet<>(before);
		expected.add(new Point(40, 40));
		assertEquals(expected, driver.liveCells());
	}

	@Test
	@DisplayName("click(), tick(): 변화가 멈춘 보드에 클릭으로 blinker를 놓으면 다음 세대에 정상으로 진동한다")
	void clickOnSettledBoardIsComputedNextGeneration() {
		// Given: block(고정 패턴)만 있는 보드를 여러 세대 진행해 변화가 멈춤
		Set<Point> block = Cells.fromRowColumn("50,50", "50,51", "51,50", "51,51");
		place(block);
		ticks(3);

		// When: 블록 안쪽과 블록 경계(7|8열)에 걸친 blinker를 클릭으로 놓음
		Set<Point> inside = Cells.fromRowColumn("3,2", "3,3", "3,4");
		Set<Point> acrossEdge = Cells.fromRowColumn("20,7", "20,8", "20,9");
		place(inside);
		place(acrossEdge);

		// Then: 세대마다 정답 계산 결과와 같음
		Set<Point> expected = driver.liveCells();
		for (int generation = 1; generation <= 4; generation++) {
			driver.tick();
			expected = ExpectedGenerationCalculator.nextGeneration(expected);
			assertEquals(expected, driver.liveCells(), generation + "세대");
		}
	}

	// ---- 헬퍼 메서드

	private void ticks(int count) {
		for (int i = 0; i < count; i++) {
			driver.tick();
		}
	}

	private void place(Set<Point> cells) {
		for (Point cell : cells) {
			driver.click(cell.y, cell.x);
		}
	}
}

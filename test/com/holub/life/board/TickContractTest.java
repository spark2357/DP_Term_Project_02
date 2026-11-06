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
 * Tick Contract 테스트: 세대 진행(Go → Tick)이 B3/S23 규칙대로 동작하는지 보드 전체에서 검증
 * 추상 클래스이며, 하위 클래스가 newDriver()로 검증할 구현을 정함 (원본: OriginalTickTest)
 *
 *   사용하는 LifeDriver 메서드
 *   - click(행, 열)  : 패턴 배치
 *   - tick()         : 한 세대 진행
 *   - liveCells()    : 결과 확인
 *   원본에서 실제로 실행되는 메서드
 *   - Neighborhood.figureNextState(), transition(), edge(), isDisruptiveTo(), userClicked()
 *   - Resident.figureNextState(), transition()
 *
 * [테스트 시나리오]
 *   1. 기본 패턴: 빈 보드, 고정 패턴(block, beehive), 진동 패턴(blinker, toad), 글라이더
 *   2. 블록 경계: 이웃 셀이 다른 8x8 블록에 있어도 정확히 세는지 확인
 *      - 가로 경계와 세로 경계 너머로 셀이 태어남
 *      - 네 블록이 만나는 꼭짓점에서 8방향의 이웃을 모두 계산
 *      - 이웃이 모두 다른 블록에 있어도 생존(이웃 2개)과 과밀 사망(이웃 4개)
 *   3. 보드 가장자리: 보드 밖은 죽은 셀(DUMMY)
 *   4. ExpectedGenerationCalculator와 비교: 세대 수나 셀 수가 많아 정답을 일일이 계산할 수 없는 경우, ExpectedGenerationCalculator가 계산한 정답과 세대마다 비교
 *      - 네 방향 글라이더가 블록 경계와 꼭짓점을 지나 벽에 닿을 때까지 (조용한 블록을 깨우는 경로 확인)
 *      - blinker와 glider를 보드의 모든 위치에 놓았을 때
 *      - 무작위 패턴을 50세대 진행했을 때
 *
 * [커버리지]
 *   - TickContractTest.java 테스트 대상: 세대 계산 결과 (블록 안, 블록 경계, 보드 가장자리)
 *   - 테스트 제외 대상
 *     - 계산 대상 셀 최적화
 *     - Load로 배치한 패턴의 진행: Store/Load 테스트에서 따로 확인
 *   - 원본 코드에서 계산 대상 셀 최적화가 이루어지지 않아서 리팩터링 시 현재 테스트로 검증
 */
abstract class TickContractTest {
	protected LifeTestDriver driver;

	abstract LifeTestDriver newDriver();

	@BeforeEach
	void setUp() {
		driver = newDriver();
	}

	// 1. 기본 패턴

	@Test
	@DisplayName("tick(): 빈 보드는 세대를 진행해도 비어 있다")
	void emptyBoardStaysEmpty() {
		// Given: 아무 셀도 놓지 않음

		// When
		ticks(3);

		// Then
		assertEquals(Cells.fromRowColumn(), driver.liveCells());
	}

	@Test
	@DisplayName("tick(): 고정 패턴(block, beehive)은 세대를 진행해도 그대로다")
	void stillLifesDoNotChange() {
		// Given
		Set<Point> block = Cells.fromRowColumn("10,10", "10,11", "11,10", "11,11");
		Set<Point> beehive = Cells.fromRowColumn("20,21", "20,22", "21,20", "21,23", "22,21", "22,22");
		place(block);
		place(beehive);

		// When
		ticks(5);

		// Then
		assertEquals(union(block, beehive), driver.liveCells());
	}

	@Test
	@DisplayName("tick(): blinker는 주기 2로 진동한다")
	void blinkerOscillates() {
		// Given
		Set<Point> horizontal = Cells.fromRowColumn("3,2", "3,3", "3,4");
		Set<Point> vertical = Cells.fromRowColumn("2,3", "3,3", "4,3");
		place(horizontal);

		// When, Then
		driver.tick();
		assertEquals(vertical, driver.liveCells(), "1세대");
		driver.tick();
		assertEquals(horizontal, driver.liveCells(), "2세대");
	}

	@Test
	@DisplayName("tick(): toad는 주기 2로 진동한다")
	void toadOscillates() {
		// Given
		Set<Point> phase1 = Cells.fromRowColumn("12,3", "12,4", "12,5", "13,2", "13,3", "13,4");
		Set<Point> phase2 = Cells.fromRowColumn("11,4", "12,2", "12,5", "13,2", "13,5", "14,3");
		place(phase1);

		// When, Then
		driver.tick();
		assertEquals(phase2, driver.liveCells(), "1세대");
		driver.tick();
		assertEquals(phase1, driver.liveCells(), "2세대");
	}

	@Test
	@DisplayName("tick(): glider는 4세대 뒤 같은 모양으로 오른쪽 아래로 한 칸 이동한다")
	void gliderMovesDiagonally() {
		// Given
		Set<Point> glider = Cells.fromRowColumn("1,2", "2,3", "3,1", "3,2", "3,3");
		place(glider);

		// When
		ticks(4);

		// Then
		assertEquals(Cells.shift(glider, 1, 1), driver.liveCells());
	}

	// 2. 블록 경계 (블록은 8x8. 0~7행/열과 8~15행/열이 서로 다른 블록)

	@Test
	@DisplayName("tick(): 가로 블록 경계(7행과 8행) 너머로 셀이 태어난다")
	void birthAcrossHorizontalBlockEdge() {
		// Given: 7행(위 블록의 아래 가장자리)과 8행(아래 블록의 위 가장자리)에 blinker
		Set<Point> downward = Cells.fromRowColumn("7,2", "7,3", "7,4");		// (8,3)이 아래 블록에서 태어나야 함
		Set<Point> upward = Cells.fromRowColumn("8,18", "8,19", "8,20");		// (7,19)가 위 블록에서 태어나야 함
		place(downward);
		place(upward);

		// When
		driver.tick();

		// Then
		Set<Point> expected = union(Cells.fromRowColumn("6,3", "7,3", "8,3"), Cells.fromRowColumn("7,19", "8,19", "9,19"));
		assertEquals(expected, driver.liveCells());
	}

	@Test
	@DisplayName("tick(): 세로 블록 경계(7열과 8열) 너머로 셀이 태어난다")
	void birthAcrossVerticalBlockEdge() {
		// Given: 7열(왼쪽 블록의 오른쪽 가장자리)과 8열(오른쪽 블록의 왼쪽 가장자리)에 세로 blinker
		Set<Point> rightward = Cells.fromRowColumn("2,7", "3,7", "4,7");		// (3,8)이 오른쪽 블록에서 태어나야 함
		Set<Point> leftward = Cells.fromRowColumn("18,8", "19,8", "20,8");		// (19,7)이 왼쪽 블록에서 태어나야 함
		place(rightward);
		place(leftward);

		// When
		driver.tick();

		// Then
		Set<Point> expected = union(Cells.fromRowColumn("3,6", "3,7", "3,8"), Cells.fromRowColumn("19,7", "19,8", "19,9"));
		assertEquals(expected, driver.liveCells());
	}

	@Test
	@DisplayName("tick(): 네 블록이 만나는 꼭짓점에서 8방향 이웃을 모두 정확히 센다")
	void cornerOfFourBlocksCountsAllEightDirections() {
		// 이웃 방향: 북, 남, 동, 서, 북동, 북서, 남동, 남서 (행 변화, 열 변화)
		String[] names = { "북", "남", "동", "서", "북동", "북서", "남동", "남서" };
		int[][] directions = { {-1, 0}, {1, 0}, {0, 1}, {0, -1}, {-1, 1}, {-1, -1}, {1, 1}, {1, -1} };

		// 대상 셀 두 곳: (8,8)은 블록 (1,1)의 왼쪽 위 꼭짓점, (7,7)은 블록 (0,0)의 오른쪽 아래 꼭짓점
		// 각 대상의 이웃 중 같은 블록 안에 있는 세 방향을 "헬퍼 이웃"으로 씀
		int[][] targets = { {8, 8}, {7, 7} };
		int[][][] helpers = {
			{ {1, 0}, {0, 1}, {1, 1} },			// (8,8)의 남, 동, 남동은 같은 블록
			{ {-1, 0}, {0, -1}, {-1, -1} }		// (7,7)의 북, 서, 북서는 같은 블록
		};

		for (int t = 0; t < targets.length; t++) {
			for (int d = 0; d < directions.length; d++) {
				// Given: 검사할 방향의 이웃 1개 + 같은 블록의 헬퍼 이웃 2개 = 산 이웃 3개
				LifeTestDriver board = newDriver();
				int row = targets[t][0];
				int column = targets[t][1];
				board.click(row + directions[d][0], column + directions[d][1]);

				int added = 0;
				for (int h = 0; h < 3 && added < 2; h++) {
					boolean sameAsChecked = helpers[t][h][0] == directions[d][0] && helpers[t][h][1] == directions[d][1];
					if (!sameAsChecked) {
						board.click(row + helpers[t][h][0], column + helpers[t][h][1]);
						added++;
					}
				}

				// When
				board.tick();

				// Then: 검사한 방향의 이웃을 제대로 셌다면 대상 셀이 태어남
				String where = "대상 (" + row + "," + column + "), " + names[d] + "쪽 이웃";
				assertTrue(board.liveCells().contains(new Point(column, row)), where);
			}
		}
	}

	@Test
	@DisplayName("tick(): 이웃이 모두 다른 블록에 있어도 생존과 과밀 사망이 맞다")
	void survivalAndOvercrowdingAcrossBlockEdges() {
		// Given: (8,8)의 이웃 중 (7,7), (7,8), (7,9), (8,7)은 모두 다른 블록
		LifeTestDriver survives = newDriver();
		place(survives, Cells.fromRowColumn("8,8", "7,7", "7,8"));				// 이웃 2개 → 생존

		LifeTestDriver overcrowded = newDriver();
		place(overcrowded, Cells.fromRowColumn("8,8", "7,7", "7,8", "7,9", "8,7"));	// 이웃 4개 → 사망

		// When
		survives.tick();
		overcrowded.tick();

		// Then
		assertTrue(survives.liveCells().contains(new Point(8, 8)), "다른 블록 이웃 2개: 생존");
		assertFalse(overcrowded.liveCells().contains(new Point(8, 8)), "다른 블록 이웃 4개: 과밀 사망");
	}

	// 3. 보드 가장자리 (보드 밖은 Cell.DUMMY: 항상 죽은 셀)

	@Test
	@DisplayName("tick(): 보드 네 변에서 보드 밖은 죽은 셀로 센다")
	void outsideOfBoardEdgesIsDead() {
		// 가장자리에 놓은 blinker는 보드 밖으로 자랄 수 없어서 두 칸만 남았다가 사라짐
		assertOneTickThenEmpty(Cells.fromRowColumn("0,1", "0,2", "0,3"), Cells.fromRowColumn("0,2", "1,2"), "위쪽 변");
		assertOneTickThenEmpty(Cells.fromRowColumn("63,1", "63,2", "63,3"), Cells.fromRowColumn("62,2", "63,2"), "아래쪽 변");
		assertOneTickThenEmpty(Cells.fromRowColumn("1,0", "2,0", "3,0"), Cells.fromRowColumn("2,0", "2,1"), "왼쪽 변");
		assertOneTickThenEmpty(Cells.fromRowColumn("1,63", "2,63", "3,63"), Cells.fromRowColumn("2,62", "2,63"), "오른쪽 변");
	}

	@Test
	@DisplayName("tick(): 보드 네 꼭짓점에서 이웃이 3개뿐이어도 규칙이 맞다")
	void boardCornersFollowTheRule() {
		// 꼭짓점 셀과 보드 안쪽 방향 (행 방향, 열 방향)
		int[][] corners = { {0, 0, 1, 1}, {0, 63, 1, -1}, {63, 0, -1, 1}, {63, 63, -1, -1} };

		for (int i = 0; i < corners.length; i++) {
			int row = corners[i][0];
			int column = corners[i][1];
			int dRow = corners[i][2];
			int dColumn = corners[i][3];

			// Given: 꼭짓점을 둘러싼 이웃 3칸(ㄱ자)만 살림
			LifeTestDriver board = newDriver();
			board.click(row, column + dColumn);
			board.click(row + dRow, column);
			board.click(row + dRow, column + dColumn);

			// When
			board.tick();

			// Then: 꼭짓점이 태어나 2x2 block이 됨
			Set<Point> block = new HashSet<>();
			block.add(new Point(column, row));
			block.add(new Point(column + dColumn, row));
			block.add(new Point(column, row + dRow));
			block.add(new Point(column + dColumn, row + dRow));
			assertEquals(block, board.liveCells(), "꼭짓점 (" + row + "," + column + ")");
		}
	}

	@Test
	@DisplayName("tick(): 보드는 반대편 가장자리와 이어지지 않는다 (토러스가 아님)")
	void boardDoesNotWrapAround() {
		// Given: 오른쪽 변과 위쪽 변에 blinker
		place(Cells.fromRowColumn("4,63", "5,63", "6,63"));
		place(Cells.fromRowColumn("0,10", "0,11", "0,12"));

		// When
		driver.tick();

		// Then: 반대편이 이어져 있다면 (5,0)과 (63,11)이 이웃 3개로 태어났을 것
		Set<Point> live = driver.liveCells();
		assertFalse(live.contains(new Point(0, 5)), "오른쪽 변의 반대편 (5,0)");
		assertFalse(live.contains(new Point(11, 63)), "위쪽 변의 반대편 (63,11)");
		assertEquals(union(Cells.fromRowColumn("5,62", "5,63"), Cells.fromRowColumn("0,11", "1,11")), live);
	}

	// 4. 정답 계산기(ExpectedGenerationCalculator)와 세대마다 비교

	@Test
	@DisplayName("tick(): 네 방향 glider가 블록 경계와 꼭짓점을 지나 벽에 닿을 때까지 정답 계산 결과와 같다")
	void glidersCrossingBlocksMatchExpected() {
		// 계산을 건너뛰는 블록을 이웃 블록이 가장자리 표시로 깨우는 경로는 이 테스트가 검증함.
		// 다른 경계 테스트는 클릭으로 대상 블록이 이미 활성이라 이웃 조회(edge())만 검증함
		// 오른쪽 아래, 왼쪽 아래, 오른쪽 위, 왼쪽 위로 가는 glider와 출발 위치
		Set<Point>[] gliders = new Set[] {
			Cells.shift(Cells.fromRowColumn("0,1", "1,2", "2,0", "2,1", "2,2"), 2, 2),
			Cells.shift(Cells.fromRowColumn("0,1", "1,0", "2,2", "2,1", "2,0"), 2, 59),
			Cells.shift(Cells.fromRowColumn("2,1", "1,2", "0,0", "0,1", "0,2"), 59, 2),
			Cells.shift(Cells.fromRowColumn("2,1", "1,0", "0,2", "0,1", "0,0"), 59, 59)
		};
		String[] names = { "오른쪽 아래", "왼쪽 아래", "오른쪽 위", "왼쪽 위" };

		for (int i = 0; i < gliders.length; i++) {
			assertMatchesExpected(gliders[i], 250, names[i] + " glider");
		}
	}

	@Test
	@DisplayName("tick(): blinker와 glider를 보드의 모든 위치에 놓아도 정답 계산 결과와 같다")
	void patternsAtEveryPositionMatchExpected() {
		Set<Point> blinker = Cells.fromRowColumn("0,0", "0,1", "0,2");
		Set<Point> glider = Cells.fromRowColumn("0,1", "1,2", "2,0", "2,1", "2,2");

		// 위치마다 새 보드를 만들면 8192번이라 느리므로, 한 보드에 8칸 간격으로 64개를 놓고
		// 시작 위치를 0~7칸씩 옮기며 64번 반복함 → 64x64의 모든 위치를 한 번씩 덮음
		// 보드 전체를 정답과 비교
		for (int startRow = 0; startRow < 8; startRow++) {
			for (int startColumn = 0; startColumn < 8; startColumn++) {
				String where = "시작 위치 (" + startRow + "," + startColumn + ")부터 8칸 간격";
				assertMatchesExpected(Cells.repeatEvery8(blinker, startRow, startColumn), 4, "blinker " + where);
				assertMatchesExpected(Cells.repeatEvery8(glider, startRow, startColumn), 4, "glider " + where);
			}
		}
	}

	@Test
	@DisplayName("tick(): 무작위 패턴을 50세대 진행해도 정답 계산 결과와 같다")
	void randomSoupMatchesExpected() {
		// Given: 시드를 고정해 매번 같은 패턴 (보드의 약 30%가 산 셀)
		Random random = new Random(2026);
		Set<Point> soup = new HashSet<>();
		for (int row = 0; row < LifeTestDriver.SIZE; row++) {
			for (int column = 0; column < LifeTestDriver.SIZE; column++) {
				if (random.nextInt(100) < 30) {
					soup.add(new Point(column, row));
				}
			}
		}

		// When, Then
		assertMatchesExpected(soup, 50, "무작위 패턴");
	}

	// 헬퍼 메서드

	private void ticks(int count) {
		for (int i = 0; i < count; i++) {
			driver.tick();
		}
	}

	private void place(Set<Point> cells) {
		place(driver, cells);
	}

	private static void place(LifeTestDriver board, Set<Point> cells) {
		for (Point cell : cells) {
			board.click(cell.y, cell.x);
		}
	}

	private static Set<Point> union(Set<Point> a, Set<Point> b) {
		Set<Point> all = new HashSet<>(a);
		all.addAll(b);
		return all;
	}

	// 새 보드에 start를 놓고, 1세대 뒤 expectedAfterOneTick이 되고 2세대 뒤 모두 사라지는지 확인
	private void assertOneTickThenEmpty(Set<Point> start, Set<Point> expectedAfterOneTick, String label) {
		LifeTestDriver board = newDriver();
		place(board, start);

		board.tick();
		assertEquals(expectedAfterOneTick, board.liveCells(), label + ": 1세대");
		board.tick();
		assertEquals(Cells.fromRowColumn(), board.liveCells(), label + ": 2세대");
	}

	// 새 보드에 start를 놓고 generations세대 동안 세대마다 ExpectedGenerationCalculator 결과와 같은지 확인
	private void assertMatchesExpected(Set<Point> start, int generations, String label) {
		LifeTestDriver board = newDriver();
		place(board, start);

		Set<Point> expected = start;
		for (int generation = 1; generation <= generations; generation++) {
			board.tick();
			expected = ExpectedGenerationCalculator.nextGeneration(expected);
			assertEquals(expected, board.liveCells(), label + ": " + generation + "세대");
		}
	}
}

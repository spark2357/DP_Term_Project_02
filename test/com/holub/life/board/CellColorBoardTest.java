package com.holub.life.board;

import com.holub.life.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 셀 색 기능 테스트 (Board): 칠한 색이 보드에 남고, 새로 태어나는 셀이 부모 색을 물려받는지 화면 없이 확인
 *
 *   메서드
 *   - brush(행, 열, 색) : 셀을 그 색으로 살림. 다른 색으로 산 셀은 색만 바뀌고, 같은 색으로 산 셀은 죽음
 *   - step()            : 살아남는 셀은 자기 색 유지. 태어나는 셀의 색은 출생 규칙을 따름
 *                         (Clash, 기본값: 부모의 다수 색, 셋 다 다르면 안 태어남 / Blend: 부모 셋 색의 평균)
 *   - setBirthColorRule(규칙) : 출생 규칙을 바꿈. 보드의 모든 셀이 바로 새 규칙을 씀
 *
 * [테스트 방법]
 *   - 셀 색을 읽는 공개 메서드는 따로 만들지 않음. 셀 하나를 1픽셀로 그리게 하고(64×64 영역),
 *     가짜 CellPainter가 받은 색을 좌표별로 모음. 모델이 화면에 넘기는 값 그대로를 확인함
 *
 * [테스트 시나리오]
 *   1. 물려받기: Clash는 2:1이면 다수 색, 1:1:1이면 안 태어남. Blend는 항상 평균 색. 살아남는 셀은 자기 색 유지. 규칙을 바꾸면 멈춘 패턴에도 다음 세대부터 적용
 *   2. 생사: Blend에서는 여러 색으로 칠한 glider와 원래 클릭으로 놓은 glider가 세대마다 같은 자리에 있음.
 *      Clash에서는 세 색이 만나는 곳에서 원래 게임과 달라짐
 *   3. 칠하기: 다른 색으로 칠하면 색만 바뀌고, 같은 색으로 칠하면 죽음. 원래 클릭과 RLE Load, Clear 뒤에는 기본색.
 *      원래 클릭은 다른 색으로 산 셀을 기본색으로 바꾸고, 기본색 셀만 죽임
 *   4. Stamp: 패턴 사각형 밖 셀의 색은 그대로. 기본색으로 산 자리에 찍어도 꺼지지 않음. 색을 주면 그 색으로 찍힘
 *   5. 다시 그리기: 안정된 블록의 셀 색만 바꿔도 바뀐 블록만 그리는 갱신(drawAll=false)에 나옴
 *   6. 저장/불러오기: .life는 색까지 저장하고 불러옴. 색이 없는 예전 .life 파일은 기본색. RLE는 색을 버림
 *      색 부분이 중간에 끊긴 .life 파일은 IOException이고 보드는 그대로. 색 부분 바로 앞에서 끊기면 예전 파일과 같음
 *   7. 스냅샷(Undo): 복원하면 셀 색도 돌아옴. 색만 다른 보드의 스냅샷은 다름 (색만 칠한 편집도 Undo 기록 대상)
 *
 * [커버리지]
 *   - CellColorBoardTest.java 테스트 대상: Board.brush, Board.toggle(색), Board.step(색), Board.setBirthColorRule, Board.stamp(색), Board.load(색), Board.store(색), Board.snapshot/restore(색)
 *   - 테스트 제외 대상
 *     - 실제 화면의 색(SwingCellPainter): 넘겨받은 CellColor를 java.awt.Color로 바꾸기만 함
 */
class CellColorBoardTest {
	private static final CellColor RED = CellColor.DEFAULT;
	private static final CellColor BLUE = new CellColor(0, 0, 255);
	private static final CellColor GREEN = new CellColor(0, 128, 0);
	private static final CellColor YELLOW = new CellColor(255, 255, 0);

	private static Point at(int row, int column) {
		return new Point(column, row);
	}

	//  1. 물려받기

	@Test
	@DisplayName("step() + Clash: 2:1이면 태어나는 셀은 다수 색이고, 살아남는 가운데 셀은 자기 색을 유지한다 (blinker)")
	void newbornTakesMajorityColor() {
		// Given: 가로 blinker. 파랑-빨강-파랑
		Board board = new Board();
		board.brush(10, 10, BLUE);
		board.brush(10, 11, RED);
		board.brush(10, 12, BLUE);

		// When: 세로로 바뀜. (9,11), (11,11)은 부모가 파랑 2, 빨강 1
		board.step();

		// Then
		assertEquals(Map.of(at(9, 11), BLUE, at(10, 11), RED, at(11, 11), BLUE), colors(board));
	}

	@Test
	@DisplayName("step() + Clash(기본값): 부모 셋이 모두 다른 색이면 태어나지 않아, 원래는 계속 도는 blinker가 끊긴다")
	void clashCancelsNewbornWithoutMajority() {
		// Given: 가로 blinker. 파랑-초록-노랑
		Board board = new Board();
		board.brush(10, 10, BLUE);
		board.brush(10, 11, GREEN);
		board.brush(10, 12, YELLOW);

		// When: (9,11), (11,11)은 부모 색이 셋 다 달라 태어나지 않음. 양 끝은 원래대로 죽고 가운데만 살아남음
		board.step();

		// Then
		assertEquals(Map.of(at(10, 11), GREEN), colors(board));
		board.step();
		assertEquals(Set.of(), board.liveCells(), "혼자 남은 가운데 셀도 다음 세대에 죽음");
	}

	@Test
	@DisplayName("step() + Blend: 2:1이어도 태어나는 셀은 세 색의 평균이다 (파랑-빨강-파랑 blinker)")
	void blendMixesEvenWithAMajority() {
		Board board = new Board();
		board.setBirthColorRule(new BlendRule());
		board.brush(10, 10, BLUE);
		board.brush(10, 11, RED);
		board.brush(10, 12, BLUE);

		board.step();

		CellColor mixed = new CellColor(85, 0, 170);
		assertEquals(Map.of(at(9, 11), mixed, at(10, 11), RED, at(11, 11), mixed), colors(board));
	}

	@Test
	@DisplayName("step() + Blend: 부모 셋이 모두 다른 색이면 세 색의 평균으로 태어난다")
	void blendMixesNewbornWithoutMajority() {
		Board board = new Board();
		board.setBirthColorRule(new BlendRule());
		board.brush(10, 10, BLUE);
		board.brush(10, 11, GREEN);
		board.brush(10, 12, YELLOW);

		board.step();

		CellColor mixed = new CellColor(85, 128, 85);
		assertEquals(Map.of(at(9, 11), mixed, at(10, 11), GREEN, at(11, 11), mixed), colors(board));
	}

	@Test
	@DisplayName("setBirthColorRule(): 이미 놓인 셀에도 바로 새 규칙이 적용된다 (Clash → Blend)")
	void switchingRuleAppliesToCellsAlreadyOnTheBoard() {
		Board board = new Board();
		board.brush(10, 10, BLUE);
		board.brush(10, 11, GREEN);
		board.brush(10, 12, YELLOW);
		assertTrue(board.birthColorRule() instanceof ClashRule, "기본값은 Clash");

		board.setBirthColorRule(new BlendRule());
		board.step();

		assertEquals(3, board.liveCells().size());
	}

	@Test
	@DisplayName("step(): 한 색으로만 된 패턴은 블록 경계를 넘어 계속 진행해도 그 색만 남는다 (glider 40세대)")
	void singleColorStaysSingleColor() {
		Board board = new Board();
		for (Point cell : glider(6, 6)) {
			board.brush(cell.y, cell.x, BLUE);
		}

		for (int generation = 0; generation < 40; ++generation) {
			board.step();
		}

		Map<Point, CellColor> colors = colors(board);
		assertEquals(5, colors.size());
		assertTrue(colors.values().stream().allMatch(BLUE::equals), colors::toString);
	}

	//  2. 생사

	@Test
	@DisplayName("step() + Blend: 여러 색으로 칠한 glider와 원래 클릭으로 놓은 glider는 60세대 동안 같은 자리에 있다")
	void blendDoesNotChangeTheGame() {
		List<CellColor> palette = List.of(RED, BLUE, GREEN, YELLOW, BLUE);
		Board colored = new Board();
		colored.setBirthColorRule(new BlendRule());
		Board plain = new Board();
		int i = 0;
		for (Point cell : glider(3, 3)) {
			colored.brush(cell.y, cell.x, palette.get(i++));
			plain.toggle(cell.y, cell.x);
		}

		for (int generation = 0; generation < 60; ++generation) {
			assertEquals(plain.liveCells(), colored.liveCells(), "generation " + generation);
			colored.step();
			plain.step();
		}
	}

	//  3. 칠하기/지우기

	@Test
	@DisplayName("brush(): 산 셀을 다른 색으로 칠하면 죽지 않고 색만 바뀐다")
	void brushingOverWithAnotherColorOnlyChangesColor() {
		Board board = new Board();
		board.brush(5, 5, BLUE);

		board.brush(5, 5, YELLOW);

		assertEquals(Map.of(at(5, 5), YELLOW), colors(board));
	}

	@Test
	@DisplayName("brush(): 같은 색으로 산 셀을 다시 칠하면 죽는다 (원래 클릭의 토글과 같음). 한 번 더 칠하면 다시 산다")
	void brushingWithTheSameColorKills() {
		Board board = new Board();
		board.brush(5, 5, BLUE);

		board.brush(5, 5, BLUE);
		assertEquals(Set.of(), board.liveCells());

		board.brush(5, 5, BLUE);
		assertEquals(Map.of(at(5, 5), BLUE), colors(board));
	}

	@Test
	@DisplayName("toggle(), clear(): 원래 클릭으로 살린 셀은 기본색이다. 칠했던 셀을 Clear한 뒤 클릭해도 기본색이다")
	void toggledCellIsDefault() {
		Board board = new Board();
		board.brush(5, 5, BLUE);
		board.clear();

		board.toggle(5, 5);
		board.toggle(7, 7);

		assertEquals(Map.of(at(5, 5), CellColor.DEFAULT, at(7, 7), CellColor.DEFAULT), colors(board));
	}

	@Test
	@DisplayName("toggle(): 다른 색으로 산 셀을 원래 클릭으로 누르면 죽지 않고 기본색(빨강)이 되고, 다시 누르면 죽는다")
	void toggleOnColoredCellTurnsItDefault() {
		// Given
		Board board = new Board();
		board.brush(5, 5, BLUE);

		// When
		board.toggle(5, 5);

		// Then: 빨강으로 바뀜. 빨강 셀을 다시 누르면 죽음
		assertEquals(Map.of(at(5, 5), RED), colors(board));
		board.toggle(5, 5);
		assertEquals(Set.of(), board.liveCells());
	}

	@Test
	@DisplayName("load() + RLE: 파일에는 색이 없으므로 Load한 셀은 기본색이다")
	void loadedCellsAreDefault() throws IOException {
		Board board = new Board();
		board.brush(0, 0, BLUE);
		String rle = "#R 10 20\nx = 2, y = 1, rule = B3/S23\n2o!\n";

		board.load(new ByteArrayInputStream(rle.getBytes(StandardCharsets.US_ASCII)),
				new RleFormat(board.widthInCells()));

		assertEquals(Map.of(at(20, 10), CellColor.DEFAULT, at(20, 11), CellColor.DEFAULT), colors(board));
	}

	@Test
	@DisplayName("setBirthColorRule(): Clash에서 멈춘 패턴도 Blend로 바꾸면 다음 세대에 새 규칙으로 태어난다")
	void switchingRuleWakesStillPattern() {
		// Given: 빨강, 초록, 파랑 ㄱ자. Clash에서는 (11,11)이 1:1:1이라 안 태어나고 멈춤
		Board board = new Board();
		board.brush(10, 10, RED);
		board.brush(10, 11, GREEN);
		board.brush(11, 10, BLUE);
		for (int generation = 0; generation < 5; ++generation) {
			board.step();
		}
		assertEquals(3, colors(board).size());

		// When
		board.setBirthColorRule(new BlendRule());
		board.step();

		// Then: (11,11)이 세 색의 평균으로 태어남
		assertEquals(new CellColor(85, 43, 85), colors(board).get(at(11, 11)));
		assertEquals(4, colors(board).size());
	}

	//  4. Stamp

	@Test
	@DisplayName("stamp(): 패턴 사각형 밖 셀의 색은 그대로이고, 찍힌 셀은 기본색이다")
	void stampKeepsColorsOutside() {
		Board board = new Board();
		board.brush(0, 0, BLUE);
		board.brush(40, 40, GREEN);
		CellPattern dot = new CellPattern(1, 1, Set.of(new Point(0, 0)));

		board.stamp(dot, 20, 20, CellColor.DEFAULT);

		assertEquals(Map.of(at(0, 0), BLUE, at(40, 40), GREEN, at(20, 20), CellColor.DEFAULT), colors(board));
	}

	@Test
	@DisplayName("stamp(): 기본색으로 이미 살아 있는 자리에 찍어도 꺼지지 않고 살아 있다 (같은 색 칠하기의 토글을 피함)")
	void stampOverDefaultColoredCellKeepsItAlive() {
		Board board = new Board();
		board.toggle(20, 20);		// 기본색
		board.brush(20, 21, BLUE);
		CellPattern pair = new CellPattern(2, 1, Set.of(new Point(0, 0), new Point(1, 0)));

		board.stamp(pair, 20, 20, CellColor.DEFAULT);

		assertEquals(Map.of(at(20, 20), CellColor.DEFAULT, at(20, 21), CellColor.DEFAULT), colors(board));
	}

	@Test
	@DisplayName("stamp(색): 찍힌 셀은 그 색으로 살아난다")
	void stampWithColor() {
		Board board = new Board();
		CellPattern pair = new CellPattern(2, 1, Set.of(new Point(0, 0), new Point(1, 0)));

		board.stamp(pair, 20, 20, BLUE);

		assertEquals(Map.of(at(20, 20), BLUE, at(20, 21), BLUE), colors(board));
	}

	//  5. 다시 그리기

	@Test
	@DisplayName("brush(): 안정된 블록의 셀 색만 바꿔도, 바뀐 블록만 그리는 갱신에 새 색으로 나온다")
	void recolorIsRedrawnByPartialRefresh() {
		// Given: 2x2 block(정지 패턴)이 안정되어, 바뀐 블록만 그리면 아무것도 그려지지 않는 상태
		Board board = new Board();
		board.brush(20, 20, BLUE);
		board.brush(20, 21, BLUE);
		board.brush(21, 20, BLUE);
		board.brush(21, 21, BLUE);
		board.step();
		partialColors(board);
		assertEquals(Map.of(), partialColors(board));

		// When: 생사는 그대로 두고 색만 바꿈
		board.brush(20, 20, YELLOW);

		// Then
		assertEquals(YELLOW, partialColors(board).get(at(20, 20)));
	}

	//  6. 저장/불러오기

	@Test
	@DisplayName("store() → load() + .life: 칠한 색이 그대로 돌아온다")
	void lifeFileKeepsColors() throws IOException {
		// Given
		Board board = new Board();
		board.brush(1, 1, BLUE);
		board.brush(1, 2, GREEN);
		board.toggle(30, 40);
		LifeFormatAdapter life = new LifeFormatAdapter(board.widthInCells());
		ByteArrayOutputStream file = new ByteArrayOutputStream();
		board.store(file, life);

		// When: 다른 보드에 불러옴
		Board loaded = new Board();
		loaded.brush(5, 5, YELLOW);
		loaded.load(new ByteArrayInputStream(file.toByteArray()), life);

		// Then
		assertEquals(Map.of(at(1, 1), BLUE, at(1, 2), GREEN, at(30, 40), RED), colors(loaded));
	}

	@Test
	@DisplayName("load() + .life: 색이 없는 예전 파일(Point 목록만)은 기본색으로 불러온다")
	void oldLifeFileLoadsAsDefault() throws IOException {
		// Given: 원래 Memento처럼 Point 목록 하나만 씀
		LinkedList<Point> cells = new LinkedList<>();
		cells.add(new Point(3, 4));
		cells.add(new Point(5, 6));
		ByteArrayOutputStream file = new ByteArrayOutputStream();
		new ObjectOutputStream(file).writeObject(cells);

		// When
		Board board = new Board();
		board.load(new ByteArrayInputStream(file.toByteArray()), new LifeFormatAdapter(board.widthInCells()));

		// Then
		assertEquals(Map.of(new Point(3, 4), RED, new Point(5, 6), RED), colors(board));
	}

	@Test
	@DisplayName("store() + .life: 색은 RGB 숫자로 저장되어 파일에 CellColor 클래스 이름이 들어가지 않는다")
	void lifeFileDoesNotDependOnCellColorClass() throws IOException {
		Board board = new Board();
		board.brush(1, 1, BLUE);
		ByteArrayOutputStream file = new ByteArrayOutputStream();

		board.store(file, new LifeFormatAdapter(board.widthInCells()));

		assertFalse(file.toString(StandardCharsets.ISO_8859_1).contains("CellColor"));
	}

	@Test
	@DisplayName("load() + .life: 색 자리에 Map이 아닌 값(null)이 있으면 IOException이고 보드는 그대로다")
	void brokenColorsThrowIOException() throws IOException {
		LinkedList<Point> cells = new LinkedList<>();
		cells.add(new Point(3, 4));
		ByteArrayOutputStream file = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(file);
		out.writeObject(cells);
		out.writeObject(null);
		out.flush();
		Board board = new Board();
		board.brush(5, 5, BLUE);

		assertThrows(IOException.class, () -> board.load(new ByteArrayInputStream(file.toByteArray()),
				new LifeFormatAdapter(board.widthInCells())));
		assertEquals(Map.of(at(5, 5), BLUE), colors(board));
	}

	@Test
	@DisplayName("load() + .life: 색 부분을 읽다가 끊긴 파일은 IOException이고 보드는 그대로다")
	void truncatedColorsThrowIOException() throws Exception {
		// Given: 색을 칠해 저장한 파일과, 그 안에서 Point 목록이 끝나는 위치
		byte[] full = coloredLifeFile();
		int boundary = pointListLength(full);
		assertTrue(boundary < full.length);

		for (int cut : new int[] {boundary + 1, (boundary + full.length) / 2, full.length - 1}) {
			Board board = new Board();
			board.brush(5, 5, BLUE);
			byte[] truncated = Arrays.copyOf(full, cut);

			// When, Then
			assertThrows(IOException.class, () -> board.load(new ByteArrayInputStream(truncated),
					new LifeFormatAdapter(board.widthInCells())), "cut at " + cut);
			assertEquals(Map.of(at(5, 5), BLUE), colors(board), "cut at " + cut);
		}
	}

	@Test
	@DisplayName("load() + .life: Point 목록 바로 뒤(색 부분 앞)에서 끊긴 파일은 예전 파일처럼 기본색으로 불러온다")
	void cutBeforeColorsLoadsAsDefault() throws Exception {
		// Given
		byte[] full = coloredLifeFile();
		byte[] pointsOnly = Arrays.copyOf(full, pointListLength(full));

		// When
		Board board = new Board();
		board.load(new ByteArrayInputStream(pointsOnly), new LifeFormatAdapter(board.widthInCells()));

		// Then
		assertEquals(Map.of(at(1, 1), RED, at(1, 2), RED, at(30, 40), RED), colors(board));
	}

	@Test
	@DisplayName("store() → load() + RLE: 위치만 저장되고 색은 기본색이 된다")
	void rleFileDropsColors() throws IOException {
		// Given
		Board board = new Board();
		board.brush(10, 10, BLUE);
		board.brush(10, 11, GREEN);
		RleFormat rle = new RleFormat(board.widthInCells());
		ByteArrayOutputStream file = new ByteArrayOutputStream();
		board.store(file, rle);

		// When
		Board loaded = new Board();
		loaded.load(new ByteArrayInputStream(file.toByteArray()), rle);

		// Then
		assertEquals(Map.of(at(10, 10), RED, at(10, 11), RED), colors(loaded));
	}

	//  7. 스냅샷(Undo)

	@Test
	@DisplayName("restore(): 색을 칠한 보드의 스냅샷으로 복원하면 셀 색도 그때로 돌아온다")
	void restoreKeepsColors() {
		// Given
		Board board = new Board();
		board.brush(10, 10, BLUE);
		board.brush(10, 11, GREEN);
		BoardSnapshot snapshot = board.snapshot();

		// When
		board.brush(10, 10, YELLOW);
		board.clear();
		board.restore(snapshot);

		// Then
		assertEquals(Map.of(at(10, 10), BLUE, at(10, 11), GREEN), colors(board));
	}

	@Test
	@DisplayName("snapshot().equals(): 살아 있는 자리가 같아도 색이 다르면 다른 스냅샷이다")
	void snapshotsDifferByColor() {
		// Given
		Board board = new Board();
		board.brush(10, 10, BLUE);
		BoardSnapshot blue = board.snapshot();

		// When
		board.brush(10, 10, GREEN);

		// Then
		assertNotEquals(blue, board.snapshot());
	}

	// ---------------------------------------------------------------

	/** 색을 칠한 보드를 .life로 저장한 바이트 */
	private static byte[] coloredLifeFile() throws IOException {
		Board board = new Board();
		board.brush(1, 1, BLUE);
		board.brush(1, 2, GREEN);
		board.toggle(30, 40);
		ByteArrayOutputStream file = new ByteArrayOutputStream();
		board.store(file, new LifeFormatAdapter(board.widthInCells()));
		return file.toByteArray();
	}

	/** .life 파일 앞부분의 Point 목록만 같은 방식으로 다시 쓴 길이 (= 색 부분이 시작하는 위치) */
	private static int pointListLength(byte[] lifeFile) throws Exception {
		Object points = new ObjectInputStream(new ByteArrayInputStream(lifeFile)).readObject();
		ByteArrayOutputStream file = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(file);
		out.writeObject(points);
		out.flush();
		byte[] pointsOnly = file.toByteArray();
		assertArrayEquals(pointsOnly, Arrays.copyOf(lifeFile, pointsOnly.length), "파일 앞부분이 Point 목록과 같아야 함");
		return pointsOnly.length;
	}

	/** 왼쪽 위가 (row, column)인 3x3 사각형 안의 glider */
	private static Set<Point> glider(int row, int column) {
		return Cells.shift(Cells.fromRowColumn("0,1", "1,2", "2,0", "2,1", "2,2"), row, column);
	}

	/** 보드 전체를 그려 산 셀의 색을 모음 (x = 열, y = 행) */
	private static Map<Point, CellColor> colors(Board board) {
		return draw(board, true);
	}

	/** 바뀐 블록만 그려(drawAll = false) 그 안에 있는 산 셀의 색을 모음 */
	private static Map<Point, CellColor> partialColors(Board board) {
		return draw(board, false);
	}

	private static Map<Point, CellColor> draw(Board board, boolean drawAll) {
		int width = board.widthInCells();
		ColorRecorder recorder = new ColorRecorder();
		board.redraw(recorder, new Rectangle(0, 0, width, width), drawAll);	// 셀 하나 = 1픽셀
		return recorder.colors;
	}

	/** 산 셀의 색을 좌표별로 기록하는 가짜 Painter */
	private static class ColorRecorder implements CellPainter {
		final Map<Point, CellColor> colors = new HashMap<>();

		public void paintCell(Rectangle here, boolean alive) {
			fail("Resident는 상태를 넘기는 paintCell(here, state)를 불러야 함");
		}

		public void paintCell(Rectangle here, CellState state) {
			state.getColor().ifPresent(color -> colors.put(new Point(here.x, here.y), color));
		}

		public void paintBorder(Rectangle here, boolean active) {
		}
	}
}

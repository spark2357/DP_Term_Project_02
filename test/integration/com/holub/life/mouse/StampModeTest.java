package com.holub.life.mouse;

import com.holub.life.*;
import com.holub.life.command.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * E. 패턴 찍기 유스케이스 통합 테스트: 스탬프 모드(StampMode)를 MouseController에 넣고 마우스 입력을 보내 화면 없이 확인
 * 진짜 LifeGame·Board에 가짜 시계(FakeTicker)와 가짜 리스너(RecordingGameListener)를 넣음. 조립은 Life.main과 같음 (기본 모드는 ToggleMode, 펜 하나를 같이 씀)
 *
 * [테스트 시나리오]
 *   1. 찍기: 마우스 셀을 왼쪽 위로 해서 패턴 사각형 안을 덮어씀 (산 칸은 펜 색, 빈 칸은 죽임). 사각형 밖은 그대로, 보드 밖은 잘라냄
 *   2. 회전 (대안 3a): 오른쪽 클릭은 패턴만 시계 방향 90도 돌리고, 보드와 화면 알림은 그대로
 *   3. 찍은 뒤: 실행 중이면 멈추지 않고, 스탬프 모드도 그대로
 *   4. 펜 색: 스탬프 모드를 켠 뒤에 바꾼 펜 색으로도 찍힘
 *   5. 미리보기 (대안 1a): 마우스가 움직이면 그 셀에 미리보기를 파란색으로 그리고, 보드 밖으로 나가면 지움
 *   6. 스탬프 모드 중 Grid 편집: Clear나 Load를 해도 스탬프 모드가 그대로라 다음 클릭은 또 찍힘
 *
 * [커버리지]
 *   - StampModeTest.java 테스트 대상: StampMode (clicked, rightClicked, moved, exited, drawPreview, pattern), MouseController,
 *     스탬프 모드 중 ClearCommand·LoadCommand
 *   - 미리보기 색은 BufferedImage에 그려 픽셀 색으로 확인함 (파란색 = 파랑 값이 빨강·초록보다 큼)
 *   - 테스트 제외 대상
 *     - 스탬프 모드 켜고 끄기(대안 3b), 파일 읽기: GridCommandTest, StampCommandTest, StampCommandFailureTest에서 확인
 *     - 찍은 것의 Undo: LifeGameUndoTest, UndoRedoUseCaseTest에서 확인
 *     - 미리보기를 실제 창에 그리는 시점 (Universe, GUI)
 */
@Tag("extension")
class StampModeTest{
	private static final CellColor BLUE = new CellColor(0, 0, 255);
	private static final CellColor GREEN = new CellColor(0, 128, 0);
	private static final CellColor YELLOW = new CellColor(255, 255, 0);

	private static final int PIXELS_PER_CELL = 4;		// 미리보기를 그릴 때 셀 하나의 픽셀 수

	// 왼쪽 위가 0,0인 3x3 glider (x = 열, y = 행)
	private static final CellPattern GLIDER = new CellPattern(3, 3, Cells.fromRowColumn("0,1", "1,2", "2,0", "2,1", "2,2"));

	private final Board board = new Board();
	private final FakeTicker ticker = new FakeTicker();
	private final LifeGame game = new LifeGame(board, ticker);
	private final RecordingGameListener view = new RecordingGameListener();
	private final Pen pen = new Pen(CellColor.DEFAULT);
	private final MouseController mouse = new MouseController(new ToggleMode(game, pen));
	private StampMode stamp;

	@BeforeEach
	void turnOnStampMode(){
		stamp = new StampMode(game, GLIDER, pen);
		mouse.setMode(stamp);
	}

	//  1. 찍기

	@Test
	@DisplayName("clicked(): 패턴 사각형 안은 패턴대로 덮어써 산 칸은 펜 색, 빈 칸은 죽이고, 사각형 밖 셀은 자리와 색이 그대로다")
	void stampOverwritesRectangleWithPenColor(){
		// Given: 사각형(10~12행, 20~22열) 안의 빈 칸 자리 (10,20)에 노랑, 산 칸 자리 (10,21)에 초록. 밖에는 (9,20) 노랑, (13,23) 초록
		board.brush(10, 20, YELLOW);
		board.brush(10, 21, GREEN);
		board.brush(9, 20, YELLOW);
		board.brush(13, 23, GREEN);
		pen.setColor(BLUE);

		// When
		mouse.clicked(10, 20);

		// Then
		Map<Point, CellColor> expected = new HashMap<>(BoardColors.allIn(BLUE,
				Cells.fromRowColumn("10,21", "11,22", "12,20", "12,21", "12,22")));
		expected.put(new Point(20, 9), YELLOW);
		expected.put(new Point(23, 13), GREEN);
		assertEquals(expected, BoardColors.of(board));
	}

	@Test
	@DisplayName("clicked(): 보드 가장자리에 찍으면 보드 밖으로 나가는 부분은 잘라내고, 안쪽만 찍는다")
	void stampClipsAtBoardEdge(){
		// Given: 사각형(62~64행, 61~63열)은 64행이 보드 밖. 사각형 안의 빈 칸 (63,62)에 셀이 있음
		board.toggle(63, 62);

		// When
		mouse.clicked(62, 61);

		// Then: glider의 (0,1), (1,2)만 보드 안. (63,62)는 빈 칸이라 죽음
		assertEquals(Cells.fromRowColumn("62,62", "63,63"), board.liveCells());
	}

	//  2. 회전

	@Test
	@DisplayName("rightClicked(): 오른쪽 클릭은 패턴만 시계 방향 90도로 돌리고, 보드는 그대로이며 화면에 알리지 않는다")
	void rightClickRotatesWithoutChangingBoard(){
		// Given
		board.toggle(5, 5);
		game.addGameListener(view);

		// When
		mouse.rightClicked(5, 5);

		// Then
		CellPattern rotated = new CellPattern(3, 3, Cells.fromRowColumn("0,0", "1,0", "1,2", "2,0", "2,1"));
		assertEquals(rotated, stamp.pattern());
		assertEquals(Cells.fromRowColumn("5,5"), board.liveCells());
		assertEquals(List.of(), view.events);
	}

	//  3. 찍은 뒤

	@Test
	@DisplayName("clicked(): 실행 중에 찍어도 게임은 멈추지 않는다")
	void stampKeepsGameRunning(){
		// Given
		game.start(100);

		// When
		mouse.clicked(10, 10);

		// Then
		assertTrue(ticker.running);
	}

	@Test
	@DisplayName("clicked(): 찍은 뒤에도 스탬프 모드가 그대로라 다시 클릭하면 또 찍힌다")
	void stampModeStaysOnAfterStamp(){
		// When
		mouse.clicked(10, 10);
		mouse.clicked(30, 30);

		// Then
		assertSame(stamp, mouse.mode());
		Set<Point> twoGliders = Cells.fromRowColumn(
				"10,11", "11,12", "12,10", "12,11", "12,12",
				"30,31", "31,32", "32,30", "32,31", "32,32");
		assertEquals(twoGliders, board.liveCells());
	}

	//  4. 펜 색

	@Test
	@DisplayName("clicked(): 스탬프 모드를 켠 뒤에 펜 색을 바꾸면 바꾼 색으로 찍힌다")
	void stampUsesCurrentPenColor(){
		// Given: 스탬프 모드는 기본색 펜으로 켜졌음
		pen.setColor(GREEN);

		// When
		mouse.clicked(10, 10);

		// Then
		assertEquals(BoardColors.allIn(GREEN, Cells.fromRowColumn("10,11", "11,12", "12,10", "12,11", "12,12")),
				BoardColors.of(board));
	}

	//  5. 미리보기

	@Test
	@DisplayName("moved(), exited(): 마우스가 다른 셀로 움직이면 그 셀을 왼쪽 위로 미리보기를 그리고, 보드 밖으로 나가면 지운다")
	void movedAndExitedTrackPreview(){
		// When, Then: 처음 들어옴 → 다시 그림. 같은 셀 → 안 그림. 다른 셀 → 다시 그림
		assertTrue(mouse.moved(2, 3));
		assertFalse(mouse.moved(2, 3));
		assertTrue(mouse.moved(4, 5));

		// Then: (4,5)를 왼쪽 위로 glider 미리보기. glider의 산 칸 (0,1)은 칠하고 빈 칸 (1,1)은 칠하지 않음
		BufferedImage shown = preview();
		assertTrue(isPainted(shown, 4, 5 + 1));
		assertFalse(isPainted(shown, 4 + 1, 5 + 1));
		assertFalse(isPainted(shown, 2, 3 + 1), "이전 셀에는 그리지 않음");

		// When, Then: 보드 밖으로 나감 → 미리보기를 지우려고 다시 그림. 한 번 더 나가도 지울 것이 없음
		assertTrue(mouse.exited());
		assertFalse(mouse.exited());
		assertFalse(isPainted(preview(), 4, 5 + 1));
	}

	@Test
	@DisplayName("drawPreview(): 미리보기는 패턴의 산 칸과 사각형 테두리를 파란색으로 그린다")
	void previewIsDrawnInBlue(){
		// Given
		mouse.moved(4, 5);

		// When
		BufferedImage shown = preview();

		// Then: 산 칸 (0,1)의 가운데 픽셀, 사각형 왼쪽 위 테두리 픽셀
		assertBlue(colorAtCellCenter(shown, 4, 5 + 1));
		assertBlue(new Color(shown.getRGB(5 * PIXELS_PER_CELL, 4 * PIXELS_PER_CELL), true));
	}

	//  6. 스탬프 모드 중 Grid 편집

	@Test
	@DisplayName("ClearCommand.execute(): 스탬프 모드에서 Clear해도 스탬프 모드가 그대로라 다음 클릭은 또 찍힌다")
	void clearKeepsStampMode(){
		// Given
		mouse.clicked(10, 10);

		// When
		new ClearCommand(game).execute();
		mouse.clicked(30, 30);

		// Then
		assertSame(stamp, mouse.mode());
		assertEquals(Cells.fromRowColumn("30,31", "31,32", "32,30", "32,31", "32,32"), board.liveCells());
	}

	@Test
	@DisplayName("LoadCommand.execute(): 스탬프 모드에서 Load해도 스탬프 모드가 그대로라 다음 클릭은 또 찍힌다")
	void loadKeepsStampMode(){
		// Given: 위치(#R)가 없는 glider 파일. Load하면 보드 가운데(왼쪽 위 30,30)에 놓임
		FakeDialogs dialogs = new FakeDialogs();
		dialogs.nextFile = new File("test/resources/glider.rle");
		PatternFormats formats = new PatternFormats(List.of(new LifeFormatAdapter(LifeTestDriver.SIZE), new RleFormat(LifeTestDriver.SIZE)));

		// When
		new LoadCommand(game, formats, dialogs).execute();
		mouse.clicked(10, 10);

		// Then
		assertSame(stamp, mouse.mode());
		Set<Point> loadedAndStamped = Cells.fromRowColumn(
				"30,31", "31,32", "32,30", "32,31", "32,32",
				"10,11", "11,12", "12,10", "12,11", "12,12");
		assertEquals(loadedAndStamped, board.liveCells());
	}

	//  헬퍼

	private BufferedImage preview(){
		int width = board.widthInCells() * PIXELS_PER_CELL;
		BufferedImage image = new BufferedImage(width, width, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		mouse.drawPreview(g, PIXELS_PER_CELL);
		g.dispose();
		return image;
	}

	// 셀 (row, column)의 가운데 픽셀에 무엇이든 그려졌는지 (미리보기 테두리는 셀 가운데를 지나지 않음)
	private static boolean isPainted(BufferedImage image, int row, int column){
		return colorAtCellCenter(image, row, column).getAlpha() != 0;
	}

	private static Color colorAtCellCenter(BufferedImage image, int row, int column){
		int x = column * PIXELS_PER_CELL + PIXELS_PER_CELL / 2;
		int y = row * PIXELS_PER_CELL + PIXELS_PER_CELL / 2;
		return new Color(image.getRGB(x, y), true);
	}

	// 그려진 픽셀이고, 파랑 값이 빨강·초록보다 큰지
	private static void assertBlue(Color color){
		assertAll(
				() -> assertNotEquals(0, color.getAlpha(), "그려져야 함"),
				() -> assertTrue(color.getBlue() > color.getRed(), "파랑 > 빨강: " + color),
				() -> assertTrue(color.getBlue() > color.getGreen(), "파랑 > 초록: " + color));
	}
}

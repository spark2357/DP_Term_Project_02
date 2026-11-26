package com.holub.life.command;

import com.holub.life.*;
import com.holub.life.mouse.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 셀 색과 파일 통합 테스트 (셀 색 기능의 펜 색 칠하기와 E. 패턴 찍기 + Grid → Store/Load): 펜 색으로 칠하고 찍은 보드를 Store/Load Command로 파일에 쓰고 읽음
 * 진짜 LifeGame·Board, 마우스 모드(ToggleMode, StampMode)에 가짜 파일 창(FakeDialogs)과 가짜 리스너를 넣어 화면 없이 실행함. 파일은 임시 폴더(@TempDir)에 씀
 *
 * [테스트 시나리오]
 *   1. .life: 칠하고 찍은 보드를 Store → Clear → Load하면 자리와 색이 같음
 *   2. 예전 .life: 원본 코드로 저장한 파일(test/resources/original-store.life)은 기대한 자리에 모두 기본색(빨강)으로 열림
 *   3. 색 부분이 잘린 .life: 오류 창 하나, 보드와 색은 그대로, 화면 알림 없음
 *   4. .rle: 위치만 저장하므로 Load하면 모두 기본색
 *   5. 새 .life 파일 구조: 맨 앞 객체는 Point 목록 (예전 코드도 읽을 수 있는 부분)
 *
 * [커버리지]
 *   - ColorFileCommandTest.java 테스트 대상: StoreCommand, LoadCommand, ClearCommand와 ToggleMode, StampMode(펜 색), LifeFormatAdapter, RleFormat
 *   - 테스트 제외 대상
 *     - 실패한 Load가 Undo 기록에 남지 않는지: LifeGameUndoTest에서 확인
 *     - 형식 하나만의 읽기/쓰기 세부: CellColorBoardTest, LifeFormatAdapterTest, RleFormatTest에서 확인
 */
@Tag("extension")
class ColorFileCommandTest{
	private static final int SIZE = LifeTestDriver.SIZE;
	private static final CellColor BLUE = new CellColor(0, 0, 255);
	private static final CellColor GREEN = new CellColor(0, 128, 0);
	private static final CellColor YELLOW = new CellColor(255, 255, 0);

	@TempDir
	Path folder;

	private final Board board = new Board();
	private final FakeTicker ticker = new FakeTicker();
	private final LifeGame game = new LifeGame(board, ticker);
	private final FakeDialogs dialogs = new FakeDialogs();
	private final PatternFormats formats = new PatternFormats(List.of(new LifeFormatAdapter(SIZE), new RleFormat(SIZE)));
	private final Pen pen = new Pen(CellColor.DEFAULT);
	private final MouseController mouse = new MouseController(new ToggleMode(game, pen));
	private final RecordingGameListener view = new RecordingGameListener();

	//  1. .life

	@Test
	@DisplayName("execute(): 펜 색으로 칠하고 찍은 보드를 .life로 Store → Clear → Load하면 자리와 색이 같다")
	void storedLifeRestoresPenColors(){
		// Given
		paintColoredBoard();
		Map<Point, CellColor> before = BoardColors.of(board);
		dialogs.nextFile = folder.resolve("colors.life").toFile();
		new StoreCommand(game, formats, dialogs).execute();

		// When
		new ClearCommand(game).execute();
		new LoadCommand(game, formats, dialogs).execute();

		// Then
		assertEquals(before, BoardColors.of(board));
		assertEquals(List.of(), dialogs.errors);
	}

	//  2. 예전 .life

	@Test
	@DisplayName("execute(): 원본 코드로 저장한 .life 파일을 Load하면 기대한 자리에 모두 기본색(빨강)으로 열린다")
	void originalLifeFileLoadsInDefaultColor(){
		// Given
		board.brush(5, 5, BLUE);
		dialogs.nextFile = new File("test/resources/original-store.life");

		// When
		new LoadCommand(game, formats, dialogs).execute();

		// Then
		Set<Point> expected = Cells.fromRowColumn(
				"1,2", "2,3", "3,1", "3,2", "3,3",		// glider
				"7,7", "7,8", "8,7", "8,8",				// 네 블록이 만나는 곳의 block
				"0,63", "63,0", "63,63");				// 보드 꼭짓점
		assertEquals(BoardColors.allIn(CellColor.DEFAULT, expected), BoardColors.of(board));
		assertEquals(List.of(), dialogs.errors);
	}

	//  3. 색 부분이 잘린 .life

	@Test
	@DisplayName("execute(): 색 부분 중간에서 잘린 .life 파일을 Load하면 오류 창을 하나 띄우고, 보드와 색은 그대로이고, 화면에 알리지 않는다")
	void truncatedColorPartFailsAndKeepsBoard() throws Exception{
		// Given: 색을 칠한 보드를 저장한 파일을 색 부분 가운데에서 자름
		paintColoredBoard();
		File full = folder.resolve("full.life").toFile();
		dialogs.nextFile = full;
		new StoreCommand(game, formats, dialogs).execute();
		byte[] bytes = Files.readAllBytes(full.toPath());
		int boundary = pointListLength(bytes);
		assertTrue(boundary < bytes.length, "색 부분이 있어야 함");
		Path truncated = folder.resolve("truncated.life");
		Files.write(truncated, Arrays.copyOf(bytes, (boundary + bytes.length) / 2));

		// 지금 보드는 저장한 것과 다른 보드
		new ClearCommand(game).execute();
		board.brush(40, 40, YELLOW);
		Map<Point, CellColor> before = BoardColors.of(board);
		game.addGameListener(view);
		dialogs.nextFile = truncated.toFile();

		// When
		new LoadCommand(game, formats, dialogs).execute();

		// Then
		assertEquals(1, dialogs.errors.size());
		assertEquals(before, BoardColors.of(board));
		assertEquals(List.of(), view.events);
	}

	//  4. .rle

	@Test
	@DisplayName("execute(): .rle로 Store한 색 보드를 Load하면 자리는 같고 색은 모두 기본색(빨강)이다")
	void rleStoreDropsColors(){
		// Given
		paintColoredBoard();
		Set<Point> positions = board.liveCells();
		dialogs.nextFile = folder.resolve("colors.rle").toFile();
		new StoreCommand(game, formats, dialogs).execute();

		// When
		new ClearCommand(game).execute();
		new LoadCommand(game, formats, dialogs).execute();

		// Then
		assertEquals(BoardColors.allIn(CellColor.DEFAULT, positions), BoardColors.of(board));
		assertEquals(List.of(), dialogs.errors);
	}

	//  5. 새 .life 파일 구조

	@Test
	@DisplayName("execute(): Store로 새로 저장한 .life 파일의 맨 앞 객체는 살아 있는 셀의 Point 목록이다 (예전 코드도 읽는 부분)")
	void newLifeFileStartsWithPointList() throws Exception{
		// Given
		paintColoredBoard();
		File file = folder.resolve("colors.life").toFile();
		dialogs.nextFile = file;

		// When
		new StoreCommand(game, formats, dialogs).execute();

		// Then
		Object first;
		try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file.toPath()))){
			first = in.readObject();
		}
		Collection<?> points = assertInstanceOf(Collection.class, first);
		assertEquals(board.liveCells(), new HashSet<>(points));
		assertEquals(board.liveCells().size(), points.size(), "셀마다 한 번씩");
	}

	//  헬퍼

	// 펜 색 셋으로 칠하고(ToggleMode), 초록 펜으로 blinker를 찍음(StampMode)
	private void paintColoredBoard(){
		pen.setColor(BLUE);
		mouse.clicked(1, 1);
		pen.setColor(YELLOW);
		mouse.clicked(1, 2);
		pen.setColor(CellColor.DEFAULT);
		mouse.clicked(30, 40);

		pen.setColor(GREEN);
		mouse.setMode(new StampMode(game, new CellPattern(3, 1, Cells.fromRowColumn("0,0", "0,1", "0,2")), pen));
		mouse.clicked(20, 20);
		mouse.resetMode();

		assertEquals(Map.of(
				new Point(1, 1), BLUE, new Point(2, 1), YELLOW, new Point(40, 30), CellColor.DEFAULT,
				new Point(20, 20), GREEN, new Point(21, 20), GREEN, new Point(22, 20), GREEN), BoardColors.of(board));
	}

	// .life 파일 앞부분의 Point 목록만 같은 방식으로 다시 쓴 길이 (= 색 부분이 시작하는 위치)
	private static int pointListLength(byte[] lifeFile) throws Exception{
		Object points = new ObjectInputStream(new ByteArrayInputStream(lifeFile)).readObject();
		ByteArrayOutputStream file = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(file);
		out.writeObject(points);
		out.flush();
		byte[] pointsOnly = file.toByteArray();
		assertArrayEquals(pointsOnly, Arrays.copyOf(lifeFile, pointsOnly.length), "파일 앞부분이 Point 목록과 같아야 함");
		return pointsOnly.length;
	}
}

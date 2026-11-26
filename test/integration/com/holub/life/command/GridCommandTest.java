package com.holub.life.command;

import com.holub.life.*;
import com.holub.life.mouse.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Tag;

/**
 * Grid 메뉴 Command 테스트: 파일 창과 오류 창을 가짜 Dialogs로, 화면을 알림을 기록하는 가짜 리스너로 바꿔 GUI 없이 검증
 *
 *   Command
 *   - ClearCommand  : Grid → Clear
 *   - LoadCommand   : Grid → Load. 게임을 멈추고, 고른 파일을 확장자에 맞는 형식으로 읽음
 *   - StoreCommand  : Grid → Store. 게임을 멈추고, 고른 파일에 확장자에 맞는 형식으로 씀
 *   - StampCommand  : Grid → Stamp. .rle 패턴을 골라 스탬프 모드를 켜고, 다시 누르면 끔
 *
 * [테스트 시나리오]
 *   1. Clear: 보드를 비우고 화면에 다시 그리라고(boardEdited) 알림
 *   2. Load/Store: .life 파일을 Load하면 보드가 파일 내용으로 바뀜.
 *      .rle는 Store할 때 알리지 않고, Store한 파일을 Load하면 같은 자리로 돌아오고, 게임은 멈추고, 화면에 다시 그리라고 알림
 *   3. Load 실패: 지원하지 않는 확장자, 깨진 파일은 오류 창을 띄우고 보드는 그대로. 깨진 파일은 알림도 없음 (트랜잭션)
 *   4. Stamp: 켜면 스탬프 모드, 클릭하면 패턴을 찍음(#R 무시), 찍으면 화면에 알림, 오른쪽 클릭은 회전, 다시 누르면 끔
 *
 * [커버리지]
 *   - GridCommandTest.java 테스트 대상: Clear, Load, Store, Stamp Command와 MouseController의 모드 전환
 *   - 테스트 제외 대상
 *     - ExitCommand: System.exit을 부름
 *     - 파일 창에서 취소: LoadCommandTest, StoreCommandTest, StampCommandTest에서 확인
 *     - Store 실패(쓸 수 없는 폴더): Load 실패와 같은 흐름
 *     - Stamp 실패: StampCommandFailureTest에서 확인
 *     - GridMenu의 메뉴 등록: GridMenuTest에서 확인
 *     - 미리보기 그리기 (GUI)
 *     - 클릭(셀 토글) 뒤의 알림: ToggleModeTest에서 확인
 */
class GridCommandTest {
	private static final int SIZE = LifeTestDriver.SIZE;

	@TempDir
	Path folder;

	private final Board board = new Board();
	private final FakeTicker ticker = new FakeTicker();
	private final LifeGame game = new LifeGame(board, ticker);
	private final FakeDialogs dialogs = new FakeDialogs();
	private final RleFormat rle = new RleFormat(SIZE);
	private final PatternFormats formats = new PatternFormats(List.of(new LifeFormatAdapter(SIZE), rle));
	private final RecordingGameListener view = new RecordingGameListener();
	private final MouseController mouse = new MouseController(new ToggleMode(game, new Pen(CellColor.DEFAULT)));
	private final StampCommand stamp = new StampCommand(game, rle, mouse, new Pen(CellColor.DEFAULT), dialogs);

	@BeforeEach
	void startGame() {
		game.addGameListener(view);
		game.start(100);
	}

	//  1. Clear

	@Test
	@Tag("improvement")
	@DisplayName("execute(): Clear하면 보드를 비우고, 화면에 보드 전체를 다시 그리라고 알린다")
	void clearEmptiesBoardAndTellsView() {
		// Given
		board.toggle(10, 10);

		// When
		new ClearCommand(game).execute();

		// Then
		assertEquals(Set.of(), board.liveCells());
		assertEquals(List.of("edited"), view.events);
	}

	//  2. Load/Store

	@Test
	@Tag("original")
	@DisplayName("execute(): .life 파일을 Load하면 보드가 파일 내용으로 바뀐다")
	void loadingLifeFileReplacesBoard() throws IOException{
		// Given: blinker를 .life로 저장해 두고, 보드에는 다른 셀
		Set<Point> blinker = Cells.fromRowColumn("3,2", "3,3", "3,4");
		place(blinker);
		File file = folder.resolve("blinker.life").toFile();
		try (OutputStream out = new FileOutputStream(file)) {
			game.store(out, new LifeFormatAdapter(SIZE));
		}
		board.clear();
		board.toggle(40, 40);
		dialogs.nextFile = file;

		// When
		new LoadCommand(game, formats, dialogs).execute();

		// Then
		assertEquals(blinker, board.liveCells());
	}

	@Test
	@Tag("extension")
	@DisplayName("execute(): .rle로 Store한 패턴을 Load하면 같은 자리로 돌아오고, 게임은 멈추고, 화면에 boardEdited를 알린다")
	void storedRleLoadsAtSamePlace() {
		// Given: 구석의 glider를 Store
		Set<Point> glider = Cells.fromRowColumn("1,2", "2,3", "3,1", "3,2", "3,3");
		place(glider);
		dialogs.nextFile = folder.resolve("glider.rle").toFile();
		new StoreCommand(game, formats, dialogs).execute();
		assertEquals(List.of(), view.events);  // Store는 보드를 바꾸지 않으니 알리지 않음

		// When: 보드를 지우고 다시 Load
		board.clear();
		game.start(100);
		new LoadCommand(game, formats, dialogs).execute();

		// Then
		assertEquals(glider, board.liveCells());
		assertFalse(ticker.running);
		assertEquals(List.of(), dialogs.errors);
		assertEquals(List.of("edited"), view.events);
	}

	//  3. Load 실패

	@Test
	@Tag("extension")
	@DisplayName("execute(): Load에서 지원하지 않는 확장자는 지원 형식을 알려주는 오류 창을 띄우고, 보드는 그대로 남는다")
	void unsupportedExtensionShowsError() throws IOException {
		// Given
		board.toggle(10, 10);
		dialogs.nextFile = write("glider.txt", "x = 3, y = 3\nbo$2bo$3o!");

		// When
		new LoadCommand(game, formats, dialogs).execute();

		// Then
		assertEquals(1, dialogs.errors.size());
		assertTrue(dialogs.errors.get(0).contains(".life, .rle"));
		assertEquals(Cells.fromRowColumn("10,10"), board.liveCells());
	}

	@Test
	@Tag("extension")
	@DisplayName("execute(): 깨진 .rle 파일을 Load하면 오류 창을 띄우고, 보드는 그대로 남고, 화면에 알리지 않는다")
	void damagedFileShowsError() throws IOException {
		// Given
		board.toggle(10, 10);
		dialogs.nextFile = write("broken.rle", "x = 3, y = 3\nbo$2bo$3x!");

		// When
		new LoadCommand(game, formats, dialogs).execute();

		// Then
		assertEquals(1, dialogs.errors.size());
		assertEquals(Cells.fromRowColumn("10,10"), board.liveCells());
		assertEquals(List.of(), view.events);
	}

	//  4. Stamp

	@Test
	@Tag("extension")
	@DisplayName("execute(): Stamp에서 .rle 파일을 고르면 마우스가 스탬프 모드가 된다")
	void stampTurnsOnStampMode() throws IOException{
		// Given
		chooseGliderWithPosition();

		// When
		stamp.execute();

		// Then
		assertInstanceOf(StampMode.class, mouse.mode());
	}

	@Test
	@Tag("extension")
	@DisplayName("execute(): 스탬프 모드에서 클릭하면 그 셀을 왼쪽 위로 패턴을 찍고 #R 위치는 무시한다")
	void clickInStampModeStampsPatternAtMouse() throws IOException{
		// Given: #R이 있는 glider 파일로 스탬프 모드를 켬. 스탬프는 위치 정보를 쓰지 않음
		chooseGliderWithPosition();
		stamp.execute();

		// When
		mouse.clicked(10, 20);

		// Then: 마우스가 가리킨 셀이 패턴의 왼쪽 위
		assertEquals(Cells.fromRowColumn("10,21", "11,22", "12,20", "12,21", "12,22"), board.liveCells());
	}

	@Test
	@Tag("extension")
	@DisplayName("execute(): 스탬프 모드에서 찍으면 화면에 boardEdited를 알린다")
	void stampingTellsView() throws IOException{
		// Given
		chooseGliderWithPosition();
		stamp.execute();

		// When
		mouse.clicked(10, 20);

		// Then
		assertEquals(List.of("edited"), view.events);
	}

	@Test
	@Tag("extension")
	@DisplayName("execute(): 스탬프 모드에서 오른쪽 클릭하면 패턴이 시계 방향으로 90도 돈다")
	void rightClickRotatesPatternClockwise() throws IOException{
		// Given
		chooseGliderWithPosition();
		stamp.execute();

		// When: 오른쪽 클릭으로 돌리고 (30, 30)에 찍음
		mouse.rightClicked(0, 0);
		mouse.clicked(30, 30);

		// Then: 시계 방향 90도 돌린 glider
		assertEquals(Cells.fromRowColumn("30,30", "31,30", "31,32", "32,30", "32,31"), board.liveCells());
	}

	@Test
	@Tag("extension")
	@DisplayName("execute(): 스탬프 모드에서 다시 누르면 모드가 꺼지고 클릭이 셀 토글로 돌아온다")
	void stampAgainTurnsOffStampMode() throws IOException{
		// Given: 스탬프 모드를 켬
		chooseGliderWithPosition();
		stamp.execute();

		// When: 다시 눌러 끄고 클릭
		stamp.execute();
		mouse.clicked(5, 5);

		// Then
		assertInstanceOf(ToggleMode.class, mouse.mode());
		assertEquals(Cells.fromRowColumn("5,5"), board.liveCells());
	}

	//  헬퍼

	private void place(Set<Point> cells) {
		for (Point p : cells) {
			board.toggle(p.y, p.x);
		}
	}

	// 파일 창에서 #R(위치 정보)이 있는 glider 파일을 고르게 함
	private void chooseGliderWithPosition() throws IOException{
		dialogs.nextFile = write("glider.rle", "#R 50 50\nx = 3, y = 3\nbo$2bo$3o!");
	}

	private File write(String name, String text) throws IOException {
		Path file = folder.resolve(name);
		Files.writeString(file, text, StandardCharsets.UTF_8);
		return file.toFile();
	}

}

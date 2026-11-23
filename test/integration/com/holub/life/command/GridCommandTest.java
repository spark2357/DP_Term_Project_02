package com.holub.life.command;

import com.holub.life.*;
import com.holub.life.mouse.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
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
 *   2. Load/Store: Store한 파일을 Load하면 같은 자리로 돌아오고, 게임은 멈추고, 화면에 다시 그리라고 알림
 *   3. Load 실패: 지원하지 않는 확장자, 깨진 파일은 오류 창을 띄우고 보드는 그대로. 깨진 파일은 알림도 없음 (트랜잭션)
 *   4. Stamp: 켜기 → 클릭으로 찍기(#R 무시)와 알림 → 오른쪽 클릭으로 회전 → 다시 눌러 끄기
 *
 * [커버리지]
 *   - GridCommandTest.java 테스트 대상: Clear, Load, Store, Stamp Command와 MouseController의 모드 전환
 *   - 테스트 제외 대상
 *     - ExitCommand: System.exit을 부름
 *     - 파일 창에서 취소: LoadCommandTest, StoreCommandTest, StampCommandTest에서 확인
 *     - Store 실패(쓸 수 없는 폴더), Stamp 실패: Load 실패와 같은 흐름
 *     - GridMenu의 메뉴 등록, 미리보기 그리기 (GUI)
 *     - 클릭(셀 토글) 뒤의 알림: 마우스 모드 테스트에서 다룸 (아직 없음)
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

	@BeforeEach
	void startGame() {
		game.addGameListener(view);
		game.start(100);
	}

	//  1. Clear

	@Test
	@Tag("improvement")
	@Tag("design")
	@DisplayName("ClearCommand: 보드를 비우고, 화면에 보드 전체를 다시 그리라고 알린다")
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
	@Tag("extension")
	@Tag("improvement")
	@Tag("design")
	@DisplayName("StoreCommand, LoadCommand: .rle로 Store한 패턴을 Load하면 같은 자리로 돌아오고, 게임은 멈추고, 화면에 boardEdited를 알린다")
	void storedRleLoadsAtSamePlace() {
		// Given: 구석의 glider를 Store
		Set<Point> glider = Cells.fromRowColumn("1,2", "2,3", "3,1", "3,2", "3,3");
		place(glider);
		dialogs.nextFile = folder.resolve("glider.rle").toFile();
		new StoreCommand(game, formats, dialogs).execute();

		// When: 보드를 지우고 다시 Load. 그 전에 받은 알림은 지움
		board.clear();
		game.start(100);
		view.events.clear();
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
	@DisplayName("LoadCommand: 지원하지 않는 확장자는 지원 형식을 알려주는 오류 창을 띄우고, 보드는 그대로 남는다")
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
	@Tag("improvement")
	@Tag("defect-fix")
	@Tag("design")
	@DisplayName("LoadCommand: 깨진 .rle 파일은 오류 창을 띄우고, 보드는 그대로 남고, 화면에 알리지 않는다")
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
	@Tag("improvement")
	@Tag("design")
	@DisplayName("StampCommand: 켜면 클릭한 곳에 패턴을 찍고(#R 무시) 화면에 알리며, 오른쪽 클릭은 회전하고, 다시 누르면 클릭이 셀 토글로 돌아온다")
	void stampModeOnAndOff() throws IOException {
		// Given: #R이 있는 glider 파일. 스탬프는 위치 정보를 쓰지 않음
		MouseController mouse = new MouseController(new ToggleMode(game));
		StampCommand stamp = new StampCommand(game, rle, mouse, dialogs);
		dialogs.nextFile = write("glider.rle", "#R 50 50\nx = 3, y = 3\nbo$2bo$3o!");

		// When: 켜고 (10, 20)에 찍음
		stamp.execute();
		mouse.clicked(10, 20);

		// Then: 마우스가 가리킨 셀이 패턴의 왼쪽 위이고, 찍은 뒤 화면에 다시 그리라고 알림
		assertInstanceOf(StampMode.class, mouse.mode());
		Set<Point> glider = Cells.fromRowColumn("10,21", "11,22", "12,20", "12,21", "12,22");
		assertEquals(glider, board.liveCells());
		assertEquals(List.of("edited"), view.events);

		// When: 오른쪽 클릭으로 돌리고 (30, 30)에 찍음
		board.clear();
		mouse.rightClicked(0, 0);
		mouse.clicked(30, 30);

		// Then: 시계 방향 90도 돌린 glider
		Set<Point> rotated = Cells.fromRowColumn("30,30", "31,30", "31,32", "32,30", "32,31");
		assertEquals(rotated, board.liveCells());

		// When: 다시 눌러 끄고 클릭
		board.clear();
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

	private File write(String name, String text) throws IOException {
		Path file = folder.resolve(name);
		Files.writeString(file, text, StandardCharsets.UTF_8);
		return file.toFile();
	}

}

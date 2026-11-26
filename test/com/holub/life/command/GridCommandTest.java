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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Grid 메뉴 Command 테스트: 파일 창과 오류 창을 가짜 Dialogs로 바꿔 GUI 없이 검증
 *
 *   Command
 *   - ClearCommand  : Grid → Clear
 *   - LoadCommand   : Grid → Load. 게임을 멈추고, 고른 파일을 확장자에 맞는 형식으로 읽음
 *   - StoreCommand  : Grid → Store. 게임을 멈추고, 고른 파일에 확장자에 맞는 형식으로 씀
 *   - StampCommand  : Grid → Stamp. .rle 패턴을 골라 스탬프 모드를 켜고, 다시 누르면 끔
 *
 * [테스트 시나리오]
 *   1. Clear: 보드를 비우고 화면에 다시 그리라고 알림
 *   2. Load/Store: Store한 파일을 Load하면 같은 자리로 돌아오고 게임은 멈춤
 *   3. Load 실패: 지원하지 않는 확장자, 깨진 파일은 오류 창을 띄우고 보드는 그대로
 *   4. Stamp: 켜기 → 클릭으로 찍기(#R 무시) → 오른쪽 클릭으로 회전 → 다시 눌러 끄기
 *
 * [커버리지]
 *   - GridCommandTest.java 테스트 대상: Clear, Load, Store, Stamp Command와 MouseController의 모드 전환
 *   - 테스트 제외 대상
 *     - ExitCommand: System.exit을 부름
 *     - 파일 창에서 취소: Command가 아무것도 하지 않음
 *     - Store 실패(쓸 수 없는 폴더), Stamp 실패: Load 실패와 같은 흐름
 *     - GridMenu의 메뉴 등록, 미리보기 그리기 (GUI)
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

	@BeforeEach
	void startGame() {
		game.start(100);
	}

	//  1. Clear

	@Test
	@DisplayName("ClearCommand: 보드를 비우고, 화면에 보드 전체를 다시 그리라고 알린다")
	void clearEmptiesBoardAndTellsView() {
		// Given
		board.toggle(10, 10);
		List<String> events = new ArrayList<>();
		game.addGameListener(new LifeGame.Listener() {
			public void boardChanged() {
				events.add("changed");
			}

			public void boardEdited() {
				events.add("edited");
			}
		});

		// When
		new ClearCommand(game).execute();

		// Then
		assertEquals(Set.of(), board.liveCells());
		assertEquals(List.of("edited"), events);
	}

	//  2. Load/Store

	@Test
	@DisplayName("StoreCommand, LoadCommand: .rle로 Store한 패턴을 Load하면 같은 자리로 돌아오고 게임은 멈춘다")
	void storedRleLoadsAtSamePlace() {
		// Given: 구석의 glider를 Store
		Set<Point> glider = Cells.fromRowColumn("1,2", "2,3", "3,1", "3,2", "3,3");
		place(glider);
		dialogs.nextFile = folder.resolve("glider.rle").toFile();
		new StoreCommand(game, formats, dialogs).execute();

		// When: 보드를 지우고 다시 Load
		board.clear();
		game.start(100);
		new LoadCommand(game, formats, dialogs).execute();

		// Then
		assertEquals(glider, board.liveCells());
		assertFalse(ticker.running);
		assertEquals(List.of(), dialogs.errors);
	}

	//  3. Load 실패

	@Test
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
	@DisplayName("LoadCommand: 깨진 .rle 파일은 오류 창을 띄우고, 보드는 그대로 남는다")
	void damagedFileShowsError() throws IOException {
		// Given
		board.toggle(10, 10);
		dialogs.nextFile = write("broken.rle", "x = 3, y = 3\nbo$2bo$3x!");

		// When
		new LoadCommand(game, formats, dialogs).execute();

		// Then
		assertEquals(1, dialogs.errors.size());
		assertEquals(Cells.fromRowColumn("10,10"), board.liveCells());
	}

	//  4. Stamp

	@Test
	@DisplayName("StampCommand: 켜면 클릭한 곳에 패턴을 찍고(#R 무시), 오른쪽 클릭은 회전하며, 다시 누르면 클릭이 셀 토글로 돌아온다")
	void stampModeOnAndOff() throws IOException {
		// Given: #R이 있는 glider 파일. 스탬프는 위치 정보를 쓰지 않음
		MouseController mouse = new MouseController(new ToggleMode(game, new Pen(CellColor.DEFAULT)));
		StampCommand stamp = new StampCommand(game, rle, mouse, new Pen(CellColor.DEFAULT), dialogs);
		dialogs.nextFile = write("glider.rle", "#R 50 50\nx = 3, y = 3\nbo$2bo$3o!");

		// When: 켜고 (10, 20)에 찍음
		stamp.execute();
		mouse.clicked(10, 20);

		// Then: 마우스가 가리킨 셀이 패턴의 왼쪽 위
		assertInstanceOf(StampMode.class, mouse.mode());
		Set<Point> glider = Cells.fromRowColumn("10,21", "11,22", "12,20", "12,21", "12,22");
		assertEquals(glider, board.liveCells());

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

	// 고른 파일을 미리 정해 두고, 오류 메시지를 모아 두는 가짜 창
	private static class FakeDialogs implements Dialogs {
		File nextFile;
		final List<String> errors = new ArrayList<>();

		@Override
		public Optional<File> chooseFile(String selectButtonText, FileNameExtensionFilter[] filters) {
			return Optional.ofNullable(nextFile);
		}

		@Override
		public void showError(String message) {
			errors.add(message);
		}
	}

	// 실제로 tick을 보내지 않고, 실행 중인지만 기억하는 가짜 시계
	private static class FakeTicker implements TickSource {
		boolean running;

		public void startTicking(int millisecondsBetweenTicks) {
			running = true;
		}

		public void stop() {
			running = false;
		}

		public void tick() {
		}

		public void addTickListener(Listener listener) {
		}
	}
}

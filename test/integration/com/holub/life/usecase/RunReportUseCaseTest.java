package com.holub.life.usecase;

import com.holub.life.command.*;
import com.holub.life.mouse.*;
import com.holub.life.support.*;
import com.holub.ui.MenuSite;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Point;
import java.io.File;
import java.util.List;
import java.util.Set;
import javax.swing.JLabel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.holub.life.format.LifeFormatAdapter;
import com.holub.life.format.PatternFormats;
import com.holub.life.format.RleFormat;
import com.holub.life.game.LifeGame;
import com.holub.life.game.RunRecorder;
import com.holub.life.model.Board;
import com.holub.life.model.CellColor;
import com.holub.life.model.CellPattern;

/**
 * E. 통계 리포트 표시, E. 멸종 시 자동 정지 유스케이스 통합 테스트: Life.main과 같은 방법으로 조립한 게임을 실행하고 멈춤
 * 진짜 LifeGame·Board·RunRecorder와 상태 표시줄(JLabel)에 가짜 시계(FakeTicker)를 넣어 시간 없이 실행함
 *
 *   조립 (Life.main과 같음)
 *   - Halt         = MacroCommand(HaltCommand, ShowRunReportCommand("사용자 정지", 상태 표시줄))  → GoMenu의 Halt 항목에 넘김
 *   - 멸종 시 동작  = MacroCommand(HaltCommand, ShowRunReportCommand("멸종으로 정지", 상태 표시줄)) → RunRecorder.whenExtinct()
 *
 * [테스트 시나리오]
 *   1. 통계 리포트 표시: 실행 중에 Go → Halt 메뉴 항목을 누르면 게임이 멈추고 상태 표시줄에 "사용자 정지" 리포트가 나옴
 *      (11-24 merge에서 GoMenu가 넘겨받은 Halt 대신 HaltCommand만 등록해 리포트가 나오지 않던 문제의 회귀 테스트)
 *      Tick(한 세대 실행)으로만 진행한 뒤 Halt해도 그 세대들의 리포트가 나옴
 *      실행 중에 Undo, Load, Store로 게임이 멈출 때는 리포트가 나오지 않음 (리포트는 Halt와 멸종 때만)
 *   2. 멸종 시 자동 정지: 실행 중 마지막 셀이 죽는 세대에 게임이 멈추고 상태 표시줄에 "멸종으로 정지" 리포트가 나옴
 *      Tick으로 마지막 셀이 죽어도 멈춘 채로 "멸종으로 정지" 리포트가 나옴
 *      처음부터 빈 보드로 실행하면 멸종으로 멈추지 않고 계속 실행되며 리포트도 나오지 않음
 *   3. 조합 (실행 중 편집): Clear, Stamp, Load는 세대로 세지 않고 리포트의 현재 셀 수에만 반영됨
 *      Load는 게임을 멈추므로, 자동 실행이 아니라 Tick으로 진행하던 중에 Load함 (Load로 멈출 때는 리포트가 나오지 않음, 1에서 확인)
 *
 * [커버리지]
 *   - RunReportUseCaseTest.java 테스트 대상: GoMenu(Halt에 넘긴 Command), MacroCommand, HaltCommand, ShowRunReportCommand, RunRecorder를 함께 쓰는 흐름,
 *     그 흐름 안에서 TickCommand, ClearCommand, LoadCommand, StoreCommand, UndoCommand, StampMode
 *   - 1의 Halt 메뉴 항목 테스트는 MenuSite가 필요해 JFrame을 만들므로(화면에 띄우지는 않음) 화면이 없는 환경에서는 건너뜀
 *   - 리포트 글자는 앞부분(이유, 세대 수, 현재 셀)만 확인함. 소수점이 들어간 뒷부분은 ShowRunReportCommandTest에서 확인
 *   - 테스트 제외 대상
 *     - Life.main 자체와 상태 표시줄이 창 아래(SOUTH)에 붙는지: 창을 띄워야 해서 제외. 조립은 테스트에서 Life.main과 같게 따라 함
 */
@Tag("extension")
class RunReportUseCaseTest{
	private static final Set<Point> BLINKER = Cells.fromRowColumn("3,2", "3,3", "3,4");

	private final FakeTicker ticker = new FakeTicker();
	private final LifeGame game = new LifeGame(new Board(), ticker);
	private final JLabel statusBar = new JLabel("시작 글자");
	private final PatternFormats formats = new PatternFormats(
			List.of(new LifeFormatAdapter(LifeTestDriver.SIZE), new RleFormat(LifeTestDriver.SIZE)));
	@TempDir
	File folder;
	private Command haltAndReport;
	private GoMenu menu;

	// Life.main과 같은 조립
	@BeforeEach
	void assembleLikeLifeMain(){
		RunRecorder recorder = new RunRecorder(game);
		haltAndReport = new MacroCommand(
				new HaltCommand(game),
				new ShowRunReportCommand(recorder, "사용자 정지", statusBar::setText));
		Command stopOnExtinction = new MacroCommand(
				new HaltCommand(game),
				new ShowRunReportCommand(recorder, "멸종으로 정지", statusBar::setText));
		recorder.whenExtinct(stopOnExtinction::execute);
	}

	@AfterEach
	void removeMenu(){
		if (menu != null){
			MenuSite.removeMyMenus(menu);
		}
	}

	//  1. 통계 리포트 표시

	@Test
	@DisplayName("register(): Halt 항목에 넘긴 묶음이 등록되어, 실행 중에 Go → Halt를 누르면 게임이 멈추고 상태 표시줄에 이번 실행의 리포트가 나온다")
	void haltMenuStopsAndShowsReport(){
		assumeFalse(MenuSiteSetup.isHeadless());

		// Given: Halt 항목에 묶음을 넘겨 Go 메뉴를 등록하고, blinker를 실행 중 3세대 진행
		MenuSiteSetup.establishOnce();
		menu = new GoMenu(game, haltAndReport);
		menu.register();
		place(BLINKER);
		game.start(70);
		ticker.tick();
		ticker.tick();
		ticker.tick();

		// When
		MenuSite.getMyMenuItem(menu, "Go", "Halt").doClick();

		// Then
		assertFalse(ticker.running);
		assertTrue(statusBar.getText().startsWith("사용자 정지 · 3세대 · 현재 3셀 · "), statusBar.getText());
	}

	@Test
	@DisplayName("execute(): 게임을 시작하지 않고 Tick으로만 진행한 뒤 Halt해도 그 세대들의 리포트가 나온다")
	void haltAfterTicksShowsReport(){
		// Given: blinker를 Tick으로 3세대 진행
		place(BLINKER);
		tick(3);

		// When
		haltAndReport.execute();

		// Then
		assertTrue(statusBar.getText().startsWith("사용자 정지 · 3세대 · 현재 3셀 · "), statusBar.getText());
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"Undo", "Load", "Store"})
	@DisplayName("execute(): 실행 중에 Undo, Load, Store로 게임이 멈추면 리포트는 나오지 않는다")
	void stopByUndoLoadOrStoreShowsNoReport(String commandName){
		// Given: blinker를 실행 중 2세대 진행
		place(BLINKER);
		game.start(70);
		ticker.tick();
		ticker.tick();
		Command stop = stopCommandNamed(commandName);

		// When
		stop.execute();

		// Then: 게임은 멈췄지만 상태 표시줄은 처음 글자 그대로
		assertFalse(ticker.running);
		assertEquals("시작 글자", statusBar.getText());
	}

	//  2. 멸종 시 자동 정지

	@Test
	@DisplayName("whenExtinct(): 실행 중 마지막 셀이 죽는 세대에 게임이 저절로 멈추고 상태 표시줄에 멸종 리포트가 나온다")
	void extinctionStopsAndShowsReport(){
		// Given: blinker를 실행 중 2세대 진행한 뒤(다시 가로 모양), 클릭으로 양 끝을 지워 가운데 셀 하나만 남김
		place(BLINKER);
		game.start(70);
		ticker.tick();
		ticker.tick();
		place(Cells.fromRowColumn("3,2", "3,4"));
		assertTrue(ticker.running);

		// When: 혼자 남은 셀이 죽음
		ticker.tick();

		// Then
		assertFalse(ticker.running);
		assertTrue(statusBar.getText().startsWith("멸종으로 정지 · 3세대 · 현재 0셀 · "), statusBar.getText());
	}

	@Test
	@DisplayName("whenExtinct(): Tick으로 진행한 세대에 마지막 셀이 죽어도 게임은 멈춘 채로 있고 상태 표시줄에 멸종 리포트가 나온다")
	void extinctionByTickShowsReport(){
		// Given: 혼자 있는 셀 하나
		place(Cells.fromRowColumn("10,10"));

		// When
		tick(1);

		// Then
		assertFalse(ticker.running);
		assertTrue(statusBar.getText().startsWith("멸종으로 정지 · 1세대 · 현재 0셀 · "), statusBar.getText());
	}

	@Test
	@DisplayName("whenExtinct(): 처음부터 빈 보드로 실행하면 멸종으로 멈추지 않고 계속 실행되며 리포트도 나오지 않는다")
	void emptyBoardKeepsRunningWithoutReport(){
		// Given: 셀이 하나도 없는 보드로 실행
		game.start(70);

		// When
		ticker.tick();
		ticker.tick();
		ticker.tick();

		// Then
		// [수정] 2026-11-27: 빈 보드는 세대가 지나도 바뀌지 않아 boardChanged 알림이 없으므로 멸종 동작이 실행되지 않음 (의도한 동작)
		assertTrue(ticker.running);
		assertEquals("시작 글자", statusBar.getText());
	}

	//  3. 조합 (실행 중 편집)

	@Test
	@DisplayName("boardEdited(): 실행 중에 Clear해도 세대로 세지 않고, 리포트의 현재 셀 수에만 반영한다")
	void clearWhileRunningCountsOnlyCurrentCells(){
		// Given: blinker를 실행 중 2세대 진행
		place(BLINKER);
		game.start(70);
		ticker.tick();
		ticker.tick();

		// When
		new ClearCommand(game).execute();
		haltAndReport.execute();

		// Then
		assertTrue(statusBar.getText().startsWith("사용자 정지 · 2세대 · 현재 0셀 · "), statusBar.getText());
	}

	@Test
	@DisplayName("boardEdited(): 실행 중에 Stamp로 찍어도 세대로 세지 않고, 리포트의 현재 셀 수에만 반영한다")
	void stampWhileRunningCountsOnlyCurrentCells(){
		// Given: blinker를 실행 중 2세대 진행하고, glider(5셀) 스탬프 모드를 켬
		place(BLINKER);
		game.start(70);
		ticker.tick();
		ticker.tick();
		Pen pen = new Pen(CellColor.DEFAULT);
		MouseController mouse = new MouseController(new BrushMode(game, pen));
		CellPattern glider = new CellPattern(3, 3, Cells.fromRowColumn("0,1", "1,2", "2,0", "2,1", "2,2"));
		mouse.setMode(new StampMode(game, glider, pen));

		// When: blinker와 떨어진 곳에 찍음
		mouse.clicked(20, 20);
		haltAndReport.execute();

		// Then: blinker 3셀 + glider 5셀
		assertTrue(statusBar.getText().startsWith("사용자 정지 · 2세대 · 현재 8셀 · "), statusBar.getText());
	}

	@Test
	@DisplayName("boardEdited(): Tick으로 진행하던 중에 Load해도 세대로 세지 않고, 리포트의 현재 셀 수에만 반영한다")
	void loadBetweenTicksCountsOnlyCurrentCells(){
		// Given: blinker를 Tick으로 2세대 진행
		place(BLINKER);
		tick(2);
		FakeDialogs dialogs = new FakeDialogs();
		dialogs.nextFile = new File("test/resources/glider.rle");
		PatternFormats formats = new PatternFormats(List.of(new LifeFormatAdapter(LifeTestDriver.SIZE), new RleFormat(LifeTestDriver.SIZE)));

		// When: glider(5셀)를 Load하고 한 세대 더 진행한 뒤 Halt
		new LoadCommand(game, formats, dialogs).execute();
		tick(1);
		haltAndReport.execute();

		// Then: Load는 세대로 세지 않아 3세대. glider는 진행해도 5셀
		assertTrue(statusBar.getText().startsWith("사용자 정지 · 3세대 · 현재 5셀 · "), statusBar.getText());
	}

	// ---- 헬퍼 메서드

	// Go → Tick (한 세대 실행)을 times번 실행
	private void tick(int times){
		for (int i = 0; i < times; i++){
			new TickCommand(game).execute();
		}
	}

	// 게임을 멈추는 Edit → Undo, Grid → Load, Grid → Store Command. Load는 glider.rle를 읽고, Store는 임시 폴더에 씀
	private Command stopCommandNamed(String name){
		FakeDialogs dialogs = new FakeDialogs();
		if (name.equals("Undo")){
			return new UndoCommand(game);
		}
		if (name.equals("Load")){
			dialogs.nextFile = new File("test/resources/glider.rle");
			return new LoadCommand(game, formats, dialogs);
		}
		dialogs.nextFile = new File(folder, "board.rle");
		return new StoreCommand(game, formats, dialogs);
	}

	// 클릭처럼 LifeGame으로 토글 (산 셀이면 지움)
	private void place(Set<Point> cells){
		for (Point p : cells){
			game.toggle(p.y, p.x);
		}
	}
}

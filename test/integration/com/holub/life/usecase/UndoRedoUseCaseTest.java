package com.holub.life.usecase;

import com.holub.life.command.*;
import com.holub.life.mouse.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.holub.life.game.LifeGame;
import com.holub.life.model.BlendRule;
import com.holub.life.model.Board;
import com.holub.life.model.CellColor;
import com.holub.life.model.CellPattern;

/**
 * E. 되돌리기, E. 다시 실행 유스케이스 통합 테스트: Edit 메뉴의 Undo/Redo Command를 Go → Tick, 셀 색, 스탬프 모드와 함께 실행
 * 진짜 LifeGame·Board와 MouseController에 가짜 시계(FakeTicker)와 가짜 리스너(RecordingGameListener)를 넣어 화면 없이 실행함
 *
 *   Command
 *   - UndoCommand : Edit → Undo. 멈추고 마지막 변경 전의 보드로 되돌림
 *   - RedoCommand : Edit → Redo. 멈추고 Undo로 되돌린 변경을 다시 실행
 *   - TickCommand : Go → Tick. 한 세대 진행 (보드가 바뀐 세대만 기록)
 *
 * [테스트 시나리오]
 *   1. Undo 뒤 Tick (E. 다시 실행 대안 3a): 보드가 바뀌는 Tick(blinker)은 Redo 기록을 지움. 안 바뀌는 Tick(정지한 block)은 지우지 않음
 *   2. 셀 색 (E. 출생 색 규칙 변경과 함께): Blend로 진행한 세 색 blinker를 Undo하면 자리와 색이 진행 전으로 돌아옴
 *   3. 스탬프 모드 (E. 패턴 찍기와 함께): 스탬프 모드로 찍고 Undo하면 보드는 돌아오고 마우스는 스탬프 모드 그대로
 *   4. 마우스 클릭: 기본 모드(칠하기)로 클릭한 것을 Undo하면 클릭 전 보드로 돌아옴. 산 셀의 색만 바뀐 클릭도 원래 색으로 돌아옴
 *
 * [커버리지]
 *   - UndoRedoUseCaseTest.java 테스트 대상: UndoCommand, RedoCommand와 TickCommand, StampMode, BrushMode(칠하기), BlendRule을 함께 쓰는 흐름
 *   - 테스트 제외 대상
 *     - 토글, Clear, Load, 기록 한도, 실행 중 Undo: LifeGameUndoTest에서 확인
 *     - Edit 메뉴 등록과 단축키: EditMenuTest에서 확인
 */
@Tag("extension")
class UndoRedoUseCaseTest{
	private static final CellColor BLUE = new CellColor(0, 0, 255);
	private static final CellColor GREEN = new CellColor(0, 128, 0);
	private static final CellColor YELLOW = new CellColor(255, 255, 0);

	private final Board board = new Board();
	private final FakeTicker ticker = new FakeTicker();
	private final LifeGame game = new LifeGame(board, ticker);
	private final RecordingGameListener view = new RecordingGameListener();

	private final Command undo = new UndoCommand(game);
	private final Command redo = new RedoCommand(game);
	private final Command tick = new TickCommand(game);

	//  1. Undo 뒤 Tick

	@Test
	@DisplayName("execute(): Undo 뒤 Tick으로 보드가 바뀌면 Redo는 아무것도 하지 않는다 (보드 그대로, 화면 알림 없음)")
	void changedTickAfterUndoClearsRedo(){
		// Given: 가로 blinker를 Tick → Undo로 되돌린 뒤 다시 Tick (blinker는 세대마다 바뀜)
		place(Cells.fromRowColumn("10,10", "10,11", "10,12"));
		tick.execute();
		undo.execute();
		tick.execute();
		Set<Point> afterTick = board.liveCells();
		game.addGameListener(view);

		// When
		redo.execute();

		// Then: Redo가 아무것도 하지 않았으므로 보드도 그대로이고 다시 그리라는 알림도 없음
		assertEquals(afterTick, board.liveCells());
		assertEquals(List.of(), view.events);
	}

	@Test
	@DisplayName("execute(): Undo 뒤 보드가 바뀌지 않은 Tick(정지한 block)이 있어도 Redo는 되돌린 Stamp를 다시 실행한다")
	void unchangedTickAfterUndoKeepsRedo(){
		// Given: block만 있는 보드에 blinker를 찍고 Undo. 남은 block은 Tick해도 그대로
		Set<Point> block = Cells.fromRowColumn("20,20", "20,21", "21,20", "21,21");
		place(block);
		game.stamp(new CellPattern(3, 1, Cells.fromRowColumn("0,0", "0,1", "0,2")), 40, 40, CellColor.DEFAULT);
		undo.execute();
		tick.execute();
		assertEquals(block, board.liveCells());

		// When
		redo.execute();

		// Then: 찍었던 blinker가 다시 돌아옴
		Set<Point> expected = Cells.fromRowColumn("20,20", "20,21", "21,20", "21,21", "40,40", "40,41", "40,42");
		assertEquals(expected, board.liveCells());
	}

	//  2. 셀 색

	@Test
	@DisplayName("execute(): Blend로 Tick한 세 색 blinker를 Undo하면 자리와 색이 Tick 전으로 돌아온다")
	void undoTickRestoresBlendColors(){
		// Given: 파랑-초록-노랑 가로 blinker, 규칙은 Blend
		board.brush(10, 10, BLUE);
		board.brush(10, 11, GREEN);
		board.brush(10, 12, YELLOW);
		game.setBirthColorRule(new BlendRule());
		Map<Point, CellColor> before = BoardColors.of(board);
		tick.execute();
		assertNotEquals(before, BoardColors.of(board));

		// When
		undo.execute();

		// Then
		assertEquals(before, BoardColors.of(board));
	}

	//  3. 스탬프 모드

	@Test
	@DisplayName("execute(): 스탬프 모드로 찍은 뒤 Undo하면 보드는 찍기 전으로 돌아오고, 마우스는 스탬프 모드 그대로다")
	void undoKeepsStampMode(){
		// Given: 셀 하나가 있는 보드에 스탬프 모드로 glider를 찍음
		Pen pen = new Pen(CellColor.DEFAULT);
		MouseController mouse = new MouseController(new BrushMode(game, pen));
		StampMode stamp = new StampMode(game, new CellPattern(3, 3, Cells.fromRowColumn("0,1", "1,2", "2,0", "2,1", "2,2")), pen);
		place(Cells.fromRowColumn("5,5"));
		Set<Point> before = board.liveCells();
		mouse.setMode(stamp);
		mouse.clicked(10, 10);
		assertNotEquals(before, board.liveCells());

		// When
		undo.execute();

		// Then
		assertEquals(before, board.liveCells());
		assertSame(stamp, mouse.mode());
	}

	//  4. 마우스 클릭

	@Test
	@DisplayName("execute(): 마우스로 빈 칸을 칠한 뒤 Undo하면 클릭 전 보드로 돌아온다")
	void undoRevertsClick(){
		// Given: 셀 하나가 있는 보드에서 빈 칸을 파랑으로 클릭 (BrushMode는 LifeGame.brush()를 부름)
		Pen pen = new Pen(BLUE);
		MouseController mouse = new MouseController(new BrushMode(game, pen));
		place(Cells.fromRowColumn("5,5"));
		Set<Point> before = board.liveCells();
		mouse.clicked(10, 10);
		assertNotEquals(before, board.liveCells());

		// When
		undo.execute();

		// Then
		assertEquals(before, board.liveCells());
	}

	@Test
	@DisplayName("execute(): 산 셀을 다른 색으로 클릭해 색만 바뀐 것도 Undo하면 원래 색으로 돌아온다")
	void undoRevertsColorOnlyClick(){
		// Given: 파랑으로 칠한 셀을 노랑 펜으로 다시 클릭 (셀은 살아 있고 색만 바뀜)
		Pen pen = new Pen(BLUE);
		MouseController mouse = new MouseController(new BrushMode(game, pen));
		mouse.clicked(5, 5);
		Map<Point, CellColor> before = BoardColors.of(board);
		pen.setColor(YELLOW);
		mouse.clicked(5, 5);
		assertEquals(Map.of(new Point(5, 5), YELLOW), BoardColors.of(board));

		// When
		undo.execute();

		// Then
		assertEquals(before, BoardColors.of(board));
	}

	//  헬퍼

	// 원래 클릭(토글)으로 놓음. 토글은 Undo 기록에 남지만, 이 테스트는 그 앞까지 되돌리지 않음
	private void place(Set<Point> cells){
		for (Point p : cells){
			game.toggle(p.y, p.x);
		}
	}
}

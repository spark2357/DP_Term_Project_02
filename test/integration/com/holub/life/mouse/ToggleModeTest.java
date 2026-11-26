package com.holub.life.mouse;

import com.holub.life.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * 기본 마우스 모드 테스트: 클릭이 펜 색으로 칠하는 것과 툴바(팔레트)가 하는 일을 화면 없이 MouseController와 ToggleMode로 확인
 *
 *   클래스
 *   - ToggleMode      : 클릭하면 펜 색으로 칠함. 같은 색으로 산 셀은 죽음 (MouseMode State 패턴의 ConcreteState)
 *   - Pen             : 지금 펜 색. 칠하기와 Stamp가 같이 읽음
 *   - MouseController : 기본 모드는 칠하기. Stamp를 끄면 칠하기로 돌아옴
 *
 * [테스트 시나리오]
 *   1. 칠하기: 클릭하면 칠해지고, 같은 색으로 다시 누르면 꺼짐. 오른쪽 클릭도 같음
 *   2. 색 바꾸기: 색을 바꾸면 그 뒤 클릭부터 새 색. 다른 색 셀을 누르면 꺼지지 않고 색만 바뀜
 *   3. 게임과의 관계: 칠해도 게임은 멈추지 않고, 화면 전체를 다시 그리라고 알림. 출생 규칙을 바꿔도 같은 알림
 *   4. 원래 클릭과의 관계: 기본색(빨강)으로 칠하기는 원래 클릭(토글)과 결과가 같음
 *   5. 모드 되돌리기: 다른 모드(Stamp)를 끄면(resetMode) 칠하기로 돌아옴
 *
 * [커버리지]
 *   - ToggleModeTest.java 테스트 대상: ToggleMode, Pen, MouseController(기본 모드, resetMode), LifeGame.setBirthColorRule(알림)
 *   - 테스트 제외 대상
 *     - BrushToolBar: Swing 도구 막대라 제외 (버튼, 팔레트 팝업, 아이콘 그리기)
 *     - Stamp가 지금 펜 색으로 찍는지: StampModeTest에서 확인
 *     - 칠한 색이 보드에서 어떻게 물려지는지: CellColorBoardTest
 */
@Tag("extension")
class ToggleModeTest {
	private static final CellColor BLUE = new CellColor(0, 0, 255);
	private static final CellColor YELLOW = new CellColor(255, 255, 0);

	private Board board;
	private FakeTicker ticker;
	private LifeGame game;
	private MouseController mouse;
	private Pen pen;
	private ToggleMode toggle;

	@BeforeEach
	void setUp() {
		board = new Board();
		ticker = new FakeTicker();
		game = new LifeGame(board, ticker);
		pen = new Pen(BLUE);
		toggle = new ToggleMode(game, pen);
		mouse = new MouseController(toggle);		// Life.main과 같이 칠하기가 기본 모드
	}

	//  1. 칠하기

	@Test
	@DisplayName("clicked(), rightClicked(): 같은 색으로 두 번 누르면 꺼진다. 오른쪽 클릭도 왼쪽 클릭과 같다")
	void clickingTwiceWithTheSameColorToggles() {
		mouse.clicked(1, 1);
		mouse.rightClicked(2, 2);
		assertEquals(Cells.fromRowColumn("1,1", "2,2"), board.liveCells());

		mouse.clicked(1, 1);
		mouse.rightClicked(2, 2);
		assertEquals(Set.of(), board.liveCells());
	}

	//  2. 색 바꾸기

	@Test
	@DisplayName("setColor(), clicked(): 색을 바꾸면 그 뒤 클릭부터 새 색이고, 다른 색으로 산 셀을 누르면 꺼지지 않는다")
	void changedColorAppliesFromTheNextClick() {
		mouse.setMode(toggle);
		mouse.clicked(1, 1);			// 파랑
		pen.setColor(YELLOW);

		mouse.clicked(2, 2);			// 노랑으로 새로 살림
		mouse.clicked(1, 1);			// 파랑 셀 → 노랑으로 바뀜 (꺼지지 않음)

		assertEquals(YELLOW, pen.getColor());
		assertEquals(Cells.fromRowColumn("1,1", "2,2"), board.liveCells());

		mouse.clicked(1, 1);			// 이제 같은 색이라 꺼짐
		assertEquals(Cells.fromRowColumn("2,2"), board.liveCells());
	}

	//  3. 게임과의 관계

	@Test
	@DisplayName("clicked(): 칠해도 게임은 멈추지 않고, 매번 화면 전체를 다시 그리라고(boardEdited) 알린다")
	void brushingKeepsGameRunningAndRequestsRepaint() {
		List<String> events = new ArrayList<>();
		game.addGameListener(new LifeGame.Listener() {
			public void boardChanged() {
				events.add("changed");
			}

			public void boardEdited() {
				events.add("edited");
			}
		});
		game.start(30);

		mouse.clicked(1, 1);
		mouse.clicked(1, 1);

		assertTrue(ticker.running);
		assertEquals(List.of("edited", "edited"), events);
	}

	@Test
	@DisplayName("setBirthColorRule(): 출생 규칙을 바꾸면 화면 전체를 다시 그리라고(boardEdited) 알린다")
	void changingBirthColorRuleRequestsRepaint() {
		// Given
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
		game.setBirthColorRule(new BlendRule());

		// Then
		assertEquals(List.of("edited"), events);
	}

	//  4. 원래 클릭과의 관계

	@Test
	@DisplayName("clicked(): 기본색(빨강)으로 칠하기는 원래 클릭(토글)과 같다. 같은 칸을 같은 순서로 누르면 보드가 같고, 진행해도 같다")
	void brushingWithTheDefaultColorIsTheOriginalClick() {
		pen.setColor(CellColor.DEFAULT);
		Board original = new Board();
		String[] clicks = {"10,10", "10,11", "10,12", "11,12", "12,11", "10,11", "10,11", "30,30", "30,30"};

		for (String click : clicks) {
			String[] rowColumn = click.split(",");
			int row = Integer.parseInt(rowColumn[0]);
			int column = Integer.parseInt(rowColumn[1]);
			mouse.clicked(row, column);
			original.toggle(row, column);
		}

		for (int generation = 0; generation < 20; ++generation) {
			assertEquals(original.liveCells(), board.liveCells(), "generation " + generation);
			board.step();
			original.step();
		}
	}

	//  5. 모드 되돌리기

	@Test
	@DisplayName("resetMode(): 다른 모드(예: Stamp)를 끄면 칠하기로 돌아온다")
	void resetModeReturnsToBrushing() {
		mouse.setMode(new StampMode(game, new CellPattern(1, 1, Set.of(new java.awt.Point(0, 0))), pen));

		mouse.resetMode();

		assertSame(toggle, mouse.mode());
	}
}

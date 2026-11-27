package com.holub.life.mouse;

import com.holub.life.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * 원본 클릭 모드 테스트: ToggleMode를 MouseController에 넣고 마우스 입력을 보내 화면 없이 확인한다
 * 진짜 LifeGame·Board에 가짜 시계(FakeTicker)를 넣는다. 조립은 BrushModeTest와 같고 모드만 ToggleMode다
 *
 *   클래스
 *   - ToggleMode : 클릭한 셀을 켜고 끈다. LifeGame.toggle()을 부른다 (MouseMode State 패턴의 ConcreteState)
 *
 * [테스트 시나리오]
 *   1. 클릭: 죽은 칸을 누르면 기본색(빨강)으로 켜지고, 같은 칸을 다시 누르면 꺼진다
 *   2. 오른쪽 클릭: 왼쪽 클릭과 같다
 *
 * [커버리지]
 *   - ToggleModeTest.java 테스트 대상: ToggleMode (clicked, rightClicked)
 *   - 켜진 셀의 색은 BoardColors로 확인한다
 *   - 테스트 제외 대상
 *     - 펜 색으로 칠하기(앱의 기본 모드): BrushModeTest에서 확인한다
 *     - 클릭한 셀이 세대 진행에서 어떻게 되는지: ClickContractTest에서 확인한다
 */
@Tag("original")
class ToggleModeTest{
	private Board board;
	private FakeTicker ticker;
	private LifeGame game;
	private MouseController mouse;

	@BeforeEach
	void setUp(){
		board = new Board();
		ticker = new FakeTicker();
		game = new LifeGame(board, ticker);
		mouse = new MouseController(new ToggleMode(game));
	}

	//  1. 클릭

	@Test
	@DisplayName("clicked(): 죽은 칸을 누르면 기본색으로 켜지고, 같은 칸을 다시 누르면 꺼진다")
	void clickingTurnsACellOnAndClickingAgainTurnsItOff(){
		// Given: 빈 보드

		// When
		mouse.clicked(1, 1);

		// Then
		assertEquals(Cells.fromRowColumn("1,1"), board.liveCells());
		assertEquals(BoardColors.allIn(CellColor.DEFAULT, Cells.fromRowColumn("1,1")), BoardColors.of(board));

		// When
		mouse.clicked(1, 1);

		// Then
		assertEquals(Set.of(), board.liveCells());
	}

	//  2. 오른쪽 클릭

	@Test
	@DisplayName("rightClicked(): 오른쪽 클릭도 왼쪽 클릭과 같다")
	void rightClickIsTheSameAsLeftClick(){
		// Given: 빈 보드

		// When
		mouse.rightClicked(2, 2);

		// Then
		assertEquals(Cells.fromRowColumn("2,2"), board.liveCells());

		// When
		mouse.rightClicked(2, 2);

		// Then
		assertEquals(Set.of(), board.liveCells());
	}
}

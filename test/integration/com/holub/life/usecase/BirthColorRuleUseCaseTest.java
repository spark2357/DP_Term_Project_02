package com.holub.life.usecase;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.holub.life.game.LifeGame;
import com.holub.life.model.BirthColorRule;
import com.holub.life.model.BlendRule;
import com.holub.life.model.Board;
import com.holub.life.model.CellColor;
import com.holub.life.model.ClashRule;

/**
 * E. 출생 색 규칙 변경 유스케이스 통합 테스트: 팔레트의 Clash / Blend 버튼이 부르는 LifeGame.setBirthColorRule()을 화면 없이 실행
 * 진짜 LifeGame·Board에 가짜 시계(FakeTicker)와 가짜 리스너(RecordingGameListener)를 넣음
 *
 * [테스트 시나리오]
 *   1. 시작: 처음 규칙은 Clash (팔레트가 Clash를 선택한 상태로 열림)
 *   2. 규칙 바꾸기: 이미 산 셀의 자리와 색은 그대로이고, 화면 전체를 다시 그리라고(boardEdited) 알림
 *   3. 실행 중에 바꾸기: 게임은 멈추지 않음
 *
 * [커버리지]
 *   - BirthColorRuleUseCaseTest.java 테스트 대상: LifeGame.setBirthColorRule, LifeGame.birthColorRule
 *   - 테스트 제외 대상
 *     - BrushToolBar의 펜 버튼, 팔레트 팝업, 바깥 클릭으로 닫기 (GUI)
 *     - 바꾼 규칙으로 태어나는 셀의 색, 멈춘 패턴을 다시 계산하는지: CellColorBoardTest에서 확인
 *     - 규칙 변경이 Undo 기록에 남지 않음(의도한 동작), 따로 테스트하지 않음
 */
@Tag("extension")
class BirthColorRuleUseCaseTest{
	private static final CellColor BLUE = new CellColor(0, 0, 255);
	private static final CellColor GREEN = new CellColor(0, 128, 0);

	private final Board board = new Board();
	private final FakeTicker ticker = new FakeTicker();
	private final LifeGame game = new LifeGame(board, ticker);
	private final RecordingGameListener view = new RecordingGameListener();

	@Test
	@DisplayName("birthColorRule(): 처음 규칙은 Clash다")
	void startsWithClash(){
		// When
		BirthColorRule rule = game.birthColorRule();

		// Then
		assertInstanceOf(ClashRule.class, rule);
	}

	@Test
	@DisplayName("setBirthColorRule(): 규칙을 바꿔도 이미 산 셀의 자리와 색은 그대로이고, 화면 전체를 다시 그리라고 알린다")
	void ruleChangeKeepsExistingColors(){
		// Given: 여러 색 셀
		board.brush(10, 10, BLUE);
		board.brush(10, 11, GREEN);
		board.toggle(30, 40);
		Map<Point, CellColor> before = BoardColors.of(board);
		game.addGameListener(view);

		// When
		game.setBirthColorRule(new BlendRule());

		// Then
		assertEquals(before, BoardColors.of(board));
		assertEquals(List.of("edited"), view.events);
	}

	@Test
	@DisplayName("setBirthColorRule(): 실행 중에 규칙을 바꿔도 게임은 멈추지 않는다")
	void ruleChangeKeepsGameRunning(){
		// Given
		game.start(100);

		// When
		game.setBirthColorRule(new BlendRule());

		// Then
		assertTrue(ticker.running);
	}
}

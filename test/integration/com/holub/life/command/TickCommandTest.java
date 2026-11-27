package com.holub.life.command;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.holub.life.game.LifeGame;
import com.holub.life.model.Board;

/**
 * TickCommand 통합 테스트: Go → Tick (Single Step)
 * 진짜 LifeGame에 가짜 시계(FakeTicker)와 알림을 기록하는 가짜 화면(RecordingGameListener)을 넣어 메뉴와 시간 없이 실행함
 *
 * [테스트 시나리오]
 *   1. 실행하면 한 세대 진행하고, 시계는 시작하지 않음
 *   2. 보드가 바뀌면 화면에 바뀐 블록만 다시 그리라고(boardChanged) 알림
 *   3. 바뀐 셀이 없으면 알리지 않음
 *
 * [커버리지]
 *   - TickCommandTest.java 테스트 대상: execute
 */
class TickCommandTest {
	private final FakeTicker ticker = new FakeTicker();
	private final LifeGame game = new LifeGame(new Board(), ticker);
	private final RecordingGameListener view = new RecordingGameListener();

	@BeforeEach
	void listen() {
		game.addGameListener(view);
	}

	@Test
	@Tag("original")
	@DisplayName("execute(): 한 세대만 진행하고 시계는 시작하지 않는다")
	void executeAdvancesOneGeneration() {
		// Given: blinker
		placeBlinker();

		// When
		new TickCommand(game).execute();

		// Then
		assertEquals(Cells.fromRowColumn("2,3", "3,3", "4,3"), game.liveCells());
		assertFalse(ticker.running);
	}

	@Test
	@Tag("improvement")
	@DisplayName("execute(): 보드가 바뀌면 화면에 boardChanged를 알린다")
	void executeNotifiesChanged() {
		// Given: blinker. 놓을 때 받은 알림은 지움
		placeBlinker();
		view.events.clear();

		// When
		new TickCommand(game).execute();

		// Then
		assertEquals(List.of("changed"), view.events);
	}

	@Test
	@Tag("improvement")
	@DisplayName("execute(): 바뀐 셀이 없으면 알리지 않는다")
	void executeWithoutChangeNotifiesNothing() {
		// Given: 빈 보드

		// When
		new TickCommand(game).execute();

		// Then
		assertEquals(List.of(), view.events);
	}

	//  헬퍼

	private void placeBlinker() {
		game.toggle(3, 2);
		game.toggle(3, 3);
		game.toggle(3, 4);
	}
}

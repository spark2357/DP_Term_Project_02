package com.holub.life.command;

import com.holub.life.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * TickCommand 단위 테스트: Go → Tick (Single Step)
 * 진짜 LifeGame에 가짜 시계(FakeTicker)를 넣어 메뉴와 시간 없이 실행함
 *
 * [테스트 시나리오]
 *   1. 실행하면 한 세대 진행하고, 시계는 시작하지 않음
 *
 * [커버리지]
 *   - TickCommandTest.java 테스트 대상: execute
 */
@Tag("original")
class TickCommandTest {

	@Test
	@DisplayName("execute(): 한 세대만 진행하고 시계는 시작하지 않는다")
	void executeAdvancesOneGeneration() {
		// Given: blinker
		FakeTicker ticker = new FakeTicker();
		LifeGame game = new LifeGame(new Board(), ticker);
		game.toggle(3, 2);
		game.toggle(3, 3);
		game.toggle(3, 4);

		// When
		new TickCommand(game).execute();

		// Then
		assertEquals(Cells.fromRowColumn("2,3", "3,3", "4,3"), game.liveCells());
		assertFalse(ticker.running);
	}
}

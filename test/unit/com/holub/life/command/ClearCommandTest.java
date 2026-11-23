package com.holub.life.command;

import com.holub.life.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * ClearCommand 단위 테스트: Grid → Clear
 * 진짜 LifeGame에 가짜 시계(FakeTicker)를 넣어 메뉴와 화면 없이 실행함
 *
 * [테스트 시나리오]
 *   1. 실행하면 보드가 비어 있음
 *
 * [커버리지]
 *   - ClearCommandTest.java 테스트 대상: execute
 *   - 테스트 제외 대상
 *     - 화면에 다시 그리라는 알림: GridCommandTest, LifeGameTest에서 확인
 */
@Tag("original")
class ClearCommandTest {

	@Test
	@DisplayName("execute(): 보드를 비운다")
	void executeClearsBoard() {
		// Given
		LifeGame game = new LifeGame(new Board(), new FakeTicker());
		game.toggle(3, 3);
		game.toggle(40, 40);

		// When
		new ClearCommand(game).execute();

		// Then
		assertTrue(game.liveCells().isEmpty());
	}
}

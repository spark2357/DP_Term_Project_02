package com.holub.life.command;

import com.holub.life.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * HaltCommand 통합 테스트: Go → Halt
 * 진짜 LifeGame에 가짜 시계(FakeTicker)를 넣어 메뉴와 시간 없이 실행함
 *
 * [테스트 시나리오]
 *   1. 실행 중이면 멈춤
 *   2. 이미 멈춰 있어도 아무 문제 없음
 *
 * [커버리지]
 *   - HaltCommandTest.java 테스트 대상: execute
 */
@Tag("original")
class HaltCommandTest {
	private final FakeTicker ticker = new FakeTicker();
	private final LifeGame game = new LifeGame(new Board(), ticker);

	@Test
	@DisplayName("execute(): 실행 중인 게임을 멈춘다")
	void executeStopsRunningGame() {
		// Given
		game.start(70);

		// When
		new HaltCommand(game).execute();

		// Then
		assertFalse(ticker.running);
	}

	@Test
	@DisplayName("execute(): 이미 멈춘 게임에 실행해도 아무 문제 없다")
	void executeOnStoppedGameDoesNothing() {
		// Given: 시작하지 않은 게임

		// When, Then
		assertDoesNotThrow(() -> new HaltCommand(game).execute());
		assertFalse(ticker.running);
	}
}

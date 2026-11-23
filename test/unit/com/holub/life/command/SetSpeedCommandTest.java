package com.holub.life.command;

import com.holub.life.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * SetSpeedCommand 단위 테스트: Go → Agonizing / Slow / Medium / Fast
 * 진짜 LifeGame에 가짜 시계(FakeTicker)를 넣어 메뉴와 시간 없이 실행함
 *
 * [테스트 시나리오]
 *   1. 실행하면 정해진 간격으로 게임을 시작함 (원본 속도 500, 150, 70, 30ms)
 *   2. 0 이하의 간격으로는 만들 수 없음 (멈춤은 HaltCommand가 맡음)
 *
 * [커버리지]
 *   - SetSpeedCommandTest.java 테스트 대상: 생성자, execute
 *   - 테스트 제외 대상
 *     - 실제로 그 간격마다 tick하는지: 타이머(SwingTickTimer)의 일이라 제외
 */
@Tag("original")
class SetSpeedCommandTest {

	@ParameterizedTest(name = "{0}ms")
	@ValueSource(ints = { 500, 150, 70, 30 })
	@DisplayName("execute(): 정해진 간격으로 게임을 시작한다")
	void executeStartsGameAtThatSpeed(int milliseconds) {
		// Given
		FakeTicker ticker = new FakeTicker();
		LifeGame game = new LifeGame(new Board(), ticker);

		// When
		new SetSpeedCommand(game, milliseconds).execute();

		// Then
		assertTrue(ticker.running);
		assertEquals(milliseconds, ticker.millisecondsBetweenTicks);
	}

	@Test
	@DisplayName("생성자: 0 이하의 간격으로는 만들 수 없다")
	void rejectsNonPositiveInterval() {
		LifeGame game = new LifeGame(new Board(), new FakeTicker());
		assertThrows(IllegalArgumentException.class, () -> new SetSpeedCommand(game, 0));
	}
}

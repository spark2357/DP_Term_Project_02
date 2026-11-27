package com.holub.life.command;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.holub.life.game.LifeGame;
import com.holub.life.game.RunRecorder;
import com.holub.life.model.Board;

/**
 * MacroCommand 통합 테스트: 여러 Command를 묶어 순서대로 실행하는 Command (Command에 Composite 적용, E. 통계 리포트 표시)
 * 순서는 실행을 기록하는 가짜 Command로, 실제 묶음(Halt = 멈추기 + 리포트)은 진짜 LifeGame·RunRecorder에 가짜 시계(FakeTicker)를 넣어 확인함
 *
 * [테스트 시나리오]
 *   1. 순서: 넘긴 순서대로 한 번씩 실행함
 *   2. Composite: MacroCommand 안에 MacroCommand를 넣어도 안쪽 것까지 순서대로 실행함
 *   3. Halt 묶음: HaltCommand + ShowRunReportCommand를 묶으면 게임이 먼저 멈춘 뒤 리포트가 출력됨
 *
 * [커버리지]
 *   - MacroCommandTest.java 테스트 대상: 생성자(실행 순서), execute
 *   - 테스트 제외 대상
 *     - 리포트 글자 형식: ShowRunReportCommandTest에서 확인
 *     - Go → Halt 메뉴와 멸종에 연결된 흐름: RunReportUseCaseTest에서 확인
 */
@Tag("extension")
class MacroCommandTest{
	private final List<String> executed = new ArrayList<>();

	//  1. 순서

	@Test
	@DisplayName("execute(): 넘긴 Command를 넘긴 순서대로 한 번씩 실행한다")
	void executesCommandsInOrder(){
		// Given
		Command macro = new MacroCommand(record("a"), record("b"), record("c"));

		// When
		macro.execute();

		// Then
		assertEquals(List.of("a", "b", "c"), executed);
	}

	//  2. Composite

	@Test
	@DisplayName("execute(): 안에 든 MacroCommand도 다른 Command처럼 제자리에서 순서대로 실행한다")
	void executesNestedMacroInPlace(){
		// Given
		Command macro = new MacroCommand(record("a"), new MacroCommand(record("b"), record("c")), record("d"));

		// When
		macro.execute();

		// Then
		assertEquals(List.of("a", "b", "c", "d"), executed);
	}

	//  3. Halt 묶음

	@Test
	@DisplayName("execute(): HaltCommand와 ShowRunReportCommand를 묶으면 게임이 멈춘 뒤에 리포트가 출력된다")
	void haltAndReportStopsBeforeReporting(){
		// Given: blinker를 실행 중 2세대 진행 (Life.main과 같은 묶음)
		FakeTicker ticker = new FakeTicker();
		LifeGame game = new LifeGame(new Board(), ticker);
		RunRecorder recorder = new RunRecorder(game);
		game.toggle(3, 2);
		game.toggle(3, 3);
		game.toggle(3, 4);
		game.start(70);
		ticker.tick();
		ticker.tick();

		List<Boolean> runningWhenShown = new ArrayList<>();
		Command haltAndReport = new MacroCommand(
				new HaltCommand(game),
				new ShowRunReportCommand(recorder, "사용자 정지", text -> runningWhenShown.add(ticker.running)));

		// When
		haltAndReport.execute();

		// Then: 리포트가 한 번 출력되었고, 그때 이미 멈춰 있었음
		assertEquals(List.of(false), runningWhenShown);
	}

	// ---- 헬퍼 메서드

	// 실행되면 이름을 기록하는 Command
	private Command record(String name){
		return () -> executed.add(name);
	}
}

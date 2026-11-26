package com.holub.life.command;

import com.holub.life.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * ShowRunReportCommand 통합 테스트: 이번 실행의 통계 요약을 글자로 만들어 출력 함수에 넘김 (E. 통계 리포트 표시)
 * 진짜 LifeGame·Board·RunRecorder에 가짜 시계(FakeTicker)를 넣고, 출력 함수는 받은 글자를 모으는 List로 바꿔 화면 없이 실행함
 *
 *   리포트 형식: "멈춘 이유 · N세대 · 현재 N셀 · 평균 N.N셀 · 활성 블록 평균 N.N/전체 블록"
 *
 * [테스트 시나리오]
 *   1. 출력: 실행한 뒤 execute()하면 멈춘 이유와 요약을 한 줄로 출력함
 *   2. 진행한 세대가 없음: 아무것도 출력하지 않음. 한 번 출력한 뒤 다시 execute()해도 출력하지 않음 (이전 리포트가 그대로 남음)
 *   3. 로케일: 기본 로케일이 독일어여도 소수점은 점(3.0)으로 나와야 함
 *      지금은 String.format에 Locale을 넘기지 않아 "3,0"으로 나오는 알려진 버그라, 태우가 형식을 고치기 전까지 실패함
 *
 * [커버리지]
 *   - ShowRunReportCommandTest.java 테스트 대상: execute, 리포트 글자 형식
 *   - 기본 로케일을 바꾸는 테스트가 있어, 테스트마다 원래 로케일로 되돌림(@AfterEach). 1, 2는 한국어 로케일로 고정함
 *   - 테스트 제외 대상
 *     - 평균을 어떻게 세는지: RunRecorderTest에서 확인
 *     - Halt 메뉴와 멸종에 연결된 흐름: RunReportUseCaseTest에서 확인
 */
@Tag("extension")
class ShowRunReportCommandTest{
	private final Board board = new Board();
	private final LifeGame game = new LifeGame(board, new FakeTicker());
	private final RunRecorder recorder = new RunRecorder(game);
	private final List<String> shown = new ArrayList<>();
	private final Command report = new ShowRunReportCommand(recorder, "사용자 정지", shown::add);

	private Locale originalLocale;

	@BeforeEach
	void pinLocale(){
		originalLocale = Locale.getDefault();
		Locale.setDefault(Locale.KOREA);
	}

	@AfterEach
	void restoreLocale(){
		Locale.setDefault(originalLocale);
	}

	//  1. 출력

	@Test
	@DisplayName("execute(): 실행한 뒤에는 멈춘 이유와 세대 수, 현재 셀, 평균 셀, 활성 블록 평균을 한 줄로 출력한다")
	void showsReasonAndSummary(){
		// Given
		runBlinker(4);

		// When
		report.execute();

		// Then
		assertEquals(List.of("사용자 정지 · 4세대 · 현재 3셀 · 평균 3.0셀 · 활성 블록 평균 1.0/64"), shown);
	}

	//  2. 진행한 세대가 없음

	@Test
	@DisplayName("execute(): 진행한 세대가 없으면 아무것도 출력하지 않는다")
	void showsNothingWithoutGenerations(){
		// Given: 셀은 있지만 진행하지 않음
		game.toggle(5, 5);

		// When
		report.execute();

		// Then
		assertEquals(List.of(), shown);
	}

	@Test
	@DisplayName("execute(): 출력한 뒤 더 진행하지 않고 다시 실행하면 출력하지 않는다 (이전 리포트가 그대로 남음)")
	void secondReportWithoutNewGenerationsShowsNothing(){
		// Given
		runBlinker(2);
		report.execute();

		// When
		report.execute();

		// Then
		assertEquals(1, shown.size());
	}

	//  3. 로케일

	@Test
	@DisplayName("execute(): 로케일과 관계없이 소수점은 점으로 표시한다")
	void decimalPointDoesNotDependOnLocale(){
		// Given: 기본 로케일이 독일어 (소수점이 쉼표)
		Locale.setDefault(Locale.GERMANY);
		runBlinker(4);

		// When
		report.execute();

		// Then
		assertEquals(List.of("사용자 정지 · 4세대 · 현재 3셀 · 평균 3.0셀 · 활성 블록 평균 1.0/64"), shown);
	}

	// ---- 헬퍼 메서드

	// 블록 하나 안쪽에서만 바뀌는 blinker(3셀)를 놓고 generations세대 진행. 활성 블록은 세대마다 1개
	private void runBlinker(int generations){
		for (Point p : Cells.fromRowColumn("3,2", "3,3", "3,4")){
			game.toggle(p.y, p.x);
		}
		for (int i = 0; i < generations; i++){
			game.step();
		}
	}
}

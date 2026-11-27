package com.holub.life.game;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.holub.life.model.Board;

/**
 * RunRecorder 통합 테스트: 한 번의 실행 동안 세대마다 통계를 모아 요약하는 Observer (E. 통계 리포트 표시, E. 멸종 시 자동 정지)
 * 진짜 LifeGame·Board에 가짜 시계(FakeTicker)를 넣고, 세대는 step()을 직접 불러 진행함 (시간 없음)
 * 통합 테스트에 둔 이유: RunRecorder는 인터페이스가 아닌 구체 클래스 LifeGame에 의존해서(생성자로 받아 구독하고,
 * 세대 수와 통계를 LifeGame에 물음) 가짜를 끼울 자리가 없음. LifeGame을 상속해 가짜를 만들어도 생성자가 진짜 Board를 요구하므로,
 * 진짜 LifeGame과 Board(보드 전체)로 테스트함
 *
 *   메서드
 *   - boardChanged()       : 보드가 바뀐 세대마다 LifeGame이 부름. 셀 수와 활성 블록 수를 더하고, 셀이 0이면 멸종 동작 실행
 *   - boardEdited()        : 클릭·Clear·Load·Stamp 뒤에 부름. 세대로 세지 않고 지금 셀 수만 갱신
 *   - finishRun()          : 이번 실행의 요약(RunReport). 진행한 세대가 없으면 빈 값. 다음 실행을 위해 기록을 비움
 *   - whenExtinct(action)  : 모든 셀이 죽었을 때 할 일
 *
 * [테스트 시나리오]
 *   1. 빈 실행: 진행한 세대가 없으면 빈 값
 *   2. 요약 값: blinker 4세대 → 세대 수, 현재 셀 수, 평균 셀 수, 활성 블록 평균, 전체 블록 수
 *   3. 안 바뀐 세대: 알림이 없는 세대(정지한 block)도 세대 수와 평균에 들어감. 바뀐 세대와 안 바뀐 세대가 섞여도 평균이 맞음
 *   4. 실행 중 편집: 클릭으로 늘어난 셀은 세대로 세지 않고 그 뒤 세대부터 평균에 들어감
 *   5. 다음 실행: finishRun() 뒤에는 새 실행으로 다시 셈
 *   6. 멸종: 마지막 셀이 죽는 세대에 멸종 동작이 한 번 실행됨. 셀이 남아 있으면 실행되지 않음
 *
 * [커버리지]
 *   - RunRecorderTest.java 테스트 대상: boardChanged, boardEdited, finishRun, whenExtinct
 *   - 테스트 제외 대상
 *     - 리포트 글자와 멈추기: ShowRunReportCommandTest, RunReportUseCaseTest에서 확인
 *     - 빈 보드에서 시작했을 때 멸종 동작을 실행하지 않는지(의도한 동작): RunReportUseCaseTest에서 확인
 */
@Tag("extension")
class RunRecorderTest{
	private static final Set<Point> BLINKER = Cells.fromRowColumn("3,2", "3,3", "3,4");		// 블록 하나 안쪽에서만 바뀜
	private static final Set<Point> BLOCK = Cells.fromRowColumn("20,20", "20,21", "21,20", "21,21");	// 바뀌지 않는 패턴
	private static final int TOTAL_BLOCKS = 64;		// 64x64 보드 = 8x8 블록 64개

	private final Board board = new Board();
	private final LifeGame game = new LifeGame(board, new FakeTicker());
	private final RunRecorder recorder = new RunRecorder(game);

	//  1. 빈 실행

	@Test
	@DisplayName("finishRun(): 진행한 세대가 없으면 빈 값을 돌려준다")
	void finishRunWithoutGenerationsIsEmpty(){
		// Given
		place(BLINKER);

		// When, Then
		assertEquals(Optional.empty(), recorder.finishRun());
	}

	//  2. 요약 값

	@Test
	@DisplayName("finishRun(): blinker를 4세대 진행하면 4세대, 현재 3셀, 평균 3셀, 활성 블록 평균 1, 전체 64블록으로 요약한다")
	void summarizesBlinkerRun(){
		// Given
		place(BLINKER);
		steps(4);

		// When
		Optional<RunReport> report = recorder.finishRun();

		// Then
		assertEquals(Optional.of(new RunReport(4, 3, 3.0, 1.0, TOTAL_BLOCKS)), report);
	}

	//  3. 안 바뀐 세대

	@Test
	@DisplayName("finishRun(): 보드가 바뀌지 않아 알림이 없던 세대(정지한 block)도 세대 수와 평균에 들어간다")
	void unchangedGenerationsAreCounted(){
		// Given
		place(BLOCK);
		steps(5);

		// When
		Optional<RunReport> report = recorder.finishRun();

		// Then: 활성 블록은 0으로 평균에 들어감
		assertEquals(Optional.of(new RunReport(5, 4, 4.0, 0.0, TOTAL_BLOCKS)), report);
	}

	@Test
	@DisplayName("finishRun(): 바뀐 세대와 안 바뀐 세대가 섞여도 평균 셀 수는 모든 세대의 셀 수 평균이다")
	void averageCoversChangedAndUnchangedGenerations(){
		// Given: L자 4셀은 세대마다 4, 6, 6, 6, 6셀 (1~3세대는 바뀌고, 4~5세대는 beehive로 멈춰 알림이 없음)
		place(Cells.fromRowColumn("10,10", "10,11", "10,12", "11,10"));
		steps(5);

		// When
		RunReport report = recorder.finishRun().orElseThrow();

		// Then: (4 + 6 + 6 + 6 + 6) / 5
		assertEquals(5, report.generations());
		assertEquals(28.0 / 5, report.averageLiveCells(), 1e-9);
	}

	//  4. 실행 중 편집

	@Test
	@DisplayName("boardEdited(): 실행 중 클릭으로 늘어난 셀은 세대로 세지 않고, 그 뒤 세대부터 평균에 들어간다")
	void cellsAddedDuringRunCountFromTheNextGeneration(){
		// Given: block 2세대 → block 하나를 더 놓음(클릭 4번) → 2세대
		place(BLOCK);
		steps(2);
		place(Cells.fromRowColumn("40,40", "40,41", "41,40", "41,41"));
		steps(2);

		// When
		RunReport report = recorder.finishRun().orElseThrow();

		// Then: (4 + 4 + 8 + 8) / 4
		assertEquals(4, report.generations());
		assertEquals(8, report.finalCells());
		assertEquals(6.0, report.averageLiveCells(), 1e-9);
	}

	//  5. 다음 실행

	@Test
	@DisplayName("finishRun(): 요약한 뒤에는 기록을 비워, 다음 실행은 그때부터 다시 센다")
	void finishRunStartsANewRun(){
		// Given: 3세대 진행하고 요약함
		place(BLINKER);
		steps(3);
		recorder.finishRun();

		// When
		steps(2);

		// Then
		assertEquals(Optional.of(new RunReport(2, 3, 3.0, 1.0, TOTAL_BLOCKS)), recorder.finishRun());
	}

	//  6. 멸종

	@Test
	@DisplayName("whenExtinct(): 마지막 셀이 죽는 세대에 멸종 동작이 한 번 실행된다")
	void extinctionActionRunsWhenLastCellsDie(){
		// Given: 혼자 있는 셀은 다음 세대에 죽음
		int[] calls = {0};
		recorder.whenExtinct(() -> calls[0]++);
		place(Cells.fromRowColumn("5,5"));

		// When
		game.step();

		// Then
		assertEquals(1, calls[0]);
	}

	@Test
	@DisplayName("whenExtinct(): 살아 있는 셀이 남아 있으면 멸종 동작은 실행되지 않는다")
	void extinctionActionDoesNotRunWhileCellsLive(){
		// Given
		int[] calls = {0};
		recorder.whenExtinct(() -> calls[0]++);
		place(BLINKER);

		// When
		steps(4);

		// Then
		assertEquals(0, calls[0]);
	}

	// ---- 헬퍼 메서드

	// 실행 전 클릭처럼 LifeGame으로 놓음 (RunRecorder가 boardEdited로 셀 수를 앎)
	private void place(Set<Point> cells){
		for (Point p : cells){
			game.toggle(p.y, p.x);
		}
	}

	private void steps(int count){
		for (int i = 0; i < count; i++){
			game.step();
		}
	}
}

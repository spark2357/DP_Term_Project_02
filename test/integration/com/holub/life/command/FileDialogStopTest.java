package com.holub.life.command;

import com.holub.life.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Load, Store Command 통합 테스트: 파일 창이 열리는 순간 게임이 이미 멈춰 있는지 (UC3 패턴 불러오기, UC4 패턴 저장)
 * 진짜 LifeGame·Board에 가짜 시계(FakeTicker)와 가짜 파일 창(FakeDialogs)을 넣고, 파일 창이 열릴 때 시계 상태를 기록함
 * 창이 떠 있는 동안 tick이 오면 Load는 읽기 전 보드가, Store는 OK를 누른 순간의 보드가 바뀌므로, 창을 열기 전에 멈춰야 함
 *
 * [테스트 시나리오]
 *   1. Load: 실행 중에 Load를 누르면 파일 창이 열릴 때 이미 멈춰 있음 (사용자는 취소)
 *   2. Store: 실행 중에 Store를 누르면 파일 창이 열릴 때 이미 멈춰 있음 (사용자는 취소)
 *
 * [커버리지]
 *   - FileDialogStopTest.java 테스트 대상: LoadCommand.execute, StoreCommand.execute (파일 창을 열기 전의 정지)
 *   - 테스트 제외 대상
 *     - 파일을 고른 뒤 끝났을 때 멈춰 있는지: LoadCommandStopTest, GridCommandTest에서 확인
 *     - 취소한 뒤 보드와 오류 창: LoadCommandTest, StoreCommandTest에서 확인
 *     - 원본에서 파일 창이 뜬 뒤에야 멈추는 것(원본 쪽 known-defect): 원본 Universe.doLoad(), doStore()는 파일 창(Files.userSelected())을
 *       먼저 열고 그 뒤에 Clock.instance().stop()을 부름. 그런데 파일 창이 static 메서드로 진짜 모달 JFileChooser를 띄우고,
 *       Universe(JPanel)와 Clock(싱글톤)에 묶여 있어 가짜 창을 넣을 수 없음. 화면 없이는 실행할 수 없어 원본 쪽 테스트를 만들지 않음
 */
@Tag("improvement")
class FileDialogStopTest{
	private final FakeTicker ticker = new FakeTicker();
	private final LifeGame game = new LifeGame(new Board(), ticker);
	private final FakeDialogs dialogs = new FakeDialogs();
	private final PatternFormats formats = new PatternFormats(
			List.of(new LifeFormatAdapter(LifeTestDriver.SIZE), new RleFormat(LifeTestDriver.SIZE)));
	private Boolean runningWhenChoosing;		// 파일 창이 열린 순간의 ticker.running. 창이 안 열렸으면 null

	@BeforeEach
	void startGameAndWatchFileDialog(){
		game.start(30);
		dialogs.nextFile = null;			// 사용자는 취소
		dialogs.onChooseFile = () -> runningWhenChoosing = ticker.running;
	}

	@Test
	@DisplayName("LoadCommand.execute(): 파일 창이 열릴 때는 게임이 이미 멈춰 있다 (결함)")
	void loadStopsBeforeFileDialog(){
		// Given: 실행 중인 게임(startGameAndWatchFileDialog). 원본 Universe.doLoad()는 파일 창을 닫은 뒤에 멈췄음

		// When
		new LoadCommand(game, formats, dialogs).execute();

		// Then
		assertEquals(Boolean.FALSE, runningWhenChoosing);
	}

	@Test
	@DisplayName("StoreCommand.execute(): 파일 창이 열릴 때는 게임이 이미 멈춰 있다 (결함)")
	void storeStopsBeforeFileDialog(){
		// Given: 실행 중인 게임(startGameAndWatchFileDialog). 원본 Universe.doStore()는 파일 창을 닫은 뒤에 멈췄음

		// When
		new StoreCommand(game, formats, dialogs).execute();

		// Then
		assertEquals(Boolean.FALSE, runningWhenChoosing);
	}
}

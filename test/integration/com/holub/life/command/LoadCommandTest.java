package com.holub.life.command;

import com.holub.life.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * LoadCommand 통합 테스트: Grid → Load에서 파일을 고르지 않고 끝나는 경로
 * 진짜 LifeGame·Board에 가짜 파일 창(FakeDialogs)과 가짜 시계(FakeTicker)를 넣어 화면과 파일 없이 실행함
 *
 * [테스트 시나리오]
 *   1. 파일 창에서 취소: 보드는 그대로이고 오류 창도 없음
 *
 * [커버리지]
 *   - LoadCommandTest.java 테스트 대상: execute (파일 창에서 취소한 경로)
 *   - 테스트 제외 대상
 *     - 지원하지 않는 확장자, 실제 파일 읽기, 깨진 파일: GridCommandTest에서 확인
 *     - 취소했을 때 게임: 파일 창을 열기 전에 멈추므로 취소해도 멈춘 채로 남음 (FileDialogStopTest에서 확인)
 *     - Files, SwingDialogs: Swing 파일 창·오류 창이라 가짜 Dialogs(FakeDialogs)로 대신함
 */
@Tag("improvement")
class LoadCommandTest {
	private final FakeDialogs dialogs = new FakeDialogs();
	private final LifeGame game = new LifeGame(new Board(), new FakeTicker());
	private final PatternFormats formats = new PatternFormats(
			List.of(new LifeFormatAdapter(LifeTestDriver.SIZE), new RleFormat(LifeTestDriver.SIZE)));

	@Test
	@DisplayName("execute(): 파일 창에서 취소하면 보드는 그대로이고 오류 창도 뜨지 않는다 (결함)")
	void cancelKeepsBoard() {
		// Given: 원본 Universe.doLoad()는 취소하면 Files.userSelected()가 던진 예외를 받아 "Read Failed!" 오류 창을 띄움
		game.toggle(1, 1);
		dialogs.nextFile = null;

		// When
		new LoadCommand(game, formats, dialogs).execute();

		// Then
		assertEquals(Cells.fromRowColumn("1,1"), game.liveCells());
		assertEquals(List.of(), dialogs.errors);
	}
}

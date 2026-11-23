package com.holub.life.command;

import com.holub.life.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * LoadCommand 단위 테스트: Grid → Load에서 파일을 읽기 전에 끝나는 경로
 * 파일 창은 가짜(FakeDialogs), 시계는 가짜(FakeTicker)로 바꿔 화면과 파일 없이 실행함
 *
 * [테스트 시나리오]
 *   1. 파일 창에서 취소: 보드는 그대로이고 오류 창도 없음
 *   2. 지원하지 않는 확장자: 지원 형식을 알려주는 오류 창, 보드는 그대로
 *
 * [커버리지]
 *   - LoadCommandTest.java 테스트 대상: execute (파일을 열기 전 경로)
 *   - 테스트 제외 대상
 *     - 실제 파일 읽기, 깨진 파일: 통합 테스트 GridCommandTest에서 확인
 *     - 취소했을 때 게임을 멈출지: 팀에서 아직 정하지 않아 확인하지 않음
 */
@Tag("original")
class LoadCommandTest {
	private final FakeDialogs dialogs = new FakeDialogs();
	private final LifeGame game = new LifeGame(new Board(), new FakeTicker());
	private final PatternFormats formats = new PatternFormats(
			List.of(new LifeFormatAdapter(LifeTestDriver.SIZE), new RleFormat(LifeTestDriver.SIZE)));

	@Test
	@DisplayName("execute(): 파일 창에서 취소하면 보드는 그대로이고 오류 창도 뜨지 않는다")
	void cancelKeepsBoard() {
		// Given
		game.toggle(1, 1);
		dialogs.nextFile = null;

		// When
		new LoadCommand(game, formats, dialogs).execute();

		// Then
		assertEquals(Cells.fromRowColumn("1,1"), game.liveCells());
		assertEquals(List.of(), dialogs.errors);
	}

	@Test
	@Tag("extension")
	@DisplayName("execute(): 지원하지 않는 확장자를 고르면 지원 형식을 알려주는 오류 창이 뜨고, 보드는 그대로다")
	void unsupportedExtensionShowsError() {
		// Given: 확장자만 보므로 실제 파일은 없어도 됨
		game.toggle(1, 1);
		dialogs.nextFile = new File("pattern.txt");

		// When
		new LoadCommand(game, formats, dialogs).execute();

		// Then
		assertEquals(Cells.fromRowColumn("1,1"), game.liveCells());
		assertEquals(1, dialogs.errors.size());
		assertTrue(dialogs.errors.get(0).contains(".life, .rle"));
	}
}

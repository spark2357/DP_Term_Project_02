package com.holub.life.command;

import com.holub.life.*;
import com.holub.life.mouse.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * StampCommand 통합 테스트: Grid → Stamp에서 파일을 읽기 전에 끝나는 경로
 * 진짜 LifeGame·Board와 MouseController에 가짜 파일 창(FakeDialogs)을 넣어 화면과 파일 없이 실행함
 *
 * [테스트 시나리오]
 *   1. 파일 창에서 취소: 마우스 모드는 그대로(토글)
 *   2. 스탬프 모드에서 다시 실행: 토글 모드로 돌아옴
 *
 * [커버리지]
 *   - StampCommandTest.java 테스트 대상: execute
 *   - 테스트 제외 대상
 *     - 패턴 파일을 읽어 스탬프 모드 켜기, 찍기, 회전: GridCommandTest에서 확인
 *     - 깨진 패턴 파일: 아직 테스트 없음 (기능 확장 이슈에서 다룸)
 */
@Tag("extension")
class StampCommandTest {
	private final FakeDialogs dialogs = new FakeDialogs();
	private final LifeGame game = new LifeGame(new Board(), new FakeTicker());
	private final RleFormat rle = new RleFormat(LifeTestDriver.SIZE);
	private final MouseController mouse = new MouseController(new ToggleMode(game));

	@Test
	@DisplayName("execute(): 파일 창에서 취소하면 마우스 모드는 그대로다")
	void cancelKeepsToggleMode() {
		// Given
		dialogs.nextFile = null;

		// When
		new StampCommand(game, rle, mouse, dialogs).execute();

		// Then
		assertInstanceOf(ToggleMode.class, mouse.mode());
		assertEquals(List.of(), dialogs.errors);
	}

	@Test
	@DisplayName("execute(): 스탬프 모드에서 다시 실행하면 토글 모드로 돌아온다")
	void executeInStampModeTurnsItOff() {
		// Given: 스탬프 모드
		mouse.setMode(new StampMode(game, new CellPattern(1, 1, Cells.fromRowColumn("0,0"))));

		// When
		new StampCommand(game, rle, mouse, dialogs).execute();

		// Then
		assertInstanceOf(ToggleMode.class, mouse.mode());
	}
}

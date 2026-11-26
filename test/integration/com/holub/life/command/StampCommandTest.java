package com.holub.life.command;

import com.holub.life.*;
import com.holub.life.mouse.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * StampCommand 통합 테스트: E. 스탬프 모드 켜기. Grid → Stamp로 스탬프 모드를 켜고 끔
 * 진짜 LifeGame·Board와 MouseController에 가짜 파일 창(FakeDialogs)을 넣어 화면 없이 실행함. 켜기 성공은 test/resources/glider.rle를 읽음
 *
 * [테스트 시나리오]
 *   1. 켜기: .rle 파일을 고르면 그 패턴으로 스탬프 모드가 켜짐
 *   2. 파일 창에서 취소: 마우스 모드는 그대로(토글)
 *   3. 스탬프 모드에서 다시 실행: 토글 모드로 돌아옴
 *
 * [커버리지]
 *   - StampCommandTest.java 테스트 대상: execute
 *   - 테스트 제외 대상
 *     - 찍기, 회전: StampModeTest, GridCommandTest에서 확인
 *     - 깨진 패턴 파일: StampCommandFailureTest에서 확인
 *     - 켤 때 실행 중인 게임을 멈추는지: 팀에서 동작을 정하는 중이라 제외
 */
@Tag("extension")
class StampCommandTest {
	private final FakeDialogs dialogs = new FakeDialogs();
	private final LifeGame game = new LifeGame(new Board(), new FakeTicker());
	private final RleFormat rle = new RleFormat(LifeTestDriver.SIZE);
	private final Pen pen = new Pen(CellColor.DEFAULT);
	private final MouseController mouse = new MouseController(new ToggleMode(game, pen));

	@Test
	@DisplayName("execute(): .rle 파일을 고르면 그 패턴으로 스탬프 모드를 켠다")
	void chosenRleTurnsOnStampModeWithItsPattern(){
		// Given
		dialogs.nextFile = new File("test/resources/glider.rle");

		// When
		new StampCommand(game, rle, mouse, pen, dialogs).execute();

		// Then
		StampMode stamp = assertInstanceOf(StampMode.class, mouse.mode());
		CellPattern glider = new CellPattern(3, 3, Cells.fromRowColumn("0,1", "1,2", "2,0", "2,1", "2,2"));
		assertEquals(glider, stamp.pattern());
		assertEquals(List.of(), dialogs.errors);
	}

	@Test
	@DisplayName("execute(): 파일 창에서 취소하면 마우스 모드는 그대로다")
	void cancelKeepsToggleMode() {
		// Given
		dialogs.nextFile = null;

		// When
		new StampCommand(game, rle, mouse, pen, dialogs).execute();

		// Then
		assertInstanceOf(ToggleMode.class, mouse.mode());
		assertEquals(List.of(), dialogs.errors);
	}

	@Test
	@DisplayName("execute(): 스탬프 모드에서 다시 실행하면 토글 모드로 돌아온다")
	void executeInStampModeTurnsItOff() {
		// Given: 스탬프 모드
		mouse.setMode(new StampMode(game, new CellPattern(1, 1, Cells.fromRowColumn("0,0")), pen));

		// When
		new StampCommand(game, rle, mouse, pen, dialogs).execute();

		// Then
		assertInstanceOf(ToggleMode.class, mouse.mode());
	}
}

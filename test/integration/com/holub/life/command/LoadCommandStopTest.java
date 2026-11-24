package com.holub.life.command;

import com.holub.life.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * LoadCommand 통합 테스트: Grid → Load에서 실행 중이던 게임이 멈추는지 (기존 기능)
 * 진짜 LifeGame·Board에 가짜 파일 창(FakeDialogs)과 가짜 시계(FakeTicker)를 넣어 화면 없이 실행함. 파일은 임시 폴더(@TempDir)에 실제로 씀
 *
 * LoadCommandTest와 따로 둔 이유
 *   - LoadCommandTest는 클래스에 improvement 태그가 붙어 있음 (취소 테스트가 원본 결함 수정이라서)
 *   - JUnit은 클래스 태그를 메서드가 무조건 물려받고, 물려받지 않게 막는 방법이 없음
 *   - 이 테스트는 원본 Universe.doLoad()도 시계를 멈추던 기존 기능이라 original만 붙어야 해서 클래스를 나눔
 *   - 통합 테스트는 클래스이름Test 규칙이 아니라 시나리오 이름으로 짓기 때문에(GridCommandTest, RleBoardLoadTest 등) 따로 두어도 됨
 *
 * [테스트 시나리오]
 *   1. 실행 중에 .life 파일을 Load하면 게임이 멈춤
 *
 * [커버리지]
 *   - LoadCommandStopTest.java 테스트 대상: execute (파일을 읽어 성공한 경로의 시계 정지)
 *   - 테스트 제외 대상
 *     - 파일 창에서 취소: LoadCommandTest에서 확인
 *     - 같은 자리로 돌아오는지, 화면 알림, 실패한 Load: GridCommandTest에서 확인
 */
@Tag("original")
class LoadCommandStopTest {
	@TempDir
	Path folder;

	private final FakeDialogs dialogs = new FakeDialogs();
	private final FakeTicker ticker = new FakeTicker();
	private final LifeGame game = new LifeGame(new Board(), ticker);
	private final PatternFormats formats = new PatternFormats(
			List.of(new LifeFormatAdapter(LifeTestDriver.SIZE), new RleFormat(LifeTestDriver.SIZE)));

	@Test
	@DisplayName("execute(): Load하면 실행 중이던 게임이 멈춘다")
	void loadStopsGame() throws IOException {
		// Given: 셀 하나를 .life로 저장해 두고, 게임은 실행 중
		File file = folder.resolve("pattern.life").toFile();
		game.toggle(1, 1);
		try (OutputStream out = new FileOutputStream(file)) {
			game.store(out, new LifeFormatAdapter(LifeTestDriver.SIZE));
		}
		game.start(30);
		dialogs.nextFile = file;

		// When
		new LoadCommand(game, formats, dialogs).execute();

		// Then
		assertFalse(ticker.running);
		assertEquals(List.of(), dialogs.errors);
	}
}

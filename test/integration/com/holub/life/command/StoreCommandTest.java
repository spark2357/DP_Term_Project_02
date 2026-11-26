package com.holub.life.command;

import com.holub.life.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * StoreCommand 통합 테스트: Grid → Store
 * 진짜 LifeGame·Board에 가짜 파일 창(FakeDialogs)과 가짜 시계(FakeTicker)를 넣어 화면 없이 실행함. 파일은 임시 폴더(@TempDir)에 실제로 씀
 *
 * [테스트 시나리오]
 *   1. 파일 창에서 취소: 오류 창 없음
 *   2. 지원하지 않는 확장자: 지원 형식을 알려주는 오류 창, 파일은 만들어지지 않음
 *   3. 저장하면 게임이 멈춤
 *
 * [커버리지]
 *   - StoreCommandTest.java 테스트 대상: execute
 *   - 테스트 제외 대상
 *     - 저장한 파일을 다시 Load: GridCommandTest에서 확인
 *     - 쓸 수 없는 폴더: 운영체제마다 달라서 제외
 */
class StoreCommandTest {
	@TempDir
	Path folder;

	private final FakeDialogs dialogs = new FakeDialogs();
	private final FakeTicker ticker = new FakeTicker();
	private final LifeGame game = new LifeGame(new Board(), ticker);
	private final PatternFormats formats = new PatternFormats(
			List.of(new LifeFormatAdapter(LifeTestDriver.SIZE), new RleFormat(LifeTestDriver.SIZE)));

	@Test
	@Tag("improvement")
	@DisplayName("execute(): 파일 창에서 취소하면 오류 창이 뜨지 않는다 (결함)")
	void cancelShowsNoError() {
		// Given: 원본 Universe.doStore()는 취소하면 Files.userSelected()가 던진 예외를 받아 "Write Failed!" 오류 창을 띄움
		dialogs.nextFile = null;

		// When
		new StoreCommand(game, formats, dialogs).execute();

		// Then
		assertEquals(List.of(), dialogs.errors);
	}

	@Test
	@Tag("extension")
	@DisplayName("execute(): 지원하지 않는 확장자를 고르면 오류 창이 뜨고 파일은 만들어지지 않는다")
	void unsupportedExtensionShowsErrorAndWritesNothing() {
		// Given
		File file = folder.resolve("pattern.txt").toFile();
		dialogs.nextFile = file;

		// When
		new StoreCommand(game, formats, dialogs).execute();

		// Then
		assertEquals(1, dialogs.errors.size());
		assertTrue(dialogs.errors.get(0).contains(".life, .rle"));
		assertFalse(file.exists());
	}

	@Test
	@Tag("original")
	@DisplayName("execute(): 저장하면 실행 중이던 게임이 멈춘다")
	void storeStopsGame() {
		// Given
		game.start(30);
		dialogs.nextFile = folder.resolve("pattern.life").toFile();

		// When
		new StoreCommand(game, formats, dialogs).execute();

		// Then
		assertFalse(ticker.running);
		assertTrue(dialogs.nextFile.exists());
	}
}

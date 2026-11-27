package com.holub.life.command;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import com.holub.life.format.LifeFormatAdapter;
import com.holub.life.format.PatternFormats;
import com.holub.life.format.RleFormat;
import com.holub.life.game.LifeGame;
import com.holub.life.model.Board;

/**
 * RLE 파일 Load 통합 테스트 (외부 파일 호환): Grid → Load로 다른 프로그램(LifeWiki, Golly 등)에서 받은 .rle 파일을 읽음
 * 진짜 LifeGame·Board에 가짜 파일 창(FakeDialogs)과 가짜 리스너(RecordingGameListener)를 넣어 화면 없이 실행함. 파일은 임시 폴더(@TempDir)에 씀
 *
 * [테스트 시나리오]
 *   1. 위치(#R)가 없는 파일: 보드 가운데에 놓음 (test/resources/glider.rle)
 *   2. 놓을 수 없는 파일 (#R 위치에서 보드를 벗어남, 보드보다 큼, B3/S23이 아닌 규칙): 오류 창 하나, 보드는 그대로, 화면 알림 없음
 *
 * [커버리지]
 *   - RleLoadCommandTest.java 테스트 대상: LoadCommand.execute와 RleFormat.read (위치 정하기, 놓을 수 없는 패턴)
 *   - 테스트 제외 대상
 *     - 이 앱이 Store한 .rle(#R 있음)을 같은 자리로 Load: GridCommandTest에서 확인
 *     - 깨진 .rle, 지원하지 않는 확장자: GridCommandTest에서 확인
 *     - RLE 문법 세부: RleFormatTest에서 확인
 *     - 실패한 Load가 Undo 기록에 남지 않는지: LifeGameUndoTest에서 확인
 */
@Tag("extension")
class RleLoadCommandTest{
	private static final int SIZE = LifeTestDriver.SIZE;

	@TempDir
	Path folder;

	private final Board board = new Board();
	private final LifeGame game = new LifeGame(board, new FakeTicker());
	private final FakeDialogs dialogs = new FakeDialogs();
	private final PatternFormats formats = new PatternFormats(List.of(new LifeFormatAdapter(SIZE), new RleFormat(SIZE)));
	private final RecordingGameListener view = new RecordingGameListener();

	@Test
	@DisplayName("execute(): 위치(#R)가 없는 .rle 파일은 보드 가운데에 놓는다 (64칸에 3칸 패턴이면 왼쪽 위가 30,30)")
	void externalRleWithoutPositionIsCentered(){
		// Given
		dialogs.nextFile = new File("test/resources/glider.rle");

		// When
		new LoadCommand(game, formats, dialogs).execute();

		// Then
		assertEquals(Cells.fromRowColumn("30,31", "31,32", "32,30", "32,31", "32,32"), board.liveCells());
		assertEquals(List.of(), dialogs.errors);
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
			보드를 벗어나는 #R 위치 | #R 62 62\\nx = 3, y = 3\\nbo$2bo$3o!
			보드보다 큰 패턴        | x = 65, y = 1\\n65o!
			B3/S23이 아닌 규칙      | x = 3, y = 1, rule = B36/S23\\n3o!
			""")
	@DisplayName("execute(): 보드에 놓을 수 없는 .rle 파일은 오류 창을 하나 띄우고, 보드는 그대로이고, 화면에 알리지 않는다")
	void unplaceableRleFailsAndKeepsBoard(String label, String content) throws IOException{
		// Given
		board.toggle(10, 10);
		game.addGameListener(view);
		Path file = folder.resolve("pattern.rle");
		Files.writeString(file, content.replace("\\n", "\n"), StandardCharsets.UTF_8);
		dialogs.nextFile = file.toFile();

		// When
		new LoadCommand(game, formats, dialogs).execute();

		// Then
		assertEquals(1, dialogs.errors.size());
		assertEquals(Cells.fromRowColumn("10,10"), board.liveCells());
		assertEquals(List.of(), view.events);
	}
}

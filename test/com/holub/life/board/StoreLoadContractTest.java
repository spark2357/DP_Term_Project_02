package com.holub.life.board;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Store/Load Contract 테스트: Grid → Store, Grid → Load가 보드를 그대로 저장하고 되살리는지 검증
 * 추상 클래스이며, 하위 클래스가 newDriver()로 검증할 구현을 정함 (원본: OriginalStoreLoadTest)
 * 파일 대화상자 대신 바이트 배열 스트림으로 저장하고 읽음
 *
 *   사용하는 LifeTestDriver 메서드
 *   - click(행, 열), tick(), liveCells()
 *   - store(out) : 원본에서는 createMemento() → transfer(STORE) → flush()
 *   - load(in)   : 원본에서는 clear() → createMemento() → load() → transfer(LOAD)
 *
 * [테스트 시나리오]
 *   1. 왕복: Store한 것을 새 보드에 Load하면 같은 보드 (꼭짓점, 블록 경계, 빈 보드, 모든 칸이 산 보드)
 *   2. Store는 보드를 바꾸지 않음: Store 뒤에도 셀과 이후 진화가 같음
 *   3. Load는 덮어씀: 다른 셀이 있던 보드에 Load하면 파일 내용만 남음
 *   4. Load 후 진화: 블록 안쪽에 놓은 blinker는 Load 뒤에도 정상으로 진동
 *   5. 원본 파일 호환: 원본 코드로 저장한 test/resources/original-store.life를 읽으면 기대한 보드가 됨
 *
 * [커버리지]
 *   - StoreLoadContractTest.java 테스트 대상: 저장과 복원 결과, 복원 뒤의 세대 진행
 *   - 테스트 제외 대상
 *     - 원본의 결함(중복 저장, Load 실패 시 보드가 지워짐, 블록 가장자리 패턴 Load): StoreLoadDefectContractTest
 *     - 원본 저장 파일의 내부 모양(중복, 보드 밖 좌표): StoreLoadOriginalBehaviorTest
 *     - 파일 대화상자와 Load/Store 때 시계 정지: 원본은 Universe에 묶여 있어 보드 단위 테스트에서 제외
 */
abstract class StoreLoadContractTest {
	// 원본 코드의 Store 순서로 저장한 파일. 아래 ORIGINAL_FILE_CELLS가 들어 있음 (원본 결함 때문에 셀마다 두 번씩)
	private static final String ORIGINAL_FILE = "test/resources/original-store.life";
	private static final Set<Point> ORIGINAL_FILE_CELLS = Cells.fromRowColumn(
			"1,2", "2,3", "3,1", "3,2", "3,3",		// glider
			"7,7", "7,8", "8,7", "8,8",				// 네 블록이 만나는 곳의 block
			"0,63", "63,0", "63,63");				// 보드 꼭짓점

	abstract LifeTestDriver newDriver();

	@Test
	@DisplayName("store(), load(): Store한 보드를 새 보드에 Load하면 같은 보드가 된다")
	void storeThenLoadGivesSameBoard() throws IOException {
		Set<Point> edgesAndCorners = Cells.fromRowColumn("0,0", "0,63", "63,0", "63,63", "7,7", "7,8", "8,7", "8,8", "31,40");
		Set<Point> empty = Cells.fromRowColumn();
		Set<Point> full = new HashSet<>();
		for (int row = 0; row < LifeTestDriver.SIZE; row++) {
			for (int column = 0; column < LifeTestDriver.SIZE; column++) {
				full.add(new Point(column, row));
			}
		}

		Set<Point>[] boards = new Set[] { edgesAndCorners, empty, full };
		String[] names = { "꼭짓점과 블록 경계", "빈 보드", "모든 칸이 산 보드" };

		for (int i = 0; i < boards.length; i++) {
			// Given
			LifeTestDriver source = newDriver();
			place(source, boards[i]);

			// When
			byte[] file = store(source);
			LifeTestDriver loaded = newDriver();
			loaded.load(new ByteArrayInputStream(file));

			// Then
			assertEquals(boards[i], loaded.liveCells(), names[i]);
		}
	}

	@Test
	@DisplayName("store(): Store해도 보드와 이후 진화가 바뀌지 않는다")
	void storeDoesNotChangeBoard() throws IOException {
		// Given: 같은 패턴을 놓은 보드 두 개. 한쪽만 Store함
		Set<Point> pattern = Cells.fromRowColumn("1,2", "2,3", "3,1", "3,2", "3,3", "20,7", "20,8", "20,9");
		LifeTestDriver stored = newDriver();
		LifeTestDriver notStored = newDriver();
		place(stored, pattern);
		place(notStored, pattern);

		// When
		store(stored);

		// Then: 셀이 그대로이고, 이후 세대도 Store하지 않은 보드와 같음
		assertEquals(pattern, stored.liveCells(), "Store 직후");
		for (int generation = 1; generation <= 10; generation++) {
			stored.tick();
			notStored.tick();
			assertEquals(notStored.liveCells(), stored.liveCells(), generation + "세대");
		}
	}

	@Test
	@DisplayName("load(): 다른 셀이 있던 보드에 Load하면 파일 내용만 남는다")
	void loadReplacesCurrentBoard() throws IOException {
		// Given: 파일에는 blinker, 보드에는 다른 셀들
		LifeTestDriver source = newDriver();
		place(source, Cells.fromRowColumn("3,2", "3,3", "3,4"));
		byte[] file = store(source);

		LifeTestDriver board = newDriver();
		place(board, Cells.fromRowColumn("40,40", "41,41", "50,10"));

		// When
		board.load(new ByteArrayInputStream(file));

		// Then
		assertEquals(Cells.fromRowColumn("3,2", "3,3", "3,4"), board.liveCells());
	}

	@Test
	@DisplayName("load(), tick(): 블록 안쪽에 놓은 blinker는 Load 뒤에도 정상으로 진동한다")
	void blinkerInsideBlockOscillatesAfterLoad() throws IOException {
		// Given: 블록 (0,0) 안쪽의 blinker를 저장했다가 새 보드에 Load
		Set<Point> horizontal = Cells.fromRowColumn("3,2", "3,3", "3,4");
		LifeTestDriver source = newDriver();
		place(source, horizontal);

		LifeTestDriver board = newDriver();
		board.load(new ByteArrayInputStream(store(source)));

		// When, Then
		board.tick();
		assertEquals(Cells.fromRowColumn("2,3", "3,3", "4,3"), board.liveCells(), "1세대");
		board.tick();
		assertEquals(horizontal, board.liveCells(), "2세대");
	}

	@Test
	@DisplayName("load(): 원본 코드로 저장한 .life 파일을 읽으면 기대한 보드가 된다")
	void loadsFileStoredByOriginal() throws IOException {
		// Given
		LifeTestDriver board = newDriver();

		// When
		InputStream file = new FileInputStream(ORIGINAL_FILE);
		board.load(file);
		file.close();

		// Then: 수정본도 기존 사용자의 .life 파일을 계속 읽을 수 있어야 함
		assertEquals(ORIGINAL_FILE_CELLS, board.liveCells());
	}

	// ---- 헬퍼 메서드

	private static void place(LifeTestDriver board, Set<Point> cells) {
		for (Point cell : cells) {
			board.click(cell.y, cell.x);
		}
	}

	private static byte[] store(LifeTestDriver board) throws IOException {
		ByteArrayOutputStream file = new ByteArrayOutputStream();
		board.store(file);
		return file.toByteArray();
	}
}

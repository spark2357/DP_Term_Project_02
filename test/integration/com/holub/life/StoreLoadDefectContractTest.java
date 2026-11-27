package com.holub.life;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Store/Load 결함 Contract 테스트: 원본이 지키지 못하는 올바른 동작을 적어 둠
 * 추상 클래스이며, 하위 클래스가 newDriver()로 검증할 구현을 정함
 * 원본용 하위 클래스(OriginalStoreLoadDefectTest)에는 known-defect 태그를 붙여 기본 실행에서 뺌.
 * 결함을 고친 현재 코드의 하위 클래스(BoardStoreLoadDefectTest)에는 improvement 태그를 붙여 기본 실행에 포함함
 *
 *   사용하는 LifeTestDriver 메서드
 *   - click(행, 열), tick(), liveCells(), store(out), load(in)
 *
 * [테스트 시나리오]
 *   1. 중복 저장: 저장 파일에 셀마다 한 번씩만 들어 있어야 함. 원본은 두 번씩 저장
 *      (Universe.doStore()가 이미 저장까지 하는 createMemento() 뒤에 transfer(STORE)를 한 번 더 부름)
 *   2. Load 실패: 깨진 파일을 Load하면 IOException이 나고 기존 보드가 남아 있어야 함. 원본은 읽기 전에 보드를 지움
 *      (Universe.doLoad()가 clear()를 memento.load()보다 먼저 부름)
 *   3. 블록 가장자리 패턴 Load: 가장자리 blinker를 Load해도 정상으로 진동해야 함. 원본은 첫 세대에 셀이 빠짐
 *      (Neighborhood.transfer(LOAD)가 이웃 블록을 깨우는 가장자리 표시를 켜지 않음)
 *   4. 모든 위치에서 Load: Load로 놓은 blinker·glider도 정답 계산 결과와 같아야 함 (3번을 모든 위치로 확장)
 *   5. .life 내용 검증: Collection이 아닌 객체를 직렬화한 파일은 IOException이어야 함. 원본은 ClassCastException
 *      (NeighborhoodState.load()가 읽은 객체를 확인 없이 Collection으로 캐스팅함. Universe.doLoad()는 IOException만 잡아 오류 창도 안 뜸)
 *   6. .life 내용 검증: Point가 아닌 원소가 든 파일은 IOException이어야 함. 원본은 오류 없이 읽고 그 원소는 무시함
 *      (NeighborhoodState.load()가 원소를 확인하지 않음)
 *
 * [커버리지]
 *   - StoreLoadDefectContractTest.java 테스트 대상: 원본 Store/Load 경로의 결함 네 가지 (중복 저장, Load 실패 때 보드 지움, 가장자리 Load, 내용 검증 없음)
 *   - 테스트 제외 대상
 *     - 파일 선택 취소 때 오류 창이 뜨는 결함: 원본은 파일 대화상자가 Universe에 묶여 있어 제외. 현재 코드는 LoadCommandTest, StoreCommandTest에서 확인
 *     - 보드 밖 좌표가 든 .life 파일: UC3 경우가 아니고 지금 코드도 무시해서 테스트하지 않음
 *     - 잘못된 .life 내용을 Memento 하나로 읽을 때: 현재 코드는 NeighborhoodStateTest에서 확인
 *   - 1번은 지금의 Java 직렬화 형식(Point 목록)을 가정함. 저장 형식을 바꾸면 확인 방법도 바꿔야 함
 *   - 같은 패턴을 클릭으로 놓으면 정상으로 진동한다는 대조군은 TickContractTest의 블록 경계 테스트가 맡음
 */
abstract class StoreLoadDefectContractTest {

	abstract LifeTestDriver newDriver();

	@Test
	@DisplayName("store(): 저장 파일에 셀마다 한 번씩만 들어 있어야 한다 (결함)")
	void storeWritesEachCellOnce() throws Exception {
		// Given: 셀 3개 (원본은 셀마다 두 번씩 저장함)
		LifeTestDriver board = newDriver();
		place(board, Cells.fromRowColumn("3,2", "3,3", "3,4"));

		// When
		byte[] file = store(board);

		// Then: 저장된 좌표 목록을 꺼내 보면 3개여야 함. 원본은 6개
		ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(file));
		Collection stored = (Collection) in.readObject();
		assertEquals(3, stored.size());
	}

	@Test
	@DisplayName("load(): 깨진 파일을 Load하면 실패하고 기존 보드는 그대로 남아야 한다 (결함)")
	void failedLoadKeepsCurrentBoard() {
		// Given: 셀이 있는 보드와 깨진 파일
		LifeTestDriver board = newDriver();
		Set<Point> before = Cells.fromRowColumn("10,10", "10,11", "11,10", "11,11");
		place(board, before);
		byte[] brokenFile = "this is not a life file".getBytes();

		// When, Then: IOException이 남
		assertThrows(IOException.class, () -> board.load(new ByteArrayInputStream(brokenFile)));

		// Then: 보드가 지워지지 않아야 함. 원본은 읽기 전에 clear()를 해서 빈 보드가 됨
		assertEquals(before, board.liveCells());
	}

	@Test
	@DisplayName("load(): 블록 아래쪽 가장자리의 blinker를 Load해도 정상으로 진동해야 한다 (결함)")
	void blinkerOnBottomBlockEdgeOscillatesAfterLoad() throws IOException {
		// Given: 블록 (0,0)의 아래쪽 가장자리(7행). 1세대에 태어날 (8,3)은 아래 블록 (1,0)에 있음
		LifeTestDriver board = loadedBoard(Cells.fromRowColumn("7,2", "7,3", "7,4"));

		// When
		board.tick();

		// Then: 원본은 아래 블록이 첫 세대에 계산되지 않아 (8,3)이 빠짐
		assertEquals(Cells.fromRowColumn("6,3", "7,3", "8,3"), board.liveCells());
	}

	@Test
	@DisplayName("load(): 블록 오른쪽 가장자리의 blinker를 Load해도 정상으로 진동해야 한다 (결함)")
	void blinkerOnRightBlockEdgeOscillatesAfterLoad() throws IOException {
		// Given: 블록 (0,0)의 오른쪽 가장자리(7열). 1세대에 태어날 (3,8)은 오른쪽 블록 (0,1)에 있음
		LifeTestDriver board = loadedBoard(Cells.fromRowColumn("2,7", "3,7", "4,7"));

		// When
		board.tick();

		// Then: 원본은 오른쪽 블록이 첫 세대에 계산되지 않아 (3,8)이 빠짐
		assertEquals(Cells.fromRowColumn("3,6", "3,7", "3,8"), board.liveCells());
	}

	@Test
	@DisplayName("load(), tick(): Load로 보드의 모든 위치에 놓은 blinker와 glider도 정답 계산 결과와 같아야 한다 (결함)")
	void loadedPatternsAtEveryPositionMatchExpected() throws IOException {
		// Given: 원본은 블록 가장자리에 걸친 패턴이 첫 세대에 셀을 잃음
		Set<Point> blinker = Cells.fromRowColumn("0,0", "0,1", "0,2");
		Set<Point> glider = Cells.fromRowColumn("0,1", "1,2", "2,0", "2,1", "2,2");

		// 8칸 간격으로 64개씩 놓고 시작 위치를 0~7칸씩 옮기며 반복 → 64x64의 모든 위치를 덮음
		for (int startRow = 0; startRow < 8; startRow++) {
			for (int startColumn = 0; startColumn < 8; startColumn++) {
				String where = "시작 위치 (" + startRow + "," + startColumn + ")부터 8칸 간격";
				assertLoadedMatchesExpected(Cells.repeatEvery8(blinker, startRow, startColumn), "blinker " + where);
				assertLoadedMatchesExpected(Cells.repeatEvery8(glider, startRow, startColumn), "glider " + where);
			}
		}
	}

	@Test
	@DisplayName("load(): Collection이 아닌 객체를 직렬화한 파일은 IOException으로 실패해야 한다 (결함)")
	void fileWithNonCollectionObjectThrowsIOException() throws IOException{
		// Given: 산 셀 목록 자리에 String을 직렬화한 파일. 원본은 캐스팅에서 ClassCastException이 남
		LifeTestDriver board = newDriver();
		byte[] wrongFile = serialize("not a collection");

		// When, Then
		assertThrows(IOException.class, () -> board.load(new ByteArrayInputStream(wrongFile)));
	}

	@Test
	@DisplayName("load(): Point가 아닌 원소가 든 파일은 IOException으로 실패해야 한다 (결함)")
	void fileWithNonPointElementThrowsIOException() throws IOException{
		// Given: Point 하나와 String 하나가 섞인 목록을 직렬화한 파일. 원본은 오류 없이 읽고 String은 무시함
		LifeTestDriver board = newDriver();
		List<Object> mixed = new ArrayList<>();
		mixed.add(new Point(3, 3));
		mixed.add("(4,4)");
		byte[] wrongFile = serialize(mixed);

		// When, Then
		assertThrows(IOException.class, () -> board.load(new ByteArrayInputStream(wrongFile)));
	}

	// ---- 헬퍼 메서드

	// .life 파일과 같은 방식(ObjectOutputStream)으로 객체 하나를 직렬화
	private static byte[] serialize(Object object) throws IOException{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
			out.writeObject(object);
		}
		return bytes.toByteArray();
	}

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

	// 다른 보드에 cells를 클릭으로 놓고 Store한 뒤, 새 보드에 Load해서 돌려줌
	private LifeTestDriver loadedBoard(Set<Point> cells) throws IOException {
		LifeTestDriver source = newDriver();
		place(source, cells);

		LifeTestDriver board = newDriver();
		board.load(new ByteArrayInputStream(store(source)));
		return board;
	}

	// start를 Load로 놓고 4세대 동안 세대마다 정답 계산 결과와 같은지 확인
	private void assertLoadedMatchesExpected(Set<Point> start, String label) throws IOException {
		LifeTestDriver board = loadedBoard(start);

		Set<Point> expected = start;
		for (int generation = 1; generation <= 4; generation++) {
			board.tick();
			expected = ExpectedGenerationCalculator.nextGeneration(expected);
			assertEquals(expected, board.liveCells(), label + ": " + generation + "세대");
		}
	}
}

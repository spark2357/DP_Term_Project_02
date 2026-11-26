package com.holub.life.board;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.LinkedList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Store/Load 원본 동작 테스트: 원본 저장 파일의 모양과 Load의 검증 없는 동작을 그대로 기록
 * 원본 코드(OriginalDriver)에서만 실행함. 동작을 고치면 기대값을 바꾸거나 테스트 삭제
 *
 *   사용하는 LifeTestDriver 메서드
 *   - click(행, 열), liveCells(), store(out), load(in)
 *
 * [테스트 시나리오]
 *   1. 중복 저장: 저장 파일에 모든 셀이 두 번씩 들어 있음
 *   2. 보드 밖 좌표: 파일에 보드 밖 좌표가 있어도 오류 없이 무시되고 보드 안 좌표만 Load됨
 *
 * [커버리지]
 *   - StoreLoadOriginalBehaviorTest.java 테스트 대상: 원본 저장 파일의 내용, 잘못된 좌표가 든 파일의 Load
 *   - 테스트 제외 대상
 *     - Point가 아닌 원소가 든 파일: NeighborhoodStateTest에서 확인
 */
class StoreLoadOriginalBehaviorTest {

	@Test
	@DisplayName("store() (원본 동작): 저장 파일에 모든 셀이 두 번씩 들어 있다. Universe.doStore()에서 중복 저장 발생")
	void storeWritesEachCellTwice() throws Exception {
		// Given: 셀 3개
		LifeTestDriver board = new OriginalDriver();
		board.click(3, 2);
		board.click(3, 3);
		board.click(3, 4);

		// When
		ByteArrayOutputStream file = new ByteArrayOutputStream();
		board.store(file);

		// Then: 좌표 목록이 6개이고, 셀마다 두 번씩 들어 있음
		ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(file.toByteArray()));
		Collection stored = (Collection) in.readObject();
		assertEquals(6, stored.size());
		for (Point cell : Cells.fromRowColumn("3,2", "3,3", "3,4")) {
			int count = 0;
			for (Object point : stored) {
				if (cell.equals(point)) {
					count++;
				}
			}
			assertEquals(2, count, "셀 " + cell);
		}
	}

	@Test
	@DisplayName("load() (원본 동작): 파일에 보드 밖 좌표가 있어도 오류 없이 무시된다. load()에서 좌표를 검증하지 않음")
	void loadIgnoresCellsOutsideBoard() throws IOException {
		// Given: 보드 안 좌표 하나와 보드 밖 좌표 두개가 포함된 파일 (원본 파일과 같은 형식: Point의 LinkedList)
		LinkedList<Point> points = new LinkedList<>();
		points.add(new Point(3, 3));		// 보드 안 (3행 3열)
		points.add(new Point(100, 100));	// 보드 밖
		points.add(new Point(-1, 5));		// 보드 밖
		byte[] file = serialize(points);

		LifeTestDriver board = new OriginalDriver();

		// When: 예외 없이 읽힘
		board.load(new ByteArrayInputStream(file));

		// Then: 보드 안 좌표만 살아 있음. 사용자는 파일 일부가 무시됐다는 것을 알 수 없음
		assertEquals(Cells.fromRowColumn("3,3"), board.liveCells());
	}

	// .life 파일과 같은 방식(ObjectOutputStream)으로 직렬화
	private static byte[] serialize(Object object) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bytes);
		out.writeObject(object);
		out.close();
		return bytes.toByteArray();
	}
}

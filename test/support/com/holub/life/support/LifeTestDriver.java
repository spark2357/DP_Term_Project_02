package com.holub.life.support;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * GUI 없이 보드의 게임 로직 조작 (테스트 전용 인터페이스)
 *
 *   구현
 *   - OriginalDriver : 원본 Neighborhood를 Universe와 같은 순서로 호출
 *   - (리팩터링 뒤) BoardDriver : 새로 만든 Board에 그대로 위임
 *
 *   메서드
 *   - tick()             : Go → Tick (Single Step). 한 세대 진행
 *   - click(행, 열)       : 마우스로 셀 하나를 클릭
 *   - clear()            : Grid → Clear
 *   - store(out)         : Grid → Store. 파일 대신 스트림에 씀
 *   - load(in)           : Grid → Load. 파일 대신 스트림에서 읽음
 *   - liveCells()        : 살아 있는 셀의 좌표(Point(x,y)) 집합. 테스트용 확인 메서드
 */
public interface LifeTestDriver {
	int SIZE = 64;

	void tick();

	void click(int row, int column);

	void clear();

	void store(OutputStream out) throws IOException;

	void load(InputStream in) throws IOException;

	Set<Point> liveCells();
}

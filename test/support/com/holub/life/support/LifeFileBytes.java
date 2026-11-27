package com.holub.life.support;

import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * .life 파일 내용을 바이트로 직접 만드는 헬퍼 (색 저장 테스트용)
 *
 *   .life 파일 = ObjectOutputStream 하나에 [산 셀 좌표 LinkedList] 다음 [색 표]를 차례로 씀
 *   - oldFormat(좌표...)          : 색 표가 없는 예전(원본) 파일
 *   - withColorPart(좌표, 색 표)   : 산 셀 목록 뒤에 아무 객체나 색 표 자리에 쓴 파일 (잘못된 색 표 만들기)
 *   - colorPartStart(좌표...)     : 색 표가 시작하는 바이트 위치. 같은 좌표로 만든 예전 파일의 길이와 같음
 *   - truncate(바이트, 길이)       : 앞에서부터 길이만큼만 남김
 */
public final class LifeFileBytes{
	private LifeFileBytes() {
	}

	public static byte[] oldFormat(Point... cells) throws IOException{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bytes);
		out.writeObject(liveCells(cells));
		out.flush();
		return bytes.toByteArray();
	}

	public static byte[] withColorPart(Point[] cells, Object colorPart) throws IOException{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bytes);
		out.writeObject(liveCells(cells));
		out.writeObject(colorPart);
		out.flush();
		return bytes.toByteArray();
	}

	// 색 표 앞부분(스트림 머리 + 산 셀 목록)은 예전 파일과 바이트가 같으므로, 예전 파일의 길이가 곧 색 표의 시작 위치
	public static int colorPartStart(Point... cells) throws IOException{
		return oldFormat(cells).length;
	}

	public static byte[] truncate(byte[] bytes, int length){
		return Arrays.copyOf(bytes, length);
	}

	// NeighborhoodState와 같은 컬렉션(LinkedList)에 같은 순서로 담음
	private static LinkedList<Point> liveCells(Point... cells){
		LinkedList<Point> list = new LinkedList<>();
		for (Point cell : cells) {
			list.add(new Point(cell));
		}
		return list;
	}
}

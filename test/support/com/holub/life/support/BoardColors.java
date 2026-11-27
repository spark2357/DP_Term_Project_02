package com.holub.life.support;

import com.holub.life.Board;
import com.holub.life.CellColor;
import com.holub.life.CellPainter;
import com.holub.life.CellState;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

/**
 * 보드의 산 셀 색을 좌표별로 모으는 테스트 헬퍼 (CellColorBoardTest의 ColorRecorder와 같은 방법)
 * Board에는 색을 읽는 공개 메서드가 없으므로, 셀 하나를 1픽셀로 보드 전체를 그리게 하고
 * 가짜 CellPainter가 받은 색을 모음. 모델이 화면에 넘기는 값 그대로를 확인함
 *
 *   - of(board) : 산 셀의 좌표(Point(x=열, y=행)) → 색. 죽은 셀은 들어 있지 않음
 */
public final class BoardColors{
	private BoardColors(){
	}

	public static Map<Point, CellColor> of(Board board){
		int width = board.widthInCells();
		Recorder recorder = new Recorder();
		board.redraw(recorder, new Rectangle(0, 0, width, width), true);
		return recorder.colors;
	}

	// 모든 셀을 같은 색으로 한 Map (예: RLE Load 뒤 기본색 확인)
	public static Map<Point, CellColor> allIn(CellColor color, Iterable<Point> cells){
		Map<Point, CellColor> colors = new HashMap<>();
		for (Point cell : cells){
			colors.put(new Point(cell), color);
		}
		return colors;
	}

	private static class Recorder implements CellPainter{
		final Map<Point, CellColor> colors = new HashMap<>();

		@Override
		public void paintCell(Rectangle here, boolean alive){
			throw new AssertionError("Resident는 상태를 넘기는 paintCell(here, state)를 불러야 함");
		}

		@Override
		public void paintCell(Rectangle here, CellState state){
			state.getColor().ifPresent(color -> colors.put(new Point(here.x, here.y), color));
		}

		@Override
		public void paintBorder(Rectangle here, boolean active){
		}
	}
}

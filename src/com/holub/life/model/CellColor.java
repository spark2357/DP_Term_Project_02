package com.holub.life.model;

// [수정] 2026-11-22: 셀 색 기능. 산 셀의 색 값. DEFAULT는 원본이 쓰던 산 셀 색(빨강)
public record CellColor( int red, int green, int blue ){
	public static final CellColor DEFAULT = new CellColor( 255, 0, 0 );

	public CellColor{
		if( !inRange(red) || !inRange(green) || !inRange(blue) )
			throw new IllegalArgumentException(
				"color components must be 0..255: " + red + ", " + green + ", " + blue );
	}

	private static boolean inRange( int component ){
		return 0 <= component && component <= 255;
	}
}

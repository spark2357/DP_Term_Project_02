package com.holub.life;

import java.util.List;

// [수정] 2026-11-22: 셀 색 기능. 부모 셋 색의 평균으로 태어남 (빨강, 초록, 파랑을 각각 평균)
// [수정] 2026-11-23: 부모 수를 3으로 고정하지 않음
public final class BlendRule implements BirthColorRule{
	public CellState newborn( List<CellColor> parents ){
		return CellState.alive( average( parents ) );
	}

	private static CellColor average( List<CellColor> parents ){
		if( parents.isEmpty() )
			throw new IllegalArgumentException( "no parents" );

		int red = 0, green = 0, blue = 0;
		for( CellColor color : parents ){
			red   += color.red();
			green += color.green();
			blue  += color.blue();
		}
		float count = parents.size();
		return new CellColor( Math.round( red / count ), Math.round( green / count ), Math.round( blue / count ) );
	}

	public String toString()	{ return "Blend"; }
}

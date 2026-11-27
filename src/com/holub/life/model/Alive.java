package com.holub.life.model;

import java.util.Objects;
import java.util.Optional;

// [수정] 2026-11-22: 셀 색 기능. 산 상태. 색 없이는 만들 수 없음.
// 같은 색으로 칠하면 죽고, 다른 색으로 칠하면 색만 바뀜
final class Alive implements CellState{
	private final CellColor color;

	Alive( CellColor color ){
		this.color = Objects.requireNonNull( color );
	}

	public boolean				isAlive()	{ return true;					}
	public Optional<CellColor>	getColor()		{ return Optional.of( color );	}

	// [수정] 2026-11-22: 산 셀을 칠할 때. 같은 색으로 다시 칠하면 셀을 끔(원래 클릭의 토글처럼),
	// 다른 색이면 죽이지 않고 그 색으로 바꿈
	public CellState brushedWith( CellColor newColor ){
		if( newColor.equals( color ) )
			return CellState.DEAD;
		return CellState.alive( newColor );
	}

	public String toString()	{ return "Alive(" + color + ")"; }
}

package com.holub.life.model;

import java.util.Optional;

// [수정] 2026-11-22: 셀 색 기능. 죽은 상태. 색이 없음 (CellState.DEAD 하나만 씀)
final class Dead implements CellState{
	Dead(){}

	public boolean				isAlive()	{ return false;				}
	public Optional<CellColor>	getColor()		{ return Optional.empty();	}

	public CellState brushedWith( CellColor color ){
		return CellState.alive( color );
	}

	public String toString()	{ return "Dead"; }
}

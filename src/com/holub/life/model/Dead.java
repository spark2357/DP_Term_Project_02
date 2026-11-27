package com.holub.life.model;

import java.util.Optional;

// [수정] 2026-11-22: 셀 색 기능. 죽은 상태. 색이 없음 (CellState.DEAD 하나만 씀)
// [수정] 2026-11-27: 하나뿐인 죽은 상태를 CellState.DEAD 대신 Dead.INSTANCE로 둠. 생성자를 막아 다른 곳에서 만들 수 없음
final class Dead implements CellState{
	static final Dead INSTANCE = new Dead();

	private Dead(){}

	public boolean				isAlive()	{ return false;				}
	public Optional<CellColor>	getColor()		{ return Optional.empty();	}

	public CellState brushedWith( CellColor color ){
		return new Alive( color );
	}

	public String toString()	{ return "Dead"; }
}

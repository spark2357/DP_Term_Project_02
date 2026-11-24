package com.holub.life;

import java.util.Optional;

// [수정] 2026-11-22: 셀 색 기능. 셀 상태(State 패턴의 State). 살았나와 색을 한 객체로 묶어 둘이 어긋날 수 없게 함.
// 상태는 바뀌지 않고, 클릭/칠하기는 다음 상태를 돌려줌
// [수정] 2026-11-23: sealed로 구현을 Dead, Alive 둘로 막음
public sealed interface CellState permits Dead, Alive{
	CellState DEAD = new Dead();

	static CellState alive( CellColor color ){
		return new Alive( color );
	}

	boolean isAlive();

	Optional<CellColor> getColor();

	CellState brushedWith( CellColor color );
}

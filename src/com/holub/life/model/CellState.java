package com.holub.life.model;

import java.util.Optional;

// [수정] 2026-11-22: 셀 색 기능. 셀 상태(State 패턴의 State). 살았나와 색을 한 객체로 묶어 둘이 어긋날 수 없게 함.
// 상태는 바뀌지 않고, 클릭/칠하기는 다음 상태를 돌려줌
// [수정] 2026-11-27: DEAD와 alive()를 뺌. 인터페이스가 구현체(Dead, Alive)를 모르게 함. 만드는 쪽은 Dead.INSTANCE, new Alive(색)을 씀
public interface CellState{
	boolean isAlive();

	Optional<CellColor> getColor();

	CellState brushedWith( CellColor color );
}

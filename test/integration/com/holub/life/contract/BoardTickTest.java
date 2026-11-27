package com.holub.life.contract;

import com.holub.life.support.*;

/**
 * 현재 코드로 Tick Contract 테스트를 실행
 * 테스트 내용은 TickContractTest에 있고, 여기서는 검증할 구현(BoardDriver)만 정함
 */
class BoardTickTest extends TickContractTest {

	LifeTestDriver newDriver() {
		return new BoardDriver();
	}
}

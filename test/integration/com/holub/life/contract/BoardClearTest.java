package com.holub.life.contract;

import com.holub.life.support.*;

/**
 * 현재 코드로 Clear Contract 테스트를 실행
 * 테스트 내용은 ClearContractTest에 있고, 여기서는 검증할 구현(BoardDriver)만 정함
 */
class BoardClearTest extends ClearContractTest {

	LifeTestDriver newDriver() {
		return new BoardDriver();
	}
}

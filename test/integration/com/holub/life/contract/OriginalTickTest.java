package com.holub.life.contract;

import com.holub.life.support.*;

/**
 * 원본 코드로 Tick Contract 테스트를 실행
 * 테스트 내용은 TickContractTest에 있고, 여기서는 검증할 구현(OriginalDriver)만 정함
 */
class OriginalTickTest extends TickContractTest {

	LifeTestDriver newDriver() {
		return new OriginalDriver();
	}
}

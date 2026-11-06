package com.holub.life.board;

import com.holub.life.support.*;

/**
 * 원본 코드로 Tick 테스트를 실행
 * 테스트 내용은 TickContractTest
 * Universe 리팩터링 뒤에는 같은 방식으로 BoardDriver를 넘기는 하위 클래스 만들어서 진행
 */
class OriginalTickTest extends TickContractTest {

	LifeTestDriver newDriver() {
		return new OriginalDriver();
	}
}

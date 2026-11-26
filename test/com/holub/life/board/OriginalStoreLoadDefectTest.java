package com.holub.life.board;

import com.holub.life.support.*;

import org.junit.jupiter.api.Tag;

/**
 * 원본 코드로 Store/Load 결함 Contract 테스트를 실행
 * 원본에는 결함이 있어 모두 실패하는 것이 정상이므로, 클래스 전체에 known-defect 태그를 붙여 기본 실행에서 뺌
 */
@Tag("known-defect")
class OriginalStoreLoadDefectTest extends StoreLoadDefectContractTest {

	LifeTestDriver newDriver() {
		return new OriginalDriver();
	}
}

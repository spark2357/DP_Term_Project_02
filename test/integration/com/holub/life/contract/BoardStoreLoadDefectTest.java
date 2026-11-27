package com.holub.life.contract;

import com.holub.life.support.*;

import org.junit.jupiter.api.Tag;

/**
 * 현재 코드로 Store/Load 결함 Contract 테스트를 실행
 * 테스트 내용은 StoreLoadDefectContractTest에 있고, 여기서는 검증할 구현(BoardDriver)만 정함
 * 원본(OriginalStoreLoadDefectTest)은 모두 실패하는 결함을 현재 코드는 고쳤으므로 known-defect 태그 없이 기본 실행에 포함함
 */
@Tag("improvement")
class BoardStoreLoadDefectTest extends StoreLoadDefectContractTest{

	LifeTestDriver newDriver(){
		return new BoardDriver();
	}
}

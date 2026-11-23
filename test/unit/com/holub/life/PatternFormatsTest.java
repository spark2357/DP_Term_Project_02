package com.holub.life;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;
import java.util.Optional;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * PatternFormats 단위 테스트: 지원하는 파일 형식 목록과 확장자 처리
 *
 *   메서드
 *   - formatFor(file)  : 파일 이름의 확장자(대소문자 무시)로 형식을 고름. 없으면 빈 Optional
 *   - loadFilters()    : 모든 형식 필터 + 형식마다 하나씩
 *   - extensionList()  : ".life, .rle"
 *
 * [테스트 시나리오]
 *   1. 형식 고르기: 확장자별, 대소문자 무시, 지원하지 않는 이름
 *   2. 필터와 메시지: Life.main에 넣은 순서대로
 *
 * [커버리지]
 *   - PatternFormatsTest.java 테스트 대상: formatFor, loadFilters, extensionList
 *   - 테스트 제외 대상
 *     - storeFilters, filterFor: loadFilters가 그대로 씀
 *     - 필터를 실제 파일 창에 붙이는 것 (GUI)
 */
@Tag("extension")
class PatternFormatsTest {
	private static final int SIZE = LifeTestDriver.SIZE;

	private final PatternFormat life = new LifeFormatAdapter(SIZE);
	private final PatternFormat rle = new RleFormat(SIZE);
	private final PatternFormats formats = new PatternFormats(List.of(life, rle));

	//  1. 형식 고르기

	@Test
	@DisplayName("formatFor(): 확장자로 형식을 고르고, 대소문자는 무시한다")
	void picksFormatByExtension() {
		assertSame(life, formats.formatFor(new File("glider.life")).orElseThrow());
		assertSame(rle, formats.formatFor(new File("dir/glider.rle")).orElseThrow());
		assertSame(rle, formats.formatFor(new File("GLIDER.RLE")).orElseThrow());
	}

	@ParameterizedTest(name = "\"{0}\"")
	@ValueSource(strings = {"glider.txt", "glider", "glider.rle.txt"})
	@DisplayName("formatFor(): 지원하지 않는 확장자, 확장자 없음, 이름 중간에만 있는 확장자는 빈 Optional")
	void unsupportedNameGivesEmpty(String name) {
		assertEquals(Optional.empty(), formats.formatFor(new File(name)));
	}

	//  2. 필터와 메시지

	@Test
	@DisplayName("loadFilters(): 모든 형식을 함께 보여주는 필터가 맨 앞이고, 그 뒤로 형식마다 하나씩 온다")
	void loadFiltersStartWithAllFormats() {
		FileNameExtensionFilter[] filters = formats.loadFilters();

		assertEquals(3, filters.length);
		assertArrayEquals(new String[]{"life", "rle"}, filters[0].getExtensions());
		assertEquals("Pattern Files (*.life, *.rle)", filters[0].getDescription());
		assertEquals("Life Files (*.life)", filters[1].getDescription());
		assertEquals("RLE Pattern Files (*.rle)", filters[2].getDescription());
	}

	@Test
	@DisplayName("extensionList(): 넣은 순서대로 \".life, .rle\"")
	void extensionListInOrder() {
		assertEquals(".life, .rle", formats.extensionList());
	}
}

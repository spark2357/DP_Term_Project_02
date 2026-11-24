package com.holub.life;

import java.util.List;

// [수정] 2026-11-22: 셀 색 기능. 태어날 셀의 상태를 부모 셋의 색으로 정하는 규칙(Strategy).
// Board가 지금 규칙을 들고 있고, 툴바의 Clash / Blend로 바꿔 끼움
// [수정] 2026-11-23: 부모 수를 3으로 고정하지 않음(출생 규칙이 바뀌어도 쓸 수 있게)
public interface BirthColorRule{
	CellState newborn( List<CellColor> parents );
}

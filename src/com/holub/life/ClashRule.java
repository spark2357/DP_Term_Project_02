package com.holub.life;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// [수정] 2026-11-22: 셀 색 기능. 부모의 다수 색으로 태어남. 셋 다 다르면 태어나지 않음(DEAD)
// [수정] 2026-11-23: 부모가 몇 명이든 가장 많은 색이 하나뿐이면 그 색, 동점이면 태어나지 않음
public final class ClashRule implements BirthColorRule{
	public CellState newborn( List<CellColor> parents ){
		return majority( parents )
					.map( CellState::alive )
					.orElse( CellState.DEAD );
	}

	private static Optional<CellColor> majority( List<CellColor> parents ){
		Map<CellColor, Integer> counts = new HashMap<>();
		for( CellColor color : parents )
			counts.put( color, counts.getOrDefault( color, 0 ) + 1 );

		CellColor winner = null;
		int most = 0;
		boolean tie = false;
		for( Map.Entry<CellColor, Integer> entry : counts.entrySet() ){
			if( entry.getValue() > most ){
				winner = entry.getKey();
				most = entry.getValue();
				tie = false;
			}
			else if( entry.getValue() == most )
				tie = true;
		}

		if( winner == null || tie )
			return Optional.empty();
		return Optional.of( winner );
	}

	public String toString()	{ return "Clash"; }
}

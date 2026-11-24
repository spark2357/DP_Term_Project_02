package com.holub.life;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import com.holub.life.Cell;
import com.holub.life.Storable;
import com.holub.life.Direction;
import com.holub.life.Neighborhood;
import com.holub.life.Universe;

/*** ****************************************************************
 * The Resident class implements a single cell---a "resident" of a
 * block.
 *
 * @include /etc/license.txt
 */

public final class Resident implements Cell
{
	// [수정] 2026-11-22: 셀 색 기능. amAlive → state, willBeAlive → nextState (State 패턴의 Context).
	// 살았나와 색이 한 객체에 있어 어긋날 수 없음. 생사 규칙(B3/S23)은 그대로
	private CellState state		= CellState.DEAD;
	private CellState nextState	= CellState.DEAD;

	// [수정] 2026-11-22: 출생 색 규칙(Strategy)은 Board가 들고 있는 지금 규칙을 물어봄. create()가 같은 출처를 넘겨 줌
	private final Supplier<BirthColorRule> birthColorRule;

	// [수정] 2026-11-24: 기본 생성자를 없앰. 기본 출생 색 규칙은 Board(Context)만 정하고, Resident는 항상 받은 규칙을 씀
	public Resident( Supplier<BirthColorRule> birthColorRule ){
		this.birthColorRule = birthColorRule;
	}

	private boolean isStable(){return state.isAlive() == nextState.isAlive(); }

	/** figure the next state.
	 *  @return true if the cell is not stable (will change state on the
	 *  next transition().
	 */
	public boolean figureNextState(
							Cell north, 	Cell south,
							Cell east, 		Cell west,
							Cell northeast, Cell northwest,
							Cell southeast, Cell southwest )
	{
		verify( north, 		"north"		);
		verify( south, 		"south"		);
		verify( east, 		"east"		);
		verify( west, 		"west"		);
		verify( northeast,	"northeast"	);
		verify( northwest,	"northwest" );
		verify( southeast,	"southeast" );
		verify( southwest,	"southwest" );

		int neighbors = 0;

		if( north.	  isAlive()) ++neighbors;
		if( south.	  isAlive()) ++neighbors;
		if( east. 	  isAlive()) ++neighbors;
		if( west. 	  isAlive()) ++neighbors;
		if( northeast.isAlive()) ++neighbors;
		if( northwest.isAlive()) ++neighbors;
		if( southeast.isAlive()) ++neighbors;
		if( southwest.isAlive()) ++neighbors;

		boolean willBeAlive = (neighbors==3 || (state.isAlive() && neighbors==2));

		// [수정] 2026-11-22: 죽을 셀은 DEAD, 살아남는 셀은 지금 상태(색) 그대로,
		// 태어나는 셀의 상태는 산 이웃(부모) 셋의 색을 보고 규칙(BirthColorRule)이 정함. 규칙이 DEAD를 주면 태어나지 않음
		if( !willBeAlive )
			nextState = CellState.DEAD;
		else if( state.isAlive() )
			nextState = state;
		else
			nextState = newborn( north, south, east, west,
								 northeast, northwest, southeast, southwest );
		return !isStable();
	}

	private CellState newborn( Cell... neighbors ){
		List<CellColor> parents = new ArrayList<>();
		for( Cell neighbor : neighbors )
			if( neighbor.isAlive() )
				parents.add( neighbor.getColor().orElseThrow() );

		return birthColorRule.get().newborn( parents );
	}

	private void verify( Cell c, String direction )
	{	assert (c instanceof Resident) || (c == Cell.DUMMY)
				: "incorrect type for " + direction +  ": " +
				   c.getClass().getName();
	}

	/** This cell is monetary, so it's at every edge of itself. It's
	 *  an internal error for any position except for (0,0) to be
	 *  requsted since the width is 1.
	 */
	public Cell	edge(int row, int column)
	{	assert row==0 && column==0;
		return this;
	}

	// [수정] 2026-11-06: isStable()은 "안 바뀜"인데 그대로 반환해 블록 건너뛰기가 동작하지 않았음. 바뀌었을 때 true를 반환하도록 수정
	public boolean transition()
	{	boolean changed = !isStable();
		state = nextState;
		return changed;
	}

	public void redraw(CellPainter painter, Rectangle here, boolean drawAll)
	{	painter.paintCell( here, state );
	}

	// [수정] 2026-11-24: 원본 클릭은 기본색(빨강)으로 칠하기와 같음 (죽음 → 빨강으로 삶, 빨강 → 죽음, 다른 색 → 빨강으로 바뀜)
	public void userClicked(Point here, Rectangle surface)
	{	state = state.brushedWith( CellColor.DEFAULT );
	}

	// [수정] 2026-11-22: 칠하기. 다음 상태는 지금 상태가 정함 (빈칸 → 그 색으로 삶, 다른 색 → 색만 바뀜, 같은 색 → 죽음)
	public void userBrushed(Point here, Rectangle surface, CellColor color){
		state = state.brushedWith( color );
	}

	public Optional<CellColor> getColor()	{return state.getColor();		}

	public void	   clear()			{state = nextState = CellState.DEAD;	}
	public boolean isAlive()		{return state.isAlive();		}
	public Cell    create()			{return new Resident( birthColorRule );	}
	public int 	   widthInCells()	{return 1;}

	// [수정] 2026-11-16: Visitor에게 자기 자신(셀 하나)을 알려 줌 (Composite의 Leaf)
	public void accept( CellVisitor visitor )
	{	visitor.visitResident( this );
	}

	public Direction isDisruptiveTo()
	{	return isStable() ? Direction.NONE : Direction.ALL ;
	}

	public boolean transfer(Storable blob,Point upperLeft,boolean doLoad)
	{
		Memento memento = (Memento)blob;
		if( doLoad )
		{	// [수정] 2026-11-23: Load한 셀은 Memento에 저장된 색. 색이 없는 파일이면 기본색
			state = nextState = memento.isAlive(upperLeft)
								? CellState.alive( memento.colorAt(upperLeft) )
								: CellState.DEAD;
			if( state.isAlive() )
				return true;
		}
		else if( state.isAlive() )  			// store only live cells
			memento.markAsAlive( upperLeft, state.getColor().orElseThrow() );

		return false;
	}

	/** Mementos must be created by Neighborhood objects. Throw an
	 *  exception if anybody tries to do it here.
	 */
	public Storable createMemento()
	{	throw new UnsupportedOperationException(
					"May not create memento of a unitary cell");
	}
}

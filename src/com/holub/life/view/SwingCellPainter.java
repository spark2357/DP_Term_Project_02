package com.holub.life.view;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import com.holub.life.model.CellColor;
import com.holub.life.model.CellPainter;
import com.holub.life.model.CellState;
import com.holub.ui.Colors;	// Contains constants specifying various
							// colors not defined in java.awt.Color.

/***
 * Draws cells and blocks with Swing. All of the colors and shapes of
 * the game live here.
 * <p>
 * The Graphics passed to the constructor belongs to the caller, so it
 * is never disposed here. Each method works on its own copy.
 */

public final class SwingCellPainter implements CellPainter
{
	private static final Color BORDER_COLOR = Colors.DARK_YELLOW;
	private static final Color LIVE_COLOR 	= Color.RED;
	private static final Color DEAD_COLOR   = Colors.LIGHT_YELLOW;

	private final Graphics graphics;

	public SwingCellPainter( Graphics graphics )
	{	this.graphics = graphics;
	}

	public void paintCell( Rectangle here, boolean alive )
	{	paintCell( here, alive ? LIVE_COLOR : DEAD_COLOR );
	}

	// [수정] 2026-11-22: 셀 색 기능. 산 셀은 모델이 기억하는 색(CellColor)으로 칠함
	public void paintCell( Rectangle here, CellState state ){
		paintCell( here, state.getColor().map( SwingCellPainter::toAwt ).orElse( DEAD_COLOR ) );
	}

	private static Color toAwt( CellColor color ){
		return new Color( color.red(), color.green(), color.blue() );
	}

	private void paintCell( Rectangle here, Color fill )
	{	Graphics g = graphics.create();
		g.setColor( fill );
		g.fillRect(here.x+1, here.y+1, here.width-1, here.height-1);

		// Doesn't draw a line on the far right and bottom of the
		// grid, but that's life, so to speak. It's not worth the
		// code for the special case.

		g.setColor( BORDER_COLOR );
		g.drawLine( here.x, here.y, here.x, here.y + here.height );
		g.drawLine( here.x, here.y, here.x + here.width, here.y  );
		g.dispose();
	}

	public void paintBorder( Rectangle here, boolean active )
	{	Graphics g = graphics.create();
		g.setColor( Colors.LIGHT_ORANGE );
		g.drawRect( here.x, here.y, here.width, here.height );

		if( active )
		{	g.setColor( Color.BLUE );
			g.drawRect(	here.x+1,	  here.y+1,
						here.width-2, here.height-2 );
		}

		g.dispose();
	}
}

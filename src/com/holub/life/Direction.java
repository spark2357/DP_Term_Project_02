package com.holub.life;

/***
 *  The Direction class is used to indicate in just what way a
 *  particular Cell is unstable. If a subcell has just changed
 *  on the north side, for example, then you indicate the
 *  change by issuing:
 *	<PRE>
 * 	Direction isDisruptive = new Direction();
 * 	isDisruptive.add( Direction.NORTH );
 *  </PRE>
 *  Later on, when updating, you can check whether a cell
 *  is disruptive on a particular edge by issuing:
 *  <Pre>
 *  if( someCell.isDisruptiveTo().the( Direction.NORTH ) )
 *  	//...
 *  </PRE>
 *  Two constant directions are provided: Direction.NONE and
 *  Direction.ANY. These differ from a standard direction in that
 *  they cannot be modified. A call to {@link #add} results in
 *  an <code>UnsupportedOperationException</code> toss.
 * 
 * @include /etc/license.txt
 */

public class Direction
{	private int	map = BITS_NONE;

	private static final int BITS_NORTH     = 0x0001;
	private static final int BITS_SOUTH     = 0x0002;
	private static final int BITS_EAST      = 0x0004;
	private static final int BITS_WEST	   	= 0x0008;
	private static final int BITS_NORTHEAST = 0x0010;
	private static final int BITS_NORTHWEST = 0x0020;
	private static final int BITS_SOUTHEAST = 0x0040;
	private static final int BITS_SOUTHWEST = 0x0080;
	private static final int BITS_ALL	   	= 0x00ff;
	private static final int BITS_NONE      = 0x0000;

	// Various directions. Note that, since we're talking
	// about the edges of a grid, that NORTH | WEST and NORTHWEST are
	// different things. NORTH means that anything along the NORTH
	// edge is active, ditto for WEST and the west edge. NORTHWEST
	// means that the cell in the NORTHWEST corner is active.
	// If the NORTHWEST corner is active, the NORTH and WEST
	// edges will also be active, but the converse is not true.

	public static final Direction NORTH 	= new Immutable(BITS_NORTH);
	public static final Direction SOUTH 	= new Immutable(BITS_SOUTH);
	public static final Direction EAST 		= new Immutable(BITS_EAST);
	public static final Direction WEST 		= new Immutable(BITS_WEST);
	public static final Direction NORTHEAST = new Immutable(BITS_NORTHEAST);
	public static final Direction NORTHWEST = new Immutable(BITS_NORTHWEST);
	public static final Direction SOUTHEAST = new Immutable(BITS_SOUTHEAST);
	public static final Direction SOUTHWEST = new Immutable(BITS_SOUTHWEST);
	public static final Direction ALL  		= new Immutable(BITS_ALL);
	public static final Direction NONE		= new Immutable(BITS_NONE);

	public  Direction()				 { 				 }
	public  Direction( Direction d  ){  map = d.map; }
	private Direction( int bits     ){  map = bits;	 }
 
	public boolean 	equals( Direction d ){ return d.map == map; }
	public void 	clear (			 	){ map = BITS_NONE; 	}
	public void 	add   ( Direction d ){ map |= d.map;	 	}
	public boolean 	has   ( Direction d ){ return the(d);				}
	public boolean 	the   ( Direction d ){ return (map & d.map)==d.map; }

	private static final class Immutable extends Direction
	{
		private static final String message =
			"May not modify Direction constant (Direction.NORTH, etc.)";

		private Immutable(int bits){ super(bits); }

		public void clear()
		{	throw new UnsupportedOperationException(message);
		}

		public void add( Direction d )
		{	throw new UnsupportedOperationException(message);
		}
	}
}

package com.holub.life.model;	// [수정] 2026-11-27: 패키지 정리. com.holub.life에서 com.holub.life.model로 옮김
import java.io.*;

/***
 * All mementos created by the Cells are Storable.
 *
 * @include /etc/license.txt
 */

public interface Storable
{	void load ( InputStream in   ) throws IOException;
	void flush( OutputStream out ) throws IOException;
}

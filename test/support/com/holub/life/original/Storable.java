// [수정] 2026-11-27: 원본(3aae53e) 코드를 테스트에서 돌리려고 com.holub.life.original 패키지로 옮김. 나머지 코드와 주석은 원본 그대로
package com.holub.life.original;
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

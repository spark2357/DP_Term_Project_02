package com.holub.io;

import java.io.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.Optional;

/*** A utility that handles various File-related operations.
 *
 * @include /etc/license.txt
 */

public class Files
{
	// [수정] 2026-11-10: 확장자 하나만 받던 userSelected(startHere, extension, description, selectButtonText)를
	// 필터 여러 개를 받는 이 메서드로 바꿈. .life와 .rle를 함께 고를 수 있어야 하기 때문.
	// 또 취소를 예외(FileNotFoundException) 대신 빈 Optional로 돌려줌. 취소는 사용자의 정상적인 선택인데,
	// 예외로 알리면 부르는 쪽이 읽기 실패와 구별하지 못해 오류 창을 띄웠음

	/** @param startHere Name of initial directory in which to look.
	 *  @param selectButtonText Replaces the "Open" on the chooser button.
	 *  @param filters the kinds of files to offer.
	 *  @return the selected file, or an empty Optional if the user
	 *  		cancelled the dialog. Cancelling is a normal choice,
	 *  		not an error.
	 */
	public static Optional<File> userSelected( String startHere,
						String selectButtonText,
						FileNameExtensionFilter... filters )
	{	JFileChooser chooser = new JFileChooser(startHere);
		for( FileNameExtensionFilter filter : filters )
			chooser.addChoosableFileFilter( filter );
		if( filters.length > 0 )
			chooser.setFileFilter( filters[0] );

		int result = chooser.showDialog(null,selectButtonText);
		if(result != JFileChooser.APPROVE_OPTION)
			return Optional.empty();

		File selected = chooser.getSelectedFile();
		if( selected.getName().indexOf('.') < 0
				&& chooser.getFileFilter() instanceof FileNameExtensionFilter )
		{	FileNameExtensionFilter inUse =
							(FileNameExtensionFilter) chooser.getFileFilter();
			selected = new File( selected.getPath() + "." + inUse.getExtensions()[0] );
		}
		return Optional.of( selected );
	}

	/** A unit test class for the methods in this class. Run with
	 *  java com.holub.io.Files\$Test
	 */
	static class Test
	{	
		public static void main(String[] args)
		{	// [수정] 2026-11-10: userSelected가 필터를 받고 Optional을 돌려주도록 바뀜
			Optional<File> f=Files.userSelected(".","Select!",
							new FileNameExtensionFilter("Test File","test"));
			if( f.isPresent() )
				System.out.println( "Selected " + f.get().getName() );
			else
				System.out.println( "No file selected" );
			System.exit(0); // Required to stop AWT thread & shut down.
		}
	}
}

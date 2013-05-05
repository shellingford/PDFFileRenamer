package pdf.renamer;

import org.apache.pdfbox.util.TextPosition;

public class EmptyTextPosition extends TextPosition{
	
	public EmptyTextPosition(){
		super();
	}
	
	@Override
	public String getCharacter() {
		return " ";
	}

}

package pdf.renamer;

import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.util.TextPosition;

public class TextMatrix {

	private List<List<TextPosition>> matrix = new ArrayList<List<TextPosition>>();

	private List<Float> yCoordinates = new ArrayList<Float>();

	private List<Float> startXCoordinates = new ArrayList<Float>();

	private List<Float> endXCoordinates = new ArrayList<Float>();

	private float pageWidth;

	public TextMatrix(float pageWidth) {
		this.pageWidth = pageWidth;
	}

	public void addText(TextPosition text) {
		int index = yCoordinates.indexOf(text.getY());
		if (index == -1) {
			addNewCoordinates(text);
			index = yCoordinates.size() - 1;
		}
		addNewText(text, index);
	}

	private void addNewText(TextPosition text, int index) {
		addTextToMatrix(text, index);
		if (index >= endXCoordinates.size()) {
			endXCoordinates.add(text.getX());
		} else {
			endXCoordinates.set(index, text.getX());
		}
	}

	private void addTextToMatrix(TextPosition text, int index) {
		int listSize = matrix.get(index).size();
		if (listSize > 0) {
			TextPosition lastChar = matrix.get(index).get(listSize - 1);
			float calculatedPosition = lastChar.getWidth() + lastChar.getX();
			if (!closeEnoughX(calculatedPosition, text.getX())) {
				TextPosition emptyChar = new EmptyTextPosition();
				matrix.get(index).add(emptyChar);
			}
		}
		matrix.get(index).add(text);
	}

	private boolean closeEnoughX(float calculatedPosition, float actualPosition) {
		float diff = Math.abs(calculatedPosition - actualPosition);
		return diff < 4;
	}

	private void addNewCoordinates(TextPosition text) {
		matrix.add(new ArrayList<TextPosition>());
		yCoordinates.add(text.getY());
		startXCoordinates.add(text.getX());
	}

	public void print() {
		for (List<TextPosition> lista : matrix) {
			for (TextPosition txt : lista) {
				System.out.print(txt.getCharacter());
			}
			System.out.println();
		}
	}

	public List<String> toLines() {
		List<String> lines = new ArrayList<String>();

		for (List<TextPosition> lineChars : matrix) {
			StringBuilder line = new StringBuilder(lineChars.size());
			for (TextPosition text : lineChars) {
				line.append(text.getCharacter());
			}
			lines.add(line.toString());
		}

		return lines;
	}

	public boolean isTextCentered() {
		for (List<TextPosition> lineTexts : matrix) {
			float startX = 0, endX = 0;
			TextPosition text = lineTexts.get(0);
			startX = text.getX();
			endX = pageWidth - text.getX();
			if (lineTexts.size() > 1) {
				text = lineTexts.get(lineTexts.size() - 1);
				endX = pageWidth - text.getX();
			}

			float diff = Math.abs(startX - endX);
			if (diff > 100) {
				return false;
			}
		}
		return true;
	}
}

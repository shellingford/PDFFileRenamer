package pdf.renamer;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.TextPosition;

public class PdfRenamer extends PDFTextStripper {
	private float currentPageWidth = 0;
	private static Map<Float, TextMatrix> matrices = new TreeMap<Float, TextMatrix>(Collections.reverseOrder());
	private String separator = "_";

	public PdfRenamer() throws IOException {
		super.setSortByPosition(true);
	}

	public static void main(String[] args) throws IOException {
		PdfRenamer renamer = new PdfRenamer();
		renamer.startRenaming(new File(args[0]));
	}

	public void startRenaming(File file) {
		if (file.isFile()) {
			try {
				renameFile(file);
			} catch (Exception e) {
				System.out.println(file.getName() + " - something went wrong with this file...");
			}
		} else if (file.isDirectory()) {
			renameDir(file);
		} else {
			System.out.println("Specified path is not a file or directory.");
		}
	}

	private void renameDir(File dir) {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				renameDir(file);
			} else if (file.isFile()) {
				try {
					renameFile(file);
				} catch (Exception e) {
					System.out.println(file.getName() + " - something went wrong with this file...");
				}
			}
		}

	}

	private void initialize() {
		matrices = new TreeMap<Float, TextMatrix>(Collections.reverseOrder());
	}

	private void renameFile(File file) throws IOException {
		initialize();
		int extensionIndex = file.getName().lastIndexOf('.');
		if (!file.getName().substring(extensionIndex + 1).equalsIgnoreCase("pdf")) {
			return;
		}

		String oldFileName = file.getName();
		PDDocument pdf = null;
		try {
			try {
				pdf = PDDocument.loadNonSeq(file, null);
			} catch (IOException e) {
				pdf = PDDocument.load(file);
			}

			PDDocumentCatalog docCatalog = pdf.getDocumentCatalog();
			if (docCatalog.getAllPages().size() <= 0) {
				System.out.println(file.getName() + " is empty pdf...");
				return;
			}
			PDPage page = (PDPage) docCatalog.getAllPages().get(0);
			currentPageWidth = page.findMediaBox().getWidth();
			PDStream contents = page.getContents();
			if (contents != null) {
				processStream(page, page.findResources(), page.getContents().getStream());

				List<String> lines = null;
				int counter = 0;
				for(Entry<Float, TextMatrix> entry : matrices.entrySet()) {
					counter++;
					lines = entry.getValue().toLines();

					if(counter >= 3 || counter == matrices.keySet().size()){
						//only check 2 different font sizes after just give up
						lines = null;
						break;
					}
					if(lines.size() <= 0 || 
							lines.size() > 5 || //if 'title' has over 5 rows, it's probably wrong
							lines.size() == 1 && lines.get(0).length() < 3){
						continue;
					}
					break;
				}
				
				if(lines == null){
					System.out.println(oldFileName + " couldn't find a title on the first page\n");
					return;
				}
				
				StringBuilder newFileName = new StringBuilder();
				for (String line : lines) {
					if (newFileName.length() > 0) {
						newFileName.append(" ").append(line);
					} else {
						newFileName.append(line);
					}
				}
				
				if(newFileName.length() <= 0){
					System.out.println(oldFileName + " couldn't find a title on the first page\n");
					return;
				}

				String fileName = removeExtraSpaces(newFileName.toString());
				if (!separator.equals(" ")) {
					fileName = fileName.trim()
									   .replaceAll("[\\\\/:?\"<>|\\]\\*]", "") //remove special characters from file name
									   .replaceAll(" ", separator) + ".pdf";
				}

				String fileNameWithPath = file.getParentFile().getAbsolutePath() + "\\" + fileName;
				if(!oldFileName.equalsIgnoreCase(fileName)) {
					// try to rename the file 5x
					for (int i = 0; i < 5; i++) {
						if (!file.renameTo(new File(fileNameWithPath))) {
							if (i == 4) {
								System.out.println(oldFileName + " not able to rename to "+ fileName +"\n");
							} else {
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									System.out.println(e);
								}
							}
						} else {
							System.out.println(oldFileName + " renamed to: " + fileName + "\n");
							break;
						}
					}
				}
				else {
					System.out.println(oldFileName + " no need to rename\n");
				}
			}

		} finally {
			if (pdf != null) pdf.close();
		}
	}

	private String removeExtraSpaces(String s) {
		while (s.contains("  ")) {
			s = s.replaceAll("  ", " ");
		}
		return s;
	}

	@Override
	protected void processTextPosition(TextPosition text) {
		if(!matrices.containsKey(text.getFontSizeInPt())){
			matrices.put(text.getFontSizeInPt(), new TextMatrix(currentPageWidth));
			matrices.get(text.getFontSizeInPt()).addText(text);
		}
		else {
			matrices.get(text.getFontSizeInPt()).addText(text);
		}
	}

}

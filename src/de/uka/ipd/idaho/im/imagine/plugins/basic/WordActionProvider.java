/*
 * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Universitaet Karlsruhe (TH) / KIT nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITAET KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.uka.ipd.idaho.im.imagine.plugins.basic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImFont;
import de.uka.ipd.idaho.im.ImLayoutObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.analysis.Imaging;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ClickActionProvider;
import de.uka.ipd.idaho.im.ocr.OcrEngine;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ClickSelectionAction;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.DisplayOverlay;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.im.util.SymbolTable;

/**
 * Provider of basic word edits, mostly for OCRed documents.
 * 
 * @author sautter
 */
public class WordActionProvider extends AbstractSelectionActionProvider implements ClickActionProvider {
	
	/** zero-argument constructor for class loading */
	public WordActionProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "IM Word Actions";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, final ImPage page, final ImDocumentMarkupPanel idmp) {
		
		//	this makes sense only in scanned documents, and only if text strings are visible
		if (idmp.documentBornDigital)
			return null;
		if (!idmp.areTextStringsPainted())
			return null;
		
		//	mark selection
		final BoundingBox selectedBox = new BoundingBox(Math.min(start.x, end.x), Math.max(start.x, end.x), Math.min(start.y, end.y), Math.max(start.y, end.y));
		
		//	get selected words
		final ImWord[] selectedWords = page.getWordsInside(selectedBox);
		
		//	get selection actions
		LinkedList actions = new LinkedList();
		
		//	offer removing words
		if (selectedWords.length != 0)
			actions.add(new SelectionAction("wordsRemove", "Remove Words", "Remove selected words.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					
					//	remove words
					ImUtils.makeStream(selectedWords, ImWord.TEXT_STREAM_TYPE_ARTIFACT, null);
					ImUtils.orderStream(selectedWords, ImUtils.leftRightTopDownOrder);
					for (ImWord imw = selectedWords[selectedWords.length-1]; imw != null; imw = imw.getPreviousWord())
						imw.setNextWord(null);
					for (int w = 0; w < selectedWords.length; w++)
						page.removeWord(selectedWords[w], true);
					
					//	indicate change
					return true;
				}
			});
		
		//	offer marking word ('in the wild' or over existing ones) if dimensions make sense
		if (selectedBox.getHeight() <= page.getImageDPI()) {
			if (selectedWords.length == 0)
				actions.add(new SelectionAction("wordsMark", "Mark Word", "Mark selected area as a word.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return markWord(page, null, selectedBox, idmp);
					}
				});
			else actions.add(new SelectionAction("wordsMark", "Mark Word", "Mark selected area as a word.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return markWord(page, selectedWords, selectedBox, idmp);
//					
//					//	get merged word, and offer editing
//					ImWord mWord = getMergedWord(selectedWords, page, idmp.document);
//					if (!idmp.editWord(mWord, true))
//						return false;
//					
//					//	get tokenizer to check and split words with multiple tokens
//					Tokenizer tokenizer = ((Tokenizer) idmp.document.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER));
//					ImWord[] mWords = tokenizeWord(mWord, tokenizer);
//					
//					//	remove words
//					ImUtils.orderStream(selectedWords, ImUtils.leftRightTopDownOrder);
//					Arrays.sort(selectedWords, ImUtils.textStreamOrder);
//					
//					//	remember start predecessor and end successor
//					ImWord startPrev = selectedWords[0].getPreviousWord();
//					ImWord endNext = selectedWords[selectedWords.length-1].getNextWord();
//					
//					//	remember any annotations starting or ending at any of the to-remove words
//					ArrayList startingAnnots = new ArrayList(3);
//					ArrayList endingAnnots = new ArrayList(3);
//					for (int w = 0; w < selectedWords.length; w++) {
//						ImAnnotation[] imwAnnots = idmp.document.getAnnotations(selectedWords[w]);
//						for (int a = 0; a < imwAnnots.length; a++) {
//							if (imwAnnots[a].getFirstWord() == selectedWords[w])
//								startingAnnots.add(imwAnnots[a]);
//							if (imwAnnots[a].getLastWord() == selectedWords[w])
//								endingAnnots.add(imwAnnots[a]);
//						}
//					}
//					
//					//	connect previous and next words to preserve annotations
//					if (startPrev != null)
//						startPrev.setNextWord(endNext);
//					else if (endNext != null)
//						endNext.setPreviousWord(startPrev);
//					
//					//	cut merged words out of streams
//					selectedWords[selectedWords.length-1].setNextWord(null);
//					selectedWords[0].setPreviousWord(null);
//					for (int w = 0; w < selectedWords.length; w++)
//						selectedWords[0].setNextWord(null);
//					
//					//	integrate merged word in document
//					for (int w = 0; w < mWords.length; w++) {
//						page.addWord(mWords[w]);
//						if (w == 0)
//							mWords[w].setPreviousWord(startPrev);
//						else mWords[w].setPreviousWord(mWords[w-1]);
//					}
//					mWords[mWords.length-1].setNextWord(endNext);
//					for (int a = 0; a < startingAnnots.size(); a++)
//						((ImAnnotation) startingAnnots.get(a)).setFirstWord(mWords[0]);
//					for (int a = 0; a < endingAnnots.size(); a++)
//						((ImAnnotation) endingAnnots.get(a)).setLastWord(mWords[mWords.length-1]);
//					
//					//	clean up merged words
//					for (int w = 0; w < selectedWords.length; w++)
//						page.removeWord(selectedWords[w], true);
//					
//					//	we _did_ change something, quite something
//					return true;
				}
			});
		}
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ClickActionProvider#getActions(de.uka.ipd.idaho.im.ImPage, java.awt.Point, int, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public ClickSelectionAction[] getActions(ImPage page, Point point, int clickCount, ImDocumentMarkupPanel idmp) {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(final ImWord start, final ImWord end, final ImDocumentMarkupPanel idmp) {
		
		//	this is extremely unlikely to be a word-aimed selection
		if (start.pageId != end.pageId)
			return null;
		if (!start.getTextStreamId().equals(end.getTextStreamId()))
			return null;
		if ((start.bounds.top >= end.centerY) || (end.centerY >= start.bounds.bottom))
			return null;
		if (end.getTextStreamPos() > (start.getTextStreamPos() + 20))
			return null;
		
		//	get selection actions
		LinkedList actions = new LinkedList();
		
		//	single word selected
		if (start == end) {
			
			//	offer editing word strings
			if (!idmp.documentBornDigital && idmp.areTextStringsPainted())
				actions.add(new SelectionAction("wordsEdit", "Edit Line Words", "Edit the OCR strings of the words in the selected line") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						editLineWords(start, idmp);
						return false;
					}
				});
			
			//	offer splitting word
			actions.add(new SelectionAction("wordsSplit", "Split Word", "Split selected word.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					
					//	tokenize word without inner punctuation as a starting point
					TokenSequence wordTokens = Gamta.NO_INNER_PUNCTUATION_TOKENIZER.tokenize(start.getString());
					
					//	create user prompt
					StringBuffer editStringBuilder = new StringBuffer();
					for (int t = 0; t < wordTokens.size(); t++) {
						if (t != 0)
							editStringBuilder.append(" ");
						editStringBuilder.append(wordTokens.valueAt(t));
					}
					JPanel wordSplitPanel = new JPanel(new BorderLayout(), true);
					wordSplitPanel.add(new JLabel("<HTML>Split the word with spaces in the field below.<BR>Do not add or remove any characters but spaces.</HTM>"), BorderLayout.CENTER);
					JTextField wordSplitField = new JTextField(editStringBuilder.toString());
					wordSplitField.setFont(new Font("Monospaced", Font.PLAIN, 12));
					wordSplitPanel.add(wordSplitField, BorderLayout.SOUTH);
					
					//	prompt user
					int choice = DialogFactory.confirm(wordSplitPanel, "Split Word", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
					if (choice != JOptionPane.OK_OPTION)
						return false;
					
					//	check consistency
					if (!wordSplitField.getText().trim().replaceAll("\\s", "").equals(start.getString())) {
						DialogFactory.alert(new JLabel("<HTML>'" + AnnotationUtils.escapeForXml(start.getString()) + "' cannot be split this way.<BR>All characters have to be preserved.<BR>Only insert spaces to mark the splitting points.</HTM>"), "Cannot Split Word", JOptionPane.ERROR_MESSAGE);
						return false;
					}
					
					//	any splits present?
					String[] splitWordParts = wordSplitField.getText().trim().split("\\s+");
					if (splitWordParts.length < 2)
						return false;
					
					//	remember predecessor and successor
					ImWord prevWord = start.getPreviousWord();
					ImWord nextWord = start.getNextWord();
					
					//	remember annotations starting or ending at to-split word
					ImAnnotation[] wordAnnots = idmp.document.getAnnotations(start);
					
					//	get character codes (convert HEX storage notation)
					System.out.println("Splitting word " + start.getString());
					String startCharCodesHex = ((String) start.getAttribute(ImFont.CHARACTER_CODE_STRING_ATTRIBUTE));
					String startCharCodes = null;
					if (startCharCodesHex != null) {
						startCharCodes = "";
						for (int c = 0; c < startCharCodesHex.length(); c += 2)
							startCharCodes = (startCharCodes + ((char) Integer.parseInt(startCharCodesHex.substring(c, (c+2)), 16)));
						System.out.print(" - char codes are " + startCharCodes + " (");
						for (int c = 0; c < startCharCodes.length(); c++)
							 System.out.print(((c == 0) ? "" : " ") + Integer.toString(((int) startCharCodes.charAt(c)), 16));
						System.out.println(")");
					}
					
					//	compute split proportions and character code
					ImWord[] splitWords = new ImWord[splitWordParts.length];
					String[] splitWordCharCodes = new String[splitWordParts.length];
					float[] splitWordWidths = new float[splitWordParts.length];
					int fontStyle = Font.PLAIN;
					if (start.hasAttribute(ImWord.BOLD_ATTRIBUTE))
						fontStyle = (fontStyle | Font.BOLD);
					if (start.hasAttribute(ImWord.ITALICS_ATTRIBUTE))
						fontStyle = (fontStyle | Font.ITALIC);
					Font font = new Font("Serif", fontStyle, start.getFontSize());
					float splitWordWidthSum = 0;
					int splitWordCharCodeStart = 0;
					for (int s = 0; s < splitWordParts.length; s++) {
						if (startCharCodes != null) {
							splitWordCharCodes[s] = startCharCodes.substring(splitWordCharCodeStart, (splitWordCharCodeStart + splitWordParts[s].length()));
							splitWordCharCodeStart += splitWordParts[s].length();
						}
						TextLayout tl = new TextLayout(splitWordParts[s], font, new FontRenderContext(null, false, true));
						splitWordWidths[s] = ((float) tl.getBounds().getWidth());
						splitWordWidthSum += splitWordWidths[s];
					}
					
					//	generate sub words, adding them to document right away
					//	TODO_not centralize this ==> too much action specific stuff in here
					int startWidth = start.bounds.getWidth();
					float splitWordLeft = start.bounds.left;
					for (int s = 0; s < splitWordParts.length; s++) {
						float splitWordWidth = ((startWidth * splitWordWidths[s]) / splitWordWidthSum);
						BoundingBox splitWordBox = new BoundingBox(Math.round(splitWordLeft), Math.round(splitWordLeft + splitWordWidth), start.bounds.top, start.bounds.bottom);
						splitWords[s] = new ImWord(start.getPage(), splitWordBox, splitWordParts[s]);
						splitWords[s].copyAttributes(start);
						if (startCharCodes != null) {
							StringBuffer splitWordCharCodeString = new StringBuffer();
							for (int c = 0; c < splitWordCharCodes[s].length(); c++)
								splitWordCharCodeString.append(Integer.toString(((int) splitWordCharCodes[s].charAt(c)), 16).toUpperCase());
							splitWords[s].setAttribute(ImFont.CHARACTER_CODE_STRING_ATTRIBUTE, splitWordCharCodeString.toString());
						}
						splitWordLeft += splitWordWidth;
						System.out.println(" --> split word part " + splitWords[s].getString() + ", bounds are " + splitWords[s].bounds);
						if (startCharCodes != null) {
							System.out.print("   - char codes are " + splitWordCharCodes[s] + " (");
							for (int c = 0; c < splitWordCharCodes[s].length(); c++)
								 System.out.print(((c == 0) ? "" : " ") + Integer.toString(((int) splitWordCharCodes[s].charAt(c)), 16));
							System.out.println(")");
						}
					}
					
					//	chain sub words together and into text stream
					for (int w = 0; w < splitWords.length; w++)
						splitWords[w].setPreviousWord((w == 0) ? prevWord : splitWords[w-1]);
					if (nextWord != null)
						nextWord.setPreviousWord(splitWords[splitWords.length-1]);
					
					//	link annotations to new words
					for (int a = 0; a < wordAnnots.length; a++) {
						if (wordAnnots[a].getFirstWord() == start)
							wordAnnots[a].setFirstWord(splitWords[0]);
						if (wordAnnots[a].getLastWord() == start)
							wordAnnots[a].setLastWord(splitWords[splitWords.length-1]);
					}
					
					//	remove original word
					start.getPage().removeWord(start, true);
					
					//	clean up annotations if required
					if (wordAnnots.length != 0)
						idmp.document.cleanupAnnotations();
					
					//	indicate change
					return true;
				}
			});
			
			//	offer removing word
			actions.add(new SelectionAction("wordsRemove", "Remove Word", "Remove selected word.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return removeWords(start, end);
				}
			});
		}
		
		//	multiple words selected
		else {
			
			//	offer removing words
			actions.add(new SelectionAction("wordsRemove", "Remove Words", "Remove selected words.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return removeWords(start, end);
				}
			});
			
			//	offer merging words if possible
			Tokenizer tokenizer = ((Tokenizer) idmp.document.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER));
			if (this.isMergeableSelection(start, end, tokenizer)) {
				StringBuffer before = new StringBuffer();
				StringBuffer after = new StringBuffer("'");
				for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
					if (imw == end) {
						if (start.getNextWord() == end)
							before.append(" and ");
						else before.append(", and ");
						before.append("'" + imw.getString() + "'");
						after.append(imw.getString() + "'");
						break;
					}
					else {
						if (imw != start)
							before.append(", ");
						before.append("'" + imw.getString() + "'");
						after.append(imw.getString());
					}
				}
				
				actions.add(new SelectionAction("wordsMerge", "Merge Words", ("Physically merge " + before.toString() + " into " + after.toString() + ".")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return mergeWords(start, end, idmp.document, false, (idmp.documentBornDigital ? null : idmp));
					}
				});
				if (idmp.documentBornDigital) // OCR errors are not as regular as font ones, no use offering 'Merge All' on scans
					actions.add(new SelectionAction("wordsMerge", "Merge All", ("Physically merge all occurrences of " + before.toString() + " into " + after.toString() + ".")) {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return mergeWords(start, end, idmp.document, true, null);
						}
					});
			}
		}
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ClickActionProvider#getActions(de.uka.ipd.idaho.im.ImWord, int, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public ClickSelectionAction[] getActions(final ImWord word, int clickCount, final ImDocumentMarkupPanel idmp) {
		if (idmp.documentBornDigital)
			return null;
		if (!idmp.areTextStringsPainted())
			return null;
		ClickSelectionAction[] csas = {
			new ClickSelectionAction("editWord", "Edit Line Words") {
				public boolean handleClick(ImDocumentMarkupPanel invoker) {
					return editLineWords(word, idmp);
				}
				public int getPriority() {
					return 10;
				}
			}
		};
		return csas;
	}
	
	private boolean editLineWords(ImWord word, ImDocumentMarkupPanel idmp) {
		ImPage page = word.getPage();
		if (page == null)
			return false;
		ImWord first = findFirstLineWord(word);
		ImWord last = findLastLineWord(word);
		OcrLineEditor oe = new OcrLineEditor(idmp, page, first, last, word);
		idmp.setDisplayOverlay(oe, word.pageId);
		return true;
	}
	
	private boolean markWord(ImPage page, ImWord[] selectedWords, BoundingBox selectedBox, ImDocumentMarkupPanel idmp) {
		
		//	collect strings and bold and italic properties from words in selection, and find predecessor and successor
		BoundingBox wordBox = adjustImageBounds(page, selectedBox, false);
		if (wordBox == null)
			return false;
		PageImage pageImage = page.getPageImage();
		StringBuffer wordString = new StringBuffer();
		int charCount = 0;
		int boldCharCount = 0;
		int italicsCharCount = 0;
		int fontSizeCharCount = 0;
		int fontSizeSum = 0;
		int baselineCharCount = 0;
		int baselineSum = 0;
		ImWord prevWord = null;
		ImWord nextWord = null;
		String textStreamType = ImWord.TEXT_STREAM_TYPE_MAIN_TEXT;
		boolean gotParagraphEnd = true;
		
		//	no existing words selected, apply OCR
		if (selectedWords == null) {
			OcrEngine ocr = this.imagineParent.getOcrEngine();
			if (ocr != null) try {
				ImDocument ocrDoc = new ImDocument(idmp.document.docId);
				ImPage ocrPage = new ImPage(ocrDoc, page.pageId, page.bounds);
				ImRegion ocrRegion = new ImRegion(ocrPage, wordBox, ImagingConstants.REGION_ANNOTATION_TYPE);
				ocr.doBlockOcr(ocrRegion, pageImage, null);
				ImWord[] ocrWords = ocrRegion.getWords();
				for (int w = 0; w < ocrWords.length; w++) {
					wordString.append(ocrWords[w].getString());
					charCount += ocrWords[w].getString().length();
					if (ocrWords[w].hasAttribute(ImWord.BOLD_ATTRIBUTE))
						boldCharCount += ocrWords[w].getString().length();
					if (ocrWords[w].hasAttribute(ImWord.ITALICS_ATTRIBUTE))
						italicsCharCount += ocrWords[w].getString().length();
					if (ocrWords[w].getFontSize() != -1) {
						fontSizeCharCount += ocrWords[w].getString().length();
						fontSizeSum += (ocrWords[w].getFontSize() * ocrWords[w].getString().length());
					}
					if (ocrWords[w].hasAttribute(ImWord.BASELINE_ATTRIBUTE)) {
						int baseline = Integer.parseInt((String) ocrWords[w].getAttribute(ImWord.BASELINE_ATTRIBUTE, "-1"));
						if (baseline != -1) {
							baselineCharCount += ocrWords[w].getString().length();
							baselineSum += (baseline * ocrWords[w].getString().length());
						}
					}
				}
			}
			catch (IOException ioe) {
				ioe.printStackTrace(System.out);
			}
			
			//	TODO compute (approximate) baseline if not done before
//			boolean gotAscent = false;
//			boolean gotDescent = false;
//			for (int c = 0; c < word.getString().length(); c++) {
//				char ch = word.getString().charAt(c);
//				char bch = StringUtils.getBaseChar(ch);
//				gotAscent = (gotAscent || Character.isUpperCase(bch) || Character.isDigit(bch) || ("bdfhijklt".indexOf(bch) != -1) || ("°!§$%&/(){}[]?#'|\\\"*€".indexOf(bch) != -1) || (ch != bch));
//				gotDescent = (gotDescent || ("gjqy".indexOf(bch) != -1) || ((bch == 'f') && word.hasAttribute(ImWord.ITALICS_ATTRIBUTE)));
//			}
//			int baseline = word.bounds.bottom;
//			if (gotDescent)
//				baseline -= ((word.bounds.bottom - word.bounds.top) / 4);
//			word.setAttribute(ImWord.BASELINE_ATTRIBUTE, ("" + baseline));
			
			//	TODO _somehow_ find predecessor and successor, check for paragraph end, and determine text stream type
		}
		
		//	we have existing words, use them
		else {
			Arrays.sort(selectedWords, ImUtils.textStreamOrder);
			HashSet selectedWordIDs = new HashSet();
			for (int w = 0; w < selectedWords.length; w++) {
				wordString.append(selectedWords[w].getString());
				charCount += selectedWords[w].getString().length();
				if (selectedWords[w].hasAttribute(ImWord.BOLD_ATTRIBUTE))
					boldCharCount += selectedWords[w].getString().length();
				if (selectedWords[w].hasAttribute(ImWord.ITALICS_ATTRIBUTE))
					italicsCharCount += selectedWords[w].getString().length();
				selectedWordIDs.add(selectedWords[w].getLocalID());
				if (prevWord == null)
					prevWord = selectedWords[w].getPreviousWord();
				nextWord = selectedWords[w].getNextWord();
				if (selectedWords[w].getFontSize() != -1) {
					fontSizeCharCount += selectedWords[w].getString().length();
					fontSizeSum += (selectedWords[w].getFontSize() * selectedWords[w].getString().length());
				}
				if (selectedWords[w].hasAttribute(ImWord.BASELINE_ATTRIBUTE)) {
					int baseline = Integer.parseInt((String) selectedWords[w].getAttribute(ImWord.BASELINE_ATTRIBUTE, "-1"));
					if (baseline != -1) {
						baselineCharCount += selectedWords[w].getString().length();
						baselineSum += (baseline * selectedWords[w].getString().length());
					}
				}
			}
			while ((prevWord != null) && selectedWordIDs.contains(prevWord.getLocalID()))
				prevWord = prevWord.getPreviousWord();
			while ((nextWord != null) && selectedWordIDs.contains(nextWord.getLocalID()))
				nextWord = nextWord.getNextWord();
			if ((nextWord != null) && (nextWord.getPreviousRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END))
				gotParagraphEnd = true;
			if (prevWord != null)
				textStreamType = prevWord.getTextStreamType();
		}
//		
//		//	collect strings and bold and italic properties from words overlapping selection, and find predecessor and successor
//		ImWord[] pWords = page.getWords();
//		ArrayList opWords = new ArrayList();
//		Arrays.sort(pWords, ImUtils.textStreamOrder);
//		ImWord prevWord = null;
//		ImWord nextWord = null;
//		boolean gotParagraphEnd = false;
//		for (int w = 0; w < pWords.length; w++) {
//			if (!wordBox.includes(pWords[w].bounds, true) && !pWords[w].bounds.includes(wordBox, true))
//				continue;
//			opWords.add(pWords[w]);
//			if (prevWord == null)
//				prevWord = pWords[w].getPreviousWord();
//			nextWord = pWords[w].getNextWord();
//			charCount += pWords[w].getString().length();
//			if (pWords[w].hasAttribute(ImWord.BOLD_ATTRIBUTE))
//				boldCharCount += pWords[w].getString().length();
//			if (pWords[w].hasAttribute(ImWord.ITALICS_ATTRIBUTE))
//				italicsCharCount += pWords[w].getString().length();
//			if (pWords[w].getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
//				gotParagraphEnd = true;
//		}
		
		//	create word
		ImWord mWord = new ImWord(idmp.document, page.pageId, wordBox, wordString.toString());
		if (charCount < (boldCharCount * 2))
			mWord.setAttribute(ImWord.BOLD_ATTRIBUTE);
		if (charCount < (italicsCharCount * 2))
			mWord.setAttribute(ImWord.ITALICS_ATTRIBUTE);
		if (fontSizeSum != 0)
			mWord.setFontSize((fontSizeSum + (fontSizeCharCount / 2)) / fontSizeCharCount);
		if (baselineSum != 0)
			mWord.setAttribute(ImWord.BASELINE_ATTRIBUTE, ("" + ((baselineSum + (baselineCharCount / 2)) / baselineCharCount)));
		
		//	get tokenizer to check and split words with multiple tokens
		Tokenizer tokenizer = ((Tokenizer) idmp.document.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER));
		TokenWord[] tWords = tokenizeWord(mWord, tokenizer);
		
		//	split word at token boundaries right here
		ImWord[] sWords = new ImWord[tWords.length];
		if (tWords.length == 1)
			sWords[0] = mWord;
		else for (int w = 0; w < tWords.length; w++) {
			sWords[w] = new ImWord(idmp.document, page.pageId, tWords[w].bounds, tWords[w].string);
			sWords[w].setFontSize(mWord.getFontSize());
			if (mWord.hasAttribute(ImWord.BOLD_ATTRIBUTE))
				sWords[w].setAttribute(ImWord.BOLD_ATTRIBUTE);
			if (mWord.hasAttribute(ImWord.ITALICS_ATTRIBUTE))
				sWords[w].setAttribute(ImWord.ITALICS_ATTRIBUTE);
			if (mWord.getFontSize() != -1)
				sWords[w].setFontSize(mWord.getFontSize());
			if (mWord.hasAttribute(ImWord.BASELINE_ATTRIBUTE))
				sWords[w].setAttribute(ImWord.BASELINE_ATTRIBUTE, mWord.getAttribute(ImWord.BASELINE_ATTRIBUTE));
			if (w != 0)
				sWords[w-1].setNextWord(sWords[w]);
		}
		
		//	display split result word(s) for editing (in modal dialog embedding OCR line editor with all split result words)
		ImWord[] mWords = this.editWords(page, sWords, idmp, idmp.getTextStreamTypeColor(textStreamType), tokenizer);
		if (mWords == null)
			return false; // canceled
		
		//	integrate newly marked word(s)
		if (selectedWords == null) {
			for (int w = 0; w < mWords.length; w++) {
				page.addWord(mWords[w]);
				if (w == 0)
					mWords[w].setPreviousWord(prevWord);
				else mWords[w].setPreviousWord(mWords[w-1]);
			}
			mWords[mWords.length-1].setNextWord(nextWord);
		}
		
		//	do the whole hustle and dance with annotation re-anchoring if we are to remove any words
		else {
			
			//	remember any annotations starting or ending at any of the to-remove words
			ArrayList startingAnnots = new ArrayList(3);
			ArrayList endingAnnots = new ArrayList(3);
			for (int w = 0; w < selectedWords.length; w++) {
				ImAnnotation[] imwAnnots = idmp.document.getAnnotations(selectedWords[w]);
				for (int a = 0; a < imwAnnots.length; a++) {
					if (imwAnnots[a].getFirstWord() == selectedWords[w])
						startingAnnots.add(imwAnnots[a]);
					if (imwAnnots[a].getLastWord() == selectedWords[w])
						endingAnnots.add(imwAnnots[a]);
				}
			}
			
			//	preserve text stream type
			String textSTreamType = selectedWords[0].getTextStreamType();
			
			//	connect previous and next words to preserve annotations TODO revisit cutting out whole thing
//			if (startPrev != null)
//				startPrev.setNextWord(endNext);
//			else if (endNext != null)
//				endNext.setPreviousWord(startPrev);
			if (prevWord != null)
				prevWord.setNextWord(nextWord);
			else if (nextWord != null)
				nextWord.setPreviousWord(prevWord);
			
			//	cut merged words out of streams
			selectedWords[selectedWords.length-1].setNextWord(null);
			selectedWords[0].setPreviousWord(null);
			for (int w = 0; w < selectedWords.length; w++)
				selectedWords[0].setNextWord(null);
			
			//	integrate merged word in document
			for (int w = 0; w < mWords.length; w++) {
				page.addWord(mWords[w]);
				if (w == 0) {
					if (prevWord == null)
						mWords[w].setTextStreamType(textSTreamType);
					else mWords[w].setPreviousWord(prevWord);
				}
				else mWords[w].setPreviousWord(mWords[w-1]);
			}
			mWords[mWords.length-1].setNextWord(nextWord);
			
			//	take care of annotations
			for (int a = 0; a < startingAnnots.size(); a++)
				((ImAnnotation) startingAnnots.get(a)).setFirstWord(mWords[0]);
			for (int a = 0; a < endingAnnots.size(); a++)
				((ImAnnotation) endingAnnots.get(a)).setLastWord(mWords[mWords.length-1]);
			
			//	clean up merged words
			for (int w = 0; w < selectedWords.length; w++)
				page.removeWord(selectedWords[w], true);
			
			//	make sure to get annotations in order in backing document
			idmp.document.cleanupAnnotations();
		}
//		
//		//	remember start predecessor and end successor
//		ImWord startPrev = selectedWords[0].getPreviousWord();
//		ImWord endNext = selectedWords[selectedWords.length-1].getNextWord();
		
		//	we _did_ change something, quite something
		return true;
	}
//	
//	private boolean markWord(ImPage page, ImWord[] exWords, BoundingBox wordBox, ImWord prevWord, ImWord nextWord, ImDocumentMarkupPanel idmp) {
//		
//		//	collect strings and bold and italic properties from words in selection, and find predecessor and successor
////		BoundingBox wordBox = new BoundingBox(eLeft, eRight, eTop, eBottom);
////		int charCount = 0;
////		int boldCharCount = 0;
////		int italicsCharCount = 0;
////		
////		//	collect strings and bold and italic properties from words overlapping selection, and find predecessor and successor
////		ImWord[] pWords = page.getWords();
////		ArrayList opWords = new ArrayList();
////		Arrays.sort(pWords, ImUtils.textStreamOrder);
////		ImWord prevWord = null;
////		ImWord nextWord = null;
////		boolean gotParagraphEnd = false;
////		for (int w = 0; w < pWords.length; w++) {
////			if (!wordBox.includes(pWords[w].bounds, true) && !pWords[w].bounds.includes(wordBox, true))
////				continue;
////			opWords.add(pWords[w]);
////			if (prevWord == null)
////				prevWord = pWords[w].getPreviousWord();
////			nextWord = pWords[w].getNextWord();
////			charCount += pWords[w].getString().length();
////			if (pWords[w].hasAttribute(ImWord.BOLD_ATTRIBUTE))
////				boldCharCount += pWords[w].getString().length();
////			if (pWords[w].hasAttribute(ImWord.ITALICS_ATTRIBUTE))
////				italicsCharCount += pWords[w].getString().length();
////			if (pWords[w].getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
////				gotParagraphEnd = true;
////		}
////		
////		//	do OCR (we only ever get here if no words are selected)
////		StringBuffer wordString = new StringBuffer();
////		for (int w = 0; w < exWords.length; w++) {
////			wordString.append(exWords[w].getString());
////			if (exWords[w].hasAttribute(ImWord.BOLD_ATTRIBUTE))
////				boldCharCount += exWords[w].getString().length();
////			if (exWords[w].hasAttribute(ImWord.ITALICS_ATTRIBUTE))
////				italicsCharCount += exWords[w].getString().length();
////		}
////		
////		//	create word
////		ImWord word = new ImWord(idmp.document, page.pageId, wordBox, wordString.toString());
////		if (charCount < (boldCharCount * 2))
////			word.setAttribute(ImWord.BOLD_ATTRIBUTE);
////		if (charCount < (italicsCharCount * 2))
////			word.setAttribute(ImWord.ITALICS_ATTRIBUTE);
////		
////		//	prompt user
////		if (!idmp.editWord(word, true) || (word.getString().trim().length() == 0))
////			return false;
////		
////		//	compute baseline
////		boolean gotAscent = false;
////		boolean gotDescent = false;
////		for (int c = 0; c < word.getString().length(); c++) {
////			char ch = word.getString().charAt(c);
////			char bch = StringUtils.getBaseChar(ch);
////			gotAscent = (gotAscent || Character.isUpperCase(bch) || Character.isDigit(bch) || ("bdfhijklt".indexOf(bch) != -1) || ("°!§$%&/(){}[]?#'|\\\"*€".indexOf(bch) != -1) || (ch != bch));
////			gotDescent = (gotDescent || ("gjqy".indexOf(bch) != -1) || ((bch == 'f') && word.hasAttribute(ImWord.ITALICS_ATTRIBUTE)));
////		}
////		int baseline = word.bounds.bottom;
////		if (gotDescent)
////			baseline -= ((word.bounds.bottom - word.bounds.top) / 4);
////		word.setAttribute(ImWord.BASELINE_ATTRIBUTE, ("" + baseline));
////		
////		//	get tokenizer to check and split words with multiple tokens
////		Tokenizer tokenizer = ((Tokenizer) idmp.document.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER));
////		ImWord[] sWords = this.tokenizeWord(word, tokenizer);
////		
////		//	add word(s) to document
////		for (int w = 0; w < sWords.length; w++) {
////			page.addWord(sWords[w]);
////			if (w != 0)
////				sWords[w-1].setNextWord(sWords[w]);
////		}
////		
////		//	cut out overwritten words
////		if (prevWord != null)
////			prevWord.setNextWord(nextWord);
////		else if (nextWord != null)
////			nextWord.setPreviousWord(prevWord);
////		
////		//	integrate in text streams
////		if (prevWord != null)
////			sWords[0].setPreviousWord(prevWord);
////		if (nextWord != null)
////			sWords[sWords.length-1].setNextWord(nextWord);
////		if (gotParagraphEnd)
////			sWords[sWords.length-1].setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
////		
////		//	clean up overwritten words
////		for (int w = 0; w < opWords.size(); w++)
////			page.removeWord(((ImWord) opWords.get(w)), true);
//		
//		//	we _did_ change something, quite something
//		return true;
//	}
	
	private static int getMaxRegionColor(int[][] regionColors) {
		int maxRegionColor = 0;
		for (int c = 0; c < regionColors.length; c++) {
			for (int r = 0; r < regionColors[c].length; r++)
				maxRegionColor = Math.max(maxRegionColor, regionColors[c][r]);
		}
		return maxRegionColor;
	}
	
	private static TokenWord[] tokenizeWord(ImWord word, Tokenizer tokenizer) {
		TokenSequence wordTokens = tokenizer.tokenize(word.getString());
		if (wordTokens.size() == 1) {
			TokenWord[] words = {new TokenWord(word.getString(), word.bounds, false)};
			return words;
		}
		else return tokenizeWord(word.getString(), word.bounds, wordTokens, new Font("Serif", Font.BOLD, 24));
	}
	
	private static TokenWord[] tokenizeWord(String wordString, BoundingBox wordBox, TokenSequence wordTokens, Font wordFont) {
		if (wordString.length() == 0)
			return new TokenWord[0];
		ArrayList wordList = new ArrayList();
//		TokenSequence wordTokens = tokenizer.tokenize(wordString);
//		
//		if (wordTokens.size() == 1)
//			wordList.add(word);
//		else {
//			System.out.println("   - splitting " + wordString + " at " + wordBox + " into " + wordTokens.size() + " parts");
//			
//			//	get width for each token at word font size
//			//	TODOne centralize this
//			String[] splitTokens = new String[wordTokens.size()];
//			float[] splitTokenWidths = new float[wordTokens.size()];
//			Font wordFont = new Font("Serif", Font.BOLD, 24);
//			float splitTokenWidthSum = 0;
//			for (int s = 0; s < splitTokens.length; s++) {
//				splitTokens[s] = wordTokens.valueAt(s);
//				TextLayout tl = new TextLayout(splitTokens[s], wordFont, new FontRenderContext(null, false, true));
//				splitTokenWidths[s] = ((float) tl.getBounds().getWidth());
//				splitTokenWidthSum += splitTokenWidths[s];
//			}
//			
//			//	store split result, splitting word bounds accordingly
//			//	TODOne centralize this
//			int wordWidth = (word.bounds.right - word.bounds.left);
//			int splitTokenStart = word.bounds.left;
//			for (int s = 0; s < splitTokens.length; s++) {
//				int splitTokenWidth = Math.round((wordWidth * splitTokenWidths[s]) / splitTokenWidthSum);
//				boolean cutLeft = ((s != 0) && (splitTokenWidths[s-1] < splitTokenWidths[s]));
//				boolean cutRight = (((s + 1) != splitTokens.length) && (splitTokenWidths[s+1] < splitTokenWidths[s]));
//				BoundingBox sWordBox = new BoundingBox(
//						(splitTokenStart + (cutLeft ? 1 : 0)),
//						Math.min((splitTokenStart + splitTokenWidth - (cutRight ? 1 : 0)), word.bounds.right),
//						word.bounds.top,
//						word.bounds.bottom
//					);
//				ImWord sWord = new ImWord(word.getDocument(), word.pageId, sWordBox, wordTokens.valueAt(s));
//				sWord.copyAttributes(word);
//				wordList.add(sWord);
//				System.out.println("     - part " + sWord.getString() + " at " + sWord.bounds);
//				splitTokenStart += splitTokenWidth;
//			}
//		}
//		
//		//	finally ...
//		return ((ImWord[]) wordList.toArray(new ImWord[wordList.size()]));
		
		//	get width for each token at word font size
		Graphics2D g = getHelperGraphics();
		String[] splitTokens = new String[wordTokens.size()];
		boolean[] spaceAfterSplitToken = new boolean[wordTokens.size()];
		float[] splitTokenWidths = new float[wordTokens.size()];
		float splitTokenWidthSum = 0;
		int spaceCount = 0;
		for (int s = 0; s < splitTokens.length; s++) {
			splitTokens[s] = wordTokens.valueAt(s);
			spaceAfterSplitToken[s] = (((s+1) == splitTokens.length) ? false : (wordTokens.getWhitespaceAfter(s).length() != 0));
			TextLayout tl = new TextLayout(splitTokens[s], wordFont, g.getFontRenderContext());
			splitTokenWidths[s] = ((float) tl.getBounds().getWidth());
			splitTokenWidthSum += splitTokenWidths[s];
			if (spaceAfterSplitToken[s])
				spaceCount++;
		}
		TextLayout tl = new TextLayout(wordString, wordFont, g.getFontRenderContext());
		float wordTokenSequenceWidth = ((float) tl.getBounds().getWidth());
		float spaceWidth = ((spaceCount == 0) ? 0 : ((wordTokenSequenceWidth - splitTokenWidthSum) / spaceCount));
		
		//	compute bounding boxes for split result
		BoundingBox[] splitBoxes = new BoundingBox[splitTokens.length];
		int splitTokenStart = wordBox.left;
		for (int s = 0; s < splitTokens.length; s++) {
			int splitTokenWidth = Math.round((wordBox.getWidth() * splitTokenWidths[s]) / wordTokenSequenceWidth);
			boolean cutLeft = ((s != 0) && (splitTokenWidths[s-1] < splitTokenWidths[s]));
			boolean cutRight = (((s + 1) != splitTokens.length) && (splitTokenWidths[s+1] < splitTokenWidths[s]));
			splitBoxes[s] = new BoundingBox(
					(splitTokenStart + (cutLeft ? 1 : 0)),
					(((s + 1) == splitTokens.length) ? wordBox.right : Math.min((splitTokenStart + splitTokenWidth - (cutRight ? 1 : 0)), wordBox.right)),
					wordBox.top,
					wordBox.bottom
				);
			wordList.add(new TokenWord(splitTokens[s], splitBoxes[s], spaceAfterSplitToken[s]));
			splitTokenStart += splitTokenWidth;
			if (((s+1) < splitTokens.length) && spaceAfterSplitToken[s])
				splitTokenStart += Math.round((wordBox.getWidth() * spaceWidth) / wordTokenSequenceWidth);
		}
		
		//	finally ...
		return ((TokenWord[]) wordList.toArray(new TokenWord[wordList.size()]));
	}
	
	private static class TokenWord {
		final String string;
		final BoundingBox bounds;
		final boolean spaceAfter;
		TokenWord(String string, BoundingBox bounds, boolean spaceAfter) {
			this.string = string;
			this.bounds = bounds;
			this.spaceAfter = spaceAfter;
		}
	}
	
	private boolean removeWords(ImWord start, ImWord end) {
		
		//	collect words
		ArrayList wordList = new ArrayList();
		for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
			wordList.add(imw);
			if (imw == end)
				break;
		}
		ImWord[] words = ((ImWord[]) wordList.toArray(new ImWord[wordList.size()]));
		
		//	remove words
		ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_ARTIFACT, null);
		ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
		for (ImWord imw = words[words.length-1]; imw != null; imw = imw.getPreviousWord())
			imw.setNextWord(null);
		for (int w = 0; w < words.length; w++) {
			ImPage page = words[w].getPage();
			if (page != null)
				page.removeWord(words[w], true);
		}
//		
//		//	cut out selected words
//		ImWord startPrev = start.getPreviousWord();
//		ImWord endNext = end.getNextWord();
//		if (startPrev != null)
//			startPrev.setNextWord(endNext);
//		else if (endNext != null)
//			endNext.setPreviousWord(startPrev);
//		
//		//	remove words, but dissolve stream first, back to front
//		ArrayList words = new ArrayList(5);
//		for (ImWord imw = end; imw != null; imw = imw.getPreviousWord()) {
//			words.add(imw);
//			imw.setNextWord(null);
//		}
//		ImPage page = start.getPage();
//		for (int w = 0; w < words.size(); w++)
//			page.removeWord(((ImWord) words.get(w)), true);
		
		//	indicate change
		return true;
	}
	
	private boolean isMergeableSelection(ImWord start, ImWord end, Tokenizer tokenizer) {
		StringBuffer mergeResult = ((tokenizer == null) ? null : new StringBuffer());
		String textStreamType = null;
		String fontName = null;
		for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
			
			//	only allow mergers inside a single line on a single page
			if (imw.pageId != start.pageId)
				return false;
			if ((imw.centerY < start.bounds.top) || (start.bounds.bottom < imw.centerY))
				return false;
			
			//	collect merged string to check tokenization
			if (mergeResult != null)
				mergeResult.append(imw.getString());
			
			//	only allow mergers if font name not set (in scanned documents) or equal
			if (fontName == null)
				fontName = ((String) imw.getAttribute(ImWord.FONT_NAME_ATTRIBUTE));
			else if (!fontName.equals(imw.getAttribute(ImWord.FONT_NAME_ATTRIBUTE, fontName)))
				return false;
			
			//	only allow mergers if text stream type equal
			if (textStreamType == null)
				textStreamType = imw.getTextStreamType();
			else if (!textStreamType.equals(imw.getTextStreamType()))
				return false;
			
			//	we have all we need
			if (imw == end)
				break;
		}
		
		//	only allow mergers if words don't tokenize apart
		return ((tokenizer == null) || (tokenizer.tokenize(mergeResult).size() == 1));
	}
	
	private ImWord getMergedWord(ImWord[] words, ImPage page, ImDocument doc) {
		
		//	collect merger data
		int mLeft = page.bounds.right;
		int mRight = page.bounds.left;
		int mTop = page.bounds.bottom;
		int mBottom = page.bounds.top;
		StringBuffer mString = new StringBuffer();
		StringBuffer mCharCodes = new StringBuffer();
		int mBoldCharCount = 0;
		int mItalicsCharCount = 0;
		int mFontSizeCount = 0;
		int mFontSizeSum = 0;
		int mBaselineCount = 0;
		int mBaselineSum = 0;
		for (int w = 0; w < words.length; w++) {
			mLeft = Math.min(mLeft, words[w].bounds.left);
			mRight = Math.max(mRight, words[w].bounds.right);
			mTop = Math.min(mTop, words[w].bounds.top);
			mBottom = Math.max(mBottom, words[w].bounds.bottom);
			mString.append(words[w].getString());
			String imwCharCodes = ((String) words[w].getAttribute(ImFont.CHARACTER_CODE_STRING_ATTRIBUTE));
			if (imwCharCodes != null)
				mCharCodes.append(imwCharCodes);
			if (words[w].hasAttribute(ImWord.BOLD_ATTRIBUTE))
				mBoldCharCount += words[w].getString().length();
			if (words[w].hasAttribute(ImWord.ITALICS_ATTRIBUTE))
				mItalicsCharCount += words[w].getString().length();
			try {
				int wFontSize = words[w].getFontSize();
				if (wFontSize != -1) {
					mFontSizeCount++;
					mFontSizeSum += wFontSize;
				}
			} catch (NumberFormatException nfe) {}
			try {
				int wBaseline = Integer.parseInt((String) words[w].getAttribute(ImWord.BASELINE_ATTRIBUTE, "-1"));
				if (wBaseline != -1) {
					mBaselineCount++;
					mBaselineSum += wBaseline;
				}
			} catch (NumberFormatException nfe) {}
		}
		
		//	physically merge words
		ImWord mWord = new ImWord(doc, page.pageId, new BoundingBox(mLeft, mRight, mTop, mBottom), mString.toString());
		if (mCharCodes.length() != 0)
			mWord.setAttribute(ImFont.CHARACTER_CODE_STRING_ATTRIBUTE, mCharCodes.toString());
		if (mBaselineCount != 0)
			mWord.setAttribute(ImWord.BASELINE_ATTRIBUTE, ("" + (mBaselineSum / mBaselineCount)));
		if ((mBoldCharCount * 2) > mString.length())
			mWord.setAttribute(ImWord.BOLD_ATTRIBUTE);
		if ((mItalicsCharCount * 2) > mString.length())
			mWord.setAttribute(ImWord.ITALICS_ATTRIBUTE);
		if (mFontSizeCount != 0)
			mWord.setFontSize(mFontSizeSum / mFontSizeCount);
		if (words[0].hasAttribute(ImWord.FONT_NAME_ATTRIBUTE))
			mWord.setAttribute(ImWord.FONT_NAME_ATTRIBUTE, words[0].getAttribute(ImWord.FONT_NAME_ATTRIBUTE));
		mWord.setTextStreamType(words[0].getTextStreamType());
		
		//	finally ...
		return mWord;
	}
	
	private ImWord getMergedWord(ImWord start, ImWord end, ImDocument doc) {
		ArrayList words = new ArrayList(3);
		for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
			words.add(imw);
			if (imw == end)
				break;
		}
		return this.getMergedWord(((ImWord[]) words.toArray(new ImWord[words.size()])), start.getPage(), doc);
	}
	
	private boolean mergeWords(ImWord start, ImWord end, ImDocument doc, boolean all, ImDocumentMarkupPanel editIdmp) {
		
		//	find all occurrences of arguments, and recurse
		if (all) {
			boolean modified = false;
			ImWord[] tshs = doc.getTextStreamHeads();
			HashMap matchStartsToEnds = new LinkedHashMap();
			for (int h = 0; h < tshs.length; h++)
				for (ImWord imw = tshs[h]; imw != null; imw = imw.getNextWord()) {
					if (imw == start)
						continue; // we already have this one
					if (!start.getString().equals(imw.getString()))
						continue;
					ImWord matchStart = imw;
					ImWord matchPos = imw.getNextWord();
					ImWord matchEnd = null;
					ImWord compPos = start.getNextWord();
					while ((matchPos != null) && (compPos != null)) {
						if (!compPos.getString().equals(matchPos.getString()))
							break;
						if (compPos == end) {
							matchEnd = matchPos;
							break;
						}
						matchPos = matchPos.getNextWord();
						compPos = compPos.getNextWord();
					}
					if ((matchEnd != null) && (this.isMergeableSelection(matchStart, matchEnd, null)))
						matchStartsToEnds.put(matchStart, matchEnd);
				}
			
			//	merge originally selected words last, we need them for matching
			modified = (this.mergeWords(start, end, doc, false, null) || modified);
			for (Iterator msit = matchStartsToEnds.keySet().iterator(); msit.hasNext();) {
				ImWord matchStart = ((ImWord) msit.next());
				ImWord matchEnd = ((ImWord) matchStartsToEnds.get(matchStart));
				modified = (this.mergeWords(matchStart, matchEnd, doc, false, null) || modified);
			}
			
			//	indicate changes
			return modified;
		}
		
		//	merge argument words
		else {
			
			//	physically merge words
			ImWord mWord = this.getMergedWord(start, end, doc);
			ImPage page = start.getPage();
			
			//	in a recursion, or with a born-digital document, we can attach the word right away
			if (editIdmp == null) {}
			
			//	if we have a scanned document, do NOT attach merged word right away, but open for editing, and only add if edit confirmed
//			else if (editIdmp.editWord(mWord, true)) {}
			else {
				ImWord[] mWords = {mWord};
				mWords = this.editWords(page, mWords, editIdmp, editIdmp.getTextStreamTypeColor(start.getTextStreamType()), ((Tokenizer) editIdmp.document.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER)));
				
				//	edit cancelled
				if (mWords == null)
					return false;
				
				//	looking good
				else if (mWords.length == 1)
					mWord = mWords[0];
				
				//	something's hinky
				else return false;
			}
			
			//	remember start predecessor and end successor
			ImWord startPrev = start.getPreviousWord();
			ImWord endNext = end.getNextWord();
			char endNextRelation = end.getNextRelation();
			
			//	remember any annotations starting or ending at any of the to-remove words
			ArrayList startingAnnots = new ArrayList(3);
			ArrayList endingAnnots = new ArrayList(3);
			for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
				ImAnnotation[] imwAnnots = doc.getAnnotations(imw);
				for (int a = 0; a < imwAnnots.length; a++) {
					if (imwAnnots[a].getFirstWord() == imw)
						startingAnnots.add(imwAnnots[a]);
					if (imwAnnots[a].getLastWord() == imw)
						endingAnnots.add(imwAnnots[a]);
				}
				if (imw == end)
					break;
			}
			
			//	connect previous and next words to preserve annotations
			if (startPrev != null)
				startPrev.setNextWord(endNext);
			else if (endNext != null)
				endNext.setPreviousWord(startPrev);
			
			//	collect merged words for removal, and dissolve stream
			end.setNextWord(null);
			start.setPreviousWord(null);
			ArrayList words = new ArrayList(5);
			for (ImWord imw = end; imw != null; imw = imw.getPreviousWord()) {
				words.add(imw);
				imw.setNextWord(null);
			}
			
			//	integrate merged word in document
			page.addWord(mWord);
			mWord.setPreviousWord(startPrev);
			mWord.setNextWord(endNext);
			mWord.setNextRelation(endNextRelation);
			for (int a = 0; a < startingAnnots.size(); a++)
				((ImAnnotation) startingAnnots.get(a)).setFirstWord(mWord);
			for (int a = 0; a < endingAnnots.size(); a++)
				((ImAnnotation) endingAnnots.get(a)).setLastWord(mWord);
			
			//	clean up merged words, finally
			for (int w = 0; w < words.size(); w++)
				page.removeWord(((ImWord) words.get(w)), true);
			
			//	we _did_ change something, quite something
			return true;
		}
	}
	
	private ImWord[] editWords(ImPage page, ImWord[] words, ImDocumentMarkupPanel idmp, Color textStreamColor, Tokenizer tokenizer) {
		
		//	get position of aggregate word bounding box ...
		BoundingBox wordsBox = ImLayoutObject.getAggregateBox(words);
		if (wordsBox == null)
			return null;
		Rectangle wordsPos = idmp.getPosition(wordsBox, page.pageId);
		if (wordsPos == null)
			return null;
		
		//	... and translate that to absolute on-screen coordinates ...
		Component comp = idmp;
		Window w = DialogFactory.getTopWindow();
		if (w == null)
			return null;
		int xOff = 0;
		int yOff = 0;
		while (comp != null) {
			if (comp == w)
				break;
			Point loc = comp.getLocation();
			xOff += loc.x;
			yOff += loc.y;
			comp = comp.getParent();
			if (comp instanceof Window)
				break;
		}
		
		//	... to position editing dialog there
		OcrLineEditorDialog oled = new OcrLineEditorDialog(w, textStreamColor, tokenizer, page, wordsBox, words);
		Insets oledi = oled.getInsets();
		oled.setLocation((wordsPos.x + xOff + w.getLocation().x - oledi.left), (wordsPos.y + yOff + w.getLocation().y - oledi.top));
		oled.setVisible(true);
		
		//	return editing result
		return oled.getResult();
	}
	
	private static BoundingBox adjustImageBounds(ImPage page, BoundingBox imageBounds, boolean isLine) {
		PageImage pageImage = page.getPageImage();
//		System.out.println("Expanding " + imageBounds);
		
		//	start with original selection
		int eLeft = imageBounds.left;
		int eRight = imageBounds.right;
		int eTop = imageBounds.top;
		int eBottom = imageBounds.bottom;
		
		//	limit expansion (more restrictive on lines, which can be skewed, warped, etc.)
		//	TODO maybe also restrict difference between upward and downward expansion ?!?
		int oCoreTop = (imageBounds.top + (isLine ? (imageBounds.getHeight() / 5) : 0));
		int oCoreBottom = (imageBounds.bottom - (isLine ? (imageBounds.getHeight() / 5) : 0));
		int eTopMin = (isLine ? (imageBounds.top - (imageBounds.getHeight() / 2)) : 0);
		int eBottomMax = (isLine ? (imageBounds.bottom + (imageBounds.getHeight() / 2)) : pageImage.image.getHeight());
		
		//	get region coloring of selected rectangle
		boolean changed;
		boolean[] originalSelectionRegionInCol;
		boolean[] originalSelectionRegionInRow;
		do {
//			System.out.println(" - inspecting " + "[" + eLeft + "," + eRight + "," + eTop + "," + eBottom + "]");
			
			//	get region coloring for current rectangle
			int[][] regionColors = Imaging.getRegionColoring(Imaging.wrapImage(pageImage.image.getSubimage(eLeft, eTop, (eRight - eLeft), (eBottom - eTop)), null), ((byte) 120), true);
			
			//	check which region colors occur in original selection
			boolean[] regionColorInOriginalSelection = new boolean[getMaxRegionColor(regionColors)];
			Arrays.fill(regionColorInOriginalSelection, false);
//			System.out.println(" - got " + regionColorInOriginalSelection.length + " region colors");
			for (int c = Math.max((imageBounds.left - eLeft), 0); c < Math.min((imageBounds.right - eLeft), regionColors.length); c++)
				for (int r = Math.max((imageBounds.top - eTop), 0); r < Math.min((imageBounds.bottom - eTop), regionColors[c].length); r++) {
					if ((r + eTop) < oCoreTop)
						continue;
					if (oCoreBottom < (r + eTop))
						continue;
					if (regionColors[c][r] != 0) {
//						if (!regionColorInOriginalSelection[regionColors[c][r]-1])
//							System.out.println("   - found " + regionColors[c][r] + " at " + (c + eLeft) + "/" + (r + eTop));
						regionColorInOriginalSelection[regionColors[c][r]-1] = true;
					}
				}
//			for (int rc = 0; rc < regionColorInOriginalSelection.length; rc++) {
//				if (!regionColorInOriginalSelection[rc])
//					System.out.println("   - " + rc + " not found");
//			}
			
			//	assess which columns and rows contain regions that overlap original selection
			originalSelectionRegionInCol = new boolean[regionColors.length];
			Arrays.fill(originalSelectionRegionInCol, false);
			originalSelectionRegionInRow = new boolean[regionColors[0].length];
			Arrays.fill(originalSelectionRegionInRow, false);
			for (int c = 0; c < regionColors.length; c++) {
				for (int r = 0; r < regionColors[c].length; r++)
					if (regionColors[c][r] != 0) {
//						if (!originalSelectionRegionInCol[c] && regionColorInOriginalSelection[regionColors[c][r]-1])
//							System.out.println("   - found " + regionColors[c][r] + " for col " + c + " at " + r);
						originalSelectionRegionInCol[c] = (originalSelectionRegionInCol[c] || regionColorInOriginalSelection[regionColors[c][r]-1]);
//						if (!originalSelectionRegionInRow[r] && regionColorInOriginalSelection[regionColors[c][r]-1])
//							System.out.println("   - found " + regionColors[c][r] + " for row " + (r + eTop) + " at " + (c + eLeft));
						originalSelectionRegionInRow[r] = (originalSelectionRegionInRow[r] || regionColorInOriginalSelection[regionColors[c][r]-1]);
					}
			}
			
			//	adjust boundaries
			changed = false;
			if (originalSelectionRegionInCol[0] && (eLeft != 0)) {
				eLeft--;
				changed = true;
			}
			else for (int c = 0; (c+1) < originalSelectionRegionInCol.length; c++) {
				if (originalSelectionRegionInCol[c+1])
					break;
				else {
					eLeft++;
					changed = true;
				}
			}
			if (originalSelectionRegionInCol[originalSelectionRegionInCol.length-1] && (eRight != (pageImage.image.getWidth()-1))) {
				eRight++;
				changed = true;
			}
			else for (int c = (originalSelectionRegionInCol.length-1); c != 0; c--) {
				if (originalSelectionRegionInCol[c-1])
					break;
				else {
					eRight--;
					changed = true;
				}
			}
//			if (originalSelectionRegionInRow[0] && (eTop != 0)) {
			if (originalSelectionRegionInRow[0] && (eTop > eTopMin)) {
				eTop--;
				changed = true;
			}
			else for (int r = 0; (r+1) < originalSelectionRegionInRow.length; r++) {
				if (originalSelectionRegionInRow[r+1])
					break;
				else {
					eTop++;
					changed = true;
				}
			}
//			if (originalSelectionRegionInRow[originalSelectionRegionInRow.length-1] && (eBottom != (pageImage.image.getHeight()-1))) {
			if (originalSelectionRegionInRow[originalSelectionRegionInRow.length-1] && (eBottom < eBottomMax)) {
				eBottom++;
				changed = true;
			}
			else for (int r = (originalSelectionRegionInRow.length-1); r != 0; r--) {
				if (originalSelectionRegionInRow[r-1])
					break;
				else {
					eBottom--;
					changed = true;
				}
			}
			
			//	check if we still have something to work with
			if ((eRight <= eLeft) || (eBottom <= eTop))
				return null;
		}
		
		//	keep going while there is adjustments
		while (changed);
		
		//	cut white edge
		if (!originalSelectionRegionInCol[0])
			eLeft++;
		if (!originalSelectionRegionInCol[originalSelectionRegionInCol.length-1])
			eRight--;
		if (!originalSelectionRegionInRow[0])
			eTop++;
		if (!originalSelectionRegionInRow[originalSelectionRegionInRow.length-1])
			eBottom--;
		
		//	collect strings and bold and italic properties from words in selection, and find predecessor and successor
		return new BoundingBox(eLeft, eRight, eTop, eBottom);
	}
	
	private static class OcrLineEditorDialog extends DialogPanel {
		BufferedImage lineImage;
		OcrLineEditor lineEditor;
		OcrLineEditorDialog(Window w, Color textStreamColor, Tokenizer tokenizer, ImPage page, BoundingBox wordsBox, ImWord[] words) {
			super(w, true);
			this.getDialog().setUndecorated(true);
			this.setBorder(BorderFactory.createLineBorder(textStreamColor, 2));
			
			wordsBox = adjustImageBounds(page, wordsBox, false);
			this.lineImage = page.getPageImage().image.getSubimage(wordsBox.left, wordsBox.top, wordsBox.getWidth(), wordsBox.getHeight());
			JPanel lineImagePanel = new JPanel() {
				public void paint(Graphics g) {
					super.paint(g);
					Insets lipi = this.getInsets();
					g.drawImage(lineImage, lipi.left, lipi.top, null);
				}
				public Dimension getPreferredSize() {
					Insets lipi = this.getInsets();
					return new Dimension((lipi.left + lineImage.getWidth() + lipi.right), (lipi.top + lineImage.getHeight() + lipi.bottom));
				}
				public Dimension getMaximumSize() {
					return this.getPreferredSize();
				}
				public Dimension getMinimumSize() {
					return this.getPreferredSize();
				}
				public Dimension getSize() {
					return this.getPreferredSize();
				}
			};
			lineImagePanel.setBackground(Color.WHITE);
			lineImagePanel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
			this.add(lineImagePanel, BorderLayout.NORTH);
			
			this.lineEditor = new OcrLineEditor(textStreamColor, tokenizer, page, words[0], words[words.length-1], words[0]) {
				protected void overlayClosing(boolean isCancel) {
					super.overlayClosing(isCancel);
					dispose();
				}
			};
			this.lineEditor.update(page.getImageDPI());
			this.lineEditor.getLayout().layoutContainer(this.lineEditor);
			this.add(this.lineEditor, BorderLayout.CENTER);
			
			JButton commit = new JButton("OK");
			commit.setBackground(Color.WHITE);
			commit.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(textStreamColor, 1), BorderFactory.createLineBorder(Color.WHITE, 1)));
			commit.setPreferredSize(new Dimension(50, commit.getPreferredSize().height));
			commit.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ee) {
					lineEditor.close(false);
				}
			});
			JButton cancel = new JButton("Cancel");
			cancel.setBackground(Color.WHITE);
			cancel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(textStreamColor, 1), BorderFactory.createLineBorder(Color.WHITE, 1)));
			cancel.setPreferredSize(new Dimension(50, cancel.getPreferredSize().height));
			cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ee) {
					lineEditor.close(true);
				}
			});
			
			JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
			buttons.setBackground(Color.WHITE);
			buttons.add(commit);
			buttons.add(cancel);
			buttons.getLayout().layoutContainer(buttons);
			this.add(buttons, BorderLayout.SOUTH);
			
			this.getLayout().layoutContainer(this);
			this.setSize(this.getPreferredSize());
		}
		
		ImWord[] getResult() {
			return this.lineEditor.result;
		}
	}
	
	private static BufferedImage helperGraphicsSource = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);
	private static Graphics2D getHelperGraphics() {
		return helperGraphicsSource.createGraphics();
	}
	
	private static class OcrLineEditorLayout implements LayoutManager {
		OcrLineEditor parent;
		OcrLineEditorLayout(OcrLineEditor parent) {
			this.parent = parent;
		}
		public void addLayoutComponent(String name, Component comp) {}
		public void removeLayoutComponent(Component comp) {}
		public Dimension preferredLayoutSize(Container parent) {
			if (this.parent.words.isEmpty())
				return null;
			int renderingDpi = this.parent.getRenderingDpi();
			Insets pi = this.parent.getInsets();
			int minCtl = Integer.MAX_VALUE;
			int maxCtr = 0;
			int minCtt = Integer.MAX_VALUE;
			int maxCtb = 0;
			for (int w = 0; w < this.parent.words.size(); w++) {
				OcrWordEditor we = ((OcrWordEditor) this.parent.words.get(w));
				Insets wi = we.getInsets(); // need to _subtract_ these from left and top, as parent insets include them for overall size computation
				int wex = (pi.left - wi.left + ((we.posInParent.x * renderingDpi) / this.parent.page.getImageDPI()));
				int wey = (pi.top - wi.top + ((we.posInParent.y * renderingDpi) / this.parent.page.getImageDPI()));
				int wew = (wi.left + ((we.sizeInParent.width * renderingDpi) / this.parent.page.getImageDPI()) + wi.right);
				int weh = (wi.top + ((we.sizeInParent.height * renderingDpi) / this.parent.page.getImageDPI()) + wi.bottom);
				minCtl = Math.min(minCtl, wex);
				maxCtr = Math.max(maxCtr, (wex + wew));
				minCtt = Math.min(minCtt, wey);
				maxCtb = Math.max(maxCtb, (wey + weh));
			}
			return new Dimension((pi.left + (maxCtr - minCtl) + pi.right), (pi.top + (maxCtb - minCtt) + pi.bottom));
		}
		public Dimension minimumLayoutSize(Container parent) {
			return this.parent.getMinimumSize();
		}
		public void layoutContainer(Container target) {
			if (this.parent.words.isEmpty())
				return;
			int renderingDpi = this.parent.getRenderingDpi();
			synchronized (target.getTreeLock()) {
				Insets pi = this.parent.getInsets();
				
				int wih = 0;
				if (this.parent.wordImageTray != null) {
					wih = ((this.parent.wordImage.getHeight() * renderingDpi) / this.parent.page.getImageDPI());
					System.out.println("Aligning word image " + wih + " pixels high");
					this.parent.wordImageTray.setBounds(pi.left, pi.top, ((this.parent.wordImage.getWidth() * renderingDpi) / this.parent.page.getImageDPI()), wih);
				}
				
				int minCtl = Integer.MAX_VALUE;
				int maxCtr = 0;
				int minCtt = Integer.MAX_VALUE;
				int maxCtb = 0;
				for (int w = 0; w < this.parent.words.size(); w++) {
					OcrWordEditor we = ((OcrWordEditor) this.parent.words.get(w));
					Insets wi = we.getInsets(); // need to _subtract_ these from left and top, as parent insets include them for overall size computation
					int wex = (pi.left - wi.left + ((we.posInParent.x * renderingDpi) / this.parent.page.getImageDPI()));
//					int wey = (pi.top - wi.top + ((we.posInParent.y * renderingDpi) / this.parent.page.getImageDPI()));
					int wey = (pi.top - wi.top + wih + ((we.posInParent.y * renderingDpi) / this.parent.page.getImageDPI()));
					int wew = (wi.left + ((we.sizeInParent.width * renderingDpi) / this.parent.page.getImageDPI()) + wi.right);
					int weh = (wi.top + ((we.sizeInParent.height * renderingDpi) / this.parent.page.getImageDPI()) + wi.bottom);
					minCtl = Math.min(minCtl, wex);
					maxCtr = Math.max(maxCtr, (wex + wew));
					minCtt = Math.min(minCtt, wey);
					maxCtb = Math.max(maxCtb, (wey + weh));
					if (we != this.parent.selWord)
						we.setBounds(wex, wey, wew, weh);
				}
				
				//	make sure selected word has some minimum width so commas, etc. display fully at least when selected
				if (this.parent.selWord != null) {
					OcrWordEditor swe = this.parent.selWord;
					Insets swi = swe.getInsets(); // need to _subtract_ these from left and top, as parent insets include them for overall size computation
					int swex = (pi.left - swi.left + ((swe.posInParent.x * renderingDpi) / this.parent.page.getImageDPI()));
//					int swey = (pi.top - swi.top + ((swe.posInParent.y * renderingDpi) / this.parent.page.getImageDPI()));
					int swey = (pi.top - swi.top + wih + ((swe.posInParent.y * renderingDpi) / this.parent.page.getImageDPI()));
					int swew = (swi.left + ((swe.sizeInParent.width * renderingDpi) / this.parent.page.getImageDPI()) + swi.right);
					int sweh = (swi.top + ((swe.sizeInParent.height * renderingDpi) / this.parent.page.getImageDPI()) + swi.bottom);
					int minWew = (swi.left + ((swe.minWidhtInParent * renderingDpi) / this.parent.page.getImageDPI()) + swi.right);
					if (swew < minWew) {
						swex -= ((minWew - swew) / 2);
						swew = minWew;
						if (swex < minCtl)
							swex = minCtl;
						else if ((swex + swew) > maxCtr)
							swex = (maxCtr - swew);
					}
					swe.setBounds(swex, swey, swew, sweh);
				}
				
				Dimension tbs = this.parent.toolBar.getPreferredSize();
				int tbw = tbs.width;
				if (tbw > (maxCtr - minCtl))
					tbw = (this.parent.toolBarSpacingLeft + (maxCtr - minCtl) + this.parent.toolBarSpacingRight);
				int tbx = (minCtl + ((this.parent.toolBar.targetCenterX * renderingDpi) / this.parent.page.getImageDPI()) - (tbw / 2) - this.parent.toolBarSpacingLeft);
				if (tbx < minCtl)
					tbx = (minCtl - this.parent.toolBarSpacingLeft);
				else if ((tbx + tbw) > maxCtr)
					tbx = (maxCtr + this.parent.toolBarSpacingRight - tbw);
				this.parent.toolBar.setBounds(tbx, maxCtb, tbw, tbs.height);
			}
		}
	}
	
	private static class OcrLineEditor extends DisplayOverlay {
		final ImDocumentMarkupPanel target;
		ImWord[] result = null;
		
		ImPage page = null;;
		Tokenizer tokenizer;
		
		ImWord first = null;;
		ImWord last = null;;
		String textStreamType;
		int baseline;
		Color textStreamColor;
		
		WordEditTools toolBar;
		SymbolTable symbolTable;
		
		OcrWordEditor selWord = null;
		ArrayList words = new ArrayList();
		BufferedImage wordImage = null;
		JPanel wordImageTray = null;
		Font wordFont = Font.getFont("Serif");
		int wordLeft;
		int wordRight;
		int wordTop;
		int wordBottom;
		Insets nonZoomContentInsets = new Insets(0, 0, 0, 0);
		int toolBarSpacingLeft = 0;
		int toolBarSpacingRight = 0;
		Color wordBackground;
		int layoutRenderingDpi = -1;
		
		//	normal editing mode (with navigation and everything)
		OcrLineEditor(ImDocumentMarkupPanel target, ImPage page, ImWord first, ImWord last, ImWord sel) {
			this(target, null, ((Tokenizer) target.document.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER)), page, first, last, sel);
		}
		//	dialog mode (one-off single-line editing)
		OcrLineEditor(Color textStreamColor, Tokenizer tokenizer, ImPage page, ImWord first, ImWord last, ImWord sel) {
			this(null, textStreamColor, tokenizer, page, first, last, sel);
		}
		private OcrLineEditor(ImDocumentMarkupPanel target, Color textStreamColor, Tokenizer tokenizer, ImPage page, ImWord first, ImWord last, ImWord sel) {
			this.target = target;
			this.textStreamColor = textStreamColor;
			this.setLayout(new OcrLineEditorLayout(this));
			Color bgc = this.getBackground();
			this.wordBackground = new Color(
					(255 - ((255 - bgc.getRed()) / 2)),
					(255 - ((255 - bgc.getGreen()) / 2)),
					(255 - ((255 - bgc.getBlue()) / 2))
				);
			this.toolBar = new WordEditTools(this);
			this.tokenizer = tokenizer;
			if (this.target != null)
				this.wordImageTray = new JPanel(true) {
					public void paint(Graphics gr) {
						super.paint(gr);
						if (wordImage != null) {
							Dimension size = this.getSize();
							gr.drawImage(wordImage, 0, 0, size.width, size.height, null);
						}
					}
				};
			if ((first != null) && (last != null))
				this.setWords(page, first, last, ((sel == null) ? first : sel), -1, false);
			else if (first != null)
				this.setWords(page, first, first, first, -1, false);
		}
		
		int getRenderingDpi() {
			return ((this.target == null) ? this.page.getImageDPI() : this.target.getRenderingDpi());
		}
		
		void setWords(ImPage page, ImWord first, ImWord last, ImWord sel, int selCaretPos, boolean isLateralMove) {
			if ((this.target == null) && (this.page != null))
				throw new IllegalStateException("Cannot navigate in dialog mode");
			
			//	check where we're coming in from
			boolean selFromPrev = (isLateralMove && (sel == first) && (this.last != null) && (first == this.last.getNextWord()));
			boolean selFromNext = (isLateralMove && (sel == last) && (this.first != null) && (last == this.first.getPreviousWord()));
//			System.out.println("Selection from line predecessor or successor is " + selFromPrev + " and " + selFromNext);
			
			//	clean up previous content
			this.page = null;
			this.first = null;
			this.last = null;
			this.textStreamType = null;
			this.baseline = -1;
			this.selWord = null;
			this.words.clear();
			this.removeAll();
			
			//	check arguments
			if ((first == null) || (last == null))
				return;
			if (!first.getTextStreamId().equals(last.getTextStreamId()))
				return;
			if (first.pageId != last.pageId)
				return;
			if (ImUtils.textStreamOrder.compare(first, last) > 0) {
				ImWord temp = first;
				first = last;
				last = temp;
			}
			
			//	display one JTextField per word (in double linked list)
			this.page = page;
			this.first = first;
			this.last = last;
			this.textStreamType = this.first.getTextStreamType();
			if (this.target != null) {
				this.textStreamColor = this.target.getTextStreamTypeColor(this.first.getTextStreamType());
				this.setBorder(BorderFactory.createLineBorder(this.textStreamColor, 1));
			}
			this.toolBar.setTextStreamColor(this.textStreamColor);
			int top = this.page.bounds.bottom;
			int bottom = this.page.bounds.top;
			int baselineCount = 0;
			int baselineSum = 0;
			OcrWordEditor selWe = null;
			OcrWordEditor lastWe = null;
			for (ImWord imw = first; imw != null; imw = imw.getNextWord()) {
				OcrWordEditor we = new OcrWordEditor(this, imw);
				if (lastWe != null) {
					lastWe.next = we;
					we.previous = lastWe;
				}
				lastWe = we;
				this.words.add(we);
				top = Math.min(top, imw.bounds.top);
				bottom = Math.max(bottom, imw.bounds.bottom);
				try {
					int bl = Integer.parseInt((String) imw.getAttribute(ImWord.BASELINE_ATTRIBUTE, "-1"));
					if (bl != -1) {
						baselineCount++;
						baselineSum += bl;
					}
				} catch (NumberFormatException nfe) {}
				if (imw == sel)
					selWe = we;
				if (imw == last)
					break;
			}
			this.wordLeft = first.bounds.left;
			this.wordRight = last.bounds.right;
			this.wordTop = top;
			this.wordBottom = bottom;
			this.baseline = ((baselineCount == 0) ? -1 : ((baselineSum + (baselineCount / 2)) / baselineCount));
			this.add(this.toolBar);
			if (this.wordImageTray != null) {
				BoundingBox wordsBox = adjustImageBounds(this.page, new BoundingBox(this.wordLeft, this.wordRight, this.wordTop, this.wordBottom), true);
				this.wordImage = this.page.getPageImage().image.getSubimage(wordsBox.left, wordsBox.top, wordsBox.getWidth(), wordsBox.getHeight());
				this.add(this.wordImageTray);
			}
			this.selectWord(selWe, selFromPrev, selFromNext, selCaretPos); // select first word (also adds all the other word editors, validates, and repaints)
			
			//	make sure we have the appropriate size and position
			this.adjustSizeAndPosition(first.bounds.left, first.bounds.top, (last.bounds.right - first.bounds.left), ((this.wordBottom - this.wordTop) + ((this.wordImage == null) ? 0 : (this.wordImage.getHeight() + 2))));
			if (this.layoutRenderingDpi != -1) {
				int layoutRenderingDpi = this.layoutRenderingDpi;
				this.layoutRenderingDpi = -1;
				this.update(layoutRenderingDpi); // updates font when content changed (e.g. via arrow key navigation)
			}
		}
		
		private boolean selectingWord = false;
		void selectWord(final OcrWordEditor selWe, boolean selFromPrev, boolean selFromNext, int prefCaretPos) {
			if (this.selectingWord)
				return; // most likely called as result of inherent (and undesired) focus traversal resulting from removing and re-adding all word editors when selecting one (to get latter on top)
			boolean unlockImmediately = true;
			try {
				this.selectingWord = true;
				unlockImmediately = this.doSelectWord(selWe, selFromPrev, selFromNext, prefCaretPos);
			}
			finally {
				if (unlockImmediately)
					this.selectingWord = false;
			}
		}
		private boolean doSelectWord(final OcrWordEditor selWe, boolean selFromPrev, boolean selFromNext, int prefCaretPos) {
//			System.out.println("SELECTING WORD " + ((selWe == null) ? "null" : selWe.getText()));
//			(new Exception()).printStackTrace(System.out);
			if (selWe == this.selWord)
				return true;
			
			this.toolBar.setTarget(null, -1);
			this.removeAll(); // need to do it this way to prevent auto-focusing some remaining text field when removing selected one to get it to end (and thus on top in painting)
			this.add(this.toolBar);
			if (this.wordImageTray != null)
				this.add(this.wordImageTray);
			
			this.nonZoomContentInsets.left = 0;
			this.nonZoomContentInsets.right = 0;
			this.nonZoomContentInsets.top = 0;
			this.nonZoomContentInsets.bottom = 0;
			for (int w = 0; w < this.words.size(); w++) {
				OcrWordEditor we = ((OcrWordEditor) this.words.get(w));
				this.nonZoomContentInsets.left = Math.max(this.nonZoomContentInsets.left, we.getInsets().left);
				this.nonZoomContentInsets.right = Math.max(this.nonZoomContentInsets.right, we.getInsets().right);
				this.nonZoomContentInsets.top = Math.max(this.nonZoomContentInsets.top, we.getInsets().top);
				this.nonZoomContentInsets.bottom = Math.max(this.nonZoomContentInsets.bottom, we.getInsets().bottom);
				if (we == selWe)
					we.setBackground(Color.WHITE);
				else {
					we.setBackground((we.getText().length() == 0) ? Color.DARK_GRAY : this.wordBackground); // set empty words to dark gray to indicate pending removal
					this.add(we);
				}
			}
			this.nonZoomContentInsets.bottom += this.toolBar.getPreferredSize().height; // add space for tool bar, which also doesn't zoom
			
			final int selWeCaretPos;
			if (selWe == null)
				selWeCaretPos = -1;
			else if (prefCaretPos != -1)
				selWeCaretPos = prefCaretPos;
			else if (selFromPrev)
				selWeCaretPos = 0; // coming in directly from left, if maybe in a line, column, or page wrapped kind of way
			else if (selFromNext)
				selWeCaretPos = selWe.getText().length(); // coming in directly from right, if maybe in a line, column, or page wrapped kind of way
			else if (this.selWord == null)
				selWeCaretPos = (selWe.getText().length() / 2);
			else selWeCaretPos = -1;
			this.selWord = selWe;
			if (this.selWord != null) {
				this.add(this.selWord); // need to add this one last so it paints on top
				this.toolBar.setTarget(this.selWord, (this.selWord.posInParent.x + (this.selWord.sizeInParent.width / 2)));
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
//						System.out.println("selectWord() requesting focus in window for " + selWord.getText() + " on selecting " + selWe.getText() + ", setting caret to " + selWeCaretPos);
						selectingWord = false;
						if (selWeCaretPos != -1)
							selWord.setCaretPosition(selWeCaretPos);
						selWord.requestFocusInWindow();
					}
				});
			}
//			System.out.println("Selected word set to " + ((this.selWord == null) ? "null" : this.selWord.getText()));
			this.validate();
			this.repaint();
			return (this.selWord == null); // need to unlock right away if there is no selection to request focus for
		}
		
		protected float getVerticalAnchor() {
			return 0.0f; // we want to sit right below the words, not on top of them
		}
		
		protected void update(int renderingDpi) {
//			System.out.println(" - updating to rendering DPI " + renderingDpi + " (previously " + this.layoutRenderingDpi + ")");
			if ((renderingDpi == -1) || (this.layoutRenderingDpi == renderingDpi))
				return;
			
			//	compute layout height
			int rLineHeight = (((this.wordBottom - this.wordTop) * renderingDpi) / this.page.getImageDPI());
//			System.out.println(" - rendering line height is " + rLineHeight);
			
			//	compute and adjust font size if required
			Graphics2D g = getHelperGraphics();
			Font wf = ((this.wordFont == null) ? new Font("Serif", Font.PLAIN, 12) : this.wordFont);
			int tfs = wf.getSize();
			Font tf = wf;
			TextLayout tl = new TextLayout("HgTq", tf, g.getFontRenderContext());
			while (tl.getBounds().getHeight() < rLineHeight) {
				tfs++;
				tf = wf.deriveFont((float) tfs);
				tl = new TextLayout("HgTq", tf, g.getFontRenderContext());
			}
			while (tl.getBounds().getHeight() > rLineHeight) {
				tfs--;
				tf = wf.deriveFont((float) tfs);
				tl = new TextLayout("HgTq", tf, g.getFontRenderContext());
			}
//			System.out.println("Font size adjusted to " + tfs);
			this.wordFont = tf;
			int renderFontHeight = g.getFontMetrics(this.wordFont).getHeight();
//			System.out.println(" - rendering font height is " + renderFontHeight);
			TextLayout mtl = new TextLayout("n", tf, g.getFontRenderContext()); // lower case n appears to be good approximation ...
			int renderMinWidth = ((int) Math.ceil(mtl.getBounds().getWidth()));
//			System.out.println(" - rendering n width is " + renderMinWidth);
			g.dispose();
			int inPageFontHeight = ((renderFontHeight * this.page.getImageDPI()) / renderingDpi);
//			System.out.println(" - page font height is " + inPageFontHeight);
			int inPageMinWidth = ((renderMinWidth * this.page.getImageDPI()) / renderingDpi);
//			System.out.println(" - page n width is " + inPageMinWidth);
			this.layoutRenderingDpi = renderingDpi;
			
			//	update individual word editors
			this.toolBarSpacingLeft = 0; // we need the pure word layout to correctly ...
			this.toolBarSpacingRight = 0; // ... compute spacing we need to add for toolbar
			this.getLayout().layoutContainer(this); // word editors need their sizes _before_ updating fonts, and we also need to measure toolbar
			int tbw = this.toolBar.getWidth();
			int tbpw = this.toolBar.getPreferredSize().width;
			if (tbw < tbpw) {
				int tbs = (tbpw - tbw);
				this.toolBarSpacingLeft = (tbs / 2);
				this.toolBarSpacingRight = (tbs - this.toolBarSpacingLeft);
				this.getLayout().layoutContainer(this);
			}
			for (int w = 0; w < this.words.size(); w++)
				((OcrWordEditor) this.words.get(w)).update(this.wordFont, inPageFontHeight, inPageMinWidth);
			//	TODO fix to _top_ of current line instead in order to to paint over normal display with undisturbed page image
			//	TODO add height of zoomed line image to overall height
//			this.adjustSizeAndPosition(this.wordLeft, this.wordBottom, (this.wordRight - this.wordLeft), inPageFontHeight);
			this.adjustSizeAndPosition(this.wordLeft, this.wordTop, (this.wordRight - this.wordLeft), (((this.wordImage == null) ? 0 : (this.wordImage.getHeight() + 2)) + inPageFontHeight));
//			if (this.selWord != null) // CANNOT DO THIS ... only helps with focus, but not other settings (display control, etc., so NO GOOD)
//				SwingUtilities.invokeLater(new Runnable() {
//					public void run() {
//						selWord.requestFocusInWindow();
//					}
//				});
		}
		
		public Insets getInsets() {
			/* TODO Centralize addition of non-zooming (border) content to overlay size:
- overwrite getInsets() method of overlay class proper ...
- ... to fetch content based additional "insets" from getNonZoomingContentInsets() mounting point ...
- ... and centrally combine those with insets for borders, etc.
- also add getNonZoomingContentSize() mounting point to centrally enforce minimum width and height (e.g. width of OCR editing toolbar) ...
- ... and use aggregate insets to also compensate for any deficiencies in that department
==> simplifies accommodating toolbar in upcoming OCR image editor, etc.
			 */
			Insets insets = super.getInsets();
			if (insets == null) {
				if ((this.toolBarSpacingLeft == 0) && (this.toolBarSpacingRight == 0))
					return this.nonZoomContentInsets;
				else return new Insets(
						(this.nonZoomContentInsets.top),
						(this.nonZoomContentInsets.left + this.toolBarSpacingLeft),
						(this.nonZoomContentInsets.bottom),
						(this.nonZoomContentInsets.right + this.toolBarSpacingRight)
					);
			}
			else return new Insets(
					(insets.top + this.nonZoomContentInsets.top),
					(insets.left + this.nonZoomContentInsets.left + this.toolBarSpacingLeft),
					(insets.bottom + this.nonZoomContentInsets.bottom),
					(insets.right + this.nonZoomContentInsets.right + this.toolBarSpacingRight)
				);
		}
		
		public void validate() {
//			System.out.println("Validating");
			this.update(this.getCurrentRenderingDpi());
			super.validate();
//			System.out.println("Validated");
		}
		
		protected void overlayClosing(boolean isCancel) {
			this.closeSymbolTable();
			if (isCancel)
				return;
			this.commitChanges();
		}
		
		void closeSymbolTable() {
			if (this.symbolTable == null)
				return;
			this.symbolTable.close();
			this.symbolTable = null;
		}
		
		void commitChanges() {
			
			//	sort out emptied word editors
			System.out.println("Checking " + this.words.size() + " word editors:");
			for (int w = 0; w < this.words.size(); w++) {
				OcrWordEditor we = ((OcrWordEditor) this.words.get(w));
				System.out.println(" - " + we.getText() + "@" + we.bounds);
				if ((we.getText().trim().length() == 0) && (we.word == null)) {
					if (we.previous != null)
						we.previous.next = we.next;
					if (we.next != null)
						we.next.previous = we.previous;
					for (we = we.next; we != null; we = we.next)
						we.textStreamPos = ((we.previous == null) ? 0 : (we.previous.textStreamPos + 1));
					this.words.remove(w--);
					System.out.println("   ==> removed as empty derived word");
				}
				else System.out.println("   ==> retained");
			}
			
			//	re-merge word editors that derive from shared ancestors, do not tokenize apart, and have no space between them
			System.out.println("Merging " + this.words.size() + " word editors:");
			for (int w = 1; w < this.words.size(); w++) {
				OcrWordEditor fwe = ((OcrWordEditor) this.words.get(w-1));
				OcrWordEditor lwe = ((OcrWordEditor) this.words.get(w));
				System.out.println(" - " + fwe.getText() + "@" + fwe.bounds + " & " + lwe.getText() + "@" + lwe.bounds);
				if (fwe.spaceAfter) {
					System.out.println("   ==> separated by space");
					continue;
				}
				AncestorSet fas = new AncestorSet();
				fas.addAll(fwe.ancestors);
				if (fas.isDisjoint(lwe.ancestors)) {
					System.out.println("   ==> disjoint ancestors");
					continue;
				}
				if (this.mergeWords(fwe, lwe, true)) {
					w--;
					System.out.println("   ==> merged");
				}
				else System.out.println("   ==> tokenize apart");
			}
			
			//	we're in dialog mode, simply line up words
			if (this.target == null) {
				System.out.println("Lining up " + this.words.size() + " dialog result words:");
				this.result = new ImWord[this.words.size()];
				for (int w = 0; w < this.words.size(); w++) {
					OcrWordEditor we = ((OcrWordEditor) this.words.get(w));
					if (we.word == null)
						we.word = new ImWord(this.page.getDocument(), this.page.pageId, we.bounds, we.getText());
					we.commitChanges();
					this.result[w] = we.word;
					System.out.println(" - " + this.result[w].getString() + "@" + this.result[w].bounds);
				}
				return;
			}
			
			//	line up original words and word editors
			ArrayList words = new ArrayList();
			ArrayList wordEds = new ArrayList(this.words);
			for (ImWord imw = this.first; imw != null; imw = imw.getNextWord()) {
				words.add(imw);
				if (imw == this.last)
					break;
			}
			
			//	start atomic action
			String lineStr;
			if (this.first == this.last)
				lineStr = this.first.getString();
			else if (this.first.getNextWord() == this.last)
				lineStr = ImUtils.getString(this.first, this.last, true);
			else lineStr = (this.first.getString() + " ... " + this.last.getString());
			this.target.beginAtomicAction("Edit OCR in Line '" + lineStr + "'");
			
			//	remember text stream neighbors
			ImWord firstPrev = this.first.getPreviousWord();
			ImWord lastNext = this.last.getNextWord();
			
			//	group word editors by common/overlapping (sets of) ancestors
			System.out.println("Clustering " + words.size() + " ancestor words and " + wordEds.size() + " editors:");
			AncestorSet ancestorTrace = new AncestorSet();
			ArrayList traceWordEds = new ArrayList();
			for (int w = 0; w < wordEds.size(); w++) {
				OcrWordEditor we = ((OcrWordEditor) wordEds.get(w));
				if (ancestorTrace.isDisjoint(we.ancestors) && (ancestorTrace.size() != 0)) {
					firstPrev = this.adjustWords(firstPrev, new ArrayList(ancestorTrace), traceWordEds, true, "");
					ancestorTrace.clear();
					traceWordEds.clear();
				}
				System.out.println(" - " + we.getText() + "@" + we.bounds);
				for (int a = 0; a < we.ancestors.length; a++)
					System.out.println("   - " + we.ancestors[a].getString() + "@" + we.ancestors[a].bounds);
				ancestorTrace.addAll(we.ancestors);
				traceWordEds.add(we);
				if (traceWordEds.size() == 1)
					System.out.println("   ==> started new cluster");
				else System.out.println("   ==> added to current cluster");
			}
			if (ancestorTrace.size() != 0)
				firstPrev = this.adjustWords(firstPrev, new ArrayList(ancestorTrace), traceWordEds, true, "");
			
			//	write changes to individual words
			for (int w = 0; w < this.words.size(); w++)
				((OcrWordEditor) this.words.get(w)).commitChanges();
			
			//	connect to text stream neighbors (firstPrev points to last word in line by now)
			if (firstPrev != null)
				firstPrev.setNextWord(lastNext);
			else if (lastNext != null)
				lastNext.setPreviousWord(null);
			
			//	finish atomic action
			this.target.endAtomicAction();
			
			//	make any changes show
			this.target.invalidate();
			this.target.validate();
			this.target.repaint();
		}
		
		private ImWord adjustWords(ImWord firstPrev, ArrayList ancestors, ArrayList wordEds, boolean tryAdjustBounds, String indent) {
			System.out.println(indent + "Adjusting words ...");
			System.out.print(indent + " - ancestors: ");
			for (int a = 0; a < ancestors.size(); a++) {
				System.out.print((a == 0) ? "[" : ", ");
				ImWord ancestor = ((ImWord) ancestors.get(a));
				System.out.print(ancestor.getString() + "@" + ancestor.bounds);
			}
			System.out.println("]");
			System.out.print(indent + " -  word eds: ");
			for (int w = 0; w < wordEds.size(); w++) {
				System.out.print((w == 0) ? "[" : ", ");
				OcrWordEditor we = ((OcrWordEditor) wordEds.get(w));
				System.out.print(we.getText() + "@" + we.bounds);
			}
			System.out.println("]");
			
			//	we have a one-on-one match-up, this one is trivial
			if ((ancestors.size() == 1) && (wordEds.size() == 1)) {
				System.out.println(indent + " ==> one-on-one matchup");
				OcrWordEditor we = ((OcrWordEditor) wordEds.get(0));
				
				//	word cleaned out, remove it (must be original word, other are cleaned up above)
				if (we.getText().trim().length() == 0) {
					ImWord nextWord = we.word.getNextWord();
					ImAnnotation[] wordAnnots = this.target.document.getAnnotations(we.word);
					for (int a = 0; a < wordAnnots.length; a++) {
						boolean annotStartsAtWord = (wordAnnots[a].getFirstWord() == we.word);
						boolean annotEndsAtWord = (wordAnnots[a].getLastWord() == we.word);
						if (annotStartsAtWord && annotEndsAtWord)
							this.target.document.removeAnnotation(wordAnnots[a]);
						else if (annotStartsAtWord && (nextWord != null))
							wordAnnots[a].setFirstWord(nextWord);
						else if (annotEndsAtWord && (firstPrev != null))
							wordAnnots[a].setLastWord(firstPrev);
						else this.target.document.removeAnnotation(wordAnnots[a]); // something is hinky about this one ...
					}
					if (nextWord != null)
						nextWord.setPreviousWord(firstPrev);
					ImPage wordPage = we.word.getPage();
					if (wordPage != null)
						wordPage.removeWord(we.word, true);
					we.word = null;
					System.out.println(indent + " ==> emptied word removed");
					return firstPrev;
				}
				
				//	make sure edits go to original word
				else {
					we.word = ((ImWord) ancestors.get(0));
					if (firstPrev == null)
						we.word.setTextStreamType(this.textStreamType);
					else we.word.setPreviousWord(firstPrev);
					System.out.println(indent + " ==> only word re-assigned");
					return we.word;
				}
			}
			
			//	try and pair up to to original words from start
			while ((ancestors.size() > 1) && (wordEds.size() > 1)) {
				ImWord fa = ((ImWord) ancestors.get(0));
				OcrWordEditor fwe = ((OcrWordEditor) wordEds.get(0));
				ImWord sa = ((ImWord) ancestors.get(1));
				OcrWordEditor swe = ((OcrWordEditor) wordEds.get(1));
				if (swe.bounds.left < fa.bounds.right)
					break; // first ancestor reaches into second word editor, not a one-to-one pair
				if (sa.bounds.left < fwe.bounds.right)
					break; // first word editor reaches into second ancestor, not a one-to-one pair
				System.out.println(indent + " ==> matched (from start) " + fwe.getText() + "@" + fwe.bounds + " to " + fa.getString() + "@" + fa.bounds + ", recursing");
				ArrayList fAncestors = new ArrayList(1);
				fAncestors.add(ancestors.remove(0));
				ArrayList fWordEds = new ArrayList(1);
				fWordEds.add(wordEds.remove(0));
				firstPrev = this.adjustWords(firstPrev, fAncestors, fWordEds, true, (indent + "  "));
				return this.adjustWords(firstPrev, ancestors, wordEds, true, (indent + "  "));
			}
			
			//	try and pair up to to original words from end
			while ((ancestors.size() > 1) && (wordEds.size() > 1)) {
				ImWord la = ((ImWord) ancestors.get(ancestors.size()-1));
				OcrWordEditor lwe = ((OcrWordEditor) wordEds.get(wordEds.size()-1));
				ImWord sla = ((ImWord) ancestors.get(ancestors.size()-2));
				OcrWordEditor slwe = ((OcrWordEditor) wordEds.get(wordEds.size()-2));
				if (lwe.bounds.left < sla.bounds.right)
					break; // second-to-last ancestor reaches into last word editor, not a one-to-one pair
				if (la.bounds.left < slwe.bounds.right)
					break; // second-to-last word editor reaches into last ancestor, not a one-to-one pair
				System.out.println(indent + " ==> matched (from end) " + lwe.getText() + "@" + lwe.bounds + " to " + la.getString() + "@" + la.bounds + ", recursing");
				ArrayList lAncestors = new ArrayList(1);
				lAncestors.add(ancestors.remove(ancestors.size()-1));
				ArrayList lWordEds = new ArrayList(1);
				lWordEds.add(wordEds.remove(wordEds.size()-1));
				firstPrev = this.adjustWords(firstPrev, ancestors, wordEds, true, (indent + "  "));
				return this.adjustWords(firstPrev, lAncestors, lWordEds, true, (indent + "  "));
			}
			
			//	try and pair up to to original words in middle
			for (int a = 1, w = 1; ((a+1) < ancestors.size()) && ((w+1) < wordEds.size()) ;) {
				ImWord ma = ((ImWord) ancestors.get(a));
				OcrWordEditor mwe = ((OcrWordEditor) wordEds.get(w));
				if (ma.bounds.right <= mwe.bounds.left) {
					a++; // ancestor to left of word editor, switch to next one
					continue;
				}
				if (mwe.bounds.right <= ma.bounds.left) {
					w++; // word editor to left of ancestor, switch to next one
					continue;
				}
				ImWord pa = ((ImWord) ancestors.get(a-1));
				OcrWordEditor pwe = ((OcrWordEditor) wordEds.get(w-1));
				if (mwe.bounds.left < pa.bounds.right) {
					w++; // word editor (partially) overlaps with previous ancestor, no good, move on to next one
					continue;
				}
				if (ma.bounds.left < pwe.bounds.right) {
					a++; // ancestor (partially) overlaps with previous word editor, no good, move on to next one
					continue;
				}
				ImWord na = ((ImWord) ancestors.get(a+1));
				OcrWordEditor nwe = ((OcrWordEditor) wordEds.get(w+1));
				if (na.bounds.left < mwe.bounds.right) {
					w++; // word editor protrudes into next ancestor, no good, move on to next one
					continue;
				}
				if (nwe.bounds.left < ma.bounds.right) {
					a++; // ancestor protrudes into next word editor, no good, move on to next one
					continue;
				}
				System.out.println(indent + " ==> matched (in middle) " + mwe.getText() + "@" + mwe.bounds + " to " + ma.getString() + "@" + ma.bounds + ", recursing");
				ArrayList pAncestors = new ArrayList(ancestors.subList(0, a));
				ArrayList pWordEds = new ArrayList(wordEds.subList(0, w));
				ArrayList mAncestors = new ArrayList(ancestors.subList(a, (a+1)));
				ArrayList mWordEds = new ArrayList(wordEds.subList(w, (w+1)));
				ArrayList nAncestors = new ArrayList(ancestors.subList((a+1), ancestors.size()));
				ArrayList nWordEds = new ArrayList(wordEds.subList((w+1), wordEds.size()));
				firstPrev = this.adjustWords(firstPrev, pAncestors, pWordEds, true, (indent + "  "));
				firstPrev = this.adjustWords(firstPrev, mAncestors, mWordEds, true, (indent + "  "));
				return this.adjustWords(firstPrev, nAncestors, nWordEds, true, (indent + "  "));
			}
			
			//	remove any leading and tailing ancestors not overlapping with any word editors (corresponding words might have been emptied after being re-split off other word)
			if ((ancestors.size() > 1) && (wordEds.size() != 0)) {
				OcrWordEditor fwe = ((OcrWordEditor) wordEds.get(0));
				OcrWordEditor lwe = ((OcrWordEditor) wordEds.get(wordEds.size()-1));
				int ancestorCount = ancestors.size();
				ImWord faPrev = firstPrev;
				ImWord laNext = ((ImWord) ancestors.get(ancestors.size()-1)).getNextWord();
				while (ancestors.size() != 0) {
					ImWord fa = ((ImWord) ancestors.get(0));
					if (fa.bounds.right <= fwe.bounds.left) {
						System.out.println(indent + " - removed spurious leading anchor " + fa.getString() + "@" + fa.bounds);
						ancestors.remove(0);
						ImAnnotation[] aAnnots = this.target.document.getAnnotations(fa);
						ImWord faNext = fa.getNextWord();
						for (int a = 0; a < aAnnots.length; a++) {
							boolean annotStartsAtWord = (aAnnots[a].getFirstWord() == fa);
							boolean annotEndsAtWord = (aAnnots[a].getLastWord() == fa);
							if (annotStartsAtWord && annotEndsAtWord)
								this.target.document.removeAnnotation(aAnnots[a]);
							else if (annotStartsAtWord && (faNext != null))
								aAnnots[a].setFirstWord(faNext);
							else if (annotEndsAtWord && (faPrev != null))
								aAnnots[a].setLastWord(faPrev);
							else this.target.document.removeAnnotation(aAnnots[a]); // something is hinky about this one ...
						}
						this.page.removeWord(fa, true);
						continue;
					}
					ImWord la = ((ImWord) ancestors.get(ancestors.size()-1));
					if (lwe.bounds.right <= la.bounds.left) {
						System.out.println(indent + " - removed spurious tailing anchor " + la.getString() + "@" + la.bounds);
						ancestors.remove(ancestors.size()-1);
						ImAnnotation[] aAnnots = this.target.document.getAnnotations(la);
						ImWord laPrev = la.getPreviousWord();
						for (int a = 0; a < aAnnots.length; a++) {
							boolean annotStartsAtWord = (aAnnots[a].getFirstWord() == la);
							boolean annotEndsAtWord = (aAnnots[a].getLastWord() == la);
							if (annotStartsAtWord && annotEndsAtWord)
								this.target.document.removeAnnotation(aAnnots[a]);
							else if (annotStartsAtWord && (laNext != null))
								aAnnots[a].setFirstWord(laNext);
							else if (annotEndsAtWord && (laPrev != null))
								aAnnots[a].setLastWord(laPrev);
							else this.target.document.removeAnnotation(aAnnots[a]); // something is hinky about this one ...
						}
						this.page.removeWord(la, true);
						continue;
					}
					break;
				}
				if (ancestors.size() < ancestorCount)
					return this.adjustWords(firstPrev, ancestors, wordEds, true, (indent + "  "));
			}
			
			//	make sure word proportions are sound inside current block (might be off due to sequence of individual from-end splits blowing them off as consequence of not considering all eventual spaces)
			if (tryAdjustBounds) {
				
				//	concatenate word editor text (including indicated spaces) and compute overall split
				StringBuffer wordBlockSb = new StringBuffer();
				for (int w = 0; w < wordEds.size(); w++) {
					OcrWordEditor we = ((OcrWordEditor) wordEds.get(w));
					wordBlockSb.append(we.getText());
					if (((w+1) < wordEds.size()) && we.spaceAfter)
						wordBlockSb.append(" ");
				}
				String wordBlockString = wordBlockSb.toString();
				System.out.println(indent + " - checking tokenization of " + wordBlockString);
				TokenSequence wordBlockTokens = this.tokenizer.tokenize(wordBlockString);
				System.out.println(indent + " - got " + wordBlockTokens.size() + " tokens");
				if (wordBlockTokens.size() == wordEds.size()) {
//					
//					//	get width for each token at word font size
//					//	TODOne centralize this
//					Graphics2D g = getHelperGraphics();
//					String[] splitTokens = new String[wordBlockTokens.size()];
//					boolean[] spaceAfterSplitToken = new boolean[wordBlockTokens.size()];
//					float[] splitTokenWidths = new float[wordBlockTokens.size()];
//					float splitTokenWidthSum = 0;
//					int spaceCount = 0;
//					for (int s = 0; s < splitTokens.length; s++) {
//						splitTokens[s] = wordBlockTokens.valueAt(s);
//						spaceAfterSplitToken[s] = (((s+1) == splitTokens.length) ? true : (wordBlockTokens.getWhitespaceAfter(s).length() != 0));
//						TextLayout tl = new TextLayout(splitTokens[s], this.wordFont, g.getFontRenderContext());
//						splitTokenWidths[s] = ((float) tl.getBounds().getWidth());
//						splitTokenWidthSum += splitTokenWidths[s];
//						if (spaceAfterSplitToken[s])
//							spaceCount++;
//					}
//					TextLayout tl = new TextLayout(wordBlockString, this.wordFont, g.getFontRenderContext());
//					float wordTokenSequenceWidth = ((float) tl.getBounds().getWidth());
//					float spaceWidth = ((spaceCount == 0) ? 0 : ((wordTokenSequenceWidth - splitTokenWidthSum) / spaceCount));
//					
//					//	compute bounding boxes for split result
//					//	TODOne centralize this
//					BoundingBox[] splitBoxes = new BoundingBox[splitTokens.length];
//					int splitBlockLeft = ((ImWord) ancestors.get(0)).bounds.left;
//					int splitBlockRight = ((ImWord) ancestors.get(ancestors.size()-1)).bounds.right;
//					int splitBlockWidth = (splitBlockRight - splitBlockLeft);
//					int splitTokenStart = splitBlockLeft;
//					for (int s = 0; s < splitTokens.length; s++) {
//						int splitTokenWidth = Math.round((splitBlockWidth * splitTokenWidths[s]) / wordTokenSequenceWidth);
//						boolean cutLeft = ((s != 0) && (splitTokenWidths[s-1] < splitTokenWidths[s]));
//						boolean cutRight = (((s + 1) != splitTokens.length) && (splitTokenWidths[s+1] < splitTokenWidths[s]));
//						splitBoxes[s] = new BoundingBox(
//								(splitTokenStart + (cutLeft ? 1 : 0)),
//								(((s + 1) == splitTokens.length) ? splitBlockRight : Math.min((splitTokenStart + splitTokenWidth - (cutRight ? 1 : 0)), splitBlockRight)),
//								this.wordTop,
//								this.wordBottom
//							);
//						splitTokenStart += splitTokenWidth;
//						if (((s+1) < splitTokens.length) && (wordBlockTokens.getWhitespaceAfter(s).length() != 0))
//							splitTokenStart += Math.round((splitBlockWidth * spaceWidth) / wordTokenSequenceWidth);
//					}
					
					//	compute proportional split result
					BoundingBox wordBlockBounds = new BoundingBox(
							((ImWord) ancestors.get(0)).bounds.left,
							((ImWord) ancestors.get(ancestors.size()-1)).bounds.right,
							this.wordTop,
							this.wordBottom
						);
					TokenWord[] splitWords = tokenizeWord(wordBlockString, wordBlockBounds, wordBlockTokens, this.wordFont);
					
					//	adjust word editor bounds accordingly
					System.out.println(indent + " - adjusting word editor boundaries to split proportions");
					boolean boundsChanged = false;
					for (int w = 0; w < wordEds.size(); w++) {
						OcrWordEditor we = ((OcrWordEditor) wordEds.get(w));
						System.out.print(indent + "   - " + we.getText() + "@" + we.bounds);
//						if (we.bounds.equals(splitBoxes[w]))
						if (we.bounds.equals(splitWords[w].bounds))
							System.out.println(" ==> unchanged");
						else {
//							we.bounds = splitBoxes[w];
							we.bounds = splitWords[w].bounds;
							System.out.println(" ==> changed to " + we.getText() + "@" + we.bounds);
							boundsChanged = true;
						}
					}
					
					//	recurse if there are any changes (but make damn sure not to do this twice !!!)
					if (boundsChanged)
						return this.adjustWords(firstPrev, ancestors, wordEds, false, (indent + "  "));
				}
			}
			
			//	splitting up single ancestor
			if (ancestors.size() == 1) {
				ImWord ancestor = ((ImWord) (ancestors.get(0)));
				System.out.println(indent + " - splitting up single ancestor " + ancestor.getString() + "@" + ancestor.bounds);
				ImWord first = null;
				ImWord last = firstPrev;
				for (int w = 0; w < wordEds.size(); w++) {
					OcrWordEditor we = ((OcrWordEditor) wordEds.get(w));
					we.word = new ImWord(this.page, we.bounds, we.getText());
					System.out.println(indent + "   - " + we.word.getString() + "@" + we.word.bounds);
					if (last == null)
						we.word.setTextStreamType(this.textStreamType);
					else last.setNextWord(we.word);
					if (first == null)
						first = we.word;
					last = we.word;
				}
				ImAnnotation[] aAnnots = this.target.document.getAnnotations(ancestor);
				for (int aa = 0; aa < aAnnots.length; aa++) {
					if (aAnnots[aa].getFirstWord() == ancestor)
						aAnnots[aa].setFirstWord(first);
					if (aAnnots[aa].getLastWord() == ancestor)
						aAnnots[aa].setLastWord(last);
				}
				this.page.removeWord(ancestor, true);
				return last;
			}
			
			//	merging several ancestors into single word
			if (wordEds.size() == 1) {
				OcrWordEditor we = ((OcrWordEditor) wordEds.get(0));
				we.word = new ImWord(this.page, we.bounds, we.getText());
				System.out.println(indent + " - merging multiple ancestors into " + we.word.getString() + "@" + we.word.bounds);
				if (firstPrev == null)
					we.word.setTextStreamType(this.textStreamType);
				else firstPrev.setNextWord(we.word);
				for (int a = 0; a < ancestors.size(); a++) {
					ImWord ancestor = ((ImWord) (ancestors.get(a)));
					System.out.println(indent + "   - " + ancestor.getString() + "@" + ancestor.bounds);
					ImAnnotation[] aAnnots = this.target.document.getAnnotations(ancestor);
					for (int aa = 0; aa < aAnnots.length; aa++) {
						if (aAnnots[aa].getFirstWord() == ancestor)
							aAnnots[aa].setFirstWord(we.word);
						if (aAnnots[aa].getLastWord() == ancestor)
							aAnnots[aa].setLastWord(we.word);
					}
					this.page.removeWord(ancestor, true);
				}
				return we.word;
			}
			
			//	TODO try and find token (type) based match-ups at most offset (by center-X) by some 10% of overall width, and at most the same difference in word width
			
			//	annotate and chain new words ...
			ImWord[] words = new ImWord[wordEds.size()];
			System.out.println(indent + " - marking " + wordEds.size() + " new words");
			for (int w = 0; w < wordEds.size(); w++) {
				OcrWordEditor we = ((OcrWordEditor) wordEds.get(w));
				we.word = new ImWord(this.page, we.bounds, we.getText());
				System.out.println(indent + "   - " + we.word.getString() + "@" + we.word.bounds);
				words[w] = we.word;
				if (firstPrev == null)
					we.word.setTextStreamType(this.textStreamType);
				else firstPrev.setNextWord(we.word);
				firstPrev = we.word;
			}
			
			//	... and clean up replaced ancestors
			System.out.println(indent + " - removing " + ancestors.size() + " obsolete ancestors");
			for (int a = 0; a < ancestors.size(); a++) {
				ImWord ancestor = ((ImWord) (ancestors.get(a)));
				System.out.println(indent + "   - " + ancestor.getString() + "@" + ancestor.bounds);
				ImAnnotation[] aAnnots = this.target.document.getAnnotations(ancestor);
				if (aAnnots.length != 0) {
					ImWord aAnnotFirst = null;
					ImWord aAnnotLast = null;
					if ((aAnnotFirst == null) || (aAnnotLast == null)) {
						for (int w = 0; w < words.length; w++) {
							if (words[w].centerX < ancestor.bounds.left)
								continue;
							if (ancestor.bounds.right < words[w].centerX)
								break;
							if (aAnnotFirst == null)
								aAnnotFirst = words[w];
							aAnnotLast = words[w];
						}
						if ((aAnnotFirst != null) && (aAnnotLast != null))
							System.out.println(indent + "     - found annotation anchors (1) " + aAnnotFirst.getString() + "@" + aAnnotFirst.bounds + " and " + aAnnotLast.getString() + "@" + aAnnotLast.bounds);
					}
					if ((aAnnotFirst == null) || (aAnnotLast == null)) {
						for (int w = 0; w < words.length; w++) {
							if (words[w].bounds.right < ancestor.centerX)
								continue;
							if (ancestor.centerX < words[w].bounds.left)
								break;
							if (aAnnotFirst == null)
								aAnnotFirst = words[w];
							aAnnotLast = words[w];
						}
						if ((aAnnotFirst != null) && (aAnnotLast != null))
							System.out.println(indent + "     - found annotation anchors (2) " + aAnnotFirst.getString() + "@" + aAnnotFirst.bounds + " and " + aAnnotLast.getString() + "@" + aAnnotLast.bounds);
					}
					if ((aAnnotFirst == null) || (aAnnotLast == null)) {
						for (int w = 0; w < words.length; w++) {
							if (words[w].bounds.right <= ancestor.bounds.left)
								continue;
							if (ancestor.bounds.right <= words[w].bounds.left)
								break;
							if (aAnnotFirst == null)
								aAnnotFirst = words[w];
							aAnnotLast = words[w];
						}
						if ((aAnnotFirst != null) && (aAnnotLast != null))
							System.out.println(indent + "     - found annotation anchors (3) " + aAnnotFirst.getString() + "@" + aAnnotFirst.bounds + " and " + aAnnotLast.getString() + "@" + aAnnotLast.bounds);
					}
					if ((aAnnotFirst == null) || (aAnnotLast == null)) {
						for (int w = 0; w < words.length; w++) {
							if (words[w].bounds.right <= ancestor.bounds.left)
								aAnnotFirst = words[w]; // find last word to left
							if (ancestor.bounds.right <= words[w].bounds.left) {
								aAnnotLast = words[w]; // find first word to right
								break;
							}
						}
						if ((aAnnotFirst != null) && (aAnnotLast != null))
							System.out.println(indent + "     - found annotation anchors (4) " + aAnnotFirst.getString() + "@" + aAnnotFirst.bounds + " and " + aAnnotLast.getString() + "@" + aAnnotLast.bounds);
					}
					if ((aAnnotFirst == null) || (aAnnotLast == null)) {
						aAnnotLast = words[0].getPreviousWord();
						aAnnotFirst = ancestor.getNextWord();
						if ((aAnnotFirst != null) && (aAnnotLast != null))
							System.out.println(indent + "     - found annotation anchors (5) " + aAnnotFirst.getString() + "@" + aAnnotFirst.bounds + " and " + aAnnotLast.getString() + "@" + aAnnotLast.bounds);
					}
					for (int aa = 0; aa < aAnnots.length; aa++) {
						boolean annotStartsAtWord = (aAnnots[aa].getFirstWord() == ancestor);
						boolean annotEndsAtWord = (aAnnots[aa].getLastWord() == ancestor);
						if (annotStartsAtWord && annotEndsAtWord) {
							if ((aAnnotFirst != null) && (aAnnotLast != null) && (aAnnotFirst.centerX <= aAnnotLast.centerX)) {
								aAnnots[aa].setFirstWord(aAnnotFirst);
								aAnnots[aa].setLastWord(aAnnotLast);
							}
							else this.target.document.removeAnnotation(aAnnots[aa]);
						}
						else if (annotStartsAtWord) {
							if (aAnnotFirst == null)
								this.target.document.removeAnnotation(aAnnots[aa]);
							else aAnnots[aa].setFirstWord(aAnnotFirst);
						}
						else if (annotEndsAtWord) {
							if (aAnnotLast == null)
								this.target.document.removeAnnotation(aAnnots[aa]);
							else aAnnots[aa].setLastWord(aAnnotLast);
						}
						else this.target.document.removeAnnotation(aAnnots[aa]); // something is hinky about this one ...
					}
				}
				this.page.removeWord(ancestor, true);
			}
			
			//	return end of current replacement sequence
			return firstPrev;
		}
		
		boolean mergeWords(OcrWordEditor first, OcrWordEditor last, boolean forCommit) {
			String mergedText = (first.getText() + last.getText());
			TokenSequence mTs = this.tokenizer.tokenize(mergedText);
			if (mTs.size() > 1)
				return false;
			OcrWordEditor merged;
			
			//	if both argument words have same parent (re-merge after split) and arguments are only children, we might want to revert instead
			if ((first.parents != null) && (last.parents != null) && (first.parents.length == 1) && (last.parents.length == 1) && (first.parents[0] == last.parents[0]) && (first.parents[0].childCount == 2)) {
				merged = first.parents[0];
				merged.childCount = 0;
				merged.setText(mergedText); // need to revert insertion of space or whatever incurred that split
//				System.out.println("Reverted split on merge");
			}
			
			//	actually merge words otherwise
			else {
				OcrWordEditor[] parents = {first, last};
				merged = new OcrWordEditor(this, mergedText, new BoundingBox(first.bounds.left, last.bounds.right, this.wordTop, this.wordBottom), first.wordFont, parents);
				int boldCharCount = ((first.bold ? first.getText().length() : 0) + (last.bold ? last.getText().length() : 0));
				merged.bold = ((boldCharCount * 2) > mergedText.length());
				int italicsCharCount = ((first.italics ? first.getText().length() : 0) + (last.italics ? last.getText().length() : 0));
				merged.italics = ((italicsCharCount * 2) > mergedText.length());
				if ((first.fontSize != -1) && (last.fontSize != -1))
					merged.fontSize = (((first.fontSize * first.getText().length()) + (last.fontSize * last.getText().length())) / (first.getText().length() + last.getText().length()));
				else if (first.fontSize != -1)
					merged.fontSize = first.fontSize;
				else if (last.fontSize != -1)
					merged.fontSize = last.fontSize;
				merged.nextRelation = last.nextRelation;
				merged.spaceAfter = last.spaceAfter;
			}
			
			//	integrate in word editor sequence and adjust position
			merged.textStreamPos = first.textStreamPos;
			if (first.previous != null) {
				merged.previous = first.previous;
				first.previous.next = merged;
			}
			if (last.next != null) {
				merged.next = last.next;
				last.next.previous = merged;
				for (OcrWordEditor we = merged.next; we != null; we = we.next)
					we.textStreamPos = (we.previous.textStreamPos + 1);
			}
			
			//	replace argument word editors with merged word editor
//			for (int w = (first.textStreamPos - this.first.getTextStreamPos()); w < this.words.size(); w++) {
			for (int w = 0; w < this.words.size(); w++) {
				OcrWordEditor we = ((OcrWordEditor) this.words.get(w));
				if (we == first)
					this.words.set(w, merged);
				else if (we == last) {
					this.words.remove(w--);
					break;
				}
			}
			
			//	no need to update display on commit
			if (forCommit)
				return true;
			
			//	re-compute layout and update font to get merged word integrated
			if (this.layoutRenderingDpi != -1) {
				int layoutRenderingDpi = this.layoutRenderingDpi;
				this.layoutRenderingDpi = -1;
				this.update(layoutRenderingDpi); // updates font when content changed (e.g. via arrow key navigation)
			}
			
			//	select merged word
			this.selectWord(merged, false, false, first.getText().length());
			return true;
		}
		
		void splitWord(OcrWordEditor word, boolean spaceSplit) {
			TokenSequence wordTokens = this.tokenizer.tokenize(word.getText());
			if (wordTokens.size() < 2)
				return;
//			
//			//	get width for each token at word font size
//			//	TODOne centralize this
//			Graphics2D g = getHelperGraphics();
//			String[] splitTokens = new String[wordTokens.size()];
//			boolean[] spaceAfterSplitToken = new boolean[wordTokens.size()];
//			float[] splitTokenWidths = new float[wordTokens.size()];
//			float splitTokenWidthSum = 0;
//			int spaceCount = 0;
//			for (int s = 0; s < splitTokens.length; s++) {
//				splitTokens[s] = wordTokens.valueAt(s);
//				spaceAfterSplitToken[s] = (((s+1) == splitTokens.length) ? word.spaceAfter : (wordTokens.getWhitespaceAfter(s).length() != 0));
//				TextLayout tl = new TextLayout(splitTokens[s], this.wordFont, g.getFontRenderContext());
//				splitTokenWidths[s] = ((float) tl.getBounds().getWidth());
//				splitTokenWidthSum += splitTokenWidths[s];
//				if (spaceAfterSplitToken[s])
//					spaceCount++;
//			}
//			TextLayout tl = new TextLayout(word.getText(), this.wordFont, g.getFontRenderContext());
//			float wordTokenSequenceWidth = ((float) tl.getBounds().getWidth());
//			float spaceWidth = ((spaceCount == 0) ? 0 : ((wordTokenSequenceWidth - splitTokenWidthSum) / spaceCount));
//			
//			//	compute bounding boxes for split result
//			//	TODOne centralize this
//			BoundingBox[] splitBoxes = new BoundingBox[splitTokens.length];
//			int splitTokenStart = word.bounds.left;
//			for (int s = 0; s < splitTokens.length; s++) {
//				int splitTokenWidth = Math.round((word.bounds.getWidth() * splitTokenWidths[s]) / wordTokenSequenceWidth);
//				boolean cutLeft = ((s != 0) && (splitTokenWidths[s-1] < splitTokenWidths[s]));
//				boolean cutRight = (((s + 1) != splitTokens.length) && (splitTokenWidths[s+1] < splitTokenWidths[s]));
//				splitBoxes[s] = new BoundingBox(
//						(splitTokenStart + (cutLeft ? 1 : 0)),
//						(((s + 1) == splitTokens.length) ? word.bounds.right : Math.min((splitTokenStart + splitTokenWidth - (cutRight ? 1 : 0)), word.bounds.right)),
//						word.bounds.top,
//						word.bounds.bottom
//					);
//				splitTokenStart += splitTokenWidth;
//				if (((s+1) < splitTokens.length) && (wordTokens.getWhitespaceAfter(s).length() != 0))
//					splitTokenStart += Math.round((word.bounds.getWidth() * spaceWidth) / wordTokenSequenceWidth);
//			}
			
			//	compute proportional split result
			TokenWord[] splitWords = tokenizeWord(word.getText(), word.bounds, wordTokens, this.wordFont);
			
			//	set up splitting
			OcrWordEditor first = null;
			OcrWordEditor last = null;
			
			//	if split result about corresponds to parents of argument word, we might want to revert a merger (re-distributes ancestor words)
//			if ((word.parents != null) && (word.parents.length == splitBoxes.length) && (spaceSplit ? (word.parents[0].spaceAfter == spaceAfterSplitToken[0]) : true)) {
			if ((word.parents != null) && (word.parents.length == splitWords.length) && (spaceSplit ? (word.parents[0].spaceAfter == splitWords[0].spaceAfter) : true)) {
				boolean onlyChild = true;
				int leftDistSum = 0;
				int widthDistSum = 0;
				for (int p = 0; p < word.parents.length; p++) {
					if (word.parents[p].childCount != 1) {
						onlyChild = false;
						break;
					}
//					leftDistSum += Math.abs(word.parents[p].bounds.left - splitBoxes[p].left);
					leftDistSum += Math.abs(word.parents[p].bounds.left - splitWords[p].bounds.left);
//					widthDistSum += Math.abs(word.parents[p].bounds.getWidth() - splitBoxes[p].getWidth());
					widthDistSum += Math.abs(word.parents[p].bounds.getWidth() - splitWords[p].bounds.getWidth());
				}
				//	TODO adjust thresholds
				if (onlyChild && ((leftDistSum * 10) < (word.bounds.getWidth() * 1)) && ((widthDistSum * 10) < (word.bounds.getWidth() * 1))) {
					first = word.parents[0];
					for (int p = 0; p < word.parents.length; p++) {
						word.parents[p].childCount = 0;
						if (first == null)
							first = word.parents[p];
						if (last != null) {
							word.parents[p].previous = last;
							last.next = word.parents[p];
						}
						last = word.parents[p];
					}
//					System.out.println("Reverted merge on split (left dists: " + leftDistSum + ", width dists: " + widthDistSum + ", overall: " + word.bounds.getWidth() + ")");
				}
			}
			
			//	create actual split result
			if ((first == null) && (last == null)) {
				OcrWordEditor[] parents = {word};
//				for (int s = 0; s < splitTokens.length; s++) {
				for (int s = 0; s < splitWords.length; s++) {
//					OcrWordEditor split = new OcrWordEditor(this, splitTokens[s], splitBoxes[s], this.wordFont, parents);
					OcrWordEditor split = new OcrWordEditor(this, splitWords[s].string, splitWords[s].bounds, this.wordFont, parents);
					split.bold = word.bold;
					split.italics = word.italics;
					split.fontSize = word.fontSize;
//					split.nextRelation = (((s+1) == splitTokens.length) ? word.nextRelation : ImWord.NEXT_RELATION_SEPARATE);
					split.nextRelation = (((s+1) == splitWords.length) ? word.nextRelation : ImWord.NEXT_RELATION_SEPARATE);
//					split.spaceAfter = (((s+1) == splitTokens.length) ? word.spaceAfter : spaceAfterSplitToken[s]);
					split.spaceAfter = (((s+1) == splitWords.length) ? word.spaceAfter : splitWords[s].spaceAfter);
					
					if (first == null) {
						first = split;
						split.textStreamPos = word.textStreamPos;
					}
					if (last != null) {
						split.previous = last;
						last.next = split;
						split.textStreamPos = (last.textStreamPos + 1);
					}
					last = split;
//					System.out.println("     - part " + split.getText() + " at " + split.bounds);
				}
			}
			
			if (word.previous != null) {
				first.previous = word.previous;
				word.previous.next = first;
			}
			if (word.next != null) {
				last.next = word.next;
				word.next.previous = last;
				for (OcrWordEditor we = last.next; we != null; we = we.next)
					we.textStreamPos = (we.previous.textStreamPos + 1);
			}
			
			//	replace argument word editor with split word editors
//			for (int w = (first.textStreamPos - this.first.getTextStreamPos()); w < this.words.size(); w++) {
			for (int w = 0; w < this.words.size(); w++) {
				OcrWordEditor we = ((OcrWordEditor) this.words.get(w));
				if (we == word) {
					this.words.set(w++, first);
					for (we = first.next; we != null; we = we.next) {
						this.words.add(w++, we);
						if (we == last)
							break;
					}
					break;
				}
			}
			
			//	re-compute layout and update font to get merged word integrated
			if (this.layoutRenderingDpi != -1) {
				int layoutRenderingDpi = this.layoutRenderingDpi;
				this.layoutRenderingDpi = -1;
				this.update(layoutRenderingDpi); // updates font when content changed (e.g. via arrow key navigation)
			}
			
			//	select start of last split result word (where caret would be in word processor after hitting space or inserting something else incurring a split)
			this.selectWord(last, false, false, 0);
		}
	}
	
	private static class AncestorSet extends LinkedHashSet {
//		boolean containsAny(Object[] ancestors) {
//			for (int a = 0; a < ancestors.length; a++) {
//				if (this.contains(ancestors[a]))
//					return true;
//			}
//			return false;
//		}
		boolean isDisjoint(Object[] ancestors) {
			for (int a = 0; a < ancestors.length; a++) {
				if (this.contains(ancestors[a]))
					return false;
			}
			return true;
		}
//		boolean containsAny(Collection c) {
//			for (Iterator it = c.iterator(); it.hasNext();) {
//				if (this.contains(it.next()))
//					return true;
//			}
//			return false;
//		}
		boolean addAll(Object[] ancestors) {
			return this.addAll(Arrays.asList(ancestors));
		}
	}
	
	private static class OcrWordEditor extends JTextField implements DocumentListener, ImagingConstants {
		OcrLineEditor parent;
		ImWord word;
		
		ImWord[] ancestors;
		OcrWordEditor[] parents;
		int childCount = 0;
		
		String oText;
		int textStreamPos;
		boolean bold;
		boolean italics;
		int fontSize = -1;
		char nextRelation;
		boolean spaceAfter;
		BoundingBox bounds;
		
		OcrWordEditor previous;
		OcrWordEditor next;
		
		Font wordFont;
		Point posInParent;
		Dimension sizeInParent;
		int minWidhtInParent;
		
		OcrWordEditor(OcrLineEditor parent, ImWord word) {
			this(parent, word.getString(), word.bounds);
			this.wordFont = this.getFont(); // for starters ...
			
			this.textStreamPos = word.getTextStreamPos();
			this.bold = word.hasAttribute(BOLD_ATTRIBUTE);
			this.italics = word.hasAttribute(ITALICS_ATTRIBUTE);
			this.fontSize = word.getFontSize();
			this.nextRelation = word.getNextRelation();
			if (word == this.parent.last)
				this.spaceAfter = true;
			else {
				ImWord next = word.getNextWord();
				if (next == null)
					this.spaceAfter = true;
				else {
					int wordDist = (next.bounds.left - word.bounds.right);
					int wordHeight = Math.max(word.bounds.getHeight(), next.bounds.getHeight());
					this.spaceAfter= ((wordHeight * 4) <= (wordDist * 20)); // 20% should be OK as an estimate lower bound, and with some safety margin (0.25 is smallest defaulting space width in born-digital text, and scanned text tends to be spaced more lavishly)
				}
			}
			
			this.word = word;
			this.ancestors = new ImWord[1];
			this.ancestors[0] = word;
			this.parents = null;
		}
		
		OcrWordEditor(OcrLineEditor parent, String text, BoundingBox bounds, Font wordFont, OcrWordEditor[] parents) {
			this(parent, text, bounds);
			this.wordFont = wordFont;
			LinkedHashSet ancestors = new LinkedHashSet();
			for (int p = 0; p < parents.length; p++) {
				ancestors.addAll(Arrays.asList(parents[p].ancestors));
				parents[p].childCount++;
			}
			this.ancestors = ((ImWord[]) ancestors.toArray(new ImWord[ancestors.size()]));
			this.parents = parents;
		}
		
		private OcrWordEditor(OcrLineEditor parent, String text, BoundingBox bounds) {
			super(text);
			final AbstractDocument doc = ((AbstractDocument) this.getDocument());
			doc.setDocumentFilter(new DocumentFilter() {
				public void insertString(FilterBypass fb, int offset, String string, AttributeSet attrs) throws BadLocationException {
					this.replace(fb, offset, 0, string, attrs);
				}
				public void replace(FilterBypass fb, int offset, int length, String string, AttributeSet attrs) throws BadLocationException {
					String fString = this.filterString(offset, length, string);
//					System.out.println("Replacing " + length + " characters with '" + string + "' ==> '" + fString + "' at " + offset);
					if ((fString.length() == 0) && (length == 0))
						Toolkit.getDefaultToolkit().beep();
					else super.replace(fb, offset, length, fString, attrs);
				}
				private String filterString(int offset, int length, String string) {
					string = string.replaceAll("\\s+", " "); // replace multi-spaces with single space (e.g. on paste), also normalizing tabs, etc.
					if (string.length() == 0)
						return string;
					if ((offset == 0) && string.startsWith(" "))
						string = string.substring(" ".length());
					if (((offset + length) == doc.getLength()) && string.endsWith(" "))
						string = string.substring(0, (string.length() - " ".length()));
					return string;
				}
			});
			
			this.parent = parent;
			this.oText = text;
			
			this.setHorizontalAlignment(CENTER);
			this.setBackground(this.parent.wordBackground);
			this.setBorder(BorderFactory.createLineBorder(this.parent.textStreamColor));
			this.bounds = bounds;
			this.posInParent = new Point((this.bounds.left - this.parent.first.bounds.left), 0);
			this.sizeInParent = new Dimension(this.bounds.getWidth(), this.bounds.getHeight());
			//	TODO maybe use matte border instead, setting left and/or right to background if words connected
			this.addFocusListener(new FocusAdapter() {
				public void focusGained(FocusEvent fe) {
//					System.out.println("Focus gained on " + getText() + ", params are " + fe.paramString());
					OcrWordEditor.this.parent.selectWord(OcrWordEditor.this, false, false, -1);
				}
			});
			this.getDocument().addDocumentListener(this);
		}
		
		void update(Font font, int inPageHeight, int inPageMinWidth) {
			this.wordFont = font;
			this.updateFont();
			this.sizeInParent.height = inPageHeight;
			this.minWidhtInParent = inPageMinWidth;
		}
		
		void setBold(boolean bold) {
			this.bold = bold;
			this.updateFont();
		}
		
		void setItalics(boolean italics) {
			this.italics = italics;
			this.updateFont();
		}
		
		private void updateFont() {
			Font font = this.wordFont;
			int style = 0;
			if (this.bold)
				style |= Font.BOLD;
			if (this.italics)
				style |= Font.ITALIC;
			if (style != 0)
				font = font.deriveFont(style);
			
			Graphics2D g = getHelperGraphics();
			TextLayout tl = new TextLayout(((this.getText().length() == 0) ? this.oText : this.getText()), font, g.getFontRenderContext());
			Insets i = this.getInsets();
			int textWidth = (this.getWidth() - i.left - i.right);
			if (tl.getBounds().getWidth() > textWidth)
				font = font.deriveFont(AffineTransform.getScaleInstance((textWidth / tl.getBounds().getWidth()), 1));
			g.dispose();
			
			this.setFont(font);
		}
		
		public void insertUpdate(DocumentEvent de) {
			int offset = de.getOffset();
			int length = de.getLength();
			boolean space;
			if (offset == 0)
				space = false;
			else if (length != 1)
				space = false;
			else if ((offset + length) == de.getDocument().getLength())
				space = false;
			else try {
				String inserted = de.getDocument().getText(offset, length);
				space = (inserted.charAt(0) == ' ');
			}
			catch (BadLocationException ble) {
				space = false;
			}
			this.parent.splitWord(this, space);
		}
		public void removeUpdate(DocumentEvent de) {
			//	TODO_NOT remove word altogether if text empty ==> since we cannot properly _insert_ we cannot afford losing bounding box, so we do this only on commit (if string is empty then)
		}
		public void changedUpdate(DocumentEvent de) {}
		
		void commitChanges() {
			if (this.word == null)
				return;
			
			this.word.setString(this.getText());
			if (this.bold)
				this.word.setAttribute(BOLD_ATTRIBUTE);
			else this.word.removeAttribute(BOLD_ATTRIBUTE);
			if (this.italics)
				this.word.setAttribute(ITALICS_ATTRIBUTE);
			else this.word.removeAttribute(ITALICS_ATTRIBUTE);
			if (this.parent.baseline != -1)
				this.word.setAttribute(BASELINE_ATTRIBUTE, ("" + this.parent.baseline));
			if (this.fontSize != -1)
				this.word.setFontSize(this.fontSize);
			this.word.setNextRelation(this.nextRelation);
		}
	}
	
	private static ImWord findFirstLineWord(ImWord word) {
		ImWord first = word;
		for (ImWord imw = word.getPreviousWord(); imw != null; imw = imw.getPreviousWord()) {
			if (imw.pageId != word.pageId)
				break;
			if (imw.bounds.bottom <= word.bounds.top)
				break;
			if (word.bounds.bottom <= imw.bounds.top)
				break;
			if (imw.bounds.right > first.centerX)
				break;
			first = imw;
		}
		return first;
	}
	
	private static ImWord findLastLineWord(ImWord word) {
		ImWord last = word;
		for (ImWord imw = word.getNextWord(); imw != null; imw = imw.getNextWord()) {
			if (imw.pageId != word.pageId)
				break;
			if (imw.bounds.bottom <= word.bounds.top)
				break;
			if (word.bounds.bottom <= imw.bounds.top)
				break;
			if (imw.bounds.left < last.centerX)
				break;
			last = imw;
		}
		return last;
	}
	
	private static class WordEditTools extends JPanel implements KeyListener {
		OcrLineEditor parent;
		OcrWordEditor target = null;
		int targetCenterX = -1;
		JCheckBox bold = new JCheckBox("Bold");
		JCheckBox italics = new JCheckBox("Italics");
		JButton symbolTable = new JButton("Symbols");
		SymbolTable.Owner symbolTableOwner;
		
		WordEditTools(OcrLineEditor parent) {
			super(new GridLayout(1, 0), true);
			this.parent = parent;
			
			this.bold.setToolTipText("Bold");
			this.bold.setBackground(Color.WHITE);
			this.bold.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 2), BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1)));
			this.bold.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					if (target != null)
						target.setBold(bold.isSelected());
				}
			});
			
			this.italics.setToolTipText("Italics");
			this.italics.setBackground(Color.WHITE);
			this.italics.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 2), BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1)));
			this.italics.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					if (target != null)
						target.setItalics(italics.isSelected());
				}
			});
			
			this.symbolTable.setToolTipText("Open symbol table");
			this.symbolTable.setBackground(Color.WHITE);
			this.symbolTable.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 2), BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1)));
			this.symbolTable.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					openSymbolTable();
				}
			});
			this.symbolTableOwner = new SymbolTable.Owner() {
				public void useSymbol(char symbol) {
					if (target != null) try {
						int caretPos = target.getCaretPosition();
						target.getDocument().insertString(caretPos, ("" + symbol), null);
						target.setCaretPosition(caretPos + 1); // move caret after inserted symbol
					}
					catch (BadLocationException ble) {
						ble.printStackTrace();
					}
					focusTargetDeferred();
				}
				public Point getLocation() {
					return WordEditTools.this.parent.getLocationOnScreen();
				}
				public Dimension getSize() {
					return WordEditTools.this.parent.getSize();
				}
				public Color getColor() {
					return WordEditTools.this.parent.textStreamColor;
				}
				public void symbolTableClosed() {
					WordEditTools.this.parent.symbolTable = null;
				}
			};
			
			this.add(this.bold);
			this.add(this.italics);
			this.add(this.symbolTable);
			this.getLayout().layoutContainer(this); // need to compute preferred size
		}
		
		void setTextStreamColor(Color textStreamColor) {
			this.bold.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 2), BorderFactory.createLineBorder(textStreamColor, 1)));
			this.italics.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 2), BorderFactory.createLineBorder(textStreamColor, 1)));
			this.symbolTable.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 2), BorderFactory.createLineBorder(textStreamColor, 1)));
		}
		
		void setTarget(OcrWordEditor target, int targetCenterX) {
			if (this.target != null) {
				this.target.removeKeyListener(this);
				this.target = null;
				this.targetCenterX = -1;
			}
			
			if (target == null)
				return;
			this.bold.setSelected(target.bold);
			this.italics.setSelected(target.italics);
			
			this.target = target;
			this.target.addKeyListener(this);
			this.targetCenterX = targetCenterX;
		}
		
		void openSymbolTable() {
			if (this.parent.symbolTable == null) {
				this.parent.symbolTable = SymbolTable.getSharedSymbolTable();
				this.parent.symbolTable.setOwner(this.symbolTableOwner);
			}
			this.parent.symbolTable.open();
			//	TODO add direct hex code entry to symbol table (will save a ton of scrolling) !!!
			this.focusTargetDeferred();
		}
		
		void focusTargetDeferred() {
			if (this.target == null)
				return;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (target != null)
						target.requestFocus(); // need to get focus back from symbol table dialog
				}
			});
		}
		
		private void moveSymbolTableDeferred() {
			if (this.parent.symbolTable == null)
				return;
			//	TODO do we need this tag-along behavior ??? ==> ask Jeremy !!!
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (parent.symbolTable != null)
						parent.symbolTable.updateOwner();
				}
			});
		}
		
		public void keyPressed(KeyEvent ke) {
			if (ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
				ke.consume();
				this.parent.close(true);
			}
			else if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
				ke.consume();
				this.parent.close(false);
			}
			else if (ke.getKeyCode() == KeyEvent.VK_UP) {
				if (this.parent.first.getPreviousWord() == null)
					return; // nowhere to go
				if (this.parent.target == null)
					return; // no navigating in dialog mode
				ke.consume();
				this.parent.commitChanges();
				ImWord upLast = this.parent.first.getPreviousWord();
				ImWord upFirst = findFirstLineWord(upLast);
				int targetCaretX = ((this.target.getText().length() == 0) ? this.targetCenterX : (this.target.bounds.left + ((this.target.bounds.getWidth() * this.target.getCaretPosition()) / this.target.getText().length())));
				int upCaretX;
				//	we have actual overlap, find word and caret position using absolute position
				if ((upFirst.bounds.left < this.parent.last.bounds.right) && (this.parent.first.bounds.left < upLast.bounds.right))
					upCaretX = targetCaretX;
				//	no actual overlap, find word and caret position via relative computation
				else upCaretX = (upFirst.bounds.left + (((upLast.bounds.right - upFirst.bounds.left) * (targetCaretX - this.parent.first.bounds.left)) / (this.parent.last.bounds.right - this.parent.first.bounds.left)));
				ImWord selWord = null;
				int selWordCaretPos = -1;
				int selWordDist = Integer.MAX_VALUE;
				for (ImWord imw = upFirst; imw != null; imw = imw.getNextWord()) {
					if ((imw.bounds.left <= upCaretX) && (upCaretX < imw.bounds.right)) {
						selWord = imw;
						selWordCaretPos = ((imw.getString().length() * (upCaretX - imw.bounds.left)) / imw.bounds.getWidth());
						break; // no need to look any further, we found the one word
					}
					else if (upCaretX < imw.bounds.left) {
						int imwDist = (imw.bounds.left - upCaretX);
						if (imwDist < selWordDist) {
							selWord = imw;
							selWordCaretPos = 0;
							selWordDist = imwDist;
						}
					}
					else if (imw.bounds.right <= upCaretX) {
						int imwDist = (upCaretX - imw.bounds.right);
						if (imwDist < selWordDist) {
							selWord = imw;
							selWordCaretPos = imw.getString().length();
							selWordDist = imwDist;
						}
					}
					if (imw == upLast)
						break;
				}
				if (selWord == null)
					selWord = upLast;
				this.parent.setWords(upFirst.getPage(), upFirst, upLast, selWord, selWordCaretPos, false);
				this.parent.target.setDisplayOverlay(this.parent, upFirst.pageId);
				this.moveSymbolTableDeferred();
			}
			else if (ke.getKeyCode() == KeyEvent.VK_DOWN) {
				if (this.parent.last.getNextWord() == null)
					return; // nowhere to go
				if (this.parent.target == null)
					return; // no navigating in dialog mode
				ke.consume();
				this.parent.commitChanges();
				ImWord downFirst = this.parent.last.getNextWord();
				ImWord downLast = findLastLineWord(downFirst);
				int targetCaretX = ((this.target.getText().length() == 0) ? this.targetCenterX : (this.target.bounds.left + ((this.target.bounds.getWidth() * this.target.getCaretPosition()) / this.target.getText().length())));
				int downCaretX;
				//	we have actual overlap, find word and caret position using absolute position
				if ((downFirst.bounds.left < this.parent.last.bounds.right) && (this.parent.first.bounds.left < downLast.bounds.right))
					downCaretX = targetCaretX;
				//	no actual overlap, find word and caret position via relative computation
				else downCaretX = (downFirst.bounds.left + (((downLast.bounds.right - downFirst.bounds.left) * (targetCaretX - this.parent.first.bounds.left)) / (this.parent.last.bounds.right - this.parent.first.bounds.left)));
				ImWord selWord = null;
				int selWordCaretPos = -1;
				int selWordDist = Integer.MAX_VALUE;
				for (ImWord imw = downFirst; imw != null; imw = imw.getNextWord()) {
					if ((imw.bounds.left <= downCaretX) && (downCaretX < imw.bounds.right)) {
						selWord = imw;
						selWordCaretPos = ((imw.getString().length() * (downCaretX - imw.bounds.left)) / imw.bounds.getWidth());
						break; // no need to look any further, we found the one word
					}
					else if (downCaretX < imw.bounds.left) {
						int imwDist = (imw.bounds.left - downCaretX);
						if (imwDist < selWordDist) {
							selWord = imw;
							selWordCaretPos = 0;
							selWordDist = imwDist;
						}
					}
					else if (imw.bounds.right <= downCaretX) {
						int imwDist = (downCaretX - imw.bounds.right);
						if (imwDist < selWordDist) {
							selWord = imw;
							selWordCaretPos = imw.getString().length();
							selWordDist = imwDist;
						}
					}
					if (imw == downLast)
						break;
				}
				if (selWord == null)
					selWord = downFirst;
				this.parent.setWords(downFirst.getPage(), downFirst, downLast, selWord, selWordCaretPos, false);
				this.parent.target.setDisplayOverlay(this.parent, downFirst.pageId);
				this.moveSymbolTableDeferred();
			}
			else if (ke.getKeyCode() == KeyEvent.VK_LEFT) {
				if (this.target.getCaretPosition() > 0)
					return; // staying inside field
				ke.consume();
				boolean startOfWord = ke.isControlDown();
				if (this.target.previous == null) {
					if (this.parent.first.getPreviousWord() == null)
						return; // nowhere to go
					if (this.parent.target == null)
						return; // no navigating in dialog mode
					this.parent.commitChanges();
					ImWord upLast = this.parent.first.getPreviousWord();
					ImWord upFirst = findFirstLineWord(upLast);
					this.parent.setWords(upFirst.getPage(), upFirst, upLast, upLast, (startOfWord ? 0 : -1), !startOfWord);
					this.parent.target.setDisplayOverlay(this.parent, upFirst.pageId);
					this.moveSymbolTableDeferred();
				}
				else this.parent.selectWord(target.previous, false, !startOfWord, (startOfWord ? 0 : -1));
			}
			else if (ke.getKeyCode() == KeyEvent.VK_RIGHT) {
				boolean startOfWord = ke.isControlDown();
				if (!startOfWord && (this.target.getCaretPosition() < this.target.getText().length()))
					return; // staying inside field
				ke.consume();
				if (this.target.next == null) {
					if (this.parent.last.getNextWord() == null)
						return; // nowhere to go
					if (this.parent.target == null)
						return; // no navigating in dialog mode
					this.parent.commitChanges();
					ImWord downFirst = this.parent.last.getNextWord();
					ImWord downLast = findLastLineWord(downFirst);
					this.parent.setWords(downFirst.getPage(), downFirst, downLast, downFirst, (startOfWord ? 0 : -1), !startOfWord);
					this.parent.target.setDisplayOverlay(this.parent, downFirst.pageId);
					this.moveSymbolTableDeferred();
				}
				else this.parent.selectWord(this.target.next, !startOfWord, false, (startOfWord ? 0 : -1));
			}
			else if (ke.getKeyCode() == KeyEvent.VK_TAB) {
				ke.consume();
				System.out.println("Tab key-down consumed");
				//	TODO somehow get a handle on TAB
			}
			else if (ke.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
				if (this.target.getCaretPosition() > 0)
					return; // staying inside field
				if (this.target.getSelectionStart() != this.target.getSelectionEnd())
					return; // selection deletion, if at start
				if (this.target.previous == null)
					return; // leave the beeping to default implementation
				ke.consume();
				this.parent.mergeWords(this.target.previous, this.target, false);
			}
			else if (ke.getKeyCode() == KeyEvent.VK_DELETE) {
				if (this.target.getCaretPosition() < this.target.getText().length())
					return; // staying inside field
				if (this.target.getSelectionStart() != this.target.getSelectionEnd())
					return; // selection deletion, if at end
				if (this.target.next == null)
					return; // leave the beeping to default implementation
				ke.consume();
				this.parent.mergeWords(this.target, this.target.next, false);
			}
			else if (ke.isControlDown()) {
				if (ke.getKeyCode() == KeyEvent.VK_I)
					this.italics.setSelected(!italics.isSelected());
				else if (ke.getKeyCode() == KeyEvent.VK_B)
					this.bold.setSelected(!bold.isSelected());
			}
		}
		public void keyReleased(KeyEvent ke) {}
		public void keyTyped(KeyEvent ke) {}
	}
}
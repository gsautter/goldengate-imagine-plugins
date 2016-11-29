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
 *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
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
import java.awt.Font;
import java.awt.Point;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImFont;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.analysis.Imaging;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider;
import de.uka.ipd.idaho.im.ocr.OcrEngine;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * Provider of basic word edits, mostly for OCRed documents.
 * 
 * @author sautter
 */
public class WordActionProvider extends AbstractSelectionActionProvider {
	
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
		if (selectedWords.length != 0) {
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
			if ((selectedBox.bottom - selectedBox.top) <= page.getImageDPI())
				actions.add(new SelectionAction("wordsMark", "Mark Word", "Mark selected area as a word.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						
						//	get merged word, and offer editing
						ImWord mWord = getMergedWord(selectedWords, page, idmp.document);
						if (!idmp.editWord(mWord, true))
							return false;
						
						//	get tokenizer to check and split words with multiple tokens
						Tokenizer tokenizer = ((Tokenizer) idmp.document.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER));
						ImWord[] mWords = tokenizeWord(mWord, tokenizer);
						
						//	remove words
						ImUtils.orderStream(selectedWords, ImUtils.leftRightTopDownOrder);
						Arrays.sort(selectedWords, ImUtils.textStreamOrder);
						
						//	remember start predecessor and end successor
						ImWord startPrev = selectedWords[0].getPreviousWord();
						ImWord endNext = selectedWords[selectedWords.length-1].getNextWord();
						
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
						
						//	connect previous and next words to preserve annotations
						if (startPrev != null)
							startPrev.setNextWord(endNext);
						else if (endNext != null)
							endNext.setPreviousWord(startPrev);
						
						//	cut merged words out of streams
						selectedWords[selectedWords.length-1].setNextWord(null);
						selectedWords[0].setPreviousWord(null);
						for (int w = 0; w < selectedWords.length; w++)
							selectedWords[0].setNextWord(null);
						
						//	integrate merged word in document
						for (int w = 0; w < mWords.length; w++) {
							page.addWord(mWords[w]);
							if (w == 0)
								mWords[w].setPreviousWord(startPrev);
							else mWords[w].setPreviousWord(mWords[w-1]);
						}
						mWords[mWords.length-1].setNextWord(endNext);
						for (int a = 0; a < startingAnnots.size(); a++)
							((ImAnnotation) startingAnnots.get(a)).setFirstWord(mWords[0]);
						for (int a = 0; a < endingAnnots.size(); a++)
							((ImAnnotation) endingAnnots.get(a)).setLastWord(mWords[mWords.length-1]);
						
						//	clean up merged words
						for (int w = 0; w < selectedWords.length; w++)
							page.removeWord(selectedWords[w], true);
						
						//	we _did_ change something, quite something
						return true;
					}
				});
		}
		
		//	offer marking word 'in the wild' if dimensions make sense
		else if ((selectedBox.bottom - selectedBox.top) <= page.getImageDPI())
			actions.add(new SelectionAction("wordsMark", "Mark Word", "Mark selected area as a word.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return markWord(page, selectedBox, idmp);
				}
			});
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
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
		
		//	multiple words selected
		if (start != end) {
			
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
			
			//	offer removing words
			actions.add(new SelectionAction("wordsRemove", "Remove Words", "Remove selected words.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return removeWords(start, end);
				}
			});
		}
		
		//	single word selected
		if (start == end) {
			
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
					Font font = new Font("Serif", fontStyle, Integer.parseInt(((String) start.getAttribute(ImWord.FONT_SIZE_ATTRIBUTE, "24"))));
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
					int startWidth = (start.bounds.right - start.bounds.left);
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
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	private boolean markWord(ImPage page, BoundingBox selectedBox, ImDocumentMarkupPanel idmp) {
		
		//	start with original selection
		int eLeft = selectedBox.left;
		int eRight = selectedBox.right;
		int eTop = selectedBox.top;
		int eBottom = selectedBox.bottom;
		
		//	get region coloring of selected rectangle
		boolean changed;
		boolean[] originalSelectionRegionInCol;
		boolean[] originalSelectionRegionInRow;
		PageImage pageImage = page.getPageImage();
		do {
			
			//	get region coloring for current rectangle
			int[][] regionColors = Imaging.getRegionColoring(Imaging.wrapImage(pageImage.image.getSubimage(eLeft, eTop, (eRight - eLeft), (eBottom - eTop)), null), ((byte) 120), true);
			
			//	check which region colors occur in original selection
			boolean[] regionColorInOriginalSelection = new boolean[this.getMaxRegionColor(regionColors)];
			Arrays.fill(regionColorInOriginalSelection, false);
			for (int c = Math.max((selectedBox.left - eLeft), 0); c < Math.min((selectedBox.right - eLeft), regionColors.length); c++)
				for (int r = Math.max((selectedBox.top - eTop), 0); r < Math.min((selectedBox.bottom - eTop), regionColors[c].length); r++) {
					if (regionColors[c][r] != 0)
						regionColorInOriginalSelection[regionColors[c][r]-1] = true;
				}
			
			//	assess which columns and rows contain regions that overlap original selection
			originalSelectionRegionInCol = new boolean[regionColors.length];
			Arrays.fill(originalSelectionRegionInCol, false);
			originalSelectionRegionInRow = new boolean[regionColors[0].length];
			Arrays.fill(originalSelectionRegionInRow, false);
			for (int c = 0; c < regionColors.length; c++) {
				for (int r = 0; r < regionColors[c].length; r++)
					if (regionColors[c][r] != 0) {
						originalSelectionRegionInCol[c] = (originalSelectionRegionInCol[c] || regionColorInOriginalSelection[regionColors[c][r]-1]);
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
			if (originalSelectionRegionInRow[0] && (eTop != 0)) {
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
			if (originalSelectionRegionInRow[originalSelectionRegionInRow.length-1] && (eBottom != (pageImage.image.getHeight()-1))) {
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
				return false;
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
		BoundingBox wordBox = new BoundingBox(eLeft, eRight, eTop, eBottom);
		int charCount = 0;
		int boldCharCount = 0;
		int italicsCharCount = 0;
		
		//	collect strings and bold and italic properties from words overlapping selection, and find predecessor and successor
		ImWord[] pWords = page.getWords();
		ArrayList opWords = new ArrayList();
		Arrays.sort(pWords, ImUtils.textStreamOrder);
		ImWord prevWord = null;
		ImWord nextWord = null;
		boolean gotParagraphEnd = false;
		for (int w = 0; w < pWords.length; w++) {
			if (!wordBox.includes(pWords[w].bounds, true) && !pWords[w].bounds.includes(wordBox, true))
				continue;
			opWords.add(pWords[w]);
			if (prevWord == null)
				prevWord = pWords[w].getPreviousWord();
			nextWord = pWords[w].getNextWord();
			charCount += pWords[w].getString().length();
			if (pWords[w].hasAttribute(ImWord.BOLD_ATTRIBUTE))
				boldCharCount += pWords[w].getString().length();
			if (pWords[w].hasAttribute(ImWord.ITALICS_ATTRIBUTE))
				italicsCharCount += pWords[w].getString().length();
			if (pWords[w].getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
				gotParagraphEnd = true;
		}
		
		//	do OCR (we only ever get here if no words are selected)
		OcrEngine ocr = imagineParent.getOcrEngine();
		StringBuffer wordString = new StringBuffer();
		if (ocr != null) try {
			ImDocument ocrDoc = new ImDocument(idmp.document.docId);
			ImPage ocrPage = new ImPage(ocrDoc, page.pageId, page.bounds);
			ImRegion ocrRegion = new ImRegion(ocrPage, wordBox, ImagingConstants.REGION_ANNOTATION_TYPE);
			ocr.doBlockOcr(ocrRegion, pageImage, null);
			ImWord[] ocrWords = ocrRegion.getWords();
			for (int w = 0; w < ocrWords.length; w++) {
				wordString.append(ocrWords[w].getString());
				if (ocrWords[w].hasAttribute(ImWord.BOLD_ATTRIBUTE))
					boldCharCount += ocrWords[w].getString().length();
				if (ocrWords[w].hasAttribute(ImWord.ITALICS_ATTRIBUTE))
					italicsCharCount += ocrWords[w].getString().length();
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
		
		//	create word
		ImWord word = new ImWord(idmp.document, page.pageId, wordBox, wordString.toString());
		if (charCount < (boldCharCount * 2))
			word.setAttribute(ImWord.BOLD_ATTRIBUTE);
		if (charCount < (italicsCharCount * 2))
			word.setAttribute(ImWord.ITALICS_ATTRIBUTE);
		
		//	prompt user
		if (!idmp.editWord(word, true) || (word.getString().trim().length() == 0))
			return false;
		
		//	compute baseline
		boolean gotAscent = false;
		boolean gotDescent = false;
		for (int c = 0; c < word.getString().length(); c++) {
			char ch = word.getString().charAt(c);
			char bch = StringUtils.getBaseChar(ch);
			gotAscent = (gotAscent || Character.isUpperCase(bch) || Character.isDigit(bch) || ("bdfhijklt".indexOf(bch) != -1) || ("°!§$%&/(){}[]?#'|\\\"*€".indexOf(bch) != -1) || (ch != bch));
			gotDescent = (gotDescent || ("gjqy".indexOf(bch) != -1) || ((bch == 'f') && word.hasAttribute(ImWord.ITALICS_ATTRIBUTE)));
		}
		int baseline = word.bounds.bottom;
		if (gotDescent)
			baseline -= ((word.bounds.bottom - word.bounds.top) / 4);
		word.setAttribute(ImWord.BASELINE_ATTRIBUTE, ("" + baseline));
		
		//	get tokenizer to check and split words with multiple tokens
		Tokenizer tokenizer = ((Tokenizer) idmp.document.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER));
		ImWord[] sWords = this.tokenizeWord(word, tokenizer);
		
		//	add word(s) to document
		for (int w = 0; w < sWords.length; w++) {
			page.addWord(sWords[w]);
			if (w != 0)
				sWords[w-1].setNextWord(sWords[w]);
		}
		
		//	cut out overwritten words
		if (prevWord != null)
			prevWord.setNextWord(nextWord);
		else if (nextWord != null)
			nextWord.setPreviousWord(prevWord);
		
		//	integrate in text streams
		if (prevWord != null)
			sWords[0].setPreviousWord(prevWord);
		if (nextWord != null)
			sWords[sWords.length-1].setNextWord(nextWord);
		if (gotParagraphEnd)
			sWords[sWords.length-1].setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
		
		//	clean up overwritten words
		for (int w = 0; w < opWords.size(); w++)
			page.removeWord(((ImWord) opWords.get(w)), true);
		
		//	we _did_ change something, quite something
		return true;
	}
	
	private int getMaxRegionColor(int[][] regionColors) {
		int maxRegionColor = 0;
		for (int c = 0; c < regionColors.length; c++) {
			for (int r = 0; r < regionColors[c].length; r++)
				maxRegionColor = Math.max(maxRegionColor, regionColors[c][r]);
		}
		return maxRegionColor;
	}
	
	private ImWord[] tokenizeWord(ImWord word, Tokenizer tokenizer) {
		ArrayList wordList = new ArrayList();
		TokenSequence wordTokens = tokenizer.tokenize(word.getString());
		
		if (wordTokens.size() == 1)
			wordList.add(word);
		else {
			System.out.println("   - splitting " + word.getString() + " at " + word.bounds + " into " + wordTokens.size() + " parts");
			
			//	get width for each token at word font size
			String[] splitTokens = new String[wordTokens.size()];
			float[] splitTokenWidths = new float[wordTokens.size()];
			Font wordFont = new Font("Serif", Font.BOLD, 24);
			float splitTokenWidthSum = 0;
			for (int s = 0; s < splitTokens.length; s++) {
				splitTokens[s] = wordTokens.valueAt(s);
				TextLayout tl = new TextLayout(splitTokens[s], wordFont, new FontRenderContext(null, false, true));
				splitTokenWidths[s] = ((float) tl.getBounds().getWidth());
				splitTokenWidthSum += splitTokenWidths[s];
			}
			
			//	store split result, splitting word bounds accordingly
			int wordWidth = (word.bounds.right - word.bounds.left);
			int splitTokenStart = word.bounds.left;
			for (int s = 0; s < splitTokens.length; s++) {
				int splitTokenWidth = Math.round((wordWidth * splitTokenWidths[s]) / splitTokenWidthSum);
				boolean cutLeft = ((s != 0) && (splitTokenWidths[s-1] < splitTokenWidths[s]));
				boolean cutRight = (((s + 1) != splitTokens.length) && (splitTokenWidths[s+1] < splitTokenWidths[s]));
				BoundingBox sWordBox = new BoundingBox(
						(splitTokenStart + (cutLeft ? 1 : 0)),
						Math.min((splitTokenStart + splitTokenWidth - (cutRight ? 1 : 0)), word.bounds.right),
						word.bounds.top,
						word.bounds.bottom
					);
				ImWord sWord = new ImWord(word.getDocument(), word.pageId, sWordBox, wordTokens.valueAt(s));
				sWord.copyAttributes(word);
				wordList.add(sWord);
				System.out.println("     - part " + sWord.getString() + " at " + sWord.bounds);
				splitTokenStart += splitTokenWidth;
			}
		}
		
		//	finally ...
		return ((ImWord[]) wordList.toArray(new ImWord[wordList.size()]));
	}
	
	private boolean removeWords(ImWord start, ImWord end) {
		
		//	cut out selected words
		ImWord startPrev = start.getPreviousWord();
		ImWord endNext = end.getNextWord();
		if (startPrev != null)
			startPrev.setNextWord(endNext);
		else if (endNext != null)
			endNext.setPreviousWord(startPrev);
		
		//	remove words, but dissolve stream first, back to front
		ArrayList words = new ArrayList(5);
		for (ImWord imw = end; imw != null; imw = imw.getPreviousWord()) {
			words.add(imw);
			imw.setNextWord(null);
		}
		ImPage page = start.getPage();
		for (int w = 0; w < words.size(); w++)
			page.removeWord(((ImWord) words.get(w)), true);
		
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
				int wFontSize = Integer.parseInt((String) words[w].getAttribute(ImWord.FONT_SIZE_ATTRIBUTE, "-1"));
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
			mWord.setAttribute(ImWord.FONT_SIZE_ATTRIBUTE, ("" + (mFontSizeSum / mFontSizeCount)));
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
			else if (editIdmp.editWord(mWord, true)) {}
			
			//	edit cancelled
			else return false;
			
			//	remember start predecessor and end successor
			ImWord startPrev = start.getPreviousWord();
			ImWord endNext = end.getNextWord();
			
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
}
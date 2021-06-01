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
package de.uka.ipd.idaho.im.imagine.plugins.fonts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.goldenGate.plugins.ResourceSplashScreen;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImFont;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImFontUtils;
import de.uka.ipd.idaho.im.util.SymbolTable;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * This plugin provides functionality for manual editing of font mapping in
 * born-digital PDFs.
 * 
 * @author sautter
 */
public class FontEditorProvider extends AbstractImageMarkupToolProvider implements SelectionActionProvider {
	
	//	font preference switch, mainly for testing Liberation Fonts against default system fonts
	private static final boolean USE_FREE_FONTS = true;
	
	/* make sure we have the fonts we need */
	static {
		if (USE_FREE_FONTS)
			ImFontUtils.loadFreeFonts();
	}
	
	private static Font serifFont = new Font((USE_FREE_FONTS ? "FreeSerif" : "Serif"), Font.PLAIN, 1);
	private static Font sansFont = new Font((USE_FREE_FONTS ? "FreeSans" : "SansSerif"), Font.PLAIN, 1);
	private static Font monospacedFont = new Font((USE_FREE_FONTS ? "FreeMono" : "Monospaced"), Font.PLAIN, 1);
	
	private static final String FONT_EDITOR_IMT_NAME = "FontEditor";
	
	private ImageMarkupTool fontEditor = new FontEditor();
	
	/** public zero-argument constructor for class loading */
	public FontEditorProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "IM Font Editor";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider#getActions(de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(final ImWord start, ImWord end, ImDocumentMarkupPanel idmp) {
		
		//	multiple words selected
		if (start != end)
			return null;
		
		//	little we can do in this document
		if (!idmp.documentBornDigital)
			return null;
		
		//	collect available actions
		LinkedList actions = new LinkedList();
		
		//	edit word font
		actions.add(new SelectionAction("editFont", "Edit Font", "Edit the font the word is rendered in") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				String fontName = ((String) start.getAttribute(ImWord.FONT_NAME_ATTRIBUTE));
				if (fontName == null)
					return false;
				ImFont font = start.getDocument().getFont(fontName);
				if (font == null)
					return false;
				ImFont[] fonts = {font};
				return editFonts(start.getDocument(), fonts);
			}
		});
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, ImPage page, ImDocumentMarkupPanel idmp) {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getEditMenuItemNames()
	 */
	public String[] getEditMenuItemNames() {
		String[] emins = {FONT_EDITOR_IMT_NAME};
		return emins;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		if (FONT_EDITOR_IMT_NAME.equals(name))
			return this.fontEditor;
		else return null;
	}
	
	private class FontEditor implements ImageMarkupTool {
		public String getLabel() {
			return "Edit Embedded Fonts";
		}
		public String getTooltip() {
			return "Edit the transcription of embedded PDF fonts into Unicode characters";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	we work on born-digital documents only
			if (!idmp.documentBornDigital) {
				DialogFactory.alert("This document originates from scanned images, no fonts to edit.", "No Fonts to Edit", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			
			//	get fonts
			ImFont[] fonts = doc.getFonts();
			if (fonts.length == 0) {
				DialogFactory.alert("This document does not contain any fonts with custom characters to transcribe.", "No Fonts to Edit", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			
			//	open dialog for font editing
			editFonts(doc, fonts);
		}
	}
	
	private boolean editFonts(ImDocument doc, ImFont[] fonts) {
		FontEditorDialog fed = new FontEditorDialog(doc, fonts);
		if (fed.feps == null) {
			DialogFactory.alert((((fonts.length == 1) ? "This font does" : "The fonts do") + " not contain any custom characters to transcribe."), "No Characters to Edit", JOptionPane.INFORMATION_MESSAGE);
			return false;
		}
		fed.setVisible(true);
		return fed.hasChanges();
	}
	
	private static final Comparator wordStringOrder = new Comparator() {
		public int compare(Object obj1, Object obj2) {
			return ((ImWord) obj1).getString().compareTo(((ImWord) obj2).getString());
		}
	};
	
	private class FontEditorDialog extends DialogPanel {
		private boolean hasChanges = false;
		private ImDocument doc;
		private TreeMap fontWordsByName = new TreeMap();
		FontEditorPanel[] feps = null;
		
		SymbolTable symbolTable;
		SymbolTable.Owner symbolTableOwner;
		SymbolTable.Owner symbolTableTarget;
		
		FontEditorDialog(ImDocument doc, ImFont[] fonts) {
			super(((fonts.length == 1) ? ("Edit Font '" + fonts[0].name + "'") : "Edit Fonts"), true);
			this.doc = doc;
			
			ArrayList fepList = new ArrayList(fonts.length);
			JTabbedPane fontTabs = new JTabbedPane();
			if (fonts.length > 1) {
				fontTabs.setTabPlacement(JTabbedPane.LEFT);
				fontTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
			}
			for (int f = 0; f < fonts.length; f++) {
				FontEditorPanel fep = new FontEditorPanel(fonts[f]);
				if (fep.ceps == null)
					continue;
				this.fontWordsByName.put(fonts[f].name, new TreeSet(wordStringOrder));
				fepList.add(fep);
				fontTabs.addTab(fonts[f].name, fep);
			}
			if (fepList.isEmpty())
				return;
			this.feps = ((FontEditorPanel[]) fepList.toArray(new FontEditorPanel[fepList.size()]));
			
			JButton ok = new JButton("OK");
			ok.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					commitChanges();
				}
			});
			JButton cancel = new JButton("Cancel");
			cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					FontEditorDialog.this.dispose();
				}
			});
			JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
			buttons.add(ok);
			buttons.add(cancel);
			
			if (this.feps.length == 1)
				this.add(this.feps[0], BorderLayout.CENTER);
			else {
				fontTabs.setSelectedIndex(0);
				this.add(fontTabs, BorderLayout.CENTER);
			}
			this.add(buttons, BorderLayout.SOUTH);
			
			this.setSize(600, 700);
			this.setLocationRelativeTo(this.getOwner());
			
			this.symbolTableOwner = new SymbolTable.Owner() {
				public void useSymbol(char symbol) {
					symbolTableTarget.useSymbol(symbol);
				}
				public Color getColor() {
					return symbolTableTarget.getColor();
				}
				public Point getLocation() {
					return symbolTableTarget.getLocation();
				}
				public Dimension getSize() {
					return symbolTableTarget.getSize();
				}
				public void symbolTableClosed() {
					symbolTable = null;
				}
			};
		}
		
		public void dispose() {
			if (this.symbolTable != null)
				this.symbolTable.close();
			super.dispose();
		}
		
		void showSymbolTableFor(SymbolTable.Owner target) {
			this.symbolTableTarget = target;
			if (this.symbolTable == null) {
				this.symbolTable = SymbolTable.getSharedSymbolTable();
				this.symbolTable.setOwner(this.symbolTableOwner);
				this.symbolTable.open();
			}
			else this.symbolTable.updateOwner();
		}
		
		boolean hasChanges() {
			return this.hasChanges;
		}
		
		void commitChanges() {
			
			//	create splash screen 
			final ResourceSplashScreen rss = new ResourceSplashScreen(this.getDialog(), "Updating Page Images", "Font updates are being written to papge images ...");
			
			//	we have to update in a separate thread
			Thread commitThread = new Thread() {
				public void run() {
					try {
						
						//	wait for splash screen to show
						while (!rss.isVisible()) try {
							Thread.sleep(25);
						} catch (InterruptedException ie) {}
						
						//	commit and collect changes to individual fonts
						rss.setStep("Collecting font updates");
						HashMap updatedCharsByFontName = new HashMap();
						for (int f = 0; f < feps.length; f++) {
							rss.setInfo(feps[f].font.name);
							HashSet updatedChars = feps[f].commitChanges();
							if (updatedChars.size() != 0) {
								updatedCharsByFontName.put(feps[f].font.name, updatedChars);
								System.out.println("Characters changed in font " + feps[f].font.name + ": " + updatedChars);
							}
						}
						
						//	update page images and words
						rss.setStep("Updating page images");
						ImPage[] pages = doc.getPages();
						for (int p = 0; p < pages.length; p++) {
							rss.setProgress((p * 100) / pages.length);
							ImWord[] pageWords = pages[p].getWords();
							HashMap pageWordFonts = new HashMap();
							HashSet pageWordCharsUpdated = new HashSet();
							for (int w = 0; w < pageWords.length; w++) {
								String fontName = ((String) pageWords[w].getAttribute(ImWord.FONT_NAME_ATTRIBUTE));
								if (!updatedCharsByFontName.containsKey(fontName))
									continue;
								HashSet updatedChars = ((HashSet) updatedCharsByFontName.get(fontName));
								if (updatedChars.contains(FONT_FLAGS_CHANGED))
									pageWordFonts.put(pageWords[w], doc.getFont(fontName));
								String charCodes = ((String) pageWords[w].getAttribute(ImFont.CHARACTER_CODE_STRING_ATTRIBUTE));
								if (charCodes == null)
									continue;
								for (int c = 0; c < charCodes.length(); c += 2)
									if (updatedChars.contains(new Integer(Integer.parseInt(charCodes.substring(c, (c+2)), 16)))) {
										pageWordFonts.put(pageWords[w], doc.getFont(fontName));
										pageWordCharsUpdated.add(pageWords[w]);
										break;
									}
							}
							rss.setInfo(pageWordFonts.size() + " words affected in page " + p);
							if (pageWordFonts.size() != 0) {
								hasChanges = true;
								updatePage(pages[p], pageWordFonts, pageWordCharsUpdated);
							}
						}
						rss.setProgress(100);
					}
					
					//	close splash screen 
					finally {
						rss.dispose();
					}
				}
			};
			commitThread.start();
			
			//	open splash screen and wait
			rss.popUp(true);
			this.dispose();
		}
		
		private void updatePage(ImPage page, HashMap pageWordFonts, HashSet pageWordCharsUpdated) {
			
			//	get page image & create graphics
			PageImage pi = page.getPageImage();
			BufferedImage pbi = new BufferedImage(pi.image.getWidth(), pi.image.getHeight(), pi.image.getType());
			Graphics2D prg = pbi.createGraphics();
			prg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			prg.drawImage(pi.image, 0, 0, null);
			
			//	get tokenizer
			Tokenizer tokenizer = ((Tokenizer) page.getDocument().getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER));
			
			//	put words in list to facilitate insertion of split results (doesn't work with iterator)
			ArrayList pageWords = new ArrayList(pageWordFonts.keySet());
			
			//	re-render affected words
			for (int w = 0; w < pageWords.size(); w++) {
				ImWord imw = ((ImWord) pageWords.get(w));
				ImFont imwFont = ((ImFont) pageWordFonts.get(imw));
				
				//	rebuild word string from char codes (only if chars modified, might be font flags only)
				if (pageWordCharsUpdated.contains(imw)) {
					System.out.println(" - changing value of word '" + imw.getString() + "'");
					String imwCharCodeString = ((String) imw.getAttribute(ImFont.CHARACTER_CODE_STRING_ATTRIBUTE));
					String[] imwCharCodes = new String[imwCharCodeString.length() / 2];
					for (int c = 0; c < imwCharCodeString.length(); c += 2)
						imwCharCodes[c/2] = imwCharCodeString.substring(c, (c+2));
					System.out.println(" - char codes are " + Arrays.toString(imwCharCodes));
					int[] imwCharCodeLengths = new int[imwCharCodes.length];
					StringBuffer imwString = new StringBuffer();
					for (int c = 0; c < imwCharCodes.length; c++) {
						String cStr = imwFont.getString(Integer.parseInt(imwCharCodes[c], 16));
						System.out.println("   - char code '" + imwCharCodes[c] + "' (ASCII '" + ((char) Integer.parseInt(imwCharCodes[c], 16)) + "') mapped to '" + cStr + "'");
						if (cStr == null) {
							cStr = ("" + ((char) Integer.parseInt(imwCharCodes[c], 16)));
							System.out.println("   --> using ASCII fallback '" + cStr + "'");
						}
						else cStr = dissolveLigature(cStr);
						imwCharCodeLengths[c] = cStr.length();
						if ((cStr.length() == 1) && (COMBINABLE_ACCENTS.indexOf(cStr) != -1) && (imwString.length() != 0)) {
							char cChar = StringUtils.getCharForName("" + imwString.charAt(imwString.length() - 1) + "" + COMBINABLE_ACCENT_MAPPINGS.get(new Character(cStr.charAt(0))));
							if (cChar > 0)
								imwString.setCharAt((imwString.length() - 1), cChar);
							else imwString.append(cStr);
						}
						else imwString.append(cStr);
					}
					System.out.println(" - string value of word '" + imw.getString() + "' changed to " + imwString.toString());
					
					//	check tokenization
					TokenSequence imwTokens = tokenizer.tokenize(imwString);
					if (imwTokens.size() < 2)
						imw.setString(imwString.toString());
					
					else {
						System.out.println(" - splitting " + imwString.toString());
						
						//	get width for each token at word font size
						String[] splitCharCodes = new String[imwTokens.size()];
						String[] splitTokens = new String[imwTokens.size()];
						float[] splitTokenWidths = new float[imwTokens.size()];
						
						//	initialize font (only if we need character width proportional split)
						Font font;
						if (imwFont.isMonospaced())
							font = null;
						else {
							int fontStyle = Font.PLAIN;
							if (imwFont.isBold())
								fontStyle = (fontStyle | Font.BOLD);
							if (imwFont.isItalics())
								fontStyle = (fontStyle | Font.ITALIC);
							int fontSize = ((imw.getFontSize() * pi.currentDpi) / 72); // 72 DPI is default according to PDF specification
							font = (imwFont.isSerif() ? serifFont : sansFont).deriveFont(fontStyle, fontSize);
						}
						
						//	perform word split
						float splitTokenWidthSum = 0;
						int splitCharCodeStart = 0;
						for (int s = 0; s < splitTokens.length; s++) {
							
							//	split character codes according to tokens
							splitTokens[s] = imwTokens.valueAt(s);
							splitCharCodes[s] = ""; // have to do it this way, as char code string (a) consists of hex digit pairs and (b) might have different length (number of hex digit pairs) than Unicode string
							for (int splitCharCodeLength = 0; splitCharCodeLength < splitTokens[s].length();) {
								splitCharCodes[s] += imwCharCodes[splitCharCodeStart];
								splitCharCodeLength += imwCharCodeLengths[splitCharCodeStart];
								splitCharCodeStart++;
							}
							
							//	apply simple equidistant split for monospaced fonts
							if (font == null)
								splitTokenWidths[s] = ((float) splitTokens[s].length());
							
							//	do character width proportional split otherwise
							else {
								TextLayout tl = new TextLayout(splitTokens[s], font, new FontRenderContext(null, false, true));
								splitTokenWidths[s] = ((float) tl.getBounds().getWidth());
							}
							splitTokenWidthSum += splitTokenWidths[s];
						}
						
						//	store split result, splitting word bounds accordingly
						float imwWidth = ((float) (imw.bounds.right - imw.bounds.left));
						float splitTokenLeft = ((float) imw.bounds.left);
						ImWord[] splitWords = new ImWord[splitTokens.length];
						for (int s = 0; s < splitTokens.length; s++) {
							float splitTokenWidth = ((imwWidth * splitTokenWidths[s]) / splitTokenWidthSum);
							splitWords[s] = new ImWord(page.getDocument(), page.pageId, new BoundingBox(Math.round(splitTokenLeft), Math.round(splitTokenLeft + splitTokenWidth), imw.bounds.top, imw.bounds.bottom), splitTokens[s]);
							splitWords[s].copyAttributes(imw);
							splitWords[s].setAttribute(ImFont.CHARACTER_CODE_STRING_ATTRIBUTE, splitCharCodes[s]);
							splitTokenLeft += splitTokenWidth;
							System.out.println(" --> split word part " + splitWords[s].getString() + ", bounds are " + splitWords[s].bounds);
						}
						
						//	set up split result for rendering
						for (int s = 0; s < splitWords.length; s++) {
							if (s == 0)
								pageWords.set(w, splitWords[s]);
							else pageWords.add((w+s), splitWords[s]);
							pageWordFonts.put(splitWords[s], imwFont);
						}
						
						//	add split result to document (insert after split word first, so to maintain annotations)
						splitWords[splitWords.length-1].setNextWord(imw.getNextWord()); // have to do this first, as after next line, first part of split result is successor of original word
						splitWords[0].setPreviousWord(imw);
						for (int s = 0; s < splitWords.length; s++) {
							page.addWord(splitWords[s]);
							if (s != 0)
								splitWords[s].setPreviousWord(splitWords[s-1]);
						}
						
						//	change annotation start and end words
						ImAnnotation[] imwStartAnnots = page.getDocument().getAnnotations(imw, null);
						for (int a = 0; a < imwStartAnnots.length; a++)
							imwStartAnnots[a].setFirstWord(splitWords[0]);
						ImAnnotation[] imwEndAnnots = page.getDocument().getAnnotations(null, imw);
						for (int a = 0; a < imwEndAnnots.length; a++)
							imwEndAnnots[a].setLastWord(splitWords[splitWords.length-1]);
						
						//	finally ...
						splitWords[0].setPreviousWord(imw.getPreviousWord());
						page.removeWord(imw, true);
					}
				}
				
				//	re-get word and font (we might have had a split)
				imw = ((ImWord) pageWords.get(w));
				imwFont = ((ImFont) pageWordFonts.get(imw));
				
				//	set word attributes
				if (imwFont.isBold())
					imw.setAttribute(ImWord.BOLD_ATTRIBUTE);
				else imw.removeAttribute(ImWord.BOLD_ATTRIBUTE);
				if (imwFont.isItalics())
					imw.setAttribute(ImWord.ITALICS_ATTRIBUTE);
				else imw.removeAttribute(ImWord.ITALICS_ATTRIBUTE);
				
				//	paint word box white
				prg.setColor(Color.WHITE);
				prg.fillRect(imw.bounds.left, imw.bounds.top, imw.bounds.getWidth(), imw.bounds.getHeight());
				
				//	prepare rendering font
				prg.setColor(Color.BLACK);
				int fontStyle = Font.PLAIN;
				if (imwFont.isBold())
					fontStyle = (fontStyle | Font.BOLD);
				if (imwFont.isItalics())
					fontStyle = (fontStyle | Font.ITALIC);
				int fontSize = ((imw.getFontSize() * pi.currentDpi) / 72); // 72 DPI is default according to PDF specification
				Font rf = (imwFont.isMonospaced() ? monospacedFont : (imwFont.isSerif() ? serifFont : sansFont)).deriveFont(fontStyle, fontSize);
				prg.setFont(rf);
				
				//	adjust word size and position
				String imwString = imw.getString();
				AffineTransform at = prg.getTransform();
				FontRenderContext imwFrc = new FontRenderContext(at, true, true);
				LineMetrics wlm = rf.getLineMetrics(imwString, imwFrc);
				TextLayout wtl = new TextLayout(imwString, rf, imwFrc);
				prg.translate(imw.bounds.left, imw.bounds.bottom);
				float leftShift = ((float) -wtl.getBounds().getMinX());
				double hScale = 1;
				
				//	rotate and scale word as required
				if (ImWord.TEXT_DIRECTION_BOTTOM_UP.equals(imw.getAttribute(ImWord.TEXT_DIRECTION_ATTRIBUTE))) {
					prg.rotate((-Math.PI / 2), (((float) imw.bounds.getWidth()) / 2), -(((float) imw.bounds.getWidth()) / 2));
					if (imwFont.isItalics())
						hScale = (((double) imw.bounds.getHeight()) / wtl.getBounds().getWidth());
					else {
						hScale = (((double) imw.bounds.getHeight()) / wtl.getAdvance());
						leftShift = 0;
					}
					prg.scale(1, hScale);
				}
				if (ImWord.TEXT_DIRECTION_TOP_DOWN.equals(imw.getAttribute(ImWord.TEXT_DIRECTION_ATTRIBUTE))) {
					prg.rotate((Math.PI / 2), (((float) imw.bounds.getHeight()) / 2), -(((float) imw.bounds.getHeight()) / 2));
					if (imwFont.isItalics())
						hScale = (((double) imw.bounds.getHeight()) / wtl.getBounds().getWidth());
					else {
						hScale = (((double) imw.bounds.getHeight()) / wtl.getAdvance());
						leftShift = 0;
					}
					prg.scale(1, hScale);
				}
				else {
					if (imwFont.isItalics())
						hScale = (((double) imw.bounds.getWidth()) / wtl.getBounds().getWidth());
					else {
						hScale = (((double) imw.bounds.getWidth()) / wtl.getAdvance());
						leftShift = 0;
					}
					prg.scale(hScale, 1);
				}
				
				//	render word, finally ...
				try {
					prg.drawGlyphVector(rf.createGlyphVector(imwFrc, imw.getString()), leftShift, -Math.round(wlm.getDescent()));
				} catch (InternalError ie) {}
				
				//	reset graphics
				prg.setTransform(at);
				System.out.println(" - word '" + imw.getString() + "' re-rendered");
			}
			
			//	store modified page image
			System.out.println(" - modified page image stored");
			page.setImage(new PageImage(pbi, pi.originalWidth, pi.originalHeight, pi.originalDpi, pi.currentDpi, pi.leftEdge, pi.rightEdge, pi.topEdge, pi.bottomEdge, pi.source));
		}
		
		private ImWord[] getFontWords(String fontName) {
//			System.out.println("Getting words for font " + fontName);
			TreeSet fontWords = ((TreeSet) this.fontWordsByName.get(fontName));
			if (fontWords == null) {
//				System.out.println(" ==> no words found");
				return new ImWord[0];
			}
			if (fontWords.isEmpty()) {
//				System.out.println(" - collecting words");
				ImPage[] pages = this.doc.getPages();
				for (int p = 0; p < pages.length; p++) {
					ImWord[] pageWords = pages[p].getWords();
					for (int w = 0; w < pageWords.length; w++) {
						if (fontName.equals(pageWords[w].getAttribute(ImWord.FONT_NAME_ATTRIBUTE)))
							fontWords.add(pageWords[w]);
					}
//					System.out.println("   - got " + fontWords.size() + " words after page " + p);
				}
			}
//			System.out.println(" ==> got " + fontWords.size() + " words");
			return ((ImWord[]) fontWords.toArray(new ImWord[fontWords.size()]));
		}
		
		void showWords(String fontName, int cid) {
//			System.out.println("Getting words for char code " + cid + " in font " + fontName);
			ImWord[] words = this.getFontWords(fontName);
			if (cid >= 0) {
				ArrayList cidWords = new ArrayList();
				String cidHex = Integer.toString(cid, 16).toUpperCase();
				if (cidHex.length() < 2)
					cidHex = ("0" + cidHex);
//				System.out.println(" - char code hex is " + cidHex);
				for (int w = 0; w < words.length; w++) {
					String charCodes = ((String) words[w].getAttribute(ImFont.CHARACTER_CODE_STRING_ATTRIBUTE));
					if ((charCodes != null) && ((charCodes.indexOf(cidHex) % 2) == 0)) {
						cidWords.add(words[w]);
//						System.out.println("   - added word " + words[w].getString() + " with char codes " + charCodes);
					}
//					else System.out.println("   - ignored word " + words[w].getString() + " with char codes " + charCodes);
				}
				words = ((ImWord[]) cidWords.toArray(new ImWord[cidWords.size()]));
//				System.out.println(" - got " + words.length + " words containing char code " + cidHex);
			}
			
			String[] wordStrings = new String[words.length];
			for (int w = 0; w < words.length; w++) {
				StringBuffer sb = new StringBuffer("<HTML>");
				ImWord pWord = words[w];
				int pContext = 0;
				while (pWord.getPreviousWord() != null) {
					pWord = pWord.getPreviousWord();
					pContext += pWord.getString().length();
					if (pContext > 10)
						break;
				}
				while (pWord != words[w]) {
					sb.append(pWord.getString() + " ");
					pWord = pWord.getNextWord();
				}
				sb.append("<B>");
				sb.append(words[w].getString());
				sb.append("</B>");
				ImWord nWord = words[w].getNextWord();
				int nContext = 0;
				while (nWord != null) {
					sb.append(" " + nWord.getString());
					nContext += nWord.getString().length();
					if (nContext > 10)
						break;
					nWord = nWord.getNextWord();
				}
				sb.append("</HTML>");
				wordStrings[w] = sb.toString();
			}
			
			final DialogPanel swd = new DialogPanel(this.getDialog(), ("Example Words for Font '" + fontName + "'"), true);
			
			JList wordList = new JList(wordStrings);
			JScrollPane wordListBox = new JScrollPane(wordList);
			wordListBox.getVerticalScrollBar().setUnitIncrement(50);
			wordListBox.getVerticalScrollBar().setBlockIncrement(50);
			
			JButton cb = new JButton("Close");
			cb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					swd.dispose();
				}
			});
			
			swd.add(wordListBox, BorderLayout.CENTER);
			swd.add(cb, BorderLayout.SOUTH);
			
			swd.setSize(250, 500);
			swd.setLocationRelativeTo(swd.getOwner());
			swd.setVisible(true);
		}
		
		class FontEditorPanel extends JPanel {
			final ImFont font;
			private JCheckBox bold;
			private JCheckBox italics;
//			private JCheckBox serif;
//			private JCheckBox monospaced;
			private JRadioButton sansSerif = new JRadioButton("Sans-Serif");
			private JRadioButton serif = new JRadioButton("Serif");
			private JRadioButton monospaced = new JRadioButton("Monospaced");
			CharEditorPanel[] ceps = null;
			FontEditorPanel(ImFont font) {
				super(new BorderLayout(), true);
				this.font = font;
				
				this.bold = new JCheckBox("Bold", this.font.isBold());
				this.italics = new JCheckBox("Italics", this.font.isItalics());
//				this.serif = new JCheckBox("Serif", this.font.isSerif());
//				this.monospaced = new JCheckBox("Monospaced", this.font.isMonospaced());
				ButtonGroup ctbg = new ButtonGroup();
				ctbg.add(this.sansSerif);
				ctbg.add(this.serif);
				ctbg.add(this.monospaced);
				if (this.font.isMonospaced())
					this.monospaced.setSelected(true);
				else if (this.font.isSerif())
					this.serif.setSelected(true);
				else this.sansSerif.setSelected(true);
				
				int[] cids = this.font.getCharacterIDs();
				BufferedImage[] cis = new BufferedImage[cids.length];
				int maxCharImageWidth = 0;
				for (int c = 0; c < cids.length; c++) {
					cis[c] = this.font.getImage(cids[c]);
					if (cis[c] != null)
						maxCharImageWidth = Math.max(maxCharImageWidth, cis[c].getWidth());
				}
				ArrayList cepList = new ArrayList(cids.length);
				for (int c = 0; c < cids.length; c++) {
					if (cis[c] != null)
						cepList.add(new CharEditorPanel(cids[c], cis[c], maxCharImageWidth));
				}
				if (cepList.isEmpty())
					return;
				this.ceps = ((CharEditorPanel[]) cepList.toArray(new CharEditorPanel[cepList.size()]));
				
				JPanel flagsPanel = new JPanel(new GridLayout(1, 0));
				flagsPanel.add(this.bold);
				flagsPanel.add(this.italics);
				flagsPanel.add(this.sansSerif);
				flagsPanel.add(this.serif);
				flagsPanel.add(this.monospaced);
				JButton swb = new JButton("Words");
				swb.setToolTipText("Show list of words in font '" + this.font.name + "'");
				swb.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						showWords(FontEditorPanel.this.font.name, -1);
					}
				});
				flagsPanel.add(swb);
				
				JPanel charPanel = new JPanel(new GridBagLayout(), true);
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridheight = 1;
				gbc.insets.left = 4;
				gbc.insets.right = 4;
				gbc.insets.top = 2;
				gbc.insets.bottom = 2;
				gbc.fill = GridBagConstraints.BOTH;
				gbc.gridwidth = 1;
				gbc.gridx = 0;
				gbc.gridy = 0;
				gbc.weightx = 1;
				gbc.weighty = 0;
				for (int c = 0; c < this.ceps.length; c++) {
					charPanel.add(this.ceps[c], gbc.clone());
					gbc.gridy++;
				}
				gbc.weighty = 1;
				charPanel.add(new JPanel(), gbc.clone());
				
				JScrollPane charPanelBox = new JScrollPane(charPanel);
				charPanelBox.getVerticalScrollBar().setUnitIncrement(50);
				charPanelBox.getVerticalScrollBar().setBlockIncrement(50);
				
				this.add(flagsPanel, BorderLayout.NORTH);
				this.add(charPanelBox, BorderLayout.CENTER);
			}
//			
//			boolean isDirty() {
//				if (this.font.isBold() != this.bold.isSelected())
//					return true;
//				if (this.font.isItalics() != this.italics.isSelected())
//					return true;
//				if (this.font.isSerif() != this.serif.isSelected())
//					return true;
//				for (int c = 0; c < this.ceps.length; c++) {
//					if (this.ceps[c].isDirty())
//						return true;
//				}
//				return false;
//			}
			
			HashSet commitChanges() {
				HashSet cCidSet = new HashSet();
				if ((this.font.isBold() != this.bold.isSelected()) || (this.font.isItalics() != this.italics.isSelected()) || (this.font.isSerif() != this.serif.isSelected()) || (this.font.isMonospaced() != this.monospaced.isSelected())) {
					cCidSet.add(FONT_FLAGS_CHANGED);
					this.font.setBold(this.bold.isSelected());
					this.font.setItalics(this.italics.isSelected());
//					this.font.setSerif(this.serif.isSelected());
//					this.font.setMonospaced(this.monospaced.isSelected());
					if (this.monospaced.isSelected())
						this.font.setMonospaced(true);
					else this.font.setSerif(this.serif.isSelected()); // setting serif to false sets type back to sans-serif
				}
				for (int c = 0; c < this.ceps.length; c++) {
					if (this.ceps[c].commitChange())
						cCidSet.add(new Integer(this.ceps[c].charId));
				}
				if (cCidSet.size() != 0)
					System.out.println("Characters changed in font " + this.font.name + ": " + cCidSet);
				return cCidSet;
			}
			
			class CharEditorPanel extends JPanel {
				final int charId;
				private String oCharStr;
				private JTextField charStr;
				private JLabel charStrHex = new JLabel();
				private SymbolTable.Owner sto;
				CharEditorPanel(int cid, BufferedImage ci, int ciw) {
					super(new BorderLayout(), true);
					this.charId = cid;
					this.oCharStr = font.getString(this.charId);
					
					String cidHex = Integer.toString(cid, 16).toUpperCase();
					if (cidHex.length() < 2)
						cidHex = ("0" + cidHex);
					JLabel cidl = new JLabel(("<HTML><B>" + cidHex + "</B></HTML>"), JLabel.CENTER);
					cidl.setBorder(BorderFactory.createLineBorder(cidl.getBackground(), 4));
					
					JLabel cil = new JLabel(new ImageIcon(ci), JLabel.CENTER);
					cil.setOpaque(true);
					cil.setBackground(Color.WHITE);
					cil.setPreferredSize(new Dimension(ciw, cil.getPreferredSize().height));
					
					this.charStr = new JTextField(this.oCharStr);
					this.charStr.setFont(new Font("Monospaced", Font.PLAIN, 24));
					this.charStr.getDocument().addDocumentListener(new DocumentListener() {
						public void insertUpdate(DocumentEvent de) {
							updateDataDisplay();
						}
						public void removeUpdate(DocumentEvent de) {
							updateDataDisplay();
						}
						public void changedUpdate(DocumentEvent de) {}
					});
					this.charStrHex.setBorder(BorderFactory.createLineBorder(this.charStrHex.getBackground(), 4));
					this.updateDataDisplay();
					
					final JButton stb = new JButton("Symbol");
					this.sto = new SymbolTable.Owner() {
						public void useSymbol(char symbol) {
							CharEditorPanel.this.useSymbol(symbol);
						}
						public Color getColor() {
							return stb.getBackground();
						}
						public Point getLocation() {
							return stb.getLocationOnScreen();
						}
						public Dimension getSize() {
							return stb.getSize();
						}
						public void symbolTableClosed() {}
					};
					stb.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							showSymbolTableFor(sto);
							charStr.requestFocus();
						}
					});
					this.charStr.addFocusListener(new FocusAdapter() {
						public void focusGained(FocusEvent fe) {
							if (symbolTable != null)
								showSymbolTableFor(sto);
						}
					});
					
					JButton swb = new JButton("Words");
					swb.setToolTipText("Show list of words in font '" + font.name + "' containing character " + this.charId);
					swb.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							showWords(FontEditorPanel.this.font.name, charId);
						}
					});
					
					JPanel cp = new JPanel(new BorderLayout(), true);
					cp.add(cidl, BorderLayout.WEST);
					cp.add(cil, BorderLayout.CENTER);
					
					JPanel dp = new JPanel(new BorderLayout(), true);
					dp.add(this.charStr, BorderLayout.CENTER);
					dp.add(this.charStrHex, BorderLayout.EAST);
					
					JPanel bp = new JPanel(new BorderLayout(), true);
					bp.add(stb, BorderLayout.WEST);
					bp.add(swb, BorderLayout.EAST);
					
					this.add(cp, BorderLayout.WEST);
					this.add(dp, BorderLayout.CENTER);
					this.add(bp, BorderLayout.EAST);
				}
				
				void useSymbol(char symbol) {
					StringBuffer sb = new StringBuffer(this.charStr.getText());
					int cp = this.charStr.getCaretPosition();
					sb.insert(cp, symbol);
					this.charStr.setText(sb.toString());
					this.charStr.setCaretPosition(++cp);
					this.charStr.requestFocus();
				}
//				
//				boolean isDirty() {
//					return !this.oCharStr.equals(this.charStr.getText().trim());
//				}
				
				boolean commitChange() {
					String charStr = this.charStr.getText().trim();
					if (this.oCharStr.equals(charStr))
						return false;
					System.out.println("String value of char " + this.charId + " '" + this.oCharStr + "' set to '" + charStr + "' in font " + font.name);
					if (font.addCharacter(this.charId, charStr, ((BufferedImage) null))) {
						System.out.println(" ==> char changed");
						return true;
					}
					else {
						System.out.println(" ==> char unchanged ... WHY DO WE GET HERE?");
						return false;
					}
				}
				
				void updateDataDisplay() {
					String charStr = this.charStr.getText();
					StringBuffer charStrHex = new StringBuffer();
					for (int c = 0; c < charStr.length(); c++) {
						String chh = Integer.toString(((int) charStr.charAt(c)), 16).toUpperCase();
						while (chh.length() < 4)
							chh = ("0" + chh);
						if (c != 0)
							charStrHex.append(' ');
						charStrHex.append(chh);
					}
					this.charStrHex.setText(charStrHex.toString());
					this.charStr.setBackground(((charStr.length() == 1) && (charStr.charAt(0) < 127)) ? Color.WHITE : Color.YELLOW);
					this.validate();
					this.repaint();
				}
			}
		}
	}
	
	private static final Integer FONT_FLAGS_CHANGED = new Integer(-1);
	
	private static final String COMBINABLE_ACCENTS;
	private static final HashMap COMBINABLE_ACCENT_MAPPINGS = new HashMap();
	static {
//		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u00A8'), "dieresis");
//		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u00AF'), "macron");
//		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u00B4'), "acute");
//		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u00B8'), "cedilla");
//		
//		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u02C6'), "circumflex");
//		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u02C7'), "caron");
//		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u02D8'), "breve");
//		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u02DA'), "ring");
		
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0300'), "grave");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0301'), "acute");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0302'), "circumflex");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0303'), "tilde");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0304'), "macron");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0306'), "breve");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0307'), "dot");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0308'), "dieresis");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0309'), "hook");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u030A'), "ring");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u030B'), "dblacute");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u030C'), "caron");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u030F'), "dblgrave");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0323'), "dotbelow");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0327'), "cedilla");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0328'), "ogonek");
		
		StringBuffer combinableAccentCollector = new StringBuffer();
		ArrayList combinableAccents = new ArrayList(COMBINABLE_ACCENT_MAPPINGS.keySet());
		for (int c = 0; c < combinableAccents.size(); c++) {
			Character combiningChar = ((Character) combinableAccents.get(c));
			combinableAccentCollector.append(combiningChar.charValue());
			String charName = ((String) COMBINABLE_ACCENT_MAPPINGS.get(combiningChar));
			char baseChar = StringUtils.getCharForName(charName);
			if ((baseChar > 0) && (baseChar != combiningChar.charValue())) {
				combinableAccentCollector.append(baseChar);
				COMBINABLE_ACCENT_MAPPINGS.put(new Character(baseChar), charName);
			}
		}
		COMBINABLE_ACCENTS = combinableAccentCollector.toString();
	}
	
	private static String dissolveLigature(String rCh) {
		if (rCh.length() != 1)
			return rCh;
		String nrCh = StringUtils.getNormalForm(rCh.charAt(0));
		if (nrCh.length() == 1)
			return rCh;
		System.out.println("   --> dissolved '" + rCh + "' to '" + nrCh + "'");
		return nrCh;
	}
}
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
package de.uka.ipd.idaho.im.imagine.plugins.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AttributeUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImSupplement;
import de.uka.ipd.idaho.im.ImSupplement.Figure;
import de.uka.ipd.idaho.im.ImSupplement.Graphics;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.gamta.ImDocumentRoot;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractReactionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener;
import de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * This plugin listens to changes to captions marked in a document, marks
 * their citations, and keeps the attributes of the latter in sync with the
 * referenced captions.
 * 
 * @author sautter
 */
public class CaptionCitationHandler extends AbstractReactionProvider implements SelectionActionProvider, GoldenGateImagineDocumentListener, ImagingConstants {
	
	/** public zero-argument constructor for class loading */
	public CaptionCitationHandler() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Caption Citation Handler";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, ImPage page, ImDocumentMarkupPanel idmp) {
		return null; // we're only annotating ...
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider#getActions(de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(final ImWord start, final ImWord end, final ImDocumentMarkupPanel idmp) {
		
		//	no use working across text stream
		if (!start.getTextStreamId().equals(end.getTextStreamId()))
			return null;
		
		//	offer 'Mark Citations' for click on caption proper
		if (ImWord.TEXT_STREAM_TYPE_CAPTION.equals(start.getTextStreamType()))
			return this.getMarkCaptionCitationsAction(start, end, idmp);
		
		//	offer 'Mark Figure/Table Citation' otherwise
		else return this.getMarkCaptionCitationActions(start, end, idmp);
	}
	
	private SelectionAction[] getMarkCaptionCitationsAction(final ImWord start, final ImWord end, final ImDocumentMarkupPanel idmp) {
		
		//	do we have a click on a single word?
		if (start != end)
			return null;
		
		//	are captions visible?
		if (!idmp.areAnnotationsPainted(CAPTION_TYPE))
			return null;
		
		//	get clicked caption
		final ImAnnotation clickedCaption = this.getCaptionAt(idmp.document, start);
		if (clickedCaption == null)
			return null;
		
		//	collect actions
		LinkedList actions = new LinkedList();
		
		//	offer marking citations of table clicked caption belongs to
//		if (clickedCaption.getFirstWord().getString().toLowerCase().startsWith("tab"))
		if (this.getStringFrom(clickedCaption.getFirstWord()).toLowerCase().startsWith("tab"))
			actions.add(new SelectionAction("annotateCaptionCitations", "Mark Citations of Table", "Annotate citations of table belonging to this caption.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return annotateCaptionCitations(idmp.document, clickedCaption, idmp);
				}
			});
		
		//	offer marking citations of figure clicked caption belongs to
		else actions.add(new SelectionAction("annotateCaptionCitations", "Mark Citations of Figure", "Annotate citations of figure belonging to this caption.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				return annotateCaptionCitations(idmp.document, clickedCaption, idmp);
			}
		});
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	private ImAnnotation getCaptionAt(ImDocument doc, ImWord imw) {
		ImAnnotation[] clickedPageCaptions = doc.getAnnotations(CAPTION_TYPE, imw.pageId);
		for (int c = 0; c < clickedPageCaptions.length; c++) {
			if (clickedPageCaptions[c].getFirstWord().getTextStreamId().equals(imw.getTextStreamId()))
				return clickedPageCaptions[c];
		}
		return null;
	}
	
	private SelectionAction[] getMarkCaptionCitationActions(final ImWord start, final ImWord end, final ImDocumentMarkupPanel idmp) {
		
		//	if selection starts with a word (not an index letter or Roman number), it has to be 'Tab*', 'Fig*', etc., whichever is present in the document
		TreeSet docCaptionStarts = this.getDocumentCaptionStarts(idmp.document, true);
//		if ((start.getString().length() > 1) && Gamta.isWord(start.getString()) && !docCaptionStarts.contains(start.getString()) && !start.getString().matches("[IiVvXxLl]+"))
//			return null;
		String startString = start.getString();
		if (startString.length() < 2) // account for emulated small-caps
			startString = this.getStringFrom(start);
		if (startString.length() < 2) {} // too short to tell
		else if (!Gamta.isWord(startString)) {} // number or punctuation
		else if (docCaptionStarts.contains(startString)) {} // valid caption start
		else if (startString.toLowerCase().endsWith("s") && docCaptionStarts.contains(startString.substring(0, (startString.length() - "s".length())))) {} // valid caption start in plural, might start caption citation enumeration
		else if (startString.matches("[IiVvXxLl]+")) {} // Roman number, might be part of caption citation enumeration
		else return null; // not looking like any of our business
		
		//	not crossing paragraph boundary with caption citation, so no use starting right before one
		if (start.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
			return null;
		
		//	check length of selection, and also if we have at least one number selected
		int selWordCount = 0;
		boolean selHasNumber = false;
		for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
			selWordCount++;
			
//			String imwString = imw.getString();
			String imwString = this.getStringFrom(imw);
			if ((imwString == null) || (imwString.trim().length() == 0))
				continue;
			
			//	skip over start word (even if joined)
			if ((imw == start) && docCaptionStarts.contains(imwString)) {
				while ((imw != null) && ((imw.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE) || (imw.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED)))
					imw = imw.getNextWord();
				continue;
			}
			
			//	Arabic index number
			if (imwString.matches("[0-9]+(\\s*[a-z])?"))
				selHasNumber = true;
			
			//	lower case Roman index number
			else if (imwString.matches("[ivxl]+"))
				selHasNumber = true;
			
			//	upper case Roman index number
			else if (imwString.matches("[IVXL]+"))
				selHasNumber = true;
			
			//	lower case index letter
			else if (imwString.matches("[a-z]"))
				selHasNumber = true;
			
			//	upper case index letter
			else if (imwString.matches("[A-Z]"))
				selHasNumber = true;
			
			//	no use looking beyond an internal word (safe for enumeration separators) ...
			if ((imwString.length() <= 1) || !Gamta.isWord(imwString) || imwString.matches("[IiVvXxLl]+")) {}
			else if (imwString.equals(",") || imwString.equals("&") || (";and;und;et;y;".indexOf(";" + imwString.toLowerCase() + ";") != -1)) {}
			else if (selHasNumber)
				return null;
			
			//	need to look any further?
			if (imw == end)
				break;
			else if (imw.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
				return null; // caption citations will rarely ever run across paragraph boundaries ...
			else if (selWordCount > 10)
				return null; // caption citations will rarely ever be longer than 'XYZ Figs. 2a - f', which is 7 tokens ...
		}
		
		//	do we have a number?
		if (!selHasNumber)
			return null;
		
		//	find caption citation head word
		String ccHead = null;
		ImWord ccNumStart = null;
		if (docCaptionStarts.contains(start.getString())) {
			ccHead = start.getString();
			ccNumStart = start.getNextWord();
		}
		else if (docCaptionStarts.contains(this.getStringFrom(start))) {
			ccHead = this.getStringFrom(start); // this one will be rare enough for a duplicate computation to be alright
			ccNumStart = this.getFollowingWordStart(start);
		}
		else for (ImWord imw = start.getPreviousWord(); imw != null; imw = imw.getPreviousWord()) {
			if (docCaptionStarts.contains(imw.getString())) {
				ccHead = imw.getString();
				ccNumStart = start;
				break;
			}
			else if (docCaptionStarts.contains(this.getStringFrom(imw))) {
				ccHead = this.getStringFrom(start); // this one will be rare enough for a duplicate computation to be alright
				ccNumStart = start;
				break;
			}
			else if (imw.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
				return null; // even enumerations of caption citations will rarely ever run across paragraph boundaries ...
		}
		if (ccHead == null)
			return null;
		
		//	get parsed caption citation objects (we'll assign captions later)
		final ArrayList ccList = this.getCaptionCitations(start, ccNumStart, end, null);
		if (ccList.isEmpty())
			return null;
		
		//	collect actions
		LinkedList actions = new LinkedList();
		
		//	offer marking table citation
		if (ccHead.toLowerCase().startsWith("tab"))
			actions.add(new SelectionAction("annotateTableCitation", "Annotate Table Citation", "Annotate selected words as a table citation and select cited table(s).") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return annotateCaptionCitation(idmp.document, true, start, end, idmp);
				}
			});
		else actions.add(new SelectionAction("annotateFigureCitation", "Annotate Figure Citation", "Annotate selected words as a figure citation and select cited figure(s).") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				return annotateCaptionCitation(idmp.document, false, start, end, idmp);
			}
		});
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	private String getStringFrom(ImWord start) {
		if ((start.getNextRelation() != ImWord.NEXT_RELATION_CONTINUE) && (start.getNextRelation() != ImWord.NEXT_RELATION_HYPHENATED))
			return start.getString();
		ImWord end = start;
		while ((end.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE) || (end.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED))
			end = end.getNextWord();
		return ImUtils.getString(start, end, true);
	}
	
	private ImWord getFollowingWordStart(ImWord start) {
		while ((start.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE) || (start.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED))
			start = start.getNextWord();
		return start.getNextWord();
	}
	
	private boolean annotateCaptionCitation(ImDocument doc, boolean isTableCitation, ImWord start, ImWord end, ImDocumentMarkupPanel idmp) {
		
		//	get applicable captions
		ImAnnotation[] allCaptions = doc.getAnnotations(ImAnnotation.CAPTION_TYPE);
		ArrayList captions = new ArrayList();
		for (int c = 0; c < allCaptions.length; c++) {
//			if (isTableCitation == allCaptions[c].getFirstWord().getString().toLowerCase().startsWith("tab"))
			if (isTableCitation == this.getStringFrom(allCaptions[c].getFirstWord()).toLowerCase().startsWith("tab"))
				captions.add(allCaptions[c]);
		}
		
		//	anything to work with?
		if (captions.isEmpty()) {
			DialogFactory.alert(("There are no" + (isTableCitation ? " table" : "") + " captions in the document to cite.\r\nPlease first mark the caption referenced by the selected words."), "No Captions To Cite", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		//	extract any numbering from selected text
		HashSet refNumbers = getRefNumbers(start, end, false);
//		System.out.println("Got caption reference numbers from '" + ImUtils.getString(start, end, true) + "': " + refNumbers);
		
		//	prepare getting user input
		JLabel captionLabel = new JLabel(("<HTML><B>Select caption(s) referenced by '" + ImUtils.getString(start, end, true) + "'</B></HTML>"), JLabel.CENTER);
		JPanel captionPanel = new JPanel(new GridLayout(0, 1), true);
		captionPanel.add(captionLabel);
		ArrayList captionSelectors = new ArrayList();
		int lastCheckedSelectorIndex = -1;
		for (int c = 0; c < captions.size(); c++) {
			ImAnnotation caption = ((ImAnnotation) captions.get(c));
			int captionStartWords = 0;
			ImWord captionStartEnd = caption.getFirstWord();
			for (ImWord imw = caption.getFirstWord(); imw != null; imw = imw.getNextWord()) {
				captionStartWords++;
				captionStartEnd = imw;
				if (imw == caption.getLastWord())
					break;
				if (captionStartWords > 15) // TODOne is 15 a good threshold? ==> looks that way
					break;
			}
			JCheckBox captionSelector = new JCheckBox(ImUtils.getString(caption.getFirstWord(), captionStartEnd, true));
			captionPanel.add(captionSelector);
			captionSelectors.add(captionSelector);
			HashSet captionRefNumbers = getRefNumbers(caption.getFirstWord(), captionStartEnd, true);
//			System.out.println("Got caption numbers from '" + ImUtils.getString(caption.getFirstWord(), captionStartEnd, true) + "': " + captionRefNumbers);
			captionRefNumbers.retainAll(refNumbers);
//			System.out.println(" ==> shared with reference are " + captionRefNumbers);
			if (captionRefNumbers.size() != 0) {
				captionSelector.setSelected(true);
				lastCheckedSelectorIndex = c;
			}
		}
		
		//	use scroll pane if more than 10 captions to choose from
		if (captionSelectors.size() > 10) {
			Dimension cpps = captionPanel.getPreferredSize();
			JScrollPane captionPanelBox = new JScrollPane(captionPanel);
			captionPanelBox.getVerticalScrollBar().setUnitIncrement(50);
			captionPanelBox.getVerticalScrollBar().setBlockIncrement(50);
			captionPanel = new JPanel(new BorderLayout(), true);
			captionPanel.add(captionLabel, BorderLayout.NORTH);
			captionPanel.add(captionPanelBox, BorderLayout.CENTER);
			if (cpps != null) {
				captionPanel.setPreferredSize(new Dimension((cpps.width + 32), 250));
				if (lastCheckedSelectorIndex != -1) {
					int captionSelectorHeight = (cpps.height / captions.size());
					captionPanelBox.getVerticalScrollBar().setValue(lastCheckedSelectorIndex * captionSelectorHeight);
				}
			}
		}
		
		//	prompt
		int choice = DialogFactory.confirm(captionPanel, "Select Cited Captions", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (choice != JOptionPane.OK_OPTION)
			return false;
		
		//	get selected captions
		ArrayList citedCaptions = new ArrayList();
		for (int c = 0; c < Math.min(captions.size(), captionSelectors.size()); c++) {
			JCheckBox captionSelector = ((JCheckBox) captionSelectors.get(c));
			if (captionSelector.isSelected())
				citedCaptions.add(captions.get(c));
		}
		
		//	anything selected?
		if (citedCaptions.isEmpty())
			return false;
		
		//	add annotation ...
		String captionCitationType =  this.annotateCaptionCitation(doc, (isTableCitation ? "table" : "figure"), start, end, citedCaptions);
		
		//	... and make it show
		if (idmp != null)
			idmp.setAnnotationsPainted(captionCitationType, true);
		return true;
	}
	
	private static HashSet getRefNumbers(ImWord start, ImWord end, boolean stopAfterFirst) {
		HashSet refNumbers = new HashSet();
		String lastRefNumber = null;
		char lastRefNumberType = ' ';
		for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
			String imwString = imw.getString();
			if (imwString == null)
				continue;
			
			//	Arabic numbering
			if (imwString.matches("[1-9][0-9]{0,2}")) {
				refNumbers.add(imwString.toLowerCase());
				if ((lastRefNumber != null) && (lastRefNumberType == 'A')) try {
					for (int rn = Integer.parseInt(lastRefNumber); rn < Integer.parseInt(imwString); rn++)
						refNumbers.add("" + rn);
				} catch (NumberFormatException nfe) {}
				lastRefNumber = imwString.toLowerCase();
				lastRefNumberType = 'A';
			}
			
			//	upper case Roman numbering
			else if (imwString.matches("[IVXL]+")) {
				refNumbers.add(imwString.toLowerCase());
				if ((lastRefNumber != null) && (lastRefNumberType == 'R')) try {
					for (int rn = StringUtils.parseRomanNumber(lastRefNumber); rn < StringUtils.parseRomanNumber(imwString); rn++) {
						refNumbers.add(StringUtils.asRomanNumber(rn, false).toLowerCase());
						refNumbers.add(StringUtils.asRomanNumber(rn, true).toLowerCase());
					}
				} catch (NumberFormatException nfe) {}
				lastRefNumber = imwString.toLowerCase();
				lastRefNumberType = 'R';
			}
			
			//	lower case Roman numbering
			else if (imwString.matches("[ivxl]+")) {
				refNumbers.add(imwString.toLowerCase());
				if ((lastRefNumber != null) && (lastRefNumberType == 'r')) try {
					for (int rn = StringUtils.parseRomanNumber(lastRefNumber); rn < StringUtils.parseRomanNumber(imwString); rn++) {
						refNumbers.add(StringUtils.asRomanNumber(rn, false).toLowerCase());
						refNumbers.add(StringUtils.asRomanNumber(rn, true).toLowerCase());
					}
				} catch (NumberFormatException nfe) {}
				lastRefNumber = imwString.toLowerCase();
				lastRefNumberType = 'r';
			}
			
			//	range marker, retain range start
			else if (imwString.matches("[\\-\\u00AD\\u2010\\u2011\\u2012\\u2013\\u2014\\u2015\\u2212]+")) {}
			
			//	other token, clean up
			else {
				lastRefNumber = null;
				lastRefNumberType = ' ';
				if (stopAfterFirst && (refNumbers.size() != 0))
					break; // seems we have collected enough
			}
			
			//	we're done here
			if (imw == end)
				break;
		}
		return refNumbers;
	}
	
	private HashMap documentCaptionStarts = new HashMap();
	private TreeSet getDocumentCaptionStarts(ImDocument doc, boolean create) {
		TreeSet captionStarts = ((TreeSet) this.documentCaptionStarts.get(doc.docId));
		
		//	collect caption starts if not done before
		if ((captionStarts == null) && create) {
			captionStarts = new TreeSet(String.CASE_INSENSITIVE_ORDER);
			ImAnnotation[] captions = doc.getAnnotations(ImAnnotation.CAPTION_TYPE);
			for (int c = 0; c < captions.length; c++)
				this.addCaptionStarts(captionStarts, doc, captions[c]);
			this.documentCaptionStarts.put(doc.docId, captionStarts);
		}
		
		//	finally ...
		return captionStarts;
	}
	
	private void addCaptionStarts(TreeSet captionStarts, ImDocument doc, ImAnnotation caption) {
		this.addCaptionStarts(captionStarts, caption.getFirstWord());
		String subCaptionStartIdStr = ((String) caption.getAttribute("subCaptionStartIDs"));
		if (subCaptionStartIdStr == null)
			return;
		String subCaptionStartIDs[] = subCaptionStartIdStr.split("\\s+");
		for (int i = 0; i < subCaptionStartIDs.length; i++)
			this.addCaptionStarts(captionStarts, doc.getWord(subCaptionStartIDs[i]));
	}
	
	private void addCaptionStarts(TreeSet captionStarts, ImWord fromWord) {
		if (fromWord == null)
			return;
		String captionStart = this.getStringFrom(fromWord);
		if (captionStartMappings.containsKey(captionStart))
			captionStart = ((String) captionStartMappings.get(captionStart));
		if (captionStartAbbreviationMappings.containsKey(captionStart))
			captionStarts.addAll((Set) captionStartAbbreviationMappings.get(captionStart));
		for (int e = 3; e <= (captionStart.length() - (captionStart.toLowerCase().endsWith("s") ? 1 : 0)); e++) {
			captionStarts.add(captionStart.substring(0, e));
			captionStarts.add(captionStart.substring(0, e) + "s");
		}
	}
	
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentOpened(de.uka.ipd.idaho.im.ImDocument, java.lang.Object, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
	 */
	public void documentOpened(ImDocument doc, Object source, ProgressMonitor pm) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentSelected(de.uka.ipd.idaho.im.ImDocument)
	 */
	public void documentSelected(ImDocument doc) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentSaving(de.uka.ipd.idaho.im.ImDocument, java.lang.Object, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
	 */
	public void documentSaving(ImDocument doc, Object dest, ProgressMonitor pm) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentSaved(de.uka.ipd.idaho.im.ImDocument, java.lang.Object, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
	 */
	public void documentSaved(ImDocument doc, Object dest, ProgressMonitor pm) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentClosed(java.lang.String)
	 */
	public void documentClosed(String docId) {
		this.documentCaptionStarts.remove(docId);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#attributeChanged(de.uka.ipd.idaho.im.ImObject, java.lang.String, java.lang.Object, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void attributeChanged(ImObject object, String attributeName, Object oldValue, ImDocumentMarkupPanel idmp, boolean allowPrompt) {
		
		//	write through modifications of a caption annotation to figure and table citations
		if ((object instanceof ImAnnotation) && CAPTION_TYPE.equals(object.getType())) {
			
			//	are we interested in this update?
			if (true 
				&& !CAPTION_TARGET_BOX_ATTRIBUTE.equals(attributeName)
				&& !CAPTION_TARGET_PAGE_ID_ATTRIBUTE.equals(attributeName)
				&& !ImAnnotation.FIRST_WORD_ATTRIBUTE.equals(attributeName)
				&& !ImAnnotation.LAST_WORD_ATTRIBUTE.equals(attributeName)
				&& !"httpUri".equals(attributeName)
				&& !"subCaptionStartIDs".equals(attributeName)
				)
				return;
			
			//	get document
			ImDocument doc = object.getDocument();
			if (doc == null)
				return;
			
			//	clean up document caption starts
			if (ImAnnotation.FIRST_WORD_ATTRIBUTE.equals(attributeName))
				this.documentCaptionStarts.remove(doc.docId);
			
			//	if sub caption start IDs changed, we might have to mark further caption citations (==> treat just like newly added caption)
			if ("subCaptionStartIDs".equals(attributeName)) {
				this.annotationAdded(((ImAnnotation) object), idmp, allowPrompt);
				return;
			}
			
			//	get caption attributes
			ImAnnotation caption = ((ImAnnotation) object);
			ImWord captionStartIdWord = (((oldValue instanceof ImWord) && ImAnnotation.FIRST_WORD_ATTRIBUTE.equals(attributeName)) ? ((ImWord) oldValue) : caption.getFirstWord());
			boolean captionStartIdWordIsTable = captionStartIdWord.getString().toLowerCase().startsWith("tab");
			
			//	update start ID
			if (ImAnnotation.FIRST_WORD_ATTRIBUTE.equals(attributeName))
				caption.setAttribute("startId", caption.getFirstWord().getLocalID());
			
			//	get affected caption citations
			Map captionCitations = this.findCaptionCitations(captionStartIdWord, captionStartIdWordIsTable, doc);
			if (captionCitations.isEmpty())
				return;
			
			//	produce update attribute values
			String captionStartId = caption.getFirstWord().getLocalID();
			String captionStart = this.getCaptionStart(caption);
			String captionText = ImUtils.getString(caption.getFirstWord(), caption.getLastWord(), true);
			String captionTargetPageId = ((String) caption.getAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE));
			if (captionTargetPageId == null)
				return;
			String captionTargetBoxString = ((String) caption.getAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE));
			if (captionTargetBoxString == null)
				return;
			String captionTargetId = this.getCaptionTargetSupplementId(doc.getPage(Integer.parseInt(captionTargetPageId)), BoundingBox.parse(captionTargetBoxString));
			boolean captionStartWordIsTable = caption.getFirstWord().getString().toLowerCase().startsWith("tab");
			
			//	update caption citations
			for (Iterator ccit = captionCitations.keySet().iterator(); ccit.hasNext();) {
				ImAnnotation captionCitation = ((ImAnnotation) ccit.next());
				String attributeNameSuffix = ((String) captionCitations.get(captionCitation));
				
				//	modify attribute caption target attributes
				if (CAPTION_TARGET_BOX_ATTRIBUTE.equals(attributeName)) {
					captionCitation.setAttribute(("captionTargetBox" + attributeNameSuffix), captionTargetBoxString);
					if (captionTargetId != null)
						captionCitation.setAttribute(("captionTargetId" + attributeNameSuffix), captionTargetId);
				}
				else if (CAPTION_TARGET_PAGE_ID_ATTRIBUTE.equals(attributeName)) {
					captionCitation.setAttribute(("captionTargetPageId" + attributeNameSuffix), captionTargetPageId);
					if (captionTargetId != null)
						captionCitation.setAttribute(("captionTargetId" + attributeNameSuffix), captionTargetId);
				}
				else if ("httpUri".equals(attributeName))
					captionCitation.setAttribute(("httpUri" + attributeNameSuffix), object.getAttribute(attributeName));
				
				//	modify attributes changing with caption start and end
				else if (ImAnnotation.FIRST_WORD_ATTRIBUTE.equals(attributeName)) {
					captionCitation.setAttribute(("captionStartId" + attributeNameSuffix), captionStartId);
					captionCitation.setAttribute(("captionStart" + attributeNameSuffix), captionStart);
					captionCitation.setAttribute(("captionText" + attributeNameSuffix), captionText);
					if (captionStartIdWordIsTable != captionStartWordIsTable)
						captionCitation.setType((captionStartWordIsTable ? ImRegion.TABLE_TYPE : "figure") + CITATION_TYPE_SUFFIX);
				}
				else if (ImAnnotation.LAST_WORD_ATTRIBUTE.equals(attributeName)) {
					captionCitation.setAttribute(("captionStart" + attributeNameSuffix), captionStart);
					captionCitation.setAttribute(("captionText" + attributeNameSuffix), captionText);
				}
			}
		}
		
		//	write through modifications of a caption word to figure and table citations
		else if ((object instanceof ImWord) && ImWord.TEXT_STREAM_TYPE_CAPTION.equals(((ImWord) object).getTextStreamType()) && ImWord.STRING_ATTRIBUTE.equals(attributeName)) {
			
			//	get document
			ImDocument doc = object.getDocument();
			if (doc == null)
				return;
			
			//	clean up document caption starts
			if (ImAnnotation.FIRST_WORD_ATTRIBUTE.equals(attributeName))
				this.documentCaptionStarts.remove(doc.docId);
			
			//	find affected caption
			ImAnnotation[] wordCaptions = doc.getAnnotationsSpanning((ImWord) object);
			if (wordCaptions.length != 1)
				return;
			
			//	get caption attributes
			ImAnnotation caption = wordCaptions[0];
			boolean captionStartIdWordIsTable = ((caption.getFirstWord() == ((ImWord) object)) ? ((String) oldValue) : caption.getFirstWord().getString()).toLowerCase().startsWith("tab");
			
			//	get affected caption citations
			Map captionCitations = this.findCaptionCitations(caption.getFirstWord(), captionStartIdWordIsTable, doc);
			if (captionCitations.isEmpty())
				return;
			
			//	produce update attribute values
			String captionStart = this.getCaptionStart(caption);
			String captionText = ImUtils.getString(caption.getFirstWord(), caption.getLastWord(), true);
			boolean captionStartWordIsTable = caption.getFirstWord().getString().toLowerCase().startsWith("tab");
			
			//	update caption citations
			for (Iterator ccit = captionCitations.keySet().iterator(); ccit.hasNext();) {
				ImAnnotation captionCitation = ((ImAnnotation) ccit.next());
				String attributeNameSuffix = ((String) captionCitations.get(captionCitation));
				
				//	modify attributes changing with caption start and end
				captionCitation.setAttribute(("captionStart" + attributeNameSuffix), captionStart);
				captionCitation.setAttribute(("captionText" + attributeNameSuffix), captionText);
				if (captionStartIdWordIsTable != captionStartWordIsTable)
					captionCitation.setType((captionStartWordIsTable ? ImRegion.TABLE_TYPE : "figure") + CITATION_TYPE_SUFFIX);
			}
		}
		
		if (!CAPTION_TARGET_BOX_ATTRIBUTE.equals(attributeName) && !CAPTION_TARGET_PAGE_ID_ATTRIBUTE.equals(attributeName))
			return;
		if (!object.hasAttribute(attributeName))
			return;
		
		//	get document
		ImDocument doc = object.getDocument();
		if (doc == null)
			return;
		
		//	get caption, identifying start string, and caption citations
		ImAnnotation caption = ((ImAnnotation) object);
		ImAnnotation[] captionCitations = doc.getAnnotations((caption.getFirstWord().getString().toLowerCase().startsWith("tab") ? ImRegion.TABLE_TYPE : "figure") + CITATION_TYPE_SUFFIX);
		
		//	work off citations one by one
		for (int cc = 0; cc < captionCitations.length; cc++) {
			String attributeNameSuffix = null;
			
			//	find affected attribute group
			if (captionCitations[cc].hasAttribute("captionStartId-0"))
				for (int s = 0;; s++) {
					if (!captionCitations[cc].hasAttribute("captionStartId-" + s))
						break;
					if (caption.getFirstWord().getLocalID().equals(captionCitations[cc].getAttribute("captionStartId-" + s))) {
						attributeNameSuffix = ("-" + s);
						break;
					}
				}
			if (captionCitations[cc].hasAttribute("captionStartId") && caption.getFirstWord().getLocalID().equals(captionCitations[cc].getAttribute("captionStartId")))
				attributeNameSuffix = "";
			
			//	no caption start match
			if (attributeNameSuffix == null)
				continue;
			
			//	modify attribute
			if (CAPTION_TARGET_BOX_ATTRIBUTE.equals(attributeName))
				captionCitations[cc].setAttribute(("captionTargetBox" + attributeNameSuffix), caption.getAttribute(CAPTION_TARGET_BOX_ATTRIBUTE));
			else if (CAPTION_TARGET_PAGE_ID_ATTRIBUTE.equals(attributeName))
				captionCitations[cc].setAttribute(("captionTargetPageId" + attributeNameSuffix), caption.getAttribute(CAPTION_TARGET_PAGE_ID_ATTRIBUTE));
		}
	}
	
	private Map findCaptionCitations(ImWord captionStart, boolean isTableCaption, ImDocument doc) {
		ImAnnotation[] allCaptionCitations = doc.getAnnotations((isTableCaption ? ImRegion.TABLE_TYPE : "figure") + CITATION_TYPE_SUFFIX);
		HashMap captionCitationsToAttributeNameSuffixes = new HashMap();
		for (int cc = 0; cc < allCaptionCitations.length; cc++) {
			String attributeNameSuffix = null;
			
			//	find affected attribute group
			if (allCaptionCitations[cc].hasAttribute("captionStartId-0"))
				for (int s = 0;; s++) {
					if (!allCaptionCitations[cc].hasAttribute("captionStartId-" + s))
						break;
					if (captionStart.getLocalID().equals(allCaptionCitations[cc].getAttribute("captionStartId-" + s))) {
						attributeNameSuffix = ("-" + s);
						break;
					}
				}
			if (captionStart.getLocalID().equals(allCaptionCitations[cc].getAttribute("captionStartId")))
				attributeNameSuffix = "";
			
			//	map caption citation to attribute suffix for further handling
			if (attributeNameSuffix != null)
				captionCitationsToAttributeNameSuffixes.put(allCaptionCitations[cc], attributeNameSuffix);
		}
		
		//	return what we got
		return captionCitationsToAttributeNameSuffixes;
	}
	
	/* TODO move this thing to ImUtils, and use it
	 * - here
	 * - in RegionActionProvider
	 * - in FigureTableExporter
	 * - wherever ...
	 */
	/* TODO make this a CaptionStart object, comprising
	 * - the caption start string ...
	 * - ... as well as possible abbreviations
	 * - the type of numbering scheme (1-12, I-XII, i-xii, A-F, or a-f)
	 * - whether or not a sub index is used (likely most often letters below Arabic numbers, but it might be the other way around)
	 * - the minimum and maximum top level number
	 * - the minimum and maximum sub index (likely only with single top level numbers)
	 */
	private String getCaptionStart(ImAnnotation caption) {
		ImWord captionStartEnd = null;
		char indexType = ((char) 0);
		boolean lastWasIndex = false;
		boolean lastWasPeriod = false;
		boolean startIsBold = caption.getFirstWord().hasAttribute(ImWord.BOLD_ATTRIBUTE);
		for (ImWord imw = caption.getFirstWord().getNextWord(); imw != null; imw = imw.getNextWord()) {
			if (imw.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE)
				continue;
			
			//	get and check string value
			String imwString = imw.getString();
			if ((imwString == null) || (imwString.trim().length() == 0))
				continue;
			
			//	if we've started out in bold face, we stop soon as bold face ends (cutting some slack for punctuation marks)
			if (startIsBold && !imw.hasAttribute(ImWord.BOLD_ATTRIBUTE) && Character.isLetterOrDigit(imwString.charAt(0)))
				break;
			
			//	Arabic index number
			if (imwString.matches("[0-9]+(\\s*[a-z])?")) {
				if (indexType == 0)
					indexType = 'A';
				else if (indexType != 'A')
					break;
				captionStartEnd = imw;
				lastWasIndex = true;
				lastWasPeriod = false;
			}
			
			//	lower case Roman index number
			else if (imwString.matches("[ivxl]+")) {
				if (indexType == 0)
					indexType = 'r';
				else if ((indexType == 'l') && (imwString.length() == 1)) { /* need to allow 'a-i', etc. ... */ }
				else if (indexType != 'r')
					break;
				captionStartEnd = imw;
				lastWasIndex = true;
				lastWasPeriod = false;
			}
			
			//	upper case Roman index number
			else if (imwString.matches("[IVXL]+")) {
				if (indexType == 0)
					indexType = 'R';
				else if ((indexType == 'L') && (imwString.length() == 1)) { /* need to allow 'A-I', etc. ... */ }
				else if (indexType != 'R')
					break;
				captionStartEnd = imw;
				lastWasIndex = true;
				lastWasPeriod = false;
			}
			
			//	lower case index letter
			else if (imwString.matches("[a-z]")) {
				if (indexType == 0)
					indexType = 'l';
				else if (indexType != 'l')
					break;
				captionStartEnd = imw;
				lastWasIndex = true;
				lastWasPeriod = false;
			}
			
			//	upper case index letter
			else if (imwString.matches("[A-Z]")) {
				if (indexType == 0)
					indexType = 'L';
				else if (indexType != 'L')
					break;
				captionStartEnd = imw;
				lastWasIndex = true;
				lastWasPeriod = false;
			}
			
			//	enumeration separator or range marker
			else if (",".equals(imwString) || "&".equals(imwString) || imwString.matches("[\\-\\u00AD\\u2010-\\u2015\\u2212]+") || (";and;und;et;y;".indexOf(";" + imwString.toLowerCase() + ";") != -1)) {
				if (!lastWasIndex && !lastWasPeriod) // no enumeration or range open, we're done here
					break;
				lastWasIndex = false;
				lastWasPeriod = false;
			}
			
			//	ignore dots right after index numbers
			else if (".".equals(imwString)) {
				if (!lastWasIndex) // no enumeration or range open, we're done here
					break;
				lastWasIndex = false;
				lastWasPeriod = true;
			}
			
			//	nothing to work with here
			else break;
		}
		
		//	get full first word of caption if index not found
		if (captionStartEnd == null) {
			for (ImWord imw = caption.getFirstWord(); imw != null; imw = imw.getNextWord())
				if (imw.getNextRelation() != ImWord.NEXT_RELATION_CONTINUE) {
					captionStartEnd = imw;
					break;
				}
		}
		
		//	finally ...
		return ImUtils.getString(caption.getFirstWord(), ((captionStartEnd == null) ? caption.getLastWord() : captionStartEnd), true);
	}
	
	private String getCaptionTargetSupplementId(ImPage targetPage, BoundingBox captionTargetBox) {
		ImSupplement[] targetPageSupplements = targetPage.getSupplements();
		for (int s = 0; s < targetPageSupplements.length; s++) {
			BoundingBox pageSupplementBox;
			if (targetPageSupplements[s] instanceof Figure)
				pageSupplementBox = ((Figure) targetPageSupplements[s]).getBounds();
			else if (targetPageSupplements[s] instanceof Graphics)
				pageSupplementBox = ((Graphics) targetPageSupplements[s]).getBounds();
			else continue;
			if ((pageSupplementBox != null) && captionTargetBox.liesIn(pageSupplementBox, true) && pageSupplementBox.liesIn(captionTargetBox, true))
				return targetPageSupplements[s].getId();
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#annotationAdded(de.uka.ipd.idaho.im.ImAnnotation, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void annotationAdded(ImAnnotation annotation, ImDocumentMarkupPanel idmp, boolean allowPrompt) {
		
		//	we're only interested in captions
		if (!CAPTION_TYPE.equals(annotation.getType()))
			return;
		
		//	get document
		ImDocument doc = annotation.getDocument();
		if (doc == null)
			return;
		
		//	update document caption starts
		TreeSet captionStarts = this.getDocumentCaptionStarts(doc, false);
		if (captionStarts != null)
			this.addCaptionStarts(captionStarts, doc, annotation);
		
		//	annotate caption citations
		this.annotateCaptionCitations(doc, annotation, idmp);
	}
	
	private boolean annotateCaptionCitations(ImDocument doc, ImAnnotation caption, ImDocumentMarkupPanel idmp) {
		
		//	collect caption start words, and generate all their prefixes
		TreeMap captionStartsToNumbers = this.getCaptionStartNumberIndex(doc);
		
		//	tag and connect caption citations TODO use caption citation properties from document style (patterns, first and foremost)
		ImWord[] textStreamHeads = doc.getTextStreamHeads();
		boolean ccMarked = false;
		for (int h = 0; h < textStreamHeads.length; h++) {
			if (ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(textStreamHeads[h].getTextStreamType()))
				ccMarked = (this.markCaptionCitations(doc, textStreamHeads[h], captionStartsToNumbers, caption) || ccMarked);
		}
		
		//	remove nested caption citations
		this.removeNestedAnnotations(doc, doc.getAnnotations("figure" + CITATION_TYPE_SUFFIX));
		this.removeNestedAnnotations(doc, doc.getAnnotations(ImRegion.TABLE_TYPE + CITATION_TYPE_SUFFIX));
		
		//	make any changes show
		if (ccMarked && (idmp != null)) {
			idmp.setAnnotationsPainted(("figure" + CITATION_TYPE_SUFFIX), true);
			idmp.setAnnotationsPainted((ImRegion.TABLE_TYPE + CITATION_TYPE_SUFFIX), true);
		}
		
		//	did we change anything?
		return ccMarked;
	}
	
	private void removeNestedAnnotations(ImDocument doc, ImAnnotation[] annots) {
		if (annots.length < 2)
			return;
		Arrays.sort(annots, annotationNestingOrder);
		ImAnnotation lastAnnot = annots[0];
		for (int a = 1; a < annots.length; a++) {
			
			//	different text streams, cannot be nested
			if (!lastAnnot.getFirstWord().getTextStreamId().equals(annots[a].getFirstWord().getTextStreamId())) {
				lastAnnot = annots[a];
				continue;
			}
			
			//	no overlap
			if (ImUtils.textStreamOrder.compare(lastAnnot.getLastWord(), annots[a].getFirstWord()) < 0) {
				lastAnnot = annots[a];
				continue;
			}
			
			//	non-including overlap, merge
			if (ImUtils.textStreamOrder.compare(lastAnnot.getLastWord(), annots[a].getLastWord()) < 0) {
				lastAnnot.setLastWord(annots[a].getLastWord());
				AttributeUtils.copyAttributes(lastAnnot, annots[a], AttributeUtils.ADD_ATTRIBUTE_COPY_MODE);
			}
			
			//	remove nested annotation
			doc.removeAnnotation(annots[a]);
		}
	}
	
	private static final Comparator annotationNestingOrder = new Comparator() {
		public int compare(Object obj1, Object obj2) {
			ImAnnotation annot1 = ((ImAnnotation) obj1);
			ImAnnotation annot2 = ((ImAnnotation) obj2);
			int c = ImUtils.textStreamOrder.compare(annot1.getFirstWord(), annot2.getFirstWord());
			return ((c == 0) ? ImUtils.textStreamOrder.compare(annot2.getLastWord(), annot1.getLastWord()) : c);
		}
	};
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractReactionProvider#annotationRemoved(de.uka.ipd.idaho.im.ImAnnotation, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void annotationRemoved(ImAnnotation annotation, ImDocumentMarkupPanel idmp, boolean allowPrompt) {
		
		//	we're only interested in captions
		if (!CAPTION_TYPE.equals(annotation.getType()))
			return;
		
		//	get document
		ImDocument doc = annotation.getDocument();
		if (doc == null)
			return;
		
		//	clean up document caption starts
		this.documentCaptionStarts.remove(doc.docId);
	}
	
	/**
	 * Mark caption citations in a document. This method expects captions to be
	 * already marked, seeking only citations of theirs.
	 * @param doc the document to process
	 */
	public void markCaptionCitations(ImDocument doc) {
		this.markCaptionCitations(doc, doc.getTextStreamHeads());
	}
	
	/**
	 * Mark caption citations in a document. This method expects captions to be
	 * already marked, seeking only citations of theirs.
	 * @param doc the document to process
	 * @param textStreamHeads the heads of the logical text streams to analyze
	 */
	public void markCaptionCitations(ImDocument doc, ImWord[] textStreamHeads) {
		
		//	collect caption start words, and generate all their prefixes
		TreeMap captionStartsToNumbers = this.getCaptionStartNumberIndex(doc);
		
		//	tag and connect caption citations TODO use caption citation properties from document style (patterns, first and foremost)
		for (int h = 0; h < textStreamHeads.length; h++) {
			if (ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(textStreamHeads[h].getTextStreamType()))
				this.markCaptionCitations(doc, textStreamHeads[h], captionStartsToNumbers, null);
		}
	}
	
	private TreeMap getCaptionStartNumberIndex(ImDocument doc) {
		TreeMap captionStartsToNumbers = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		
		//	go caption by caption
		ImAnnotation[] captions = doc.getAnnotations(CAPTION_TYPE);
		for (int c = 0; c < captions.length; c++) {
			if (DEBUG_MARK_CAPTION_CITATIONS)
				System.out.println("Analyzing caption (page " + captions[c].getFirstWord().pageId + ") '" + ImUtils.getString(captions[c].getFirstWord(), captions[c].getLastWord(), true) + "'");
			this.indexCaptionStartNumbers(captions[c], captions[c].getFirstWord(), captionStartsToNumbers);
			String subCaptionStartIdStr = ((String) captions[c].getAttribute("subCaptionStartIDs"));
			if (subCaptionStartIdStr == null)
				continue;
			String subCaptionStartIDs[] = subCaptionStartIdStr.split("\\s+");
			for (int i = 0; i < subCaptionStartIDs.length; i++)
				this.indexCaptionStartNumbers(captions[c], doc.getWord(subCaptionStartIDs[i]), captionStartsToNumbers);
//			String captionStart = null;
//			int minNumber = -1;
//			int maxNumber = -1;
//			boolean numberRange = false;
//			int minLetter = -1;
//			int maxLetter = -1;
//			int letterBase = 0;
//			boolean letterRange = false;
//			//	TODOne also handle sub caption starts
//			for (ImWord imw = captions[c].getFirstWord(); imw != null; imw = imw.getNextWord()) {
//				String imwString = imw.getString();
//				if (imwString == null)
//					continue;
//				
//				//	mark caption start on first word, quit at next word
//				if ((imwString.length() >= 3) && Gamta.isWord(imwString)) {
//					if (captionStart == null) {
//						captionStart = imwString;
//						continue;
//					}
//					else break;
//				}
//				
//				//	nothing to work with yet
//				if (captionStart == null)
//					continue;
//				
//				//	number
//				if (imwString.matches("[1-9][0-9]{0,2}")) {
//					if (minNumber == -1) // start of range
//						minNumber = Integer.parseInt(imwString);
//					else if (!numberRange) // no range expected, quit
//						break;
//					else if (maxNumber == -1) // end of range
//						maxNumber = Integer.parseInt(imwString);
//					else break; // we've had enough numbers here
//				}
//				
//				//	lower case index letter
//				else if (imwString.matches("[a-z]")) {
//					if (minLetter == -1) { // start of range
//						minLetter = (Character.toLowerCase(imwString.charAt(0)) - 'a');
//						letterBase = 'a';
//					}
//					else if (!letterRange) // no range expected, quit
//						break;
//					else if (maxLetter == -1) // end of range
//						maxLetter = (Character.toLowerCase(imwString.charAt(0)) - 'a');
//					else break; // we've had enough index letters here
//				}
//				
//				//	upper case index letter
//				else if (imwString.matches("[A-Z]")) {
//					if (minLetter == -1) { // start of range
//						minLetter = (Character.toUpperCase(imwString.charAt(0)) - 'A');
//						letterBase = 'A';
//					}
//					else if (!letterRange) // no range expected, quit
//						break;
//					else if (maxLetter == -1) // end of range
//						maxLetter = (Character.toUpperCase(imwString.charAt(0)) - 'A');
//					else break; // we've had enough index letters here
//				}
//				
//				//	separator
//				else if (imwString.matches("[\\,\\&\\-\\u00AD\\u2010-\\u2015\\u2212]+")) {
//					if ((minLetter != -1) && (maxLetter == -1)) // indicator of letter range
//						letterRange = true;
//					else if ((minNumber != -1) && (maxNumber == -1)) // indicator of number range
//						numberRange = true;
//					else break; // other punctuation, quit
//				}
//				
//				//	ignore punctuation mark
//				else if (".:,;".indexOf(imwString) == -1)
//					break;
//			}
//			
//			//	expand common abbreviations
//			if (captionStartMappings.containsKey(captionStart))
//				captionStart = ((String) captionStartMappings.get(captionStart));
//			
//			//	what do we have?
//			if (DEBUG_MARK_CAPTION_CITATIONS) {
//				System.out.println(" - start is '" + captionStart + "'");
//				System.out.println(" - min number is " + minNumber);
//				System.out.println(" - max number is " + maxNumber);
//				System.out.println(" - min letter is " + ((char) (minLetter + letterBase)));
//				System.out.println(" - max letter is " + ((char) (maxLetter + letterBase)));
//			}
//			
//			//	do we have anything to work with?
//			if (captionStart == null)
//				continue;
//			if ((minNumber == -1) && (minLetter == -1))
//				continue;
//			
//			//	generate set of numbers associated with caption start
//			TreeMap numbers = new TreeMap(String.CASE_INSENSITIVE_ORDER);
//			
//			//	index letters only
//			if (minNumber == -1) {
//				if (maxLetter == -1)
//					numbers.put(("" + ((char) (minLetter + 'a'))), captions[c]);
//				else for (int l = minLetter; l <= maxLetter; l++)
//					numbers.put(("" + ((char) (l + 'a'))), captions[c]);
//			}
//			
//			//	single number, possibly with index letters
//			else if (maxNumber == -1) {
//				numbers.put(("" + minNumber), captions[c]);
//				if (maxLetter != -1) {
//					for (int l = minLetter; l <= maxLetter; l++)
//						numbers.put(("" + minNumber + "" + ((char) (l + letterBase))), captions[c]);
//				}
//				else if (minLetter != -1)
//					numbers.put(("" + minNumber + "" + ((char) (minLetter + letterBase))), captions[c]);
//			}
//			
//			//	number range
//			else for (int n = minNumber; n <= maxNumber; n++)
//				numbers.put(("" + n), captions[c]);
//			
//			//	generate caption start prefixes
//			TreeSet captionStartPrefixes = new TreeSet(String.CASE_INSENSITIVE_ORDER);
//			for (int e = 3; e <= (captionStart.length() - (captionStart.toLowerCase().endsWith("s") ? 1 : 0)); e++) {
//				captionStartPrefixes.add(captionStart.substring(0, e));
//				captionStartPrefixes.add(captionStart.substring(0, e) + "s");
//			}
//			
//			//	map caption start prefixes to numbers
//			for (Iterator cspit = captionStartPrefixes.iterator(); cspit.hasNext();) {
//				String captionStartPrefix = ((String) cspit.next());
//				TreeMap captionStartNumbers = ((TreeMap) captionStartsToNumbers.get(captionStartPrefix));
//				if (captionStartNumbers == null)
//					captionStartsToNumbers.put(captionStartPrefix, numbers);
//				else for (Iterator cnit = numbers.keySet().iterator(); cnit.hasNext();) {
//					String number = ((String) cnit.next());
//					if (!captionStartNumbers.containsKey(number))
//						captionStartNumbers.put(number, captions[c]);
//				}
//			}
//			
//			if (DEBUG_MARK_CAPTION_CITATIONS) {
//				System.out.println(" - got " + numbers.size() + " numbers:");
//				for (Iterator cnit = numbers.keySet().iterator(); cnit.hasNext();)
//					System.out.println("   - " + cnit.next());
//			}
		}
		
		if (DEBUG_MARK_CAPTION_CITATIONS) {
			System.out.println("Got captions:");
			for (Iterator csit = captionStartsToNumbers.keySet().iterator(); csit.hasNext();) {
				String captionStart = ((String) csit.next());
				System.out.print(" - " + captionStart);
				TreeMap captionStartNumbers = ((TreeMap) captionStartsToNumbers.get(captionStart));
				for (Iterator cnit = captionStartNumbers.keySet().iterator(); cnit.hasNext();)
					System.out.print(" " + cnit.next());
				System.out.println();
			}
		}
		
		//	finally ...
		return captionStartsToNumbers;
	}
	
	private void indexCaptionStartNumbers(ImAnnotation caption, ImWord fromWord, TreeMap captionStartsToNumbers) {
		String captionStart = null;
		boolean captionStartIsBold = false;
		
		int minNumber = -1;
		int maxNumber = -1;
		boolean numberRange = false;
		int minLetter = -1;
		int maxLetter = -1;
		int letterBase = 0;
		boolean letterRange = false;
		
		boolean lastWasPunctuation = false;
		for (ImWord imw = fromWord; imw != null; imw = imw.getNextWord()) {
//			String imwString = imw.getString();
			if ((imw.getPreviousRelation() == ImWord.NEXT_RELATION_HYPHENATED) || (imw.getPreviousRelation() == ImWord.NEXT_RELATION_CONTINUE))
				continue;
			String imwString = this.getStringFrom(imw);
			if (imwString == null)
				continue;
			
			//	mark caption start on first word, quit at next word
			if ((imwString.length() >= 3) && Gamta.isWord(imwString)) {
				if (captionStart == null) {
					captionStart = imwString;
					captionStartIsBold = imw.hasAttribute(ImWord.BOLD_ATTRIBUTE);
					continue;
				}
				else break;
			}
			
			//	nothing to work with yet
			if (captionStart == null)
				continue;
			
			//	if we've started out in bold false, we stop soon as bold face ends (cutting some slack for punctuation marks)
			if (captionStartIsBold && !imw.hasAttribute(ImWord.BOLD_ATTRIBUTE) && Character.isLetterOrDigit(imwString.charAt(0)))
				break;
			
			//	number
			if (imwString.matches("[1-9][0-9]{0,2}")) {
				if (minNumber == -1) // start of range
					minNumber = Integer.parseInt(imwString);
				else if (!numberRange) // no range expected, quit
					break;
				else if (maxNumber == -1) // end of range
					maxNumber = Integer.parseInt(imwString);
				else break; // we've had enough numbers here
				lastWasPunctuation = false;
			}
			
			//	lower case index letter
			else if (imwString.matches("[a-z]")) {
				if (minLetter == -1) { // start of range
					minLetter = (Character.toLowerCase(imwString.charAt(0)) - 'a');
					letterBase = 'a';
				}
				else if (!letterRange) // no range expected, quit
					break;
				else if (maxLetter == -1) // end of range
					maxLetter = (Character.toLowerCase(imwString.charAt(0)) - 'a');
				else break; // we've had enough index letters here
				lastWasPunctuation = false;
			}
			
			//	upper case index letter
			else if (imwString.matches("[A-Z]")) {
				if (minLetter == -1) { // start of range
					minLetter = (Character.toUpperCase(imwString.charAt(0)) - 'A');
					letterBase = 'A';
				}
				else if (!letterRange) // no range expected, quit
					break;
				else if (maxLetter == -1) // end of range
					maxLetter = (Character.toUpperCase(imwString.charAt(0)) - 'A');
				else break; // we've had enough index letters here
				lastWasPunctuation = false;
			}
			
			//	range or enumeration separator
			else if (imwString.matches("[\\-\\u00AD\\u2010-\\u2015\\u2212]+") || ",".equals(imwString) || "&".equals(imwString) || (";and;und;et;y;".indexOf(";" + imwString.toLowerCase() + ";") != -1)) {
				if ((minLetter != -1) && (maxLetter == -1)) // indicator of letter range
					letterRange = true;
				else if ((minNumber != -1) && (maxNumber == -1)) // indicator of number range
					numberRange = true;
				else break; // other punctuation, quit
				lastWasPunctuation = !Character.isLetterOrDigit(imwString.charAt(0));
			}
			
			//	ignore punctuation mark after caption start or index letter
			else if (".:,;".indexOf(imwString) != -1) {
				if (lastWasPunctuation)
					break;
				lastWasPunctuation = true;
			}
			
			//	stop at anything else
			else break;
		}
		
		//	expand common abbreviations
		if (captionStartMappings.containsKey(captionStart))
			captionStart = ((String) captionStartMappings.get(captionStart));
		
		//	what do we have?
		if (DEBUG_MARK_CAPTION_CITATIONS) {
			System.out.println(" - start is '" + captionStart + "'");
			System.out.println(" - min number is " + minNumber);
			System.out.println(" - max number is " + maxNumber);
			System.out.println(" - min letter is " + ((char) (minLetter + letterBase)));
			System.out.println(" - max letter is " + ((char) (maxLetter + letterBase)));
		}
		
		//	do we have anything to work with?
		if (captionStart == null)
			return;
		if ((minNumber == -1) && (minLetter == -1))
			return;
		
		//	generate set of numbers associated with caption start
		TreeMap numbers = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		
		//	index letters only
		if (minNumber == -1) {
			if ((maxLetter == -1) || !lastWasPunctuation)
				numbers.put(("" + ((char) (minLetter + 'a'))), caption);
			else for (int l = minLetter; l <= maxLetter; l++)
				numbers.put(("" + ((char) (l + 'a'))), caption);
		}
		
		//	single number, possibly with index letters
		else if (maxNumber == -1) {
			numbers.put(("" + minNumber), caption);
			if ((maxLetter != -1) && lastWasPunctuation) {
				for (int l = minLetter; l <= maxLetter; l++)
					numbers.put(("" + minNumber + "" + ((char) (l + letterBase))), caption);
			}
			else if (minLetter != -1)
				numbers.put(("" + minNumber + "" + ((char) (minLetter + letterBase))), caption);
		}
		
		//	number range
		else if (lastWasPunctuation) {
			for (int n = minNumber; n <= maxNumber; n++)
				numbers.put(("" + n), caption);
		}
		
		//	generate caption start prefixes
		TreeSet captionStartPrefixes = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		for (int e = 3; e <= (captionStart.length() - (captionStart.toLowerCase().endsWith("s") ? 1 : 0)); e++) {
			captionStartPrefixes.add(captionStart.substring(0, e));
			captionStartPrefixes.add(captionStart.substring(0, e) + "s");
		}
		
		//	map caption start prefixes to numbers
		for (Iterator cspit = captionStartPrefixes.iterator(); cspit.hasNext();) {
			String captionStartPrefix = ((String) cspit.next());
			TreeMap captionStartNumbers = ((TreeMap) captionStartsToNumbers.get(captionStartPrefix));
			if (captionStartNumbers == null)
				captionStartsToNumbers.put(captionStartPrefix, numbers);
			else for (Iterator cnit = numbers.keySet().iterator(); cnit.hasNext();) {
				String number = ((String) cnit.next());
				if (!captionStartNumbers.containsKey(number))
					captionStartNumbers.put(number, caption);
			}
		}
		
		if (DEBUG_MARK_CAPTION_CITATIONS) {
			System.out.println(" - got " + numbers.size() + " numbers:");
			for (Iterator cnit = numbers.keySet().iterator(); cnit.hasNext();)
				System.out.println("   - " + cnit.next());
		}
	}
	
	private static final boolean DEBUG_MARK_CAPTION_CITATIONS = true;
	
	private boolean markCaptionCitations(ImDocument doc, ImWord textStreamHead, TreeMap captionStartsToNumbers, ImAnnotation targetCaption) {
		
		//	try marking caption citations in title case first (way faster, and usually sufficient) ...
		return (this.markCaptionCitations(doc, textStreamHead, captionStartsToNumbers, true, targetCaption)
			
			//	... and try case insensitive only if none found
			|| this.markCaptionCitations(doc, textStreamHead, captionStartsToNumbers, false, targetCaption));
	}
	
	private boolean markCaptionCitations(ImDocument doc, ImWord textStreamHead, TreeMap captionStartsToNumbers, boolean titleCaseOnly, ImAnnotation targetCaption) {
		boolean ccMarked = false;
		for (ImWord imw = textStreamHead; imw.getNextWord() != null; imw = imw.getNextWord()) {
			if (imw.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
				continue; // not crossing paragraph boundary with caption citation, so no use starting right before it
//			String imwString = imw.getString();
			String imwString = this.getStringFrom(imw);
			if ((imwString == null) || (imwString.length() == 0))
				continue;
			if (titleCaseOnly && (Character.toTitleCase(imwString.charAt(0)) != imwString.charAt(0)))
				continue;
			TreeMap captionStartNumbers = ((TreeMap) captionStartsToNumbers.get(imwString));
			if ((captionStartNumbers == null) && imwString.toLowerCase().endsWith("s")) // try truncating plural s to get lookup match
				captionStartNumbers = ((TreeMap) captionStartsToNumbers.get(imwString.substring(0, (imwString.length() - "s".length()))));
			if (captionStartNumbers == null)
				continue;
			if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println("Starting caption citation with '" + imwString + "' on page " + imw.pageId + " at " + imw.bounds);
//			imw = this.markCaptionCitation(doc, imw, captionStartNumbers);
			ImWord ccEnd = this.markCaptionCitation(doc, imw, captionStartNumbers, targetCaption);
			ccMarked = (ccMarked || (ccEnd != imw));
			imw = ccEnd;
		}
		return ccMarked;
	}
	
	private ImWord markCaptionCitation(ImDocument doc, ImWord ccHead, TreeMap captionStartNumbers, ImAnnotation targetCaption) {
		
		//	get caption citations
//		ArrayList ccList = this.getCaptionCitations(ccHead, ccHead.getNextWord(), null, captionStartNumbers);
		ImWord ccNumStart = ccHead.getNextWord();
		for (ImWord imw = ccHead; ((imw.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE) || (imw.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED));) {
			imw = imw.getNextWord();
			ccNumStart = imw.getNextWord();
		}
		ArrayList ccList = this.getCaptionCitations(ccHead, ccNumStart, null, captionStartNumbers);
		
		//	anything to work with at all?
		if (ccList.isEmpty())
			return ccHead;
		if (DEBUG_MARK_CAPTION_CITATIONS) {
			System.out.println(" ==> found caption citation sequence '" + ImUtils.getString(ccHead, ((CaptionCitation) ccList.get(ccList.size()-1)).lastWord, true) + "' with " + ccList.size() + " caption citations:");
			for (int c = 0; c < ccList.size(); c++) {
				CaptionCitation cc = ((CaptionCitation) ccList.get(c));
				System.out.println("   - " + ImUtils.getString(cc.firstWord, cc.lastWord, true) + " (" + cc.citedCaptions.size() + " cited captions)");
			}
		}
		
		//	merge caption citations if cited captions overlap or are empty on either side
		for (int c = 0; c < (ccList.size() - 1); c++) {
			CaptionCitation cc = ((CaptionCitation) ccList.get(c));
			CaptionCitation ncc = ((CaptionCitation) ccList.get(c+1));
			boolean mergeCcs = false;
			if (cc.citedCaptions.isEmpty())
				mergeCcs = true;
			else if (ncc.citedCaptions.isEmpty())
				mergeCcs = true;
			else for (Iterator nccit = ncc.citedCaptions.iterator(); nccit.hasNext();)
				if (cc.citedCaptions.contains(nccit.next())) {
					mergeCcs = true;
					break;
				}
			if (mergeCcs) {
				cc.lastWord = ncc.lastWord;
				cc.citedCaptions.addAll(ncc.citedCaptions);
				ccList.remove(c+1);
				c--;
			}
		}
		
		//	check if any cited caption found to link to
		for (int c = 0; c < ccList.size(); c++) {
			CaptionCitation cc = ((CaptionCitation) ccList.get(c));
			if (cc.citedCaptions.isEmpty())
				ccList.remove(c--);
		}
		
		//	anything to reference?
		if (ccList.isEmpty())
			return ccHead;
		if (DEBUG_MARK_CAPTION_CITATIONS) {
			System.out.println(" ==> got " + ccList.size() + " caption citations after merging and cleanup:");
			for (int c = 0; c < ccList.size(); c++) {
				CaptionCitation cc = ((CaptionCitation) ccList.get(c));
				System.out.println("   - " + ImUtils.getString(cc.firstWord, cc.lastWord, true) + " (" + cc.citedCaptions.size() + " cited captions)");
			}
		}
		
		//	mark caption citation, distinguishing table citations from figure citations
		ImWord lastCcLastWord = ccHead;
		for (int c = 0; c < ccList.size(); c++) {
			CaptionCitation cc = ((CaptionCitation) ccList.get(c));
			if ((targetCaption == null) || cc.citedCaptions.contains(targetCaption)) {
//				this.annotateCaptionCitation(doc, ccHead.getString(), cc.firstWord, cc.lastWord, cc.citedCaptions);
				this.annotateCaptionCitation(doc, this.getStringFrom(ccHead), cc.firstWord, cc.lastWord, cc.citedCaptions);
				lastCcLastWord = cc.lastWord;
			}
		}
		
		//	return last word of last caption citation
		return lastCcLastWord;
	}
	
	private ArrayList getCaptionCitations(ImWord ccHead, ImWord ccNumStart, ImWord ccEnd, TreeMap captionStartNumbers) {
		int lastNumber = -1;
		String lastNumberStr = null;
		int lastLetter = -1;
//		String lastLetterStr = null; // we'll need that for Arabic+Roman and Roman+Arabic combinations
		boolean lastWasLetter = false;
		CaptionCitation cc = null;
		ArrayList ccList = new ArrayList();
		//	TODO somehow handle Arabic main numbers with Roman index numbers
		//	TODO somehow handle Roman main numbers with Arabic index numbers
		
		//	TODO insist on increasing order in enumerations to prevent overshooting
		
		//	try and find numbers, starting from successor of start word
		for (ImWord imw = ccNumStart; imw != null; imw = imw.getNextWord()) {
			String imwString = imw.getString();
			if (imwString == null)
				continue;
			
			//	Arabic number
			if (imwString.matches("[1-9][0-9]{0,2}") && ((captionStartNumbers == null) || captionStartNumbers.containsKey(imwString))) {
				if ((lastNumber == -1) || (lastLetter != -1) || lastWasLetter) {
					if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got number " + imwString);
					cc = new CaptionCitation((ccList.isEmpty() ? ccHead : imw), imw);
					ccList.add(cc);
					if (captionStartNumbers != null) {
						ImAnnotation citedCaption = ((ImAnnotation) captionStartNumbers.get(imwString));
						if (citedCaption != null)
							cc.citedCaptions.add(citedCaption);
					}
				}
				else {
					if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got number range " + lastNumber + "-" + imwString);
					cc.lastWord = imw;
					if (captionStartNumbers != null)
						for (int n = lastNumber; n <= Integer.parseInt(imwString); n++) {
							ImAnnotation citedCaption = ((ImAnnotation) captionStartNumbers.get("" + n));
							if (citedCaption != null)
								cc.citedCaptions.add(citedCaption);
						}
				}
				lastNumber = Integer.parseInt(imwString);
				lastNumberStr = imwString;
				lastLetter = -1;
//				lastLetterStr = null;
				lastWasLetter = false;
			}
			
			//	lower case Roman number (for now only if we don't have an Arabic number open)
			else if ((lastNumber == -1) && imwString.matches("[ivxl]+") && StringUtils.isRomanNumber(imwString) && ((captionStartNumbers == null) || captionStartNumbers.containsKey(imwString))) {
				if ((lastNumber == -1) || (lastLetter != -1) || lastWasLetter) {
					if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got number " + imwString);
					cc = new CaptionCitation((ccList.isEmpty() ? ccHead : imw), imw);
					ccList.add(cc);
					if (captionStartNumbers != null) {
						ImAnnotation citedCaption = ((ImAnnotation) captionStartNumbers.get(imwString));
						if (citedCaption != null)
							cc.citedCaptions.add(citedCaption);
					}
				}
				else {
					if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got number range " + lastNumber + "-" + imwString);
					cc.lastWord = imw;
					if (captionStartNumbers != null)
						for (int n = lastNumber; n <= Integer.parseInt(imwString); n++) {
							ImAnnotation citedCaption = ((ImAnnotation) captionStartNumbers.get("" + n));
							if (citedCaption != null)
								cc.citedCaptions.add(citedCaption);
						}
				}
				lastNumber = StringUtils.parseRomanNumber(imwString);
				lastNumberStr = imwString;
				lastLetter = -1;
//				lastLetterStr = null;
				lastWasLetter = false;
			}
			
			//	upper case Roman number (for now only if we don't have an Arabic number open)
			else if ((lastNumber == -1) && imwString.matches("[IVXL]+") && StringUtils.isRomanNumber(imwString) && ((captionStartNumbers == null) || captionStartNumbers.containsKey(imwString))) {
				if ((lastNumber == -1) || (lastLetter != -1) || lastWasLetter) {
					if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got number " + imwString);
					cc = new CaptionCitation((ccList.isEmpty() ? ccHead : imw), imw);
					ccList.add(cc);
					if (captionStartNumbers != null) {
						ImAnnotation citedCaption = ((ImAnnotation) captionStartNumbers.get(imwString));
						if (citedCaption != null)
							cc.citedCaptions.add(citedCaption);
					}
				}
				else {
					if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got number range " + lastNumber + "-" + imwString);
					cc.lastWord = imw;
					if (captionStartNumbers != null)
						for (int n = lastNumber; n <= Integer.parseInt(imwString); n++) {
							ImAnnotation citedCaption = ((ImAnnotation) captionStartNumbers.get("" + n));
							if (citedCaption != null)
								cc.citedCaptions.add(citedCaption);
						}
				}
				lastNumber = StringUtils.parseRomanNumber(imwString);
				lastNumberStr = imwString;
				lastLetter = -1;
//				lastLetterStr = null;
				lastWasLetter = false;
			}
			
			//	lower case index letter
			else if (imwString.matches("[a-z]")) {
				
				//	handle sub index letter
				if (lastNumber != -1) {
					if (lastLetter == -1) {
						if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got index letter " + imwString);
						//	TODO anything to do here ???
					}
					else {
						if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got index letter range " + ((char) (lastLetter + 'a')) + "-" + imwString);
						//	TODO anything to do here ???
					}
					cc.lastWord = imw;
				}
				
				//	handle top level index letter
				else if ((captionStartNumbers == null) || captionStartNumbers.containsKey(imwString) || captionStartNumbers.containsKey("" + lastNumberStr + imwString)) {
					if (lastLetter == -1) {
						if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got letter " + imwString);
						if (cc == null) { // letters only, no caption citation open
							cc = new CaptionCitation((ccList.isEmpty() ? ccHead : imw), imw);
							ccList.add(cc);
						}
						else cc.lastWord = imw; // combination of numbers and letters, don't overwrite number start
						if (captionStartNumbers != null) {
							ImAnnotation citedCaption = ((ImAnnotation) captionStartNumbers.get(imwString));
							if (citedCaption == null)
								citedCaption = ((ImAnnotation) captionStartNumbers.get("" + lastNumberStr + imwString));
							if (citedCaption != null)
								cc.citedCaptions.add(citedCaption);
						}
					}
					else {
						if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got letter range " + ((char) (lastLetter + 'a')) + "-" + imwString);
						cc.lastWord = imw;
						if (captionStartNumbers != null)
							for (int l = lastLetter; l <= (imwString.charAt(0) - 'a'); l++) {
								ImAnnotation citedCaption = ((ImAnnotation) captionStartNumbers.get("" + ((char) (l + 'a'))));
								if (citedCaption == null)
									citedCaption = ((ImAnnotation) captionStartNumbers.get("" + lastNumberStr + ((char) (l + 'a'))));
								if (citedCaption != null)
									cc.citedCaptions.add(citedCaption);
							}
					}
				}
				lastLetter = (imwString.charAt(0) - 'a');
//				lastLetterStr = imwString;
				lastWasLetter = true;
			}
			
			//	upper case index letter
			else if (imwString.matches("[A-Z]")) {
				
				//	handle sub index letter
				if (lastNumber != -1) {
					if (lastLetter == -1) {
						if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got index letter " + imwString);
						//	TODO anything to do here ???
					}
					else {
						if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got index letter range " + ((char) (lastLetter + 'a')) + "-" + imwString);
						//	TODO anything to do here ???
					}
					cc.lastWord = imw;
				}
				
				//	handle top level index letter
				else if ((captionStartNumbers == null) || captionStartNumbers.containsKey(imwString) || captionStartNumbers.containsKey("" + lastNumberStr + imwString)) {
					if (lastLetter == -1) {
						if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got letter " + imwString);
						if (cc == null) { // letters only, no caption citation open
							cc = new CaptionCitation((ccList.isEmpty() ? ccHead : imw), imw);
							ccList.add(cc);
						}
						else cc.lastWord = imw; // combination of numbers and letters, don't overwrite number start
						if (captionStartNumbers != null) {
							ImAnnotation citedCaption = ((ImAnnotation) captionStartNumbers.get(imwString));
							if (citedCaption == null)
								citedCaption = ((ImAnnotation) captionStartNumbers.get("" + lastNumberStr + imwString));
							if (citedCaption != null)
								cc.citedCaptions.add(citedCaption);
						}
					}
					else {
						if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got letter range " + ((char) (lastLetter + 'A')) + "-" + imwString);
						cc.lastWord = imw;
						if (captionStartNumbers != null)
							for (int l = lastLetter; l <= (imwString.charAt(0) - 'A'); l++) {
								ImAnnotation citedCaption = ((ImAnnotation) captionStartNumbers.get("" + ((char) (l + 'A'))));
								if (citedCaption == null)
									citedCaption = ((ImAnnotation) captionStartNumbers.get("" + lastNumberStr + ((char) (l + 'A'))));
								if (citedCaption != null)
									cc.citedCaptions.add(citedCaption);
							}
					}
				}
				lastLetter = (imwString.charAt(0) - 'A');
//				lastLetterStr = imwString;
				lastWasLetter = true;
			}
			
			//	enumeration separator
			else if (imwString.equals(",") || imwString.equals(";") || imwString.equals("&") || (";and;und;et;y;".indexOf(";" + imwString.toLowerCase() + ";") != -1)) {
				if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got enumeration separator " + imwString);
				if (lastLetter != -1) {// clear last letter
					if (lastNumber == -1)
						cc = null; // close current citation only if letters on top level
					lastLetter = -1;
//					lastLetterStr = null;
					if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println("   ==> letter range start reset");
				}
				else if (lastNumber != -1) {// clear last number
					cc = null;
					lastNumber = -1;
					lastNumberStr = null;
					if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println("   ==> number range start reset");
				}
				else {
					if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println("   ==> no enumeration open, end of caption citation sequence");
					break; // nothing to clear, we're done here
				}
			}
			
			//	range marker
			else if (imwString.matches("[\\-\\u00AD\\u2010\\u2011\\u2012\\u2013\\u2014\\u2015\\u2212]+")) {
				if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got range separator " + imwString);
				if ((lastNumber == -1) && (lastLetter == -1)) {// no range open, we're done here
					if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println("   ==> no range open, end of caption citation sequence");
					break;
				}
			}
			
			//	ignore dots
			else if (!".".equals(imwString)) {
				if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - end of caption citation sequence at " + imwString);
				break;
			}
			
			//	stop looking if requested
			if (imw == ccEnd)
				break;
		}
		
		//	finally ...
		return ccList;
	}
	
	private String annotateCaptionCitation(ImDocument doc, String ccHead, ImWord firstWord, ImWord lastWord, Collection citedCaptions) {
		ImAnnotation captionCitation = doc.addAnnotation(firstWord, lastWord, ((ccHead.toLowerCase().startsWith("tab") ? ImRegion.TABLE_TYPE : "figure") + CITATION_TYPE_SUFFIX));
		int ccIndex = 0;
		for (Iterator ccit = citedCaptions.iterator(); ccit.hasNext();) {
			ImAnnotation citedCaption = ((ImAnnotation) ccit.next());
			String captionStart = this.getCaptionStart(citedCaption);
			String captionText = ImUtils.getString(citedCaption.getFirstWord(), citedCaption.getLastWord(), true);
			String captionTargetPageId = ((String) citedCaption.getAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE));
			BoundingBox captionTargetBox = BoundingBox.parse((String) citedCaption.getAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE));
			String captionHttpUri = ((String) citedCaption.getAttribute("httpUri"));
			if (citedCaptions.size() == 1) {
				captionCitation.setAttribute("captionStartId", citedCaption.getFirstWord().getLocalID());
				captionCitation.setAttribute("captionStart", captionStart);
				captionCitation.setAttribute("captionText", captionText);
				if ((captionTargetPageId != null) && (captionTargetBox != null)) {
					captionCitation.setAttribute("captionTargetPageId", captionTargetPageId);
					captionCitation.setAttribute("captionTargetBox", captionTargetBox.toString());
					String captionTargetId = this.getCaptionTargetSupplementId(citedCaption.getDocument().getPage(Integer.parseInt(captionTargetPageId)), captionTargetBox);
					if (captionTargetId != null)
						captionCitation.setAttribute("captionTargetId", captionTargetId);
				}
				if (captionHttpUri != null)
					captionCitation.setAttribute("httpUri", captionHttpUri);
			}
			else {
				int cci = ccIndex++;
				captionCitation.setAttribute(("captionStartId-" + cci), citedCaption.getFirstWord().getLocalID());
				captionCitation.setAttribute(("captionStart-" + cci), captionStart);
				captionCitation.setAttribute(("captionText-" + cci), captionText);
				if ((captionTargetPageId != null) && (captionTargetBox != null)) {
					captionCitation.setAttribute(("captionTargetPageId-" + cci), captionTargetPageId);
					captionCitation.setAttribute(("captionTargetBox-" + cci), captionTargetBox.toString());
					String captionTargetId = this.getCaptionTargetSupplementId(citedCaption.getDocument().getPage(Integer.parseInt(captionTargetPageId)), captionTargetBox);
					if (captionTargetId != null)
						captionCitation.setAttribute(("captionTargetId-" + cci), captionTargetId);
				}
				if (captionHttpUri != null)
					captionCitation.setAttribute(("httpUri-" + cci), captionHttpUri);
			}
		}
		
		if (DEBUG_MARK_CAPTION_CITATIONS) {
			ImDocumentRoot ccDoc = new ImDocumentRoot(captionCitation, ImDocumentRoot.NORMALIZATION_LEVEL_STREAMS);
			Annotation[] ccAnnot = ccDoc.getAnnotations(captionCitation.getType());
			if (ccAnnot.length != 0)
				System.out.println(" - " + ccAnnot[0].toXML());
		}
		
		return captionCitation.getType();
	}
	
	//	TODO amend this from config file if data provider present ==> use instance field (and then don't, instance is singleton anyway, and GG makes sure of it)
	private static TreeMap captionStartMappings = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	static {
		captionStartMappings.put("Fig", "Figure");
		captionStartMappings.put("Figs", "Figures");
		captionStartMappings.put("Abb", "Abbildung");
		captionStartMappings.put("Diag", "Diagram");
		captionStartMappings.put("Diags", "Diagrams");
		captionStartMappings.put("Tab", "Table");
	}
	
	//	TODO amend this from config file if data provider present ==> use instance field (and then don't, instance is singleton anyway, and GG makes sure of it)
	private static TreeMap captionStartAbbreviationMappings = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	static {
		Set captionStartAbbreviations;
		captionStartAbbreviations = new TreeSet();
		captionStartAbbreviations.add("Pl");
		captionStartAbbreviations.add("Plt");
		captionStartAbbreviationMappings.put("Plate", captionStartAbbreviations);
		//	TODO think of more
	}
	
	private static class CaptionCitation {
		ImWord firstWord;
		ImWord lastWord;
		LinkedHashSet citedCaptions = new LinkedHashSet();
		CaptionCitation(ImWord firstWord, ImWord lastWord) {
			this.firstWord = firstWord;
			this.lastWord = lastWord;
		}
	}
}
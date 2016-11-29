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

import java.awt.Point;
import java.util.Arrays;
import java.util.LinkedList;

import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.DocumentStyle;
import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImLayoutObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImSupplement;
import de.uka.ipd.idaho.im.ImSupplement.Figure;
import de.uka.ipd.idaho.im.ImSupplement.Graphics;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.analysis.Imaging;
import de.uka.ipd.idaho.im.analysis.Imaging.AnalysisImage;
import de.uka.ipd.idaho.im.analysis.Imaging.ImagePartRectangle;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;

/**
 * This class provides basic actions for working with text blocks.
 * 
 * @author sautter
 */
public class TextBlockActionProvider extends AbstractSelectionActionProvider implements LiteratureConstants {
	
	/** public zero-argument constructor for class loading */
	public TextBlockActionProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Text Block Actions";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(final ImWord start, final ImWord end, ImDocumentMarkupPanel idmp) {
		
		//	we strictly work on one page at a time
		if (start.pageId != end.pageId)
			return null;
		
		//	we also work on individual text streams only
		if (!start.getTextStreamId().equals(end.getTextStreamId()))
			return null;
		
		//	line up words
		LinkedList words = new LinkedList();
		for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
			words.addLast(imw);
			if (imw == end)
				break;
		}
		
		//	return actions
		return this.getActions(((ImWord[]) words.toArray(new ImWord[words.size()])), null, null, idmp);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, ImPage page, ImDocumentMarkupPanel idmp) {
		
		//	mark selection
		BoundingBox selectedBox = new BoundingBox(Math.min(start.x, end.x), Math.max(start.x, end.x), Math.min(start.y, end.y), Math.max(start.y, end.y));
		
		//	get selected words
		ImWord[] selectedWords = page.getWordsInside(selectedBox);
		
		//	return actions if selection not empty
		return ((selectedWords.length == 0) ? null : this.getActions(selectedWords, page, selectedBox, idmp));
	}
	
	private SelectionAction[] getActions(final ImWord[] words, final ImPage page, final BoundingBox selectedBox, final ImDocumentMarkupPanel idmp) {
		LinkedList actions = new LinkedList();
		
		//	mark selected words as a caption
		actions.add(new SelectionAction("markRegionCaption", "Mark Caption", "Mark selected words as a caption.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				
				//	cut out caption
				ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_CAPTION, null);
				ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
				
				//	annotate caption
				ImAnnotation caption = words[0].getDocument().addAnnotation(words[0], words[words.length-1], CAPTION_TYPE);
				caption.getLastWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
				invoker.setAnnotationsPainted(CAPTION_TYPE, true);
				BoundingBox captionBox = ImLayoutObject.getAggregateBox(words);
				
				//	do we have a table caption or a figure caption?
				boolean isTableCaption = words[0].getString().toLowerCase().startsWith("tab"); // covers most Latin based languages
				
				//	find possible targets
				ImRegion[] targets;
				if (isTableCaption)
					targets = page.getRegions(ImRegion.TABLE_TYPE);
				else {
					targets = page.getRegions(ImRegion.IMAGE_TYPE);
					if (targets.length == 0)
						targets = page.getRegions(ImRegion.GRAPHICS_TYPE);
					if (targets.length == 0)
						targets = page.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
				}
				if (targets.length == 0)
					return true;
				
				//	assign target
				Arrays.sort(targets, ImUtils.topDownOrder);
				for (int t = 0; t < targets.length; t++) {
					
					//	check vertical alignment
					if (!isTableCaption && (captionBox.top < targets[t].bounds.bottom))
						break; // due to top-down sort order, we won't find any matches from here onward
					
					//	check general alignment
					if (ImUtils.isCaptionBelowTargetMatch(captionBox, targets[t].bounds, page.getImageDPI())) {}
					else if (isTableCaption && ImUtils.isCaptionAboveTargetMatch(captionBox, targets[t].bounds, page.getImageDPI())) {}
					else if (!isTableCaption && ImUtils.isCaptionBesideTargetMatch(captionBox, targets[t].bounds, page.getImageDPI())) {}
					else continue;
					
					//	check size and words if using block fallback
					if (ImRegion.BLOCK_ANNOTATION_TYPE.equals(targets[t].getType())) {
						if ((targets[t].bounds.right - targets[t].bounds.left) < page.getImageDPI())
							continue;
						if ((targets[t].bounds.bottom - targets[t].bounds.top) < page.getImageDPI())
							continue;
						ImWord[] imageWords = targets[t].getWords();
						if (imageWords.length != 0)
							continue;
					}
					
					//	link caption to target
					caption.setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + targets[t].pageId));
					caption.setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, targets[t].bounds.toString());
					if (isTableCaption)
						caption.setAttribute("targetIsTable");
					break;
				}
				
				//	finally ...
				return true;
			}
		});
		
		//	mark selected words as a footnote
		actions.add(new SelectionAction("markRegionFootnote", "Mark Footnote", "Mark selected words as a footnote.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_FOOTNOTE, null);
				ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
				ImAnnotation footnote = words[0].getDocument().addAnnotation(words[0], words[words.length-1], FOOTNOTE_TYPE);
				footnote.getLastWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
				invoker.setAnnotationsPainted(FOOTNOTE_TYPE, true);
				return true;
			}
		});
		
		//	mark selected words as a table note
		actions.add(new SelectionAction("markRegionTableNote", "Mark Table Note", "Mark selected words as a table note, i.e., explanatory text associated with a table.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				
				//	cut out and mark table note
				ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_TABLE_NOTE, null);
				ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
				ImAnnotation tableNote = words[0].getDocument().addAnnotation(words[0], words[words.length-1], TABLE_NOTE_TYPE);
				tableNote.getLastWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
				invoker.setAnnotationsPainted(TABLE_NOTE_TYPE, true);
				BoundingBox tableNoteBox = ImLayoutObject.getAggregateBox(words);
				
				//	find possible targets
				ImRegion[] tables = page.getRegions(ImRegion.TABLE_TYPE);
				if (tables.length == 0)
					return true;
				
				//	assign target
				Arrays.sort(tables, ImUtils.topDownOrder);
				for (int t = 0; t < tables.length; t++) {
					
					//	check general alignment
					if (ImUtils.isCaptionBelowTargetMatch(tableNoteBox, tables[t].bounds, page.getImageDPI())) {}
					else if (ImUtils.isCaptionAboveTargetMatch(tableNoteBox, tables[t].bounds, page.getImageDPI())) {}
					else continue;
					
					//	link table note to target
					tableNote.setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + tables[t].pageId));
					tableNote.setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, tables[t].bounds.toString());
					break;
				}
				
				//	finally ...
				return true;
			}
		});
		
		//	mark selected words as a page header
		actions.add(new SelectionAction("markRegionPageHeader", "Mark Page Header", "Mark selected words as a page header or footer.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_PAGE_TITLE, null);
				ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
				ImAnnotation pageTitle = words[0].getDocument().addAnnotation(words[0], words[words.length-1], PAGE_TITLE_TYPE);
				pageTitle.getLastWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
				invoker.setAnnotationsPainted(PAGE_TITLE_TYPE, true);
				return true;
			}
		});
		
		//	mark selected words as a page header
		actions.add(new SelectionAction("markRegionParenthesis", "Mark Parenthesis", "Mark selected words as a parenthesis, e.g. a standalone text box or a note.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_MAIN_TEXT, null);
				ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
				ImAnnotation pageTitle = words[0].getDocument().addAnnotation(words[0], words[words.length-1], PARENTHESIS_TYPE);
				pageTitle.getLastWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
				invoker.setAnnotationsPainted(PARENTHESIS_TYPE, true);
				return true;
			}
		});
		
		//	mark selected words as an artifact
		actions.add(new SelectionAction("markRegionArtifact", "Mark Artifact", "Mark selected words as an OCR or layout artifact, and remove them.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				
				//	remove words
				ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_ARTIFACT, null);
				ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
				for (ImWord imw = words[words.length-1]; imw != null; imw = imw.getPreviousWord())
					imw.setNextWord(null);
				for (int w = 0; w < words.length; w++)
					page.removeWord(words[w], true);
				
				//	indicate change
				return true;
			}
		});
		
		//	if we're on text stream selection, we're done here
		if ((page == null) || (selectedBox == null))
			return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
		
		//	check supplements to tell image from graphics if document born-digital
		boolean isImage;
		if (idmp.documentBornDigital) {
			ImSupplement[] pageSupplements = page.getSupplements();
			Figure[] figures = ImSupplement.getFiguresIn(pageSupplements, selectedBox);
			int figureArea = 0;
			for (int f = 0; f < figures.length; f++)
				figureArea += figures[f].getBounds().getArea();
			Graphics[] graphics = ImSupplement.getGraphicsIn(pageSupplements, selectedBox);
			int graphicsArea = 0;
			for (int g = 0; g < graphics.length; g++)
				graphicsArea += graphics[g].getBounds().getArea();
			
			//	area mostly (>= 80%) covered by figures, any graphics are likely decoration
			if ((figureArea * 5) >= (selectedBox.getArea() * 4))
				isImage = true;
			//	area contains more figures that graphics
			else if (figureArea > graphicsArea)
				isImage = true;
			//	less figures than graphics
			else isImage = false;
		}
		else isImage = true;
		
		//	mark selected non-white area as image (case without words comes from region actions)
		if (isImage)
			actions.add(new SelectionAction("markRegionImage", "Mark Image", "Mark selected region as an image.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					markImageOrGraphics(words, page, selectedBox, ImRegion.IMAGE_TYPE, idmp);
					return true;
				}
			});
		
		//	mark selected non-white area as graphics (case without words comes from region actions)
		else actions.add(new SelectionAction("markRegionGraphics", "Mark Graphics", "Mark selected region as a vector based graphics.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				markImageOrGraphics(words, page, selectedBox, ImRegion.GRAPHICS_TYPE, idmp);
				return true;
			}
		});
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	private void markImageOrGraphics(ImWord[] words, ImPage page, BoundingBox selectedBox, String type, ImDocumentMarkupPanel idmp) {
		
		//	make words into 'label' text stream if document is born-digital
		if (idmp.documentBornDigital) {
			ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_LABEL, null);
			ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
		}
		
		//	remove words if document is scanned (odds are most of them are artifacts)
		else {
			ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_ARTIFACT, null);
			ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
			for (ImWord imw = words[words.length-1]; imw != null; imw = imw.getPreviousWord())
				imw.setNextWord(null);
			for (int w = 0; w < words.length; w++)
				page.removeWord(words[w], true);
		}
		
		//	shrink selection to what is actually painted
		PageImage pi = page.getImage();
		AnalysisImage ai = Imaging.wrapImage(pi.image, null);
		ImagePartRectangle ipr = Imaging.getContentBox(ai);
		ImagePartRectangle selectedIpr = ipr.getSubRectangle(Math.max(selectedBox.left, page.bounds.left), Math.min(selectedBox.right, page.bounds.right), Math.max(selectedBox.top, page.bounds.top), Math.min(selectedBox.bottom, page.bounds.bottom));
		selectedIpr = Imaging.narrowLeftAndRight(selectedIpr);
		selectedIpr = Imaging.narrowTopAndBottom(selectedIpr);
		BoundingBox iogBox = new BoundingBox(selectedIpr.getLeftCol(), selectedIpr.getRightCol(), selectedIpr.getTopRow(), selectedIpr.getBottomRow());
		
		//	clean up nested regions
		ImRegion[] selectedRegions = page.getRegionsInside(selectedBox, true);
		for (int r = 0; r < selectedRegions.length; r++) {
			if (!iogBox.liesIn(selectedRegions[r].bounds, false))
				page.removeRegion(selectedRegions[r]);
		}
		
		//	mark image or graphics
		ImRegion iog = new ImRegion(page, iogBox, type);
		idmp.setRegionsPainted(type, true);
		
		//	consult document style regarding where captions might be located
		DocumentStyle docStyle = DocumentStyle.getStyleFor(idmp.document);
		final DocumentStyle docLayout = docStyle.getSubset("layout");
		boolean captionAbove = docLayout.getBooleanProperty("caption.aboveFigure", false);
		boolean captionBelow = docLayout.getBooleanProperty("caption.belowFigure", true);
//		boolean captionBeside = docLayout.getBooleanProperty("caption.besideFigure", true);
		
		//	get potential captions
		//	TODO also consider beside target captions (5-argument method) once ImUtils update is spread
		ImAnnotation[] captionAnnots = ImUtils.findCaptions(iog, captionAbove, captionBelow/*, captionBeside*/, true);
		
		//	try setting attributes in unassigned captions first
		for (int a = 0; a < captionAnnots.length; a++) {
			if (captionAnnots[a].hasAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE) || captionAnnots[a].hasAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE))
				continue;
			captionAnnots[a].setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + iog.pageId));
			captionAnnots[a].setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, iog.bounds.toString());
			return;
		}
		
		//	set attributes in any caption (happens if user corrects, for instance)
		if (captionAnnots.length != 0) {
			captionAnnots[0].setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + iog.pageId));
			captionAnnots[0].setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, iog.bounds.toString());
		}
	}
}
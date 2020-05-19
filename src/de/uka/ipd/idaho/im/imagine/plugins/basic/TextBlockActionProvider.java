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

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JMenuItem;

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
	private RegionActionProvider regionActions = null;
	
	/** public zero-argument constructor for class loading */
	public TextBlockActionProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Text Block Actions";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractGoldenGateImaginePlugin#initImagine()
	 */
	public void initImagine() {
		
		//	get region action provider
		this.regionActions = ((RegionActionProvider) this.imagineParent.getPlugin(RegionActionProvider.class.getName()));
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
		return this.getActions(((ImWord[]) words.toArray(new ImWord[words.size()])), start.getPage(), null, idmp);
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
		
		//	get bounding box of selected words
		final BoundingBox wordBox = ImLayoutObject.getAggregateBox(words);
		
		//	mark selected words as a caption
		actions.add(new SelectionAction("markRegionCaption", "Mark Caption", "Mark selected words as a caption.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				return markCaption(page, words, wordBox, invoker, true);
			}
			public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
				JMenuItem mi = super.getMenuItem(invoker);
				Color regionTypeColor = idmp.getTextStreamTypeColor(ImWord.TEXT_STREAM_TYPE_CAPTION);
				if (regionTypeColor != null) {
					mi.setOpaque(true);
					mi.setBackground(regionTypeColor);
				}
				return mi;
			}
		});
		
		//	mark selected words as an in-line caption
		actions.add(new SelectionAction("markInLineCaption", "Mark In-Line Caption", "Mark selected words as an in-line caption.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				return markCaption(page, words, wordBox, invoker, false);
			}
			public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
				JMenuItem mi = super.getMenuItem(invoker);
				Color regionTypeColor = idmp.getAnnotationColor(CAPTION_TYPE);
				if (regionTypeColor != null) {
					mi.setOpaque(true);
					mi.setBackground(regionTypeColor);
				}
				return mi;
			}
		});
		
		//	mark selected words as a footnote
		actions.add(new SelectionAction("markRegionFootnote", "Mark Footnote", "Mark selected words as a footnote.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				splitOverlappingBlocks(page, wordBox);
				ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_FOOTNOTE, null);
				ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
				ImAnnotation footnote = words[0].getDocument().addAnnotation(words[0], words[words.length-1], FOOTNOTE_TYPE);
				footnote.getLastWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
				invoker.setAnnotationsPainted(FOOTNOTE_TYPE, true);
				return true;
			}
			public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
				JMenuItem mi = super.getMenuItem(invoker);
				Color regionTypeColor = idmp.getTextStreamTypeColor(ImWord.TEXT_STREAM_TYPE_FOOTNOTE);
				if (regionTypeColor != null) {
					mi.setOpaque(true);
					mi.setBackground(regionTypeColor);
				}
				return mi;
			}
		});
		
		//	mark selected words as a table note
		actions.add(new SelectionAction("markRegionTableNote", "Mark Table Note", "Mark selected words as a table note, i.e., explanatory text associated with a table.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				
				//	split any partially selected blocks
				splitOverlappingBlocks(page, wordBox);
				
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
			public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
				JMenuItem mi = super.getMenuItem(invoker);
				Color regionTypeColor = idmp.getTextStreamTypeColor(ImWord.TEXT_STREAM_TYPE_TABLE_NOTE);
				if (regionTypeColor != null) {
					mi.setOpaque(true);
					mi.setBackground(regionTypeColor);
				}
				return mi;
			}
		});
		
		//	mark selected words as a page header
		actions.add(new SelectionAction("markRegionPageHeader", "Mark Page Header", "Mark selected words as a page header or footer.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				splitOverlappingBlocks(page, wordBox);
				ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_PAGE_TITLE, null);
				ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
				ImAnnotation pageTitle = words[0].getDocument().addAnnotation(words[0], words[words.length-1], PAGE_TITLE_TYPE);
				pageTitle.getLastWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
				invoker.setAnnotationsPainted(PAGE_TITLE_TYPE, true);
				return true;
			}
			public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
				JMenuItem mi = super.getMenuItem(invoker);
				Color regionTypeColor = idmp.getTextStreamTypeColor(ImWord.TEXT_STREAM_TYPE_PAGE_TITLE);
				if (regionTypeColor != null) {
					mi.setOpaque(true);
					mi.setBackground(regionTypeColor);
				}
				return mi;
			}
		});
		
		//	mark selected words as a page header
		actions.add(new SelectionAction("markRegionParenthesis", "Mark Parenthesis", "Mark selected words as a parenthesis, e.g. a standalone text box or a note.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				splitOverlappingBlocks(page, wordBox);
				ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_MAIN_TEXT, null);
				ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
				ImAnnotation pageTitle = words[0].getDocument().addAnnotation(words[0], words[words.length-1], PARENTHESIS_TYPE);
				pageTitle.getLastWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
				invoker.setAnnotationsPainted(PARENTHESIS_TYPE, true);
				return true;
			}
			public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
				JMenuItem mi = super.getMenuItem(invoker);
				Color regionTypeColor = idmp.getTextStreamTypeColor(ImWord.TEXT_STREAM_TYPE_MAIN_TEXT);
				if (regionTypeColor != null) {
					mi.setOpaque(true);
					mi.setBackground(regionTypeColor);
				}
				return mi;
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
				
				//	also remove or shrink regions
				if ((page != null) && (selectedBox != null)) {
					ImRegion[] regions = page.getRegions();
					ArrayList shrinkRegions = new ArrayList();
					for (int r = 0; r < regions.length; r++) {
						if (selectedBox.includes(regions[r].bounds, false))
							page.removeRegion(regions[r]);
						else if (selectedBox.overlaps(regions[r].bounds))
							shrinkRegions.add(regions[r]);
					}
					for (int r = 0; r < shrinkRegions.size(); r++) {
						ImRegion region = ((ImRegion) shrinkRegions.get(r));
						if (ImRegion.IMAGE_TYPE.equals(region.getType()))
							continue;
						if (ImRegion.GRAPHICS_TYPE.equals(region.getType()))
							continue;
						ImWord[] regionWords = region.getWords();
						if (regionWords.length == 0) {
							page.removeRegion(region);
							continue;
						}
						BoundingBox regionBounds = ImLayoutObject.getAggregateBox(regionWords);
						if (regionBounds == null) {
							page.removeRegion(region);
							continue;
						}
						if (regionBounds.equals(region.bounds))
							continue;
						(new ImRegion(page, regionBounds, region.getType())).copyAttributes(region);
						page.removeRegion(region);
					}
				}
				
				//	indicate change
				return true;
			}
			public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
				JMenuItem mi = super.getMenuItem(invoker);
				Color regionTypeColor = idmp.getTextStreamTypeColor(ImWord.TEXT_STREAM_TYPE_ARTIFACT);
				if (regionTypeColor != null) {
					mi.setOpaque(true);
					mi.setBackground(regionTypeColor);
				}
				return mi;
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
			//	area contains more figures than graphics
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
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					JMenuItem mi = super.getMenuItem(invoker);
					Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.IMAGE_TYPE);
					if (regionTypeColor != null) {
						mi.setOpaque(true);
						mi.setBackground(regionTypeColor);
					}
					return mi;
				}
			});
		
		//	mark selected non-white area as graphics (case without words comes from region actions)
		else actions.add(new SelectionAction("markRegionGraphics", "Mark Graphics", "Mark selected region as a vector based graphics.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				markImageOrGraphics(words, page, selectedBox, ImRegion.GRAPHICS_TYPE, idmp);
				return true;
			}
			public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
				JMenuItem mi = super.getMenuItem(invoker);
				Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.GRAPHICS_TYPE);
				if (regionTypeColor != null) {
					mi.setOpaque(true);
					mi.setBackground(regionTypeColor);
				}
				return mi;
			}
		});
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	private boolean markCaption(ImPage page, ImWord[] words, BoundingBox wordBox, ImDocumentMarkupPanel invoker, boolean isBlockCaption) {
		
		//	make block captions into separate text stream
		if (isBlockCaption) {
			
			//	split any partially selected blocks
			this.splitOverlappingBlocks(page, wordBox);
			
			//	cut out caption
			ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_CAPTION, null);
			ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
		}
		
		//	make sure to get words in right order for in-line caption
		else {
			ImUtils.orderStream(words, ImUtils.textStreamOrder);
			if (!words[0].getTextStreamId().equals(words[words.length-1].getTextStreamId()))
				return false;
		}
		
		//	annotate caption
		ImAnnotation caption = words[0].getDocument().addAnnotation(words[0], words[words.length-1], CAPTION_TYPE);
		if (isBlockCaption)
			caption.getLastWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
		else caption.setAttribute(IN_LINE_OBJECT_MARKER_ATTRIBUTE);
		invoker.setAnnotationsPainted(CAPTION_TYPE, true);
		
		//	do we have a table caption or a figure caption?
		boolean isTableCaption = words[0].getString().toLowerCase().startsWith("tab"); // covers most Latin based languages
		
		//	find possible targets
		ImRegion[] targets = this.getCaptionTargets(page, isTableCaption, isBlockCaption);
		if (targets.length == 0)
			return true;
		
		//	assign target TODO factor in style template specified caption positioning
		Arrays.sort(targets, ImUtils.topDownOrder);
		ImRegion target = this.findCaptionTarget(page.getImageDPI(), wordBox, targets, isTableCaption, isBlockCaption);
		
		//	link caption to target
		if (target != null) {
			caption.setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + target.pageId));
			caption.setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, target.bounds.toString());
			if (isBlockCaption)
				caption.setAttribute("startId", caption.getFirstWord().getLocalID());
			if (isTableCaption)
				caption.setAttribute("targetIsTable");
		}
		
		//	finally ...
		return true;
	}
	
	private ImRegion[] getCaptionTargets(ImPage page, boolean isTableCaption, boolean isBlockCaption) {
		ArrayList targets = new ArrayList(5);
		
		//	observe caption type in block captions, as they usually have indicative start words
		if (isBlockCaption) {
			if (isTableCaption)
				targets.addAll(Arrays.asList(page.getRegions(ImRegion.TABLE_TYPE)));
			else {
				targets.addAll(Arrays.asList(page.getRegions(ImRegion.IMAGE_TYPE)));
				targets.addAll(Arrays.asList(page.getRegions(ImRegion.GRAPHICS_TYPE)));
				if (targets.isEmpty())
					targets.addAll(Arrays.asList(page.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE)));
			}
		}
		
		//	work with all possible targets if we have an in-line caption, as they might not have have the indicative start words
		else {
			targets.addAll(Arrays.asList(page.getRegions(ImRegion.TABLE_TYPE)));
			targets.addAll(Arrays.asList(page.getRegions(ImRegion.IMAGE_TYPE)));
			targets.addAll(Arrays.asList(page.getRegions(ImRegion.GRAPHICS_TYPE)));
		}
		
		return ((ImRegion[]) targets.toArray(new ImRegion[targets.size()]));
	}
	
	private ImRegion findCaptionTarget(int pageImageDpi, BoundingBox captionBox, ImRegion[] targets, boolean isTableCaption, boolean isBlockCaption) {
		
		//	for block caption, prefer caption above table and caption below figure
		if (isBlockCaption) {
			
			//	run with preferred assignment direction first
			for (int t = 0; t < targets.length; t++) {
				if (!isTableCaption && ImUtils.isCaptionBelowTargetMatch(captionBox, targets[t].bounds, pageImageDpi)) {}
				else if (isTableCaption && ImUtils.isCaptionAboveTargetMatch(captionBox, targets[t].bounds, pageImageDpi)) {}
				else continue;
				if (this.isValidCaptionTarget(targets[t], pageImageDpi))
					return targets[t];
			}
			
			//	check all assignment directions
			for (int t = 0; t < targets.length; t++) {
				if (ImUtils.isCaptionBelowTargetMatch(captionBox, targets[t].bounds, pageImageDpi)) {}
				else if (ImUtils.isCaptionAboveTargetMatch(captionBox, targets[t].bounds, pageImageDpi)) {}
				else if (!isTableCaption && ImUtils.isCaptionBesideTargetMatch(captionBox, targets[t].bounds, pageImageDpi)) {}
				else continue;
				if (this.isValidCaptionTarget(targets[t], pageImageDpi))
					return targets[t];
			}
		}
		
		//	for in-line caption, prefer closest target
		else {
			ImRegion closestTarget = null;
			int closestTargetDist = Integer.MAX_VALUE;
			
			//	run with main caption targets first
			for (int t = 0; t < targets.length; t++) {
				if (ImRegion.BLOCK_ANNOTATION_TYPE.equals(targets[t].getType()))
					continue;
				int targetDist;
				if (captionBox.right <= targets[t].bounds.left) {
					if (targets[t].bounds.bottom <= captionBox.top)
						continue;
					if (captionBox.bottom <= targets[t].bounds.top)
						continue;
					targetDist = (targets[t].bounds.left - captionBox.right);
				}
				else if (targets[t].bounds.right <= captionBox.left) {
					if (targets[t].bounds.bottom <= captionBox.top)
						continue;
					if (captionBox.bottom <= targets[t].bounds.top)
						continue;
					targetDist = (captionBox.left - targets[t].bounds.right);
				}
				else if (targets[t].bounds.bottom <= captionBox.top)
					targetDist = (captionBox.top - targets[t].bounds.bottom);
				else if (captionBox.bottom <= targets[t].bounds.top)
					targetDist = (targets[t].bounds.top - captionBox.bottom);
				else targetDist = 0;
				if (targetDist < closestTargetDist) {
					closestTarget = targets[t];
					closestTargetDist = targetDist;
				}
			}
			
			//	this one is close enough
			if (closestTargetDist < pageImageDpi)
				return closestTarget;
		}
		
		//	nothing found
		return null;
	}
	
	private boolean isValidCaptionTarget(ImRegion target, int pageImageDpi) {
		
		//	check size and words if using block fallback
		if (ImRegion.BLOCK_ANNOTATION_TYPE.equals(target.getType())) {
			if ((target.bounds.right - target.bounds.left) < pageImageDpi)
				return false;
			if ((target.bounds.bottom - target.bounds.top) < pageImageDpi)
				return false;
			ImWord[] imageWords = target.getWords();
			return (imageWords.length == 0);
		}
		
		//	all other targets are inherently valid
		else return true;
	}
	
	private void splitOverlappingBlocks(ImPage page, BoundingBox bounds) {
		if (this.regionActions == null)
			return;
		
		//	split any overlapping blocks
		ImRegion[] pageBlocks = page.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
		for (int b = 0; b < pageBlocks.length; b++) {
			if (pageBlocks[b].bounds.liesIn(bounds, false))
				continue; // completely contained, no need for splitting
			if (pageBlocks[b].bounds.overlaps(bounds))
				this.regionActions.splitBlock(page, pageBlocks[b], bounds.intersect(pageBlocks[b].bounds));
		}
	}
	
	private void markImageOrGraphics(ImWord[] words, ImPage page, BoundingBox selectedBox, String type, ImDocumentMarkupPanel idmp) {
		
		//	shrink selection to what is actually painted
		PageImage pi = page.getImage();
		AnalysisImage ai = Imaging.wrapImage(pi.image, null);
		ImagePartRectangle ipr = Imaging.getContentBox(ai);
		ImagePartRectangle selectedIpr = ipr.getSubRectangle(Math.max(selectedBox.left, page.bounds.left), Math.min(selectedBox.right, page.bounds.right), Math.max(selectedBox.top, page.bounds.top), Math.min(selectedBox.bottom, page.bounds.bottom));
		selectedIpr = Imaging.narrowLeftAndRight(selectedIpr);
		selectedIpr = Imaging.narrowTopAndBottom(selectedIpr);
		BoundingBox iogBounds = new BoundingBox(selectedIpr.getLeftCol(), selectedIpr.getRightCol(), selectedIpr.getTopRow(), selectedIpr.getBottomRow());
		HashMap colWordsByStreamId = null;
		
		//	make words into 'label' text stream if document is born-digital
		if (idmp.documentBornDigital) {
			ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_LABEL, null);
			ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
			words = new ImWord[0];
		}
		
		//	remove words if document is scanned (odds are most of them are artifacts)
		else {
			
			//	spare any words of type 'caption' or 'label', though
			ArrayList removeWordList = new ArrayList();
			colWordsByStreamId = new HashMap();
			for (int w = 0; w < words.length; w++) {
				if (ImWord.TEXT_STREAM_TYPE_CAPTION.equals(words[w].getTextStreamType()) || ImWord.TEXT_STREAM_TYPE_LABEL.equals(words[w].getTextStreamType())) {
					ArrayList colWordList = ((ArrayList) colWordsByStreamId.get(words[w].getTextStreamId()));
					if (colWordList == null) {
						colWordList = new ArrayList();
						colWordsByStreamId.put(words[w].getTextStreamId(), colWordList);
					}
					colWordList.add(words[w]);
				}
				else removeWordList.add(words[w]);
			}
			words = ((ImWord[]) removeWordList.toArray(new ImWord[removeWordList.size()]));
			
			//	dissolve remaining words out of text streams (helps prevent cycles on block splitting)
			ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_ARTIFACT, null);
			ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
			for (ImWord imw = words[words.length-1]; imw != null; imw = imw.getPreviousWord()) // going backwards saves us propagating changes of text stream ID
				imw.setNextWord(null);
		}
		
		//	split any overlapping block
		if (this.regionActions != null) {
			this.splitOverlappingBlocks(page, iogBounds);
			
			//	restore text streams preserved above
			if (colWordsByStreamId != null)
				for (Iterator tsit = colWordsByStreamId.keySet().iterator(); tsit.hasNext();) {
					ArrayList colWordList = ((ArrayList) colWordsByStreamId.get(tsit.next()));
					ImUtils.makeStream(((ImWord[]) colWordList.toArray(new ImWord[colWordList.size()])), ((ImWord) colWordList.get(0)).getTextStreamType(), null);
				}
			
			//	cut to-remove words out of text streams again (in case block splitting merged them back in some way)
			if ((words != null) && (words.length != 0)) {
				ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_ARTIFACT, null);
				ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
				for (ImWord imw = words[words.length-1]; imw != null; imw = imw.getPreviousWord()) // going backwards saves us propagating changes of text stream ID
					imw.setNextWord(null);
			}
		}
		
		//	remove remaining words (only now that we're done with block splitting)
		for (int w = 0; w < words.length; w++)
			page.removeWord(words[w], true);
		
		//	clean up nested regions (repetitively, as some removals trigger adding new regions via reactions)
		ImRegion[] selectedRegions = page.getRegionsInside(iogBounds, true);
		for (int r = 0; r < selectedRegions.length; r++) {
			if (!iogBounds.liesIn(selectedRegions[r].bounds, false) || iogBounds.equals(selectedRegions[r].bounds))
				page.removeRegion(selectedRegions[r]);
		}
		
		//	mark image or graphics
		ImRegion iog = new ImRegion(page, iogBounds, type);
		idmp.setRegionsPainted(type, true);
		
		//	consult document style regarding where captions might be located
		DocumentStyle docStyle = DocumentStyle.getStyleFor(idmp.document);
		final DocumentStyle docLayout = docStyle.getSubset("layout");
		boolean captionAbove = docLayout.getBooleanProperty("caption.aboveFigure", false);
		boolean captionBelow = docLayout.getBooleanProperty("caption.belowFigure", true);
		boolean captionBeside = docLayout.getBooleanProperty("caption.besideFigure", true);
		boolean captionInside = docLayout.getBooleanProperty("caption.insideFigure", true);
		
		//	get potential captions
		ImAnnotation[] captionAnnots = ImUtils.findCaptions(iog, captionAbove, captionBelow, captionBeside, captionInside, true);
		
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
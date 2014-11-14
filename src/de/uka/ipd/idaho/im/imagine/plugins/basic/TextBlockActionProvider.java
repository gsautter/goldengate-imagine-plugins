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
import java.util.LinkedList;

import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImWord;
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
	public SelectionAction[] getActions(final ImWord start, final ImWord end, final ImDocumentMarkupPanel idmp) {
		
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
		return this.getActions(((ImWord[]) words.toArray(new ImWord[words.size()])));
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
		return ((selectedWords.length == 0) ? null : this.getActions(selectedWords));
	}
	
	private SelectionAction[] getActions(final ImWord[] words) {
		LinkedList actions = new LinkedList();
		
		//	mark selected words as a caption
		actions.add(new SelectionAction("Mark Caption", "Mark selected words as a caption.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_CAPTION, null);
				ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
				ImAnnotation caption = words[0].getDocument().addAnnotation(words[0], words[words.length-1], CAPTION_TYPE);
				caption.getLastWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
				invoker.setAnnotationsPainted(CAPTION_TYPE, true);
				return true;
			}
		});
		
		//	mark selected words as a footnote
		actions.add(new SelectionAction("Mark Footnote", "Mark selected words as a footnote.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_FOOTNOTE, null);
				ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
				ImAnnotation footnote = words[0].getDocument().addAnnotation(words[0], words[words.length-1], FOOTNOTE_TYPE);
				footnote.getLastWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
				invoker.setAnnotationsPainted(FOOTNOTE_TYPE, true);
				return true;
			}
		});
		
		//	mark selected words as a page header
		actions.add(new SelectionAction("Mark Page Header", "Mark selected words as a page header or footer.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_PAGE_TITLE, null);
				ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
				ImAnnotation pageTitle = words[0].getDocument().addAnnotation(words[0], words[words.length-1], PAGE_TITLE_TYPE);
				pageTitle.getLastWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
				invoker.setAnnotationsPainted(PAGE_TITLE_TYPE, true);
					return true;
			}
		});
		
		//	mark selected words as an artifact
		actions.add(new SelectionAction("Mark Artifact", "Mark selected words as an OCR or layout artifact.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_ARTIFACT, null);
				ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
				for (ImWord imw = words[words.length-1]; imw != null; imw = imw.getPreviousWord())
					imw.setPreviousWord(null);
				return true;
			}
		});
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
}
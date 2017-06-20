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
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicFileChooserUI;

import de.uka.ipd.idaho.easyIO.streams.CharSequenceReader;
import de.uka.ipd.idaho.gamta.AttributeUtils;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.DocumentStyle;
import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImLayoutObject;
import de.uka.ipd.idaho.im.ImObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImSupplement;
import de.uka.ipd.idaho.im.ImSupplement.Figure;
import de.uka.ipd.idaho.im.ImSupplement.Graphics;
import de.uka.ipd.idaho.im.ImSupplement.Scan;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.analysis.Imaging;
import de.uka.ipd.idaho.im.analysis.Imaging.AnalysisImage;
import de.uka.ipd.idaho.im.analysis.Imaging.ImagePartRectangle;
import de.uka.ipd.idaho.im.analysis.PageAnalysis;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.TwoClickSelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.im.util.ImUtils.StringPair;

/**
 * This class provides basic actions for handling regions.
 * 
 * @author sautter
 */
public class RegionActionProvider extends AbstractSelectionActionProvider implements ImageMarkupToolProvider, LiteratureConstants, ReactionProvider {
	
	private static final String REGION_CONVERTER_IMT_NAME = "ConvertRegions";
	private static final String REGION_RETYPER_IMT_NAME = "RetypeRegions";
	private static final String REGION_REMOVER_IMT_NAME = "RemoveRegions";
	
	private ImageMarkupTool regionConverter = new RegionConverter();
	private ImageMarkupTool regionRetyper = new RegionRetyper();
	private ImageMarkupTool regionRemover = new RegionRemover();
	
	/** public zero-argument constructor for class loading */
	public RegionActionProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Region Actions";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getEditMenuItemNames()
	 */
	public String[] getEditMenuItemNames() {
		String[] emins = {REGION_CONVERTER_IMT_NAME, REGION_RETYPER_IMT_NAME, REGION_REMOVER_IMT_NAME};
		return emins;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getToolsMenuItemNames()
	 */
	public String[] getToolsMenuItemNames() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		if (REGION_CONVERTER_IMT_NAME.equals(name))
			return this.regionConverter;
		else if (REGION_RETYPER_IMT_NAME.equals(name))
			return this.regionRetyper;
		else if (REGION_REMOVER_IMT_NAME.equals(name))
			return this.regionRemover;
		else return null;
	}
	
	private abstract class RegionMarkupTool implements ImageMarkupTool {
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	get region types
			ImPage[] pages = doc.getPages();
			TreeSet allRegionTypes = new TreeSet();
			TreeSet paintedRegionTypes = new TreeSet();
			for (int p = 0; p < pages.length; p++) {
				String[] pageRegionTypes = pages[p].getRegionTypes();
				for (int t = 0; t < pageRegionTypes.length; t++) {
					allRegionTypes.add(pageRegionTypes[t]);
					if (idmp.areRegionsPainted(pageRegionTypes[t]))
						paintedRegionTypes.add(pageRegionTypes[t]);
				}
			}
			
			//	nothing to work with
			if (allRegionTypes.isEmpty()) {
				DialogFactory.alert("There are no regions in this document.", "No Regions", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			
			//	get region types
			String[] regionTypes = (paintedRegionTypes.isEmpty() ? ((String[]) allRegionTypes.toArray(new String[allRegionTypes.size()])) : ((String[]) paintedRegionTypes.toArray(new String[paintedRegionTypes.size()])));
			String regionType = ImUtils.promptForObjectType("Select Region Type", "Select type of regions", regionTypes, regionTypes[0], false);
			if (regionType == null)
				return;
			
			//	process regions of selected type
			this.processRegions(doc, pages, regionType);
		}
		abstract void processRegions(ImDocument doc, ImPage[] pages, String regionType);
	}
	
	private class RegionConverter extends RegionMarkupTool {
		public String getLabel() {
			return "Regions To Annotations";
		}
		public String getTooltip() {
			return "Convert regions into annotations";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		void processRegions(ImDocument doc, ImPage[] pages, String regionType) {
			for (int p = 0; p < pages.length; p++) {
				ImRegion[] pageRegions = pages[p].getRegions(regionType);
				for (int r = 0; r < pageRegions.length; r++) {
					ImWord[] regionWords = pageRegions[r].getWords();
					if (regionWords.length == 0)
						continue;
					Arrays.sort(regionWords, ImUtils.textStreamOrder);
					
					ImWord firstWord = regionWords[0];
					ImWord lastWord = regionWords[0];
					for (int w = 1; w < regionWords.length; w++) {
						if (firstWord.getTextStreamId().equals(regionWords[w].getTextStreamId()))
							lastWord = regionWords[w];
						else {
							ImAnnotation regionAnnot = doc.addAnnotation(firstWord, lastWord, regionType);
							regionAnnot.copyAttributes(pageRegions[r]);
							firstWord = regionWords[w];
							lastWord = regionWords[w];
						}
					}
					ImAnnotation regionAnnot = doc.addAnnotation(firstWord, lastWord, regionType);
					regionAnnot.copyAttributes(pageRegions[r]);
					
					pages[p].removeRegion(pageRegions[r]);
				}
			}
		}
	}
	
	private class RegionRetyper implements ImageMarkupTool {
		public String getLabel() {
			return "Change Region Type";
		}
		public String getTooltip() {
			return "Replace a region type with another one";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	get region types
			ImPage[] pages = doc.getPages();
			TreeSet allRegionTypes = new TreeSet();
			TreeSet paintedRegionTypes = new TreeSet();
			for (int p = 0; p < pages.length; p++) {
				String[] pageRegionTypes = pages[p].getRegionTypes();
				for (int t = 0; t < pageRegionTypes.length; t++) {
					allRegionTypes.add(pageRegionTypes[t]);
					if (idmp.areRegionsPainted(pageRegionTypes[t]))
						paintedRegionTypes.add(pageRegionTypes[t]);
				}
			}
			
			//	nothing to work with
			if (allRegionTypes.isEmpty()) {
				DialogFactory.alert("There are no regions in this document.", "No Regions", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			
			//	get region types
			String[] regionTypes = (paintedRegionTypes.isEmpty() ? ((String[]) allRegionTypes.toArray(new String[allRegionTypes.size()])) : ((String[]) paintedRegionTypes.toArray(new String[paintedRegionTypes.size()])));
			StringPair regionTypeChange = ImUtils.promptForObjectTypeChange("Select Region Type", "Select type of regions to change", "Select or enter new region type", regionTypes, regionTypes[0], true);
			if (regionTypeChange == null)
				return;
			
			//	process regions of selected type
			for (int p = 0; p < pages.length; p++) {
				ImRegion[] pageRegions = pages[p].getRegions(regionTypeChange.strOld);
				for (int r = 0; r < pageRegions.length; r++)
					pageRegions[r].setType(regionTypeChange.strNew);
			}
		}
	}
	
	private class RegionRemover extends RegionMarkupTool {
		public String getLabel() {
			return "Remove Regions";
		}
		public String getTooltip() {
			return "Remove regions from document";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		void processRegions(ImDocument doc, ImPage[] pages, String regionType) {
			for (int p = 0; p < pages.length; p++) {
				ImRegion[] pageRegions = pages[p].getRegions(regionType);
				for (int r = 0; r < pageRegions.length; r++)
					pages[p].removeRegion(pageRegions[r]);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#typeChanged(de.uka.ipd.idaho.im.ImObject, java.lang.String, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void typeChanged(ImObject object, String oldType, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#attributeChanged(de.uka.ipd.idaho.im.ImObject, java.lang.String, java.lang.Object, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void attributeChanged(ImObject object, String attributeName, Object oldValue, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#regionAdded(de.uka.ipd.idaho.im.ImRegion, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void regionAdded(ImRegion region, ImDocumentMarkupPanel idmp, boolean allowPrompt) {
		
		//	we're only interested in paragraphs
		if (!ImRegion.PARAGRAPH_TYPE.equals(region.getType()))
			return;
		
		//	get words
		ImWord[] pWords = region.getWords();
		if (pWords.length == 0)
			return;
		
		//	remove artifacts and deleted words
		ArrayList pWordList = new ArrayList();
		for (int w = 0; w < pWords.length; w++) {
			if (!ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(pWords[w].getTextStreamType()) && !ImWord.TEXT_STREAM_TYPE_DELETED.equals(pWords[w].getTextStreamType()))
				pWordList.add(pWords[w]);
		}
		if (pWordList.isEmpty())
			return;
		if (pWordList.size() < pWords.length)
			pWords = ((ImWord[]) pWordList.toArray(new ImWord[pWordList.size()]));
		
		//	check text stream order
		ImUtils.orderStream(pWords, ImUtils.leftRightTopDownOrder);
		
		//	remove internal paragraph breaks
		for (int w = 0; w < (pWords.length - 1); w++) {
			if (pWords[w].getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
				pWords[w].setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
		}
		
		//	if predecessor of first word is further up on same page, mark logical paragraph break if nothing in between
		ImWord startWordPrev = pWords[0].getPreviousWord();
		if ((startWordPrev != null) && (startWordPrev.pageId == pWords[0].pageId) && (startWordPrev.centerY < pWords[0].bounds.top) && (startWordPrev.getNextRelation() != ImWord.NEXT_RELATION_PARAGRAPH_END)) {
			BoundingBox between = new BoundingBox(region.bounds.left, region.bounds.right, startWordPrev.bounds.bottom, pWords[0].bounds.top);
			ImWord[] betweenWords = region.getPage().getWordsInside(between);
			if (betweenWords.length == 0)
				startWordPrev.setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
		}
		
		//	if successor of last word is further down on same page, mark logical paragraph break if nothing in between
		ImWord endWordNext = pWords[pWords.length-1].getNextWord();
		if ((endWordNext != null) && (endWordNext.pageId == pWords[pWords.length-1].pageId) && (endWordNext.centerY > pWords[pWords.length-1].bounds.bottom) && (pWords[pWords.length-1].getNextRelation() != ImWord.NEXT_RELATION_PARAGRAPH_END)) {
			BoundingBox between = new BoundingBox(region.bounds.left, region.bounds.right, pWords[pWords.length-1].bounds.bottom, endWordNext.bounds.top);
			ImWord[] betweenWords = region.getPage().getWordsInside(between);
			if (betweenWords.length == 0)
				pWords[pWords.length-1].setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#regionRemoved(de.uka.ipd.idaho.im.ImRegion, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void regionRemoved(ImRegion region, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#annotationAdded(de.uka.ipd.idaho.im.ImAnnotation, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void annotationAdded(ImAnnotation annotation, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#annotationRemoved(de.uka.ipd.idaho.im.ImAnnotation, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void annotationRemoved(ImAnnotation annotation, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, final ImPage page, final ImDocumentMarkupPanel idmp) {
		
		//	mark selection
		BoundingBox selectedBox = new BoundingBox(Math.min(start.x, end.x), Math.max(start.x, end.x), Math.min(start.y, end.y), Math.max(start.y, end.y));
		
		//	get selected and context regions
		ImRegion[] pageRegions = page.getRegions();
		LinkedList selectedRegionList = new LinkedList();
		LinkedList contextRegionList = new LinkedList();
		for (int r = 0; r < pageRegions.length; r++) {
			if (!idmp.areRegionsPainted(pageRegions[r].getType()))
				continue;
			if (pageRegions[r].bounds.includes(selectedBox, true))
				contextRegionList.add(pageRegions[r]);
			if (pageRegions[r].bounds.right < selectedBox.left)
				continue;
			if (selectedBox.right < pageRegions[r].bounds.left)
				continue;
			if (pageRegions[r].bounds.bottom < selectedBox.top)
				continue;
			if (selectedBox.bottom < pageRegions[r].bounds.top)
				continue;
			selectedRegionList.add(pageRegions[r]);
		}
		final ImRegion[] selectedRegions = ((ImRegion[]) selectedRegionList.toArray(new ImRegion[selectedRegionList.size()]));
		final ImRegion[] contextRegions = ((ImRegion[]) contextRegionList.toArray(new ImRegion[contextRegionList.size()]));
		
		//	index selected regions by type for group removal
		final TreeMap selectedRegionsByType = new TreeMap();
		final TreeMap multiSelectedRegionsByType = new TreeMap();
		for (int r = 0; r < selectedRegions.length; r++) {
			LinkedList typeRegions = ((LinkedList) selectedRegionsByType.get(selectedRegions[r].getType()));
			if (typeRegions == null) {
				typeRegions = new LinkedList();
				selectedRegionsByType.put(selectedRegions[r].getType(), typeRegions);
			}
			typeRegions.add(selectedRegions[r]);
			if (typeRegions.size() > 1)
				multiSelectedRegionsByType.put(selectedRegions[r].getType(), typeRegions);
		}
		
		//	index context regions by type
		final TreeMap contextRegionsByType = new TreeMap();
		for (int r = 0; r < contextRegions.length; r++) {
			LinkedList typeRegions = ((LinkedList) contextRegionsByType.get(contextRegions[r].getType()));
			if (typeRegions == null) {
				typeRegions = new LinkedList();
				contextRegionsByType.put(contextRegions[r].getType(), typeRegions);
			}
			typeRegions.add(contextRegions[r]);
		}
		
		//	get selected words
		ImWord[] selectedWords = page.getWordsInside(selectedBox);
		
		//	collect actions
		LinkedList actions = new LinkedList();
		
		//	get region types
		final String[] regionTypes = page.getRegionTypes();
		final TreeSet paintedRegionTypes = new TreeSet();
		for (int t = 0; t < regionTypes.length; t++) {
			if (idmp.areRegionsPainted(regionTypes[t]))
				paintedRegionTypes.add(regionTypes[t]);
		}
		
		//	get bounding box surrounding selected words
		final BoundingBox selectedBounds;
		if (selectedWords.length == 0) {
			int sLeft = Math.max(page.bounds.left, selectedBox.left);
			int sRight = Math.min(page.bounds.right, selectedBox.right);
			int sTop = Math.max(page.bounds.top, selectedBox.top);
			int sBottom = Math.min(page.bounds.bottom, selectedBox.bottom);
			selectedBounds = new BoundingBox(sLeft, sRight, sTop, sBottom);
		}
		else {
			int sLeft = page.bounds.right;
			int sRight = page.bounds.left;
			int sTop = page.bounds.bottom;
			int sBottom = page.bounds.top;
			for (int w = 0; w < selectedWords.length; w++) {
				sLeft = Math.min(sLeft, selectedWords[w].bounds.left);
				sRight = Math.max(sRight, selectedWords[w].bounds.right);
				sTop = Math.min(sTop, selectedWords[w].bounds.top);
				sBottom = Math.max(sBottom, selectedWords[w].bounds.bottom);
			}
			selectedBounds = new BoundingBox(sLeft, sRight, sTop, sBottom);
		}
		
		//	mark region
		if (paintedRegionTypes.isEmpty())
			actions.add(new SelectionAction("markRegion", "Mark Region", ("Mark a region from the selection")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					String regionType = ImUtils.promptForObjectType("Enter Region Type", "Enter or select type of region to create", regionTypes, null, true);
					if (regionType == null)
						return false;
					ImRegion region = new ImRegion(page, selectedBounds, regionType);
					if (ImRegion.BLOCK_ANNOTATION_TYPE.equals(regionType))
						ensureBlockStructure(region);
					idmp.setRegionsPainted(regionType, true);
					return true;
				}
			});
		else actions.add(new SelectionAction("markRegion", "Mark Region", ("Mark a region from the selection")) {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				return false;
			}
			public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
				JMenu pm = new JMenu("Mark Region");
				JMenuItem mi = new JMenuItem("Mark Region ...");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						String regionType = ImUtils.promptForObjectType("Enter Region Type", "Enter or select type of region to create", regionTypes, null, true);
						if (regionType != null) {
							invoker.beginAtomicAction("Mark '" + regionType + "' Region");
							ImRegion region = new ImRegion(page, selectedBounds, regionType);
							if (ImRegion.BLOCK_ANNOTATION_TYPE.equals(regionType))
								ensureBlockStructure(region);
							invoker.endAtomicAction();
							invoker.setRegionsPainted(regionType, true);
							invoker.validate();
							invoker.repaint();
						}
					}
				});
				pm.add(mi);
				for (Iterator rtit = paintedRegionTypes.iterator(); rtit.hasNext();) {
					final String regionType = ((String) rtit.next());
					mi = new JMenuItem("- " + regionType);
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							invoker.beginAtomicAction("Mark '" + regionType + "' Region");
							ImRegion region = new ImRegion(page, selectedBounds, regionType);
							if (ImRegion.BLOCK_ANNOTATION_TYPE.equals(regionType))
								ensureBlockStructure(region);
							invoker.endAtomicAction();
							invoker.setRegionsPainted(regionType, true);
							invoker.validate();
							invoker.repaint();
						}
					});
					pm.add(mi);
				}
				return pm;
			}
		});
		
		//	no words selected
		if (selectedWords.length == 0) {
			
			//	no region selected, either
			if (selectedRegions.length == 0) {
				actions.add(new SelectionAction("editAttributesPage", ("Edit Page Attributes"), ("Edit attributes of page.")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						idmp.editAttributes(page, PAGE_TYPE, "");
						return true;
					}
				});
				if (!idmp.documentBornDigital) {
					actions.add(new SelectionAction("editPage", ("Edit Page Image & Words"), ("Edit page image and words recognized in page.")) {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return idmp.editPage(page.pageId);
						}
					});
					actions.add(new SelectionAction("cleanPageRegions", ("Cleanup Page Regions"), ("Clean up and sanitize all regions in this page, remove duplicates, etc.")) {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return cleanupPageRegions(page);
						}
					});
				}
			}
			
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
			
			//	mark selected non-white area as image (case with words comes from text block actions)
			if (isImage)
				actions.add(new SelectionAction("markRegionImage", "Mark Image", "Mark selected region as an image.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return markImageOrGraphics(page, selectedBounds, selectedRegions, ImRegion.IMAGE_TYPE, idmp);
					}
				});
			
			//	mark selected non-white area as graphics (case with words comes from text block actions)
			else actions.add(new SelectionAction("markRegionGraphics", "Mark Graphics", "Mark selected region as a vector based graphics.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return markImageOrGraphics(page, selectedBounds, selectedRegions, ImRegion.GRAPHICS_TYPE, idmp);
				}
			});
		}
		
		//	single region selected
		if (selectedRegions.length == 1) {
			
			//	edit attributes of existing region
			actions.add(new SelectionAction("editAttributesRegion", ("Edit " + selectedRegions[0].getType() + " Attributes"), ("Edit attributes of '" + selectedRegions[0].getType() + "' region.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					idmp.editAttributes(selectedRegions[0], selectedRegions[0].getType(), "");
					return true;
				}
			});
			
			//	remove existing annotation
			actions.add(new SelectionAction("removeRegion", ("Remove " + selectedRegions[0].getType() + " Region"), ("Remove '" + selectedRegions[0].getType() + "' region.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					page.removeRegion(selectedRegions[0]);
					return true;
				}
			});
			
			//	change type of existing annotation
			actions.add(new SelectionAction("changeTypeRegion", ("Change Region Type"), ("Change type of '" + selectedRegions[0].getType() + "' region.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					String regionType = ImUtils.promptForObjectType("Enter Region Type", "Enter or select new region type", page.getRegionTypes(), selectedRegions[0].getType(), true);
					if (regionType == null)
						return false;
					selectedRegions[0].setType(regionType);
					idmp.setRegionsPainted(regionType, true);
					return true;
				}
			});
		}
		
		//	multiple regions selected
		else if (selectedRegions.length > 1) {
			
			//	edit region attributes
			actions.add(new SelectionAction("editAttributesRegion", "Edit Region Attributes ...", "Edit attributes of selected regions.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Edit Region Attributes ...");
					JMenuItem mi;
					for (int t = 0; t < selectedRegions.length; t++) {
						final ImRegion selectedRegion = selectedRegions[t];
						mi = new JMenuItem("- " + selectedRegion.getType() + " at " + selectedRegion.bounds.toString());
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								idmp.editAttributes(selectedRegion, selectedRegion.getType(), "");
								invoker.validate();
								invoker.repaint();
							}
						});
						pm.add(mi);
					}
					return pm;
				}
			});
			
			//	remove existing annotation
			actions.add(new SelectionAction("removeRegion", "Remove Region ...", "Remove selected regions.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Remove Region ...");
					JMenuItem mi;
					for (int t = 0; t < selectedRegions.length; t++) {
						final ImRegion selectedRegion = selectedRegions[t];
						mi = new JMenuItem("- " + selectedRegion.getType() + " at " + selectedRegion.bounds.toString());
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								invoker.beginAtomicAction("Remove '" + selectedRegion.getType() + "' Region");
								page.removeRegion(selectedRegion);
								invoker.endAtomicAction();
								invoker.validate();
								invoker.repaint();
							}
						});
						pm.add(mi);
					}
					return pm;
				}
			});
			
			//	remove regions of some type together
			if (multiSelectedRegionsByType.size() == 1)
				actions.add(new SelectionAction("removeTypeRegion", ("Remove " + multiSelectedRegionsByType.firstKey() + " Regions"), ("Remove all selected '" + multiSelectedRegionsByType.firstKey() + "' regions.")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						LinkedList typeRegs = ((LinkedList) multiSelectedRegionsByType.get(multiSelectedRegionsByType.firstKey()));
						while (typeRegs.size() != 0)
							page.removeRegion((ImRegion) typeRegs.removeFirst());
						return true;
					}
				});
			else if (multiSelectedRegionsByType.size() > 1)
				actions.add(new SelectionAction("removeTypeRegion", "Remove Regions ...", "Remove selected regions by type.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return false;
					}
					public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
						JMenu pm = new JMenu("Remove Regions ...");
						JMenuItem mi;
						for (Iterator rtit = multiSelectedRegionsByType.keySet().iterator(); rtit.hasNext();) {
							final String regType = ((String) rtit.next());
							final LinkedList typeRegs = ((LinkedList) multiSelectedRegionsByType.get(regType));
							mi = new JMenuItem("- " + regType + " (" + typeRegs.size() + ")");
							mi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									invoker.beginAtomicAction("Remove '" + regType + "' Regions");
									while (typeRegs.size() != 0)
										page.removeRegion((ImRegion) typeRegs.removeFirst());
									invoker.endAtomicAction();
									invoker.validate();
									invoker.repaint();
								}
							});
							pm.add(mi);
						}
						return pm;
					}
				});
			
			//	change type of existing annotation
			actions.add(new SelectionAction("changeTypeRegion", "Change Region Type ...", "Change the type of selected regions.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Change Region Type ...");
					JMenuItem mi;
					for (int t = 0; t < selectedRegions.length; t++) {
						final ImRegion selectedRegion = selectedRegions[t];
						mi = new JMenuItem("- " + selectedRegion.getType() + " " + selectedRegion.bounds.toString());
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								String regionType = ImUtils.promptForObjectType("Enter Region Type", "Enter or select new region type", page.getRegionTypes(), selectedRegion.getType(), true);
								if (regionType != null) {
									invoker.beginAtomicAction("Chage Region Type");
									selectedRegion.setType(regionType);
									invoker.endAtomicAction();
									invoker.setAnnotationsPainted(regionType, true);
									invoker.validate();
									invoker.repaint();
								}
							}
						});
						pm.add(mi);
					}
					return pm;
				}
			});
		}
		
		//	change region into annotation if eligible and likely
		for (int r = 0; r < selectedRegions.length; r++) {
			
			//	this one is not alone, selection looks like having other purpose
			if (multiSelectedRegionsByType.containsKey(selectedRegions[r].getType()))
				continue;
			
			//	get region words
			ImWord[] regionWords = page.getWordsInside(selectedRegions[r].bounds);
			if (regionWords.length == 0)
				continue;
			
			//	order words
			Arrays.sort(regionWords, ImUtils.textStreamOrder);
			
			//	check if we have a single continuous stream
			boolean streamBroken = false;
			for (int w = 1; w < selectedWords.length; w++)
				if (selectedWords[w].getPreviousWord() != selectedWords[w-1]) {
					streamBroken = true;
					break;
				}
			
			//	can we work with this one?
			if (streamBroken)
				continue;
			
			//	offer transforming region into annotation
			final ImWord firstWord = regionWords[0];
			final ImWord lastWord = regionWords[regionWords.length-1];
			final String regionType = selectedRegions[r].getType();
			final ImRegion selectedRegion = selectedRegions[r];
			actions.add(new SelectionAction("annotateRegion", ("Annotate '" + regionType + "' Region"), ("Create an annotation from '" + regionType + "' region.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					page.removeRegion(selectedRegion);
					idmp.document.addAnnotation(firstWord, lastWord, regionType);
					idmp.setAnnotationsPainted(regionType, true);
					return true;
				}
			});
		}
		
		//	offer paragraph actions
		if (idmp.areRegionsPainted(ImRegion.PARAGRAPH_TYPE)) {
			
			//	if part of a paragraph is selected, offer splitting
			if (contextRegionsByType.containsKey(ImRegion.PARAGRAPH_TYPE) && !multiSelectedRegionsByType.containsKey(ImRegion.PARAGRAPH_TYPE)) {
				final ImRegion paragraph = ((ImRegion) ((LinkedList) contextRegionsByType.get(ImRegion.PARAGRAPH_TYPE)).getFirst());
				final ImRegion[] paragraphLines = paragraph.getRegions(ImRegion.LINE_ANNOTATION_TYPE);
				final ImRegion[] selectedLines = page.getRegionsInside(selectedBox, true);
				if ((selectedLines.length != 0) && (selectedLines.length < paragraphLines.length))
					actions.add(new SelectionAction("paragraphsSplit", "Split 'paragraph' Region", "Split the selected paragraph.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							if (sortLinesIntoParagraphs(page, paragraphLines, selectedBounds)) {
								page.removeRegion(paragraph);
								return true;
							}
							else return false;
						}
					});
			}
			
			//	if two or more paragraphs are selected, offer merging
			if (multiSelectedRegionsByType.containsKey(ImRegion.PARAGRAPH_TYPE)) {
				final LinkedList paragraphList = ((LinkedList) multiSelectedRegionsByType.get(ImRegion.PARAGRAPH_TYPE));
				actions.add(new SelectionAction("paragraphsMerge", "Merge 'paragraph' Regions", "Merge the selected paragraphs.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						ImRegion[] paragraphs = ((ImRegion[]) paragraphList.toArray(new ImRegion[paragraphList.size()]));
						ImRegion mergedParagraph = new ImRegion(page, ImLayoutObject.getAggregateBox(paragraphs), ImRegion.PARAGRAPH_TYPE);
						mergedParagraph.copyAttributes(paragraphs[0]);
						for (int p = 0; p < paragraphs.length; p++)
							page.removeRegion(paragraphs[p]);
						ImWord[] mergedParagraphWords = mergedParagraph.getWords();
						if (mergedParagraphWords.length != 0) {
							Arrays.sort(mergedParagraphWords, ImUtils.textStreamOrder);
							for (int w = 0; w < (mergedParagraphWords.length-1); w++) {
								if (mergedParagraphWords[w].getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
									mergedParagraphWords[w].setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
							}
						}
						return true;
					}
				});
			}
			
			//	if blocks and paragraphs visible and click inside block or block selected, offer re-detecting paragraphs
			if (idmp.areRegionsPainted(ImRegion.BLOCK_ANNOTATION_TYPE) && (selectedRegionsByType.containsKey(ImRegion.BLOCK_ANNOTATION_TYPE) || contextRegionsByType.containsKey(ImRegion.BLOCK_ANNOTATION_TYPE)) && !multiSelectedRegionsByType.containsKey(ImRegion.BLOCK_ANNOTATION_TYPE)) {
				
				//	get block
				final ImRegion block;
				if (selectedRegionsByType.containsKey(ImRegion.BLOCK_ANNOTATION_TYPE))
					block = ((ImRegion) ((LinkedList) selectedRegionsByType.get(ImRegion.BLOCK_ANNOTATION_TYPE)).getFirst());
				else block = ((ImRegion) ((LinkedList) contextRegionsByType.get(ImRegion.BLOCK_ANNOTATION_TYPE)).getFirst());
				
				//	get lines
				final ImRegion[] blockLines = block.getRegions(ImRegion.LINE_ANNOTATION_TYPE);
				if (blockLines.length > 1)
					actions.add(new SelectionAction("paragraphsInBlock", "Revise Block Paragraphs", "Revise the grouping of the lines in the block into paragraphs.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return restructureBlock(page, block, block.getRegions(ImRegion.PARAGRAPH_TYPE), blockLines, idmp.getMaxPageImageDpi());
						}
					});
			}
			
			//	if blocks and paragraphs visible and multiple blocks selected, offer merging blocks and re-detecting paragraphs and lines
			if (idmp.areRegionsPainted(ImRegion.BLOCK_ANNOTATION_TYPE) && multiSelectedRegionsByType.containsKey(ImRegion.BLOCK_ANNOTATION_TYPE))
				actions.add(new SelectionAction("paragraphsInBlock", "Merge Blocks", "Merge selected blocks, re-detect lines, and group them into paragraphs.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						
						//	get & merge blocks
						LinkedList blockList = ((LinkedList) multiSelectedRegionsByType.get(ImRegion.BLOCK_ANNOTATION_TYPE));
						ImRegion[] blocks = ((ImRegion[]) blockList.toArray(new ImRegion[blockList.size()]));
						for (int b = 0; b < blocks.length; b++)
							page.removeRegion(blocks[b]);
						ImRegion block = new ImRegion(page, ImLayoutObject.getAggregateBox(blocks), ImRegion.BLOCK_ANNOTATION_TYPE);
						
						//	remove old lines and paragraphs
						ImRegion[] blockParagraphs = block.getRegions(ImRegion.PARAGRAPH_TYPE);
						for (int p = 0; p < blockParagraphs.length; p++)
							page.removeRegion(blockParagraphs[p]);
						ImRegion[] blockLines = block.getRegions(ImRegion.LINE_ANNOTATION_TYPE);
						for (int l = 0; l < blockLines.length; l++)
							page.removeRegion(blockLines[l]);
						
						//	get block words
						ImWord[] blockWords = block.getWords();
						sortIntoLines(page, blockWords);
						
						//	re-detect paragraphs
						PageAnalysis.splitIntoParagraphs(block, page.getImageDPI(), null);
						
						//	update text stream structure
						updateBlockTextStream(block);
						
						//	finally ...
						return true;
					}
				});
			
			//	if one block partially selected, offer splitting
			if (idmp.areRegionsPainted(ImRegion.BLOCK_ANNOTATION_TYPE) && contextRegionsByType.containsKey(ImRegion.BLOCK_ANNOTATION_TYPE) && !multiSelectedRegionsByType.containsKey(ImRegion.BLOCK_ANNOTATION_TYPE)) {
				final ImRegion block = ((ImRegion) ((LinkedList) contextRegionsByType.get(ImRegion.BLOCK_ANNOTATION_TYPE)).getFirst());
				actions.add(new SelectionAction("paragraphsInBlock", "Split Block", "Split selected block, re-detect lines, and group them into paragraphs.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return splitBlock(page, block, selectedBounds);
					}
				});
			}
		}
		
		//	get supplements if we might need them for images or graphics
		final ImSupplement[] supplements = ((idmp.areRegionsPainted(ImRegion.IMAGE_TYPE) || idmp.areRegionsPainted(ImRegion.GRAPHICS_TYPE)) ? page.getSupplements() : null);
		
		//	offer copying selected figure to clipboard, as well as assigning caption
		if (idmp.areRegionsPainted(ImRegion.IMAGE_TYPE)) {
			LinkedHashSet selectedImages = new LinkedHashSet();
			for (int r = 0; r < contextRegions.length; r++) {
				if (ImRegion.IMAGE_TYPE.equals(contextRegions[r].getType()))
					selectedImages.add(contextRegions[r]);
			}
			for (int r = 0; r < selectedRegions.length; r++) {
				if (ImRegion.IMAGE_TYPE.equals(selectedRegions[r].getType()))
					selectedImages.add(selectedRegions[r]);
			}
			if (selectedImages.size() == 1) {
				final ImRegion selImage = ((ImRegion) selectedImages.iterator().next());
				actions.add(new TwoClickSelectionAction("assignCaptionImage", "Assign Caption", "Assign a caption to this image with a second click.") {
					private ImWord artificialStartWord = null;
					public boolean performAction(ImWord secondWord) {
						if (!ImWord.TEXT_STREAM_TYPE_CAPTION.equals(secondWord.getTextStreamType()))
							return false;
						
						//	find affected caption
						ImAnnotation[] wordAnnots = idmp.document.getAnnotationsSpanning(secondWord);
						ArrayList wordCaptions = new ArrayList(2);
						for (int a = 0; a < wordAnnots.length; a++) {
							if (ImAnnotation.CAPTION_TYPE.equals(wordAnnots[a].getType()))
								wordCaptions.add(wordAnnots[a]);
						}
						if (wordCaptions.size() != 1)
							return false;
						ImAnnotation wordCaption = ((ImAnnotation) wordCaptions.get(0));
						
						//	does this caption match?
						if (wordCaption.getFirstWord().getString().toLowerCase().startsWith("tab"))
							return false;
						
						//	set attributes
						wordCaption.setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, selImage.bounds.toString());
						wordCaption.setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + selImage.pageId));
						return true;
					}
					public ImWord getFirstWord() {
						if (this.artificialStartWord == null)
							this.artificialStartWord = new ImWord(selImage.getDocument(), selImage.pageId, selImage.bounds, "IMAGE");
						return this.artificialStartWord;
					}
					public String getActiveLabel() {
						return ("Click on a caption to assign it to the image at " + selImage.bounds.toString() + " on page " + (selImage.pageId + 1));
					}
				});
				final Figure[] clickedFigures = ImSupplement.getFiguresAt(supplements, selectedBox);
				final Figure[] contextFigures = ImSupplement.getFiguresIn(supplements, selImage.bounds);
				if (clickedFigures.length < 2) // clicked single figure, or we have a scan and no figures at all
					actions.add(new SelectionAction("copyImage", "Copy Image", "Copy the selected image to the system clipboard.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							Image image = null;
							try {
								if (clickedFigures.length == 1)
									image = ImageIO.read(clickedFigures[0].getInputStream());
								else for (int s = 0; s < supplements.length; s++)
									if (supplements[s] instanceof Scan) {
										BufferedImage scan = ImageIO.read(((Scan) supplements[s]).getInputStream());
										image = scan.getSubimage(selImage.bounds.left, selImage.bounds.top, (selImage.bounds.right - selImage.bounds.left), (selImage.bounds.bottom - selImage.bounds.top));
									}
							} catch (Exception e) {}
							if (image == null) {
								PageImage pageImage = page.getPageImage();
								if (pageImage != null)
									image = pageImage.image.getSubimage(selImage.bounds.left, selImage.bounds.top, (selImage.bounds.right - selImage.bounds.left), (selImage.bounds.bottom - selImage.bounds.top));
							}
							if (image != null)
								ImUtils.copy(new ImageSelection(image));
							return false;
						}
					});
				if (clickedFigures.length == 1) // clicked single figure
					actions.add(new SelectionAction("saveImage", "Save Image", "Save the selected image to disk.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							String imageFileName = getExportFileName(idmp.document, page, selImage);
							File imageFile = chooseExportFile(imageFileName, null, false);
							if (imageFile == null)
								return false;
							
							//	write image (we can simply loop through PNG)
							try {
								OutputStream out = new BufferedOutputStream(new FileOutputStream(imageFile));
								if (clickedFigures[0].getMimeType().toLowerCase().endsWith("/png")) {
									InputStream in = new BufferedInputStream(clickedFigures[0].getInputStream());
									byte[] buffer = new byte[1024];
									for (int r; (r = in.read(buffer, 0, buffer.length)) != -1;)
										out.write(buffer, 0, r);
									in.close();
								}
								else {
									BufferedImage image = ImageIO.read(clickedFigures[0].getInputStream());
									ImageIO.write(image, "PNG", out);
								}
								out.flush();
								out.close();
							}
							catch (IOException ioe) {
								ioe.printStackTrace(System.out);
								DialogFactory.alert(("The selected image could not be saved:\r\n" + ioe.getMessage()), "Error Saving Image", JOptionPane.ERROR_MESSAGE);
							}
							
							//	we did not change anything ...
							return false;
						}
					});
				if (contextFigures.length > 1) {// we have an image group to copy or save
					actions.add(new SelectionAction("copyImageGroup", "Copy Image Group", "Copy the whole selected image group to the system clipboard.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							copyCompositeImage(contextFigures, -1, null, null, page);
							return false;
						}
					});
					actions.add(new SelectionAction("saveImageGroup", "Save Image Group", "Save the whole selected image group to disk.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							String imageFileName = getExportFileName(idmp.document, page, selImage);
							File imageFile = chooseExportFile(imageFileName, null, false);
							if (imageFile != null)
								saveCompositeImage(imageFile, contextFigures, -1, null, null, page);
							return false;
						}
					});
				}
				final Graphics[] contextGraphics = ImSupplement.getGraphicsIn(supplements, selImage.bounds);
				final ImWord[] contextWords = page.getWordsInside(selImage.bounds);
				if ((clickedFigures.length != 0) && ((selectedWords.length + contextGraphics.length) != 0)) {
					actions.add(new SelectionAction("copyImageCustom", "Copy Image or Group ...", "Copy the selected image or group to the system clipboard, with custom options.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							ExportParameterPanel exportParamPanel = new ExportParameterPanel(contextFigures, contextGraphics, contextWords, false, false);
							int choice = DialogFactory.confirm(exportParamPanel, "Image Export Options", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
							if (choice == JOptionPane.OK_OPTION) {
								int exportDpi = exportParamPanel.getDpi();
								copyCompositeImage(contextFigures, exportDpi, (exportParamPanel.includeGraphics() ? contextGraphics : null), (exportParamPanel.includeWords() ? contextWords : null), page);
							}
							return false;
						}
					});
					actions.add(new SelectionAction("saveImageCustom", "Save Image or Group ...", "Save the selected image or group to disk, with custom options.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							ExportParameterPanel exportParamPanel = new ExportParameterPanel(contextFigures, contextGraphics, contextWords, false, true);
							String imageFileName = getExportFileName(idmp.document, page, selImage);
							File imageFile = chooseExportFile(imageFileName, exportParamPanel, false);
							if (imageFile != null)
								saveCompositeImage(imageFile, contextFigures, exportParamPanel.getDpi(), (exportParamPanel.includeGraphics() ? contextGraphics : null), (exportParamPanel.includeWords() ? contextWords : null), page);
							return false;
						}
					});
				}
			}
		}
		
		//	offer copying selected graphics to clipboard, as well as assigning caption
		if (idmp.areRegionsPainted(ImRegion.GRAPHICS_TYPE)) {
			LinkedHashSet selectedGraphics = new LinkedHashSet();
			for (int r = 0; r < contextRegions.length; r++) {
				if (ImRegion.GRAPHICS_TYPE.equals(contextRegions[r].getType()))
					selectedGraphics.add(contextRegions[r]);
			}
			for (int r = 0; r < selectedRegions.length; r++) {
				if (ImRegion.GRAPHICS_TYPE.equals(selectedRegions[r].getType()))
					selectedGraphics.add(selectedRegions[r]);
			}
			if (selectedGraphics.size() == 1) {
				final ImRegion selGraphics = ((ImRegion) selectedGraphics.iterator().next());
				actions.add(new TwoClickSelectionAction("assignCaptionGraphics", "Assign Caption", "Assign a caption to this graphics with a second click.") {
					private ImWord artificialStartWord = null;
					public boolean performAction(ImWord secondWord) {
						if (!ImWord.TEXT_STREAM_TYPE_CAPTION.equals(secondWord.getTextStreamType()))
							return false;
						
						//	find affected caption
						ImAnnotation[] wordAnnots = idmp.document.getAnnotationsSpanning(secondWord);
						ArrayList wordCaptions = new ArrayList(2);
						for (int a = 0; a < wordAnnots.length; a++) {
							if (ImAnnotation.CAPTION_TYPE.equals(wordAnnots[a].getType()))
								wordCaptions.add(wordAnnots[a]);
						}
						if (wordCaptions.size() != 1)
							return false;
						ImAnnotation wordCaption = ((ImAnnotation) wordCaptions.get(0));
						
						//	does this caption match?
						if (wordCaption.getFirstWord().getString().toLowerCase().startsWith("tab"))
							return false;
						
						//	set attributes
						wordCaption.setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, selGraphics.bounds.toString());
						wordCaption.setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + selGraphics.pageId));
						return true;
					}
					public ImWord getFirstWord() {
						if (this.artificialStartWord == null)
							this.artificialStartWord = new ImWord(selGraphics.getDocument(), selGraphics.pageId, selGraphics.bounds, "IMAGE");
						return this.artificialStartWord;
					}
					public String getActiveLabel() {
						return ("Click on a caption to assign it to the graphics at " + selGraphics.bounds.toString() + " on page " + (selGraphics.pageId + 1));
					}
				});
				final Graphics[] clickedGraphics = ImSupplement.getGraphicsAt(supplements, selectedBox);
				final Graphics[] contextGraphics = ImSupplement.getGraphicsIn(supplements, selGraphics.bounds);
				if ((clickedGraphics.length + contextGraphics.length) != 0) {
					actions.add(new SelectionAction("copyGraphics", "Copy Graphics", "Copy the selected graphics to the system clipboard.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							copyGraphics(((clickedGraphics.length == 0) ? contextGraphics : clickedGraphics), null, -1, null, page);
							return false;
						}
					});
					actions.add(new SelectionAction("saveGraphics", "Save Graphics", "Save the selected graphics to disk.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							ExportParameterPanel exportParamPanel = new ExportParameterPanel(null, contextGraphics, null, true, true);
							String graphicsFileName = getExportFileName(idmp.document, page, selGraphics);
							File graphicsFile = chooseExportFile(graphicsFileName, exportParamPanel, true);
							if (graphicsFile != null)
								saveGraphics(graphicsFile, ((clickedGraphics.length == 0) ? contextGraphics : clickedGraphics), null, exportParamPanel.getDpi(), null, page);
							return false;
						}
					});
				}
				final Figure[] contextFigures = ImSupplement.getFiguresIn(supplements, selGraphics.bounds);
				final ImWord[] contextWords = page.getWordsInside(selGraphics.bounds);
				if (((selectedWords.length + contextFigures.length) != 0) || true) {
					actions.add(new SelectionAction("copyGraphicsCustom", "Copy Graphics ...", "Copy the selected graphics to the system clipboard, with custom options.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							ExportParameterPanel exportParamPanel = new ExportParameterPanel(contextFigures, contextGraphics, contextWords, true, false);
							int choice = DialogFactory.confirm(exportParamPanel, "Graphics Export Options", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
							if (choice == JOptionPane.OK_OPTION) {
								int exportDpi = exportParamPanel.getDpi();
								copyGraphics(((clickedGraphics.length == 0) ? contextGraphics : clickedGraphics), (exportParamPanel.includeFigures() ? contextFigures : null), exportDpi, (exportParamPanel.includeWords() ? contextWords : null), page);
							}
							return false;
						}
					});
					actions.add(new SelectionAction("saveGraphicsCustom", "Save Graphics ...", "Save the selected graphics to disk, with custom options.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							ExportParameterPanel exportParamPanel = new ExportParameterPanel(contextFigures, contextGraphics, contextWords, true, true);
							String graphicsFileName = getExportFileName(idmp.document, page, selGraphics);
							File graphicsFile = chooseExportFile(graphicsFileName, exportParamPanel, true);
							if (graphicsFile != null)
								saveGraphics(graphicsFile, ((clickedGraphics.length == 0) ? contextGraphics : clickedGraphics), (exportParamPanel.includeFigures() ? contextFigures : null), exportParamPanel.getDpi(), (exportParamPanel.includeWords() ? contextWords : null), page);
							return false;
						}
					});
				}
			}
		}
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	private boolean cleanupPageRegions(ImPage page) {
		
		//	keep track of any changes
		boolean pageRegionsChanged = false;
		
		//	get page regions and words
		ArrayList pageRegions = new ArrayList(Arrays.asList(page.getRegions()));
		ImWord[] pageWords = page.getWords();
		
		//	shrink any text regions to contained words (TODO really also take on columns?)
		for (int r = 0; r < pageRegions.size(); r++) {
			ImRegion pageRegion = ((ImRegion) pageRegions.get(r));
			
			//	discarding any generic 'region' regions right away (TODO do we really want this?)
			if (ImRegion.REGION_ANNOTATION_TYPE.equals(pageRegion.getType())) {
				page.removeRegion(pageRegion);
				pageRegions.remove(r--);
				pageRegionsChanged = true;
				continue;
			}
			
			//	we're only touching text regions here ...
			if (false) {}
			else if (ImRegion.COLUMN_ANNOTATION_TYPE.equals(pageRegion.getType())) {}
			else if (ImRegion.BLOCK_ANNOTATION_TYPE.equals(pageRegion.getType())) {}
			else if (ImRegion.PARAGRAPH_TYPE.equals(pageRegion.getType())) {}
			else if (ImRegion.LINE_ANNOTATION_TYPE.equals(pageRegion.getType())) {}
			else continue;
			
			//	shrink region to words
			ImRegion sPageRegion = this.shrinkToWords(pageRegion, pageWords);
			
			//	no words contained at all
			if (sPageRegion == null) {
				page.removeRegion(pageRegion);
				pageRegions.remove(r--);
				pageRegionsChanged = true;
			}
			
			//	region reduced in size
			else if (sPageRegion != pageRegion) {
				page.removeRegion(pageRegion);
				pageRegions.set(r, sPageRegion);
				pageRegionsChanged = true;
			}
		}
		
		//	shrink any image and graphics regions to content
		AnalysisImage pai = Imaging.wrapImage(page.getImage().image, null);
		for (int r = 0; r < pageRegions.size(); r++) {
			ImRegion pageRegion = ((ImRegion) pageRegions.get(r));
			
			//	we're only touching image and graphics regions here ...
			if (false) {}
			else if (ImRegion.IMAGE_TYPE.equals(pageRegion.getType())) {}
			else if (ImRegion.GRAPHICS_TYPE.equals(pageRegion.getType())) {}
			else continue;
			
			//	shrink region to words
			ImRegion sPageRegion = this.shrinkToContent(pageRegion, pai);
			
			//	nothing contained at all
			if (sPageRegion == null) {
				page.removeRegion(pageRegion);
				pageRegions.remove(r--);
				pageRegionsChanged = true;
			}
			
			//	region reduced in size
			else if (sPageRegion != pageRegion) {
				page.removeRegion(pageRegion);
				pageRegions.set(r, sPageRegion);
				pageRegionsChanged = true;
			}
		}
		
		//	remove any duplicate regions, aggregating attributes
		HashMap pageRegionIndex = new HashMap();
		for (int r = 0; r < pageRegions.size(); r++) {
			ImRegion pageRegion = ((ImRegion) pageRegions.get(r));
			
			//	get any potential duplicate region
			String pageRegionKey = (pageRegion.getType() + "@" + pageRegion.bounds);
			ImRegion dPageRegion = ((ImRegion) pageRegionIndex.get(pageRegionKey));
			if (dPageRegion == null) {
				pageRegionIndex.put(pageRegionKey, pageRegion);
				continue;
			}
			
			//	clean up duplicate region
			AttributeUtils.copyAttributes(pageRegion, dPageRegion, AttributeUtils.ADD_ATTRIBUTE_COPY_MODE);
			page.removeRegion(pageRegion);
			pageRegions.remove(r--);
			pageRegionsChanged = true;
		}
		
		//	remove any text region contained in other region of same type, and aggregate overlapping ones
		for (int r = 0; r < pageRegions.size(); r++) {
			ImRegion pageRegion = ((ImRegion) pageRegions.get(r));
			
			//	we're only touching text regions here ...
			if (false) {}
			else if (ImRegion.COLUMN_ANNOTATION_TYPE.equals(pageRegion.getType())) {}
			else if (ImRegion.BLOCK_ANNOTATION_TYPE.equals(pageRegion.getType())) {}
			else if (ImRegion.PARAGRAPH_TYPE.equals(pageRegion.getType())) {}
			else if (ImRegion.LINE_ANNOTATION_TYPE.equals(pageRegion.getType())) {}
			else continue;
			
			//	compare to contained smaller regions of same type
			for (int cr = (r+1); cr < pageRegions.size(); cr++) {
				ImRegion cPageRegion = ((ImRegion) pageRegions.get(cr));
				if (!pageRegion.getType().equals(cPageRegion.getType()))
					continue;
				
				//	clean up contained region
				if (pageRegion.bounds.includes(cPageRegion.bounds, false)) {
					AttributeUtils.copyAttributes(cPageRegion, pageRegion, AttributeUtils.ADD_ATTRIBUTE_COPY_MODE);
					page.removeRegion(cPageRegion);
					pageRegions.remove(cr--);
					pageRegionsChanged = true;
				}
				
				//	merge with overlapping region
				else if (pageRegion.bounds.overlaps(cPageRegion.bounds)) {
					ImRegion mPageRegion = new ImRegion(page, this.getUnion(pageRegion.bounds, cPageRegion.bounds), pageRegion.getType());
					AttributeUtils.copyAttributes(pageRegion, mPageRegion, AttributeUtils.ADD_ATTRIBUTE_COPY_MODE);
					page.removeRegion(pageRegion);
					pageRegions.set(r--, mPageRegion);
					AttributeUtils.copyAttributes(cPageRegion, mPageRegion, AttributeUtils.ADD_ATTRIBUTE_COPY_MODE);
					page.removeRegion(cPageRegion);
					pageRegions.remove(cr--);
					pageRegionsChanged = true;
					break;
				}
			}
		}
		
		//	finally ...
		return pageRegionsChanged;
	}
	
	private ImRegion shrinkToWords(ImRegion region, ImWord[] pageWords) {
		
		//	compute aggregate bounds of (fuzzy) included words
		int left = Integer.MAX_VALUE;
		int right = 0;
		int top = Integer.MAX_VALUE;
		int bottom = 0;
		for (int w = 0; w < pageWords.length; w++)
			if (region.bounds.includes(pageWords[w].bounds, true)) {
				left = Math.min(left, pageWords[w].bounds.left);
				right = Math.max(right, pageWords[w].bounds.right);
				top = Math.min(top, pageWords[w].bounds.top);
				bottom = Math.max(bottom, pageWords[w].bounds.bottom);
			}
		
		//	any words contained at all?
		if ((right <= left) || (bottom <= top))
			return null;
		
		//	did we change anything?
		BoundingBox regionWordBounds = new BoundingBox(left, right, top, bottom);
		if (regionWordBounds.equals(region.bounds))
			return region;
		
		//	shrink original region
		ImRegion sRegion = new ImRegion(region.getPage(), regionWordBounds, region.getType());
		sRegion.copyAttributes(region);
		return sRegion;
	}
	
	private ImRegion shrinkToContent(ImRegion imageOrGraphics, AnalysisImage pai) {
		
		//	get brightness
		byte[][] paiBrightness = pai.getBrightness();
		
		//	compute aggregate bounds of (fuzzy) included words
		int left = imageOrGraphics.bounds.left;
		int right = imageOrGraphics.bounds.right;
		int top = imageOrGraphics.bounds.top;
		int bottom = imageOrGraphics.bounds.bottom;
		for (boolean cut = true; cut;) {
			cut = false;
			
			//	did we collapse the whole area?
			if (right <= left)
				return null;
			if (bottom <= top)
				return null;
			
			//	check edges
			if (this.colBlank(left, top, bottom, paiBrightness)) {
				left++;
				cut = true;
			}
			if (this.colBlank((right-1), top, bottom, paiBrightness)) {
				right--;
				cut = true;
			}
			if (this.rowBlank(top, left, right, paiBrightness)) {
				top++;
				cut = true;
			}
			if (this.rowBlank((bottom-1), left, right, paiBrightness)) {
				bottom--;
				cut = true;
			}
		}
		
		//	did we collapse the whole area?
		if ((right <= left) || (bottom <= top))
			return null;
		
		//	did we change anything?
		BoundingBox imageOrGraphicsBounds = new BoundingBox(left, right, top, bottom);
		if (imageOrGraphicsBounds.equals(imageOrGraphics.bounds))
			return imageOrGraphics;
		
		//	shrink original region
		ImRegion sImageOrGraphics = new ImRegion(imageOrGraphics.getPage(), imageOrGraphicsBounds, imageOrGraphics.getType());
		sImageOrGraphics.copyAttributes(imageOrGraphics);
		return sImageOrGraphics;
	}
	
	private boolean colBlank(int col, int top, int bottom, byte[][] brightness) {
		for (int r = top; r < bottom; r++)
			//	TODO make this threshold adjustable to lower values
			if (brightness[col][r] < Byte.MAX_VALUE) {
//				System.out.println("Found brightness " + brightness[col][r] + " at " + col + "/" + r);
				return false;
			}
		return true;
	}
	
	private boolean rowBlank(int row, int left, int right, byte[][] brightness) {
		for (int c = left; c < right; c++)
			//	TODO make this threshold adjustable to lower values
			if (brightness[c][row] < Byte.MAX_VALUE) {
//				System.out.println("Found brightness " + brightness[c][row] + " at " + c + "/" + row);
				return false;
			}
		return true;
	}
	
	private BoundingBox getUnion(BoundingBox bb1, BoundingBox bb2) {
		BoundingBox[] bbs = {bb1, bb2};
		return BoundingBox.aggregate(bbs);
	}
	
	private boolean markImageOrGraphics(ImPage page, BoundingBox selectedBounds, ImRegion[] selectedRegions, String type, ImDocumentMarkupPanel idmp) {
		
		//	shrink selection
		PageImage pi = page.getImage();
		AnalysisImage ai = Imaging.wrapImage(pi.image, null);
		ImagePartRectangle ipr = Imaging.getContentBox(ai);
		ImagePartRectangle selectedIpr = ipr.getSubRectangle(selectedBounds.left, selectedBounds.right, selectedBounds.top, selectedBounds.bottom);
		selectedIpr = Imaging.narrowLeftAndRight(selectedIpr);
		selectedIpr = Imaging.narrowTopAndBottom(selectedIpr);
		
		//	anything selected at all (too much effort to check in advance)
		if ((selectedIpr.getWidth() == 0) || (selectedIpr.getHeight() == 0) || (Imaging.computeAverageBrightness(selectedIpr) > 125)) {
			DialogFactory.alert(("The selection appears to be completely empty and thus cannot be marked as " + (ImRegion.IMAGE_TYPE.equals(type) ? "an image" : "a vector based graphics") + "."), ("Cannot Mark " + (ImRegion.IMAGE_TYPE.equals(type) ? "Image" : "Graphics")), JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		//	determine bounding box
		BoundingBox iogBounds = new BoundingBox(selectedIpr.getLeftCol(), selectedIpr.getRightCol(), selectedIpr.getTopRow(), selectedIpr.getBottomRow());
		
		//	clean up nested regions
		for (int r = 0; r < selectedRegions.length; r++) {
			if (type.equals(selectedRegions[r].getType()) && !iogBounds.includes(selectedRegions[r].bounds, true))
				continue;
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
//		boolean captionInside = docLayout.getBooleanProperty("caption.insideFigure", false);
		
		//	get potential captions
		//	TODO also consider inside target captions (implement 6-argument method in ImUtils)
		ImAnnotation[] captionAnnots = ImUtils.findCaptions(iog, captionAbove, captionBelow, captionBeside,/* captionInside,*/ true);
		
		//	try setting attributes in unassigned captions first
		for (int a = 0; a < captionAnnots.length; a++) {
			if (captionAnnots[a].hasAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE) || captionAnnots[a].hasAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE))
				continue;
			captionAnnots[a].setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + iog.pageId));
			captionAnnots[a].setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, iog.bounds.toString());
			return true;
		}
		
		//	set attributes in any caption (happens if user corrects, for instance)
		if (captionAnnots.length != 0) {
			captionAnnots[0].setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + iog.pageId));
			captionAnnots[0].setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, iog.bounds.toString());
		}
		
		//	finally ...
		return true;
	}
	
	private static void copyCompositeImage(Figure[] figures, int scaleToDpi, Graphics[] graphics, ImWord[] words, ImPage page) {
		BufferedImage image = ImSupplement.getCompositeImage(figures, scaleToDpi, graphics, words, page);
		if (image != null)
			ImUtils.copy(new ImageSelection(image));
	}
	
	private static void copyGraphics(Graphics[] graphics, Figure[] figures, int scaleToDpi, ImWord[] words, ImPage page) {
		
		//	bitmap export
		if (scaleToDpi > 0)
			copyCompositeImage(figures, scaleToDpi, graphics, words, page);
		
		//	SVG export
		else {
			CharSequence svg = ImSupplement.getSvg(graphics, words, page);
			ImUtils.copy(new StringSelection(svg.toString()));
		}
	}
	
	private static class ImageSelection implements Transferable {
		private Image image;
		ImageSelection(Image image) {
			this.image = image;
		}
		public DataFlavor[] getTransferDataFlavors() {
			DataFlavor[] dfs = {DataFlavor.imageFlavor};
			return dfs;
		}
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return DataFlavor.imageFlavor.equals(flavor);
		}
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (DataFlavor.imageFlavor.equals(flavor))
				return image;
			else throw new UnsupportedFlavorException(flavor);
		}
	}
	
	private String getExportFileName(ImDocument doc, ImPage page, ImRegion exportRegion) {
		
		//	start with document name
		String docName = ((String) doc.getAttribute(DOCUMENT_NAME_ATTRIBUTE, doc.docId));
		if (docName.toLowerCase().indexOf(".pdf") != -1) // TODO observe more common file name suffixes, if only as we add more decoders ...
			docName = docName.substring(0, (docName.toLowerCase().indexOf(".pdf") + ".pdf".length()));
		
		//	find caption
		String regionBoundsStr = exportRegion.bounds.toString();
		ImAnnotation[] captions = doc.getAnnotations(ImAnnotation.CAPTION_TYPE, page.pageId);
		String captionStart = null;
		for (int c = 0; c < captions.length; c++)
			if (regionBoundsStr.equals(captions[c].getAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE))) {
				captionStart = this.getCaptionStart(captions[c]);
				break;
			}
		
		//	no caption start to work with, default to generic naming
		if (captionStart == null)
			return (docName + "." + exportRegion.getType() + "@" + page.pageId + "." + exportRegion.bounds.toString());
		
		//	use (normalized) caption start
		captionStart = captionStart.replaceAll("\\s+", ""); // eliminate spaces
		captionStart = captionStart.replaceAll("\\.", ""); // eliminate dots
		captionStart = captionStart.replaceAll("[\\-\\u00AD\\u2010-\\u2015\\u2212]+", "-"); // normalize dashes
		captionStart = captionStart.replaceAll("[^a-zA-Z0-9\\-\\_]+", "_"); // replace anything that might cause trouble
		return (docName + "." + captionStart);
	}
	
	private String getCaptionStart(ImAnnotation caption) {
		ImWord captionStartEnd = null;
		char indexType = ((char) 0);
		boolean lastWasIndex = false;
		for (ImWord imw = caption.getFirstWord().getNextWord(); imw != null; imw = imw.getNextWord()) {
			String imwString = imw.getString();
			if ((imwString == null) || (imwString.trim().length() == 0))
				continue;
			
			//	Arabic index number
			if (imwString.matches("[0-9]+(\\s*[a-z])?")) {
				if (indexType == 0)
					indexType = 'A';
				else if (indexType != 'A')
					break;
				captionStartEnd = imw;
				lastWasIndex = true;
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
			}
			
			//	lower case index letter
			else if (imwString.matches("[a-z]")) {
				if (indexType == 0)
					indexType = 'l';
				else if (indexType != 'l')
					break;
				captionStartEnd = imw;
				lastWasIndex = true;
			}
			
			//	upper case index letter
			else if (imwString.matches("[A-Z]")) {
				if (indexType == 0)
					indexType = 'L';
				else if (indexType != 'L')
					break;
				captionStartEnd = imw;
				lastWasIndex = true;
			}
			
			//	enumeration separator or range marker
			else if (imwString.equals(",") || imwString.equals("&") || (";and;und;et;y;".indexOf(";" + imwString.toLowerCase() + ";") != -1) || imwString.matches("[\\-\\u00AD\\u2010-\\u2015\\u2212]+")) {
				if (!lastWasIndex) // no enumeration or range open, we're done here
					break;
				lastWasIndex = false;
			}
			
			//	ignore dots
			else if (!".".equals(imwString))
				break;
		}
		
		//	finally ...
		return ImUtils.getString(caption.getFirstWord(), ((captionStartEnd == null) ? caption.getLastWord() : captionStartEnd), true);
	}
	
	private File chooseExportFile(final String fileName, final ExportParameterPanel paramPanel, boolean forGraphics) {
		if (this.fileChooser == null) {
			this.fileChooser = new JFileChooser();
			this.fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			this.fileChooser.setAcceptAllFileFilterUsed(false);
		}
		
		if (paramPanel == null)
			this.fileChooser.setAccessory(null);
		else {
			paramPanel.setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, paramPanel.getBackground()));
			JPanel paramPanelTray = new JPanel(new BorderLayout(), true);
			paramPanelTray.add(paramPanel, BorderLayout.SOUTH);
			this.fileChooser.setAccessory(paramPanelTray);
		}
		
		this.fileChooser.resetChoosableFileFilters();
		this.fileChooser.addChoosableFileFilter(pngFileFilter);
		if (forGraphics) {
			this.fileChooser.addChoosableFileFilter(svgFileFilter);
			this.fileChooser.setFileFilter(svgFileFilter);
			this.fileChooser.setSelectedFile(new File(this.fileChooser.getCurrentDirectory(), fileName + ".svg"));
		}
		else {
			this.fileChooser.setFileFilter(pngFileFilter);
			this.fileChooser.setSelectedFile(new File(this.fileChooser.getCurrentDirectory(), fileName + ".png"));
		}
		
		if (forGraphics && (paramPanel != null))
			paramPanel.dpiSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					int dpi = paramPanel.getDpi();
					String selFileName;
					try {
						BasicFileChooserUI fileChooserUI = (BasicFileChooserUI) fileChooser.getUI();
						selFileName = fileChooserUI.getFileName();
					}
					catch (Exception e) {
						File selFile = fileChooser.getSelectedFile();
						selFileName = ((selFile == null) ? fileName : selFile.getName());
					}
//					System.out.println("Selected file is " + selFileName);
					fileChooser.setFileFilter((dpi < 0) ? svgFileFilter : pngFileFilter);
					if (selFileName.toLowerCase().endsWith(".png") || selFileName.toLowerCase().endsWith(".svg"))
						selFileName = selFileName.substring(0, selFileName.lastIndexOf('.'));
					fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory(), (selFileName + ((dpi < 0) ? ".svg" : ".png"))));
				}
			});
		
		File exportFile = null;
		if (this.fileChooser.showSaveDialog(DialogFactory.getTopWindow()) == JFileChooser.APPROVE_OPTION)
			exportFile = this.fileChooser.getSelectedFile();
		
		this.fileChooser.setAccessory(null);
		return exportFile;
	}
	
	private JFileChooser fileChooser = null;
	
	private static FileFilter pngFileFilter = new FileFilter() {
		public String getDescription() {
			return "Portable Network Graphics (PNG)";
		}
		public boolean accept(File file) {
			return (file.isDirectory() || file.getName().toLowerCase().endsWith(".png"));
		}
	};
	
	private static FileFilter svgFileFilter = new FileFilter() {
		public String getDescription() {
			return "Scalable Vector Graphics (SVG)";
		}
		public boolean accept(File file) {
			return (file.isDirectory() || file.getName().toLowerCase().endsWith(".svg"));
		}
	};
	
	private static class ExportParameterPanel extends JPanel {
		final JComboBox dpiSelector;
		private JCheckBox exportFigures;
		private JCheckBox exportGraphics;
		private JCheckBox exportWords;
		ExportParameterPanel(Figure[] figures, Graphics[] graphics, ImWord[] words, boolean forGraphics, boolean forFileChooser) {
			super(new GridLayout(0, 1), true);
			
			if (forFileChooser)
				this.add(new JLabel(("<HTML><B>" + (forGraphics ? "Graphics" : "Image") + " Export Options</B></HTML>"), JLabel.CENTER));
			
			TreeSet figureDpiSet = new TreeSet();
			if (figures != null) {
				for (int f = 0; f < figures.length; f++)
					figureDpiSet.add(new Integer(figures[f].getDpi()));
			}
			Integer maxFigureDpi = (figureDpiSet.isEmpty() ? null : ((Integer) figureDpiSet.last()));
			TreeSet dpiSet = new TreeSet();
			if (forGraphics)
				dpiSet.add(new SelectableDPI(-1, ("Copy as SVG" + (((figures == null) || (figures.length == 0)) ? "" : " (excludes figures)"))));
			for (Iterator dpiit = figureDpiSet.iterator(); dpiit.hasNext();) {
				Integer dpi = ((Integer) dpiit.next());
				dpiSet.add(new SelectableDPI(dpi, ("render at " + dpi + " DPI")));
			}
			if (!figureDpiSet.contains(new Integer(300)))
				dpiSet.add(new SelectableDPI(300, ("render at 300 DPI")));
			if (!figureDpiSet.contains(new Integer(200)))
				dpiSet.add(new SelectableDPI(200, ("render at 200 DPI")));
			if (!figureDpiSet.contains(new Integer(150)))
				dpiSet.add(new SelectableDPI(150, ("render at 150 DPI")));
			SelectableDPI[] dpiOptions = ((SelectableDPI[]) dpiSet.toArray(new SelectableDPI[dpiSet.size()]));
			this.dpiSelector = new JComboBox(dpiOptions);
			if (maxFigureDpi == null)
				this.dpiSelector.setSelectedItem(new SelectableDPI(-1, ""));
			else if (maxFigureDpi.intValue() <= 600)
				this.dpiSelector.setSelectedItem(new SelectableDPI(maxFigureDpi.intValue(), ""));
			else this.dpiSelector.setSelectedItem(new SelectableDPI(300, ""));
			
			JPanel dpiPanel = new JPanel(new BorderLayout(), true);
			dpiPanel.add(new JLabel("Export Resolution ", JLabel.LEFT), BorderLayout.WEST);
			dpiPanel.add(this.dpiSelector, BorderLayout.CENTER);
			this.add(dpiPanel);
			
			if (forGraphics) {
				this.exportFigures = new JCheckBox("Include Bitmap Images", false);
				this.exportGraphics = null;
				if ((figures != null) && (figures.length != 0)) {
					this.add(this.exportFigures);
					this.dpiSelector.addItemListener(new ItemListener() {
						public void itemStateChanged(ItemEvent ie) {
							SelectableDPI selDpi = ((SelectableDPI) dpiSelector.getSelectedItem());
							exportFigures.setEnabled(selDpi.dpi != -1);
						}
					});
				}
			}
			else {
				this.exportFigures = null;
				this.exportGraphics = new JCheckBox("Include Line Graphics", (graphics.length != 0));
				if (graphics.length != 0)
					this.add(this.exportGraphics);
			}
			
			this.exportWords = new JCheckBox("Include Label Text", false);
			if ((words != null) && (words.length != 0))
				this.add(this.exportWords);
		}
		int getDpi() {
			return ((SelectableDPI) this.dpiSelector.getSelectedItem()).dpi;
		}
		boolean includeFigures() {
			return ((this.getDpi() > 0) && ((this.exportFigures == null) || this.exportFigures.isSelected()));
		}
		boolean includeGraphics() {
			return ((this.exportGraphics == null) || this.exportGraphics.isSelected());
		}
		boolean includeWords() {
			return ((this.exportWords == null) || this.exportWords.isSelected());
		}
	}
	
	private static class SelectableDPI implements Comparable {
		final int dpi;
		final String label;
		SelectableDPI(int dpi, String label) {
			this.dpi = dpi;
			this.label = label;
		}
		public String toString() {
			return this.label;
		}
		public boolean equals(Object obj) {
			if (obj instanceof SelectableDPI)
				return (this.compareTo(obj) == 0);
			else if (obj instanceof Number)
				return (this.dpi == ((Number) obj).intValue());
			else return false;
		}
		public int compareTo(Object obj) {
			return (this.dpi - ((SelectableDPI) obj).dpi);
		}
	}
	
	private static void saveCompositeImage(File file, Figure[] figures, int scaleToDpi, Graphics[] graphics, ImWord[] words, ImPage page) {
		try {
			BufferedImage image = ImSupplement.getCompositeImage(figures, scaleToDpi, graphics, words, page);
			OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
			ImageIO.write(image, "PNG", out);
			out.flush();
			out.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
			DialogFactory.alert(("The selected image could not be saved:\r\n" + ioe.getMessage()), "Error Saving Image Group", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private static void saveGraphics(File file, Graphics[] graphics, Figure[] figures, int scaleToDpi, ImWord[] words, ImPage page) {
		try {
			if (scaleToDpi < 1) {
				CharSequence svg = ImSupplement.getSvg(graphics, words, page);
				Reader in = new CharSequenceReader(svg);
				Writer out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(file)), "UTF-8");
				char[] buffer = new char[1024];
				for (int r; (r = in.read(buffer, 0, buffer.length)) != -1;)
					out.write(buffer, 0, r);
				in.close();
				out.flush();
				out.close();
			}
			else {
				BufferedImage image = ImSupplement.getCompositeImage(figures, scaleToDpi, graphics, words, page);
				OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
				ImageIO.write(image, "PNG", out);
				out.flush();
				out.close();
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
			DialogFactory.alert(("The selected graphics could not be saved:\r\n" + ioe.getMessage()), "Error Saving Graphics", JOptionPane.ERROR_MESSAGE);
		}
	}
	
////	private static DataFlavor svgDataFlavor = new DataFlavor("image/svg+xml;class=java.io.Reader", "Scalable Vector Graphics");
//	private static DataFlavor svgDataFlavor = new DataFlavor("text/xml;class=java.io.Reader", "Scalable Vector Graphics");
//	private static class GraphicsSelection implements Transferable {
////		private CharSequence data;
////		GraphicsSelection(CharSequence data) {
////			this.data = data;
////		}
////		private byte[] data;
////		GraphicsSelection(byte[] data) {
////			this.data = data;
////		}
//		private CharSequence data;
//		GraphicsSelection(CharSequence data) {
//			this.data = data;
//		}
//		public DataFlavor[] getTransferDataFlavors() {
//			DataFlavor[] dfs = {svgDataFlavor};
//			return dfs;
//		}
////		public DataFlavor[] getTransferDataFlavors() {
////			DataFlavor[] dfs = {DataFlavor.stringFlavor};
////			return dfs;
////		}
//		public boolean isDataFlavorSupported(DataFlavor flavor) {
//			return svgDataFlavor.equals(flavor);
//		}
////		public boolean isDataFlavorSupported(DataFlavor flavor) {
////			return DataFlavor.stringFlavor.equals(flavor);
////		}
////		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
////			if (svgDataFlavor.equals(flavor))
////				return new CharSequenceReader(this.data);
////			else throw new UnsupportedFlavorException(flavor);
////		}
////		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
////			if (svgDataFlavor.equals(flavor))
////				return new ByteArrayInputStream(this.data);
////			else throw new UnsupportedFlavorException(flavor);
////		}
//		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
//			if (DataFlavor.stringFlavor.equals(flavor))
//				return new CharSequenceReader(this.data);
//			else throw new UnsupportedFlavorException(flavor);
//		}
//	}
//	
//	//	TODOne test this stuff
//	public static void main(String[] args) throws Exception {
//		try {
//			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//		} catch (Exception e) {}
//		
//		RegionActionProvider rap = new RegionActionProvider();
//		
//		final String docName;
////		docName = "C70.1.13-18.pdf.imf"; // nice combo of multi-part figure on page 2, complete with graphics and labels
//		docName = "Zootaxa/zt04173p201.pdf.imf"; // vector graphics on page 2
////		docName = "Zootaxa/zt04173p201.pdf.imf"; // mixed image/graphics mix on page 6
//		
//		ImDocument doc = ImDocumentIO.loadDocument(new File("E:/Testdaten/PdfExtract", docName));
//		ImPage page = doc.getPage(2);
//		ImSupplement[] supplements = page.getSupplements();
//		
////		ImRegion[] imageRegs = page.getRegions(ImRegion.IMAGE_TYPE);
////		ImRegion imageReg = imageRegs[0];
////		Figure[] figures = ImSupplement.getFiguresIn(supplements, imageReg.bounds);
////		Graphics[] graphics = ImSupplement.getGraphicsIn(supplements, imageReg.bounds);
////		ImWord[] words = page.getWordsInside(imageReg.bounds);
////		copyCompositeImage(figures, figures[0].getDpi(), graphics, words, page);
////		ExportParameterPanel epp = new ExportParameterPanel(figures, graphics, words, false, true);
////		File file = rap.chooseExportFile(docName, epp, false);
////		System.out.println(file);
//		
//		
//		ImRegion[] graphicsRegs = page.getRegions(ImRegion.GRAPHICS_TYPE);
//		ImRegion graphicsReg = graphicsRegs[0];
//		Figure[] figures = ImSupplement.getFiguresIn(supplements, graphicsReg.bounds);
//		Graphics[] graphics = ImSupplement.getGraphicsIn(supplements, graphicsReg.bounds);
//		ImWord[] words = page.getWordsInside(graphicsReg.bounds);
//		copyGraphics(graphics, figures, 300, words, page);
////		ExportParameterPanel epp = new ExportParameterPanel(figures, graphics, words, true, true);
////		File file = rap.chooseExportFile(docName, epp, true);
////		System.out.println(file);
//	}
	
	/**
	 * Split a block region, marking paragraphs and lines. The argument block
	 * region has to be attached to the page it refers to, and the argument
	 * selection box has to overlap with the argument block.
	 * @param block the block to split
	 * @param splitBounds the bounding box to perform the split with
	 * @return true if the block was split, false otherwise
	 */
	public boolean splitBlock(ImRegion block, BoundingBox splitBounds) {
		if (!block.bounds.overlaps(splitBounds))
			return false;
		ImPage page = block.getPage();
		if (page == null)
			return false;
		return this.splitBlock(page, block, splitBounds);
	}
	
	/**
	 * Split a block region overlapping with a given bounding box, marking
	 * paragraphs and lines. If the argument selection box does not overlap
	 * with exactly one block, this method does nothing.
	 * @param page the page to split split a block in
	 * @param splitBounds the bounding box to perform the split with
	 * @return true if a block was split, false otherwise
	 */
	public boolean splitBlock(ImPage page, BoundingBox splitBounds) {
		ImRegion block = null;
		ImRegion[] blocks = page.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
		for (int b = 0; b < blocks.length; b++) 
			if (blocks[b].bounds.overlaps(splitBounds)) {
				if (block == null)
					block = blocks[b];
				else return false;
			}
		if (block == null)
			return false;
		return this.splitBlock(page, block, splitBounds);
	}
	
	/**
	 * Split a block region, marking paragraphs and lines. The argument
	 * selection box has to overlap with the argument block. Whether or not the
	 * argument block is attached to the argument page, the result of the split
	 * is.
	 * @param page the page to split split this block in
	 * @param block the block to split
	 * @param splitBounds the bounding box to perform the split with
	 * @return true if the block was split, false otherwise
	 */
	public boolean splitBlock(ImPage page, ImRegion block, BoundingBox selectedBounds) {
		
		//	sort block words into top, left, right, and bottom of as well as inside selection
		ImWord[] blockWords = block.getWords();
		ArrayList aboveWords = new ArrayList();
		ArrayList belowWords = new ArrayList();
		ArrayList leftWords = new ArrayList();
		ArrayList rightWords = new ArrayList();
		ArrayList selectedWords = new ArrayList();
		for (int w = 0; w < blockWords.length; w++) {
			if (blockWords[w].centerY < selectedBounds.top)
				aboveWords.add(blockWords[w]);
			else if (selectedBounds.bottom <= blockWords[w].centerY)
				belowWords.add(blockWords[w]);
			else if (blockWords[w].centerX < selectedBounds.left)
				leftWords.add(blockWords[w]);
			else if (selectedBounds.right <= blockWords[w].centerX)
				rightWords.add(blockWords[w]);
			else selectedWords.add(blockWords[w]);
		}
		
		//	anything selected, everything selected?
		if (selectedWords.isEmpty() || (selectedWords.size() == blockWords.length))
			return false;
		
		//	get existing paragraphs and lines
		ImRegion[] blockParagraphs = block.getRegions(ImRegion.PARAGRAPH_TYPE);
		ImRegion[] blockLines = block.getRegions(ImRegion.LINE_ANNOTATION_TYPE);
		
		//	if we have a plain top-bottom split, we can retain lines and word order, and likely most paragraphs
		if (leftWords.isEmpty() && rightWords.isEmpty()) {
			
			//	split up paragraphs that intersect with the selection
			for (int p = 0; p < blockParagraphs.length; p++) {
				if (!blockParagraphs[p].bounds.overlaps(selectedBounds))
					continue;
				else if (blockParagraphs[p].bounds.liesIn(selectedBounds, false))
					continue;
				ImRegion[] paragraphLines = blockParagraphs[p].getRegions(ImRegion.LINE_ANNOTATION_TYPE);
				if (sortLinesIntoParagraphs(page, paragraphLines, selectedBounds))
					page.removeRegion(blockParagraphs[p]);
			}
			
			//	split the block proper
			if (aboveWords.size() != 0) {
				ImRegion aboveBlock = new ImRegion(page, ImLayoutObject.getAggregateBox((ImWord[]) aboveWords.toArray(new ImWord[aboveWords.size()])), ImRegion.BLOCK_ANNOTATION_TYPE);
				updateBlockTextStream(aboveBlock);
			}
			ImRegion selectedBlock = new ImRegion(page, ImLayoutObject.getAggregateBox((ImWord[]) selectedWords.toArray(new ImWord[selectedWords.size()])), ImRegion.BLOCK_ANNOTATION_TYPE);
			updateBlockTextStream(selectedBlock);
			if (belowWords.size() != 0) {
				ImRegion belowBlock = new ImRegion(page, ImLayoutObject.getAggregateBox((ImWord[]) belowWords.toArray(new ImWord[belowWords.size()])), ImRegion.BLOCK_ANNOTATION_TYPE);
				updateBlockTextStream(belowBlock);
			}
			page.removeRegion(block);
			
			//	indicate we've done something
			return true;
		}
		
		//	remove old lines and paragraphs
		for (int p = 0; p < blockParagraphs.length; p++)
			page.removeRegion(blockParagraphs[p]);
		for (int l = 0; l < blockLines.length; l++)
			page.removeRegion(blockLines[l]);
		
		//	get last external predecessor and first external successor
		Arrays.sort(blockWords, ImUtils.textStreamOrder);
		ImWord blockPrevWord = blockWords[0].getPreviousWord();
		ImWord blockNextWord = blockWords[blockWords.length-1].getNextWord();
		
		//	cut out block words to avoid conflicts while re-chaining
		if (blockPrevWord != null)
			blockPrevWord.setNextWord(null);
		if (blockNextWord != null)
			blockNextWord.setPreviousWord(null);
		
		//	collect block words in order of chaining
		ArrayList blockWordLists = new ArrayList();
		
		//	if we have a plain left-right split, chain blocks left to right
		if (aboveWords.isEmpty() && belowWords.isEmpty()) {
			if (leftWords.size() != 0)
				blockWordLists.add(this.markBlock(page, leftWords));
			if (selectedWords.size() != 0)
				blockWordLists.add(this.markBlock(page, selectedWords));
			if (rightWords.size() != 0)
				blockWordLists.add(this.markBlock(page, rightWords));
		}
		
		//	selection cut out on top left corner
		else if (aboveWords.isEmpty() && leftWords.isEmpty()) {
			if (selectedWords.size() != 0)
				blockWordLists.add(this.markBlock(page, selectedWords));
			if (rightWords.size() != 0)
				blockWordLists.add(this.markBlock(page, rightWords));
			if (belowWords.size() != 0)
				blockWordLists.add(this.markBlock(page, belowWords));
		}
		
		//	selection cut out in some other place
		else {
			if (aboveWords.size() != 0)
				blockWordLists.add(this.markBlock(page, aboveWords));
			if (leftWords.size() != 0)
				blockWordLists.add(this.markBlock(page, leftWords));
			if (rightWords.size() != 0)
				blockWordLists.add(this.markBlock(page, rightWords));
			if (belowWords.size() != 0)
				blockWordLists.add(this.markBlock(page, belowWords));
			if (selectedWords.size() != 0)
				blockWordLists.add(this.markBlock(page, selectedWords));
		}
		
		//	chain block text streams (only if text stream types match, however)
		for (int b = 0; b < blockWordLists.size(); b++) {
			ImWord[] imws = ((ImWord[]) blockWordLists.get(b));
			if (imws.length == 0)
				continue;
			if ((blockPrevWord != null) && !blockPrevWord.getTextStreamType().endsWith(imws[0].getTextStreamType()))
				continue;
			if (blockPrevWord != null)
				blockPrevWord.setNextWord(imws[0]);
			blockPrevWord = imws[imws.length - 1];
		}
		if ((blockPrevWord != null) && (blockNextWord != null))
			blockPrevWord.setNextWord(blockNextWord);
		
		//	remove the split block
		page.removeRegion(block);
		
		//	indicate we've done something
		return true;
	}
	
	private ImWord[] markBlock(ImPage page, ArrayList wordList) {
		ImWord[] words = ((ImWord[]) wordList.toArray(new ImWord[wordList.size()]));
		if (words.length == 0)
			return words;
		
		//	mark lines
		ImUtils.makeStream(words, words[0].getTextStreamType(), null);
		sortIntoLines(page, words);
		
		//	mark block proper
		ImRegion block = new ImRegion(page, ImLayoutObject.getAggregateBox(words), ImRegion.BLOCK_ANNOTATION_TYPE);
		
		//	re-detect paragraphs
		PageAnalysis.splitIntoParagraphs(block, page.getImageDPI(), null);
		
		//	finally ...
		Arrays.sort(words, ImUtils.textStreamOrder);
		return words;
	}
	
	private void sortIntoLines(ImPage page, ImWord[] words) {
		
		//	order text stream
		ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
		Arrays.sort(words, ImUtils.textStreamOrder);
		
		//	re-detect lines
		this.markLines(page, words, null);
	}
	
	/**
	 * Mark a block region, including paragraphs and lines. This method also
	 * orders the text stream. If the argument bounding box does not contain
	 * any words, this method returns null.
	 * @param page the page the block belongs to
	 * @param blockBounds the bounding box of the block to create
	 * @return the newly marked block
	 */
	public ImRegion markBlock(ImPage page, BoundingBox blockBounds) {
		ImWord[] words = page.getWordsInside(blockBounds);
		if (words.length == 0)
			return null;
		
		//	mark lines
		ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
		Arrays.sort(words, ImUtils.textStreamOrder);
		this.markLines(page, words, null);
		
		//	mark block proper
		ImRegion block = new ImRegion(page, ImLayoutObject.getAggregateBox(words), ImRegion.BLOCK_ANNOTATION_TYPE);
		
		//	re-detect paragraphs
		PageAnalysis.splitIntoParagraphs(block, page.getImageDPI(), null);
		
		//	finally ...
		return block;
	}
	
	private void markLines(ImPage page, ImWord[] words, Map existingLines) {
		int lineStartWordIndex = 0;
		for (int w = 1; w <= words.length; w++)
			
			//	end line at downward or leftward jump, and at last word
			if ((w == words.length) || (words[lineStartWordIndex].bounds.bottom <= words[w].bounds.top) || (words[w].centerX < words[w-1].centerX)) {
				BoundingBox lBounds = ImLayoutObject.getAggregateBox(words, lineStartWordIndex, w);
				if (existingLines == null)
					new ImRegion(page, lBounds, ImRegion.LINE_ANNOTATION_TYPE);
				else {
					ImRegion bLine = ((ImRegion) existingLines.remove(lBounds));
					if (bLine == null)
						bLine = new ImRegion(page, lBounds, ImRegion.LINE_ANNOTATION_TYPE);
				}
				lineStartWordIndex = w;
			}
		
		//	remove now-spurious lines (if any)
		if (existingLines != null)
			for (Iterator lbit = existingLines.keySet().iterator(); lbit.hasNext();)
				page.removeRegion((ImRegion) existingLines.get(lbit.next()));
	}
	
	private void updateBlockTextStream(ImRegion block) {
		ImRegion[] blockParagraphs = block.getRegions(ImRegion.PARAGRAPH_TYPE);
		for (int p = 0; p < blockParagraphs.length; p++) {
			ImWord[] paragraphWords = blockParagraphs[p].getWords();
			Arrays.sort(paragraphWords, ImUtils.textStreamOrder);
			for (int w = 0; w < paragraphWords.length; w++) {
				if ((w+1) == paragraphWords.length) {
					if (paragraphWords[w].getNextRelation() != ImWord.NEXT_RELATION_PARAGRAPH_END)
						paragraphWords[w].setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
				}
				else if (paragraphWords[w].getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
					paragraphWords[w].setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
			}
		}
	}
	
	private boolean sortLinesIntoParagraphs(ImPage page, ImRegion[] lines, BoundingBox selectedBounds) {
		Arrays.sort(lines, ImUtils.topDownOrder);
		
		//	find boundaries between above, inside, and below selection
		int firstSelectedLineIndex = 0;
		int firstNonSelectedLineIndex = lines.length;
		for (int l = 0; l < lines.length; l++) {
			int lineCenterY = ((lines[l].bounds.top + lines[l].bounds.bottom) / 2);
			if (lineCenterY < selectedBounds.top)
				firstSelectedLineIndex = (l+1);
			if (lineCenterY < selectedBounds.bottom)
				firstNonSelectedLineIndex = (l+1);
		}
		
		//	anything to work with?
		if (firstSelectedLineIndex == firstNonSelectedLineIndex)
			return false;
		
		//	mark parts, and update word relations
		if (firstSelectedLineIndex > 0) {
			ImRegion aboveSelectionParagraph = new ImRegion(page, ImLayoutObject.getAggregateBox(lines, 0, firstSelectedLineIndex), ImRegion.PARAGRAPH_TYPE);
			ImWord[] aboveSelectionParagraphWords = aboveSelectionParagraph.getWords();
			if (aboveSelectionParagraphWords.length != 0) {
				Arrays.sort(aboveSelectionParagraphWords, ImUtils.textStreamOrder);
				aboveSelectionParagraphWords[aboveSelectionParagraphWords.length-1].setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
			}
		}
		ImRegion selectionParagraph = new ImRegion(page, ImLayoutObject.getAggregateBox(lines, firstSelectedLineIndex, firstNonSelectedLineIndex), ImRegion.PARAGRAPH_TYPE);
		if (firstNonSelectedLineIndex < lines.length) {
			ImWord[] selectionParagraphWords = selectionParagraph.getWords();
			if (selectionParagraphWords.length != 0) {
				Arrays.sort(selectionParagraphWords, ImUtils.textStreamOrder);
				selectionParagraphWords[selectionParagraphWords.length-1].setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
			}
			new ImRegion(page, ImLayoutObject.getAggregateBox(lines, firstNonSelectedLineIndex, lines.length), ImRegion.PARAGRAPH_TYPE);
		}
		return true;
	}
	
	private void ensureBlockStructure(ImRegion block) {
		
		//	get words
		ImWord[] bWords = block.getWords();
		if (bWords.length == 0)
			return;
		
		//	check if all (non-artifact) words inside lines
		ImRegion[] bLines = block.getRegions(ImRegion.LINE_ANNOTATION_TYPE); 
		int lWordCount = 0;
		for (int l = 0; l < bLines.length; l++) {
			ImWord[] lWords = bLines[l].getWords();
			lWordCount += lWords.length;
		}
		
		//	check if all (non-artifact) words inside paragraphs
		ImRegion[] bParagraphs = block.getRegions(ImRegion.PARAGRAPH_TYPE); 
		int pWordCount = 0;
		for (int p = 0; p < bParagraphs.length; p++) {
			ImWord[] pWords = bParagraphs[p].getWords();
			pWordCount += pWords.length;
		}
		
		//	all words nested properly, we're done here
		if ((bWords.length <= lWordCount) && (bWords.length <= pWordCount))
			return;
		
		//	get page
		ImPage page = block.getPage();
		
		//	repair line nesting
		if (lWordCount < bWords.length) {
			
			//	index lines by bounding boxes
			HashMap exLines = new HashMap();
			for (int l = 0; l < bLines.length; l++)
				exLines.put(bLines[l].bounds, bLines[l]);
			
			//	order words and mark lines
			ImUtils.sortLeftRightTopDown(bWords);
			int lStartIndex = 0;
			for (int w = 1; w <= bWords.length; w++)
				
				//	end line at downward or leftward jump, and at last word
				if ((w == bWords.length) || (bWords[lStartIndex].bounds.bottom <= bWords[w].bounds.top) || (bWords[w].centerX < bWords[w-1].centerX)) {
					BoundingBox lBounds = ImLayoutObject.getAggregateBox(bWords, lStartIndex, w);
					ImRegion bLine = ((ImRegion) exLines.remove(lBounds));
					if (bLine == null)
						bLine = new ImRegion(page, lBounds, ImRegion.LINE_ANNOTATION_TYPE);
					lStartIndex = w;
				}
			
			//	remove now-spurious lines
			for (Iterator lbit = exLines.keySet().iterator(); lbit.hasNext();)
				page.removeRegion((ImRegion) exLines.get(lbit.next()));
			
			//	re-get lines
			bLines = block.getRegions(ImRegion.LINE_ANNOTATION_TYPE);
		}
		
		//	repair paragraph nesting
		if (pWordCount < bWords.length) {
			
			//	clean up existing paragraphs
			for (int p = 0; p < bParagraphs.length; p++)
				page.removeRegion(bParagraphs[p]);
			
			//	re-mark paragraphs
			PageAnalysis.splitIntoParagraphs(block, page.getImageDPI(), ProgressMonitor.dummy);
		}
	}
	
	private boolean restructureBlock(ImPage page, ImRegion block, ImRegion[] blockParagraphs, ImRegion[] blockLines, int dpi) {
		
		//	make sure block lines come top-down
		Arrays.sort(blockLines, ImUtils.topDownOrder);
		
		//	index paragraphs start lines
		HashSet paragraphStartLineBounds = new HashSet();
		for (int p = 0; p < blockParagraphs.length; p++) {
			ImRegion[] paragraphLines = blockParagraphs[p].getRegions(ImRegion.LINE_ANNOTATION_TYPE);
			if (paragraphLines.length == 0)
				continue;
			Arrays.sort(paragraphLines, ImUtils.topDownOrder);
			paragraphStartLineBounds.add(paragraphLines[0].bounds);
		}
		
		//	assess line starts
		int pslLeftDistSum = 0;
		int leftDistSum = 0;
		boolean[] isIndentedLine = new boolean[blockLines.length];
		boolean[] isShortLine = new boolean[blockLines.length];
		for (int l = 0; l < blockLines.length; l++) {
			int leftDist = (blockLines[l].bounds.left - block.bounds.left);
			if (paragraphStartLineBounds.contains(blockLines[l].bounds))
				pslLeftDistSum += leftDist;
			leftDistSum += leftDist;
			isIndentedLine[l] = ((dpi / 12) /* about 2mm */ < leftDist); // at least 2mm shy of left block edge
			int rightDist = (block.bounds.right - blockLines[l].bounds.right);
			isShortLine[l] = ((block.bounds.right - block.bounds.left) < (rightDist * 20)); // at least 5% shy of right block edge
		}
		int avgLeftDist = (leftDistSum / blockLines.length);
		int avgPslLeftDist = ((paragraphStartLineBounds.isEmpty()) ? avgLeftDist : (pslLeftDistSum / paragraphStartLineBounds.size()));
		
		//	assemble split option panel
		final JRadioButton pslIndent = new JRadioButton("indented", ((avgLeftDist + (dpi / 12)) < avgPslLeftDist)); // at least 2mm difference
		final JRadioButton pslOutdent = new JRadioButton("outdented", ((avgPslLeftDist + (dpi / 12)) < avgLeftDist)); // at least 2mm difference
		final JRadioButton pslFlush = new JRadioButton("neither", (!pslIndent.isSelected() && !pslOutdent.isSelected()));
		ButtonGroup pslButtonGroup = new ButtonGroup();
		pslButtonGroup.add(pslIndent);
		pslButtonGroup.add(pslOutdent);
		pslButtonGroup.add(pslFlush);
		JPanel pslButtonPanel = new JPanel(new GridLayout(1, 0), true);
		pslButtonPanel.add(pslIndent);
		pslButtonPanel.add(pslOutdent);
		pslButtonPanel.add(pslFlush);
		JPanel pslPanel = new JPanel(new BorderLayout(), true);
		pslPanel.add(new JLabel("Paragraph start lines are "), BorderLayout.WEST);
		pslPanel.add(pslButtonPanel, BorderLayout.CENTER);
		
		final JCheckBox ilhBold = new JCheckBox("bold", false); // at least 2mm difference
		final JCheckBox ilhItalics = new JCheckBox("italics", false); // at least 2mm difference
		final JCheckBox ilhAllCaps = new JCheckBox("all-caps", false);
		JPanel ilhButtonPanel = new JPanel(new GridLayout(1, 0), true);
		ilhButtonPanel.add(ilhBold);
		ilhButtonPanel.add(ilhItalics);
		ilhButtonPanel.add(ilhAllCaps);
		JPanel ilhPanel = new JPanel(new BorderLayout(), true);
		ilhPanel.add(new JLabel("Split before in-line headings in "), BorderLayout.WEST);
		ilhPanel.add(ilhButtonPanel, BorderLayout.CENTER);
		
		final JCheckBox shortLineEndsParagraph = new JCheckBox("Lines short of right block edge end paragraphs", true);
		final JCheckBox singleLineParagraphs = new JCheckBox("Make each line a separate paragraph", false);
		singleLineParagraphs.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ie) {
				pslIndent.setEnabled(!singleLineParagraphs.isSelected());
				pslOutdent.setEnabled(!singleLineParagraphs.isSelected());
				pslFlush.setEnabled(!singleLineParagraphs.isSelected());
				shortLineEndsParagraph.setEnabled(!singleLineParagraphs.isSelected());
				ilhBold.setEnabled(!singleLineParagraphs.isSelected());
				ilhItalics.setEnabled(!singleLineParagraphs.isSelected());
				ilhAllCaps.setEnabled(!singleLineParagraphs.isSelected());
			}
		});
		
		JPanel blockSplitOptionPanel = new JPanel(new GridLayout(0, 1), true);
		blockSplitOptionPanel.add(pslPanel);
		blockSplitOptionPanel.add(shortLineEndsParagraph);
		blockSplitOptionPanel.add(ilhPanel);
		blockSplitOptionPanel.add(singleLineParagraphs);
		
		//	prompt user
		if (DialogFactory.confirm(blockSplitOptionPanel, "Select Block Splitting Options", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION)
			return false;
		
		//	get start line position
		char paragraphStartLinePos = 'N';
		if (pslIndent.isSelected())
			paragraphStartLinePos = 'I';
		if (pslOutdent.isSelected())
			paragraphStartLinePos = 'O';
		
		//	get in-line heading style
		String inLineHeadingStyle = "";
		if (ilhBold.isSelected())
			inLineHeadingStyle += "B";
		if (ilhItalics.isSelected())
			inLineHeadingStyle += "I";
		if (ilhAllCaps.isSelected())
			inLineHeadingStyle += "C";
		
		//	loop through to multi-access signature
		return this.restructureBlock(page, block, blockParagraphs, blockLines, isIndentedLine, isShortLine, singleLineParagraphs.isSelected(), paragraphStartLinePos, shortLineEndsParagraph.isSelected(), inLineHeadingStyle);
//		
//		//	index paragraphs
//		HashMap paragraphsByBounds = new HashMap();
//		for (int p = 0; p < blockParagraphs.length; p++)
//			paragraphsByBounds.put(blockParagraphs[p].bounds, blockParagraphs[p]);
//		
//		//	keep track of logical paragraphs at block boundaries
//		boolean firstParagraphHasStart = (singleLineParagraphs.isSelected() || (isIndentedLine[0] && pslIndent.isSelected()) || (!isIndentedLine[0] && pslOutdent.isSelected()));
//		boolean lastParagraphHasEnd = (singleLineParagraphs.isSelected() || (isShortLine[blockLines.length-1] && shortLineEndsParagraph.isSelected()));
//		
//		//	do the splitting
//		int paragraphStartLineIndex = 0;
//		for (int l = 0; l < blockLines.length; l++) {
//			
//			//	assess whether or not to split after current line
//			boolean lineEndsParagraph = false;
//			if (singleLineParagraphs.isSelected()) // just split after each line
//				lineEndsParagraph = true;
//			else if (shortLineEndsParagraph.isSelected() && isShortLine[l]) // split after short line
//				lineEndsParagraph = true;
//			else if (((l+1) < blockLines.length) && isIndentedLine[l+1] && pslIndent.isSelected()) // split before indented line
//				lineEndsParagraph = true;
//			else if (((l+1) < blockLines.length) && !isIndentedLine[l+1] && pslOutdent.isSelected()) // split before outdented line
//				lineEndsParagraph = true;
//			
//			//	perform split
//			if (lineEndsParagraph) {
//				BoundingBox paragraphBounds = ImLayoutObject.getAggregateBox(blockLines, paragraphStartLineIndex, (l+1));
//				ImRegion paragraph = ((ImRegion) paragraphsByBounds.remove(paragraphBounds));
//				if (paragraph == null)
//					paragraph = new ImRegion(page, paragraphBounds, ImRegion.PARAGRAPH_TYPE);
//				paragraphStartLineIndex = (l+1);
//			}
//		}
//		if (paragraphStartLineIndex < blockLines.length) {
//			BoundingBox paragraphBounds = ImLayoutObject.getAggregateBox(blockLines, paragraphStartLineIndex, blockLines.length);
//			ImRegion paragraph = ((ImRegion) paragraphsByBounds.remove(paragraphBounds));
//			if (paragraph == null)
//				paragraph = new ImRegion(page, paragraphBounds, ImRegion.PARAGRAPH_TYPE);
//		}
//		
//		//	clean up now-spurious paragraphs
//		for (Iterator pbit = paragraphsByBounds.keySet().iterator(); pbit.hasNext();)
//			page.removeRegion((ImRegion) paragraphsByBounds.get(pbit.next()));
//		
//		//	update word relations
//		blockParagraphs = block.getRegions(ImRegion.PARAGRAPH_TYPE);
//		for (int p = 0; p < blockParagraphs.length; p++) {
//			ImWord[] paragraphWords = blockParagraphs[p].getWords();
//			if (paragraphWords.length == 0)
//				continue;
//			Arrays.sort(paragraphWords, ImUtils.textStreamOrder);
//			for (int w = 0; w < (paragraphWords.length-1); w++) {
//				if (paragraphWords[w].getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
//					paragraphWords[w].setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
//			}
//			if (((p != 0) || firstParagraphHasStart) && (paragraphWords[0].getPreviousWord() != null))
//				paragraphWords[0].getPreviousWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
//			if (((p+1) != blockParagraphs.length) || lastParagraphHasEnd)
//				paragraphWords[paragraphWords.length-1].setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
//		}
//		
//		//	finally ...
//		return true;
	}
	
	/**
	 * Restructure the paragraphs in a block. If single line paragraphs are
	 * specified, each line in the argument block becomes a paragraph of its
	 * own. Otherwise, the block is split up at short lines if specified, and
	 * at indented ('I') lines or outdented ('O') lines, depending on the other
	 * arguments.
	 * @param block the block whose lines to group into paragraphs
	 * @param singleLineParagraphs make each line a separate paragraph?
	 * @param paragraphStartLinePos use 'I' to indicate indented paragraph
	 *            start lines, 'O' to indicate outdented ones, and any other
	 *            char to disable indent/outdent based splitting
	 * @param shortLineEndsParagraph end paragraphs at short lines?
	 * @return true if the argument block was modified
	 */
	public boolean restructureBlock(ImRegion block, boolean singleLineParagraphs, char paragraphStartLinePos, boolean shortLineEndsParagraph) {
		
		//	get page
		ImPage page = block.getPage();
		if (page == null)
			return false;
		
		//	get block lines
		ImRegion[] blockLines = block.getRegions(ImRegion.LINE_ANNOTATION_TYPE);
		Arrays.sort(blockLines, ImUtils.topDownOrder);
		
		//	assess line properties
		int dpi = page.getImageDPI();
		boolean[] isIndentedLine = new boolean[blockLines.length];
		boolean[] isShortLine = new boolean[blockLines.length];
		for (int l = 0; l < blockLines.length; l++) {
			int leftDist = (blockLines[l].bounds.left - block.bounds.left);
			isIndentedLine[l] = ((dpi / 12) /* about 2mm */ < leftDist); // at least 2mm shy of left block edge
			int rightDist = (block.bounds.right - blockLines[l].bounds.right);
			isShortLine[l] = ((block.bounds.right - block.bounds.left) < (rightDist * 20)); // at least 5% shy of right block edge
		}
		
		//	do restructuring
		return this.restructureBlock(block, blockLines, isIndentedLine, isShortLine, singleLineParagraphs, paragraphStartLinePos, shortLineEndsParagraph);
	}
	
	/**
	 * Restructure the paragraphs in a block. If single line paragraphs are
	 * specified, each line in the argument block becomes a paragraph of its
	 * own. Otherwise, the block is split up at short lines if specified, and
	 * at indented ('I') lines or outdented ('O') lines, depending on the other
	 * arguments. The two boolean arrays are expected to have the same length
	 * as the array holding the lines. Further, the lines are expected to be
	 * sorted top-down (this method cannot sort without potentially losing the
	 * relation with the boolean arrays).
	 * @param block the block whose lines to group into paragraphs
	 * @param blockLines the lines of the argument block
	 * @param isIndentedLine an array of booleans indicating for each line
	 *            whether or not it starts flush left in the block
	 * @param isShortLine an array of booleans indicating for each line
	 *            whether or not it ends flush right in the block
	 * @param singleLineParagraphs make each line a separate paragraph?
	 * @param paragraphStartLinePos use 'I' to indicate indented paragraph
	 *            start lines, 'O' to indicate outdented ones, and any other
	 *            char to disable indent/outdent based splitting
	 * @param shortLineEndsParagraph end paragraphs at short lines?
	 * @return true if the argument block was modified
	 */
	public boolean restructureBlock(ImRegion block, ImRegion[] blockLines, boolean[] isIndentedLine, boolean[] isShortLine, boolean singleLineParagraphs, char paragraphStartLinePos, boolean shortLineEndsParagraph) {
		
		//	get page
		ImPage page = block.getPage();
		if (page == null)
			return false;
		
		//	get block paragraphs
		ImRegion[] blockParagraphs = block.getRegions(ImRegion.PARAGRAPH_TYPE);
		
		//	do restructuring
		return this.restructureBlock(page, block, blockParagraphs, blockLines, isIndentedLine, isShortLine, singleLineParagraphs, paragraphStartLinePos, shortLineEndsParagraph, "");
	}
	
	private boolean restructureBlock(ImPage page, ImRegion block, ImRegion[] blockParagraphs, ImRegion[] blockLines, boolean[] isIndentedLine, boolean[] isShortLine, boolean singleLineParagraphs, char paragraphStartLinePos, boolean shortLineEndsParagraph, String inLineHeadingStyle) {
		
		//	index paragraphs
		HashMap paragraphsByBounds = new HashMap();
		for (int p = 0; p < blockParagraphs.length; p++)
			paragraphsByBounds.put(blockParagraphs[p].bounds, blockParagraphs[p]);
		
		//	keep track of logical paragraphs at block boundaries
		boolean firstParagraphHasStart = (singleLineParagraphs || (isIndentedLine[0] && (paragraphStartLinePos == 'I')) || (!isIndentedLine[0] && (paragraphStartLinePos == 'O')));
		boolean lastParagraphHasEnd = (singleLineParagraphs || (isShortLine[blockLines.length-1] && shortLineEndsParagraph));
		
		//	do the splitting
		int paragraphStartLineIndex = 0;
		for (int l = 0; l < blockLines.length; l++) {
			
			//	assess whether or not to split after current line
			boolean lineEndsParagraph = false;
			if (singleLineParagraphs) // just split after each line
				lineEndsParagraph = true;
			else if (shortLineEndsParagraph && isShortLine[l]) // split after short line
				lineEndsParagraph = true;
			else if (((l+1) < blockLines.length) && isIndentedLine[l+1] && (paragraphStartLinePos == 'I')) // split before indented line
				lineEndsParagraph = true;
			else if (((l+1) < blockLines.length) && !isIndentedLine[l+1] && (paragraphStartLinePos == 'O')) // split before outdented line
				lineEndsParagraph = true;
			else if (((l+1) < blockLines.length) && (inLineHeadingStyle.length() != 0)) {
				ImWord[] nextLineWords = blockLines[l+1].getWords();
				if (nextLineWords.length != 0) {
					Arrays.sort(nextLineWords, ImUtils.leftRightOrder);
					ImWord nextLineStart = nextLineWords[0];
					if ((nextLineStart != null) && (inLineHeadingStyle.indexOf('B') != -1) && !nextLineStart.hasAttribute(ImWord.BOLD_ATTRIBUTE))
						nextLineStart = null;
					if ((nextLineStart != null) && (inLineHeadingStyle.indexOf('I') != -1) && !nextLineStart.hasAttribute(ImWord.ITALICS_ATTRIBUTE))
						nextLineStart = null;
					if ((nextLineStart != null) && (inLineHeadingStyle.indexOf('C') != -1) && ((nextLineStart.getString().length() < 3) || !nextLineStart.getString().equals(nextLineStart.getString().toUpperCase())))
						nextLineStart = null;
					if (nextLineStart != null)
						lineEndsParagraph = true;
				}
			}
			
			//	perform split
			if (lineEndsParagraph) {
				BoundingBox paragraphBounds = ImLayoutObject.getAggregateBox(blockLines, paragraphStartLineIndex, (l+1));
				ImRegion paragraph = ((ImRegion) paragraphsByBounds.remove(paragraphBounds));
				if (paragraph == null)
					paragraph = new ImRegion(page, paragraphBounds, ImRegion.PARAGRAPH_TYPE);
				paragraphStartLineIndex = (l+1);
			}
		}
		if (paragraphStartLineIndex < blockLines.length) {
			BoundingBox paragraphBounds = ImLayoutObject.getAggregateBox(blockLines, paragraphStartLineIndex, blockLines.length);
			ImRegion paragraph = ((ImRegion) paragraphsByBounds.remove(paragraphBounds));
			if (paragraph == null)
				paragraph = new ImRegion(page, paragraphBounds, ImRegion.PARAGRAPH_TYPE);
		}
		
		//	clean up now-spurious paragraphs
		for (Iterator pbit = paragraphsByBounds.keySet().iterator(); pbit.hasNext();)
			page.removeRegion((ImRegion) paragraphsByBounds.get(pbit.next()));
		
		//	update word relations
		blockParagraphs = block.getRegions(ImRegion.PARAGRAPH_TYPE);
		for (int p = 0; p < blockParagraphs.length; p++) {
			ImWord[] paragraphWords = blockParagraphs[p].getWords();
			if (paragraphWords.length == 0)
				continue;
			Arrays.sort(paragraphWords, ImUtils.textStreamOrder);
			for (int w = 0; w < (paragraphWords.length-1); w++) {
				if (paragraphWords[w].getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
					paragraphWords[w].setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
			}
			if (((p != 0) || firstParagraphHasStart) && (paragraphWords[0].getPreviousWord() != null))
				paragraphWords[0].getPreviousWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
			if (((p+1) != blockParagraphs.length) || lastParagraphHasEnd)
				paragraphWords[paragraphWords.length-1].setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
		}
		
		//	finally ...
		return true;
	}
}
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;

import de.uka.ipd.idaho.gamta.AttributeUtils;
import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.gamta.util.DocumentStyle.PropertiesData;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImLayoutObject;
import de.uka.ipd.idaho.im.ImObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.analysis.Imaging;
import de.uka.ipd.idaho.im.analysis.Imaging.AnalysisImage;
import de.uka.ipd.idaho.im.analysis.PageAnalysis;
import de.uka.ipd.idaho.im.analysis.PageAnalysis.BlockLayout;
import de.uka.ipd.idaho.im.analysis.PageAnalysis.BlockMetrics;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImDocumentStyle;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.im.util.ImUtils.StringPair;

/**
 * This class provides basic actions for handling regions.
 * 
 * @author sautter
 */
public class RegionActionProvider extends AbstractSelectionActionProvider implements LiteratureConstants, ImageMarkupToolProvider, ReactionProvider {
	private static final String REGION_CONVERTER_IMT_NAME = "ConvertRegions";
	private static final String REGION_RETYPER_IMT_NAME = "RetypeRegions";
	private static final String REGION_REMOVER_IMT_NAME = "RemoveRegions";
	private static final String REGION_DUPLICATE_REMOVER_IMT_NAME = "RemoveDuplicateRegions";
	
	private ImageMarkupTool regionConverter = new RegionConverter();
	private ImageMarkupTool regionRetyper = new RegionRetyper();
	private ImageMarkupTool regionRemover = new RegionRemover();
	private ImageMarkupTool regionDuplicateRemover = new RegionDuplicateRemover();
	
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
		String[] emins = {REGION_CONVERTER_IMT_NAME, REGION_RETYPER_IMT_NAME, REGION_REMOVER_IMT_NAME, REGION_DUPLICATE_REMOVER_IMT_NAME};
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
		else if (REGION_DUPLICATE_REMOVER_IMT_NAME.equals(name))
			return this.regionDuplicateRemover;
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
	
	private class RegionDuplicateRemover implements ImageMarkupTool {
		public String getLabel() {
			return "Remove Duplicate Regions";
		}
		public String getTooltip() {
			return "Remove duplicate regions from document";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	get pages
			ImPage[] pages = doc.getPages();
			
			//	process pages
			for (int p = 0; p < pages.length; p++) {
				
				//	get regions
				ImRegion[] pageRegions = pages[p].getRegions();
				if (pageRegions.length < 2)
					continue;
				
				//	detect and merge duplicates
				HashMap regionsByKey = new HashMap();
				for (int r = 0; r < pageRegions.length; r++) {
					String regionKey = (pageRegions[r].getType() + "@" + pageRegions[r].bounds.toString());
					
					//	get previously indexed duplicate
					ImRegion dupPageRegion = ((ImRegion) regionsByKey.get(regionKey));
					if (dupPageRegion == null) {
						regionsByKey.put(regionKey, pageRegions[r]);
						continue;
					}
					
					//	merge attributes
					AttributeUtils.copyAttributes(pageRegions[r], dupPageRegion, AttributeUtils.ADD_ATTRIBUTE_COPY_MODE);
					
					//	clean up duplicate
					pages[p].removeRegion(pageRegions[r]);
				}
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
	
	static ImDocumentStyle getBlockStyle(ImDocument doc) {
		ImDocumentStyle docStyle = ImDocumentStyle.getStyleFor(doc);
		if (docStyle == null)
			docStyle = new ImDocumentStyle(new PropertiesData(new Properties()));
		return docStyle.getImSubset("layout").getImSubset("block");
	}
	
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
			actions.add(new SelectionAction("markRegion", "Mark Region", "Mark a region from the selection") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					String regionType = ImUtils.promptForObjectType("Enter Region Type", "Enter or select type of region to create", regionTypes, null, true);
					if (regionType == null)
						return false;
					ImRegion region = new ImRegion(page, selectedBounds, regionType);
					if (ImRegion.BLOCK_ANNOTATION_TYPE.equals(regionType))
						ensureBlockStructure(region);
					else if (ImRegion.COLUMN_ANNOTATION_TYPE.equals(regionType))
						ensureColumnStructure(region);
					idmp.setRegionsPainted(regionType, true);
					return true;
				}
			});
		else actions.add(new SelectionAction("markRegion", "Mark Region", "Mark a region from the selection") {
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
							else if (ImRegion.COLUMN_ANNOTATION_TYPE.equals(regionType))
								ensureColumnStructure(region);
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
							else if (ImRegion.COLUMN_ANNOTATION_TYPE.equals(regionType))
								ensureColumnStructure(region);
							invoker.endAtomicAction();
							invoker.setRegionsPainted(regionType, true);
							invoker.validate();
							invoker.repaint();
						}
					});
					Color regionTypeColor = idmp.getLayoutObjectColor(regionType);
					if (regionTypeColor != null) {
						mi.setOpaque(true);
						mi.setBackground(regionTypeColor);
					}
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
					protected boolean isAtomicAction() {
						return false;
					}
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						idmp.editAttributes(page, PAGE_TYPE, "");
						return true;
					}
				});
				if (!idmp.documentBornDigital)
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
		
		//	single region selected
		if (selectedRegions.length == 1) {
			
			//	edit attributes of existing region
			actions.add(new SelectionAction("editAttributesRegion", ("Edit " + selectedRegions[0].getType() + " Attributes"), ("Edit attributes of '" + selectedRegions[0].getType() + "' region.")) {
				protected boolean isAtomicAction() {
					return false;
				}
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					idmp.editAttributes(selectedRegions[0], selectedRegions[0].getType(), "");
					return true;
				}
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					JMenuItem mi = super.getMenuItem(invoker);
					Color regionTypeColor = idmp.getLayoutObjectColor(selectedRegions[0].getType());
					if (regionTypeColor != null) {
						mi.setOpaque(true);
						mi.setBackground(regionTypeColor);
					}
					return mi;
				}
			});
			
			//	remove existing region
			actions.add(new SelectionAction("removeRegion", ("Remove " + selectedRegions[0].getType() + " Region"), ("Remove '" + selectedRegions[0].getType() + "' region.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					page.removeRegion(selectedRegions[0]);
					return true;
				}
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					JMenuItem mi = super.getMenuItem(invoker);
					Color regionTypeColor = idmp.getLayoutObjectColor(selectedRegions[0].getType());
					if (regionTypeColor != null) {
						mi.setOpaque(true);
						mi.setBackground(regionTypeColor);
					}
					return mi;
				}
			});
			
			//	change type of existing region
			actions.add(new SelectionAction("changeTypeRegion", ("Change Region Type"), ("Change type of '" + selectedRegions[0].getType() + "' region.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					String regionType = ImUtils.promptForObjectType("Enter Region Type", "Enter or select new region type", page.getRegionTypes(), selectedRegions[0].getType(), true);
					if (regionType == null)
						return false;
					selectedRegions[0].setType(regionType);
					idmp.setRegionsPainted(regionType, true);
					return true;
				}
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					JMenuItem mi = super.getMenuItem(invoker);
					Color regionTypeColor = idmp.getLayoutObjectColor(selectedRegions[0].getType());
					if (regionTypeColor != null) {
						mi.setOpaque(true);
						mi.setBackground(regionTypeColor);
					}
					return mi;
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
						Color regionTypeColor = idmp.getLayoutObjectColor(selectedRegion.getType());
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						pm.add(mi);
					}
					return pm;
				}
			});
			
			//	remove existing region
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
						Color regionTypeColor = idmp.getLayoutObjectColor(selectedRegion.getType());
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						pm.add(mi);
					}
					return pm;
				}
			});
			
			//	remove regions of some type together
			if (multiSelectedRegionsByType.size() == 1) {
				final String removeRegionType = ((String) multiSelectedRegionsByType.firstKey());
				actions.add(new SelectionAction("removeTypeRegion", ("Remove " + multiSelectedRegionsByType.firstKey() + " Regions"), ("Remove all selected '" + multiSelectedRegionsByType.firstKey() + "' regions.")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						LinkedList typeRegs = ((LinkedList) multiSelectedRegionsByType.get(removeRegionType));
						while (typeRegs.size() != 0)
							page.removeRegion((ImRegion) typeRegs.removeFirst());
						return true;
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(removeRegionType);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
			}
			else if (multiSelectedRegionsByType.size() > 1)
				actions.add(new SelectionAction("removeTypeRegion", "Remove Regions ...", "Remove selected regions by type.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return false;
					}
					public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
						JMenu pm = new JMenu("Remove Regions ...");
						JMenuItem mi;
						for (Iterator rtit = multiSelectedRegionsByType.keySet().iterator(); rtit.hasNext();) {
							final String regionType = ((String) rtit.next());
							final LinkedList typeRegions = ((LinkedList) multiSelectedRegionsByType.get(regionType));
							mi = new JMenuItem("- " + regionType + " (" + typeRegions.size() + ")");
							mi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									invoker.beginAtomicAction("Remove '" + regionType + "' Regions");
									while (typeRegions.size() != 0)
										page.removeRegion((ImRegion) typeRegions.removeFirst());
									invoker.endAtomicAction();
									invoker.validate();
									invoker.repaint();
								}
							});
							Color regionTypeColor = idmp.getLayoutObjectColor(regionType);
							if (regionTypeColor != null) {
								mi.setOpaque(true);
								mi.setBackground(regionTypeColor);
							}
							pm.add(mi);
						}
						return pm;
					}
				});
			
			//	change type of existing region
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
						Color regionTypeColor = idmp.getLayoutObjectColor(selectedRegion.getType());
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
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
						public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
							JMenuItem mi = super.getMenuItem(invoker);
							Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.PARAGRAPH_TYPE);
							if (regionTypeColor != null) {
								mi.setOpaque(true);
								mi.setBackground(regionTypeColor);
							}
							return mi;
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
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.PARAGRAPH_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
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
				
				//	get lines, and offer revising block structure if there are multiple
				final ImRegion[] blockLines = block.getRegions(ImRegion.LINE_ANNOTATION_TYPE);
				if (blockLines.length > 1)
					actions.add(new SelectionAction("paragraphsInBlock", "Revise Block Paragraphs", "Revise the grouping of the lines in the block into paragraphs.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return restructureBlock(page, block, block.getRegions(ImRegion.PARAGRAPH_TYPE), blockLines);
						}
						public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
							JMenuItem mi = super.getMenuItem(invoker);
							Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.BLOCK_ANNOTATION_TYPE);
							if (regionTypeColor != null) {
								mi.setOpaque(true);
								mi.setBackground(regionTypeColor);
							}
							return mi;
						}
					});
				
				//	offer adjusting block text direction
				if (idmp.areTextStreamsPainted())
					actions.add(new SelectionAction("textDirectionBlock", "Set Block Text Direction", "Adjust the (general, predominant) text direction of the block.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return false;
						}
						public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
							String blockTextDirection = ((String) block.getAttribute(ImRegion.TEXT_DIRECTION_ATTRIBUTE, ImRegion.TEXT_DIRECTION_LEFT_RIGHT));
							JMenu pm = new JMenu("Block Text Direction");
							ButtonGroup bg = new ButtonGroup();
							final JMenuItem lrmi = new JRadioButtonMenuItem("Left-Right", ImRegion.TEXT_DIRECTION_LEFT_RIGHT.equals(blockTextDirection));
							lrmi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									if (lrmi.isSelected())
										setTextDirection(ImRegion.TEXT_DIRECTION_LEFT_RIGHT, invoker);
								}
							});
							pm.add(lrmi);
							bg.add(lrmi);
							final JMenuItem bumi = new JRadioButtonMenuItem("Bottom-Up", ImRegion.TEXT_DIRECTION_BOTTOM_UP.equals(blockTextDirection));
							bumi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									if (bumi.isSelected())
										setTextDirection(ImRegion.TEXT_DIRECTION_BOTTOM_UP, invoker);
								}
							});
							pm.add(bumi);
							bg.add(bumi);
							final JMenuItem tdmi = new JRadioButtonMenuItem("Top-Down", ImRegion.TEXT_DIRECTION_TOP_DOWN.equals(blockTextDirection));
							tdmi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									if (tdmi.isSelected())
										setTextDirection(ImRegion.TEXT_DIRECTION_TOP_DOWN, invoker);
								}
							});
							pm.add(tdmi);
							bg.add(tdmi);
							final JMenuItem udmi = new JRadioButtonMenuItem("Right-Left & Upside-Down", ImRegion.TEXT_DIRECTION_RIGHT_LEFT_UPSIDE_DOWN.equals(blockTextDirection));
							udmi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									if (udmi.isSelected())
										setTextDirection(ImRegion.TEXT_DIRECTION_RIGHT_LEFT_UPSIDE_DOWN, invoker);
								}
							});
							pm.add(udmi);
							bg.add(udmi);
							pm.setToolTipText(this.tooltip);
							Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.BLOCK_ANNOTATION_TYPE);
							if (regionTypeColor != null) {
								pm.setOpaque(true);
								pm.setBackground(regionTypeColor);
							}
							return pm;
						}
						private void setTextDirection(String textDirection, ImDocumentMarkupPanel invoker) {
							invoker.beginAtomicAction(this.label);
							RegionActionProvider.this.setTextDirection(block, textDirection);
							invoker.endAtomicAction();
							invoker.validate();
							invoker.repaint();
						}
					});
//				
//				//	TODO remove this, and DO NOT EXPORT
//				actions.add(new SelectionAction("paragraphsInBlock", "Analyze Block Structure (TEST)", "Assess the grouping of the lines in the block into paragraphs (TEST).") {
//					public boolean performAction(ImDocumentMarkupPanel invoker) {
//						BlockMetrics blockMetrics = PageAnalysis.computeBlockMetrics(page, page.getImageDPI(), block);
//						System.out.println("Analysis results for block " + block.bounds);
//						System.out.println(" - font sizes: " + blockMetrics.fontSizes);
//						System.out.println("   ==> main font size: " + blockMetrics.mainFontSize);
//						System.out.println(" - font sizes (by chars): " + blockMetrics.charFontSizes);
//						System.out.println("   ==> main font size (by chars): " + blockMetrics.mainCharFontSize);
//						System.out.println(" - font names: " + blockMetrics.fontNames);
//						System.out.println("   ==> main font name: " + blockMetrics.mainFontName);
//						System.out.println(" - font names (by chars): " + blockMetrics.charFontNames);
//						System.out.println("   ==> main font name (by chars): " + blockMetrics.mainCharFontName);
//						System.out.println(" - words: " + blockMetrics.wordCount);
//						System.out.println("   in bold: " + blockMetrics.boldWordCount);
//						System.out.println("   in italics: " + blockMetrics.italicsWordCount);
//						System.out.println(" - chars: " + blockMetrics.charCount);
//						System.out.println("   in bold: " + blockMetrics.boldCharCount);
//						System.out.println("   in italics: " + blockMetrics.italicsCharCount);
//						blockMetrics.analyze();
//						return false;
//					}
//					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
//						JMenuItem mi = super.getMenuItem(invoker);
//						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.BLOCK_ANNOTATION_TYPE);
//						if (regionTypeColor != null) {
//							mi.setOpaque(true);
//							mi.setBackground(regionTypeColor);
//						}
//						return mi;
//					}
//				});
//				//	TODO remove this, and DO NOT EXPORT
//				actions.add(new SelectionAction("paragraphsInContinuedBlock", "Analyze Continue Block Structure (TEST)", "Assess the grouping of the lines in the block into paragraphs, assuming it continuing the previous block (TEST).") {
//					public boolean performAction(ImDocumentMarkupPanel invoker) {
//						ImWord[] blockWords = block.getWords();
//						Arrays.sort(blockWords, ImUtils.textStreamOrder);
//						ImWord prevBlockLastWord = blockWords[0].getPreviousWord();
//						if (prevBlockLastWord == null) {
//							System.out.println("Cannot find predecessor word of " + blockWords[0] + ", so no block to continue from.");
//							return false;
//						}
//						ImPage prevBlockPage = page.getDocument().getPage(prevBlockLastWord.pageId);
//						ImRegion[] prevBlockPageBlocks = prevBlockPage.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
//						ImRegion prevBlock = null;
//						for (int b = 0; b < prevBlockPageBlocks.length; b++)
//							if (prevBlockPageBlocks[b].bounds.includes(prevBlockLastWord.bounds, true)) {
//								prevBlock = prevBlockPageBlocks[b];
//								break;
//							}
//						if (prevBlock == null) {
//							System.out.println("Cannot find block containing " + prevBlockLastWord + " to continue from.");
//							return false;
//						}
//						
//						BlockMetrics prevBlockMetrics = PageAnalysis.computeBlockMetrics(prevBlockPage, prevBlockPage.getImageDPI(), prevBlock);
//						System.out.println("Analysis results for block " + prevBlock.bounds);
//						System.out.println(" - font sizes: " + prevBlockMetrics.fontSizes);
//						System.out.println("   ==> main font size: " + prevBlockMetrics.mainFontSize);
//						System.out.println(" - font sizes (by chars): " + prevBlockMetrics.charFontSizes);
//						System.out.println("   ==> main font size (by chars): " + prevBlockMetrics.mainCharFontSize);
//						System.out.println(" - font names: " + prevBlockMetrics.fontNames);
//						System.out.println("   ==> main font name: " + prevBlockMetrics.mainFontName);
//						System.out.println(" - font names (by chars): " + prevBlockMetrics.charFontNames);
//						System.out.println("   ==> main font name (by chars): " + prevBlockMetrics.mainCharFontName);
//						System.out.println(" - words: " + prevBlockMetrics.wordCount);
//						System.out.println("   in bold: " + prevBlockMetrics.boldWordCount);
//						System.out.println("   in italics: " + prevBlockMetrics.italicsWordCount);
//						System.out.println(" - chars: " + prevBlockMetrics.charCount);
//						System.out.println("   in bold: " + prevBlockMetrics.boldCharCount);
//						System.out.println("   in italics: " + prevBlockMetrics.italicsCharCount);
//						BlockMetrics blockMetrics = PageAnalysis.computeBlockMetrics(page, page.getImageDPI(), block);
//						System.out.println("Analysis results for block " + block.bounds);
//						System.out.println(" - font sizes: " + blockMetrics.fontSizes);
//						System.out.println("   ==> main font size: " + blockMetrics.mainFontSize);
//						System.out.println(" - font sizes (by chars): " + blockMetrics.charFontSizes);
//						System.out.println("   ==> main font size (by chars): " + blockMetrics.mainCharFontSize);
//						System.out.println(" - font names: " + blockMetrics.fontNames);
//						System.out.println("   ==> main font name: " + blockMetrics.mainFontName);
//						System.out.println(" - font names (by chars): " + blockMetrics.charFontNames);
//						System.out.println("   ==> main font name (by chars): " + blockMetrics.mainCharFontName);
//						System.out.println(" - words: " + blockMetrics.wordCount);
//						System.out.println("   in bold: " + blockMetrics.boldWordCount);
//						System.out.println("   in italics: " + blockMetrics.italicsWordCount);
//						System.out.println(" - chars: " + blockMetrics.charCount);
//						System.out.println("   in bold: " + blockMetrics.boldCharCount);
//						System.out.println("   in italics: " + blockMetrics.italicsCharCount);
//						
//						CountingSet lineGaps = new CountingSet(new TreeMap());
//						int lineGapSum = 0;
//						for (int l = 1; l < prevBlockMetrics.aboveLineGaps.length; l++) {
//							lineGapSum += prevBlockMetrics.aboveLineGaps[l];
//							lineGaps.add(new Integer(prevBlockMetrics.aboveLineGaps[l]));
//						}
//						for (int l = 1; l < blockMetrics.aboveLineGaps.length; l++) {
//							lineGapSum += blockMetrics.aboveLineGaps[l];
//							lineGaps.add(new Integer(blockMetrics.aboveLineGaps[l]));
//						}
//						int minLineGap = ((lineGaps.size() == 0) ? 0 : ((Integer) lineGaps.first()).intValue());
//						int maxLineGap = ((lineGaps.size() == 0) ? 0 : ((Integer) lineGaps.last()).intValue());
//						int avgLineGap = ((lineGaps.size() == 0) ? 0 : ((lineGapSum + (lineGaps.size() / 2)) / lineGaps.size()));
//						if (avgLineGap == 0) {
//							System.out.println("Cannot analyze block continuation with two single-line blocks.");
//							return false;
//						}
//						
//						//	we only have a single line distance
//						if ((maxLineGap - minLineGap) < ((page.getImageDPI() + (25 / 2)) / 25)) {
//							System.out.println("ANALYZING WITH AVERAGE LINE GAP OF " + avgLineGap);
//							blockMetrics.analyzeContinuingFrom(prevBlockMetrics, avgLineGap);
//						}
//						
//						//	we might have blocks with line distance paragraph separating
//						else {
//							System.out.println("ANALYZING WITH MINIMUM LINE GAP OF " + minLineGap);
//							blockMetrics.analyzeContinuingFrom(prevBlockMetrics, minLineGap);
//							System.out.println("ANALYZING WITH MAXIMUM LINE GAP OF " + maxLineGap);
//							blockMetrics.analyzeContinuingFrom(prevBlockMetrics, maxLineGap);
//						}
//						
//						return false;
//					}
//					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
//						JMenuItem mi = super.getMenuItem(invoker);
//						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.BLOCK_ANNOTATION_TYPE);
//						if (regionTypeColor != null) {
//							mi.setOpaque(true);
//							mi.setBackground(regionTypeColor);
//						}
//						return mi;
//					}
//				});
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
						
						//	get block style
						ImDocumentStyle blockStyle = getBlockStyle(page.getDocument());
						
						//	update paragraphs and text stream structure
						updateBlockStructure(page, block, blockStyle, true);
						
						//	finally ...
						return true;
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.BLOCK_ANNOTATION_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
			
			//	if one block partially selected, offer splitting
			if (idmp.areRegionsPainted(ImRegion.BLOCK_ANNOTATION_TYPE) && contextRegionsByType.containsKey(ImRegion.BLOCK_ANNOTATION_TYPE) && !multiSelectedRegionsByType.containsKey(ImRegion.BLOCK_ANNOTATION_TYPE)) {
				final ImRegion block = ((ImRegion) ((LinkedList) contextRegionsByType.get(ImRegion.BLOCK_ANNOTATION_TYPE)).getFirst());
				actions.add(new SelectionAction("paragraphsInBlock", "Split Block", "Split selected block, re-detect lines, and group them into paragraphs.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return splitBlock(page, block, selectedBounds);
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.BLOCK_ANNOTATION_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
			}
			
			//	if columns, blocks, and paragraphs visible and multiple columns selected, offer merging columns and re-detecting blocks, paragraphs, and lines
			if (idmp.areRegionsPainted(ImRegion.COLUMN_ANNOTATION_TYPE) && idmp.areRegionsPainted(ImRegion.BLOCK_ANNOTATION_TYPE) && multiSelectedRegionsByType.containsKey(ImRegion.COLUMN_ANNOTATION_TYPE))
				actions.add(new SelectionAction("blocksInColumn", "Merge Columns", "Merge selected columns, re-detect first blocks, then lines, and group the latter into paragraphs.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						
						//	compute merged column bounds and clean up columns
						LinkedList columnList = ((LinkedList) multiSelectedRegionsByType.get(ImRegion.COLUMN_ANNOTATION_TYPE));
						ImRegion[] columns = ((ImRegion[]) columnList.toArray(new ImRegion[columnList.size()]));
						for (int b = 0; b < columns.length; b++)
							page.removeRegion(columns[b]);
						BoundingBox columnBounds = ImLayoutObject.getAggregateBox(columns);
						
						//	remove old blocks, paragraphs, and lines
						ImRegion[] columnBlocks = page.getRegionsInside(ImRegion.BLOCK_ANNOTATION_TYPE, columnBounds, false);
						for (int b = 0; b < columnBlocks.length; b++)
							page.removeRegion(columnBlocks[b]);
						ImRegion[] columnParagraphs = page.getRegionsInside(ImRegion.PARAGRAPH_TYPE, columnBounds, false);
						for (int p = 0; p < columnParagraphs.length; p++)
							page.removeRegion(columnParagraphs[p]);
						ImRegion[] columnLines = page.getRegionsInside(ImRegion.LINE_ANNOTATION_TYPE, columnBounds, false);
						for (int l = 0; l < columnLines.length; l++)
							page.removeRegion(columnLines[l]);
						
						//	mark merged column
						ImRegion column = markColumn(page, columnBounds);
						
						//	did we change anything?
						return (column != null);
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.COLUMN_ANNOTATION_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
			
			//	if one column partially selected, and one or more blocks fully selected, offer splitting
			if (idmp.areRegionsPainted(ImRegion.COLUMN_ANNOTATION_TYPE) && idmp.areRegionsPainted(ImRegion.BLOCK_ANNOTATION_TYPE) && contextRegionsByType.containsKey(ImRegion.COLUMN_ANNOTATION_TYPE) && !multiSelectedRegionsByType.containsKey(ImRegion.COLUMN_ANNOTATION_TYPE) && selectedRegionsByType.containsKey(ImRegion.BLOCK_ANNOTATION_TYPE)) {
				final ImRegion column = ((ImRegion) ((LinkedList) contextRegionsByType.get(ImRegion.COLUMN_ANNOTATION_TYPE)).getFirst());
				actions.add(new SelectionAction("blocksInColumn", "Split Column", "Split selected column, re-detect blocks, then lines, and group the latter into paragraphs.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return splitColumn(page, column, selectedBounds);
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.COLUMN_ANNOTATION_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
			}
			
			//	if blocks and paragraphs visible and click inside block or block selected, offer re-detecting paragraphs
			if (idmp.areRegionsPainted(ImRegion.COLUMN_ANNOTATION_TYPE) && (selectedRegionsByType.containsKey(ImRegion.COLUMN_ANNOTATION_TYPE) || contextRegionsByType.containsKey(ImRegion.COLUMN_ANNOTATION_TYPE)) && !multiSelectedRegionsByType.containsKey(ImRegion.COLUMN_ANNOTATION_TYPE)) {
				
				//	get block
				final ImRegion column;
				if (selectedRegionsByType.containsKey(ImRegion.COLUMN_ANNOTATION_TYPE))
					column = ((ImRegion) ((LinkedList) selectedRegionsByType.get(ImRegion.COLUMN_ANNOTATION_TYPE)).getFirst());
				else column = ((ImRegion) ((LinkedList) contextRegionsByType.get(ImRegion.COLUMN_ANNOTATION_TYPE)).getFirst());
				
//				//	TODO get lines, offer revising column structure (basically to revise block splits and then paragraphs in each block)
//				final ImRegion[] blockLines = column.getRegions(ImRegion.LINE_ANNOTATION_TYPE);
//				if (blockLines.length > 1)
//					actions.add(new SelectionAction("paragraphsInBlock", "Revise Block Paragraphs", "Revise the grouping of the lines in the block into paragraphs.") {
//						public boolean performAction(ImDocumentMarkupPanel invoker) {
//							return restructureBlock(page, column, column.getRegions(ImRegion.PARAGRAPH_TYPE), blockLines);
//						}
//						public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
//							JMenuItem mi = super.getMenuItem(invoker);
//							Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.BLOCK_ANNOTATION_TYPE);
//							if (regionTypeColor != null) {
//								mi.setOpaque(true);
//								mi.setBackground(regionTypeColor);
//							}
//							return mi;
//						}
//					});
				
				//	offer adjusting column text direction
				if (idmp.areTextStreamsPainted())
					actions.add(new SelectionAction("textDirectionColumn", "Set Column Text Direction", "Adjust the (general, predominant) text direction of the column.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return false;
						}
						public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
							String columnTextDirection = ((String) column.getAttribute(ImRegion.TEXT_DIRECTION_ATTRIBUTE, ImRegion.TEXT_DIRECTION_LEFT_RIGHT));
							JMenu pm = new JMenu("Column Text Direction");
							ButtonGroup bg = new ButtonGroup();
							final JMenuItem lrmi = new JRadioButtonMenuItem("Left-Right", ImRegion.TEXT_DIRECTION_LEFT_RIGHT.equals(columnTextDirection));
							lrmi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									if (lrmi.isSelected())
										setTextDirection(ImRegion.TEXT_DIRECTION_LEFT_RIGHT, invoker);
								}
							});
							pm.add(lrmi);
							bg.add(lrmi);
							final JMenuItem bumi = new JRadioButtonMenuItem("Bottom-Up", ImRegion.TEXT_DIRECTION_BOTTOM_UP.equals(columnTextDirection));
							bumi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									if (bumi.isSelected())
										setTextDirection(ImRegion.TEXT_DIRECTION_BOTTOM_UP, invoker);
								}
							});
							pm.add(bumi);
							bg.add(bumi);
							final JMenuItem tdmi = new JRadioButtonMenuItem("Top-Down", ImRegion.TEXT_DIRECTION_TOP_DOWN.equals(columnTextDirection));
							tdmi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									if (tdmi.isSelected())
										setTextDirection(ImRegion.TEXT_DIRECTION_TOP_DOWN, invoker);
								}
							});
							pm.add(tdmi);
							bg.add(tdmi);
							final JMenuItem udmi = new JRadioButtonMenuItem("Right-Left & Upside-Down", ImRegion.TEXT_DIRECTION_RIGHT_LEFT_UPSIDE_DOWN.equals(columnTextDirection));
							udmi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									if (udmi.isSelected())
										setTextDirection(ImRegion.TEXT_DIRECTION_RIGHT_LEFT_UPSIDE_DOWN, invoker);
								}
							});
							pm.add(udmi);
							bg.add(udmi);
							Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.COLUMN_ANNOTATION_TYPE);
							if (regionTypeColor != null) {
								pm.setOpaque(true);
								pm.setBackground(regionTypeColor);
							}
							return pm;
						}
						private void setTextDirection(String textDirection, ImDocumentMarkupPanel invoker) {
							invoker.beginAtomicAction(this.label);
							RegionActionProvider.this.setTextDirection(column, textDirection);
							invoker.endAtomicAction();
							invoker.validate();
							invoker.repaint();
						}
					});
			}
		}
		
		/* TODO offer sanitizing selected regions:
		 * - shrink any selected 'column', 'block', 'paragraph', and 'line' regions to their contained words ...
		 * - ... and remove duplicates afterwards
		 * - also remove any regions nested in other regions of same type
		 * 
		 * - apply this to all regions that have at least one word selected (in terms of center point)
		 * 
		 * TODO also consider applying this to generic 'region' regions, or discarding these guys altogether
		 */
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	void setTextDirection(ImRegion columnOrBlock, String textDirection) {
		
		//	prepare effective attribute value (default is omitted)
		if (ImRegion.TEXT_DIRECTION_LEFT_RIGHT.equals(textDirection))
			textDirection = null;
		
		//	set text direction of column or block proper
		if (textDirection == null)
			columnOrBlock.removeAttribute(ImRegion.TEXT_DIRECTION_ATTRIBUTE);
		else columnOrBlock.setAttribute(ImRegion.TEXT_DIRECTION_ATTRIBUTE, textDirection);
		
		//	on columns, set text direction of blocks
		if (ImRegion.COLUMN_ANNOTATION_TYPE.equals(columnOrBlock.getType()))
			setRegionTextDirection(columnOrBlock.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE), textDirection);
		
		//	set text direction of paragraphs and lines
		setRegionTextDirection(columnOrBlock.getRegions(ImRegion.PARAGRAPH_TYPE), textDirection);
		setRegionTextDirection(columnOrBlock.getRegions(ImRegion.LINE_ANNOTATION_TYPE), textDirection);
		
		//	order text stream
		ImUtils.orderStream(columnOrBlock.getWords(), textDirection);
	}
	
	private static void setRegionTextDirection(ImRegion[] regions, String textDirection) {
		for (int r = 0; r < regions.length; r++) {
			if (textDirection == null)
				regions[r].removeAttribute(ImRegion.TEXT_DIRECTION_ATTRIBUTE);
			else regions[r].setAttribute(ImRegion.TEXT_DIRECTION_ATTRIBUTE, textDirection);
		}
	}
	
	boolean cleanupPageRegions(ImPage page) {
		
		//	keep track of any changes
		boolean pageRegionsChanged = false;
		
		//	get page regions and words
		ArrayList pageRegions = new ArrayList(Arrays.asList(page.getRegions()));
		ImWord[] pageWords = page.getWords();
		
		//	shrink any text regions to contained words
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
	
	/**
	 * Mark a column region, including blocks, paragraphs, and lines. This
	 * method also orders the text stream. If the argument bounding box does
	 * not contain any words, this method returns null.
	 * @param page the page the column belongs to
	 * @param columnBounds the bounding box of the column to create
	 * @return the newly marked column
	 */
	public ImRegion markColumn(ImPage page, BoundingBox columnBounds) {
		return markColumn(page, columnBounds, null);
	}
	
	private static ImRegion markColumn(ImPage page, BoundingBox columnBounds, ImDocumentStyle blockStyle) {
		ImWord[] words = page.getWordsInside(columnBounds);
		if (words.length == 0)
			return null;
		
		//	get last external predecessor and first external successor
		Arrays.sort(words, ImUtils.textStreamOrder);
		ImWord columnPrevWord = words[0].getPreviousWord();
		ImWord columnNextWord = words[words.length-1].getNextWord();
		
		//	cut out column words to avoid conflicts while re-chaining
		if (columnPrevWord != null)
			columnPrevWord.setNextWord(null);
		if (columnNextWord != null)
			columnNextWord.setPreviousWord(null);
		
		//	get block style
		if (blockStyle == null)
			blockStyle = getBlockStyle(page.getDocument());
		
		//	mark column and structure
		ImRegion[] returnColumn = {null};
		words = markColumn(page, words, returnColumn, blockStyle);
		
		//	re-wire text stream
		words[0].setPreviousWord(columnPrevWord);
		words[words.length - 1].setNextWord(columnNextWord);
		
		//	finally
		return returnColumn[0];
	}
	
	/**
	 * Split a column region, marking blocks, paragraphs, and lines. The
	 * argument column region has to be attached to the page it refers to, and
	 * the argument selection box has to overlap with the argument column.
	 * @param column the column to split
	 * @param splitBounds the bounding box to perform the split with
	 * @return true if the column was split, false otherwise
	 */
	public boolean splitColumn(ImRegion column, BoundingBox splitBounds) {
		if (!column.bounds.overlaps(splitBounds))
			return false;
		ImPage page = column.getPage();
		if (page == null)
			return false;
		return this.splitColumn(page, column, splitBounds);
	}
	
	/**
	 * Split a column region, marking blocks, paragraphs, and lines. The
	 * argument selection box has to overlap with the argument column. Whether
	 * or not the argument column is attached to the argument page, the result
	 * of the split is.
	 * @param page the page to split split the column in
	 * @param column the column to split
	 * @param splitBounds the bounding box to perform the split with
	 * @return true if the column was split, false otherwise
	 */
	public boolean splitColumn(ImPage page, ImRegion column, BoundingBox selectedBounds) {
		
		//	TODO leave tables alone
		
		//	TODO leave captions alone
		//	TODO leave labels alone
		
		//	sort block words into top, left, right, and bottom of as well as inside selection
		ImWord[] columnWords = column.getWords();
		ArrayList aboveWords = new ArrayList();
		ArrayList belowWords = new ArrayList();
		ArrayList leftWords = new ArrayList();
		ArrayList rightWords = new ArrayList();
		ArrayList selectedWords = new ArrayList();
		for (int w = 0; w < columnWords.length; w++) {
			if (columnWords[w].centerY < selectedBounds.top)
				aboveWords.add(columnWords[w]);
			else if (selectedBounds.bottom <= columnWords[w].centerY)
				belowWords.add(columnWords[w]);
			else if (columnWords[w].centerX < selectedBounds.left)
				leftWords.add(columnWords[w]);
			else if (selectedBounds.right <= columnWords[w].centerX)
				rightWords.add(columnWords[w]);
			else selectedWords.add(columnWords[w]);
		}
		
		//	anything selected, everything selected?
		if (selectedWords.isEmpty() || (selectedWords.size() == columnWords.length))
			return false;
		
		//	remove old blocks, paragraphs, and lines
		ImRegion[] columnBlocks = column.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
		for (int b = 0; b < columnBlocks.length; b++)
			page.removeRegion(columnBlocks[b]);
		ImRegion[] columnParagraphs = column.getRegions(ImRegion.PARAGRAPH_TYPE);
		for (int p = 0; p < columnParagraphs.length; p++)
			page.removeRegion(columnParagraphs[p]);
		ImRegion[] columnLines = column.getRegions(ImRegion.LINE_ANNOTATION_TYPE);
		for (int l = 0; l < columnLines.length; l++)
			page.removeRegion(columnLines[l]);
		
		//	remove the to-split column (need to do this before marking the blocks so it doesn't interfere with paragraph detection)
		page.removeRegion(column);
		
		//	get last external predecessor and first external successor
		//	TODO make damn sure to connect text streams to outside predecessors and successors
		//	TODO also make damn sure to not connect any previously unconnected words (page titles, captions, etc.)
		Arrays.sort(columnWords, ImUtils.textStreamOrder);
		ImWord columnPrevWord = columnWords[0].getPreviousWord();
		ImWord columnNextWord = columnWords[columnWords.length-1].getNextWord();
		
		//	cut out column words to avoid conflicts while re-chaining
		if (columnPrevWord != null)
			columnPrevWord.setNextWord(null);
		if (columnNextWord != null)
			columnNextWord.setPreviousWord(null);
		
		//	get block style (no need to do so over and over again when marking blocks)
		ImDocumentStyle blockStyle = getBlockStyle(page.getDocument());
		
		//	collect block words in order of chaining
		ArrayList columnWordLists = new ArrayList();
		
		//	if we have a plain left-right split, chain columns left to right
		if (aboveWords.isEmpty() && belowWords.isEmpty()) {
			if (leftWords.size() != 0)
				columnWordLists.add(markColumn(page, leftWords, blockStyle));
			if (selectedWords.size() != 0)
				columnWordLists.add(markColumn(page, selectedWords, blockStyle));
			if (rightWords.size() != 0)
				columnWordLists.add(markColumn(page, rightWords, blockStyle));
		}
		
		//	selection cut out on top left corner
		else if (aboveWords.isEmpty() && leftWords.isEmpty()) {
			if (selectedWords.size() != 0)
				columnWordLists.add(markColumn(page, selectedWords, blockStyle));
			if (rightWords.size() != 0)
				columnWordLists.add(markColumn(page, rightWords, blockStyle));
			if (belowWords.size() != 0)
				columnWordLists.add(markColumn(page, belowWords, blockStyle));
		}
		
		//	selection cut out in some other place
		else {
			if (aboveWords.size() != 0)
				columnWordLists.add(markColumn(page, aboveWords, blockStyle));
			if (leftWords.size() != 0)
				columnWordLists.add(markColumn(page, leftWords, blockStyle));
			if (selectedWords.size() != 0)
				columnWordLists.add(markColumn(page, selectedWords, blockStyle));
			if (rightWords.size() != 0)
				columnWordLists.add(markColumn(page, rightWords, blockStyle));
			if (belowWords.size() != 0)
				columnWordLists.add(markColumn(page, belowWords, blockStyle));
		}
		
		//	chain block text streams (only if text stream types match, however)
		for (int b = 0; b < columnWordLists.size(); b++) {
			ImWord[] imws = ((ImWord[]) columnWordLists.get(b));
			if (imws.length == 0)
				continue;
			if ((columnPrevWord != null) && !columnPrevWord.getTextStreamType().equals(imws[0].getTextStreamType()))
				continue;
			if (columnPrevWord != null)
				columnPrevWord.setNextWord(imws[0]);
			columnPrevWord = imws[imws.length - 1];
		}
		if ((columnPrevWord != null) && (columnNextWord != null))
			columnPrevWord.setNextWord(columnNextWord);
		
		//	indicate we've done something
		return true;
	}
	
	private static ImWord[] markColumn(ImPage page, ArrayList wordList, ImDocumentStyle blockStyle) {
		return markColumn(page, ((ImWord[]) wordList.toArray(new ImWord[wordList.size()])), null, blockStyle);
	}
	
	private static ImWord[] markColumn(ImPage page, ImWord[] words, ImRegion[] returnColumn, ImDocumentStyle blockStyle) {
		if (words.length == 0)
			return words;
		
		//	mark column
		ImRegion column = new ImRegion(page, ImLayoutObject.getAggregateBox(words), ImRegion.COLUMN_ANNOTATION_TYPE);
		if (returnColumn != null)
			returnColumn[0] = column;
		
		//	cut out column words to avoid conflicts while re-chaining
		Arrays.sort(words, ImUtils.textStreamOrder);
		if (words[0].getPreviousWord() != null)
			words[0].getPreviousWord().setNextWord(null);
		if ( words[words.length-1].getNextWord() != null)
			 words[words.length-1].getNextWord().setPreviousWord(null);
		
		//	get block boundaries
		BoundingBox[] columnBlockBounds = getColumnBlockBounds(page, column, words);
		
		//	mark blocks (analyzes structure internally)
		ArrayList columnBlockWordLists = new ArrayList();
		for (int b = 0; b < columnBlockBounds.length; b++) {
			ImWord[] blockWords = page.getWordsInside(columnBlockBounds[b]);
			if (blockWords.length != 0)
				columnBlockWordLists.add(markBlock(page, blockWords, blockStyle));
		}
		
		//	chain block text streams (only inside column, though, as outside chaining happens in calling code)
		for (int b = 1; b < columnBlockWordLists.size(); b++) {
			ImWord[] imws1 = ((ImWord[]) columnBlockWordLists.get(b-1));
			ImWord[] imws2 = ((ImWord[]) columnBlockWordLists.get(b));
			if ((imws1.length != 0) && (imws2.length != 0))
				imws1[imws1.length - 1].setNextWord(imws2[0]);
		}
		
		//	finally ...
		Arrays.sort(words, ImUtils.textStreamOrder);
		return words;
	}
	
	private static BoundingBox[] getColumnBlockBounds(ImPage page, ImRegion column, ImWord[] words) {
		
		//	analyze line gaps
		boolean[] containsWord = new boolean[column.bounds.getHeight()];
		Arrays.fill(containsWord, false);
		for (int w = 0; w < words.length; w++) {
			for (int r = words[w].bounds.top; r < words[w].bounds.bottom; r++)
				containsWord[r - column.bounds.top] = true;
		}
		CountingSet lineGaps = new CountingSet(new TreeMap());
		for (int r = 0; r < containsWord.length; r++) {
			if (containsWord[r])
				continue;
			int lineGap = 1;
			while ((r + lineGap) < containsWord.length) {
				if (containsWord[r + lineGap])
					break;
				else lineGap++;
			}
			lineGaps.add(new Integer(lineGap));
			r += lineGap; // we've seen all of those, including next occupied row
		}
		System.out.println("Got line gaps: " + lineGaps);
		int lineGapSum = 0;
		for (Iterator lgit = lineGaps.iterator(); lgit.hasNext();) {
			Integer lineGap = ((Integer) lgit.next());
			lineGapSum += (lineGap.intValue() * lineGaps.getCount(lineGap));
		}
		int avgLineGap = (lineGaps.isEmpty() ? -1 : ((lineGapSum + (lineGaps.size() / 2)) / lineGaps.size()));
		System.out.println("Average line gap is " + avgLineGap);
		
		//	identify block gaps as ones larger than smallest group
		int minBlockMargin = Integer.MAX_VALUE;
		int maxBelowAvgLineGap = 0;
		int minAboveAvgLineGap = Integer.MAX_VALUE;
		for (Iterator lgit = lineGaps.iterator(); lgit.hasNext();) {
			Integer lineGap = ((Integer) lgit.next());
			if (lineGap.intValue() <= avgLineGap)
				maxBelowAvgLineGap = lineGap.intValue();
			else {
				minAboveAvgLineGap = lineGap.intValue();
				break;
			}
		}
		System.out.println("Split is between " + maxBelowAvgLineGap + " and " + minAboveAvgLineGap);
		if ((minAboveAvgLineGap - maxBelowAvgLineGap) < (page.getImageDPI() / 25))
			System.out.println(" ==> too insignificant");
		else if (minAboveAvgLineGap == Integer.MAX_VALUE)
			System.out.println(" ==> too uniformly distributed");
		else minBlockMargin = minAboveAvgLineGap;
		
		//	get configured style
		//	TODO how to go about discrepancy between measurements and template ???
		ImDocumentStyle docStyle = ImDocumentStyle.getStyleFor(page.getDocument());
		if (docStyle == null)
			docStyle = new ImDocumentStyle(new PropertiesData(new Properties()));
		int styleMinBlockMargin = docStyle.getImSubset("layout").getIntProperty("minBlockMargin", Integer.MAX_VALUE, page.getImageDPI());
		System.out.println("Template split is above " + styleMinBlockMargin);
		if (styleMinBlockMargin == Integer.MAX_VALUE)
			System.out.println(" ==> template parameter unavailable");
		
		//	mark blocks (analyzes structure internally)
		ArrayList columnBlockBounds = new ArrayList();
		int blockTop = column.bounds.top;
		for (int r = 0; r < containsWord.length; r++) {
			if (containsWord[r])
				continue;
			int lineGap = 1;
			while ((r + lineGap) < containsWord.length) {
				if (containsWord[r + lineGap])
					break;
				else lineGap++;
			}
			if (lineGap >= minBlockMargin) {
				columnBlockBounds.add(new BoundingBox(column.bounds.left, column.bounds.right, blockTop, (r + column.bounds.top)));
				blockTop = (r + lineGap + column.bounds.top);
			}
			r += lineGap; // we've seen all of those, including next occupied row
		}
		columnBlockBounds.add(new BoundingBox(column.bounds.left, column.bounds.right, blockTop, column.bounds.bottom));
		
		//	finally ...
		return ((BoundingBox[]) columnBlockBounds.toArray(new BoundingBox[columnBlockBounds.size()]));
	}
	
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
	 * @param page the page to split split the block in
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
		
		//	get block style
		ImDocumentStyle blockStyle = getBlockStyle(page.getDocument());
		
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
				updateBlockStructure(page, aboveBlock, blockStyle, false);
			}
			ImRegion selectedBlock = new ImRegion(page, ImLayoutObject.getAggregateBox((ImWord[]) selectedWords.toArray(new ImWord[selectedWords.size()])), ImRegion.BLOCK_ANNOTATION_TYPE);
			updateBlockStructure(page, selectedBlock, blockStyle, false);
			if (belowWords.size() != 0) {
				ImRegion belowBlock = new ImRegion(page, ImLayoutObject.getAggregateBox((ImWord[]) belowWords.toArray(new ImWord[belowWords.size()])), ImRegion.BLOCK_ANNOTATION_TYPE);
				updateBlockStructure(page, belowBlock, blockStyle, false);
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
				blockWordLists.add(markBlock(page, leftWords, blockStyle));
			if (selectedWords.size() != 0)
				blockWordLists.add(markBlock(page, selectedWords, blockStyle));
			if (rightWords.size() != 0)
				blockWordLists.add(markBlock(page, rightWords, blockStyle));
		}
		
		//	selection cut out on top left corner
		else if (aboveWords.isEmpty() && leftWords.isEmpty()) {
			if (selectedWords.size() != 0)
				blockWordLists.add(markBlock(page, selectedWords, blockStyle));
			if (rightWords.size() != 0)
				blockWordLists.add(markBlock(page, rightWords, blockStyle));
			if (belowWords.size() != 0)
				blockWordLists.add(markBlock(page, belowWords, blockStyle));
		}
		
		//	selection cut out in some other place
		else {
			if (aboveWords.size() != 0)
				blockWordLists.add(markBlock(page, aboveWords, blockStyle));
			if (leftWords.size() != 0)
				blockWordLists.add(markBlock(page, leftWords, blockStyle));
			if (rightWords.size() != 0)
				blockWordLists.add(markBlock(page, rightWords, blockStyle));
			if (belowWords.size() != 0)
				blockWordLists.add(markBlock(page, belowWords, blockStyle));
			if (selectedWords.size() != 0)
				blockWordLists.add(markBlock(page, selectedWords, blockStyle));
		}
		
		//	chain block text streams (only if text stream types match, however)
		for (int b = 0; b < blockWordLists.size(); b++) {
			ImWord[] imws = ((ImWord[]) blockWordLists.get(b));
			if (imws.length == 0)
				continue;
			if ((blockPrevWord != null) && !blockPrevWord.getTextStreamType().equals(imws[0].getTextStreamType()))
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
	
	private static ImWord[] markBlock(ImPage page, ArrayList wordList, ImDocumentStyle blockStyle) {
		return markBlock(page, ((ImWord[]) wordList.toArray(new ImWord[wordList.size()])), blockStyle);
	}
	
	private static ImWord[] markBlock(ImPage page, ImWord[] words, ImDocumentStyle blockStyle) {
		if (words.length == 0)
			return words;
		
		//	mark lines
		ImUtils.makeStream(words, words[0].getTextStreamType(), null, false); // no need to clean up annotations, we're re-joining the streams later
		sortIntoLines(page, words);
		
		//	mark block proper
		ImRegion block = new ImRegion(page, ImLayoutObject.getAggregateBox(words), ImRegion.BLOCK_ANNOTATION_TYPE);
		
		//	re-detect paragraphs
		PageAnalysis.splitIntoParagraphs(page, page.getImageDPI(), block, blockStyle);
		
		//	finally ...
		Arrays.sort(words, ImUtils.textStreamOrder);
		return words;
	}
	
	static void sortIntoLines(ImPage page, ImWord[] words) {
		
		//	order text stream
		ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
		Arrays.sort(words, ImUtils.textStreamOrder);
		
		//	re-detect lines
		markLines(page, words, null);
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
		return markBlock(page, blockBounds, null);
	}
	
	private static ImRegion markBlock(ImPage page, BoundingBox blockBounds, ImDocumentStyle blockStyle) {
		ImWord[] words = page.getWordsInside(blockBounds);
		if (words.length == 0)
			return null;
		
		//	mark lines
		ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
		Arrays.sort(words, ImUtils.textStreamOrder);
		markLines(page, words, null);
		
		//	mark block proper
		ImRegion block = new ImRegion(page, ImLayoutObject.getAggregateBox(words), ImRegion.BLOCK_ANNOTATION_TYPE);
		
		//	get block style
		if (blockStyle == null)
			blockStyle = getBlockStyle(page.getDocument());
		
		//	re-detect paragraphs
		PageAnalysis.splitIntoParagraphs(page, page.getImageDPI(), block, blockStyle);
		
		//	finally ...
		return block;
	}
	
	private static void markLines(ImPage page, ImWord[] words, Map existingLines) {
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
	
	static void updateBlockStructure(ImPage page, ImRegion block, ImDocumentStyle blockStyle, boolean forceRevisitParagraphs) {
		
		//	update paragraphs if either requested explicitly or sufficient lines present
		boolean revisitParagraphs;
		if (forceRevisitParagraphs)
			revisitParagraphs = true;
		else {
			ImRegion[] blockLines = block.getRegions(ImRegion.LINE_ANNOTATION_TYPE);
			revisitParagraphs = (blockLines.length > 4);
		}
		if (revisitParagraphs)
			PageAnalysis.splitIntoParagraphs(page, page.getImageDPI(), block, blockStyle);
		
		//	update text stream in any case
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
	
	static boolean sortLinesIntoParagraphs(ImPage page, ImRegion[] lines, BoundingBox selectedBounds) {
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
	
	static void ensureColumnStructure(ImRegion column) {
		
		//	get words
		ImWord[] cWords = column.getWords();
		if (cWords.length == 0)
			return;
		
		//	check if all (non-artifact) words inside lines
		ImRegion[] cBlocks = column.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
		int bWordCount = 0;
		for (int b = 0; b < cBlocks.length; b++) {
			ImWord[] bWords = cBlocks[b].getWords();
			bWordCount += bWords.length;
		}
		
		//	some words outside blocks
		if (bWordCount < cWords.length) {
			
			//	get page
			ImPage page = column.getPage();
			
			//	index blocks by bounding boxes
			HashMap exBlocks = new HashMap();
			for (int b = 0; b < cBlocks.length; b++)
				exBlocks.put(cBlocks[b].bounds, cBlocks[b]);
			
			//	get column block bounds
			BoundingBox[] cBlockBounds = getColumnBlockBounds(page, column, cWords);
			
			//	check for existing blocks
			for (int b = 0; b < cBlockBounds.length; b++) {
				ImRegion cBlock = ((ImRegion) exBlocks.remove(cBlockBounds[b]));
				if (cBlock == null)
					cBlock = new ImRegion(page, cBlockBounds[b], ImRegion.BLOCK_ANNOTATION_TYPE);
			}
			
			//	remove now-spurious blocks
			for (Iterator lbit = exBlocks.keySet().iterator(); lbit.hasNext();)
				page.removeRegion((ImRegion) exBlocks.get(lbit.next()));
			
			//	get now-valid blocks
			cBlocks = column.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
		}
		
		//	check individual blocks
		for (int b = 0; b < cBlocks.length; b++)
			ensureBlockStructure(cBlocks[b]);
	}
	
	static void ensureBlockStructure(ImRegion block) {
		
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
			
			//	get block style
			ImDocumentStyle blockStyle = getBlockStyle(page.getDocument());
			
			//	re-mark paragraphs
//			PageAnalysis.splitIntoParagraphs(block, page.getImageDPI(), ProgressMonitor.dummy);
//			BlockMetrics blockMetrics = PageAnalysis.computeBlockMetrics(page, page.getImageDPI(), block);
//			BlockLayout blockLayout = blockMetrics.analyze();
//			blockLayout.writeParagraphStructure();
			PageAnalysis.splitIntoParagraphs(page, page.getImageDPI(), block, blockStyle);
		}
	}
	
	static boolean restructureBlock(ImPage page, ImRegion block, ImRegion[] blockParagraphs, ImRegion[] blockLines) {
		
		//	compute block metrics
		BlockMetrics blockMetrics = PageAnalysis.computeBlockMetrics(page, page.getImageDPI(), block);
		if (blockMetrics == null)
			return false; // happens if there are no lines at all
		
		//	run analysis (do not use document style here, would influence user selected options)
		BlockLayout blockLayout = blockMetrics.analyze();
		
		//	assemble split option panel
		final JRadioButton pslIndent = new JRadioButton("indented", (blockLayout.paragraphStartLinePos == 'I'));
		final JRadioButton pslOutdent = new JRadioButton("outdented", (blockLayout.paragraphStartLinePos == 'O'));
		final JRadioButton pslFlush = new JRadioButton("neither", (!pslIndent.isSelected() && !pslOutdent.isSelected()));
		ButtonGroup pslButtonGroup = new ButtonGroup();
		pslButtonGroup.add(pslIndent);
		pslButtonGroup.add(pslOutdent);
		pslButtonGroup.add(pslFlush);
		JPanel pslButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT), true);
		pslButtonPanel.add(pslIndent);
		pslButtonPanel.add(pslOutdent);
		pslButtonPanel.add(pslFlush);
		JPanel pslPanel = new JPanel(new BorderLayout(), true);
		pslPanel.add(new JLabel("Paragraph start lines are "), BorderLayout.WEST);
		pslPanel.add(pslButtonPanel, BorderLayout.CENTER);
		
		final JCheckBox ilhBold = new JCheckBox("bold", false);
		final JCheckBox ilhItalics = new JCheckBox("italics", false);
		final JCheckBox ilhAllCaps = new JCheckBox("all-caps", false);
		final JTextField ilhTerminator = new JTextField(); // TODO use drop-down with all found ???
		ilhTerminator.setPreferredSize(new Dimension(20, ilhTerminator.getPreferredSize().height));
		JPanel ilhButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT), true);
		ilhButtonPanel.add(ilhBold);
		ilhButtonPanel.add(ilhItalics);
		ilhButtonPanel.add(ilhAllCaps);
		ilhButtonPanel.add(new JLabel("  terminated by "));
		ilhButtonPanel.add(ilhTerminator);
		JPanel ilhPanel = new JPanel(new BorderLayout(), true);
		ilhPanel.add(new JLabel("Split before in-line headings in "), BorderLayout.WEST);
		ilhPanel.add(ilhButtonPanel, BorderLayout.CENTER);
		if (blockLayout.inLineHeadingStyle != null) {
			ilhBold.setSelected(blockLayout.inLineHeadingStyle.indexOf('B') != -1);
			ilhItalics.setSelected(blockLayout.inLineHeadingStyle.indexOf('I') != -1);
			ilhAllCaps.setSelected(blockLayout.inLineHeadingStyle.indexOf('C') != -1);
			ilhTerminator.setText("" + blockLayout.inLineHeadingTerminator);
		}
		
		final JCheckBox uriAbove = new JCheckBox("with", ("RA".indexOf(blockLayout.uriLineSplitMode) != -1));
		final JCheckBox uriBelow = new JCheckBox("after", ("RB".indexOf(blockLayout.uriLineSplitMode) != -1));
		JPanel uriButtonPanel = new JPanel(new GridLayout(1, 0), true);
		uriButtonPanel.add(uriAbove);
		uriButtonPanel.add(uriBelow);
		JPanel uriPanel = new JPanel(new FlowLayout(FlowLayout.LEFT), true);
		uriPanel.add(new JLabel("Start new paragraph "), BorderLayout.WEST);
		uriPanel.add(uriButtonPanel, BorderLayout.CENTER);
		uriPanel.add(new JLabel(" lines consisting of a URI"), BorderLayout.EAST);
		
		final JCheckBox shortLineEndsParagraph = new JCheckBox("Lines short of right block edge end paragraphs", blockLayout.shortLineEndsParagraph);
		final JCheckBox lineDistEndsParagraph = new JCheckBox("Large line distance ends paragraphs", (blockLayout.paragraphDistance != -1));
		final JCheckBox singleLineParagraphs = new JCheckBox("Make each line a separate paragraph", false);
		singleLineParagraphs.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ie) {
				pslIndent.setEnabled(!singleLineParagraphs.isSelected());
				pslOutdent.setEnabled(!singleLineParagraphs.isSelected());
				pslFlush.setEnabled(!singleLineParagraphs.isSelected());
				shortLineEndsParagraph.setEnabled(!singleLineParagraphs.isSelected());
				lineDistEndsParagraph.setEnabled(!singleLineParagraphs.isSelected());
				ilhBold.setEnabled(!singleLineParagraphs.isSelected());
				ilhItalics.setEnabled(!singleLineParagraphs.isSelected());
				ilhAllCaps.setEnabled(!singleLineParagraphs.isSelected());
				ilhTerminator.setEditable(!singleLineParagraphs.isSelected());
				uriAbove.setEnabled(!singleLineParagraphs.isSelected());
				uriBelow.setEnabled(!singleLineParagraphs.isSelected());
			}
		});
		
		JPanel blockSplitOptionPanel = new JPanel(new GridLayout(0, 1), true);
		blockSplitOptionPanel.add(pslPanel);
		blockSplitOptionPanel.add(shortLineEndsParagraph);
		blockSplitOptionPanel.add(lineDistEndsParagraph);
		blockSplitOptionPanel.add(ilhPanel);
		blockSplitOptionPanel.add(uriPanel);
		blockSplitOptionPanel.add(singleLineParagraphs);
		
		//	prompt user
		if (DialogFactory.confirm(blockSplitOptionPanel, "Select Block Splitting Options", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION)
			return false;
		
		//	do single-paragraph line split if requested
		if (singleLineParagraphs.isSelected()) {
			ImRegion[] lines = new ImRegion[blockMetrics.lines.length];
			for (int l = 0; l < blockMetrics.lines.length; l++)
				lines[l] = blockMetrics.lines[l].line;
			return PageAnalysis.writeSingleLineParagraphStructure(page, block, lines, BlockLayout.getTextOrientation(blockLayout.alignment), BlockLayout.getIntentation(blockLayout.paragraphStartLinePos));
		}
		
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
		if (inLineHeadingStyle.length() == 0)
			inLineHeadingStyle = null;
		String ilhTerminatorStr = ilhTerminator.getText().trim();
		char inLineHeadingTerminator = ((ilhTerminatorStr.length() == 1) ? ilhTerminatorStr.charAt(0) : ((char) 0));
		if (inLineHeadingTerminator == 0)
			inLineHeadingStyle = null;
		
		//	get URI line splitting mode
		char uriLineSplitMode = 'N';
		if (uriAbove.isSelected() && uriBelow.isSelected())
			uriLineSplitMode = 'R';
		else if (uriAbove.isSelected())
			uriLineSplitMode = 'A';
		else if (uriBelow.isSelected())
			uriLineSplitMode = 'B';
		
		//	re-analyze layout with user input
		blockLayout = blockMetrics.analyze(blockLayout.alignment, paragraphStartLinePos, shortLineEndsParagraph.isSelected(), inLineHeadingStyle, inLineHeadingTerminator, (lineDistEndsParagraph.isSelected() ? blockLayout.paragraphDistance : -1), uriLineSplitMode);
		
		//	apply computed layout
		return blockLayout.writeParagraphStructure();
	}
}
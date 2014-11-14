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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.im.util.ImUtils.StringPair;

/**
 * This class provides basic actions for handling regions.
 * 
 * @author sautter
 */
public class RegionActionProvider extends AbstractSelectionActionProvider implements ImageMarkupToolProvider, LiteratureConstants {
	
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
				JOptionPane.showMessageDialog(DialogFactory.getTopWindow(), "There are no regions in this document.", "No Regions", JOptionPane.INFORMATION_MESSAGE);
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
				JOptionPane.showMessageDialog(DialogFactory.getTopWindow(), "There are no regions in this document.", "No Regions", JOptionPane.INFORMATION_MESSAGE);
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
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, final ImPage page, final ImDocumentMarkupPanel idmp) {
		
		//	mark selection
		BoundingBox selectedBox = new BoundingBox(Math.min(start.x, end.x), Math.max(start.x, end.x), Math.min(start.y, end.y), Math.max(start.y, end.y));
		
		//	get selected regions
		ImRegion[] pageRegions = page.getRegions();
		LinkedList regionList = new LinkedList();
		for (int r = 0; r < pageRegions.length; r++) {
			if (pageRegions[r].bounds.right < selectedBox.left)
				continue;
			if (selectedBox.right < pageRegions[r].bounds.left)
				continue;
			if (pageRegions[r].bounds.bottom < selectedBox.top)
				continue;
			if (selectedBox.bottom < pageRegions[r].bounds.top)
				continue;
			if (idmp.areRegionsPainted(pageRegions[r].getType()))
				regionList.add(pageRegions[r]);
		}
		final ImRegion[] selectedRegions = ((ImRegion[]) regionList.toArray(new ImRegion[regionList.size()]));
		
		//	index selected regions by type for group removal
		final TreeMap selectedRegionsByType = new TreeMap();
		for (int a = 0; a < selectedRegions.length; a++) {
			LinkedList typeRegions = ((LinkedList) selectedRegionsByType.get(selectedRegions[a].getType()));
			if (typeRegions == null) {
				typeRegions = new LinkedList();
				selectedRegionsByType.put(selectedRegions[a].getType(), typeRegions);
			}
			typeRegions.add(selectedRegions[a]);
		}
		for (Iterator atit = selectedRegionsByType.keySet().iterator(); atit.hasNext();) {
			LinkedList typeRegions = ((LinkedList) selectedRegionsByType.get(atit.next()));
			if (typeRegions.size() < 2)
				atit.remove();
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
		if (selectedWords.length == 0)
			selectedBounds = selectedBox;
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
			actions.add(new SelectionAction("Mark Region", ("Mark a region from the selection")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					String regionType = ImUtils.promptForObjectType("Enter Region Type", "Enter or select type of region to create", regionTypes, null, true);
					if (regionType == null)
						return false;
					new ImRegion(page, selectedBounds, regionType);
					idmp.setRegionsPainted(regionType, true);
					return true;
				}
			});
		else actions.add(new SelectionAction("Mark Region", ("Mark a region from the selection")) {
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
							new ImRegion(page, selectedBounds, regionType);
							idmp.setRegionsPainted(regionType, true);
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
							new ImRegion(page, selectedBounds, regionType);
							idmp.setRegionsPainted(regionType, true);
							invoker.validate();
							invoker.repaint();
						}
					});
					pm.add(mi);
				}
				return pm;
			}
		});
		
		//	no region or word selected at all
		if ((selectedRegions.length + selectedWords.length) == 0) {
			actions.add(new SelectionAction(("Edit Page Attributes"), ("Edit attributes of page.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					idmp.editAttributes(page, PAGE_TYPE, "");
					return true;
				}
			});
			actions.add(new SelectionAction(("Edit Page Image & Words"), ("Edit page image and words recognized in page.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return idmp.editPage(page.pageId);
				}
			});
		}
		
		//	single region selected
		if (selectedRegions.length == 1) {
			
			//	edit attributes of existing region
			actions.add(new SelectionAction(("Edit " + selectedRegions[0].getType() + " Attributes"), ("Edit attributes of '" + selectedRegions[0].getType() + "' region.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					idmp.editAttributes(selectedRegions[0], selectedRegions[0].getType(), "");
					return true;
				}
			});
			
			//	remove existing annotation
			actions.add(new SelectionAction(("Remove " + selectedRegions[0].getType() + " Region"), ("Remove '" + selectedRegions[0].getType() + "' region.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					page.removeRegion(selectedRegions[0]);
					return true;
				}
			});
			
			//	change type of existing annotation
			actions.add(new SelectionAction(("Change Region Type"), ("Change type of '" + selectedRegions[0].getType() + "' region.")) {
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
			actions.add(new SelectionAction("Edit Region Attributes ...", "Edit attributes of selected regions.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Edit Region Attributes ...");
					JMenuItem mi;
					for (int t = 0; t < selectedRegions.length; t++) {
						final ImRegion selectedRegion = selectedRegions[t];
						mi = new JMenuItem("- " + selectedRegion.getType());
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								idmp.editAttributes(selectedRegion, selectedRegion.getType(), "");
								invoker.repaint();
							}
						});
						pm.add(mi);
					}
					return pm;
				}
			});
			
			//	remove existing annotation
			actions.add(new SelectionAction("Remove Region ...", "Remove selected regions.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Remove Region ...");
					JMenuItem mi;
					for (int t = 0; t < selectedRegions.length; t++) {
						final ImRegion selectedReg = selectedRegions[t];
						mi = new JMenuItem("- " + selectedReg.getType());
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								page.removeRegion(selectedReg);
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
			if (selectedRegionsByType.size() == 1)
				actions.add(new SelectionAction(("Remove " + selectedRegionsByType.firstKey() + " Regions"), ("Remove all selected '" + selectedRegionsByType.firstKey() + "' regions.")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						page.removeRegion(selectedRegions[0]);
						return true;
					}
				});
			else if (selectedRegionsByType.size() > 1)
				actions.add(new SelectionAction("Remove Regions ...", "Remove selected regions by type.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return false;
					}
					public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
						JMenu pm = new JMenu("Remove Regions ...");
						JMenuItem mi;
						for (Iterator rtit = selectedRegionsByType.keySet().iterator(); rtit.hasNext();) {
							final String regType = ((String) rtit.next());
							final LinkedList typeRegs = ((LinkedList) selectedRegionsByType.get(regType));
							mi = new JMenuItem("- " + regType);
							mi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									invoker.beginAtomicAction("Remove Regions");
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
			actions.add(new SelectionAction("Change Region Type ...", "Change the type of selected regions.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Change Region Type ...");
					JMenuItem mi;
					for (int t = 0; t < selectedRegions.length; t++) {
						final ImRegion selectedReg = selectedRegions[t];
						mi = new JMenuItem("- " + selectedReg.getType());
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								String regionType = ImUtils.promptForObjectType("Enter Region Type", "Enter or select new region type", page.getRegionTypes(), selectedReg.getType(), true);
								if (regionType != null) {
									selectedReg.setType(regionType);
									idmp.setAnnotationsPainted(regionType, true);
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
		
		//	change region into annotation if eligible
		for (int r = 0; r < selectedRegions.length; r++) {
			
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
			actions.add(new SelectionAction(("Annotate '" + regionType + "' Region"), ("Create an annotation from '" + regionType + "' region.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					page.removeRegion(selectedRegion);
					idmp.document.addAnnotation(firstWord, lastWord, regionType);
					idmp.setAnnotationsPainted(regionType, true);
					return true;
				}
			});
		}
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
}
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
package de.uka.ipd.idaho.im.imagine.plugins.tables;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;

import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
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
import de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.basic.RegionActionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.tables.TableAreaStatistics.ColumnOccupationFlank;
import de.uka.ipd.idaho.im.imagine.plugins.tables.TableAreaStatistics.ColumnOccupationLow;
import de.uka.ipd.idaho.im.imagine.plugins.tables.TableAreaStatistics.GraphicsSlice;
import de.uka.ipd.idaho.im.imagine.plugins.tables.TableAreaStatistics.RowOccupationGap;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.TwoClickSelectionAction;
import de.uka.ipd.idaho.im.util.ImObjectTransformer;
import de.uka.ipd.idaho.im.util.ImObjectTransformer.AttributeTransformer;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * This plugin provides actions for marking tables in Image Markup documents,
 * and for very simple data extraction from tables.
 * 
 * @author sautter
 */
public class TableActionProvider extends AbstractSelectionActionProvider implements ImageMarkupToolProvider, ReactionProvider {
	private static final String TABLE_CLEANER_IMT_NAME = "TableAnnotCleaner";
	private ImageMarkupTool tableCleaner = new TableCleaner();
	
	private static final String SPLIT_OPERATION = "split";
	private static final String REMOVE_OPERATION = "remove";
	private static final String CUT_TO_START_OPERATION = "cutToStart";
	private static final String CUT_TO_END_OPERATION = "cutToEnd";
	private Properties annotTypeOperations = new Properties();
	
	private RegionActionProvider regionActionProvider;
	
	/** public zero-argument constructor for class loading */
	public TableActionProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Table Actions";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractGoldenGateImaginePlugin#initImagine()
	 */
	public void initImagine() {
		ImObjectTransformer.addGlobalAttributeTransformer(new AttributeTransformer() {
			public boolean canTransformAttribute(String name) {
				return ("colsContinueFrom".equals(name) || "colsContinueIn".equals(name) || "rowsContinueFrom".equals(name) || "rowsContinueIn".equals(name));
			}
			public Object transformAttributeValue(ImObject object, String name, Object value, ImObjectTransformer transformer) {
				if (value == null)
					return null;
				String wordId = value.toString();
				int split = wordId.indexOf(".");
				if (split == -1)
					return value;
				try {
					int pageId = Integer.parseInt(wordId.substring(0, split));
					if (pageId != transformer.fromPageId)
						return value;
					BoundingBox bounds = BoundingBox.parse(wordId.substring(split + ".".length()));
					return (transformer.toPageId + "." + transformer.transformBounds(bounds));
				}
				catch (RuntimeException re) {
					return value;
				}
			}
		});
	}
	
	/* TODO visualize connected table columns or rows:
	 * - use display extension provider ...
	 * - ... to thicken connected edges of tables
	 * - only if table regions displaying
	 * - use lower-alpha version of table region color
	 * - thicken region edge by some 10-20 pixels
	 * - maybe generate some kind of outward pointing triangles (like paper ripped off over a respective blade)
	 * 
	 * ==> saves a ton of attribute checking
	 * ==> change display extensions when table attributes change
	 */
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		
		//	read operations for individual annotation types
		if (this.parent != null) try {
			Reader tor = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream("tableCleanupOptions.cnfg")));
			StringVector typeOperationList = StringVector.loadList(tor);
			tor.close();
			for (int t = 0; t < typeOperationList.size(); t++) {
				String typeOperation = typeOperationList.get(t).trim();
				if (typeOperation.length() == 0)
					continue;
				if (typeOperation.startsWith("//"))
					continue;
				String[] typeAndOperation = typeOperation.split("\\s+");
				if (typeAndOperation.length == 2)
					this.annotTypeOperations.setProperty(typeAndOperation[0], typeAndOperation[1]);
			}
		} catch (IOException ioe) {}
		
		//	get region action provider
		if (this.parent == null)
			this.regionActionProvider = new RegionActionProvider();
		else this.regionActionProvider = ((RegionActionProvider) this.parent.getPlugin(RegionActionProvider.class.getName()));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getEditMenuItemNames()
	 */
	public String[] getEditMenuItemNames() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getToolsMenuItemNames()
	 */
	public String[] getToolsMenuItemNames() {
		String[] tmins = {TABLE_CLEANER_IMT_NAME};
		return tmins;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		if (TABLE_CLEANER_IMT_NAME.equals(name))
			return this.tableCleaner;
		else return null;
	}
	
	private class TableCleaner implements ImageMarkupTool {
		public String getLabel() {
			return "Clean Table Annotations";
		}
		public String getTooltip() {
			return "Clean annotations in tables, specifically ones spanning column breaks";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	we're only processing documents as a whole
			if (annot == null)
				cleanupTableAnnotations(doc, pm);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(final ImWord start, ImWord end, final ImDocumentMarkupPanel idmp) {
		
		//	anything to work with?
		if (!ImWord.TEXT_STREAM_TYPE_TABLE.equals(start.getTextStreamType()) || !start.getTextStreamId().equals(end.getTextStreamId()) || (start.pageId != end.pageId))
			return null;
		if (!idmp.areRegionsPainted(ImRegion.TABLE_TYPE))
			return null;
		
		//	get start table
		final ImRegion startTable = this.getTableAt(start);
		if (startTable == null) {
			System.out.println("Enclosing table not found");
			return null;
		}
		
		//	collect actions
		LinkedList actions = new LinkedList();
		
		//	offer merging rows across tables
		if (idmp.areRegionsPainted(ImRegion.TABLE_ROW_TYPE) && !startTable.hasAttribute("rowsContinueIn"))
			actions.add(new TwoClickSelectionAction("tableExtendRows", "Connect Table Rows", "Connect the rows in this table to those in another table, merging the tables left to right.") {
//				private ImWord artificialStartWord = null;
				public boolean performAction(ImWord secondWord) {
					return connectTableRows(startTable, secondWord);
				}
				public boolean performAction(ImPage secondPage, Point secondPoint) {
					return connectTableRows(startTable, secondPage, secondPoint);
				}
				public ImRegion getFirstRegion() {
					return startTable;
				}
//				public ImWord getFirstWord() {
//					if (this.artificialStartWord == null)
//						this.artificialStartWord = new ImWord(startTable.getDocument(), startTable.pageId, startTable.bounds, "TABLE");
//					return this.artificialStartWord;
//				}
				public String getActiveLabel() {
					return ("Click some word in the table to connect rows to");
				}
			});
		
		//	offer merging columns across tables
		if (idmp.areRegionsPainted(ImRegion.TABLE_COL_TYPE) && !startTable.hasAttribute("colsContinueIn"))
			actions.add(new TwoClickSelectionAction("tableExtendCols", "Connect Table Columns", "Connect the columns in this table to those in another table, merging the tables top to bottom.") {
//				private ImWord artificialStartWord = null;
				public boolean performAction(ImWord secondWord) {
					return connectTableCols(startTable, secondWord);
				}
				public boolean performAction(ImPage secondPage, Point secondPoint) {
					return connectTableCols(startTable, secondPage, secondPoint);
				}
				public ImRegion getFirstRegion() {
					return startTable;
				}
//				public ImWord getFirstWord() {
//					if (this.artificialStartWord == null)
//						this.artificialStartWord = new ImWord(startTable.getDocument(), startTable.pageId, startTable.bounds, "TABLE");
//					return this.artificialStartWord;
//				}
				public String getActiveLabel() {
					return ("Click some word in the table to connect columns to");
				}
			});
		
		//	offer dissecting as rows and columns
		if (startTable.hasAttribute("rowsContinueFrom") && startTable.hasAttribute("rowsContinueIn"))
			actions.add(new SelectionAction("tableCutRows", "Disconnect Table Rows", "Disconnect the rows in this table from the other tables they are connected to.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
//					return disconnectTableRows(idmp.document, startTable);
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Disconnect Table Rows");
					JMenuItem mi;
					mi = new JMenuItem("Left");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							disconnectTables(true, false, invoker);
						}
					});
					pm.add(mi);
					mi = new JMenuItem("Right");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							disconnectTables(false, true, invoker);
						}
					});
					pm.add(mi);
					mi = new JMenuItem("Left & Right");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							disconnectTables(true, true, invoker);
						}
					});
					pm.add(mi);
					return pm;
				}
				private void disconnectTables(boolean left, boolean right, ImDocumentMarkupPanel invoker) {
					invoker.beginAtomicAction("Disconnect Table Rows (" + ((left && right) ? "Left & Right" : (left ? "Left" : "Right")) + ")");
					disconnectTableRows(idmp.document, startTable, left, right);
					invoker.endAtomicAction();
					invoker.validate();
					invoker.repaint();
				}
			});
		else if (startTable.hasAttribute("rowsContinueFrom"))
			actions.add(new SelectionAction("tableCutRows", "Disconnect Table Rows (Left)", "Disconnect the rows in this table from the table to the left they are connected to.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return disconnectTableRows(idmp.document, startTable, true, false);
				}
			});
		else if (startTable.hasAttribute("rowsContinueIn"))
			actions.add(new SelectionAction("tableCutRows", "Disconnect Table Rows (Right)", "Disconnect the rows in this table from the table to the right they are connected to.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return disconnectTableRows(idmp.document, startTable, false, true);
				}
			});
		if (startTable.hasAttribute("colsContinueFrom") && startTable.hasAttribute("colsContinueIn"))
			actions.add(new SelectionAction("tableCutCols", "Disconnect Table Columns", "Disconnect the columns in this table from the other tables they are connected to.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
//					return disconnectTableCols(idmp.document, startTable);
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Disconnect Table Columns");
					JMenuItem mi;
					mi = new JMenuItem("Up");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							disconnectTables(true, false, invoker);
						}
					});
					pm.add(mi);
					mi = new JMenuItem("Down");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							disconnectTables(false, true, invoker);
						}
					});
					pm.add(mi);
					mi = new JMenuItem("Up & Down");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							disconnectTables(true, true, invoker);
						}
					});
					pm.add(mi);
					return pm;
				}
				private void disconnectTables(boolean up, boolean down, ImDocumentMarkupPanel invoker) {
					invoker.beginAtomicAction("Disconnect Table Columns (" + ((up && down) ? "Up & Down" : (up ? "Up" : "Down")) + ")");
					disconnectTableCols(idmp.document, startTable, up, down);
					invoker.endAtomicAction();
					invoker.validate();
					invoker.repaint();
				}
			});
		else if (startTable.hasAttribute("colsContinueFrom"))
			actions.add(new SelectionAction("tableCutCols", "Disconnect Table Columns", "Disconnect the columns in this table from the table above it they are connected to.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return disconnectTableCols(idmp.document, startTable, true, false);
				}
			});
		else if (startTable.hasAttribute("colsContinueIn"))
			actions.add(new SelectionAction("tableCutCols", "Disconnect Table Columns", "Disconnect the columns in this table from the table below it they are connected to.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return disconnectTableCols(idmp.document, startTable, false, true);
				}
			});
		
		//	offer assigning caption with second click
		if (!startTable.hasAttribute(ImRegion.IN_LINE_OBJECT_MARKER_ATTRIBUTE))
			actions.add(new TwoClickSelectionAction("assignCaptionTable", "Assign Caption", "Assign a caption to this table with a second click.") {
//				private ImWord artificialStartWord = null;
				public boolean performAction(ImWord secondWord) {
					return assignTableCaption(idmp.document, startTable, secondWord);
				}
				public boolean performAction(ImPage secondPage, Point secondPoint) {
					return false;
				}
				public ImRegion getFirstRegion() {
					return startTable;
				}
//				public ImWord getFirstWord() {
//					if (this.artificialStartWord == null)
//						this.artificialStartWord = new ImWord(startTable.getDocument(), startTable.pageId, startTable.bounds, "TABLE");
//					return this.artificialStartWord;
//				}
				public String getActiveLabel() {
					return ("Click on a caption to assign it to the table at " + startTable.bounds.toString() + " on page " + (startTable.pageId + 1));
				}
			});
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	private ImRegion getTableAt(ImWord imw) {
		ImRegion[] imwPageTables = imw.getPage().getRegions(ImRegion.TABLE_TYPE);
		if (imwPageTables.length == 0)
			return null;
		for (int t = 0; t < imwPageTables.length; t++) {
			if (imwPageTables[t].bounds.includes(imw.bounds, false))
				return imwPageTables[t];
		}
		return null;
	}
	private ImRegion getTableAt(ImPage page, Point point) {
		ImRegion[] pageTables = page.getRegions(ImRegion.TABLE_TYPE);
		if (pageTables.length == 0)
			return null;
		for (int t = 0; t < pageTables.length; t++) {
			if (pageTables[t].bounds.includes(point))
				return pageTables[t];
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#typeChanged(de.uka.ipd.idaho.im.ImObject, java.lang.String, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void typeChanged(ImObject object, String oldType, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#attributeChanged(de.uka.ipd.idaho.im.ImObject, java.lang.String, java.lang.Object, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void attributeChanged(ImObject object, String attributeName, Object oldValue, ImDocumentMarkupPanel idmp, boolean allowPrompt) {
		
		//	we're only interested in chunks of text cut off table
		if (!(object instanceof ImWord) || !ImWord.TEXT_STREAM_TYPE_ATTRIBUTE.equals(attributeName) || !ImWord.TEXT_STREAM_TYPE_TABLE.equals(oldValue))
			return;
		
		//	we're not interested in deletions
		if (ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(object.getAttribute(attributeName)) || ImWord.TEXT_STREAM_TYPE_DELETED.equals(object.getAttribute(attributeName)))
			return;
		
		//	get word and page
		ImWord word = ((ImWord) object);
		ImPage wordPage = word.getPage();
		if (wordPage == null)
			return;
		
		//	get table word lies in
		ImRegion[] wordTable = getRegionsOverlapping(wordPage, word.bounds, ImRegion.TABLE_TYPE);
		if (wordTable.length != 1)
			return;
		
		//	get whole text stream
		ImWord streamStartWord = word;
		while (streamStartWord.getPreviousWord() != null) {
			streamStartWord = streamStartWord.getPreviousWord();
			if (streamStartWord.pageId != word.pageId)
				return;
		}
		ImWord streamEndWord = word;
		while (streamEndWord.getNextWord() != null) {
			streamEndWord = streamEndWord.getNextWord();
			if (streamEndWord.pageId != word.pageId)
				return;
		}
		
		//	get bounds of text stream
		ArrayList streamWords = new ArrayList();
		for (ImWord imw = streamStartWord; imw != null; imw = imw.getNextWord())
			streamWords.add(imw);
		BoundingBox streamBounds = ImLayoutObject.getAggregateBox((ImWord[]) streamWords.toArray(new ImWord[streamWords.size()]));
		if (!wordTable[0].bounds.overlaps(streamBounds))
			return;
		if (wordTable[0].bounds.liesIn(streamBounds, true))
			return;
		
		//	get bounds of remaining table
		ImWord[] tableWords = wordPage.getWordsInside(wordTable[0].bounds);
		ArrayList cutTableWords = new ArrayList();
		for (int w = 0; w < tableWords.length; w++) {
			if (!streamBounds.includes(tableWords[w].bounds, true))
				cutTableWords.add(tableWords[w]);
		}
		if (cutTableWords.isEmpty())
			return;
		BoundingBox cutTableBounds = ImLayoutObject.getAggregateBox((ImWord[]) cutTableWords.toArray(new ImWord[cutTableWords.size()]));
		if (cutTableBounds.overlaps(streamBounds))
			return;
		
		//	cut marked stream off table, but don't revert it to main text
		this.cutTable(wordPage, wordTable[0], cutTableBounds, streamBounds, null, idmp);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#regionAdded(de.uka.ipd.idaho.im.ImRegion, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void regionAdded(ImRegion region, ImDocumentMarkupPanel idmp, boolean allowPrompt) {
		if (!ImRegion.TABLE_TYPE.equals(region.getType()))
			return;
		
		//	get potential captions
		ImAnnotation[] captionAnnots = ImUtils.findCaptions(region, true, true, true);
		
		//	try setting attributes in unassigned captions first
		for (int a = 0; a < captionAnnots.length; a++) {
			if (captionAnnots[a].hasAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE) || captionAnnots[a].hasAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE))
				continue;
			captionAnnots[a].setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + region.pageId));
			captionAnnots[a].setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, region.bounds.toString());
			captionAnnots[a].setAttribute(ImAnnotation.CAPTION_TARGET_IS_TABLE_ATTRIBUTE);
			return;
		}
		
		//	set attributes in any caption (happens if user corrects, for instance)
		if (captionAnnots.length != 0) {
			captionAnnots[0].setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + region.pageId));
			captionAnnots[0].setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, region.bounds.toString());
			captionAnnots[0].setAttribute(ImAnnotation.CAPTION_TARGET_IS_TABLE_ATTRIBUTE);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#regionRemoved(de.uka.ipd.idaho.im.ImRegion, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void regionRemoved(ImRegion region, ImDocumentMarkupPanel idmp, boolean allowPrompt) {
		
		//	we're only interested in table removals
		if (!ImRegion.TABLE_TYPE.equals(region.getType()))
			return;
		
		//	clean up table structure
		ImPage page = idmp.document.getPage(region.pageId);
		ImRegion[] subRegions = page.getRegionsInside(region.bounds, true);
		for (int r = 0; r < subRegions.length; r++) {
			if (ImRegion.TABLE_CELL_TYPE.equals(subRegions[r].getType()) || ImRegion.TABLE_COL_TYPE.equals(subRegions[r].getType()) || ImRegion.TABLE_ROW_TYPE.equals(subRegions[r].getType()))
				page.removeRegion(subRegions[r]);
		}
		
		//	this one is an internal removal, leave text streams alone
		if (region.hasAttribute("ignoreWordCleanup"))
			return;
		
		//	revert former table words to main text stream
		ImWord[] words = page.getWordsInside(region.bounds);
		if (words.length != 0) {
			ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
			Arrays.sort(words, ImUtils.textStreamOrder);
			if (words[0].getPreviousWord() == null)
				words[0].setTextStreamType(ImWord.TEXT_STREAM_TYPE_MAIN_TEXT);
			this.regionActionProvider.markBlock(page, ImLayoutObject.getAggregateBox(words));
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#annotationAdded(de.uka.ipd.idaho.im.ImAnnotation, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void annotationAdded(ImAnnotation annotation, ImDocumentMarkupPanel idmp, boolean allowPrompt) {
		
		//	we're only interested in tables
		if (!ImWord.TEXT_STREAM_TYPE_TABLE.equals(annotation.getFirstWord().getTextStreamType()))
			return;
		
		//	single-word annotation cannot span multiple table cells
		if (annotation.getFirstWord() == annotation.getLastWord())
			return;
		
		//	annotation spanning two pages, none of out business here
		if (annotation.getFirstWord().pageId != annotation.getLastWord().pageId)
			return;
		
		//	get document and page
		ImDocument doc = annotation.getDocument();
		if (doc == null)
			return;
		ImPage page = doc.getPage(annotation.getFirstWord().pageId);
		if (page == null)
			return;
		
		//	get and sort table cells
		ImRegion[] cells = page.getRegions(ImRegion.TABLE_CELL_TYPE);
		Arrays.sort(cells, ImUtils.leftRightOrder);
		Arrays.sort(cells, ImUtils.topDownOrder);
		
		//	index cells by words
		Map wordsToCells = new HashMap();
		ImRegion wordCell = null;
		for (ImWord imw = annotation.getFirstWord(); imw != null; imw = imw.getNextWord()) {
			if ((wordCell != null) && (wordCell.bounds.includes(imw.bounds, true)))
				wordsToCells.put(imw, wordCell);
			else for (int c = 0; c < cells.length; c++)
				if (cells[c].bounds.includes(imw.bounds, true)) {
					wordCell = cells[c];
					wordsToCells.put(imw, wordCell);
					break;
				}
			if (imw == annotation.getLastWord())
				break;
		}
		
		//	mark parts of original annotation in individual cells
		this.cleanupTableAnnotation(doc, annotation, wordsToCells);
		
		//	perform document annotation cleanup to deal with whatever duplicates we might have incurred
		doc.cleanupAnnotations();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#annotationRemoved(de.uka.ipd.idaho.im.ImAnnotation, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void annotationRemoved(ImAnnotation annotation, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	private void cleanupTableAnnotations(ImDocument doc, ProgressMonitor pm) {
		
		//	clean up tables page by page
		ImPage[] pages = doc.getPages();
		for (int p = 0; p < pages.length; p++) {
			pm.setProgress((p * 100) / pages.length);
			ImRegion[] pageTables = pages[p].getRegions(ImRegion.TABLE_TYPE);
			if (pageTables.length == 0)
				continue;
			for (int t = 0; t < pageTables.length; t++)
				this.cleanupTableAnnotations(doc, pageTables[t]);
		}
		
		//	perform document annotation cleanup to deal with whatever duplicates we might have incurred
		doc.cleanupAnnotations();
	}
	
	/**
	 * Clean up the annotations in a table, i.e., make sure they do not cross
	 * cell boundaries.
	 * @param doc the document the table belongs to
	 * @param table the table to clean up
	 */
	public void cleanupTableAnnotations(ImDocument doc, ImRegion table) {
		
		//	get annotations lying inside table
		ImAnnotation[] annots = doc.getAnnotations(table.pageId);
		ArrayList annotList = new ArrayList();
		for (int a = 0; a < annots.length; a++) {
			if (!ImWord.TEXT_STREAM_TYPE_TABLE.equals(annots[a].getFirstWord().getTextStreamType()))
				continue;
			if (table.pageId != annots[a].getFirstWord().pageId)
				continue;
			if (table.pageId != annots[a].getLastWord().pageId)
				continue;
			if (!table.bounds.includes(annots[a].getFirstWord().bounds, true))
				continue;
			if (!table.bounds.includes(annots[a].getLastWord().bounds, true))
				continue;
			annotList.add(annots[a]);
		}
		if (annotList.isEmpty())
			return;
		
		//	get and sort table cells
		ImRegion[] cells = table.getRegions(ImRegion.TABLE_CELL_TYPE);
		if (cells.length == 0)
			return;
		Arrays.sort(cells, ImUtils.leftRightOrder);
		Arrays.sort(cells, ImUtils.topDownOrder);
		
		//	get table words
		ImWord[] words = table.getWords();
		if (words.length == 0)
			return;
		Arrays.sort(words, ImUtils.textStreamOrder);
		
		//	index cells by words
		Map wordsToCells = new HashMap();
		ImRegion wordCell = null;
		for (int w = 0; w < words.length; w++) {
			if ((wordCell != null) && (wordCell.bounds.includes(words[w].bounds, true)))
				wordsToCells.put(words[w], wordCell);
			else for (int c = 0; c < cells.length; c++)
				if (cells[c].bounds.includes(words[w].bounds, true)) {
					wordCell = cells[c];
					wordsToCells.put(words[w], wordCell);
					break;
				}
		}
		
		//	mark parts of original annotation in individual cells
		for (int a = 0; a < annotList.size(); a++)
			this.cleanupTableAnnotation(doc, ((ImAnnotation) annotList.get(a)), wordsToCells);
		
		//	perform document annotation cleanup to deal with whatever duplicates we might have incurred
		doc.cleanupAnnotations();
	}
	
	private void cleanupTableAnnotation(ImDocument doc, ImAnnotation annotation, Map wordsToCells) {
		
		//	single-cell annotation, nothing to chop
		if (wordsToCells.get(annotation.getFirstWord()) == wordsToCells.get(annotation.getLastWord()))
			return;
		
		//	get cleanup operation, and behave accordingly
		String annotTypeOperation = this.annotTypeOperations.getProperty(annotation.getType(), SPLIT_OPERATION);
		
		//	remove annotation (happens below)
		if (REMOVE_OPERATION.equals(annotTypeOperation)) {}
		
		//	cut at end of start cell
		else if (CUT_TO_START_OPERATION.equals(annotTypeOperation)) {
			ImRegion annotStartCell = ((ImRegion) wordsToCells.get(annotation.getFirstWord()));
			for (ImWord imw = annotation.getFirstWord(); imw != null; imw = imw.getNextWord())
				if (wordsToCells.get(imw) != annotStartCell) {
					annotation.setLastWord(imw.getPreviousWord());
					break;
				}
		}
		
		//	cut at start of end cell
		else if (CUT_TO_END_OPERATION.equals(annotTypeOperation)) {
			ImRegion annotEndCell = ((ImRegion) wordsToCells.get(annotation.getLastWord()));
			for (ImWord imw = annotation.getLastWord(); imw != null; imw = imw.getPreviousWord())
				if (wordsToCells.get(imw) != annotEndCell) {
					annotation.setFirstWord(imw.getNextWord());
					break;
				}
		}
		
		//	split up to individual cells
		else if (SPLIT_OPERATION.equals(annotTypeOperation)) {
			ImRegion annotCell = ((ImRegion) wordsToCells.get(annotation.getFirstWord()));
			ImAnnotation annot = doc.addAnnotation(annotation.getFirstWord(), annotation.getType());
			annot.copyAttributes(annotation);
			for (ImWord imw = annotation.getFirstWord(); imw != null; imw = imw.getNextWord()) {
				if (wordsToCells.get(imw) != annotCell) {
					annot.setLastWord(imw.getPreviousWord());
					annot = doc.addAnnotation(imw, annotation.getType());
					annot.copyAttributes(annotation);
					annotCell = ((ImRegion) wordsToCells.get(imw));
				}
				if (imw == annotation.getLastWord()) {
					annot.setLastWord(imw);
					break;
				}
			}
		}
		
		//	clean up original annotation
		doc.removeAnnotation(annotation);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, ImPage page, ImDocumentMarkupPanel idmp) {
		
		//	mark selection
		BoundingBox selectedBox = new BoundingBox(Math.min(start.x, end.x), Math.max(start.x, end.x), Math.min(start.y, end.y), Math.max(start.y, end.y));
		
		//	get selected words
		ImWord[] selectedWords = page.getWordsInside(selectedBox);
		
		//	get selected columns and rows before shrinking selection (they can be more than half empty so center point ends up outside word box ...)
		ImRegion[] selectedCols = null;
		if ((idmp != null) && idmp.areRegionsPainted(ImRegion.TABLE_COL_TYPE))
			selectedCols = getRegionsInside(page, selectedBox, ImRegion.TABLE_COL_TYPE, true);
		ImRegion[] selectedRows = null;
		if ((idmp != null) && idmp.areRegionsPainted(ImRegion.TABLE_ROW_TYPE))
			selectedRows = getRegionsInside(page, selectedBox, ImRegion.TABLE_ROW_TYPE, true);
		
		
		//	get selected cells before shrinking selection (they are the one region type that can be empty ...)
		ImRegion[] selectedCells = null;
		if ((idmp != null) && idmp.areRegionsPainted(ImRegion.TABLE_CELL_TYPE))
			selectedCells = getRegionsInside(page, selectedBox, ImRegion.TABLE_CELL_TYPE, true);
		
		//	shrink bounding box to words
		if (selectedWords.length != 0) {
			int tbLeft = selectedBox.right;
			int tbRight = selectedBox.left;
			int tbTop = selectedBox.bottom;
			int tbBottom = selectedBox.top;
			for (int w = 0; w < selectedWords.length; w++) {
				tbLeft = Math.min(tbLeft, selectedWords[w].bounds.left);
				tbRight = Math.max(tbRight, selectedWords[w].bounds.right);
				tbTop = Math.min(tbTop, selectedWords[w].bounds.top);
				tbBottom = Math.max(tbBottom, selectedWords[w].bounds.bottom);
			}
			selectedBox = new BoundingBox(tbLeft, tbRight, tbTop, tbBottom);
		}
		
		//	return actions if selection not empty
		return this.getActions(page, selectedWords, selectedBox, selectedCols, selectedRows, selectedCells, idmp);
	}
	
	private SelectionAction[] getActions(final ImPage page, final ImWord[] selWords, final BoundingBox selWordsBox, final ImRegion[] selCols, final ImRegion[] selRows, final ImRegion[] selCells, final ImDocumentMarkupPanel idmp) {
		LinkedList actions = new LinkedList();
		
		//	get table(s) overlapping selection, even without center
		final ImRegion[] selTables = getRegionsOverlapping(page, selWordsBox, ImRegion.TABLE_TYPE);
		
		//	if multiple tables selected, offer merging
		if (selTables.length > 1) {
			actions.add(new SelectionAction("mergeTable", "Merge Tables", "Merge the selected tables into one.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return mergeTables(page, selTables, idmp);
				}
			});
			
			//	little else to do ...
			return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
		}
		
		//	no table selected
		if (selTables.length == 0) {
			
			//	offer marking selected words as a table and analyze table structure
			if (selWords.length != 0) {
				actions.add(new SelectionAction("markRegionTable", "Mark Table", "Mark selected words as a table and analyze table structure.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return markTable(page, selWords, selWordsBox, null, false, invoker);
					}
				});
				actions.add(new SelectionAction("markRegionTable", "Mark In-Line Table", "Mark selected words as an in-line table and analyze table structure.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return markTable(page, selWords, selWordsBox, null, true, invoker);
					}
				});
			}
			
			//	little else to do ...
			return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
		}
		
		//	get all table words
		final ImWord[] tableWords = selTables[0].getWords();
		
		//	sort selected words inside and outside table
		ArrayList selTableWordList = new ArrayList();
		ArrayList selNonTableWordwList = new ArrayList();
		for (int w = 0; w < selWords.length; w++) {
			if (selTables[0].bounds.includes(selWords[w].bounds, true))
				selTableWordList.add(selWords[w]);
			else selNonTableWordwList.add(selWords[w]);
		}
		final ImWord[] selTableWords = ((ImWord[]) selTableWordList.toArray(new ImWord[selTableWordList.size()]));
		final ImWord[] selNonTableWords = ((ImWord[]) selNonTableWordwList.toArray(new ImWord[selNonTableWordwList.size()]));
		
		//	check if selected table words are at some edge, i.e., there are no non-selected table words in the respective direction of the selected table words
		BoundingBox hStripe = new BoundingBox(selTables[0].bounds.left, selTables[0].bounds.right, Math.max(selWordsBox.top, selTables[0].bounds.top), Math.min(selWordsBox.bottom, selTables[0].bounds.bottom));
		ImWord[] hStripeWords = page.getWordsInside(hStripe);
		boolean leftEdge = true;
		boolean rightEdge = true;
		for (int w = 0; w < hStripeWords.length; w++) {
			if (hStripeWords[w].centerX < selWordsBox.left)
				leftEdge = false;
			if (hStripeWords[w].centerX >= selWordsBox.right)
				rightEdge = false;
		}
		boolean topTableEdgeSelected = false;
		boolean bottomTableEdgeSelected = false;
		if (leftEdge && rightEdge) {
			topTableEdgeSelected = true;
			bottomTableEdgeSelected = true;
			for (int w = 0; w < tableWords.length; w++) {
				if (tableWords[w].centerY < hStripe.top)
					topTableEdgeSelected = false;
				if (tableWords[w].centerY >= hStripe.bottom)
					bottomTableEdgeSelected = false;
			}
		}
		BoundingBox vStripe = new BoundingBox(Math.max(selWordsBox.left, selTables[0].bounds.left), Math.min(selWordsBox.right, selTables[0].bounds.right), selTables[0].bounds.top, selTables[0].bounds.bottom);
		ImWord[] vStripeWords = page.getWordsInside(vStripe);
		boolean topEdge = true;
		boolean bottomEdge = true;
		for (int w = 0; w < vStripeWords.length; w++) {
			if (vStripeWords[w].centerY < selWordsBox.top)
				topEdge = false;
			if (vStripeWords[w].centerY >= selWordsBox.bottom)
				bottomEdge = false;
		}
		boolean leftTableEdgeSelected = false;
		boolean rightTableEdgeSelected = false;
		if (topEdge && bottomEdge) {
			leftTableEdgeSelected = true;
			rightTableEdgeSelected = true;
			for (int w = 0; w < tableWords.length; w++) {
				if (tableWords[w].centerX < vStripe.left)
					leftTableEdgeSelected = false;
				if (tableWords[w].centerX >= hStripe.right)
					rightTableEdgeSelected = false;
			}
		}
		final BoundingBox cutTableBox;
		final BoundingBox cutOffTableBox;
		final boolean cutFromTable;
		if ((selTableWords.length != 0) && (selNonTableWords.length == 0) && ((topTableEdgeSelected != bottomTableEdgeSelected) || (leftTableEdgeSelected != rightTableEdgeSelected))) {
			if (leftTableEdgeSelected) {
				if (selWordsBox.right < ((selTables[0].bounds.left + selTables[0].bounds.right) / 2)) {
					cutTableBox = new BoundingBox(selWordsBox.right, selTables[0].bounds.right, selTables[0].bounds.top, selTables[0].bounds.bottom);
					cutOffTableBox = new BoundingBox(selTables[0].bounds.left, selWordsBox.right, selTables[0].bounds.top, selTables[0].bounds.bottom);
					cutFromTable = true;
				}
				else {
					cutTableBox = new BoundingBox(selTables[0].bounds.left, selWordsBox.right, selTables[0].bounds.top, selTables[0].bounds.bottom);
					cutOffTableBox = new BoundingBox(selWordsBox.right, selTables[0].bounds.right, selTables[0].bounds.top, selTables[0].bounds.bottom);
					cutFromTable = false;
				}
			}
			else if (rightTableEdgeSelected) {
				if (selWordsBox.left > ((selTables[0].bounds.left + selTables[0].bounds.right) / 2)) {
					cutTableBox = new BoundingBox(selTables[0].bounds.left, selWordsBox.left, selTables[0].bounds.top, selTables[0].bounds.bottom);
					cutOffTableBox = new BoundingBox(selWordsBox.left, selTables[0].bounds.right, selTables[0].bounds.top, selTables[0].bounds.bottom);
					cutFromTable = true;
				}
				else {
					cutTableBox = new BoundingBox(selWordsBox.left, selTables[0].bounds.right, selTables[0].bounds.top, selTables[0].bounds.bottom);
					cutOffTableBox = new BoundingBox(selTables[0].bounds.left, selWordsBox.left, selTables[0].bounds.top, selTables[0].bounds.bottom);
					cutFromTable = false;
				}
			}
			else if (topTableEdgeSelected) {
				if (selWordsBox.bottom < ((selTables[0].bounds.top + selTables[0].bounds.bottom) / 2)) {
					cutTableBox = new BoundingBox(selTables[0].bounds.left, selTables[0].bounds.right, selWordsBox.bottom, selTables[0].bounds.bottom);
					cutOffTableBox = new BoundingBox(selTables[0].bounds.left, selTables[0].bounds.right, selTables[0].bounds.top, selWordsBox.bottom);
					cutFromTable = true;
				}
				else {
					cutTableBox = new BoundingBox(selTables[0].bounds.left, selTables[0].bounds.right, selTables[0].bounds.top, selWordsBox.bottom);
					cutOffTableBox = new BoundingBox(selTables[0].bounds.left, selTables[0].bounds.right, selWordsBox.bottom, selTables[0].bounds.bottom);
					cutFromTable = false;
				}
			}
			else if (bottomTableEdgeSelected) {
				if (selWordsBox.top > ((selTables[0].bounds.top + selTables[0].bounds.bottom) / 2)) {
					cutTableBox = new BoundingBox(selTables[0].bounds.left, selTables[0].bounds.right, selTables[0].bounds.top, selWordsBox.top);
					cutOffTableBox = new BoundingBox(selTables[0].bounds.left, selTables[0].bounds.right, selWordsBox.top, selTables[0].bounds.bottom);
					cutFromTable = true;
				}
				else {
					cutTableBox = new BoundingBox(selTables[0].bounds.left, selTables[0].bounds.right, selWordsBox.top, selTables[0].bounds.bottom);
					cutOffTableBox = new BoundingBox(selTables[0].bounds.left, selTables[0].bounds.right, selTables[0].bounds.top, selWordsBox.top);
					cutFromTable = false;
				}
			}
			else {
				cutTableBox = null;
				cutOffTableBox = null;
				cutFromTable = false;
			}
		}
		else {
			cutTableBox = null;
			cutOffTableBox = null;
			cutFromTable = false;
		}
		
		//	working on table rows
		if ((selRows != null) && (selWords.length != 0)) {
			final ImRegion[] partSelRows = getRegionsIncluding(page, selWordsBox, ImRegion.TABLE_ROW_TYPE, true);
			
			//	if multiple table row regions selected, offer merging them, and cells along the way
			if (selRows.length > 1)
				actions.add(new SelectionAction("tableMergeRows", "Merge Table Rows", "Merge selected table rows.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return mergeTableRows(selTables[0], selRows);
					}
				});
			
			//	if only part of one row selected, offer splitting row
			else if (partSelRows.length == 1) {
				actions.add(new SelectionAction("tableSplitRow", "Split Table Row", "Split selected table row.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return splitTableRow(selTables[0], partSelRows[0], selWordsBox);
					}
				});
				if (!selTables[0].hasAttribute("rowsContinueFrom") && !selTables[0].hasAttribute("rowsContinueIn"))
					actions.add(new SelectionAction("tableSplitRow", "Split Table Row to Lines", "Split selected table row into individual lines.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return splitTableRowToLines(page, selTables[0], partSelRows[0]);
						}
					});
			}
		}
		
		//	working on table columns
		if ((selCols != null) && (selWords.length != 0)) {
			final ImRegion[] partSelCols = getRegionsIncluding(page, selWordsBox, ImRegion.TABLE_COL_TYPE, true);
			
			//	if multiple table column regions selected, offer merging them, and cells along the way
			if (selCols.length > 1)
				actions.add(new SelectionAction("tableMergeColumns", "Merge Table Columns", "Merge selected table columns.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return mergeTableCols(selTables[0], selCols);
					}
				});
			
			//	if only part of one column selected, offer splitting column
			else if (partSelCols.length == 1)
				actions.add(new SelectionAction("tableSplitColumns", "Split Table Column", "Split selected table column.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return splitTableCol(page, selTables[0], partSelCols[0], selWordsBox);
					}
				});
		}
		
		//	working on table cells
		if ((selCells != null) && (selWords.length != 0)) {
			final ImRegion[] partSelCells = getRegionsIncluding(page, selWordsBox, ImRegion.TABLE_CELL_TYPE, true);
			
			//	if multiple table cell regions selected, offer merging them
			if (selCells.length > 1)
				actions.add(new SelectionAction("tableMergeCells", "Merge Table Cells", "Merge selected table cells.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return mergeTableCells(selTables[0], selCells);
					}
				});
			
			//	if only part of one cell selected, offer splitting cell
			else if (partSelCells.length == 1) {
				final ImRegion[] partSelRows = getRegionsOverlapping(page, selWordsBox, ImRegion.TABLE_ROW_TYPE);
				final ImRegion[] selCellRows = getRegionsOverlapping(page, partSelCells[0].bounds, ImRegion.TABLE_ROW_TYPE);
				final ImRegion[] partSelCols = getRegionsOverlapping(page, selWordsBox, ImRegion.TABLE_COL_TYPE);
				final ImRegion[] selCellCols = getRegionsOverlapping(page, partSelCells[0].bounds, ImRegion.TABLE_COL_TYPE);
				if ((partSelRows.length != 0) && (partSelRows.length < selCellRows.length))
					actions.add(new SelectionAction("tableSplitCells", "Split Table Cell (Rows)", "Split selected table cell horizontally.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return splitTableCellRows(selTables[0], partSelCells[0], selCellRows, selCellCols, selWordsBox);
						}
					});
				if ((partSelCols.length != 0) && (partSelCols.length < selCellCols.length))
					actions.add(new SelectionAction("tableSplitCells", "Split Table Cell (Columns)", "Split selected table cell vertically.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return splitTableCellCols(selTables[0], partSelCells[0], selCellRows, selCellCols, selWordsBox);
						}
					});
			}
		}
		
		//	reduce size of table
		if ((cutTableBox != null) && (cutOffTableBox != null))
			actions.add(new SelectionAction("tableCut", "Cut Table", (cutFromTable ? "Cut selected words off table." : "Cut table to selected words.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return cutTable(page, selTables[0], cutTableBox, cutOffTableBox, ImWord.TEXT_STREAM_TYPE_MAIN_TEXT, idmp);
				}
			});
		
		//	extend table
		if (selNonTableWords.length != 0)
			actions.add(new SelectionAction("tableExtend", "Extend Table", "Extend table to include all selected words.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return extendTable(page, selTables[0], selNonTableWords, idmp);
				}
			});
		
		//	update table structure after manual modifications
		actions.add(new SelectionAction("tableUpdate", "Update Table", "Update table to reflect manual modifications.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				ImUtils.orderTableWords(markTableCells(page, selTables[0]));
				cleanupTableAnnotations(page.getDocument(), selTables[0]);
				return true;
			}
		});
		
		
		//	offer adjusting block text direction
		if (idmp.areTextStreamsPainted())
			actions.add(new SelectionAction("textDirectionTable", "Set Table Text Direction", "Adjust the (general, predominant) text direction of the table.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					String tableTextDirection = ((String) selTables[0].getAttribute(ImRegion.TEXT_DIRECTION_ATTRIBUTE, ImRegion.TEXT_DIRECTION_LEFT_RIGHT));
					JMenu pm = new JMenu("Table Text Direction");
					ButtonGroup bg = new ButtonGroup();
					final JMenuItem lrmi = new JRadioButtonMenuItem("Left-Right", ImRegion.TEXT_DIRECTION_LEFT_RIGHT.equals(tableTextDirection));
					lrmi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (lrmi.isSelected())
								setTextDirection(ImRegion.TEXT_DIRECTION_LEFT_RIGHT, invoker);
						}
					});
					pm.add(lrmi);
					bg.add(lrmi);
					final JMenuItem bumi = new JRadioButtonMenuItem("Bottom-Up", ImRegion.TEXT_DIRECTION_BOTTOM_UP.equals(tableTextDirection));
					bumi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (bumi.isSelected())
								setTextDirection(ImRegion.TEXT_DIRECTION_BOTTOM_UP, invoker);
						}
					});
					pm.add(bumi);
					bg.add(bumi);
					final JMenuItem tdmi = new JRadioButtonMenuItem("Top-Down", ImRegion.TEXT_DIRECTION_TOP_DOWN.equals(tableTextDirection));
					tdmi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (tdmi.isSelected())
								setTextDirection(ImRegion.TEXT_DIRECTION_TOP_DOWN, invoker);
						}
					});
					pm.add(tdmi);
					bg.add(tdmi);
					final JMenuItem udmi = new JRadioButtonMenuItem("Right-Left & Upside-Down", ImRegion.TEXT_DIRECTION_RIGHT_LEFT_UPSIDE_DOWN.equals(tableTextDirection));
					udmi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (udmi.isSelected())
								setTextDirection(ImRegion.TEXT_DIRECTION_RIGHT_LEFT_UPSIDE_DOWN, invoker);
						}
					});
					pm.add(udmi);
					bg.add(udmi);
					pm.setToolTipText(this.tooltip);
					Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.TABLE_TYPE);
					if (regionTypeColor != null) {
						pm.setOpaque(true);
						pm.setBackground(regionTypeColor);
					}
					return pm;
				}
				private void setTextDirection(String textDirection, ImDocumentMarkupPanel invoker) {
					invoker.beginAtomicAction(this.label);
					TableActionProvider.this.setTextDirection(selTables[0], textDirection);
					invoker.endAtomicAction();
					invoker.validate();
					invoker.repaint();
				}
			});
		
		//	offer assigning caption with second click
		if (!selTables[0].hasAttribute(ImRegion.IN_LINE_OBJECT_MARKER_ATTRIBUTE))
			actions.add(new TwoClickSelectionAction("assignCaptionTable", "Assign Caption", "Assign a caption to this table with a second click.") {
				public boolean performAction(ImWord secondWord) {
					return assignTableCaption(idmp.document, selTables[0], secondWord);
				}
				public boolean performAction(ImPage secondPage, Point secondPoint) {
					return false;
				}
				public ImRegion getFirstRegion() {
					return selTables[0];
				}
				public String getActiveLabel() {
					return ("Click on a caption to assign it to the table at " + selTables[0].bounds.toString() + " on page " + (selTables[0].pageId + 1));
				}
			});
		
		//	offer merging rows across tables
		if (idmp.areRegionsPainted(ImRegion.TABLE_ROW_TYPE) && !selTables[0].hasAttribute("rowsContinueIn"))
			actions.add(new TwoClickSelectionAction("tableExtendRows", "Connect Table Rows (Right)", "Connect the rows in this table to those in another table, merging the tables left to right.") {
				public boolean performAction(ImWord secondWord) {
					return connectTableRows(selTables[0], secondWord);
				}
				public boolean performAction(ImPage secondPage, Point secondPoint) {
					return connectTableRows(selTables[0], secondPage, secondPoint);
				}
				public ImRegion getFirstRegion() {
					return selTables[0];
				}
				public String getActiveLabel() {
					return ("Click some word in the table to connect rows to");
				}
			});
		
		//	offer merging columns across tables
		if (idmp.areRegionsPainted(ImRegion.TABLE_COL_TYPE) && !selTables[0].hasAttribute("colsContinueIn"))
			actions.add(new TwoClickSelectionAction("tableExtendCols", "Connect Table Columns (Down)", "Connect the columns in this table to those in another table, merging the tables top to bottom.") {
				public boolean performAction(ImWord secondWord) {
					return connectTableCols(selTables[0], secondWord);
				}
				public boolean performAction(ImPage secondPage, Point secondPoint) {
					return connectTableCols(selTables[0], secondPage, secondPoint);
				}
				public ImRegion getFirstRegion() {
					return selTables[0];
				}
				public String getActiveLabel() {
					return ("Click some word in the table to connect columns to");
				}
			});
		
		//	offer dissecting as rows and columns TODO distinguish upward cut and downward cut, distinguish leftward and rightward cut
		if (selTables[0].hasAttribute("rowsContinueFrom") && selTables[0].hasAttribute("rowsContinueIn"))
			actions.add(new SelectionAction("tableCutRows", "Disconnect Table Rows", "Disconnect the rows in this table from the other tables they are connected to.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
//					return disconnectTableRows(idmp.document, selTables[0]);
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Disconnect Table Rows");
					JMenuItem mi;
					mi = new JMenuItem("Left");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							disconnectTables(true, false, invoker);
						}
					});
					pm.add(mi);
					mi = new JMenuItem("Right");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							disconnectTables(false, true, invoker);
						}
					});
					pm.add(mi);
					mi = new JMenuItem("Left & Right");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							disconnectTables(true, true, invoker);
						}
					});
					pm.add(mi);
					return pm;
				}
				private void disconnectTables(boolean left, boolean right, ImDocumentMarkupPanel invoker) {
					invoker.beginAtomicAction("Disconnect Table Rows (" + ((left && right) ? "Left & Right" : (left ? "Left" : "Right")) + ")");
					disconnectTableRows(idmp.document, selTables[0], left, right);
					invoker.endAtomicAction();
					invoker.validate();
					invoker.repaint();
				}
			});
		else if (selTables[0].hasAttribute("rowsContinueFrom"))
			actions.add(new SelectionAction("tableCutRows", "Disconnect Table Rows (Left)", "Disconnect the rows in this table from the table to the left they are connected to.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return disconnectTableRows(idmp.document, selTables[0], true, false);
				}
			});
		else if (selTables[0].hasAttribute("rowsContinueIn"))
			actions.add(new SelectionAction("tableCutRows", "Disconnect Table Rows (Right)", "Disconnect the rows in this table from the table to the right they are connected to.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return disconnectTableRows(idmp.document, selTables[0], false, true);
				}
			});
		if (selTables[0].hasAttribute("colsContinueFrom") && selTables[0].hasAttribute("colsContinueIn"))
			actions.add(new SelectionAction("tableCutCols", "Disconnect Table Columns", "Disconnect the columns in this table from the other tables they are connected to.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
//					return disconnectTableCols(idmp.document, selTables[0]);
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Disconnect Table Columns");
					JMenuItem mi;
					mi = new JMenuItem("Up");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							disconnectTables(true, false, invoker);
						}
					});
					pm.add(mi);
					mi = new JMenuItem("Down");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							disconnectTables(false, true, invoker);
						}
					});
					pm.add(mi);
					mi = new JMenuItem("Up & Down");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							disconnectTables(true, true, invoker);
						}
					});
					pm.add(mi);
					return pm;
				}
				private void disconnectTables(boolean up, boolean down, ImDocumentMarkupPanel invoker) {
					invoker.beginAtomicAction("Disconnect Table Columns (" + ((up && down) ? "Up & Down" : (up ? "Up" : "Down")) + ")");
					disconnectTableCols(idmp.document, selTables[0], up, down);
					invoker.endAtomicAction();
					invoker.validate();
					invoker.repaint();
				}
			});
		else if (selTables[0].hasAttribute("colsContinueFrom"))
			actions.add(new SelectionAction("tableCutCols", "Disconnect Table Columns (Up)", "Disconnect the columns in this table from the table above it they are connected to.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return disconnectTableCols(idmp.document, selTables[0], true, false);
				}
			});
		else if (selTables[0].hasAttribute("colsContinueIn"))
			actions.add(new SelectionAction("tableCutCols", "Disconnect Table Columns (Down)", "Disconnect the columns in this table from the table below it they are connected to.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return disconnectTableCols(idmp.document, selTables[0], false, true);
				}
			});
		
		//	offer exporting tables as CSV or TSV
		actions.add(new SelectionAction("tableCopySingle", "Copy Table Data", "Copy the data in the table to the clipboard.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				return false;
			}
			public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
				JMenu pm = new JMenu("Copy Table Data ...");
				JMenuItem mi;
				mi = new JMenuItem("- CSV (comma separated)");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						copyTableData(selTables[0], ',');
					}
				});
				pm.add(mi);
				mi = new JMenuItem("- Excel (semicolon separated)");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						copyTableData(selTables[0], ';');
					}
				});
				pm.add(mi);
				mi = new JMenuItem("- Text (tab separated)");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						copyTableData(selTables[0], '\t');
					}
				});
				pm.add(mi);
				mi = new JMenuItem("- XHTML");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						copyTableDataXml(selTables[0]);
					}
				});
				pm.add(mi);
				return pm;
			}
		});
		
		//	offer exporting grid of connected tables as CSV or TSV
		if (selTables[0].hasAttribute("rowsContinueIn") || selTables[0].hasAttribute("rowsContinueFrom") || selTables[0].hasAttribute("colsContinueIn") || selTables[0].hasAttribute("colsContinueFrom"))
			actions.add(new SelectionAction("tableCopyGrid", "Copy Table Grid Data", "Copy the data in the table and all connected tables to the clipboard.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Copy Table Grid Data ...");
					JMenuItem mi;
					mi = new JMenuItem("- CSV (comma separated)");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							copyTableGridData(selTables[0], ',');
						}
					});
					pm.add(mi);
					mi = new JMenuItem("- Excel (semicolon separated)");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							copyTableGridData(selTables[0], ';');
						}
					});
					pm.add(mi);
					mi = new JMenuItem("- Text (tab separated)");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							copyTableGridData(selTables[0], '\t');
						}
					});
					pm.add(mi);
					mi = new JMenuItem("- XHTML");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							copyTableGridDataXml(selTables[0]);
						}
					});
					pm.add(mi);
					return pm;
				}
			});
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	void setTextDirection(ImRegion table, String textDirection) {
		
		//	assess word direction before normalizing argument for attribute setting
		ImWord[] tableWords = table.getWords();
		CountingSet wordDirections = new CountingSet(new HashMap());
		for (int w = 0; w < tableWords.length; w++)
			wordDirections.add((String) tableWords[w].getAttribute(ImRegion.TEXT_DIRECTION_ATTRIBUTE, ImRegion.TEXT_DIRECTION_LEFT_RIGHT));
		int inDirectionWordCount = wordDirections.getCount(textDirection);
		
		//	prepare effective attribute value (default is omitted)
		if (ImRegion.TEXT_DIRECTION_LEFT_RIGHT.equals(textDirection))
			textDirection = null;
		
		//	set text direction of column or block proper
		if (textDirection == null)
			table.removeAttribute(ImRegion.TEXT_DIRECTION_ATTRIBUTE);
		else table.setAttribute(ImRegion.TEXT_DIRECTION_ATTRIBUTE, textDirection);
		
		//	set text direction of columns, rows, and cells
		ImRegion[] cols = table.getRegions(ImRegion.TABLE_COL_TYPE);
		setRegionTextDirection(cols, textDirection);
		ImRegion[] rows = table.getRegions(ImRegion.TABLE_ROW_TYPE);
		setRegionTextDirection(rows, textDirection);
		setRegionTextDirection(table.getRegions(ImRegion.TABLE_CELL_TYPE), textDirection);
		
		//	touching words proper only if majority is off direction
		if ((wordDirections.size() - inDirectionWordCount) < inDirectionWordCount)
			return;
		
		//	TODO multiple text directions, need to turn
		if (wordDirections.elementCount() > 1)
			return;
		
		//	update word text direction
		setRegionTextDirection(tableWords, textDirection);
		ImUtils.orderTableWords(ImUtils.getTableCells(table, rows, cols));
	}
	
	private static void setRegionTextDirection(ImRegion[] regions, String textDirection) {
		for (int r = 0; r < regions.length; r++) {
			if (textDirection == null)
				regions[r].removeAttribute(ImRegion.TEXT_DIRECTION_ATTRIBUTE);
			else regions[r].setAttribute(ImRegion.TEXT_DIRECTION_ATTRIBUTE, textDirection);
		}
	}
	
	/**
	 * Merge up a bunch of table rows.
	 * @param table the table the rows belong to
	 * @param mergeRows the rows to merge
	 * @return true if the table was modified
	 */
	boolean mergeTableRows(ImRegion table, ImRegion[] mergeRows) {
		ImRegion[] tables = ImUtils.getRowConnectedTables(table);
		
		//	cut effort short in basic case
		if (tables.length == 1) {
			this.doMergeTableRows(table.getPage(), table, mergeRows);
			return true;
		}
		
		//	compute merge area
		ImRegion[] tableRows = ImUtils.getTableRows(table);
		HashSet mergeRowSet = new HashSet(Arrays.asList(mergeRows));
		Arrays.sort(tableRows, ImUtils.topDownOrder);
		int firstMergeRowIndex = -1;
		for (int r = 0; r < tableRows.length; r++)
			if (mergeRowSet.contains(tableRows[r])) {
				firstMergeRowIndex = r;
				break;
			}
		int tableRowCount = tableRows.length;
		
		//	get corresponding rows in connected tables
		System.out.println("Collecting merge row sets ...");
		System.out.println(" - merge area is rows " + firstMergeRowIndex + " to " + (firstMergeRowIndex + mergeRows.length - 1));
		ImRegion[][] mergeTableRows = new ImRegion[tables.length][];
		for (int t = 0; t < tables.length; t++) {
			System.out.println(" - from " + tables[t].pageId + "." + tables[t].bounds);
			
			//	this one's clear
			if (tables[t] == table) {
				System.out.println(" --> original selection");
				mergeTableRows[t] = mergeRows;
				continue;
			}
			
			//	collect rows inside merge area 
			tableRows = ImUtils.getTableRows(tables[t]);
			if (tableRows.length != tableRowCount) {
				System.out.println(" ==> row count mismatch, merge not possible");
				return false;
			}
			Arrays.sort(tableRows, ImUtils.topDownOrder);
			mergeTableRows[t] = new ImRegion[mergeRows.length];
			System.arraycopy(tableRows, firstMergeRowIndex, mergeTableRows[t], 0, mergeRows.length);
		}
		System.out.println(" ==> found all merge row sets");
		
		//	perform actual merge
		for (int t = 0; t < tables.length; t++)
			this.doMergeTableRows(tables[t].getPage(), tables[t], mergeTableRows[t]);
		return true;
	}
	
	private void doMergeTableRows(ImPage page, ImRegion table, ImRegion[] mergeRows) {
		
		//	mark merged row
		Arrays.sort(mergeRows, ImUtils.topDownOrder);
		ImRegion mRow = new ImRegion(page, ImLayoutObject.getAggregateBox(mergeRows), ImRegion.TABLE_ROW_TYPE);
		
		//	collect cells in merged rows
		ImRegion[] mRowCols = getRegionsOverlapping(page, mRow.bounds, ImRegion.TABLE_COL_TYPE);
		Arrays.sort(mRowCols, ImUtils.leftRightOrder);
		ImRegion[] mRowCells = getRegionsOverlapping(page, mRow.bounds, ImRegion.TABLE_CELL_TYPE);
		Arrays.sort(mRowCells, ImUtils.leftRightOrder);
		ImRegion[][] mergeColCells = new ImRegion[mergeRows.length][mRowCols.length];
		for (int r = 0; r < mergeRows.length; r++)
			for (int c = 0; c < mRowCols.length; c++) {
				for (int l = 0; l < mRowCells.length; l++)
					if (mRowCells[l].bounds.overlaps(mergeRows[r].bounds) && mRowCells[l].bounds.overlaps(mRowCols[c].bounds)) {
						mergeColCells[r][c] = mRowCells[l];
						break;
					}
				//	TODO make this nested loop join faster, somehow
			}
		
		//	create merged cells column-wise
		for (int c = 0; c < mRowCols.length; c++) {
			int lc = c;
			
			//	find column with straight right edge
			while ((lc+1) < mRowCols.length) {
				boolean colClosed = true;
				for (int r = 0; r < mergeRows.length; r++)
					if (mergeColCells[r][lc].bounds.overlaps(mRowCols[lc+1].bounds)) {
						colClosed = false;
						break;
					}
				if (colClosed)
					break;
				else lc++;
			}
			
			//	aggregate merged cell bounds
			int mCellTop = mRow.bounds.top;
			int mCellBottom = mRow.bounds.bottom;
			for (int ac = c; ac <= lc; ac++)
				for (int r = 0; r < mergeRows.length; r++) {
					mCellTop = Math.min(mCellTop, mergeColCells[r][ac].bounds.top);
					mCellBottom = Math.max(mCellBottom, mergeColCells[r][ac].bounds.bottom);
				}
			BoundingBox mCellBounds = new BoundingBox(mRowCols[c].bounds.left, mRowCols[lc].bounds.right, mCellTop, mCellBottom);
			
			//	remove merged cells
			ImRegion[] mCellCells = getRegionsOverlapping(page, mCellBounds, ImRegion.TABLE_CELL_TYPE);
			for (int l = 0; l < mCellCells.length; l++)
				page.removeRegion(mCellCells[l]);
			
			//	add merged cell
			this.markTableCell(page, mCellBounds, true, false);
			
			//	jump to last column (loop increment will switch one further)
			c = lc;
		}
		
		//	remove merged rows
		for (int r = 0; r < mergeRows.length; r++)
			page.removeRegion(mergeRows[r]);
		
		//	update cells (whichever we might have cut apart in rows above or below merger)
		ImRegion[][] tableCells = this.markTableCells(page, table);
		
		//	clean up table structure
		ImUtils.orderTableWords(tableCells);
		this.cleanupTableAnnotations(table.getDocument(), table);
	}
	
	/**
	 * Split a table row. Depending on the splitting box, the argument table
	 * row is split into two (splitting off top or bottom) or three (splitting
	 * across the middle) new rows.
	 * @param table the table the row belongs to
	 * @param toSplitRow the table row to split
	 * @param splitBox the box defining where to split
	 * @return true if the table was modified
	 */
	boolean splitTableRow(ImRegion table, ImRegion toSplitRow, BoundingBox splitBox) {
		ImRegion[] tables = ImUtils.getRowConnectedTables(table);
		ImRegion[] toSplitRowCells = getRegionsOverlapping(table.getPage(), toSplitRow.bounds, ImRegion.TABLE_CELL_TYPE);
		Arrays.sort(toSplitRowCells, ImUtils.leftRightOrder);
		WordBlock[] toSplitRowWords = this.getWordBlocksForRowSplit(table.getPage(), toSplitRowCells, toSplitRow.bounds, splitBox);
		
		//	compute relative split bounds
		int relAboveSplitBottom = 0;
		int relSplitTop = (splitBox.bottom - toSplitRow.bounds.top);
		int relSplitBottom = (splitBox.top - toSplitRow.bounds.top);
		int relBelowSplitTop = (toSplitRow.bounds.bottom - toSplitRow.bounds.top);
		
		//	order words above, inside, and below selection
		for (int w = 0; w < toSplitRowWords.length; w++) {
			if (toSplitRowWords[w].centerY < splitBox.top)
				relAboveSplitBottom = Math.max(relAboveSplitBottom, (toSplitRowWords[w].bounds.bottom - toSplitRow.bounds.top));
			else if (toSplitRowWords[w].centerY < splitBox.bottom) {
				relSplitTop = Math.min(relSplitTop, (toSplitRowWords[w].bounds.top - toSplitRow.bounds.top));
				relSplitBottom = Math.max(relSplitBottom, (toSplitRowWords[w].bounds.bottom - toSplitRow.bounds.top));
			}
			else relBelowSplitTop = Math.min(relBelowSplitTop, (toSplitRowWords[w].bounds.top - toSplitRow.bounds.top));
		}
		
		//	anything to split at all?
		int emptySplitRows = 0;
		if (relAboveSplitBottom == 0)
			emptySplitRows++;
		if (relSplitBottom <= relSplitTop)
			emptySplitRows++;
		if (relBelowSplitTop == (toSplitRow.bounds.bottom - toSplitRow.bounds.top))
			emptySplitRows++;
		if (emptySplitRows >= 2)
			return false;
		
		//	check if we have words obstructing the split
		HashSet multiRowWords = null;
		if ((relSplitTop < relAboveSplitBottom) || (relBelowSplitTop < relSplitBottom)) {
			
			//	determine bottom edge of all words _completely_ above of selection ...
			//	... and top edge of all words _completely_ below of selection ...
			int aboveSplitBottom = toSplitRow.bounds.top;
			int belowSplitTop = toSplitRow.bounds.bottom;
			ArrayList insideSplitWords = new ArrayList();
			for (int w = 0; w < toSplitRowWords.length; w++) {
				if (toSplitRowWords[w].bounds.bottom <= splitBox.top)
					aboveSplitBottom = Math.max(aboveSplitBottom, toSplitRowWords[w].bounds.bottom);
				else if (splitBox.bottom <= toSplitRowWords[w].bounds.top)
					belowSplitTop = Math.min(belowSplitTop, toSplitRowWords[w].bounds.top);
				else insideSplitWords.add(toSplitRowWords[w]);
			}
			
			//	mark all remaining words that protrude beyond (or even only close to ???) either of these edges as multi-row
			//	... and determine boundaries of middle row from the rest
			//	TODO depending on average row gap, be even more sensitive about multi-row cells ...
			//	... as they might end up failing to completely reach edge of neighbor row and only get close
			int splitTop = splitBox.bottom;
			int splitBottom = splitBox.top;
			multiRowWords = new HashSet();
			for ( int w = 0; w < insideSplitWords.size(); w++) {
				WordBlock isWord = ((WordBlock) insideSplitWords.get(w));
				if ((toSplitRow.bounds.top < aboveSplitBottom) && (isWord.bounds.top <= aboveSplitBottom))
					multiRowWords.add(isWord);
				else if ((belowSplitTop < toSplitRow.bounds.bottom) && (belowSplitTop <= isWord.bounds.bottom))
					multiRowWords.add(isWord);
				else {
					splitTop = Math.min(splitTop, isWord.bounds.top);
					splitBottom = Math.max(splitBottom, isWord.bounds.bottom);
				}
			}
			
			//	re-compute relative split bounds
			relAboveSplitBottom = (aboveSplitBottom - toSplitRow.bounds.top);
			relSplitTop = (splitTop - toSplitRow.bounds.top);
			relSplitBottom = (splitBottom - toSplitRow.bounds.top);
			relBelowSplitTop = (belowSplitTop - toSplitRow.bounds.top);
		}
		
		//	cut effort short in basic case
		if (tables.length == 1) {
			this.doSplitTableRow(table.getPage(), table, toSplitRow, toSplitRowWords, multiRowWords, relAboveSplitBottom, relSplitTop, relSplitBottom, relBelowSplitTop);
			return true;
		}
		
		//	compute table-relative split area
		int relToSplitRowTop = (toSplitRow.bounds.top - table.bounds.top);
		int relToSplitRowBottom = (toSplitRow.bounds.bottom - table.bounds.top);
		
		//	get corresponding rows in connected tables
		System.out.println("Collecting to-split rows ...");
		System.out.println(" - relative split area is " + relToSplitRowTop + " to " + relToSplitRowBottom);
		ImRegion[] toSplitTableRows = new ImRegion[tables.length];
		for (int t = 0; t < tables.length; t++) {
			System.out.println(" - from " + tables[t].pageId + "." + tables[t].bounds);
			
			//	this one's clear
			if (tables[t] == table) {
				System.out.println(" --> original selection");
				toSplitTableRows[t] = toSplitRow;
				continue;
			}
			
			//	collect rows inside table-relative merge area
			ImRegion[] tableRows = ImUtils.getTableRows(tables[t]);
			for (int r = 0; r < tableRows.length; r++) {
				int relRowCenter = (((tableRows[r].bounds.top + tableRows[r].bounds.bottom) / 2) - tables[t].bounds.top);
				System.out.println("   - row " + tableRows[r].bounds + ", relative center is " + relRowCenter);
				if ((relToSplitRowTop < relRowCenter) && (relRowCenter < relToSplitRowBottom)) {
					System.out.println("   --> inside selection");
					if (toSplitTableRows[t] == null)
						toSplitTableRows[t] = tableRows[r];
					else {
						System.out.println(" ==> duplicate match, split not possible");
						return false;
					}
				}
				else System.out.println("   --> outside selection");
			}
		}
		System.out.println(" ==> found all to-split rows");
		
		//	perform split
		float relTopMiddleSplit = (((float) (relAboveSplitBottom + relSplitTop)) / (2 * (toSplitRow.bounds.bottom - toSplitRow.bounds.top)));
		float relMiddleBottomSplit = (((float) (relSplitBottom + relBelowSplitTop)) / (2 * (toSplitRow.bounds.bottom - toSplitRow.bounds.top)));
		for (int t = 0; t < tables.length; t++) {
			if (tables[t] == table)
				this.doSplitTableRow(tables[t].getPage(), tables[t], toSplitTableRows[t], toSplitRowWords, multiRowWords, relAboveSplitBottom, relSplitTop, relSplitBottom, relBelowSplitTop);
			else this.doSplitTableRow(tables[t], toSplitTableRows[t], relTopMiddleSplit, relMiddleBottomSplit);
		}
		return true;
	}
	
	private void doSplitTableRow(ImRegion table, ImRegion toSplitRow, float relTopMiddleSplit, float relMiddleBottomSplit) {
		
		//	compute relative split box
		int splitBoxTop = (((int) (relTopMiddleSplit * (toSplitRow.bounds.bottom - toSplitRow.bounds.top))) + toSplitRow.bounds.top);
		int splitBoxBottom = (((int) (relMiddleBottomSplit * (toSplitRow.bounds.bottom - toSplitRow.bounds.top))) + toSplitRow.bounds.top);
		
		//	compute relative split bounds
		int relAboveSplitBottom = 0;
		int relSplitTop = (splitBoxBottom - toSplitRow.bounds.top);
		int relSplitBottom = (splitBoxTop - toSplitRow.bounds.top);
		int relBelowSplitTop = (toSplitRow.bounds.bottom - toSplitRow.bounds.top);
		
		//	order words above, inside, and below selection
		ImRegion[] toSplitRowCells = getRegionsOverlapping(table.getPage(), toSplitRow.bounds, ImRegion.TABLE_CELL_TYPE);
		Arrays.sort(toSplitRowCells, ImUtils.leftRightOrder);
		WordBlock[] toSplitRowWords = this.getWordBlocksForRowSplit(table.getPage(), toSplitRowCells, toSplitRow.bounds, new BoundingBox(table.bounds.left, table.bounds.right, splitBoxTop, splitBoxBottom));
		for (int w = 0; w < toSplitRowWords.length; w++) {
			if (toSplitRowWords[w].centerY < splitBoxTop)
				relAboveSplitBottom = Math.max(relAboveSplitBottom, (toSplitRowWords[w].bounds.bottom - toSplitRow.bounds.top));
			else if (toSplitRowWords[w].centerY < splitBoxBottom) {
				relSplitTop = Math.min(relSplitTop, (toSplitRowWords[w].bounds.top - toSplitRow.bounds.top));
				relSplitBottom = Math.max(relSplitBottom, (toSplitRowWords[w].bounds.bottom - toSplitRow.bounds.top));
			}
			else relBelowSplitTop = Math.min(relBelowSplitTop, (toSplitRowWords[w].bounds.top - toSplitRow.bounds.top));
		}
		
		//	check if we have words obstructing the split
		HashSet multiRowWords = null;
		if ((relSplitTop < relAboveSplitBottom) || (relBelowSplitTop < relSplitBottom)) {
			
			//	determine bottom edge of all words _completely_ above of selection ...
			//	... and top edge of all words _completely_ below of selection ...
			int aboveSplitBottom = toSplitRow.bounds.top;
			int belowSplitTop = toSplitRow.bounds.bottom;
			ArrayList insideSplitWords = new ArrayList();
			for (int w = 0; w < toSplitRowWords.length; w++) {
				if (toSplitRowWords[w].bounds.bottom <= splitBoxTop)
					aboveSplitBottom = Math.max(aboveSplitBottom, toSplitRowWords[w].bounds.bottom);
				else if (splitBoxBottom <= toSplitRowWords[w].bounds.top)
					belowSplitTop = Math.min(belowSplitTop, toSplitRowWords[w].bounds.top);
				else insideSplitWords.add(toSplitRowWords[w]);
			}
			
			//	mark all remaining words that protrude beyond (or even only close to ???) either of these edges as multi-row
			//	... and determine boundaries of middle row from the rest
			//	TODO depending on average row gap, be even more sensitive about multi-row cells ...
			//	... as they might end up failing to completely reach edge of neighbor row and only get close
			int splitTop = splitBoxBottom;
			int splitBottom = splitBoxTop;
			multiRowWords = new HashSet();
			for ( int w = 0; w < insideSplitWords.size(); w++) {
				WordBlock isWord = ((WordBlock) insideSplitWords.get(w));
				if ((toSplitRow.bounds.top < aboveSplitBottom) && (isWord.bounds.top <= aboveSplitBottom))
					multiRowWords.add(isWord);
				else if ((belowSplitTop < toSplitRow.bounds.bottom) && (belowSplitTop <= isWord.bounds.bottom))
					multiRowWords.add(isWord);
				else {
					splitTop = Math.min(splitTop, isWord.bounds.top);
					splitBottom = Math.max(splitBottom, isWord.bounds.bottom);
				}
			}
			
			//	re-compute relative split bounds
			relAboveSplitBottom = (aboveSplitBottom - toSplitRow.bounds.top);
			relSplitTop = (splitTop - toSplitRow.bounds.top);
			relSplitBottom = (splitBottom - toSplitRow.bounds.top);
			relBelowSplitTop = (belowSplitTop - toSplitRow.bounds.top);
		}
		
		//	do split with case adjusted absolute numbers
		this.doSplitTableRow(table.getPage(), table, toSplitRow, toSplitRowWords, multiRowWords, relAboveSplitBottom, relSplitTop, relSplitBottom, relBelowSplitTop);
	}
	
	private void doSplitTableRow(ImPage page, ImRegion table, ImRegion toSplitRow, WordBlock[] toSplitRowWords, HashSet multiRowWords, int relAboveSplitBottom, int relSplitTop, int relSplitBottom, int relBelowSplitTop) {
		
		//	create two or three new rows
		if (0 < relAboveSplitBottom) {
			BoundingBox arBox = new BoundingBox(table.bounds.left, table.bounds.right, toSplitRow.bounds.top, (toSplitRow.bounds.top + relAboveSplitBottom));
			new ImRegion(page, arBox, ImRegion.TABLE_ROW_TYPE);
		}
		if (relSplitTop < relSplitBottom) {
			BoundingBox irBox = new BoundingBox(table.bounds.left, table.bounds.right, (toSplitRow.bounds.top + relSplitTop), (toSplitRow.bounds.top + relSplitBottom));
			new ImRegion(page, irBox, ImRegion.TABLE_ROW_TYPE);
		}
		if (relBelowSplitTop < (toSplitRow.bounds.bottom - toSplitRow.bounds.top)) {
			BoundingBox brBox = new BoundingBox(table.bounds.left, table.bounds.right, (toSplitRow.bounds.top + relBelowSplitTop), toSplitRow.bounds.bottom);
			new ImRegion(page, brBox, ImRegion.TABLE_ROW_TYPE);
		}
		
		//	remove selected row
		page.removeRegion(toSplitRow);
		
		//	get split rows and existing cells
		ImRegion[] sRows = getRegionsInside(page, toSplitRow.bounds, ImRegion.TABLE_ROW_TYPE, false);
		Arrays.sort(sRows, ImUtils.topDownOrder);
		ImRegion[] toSplitRowCells = getRegionsOverlapping(page, toSplitRow.bounds, ImRegion.TABLE_CELL_TYPE);
		Arrays.sort(toSplitRowCells, ImUtils.leftRightOrder);
		
		//	create new cells left to right
		for (int l = 0; l < toSplitRowCells.length; l++) {
			
			//	handle base case, saving all the hassle below
			if (multiRowWords == null) {
				
				//	create split cells
				for (int r = 0; r < sRows.length; r++) {
					int sCellTop = ((r == 0) ? toSplitRowCells[l].bounds.top : sRows[r].bounds.top);
					int sCellBottom = (((r+1) == sRows.length) ? toSplitRowCells[l].bounds.bottom : sRows[r].bounds.bottom);
					this.markTableCell(page, new BoundingBox(toSplitRowCells[l].bounds.left, toSplitRowCells[l].bounds.right, sCellTop, sCellBottom), true, false);
				}
				
				//	clean up and we're done here
				page.removeRegion(toSplitRowCells[l]);
				continue;
			}
			
			//	check for multi-row cells
			boolean[] cellReachesIntoNextRow = new boolean[sRows.length - 1];
			Arrays.fill(cellReachesIntoNextRow, false);
			for (int r = 0; r < (sRows.length - 1); r++) {
				WordBlock[] sCellWords = this.getWordBlocksOverlapping(toSplitRowWords, new BoundingBox(toSplitRowCells[l].bounds.left, toSplitRowCells[l].bounds.right, sRows[r].bounds.top, sRows[r+1].bounds.bottom));
				if (sCellWords.length == 0)
					continue;
				Arrays.sort(sCellWords, ImUtils.topDownOrder);
				for (int w = 0; w < sCellWords.length; w++) {
					if (sRows[r+1].bounds.top < sCellWords[w].bounds.top)
						break; // we're only interested in words overlapping with space between the current pair of rows
					if (multiRowWords.contains(sCellWords[w])) {
						cellReachesIntoNextRow[r] = true;
						break; // one word overlapping both rows is enough
					}
				}
			}
			
			//	create cells for split rows
			int sCellTop = Math.min(toSplitRowCells[l].bounds.top, sRows[0].bounds.top);
			for (int r = 0; r < sRows.length; r++) {
				if ((r < cellReachesIntoNextRow.length) && cellReachesIntoNextRow[r])
					continue;
				if ((r+1) == sRows.length)
					this.markTableCell(page, new BoundingBox(toSplitRowCells[l].bounds.left, toSplitRowCells[l].bounds.right, sCellTop, toSplitRowCells[l].bounds.bottom), true, false);
				else {
					this.markTableCell(page, new BoundingBox(toSplitRowCells[l].bounds.left, toSplitRowCells[l].bounds.right, sCellTop, sRows[r].bounds.bottom), true, false);
					sCellTop = sRows[r+1].bounds.top;
				}
			}
			
			//	clean up
			page.removeRegion(toSplitRowCells[l]);
		}
		
		//	clean up table structure
		ImUtils.orderTableWords(this.markTableCells(page, table));
		this.cleanupTableAnnotations(table.getDocument(), table);
	}
	
	private static class TableRowLine {
		final ImWord[] words;
		final ImRegion table;
		final HashSet overlappingLines = new HashSet();
		final HashSet overlappingWords = new HashSet();
		int top;
		int bottom;
		TableRowLine(ImWord[] words, ImRegion table) {
			this.words = words;
			int top = Integer.MAX_VALUE;
			int bottom = Integer.MIN_VALUE;
			for (int w = 0; w < this.words.length; w++) {
				top = Math.min(top, this.words[w].bounds.top);
				bottom = Math.max(bottom, this.words[w].bounds.bottom);
			}
			this.top = top;
			this.bottom = bottom;
			this.table = table;
			System.out.println("   - " + this.getBounds());
		}
		BoundingBox getBounds() {
			return new BoundingBox(this.table.bounds.left, this.table.bounds.right, this.top, this.bottom);
		}
	}
	
	private boolean splitTableRowToLines(ImPage page, ImRegion table, ImRegion toSplitRow) {
//		System.out.println("Splitting " + toSplitRow.bounds + " to lines");
		
		//	get table row words
		ImWord[] toSplitRowWords = toSplitRow.getWords();
		ImUtils.sortLeftRightTopDown(toSplitRowWords);
//		System.out.println(" - words sorted");
		
		//	sort words into rows
		int rowStartWordIndex = 0;
		ArrayList rowLines = new ArrayList();
//		System.out.println(" - collecting line rows");
		for (int w = 0; w < toSplitRowWords.length; w++)
			if (((w+1) == toSplitRowWords.length) || (toSplitRowWords[w+1].centerY > toSplitRowWords[w].bounds.bottom)) {
				rowLines.add(new TableRowLine(Arrays.copyOfRange(toSplitRowWords, rowStartWordIndex, (w+1)), table));
				rowStartWordIndex = (w+1);
			}
//		System.out.println(" - line rows collected");
		
		//	check for conflicts
		System.out.println(" - checking for conflicts");
		for (int l = 0; l < rowLines.size(); l++) {
			TableRowLine rowLine = ((TableRowLine) rowLines.get(l));
			for (int cl = 0; cl < rowLines.size(); cl++) {
				if (cl == l)
					continue;
				TableRowLine cRowLine = ((TableRowLine) rowLines.get(cl));
				if (cRowLine.bottom <= rowLine.top)
					continue;
				if (rowLine.bottom <= cRowLine.top)
					break;
				rowLine.overlappingLines.add(cRowLine);
				cRowLine.overlappingLines.add(rowLine);
			}
//			System.out.println("   - found " + rowLine.overlappingLines.size() + " conflicts for " + rowLine.getBounds());
		}
		
		//	resolve conflicts, and collect between-line words
		HashSet betweenLineWords = new HashSet();
		System.out.println(" - resolving conflicts");
		for (boolean lineOverlapResolved = true; lineOverlapResolved;) {
			lineOverlapResolved = false;
			
			//	find line overlapping maximum number of other lines
			int maxOverlappingLines = 0;
			TableRowLine maxOverlappedLine = null;
			ArrayList maxOverlappedLines = new ArrayList();
			for (int l = 0; l < rowLines.size(); l++) {
				TableRowLine rowLine = ((TableRowLine) rowLines.get(l));
				if (maxOverlappingLines < rowLine.overlappingLines.size()) {
					maxOverlappingLines = rowLine.overlappingLines.size();
					maxOverlappedLine = rowLine;
					maxOverlappedLines.clear();
					maxOverlappedLines.add(rowLine);
				}
				else if (maxOverlappingLines == 0) {}
				else if (maxOverlappingLines == rowLine.overlappingLines.size())
					maxOverlappedLines.add(rowLine);
			}
			if (maxOverlappedLine == null)
				break;
			
			//	if we have multiple lines with same number of conflicting rows, first take care of the one with most non-conflicting words
			if (maxOverlappedLines.size() > 1) {
//				System.out.println(" - choosing between " + maxOverlappedLines.size() + " lines overlapping with " + maxOverlappingLines + " others");
				int maxNonOverlappingWordCount = 0;
				for (int l = 0; l < maxOverlappedLines.size(); l++) {
					TableRowLine rowLine = ((TableRowLine) maxOverlappedLines.get(l));
					HashSet nonOverlappingWords = new HashSet(Arrays.asList(rowLine.words));
					for (Iterator olit = rowLine.overlappingLines.iterator(); olit.hasNext();) {
						TableRowLine oRowLine = ((TableRowLine) olit.next());
						for (int w = 0; w < rowLine.words.length; w++) {
							if (rowLine.words[w].bounds.bottom <= oRowLine.top) { /* word above conflicting line */ }
							else if (oRowLine.bottom <= rowLine.words[w].bounds.top) { /* word below conflicting line */ }
							else nonOverlappingWords.remove(rowLine.words[w]);
						}
					}
//					System.out.println("   - " + rowLine.getBounds() + " has " + nonOverlappingWords.size() + " non-conflicting words");
					if (maxNonOverlappingWordCount < nonOverlappingWords.size()) {
						maxNonOverlappingWordCount = nonOverlappingWords.size();
						maxOverlappedLine = rowLine;
					}
				}
//				System.out.println("   ==> selected " + maxOverlappedLine.getBounds() + " with " + maxNonOverlappingWordCount + " non-conflicting words");
			}
			lineOverlapResolved = true; // we're going to resolve this one ... somehow ...
//			System.out.println("   - shrinking " + maxOverlappedLine.getBounds() + " with " + maxOverlappedLine.overlappingLines.size() + " overlapping lines");
			
			//	collect words in said line that don't overlap other lines
			HashSet nonOverlappingWords = new HashSet(Arrays.asList(maxOverlappedLine.words));
			for (Iterator olit = maxOverlappedLine.overlappingLines.iterator(); olit.hasNext();) {
				TableRowLine oRowLine = ((TableRowLine) olit.next());
				for (int w = 0; w < maxOverlappedLine.words.length; w++) {
					if (maxOverlappedLine.words[w].bounds.bottom <= oRowLine.top) { /* word above conflicting line */ }
					else if (oRowLine.bottom <= maxOverlappedLine.words[w].bounds.top) { /* word below conflicting line */ }
					else nonOverlappingWords.remove(maxOverlappedLine.words[w]);
				}
			}
			
			//	no words remaining, eliminate it and collect words for latter cell mergers
			if (nonOverlappingWords.isEmpty()) {
				betweenLineWords.addAll(Arrays.asList(maxOverlappedLine.words));
				rowLines.remove(maxOverlappedLine);
				for (int l = 0; l < rowLines.size(); l++)
					((TableRowLine) rowLines.get(l)).overlappingLines.remove(maxOverlappedLine);
//				System.out.println("     ==> eliminated altogether (1)");
				continue;
			}
			
			//	shrink line to non-conflicting dimensions
			int nonOverlappingTop = Integer.MAX_VALUE;
			int nonOverlappingBottom = Integer.MIN_VALUE;
			for (Iterator nowit = nonOverlappingWords.iterator(); nowit.hasNext();) {
				ImWord noWord = ((ImWord) nowit.next());
				nonOverlappingTop = Math.min(nonOverlappingTop, noWord.bounds.top);
				nonOverlappingBottom = Math.max(nonOverlappingBottom, noWord.bounds.bottom);
			}
			if (nonOverlappingBottom <= nonOverlappingTop) {
				betweenLineWords.addAll(Arrays.asList(maxOverlappedLine.words));
				rowLines.remove(maxOverlappedLine);
				System.out.println("     ==> eliminated altogether (2)");
			}
			else {
				maxOverlappedLine.top = nonOverlappingTop;
				maxOverlappedLine.bottom = nonOverlappingBottom;
				for (int w = 0; w < maxOverlappedLine.words.length; w++) {
					if (!nonOverlappingWords.contains(maxOverlappedLine.words[w]))
						maxOverlappedLine.overlappingWords.add(maxOverlappedLine.words[w]);
				}
//				System.out.println("     ==> shrunk to " + maxOverlappedLine.getBounds());
//				System.out.println("     ==> got conflict inducing words " + maxOverlappedLine.overlappingWords);
			}
			for (int l = 0; l < rowLines.size(); l++)
				((TableRowLine) rowLines.get(l)).overlappingLines.remove(maxOverlappedLine);
			maxOverlappedLine.overlappingLines.clear();
		}
		
		//	mark line rows
		ImRegion[] sRows = new ImRegion[rowLines.size()];
		for (int l = 0; l < rowLines.size(); l++)
			sRows[l] = new ImRegion(table.getPage(), ((TableRowLine) rowLines.get(l)).getBounds(), ImRegion.TABLE_ROW_TYPE);
//		System.out.println(" - line rows added");
		
		//	remove selected row
		table.getPage().removeRegion(toSplitRow);
//		System.out.println(" - to split row removed");
		
		//	get split rows and existing cells
//		System.out.println(" - got " + sRows.length + " line rows");
		ImRegion[] toSplitRowCells = getRegionsOverlapping(page, toSplitRow.bounds, ImRegion.TABLE_CELL_TYPE);
		Arrays.sort(toSplitRowCells, ImUtils.leftRightOrder);
//		System.out.println(" - got " + toSplitRowCells.length + " cells to split");
		
		//	create new cells left to right
		for (int c = 0; c < toSplitRowCells.length; c++) {
//			System.out.println(" - splitting cell " + toSplitRowCells[c].bounds);
			
			//	create split cells
			for (int r = 0; r < sRows.length; r++) {
				int sCellTop = ((r == 0) ? toSplitRowCells[c].bounds.top : sRows[r].bounds.top);
				int sCellBottom = (((r+1) == sRows.length) ? toSplitRowCells[c].bounds.bottom : sRows[r].bounds.bottom);
				ImRegion sCell = this.markTableCell(page, new BoundingBox(toSplitRowCells[c].bounds.left, toSplitRowCells[c].bounds.right, sCellTop, sCellBottom), true, false);
//				System.out.println("   - " + sCell.bounds);
			}
			
			//	clean up
			page.removeRegion(toSplitRowCells[c]);
//			System.out.println("   - cell removed");
		}
		
		//	merge cells with overlapping content
		for (int l = 0; l < rowLines.size(); l++)
			betweenLineWords.addAll(((TableRowLine) rowLines.get(l)).overlappingWords);
		for (Iterator blwit = betweenLineWords.iterator(); blwit.hasNext();) {
			ImWord blWord = ((ImWord) blwit.next());
//			System.out.println(" - merging cells overlapping " + blWord);
			ImRegion[] blWordCells = getRegionsOverlapping(page, blWord.bounds, ImRegion.TABLE_CELL_TYPE);
//			System.out.println("   - got " + blWordCells.length + " overlapping cells");
			if (blWordCells.length < 2)
				continue;
			BoundingBox mCellBounds = ImLayoutObject.getAggregateBox(blWordCells);
			ImRegion mCell = this.markTableCell(page, mCellBounds, true, false);
//			System.out.println("   - got merged cell " + mCell.bounds);
			for (int c = 0; c < blWordCells.length; c++)
				page.removeRegion(blWordCells[c]);
//			System.out.println("   - merged cells removed");
		}
		
		//	clean up table structure
		ImUtils.orderTableWords(this.markTableCells(page, table));
		this.cleanupTableAnnotations(table.getDocument(), table);
		
		//	finally ...
		return true;
	}
	
	private WordBlock[] getWordBlocksForRowSplit(ImPage page, ImRegion[] cells, BoundingBox bounds, BoundingBox splitBounds) {
		
		//	compute average row margin within and across split
		int inCellRowGapSum = 0;
		int inCellRowGapCount = 0;
		int crossCellRowGapSum = 0;
		int crossCellRowGapCount = 0;
		int wordHeightSum = 0;
		int wordHeightCount = 0;
		for (int c = 0; c < cells.length; c++) {
			ImWord[] cellWords = page.getWordsInside(cells[c].bounds);
			ImUtils.sortLeftRightTopDown(cellWords);
			for (int w = 0; w < cellWords.length; w++) {
				wordHeightSum += cellWords[w].bounds.getHeight();
				wordHeightCount++;
				if ((w + 1) == cellWords.length)
					break; // only need to count height from last word
				if (cellWords[w].bounds.bottom > cellWords[w+1].centerY)
					continue; // same line, no gap to analyze here
				if (cellWords[w].bounds.right <= cellWords[w+1].bounds.left)
					continue; // horizontal offset to right, unsafe
				if (cellWords[w].centerY < splitBounds.top) {
					if (cellWords[w+1].centerY < splitBounds.top) {
						inCellRowGapSum += (cellWords[w+1].bounds.top - cellWords[w].bounds.bottom);
						inCellRowGapCount++;
					}
					else {
						crossCellRowGapSum += (cellWords[w+1].bounds.top - cellWords[w].bounds.bottom);
						crossCellRowGapCount++;
					}
				}
				else if (cellWords[w].centerY < splitBounds.bottom) {
					if (cellWords[w+1].centerY < splitBounds.bottom) {
						inCellRowGapSum += (cellWords[w+1].bounds.top - cellWords[w].bounds.bottom);
						inCellRowGapCount++;
					}
					else {
						crossCellRowGapSum += (cellWords[w+1].bounds.top - cellWords[w].bounds.bottom);
						crossCellRowGapCount++;
					}
				}
				else {
					inCellRowGapSum += (cellWords[w+1].bounds.top - cellWords[w].bounds.bottom);
					inCellRowGapCount++;
				}
			}
		}
		int inCellRowGap = ((inCellRowGapCount == 0) ? -1 : (inCellRowGapSum / inCellRowGapCount));
		int crossCellRowGap = ((crossCellRowGapCount == 0) ? -1 : (crossCellRowGapSum / crossCellRowGapCount));
		int wordHeight = ((wordHeightCount == 0) ? 0 : (wordHeightSum / wordHeightCount));
//		System.out.println("In-cell row gap is " + inCellRowGap + ", cross-cell row gap is " + crossCellRowGap + ", word height is " + wordHeight);
		
		//	fall back to word height based estimates if gaps are 0
		if (inCellRowGap == -1)
			inCellRowGap = (wordHeight / 4);
		if (crossCellRowGap == -1)
			crossCellRowGap = (wordHeight / 2);
		
		//	aggregate words to blocks for individual cells
		ArrayList wbs = new ArrayList();
		for (int c = 0; c < cells.length; c++) {
			ImWord[] cellWords = page.getWordsInside(cells[c].bounds);
			ImUtils.sortLeftRightTopDown(cellWords);
			int wbStart = 0;
			for (int w = 1; w <= cellWords.length; w++) {
				if ((w == cellWords.length) || this.isRowGap(cellWords[w-1], cellWords[w], inCellRowGap, crossCellRowGap)) {
					ImWord[] wbWords = new ImWord[w - wbStart];
					System.arraycopy(cellWords, wbStart, wbWords, 0, wbWords.length);
					WordBlock wb = new WordBlock(page, wbWords, false);
//					System.out.println("Word block: " + wb + " at " + wb.bounds);
					wbs.add(wb);
					wbStart = w;
				}
			}
		}
		
		//	finally ...
		return ((WordBlock[]) wbs.toArray(new WordBlock[wbs.size()]));
	}
	
	private boolean isRowGap(ImWord word1, ImWord word2, int inCellRowGap, int crossCellRowGap) {
		if (word1.bounds.bottom > word2.centerY)
			return false; // presumably same line
		if (word1.bounds.bottom > word2.bounds.top)
			return false; // no gap at all
		if (word1.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE)
			return false; // explicit continuation marker
		if (word1.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED)
			return false; // explicit continuation marker
		if ((inCellRowGap * 2) > crossCellRowGap)
			return true; // too close to call, stay safe and avoid gluing words together
		int wordGap = (word2.bounds.top - word1.bounds.bottom);
		return ((crossCellRowGap - wordGap) < (wordGap - inCellRowGap)); // TODO make sure to measure sensibly
	}
	
	/**
	 * Merge up a bunch of table columns.
	 * @param table the table the columns belong to
	 * @param mergeCols the columns to merge
	 * @return true if the table was modified
	 */
	boolean mergeTableCols(ImRegion table, ImRegion[] mergeCols) {
		ImRegion[] tables = ImUtils.getColumnConnectedTables(table);
		
		//	cut effort short in basic case
		if (tables.length == 1) {
			this.doMergeTableCols(table.getPage(), table, mergeCols);
			return true;
		}
		
		//	compute merge area
		ImRegion[] tableCols = ImUtils.getTableColumns(table);
		Arrays.sort(tableCols, ImUtils.leftRightOrder);
		HashSet mergeRowSet = new HashSet(Arrays.asList(mergeCols));
		int firstMergeColIndex = -1;
		for (int c = 0; c < tableCols.length; c++)
			if (mergeRowSet.contains(tableCols[c])) {
				firstMergeColIndex = c;
				break;
			}
		int tableColCount = tableCols.length;
		
		//	get corresponding rows in connected tables
		System.out.println("Collecting merge column sets ...");
		System.out.println(" - merge area is columns " + firstMergeColIndex + " to " + (firstMergeColIndex + mergeCols.length - 1));
		ImRegion[][] mergeTableCols = new ImRegion[tables.length][];
		for (int t = 0; t < tables.length; t++) {
			System.out.println(" - from " + tables[t].pageId + "." + tables[t].bounds);
			
			//	this one's clear
			if (tables[t] == table) {
				System.out.println(" --> original selection");
				mergeTableCols[t] = mergeCols;
				continue;
			}
			
			//	collect columns inside merge area 
			tableCols = ImUtils.getTableColumns(tables[t]);
			if (tableCols.length != tableColCount) {
				System.out.println(" ==> column count mismatch, merge not possible");
				return false;
			}
			Arrays.sort(tableCols, ImUtils.leftRightOrder);
			mergeTableCols[t] = new ImRegion[mergeCols.length];
			System.arraycopy(tableCols, firstMergeColIndex, mergeTableCols[t], 0, mergeCols.length);
		}
		System.out.println(" ==> found all merge column sets");
		
		//	perform actual merge
		for (int t = 0; t < tables.length; t++)
			this.doMergeTableCols(tables[t].getPage(), tables[t], mergeTableCols[t]);
		return true;
	}
	
	private void doMergeTableCols(ImPage page, ImRegion table, ImRegion[] mergeCols) {
		
		//	mark merged column
		Arrays.sort(mergeCols, ImUtils.leftRightOrder);
		ImRegion mCol = new ImRegion(page, ImLayoutObject.getAggregateBox(mergeCols), ImRegion.TABLE_COL_TYPE);
		
		//	collect cells in merged columns
		ImRegion[] mColRows = getRegionsOverlapping(page, mCol.bounds, ImRegion.TABLE_ROW_TYPE);
		Arrays.sort(mColRows, ImUtils.topDownOrder);
		ImRegion[] mColCells = getRegionsOverlapping(page, mCol.bounds, ImRegion.TABLE_CELL_TYPE);
		Arrays.sort(mColCells, ImUtils.topDownOrder);
		ImRegion[][] mergeColCells = new ImRegion[mColRows.length][mergeCols.length];
		for (int r = 0; r < mColRows.length; r++)
			for (int c = 0; c < mergeCols.length; c++) {
				for (int l = 0; l < mColCells.length; l++)
					if (mColCells[l].bounds.overlaps(mColRows[r].bounds) && mColCells[l].bounds.overlaps(mergeCols[c].bounds)) {
						mergeColCells[r][c] = mColCells[l];
						break;
					}
				//	TODO make this nested loop join faster, somehow
			}
		
		//	create merged cells row-wise
		for (int r = 0; r < mColRows.length; r++) {
			int lr = r;
			
			//	find row with straight bottom
			while ((lr+1) < mColRows.length) {
				boolean rowClosed = true;
				for (int c = 0; c < mergeCols.length; c++)
					if ((mergeColCells[lr][c] != null) && mergeColCells[lr][c].bounds.overlaps(mColRows[lr+1].bounds)) {
						rowClosed = false;
						break;
					}
				if (rowClosed)
					break;
				else lr++;
			}
			
			//	aggregate merged cell bounds
			int mCellLeft = mCol.bounds.left;
			int mCellRight = mCol.bounds.right;
			for (int ar = r; ar <= lr; ar++) {
				for (int c = 0; c < mergeCols.length; c++)
					if (mergeColCells[ar][c] != null){
						mCellLeft = Math.min(mCellLeft, mergeColCells[ar][c].bounds.left);
						mCellRight = Math.max(mCellRight, mergeColCells[ar][c].bounds.right);
					}
			}
			BoundingBox mCellBounds = new BoundingBox(mCellLeft, mCellRight, mColRows[r].bounds.top, mColRows[lr].bounds.bottom);
			
			//	remove merged cells
			ImRegion[] mCellCells = getRegionsOverlapping(page, mCellBounds, ImRegion.TABLE_CELL_TYPE);
			for (int c = 0; c < mCellCells.length; c++)
				page.removeRegion(mCellCells[c]);
			
			//	add merged cell
			this.markTableCell(page, mCellBounds, true, false);
			
			//	jump to last row (loop increment will switch one further)
			r = lr;
		}
		
		//	remove merged columns
		for (int c = 0; c < mergeCols.length; c++)
			page.removeRegion(mergeCols[c]);
		
		//	update cells (whichever we might have cut apart in columns to left or right of merger)
		ImRegion[][] tableCells = this.markTableCells(page, table);
		
		//	clean up table structure
		ImUtils.orderTableWords(tableCells);
		this.cleanupTableAnnotations(table.getDocument(), table);
	}
	
	/**
	 * Split a table column. Depending on the splitting box, the argument table
	 * column is split into two (splitting off left or right) or three (splitting
	 * down the middle) new columns.
	 * @param page the page the table lies upon
	 * @param table the table the column belongs to
	 * @param toSplitCol the table column to split
	 * @param splitBox the box defining where to split
	 * @return true if the table was modified
	 */
	boolean splitTableCol(ImPage page, ImRegion table, ImRegion toSplitCol, BoundingBox splitBox) {
		return this.splitTableCol(page, table, null, toSplitCol, splitBox);
	}
	
	/**
	 * Split a table column. Depending on the splitting box, the argument table
	 * column is split into two (splitting off left or right) or three (splitting
	 * down the middle) new columns.
	 * @param page the page the table lies upon
	 * @param table the table the column belongs to
	 * @param tableStats statistics on the distribution of words in the argument
	 *            table
	 * @param toSplitCol the table column to split
	 * @param splitBox the box defining where to split
	 * @return true if the table was modified
	 */
	boolean splitTableCol(ImPage page, ImRegion table, TableAreaStatistics tableStats, ImRegion toSplitCol, BoundingBox splitBox) {
		ImRegion[] tables = ImUtils.getColumnConnectedTables(table);
		
		//	compute table area statistics if not given
		ImWord[] tableWords = table.getWords();
		if (tableStats == null)
			tableStats = TableAreaStatistics.getTableAreaStatistics(page, tableWords);
		
		//	aggregate words to blocks, bridging spaces to prevent mistaking them for column gaps
		WordBlock[] toSplitColWords = this.getWordBlocksForColSplit(tableWords, toSplitCol.bounds, tableStats.normSpaceWidth);
		
		//	compute relative split bounds
		int relLeftSplitRight = 0;
		int relSplitLeft = (splitBox.right - toSplitCol.bounds.left);
		int relSplitRight = (splitBox.left - toSplitCol.bounds.left);
		int relRightSplitLeft = toSplitCol.bounds.getWidth();
		
		//	order words left of, inside, and right of selection
		for (int w = 0; w < toSplitColWords.length; w++) {
			if (toSplitColWords[w].centerX < splitBox.left)
				relLeftSplitRight = Math.max(relLeftSplitRight, (toSplitColWords[w].bounds.right - toSplitCol.bounds.left));
			else if (toSplitColWords[w].centerX < splitBox.right) {
				relSplitLeft = Math.min(relSplitLeft, (toSplitColWords[w].bounds.left - toSplitCol.bounds.left));
				relSplitRight = Math.max(relSplitRight, (toSplitColWords[w].bounds.right - toSplitCol.bounds.left));
			}
			else relRightSplitLeft = Math.min(relRightSplitLeft, (toSplitColWords[w].bounds.left - toSplitCol.bounds.left));
		}
		
		//	anything to split at all?
		int emptySplitCols = 0;
		if (relLeftSplitRight == 0)
			emptySplitCols++;
		if (relSplitRight <= relSplitLeft)
			emptySplitCols++;
		if (relRightSplitLeft == toSplitCol.bounds.getWidth())
			emptySplitCols++;
		if (emptySplitCols >= 2)
			return false;
		
		//	check if we have words obstructing the split
		HashSet multiColWords = null;
		if ((relSplitLeft < relLeftSplitRight) || (relRightSplitLeft < relSplitRight)) {
			
			//	determine right edge of all words _completely_ to left of selection ...
			//	... and left edge of all words _completely_ to right of selection ...
			int leftSplitRight = toSplitCol.bounds.left;
			int rightSplitLeft = toSplitCol.bounds.right;
			ArrayList insideSplitWords = new ArrayList();
			for (int w = 0; w < toSplitColWords.length; w++) {
				if (toSplitColWords[w].bounds.right <= splitBox.left)
					leftSplitRight = Math.max(leftSplitRight, toSplitColWords[w].bounds.right);
				else if (splitBox.right <= toSplitColWords[w].bounds.left)
					rightSplitLeft = Math.min(rightSplitLeft, toSplitColWords[w].bounds.left);
				else insideSplitWords.add(toSplitColWords[w]);
			}
			
			//	mark all remaining words that protrude beyond (or even only close to ???) either of these edges as multi-column
			//	... and determine boundaries of middle column from the rest
			//	TODO depending on average column gap, be even more sensitive about multi-column cells ...
			//	... as they might end up failing to completely reach edge of neighbor column and only get close
			int splitLeft = splitBox.right;
			int splitRight = splitBox.left;
			multiColWords = new HashSet();
			for ( int w = 0; w < insideSplitWords.size(); w++) {
				WordBlock isWord = ((WordBlock) insideSplitWords.get(w));
				if ((toSplitCol.bounds.left < leftSplitRight) && (isWord.bounds.left <= leftSplitRight))
					multiColWords.add(isWord);
				else if ((rightSplitLeft < toSplitCol.bounds.right) && (rightSplitLeft <= isWord.bounds.right))
					multiColWords.add(isWord);
				else {
					splitLeft = Math.min(splitLeft, isWord.bounds.left);
					splitRight = Math.max(splitRight, isWord.bounds.right);
				}
			}
			
			//	re-compute relative split bounds
			relLeftSplitRight = (leftSplitRight - toSplitCol.bounds.left);
			relSplitLeft = (splitLeft - toSplitCol.bounds.left);
			relSplitRight = (splitRight - toSplitCol.bounds.left);
			relRightSplitLeft = (rightSplitLeft - toSplitCol.bounds.left);
		}
		
		//	left side of split empty ==> move out left edge of middle
		if (relLeftSplitRight == 0)
			relSplitLeft = 0;
		
		//	right side of split empty ==> move out right edge of middle
		if (relRightSplitLeft == toSplitCol.bounds.getWidth())
			relSplitRight = toSplitCol.bounds.getWidth();
		
		//	cut effort short in basic case
		if (tables.length == 1) {
			this.doSplitTableCol(table.getPage(), table, toSplitCol, toSplitColWords, multiColWords, relLeftSplitRight, relSplitLeft, relSplitRight, relRightSplitLeft);
			return true;
		}
		
		//	compute table-relative split area
		int relToSplitColLeft = (toSplitCol.bounds.left - table.bounds.left);
		int relToSplitColRight = (toSplitCol.bounds.right - table.bounds.left);
		
		//	get corresponding columns in connected tables
		System.out.println("Collecting to-split columns ...");
		System.out.println(" - relative split area is " + relToSplitColLeft + " to " + relToSplitColRight);
		ImRegion[] toSplitTableCols = new ImRegion[tables.length];
		for (int t = 0; t < tables.length; t++) {
			System.out.println(" - from " + tables[t].pageId + "." + tables[t].bounds);
			
			//	this one's clear
			if (tables[t] == table) {
				System.out.println(" --> original selection");
				toSplitTableCols[t] = toSplitCol;
				continue;
			}
			
			//	collect rows inside table-relative merge area
			ImRegion[] tableCols = ImUtils.getTableColumns(tables[t]);
			for (int c = 0; c < tableCols.length; c++) {
				int relColCenter = (((tableCols[c].bounds.left + tableCols[c].bounds.right) / 2) - tables[t].bounds.left);
				System.out.println("   - row " + tableCols[c].bounds + ", relative center is " + relColCenter);
				if ((relToSplitColLeft < relColCenter) && (relColCenter < relToSplitColRight)) {
					System.out.println("   --> inside selection");
					if (toSplitTableCols[t] == null)
						toSplitTableCols[t] = tableCols[c];
					else {
						System.out.println(" ==> duplicate match, split not possible");
						return false;
					}
				}
				else System.out.println("   --> outside selection");
			}
		}
		System.out.println(" ==> found all to-split columns");
		
		//	perform split
		float relLeftMiddleSplit = (((float) (relLeftSplitRight + relSplitLeft)) / (2 * (toSplitCol.bounds.right - toSplitCol.bounds.left)));
		float relMiddleRightSplit = (((float) (relSplitRight + relRightSplitLeft)) / (2 * (toSplitCol.bounds.right - toSplitCol.bounds.left)));
		for (int t = 0; t < tables.length; t++) {
			if (tables[t] == table)
				this.doSplitTableCol(tables[t].getPage(), tables[t], toSplitTableCols[t], toSplitColWords, multiColWords, relLeftSplitRight, relSplitLeft, relSplitRight, relRightSplitLeft);
			else this.doSplitTableCol(tables[t], toSplitTableCols[t], relLeftMiddleSplit, relMiddleRightSplit, tableStats.normSpaceWidth);
		}
		return true;
	}
	
	private void doSplitTableCol(ImRegion table, ImRegion toSplitCol, float relLeftMiddleSplit, float relMiddleRightSplit, float normSpaceWidth) {
		
		//	compute relative split box
		int splitBoxLeft = (((int) (relLeftMiddleSplit * (toSplitCol.bounds.right - toSplitCol.bounds.left))) + toSplitCol.bounds.left);
		int splitBoxRight = (((int) (relMiddleRightSplit * (toSplitCol.bounds.right - toSplitCol.bounds.left))) + toSplitCol.bounds.left);
		
		//	compute relative split bounds
		int relLeftSplitRight = 0;
		int relSplitLeft = (splitBoxRight - toSplitCol.bounds.left);
		int relSplitRight = (splitBoxLeft - toSplitCol.bounds.left);
		int relRightSplitLeft = (toSplitCol.bounds.right - toSplitCol.bounds.left);
		
		//	order words left of, inside, and right of selection
		WordBlock[] toSplitColWords = this.getWordBlocksForColSplit(table.getWords(), toSplitCol.bounds, normSpaceWidth);
		for (int w = 0; w < toSplitColWords.length; w++) {
			if (toSplitColWords[w].centerX < splitBoxLeft)
				relLeftSplitRight = Math.max(relLeftSplitRight, (toSplitColWords[w].bounds.right - toSplitCol.bounds.left));
			else if (toSplitColWords[w].centerX < splitBoxRight) {
				relSplitLeft = Math.min(relSplitLeft, (toSplitColWords[w].bounds.left - toSplitCol.bounds.left));
				relSplitRight = Math.max(relSplitRight, (toSplitColWords[w].bounds.right - toSplitCol.bounds.left));
			}
			else relRightSplitLeft = Math.min(relRightSplitLeft, (toSplitColWords[w].bounds.left - toSplitCol.bounds.left));
		}
		
		//	check if we have words obstructing the split
		HashSet multiColWords = null;
		if ((relSplitLeft < relLeftSplitRight) || (relRightSplitLeft < relSplitRight)) {
			
			//	determine right edge of all words _completely_ to left of selection ...
			//	... and left edge of all words _completely_ to right of selection ...
			int leftSplitRight = toSplitCol.bounds.left;
			int rightSplitLeft = toSplitCol.bounds.right;
			ArrayList insideSplitWords = new ArrayList();
			for (int w = 0; w < toSplitColWords.length; w++) {
				if (toSplitColWords[w].bounds.right <= splitBoxLeft)
					leftSplitRight = Math.max(leftSplitRight, toSplitColWords[w].bounds.right);
				else if (splitBoxRight <= toSplitColWords[w].bounds.left)
					rightSplitLeft = Math.min(rightSplitLeft, toSplitColWords[w].bounds.left);
				else insideSplitWords.add(toSplitColWords[w]);
			}
			
			//	mark all remaining words that protrude beyond either of these edges as multi-column
			//	... and determine boundaries of middle column from the rest
			//	TODO depending on average column gap, be even more sensitive about multi-column cells ...
			//	... as they might end up failing to completely reach edge of neighbor column and only get close
			int splitLeft = splitBoxRight;
			int splitRight = splitBoxLeft;
			multiColWords = new HashSet();
			for ( int w = 0; w < insideSplitWords.size(); w++) {
				WordBlock isWord = ((WordBlock) insideSplitWords.get(w));
				if ((toSplitCol.bounds.left < leftSplitRight) && (isWord.bounds.left <= leftSplitRight))
					multiColWords.add(isWord);
				else if ((rightSplitLeft < toSplitCol.bounds.right) && (rightSplitLeft <= isWord.bounds.right))
					multiColWords.add(isWord);
				else {
					splitLeft = Math.min(splitLeft, isWord.bounds.left);
					splitRight = Math.max(splitRight, isWord.bounds.right);
				}
			}
			
			//	re-compute relative split bounds
			relLeftSplitRight = (leftSplitRight - toSplitCol.bounds.left);
			relSplitLeft = (splitLeft - toSplitCol.bounds.left);
			relSplitRight = (splitRight - toSplitCol.bounds.left);
			relRightSplitLeft = (rightSplitLeft - toSplitCol.bounds.left);
		}
		
		//	do split with case adjusted absolute numbers
		this.doSplitTableCol(table.getPage(), table, toSplitCol, toSplitColWords, multiColWords, relLeftSplitRight, relSplitLeft, relSplitRight, relRightSplitLeft);
	}
	
	private void doSplitTableCol(ImPage page, ImRegion table, ImRegion toSplitCol, WordBlock[] toSplitColWords, HashSet multiColWords, int relLeftSplitRight, int relSplitLeft, int relSplitRight, int relRightSplitLeft) {
		
		//	create two or three new columns
		if (0 < relLeftSplitRight) {
			BoundingBox lcBox = new BoundingBox(toSplitCol.bounds.left, (toSplitCol.bounds.left + relLeftSplitRight), table.bounds.top, table.bounds.bottom);
			new ImRegion(table.getPage(), lcBox, ImRegion.TABLE_COL_TYPE);
		}
		if (relSplitLeft < relSplitRight) {
			BoundingBox icBox = new BoundingBox((toSplitCol.bounds.left + relSplitLeft), (toSplitCol.bounds.left + relSplitRight), table.bounds.top, table.bounds.bottom);
			new ImRegion(table.getPage(), icBox, ImRegion.TABLE_COL_TYPE);
		}
		if (relRightSplitLeft < (toSplitCol.bounds.right - toSplitCol.bounds.left)) {
			BoundingBox rcBox = new BoundingBox((toSplitCol.bounds.left + relRightSplitLeft), toSplitCol.bounds.right, table.bounds.top, table.bounds.bottom);
			new ImRegion(table.getPage(), rcBox, ImRegion.TABLE_COL_TYPE);
		}
		
		//	remove selected column
		page.removeRegion(toSplitCol);
		
		//	get split columns and existing cells
		ImRegion[] sCols = getRegionsInside(page, toSplitCol.bounds, ImRegion.TABLE_COL_TYPE, false);
		Arrays.sort(sCols, ImUtils.leftRightOrder);
		ImRegion[] toSplitColCells = getRegionsOverlapping(page, toSplitCol.bounds, ImRegion.TABLE_CELL_TYPE);
		Arrays.sort(toSplitColCells, ImUtils.topDownOrder);
		
		//	create new cells left to right
		for (int l = 0; l < toSplitColCells.length; l++) {
			
			//	handle base case, saving all the hassle below
			if (multiColWords == null) {
				
				//	create split cells
				for (int c = 0; c < sCols.length; c++) {
					int sCellLeft = ((c == 0) ? toSplitColCells[l].bounds.left : sCols[c].bounds.left);
					int sCellRight = (((c+1) == sCols.length) ? toSplitColCells[l].bounds.right : sCols[c].bounds.right);
					this.markTableCell(page, new BoundingBox(sCellLeft, sCellRight, toSplitColCells[l].bounds.top, toSplitColCells[l].bounds.bottom), false, true);
				}
				
				//	clean up and we're done here
				page.removeRegion(toSplitColCells[l]);
				continue;
			}
			
			//	check for multi-column cells
			boolean[] cellReachesIntoNextCol = new boolean[sCols.length - 1];
			Arrays.fill(cellReachesIntoNextCol, false);
			for (int c = 0; c < (sCols.length - 1); c++) {
				WordBlock[] sCellWords = this.getWordBlocksOverlapping(toSplitColWords, new BoundingBox(sCols[c].bounds.left, sCols[c+1].bounds.right, toSplitColCells[l].bounds.top, toSplitColCells[l].bounds.bottom));
				if (sCellWords.length == 0)
					continue;
				Arrays.sort(sCellWords, ImUtils.leftRightOrder);
				for (int w = 0; w < sCellWords.length; w++) {
					if (sCols[c+1].bounds.left < sCellWords[w].bounds.left)
						break; // we're only interested in words overlapping with space between the current pair of columns
					if (multiColWords.contains(sCellWords[w])) {
						cellReachesIntoNextCol[c] = true;
						break; // one word overlapping both columns is enough
					}
				}
			}
			
			//	create cells for split columns
			int sCellLeft = Math.min(toSplitColCells[l].bounds.left, sCols[0].bounds.left);
			for (int c = 0; c < sCols.length; c++) {
				if ((c < cellReachesIntoNextCol.length) && cellReachesIntoNextCol[c])
					continue;
				if ((c+1) == sCols.length)
					this.markTableCell(page, new BoundingBox(sCellLeft, toSplitColCells[l].bounds.right, toSplitColCells[l].bounds.top, toSplitColCells[l].bounds.bottom), false, true);
				else {
					this.markTableCell(page, new BoundingBox(sCellLeft, sCols[c].bounds.right, toSplitColCells[l].bounds.top, toSplitColCells[l].bounds.bottom), false, true);
					sCellLeft = sCols[c+1].bounds.left;
				}
			}
			
			//	clean up
			page.removeRegion(toSplitColCells[l]);
		}
		
		//	clean up table structure
		ImUtils.orderTableWords(this.markTableCells(page, table));
		this.cleanupTableAnnotations(table.getDocument(), table);
	}
	
	private WordBlock[] getWordBlocksForColSplit(ImWord[] tableWords, BoundingBox bounds, float normSpaceWidth) {
		
		//	get and sort words
		ImWord[] words = getWordsOverlapping(tableWords, bounds);
		ImUtils.sortLeftRightTopDown(words);
		
		//	aggregate words to sequences
		ArrayList wbs = new ArrayList();
		int wbStart = 0;
		for (int w = 1; w <= words.length; w++) {
			if ((w == words.length) || this.isColumnGap(words[w-1], words[w], normSpaceWidth)) {
				ImWord[] wbWords = new ImWord[w - wbStart];
				System.arraycopy(words, wbStart, wbWords, 0, wbWords.length);
				WordBlock wb = new WordBlock(wbWords[0].getPage(), wbWords, true);
//				System.out.println("Word block: " + wb + " at " + wb.bounds);
				wbs.add(wb);
				wbStart = w;
			}
		}
		
		//	finally ...
		return ((WordBlock[]) wbs.toArray(new WordBlock[wbs.size()]));
	}
	
	private boolean isColumnGap(ImWord word1, ImWord word2, float normSpaceWidth) {
		if (word1.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
			return true; // explicit end marker
		if (word1.bounds.bottom < word2.centerY)
			return true; // next line
		if (word1.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE)
			return false; // explicit continuation marker
		if (word1.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED)
			return false; // explicit continuation marker
		int lineHeight = ((word1.bounds.getHeight() + word2.bounds.getHeight()) / 2);
		int wordDist = (word2.bounds.left - word1.bounds.right);
//		return ((wordDist * 3) > lineHeight);
		return ((lineHeight * normSpaceWidth) < wordDist);
	}
	
	private WordBlock[] getWordBlocksOverlapping(WordBlock[] words, BoundingBox box) {
		ArrayList wbs = new ArrayList();
		for (int w = 0; w < words.length; w++) {
			if (words[w].bounds.overlaps(box))
				wbs.add(words[w]);
		}
		return ((WordBlock[]) wbs.toArray(new WordBlock[wbs.size()]));
	}
	
	private static class WordBlock extends ImRegion {
		final int centerX;
		final int centerY;
		final ImWord[] words;
		final boolean forColSplit;
		WordBlock(ImPage page, ImWord[] words, boolean forColSplit) {
			super(page.getDocument(), page.pageId, ImLayoutObject.getAggregateBox(words), "wordBlock");
			this.centerX = ((this.bounds.left + this.bounds.right) / 2);
			this.centerY = ((this.bounds.top + this.bounds.bottom) / 2);
			this.words = words;
			this.forColSplit = forColSplit;
		}
		public String toString() {
			return ImUtils.getString(this.words[0], this.words[this.words.length-1], this.forColSplit);
		}
	}
	
	/**
	 * Merge up a bunch of table cells.
	 * @param table the table the cells belong to
	 * @param mergeCells the cells to merge
	 * @return true if the table was modified
	 */
	boolean mergeTableCells(ImRegion table, ImRegion[] mergeCells) {
		ImPage page = table.getPage();
		
		//	get aggregate cell bounds (transitive hull, to keep cells rectangular)
		BoundingBox mCellBounds = ImLayoutObject.getAggregateBox(mergeCells);
		while (true) {
			mergeCells = getRegionsOverlapping(page, mCellBounds, ImRegion.TABLE_CELL_TYPE);
			BoundingBox eMergedBounds = ImLayoutObject.getAggregateBox(mergeCells);
			if (mCellBounds.equals(eMergedBounds))
				break;
			else mCellBounds = eMergedBounds;
		}
		
		//	clean up factually merged cells (not only originally selected ones, but all in transitive hull of merge result)
		mergeCells = getRegionsInside(page, mCellBounds, ImRegion.TABLE_CELL_TYPE, false);
		for (int c = 0; c < mergeCells.length; c++)
			page.removeRegion(mergeCells[c]);
		
		//	mark aggregate cell
		ImRegion mCell = new ImRegion(page, mCellBounds, ImRegion.TABLE_CELL_TYPE);
		
		//	shrink affected rows and remaining fully contained cells
		ImRegion[] mCellRows = getRegionsOverlapping(page, mCell.bounds, ImRegion.TABLE_ROW_TYPE);
		Arrays.sort(mCellRows, ImUtils.topDownOrder);
		if (mCellRows.length > 1)
			for (int r = 0; r < mCellRows.length; r++) {
				
				//	assess words contained in cells that do not reach beyond row
				ImRegion[] mCellRowCells = getRegionsOverlapping(page, mCellRows[r].bounds, ImRegion.TABLE_CELL_TYPE);
				int mCellRowTop = mCellRows[r].bounds.bottom;
				int mCellRowBottom = mCellRows[r].bounds.top;
				for (int l = 0; l < mCellRowCells.length; l++) {
					if (mCell.bounds.overlaps(mCellRowCells[l].bounds))
						continue; // no use considering merge result here
					ImWord[] mCellRowCellWords = page.getWordsInside(mCellRowCells[l].bounds);
					for (int w = 0; w < mCellRowCellWords.length; w++) {
						if (mCellRows[r].bounds.top <= mCellRowCells[l].bounds.top)
							mCellRowTop = Math.min(mCellRowTop, mCellRowCellWords[w].bounds.top);
						if (mCellRowCells[l].bounds.bottom <= mCellRows[r].bounds.bottom)
							mCellRowBottom = Math.max(mCellRowBottom, mCellRowCellWords[w].bounds.bottom);
					}
				}
				if ((mCellRowTop <= mCellRows[r].bounds.top) && (mCellRows[r].bounds.bottom <= mCellRowBottom))
					continue; // nothing to shrink here
				
				//	don't shrink outside edges of edge rows
				if (r == 0)
					mCellRowTop = Math.min(mCellRowTop, mCellRows[r].bounds.top);
				if (r == (mCellRows.length - 1))
					mCellRowBottom = Math.max(mCellRowBottom, mCellRows[r].bounds.bottom);
				
				//	compute reduced bounds
				BoundingBox mCellRowBox = new BoundingBox(table.bounds.left, table.bounds.right, mCellRowTop, mCellRowBottom);
				
				//	shrink cells to reduced row height
				for (int l = 0; l < mCellRowCells.length; l++) {
					if (mCell.bounds.overlaps(mCellRowCells[l].bounds))
						continue; // don't shrink merge result
					page.removeRegion(mCellRowCells[l]);
					BoundingBox mCellRowCellBox = new BoundingBox(
							mCellRowCells[l].bounds.left,
							mCellRowCells[l].bounds.right,
							((mCellRows[r].bounds.top <= mCellRowCells[l].bounds.top) ? mCellRowTop : mCellRowCells[l].bounds.top),
							((mCellRowCells[l].bounds.bottom <= mCellRows[r].bounds.bottom) ? mCellRowBottom : mCellRowCells[l].bounds.bottom)
						);
					mCellRowCells[l] = new ImRegion(page, mCellRowCellBox, ImRegion.TABLE_CELL_TYPE);
 				}
				
				//	shrink row proper
				page.removeRegion(mCellRows[r]);
				mCellRows[r] = new ImRegion(page, mCellRowBox, ImRegion.TABLE_ROW_TYPE);
			}
		
		//	shrink affected columns to remaining fully contained cells
		ImRegion[] mCellCols = getRegionsOverlapping(page, mCell.bounds, ImRegion.TABLE_COL_TYPE);
		Arrays.sort(mCellCols, ImUtils.leftRightOrder);
		if (mCellCols.length > 1)
			for (int c = 0; c < mCellCols.length; c++) {
				
				//	assess words contained in cells that do not reach beyond column
				ImRegion[] mCellColCells = getRegionsOverlapping(page, mCellCols[c].bounds, ImRegion.TABLE_CELL_TYPE);
				int mCellColLeft = mCellCols[c].bounds.right;
				int mCellColRight = mCellCols[c].bounds.left;
				for (int l = 0; l < mCellColCells.length; l++) {
					if (mCell.bounds.overlaps(mCellColCells[l].bounds))
						continue; // no use considering merge result here
					ImWord[] mCellRowCellWords = page.getWordsInside(mCellColCells[l].bounds);
					for (int w = 0; w < mCellRowCellWords.length; w++) {
						if (mCellCols[c].bounds.left <= mCellColCells[l].bounds.left)
							mCellColLeft = Math.min(mCellColLeft, mCellRowCellWords[w].bounds.left);
						if (mCellColCells[l].bounds.right <= mCellCols[c].bounds.right)
							mCellColRight = Math.max(mCellColRight, mCellRowCellWords[w].bounds.right);
					}
				}
				
				//	don't shrink outside edges of edge columns
				if (c == 0)
					mCellColLeft = Math.min(mCellColLeft, mCellCols[c].bounds.left);
				if (c == (mCellCols.length - 1))
					mCellColRight = Math.max(mCellColRight, mCellCols[c].bounds.right);
				
				//	anything to shrink at all?
				if ((mCellColLeft <= mCellCols[c].bounds.left) && (mCellCols[c].bounds.right <= mCellColRight))
					continue;
				
				//	compute reduced bounds
				BoundingBox mCellColBox = new BoundingBox(mCellColLeft, mCellColRight, table.bounds.top, table.bounds.bottom);
				
				//	shrink cells to reduced row height
				for (int l = 0; l < mCellColCells.length; l++) {
					if (mCell.bounds.overlaps(mCellColCells[l].bounds))
						continue; // don't shrink merge result
					page.removeRegion(mCellColCells[l]);
					BoundingBox mCellColCellBox = new BoundingBox(
							((mCellCols[c].bounds.left <= mCellColCells[l].bounds.left) ? mCellColLeft : mCellColCells[l].bounds.left),
							((mCellColCells[l].bounds.right <= mCellCols[c].bounds.right) ? mCellColRight : mCellColCells[l].bounds.right),
							mCellColCells[l].bounds.top,
							mCellColCells[l].bounds.bottom
						);
					mCellColCells[l] = new ImRegion(page, mCellColCellBox, ImRegion.TABLE_CELL_TYPE);
 				}
				
				//	shrink column proper
				page.removeRegion(mCellCols[c]);
				mCellCols[c] = new ImRegion(page, mCellColBox, ImRegion.TABLE_COL_TYPE);
			}
		
		//	update table
		ImUtils.orderTableWords(this.markTableCells(page, table));
		this.cleanupTableAnnotations(table.getDocument(), table);
		
		//	indicate change
		return true;
	}
	
	private boolean splitTableCellRows(ImRegion table, ImRegion toSplitCell, ImRegion[] toSplitCellRows, ImRegion[] toSplitCellCols, BoundingBox splitBox) {
		Arrays.sort(toSplitCellRows, ImUtils.topDownOrder);
		Arrays.sort(toSplitCellCols, ImUtils.leftRightOrder);
		ImPage page = table.getPage();
		
		//	order words above, inside, and below selection
		ArrayList aboveSplitRows = new ArrayList();
		ArrayList splitRows = new ArrayList();
		ArrayList belowSplitRows = new ArrayList();
		for (int r = 0; r < toSplitCellRows.length; r++) {
			int rowCenterY = ((toSplitCellRows[r].bounds.top + toSplitCellRows[r].bounds.bottom) / 2);
			if (rowCenterY < splitBox.top)
				aboveSplitRows.add(toSplitCellRows[r]);
			else if (rowCenterY < splitBox.bottom)
				splitRows.add(toSplitCellRows[r]);
			else belowSplitRows.add(toSplitCellRows[r]);
		}
		
		//	anything to split at all?
		int emptySplitRows = 0;
		if (aboveSplitRows.isEmpty())
			emptySplitRows++;
		if (splitRows.isEmpty())
			emptySplitRows++;
		if (belowSplitRows.isEmpty())
			emptySplitRows++;
		if (emptySplitRows >= 2)
			return false;
		
		//	remove selected cell
		page.removeRegion(toSplitCell);
		
		//	sort words in to-split cell
		ImWord[] toSplitCellWords = page.getWordsInside(toSplitCell.bounds);
		
		//	perform split
		if (aboveSplitRows.size() != 0) {
			ImRegion aboveSplitBottomRow = ((ImRegion) aboveSplitRows.get(aboveSplitRows.size() - 1));
			int aboveSplitCellTop = ((ImRegion) aboveSplitRows.get(0)).bounds.top;
			int aboveSplitCellBottom = aboveSplitBottomRow.bounds.bottom;
			for (int w = 0; w < toSplitCellWords.length; w++) {
				if (toSplitCellWords[w].centerY < splitBox.top)
					aboveSplitCellBottom = Math.max(aboveSplitCellBottom, toSplitCellWords[w].bounds.bottom);
			}
			
			BoundingBox ascBox = new BoundingBox(toSplitCell.bounds.left, toSplitCell.bounds.right, aboveSplitCellTop, aboveSplitCellBottom);
			this.markTableCell(page, ascBox, true, (aboveSplitRows.size() == 1));
			
			if (aboveSplitBottomRow.bounds.bottom < aboveSplitCellBottom) {
				page.removeRegion(aboveSplitBottomRow);
				aboveSplitBottomRow = new ImRegion(page, new BoundingBox(aboveSplitBottomRow.bounds.left, aboveSplitBottomRow.bounds.right, aboveSplitBottomRow.bounds.top, aboveSplitCellBottom), ImRegion.TABLE_ROW_TYPE);
				
				ImRegion[] aboveSplitBottomCells = getRegionsOverlapping(page, aboveSplitBottomRow.bounds, ImRegion.TABLE_CELL_TYPE);
				for (int c = 0; c < aboveSplitBottomCells.length; c++) {
					if (aboveSplitCellBottom <= aboveSplitBottomCells[c].bounds.bottom)
						continue; // multi-row cell that reaches below split, no need for expanding
					page.removeRegion(aboveSplitBottomCells[c]);
					ascBox = new BoundingBox(aboveSplitBottomCells[c].bounds.left, aboveSplitBottomCells[c].bounds.right, aboveSplitBottomCells[c].bounds.top, aboveSplitCellBottom);
					this.markTableCell(page, ascBox, true, (aboveSplitRows.size() == 1));
				}
			}
		}
		
		if (splitRows.size() != 0) {
			ImRegion splitTopRow = ((ImRegion) splitRows.get(0));
			ImRegion splitBottomRow = ((ImRegion) splitRows.get(splitRows.size() - 1));
			int splitCellTop = splitTopRow.bounds.top;
			int splitCellBottom = splitBottomRow.bounds.bottom;
			for (int w = 0; w < toSplitCellWords.length; w++)
				if ((splitBox.top <= toSplitCellWords[w].centerY) && (toSplitCellWords[w].centerY < splitBox.bottom)) {
					splitCellTop = Math.min(splitCellTop, toSplitCellWords[w].bounds.top);
					splitCellBottom = Math.max(splitCellBottom, toSplitCellWords[w].bounds.bottom);
				}
			
			BoundingBox iscBox = new BoundingBox(toSplitCell.bounds.left, toSplitCell.bounds.right, splitCellTop, splitCellBottom);
			this.markTableCell(page, iscBox, true, (splitRows.size() == 1));
			
			if (splitTopRow == splitBottomRow) {
				if ((splitCellTop < splitTopRow.bounds.top) || (splitTopRow.bounds.bottom < splitCellBottom)) {
					page.removeRegion(splitTopRow);
					splitTopRow = new ImRegion(page, new BoundingBox(splitTopRow.bounds.left, splitTopRow.bounds.right, Math.min(splitCellTop, splitTopRow.bounds.top), Math.max(splitCellBottom, splitTopRow.bounds.bottom)), ImRegion.TABLE_ROW_TYPE);
					
					ImRegion[] splitCells = getRegionsOverlapping(page, splitTopRow.bounds, ImRegion.TABLE_CELL_TYPE);
					for (int c = 0; c < splitCells.length; c++) {
						if ((splitCells[c].bounds.top <= splitCellTop) && (splitCellBottom <= splitCells[c].bounds.bottom))
							continue; // multi-row cell that reaches above and below split, no need for expanding
						page.removeRegion(splitCells[c]);
						iscBox = new BoundingBox(splitCells[c].bounds.left, splitCells[c].bounds.right, Math.min(splitCellTop, splitCells[c].bounds.top), Math.max(splitCellBottom, splitCells[c].bounds.bottom));
						this.markTableCell(page, iscBox, true, (splitRows.size() == 1));
					}
				}
			}
			else {
				if (splitCellTop < splitTopRow.bounds.top) {
					page.removeRegion(splitTopRow);
					splitTopRow = new ImRegion(page, new BoundingBox(splitTopRow.bounds.left, splitTopRow.bounds.right, splitCellTop, splitTopRow.bounds.bottom), ImRegion.TABLE_ROW_TYPE);
					
					ImRegion[] splitTopCells = getRegionsOverlapping(page, splitTopRow.bounds, ImRegion.TABLE_CELL_TYPE);
					for (int c = 0; c < splitTopCells.length; c++) {
						if (splitTopCells[c].bounds.top <= splitCellTop)
							continue; // multi-row cell that reaches above split, no need for expanding
						page.removeRegion(splitTopCells[c]);
						iscBox = new BoundingBox(splitTopCells[c].bounds.left, splitTopCells[c].bounds.right, splitCellTop, splitTopCells[c].bounds.bottom);
						this.markTableCell(page, iscBox, true, (belowSplitRows.size() == 1));
					}
				}
				if (splitBottomRow.bounds.bottom < splitCellBottom) {
					page.removeRegion(splitBottomRow);
					splitBottomRow = new ImRegion(page, new BoundingBox(splitBottomRow.bounds.left, splitBottomRow.bounds.right, splitBottomRow.bounds.top, splitCellBottom), ImRegion.TABLE_ROW_TYPE);
					
					ImRegion[] splitBottomCells = getRegionsOverlapping(page, splitBottomRow.bounds, ImRegion.TABLE_CELL_TYPE);
					for (int c = 0; c < splitBottomCells.length; c++) {
						if (splitCellBottom <= splitBottomCells[c].bounds.bottom)
							continue; // multi-row cell that reaches below split, no need for expanding
						page.removeRegion(splitBottomCells[c]);
						iscBox = new BoundingBox(splitBottomCells[c].bounds.left, splitBottomCells[c].bounds.right, splitBottomCells[c].bounds.top, splitCellBottom);
						this.markTableCell(page, iscBox, true, (aboveSplitRows.size() == 1));
					}
				}
			}
		}
		
		if (belowSplitRows.size() != 0) {
			ImRegion belowSplitTopRow = ((ImRegion) belowSplitRows.get(0));
			int belowSplitCellTop = belowSplitTopRow.bounds.top;
			int belowSplitCellBottom = ((ImRegion) belowSplitRows.get(belowSplitRows.size() - 1)).bounds.bottom;
			for (int w = 0; w < toSplitCellWords.length; w++) {
				if (splitBox.bottom <= toSplitCellWords[w].centerY)
					belowSplitCellTop = Math.min(belowSplitCellTop, toSplitCellWords[w].bounds.top);
			}
			
			BoundingBox bscBox = new BoundingBox(toSplitCell.bounds.left, toSplitCell.bounds.right, belowSplitCellTop, belowSplitCellBottom);
			this.markTableCell(page, bscBox, true, (belowSplitRows.size() == 1));
			
			if (belowSplitCellTop < belowSplitTopRow.bounds.top) {
				page.removeRegion(belowSplitTopRow);
				belowSplitTopRow = new ImRegion(page, new BoundingBox(belowSplitTopRow.bounds.left, belowSplitTopRow.bounds.right, belowSplitCellTop, belowSplitTopRow.bounds.bottom), ImRegion.TABLE_ROW_TYPE);
				
				ImRegion[] belowSplitTopCells = getRegionsOverlapping(page, belowSplitTopRow.bounds, ImRegion.TABLE_CELL_TYPE);
				for (int c = 0; c < belowSplitTopCells.length; c++) {
					if (belowSplitTopCells[c].bounds.top <= belowSplitCellTop)
						continue; // multi-row cell that reaches above split, no need for expanding
					page.removeRegion(belowSplitTopCells[c]);
					bscBox = new BoundingBox(belowSplitTopCells[c].bounds.left, belowSplitTopCells[c].bounds.right, belowSplitCellTop, belowSplitTopCells[c].bounds.bottom);
					this.markTableCell(page, bscBox, true, (belowSplitRows.size() == 1));
				}
			}
		}
		
		//	clean up table structure
		ImUtils.orderTableWords(this.markTableCells(page, table));
		this.cleanupTableAnnotations(table.getDocument(), table);
		
		//	indicate we changed something
		return true;
	}
	
	private boolean splitTableCellCols(ImRegion table, ImRegion toSplitCell, ImRegion[] toSplitCellRows, ImRegion[] toSplitCellCols, BoundingBox splitBox) {
		Arrays.sort(toSplitCellRows, ImUtils.topDownOrder);
		Arrays.sort(toSplitCellCols, ImUtils.leftRightOrder);
		ImPage page = table.getPage();
		
		//	order words above, inside, and below selection
		ArrayList leftSplitCols = new ArrayList();
		ArrayList splitCols = new ArrayList();
		ArrayList rightSplitCols = new ArrayList();
		for (int c = 0; c < toSplitCellCols.length; c++) {
			int colCenterX = ((toSplitCellCols[c].bounds.left + toSplitCellCols[c].bounds.right) / 2);
			if (colCenterX < splitBox.left)
				leftSplitCols.add(toSplitCellCols[c]);
			else if (colCenterX < splitBox.right)
				splitCols.add(toSplitCellCols[c]);
			else rightSplitCols.add(toSplitCellCols[c]);
		}
		
		//	anything to split at all?
		int emptySplitCols = 0;
		if (leftSplitCols.isEmpty())
			emptySplitCols++;
		if (splitCols.isEmpty())
			emptySplitCols++;
		if (rightSplitCols.isEmpty())
			emptySplitCols++;
		if (emptySplitCols >= 2)
			return false;
		
		//	remove selected cell
		page.removeRegion(toSplitCell);
		
		//	sort words in to-split cell
		ImWord[] toSplitCellWords = page.getWordsInside(toSplitCell.bounds);
		
		//	perform split
		if (leftSplitCols.size() != 0) {
			ImRegion leftSplitRightCol = ((ImRegion) leftSplitCols.get(leftSplitCols.size() - 1));
			int leftSplitCellLeft = ((ImRegion) leftSplitCols.get(0)).bounds.left;
			int leftSplitCellRight = leftSplitRightCol.bounds.right;
			for (int w = 0; w < toSplitCellWords.length; w++) {
				if (toSplitCellWords[w].centerX < splitBox.left)
					leftSplitCellRight = Math.max(leftSplitCellRight, toSplitCellWords[w].bounds.right);
			}
			
			BoundingBox lscBox = new BoundingBox(leftSplitCellLeft, leftSplitCellRight, toSplitCell.bounds.top, toSplitCell.bounds.bottom);
			this.markTableCell(page, lscBox, (leftSplitCols.size() == 1), (toSplitCellRows.length == 1));
			
			if (leftSplitRightCol.bounds.right < leftSplitCellRight) {
				page.removeRegion(leftSplitRightCol);
				leftSplitRightCol = new ImRegion(page, new BoundingBox(leftSplitRightCol.bounds.left, leftSplitCellRight, leftSplitRightCol.bounds.top, leftSplitRightCol.bounds.bottom), ImRegion.TABLE_COL_TYPE);
				
				ImRegion[] leftSplitRightCells = getRegionsOverlapping(page, leftSplitRightCol.bounds, ImRegion.TABLE_CELL_TYPE);
				for (int c = 0; c < leftSplitRightCells.length; c++) {
					if (leftSplitCellRight <= leftSplitRightCells[c].bounds.right)
						continue; // multi-column cell that reaches rightward of split, no need for expanding
					page.removeRegion(leftSplitRightCells[c]);
					lscBox = new BoundingBox(leftSplitRightCells[c].bounds.left, leftSplitCellRight, leftSplitRightCells[c].bounds.top, leftSplitRightCells[c].bounds.bottom);
					this.markTableCell(page, lscBox, (leftSplitCols.size() == 1), (toSplitCellRows.length == 1));
				}
			}
		}
		
		if (splitCols.size() != 0) {
			ImRegion splitLeftCol = ((ImRegion) splitCols.get(0));
			ImRegion splitRightCol = ((ImRegion) splitCols.get(splitCols.size() - 1));
			int splitCellLeft = splitLeftCol.bounds.left;
			int splitCellRight = splitRightCol.bounds.right;
			for (int w = 0; w < toSplitCellWords.length; w++)
				if ((splitBox.left <= toSplitCellWords[w].centerX) && (toSplitCellWords[w].centerX < splitBox.right)) {
					splitCellLeft = Math.min(splitCellLeft, toSplitCellWords[w].bounds.left);
					splitCellRight = Math.max(splitCellRight, toSplitCellWords[w].bounds.right);
				}
			
			BoundingBox iscBox = new BoundingBox(splitCellLeft, splitCellRight, toSplitCell.bounds.top, toSplitCell.bounds.bottom);
			this.markTableCell(page, iscBox, (splitCols.size() == 1), (toSplitCellRows.length == 1));
			
			if (splitLeftCol == splitRightCol) {
				if ((splitCellLeft < splitLeftCol.bounds.left) || (splitLeftCol.bounds.right < splitCellRight)) {
					page.removeRegion(splitLeftCol);
					splitLeftCol = new ImRegion(page, new BoundingBox(Math.min(splitCellLeft, splitLeftCol.bounds.left), Math.max(splitCellRight, splitLeftCol.bounds.right), splitLeftCol.bounds.top, splitLeftCol.bounds.bottom), ImRegion.TABLE_COL_TYPE);
					
					ImRegion[] splitCells = getRegionsOverlapping(page, splitLeftCol.bounds, ImRegion.TABLE_CELL_TYPE);
					for (int c = 0; c < splitCells.length; c++) {
						if ((splitCells[c].bounds.left <= splitCellLeft) && (splitCellRight <= splitCells[c].bounds.right))
							continue; // multi-row cell that reaches leftward and rightward of split, no need for expanding
						page.removeRegion(splitCells[c]);
						iscBox = new BoundingBox(Math.min(splitCellLeft, splitCells[c].bounds.left), Math.max(splitCellRight, splitCells[c].bounds.right), splitCells[c].bounds.top, splitCells[c].bounds.bottom);
						this.markTableCell(page, iscBox, (rightSplitCols.size() == 1), (toSplitCellRows.length == 1));
					}
				}
			}
			else {
				if (splitCellLeft < splitLeftCol.bounds.left) {
					page.removeRegion(splitLeftCol);
					splitLeftCol = new ImRegion(page, new BoundingBox(splitCellLeft, splitLeftCol.bounds.right, splitLeftCol.bounds.top, splitLeftCol.bounds.bottom), ImRegion.TABLE_COL_TYPE);
					
					ImRegion[] splitLeftCells = getRegionsOverlapping(page, splitLeftCol.bounds, ImRegion.TABLE_CELL_TYPE);
					for (int c = 0; c < splitLeftCells.length; c++) {
						if (splitLeftCells[c].bounds.left <= splitCellLeft)
							continue; // multi-column cell that reaches leftward of split, no need for expanding
						page.removeRegion(splitLeftCells[c]);
						iscBox = new BoundingBox(splitCellLeft, splitLeftCells[c].bounds.right, splitLeftCells[c].bounds.top, splitLeftCells[c].bounds.bottom);
						this.markTableCell(page, iscBox, (rightSplitCols.size() == 1), (toSplitCellRows.length == 1));
					}
				}
				if (splitRightCol.bounds.right < splitCellRight) {
					page.removeRegion(splitRightCol);
					splitRightCol = new ImRegion(page, new BoundingBox(splitRightCol.bounds.left, splitCellRight, splitRightCol.bounds.top, splitRightCol.bounds.bottom), ImRegion.TABLE_COL_TYPE);
					
					ImRegion[] splitRightCells = getRegionsOverlapping(page, splitRightCol.bounds, ImRegion.TABLE_CELL_TYPE);
					for (int c = 0; c < splitRightCells.length; c++) {
						if (splitCellRight <= splitRightCells[c].bounds.right)
							continue; // multi-column cell that reaches rightward of split, no need for expanding
						page.removeRegion(splitRightCells[c]);
						iscBox = new BoundingBox(splitRightCells[c].bounds.left, splitCellRight, splitRightCells[c].bounds.top, splitRightCells[c].bounds.bottom);
						this.markTableCell(page, iscBox, (leftSplitCols.size() == 1), (toSplitCellRows.length == 1));
					}
				}
			}
		}
		
		if (rightSplitCols.size() != 0) {
			ImRegion rightSplitLeftCol = ((ImRegion) rightSplitCols.get(0));
			int rightSplitCellLeft = rightSplitLeftCol.bounds.left;
			int rightSplitCellRight = ((ImRegion) rightSplitCols.get(rightSplitCols.size() - 1)).bounds.right;
			for (int w = 0; w < toSplitCellWords.length; w++) {
				if (splitBox.right <= toSplitCellWords[w].centerX)
					rightSplitCellLeft = Math.min(rightSplitCellLeft, toSplitCellWords[w].bounds.left);
			}
			
			BoundingBox rscBox = new BoundingBox(rightSplitCellLeft, rightSplitCellRight, toSplitCell.bounds.top, toSplitCell.bounds.bottom);
			this.markTableCell(page, rscBox, (rightSplitCols.size() == 1), (toSplitCellRows.length == 1));
			
			if (rightSplitCellLeft < rightSplitLeftCol.bounds.left) {
				page.removeRegion(rightSplitLeftCol);
				rightSplitLeftCol = new ImRegion(page, new BoundingBox(rightSplitCellLeft, rightSplitLeftCol.bounds.right, rightSplitLeftCol.bounds.top, rightSplitLeftCol.bounds.bottom), ImRegion.TABLE_COL_TYPE);
				
				ImRegion[] rightSplitLeftCells = getRegionsOverlapping(page, rightSplitLeftCol.bounds, ImRegion.TABLE_CELL_TYPE);
				for (int c = 0; c < rightSplitLeftCells.length; c++) {
					if (rightSplitLeftCells[c].bounds.left <= rightSplitCellLeft)
						continue; // multi-column cell that reaches leftward of split, no need for expanding
					page.removeRegion(rightSplitLeftCells[c]);
					rscBox = new BoundingBox(rightSplitCellLeft, rightSplitLeftCells[c].bounds.right, rightSplitLeftCells[c].bounds.top, rightSplitLeftCells[c].bounds.bottom);
					this.markTableCell(page, rscBox, (rightSplitCols.size() == 1), (toSplitCellRows.length == 1));
				}
			}
		}
		
		//	clean up table structure
		ImUtils.orderTableWords(this.markTableCells(page, table));
		this.cleanupTableAnnotations(table.getDocument(), table);
		
		//	indicate we changed something
		return true;
	}
	
	/**
	 * Mark a table in a page, using the argument bounding box as the table
	 * area. The bounding box will be shrunk or expanded only so far as to fit
	 * tightly around its contained words. Any block, paragraph, or line inside
	 * the argument bounding box will be removed; blocks partially inside the
	 * argument bounding box will be split.
	 * @param page the page to mark the table in
	 * @param tableBox the bounding box to use for the table
	 * @param tableBoxStats statistics on the distribution of words in the
	 *            argument table bounding box
	 * @param idmp the markup panel the page is displaying in (if any)
	 * @return true if a table was marked, false otherwise
	 */
	boolean markTable(ImPage page, BoundingBox tableBox, TableAreaStatistics tableBoxStats, ImDocumentMarkupPanel idmp) {
		ImWord[] words = page.getWordsInside(tableBox);
		if (words.length == 0)
			return false;
		return this.markTable(page, words, tableBox, tableBoxStats, false, idmp);
	}
	
	private boolean markTable(ImPage page, ImWord[] tableWords, BoundingBox tableBox, TableAreaStatistics tableBoxStats, boolean inLineTable, ImDocumentMarkupPanel idmp) {
		
		//	compute stats if not given
		if (tableBoxStats == null)
			tableBoxStats = TableAreaStatistics.getTableAreaStatistics(page, tableWords);
		
		//	get table markup
		TableAreaMarkup tableMarkup = this.getTableMarkup(page, tableWords, null, tableBox, tableBoxStats, inLineTable);
		if (tableMarkup == null) {
			DialogFactory.alert("The selected area cannot be marked as a table because it does not split into columns.", "Cannot Mark Tables", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		//	finish markup
		return this.finishMarkTable(page, tableBox, tableWords, tableMarkup, null, idmp);
	}
	
	private boolean clearTableArea(ImPage page, BoundingBox tableBox, ImWord[] words) {
		
		//	get and split blocks overlapping with table
		ImRegion[] blocks = page.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
		int blockSplitCount = 0;
		for (int b = 0; b < blocks.length; b++) {
			if (!tableBox.overlaps(blocks[b].bounds))
				continue; // this one is not affected
			if (tableBox.includes(blocks[b].bounds, false))
				continue; // this one doesn't need to be split
			
			BoundingBox overlapBox = new BoundingBox(
					Math.max(blocks[b].bounds.left, tableBox.left),
					Math.min(blocks[b].bounds.right, tableBox.right),
					Math.max(blocks[b].bounds.top, tableBox.top),
					Math.min(blocks[b].bounds.bottom, tableBox.bottom)
				);
			ImWord[] overlapWords = page.getWordsInside(overlapBox);
			if (overlapWords.length == 0)
				continue; // no words in overlap, selection just brushes this one, so no need for splitting
			
			ImWord[] blockWords = page.getWordsInside(blocks[b].bounds);
			HashSet remainingBlockWords = new HashSet();
			for (int bw = 0; bw < blockWords.length; bw++)
				remainingBlockWords.add(blockWords[bw].bounds);
			for (int w = 0; w < words.length; w++)
				remainingBlockWords.remove(words[w].bounds);
			if (remainingBlockWords.isEmpty())
				continue; // all words of block covered, we'll take care of this one later
			
			if (this.regionActionProvider.splitBlock(page, blocks[b], overlapBox))
				blockSplitCount++;
		}
		
		//	split anything?
		return (blockSplitCount != 0);
	}
	
	private boolean addTableMarkup(TableAreaMarkup tableMarkup) {
		
		//	get existing columns, rows, and cells
		ImRegion[] exCols = getRegionsOverlapping(tableMarkup.page, tableMarkup.table.bounds, ImRegion.TABLE_COL_TYPE);
		ImRegion[] exRows = getRegionsOverlapping(tableMarkup.page, tableMarkup.table.bounds, ImRegion.TABLE_ROW_TYPE);
		ImRegion[] exCells = getRegionsOverlapping(tableMarkup.page, tableMarkup.table.bounds, ImRegion.TABLE_CELL_TYPE);
		
		//	index existing markup
		HashSet exColSet = new HashSet();
		for (int ec = 0; ec < exCols.length; ec++)
			exColSet.add(exCols[ec]);
		HashSet exRowSet = new HashSet();
		for (int er = 0; er < exRows.length; er++)
			exRowSet.add(exRows[er]);
		HashSet exCellSet = new HashSet();
		for (int ec = 0; ec < exCells.length; ec++)
			exCellSet.add(exCells[ec]);
		
		//	mark columns and rows
		for (int c = 0; c < tableMarkup.cols.length; c++) {
			if (tableMarkup.cols[c].getPage() == null)
				tableMarkup.page.addRegion(tableMarkup.cols[c]);
			else exColSet.remove(tableMarkup.cols[c]);
		}
		for (int r = 0; r < tableMarkup.rows.length; r++) {
			if (tableMarkup.rows[r].getPage() == null)
				tableMarkup.page.addRegion(tableMarkup.rows[r]);
			else exRowSet.remove(tableMarkup.rows[r]);
		}
		
		//	mark cells (making sure to not add multi-cells twice)
		for (int c = 0; c < tableMarkup.cols.length; c++)
			for (int r = 0; r < tableMarkup.rows.length; r++) {
				if (tableMarkup.cells[c][r].getPage() == null)
					tableMarkup.page.addRegion(tableMarkup.cells[c][r]);
				else exCellSet.remove(tableMarkup.cells[c][r]);
			}
		
		//	clean up any non-retained existing regions
		for (Iterator ecit = exColSet.iterator(); ecit.hasNext();)
			tableMarkup.page.removeRegion((ImRegion) ecit.next());
		for (Iterator erit = exRowSet.iterator(); erit.hasNext();)
			tableMarkup.page.removeRegion((ImRegion) erit.next());
		for (Iterator ecit = exCellSet.iterator(); ecit.hasNext();)
			tableMarkup.page.removeRegion((ImRegion) ecit.next());
		
		//	add table proper to page
		if (tableMarkup.table.getPage() == null)
			tableMarkup.page.addRegion(tableMarkup.table);
		
		//	cut table out of main text
		ImUtils.makeStream(tableMarkup.words, ImWord.TEXT_STREAM_TYPE_TABLE, null);
		
		//	flatten out table content
		for (int w = 0; w < tableMarkup.words.length; w++) {
			if (tableMarkup.words[w].getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
				tableMarkup.words[w].setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
		}
		
		//	remove all regions not related to table
		ImRegion[] tableRegions = tableMarkup.page.getRegionsInside(tableMarkup.table.bounds, false);
		for (int r = 0; r < tableRegions.length; r++) {
			if (ImRegion.LINE_ANNOTATION_TYPE.equals(tableRegions[r].getType()))
				tableMarkup.page.removeRegion(tableRegions[r]);
			else if (ImRegion.PARAGRAPH_TYPE.equals(tableRegions[r].getType()))
				tableMarkup.page.removeRegion(tableRegions[r]);
			else if (ImRegion.BLOCK_ANNOTATION_TYPE.equals(tableRegions[r].getType()))
				tableMarkup.page.removeRegion(tableRegions[r]);
			else if (ImRegion.COLUMN_ANNOTATION_TYPE.equals(tableRegions[r].getType()))
				tableMarkup.page.removeRegion(tableRegions[r]);
			else if (ImRegion.REGION_ANNOTATION_TYPE.equals(tableRegions[r].getType()))
				tableMarkup.page.removeRegion(tableRegions[r]);
		}
		
		//	order words from each cell as a text stream
		ImUtils.orderTableWords(this.markTableCells(tableMarkup.page, tableMarkup.table));
		this.cleanupTableAnnotations(tableMarkup.page.getDocument(), tableMarkup.table);
		
		//	finally ...
		return true;
	}
	
	private TableAreaMarkup diffTableMarkup(TableAreaMarkup tableMarkup, ImRegion exTable) {
		
		//	get existing columns and rows
		ImRegion[] exCols = exTable.getRegions(ImRegion.TABLE_COL_TYPE);
		Arrays.sort(exCols, ImUtils.leftRightOrder);
		ImRegion[] exRows = exTable.getRegions(ImRegion.TABLE_ROW_TYPE);
		Arrays.sort(exRows, ImUtils.topDownOrder);
		
		//	anything to adjust to?
		if ((exCols.length + exRows.length) == 0)
			return tableMarkup;
		
		//	get existing cells
		ImRegion[][] exCells = ImUtils.getTableCells(exTable, null, exRows, exCols, false);
		
		//	adjust new markup to preserve cell mergers (columns and rows handled beforehand)
		for (int ec = 0; ec < exCells.length; ec++) {
			for (int er = 0; er < exCells[ec].length; er++) {
				
				//	check for merged cells staring at current point
				boolean exMergedRight = (((ec + 1) < exCells.length) && (exCells[ec][er] == exCells[ec + 1][er]));
				boolean exMergedDown = (((er + 1) < exCells[ec].length) && (exCells[ec][er] == exCells[ec][er + 1]));
				
				//	no mergers to preserve
				if (!exMergedRight && !exMergedDown)
					continue;
				
				//	check for incoming mergers
				boolean exMergedLeft = ((ec != 0) && (exCells[ec][er] == exCells[ec - 1][er]));
				boolean exMergedUp = ((er != 0) && (exCells[ec][er] == exCells[ec][er - 1]));
				
				//	handled before
				if (exMergedLeft || exMergedUp)
					continue;
				
				//	find corresponding cells in new markup, as well as dimensions
				HashSet mergeCells = new HashSet();
				int minMergeCellCol = tableMarkup.cells.length;
				int maxMergeCellCol = 0;
				int minMergeCellRow = tableMarkup.cells[0].length;
				int maxMergeCellRow = 0;
				int mergeCellLeft = tableMarkup.table.bounds.right;
				int mergeCellRight = tableMarkup.table.bounds.left;
				int mergeCellTop = tableMarkup.table.bounds.bottom;
				int mergeCellBottom = tableMarkup.table.bounds.top;
				for (int c = 0; c < tableMarkup.cells.length; c++) {
					if (tableMarkup.cells[c][0].bounds.right < exCells[ec][er].bounds.left)
						continue; // left of existing merged cell
					if (exCells[ec][er].bounds.right < tableMarkup.cells[c][0].bounds.left)
						break; // right of existing merged cell, we're done
					for (int r = 0; r < tableMarkup.cells[c].length; r++) {
						if (tableMarkup.cells[c][r].bounds.bottom < exCells[ec][er].bounds.top)
							continue; // above existing merged cell
						if (exCells[ec][er].bounds.bottom < tableMarkup.cells[c][r].bounds.top)
							break; // below existing merged cell, we're done
						
						//	got to merge this one, and store coordinates and extent
						if (tableMarkup.cells[c][r].bounds.overlaps(exCells[ec][er].bounds)) {
							mergeCells.add(tableMarkup.cells[c][r]);
							minMergeCellCol = Math.min(minMergeCellCol, c);
							maxMergeCellCol = Math.max(maxMergeCellCol, c);
							minMergeCellRow = Math.min(minMergeCellRow, r);
							maxMergeCellRow = Math.max(maxMergeCellRow, r);
							mergeCellLeft = Math.min(mergeCellLeft, tableMarkup.cells[c][r].bounds.left);
							mergeCellRight = Math.max(mergeCellRight, tableMarkup.cells[c][r].bounds.right);
							mergeCellTop = Math.min(mergeCellTop, tableMarkup.cells[c][r].bounds.top);
							mergeCellBottom = Math.max(mergeCellBottom, tableMarkup.cells[c][r].bounds.bottom);
						}
					}
				}
				
				//	anything to merge?
				if (mergeCells.size() < 2)
					continue;
				
				//	merge cells and replace them in markup
				ImRegion mCell = new ImRegion(tableMarkup.page.getDocument(), tableMarkup.page.pageId, new BoundingBox(mergeCellLeft, mergeCellRight, mergeCellTop, mergeCellBottom), ImRegion.TABLE_CELL_TYPE);
				for (int c = minMergeCellCol; c <= maxMergeCellCol; c++) {
					for (int r = minMergeCellRow; r <= maxMergeCellRow; c++)
						tableMarkup.cells[c][r] = mCell;
				}
			}
		}
		
		//	index existing markup
		HashMap exColsByBounds = new HashMap();
		for (int ec = 0; ec < exCols.length; ec++)
			exColsByBounds.put(exCols[ec].bounds, exCols[ec]);
		HashMap exRowsByBounds = new HashMap();
		for (int er = 0; er < exRows.length; er++)
			exRowsByBounds.put(exRows[er].bounds, exRows[er]);
		HashMap exCellsByBounds = new HashMap();
		for (int ec = 0; ec < exCells.length; ec++) {
			for (int er = 0; er < exCells[ec].length; er++)
				exCellsByBounds.put(exCells[ec][er].bounds, exCells[ec][er]);
		}
		
		//	diff new markup with existing, and replace detached regions in arrays with existing attached ones where possible
		for (int c = 0; c < tableMarkup.cols.length; c++) {
			if (exColsByBounds.containsKey(tableMarkup.cols[c].bounds))
				tableMarkup.cols[c] = ((ImRegion) exColsByBounds.get(tableMarkup.cols[c].bounds));
		}
		for (int r = 0; r < tableMarkup.rows.length; r++) {
			if (exRowsByBounds.containsKey(tableMarkup.rows[r].bounds))
				tableMarkup.rows[r] = ((ImRegion) exRowsByBounds.get(tableMarkup.rows[r].bounds));
		}
		for (int c = 0; c < tableMarkup.cells.length; c++)
			for (int r = 0; r < tableMarkup.cells[c].length; r++) {
				if (exRowsByBounds.containsKey(tableMarkup.cells[c][r].bounds))
					tableMarkup.cells[c][r] = ((ImRegion) exRowsByBounds.get(tableMarkup.cells[c][r].bounds));
			}
		
		//	finally ...
		return tableMarkup;
	}
	
	private TableAreaMarkup getTableMarkup(ImPage page, ImWord[] words, ImRegion[] exTables, BoundingBox tableBox, TableAreaStatistics tableBoxStats, boolean inLineTable) {
		
		//	get overall bounds of any existing tables
		BoundingBox exTablesBox = (((exTables == null) || (exTables.length == 0)) ? null : ImLayoutObject.getAggregateBox(exTables));
		
		//	compute stats if not given
		if (tableBoxStats == null)
			tableBoxStats = TableAreaStatistics.getTableAreaStatistics(page, words);
		
		//	get column gaps
		TreeSet colOccupationLows = new TreeSet(tableBoxStats.getBrgColOccupationLowsReduced());
		System.out.println("Column occupation lows are " + colOccupationLows);
		if (colOccupationLows.isEmpty()) {
			System.out.println(" ==> cannot mark table");
			return null;
		}
		int colMarginSum = 0;
		int minColMargin = Integer.MAX_VALUE;
		int maxColMargin = 0;
		CountingSet colOccupationLowWidths = new CountingSet(new TreeMap());
		int blockedColMarginCount = 0;
		int peakBlockedColMarginCount = 0;
		int flankBlockedColMarginCount = 0;
		for (Iterator colit = colOccupationLows.iterator(); colit.hasNext();) {
			ColumnOccupationLow col = ((ColumnOccupationLow) colit.next());
			colMarginSum += col.getWidth();
			minColMargin = Math.min(minColMargin, col.getWidth());
			maxColMargin = Math.max(maxColMargin, col.getWidth());
			colOccupationLowWidths.add(new Integer(col.getWidth()));
			if (col.isBlockedByPeak()) {
				peakBlockedColMarginCount++;
				System.out.println(" - peak in " + col);
			}
			if (col.isBlockedByFlank()) {
				flankBlockedColMarginCount++;
				System.out.println(" - flank in " + col);
			}
			if (col.isBlockedByGraphics()) {
				blockedColMarginCount++;
				col.enforced = true;
				System.out.println(" - graphics separated column gap at " + col);
			}
		}
		System.out.println(" - average word height is " + tableBoxStats.avgWordHeight);
		System.out.println(" - got " + blockedColMarginCount + " graphics blocked column occupation lows (" + peakBlockedColMarginCount + " by peaks, " + flankBlockedColMarginCount + " by flanks)");
		int avgColMargin = ((colMarginSum + (colOccupationLows.size() / 2)) / colOccupationLows.size());
		System.out.println(" - average column occupation low width is " + avgColMargin + " (" + minColMargin + "/" + maxColMargin + "):");
		for (Iterator cmit = colOccupationLowWidths.iterator(); cmit.hasNext();) {
			Integer colMargin = ((Integer) cmit.next());
			System.out.println("   - " + colMargin + ": " + colOccupationLowWidths.getCount(colMargin));
		}
		System.out.println(" ==> minimum column margin is " + minColMargin);
		
		//	if many column gaps have peaks or flanks, discard the ones that don't (we likely have a full vertical grid)
		if (
				//	require peaks in half of column gaps for this reduction
				((2 < peakBlockedColMarginCount) && (colOccupationLows.size() < (peakBlockedColMarginCount * 2)) && (peakBlockedColMarginCount < colOccupationLows.size()))
				||
				//	require flanks only in one third of column gaps for this reduction, as vertical stripe grids are barely ever partial
				((2 < flankBlockedColMarginCount) && (colOccupationLows.size() < (flankBlockedColMarginCount * 3)) && (flankBlockedColMarginCount < colOccupationLows.size()))
			) {
			
			//	sort out non-blocked column gaps
			for (Iterator colit = colOccupationLows.iterator(); colit.hasNext();) {
				ColumnOccupationLow col = ((ColumnOccupationLow) colit.next());
				if (!col.isBlockedByGraphics())
					colit.remove();
			}
			
			//	re-compute averages
			System.out.println("Graphics reduced column occupation lows are " + colOccupationLows);
			colMarginSum = 0;
			minColMargin = Integer.MAX_VALUE;
			maxColMargin = 0;
			colOccupationLowWidths.clear();
			for (Iterator colit = colOccupationLows.iterator(); colit.hasNext();) {
				ColumnOccupationLow col = ((ColumnOccupationLow) colit.next());
				colMarginSum += col.getWidth();
				minColMargin = Math.min(minColMargin, col.getWidth());
				maxColMargin = Math.max(maxColMargin, col.getWidth());
				colOccupationLowWidths.add(new Integer(col.getWidth()));
			}
			avgColMargin = ((colMarginSum + (colOccupationLows.size() / 2)) / colOccupationLows.size());
			System.out.println(" - average column occupation low width now is " + avgColMargin + " (" + minColMargin + "/" + maxColMargin + "):");
			for (Iterator cmit = colOccupationLowWidths.iterator(); cmit.hasNext();) {
				Integer colMargin = ((Integer) cmit.next());
				System.out.println("   - " + colMargin + ": " + colOccupationLowWidths.getCount(colMargin));
			}
			System.out.println(" ==> minimum column margin now is " + minColMargin);
		}
		
		//	assess fully open vs. partially obstructed column gaps
		TreeSet openColOccupationLows = new TreeSet();
		int minOpenColMargin = Integer.MAX_VALUE;
		int maxOpenColMargin = 0;
		TreeSet bridgedColOccupationLows = new TreeSet();
		int minBridgedColMargin = Integer.MAX_VALUE;
		int maxBridgedColMargin = 0;
		for (Iterator colit = colOccupationLows.iterator(); colit.hasNext();) {
			ColumnOccupationLow col = ((ColumnOccupationLow) colit.next());
			if ((col.max == 0) || col.isBlockedByGraphics()) {
				openColOccupationLows.add(col);
				minOpenColMargin = Math.min(minOpenColMargin, col.getWidth());
				maxOpenColMargin = Math.max(maxOpenColMargin, col.getWidth());
			}
			else {
				bridgedColOccupationLows.add(col);
				minBridgedColMargin = Math.min(minBridgedColMargin, col.getWidth());
				maxBridgedColMargin = Math.max(maxBridgedColMargin, col.getWidth());
			}
		}
		System.out.println("Got " + openColOccupationLows.size() + " open column gaps (" + minOpenColMargin + "/" + maxOpenColMargin + ") and " + bridgedColOccupationLows.size() + " bridged ones (" + minBridgedColMargin + "/" + maxBridgedColMargin + ")");
		
		//	filter all too narrow partially obstructed column gaps
		if (minBridgedColMargin < minOpenColMargin) {
			
			//	do filtering
			System.out.println(" - checking for all too narrow bridged gaps");
			for (Iterator colit = colOccupationLows.iterator(); colit.hasNext();) {
				ColumnOccupationLow col = ((ColumnOccupationLow) colit.next());
				System.out.print("   - " + col);
				if ((col.max == 0) || col.isBlockedByGraphics()) {
					System.out.println(" ==> retained as open or blocked");
					continue; // open gap, or graphically marked one
				}
				if (minOpenColMargin <= col.getWidth()) {
					System.out.println(" ==> retained as wider than some open gap");
					continue; // wider than some open gap(s)
				}
				if (tableBoxStats.avgWordHeight < (col.getWidth() * 2)) {
					System.out.println(" ==> retained as wider than wide space");
					continue; // wider than a 50% word height space
				}
				System.out.println(" ==> removed");
				colit.remove();
			}
			
			//	re-compute averages
			System.out.println("Obstruction reduced column occupation lows are " + colOccupationLows);
			colMarginSum = 0;
			minColMargin = Integer.MAX_VALUE;
			maxColMargin = 0;
			colOccupationLowWidths.clear();
			for (Iterator colit = colOccupationLows.iterator(); colit.hasNext();) {
				ColumnOccupationLow col = ((ColumnOccupationLow) colit.next());
				colMarginSum += col.getWidth();
				minColMargin = Math.min(minColMargin, col.getWidth());
				maxColMargin = Math.max(maxColMargin, col.getWidth());
				colOccupationLowWidths.add(new Integer(col.getWidth()));
			}
			avgColMargin = ((colMarginSum + (colOccupationLows.size() / 2)) / colOccupationLows.size());
			System.out.println(" - average column occupation low width now is " + avgColMargin + " (" + minColMargin + "/" + maxColMargin + "):");
			for (Iterator cmit = colOccupationLowWidths.iterator(); cmit.hasNext();) {
				Integer colMargin = ((Integer) cmit.next());
				System.out.println("   - " + colMargin + ": " + colOccupationLowWidths.getCount(colMargin));
			}
			System.out.println(" ==> minimum column margin now is " + minColMargin);
		}
		
		//	diff with any existing column gaps
		if (exTablesBox != null) {
			
			//	prepare marking visited pixel columns
			byte[] exColDiff = new byte[tableBox.getWidth()];
			Arrays.fill(exColDiff, ((byte) -1));
			
			//	mark any existing column structures
			for (int t = 0; t < exTables.length; t++) {
				
				//	mark pixel columns as covered
				for (int x = exTables[t].bounds.left; x < exTables[t].bounds.right; x++) {
					if (exColDiff[x - tableBox.left] == -1)
						exColDiff[x - tableBox.left] = 0;
				}
				
				//	mark any existing column gaps
				ImRegion[] exCols = getRegionsInside(page, exTables[t].bounds, ImRegion.TABLE_COL_TYPE, false);
				Arrays.sort(exCols, ImUtils.leftRightOrder);
				for (int c = 1; c < exCols.length; c++) {
					for (int x = exCols[c-1].bounds.right; x < exCols[c].bounds.left; x++)
						exColDiff[x - tableBox.left]++;
				}
			}
			
			//	diff column gaps
			for (Iterator colit = colOccupationLows.iterator(); colit.hasNext();) {
				ColumnOccupationLow col = ((ColumnOccupationLow) colit.next());
				
				//	check for any support in existing tables
				int minExColDiff = exTables.length;
				int maxExColDiff = -1;
				for (int x = col.left; x < col.right; x++) {
					minExColDiff = Math.min(minExColDiff, exColDiff[x - tableBox.left]);
					maxExColDiff = Math.max(maxExColDiff, exColDiff[x - tableBox.left]);
				}
				
				//	this one lies outside existing tables, at least partially
				if ((maxExColDiff == -1) || (minExColDiff == -1))
					continue;
				
				//	this one has no counterpart in existing tables, remove it
				if (maxExColDiff == 0) {
					colit.remove();
					continue;
				}
				
				//	flatten out current gap, retaining observed gap
				for (int x = col.left; x < col.right; x++) {
					if (0 < exColDiff[x - exTablesBox.left])
						exColDiff[x - tableBox.left] = 0;
				}
				for (int lx = col.right; lx < exTablesBox.right; lx++) {
					if (exColDiff[lx - exTablesBox.left] <= 0)
						break; // reached end of existing gap
					else exColDiff[lx - tableBox.left] = 0;
				}
				for (int lx = (col.left - 1); lx >= exTablesBox.left; lx--) {
					if (exColDiff[lx - exTablesBox.left] <= 0)
						break; // reached end of existing gap
					else exColDiff[lx - tableBox.left] = 0;
				}
				
				//	set enforce flag of gap to prevent mergers across it
				col.enforced = true;
			}
			
			//	add any existing column gaps not matched to some counterpart
			for (int x = tableBox.left; x < tableBox.right; x++) {
				if (exColDiff[x - tableBox.left] <= 0)
					continue; // no way of starting a gap here
				for (int lx = (x + 1); lx < tableBox.right; lx++)
					if (exColDiff[lx - tableBox.left] <= 0) {
						colOccupationLows.add(new ColumnOccupationLow(tableBoxStats, x, lx, -1, -1, true));
						x = lx; // no need to investigate this one again, so no need to compensate for loop increment
						break; // we're done with this one
					}
			}
			System.out.println("Adjusted column occupation lows are " + colOccupationLows);
		}
		
		
		//	get row gaps
		//	TODO TEST table on page 5 of EJT/ejt-496_read_enghoff.pdf (all row gaps blocked by graphics, but row merges none the less)
		TreeSet rowOccupationGaps = new TreeSet(tableBoxStats.getRowOccupationGaps());
		System.out.println("Row occupation gaps are " + rowOccupationGaps);
		int rowMarginSum = 0;
		int minRowMargin = Integer.MAX_VALUE;
		int maxRowMargin = 0;
		int blockedRowMarginCount = 0;
		int peakBlockedRowMarginCount = 0;
		int flankBlockedRowMarginCount = 0;
		CountingSet rowOccupationGapWidths = new CountingSet(new TreeMap());
		for (Iterator rogit = rowOccupationGaps.iterator(); rogit.hasNext();) {
			RowOccupationGap rog = ((RowOccupationGap) rogit.next());
			rowMarginSum += rog.getHeight();
			minRowMargin = Math.min(minRowMargin, rog.getHeight());
			maxRowMargin = Math.max(maxRowMargin, rog.getHeight());
			rowOccupationGapWidths.add(new Integer(rog.getHeight()));
			System.out.println(" - row gap at " + rog);
			GraphicsSlice gapGraphics = tableBoxStats.getRowGraphicsStats(rog.top, rog.bottom);
			if (gapGraphics == null)
				System.out.println("   no graphics at all");
			else System.out.println("   graphics are " + Arrays.toString(gapGraphics.pixelOccupation));
			GraphicsSlice extGapGraphics = tableBoxStats.getRowGraphicsStats(rog.top, rog.bottom);
			if (extGapGraphics != null)
				System.out.println("   extended " + Arrays.toString(tableBoxStats.getRowGraphicsStats((rog.top-3), (rog.bottom+3)).pixelOccupation));
			if (rog.isBlockedByPeak()) {
				peakBlockedRowMarginCount++;
				System.out.println("   got peak");
			}
			if (rog.isBlockedByFlank()) {
				flankBlockedRowMarginCount++;
				System.out.println("   got flank");
			}
			if (rog.isBlockedByGraphics()) {
				blockedRowMarginCount++;
				rog.enforced = true;
				System.out.println("   graphics separated row gap at " + rog);
			}
		}
		System.out.println(" - got " + blockedRowMarginCount + " graphics blocked row occupation gaps (" + peakBlockedRowMarginCount + " by peaks, " + flankBlockedRowMarginCount + " by flanks)");
		int avgRowMargin = ((rowMarginSum + (rowOccupationGaps.size() / 2)) / rowOccupationGaps.size());
		System.out.println(" - average row occupation gap height is " + avgRowMargin + " (" + minRowMargin + "/" + maxRowMargin + "):");
		for (Iterator rmit = rowOccupationGapWidths.iterator(); rmit.hasNext();) {
			Integer rowMargin = ((Integer) rmit.next());
			System.out.println("   - " + rowMargin + ": " + rowOccupationGapWidths.getCount(rowMargin));
		}
		System.out.println(" ==> minimum row margin is " + minRowMargin);
		
		//	if we have both peaks and flanks, check for mingled peaks (words grown into graphics)
		boolean gotMingledPeaks = false;
		int mingleCheckRadius = 0;
		if ((peakBlockedRowMarginCount > 3) && (flankBlockedRowMarginCount != 0)) {
			mingleCheckRadius = ((page.getImageDPI() + (50 / 2)) / 50); // some half millimeter
			System.out.println("Checking for mingled-peak-blocked row occupation gaps with radius " + mingleCheckRadius);
			for (Iterator rogit = rowOccupationGaps.iterator(); rogit.hasNext();) {
				RowOccupationGap rog = ((RowOccupationGap) rogit.next());
				if (rog.isBlockedByPeak())
					continue;
				if (!rog.isBlockedByFlank())
					continue;
				GraphicsSlice gs = tableBoxStats.getRowGraphicsStats((rog.top - mingleCheckRadius), (rog.bottom + mingleCheckRadius));
				if ((gs != null) && gs.hasPeak()) {
					gotMingledPeaks = true;
					peakBlockedRowMarginCount++;
					flankBlockedRowMarginCount--;
					System.out.println(" - got mingled peak in row gap at " + rog);
				}
			}
		}
		
		//	if many row gaps have peaks or flanks, discard the ones that don't (we likely have a full horizontal grid)
		if (
				//	require peaks in half of row gaps for this reduction (must not mess up partial horizontal line grids)
				((2 < peakBlockedRowMarginCount) && (rowOccupationGaps.size() < (peakBlockedRowMarginCount * 2)) && (peakBlockedRowMarginCount < rowOccupationGaps.size()))
				||
				//	require flanks only in one third of row gaps for this reduction, as horizontal stripe grids are barely ever partial
				((2 < flankBlockedRowMarginCount) && (rowOccupationGaps.size() < (flankBlockedRowMarginCount * 3)) && (flankBlockedRowMarginCount < rowOccupationGaps.size()))
			) {
			
			//	sort out non-blocked column gaps
			for (Iterator rogit = rowOccupationGaps.iterator(); rogit.hasNext();) {
				RowOccupationGap rog = ((RowOccupationGap) rogit.next());
				if (rog.isBlockedByGraphics())
					continue;
				if (gotMingledPeaks) {
					GraphicsSlice gs = tableBoxStats.getRowGraphicsStats((rog.top - mingleCheckRadius), (rog.bottom + mingleCheckRadius));
					if ((gs != null) && gs.hasPeak())
						continue;
				}
				rogit.remove();
			}
			
			//	re-compute averages
			System.out.println("Graphics reduced row occupation gaps are " + rowOccupationGaps);
			rowMarginSum = 0;
			minRowMargin = Integer.MAX_VALUE;
			maxRowMargin = 0;
			rowOccupationGapWidths.clear();
			for (Iterator rogit = rowOccupationGaps.iterator(); rogit.hasNext();) {
				RowOccupationGap rog = ((RowOccupationGap) rogit.next());
				rowMarginSum += rog.getHeight();
				minRowMargin = Math.min(minRowMargin, rog.getHeight());
				maxRowMargin = Math.max(maxRowMargin, rog.getHeight());
				rowOccupationGapWidths.add(new Integer(rog.getHeight()));
			}
			avgRowMargin = ((rowMarginSum + (rowOccupationGaps.size() / 2)) / rowOccupationGaps.size());
			System.out.println(" - average row occupation gap height now is " + avgRowMargin + " (" + minRowMargin + "/" + maxRowMargin + "):");
			for (Iterator rmit = rowOccupationGapWidths.iterator(); rmit.hasNext();) {
				Integer rowMargin = ((Integer) rmit.next());
				System.out.println("   - " + rowMargin + ": " + rowOccupationGapWidths.getCount(rowMargin));
			}
			System.out.println(" ==> minimum row margin now is " + minRowMargin);
			
			//	in a full horizontal line grid, scan for lines not included in a gap to find partially obstructed row boundaries
			if (peakBlockedRowMarginCount > flankBlockedRowMarginCount) {
				System.out.println("Scanning for partially obstructed graphics backed row occupation gaps");
				
				//	scan space between row occupation gaps for further peaks
				ArrayList rowOccupationGapList = new ArrayList(rowOccupationGaps);
				for (int g = 0; g <= rowOccupationGapList.size(); g++) {
					RowOccupationGap topRog = ((g == 0) ? null : ((RowOccupationGap) rowOccupationGapList.get(g-1)));
					RowOccupationGap bottomRog = ((g == rowOccupationGapList.size()) ? null : ((RowOccupationGap) rowOccupationGapList.get(g)));
					int scanTop = ((topRog == null) ? tableBox.top : topRog.bottom);
					int scanBottom = ((bottomRog == null) ? tableBox.bottom : bottomRog.top);
					
					//	get graphics stats for scan area
					GraphicsSlice gs = tableBoxStats.getRowGraphicsStats(scanTop, scanBottom);
					if ((gs == null) || ((gs.maxOccupiedPixels * 3) < (tableBox.getWidth() * 2)))
						continue; // no peaks in this one
					
					//	scan for peaks
					int peakTop = -1;
					for (int y = 0; y < gs.pixelOccupation.length; y++) {
						
						//	pixel row has 67% peak
						if ((gs.pixelOccupation[y] * 3) > (tableBox.getWidth() * 2)) {
							if (peakTop == -1)
								peakTop = y;
						}
						
						//	peak ends here, store it and reset (adding a quite narrow artificial gap is unproblematic, as we're in a full grid anyway, and thus don't need to care about row merging thresholds)
						else if (peakTop != -1) {
							RowOccupationGap rog = new RowOccupationGap(tableBoxStats, (scanTop + peakTop - 1), (scanTop + y + 1), true);
							rowOccupationGaps.add(rog);
							peakTop = -1;
							System.out.println(" --> found additional graphics separated row gap at " + rog);
						}
					}
				}
				
				//	if we have added gaps, we need to re-compute averages again
				if (rowOccupationGapList.size() < rowOccupationGaps.size()) {
					System.out.println("Graphics augmented row occupation gaps are " + rowOccupationGaps);
					rowMarginSum = 0;
					minRowMargin = Integer.MAX_VALUE;
					maxRowMargin = 0;
					rowOccupationGapWidths.clear();
					for (Iterator rogit = rowOccupationGaps.iterator(); rogit.hasNext();) {
						RowOccupationGap rog = ((RowOccupationGap) rogit.next());
						rowMarginSum += rog.getHeight();
						minRowMargin = Math.min(minRowMargin, rog.getHeight());
						maxRowMargin = Math.max(maxRowMargin, rog.getHeight());
						rowOccupationGapWidths.add(new Integer(rog.getHeight()));
					}
					avgRowMargin = ((rowMarginSum + (rowOccupationGaps.size() / 2)) / rowOccupationGaps.size());
					System.out.println(" - average row occupation gap height now is " + avgRowMargin + " (" + minRowMargin + "/" + maxRowMargin + "):");
					for (Iterator rmit = rowOccupationGapWidths.iterator(); rmit.hasNext();) {
						Integer rowMargin = ((Integer) rmit.next());
						System.out.println("   - " + rowMargin + ": " + rowOccupationGapWidths.getCount(rowMargin));
					}
					System.out.println(" ==> minimum row margin now is " + minRowMargin);
				}
			}
		}
		
		//	diff with any existing row gaps
		if (exTablesBox != null) {
			
			//	prepare marking visited pixel rows
			byte[] exRowDiff = new byte[tableBox.getHeight()];
			Arrays.fill(exRowDiff, ((byte) -1));
			
			//	mark any existing row structures
			for (int t = 0; t < exTables.length; t++) {
				
				//	mark pixel rows as covered
				for (int y = exTables[t].bounds.top; y < exTables[t].bounds.bottom; y++) {
					if (exRowDiff[y - tableBox.top] == -1)
						exRowDiff[y - tableBox.top] = 0;
				}
				
				//	mark any existing row gaps
				ImRegion[] exRows = getRegionsInside(page, exTables[t].bounds, ImRegion.TABLE_ROW_TYPE, false);
				Arrays.sort(exRows, ImUtils.topDownOrder);
				for (int r = 1; r < exRows.length; r++) {
					for (int y = exRows[r-1].bounds.bottom; y < exRows[r].bounds.top; y++)
						exRowDiff[y - tableBox.top]++;
				}
			}
			
			//	diff row gaps
			for (Iterator rogit = rowOccupationGaps.iterator(); rogit.hasNext();) {
				RowOccupationGap rog = ((RowOccupationGap) rogit.next());
				
				//	check for any support in existing tables
				int minExRowDiff = exTables.length;
				int maxExRowDiff = -1;
				for (int y = rog.top; y < rog.bottom; y++) {
					minExRowDiff = Math.min(minExRowDiff, exRowDiff[y - tableBox.top]);
					maxExRowDiff = Math.max(maxExRowDiff, exRowDiff[y - tableBox.top]);
				}
				
				//	this one lies outside existing tables, at least partially
				if ((maxExRowDiff == -1) || (minExRowDiff == -1))
					continue;
				
				//	this one has no counterpart in existing tables, remove it
				if (maxExRowDiff == 0) {
					rogit.remove();
					continue;
				}
				
				//	flatten out current gap, retaining observed gap
				for (int y = rog.top; y < rog.bottom; y++) {
					if (0 < exRowDiff[y - exTablesBox.top])
						exRowDiff[y - tableBox.top] = 0;
				}
				for (int ly = rog.bottom; ly < exTablesBox.bottom; ly++) {
					if (exRowDiff[ly - exTablesBox.top] <= 0)
						break; // reached end of existing gap
					else exRowDiff[ly - tableBox.top] = 0;
				}
				for (int ly = (rog.top - 1); ly >= exTablesBox.top; ly--) {
					if (exRowDiff[ly - exTablesBox.top] <= 0)
						break; // reached end of existing gap
					else exRowDiff[ly - tableBox.top] = 0;
				}
				
				//	set enforce flag of gap to prevent mergers across it
				rog.enforced = true;
			}
			
			//	add any existing row gaps not matched to some counterpart
			for (int y = tableBox.top; y < tableBox.bottom; y++) {
				if (exRowDiff[y - tableBox.top] <= 0)
					continue; // no way of starting a gap here
				for (int ly = (y + 1); ly < tableBox.bottom; ly++)
					if (exRowDiff[ly - tableBox.top] <= 0) {
						rowOccupationGaps.add(new RowOccupationGap(tableBoxStats, y, ly, true));
						y = ly; // no need to investigate this one again, so no need to compensate for loop increment
						break; // we're done with this one
					}
			}
			System.out.println("Adjusted row occupation gaps are " + rowOccupationGaps);
		}
		
		//	create markup
		return this.getTableMarkup(page, words, tableBox, tableBoxStats, minColMargin, minRowMargin, colOccupationLows, rowOccupationGaps, inLineTable);
	}
	
	private TableAreaMarkup getTableMarkup(ImPage page, ImWord[] words, BoundingBox tableBox, TableAreaStatistics tableBoxStats, int minColMargin, int minRowMargin, TreeSet colOccupationLows, TreeSet rowOccupationGaps, boolean inLineTable) {
		
		//	wrap region around words
		ImRegion tableRegion = new ImRegion(page.getDocument(), page.pageId, tableBox, ImRegion.TABLE_TYPE);
		if (inLineTable)
			tableRegion.setAttribute(ImRegion.IN_LINE_OBJECT_MARKER_ATTRIBUTE);
		
		//	create columns based on reduced column gaps from statistics
		ColumnOccupationLow[] areaColGaps = ((ColumnOccupationLow[]) colOccupationLows.toArray(new ColumnOccupationLow[colOccupationLows.size()]));
		TableAreaColumn[] areaCols = new TableAreaColumn[(areaColGaps.length * 2) + 1];
		for (int c = 0; c <= areaColGaps.length; c++) {
			int cLeft = ((c == 0) ? tableBox.left : areaColGaps[c-1].right);
			int cRight = ((c == areaColGaps.length) ? tableBox.right : areaColGaps[c].left);
			areaCols[c * 2] = new TableAreaColumn(false, cLeft, cRight);
			if (c < areaColGaps.length)
				areaCols[(c * 2) + 1] = new TableAreaColumn(true, areaColGaps[c].left, areaColGaps[c].right, areaColGaps[c].enforced);
		}
		
		//	create rows based on row gaps from statistics
		RowOccupationGap[] areaRowGaps = ((RowOccupationGap[]) rowOccupationGaps.toArray(new RowOccupationGap[rowOccupationGaps.size()]));
		TableAreaRow[] areaRows = new TableAreaRow[(areaRowGaps.length * 2) + 1];
		for (int r = 0; r <= areaRowGaps.length; r++) {
			int rTop = ((r == 0) ? tableBox.top : areaRowGaps[r-1].bottom);
			int rBottom = ((r == areaRowGaps.length) ? tableBox.bottom : areaRowGaps[r].top);
			areaRows[r * 2] = new TableAreaRow(false, rTop, rBottom);
			if (r < areaRowGaps.length)
				areaRows[(r * 2) + 1] = new TableAreaRow(true, areaRowGaps[r].top, areaRowGaps[r].bottom, areaRowGaps[r].enforced);
		}
		
		//	create raw cells from raw columns and rows
		TableAreaCell[][] areaCells = new TableAreaCell[areaCols.length][areaRows.length];
		for (int c = 0; c < areaCols.length; c++)
			for (int r = 0; r < areaRows.length; r++) {
				areaCells[c][r] = new TableAreaCell(areaCols[c], areaRows[r]);
				areaCells[c][r].updateWords(words); // automatically flags any space cell as bridged if it contains words
			}
		
		
		//	compute minimum row index safe to assume non-header
		int minSureNonHeaderRowIndex = Math.min(((areaRows.length + 2) / 3) /* top third of rows */, (4 * 2 /* consider interspersed spacers */) /* assume at most four header rows */);
		
		//	widen columns in data part of table (about lower two thirds) to accommodate the occasional over-length string
		//	TODO maybe detect row group label rows first (even though they will hardly pass spacing checks)
		for (int c = 0; c < (areaCols.length - 1); c += 2) {
			
			//	check if left and right columns are left aligned
			LinkedHashSet colOccupationFlanks = tableBoxStats.getBrgColOccupationFlanks();
			boolean leftColIsLeftAligned = false;
			boolean rightColIsLeftAligned = false;
			for (Iterator cofit = colOccupationFlanks.iterator(); cofit.hasNext();) {
				ColumnOccupationFlank cof = ((ColumnOccupationFlank) cofit.next());
				if (!cof.isRise)
					continue; // we need a rising flank for left alignment
				if ((cof.height * 3) < (tableBoxStats.wordPixelRows * 2))
					continue; // we need a well pronounced rise for left alignment
				if (Math.abs(cof.center - areaCols[c].min) < 3)
					leftColIsLeftAligned = true;
				if (Math.abs(cof.center - areaCols[c+2].min) < 3)
					rightColIsLeftAligned = true;
			}
			if (!leftColIsLeftAligned || !rightColIsLeftAligned)
				continue; // we need two left aligned columns for widening gap
			
			//	find right edges of widened column
			int[] wColRightMaxRow = new int[areaCols[c+1].getSpan()];
			Arrays.fill(wColRightMaxRow, -1); // keep track of lowest (largest index) rows to reach into each pixel of extended width
			for (int r = 0; r < areaRows.length; r += 2) {
				if (areaCells[c+1][r].wordBounds == null)
					continue; // no need to bother with empty spacer cell
				if ((areaCols[c+1].max - areaCells[c+1][r].wordBounds.right) < (areaCells[c+1][r].wordBounds.left - areaCols[c+1].min))
					continue; // right distance less than left distance, no way of attaching leftward
				if (areaCols[c+1].max < (areaCells[c+1][r].wordBounds.right + (tableBoxStats.avgSpaceWidth / 2)))
					continue; // spacer cell words reach too close (within half a norm space) to or even into right data column, no way of attaching leftward
				if ((areaCols[c+1].min + tableBoxStats.avgSpaceWidth) < areaCells[c+1][r].wordBounds.left)
					continue; // spacer cell words do not reach within a norm space of left data column, no way of attaching
				for (int x = 0; x < (areaCells[c+1][r].wordBounds.right - areaCols[c+1].min); x++)
					wColRightMaxRow[x] = r; // remember last row each extended width comes from (helps refuse those from likely headers)
			}
			
			//	anything to widen at all?
			if (wColRightMaxRow[0] == -1)
				continue; // not a single cell to widen
			if (wColRightMaxRow[0] < minSureNonHeaderRowIndex)
				continue; // only header cell to widen
			
			//	find right edge of widened column
			int wColRight = areaCols[c].max;
			for (int x = 0; x < wColRightMaxRow.length; x++)
				if (wColRightMaxRow[x] < minSureNonHeaderRowIndex) {
					wColRight = (areaCols[c].max + x);
					break;
				}
			if (wColRight == areaCols[c].max)
				continue;
			
			//	widen left column and update cell words
			areaCols[c].max = wColRight;
			areaCols[c+1].min = wColRight;
			for (int r = 0; r < areaRows.length; r++) {
				areaCells[c][r].updateWords(words);
				areaCells[c+1][r].isBridged = false; // reset bridged flag
				areaCells[c+1][r].updateWords(words); // automatically re-flags space cell as bridged if it still contains words
			}
		}
		
		//	un-bridge spacer cells that have wide gap in middle, and widen adjacent columns accordingly
		for (int c = 1; c < areaCols.length; c += 2) {
			
			//	this one is already too narrow to yield further space to content of adjacent columns
			if (areaCols[c].getSpan() < tableBoxStats.avgWordHeight)
				continue;
			
			//	get bridging word boundaries
			int maxLeftWordRight = areaCols[c].min;
			int minRightWordLeft = areaCols[c].max;
			for (int r = 0; r < areaRows.length; r += 2) {
				if (areaCells[c][r].wordBounds == null)
					continue; // nothing to work with in this one
				
				//	compute word distribution in bridging cell
				int[] cellColOccupations = new int[areaCols[c].getSpan()];
				Arrays.fill(cellColOccupations, 0);
				for (int w = 0; w < areaCells[c][r].words.length; w++) {
					for (int x = Math.max(areaCols[c].min, areaCells[c][r].words[w].bounds.left); x < Math.min(areaCols[c].max, areaCells[c][r].words[w].bounds.right); x++)
						cellColOccupations[x - areaCols[c].min]++;
				}
				
				//	check for gap in middle
				int leftNonZeroX = (areaCols[c].getSpan() / 2);
				while ((leftNonZeroX > 0) && (cellColOccupations[leftNonZeroX-1] == 0))
					leftNonZeroX--;
				int rightNonZeroX = (areaCols[c].getSpan() / 2);
				while ((rightNonZeroX < cellColOccupations.length) && (cellColOccupations[rightNonZeroX] == 0))
					rightNonZeroX++;
				
				//	do we have wide enough a gap?
				if ((rightNonZeroX - leftNonZeroX) < tableBoxStats.avgWordHeight)
					continue;
				
				//	remember word boundaries
				maxLeftWordRight = Math.max(maxLeftWordRight, (leftNonZeroX + areaCols[c].min));
				minRightWordLeft = Math.min(minRightWordLeft, (rightNonZeroX + areaCols[c].min));
			}
			
			//	do we have wide enough a gap overall?
			if ((minRightWordLeft - maxLeftWordRight) < tableBoxStats.avgWordHeight)
				continue;
			
			//	widen adjacent columns and update cell words
			areaCols[c-1].max = maxLeftWordRight;
			areaCols[c].min = maxLeftWordRight;
			areaCols[c].max = minRightWordLeft;
			areaCols[c+1].min = minRightWordLeft;
			for (int r = 0; r < areaRows.length; r++) {
				areaCells[c][r].isBridged = false; // reset bridged flag
				areaCells[c][r].updateWords(words); // automatically re-flags space cell as bridged if it still contains words
				areaCells[c-1][r].updateWords(words);
				areaCells[c+1][r].updateWords(words);
			}
		}
		
		
//		//	TODO for test purposes, visualize table area graphics and checkerboard cells
//		BufferedImage bi = new BufferedImage(tableBoxStats.grImage.getWidth(), tableBoxStats.grImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
//		Graphics2D gr = bi.createGraphics();
//		gr.setColor(Color.WHITE);
//		gr.fillRect(0, 0, bi.getWidth(), bi.getHeight());
//		gr.drawImage(tableBoxStats.grImage, 0, 0, null);
//		gr.translate(-Math.min(tableBoxStats.bounds.left, tableBoxStats.grBounds.left), -Math.min(tableBoxStats.bounds.top, tableBoxStats.grBounds.top));
//		for (int c = 0; c < areaCells.length; c++)
//			for (int r = 0; r < areaCells[c].length; r++) {
//				BoundingBox cellBox = new BoundingBox(areaCols[c].min, areaCols[c].max, areaRows[r].min, areaRows[r].max);
//				if ((cellBox.getWidth() <= 0) || (cellBox.getHeight() <= 0))
//					continue;
//				if (((c % 2) == 0) && ((r % 2) == 0)) {
//					gr.setColor(Color.GREEN);
//					gr.drawRect(cellBox.left, cellBox.top, cellBox.getWidth(), cellBox.getHeight());
//					continue;
//				}
//				GraphicsAreaBrightness gab = tableBoxStats.getGrBrightness(cellBox);
//				if (gab.minBrightness < Byte.MAX_VALUE) {
//					gr.setColor(new Color(255, 0, 0, 64));
//					gr.fillRect(cellBox.left, cellBox.top, cellBox.getWidth(), cellBox.getHeight());
//				}
//				if (areaCells[c][r].isBridged) {
//					gr.setColor(new Color(0, 255, 0, 64));
//					gr.fillRect(cellBox.left, cellBox.top, cellBox.getWidth(), cellBox.getHeight());
//				}
//			}
//		ImageDisplayDialog grIdd = new ImageDisplayDialog("Table Area Graphics in Page " + page.pageId + " at " + tableBox);
//		grIdd.addImage(bi, "");
//		grIdd.setSize(600, 800);
//		grIdd.setLocationRelativeTo(null);
//		grIdd.setVisible(true);
//		
		/* There are three categories of rows:
		 * - regular data rows (no action to take)
		 *   - multiple non-bridged occupied cells
		 *   - (about) average gaps above and below them
		 * 
		 * - row group labels (to merge through all columns)
		 *   - average or above gaps above and below them
		 *   - single (streak of) occupied cell(s), starting at very left
		 *   - possibly prominent font (bold or larger-than-average)
		 * 
		 * - overflow rows (to merge with row above or below them)
		 *   - narrower-than-average gap towards data row they belong to
		 *   - at most the same cells occupied as data row they belong to
		 *   - not bridging any column gaps open in data row they belong to
		 */
		
		
		/* TODO maybe detect header/data split first, and compute row mergers separately:
		 * - there tends to be a line between headers and actual data ==> get them from page
		 * - headers are quite often bold ==> check font style breaks (we compute that stuff already)
		 * - even numeric or symbolic columns tend to have text headers ==> do check data types
		 */
		
		//	assess row properties (margins, font properties, bridging adjacent spacers, occupied cells, ...)
		assessAreaRowProperties(areaCols, areaRows, areaCells);
		
		//	compute average row margin (ignore peak blocked row gaps, as they tend to add some space around lines)
		int rowMarginSum = 0;
		int nonPeakRowMarginCount = 0;
		int nonPeakRowMarginSum = 0;
		for (Iterator rogit = rowOccupationGaps.iterator(); rogit.hasNext();) {
			RowOccupationGap rog = ((RowOccupationGap) rogit.next());
			rowMarginSum += rog.getHeight();
			if (!rog.isBlockedByPeak()) {
				nonPeakRowMarginCount++;
				nonPeakRowMarginSum += rog.getHeight();
			}
		}
		int avgRowMargin = ((rowMarginSum + (rowOccupationGaps.size() / 2)) / rowOccupationGaps.size());
		int avgNonPeakRowMargin = ((nonPeakRowMarginCount == 0) ? avgRowMargin : ((nonPeakRowMarginSum + (nonPeakRowMarginCount / 2)) / nonPeakRowMarginCount));
		
		//	we have some graphics blocked row gaps ==> take column header row gaps out of average
		if (nonPeakRowMarginCount < rowOccupationGaps.size()) {
			int rowMarginCount = rowOccupationGaps.size();
			for (Iterator rogit = rowOccupationGaps.iterator(); rogit.hasNext();) {
				RowOccupationGap rog = ((RowOccupationGap) rogit.next());
				if (rog.isBlockedByPeak())
					break; // stop at first graphically marked row gap (conservatively regarding it as marking end of column header area)
				rowMarginCount--;
				rowMarginSum -= rog.getHeight();
				nonPeakRowMarginCount--;
				nonPeakRowMarginSum -= rog.getHeight();
			}
			avgRowMargin = ((rowMarginSum + (rowMarginCount / 2)) / rowMarginCount);
			avgNonPeakRowMargin = ((nonPeakRowMarginCount == 0) ? avgRowMargin : ((nonPeakRowMarginSum + (nonPeakRowMarginCount / 2)) / nonPeakRowMarginCount));
		}
		
		//	we have no graphically marked row gaps at all ==> take topmost (column header) row gap out of average
		else if (rowOccupationGaps.size() > 1) {
			int rowMarginCount = rowOccupationGaps.size();
			RowOccupationGap topRog = ((RowOccupationGap) rowOccupationGaps.iterator().next());
			rowMarginCount--;
			rowMarginSum -= topRog.getHeight();
			nonPeakRowMarginCount--;
			nonPeakRowMarginSum -= topRog.getHeight();
			avgRowMargin = ((rowMarginSum + (rowMarginCount / 2)) / rowMarginCount);
			avgNonPeakRowMargin = ((nonPeakRowMarginCount == 0) ? avgRowMargin : ((nonPeakRowMarginSum + (nonPeakRowMarginCount / 2)) / nonPeakRowMarginCount));
		}
		
		for (int r = 0; r < areaRows.length; r += 2)
			System.out.println("Row " + (r / 2) + ": " + areaRows[r].occupiedCellCount + " cells occupied, space below is " + areaRows[r].belowRowGap);
		
		//	determine row gap margin threshold that will leave us with sensible number of rows
		//	TODO try accumulation point analysis
		int minNonMergeRowMargin = avgNonPeakRowMargin;
		do {
			
			//	check what current merging threshold would leave us with
			int nonMergeRowCount = 1; // we have one row more than gaps separating them
			int maxMergeRowMargin = 0;
			for (Iterator rogit = rowOccupationGaps.iterator(); rogit.hasNext();) {
				RowOccupationGap rog = ((RowOccupationGap) rogit.next());
				if (rog.isBlockedByGraphics())
					nonMergeRowCount++; // cannot merge across blocking graphics ==> will always yield one more row
				else if (minNonMergeRowMargin <= rog.getHeight())
					nonMergeRowCount++; // would not merge this one under current margin ==> would yield one more row
				else maxMergeRowMargin = Math.max(maxMergeRowMargin, rog.getHeight());
			}
			
			//	nothing merged at all, no need for any (further) reduction
			if (nonMergeRowCount == ((areaRows.length + 1) / 2))
				break;
			
			//	check if current threshold makes sense
			if (nonMergeRowCount < 3) { /* just too few rows */ }
			else if ((nonMergeRowCount * 7) < ((areaRows.length + 1) / 2)) { /* still too few rows for such a large table, we rarely have more than 6 lines per cell */ }
			else if ((maxMergeRowMargin + 1) == minNonMergeRowMargin) { /* threshold lies inside larger group of observed margins, too insecure */ }
			else break; // current margin leaves sensible number of rows, let's work with that
			
			//	let's try where smaller threshold would take us
			minNonMergeRowMargin--;
		} while (true); // loop body knows when to stop, and keeps that knowledge scoped in
		System.out.println(" ==> minimum non-mergeable row margin is " + minNonMergeRowMargin);
		
		//	determine row mergers
		char[] areaRowMergeDirections = getAreaRowMergers(areaRows, areaCells, minNonMergeRowMargin);
		
		//	check for saw blade row occupation pattern if no rows mergeable at all (only with minimum row gaps below (DPI/25), though)
		if ((minNonMergeRowMargin <= minRowMargin) && (minRowMargin <= ((page.getImageDPI() + 13) / 25)))
			handleCellOccupationSawBladePatterns(((areaCols.length + 1) / 2), areaRows, areaCells, areaRowMergeDirections);
		
		//	count unambiguous mergers in both directions to decide ambiguous ones
		int areaRowMergeCount = 0;
		int areaRowUpwardMergeCount = 0;
		int areaRowDownwardMergeCount = 0;
		for (int r = 0; r < areaRows.length; r += 2) {
			if (areaRowMergeDirections[r] != 'N')
				areaRowMergeCount++;
			if (areaRowMergeDirections[r] == 'U')
				areaRowUpwardMergeCount++;
			else if (areaRowMergeDirections[r] == 'D')
				areaRowDownwardMergeCount++;
		}
		
		//	decide ambiguous mergers (with a little bias towards merging upwards, as that is more frequent)
		for (int r = 0; r < areaRows.length; r += 2) {
			if (areaRowMergeDirections[r] == 'A')
				areaRowMergeDirections[r] = ((areaRowUpwardMergeCount < areaRowDownwardMergeCount) ? 'D' : 'U');
		}
		
		//	perform row mergers (if any)
		if (areaRowMergeCount != 0) {
			
			//	merge rows proper
			ArrayList mAreaRowList = new ArrayList();
			int rTop = tableBox.top;
			for (int r = 0; r <= areaRows.length; r += 2) {
				if (areaRowMergeDirections[r] == 'D')
					continue;
				if ((r + 1) == areaRows.length)
					mAreaRowList.add(new TableAreaRow(false, rTop, tableBox.bottom));
				else if (areaRowMergeDirections[r + 2] != 'U') {
					mAreaRowList.add(new TableAreaRow(false, rTop, areaRows[r].max));
					mAreaRowList.add(new TableAreaRow(true, areaRows[r + 1].min, areaRows[r + 1].max));
					rTop = areaRows[r + 2].min;
				}
			}
			TableAreaRow[] mAreaRows = ((TableAreaRow[]) mAreaRowList.toArray(new TableAreaRow[mAreaRowList.size()]));
			
			//	merge cells
			TableAreaCell[][] mAreaCells = new TableAreaCell[areaCols.length][mAreaRows.length];
			for (int c = 0; c < areaCols.length; c++)
				for (int r = 0; r < mAreaRows.length; r++) {
					mAreaCells[c][r] = new TableAreaCell(areaCols[c], mAreaRows[r]);
					mAreaCells[c][r].updateWords(words); // automatically flags any space cell as bridged if it contains words
					
					//	transfer any bridged property from raw cells (for spacer rows only)
					if ((r % 2) == 1) {
						for (int lr = 0; lr < areaRows.length; lr++)
							if ((areaRows[lr].min == mAreaRows[r].min) && (areaRows[lr].max == mAreaRows[r].max) && areaCells[c][lr].isBridged) {
								mAreaCells[c][r].isBridged = true;
								break;
							}
					}
					
					//	transfer any bridged property from raw cells (for data rows in spacer columns only)
					else if ((c % 2) == 1) {
						for (int lr = 0; lr < areaRows.length; lr++)
							if ((mAreaRows[r].min <= areaRows[lr].min) && (areaRows[lr].max <= mAreaRows[r].max) && areaCells[c][lr].isBridged) {
								mAreaCells[c][r].isBridged = true;
								break;
							}
					}
				}
			
			//	assess row properties (margins, font properties, bridging adjacent spacers, occupied cells, ...)
			assessAreaRowProperties(areaCols, mAreaRows, mAreaCells);
			
			//	switch to merged rows
			areaRows = mAreaRows;
			areaCells = mAreaCells;
		}
		
		//	detect fully bridged left anchored and centered (streaks of connected) cells (row group labels)
		mergeRowGroupLabels(tableBox, areaCols, areaRows, avgNonPeakRowMargin, areaCells);
		
		//	assess column properties (margins, font properties, bridging adjacent spacers, occupied cells, ...)
		assessAreaColumnProperties(areaCols, areaRows, areaCells);
		
		//	determine column mergers
		char[] areaColMergeDirections = getAreaColumnMergers(areaCols, areaRows, areaCells, tableBoxStats.avgWordHeight);
		
		//	count unambiguous mergers in both directions to decide ambiguous ones
		int areaColMergeCount = 0;
		for (int c = 0; c < areaCols.length; c += 2) {
			if (areaColMergeDirections[c] != 'N')
				areaColMergeCount++;
		}
		
		//	perform column mergers (if any)
		if (areaColMergeCount != 0) {
			
			//	merge rows proper
			ArrayList mAreaColList = new ArrayList();
			int cLeft = tableBox.left;
			for (int c = 0; c <= areaCols.length; c += 2) {
				if (areaColMergeDirections[c] == 'R')
					continue;
				if ((c + 1) == areaCols.length)
					mAreaColList.add(new TableAreaColumn(false, cLeft, tableBox.right));
				else if (areaColMergeDirections[c] == 'X') {
					((TableAreaColumn) mAreaColList.get(mAreaColList.size() - 1)).max = areaCols[c + 1].max;
					cLeft = areaCols[c + 2].min;
				}
				else if (areaColMergeDirections[c + 2] != 'L') {
					mAreaColList.add(new TableAreaColumn(false, cLeft, areaCols[c].max));
					mAreaColList.add(new TableAreaColumn(true, areaCols[c + 1].min, areaCols[c + 1].max));
					cLeft = areaCols[c + 2].min;
				}
			}
			TableAreaColumn[] mAreaCols = ((TableAreaColumn[]) mAreaColList.toArray(new TableAreaColumn[mAreaColList.size()]));
			
			//	merge cells
			TableAreaCell[][] mAreaCells = new TableAreaCell[mAreaCols.length][areaRows.length];
			for (int c = 0; c < mAreaCols.length; c++)
				for (int r = 0; r < areaRows.length; r++) {
					mAreaCells[c][r] = new TableAreaCell(mAreaCols[c], areaRows[r]);
					mAreaCells[c][r].updateWords(words); // automatically flags any space cell as bridged if it contains words
					
					//	transfer any bridged property from raw cells (for spacer columns only)
					if ((c % 2) == 1) {
						for (int lc = 0; lc < areaCols.length; lc++)
							if ((areaCols[lc].min == mAreaCols[c].min) && (areaCols[lc].max == mAreaCols[c].max) && areaCells[lc][r].isBridged) {
								mAreaCells[c][r].isBridged = true;
								break;
							}
					}
					
					//	transfer any bridged property from raw cells (for data columns in spacer rows only)
					else if ((r % 2) == 1) {
						for (int lc = 0; lc < areaCols.length; lc++)
							if ((mAreaCols[c].min <= areaCols[lc].min) && (areaCols[lc].max <= mAreaCols[c].max) && areaCells[lc][r].isBridged) {
								mAreaCells[c][r].isBridged = true;
								break;
							}
					}
				}
			
			//	assess column properties (margins, font properties, bridging adjacent spacers, occupied cells, ...)
			assessAreaColumnProperties(mAreaCols, areaRows, mAreaCells);
			
			//	switch to merged columns
			areaCols = mAreaCols;
			areaCells = mAreaCells;
		}
		
		//	pull columns snug around cells not bridging out
		for (int c = 0; c < areaCols.length; c += 2) {
			
			//	compute extent of words not bridging out
			int minWordLeft = areaCols[c].max;
			int maxWordRight = areaCols[c].min;
			for (int r = 0; r < areaRows.length; r +=2) {
				if (areaCells[c][r].wordBounds == null)
					continue;
				if ((c == 0) || !areaCells[c-1][r].isBridged)
					minWordLeft = Math.min(minWordLeft, areaCells[c][r].wordBounds.left);
				if (((c+1) == areaCols.length) || !areaCells[c+1][r].isBridged)
					maxWordRight = Math.max(maxWordRight, areaCells[c][r].wordBounds.right);
			}
			
			//	anything to work with?
			if (minWordLeft < maxWordRight) {
				areaCols[c].min = minWordLeft;
				if (c != 0)
					areaCols[c-1].max = minWordLeft;
				areaCols[c].max = maxWordRight;
				if ((c+1) != areaCols.length)
					areaCols[c+1].min = maxWordRight;
			}
		}
		
		//	pull rows snug around cells not bridging out
		for (int r = 0; r < areaRows.length; r +=2) {
			
			//	compute extent of words not bridging out
			int minWordTop = areaRows[r].max;
			int maxWordBottom = areaRows[r].min;
			for (int c = 0; c < areaCols.length; c += 2) {
				if (areaCells[c][r].wordBounds == null)
					continue;
				if ((r == 0) || !areaCells[c][r-1].isBridged)
					minWordTop = Math.min(minWordTop, areaCells[c][r].wordBounds.top);
				if (((r+1) == areaRows.length) || !areaCells[c][r+1].isBridged)
					maxWordBottom = Math.max(maxWordBottom, areaCells[c][r].wordBounds.bottom);
			}
			
			//	anything to work with?
			if (minWordTop < maxWordBottom) {
				areaRows[r].min = minWordTop;
				if (r != 0)
					areaRows[r-1].max = minWordTop;
				areaRows[r].max = maxWordBottom;
				if ((r+1) != areaRows.length)
					areaRows[r+1].min = maxWordBottom;
			}
		}
		
		//	if we have a full grid, merge cells to correspond to those given graphically
		if (isFullGrid(colOccupationLows, rowOccupationGaps)) {
			
			//	copy graphics image, and subtract edge pixels to prevent any flow-around in a grid without the framing box
			BufferedImage bi = new BufferedImage((tableBoxStats.grImage.getWidth() - 4), (tableBoxStats.grImage.getHeight() - 4), BufferedImage.TYPE_BYTE_GRAY);
//			BufferedImage bi = new BufferedImage((tableBoxStats.grImage.getWidth() - 4), (tableBoxStats.grImage.getHeight() - 4), BufferedImage.TYPE_INT_ARGB);
			Graphics2D gr = bi.createGraphics();
			gr.setColor(Color.WHITE);
			gr.fillRect(0, 0, bi.getWidth(), bi.getHeight());
			gr.drawImage(tableBoxStats.grImage, -2, -2, null);
			
			//	wrap graphic image
			AnalysisImage ai = Imaging.wrapImage(bi, null);
			
			//	obtain region coloring (negative threshold to color light regions)
			int[][] abiRegionColors = Imaging.getRegionColoring(ai, ((byte) -96), false);
			int maxRegionColor = 0;
			for (int x = 0; x < bi.getWidth(); x++) {
				for (int y = 0; y < bi.getHeight(); y++)
					maxRegionColor = Math.max(maxRegionColor, abiRegionColors[x][y]);
			}
			int[] regionColorMinX = new int[maxRegionColor];
			Arrays.fill(regionColorMinX, Integer.MAX_VALUE);
			int[] regionColorMaxX = new int[maxRegionColor];
			Arrays.fill(regionColorMaxX, Integer.MIN_VALUE);
			int[] regionColorMinY = new int[maxRegionColor];
			Arrays.fill(regionColorMinY, Integer.MAX_VALUE);
			int[] regionColorMaxY = new int[maxRegionColor];
			Arrays.fill(regionColorMaxY, Integer.MIN_VALUE);
			for (int x = 0; x < bi.getWidth(); x++)
				for (int y = 0; y < bi.getHeight(); y++) {
					if (abiRegionColors[x][y] == 0)
						continue;
					int rci = (abiRegionColors[x][y] - 1);
					regionColorMinX[rci] = Math.min(regionColorMinX[rci], x);
					regionColorMaxX[rci] = Math.max(regionColorMaxX[rci], x);
					regionColorMinY[rci] = Math.min(regionColorMinY[rci], x);
					regionColorMaxY[rci] = Math.max(regionColorMaxY[rci], x);
				}
			
			//	fix offsets of region color array against page coordinates
			int leftOffset = (tableBoxStats.grBounds.left + 2);
			int topOffset = (tableBoxStats.grBounds.top + 2);
			
			//	horizontally merge cells of same region color
			for (int c = 1; c < areaCells.length; c += 2)
				for (int r = 0; r < areaCells[c].length; r += 2) {
					if (areaCells[c][r].isBridged)
						continue; // no need to bother with this one
					
					//	compute region colors of cells to left and right
					int leftCenterX = ((areaCols[c - 1].min + areaCols[c - 1].max) / 2);
					int rightCenterX = ((areaCols[c + 1].min + areaCols[c + 1].max) / 2);
					int centerY = ((areaRows[r].min + areaRows[r].max) / 2);
					int leftRci = (abiRegionColors[leftCenterX - leftOffset][centerY - topOffset] - 1);
					int rightRci = (abiRegionColors[rightCenterX - leftOffset][centerY - topOffset] - 1);
					if (leftRci != rightRci)
						continue; // different colors
					
					//	check region extent
					if (tableBoxStats.grBounds.getWidth() < ((regionColorMaxX[leftRci] - regionColorMinX[leftRci]) * 2))
						continue; // over half of table width
					if (tableBoxStats.grBounds.getHeight() < ((regionColorMaxY[leftRci] - regionColorMinY[leftRci]) * 2))
						continue; // over half of table height
					if (tableBoxStats.grBounds.getWidth() < ((regionColorMaxX[rightRci] - regionColorMinX[rightRci]) * 2))
						continue; // over half of table width
					if (tableBoxStats.grBounds.getHeight() < ((regionColorMaxY[rightRci] - regionColorMinY[rightRci]) * 2))
						continue; // over half of table height
					
					//	merge cells
					areaCells[c][r].isBridged = true;
				}
			
			//	vertically merge cells of same region color
			for (int c = 0; c < areaCells.length; c += 2)
				for (int r = 1; r < areaCells[c].length; r += 2) {
					if (areaCells[c][r].isBridged)
						continue; // no need to bother with this one
					
					//	compute region colors of cells above and below
					int centerX = ((areaCols[c].min + areaCols[c].max) / 2);
					int topCenterY = ((areaRows[r - 1].min + areaRows[r - 1].max) / 2);
					int bottomCenterY = ((areaRows[r + 1].min + areaRows[r + 1].max) / 2);
					int topRci = (abiRegionColors[centerX - leftOffset][topCenterY - topOffset] - 1);
					int bottomRci = (abiRegionColors[centerX - leftOffset][bottomCenterY - topOffset] - 1);
					if (topRci != bottomRci)
						continue; // different colors
					
					//	check region extent
					if (tableBoxStats.grBounds.getWidth() < ((regionColorMaxX[topRci] - regionColorMinX[topRci]) * 2))
						continue; // over half of table width
					if (tableBoxStats.grBounds.getHeight() < ((regionColorMaxY[topRci] - regionColorMinY[topRci]) * 2))
						continue; // over half of table height
					if (tableBoxStats.grBounds.getWidth() < ((regionColorMaxX[bottomRci] - regionColorMinX[bottomRci]) * 2))
						continue; // over half of table width
					if (tableBoxStats.grBounds.getHeight() < ((regionColorMaxY[bottomRci] - regionColorMinY[bottomRci]) * 2))
						continue; // over half of table height
					
					//	merge cells
					areaCells[c][r].isBridged = true;
				}
//			
//			//	TODO use this visualization for trouble shooting (switch to ARGB image above)
//			HashMap regionColors = new HashMap();
//			for (int x = 0; x < bi.getWidth(); x++) {
//				for (int y = 0; y < bi.getHeight(); y++) {
//					if (abiRegionColors[x][y] == 0)
//						continue;
//					Color c = ((Color) (regionColors.get(Integer.valueOf(abiRegionColors[x][y]))));
//					if (c == null) {
//						c = new Color(Color.HSBtoRGB(((float) Math.random()), 1f, 1f));
//						regionColors.put(Integer.valueOf(abiRegionColors[x][y]), c);
//					}
//					bi.setRGB(x, y, c.getRGB());
//				}
//			}
//			ImageDisplayDialog grIdd = new ImageDisplayDialog("Table Area Graphics in Page " + page.pageId + " at " + tableBox);
//			grIdd.addImage(bi, "");
//			grIdd.setSize(600, 800);
//			grIdd.setLocationRelativeTo(null);
//			grIdd.setVisible(true);
		}
		
		//	mark final columns and rows
		ImRegion[] tableCols = new ImRegion[(areaCols.length + 1) / 2];
		for (int c = 0; c < tableCols.length; c++)
			tableCols[c] = new ImRegion(page.getDocument(), page.pageId, new BoundingBox(areaCols[c * 2].min, areaCols[c * 2].max, tableBox.top, tableBox.bottom), ImRegion.TABLE_COL_TYPE);
		ImRegion[] tableRows = new ImRegion[(areaRows.length + 1) / 2];
		for (int r = 0; r < tableRows.length; r++)
			tableRows[r] = new ImRegion(page.getDocument(), page.pageId, new BoundingBox(tableBox.left, tableBox.right, areaRows[r * 2].min, areaRows[r * 2].max), ImRegion.TABLE_ROW_TYPE);
		
		//	mark final cells, observing merger flags
		ImRegion[][] tableCells = new ImRegion[tableCols.length][tableRows.length];
		for (int c = 0; c < tableCols.length; c++) {
			for (int r = 0; r < tableRows.length; r++) {
				if (tableCells[c][r] != null)
					continue; // covered previously by merged cell
				
				//	find range of cell
				int lc = c;
				for (; lc < tableCols.length; lc++) {
					if (((lc+1) == tableCols.length) || !areaCells[(lc * 2) + 1][r * 2].isBridged)
						break;
				}
				int lr = r;
				for (; lr < tableRows.length; lr++) {
					if (((lr+1) == tableRows.length) || !areaCells[c * 2][(lr * 2) + 1].isBridged)
						break;
				}
				
				//	create and store cell
				ImRegion cell = new ImRegion(page.getDocument(), page.pageId, new BoundingBox(areaCols[c * 2].min, areaCols[lc * 2].max, areaRows[r * 2].min, areaRows[lr * 2].max), ImRegion.TABLE_CELL_TYPE);
				for (int sc = c; sc <= lc; sc++) {
					for (int sr = r; sr <= lr; sr++)
						tableCells[sc][sr] = cell;
				}
			}
		}
		
		//	return resulting markup
		return new TableAreaMarkup(page, words, tableRegion, tableCols, tableRows, tableCells);
	}
	
	private static class TableAreaMarkup {
		final ImPage page;
		final ImWord[] words;
		final ImRegion table;
		final ImRegion[] cols;
		final ImRegion[] rows;
		final ImRegion[][] cells;
		
		TableAreaMarkup(ImPage page, ImWord[] words, ImRegion table, ImRegion[] cols, ImRegion[] rows, ImRegion[][] cells) {
			this.page = page;
			this.words = words;
			this.table = table;
			this.cols = cols;
			this.rows = rows;
			this.cells = cells;
		}
	}
	
	private static class TableAreaSlice {
		final boolean isSpace;
		final boolean isEnforced;
		
		int min;
		int max;
		
		int wordCount = 0;
		int boldWordCount = 0;
		int italicsWordCount = 0;
		
		int wordFontSizeCount = 0;
		int wordFontSizeSum = 0;
		int wordFontSizeMin = 72;
		int wordFontSizeMax = 0;
		
		TableAreaSlice(boolean isSpace, int min, int max) {
			this(isSpace, min, max, false);
		}
		TableAreaSlice(boolean isSpace, int min, int max, boolean isEnforced) {
			this.isSpace = isSpace;
			this.min = min;
			this.max = max;
			this.isEnforced = isEnforced;
		}

		int getSpan() {
			return (this.max - this.min);
		}
		
		boolean isBold() {
			return ((this.boldWordCount * 10) > (this.wordCount * 9)); // >90% of words in bold
		}
		boolean isItalics() {
			return ((this.italicsWordCount * 10) > (this.wordCount * 9)); // >90% of words in italics
		}
		int getFontSize() {
			return ((this.wordFontSizeCount == 0) ? 0 : ((this.wordFontSizeSum + (this.wordFontSizeCount / 2)) / this.wordFontSizeCount));
		}
	}
	
	private static class TableAreaColumn extends TableAreaSlice {
		int leftColGap = -1;
		int rightColGap = -1;
//		
//		int bridgedLeftCount = 0;
//		int bridgedRightCount = 0;
		
		int occupiedCellCount = 0;
		int disjointOccupiedCellCount = 0;
		int minOccupiedCellRow = Integer.MAX_VALUE;
		int maxOccupiedCellRow = Integer.MIN_VALUE;
		
		TableAreaColumn(boolean isSpace, int min, int max) {
			super(isSpace, min, max);
		}
		TableAreaColumn(boolean isSpace, int min, int max, boolean isEnforced) {
			super(isSpace, min, max, isEnforced);
		}
	}
	
	private static char[] getAreaRowMergers(TableAreaRow[] areaRows, TableAreaCell[][] areaCells, int minNonMergeRowMargin) {
		char[] areaRowMergeDirections = new char[areaRows.length];
		Arrays.fill(areaRowMergeDirections, 'N'); // initialize to 'N' for 'none'
		for (int r = 0; r < areaRows.length; r += 2) {
			
			//	merge upward ignoring all other criteria if every populated cell is bridged to from top
			if ((r != 0) && !areaRows[r - 1].isEnforced && isRowGapFullyBridged(areaCells, r, (r - 2))) {
				areaRowMergeDirections[r] = 'U';
				continue;
			}
			
			//	merge downward ignoring all other criteria if every populated cell is bridged to from below
			if (((r+2) < areaRows.length) && !areaRows[r + 1].isEnforced && isRowGapFullyBridged(areaCells, r, (r + 2))) {
				areaRowMergeDirections[r] = 'D';
				continue;
			}
			
			//	test for upward merger (gap has to be not enforced, less than 90% the average, and more than 1 less than average to exclude rounding errors)
//			boolean mergeableUpward = ((r != 0) && !areaRows[r - 1].isEnforced && ((areaRows[r].aboveRowGap * 10) < (minNonMergeRowMargin * 9)) && ((areaRows[r].aboveRowGap + 1) < minNonMergeRowMargin));
			boolean mergeableUpward = ((r != 0) && isRowGapMergeable((r - 1), areaRows, areaCells, minNonMergeRowMargin));
			
			//	test for downward merger (gap has to be not enforced, less than 90% the average, and more than 1 less than average to exclude rounding errors)
//			boolean mergeableDownward = (((r+2) < areaRows.length) && !areaRows[r + 1].isEnforced && ((areaRows[r].belowRowGap * 10) < (minNonMergeRowMargin * 9)) && ((areaRows[r].belowRowGap + 1) < minNonMergeRowMargin));
			boolean mergeableDownward = (((r+2) < areaRows.length) && isRowGapMergeable((r + 1), areaRows, areaCells, minNonMergeRowMargin));
			
			//	no mergers for this row
			if (!mergeableUpward && !mergeableDownward)
				continue;
			
			//	check in-cell word length only if row is merely overflow in either direction
			boolean checkInCellWords = (mergeableUpward && mergeableDownward && (areaRows[r].occupiedCellCount < areaRows[r-2].occupiedCellCount) && (areaRows[r].occupiedCellCount < areaRows[r+2].occupiedCellCount));
			
			//	check cell compatibility
			mergeableUpward = (mergeableUpward && areRowCellsOverflowCompatibleWith(areaCells, r, (r - 2), checkInCellWords));
			mergeableDownward = (mergeableDownward && areRowCellsOverflowCompatibleWith(areaCells, r, (r + 2), checkInCellWords));
			
			//	no mergers for this row after considering cells
			if (!mergeableUpward && !mergeableDownward)
				continue;
			
			//	this one is unambiguous
			if (mergeableUpward != mergeableDownward) {
				if (mergeableUpward)
					areaRowMergeDirections[r] = 'U';
				else if (mergeableDownward)
					areaRowMergeDirections[r] = 'D';
				continue;
			}
//			
//			//	we have a clear spacial tendency (even with tolerance of 1 accounting for rounding errors)
//			if (1 < Math.abs(areaRows[r].aboveRowGap - areaRows[r].belowRowGap)) {
//				areaRowMergeDirections[r] = ((areaRows[r].aboveRowGap < areaRows[r].belowRowGap) ? 'U' : 'D');
//				continue;
//			}
			
			//	measure distance between individual cells
			int maxCellWordGapAbove = getMaxRowCellWordGap(areaCells, r, (r - 2));
			int maxCellWordGapBelow = getMaxRowCellWordGap(areaCells, r, (r + 2));
			
			//	we have a clear spacial tendency (even with tolerance of 1 accounting for rounding errors)
			if (1 < Math.abs(maxCellWordGapAbove - maxCellWordGapBelow)) {
				areaRowMergeDirections[r] = ((maxCellWordGapAbove < maxCellWordGapBelow) ? 'U' : 'D');
				continue;
			}
			
			//	prefer merging into more occupied row (higher likelihood of overflow)
			if (areaRows[r-2].occupiedCellCount != areaRows[r+2].occupiedCellCount) {
				areaRowMergeDirections[r] = ((areaRows[r-2].occupiedCellCount < areaRows[r].occupiedCellCount) ? 'D' : 'U');
				continue;
			}
			
			//	row above merging upward, follow suite
			if ((r != 0) && (areaRowMergeDirections[r-2] == 'U'))
				areaRowMergeDirections[r] = 'U';
			
			//	row above merging downward, follow suite
			else if ((r != 0) && (areaRowMergeDirections[r-2] == 'D'))
				areaRowMergeDirections[r] = 'D';
			
			//	this one is ambiguous by all means, decide later
			else areaRowMergeDirections[r] = 'A';
		}
		
		//	finally
		return areaRowMergeDirections;
	}
	
	private static void mergeRowGroupLabels(BoundingBox tableBox, TableAreaColumn[] areaCols, TableAreaRow[] areaRows, int avgNonPeakRowMargin, TableAreaCell[][] areaCells) {
		for (int r = 0; r < areaRows.length; r += 2) {
			if (areaRows[r].disjointOccupiedCellCount > 1)
				continue; // not fully bridged
			
			//	check if we have a bold row, and cut some 10% slack in minimum gaps if we do (bold also pronounces the row, after all)
			boolean rowIsBold = areaRows[r].isBold();
			
			//	check upward and downward row gaps, as well as graphics blocking them (blocked gap is always large enough)
			if (((areaRows[r].aboveRowGap * 10) < (avgNonPeakRowMargin * (rowIsBold ? 9 : 10))) && (r != 0) && !areaRows[r-1].isEnforced)
				continue; // too close to be distinguished row group label
			if (((areaRows[r].belowRowGap * 10) < (avgNonPeakRowMargin * (rowIsBold ? 9 : 10))) && ((r+1) != areaRows.length) && !areaRows[r+1].isEnforced)
				continue; // too close to be distinguished row group label
			
			//	check for cells above and below bridging in (row group labels will not include any multi-row cells)
			boolean rowBridgedTo = false;
			for (int c = 0; c < areaCols.length; c += 2) {
				if ((r != 0) && areaCells[c][r-1].isBridged) {
					rowBridgedTo = true;
					break;
				}
				if (((r+1) != areaRows.length) && areaCells[c][r+1].isBridged) {
					rowBridgedTo = true;
					break;
				}
			}
			if (rowBridgedTo)
				continue;
			
			//	measure left and right distances from respective table edges, as well as left distance from left edge of second column
			int leftDist = -1;
			int leftDistAfterRowLabelCol = -1;
			int rightDist = -1;
			for (int c = 0; c < areaCols.length; c += 2)
				if (areaCells[c][r].wordBounds != null) {
					if (leftDist == -1)
						leftDist = (areaCells[c][r].wordBounds.left - tableBox.left);
					rightDist = (tableBox.right - areaCells[c][r].wordBounds.right);
					if (leftDistAfterRowLabelCol == -1) {
						if (2 <= c)
							leftDistAfterRowLabelCol = (areaCells[c][r].wordBounds.left - areaCols[2].min);
						else leftDistAfterRowLabelCol = Short.MAX_VALUE; // leaving enough room for multiplication without overflow
					}
				}
			
			//	test for left alignment: Table 2 on Page 15 in Zootaxa/zootaxa.4375.3.5.pdf.imf
			//	test for center alignment: Table 1 on Page 11 of EJT/ejt-402 (D03EFF9FFFC5D43CFFDCFFD0FE0FFFBE) (EJT-testbed/174)
			
			//	left distance has to be very small (< 2% of table width) for left alignment
			boolean isLeftAlignedOnTableEdge = ((areaRows[r].minOccupiedCellCol == 0) && ((leftDist * 50) < tableBox.getWidth()));
			
			//	left distance after row label column has to be very small (< 2% of table width) for left alignment
			boolean isLeftAlignedAfterRowLabelCol = ((areaRows[r].minOccupiedCellCol == 2) && ((leftDistAfterRowLabelCol * 50) < tableBox.getWidth()));
			
			//	left and right distances have to be within 2% of table width from one another for center alignment
			boolean isCenterAlignedOnTableEdge = ((Math.abs(leftDist - rightDist) * 50) < tableBox.getWidth());
			
			//	left and right distances (left after row label column) have to be within 2% of table width from one another for center alignment
			boolean isCenterAlignedAfterRowLabelCol = ((Math.abs(leftDistAfterRowLabelCol - rightDist) * 50) < tableBox.getWidth());
			
			//	aggregate alignments
			boolean isLeftAligned = (isLeftAlignedOnTableEdge || isLeftAlignedAfterRowLabelCol);
			boolean isCenterAligned = (isCenterAlignedOnTableEdge || isCenterAlignedAfterRowLabelCol);
			
			//	neither left aligned nor centered, not a row group label
			if (!isLeftAligned && !isCenterAligned)
				continue;
			
			//	row has ample space up and down, and only a single (streak of) populated cell(s) in the middle ==> bridge row label
			for (int c = 1; c < areaCols.length; c += 2)
				areaCells[c][r].isBridged = true;
		}
	}
	
	private static void handleCellOccupationSawBladePatterns(int areaColCount, TableAreaRow[] areaRows, TableAreaCell[][] areaCells, char[] areaRowMergeDirections) {
		System.out.println("Checking for cell occupation saw blade patterns ...");
		
		//	TODO check against other usual test tables
		
		//	TODO TEST: zootaxa.4420.2.5.pdf (goldengate-imagine/issues/466) Pages 7 and 10+11
		
		//	count full occupation rows and rows occupied in row label column, as well as graphics enforced row gaps
		int fullCellOccupationCount = 0;
		int firstCellOccupationCount = 0;
		int graphicsRowGapCount = 0;
		for (int r = 0; r < areaRows.length; r+= 2) {
			if (areaColCount <= areaRows[r].occupiedCellCount)
				fullCellOccupationCount++;
			if ((areaCells[0][r].wordBounds != null))
				firstCellOccupationCount++;
			if ((r != 0) && areaRows[r-1].isEnforced)
				graphicsRowGapCount++;
		}
		System.out.println("Full cell occupation count is " + fullCellOccupationCount);
		System.out.println("First cell occupation count is " + firstCellOccupationCount);
		System.out.println("Graphics enforced row gap count is " + graphicsRowGapCount);
		
		//	check for downward-reducing saw blade pattern
		boolean isDownwardSawBladeRowTable = true;
		int downwardCellOccupationReductionCount = 0;
		int downwardCellOccupationReductionSum = 0;
		int downwardCellOccupationReductionEndCount = 0;
		int downwardCellOccupationReductionEndSpaceSum = 0;
		int downwardCellOccupationReductionEndSpaceDiffSum = 0;
		int downwardCellOccupationReductionEndGraphicsCount = 0;
		System.out.println("Checking for downward saw blade pattern in row occupation");
		for (int r = 1; r < areaRows.length; r+= 2) {
			
			//	at most as many cells occupied as in row above, we're in a descent
			if (areaRows[r+1].occupiedCellCount <= areaRows[r-1].occupiedCellCount) {
				if (areaRows[r+1].occupiedCellCount < areaRows[r-1].occupiedCellCount) {
					downwardCellOccupationReductionCount++;
					downwardCellOccupationReductionSum += (areaRows[r-1].occupiedCellCount - areaRows[r+1].occupiedCellCount);
				}
				continue;
			}
			
			//	count row spacing and row spacing difference at rise in cell occupation
			else {
				downwardCellOccupationReductionEndCount++;
				downwardCellOccupationReductionEndSpaceSum += areaRows[r+1].aboveRowGap;
				int prevRowGapDiff = ((0 < (r-2)) ? (areaRows[r-1].belowRowGap - areaRows[r-1].aboveRowGap) : 0);
				int nextRowGapDiff = (((r+2) < areaRows.length) ? (areaRows[r+1].aboveRowGap - areaRows[r+1].belowRowGap) : 0);
				downwardCellOccupationReductionEndSpaceDiffSum += Math.max(prevRowGapDiff, nextRowGapDiff);
				if (areaRows[r].isEnforced)
					downwardCellOccupationReductionEndGraphicsCount++;
			}
			
			//	increase in cell occupation without returning to full column count ==> not a saw blade
			if (areaRows[r+1].occupiedCellCount < areaColCount) {
				isDownwardSawBladeRowTable = false;
				System.out.println(" ==> rise row " + ((r+1)/2) + " has fewer cell occupied than there are columns");
				break; 
			}
			
			//	smaller gap above return to full cell occupation than below it, even accounting for rounding error ==> not a saw blade
			if (((r+2) < areaRows.length) && (areaRows[r+1].aboveRowGap + 1) < areaRows[r+1].belowRowGap) {
				isDownwardSawBladeRowTable = false;
				System.out.println(" ==> rise row " + ((r+1)/2) + " has less space above than below it");
				break; 
			}
			
			//	smaller gap on far side of return to full cell occupation than right at it, even accounting for rounding error ==> not a saw blade
			if ((0 < (r-2)) && ((areaRows[r-1].belowRowGap + 1) < areaRows[r-1].aboveRowGap)) {
				isDownwardSawBladeRowTable = false;
				System.out.println(" ==> before-rise row " + ((r-1)/2) + " has less space below than above it");
				break;
			}
		}
		
		//	make DAMN sure this does NOT kick in in fully occupied tables with NO or FEW multi-line cells (they are the normal ones, after all ...)
		if ((fullCellOccupationCount * 4) < (downwardCellOccupationReductionEndCount * 5)) {} // we have at least 80% of fully occupied rows at actual tooth-tips
		else if (downwardCellOccupationReductionEndCount < 3) {
			isDownwardSawBladeRowTable = false;
			System.out.println(" ==> too few rises");
		}
		else if (downwardCellOccupationReductionEndCount == downwardCellOccupationReductionEndGraphicsCount) {} // all tooth-tips marked with graphics
		else {
			isDownwardSawBladeRowTable = false;
			System.out.println(" ==> too few fully occupied cells are actual rises, and too few rises are graphics enforced");
		}
		
		//	if we have any graphics enforced gaps (with one tolerance for header/body split), they have to correspond to tooth-tips
		if ((downwardCellOccupationReductionEndGraphicsCount + 1) < graphicsRowGapCount) {
			isDownwardSawBladeRowTable = false;
			System.out.println(" ==> only " + downwardCellOccupationReductionEndGraphicsCount + " of " + graphicsRowGapCount + " graphics row gaps correspond with tooth-tips");
		}
		
		//	what do we have?
		System.out.println("Downward saw blade is " + isDownwardSawBladeRowTable);
		System.out.println("Downward saw blade cell occupation drop count is " + downwardCellOccupationReductionCount);
		System.out.println("Downward saw blade cell occupation drop sum is " + downwardCellOccupationReductionSum);
		System.out.println("Downward saw blade tooth-tip count is " + downwardCellOccupationReductionEndCount);
		System.out.println("Downward saw blade pre-tooth-tip gap sum is " + downwardCellOccupationReductionEndSpaceSum);
		System.out.println("Downward saw blade pre-tooth-tip gap difference sum is " + downwardCellOccupationReductionEndSpaceDiffSum);
		System.out.println("Downward saw blade pre-tooth-tip graphics count is " + downwardCellOccupationReductionEndGraphicsCount);
		
		//	check of upward-reducing saw blade pattern
		boolean isUpwardSawBladeRowTable = true;
		int upwardCellOccupationReductionCount = 0;
		int upwardCellOccupationReductionSum = 0;
		int upwardCellOccupationReductionEndCount = 0;
		int upwardCellOccupationReductionEndSpaceSum = 0;
		int upwardCellOccupationReductionEndSpaceDiffSum = 0;
		int upwardCellOccupationReductionEndGraphicsCount = 0;
		System.out.println("Checking for upward saw blade pattern in row occupation");
		for (int r = 1; r < areaRows.length; r+= 2) {
			
			//	at least as many cells occupied as in row above, we're in an ascent
			if (areaRows[r-1].occupiedCellCount <= areaRows[r+1].occupiedCellCount) {
				if (areaRows[r-1].occupiedCellCount < areaRows[r+1].occupiedCellCount) {
					upwardCellOccupationReductionCount++;
					upwardCellOccupationReductionSum += (areaRows[r+1].occupiedCellCount - areaRows[r-1].occupiedCellCount);
				}
				continue;
			}
			
			//	count row spacing and row spacing difference at drop in cell occupation
			else {
				upwardCellOccupationReductionEndCount++;
				upwardCellOccupationReductionEndSpaceSum += areaRows[r].aboveRowGap;
				int prevRowGapDiff = ((0 < (r-2)) ? (areaRows[r-1].belowRowGap - areaRows[r-1].aboveRowGap) : 0);
				int nextRowGapDiff = (((r+2) < areaRows.length) ? (areaRows[r+1].aboveRowGap - areaRows[r+1].belowRowGap) : 0);
				upwardCellOccupationReductionEndSpaceDiffSum += Math.max(prevRowGapDiff, nextRowGapDiff);
				if (areaRows[r-1].isEnforced)
					upwardCellOccupationReductionEndGraphicsCount++;
			}
			
			//	decrease in cell occupation without coming from full column count ==> not a saw blade
			if (areaRows[r-1].occupiedCellCount < areaColCount) {
				isUpwardSawBladeRowTable = false;
				System.out.println(" ==> before-drop row " + ((r-1)/2) + " has fewer cell occupied than there are columns");
				break;
			}
			
			//	smaller gap above drop from full cell occupation than below it, even accounting for rounding error ==> not a saw blade
			if (((r+2) < areaRows.length) && ((areaRows[r+1].aboveRowGap + 1) < areaRows[r+1].belowRowGap)) {
				isUpwardSawBladeRowTable = false;
				System.out.println(" ==> drop row " + ((r+1)/2) + " has less space above than below it");
				break;
			}
			
			//	smaller gap on far side of drop from full cell occupation than right at it, even accounting for rounding error ==> not a saw blade
			if ((0 < (r-2)) && (areaRows[r-1].belowRowGap + 1) < areaRows[r-1].aboveRowGap) {
				isUpwardSawBladeRowTable = false;
				System.out.println(" ==> before-drop row " + ((r-1)/2) + " has less space below than above it");
				break;
			}
		}
		
		//	make DAMN sure this does NOT kick in in fully occupied tables with NO or FEW multi-line cells (they are the normal ones, after all ...)
		if ((fullCellOccupationCount * 4) < (upwardCellOccupationReductionEndCount * 5)) {} // we have at least 80% of fully occupied rows at actual tooth-tips
		else if (upwardCellOccupationReductionEndCount < 3) {
			isUpwardSawBladeRowTable = false;
			System.out.println(" ==> too few rises");
		}
		else if (upwardCellOccupationReductionEndCount == upwardCellOccupationReductionEndGraphicsCount) {} // all tooth-tips marked with graphics
		else {
			isUpwardSawBladeRowTable = false;
			System.out.println(" ==> too few fully occupied cells are actual rises, and too few rises are graphics enforced");
		}
		
		//	if we have any graphics enforced gaps (with one tolerance for header/body split), they have to correspond to tooth-tips
		if ((upwardCellOccupationReductionEndGraphicsCount + 1) < graphicsRowGapCount) {
			isUpwardSawBladeRowTable = false;
			System.out.println(" ==> only " + upwardCellOccupationReductionEndGraphicsCount + " of " + graphicsRowGapCount + " graphics row gaps correspond with tooth-tips");
		}
		
		//	one tooth-tip doesn't make for a saw blade ...
		if ((upwardCellOccupationReductionEndSpaceSum + 1) < graphicsRowGapCount) {
			isUpwardSawBladeRowTable = false;
			System.out.println(" ==> only " + upwardCellOccupationReductionEndGraphicsCount + " of " + graphicsRowGapCount + " graphics row gaps correspond with tooth-tips");
		}
		
		//	what do we have?
		//	TODOne TEST avoid false positive in multi-page table at end of TableTest/zt03652p155.pdf.clean.imf
		System.out.println("Upward saw blade is " + isUpwardSawBladeRowTable);
		System.out.println("Upward saw blade cell occupation drop count is " + upwardCellOccupationReductionCount);
		System.out.println("Upward saw blade cell occupation drop sum is " + upwardCellOccupationReductionSum);
		System.out.println("Upward saw blade tooth-tip count is " + upwardCellOccupationReductionEndCount);
		System.out.println("Upward saw blade pre-tooth-tip gap sum is " + upwardCellOccupationReductionEndSpaceSum);
		System.out.println("Upward saw blade pre-tooth-tip gap difference sum is " + upwardCellOccupationReductionEndSpaceDiffSum);
		System.out.println("Upward saw blade pre-tooth-tip graphics count is " + upwardCellOccupationReductionEndGraphicsCount);
		
		//	anything to actually do?
		if (!isDownwardSawBladeRowTable && !isUpwardSawBladeRowTable)
			return;
		
		//	we have a saw blade both ways, check relative row distances
		if (isDownwardSawBladeRowTable && isUpwardSawBladeRowTable) {
			System.out.println("Breaking tie of saw blade patterns in row occupation");
			if (Math.max(upwardCellOccupationReductionEndSpaceDiffSum, 0) < downwardCellOccupationReductionEndSpaceDiffSum) {
				isUpwardSawBladeRowTable = false;
				System.out.println(" ==> downward saw blade has higher row gap gain of " + downwardCellOccupationReductionEndSpaceDiffSum + " vs. " + upwardCellOccupationReductionEndSpaceDiffSum);
			}
			else if (Math.max(downwardCellOccupationReductionEndSpaceDiffSum, 0) < upwardCellOccupationReductionEndSpaceDiffSum) {
				isDownwardSawBladeRowTable = false;
				System.out.println(" ==> upward saw blade has higher row gap gain of " + upwardCellOccupationReductionEndSpaceDiffSum + " vs. " + downwardCellOccupationReductionEndSpaceDiffSum);
			}
			else if (Math.max(upwardCellOccupationReductionEndSpaceSum, 0) < downwardCellOccupationReductionEndSpaceSum) {
				isUpwardSawBladeRowTable = false;
				System.out.println(" ==> downward saw blade has larger overall row gap of " + downwardCellOccupationReductionEndSpaceSum + " vs. " + upwardCellOccupationReductionEndSpaceSum);
			}
			else if (Math.max(downwardCellOccupationReductionEndSpaceSum, 0) < upwardCellOccupationReductionEndSpaceSum) {
				isDownwardSawBladeRowTable = false;
				System.out.println(" ==> upward saw blade has larger overall row gap of " + upwardCellOccupationReductionEndSpaceSum + " vs. " + downwardCellOccupationReductionEndSpaceSum);
			}
			else {
				isUpwardSawBladeRowTable = false;
				System.out.println(" ==> downward saw blade is more likely than upward saw blade");
			}
		}
		
		//	merge upward in downward saw blade pattern
		if (isDownwardSawBladeRowTable) {
			System.out.println("Applying downward saw blade pattern");
			for (int r = 1; r < areaRows.length; r += 2) {
				if (!areaRows[r].isEnforced && (areaRows[r+1].occupiedCellCount <= areaRows[r-1].occupiedCellCount))
					areaRowMergeDirections[r+1] = 'U';
			}
		}
		
		//	merge downward in upward saw blade pattern
		else if (isUpwardSawBladeRowTable) {
			System.out.println("Applying upward saw blade pattern");
			for (int r = 1; r < areaRows.length; r += 2) {
				if (!areaRows[r].isEnforced && (areaRows[r-1].occupiedCellCount <= areaRows[r+1].occupiedCellCount))
					areaRowMergeDirections[r-1] = 'D';
			}
		}
	}
	
	private static char[] getAreaColumnMergers(TableAreaColumn[] areaCols, TableAreaRow[] areaRows, TableAreaCell[][] areaCells, int avgWordHeight) {
		char[] areaColMergeDirections = new char[areaCols.length];
		Arrays.fill(areaColMergeDirections, 'N'); // initialize to 'N' for 'none'
		
		//	assess individual columns
		for (int c = 0; c < areaCols.length; c += 2) {
			
			//	merge left ignoring all other criteria if every populated cell is bridged to from left
			if ((c != 0) && !areaCols[c - 1].isEnforced && isColumnFullyBridgedTo(areaCells, c, (c - 2))) {
				areaColMergeDirections[c] = 'L';
				continue;
			}
			
			//	merge right ignoring all other criteria if every populated cell is bridged to from right
			if (((c+2) < areaCols.length) && !areaCols[c + 1].isEnforced && isColumnFullyBridgedTo(areaCells, c, (c + 2))) {
				areaColMergeDirections[c] = 'R';
				continue;
			}
			
			//	merge left ignoring all other criteria if every cell populated on either side is either bridged to or has an empty counterpart
			if ((c != 0) && !areaCols[c - 1].isEnforced && isColumnGapFullyBridged(areaCells, (c - 2), c)) {
				areaColMergeDirections[c] = 'L';
				continue;
			}
			
			//	merge right ignoring all other criteria if every cell populated on either side is either bridged to or has an empty counterpart
			if (((c+2) < areaCols.length) && !areaCols[c + 1].isEnforced && isColumnGapFullyBridged(areaCells, c, (c + 2))) {
				areaColMergeDirections[c] = 'R';
				continue;
			}
			
			//	merge to both sides if (a) column below completely empty and (b) _both_ adjacent cells empty
			if ((areaCols[c].occupiedCellCount == 1) && (areaCols[c].minOccupiedCellRow <= 2)
					&& (c != 0) && !areaCols[c - 1].isEnforced && (areaCells[c - 2][areaCols[c].minOccupiedCellRow].wordBounds == null)
					&& ((c + 2) < areaCols.length) && !areaCols[c + 1].isEnforced && (areaCells[c + 2][areaCols[c].minOccupiedCellRow].wordBounds == null)) {
				areaColMergeDirections[c] = 'X';
				continue;
			}
			
			//	do we have a sparse column TODO test thresholds, as well as pivot 10
			if (((areaRows.length < 10) ? 1 : 2) < areaCols[c].occupiedCellCount)
				continue;
			
			//	test for leftward merger (gap has to be not enforced and less than average word height, as the latter is the absolute limit for the width of a regular space)
			boolean mergeableLeftwrad = ((c != 0) && !areaCols[c - 1].isEnforced && (areaCols[c].leftColGap < avgWordHeight));
			
			//	test for rightward merger (gap has to be not enforced and less than average word height, as the latter is the absolute limit for the width of a regular space)
			boolean mergeableRightward = (((c+2) < areaCols.length) && !areaCols[c + 1].isEnforced && (areaCols[c].rightColGap < avgWordHeight));
			
			//	no mergers for this column
			if (!mergeableLeftwrad && !mergeableRightward)
				continue;
			
			//	we can only merge leftward if (a) column on left is less sparse or (b) will merge leftward itself
			mergeableLeftwrad = (mergeableLeftwrad && (
					(areaCols[c].occupiedCellCount < areaCols[c-2].occupiedCellCount)
					||
					((areaCols[c].occupiedCellCount == areaCols[c-2].occupiedCellCount) && (areaColMergeDirections[c - 2] == 'L'))
				));
			
			//	we can only merge rightward if (a) column on right is less sparse
			mergeableRightward = (mergeableRightward &&
					(areaCols[c].occupiedCellCount <= areaCols[c+2].occupiedCellCount)
				);
			
			//	no mergers for this column after considering sparseness
			if (!mergeableLeftwrad && !mergeableRightward)
				continue;
			
			//	check cell compatibility
			mergeableLeftwrad = (mergeableLeftwrad && areColumnCellsExtensionCompatibleWith(areaCells, c, (c - 2), avgWordHeight));
			mergeableRightward = (mergeableRightward && areColumnCellsExtensionCompatibleWith(areaCells, c, (c + 2), avgWordHeight));
			
			//	no mergers for this column after considering cells
			if (!mergeableLeftwrad && !mergeableRightward)
				continue;
			
			//	this one is unambiguous
			if (mergeableLeftwrad != mergeableRightward) {
				if (mergeableLeftwrad)
					areaColMergeDirections[c] = 'L';
				else if (mergeableRightward)
					areaColMergeDirections[c] = 'R';
				continue;
			}
//			
//			//	we have a clear spacial tendency (even with tolerance of 1 accounting for rounding errors)
//			if (1 < Math.abs(areaCols[c].leftColGap - areaCols[c].rightColGap)) {
//				areaColMergeDirections[c] = ((areaCols[c].leftColGap < areaCols[c].rightColGap) ? 'L' : 'R');
//				continue;
//			}
			
			//	measure distance between individual cells
			int maxCellWordGapLeft = getMaxColumnCellWordGap(areaCells, c, (c - 2));
			int maxCellWordGapRight = getMaxColumnCellWordGap(areaCells, c, (c + 2));
			
			//	we have a clear spacial tendency (even with tolerance of 1 accounting for rounding errors)
			if (1 < Math.abs(maxCellWordGapLeft - maxCellWordGapRight)) {
				areaColMergeDirections[c] = ((maxCellWordGapLeft < maxCellWordGapRight) ? 'L' : 'R');
				continue;
			}
			
			//	prefer merging left into left-merging column (longish leftward extensions that require merging rightward are rare)
			if ((c != 0) && areaColMergeDirections[c-2] == 'L') {
				areaColMergeDirections[c] = 'L';
				continue;
			}
			
			//	prefer merging left (longish leftward extensions that require merging rightward are rare)
			areaColMergeDirections[c] = 'L';
		}
		
		//	finally ...
		return areaColMergeDirections;
	}
	
	private static void assessAreaColumnProperties(TableAreaColumn[] areaCols, TableAreaRow[] areaRows, TableAreaCell[][] areaCells) {
		for (int c = 0; c < areaCols.length; c += 2) {
			
			//	check upward and downward row gaps
			areaCols[c].leftColGap = ((c == 0) ? Integer.MAX_VALUE : areaCols[c-1].getSpan());
			areaCols[c].rightColGap = (((c+1) == areaCols.length) ? Integer.MAX_VALUE : areaCols[c+1].getSpan());
			
			//	count populated cells per row, as well as their column positions, and in-bridging 
			for (int r = 0; r < areaRows.length; r += 2) {
				
				//	assess font properties
				for (int w = 0; w < areaCells[c][r].words.length; w++) {
					areaCols[c].wordCount++;
					if (areaCells[c][r].words[w].hasAttribute(ImWord.BOLD_ATTRIBUTE))
						areaCols[c].boldWordCount++;
					if (areaCells[c][r].words[w].hasAttribute(ImWord.ITALICS_ATTRIBUTE))
						areaCols[c].italicsWordCount++;
					try {
						int fontSize = areaCells[c][r].words[w].getFontSize();
						areaCols[c].wordFontSizeCount++;
						areaCols[c].wordFontSizeSum += fontSize;
						areaCols[c].wordFontSizeMin = Math.min(areaCols[c].wordFontSizeMin, fontSize);
						areaCols[c].wordFontSizeMax = Math.max(areaCols[c].wordFontSizeMax, fontSize);
					} catch (Exception e) {}
				}
//				
//				//	check for incoming bridging spacer cells
//				if ((c != 0) && areaCells[c-1][r].isBridged)
//					areaCols[c].bridgedLeftCount++;
//				if (((c+1) != areaCols.length) && areaCells[c+1][r].isBridged)
//					areaCols[c].bridgedRightCount++;
				
				//	check cell occupation
				if (areaCells[c][r].wordBounds != null) { /* cell has content */ }
				else if ((c != 0) && areaCells[c-1][r].isBridged) { /* bridged to from left */ }
				else if (((c+1) != areaCols.length) && areaCells[c+1][r].isBridged) { /* bridged to from right */ }
				else if ((r != 0) && areaCells[c][r-1].isBridged) { /* bridged to from above */ }
				else if (((r+1) != areaRows.length) && areaCells[c][r+1].isBridged) { /* bridged to from below */ }
				else continue; // empty cell with no bridging neighbors at all, no use counting
				
				//	count occupied (or bridged to) cell
				areaCols[c].occupiedCellCount++;
				areaCols[c].minOccupiedCellRow = Math.min(areaCols[c].minOccupiedCellRow, r);
				areaCols[c].maxOccupiedCellRow = Math.max(areaCols[c].maxOccupiedCellRow, r);
				if ((r == 0) || !areaCells[c][r-1].isBridged)
					areaCols[c].disjointOccupiedCellCount++; // not bridged to from above, count (starting streak of) disjoint populated cell(s)
			}
		}
	}
	
	private static class TableAreaRow extends TableAreaSlice {
		int aboveRowGap = -1;
		int belowRowGap = -1;
		
		int occupiedCellCount = 0;
		int disjointOccupiedCellCount = 0;
		int minOccupiedCellCol = Integer.MAX_VALUE;
		int maxOccupiedCellCol = Integer.MIN_VALUE;
		
		TableAreaRow(boolean isSpace, int min, int max) {
			super(isSpace, min, max);
		}
		TableAreaRow(boolean isSpace, int min, int max, boolean isEnforced) {
			super(isSpace, min, max, isEnforced);
		}
	}
	
	private static void assessAreaRowProperties(TableAreaColumn[] areaCols, TableAreaRow[] areaRows, TableAreaCell[][] areaCells) {
		for (int r = 0; r < areaRows.length; r += 2) {
			
			//	check upward and downward row gaps
			areaRows[r].aboveRowGap = ((r == 0) ? Integer.MAX_VALUE : areaRows[r-1].getSpan());
			areaRows[r].belowRowGap = (((r+1) == areaRows.length) ? Integer.MAX_VALUE : areaRows[r+1].getSpan());
			
			//	count populated cells per row, as well as their column positions, and in-bridging 
			for (int c = 0; c < areaCols.length; c += 2) {
				
				//	assess font properties
				for (int w = 0; w < areaCells[c][r].words.length; w++) {
					areaRows[r].wordCount++;
					if (areaCells[c][r].words[w].hasAttribute(ImWord.BOLD_ATTRIBUTE))
						areaRows[r].boldWordCount++;
					if (areaCells[c][r].words[w].hasAttribute(ImWord.ITALICS_ATTRIBUTE))
						areaRows[r].italicsWordCount++;
					try {
						int fontSize = areaCells[c][r].words[w].getFontSize();
						areaRows[r].wordFontSizeCount++;
						areaRows[r].wordFontSizeSum += fontSize;
						areaRows[r].wordFontSizeMin = Math.min(areaRows[r].wordFontSizeMin, fontSize);
						areaRows[r].wordFontSizeMax = Math.max(areaRows[r].wordFontSizeMax, fontSize);
					} catch (Exception e) {}
				}
//				
//				//	check for incoming bridging spacer cells
//				if ((r != 0) && areaCells[c][r-1].isBridged)
//					areaRows[r].bridgedAboveCount++;
//				if (((r+1) != areaRows.length) && areaCells[c][r+1].isBridged)
//					areaRows[r].bridgedBelowCount++;
				
				//	check cell occupation
				if (areaCells[c][r].wordBounds != null) { /* cell has content */ }
				else if ((c != 0) && areaCells[c-1][r].isBridged) { /* bridged to from left */ }
				else if (((c+1) != areaCols.length) && areaCells[c+1][r].isBridged) { /* bridged to from right */ }
				else if ((r != 0) && areaCells[c][r-1].isBridged) { /* bridged to from above */ }
				else if (((r+1) != areaRows.length) && areaCells[c][r+1].isBridged) { /* bridged to from below */ }
				else continue; // empty cell with no bridging neighbors at all, no use counting
				
				//	count occupied (or bridged to) cell
				areaRows[r].occupiedCellCount++;
				areaRows[r].minOccupiedCellCol = Math.min(areaRows[r].minOccupiedCellCol, c);
				areaRows[r].maxOccupiedCellCol = Math.max(areaRows[r].maxOccupiedCellCol, c);
				if ((c == 0) || !areaCells[c-1][r].isBridged)
					areaRows[r].disjointOccupiedCellCount++; // not bridged to from left, count (starting streak of) disjoint populated cell(s)
			}
		}
	}
	
	//	factor in vertical distance between words in individual cells populated in both rows ...
	//	... and refuse merger if _average_ distance larger than normal (to prevent erroneous row mergers due to a single line wrapped cell)
	//	TEST: Table 2 (Pages 4-6) in Zootaxa/zootaxa.4372.2.6.pdf.imf
	private static boolean isRowGapMergeable(int rg, TableAreaRow[] areaRows, TableAreaCell[][] areaCells, int minNonMergeRowMargin) {
		
		//	no need to investigate this one any further
		if (areaRows[rg].isEnforced)
			return false;
		
		//	more than 90% of non-merge threshold
		if ((minNonMergeRowMargin * 9) <= (areaRows[rg].getSpan() * 10))
			return false;
		
		//	within rounding error of non-merge threshold
		if (minNonMergeRowMargin <= (areaRows[rg].getSpan() + 1))
			return false;
		
		//	compute cell-by-cell average margin and cell-by-cell weighted mergin
		int cellWordDistCount = 0;
		int cellWordDistSum = 0;
		int cellWordDistWidthSum = 0;
		int cellWordDistAreaSum = 0;
		for (int c = 0; c < areaCells.length; c += 2) {
			if (areaCells[c][rg - 1].wordBounds == null)
				continue;
			if (areaCells[c][rg + 1].wordBounds == null)
				continue;
			int cellWordDist = (areaCells[c][rg + 1].wordBounds.top - areaCells[c][rg - 1].wordBounds.bottom);
			cellWordDistCount++;
			cellWordDistSum += cellWordDist;
			int cellWordDistWidth = Math.max(areaCells[c][rg - 1].wordBounds.getWidth(), areaCells[c][rg + 1].wordBounds.getWidth());
			cellWordDistWidthSum += cellWordDistWidth;
			cellWordDistAreaSum += (cellWordDist * cellWordDistWidth);
		}
		int avgCellWordDist = ((cellWordDistCount == 0) ? 0 : ((cellWordDistSum + (cellWordDistCount / 2)) / cellWordDistCount));
		int avgCellWordDistByWidth = ((cellWordDistWidthSum == 0) ? 0 : ((cellWordDistAreaSum + (cellWordDistWidthSum / 2)) / cellWordDistWidthSum));
		
		//	cell-by-cell average distance more than 90% of non-merge threshold
		if ((minNonMergeRowMargin * 9) <= (avgCellWordDist * 10))
			return false;
		
		//	cell-by-cell average distance within rounding error of non-merge threshold
		if (minNonMergeRowMargin <= (avgCellWordDist + 1))
			return false;
		
		//	weighted cell-by-cell average distance more than 90% of non-merge threshold
		if ((minNonMergeRowMargin * 9) <= (avgCellWordDistByWidth * 10))
			return false;
		
		//	weighted cell-by-cell average distance within rounding error of non-merge threshold
		if (minNonMergeRowMargin <= (avgCellWordDistByWidth + 1))
			return false;
		
		//	this gap looks good for merging over
		return true;
	}
	
	private static class TableAreaCell {
		final boolean isSpace;
		final TableAreaColumn col;
		final TableAreaRow row;
		
		ImWord[] words;
		BoundingBox wordBounds;
		String mainTextDirection;
		
		boolean isBridged = false;
		
		TableAreaCell(TableAreaColumn col, TableAreaRow row) {
			this.col = col;
			this.row = row;
			this.isSpace = (this.col.isSpace || this.row.isSpace);
		}
		
		void updateWords(ImWord[] tableWords) {
			ArrayList words = new ArrayList();
			int lrWords = 0;
			int buWords = 0;
			int tdWords = 0;
			for (int w = 0; w < tableWords.length; w++) {
				if (tableWords[w].bounds.right <= this.col.min)
					continue;
				if (this.col.max <= tableWords[w].bounds.left)
					continue;
				if (tableWords[w].bounds.bottom <= this.row.min)
					continue;
				if (this.row.max <= tableWords[w].bounds.top)
					continue;
				
				words.add(tableWords[w]);
				
				String wordDirection = ((String) (tableWords[w].getAttribute(ImWord.TEXT_DIRECTION_ATTRIBUTE, ImWord.TEXT_DIRECTION_LEFT_RIGHT)));
				if (ImWord.TEXT_DIRECTION_BOTTOM_UP.equals(wordDirection))
					buWords++;
				else if (ImWord.TEXT_DIRECTION_TOP_DOWN.equals(wordDirection))
					tdWords++;
				else lrWords++;
			}
			this.words = ((ImWord[]) words.toArray(new ImWord[words.size()]));
			this.wordBounds = ((this.words.length == 0) ? null : ImLayoutObject.getAggregateBox(this.words));
			if (this.wordBounds == null)
				this.mainTextDirection = ImWord.TEXT_DIRECTION_LEFT_RIGHT;
			else if ((lrWords < buWords) && (tdWords < buWords))
				this.mainTextDirection = ImWord.TEXT_DIRECTION_BOTTOM_UP;
			else if ((lrWords < tdWords) && (buWords < tdWords))
				this.mainTextDirection = ImWord.TEXT_DIRECTION_TOP_DOWN;
			else this.mainTextDirection = ImWord.TEXT_DIRECTION_LEFT_RIGHT;
			this.isBridged = (this.isBridged || (this.isSpace && (this.wordBounds != null)));
		}
	}
	
	private static boolean isColumnFullyBridgedTo(TableAreaCell[][] colCells, int extendColIndex, int mainColIndex) {
		for (int r = 0; r < colCells[mainColIndex].length; r += 2) {
			if (colCells[extendColIndex][r].wordBounds == null)
				continue; // no use requiring bridge to empty cell
			if (colCells[mainColIndex][r].wordBounds == null)
				return false; // empty cells don't bridge
			if (!colCells[(extendColIndex + mainColIndex) / 2][r].isBridged)
				return false; // not bridged
		}
		return true; // no counter indications found here
	}
	
	private static boolean isColumnGapFullyBridged(TableAreaCell[][] colCells, int leftColIndex, int rightColIndex) {
		boolean gotBridgedCell = false;
		for (int r = 0; r < colCells[leftColIndex].length; r += 2) {
			if (colCells[leftColIndex][r].wordBounds == null)
				continue; // no use requiring bridge to empty cell
			if (colCells[rightColIndex][r].wordBounds == null)
				continue; // no use requiring bridge to empty cell
			if (colCells[(leftColIndex + rightColIndex) / 2][r].isBridged)
				gotBridgedCell = true;
			else return false; // not bridged
		}
		return gotBridgedCell; // no counter indications found here
	}
	
	private static boolean areColumnCellsExtensionCompatibleWith(TableAreaCell[][] colCells, int extendColIndex, int mainColIndex, int avgWordHeight) {
		for (int r = 0; r < colCells[mainColIndex].length; r += 2) {
			if ((r != 0) && colCells[extendColIndex][r-1].isBridged && !colCells[mainColIndex][r-1].isBridged)
				return false; // extension columns don't bridge row gaps open in columns they extend
			if (colCells[extendColIndex][r].wordBounds == null)
				continue; // no use investigating empty cell
			if (colCells[mainColIndex][r].wordBounds == null)
				return false; // empty cells don't extend
			if (!colCells[extendColIndex][r].mainTextDirection.equals(colCells[mainColIndex][r].mainTextDirection))
				return false; // predominant text orientation has to match
			int cellWordGap = ((extendColIndex < mainColIndex) ? (colCells[mainColIndex][r].wordBounds.left - colCells[extendColIndex][r].wordBounds.right) : (colCells[extendColIndex][r].wordBounds.left - colCells[mainColIndex][r].wordBounds.right));
			if (avgWordHeight <= cellWordGap)
				return false; // extending cells have to be close to actual cells
		}
		return true; // no counter indications found here
	}
	
	private static int getMaxColumnCellWordGap(TableAreaCell[][] colCells, int extendColIndex, int mainColIndex) {
		int maxCellWordGap = -1;
		for (int r = 0; r < colCells[mainColIndex].length; r += 2) {
			if ((colCells[extendColIndex][r].wordBounds == null) || (colCells[mainColIndex][r].wordBounds == null))
				continue; // we can only measure gap between two populated cells
			int cellWordGap = ((extendColIndex < mainColIndex) ? (colCells[mainColIndex][r].wordBounds.left - colCells[extendColIndex][r].wordBounds.right) : (colCells[extendColIndex][r].wordBounds.left - colCells[mainColIndex][r].wordBounds.right));
			maxCellWordGap = Math.max(maxCellWordGap, cellWordGap);
		}
		return maxCellWordGap;
	}
	
	private static boolean isRowGapFullyBridged(TableAreaCell[][] rowCells, int overflowRowIndex, int dataRowIndex) {
		for (int c = 0; c < rowCells.length; c += 2) {
			if (rowCells[c][overflowRowIndex].wordBounds == null)
				continue; // no use requiring bridge to empty cell
			if (rowCells[c][dataRowIndex].wordBounds == null)
				return false; // empty cells don't bridge
			if (!rowCells[c][(overflowRowIndex + dataRowIndex) / 2].isBridged)
				return false; // not bridged
		}
		return true; // no counter indications found here
	}
	
	private static boolean areRowCellsOverflowCompatibleWith(TableAreaCell[][] rowCells, int overflowRowIndex, int dataRowIndex, boolean checkInCellWords) {
		for (int c = 0; c < rowCells.length; c += 2) {
			if ((c != 0) && rowCells[c-1][overflowRowIndex].isBridged && !rowCells[c-1][dataRowIndex].isBridged)
				return false; // overflow rows don't bridge column gaps open in data rows they extend
			if (rowCells[c][overflowRowIndex].wordBounds == null)
				continue; // no use investigating empty cell
			if (rowCells[c][dataRowIndex].wordBounds == null)
				return false; // empty cells don't cause any overflow
			if (!rowCells[c][overflowRowIndex].mainTextDirection.equals(rowCells[c][dataRowIndex].mainTextDirection))
				return false; // predominant text orientation has to match
			if (checkInCellWords) {
				int topWordWidth = rowCells[c][Math.min(dataRowIndex, overflowRowIndex)].wordBounds.getWidth();
				int bottomWordWidth = rowCells[c][Math.max(dataRowIndex, overflowRowIndex)].wordBounds.getWidth();
				if ((topWordWidth * 3) < (bottomWordWidth * 2))
					return false; // words in top cell far shorter than in bottom cell (we still read top-down ...)
			}
		}
		return true; // no counter indications found here
	}
	
	private static int getMaxRowCellWordGap(TableAreaCell[][] rowCells, int overflowRowIndex, int dataRowIndex) {
		int maxCellWordGap = -1;
		for (int c = 0; c < rowCells.length; c += 2) {
			if ((rowCells[c][overflowRowIndex].wordBounds == null) || (rowCells[c][dataRowIndex].wordBounds == null))
				continue; // we can only measure gap between two populated cells
			int cellWordGap = ((overflowRowIndex < dataRowIndex) ? (rowCells[c][dataRowIndex].wordBounds.top - rowCells[c][overflowRowIndex].wordBounds.bottom) : (rowCells[c][overflowRowIndex].wordBounds.top - rowCells[c][dataRowIndex].wordBounds.bottom));
			maxCellWordGap = Math.max(maxCellWordGap, cellWordGap);
		}
		return maxCellWordGap;
	}
	
	private static boolean isFullGrid(TreeSet colOccupationLows, TreeSet rowOccupationGaps) {
		
		//	check column gaps
		int blockedColCount = 0;
		for (Iterator colit = colOccupationLows.iterator(); colit.hasNext();) {
			ColumnOccupationLow col = ((ColumnOccupationLow) colit.next());
			if (col.isBlockedByGraphics())
				blockedColCount++;
		}
		
		//	require two thirds of column gaps to be blocked by graphics, and more than 1
		if (blockedColCount < 2)
			return false;
		if ((blockedColCount * 3) < (colOccupationLows.size() * 2))
			return false;
		
		//	check row gaps
		int blockedRogCount = 0;
		for (Iterator rogit = rowOccupationGaps.iterator(); rogit.hasNext();) {
			RowOccupationGap rog = ((RowOccupationGap) rogit.next());
			if (rog.isBlockedByGraphics())
				blockedRogCount++;
		}
		
		//	require two thirds of row gaps to be blocked by graphics, and more than 1
		if (blockedRogCount < 2)
			return false;
		if ((blockedRogCount * 3) < (rowOccupationGaps.size() * 2))
			return false;
		
		//	this one looks good
		return true;
	}
	
	//	TODO-not remove this method, does nothing but call some constructor
	private ImRegion markTableCell(ImPage page, BoundingBox bounds, boolean shrinkLeftRight, boolean shrinkTopBottom) {
//		//	TODOne DO NOT SHRINK !!!
//		//	TODOne TEST WITHOUT SHRINKING !!! ==> TODOne only need to shrink _columns_ before marking cells
//		if (shrinkLeftRight || shrinkTopBottom) {
//			ImWord[] tableCellWords = page.getWordsInside(bounds);
//			if (tableCellWords.length != 0) {
//				BoundingBox wordBounds = ImLayoutObject.getAggregateBox(tableCellWords);
//				bounds = new BoundingBox(
//					(shrinkLeftRight ? Math.max(bounds.left, wordBounds.left) : bounds.left),
//					(shrinkLeftRight ? Math.min(bounds.right, wordBounds.right) : bounds.right),
//					(shrinkTopBottom ? Math.max(bounds.top, wordBounds.top) : bounds.top),
//					(shrinkTopBottom ? Math.min(bounds.bottom, wordBounds.bottom) : bounds.bottom)
//				);
//			}
//		}
		return new ImRegion(page, bounds, ImRegion.TABLE_CELL_TYPE);
	}
	
	private ImRegion[][] markTableCells(ImPage page, ImRegion table) {
		
		//	get cells
		ImRegion[][] cells = ImUtils.getTableCells(table, null, null);
		
		//	make sure cells are attached to page
		for (int r = 0; r < cells.length; r++)
			for (int c = 0; c < cells[r].length; c++) {
				if (cells[r][c].getPage() == null)
					page.addRegion(cells[r][c]);
			}
		
		//	finally ...
		return cells;
	}
	
	private boolean mergeTables(ImPage page, ImRegion[] selTables, ImDocumentMarkupPanel idmp) {
		
		//	check if we have an in-line table
		int inLineTableCount = 0;
		for (int t = 0; t < selTables.length; t++) {
			if (selTables[t].hasAttribute(ImRegion.IN_LINE_OBJECT_MARKER_ATTRIBUTE))
				inLineTableCount++;
		}
		
		//	get aggregate bounds
		BoundingBox mTableBox = ImLayoutObject.getAggregateBox(selTables);
		
		//	get words
		ImWord[] mTableWords = page.getWordsInside(mTableBox);
		
		//	compute statistics for merged table area
		TableAreaStatistics mTableBoxStats = TableAreaStatistics.getTableAreaStatistics(page, mTableWords);
		
		//	compute merged table markup, considering existing gaps
		TableAreaMarkup mTableMarkup = this.getTableMarkup(page, mTableWords, selTables, mTableBox, mTableBoxStats, ((inLineTableCount * 2) > selTables.length));
		
		//	check if merger makes sense
		if (mTableMarkup.rows.length < 3) {
			DialogFactory.alert("The selected tables cannot be merged because the result does not split into rows.", "Cannot Merge Tables", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if (mTableMarkup.cols.length < 2) {
			DialogFactory.alert("The selected tables cannot be merged because the result does not split into columns.", "Cannot Merge Tables", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		//	finish markup
		return this.finishMarkTable(page, mTableBox, mTableWords, mTableMarkup, selTables, idmp);
	}
	
	private boolean extendTable(ImPage page, ImRegion selTable, ImWord[] selNonTableWords, ImDocumentMarkupPanel idmp) {
		
		//	prepare marking table in convex hull of selected tables
		BoundingBox extWordsBox = ImLayoutObject.getAggregateBox(selNonTableWords);
		BoundingBox extTableBox = new BoundingBox(
				Math.min(extWordsBox.left, selTable.bounds.left),
				Math.max(extWordsBox.right, selTable.bounds.right),
				Math.min(extWordsBox.top, selTable.bounds.top),
				Math.max(extWordsBox.bottom, selTable.bounds.bottom)
			);
		
		//	get words
		ImWord[] extTableWords = page.getWordsInside(extTableBox);
		
		//	get statistics for extended table area
		TableAreaStatistics extTableBoxStats = TableAreaStatistics.getTableAreaStatistics(page, extTableWords);
		
		//	compute merged table markup, considering existing gaps
		ImRegion[] selTables = {selTable};
		TableAreaMarkup extTableMarkup = this.getTableMarkup(page, extTableWords, selTables, extTableBox, extTableBoxStats, selTable.hasAttribute(ImRegion.IN_LINE_OBJECT_MARKER_ATTRIBUTE));
		
		//	get rows and columns of extended table
		if (extTableMarkup.rows.length < 3) {
			DialogFactory.alert("The table cannot be extended because the extension does not split into rows.", "Cannot Extend Table", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if (extTableMarkup.cols.length < 2) {
			DialogFactory.alert("The table cannot be extended because the extension does not split into columns.", "Cannot Extend Table", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		//	finish markup
		return this.finishMarkTable(page, extTableBox, extTableWords, extTableMarkup, selTables, idmp);
	}
	
	private boolean finishMarkTable(ImPage page, BoundingBox tableBox, ImWord[] tableWords, TableAreaMarkup tableMarkup, ImRegion[] exTables, ImDocumentMarkupPanel idmp) {
		
		//	clear table area (there might be overlapping blocks)
		this.clearTableArea(page, tableBox, tableWords);
		
		//	diff merged table markup
		if (exTables != null) {
			for (int t = 0; t < exTables.length; t++)
				tableMarkup = this.diffTableMarkup(tableMarkup, exTables[t]);
		}
		
		//	remove existing tables
		if (exTables != null)
			for (int t = 0; t < exTables.length; t++) {
				ImRegion[] selTableCells = getRegionsInside(page, exTables[t].bounds, ImRegion.TABLE_CELL_TYPE, true);
				for (int c = 0; c < selTableCells.length; c++)
					page.removeRegion(selTableCells[c]);
				ImRegion[] selTableRows = getRegionsInside(page, exTables[t].bounds, ImRegion.TABLE_ROW_TYPE, true);
				for (int r = 0; r < selTableRows.length; r++)
					page.removeRegion(selTableRows[r]);
				ImRegion[] selTableCols = getRegionsInside(page, exTables[t].bounds, ImRegion.TABLE_COL_TYPE, true);
				for (int c = 0; c < selTableCols.length; c++)
					page.removeRegion(selTableCols[c]);
				exTables[t].setAttribute("ignoreWordCleanup"); // save the hassle, we're marking another table right away
				page.removeRegion(exTables[t]);
			}
		
		//	add merged table markup
		this.addTableMarkup(tableMarkup);
		
		//	put words in correct order
		ImUtils.makeStream(tableWords, ImWord.TEXT_STREAM_TYPE_TABLE, null);
		ImUtils.orderTableWords(this.markTableCells(page, tableMarkup.table));
		this.cleanupTableAnnotations(page.getDocument(), tableMarkup.table);
		
		//	TODO test gap preservation (doesn't seen to work in all cases yet, especially for row gaps)
		
		//	show regions in invoker
		if (idmp != null) {
			idmp.setRegionsPainted(ImRegion.TABLE_TYPE, true);
			idmp.setRegionsPainted(ImRegion.TABLE_ROW_TYPE, true);
			idmp.setRegionsPainted(ImRegion.TABLE_COL_TYPE, true);
			idmp.setRegionsPainted(ImRegion.TABLE_CELL_TYPE, true);
		}
		
		//	indicate changes
		return true;
	}
	
	private boolean cutTable(ImPage page, ImRegion table, BoundingBox cutTableBox, BoundingBox cutOffTableBox, String cutOffTextStreamType, ImDocumentMarkupPanel idmp) {
		
		//	compute new table bounds
		ImWord[] cTableWords = page.getWordsInside(cutTableBox);
		BoundingBox cTableBox = ImLayoutObject.getAggregateBox(cTableWords);
		
		//	get existing columns and rows
		ImRegion[] cols = getRegionsOverlapping(page, table.bounds, ImRegion.TABLE_COL_TYPE);
		ImRegion[] rows = getRegionsOverlapping(page, table.bounds, ImRegion.TABLE_ROW_TYPE);
		
		//	cut existing columns to new table bounds, and remove spurious ones
		ArrayList cTableColList = new ArrayList();
		for (int c = 0; c < cols.length; c++) {
			if (cTableBox.overlaps(cols[c].bounds))
				cTableColList.add(new ImRegion(page.getDocument(), page.pageId, new BoundingBox(
					Math.max(cTableBox.left, cols[c].bounds.left),
					Math.min(cTableBox.right, cols[c].bounds.right),
					cTableBox.top,
					cTableBox.bottom
				), ImRegion.TABLE_COL_TYPE));
			else page.removeRegion(cols[c]);
		}
		ImRegion[] cTableCols = ((ImRegion[]) cTableColList.toArray(new ImRegion[cTableColList.size()]));
		
		//	cut existing rows to new table bounds
		ArrayList cTableRowList = new ArrayList();
		for (int r = 0; r < rows.length; r++) {
			if (cTableBox.overlaps(rows[r].bounds))
				cTableRowList.add(new ImRegion(page.getDocument(), page.pageId, new BoundingBox(
					cTableBox.left,
					cTableBox.right,
					Math.max(cTableBox.top, rows[r].bounds.top),
					Math.min(cTableBox.bottom, rows[r].bounds.bottom)
				), ImRegion.TABLE_ROW_TYPE));
			else page.removeRegion(rows[r]);
		}
		ImRegion[] cTableRows = ((ImRegion[]) cTableRowList.toArray(new ImRegion[cTableRowList.size()]));
		
		//	generate cells (naively as intersections of cur columns and rows)
		ImRegion[][] cTableCells = new ImRegion[cTableCols.length][cTableRows.length];
		for (int c = 0; c < cTableCols.length; c++) {
			for (int r = 0; r < cTableRows.length; r++)
				cTableCells[c][r] = new ImRegion(page.getDocument(), page.pageId, new BoundingBox(
					cTableCols[c].bounds.left,
					cTableCols[c].bounds.right,
					cTableRows[r].bounds.top,
					cTableRows[r].bounds.bottom
				), ImRegion.TABLE_CELL_TYPE);
		}
		
		//	mark cut table (we need that for diffing)
		ImRegion cTable = new ImRegion(page.getDocument(), page.pageId, cTableBox, ImRegion.TABLE_TYPE);
		if (table.hasAttribute(ImRegion.IN_LINE_OBJECT_MARKER_ATTRIBUTE))
			cTable.setAttribute(ImRegion.IN_LINE_OBJECT_MARKER_ATTRIBUTE);
		
		//	diff table markup (salvages merged cells)
		TableAreaMarkup cTableMarkup = new TableAreaMarkup(page, cTableWords, cTable, cTableCols, cTableRows, cTableCells);
		cTableMarkup = this.diffTableMarkup(cTableMarkup, table);
		
		//	keep track of whether or not we'll have to repeat the whole procedure
		boolean columnOrRowReduced = false;
		
		//	shrink cut columns to words in cells not protruding beyond respective column boundary
		for (int c = 0; c < cTableCols.length; c++) {
			int colWordLeft = cTableCols[c].bounds.right;
			int colWordRight = cTableCols[c].bounds.left;
			for (int r = 0; r < cTableRows.length; r++) {
				ImWord[] cellWords = page.getWordsInside(cTableMarkup.cells[c][r].bounds);
				if (cellWords.length == 0)
					continue;
				BoundingBox cellWordBounds = ImLayoutObject.getAggregateBox(cellWords);
				if ((c == 0) || (cTableMarkup.cells[c - 1][r] != cTableMarkup.cells[c][r]))
					colWordLeft = Math.min(colWordLeft, cellWordBounds.left);
				if (((c + 1) == cTableCols.length) || (cTableMarkup.cells[c + 1][r] != cTableMarkup.cells[c][r]))
					colWordRight = Math.max(colWordRight, cellWordBounds.right);
			}
			if ((colWordLeft < colWordRight) && ((cTableCols[c].bounds.left < colWordLeft) || (colWordRight < cTableCols[c].bounds.right))) {
				cTableCols[c] = new ImRegion(page.getDocument(), page.pageId, new BoundingBox(
					colWordLeft,
					colWordRight,
					cTableBox.top,
					cTableBox.bottom
				), ImRegion.TABLE_COL_TYPE); 
				columnOrRowReduced = true;
			}
		}
		
		//	shrink cut rows to words in cells not protruding beyond respective row boundary
		for (int r = 0; r < cTableRows.length; r++) {
			int rowWordTop = cTableRows[r].bounds.bottom;
			int rowWordBottom = cTableRows[r].bounds.top;
			for (int c = 0; c < cTableCols.length; c++) {
				ImWord[] cellWords = page.getWordsInside(cTableMarkup.cells[c][r].bounds);
				if (cellWords.length == 0)
					continue;
				BoundingBox cellWordBounds = ImLayoutObject.getAggregateBox(cellWords);
				if ((r == 0) || (cTableMarkup.cells[c][r - 1] != cTableMarkup.cells[c][r]))
					rowWordTop = Math.min(rowWordTop, cellWordBounds.top);
				if (((r + 1) == cTableRows.length) || (cTableMarkup.cells[c][r + 1] != cTableMarkup.cells[c][r]))
					rowWordBottom = Math.max(rowWordBottom, cellWordBounds.bottom);
			}
			if ((rowWordTop < rowWordBottom) && ((cTableRows[r].bounds.top < rowWordTop) || (rowWordBottom < cTableRows[r].bounds.bottom))) {
				cTableRows[r] = new ImRegion(page.getDocument(), page.pageId, new BoundingBox(
					cTableBox.left,
					cTableBox.right,
					rowWordTop,
					rowWordBottom
				), ImRegion.TABLE_ROW_TYPE); 
				columnOrRowReduced = true;
			}
		}
		
		//	if we did shrink something, we need to repeat the whole hassle
		if (columnOrRowReduced) {
			
			//	re-generate cells (naively as intersections)
			for (int c = 0; c < cTableCols.length; c++) {
				for (int r = 0; r < cTableRows.length; r++)
					cTableCells[c][r] = new ImRegion(page.getDocument(), page.pageId, new BoundingBox(
						cTableCols[c].bounds.left,
						cTableCols[c].bounds.right,
						cTableRows[r].bounds.top,
						cTableRows[r].bounds.bottom
					), ImRegion.TABLE_CELL_TYPE);
			}
			
			
			//	re-diff table markup (re-salvages merged cells)
			cTableMarkup = this.diffTableMarkup(cTableMarkup, table);
		}
		
		//	clean up old cells not overlapping with new table bounds (columns and rows are cleaned up above)
		ImRegion[] cells = getRegionsOverlapping(page, table.bounds, ImRegion.TABLE_CELL_TYPE);
		for (int c = 0; c < cells.length; c++) {
			if (!cTableBox.overlaps(cells[c].bounds))
				page.removeRegion(cells[c]);
		}
		
		//	remove old table
		table.setAttribute("ignoreWordCleanup"); // save the hassle, we're marking another table right away, and take care of the leftovers ourselves
		page.removeRegion(table);
		
		//	enclose cut-off words in block
		ImWord[] coTableWords = page.getWordsInside(cutOffTableBox);
		if (cutOffTextStreamType != null)
			ImUtils.makeStream(coTableWords, cutOffTextStreamType, null);
		ImUtils.orderStream(coTableWords, ImUtils.leftRightTopDownOrder);
		Arrays.sort(coTableWords, ImUtils.textStreamOrder);
		this.regionActionProvider.markBlock(page, ImLayoutObject.getAggregateBox(coTableWords));
		
		//	add new markup (will recycle or clean up old regions that overlap with new table)
		this.addTableMarkup(cTableMarkup);
		
		//	put words in correct order
		ImUtils.orderTableWords(this.markTableCells(page, cTableMarkup.table));
		cleanupTableAnnotations(page.getDocument(), cTableMarkup.table);
		
		//	indicate change
		return true;
	}
	
	private boolean assignTableCaption(ImDocument doc, ImRegion table, ImWord captionWord) {
		
		//	find affected caption
		ImAnnotation[] wordAnnots = doc.getAnnotationsSpanning(captionWord);
		ArrayList wordCaptions = new ArrayList(2);
		for (int a = 0; a < wordAnnots.length; a++) {
			if (!ImAnnotation.CAPTION_TYPE.equals(wordAnnots[a].getType()))
				continue;
			if (ImWord.TEXT_STREAM_TYPE_CAPTION.equals(captionWord.getTextStreamType()))
				wordCaptions.add(wordAnnots[a]);
			else if (wordAnnots[a].hasAttribute(ImAnnotation.IN_LINE_OBJECT_MARKER_ATTRIBUTE))
				wordCaptions.add(wordAnnots[a]);
		}
		if (wordCaptions.size() != 1)
			return false;
		ImAnnotation wordCaption = ((ImAnnotation) wordCaptions.get(0));
		
		//	does this caption match?
		String firstWordStr = ImUtils.getStringFrom(wordCaption.getFirstWord());
		if (!firstWordStr.toLowerCase().startsWith("tab")) {
			int choice = DialogFactory.confirm("This caption appears to belong to a figure rather than a table.\r\nAre you sure you want to assign it to the table?", "Assign Figure Caption to Table?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (choice != JOptionPane.YES_OPTION)
				return false;
		}
		
		//	set attributes
		wordCaption.setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, table.bounds.toString());
		wordCaption.setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + table.pageId));
		wordCaption.setAttribute(ImAnnotation.CAPTION_TARGET_IS_TABLE_ATTRIBUTE);
		return true;
	}
	
	private boolean connectTableRows(ImRegion startTable, ImWord secondWord) {
		ImRegion secondWordTable = this.getTableAt(secondWord);
		if (!ImWord.TEXT_STREAM_TYPE_TABLE.equals(secondWord.getTextStreamType()) || (secondWordTable == null)) {
			DialogFactory.alert(("'" + secondWord.getString() + "' does not belong to a table"), "Cannot Merge Rows", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return this.connectTableRows(startTable, secondWordTable);
	}
	private boolean connectTableRows(ImRegion startTable, ImPage secondPage, Point secondPoint) {
		ImRegion secondPointTable = this.getTableAt(secondPage, secondPoint);
		if (secondPointTable == null)
			return false;
		return this.connectTableRows(startTable, secondPointTable);
	}
	private boolean connectTableRows(ImRegion startTable, ImRegion secondTable) {
		if ((startTable.pageId == secondTable.pageId) && startTable.bounds.equals(secondTable.bounds)) {
			DialogFactory.alert("A table cannot be merged with itself", "Cannot Merge Rows", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		if (!ImUtils.areTableRowsCompatible(startTable, secondTable, false)) {
			DialogFactory.alert("The two tables are not compatible; in order to merge rows, two tables have to have the same number of rows", "Cannot Merge Rows", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		String[] labels1 = ImUtils.getTableRowLabels(startTable);
		String[] labels2 = ImUtils.getTableRowLabels(secondTable);
		if ((labels1 == null) || (labels2 == null) || (labels1.length != labels2.length)) {
			DialogFactory.alert(("The two tables are not compatible; in order to merge rows, two tables have to have the same number of rows"), "Cannot Merge Rows", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		StringVector misMatchLabels = null;
		for (int l = 1; l < labels1.length; l++) {
			if (labels1[l].equals(labels2[l]))
				continue;
			if (misMatchLabels == null)
				misMatchLabels = new StringVector();
			if (misMatchLabels.size() < 5)
				misMatchLabels.addElementIgnoreDuplicates(labels1[l] + "    " + labels2[l]);
			else misMatchLabels.addElementIgnoreDuplicates("...");
		}
		if (misMatchLabels != null) {
			int choice = DialogFactory.confirm(("The row labels of the two tables are not the same:\r\n  " + misMatchLabels.concatStrings("\r\n  ") + "\r\nMerge table rows anyway?"), "Row Labels Not Matching", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (choice != JOptionPane.YES_OPTION)
				return false;
		}
		
		ImRegion[] startTables = ImUtils.getColumnConnectedTables(startTable);
		ImRegion[] secondWordTables = ImUtils.getColumnConnectedTables(secondTable);
		if (startTables.length != secondWordTables.length) {
			DialogFactory.alert("The two tables are connected to other tables, and the rows not compatible;\nin order to merge rows, two tables have to have\n- the same number of rows\n- the same label on each one but the first (column header) row\nTry establishing all column extension relations first", "Cannot Merge Rows", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		if (startTables.length > 1) {
			for (int t = 0; t < startTables.length; t++)
				if (!ImUtils.areTableRowsCompatible(startTables[t], secondWordTables[t], true)) {
					DialogFactory.alert("The two tables are connected to other tables, and the rows not compatible;\nin order to merge rows, two tables have to have\n- the same number of rows\n- the same label on each one but the first (column header) row\nTry establishing all column extension relations first", "Cannot Merge Rows", JOptionPane.ERROR_MESSAGE);
					return false;
				}
		}
		
		for (int t = 0; t < startTables.length; t++) {
			startTables[t].setAttribute("rowsContinueIn", (secondWordTables[t].pageId + "." + secondWordTables[t].bounds));
			secondWordTables[t].setAttribute("rowsContinueFrom", (startTables[t].pageId + "." + startTables[t].bounds));
		}
		DialogFactory.alert("Table rows merged successfully; you can copy the whole grid of connected tables via 'Copy Table Grid Data'", "Table Rows Merged", JOptionPane.INFORMATION_MESSAGE);
		return true;
	}
	
	private boolean disconnectTableRows(ImDocument doc, ImRegion table, boolean left, boolean right) {
		ImRegion[] colTables = ImUtils.getColumnConnectedTables(table);
		ImRegion leftTable = null;
		if (left) {
			leftTable = ImUtils.getTableForId(doc, ((String) table.getAttribute("rowsContinueFrom")));
			ImRegion[] leftTables = null;
			if (leftTable != null) {
				leftTables = ImUtils.getColumnConnectedTables(leftTable);
				for (int t = 0; t < leftTables.length; t++)
					leftTables[t].removeAttribute("rowsContinueIn");
				for (int t = 0; t < colTables.length; t++)
					colTables[t].removeAttribute("rowsContinueFrom");
			}
		}
		ImRegion rightTable = null;
		if (right) {
			rightTable = ImUtils.getTableForId(doc, ((String) table.getAttribute("rowsContinueIn")));
			ImRegion[] rightTables = null;
			if (rightTable != null) {
				rightTables = ImUtils.getColumnConnectedTables(rightTable);
				for (int t = 0; t < colTables.length; t++)
					colTables[t].removeAttribute("rowsContinueIn");
				for (int t = 0; t < rightTables.length; t++)
					rightTables[t].removeAttribute("rowsContinueFrom");
			}
		}
		if ((leftTable != null) && (rightTable != null))
			ImUtils.connectTableRows(leftTable, rightTable);
		DialogFactory.alert("Table rows dissected successfully", "Table Rows Dissected", JOptionPane.INFORMATION_MESSAGE);
		return true;
	}
	
	private boolean connectTableCols(ImRegion startTable, ImWord secondWord) {
		ImRegion secondWordTable = this.getTableAt(secondWord);
		if (!ImWord.TEXT_STREAM_TYPE_TABLE.equals(secondWord.getTextStreamType()) || (secondWordTable == null)) {
			DialogFactory.alert(("'" + secondWord.getString() + "' does not belong to a table"), "Cannot Merge Rows", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return this.connectTableCols(startTable, secondWordTable);
	}
	private boolean connectTableCols(ImRegion startTable, ImPage secondPage, Point secondPoint) {
		ImRegion secondPointTable = this.getTableAt(secondPage, secondPoint);
		if (secondPointTable == null)
			return false;
		return this.connectTableCols(startTable, secondPointTable);
	}
	private boolean connectTableCols(ImRegion startTable, ImRegion secondTable) {
		if ((startTable.pageId == secondTable.pageId) && startTable.bounds.equals(secondTable.bounds)) {
			DialogFactory.alert("A table cannot be merged with itself", "Cannot Merge Columns", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		if (!ImUtils.areTableColumnsCompatible(startTable, secondTable, false)) {
			DialogFactory.alert("The two tables are not compatible; in order to merge columns, two tables have to have the same number of columns", "Cannot Merge Columns", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		String[] labels1 = ImUtils.getTableColumnHeaders(startTable);
		String[] labels2 = ImUtils.getTableColumnHeaders(secondTable);
		if ((labels1 == null) || (labels2 == null) || (labels1.length != labels2.length)) {
			DialogFactory.alert("The two tables are not compatible; in order to merge columns, two tables have to have the same number of columns", "Cannot Merge Columns", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		StringVector misMatchHeaders = null;
		for (int l = 1; l < labels1.length; l++) {
			if (labels1[l].equals(labels2[l]))
				continue;
			if (misMatchHeaders == null)
				misMatchHeaders = new StringVector();
			if (misMatchHeaders.size() < 5)
				misMatchHeaders.addElementIgnoreDuplicates(labels1[l] + "    " + labels2[l]);
			else misMatchHeaders.addElementIgnoreDuplicates("...");
		}
		if (misMatchHeaders != null) {
			int choice = DialogFactory.confirm(("The column headers of the two tables are not the same:\r\n  " + misMatchHeaders.concatStrings("\r\n  ") + "\r\nMerge table columns anyway?"), "Column Headers Not Matching", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (choice != JOptionPane.YES_OPTION)
				return false;
		}
		
		ImRegion[] startTables = ImUtils.getRowConnectedTables(startTable);
		ImRegion[] secondWordTables = ImUtils.getRowConnectedTables(secondTable);
		if (startTables.length != secondWordTables.length) {
			DialogFactory.alert("The two tables are connected to other tables, and the columns not compatible;\nin order to merge columns, two tables have to have\n- the same number of columns\n- the same label on each one but the first (row label) column\nTry establishing all row extension relations first", "Cannot Merge Columns", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		if (startTables.length > 1) {
			for (int t = 0; t < startTables.length; t++)
				if (!ImUtils.areTableColumnsCompatible(startTables[t], secondWordTables[t], true)) {
					DialogFactory.alert("The two tables are connected to other tables, and the columns not compatible;\nin order to merge columns, two tables have to have\n- the same number of columns\n- the same label on each one but the first (row label) column\nTry establishing all row extension relations first", "Cannot Merge Columns", JOptionPane.ERROR_MESSAGE);
					return false;
				}
		}
		
		for (int t = 0; t < startTables.length; t++) {
			startTables[t].setAttribute("colsContinueIn", (secondWordTables[t].pageId + "." + secondWordTables[t].bounds));
			secondWordTables[t].setAttribute("colsContinueFrom", (startTables[t].pageId + "." + startTables[t].bounds));
		}
		DialogFactory.alert("Table columns merged successfully; you can copy the whole grid of connected tables via 'Copy Table Grid Data'", "Table Columns Merged", JOptionPane.INFORMATION_MESSAGE);
		return true;
	}
	
	private boolean disconnectTableCols(ImDocument doc, ImRegion table, boolean up, boolean down) {
		ImRegion[] rowTables = ImUtils.getColumnConnectedTables(table);
		ImRegion topTable = null;
		if (up) {
			topTable = ImUtils.getTableForId(doc, ((String) table.getAttribute("colsContinueFrom")));
			ImRegion[] topTables = null;
			if (topTable != null) {
				topTables = ImUtils.getRowConnectedTables(topTable);
				for (int t = 0; t < topTables.length; t++)
					topTables[t].removeAttribute("colsContinueIn");
				for (int t = 0; t < rowTables.length; t++)
					rowTables[t].removeAttribute("colsContinueFrom");
			}
		}
		ImRegion bottomTable = null;
		if (down) {
			bottomTable = ImUtils.getTableForId(doc, ((String) table.getAttribute("colsContinueIn")));
			ImRegion[] bottomTables = null;
			if (bottomTable != null) {
				bottomTables = ImUtils.getRowConnectedTables(bottomTable);
				for (int t = 0; t < rowTables.length; t++)
					rowTables[t].removeAttribute("colsContinueIn");
				for (int t = 0; t < bottomTables.length; t++)
					bottomTables[t].removeAttribute("colsContinueFrom");
			}
		}
		if ((topTable != null) && (bottomTable != null))
			ImUtils.connectTableColumns(topTable, bottomTable);
		DialogFactory.alert("Table columns dissected successfully", "Table Columns Dissected", JOptionPane.INFORMATION_MESSAGE);
		return true;
	}
	
	private static ImRegion[] getRegionsInside(ImPage page, BoundingBox box, String type, boolean fuzzy) {
		ImRegion[] regions = page.getRegionsInside(box, fuzzy);
		ArrayList regionList = new ArrayList();
		for (int r = 0; r < regions.length; r++) {
			if (type.equals(regions[r].getType()))
				regionList.add(regions[r]);
		}
		return ((ImRegion[]) regionList.toArray(new ImRegion[regionList.size()]));
	}
	
	private static ImRegion[] getRegionsIncluding(ImPage page, BoundingBox box, String type, boolean fuzzy) {
		ImRegion[] regions = page.getRegionsIncluding(box, fuzzy);
		ArrayList regionList = new ArrayList();
		for (int r = 0; r < regions.length; r++) {
			if (type.equals(regions[r].getType()))
				regionList.add(regions[r]);
		}
		return ((ImRegion[]) regionList.toArray(new ImRegion[regionList.size()]));
	}
	
	private static ImRegion[] getRegionsOverlapping(ImPage page, BoundingBox box, String type) {
		ImRegion[] regions = page.getRegions(type);
		ArrayList regionList = new ArrayList();
		for (int r = 0; r < regions.length; r++) {
			if (regions[r].bounds.overlaps(box))
				regionList.add(regions[r]);
		}
		return ((ImRegion[]) regionList.toArray(new ImRegion[regionList.size()]));
	}
	
	private static ImWord[] getWordsOverlapping(ImWord[] words, BoundingBox box) {
		ArrayList wordList = new ArrayList();
		for (int w = 0; w < words.length; w++) {
			if (words[w].bounds.overlaps(box))
				wordList.add(words[w]);
		}
		return ((ImWord[]) wordList.toArray(new ImWord[wordList.size()]));
	}
	
	private static void copyTableData(ImRegion table, char separator) {
		ImUtils.copy(new StringSelection(ImUtils.getTableData(table, separator)));
	}
	
	private static void copyTableDataXml(ImRegion table) {
		ImUtils.copy(new HtmlSelection(ImUtils.getTableDataXml(table)));
	}
	
	private static void copyTableGridData(ImRegion table, char separator) {
		ImUtils.copy(new StringSelection(ImUtils.getTableGridData(table, separator)));
	}
	
	private static void copyTableGridDataXml(ImRegion table) {
		ImUtils.copy(new HtmlSelection(ImUtils.getTableGridDataXml(table)));
	}
	
	private static class HtmlSelection implements Transferable {
		private static List htmlFlavors = new ArrayList(3);
		static {
			try {
				htmlFlavors.add(new DataFlavor("text/html;class=java.lang.String"));
				htmlFlavors.add(new DataFlavor("text/html;class=java.io.Reader"));
				htmlFlavors.add(new DataFlavor("text/html;charset=unicode;class=java.io.InputStream"));
			}
			catch (ClassNotFoundException ex) {
				ex.printStackTrace();
			}
		}
		
		private String html;
		
		HtmlSelection(String html) {
			this.html = html;
		}
		
		public DataFlavor[] getTransferDataFlavors() {
			return (DataFlavor[]) htmlFlavors.toArray(new DataFlavor[htmlFlavors.size()]);
		}
		
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return htmlFlavors.contains(flavor);
		}
		
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if (String.class.equals(flavor.getRepresentationClass()))
				return this.html;
			else if (Reader.class.equals(flavor.getRepresentationClass()))
				return new StringReader(this.html);
			else if (InputStream.class.equals(flavor.getRepresentationClass())) try {
				return new ByteArrayInputStream(this.html.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException uee) { /* UTF-8 _is_ supported */ }
			throw new UnsupportedFlavorException(flavor);
		}
	}
}
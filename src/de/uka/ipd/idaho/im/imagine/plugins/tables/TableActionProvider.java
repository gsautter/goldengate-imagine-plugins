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
package de.uka.ipd.idaho.im.imagine.plugins.tables;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import de.uka.ipd.idaho.gamta.util.constants.TableConstants;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.analysis.Imaging;
import de.uka.ipd.idaho.im.analysis.Imaging.AnalysisImage;
import de.uka.ipd.idaho.im.analysis.Imaging.ImagePartRectangle;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;

/**
 * This plugin provides actions for marking tables in Image Markup documents,
 * and for very simple data extraction from tables.
 * 
 * @author sautter
 */
public class TableActionProvider extends AbstractSelectionActionProvider implements TableConstants {
	
	//	example with multi-line cells (in-cell line margin about 5 pixels, row margin about 15 pixels): zt00904.pdf, page 6
	
	//	example without multi-line cells, very tight row margin (2 pixels): Milleretal2014Anthracotheres.pdf, page 4
	
	//	example without multi-line cells, normal row margin: zt00619.o.pdf, page 3
	
	private static final String TABLE_ROW_TYPE = "tableRow";
	private static final String TABLE_COL_TYPE = "tableCol";
	private static final String TABLE_CELL_TYPE = "tableCell";
	
	/** public zero-argument constructor for class loading */
	public TableActionProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Table Actions";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, ImPage page, ImDocumentMarkupPanel idmp) {
		
		//	mark selection
		BoundingBox selectedBox = new BoundingBox(Math.min(start.x, end.x), Math.max(start.x, end.x), Math.min(start.y, end.y), Math.max(start.y, end.y));
		
		//	get selected words
		ImWord[] selectedWords = page.getWordsInside(selectedBox);
		
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
		return this.getActions(page, selectedWords, selectedBox, idmp);
	}
	
	private SelectionAction[] getActions(final ImPage page, final ImWord[] words, final BoundingBox wordsBox, ImDocumentMarkupPanel idmp) {
		LinkedList actions = new LinkedList();
		
		//	get selected table
		final ImRegion[] tables = this.getRegionsIncluding(page, wordsBox, TABLE_ANNOTATION_TYPE, true);
		
		//	no table selected, offer marking selected words as a table and analyze table structure
		if ((tables.length == 0) && (words.length != 0))
			actions.add(new SelectionAction("Mark Table", "Mark selected words as a table and analyze table structure.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return markTable(page, words, wordsBox, invoker);
				}
			});
		
		//	one table selected, offer editing structure
		else if (tables.length == 1) {
			
			//	working on table rows
			if (idmp.areRegionsPainted(TABLE_ROW_TYPE) && (words.length != 0)) {
				final ImRegion[] selectedRows = this.getRegionsInside(page, wordsBox, TABLE_ROW_TYPE, true);
				final ImRegion[] partSelectedRows = this.getRegionsIncluding(page, wordsBox, TABLE_ROW_TYPE, true);
				
				//	if multiple table row regions selected, offer merging them, and cells along the way
				if (selectedRows.length > 1)
					actions.add(new SelectionAction("Merge Table Rows", "Merge selected table rows.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							new ImRegion(page, wordsBox, TABLE_ROW_TYPE);
							for (int r = 0; r < selectedRows.length; r++)
								page.removeRegion(selectedRows[r]);
							ImRegion[][] tableCells = markTableCells(page, tables[0], getRegionsInside(page, tables[0].bounds, TABLE_ROW_TYPE, false), getRegionsInside(page, tables[0].bounds, TABLE_COL_TYPE, false));
							orderTableWords(tableCells);
							return true;
						}
					});
				
				//	if only part of one row selected, offer splitting row
				else if (partSelectedRows.length == 1)
					actions.add(new SelectionAction("Split Table Row", "Split selected table row.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							ImWord[] selectedRowWords = partSelectedRows[0].getWords();
							
							//	order words above, inside, and below selection
							LinkedList above = new LinkedList();
							LinkedList inside = new LinkedList();
							LinkedList below = new LinkedList();
							for (int w = 0; w < selectedRowWords.length; w++) {
								 if (selectedRowWords[w].centerY < wordsBox.top)
									above.add(selectedRowWords[w]);
								 else if (selectedRowWords[w].centerY < wordsBox.bottom)
									inside.add(selectedRowWords[w]);
								 else below.add(selectedRowWords[w]);
							}
							
							//	anything to split at all?
							int emptySplitRows = 0;
							if (above.isEmpty())
								emptySplitRows++;
							if (inside.isEmpty())
								emptySplitRows++;
							if (below.isEmpty())
								emptySplitRows++;
							if (emptySplitRows >= 2)
								return false;
							
							//	create two or three new rows
							if (above.size() != 0) {
								int arBottom = partSelectedRows[0].bounds.top;
								for (Iterator wit = above.iterator(); wit.hasNext();)
									arBottom = Math.max(arBottom, ((ImWord) wit.next()).bounds.bottom);
								BoundingBox arBox = new BoundingBox(tables[0].bounds.left, tables[0].bounds.right, partSelectedRows[0].bounds.top, arBottom);
								new ImRegion(page, arBox, TABLE_ROW_TYPE);
							}
							if (inside.size() != 0) {
								int irTop = partSelectedRows[0].bounds.bottom;
								int irBottom = partSelectedRows[0].bounds.top;
								for (Iterator wit = inside.iterator(); wit.hasNext();) {
									ImWord imw = ((ImWord) wit.next());
									irTop = Math.min(irTop, imw.bounds.top);
									irBottom = Math.max(irBottom, imw.bounds.bottom);
								}
								BoundingBox irBox = new BoundingBox(tables[0].bounds.left, tables[0].bounds.right, irTop, irBottom);
								new ImRegion(page, irBox, TABLE_ROW_TYPE);
							}
							if (below.size() != 0) {
								int brTop = partSelectedRows[0].bounds.bottom;
								for (Iterator wit = below.iterator(); wit.hasNext();)
									brTop = Math.min(brTop, ((ImWord) wit.next()).bounds.top);
								BoundingBox brBox = new BoundingBox(tables[0].bounds.left, tables[0].bounds.right, brTop, partSelectedRows[0].bounds.bottom);
								new ImRegion(page, brBox, TABLE_ROW_TYPE);
							}
							
							//	remove selected row
							page.removeRegion(partSelectedRows[0]);
							
							//	clean up table structure
							ImRegion[][] tableCells = markTableCells(page, tables[0], getRegionsInside(page, tables[0].bounds, TABLE_ROW_TYPE, false), getRegionsInside(page, tables[0].bounds, TABLE_COL_TYPE, false));
							orderTableWords(tableCells);
							return true;
						}
					});
			}
			
			//	working on table columns
			if (idmp.areRegionsPainted(TABLE_COL_TYPE) && (words.length != 0)) {
				final ImRegion[] selectedCols = this.getRegionsInside(page, wordsBox, TABLE_COL_TYPE, true);
				final ImRegion[] partSelectedCols = this.getRegionsIncluding(page, wordsBox, TABLE_COL_TYPE, true);
				
				//	if multiple table column regions selected, offer merging them, and cells along the way
				if (selectedCols.length > 1)
					actions.add(new SelectionAction("Merge Table Columns", "Merge selected table columns.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							new ImRegion(page, wordsBox, TABLE_COL_TYPE);
							for (int c = 0; c < selectedCols.length; c++)
								page.removeRegion(selectedCols[c]);
							ImRegion[][] tableCells = markTableCells(page, tables[0], getRegionsInside(page, tables[0].bounds, TABLE_ROW_TYPE, false), getRegionsInside(page, tables[0].bounds, TABLE_COL_TYPE, false));
							orderTableWords(tableCells);
							return true;
						}
					});
				
				//	if only part of one column selected, offer splitting column
				else if (partSelectedCols.length == 1)
					actions.add(new SelectionAction("Split Table Column", "Split selected table column.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							ImWord[] selectedColWords = partSelectedCols[0].getWords();
							
							//	order words left of, inside, and right of selection
							LinkedList left = new LinkedList();
							LinkedList inside = new LinkedList();
							LinkedList right = new LinkedList();
							for (int w = 0; w < selectedColWords.length; w++) {
								 if (selectedColWords[w].centerX < wordsBox.left)
									left.add(selectedColWords[w]);
								 else if (selectedColWords[w].centerX < wordsBox.right)
									inside.add(selectedColWords[w]);
								 else right.add(selectedColWords[w]);
							}
							
							//	anything to split at all?
							int emptySplitCols = 0;
							if (left.isEmpty())
								emptySplitCols++;
							if (inside.isEmpty())
								emptySplitCols++;
							if (right.isEmpty())
								emptySplitCols++;
							if (emptySplitCols >= 2)
								return false;
							
							//	create two or three new columns
							if (left.size() != 0) {
								int lcRight = partSelectedCols[0].bounds.left;
								for (Iterator wit = left.iterator(); wit.hasNext();)
									lcRight = Math.max(lcRight, ((ImWord) wit.next()).bounds.right);
								BoundingBox lcBox = new BoundingBox(partSelectedCols[0].bounds.left, lcRight, tables[0].bounds.top, tables[0].bounds.bottom);
								new ImRegion(page, lcBox, TABLE_COL_TYPE);
							}
							if (inside.size() != 0) {
								int icLeft = partSelectedCols[0].bounds.right;
								int icRight = partSelectedCols[0].bounds.left;
								for (Iterator wit = inside.iterator(); wit.hasNext();) {
									ImWord imw = ((ImWord) wit.next());
									icLeft = Math.min(icLeft, imw.bounds.left);
									icRight = Math.max(icRight, imw.bounds.right);
								}
								BoundingBox icBox = new BoundingBox(icLeft, icRight, tables[0].bounds.top, tables[0].bounds.bottom);
								new ImRegion(page, icBox, TABLE_COL_TYPE);
							}
							if (right.size() != 0) {
								int rcLeft = partSelectedCols[0].bounds.right;
								for (Iterator wit = right.iterator(); wit.hasNext();)
									rcLeft = Math.min(rcLeft, ((ImWord) wit.next()).bounds.left);
								BoundingBox rcBox = new BoundingBox(rcLeft, partSelectedCols[0].bounds.right, tables[0].bounds.top, tables[0].bounds.bottom);
								new ImRegion(page, rcBox, TABLE_COL_TYPE);
							}
							
							//	remove selected column
							page.removeRegion(partSelectedCols[0]);
							
							//	clean up table structure
							ImRegion[][] tableCells = markTableCells(page, tables[0], getRegionsInside(page, tables[0].bounds, TABLE_ROW_TYPE, false), getRegionsInside(page, tables[0].bounds, TABLE_COL_TYPE, false));
							orderTableWords(tableCells);
							return true;
						}
					});
			}
			
			//	update table structure after manual modifications
			actions.add(new SelectionAction("Update Table", "Update table to reflect manual modifications.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					ImRegion[][] tableCells = markTableCells(page, tables[0], getRegionsInside(page, tables[0].bounds, TABLE_ROW_TYPE, false), getRegionsInside(page, tables[0].bounds, TABLE_COL_TYPE, false));
					orderTableWords(tableCells);
					return true;
				}
			});
			
			//	offer exporting tables as CSV
			actions.add(new SelectionAction("Copy Table Data", "Copy the data in the table to the clipboard.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Copy Table Data ...");
					JMenuItem mi;
					mi = new JMenuItem("- CSV (comma separated)");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							copyTableData(tables[0], ',');
						}
					});
					pm.add(mi);
					mi = new JMenuItem("- Excel (semicolon separated)");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							copyTableData(tables[0], ';');
						}
					});
					pm.add(mi);
					mi = new JMenuItem("- Text (tab separated)");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							copyTableData(tables[0], '\t');
						}
					});
					pm.add(mi);
					return pm;
				}
			});
		}
		
		//	TODO if multiple table cell regions selected, offer merging them, setting colspan and rowspan
		
		//	TODO also offer merging whole tables, namely rows across tables
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	private boolean markTable(ImPage page, ImWord[] words, BoundingBox tableBox, ImDocumentMarkupPanel idmp) {
		
		//	cut table out of main text
		ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_TABLE, null);
		
		//	flatten out table content
		for (int w = 0; w < words.length; w++) {
			if (words[w].getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
				words[w].setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
		}
		
		//	wrap region around words
		ImRegion tableRegion = new ImRegion(words[0].getDocument(), words[0].pageId, tableBox, TABLE_ANNOTATION_TYPE);
		PageImage tableImage = null;
		
		//	synthesize region image with words as black boxes
		AnalysisImage tableWordImage = null;
		ImagePartRectangle tableWordImageBox = null;
		
		//	get rows
		ImRegion[] tableRows = this.getRegionsInside(page, tableBox, TABLE_ROW_TYPE, false);
		if (tableRows.length == 0) {
			if (tableWordImage == null) {
				tableImage = tableRegion.getImage();
				tableWordImage = this.getTableWordImage(words, tableImage);
				tableWordImageBox = Imaging.getContentBox(tableWordImage);
			}
			int maxScore = 0;
			ImagePartRectangle[] maxScoreRows = null;
			for (int rowGap = (tableImage.currentDpi / 6) /* a sixth of an inch, some 6mm */; rowGap > (tableImage.currentDpi / 30) /* less than 1mm */; rowGap--) {
				ImagePartRectangle[] rows = Imaging.splitIntoRows(tableWordImageBox, rowGap);
				if ((rows.length * rowGap) > maxScore) {
					maxScore = (rows.length * rowGap);
					maxScoreRows = rows;
				}
			}
			if (maxScoreRows == null)
				return false;
			tableRows = new ImRegion[maxScoreRows.length];
			for (int r = 0; r < maxScoreRows.length; r++)
				tableRows[r] = new ImRegion(words[0].getDocument(), words[0].pageId, new BoundingBox((maxScoreRows[r].getLeftCol() + tableBox.left), (maxScoreRows[r].getRightCol() + tableBox.left), (maxScoreRows[r].getTopRow() + tableBox.top), (maxScoreRows[r].getBottomRow() + tableBox.top)), TABLE_ROW_TYPE);
		}
		
		//	get columns
		ImRegion[] tableCols = this.getRegionsInside(page, tableBox, TABLE_COL_TYPE, false);
		if (tableCols.length == 0) {
			if (tableWordImage == null) {
				tableImage = tableRegion.getImage();
				tableWordImage = this.getTableWordImage(words, tableImage);
				tableWordImageBox = Imaging.getContentBox(tableWordImage);
			}
			int maxScore = 0;
			ImagePartRectangle[] maxScoreCols = null;
			for (int colGap = (tableImage.currentDpi / 4) /* a quarter of an inch, some 6mm */; colGap > (tableImage.currentDpi / 25) /* 1mm, more like a word margin */; colGap--) {
				ImagePartRectangle[] cols = Imaging.splitIntoColumns(tableWordImageBox, colGap);
				if ((cols.length * colGap) > maxScore) {
					maxScore = (cols.length * colGap);
					maxScoreCols = cols;
				}
			}
			if (maxScoreCols == null)
				return false;
			tableCols = new ImRegion[maxScoreCols.length];
			for (int c = 0; c < maxScoreCols.length; c++)
				tableCols[c] = new ImRegion(words[0].getDocument(), words[0].pageId, new BoundingBox((maxScoreCols[c].getLeftCol() + tableBox.left), (maxScoreCols[c].getRightCol() + tableBox.left), (maxScoreCols[c].getTopRow() + tableBox.top), (maxScoreCols[c].getBottomRow() + tableBox.top)), TABLE_COL_TYPE);
		}
		
		//	add regions to page so users can correct
		page.addRegion(tableRegion);
		for (int c = 0; c < tableCols.length; c++)
			page.addRegion(tableCols[c]);
		for (int r = 0; r < tableRows.length; r++)
			page.addRegion(tableRows[r]);
		
		//	mark cells as intersection of columns and rows
		ImRegion[][] tableCells = this.markTableCells(page, tableRegion, tableRows, tableCols);
		
		//	order words from each cell as a text stream
		this.orderTableWords(tableCells);
		
		//	show regions in invoker
		idmp.setRegionsPainted(TABLE_ANNOTATION_TYPE, true);
		idmp.setRegionsPainted(TABLE_ROW_TYPE, true);
		idmp.setRegionsPainted(TABLE_COL_TYPE, true);
		idmp.setRegionsPainted(TABLE_CELL_TYPE, true);
		
		//	finally ...
		return true;
	}
	
	private AnalysisImage getTableWordImage(ImWord[] words, PageImage tableImage) {
		BufferedImage tableWordImage = new BufferedImage(tableImage.image.getWidth(), tableImage.image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		Graphics twig = tableWordImage.createGraphics();
		twig.setColor(Color.WHITE);
		twig.fillRect(0, 0, tableWordImage.getWidth(), tableWordImage.getHeight());
		twig.setColor(Color.BLACK);
		for (int w = 0; w < words.length; w++)
			twig.fillRect((words[w].bounds.left - tableImage.leftEdge), (words[w].bounds.top - tableImage.topEdge), (words[w].bounds.right - words[w].bounds.left), (words[w].bounds.bottom - words[w].bounds.top));
		return Imaging.wrapImage(tableWordImage, null);
	}
	
	private ImRegion[][] markTableCells(ImPage page, ImRegion table, ImRegion[] rows, ImRegion[] cols) {
		
		//	sort rows and columns
		Arrays.sort(rows, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				return (((ImRegion) obj1).bounds.top - ((ImRegion) obj2).bounds.top);
			}
		});
		Arrays.sort(cols, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				return (((ImRegion) obj1).bounds.left - ((ImRegion) obj2).bounds.left);
			}
		});
		
		//	get and index existing cells
		ImRegion[] existingCells = table.getRegions(TABLE_CELL_TYPE);
		HashMap cellsByBounds = new HashMap();
		for (int c = 0; c < existingCells.length; c++)
			cellsByBounds.put(existingCells[c].bounds.toString(), existingCells[c]);
		
		//	get current cells
		ImRegion[][] cells = new ImRegion[rows.length][cols.length];
		for (int r = 0; r < rows.length; r++)
			for (int c = 0; c < cols.length; c++) {
				BoundingBox cellBounds = new BoundingBox(cols[c].bounds.left, cols[c].bounds.right, rows[r].bounds.top, rows[r].bounds.bottom);
				ImWord[] cellWords = page.getWordsInside(cellBounds);
				if (cellWords.length != 0) {
					int cbLeft = cellBounds.right;
					int cbRight = cellBounds.left;
					int cbTop = cellBounds.bottom;
					int cbBottom = cellBounds.top;
					for (int w = 0; w < cellWords.length; w++) {
						cbLeft = Math.min(cbLeft, cellWords[w].bounds.left);
						cbRight = Math.max(cbRight, cellWords[w].bounds.right);
						cbTop = Math.min(cbTop, cellWords[w].bounds.top);
						cbBottom = Math.max(cbBottom, cellWords[w].bounds.bottom);
					}
					cellBounds = new BoundingBox(cbLeft, cbRight, cbTop, cbBottom);
				}
				cells[r][c] = ((ImRegion) cellsByBounds.remove(cellBounds.toString()));
				if (cells[r][c] == null)
					cells[r][c] = new ImRegion(page, cellBounds, TABLE_CELL_TYPE);
			}
		
		//	remove spurious cells
		for (Iterator cit = cellsByBounds.values().iterator(); cit.hasNext();)
			page.removeRegion((ImRegion) cit.next());
		
		//	finally ...
		return cells;
	}
	
	private void orderTableWords(ImRegion[][] cells) {
		ImWord lastCellEnd = null;
		for (int r = 0; r < cells.length; r++)
			for (int c = 0; c < cells[r].length; c++) {
				ImWord[] cellWords = cells[r][c].getWords();
				if (cellWords.length == 0)
					continue;
				ImUtils.makeStream(cellWords, null, null);
				ImUtils.orderStream(cellWords, ImUtils.leftRightTopDownOrder);
				Arrays.sort(cellWords, ImUtils.textStreamOrder);
				if (lastCellEnd != null)
					cellWords[0].setPreviousWord(lastCellEnd);
				lastCellEnd = cellWords[cellWords.length-1];
			}
	}
	
	private ImRegion[] getRegionsInside(ImPage page, BoundingBox box, String type, boolean fuzzy) {
		ImRegion[] regions = page.getRegionsInside(box, fuzzy);
		ArrayList regionList = new ArrayList();
		for (int r = 0; r < regions.length; r++) {
			if (type.equals(regions[r].getType()))
				regionList.add(regions[r]);
		}
		return ((ImRegion[]) regionList.toArray(new ImRegion[regionList.size()]));
	}
	
	private ImRegion[] getRegionsIncluding(ImPage page, BoundingBox box, String type, boolean fuzzy) {
		ImRegion[] regions = page.getRegionsIncluding(box, fuzzy);
		ArrayList regionList = new ArrayList();
		for (int r = 0; r < regions.length; r++) {
			if (type.equals(regions[r].getType()))
				regionList.add(regions[r]);
		}
		return ((ImRegion[]) regionList.toArray(new ImRegion[regionList.size()]));
	}
	
	private void copyTableData(ImRegion table, char separator) {
		
		//	get rows and columns
		ImRegion[] rows = this.getRegionsInside(table.getPage(), table.bounds, TABLE_ROW_TYPE, false);
		Arrays.sort(rows, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				return (((ImRegion) obj1).bounds.top - ((ImRegion) obj2).bounds.top);
			}
		});
		ImRegion[] cols = this.getRegionsInside(table.getPage(), table.bounds, TABLE_COL_TYPE, false);
		Arrays.sort(cols, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				return (((ImRegion) obj1).bounds.left - ((ImRegion) obj2).bounds.left);
			}
		});
		
		//	write table data
		StringBuffer tableData = new StringBuffer();
		for (int r = 0; r < rows.length; r++)
			for (int c = 0; c < cols.length; c++) {
				BoundingBox cellBounds = new BoundingBox(cols[c].bounds.left, cols[c].bounds.right, rows[r].bounds.top, rows[r].bounds.bottom);
				ImWord[] cellWords = table.getPage().getWordsInside(cellBounds);
				if ((separator == ',') || (separator == ';'))
					tableData.append('"');
				if (cellWords.length != 0) {
					Arrays.sort(cellWords, ImUtils.textStreamOrder);
					String cellStr = ImUtils.getString(cellWords[0], cellWords[cellWords.length-1], true);
					if ((separator == ',') || (separator == ';')) {
						StringBuffer eCellStr = new StringBuffer();
						for (int i = 0; i < cellStr.length(); i++) {
							char ch = cellStr.charAt(i);
							if (ch == '"')
								eCellStr.append('"');
							eCellStr.append(ch);
						}
						cellStr = eCellStr.toString();
					}
					tableData.append(cellStr);
				}
				if ((separator == ',') || (separator == ';'))
					tableData.append('"');
				if ((c+1) == cols.length)
					tableData.append("\r\n");
				else tableData.append(separator);
			}
		
		//	put data in clipboard
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(tableData.toString()), null);
	}
}
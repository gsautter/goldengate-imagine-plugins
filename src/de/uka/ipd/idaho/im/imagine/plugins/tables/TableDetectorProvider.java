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
import java.awt.Shape;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.gamta.util.ParallelJobRunner;
import de.uka.ipd.idaho.gamta.util.ParallelJobRunner.ParallelFor;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor.SynchronizedProgressMonitor;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.DocumentStyle;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImLayoutObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImSupplement;
import de.uka.ipd.idaho.im.ImSupplement.Graphics;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractGoldenGateImaginePlugin;
import de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.tables.TableAreaStatistics.ColumnOccupationLow;
import de.uka.ipd.idaho.im.imagine.plugins.tables.TableAreaStatistics.GraphicsSlice;
import de.uka.ipd.idaho.im.util.ImDocumentIO;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImUtils;

/**
 * @author sautter
 *
 */
public class TableDetectorProvider extends AbstractGoldenGateImaginePlugin implements ImageMarkupToolProvider {
	private static final String TABLE_DETECTOR_IMT_NAME = "TableDetector";
	private ImageMarkupTool tableDetector = new TableDetector();
	
	private TableActionProvider tableActionProvider;
	
	/** public zero-argument constructor for class loading */
	public TableDetectorProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Table Detector";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		
		//	get table action provider
		if (this.parent == null) {
			this.tableActionProvider = new TableActionProvider();
			this.tableActionProvider.init();
		}
		else this.tableActionProvider = ((TableActionProvider) this.parent.getPlugin(TableActionProvider.class.getName()));
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
		String[] tmins = {TABLE_DETECTOR_IMT_NAME};
		return tmins;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		if (TABLE_DETECTOR_IMT_NAME.equals(name))
			return this.tableDetector;
		else return null;
	}
	
	private class TableDetector implements ImageMarkupTool {
		public String getLabel() {
			return "Detect Tables";
		}
		public String getTooltip() {
			return "Detect tables with graphics grids";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	we're only handling documents as a whole
			if (annot != null)
				return;
			
			//	get document table style
			DocumentStyle docStyle = DocumentStyle.getStyleFor(doc);
			DocumentStyle docLayout = docStyle.getSubset("layout");
			DocumentStyle tableLayout = docLayout.getSubset("table");
			
			//	run detection for individual classes of tables
			detectTables(doc, tableLayout, idmp, pm);
		}
	}
	
	private static final boolean DEBUG = true;
	
	/**
	 * Detect the tables in a document, page by page.
	 * @param doc the document to process
	 * @param tableStyle the table layout subset of a document style template
	 * @param idmp the markup panel the document is displaying in (if any)
	 * @param pm a progress monitor to inform on table detection progress
	 */
	public void detectTables(final ImDocument doc, final DocumentStyle tableStyle, final ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
		if (pm == null)
			pm = ProgressMonitor.dummy;
		
		//	build progress monitor with synchronized methods instead of synchronizing in-code
		final ProgressMonitor spm = ((pm instanceof SynchronizedProgressMonitor) ? ((SynchronizedProgressMonitor) pm) : new SynchronizedProgressMonitor(pm));
		
		//	get pages
		final ImPage[] pages = doc.getPages();
		
		//	collect repeating graphics
		final HashSet ignoreGraphics = this.getPageDesignGraphics(doc, pages, spm);
		
		//	compute average density of document blocks
		System.out.println("Computing document word density");
		int stripeWidthSum = 0;
		int stripeArea = 0;
		int wordWidthSum = 0;
		int wordHeightSum = 0;
		int wordCount = 0;
		int wordArea = 0;
		int wordDistanceSum = 0;
		float nWordDistanceSum = 0;
		int wordDistanceCount = 0;
		for (int pg = 0; pg < pages.length; pg++) {
			
			//	assess page blocks
			ImRegion[] pgBlocks = pages[pg].getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
			Arrays.sort(pgBlocks, ImUtils.topDownOrder);
			for (int b = 0; b < pgBlocks.length; b++) {
				System.out.println(" - assessing block " + pages[pg].pageId + "." + pgBlocks[b].bounds);
				
				//	TODO get rid of row stripes here
				RowStripe[] bStripes = getRowStripes(pages[pg], pgBlocks[b].bounds);
				if (bStripes.length == 0) {
//					System.out.println("   --> empty block");
					continue; // empty block, image or graphics with no labels
				}
//				System.out.println("   - got " + bStripes.length + " block stripes");
//				float bWordDensity = getWordDensity(bStripes);
//				System.out.println("   --> block word density is " + bWordDensity);
				
				for (int s = 0; s < bStripes.length; s++) {
					stripeWidthSum += bStripes[s].bounds.getWidth();
					stripeArea += bStripes[s].area;
					wordWidthSum += bStripes[s].wordWidthSum;
					wordHeightSum += bStripes[s].wordHeightSum;
					wordCount += bStripes[s].words.length;
					wordArea += bStripes[s].wordArea;
					wordDistanceSum += bStripes[s].wordDistanceSum;
					nWordDistanceSum += bStripes[s].nWordDistanceSum;
					wordDistanceCount += bStripes[s].wordDistanceCount;
				}
			}
		}
		final float docWordDensity1d = ((stripeWidthSum < 1) ? 0 : (((float) wordWidthSum) / stripeWidthSum));
		final float docWordDensity2d = ((stripeArea < 1) ? 0 : (((float) wordArea) / stripeArea));
		System.out.println("Average document word density is " + docWordDensity1d + " horizonatally, " + docWordDensity2d + " by area");
		final float docWordSpacing = ((wordDistanceCount < 1) ? 0 : (((float) wordDistanceSum) / wordDistanceCount));
		System.out.println("Average document word spacing " + docWordSpacing);
		final float docNormSpaceWidth = ((wordDistanceCount < 1) ? 0 : (((float) nWordDistanceSum) / wordDistanceCount));
		System.out.println("Average normalized document word spacing " + docNormSpaceWidth);
		final float docWordHeight = ((wordCount < 1) ? 0 : (((float) wordHeightSum) / wordCount));
		System.out.println("Average document word height " + docWordHeight);
		
		//	mark tables (now we can run in parallel)
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int p) throws Exception {
				spm.setProgress((p * 100) / pages.length);
				try {
					detectTables(pages[p], doc, tableStyle, ignoreGraphics, docWordHeight, docWordDensity1d, docWordDensity2d, docWordSpacing, docNormSpaceWidth, idmp, spm);
				}
				catch (Exception e) {
					e.printStackTrace(System.out);
				}
				//	TODO figure out what layout hints might be useful for tables
				
				/* TODO restrict table detection:
				 * - maxColumnWidth: maximum width of individual column, prevents putting multi-column text in tables
				 * 
				 * - maxRowHeight: maximum height for individual rows, prevents main text cross splits
				 * - maxAvgRowHeight: maximum average row height, prevents main text multi-cross splits
				 * 
				 * - minColumnCount: minimum number of columns, excludes, e.g., two-column tables
				 * - minRowCount: minimum number of rows: excludes main text cross splits
				 * - minCellCount: minimum number of cells (columns x rows): might be above product of minimums for both dimensions
				 * 
				 * - grid.horizontal: boolean indicating if there are horizontal grid lines, prevents left and right merging beyond widest block
				 * - grid.vertical: boolean indicating if there are vertical grid lines, prevents up and down merging beyond highest block
				 * - grid.frame: boolean indicating outside frame, prevents any merging
				 * ==> not sure whether or not to use these booleans, as we do want to keep grid lines (and anything but words in general) out of block detection
				 */
				
				/* TODO consider using word density in block for table detection
				 * - far fewer words per area in table than in main text
				 *   ==> try and measure this to get an idea
				 * - also measure regularity of spaces in lines / rows
				 *   - should be far less regular in table than in main text
				 *   - average it out for each block ...
				 *   - ... and find regularity distribution in document
				 *     ==> top outliers are potential tables (pending all the other checks, like column headers and row labels)
				 *   ==> try and measure this to get an idea
				 * - also make use of column anchor points (already outlined below)
				 *   - left and right aligned anchor points have to match closely ...
				 *   - ... center anchor points overlap maybe with central third or quarter
				 * ==> anchor points (and in particular their violation) might well help to chop captions off the top of tables
				 */
			}
		}, pages.length, (DEBUG ? 1 : -1));
	}
	
	/**
	 * Detect the tables in a single document page. This method facilitates
	 * parallel processing of the pages in a document. The version of this
	 * method that only takes the document proper as an argument is linear.
	 * To use this method in parallel, page decoration graphics need to be
	 * obtained up front.
	 * @param page the page to mark the table in
	 * @param doc the document the page belongs to
	 * @param tableStyle the table layout subset of a document style template
	 * @param ignoreGraphics graphics objects to ignore as page style elements
	 * @param docWordHeight the word height average across the document
	 * @param docNormSpaceWidth the space width average across the document,
	 *            normalized by height of adjacent words
	 * @param idmp the markup panel the document is displaying in (if any)
	 * @param pm a progress monitor to inform on table detection progress
	 */
	public void detectTables(ImPage page, ImDocument doc, DocumentStyle tableStyle, Set ignoreGraphics, float docWordHeight, float docWordDensity1d, float docWordDensity2d, float docWordSpacing, float docNormSpaceWidth, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
		if (pm == null)
			pm = ProgressMonitor.dummy;
		
		//	get and filter page graphics
		ImSupplement[] pageSupplements = page.getSupplements();
		ArrayList pageGraphicsList = new ArrayList();
		for (int ps = 0; ps < pageSupplements.length; ps++)
			if (pageSupplements[ps] instanceof Graphics) {
				Graphics gr = ((Graphics) pageSupplements[ps]);
				if (ignoreGraphics.contains(pageSupplements[ps]))
					continue; // skip page decoration
				
				//	sort out graphics too narrow and low for a table (less than 1 inch wide, less than half inch tall)
				BoundingBox grBounds = gr.getBounds();
				if ((grBounds.getWidth() < page.getImageDPI()) && ((grBounds.getHeight() * 2) < page.getImageDPI()))
					continue;
				
				//	remove graphics that include mainly curves, as table grids (mainly) consist of straight lines
				Graphics.Path[] grPaths = gr.getPaths();
				int grLineCount = 0;
				int grCurveCount = 0;
				for (int p = 0; p < grPaths.length; p++) {
					Graphics.SubPath[] subPaths = grPaths[p].getSubPaths();
					for (int s = 0; s < subPaths.length; s++) {
						Shape[] subPathShapes = subPaths[s].getShapes();
						for (int h = 0; h < subPathShapes.length; h++) {
							if (subPathShapes[h] instanceof Line2D)
								grLineCount++;
							else if (subPathShapes[h] instanceof CubicCurve2D)
								grCurveCount++;
							else if (subPathShapes[h] instanceof QuadCurve2D)
								grCurveCount++;
						}
					}
				}
				if (grCurveCount > grLineCount)
					continue; // too many curves, too few lines
				
				//	hold on to this one
				pageGraphicsList.add(pageSupplements[ps]);
			}
		Graphics[] pageGraphics = ((Graphics[]) pageGraphicsList.toArray(new Graphics[pageGraphicsList.size()]));
		
		//	anything to work with?
		if (pageGraphics.length == 0)
			return;
		
		/* detect tables:
		 * - categories activated by default or by document style template
		 * - starting out with multi-piece grids to prevent having them partially marked under single-piece categories
		 */
		
		/* This covers the following types of table grids (most Zootaxa, and many others):
		- column header separating horizontal line grids (one internal horizontal line, separating column headers from data, plus maybe some lines inside column header part)
		- partial horizontal line grids (header separating, plus orientation line every two or three rows)
		- full horizontal line grids (horizontal lines or stripe flanks in all row gaps)
		- full horizontal stripe grids
		==> will detect as currently implemented in test code
		 */
		boolean seekHorizontalMultiPieceGridTables = tableStyle.getBooleanProperty("horizontalMultiPieceGrid", true);
		if (seekHorizontalMultiPieceGridTables)
			this.detectHorizontalMultiPieceGridTables(page, pageGraphics, docWordHeight, docWordDensity1d, docWordDensity2d, docWordSpacing, docNormSpaceWidth, pm);
		
		/* This covers the following types of table grids (unseen thus far):
		- row label separating vertical line grids (one internal vertical line or stripe flank, separating row labels from data)
		- full vertical line grids (vertical lines or stripe flanks in all column gaps)
		- partial vertical  line grids unseen as of now
		- full vertical stripe grids
		==> will require combining same-length vertical lines at same vertical position to detect
		==> flip current test code to vertical for detection, combining vertical graphics into grids
		  - require enclosed area to span whole (with some 5% of slack) block, column, or page content width
		 */
		boolean seekVerticalMultiPieceGridTables = tableStyle.getBooleanProperty("verticalMultiPieceGrid", false);
		if (seekVerticalMultiPieceGridTables)
			this.detectVerticalMultiPieceGridTables(page, pageGraphics, docWordHeight, docNormSpaceWidth, pm);
		
		/* This covers the following types of table grids:
		- full frame grids (outside lines boxing whole table)
		  - row label separating (one internal vertical line or stripe flank, separating row labels from data) or full (vertical lines or stripe flanks in all column gaps) column grid (partial unseen as of now)
		  - column header separating (one internal horizontal line or stripe flank, separating column headers from data, plus maybe some lines inside column header part), partial (header separating, plus orientation line every two or three rows), or full row grid (horizontal lines or stripe flanks in all row gaps)
		- comb grids
		  - single horizontal line below column headers, crossing vertical lines in all column gaps
		  - single vertical line after row labels, crossing horizontal lines partially or fully separating rows
		- inverse L stripe grids (L-ing stripes highlighting column headers and row labels, no lines or stripes in data part)
		- cross grids (single horizontal line separating column headers from data, crossing single vertical line separating row labels from data)
		==>  will require separate detector method testing for respective graphics
		 */
		boolean seekSpanningOnePieceGridTables = tableStyle.getBooleanProperty("spanningOnePieceGrid", true);
		if (seekSpanningOnePieceGridTables)
			this.detectSpanningOnePieceGridTables(page, pageGraphics, docWordHeight, docWordDensity1d, docWordDensity2d, docWordSpacing, docNormSpaceWidth, pm);
		
		/* This covers the following types of table grids (unseen as of now):
		- T grids
		  - single horizontal line, separating column headers from data, T-ing single vertical line separating row labels from data, but not protruding into column header row
		  - single vertical line, separating row labels from data, T-ing single horizontal line separating column headers from data, but not protruding into row label column
		- inverse L grids (single horizontal line, separating column headers from data, but not protruding into row label column, L-ing single vertical line separating row labels from data, but not protruding into column header row)
		==> will require row stripe extension above column header separating horizontal line to detect full table
		==> will require extension to block width or even left neighboring block to detect full table
		 */
		boolean seekDataSpanningOnePieceGridTables = tableStyle.getBooleanProperty("dataSpanningOnePieceGrid", false);
		if (seekDataSpanningOnePieceGridTables)
			this.detectDataSpanningOnePieceGridTables(page, pageGraphics, docWordHeight, docNormSpaceWidth, pm);
		
		/* This covers the following types of table grids (many IJSEM in-text-flow tables, and many others):
		- single horizontal line across whole block (or column) width (with some 5% slack) separating column headers from data
		==> will require row stripe extension above and below column header separating horizontal line to detect table
		  - require low row stripe density (> 20% below document average)
		  - require pronounced column gaps (> 66% of average word height)
		  - require large fraction of occupied cells
		  - work in column, and across blocks (row gaps might be above vertical block margin)
		 */
		boolean seekHorizontalSingleLineGridTables = tableStyle.getBooleanProperty("horizontalSingleLineGrid", true);
		if (seekHorizontalSingleLineGridTables)
			this.detectHorizontalSingleLineGridTables(page, pageGraphics, docWordHeight, docNormSpaceWidth, pm);
		
		/* This covers the following types of table grids (unseen thus far):
		- single vertical line separating row labels from data
		==> will require some row stripe analysis to detect
		  - require low row stripe density (> 20% below document average)
		  - require pronounced column gaps (> 66% of average word height)
		  - require large fraction of occupied cells
		 */
		boolean seekVerticalSingleLineGridTables = tableStyle.getBooleanProperty("verticalSingleLineGrid", false);
		if (seekVerticalSingleLineGridTables)
			this.detectVerticalSingleLineGridTables(page, pageGraphics, docWordHeight, docNormSpaceWidth, pm);
	}
	
	/**
	 * Produce a set of graphics supplements that repeat on but all pages and
	 * can thus be considered page design elements rather than pieces of a
	 * table grid.
	 * @param doc the document whose graphics to filter
	 * @param pages the pages of the document
	 * @param pm a progress monitor to inform on table detection progress
	 * @return a set containing the page design graphics
	 */
	public HashSet getPageDesignGraphics(ImDocument doc, ImPage[] pages, ProgressMonitor pm) {
		HashSet pageDesignGraphics = new HashSet();
		if (pm == null)
			pm = ProgressMonitor.dummy;
		
		//	get vector graphics supplements for each page, and count frequencies
		Graphics[][] pageGraphics = new Graphics[pages.length][];
		CountingSet graphicsPageCounts = new CountingSet();
		CountingSet graphicsEvenPageCounts = new CountingSet();
		CountingSet graphicsOddPageCounts = new CountingSet();
		for (int pg = 0; pg < pages.length; pg++) {
			pm.setInfo("Collecting graphics in page " + pages[pg].pageId);
			ImSupplement[] pageSupplements = pages[pg].getSupplements();
			ArrayList pageGraphicsList = new ArrayList();
			for (int s = 0; s < pageSupplements.length; s++) {
				if (pageSupplements[s] instanceof Graphics)
					pageGraphicsList.add(pageSupplements[s]);
			}
			pageGraphics[pg] = ((Graphics[]) pageGraphicsList.toArray(new Graphics[pageGraphicsList.size()]));
			pm.setInfo(" - found " + pageGraphics[pg].length + " individual graphics:");
			for (int g = 0; g < pageGraphics[pg].length; g++) {
				BoundingBox graphBounds = pageGraphics[pg][g].getBounds();
				pm.setInfo("   - " + graphBounds);
				graphicsPageCounts.add(graphBounds);
				if ((pg % 2) == 0)
					graphicsEvenPageCounts.add(graphBounds);
				else graphicsOddPageCounts.add(graphBounds);
			}
		}
		
		//	collect graphics that repeat on but all (or but all even, or but all odd) pages
		for (int pg = 0; pg < pages.length; pg++) {
			pm.setInfo("Assessing graphics in page " + pages[pg].pageId);
			for (int g = 0; g < pageGraphics[pg].length; g++) {
				BoundingBox graphBounds = pageGraphics[pg][g].getBounds();
				pm.setInfo(" - " + graphBounds + ":");
				
				//	graphics existing in majority (maybe 2/3) of pages, likely page decoration
				int graphPageCount = graphicsPageCounts.getCount(graphBounds);
				int graphEvenPageCount = graphicsEvenPageCounts.getCount(graphBounds);
				int graphOddPageCount = graphicsOddPageCounts.getCount(graphBounds);
				pm.setInfo("   - present in " + graphPageCount + " of " + pages.length + " pages (" + graphEvenPageCount + " even, " + graphOddPageCount + " odd)");
				if ((graphPageCount * 3) > (pages.length * 2)) {
					pageDesignGraphics.add(pageGraphics[pg][g]);
					pm.setInfo("     ==> got page decoration");
				}
				else if ((graphOddPageCount == 0) && ((graphEvenPageCount * 3) > pages.length)) {
					pageDesignGraphics.add(pageGraphics[pg][g]);
					pm.setInfo("     ==> got even page decoration");
				}
				else if ((graphEvenPageCount == 0) && ((graphOddPageCount * 3) > pages.length)) {
					pageDesignGraphics.add(pageGraphics[pg][g]);
					pm.setInfo("     ==> got odd page decoration");
				}
			}
		}
		
		//	finally ...
		return pageDesignGraphics;
	}
	
	private void detectSpanningOnePieceGridTables(ImPage page, Graphics[] pageGraphics, float docWordHeight, float docWordDensity1d, float docWordDensity2d, float docWordSpacing, float docNormSpaceWidth, ProgressMonitor pm) {
		
		//	filter out lines
		ArrayList pageGraphicsList = new ArrayList();
		for (int g = 0; g < pageGraphics.length; g++) {
			
			//	sort out graphics too narrow or low for a table (less than 1 inch wide, or less than half inch tall)
			BoundingBox grBounds = pageGraphics[g].getBounds();
			if ((grBounds.getWidth() < page.getImageDPI()) || ((grBounds.getHeight() * 2) < page.getImageDPI()))
				continue;
			
			//	sort out graphics that cannot accommodate 3 rows
			if (grBounds.getHeight() < (3 * docWordHeight))
				continue; 
			
			//	retain this one for further investigation
			pageGraphicsList.add(pageGraphics[g]);
		}
		if (pageGraphicsList.size() < pageGraphics.length)
			pageGraphics = ((Graphics[]) pageGraphicsList.toArray(new Graphics[pageGraphicsList.size()]));
		
		//	anything left to work with?
		if (pageGraphics.length == 0)
			return;
		
		//	sort graphics top-down
		pm.setInfo("Assessing potential full table grids in page " + page.pageId);
		Arrays.sort(pageGraphics, topDownGraphicsOrder);
		pm.setInfo(" - " + pageGraphics.length + " graphics sorted");
		
		//	assess potential table areas (all the graphics passing above filters are candidates for full grid tables)
		pm.setInfo(" - assessing " + pageGraphics.length + " potential table grids");
		ArrayList pageTableAreaCandidates = new ArrayList();
		for (int g = 0; g < pageGraphics.length; g++) {
			BoundingBox tableArea = pageGraphics[g].getBounds();
			pm.setInfo("   - assessing " + tableArea);
			TableAreaCandidate tac = getTableAreaCandidate(page, tableArea, docWordDensity1d, docWordDensity2d, docWordSpacing, docNormSpaceWidth, pm);
			if (tac != null)
				pageTableAreaCandidates.add(tac);
		}
		
		//	anything to work with?
		if (pageTableAreaCandidates.size() == 0)
			return;
		
		//	mark what we have
		pm.setInfo(" - retained " + pageTableAreaCandidates.size() + " candidate table grids:");
		for (int t = 0; t < pageTableAreaCandidates.size(); t++) {
			TableAreaCandidate tac = ((TableAreaCandidate) pageTableAreaCandidates.get(t));
			pm.setInfo("   - " + tac.bounds + ":");
			this.tableActionProvider.markTable(page, tac.bounds, tac.stats, null);
		}
	}
	
	private void detectDataSpanningOnePieceGridTables(ImPage page, Graphics[] pageGraphics, float docWordHeight, float docNormSpaceWidth, ProgressMonitor pm) {
		
		//	filter out lines
		ArrayList pageGraphicsList = new ArrayList();
		for (int g = 0; g < pageGraphics.length; g++) {
			
			//	sort out graphics too narrow or low for a table (less than 1 inch wide, or less than half inch tall)
			BoundingBox grBounds = pageGraphics[g].getBounds();
			if ((grBounds.getWidth() < page.getImageDPI()) || ((grBounds.getHeight() * 2) < page.getImageDPI()))
				continue;
			
			//	sort out graphics that cannot accommodate 2 data rows
			if (grBounds.getHeight() < (2 * docWordHeight))
				continue; 
			
			//	retain this one for further investigation
			pageGraphicsList.add(pageGraphics[g]);
		}
		if (pageGraphicsList.size() < pageGraphics.length)
			pageGraphics = ((Graphics[]) pageGraphicsList.toArray(new Graphics[pageGraphicsList.size()]));
		
		//	anything left to work with?
		if (pageGraphics.length == 0)
			return;
		
		//	TODO implement this (once we have an example)
		
		//	TODO find topmost horizontal line
		
		//	TODO expand upward if no words above topmost line (using row stripes in enclosing block)
		
		//	TODO do this for at most four row stripes (there should not be more rows in a table header), yielding one candidate each
		
		//	TODO find leftmost vertical line
		
		//	TODO expand to left block edge if no words on left of it
		//	TODO ... and then, always expand to block edge
		
		//	TODO assess resulting table candidate area(s)
	}
	
	private void detectHorizontalMultiPieceGridTables(ImPage page, Graphics[] pageGraphics, float docWordHeight, float docWordDensity1d, float docWordDensity2d, float docWordSpacing, float docNormSpaceWidth, ProgressMonitor pm) {
		
		//	anything to work with?
		if (pageGraphics.length < 2)
			return;
		
		//	filter out vertical lines
		ArrayList pageGraphicsList = new ArrayList();
		for (int g = 0; g < pageGraphics.length; g++) {
			
			//	sort out graphics too narrow for a table (less than 1 inch wide)
			BoundingBox grBounds = pageGraphics[g].getBounds();
			if (grBounds.getWidth() < page.getImageDPI())
				continue;
			
			//	retain this one for further investigation
			pageGraphicsList.add(pageGraphics[g]);
		}
		if (pageGraphicsList.size() < pageGraphics.length)
			pageGraphics = ((Graphics[]) pageGraphicsList.toArray(new Graphics[pageGraphicsList.size()]));
		
		//	anything left to work with?
		if (pageGraphics.length < 2)
			return;
		
		//	sort graphics top-down
		pm.setInfo("Assessing potential multi-piece table grids in page " + page.pageId);
		Arrays.sort(pageGraphics, topDownGraphicsOrder);
		pm.setInfo(" - " + pageGraphics.length + " graphics sorted");
		
		/* mark region as non-table area if
		 * (a) only two lines contained
		 * (b) lines more than half average word height apart (to account for double lines)
		 * (c) no words contained at all */
		ArrayList pageNonTableAreas = new ArrayList();
		for (int g = 0; g < pageGraphics.length; g++) {
			BoundingBox grBounds = pageGraphics[g].getBounds();
			pm.setInfo(" - seeking non-table areas starting with " + grBounds);
			
			//	combine with other graphics in same column
			for (int cg = (g+1); cg < pageGraphics.length; cg++) {
				BoundingBox cGrBounds = pageGraphics[cg].getBounds();
//				System.out.println("     - checking against " + cGraphBounds + " ...");
				
				//	check column overlap (require 97% to accommodate sloppy layout)
				int uWidth = (Math.max(grBounds.right, cGrBounds.right) - Math.min(grBounds.left, cGrBounds.left));
				int iWidth = (Math.min(grBounds.right, cGrBounds.right) - Math.max(grBounds.left, cGrBounds.left));
				if ((iWidth * 100) < (uWidth * 97)) {
//					System.out.println("       ==> alignment mismatch");
					continue;
				}
				
				//	check combined height
				int uHeight = (Math.max(grBounds.bottom, cGrBounds.bottom) - Math.min(grBounds.top, cGrBounds.top));
				if ((uHeight * 2) < docWordHeight) {
//					System.out.println("       ==> lower than half a word, potential double line");
					continue;
				}
				
				//	mark potential non-table area
				BoundingBox uGrBounds = new BoundingBox(
						Math.min(grBounds.left, cGrBounds.left),
						Math.max(grBounds.right, cGrBounds.right),
						Math.min(grBounds.top, cGrBounds.top),
						Math.max(grBounds.bottom, cGrBounds.bottom)
					);
				
				//	check for any contained graphics
				for (int og = (g+1); og < pageGraphics.length; og++) {
					if (og == cg)
						break; // no need to seek any further thanks to top-down sorting
					BoundingBox oGrBounds = pageGraphics[og].getBounds();
					
					//	check width (require 97% to accommodate sloppy layout)
					if ((oGrBounds.getWidth() * 100) < (uGrBounds.getWidth() * 97))
						continue;
					
					//	check overlap (having checked width before, fuzzy inclusion should do)
					if (!uGrBounds.includes(oGrBounds, true))
						continue;
					
					//	we can exclude this one
					uGrBounds = null;
//					System.out.println("       ==> contains " + oGraphBounds);
					break;
				}
				if (uGrBounds == null)
					continue;
				
				//	check for contained words
				ImWord[] uGraphWords = page.getWordsInside(uGrBounds);
				
				//	mark empty non-table area
				if (uGraphWords.length == 0) {
					pageNonTableAreas.add(uGrBounds);
					pm.setInfo("   - got void non-table area together with " + cGrBounds + ": " + uGrBounds);
					continue;
				}
				
				//	check for table caption
				ImUtils.sortLeftRightTopDown(uGraphWords);
				String uGraphStartWordString = uGraphWords[0].getString().toLowerCase();
				if (uGraphStartWordString.equals("t") || uGraphStartWordString.startsWith("tab")) {// need to catch word boundary due to font size change emulating small caps in some layouts
					String uGraphStartString = ImUtils.getString(uGraphWords[0], uGraphWords[Math.min(uGraphWords.length, 5) - 1], true);
					if (uGraphStartString.matches("(Tab|TAB)[^\\s]*\\.?\\s*[1-9][0-9]*.*")) {
						pageNonTableAreas.add(uGrBounds);
						pm.setInfo("   - got caption non-table area together with " + cGrBounds + ": " + uGrBounds);
						continue;
					}
				}
				
				//	TODO anything else to check?
			}
		}
		
		//	group graphics into potential table grids
		ArrayList pageTableAreas = new ArrayList();
		for (int g = 0; g < pageGraphics.length; g++) {
			BoundingBox grBounds = pageGraphics[g].getBounds();
			pm.setInfo(" - seeking table grids starting with " + grBounds);
			
			//	combine with other graphics in same column
			for (int cg = (g+1); cg < pageGraphics.length; cg++) {
				BoundingBox cGrBounds = pageGraphics[cg].getBounds();
				
				//	check column overlap (require 97% to accommodate sloppy layout)
				int uWidth = (Math.max(grBounds.right, cGrBounds.right) - Math.min(grBounds.left, cGrBounds.left));
				int iWidth = (Math.min(grBounds.right, cGrBounds.right) - Math.max(grBounds.left, cGrBounds.left));
				if ((iWidth * 100) < (uWidth * 97))
					continue;
				
				//	check combined height TODO figure out sensible minimum
				int uHeight = (Math.max(grBounds.bottom, cGrBounds.bottom) - Math.min(grBounds.top, cGrBounds.top));
				if ((uHeight * 2) < page.getImageDPI())
					continue;
				
				//	sort out graphics combinations that cannot accommodate 3 rows
				if (uHeight < (3 * docWordHeight))
					continue; 
				
				//	mark potential table bounds
				BoundingBox uGrBounds = new BoundingBox(
						Math.min(grBounds.left, cGrBounds.left),
						Math.max(grBounds.right, cGrBounds.right),
						Math.min(grBounds.top, cGrBounds.top),
						Math.max(grBounds.bottom, cGrBounds.bottom)
					);
				
				//	check if area includes any non-table areas
				for (int nta = 0; nta < pageNonTableAreas.size(); nta++) {
					BoundingBox ntaBounds = ((BoundingBox) pageNonTableAreas.get(nta));
					if (uGrBounds.includes(ntaBounds, true)) {
						uGrBounds = null;
						break;
					}
				}
				if (uGrBounds == null)
					continue;
				
				//	store potential table area
				pageTableAreas.add(uGrBounds);
				pm.setInfo("   - got potential (partial) table grid together with " + cGrBounds + ": " + uGrBounds);
			}
		}
		
		//	anything left to work with?
		if (pageTableAreas.size() == 0)
			return;
		
		//	order potential table areas by descending size (makes sure to find whole tables before partial ones)
		Collections.sort(pageTableAreas, descendingBoxSizeOrder);
		
		//	assess potential table areas
		pm.setInfo(" - assessing " + pageTableAreas.size() + " potential table grids");
		ArrayList pageTableAreaCandidates = new ArrayList();
		for (int t = 0; t < pageTableAreas.size(); t++) {
			BoundingBox tableArea = ((BoundingBox) pageTableAreas.get(t));
			pm.setInfo("   - assessing " + tableArea);
			TableAreaCandidate tac = getTableAreaCandidate(page, tableArea, docWordDensity1d, docWordDensity2d, docWordSpacing, docNormSpaceWidth, pm);
			if (tac != null)
				pageTableAreaCandidates.add(tac);
		}
		
		//	anything to work with?
		if (pageTableAreaCandidates.size() == 0)
			return;
		
		//	filter table candidates contained in others and having no more columns than containing one
		pm.setInfo(" - filtering " + pageTableAreaCandidates.size() + " candidate table grids");
		for (int t = 0; t < pageTableAreaCandidates.size(); t++) {
			TableAreaCandidate tac = ((TableAreaCandidate) pageTableAreaCandidates.get(t));
			for (int ct = (t+1); ct < pageTableAreaCandidates.size(); ct++) {
				TableAreaCandidate cTac = ((TableAreaCandidate) pageTableAreaCandidates.get(ct));
				if ((cTac.cols.length <= tac.cols.length) && cTac.bounds.liesIn(tac.bounds, false)) {
					pageTableAreaCandidates.remove(ct--);
					pm.setInfo("   - removed " + cTac.bounds + " as contained (1) in " + tac.bounds);
				}
			}
		}
		
		//	filter table candidates containing two or more others with more columns than they have themselves (helps sort out stacked tables with partially compatible column gaps marked as one)
		for (int t = 0; t < pageTableAreaCandidates.size(); t++) {
			TableAreaCandidate tac = ((TableAreaCandidate) pageTableAreaCandidates.get(t));
			ArrayList subTableAreaCandidates = new ArrayList();
			for (int ct = (t+1); ct < pageTableAreaCandidates.size(); ct++) {
				TableAreaCandidate cTac = ((TableAreaCandidate) pageTableAreaCandidates.get(ct));
				if ((cTac.cols.length > tac.cols.length) && cTac.bounds.liesIn(tac.bounds, false))
					subTableAreaCandidates.add(cTac);
			}
			if (subTableAreaCandidates.size() < 2)
				continue;
			for (int ct = 0; ct < subTableAreaCandidates.size(); ct++) {
				TableAreaCandidate cTac = ((TableAreaCandidate) subTableAreaCandidates.get(ct));
				for (int cct = (ct+1); cct < subTableAreaCandidates.size(); cct++) {
					TableAreaCandidate ccTac = ((TableAreaCandidate) subTableAreaCandidates.get(cct));
					if (ccTac.bounds.overlaps(cTac.bounds))
						subTableAreaCandidates.remove(cct--);
				}
			}
			if (subTableAreaCandidates.size() < 2)
				continue;
			
			pm.setInfo("   - checking columns in " + tac.bounds + " against sub candidates");
			for (int ct = 0; ct < subTableAreaCandidates.size(); ct++) {
				TableAreaCandidate cTac = ((TableAreaCandidate) subTableAreaCandidates.get(ct));
				pm.setInfo("     - candidate " + cTac.bounds);
				int colFlankMatchCount = 0;
				int flankMatchColCount = 0;
				for (int cc = 0; cc < cTac.cols.length; cc++) {
					pm.setInfo("       - " + cTac.cols[cc].bounds);
					for (int c = 0; c < tac.cols.length; c++) {
						if (tac.cols[c].bounds.right <= cTac.cols[cc].bounds.left)
							continue;
						if (cTac.cols[cc].bounds.right <= tac.cols[c].bounds.left)
							pm.setInfo("       ==> no match");
						else {
							pm.setInfo("       ==> overlaps with " + tac.cols[c].bounds);
							boolean flankMatch = false;
							if (cTac.cols[cc].bounds.left == tac.cols[c].bounds.left) {
								pm.setInfo("       ==> left flank match");
								colFlankMatchCount++;
								flankMatch = true;
							}
							if (cTac.cols[cc].bounds.right == tac.cols[c].bounds.right) {
								pm.setInfo("       ==> right flank match");
								colFlankMatchCount++;
								flankMatch = true;
							}
							if (flankMatch)
								flankMatchColCount++;
						}
						break;
					}
				}
				pm.setInfo("     ==> got " + colFlankMatchCount + " matching column flanks of " + (tac.cols.length * 2) + " / " + (cTac.cols.length * 2));
				pm.setInfo("     ==> got " + flankMatchColCount + " columns with matching flanks of " + tac.cols.length + " / " + cTac.cols.length);
			}
			
			pageTableAreaCandidates.remove(t--);
			pm.setInfo("   - removed " + tac.bounds + " as likely conflation of " + subTableAreaCandidates.size() + " better candidates:");
			for (int s = 0; s < subTableAreaCandidates.size(); s++) {
				TableAreaCandidate sTac = ((TableAreaCandidate) subTableAreaCandidates.get(s));
				pm.setInfo("     - " + sTac.bounds);
			}
		}
//		
//		//	filter table candidates overlapping two or more others with at least as many columns as they have themselves, and lower column gap oddity (helps sort out stacked tables with partially compatible column gaps marked as one)
//		for (int t = 0; t < pageTableAreaCandidates.size(); t++) {
//			TableAreaCandidate tac = ((TableAreaCandidate) pageTableAreaCandidates.get(t));
//			ArrayList subTableCands = new ArrayList();
//			for (int ct = (t+1); ct < pageTableAreaCandidates.size(); ct++) {
//				TableAreaCandidate cTac = ((TableAreaCandidate) pageTableAreaCandidates.get(ct));
//				if ((cTac.cols.length >= tac.cols.length) && (cTac.getColumnGapOddity() < tac.getColumnGapOddity()) && cTac.bounds.overlaps(tac.bounds))
//					subTableCands.add(cTac);
//			}
//			if (subTableCands.size() > 1) {
//				pageTableAreaCandidates.remove(t--);
//				System.out.println("   - removed " + tac.bounds + " as overlapping with " + subTableCands.size() + " better candidates:");
//				for (int s = 0; s < subTableCands.size(); s++) {
//					TableAreaCandidate subTableCand = ((TableAreaCandidate) subTableCands.get(s));
//					System.out.println("     - " + subTableCand.bounds);
//				}
//			}
//		}
		
		//	filter table candidates contained in others, regardless of number of columns (must not nest tables)
		pm.setInfo(" - filtering " + pageTableAreaCandidates.size() + " candidate table grids");
		for (int t = 0; t < pageTableAreaCandidates.size(); t++) {
			TableAreaCandidate tac = ((TableAreaCandidate) pageTableAreaCandidates.get(t));
			for (int ct = (t+1); ct < pageTableAreaCandidates.size(); ct++) {
				TableAreaCandidate cTac = ((TableAreaCandidate) pageTableAreaCandidates.get(ct));
				if (cTac.bounds.liesIn(tac.bounds, false)) {
					pageTableAreaCandidates.remove(ct--);
					pm.setInfo("   - removed " + cTac.bounds + " as contained (2) in " + tac.bounds);
				}
			}
		}
		
		//	mark what we have
		pm.setInfo(" - retained " + pageTableAreaCandidates.size() + " candidate table grids:");
		for (int t = 0; t < pageTableAreaCandidates.size(); t++) {
			TableAreaCandidate tac = ((TableAreaCandidate) pageTableAreaCandidates.get(t));
			pm.setInfo("   - " + tac.bounds + ":");
			this.tableActionProvider.markTable(page, tac.bounds, tac.stats, null);
		}
	}
	
	private void detectVerticalMultiPieceGridTables(ImPage page, Graphics[] pageGraphics, float docWordHeight, float docNormSpaceWidth, ProgressMonitor pm) {
		
		//	anything to work with?
		if (pageGraphics.length < 2)
			return;
		
		//	filter out horizontal lines
		ArrayList pageGraphicsList = new ArrayList();
		for (int g = 0; g < pageGraphics.length; g++) {
			
			//	sort out graphics too low for a table (less than half inch tall)
			BoundingBox grBounds = pageGraphics[g].getBounds();
			if ((grBounds.getHeight() * 2) < page.getImageDPI())
				continue;
			
			//	retain this one for further investigation
			pageGraphicsList.add(pageGraphics[g]);
		}
		if (pageGraphicsList.size() < pageGraphics.length)
			pageGraphics = ((Graphics[]) pageGraphicsList.toArray(new Graphics[pageGraphicsList.size()]));
		
		//	anything left to work with?
		if (pageGraphics.length < 2)
			return;
		
		//	TODO implement this (once we have an example)
		
		//	TODO find horizontally aligned vertical lines
		
		//	TODO try and span tables between them
	}
	
	private void detectHorizontalSingleLineGridTables(ImPage page, Graphics[] pageGraphics, float docWordHeight, float docNormSpaceWidth, ProgressMonitor pm) {
		
		//	filter out vertical lines
		ArrayList pageGraphicsList = new ArrayList();
		for (int g = 0; g < pageGraphics.length; g++) {
			
			//	sort out graphics too narrow for a table (less than 1 inch wide)
			BoundingBox grBounds = pageGraphics[g].getBounds();
			if (grBounds.getWidth() < page.getImageDPI())
				continue;
			
			//	retain this one for further investigation
			pageGraphicsList.add(pageGraphics[g]);
		}
		if (pageGraphicsList.size() < pageGraphics.length)
			pageGraphics = ((Graphics[]) pageGraphicsList.toArray(new Graphics[pageGraphicsList.size()]));
		
		//	anything left to work with?
		if (pageGraphics.length == 0)
			return;
		
		//	TODO implement this
		
		//	TODO get blocks enclosing lines
		
		//	TODO make sure not to act upon table previously marked by other methods
		
		//	TODO get block row stripes
		
		/* TODO expand outward from lines:
		 * - at least one row stripe above line
		 * - at least two row stripes below line
		 * - expand as long as density remains low
		 */
	}
	
	private void detectVerticalSingleLineGridTables(ImPage page, Graphics[] pageGraphics, float docWordHeight, float docNormSpaceWidth, ProgressMonitor pm) {
		
		//	filter out horizontal lines
		ArrayList pageGraphicsList = new ArrayList();
		for (int g = 0; g < pageGraphics.length; g++) {
			
			//	sort out graphics too low for a table (less than half inch tall)
			BoundingBox grBounds = pageGraphics[g].getBounds();
			if ((grBounds.getHeight() * 2) < page.getImageDPI())
				continue;
			
			//	retain this one for further investigation
			pageGraphicsList.add(pageGraphics[g]);
		}
		if (pageGraphicsList.size() < pageGraphics.length)
			pageGraphics = ((Graphics[]) pageGraphicsList.toArray(new Graphics[pageGraphicsList.size()]));
		
		//	anything left to work with?
		if (pageGraphics.length == 0)
			return;
		
		//	TODO implement this (once we have an example)
		
		//	TODO find blocks enclosing lines
		
		//	TODO try and mark tables to height of line and horizontal extent of block
	}
	
	private static RowStripe[] getRowStripes(ImPage page, BoundingBox regionBounds) {
		
		//	get and sort words (top-down order assures proper row list carrying in stripe identification)
		ImWord[] words = page.getWordsInside(regionBounds);
		Arrays.sort(words, ImUtils.topDownOrder);
		
		//	find horizontal stripes (can comprise multiple lines)
		ArrayList[] stripeWordLists = new ArrayList[regionBounds.bottom - regionBounds.top];
		Arrays.fill(stripeWordLists, null);
		for (int w = 0; w < words.length; w++) {
			if (ImWord.TEXT_STREAM_TYPE_DELETED.equals(words[w].getTextStreamType())) {}
			else if (ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(words[w].getTextStreamType())) {}
			else if (ImWord.TEXT_STREAM_TYPE_PAGE_TITLE.equals(words[w].getTextStreamType())) {}
			else for (int r = Math.max(regionBounds.top, words[w].bounds.top); r < Math.min(regionBounds.bottom, words[w].bounds.bottom); r++) {
				if ((r == regionBounds.top) || (stripeWordLists[r - regionBounds.top - 1] == null))
					stripeWordLists[r - regionBounds.top] = new ArrayList();
				else stripeWordLists[r - regionBounds.top] = stripeWordLists[r - regionBounds.top - 1];
			}
		}
		
		//	sort words into stripes
		for (int w = 0; w < words.length; w++) {
			if (ImWord.TEXT_STREAM_TYPE_DELETED.equals(words[w].getTextStreamType())) {}
			else if (ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(words[w].getTextStreamType())) {}
			else if (ImWord.TEXT_STREAM_TYPE_PAGE_TITLE.equals(words[w].getTextStreamType())) {}
			else if (words[w].bounds.getHeight() != 0) // stripe word list can be null for height 0 (OCR artifacts !!!)
				stripeWordLists[Math.max(regionBounds.top, words[w].bounds.top) - regionBounds.top].add(words[w]);
		}
		
		//	generate stripes
		ArrayList stripeList = new ArrayList();
		for (int r = 0; r < stripeWordLists.length; r++) {
			if (stripeWordLists[r] == null)
				continue;
			if (stripeWordLists[r].isEmpty())
				continue;
			if ((r != 0) && (stripeWordLists[r] == stripeWordLists[r-1]))
				continue;
			stripeList.add(new RowStripe((ImWord[]) stripeWordLists[r].toArray(new ImWord[stripeWordLists[r].size()])));
		}
		return ((RowStripe[]) stripeList.toArray(new RowStripe[stripeList.size()]));
	}
	
	private static class RowStripe {
		final ImWord[] words;
		final int wordWidthSum;
		final int wordHeightSum;
		final int wordArea;
		final int wordDistanceCount;
		final int minWordDistance;
		final int maxWordDistance;
		final int wordDistanceSum;
		final int wordDistanceSquareSum;
		final float nMinWordDistance;
		final float nMaxWordDistance;
		final float nWordDistanceSum;
		final float nWordDistanceSquareSum;
		final BoundingBox bounds;
		final int area;
		final boolean[] colHasWord;
		final char[] colType;
		int colCount = 0;
//		float aSharedWordCols = 0;
//		float aSharedNonWordCols = 0;
//		float bSharedWordCols = 0;
//		float bSharedNonWordCols = 0;
		float sharedWordCols = 0;
		float sharedNonWordCols = 0;
//		float aFwSharedWordCols = 0;
//		float aFwSharedNonWordCols = 0;
		int aFwMaxSeqSharedNonWordCols = 0;
		float aFwAvgSeqSharedNonWordCols = 0;
		float aFwMatchTypeCols = 0;
//		float bFwSharedWordCols = 0;
//		float bFwSharedNonWordCols = 0;
		int bFwMaxSeqSharedNonWordCols = 0;
		float bFwAvgSeqSharedNonWordCols = 0;
		float bFwMatchTypeCols = 0;
		int aSpace = Short.MAX_VALUE; // not using Integer.MAX_VALUE to prevent having to handle arithmetic overflows
		int bSpace = Short.MAX_VALUE; // not using Integer.MAX_VALUE to prevent having to handle arithmetic overflows
		boolean isCaptionOrNote = false;
		RowStripe(ImWord[] words) {
			Arrays.sort(words, ImUtils.leftRightOrder);
			this.words = words;
			int wordArea = 0;
			int wordWidthSum = 0;
			int wordHeightSum = 0; // using average word height as proxy for both font size and DPI
			int wordDistanceCount = 0;
			int minWordDistance = Integer.MAX_VALUE;
			int maxWordDistance = 0;
			int wordDistanceSum = 0;
			int wordDistanceSquareSum = 0;
			for (int w = 0; w < this.words.length; w++) {
				wordArea += getArea(this.words[w]);
				wordWidthSum += (this.words[w].bounds.right - this.words[w].bounds.left);
				wordHeightSum += (this.words[w].bounds.bottom - this.words[w].bounds.top);
				if (w != 0) {
					int wordDistance = (this.words[w].bounds.left - this.words[w-1].bounds.right);
					if (wordDistance > 0) {
						wordDistanceCount++;
						minWordDistance = Math.min(minWordDistance, wordDistance);
						maxWordDistance = Math.max(maxWordDistance, wordDistance);
						wordDistanceSum += wordDistance;
						wordDistanceSquareSum += (wordDistance * wordDistance);
					}
				}
			}
			
			//	compute overall word stats
			this.wordWidthSum = wordWidthSum;
			this.wordHeightSum = wordHeightSum;
			this.wordArea = wordArea;
			this.wordDistanceCount = wordDistanceCount;
			this.minWordDistance = ((minWordDistance < Integer.MAX_VALUE) ? minWordDistance : -1);
			this.maxWordDistance = ((maxWordDistance > 0) ? maxWordDistance : -1);
			this.wordDistanceSum = wordDistanceSum;
			this.wordDistanceSquareSum = wordDistanceSquareSum;
			this.nMinWordDistance = ((minWordDistance < Integer.MAX_VALUE) ? (((float) (minWordDistance * this.words.length)) / wordHeightSum) : -1);
			this.nMaxWordDistance = ((maxWordDistance > 0) ? (((float) (maxWordDistance * this.words.length)) / wordHeightSum) : -1);
			this.nWordDistanceSum = (((float) (wordDistanceSum * this.words.length)) / wordHeightSum);
			this.nWordDistanceSquareSum = (((float) (wordDistanceSquareSum * this.words.length * this.words.length)) / (wordHeightSum * wordHeightSum));
			
			//	compute own stats
			this.bounds = ImLayoutObject.getAggregateBox(this.words);
			this.area = getArea(this.bounds);
			
			//	check where words are
			this.colHasWord = new boolean[this.bounds.right - this.bounds.left];
			Arrays.fill(this.colHasWord, false);
			this.colType = new char[this.bounds.right - this.bounds.left];
			Arrays.fill(this.colType, ' '); // initialize to space
			for (int w = 0; w < this.words.length; w++) {
				final char wordType;
				if (Gamta.isRomanNumber(this.words[w].getString()))
					wordType = 'R'; // Roman numeral
				else if (Gamta.isNumber(this.words[w].getString()))
					wordType = 'A'; // Arabic numeral
				else if (this.words[w].getString().length() == 1) {
					if (":.;,/|".indexOf(this.words[w].getString()) != -1)
						wordType = 'S'; // separating punctuation (more common _outside_ tables !!!)
					else if ("?".indexOf(this.words[w].getString()) != -1)
						wordType = 'W'; // wildcard (can occur to indicate an unknown value)
					else if ("-".indexOf(this.words[w].getString()) != -1)
						wordType = 'B'; // blank (can occur to indicate a non-existing value)
					else if ("+".indexOf(this.words[w].getString()) != -1)
						wordType = 'B'; // boolean (need to conflate with 'blank' for +/- columns)
					else if (!Character.isLetter(this.words[w].getString().charAt(0)))
						wordType = 'P'; // general punctuation mark (only need exclude letters, as numbers would have been caught above)
					else wordType = 'T'; // default to text
				}
				else wordType = 'T'; // default to text
				for (int c = this.words[w].bounds.left; c < this.words[w].bounds.right; c++) {
					this.colHasWord[c - this.bounds.left] = true;
					this.colType[c - this.bounds.left] = wordType;
				}
			}
		}
		float getWordDensity() {
//			return ((this.area < 1) ? 0 : (((float) this.wordArea) / this.area));
			//	use horizontal density only to counter effect of superscript or subscript characters expanding row height
			return ((this.bounds.left < this.bounds.right) ? (((float) this.wordWidthSum) / this.bounds.getWidth()) : 0);
		}
		float getWordSpacing() {
//			return ((this.words.length < 2) ? 0 : (((float) this.wordDistanceSum) / (this.words.length - 1)));
			return ((this.wordDistanceCount < 1) ? 0 : (((float) this.wordDistanceSum) / this.wordDistanceCount));
		}
		float getSquareWordSpacing() {
//			return ((this.words.length < 2) ? 0 : (((float) this.wordDistanceSquareSum) / (this.words.length - 1)));
			return ((this.wordDistanceCount < 1) ? 0 : (((float) this.wordDistanceSquareSum) / this.wordDistanceCount));
		}
		float getNormWordSpacing() {
//			return ((this.words.length < 2) ? 0 : (((float) this.nWordDistanceSum) / (this.words.length - 1)));
			return ((this.wordDistanceCount < 1) ? 0 : (((float) this.nWordDistanceSum) / this.wordDistanceCount));
		}
		float getNormSquareWordSpacing() {
//			return ((this.words.length < 2) ? 0 : (((float) this.nWordDistanceSquareSum) / (this.words.length - 1)));
			return ((this.wordDistanceCount < 1) ? 0 : (((float) this.nWordDistanceSquareSum) / this.wordDistanceCount));
		}
		float getWordSpacingQuotient() {
			return ((this.wordDistanceSum < 1) ? 0 : (((float) this.wordDistanceSquareSum) / this.wordDistanceSum));
		}
		int findWordMatching(String pattern) {
			for (int w = 0; w < this.words.length; w++) {
				if (this.words[w].getString().matches(pattern))
					return w;
			}
			return -1;
		}
	}
	
	private static int getArea(ImLayoutObject ilo) {
		return getArea(ilo.bounds);
	}
	
	private static int getArea(BoundingBox bounds) {
		return ((bounds.right - bounds.left) * (bounds.bottom - bounds.top));
	}
	
	/*
When marking a table, keep using current approach of optimizing column count and column gaps ...
... but add second step:
- compute word density and average space widths in columns ...
- ... and split up columns whose values are way above average and have internal low-whitespace areas
==> should also help with column detection in the presence of multi-column cells
- also split up columns that have internal whitespace gaps at least 67% as wide as table average
==> should help with wide (as well as misplaced, see that Zootaxa with the five-page table appendix) column headers
- ultimately, word density in table _cells_ should be in vicinity (most likely north of) document average
==> use the latter for splitting up columns, also in the presence of multi-column cells
	 */
	
	private static class TableAreaCandidate {
		final BoundingBox bounds;
		final ImWord[] words;
		final TableAreaStripe[] rows;
		final ImRegion[] cols;
		final float colGapFract;
		final TableAreaStatistics stats;
		TableAreaCandidate(TableAreaStripe[] rows, ImRegion[] cols, TableAreaStatistics stats) {
			this.rows = rows;
			this.cols = cols;
			this.stats = stats;
			this.bounds = this.stats.wordBounds;
			this.words = this.stats.words;
			this.colGapFract = this.stats.getBrgColOccupationLowFractReduced();
		}
	}
	
	private static TableAreaCandidate getTableAreaCandidate(ImPage page, BoundingBox tableArea, float docWordDensity1d, float docWordDensity2d, float docWordSpacing, float docNormSpaceWidth, ProgressMonitor pm) {
		if ((aimAtPage != -1) && (page.pageId != aimAtPage))
			return null;
		
		//	make sure to (a) wrap table area tightly around contained words, but (b) not to cut off any words
		ImWord[] areaWords;
		BoundingBox wTableArea;
		while (true) {
			areaWords = page.getWordsInside(tableArea);
			wTableArea = ImLayoutObject.getAggregateBox(areaWords);
			if (wTableArea == null) {
				pm.setInfo("   ==> cannot build table without words");
				return null;
			}
			else if (tableArea.equals(wTableArea))
				break;
			else {
				tableArea = wTableArea;
				pm.setInfo("   - reduced to " + tableArea);
			}
		}
		
		//	check for any table words to prevent marking tables twice (from more than one detector method)
		for (int w = 0; w < areaWords.length; w++)
			if (ImWord.TEXT_STREAM_TYPE_TABLE.equals(areaWords[w].getTextStreamType())) {
				pm.setInfo("   ==> cannot build table inside table");
				return null;
			}
		
		//	compute average word height
		int tableAreaWordHeightSum = 0;
		for (int w = 0; w < areaWords.length; w++)
			tableAreaWordHeightSum += areaWords[w].bounds.getHeight();
		int tableAreaWordHeight = ((areaWords.length == 0) ? 0 : ((tableAreaWordHeightSum + (areaWords.length / 2)) / areaWords.length));
		pm.setInfo("   - average word height is " + tableAreaWordHeight);
		
		//	TODO test horizontal stripe grid table (with white-background headers) in Pages 4, 5, 6 of journal.pone.0149556.PDF.pdf
		//	TODO prevent inclusion of table notes residing right below tables
		//	==> TODO reject table candidates whose last row has only bridged gaps
		//	==> TODO also consider drop in word height or font size (table notes do tend to be smaller, actual rows barely ever do so)
		//	==> TODO consider introducing table font size style parameter
		
		
		//	TODOne prevent marking overlapping tables !!!
		//	TODO example: EJT 404 / IMF FFD9CE63FF9BF752FF85FF922711FFB9 (EJT-testbed/197)
		
		//	TODOne double-check area peak vs. stripe grid assessment ==> corrected error in stripe flank computation
		//	TODOne TEST ejt-585_ebersole_cicimurri_stinger.pdf.imd (page 6)
		
		//	compute table area statistics
//		TableAreaStatistics areaStats = new TableAreaStatistics(areaWords, normSpaceWidth);
		TableAreaStatistics areaStats = TableAreaStatistics.getTableAreaStatistics(page, areaWords, docNormSpaceWidth);
		
		//	get horizontal stripes and density
		TableAreaStripe[] areaStripes = getTableAreaStripes(page, areaStats);
		pm.setInfo("   - got " + areaStripes.length + " stripes:");
		for (int r = 0; r < areaStripes.length; r++)
			pm.setInfo("     - " + areaStripes[r].bounds + ": " + areaStripes[r].words[0].getString());
		if (areaStripes.length < 3) {
			pm.setInfo("   ==> cannot build table with fewer than three rows");
			return null;
		}
		
		//	compute area density
		int stripeWidthSum = 0;
		int stripeArea = 0;
		int wordWidthSum = 0;
		int wordHeightSum = 0;
		int wordCount = 0;
		int wordArea = 0;
		int wordDistanceSum = 0;
		float nWordDistanceSum = 0;
		int wordDistanceCount = 0;
		for (int s = 0; s < areaStripes.length; s++) {
			stripeWidthSum += areaStripes[s].bounds.getWidth();
			stripeArea += areaStripes[s].area;
			wordWidthSum += areaStripes[s].wordWidthSum;
			wordHeightSum += areaStripes[s].wordHeightSum;
			wordCount += areaStripes[s].words.length;
			wordArea += areaStripes[s].wordArea;
			wordDistanceSum += areaStripes[s].wordDistanceSum;
			nWordDistanceSum += areaStripes[s].nWordDistanceSum;
			wordDistanceCount += areaStripes[s].wordDistanceCount;
		}
		float areaWordDensity1d = ((stripeWidthSum < 1) ? 0 : (((float) wordWidthSum) / stripeWidthSum));
		float areaWordDensity2d = ((stripeArea < 1) ? 0 : (((float) wordArea) / stripeArea));
		System.out.println(" - average word density is " + areaWordDensity1d + " horizonatally"  + " (document: " + docWordDensity1d + ")" + ", " + areaWordDensity2d + " by area (document: " + docWordDensity2d + ")");
		float areaWordSpacing = ((wordDistanceCount < 1) ? 0 : (((float) wordDistanceSum) / wordDistanceCount));
		System.out.println(" - average word spacing " + areaWordSpacing  + " (document: " + docWordSpacing + ")");
		final float areaNormSpaceWidth = ((wordDistanceCount < 1) ? 0 : (((float) nWordDistanceSum) / wordDistanceCount));
		System.out.println(" - average normalized word spacing " + areaNormSpaceWidth + " (document: " + docNormSpaceWidth + ")");
		final float areaWordHeight = ((wordCount < 1) ? 0 : (((float) wordHeightSum) / wordCount));
		System.out.println(" - average word height " + areaWordHeight);
		
		//	also get area stats with area normalized space width
		/* Rationale: if increasing normalized space width increases column
		 * gaps, we do have actual column gaps, if with a few obstructions that
		 * end up bridged with the wider normalized space. On the other hand,
		 * if we already have relatively wide column gaps, local normalized
		 * space width is so large that applying it will decrease column gap
		 * fraction. Lastly, if the wider spaces driving up local normalized
		 * space width do not align (i.e., we don't have a table), applying the
		 * increased normalized space width will not find any column gaps, so
		 * we don't really take the risk of incurring false positives. */
		TableAreaStatistics localAreaStats = TableAreaStatistics.getTableAreaStatistics(page, areaWords, areaNormSpaceWidth);
		
		//	we need a good few core lows wider than average word spacing (i.e., filtered) to have any chance of a good column split
		System.out.println("   - filtered bridged lows are " + areaStats.getBrgColOccupationLowsFiltered());
		System.out.println("   - local filtered bridged lows are " + localAreaStats.getBrgColOccupationLowsFiltered());
		float bColPixelLowFractFiltered = areaStats.getBrgColOccupationLowFractFiltered();
		float bLocalColPixelLowFractFiltered = localAreaStats.getBrgColOccupationLowFractFiltered();
		System.out.println("   - column gap fractions are " + bColPixelLowFractFiltered + " and local " + bLocalColPixelLowFractFiltered);
		if (0.05 <= bColPixelLowFractFiltered) {}
		else if (((docNormSpaceWidth * 2) <= areaNormSpaceWidth) && (0.05 <= bLocalColPixelLowFractFiltered)) {}
		else {
			pm.setInfo("   ==> cannot build table with less than 5% column gaps");
			return null;
		}
		
		//	we still need a good few core lows wider than average word spacing (i.e., filtered) to have any chance of a good column split
		System.out.println("   - reduced bridged lows are " + areaStats.getBrgColOccupationLowsReduced());
		System.out.println("   - local reduced bridged lows are " + localAreaStats.getBrgColOccupationLowsReduced());
		float bColPixelLowFractReduced = areaStats.getBrgColOccupationLowFractReduced();
		float bLocalColPixelLowFractReduced = localAreaStats.getBrgColOccupationLowFractReduced();
		System.out.println("   - reduced column gap fractions are " + bColPixelLowFractReduced + " and local " + bLocalColPixelLowFractReduced);
		if (0.05 <= bColPixelLowFractReduced) {}
		else if (((docNormSpaceWidth * 2) <= areaNormSpaceWidth) && (0.05 <= bLocalColPixelLowFractReduced)) {}
		else {
			pm.setInfo("   ==> cannot build table with less than 5% core column gaps");
			return null;
		}
		
		//	get column gaps, and count out graphics supported ones
		TreeSet colOccupationLows = new TreeSet(areaStats.getBrgColOccupationLowsReduced());
		int blockedColMarginCount = 0;
		int peakBlockedColMarginCount = 0;
		int flankBlockedColMarginCount = 0;
		for (Iterator colit = colOccupationLows.iterator(); colit.hasNext();) {
			ColumnOccupationLow col = ((ColumnOccupationLow) colit.next());
			if (col.isBlockedByPeak())
				peakBlockedColMarginCount++;
			if (col.isBlockedByFlank())
				flankBlockedColMarginCount++;
			if (col.isBlockedByGraphics())
				blockedColMarginCount++;
		}
		
		/* If we have flanks, exclude all non-zero, non-flank column gaps that
		 * lie inside some vertical stripe (i.e., are non-white over more than
		 * 80% table height).
		 * Rationale:
		 * - In a vertical stripe grid (i.e., flank column gaps), we should have
		 *   flanks in _all_ actual column gaps.
		 * - With highlighting stripes only under column headers and row labels,
		 *   column gaps won't be filled to over 80%.
		 * - In a horizontal stripe grid, even if we happen to have some flank,
		 *   column gaps still won't be filled to over 80%. */
		if (flankBlockedColMarginCount != 0)
			for (Iterator colit = colOccupationLows.iterator(); colit.hasNext();) {
				ColumnOccupationLow col = ((ColumnOccupationLow) colit.next());
				if (col.isBlockedByGraphics())
					continue;
				if (col.max == 0)
					continue;
				GraphicsSlice colGs = areaStats.getColGraphicsStats(col.left, col.right);
				if ((tableArea.getHeight() * 4) < (colGs.minOccupiedPixels * 5))
					colit.remove();
			}
		
		//	if we only have a single column gap (i.e., two columns):
		if (colOccupationLows.size() == 1) {
			ColumnOccupationLow col = ((ColumnOccupationLow) colOccupationLows.iterator().next());
			
			//	require large gap (say, at least twice average word height)
			if (col.getWidth() < (areaStats.avgWordHeight * 2)) {
				pm.setInfo("   ==> cannot build two-column table with all too narrow column gap");
				return null;
			}
			
			//	require said gap to include middle of table area
			int tableAreaCenterX = ((tableArea.left + tableArea.right) / 2);
			if ((tableAreaCenterX <= col.left) || (col.right <= tableAreaCenterX)) {
				pm.setInfo("   ==> cannot build two-column table with off-center column gap");
				return null;
			}
		}
		
		/* TODO also call mis-match if maximum gap is more than, say, five times average gap
		 * ==> super large gap most likely due to short headers over long-valued column 
		 * ==> likely got largish heading section only, no good for table by itself */
		
		//	check individual stripes for table caption (non-table area detection only catches captions boxed in by themselves)
		String captionStartString = null;
		for (int r = 0; r < areaStripes.length; r++) {
			String stripeStartWordString = areaStripes[r].words[0].getString().toLowerCase();
			if (ImWord.TEXT_STREAM_TYPE_CAPTION.equals(areaStripes[r].words[0].getTextStreamType())) {}
			else if (stripeStartWordString.equals("t")) {} // need to catch word boundary due to font size change emulating small caps in some layouts
			else if (stripeStartWordString.startsWith("tab")) {}
			else if (stripeStartWordString.equals("a")) {} // need to catch word boundary due to font size change emulating small caps in some layouts
			else if (stripeStartWordString.startsWith("appendi")) {}
			else continue;
			String stripeStartString = ImUtils.getString(areaStripes[r].words[0], areaStripes[r].words[Math.min(areaStripes[r].words.length, 5) - 1], true);
			if (stripeStartString.matches("(Tab|TAB)[^\\s]*\\.?\\s*[1-9][0-9]*.*")) {
				tableArea = null;
				captionStartString = stripeStartString;
				break;
			}
			else if (stripeStartString.matches("(Appendi|APPENDI)[^\\s]*\\.?\\s*[1-9][0-9]*.*")) {
				tableArea = null;
				captionStartString = stripeStartString;
				break;
			}
			else if (ImWord.TEXT_STREAM_TYPE_CAPTION.equals(areaStripes[r].words[0].getTextStreamType())) {
				tableArea = null;
				captionStartString = stripeStartString;
				break;
			}
		}
		if (tableArea == null) {
			pm.setInfo("   ==> cannot span table across caption '" + captionStartString + "'");
			return null;
		}
		
		//	collect words overlapping with lows to ignore them on column splitting
		HashSet multiColumnCellWords = new HashSet();
		for (Iterator colit = colOccupationLows.iterator(); colit.hasNext();) {
			ColumnOccupationLow col = ((ColumnOccupationLow) colit.next());
			for (int w = 0; w < areaWords.length; w++) {
				if (areaWords[w].bounds.right <= col.left)
					continue;
				if (col.right <= areaWords[w].bounds.left)
					continue;
				multiColumnCellWords.add(areaWords[w]);
			}
		}
		
		//	get total width of column occupation lows
		int colOccupationLowWidthSum = 0;
		for (Iterator colit = colOccupationLows.iterator(); colit.hasNext();)
			colOccupationLowWidthSum += ((ColumnOccupationLow) colit.next()).getWidth();
		
		//	get speculative columns TODO mind row group labels spanning whole table width !!! ==> above lows have to do !!!
		//	rationale: if we actually have a table, we should be able to distinguish at least _several_ columns
		ImRegion areaTestTable = new ImRegion(page.getDocument(), page.pageId, tableArea, ImRegion.TABLE_TYPE);
//		ImRegion[] areaTestCols = ImUtils.createTableColumns(areaTestTable, multiColumnCellWords, (areaStats.avgWordHeight / 3), (areaStats.getBrgColOccupationLowsReduced().size() + 1));
		ImRegion[] areaTestCols = null;
		int minAreaTestColCount = (areaStats.getBrgColOccupationLowsReduced().size() + 1);
		while (areaTestCols == null) {
//			TODO figure out if we need these thresholds, or if below width sum check is sufficiently safe
//			if ((minAreaTestColCount * 3) < ((areaStats.getBrgColOccupationLowsReduced().size() + 1) * 2))
//				break;
//			if (minAreaTestColCount < ((areaStats.getBrgColOccupationLowsReduced().size() + 1) - 2))
//				break;
			areaTestCols = ImUtils.createTableColumns(areaTestTable, multiColumnCellWords, (areaStats.avgWordHeight / 3), minAreaTestColCount);
			minAreaTestColCount--;
			if (minAreaTestColCount < 2)
				break;
		}
		if (areaTestCols == null) {
			pm.setInfo("   ==> cannot build table with no clean column gaps at all");
			return null;
		}
		if (areaTestCols.length < 2) {
			pm.setInfo("   ==> cannot build table with less than two columns");
			return null;
		}
		int areaTestColGapWidthSum = 0;
		for (int c = 1; c < areaTestCols.length; c++)
			areaTestColGapWidthSum += (areaTestCols[c].bounds.left - areaTestCols[c-1].bounds.right);
		System.out.println("   - column occupation lows sum to " + colOccupationLowWidthSum + ", test column gaps to " + areaTestColGapWidthSum);
		if ((areaTestCols.length < (areaStats.getBrgColOccupationLowsReduced().size() + 1)) && (areaTestColGapWidthSum < colOccupationLowWidthSum)) {
			pm.setInfo("   ==> cannot build table with no reliable column gaps");
			return null;
		}
		
		//	assess cell occupation (especially for row labels and column headers)
		int areaTestCellArea = 0;
		int areaTestCellAreaOccupied = 0;
		int areaTestCellAreaEmpty = 0;
		int areaTestCellsOccupied = 0;
		int areaTestCellsEmpty = 0;
		int[] areaTestCellsLeadingOccupied = new int[(areaTestCols.length < 4) ? 1 : 2];
		int[] areaTestCellsLeadingEmpty = new int[(areaTestCols.length < 4) ? 1 : 2];
		int[] areaTestCellsTopOccupied = new int[(areaStripes.length < 4) ? 1 : ((areaStripes.length < 6) ? 2 : 3)];
		int[] areaTestCellsTopEmpty = new int[(areaStripes.length < 4) ? 1 : ((areaStripes.length < 6) ? 2 : 3)];
		pm.setInfo("   - potential cells are:");
		for (int c = 0; c < areaTestCols.length; c++) {
			pm.setInfo("     - in " + areaTestCols[c].bounds);
			for (int r = 0; r < areaStripes.length; r++) {
				BoundingBox tacBounds = new BoundingBox(areaTestCols[c].bounds.left, areaTestCols[c].bounds.right, areaStripes[r].bounds.top, areaStripes[r].bounds.bottom);
				areaTestCellArea += tacBounds.getArea();
				ImWord[] tacWords = page.getWordsInside(tacBounds);
				String tacString = ImUtils.getString(tacWords, ImUtils.leftRightTopDownOrder, true);
				pm.setInfo("       - " + tacString);
				if (tacWords.length == 0) {
					areaTestCellAreaEmpty += tacBounds.getArea();
					areaTestCellsEmpty++;
					if (c < areaTestCellsLeadingEmpty.length)
						areaTestCellsLeadingEmpty[c]++;
					if (r < areaTestCellsTopEmpty.length)
						areaTestCellsTopEmpty[r]++;
				}
				else {
					areaTestCellAreaOccupied += tacBounds.getArea();
					areaTestCellsOccupied++;
					if (c < areaTestCellsLeadingOccupied.length)
						areaTestCellsLeadingOccupied[c]++;
					if (r < areaTestCellsTopOccupied.length)
						areaTestCellsTopOccupied[r]++;
				}
			}
		}
		
		//	test for super sparse candidates (graphics with labels mistaken for tables)
		int sparseTableAreaScore = 0;
		pm.setInfo("   --> got " + areaTestCellArea + " pixels occupied by cells out of " + tableArea.getArea() + " (" + ((areaTestCellArea * 100) / tableArea.getArea()) + "%)");
		pm.setInfo("   --> got " + areaTestCellAreaOccupied + " pixels occupied by occupied cells out of " + tableArea.getArea() + " (" + ((areaTestCellAreaOccupied * 100) / tableArea.getArea()) + "%)");
		pm.setInfo("   --> got " + areaTestCellAreaEmpty + " pixels occupied by empty cells out of " + tableArea.getArea() + " (" + ((areaTestCellAreaEmpty * 100) / tableArea.getArea()) + "%)");
		if ((areaTestCellArea * 5) < tableArea.getArea()) { // less than 20% of overall area occupied by cells
			sparseTableAreaScore++;
			pm.setInfo("   ==> area sparsely covered by cells");
		}
		else if ((areaTestCellAreaOccupied * 10) < tableArea.getArea()) { // less than 10% of overall area occupied by non-empty cells
			sparseTableAreaScore++;
			pm.setInfo("   ==> area sparsely covered by populated cells");
		}
		pm.setInfo("   --> got " + areaTestCellsOccupied + " occupied cells, " + areaTestCellsEmpty + " empty ones");
		if ((areaTestCellsOccupied * 3) < (areaTestCellsEmpty)) { // less than 25% of cells populated
			sparseTableAreaScore++;
			pm.setInfo("   ==> cells sparsely populated");
		}
		pm.setInfo("   --> got " + max(areaTestCellsLeadingOccupied) + " occupied row label cells, " + min(areaTestCellsLeadingEmpty) + " empty ones");
		if (max(areaTestCellsLeadingOccupied) < min(areaTestCellsLeadingEmpty)) { // less than half of potential row labels populated
			sparseTableAreaScore++;
			pm.setInfo("   ==> row labels sparsely populated");
		}
		pm.setInfo("   --> got " + max(areaTestCellsTopOccupied) + " occupied column header cells, " + min(areaTestCellsTopEmpty) + " empty ones");
		if (max(areaTestCellsTopOccupied) < min(areaTestCellsTopEmpty)) { // less than half of potential column headers populated
			sparseTableAreaScore++;
			pm.setInfo("   ==> column headers sparsely populated");
		}
		if (sparseTableAreaScore >= 3) {
			pm.setInfo("   ==> cannot build table in all too sparse area");
			return null;
		}
		
		//	TODOne check "short horizontal grid line" observation ==> added a few pixels of tolerance for grid lines mingled into words
		//	TODOne TEST ejt-598_rivera_herculano_lanna.pdf.imd (page 6), EJT-testbed/615
		
		//	use this for debug
//		BufferedImage areaImage = null;
		Graphics2D areaGr = null;
		
		//	if we have considerable horizontal peaks in in-word graphics, check if they coincide with row gaps
		if (areaStats.grRowOccupations != null) {
			System.out.println("Checking horizontal graphics in " + areaStats.wordBounds + ":");
//			if (areaImage == null) {
//				areaImage = new BufferedImage(areaStats.grImage.getWidth(), areaStats.grImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
//				areaGr = areaImage.createGraphics();
////				areaGr.drawImage(areaStats.grImage, 0, 0, null);
//				areaGr.setColor(new Color(128, 128, 128, 128));
//				for (int w = 0; w < areaStats.words.length; w++) {
//					int wordLeft = (areaStats.words[w].bounds.left - areaStats.grBounds.left);
//					int wordTop = (areaStats.words[w].bounds.top - areaStats.grBounds.top);
//					areaGr.fillRect(wordLeft, wordTop, areaStats.words[w].bounds.getWidth(), areaStats.words[w].bounds.getHeight());
//				}
//			}
//			areaGr.setColor(new Color(0, 0, 255, 64));
//			for (int tr = 0; tr < areaStripes.length; tr++)
//				areaGr.fillRect(0, (areaStripes[tr].bounds.top - areaStats.grBounds.top), areaStats.grBounds.getWidth(), areaStripes[tr].bounds.getHeight());
//			areaGr.setColor(new Color(0, 0, 255, 64));
//			for (int r = 0; r < areaStats.grRowOccupations.length; r++)
//				areaGr.fillRect(0, r, areaStats.grRowOccupations[r], 1);
//			if (areaImage != null) {
//				areaGr.dispose();
//				ImageDisplayDialog grIdd = new ImageDisplayDialog("Table Area Graphics in Page " + page.pageId + " at " + areaStats.grBounds);
//				grIdd.addImage(areaImage, "");
//				grIdd.setSize(600, 800);
//				grIdd.setLocationRelativeTo(null);
//				grIdd.setVisible(true);
//			}
			
			GraphicsPeakStats grRowStats25 = analyzeRowGraphicsPeaks(localAreaStats, areaStripes, areaWordHeight, 0.25f, 0.7f, areaGr);
//			GraphicsPeakStats grRowStats33 = analyzeRowGraphicsPeaks(localAreaStats, areaStripes, areaWordHeight, 0.33f, 0.7f, areaGr);
//			GraphicsPeakStats grRowStats50 = analyzeRowGraphicsPeaks(localAreaStats, areaStripes, areaWordHeight, 0.5f, 0.7f, areaGr);
//			GraphicsPeakStats grRowStats67 = analyzeRowGraphicsPeaks(localAreaStats, areaStripes, areaWordHeight, 0.67f, 0.7f, areaGr);
			//	TODO maybe use 33 stats ???
			if ((areaStats.grCount == 1) && (grRowStats25.fullHeightPeaks == 0) && ((areaStats.inWordGrBounds.getWidth() * 5) > (areaStats.wordBounds.getWidth() * 4))) {
				pm.setInfo("   ==> cannot build single-piece grid table with short horizontal grid lines");
				return null; // if we have a single-piece full-width grid, there has to be at least one spanning horizontal line
			}
			if ((grRowStats25.peaks != 0) && (grRowStats25.fullHeightPeaks == 0)) {
				pm.setInfo("   ==> cannot build table with short horizontal grid lines");
				return null; // if we have horizontal peaks, at least the one of them has to go all the width: the one below the column headers
			}
			if ((grRowStats25.peaks != 0) && (grRowStats25.intraStripePeaks > grRowStats25.interStripePeaks)) {
				pm.setInfo("   ==> cannot build table with in-row horizontal grid lines");
				return null; // if we have horizontal peaks, at least half of them should be between row stripes
			}
		}
		
		//	if we have considerable vertical peaks in in-word graphics, check if they coincide with column gaps
		if (areaStats.grColOccupations != null) {
			System.out.println("Checking vertical graphics " + areaStats.wordBounds + ":");
//			if (areaImage == null) {
//				areaImage = new BufferedImage(areaStats.grImage.getWidth(), areaStats.grImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
//				areaGr = areaImage.createGraphics();
////				areaGr.drawImage(areaStats.grImage, 0, 0, null);
//				areaGr.setColor(new Color(128, 128, 128, 128));
//				for (int w = 0; w < areaStats.words.length; w++) {
//					int wordLeft = (areaStats.words[w].bounds.left - areaStats.grBounds.left);
//					int wordTop = (areaStats.words[w].bounds.top - areaStats.grBounds.top);
//					areaGr.fillRect(wordLeft, wordTop, areaStats.words[w].bounds.getWidth(), areaStats.words[w].bounds.getHeight());
//				}
//			}
//			areaGr.setColor(new Color(255, 0, 0, 64));
//			for (int tc = 0; tc < areaTestCols.length; tc++)
//				areaGr.fillRect((areaTestCols[tc].bounds.left - areaStats.grBounds.left), 0, areaTestCols[tc].bounds.getWidth(), areaStats.grBounds.getHeight());
//			areaGr.setColor(new Color(255, 0, 0, 64));
//			for (int c = 0; c < areaStats.grColOccupations.length; c++)
//				areaGr.fillRect(c, (areaStats.grBounds.getHeight() - areaStats.grColOccupations[c]), 1, areaStats.grColOccupations[c]);
//			if (areaImage != null) {
//				areaGr.dispose();
//				ImageDisplayDialog grIdd = new ImageDisplayDialog("Table Area Graphics in Page " + page.pageId + " at " + areaStats.grBounds);
//				grIdd.addImage(areaImage, "");
//				grIdd.setSize(600, 800);
//				grIdd.setLocationRelativeTo(null);
//				grIdd.setVisible(true);
//			}
			
			GraphicsPeakStats grColStats25 = analyzeColGraphicsPeaks(localAreaStats, areaTestCols, areaWordHeight, docNormSpaceWidth, 0.25f, 0.8f, areaGr);
//			GraphicsPeakStats grColStats33 = analyzeColGraphicsPeaks(localAreaStats, areaTestCols, areaWordHeight, docNormSpaceWidth, 0.33f, 0.8f, areaGr);
//			GraphicsPeakStats grColStats50 = analyzeColGraphicsPeaks(localAreaStats, areaTestCols, areaWordHeight, docNormSpaceWidth, 0.5f, 0.8f, areaGr);
//			GraphicsPeakStats grColStats67 = analyzeColGraphicsPeaks(localAreaStats, areaTestCols, areaWordHeight, docNormSpaceWidth, 0.67f, 0.8f, areaGr);
			//	TODO maybe use 33 stats ???
			if ((areaStats.grCount == 1) && (grColStats25.fullHeightPeaks == 0) && ((areaStats.inWordGrBounds.getHeight() * 5) > (areaStats.wordBounds.getHeight() * 4))) {
				pm.setInfo("   ==> cannot build single-piece grid table with short vertical grid lines");
				return null; // if we have a single-piece full-height grid, there has to be at least one spanning horizontal line
			}
			if ((grColStats25.peaks != 0) && (grColStats25.fullHeightPeaks == 0)) {
				pm.setInfo("   ==> cannot build table with low vertical grid lines");
				return null; // if we have vertical peaks, at least the one of them has to go all the height: the one right of the row labels
			}
			if ((grColStats25.peaks != 0) && (grColStats25.intraStripePeaks > grColStats25.interStripePeaks)) {
				pm.setInfo("   ==> cannot build table with in-column vertical grid lines");
				return null; // if we have vertical peaks, at least half of them should be between column stripes
			}
		}
//		if (areaImage != null) {
//			areaGr.dispose();
//			ImageDisplayDialog grIdd = new ImageDisplayDialog("Table Area Graphics in Page " + page.pageId + " at " + areaStats.grBounds);
//			grIdd.addImage(areaImage, "");
//			grIdd.setSize(600, 800);
//			grIdd.setLocationRelativeTo(null);
//			grIdd.setVisible(true);
//		}
		
		//	TODO mark region as dead if (a) only two lines contained and (b) column split fails
		
		//	compute average column spacing ...
		//	TODO use table area stats above
		//	TODO maybe also compute mean (by means of CountingSet and elimination of extremes)
		int columnGapSum = 0;
		for (int c = 1; c < areaTestCols.length; c++)
			columnGapSum += (areaTestCols[c].bounds.left - areaTestCols[c-1].bounds.right);
		int avgColumnGap = (columnGapSum / (areaTestCols.length - 1));
		pm.setInfo("   - average column gap is " + avgColumnGap);
		
		//	TODO ... and compare in-cell word spacing to that
		for (int c = 0; c < areaTestCols.length; c++) {
			pm.setInfo("   - checking words distances in " + areaTestCols[c].bounds);
			for (int r = 0; r < areaStripes.length; r++) {
				BoundingBox tacBounds = new BoundingBox(areaTestCols[c].bounds.left, areaTestCols[c].bounds.right, areaStripes[r].bounds.top, areaStripes[r].bounds.bottom);
				ImWord[] tacWords = page.getWordsInside(tacBounds);
				ImUtils.sortLeftRightTopDown(tacWords);
				for (int w = 1; w < tacWords.length; w++) {
					if (tacWords[w-1].bounds.bottom < tacWords[w].centerY)
						continue; // no use comparing across line break
					int tacWordDist = (tacWords[w].bounds.left - tacWords[w-1].bounds.right);
					if ((avgColumnGap * 3) < (tacWordDist * 4)) {
						pm.setInfo("     - column gap grade word distance of " + tacWordDist + " in " + ImUtils.getString(tacWords, ImUtils.leftRightTopDownOrder, true) + " between '" + tacWords[w-1].getString() + "' and '" + tacWords[w].getString() + "'");
						//	TODO make this count
					}
				}
			}
		}
		
		//	finally ...
		return new TableAreaCandidate(areaStripes, areaTestCols, areaStats);
	}
	
	private static int min(int[] ints) {
		int minInt = Integer.MAX_VALUE;
		for (int i = 0; i < ints.length; i++) {
			if (ints[i] < minInt)
				minInt = ints[i];
		}
		return minInt;
	}
	
	private static int max(int[] ints) {
		int maxInt = Integer.MIN_VALUE;
		for (int i = 0; i < ints.length; i++) {
			if (maxInt < ints[i])
				maxInt = ints[i];
		}
		return maxInt;
	}
	
	private static int avg(int[] ints) {
		int sum = 0;
		for (int i = 0; i < ints.length; i++)
			sum += ints[i];
		return ((ints.length == 0) ? 0 : ((sum + (ints.length / 2)) / ints.length));
	}
	
	private static class GraphicsPeakStats {
		final int peaks;
		final int fullHeightPeaks;
		final int interStripePeaks;
		final int intraStripePeaks;
		final int[] stripeIntraPeaks;
		GraphicsPeakStats(int peaks, int fullHeightPeaks, int interStripePeaks, int intraStripePeaks, int[] stripeIntraPeaks) {
			this.peaks = peaks;
			this.fullHeightPeaks = fullHeightPeaks;
			this.interStripePeaks = interStripePeaks;
			this.intraStripePeaks = intraStripePeaks;
			this.stripeIntraPeaks = stripeIntraPeaks;
		}
	}
	
	private static GraphicsPeakStats analyzeRowGraphicsPeaks(TableAreaStatistics areaStats, TableAreaStripe[] areaStripes, float areaWordHeight, float peakFactor, float fullHeightPeakFactor, Graphics2D areaGr) {
		int avgRowOccupation = avg(areaStats.grRowOccupations);
		if (areaGr != null) {
			int minRowPeakOccupation = (avgRowOccupation + Math.round((areaStats.grBounds.getWidth() - avgRowOccupation) * peakFactor));
			areaGr.setColor(new Color(0, 0, 255, 255));
			areaGr.fillRect((minRowPeakOccupation - 1), 0, 2, areaStats.grBounds.getHeight());
		}
		BoundingBox[] stripeBoxes = new BoundingBox[areaStripes.length];
		for (int s = 0; s < areaStripes.length; s++)
			stripeBoxes[s] = areaStripes[s].bounds;
		return analyzeGraphicsPeaks(areaStats.grRowOccupations, avgRowOccupation, areaStats.grBounds.getWidth(), peakFactor, fullHeightPeakFactor, areaWordHeight, areaStats.grBounds.top, stripeBoxes, false);
	}
	
	private static GraphicsPeakStats analyzeColGraphicsPeaks(TableAreaStatistics areaStats, ImRegion[] areaTestCols, float areaWordHeight, float docNormSpaceWidth, float peakFactor, float fullHeightPeakFactor, Graphics2D areaGr) {
		int avgColOccupation = avg(areaStats.grColOccupations);
		if (areaGr != null) {
			int minColPeakOccupation = (avgColOccupation + Math.round((areaStats.grBounds.getHeight() - avgColOccupation) * peakFactor));
			areaGr.setColor(new Color(255, 0, 0, 255));
			areaGr.fillRect(0, (areaStats.grBounds.getHeight() - minColPeakOccupation - 1), areaStats.grBounds.getWidth(), 2);
		}
		BoundingBox[] stripeBoxes = new BoundingBox[areaTestCols.length];
		for (int s = 0; s < areaTestCols.length; s++)
			stripeBoxes[s] = areaTestCols[s].bounds;
		return analyzeGraphicsPeaks(areaStats.grColOccupations, avgColOccupation, areaStats.grBounds.getHeight(), peakFactor, fullHeightPeakFactor, (areaWordHeight * docNormSpaceWidth * 2), areaStats.grBounds.left, stripeBoxes, true);
	}
	
	private static GraphicsPeakStats analyzeGraphicsPeaks(int[] grOccupations, int avgOccupation, int maxGrOccupation, float peakFactor, float fullHeightPeakFactor, float stripeMinWidth, int areaOffset, BoundingBox[] areaStripes, boolean stripesVertical) {
		int peaks = 0;
		int fullHeightPeaks = 0;
		int interStripePeaks = 0;
		int intraStripePeaks = 0;
		int[] stripeIntraPeaks = new int[areaStripes.length];
		int minPeakOccupation = (avgOccupation + Math.round((maxGrOccupation - avgOccupation) * peakFactor));
		int minFullHeightPeakOccupation = (avgOccupation + Math.round((maxGrOccupation - avgOccupation) * fullHeightPeakFactor));
		for (int o = 0; o < grOccupations.length; o++) {
			if (grOccupations[o] < minPeakOccupation)
				continue;
			int peakHeightSum = grOccupations[o];
			for (int lo = (o+1); lo < grOccupations.length; lo++) {
				if (grOccupations[lo] >= minPeakOccupation) {
					peakHeightSum += grOccupations[lo];
					continue;
				}
				int peakWidth = (lo-o);
				int peakHeight = (peakHeightSum / peakWidth);
				System.out.println(" - got peak from " + o + " to " + (lo-1) + " at " + peakHeight + " of " + maxGrOccupation);
				peaks++;
				if (peakHeight > minFullHeightPeakOccupation)
					fullHeightPeaks++; // count 80% as full height (comb grid tables don't go up all the way)
				//	TODO use grid type distinction from area stats for this switch (once it's implemented)
				//	peak to low to even accommodate a word, hardly a stripe grid
				if (peakWidth < stripeMinWidth) {
					int peakCenter = (o + (peakWidth / 2) + areaOffset);
					//	TODO cut some slack for mingled lines?
					for (int s = 0; s < areaStripes.length; s++) {
						int slack = getGraphicsPeakUntagleSlack(stripesVertical, areaStripes[s]);
						if ((stripesVertical ? areaStripes[s].right : areaStripes[s].bottom) <= (peakCenter + slack)) {
							if ((stripesVertical ? areaStripes[s].right : areaStripes[s].bottom) > peakCenter)
								System.out.println("   --> accepted center " + peakCenter + " mingled into right/bottom of " + areaStripes[s]);
							continue;
						}
						if ((stripesVertical ? areaStripes[s].left : areaStripes[s].top) < (peakCenter - slack)) {
							intraStripePeaks++;
							stripeIntraPeaks[s]++;
							System.out.println("   --> found center " + peakCenter + " inside " + areaStripes[s]);
						}
						else {
							interStripePeaks++;
							if ((stripesVertical ? areaStripes[s].left : areaStripes[s].top) < peakCenter)
								System.out.println("   --> accepted center " + peakCenter + " mingled into left/top of " + areaStripes[s]);
							else System.out.println("   --> found center " + peakCenter + " before " + areaStripes[s]);
						}
						break;
					}
				}
				//	peak to wide to be a simple line, likely stripe grid, look at flanks
				else {
					int peakMinFlank = (o + areaOffset);
					//	TODO cut some slack for mingled flanks?
					for (int s = 0; s < areaStripes.length; s++) {
						int slack = getGraphicsPeakUntagleSlack(stripesVertical, areaStripes[s]);
						if ((stripesVertical ? areaStripes[s].right : areaStripes[s].bottom) <= (peakMinFlank + slack)) {
							if ((stripesVertical ? areaStripes[s].right : areaStripes[s].bottom) > peakMinFlank)
								System.out.println("   --> accepted min flank " + peakMinFlank + " mingled into right/bottom of " + areaStripes[s]);
							continue;
						}
						if ((stripesVertical ? areaStripes[s].left : areaStripes[s].top) < (peakMinFlank - slack)) {
							intraStripePeaks++;
							stripeIntraPeaks[s]++;
							System.out.println("   --> found min flank " + peakMinFlank + " inside " + areaStripes[s]);
						}
						else {
							interStripePeaks++;
							if ((stripesVertical ? areaStripes[s].left : areaStripes[s].top) < peakMinFlank)
								System.out.println("   --> accepted min flank " + peakMinFlank + " mingled into left/top of " + areaStripes[s]);
							else System.out.println("   --> found min flank " + peakMinFlank + " before " + areaStripes[s]);
						}
						break;
					}
					int peakMaxFlank = (lo + areaOffset);
					//	TODO cut some slack for mingled flanks?
					for (int s = 0; s < areaStripes.length; s++) {
						int slack = getGraphicsPeakUntagleSlack(stripesVertical, areaStripes[s]);
						if ((stripesVertical ? areaStripes[s].right : areaStripes[s].bottom) <= (peakMaxFlank + slack)) {
							if ((stripesVertical ? areaStripes[s].right : areaStripes[s].bottom) > peakMaxFlank)
								System.out.println("   --> accepted max flank " + peakMaxFlank + " mingled into right/bottom of " + areaStripes[s]);
							continue;
						}
						if ((stripesVertical ? areaStripes[s].left : areaStripes[s].top) < (peakMaxFlank - slack)) {
							intraStripePeaks++;
							stripeIntraPeaks[s]++;
							System.out.println("   --> found max flank " + peakMaxFlank + " inside " + areaStripes[s]);
						}
						else {
							interStripePeaks++;
							if ((stripesVertical ? areaStripes[s].left : areaStripes[s].top) < peakMaxFlank)
								System.out.println("   --> accepted max flank " + peakMaxFlank + " mingled into left/top of " + areaStripes[s]);
							else System.out.println("   --> found max flank " + peakMaxFlank + " before " + areaStripes[s]);
						}
						break;
					}
				}
				o = lo;
				break;
			}
		}
		System.out.println(" ==> got " + peaks + " peaks, " + fullHeightPeaks + " full height ones, " + interStripePeaks + " between stripes, and " + intraStripePeaks + " inside stripes (" + Arrays.toString(stripeIntraPeaks) + ")");
		return new GraphicsPeakStats(peaks, fullHeightPeaks, interStripePeaks, intraStripePeaks, stripeIntraPeaks);
	}
	
	private static int getGraphicsPeakUntagleSlack(boolean stripesVertical, BoundingBox areaStripe) {
		return Math.min(2, (((stripesVertical ? areaStripe.getWidth() : areaStripe.getHeight()) * 3) / 20));
	}
	
	private static class TableAreaStripe {
		final ImWord[] words;
		final BoundingBox bounds;
		final int wordWidthSum;
		final int wordHeightSum;
		final int wordArea;
		final int wordDistanceCount;
		final int wordDistanceSum;
		final float nWordDistanceSum;
		final int area;
		TableAreaStripe(ImWord[] words) {
			Arrays.sort(words, ImUtils.leftRightOrder);
			this.words = words;
			this.bounds = ImLayoutObject.getAggregateBox(this.words);
			
			int wordArea = 0;
			int wordWidthSum = 0;
			int wordHeightSum = 0; // using average word height as proxy for both font size and DPI
			int wordDistanceCount = 0;
			int minWordDistance = Integer.MAX_VALUE;
			int maxWordDistance = 0;
			int wordDistanceSum = 0;
			int wordDistanceSquareSum = 0;
			for (int w = 0; w < this.words.length; w++) {
				wordArea += getArea(this.words[w]);
				wordWidthSum += (this.words[w].bounds.right - this.words[w].bounds.left);
				wordHeightSum += (this.words[w].bounds.bottom - this.words[w].bounds.top);
				if (w != 0) {
					int wordDistance = (this.words[w].bounds.left - this.words[w-1].bounds.right);
					if (wordDistance > 0) {
						wordDistanceCount++;
						minWordDistance = Math.min(minWordDistance, wordDistance);
						maxWordDistance = Math.max(maxWordDistance, wordDistance);
						wordDistanceSum += wordDistance;
						wordDistanceSquareSum += (wordDistance * wordDistance);
					}
				}
			}
			
			//	compute overall word stats
			this.wordWidthSum = wordWidthSum;
			this.wordHeightSum = wordHeightSum;
			this.wordArea = wordArea;
			this.wordDistanceCount = wordDistanceCount;
			this.wordDistanceSum = wordDistanceSum;
			this.nWordDistanceSum = (((float) (wordDistanceSum * this.words.length)) / wordHeightSum);
			
			//	compute own stats
			this.area = getArea(this.bounds);
		}
	}
	
	private static TableAreaStripe[] getTableAreaStripes(ImPage page, TableAreaStatistics stats) {
		ArrayList stripeList = new ArrayList();
		int lastNonEmptyRow = -1;
		for (int r = 0; r < stats.rowOccupations.length; r++) {
			if (stats.rowOccupations[r] == 0) {
				if (lastNonEmptyRow != -1) {
					BoundingBox stipeBounds = new BoundingBox(stats.wordBounds.left, stats.wordBounds.right, (stats.wordBounds.top + lastNonEmptyRow), (stats.wordBounds.top + r));
					ImWord[] stripeWords = page.getWordsInside(stipeBounds);
					if (stripeWords.length != 0)
						stripeList.add(new TableAreaStripe(stripeWords));
				}
				lastNonEmptyRow = -1;
			}
			else if (lastNonEmptyRow == -1)
				lastNonEmptyRow = r;
		}
		if (lastNonEmptyRow != -1) {
			BoundingBox stipeBounds = new BoundingBox(stats.wordBounds.left, stats.wordBounds.right, (stats.wordBounds.top + lastNonEmptyRow), (stats.wordBounds.top + stats.rowOccupations.length));
			ImWord[] stripeWords = page.getWordsInside(stipeBounds);
			if (stripeWords.length != 0)
				stripeList.add(new TableAreaStripe(stripeWords));
		}
		return ((TableAreaStripe[]) stripeList.toArray(new TableAreaStripe[stripeList.size()]));
	}
	/*
When seeking tables split up into multiple blocks:
- compare column gaps and anchors between adjacent blocks ...
- ... as well as general word / whitespace compatibility
==> build analyzeArea(ImPage, BoundingBox) method (or the like named, maybe isLikelyTable):
- analyze row stripe word density, ...
- ... whitespace distribution, ...
- ... potential column splits and gaps (gap width in particular), ...
- ... etc.
	 */
	private static final Comparator topDownGraphicsOrder = new Comparator() {
		public int compare(Object obj1, Object obj2) {
			BoundingBox bb1 = ((Graphics) obj1).getBounds();
			BoundingBox bb2 = ((Graphics) obj2).getBounds();
			if (bb1.top == bb2.top)
				return (bb1.left - bb2.left);
			else return (bb1.top - bb2.top);
		}
	};
	
	private static final Comparator descendingBoxSizeOrder = new Comparator() {
		public int compare(Object obj1, Object obj2) {
			BoundingBox bb1 = ((BoundingBox) obj1);
			BoundingBox bb2 = ((BoundingBox) obj2);
			return ((bb2.getArea() - bb1.getArea()));
		}
	};
	
	/** TEST ONLY !!! */
	public static void main(String[] args) throws Exception {
		//	zoosystema2020v42a3.pdf.imdir (partially ultra-dense table on page 5)
		//	EJT/ejt-585_ebersole_cicimurri_stinger.pdf.imdir (horizontal stripe grid on page 6)
		//	EJT/ejt-598_rivera_herculano_lanna.pdf.imdir (mingled horizontal line grid on page 6)
		String testDocPath = "E:/Testdaten/PdfExtract/zoosystema2020v42a3.pdf.imdir";
		aimAtPage = 5;
		ImDocument doc = ImDocumentIO.loadDocument(new File(testDocPath));
		TableDetectorProvider tdp = new TableDetectorProvider();
		tdp.init();
		tdp.detectTables(doc, DocumentStyle.getStyleFor(doc), null, ProgressMonitor.dummy);
	}
	private static int aimAtPage = -1;
}
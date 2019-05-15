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
package de.uka.ipd.idaho.im.imagine.plugins.doc;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.gamta.util.ParallelJobRunner;
import de.uka.ipd.idaho.gamta.util.ParallelJobRunner.ParallelFor;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor.SynchronizedProgressMonitor;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.DocumentStyle;
import de.uka.ipd.idaho.gamta.util.imaging.DocumentStyle.ParameterDescription;
import de.uka.ipd.idaho.gamta.util.imaging.DocumentStyle.ParameterGroupDescription;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.goldenGate.plugins.PluginDataProviderFileBased;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImLayoutObject;
import de.uka.ipd.idaho.im.ImObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.analysis.PageAnalysis;
import de.uka.ipd.idaho.im.analysis.PageAnalysis.BlockLayout;
import de.uka.ipd.idaho.im.analysis.PageAnalysis.BlockMetrics;
import de.uka.ipd.idaho.im.gamta.ImTokenSequence;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.DisplayExtensionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.tables.TableActionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.tables.TableDetectorProvider;
import de.uka.ipd.idaho.im.imagine.plugins.util.CaptionCitationHandler;
import de.uka.ipd.idaho.im.util.ImDocumentIO;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.DisplayExtension;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.DisplayExtensionGraphics;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;

/**
 * This plugin provides a detector for the structure of a document. The detector
 * identified page headers, page numbers, captions, and footnotes. It can also
 * be used as a component outside of GoldenGATE Imagine.
 * 
 * @author sautter
 */
public class DocumentStructureDetectorProvider extends AbstractImageMarkupToolProvider implements ImagingConstants, ReactionProvider {
	private boolean debug = false;
//	private static final boolean detectTablesSimpleDefault = true;
//	private boolean detectTablesSimple = detectTablesSimpleDefault;
	
	private static final String STRUCTURE_DETECTOR_IMT_NAME = "StructureDetector";
	
	private String pageNumberPattern = "[1-9][0-9]*";
	private String ocrPageNumberPartPattern = "[0-9]+";
	private HashMap ocrPageNumberCharacterTranslations = new HashMap();
	
	private CaptionStartPattern[] captionStartPatterns = new CaptionStartPattern[0];
	
	private Pattern[] footnoteStartPatterns = new Pattern[0];
	
	private CaptionCitationHandler captionCitationHandler;
	
	private TableActionProvider tableActionProvider;
	private TableDetectorProvider tableDetectorProvider;
	
	private ImageMarkupTool structureDetector = new StructureDetector();
	
	/** public zero-argument constructor for class loading */
	public DocumentStructureDetectorProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Document Structure Detector";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		
		//	load character mappings
		try {
			InputStream is = this.dataProvider.getInputStream("pageNumberCharacters.txt");
			StringVector characterLines = StringVector.loadList(is);
			is.close();
			
			TreeSet pageNumberCharacters = new TreeSet();
			for (int l = 0; l < characterLines.size(); l++) {
				String characterLine = characterLines.get(l).trim();
				if ((characterLine.length() == 0) || characterLine.startsWith("//") || (characterLine.indexOf(' ') == -1))
					continue;
				
				String digit = characterLine.substring(0, characterLine.indexOf(' ')).trim();
				if (!Gamta.isNumber(digit))
					continue;
				
				pageNumberCharacters.add(digit);
				ArrayList characterTranslations = ((ArrayList) this.ocrPageNumberCharacterTranslations.get(digit));
				if (characterTranslations == null) {
					characterTranslations = new ArrayList(2);
					this.ocrPageNumberCharacterTranslations.put(digit, characterTranslations);
				}
				characterTranslations.add(digit);
				
				String characters = characterLine.substring(characterLine.indexOf(' ')).trim();
				for (int c = 0; c < characters.length(); c++) {
					String character = characters.substring(c, (c+1));
					pageNumberCharacters.add(character);
					characterTranslations = ((ArrayList) this.ocrPageNumberCharacterTranslations.get(character));
					if (characterTranslations == null) {
						characterTranslations = new ArrayList(2);
						this.ocrPageNumberCharacterTranslations.put(character, characterTranslations);
					}
					characterTranslations.add(digit);
				}
			}
			
			for (int d = 0; d < Gamta.DIGITS.length(); d++)
				pageNumberCharacters.add(Gamta.DIGITS.substring(d, (d+1)));
			
			StringBuffer ocrPageNumberCharaterPatternBuilder = new StringBuffer();
			for (Iterator cit = pageNumberCharacters.iterator(); cit.hasNext();) {
				String ocrPageNumberCharacter = ((String) cit.next());
				ocrPageNumberCharaterPatternBuilder.append(ocrPageNumberCharacter);
				ArrayList characterTranslations = ((ArrayList) this.ocrPageNumberCharacterTranslations.get(ocrPageNumberCharacter));
				if (characterTranslations == null)
					continue;
				int[] cts = new int[characterTranslations.size()];
				for (int t = 0; t < characterTranslations.size(); t++)
					cts[t] = Integer.parseInt((String) characterTranslations.get(t));
				this.ocrPageNumberCharacterTranslations.put(ocrPageNumberCharacter, cts);
			}
			
			String ocrPageNumberCharacterPattern = ("[" + RegExUtils.escapeForRegEx(ocrPageNumberCharaterPatternBuilder.toString()) + "]");
			this.ocrPageNumberPartPattern = (ocrPageNumberCharacterPattern + "+(\\s?" + ocrPageNumberCharacterPattern + "+)*");
		} catch (IOException ioe) {}
		
		//	load caption indicators
		try {
			InputStream is = this.dataProvider.getInputStream("captionStartPatterns.txt");
			StringVector captionStartPatternStrings = StringVector.loadList(is);
			is.close();
			
			ArrayList captionStartPatterns = new ArrayList();
			for (int l = 0; l < captionStartPatternStrings.size(); l++) {
				String captionStartPattern = captionStartPatternStrings.get(l).trim();
				if ((captionStartPattern.length() == 0) || captionStartPattern.startsWith("//"))
					continue;
				try {
					captionStartPatterns.add(CaptionStartPattern.createPattern(captionStartPattern, false));
				} catch (PatternSyntaxException pse) {}
			}
			
			this.captionStartPatterns = ((CaptionStartPattern[]) captionStartPatterns.toArray(new CaptionStartPattern[captionStartPatterns.size()]));
		} catch (IOException ioe) {}
		
		//	load footnote indicators
		try {
			InputStream is = this.dataProvider.getInputStream("footnoteStartPatterns.txt");
			StringVector footnoteStartPatternStrings = StringVector.loadList(is);
			is.close();
			
			ArrayList footnoteStartPatterns = new ArrayList();
			for (int l = 0; l < footnoteStartPatternStrings.size(); l++) {
				String footnoteStartPattern = footnoteStartPatternStrings.get(l).trim();
				if ((footnoteStartPattern.length() == 0) || footnoteStartPattern.startsWith("//"))
					continue;
				try {
					footnoteStartPatterns.add(Pattern.compile(footnoteStartPattern, Pattern.CASE_INSENSITIVE));
				} catch (PatternSyntaxException pse) {}
			}
			
			this.footnoteStartPatterns = ((Pattern[]) footnoteStartPatterns.toArray(new Pattern[footnoteStartPatterns.size()]));
		} catch (IOException ioe) {}
		
		//	get table action provider
		if (this.parent == null)
			this.tableActionProvider = new TableActionProvider();
		else this.tableActionProvider = ((TableActionProvider) this.parent.getPlugin(TableActionProvider.class.getName()));
		
		//	get table detector provider
		if (this.parent == null)
			this.tableDetectorProvider = new TableDetectorProvider();
		else this.tableDetectorProvider = ((TableDetectorProvider) this.parent.getPlugin(TableDetectorProvider.class.getName()));
		
		//	get caption citation handler
		if (this.parent == null)
			this.captionCitationHandler = new CaptionCitationHandler();
		else this.captionCitationHandler = ((CaptionCitationHandler) this.parent.getPlugin(CaptionCitationHandler.class.getName()));
		
		//	register parameter group descriptions for all the various style parameters we use
		ParameterGroupDescription pgd;
		
		//	general page layout
		pgd = new ParameterGroupDescription("layout");
		pgd.setLabel("General Page Layout");
		pgd.setDescription("Parameters describing the general layout pages, especially the grouping of paragraphs into blocks and columns.");
		pgd.setParamLabel("columnAreas", "Column Areas");
		pgd.setParamDescription("columnAreas", "One or more bounding boxes marking individual text columns (normalized to 72 DPI); column splits occur only between these bounding boxes, not inside.");
		pgd.setParamLabel("columnCount", "Number Of Columns");
		pgd.setParamDescription("columnCount", "The number of columns.");
		pgd.setParamLabel("contentArea", "Page Content Area");
		pgd.setParamDescription("contentArea", "A bounding box enclosing all content of a page (normalized to 72 DPI), including any page headers and footers; all content outside this bounding box is removed (e.g. a source URL added on page edges by a download portal).");
		pgd.setParamLabel("coverPageCount", "Number Of Cover Pages");
		pgd.setParamDescription("coverPageCount", "The number of cover pages, i.e., the number of pages before the actual document starts (e.g. added by a download portal).");
		pgd.setParameterDescription("minBlockMargin", new MarginParameterDescription(pgd.parameterNamePrefix + "." + "minBlockMargin"));
		pgd.setParamLabel("minBlockMargin", "Minimum Block Margin");
		pgd.setParamDescription("minBlockMargin", "The minimum block magin, i.e., the minimum hight (in pixels) of the space between two text blocks (normalized to 72 DPI).");
		pgd.setParameterDescription("minColumnMargin", new MarginParameterDescription(pgd.parameterNamePrefix + "." + "minColumnMargin"));
		pgd.setParamLabel("minColumnMargin", "Minimum Column Margin");
		pgd.setParamDescription("minColumnMargin", "The minimum column magin, i.e., the minimum width (in pixels) of the space between two text columns (normalized to 72 DPI).");
		pgd.setParamLabel("minColumnWidth", "Minimum Column Width");
		pgd.setParamDescription("minColumnWidth", "The minimum column width, i.e., the minimum width (in pixels) of a text column (normalized to 72 DPI).");
		DocumentStyle.addParameterGroupDescription(pgd);
		
		//	block layout
		pgd = new ParameterGroupDescription("layout.block");
		pgd.setLabel("Block Layout");
		pgd.setDescription("Parameters describing the layout of text blocks, especially the grouping of lines into paragraphs.");
		pgd.setParamLabel("minCenterRightAccumulationPointSupport", "Minimum Support for Accumulation Point in Center and on Right");
		pgd.setParamDescription("minCenterRightAccumulationPointSupport", "The relative minimum support for an accumulation point of line centers or right line ends; used for paragraph splitting.");
		pgd.setParamLabel("minLeftAccumulationPointSupport", "Minimum Support for Accumulation Point on Left");
		pgd.setParamDescription("minLeftAccumulationPointSupport", "The relative minimum support for an accumulation point of left line ends; used for paragraph splitting.");
		pgd.setParamLabel("minSignificantHorizontalDist", "Minimum Significant Horizontal Distance");
		pgd.setParamDescription("minSignificantHorizontalDist", "The minimum significant horizontal distance between two accumulation point to be considered distinct (in pixels at 72 DPI); the more accurate document layout is, the lower this value can be, i.e., higher values are mostly needed for scans.");
		pgd.setParamLabel("minSignificantVerticalDist", "Minimum Significant Vertical Distance");
		pgd.setParamDescription("minSignificantVerticalDist", "The minimum significant vertical distance between two accumulation point to be considered distinct (in pixels at 72 DPI); the more accurate document layout is, the lower this value can be, i.e., higher values are mostly needed for scans.");
		pgd.setParamLabel("normSpaceWidth", "Normalized Space Width");
		pgd.setParamDescription("normSpaceWidth", "The normalized width of a non-stretched space, relative to line height.");
		DocumentStyle.addParameterGroupDescription(pgd);
		
		//	caption layout and positioning
		pgd = new TestableParameterGroupDescription("layout.caption");
		pgd.setLabel("Caption Layout & Positioning");
		pgd.setDescription("Parameters describing the where captions can be located relative to the figure, graphics, or table they describe, as well as styling of captions proper.");
		pgd.setParamLabel("aboveFigure", "Captions Above Figure");
		pgd.setParamDescription("aboveFigure", "Do figure captions occur above the figures they describe?");
		pgd.setParamLabel("aboveTable", "Captions Above Table");
		pgd.setParamDescription("aboveTable", "Do table captions occur above the tables they describe?");
		pgd.setParamLabel("belowFigure", "Captions Below Figure");
		pgd.setParamDescription("belowFigure", "Do figure captions occur below the figures they describe?");
		pgd.setParamLabel("belowTable", "Captions Below Table");
		pgd.setParamDescription("belowTable", "Do table captions occur below the figures they describe?");
		pgd.setParamLabel("besideFigure", "Captions Beside Figure");
		pgd.setParamDescription("besideFigure", "Do figure captions occur beside the figures they describe, i.e., on their left or right?");
		pgd.setParamLabel("besideTable", "Captions Beside Table");
		pgd.setParamDescription("besideTable", "Do table captions occur beside the tables they describe, i.e., on their left or right?");
		pgd.setParamLabel("insideFigure", "Captions Inside Figure");
		pgd.setParamDescription("insideFigure", "Do figure captions occur inside the figures they describe, i.e., as an overlay of the figure proper?");
		pgd.setParamLabel("figureStartPatterns", "Figure Caption Start Patterns");
		pgd.setParamDescription("figureStartPatterns", "One or more patterns matching the start of figure captions, right up to the figure number.");
		pgd.setParamLabel("fontSize", "Font Size");
		pgd.setParamDescription("fontSize", "The exact font size of captions (use minimum and maximum font size if variation present or to be expected).");
		pgd.setParamLabel("maxFontSize", "Maximum Font Size");
		pgd.setParamDescription("maxFontSize", "The maximum font size of captions (use only if variation present or to be expected, otherwise use exact font size).");
		pgd.setParamLabel("minFontSize", "Minimum Font Size");
		pgd.setParamDescription("minFontSize", "The minimum font size of captions (use only if variation present or to be expected, otherwise use exact font size).");
		pgd.setParamLabel("platePartStartPatterns", "Plate Part Caption Start Patterns");
		pgd.setParamDescription("platePartStartPatterns", "One or more patterns matching the start of plate sub-captions (i.e., the chunks of a plate caption describing individual figures inside the plate), right up to the figure number.");
		pgd.setParamLabel("plateStartPatterns", "Plate Caption Start Patterns");
		pgd.setParamDescription("plateStartPatterns", "One or more patterns matching the start of plate captions, right up to the figure number.");
		pgd.setParamLabel("startIsBold", "Do Captions Start in Bold?");
		pgd.setParamDescription("startIsBold", "Are caption starts set in bold face, i.e., bold up to and including the numbering?");
		pgd.setParamLabel("startPatterns", "Start Patterns");
		pgd.setParamDescription("startPatterns", "One or more patterns matching the start of any kind of captions, right up to the figure number; to be used alternatively to the four patterns dedicated to figures, tables, and plates and their parts.");
		pgd.setParamLabel("tableStartPatterns", "Table Caption Start Patterns");
		pgd.setParamDescription("tableStartPatterns", "One or more patterns matching the start of table captions, right up to the figure number.");
		DocumentStyle.addParameterGroupDescription(pgd);
		
		//	footnotes
		pgd = new TestableParameterGroupDescription("layout.footnote");
		pgd.setLabel("Footnote Layout");
		pgd.setDescription("Parameters describing the styling of footnotes and where they can be located on a page.");
		pgd.setParamLabel("area", "Footnote Area");
		pgd.setParamDescription("area", "A bounding box marking the area on a page where footnotes can be located (normalized to 72 DPI).");
		pgd.setParamLabel("fontSize", "Font Size");
		pgd.setParamDescription("fontSize", "The exact font size of footnotes (use minimum and maximum font size if variation present or to be expected).");
		pgd.setParamLabel("maxFontSize", "Maximum Font Size");
		pgd.setParamDescription("maxFontSize", "The maximum font size of footnotes (use only if variation present or to be expected, otherwise use exact font size).");
		pgd.setParamLabel("minFontSize", "Minimum Font Size");
		pgd.setParamDescription("minFontSize", "The minimum font size of footnotes (use only if variation present or to be expected, otherwise use exact font size).");
		pgd.setParamLabel("startPatterns", "Start Patterns");
		pgd.setParamDescription("startPatterns", "One or more patterns matching the start of a footnote.");
		DocumentStyle.addParameterGroupDescription(pgd);
		
		//	pages in general ...
		pgd = new ParameterGroupDescription("layout.page");
		pgd.setLabel("Page Layout");
		pgd.setDescription("Parameters describing the layout of pages in general. The parameters that differ between even and odd pages should be entered in the respective sub groups instead. The same applies to parameters for the first page only (article headers, etc.).");
		pgd.setParamLabel("headerAreas", "Header Areas");
		pgd.setParamDescription("headerAreas", "One or more bounding boxes marking areas of the page that contain headers and footers (normalized to 72 DPI); text in such areas will be cut out of the main document text.");
		DocumentStyle.addParameterGroupDescription(pgd);
		
		//	... and the respective page number
		pgd = new ParameterGroupDescription("layout.page.number");
		pgd.setLabel("Page Number");
		pgd.setDescription("Parameters describing the layout and location of page numbers. The parameters that differ between even and odd pages should be entered in the respective sub groups instead. The same applies to parameters for the first page only (article headers, etc.).");
		pgd.setParamLabel("area", "Page Number Area");
		pgd.setParamDescription("area", "A bounding boxes marking the area of the page that contain the page number (normalized to 72 DPI); text outside that area will be ignored on page number extraction.");
		pgd.setParamLabel("fontSize", "Font Size");
		pgd.setParamDescription("fontSize", "The exact font size of the page numbers.");
		pgd.setParamLabel("isBold", "Are Page Numbers Bold?");
		pgd.setParamDescription("isBold", "Are page numbers set in bold face?");
		pgd.setParamLabel("pattern", "Page Number Pattern");
		pgd.setParamDescription("pattern", "A regular expression pattern matching the page numbers.");
		DocumentStyle.addParameterGroupDescription(pgd);
		
		//	even pages ...
		pgd = new ParameterGroupDescription("layout.page.even");
		pgd.setLabel("Layout Of Even-Number Pages");
		pgd.setDescription("Parameters describing the layout of even-number pages. Parameters that are the same on both even and odd pages should be specified in the general layout group.");
		pgd.setParamLabel("headerAreas", "Header Areas");
		pgd.setParamDescription("headerAreas", "One or more bounding boxes marking areas of the page that contain headers and footers (normalized to 72 DPI); text in such areas will be cut out of the main document text.");
		DocumentStyle.addParameterGroupDescription(pgd);
		
		//	... and the respective page number
		pgd = new ParameterGroupDescription("layout.page.even.number");
		pgd.setLabel("Even Page Number");
		pgd.setDescription("Parameters describing the layout and location of page numbers on even-numbered pages. Parameters that are the same on both even and odd pages should be specified in the general layout group.");
		pgd.setParamLabel("area", "Page Number Area");
		pgd.setParamDescription("area", "A bounding boxes marking the area of the page that contain the page number (normalized to 72 DPI); text outside that area will be ignored on page number extraction.");
		pgd.setParamLabel("fontSize", "Font Size");
		pgd.setParamDescription("fontSize", "The exact font size of the page numbers.");
		pgd.setParamLabel("isBold", "Are Page Numbers Bold?");
		pgd.setParamDescription("isBold", "Are page numbers set in bold face?");
		pgd.setParamLabel("pattern", "Page Number Pattern");
		pgd.setParamDescription("pattern", "A regular expression pattern matching the page numbers.");
		DocumentStyle.addParameterGroupDescription(pgd);
		
		//	odd pages ...
		pgd = new ParameterGroupDescription("layout.page.odd");
		pgd.setLabel("Layout Of Odd-Number Pages");
		pgd.setDescription("Parameters describing the layout of odd-number pages. Parameters that are the same on both even and odd pages should be specified in the general layout group.");
		pgd.setParamLabel("headerAreas", "Header Areas");
		pgd.setParamDescription("headerAreas", "One or more bounding boxes marking areas of the page that contain headers and footers (normalized to 72 DPI); text in such areas will be cut out of the main document text.");
		DocumentStyle.addParameterGroupDescription(pgd);
		
		//	... and the respective page number
		pgd = new ParameterGroupDescription("layout.page.odd.number");
		pgd.setLabel("Odd Page Number");
		pgd.setDescription("Parameters describing the layout and location of page numbers on odd-numbered pages. Parameters that are the same on both even and odd pages should be specified in the general layout group.");
		pgd.setParamLabel("area", "Page Number Area");
		pgd.setParamDescription("area", "A bounding boxes marking the area of the page that contain the page number (normalized to 72 DPI); text outside that area will be ignored on page number extraction.");
		pgd.setParamLabel("fontSize", "Font Size");
		pgd.setParamDescription("fontSize", "The exact font size of the page numbers.");
		pgd.setParamLabel("isBold", "Are Page Numbers Bold?");
		pgd.setParamDescription("isBold", "Are page numbers set in bold face?");
		pgd.setParamLabel("pattern", "Page Number Pattern");
		pgd.setParamDescription("pattern", "A regular expression pattern matching the page numbers.");
		DocumentStyle.addParameterGroupDescription(pgd);
		
		//	first page ...
		pgd = new ParameterGroupDescription("layout.page.first");
		pgd.setLabel("Layout Of First Page");
		pgd.setDescription("Parameters describing the layout of the first page. Parameters that are the same on both even and odd pages should be specified in the general layout group. This group is specifically intended to deal with additional decoration and headers frequently found on the start pages of journal articles.");
		pgd.setParamLabel("headerAreas", "Header Areas");
		pgd.setParamDescription("headerAreas", "One or more bounding boxes marking areas of the page that contain headers and footers (normalized to 72 DPI); text in such areas will be cut out of the main document text.");
		DocumentStyle.addParameterGroupDescription(pgd);
		
		//	... and the respective page number
		pgd = new ParameterGroupDescription("layout.page.first");
		pgd.setLabel("First Page Number");
		pgd.setDescription("Parameters describing the layout and location of the page number on the first page. Parameters that are the same on both even and odd pages should be specified in the general layout group.");
		pgd.setParamLabel("area", "Page Number Area");
		pgd.setParamDescription("area", "A bounding boxes marking the area of the page that contain the page number (normalized to 72 DPI); text outside that area will be ignored on page number extraction.");
		pgd.setParamLabel("fontSize", "Font Size");
		pgd.setParamDescription("fontSize", "The exact font size of the page numbers.");
		pgd.setParamLabel("isBold", "Are Page Numbers Bold?");
		pgd.setParamDescription("isBold", "Are page numbers set in bold face?");
		pgd.setParamLabel("pattern", "Page Number Pattern");
		pgd.setParamDescription("pattern", "A regular expression pattern matching the page numbers.");
		DocumentStyle.addParameterGroupDescription(pgd);
		
		//	tables
		pgd = new ParameterGroupDescription("layout.table");
		pgd.setLabel("Table Layout");
		pgd.setDescription("Parameters describing the layout of tables, i.e., which forms of table grids occur.");
		pgd.setParamLabel("spanningOnePieceGrid", "Full-Grid Tables");
		pgd.setParamDescription("spanningOnePieceGrid", "Indicate to expect tables with a connected grid of horizontal and verticl lines that encloses the whole table area.");
		pgd.setParamLabel("dataSpanningOnePieceGrid", "Spanning-Grid Tables");
		pgd.setParamDescription("dataSpanningOnePieceGrid", "Indicate to expect tables with a connected grid of horizontal and verticl lines that spans the table area, but does not enclose it; such grids can for instance take te form of a disproportionate cross or multiple crosses connected side by side.");
		pgd.setParamLabel("horizontalMultiPieceGrid", "Horizontal Multi-Line-Grid Tables");
		pgd.setParamDescription("horizontalMultiPieceGrid", "Indicate to expect tables whose grid consists of horizontal lines at top and bottom an potentially between rows, but no vertical lines.");
		pgd.setParamLabel("verticalMultiPieceGrid", "Vertical Multi-Line-Grid Tables");
		pgd.setParamDescription("verticalMultiPieceGrid", "Indicate to expect tables whose grid consists of vertical lines at left and right an potentially between columns, but no horizontal lines.");
		pgd.setParamLabel("horizontalSingleLineGrid", "Horizontal Single-Line-Grid Tables");
		pgd.setParamDescription("horizontalSingleLineGrid", "Indicate to expect tables whose grid consists of a single horizontal line, typically between column headers and data rows.");
		pgd.setParamLabel("verticalSingleLineGrid", "Vertical Single-Line-Grid Tables");
		pgd.setParamDescription("verticalSingleLineGrid", "Indicate to expect tables whose grid consists of a single vertical line, typically between row labels and data columns.");
		pgd.setParamLabel("minColumnMargin", "Minimum Column Margin");
		pgd.setParamDescription("minColumnMargin", "The minimum margin (in pixels) to expect between table columns (normalized to 72 DPI).");
		pgd.setParamLabel("minRowMargin", "Minimum Row Margin");
		pgd.setParamDescription("minRowMargin", "The minimum margin (in pixels) to expect between table rows (normalized to 72 DPI).");
		DocumentStyle.addParameterGroupDescription(pgd);
		
		//	headings in general ...
		pgd = new ParameterGroupDescription("style.heading");
		pgd.setLabel("Headings In General");
		pgd.setDescription("Parameters describing headings in general, in particular the number of heading levels.");
		pgd.setParamLabel("levels", "Heading Levels");
		pgd.setParamDescription("levels", "The heading levels, i.e., a space-separated list of the level numbers, starting with 1.");
		DocumentStyle.addParameterGroupDescription(pgd);
		
		//	... and level-specific
		String[] alignments = {ImRegion.TEXT_ORIENTATION_LEFT, ImRegion.TEXT_ORIENTATION_RIGHT, ImRegion.TEXT_ORIENTATION_CENTERED, ImRegion.TEXT_ORIENTATION_JUSTIFIED};
		for (int l = 1; l <= 7; l++) {
			pgd = new TestableParameterGroupDescription("style.heading." + l);
			pgd.setLabel("Level " + l + " Headings");
			pgd.setDescription("Parameters describing headings of level " + l + ".");
			pgd.setParamLabel("alignment", "Paragraph Alignment");
			pgd.setParamDescription("alignment", ("The alignment of paragraphs representing headings of level " + l + "."));
			pgd.setParamValues("alignment", alignments);
			pgd.setParamLabel("containsAllCaps", "Contains All-Caps?");
			pgd.setParamDescription("containsAllCaps", ("Do headings of level " + l + " necessarily include words in all-caps?"));
			pgd.setParamLabel("containsBold", "Contains Bold?");
			pgd.setParamDescription("containsBold", ("Do headings of level " + l + " necessarily include words in bold face?"));
			pgd.setParamLabel("containsItalics", "Contains Italics?");
			pgd.setParamDescription("containsItalics", ("Do headings of level " + l + " necessarily include words in italics?"));
			pgd.setParamLabel("startIsAllCaps", "Start In All-Caps?");
			pgd.setParamDescription("startIsAllCaps", ("Do headings of level " + l + " necessarily start with words in all-caps?"));
			pgd.setParamLabel("startIsBold", "Start In Bold?");
			pgd.setParamDescription("startIsBold", ("Do headings of level " + l + " necessarily start with words in bold face?"));
			pgd.setParamLabel("isAllCaps", "Completely All-Caps?");
			pgd.setParamDescription("isAllCaps", ("Are headings of level " + l + " completely in all-caps?"));
			pgd.setParamLabel("isBold", "Completely Bold?");
			pgd.setParamDescription("isBold", ("Are headings of level " + l + " completely in bold face?"));
			pgd.setParamLabel("isNonBlock", "Can Share Block?");
			pgd.setParamDescription("isNonBlock", ("Can headings of level " + l + " be in one text block together with subsequent document text, or do these headings always sit in a text block of their own?"));
			pgd.setParamLabel("fontSize", "Font Size");
			pgd.setParamDescription("fontSize", ("The exact font size of headings of level " + l + " (use minimum and maximum font size if variation present or to be expected)."));
			pgd.setParamLabel("maxFontSize", "Maximum Font Size");
			pgd.setParamDescription("maxFontSize", ("The maximum font size of headings of level " + l + " (use only if variation present or to be expected, otherwise use exact font size)."));
			pgd.setParamLabel("minFontSize", "Minimum Font Size");
			pgd.setParamDescription("minFontSize", ("The minimum font size of headings of level " + l + " (use only if variation present or to be expected, otherwise use exact font size)."));
			pgd.setParamLabel("maxLineCount", "Maximum Number Of Lines");
			pgd.setParamDescription("maxLineCount", ("The maximum number of lines contained in headings of level " + l + "."));
			pgd.setParamLabel("startPatterns", "Start Patterns");
			pgd.setParamDescription("startPatterns", ("One or more regular expression patterns matching the starts of headings of level " + l + "; best suited to matching on numbered headings."));
			DocumentStyle.addParameterGroupDescription(pgd);
		}
	}
	
	private static class TestableParameterGroupDescription extends ParameterGroupDescription implements DisplayExtension {
		TestableParameterGroupDescription(String pnp) {
			super(pnp);
		}
		public boolean isActive() {
			return true;
		}
		public DisplayExtensionGraphics[] getExtensionGraphics(ImPage page, ImDocumentMarkupPanel idmp) {
			DocumentStyle docStyle = DocumentStyle.getStyleFor(idmp.document);
			DocumentStyle style = docStyle.getSubset(this.parameterNamePrefix);
			if (style.isEmpty())
				return DisplayExtensionProvider.NO_DISPLAY_EXTENSION_GRAPHICS;
			
			//	collect any highlights, and set color
			ArrayList shapes = new ArrayList();
			Color color = null;
			if (this.parameterNamePrefix.startsWith("style.heading.")) {
				this.addHeadingShapes(page, docStyle, shapes);
				color = idmp.getAnnotationColor(HEADING_TYPE);
			}
			else if ("layout.caption".equals(this.parameterNamePrefix)) {
				this.addCaptionShapes(page, style, shapes);
				color = idmp.getTextStreamTypeColor(ImWord.TEXT_STREAM_TYPE_CAPTION);
			}
			else if ("layout.footnote".equals(this.parameterNamePrefix)) {
				this.addFootnoteShapes(page, style, shapes);
				color = idmp.getTextStreamTypeColor(ImWord.TEXT_STREAM_TYPE_FOOTNOTE);
			}
			
			//	anything to show?
			if (shapes.size() == 0)
				return DisplayExtensionProvider.NO_DISPLAY_EXTENSION_GRAPHICS;
			
			//	determine line and fill color
			Color degLineColor = ((color == null) ? matchLineColor : color);
			Color degFillColor = ((color == null) ? matchFillColor : new Color(degLineColor.getRed(), degLineColor.getGreen(), degLineColor.getBlue(), 64));
			
			//	create highlight graphics
			DisplayExtensionGraphics[] degs = {new DisplayExtensionGraphics(this, null, page, ((Shape[]) shapes.toArray(new Shape[shapes.size()])), degLineColor, matchLineStroke, degFillColor) {
				public boolean isActive() {
					return true;
				}
			}};
			return degs;
		}
		private void addHeadingShapes(ImPage page, DocumentStyle docStyle, ArrayList shapes) {
			int level = Integer.parseInt(this.parameterNamePrefix.substring("style.heading.".length()));
			HeadingStyleDefined hsd = new HeadingStyleDefined(level, docStyle.getSubset("style.heading"));
			ImRegion[] pageBlocks = page.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
			for (int b = 0; b < pageBlocks.length; b++) {
				ImRegion[] blockParentColumns = page.getRegionsIncluding(ImRegion.COLUMN_ANNOTATION_TYPE, pageBlocks[b].bounds, false);
				BoundingBox blockParentColumnBounds = ((blockParentColumns.length == 0) ? pageBlocks[b].bounds : blockParentColumns[0].bounds);
				ImRegion[] blockLines = pageBlocks[b].getRegions(ImRegion.LINE_ANNOTATION_TYPE);
				if (blockLines.length == 0)
					continue;
				Arrays.sort(blockLines, ImUtils.topDownOrder);
				
				//	measure line positions
				ImWord[][] lineWords = new ImWord[blockLines.length][];
				boolean[] lineIsFlushLeft = new boolean[blockLines.length];
				boolean[] lineIsFlushRight = new boolean[blockLines.length];
				for (int l = 0; l < blockLines.length; l++) {
					lineWords[l] = getLargestTextStreamWords(blockLines[l].getWords());
					if (lineWords[l].length == 0)
						continue;
					Arrays.sort(lineWords[l], ImUtils.textStreamOrder);
					if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(lineWords[l][0].getTextStreamType()))
						continue;
					
					//	assess line position and length (center alignment against surrounding column to catch short blocks)
					int leftDist = (blockLines[l].bounds.left - blockParentColumnBounds.left);
					lineIsFlushLeft[l] = ((leftDist * 25) < page.getImageDPI());
					int rightDist = (blockParentColumnBounds.right - blockLines[l].bounds.right);
					lineIsFlushRight[l] = ((rightDist * 25) < page.getImageDPI());
					
					//	no use looking for headings more than 3 lines down the block
					if (l == 3)
						break;
				}
				
				//	find headings (first three lines at most)
				HeadingStyleDefined[] lineHeadingStyles = new HeadingStyleDefined[blockLines.length]; 
				Arrays.fill(lineHeadingStyles, null);
				for (int l = 0; l < Math.min(blockLines.length, 4); l++) {
					if (lineWords[l] == null)
						break;
					if (lineWords[l].length == 0)
						continue;
					if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(lineWords[l][0].getTextStreamType()))
						continue;
					
					//	check against to-visualize style
					if (hsd.matches(blockLines[l], lineWords[l], l, blockLines.length, lineIsFlushLeft[l], lineIsFlushRight[l]))
						lineHeadingStyles[l] = hsd;
				}
				
				//	highlight headings (subsequent lines with same heading style together, unless line is short)
				int headingStartLineIndex = -1;
				for (int l = 0; l < Math.min(blockLines.length, 4); l++) {
					if (lineWords[l].length == 0)
						continue;
					if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(lineWords[l][0].getTextStreamType()))
						continue;
					if (lineHeadingStyles[l] == null) {
						if (headingStartLineIndex != -1) {
							BoundingBox hb = ImLayoutObject.getAggregateBox(blockLines, headingStartLineIndex, l);
							shapes.add(new Rectangle2D.Float(hb.left, hb.top, hb.getWidth(), hb.getHeight()));
						}
						headingStartLineIndex = -1;
					}
					else if (headingStartLineIndex == -1)
						headingStartLineIndex = l;
				}
				if (headingStartLineIndex != -1) {
					BoundingBox hb = ImLayoutObject.getAggregateBox(blockLines, headingStartLineIndex, Math.min(blockLines.length, 4));
					shapes.add(new Rectangle2D.Float(hb.left, hb.top, hb.getWidth(), hb.getHeight()));
				}
			}
		}
		private void addCaptionShapes(ImPage page, DocumentStyle style, ArrayList shapes) {
			boolean startIsBold = style.getBooleanProperty("startIsBold", false);
			int fontSize = style.getIntProperty("fontSize", -1);
			int minFontSize = style.getIntProperty("minFontSize", ((fontSize == -1) ? 0 : fontSize));
			int maxFontSize = style.getIntProperty("maxFontSize", ((fontSize == -1) ? 72 : fontSize));
			String[] startPatternStrs = style.getListProperty("startPatterns", null, " ");
			String[] figureStartPatternStrs = style.getListProperty("figureStartPatterns", null, " ");
			String[] tableStartPatternStrs = style.getListProperty("tableStartPatterns", null, " ");
			String[] plateStartPatternStrs = style.getListProperty("plateStartPatterns", null, " ");
			String[] platePartStartPatternStrs = style.getListProperty("platePartStartPatterns", null, " ");
			CaptionStartPattern[] startPatterns;
			if ((figureStartPatternStrs != null) || (tableStartPatternStrs != null) || (plateStartPatternStrs != null)) {
				ArrayList captionStartPatternList = new ArrayList();
				captionStartPatternList.addAll(Arrays.asList(CaptionStartPattern.createPatterns(figureStartPatternStrs, CaptionStartPattern.FIGURE_TYPE, true)));
				captionStartPatternList.addAll(Arrays.asList(CaptionStartPattern.createPatterns(tableStartPatternStrs, CaptionStartPattern.TABLE_TYPE, true)));
				captionStartPatternList.addAll(Arrays.asList(CaptionStartPattern.createPatterns(plateStartPatternStrs, CaptionStartPattern.PLATE_TYPE, true)));
				captionStartPatternList.addAll(Arrays.asList(CaptionStartPattern.createPatterns(platePartStartPatternStrs, CaptionStartPattern.PLATE_PART_TYPE, true)));
				startPatterns = ((CaptionStartPattern[]) captionStartPatternList.toArray(new CaptionStartPattern[captionStartPatternList.size()]));			
			}
			else if (startPatternStrs != null)
				startPatterns = CaptionStartPattern.createPatterns(startPatternStrs, CaptionStartPattern.GENERIC_TYPE, true);
			else startPatterns = CaptionStartPattern.createPatterns("[^\\s]+".split("\\;"), CaptionStartPattern.GENERIC_TYPE, true);
			
			ImRegion[] pageBlocks = page.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
			ImRegion[] pageParagraphs = page.getRegions(ImRegion.PARAGRAPH_TYPE);
			for (int b = 0; b < pageBlocks.length; b++) {
				HashMap blockCaptionsByBounds = markBlockCaptions(null, pageBlocks[b], pageParagraphs, startPatterns, new CaptionStartPattern[0], minFontSize, maxFontSize, startIsBold, ProgressMonitor.silent);
				if (blockCaptionsByBounds == null)
					continue;
				for (Iterator cbit = blockCaptionsByBounds.keySet().iterator(); cbit.hasNext();) {
					BoundingBox cb = ((BoundingBox) cbit.next());
					shapes.add(new Rectangle2D.Float(cb.left, cb.top, cb.getWidth(), cb.getHeight()));
				}
			}
		}
		private void addFootnoteShapes(ImPage page, DocumentStyle style, ArrayList shapes) {
			int fontSize = style.getIntProperty("fontSize", -1);
			int minFontSize = style.getIntProperty("minFontSize", ((fontSize == -1) ? 0 : fontSize));
			int maxFontSize = style.getIntProperty("maxFontSize", ((fontSize == -1) ? 72 : fontSize));
			String[] startPatternStrs = style.getListProperty("startPatterns", "[^\\s]+".split("\\;"), " ");
			Pattern[] startPatterns = new Pattern[startPatternStrs.length];
			for (int p = 0; p < startPatternStrs.length; p++)
				startPatterns[p] = Pattern.compile(startPatternStrs[p]);
			BoundingBox area = style.getBoxProperty("area", page.bounds, page.getImageDPI());
			
			ImRegion[] pageParagraphs = page.getRegions(ImRegion.PARAGRAPH_TYPE);
			int pageFontSize = getAverageFontSize(page);
			Arrays.sort(pageParagraphs, ImUtils.topDownOrder);
			for (int p = (pageParagraphs.length - 1); p >= 0; p--) {
				if (pageParagraphs[p].bounds.bottom < area.top)
					break; // since we're going bottom-up, there cannot be any area-wise candidate following
				if (!area.includes(pageParagraphs[p].bounds, false))
					continue; // not fully in bounds
				ImWord[] paragraphWords = pageParagraphs[p].getWords();
				if (paragraphWords.length == 0)
					continue;
				if (isFootnote(pageParagraphs[p], paragraphWords, pageFontSize, startPatterns, minFontSize, maxFontSize))
					shapes.add(new Rectangle2D.Float(pageParagraphs[p].bounds.left, pageParagraphs[p].bounds.top, pageParagraphs[p].bounds.getWidth(), pageParagraphs[p].bounds.getHeight()));
			}
		}
		private static final Color matchLineColor = Color.BLUE;
		private static final BasicStroke matchLineStroke = new BasicStroke(3);
		private static final Color matchFillColor = new Color(matchLineColor.getRed(), matchLineColor.getGreen(), matchLineColor.getBlue(), 64);
	}
	
	private static class MarginParameterDescription extends ParameterDescription implements DisplayExtension {
		private final boolean isHorizontal;
		MarginParameterDescription(String fpn) {
			super(fpn);
			this.isHorizontal = fpn.endsWith("BlockMargin");
		}
		public boolean isActive() {
			return true;
		}
		public DisplayExtensionGraphics[] getExtensionGraphics(ImPage page, ImDocumentMarkupPanel idmp) {
			DocumentStyle docStyle = DocumentStyle.getStyleFor(idmp.document);
			DocumentStyle layoutStyle = docStyle.getSubset("layout");
			int minMargin = layoutStyle.getIntProperty(this.localName, -1, page.getImageDPI());
			if (minMargin < 0)
				return DisplayExtensionProvider.NO_DISPLAY_EXTENSION_GRAPHICS;
			
			//	get lines
			ImRegion[] lines = page.getRegions(ImRegion.LINE_ANNOTATION_TYPE);
			if (lines.length < 2) // no such thing as a margin with fewer than two lines ...
				return DisplayExtensionProvider.NO_DISPLAY_EXTENSION_GRAPHICS;
			Arrays.sort(lines, ImUtils.topDownOrder);
			
			//	extract current matches from page and highlight them
			ArrayList shapes = new ArrayList();
			int firstFitBottom;
			for (int l = 0; l < lines.length; l++) {
				firstFitBottom = -1;
				for (int cl = (l+1); cl < lines.length; cl++) {
					if (this.isHorizontal) {
						if (lines[l].bounds.right <= lines[cl].bounds.left)
							continue; // off to left
						if (lines[cl].bounds.right <= lines[l].bounds.left)
							continue; // off to right
						if (firstFitBottom == -1)
							firstFitBottom = lines[cl].bounds.bottom;
						else if (lines[cl].bounds.top >= firstFitBottom)
							break;
						int margin = (lines[cl].bounds.top - lines[l].bounds.bottom);
						if (minMargin <= margin) {
							int mLeft = Math.max(lines[l].bounds.left, lines[cl].bounds.left);
							int mRight = Math.min(lines[l].bounds.right, lines[cl].bounds.right);
							shapes.add(new Rectangle2D.Float(mLeft, lines[l].bounds.bottom, (mRight - mLeft), margin));
						}
					}
					else {
						if (lines[l].bounds.bottom <= lines[cl].bounds.top)
							break; // below, nothing more to come
						int margin = -1;
						if (lines[l].bounds.right <= lines[cl].bounds.left)
							margin = (lines[cl].bounds.left - lines[l].bounds.right); // off to left
						if (lines[cl].bounds.right <= lines[l].bounds.left)
							margin = (lines[l].bounds.left - lines[cl].bounds.right); // off to right
						if (minMargin <= margin)
							shapes.add(new Rectangle2D.Float(Math.min(lines[l].bounds.right, lines[cl].bounds.right), lines[cl].bounds.top, margin, (lines[l].bounds.bottom - lines[cl].bounds.top)));
					}
				}
			}
			if (shapes.size() == 0)
				return DisplayExtensionProvider.NO_DISPLAY_EXTENSION_GRAPHICS;
			DisplayExtensionGraphics[] degs = {new DisplayExtensionGraphics(this, null, page, ((Shape[]) shapes.toArray(new Shape[shapes.size()])), null, null, marginFillColor) {
				public boolean isActive() {
					return true;
				}
			}};
			return degs;
		}
		private static final Color marginBaseColor = Color.RED;
		private static final Color marginFillColor = new Color(marginBaseColor.getRed(), marginBaseColor.getGreen(), marginBaseColor.getBlue(), 128);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider#getToolsMenuItemNames()
	 */
	public String[] getToolsMenuItemNames() {
		String[] tmins = {STRUCTURE_DETECTOR_IMT_NAME};
		return tmins;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		if (STRUCTURE_DETECTOR_IMT_NAME.equals(name))
			return this.structureDetector;
		else return null;
	}
	
	private class StructureDetector implements ImageMarkupTool {
		public String getLabel() {
			return "Detect Document Structure";
		}
		public String getTooltip() {
			return "Detect page headers, page numbers, captions, and footnotes, etc.";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			if (annot == null) {
				boolean documentBornDigital;
				if (idmp != null)
					documentBornDigital = idmp.documentBornDigital;
				else {
					ImPage[] pages = doc.getPages();
					int wordCount = 0;
					int fnWordCount = 0;
					for (int p = 0; p < pages.length; p++) {
						ImWord[] pageWords = pages[p].getWords();
						for (int w = 0; w < pageWords.length; w++) {
							wordCount++;
							if (pageWords[w].hasAttribute(ImWord.FONT_NAME_ATTRIBUTE))
								fnWordCount++;
						}
					}
					documentBornDigital = ((wordCount * 2) < (fnWordCount * 3));
				}
				detectDocumentStructure(doc, documentBornDigital, pm);
			}
			else pm.setStep("Cannot detect document structure on single annotation");
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#typeChanged(de.uka.ipd.idaho.im.ImObject, java.lang.String, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void typeChanged(ImObject object, String oldType, ImDocumentMarkupPanel idmp, boolean allowPrompt) { /* we're not into this */ }
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#attributeChanged(de.uka.ipd.idaho.im.ImObject, java.lang.String, java.lang.Object, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void attributeChanged(ImObject object, String attributeName, Object oldValue, ImDocumentMarkupPanel idmp, boolean allowPrompt) { /* we're not into this */ }
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#regionAdded(de.uka.ipd.idaho.im.ImRegion, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void regionAdded(ImRegion region, ImDocumentMarkupPanel idmp, boolean allowPrompt) { /* we're not into this */ }
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#regionRemoved(de.uka.ipd.idaho.im.ImRegion, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void regionRemoved(ImRegion region, ImDocumentMarkupPanel idmp, boolean allowPrompt) { /* we're not into this */ }
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#annotationAdded(de.uka.ipd.idaho.im.ImAnnotation, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void annotationAdded(ImAnnotation annotation, ImDocumentMarkupPanel idmp, boolean allowPrompt) {
		
		//	we're only interested in captions
		if (!CAPTION_TYPE.equals(annotation.getType()))
			return;
		
		//	add any sub caption starts ...
		if (addPlateSubCaptionPatternStarts(annotation, this.captionStartPatterns, null))
			
			//	... and have citation handler process any findings (get called for reaction before us ...)
			this.captionCitationHandler.annotationAdded(annotation, idmp, allowPrompt);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#annotationRemoved(de.uka.ipd.idaho.im.ImAnnotation, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void annotationRemoved(ImAnnotation annotation, ImDocumentMarkupPanel idmp, boolean allowPrompt) { /* we're not into this */ }
	
	/**
	 * Detect and mark the structure of a document. In particular, this method
	 * identifies page numbers, page headers, captions, footnotes, and tables.
	 * If the <code>documentBornDigital</code> argument is set to false, page
	 * number detection considers a wide range of characters to potentially be
	 * page number digits that have been mis-recognized by OCR.
	 * @param doc the document whose structure to detect
	 * @param documentBornDigital is the document born digital or scanned?
	 * @param spm a progress monitor observing document processing
	 */
	public void detectDocumentStructure(final ImDocument doc, boolean documentBornDigital, ProgressMonitor pm) {
		
		//	build progress monitor with synchronized methods instead of synchronizing in-code
		final ProgressMonitor spm = new SynchronizedProgressMonitor(pm);
		
		//	get pages
		final ImPage[] pages = doc.getPages();
		spm.setStep("Detecting document structure from " + pages.length + " pages");
		
		//	compute average resolution
		int pageImageDpiSum = 0;
		for (int p = 0; p < pages.length; p++)
			pageImageDpiSum += pages[p].getImageDPI();
		final int pageImageDpi = ((pageImageDpiSum + (pages.length / 2)) / pages.length);
		spm.setInfo(" - resolution is " + pageImageDpi);
		
		//	get document style
		DocumentStyle docStyle = DocumentStyle.getStyleFor(doc);
		final DocumentStyle docLayout = docStyle.getSubset("layout");
		
		//	collect blocks adjacent to page edge (top or bottom) from each page
		spm.setStep(" - gathering data");
		spm.setBaseProgress(0);
		spm.setMaxProgress(10);
		final PageData[] pageData = new PageData[pages.length];
		final BoundingBox[][] pageHeaderAreas = new BoundingBox[pages.length][];
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int p) throws Exception {
				spm.setProgress((p * 100) / pages.length);
				
				//	get header areas (default to general page layout if page class layout not given)
				String layoutPrefix;
				if (p == 0)
					layoutPrefix = "first.";
				else if ((p % 2) == 0) // first page is odd, so we need to decide 1-based, not 0-based
					layoutPrefix = "odd.";
				else layoutPrefix = "even."; // first page is odd, so we need to decide 1-based, not 0-based
				pageHeaderAreas[p] = docLayout.getBoxListProperty(("page." + layoutPrefix + "headerAreas"), docLayout.getBoxListProperty(("page.headerAreas"), null, pageImageDpi), pageImageDpi);
				
				//	go collect header areas
				pageData[p] = getPageData(pages[p], pageImageDpi, pageHeaderAreas[p], spm);
			}
		}, pages.length, (this.debug ? 1 : (pages.length / 8)));
		
		/* TODO re-detect document structure (starting all the way from columns and blocks):
		 * - use margins, table grid schemes, etc. from journal style
		 * - use minimum column widths to avoid splitting numbers off keys, etc.
		 * OR
		 * - determine minimum block width and column margins on the fly from document ...
		 * - ... and re-merge side-by-side pairs of blocks that (a) have similar height, (b) add up to column width, and (c) have narrower column on left (e.g. pages 8, 9, 10 in Moore & Gosliner 2014.pdf.imf)
		 * - use detected minimum margin also on flat (up to 3 lines) sets of block that add up to page width and might have been split up due to wide word gaps 
		 * - do that before detecting captions, as it helps repair flat captions (e.g. page 3 in Moore & Gosliner 2014.pdf.imf)
		 */
		
		//	collect words from top and bottom regions, separate for even and odd pages
		final CountingSet topWordsOdd = new CountingSet(String.CASE_INSENSITIVE_ORDER);
		final CountingSet topWordsEven = new CountingSet(String.CASE_INSENSITIVE_ORDER);
		final CountingSet bottomWordsOdd = new CountingSet(String.CASE_INSENSITIVE_ORDER);
		final CountingSet bottomWordsEven = new CountingSet(String.CASE_INSENSITIVE_ORDER);
		final CountingSet allWordsOdd = new CountingSet(String.CASE_INSENSITIVE_ORDER);
		final CountingSet allWordsEven = new CountingSet(String.CASE_INSENSITIVE_ORDER);
		
		//	collect words that might be (parts of) page numbers
		String pageNumberPartPattern = (documentBornDigital ? this.pageNumberPattern : this.ocrPageNumberPartPattern);
		
		//	collect the words, and find possible page numbers
		spm.setBaseProgress(10);
		spm.setMaxProgress(20);
		for (int p = 0; p < pages.length; p++) {
			spm.setProgress((p * 100) / pages.length);
			ArrayList pageNumberParts = new ArrayList();
			HashSet pageBorderWordIDs = new HashSet();
			
			//	get page number area and font properties (default to general page layout if page class layout not given)
			String layoutPrefix;
			if (p == 0)
				layoutPrefix = "first.";
			else if ((p % 2) == 0) // first page is odd, so we need to decide 1-based, not 0-based
				layoutPrefix = "odd.";
			else layoutPrefix = "even."; // first page is odd, so we need to decide 1-based, not 0-based
			BoundingBox pageNumberArea = docLayout.getBoxProperty(("page." + layoutPrefix + "number.area"), docLayout.getBoxProperty(("page.number.area"), null, pageImageDpi), pageImageDpi);
			int pageNumberFontSize = docLayout.getIntProperty(("page." + layoutPrefix + "number.fontSize"), docLayout.getIntProperty(("page.number.fontSize"), -1));
			boolean pageNumberIsBold = docLayout.getBooleanProperty(("page." + layoutPrefix + "number.isBold"), docLayout.getBooleanProperty(("page.number.isBold"), false));
			pageNumberPartPattern = docLayout.getProperty(("page." + layoutPrefix + "number.pattern"), docLayout.getProperty(("page.number.pattern"), pageNumberPartPattern));
			
			//	words from page top candidates
			TreeSet topWords = new TreeSet(String.CASE_INSENSITIVE_ORDER);
			for (int r = 0; r < pageData[p].topRegions.size(); r++) {
				ImWord[] words = ((ImRegion) pageData[p].topRegions.get(r)).getWords();
				ImUtils.sortLeftRightTopDown(words);
				for (int w = 0; w < words.length; w++) {
					if (!pageBorderWordIDs.add(words[w].getLocalID()))
						continue;
					if (pageHeaderAreas[p] == null)
						topWords.add(words[w].getString());
					if ((pageNumberArea != null) && !pageNumberArea.includes(words[w].bounds, true))
						continue;
					if (pageNumberIsBold && !words[w].hasAttribute(ImWord.BOLD_ATTRIBUTE))
						continue;
					if ((pageNumberFontSize != -1) && (pageNumberFontSize != words[w].getFontSize()))
						continue;
					if (words[w].getString().matches(pageNumberPartPattern))
						pageNumberParts.add(words[w]);
				}
			}
			(((p % 2) == 0) ? topWordsEven : topWordsOdd).addAll(topWords);
			
			//	words from page bottom candidates
			TreeSet bottomWords = new TreeSet(String.CASE_INSENSITIVE_ORDER);
			for (int r = 0; r < pageData[p].bottomRegions.size(); r++) {
				ImWord[] words = ((ImRegion) pageData[p].bottomRegions.get(r)).getWords();
				ImUtils.sortLeftRightTopDown(words);
				for (int w = 0; w < words.length; w++) {
					if (!pageBorderWordIDs.add(words[w].getLocalID()))
						continue;
					if (pageHeaderAreas[p] == null)
						bottomWords.add(words[w].getString());
					if ((pageNumberArea != null) && !pageNumberArea.includes(words[w].bounds, true))
						continue;
					if (pageNumberIsBold && !words[w].hasAttribute(ImWord.BOLD_ATTRIBUTE))
						continue;
					if ((pageNumberFontSize != -1) && (pageNumberFontSize != words[w].getFontSize()))
						continue;
					if (words[w].getString().matches(pageNumberPartPattern))
						pageNumberParts.add(words[w]);
				}
			}
			(((p % 2) == 0) ? bottomWordsEven : bottomWordsOdd).addAll(bottomWords);
			
			//	overall words for comparison (only necessary if we don't know where page headers are, though)
			if (pageHeaderAreas[p] == null) {
				TreeSet allWords = new TreeSet(String.CASE_INSENSITIVE_ORDER);
				ImWord[] words = pages[p].getWords();
				for (int w = 0; w < words.length; w++)
					allWords.add(words[w].getString());
				(((p % 2) == 0) ? allWordsEven : allWordsOdd).addAll(allWords);
			}
			
			//	build candidate page numbers from collected parts
			ImUtils.sortLeftRightTopDown(pageNumberParts);
			if (documentBornDigital) {
				spm.setInfo(" - got " + pageNumberParts.size() + " possible page numbers for page " + p + ":");
				for (int w = 0; w < pageNumberParts.size(); w++)  try {
					pageData[p].pageNumberCandidates.add(new PageNumber((ImWord) pageNumberParts.get(w)));
					spm.setInfo("   - " + ((ImWord) pageNumberParts.get(w)).getString());
				} catch (NumberFormatException nfe) {}
			}
			else {
				spm.setInfo(" - got " + pageNumberParts.size() + " parts of possible page numbers:");
				HashSet pageNumberPartIDs = new HashSet();
				for (int w = 0; w < pageNumberParts.size(); w++)
					pageNumberPartIDs.add(((ImWord) pageNumberParts.get(w)).getLocalID());
				for (int w = 0; w < pageNumberParts.size(); w++) {
					spm.setInfo("   - " + ((ImWord) pageNumberParts.get(w)).getString());
					addPageNumberCandidates(pageData[p].pageNumberCandidates, ((ImWord) pageNumberParts.get(w)), pageNumberPartIDs, pageImageDpi, spm);
					Collections.sort(pageData[p].pageNumberCandidates, pageNumberValueOrder);
				}
			}
		}
		
		//	score and select page numbers for each page
		spm.setStep(" - scoring page numbers:");
		this.scoreAndSelectPageNumbers(pageData, spm);
		
		//	check page number sequence
		spm.setStep(" - checking page number sequence:");
		this.checkPageNumberSequence(pageData, spm);
		
		//	fill in missing page numbers
		spm.setStep(" - filling in missing page numbers:");
		this.fillInMissingPageNumbers(pageData, spm);
		
		//	annotate page numbers, collecting page number words
		final HashSet pageNumberWordIDs = new HashSet();
		for (int p = 0; p < pageData.length; p++)
			this.annotatePageNumbers(pageData[p], pageNumberWordIDs);
		
		//	judge on page headers based on frequent words and on page numbers
		spm.setStep(" - detecting page headers");
		spm.setBaseProgress(20);
		spm.setMaxProgress(25);
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int p) throws Exception {
				spm.setProgress((p * 100) / pages.length);
				
				//	use heuristics for page top regions
				for (int r = 0; r < pageData[p].topRegions.size(); r++) {
					ImRegion topRegion = ((ImRegion) pageData[p].topRegions.get(r));
					ImWord[] regionWords = topRegion.getWords();
					Arrays.sort(regionWords, ImUtils.textStreamOrder);
					if ((regionWords.length == 0) || ImWord.TEXT_STREAM_TYPE_PAGE_TITLE.equals(regionWords[0].getTextStreamType()))
						continue;
					spm.setInfo("Testing " + topRegion.getType() + "@" + topRegion.bounds + " for top page header" + (regionWords[0].getTextStreamId().equals(regionWords[regionWords.length - 1].getTextStreamId()) ? (": " + ImUtils.getString(regionWords[0], regionWords[regionWords.length - 1], true)) : ""));
					if ((pageHeaderAreas[p] != null) || isPageTitle(topRegion, regionWords, (pages.length / 2), (((p % 2) == 0) ? topWordsEven : topWordsOdd), (((p % 2) == 0) ? allWordsEven : allWordsOdd), pageNumberWordIDs)) {
						spm.setInfo(" ==> page header found");
						synchronized (doc) {
							ImUtils.makeStream(regionWords, ImWord.TEXT_STREAM_TYPE_PAGE_TITLE, PAGE_TITLE_TYPE);
							ImUtils.orderStream(regionWords, ImUtils.leftRightTopDownOrder);
						}
					}
					else spm.setInfo(" ==> not a page header");
				}
				
				//	use heuristics for page bottom regions
				for (int r = 0; r < pageData[p].bottomRegions.size(); r++) {
					ImRegion bottomRegion = ((ImRegion) pageData[p].bottomRegions.get(r));
					ImWord[] regionWords = bottomRegion.getWords();
					Arrays.sort(regionWords, ImUtils.textStreamOrder);
					if ((regionWords.length == 0) || ImWord.TEXT_STREAM_TYPE_PAGE_TITLE.equals(regionWords[0].getTextStreamType()))
						continue;
					spm.setInfo("Testing " + bottomRegion.getType() + "@" + bottomRegion.bounds + " for bottom page header" + (regionWords[0].getTextStreamId().equals(regionWords[regionWords.length - 1].getTextStreamId()) ? (": " + ImUtils.getString(regionWords[0], regionWords[regionWords.length - 1], true)) : ""));
					if ((pageHeaderAreas[p] != null) || isPageTitle(bottomRegion, regionWords, (pages.length / 2), (((p % 2) == 0) ? bottomWordsEven : bottomWordsOdd), (((p % 2) == 0) ? allWordsEven : allWordsOdd), pageNumberWordIDs)) {
						spm.setInfo(" ==> page header found");
						synchronized (doc) {
							ImUtils.makeStream(regionWords, ImWord.TEXT_STREAM_TYPE_PAGE_TITLE, PAGE_TITLE_TYPE);
							ImUtils.orderStream(regionWords, ImUtils.leftRightTopDownOrder);
						}
					}
					else spm.setInfo(" ==> not a page header");
				}
			}
		}, pages.length, (this.debug ? 1 : (pages.length / 8)));
		
		//	turn empty tables into regular blocks
		spm.setStep(" - correcting empty tables");
		spm.setBaseProgress(25);
		spm.setMaxProgress(30);
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int p) throws Exception {
				spm.setProgress((p * 100) / pages.length);
				ImRegion[] pageTables = pages[p].getRegions(ImRegion.TABLE_TYPE);
				for (int t = 0; t < pageTables.length; t++) {
					ImWord[] tableWords = pageTables[t].getWords();
					if (tableWords.length == 0)
						pageTables[t].setType(ImRegion.BLOCK_ANNOTATION_TYPE);
				}
			}
		}, pages.length, (this.debug ? 1 : (pages.length / 8)));
		
		//	get table layout hints, defaulting to somewhat universal ballpark figures
		DocumentStyle tableLayout = docLayout.getSubset(ImRegion.TABLE_TYPE);
//		final int minTableColMargin = tableLayout.getIntProperty("minColumnMargin", (pageImageDpi / 30), pageImageDpi);
//		final int minTableRowMargin = tableLayout.getIntProperty("minRowMargin", (pageImageDpi / 50), pageImageDpi);
		
		//	detect tables
		spm.setStep(" - detecting tables");
		spm.setBaseProgress(30);
		spm.setMaxProgress(40);
		this.tableDetectorProvider.detectTables(doc, tableLayout, null, spm);
		
		//	detect artifacts (blocks with word coverage below 10%, only for scanned documents)
		if (!documentBornDigital) {
			spm.setStep(" - detecting OCR artifacts in images");
			spm.setBaseProgress(40);
			spm.setMaxProgress(45);
			ParallelJobRunner.runParallelFor(new ParallelFor() {
				public void doFor(int p) throws Exception {
					spm.setProgress((p * 100) / pages.length);
					BoundingBox contentArea = docLayout.getBoxProperty("contentArea", pages[p].bounds, pageImageDpi);
					ImRegion[] pageBlocks = pages[p].getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
					for (int b = 0; b < pageBlocks.length; b++) {
						spm.setInfo("Testing block " + pageBlocks[b].bounds + " on page " + pages[p].pageId + " for artifact");
						ImWord[] blockWords = pageBlocks[b].getWords();
						if (!contentArea.includes(pageBlocks[b].bounds, false) || isArtifact(pageBlocks[b], blockWords, pageImageDpi)) {
							spm.setInfo(" ==> artifact detected");
							synchronized (doc) {
								ImUtils.makeStream(blockWords, ImWord.TEXT_STREAM_TYPE_ARTIFACT, null);
								ImUtils.orderStream(blockWords, ImUtils.leftRightTopDownOrder);
								for (ImWord imw = blockWords[blockWords.length-1]; imw != null; imw = imw.getPreviousWord())
									imw.setNextWord(null);
							}
						}
						else spm.setInfo(" ==> not an artifact");
					}
				}
			}, pages.length, (this.debug ? 1 : -1));
		}
		
		//	compute document-wide main text font size
		spm.setStep(" - computing main text font size");
		ImWord[] textStreamHeads = doc.getTextStreamHeads();
		int docFontSizeSum = 0;
		int docFontSizeWordCount = 0;
		for (int h = 0; h < textStreamHeads.length; h++) {
			if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(textStreamHeads[h].getTextStreamType()))
				continue;
			for (ImWord imw = textStreamHeads[h]; imw != null; imw = imw.getNextWord())
				if (imw.hasAttribute(ImWord.FONT_SIZE_ATTRIBUTE)) {
					docFontSizeSum += imw.getFontSize();
					docFontSizeWordCount++;
				}
		}
		final int docFontSize = ((docFontSizeWordCount == 0) ? -1 : ((docFontSizeSum + (docFontSizeWordCount / 2)) / docFontSizeWordCount));
		spm.setInfo("Average font size is " + docFontSize + " (based on " + docFontSizeWordCount + " main text words)");
		
		//	detect caption paragraphs
		//	TODO allow whole-block multi-paragraph captions via template parameter (default: false)
		spm.setStep(" - detecting captions");
		spm.setBaseProgress(documentBornDigital ? 40 : 45);
		spm.setMaxProgress(50);
		final HashMap[] pageCaptionsByBounds = new HashMap[pages.length];
		Arrays.fill(pageCaptionsByBounds, null);
		final boolean captionStartIsBold = docLayout.getBooleanProperty("caption.startIsBold", false);
		final int captionFontSize = docLayout.getIntProperty("caption.fontSize", -1);
		final int captionMinFontSize = docLayout.getIntProperty("caption.minFontSize", ((captionFontSize == -1) ? 0 : captionFontSize));
		final int captionMaxFontSize = docLayout.getIntProperty("caption.maxFontSize", ((captionFontSize == -1) ? 72 : captionFontSize));
		String[] captionStartPatternStrs = docLayout.getListProperty("caption.startPatterns", null, " ");
		String[] figureCaptionStartPatternStrs = docLayout.getListProperty("caption.figureStartPatterns", null, " ");
		String[] tableCaptionStartPatternStrs = docLayout.getListProperty("caption.tableStartPatterns", null, " ");
		String[] plateCaptionStartPatternStrs = docLayout.getListProperty("caption.plateStartPatterns", null, " ");
		String[] platePartCaptionStartPatternStrs = docLayout.getListProperty("caption.platePartStartPatterns", null, " ");
		final CaptionStartPattern[] captionStartPatterns;
		if ((figureCaptionStartPatternStrs != null) || (tableCaptionStartPatternStrs != null) || (plateCaptionStartPatternStrs != null)) {
			ArrayList captionStartPatternList = new ArrayList();
			captionStartPatternList.addAll(Arrays.asList(CaptionStartPattern.createPatterns(figureCaptionStartPatternStrs, CaptionStartPattern.FIGURE_TYPE, true)));
			captionStartPatternList.addAll(Arrays.asList(CaptionStartPattern.createPatterns(tableCaptionStartPatternStrs, CaptionStartPattern.TABLE_TYPE, true)));
			captionStartPatternList.addAll(Arrays.asList(CaptionStartPattern.createPatterns(plateCaptionStartPatternStrs, CaptionStartPattern.PLATE_TYPE, true)));
			captionStartPatternList.addAll(Arrays.asList(CaptionStartPattern.createPatterns(platePartCaptionStartPatternStrs, CaptionStartPattern.PLATE_PART_TYPE, true)));
			captionStartPatterns = ((CaptionStartPattern[]) captionStartPatternList.toArray(new CaptionStartPattern[captionStartPatternList.size()]));			
		}
		else if (captionStartPatternStrs != null)
			captionStartPatterns = CaptionStartPattern.createPatterns(captionStartPatternStrs, CaptionStartPattern.GENERIC_TYPE, true);
		else captionStartPatterns = this.captionStartPatterns;
//		if (captionStartPatternStrs == null)
//			captionStartPatterns = this.captionStartPatterns;
//		else {
//			captionStartPatterns = new Pattern[captionStartPatternStrs.length];
//			for (int p = 0; p < captionStartPatternStrs.length; p++)
//				captionStartPatterns[p] = Pattern.compile(captionStartPatternStrs[p]);
//		}
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int pg) throws Exception {
				spm.setProgress((pg * 100) / pages.length);
				ImRegion[] pageBlocks = pages[pg].getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
				ImRegion[] pageParagraphs = pages[pg].getRegions(ImRegion.PARAGRAPH_TYPE);
				for (int b = 0; b < pageBlocks.length; b++) {
					HashMap blockCaptionsByBounds = markBlockCaptions(doc, pageBlocks[b], pageParagraphs, captionStartPatterns, DocumentStructureDetectorProvider.this.captionStartPatterns, captionMinFontSize, captionMaxFontSize, captionStartIsBold, spm);
					if (blockCaptionsByBounds != null) {
						if (pageCaptionsByBounds[pg] == null)
							pageCaptionsByBounds[pg] = new HashMap();
						pageCaptionsByBounds[pg].putAll(blockCaptionsByBounds);
					}
					
					//	TODO add in-text notes residing between table and caption to caption proper
					//	TODO ==> specify (potential) target position above as booleans targetAbove and targetBelow
					//	TODO ==> make whole block into caption if first paragraph matches with target below
					//	TODO TEST: table in page 14 of EJT/ejt-431_boonyanusith_sanoamuang_brancelj.pdf.imd (EJT-testbed issue 274)
					//	TODO observe such notes when computing distance between table and caption on match-up
				}
			}
		}, pages.length, (this.debug ? 1 : (pages.length / 8)));
		
		//	detect footnote paragraphs
		//	TODO maybe require footnotes to form separate block of their own
		spm.setStep(" - detecting footnotes");
		spm.setBaseProgress(50);
		spm.setMaxProgress(55);
		final int footnoteFontSize = docLayout.getIntProperty("footnote.fontSize", -1);
		final int footnoteMinFontSize = docLayout.getIntProperty("footnote.minFontSize", ((footnoteFontSize == -1) ? 0 : footnoteFontSize));
		final int footnoteMaxFontSize = docLayout.getIntProperty("footnote.maxFontSize", ((footnoteFontSize == -1) ? 72 : footnoteFontSize));
		String[] footnoteStartPatternStrs = docLayout.getListProperty("footnote.startPatterns", null, " ");
		final Pattern[] footnoteStartPatterns;
		if (footnoteStartPatternStrs == null)
			footnoteStartPatterns = this.footnoteStartPatterns;
		else {
			footnoteStartPatterns = new Pattern[footnoteStartPatternStrs.length];
			for (int p = 0; p < footnoteStartPatternStrs.length; p++)
				footnoteStartPatterns[p] = Pattern.compile(footnoteStartPatternStrs[p]);
		}
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int pg) throws Exception {
				spm.setProgress((pg * 100) / pages.length);
				BoundingBox footnoteArea = docLayout.getBoxProperty("footnote.area", pages[pg].bounds, pageImageDpi);
				ImRegion[] pageParagraphs = pages[pg].getRegions(ImRegion.PARAGRAPH_TYPE);
				boolean newFootnoteFound;
				do {
					newFootnoteFound = false;
					for (int p = 0; p < pageParagraphs.length; p++) {
						if (!footnoteArea.includes(pageParagraphs[p].bounds, false))
							continue;
						ImWord[] paragraphWords = getLargestTextStreamWords(pageParagraphs[p].getWords());
						if (paragraphWords.length == 0)
							continue;
						Arrays.sort(paragraphWords, ImUtils.textStreamOrder);
						if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(paragraphWords[0].getTextStreamType()))
							continue;
						spm.setInfo("Testing paragraph " + pageParagraphs[p].bounds + " on page " + pages[pg].pageId + " for footnote");
						boolean nonFootnoteBelow = false;
						for (int cp = 0; cp < pageParagraphs.length; cp++) {
							if (cp == p)
								continue;
							if ((pageParagraphs[p].bounds.right < pageParagraphs[cp].bounds.left) || (pageParagraphs[cp].bounds.right < pageParagraphs[p].bounds.left))
								continue;
							if (pageParagraphs[cp].bounds.bottom < pageParagraphs[p].bounds.top)
								continue;
							ImWord[] cRegionWords = pageParagraphs[cp].getWords();
							if (cRegionWords.length == 0)
								continue;
							Arrays.sort(cRegionWords, ImUtils.textStreamOrder);
							if (!ImWord.TEXT_STREAM_TYPE_FOOTNOTE.equals(cRegionWords[0].getTextStreamType()) && !ImWord.TEXT_STREAM_TYPE_PAGE_TITLE.equals(cRegionWords[0].getTextStreamType()) && !ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(cRegionWords[0].getTextStreamType())) {
								nonFootnoteBelow = true;
								break;
							}
						}
						if (nonFootnoteBelow) {
							spm.setInfo(" ==> non-footnote below");
							continue;
						}
						if (isFootnote(pageParagraphs[p], paragraphWords, docFontSize, footnoteStartPatterns, footnoteMinFontSize, footnoteMaxFontSize)) {
							spm.setInfo(" ==> footnote detected");
							synchronized (doc) {
								ImUtils.makeStream(paragraphWords, ImWord.TEXT_STREAM_TYPE_FOOTNOTE, FOOTNOTE_TYPE);
								ImUtils.orderStream(paragraphWords, ImUtils.leftRightTopDownOrder);
							}
							newFootnoteFound = true;
						}
						else spm.setInfo(" ==> not a footnote");
					}
				} while (newFootnoteFound);
			}
		}, pages.length, (this.debug ? 1 : (pages.length / 8)));
		
		//	TODO detect in-text notes (paragraphs whose average font size is significantly smaller than main text) 
		
		//	TODO add in-text notes residing between table and caption to caption proper
		//	TODO TEST: table in page 14 of EJT/ejt-431_boonyanusith_sanoamuang_brancelj.pdf.imd (EJT-testbed issue 274)
		//	TODO observe such notes when computing distance between table and caption on match-up
		
		//	TODOne TEST: any Zootaxa with a "Continued on next page ..." table (e.g. Zootaxa/zootaxa.4369.4.4.pdf.imf)
		
		spm.setStep(" - identifying table notes");
		spm.setBaseProgress(55);
		spm.setMaxProgress(60);
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int pg) throws Exception {
				spm.setProgress((pg * 100) / pages.length);
				ImRegion[] pageTables = pages[pg].getRegions(ImRegion.TABLE_TYPE);
				if (pageTables.length == 0)
					return;
				ImRegion[] pageBlocks = pages[pg].getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
				if (pageBlocks.length == 0)
					return;
				for (int b = 0; b < pageBlocks.length; b++) {
					spm.setInfo("Testing block " + pageBlocks[b].bounds + " on page " + pages[pg].pageId + " for table note");
					ImWord[] blockWords = getLargestTextStreamWords(pageBlocks[b].getWords());
					if (blockWords.length == 0)
						continue;
					if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(blockWords[0].getTextStreamType()))
						continue;
					
					//	check font size (has to be smaller than table)
					//	TODO maybe allow font size equality if block has single line
					int blockFontSize = getAverageFontSize(blockWords);
					if (blockFontSize == -1) {
						spm.setInfo(" ==> could not determine font size");
						continue;
					}
					if (blockFontSize >= docFontSize) {
						spm.setInfo(" ==> font too large (" + blockFontSize + ")");
						continue;
					}
					
					//	check distance to table
					//	TODO maybe even require as close as 1/6 inch ...
					int tableDist = Integer.MAX_VALUE;
					ImRegion minDistTable = null;
					for (int t = 0; t < pageTables.length; t++) {
						int tableTopDist = Math.abs(pageTables[t].bounds.top - pageBlocks[b].bounds.bottom);
						if (tableTopDist < tableDist) {
							tableDist = tableTopDist;
							minDistTable = pageTables[t];
						}
						int tableBottomDist = Math.abs(pageBlocks[b].bounds.top - pageTables[t].bounds.bottom);
						if (tableBottomDist < tableDist) {
							tableDist = tableBottomDist;
							minDistTable = pageTables[t];
						}
					}
					if (tableDist >= (pages[pg].getImageDPI() / 6)) {
						spm.setInfo(" ==> too far from any table (" + tableDist + ")");
						continue;
					}
					
					//	cut out and mark table note
					ImAnnotation tableNote;
					synchronized (doc) {
						tableNote = ImUtils.makeStream(blockWords, ImWord.TEXT_STREAM_TYPE_TABLE_NOTE, ImAnnotation.TABLE_NOTE_TYPE);
						if (tableNote == null)
							continue;
						ImUtils.orderStream(blockWords, ImUtils.leftRightTopDownOrder);
					}
					spm.setInfo(" ==> table note detected");
					
					//	link table note to target
					tableNote.setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + minDistTable.pageId));
					tableNote.setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, minDistTable.bounds.toString());
				}
			}
		}, pages.length, (this.debug ? 1 : (pages.length / 8)));
		
		
		//	index paragraph starts and ends (in preparation for paragraph merging)
		spm.setStep(" - indexing paragraph end words");
		spm.setBaseProgress(60);
		spm.setMaxProgress(62);
		HashMap paragraphStartWords = new HashMap();
		HashMap paragraphStartWordBlocks = new HashMap();
		HashMap paragraphStartWordColumns = new HashMap();
		HashMap paragraphEndWords = new HashMap();
		HashMap paragraphEndWordBlocks = new HashMap();
		HashMap paragraphEndWordColumns = new HashMap();
		for (int pg = 0; pg < pages.length; pg++) {
			spm.setProgress((pg * 100) / pages.length);
			spm.setInfo("Indexing paragraphs on page " + pages[pg].pageId);
			ImRegion[] pageParagraphs = pages[pg].getRegions(ImRegion.PARAGRAPH_TYPE);
			for (int p = 0; p < pageParagraphs.length; p++) {
				ImWord[] paragraphWords = pageParagraphs[p].getWords();
				if (paragraphWords.length == 0)
					continue;
				Arrays.sort(paragraphWords, ImUtils.textStreamOrder);
				paragraphStartWords.put(paragraphWords[0], pageParagraphs[p]);
				paragraphEndWords.put(paragraphWords[paragraphWords.length-1], pageParagraphs[p]);
			}
			ImRegion[] pageBlocks = pages[pg].getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
			for (int b = 0; b < pageBlocks.length; b++) {
				ImWord[] blockWords = pageBlocks[b].getWords();
				for (int w = 0; w < blockWords.length; w++) {
					if (paragraphStartWords.containsKey(blockWords[w]))
						paragraphStartWordBlocks.put(blockWords[w], pageBlocks[b]);
					if (paragraphEndWords.containsKey(blockWords[w]))
						paragraphEndWordBlocks.put(blockWords[w], pageBlocks[b]);
				}
			}
			ImRegion[] pageColumns = pages[pg].getRegions(ImRegion.COLUMN_ANNOTATION_TYPE);
			for (int c = 0; c < pageColumns.length; c++) {
				ImWord[] columnWords = pageColumns[c].getWords();
				for (int w = 0; w < columnWords.length; w++) {
					if (paragraphStartWords.containsKey(columnWords[w]))
						paragraphStartWordColumns.put(columnWords[w], pageColumns[c]);
					if (paragraphEndWords.containsKey(columnWords[w]))
						paragraphEndWordColumns.put(columnWords[w], pageColumns[c]);
				}
			}
		}
		
		//	index center-line words
		spm.setStep(" - indexing document words");
		spm.setBaseProgress(62);
		spm.setMaxProgress(65);
		textStreamHeads = doc.getTextStreamHeads();
		CountingSet docWords = new CountingSet(new TreeMap(String.CASE_INSENSITIVE_ORDER));
		for (int h = 0; h < textStreamHeads.length; h++) {
			spm.setProgress((h * 100) / textStreamHeads.length);
			if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(textStreamHeads[h].getTextStreamType()))
				continue;
			for (ImWord imw = textStreamHeads[h]; imw.getNextWord() != null; imw = imw.getNextWord()) {
				
				//	little to index here ...
				String imwStr = imw.getString();
				if ((imwStr == null) || (imwStr.length() == 0))
					continue;
				
				//	not counting (potentially) hyphenated words here, nor their successors
				if (imwStr.matches(hyphenatedWordRegEx)) {
					imw = imw.getNextWord();
					if (imw.getNextWord() == null)
						break; // cheaper to catch this here than in loop head
					else continue;
				}
				
				//	add word to stats
				docWords.add(imwStr);
			}
		}
		
		//	de-hyphenate line breaks within paragraphs
		spm.setStep(" - de-hyphenating line breaks");
		spm.setBaseProgress(65);
		spm.setMaxProgress(68);
		for (int h = 0; h < textStreamHeads.length; h++) {
			spm.setProgress((h * 100) / textStreamHeads.length);
			if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(textStreamHeads[h].getTextStreamType()))
				continue;
			for (ImWord imw = textStreamHeads[h]; imw.getNextWord() != null; imw = imw.getNextWord()) {
				
				//	these two have a paragraph break between them, we're handling these more complex cases below
				if (imw.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
					continue;
				
				//	little we can do here ...
				String imwStr = imw.getString();
				if ((imwStr == null) || (imwStr.length() < 2))
					continue;
				
				//	get next word
				ImWord nextImw = imw.getNextWord();
				
				//	little we can do here, either ...
				String nextImwStr = nextImw.getString();
				if ((nextImwStr == null) || (nextImwStr.length() < 2))
					continue;
				
				//	not a line break (cutting some slack on vertical overlap for OCR documents)
				if (imw.bounds.left < nextImw.bounds.left)
					continue; // looks like a normal in-line word sequence here
				if (documentBornDigital && (nextImw.bounds.top < imw.bounds.bottom))
					continue; // we have vertical overlap, hardly a line break in a born-digital document
				if ((nextImw.centerY < imw.bounds.bottom) || (nextImw.bounds.top < imw.centerY))
					continue; // too much vertical overlap even for OCR documents
				
				//	this one's last in its paragraph, we're handling these more complex cases below
				ImRegion imwParagraph = ((ImRegion) paragraphEndWords.get(imw));
				if (imwParagraph != null)
					continue;
				
				//	no use checking for hyphenation with a sentence end
				if (Gamta.isSentenceEnd(imwStr))
					continue;
				if (Gamta.isClosingBracket(imwStr) && (imw.getPreviousWord() != null) && (imw.getPreviousWord().getString() != null) && (imw.getPreviousWord().getString().length() != 0) && Gamta.isSentenceEnd(imw.getPreviousWord().getString()))
					continue;
				
				//	check for hyphenation or line broken double word
				char imwNextRelation = this.getWordRelation(imwStr, nextImwStr, docWords);
				
				//	we do have a hyphenated or double word
				if (imwNextRelation != ImWord.NEXT_RELATION_SEPARATE)
					imw.setNextRelation(imwNextRelation);
			}
		}
		
		//	try and merge paragraphs across blocks
		spm.setStep(" - merging interrupted paragraphs");
		spm.setBaseProgress(68);
		spm.setMaxProgress(70);
		final DocumentStyle blockStyle = docLayout.getSubset("block");
		for (int h = 0; h < textStreamHeads.length; h++) {
			spm.setProgress((h * 100) / textStreamHeads.length);
			if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(textStreamHeads[h].getTextStreamType()))
				continue;
			for (ImWord imw = textStreamHeads[h]; imw.getNextWord() != null; imw = imw.getNextWord()) {
				
				//	these two don't have a paragraph break between them
				if (imw.getNextRelation() != ImWord.NEXT_RELATION_PARAGRAPH_END)
					continue;
				
				//	this one's not last in its paragraph
				ImRegion imwParagraph = ((ImRegion) paragraphEndWords.get(imw));
				if (imwParagraph == null)
					continue;
				
				//	little we can do here ...
				String imwStr = imw.getString();
				if ((imwStr == null) || (imwStr.length() == 0))
					continue;
				
				//	get next word
				ImWord nextImw = imw.getNextWord();
				
				//	little we can do here, either ...
				String nextImwStr = nextImw.getString();
				if ((nextImwStr == null) || (nextImwStr.length() == 0))
					continue;
				
				System.out.println("Checking cross-break paragraph relation between " + imwStr + " and " + nextImwStr);
				
//				//	this one's too far left in its paragraph (further than 10% of width away from right boundary)
//				//	LET TWO-BLOCK PARAGRAPH ANALYSIS DECIDE ABOUT THIS
//				if (((imwParagraph.bounds.right - imw.bounds.right) * 10) > imwParagraph.bounds.getWidth())
//					continue;
				
//				//	this one's too far left in its column or block (further than 10% of width away from right boundary)
//				//	LET TWO-BLOCK PARAGRAPH ANALYSIS DECIDE ABOUT THIS
//				if (((imwParentRegion.bounds.right - imw.bounds.right) * 10) > imwParentRegion.bounds.getWidth())
//					continue;
				
				//	get parent column or block to compare width, also of successor (helps with single-line paragraphs)
				ImRegion imwBlock = ((ImRegion) paragraphEndWordBlocks.get(imw));
				if (imwBlock == null) {
					System.out.println(" ==> first block not found");
					continue;
				}
				ImRegion nextImwBlock = ((ImRegion) paragraphStartWordBlocks.get(nextImw));
				if (nextImwBlock == null) {
					System.out.println(" ==> second block not found");
					continue;
				}
				
				//	same block, nothing to do here
				if (imwBlock == nextImwBlock) {
					System.out.println(" ==> same block");
					continue;
				}
				
				//	without layout disruptions, there is no reason for splitting up a paragraph, so no reason to go search for one
				if (PageAnalysis.areContinuousLayout(imwBlock, nextImwBlock)) {
					System.out.println(" ==> continuous layout");
					continue;
				}
				
				//	check for hyphenation or line broken double word
				char imwNextRelation = this.getWordRelation(imwStr, nextImwStr, docWords);
				System.out.println(" - cross-break word relation is " + imwNextRelation);
				if (imwNextRelation == ImWord.NEXT_RELATION_HYPHENATED) {
					imw.setNextRelation(imwNextRelation);
					System.out.println(" ==> joined cross-block hyphenated word");
					continue;
				}
				if (imwNextRelation == ImWord.NEXT_RELATION_CONTINUE) {
					imw.setNextRelation(imwNextRelation);
					System.out.println(" ==> joined cross-block broken double word");
					continue;
				}
				
				//	compute block layouts
				BlockMetrics imwBlockMetrics = PageAnalysis.computeBlockMetrics(imwBlock);
				if (imwBlockMetrics == null) {
					System.out.println(" ==> no lines to start with");
					continue; // happens if there are no lines at all
				}
				BlockLayout imwBlockLayout = imwBlockMetrics.analyze(blockStyle);
				BlockMetrics nextImwBlockMetrics = PageAnalysis.computeBlockMetrics(nextImwBlock);
				if (nextImwBlockMetrics == null) {
					System.out.println(" ==> no lines to continue with");
					continue; // happens if there are no lines at all
				}
				BlockLayout nextImwBlockLayout = nextImwBlockMetrics.analyze(blockStyle);
				
				//	no chance of flowing text continuing across blocks with incompatible style
				if (!PageAnalysis.areContinuousStyle(imwBlockLayout, nextImwBlockLayout)) {
					System.out.println(" ==> style mismatch");
					continue;
				}
				
				//	analyze blocks as one
				int imwBlockPargraphMargin = ((imwBlockLayout.paragraphDistance < 0) ? imwBlockMetrics.avgAboveLineGap : (imwBlockLayout.paragraphDistance - 1));
				int nextImwBlockPargraphMargin = ((nextImwBlockLayout.paragraphDistance < 0) ? nextImwBlockMetrics.avgAboveLineGap : (nextImwBlockLayout.paragraphDistance - 1));
				int blockMargin = Math.min(((imwBlockPargraphMargin < 0) ? Integer.MAX_VALUE : imwBlockPargraphMargin), ((nextImwBlockPargraphMargin < 0) ? Integer.MAX_VALUE : nextImwBlockPargraphMargin));
				BlockLayout jointBlockMetrics = nextImwBlockMetrics.analyzeContinuingFrom(imwBlockMetrics, blockMargin, blockStyle);
				
				//	all indications are next block starts paragraph of its own
				if (jointBlockMetrics.isParagraphStartLine[0]) {
					System.out.println(" ==> second block has paragraph start line on top");
					continue;
				}
				
				 //	TODOne TEST: paragraph across page break 17/18 in TableTest/zt00872.pdf (looks like sentence end)
				
				 //	TODOne TEST: paragraph across page break 16/17 in TableTest/zt00872.pdf (fails to connect)
				
//				//	no use checking with a sentence end
//				//	DO NOT CHECK FOR SENTENCE ENDING PUNCTUATION ONLY, DOTS MAY ALSO OCCUR IN ABBREVIATIONS ==> USE LAYOUT
//				if (Gamta.isSentenceEnd(imw.getString()))
//					continue;
//				else if (Gamta.isClosingBracket(imw.getString()) && (imw.getPreviousWord() != null) && (imw.getPreviousWord().getString() != null) && (imw.getPreviousWord().getString().length() != 0) && Gamta.isSentenceEnd(imw.getPreviousWord().getString()))
//					continue;
				
				//	we have two separate words, but no paragraph break
				imw.setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
				System.out.println(" ==> joined cross-block paragraph");
			}
		}
		
		//	collect blocks that might be assigned captions
		spm.setStep(" - identifying caption target areas");
		spm.setBaseProgress(70);
		spm.setMaxProgress(75);
		final HashMap tablesToCaptionAnnots = new HashMap();
		final boolean figureAboveCaptions = docLayout.getBooleanProperty("caption.belowFigure", true);
		final boolean figureBelowCaptions = docLayout.getBooleanProperty("caption.aboveFigure", false);
		final boolean figureBesideCaptions = docLayout.getBooleanProperty("caption.besideFigure", true);
		final boolean tableAboveCaptions = docLayout.getBooleanProperty("caption.belowTable", true);
		final boolean tableBelowCaptions = docLayout.getBooleanProperty("caption.aboveTable", true);
		final boolean tableBesideCaptions = docLayout.getBooleanProperty("caption.besideTable", false);
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int p) throws Exception {
				spm.setProgress((p * 100) / pages.length);
				
				//	no captions in this page
				if (pageCaptionsByBounds[p] == null)
					return;
				
				//	collect possible caption target areas
				HashMap aboveCaptionTargets = new HashMap();
				HashMap belowCaptionTargets = new HashMap();
				HashMap besideCaptionTargets = new HashMap();
				for (Iterator cit = pageCaptionsByBounds[p].keySet().iterator(); cit.hasNext();) {
					BoundingBox captionBounds = ((BoundingBox) cit.next());
					ImAnnotation captionAnnot = ((ImAnnotation) pageCaptionsByBounds[p].get(captionBounds));
					boolean isTableCaption = captionAnnot.getFirstWord().getString().toLowerCase().startsWith("tab"); // covers English, German, French, Italian, and Spanish (at least ...)
					if (isTableCaption ? tableAboveCaptions : figureAboveCaptions) {
						ImRegion aboveCaptionTarget = getAboveCaptionTarget(pages[p], captionBounds, pageImageDpi, isTableCaption);
						if (aboveCaptionTarget != null)
							aboveCaptionTargets.put(captionBounds, aboveCaptionTarget);
					}
					if (isTableCaption ? tableBelowCaptions : figureBelowCaptions) {
						ImRegion belowCaptionTarget = getBelowCaptionTarget(pages[p], captionBounds, pageImageDpi, isTableCaption);
						if (belowCaptionTarget != null)
							belowCaptionTargets.put(captionBounds, belowCaptionTarget);
					}
					if (isTableCaption ? tableBesideCaptions : figureBesideCaptions) {
						ImRegion besideCaptionTarget = getBesideCaptionTarget(pages[p], captionBounds, pageImageDpi, isTableCaption);
						if (besideCaptionTarget != null)
							besideCaptionTargets.put(captionBounds, besideCaptionTarget);
					}
				}
				
				//	assign target areas to captions (unambiguous ones first, then remaining ones)
				BoundingBox[] captionBounds = null;
				boolean skipAmbiguousCaptions = true;
				ArrayList assignedCaptionTargets = new ArrayList();
				do {
					
					//	activate ambiguous captions if no new assignments in previous round
					if ((captionBounds != null) && (captionBounds.length == pageCaptionsByBounds[p].size()))
						skipAmbiguousCaptions = false;
					
					//	get remaining captions
					captionBounds = ((BoundingBox[]) pageCaptionsByBounds[p].keySet().toArray(new BoundingBox[pageCaptionsByBounds[p].size()]));
					for (int c = 0; c < captionBounds.length; c++) {
						
						//	get candidate targets, and check if still available
						ImRegion act = ((ImRegion) aboveCaptionTargets.get(captionBounds[c]));
						if (act != null) {
							for (int a = 0; a < assignedCaptionTargets.size(); a++)
								if (((ImRegion) assignedCaptionTargets.get(a)).bounds.overlaps(act.bounds)) {
									 aboveCaptionTargets.remove(captionBounds[c]);
									 act = null;
									 break;
								}
						}
						ImRegion bct = ((ImRegion) belowCaptionTargets.get(captionBounds[c]));
						if (bct != null) {
							for (int a = 0; a < assignedCaptionTargets.size(); a++)
								if (((ImRegion) assignedCaptionTargets.get(a)).bounds.overlaps(bct.bounds)) {
									 belowCaptionTargets.remove(captionBounds[c]);
									 bct = null;
									 break;
								}
						}
						ImRegion sct = ((ImRegion) besideCaptionTargets.get(captionBounds[c]));
						if (sct != null) {
							for (int a = 0; a < assignedCaptionTargets.size(); a++)
								if (((ImRegion) assignedCaptionTargets.get(a)).bounds.overlaps(sct.bounds)) {
									 besideCaptionTargets.remove(captionBounds[c]);
									 sct = null;
									 break;
								}
						}
						
						//	count targets
						int ctCount = (((act == null) ? 0 : 1) + ((bct == null) ? 0 : 1) + ((sct == null) ? 0 : 1));
						
						//	no target found or left for this one
						if (ctCount == 0) {
							pageCaptionsByBounds[p].remove(captionBounds[c]);
							continue;
						}
						
						//	this one's ambiguous, save for another round
						if (skipAmbiguousCaptions && (ctCount != 1))
							continue;
						
						//	get target area (prefer above over below, and below over beside)
						ImRegion ct = ((act == null) ? ((bct == null) ? sct : bct) : act);
						
						//	get annotation and check target type
						ImAnnotation captionAnnot = ((ImAnnotation) pageCaptionsByBounds[p].get(captionBounds[c]));
						
						//	mark table caption
						if (ImRegion.TABLE_TYPE.equals(ct.getType())) {
							captionAnnot.setAttribute("targetIsTable");
							synchronized (tablesToCaptionAnnots) {
								tablesToCaptionAnnots.put(ct, captionAnnot);
							}
						}
						
						//	mark image region, and clean up contents
						else {
							ct.setType(ImRegion.IMAGE_TYPE);
							if (ct.getPage() == null) {
								pages[p].addRegion(ct);
								ImRegion[] ctRegions = ct.getRegions();
								for (int r = 0; r < ctRegions.length; r++) {
									if (ctRegions[r] != ct)
										pages[p].removeRegion(ctRegions[r]);
								}
							}
						}
						
						//	set target attributes
						captionAnnot.setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + ct.pageId));
						captionAnnot.setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, ct.bounds.toString());
						captionAnnot.setAttribute("startId", captionAnnot.getFirstWord().getLocalID());
						
						//	remember we have assigned this one
						assignedCaptionTargets.add(ct);
						
						//	clean up
						pageCaptionsByBounds[p].remove(captionBounds[c]);
						aboveCaptionTargets.remove(captionBounds[c]);
						belowCaptionTargets.remove(captionBounds[c]);
						besideCaptionTargets.remove(captionBounds[c]);
					}
				}
				
				//	keep going while either new assignments happen, or ambiguous captions are left to handle
				while ((captionBounds.length > pageCaptionsByBounds[p].size()) || skipAmbiguousCaptions);
			}
		}, pages.length, (this.debug ? 1 : (pages.length / 8)));
		
		/* TODO handle any captions remaining without targets:
		 * - collect any captions that remain without targets
		 *   ==> most likely in above parallel loop (synchronize on overall collections !!!)
		 * - also collect all captions that do have targets as well
		 *   ==> most likely in above parallel loop (synchronize on overall collections !!!)
		 * - collect all caption targets (figures and graphics), and index by page ID plus bounding box string
		 * - eliminate caption targets already assigned to caption, together with latter caption proper
		 * - for remaining captions, seek targets on adjacent pages ...
		 * - ... comparing backward and forward assignment where both possible
		 */
		
		//	merge tables within pages, and collect large ones along the way for cross-page mergers
		spm.setStep(" - merging tables within pages");
		spm.setBaseProgress(75);
		spm.setMaxProgress(80);
		final LinkedList docTableList = new LinkedList();
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int p) throws Exception {
				spm.setProgress((p * 100) / pages.length);
				ImRegion[] pageTables = pages[p].getRegions(ImRegion.TABLE_TYPE);
				if (pageTables.length == 0) {
					spm.setInfo("No tables to merge on page " + pages[p].pageId);
					return;
				}
				
				//	sort out large tables (over 70% of page in each direction), and collect smaller ones
				LinkedList pageTableList = new LinkedList();
				for (int t = 0; t < pageTables.length; t++) {
					if ((((pageTables[t].bounds.right - pageTables[t].bounds.left) * 10) < ((pages[p].bounds.right - pages[p].bounds.left) * 7)) || ((pageTables[t].bounds.bottom - pageTables[t].bounds.top) * 10) < ((pages[p].bounds.bottom - pages[p].bounds.top) * 7)) {
						pageTableList.add(pageTables[t]);
						if (pageTables.length == 1)
							synchronized (docTableList) {
								docTableList.add(pageTables[t]);
							}
					}
					else synchronized (docTableList) {
						docTableList.add(pageTables[t]);
					}
				}
				
				//	anything left to handle on this page?
				if (pageTableList.size() < 2) {
					spm.setInfo("No tables to merge on page " + pages[p].pageId);
					return;
				}
				else if (pageTableList.size() < pageTables.length)
					pageTables = ((ImRegion[]) pageTableList.toArray(new ImRegion[pageTableList.size()]));
				
				//	investigate tables top-down to find row mergers (column mergers make no sense here)
				Arrays.sort(pageTables, ImUtils.topDownOrder);
				for (int t = 0; t < pageTables.length; t++) {
					ImAnnotation tableCaption = ((ImAnnotation) tablesToCaptionAnnots.get(pageTables[t]));
					spm.setInfo("Attempting to row-merge table " + pageTables[t].bounds + " on page " + pageTables[t].pageId);
					String tableCaptionStart = ((tableCaption == null) ? null : getCaptionStartForCheck(tableCaption));
					spm.setInfo(" - caption start is " + ((tableCaptionStart == null) ? "null" : ("'" + tableCaptionStart + "'")));
					for (int c = (t+1); c < pageTables.length; c++) {
						spm.setInfo(" - comparing to table " + pageTables[c].bounds);
						if (pageTables[c].bounds.top < pageTables[t].bounds.bottom) {
							spm.setInfo(" --> tables overlapping or side by side");
							continue;
						}
						if ((pageTables[c].bounds.right < pageTables[t].bounds.left) && (pageTables[t].bounds.right < pageTables[c].bounds.left)) {
							spm.setInfo(" --> tables horizontally offset");
							continue;
						}
						if (!ImUtils.areTableRowsCompatible(pageTables[t], pageTables[c])) {
							spm.setInfo(" --> rows not compatible");
							continue;
						}
						ImAnnotation cTableCaption = ((ImAnnotation) tablesToCaptionAnnots.get(pageTables[c]));
						String cTableCaptionStart = ((cTableCaption == null) ? null : getCaptionStartForCheck(cTableCaption));
						if ((tableCaptionStart != null) && (cTableCaptionStart != null) && !tableCaptionStart.equalsIgnoreCase(cTableCaptionStart)) {
							spm.setInfo(" --> caption start '" + cTableCaptionStart + "' not compatible");
							continue;
						}
						ImUtils.connectTableRows(pageTables[t], pageTables[c]);
						spm.setInfo(" --> table rows merged");
						break;
					}
				}
				
				//	investigate tables left-right to find column mergers (row mergers make no sense here)
				Arrays.sort(pageTables, ImUtils.leftRightOrder);
				for (int t = 0; t < pageTables.length; t++) {
					ImAnnotation tableCaption = ((ImAnnotation) tablesToCaptionAnnots.get(pageTables[t]));
					spm.setInfo("Attempting to column-merge table " + pageTables[t].bounds + " on page " + pageTables[t].pageId);
					String tableCaptionStart = ((tableCaption == null) ? null : getCaptionStartForCheck(tableCaption));
					spm.setInfo(" - caption start is " + ((tableCaptionStart == null) ? "null" : ("'" + tableCaptionStart + "'")));
					for (int c = (t+1); c < pageTables.length; c++) {
						spm.setInfo(" - comparing to table " + pageTables[c].bounds);
						if (pageTables[c].bounds.left < pageTables[t].bounds.right) {
							spm.setInfo(" --> tables overlapping or atop one another");
							continue;
						}
						if (!ImUtils.areTableColumnsCompatible(pageTables[t], pageTables[c])) {
							spm.setInfo(" --> columns not compatible");
							continue;
						}
						ImAnnotation cTableCaption = ((ImAnnotation) tablesToCaptionAnnots.get(pageTables[c]));
						String cTableCaptionStart = ((cTableCaption == null) ? null : getCaptionStartForCheck(cTableCaption));
						if ((tableCaptionStart != null) && (cTableCaptionStart != null) && !tableCaptionStart.equalsIgnoreCase(cTableCaptionStart)) {
							spm.setInfo(" --> caption start '" + cTableCaptionStart + "' not compatible");
							continue;
						}
						ImUtils.connectTableColumns(pageTables[t], pageTables[c]);
						spm.setInfo(" --> table columns merged");
						break;
					}
				}
			}
		}, pages.length, (this.debug ? 1 : (pages.length / 8)));
		
		//	merge tables across pages
		spm.setStep(" - merging tables across pages");
		ImRegion[] docTables;
		
		//	get and sort document tables
		docTables = ((ImRegion[]) docTableList.toArray(new ImRegion[docTableList.size()]));
		docTableList.clear();
		Arrays.sort(docTables, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				ImRegion reg1 = ((ImRegion) obj1);
				ImRegion reg2 = ((ImRegion) obj2);
				return ((reg1.pageId == reg2.pageId) ? ImUtils.topDownOrder.compare(reg1, reg2) : (reg1.pageId - reg2.pageId));
			}
		});
		
		//	investigate row mergers
		spm.setBaseProgress(75);
		spm.setMaxProgress(80);
		for (int t = 0; t < docTables.length; t++) {
			spm.setProgress((t * 100) / docTables.length);
			ImAnnotation tableCaption = ((ImAnnotation) tablesToCaptionAnnots.get(docTables[t]));
			spm.setInfo("Attempting to row-merge table " + docTables[t].bounds + " on page " + docTables[t].pageId);
			String tableCaptionStart = ((tableCaption == null) ? null : getCaptionStartForCheck(tableCaption));
			spm.setInfo(" - caption start is " + ((tableCaptionStart == null) ? "null" : ("'" + tableCaptionStart + "'")));
			for (int c = (t+1); c < docTables.length; c++) {
				spm.setInfo(" - comparing to table " + docTables[c].bounds + " on page " + docTables[c].pageId);
				if (docTables[c].pageId == docTables[t].pageId) {
					spm.setInfo(" --> same page");
					break;
				}
				if ((docTables[c].pageId - docTables[t].pageId) > 2) {
					spm.setInfo(" --> too many pages in between");
					break;
				}
				if (!ImUtils.areTableRowsCompatible(docTables[t], docTables[c])) {
					spm.setInfo(" --> rows not compatible");
					continue;
				}
				ImAnnotation cTableCaption = ((ImAnnotation) tablesToCaptionAnnots.get(docTables[c]));
				String cTableCaptionStart = ((cTableCaption == null) ? null : getCaptionStartForCheck(cTableCaption));
				if ((tableCaptionStart != null) && (cTableCaptionStart != null) && !tableCaptionStart.equalsIgnoreCase(cTableCaptionStart)) {
					spm.setInfo(" --> caption start '" + cTableCaptionStart + "' not compatible");
					continue;
				}
				ImUtils.connectTableRows(docTables[t], docTables[c]);
				spm.setInfo(" --> table rows merged");
				break;
			}
			
			//	collect non-merged tables as well as leftmost tables of table grid rows
			if (!docTables[t].hasAttribute("rowsContinueFrom"))
				docTableList.add(docTables[t]);
		}
		
		//	investigate column mergers
		docTables = ((ImRegion[]) docTableList.toArray(new ImRegion[docTableList.size()]));
		docTableList.clear();
		spm.setBaseProgress(80);
		spm.setMaxProgress(85);
		for (int t = 0; t < docTables.length; t++) {
			spm.setProgress((t * 100) / docTables.length);
			ImAnnotation tableCaption = ((ImAnnotation) tablesToCaptionAnnots.get(docTables[t]));
			spm.setInfo("Attempting to column-merge table " + docTables[t].bounds + " on page " + docTables[t].pageId);
			String tableCaptionStart = ((tableCaption == null) ? null : getCaptionStartForCheck(tableCaption));
			spm.setInfo(" - caption start is " + ((tableCaptionStart == null) ? "null" : ("'" + tableCaptionStart + "'")));
			ImRegion[] tableGridRow = ImUtils.getRowConnectedTables(docTables[t]);
			spm.setInfo(" - got " + tableGridRow.length + " row-connected tables");
			for (int c = (t+1); c < docTables.length; c++) {
				spm.setInfo(" - comparing to table " + docTables[c].bounds + " on page " + docTables[c].pageId);
				if (docTables[c].pageId == docTables[t].pageId) {
					spm.setInfo(" --> same page");
					break;
				}
				if (!ImUtils.areTableColumnsCompatible(docTables[t], docTables[c])) {
					spm.setInfo(" --> columns not compatible");
					continue;
				}
				ImAnnotation cTableCaption = ((ImAnnotation) tablesToCaptionAnnots.get(docTables[c]));
				String cTableCaptionStart = ((cTableCaption == null) ? null : getCaptionStartForCheck(cTableCaption));
				if ((tableCaptionStart != null) && (cTableCaptionStart != null) && !tableCaptionStart.equalsIgnoreCase(cTableCaptionStart)) {
					spm.setInfo(" --> caption start '" + cTableCaptionStart + "' not compatible");
					continue;
				}
				ImRegion[] cTableGridRow = ImUtils.getRowConnectedTables(docTables[c]);
				if (tableGridRow.length != cTableGridRow.length) {
					spm.setInfo(" --> row-connected tables not compatible");
					continue;
				}
				ImUtils.connectTableColumns(docTables[t], docTables[c]);
				spm.setInfo(" --> table columns merged");
				break;
			}
		}
		
		//	tag and connect caption citations
		spm.setStep(" - marking caption citations");
		spm.setBaseProgress(85);
		spm.setMaxProgress(90);
		this.captionCitationHandler.markCaptionCitations(doc, textStreamHeads);
		
		//	mark headings, as well as bold/italics emphases, and super- and subscripts
		spm.setStep(" - marking emphases, headings, and super- and subscripts");
		spm.setBaseProgress(90);
		spm.setMaxProgress(100);
		
		//	extrapolate bold and italics properties to dash-like words
		for (int h = 0; h < textStreamHeads.length; h++)
			for (ImWord imw = textStreamHeads[h]; imw != null; imw = imw.getNextWord()) {
				
				//	not eligible for font property extrapolation
				if ((imw.getString().length() > 1) || ("+-\u00B1\u00AD\u2010\u2012\u2011\u2013\u2014\u2015\u2212".indexOf(imw.getString()) == -1))
					continue;
				
				//	get surrounding words
				ImWord pImw = imw.getPreviousWord();
				ImWord nImw = imw.getNextWord();
				
				//	no two surrounding words to check
				if ((pImw == null) || (nImw == null))
					continue;
				
				//	extrapolate bold
				if (pImw.hasAttribute(ImWord.BOLD_ATTRIBUTE) && nImw.hasAttribute(ImWord.BOLD_ATTRIBUTE) && !imw.hasAttribute(ImWord.BOLD_ATTRIBUTE))
					imw.setAttribute(ImWord.BOLD_ATTRIBUTE);
				
				//	extrapolate italics (for dashes only, as characters involving a plus sign do have vertical strokes)
				if (pImw.hasAttribute(ImWord.ITALICS_ATTRIBUTE) && nImw.hasAttribute(ImWord.ITALICS_ATTRIBUTE) && !imw.hasAttribute(ImWord.ITALICS_ATTRIBUTE) && ("-\u00AD\u2010\u2012\u2011\u2013\u2014\u2015\u2212".indexOf(imw.getString()) != -1))
					imw.setAttribute(ImWord.ITALICS_ATTRIBUTE);
			}
		
		//	mark emphases
		HashSet emphasisWords = new HashSet();
		for (int h = 0; h < textStreamHeads.length; h++) {
			ImWord boldStart = null;
			ImWord italicsStart = null;
			for (ImWord imw = textStreamHeads[h]; imw != null; imw = imw.getNextWord()) {
				boolean markedBoldEmphasis = false;
				boolean markedItalicsEmphasis = false;
				
				//	finish bold emphasis (for style reasons, or because paragraph ends)
				if ((boldStart != null) && (!imw.hasAttribute(ImWord.BOLD_ATTRIBUTE) || (imw.getPreviousWord().getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END) || !this.isFontSizeMatch(boldStart.getFontSize(), imw))) {
					ImAnnotation emphasis = doc.addAnnotation(boldStart, imw.getPreviousWord(), ImAnnotation.EMPHASIS_TYPE);
					emphasis.setAttribute(ImWord.BOLD_ATTRIBUTE);
					markedBoldEmphasis = true;
					if ((italicsStart != null) && (ImUtils.textStreamOrder.compare(italicsStart, boldStart) <= 0)) {
						emphasis.setAttribute(ImWord.ITALICS_ATTRIBUTE);
						spm.setInfo("Found bold+italics emphasis on '" + emphasis.getFirstWord().getTextStreamType() + "' text stream: " + ImUtils.getString(emphasis.getFirstWord(), emphasis.getLastWord(), true));
						if ((boldStart == italicsStart) && !imw.hasAttribute(ImWord.ITALICS_ATTRIBUTE))
							italicsStart = null;
					}
					else spm.setInfo("Found bold emphasis on '" + emphasis.getFirstWord().getTextStreamType() + "' text stream: " + ImUtils.getString(emphasis.getFirstWord(), emphasis.getLastWord(), true));
					
					//	remember emphasized words
					for (ImWord eImw = emphasis.getFirstWord(); eImw != null; eImw = eImw.getNextWord()) {
						emphasisWords.add(eImw);
						if (eImw == emphasis.getLastWord())
							break;
					} // TODO_ne reactivate regular code once bold face detection in scans more reliable
//					if (documentBornDigital || (italicsStart != null)) {
//						ImAnnotation emphasis = doc.addAnnotation(boldStart, imw.getPreviousWord(), ImAnnotation.EMPHASIS_TYPE);
//						emphasis.setAttribute(ImWord.BOLD_ATTRIBUTE);
//						markedBoldEmphasis = true;
//						if ((italicsStart == null) || (ImUtils.textStreamOrder.compare(boldStart, italicsStart) < 0))
//							spm.setInfo("Found bold emphasis on '" + emphasis.getFirstWord().getTextStreamType() + "' text stream: " + ImUtils.getString(emphasis.getFirstWord(), emphasis.getLastWord(), true));
//						else {
//							emphasis.setAttribute(ImWord.ITALICS_ATTRIBUTE);
//							spm.setInfo("Found bold+italics emphasis on '" + emphasis.getFirstWord().getTextStreamType() + "' text stream: " + ImUtils.getString(emphasis.getFirstWord(), emphasis.getLastWord(), true));
//							if ((boldStart == italicsStart) && !imw.hasAttribute(ImWord.ITALICS_ATTRIBUTE))
//								italicsStart = null;
//						}
//						
//						//	remember emphasized words
//						for (ImWord eImw = emphasis.getFirstWord(); eImw != null; eImw = eImw.getNextWord()) {
//							emphasisWords.add(eImw);
//							if (eImw == emphasis.getLastWord())
//								break;
//						}
//					}
//					else markedBoldEmphasis = true;
				}
				
				//	finish italics emphasis (for style reasons, or because paragraph ends)
				if ((italicsStart != null) && (!imw.hasAttribute(ImWord.ITALICS_ATTRIBUTE) || (imw.getPreviousWord().getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END) || !this.isFontSizeMatch(italicsStart.getFontSize(), imw))) {
					ImAnnotation emphasis = doc.addAnnotation(italicsStart, imw.getPreviousWord(), ImAnnotation.EMPHASIS_TYPE);
					emphasis.setAttribute(ImWord.ITALICS_ATTRIBUTE);
					markedItalicsEmphasis = true;
					if ((boldStart != null) && (ImUtils.textStreamOrder.compare(boldStart, italicsStart) <= 0)) {
						emphasis.setAttribute(ImWord.BOLD_ATTRIBUTE);
						spm.setInfo("Found italics+bold emphasis on '" + emphasis.getFirstWord().getTextStreamType() + "' text stream: " + ImUtils.getString(emphasis.getFirstWord(), emphasis.getLastWord(), true));
						if ((italicsStart == boldStart) && !imw.hasAttribute(ImWord.BOLD_ATTRIBUTE))
							boldStart = null;
					}
					else spm.setInfo("Found italics emphasis on '" + emphasis.getFirstWord().getTextStreamType() + "' text stream: " + ImUtils.getString(emphasis.getFirstWord(), emphasis.getLastWord(), true));
					
					//	remember emphasized words
					for (ImWord eImw = emphasis.getFirstWord(); eImw != null; eImw = eImw.getNextWord()) {
						emphasisWords.add(eImw);
						if (eImw == emphasis.getLastWord())
							break;
					}
				}
				
				//	set or reset emphasis starts (possible only now, as we need both in marking either)
				if (imw.hasAttribute(ImWord.BOLD_ATTRIBUTE)) {
					if (((boldStart == null) && (".,;:".indexOf(imw.getString()) == -1)) || markedBoldEmphasis)
						boldStart = imw;
				}
				else boldStart = null;
				if (imw.hasAttribute(ImWord.ITALICS_ATTRIBUTE)) {
					if (((italicsStart == null) && (".,;:".indexOf(imw.getString()) == -1)) || markedItalicsEmphasis)
						italicsStart = imw;
				}
				else italicsStart = null;
			}
		}
		
		//	mark super- and subscripts
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int pg) throws Exception {
				ImRegion[] pageParagraphs = pages[pg].getRegions(ImRegion.PARAGRAPH_TYPE);
				if (pageParagraphs.length == 0)
					return;
				ImRegion[] pageLines = pages[pg].getRegions(ImRegion.LINE_ANNOTATION_TYPE);
				if (pageLines.length == 0)
					return;
				
				//	for each paragraph, investigate lines
				for (int p = 0; p < pageParagraphs.length; p++) {
					System.out.println("Seeking super- and sub scripts in " + pageParagraphs[p].bounds);
					
					//	collect paragraph lines, and collect font sizes
					ArrayList paragraphLines = new ArrayList();
					int lineWordWidthSum = 0;
					CountingSet lineWordFontSizes = new CountingSet(new TreeMap());
					for (int l = 0; l < pageLines.length; l++) {
						if (!pageLines[l].bounds.liesIn(pageParagraphs[p].bounds, true))
							continue;
						paragraphLines.add(pageLines[l]);
						ImWord[] lineWords = pageLines[l].getWords();
						for (int w = 0; w < lineWords.length; w++) {
							lineWordWidthSum += lineWords[w].bounds.getWidth();
							if (lineWords[w].hasAttribute(ImWord.FONT_SIZE_ATTRIBUTE)) try {
								lineWordFontSizes.add(Integer.valueOf(lineWords[w].getFontSize()), lineWords[w].bounds.getWidth());
							} catch (NumberFormatException nfe) {}
						}
					}
					
					//	anything to work with at all?
					if (paragraphLines.isEmpty()) {
						System.out.println(" ==> no lines at all");
						continue;
					}
					
					//	do we have a reliable basis for font size analysis?
					if ((lineWordFontSizes.size() * 5) < (lineWordWidthSum * 4)) {
						System.out.println(" ==> too few words with font size (only " + lineWordFontSizes.size() + " out of " + lineWordWidthSum + ")");
						continue;
					}
					
					//	anything to analyze? (no use going for lower outliers if only one font size present at all)
					if (lineWordFontSizes.elementCount() < 2) {
						System.out.println(" ==> only one font size");
						continue;
					}
					
					//	get minimum and maximum font sizes
					int minFontSize = ((Integer) lineWordFontSizes.first()).intValue();
					int maxFontSize = ((Integer) lineWordFontSizes.last()).intValue();
					System.out.println(" - minimum font size is " + minFontSize + ", maximum is " + maxFontSize);
					
					//	compute main font size
					int fontSizeCharWidthSum = 0;
					int minMainFontSize = -1;
					int maxMainFontSize = -1;
					for (int fs = minFontSize; fs <= maxFontSize; fs++) {
						fontSizeCharWidthSum += lineWordFontSizes.getCount(new Integer(fs));
						if ((minMainFontSize == -1) && ((fontSizeCharWidthSum * 10) > (lineWordWidthSum * 2)))
							minMainFontSize = fs; // minimum font size to exceed 20% smallest characters
						if ((maxMainFontSize == -1) && ((fontSizeCharWidthSum * 10) > (lineWordWidthSum * 8)))
							maxMainFontSize = fs; // minimum font size to exceed 80% smallest characters
					}
					System.out.println(" - minimum main font size is " + minMainFontSize + ", maximum is " + maxMainFontSize);
					if ((minMainFontSize == -1) || (maxMainFontSize == -1))
						continue;
					
					//	anything significantly below main font size minimum?
					if ((minMainFontSize - minFontSize) < 2) {
						System.out.println(" ==> too little difference between minimum and main minimum font size (only " + (minMainFontSize - minFontSize) + ")");
						continue;
					}
					
					//	mark super- and subscripts in individual lines
					Collections.sort(paragraphLines, ImUtils.topDownOrder);
					for (int l = 0; l < paragraphLines.size(); l++) {
						ImRegion paragraphLine = ((ImRegion) paragraphLines.get(l));
						ImWord[] lineWords = paragraphLine.getWords();
						
						//	compute average vertical center of main font size words, collect small words, and mark main font size areas
						int mainFontSizeWordWidthSum = 0;
						int mainFontSizeWordCenterYSum = 0;
						ArrayList smallLineWords = new ArrayList();
						ImWord[] mainFontSizeWord = new ImWord[pageParagraphs[p].bounds.getWidth()];
						Arrays.fill(mainFontSizeWord, null);
						for (int w = 0; w < lineWords.length; w++)
							if (lineWords[w].hasAttribute(ImWord.FONT_SIZE_ATTRIBUTE)) {
								int fs = lineWords[w].getFontSize();
								if ((minMainFontSize <= fs) && (fs <= maxMainFontSize)) {
									mainFontSizeWordWidthSum += lineWords[w].bounds.getWidth();
									mainFontSizeWordCenterYSum += (lineWords[w].bounds.getWidth() * lineWords[w].centerY);
									Arrays.fill(mainFontSizeWord, Math.max(0, (lineWords[w].bounds.left - pageParagraphs[p].bounds.left)), Math.min(mainFontSizeWord.length, (lineWords[w].bounds.right - pageParagraphs[p].bounds.left)), lineWords[w]);
								}
								else if (fs < minMainFontSize)
									smallLineWords.add(lineWords[w]);
							}
						
						//	anything to work with?
						if (smallLineWords.isEmpty())
							continue;
						if (mainFontSizeWordWidthSum == 0)
							continue;
						int mainFontSizeWordCenterY = (mainFontSizeWordCenterYSum / mainFontSizeWordWidthSum);
						
						//	sort words in super- and subscripts
						ArrayList superScriptWords = new ArrayList();
						ArrayList subScriptWords = new ArrayList();
						for (int w = 0; w < smallLineWords.size(); w++) {
							ImWord lineWord = ((ImWord) smallLineWords.get(w));
							int aboveCenterY = (mainFontSizeWordCenterY - lineWord.bounds.top);
							int belowCenterY = (lineWord.bounds.bottom - mainFontSizeWordCenterY);
							
							//	at least two thirds above center ==> superscript
							if ((belowCenterY * 2) < aboveCenterY)
								superScriptWords.add(lineWord);
							
							//	at least two thirds below center ==> subscript
							else if ((aboveCenterY * 2) < belowCenterY)
								subScriptWords.add(lineWord);
						}
						
						//	add super- and subscript annotations
						this.annotateSuperSubScripts(pageParagraphs[p], superScriptWords, ImAnnotation.SUPER_SCRIPT_TYPE, mainFontSizeWord);
						this.annotateSuperSubScripts(pageParagraphs[p], subScriptWords, ImAnnotation.SUB_SCRIPT_TYPE, mainFontSizeWord);
					}
				}
			}
			private void annotateSuperSubScripts(ImRegion paragraph, ArrayList superOrSubScriptWords, String type, ImWord[] mainFontSizeWord) {
				if (superOrSubScriptWords.isEmpty())
					return;
				Collections.sort(superOrSubScriptWords, ImUtils.leftRightOrder);
				ArrayList sossWords = new ArrayList();
				for (int w = 0; w < superOrSubScriptWords.size(); w++) {
					ImWord sossWord = ((ImWord) superOrSubScriptWords.get(w));
					
					//	starting new super- or subscript
					if (sossWords.isEmpty()) {
						sossWords.add(sossWord);
						continue;
					}
					
					//	check if we can attach current word to predecessor
					ImWord lastSossWord = ((ImWord) sossWords.get(sossWords.size() - 1));
					boolean sossContinues = true;
					for (int x = lastSossWord.bounds.right; x < sossWord.bounds.left; x++)
						if (mainFontSizeWord[x - paragraph.bounds.left] != null) {
							sossContinues = false;
							break;
						}
					if (sossContinues) {
						sossWords.add(sossWord);
						continue;
					}
					
					//	main font size word between, annotate collected words and clean up
					this.annotateSuperSubScript(paragraph, sossWords, type, mainFontSizeWord);
					
					//	start over with current word
					sossWords.add(sossWord);
				}
				
				//	anything left to annotate?
				this.annotateSuperSubScript(paragraph, sossWords, type, mainFontSizeWord);
			}
			private void annotateSuperSubScript(ImRegion paragraph, ArrayList superOrSubScriptWords, String type, ImWord[] mainFontSizeWord) {
				if (superOrSubScriptWords.isEmpty())
					return;
				
				//	annotate super- or subscript
				ImWord sossFirstWord = ((ImWord) superOrSubScriptWords.get(0));
				ImWord sossLastWord = ((ImWord) superOrSubScriptWords.get(superOrSubScriptWords.size() - 1));
				ImAnnotation soss;
				if (sossFirstWord == sossLastWord) {
					soss = paragraph.getDocument().addAnnotation(sossFirstWord, type);
					System.out.println(" ==> annotated " + type + " '" + sossFirstWord.getString() + "'");
				}
				else {
					ImUtils.orderStream(((ImWord[]) superOrSubScriptWords.toArray(new ImWord[superOrSubScriptWords.size()])), ImUtils.leftRightOrder);
					soss = paragraph.getDocument().addAnnotation(sossFirstWord, sossLastWord, type);
					System.out.println(" ==> annotated " + type + " from '" + sossFirstWord.getString() + "' to '" + sossLastWord.getString() + "'");
				}
				superOrSubScriptWords.clear();
				
				//	add font size attribute (first word is punctuation mark way more often than last ==> prefer latter)
				soss.setAttribute(ImAnnotation.FONT_SIZE_ATTRIBUTE, sossLastWord.getAttribute(ImWord.FONT_SIZE_ATTRIBUTE, sossFirstWord.getAttribute(ImWord.FONT_SIZE_ATTRIBUTE)));
				System.out.println("   - font size is " + soss.getAttribute(ImWord.FONT_SIZE_ATTRIBUTE));
				
				//	add attachment attribute, preferring word over punctuation mark
				int leftDist = (sossFirstWord.bounds.left - paragraph.bounds.left);
				boolean leftIsWord = false;
				for (int x = sossFirstWord.bounds.left; x >= paragraph.bounds.left; x--)
					if (mainFontSizeWord[x - paragraph.bounds.left] != null) {
						leftDist = (sossFirstWord.bounds.left - x);
						leftIsWord = Gamta.isWord(mainFontSizeWord[x - paragraph.bounds.left].getString());
						break;
					}
				System.out.println("   - left distance is " + leftDist + ", word is " + leftIsWord);
				int rightDist = (paragraph.bounds.right - sossLastWord.bounds.right);
				boolean rightIsWord = false;
				for (int x = sossLastWord.bounds.right; x < paragraph.bounds.right; x++)
					if (mainFontSizeWord[x - paragraph.bounds.left] != null) {
						rightDist = (x - sossLastWord.bounds.right);
						rightIsWord = Gamta.isWord(mainFontSizeWord[x - paragraph.bounds.left].getString());
						break;
					}
				System.out.println("   - right distance is " + rightDist + ", word is " + rightIsWord);
				if ((leftDist + 1) < rightDist)
					soss.setAttribute("attach", "left");
				else if ((rightDist + 1) < leftDist)
					soss.setAttribute("attach", "right");
				else if (leftIsWord && !rightIsWord)
					soss.setAttribute("attach", "left");
				else if (rightIsWord && !leftIsWord)
					soss.setAttribute("attach", "right");
				else if (rightIsWord && leftIsWord && (leftDist <= 2) && (rightDist <= 2))
					soss.setAttribute("attach", "both");
				else soss.setAttribute("attach", "none");
				System.out.println("   --> attach is " + soss.getAttribute("attach"));
			}
//		}, pages.length, (this.debug ? 1 : (pages.length / 8)));
		}, pages.length, 1); // TODO switch back to parallel after more tests
		
		//	clean table markup after marking emphases
		for (int t = 0; t < docTables.length; t++)
			this.tableActionProvider.cleanupTableAnnotations(doc, docTables[t]);
		
		//	get heading styles
		HeadingStyleDefined[] headingStyles = null;
		DocumentStyle headingStyle = docStyle.getSubset("style.heading");
		int[] headingLevels = headingStyle.getIntListProperty("levels", null);
		if (headingLevels != null) {
			headingStyles = new HeadingStyleDefined[headingLevels.length];
			for (int l = 0; l < headingLevels.length; l++)
				headingStyles[l] = new HeadingStyleDefined(headingLevels[l], headingStyle);
		}
		
		//	extract headings using heuristics
		if (headingStyles == null) {
			
			//	mark headings
			ArrayList headings = new ArrayList();
			for (int pg = 0; pg < pages.length; pg++) {
				spm.setProgress((pg * 100) / pages.length);
				this.markHeadings(pages[pg], docFontSize, pageImageDpi, emphasisWords, spm, headings);
			}
			
			//	assess hierarchy of headings
			spm.setStep(" - assessing hierarchy of headings");
			this.assessHeadingHierarchy(headings, docFontSize, pages[0].pageId, spm);
			
		}
		
		//	extract headings using style templates
		else for (int pg = 0; pg < pages.length; pg++) {
			spm.setProgress((pg * 100) / pages.length);
			markHeadings(pages[pg], pages[pg].getImageDPI(), headingStyles, spm);
		}
		
		//	finally, we're done
		spm.setProgress(100);
	}
	
	//	prepositions occurring in enumerations (western European languages for now)
	private static final String hyphenatedWordRegEx = "and;or;und;oder;et;ou;y;e;o;u;ed";
	
	//	pattern matching any word ending in any kind of hyphen
	private static final String enumStopWords = ".+[\\-\\u00AD\\u2010\\u2011\\u2012\\u2013\\u2014\\u2015\\u2212]";
	
	private char getWordRelation(String imwStr, String nextImwStr, CountingSet docWords) {
		
		//	hyphenation never leaves a single character
		if ((imwStr.length() < 2) || (nextImwStr.length() < 2))
			return ImWord.NEXT_RELATION_SEPARATE;
		
		//	not a hyphenated word
		if (!imwStr.matches(hyphenatedWordRegEx))
			return ImWord.NEXT_RELATION_SEPARATE;
		
		//	this one rather looks like an enumeration continued than a word (western European languages for now)
		if (enumStopWords.indexOf(nextImwStr.toLowerCase()) != -1)
			return ImWord.NEXT_RELATION_SEPARATE;
		
		//	check morphological properties
		boolean imwAllCaps = (imwStr.equals(imwStr.toUpperCase()));
		boolean nextImwAllCaps = (nextImwStr.equals(nextImwStr.toUpperCase()));
		
		//	these two don't seem to fit
		if (imwAllCaps != nextImwAllCaps)
			return ImWord.NEXT_RELATION_SEPARATE;
		
		//	create both possible connected words
		String hyphenatedWordStr = (imwStr.substring(0, (imwStr.length() - 1)) + nextImwStr);
		int hyphenatedWordScore = docWords.getCount(hyphenatedWordStr);
		String sameWordStr = (imwStr + nextImwStr);
		int sameWordScore = docWords.getCount(sameWordStr);
		
		//	we have a hyphenated word for occurring as one elsewhere in document
		if (sameWordScore < hyphenatedWordScore)
			return ImWord.NEXT_RELATION_HYPHENATED;
		
		//	we have a double word for occurring as one elsewhere in document
		if (hyphenatedWordScore < sameWordScore)
			return ImWord.NEXT_RELATION_CONTINUE;
		
		//	we have a hyphenated word for morphology (lower case continuation)
		if (nextImwStr.equals(nextImwStr.toLowerCase()))
			return ImWord.NEXT_RELATION_HYPHENATED;
		
		//	default to two separate words
		return ImWord.NEXT_RELATION_SEPARATE;
	}
	
	/* string of punctuation marks whose font size can differ a little from the
	 * surrounding text due to sloppy layout or even adjustment */
	private static final String fontSizeVariablePunctuationMarks = ",.;:^°\"'=+-\u00B1\u00AD\u2010\u2012\u2011\u2013\u2014\u2015\u2212";
	
	/* string of punctuation marks whose font style (bold, italics) can differ
	 * from the surrounding text due to sloppy layout or even adjustment */
	private static final String fontStyleVariablePunctuationMarks = ",.°'=+-\u00B1\u00AD\u2010\u2012\u2011\u2013\u2014\u2015\u2212";
	
	private boolean isFontSizeMatch(int fontSize, ImWord imw) {
		if (fontSize == -1)
			return true;
		if (imw.getFontSize() == -1)
			return true;
		if (fontSize == imw.getFontSize())
			return true;
		if ((imw.getString().length() > 1) || (fontSizeVariablePunctuationMarks.indexOf(imw.getString()) == -1))
			return false;
		return (Math.abs(fontSize - imw.getFontSize()) <= 1);
	}
	
	private void assessHeadingHierarchy(ArrayList headings, int docFontSize, int docFirstPageId, ProgressMonitor pm) {
		
		//	collect properties of headings (font size, bold, all-caps, centered, numbering), as well as their global position among all headings
		Collections.sort(headings, annotationTextStreamOrder);
		TreeMap headingStylesByKey = new TreeMap(Collections.reverseOrder());
		for (int h = 0; h < headings.size(); h++) {
			ImAnnotation heading = ((ImAnnotation) headings.get(h));
			HeadingStyleObserved headingStyle = ((HeadingStyleObserved) headingStylesByKey.get(HeadingStyleObserved.getStyleKey(heading)));
			if (headingStyle == null) {
				headingStyle = new HeadingStyleObserved(heading);
				headingStylesByKey.put(headingStyle.key, headingStyle);
			}
			else headingStyle.headings.add(heading);
		}
		ArrayList headingStyles = new ArrayList();
		for (Iterator hskit = headingStylesByKey.keySet().iterator(); hskit.hasNext();)
			headingStyles.add(headingStylesByKey.get(hskit.next()));
		pm.setInfo("Got " + headingStyles.size() + " initial heading styles");
		
		//	try and merge heading styles that are equal in everything but the font size differing by one
		for (int s = 0; s < headingStyles.size(); s++) {
			HeadingStyleObserved headingStyle = ((HeadingStyleObserved) headingStyles.get(s));
			for (int cs = (s+1); cs < headingStyles.size(); cs++) {
				HeadingStyleObserved cHeadingStyle = ((HeadingStyleObserved) headingStyles.get(cs));
				if (headingStyle.bold != cHeadingStyle.bold)
					continue;
				if (headingStyle.allCaps != cHeadingStyle.allCaps)
					continue;
				if (headingStyle.centered != cHeadingStyle.centered)
					continue;
				if (Math.abs(headingStyle.fontSize - cHeadingStyle.fontSize) > 1)
					continue;
				if ((headingStyle.headings.size() > 1) && (cHeadingStyle.headings.size() > 1))
					continue;
				if (headingStyle.headings.size() < cHeadingStyle.headings.size()) {
					pm.setInfo("Merging " + headingStyle.key + " into " + cHeadingStyle.key);
					cHeadingStyle.headings.addAll(headingStyle.headings);
					headingStyles.remove(s--);
					break;
				}
				else {
					pm.setInfo("Merging " + cHeadingStyle.key + " into " + headingStyle.key);
					headingStyle.headings.addAll(cHeadingStyle.headings);
					headingStyles.remove(cs--);
				}
			}
		}
		
		//	score square distances between headings of each style
		for (int s = 0; s < headingStyles.size(); s++) {
			HeadingStyleObserved headingStyle = ((HeadingStyleObserved) headingStyles.get(s));
			Collections.sort(headingStyle.headings, annotationTextStreamOrder);
			int lastPos = -1;
			int posDistSquareSum = 0;
			for (int h = 0; h < headingStyle.headings.size(); h++) {
				ImAnnotation heading = ((ImAnnotation) headingStyle.headings.get(h));
				int pos = headings.indexOf(heading);
				if (lastPos != -1)
					posDistSquareSum += ((pos - lastPos) * (pos - lastPos));
				lastPos = pos;
			}
			if (headingStyle.headings.size() > 1)
				headingStyle.avgPosDistSquare = (posDistSquareSum / (headingStyle.headings.size() - 1));
		}
		
		//	what do we got?
		pm.setInfo("Got " + headingStyles.size() + " heading styles:");
		for (int s = 0; s < headingStyles.size(); s++) {
			HeadingStyleObserved headingStyle = ((HeadingStyleObserved) headingStyles.get(s));
			pm.setInfo(" - " + headingStyle.key + " with " + headingStyle.headings.size() + " headings:");
			for (int h = 0; h < headingStyle.headings.size(); h++) {
				ImAnnotation heading = ((ImAnnotation) headingStyle.headings.get(h));
				headingStyle.firstPageId = Math.min(headingStyle.firstPageId, heading.getFirstWord().pageId);
				headingStyle.lastPageId = Math.max(headingStyle.lastPageId, heading.getLastWord().pageId);
				pm.setInfo("   - " + ImUtils.getString(heading.getFirstWord(), heading.getLastWord(), true) + " (at position " + headings.indexOf(heading) + ")");
			}
			pm.setInfo(" --> average position distance is " + headingStyle.avgPosDistSquare);
			pm.setInfo(" --> first page ID is " + headingStyle.firstPageId);
			pm.setInfo(" --> last page ID is " + headingStyle.lastPageId);
			
			//	sort out document headers right away
			if (headingStyle.headings.size() > 1)
				continue;
			if ((headingStyle.firstPageId != docFirstPageId) || (headingStyle.lastPageId != docFirstPageId))
				continue;
			if (headingStyle.fontSize < docFontSize)
				continue;
			pm.setInfo(" ==> document head");
			((ImAnnotation) headingStyle.headings.get(0)).setAttribute("level", "0");
			headingStyles.remove(s--);
		}
		
		//	from that, try and establish heading style hierarchy
		Collections.sort(headingStyles, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				HeadingStyleObserved hs1 = ((HeadingStyleObserved) obj1);
				HeadingStyleObserved hs2 = ((HeadingStyleObserved) obj2);
				if (hs1.allCaps == hs2.allCaps) {
					if (hs1.fontSize != hs2.fontSize)
						return (hs2.fontSize - hs1.fontSize);
					else if (hs1.bold != hs2.bold)
						return (hs1.bold ? -1 : 1);
					else if ((hs2.avgPosDistSquare != -1) && (hs1.avgPosDistSquare != -1))
						return (hs2.avgPosDistSquare - hs1.avgPosDistSquare);
					else return 0;
				}
				else if (hs1.fontSize == hs2.fontSize) {
					if ((hs1.allCaps != hs2.allCaps) && ((hs1.bold == hs2.bold) || (hs1.bold == hs1.allCaps)))
						return (hs1.allCaps ? -1 : 1);
					else if ((hs1.bold != hs2.bold) && ((hs1.allCaps == hs2.allCaps) || (hs1.allCaps == hs1.bold)))
						return (hs1.bold ? -1 : 1);
					else return 0;
				}
				else if ((hs2.avgPosDistSquare != -1) && (hs1.avgPosDistSquare != -1))
					return (hs2.avgPosDistSquare - hs1.avgPosDistSquare);
				else return 0;
			}
		});
		
		//	set heading level attributes
		pm.setInfo("Sorted " + headingStyles.size() + " heading styles:");
		for (int s = 0; s < headingStyles.size(); s++) {
			HeadingStyleObserved headingStyle = ((HeadingStyleObserved) headingStyles.get(s));
			pm.setInfo(" - level " + (s+1) + ": " + headingStyle.key + " with " + headingStyle.headings.size() + " headings:");
			for (int h = 0; h < headingStyle.headings.size(); h++) {
				ImAnnotation heading = ((ImAnnotation) headingStyle.headings.get(h));
				heading.setAttribute("level", ("" + (s+1)));
				pm.setInfo("   - " + ImUtils.getString(heading.getFirstWord(), heading.getLastWord(), true) + " (at position " + headings.indexOf(heading) + ")");
			}
		}
	}
	
	private static class HeadingStyleDefined {
		final int level;
		int minFontSize;
		int maxFontSize;
		int maxLineCount;
		boolean isBlock;
		boolean isBold;
		boolean startIsBold;
		boolean containsBold;
		boolean containsItalics;
		boolean isAllCaps;
		boolean startIsAllCaps;
		boolean containsAllCaps;
		Pattern[] startPatterns;
		boolean isLeftAligned;
		boolean isRightAligned;
		HeadingStyleDefined(int level, DocumentStyle style) {
			this.level = level;
			int fontSize = style.getIntProperty((this.level + ".fontSize"), -1);
			this.minFontSize = style.getIntProperty((this.level + ".minFontSize"), ((fontSize == -1) ? 0 : fontSize));
			this.maxFontSize = style.getIntProperty((this.level + ".maxFontSize"), ((fontSize == -1) ? 72 : fontSize));
			this.maxLineCount = style.getIntProperty((this.level + ".maxLineCount"), 0);
			this.isBlock = !style.getBooleanProperty((this.level + ".isNonBlock"), false); // we need to use inversion here, as styles save only 'true'
			this.isBold = style.getBooleanProperty((this.level + ".isBold"), false);
			this.startIsBold = style.getBooleanProperty((this.level + ".startIsBold"), false);
			this.containsBold = style.getBooleanProperty((this.level + ".containsBold"), false);
			this.containsItalics = style.getBooleanProperty((this.level + ".containsItalics"), false);
			this.isAllCaps = style.getBooleanProperty((this.level + ".isAllCaps"), false);
			this.startIsAllCaps = style.getBooleanProperty((this.level + ".startIsAllCaps"), false);
			this.startIsAllCaps = style.getBooleanProperty((this.level + ".containsAllCaps"), false);
			String[] startPatternStrs = style.getListProperty((this.level + ".startPatterns"), null, " ");
			if (startPatternStrs != null) try {
				Pattern[] startPatterns = new Pattern[startPatternStrs.length];
				for (int p = 0; p < startPatternStrs.length; p++)
					startPatterns[p] = Pattern.compile(startPatternStrs[p]);
				this.startPatterns = startPatterns;
			} catch (PatternSyntaxException pse) {}
			String alignment = style.getProperty((this.level + ".alignment"), "");
			this.isLeftAligned = ("left".equals(alignment) || "justified".equals(alignment));
			this.isRightAligned = ("right".equals(alignment) || "justified".equals(alignment));
			//	TODO observe center alignment (require distance to left and right edge of column to be about equal)
		}
		boolean matches(ImRegion line, ImWord[] lineWords, int blockLinePos, int blockLineCount, boolean isFlushLeft, boolean isFlushRight) {
			
			//	check alignment first
			if (this.isLeftAligned && !isFlushLeft)
				return false;
			if (this.isRightAligned && !isFlushRight)
				return false;
			if (!this.isLeftAligned && !this.isRightAligned && (isFlushLeft != isFlushRight))
				return false;
			
			//	check line count and position
			if (this.maxLineCount > 0) {
				
				//	headings are in their own blocks, check block size
				if (this.isBlock) {
					if (blockLineCount > this.maxLineCount)
						return false;
				}
				
				//	headings sit atop a text block, all we can check is line position
				else {
					if (blockLinePos >= this.maxLineCount)
						return false;
				}
			}
			
			//	check start style
			if ((this.isBold || this.startIsBold) && !lineWords[0].hasAttribute(ImWord.BOLD_ATTRIBUTE))
				return false;
			if ((this.isAllCaps || this.startIsAllCaps) && !lineWords[0].getString().equals(lineWords[0].getString().toUpperCase()))
				return false;
			
			//	concatenate words, checking style along the way
			StringBuffer lineWordString = new StringBuffer();
			boolean gotBold = false;
			boolean gotItalics = false;
			boolean gotAllCaps = false;
			for (int w = 0; w < lineWords.length; w++) {
				String wordStr = lineWords[w].getString();
				if (this.isBold && !lineWords[w].hasAttribute(ImWord.BOLD_ATTRIBUTE) && ((wordStr.length() > 1) || (fontStyleVariablePunctuationMarks.indexOf(wordStr) != -1)))
					return false;
				if (this.isAllCaps && !wordStr.equals(wordStr.toUpperCase()))
					return false;
				gotBold = (gotBold || lineWords[w].hasAttribute(ImWord.BOLD_ATTRIBUTE));
				gotItalics = (gotItalics || lineWords[w].hasAttribute(ImWord.ITALICS_ATTRIBUTE));
				gotAllCaps = (gotAllCaps || (
						(wordStr.length() > 1)
						&&
						wordStr.equals(wordStr.toUpperCase())
						&&
						!wordStr.equals(wordStr.toLowerCase())
					));
				if (lineWords[w].hasAttribute(ImWord.FONT_SIZE_ATTRIBUTE)) {
					int wfs = lineWords[w].getFontSize();
					int wfsTolerance = (((wordStr.length() == 1) && (fontSizeVariablePunctuationMarks.indexOf(wordStr) != -1)) ? 1 : 0);
					if ((wfs + wfsTolerance) < this.minFontSize)
						return false;
					if (this.maxFontSize < (wfs - wfsTolerance))
						return false;
				}
				lineWordString.append(wordStr);
				if ((w+1) == lineWords.length)
					break;
				if (lineWords[w].getNextWord() != lineWords[w+1])
					lineWordString.append(" ");
				else if ((lineWords[w].getNextRelation() == ImWord.NEXT_RELATION_SEPARATE) && Gamta.insertSpace(lineWords[w].getString(), lineWords[w+1].getString()))
					lineWordString.append(" ");
				else if (lineWords[w].getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
					lineWordString.append(" ");
				else if ((lineWords[w].getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED) && (lineWordString.length() != 0))
					lineWordString.deleteCharAt(lineWordString.length()-1);
			}
			
			//	check font style containment
			if (this.containsBold && !gotBold)
				return false;
			if (this.containsItalics && !gotItalics)
				return false;
			if (this.containsAllCaps && !gotAllCaps)
				return false;
			
			//	no patterns to match, this one looks good
			if (this.startPatterns == null)
				return true;
			
			//	check against start patterns
			for (int p = 0; p < this.startPatterns.length; p++) {
				Matcher m = this.startPatterns[p].matcher(lineWordString);
				if (m.find() && (m.start() == 0))
					return true;
			}
			
			//	no pattern matched
			return false;
		}
	}
	
	private static void markHeadings(ImPage page, int pageImageDpi, HeadingStyleDefined[] headingStyles, ProgressMonitor pm) {
		ImRegion[] pageBlocks = page.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
		
		//	get block words
		ImWord[][] blockWords = new ImWord[pageBlocks.length][];
		for (int b = 0; b < pageBlocks.length; b++) {
			blockWords[b] = pageBlocks[b].getWords();
			Arrays.sort(blockWords[b], ImUtils.textStreamOrder);
		}
		
		/* TODO compute _fractional_ column width over whole document main text, not per page
		 * - articles, not even books, tend to change their column layout ...
		 * - using fraction abstracts from page image resolution, helps if latter is inconsistent
		 * ==> mitigates effect of single-column article header sitting on top of two-column / multi-column layout
		 * ==> consider leaving first page out of this computation altogether if document long enough
		 * 
		 * - use ZJLS_Hertach2015.pdf to test this (main text lines on first page mistaken for headings because mistaken for short due to very large single-column document head)
		 */
		
		//	TODO compute alignment at least from paragraphs as a whole
		
		//	get block parent columns
		//	TODO use column area style template parameter as first choice
		//	TODO get words (fully) inside individual column areas, and use their hull as column
		ImRegion[] pageColumns = page.getRegions(ImRegion.COLUMN_ANNOTATION_TYPE);
		ImRegion[] blockParentColumns = new ImRegion[pageBlocks.length];
		for (int b = 0; b < pageBlocks.length; b++) {
			if ((blockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(blockWords[b][0].getTextStreamType()))
				continue;
			
			//	try and get marked parent column
			for (int c = 0; c < pageColumns.length; c++)
				if (pageColumns[c].bounds.includes(pageBlocks[b].bounds, false)) {
					blockParentColumns[b] = pageColumns[c];
					break;
				}
			if (blockParentColumns[b] != null)
				continue;
			
			//	synthesize parent column from blocks above and below
			int pcLeft = pageBlocks[b].bounds.left;
			int pcRight = pageBlocks[b].bounds.right;
			int pcTop = pageBlocks[b].bounds.top;
			int pcBottom = pageBlocks[b].bounds.bottom;
			for (int cb = 0; cb < pageBlocks.length; cb++) {
				if (cb == b)
					continue;
				if (pageBlocks[cb].bounds.right <= pcLeft)
					continue;
				if (pcRight <= pageBlocks[cb].bounds.left)
					continue;
				if ((blockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(blockWords[b][0].getTextStreamType()))
					continue;
				pcLeft = Math.min(pcLeft, pageBlocks[cb].bounds.left);
				pcRight = Math.max(pcRight, pageBlocks[cb].bounds.right);
				pcTop = Math.min(pcTop, pageBlocks[cb].bounds.top);
				pcBottom = Math.max(pcBottom, pageBlocks[cb].bounds.bottom);
			}
			blockParentColumns[b] = new ImRegion(page.getDocument(), page.pageId, new BoundingBox(pcLeft, pcRight, pcTop, pcBottom), "blockColumn");
		}
		
		//	compute average main text block width (both non-weighted and weighted by block height)
		int mainTextBlockCount = 0;
		int mainTextBlockWidthSum = 0;
		int mainTextBlockHeightSum = 0;
		int mainTextBlockAreaSum = 0;
		for (int b = 0; b < pageBlocks.length; b++) {
			if ((blockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(blockWords[b][0].getTextStreamType()))
				continue;
			mainTextBlockCount++;
			mainTextBlockWidthSum += pageBlocks[b].bounds.getWidth();
			mainTextBlockHeightSum += pageBlocks[b].bounds.getHeight();
			mainTextBlockAreaSum += pageBlocks[b].bounds.getArea();
		}
		if (mainTextBlockCount == 0) {
			pm.setInfo("No main text blocks on page " + page.pageId);
			return;
		}
		int mainTextBlockWidthC = (mainTextBlockWidthSum / mainTextBlockCount);
		int mainTextBlockWidthA = (mainTextBlockAreaSum / mainTextBlockHeightSum);
		pm.setInfo("Average main text block width on page " + page.pageId + " is " + mainTextBlockWidthC + " (based on " + mainTextBlockCount + " blocks) / " + mainTextBlockWidthA + " (based on " + mainTextBlockHeightSum + " block pixel rows)");
		
		//	assess block lines
		for (int b = 0; b < pageBlocks.length; b++) {
			if ((blockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(blockWords[b][0].getTextStreamType()))
				continue;
			pm.setInfo("Assessing lines in block " + pageBlocks[b].bounds);
			pm.setInfo(" - parent column is " + blockParentColumns[b].bounds + ("blockColumn".equals(blockParentColumns[b].getType()) ? " (synthesized)" : ""));
			ImRegion[] blockLines = pageBlocks[b].getRegions(ImRegion.LINE_ANNOTATION_TYPE);
			if (blockLines.length == 0)
				continue;
			Arrays.sort(blockLines, ImUtils.topDownOrder);
			
			//	measure line positions
			ImWord[][] lineWords = new ImWord[blockLines.length][];
			boolean[] lineIsFlushLeft = new boolean[blockLines.length];
			boolean[] lineIsFlushRight = new boolean[blockLines.length];
			boolean[] lineIsShort = new boolean[blockLines.length];
			for (int l = 0; l < blockLines.length; l++) {
				lineWords[l] = getLargestTextStreamWords(blockLines[l].getWords());
				if (lineWords[l].length == 0)
					continue;
				Arrays.sort(lineWords[l], ImUtils.textStreamOrder);
				if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(lineWords[l][0].getTextStreamType()))
					continue;
				pm.setInfo(" - line " + blockLines[l].bounds + ": " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
				
				//	assess line position and length (center alignment against surrounding column to catch short blocks)
				int leftDist = (blockLines[l].bounds.left - blockParentColumns[b].bounds.left);
				lineIsFlushLeft[l] = ((leftDist * 25) < pageImageDpi);
				if (lineIsFlushLeft[l])
					pm.setInfo(" --> flush left");
				int rightDist = (blockParentColumns[b].bounds.right - blockLines[l].bounds.right);
				lineIsFlushRight[l] = ((rightDist * 25) < pageImageDpi);
				if (lineIsFlushRight[l])
					pm.setInfo(" --> flush right");
				lineIsShort[l] = (((blockLines[l].bounds.right - blockLines[l].bounds.left) * 3) < (mainTextBlockWidthA * 2));
				if (lineIsShort[l]) // TODO replace this with measurement used in block line metrics
					pm.setInfo(" --> short");
				
				//	no use looking for headings more than 3 lines down the block
				if (l == 3)
					break;
			}
			
			//	find headings (first three lines at most)
			HeadingStyleDefined[] lineHeadingStyles = new HeadingStyleDefined[blockLines.length]; 
			Arrays.fill(lineHeadingStyles, null);
			for (int l = 0; l < Math.min(blockLines.length, 4); l++) {
				if (lineWords[l] == null)
					break;
				if (lineWords[l].length == 0)
					continue;
				if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(lineWords[l][0].getTextStreamType()))
					continue;
				pm.setInfo(" - line " + blockLines[l].bounds + ": " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
				
				//	check against styles
				for (int s = 0; s < headingStyles.length; s++)
					if (headingStyles[s].matches(blockLines[l], lineWords[l], l, blockLines.length, lineIsFlushLeft[l], lineIsFlushRight[l])) {
						lineHeadingStyles[l] = headingStyles[s];
						pm.setInfo(" --> match at level " + lineHeadingStyles[l].level);
						break;
					}
				
				//	no use looking for headings below non-headings
				if (lineHeadingStyles[l] == null) {
					pm.setInfo(" --> no style match");
					break;
				}
			}
			
			//	no use looking any further if block doesn't start with a heading
			if (lineHeadingStyles[0] == null)
				continue;
			
			//	if we have a full block heading (not block-top), refuse match if block has further lines
			if (lineHeadingStyles[0].isBlock && (lineHeadingStyles[lineHeadingStyles.length-1] == null))
				continue;
			
			//	annotate headings (subsequent lines with same heading style together, unless line is short)
			ImWord headingStart = lineWords[0][0];
			ImWord headingEnd = lineWords[0][lineWords[0].length-1];
			HeadingStyleDefined headingStyle = lineHeadingStyles[0];
			int headingStartLineIndex = 0;
			boolean headingEndLineShort = lineIsShort[0];
			for (int l = 1; l < Math.min(blockLines.length, 4); l++) {
				if (lineWords[l].length == 0)
					continue;
				if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(lineWords[l][0].getTextStreamType()))
					continue;
				if (lineHeadingStyles[l] == null)
					break;
				
				//	extend in same heading style unless line is short
				if ((lineHeadingStyles[l] == lineHeadingStyles[l-1]) && !lineIsShort[l-1]) {
					headingEnd = lineWords[l][lineWords[l].length-1];
					headingEndLineShort = lineIsShort[l];
				}
				
				//	mark heading in current style and start new one in new style
				else {
					markHeading(page, headingStart, headingEnd, headingStyle, lineIsFlushLeft[headingStartLineIndex], lineIsFlushRight[headingStartLineIndex], headingEndLineShort, pm);
					headingStart = lineWords[l][0];
					headingEnd = lineWords[l][lineWords[l].length-1];
					headingStyle = lineHeadingStyles[l];
					headingStartLineIndex = l;
					headingEndLineShort = lineIsShort[l];
				}
			}
			if (headingStyle != null)
				markHeading(page, headingStart, headingEnd, headingStyle, lineIsFlushLeft[headingStartLineIndex], lineIsFlushRight[headingStartLineIndex], headingEndLineShort, pm);
		}
	}
	
	private static void markHeading(ImPage page, ImWord headingStart, ImWord headingEnd, HeadingStyleDefined headingStyle, boolean startLineFlushLeft, boolean startLineFlushRight, boolean endLineShort, ProgressMonitor pm) {
		
		//	accept non-block heading only if (a) it is bold or all-caps as a whole or (b) it (its last line) is short
		if (!headingStyle.isBlock && !endLineShort && !headingStyle.isBold && !headingStyle.isAllCaps)
			return;
		
		//	mark heading, including all the attributes
		ImAnnotation heading = page.getDocument().addAnnotation(headingStart, headingEnd, ImAnnotation.HEADING_TYPE);
		heading.setAttribute("reason", ("" + headingStyle.level));
		heading.setAttribute("level", ("" + headingStyle.level));
		if (!startLineFlushLeft && !startLineFlushRight)
			heading.setAttribute(ImAnnotation.TEXT_ORIENTATION_CENTERED);
		if (headingStyle.isAllCaps)
			heading.setAttribute(ImAnnotation.ALL_CAPS_ATTRIBUTE);
		if (headingStyle.isBold)
			heading.setAttribute(ImAnnotation.BOLD_ATTRIBUTE);
		if (((headingStyle.minFontSize + headingStyle.maxFontSize) / 2) > 6)
			heading.setAttribute(ImAnnotation.FONT_SIZE_ATTRIBUTE, ("" + ((headingStyle.minFontSize + headingStyle.maxFontSize) / 2)));
		straightenHeadingStreamStructure(heading);
		pm.setInfo(" ==> marked heading level " + headingStyle.level + ": " + ImUtils.getString(heading.getFirstWord(), heading.getLastWord(), true));
	}
	
	private static class HeadingStyleObserved {
		/* TODO also observe numbering schemes
		 * - non-repeating numbers --> higher in hierarchy
		 * - repeating numbers --> lower in hierarchy
		 * - numbers with separating dots (e.g. '1.2 Methods') --> lower in hierarchy
		 */
		int fontSize;
		boolean bold;
		boolean allCaps;
		boolean centered;
		String key;
		ArrayList headings = new ArrayList();
		int avgPosDistSquare = -1;
		int firstPageId = Integer.MAX_VALUE;
		int lastPageId = 0;
		HeadingStyleObserved(ImAnnotation heading) {
			this.fontSize = Integer.parseInt((String) heading.getAttribute(ImAnnotation.FONT_SIZE_ATTRIBUTE, "-1"));
			this.bold = heading.hasAttribute(ImAnnotation.BOLD_ATTRIBUTE);
			this.allCaps = heading.hasAttribute(ImAnnotation.ALL_CAPS_ATTRIBUTE);
			this.centered = heading.hasAttribute(ImAnnotation.TEXT_ORIENTATION_CENTERED);
			this.key = getStyleKey(heading);
			this.headings.add(heading);
		}
		static String getStyleKey(ImAnnotation heading) {
			StringBuffer styleKey = new StringBuffer("style");
			int fontSize = Integer.parseInt((String) heading.getAttribute(ImAnnotation.FONT_SIZE_ATTRIBUTE, "-1"));
			if (fontSize != -1)
				styleKey.append("-" + ((fontSize < 10) ? "0" : "") + fontSize);
			if (heading.hasAttribute(ImAnnotation.BOLD_ATTRIBUTE))
				styleKey.append("-" + ImAnnotation.BOLD_ATTRIBUTE);
			if (heading.hasAttribute(ImAnnotation.ALL_CAPS_ATTRIBUTE))
				styleKey.append("-" + ImAnnotation.ALL_CAPS_ATTRIBUTE);
			if (heading.hasAttribute(ImAnnotation.TEXT_ORIENTATION_CENTERED))
				styleKey.append("-" + ImAnnotation.TEXT_ORIENTATION_CENTERED);
			return styleKey.toString();
		}
	}
	
	private static final Comparator annotationTextStreamOrder = new Comparator() {
		public int compare(Object obj1, Object obj2) {
			ImWord imw1 = ((ImAnnotation) obj1).getFirstWord();
			ImWord imw2 = ((ImAnnotation) obj2).getFirstWord();
			return ImUtils.textStreamOrder.compare(imw1, imw2);
		}
	};
	
	private void markHeadings(ImPage page, int docFontSize, int pageImageDpi, HashSet emphasisWords, ProgressMonitor pm, ArrayList headings) {
		ImRegion[] pageBlocks = page.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
		
		//	get block words
		ImWord[][] blockWords = new ImWord[pageBlocks.length][];
		for (int b = 0; b < pageBlocks.length; b++) {
			blockWords[b] = pageBlocks[b].getWords();
			Arrays.sort(blockWords[b], ImUtils.textStreamOrder);
		}
		
		/* TODO compute _fractional_ column width over whole document main text, not per page
		 * - articles, not even books, tend to change their column layout ...
		 * - using fraction abstracts from page image resolution, helps if latter is inconsistent
		 * ==> mitigates effect of single-column article header sitting on top of two-column / multi-column layout
		 * ==> consider leaving first page out of this computation altogether if document long enough
		 * 
		 * - use ZJLS_Hertach2015.pdf to test this (main text lines on first page mistaken for headings because mistaken for short due to very large single-column document head)
		 */
		
		//	get block parent columns
		ImRegion[] pageColumns = page.getRegions(ImRegion.COLUMN_ANNOTATION_TYPE);
		ImRegion[] blockParentColumns = new ImRegion[pageBlocks.length];
		for (int b = 0; b < pageBlocks.length; b++) {
			if ((blockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(blockWords[b][0].getTextStreamType()))
				continue;
			
			//	try and get marked parent column
			for (int c = 0; c < pageColumns.length; c++)
				if (pageColumns[c].bounds.includes(pageBlocks[b].bounds, false)) {
					blockParentColumns[b] = pageColumns[c];
					break;
				}
			if (blockParentColumns[b] != null)
				continue;
			
			//	synthesize parent column from blocks above and below
			int pcLeft = pageBlocks[b].bounds.left;
			int pcRight = pageBlocks[b].bounds.right;
			int pcTop = pageBlocks[b].bounds.top;
			int pcBottom = pageBlocks[b].bounds.bottom;
			for (int cb = 0; cb < pageBlocks.length; cb++) {
				if (cb == b)
					continue;
				if (pageBlocks[cb].bounds.right <= pcLeft)
					continue;
				if (pcRight <= pageBlocks[cb].bounds.left)
					continue;
				if ((blockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(blockWords[b][0].getTextStreamType()))
					continue;
				pcLeft = Math.min(pcLeft, pageBlocks[cb].bounds.left);
				pcRight = Math.max(pcRight, pageBlocks[cb].bounds.right);
				pcTop = Math.min(pcTop, pageBlocks[cb].bounds.top);
				pcBottom = Math.max(pcBottom, pageBlocks[cb].bounds.bottom);
			}
			blockParentColumns[b] = new ImRegion(page.getDocument(), page.pageId, new BoundingBox(pcLeft, pcRight, pcTop, pcBottom), "blockColumn");
		}
		
		//	compute average main text block width (both non-weighted and weighted by block height)
		int mainTextBlockCount = 0;
		int mainTextBlockWidthSum = 0;
		int mainTextBlockHeightSum = 0;
		int mainTextBlockAreaSum = 0;
		for (int b = 0; b < pageBlocks.length; b++) {
			if ((blockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(blockWords[b][0].getTextStreamType()))
				continue;
			mainTextBlockCount++;
			mainTextBlockWidthSum += (pageBlocks[b].bounds.right - pageBlocks[b].bounds.left);
			mainTextBlockHeightSum += (pageBlocks[b].bounds.bottom - pageBlocks[b].bounds.top);
			mainTextBlockAreaSum += ((pageBlocks[b].bounds.right - pageBlocks[b].bounds.left) * (pageBlocks[b].bounds.bottom - pageBlocks[b].bounds.top));
		}
		if (mainTextBlockCount == 0) {
			pm.setInfo("No main text blocks on page " + page.pageId);
			return;
		}
		int mainTextBlockWidthC = (mainTextBlockWidthSum / mainTextBlockCount);
		int mainTextBlockWidthA = (mainTextBlockAreaSum / mainTextBlockHeightSum);
		pm.setInfo("Average main text block width on page " + page.pageId + " is " + mainTextBlockWidthC + " (based on " + mainTextBlockCount + " blocks) / " + mainTextBlockWidthA + " (based on " + mainTextBlockHeightSum + " block pixel rows)");
		
		//	assess block lines
		for (int b = 0; b < pageBlocks.length; b++) {
			if ((blockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(blockWords[b][0].getTextStreamType()))
				continue;
			pm.setInfo("Assessing lines in block " + pageBlocks[b].bounds);
			pm.setInfo(" - parent column is " + blockParentColumns[b].bounds + ("blockColumn".equals(blockParentColumns[b].getType()) ? " (synthesized)" : ""));
			ImRegion[] blockLines = pageBlocks[b].getRegions(ImRegion.LINE_ANNOTATION_TYPE);
			Arrays.sort(blockLines, ImUtils.topDownOrder);
			
			//	classify lines
			ImWord[][] lineWords = new ImWord[blockLines.length][];
			boolean[] lineIsShort = new boolean[blockLines.length];
			boolean[] lineIsCentered = new boolean[blockLines.length];
			boolean[] lineIsAllCaps = new boolean[blockLines.length];
			boolean[] lineHasEmphasis = new boolean[blockLines.length];
			boolean[] lineIsEmphasized = new boolean[blockLines.length];
			boolean[] lineHasBoldEmphasis = new boolean[blockLines.length];
			boolean[] lineIsBold = new boolean[blockLines.length];
			int[] lineFontSize = new int[blockLines.length];
			boolean[] lineIsLargeFont = new boolean[blockLines.length];
			for (int l = 0; l < blockLines.length; l++) {
				lineWords[l] = getLargestTextStreamWords(blockLines[l].getWords());
				if (lineWords[l].length == 0)
					continue;
				Arrays.sort(lineWords[l], ImUtils.textStreamOrder);
				if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(lineWords[l][0].getTextStreamType()))
					continue;
				pm.setInfo(" - line " + blockLines[l].bounds + ": " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
				
				//	assess line measurements (center alignment against surrounding column to catch short blocks)
				lineIsShort[l] = (((blockLines[l].bounds.right - blockLines[l].bounds.left) * 3) < (mainTextBlockWidthA * 2));
				if (lineIsShort[l])
					pm.setInfo(" --> short");
				int leftDist = (blockLines[l].bounds.left - blockParentColumns[b].bounds.left);
				int rightDist = (blockParentColumns[b].bounds.right - blockLines[l].bounds.right);
				lineIsCentered[l] = (
						(((leftDist * 9) < (rightDist * 10)) && ((rightDist * 9) < (leftDist * 10)))
						||
						(((leftDist * 25) < pageImageDpi) && ((rightDist * 25) < pageImageDpi))
					);
				if (lineIsCentered[l])
					pm.setInfo(" --> centered");
				
				//	assess line words
				int lineWordCount = 0;
				int lineAllCapWordCount = 0;
				int lineEmphasisWordCount = 0;
				int lineBoldWordCount = 0;
				int lineNonBoldBoldWordCount = 0;
				int lineFontSizeSum = 0;
				int lineFontSizeWordCount = 0;
				for (int w = 0; w < lineWords[l].length; w++) {
					String lineWordString = lineWords[l][w].getString();
					if (lineWordString == null)
						continue;
					if (Gamta.isWord(lineWordString)) {
						lineWordCount++;
						if (lineWordString.equals(lineWordString.toUpperCase()))
							lineAllCapWordCount++;
						if (emphasisWords.contains(lineWords[l][w]))
							lineEmphasisWordCount++;
						if (lineWords[l][w].hasAttribute(ImWord.BOLD_ATTRIBUTE))
							lineBoldWordCount++;
						else if ((lineWords[l][w].getString().length() < 2) && (fontStyleVariablePunctuationMarks.indexOf(lineWords[l][w].getString()) != -1))
							lineNonBoldBoldWordCount++;
					}
					if (lineWords[l][w].hasAttribute(ImWord.FONT_SIZE_ATTRIBUTE)) try {
						lineFontSizeSum += lineWords[l][w].getFontSize();
						lineFontSizeWordCount++;
					} catch (NumberFormatException nfe) {}
				}
				lineIsAllCaps[l] = ((lineWordCount != 0) && (lineAllCapWordCount == lineWordCount));
				if (lineIsAllCaps[l])
					pm.setInfo(" --> all caps");
				lineHasEmphasis[l] = (lineEmphasisWordCount != 0);
				if (lineHasEmphasis[l])
					pm.setInfo(" --> has emphasis");
				lineIsEmphasized[l] = ((lineWordCount != 0) && (lineEmphasisWordCount == lineWordCount));
				if (lineIsEmphasized[l])
					pm.setInfo(" --> emphasized");
				lineHasBoldEmphasis[l] = (lineBoldWordCount != 0);
				if (lineHasBoldEmphasis[l])
					pm.setInfo(" --> has bold emphasis");
				lineIsBold[l] = ((lineWordCount != 0) && ((lineBoldWordCount + lineNonBoldBoldWordCount) == lineWordCount));
				if (lineIsBold[l])
					pm.setInfo(" --> bold");
				lineFontSize[l] = ((lineFontSizeWordCount == 0) ? -1 : ((lineFontSizeSum + (lineFontSizeWordCount / 2)) / lineFontSizeWordCount));
				lineIsLargeFont[l] = ((docFontSize > 6) && (lineFontSize[l] > docFontSize));
				if (lineIsLargeFont[l])
					pm.setInfo(" --> large font " + lineFontSize[l]);
				
				//	no use looking for headings more than 3 lines down the block
				if (l == 3)
					break;
			}
			
			//	find headings (first three lines at most)
			int[] lineIsHeadingBecause = new int[blockLines.length]; 
			Arrays.fill(lineIsHeadingBecause, -1);
			for (int l = 0; l < Math.min(blockLines.length, 4); l++) {
				if (lineWords[l].length == 0)
					continue;
				if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(lineWords[l][0].getTextStreamType()))
					continue;
				
				//	no heading without words
				boolean noWords = true;
				for (int w = 0; w < lineWords[l].length; w++)
					if ((lineWords[l][w].getString().length() > 2) && Gamta.isWord(lineWords[l][w].getString())) {
						noWords = false;
						break;
					}
				if (noWords)
					break;
				
				//	no heading starts with a citation
				ImAnnotation[] lineStartAnnots = page.getDocument().getAnnotations(lineWords[l][0], null);
				boolean gotCitationStart = false;
				for (int a = 0; a < lineStartAnnots.length; a++)
					if (lineStartAnnots[a].getType().endsWith("Citation")) {
						gotCitationStart = true;
						break;
					}
				if (gotCitationStart)
					break;
				
				//	TODO refine this
				
				//	below average font size + not all caps + not bold ==> not a heading
				if ((lineFontSize[l] < docFontSize) && !lineIsBold[l] && !lineIsAllCaps[l])
					break;
				
				//	short + large-font ==> heading
				if (lineIsBold[l] && lineIsLargeFont[l]) {
					pm.setInfo(" ==> heading (0): " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
					lineIsHeadingBecause[l] = 0;
					continue;
				}
				
				//	short + large-font ==> heading
				if (lineIsShort[l] && lineIsLargeFont[l]) {
					pm.setInfo(" ==> heading (1): " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
					lineIsHeadingBecause[l] = 1;
					continue;
				}
				
				//	short + bold + single-line-block ==> heading
				if (lineIsShort[l] && lineIsBold[l] && (blockLines.length == 1)) {
					pm.setInfo(" ==> heading (2): " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
					lineIsHeadingBecause[l] = 2;
					continue;
				}
				
				//	short + all-caps + single-line-block ==> heading
				if (lineIsShort[l] && lineIsAllCaps[l] && (blockLines.length == 1)) {
					pm.setInfo(" ==> heading (3): " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
					lineIsHeadingBecause[l] = 3;
					continue;
				}
				
				//	short + centered ==> heading
				if (lineIsShort[l] && lineIsCentered[l]) {
					pm.setInfo(" ==> heading (4): " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
					lineIsHeadingBecause[l] = 4;
					continue;
				}
				
				//	centered + all-caps ==> heading
				if (lineIsCentered[l] && lineIsAllCaps[l]) {
					pm.setInfo(" ==> heading (5): " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
					lineIsHeadingBecause[l] = 5;
					continue;
				}
				
				//	short + bold + block-top-line ==> heading
//				if (lineIsShort[l] && lineIsBold[l] && (l == 0)) {
				if (lineIsShort[l] && lineIsBold[l]) {
					pm.setInfo(" ==> heading (6): " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
					lineIsHeadingBecause[l] = 6;
					continue;
				}
				
				//	centered + emphasis + block-top-line + short-centered-line-below ==> heading
				if (lineIsCentered[l] && lineHasEmphasis[l] && (l == 0) && (blockLines.length > 1) && lineIsShort[l+1] && lineIsCentered[l+1]) {
					pm.setInfo(" ==> heading (7): " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
					lineIsHeadingBecause[l] = 4; // conflate with 4, as this is the two-line case of 4
					continue;
				}
				
				//	no use looking for headings below non-headings
				break;
			}
			
			
			//	rule out lines if lines further down the block exhibit same properties (not for large font, though ... document titles may have 4 or more lines)
			if ((blockLines.length >= 4) && !lineIsLargeFont[0] && (lineIsHeadingBecause[3] != -1)) {
				pm.setInfo(" ==> ignoring potential headings for style continuing through block");
				for (int l = 2; l >= 0; l--) {
					if (lineIsHeadingBecause[l] == lineIsHeadingBecause[3])
						lineIsHeadingBecause[l] = -1;
					else break;
				}
				if (lineIsHeadingBecause[0] == -1)
					continue;
			}
			
			//	annotate headings (subsequent lines with same heading reason together, unless line is short)
			ImWord headingStart = null;
			ImWord headingEnd = null;
			int headingReason = -1;
			int headingStartLineIndex = -1;
			for (int l = 0; l < Math.min(blockLines.length, 4); l++) {
				if ((l == 0) && (lineIsHeadingBecause[l] == -1))
					break;
				if (lineWords[l].length == 0)
					continue;
				if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(lineWords[l][0].getTextStreamType()))
					continue;
				if (l == 0) {
					headingStart = lineWords[l][0];
					headingEnd = lineWords[l][lineWords[l].length-1];
					headingReason = lineIsHeadingBecause[l];
					headingStartLineIndex = l;
				}
				else if ((lineIsHeadingBecause[l] == lineIsHeadingBecause[l-1]) && !lineIsShort[l-1])
					headingEnd = lineWords[l][lineWords[l].length-1];
				else {
					if (headingReason != -1) {
						ImAnnotation heading = page.getDocument().addAnnotation(headingStart, headingEnd, ImAnnotation.HEADING_TYPE);
						heading.setAttribute("reason", ("" + headingReason));
						if (lineIsCentered[headingStartLineIndex])
							heading.setAttribute(ImAnnotation.TEXT_ORIENTATION_CENTERED);
						if (lineIsAllCaps[headingStartLineIndex])
							heading.setAttribute(ImAnnotation.ALL_CAPS_ATTRIBUTE);
						if (lineIsBold[headingStartLineIndex])
							heading.setAttribute(ImAnnotation.BOLD_ATTRIBUTE);
						if (lineFontSize[headingStartLineIndex] > 6)
							heading.setAttribute(ImAnnotation.FONT_SIZE_ATTRIBUTE, ("" + lineFontSize[headingStartLineIndex]));
						headings.add(heading);
						straightenHeadingStreamStructure(heading);
						pm.setInfo(" ==> marked heading: " + ImUtils.getString(heading.getFirstWord(), heading.getLastWord(), true));
					}
					headingStart = lineWords[l][0];
					headingEnd = lineWords[l][lineWords[l].length-1];
					headingReason = lineIsHeadingBecause[l];
					headingStartLineIndex = l;
				}
				if (lineIsHeadingBecause[l] == -1) {
					headingStart = null;
					headingEnd = null;
					headingReason = -1;
					headingStartLineIndex = -1;
					break;
				}
			}
			if (headingReason != -1) {
				ImAnnotation heading = page.getDocument().addAnnotation(headingStart, headingEnd, ImAnnotation.HEADING_TYPE);
				heading.setAttribute("reason", ("" + headingReason));
				if (lineIsCentered[headingStartLineIndex])
					heading.setAttribute(ImAnnotation.TEXT_ORIENTATION_CENTERED);
				if (lineIsAllCaps[headingStartLineIndex])
					heading.setAttribute(ImAnnotation.ALL_CAPS_ATTRIBUTE);
				if (lineIsBold[headingStartLineIndex])
					heading.setAttribute(ImAnnotation.BOLD_ATTRIBUTE);
				if (lineFontSize[headingStartLineIndex] > 6)
					heading.setAttribute(ImAnnotation.FONT_SIZE_ATTRIBUTE, ("" + lineFontSize[headingStartLineIndex]));
				headings.add(heading);
				straightenHeadingStreamStructure(heading);
				pm.setInfo(" ==> marked heading: " + ImUtils.getString(heading.getFirstWord(), heading.getLastWord(), true));
			}
		}
	}
	
	private static void straightenHeadingStreamStructure(ImAnnotation heading) {
		if (heading.getFirstWord().getPreviousWord() != null)
			heading.getFirstWord().getPreviousWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
		for (ImWord imw = heading.getFirstWord(); (imw != heading.getLastWord()) && (imw != null); imw = imw.getNextWord()) {
			if (imw.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
				imw.setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
		}
		heading.getLastWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
	}
	
	private static String getCaptionStartForCheck(ImAnnotation caption) {
		for (ImWord imw = caption.getFirstWord(); imw != null; imw = imw.getNextWord()) {
			String imwString = imw.getString();
			if ((imwString != null) && imwString.matches("([0-9]+(\\s*[a-z])?)|[IiVvXxLl]+|[a-zA-Z]"))
				return ImUtils.getString(caption.getFirstWord(), imw, true);
		}
		return ImUtils.getString(caption.getFirstWord(), caption.getLastWord(), true);
	}
	
	private static final boolean DEBUG_IS_ARTIFACT = true;
	private boolean isArtifact(ImRegion block, ImWord[] blockWords, int dpi) {
		if (DEBUG_IS_ARTIFACT) System.out.println("Assessing block " + block.bounds + " on page " + block.pageId + " for artifact");
		if (blockWords.length == 0) {
			if (DEBUG_IS_ARTIFACT) System.out.println(" ==> no words at all");
			return false;
		}
		
		//	check if cut-out still possible
		Arrays.sort(blockWords, ImUtils.textStreamOrder);
		if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(blockWords[0].getTextStreamType())) {
			if (DEBUG_IS_ARTIFACT) System.out.println(" ==> already cut from main text");
			return false;
		}
		
		//	check dimensions
		if ((block.bounds.right - block.bounds.left) < dpi) {
			if (DEBUG_IS_ARTIFACT) System.out.println(" ==> too narrow");
			return false;
		}
		if ((block.bounds.bottom - block.bounds.top) < dpi) {
			if (DEBUG_IS_ARTIFACT) System.out.println(" ==> too low");
			return false;
		}
		
		//	get block area
		int blockArea = ((block.bounds.right - block.bounds.left) * (block.bounds.bottom - block.bounds.top));
		
		//	assess word distribution (we really have to do it pixel by pixel in order to not miss word overlap)
		int[][] wordDistribution = new int[block.bounds.right - block.bounds.left][block.bounds.bottom - block.bounds.top];
		for (int c = 0; c < wordDistribution.length; c++)
			Arrays.fill(wordDistribution[c], 0);
		for (int w = 0; w < blockWords.length; w++)
			for (int c = blockWords[w].bounds.left; c < blockWords[w].bounds.right; c++) {
				for (int r = blockWords[w].bounds.top; r < blockWords[w].bounds.bottom; r++)
					wordDistribution[c - block.bounds.left][r - block.bounds.top]++;
			}
		int maxWordOverlap = 0;
		for (int c = 0; c < wordDistribution.length; c++) {
			for (int r = 0; r < wordDistribution[c].length; r++)
				maxWordOverlap = Math.max(maxWordOverlap, wordDistribution[c][r]);
		}
		if (DEBUG_IS_ARTIFACT) System.out.println(" - maximum word overlap is " + maxWordOverlap);
		int[] wordOverlapDistribution = new int[maxWordOverlap + 1];
		for (int c = 0; c < wordDistribution.length; c++) {
			for (int r = 0; r < wordDistribution[c].length; r++)
				wordOverlapDistribution[wordDistribution[c][r]]++;
		}
		if (DEBUG_IS_ARTIFACT) System.out.println(" - word overlap distribution:");
		int blockWordArea = 0;
		for (int c = 0; c < wordOverlapDistribution.length; c++) {
			if (DEBUG_IS_ARTIFACT) System.out.println("   - " + c + ": " + wordOverlapDistribution[c] + ", " + ((wordOverlapDistribution[c] * 100) / blockArea) + "%");
			if (c != 0)
				blockWordArea += wordOverlapDistribution[c];
		}
		if (DEBUG_IS_ARTIFACT) System.out.println(" - block word ratio is " + ((blockWordArea * 100) / blockArea) + "% (" + blockWordArea + "/" + blockArea + ")");
		
		//	sparse block (less than 10% covered with words)
		if ((blockWordArea * 10) < blockArea) {
			if (DEBUG_IS_ARTIFACT) System.out.println(" ==> artifact for very sparse words");
			return true;
		}
		
		//	words chaotic (at least 10% of word area with overlap)
		if ((1 < maxWordOverlap) && (wordOverlapDistribution[1] < (wordOverlapDistribution[2] * 10))) {
			if (DEBUG_IS_ARTIFACT) System.out.println(" ==> artifact for word overlap");
			return true;
		}
		
		//	words very chaotic (maximum overlap of three or more)
		if (2 < maxWordOverlap) {
			if (DEBUG_IS_ARTIFACT) System.out.println(" ==> artifact for multi-word overlap");
			return true;
		}
		
		//	this one looks at least marginally normal
		if (DEBUG_IS_ARTIFACT) System.out.println(" ==> not an artifact");
		return false;
	}
	
	private static final boolean DEBUG_IS_FOOTNOTE = false;
	private static boolean isFootnote(ImRegion paragraph, ImWord[] paragraphWords, int docFontSize, Pattern[] startPatterns, int minFontSize, int maxFontSize) {
		
		//	run font-size matching on paragraph level (let's not get thrown off by some super scripts or sub scripts)
		if (((0 < minFontSize) || (maxFontSize < 72)) && !isFontSizeMatch(paragraphWords, minFontSize, maxFontSize, DEBUG_IS_FOOTNOTE))
			return false;
		
		//	if footnote is large (> 50% of page width, or > 30 words), require font size to be lower than in main text (even if we have a style template ... numbered enumerations, etc.)
		int paragraphWidth = paragraph.bounds.getWidth();
		int pageWidth = paragraph.getPage().bounds.getWidth();
		if (((paragraphWidth * 2) > pageWidth) || (paragraphWords.length > 30)) {
			int paragraphFontSize = getMatchFontSize(paragraphWords, minFontSize, maxFontSize);
			if (paragraphFontSize >= docFontSize) {
				if (DEBUG_IS_FOOTNOTE) System.out.println(" ==> font too large for big footnote (" + ((paragraphWidth * 100) / pageWidth) + "% of page width, " + paragraphWords.length + " words, font size " + paragraphFontSize + " vs. " + docFontSize + " in doc)");
				return false;
			}
		}
		
		//	find end of first line
		ImWord firstLineEnd = getParagraphFirstLineEnd(paragraphWords);
		
		//	concatenate first line of paragraph
		String paragraphFirstLine = ImUtils.getString(paragraphWords[0], firstLineEnd, true);
		if (DEBUG_IS_FOOTNOTE) System.out.println(" - footnote test first line is " + paragraphFirstLine);
		
		//	test paragraph start against detector patterns
		for (int p = 0; p < startPatterns.length; p++) {
			Matcher startMatcher = startPatterns[p].matcher(paragraphFirstLine);
			if (startMatcher.find() && (startMatcher.start() == 0)) {
				if (DEBUG_IS_FOOTNOTE) System.out.println(" ==> pattern match");
				return true;
			}
		}
		
		//	no match found
		if (DEBUG_IS_FOOTNOTE) System.out.println(" ==> no pattern match");
		return false;
	}
	
	private static class CaptionStartPattern {
		static final char GENERIC_TYPE = 'G';
		static final char FIGURE_TYPE = 'F';
		static final char TABLE_TYPE = 'T';
		static final char PLATE_TYPE = 'P';
		static final char PLATE_PART_TYPE = 'S';
		final char type;
		final Pattern pattern;
		private CaptionStartPattern(char type, Pattern pattern) {
			this.type = type;
			this.pattern = pattern;
		}
		boolean isFigureCaption() {
			return (this.type == FIGURE_TYPE);
		}
		boolean isTableCaption() {
			return (this.type == TABLE_TYPE);
		}
		boolean isPlateCaption() {
			return (this.type == PLATE_TYPE);
		}
		boolean isPlateSubCaption() {
			return (this.type == PLATE_PART_TYPE);
		}
		Matcher matcher(CharSequence cs) {
			return this.pattern.matcher(cs);
		}
		static CaptionStartPattern createPattern(String patternString, boolean caseSensitive) throws PatternSyntaxException {
			char type = CaptionStartPattern.GENERIC_TYPE;
			if (patternString.matches("[A-Z]\\:.*")) {
				type = patternString.charAt(0);
				patternString = patternString.substring("T:".length()).trim();
			}
			return createPattern(patternString, type, caseSensitive);
		}
		static CaptionStartPattern createPattern(String patternString, char type, boolean caseSensitive) throws PatternSyntaxException {
			return new CaptionStartPattern(type, Pattern.compile(patternString, (caseSensitive ? 0 : Pattern.CASE_INSENSITIVE)));
		}
		static CaptionStartPattern[] createPatterns(String[] patternStrings, char type, boolean caseSensitive) {
			if (patternStrings == null)
				return new CaptionStartPattern[0];
			ArrayList patternList = new ArrayList(patternStrings.length);
			for (int p = 0; p < patternStrings.length; p++) try {
				patternList.add(createPattern(patternStrings[p], type, caseSensitive));
			} catch (PatternSyntaxException pse) {}
			return ((CaptionStartPattern[]) patternList.toArray(new CaptionStartPattern[patternList.size()]));			
		}
	}
	
	private static class CaptionPatternMatch {
		final String startLineString;
		final CaptionStartPattern startPattern;
		final String startPatternMatch;
		final char type;
		CaptionPatternMatch(String startLineString, CaptionStartPattern startPattern, String startPatternMatch, char type) {
			this.startLineString = startLineString;
			this.startPattern = startPattern;
			this.startPatternMatch = startPatternMatch;
			this.type = type;
		}
	}
	
	private static HashMap markBlockCaptions(ImDocument doc, ImRegion block, ImRegion[] pageParagraphs, CaptionStartPattern[] startPatterns, CaptionStartPattern[] defaultStartPatterns, int minFontSize, int maxFontSize, boolean startIsBold, ProgressMonitor pm) {
		ArrayList blockParagraphList = new ArrayList();
		HashMap blockParagraphsToWords = new HashMap();
		for (int p = 0; p < pageParagraphs.length; p++) {
			if (!pageParagraphs[p].bounds.liesIn(block.bounds, false))
				continue; // this one belongs to a different block
			ImWord[] paragraphWords = getLargestTextStreamWords(pageParagraphs[p].getWords());
			if (paragraphWords.length == 0)
				continue; // nothing to work with
			Arrays.sort(paragraphWords, ImUtils.textStreamOrder);
			if (doc == null) {
				if (ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(paragraphWords[0].getTextStreamType())) {}
				else if (ImWord.TEXT_STREAM_TYPE_CAPTION.equals(paragraphWords[0].getTextStreamType())) {}
				else continue; // in visualization mode, we want to match previously marked captions along with main text
			}
			else {
				if (ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(paragraphWords[0].getTextStreamType())) {}
				else continue; // none of our business if not in visualization mode
			}
			blockParagraphList.add(pageParagraphs[p]);
			blockParagraphsToWords.put(pageParagraphs[p], paragraphWords);
		}
		if (blockParagraphList.isEmpty())
			return null;
		
		//	line up paragraphs in top-down order
		ImRegion[] blockParagraphs = ((ImRegion[]) blockParagraphList.toArray(new ImRegion[blockParagraphList.size()]));
		Arrays.sort(blockParagraphs, ImUtils.topDownOrder);
		
		//	compute average paragraph distance
		int blockParagraphDistSum = 0;
		for (int p = 1; p < blockParagraphs.length; p++)
			blockParagraphDistSum += Math.max(0, (blockParagraphs[p].bounds.top - blockParagraphs[p-1].bounds.bottom));
		int avgBlockParagraphDist = ((blockParagraphs.length < 2) ? 0 : ((blockParagraphDistSum + (blockParagraphs.length / 2)) / (blockParagraphs.length - 1)));
		
		//	get minimum distance below caption (12th of an inch, some 2mm)
		int minAboveCaptionParagraphDist = (block.getPage().getImageDPI() / 12);
		
		//	check paragraphs for caption
		CaptionPatternMatch[] paragraphPatternMatches = new CaptionPatternMatch[blockParagraphs.length];
		int[] matchFontSizes = new int[blockParagraphs.length];
		ImWord[] matchStartWords = new ImWord[blockParagraphs.length];
		ImWord[] matchEndWords = new ImWord[blockParagraphs.length];
		Arrays.fill(matchFontSizes, -1);
		for (int p = 0; p < blockParagraphs.length; p++) {
			pm.setInfo("Testing paragraph " + blockParagraphs[p].bounds + " on page " + block.pageId + " for caption");
			
			//	compute distance to predecessor
			int prevParagraphDist = ((p == 0) ? Integer.MAX_VALUE : Math.max(0, (blockParagraphs[p].bounds.top - blockParagraphs[p-1].bounds.bottom)));
			
			//	test for (start of) caption
			ImWord[] paragraphWords = ((ImWord[]) blockParagraphsToWords.get(blockParagraphs[p]));
			paragraphPatternMatches[p] = getCaptionPatternMatch(blockParagraphs[p], paragraphWords, startPatterns, defaultStartPatterns, minFontSize, maxFontSize, startIsBold);
			if (paragraphPatternMatches[p] != null) {
				//	TODO prevent marking captions inside larger main text blocks, unless at very top (bottom right of Page 10 in TableTest/Milleretal2014Anthracotheres.pdf.clean.imf)
				//	==> TODO check distance to predecessor (should be above average)
				if (prevParagraphDist < Math.max(minAboveCaptionParagraphDist, avgBlockParagraphDist)) {
					paragraphPatternMatches[p] = null;
					pm.setInfo(" ==> not a caption (pattern match, but too close)");
					continue;
				}
				matchFontSizes[p] = getMatchFontSize(paragraphWords, minFontSize, maxFontSize);
				matchStartWords[p] = paragraphWords[0];
				matchEndWords[p] = paragraphWords[paragraphWords.length - 1];
				pm.setInfo(" ==> caption detected");
				continue;
			}
			
			//	no predecessor to attach to, or too far up (distance to predecessor should be below average for continuation)
			if ((p == 0) || (matchStartWords[p-1] == null) || (matchEndWords[p-1] == null) || (matchFontSizes[p-1] == -1) || (prevParagraphDist > avgBlockParagraphDist)) {
				pm.setInfo(" ==> not a caption (and none above to continue from)");
				continue;
			}
			
			//	check for continuous text stream
			if (!matchEndWords[p-1].getTextStreamId().equals(paragraphWords[0].getTextStreamId())) {
				pm.setInfo(" ==> not a caption (broken text stream)");
				continue;
			}
			else if (paragraphWords[0].getTextStreamPos() < matchEndWords[p-1].getTextStreamPos()) {
				pm.setInfo(" ==> not a caption (broken text stream order)");
				continue;
			}
			
			//	check sentence start (continuation must come from elsewhere)
			String firstWord = paragraphWords[0].getString();
			if (firstWord.equals(firstWord.toLowerCase())) {
				pm.setInfo(" ==> not a caption (sentence continuation)");
				continue;
			}
			
			//	check font size
			int paragraphFontSize = getMatchFontSize(paragraphWords, minFontSize, maxFontSize);
			if (paragraphFontSize != matchFontSizes[p-1]) {
				pm.setInfo(" ==> not a caption (font size mismatch)");
				continue;
			}
			
			//	mark for attaching
			matchFontSizes[p] = paragraphFontSize;
			matchStartWords[p] = paragraphWords[0];
			matchEndWords[p] = paragraphWords[paragraphWords.length - 1];
			pm.setInfo(" ==> caption continuation detected");
		}
		
		//	clean up any false positives in middle of block
		if (blockParagraphs.length > 2) {
			boolean[] matchOnBlockEdge = new boolean[blockParagraphs.length];
			Arrays.fill(matchOnBlockEdge, false);
			for (int p = 0; p < blockParagraphs.length; p++) {
				if ((matchStartWords[p] == null) || (matchEndWords[p] == null) || (matchFontSizes[p] == -1))
					break;
				matchOnBlockEdge[p] = true;
			}
			for (int p = (blockParagraphs.length - 1); p >= 0; p--) {
				if ((matchStartWords[p] == null) || (matchEndWords[p] == null) || (matchFontSizes[p] == -1))
					break;
				matchOnBlockEdge[p] = true;
			}
			for (int p = 0; p < blockParagraphs.length; p++) {
				if (matchOnBlockEdge[p])
					continue; // connected to either edge of block
				if ((matchStartWords[p] == null) && (matchEndWords[p] == null) && (matchFontSizes[p] == -1))
					continue; // nothing to clean up here
				paragraphPatternMatches[p] = null;
				matchFontSizes[p] = -1;
				matchStartWords[p] = null;
				matchEndWords[p] = null;
				pm.setInfo(" ==> cleaned up mid-block false positive at " + blockParagraphs[p].bounds);
			}
		}
		
		//	TODO also use placement of captions relative to targets (based upon style)
		
		/* TODO use style vote to filter false positive captions:
		 * - create caption _candidates_ first, holding:
		 *   - words
		 *   - matching pattern
		 *   - bounding box
		 *   - above and below distances
		 * - compare caption candidates by style:
		 *   - bold start
		 *   - start words
		 *   - numbering
		 * - filter style outliers (non-bold starts if majority bold, etc.)
		 * - filter morphological outliers (title case start is majority is all-caps, etc.)
		 * - filter based upon numbers (should be unique in document and form increasing sequence)
		 *   - do this separately for figure captions, plate captions, and table captions (each have their own numbering sequence)
		 * - filter distance outliers (ones too close to text above or below)
		 */
		
		//	mark captions
		HashMap blockCaptionsByBounds = null;
		for (int p = 0; p < blockParagraphs.length; p++) {
			
			//	no pattern match to work with
			if (paragraphPatternMatches[p] == null)
				continue;
			
			//	start with matched paragraph ...
			CaptionPatternMatch captionPatternMatch = paragraphPatternMatches[p];
			ImWord[] paragraphWords = ((ImWord[]) blockParagraphsToWords.get(blockParagraphs[p]));
			ImWord[] captionWords = paragraphWords;
			BoundingBox captionBounds = blockParagraphs[p].bounds;
			
			//	and attach any continuing matches
			while ((p+1) < blockParagraphs.length) {
				if ((matchFontSizes[p+1] == -1) || (matchStartWords[p+1] == null) || (matchEndWords[p+1] == null))
					break;
				if (paragraphPatternMatches[p+1] != null)
					break;
				
				//	TODO try and attach only if previous paragraph is extremely short (start pattern match only)
				
				//	TODO find more filters
				
				//	attach continuing paragraph
				ImWord[] nParagraphWords = ((ImWord[]) blockParagraphsToWords.get(blockParagraphs[p+1]));
				ImWord[] nCaptionWords = new ImWord[captionWords.length + nParagraphWords.length];
				System.arraycopy(captionWords, 0, nCaptionWords, 0, captionWords.length);
				System.arraycopy(nParagraphWords, 0, nCaptionWords, captionWords.length, nParagraphWords.length);
				captionWords = nCaptionWords;
				captionBounds = captionBounds.union(blockParagraphs[p+1].bounds);
				
				//	move on (no need to revisit this one in main loop, either)
				p++;
			}
			
			//	we're in visualization mode, only store bounding box
			if (doc == null) {
				if (blockCaptionsByBounds == null)
					blockCaptionsByBounds = new HashMap();
				blockCaptionsByBounds.put(captionBounds, "");
			}
			
			//	we're in processing mode, do the real thing
			else {
				
				//	annotate caption
				ImAnnotation caption = null;
				synchronized (doc) {
					caption = ImUtils.makeStream(captionWords, ImWord.TEXT_STREAM_TYPE_CAPTION, CAPTION_TYPE);
					if (caption == null)
						continue;
					ImUtils.orderStream(captionWords, ImUtils.leftRightTopDownOrder);
				}
				
				//	index annotation
				if (blockCaptionsByBounds == null)
					blockCaptionsByBounds = new HashMap();
				blockCaptionsByBounds.put(captionBounds, caption);
				
				//	unless we have a plate, we're done with this one
				if (captionPatternMatch.startPattern.isPlateCaption())
					addPlateSubCaptionPatternStarts(caption, startPatterns, pm);
			}
		}
		
		//	return indexed captions
		return blockCaptionsByBounds;
	}
	
	private static boolean addPlateSubCaptionPatternStarts(ImAnnotation caption, CaptionStartPattern[] startPatterns, ProgressMonitor pm) {
		
		//	line up caption text together with words
		ImTokenSequence captionText = new ImTokenSequence(caption.getFirstWord(), caption.getLastWord(), false);
		
		//	identify sub captions
		StringVector subCaptionStarts = new StringVector();
		StringVector subCaptionStartIDs = new StringVector();
		addPlateSubCaptionStarts(captionText, startPatterns, true, subCaptionStarts, subCaptionStartIDs, pm);
		if (subCaptionStartIDs.isEmpty())
			addPlateSubCaptionStarts(captionText, startPatterns, false, subCaptionStarts, subCaptionStartIDs, pm);
		if (subCaptionStartIDs.isEmpty())
			return false;
		
		//	store sub caption starts
		caption.setAttribute("subCaptionStarts", subCaptionStarts.concatStrings(" & "));
		caption.setAttribute("subCaptionStartIDs", subCaptionStartIDs.concatStrings(" "));
		
		//	indicate we found something
		return true;
	}
	
	private static void addPlateSubCaptionStarts(ImTokenSequence captionText, CaptionStartPattern[] startPatterns, boolean subCaptionPatternsOnly, StringVector subCaptionStarts, StringVector subCaptionStartIDs, ProgressMonitor pm) {
		for (int t = 0; t < startPatterns.length; t++) {
			if (subCaptionPatternsOnly && !startPatterns[t].isPlateSubCaption())
				continue;
			if (startPatterns[t].isTableCaption())
				continue;
			if (startPatterns[t].isPlateCaption())
				continue;
			for (Matcher m = startPatterns[t].matcher(captionText); m.find();) {
				if (m.start() == 0)
					continue; // let's not find that start again ...
				if (pm != null)
					pm.setInfo(" ==> found plate sub-caption starting at " + m.group());
				ImWord subCaptionStartWord = captionText.wordAtOffset(m.start());
				if (subCaptionStartWord != null) {
					subCaptionStarts.addElement(m.group());
					subCaptionStartIDs.addElement(subCaptionStartWord.getLocalID());
				}
			}
			if (subCaptionStarts.size() != 0)
				break;
		}
	}
	
	private static final boolean DEBUG_IS_CAPTION = true;
	private static CaptionPatternMatch getCaptionPatternMatch(ImRegion paragraph, ImWord[] paragraphWords, CaptionStartPattern[] startPatterns, CaptionStartPattern[] defaultStartPatterns, int minFontSize, int maxFontSize, boolean startIsBold) {
		
		//	test bold property first thing
		if (startIsBold && !paragraphWords[0].hasAttribute(ImWord.BOLD_ATTRIBUTE)) {
			if (DEBUG_IS_CAPTION) System.out.println(" ==> not bold at " + paragraphWords[0].getString());
			return null;
		}
		
		//	run font-size matching on paragraph level (let's not get thrown off by some super scripts or sub scripts)
		if (((0 < minFontSize) || (maxFontSize < 72)) && !isFontSizeMatch(paragraphWords, minFontSize, maxFontSize, DEBUG_IS_CAPTION))
			return null;
		
		//	find end of first line
		ImWord firstLineEnd = getParagraphFirstLineEnd(paragraphWords);
		
		//	concatenate first line of paragraph
		String paragraphFirstLine = ImUtils.getString(paragraphWords[0], firstLineEnd, true);
		if (DEBUG_IS_CAPTION) System.out.println("Caption test first line is " + paragraphFirstLine);
		
		//	test first line against detector patterns
		CaptionStartPattern startPattern = null;
		String startPatternMatch = null;
		for (int p = 0; p < startPatterns.length; p++) {
			if (startPatterns[p].isPlateSubCaption())
				continue;
			Matcher startMatcher = startPatterns[p].matcher(paragraphFirstLine);
			if (startMatcher.find() && (startMatcher.start() == 0)) {
				startPattern = startPatterns[p];
				startPatternMatch = startMatcher.group();
				break;
			}
		}
		if (startPattern == null) {
			if (DEBUG_IS_CAPTION) System.out.println(" ==> no pattern match");
			return null;
		}
		
		//	count words and numbers in first line
		int wordCount = 0;
		int numberCountArabic = 0;
		int numberCountRomanSecure = 0;
		int numberCountRomanUpper = 0;
		int numberCountRomanLower = 0;
		int indexLetterCountUpper = 0;
		int indexLetterCountLower = 0;
		for (int w = 1; w < paragraphWords.length; w++) {
			String wordString = paragraphWords[w].getString();
			if ((wordString == null) || (wordString.length() == 0))
				continue;
			if (Gamta.isNumber(wordString)) {
				if (wordString.length() < 4)
					numberCountArabic++;
				else wordCount++;
			}
			else if (Gamta.isRomanNumber(wordString)) {
				if (wordString.equals(wordString.toLowerCase()))
					numberCountRomanLower++;
				else if (wordString.equals(wordString.toUpperCase()))
					numberCountRomanUpper++;
				if (wordString.length() > 1)
					numberCountRomanSecure++;
			}
			else if (Gamta.isWord(wordString)) {
				if (wordString.length() > 1)
					wordCount++;
				else if (wordString.matches("[a-z]"))
					indexLetterCountLower++;
				else if (wordString.matches("[A-Z]"))
					indexLetterCountUpper++;
			}
			if (paragraphWords[w] == firstLineEnd)
				break;
		}
		
		//	if we have Roman numbers, they are (presumably) consistent regarding case
		int numberCountRoman = Math.max(numberCountRomanUpper, numberCountRomanLower);
		wordCount += Math.min(numberCountRomanUpper, numberCountRomanLower);
		
		//	no numbers at all, with universal patterns ==> not a caption
		if ((startPatterns == defaultStartPatterns) && ((numberCountArabic + numberCountRoman) == 0)) {
			if (DEBUG_IS_CAPTION) System.out.println(" ==> no numbers at all, not a caption");
			return null;
		}
		
		//	index letters are either lower or upper case, but (presumably) never mixed
		int indexLetterCount = Math.max(indexLetterCountLower, indexLetterCountUpper);
		wordCount += Math.min(indexLetterCountLower, indexLetterCountUpper);
		
		//	if we have multiple Roman numbers, it's extremely unlikely they are all single-lettered
		if ((numberCountRoman > 1) && (numberCountRomanSecure == 0)) {
			indexLetterCount += (numberCountRoman - 1);
			numberCountRoman = 1;
		}
		
		//	we have at most two levels of numbering, the third is actually words
		int[] numberingCounts = {
			numberCountArabic,
			numberCountRoman,
			indexLetterCount
		};
		Arrays.sort(numberingCounts);
		wordCount += numberingCounts[0];
		int numberingCount = (numberingCounts[1] + numberingCounts[2]);
		
		//	more numbering than words ==> reference to caption (we can cut some more slack if we have other clues)
		boolean isCaptionReference;
		if (startIsBold || (startPatterns != defaultStartPatterns))
			isCaptionReference = ((wordCount * 3) < (numberingCount * 2));
		else isCaptionReference = (wordCount < numberingCount);
		if (isCaptionReference) {
			if (DEBUG_IS_CAPTION) System.out.println(" ==> " + wordCount + " words vs. " + numberingCount + " numbers, caption reference");
			return null;
		}
		
		//	assess caption type
		char type = startPattern.type;
		
		//	try and classify via universal patterns first (unless we used them above already)
		if ((type == CaptionStartPattern.GENERIC_TYPE) && (startPatterns != defaultStartPatterns)) {
			for (int p = 0; p < defaultStartPatterns.length; p++) {
				Matcher startMatcher = defaultStartPatterns[p].matcher(paragraphFirstLine);
				if (startMatcher.find() && (startMatcher.start() == 0)) {
					type = defaultStartPatterns[p].type;
					break;
				}
			}
		}
		
		//	use likely table caption start (covers but all major publication languages)
		if ((type == CaptionStartPattern.GENERIC_TYPE) && startPatternMatch.toLowerCase().startsWith("tab"))
			type = CaptionStartPattern.TABLE_TYPE;
		
		//	default to figure
		if (type == CaptionStartPattern.GENERIC_TYPE)
			type = CaptionStartPattern.FIGURE_TYPE;
		
		//	more words than numbering ==> actual caption
		if (DEBUG_IS_CAPTION) System.out.println(" ==> " + wordCount + " words vs. " + numberingCount + " numbers, caption");
		return new CaptionPatternMatch(paragraphFirstLine, startPattern, startPatternMatch, type);
	}
	
	private static boolean isFontSizeMatch(ImWord[] paragraphWords, int minFontSize, int maxFontSize, boolean debug) {
		
		//	collect word font sizes (matching as well as below-minimum), and count lack of font size as well
		int matchFontSizeWordCount = 0;
		int matchFontSizeWordCharCount = 0;
		int lowFontSizeWordCount = 0;
		int lowFontSizeWordCharCount = 0;
		int noFontSizeWordCount = 0;
		int noFontSizeWordCharCount = 0;
		for (int w = 0; w < paragraphWords.length; w++) try {
			int wfs = paragraphWords[w].getFontSize();
			
			//	no font size at all
			if (wfs == -1) {
				noFontSizeWordCount++;
				noFontSizeWordCharCount += paragraphWords[w].getString().length();
			}
			
			//	this one's good (with some upward tolerance for certain punctuation marks)
			else if (isFontSizeMatch(paragraphWords[w], wfs, minFontSize, maxFontSize, debug)) {
				matchFontSizeWordCount++;
				matchFontSizeWordCharCount += paragraphWords[w].getString().length();
			}
			
			//	this one's too large, we're not having that
			else if (maxFontSize < wfs) {
				if (debug) System.out.println(" ==> too large words (font size " + wfs + ")");
				return false;
			}
			
			//	this one's too small, allow this to a certain degree (super- and subscripts)
			else if (wfs < minFontSize) {
				lowFontSizeWordCount++;
				lowFontSizeWordCharCount += paragraphWords[w].getString().length();
			}
		}
		catch (NumberFormatException nfe) {
			noFontSizeWordCount++;
			noFontSizeWordCharCount += paragraphWords[w].getString().length();
		}
		
		//	declare mismatch if more than one third of words or characters have no font size at all
		//	TODO verify thresholds, might be too lenient
		if ((noFontSizeWordCount * 3) > matchFontSizeWordCount) {
			if (debug) System.out.println(" ==> too many words without font size (" + noFontSizeWordCount + " against " + matchFontSizeWordCount + " with)");
			return false;
		}
		if ((noFontSizeWordCharCount * 3) > matchFontSizeWordCharCount) {
			if (debug) System.out.println(" ==> too many characters without font size (" + noFontSizeWordCharCount + " against " + matchFontSizeWordCharCount + " with)");
			return false;
		}
		
		//	declare mismatch if more than one fifth of words or one tenth of characters have too small font size
		//	TODO verify thresholds, might be too lenient
		if ((lowFontSizeWordCount * 5) > matchFontSizeWordCount) {
			if (debug) System.out.println(" ==> too many words below minimum font size (" + lowFontSizeWordCount + " against " + matchFontSizeWordCount + " above)");
			return false;
		}
		if ((lowFontSizeWordCharCount * 10) > matchFontSizeWordCharCount) {
			if (debug) System.out.println(" ==> too many characters below minimum font size (" + lowFontSizeWordCharCount + " against " + matchFontSizeWordCharCount + " above)");
			return false;
		}
		
		/* TODO maybe enforce gap of at least 2 between average match font size and average low font size:
		 * - maybe use even higher minimum gap, or some factor between font sizes (maybe 2/3 ?)
		 * - rationale: super- and subscripts normally are a good bit smaller than main text
		 *   ==> can verify it's actually super- or subscripts
		 */
		
		//	no red flags on this one ...
		return true;
	}
//	
//	private static boolean isFontSizeMatch(ImWord word, int minFontSize, int maxFontSize, boolean debug) {
//		return isFontSizeMatch(word, Integer.parseInt((String) word.getAttribute(ImWord.FONT_SIZE_ATTRIBUTE, "-1")), minFontSize, maxFontSize, debug);
//	}
	
	private static boolean isFontSizeMatch(ImWord word, int wordFontSize, int minFontSize, int maxFontSize, boolean debug) {
		if (wordFontSize < 0)
			return true;
		int fontSizeTolerance = (((word.getString().length() > 1) || (fontSizeVariablePunctuationMarks.indexOf(word.getString()) == -1)) ? 0 : 1);
		if ((wordFontSize + fontSizeTolerance) < minFontSize) {
			boolean isPossibleSuperOrSubScript = false;
			if ((word.getString().length() == 1) && Character.isLetterOrDigit(word.getString().charAt(0)))
				isPossibleSuperOrSubScript = true; // index letter or digit
			else if (" st nd rd th ".indexOf(word.getString()) != -1)
				isPossibleSuperOrSubScript = true; // English ordinal number suffixes (all other Latin based languages use single letter ones or none at all)
			if (isPossibleSuperOrSubScript) {
				if (debug) System.out.println(" ==> font smaller than " + minFontSize + " at " + wordFontSize + " tolerated for " + word.getString() + " as potential super- or subscript");
			}
			else {
				if (debug) System.out.println(" ==> font smaller than " + minFontSize + " at " + wordFontSize + " for " + word.getString());
				return false;
			}
		}
		if (maxFontSize < (wordFontSize - fontSizeTolerance)) {
			if (debug) System.out.println(" ==> font larger than " + maxFontSize + " at " + wordFontSize + " for " + word.getString());
			return false;
		}
		return true;
	}
	
	private static int getAverageFontSize(ImRegion region) {
		return getMatchFontSize(region.getWords(), 0, 72);
	}
	
	private static int getAverageFontSize(ImWord[] words) {
		return getMatchFontSize(words, 0, 72);
	}
	
	private static int getMatchFontSize(ImWord[] words, int minFontSize, int maxFontSize) {
		int fontSizeSum = 0;
		int fontSizeWordCount = 0;
		for (int w = 0; w < words.length; w++) {
			int wfs = words[w].getFontSize();
			if (wfs < minFontSize)
				continue; // let's not dilute measurements with super- and subscripts
			if (maxFontSize < wfs)
				continue; // let's not dilute measurements with oversize words (how ever this one made it into the match at all)
			fontSizeSum += wfs;
			fontSizeWordCount++;
		}
		return ((fontSizeWordCount == 0) ? -1 : (fontSizeSum / fontSizeWordCount));
	}
	
	private static ImWord getParagraphFirstLineEnd(ImWord[] paragraphWords) {
		ImWord firstLineEnd = paragraphWords[0];
		for (int w = 0; w < paragraphWords.length; w++) {
			firstLineEnd = paragraphWords[w];
			if (firstLineEnd.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
				break;
			ImWord firstLineEndNext = firstLineEnd.getNextWord();
			if (firstLineEndNext == null)
				break;
			if (firstLineEndNext.bounds.right < firstLineEnd.bounds.left)
				break;
			if (firstLineEnd.bounds.bottom < firstLineEndNext.bounds.top)
				break;
			if (firstLineEndNext.bounds.bottom < firstLineEnd.bounds.top)
				break;
		}
		return firstLineEnd;
	}
	
	private ImRegion getAboveCaptionTarget(ImPage page, BoundingBox captionBounds, int dpi, boolean isTableCaption) {
		
		//	get regions
		ImRegion[] pageRegions = page.getRegions(isTableCaption ? ImRegion.TABLE_TYPE : null);
		
		//	seek suitable target regions
		ImRegion targetRegion = null;
		for (int r = 0; r < pageRegions.length; r++) {
			
			//	ignore lines and paragraphs
			if (ImRegion.LINE_ANNOTATION_TYPE.equals(pageRegions[r].getType()) || ImRegion.PARAGRAPH_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	ignore tables we're not out for
			if (isTableCaption != ImRegion.TABLE_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	this one's too far down (we need to cut this a little slack, as images can be masked to smaller sizes in born-digital PDFs)
//			if (caption.bounds.top < pageRegions[r].bounds.bottom)
//				continue;
			if (((captionBounds.top + captionBounds.bottom) / 2) < pageRegions[r].bounds.bottom)
				continue;
			
			//	check if size is sufficient
			if (((pageRegions[r].bounds.bottom - pageRegions[r].bounds.top) * 2) < dpi)
				continue;
			
			//	check alignment
			if (!ImUtils.isCaptionBelowTargetMatch(captionBounds, pageRegions[r].bounds, dpi))
				continue;
			
			//	check if candidate target contains words that cannot belong to a caption target area, and find lower edge
//			ImWord[] regionWords = page.getWordsInside(pageRegions[r].bounds);
//			boolean gotNonCaptionableWords = false;
//			for (int w = 0; w < regionWords.length; w++) {
//				if (ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(regionWords[w].getTextStreamType()) || ImWord.TEXT_STREAM_TYPE_DELETED.equals(regionWords[w].getTextStreamType()) || ImWord.TEXT_STREAM_TYPE_LABEL.equals(regionWords[w].getTextStreamType()))
//					continue;
//				if (isTableCaption && ImWord.TEXT_STREAM_TYPE_TABLE.equals(regionWords[w].getTextStreamType()))
//					continue;
//				gotNonCaptionableWords = true;
//				break;
//			}
//			if (gotNonCaptionableWords)
//				continue;
			if (this.containsNonCaptionableWords(pageRegions[r].bounds, page))
				continue;
			
			//	this one looks good
			if (isTableCaption) // return target table right away
				return pageRegions[r];
			else { // store target figure (might be part of actual figure, to be restored below)
				targetRegion = pageRegions[r];
				break;
			}
		}
		
		//	do we have a target region to start with?
		if (targetRegion == null)
			return null;
		
		//	try and restore images cut apart horizontally by page structure detection (vertical splits result in the parent region being retained, so we'll find the latter first)
		for (int r = 0; r < pageRegions.length; r++) {
			
			//	ignore lines and paragraphs
			if (ImRegion.LINE_ANNOTATION_TYPE.equals(pageRegions[r].getType()) || ImRegion.PARAGRAPH_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	ignore tables we're not out for
			if (ImRegion.TABLE_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	this one lies inside what we already have
			if (targetRegion.bounds.includes(pageRegions[r].bounds, false))
				continue;
			
			//	this one's too far down (we need to cut this a little slack, as images can be masked to smaller sizes in born-digital PDFs)
//			if (caption.bounds.top < pageRegions[r].bounds.bottom)
//				continue;
			if (((captionBounds.top + captionBounds.bottom) / 2) < pageRegions[r].bounds.bottom)
				continue;
			
			//	check if candidate aggregate target contains words that cannot belong to a caption target area, and find lower edge
			BoundingBox aggregateBounds = new BoundingBox(Math.min(targetRegion.bounds.left, pageRegions[r].bounds.left), Math.max(targetRegion.bounds.right, pageRegions[r].bounds.right), Math.min(targetRegion.bounds.top, pageRegions[r].bounds.top), Math.max(targetRegion.bounds.bottom, pageRegions[r].bounds.bottom));
//			ImWord[] aggregateWords = page.getWordsInside(aggregateBounds);
//			boolean gotNonCaptionableWords = false;
//			for (int w = 0; w < aggregateWords.length; w++) {
//				if (ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(aggregateWords[w].getTextStreamType()) || ImWord.TEXT_STREAM_TYPE_DELETED.equals(aggregateWords[w].getTextStreamType()) || ImWord.TEXT_STREAM_TYPE_LABEL.equals(aggregateWords[w].getTextStreamType()))
//					continue;
//				gotNonCaptionableWords = true;
//				break;
//			}
//			if (gotNonCaptionableWords)
//				continue;
			if (this.containsNonCaptionableWords(aggregateBounds, page))
				continue;
			
			//	include this one in the aggregate
			targetRegion = new ImRegion(targetRegion.getDocument(), targetRegion.pageId, aggregateBounds, ImRegion.REGION_ANNOTATION_TYPE);
		}
		
		//	return what we got
		return targetRegion;
	}
	
	private ImRegion getBelowCaptionTarget(ImPage page, BoundingBox captionBounds, int dpi, boolean isTableCaption) {
		
		//	get regions
		ImRegion[] pageRegions = page.getRegions(isTableCaption ? ImRegion.TABLE_TYPE : null);
		
		//	seek suitable target regions
		ImRegion targetRegion = null;
		for (int r = 0; r < pageRegions.length; r++) {
			
			//	ignore lines and paragraphs
			if (ImRegion.LINE_ANNOTATION_TYPE.equals(pageRegions[r].getType()) || ImRegion.PARAGRAPH_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	ignore tables we're not out for
			if (isTableCaption != ImRegion.TABLE_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	this one's too high up (we need to cut this a little slack, as images can be masked to smaller sizes in born-digital PDFs)
//			if (pageRegions[r].bounds.top < caption.bounds.bottom)
//				continue;
			if (pageRegions[r].bounds.top < ((captionBounds.top + captionBounds.bottom) / 2))
				continue;
			
			//	check if size is sufficient
			if (((pageRegions[r].bounds.bottom - pageRegions[r].bounds.top) * 2) < dpi)
				continue;
			
			//	check alignment
			if (!ImUtils.isCaptionAboveTargetMatch(captionBounds, pageRegions[r].bounds, dpi))
				continue;
			
			//	check if candidate target contains words that cannot belong to a caption target area, and find lower edge
//			ImWord[] pageRegionWords = page.getWordsInside(pageRegions[r].bounds);
//			boolean gotNonCaptionableWords = false;
//			for (int w = 0; w < pageRegionWords.length; w++) {
//				if (ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(pageRegionWords[w].getTextStreamType()) || ImWord.TEXT_STREAM_TYPE_DELETED.equals(pageRegionWords[w].getTextStreamType()) || ImWord.TEXT_STREAM_TYPE_LABEL.equals(pageRegionWords[w].getTextStreamType()))
//					continue;
//				if (isTableCaption && ImWord.TEXT_STREAM_TYPE_TABLE.equals(pageRegionWords[w].getTextStreamType()))
//					continue;
//				gotNonCaptionableWords = true;
//				break;
//			}
//			if (gotNonCaptionableWords)
//				continue;
			if (this.containsNonCaptionableWords(pageRegions[r].bounds, page))
				continue;
			
			//	this one looks good
			if (isTableCaption) // return target table right away
				return pageRegions[r];
			else { // store target figure (might be part of actual figure, to be restored below)
				targetRegion = pageRegions[r];
				break;
			}
		}
		
		//	do we have a target region to start with?
		if (targetRegion == null)
			return null;
		
		//	try and restore images cut apart horizontally by page structure detection (vertical splits result in the parent region being retained, so we'll find the latter first)
		for (int r = 0; r < pageRegions.length; r++) {
			
			//	ignore lines and paragraphs
			if (ImRegion.LINE_ANNOTATION_TYPE.equals(pageRegions[r].getType()) || ImRegion.PARAGRAPH_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	ignore tables we're not out for
			if (ImRegion.TABLE_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	this one lies inside what we already have
			if (targetRegion.bounds.includes(pageRegions[r].bounds, false))
				continue;
			
			//	this one's too high up (we need to cut this a little slack, as images can be masked to smaller sizes in born-digital PDFs)
//			if (pageRegions[r].bounds.top < caption.bounds.bottom)
//				continue;
			if (pageRegions[r].bounds.top < ((captionBounds.top + captionBounds.bottom) / 2))
				continue;
			
			//	check if candidate aggregate target contains words that cannot belong to a caption target area, and find lower edge
			BoundingBox aggregateBounds = new BoundingBox(Math.min(targetRegion.bounds.left, pageRegions[r].bounds.left), Math.max(targetRegion.bounds.right, pageRegions[r].bounds.right), Math.min(targetRegion.bounds.top, pageRegions[r].bounds.top), Math.max(targetRegion.bounds.bottom, pageRegions[r].bounds.bottom));
//			ImWord[] aggregateWords = page.getWordsInside(aggregateBounds);
//			boolean gotNonCaptionableWords = false;
//			for (int w = 0; w < aggregateWords.length; w++) {
//				if (ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(aggregateWords[w].getTextStreamType()) || ImWord.TEXT_STREAM_TYPE_DELETED.equals(aggregateWords[w].getTextStreamType()) || ImWord.TEXT_STREAM_TYPE_LABEL.equals(aggregateWords[w].getTextStreamType()))
//					continue;
//				gotNonCaptionableWords = true;
//				break;
//			}
//			if (gotNonCaptionableWords)
//				continue;
			if (this.containsNonCaptionableWords(aggregateBounds, page))
				continue;
			
			//	include this one in the aggregate
			targetRegion = new ImRegion(targetRegion.getDocument(), targetRegion.pageId, aggregateBounds, ImRegion.REGION_ANNOTATION_TYPE);
		}
		
		//	return what we got
		return targetRegion;
	}
	
	private ImRegion getBesideCaptionTarget(ImPage page, BoundingBox captionBounds, int dpi, boolean isTableCaption) {
		
		//	get regions
		ImRegion[] pageRegions = page.getRegions(isTableCaption ? ImRegion.TABLE_TYPE : null);
		
		//	compute vertical center of caption
		int captionCenterY = ((captionBounds.top + captionBounds.bottom) / 2);
		
		//	seek suitable target regions
		ImRegion targetRegion = null;
		for (int r = 0; r < pageRegions.length; r++) {
			
			//	ignore lines and paragraphs
			if (ImRegion.LINE_ANNOTATION_TYPE.equals(pageRegions[r].getType()) || ImRegion.PARAGRAPH_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	ignore tables we're not out for
			if (isTableCaption != ImRegion.TABLE_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	this one's too far down
			if (captionCenterY < pageRegions[r].bounds.top)
				continue;
			
			//	this one's too high up
			if (pageRegions[r].bounds.bottom < captionCenterY)
				continue;
			
			//	check if size is sufficient
			if (((pageRegions[r].bounds.bottom - pageRegions[r].bounds.top) * 2) < dpi)
				continue;
			
			//	check if candidate target contains words that cannot belong to a caption target area, and find lower edge
//			ImWord[] pageRegionWords = page.getWordsInside(pageRegions[r].bounds);
//			boolean gotNonCaptionableWords = false;
//			for (int w = 0; w < pageRegionWords.length; w++) {
//				if (ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(pageRegionWords[w].getTextStreamType()) || ImWord.TEXT_STREAM_TYPE_DELETED.equals(pageRegionWords[w].getTextStreamType()) || ImWord.TEXT_STREAM_TYPE_LABEL.equals(pageRegionWords[w].getTextStreamType()))
//					continue;
//				if (isTableCaption && ImWord.TEXT_STREAM_TYPE_TABLE.equals(pageRegionWords[w].getTextStreamType()))
//					continue;
//				gotNonCaptionableWords = true;
//				break;
//			}
//			if (gotNonCaptionableWords)
//				continue;
			if (this.containsNonCaptionableWords(pageRegions[r].bounds, page))
				continue;
			
			//	this one looks good
			if (isTableCaption) // return target table right away
				return pageRegions[r];
			else { // store target figure (might be part of actual figure, to be restored below)
				targetRegion = pageRegions[r];
				break;
			}
		}
		
		//	do we have a target region to start with?
		if (targetRegion == null)
			return null;
		
		//	try and restore images cut apart horizontally by page structure detection (vertical splits result in the parent region being retained, so we'll find the latter first)
		for (int r = 0; r < pageRegions.length; r++) {
			
			//	ignore lines and paragraphs
			if (ImRegion.LINE_ANNOTATION_TYPE.equals(pageRegions[r].getType()) || ImRegion.PARAGRAPH_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	ignore tables we're not out for
			if (ImRegion.TABLE_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	this one's too far down
			if (captionCenterY < pageRegions[r].bounds.top)
				continue;
			
			//	this one's too high up
			if (pageRegions[r].bounds.bottom < captionCenterY)
				continue;
			
			//	this one lies inside what we already have
			if (targetRegion.bounds.includes(pageRegions[r].bounds, false))
				continue;
			
			//	check if candidate aggregate target contains words that cannot belong to a caption target area, and find lower edge
			BoundingBox aggregateBounds = new BoundingBox(Math.min(targetRegion.bounds.left, pageRegions[r].bounds.left), Math.max(targetRegion.bounds.right, pageRegions[r].bounds.right), Math.min(targetRegion.bounds.top, pageRegions[r].bounds.top), Math.max(targetRegion.bounds.bottom, pageRegions[r].bounds.bottom));
//			ImWord[] aggregateWords = page.getWordsInside(aggregateBounds);
//			boolean gotNonCaptionableWords = false;
//			for (int w = 0; w < aggregateWords.length; w++) {
//				if (ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(aggregateWords[w].getTextStreamType()) || ImWord.TEXT_STREAM_TYPE_DELETED.equals(aggregateWords[w].getTextStreamType()) || ImWord.TEXT_STREAM_TYPE_LABEL.equals(aggregateWords[w].getTextStreamType()))
//					continue;
//				gotNonCaptionableWords = true;
//				break;
//			}
//			if (gotNonCaptionableWords)
//				continue;
			if (this.containsNonCaptionableWords(aggregateBounds, page))
				continue;
			
			//	include this one in the aggregate
			targetRegion = new ImRegion(targetRegion.getDocument(), targetRegion.pageId, aggregateBounds, ImRegion.REGION_ANNOTATION_TYPE);
		}
		
		//	return what we got
		return targetRegion;
	}
	
	private boolean containsNonCaptionableWords(BoundingBox bounds, ImPage page) {
		ImWord[] words = page.getWordsInside(bounds);
		for (int w = 0; w < words.length; w++) {
			if (!ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(words[w].getTextStreamType()) && !ImWord.TEXT_STREAM_TYPE_DELETED.equals(words[w].getTextStreamType()) && !ImWord.TEXT_STREAM_TYPE_LABEL.equals(words[w].getTextStreamType()))
				return true;
		}
		return false;
	}
	
	private static ImWord[] getLargestTextStreamWords(ImWord[] words) {
		
		//	nothing to filter here
		if (words.length < 2)
			return words;
		
		//	count text stream IDs
		CountingSet textStreamIDs = new CountingSet(new TreeMap());
		for (int w = 0; w < words.length; w++)
			textStreamIDs.add(words[w].getTextStreamId());
		
		//	nothing to sort out here
		if (textStreamIDs.elementCount() == 1)
			return words;
		
		//	find most frequent text stream ID
		String mostFrequentTextStreamId = "";
		for (Iterator tsidit = textStreamIDs.iterator(); tsidit.hasNext();) {
			String textStreamId = ((String) tsidit.next());
			if (textStreamIDs.getCount(textStreamId) > textStreamIDs.getCount(mostFrequentTextStreamId))
				mostFrequentTextStreamId = textStreamId;
		}
		
		//	extract words from largest text stream
		ImWord[] largestTextStreamWords = new ImWord[textStreamIDs.getCount(mostFrequentTextStreamId)];
		for (int w = 0, ltsw = 0; w < words.length; w++) {
			if (mostFrequentTextStreamId.equals(words[w].getTextStreamId()))
				largestTextStreamWords[ltsw++] = words[w];
		}
		
		//	finally ...
		return largestTextStreamWords;
	}
	
	private static final boolean DEBUG_IS_PAGE_TITLE = false;
	private boolean isPageTitle(ImRegion region, ImWord[] regionWords, int pageCount, CountingSet edgeWords, CountingSet allWords, HashSet pageNumberWordIDs) {
		
		//	do we have a page number?
		for (int w = 0; w < regionWords.length; w++)
			if (pageNumberWordIDs.contains(regionWords[w].getLocalID())) {
				if (DEBUG_IS_PAGE_TITLE) System.out.println(" ==> contains page number");
				return true;
			}
		
		//	check words
		TreeSet edgeWordStrings = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		for (int w = 0; w < regionWords.length; w++) {
			String regionWordString = regionWords[w].getString();
			if (Gamta.isWord(regionWordString) || Gamta.isNumber(regionWordString))
				edgeWordStrings.add(regionWordString);
		}
		if (DEBUG_IS_PAGE_TITLE) System.out.println(" - got " + edgeWordStrings.size() + " edge word strings");
		int edgeWordFrequencySum = 0;
		for (Iterator wit = edgeWordStrings.iterator(); wit.hasNext();) {
			String edgeWord = ((String) wit.next());
			int edgeWordFrequency = edgeWords.getCount(edgeWord);
			if (edgeWordFrequency > 1) // own occurrence is not enough
				edgeWordFrequencySum += edgeWords.getCount(edgeWord);
			if (DEBUG_IS_PAGE_TITLE) System.out.println("   - " + edgeWord + " occurs " + edgeWords.getCount(edgeWord) + " times on edges");
		}
		if ((edgeWordFrequencySum * 2) > (pageCount * edgeWordStrings.size())) {
			if (DEBUG_IS_PAGE_TITLE) System.out.println(" ==> contains many frequent edge words");
			return true;
		}
		
		//	check case as last resort
		int charCount = 0;
		int capCharCount = 0;
		for (int w = 0; w < regionWords.length; w++) {
			String wordStr = regionWords[w].getString();
			if (!Gamta.isWord(wordStr))
				continue;
			charCount += wordStr.length();
			for (int c = 0; c < wordStr.length(); c++) {
				if (wordStr.charAt(c) == Character.toUpperCase(wordStr.charAt(c)))
					capCharCount++;
			}
		}
		if ((capCharCount * 2) > charCount)  {
			if (DEBUG_IS_PAGE_TITLE) System.out.println(" ==> many capital letters, likely a page title");
			return true;
		}
		else {
			if (DEBUG_IS_PAGE_TITLE) System.out.println(" ==> few capital letters, likely a not page title");
			return false;
		}
	}
	
	private PageData getPageData(ImPage page, int pageImageDpi, BoundingBox[] headerAreas, ProgressMonitor pm) {
		PageData pageData = new PageData(page);
		
		//	get regions
		ImRegion[] regions = page.getRegions();
		pm.setInfo(" - got " + regions.length + " regions in page " + page.pageId);
		
		//	collect regions of interest
		for (int r = 0; r < regions.length; r++) {
			pm.setInfo(" - assessing region " + regions[r].getType() + "@" + regions[r].bounds.toString());
			
			//	lines are not of interest here, as we are out for standalone blocks
			if (LINE_ANNOTATION_TYPE.equals(regions[r].getType())) {
				pm.setInfo(" --> ignoring line");
				continue;
			}
			
			//	no header areas defined, have to use heuristics
			if (headerAreas == null) {
				
				//	this one's too large (higher than an inch, wider than three inches) to be a page header (would just waste too much space)
				if (((regions[r].bounds.bottom - regions[r].bounds.top) > pageImageDpi) && ((regions[r].bounds.right - regions[r].bounds.left) > (pageImageDpi * 3))) {
					pm.setInfo(" --> too large");
					continue;
				}
				
				//	get words
				ImWord[] regionWords = regions[r].getWords();
				if (regionWords.length == 0)
					continue;
				Arrays.sort(regionWords, ImUtils.textStreamOrder);
				
				//	too many words for a page header
				if (regionWords.length > 30) {
					pm.setInfo(" --> too many words (" + regionWords.length + ")");
					continue;
				}
				
				//	this one has been worked on before (we still need page headers for page number detection, though)
				if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(regionWords[0].getTextStreamType()) && !ImWord.TEXT_STREAM_TYPE_PAGE_TITLE.equals(regionWords[0].getTextStreamType()))
					continue;
				
				//	check if any regions above or below
				boolean regionsAbove = false;
				boolean regionsBelow = false;
				for (int cr = 0; cr < regions.length; cr++) {
					if (cr == r)
						continue;
					if (LINE_ANNOTATION_TYPE.equals(regions[cr].getType()))
						continue;
					
					//	test if regions are really above/below one another
					if ((regions[r].bounds.right <= regions[cr].bounds.left) || (regions[cr].bounds.right <= regions[r].bounds.left))
						continue;
					
					//	test relative position
					if (regions[r].bounds.bottom <= regions[cr].bounds.top)
						regionsBelow = true;
					if (regions[cr].bounds.bottom <= regions[r].bounds.top)
						regionsAbove = true;
					
					//	do we need to check any further?
					if (regionsAbove && regionsBelow)
						break;
				}
				
				//	remember region of interest
				if (!regionsAbove && (regions[r].bounds.bottom < (page.bounds.bottom / 2))) {
					pageData.topRegions.add(regions[r]);
					pm.setInfo(" --> found top region");
				}
				if (!regionsBelow && (regions[r].bounds.top > (page.bounds.bottom / 2))) {
					pageData.bottomRegions.add(regions[r]);
					pm.setInfo(" --> found bottom region");
				}
			}
			
			//	use header areas
			else for (int h = 0; h < headerAreas.length; h++) {
				if (!headerAreas[h].includes(regions[r].bounds, false))
					continue;
				
				//	use only top region list if we know header areas
				pageData.topRegions.add(regions[r]);
				pm.setInfo(" --> found header region");
				
				//	we're done with this region, make sure not to add it twice
				break;
			}
		}
		
		//	finally ...
		return pageData;
	}
	
	private void addPageNumberCandidates(List pageNumberCandidates, ImWord pageNumberStartPart, HashSet pageNumberPartIDs, int dpi, ProgressMonitor pm) {
		String pageNumberString = pageNumberStartPart.getString();
		this.addPageNumberCandidates(pageNumberCandidates, pageNumberStartPart, pageNumberStartPart, pageNumberString, pm);
		for (ImWord imw = pageNumberStartPart.getNextWord(); imw != null; imw = imw.getNextWord()) {
			if (imw.pageId != pageNumberStartPart.pageId)
				break; // off page
			if ((imw.centerY < pageNumberStartPart.bounds.top) || (pageNumberStartPart.bounds.bottom < imw.centerY))
				break; // out off line
			if (imw.bounds.left < pageNumberStartPart.bounds.right)
				break; // out of left-to-right order
			if ((dpi / 13) < (imw.bounds.left - pageNumberStartPart.bounds.right))
				break; // too far away (more than 2 mm)
			if (!pageNumberPartIDs.contains(imw.getLocalID()))
				break;
			pageNumberString += imw.getString();
			boolean newPageNumbers = this.addPageNumberCandidates(pageNumberCandidates, pageNumberStartPart, imw, pageNumberString, pm);
			if (!newPageNumbers)
				break; // no matches found
		}
	}
	
	private boolean addPageNumberCandidates(List pageNumberCandidates, ImWord firstWord, ImWord lastWord, String pageNumberString, ProgressMonitor pm) {
		
		//	page numbers with six or more digits are rather improbable ...
		if (pageNumberString.length() > 5)
			return false;
		
		//	extract possible interpretations, computing ambiguity along the way
		int [][] pageNumberDigits = new int[pageNumberString.length()][];
		int ambiguity = 1;
		for (int c = 0; c < pageNumberString.length(); c++) {
			String pageNumberCharacter = pageNumberString.substring(c, (c+1));
			pageNumberDigits[c] = ((int[]) this.ocrPageNumberCharacterTranslations.get(pageNumberCharacter));
			if (pageNumberDigits[c] == null)
				return false; // invalid character
			ambiguity *= pageNumberDigits[c].length;
		}
		
		//	the first digit is never zero ...
		if ((pageNumberDigits[0].length == 1) && (pageNumberDigits[0][0] == 0))
			return false;
		
		//	create all candidate values
		TreeSet values = this.getAllPossibleValues(pageNumberDigits);
		if (values.size() == 0)
			return false;
		pm.setInfo("   - got " + values.size() + " possible values");
		
		//	compute fuzzyness for each value
		for (Iterator vit = values.iterator(); vit.hasNext();) try {
			String pageNumberValue = ((String) vit.next());
			pm.setInfo("     - " + pageNumberValue);
			int fuzzyness = 0;
			for (int d = 0; d < pageNumberValue.length(); d++) {
				if (pageNumberValue.charAt(d) != pageNumberString.charAt(d))
					fuzzyness++;
			}
			pageNumberCandidates.add(new PageNumber(pageNumberString, Integer.parseInt(pageNumberValue), fuzzyness, ambiguity, firstWord, lastWord));
		} catch (NumberFormatException nfe) {}
		
		//	did we add anything?
		return true;
	}
	
	private TreeSet getAllPossibleValues(int[][] baseDigits) {
		TreeSet allPossibleValues = new TreeSet();
		int[] digits = new int[baseDigits.length];
		addAllPossibleValues(baseDigits, digits, 0, allPossibleValues);
		return allPossibleValues;
	}
	
	private void addAllPossibleValues(int[][] baseDigits, int[] digits, int pos, TreeSet values) {
		if (pos == digits.length)
			values.add(this.getValueString(digits));
		else for (int d = 0; d < baseDigits[pos].length; d++) {
			digits[pos] = baseDigits[pos][d];
			addAllPossibleValues(baseDigits, digits, (pos+1), values);
		}
	}
	
	private String getValueString(int[] digits) {
		StringBuffer valueString = new StringBuffer();
		for (int d = 0; d < digits.length; d++)
			valueString.append("" + digits[d]);
		return valueString.toString();
	}
	
	private void scoreAndSelectPageNumbers(PageData[] pageData, ProgressMonitor pm) {
		
		//	work page by page
		for (int p = 0; p < pageData.length; p++) {
			
			//	get existing page number
			int pageNumber = -1;
			if (pageData[p].page.hasAttribute(PAGE_NUMBER_ATTRIBUTE)) try {
				pageNumber = Integer.parseInt((String) pageData[p].page.getAttribute(PAGE_NUMBER_ATTRIBUTE));
			} catch (NumberFormatException nfe) {}
			
			//	score page numbers
			for (int n = 0; n < pageData[p].pageNumberCandidates.size(); n++) {
				PageNumber pn = ((PageNumber) pageData[p].pageNumberCandidates.get(n));
				
				//	reward consistency with pre-existing page number
				if (pageNumber != -1)
					pn.score += pageData.length;
				
				//	look forward
				int fMisses = 0;
				for (int l = 1; (p+l) < pageData.length; l++) {
					PageNumber cpn = null;
					for (int c = 0; c < pageData[p+l].pageNumberCandidates.size(); c++) {
						cpn = ((PageNumber) pageData[p+l].pageNumberCandidates.get(c));
						if (cpn.isConsistentWith(pn)) {
							pn.score += (((float) l) / (1 + cpn.fuzzyness));
							break;
						}
						else cpn = null;
					}
					if (cpn == null) {
						fMisses++;
						if (fMisses == 3)
							break;
					}
				}
				
				//	look backward
				int bMisses = 0;
				for (int l = 1; l <= p; l++) {
					PageNumber cpn = null;
					for (int c = 0; c < pageData[p-l].pageNumberCandidates.size(); c++) {
						cpn = ((PageNumber) pageData[p-l].pageNumberCandidates.get(c));
						if (cpn.isConsistentWith(pn)) {
							pn.score += (((float) l) / (1 + cpn.fuzzyness));
							break;
						}
						else cpn = null;
					}
					if (cpn == null) {
						bMisses++;
						if (bMisses == 3)
							break;
					}
				}
				
				//	penalize fuzzyness
				pn.score /= (pn.fuzzyness+1);
				pm.setInfo("   - " + pn.valueStr + " (as " + pn.value + ", fuzzyness " + pn.fuzzyness + ", ambiguity " + pn.ambiguity + ") on page " + p + " ==> " + pn.score);
			}
			
			//	select page number
			for (int n = 0; n < pageData[p].pageNumberCandidates.size(); n++) {
				PageNumber pn = ((PageNumber) pageData[p].pageNumberCandidates.get(n));
				if (pn.score < 1)
					continue;
				if ((pageData[p].pageNumber == null) || (pageData[p].pageNumber.score < pn.score))
					pageData[p].pageNumber = pn;
			}
			if (pageData[p].pageNumber == null)
				pm.setInfo(" --> could not determine page number of page " + p + ".");
			else pm.setInfo(" --> page number of " + p + " identified as " + pageData[p].pageNumber.value + " (score " + pageData[p].pageNumber.score + ")");
		}
	}
	
	private void checkPageNumberSequence(PageData[] pageData, ProgressMonitor pm) {
		for (int p = 0; p < pageData.length; p++) {
			if (pageData[p].pageNumber == null)
				continue;
			
			//	get adjacent page numbers
			PageNumber bpn = ((p == 0) ? null : pageData[p-1].pageNumber);
			PageNumber fpn = (((p+1) == pageData.length) ? null : pageData[p+1].pageNumber);
			if ((bpn == null) && (fpn == null))
				continue;
			
			//	check sequence consistent with what we can check against
			if (((bpn == null) || bpn.isConsistentWith(pageData[p].pageNumber)) && ((fpn == null) || fpn.isConsistentWith(pageData[p].pageNumber)))
				continue;
			
			//	correct that one oddjob in the middle
			if ((bpn != null) && (fpn != null) && bpn.isConsistentWith(fpn)) {
				pm.setInfo("   - eliminated page number " + pageData[p].pageNumber.value + " in page " + p + " for sequence inconsistency.");
				pageData[p].pageNumber = null;
				for (int n = 0; n < pageData[p].pageNumberCandidates.size(); n++) {
					PageNumber pn = ((PageNumber) pageData[p].pageNumberCandidates.get(n));
					if (pn.isConsistentWith(bpn) && pn.isConsistentWith(fpn)) {
						pageData[p].pageNumber = pn;
						pm.setInfo("   --> re-assigned to " + pageData[p].pageNumber.value);
						break;
					}
				}
				continue;
			}
			
			//	one side not set, other inconsistent and far more secure
			if ((bpn == null) && !fpn.isConsistentWith(pageData[p].pageNumber) && ((pageData[p].pageNumber.score * 2) < fpn.score)) {
				pm.setInfo("   - eliminated page number " + pageData[p].pageNumber.value + " in page " + p + " for sequence front edge inconsistency.");
				pageData[p].pageNumber = null;
				continue;
			}
			else if ((fpn == null) && !bpn.isConsistentWith(pageData[p].pageNumber) && ((pageData[p].pageNumber.score * 2) < bpn.score)) {
				pm.setInfo("   - eliminated page number " + pageData[p].pageNumber.value + " in page " + p + " for sequence back edge inconsistency.");
				pageData[p].pageNumber = null;
				continue;
			}
		}
	}
	
	private void fillInMissingPageNumbers(PageData[] pageData, ProgressMonitor pm) {
		
		//	make sure not to extrapolate to page numbers that are already taken
		HashSet existingPageNumbers = new HashSet();
		for (int p = 0; p < pageData.length; p++) {
			if (pageData[p].pageNumber != null)
				existingPageNumbers.add("" + pageData[p].pageNumber.value);
		}
		
		//	do backward sequence extrapolation
		PageNumber bPageNumber = null;
		for (int p = (pageData.length - 2); p >= 0; p--) {
			if (pageData[p].pageNumber != null) {
				bPageNumber = pageData[p].pageNumber;
				continue;
			}
			if (bPageNumber == null)
				continue;
			PageNumber ePageNumber = new PageNumber(p, (bPageNumber.value - bPageNumber.pageId + p));
			if (existingPageNumbers.add("" + ePageNumber.value)) {
				pageData[p].pageNumber = ePageNumber;
				pageData[p].page.setAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + ePageNumber.value));
				bPageNumber = ePageNumber;
				pm.setInfo(" --> backward-extrapolated page number of page " + p + " to " + pageData[p].pageNumber.value);
			}
			else pm.setInfo(" --> could not backward-extrapolated page number of page " + p + ", page number " + ePageNumber.value + " already assigned");
		}
		
		//	do forward sequence extrapolation
		PageNumber fPageNumber = null;
		for (int p = 1; p < pageData.length; p++) {
			if (pageData[p].pageNumber != null) {
				fPageNumber = pageData[p].pageNumber;
				continue;
			}
			if (fPageNumber == null)
				continue;
			PageNumber ePageNumber = new PageNumber(p, (bPageNumber.value - bPageNumber.pageId + p));
			if (existingPageNumbers.add("" + ePageNumber.value)) {
				pageData[p].pageNumber = ePageNumber;
				pageData[p].page.setAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + ePageNumber.value));
				fPageNumber = ePageNumber;
				pm.setInfo(" --> forward-extrapolated page number of page " + p + " to " + pageData[p].pageNumber.value);
			}
			else pm.setInfo(" --> could not forward-extrapolated page number of page " + p + ", page number " + ePageNumber.value + " already assigned");
		}
	}
	
	private void annotatePageNumbers(PageData pageData, HashSet pageNumberWordIDs) {
		if (pageData.pageNumber == null)
			return;
		pageData.page.setAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + pageData.pageNumber.value));
		if (pageData.pageNumber.firstWord == null)
			return;
		ImAnnotation pageNumber = pageData.page.getDocument().addAnnotation(pageData.pageNumber.firstWord, pageData.pageNumber.lastWord, PAGE_NUMBER_TYPE);
		pageNumber.setAttribute("value", ("" + pageData.pageNumber.value));
		pageNumber.setAttribute("score", ("" + pageData.pageNumber.score));
		pageNumber.setAttribute("ambiguity", ("" + pageData.pageNumber.ambiguity));
		pageNumber.setAttribute("fuzzyness", ("" + pageData.pageNumber.fuzzyness));
		for (ImWord imw = pageData.pageNumber.firstWord; imw != null; imw = imw.getNextWord()) {
			pageNumberWordIDs.add(imw.getLocalID());
			if (imw == pageData.pageNumber.lastWord)
				break;
		}
	}
	
	private static class PageData {
		final ImPage page;
		
		List topRegions = new ArrayList();
		List bottomRegions = new ArrayList();
		
		List pageNumberCandidates = new ArrayList(1);
		PageNumber pageNumber = null;
		
		PageData(ImPage page) {
			this.page = page;
		}
	}
	
	private class PageNumber {
		final int pageId;
		final String valueStr;
		final int value; // the integer value
		final int fuzzyness; // how many characters do not match our integer value
		final int ambiguity; // how many possible values are there for our word (sequence), including this one
		final ImWord firstWord;
		final ImWord lastWord;
		float score = 0;
		
		//	used for born-digital documents, where we have no need for interpretation
		PageNumber(ImWord word) {
			this.pageId = word.pageId;
			this.valueStr = word.getString();
			this.value = Integer.parseInt(word.getString());
			this.fuzzyness = 0;
			this.ambiguity = 1;
			this.firstWord = word;
			this.lastWord = word;
		}
		
		//	used for scanned documents
		PageNumber(String valueStr, int value, int fuzzyness, int ambiguity, ImWord firstWord, ImWord lastWord) {
			this.pageId = firstWord.pageId;
			this.valueStr = valueStr;
			this.value = value;
			this.fuzzyness = fuzzyness;
			this.ambiguity = ambiguity;
			this.firstWord = firstWord;
			this.lastWord = lastWord;
		}
		
		//	used for extrapolation
		PageNumber(int pageId, int value) {
			this.pageId = pageId;
			this.valueStr = null;
			this.value = value;
			this.fuzzyness = Integer.MAX_VALUE;
			this.ambiguity = Integer.MAX_VALUE;
			this.firstWord = null;
			this.lastWord = null;
		}
		
		public boolean equals(Object obj) {
			return (((PageNumber) obj).value == this.value);
		}
		public int hashCode() {
			return this.value;
		}
		public String toString() {
			return ("" + this.value);
		}
		boolean isConsistentWith(PageNumber pn) {
			return ((this.value - pn.value) == (this.pageId - pn.pageId));
		}
//		float getAdjustedScore() {
//			return (((this.fuzzyness == Integer.MAX_VALUE) || (this.ambiguity == Integer.MAX_VALUE)) ? 0 : (this.score / (this.fuzzyness + this.ambiguity)));
//		}
	}
	
	private static final Comparator pageNumberValueOrder = new Comparator() {
		public int compare(Object obj1, Object obj2) {
			PageNumber pn1 = ((PageNumber) obj1);
			PageNumber pn2 = ((PageNumber) obj2);
			return ((pn1.value == pn2.value) ? (pn1.fuzzyness - pn2.fuzzyness) : (pn1.value - pn2.value));
		}
	};
	
	/* TODO
Create HeadingStructureEditor
- show all headings, in text order, in list
- display hierarchy by means of indents (simply use spaces) ...
- ... using drop-down for heading level
--> index headings by style, and update level for all headings in same style together ...
     ... by default, but offer checkbox disabling that behavior ...
     ... and split affected indexed heading styles on update if checkbox is unchecked
- offer heading removal option
- use non-modal dialog  to allow for marking headings missed by auto-detection ...
- ... and document listener to keep in sync with user edits

- UPDATE: do NOT index headings by style ...
- ... but by level (which is based on style originally) ...
- ... and transfer headings between levels by their original group if group transfer is enabled
	 */
	
	//	TODO factor out heading detection and editing to separate plugin, and make this one dependent on the latter
	
	//	TODO back up headings with rule-based hierarchy classification
	
	public static void main(String[] args) throws Exception {
		DocumentStructureDetectorProvider dsdp = new DocumentStructureDetectorProvider();
		dsdp.setDataProvider(new PluginDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/DocumentStructureDetectorData/")));
		dsdp.init();
		dsdp.debug = true;
		
		final String testFileName;
//		testFileName = "zt00872.pdf.imf";
//		testFileName = "zt00904.pdf.test.imf";
//		testFileName = "arac-38-02-328.pdf.imf";
//		testFileName = "zt03652p155.pdf.imf";
//		testFileName = "zt03456p035.pdf.test.imf";
//		testFileName = "6860_Cutler_1990_Bul_8_105-108.imf";
//		testFileName = "dikow_2012.test.pdf.imf";
//		testFileName = "ZJLS_Hertach2015.pdf.raw.imf"; // heading detection and ranking hampered by sloppy layout, pretty rough case this one
//		testFileName = "1_5_Farkac.pdf.imf"; // heading detection and ranking hampered by sloppy layout, pretty rough case this one
//		testFileName = "zt03911p493.pdf.raw.imf"; // key entries detected as footnotes on page 4 ==> fixed via font size
//		testFileName = "ZM1967042005.pdf.imf"; // two footnotes not detected on page 0
//		testFileName = "IJSEM/19.full.pdf.raw.imf"; // caption on page 2 mistaken for table
		
//		testFileName = "RSZ_2015_122_181-184_RihaFarkac.pdf" // TODO caption block on page 1: 10 single-line captions in one block, referring to single image with 10 numbered parts ==> needs to be single caption, treated like "Figures 1-10".
		
//		testFileName = "29882.pdf"; // TODO figures come in horizontal stripes, bundle images
		
//		testFileName = "zt03881p227.pdf.imf"; // fails to detect full-page table on page 5
//		testFileName = "zt03881p227.pdf.imdir"; // TODO fails to detect full-page table on page 5
//		testFileName = "zt03881p227.pdf.imf"; // cuts off column headers on page 8 due to too narrow column gap
		/* TODO revisit table extraction, try this:
		 * - cut blocks into individual lines altogether
		 * - for each line, collect
		 *   - above-average gaps as possible column gaps
		 *   - column anchors: left edge, center, and right edge of word blocks separated by these gaps
		 * - sort lines top-down
		 * - assemble tables from lines, scoring by compatibility of column gap and column anchor points
		 * ==> exploits alignment of column content
		 * ==> more versatile than mere column gap approach
		 * ==> can handle column gap constrictions due to wide values
		 */
		testFileName = "Zootaxa/zt02241p032.pdf.raw.imf"; // TODO finally found bogus table document: zt02241p032.pdf (page 5) ==> test this sucker right to the solution
		
		ImDocument doc = ImDocumentIO.loadDocument(new File("E:/Testdaten/PdfExtract/" + testFileName));
		
		DocumentStyle.addProvider(new DocumentStyle.Provider() {
			public Properties getStyleFor(Attributed doc) {
				Settings ds = Settings.loadSettings(new File("E:/GoldenGATEv3/Plugins/DocumentStyleProviderData/zootaxa.2007.journal_article.docStyle"));
//				Settings ds = Settings.loadSettings(new File("E:/GoldenGATEv3/Plugins/DocumentStyleProviderData/ijsem.0000.journal_article.docStyle"));
				return ds.toProperties();
			}
		});
		
//		dsdp.detectTablesSimple = false;
		dsdp.detectDocumentStructure(doc, true, ProgressMonitor.dummy);
//		
//		String[] params = DocumentStyle.getParameterNames();
//		Arrays.sort(params);
//		for (int p = 0; p < params.length; p++)
//			System.out.println(params[p] + " = \"" + DocumentStyle.getParameterValueClass(params[p]).getName() + "\";");
	}
//	
//	private void detectTablesTest(ImPage page, int pageImageDpi, int minColMargin, int minRowMargin, ProgressMonitor pm) {
//		ImRegion[] pageBlocks = page.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
//		Arrays.sort(pageBlocks, ImUtils.topDownOrder);
//		
//		//	split blocks with varying line distances (might be tables with multi-line rows) at larger line gaps, and let merging do its magic
//		ArrayList pageBlockList = new ArrayList();
//		HashMap pageBlocksBySubBlocks = new HashMap();
//		for (int b = 0; b < pageBlocks.length; b++) {
//			if (DEBUG_IS_TABLE) System.out.println("Testing irregular line gap split in block " + pageBlocks[b].bounds + " on page " + page.pageId + " for table detection");
//			
//			//	get block lines
//			ImRegion[] blockLines = pageBlocks[b].getRegions(ImRegion.LINE_ANNOTATION_TYPE);
//			if (blockLines.length < 5) {
//				pageBlockList.add(pageBlocks[b]);
//				if (DEBUG_IS_TABLE) System.out.println(" ==> too few lines to assess gaps");
//				continue;
//			}
//			Arrays.sort(blockLines, ImUtils.topDownOrder);
//			if (DEBUG_IS_TABLE) System.out.println(" --> got " + blockLines.length + " lines");
//			
//			//	compute line gaps
//			int minLineGap = (pageBlocks[b].bounds.bottom - pageBlocks[b].bounds.top);
//			int maxLineGap = 0;
//			int lineGapCount = 0;
//			int lineGapSum = 0;
//			for (int l = 1; l < blockLines.length; l++) {
//				int lineGap = (blockLines[l].bounds.top - blockLines[l-1].bounds.bottom);
//				if (lineGap < 0)
//					continue;
//				minLineGap = Math.min(minLineGap, lineGap);
//				maxLineGap = Math.max(maxLineGap, lineGap);
//				lineGapSum += lineGap;
//				lineGapCount++;
//			}
//			if (lineGapCount < 4) {
//				pageBlockList.add(pageBlocks[b]);
//				if (DEBUG_IS_TABLE) System.out.println(" ==> too few non-overlapping lines to assess gaps");
//				continue;
//			}
//			int avgLineGap = ((lineGapSum + (lineGapCount / 2)) / lineGapCount);
//			if (DEBUG_IS_TABLE) System.out.println(" --> measured " + lineGapCount + " line gaps, min is " + minLineGap + ", max is " + maxLineGap + ", avg is " + avgLineGap);
//			
//			//	line gaps too regular to use, split at all gaps if large enough, or not at all
//			if ((minLineGap * 4) > (maxLineGap * 3)) {
//				if (minLineGap > (page.getImageDPI() / 13)) {
//					avgLineGap = (minLineGap -1);
//					if (DEBUG_IS_TABLE) System.out.println(" --> line gaps too regular, splitting at all gaps using minimum " + avgLineGap);
//				}
//				else {
//					pageBlockList.add(pageBlocks[b]);
//					if (DEBUG_IS_TABLE) System.out.println(" ==> line gaps too regular");
//					continue;
//				}
//			}
//			
//			//	count above-average line gaps
//			int aboveAvgLineGapCount = 0;
//			for (int l = 1; l < blockLines.length; l++) {
//				int lineGap = (blockLines[l].bounds.top - blockLines[l-1].bounds.bottom);
//				if (lineGap > avgLineGap)
//					aboveAvgLineGapCount++;
//			}
//			
//			//	too few above-average line gaps
//			if ((aboveAvgLineGapCount * 5) < blockLines.length) {
//				pageBlockList.add(pageBlocks[b]);
//				if (DEBUG_IS_TABLE) System.out.println(" ==> too few above-average line gaps");
//				continue;
//			}
//			
//			//	create sub blocks at above-average line gaps
//			int subBlockStartLine = 0;
//			for (int l = 1; l < blockLines.length; l++) {
//				int lineGap = (blockLines[l].bounds.top - blockLines[l-1].bounds.bottom);
//				if (lineGap < avgLineGap)
//					continue;
//				ImRegion subBlock = new ImRegion(pageBlocks[b].getDocument(), pageBlocks[b].pageId, new BoundingBox(pageBlocks[b].bounds.left, pageBlocks[b].bounds.right, blockLines[subBlockStartLine].bounds.top, blockLines[l-1].bounds.bottom), ImRegion.BLOCK_ANNOTATION_TYPE);
//				pageBlockList.add(subBlock);
//				pageBlocksBySubBlocks.put(subBlock, pageBlocks[b]);
//				if (DEBUG_IS_TABLE) System.out.println(" --> got sub block at " + subBlock.bounds + " with " + (l - subBlockStartLine) + " lines");
//				subBlockStartLine = l;
//			}
//			if (subBlockStartLine != 0) {
//				ImRegion subBlock = new ImRegion(pageBlocks[b].getDocument(), pageBlocks[b].pageId, new BoundingBox(pageBlocks[b].bounds.left, pageBlocks[b].bounds.right, blockLines[subBlockStartLine].bounds.top, pageBlocks[b].bounds.bottom), ImRegion.BLOCK_ANNOTATION_TYPE);
//				pageBlockList.add(subBlock);
//				pageBlocksBySubBlocks.put(subBlock, pageBlocks[b]);
//				if (DEBUG_IS_TABLE) System.out.println(" --> got sub block at " + subBlock.bounds + " with " + (blockLines.length - subBlockStartLine) + " lines");
//			}
//			
//			//	increase minimum row margin to average line gap TODO_ne assess if this doesn't wreck havoc in some situations ==> seems OK with line gap width distribution catch
//			minRowMargin = Math.max(minRowMargin, avgLineGap);
//		}
//		
//		//	update page blocks if sub blocks added (do NOT re-sort, so sub blocks of same original block stay together)
//		if (pageBlocks.length < pageBlockList.size())
//			pageBlocks = ((ImRegion[]) pageBlockList.toArray(new ImRegion[pageBlockList.size()]));
//		
//		/* TODOne revisit table extraction, try this:
//		 * - for each line, collect
//		 *   - above-average gaps as possible column gaps
//		 *   - column anchors: left edge, center, and right edge of word blocks separated by these gaps
//		 * - sort lines top-down
//		 * - assemble tables from lines, scoring by compatibility of column gap and column anchor points
//		 * ==> exploits alignment of column content
//		 * ==> more versatile than mere column gap approach
//		 * ==> can handle column gap constrictions due to wide values
//		 */
//		
//		//	collect words and columns for each block, and measure width
//		boolean[] isPageBlockNarrow = new boolean[pageBlocks.length];
//		ImWord[][] pageBlockWords = new ImWord[pageBlocks.length][];
//		ImRegion[][] pageBlockCols = new ImRegion[pageBlocks.length][];
//		ImRegion[][] pageBlockRows = new ImRegion[pageBlocks.length][];
//		TableColGap[][] pageBlockColGaps = new TableColGap[pageBlocks.length][];
//		TableColAnchor[][] pageBlockColAnchors = new TableColAnchor[pageBlocks.length][];
//		for (int b = 0; b < pageBlocks.length; b++) {
//			isPageBlockNarrow[b] = (((pageBlocks[b].bounds.right - pageBlocks[b].bounds.left) * 5) < (page.bounds.right - page.bounds.left));
//			pageBlockWords[b] = page.getWordsInside(pageBlocks[b].bounds);
//			System.out.println("Testing block " + pageBlocks[b].bounds + " on page " + page.pageId + " for table or table part");
//			if (pageBlockWords[b].length == 0) {
//				System.out.println(" ==> no words");
//				continue;
//			}
//			Arrays.sort(pageBlockWords[b], ImUtils.textStreamOrder);
//			if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(pageBlockWords[b][0].getTextStreamType()))
//				continue;
//			pm.setInfo("Testing block " + pageBlocks[b].bounds + " on page " + page.pageId + " for table or table part");
//			pageBlockCols[b] = this.getTableColumns(pageBlocks[b], minColMargin);
//			if (pageBlockCols[b] != null)
//				pm.setInfo(" ==> columns OK");
//			pageBlockRows[b] = this.getTableRows(pageBlocks[b], minRowMargin);
//			
//			//	assess row occupancy, and compute average word height
//			int[] colWordRows = new int[pageBlocks[b].bounds.right - pageBlocks[b].bounds.left];
//			int wordHeightSum = 0;
//			Arrays.fill(colWordRows, 0);
//			for (int w = 0; w < pageBlockWords[b].length; w++) {
//				for (int c = Math.max(pageBlocks[b].bounds.left, pageBlockWords[b][w].bounds.left); c < Math.min(pageBlocks[b].bounds.right, pageBlockWords[b][w].bounds.right); c++)
//					colWordRows[c - pageBlocks[b].bounds.left] += (pageBlockWords[b][w].bounds.bottom - pageBlockWords[b][w].bounds.top);
//				wordHeightSum += (pageBlockWords[b][w].bounds.bottom - pageBlockWords[b][w].bounds.top);
//			}
//			int avgWordHeight = ((wordHeightSum + (pageBlockWords[b].length / 2)) / pageBlockWords[b].length);
//			
//			//	fill in gaps that are too small (considerably simplifies gap and anchor collection logic)
//			int start = 0;
//			for (int c = 1; c <= colWordRows.length; c++) {
//				if ((c != colWordRows.length) && ((colWordRows[c] == 0) == (colWordRows[c-1] == 0)))
//					continue;
//				if ((colWordRows[c-1] == 0) && ((c - start) < (avgWordHeight / 2))) {
//					for (int f = start; f < c; f++)
//						colWordRows[f] = 1;
//				}
//				start = c;
//			}
//			
//			//	collect gaps and column anchors
//			ArrayList colGaps = new ArrayList();
//			ArrayList colAnchors = new ArrayList();
//			for (int c = 0; c <= colWordRows.length; c++) {
//				if ((c != 0) && (c != colWordRows.length) && ((colWordRows[c] == 0) == (colWordRows[c-1] == 0)))
//					continue;
//				if (c != 0) {
//					if (colWordRows[c-1] == 0)
//						colGaps.add(new TableColGap((start + pageBlocks[b].bounds.left), (c + pageBlocks[b].bounds.left)));
//					else {
//						colAnchors.add(new TableColAnchor((((c + start) / 2) + pageBlocks[b].bounds.left), 'C'));
//						colAnchors.add(new TableColAnchor((c + pageBlocks[b].bounds.left), 'R'));
//					}
//				}
//				if (c != colWordRows.length) {
//					if (colWordRows[c] != 0)
//						colAnchors.add(new TableColAnchor((c + pageBlocks[b].bounds.left), 'L'));
//				}
//				start = c;
//			}
//			System.out.println("Column Gaps: " + colGaps.toString());
//			System.out.println("Column Anchors: " + colAnchors.toString());
//		}
//		
//		//	assess possible block mergers
//		for (int b = 0; b < pageBlocks.length; b++) {
//			if (pageBlocks[b] == null)
//				continue;
//			if ((pageBlockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(pageBlockWords[b][0].getTextStreamType()))
//				continue;
//			
//			//	collect merge result
//			ImRegion mergedBlock = null;
//			ImRegion[] mergedBlockCols = null;
//			int mergedBlockRowCount = 3;
//			int mergedBlockCellCount = 0;
//			
//			//	try merging downward from narrow block (might be table column cut off others)
//			if (isPageBlockNarrow[b]) {
//				
//				//	find block with columns
//				int mergeBlockIndex = -1;
//				for (int l = (b+1); l < pageBlocks.length; l++) {
//					if (pageBlocks[l] == null)
//						continue;
//					if ((pageBlockWords[l].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(pageBlockWords[l][0].getTextStreamType()))
//						continue;
//					if (isPageBlockNarrow[l])
//						continue;
//					if (pageBlocks[l].bounds.top < pageBlocks[b].bounds.bottom)
//						continue;
//					if (pageBlockCols[l] != null)
//						mergeBlockIndex = l;
//					break;
//				}
//				if (mergeBlockIndex == -1)
//					continue;
//				
//				//	attempt merger
//				pm.setInfo("Attempting merger of " + pageBlocks[b].bounds + " and " + pageBlocks[mergeBlockIndex].bounds + " (" + pageBlockCols[mergeBlockIndex].length + " columns)");
//				BoundingBox mergeTestBlockBounds = new BoundingBox(Math.min(pageBlocks[b].bounds.left, pageBlocks[mergeBlockIndex].bounds.left), Math.max(pageBlocks[b].bounds.right, pageBlocks[mergeBlockIndex].bounds.right), pageBlocks[b].bounds.top, pageBlocks[mergeBlockIndex].bounds.bottom);
//				int mtbbLeft = mergeTestBlockBounds.left;
//				int mtbbRight = mergeTestBlockBounds.right;
//				for (int tb = 0; tb < pageBlocks.length; tb++) {
//					if (pageBlocks[tb] == null)
//						continue;
//					if (pageBlocks[tb].bounds.liesIn(mergeTestBlockBounds, false))
//						continue;
//					if (!pageBlocks[tb].bounds.liesIn(mergeTestBlockBounds, true))
//						continue;
//					mtbbLeft = Math.min(mtbbLeft, pageBlocks[tb].bounds.left);
//					mtbbRight = Math.max(mtbbRight, pageBlocks[tb].bounds.right);
//				}
//				if ((mtbbLeft < mergeTestBlockBounds.left) || (mergeTestBlockBounds.right < mtbbRight))
//					mergeTestBlockBounds = new BoundingBox(mtbbLeft, mtbbRight, mergeTestBlockBounds.top, mergeTestBlockBounds.bottom);
//				ImRegion mergeTestBlock = new ImRegion(page.getDocument(), page.pageId, mergeTestBlockBounds, ImRegion.BLOCK_ANNOTATION_TYPE);
//				pm.setInfo(" - merged block is " + mergeTestBlock.bounds);
//				ImRegion[] mergeTestBlockCols = this.getTableColumns(mergeTestBlock, minColMargin);
//				if (mergeTestBlockCols == null) {
//					pm.setInfo(" ==> foo few columns");
//					continue;
//				}
//				
//				//	allow tolerance of one column lost if 9 or more columns in main block ...
//				if (mergeTestBlockCols.length < pageBlockCols[mergeBlockIndex].length) {
//					if ((pageBlockCols[mergeBlockIndex].length > 8) && ((mergeTestBlockCols.length + 1) == pageBlockCols[mergeBlockIndex].length))
//						pm.setInfo(" - one column loss tolerated");
//					else {
//						pm.setInfo(" ==> too few columns (" + mergeTestBlockCols.length + ")");
//						break;
//					}
//				}
//				pm.setInfo(" - columns OK");
//				ImRegion[] mergeTestBlockRows = this.getTableRows(mergeTestBlock, minRowMargin);
//				if (mergeTestBlockRows == null) {
//					pm.setInfo(" ==> too few rows");
//					continue;
//				}
//				pm.setInfo(" - rows OK");
//				
//				//	... but only if more total cells in merge result than in main block
//				if ((pageBlockRows[mergeBlockIndex] != null) && ((mergeTestBlockCols.length * mergeTestBlockRows.length) < (pageBlockCols[mergeBlockIndex].length * pageBlockCols[mergeBlockIndex].length))) {
//					pm.setInfo(" ==> too few cells (1, " + mergeTestBlockCols.length + "x" + mergeTestBlockRows.length + " vs. " + pageBlockCols[b].length + "x" + pageBlockRows[b].length + ")");
//					continue;
//				}
//				else if ((mergeTestBlockCols.length * mergeTestBlockRows.length) < mergedBlockCellCount) {
//					pm.setInfo(" ==> too few cells (2, " + mergeTestBlockCols.length + "x" +  mergeTestBlockRows.length + " vs. " + mergedBlockCellCount + ")");
//					continue;
//				}
//				ImRegion[][] mergeTestBlockCells = this.getTableCells(page, mergeTestBlock, mergeTestBlockRows, mergeTestBlockCols, true);
//				if (mergeTestBlockCells == null) {
//					pm.setInfo(" ==> cells incomplete");
//					continue;
//				}
//				pm.setInfo(" - cells OK");
//				mergedBlock = mergeTestBlock;
//				mergedBlockCols = mergeTestBlockCols;
//				mergedBlockCellCount = (mergeTestBlockCols.length * mergeTestBlockRows.length);
//			}
//			
//			//	try merging downward from block with viable column gaps
//			else if (pageBlockCols[b] != null) {
//				
//				//	try merging downward
//				for (int l = (b+1); l < pageBlocks.length; l++) {
//					if (pageBlocks[l] == null)
//						continue;
//					if ((pageBlockWords[l].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(pageBlockWords[l][0].getTextStreamType()))
//						continue;
//					if (pageBlocks[l].bounds.top < pageBlocks[b].bounds.bottom)
//						continue;
//					
//					//	attempt merger
//					pm.setInfo("Attempting merger of " + pageBlocks[b].bounds + " (" + pageBlockCols[b].length + " columns) and " + pageBlocks[l].bounds);
//					BoundingBox mergeTestBlockBounds = new BoundingBox(Math.min(pageBlocks[b].bounds.left, pageBlocks[l].bounds.left), Math.max(pageBlocks[b].bounds.right, pageBlocks[l].bounds.right), pageBlocks[b].bounds.top, pageBlocks[l].bounds.bottom);
//					int mtbbLeft = mergeTestBlockBounds.left;
//					int mtbbRight = mergeTestBlockBounds.right;
//					for (int tb = 0; tb < pageBlocks.length; tb++) {
//						if (pageBlocks[tb] == null)
//							continue;
//						if (pageBlocks[tb].bounds.liesIn(mergeTestBlockBounds, false))
//							continue;
//						if (!pageBlocks[tb].bounds.liesIn(mergeTestBlockBounds, true))
//							continue;
//						mtbbLeft = Math.min(mtbbLeft, pageBlocks[tb].bounds.left);
//						mtbbRight = Math.max(mtbbRight, pageBlocks[tb].bounds.right);
//					}
//					if ((mtbbLeft < mergeTestBlockBounds.left) || (mergeTestBlockBounds.right < mtbbRight))
//						mergeTestBlockBounds = new BoundingBox(mtbbLeft, mtbbRight, mergeTestBlockBounds.top, mergeTestBlockBounds.bottom);
//					ImRegion mergeTestBlock = new ImRegion(page.getDocument(), page.pageId, mergeTestBlockBounds, ImRegion.BLOCK_ANNOTATION_TYPE);
//					pm.setInfo(" - merged block is " + mergeTestBlock.bounds);
//					ImRegion[] mergeTestBlockCols = this.getTableColumns(mergeTestBlock, minColMargin);
//					if (mergeTestBlockCols == null) {
//						pm.setInfo(" ==> too few columns");
//						break;
//					}
//					
//					//	allow tolerance of one column lost if 9 or more columns in main block ...
//					if (mergeTestBlockCols.length < pageBlockCols[b].length) {
//						if ((pageBlockCols[b].length > 8) && ((mergeTestBlockCols.length + 1) == pageBlockCols[b].length))
//							pm.setInfo(" - one column loss tolerated");
//						else {
//							pm.setInfo(" ==> too few columns (" + mergeTestBlockCols.length + ")");
//							break;
//						}
//					}
//					else pm.setInfo(" - columns OK");
//					ImRegion[] mergeTestBlockRows = this.getTableRows(mergeTestBlock, minRowMargin, mergedBlockRowCount);
//					if (mergeTestBlockRows == null) {
//						pm.setInfo(" ==> too few rows");
//						continue;
//					}
//					pm.setInfo(" - rows OK");
//					
//					//	... but only if more total cells in merge result than in main block
//					if ((pageBlockRows[b] != null) && ((mergeTestBlockCols.length * mergeTestBlockRows.length) < (pageBlockCols[b].length * pageBlockRows[b].length))) {
//						pm.setInfo(" ==> too few cells (1, " + mergeTestBlockCols.length + "x" + mergeTestBlockRows.length + " vs. " + pageBlockCols[b].length + "x" + pageBlockRows[b].length + ")");
//						continue;
//					}
//					else if ((mergeTestBlockCols.length * mergeTestBlockRows.length) < mergedBlockCellCount) {
//						pm.setInfo(" ==> too few cells (2, " + mergeTestBlockCols.length + "x" +  mergeTestBlockRows.length + " vs. " + mergedBlockCellCount + ")");
//						continue;
//					}
//					ImRegion[][] mergeTestBlockCells = this.getTableCells(page, mergeTestBlock, mergeTestBlockRows, mergeTestBlockCols, true);
//					if (mergeTestBlockCells == null) {
//						pm.setInfo(" ==> cells incomplete");
//						continue;
//					}
//					pm.setInfo(" - cells OK");
//					mergedBlock = mergeTestBlock;
//					mergedBlockCols = mergeTestBlockCols;
//					mergedBlockRowCount = mergeTestBlockRows.length;
//					mergedBlockCellCount = (mergeTestBlockCols.length * mergeTestBlockRows.length);
//				}
//			}
//			
//			//	any success?
//			if (mergedBlock == null)
//				continue;
//			
//			//	store merged block, and clean up all blocks inside
//			for (int c = 0; c < pageBlocks.length; c++) {
//				if (pageBlocks[c] == null)
//					continue;
//				if (!mergedBlock.bounds.includes(pageBlocks[c].bounds, true))
//					continue;
//				page.removeRegion(pageBlocks[c]);
//				pageBlocksBySubBlocks.remove(pageBlocks[c]);
//				pageBlocks[c] = null;
//				pageBlockCols[c] = null;
//				pageBlockWords[c] = null;
//				isPageBlockNarrow[c] = false;
//			}
//			
//			//	add merged block
//			page.addRegion(mergedBlock);
//			pageBlocks[b] = mergedBlock;
//			pageBlockCols[b] = mergedBlockCols;
//			pageBlockWords[b] = mergedBlock.getWords();
//			Arrays.sort(pageBlockWords[b], ImUtils.textStreamOrder);
//			isPageBlockNarrow[b] = false;
//			pm.setInfo(" - got merged block " + mergedBlock.bounds);
//		}
//		
//		//	merge adjacent sub blocks of same parent block that were not merged to form a table
//		ArrayList subBlockList = new ArrayList();
//		ImRegion subBlockParent = null;
//		int subBlockStartIndex = -1;
//		for (int b = 0; b <= pageBlocks.length; b++) {
//			if ((b < pageBlocks.length) && (pageBlocks[b] == null))
//				continue;
//			
//			//	get parent block to assess what to do
//			ImRegion pageBlockParent = null;
//			
//			//	we're in the last run, just end whatever we have
//			if (b == pageBlocks.length) {}
//			
//			//	this one's original, or a merger result, just end whatever we have
//			else if (pageBlocks[b].getPage() != null) {}
//			
//			//	we have a sub block
//			else {
//				pageBlockParent = ((ImRegion) pageBlocksBySubBlocks.remove(pageBlocks[b]));
//				if (pageBlockParent == null)
//					continue; // highly unlikely, but let's have this safety net
//				
//				//	same parent as previous sub block(s), add to list
//				if (subBlockParent == pageBlockParent) {
//					subBlockList.add(pageBlocks[b]);
//					continue;
//				}
//			}
//			
//			//	merge collected sub blocks and clean up (if we get here, we're not continuing current sub block)
//			if (subBlockList.size() != 0) {
//				BoundingBox subBlockBounds = ImLayoutObject.getAggregateBox((ImRegion[]) subBlockList.toArray(new ImRegion[subBlockList.size()]));
//				BoundingBox subBlockWordBounds = ImLayoutObject.getAggregateBox(page.getWordsInside(subBlockBounds));
//				pageBlocks[subBlockStartIndex] = new ImRegion(page, ((subBlockWordBounds == null) ? subBlockBounds : subBlockWordBounds), ImRegion.BLOCK_ANNOTATION_TYPE);
//				
//				pageBlockCols[subBlockStartIndex] = null;
//				pageBlockWords[subBlockStartIndex] = page.getWordsInside(pageBlocks[subBlockStartIndex].bounds);
//				Arrays.sort(pageBlockWords[subBlockStartIndex], ImUtils.textStreamOrder);
//				
//				for (int c = (subBlockStartIndex + 1); c < b; c++) {
//					pageBlocks[c] = null;
//					pageBlockCols[c] = null;
//					pageBlockWords[c] = null;
//				}
//				page.removeRegion(subBlockParent);
//				
//				subBlockList.clear();
//				subBlockParent = null;
//				subBlockStartIndex = -1;
//			}
//			
//			//	start new sub block (if we get here and have a parent block, we are to start a new sub block)
//			if (pageBlockParent != null) {
//				subBlockList.add(pageBlocks[b]);
//				subBlockParent = pageBlockParent;
//				subBlockStartIndex = b;
//			}
//		}
//		
//		//	mark single-block tables
//		for (int b = 0; b < pageBlocks.length; b++) {
//			if (pageBlocks[b] == null)
//				continue;
//			if ((pageBlockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(pageBlockWords[b][0].getTextStreamType()))
//				continue;
//			pm.setInfo("Testing block " + pageBlocks[b].bounds + " on page " + page.pageId + " for table");
//			if (this.markIfIsTable(page, pageBlocks[b], pageBlockCols[b], pageBlockWords[b], pageImageDpi, minColMargin, minRowMargin)) {
//				pm.setInfo(" ==> table detected");
//				page.removeRegion(pageBlocks[b]);
//			}
//			else if (this.markIfContainsTable(page, pageBlocks[b], pageBlockWords[b], pageImageDpi, minColMargin, minRowMargin)) {
//				pm.setInfo(" ==> table extracted");
//				page.removeRegion(pageBlocks[b]);
//			}
//			else pm.setInfo(" ==> not a table");
//		}
//	}
//	
//	private static class TableColGap implements Comparable {
//		final int left;
//		final int right;
//		TableColGap(int left, int right) {
//			this.left = left;
//			this.right = right;
//		}
//		boolean isCompatibleWith(TableColGap tcg) {
//			return ((this.left < tcg.right) && (tcg.left < this.right));
//		}
//		TableColGap joinWith(TableColGap tcg) {
//			return new TableColGap(Math.max(this.left, tcg.left), Math.min(this.right, tcg.right));
//		}
//		public String toString() {
//			return ("" + this.left + "-" + this.right);
//		}
//		public boolean equals(Object obj) {
//			return this.toString().equals(obj.toString());
//		}
//		public int compareTo(Object obj) {
//			TableColGap tcg = ((TableColGap) obj);
//			return ((this.left == tcg.left) ? (tcg.right - this.right) : (this.left - tcg.left));
////			if (obj instanceof TableColGap) {
////				TableColGap tcg = ((TableColGap) obj);
////				return ((this.left == tcg.left) ? (tcg.right - this.right) : (this.left - tcg.left));
////			}
////			else return -1;
//		}
//	}
//	
//	private static Comparator tableColGapOrder = new Comparator() {
//		public int compare(Object obj1, Object obj2) {
//			TableColGap tcg1 = ((TableColGap) obj1);
//			TableColGap tcg2 = ((TableColGap) obj2);
//			return ((tcg1.left == tcg2.left) ? (tcg2.right - tcg1.right) : (tcg1.left - tcg2.left));
//		}
//	};
//	
//	private static ArrayList joinTableColGaps(ArrayList gaps1, ArrayList gaps2) {
//		ArrayList gaps = new ArrayList();
//		for (int g = 0; g < gaps1.size(); g++) {
//			TableColGap tcg = ((TableColGap) gaps1.get(g));
//			for (int jg = 0; jg < gaps2.size(); jg++) {
//				TableColGap jTcg = ((TableColGap) gaps2.get(jg));
//				if (tcg.isCompatibleWith(jTcg)) {
//					System.out.println("Joining " + tcg + " with " + jTcg + " ==> " + tcg.joinWith(jTcg));
//					gaps.add(tcg.joinWith(jTcg));
//				}
//			}
//		}
//		Collections.sort(gaps);
//		return gaps;
//	}
//	
//	private static class TableColAnchor implements Comparable {
//		final int anchor;
//		final char type;
//		TableColAnchor(int anchor, char type) {
//			this.anchor = anchor;
//			this.type = type;
//		}
//		boolean isCompatibleWith(TableColAnchor tca, int dpi) {
//			return ((this.type == tca.type) && (Math.abs(this.anchor - tca.anchor) < (dpi / ((this.type == 'C') ? 20 : 50))));
//		}
//		TableColAnchor joinWith(TableColAnchor tca) {
//			return new TableColAnchor(((this.anchor + tca.anchor) / 2), this.type);
//		}
//		public String toString() {
//			return ("" + this.anchor + "-" + this.type);
//		}
//		public boolean equals(Object obj) {
//			return this.toString().equals(obj.toString());
//		}
//		public int compareTo(Object obj) {
//			return (this.anchor - ((TableColAnchor) obj).anchor);
////			if (obj instanceof TableColAnchor)
////				return (this.anchor - ((TableColAnchor) obj).anchor);
////			else return -1;
//		}
//	}
//	
//	private static ArrayList joinTableColAnchors(ArrayList anchors1, ArrayList anchors2, int dpi) {
//		ArrayList anchors = new ArrayList();
//		for (int a = 0; a < anchors1.size(); a++) {
//			TableColAnchor tca = ((TableColAnchor) anchors1.get(a));
//			for (int ja = 0; ja < anchors2.size(); ja++) {
//				TableColAnchor jTca = ((TableColAnchor) anchors2.get(ja));
//				if (tca.isCompatibleWith(jTca, dpi))
//					anchors.add(tca.joinWith(jTca));
//			}
//		}
//		Collections.sort(anchors);
//		return anchors;
//	}
}
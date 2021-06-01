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
package de.uka.ipd.idaho.im.imagine.plugins.doc;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.gamta.util.DocumentStyle;
import de.uka.ipd.idaho.gamta.util.DocumentStyle.ParameterDescription;
import de.uka.ipd.idaho.gamta.util.DocumentStyle.ParameterGroupDescription;
import de.uka.ipd.idaho.gamta.util.DocumentStyle.PropertiesData;
import de.uka.ipd.idaho.gamta.util.DocumentStyle.TestableElement;
import de.uka.ipd.idaho.gamta.util.ParallelJobRunner;
import de.uka.ipd.idaho.gamta.util.ParallelJobRunner.ParallelFor;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor.SynchronizedProgressMonitor;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.goldenGate.plugins.PluginDataProviderFileBased;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImLayoutObject;
import de.uka.ipd.idaho.im.ImObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImSupplement;
import de.uka.ipd.idaho.im.ImSupplement.Figure;
import de.uka.ipd.idaho.im.ImSupplement.Graphics;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.analysis.Imaging;
import de.uka.ipd.idaho.im.analysis.Imaging.AnalysisImage;
import de.uka.ipd.idaho.im.analysis.PageAnalysis;
import de.uka.ipd.idaho.im.analysis.PageAnalysis.BlockLayout;
import de.uka.ipd.idaho.im.analysis.PageAnalysis.BlockMetrics;
import de.uka.ipd.idaho.im.analysis.PageAnalysis.LineMetrics;
import de.uka.ipd.idaho.im.analysis.PageImageAnalysis;
import de.uka.ipd.idaho.im.analysis.PageImageAnalysis.Region;
import de.uka.ipd.idaho.im.gamta.ImDocumentRoot;
import de.uka.ipd.idaho.im.gamta.ImTokenSequence;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.DisplayExtensionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener;
import de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.basic.RegionActionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.tables.TableActionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.tables.TableDetectorProvider;
import de.uka.ipd.idaho.im.imagine.plugins.util.CaptionCitationHandler;
import de.uka.ipd.idaho.im.util.ImDocumentIO;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.DisplayExtension;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.DisplayExtensionGraphics;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentStyle;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;

/**
 * This plugin provides a detector for the structure of a document. The detector
 * identified page headers, page numbers, captions, and footnotes. It can also
 * be used as a component outside of GoldenGATE Imagine.
 * 
 * @author sautter
 */
public class DocumentStructureDetectorProvider extends AbstractImageMarkupToolProvider implements ImagingConstants, ReactionProvider, GoldenGateImagineDocumentListener {
	private boolean debug = false;
	private static final String TEXT_ORIENTATION_CENTERED_STRICT = "centered_strict";
//	private static final boolean detectTablesSimpleDefault = true;
//	private boolean detectTablesSimple = detectTablesSimpleDefault;
	
	private static final String STRUCTURE_DETECTOR_IMT_NAME = "StructureDetector";
	
	private String pageNumberPattern = "[1-9][0-9]*";
	private String ocrPageNumberPartPattern = "[0-9]+";
	private HashMap ocrPageNumberCharacterTranslations = new HashMap();
	
	private CaptionStartPattern[] captionStartPatterns = new CaptionStartPattern[0];
	
	private Pattern[] footnoteStartPatterns = new Pattern[0];
	
	private CaptionCitationHandler captionCitationHandler;
	
	private RegionActionProvider regionActionProvider;
	
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
		
		//	get region action provider
		if (this.parent == null)
			this.regionActionProvider = new RegionActionProvider();
		else this.regionActionProvider = ((RegionActionProvider) this.parent.getPlugin(RegionActionProvider.class.getName()));
		
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
		pgd.setParamLabel("splitLinePatterns", "Split Line Patterns");
		pgd.setParamDescription("splitLinePatterns", "<HTML>Patterns matching lines above or below which to enforce a paragraph split. These patterns are plain regular expression patterns prefixed with layout based matching parameters enclosed in curly brackets. There are the following parameters:<UL>" +
				"<LI><TT>PO</TT>: paragraph orientation in blocks the pattern applies to, either (combination) of L (left), R (right), J (justified), or C (centered), omission indicates wildcard; example: <TT>{PO:LJ} matches lines in blocks with left or justified text orientation</TT></LI>" +
				"<LI><TT>FS</TT>: font size of lines the pattern applies to, either a single number or a dash separated range, omission indicates wildcard; example: <TT>{FS:10-12}</TT> matches lines with font size between 10 and 12 (inclusive)</LI>" +
				"<LI><TT>FP</TT>: font properties of lines the pattern applies to, either (combination) of B (bold), I (italics), or C (all-caps), omission indicates wildcard; example: <TT>{FP:B}</TT> matches lines in bold</LI>" +
				"<LI><TT>SFP</TT>: font properties of the start of lines the pattern applies to, either (combination) of B (bold), I (italics), or C (all-caps), omission indicates wildcard; example: <TT>{SFP:B}</TT> matches lines with a bold start</LI>" +
				"<LI><TT>MBL</TT>: maximum number of lines in blocks the pattern applies to, omission indicates wildcard; example: <TT>{MBL:7}</TT> matches lines in blocks with up to 7 lines (inclusive)</LI>" +
				"<LI><TT>MAXPID</TT>: maximum page ID of lines the pattern applies to, omission indicates wildcard; example: <TT>{MAXPID:1}</TT> matches lines on pages up to 1 (inclusive)</LI>" +
				"<LI><TT>MINPID</TT>: minimum page ID of lines the pattern applies to, omission indicates wildcard; example: <TT>{MINPID:2}</TT> matches lines on pages from 2 onward (inclusive)</LI>" +
				"<LI><TT>SD</TT>: split direction to apply to mathing lines, A (above), B (below), or AB (both); example: <TT>{SD:A}</TT> indicates a split above matching lines</LI>" +
				"<LI><TT>R</TT>: the reason of the split, i.e., a label for the pattern, to increase tractability; example: <TT>{R:Heading}</TT> labels a split as being due to a heading</LI>" +
				"</UL></HTML>");
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
		pgd.setParamLabel("startIsSeparateLine", "Do Captions have a Line Breaks after their Starts?");
		pgd.setParamDescription("startIsSeparateLine", "Is there a line break separating caption starts from the main caption text, i.e., after the numbering? (setting this option to true requires start patterns to match entire caption start line)");
		pgd.setParamLabel("startPatterns", "Start Patterns");
		pgd.setParamDescription("startPatterns", "One or more patterns matching the start of any kind of captions, right up to the figure number; to be used alternatively to the four patterns dedicated to figures, tables, and plates and their parts.");
		pgd.setParamLabel("tableStartPatterns", "Table Caption Start Patterns");
		pgd.setParamDescription("tableStartPatterns", "One or more patterns matching the start of table captions, right up to the figure number.");
		pgd.setParamLabel("continuationPatterns", "Continuation Patterns");
		pgd.setParamDescription("continuationPatterns", "One or more patterns matching captions that belong to continuations of tables or figures.");
		DocumentStyle.addParameterGroupDescription(pgd);
		
		//	footnotes
		pgd = new TestableParameterGroupDescription("layout.footnote");
		pgd.setLabel("Footnote Layout");
		pgd.setDescription("Parameters describing the styling of footnotes and where they can be located on a page.");
		pgd.setParamLabel("area", "Footnote Area");
		pgd.setParamDescription("area", "A bounding box marking the area on a page where footnotes can be located (normalized to 72 DPI).");
		pgd.setParamLabel("separateBlock", "Are Footnotes in Separate Block?");
		pgd.setParamDescription("separateBlock", "Are footnotes located in a block of their own, separate from other parts like the main text?");
		pgd.setParamLabel("fontSize", "Font Size");
		pgd.setParamDescription("fontSize", "The exact font size of footnotes (use minimum and maximum font size if variation present or to be expected).");
		pgd.setParamLabel("maxFontSize", "Maximum Font Size");
		pgd.setParamDescription("maxFontSize", "The maximum font size of footnotes (use only if variation present or to be expected, otherwise use exact font size).");
		pgd.setParamLabel("minFontSize", "Minimum Font Size");
		pgd.setParamDescription("minFontSize", "The minimum font size of footnotes (use only if variation present or to be expected, otherwise use exact font size).");
		pgd.setParamLabel("startPatterns", "Start Patterns");
		pgd.setParamDescription("startPatterns", "One or more patterns matching the start of a footnote.");
		//footnote.separateBlock
		DocumentStyle.addParameterGroupDescription(pgd);
		
		//	pages in general ...
		pgd = new ParameterGroupDescription("layout.page");
		pgd.setLabel("Page Layout");
		pgd.setDescription("Parameters describing the layout of pages in general. The parameters that differ between even and odd pages should be entered in the respective sub groups instead. The same applies to parameters for the first page only (article headers, etc.).");
		pgd.setParamLabel("headerAreas", "Header Areas");
		pgd.setParamDescription("headerAreas", "One or more bounding boxes marking areas of the page that contain headers and footers (normalized to 72 DPI); text in such areas will be cut out of the main document text.");
		pgd.setParamLabel("columnAreas", "Column Areas");
		pgd.setParamDescription("columnAreas", "One or more bounding boxes marking individual text columns (normalized to 72 DPI); column splits occur only between these bounding boxes, not inside.");
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
		pgd.setParamLabel("columnAreas", "Column Areas");
		pgd.setParamDescription("columnAreas", "One or more bounding boxes marking individual text columns (normalized to 72 DPI); column splits occur only between these bounding boxes, not inside.");
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
		pgd.setParamLabel("columnAreas", "Column Areas");
		pgd.setParamDescription("columnAreas", "One or more bounding boxes marking individual text columns (normalized to 72 DPI); column splits occur only between these bounding boxes, not inside.");
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
		pgd.setParamLabel("columnAreas", "Column Areas");
		pgd.setParamDescription("columnAreas", "One or more bounding boxes marking individual text columns (normalized to 72 DPI); column splits occur only between these bounding boxes, not inside.");
		DocumentStyle.addParameterGroupDescription(pgd);
		
		//	... and the respective page number
		pgd = new ParameterGroupDescription("layout.page.first.number");
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
		pgd.setDescription("Parameters describing headings in general, in particular the number of heading groups at different levels.");
		pgd.setParamLabel("groups", "Heading Groups");
		pgd.setParamDescription("groups", "The heading groups to use, i.e., a space-separated list of the group numbers, starting with 1.");
		DocumentStyle.addParameterGroupDescription(pgd);
		
		//	... and level-specific ...
		String[] alignments = {ImRegion.TEXT_ORIENTATION_LEFT, ImRegion.TEXT_ORIENTATION_RIGHT, ImRegion.TEXT_ORIENTATION_CENTERED, TEXT_ORIENTATION_CENTERED_STRICT, ImRegion.TEXT_ORIENTATION_JUSTIFIED};
		String[] levels = {"1", "2", "3", "4", "5"};
		String[] lineBreakMergeModes = {"never", "group", "level-downwards", "level-upwards", "level"};
		for (int g = 1; g <= 9; g++) {
			pgd = new TestableParameterGroupDescription("style.heading." + g);
			pgd.setLabel("Group " + g + " Headings");
			pgd.setDescription("Parameters describing headings of group " + g + ".");
			pgd.setParamLabel("level", "Heading Level");
			pgd.setParamDescription("level", ("The logical level of paragraphs matched as headings of group " + g + " (defaults to " + g + " if not selected)."));
			pgd.setParamDefaultValue("level", ("" + g));
			pgd.setParamRequired("level");
			pgd.setParamValues("level", levels);
			pgd.setParamLabel("alignment", "Paragraph Alignment");
			pgd.setParamDescription("alignment", ("The alignment of paragraphs representing headings of group " + g + "."));
			pgd.setParamValues("alignment", alignments);
			pgd.setParamLabel("alignmentInBlock", "Locally Measure Paragraph Alignment in Block?");
			pgd.setParamDescription("alignmentInBlock", ("Measure the alignment of paragraphs representing headings of group " + g + " against the text block instead of the whole column?"));
			pgd.setParamLabel("lineBreakMergeMode", "Merge Across Line Breaks");
			pgd.setParamDescription("lineBreakMergeMode", "Indicates how to merge headings in this group with others across line breaks (if merging with other heading groups on same level, more restrictive one of the two merge modes counts)");
			pgd.setParamValues("lineBreakMergeMode", lineBreakMergeModes);
			pgd.setParamValueLabel("lineBreakMergeMode", "never", "Never");
			pgd.setParamValueLabel("lineBreakMergeMode", "group", "With Headings in Same Group");
			pgd.setParamValueLabel("lineBreakMergeMode", "level-downwards", "With Headings at Same Level (Downwards Only)");
			pgd.setParamValueLabel("lineBreakMergeMode", "level-upwards", "With Headings at Same Level (Upwards Only)");
			pgd.setParamValueLabel("lineBreakMergeMode", "level", "With Headings at Same Level");
			pgd.setParamLabel("canHaveIndent", "First Line Indented?");
			pgd.setParamDescription("canHaveIndent", ("Is the first line in headings of group " + g + " indented (relevant unless alignment is 'centered')?"));
			pgd.setParamLabel("containsAllCaps", "Contains All-Caps?");
			pgd.setParamDescription("containsAllCaps", ("Do headings of group " + g + " necessarily include words in all-caps?"));
			pgd.setParamLabel("containsSmallCaps", "Contains Small-Caps?");
			pgd.setParamDescription("containsSmallCaps", ("Do headings of group " + g + " necessarily include words in small-caps (requires presence of two font sizes)?"));
			pgd.setParamLabel("containsBold", "Contains Bold?");
			pgd.setParamDescription("containsBold", ("Do headings of group " + g + " necessarily include words in bold face?"));
			pgd.setParamLabel("containsItalics", "Contains Italics?");
			pgd.setParamDescription("containsItalics", ("Do headings of group " + g + " necessarily include words in italics?"));
			pgd.setParamLabel("startIsAllCaps", "Start In All-Caps?");
			pgd.setParamDescription("startIsAllCaps", ("Do headings of group " + g + " necessarily start with words in all-caps?"));
			pgd.setParamLabel("startIsSmallCaps", "Start In Small-Caps?");
			pgd.setParamDescription("startIsSmallCaps", ("Do headings of group " + g + " necessarily start with words in small-caps (requires presence of two font sizes)?"));
			pgd.setParamLabel("startIsBold", "Start In Bold?");
			pgd.setParamDescription("startIsBold", ("Do headings of group " + g + " necessarily start with words in bold face?"));
			pgd.setParamLabel("startIsItalics", "Start In Italics?");
			pgd.setParamDescription("containsItalics", ("Do headings of group " + g + " necessarily start with words in italics?"));
			pgd.setParamLabel("isAllCaps", "Completely All-Caps?");
			pgd.setParamDescription("isAllCaps", ("Are headings of group " + g + " completely in all-caps?"));
			pgd.setParamLabel("isSmallCaps", "Completely Small-Caps?");
			pgd.setParamDescription("isSmallCaps", ("Are headings of group " + g + " completely in small-caps (requires presence of two font sizes)?"));
			pgd.setParamLabel("isBold", "Completely Bold?");
			pgd.setParamDescription("isBold", ("Are headings of group " + g + " completely in bold face?"));
			pgd.setParamLabel("isItalics", "Completely In Italics?");
			pgd.setParamDescription("containsItalics", ("Are headings of group " + g + " completely in italics?"));
			pgd.setParamLabel("ignoreParentheses", "Ignore Parts in Parentheses in Font Style Assessment?");
			pgd.setParamDescription("ignoreParentheses", ("Ignore parts in parentheses on enforcing bold and all-caps font style?"));
			pgd.setParamLabel("isNonBlock", "Can Share Block?");
			pgd.setParamDescription("isNonBlock", ("Can headings of group " + g + " be in one text block together with subsequent document text, or do these headings always sit in a text block of their own?"));
			pgd.getParameterDescription("isNonBlock").addExcludedParameter("false", "alignmentInBlock");
			pgd.setParamLabel("fontSize", "Font Size");
			pgd.setParamDescription("fontSize", ("The exact font size of headings of group " + g + " (use minimum and maximum font size if variation present or to be expected)."));
			pgd.setParamLabel("maxFontSize", "Maximum Font Size");
			pgd.setParamDescription("maxFontSize", ("The maximum font size of headings of group " + g + " (use only if variation present or to be expected, otherwise use exact font size)."));
			pgd.setParamLabel("minFontSize", "Minimum Font Size");
			pgd.setParamDescription("minFontSize", ("The minimum font size of headings of group " + g + " (use only if variation present or to be expected, otherwise use exact font size)."));
			pgd.setParamLabel("maxLineCount", "Maximum Number Of Lines");
			pgd.setParamDescription("maxLineCount", ("The maximum number of lines contained in headings of group " + g + "."));
			pgd.setParamLabel("startPatterns", "Start Patterns");
			pgd.setParamDescription("startPatterns", ("One or more regular expression patterns matching the starts of headings of group " + g + "; best suited to matching on numbered headings."));
			pgd.setParamLabel("filterPatterns", "Filter Patterns");
			pgd.setParamDescription("filterPatterns", ("One or more regular expression patterns matching the starts of lines to exclude from heading group " + g + "; best suited to filtering out certain lines from a group of headings that use too similar font properties."));
			pgd.setParamLabel("splitOffBlock", "Split Off Block?");
			pgd.setParamDescription("splitOffBlock", ("Split headings of group " + g + " off the top of a larger text block if they are included in one?"));
			DocumentStyle.addParameterGroupDescription(pgd);
		}
		
		//	... as well as for in-line headings
		for (int g = 1; g <= 3; g++) {
			pgd = new TestableParameterGroupDescription("style.heading.inLine." + g);
			pgd.setLabel("Group " + g + " In-Line Headings");
			pgd.setDescription("Parameters describing in-line headings of group " + g + ".");
			pgd.setParamLabel("containsAllCaps", "Contains All-Caps?");
			pgd.setParamDescription("containsAllCaps", ("Do in-line headings of group " + g + " necessarily include words in all-caps?"));
			pgd.setParamLabel("containsSmallCaps", "Contains Small-Caps?");
			pgd.setParamDescription("containsSmallCaps", ("Do in-line headings of group " + g + " necessarily include words in small-caps (requires presence of two font sizes)?"));
			pgd.setParamLabel("containsBold", "Contains Bold?");
			pgd.setParamDescription("containsBold", ("Do in-line headings of group " + g + " necessarily include words in bold face?"));
			pgd.setParamLabel("containsItalics", "Contains Italics?");
			pgd.setParamDescription("containsItalics", ("Do in-line headings of group " + g + " necessarily include words in italics?"));
			pgd.setParamLabel("startIsAllCaps", "Start In All-Caps?");
			pgd.setParamDescription("startIsAllCaps", ("Do in-line headings of group " + g + " necessarily start with words in all-caps?"));
			pgd.setParamLabel("startIsSmallCaps", "Start In Small-Caps?");
			pgd.setParamDescription("startIsSmallCaps", ("Do in-line headings of group " + g + " necessarily start with words in small-caps (requires presence of two font sizes)?"));
			pgd.setParamLabel("startIsBold", "Start In Bold?");
			pgd.setParamDescription("startIsBold", ("Do in-line headings of group " + g + " necessarily start with words in bold face?"));
			pgd.setParamLabel("startIsItalics", "Start In Italics?");
			pgd.setParamDescription("containsItalics", ("Do in-line headings of group " + g + " necessarily start with words in italics?"));
			pgd.setParamLabel("isAllCaps", "Completely All-Caps?");
			pgd.setParamDescription("isAllCaps", ("Are in-line headings of group " + g + " completely in all-caps?"));
			pgd.setParamLabel("isSmallCaps", "Completely Small-Caps?");
			pgd.setParamDescription("isSmallCaps", ("Are in-line headings of group " + g + " completely in small-caps (requires presence of two font sizes)?"));
			pgd.setParamLabel("isBold", "Completely Bold?");
			pgd.setParamDescription("isBold", ("Are in-line headings of group " + g + " completely in bold face?"));
			pgd.setParamLabel("isItalics", "Completely In Italics?");
			pgd.setParamDescription("containsItalics", ("Are in-line headings of group " + g + " completely in italics?"));
			pgd.setParamLabel("ignoreParentheses", "Ignore Parts in Parentheses in Font Style Assessment?");
			pgd.setParamDescription("ignoreParentheses", ("Ignore parts in parentheses on enforcing bold and all-caps font style?"));
			pgd.setParamLabel("fontSize", "Font Size");
			pgd.setParamDescription("fontSize", ("The exact font size of in-line headings of group " + g + " (use minimum and maximum font size if variation present or to be expected)."));
			pgd.setParamLabel("maxFontSize", "Maximum Font Size");
			pgd.setParamDescription("maxFontSize", ("The maximum font size of in-line headings of group " + g + " (use only if variation present or to be expected, otherwise use exact font size)."));
			pgd.setParamLabel("minFontSize", "Minimum Font Size");
			pgd.setParamDescription("minFontSize", ("The minimum font size of in-line headings of group " + g + " (use only if variation present or to be expected, otherwise use exact font size)."));
			pgd.setParamLabel("minBlockLineCount", "Minimum Number Of Lines in Block");
			pgd.setParamDescription("minBlockLineCount", ("The minimum number of lines in a block containing in-line headings of group " + g + "."));
			pgd.setParamLabel("startPatterns", "Start Patterns");
			pgd.setParamDescription("startPatterns", ("One or more regular expression patterns matching the starts of in-line headings of group " + g + "."));
			pgd.setParamLabel("filterPatterns", "Filter Patterns");
			pgd.setParamDescription("filterPatterns", ("One or more regular expression patterns matching the starts of lines to exclude as in-line headings of group " + g + "; best suited to filtering out certain lines as in-line headings if start patterns are rather general."));
			DocumentStyle.addParameterGroupDescription(pgd);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#exit()
	 */
	public void exit() {
		TestableParameterGroupDescription.closeAttributeExtractionLogDisplay(null);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentOpened(de.uka.ipd.idaho.im.ImDocument, java.lang.Object, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
	 */
	public void documentOpened(ImDocument doc, Object source, ProgressMonitor pm) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentSelected(de.uka.ipd.idaho.im.ImDocument)
	 */
	public void documentSelected(ImDocument doc) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentSaving(de.uka.ipd.idaho.im.ImDocument, java.lang.Object, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
	 */
	public void documentSaving(ImDocument doc, Object dest, ProgressMonitor pm) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentSaved(de.uka.ipd.idaho.im.ImDocument, java.lang.Object, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
	 */
	public void documentSaved(ImDocument doc, Object dest, ProgressMonitor pm) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentClosed(java.lang.String)
	 */
	public void documentClosed(String docId) {
		TestableParameterGroupDescription.closeAttributeExtractionLogDisplay(docId);
	}
	
	private static class TestableParameterGroupDescription extends ParameterGroupDescription implements DisplayExtension, TestableElement {
		TestableParameterGroupDescription(String pnp) {
			super(pnp);
		}
		public boolean isActive() {
			return true;
		}
		public DisplayExtensionGraphics[] getExtensionGraphics(ImPage page, ImDocumentMarkupPanel idmp) {
			ImDocumentStyle docStyle = ImDocumentStyle.getStyleFor(idmp.document);
			if (docStyle == null)
				return DisplayExtensionProvider.NO_DISPLAY_EXTENSION_GRAPHICS;
			ImDocumentStyle style = docStyle.getImSubset(this.parameterNamePrefix);
			if (style.getPropertyNames().length == 0)
				return DisplayExtensionProvider.NO_DISPLAY_EXTENSION_GRAPHICS;
			
			//	collect any highlights, and set color
			ArrayList shapes = new ArrayList();
			Color color = null;
			if (this.parameterNamePrefix.startsWith("style.heading.inLine.")) {
				this.addInLineHeadingShapes(page, docStyle, shapes, idmp);
				color = idmp.getAnnotationColor(EMPHASIS_TYPE);
			}
			else if (this.parameterNamePrefix.startsWith("style.heading.")) {
				this.addHeadingShapes(page, docStyle, shapes, idmp);
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
		private void addHeadingShapes(ImPage page, ImDocumentStyle docStyle, ArrayList shapes, ImDocumentMarkupPanel idmp) {
			int group = Integer.parseInt(this.parameterNamePrefix.substring("style.heading.".length()));
			HeadingStyleDefined hsd = HeadingStyleDefined.getHeadingStyle(group, docStyle.getSubset("style.heading"));
			if (hsd == null)
				return;
			
			//	set up match logging
			String docId = ((idmp == null) ? ((page.getDocument() == null) ? null : page.getDocument().docId) : idmp.document.docId);
			ByteArrayOutputStream logBaos = new ByteArrayOutputStream();
			PrintStream log;
			try {
				log = new PrintStream(logBaos, true, "UTF-8");
			}
			catch (UnsupportedEncodingException uee) {
				log = new PrintStream(logBaos, true); // never gonna happen, but Java is dumb !!!
			}
			
			//	get matching lines
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
				int[] lineLeftColDists = new int[blockLines.length];
				int[] lineRightColDists = new int[blockLines.length];
				int[] lineLeftBlockDists = new int[blockLines.length];
				int[] lineRightBlockDists = new int[blockLines.length];
				for (int l = 0; l < blockLines.length; l++) {
					lineWords[l] = getLargestTextStreamWords(blockLines[l].getWords());
					if (lineWords[l].length == 0)
						continue;
					Arrays.sort(lineWords[l], ImUtils.textStreamOrder);
					if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(lineWords[l][0].getTextStreamType()))
						continue;
					
					//	assess line position and length (center alignment against surrounding column to catch short blocks)
					lineLeftColDists[l] = (blockLines[l].bounds.left - blockParentColumnBounds.left);
					lineRightColDists[l] = (blockParentColumnBounds.right - blockLines[l].bounds.right);
					lineLeftBlockDists[l] = (blockLines[l].bounds.left - pageBlocks[b].bounds.left);
					lineRightBlockDists[l] = (pageBlocks[b].bounds.right - blockLines[l].bounds.right);
					
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
					if (hsd.matches(page, blockLines[l], lineWords[l], l, blockLines.length, lineLeftColDists[l], lineRightColDists[l], lineLeftBlockDists[l], lineRightBlockDists[l], log))
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
			
			//	display matching protocol
			displayAttributeExtractionLog(docId, this.parameterNamePrefix, logBaos.toByteArray());
		}
		private void addInLineHeadingShapes(ImPage page, DocumentStyle docStyle, ArrayList shapes, ImDocumentMarkupPanel idmp) {
			int group = Integer.parseInt(this.parameterNamePrefix.substring("style.heading.inLine.".length()));
			InLineHeadingStyle ilhs = InLineHeadingStyle.getInLineHeadingStyle(group, docStyle.getSubset("style.heading.inLine"));
			if (ilhs == null)
				return;
			
			//	set up match logging
			String docId = ((idmp == null) ? ((page.getDocument() == null) ? null : page.getDocument().docId) : idmp.document.docId);
			ByteArrayOutputStream logBaos = new ByteArrayOutputStream();
			PrintStream log;
			try {
				log = new PrintStream(logBaos, true, "UTF-8");
			}
			catch (UnsupportedEncodingException uee) {
				log = new PrintStream(logBaos, true); // never gonna happen, but Java is dumb !!!
			}
			
			//	work block by block
			ImRegion[] pageBlocks = page.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
			for (int b = 0; b < pageBlocks.length; b++) {
				ImRegion[] blockLines = pageBlocks[b].getRegions(ImRegion.LINE_ANNOTATION_TYPE);
				if (blockLines.length == 0)
					continue;
				
				//	work line by line
				Arrays.sort(blockLines, ImUtils.topDownOrder);
				for (int l = 0; l < blockLines.length; l++) {
					ImWord[] lineWords = blockLines[l].getWords();
					if (lineWords.length == 0)
						continue;
					Arrays.sort(lineWords, ImUtils.textStreamOrder);
					ImWord inLineHeadingEnd = ilhs.getMatchEnd(page, blockLines[l], lineWords, blockLines.length, log);
					if (inLineHeadingEnd == null)
						continue;
					for (int w = 0; w < lineWords.length; w++)
						if (lineWords[w] == inLineHeadingEnd) {
							BoundingBox hb = ImLayoutObject.getAggregateBox(lineWords, 0, (w+1));
							shapes.add(new Rectangle2D.Float(hb.left, hb.top, hb.getWidth(), hb.getHeight()));
							break;
						}
				}
			}
			
			//	display matching protocol
			displayAttributeExtractionLog(docId, this.parameterNamePrefix, logBaos.toByteArray());
		}
		private void addCaptionShapes(ImPage page, DocumentStyle style, ArrayList shapes) {
			boolean startIsBold = style.getBooleanProperty("startIsBold", false);
			boolean startIsSeparateLine = style.getBooleanProperty("startIsSeparateLine", false);
			int fontSize = style.getIntProperty("fontSize", -1);
			int minFontSize = style.getIntProperty("minFontSize", ((fontSize == -1) ? 0 : fontSize));
			int maxFontSize = style.getIntProperty("maxFontSize", ((fontSize == -1) ? 72 : fontSize));
			String[] startPatternStrs = style.getStringListProperty("startPatterns", null, " ");
			String[] figureStartPatternStrs = style.getStringListProperty("figureStartPatterns", null, " ");
			String[] tableStartPatternStrs = style.getStringListProperty("tableStartPatterns", null, " ");
			String[] plateStartPatternStrs = style.getStringListProperty("plateStartPatterns", null, " ");
			String[] platePartStartPatternStrs = style.getStringListProperty("platePartStartPatterns", null, " ");
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
				HashMap blockCaptionsByBounds = markBlockCaptions(null, pageBlocks[b], pageParagraphs, startPatterns, new CaptionStartPattern[0], minFontSize, maxFontSize, startIsBold, startIsSeparateLine, ProgressMonitor.silent);
				if (blockCaptionsByBounds == null)
					continue;
				for (Iterator cbit = blockCaptionsByBounds.keySet().iterator(); cbit.hasNext();) {
					BoundingBox cb = ((BoundingBox) cbit.next());
					shapes.add(new Rectangle2D.Float(cb.left, cb.top, cb.getWidth(), cb.getHeight()));
				}
			}
		}
		private void addFootnoteShapes(ImPage page, ImDocumentStyle style, ArrayList shapes) {
			int fontSize = style.getIntProperty("fontSize", -1);
			int minFontSize = style.getIntProperty("minFontSize", ((fontSize == -1) ? 0 : fontSize));
			int maxFontSize = style.getIntProperty("maxFontSize", ((fontSize == -1) ? 72 : fontSize));
			String[] startPatternStrs = style.getStringListProperty("startPatterns", "[^\\s]+".split("\\;"), " ");
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
		public void test(DocumentStyle paramGroup) {
			if (!this.parameterNamePrefix.startsWith("style.heading."))
				return; // TODO find other elements with match logging
			if (attributeExtractionLogDisplay == null)
				attributeExtractionLogDisplay = new AttributeExtractionLogDisplay();
			attributeExtractionLogDisplay.setVisible(true);
			attributeExtractionLogDisplay.getDialog().toFront();
			//	actual content will come from display extension update in our case
		}
		
		private static final Color attributeMatchLineColor = Color.BLUE;
		private static final BasicStroke attributeMatchLineStroke = new BasicStroke(3);
		private static final Color attributeMatchFillColor = new Color(attributeMatchLineColor.getRed(), attributeMatchLineColor.getGreen(), attributeMatchLineColor.getBlue(), 64);
		
		//	TODO any reasons to bind this to a plug-in instance ???
		private static AttributeExtractionLogDisplay attributeExtractionLogDisplay = null;
		private static void displayAttributeExtractionLog(String docId, String attribute, byte[] logBytes) {
			if (attributeExtractionLogDisplay == null)
				return;
			String log;
			try {
				log = new String(logBytes, "UTF-8");
			}
			catch (UnsupportedEncodingException uee) {
				log = new String(logBytes);
			}
			attributeExtractionLogDisplay.setLog(docId, attribute, log);
		}
		static void closeAttributeExtractionLogDisplay(String docId) {
			if (attributeExtractionLogDisplay == null)
				return;
			if ((docId != null) && !docId.equals(attributeExtractionLogDisplay.docId))
				return;
			attributeExtractionLogDisplay.dispose();
		}
		private static class AttributeExtractionLogDisplay extends DialogPanel {
			private String docId;
			private JTextArea log = new JTextArea();
			AttributeExtractionLogDisplay() {
				super("Heading Style Match Log", false);
				
				JScrollPane logBox = new JScrollPane(this.log);
				logBox.getHorizontalScrollBar().setUnitIncrement(50);
				logBox.getHorizontalScrollBar().setBlockIncrement(500);
				logBox.getVerticalScrollBar().setUnitIncrement(50);
				logBox.getVerticalScrollBar().setBlockIncrement(500);
				this.add(logBox, BorderLayout.CENTER);
				
				JButton close = new JButton("Close");
				close.setBorder(BorderFactory.createRaisedBevelBorder());
				close.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						dispose();
					}
				});
				this.add(close, BorderLayout.SOUTH);
				
				this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				this.setSize(600, 800);
				this.setLocationRelativeTo(null);
			}
			public void dispose() {
				attributeExtractionLogDisplay = null;
				super.dispose();
			}
			void setLog(String docId, String attribute, String log) {
				this.docId = docId;
				this.setTitle("Attribute Extraction Log - " + attribute);
				this.log.setText(log);
			}
		}
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
			ImDocumentStyle docStyle = ImDocumentStyle.getStyleFor(idmp.document);
			if (docStyle == null)
				return DisplayExtensionProvider.NO_DISPLAY_EXTENSION_GRAPHICS;
			ImDocumentStyle layoutStyle = docStyle.getImSubset("layout");
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
		if (annotation.hasAttribute(IN_LINE_OBJECT_MARKER_ATTRIBUTE))
			return;
		
		//	add any sub caption starts ...
		if (addPlateSubCaptionPatternStarts(annotation, this.captionStartPatterns, null))
			
			//	... and have citation handler process any findings (gets called for reaction before us ...)
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
		ImDocumentStyle docStyle = ImDocumentStyle.getStyleFor(doc);
		if (docStyle == null)
			docStyle = new ImDocumentStyle(new PropertiesData(new Properties()));
		final ImDocumentStyle docLayout = docStyle.getImSubset("layout");
		
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
				pageHeaderAreas[p] = docLayout.getBoxListProperty(("page.headerAreas"), null, pageImageDpi);
				//	first page is odd, so we need to decide 1-based, not 0-based
				pageHeaderAreas[p] = docLayout.getBoxListProperty(("page." + (((p % 2) == 0) ? "odd" : "even") + ".headerAreas"), pageHeaderAreas[p], pageImageDpi);
				if (p == 0) // get header areas for first page exclusive
					pageHeaderAreas[p] = docLayout.getBoxListProperty("page.first.headerAreas", pageHeaderAreas[p], pageImageDpi);
				
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
				layoutPrefix = "first";
			else if ((p % 2) == 0) // first page is odd, so we need to decide 1-based, not 0-based
				layoutPrefix = "odd";
			else layoutPrefix = "even"; // first page is odd, so we need to decide 1-based, not 0-based
			BoundingBox pageNumberArea = docLayout.getBoxProperty(("page." + layoutPrefix + ".number.area"), docLayout.getBoxProperty(("page.number.area"), null, pageImageDpi), pageImageDpi);
			int pageNumberFontSize = docLayout.getIntProperty(("page." + layoutPrefix + ".number.fontSize"), docLayout.getIntProperty(("page.number.fontSize"), -1));
			boolean pageNumberIsBold = docLayout.getBooleanProperty(("page." + layoutPrefix + ".number.isBold"), docLayout.getBooleanProperty(("page.number.isBold"), false));
			pageNumberPartPattern = docLayout.getStringProperty(("page." + layoutPrefix + ".number.pattern"), docLayout.getStringProperty(("page.number.pattern"), pageNumberPartPattern));
			
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
				spm.setInfo(" - got " + pageNumberParts.size() + " possible page numbers for page " + p + " (id " + pageData[p].page.pageId + "):");
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
		ImDocumentStyle tableLayout = docLayout.getImSubset(ImRegion.TABLE_TYPE);
//		final int minTableColMargin = tableLayout.getIntProperty("minColumnMargin", (pageImageDpi / 30), pageImageDpi);
//		final int minTableRowMargin = tableLayout.getIntProperty("minRowMargin", (pageImageDpi / 50), pageImageDpi);
		
		//	detect tables
		spm.setStep(" - detecting tables");
		spm.setBaseProgress(30);
		spm.setMaxProgress(40);
		this.tableDetectorProvider.detectTables(doc, tableLayout, null, spm);
		
		//	split main text blocks off images if document born digital
		if (documentBornDigital) {
			spm.setStep(" - splitting main text blocks off images");
			spm.setBaseProgress(40);
			spm.setMaxProgress(45);
			
			//	find main text stream(s) of document (we need to record them all in case there is a break somewhere in the middle)
			ImWord[] docTextStreamHeads = doc.getTextStreamHeads();
			HashSet docMainTextStreamIDs = new HashSet();
			for (int h = 0; h < docTextStreamHeads.length; h++) {
				if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(docTextStreamHeads[h].getTextStreamType()))
					continue;
				HashSet textStreamPageIDs = new HashSet();
				for (ImWord imw = docTextStreamHeads[h]; imw != null; imw = imw.getNextWord()) {
					if ((imw.getNextWord() == null) || (imw.pageId != imw.getNextWord().pageId))
						textStreamPageIDs.add(new Integer(imw.pageId));
				}
				if (textStreamPageIDs.size() > Math.max(Math.min(3, pages.length), (pages.length / 5)))
					docMainTextStreamIDs.add(docTextStreamHeads[h].getTextStreamId());
			}
			
			//	work page by page (cannot go parallel because text stream chaining needs to work linear)
			for (int p = 0; p < pages.length; p++) {
				spm.setProgress((p * 100) / pages.length);
				ImRegion[] pageImages = pages[p].getRegions(ImRegion.IMAGE_TYPE);
				if (pageImages.length == 0)
					continue;
				if (pageImages.length > 1)
					Arrays.sort(pageImages, ImUtils.topDownOrder);
				ImSupplement[] pageSupplements = pages[p].getSupplements();
				for (int i = 0; i < pageImages.length; i++) {
					pm.setInfo("Investigating image region at " + pageImages[i].pageId + "@" + pageImages[i].bounds);
					
					//	get words
					ImWord[] imageWords = pageImages[i].getWords();
					if (imageWords.length == 0) {
						pm.setInfo(" ==> no words enclosed");
						continue; // nothing to split
					}
					pm.setInfo(" - got " + imageWords.length + " enclosed words");
					
					//	get bounds of actual figure image(s) and graphics objects
					Figure[] imageFigures = ImSupplement.getFiguresIn(pageSupplements, pageImages[i].bounds);
					if (imageFigures.length == 0) {
						pm.setInfo(" ==> figure supplements not found");
						continue; // nothing to split around
					}
					BoundingBox imageFigureBounds;
					if (imageFigures.length == 1)
						imageFigureBounds = imageFigures[0].getBounds();
					else {
						BoundingBox[] figureBounds = new BoundingBox[imageFigures.length];
						for (int f = 0; f < imageFigures.length; f++)
							figureBounds[f] = imageFigures[f].getBounds();
						imageFigureBounds = BoundingBox.aggregate(figureBounds);
					}
					pm.setInfo(" - got " + imageFigures.length + " figure supplements spanning " + imageFigureBounds);
					Graphics[] imageGraphics = ImSupplement.getGraphicsIn(pageSupplements, pageImages[i].bounds);
					if (imageGraphics.length != 0) {
						BoundingBox imageGraphicsBounds;
						if (imageGraphics.length == 1)
							imageGraphicsBounds =  imageGraphics[0].getBounds();
						else {
							BoundingBox[] graphicsBounds = new BoundingBox[imageGraphics.length];
							for (int g = 0; g < imageGraphics.length; g++)
								graphicsBounds[g] = imageGraphics[g].getBounds();
							imageGraphicsBounds = BoundingBox.aggregate(graphicsBounds);
						}
						pm.setInfo(" - got " + imageGraphics.length + " graphics supplements spanning " + imageGraphicsBounds);
						imageFigureBounds = imageFigureBounds.union(imageGraphicsBounds);
						pm.setInfo(" - got figure and graphics supplements spanning " + imageFigureBounds);
					}
					
					//	collect words lying inside and outside figures
					ArrayList insideFigureWords = new ArrayList();
					ArrayList outsideFigureWords = new ArrayList();
					for (int w = 0; w < imageWords.length; w++) {
						
						//	outside aggregate figure bounds
						if (!imageFigureBounds.overlaps(imageWords[w].bounds)) {
							outsideFigureWords.add(imageWords[w]);
							continue;
						}
						
						//	with single figure, the bounds mark a compact and continuous area
						if (imageFigures.length == 1) {
							insideFigureWords.add(imageWords[w]);
							continue;
						}
						
						//	test against individual figures and graphics
						boolean wordOverlapsFigure = false;
						if (!wordOverlapsFigure)
							for (int f = 0; f < imageFigures.length; f++) {
								if (imageWords[w].bounds.bottom <= imageFigures[f].getBounds().top)
									continue;
								if (imageFigures[f].getBounds().bottom <= imageWords[w].bounds.top)
									continue;
								wordOverlapsFigure = true;
								break;
							}
						if (!wordOverlapsFigure)
							for (int g = 0; g < imageGraphics.length; g++) {
								if (imageWords[w].bounds.bottom <= imageGraphics[g].getBounds().top)
									continue;
								if (imageGraphics[g].getBounds().bottom <= imageWords[w].bounds.top)
									continue;
								wordOverlapsFigure = true;
								break;
							}
						(wordOverlapsFigure ? insideFigureWords : outsideFigureWords).add(imageWords[w]);
					}
					
					//	anything to split off?
					if (outsideFigureWords.isEmpty()) {
						pm.setInfo(" ==> all words located inside figure supplements");
						continue;
					}
					pm.setInfo(" - got " + insideFigureWords.size() + " words inside figure supplements, " + outsideFigureWords.size() + " outside");
					
					/* TODO check if words outside image actually constitute text blocks:
					 * - homogeneous font size
					 * - alignment in proper lines (as opposed to scattered words)
					 * - word distances similar to main text (as opposed to scattered words)
					 * - word lengths beyond index letters
					 */
					
					//	run blocking on image of figure region, but only render words, not figures
					BufferedImage bi = new BufferedImage(pageImages[i].bounds.getWidth(), pageImages[i].bounds.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
					Graphics2D gr = bi.createGraphics();
					gr.setColor(Color.WHITE);
					gr.fillRect(0, 0, bi.getWidth(), bi.getHeight());
					for (int w = 0; w < imageWords.length; w++)
						for (int x = Math.max(0, (imageWords[w].bounds.left - pageImages[i].bounds.left)); x < Math.min(bi.getWidth(), (imageWords[w].bounds.right - pageImages[i].bounds.left)); x++) {
							for (int y = Math.max(0, (imageWords[w].bounds.top - pageImages[i].bounds.top)); y < Math.min(bi.getHeight(), (imageWords[w].bounds.bottom - pageImages[i].bounds.top)); y++)
								bi.setRGB(x, y, 0xFF000000);
						}
					AnalysisImage ai = Imaging.wrapImage(bi, null);
					Region imageRegion = PageImageAnalysis.getPageRegion(ai, pages[p].getImageDPI(), Short.MAX_VALUE, (pages[p].getImageDPI() / 5), false, pm);
					System.out.println(" - got " + imageRegion.getSubRegionCount() + " initial blocks inside image region:");
					ArrayList imageBlocks = new ArrayList();
					imageBlocks.add(imageRegion);
					for (int b = 0; b < imageBlocks.size(); b++) {
						imageRegion = ((Region) imageBlocks.get(b));
						System.out.println("   - " + imageRegion.getBoundingBox());
						if (imageRegion.isAtomic())
							continue;
						for (int r = 0; r < imageRegion.getSubRegionCount(); r++) {
							System.out.println("     - " + imageRegion.getSubRegion(r).getBoundingBox());
							imageBlocks.add(imageRegion.getSubRegion(r));
						}
						imageBlocks.remove(b--); // we've replaced this one with its sub regions
					}
					pm.setInfo(" - got " + imageBlocks.size() + " blocks inside image region");
					if (imageBlocks.isEmpty())
						continue;
					
					//	classify blocks into text and image regions, and translate bounding boxes
					ArrayList imageTextBlockBoxes = new ArrayList();
					ArrayList imageLabelBlockBoxes = new ArrayList();
					pm.setInfo(" - classifying image blocks:");
					for (int b = 0; b < imageBlocks.size(); b++) {
						imageRegion = ((Region) imageBlocks.get(b));
						BoundingBox imageBlockBox = imageRegion.getBoundingBox().translate(pageImages[i].bounds.left, pageImages[i].bounds.top);
						pm.setInfo("   - " + imageBlockBox);
						
						//	outside aggregate figure bounds
						if (!imageFigureBounds.overlaps(imageBlockBox)) {
							imageTextBlockBoxes.add(imageBlockBox);
							pm.setInfo("      ==> text block");
							continue;
						}
						
						//	with single figure, the bounds mark a compact and continuous area
						if (imageFigures.length == 1) {
							imageLabelBlockBoxes.add(imageBlockBox);
							pm.setInfo("      ==> image block");
							continue;
						}
						
						//	test against individual figures
						boolean blockOverlapsFigure = false;
						for (int f = 0; f < imageFigures.length; f++) {
							if (imageBlockBox.bottom <= imageFigures[f].getBounds().top)
								continue;
							if (imageFigures[f].getBounds().bottom <= imageBlockBox.top)
								continue;
							blockOverlapsFigure = true;
							break;
						}
						(blockOverlapsFigure ? imageLabelBlockBoxes : imageTextBlockBoxes).add(imageBlockBox);
						pm.setInfo("      ==> " + (blockOverlapsFigure ? "image" : "text") + " block");
					}
					
					//	anything to split?
					if (imageTextBlockBoxes.isEmpty())
						continue;
					if (imageTextBlockBoxes.size() > 1)
						Collections.sort(imageTextBlockBoxes, new Comparator() {
							public int compare(Object obj1, Object obj2) {
								return (((BoundingBox) obj1).top - ((BoundingBox) obj2).top);
							}
						});
					
					//	wrap reduced image boxes around content of space between text blocks
					ArrayList reducedImageBoxes = new ArrayList();
					for (int tb = 0; tb <= imageTextBlockBoxes.size(); tb++) {
						int imageBoxTop = ((tb == 0) ? pageImages[i].bounds.top : ((BoundingBox) imageTextBlockBoxes.get(tb-1)).bottom);
						int imageBoxBottom = ((tb == imageTextBlockBoxes.size()) ? pageImages[i].bounds.bottom : ((BoundingBox) imageTextBlockBoxes.get(tb)).top);
						int ibLeft = pageImages[i].bounds.right;
						int ibRight = pageImages[i].bounds.left;
						int ibTop = imageBoxBottom;
						int ibBottom = imageBoxTop;
						for (int f = 0; f < imageFigures.length; f++) {
							BoundingBox imageFigureBox = imageFigures[f].getBounds();
							if (imageBoxBottom <= imageFigureBox.top)
								continue;
							if (imageFigureBox.bottom <= imageBoxTop)
								continue;
							ibLeft = Math.min(ibLeft, imageFigureBox.left);
							ibRight = Math.max(ibRight, imageFigureBox.right);
							ibTop = Math.min(ibTop, imageFigureBox.top);
							ibBottom = Math.max(ibBottom, imageFigureBox.bottom);
						}
						for (int lb = 0; lb < imageLabelBlockBoxes.size(); lb++) {
							BoundingBox labelBlockBox = ((BoundingBox) imageLabelBlockBoxes.get(lb));
							if (imageBoxBottom <= labelBlockBox.top)
								continue;
							if (labelBlockBox.bottom <= imageBoxTop)
								continue;
							ibLeft = Math.min(ibLeft, labelBlockBox.left);
							ibRight = Math.max(ibRight, labelBlockBox.right);
							ibTop = Math.min(ibTop, labelBlockBox.top);
							ibBottom = Math.max(ibBottom, labelBlockBox.bottom);
						}
						if ((ibLeft < ibRight) && (ibTop < ibBottom))
							reducedImageBoxes.add(new BoundingBox(ibLeft, ibRight, ibTop, ibBottom));
					}
					
					//	remove image region
					pages[p].removeRegion(pageImages[i]);
					
					//	mark reduced image region(s)
					for (int ri = 0; ri < reducedImageBoxes.size(); ri++) {
						ImRegion reducedImage = new ImRegion(pages[p], ((BoundingBox) reducedImageBoxes.get(ri)), ImRegion.IMAGE_TYPE);
						pm.setInfo(" ==> marked reduced image at " + reducedImage.bounds);
					}
					
					//	mark text blocks
					ImWord imageFirstWord = null;
					ImWord imageLastWord = null;
					for (int b = 0; b < imageTextBlockBoxes.size(); b++) {
						ImRegion textBlock = this.regionActionProvider.markBlock(pages[p], ((BoundingBox) imageTextBlockBoxes.get(b)));
						if (textBlock == null)
							continue;
						pm.setInfo(" ==> marked text block at " + textBlock.bounds);
						ImWord[] blockWords = textBlock.getWords();
						Arrays.sort(blockWords, ImUtils.textStreamOrder);
						if (imageFirstWord == null) {
							imageFirstWord = blockWords[0];
							imageFirstWord.setTextStreamType(ImWord.TEXT_STREAM_TYPE_MAIN_TEXT);
						}
						if (imageLastWord != null) {
							imageLastWord.setNextWord(blockWords[0]);
							imageLastWord.setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
						}
						imageLastWord = blockWords[blockWords.length-1];
					}
					if ((imageFirstWord == null) || (imageLastWord == null))
						continue; // failed to mark any block for some reason
					pm.setInfo(" - first text block word is " + imageFirstWord.getString() + " at " + imageFirstWord.getLocalID());
					pm.setInfo(" - last text block word is " + imageLastWord.getString() + " at " + imageLastWord.getLocalID());
					
					//	find predecessor and successor words of image region (for linking in text blocks)
					ImWord imagePredecessor = null;
					ImWord imageSuccessor = null;
					pm.setInfo(" - seeking predecessor for " + imageFirstWord.getString() + " at " + imageFirstWord.getLocalID());
					for (int lp = p; lp >= 0; lp--) {
						ImWord[] textStreamHeads = pages[lp].getTextStreamHeads();
						pm.setInfo("   - got " + textStreamHeads.length + " text stream heads in page " + pages[lp].pageId);
						for (int h = 0; h < textStreamHeads.length; h++) {
							if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(textStreamHeads[h].getTextStreamType()))
								continue; // we're only after main text
							if (!docMainTextStreamIDs.contains(textStreamHeads[h].getTextStreamId()))
								continue; // skip over parentheses, etc., which only run for one or two pages
							if ((lp == p) && (pageImages[i].bounds.includes(textStreamHeads[h].bounds, true)))
								continue; // no use checking the head of the labels we are trying to splice in
							
							//	same page, use text stream head if above or to left of image, use its predecessor otherwise
							pm.setInfo("   - checking " + textStreamHeads[h].getString() + " at " + textStreamHeads[h].getLocalID());
							if (textStreamHeads[h].pageId == pages[p].pageId) {
								ImRegion[] tshBlocks = pages[p].getRegionsIncluding(ImRegion.BLOCK_ANNOTATION_TYPE, textStreamHeads[h].bounds, true);
								BoundingBox tshAlignmentCheckBox = ((tshBlocks.length == 0) ? textStreamHeads[h].bounds : tshBlocks[0].bounds);
								if (pageImages[i].bounds.right < tshAlignmentCheckBox.left) {
									if (textStreamHeads[h].getPreviousWord() == null) {
										pm.setInfo("     ==> in block " + tshAlignmentCheckBox + " to right of image, and no predecessor");
										continue; // this one is to the right on same page, and it starts there
									}
									else {
										imagePredecessor  = textStreamHeads[h].getPreviousWord();
										pm.setInfo("     ==> in block " + tshAlignmentCheckBox + " to right of image, using predecessor " + imagePredecessor.getString() + " at " + imagePredecessor.getLocalID());
									}
								}
								else if ((pageImages[i].bounds.left < tshAlignmentCheckBox.right) && (pageImages[i].bounds.bottom < tshAlignmentCheckBox.top)) {
									if (textStreamHeads[h].getPreviousWord() == null) {
										pm.setInfo("     ==> in block " + tshAlignmentCheckBox + " below image, and no predecessor");
										continue; // this one is below on same page
									}
									else {
										imagePredecessor  = textStreamHeads[h].getPreviousWord();
										pm.setInfo("     ==> in block " + tshAlignmentCheckBox + " below image, using predecessor " + imagePredecessor.getString() + " at " + imagePredecessor.getLocalID());
									}
								}
								else {
									imagePredecessor = textStreamHeads[h];
									pm.setInfo("   - starting with " + imagePredecessor.getString() + " at " + imagePredecessor.getLocalID() + ", scanning towards image");
									BoundingBox nextImwAlignmentCheckBox = tshAlignmentCheckBox;
									while (imagePredecessor.getNextWord() != null) {
										ImWord nextImw = imagePredecessor.getNextWord();
										if (nextImw.pageId == pages[p].pageId) {
											if (!nextImwAlignmentCheckBox.includes(nextImw.bounds, true)) {
												ImRegion[] nextImwBlocks = pages[p].getRegionsIncluding(ImRegion.BLOCK_ANNOTATION_TYPE, nextImw.bounds, true);
												nextImwAlignmentCheckBox = ((nextImwBlocks.length == 0) ? nextImw.bounds : nextImwBlocks[0].bounds);
											}
											if (pageImages[i].bounds.right < nextImwAlignmentCheckBox.left) {
												pm.setInfo("     ==> stopped at " + imagePredecessor.getString() + " at " + imagePredecessor.getLocalID() + ", successor " + nextImw.getString() + " at " + nextImw.getLocalID() + " in block " + nextImwAlignmentCheckBox + " to right of image");
												break; // successor is to the right on same page, we're there
											}
											if ((pageImages[i].bounds.left < nextImwAlignmentCheckBox.right) && (pageImages[i].bounds.bottom < nextImwAlignmentCheckBox.top)) {
												pm.setInfo("     ==> stopped at " + imagePredecessor.getString() + " at " + imagePredecessor.getLocalID() + ", successor " + nextImw.getString() + " at " + nextImw.getLocalID() + " in block " + nextImwAlignmentCheckBox + " below image");
												break; // successor is below on same page, we're there
											}
										}
										else if (nextImw.pageId != pages[p].pageId) {
											pm.setInfo("     ==> stopped at " + imagePredecessor.getString() + " at " + imagePredecessor.getLocalID() + ", successor " + nextImw.getString() + " at " + nextImw.getLocalID() + " on different page");
											break; // successor is on subsequent page, we're there
										}
										imagePredecessor = imagePredecessor.getNextWord();
									}
								}
							}
							
							//	preceding page, follow text stream until it reaches or jumps over current page (cannot enter current page before image, we would have found that word above)
							else {
								imagePredecessor  = textStreamHeads[h];
								pm.setInfo("   - starting with " + imagePredecessor.getString() + " at " + imagePredecessor.getLocalID() + ", scanning towards image");
								while ((imagePredecessor.getNextWord() != null) && (imagePredecessor.getNextWord().pageId == pages[lp].pageId))
									imagePredecessor = imagePredecessor.getNextWord();
								pm.setInfo("     ==> stopped at " + imagePredecessor.getString() + " at " + imagePredecessor.getLocalID());
								if (imagePredecessor.getNextWord() == null)
									pm.setInfo("         reached end of text stream");
								else pm.setInfo("         successor " + imagePredecessor.getNextWord().getString() + " at " + imagePredecessor.getNextWord().getLocalID() + " below or to right of image, or on subsequent page");
							}
							if (imagePredecessor != null) 
								break; // no need to look any further
						}
						if (imagePredecessor != null) {
							imageSuccessor = imagePredecessor.getNextWord();
							pm.setInfo("   ==> found predecessor of " + imageFirstWord.getString() + " at " + imageFirstWord.getLocalID() + ": " + imagePredecessor.getString() + " at " + imagePredecessor.getLocalID());
							break; // no need to look any further
						}
					}
					if (imagePredecessor == null) {
						pm.setInfo(" - seeking successor for " + imageLastWord.getString() + " at " + imageLastWord.getLocalID());
						for (int lp = p; lp < pages.length; lp++) {
							ImWord[] textStreamHeads = pages[lp].getTextStreamHeads();
							for (int h = 0; h < textStreamHeads.length; h++) {
								if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(textStreamHeads[h].getTextStreamType()))
									continue; // we're only after main text
								if (!docMainTextStreamIDs.contains(textStreamHeads[h].getTextStreamId()))
									continue; // skip over parentheses, etc., which only run for one or two pages
								if ((lp == p) && (pageImages[i].bounds.includes(textStreamHeads[h].bounds, true)))
									continue; // no use checking the head of the labels we are trying to splice in
								
								//	we can simply use this one (would have found it as a predecessor above if it wasn't in the right position)
								imageSuccessor  = textStreamHeads[h];
								break;
							}
							if (imageSuccessor != null) {
								pm.setInfo("   ==> found successor of " + imageLastWord.getString() + " at " + imageLastWord.getLocalID() + ": " + imageSuccessor.getString() + " at " + imageSuccessor.getLocalID());
								break; // no need to look any further
							}
						}
					}
					
					//	splice text blocks into main text stream
					if (imagePredecessor != null) {
						pm.setInfo(" - found predecessor of " + imageFirstWord.getString() + " at " + imageFirstWord.getLocalID() + ": " + imagePredecessor.getString() + " at " + imagePredecessor.getLocalID());
						imagePredecessor.setNextWord(imageFirstWord);
						imagePredecessor.setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
					}
					if (imageSuccessor != null) {
						pm.setInfo(" - found successor of " + imageLastWord.getString() + " at " + imageLastWord.getLocalID() + ": " + imageSuccessor.getString() + " at " + imageSuccessor.getLocalID());
						imageLastWord.setNextWord(imageSuccessor);
						imageLastWord.setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
					}
				}
			}
		}
		
		//	detect artifacts (blocks with word coverage below 10%, only for scanned documents)
		else {
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
		CountingSet docFontSizes = new CountingSet(new TreeMap());
		for (int h = 0; h < textStreamHeads.length; h++) {
			if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(textStreamHeads[h].getTextStreamType()))
				continue;
			for (ImWord imw = textStreamHeads[h]; imw != null; imw = imw.getNextWord())
				if (imw.hasAttribute(ImWord.FONT_SIZE_ATTRIBUTE)) {
					docFontSizeSum += imw.getFontSize();
					docFontSizeWordCount++;
					docFontSizes.add(new Integer(imw.getFontSize()));
				}
		}
		final int docAvgFontSize = ((docFontSizeWordCount == 0) ? -1 : ((docFontSizeSum + (docFontSizeWordCount / 2)) / docFontSizeWordCount));
		final int docMainFontSize = (docFontSizes.isEmpty() ? -1 : ((Integer) docFontSizes.max()).intValue());
		spm.setInfo("Average font size is " + docAvgFontSize + " (based on " + docFontSizeWordCount + " main text words)");
		spm.setInfo("Font size distribution is " + docFontSizes);
		
		//	detect caption paragraphs
		//	TODO allow whole-block multi-paragraph captions via template parameter (default: false)
		spm.setStep(" - detecting captions");
		spm.setBaseProgress(45);
		spm.setMaxProgress(50);
		final HashMap[] pageCaptionsByBounds = new HashMap[pages.length];
		Arrays.fill(pageCaptionsByBounds, null);
		final boolean captionStartIsBold = docLayout.getBooleanProperty("caption.startIsBold", false);
		final boolean captionStartIsSeparateLine = docLayout.getBooleanProperty("caption.startIsSeparateLine", false);
		final boolean captionsInsideFigures = docLayout.getBooleanProperty("caption.insideFigure", true);
		final int captionFontSize = docLayout.getIntProperty("caption.fontSize", -1);
		final int captionMinFontSize = docLayout.getIntProperty("caption.minFontSize", ((captionFontSize == -1) ? 0 : captionFontSize));
		final int captionMaxFontSize = docLayout.getIntProperty("caption.maxFontSize", ((captionFontSize == -1) ? 72 : captionFontSize));
		String[] captionStartPatternStrs = docLayout.getStringListProperty("caption.startPatterns", null, " ");
		String[] figureCaptionStartPatternStrs = docLayout.getStringListProperty("caption.figureStartPatterns", null, " ");
		String[] tableCaptionStartPatternStrs = docLayout.getStringListProperty("caption.tableStartPatterns", null, " ");
		String[] plateCaptionStartPatternStrs = docLayout.getStringListProperty("caption.plateStartPatterns", null, " ");
		String[] platePartCaptionStartPatternStrs = docLayout.getStringListProperty("caption.platePartStartPatterns", null, " ");
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
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int pg) throws Exception {
				spm.setProgress((pg * 100) / pages.length);
				ImRegion[] pageBlocks = pages[pg].getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
				if (pageBlocks.length == 0)
					return;
				ImRegion[] pageParagraphs = pages[pg].getRegions(ImRegion.PARAGRAPH_TYPE);
				
				//	if we have captions _inside_ figures, mark coherent text blocks inside figures and set type to main text
				ArrayList insideImageMainTextWords = null;
				if (captionsInsideFigures) {
					insideImageMainTextWords = new ArrayList();
					this.prepareInsideImageWordBlocks(pages[pg], insideImageMainTextWords);
					if (insideImageMainTextWords.size() != 0) {
						pageBlocks = pages[pg].getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
						pageParagraphs = pages[pg].getRegions(ImRegion.PARAGRAPH_TYPE);
					}
				}
				
				//	mark captions
				for (int b = 0; b < pageBlocks.length; b++) {
					HashMap blockCaptionsByBounds = markBlockCaptions(doc, pageBlocks[b], pageParagraphs, captionStartPatterns, DocumentStructureDetectorProvider.this.captionStartPatterns, captionMinFontSize, captionMaxFontSize, captionStartIsBold, captionStartIsSeparateLine, spm);
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
				
				//	if we have captions _inside_ figures, set type of main text words in figures back to label (captions are type caption now)
				if (insideImageMainTextWords != null)
					for (int w = 0; w < insideImageMainTextWords.size(); w++) {
						ImWord imw = ((ImWord) insideImageMainTextWords.get(w));
						if (ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(imw.getTextStreamType()))
							imw.setTextStreamType(ImWord.TEXT_STREAM_TYPE_LABEL);
					}
			}
			
			//	TODOne TEST RecAustMus.67.6.185-199.pdf.imf (https://github.com/plazi/ggi/issues/90)
			private void prepareInsideImageWordBlocks(ImPage page, ArrayList insideImageWords) {
				ImRegion[] images = page.getRegions(ImRegion.IMAGE_TYPE);
				if (images.length == 0)
					return;
				for (int i = 0; i < images.length; i++) {
					ArrayList imageWords = new ArrayList(Arrays.asList(images[i].getWords()));
					if (imageWords.size() < 7)
						continue; // no caption below 7 words ...
					while (imageWords.size() != 0) {
						WordCluster wordCluster = new WordCluster((ImWord) imageWords.remove(0));
						for (boolean wordAdded = true; wordAdded;) {
							wordAdded = false;
							for (int w = 0; w < imageWords.size(); w++) {
								ImWord imw = ((ImWord) imageWords.get(w));
								if (wordCluster.overlaps(imw)) {
									wordCluster.addWord(imw);
									imageWords.remove(w--);
									wordAdded = true;
								}
							}
						}
						if (wordCluster.size() < 7)
							continue; // still no caption below 7 words ...
						ImWord[] clusterWords = wordCluster.getWords();
						ImUtils.sortLeftRightTopDown(clusterWords);
						ImUtils.makeStream(clusterWords, ImWord.TEXT_STREAM_TYPE_MAIN_TEXT, null);
						regionActionProvider.markBlock(page, ImLayoutObject.getAggregateBox(clusterWords));
						insideImageWords.addAll(Arrays.asList(clusterWords));
					}
				}
			}
			
			class WordCluster {
				ArrayList words = new ArrayList();
				int left;
				int right;
				int top;
				int bottom;
				int wordHeightSum = 0;
				int margin = 0;
				WordCluster(ImWord imw) {
					this.words.add(imw);
					this.left = imw.bounds.left;
					this.right = imw.bounds.right;
					this.top = imw.bounds.top;
					this.bottom = imw.bounds.bottom;
					this.wordHeightSum += imw.bounds.getHeight();
					this.margin = ((this.wordHeightSum + (this.words.size() / 2)) / this.words.size());
				}
				void addWord(ImWord imw) {
					this.words.add(imw);
					this.left = Math.min(this.left, imw.bounds.left);
					this.right = Math.max(this.right, imw.bounds.right);
					this.top = Math.min(this.top, imw.bounds.top);
					this.bottom = Math.max(this.bottom, imw.bounds.bottom);
					this.wordHeightSum += imw.bounds.getHeight();
					this.margin = ((this.wordHeightSum + (this.words.size() / 2)) / this.words.size());
				}
				boolean overlaps(ImWord imw) {
					if ((imw.bounds.right + this.margin) <= this.left)
						return false;
					if ((this.right + this.margin) <= imw.bounds.left)
						return false;
					if ((imw.bounds.bottom + this.margin) <= this.top)
						return false;
					if ((this.bottom + this.margin) <= imw.bounds.top)
						return false;
					return true;
				}
				int size() {
					return this.words.size();
				}
				ImWord[] getWords() {
					return ((ImWord[]) this.words.toArray(new ImWord[this.words.size()]));
				}
			}
		}, pages.length, (this.debug ? 1 : (pages.length / 8)));
		
		//	detect footnote paragraphs
		//	TODO maybe require footnotes to form separate block of their own
		spm.setStep(" - detecting footnotes");
		spm.setBaseProgress(50);
		spm.setMaxProgress(55);
		final boolean footnotesAreBlocks = docLayout.getBooleanProperty("footnote.separateBlock", false);
		final int footnoteFontSize = docLayout.getIntProperty("footnote.fontSize", -1);
		final int footnoteMinFontSize = docLayout.getIntProperty("footnote.minFontSize", ((footnoteFontSize == -1) ? 0 : footnoteFontSize));
		final int footnoteMaxFontSize = docLayout.getIntProperty("footnote.maxFontSize", ((footnoteFontSize == -1) ? 72 : footnoteFontSize));
		String[] footnoteStartPatternStrs = docLayout.getStringListProperty("footnote.startPatterns", null, " ");
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
				ImRegion[] pageBlocks = pages[pg].getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
				Arrays.sort(pageBlocks, Collections.reverseOrder(ImUtils.topDownOrder)); // sort bottom-up
				boolean[] nonFootnoteBelow = new boolean[pages[pg].bounds.right];
				Arrays.fill(nonFootnoteBelow, false);
				for (int b = 0; b < pageBlocks.length; b++) {
					if (!footnoteArea.overlaps(pageBlocks[b].bounds))
						continue;
					if (footnotesAreBlocks && !footnoteArea.includes(pageBlocks[b].bounds, false))
						continue;
					spm.setInfo("Testing paragraphs in " + pageBlocks[b].bounds + " on page " + pages[pg].pageId + " for footnotes");
					
					ImRegion[] blockParagraphs = pages[pg].getRegionsInside(ImRegion.PARAGRAPH_TYPE, pageBlocks[b].bounds, false);
					Arrays.sort(blockParagraphs, Collections.reverseOrder(ImUtils.topDownOrder)); // sort bottom-up
					for (int p = 0; p < blockParagraphs.length; p++) {
						if (!footnoteArea.includes(blockParagraphs[p].bounds, false))
							break; // no footnote to come above non-footnote (we're going through them paragraphs bottom-up)
						ImWord[] paragraphWords = getLargestTextStreamWords(blockParagraphs[p].getWords());
						if (paragraphWords.length == 0)
							continue;
						Arrays.sort(paragraphWords, ImUtils.textStreamOrder);
						if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(paragraphWords[0].getTextStreamType()))
							continue;
						spm.setInfo(" - testing paragraph " + blockParagraphs[p].bounds + " on page " + pages[pg].pageId + " for footnote");
						boolean onlyFootnotesBelow = true;
						for (int c =  pageBlocks[b].bounds.left; c < pageBlocks[b].bounds.right; c++)
							if (nonFootnoteBelow[c]) {
								onlyFootnotesBelow = false;
								spm.setInfo(" ==> found non-footnote below");
								break;
							}
						if (onlyFootnotesBelow && isFootnote(blockParagraphs[p], paragraphWords, docAvgFontSize, footnoteStartPatterns, footnoteMinFontSize, footnoteMaxFontSize)) {
							spm.setInfo(" ==> footnote detected");
							synchronized (doc) {
								ImUtils.makeStream(paragraphWords, ImWord.TEXT_STREAM_TYPE_FOOTNOTE, FOOTNOTE_TYPE);
								ImUtils.orderStream(paragraphWords, ImUtils.leftRightTopDownOrder);
							}
						}
						else {
							spm.setInfo(" ==> not a footnote");
							Arrays.fill(nonFootnoteBelow, pageBlocks[b].bounds.left, pageBlocks[b].bounds.right, true);
							break; // no footnote to come above non-footnote (we're going through them paragraphs bottom-up)
						}
					}
				}
			}
		}, pages.length, ((this.debug || DEBUG_IS_FOOTNOTE) ? 1 : (pages.length / 8)));
		
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
					if (blockFontSize >= docAvgFontSize) {
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
		
		//	re-merge words in super-stretched lines
		spm.setStep(" - repairing super-streched lines");
		spm.setBaseProgress(65);
		spm.setMaxProgress(68);
		Tokenizer tokenizer = ((Tokenizer) doc.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER));
		for (int pg = 0; pg < pages.length; pg++) {
			spm.setProgress((pg * 100) / pages.length);
			spm.setInfo("Checking paragraphs on page " + pages[pg].pageId);
			ImRegion[] pageBlocks = pages[pg].getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
			for (int b = 0; b < pageBlocks.length; b++) {
				
				//	get block metrics
				BlockMetrics blockMetrics = PageAnalysis.computeBlockMetrics(pageBlocks[b]);
				if (blockMetrics == null)
					continue; // let's not stumble over some weird artifact
				if (blockMetrics.lines.length < 5)
					continue; // too small a basis for reliable analysis
				
				//	compute block averages
				int blockWordCount = 0;
				int blockWordLengthSum = 0;
				int blockWordDistCount = 0;
				int blockWordDistSum = 0;
				CountingSet blockWordDists = new CountingSet(new TreeMap());
				int blockWordDistCountNs = 0;
				int blockWordDistSumNs = 0;
				CountingSet blockWordDistsNs = new CountingSet(new TreeMap());
				int blockWordDistCountSp = 0;
				int blockWordDistSumSp = 0;
				CountingSet blockWordDistsSp = new CountingSet(new TreeMap());
				int blockWordDistCountNsp = 0;
				int blockWordDistSumNsp = 0;
				CountingSet blockWordDistsNsp = new CountingSet(new TreeMap());
				for (int l = 0; l < blockMetrics.lines.length; l++) {
					LineMetrics lm = blockMetrics.lines[l];
					String wordString = lm.words[0].getString();
					if (Gamta.isWord(wordString) || Gamta.isNumber(wordString)) {
						blockWordCount++;
						blockWordLengthSum += wordString.length();
					}
					for (int w = 1; w < lm.words.length; w++) {
						wordString = lm.words[w].getString();
						if (Gamta.isWord(wordString) || Gamta.isNumber(wordString)) {
							blockWordCount++;
							blockWordLengthSum += wordString.length();
						}
						blockWordDistCount++;
						blockWordDistSum += (lm.words[w].bounds.left - lm.words[w-1].bounds.right);
						blockWordDists.add(new Integer(lm.words[w].bounds.left - lm.words[w-1].bounds.right));
						TokenSequence cWordTokens = tokenizer.tokenize(lm.words[w-1].getString() + lm.words[w].getString());
						if (cWordTokens.size() < 2) {
							blockWordDistCountNs++;
							blockWordDistSumNs += (lm.words[w].bounds.left - lm.words[w-1].bounds.right);
							blockWordDistsNs.add(new Integer(lm.words[w].bounds.left - lm.words[w-1].bounds.right));
						}
						else if (Gamta.insertSpace(lm.words[w-1].getString(), lm.words[w].getString())) {
							blockWordDistCountSp++;
							blockWordDistSumSp += (lm.words[w].bounds.left - lm.words[w-1].bounds.right);
							blockWordDistsSp.add(new Integer(lm.words[w].bounds.left - lm.words[w-1].bounds.right));
						}
						else {
							blockWordDistCountNsp++;
							blockWordDistSumNsp += (lm.words[w].bounds.left - lm.words[w-1].bounds.right);
							blockWordDistsNsp.add(new Integer(lm.words[w].bounds.left - lm.words[w-1].bounds.right));
						}
					}
				}
				if ((blockWordCount == 0) || (blockWordDistCount == 0))
					continue; // nothing to work with here
				int blockAvgWordLength = ((blockWordLengthSum + (blockWordCount / 2)) / blockWordCount);
				int blockAvgWordDist = ((blockWordDistSum + (blockWordDistCount / 2)) / blockWordDistCount);
				int blockAvgWordDistNs = ((blockWordDistCountNs == 0) ? -1 : ((blockWordDistSumNs + (blockWordDistCountNs / 2)) / blockWordDistCountNs));
				int blockAvgWordDistSp = ((blockWordDistCountSp == 0) ? -1 : ((blockWordDistSumSp + (blockWordDistCountSp / 2)) / blockWordDistCountSp));
				int blockAvgWordDistNsp = ((blockWordDistCountNsp == 0) ? -1 : ((blockWordDistSumNsp + (blockWordDistCountNsp / 2)) / blockWordDistCountNsp));
				spm.setInfo(" - average word length in block is " + blockAvgWordLength + " (" + blockWordLengthSum + "/" + blockWordCount + ")");
				spm.setInfo(" - average word distance in block is " + blockAvgWordDist + " (" + blockWordDistSum + "/" + blockWordDistCount + ")");
				spm.setInfo(" - word distances in block are " + blockWordDists);
				spm.setInfo(" - average non-separating word distance in block is " + blockAvgWordDistNs + " (" + blockWordDistSumNs + "/" + blockWordDistCountNs + ")");
				spm.setInfo(" - non-separating word distances in block are " + blockWordDistsNs);
				spm.setInfo(" - average spaced word distance in block is " + blockAvgWordDistSp + " (" + blockWordDistSumSp + "/" + blockWordDistCountSp + ")");
				spm.setInfo(" - spaced word distances in block are " + blockWordDistsSp);
				spm.setInfo(" - average non-spaced word distance in block is " + blockAvgWordDistNsp + " (" + blockWordDistSumNsp + "/" + blockWordDistCountNsp + ")");
				spm.setInfo(" - non-spaced word distances in block are " + blockWordDistsNsp);
				
				//	work through lines
				for (int l = 0; l < blockMetrics.lines.length; l++) {
					LineMetrics lm = blockMetrics.lines[l];
					if (lm.words.length < 2)
						continue; // nothing to work with here
					spm.setInfo(" - checking line " + ImUtils.getString(lm.words, true));
					int lineWordCount = 0;
					int lineWordLengthSum = 0;
					int lineMinWordLength = Integer.MAX_VALUE;
					int lineMaxWordLength = 0;
					int lineWordDistCount = 0;
					int lineWordDistSum = 0;
					CountingSet lineWordDists = new CountingSet(new TreeMap());
					int nonSeparatingWordPairCount = 0;
//					int lineWordDistCountNs = 0;
//					int lineWordDistSumNs = 0;
//					CountingSet lineWordDistsNs = new CountingSet(new TreeMap());
//					int lineWordDistCountSp = 0;
//					int lineWordDistSumSp = 0;
//					CountingSet lineWordDistsSp = new CountingSet(new TreeMap());
//					int lineWordDistCountNsp = 0;
//					int lineWordDistSumNsp = 0;
//					CountingSet lineWordDistsNsp = new CountingSet(new TreeMap());
					String wordString = lm.words[0].getString();
					if (Gamta.isWord(wordString) || Gamta.isNumber(wordString)) {
						lineWordCount++;
						lineWordLengthSum += wordString.length();
						lineMinWordLength = Math.min(lineMinWordLength, wordString.length());
						lineMaxWordLength = Math.max(lineMaxWordLength, wordString.length());
					}
					for (int w = 1; w < lm.words.length; w++) {
						wordString = lm.words[w].getString();
						if (Gamta.isWord(wordString) || Gamta.isNumber(wordString)) {
							lineWordCount++;
							lineWordLengthSum += wordString.length();
							lineMinWordLength = Math.min(lineMinWordLength, wordString.length());
							lineMaxWordLength = Math.max(lineMaxWordLength, wordString.length());
						}
						lineWordDistCount++;
						lineWordDistSum += (lm.words[w].bounds.left - lm.words[w-1].bounds.right);
						lineWordDists.add(new Integer(lm.words[w].bounds.left - lm.words[w-1].bounds.right));
						TokenSequence cWordTokens = tokenizer.tokenize(lm.words[w-1].getString() + lm.words[w].getString());
						if (cWordTokens.size() < 2) {
							nonSeparatingWordPairCount++;
//							lineWordDistCountNs++;
//							lineWordDistSumNs += (lm.words[w].bounds.left - lm.words[w-1].bounds.right);
//							lineWordDistsNs.add(new Integer(lm.words[w].bounds.left - lm.words[w-1].bounds.right));
						}
						else if (Gamta.insertSpace(lm.words[w-1].getString(), lm.words[w].getString())) {
//							lineWordDistCountSp++;
//							lineWordDistSumSp += (lm.words[w].bounds.left - lm.words[w-1].bounds.right);
//							lineWordDistsSp.add(new Integer(lm.words[w].bounds.left - lm.words[w-1].bounds.right));
						}
						else {
//							lineWordDistCountNsp++;
//							lineWordDistSumNsp += (lm.words[w].bounds.left - lm.words[w-1].bounds.right);
//							lineWordDistsNsp.add(new Integer(lm.words[w].bounds.left - lm.words[w-1].bounds.right));
						}
					}
					if ((lineWordCount == 0) || (lineWordDistCount == 0))
						continue; // nothing to work with here
					int lineAvgWordLength = ((lineWordLengthSum + (lineWordCount / 2)) / lineWordCount);
					int lineAvgWordDist = ((lineWordDistSum + (lineWordDistCount / 2)) / lineWordDistCount);
//					int lineAvgWordDistNs = ((lineWordDistCountNs == 0) ? -1 : ((lineWordDistSumNs + (lineWordDistCountNs / 2)) / lineWordDistCountNs));
//					int lineAvgWordDistSp = ((lineWordDistCountSp == 0) ? bAvgWordDistSp : ((lineWordDistSumSp + (lineWordDistCountSp / 2)) / lineWordDistCountSp));
//					int lineAvgWordDistNsp = ((lineWordDistCountNsp == 0) ? bAvgWordDistNsp : ((lineWordDistSumNsp + (lineWordDistCountNsp / 2)) / lineWordDistCountNsp));
					spm.setInfo("   - average word length is " + lineAvgWordLength + " (" + lineWordLengthSum + "/" + lineWordCount + "), minimum is " + lineMinWordLength + ", maximum is " + lineMaxWordLength);
					spm.setInfo("   - average word distance is " + lineAvgWordDist + " (" + lineWordDistSum + "/" + lineWordDistCount + ")");
					spm.setInfo("   - word distances are " + lineWordDists);
//					spm.setInfo("   - average non-separating word distance is " + lineAvgWordDistNs + " (" + lineWordDistSumNs + "/" + lineWordDistCountNs + ")");
//					spm.setInfo("   - non-separating word distances are " + lineWordDistsNs);
//					spm.setInfo("   - average spaced word distance is " + lineAvgWordDistSp + " (" + lineWordDistSumSp + "/" + lineWordDistCountSp + ")");
//					spm.setInfo("   - spaced word distances are " + lineWordDistsSp);
//					spm.setInfo("   - average non-spaced word distance is " + lineAvgWordDistNsp + " (" + lineWordDistSumNsp + "/" + lineWordDistCountNsp + ")");
//					spm.setInfo("   - non-spaced word distances are " + lineWordDistsNsp);
					if (lineAvgWordLength > 2) {
						spm.setInfo("   ==> words too long to be super-stretched (average)");
						continue; // this one looks fine
					}
					if (lineMaxWordLength > 2) {
						spm.setInfo("   ==> words too long to be super-stretched (maximum)");
						continue; // this one looks fine
					}
					if (nonSeparatingWordPairCount == -1) {
						spm.setInfo("   ==> no words or numbers separated by spaces");
						continue; // nothing to check at all
					}
					
					//	get range of word distance distribution
					int lineMinWordDist = ((Integer) lineWordDists.first()).intValue();
					int lineMaxWordDist = ((Integer) lineWordDists.last()).intValue();
					spm.setInfo("   - minimum word distance is " + lineMinWordDist + ", maximum is " + lineMaxWordDist);
					
					//	check for uniform space below half of block average (URL lines, which have no spaces)
					if ((lineMaxWordDist - lineMinWordDist) < (blockAvgWordDistSp - blockAvgWordDistNsp)) {
						spm.setInfo("   --> checking for space-free line");
						if (lineMinWordDist <= blockAvgWordDistNsp) {
							spm.setInfo("   ==> too small minimum word distance");
							continue; // in super-stretched line, no space would stay below block average
						}
					}
					
					//	check for large gap in distribution of non-separating word distances (multi-word lines)
					else {
						spm.setInfo("   --> seeking split in word distance distribution around maximum gap");
						
						//	find actual gap in word distance distribution
						int splitMinWordDist = lineAvgWordDist;
						int splitMinWordDistScore = 1;
						for (Iterator spit = lineWordDists.iterator(); spit.hasNext();) {
							Integer wordDist = ((Integer) spit.next());
							if (wordDist.intValue() < splitMinWordDist)
								continue; // only seek from average upwards
							if (wordDist.intValue() == lineMaxWordDist)
								break; // no use seeking beyond maximum ...
							int wordDistScore = 0;
							for (int wdl = (wordDist.intValue() + 1);; wdl++) {
								if (lineWordDists.contains(new Integer(wdl)))
									break;
								wordDistScore++;
							}
							spm.setInfo("     - score for gap above " + wordDist + " is " + wordDistScore);
							if (splitMinWordDistScore < wordDistScore) {
								spm.setInfo("     ==> new best split pivot");
								splitMinWordDist = wordDist.intValue();
								splitMinWordDistScore = wordDistScore;
							}
						}
						
						//	split word distance distribution
						spm.setInfo("     --> splitting word distance distribution around gap above " + splitMinWordDist);
						int belowAvgWordDistCount = 0;
						int maxBelowAvgWordDist = 0;
						int aboveAvgWordDistCount = 0;
						int minAboveAvgWordDist = Integer.MAX_VALUE;
						for (Iterator spit = lineWordDists.iterator(); spit.hasNext();) {
							Integer wordDist = ((Integer) spit.next());
							if (wordDist.intValue() <= splitMinWordDist) {
								belowAvgWordDistCount += lineWordDists.getCount(wordDist);
								maxBelowAvgWordDist = Math.max(maxBelowAvgWordDist, wordDist.intValue());
							}
							else {
								aboveAvgWordDistCount += lineWordDists.getCount(wordDist);
								minAboveAvgWordDist = Math.min(minAboveAvgWordDist, wordDist.intValue());
							}
						}
						spm.setInfo("   - got " + belowAvgWordDistCount + " word distances below " + splitMinWordDist + ", max is " + maxBelowAvgWordDist);
						spm.setInfo("   - got " + aboveAvgWordDistCount + " word distances above " + splitMinWordDist + ", min is " + minAboveAvgWordDist);
						spm.setInfo("   --> gap is " + (minAboveAvgWordDist - maxBelowAvgWordDist) + ", block average is " + (blockAvgWordDistSp - blockAvgWordDistNsp));
						if ((minAboveAvgWordDist - maxBelowAvgWordDist) <= (blockAvgWordDistSp - blockAvgWordDistNsp)) {
							spm.setInfo("   ==> no distinctive gap in word distances");
							continue; // in super-stretched line, actual space would be increased as well, bringing proportion well beyond block average
						}
					}
					spm.setInfo("   ==> found super-stretched line, joining words");
					
					//	concatenate line and line up words
					StringBuffer lineString = new StringBuffer();
					ArrayList wordAtOffset = new ArrayList();
					for (int w = 0; w < lm.words.length; w++) {
						if ((w != 0) && (blockAvgWordDistSp < (lm.words[w].bounds.left - lm.words[w-1].bounds.right))) /* add space if distance above block average space */ {
							lineString.append(' ');
							wordAtOffset.add(null);
						}
						wordString = lm.words[w].getString();
						for (int c = 0; c < wordString.length(); c++) {
							lineString.append(wordString.charAt(c));
							wordAtOffset.add(lm.words[w]);
						}
					}
					spm.setInfo("   - line string is " + lineString);
					
					//	re-tokenize line and line up tokens
					TokenSequence lineTokens = tokenizer.tokenize(lineString);
					ArrayList tokenAtOffset = new ArrayList();
					for (int t = 0; t < lineTokens.size(); t++) {
						while (tokenAtOffset.size() < lineTokens.tokenAt(t).getStartOffset())
							tokenAtOffset.add(null);
						String tokenString = lineTokens.valueAt(t);
						for (int c = 0; c < tokenString.length(); c++)
							tokenAtOffset.add(lineTokens.tokenAt(t));
					}
					
					//	join words that ended up in same token
					for (int o = 1; o < wordAtOffset.size(); o++) {
						if (wordAtOffset.get(o-1) == null)
							continue; // after significant space
						if (wordAtOffset.get(o) == null)
							continue; // on significant space
						if (wordAtOffset.get(o-1) == wordAtOffset.get(o))
							continue; // same word
						if (tokenAtOffset.get(o-1) != tokenAtOffset.get(o))
							continue; // tokenized apart
						((ImWord) wordAtOffset.get(o-1)).setNextRelation(ImWord.NEXT_RELATION_CONTINUE); // join words
					}
					spm.setInfo("   ==> line words joined to " + ImUtils.getString(lm.words, true));
//					spm.setInfo("   ==> line words joined to " + ImUtils.getString(lm.words, true, blockAvgWordDistSp));
					//doc.addAnnotation(lm.words[0], lm.words[lm.words.length-1], "superStretchedLine"); // TODO activate this line for debugging
				}
			}
		}
		
		//	de-hyphenate line breaks within paragraphs
		spm.setStep(" - de-hyphenating line breaks");
		spm.setBaseProgress(68);
		spm.setMaxProgress(72);
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
		spm.setBaseProgress(72);
		spm.setMaxProgress(75);
		final ImDocumentStyle blockStyle = docLayout.getImSubset("block");
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
				BlockMetrics nextImwBlockMetrics = PageAnalysis.computeBlockMetrics(nextImwBlock);
				if (nextImwBlockMetrics == null) {
					System.out.println(" ==> no lines to continue with");
					continue; // happens if there are no lines at all
				}
				BlockLayout imwBlockLayout = imwBlockMetrics.analyze(blockStyle);
				BlockLayout nextImwBlockLayout = nextImwBlockMetrics.analyze(blockStyle);
				
				//	no chance of flowing text continuing across blocks with incompatible style
				if (!PageAnalysis.areContinuousStyle(imwBlockLayout, nextImwBlockLayout)) {
					System.out.println(" ==> style mismatch");
					continue;
				}
				
				//	analyze blocks as one
				int imwBlockPargraphMargin = ((imwBlockLayout.paragraphDistance < 0) ? imwBlockMetrics.avgAboveLineGap : (imwBlockLayout.paragraphDistance - 1));
				int nextImwBlockPargraphMargin = ((nextImwBlockLayout.paragraphDistance < 0) ? nextImwBlockMetrics.avgAboveLineGap : (nextImwBlockLayout.paragraphDistance - 1));
				int blockMargin = Math.min(((imwBlockPargraphMargin < 0) ? Short.MAX_VALUE : imwBlockPargraphMargin), ((nextImwBlockPargraphMargin < 0) ? Integer.MAX_VALUE : nextImwBlockPargraphMargin));
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
		spm.setBaseProgress(75);
		spm.setMaxProgress(80);
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
				HashMap aroundCaptionTargets = new HashMap();
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
					if (isTableCaption ? false : captionsInsideFigures) {
						ImRegion aroundCaptionTarget = getAroundCaptionTarget(pages[p], captionBounds, pageImageDpi, isTableCaption);
						if (aroundCaptionTarget != null)
							aroundCaptionTargets.put(captionBounds, aroundCaptionTarget);
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
						ImRegion rct = ((ImRegion) aroundCaptionTargets.get(captionBounds[c]));
						if (rct != null) {
							for (int a = 0; a < assignedCaptionTargets.size(); a++)
								if (((ImRegion) assignedCaptionTargets.get(a)).bounds.overlaps(rct.bounds)) {
									 aroundCaptionTargets.remove(captionBounds[c]);
									 rct = null;
									 break;
								}
						}
						
						//	if a caption lies inside a target, association is rather clear
						if (rct != null) {
							act = null;
							bct = null;
							sct = null;
						}
						
						//	count targets
						int ctCount = (((act == null) ? 0 : 1) + ((bct == null) ? 0 : 1) + ((sct == null) ? 0 : 1) + ((rct == null) ? 0 : 1));
						
						//	no target found or left for this one
						if (ctCount == 0) {
							pageCaptionsByBounds[p].remove(captionBounds[c]);
							continue;
						}
						
						//	this one's ambiguous, save for another round
						if (skipAmbiguousCaptions && (ctCount != 1))
							continue;
						
						//	get target area (prefer around over above, above over below, and below over beside)
						ImRegion ct = ((rct == null) ? ((act == null) ? ((bct == null) ? sct : bct) : act) : rct);
						
						//	get annotation and check target type
						ImAnnotation captionAnnot = ((ImAnnotation) pageCaptionsByBounds[p].get(captionBounds[c]));
						
						//	mark table caption
						if (ImRegion.TABLE_TYPE.equals(ct.getType())) {
							captionAnnot.setAttribute(ImAnnotation.CAPTION_TARGET_IS_TABLE_ATTRIBUTE);
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
		spm.setBaseProgress(80);
		spm.setMaxProgress(82);
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
		spm.setBaseProgress(82);
		spm.setMaxProgress(83);
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
		spm.setBaseProgress(83);
		spm.setMaxProgress(85);
		for (int t = 0; t < docTables.length; t++) {
			spm.setProgress((t * 100) / docTables.length);
			ImAnnotation tableCaption = ((ImAnnotation) tablesToCaptionAnnots.get(docTables[t]));
			spm.setInfo("Attempting to column-merge table " + docTables[t].bounds + " on page " + docTables[t].pageId);
			String tableCaptionStart = ((tableCaption == null) ? null : getCaptionStartForCheck(tableCaption));
			spm.setInfo(" - caption start is " + ((tableCaptionStart == null) ? "null" : ("'" + tableCaptionStart + "'")));
			ImRegion[] tableGridRow = ImUtils.getRowConnectedTables(docTables[t]);
			spm.setInfo(" - got " + tableGridRow.length + " row-connected tables");
			for (int ct = (t+1); ct < docTables.length; ct++) {
				spm.setInfo(" - comparing to table " + docTables[ct].bounds + " on page " + docTables[ct].pageId);
				if (docTables[ct].pageId == docTables[t].pageId) {
					spm.setInfo(" --> same page");
					break;
				}
				if (!ImUtils.areTableColumnsCompatible(docTables[t], docTables[ct])) {
					spm.setInfo(" --> columns not compatible");
					continue;
				}
				ImAnnotation cTableCaption = ((ImAnnotation) tablesToCaptionAnnots.get(docTables[ct]));
				String cTableCaptionStart = ((cTableCaption == null) ? null : getCaptionStartForCheck(cTableCaption));
				if ((tableCaptionStart != null) && (cTableCaptionStart != null) && !tableCaptionStart.equalsIgnoreCase(cTableCaptionStart)) {
					spm.setInfo(" --> caption start '" + cTableCaptionStart + "' not compatible");
					continue;
				}
				ImRegion[] cTableGridRow = ImUtils.getRowConnectedTables(docTables[ct]);
				if (tableGridRow.length != cTableGridRow.length) {
					spm.setInfo(" --> row-connected tables not compatible");
					continue;
				}
				ImUtils.connectTableColumns(docTables[t], docTables[ct]);
				spm.setInfo(" --> table columns merged");
				break;
			}
		}
		
		//	tag and connect caption citations
		spm.setStep(" - marking caption citations");
		spm.setBaseProgress(85);
		spm.setMaxProgress(90);
		this.captionCitationHandler.markContinuationCaptions(doc);
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
						Arrays.sort(lineWords, ImUtils.textStreamOrder);
						
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
								else if (fs < minMainFontSize) {
									if (w == 0) {}
									else if (lineWords[w].getPreviousWord() == null) {}
									else if (lineWords[w].getPreviousRelation() != ImWord.NEXT_RELATION_CONTINUE) {}
									else if ((smallLineWords.size() != 0) && (smallLineWords.get(smallLineWords.size()-1) == lineWords[w].getPreviousWord())) {}
									else if (!StringUtils.normalizeString(lineWords[w].getString()).matches("[A-Z\\-]+")) {}
									else if (!StringUtils.normalizeString(lineWords[w].getPreviousWord().getString()).matches("[A-Z]")) {}
									else continue; // continuing emulated small-caps from non-small predecessor
									if (w == 0) {}
									else if (lineWords[w].getPreviousWord() == null) {}
									else if (lineWords[w].getPreviousRelation() != ImWord.NEXT_RELATION_SEPARATE) {}
									else if (lineWords[w].getFontSize() < lineWords[w].getPreviousWord().getFontSize()) {}
									else if ((smallLineWords.size() != 0) && (smallLineWords.get(smallLineWords.size()-1) == lineWords[w].getPreviousWord())) {}
									else if (!StringUtils.normalizeString(lineWords[w].getString()).matches("[A-Z\\-]+")) {}
									else if (!StringUtils.normalizeString(lineWords[w].getPreviousWord().getString()).matches("[A-Z\\-]+")) {}
									else continue; // second word in emulated small-caps, with non-small predecessor of same font size
									smallLineWords.add(lineWords[w]);
								}
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
				
				//	TODO also observe superscript and subscript Unicode blocks
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
		}, pages.length, (this.debug ? 1 : (pages.length / 8)));
		
		//	clean table markup after marking emphases
		for (int t = 0; t < docTables.length; t++)
			this.tableActionProvider.cleanupTableAnnotations(doc, docTables[t]);
		
		//	get heading styles
		HeadingStyleDefined[] headingStyles = null;
		DocumentStyle headingStyle = docStyle.getSubset("style.heading");
//		int[] headingLevels = headingStyle.getIntListProperty("levels", null);
//		if (headingLevels != null) {
//			headingStyles = new HeadingStyleDefined[headingLevels.length];
//			for (int l = 0; l < headingLevels.length; l++)
//				headingStyles[l] = new HeadingStyleDefined(headingLevels[l], headingStyle);
//		}
//		int[] headingGroups = headingStyle.getIntListProperty("groups", null);
//		if (headingGroups != null) {
//			headingStyles = new HeadingStyleDefined[headingGroups.length];
//			for (int g = 0; g < headingGroups.length; g++)
//				headingStyles[g] = new HeadingStyleDefined(headingGroups[g], headingStyle);
//		}
		ArrayList definedHeadingStyles = new ArrayList();
		for (int g = 1; g <= 9; g++) {
			HeadingStyleDefined hsd = HeadingStyleDefined.getHeadingStyle(g, headingStyle);
			if (hsd != null)
				definedHeadingStyles.add(hsd);
		}
		if (definedHeadingStyles.size() != 0)
			headingStyles = ((HeadingStyleDefined[]) definedHeadingStyles.toArray(new HeadingStyleDefined[definedHeadingStyles.size()]));
		
		//	extract headings using heuristics
		if (headingStyles == null) {
			
			//	mark headings
			ArrayList headings = new ArrayList();
			for (int pg = 0; pg < pages.length; pg++) {
				spm.setProgress((pg * 100) / pages.length);
				this.markHeadings(pages[pg], docAvgFontSize, docMainFontSize, pageImageDpi, emphasisWords, spm, headings);
			}
			
			//	assess hierarchy of headings
			spm.setStep(" - assessing hierarchy of headings");
			this.assessHeadingHierarchy(headings, docAvgFontSize, pages[0].pageId, spm);
		}
		
		//	extract headings using style templates
		else for (int pg = 0; pg < pages.length; pg++) {
			spm.setProgress((pg * 100) / pages.length);
			markHeadings(pages[pg], headingStyles, spm);
		}
		
		//	mark in-line headings if we have defined styles
		DocumentStyle inLineHeadingStyle = headingStyle.getSubset("inLine");
		ArrayList inLineHeadingStyles = new ArrayList();
		for (int g = 1; g <= 3; g++) {
			InLineHeadingStyle ilhs = InLineHeadingStyle.getInLineHeadingStyle(g, inLineHeadingStyle);
			if (ilhs != null)
				inLineHeadingStyles.add(ilhs);
		}
		if (inLineHeadingStyles.size() != 0) {
			InLineHeadingStyle[] ilhss = ((InLineHeadingStyle[]) inLineHeadingStyles.toArray(new InLineHeadingStyle[inLineHeadingStyles.size()]));
			for (int pg = 0; pg < pages.length; pg++) {
				spm.setProgress((pg * 100) / pages.length);
				markInLineHeadings(pages[pg], ilhss, spm);
			}
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
	private static final String fontSizeVariablePunctuationMarks = (",.;:^°\"'=+-\u00B1" + StringUtils.DASHES);
	
	/* string of punctuation marks whose font style (bold, italics) can differ
	 * from the surrounding text due to sloppy layout or even adjustment */
	private static final String fontStyleVariablePunctuationMarks = (",.°'=+-\u00B1" + StringUtils.DASHES);
	
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
		final int group;
		final int level;
		String lineBreakMergeMode;
		int minFontSize;
		int maxFontSize;
		int maxLineCount;
		boolean isBlock;
		boolean isBlockAligned;
		boolean isBold;
		boolean startIsBold;
		boolean containsBold;
		boolean isItalics;
		boolean startIsItalics;
		boolean containsItalics;
		boolean isAllCaps;
		boolean startIsAllCaps;
		boolean containsAllCaps;
		boolean isSmallCaps;
		boolean startIsSmallCaps;
		boolean containsSmallCaps;
		boolean ignoreParentheses;
		Pattern[] startPatterns;
		Pattern[] filterPatterns;
		boolean isLeftAligned;
		boolean isCenterAligned;
		boolean isRightAligned;
		boolean canHaveIndent;
		boolean isCenterStrict;
		boolean splitOffBlock;
		
		private HeadingStyleDefined(int group, DocumentStyle style) {
			this.group = group;
			this.level = style.getIntProperty((this.group + ".level"), this.group);
			this.lineBreakMergeMode = style.getStringProperty((this.group + ".lineBreakMergeMode"), "never");
			int fontSize = style.getIntProperty((this.group + ".fontSize"), -1);
			this.minFontSize = style.getIntProperty((this.group + ".minFontSize"), ((fontSize == -1) ? 0 : fontSize));
			this.maxFontSize = style.getIntProperty((this.group + ".maxFontSize"), ((fontSize == -1) ? 72 : fontSize));
			this.maxLineCount = style.getIntProperty((this.group + ".maxLineCount"), 0);
			this.isBlock = !style.getBooleanProperty((this.group + ".isNonBlock"), false); // we need to use inversion here, as styles save only 'true'
			this.isBlockAligned = style.getBooleanProperty((this.group + ".alignmentInBlock"), false);
			this.isBold = style.getBooleanProperty((this.group + ".isBold"), false);
			this.startIsBold = style.getBooleanProperty((this.group + ".startIsBold"), false);
			this.containsBold = style.getBooleanProperty((this.group + ".containsBold"), false);
			this.isItalics = style.getBooleanProperty((this.group + ".isItalics"), false);
			this.startIsItalics = style.getBooleanProperty((this.group + ".startIsItalics"), false);
			this.containsItalics = style.getBooleanProperty((this.group + ".containsItalics"), false);
			this.isAllCaps = style.getBooleanProperty((this.group + ".isAllCaps"), false);
			this.startIsAllCaps = style.getBooleanProperty((this.group + ".startIsAllCaps"), false);
			this.containsAllCaps = style.getBooleanProperty((this.group + ".containsAllCaps"), false);
			this.isSmallCaps = style.getBooleanProperty((this.group + ".isSmallCaps"), false);
			this.startIsSmallCaps = style.getBooleanProperty((this.group + ".startIsSmallCaps"), false);
			this.containsSmallCaps = style.getBooleanProperty((this.group + ".containsSmallCaps"), false);
			this.ignoreParentheses = style.getBooleanProperty((this.group + ".ignoreParentheses"), false);
			this.startPatterns = getPatterns(style.getStringListProperty((this.group + ".startPatterns"), null, " "));
			this.filterPatterns = getPatterns(style.getStringListProperty((this.group + ".filterPatterns"), null, " "));
			String alignment = style.getStringProperty((this.group + ".alignment"), "");
			this.isLeftAligned = (ImAnnotation.TEXT_ORIENTATION_LEFT.equals(alignment) || ImAnnotation.TEXT_ORIENTATION_JUSTIFIED.equals(alignment));
			this.isCenterAligned = (ImAnnotation.TEXT_ORIENTATION_CENTERED.equals(alignment) || TEXT_ORIENTATION_CENTERED_STRICT.equals(alignment));
			this.isRightAligned = (ImAnnotation.TEXT_ORIENTATION_RIGHT.equals(alignment) || ImAnnotation.TEXT_ORIENTATION_JUSTIFIED.equals(alignment));
			this.canHaveIndent = style.getBooleanProperty((this.group + ".canHaveIndent"), false);
			this.isCenterStrict = TEXT_ORIENTATION_CENTERED_STRICT.equals(alignment);
			this.splitOffBlock = style.getBooleanProperty((this.group + ".splitOffBlock"), false);
		}
		
		boolean matches(ImPage page, ImRegion line, ImWord[] lineWords, int blockLinePos, int blockLineCount, int leftColDist, int rightColDist, int leftBlockDist, int rightBlockDist, PrintStream log) {
			log.println("Mathing against line " + line.bounds);
			
			//	select how to measure alignment
			int leftDist = ((!this.isBlock && this.isBlockAligned) ? leftBlockDist : leftColDist);
			int rightDist = ((!this.isBlock && this.isBlockAligned) ? rightBlockDist : rightColDist);
			
			//	add some tolerance for indent (up to half an inch, some 12mm)
			if (this.isLeftAligned && this.canHaveIndent && (blockLinePos == 0) && ((leftDist * 2) < page.getImageDPI())) {
				if (leftDist != 0)
					log.println(" - ignoring indent left edge distance " + leftDist);
				leftDist = 0;
			}
			
			//	compute orientation
			log.println(" - checking orientation for " + ((!this.isBlock && this.isBlockAligned) ? "block" : "column") + " edge distances " + leftDist + " and " + rightDist);
			boolean isFlushLeft = ((leftDist * 25) < page.getImageDPI());
			log.println("   - left aligned: " + isFlushLeft);
			boolean isFlushRight = ((rightDist * 25) < page.getImageDPI());
			log.println("   - right aligned: " + isFlushRight);
			boolean isCentered = ((Math.abs(leftDist - rightDist) * 8) < page.getImageDPI());
			log.println("   - center aligned: " + isCentered);
			
			//	check alignment first
			if (this.isCenterStrict && (isFlushLeft || isFlushRight)) {
				log.println(" ==> mismatch on strictly-centered orientation");
				return false;
			}
			if (this.isLeftAligned && !isFlushLeft) {
				log.println(" ==> mismatch on left block edge");
				return false;
			}
			if (this.isRightAligned && !isFlushRight) {
				log.println(" ==> mismatch on right block edge");
				return false;
			}
			if (this.isCenterAligned && !isCentered) {
				log.println(" ==> mismatch on centered orientation");
				return false;
			}
			log.println(" - orientation match");
			
			//	check line count and position
			if (this.maxLineCount > 0) {
				log.println(" - checking number of lines");
				
				//	headings are in their own blocks, check block size
				if (this.isBlock) {
					if (blockLineCount > this.maxLineCount) {
						log.println(" ==> mismatch on maximum of " + this.maxLineCount + " lines in block, got " + blockLineCount);
						return false;
					}
				}
				
				//	headings sit atop a text block, all we can check is line position
				else {
					if (blockLinePos >= this.maxLineCount) {
						log.println(" ==> mismatch on maximum line position of " + this.maxLineCount + ", at " + blockLinePos);
						return false;
					}
				}
				log.println(" - line maximum match");
			}
			
			//	check start style
			if ((this.isBold || this.startIsBold) && !lineWords[0].hasAttribute(ImWord.BOLD_ATTRIBUTE)) {
				log.println(" ==> expected bold start not found");
				return false;
			}
			if ((this.isItalics || this.startIsItalics) && !lineWords[0].hasAttribute(ImWord.ITALICS_ATTRIBUTE)) {
				log.println(" ==> expected italics start not found");
				return false;
			}
			if ((this.isAllCaps || this.startIsAllCaps || this.isSmallCaps || this.startIsSmallCaps) && !lineWords[0].getString().equals(lineWords[0].getString().toUpperCase())) {
				log.println(" ==> expected all-caps or small-caps start not found");
				return false;
			}
			log.println(" - start style match");
			
			//	concatenate words, checking style along the way
			log.println(" - checking font style");
			StringBuffer lineWordString = new StringBuffer();
			boolean gotBold = false;
			boolean gotItalics = false;
			boolean gotAllCaps = false;
			boolean gotSmallCaps = false;
			int capsMinFontSize = 72;
			int capsMaxFontSize = 0;
			LinkedList openBrackets = new LinkedList();
			for (int w = 0; w < lineWords.length; w++) {
				String wordStr = lineWords[w].getString();
				if (wordStr == null)
					continue;
				wordStr = StringUtils.normalizeString(wordStr); // normalize string, like all other pattern editing does
				if (this.ignoreParentheses && Gamta.isOpeningBracket(wordStr))
					openBrackets.addLast(wordStr);
				if (openBrackets.isEmpty()) {
					if (this.isBold && !lineWords[w].hasAttribute(ImWord.BOLD_ATTRIBUTE) && ((wordStr.length() > 1) || (fontStyleVariablePunctuationMarks.indexOf(wordStr) == -1))) {
						log.println(" ==> expected bold font not found at " + wordStr);
						return false;
					}
					if (this.isItalics && !lineWords[w].hasAttribute(ImWord.ITALICS_ATTRIBUTE) && ((wordStr.length() > 1) || (fontStyleVariablePunctuationMarks.indexOf(wordStr) == -1))) {
						log.println(" ==> expected italics not found at " + wordStr);
						return false;
					}
					if ((this.isAllCaps || this.isSmallCaps) && !wordStr.equals(wordStr.toUpperCase())) {
						log.println(" ==> expected all-caps or small-caps not found at " + wordStr);
						return false;
					}
				}
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
					if (openBrackets.isEmpty() && ((wfs + wfsTolerance) < this.minFontSize)) {
						log.println(" ==> font size below minimum of " + this.minFontSize +  " at " + wordStr + ": " + wfs);
						return false;
					}
					if (this.maxFontSize < (wfs - wfsTolerance)) {
						log.println(" ==> font size above maximum of " + this.maxFontSize +  " at " + wordStr + ": " + wfs);
						return false;
					}
					if (wordStr.equals(wordStr.toUpperCase()) && !wordStr.equals(wordStr.toLowerCase())) {
						if ((w != 0) && !gotSmallCaps && (lineWords[w-1].getNextRelation() == ImWord.NEXT_RELATION_CONTINUE)) {
							int pwfs = lineWords[w-1].getFontSize();
							if (wfs < pwfs)
								gotSmallCaps = true;
						}
						capsMinFontSize = Math.min(capsMinFontSize, wfs);
						capsMaxFontSize = Math.max(capsMaxFontSize, wfs);
					}
				}
				lineWordString.append(wordStr);
				if (this.ignoreParentheses && Gamta.isClosingBracket(wordStr) && (openBrackets.size() != 0)) {
					if (Gamta.closes(wordStr, ((String) openBrackets.getLast())))
						openBrackets.removeLast();
				}
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
			log.println("   - got bold: " + gotBold);
			log.println("   - got italics: " + gotItalics);
			log.println("   - got all-caps: " + gotAllCaps);
			log.println("   - caps font size range: " + capsMinFontSize + "-" + capsMaxFontSize);
			
			//	check font style containment
			if (this.containsBold && !gotBold) {
				log.println(" ==> expected bold part not found");
				return false;
			}
			if (this.containsItalics && !gotItalics) {
				log.println(" ==> expected part in italics not found");
				return false;
			}
			if (this.containsAllCaps && !gotAllCaps) {
				log.println(" ==> expected part in all-caps not found");
				return false;
			}
			if (this.containsSmallCaps && !gotAllCaps) {
				log.println(" ==> expected part in small-caps not found");
				return false;
			}
			if (this.containsSmallCaps && !gotSmallCaps) {
				log.println(" ==> expected part in small-caps not found");
				return false;
			}
			if ((this.isSmallCaps || this.startIsSmallCaps || this.containsSmallCaps) && (capsMaxFontSize <= capsMinFontSize)) {
				log.println(" ==> expected part in small-caps not found, all-caps font size range is " + capsMinFontSize + "-" + capsMaxFontSize);
				return false;
			}
			
			//	use filter patterns
			if (this.filterPatterns != null) {
				log.println(" - checking filter patterns");
				for (int p = 0; p < this.filterPatterns.length; p++) {
					Matcher m = this.filterPatterns[p].matcher(lineWordString);
					if (m.find() && (m.start() == 0)) {
						log.println(" ==> excluded by filter pattern match on " + m.group());
						return false;
					}
				}
			}
			
			//	no patterns to match, this one looks good
			if (this.startPatterns == null) {
				log.println(" ==> match");
				return true;
			}
			
			//	check against start patterns
			log.println(" - checking start patterns");
			for (int p = 0; p < this.startPatterns.length; p++) {
				Matcher m = this.startPatterns[p].matcher(lineWordString);
				if (m.find() && (m.start() == 0)) {
					log.println(" ==> match via start pattern match on " + m.group());
					return true;
				}
			}
			
			//	no pattern matched
			log.println(" ==> failed to match any start pattern");
			return false;
		}
		
		private static HeadingStyleDefined getHeadingStyle(int group, DocumentStyle style) {
			if (style.getSubset("" + group).getPropertyNames().length == 0)
				return null;
			return new HeadingStyleDefined(group, style);
		}
	}
	
	private static Pattern[] getPatterns(String[] patternStrings) {
		if (patternStrings == null)
			return null;
		ArrayList patterns = new ArrayList(patternStrings.length);
		for (int p = 0; p < patternStrings.length; p++) try {
			Pattern pattern = Pattern.compile(patternStrings[p]);
			patterns.add(pattern);
		}
		catch (PatternSyntaxException pse) {
			System.out.println("Could not compile pattern " + patternStrings[p]);
			pse.printStackTrace(System.out);
		}
		if (patterns.isEmpty())
			return null;
		return ((Pattern[]) patterns.toArray(new Pattern[patterns.size()]));
	}
	
	private void markHeadings(ImPage page, HeadingStyleDefined[] headingStyles, ProgressMonitor pm) {
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
				if ((blockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(blockWords[cb][0].getTextStreamType()))
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
			int[] lineLeftColDists = new int[blockLines.length];
			int[] lineRightColDists = new int[blockLines.length];
			int[] lineLeftBlockDists = new int[blockLines.length];
			int[] lineRightBlockDists = new int[blockLines.length];
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
				lineLeftColDists[l] = (blockLines[l].bounds.left - blockParentColumns[b].bounds.left);
				if ((lineLeftColDists[l] * 25) < page.getImageDPI())
					pm.setInfo(" --> flush left in column");
				lineRightColDists[l] = (blockParentColumns[b].bounds.right - blockLines[l].bounds.right);
				if ((lineRightColDists[l] * 25) < page.getImageDPI())
					pm.setInfo(" --> flush right in column");
				lineLeftBlockDists[l] = (blockLines[l].bounds.left - pageBlocks[b].bounds.left);
				if ((lineLeftBlockDists[l] * 25) < page.getImageDPI())
					pm.setInfo(" --> flush left in block");
				lineRightBlockDists[l] = (pageBlocks[b].bounds.right - blockLines[l].bounds.right);
				if ((lineRightBlockDists[l] * 25) < page.getImageDPI())
					pm.setInfo(" --> flush right in block");
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
					if (headingStyles[s].matches(page, blockLines[l], lineWords[l], l, blockLines.length, lineLeftColDists[l], lineRightColDists[l], lineLeftBlockDists[l], lineRightBlockDists[l], System.out)) {
						lineHeadingStyles[l] = headingStyles[s];
						pm.setInfo(" --> match in group " + lineHeadingStyles[l].group + " for level " + lineHeadingStyles[l].level);
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
			ArrayList headingAnnots = new ArrayList();
			ArrayList headingAnnotStyles = new ArrayList();
			boolean splitHeadingOffBlock = false;
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
					int lineLeftDist = (headingStyle.isBlockAligned ? lineLeftBlockDists[headingStartLineIndex] : lineLeftColDists[headingStartLineIndex]);
					int lineRightDist = (headingStyle.isBlockAligned ? lineRightBlockDists[headingStartLineIndex] : lineRightColDists[headingStartLineIndex]);
					ImAnnotation headingAnnot = markHeading(page, headingStart, headingEnd, headingStyle, lineLeftDist, lineRightDist, headingEndLineShort, pm);
					if (headingAnnot != null) {
						headingAnnots.add(headingAnnot);
						headingAnnotStyles.add(headingStyle);
						splitHeadingOffBlock = (splitHeadingOffBlock || headingStyle.splitOffBlock);
					}
					headingStart = lineWords[l][0];
					headingEnd = lineWords[l][lineWords[l].length-1];
					headingStyle = lineHeadingStyles[l];
					headingStartLineIndex = l;
					headingEndLineShort = lineIsShort[l];
				}
			}
			if (headingStyle != null) {
				int lineLeftDist = (headingStyle.isBlockAligned ? lineLeftBlockDists[headingStartLineIndex] : lineLeftColDists[headingStartLineIndex]);
				int lineRightDist = (headingStyle.isBlockAligned ? lineRightBlockDists[headingStartLineIndex] : lineRightColDists[headingStartLineIndex]);
				ImAnnotation headingAnnot = markHeading(page, headingStart, headingEnd, headingStyle, lineLeftDist, lineRightDist, headingEndLineShort, pm);
				if (headingAnnot != null) {
					headingAnnots.add(headingAnnot);
					headingAnnotStyles.add(headingStyle);
					splitHeadingOffBlock = (splitHeadingOffBlock || headingStyle.splitOffBlock);
				}
			}
			
			//	anything to merge? anything to split?
			if ((headingAnnots.size() < 2) && !splitHeadingOffBlock)
				continue;
			
			//	perform mergers where applicable
			for (int h = 1; h < headingAnnots.size(); h++) {
				ImAnnotation topHeading = ((ImAnnotation) headingAnnots.get(h-1));
				HeadingStyleDefined topStyle = ((HeadingStyleDefined) headingAnnotStyles.get(h-1));
				ImAnnotation bottomHeading = ((ImAnnotation) headingAnnots.get(h));
				HeadingStyleDefined bottomStyle = ((HeadingStyleDefined) headingAnnotStyles.get(h));
				pm.setInfo(" - checking merger of headings " + ImUtils.getString(topHeading.getFirstWord(), topHeading.getLastWord(), true) + " and " + ImUtils.getString(bottomHeading.getFirstWord(), bottomHeading.getLastWord(), true));
				
				//	check levels
				if (topStyle.level != bottomStyle.level) {
					pm.setInfo("   ==> cannot merge across levels (" + topStyle.level + " in top group " + topStyle.group + ", " + bottomStyle.level + " in bottom group " + bottomStyle.group + ")");
					continue;
				}
				
				//	check merge mode(s)
				if ("never".equals(topStyle.lineBreakMergeMode)) {
					pm.setInfo("   ==> group " + topStyle.group + " (top) never merges");
					continue;
				}
				else if ("never".equals(bottomStyle.lineBreakMergeMode)) {
					pm.setInfo("   ==> group " + bottomStyle.group + " (bottom) never merges");
					continue;
				}
				
				//	check cross-group merge modes
				if (topStyle.group == bottomStyle.group) {}
				else if ("group".equals(topStyle.lineBreakMergeMode)) {
					pm.setInfo("   ==> group " + topStyle.group + " (top) never merges across groups (bottom group is " + bottomStyle.group + ")");
					continue;
				}
				else if ("group".equals(bottomStyle.lineBreakMergeMode)) {
					pm.setInfo("   ==> group " + bottomStyle.group + " (bottom) never merges across groups (top group is " + topStyle.group + ")");
					continue;
				}
				else if ("level-upwards".equals(topStyle.lineBreakMergeMode)) {
					pm.setInfo("   ==> group " + topStyle.group + " (top) only merges upwards across groups (bottom group is " + bottomStyle.group + ")");
					continue;
				}
				else if ("level-downwards".equals(bottomStyle.lineBreakMergeMode)) {
					pm.setInfo("   ==> group " + bottomStyle.group + " (bottom) only merges downwards across groups (top group is " + topStyle.group + ")");
					continue;
				}
				
				//	perform merger
				ImAnnotation heading = page.getDocument().addAnnotation(topHeading.getFirstWord(), bottomHeading.getLastWord(), ImAnnotation.HEADING_TYPE);
				heading.setAttribute("reason", ((topStyle.group == bottomStyle.group) ? topHeading.getAttribute("reason") : (topHeading.getAttribute("reason") + "+" + bottomStyle.group)));
				heading.setAttribute("level", topHeading.getAttribute("level"));
				heading.copyAttributes(topHeading);
				heading.copyAttributes(bottomHeading);
				page.getDocument().removeAnnotation(topHeading);
				page.getDocument().removeAnnotation(bottomHeading);
				straightenHeadingStreamStructure(heading);
				headingAnnots.set(h, heading);
				headingAnnotStyles.set(h, topStyle); // preserve top style for merge behavior, as there might be one more heading coming
				pm.setInfo("   ==> merged: " + ImUtils.getString(heading.getFirstWord(), heading.getLastWord(), true));
			}
			
			//	split headings off (or out of) block if required
			for (int h = 0; h < headingAnnots.size(); h++) {
				HeadingStyleDefined style = ((HeadingStyleDefined) headingAnnotStyles.get(h));
				if (!style.splitOffBlock)
					continue;
				ImAnnotation heading = ((ImAnnotation) headingAnnots.get(h));
				int headingTop = pageBlocks[b].bounds.bottom;
				int headingBottom = pageBlocks[b].bounds.top;
				for (ImWord imw = heading.getFirstWord(); imw != null; imw = imw.getNextWord()) {
					headingTop = Math.min(headingTop, imw.bounds.top);
					headingBottom = Math.max(headingBottom, imw.bounds.bottom);
					if (imw == heading.getLastWord())
						break;
				}
				if ((pageBlocks[b].bounds.getHeight() - (headingBottom - headingTop)) < (page.getImageDPI() / 12))
					continue; // too little left of block to accommodate anything to split
				BoundingBox headingBox = new BoundingBox(pageBlocks[b].bounds.left, pageBlocks[b].bounds.right, headingTop, headingBottom);
				this.regionActionProvider.splitBlock(page, pageBlocks[b], headingBox);
			}
		}
	}
	
	private static ImAnnotation markHeading(ImPage page, ImWord headingStart, ImWord headingEnd, HeadingStyleDefined headingStyle, int startLineLeftDist, int startLineRightDist, boolean endLineShort, ProgressMonitor pm) {
		
		//	accept non-block heading only if (a) it is bold or all-caps as a whole or (b) it (its last line) is short or ends with a paragraph break
		if (!headingStyle.isBlock && !headingStyle.isBold && !headingStyle.isAllCaps && !endLineShort && (headingEnd.getNextRelation() != ImWord.NEXT_RELATION_PARAGRAPH_END))
			return null;
		
		//	mark heading, including all the attributes
		ImAnnotation heading = page.getDocument().addAnnotation(headingStart, headingEnd, ImAnnotation.HEADING_TYPE);
		heading.setAttribute("reason", ("" + headingStyle.group));
		heading.setAttribute("level", ("" + headingStyle.level));
		if (((Math.abs(startLineLeftDist - startLineRightDist) * 8) < page.getImageDPI()))
			heading.setAttribute(ImAnnotation.TEXT_ORIENTATION_CENTERED);
		if (headingStyle.isAllCaps)
			heading.setAttribute(ImAnnotation.ALL_CAPS_ATTRIBUTE);
		if (headingStyle.isBold)
			heading.setAttribute(ImAnnotation.BOLD_ATTRIBUTE);
		if (((headingStyle.minFontSize + headingStyle.maxFontSize) / 2) > 6)
			heading.setAttribute(ImAnnotation.FONT_SIZE_ATTRIBUTE, ("" + ((headingStyle.minFontSize + headingStyle.maxFontSize) / 2)));
		straightenHeadingStreamStructure(heading);
		pm.setInfo(" ==> marked heading at level " + headingStyle.level + " (matching group " + headingStyle.group + "): " + ImUtils.getString(heading.getFirstWord(), heading.getLastWord(), true));
		return heading;
	}
	
	private static class InLineHeadingStyle {
		final int group;
		int minFontSize;
		int maxFontSize;
		int minBlockLineCount;
		boolean isBold;
		boolean startIsBold;
		boolean containsBold;
		boolean isItalics;
		boolean startIsItalics;
		boolean containsItalics;
		boolean isAllCaps;
		boolean startIsAllCaps;
		boolean containsAllCaps;
		boolean isSmallCaps;
		boolean startIsSmallCaps;
		boolean containsSmallCaps;
		boolean ignoreParentheses;
		Pattern[] startPatterns;
		Pattern[] filterPatterns;
		
		private InLineHeadingStyle(int group, DocumentStyle style) {
			this.group = group;
			int fontSize = style.getIntProperty((this.group + ".fontSize"), -1);
			this.minFontSize = style.getIntProperty((this.group + ".minFontSize"), ((fontSize == -1) ? 0 : fontSize));
			this.maxFontSize = style.getIntProperty((this.group + ".maxFontSize"), ((fontSize == -1) ? 72 : fontSize));
			this.minBlockLineCount = style.getIntProperty((this.group + ".minBlockLineCount"), 0);
			this.isBold = style.getBooleanProperty((this.group + ".isBold"), false);
			this.startIsBold = style.getBooleanProperty((this.group + ".startIsBold"), false);
			this.containsBold = style.getBooleanProperty((this.group + ".containsBold"), false);
			this.isItalics = style.getBooleanProperty((this.group + ".isItalics"), false);
			this.startIsItalics = style.getBooleanProperty((this.group + ".startIsItalics"), false);
			this.containsItalics = style.getBooleanProperty((this.group + ".containsItalics"), false);
			this.isAllCaps = style.getBooleanProperty((this.group + ".isAllCaps"), false);
			this.startIsAllCaps = style.getBooleanProperty((this.group + ".startIsAllCaps"), false);
			this.containsAllCaps = style.getBooleanProperty((this.group + ".containsAllCaps"), false);
			this.isSmallCaps = style.getBooleanProperty((this.group + ".isSmallCaps"), false);
			this.startIsSmallCaps = style.getBooleanProperty((this.group + ".startIsSmallCaps"), false);
			this.containsSmallCaps = style.getBooleanProperty((this.group + ".containsSmallCaps"), false);
			this.ignoreParentheses = style.getBooleanProperty((this.group + ".ignoreParentheses"), false);
			this.startPatterns = getPatterns(style.getStringListProperty((this.group + ".startPatterns"), null, " "));
			this.filterPatterns = getPatterns(style.getStringListProperty((this.group + ".filterPatterns"), null, " "));
		}
		
		ImWord getMatchEnd(ImPage page, ImRegion line, ImWord[] lineWords, int blockLineCount, PrintStream log) {
			log.println("Mathing against line " + line.bounds);
//			
//			//	select how to measure alignment
//			int leftDist = ((!this.isBlock && this.isBlockAligned) ? leftBlockDist : leftColDist);
//			int rightDist = ((!this.isBlock && this.isBlockAligned) ? rightBlockDist : rightColDist);
//			
//			//	add some tolerance for indent (up to half an inch, some 12mm)
//			if (this.isLeftAligned && this.canHaveIndent && (blockLinePos == 0) && ((leftDist * 2) < page.getImageDPI())) {
//				if (leftDist != 0)
//					log.println(" - ignoring indent left edge distance " + leftDist);
//				leftDist = 0;
//			}
//			
//			//	compute orientation
//			log.println(" - checking orientation for " + ((!this.isBlock && this.isBlockAligned) ? "block" : "column") + " edge distances " + leftDist + " and " + rightDist);
//			boolean isFlushLeft = ((leftDist * 25) < page.getImageDPI());
//			log.println("   - left aligned: " + isFlushLeft);
//			boolean isFlushRight = ((rightDist * 25) < page.getImageDPI());
//			log.println("   - right aligned: " + isFlushRight);
//			boolean isCentered = ((Math.abs(leftDist - rightDist) * 8) < page.getImageDPI());
//			log.println("   - center aligned: " + isCentered);
//			
//			//	check alignment first
//			if (this.isCenterStrict && (isFlushLeft || isFlushRight)) {
//				log.println(" ==> mismatch on strictly-centered orientation");
//				return false;
//			}
//			if (this.isLeftAligned && !isFlushLeft) {
//				log.println(" ==> mismatch on left block edge");
//				return false;
//			}
//			if (this.isRightAligned && !isFlushRight) {
//				log.println(" ==> mismatch on right block edge");
//				return false;
//			}
//			if (this.isCenterAligned && !isCentered) {
//				log.println(" ==> mismatch on centered orientation");
//				return false;
//			}
//			log.println(" - orientation match");
			
			//	check line count and position
			if (this.minBlockLineCount > 0) {
				log.println(" - checking number of lines in block");
				if (blockLineCount < this.minBlockLineCount) {
					log.println(" ==> too few lines (" + blockLineCount + ") in block, expecting at least " + this.minBlockLineCount);
					return null;
				}
				log.println(" - block line count match");
			}
			
			//	check start style
			if ((this.isBold || this.startIsBold) && !lineWords[0].hasAttribute(ImWord.BOLD_ATTRIBUTE)) {
				log.println(" ==> expected bold start not found");
				return null;
			}
			if ((this.isItalics || this.startIsItalics) && !lineWords[0].hasAttribute(ImWord.ITALICS_ATTRIBUTE)) {
				log.println(" ==> expected italics start not found");
				return null;
			}
			if ((this.isAllCaps || this.startIsAllCaps || this.isSmallCaps || this.startIsSmallCaps) && !lineWords[0].getString().equals(lineWords[0].getString().toUpperCase())) {
				log.println(" ==> expected all-caps or small-caps start not found");
				return null;
			}
			log.println(" - start style match");
			
			//	concatenate words, checking style along the way
			log.println(" - checking font style");
//			StringBuffer lineWordString = new StringBuffer();
			boolean gotBold = false;
			boolean gotItalics = false;
			boolean gotAllCaps = false;
			boolean gotSmallCaps = false;
			int capsMinFontSize = 72;
			int capsMaxFontSize = 0;
			LinkedList openBrackets = new LinkedList();
			ImWord inLineHeadingEnd = null;
			for (int w = 0; w < lineWords.length; w++) {
				String wordStr = lineWords[w].getString();
				if (this.ignoreParentheses && Gamta.isOpeningBracket(wordStr))
					openBrackets.addLast(wordStr);
				if (openBrackets.isEmpty()) {
					if (this.isBold && !lineWords[w].hasAttribute(ImWord.BOLD_ATTRIBUTE) && ((wordStr.length() > 1) || (fontStyleVariablePunctuationMarks.indexOf(wordStr) == -1))) {
						log.println(" - expected bold font ends before " + wordStr);
						break;
					}
					if (this.isItalics && !lineWords[w].hasAttribute(ImWord.ITALICS_ATTRIBUTE) && ((wordStr.length() > 1) || (fontStyleVariablePunctuationMarks.indexOf(wordStr) == -1))) {
						log.println(" - expected italics font ends before " + wordStr);
						break;
					}
					if ((this.isAllCaps || this.isSmallCaps) && !wordStr.equals(wordStr.toUpperCase())) {
						log.println(" - expected all-caps or small-caps ends before " + wordStr);
						break;
					}
				}
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
					if (openBrackets.isEmpty() && ((wfs + wfsTolerance) < this.minFontSize)) {
						log.println(" - expected minumum font size of " + this.minFontSize +  " ends before " + wordStr + ": " + wfs);
						break;
					}
					if (this.maxFontSize < (wfs - wfsTolerance)) {
						log.println(" - expected maximum font size of " + this.maxFontSize +  " ends before " + wordStr + ": " + wfs);
						break;
					}
					if (wordStr.equals(wordStr.toUpperCase()) && !wordStr.equals(wordStr.toLowerCase())) {
						if ((w != 0) && !gotSmallCaps && (lineWords[w-1].getNextRelation() == ImWord.NEXT_RELATION_CONTINUE)) {
							int pwfs = lineWords[w-1].getFontSize();
							if (wfs < pwfs)
								gotSmallCaps = true;
						}
						capsMinFontSize = Math.min(capsMinFontSize, wfs);
						capsMaxFontSize = Math.max(capsMaxFontSize, wfs);
					}
				}
//				lineWordString.append(wordStr);
				if (this.ignoreParentheses && Gamta.isClosingBracket(wordStr) && (openBrackets.size() != 0)) {
					if (Gamta.closes(wordStr, ((String) openBrackets.getLast())))
						openBrackets.removeLast();
				}
				if (lineWords[w].getTextStreamId().equals(lineWords[0].getTextStreamId()) && openBrackets.isEmpty())
					inLineHeadingEnd = lineWords[w];
//				if ((w+1) == lineWords.length)
//					break;
//				if (lineWords[w].getNextWord() != lineWords[w+1])
//					lineWordString.append(" ");
//				else if ((lineWords[w].getNextRelation() == ImWord.NEXT_RELATION_SEPARATE) && Gamta.insertSpace(lineWords[w].getString(), lineWords[w+1].getString()))
//					lineWordString.append(" ");
//				else if (lineWords[w].getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
//					lineWordString.append(" ");
//				else if ((lineWords[w].getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED) && (lineWordString.length() != 0))
//					lineWordString.deleteCharAt(lineWordString.length()-1);
			}
			log.println("   - got bold: " + gotBold);
			log.println("   - got italics: " + gotItalics);
			log.println("   - got all-caps: " + gotAllCaps);
			log.println("   - caps font size range: " + capsMinFontSize + "-" + capsMaxFontSize);
			
			//	did we find some possible end?
			if (inLineHeadingEnd == null) {
				log.println(" ==> no viable end word found (or text stream broken)");
				return null;
			}
//			
//			//	any brackets left open?
//			//	TODOne try and go back to matching opening bracket (might have a match at that point already)
//			if (openBrackets.size() != 0) {
//				log.println(" ==> found brackets left open: " + openBrackets);
//				return null;
//			}
			
			//	check font style containment
			if (this.containsBold && !gotBold) {
				log.println(" ==> expected bold part not found");
				return null;
			}
			if (this.containsItalics && !gotItalics) {
				log.println(" ==> expected part in italics not found");
				return null;
			}
			if (this.containsAllCaps && !gotAllCaps) {
				log.println(" ==> expected part in all-caps not found");
				return null;
			}
			if (this.containsSmallCaps && !gotAllCaps) {
				log.println(" ==> expected part in small-caps not found");
				return null;
			}
			if (this.containsSmallCaps && !gotSmallCaps) {
				log.println(" ==> expected part in small-caps not found");
				return null;
			}
			if ((this.isSmallCaps || this.startIsSmallCaps || this.containsSmallCaps) && (capsMaxFontSize <= capsMinFontSize)) {
				log.println(" ==> expected part in small-caps not found, all-caps font size range is " + capsMinFontSize + "-" + capsMaxFontSize);
				return null;
			}
			
			//	create token sequence for pattern matching
			ImDocumentRoot lineDoc = new ImDocumentRoot(lineWords[0], inLineHeadingEnd, (ImDocumentRoot.NORMALIZE_CHARACTERS | ImDocumentRoot.NORMALIZATION_LEVEL_WORDS));
			
			//	use filter patterns
			if (this.filterPatterns != null) {
				log.println(" - checking filter patterns");
				for (int p = 0; p < this.filterPatterns.length; p++) {
					Matcher m = this.filterPatterns[p].matcher(lineDoc);
					if (m.find() && (m.start() == 0)) {
						log.println(" ==> excluded by filter pattern match on " + m.group());
						return null;
					}
				}
			}
			
			//	no patterns to match, this one looks good
			if (this.startPatterns == null) {
				log.println(" ==> match");
				return inLineHeadingEnd;
			}
			
			//	check against start patterns
			log.println(" - checking start patterns");
			for (int p = 0; p < this.startPatterns.length; p++) {
				Matcher m = this.startPatterns[p].matcher(lineDoc);
				if (m.find() && (m.start() == 0)) {
					log.println(" ==> match via start pattern match on " + m.group());
					return lineDoc.wordAtOffset(m.end()-1);
				}
			}
			
			//	no pattern matched
			log.println(" ==> failed to match any start pattern");
			return null;
		}
		
		private static InLineHeadingStyle getInLineHeadingStyle(int group, DocumentStyle style) {
			if (style.getSubset("" + group).getPropertyNames().length == 0)
				return null;
			return new InLineHeadingStyle(group, style);
		}
	}
	
	private static void markInLineHeadings(ImPage page, InLineHeadingStyle[] inLineHeadingStyles, ProgressMonitor spm) {
		
		//	work block by block
		ImRegion[] pageBlocks = page.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
		for (int b = 0; b < pageBlocks.length; b++) {
			spm.setInfo("Seeking in-line headings in block " + pageBlocks[b].pageId + "@" + pageBlocks[b].bounds);
			ImRegion[] blockLines = pageBlocks[b].getRegions(ImRegion.LINE_ANNOTATION_TYPE);
			spm.setInfo(" - got " + blockLines.length + " lines");
			if (blockLines.length == 0)
				continue;
			
			//	work line by line
			Arrays.sort(blockLines, ImUtils.topDownOrder);
			for (int l = 0; l < blockLines.length; l++) {
				spm.setInfo(" - checking line " + blockLines[l].pageId + "@" + blockLines[l].bounds);
				ImWord[] lineWords = blockLines[l].getWords();
				spm.setInfo("   - got " + lineWords.length + " words");
				if (lineWords.length == 0)
					continue;
				Arrays.sort(lineWords, ImUtils.textStreamOrder);
				if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(lineWords[0].getTextStreamType())) {
					spm.setInfo("   ==> not main text");
					continue;
				}
				for (int s = 0; s < inLineHeadingStyles.length; s++) {
					ImWord inLineHeadingEnd = inLineHeadingStyles[s].getMatchEnd(page, blockLines[l], lineWords, blockLines.length, System.out);
					if (inLineHeadingEnd == null) {
						spm.setInfo("   ==> no match");
						continue;
					}
					ImAnnotation inLineHeading = page.getDocument().addAnnotation(lineWords[0], inLineHeadingEnd, EMPHASIS_TYPE);
					if (inLineHeading == null) {
						spm.setInfo("   ==> cannot annotate across text streams");
						continue;
					}
					inLineHeading.setAttribute("inLineHeading");
					inLineHeading.setAttribute("reason", ("" + inLineHeadingStyles[s].group));
					spm.setInfo("   ==> got in-line heading " + ImUtils.getString(lineWords[0], inLineHeadingEnd, true));
					break;
				}
			}
		}
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
	
	private void markHeadings(ImPage page, int docAvgFontSize, int docMainFontSize, int pageImageDpi, HashSet emphasisWords, ProgressMonitor pm, ArrayList headings) {
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
				if ((blockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(blockWords[cb][0].getTextStreamType()))
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
			boolean[] lineIsStrictlyCentered = new boolean[blockLines.length];
			boolean[] lineIsAllCaps = new boolean[blockLines.length];
			boolean[] lineIsSmallCaps = new boolean[blockLines.length];
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
				lineIsStrictlyCentered[l] = (
						(((leftDist * 9) < (rightDist * 10)) && ((rightDist * 9) < (leftDist * 10)))
						&&
						(((leftDist * 25) > pageImageDpi) && ((rightDist * 25) > pageImageDpi))
					);
				if (lineIsStrictlyCentered[l])
					pm.setInfo(" --> strictly centered");
				
				//	assess line words
				int lineWordCount = 0;
				int lineAllCapsWordCount = 0;
				int lineAllCapsMinFontSize = 72;
				int lineAllCapsMaxFontSize = 0;
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
						if (lineWordString.equals(lineWordString.toUpperCase())) {
							lineAllCapsWordCount++;
							if (lineWords[l][w].hasAttribute(ImWord.FONT_SIZE_ATTRIBUTE)) try {
								lineAllCapsMinFontSize = Math.min(lineAllCapsMinFontSize, lineWords[l][w].getFontSize());
								lineAllCapsMaxFontSize = Math.max(lineAllCapsMaxFontSize, lineWords[l][w].getFontSize());
							} catch (NumberFormatException nfe) {}
						}
						if (emphasisWords.contains(lineWords[l][w]))
							lineEmphasisWordCount++;
						if (lineWords[l][w].hasAttribute(ImWord.BOLD_ATTRIBUTE))
							lineBoldWordCount++;
						else if ((lineWords[l][w].getString().length() > 1) || (fontStyleVariablePunctuationMarks.indexOf(lineWords[l][w].getString()) == -1))
							lineNonBoldBoldWordCount++;
					}
					if (lineWords[l][w].hasAttribute(ImWord.FONT_SIZE_ATTRIBUTE)) try {
						lineFontSizeSum += lineWords[l][w].getFontSize();
						lineFontSizeWordCount++;
					} catch (NumberFormatException nfe) {}
				}
				lineIsAllCaps[l] = ((lineWordCount != 0) && (lineAllCapsWordCount == lineWordCount));
				if (lineIsAllCaps[l])
					pm.setInfo(" --> all caps");
				lineIsSmallCaps[l] = ((lineWordCount != 0) && (lineAllCapsWordCount == lineWordCount) && (lineAllCapsMinFontSize < lineAllCapsMaxFontSize));
				if (lineIsSmallCaps[l])
					pm.setInfo(" --> small caps");
				lineHasEmphasis[l] = (lineEmphasisWordCount != 0);
				if (lineHasEmphasis[l])
					pm.setInfo(" --> has emphasis");
				lineIsEmphasized[l] = ((lineWordCount != 0) && (lineEmphasisWordCount == lineWordCount));
				if (lineIsEmphasized[l])
					pm.setInfo(" --> emphasized");
				lineHasBoldEmphasis[l] = (lineBoldWordCount != 0);
				if (lineHasBoldEmphasis[l])
					pm.setInfo(" --> has bold emphasis");
				lineIsBold[l] = ((lineWordCount != 0) && (lineBoldWordCount > lineNonBoldBoldWordCount) && ((lineBoldWordCount + lineNonBoldBoldWordCount) == lineWordCount));
				if (lineIsBold[l])
					pm.setInfo(" --> bold");
				lineFontSize[l] = ((lineFontSizeWordCount == 0) ? -1 : ((lineFontSizeSum + (lineFontSizeWordCount / 2)) / lineFontSizeWordCount));
				lineIsLargeFont[l] = ((docAvgFontSize > 6) && (lineFontSize[l] > docAvgFontSize) && (docMainFontSize > 6) && (lineFontSize[l] > docMainFontSize));
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
				if ((lineFontSize[l] < docAvgFontSize) && !lineIsBold[l] && !lineIsAllCaps[l])
					break;
				
				//	bold + large-font ==> heading
				if (lineIsBold[l] && lineIsLargeFont[l]) {
					//	TODO check 
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
//				if (lineIsShort[l] && lineIsCentered[l]) {
				if (lineIsShort[l] && lineIsStrictlyCentered[l]) {
					pm.setInfo(" ==> heading (4): " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
					lineIsHeadingBecause[l] = 4;
					continue;
				}
				
				//	centered + all-caps ==> heading
//				if (lineIsCentered[l] && lineIsAllCaps[l]) {
				if (lineIsStrictlyCentered[l] && lineIsAllCaps[l]) {
					pm.setInfo(" ==> heading (5): " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
					lineIsHeadingBecause[l] = 5;
					continue;
				}
				
				//	short + bold + block-top-line ==> heading
				if (lineIsShort[l] && lineIsBold[l]) {
					pm.setInfo(" ==> heading (6): " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
					lineIsHeadingBecause[l] = 6;
					continue;
				}
				
				//	centered + emphasis + block-top-line + short-centered-line-below ==> heading
//				if (lineIsCentered[l] && lineHasEmphasis[l] && (l == 0) && (blockLines.length > 1) && lineIsShort[l+1] && lineIsCentered[l+1]) {
				if (lineIsCentered[l] && lineHasEmphasis[l] && (l == 0) && (blockLines.length > 1) && lineIsShort[l+1] && lineIsStrictlyCentered[l+1]) {
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
//						if (lineIsCentered[headingStartLineIndex])
						if (lineIsStrictlyCentered[headingStartLineIndex])
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
//				if (lineIsCentered[headingStartLineIndex])
				if (lineIsStrictlyCentered[headingStartLineIndex])
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
		
		//	normalize first line of paragraph
		paragraphFirstLine = StringUtils.normalizeString(paragraphFirstLine);
		if (DEBUG_IS_FOOTNOTE) System.out.println(" - normalized is " + paragraphFirstLine);
		
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
	
	private static HashMap markBlockCaptions(ImDocument doc, ImRegion block, ImRegion[] pageParagraphs, CaptionStartPattern[] startPatterns, CaptionStartPattern[] defaultStartPatterns, int minFontSize, int maxFontSize, boolean startIsBold, boolean startIsSeparateLine, ProgressMonitor pm) {
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
			int prevParagraphDist = ((p == 0) ? Short.MAX_VALUE : Math.max(0, (blockParagraphs[p].bounds.top - blockParagraphs[p-1].bounds.bottom)));
			
			//	test for (start of) caption
			ImWord[] paragraphWords = ((ImWord[]) blockParagraphsToWords.get(blockParagraphs[p]));
			paragraphPatternMatches[p] = getCaptionPatternMatch(blockParagraphs[p], paragraphWords, startPatterns, defaultStartPatterns, minFontSize, maxFontSize, startIsBold, startIsSeparateLine);
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
			
			//	... and attach any continuing matches
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
					caption.setAttribute("startId", caption.getFirstWord().getLocalID());
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
	private static CaptionPatternMatch getCaptionPatternMatch(ImRegion paragraph, ImWord[] paragraphWords, CaptionStartPattern[] startPatterns, CaptionStartPattern[] defaultStartPatterns, int minFontSize, int maxFontSize, boolean startIsBold, boolean startIsSeparateLine) {
		
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
		if (DEBUG_IS_CAPTION) System.out.println(" - caption test first line is " + paragraphFirstLine);
		
		//	normalize first line of paragraph
		paragraphFirstLine = StringUtils.normalizeString(paragraphFirstLine);
		if (DEBUG_IS_CAPTION) System.out.println(" - normalized is " + paragraphFirstLine);
		
		//	test first line against detector patterns
		CaptionStartPattern startPattern = null;
		String startPatternMatch = null;
		for (int p = 0; p < startPatterns.length; p++) {
			if (startPatterns[p].isPlateSubCaption())
				continue;
			Matcher startMatcher = startPatterns[p].matcher(paragraphFirstLine);
			if (startMatcher.find() && (startMatcher.start() == 0)) {
				if (startIsSeparateLine && (startMatcher.end() != paragraphFirstLine.length()))
					continue;
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
		for (int w = 1 /* 0 if lead word */; w < paragraphWords.length; w++) {
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
		if (!startIsSeparateLine) {
			boolean isCaptionReference;
			if (startIsBold || (startPatterns != defaultStartPatterns))
				isCaptionReference = ((wordCount * 3) < (numberingCount * 2));
			else isCaptionReference = (wordCount < numberingCount);
			if (isCaptionReference) {
				if (DEBUG_IS_CAPTION) System.out.println(" ==> " + wordCount + " words vs. " + numberingCount + " numbers, caption reference");
				return null;
			}
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
		if (DEBUG_IS_CAPTION) System.out.println(" ==> " + wordCount + " words vs. " + numberingCount + " numbers, caption" + (startIsSeparateLine ? " start" : ""));
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
	
	private ImRegion getAroundCaptionTarget(ImPage page, BoundingBox captionBounds, int dpi, boolean isTableCaption) {
		
		//	get regions
		ImRegion[] pageRegions = page.getRegions(isTableCaption ? ImRegion.TABLE_TYPE : null);
		
		//	seek suitable target regions
		ImRegion targetRegion = null;
		for (int r = 0; r < pageRegions.length; r++) {
			
			//	ignore lines and paragraphs, as well as blocks, columns, and generic regions
			if (ImRegion.LINE_ANNOTATION_TYPE.equals(pageRegions[r].getType()) || ImRegion.PARAGRAPH_TYPE.equals(pageRegions[r].getType()) || ImRegion.BLOCK_ANNOTATION_TYPE.equals(pageRegions[r].getType()) || ImRegion.COLUMN_ANNOTATION_TYPE.equals(pageRegions[r].getType()) || ImRegion.REGION_ANNOTATION_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	ignore tables we're not out for
			if (isTableCaption != ImRegion.TABLE_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	ignore anything not surrounding caption
			if (!pageRegions[r].bounds.includes(captionBounds, true))
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
//			
//			//	ignore lines and paragraphs, as well as blocks and columns
//			if (ImRegion.LINE_ANNOTATION_TYPE.equals(pageRegions[r].getType()) || ImRegion.PARAGRAPH_TYPE.equals(pageRegions[r].getType()))
//				continue;
//			
//			//	ignore tables we're not out for
//			if (ImRegion.TABLE_TYPE.equals(pageRegions[r].getType()))
//				continue;
			
			//	ignore anything but images and graphics
			if (!ImRegion.IMAGE_TYPE.equals(pageRegions[r].getType()) && !ImRegion.GRAPHICS_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	this one lies inside what we already have
			if (targetRegion.bounds.includes(pageRegions[r].bounds, false))
				continue;
			
			//	check if candidate aggregate target contains words that cannot belong to a caption target area, and find lower edge
			BoundingBox aggregateBounds = new BoundingBox(Math.min(targetRegion.bounds.left, pageRegions[r].bounds.left), Math.max(targetRegion.bounds.right, pageRegions[r].bounds.right), Math.min(targetRegion.bounds.top, pageRegions[r].bounds.top), Math.max(targetRegion.bounds.bottom, pageRegions[r].bounds.bottom));
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
		
		//	if we have defined header areas, split any blocks that partially overlap them right away
		if ((headerAreas != null) && (this.regionActionProvider != null)) {
			ImRegion[] pageBlocks = page.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
			boolean newSplit = false;
			for (int h = 0; h < headerAreas.length; h++) {
				pm.setInfo(" - splitting blocks overlapping header area " + headerAreas[h] + " on page " + page.pageId);
				if (newSplit) // need to re-get blocks if we split anything for last header area
					pageBlocks = page.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
				newSplit = false;
				for (int b = 0; b < pageBlocks.length; b++) {
					pm.setInfo("   - checking block at " + pageBlocks[b].bounds);
					if (pageBlocks[b].bounds.liesIn(headerAreas[h], false)) {
						pm.setInfo("     ==> completely contained");
						continue; // completely contained, no need for splitting
					}
					if (!pageBlocks[b].bounds.overlaps(headerAreas[h])) {
						pm.setInfo("     ==> no overlap");
						continue; // no need for splitting this one
					}
					if (this.regionActionProvider.splitBlock(page, pageBlocks[b], headerAreas[h].intersect(pageBlocks[b].bounds))) {
						newSplit = true;
						pm.setInfo("     ==> split successfully");
					}
					else pm.setInfo("     ==> split failed");
				}
			}
		}
		
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
			
			//	use defined header areas
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
			else pm.setInfo(" --> page number of " + p + " (id " + pageData[p].page.pageId + ") identified as " + pageData[p].pageNumber.value + " (score " + pageData[p].pageNumber.score + ")");
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
				pm.setInfo("   - eliminated page number " + pageData[p].pageNumber.value + " in page " + p + " (id " + pageData[p].page.pageId + ") for sequence inconsistency.");
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
			PageNumber ePageNumber = new PageNumber(pageData[p].page.pageId, (bPageNumber.value - bPageNumber.pageId + pageData[p].page.pageId));
			if (existingPageNumbers.add("" + ePageNumber.value)) {
				pageData[p].pageNumber = ePageNumber;
				pageData[p].page.setAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + ePageNumber.value));
				bPageNumber = ePageNumber;
				pm.setInfo(" --> backward-extrapolated page number of page " + p + " (id " + pageData[p].page.pageId + ") to " + pageData[p].pageNumber.value);
			}
			else pm.setInfo(" --> could not backward-extrapolated page number of page " + p + " (id " + pageData[p].page.pageId + "), page number " + ePageNumber.value + " already assigned");
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
			PageNumber ePageNumber = new PageNumber(pageData[p].page.pageId, (bPageNumber.value - bPageNumber.pageId + pageData[p].page.pageId));
			if (existingPageNumbers.add("" + ePageNumber.value)) {
				pageData[p].pageNumber = ePageNumber;
				pageData[p].page.setAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + ePageNumber.value));
				fPageNumber = ePageNumber;
				pm.setInfo(" --> forward-extrapolated page number of page " + p + " (id " + pageData[p].page.pageId + ") to " + pageData[p].pageNumber.value);
			}
			else pm.setInfo(" --> could not forward-extrapolated page number of page " + p + " (id " + pageData[p].page.pageId + "), page number " + ePageNumber.value + " already assigned");
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
	
	private static class PageNumber {
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
//			this.value = Integer.parseInt(word.getString()); // TODOne handle Roman numbers !!!
			this.value = (this.valueStr.matches("[0-9]+") ? Integer.parseInt(this.valueStr) : StringUtils.parseRomanNumber(this.valueStr));
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
//		testFileName = "zt03881p227.pdf.imdir"; // TODOne fails to detect full-page table on page 5
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
//		testFileName = "Zootaxa/zt02241p032.pdf.raw.imf"; // TODOne finally found bogus table document: zt02241p032.pdf (page 5) ==> works after re-decoding to add graphics
		testFileName = "PROC.ENTOMOL.SOC.WASH.122.1.12–24.pdf.raw.imf"; // TODO fix heading false positives
		
		ImDocument doc = ImDocumentIO.loadDocument(new File("E:/Testdaten/PdfExtract/" + testFileName));
//		
//		DocumentStyle.addProvider(new DocumentStyle.Provider() {
//			public Properties getStyleFor(Attributed doc) {
//				Settings ds = Settings.loadSettings(new File("E:/GoldenGATEv3/Plugins/DocumentStyleProviderData/zootaxa.2007.journal_article.docStyle"));
////				Settings ds = Settings.loadSettings(new File("E:/GoldenGATEv3/Plugins/DocumentStyleProviderData/ijsem.0000.journal_article.docStyle"));
//				return ds.toProperties();
//			}
//		});
		
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
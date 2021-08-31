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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed;
import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.gamta.util.DocumentStyle;
import de.uka.ipd.idaho.gamta.util.DocumentStyle.ParameterDescription;
import de.uka.ipd.idaho.gamta.util.DocumentStyle.ParameterGroupDescription;
import de.uka.ipd.idaho.gamta.util.DocumentStyle.PropertiesData;
import de.uka.ipd.idaho.gamta.util.DocumentStyle.TestableElement;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.BufferedLineWriter;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.goldenGate.plugins.PluginDataProviderFileBased;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder.HtmlPageBuilderHost;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImLayoutObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.gamta.ImDocumentRoot;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.DisplayExtensionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener;
import de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider;
import de.uka.ipd.idaho.im.imagine.web.plugins.WebDocumentViewer;
import de.uka.ipd.idaho.im.util.ImDocumentIO;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.DisplayExtension;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.DisplayExtensionGraphics;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImDocumentStyle;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefConstants;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefDataSource;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefEditorFormHandler;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefEditorPanel;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefTypeSystem;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefTypeSystem.BibRefType;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils.AuthorData;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils.RefData;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * This plugin provides an editor for the bibliographic metadata of a document.
 * 
 * @author sautter
 */
public class DocumentMetaDataEditorProvider extends AbstractImageMarkupToolProvider implements SelectionActionProvider, GoldenGateImagineDocumentListener, BibRefConstants {
	private static final String META_DATA_EDITOR_IMT_NAME = "MetaDataEditor";
	private ImageMarkupTool metaDataEditor = new MetaDataEditor(META_DATA_EDITOR_IMT_NAME);
	private static final String META_DATA_ADDER_IMT_NAME = "MetaDataAdder";
	private ImageMarkupTool metaDataAdder = new MetaDataEditor(META_DATA_ADDER_IMT_NAME);
	
	private BibRefTypeSystem refTypeSystem = BibRefTypeSystem.getDefaultInstance();
	private String refTypeDefault = null;
	private String[] refIdTypes = {};
	private HashSet searchRefIdTypes = new HashSet();
	private String[] authorDetails = {};
	
	private BibRefDataSource[] refDataSources = null;
	private boolean inMasterConfiguration = false;
	
	/** public zero-argument constructor for class loading */
	public DocumentMetaDataEditorProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Document Metadata Editor";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		
		//	check verbosity
		this.inMasterConfiguration = this.parent.getConfiguration().isMasterConfiguration();
		
		//	load configuration
		Settings set = new Settings();
		try {
			set = Settings.loadSettings(new InputStreamReader(this.dataProvider.getInputStream("config.cnfg"), "UTF-8"));
		} catch (IOException ioe) {}
		
		//	get publication ID types
		LinkedHashSet refIdTypes = new LinkedHashSet();
		refIdTypes.addAll(Arrays.asList((" " + set.getSetting("refIdTypes", "DOI Handle ISBN ISSN")).split("\\s+")));
		this.refIdTypes = ((String[]) refIdTypes.toArray(new String[refIdTypes.size()]));
		
		//	get publication ID types suitable for searching sources searchRefIdTypes
		this.searchRefIdTypes.addAll(Arrays.asList((set.getSetting("searchRefIdTypes", "DOI ISBN")).split("\\s+")));
		
		//	get author detail types
		LinkedHashSet authorDetails = new LinkedHashSet();
		authorDetails.addAll(Arrays.asList((" " + set.getSetting("authorDetails", "")).split("\\s+")));
		authorDetails.remove("");
		this.authorDetails = ((String[]) authorDetails.toArray(new String[authorDetails.size()]));
		
		//	find default publication type
		this.refTypeDefault = set.getSetting("refTypeDefault");
		if (this.refTypeSystem.getBibRefType(this.refTypeDefault) == null) {
			if (this.refTypeDefault != null)
				this.logInMasterConfiguration("DocumentMetaDataEditorProvider: invalid default reference type '" + this.refTypeDefault + "', available reference types are " + Arrays.toString(this.refTypeSystem.getBibRefTypeNames()));
			this.refTypeDefault = null;
		}
		
		//	add style parameter group descriptions for metadata in general ...
		ParameterGroupDescription pgd = new ParameterGroupDescription("docMeta");
		pgd.setLabel("Document Metadata");
		pgd.setDescription("General parameters for locating document metadata as a whole. Not all metadata attributes need to be used in all types of publications: For instance, a template tailored to articles published in a specific journal will likely not extract any publisher or location of publishing.");
		pgd.setParamLabel("minPageId", "Skip Over Pages From Start");
		pgd.setParamDescription("minPageId", "The number of pages to skip over from the document start before searching for metadata attributes, treating first page as 0 (aimed at skipping over title pages, etc.)");
		pgd.setParamLabel("maxPageId", "Maximum Page From Start");
		pgd.setParamDescription("maxPageId", "Maximum page from document start to search for metadata attributes, treating first page as 0 (aimed at front matter data)");
		pgd.setParamLabel("maxFromEndPageId", "Maximum Page From End");
		pgd.setParamDescription("maxFromEndPageId", "Maximum page from document end to search for metadata attributes, treating last page as 1 (aimed at back matter data)");
		pgd.setParamLabel("docRefPresent", "Extract Document Reference?");
		pgd.setParamDescription("docRefPresent", "Extract a self-document reference, i.e., a bibliographic reference to the document proper?");
		pgd.setParamLabel("dataSourceMode", "How to Use External Sources of Metadata");
		pgd.setParamDescription("dataSourceMode", "Behavior of lookups in external data sources like RefBank, to complete the metadata");
		String[] dsModes = {"noLookup", "completeBasic", "completeAll", "importSelected", "importAll"};
		pgd.setParamValues("dataSourceMode", dsModes);
		pgd.setParamValueLabel("dataSourceMode", "noLookup", "Never do a lookup");
		pgd.setParamValueLabel("dataSourceMode", "completeBasic", "Only do a lookup if basic attributes missing");
		pgd.setParamValueLabel("dataSourceMode", "completeAll", "Always do a lookup, and fill empty fields with result");
		pgd.setParamValueLabel("dataSourceMode", "importSelected", "Always do a lookup, and apply peferences to fields");
		pgd.setParamValueLabel("dataSourceMode", "importAll", "Always do a lookup, and use all values from result");
		DocumentStyle.addParameterGroupDescription(pgd);
		
		//	... and individual attributes ...
		for (int f = 0; f < extractableFieldNames.length; f++) {
			if (YEAR_ANNOTATION_TYPE.equals(extractableFieldNames[f]))
				addParameterGroupDescription(extractableFieldNames[f], "Year of Publication", "Parameters for automated extraction of the year a document was published", false);
			else if (PUBLICATION_DATE_ANNOTATION_TYPE.equals(extractableFieldNames[f]))
				addParameterGroupDescription(extractableFieldNames[f], "Exact Date of Publication", "Parameters for automated extraction of the exact date a document was published", false);
			else if (SERIES_IN_JOURNAL_ANNOTATION_TYPE.equals(extractableFieldNames[f]))
				addParameterGroupDescription(extractableFieldNames[f], "Series in Journal", "Parameters for automated extraction of the series within a multi-series journal", false);
			else if (VOLUME_DESIGNATOR_ANNOTATION_TYPE.equals(extractableFieldNames[f]) || ISSUE_DESIGNATOR_ANNOTATION_TYPE.equals(extractableFieldNames[f]) || NUMERO_DESIGNATOR_ANNOTATION_TYPE.equals(extractableFieldNames[f]))
				addParameterGroupDescription(extractableFieldNames[f], (extractableFieldNames[f].substring(0, 1).toUpperCase() + extractableFieldNames[f].substring(1) + " Number"), ("Parameters for automated extraction of the " + extractableFieldNames[f] + " number of a document"), false);
			else if (VOLUME_TITLE_ANNOTATION_TYPE.equals(extractableFieldNames[f]))
				addParameterGroupDescription(extractableFieldNames[f], "Volume Title", "Parameters for automated extraction of the volume title, i.e., the title of a publication target documents are a part of (mostly for book chapters, the overall book title)", false);
			else if (AUTHOR_ANNOTATION_TYPE.equals(extractableFieldNames[f]) || EDITOR_ANNOTATION_TYPE.equals(extractableFieldNames[f])) {
				pgd = addParameterGroupDescription(extractableFieldNames[f], (extractableFieldNames[f].substring(0, 1).toUpperCase() + extractableFieldNames[f].substring(1) + "s"), ("Parameters for automated extraction of the " + extractableFieldNames[f] + "s of a document"), false);
				pgd.setParamLabel("isLastNameLast", "Names are Last Name Last");
				pgd.setParamDescription("isLastNameLast", "The order of author name parts, i.e., last name before ('Kennedy, J.F.') or after ('J.F. Kennedy') first name.");
			}
			else addParameterGroupDescription(extractableFieldNames[f], (extractableFieldNames[f].substring(0, 1).toUpperCase() + extractableFieldNames[f].substring(1)), ("Parameters for automated extraction of the " + extractableFieldNames[f] + " of a document"), false);
		}
		
		//	add parameter groups for author details (best right below authors proper)
		if ((this.authorDetails != null) && (this.authorDetails.length != 0)) {
			pgd = new TestableParameterGroupDescription("docMeta" + "." + AUTHOR_ANNOTATION_TYPE + "." + "details");
			pgd.setLabel("Author Details");
			pgd.setDescription("Parameters for automated extraction of details about document authors");
			pgd.setParamLabel("indexChars", "All Index Characters");
			pgd.setParamDescription("indexChars", "List of all index characters used for any author details (for index based association only).");
			DocumentStyle.addParameterGroupDescription(pgd);
			
			for (int d = 0; d < this.authorDetails.length; d++) {
				if (AuthorData.AUTHOR_NAME_ATTRIBUTE.equals(this.authorDetails[d]))
					continue;
				pgd = addParameterGroupDescription((AUTHOR_ANNOTATION_TYPE + "." + "details" + "." + this.authorDetails[d]), ("Author " + this.authorDetails[d].substring(0, 1).toUpperCase() + this.authorDetails[d].substring(1) + "s"), ("Parameters for automated extraction of the " + this.authorDetails[d] + "s of the authors of a document"), true);
				pgd.setParamLabel("indexChars", "Index Characters");
				pgd.setParamDescription("indexChars", ("List of all index characters used for author " + this.authorDetails[d] + "s (for index based association only, other options represent proximity based association)."));
				String[] indexCharValues = {"", "BELOW", "RIGHT", "ABOVE", "LEFT", "INLINE-LEFT", "INLINE-RIGHT"};
				pgd.setParamValues("indexChars", indexCharValues);
				pgd.setParamValueLabel("indexChars", "BELOW", "None (values below author names)");
				pgd.setParamValueLabel("indexChars", "RIGHT", "None (values to right of author names)");
				pgd.setParamValueLabel("indexChars", "ABOVE", "None (values above author names)");
				pgd.setParamValueLabel("indexChars", "LEFT", "None (values to left of author names)");
				pgd.setParamValueLabel("indexChars", "INLINE-RIGHT", "None (values in-line following author names)");
				pgd.setParamValueLabel("indexChars", "INLINE-LEFT", "None (values in-line preceding author names)");
				
				pgd.setParameterDescription("maxDistanceFromAuthorAnnots", new ValueAreaParameterDescription(pgd, "maxDistanceFromAuthorAnnots"));
				pgd.setParamLabel("maxDistanceFromAuthorAnnots", "Maximum Distance from Author Name");
				pgd.setParamDescription("maxDistanceFromAuthorAnnots", ("Maximum distance of author " + this.authorDetails[d] + "s from author names (for proximity based association, to side for right and left, down for below, up for above)."));
				
				pgd.setParameterDescription("areaPositionHorizontal", new ValueAreaParameterDescription(pgd, "areaPositionHorizontal"));
				pgd.setParamLabel("areaPositionHorizontal", "Position of Extraction Area (Horizontal)");
				pgd.setParamDescription("areaPositionHorizontal", ("Position of extraction area for " + this.authorDetails[d] + "s below or above author names (for proximity based association)"));
				String[] areaPositionHorizontalValues = {"ABSOLUTE", "RIGHT", "CENTER", "LEFT"};
				pgd.setParamValues("areaPositionHorizontal", areaPositionHorizontalValues);
				pgd.setParamValueLabel("areaPositionHorizontal", "ABSOLUTE", "Absolute");
				pgd.setParamValueLabel("areaPositionHorizontal", "RIGHT", "Relative to author name (values starting flush left with name, extending right)");
				pgd.setParamValueLabel("areaPositionHorizontal", "CENTER", "Relative to author name (values centered below or above name)");
				pgd.setParamValueLabel("areaPositionHorizontal", "LEFT", "Relative to author name (values extending left, ending flush right with name)");
				
				pgd.setParameterDescription("areaPositionVertical", new ValueAreaParameterDescription(pgd, "areaPositionVertical"));
				pgd.setParamLabel("areaPositionVertical", "Position of Extraction Area (Vertical)");
				pgd.setParamDescription("areaPositionVertical", ("Position of extraction area for " + this.authorDetails[d] + "s to right or left of author names (for proximity based association)"));
				String[] areaPositionVerticalValues = {"ABSOLUTE", "DOWN", "CENTER", "UP"};
				pgd.setParamValues("areaPositionVertical", areaPositionVerticalValues);
				pgd.setParamValueLabel("areaPositionVertical", "ABSOLUTE", "Absolute");
				pgd.setParamValueLabel("areaPositionVertical", "DOWN", "Relative to author name (values starting same height as name, extending down)");
				pgd.setParamValueLabel("areaPositionVertical", "CENTER", "Relative to author name (values centered left or right of name)");
				pgd.setParamValueLabel("areaPositionVertical", "UP", "Relative to author name (values ending same height as name, extending above)");
				
				String[] indexCharPositionValues = {"leading-superscript", "leading", "tailing-superscript", "tailing"};
				pgd.setParamLabel("indexCharPositionOnAuthorAnnots", "Index Character Position on Author Names");
				pgd.setParamDescription("indexCharPositionOnAuthorAnnots", ("Position of index characters on author names (for index based association only)."));
				pgd.setParamValues("indexCharPositionOnAuthorAnnots", indexCharPositionValues);
				pgd.setParamLabel("indexCharPositionOnDetailValues", ("Index Character Position on Author " + this.authorDetails[d].substring(0, 1).toUpperCase() + this.authorDetails[d].substring(1) + "s"));
				pgd.setParamDescription("indexCharPositionOnDetailValues", ("Position of index characters on author " + this.authorDetails[d] + "s (for index based association only)."));
				pgd.setParamValues("indexCharPositionOnDetailValues", indexCharPositionValues);
				pgd.setParamLabel("detailValueSeparator", ("Separator Character Between Author " + this.authorDetails[d].substring(0, 1).toUpperCase() + this.authorDetails[d].substring(1) + "s"));
				pgd.setParamDescription("detailValueSeparator", ("Separator character between individual author " + this.authorDetails[d] + "s (for index based association only)."));
				String[] detailValueSeparatorValues = {"", "PARAGRAPH", "LINE", "INDEX"};
				pgd.setParamValues("detailValueSeparator", detailValueSeparatorValues);
				pgd.setParamValueLabel("detailValueSeparator", "PARAGRAPH", "Values are whole paragraphs");
				pgd.setParamValueLabel("detailValueSeparator", "LINE", "Values are whole lines");
				pgd.setParamValueLabel("detailValueSeparator", "INDEX", "Values separated by index characters");
				pgd.setParamLabel("useFirstAuthorValue", "Use Only First Value for each Author?");
				pgd.setParamDescription("useFirstAuthorValue", ("Use only the first non-empty extracted author " + this.authorDetails[d] + "s per author (for index based association only)."));
				pgd.setParamLabel("useFirstDetailValue", "Use Only First Value for each Index Character?");
				pgd.setParamDescription("useFirstDetailValue", ("Use only the first non-empty extracted author " + this.authorDetails[d] + "s per index character (for index based association only)."));
				
				ParameterDescription icPd = pgd.getParameterDescription("indexChars");
				icPd.setRequired();
				icPd.addRequiredParameter(null, "area");
				icPd.addRequiredParameter(null, "indexCharPositionOnAuthorAnnots");
				icPd.addRequiredParameter(null, "indexCharPositionOnDetailValues");
				icPd.addRequiredParameter(null, "detailValueSeparator");
				icPd.addExcludedParameter(null, "areaPositionHorizontal");
				icPd.addExcludedParameter(null, "areaPositionVertical");
				icPd.addExcludedParameter(null, "maxDistanceFromAuthorAnnots");
				markInLineAssociationDependencies(icPd, "INLINE-RIGHT");
				markInLineAssociationDependencies(icPd, "INLINE-LEFT");
				markSpacialAssociationDependencies(icPd, "BELOW");
				markSpacialAssociationDependencies(icPd, "ABOVE");
				markSpacialAssociationDependencies(icPd, "RIGHT");
				markSpacialAssociationDependencies(icPd, "LEFT");
				
				if (AuthorData.AUTHOR_AFFILIATION_ATTRIBUTE.equals(this.authorDetails[d]))
					markFixedValueExcludingAll(pgd);
			}
		}
		
		//	... as well as configured identifier types ...
		for (int t = 0; t < this.refIdTypes.length; t++) {
			if (this.refIdTypes[t].length() != 0)
				addParameterGroupDescription(("ID-" + this.refIdTypes[t]), (this.refIdTypes[t] + " Identifier"), ("Parameters for automated extraction of the identifier for a document as issued by " + this.refIdTypes[t] + "."), false);
		}
		
		//	... and any document reference
		pgd = new TestableParameterGroupDescription("docMeta" + "." + "docRef");
		pgd.setLabel("Document Reference");
		pgd.setDescription("Parameters for automated extraction of the document self-reference");
		pgd.setParamLabel("maxPageId", "Maximum Page From Start");
		pgd.setParamDescription("maxPageId", "Maximum page from document start to search for the document self-reference, treating first page as 0 (aimed at front matter data).");
		pgd.setParamLabel("maxFromEndPageId", "Maximum Page From End");
		pgd.setParamDescription("maxFromEndPageId", "Maximum page from document end to search for the document self-reference, treating last page as 1 (aimed at back matter data).");
		pgd.setParamLabel("fontSize", "Font Size");
		pgd.setParamDescription("fontSize", ("The exact font size of the document self-reference (use minimum and maximum font size if variation present or to be expected)."));
		pgd.setParamLabel("minFontSize", "Minimum Font Size");
		pgd.setParamDescription("minFontSize", ("The minimum font size of the document self-reference (use only if variation present or to be expected, otherwise use exact font size)."));
		pgd.setParamLabel("maxFontSize", "Maximum Font Size");
		pgd.setParamDescription("maxFontSize", ("The maximum font size of the document self-reference (use only if variation present or to be expected, otherwise use exact font size)."));
		DocumentStyle.addParameterGroupDescription(pgd);
	}
	
	private static ParameterGroupDescription addParameterGroupDescription(String name, String label, String description, boolean isAuthorDetail) {
		ParameterGroupDescription pgd = new TestableParameterGroupDescription("docMeta" + "." + name);
		pgd.setLabel(label);
		pgd.setDescription(description);
		
		pgd.setParamLabel("maxPageId", "Maximum Page From Start");
		pgd.setParamDescription("maxPageId", "Maximum page from document start to search for the " + pgd.getLabel() + ", treating first page as 0 (aimed at front matter data).");
		pgd.setParamLabel("maxFromEndPageId", "Maximum Page From End");
		pgd.setParamDescription("maxFromEndPageId", "Maximum page from document end to search for the " + pgd.getLabel() + ", treating last page as 1 (aimed at back matter data).");
		
		if (isAuthorDetail)
			pgd.setParameterDescription("area", new ValueAreaParameterDescription(pgd, "area"));
		pgd.setParamLabel("area", "Extraction Area");
		pgd.setParamDescription("area", ("A bounding box restricting the area to search the " + pgd.getLabel() + " in (normalized to 72 DPI, leaving this parameter empty deactivates extraction of the " + pgd.getLabel() + ")."));
		if (!isAuthorDetail)
			pgd.setParamRequired("area");
		
		pgd.setParamLabel("isBold", "Value is Bold");
		pgd.setParamDescription("isBold", ("Include only words in Bold for extraction of " + pgd.getLabel() + " (for filtering)."));
		pgd.setParamLabel("isItalics", "Value is in Italics");
		pgd.setParamDescription("isItalics", ("Include only words in Italics for extraction of " + pgd.getLabel() + " (for filtering)."));
		pgd.setParamLabel("isAllCaps", "Value is in All Caps");
		pgd.setParamDescription("isAllCaps", ("Include only words in All Caps for extraction of " + pgd.getLabel() + " (for filtering)."));
		if (AUTHOR_ANNOTATION_TYPE.equals(name) || EDITOR_ANNOTATION_TYPE.equals(name)) {
			pgd.setParamLabel("isPartAllCaps", "Value is Partially in All Caps");
			pgd.setParamDescription("isPartAllCaps", ("Indicate that some part of " + pgd.getLabel() + " is in all-caps and requires normalization."));
		}
		
		pgd.setParamLabel("fontSize", "Font Size");
		pgd.setParamDescription("fontSize", ("The exact font size of the " + pgd.getLabel() + " (use minimum and maximum font size if variation present or to be expected)."));
		pgd.setParamLabel("minFontSize", "Minimum Font Size");
		pgd.setParamDescription("minFontSize", ("The minimum font size of the " + pgd.getLabel() + " (use only if variation present or to be expected, otherwise use exact font size)."));
		pgd.setParamLabel("maxFontSize", "Maximum Font Size");
		pgd.setParamDescription("maxFontSize", ("The maximum font size of the " + pgd.getLabel() + " (use only if variation present or to be expected, otherwise use exact font size)."));
		
		if (AUTHOR_ANNOTATION_TYPE.equals(name) || EDITOR_ANNOTATION_TYPE.equals(name)) {
			pgd.setParamLabel("singleLineValues", "Individual Values on Single Lines?");
			pgd.setParamDescription("singleLineValues", ("Restrict individual pattern matches for " + pgd.getLabel() + " to single lines unless line breaks matched explicitly (helpful for multi-value attributes like authors if individual values are only separated by line breaks)."));
		}
		pgd.setParamLabel("contextPattern", "Context Pattern");
		pgd.setParamDescription("contextPattern", ("A regular expression pattern extracting the " + pgd.getLabel() + " plus some context (helpful especially for numeric attributes like the volume number, e.g. to disambiguate based upon surrounding punctuation)."));
		pgd.setParamLabel("valuePattern", "Value Pattern");
		pgd.setParamDescription("valuePattern", ("A regular expression pattern extracting the actual " + pgd.getLabel() + " (if a context pattern is in use, the value pattern is only applied to what the latter matches)."));
		if (isAuthorDetail) {
			pgd.setParamLabel("filterPatterns", "Filter Patterns");
			pgd.setParamDescription("filterPatterns", ("Regular expression patterns excluding specific values as " + pgd.getLabel() + " (useful for filtering out specific values if actual values vary widely)."));
		}
		
		if (!isAuthorDetail) {
			pgd.setParamLabel("dataSourceResultMergeMode", "External Data Merge Mode");
			pgd.setParamDescription("dataSourceResultPreference", ("Indication of how to merge the " + pgd.getLabel() + " from external data sources with the ones extracted from the document."));
			String[] dsrMergeModes = {"document", "sources"};
			pgd.setParamValues("dataSourceResultMergeMode", dsrMergeModes);
			pgd.setParamValueLabel("dataSourceResultMergeMode", "document", "Prefer values extracted from document");
			pgd.setParamValueLabel("dataSourceResultMergeMode", "sources", "Prefer values from external sources");
		}
		
		if (PAGINATION_ANNOTATION_TYPE.equals(name)) {
			/* TODO_maybe add template parameter indicating preferred source of pagination:
			 * - "metadata only" (only extract via template, or take from lookup)
			 * - "prefer metadata" (extract via template, or take from lookup, and fall back to page numbers if former two fail)
			 * - "prefer page numbers" (count out from page numbers if latter given, extract via template, or take from lookup if page numbers absent)
			 * - "page numbers only" (only counted out from page numbers)
			 * 
			 * TODO_maybe add template parameter indicating how to count out start page number:
			 * - "first page" (always use first page in document, extrapolated or not)
			 * - "first explicit page number" (use number from first page that explicitly has a marked page number on it)
			 * 
			 * TODO_maybe add template parameter indicating how to count out end page number:
			 * - "last page" (always use last page in document, extrapolated or not)
			 * - "last explicit page number" (use number from last page that explicitly has a marked page number on it)
			 */
		}
		
		if (fixableFieldNames.contains(name)) {
			pgd.setParamLabel("fixedValue", "Fixed Value");
			pgd.setParamDescription("fixedValue", ("A fixed value to use as the " + pgd.getLabel() + " (mostly for journal names)."));
			markFixedValueExcludingAll(pgd);
		}
		
		DocumentStyle.addParameterGroupDescription(pgd);
		return pgd;
	}
	
	private static void markInLineAssociationDependencies(ParameterDescription icPd, String association) {
		icPd.addExcludedParameter(association, "area");
		icPd.addExcludedParameter(association, "indexCharPositionOnAuthorAnnots");
		icPd.addExcludedParameter(association, "indexCharPositionOnDetailValues");
		icPd.addExcludedParameter(association, "detailValueSeparator");
		icPd.addExcludedParameter(association, "useFirstAuthorValue");
		icPd.addExcludedParameter(association, "useFirstDetailValue");
	}
	
	private static void markSpacialAssociationDependencies(ParameterDescription icPd, String association) {
		icPd.addRequiredParameter(association, "areaPositionHorizontal");
		icPd.addRequiredParameter(association, "areaPositionVertical");
		icPd.addRequiredParameter(association, "maxDistanceFromAuthorAnnots");
		icPd.addExcludedParameter(association, "indexCharPositionOnAuthorAnnots");
		icPd.addExcludedParameter(association, "indexCharPositionOnDetailValues");
		icPd.addExcludedParameter(association, "detailValueSeparator");
		icPd.addExcludedParameter(association, "useFirstAuthorValue");
		icPd.addExcludedParameter(association, "useFirstDetailValue");
	}
	
	private static void markFixedValueExcludingAll(ParameterGroupDescription pgd) {
		ParameterDescription fvPd = pgd.getParameterDescription("fixedValue");
		if (fvPd != null) {
			String[] pns = pgd.getParameterNames();
			for (int p = 0; p < pns.length; p++) {
				if (!"fixedValue".equals(pns[p]))
					fvPd.addExcludedParameter(null, pns[p]);
			}
		}
	}
	
	private static class TestableParameterGroupDescription extends ParameterGroupDescription implements DisplayExtension, TestableElement {
		private final String localPnp;
		TestableParameterGroupDescription(String pnp) {
			super(pnp);
			this.localPnp = (pnp.startsWith("docMeta.") ? pnp.substring("docMeta.".length()) : null);
		}
		public boolean isActive() {
			return true;
		}
		public DisplayExtensionGraphics[] getExtensionGraphics(ImPage page, ImDocumentMarkupPanel idmp) {
			if (this.localPnp == null)
				return DisplayExtensionProvider.NO_DISPLAY_EXTENSION_GRAPHICS;
			ImDocumentStyle docStyle = ImDocumentStyle.getStyleFor(idmp.document);
			if (docStyle == null)
				docStyle = new ImDocumentStyle(new PropertiesData(new Properties()));
			ImDocumentStyle metaDataStyle = docStyle.getImSubset("docMeta");
			ImDocumentStyle attributeStyle = metaDataStyle.getImSubset(this.localPnp);
			if (attributeStyle.getPropertyNames().length == 0)
				return DisplayExtensionProvider.NO_DISPLAY_EXTENSION_GRAPHICS;
			
			//	observe minimum and maximum page
			int maxFromStartPageId = attributeStyle.getIntProperty("maxPageId", metaDataStyle.getIntProperty("maxPageId", 0));
			int maxFromEndPageId = attributeStyle.getIntProperty("maxFromEndPageId", metaDataStyle.getIntProperty("maxFromEndPageId", 0));
			if (page.pageId <= maxFromStartPageId) { /* in range at start */ }
			else if (page.pageId >= (idmp.document.getPageCount() - maxFromEndPageId)) { /* in range at end */ }
			else return DisplayExtensionProvider.NO_DISPLAY_EXTENSION_GRAPHICS; // page out of range
			
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
			
			//	extract current matches from page and highlight them
			ArrayList attributeValues = new ArrayList();
			boolean gotDegs;
			if (this.localPnp.startsWith(AUTHOR_ANNOTATION_TYPE + "." + "details" + ".")) {
				ImDocumentStyle allDetailsStyle = metaDataStyle.getImSubset(AUTHOR_ANNOTATION_TYPE + "." + "details");
				String detailName = this.localPnp.substring((AUTHOR_ANNOTATION_TYPE + "." + "details" + ".").length());
				gotDegs = extractAuthorDetail(null, detailName, !AuthorData.AUTHOR_AFFILIATION_ATTRIBUTE.equals(detailName), page, page.getImageDPI(), attributeStyle, allDetailsStyle, null, attributeValues, log);
			}
			else gotDegs = extractAttribute(this.localPnp, this.localPnp.startsWith("ID-"), page, page.getImageDPI(), attributeStyle, (AUTHOR_ANNOTATION_TYPE.equals(this.localPnp) || EDITOR_ANNOTATION_TYPE.equals(this.localPnp)), null, attributeValues, log);
			displayAttributeExtractionLog(docId, this.localPnp, logBaos.toByteArray());
			if (gotDegs) {
				ArrayList shapes = new ArrayList();
				for (int v = 0; v < attributeValues.size(); v++) {
					ExtractedAttributeValue eav = ((ExtractedAttributeValue) attributeValues.get(v));
					for (int w = 0; w < eav.words.length; w++)
						shapes.add(new Rectangle2D.Float(eav.words[w].bounds.left, eav.words[w].bounds.top, eav.words[w].bounds.getWidth(), eav.words[w].bounds.getHeight()));
				}
				DisplayExtensionGraphics[] degs = {new DisplayExtensionGraphics(this, idmp, page, ((Shape[]) shapes.toArray(new Shape[shapes.size()])), attributeMatchLineColor, attributeMatchLineStroke, attributeMatchFillColor) {
					public boolean isActive() {
						return true;
					}
				}};
				return degs;
			}
			
			//	nothing to show right now
			else return DisplayExtensionProvider.NO_DISPLAY_EXTENSION_GRAPHICS;
		}
		public void test(DocumentStyle paramGroup) {
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
				super("Attribute Extraction Log", false);
				
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
	
	private static class ValueAreaParameterDescription extends ParameterDescription implements DisplayExtension {
		private final String localPnp;
		public ValueAreaParameterDescription(ParameterGroupDescription pgd, String lpn) {
			super(pgd.parameterNamePrefix + "." + lpn);
			this.localPnp = (pgd.parameterNamePrefix.startsWith("docMeta.") ? pgd.parameterNamePrefix.substring("docMeta.".length()) : null);
		}
		public boolean isActive() {
			return true;
		}
		public DisplayExtensionGraphics[] getExtensionGraphics(ImPage page, ImDocumentMarkupPanel idmp) {
			ImDocumentStyle docStyle = ImDocumentStyle.getStyleFor(idmp.document);
			if (docStyle == null)
				docStyle = new ImDocumentStyle(new PropertiesData(new Properties()));
			ImDocumentStyle metaDataStyle = docStyle.getImSubset("docMeta");
			ImDocumentStyle detailStyle = metaDataStyle.getImSubset(this.localPnp);
			if (detailStyle.getPropertyNames().length == 0)
				return DisplayExtensionProvider.NO_DISPLAY_EXTENSION_GRAPHICS;
			
			//	observe minimum and maximum page
			int maxFromStartPageId = detailStyle.getIntProperty("maxPageId", metaDataStyle.getIntProperty("maxPageId", 0));
			int maxFromEndPageId = detailStyle.getIntProperty("maxFromEndPageId", metaDataStyle.getIntProperty("maxFromEndPageId", 0));
			if (page.pageId <= maxFromStartPageId) { /* in range at start */ }
			else if (page.pageId >= (idmp.document.getPageCount() - maxFromEndPageId)) { /* in range at end */ }
			else return DisplayExtensionProvider.NO_DISPLAY_EXTENSION_GRAPHICS; // page out of range
			
			//	get detail association data
			String associationData = detailStyle.getStringProperty("indexChars", null);
			
			//	highlight areas relative to author annotations
			if ("BELOW".equals(associationData) || "RIGHT".equals(associationData) || "ABOVE".equals(associationData) || "LEFT".equals(associationData)) {
				ImAnnotation[] authorAnnots = idmp.document.getAnnotations("docAuthor", page.pageId);
				
				//	nothing to work with, highlight area normally
				if (authorAnnots.length == 0) {
					if ("area".equals(this.localName))
						return this.getAreaVisualizationGraphics(page, detailStyle);
					return DisplayExtensionProvider.NO_DISPLAY_EXTENSION_GRAPHICS;
				}
				
				//	get and wrap author relative areas
				BoundingBox[] vaBounds = getAuthorDetailValueAreas(authorAnnots, page, associationData, detailStyle, page.getImageDPI(), System.out);
				ArrayList degs = new ArrayList(vaBounds.length);
				for (int a = 0; a < vaBounds.length; a++) {
					if (vaBounds[a] != null)
						degs.add(getBoundingBoxVisualizationGraphics(this, page, vaBounds[a], true));
				}
				
				//	show main bounding box as well if that is our field
				if ("area".equals(this.localName))
					degs.addAll(Arrays.asList(this.getAreaVisualizationGraphics(page, detailStyle)));
				
				//	anything to show?
				if (degs.size() != 0)
					return ((DisplayExtensionGraphics[]) degs.toArray(new DisplayExtensionGraphics[degs.size()]));
			}
			
			//	visualize bounding box normally
			if ("area".equals(this.localName))
				return this.getAreaVisualizationGraphics(page, detailStyle);
			
			//	display nothing for other involved fields
			return DisplayExtensionProvider.NO_DISPLAY_EXTENSION_GRAPHICS;
		}
		private DisplayExtensionGraphics[] getAreaVisualizationGraphics(ImPage page, ImDocumentStyle detailStyle) {
			BoundingBox area = detailStyle.getBoxProperty("area", null, page.getImageDPI());
			if (area == null)
				return DisplayExtensionProvider.NO_DISPLAY_EXTENSION_GRAPHICS;
			DisplayExtensionGraphics[] degs = {getBoundingBoxVisualizationGraphics(this, page, area, false)};
			return degs;
		}
		
		private static final Color boundingBoxLineColor = Color.GREEN;
		private static final BasicStroke boundingBoxLineStroke = new BasicStroke(3);
		private static final Color boundingBoxFillColor = new Color(boundingBoxLineColor.getRed(), boundingBoxLineColor.getGreen(), boundingBoxLineColor.getBlue(), 64);
		private static final Color relBoundingBoxLineColor = Color.BLUE;
		private static final BasicStroke relBoundingBoxLineStroke = new BasicStroke(1);
		private static final Color relBoundingBoxFillColor = new Color(relBoundingBoxLineColor.getRed(), relBoundingBoxLineColor.getGreen(), relBoundingBoxLineColor.getBlue(), 64);
		private static DisplayExtensionGraphics getBoundingBoxVisualizationGraphics(ValueAreaParameterDescription parent, ImPage page, BoundingBox bb, boolean isRelative) {
			Shape[] shapes = {new Rectangle2D.Float(bb.left, bb.top, bb.getWidth(), bb.getHeight())};
			return new DisplayExtensionGraphics(parent, null, page, shapes, 
					(isRelative ? relBoundingBoxLineColor : boundingBoxLineColor), 
					(isRelative ? relBoundingBoxLineStroke : boundingBoxLineStroke), 
					(isRelative ? relBoundingBoxFillColor : boundingBoxFillColor)
			) {
				public boolean isActive() {
					return true;
				}
			};
		}
	}
	
	private void checkRefDataSources() {
		if (this.refDataSources != null)
			return;
		BibRefDataSource[] refDataSources = BibRefDataSource.getDataSources();
		if (refDataSources.length != 0)
			this.refDataSources = refDataSources;
	}
	
	private void logInMasterConfiguration(String output) {
		if (this.inMasterConfiguration)
			System.out.println(output);
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
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getEditMenuItemNames()
	 */
	public String[] getEditMenuItemNames() {
		String[] emins = {META_DATA_EDITOR_IMT_NAME};
		return emins;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider#getToolsMenuItemNames()
	 */
	public String[] getToolsMenuItemNames() {
		String[] tmins = {META_DATA_ADDER_IMT_NAME};
		return tmins;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider#getActions(de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(final ImWord start, final ImWord end, final ImDocumentMarkupPanel idmp) {
		
		//	provide actions only in the first and last couple of pages
//		if (start.pageId == 0) {}
//		else if ((start.pageId == 1) && (idmp.document.getPageCount() >= 15)) {}
//		else if ((start.pageId == 2) && (idmp.document.getPageCount() >= 100)) {}
//		else if (start.pageId == 1) {}
//		else if (start.pageId == 2) {}
//		else if ((idmp.document.getPageCount() - start.pageId) == 1) {}
//		else if (((idmp.document.getPageCount() - start.pageId) == 2) && (idmp.document.getPageCount() >= 15)) {}
//		else if ((idmp.document.getPageCount() - start.pageId) == 2) {}
//		else if ((idmp.document.getPageCount() - start.pageId) == 3) {}
		if (start.pageId < 3) {}
		else if ((idmp.document.getPageCount() - start.pageId) <= 3) {}
		else return null;
		
		//	allow extraction only in single text streams
		if (!start.getTextStreamId().equals(end.getTextStreamId()))
			return null;
		
		//	get existing bibliographic metadata
		final RefData ref = getDocumentRefData(idmp.document);
		
		//	add selection actions that allow using selected text as bibliographic attribute
		SelectionAction[] actions = {
			new SelectionAction("useMetadata", "Use in Metadata ...", ("Use '" + this.getSelectionShortValue(start, end) + "' in the bibliographic metadata of the document")) {
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					JMenu m = new JMenu(this.label);
					JMenuItem mi;
					for (int f = 0; f < extractableFieldNames.length; f++) {
						final String efn = extractableFieldNames[f];
						String cfv = getCurrentValue(efn, ref);
						mi = new JMenuItem(efn.substring(0, 1).toUpperCase() + efn.substring(1) + " (" + ((cfv == null) ? "-" : "+") + ")");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								useSelection(start, end, efn, false, ref, idmp);
							}
						});
						mi.setToolTipText("Use as " + efn + ((cfv == null) ? "" : (" (currently '" + cfv + "')")));
						mi.setBackground(idmp.getAnnotationColor("doc" + Character.toUpperCase(efn.charAt(0)) + efn.substring(1)));
						m.add(mi);
						if (!AUTHOR_ANNOTATION_TYPE.equals(extractableFieldNames[f]))
							continue;
						String[] docAuthors = ref.getAttributeValues(AUTHOR_ANNOTATION_TYPE);
						if ((docAuthors == null) || (docAuthors.length == 0))
							continue;
						JMenuItem admi;
						for (int d = 0; d < authorDetails.length; d++) {
							if (AuthorData.AUTHOR_NAME_ATTRIBUTE.equals(authorDetails[d]))
								continue;
							final String eadn = authorDetails[d];
							final boolean isAuthorIdentifier = (!AuthorData.AUTHOR_AFFILIATION_ATTRIBUTE.equals(eadn)); 
							JMenu adm = new JMenu("Author " + eadn.substring(0, 1).toUpperCase() + eadn.substring(1));
							adm.setBackground(idmp.getAnnotationColor("doc" + "Author" + Character.toUpperCase(eadn.charAt(0)) + eadn.substring(1)));
							for (int a = 0; a < docAuthors.length; a++) {
								final String dan = docAuthors[a];
								String cadv = ref.getAuthorAttribute(docAuthors[a], authorDetails[d]);
								admi = new JMenuItem(docAuthors[a] + " (" + ((cadv == null) ? "-" : "+") + ")");
								admi.addActionListener(new ActionListener() {
									public void actionPerformed(ActionEvent ae) {
										useAuthorDetailSelection(start, end, dan, eadn, isAuthorIdentifier, ref, idmp);
									}
								});
								admi.setToolTipText("Use as " + eadn + " for " + docAuthors[a] + ((cadv == null) ? "" : (" (currently '" + cadv + "')")));
								admi.setBackground(idmp.getAnnotationColor("doc" + "Author" + Character.toUpperCase(eadn.charAt(0)) + eadn.substring(1)));
								adm.add(admi);
							}
							m.add(adm);
						}
					}
					if (refIdTypes.length != 0)
						m.addSeparator();
					for (int t = 0; t < refIdTypes.length; t++) {
						if (refIdTypes[t].length() == 0)
							continue;
						final String idt = refIdTypes[t];
						String idv = ref.getIdentifier(idt);
						mi = new JMenuItem("ID-" + idt + " (" + ((idv == null) ? "-" : "+") + ")");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								useSelection(start, end, idt, true, ref, idmp);
							}
						});
						mi.setToolTipText("Use as ID-" + idt + ((idv == null) ? "" : (" (currently '" + idv + "')")));
						mi.setBackground(idmp.getAnnotationColor("docId" + idt));
						m.add(mi);
					}
					return m;
				}
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
			}
		};
		return actions;
	}
	
	private HashMap refDataByDocId = new LinkedHashMap(5, 0.9f) {
		protected boolean removeEldestEntry(Entry eldest) {
			return (this.size() > 4);
		}
	};
	private RefData getDocumentRefData(ImDocument doc) {
		RefData ref = ((RefData) this.refDataByDocId.get(doc.docId));
		if (ref == null) {
			ref = BibRefUtils.modsAttributesToRefData(doc);
			this.refDataByDocId.put(doc.docId, ref);
		}
		return ref;
	}
	
	private String getSelectionShortValue(ImWord start, ImWord end) {
		if (start == end)
			return start.getString();
		else if (start.getNextWord() == end)
			return (start.getString() + (Gamta.insertSpace(start.getString(), end.getString()) ? " " : "") + end.getString());
		else return (start.getString() + " ... " + end.getString());
	}
	
	private String getCurrentValue(String type, RefData ref) {
		if (!ref.hasAttribute(type))
			return null;
		else if (AUTHOR_ANNOTATION_TYPE.equals(type) || EDITOR_ANNOTATION_TYPE.equals(type))
			return ref.getAttributeValueString(type, " & ");
		else return ref.getAttribute(type);
	}
	
	private void useSelection(ImWord start, ImWord end, String type, boolean isIdentifier, RefData ref, ImDocumentMarkupPanel idmp) {
		idmp.beginAtomicAction("Mark " + (isIdentifier ? "ID-" : "") + type);
		
		//	assemble annotated value
		String value = ImUtils.getString(start, end, true);
		
		//	sanitize attribute value, and check validity along the way:
		//	- check values for numeric attributes
		//	- translate Roman numbers
		//	- try to enforce <lastName>, <firstName> format for person name attributes
		String sValue = sanitizeAttributeValue(type, value, isIdentifier);
		if (sValue == null) {
			//	TODO maybe prompt for permission only ?!?
			DialogFactory.alert(("'" + value + "' is not a valid value for document " + type), ("Invalid Document " + Character.toUpperCase(type.charAt(0)) + type.substring(1)), JOptionPane.ERROR_MESSAGE);
			return;
		}
		else value = sValue;
		
		//	generate annotation type
		String annotType;
		if (isIdentifier)
			annotType = ("docId" + type);
		else annotType = ("doc" + Character.toUpperCase(type.charAt(0)) + type.substring(1));
		
		//	check existing annotations
		ImAnnotation exAnnot = null;
		ImAnnotation[] exAnnots = idmp.document.getAnnotations(annotType);
		for (int a = 0; a < exAnnots.length; a++) {
			
			//	this one is exactly what we intend to annotate, hold on to it
			if ((exAnnots[a].getFirstWord() == start) && (exAnnots[a].getLastWord() == end))
				exAnnot = exAnnots[a];
			
			//	only remove overlapping annotations for list-valued attributes (author and editor)
			else if (AUTHOR_ANNOTATION_TYPE.equals(type) || EDITOR_ANNOTATION_TYPE.equals(type)) {
				if (!start.getTextStreamId().equals(exAnnots[a].getFirstWord().getTextStreamId())) { /* different text streams, for whatever reason */ }
				else if (ImUtils.textStreamOrder.compare(end, exAnnots[a].getFirstWord()) <= 0) { /* selection lies before existing annotation */ }
				else if (ImUtils.textStreamOrder.compare(exAnnots[a].getLastWord(), start) <= 0) { /* selection lies after existing annotation */ }
				else idmp.document.removeAnnotation(exAnnots[a]);
			}
			
			//	clean up existing annotation for single-value attributes
			else idmp.document.removeAnnotation(exAnnots[a]);
		}
		
		//	annotate selection (unless done before)
		if (exAnnot == null)
			idmp.document.addAnnotation(start, end, annotType);
		
		//	set attribute
		if (isIdentifier)
			ref.setIdentifier(type, value.replaceAll("\\s", ""));
		else if (AUTHOR_ANNOTATION_TYPE.equals(type) || EDITOR_ANNOTATION_TYPE.equals(type)) {
			exAnnots = idmp.document.getAnnotations(annotType);
			for (int a = 0; a < exAnnots.length; a++) {
				String exAnnotValue = ImUtils.getString(exAnnots[a].getFirstWord(), exAnnots[a].getLastWord(), true);
				String sExAnnotValue = sanitizeAttributeValue(type, exAnnotValue, isIdentifier);
				if (a == 0)
					ref.setAttribute(type, ((sExAnnotValue == null) ? exAnnotValue : sExAnnotValue));
				else ref.addAttribute(type, ((sExAnnotValue == null) ? exAnnotValue : sExAnnotValue));
			}
		}
		else if (PUBLICATION_DATE_ANNOTATION_TYPE.equals(type)) {
			ref.setAttribute(type, value);
			if (!ref.hasAttribute(YEAR_ANNOTATION_TYPE))
				ref.setAttribute(YEAR_ANNOTATION_TYPE, value.substring(0, 4));
		}
		else ref.setAttribute(type, value);
		
		//	store attributes, and invalidate cache
		BibRefUtils.toModsAttributes(ref, idmp.document);
		if (AUTHOR_ANNOTATION_TYPE.equals(type))
			setAttribute(idmp.document, DOCUMENT_AUTHOR_ATTRIBUTE, ref, AUTHOR_ANNOTATION_TYPE, false);
		if (TITLE_ANNOTATION_TYPE.equals(type))
			setAttribute(idmp.document, DOCUMENT_TITLE_ATTRIBUTE, ref, TITLE_ANNOTATION_TYPE, true);
		if (YEAR_ANNOTATION_TYPE.equals(type))
			setAttribute(idmp.document, DOCUMENT_DATE_ATTRIBUTE, ref, YEAR_ANNOTATION_TYPE, true);
		this.refDataByDocId.remove(idmp.document.docId);
		
		//	mark document reference if metadata complete
		if (this.refTypeSystem.classify(ref) != null)
			this.annotateDocReference(idmp.document, ref);
		
		//	finally ...
		idmp.endAtomicAction();
		
		//	make changes show
		idmp.setAnnotationsPainted(annotType, true);
		idmp.validate();
		idmp.repaint();
	}
	
	private void useAuthorDetailSelection(ImWord start, ImWord end, String authorName, String type, boolean isIdentifier, RefData ref, ImDocumentMarkupPanel idmp) {
		idmp.beginAtomicAction("Mark Author " + Character.toUpperCase(type.charAt(0)) + type.substring(1));
		
		//	assemble annotated value
		String value = ImUtils.getString(start, end, true);
		
		//	TODO sanitize attribute value, and check validity along the way
		//	TODO somehow validate identifier (and configure patterns in the first place !!!)
//		String sValue = sanitizeAttributeValue(type, value, isIdentifier);
//		if (sValue == null) {
//			//	TODO maybe prompt for permission only ?!?
//			DialogFactory.alert(("'" + value + "' is not a valid value for document " + type), ("Invalid Document " + Character.toUpperCase(type.charAt(0)) + type.substring(1)), JOptionPane.ERROR_MESSAGE);
//			return;
//		}
//		else value = sValue;
		
		//	generate annotation type
		String annotType = ("docAuthor" + Character.toUpperCase(type.charAt(0)) + type.substring(1));
		
		//	check existing annotations
		ImAnnotation exAnnot = null;
		ImAnnotation[] exAnnots = idmp.document.getAnnotations(annotType);
		for (int a = 0; a < exAnnots.length; a++) {
			
			//	this one is exactly what we intend to annotate, hold on to it
			if ((exAnnots[a].getFirstWord() == start) && (exAnnots[a].getLastWord() == end))
				exAnnot = exAnnots[a];
			
			//	only remove overlapping annotations, as author details are list-valued
			else {
				if (!start.getTextStreamId().equals(exAnnots[a].getFirstWord().getTextStreamId())) { /* different text streams, for whatever reason */ }
				else if (ImUtils.textStreamOrder.compare(end, exAnnots[a].getFirstWord()) <= 0) { /* selection lies before existing annotation */ }
				else if (ImUtils.textStreamOrder.compare(exAnnots[a].getLastWord(), start) <= 0) { /* selection lies after existing annotation */ }
				else idmp.document.removeAnnotation(exAnnots[a]);
			}
		}
		
		//	annotate selection (unless done before)
		if (exAnnot == null)
			idmp.document.addAnnotation(start, end, annotType);
		
		//	set attribute
		if (isIdentifier)
			ref.setAuthorAttribute(authorName, type, value.replaceAll("\\s", ""));
		else ref.setAuthorAttribute(authorName, type, value);
		
		//	store attributes, and invalidate cache
		BibRefUtils.toModsAttributes(ref, idmp.document);
		this.refDataByDocId.remove(idmp.document.docId);
		
		//	finally ...
		idmp.endAtomicAction();
		
		//	make changes show
		idmp.setAnnotationsPainted(annotType, true);
		idmp.validate();
		idmp.repaint();
	}
	
	private static String sanitizeAttributeValue(String type, String value, boolean isIdentifier) {
//		
//		//	normalize dashes and whitespace
//		String sValue = sanitizeString(value, (AUTHOR_ANNOTATION_TYPE.equals(type) || EDITOR_ANNOTATION_TYPE.equals(type)), isIdentifier);
		
		//	check numeric attributes, translating Roman numbers in the process
		if (YEAR_ANNOTATION_TYPE.equals(type)) {
//			sValue = sValue.replaceAll("\\s", "");
//			sValue = removeLeadingZeros(sValue);
//			if (StringUtils.isRomanNumber(sValue))
//				sValue = ("" + StringUtils.parseRomanNumber(sValue));
//			return (sValue.matches("[12][0-9]{3}") ? sValue : null);
			return BibRefUtils.sanitizeAttributeValue(type, value);
		}
		else if (PUBLICATION_DATE_ANNOTATION_TYPE.equals(type)) {
//			String sDate = DateTimeUtils.parseTextDate(sValue);
//			if (sDate != null)
//				return sDate;
//			
//			//	substitute 16 as the day to get month parsed
//			String tsValue;
//			if (sValue.matches("[12][0-9]{3}.*"))
//				tsValue = (sValue + " 16"); // add day tailing if year leading
//			else if (sValue.matches(".*?[12][0-9]{3}"))
//				tsValue = ("16 " + sValue); // add day leading if year tailing
//			else return sDate;
//			String tsDate = DateTimeUtils.parseTextDate(tsValue);
//			if (tsDate == null)
//				return sDate;
//			
//			//	extract month and determine last day
//			String sMonth = tsDate.substring("1999-".length(), "1999-12".length());
//			String sDay;
//			if ("02".equals(sMonth))
//				sDay = "28";
//			else if ("04 06 09 11".indexOf(sMonth) != -1)
//				sDay = "30";
//			else sDay = "31";
//			String rsValue;
//			if (sValue.matches("[12][0-9]{3}.*"))
//				rsValue = (sValue + " " + sDay); // add day tailing if year leading
//			else if (sValue.matches(".*?[12][0-9]{3}"))
//				rsValue = (sDay + " " + sValue); // add day leading if year tailing
//			else return sDate;
//			
//			//	finally ...
//			return DateTimeUtils.parseTextDate(StringUtils.normalizeString(rsValue));
			return BibRefUtils.sanitizeAttributeValue(type, value);
		}
		else if (VOLUME_DESIGNATOR_ANNOTATION_TYPE.equals(type) || ISSUE_DESIGNATOR_ANNOTATION_TYPE.equals(type) || NUMERO_DESIGNATOR_ANNOTATION_TYPE.equals(type)) {
//			sValue = sValue.replaceAll("\\s", "");
//			String sValuePrefix = "";
//			if (sValue.startsWith("e") || sValue.startsWith("s") || sValue.startsWith("E") || sValue.startsWith("S")) {
//				sValuePrefix = sValue.substring(0,1);
//				sValue = sValue.substring(1);
//			}
//			if (sValue.indexOf('-') == -1) {
//				sValue = removeLeadingZeros(sValue);
//				return ((sValue.matches("[1-9][0-9]*") || StringUtils.isRomanNumber(sValue)) ? (sValuePrefix + sValue) : null);
//			}
//			else {
//				String[] valueParts = sValue.split("\\s*\\-\\s*");
//				if (valueParts.length != 2)
//					return null;
//				String fpd = sanitizeAttributeValue(type, valueParts[0], isIdentifier);
//				if (fpd == null)
//					return null;
//				String lpd = sanitizeAttributeValue(type, valueParts[1], isIdentifier);
//				if (lpd == null)
//					return null;
//				if ((fpd.matches("[1-9][0-9]*")) && (lpd.length() < fpd.length()))
//					lpd = (fpd.substring(0, (fpd.length() - lpd.length())) + lpd);
//				return (sValuePrefix + fpd + "-" + lpd);
//			}
			return BibRefUtils.sanitizeAttributeValue(type, value);
		}
		else if (PAGINATION_ANNOTATION_TYPE.equals(type)) {
//			sValue = sValue.replaceAll("\\s", "");
//			if (sValue.indexOf('-') == -1) {
//				sValue = removeLeadingZeros(sValue);
//				if (StringUtils.isRomanNumber(sValue))
//					sValue = ("" + StringUtils.parseRomanNumber(sValue));
//				return (sValue.matches("e?[1-9][0-9]*") ? sValue : null);
//			}
//			else if (sValue.indexOf('-') == sValue.lastIndexOf('-')) {
//				String[] valueParts = sValue.split("\\s*\\-\\s*");
//				if (valueParts.length != 2)
//					return null;
//				String fpn = sanitizeAttributeValue(type, valueParts[0], isIdentifier);
//				if (fpn == null)
//					return null;
//				String lpn = sanitizeAttributeValue(type, valueParts[1], isIdentifier);
//				if (lpn == null)
//					return null;
//				if (lpn.length() < fpn.length())
//					lpn = (fpn.substring(0, (fpn.length() - lpn.length())) + lpn);
//				return (fpn + "-" + lpn);
//			}
//			else return null;
			return BibRefUtils.sanitizeAttributeValue(type, value);
		}
		
		//	for person name attributes (author and editor), try and convert to <lastName>, <firstName>
		else if (AUTHOR_ANNOTATION_TYPE.equals(type) || EDITOR_ANNOTATION_TYPE.equals(type)) {
			String sValue = sanitizeString(value, true, isIdentifier);
			return flipNameParts(sValue, value);
		}
		
		//	return normalized value for all other attributes
		else return sanitizeString(value, false, isIdentifier);
	}
//	
//	private static String removeLeadingZeros(String value) {
//		if (value == null)
//			return null;
//		value = value.trim();
//		while (value.startsWith("0"))
//			value = value.substring("0".length()).trim();
//		return value;
//	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, ImPage page, ImDocumentMarkupPanel idmp) {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		if (META_DATA_EDITOR_IMT_NAME.equals(name))
			return this.metaDataEditor;
		else if (META_DATA_ADDER_IMT_NAME.equals(name))
			return this.metaDataAdder;
		else return null;
	}
	
	private static String[] extractableFieldNames = {
		AUTHOR_ANNOTATION_TYPE,
		TITLE_ANNOTATION_TYPE,
		YEAR_ANNOTATION_TYPE,
		PUBLICATION_DATE_ANNOTATION_TYPE,
		PAGINATION_ANNOTATION_TYPE,
		JOURNAL_NAME_ANNOTATION_TYPE,
		SERIES_IN_JOURNAL_ANNOTATION_TYPE,
		VOLUME_DESIGNATOR_ANNOTATION_TYPE,
		ISSUE_DESIGNATOR_ANNOTATION_TYPE,
		NUMERO_DESIGNATOR_ANNOTATION_TYPE,
		PUBLISHER_ANNOTATION_TYPE,
		LOCATION_ANNOTATION_TYPE,
		EDITOR_ANNOTATION_TYPE,
		VOLUME_TITLE_ANNOTATION_TYPE,
		PUBLICATION_URL_ANNOTATION_TYPE,
	};
	
	private static Set fixableFieldNames = Collections.synchronizedSet(new HashSet());
	static {
		fixableFieldNames.add(JOURNAL_NAME_ANNOTATION_TYPE);
		fixableFieldNames.add(SERIES_IN_JOURNAL_ANNOTATION_TYPE);
		fixableFieldNames.add(PUBLISHER_ANNOTATION_TYPE);
		fixableFieldNames.add(LOCATION_ANNOTATION_TYPE);
		fixableFieldNames.add(YEAR_ANNOTATION_TYPE);
		fixableFieldNames.add(EDITOR_ANNOTATION_TYPE);
		fixableFieldNames.add("ID-ISSN");
		fixableFieldNames.add(AuthorData.AUTHOR_AFFILIATION_ATTRIBUTE);
		fixableFieldNames.add(AUTHOR_ANNOTATION_TYPE + ".details." + AuthorData.AUTHOR_AFFILIATION_ATTRIBUTE);
	}
	
	private class MetaDataEditor implements ImageMarkupTool, WebDocumentViewer {
		private String name;
		MetaDataEditor(String name) {
			this.name = name;
		}
		public String getLabel() {
			if (META_DATA_EDITOR_IMT_NAME.equals(this.name))
				return "Edit Document Metadata";
			else if (META_DATA_ADDER_IMT_NAME.equals(this.name))
				return "Add Document Metadata";
			else return null;
		}
		public String getTooltip() {
			if (META_DATA_EDITOR_IMT_NAME.equals(this.name))
				return "Edit the bibliographic metadata of the document";
			else if (META_DATA_ADDER_IMT_NAME.equals(this.name))
				return "Import or extract the bibliographic metadata of the document";
			else return null;
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			extractDocumentMetaData(doc, (idmp != null), pm);
		}
		
		//	return a web based view controller
		public WebDocumentView getWebDocumentView(String baseUrl) {
			checkRefDataSources();
			return new WebDocumentView(this, baseUrl) {
				private RefData ref;
				private HashMap attributesToValues = new HashMap();
				protected void preProcess(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp) {
					
					//	get bibliographic data from document
					this.ref = getRefData(doc, getMetaDataStyle(doc), this.attributesToValues, true);
				}
				protected void postProcess(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp) {
					
					//	store bibliographic data if it's valid and not canceled
					if (this.ref != null)
						storeRefData(doc, this.ref, this.attributesToValues, false);
				}
				public Reader getViewBasePage() {
					//	try configured base page first ...
					try {
						return new BufferedReader(new InputStreamReader(dataProvider.getInputStream("webView.html"), "UTF-8"));
					}
					catch (IOException ioe) {
						ioe.printStackTrace(System.out);
					}
					//	... and then build-in one ...
					try {
						InputStream vbpIn = this.getClass().getClassLoader().getResourceAsStream("webView.html");
						if (vbpIn != null)
							return new BufferedReader(new InputStreamReader(vbpIn, "UTF-8"));
					}
					catch (IOException ioe) {
						ioe.printStackTrace(System.out);
					}
					//	... before falling back to default
					return super.getViewBasePage();
				}
				public HtmlPageBuilder getViewPageBuilder(HtmlPageBuilderHost host, HttpServletRequest request, HttpServletResponse response) throws IOException {
					
					//	wait for pre-processing to finish (we can do that without locking, as pre-processing is quick)
					while (this.ref == null) try {
						Thread.sleep(10);
					} catch (InterruptedException ie) {}
					
					//	return page builder
					return new HtmlPageBuilder(host, request, response) {
						protected String[] getOnloadCalls() {
							String[] olcs = { "bibRefEditor_setRef(theBibRef);" };
							return olcs;
						}
						protected boolean includeJavaScriptDomHelpers() {
							return true;
						}
						protected void include(String type, String tag) throws IOException {
							if ("includeBody".equals(type)) {
								
								//	use this for style development !!!
								if (this.host.findFile("WebDocumentView.dev.html") != null)
									this.includeFile("WebDocumentView.dev.html");
								
								//	open dialog content
								this.writeLine("<div class=\"bibDataDialog\">");
								
								//	add title
								this.writeLine("<div class=\"bibDataTitle\" style=\"font-weight: bold; text-align: center;\">Edit Document Metadata</div>");
								
								//	add reference editor form
								BibRefEditorFormHandler.createHtmlForm(this.asWriter(), false, refTypeSystem, refIdTypes);
								
								//	add Validate, Search (if we have a RefBank uplink), OK, Cancel, and Reset buttons
								this.writeLine("<div class=\"bibDataButtons\" style=\"text-align: center;\">");
								this.writeLine("<button class=\"bibDataButton\" onclick=\"return validateRefData();\">Validate</button>");
//								if (refBankClient != null)
								if (refDataSources != null)
									this.writeLine("<button class=\"bibDataButton\" onclick=\"return searchRefData();\">Search</button>");
								this.writeLine("<button class=\"bibDataButton\" onclick=\"return submitRefData('OK');\">OK</button>");
								this.writeLine("<button class=\"bibDataButton\" onclick=\"return submitRefData('C');\">Cancel</button>");
								this.writeLine("<button class=\"bibDataButton\" onclick=\"window.location.reload();\">Reset</button>");
								this.writeLine("</div>");
								
								this.writeLine("</div>");
								this.writeLine("<script id=\"dynamicSearchScript\" type=\"text/javascript\" src=\"toBeSetDynamically\"></script>");
							}
							else super.include(type, tag);
						}
						protected void writePageHeadExtensions() throws IOException {
							BibRefEditorFormHandler.writeJavaScripts(this.asWriter(), refTypeSystem, refIdTypes);
							this.writeLine("<script type=\"text/javascript\">");
							
							//	add validation function
							this.writeLine("function validateRefData() {");
							this.writeLine("  var errors = bibRefEditor_getRefErrors();");
							this.writeLine("  var message = '';");
							this.writeLine("  if (errors == null)");
							this.writeLine("    message = 'The bibliographic metadata is valid.';");
							this.writeLine("  else {");
							this.writeLine("    message = 'The bibliographic metadata has errors:';");
							this.writeLine("    for (var e = 0;; e++) {");
							this.writeLine("      if (!errors['' + e])");
							this.writeLine("        break;");
							this.writeLine("      message += '\\r\\n';");
							this.writeLine("      message += ('- ' + errors['' + e]);");
							this.writeLine("    }");
							this.writeLine("  }");
							this.writeLine("  alert(message);");
							this.writeLine("}");
							
							//	add search functions
//							if (refBankClient != null) {
							if (refDataSources != null) {
								this.writeLine("var searchRefDataOverlay = null;");
								this.writeLine("var searchRefDataDialog = null;");
								this.writeLine("var searchRefDataLabel = null;");
								this.writeLine("var searchRefDataResultList = null;");
								this.writeLine("function searchRefData() {");
								this.writeLine("  var ref = bibRefEditor_getRef();");
								this.writeLine("  var params = '';");
								this.writeLine("  for (var an in ref)");
								this.writeLine("    params += ('&' + an + '=' + encodeURIComponent(ref[an]));");
								this.writeLine("  if (params == '') {");
								this.writeLine("    showAlertDialog(('Please specify one or more attributes as search values.\\r\\nA part of the title or last name of a author will do,\\r\\nas will the journal name and year, or one of the identifiers.'), 'No Attributes To Search For', 0);");
								this.writeLine("    return;");
								this.writeLine("  }");
								this.writeLine("  var searchRefDataTitle = newElement('div', null, 'bibDataSearchTitle', 'Search Bibliographic Data');");
								this.writeLine("  searchRefDataLabel = newElement('div', null, 'bibDataSearchLabel', 'Searching ...');");
								this.writeLine("  searchRefDataResultList = newElement('div', null, 'bibDataSearchResultList', null);");
								this.writeLine("  searchRefDataResultList.appendChild(searchRefDataLabel);");
								this.writeLine("  var searchRefDataCancel = newElement('button', null, 'bibDataSearchCancel', 'Cancel');");
								this.writeLine("  searchRefDataCancel.onclick = function() {");
								this.writeLine("    removeElement(searchRefDataOverlay);");
								this.writeLine("    searchRefDataOverlay = null;");
								this.writeLine("    searchRefDataDialog = null;");
								this.writeLine("    searchRefDataLabel = null;");
								this.writeLine("    searchRefDataResultList = null;");
								this.writeLine("  };");
								this.writeLine("  var searchRefDataButtons = newElement('div', null, 'bibDataSearchButtons', null);");
								this.writeLine("  searchRefDataButtons.appendChild(searchRefDataCancel);");
								this.writeLine("  searchRefDataDialog = newElement('div', null, 'bibDataSearchResultDialog', null);");
								this.writeLine("  searchRefDataDialog.appendChild(searchRefDataTitle);");
								this.writeLine("  searchRefDataDialog.appendChild(searchRefDataResultList);");
								this.writeLine("  searchRefDataDialog.appendChild(searchRefDataButtons);");
								this.writeLine("  searchRefDataOverlay = getOverlay('searchRefDataOverlay', null, true);");
								this.writeLine("  searchRefDataOverlay.appendChild(searchRefDataDialog);");
								this.writeLine("  var dss = getById('dynamicSearchScript');");
								this.writeLine("  var dssp = dss.parentNode;");
								this.writeLine("  removeElement(dss);");
								this.writeLine("  var dssSrc = ('" + baseUrl + "searchRefs.js?time=' + (new Date()).getTime() + params);");
								this.writeLine("  dss = newElement('script', 'dynamicSearchScript');");
								this.writeLine("  dss.type = 'text/javascript';");
								this.writeLine("  dss.src = dssSrc;");
								this.writeLine("  dssp.appendChild(dss);");
								this.writeLine("}");
								this.writeLine("function searchRefData_emptyResult() {");
								this.writeLine("  while (searchRefDataLabel.firstChild)");
								this.writeLine("    searchRefDataLabel.removeChild(searchRefDataLabel.firstChild);");
								this.writeLine("  searchRefDataLabel.appendChild(document.createTextNode('Your search did not return any results, sorry.'));");
								this.writeLine("}");
								this.writeLine("function searchRefData_showResult(ref) {");
								this.writeLine("  if (searchRefDataLabel != null) {");
								this.writeLine("    removeElement(searchRefDataLabel);");
								this.writeLine("    searchRefDataLabel = null;");
								this.writeLine("  }");
								this.writeLine("  var refDiv = newElement('div', null, 'bibDataSearchResult', ref.displayString);");
								this.writeLine("  refDiv.onclick = function() {");
								this.writeLine("    bibRefEditor_setRef(ref);");
								this.writeLine("    removeElement(searchRefDataOverlay);");
								this.writeLine("    searchRefDataOverlay = null;");
								this.writeLine("    searchRefDataDialog = null;");
								this.writeLine("    searchRefDataLabel = null;");
								this.writeLine("    searchRefDataResultList = null;");
								this.writeLine("  };");
								this.writeLine("  searchRefDataResultList.appendChild(refDiv);");
								this.writeLine("}");
							}
							
							//	add reference submission function, taking OK or Cancel as arguments
							this.writeLine("function submitRefData(status) {");
							//	if status is 'OK', check for errors, and disallow submission if there are any
							this.writeLine("  if (status == 'OK') {");
							this.writeLine("    var errors = bibRefEditor_getRefErrors();");
							this.writeLine("    if (errors != null) {");
							this.writeLine("      var errorMessage = 'The bibliographic metadata has errors:';");
							this.writeLine("      for (var e = 0;; e++) {");
							this.writeLine("        if (!errors['' + e])");
							this.writeLine("          break;");
							this.writeLine("        errorMessage += '\\r\\n';");
							this.writeLine("        errorMessage += ('- ' + errors['' + e]);");
							this.writeLine("      }");
							this.writeLine("      alert(errorMessage);");
							this.writeLine("      return;");
							this.writeLine("    }");
							this.writeLine("  }");
							//	create form
							this.writeLine("  var refDataForm = newElement('form', null, null, null);");
							this.writeLine("  refDataForm.method = 'POST';");
							this.writeLine("  refDataForm.action = '" + baseUrl + "submitRef';");
							//	have reference data added if submitting
							this.writeLine("  if (status == 'OK')");
							this.writeLine("    bibRefEditor_addRefAttributeInputs(refDataForm);");
							//	add status to form, also in hidden field
							this.writeLine("  var statusField = newElement('input', null, null, null);");
							this.writeLine("  statusField.type = 'hidden';");
							this.writeLine("  statusField.name = 'status';");
							this.writeLine("  statusField.value = status;");
							this.writeLine("  refDataForm.appendChild(statusField);");
							//	append form to body and submit it
							this.writeLine("  getByName('body').appendChild(refDataForm);");
							this.writeLine("  refDataForm.submit();");
							this.writeLine("}");
							this.writeLine("</script>");
							
							this.writeLine("<script type=\"text/javascript\">");
							this.writeLine("var theBibRef = new Object();");
							BibRefEditorFormHandler.writeRefDataAsJavaScriptObject(this.asWriter(), ref, "theBibRef");
							this.writeLine("</script>");
						}
					};
				}
				public boolean handleRequest(HttpServletRequest request, HttpServletResponse response, String pathInfo) throws IOException {
					
					//	handle reference search request
					if (pathInfo.equals("/searchRefs.js")) {
						
						//	read request and perform search
						RefData query = this.readRefData(request);
						Vector refs = searchRefData(query);
						
						//	send JavaScript calls up to this point
						response.setContentType("text/plain");
						response.setCharacterEncoding("UTF-8");
						Writer out = new OutputStreamWriter(response.getOutputStream(), "UTF-8");
						BufferedLineWriter blw = new BufferedLineWriter(out);
						if (refs.size() == 0)
							blw.writeLine("searchRefData_emptyResult();");
						else for (int r = 0; r < refs.size(); r++) {
							RefDataListElement rdle = ((RefDataListElement) refs.get(r));
							blw.writeLine("var srRef = new Object();");
							BibRefEditorFormHandler.writeRefDataAsJavaScriptObject(blw, rdle.ref, "srRef");
							blw.writeLine("srRef['displayString'] = '" + BibRefEditorFormHandler.escapeForJavaScript(rdle.displayString) + "';");
							blw.writeLine("searchRefData_showResult(srRef);");
						}
						blw.flush();
						out.flush();
						blw.close();
						return true;
					}
					
					//	none of our business
					return false;
				}
				public boolean isCloseRequest(HttpServletRequest request, String pathInfo) {
					
					//	form submission (OK or Cancel)
					if (pathInfo.equals("/submitRef")) {
						String status = request.getParameter("status");
						if ("OK".equals(status))
							this.ref = this.readRefData(request);
						else this.ref = null;
						return true;
					}
					
					//	none of our business
					return false;
				}
				private RefData readRefData(HttpServletRequest request) {
					RefData rd = new RefData();
					for (Enumeration pne = request.getParameterNames(); pne.hasMoreElements();) {
						String pn = ((String) pne.nextElement());
						if (!"time".equals(pn) && !"status".equals(pn))
							rd.setAttribute(pn, request.getParameter(pn));
					}
					return rd;
				}
			};
		}
	}
	
	/**
	 * Extract bibliographic metadata from the header area of a document.
	 * @param doc the document whose metadata to extract
	 * @param pm a progress monitor observing document processing
	 */
	public void extractDocumentMetaData(ImDocument doc, ProgressMonitor pm) {
		this.extractDocumentMetaData(doc, false, pm);
	}
	
	static ImDocumentStyle getMetaDataStyle(ImDocument doc) {
		ImDocumentStyle docStyle = ImDocumentStyle.getStyleFor(doc);
		if (docStyle == null)
			docStyle = new ImDocumentStyle(new PropertiesData(new Properties()));
		return docStyle.getImSubset("docMeta");
	}
	
	private void extractDocumentMetaData(ImDocument doc, boolean allowPrompt, ProgressMonitor pm) {
		ImDocumentStyle metaDataStyle = getMetaDataStyle(doc);
		
		//	get bibliographic data from document
		HashMap attributesToValues = new HashMap();
		RefData ref = this.getRefData(doc, metaDataStyle, attributesToValues, allowPrompt);
		
		//	open metadata for editing if allowed to
		if (allowPrompt) {
			ref = this.editRefData(doc, ref, metaDataStyle, attributesToValues);
			
			//	editing cancelled
			if (ref == null)
				return;
		}
		
		//	we're in fully automated mode
		else {
			
			//	try and determine reference type to see if we have a valid reference
			if (!ref.hasAttribute(PUBLICATION_TYPE_ATTRIBUTE)) {
				String type = this.refTypeSystem.classify(ref);
				if (type != null)
					ref.setAttribute(PUBLICATION_TYPE_ATTRIBUTE, type);
			}
			
			//	we don't have a valid reference, little we can do without help from user
			if (!ref.hasAttribute(PUBLICATION_TYPE_ATTRIBUTE))
				return;
		}
		
		//	store reference data
		this.storeRefData(doc, ref, attributesToValues, !allowPrompt);
	}
	
	private RefData getRefData(ImDocument doc, ImDocumentStyle metaDataStyle, HashMap attributesToValues, boolean willPrompt) {
		
		//	put attributes from document in BibRef object
		RefData ref = BibRefUtils.modsAttributesToRefData(doc);
		
		//	use document style template if given
//		boolean useDataSources = metaDataStyle.getBooleanProperty("useDataSources", false);
		String dataSourceMode = metaDataStyle.getStringProperty("dataSourceMode", "noLookup");
		int metaDataMinFromStartPageId = metaDataStyle.getIntProperty("minPageId", 0);
		int metaDataMaxFromStartPageId = metaDataStyle.getIntProperty("maxPageId", 0);
		int metaDataMaxFromEndPageId = metaDataStyle.getIntProperty("maxFromEndPageId", 0);
		ImPage[] pages = doc.getPages();
		if (extractAttribute(AUTHOR_ANNOTATION_TYPE, false, pages, metaDataMinFromStartPageId, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, true, ref, attributesToValues)) {
			String[] authors = ref.getAttributeValues(AUTHOR_ANNOTATION_TYPE);
			for (int a = 0; a < authors.length; a++) {
				String oAuthor = authors[a];
				if (metaDataStyle.getBooleanProperty((AUTHOR_ANNOTATION_TYPE + ".isAllCaps"), false) || metaDataStyle.getBooleanProperty((AUTHOR_ANNOTATION_TYPE + ".isPartAllCaps"), false))
					authors[a] = sanitizeString(authors[a], true, false);
				if (metaDataStyle.getBooleanProperty((AUTHOR_ANNOTATION_TYPE + ".isLastNameLast"), false))
					authors[a] = flipNameParts(authors[a], oAuthor);
				if (a == 0)
					ref.setAttribute(AUTHOR_ANNOTATION_TYPE, authors[a]);
				else ref.addAttribute(AUTHOR_ANNOTATION_TYPE, authors[a]);
			}
		}
		extractAttribute(YEAR_ANNOTATION_TYPE, false, pages, metaDataMinFromStartPageId, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues);
		extractAttribute(PUBLICATION_DATE_ANNOTATION_TYPE, false, pages, metaDataMinFromStartPageId, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues);
		extractAttribute(TITLE_ANNOTATION_TYPE, false, pages, metaDataMinFromStartPageId, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues);
		
		extractAttribute(JOURNAL_NAME_ANNOTATION_TYPE, false, pages, metaDataMinFromStartPageId, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues);
		extractAttribute(SERIES_IN_JOURNAL_ANNOTATION_TYPE, false, pages, metaDataMinFromStartPageId, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues);
		extractAttribute(VOLUME_DESIGNATOR_ANNOTATION_TYPE, false, pages, metaDataMinFromStartPageId, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues);
		extractAttribute(ISSUE_DESIGNATOR_ANNOTATION_TYPE, false, pages, metaDataMinFromStartPageId, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues);
		extractAttribute(NUMERO_DESIGNATOR_ANNOTATION_TYPE, false, pages, metaDataMinFromStartPageId, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues);
		
		extractAttribute(PUBLISHER_ANNOTATION_TYPE, false, pages, metaDataMinFromStartPageId, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues);
		extractAttribute(LOCATION_ANNOTATION_TYPE, false, pages, metaDataMinFromStartPageId, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues);
		
		extractAttribute(VOLUME_TITLE_ANNOTATION_TYPE, false, pages, metaDataMinFromStartPageId, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues);
		if (extractAttribute(EDITOR_ANNOTATION_TYPE, false, pages, metaDataMinFromStartPageId, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, true, ref, attributesToValues)) {
			String[] editors = ref.getAttributeValues(EDITOR_ANNOTATION_TYPE);
			for (int e = 0; e < editors.length; e++) {
				String oEditor = editors[e];
				if (metaDataStyle.getBooleanProperty((EDITOR_ANNOTATION_TYPE + ".isAllCaps"), false) || metaDataStyle.getBooleanProperty((EDITOR_ANNOTATION_TYPE + ".isPartAllCaps"), false))
					editors[e] = sanitizeString(editors[e], true, false);
				if (metaDataStyle.getBooleanProperty((EDITOR_ANNOTATION_TYPE + ".isLastNameLast"), false))
					editors[e] = flipNameParts(editors[e], oEditor);
				if (e == 0)
					ref.setAttribute(EDITOR_ANNOTATION_TYPE, editors[e]);
				else ref.addAttribute(EDITOR_ANNOTATION_TYPE, editors[e]);
			}
		}
		
		if (extractAttribute(PAGINATION_ANNOTATION_TYPE, false, pages, metaDataMinFromStartPageId, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues))
			ref.setAttribute(PAGINATION_ANNOTATION_TYPE, ref.getAttribute(PAGINATION_ANNOTATION_TYPE).trim().replaceAll("[^0-9]+", "-"));
		
		for (int t = 0; t < this.refIdTypes.length; t++) {
			if (this.refIdTypes[t].length() == 0)
				continue; // skip over wildcard ID type
			if (extractAttribute(("ID-" + this.refIdTypes[t]), true, pages, metaDataMinFromStartPageId, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues))
				ref.setIdentifier(this.refIdTypes[t], ref.getIdentifier(this.refIdTypes[t]).replaceAll("\\s", ""));
		}
		
		//	extract author details
		if ((this.authorDetails != null) && (this.authorDetails.length != 0)) {
			ImDocumentStyle authorDetailsStyle = metaDataStyle.getImSubset(AUTHOR_ANNOTATION_TYPE + ".details");
			ArrayList extractedAuthorNames = ((ArrayList) attributesToValues.get(AUTHOR_ANNOTATION_TYPE));
			if ((extractedAuthorNames != null) && (extractedAuthorNames.size() != 0)) {
				ExtractedAttributeValue[] docAuthors = ((ExtractedAttributeValue[]) extractedAuthorNames.toArray(new ExtractedAttributeValue[extractedAuthorNames.size()]));
				for (int d = 0; d < this.authorDetails.length; d++) {
					if (!AuthorData.AUTHOR_NAME_ATTRIBUTE.equals(this.authorDetails[d]))
						extractAuthorDetail(docAuthors, this.authorDetails[d], !AuthorData.AUTHOR_AFFILIATION_ATTRIBUTE.equals(this.authorDetails[d]), authorDetailsStyle, ref, attributesToValues, System.out);
				}
			}
		}
		
		//	try to find title in document head if not already given
		if (!ref.hasAttribute(TITLE_ANNOTATION_TYPE)) {
			String title = findTitle(doc);
			if (title != null)
				ref.setAttribute(TITLE_ANNOTATION_TYPE, title);
		}
		
		/* TODO_maybe add template parameter indicating preferred source of pagination:
		 * - "metadata only" (only extract via template, or take from lookup)
		 * - "prefer metadata" (extract via template, or take from lookup, and fall back to page numbers if former two fail)
		 * - "prefer page numbers" (count out from page numbers if latter given, extract via template, or take from lookup if page numbers absent)
		 * - "page numbers only" (only counted out from page numbers)
		 * 
		 * TODO_maybe add template parameter indicating how to count out start page number:
		 * - "first page" (always use first page in document, extrapolated or not)
		 * - "first explicit page number" (use number from first page that explicitly has a marked page number on it)
		 * 
		 * TODO_maybe add template parameter indicating how to count out end page number:
		 * - "last page" (always use last page in document, extrapolated or not)
		 * - "last explicit page number" (use number from last page that explicitly has a marked page number on it)
		 */
		//	try and add pagination from document if not already given
		if ((pages.length != 0) && pages[0].hasAttribute(PAGE_NUMBER_ATTRIBUTE) && pages[pages.length-1].hasAttribute(PAGE_NUMBER_ATTRIBUTE) && !ref.hasAttribute(PAGINATION_ANNOTATION_TYPE)) try {
			int firstPageNumber = Integer.parseInt((String) pages[0].getAttribute(PAGE_NUMBER_ATTRIBUTE));
			int lastPageNumber = Integer.parseInt((String) pages[pages.length-1].getAttribute(PAGE_NUMBER_ATTRIBUTE));
			if ((lastPageNumber - firstPageNumber + 1) == pages.length)
				ref.setAttribute(PAGINATION_ANNOTATION_TYPE, (firstPageNumber + "-" + lastPageNumber));
		} catch (NumberFormatException nfe) {}
		
		//	transfer PDF URL is given
		if (doc.hasAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE) && (doc.getAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE) instanceof String) && !ref.hasAttribute(PUBLICATION_URL_ANNOTATION_TYPE))
			ref.setAttribute(PUBLICATION_URL_ANNOTATION_TYPE, ((String) doc.getAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE)));
		
		//	check existing reference type, and facilitate re-classification if there are errors
		if (ref.hasAttribute(PUBLICATION_TYPE_ATTRIBUTE)) {
			String[] refErrors = this.refTypeSystem.checkType(ref);
			if (refErrors != null)
				ref.removeAttribute(PUBLICATION_TYPE_ATTRIBUTE);
		}
		
		//	try and classify existing reference
		if (!ref.hasAttribute(PUBLICATION_TYPE_ATTRIBUTE)) {
			String type = this.refTypeSystem.classify(ref);
			if (type != null)
				ref.setAttribute(PUBLICATION_TYPE_ATTRIBUTE, type);
		}
		
//		//	this one is all set
//		if (ref.hasAttribute(PUBLICATION_TYPE_ATTRIBUTE))
//			return ref;
//		
//		//	this one will be taken care of by user, or we're not allowed to use our data sources
//		if (!useDataSources || willPrompt)
//			return ref;
//		
		//	this one will be taken care of by user
		if (willPrompt)
			return ref;
		
		//	no lookups at all
		if ("noLookup".equals(dataSourceMode))
			return ref;
		
		//	this one is all set, and we aren't augmenting
		if ("completeBasic".equals(dataSourceMode) && ref.hasAttribute(PUBLICATION_TYPE_ATTRIBUTE))
			return ref;
		
		//	use web lookup if we have something to work with
		this.logInMasterConfiguration("Completing metadata via data source lookup");
		Vector dsRefs = this.searchRefData(ref);
		if ((dsRefs == null) || dsRefs.isEmpty()) {
			this.logInMasterConfiguration(" ==> no references found");
			return ref;
		}
		
		//	use what we found (best match tends to come first anyway)
		RefDataListElement dsRes = ((RefDataListElement) dsRefs.get(0));
		this.logInMasterConfiguration(" ==> found " + dsRes.toString());
		RefData dsRef = dsRes.ref;
		AttributesTransferPolicy atp;
		if ("completeBasic".equals(dataSourceMode) || "completeAll".equals(dataSourceMode))
			atp = preferTransferTarget;
		else if ("importAll".equals(dataSourceMode))
			atp = preferTransferSource;
		else atp = new DocumentStyleAttributesTransferPolicy(metaDataStyle);
		transferSearchResultAttributes(dsRef, ref, atp);
		
		//	check for errors one last time
		String type = this.refTypeSystem.classify(ref);
		if (type != null)
			ref.setAttribute(PUBLICATION_TYPE_ATTRIBUTE, type);
		
		//	finally ...
		return ref;
	}
	
	private static final String[] lookupTransferAttributeNames = {
		AUTHOR_ANNOTATION_TYPE,
		YEAR_ANNOTATION_TYPE,
		PUBLICATION_DATE_ANNOTATION_TYPE,
		TITLE_ANNOTATION_TYPE,
		
		JOURNAL_NAME_ANNOTATION_TYPE,
		SERIES_IN_JOURNAL_ANNOTATION_TYPE,
		VOLUME_DESIGNATOR_ANNOTATION_TYPE,
		ISSUE_DESIGNATOR_ANNOTATION_TYPE,
		NUMERO_DESIGNATOR_ANNOTATION_TYPE,
		
		PUBLISHER_ANNOTATION_TYPE,
		LOCATION_ANNOTATION_TYPE,
		
		VOLUME_TITLE_ANNOTATION_TYPE,
		EDITOR_ANNOTATION_TYPE,
		
		PAGINATION_ANNOTATION_TYPE,
		
		PUBLICATION_URL_ANNOTATION_TYPE
	};
	
	private static abstract class AttributesTransferPolicy {
		abstract boolean transferAttribute(String name, String fromRefValue, String toRefValue);
	}
	private static final AttributesTransferPolicy preferTransferSource = new AttributesTransferPolicy() {
		boolean transferAttribute(String name, String fromRefValue, String toRefValue) {
			return true;
		}
	};
	private static final AttributesTransferPolicy preferTransferTarget = new AttributesTransferPolicy() {
		boolean transferAttribute(String name, String fromRefValue, String toRefValue) {
			return (toRefValue == null); // nothing to overwrite, use search result
		}
	};
	private class DocumentStyleAttributesTransferPolicy extends AttributesTransferPolicy {
		DocumentStyle docStyle;
		DocumentStyleAttributesTransferPolicy(DocumentStyle docStyle) {
			this.docStyle = docStyle;
		}
		boolean transferAttribute(String name, String fromRefValue, String toRefValue) {
			if (toRefValue == null)
				return true; // nothing to overwrite, use search result
			String mergeMode = this.docStyle.getStringProperty((name + "." + "dataSourceResultMergeMode"), null);
			logInMasterConfiguration(" - merge mode is '" + ((mergeMode == null) ? "(default) document" : mergeMode) + "'");
			return "sources".equals(mergeMode);
		}
	};
	
	void transferSearchResultAttributes(RefData fromRef, RefData toRef, AttributesTransferPolicy atp) {
		
		//	transfer attributes
		for (int a = 0; a < lookupTransferAttributeNames.length; a++) {
			this.logInMasterConfiguration(" - importing " + lookupTransferAttributeNames[a]);
			String fromRefValue = fromRef.getAttribute(lookupTransferAttributeNames[a]);
			if (fromRefValue == null)
				continue;
			String toRefValue = toRef.getAttribute(lookupTransferAttributeNames[a]);
			if (atp.transferAttribute(lookupTransferAttributeNames[a], fromRefValue, toRefValue)) {
				toRef.setAttribute(lookupTransferAttributeNames[a], fromRefValue);
				if (AUTHOR_ANNOTATION_TYPE.equals(lookupTransferAttributeNames[a]) || EDITOR_ANNOTATION_TYPE.equals(lookupTransferAttributeNames[a])) {
					String[] fromRefValues = fromRef.getAttributeValues(lookupTransferAttributeNames[a]);
					for (int v = 1; v < fromRefValues.length; v++)
						toRef.addAttribute(lookupTransferAttributeNames[a], fromRefValues[v]);
					this.logInMasterConfiguration("   =transfer=> " + fromRef.getAttributeValueString(lookupTransferAttributeNames[a], " & "));
				}
				else this.logInMasterConfiguration("   =transfer=> " + fromRefValue);
			}
			else this.logInMasterConfiguration("   =retain=> " + toRefValue);
		}
		
		//	transfer identifiers (TODOne only the ones we're observing, though)
//		String[] fromRefIdTypes = fromRef.getIdentifierTypes();
//		for (int i = 0; i < fromRefIdTypes.length; i++) {
//			this.logInMasterConfiguration(" - importing ID-" + fromRefIdTypes[i]);
//			String fromRefId = fromRef.getIdentifier(fromRefIdTypes[i]);
//			String toRefId = toRef.getIdentifier(fromRefIdTypes[i]);
//			if (atp.transferAttribute(("ID-" + fromRefIdTypes[i]), fromRefId, toRefId)) {
//				toRef.setIdentifier(fromRefIdTypes[i], fromRefId);
//				this.logInMasterConfiguration("   =transfer=> " + fromRefId);
//			}
//			else this.logInMasterConfiguration("   =retain=> " + toRefId);
//		}
		for (int i = 0; i < this.refIdTypes.length; i++) {
			String fromRefId = fromRef.getIdentifier(this.refIdTypes[i]);
			if (fromRefId == null)
				continue;
			this.logInMasterConfiguration(" - importing ID-" + this.refIdTypes[i]);
			String toRefId = toRef.getIdentifier(this.refIdTypes[i]);
			if (atp.transferAttribute(("ID-" + this.refIdTypes[i]), fromRefId, toRefId)) {
				toRef.setIdentifier(this.refIdTypes[i], fromRefId);
				this.logInMasterConfiguration("   =transfer=> " + fromRefId);
			}
			else this.logInMasterConfiguration("   =retain=> " + toRefId);
		}
	}
//	static void transferSearchResultAttributes(RefData fromRef, RefData toRef) {
//		//	TODOne_above observe preferences !!!
//		for (int a = 0; a < lookupTransferAttributeNames.length; a++) {
//			if (toRef.hasAttribute(lookupTransferAttributeNames[a]))
//				continue;
//			if (!fromRef.hasAttribute(lookupTransferAttributeNames[a]))
//				continue;
//			System.out.println(" - importing " + lookupTransferAttributeNames[a]);
//			if (AUTHOR_ANNOTATION_TYPE.equals(lookupTransferAttributeNames[a]) || EDITOR_ANNOTATION_TYPE.equals(lookupTransferAttributeNames[a])) {
//				String[] fromRefAvs = fromRef.getAttributeValues(lookupTransferAttributeNames[a]);
//				for (int v = 0; v < fromRefAvs.length; v++) {
//					if (v == 0)
//						toRef.setAttribute(lookupTransferAttributeNames[a], fromRefAvs[v]);
//					else toRef.addAttribute(lookupTransferAttributeNames[a], fromRefAvs[v]);
//				}
//				System.out.println("   ==> " + fromRef.getAttributeValueString(lookupTransferAttributeNames[a], " & "));
//			}
//			else {
//				toRef.setAttribute(lookupTransferAttributeNames[a], fromRef.getAttribute(lookupTransferAttributeNames[a]));
//				System.out.println("   ==> " + fromRef.getAttribute(lookupTransferAttributeNames[a]));
//			}
//		}
//		
//		//	TODOne_above observe preferences !!!
//		String[] fromRefIdTypes = fromRef.getIdentifierTypes();
//		for (int i = 0; i < fromRefIdTypes.length; i++) {
//			if (toRef.getIdentifier(fromRefIdTypes[i]) == null)
//				toRef.setIdentifier(fromRefIdTypes[i], fromRef.getIdentifier(fromRefIdTypes[i]));
//		}
//	}
	
	void storeRefData(ImDocument doc, RefData ref, HashMap attributesToValues, boolean auto) {
		
		//	store metadata in respective attributes
		this.setDocumentAttributes(doc, ref);
		
		//	if we have a pagination, set page numbers by counting through pages (only if no page numbers yet, though)
		ImPage[] pages = doc.getPages();
		if ((pages.length != 0) && doc.hasAttribute(PAGE_NUMBER_ATTRIBUTE)) try {
			int firstPageNumber = Integer.parseInt((String) doc.getAttribute(PAGE_NUMBER_ATTRIBUTE));
			if (doc.hasAttribute(LAST_PAGE_NUMBER_ATTRIBUTE)) {
				int lastPageNumber = Integer.parseInt((String) doc.getAttribute(LAST_PAGE_NUMBER_ATTRIBUTE));
				if ((lastPageNumber - firstPageNumber + 1) == pages.length) {
					for (int p = 0; p < pages.length; p++) {
						if (!pages[p].hasAttribute(PAGE_NUMBER_ATTRIBUTE))
							pages[p].setAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + (firstPageNumber + p)));
					}
				}
				else if (!pages[0].hasAttribute(PAGE_NUMBER_ATTRIBUTE))
					pages[0].setAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + firstPageNumber));
			}
			else if (!pages[0].hasAttribute(PAGE_NUMBER_ATTRIBUTE))
				pages[0].setAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + firstPageNumber));
		} catch (NumberFormatException nfe) {}
		
		//	annotate attribute values in document head
		this.annotateAttributeValues(ref, ref.getAttributeNames(), extractableFieldNames, false, doc, attributesToValues, auto);
		this.annotateAttributeValues(ref, ref.getIdentifierTypes(), this.refIdTypes, true, doc, attributesToValues, auto);
		
		//	annotate author details
		String[] refAuthorNames = ref.getAttributeValues(AUTHOR_ANNOTATION_TYPE);
		this.logInMasterConfiguration(Arrays.toString(refAuthorNames));
		this.logInMasterConfiguration(Arrays.toString(this.authorDetails));
		this.annotateAuthorDetailValues(ref, refAuthorNames, ref.getAuthorAttributeNames(), this.authorDetails, doc, attributesToValues, auto);
		
		//	annotate document reference
		this.annotateDocReference(doc, ref);
	}
	
	private void annotateDocReference(ImDocument doc, RefData ref) {
		
		//	check for existing annotations
		ImAnnotation[] docRefAnnots = doc.getAnnotations("docRef");
		if (docRefAnnots.length != 0)
			return;
		
		//	use document style template if given
		ImDocumentStyle docStyle = ImDocumentStyle.getStyleFor(doc);
		if (docStyle == null)
			return;
		ImDocumentStyle metaDataStyle = docStyle.getImSubset("docMeta");
		if (!metaDataStyle.getBooleanProperty("docRefPresent", false))
			return;
		
		//	create counting set of reference data tokens, including identifiers
		CountingSet refDataTokens = new CountingSet(new TreeMap(String.CASE_INSENSITIVE_ORDER));
		String[] refAttributeNames = ref.getAttributeNames();
		for (int a = 0; a < refAttributeNames.length; a++) {
			if ("ID".equals(refAttributeNames[a]))
				continue; // we handle IDs below
			if (RefData.PUBLICATION_TYPE_ATTRIBUTE.equals(refAttributeNames[a]))
				continue; // no use looking at type
			if (PUBLICATION_DATE_ANNOTATION_TYPE.equals(refAttributeNames[a]))
				continue; // not usually part of reference string
			String[] refAttributeValues = ref.getAttributeValues(refAttributeNames[a]);
			for (int v = 0; v < refAttributeValues.length; v++) {
				TokenSequence valueTokens = Gamta.newTokenSequence(refAttributeValues[v], Gamta.INNER_PUNCTUATION_TOKENIZER);
				for (int t = 0; t < valueTokens.size(); t++) {
					String valueToken = StringUtils.normalizeString(valueTokens.valueAt(t)).trim();
					if (!Gamta.isPunctuation(valueToken))
						refDataTokens.add(valueToken);
				}
			}
		}
		CountingSet refIdTokens = new CountingSet(new TreeMap(String.CASE_INSENSITIVE_ORDER));
		String[] refIdTypes = ref.getIdentifierTypes();
		for (int i = 0; i < refIdTypes.length; i++) {
			String refId = ref.getIdentifier(refIdTypes[i]);
			TokenSequence idTokens = Gamta.newTokenSequence(refId, Gamta.INNER_PUNCTUATION_TOKENIZER);
			for (int t = 0; t < idTokens.size(); t++) {
				String idToken = StringUtils.normalizeString(idTokens.valueAt(t)).trim();
				if (!Gamta.isPunctuation(idToken))
					refIdTokens.add(idToken);
			}
		}
		
		//	get extraction parameters
		int metaDataMinFromStartPageId = metaDataStyle.getIntProperty("minPageId", 0);
		int metaDataMaxFromStartPageId = metaDataStyle.getIntProperty("maxPageId", 0);
		int metaDataMaxFromEndPageId = metaDataStyle.getIntProperty("maxFromEndPageId", 0);
		ImDocumentStyle docRefStyle = metaDataStyle.getImSubset("docRef");
		int fontSize = docRefStyle.getIntProperty("fontSize", -1);
		int minFontSize = docRefStyle.getIntProperty("minFontSize", ((fontSize == -1) ? 0 : fontSize));
		int maxFontSize = docRefStyle.getIntProperty("maxFontSize", ((fontSize == -1) ? 72 : fontSize));
		
		//	collect candidates through pages
		ImPage[] pages = doc.getPages();
		ArrayList docRefCandidates = new ArrayList();
		
		//	try extraction from document start first
		int minFromStartPageId = docRefStyle.getIntProperty("minPageId", metaDataMinFromStartPageId);
		int maxFromStartPageId = docRefStyle.getIntProperty("maxPageId", metaDataMaxFromStartPageId);
		for (int p = minFromStartPageId; (p <= maxFromStartPageId) && (p < pages.length); p++)
			this.addDocReferenceCandidates(doc, pages[p], minFontSize, maxFontSize, refDataTokens, refIdTokens, docRefCandidates);
		
		//	try extraction from document end only second
		int maxFromEndPageId = docRefStyle.getIntProperty("maxFromEndPageId", metaDataMaxFromEndPageId);
		for (int p = (pages.length - maxFromEndPageId); p < pages.length; p++)
			this.addDocReferenceCandidates(doc, pages[p], minFontSize, maxFontSize, refDataTokens, refIdTokens, docRefCandidates);
		
		//	anything to work with?
		if (docRefCandidates.isEmpty())
			return;
		
		//	select best candidate
		Collections.sort(docRefCandidates);
		DocReferenceCandidate docRef = ((DocReferenceCandidate) docRefCandidates.get(0));
		Arrays.sort(docRef.words, ImUtils.textStreamOrder);
		
		//	get longest text stream (in case we have more than one)
		ImWord docRefStart = null;
		ImWord docRefEnd = null;
		int docRefLength = 0;
		ImWord tsStart = null;
		ImWord tsEnd = null;
		int tsLength = 0;
		for (int w = 0; w <= docRef.words.length; w++) {
			if (w == docRef.words.length) { // finalize (last) text stream
				if (tsLength > docRefLength) {
					docRefStart = tsStart;
					docRefEnd = tsEnd;
					docRefLength = tsLength;
				}
			}
			else if (tsStart == null) { // start of first text stream
				tsStart = docRef.words[w];
				tsEnd = docRef.words[w];
				tsLength = 1;
			}
			else if (tsStart.getTextStreamId().endsWith(docRef.words[w].getTextStreamId())) { // text stream continues
				tsEnd = docRef.words[w];
				tsLength++;
			}
			else { // text stream broken
				if (tsLength > docRefLength) { // finalize just-finished text stream
					docRefStart = tsStart;
					docRefEnd = tsEnd;
					docRefLength = tsLength;
				}
				tsStart = docRef.words[w];
				tsEnd = docRef.words[w];
				tsLength = 1;
			}
		}
		
		//	add annotation
		if ((docRefStart != null) && (docRefEnd != null)) {
			ImAnnotation docRefAnnot = doc.addAnnotation(docRefStart, docRefEnd, "docRef");
			docRefAnnot.setAttribute("accuracy", ("" + docRef.accuracy));
		}
	}
	
	private void addDocReferenceCandidates(ImDocument doc, ImPage page, int minFontSize, int maxFontSize, CountingSet refDataTokens, CountingSet refIdTokens, ArrayList docRefCandidates) {
		
		//	work through paragraphs
		ImRegion[] paragraphs = page.getRegions(ImRegion.PARAGRAPH_TYPE);
		for (int p = 0; p < paragraphs.length; p++) {
			ImWord[] words = paragraphs[p].getWords();
			this.logInMasterConfiguration("Checking paragraph " + page.pageId + "." + paragraphs[p].bounds + " for document reference");
			
			//	check length first
			if ((refDataTokens.size() * 3) < words.length) {
				this.logInMasterConfiguration(" ==> too large (" + words.length + " for " + refDataTokens.size() + " reference tokens)");
				continue; // this one is just too large, even accounting for punctuation and identifiers
			}
			if ((words.length * 2) < refDataTokens.size()) {
				this.logInMasterConfiguration(" ==> too small (" + words.length + " for " + refDataTokens.size() + " reference tokens)");
				continue; // this one is too small to accommodate the reference, even accounting for shortened author names, etc.
			}
			
			//	check font size first if present (allow at most 10% off, weighted by word size)
			if ((minFontSize > 0) && (maxFontSize < 72)) {
				int fsWordWidth = 0;
				int nonFsWordWidth = 0;
				for (int w = 0; w < words.length; w++) {
					int wordFs = words[w].getFontSize();
					if ((minFontSize <= wordFs) && (wordFs <= maxFontSize))
						fsWordWidth += words[w].bounds.getWidth();
					else if (!Gamta.isPunctuation(words[w].getString()))
						nonFsWordWidth += words[w].bounds.getWidth();
				}
				if ((nonFsWordWidth * 10) > fsWordWidth) {
					this.logInMasterConfiguration(" ==> too many words outside font size range (" + nonFsWordWidth + " against " + fsWordWidth + " in range [" + minFontSize + "," + maxFontSize + "])");
					continue;
				}
			}
			
			//	match words against metadata tokens
			int tokenCount = 0;
			int refTokenCount = 0;
			int idTokenCount = 0;
			CountingSet spuriousTokens = new CountingSet(new TreeMap(String.CASE_INSENSITIVE_ORDER));
			CountingSet unmatchedRefDataTokens = new CountingSet(new TreeMap(String.CASE_INSENSITIVE_ORDER));
			unmatchedRefDataTokens.addAll(refDataTokens);
			ImDocumentRoot tokens = new ImDocumentRoot(paragraphs[p], (ImDocumentRoot.NORMALIZATION_LEVEL_WORDS | ImDocumentRoot.NORMALIZE_CHARACTERS));
			this.logInMasterConfiguration(" - checking " + tokens.size() + " tokens:");
			for (int t = 0; t < tokens.size(); t++) {
				String token = StringUtils.normalizeString(tokens.valueAt(t)).trim();
				if (Gamta.isPunctuation(token))
					continue;
				tokenCount++;
				if (unmatchedRefDataTokens.contains(token)) {
					unmatchedRefDataTokens.remove(token);
					refTokenCount++;
					this.logInMasterConfiguration("   - got matching token '" + token + "'");
				}
				else if (refIdTokens.contains(token)) {
					idTokenCount++;
					this.logInMasterConfiguration("   - got identifier token '" + token + "'");
				}
				else {
					spuriousTokens.add(token);
					this.logInMasterConfiguration("   - got spurious token '" + token + "'");
				}
			}
			this.logInMasterConfiguration(" --> got " + tokenCount + " tokens");
			this.logInMasterConfiguration(" --> got " + refTokenCount + " matching tokens");
			this.logInMasterConfiguration(" --> got " + idTokenCount + " identifier tokens");
			this.logInMasterConfiguration(" --> got " + spuriousTokens.size() + " spurious tokens");
			this.logInMasterConfiguration(" --> got " + unmatchedRefDataTokens.size() + " unmatched reference tokens:");
			int abbrevMatchTokenCount = 0;
			for (Iterator umtit = unmatchedRefDataTokens.iterator(); umtit.hasNext();) {
				String unmatchedToken = ((String) umtit.next());
				this.logInMasterConfiguration("   - '" + unmatchedToken + "' (" + unmatchedRefDataTokens.getCount(unmatchedToken) + ")");
				for (Iterator stit = spuriousTokens.iterator(); stit.hasNext();) {
					String spuriousToken = ((String) stit.next());
					if (StringUtils.isAbbreviationOf(unmatchedToken, spuriousToken, true)) {
						abbrevMatchTokenCount += Math.min(unmatchedRefDataTokens.getCount(unmatchedToken), spuriousTokens.getCount(spuriousToken));
						this.logInMasterConfiguration("     --> abbreviation matched to '" + spuriousToken + "' (" + spuriousTokens.getCount(spuriousToken) + ")");
						break;
					}
				}
			}
			this.logInMasterConfiguration(" --> got " + abbrevMatchTokenCount + " abbeviation matching tokens");
			float precision = (((float) (refTokenCount + idTokenCount + ((abbrevMatchTokenCount + 1) / 2))) / tokenCount);
			float recall = (((float) ((refDataTokens.size() - unmatchedRefDataTokens.size()) + ((abbrevMatchTokenCount + 1) / 2))) / refDataTokens.size());
			float accuracy = (precision * recall);
			this.logInMasterConfiguration(" ==> match accuracy is " + accuracy + " (P" + precision + "/R" + recall + ")");
			
			//	store candidate if match good enough
			if (accuracy >= 0.75) {
				docRefCandidates.add(new DocReferenceCandidate(paragraphs[p], words, accuracy));
				this.logInMasterConfiguration(" ==> good candidate");
			}
		}
	}
	
	private static class DocReferenceCandidate implements Comparable {
		final ImRegion paragraph;
		final ImWord[] words;
		final float accuracy;
		DocReferenceCandidate(ImRegion paragraph, ImWord[] words, float accuracy) {
			this.paragraph = paragraph;
			this.words = words;
			this.accuracy = accuracy;
		}
		public int compareTo(Object obj) {
			return Float.compare(((DocReferenceCandidate) obj).accuracy, this.accuracy); // sort descending
		}
	}
	
	private static String findTitle(ImDocument doc) {
		ImAnnotation[] headings = doc.getAnnotations(HEADING_TYPE);
		String title = null;
		int titleArea = 0;
		for (int h = 0; h < headings.length; h++) {
			if (headings[h].getFirstWord().pageId != 0)
				continue;
			if (!"0".equals(headings[h].getAttribute("level")))
				continue;
			int headingArea = 0;
			for (ImWord imw = headings[h].getFirstWord(); imw != null; imw = imw.getNextWord()) {
				headingArea += ((imw.bounds.right - imw.bounds.left) * (imw.bounds.bottom - imw.bounds.top));
				if (imw == headings[h].getLastWord())
					break;
			}
			if (headingArea > titleArea) {
				title = ImUtils.getString(headings[h].getFirstWord(), headings[h].getLastWord(), true);
				titleArea = headingArea;
			}
		}
		return title;
	}
	
	private static boolean extractAttribute(String name, boolean isIdentifier, ImPage[] pages, int minFromStartPageId, int maxFromStartPageId, int maxFromEndPageId, ImDocumentStyle docStyle, boolean isMultiValue, RefData ref, HashMap attributesToValues) {
		
		//	get attribute specific parameters
		ImDocumentStyle attributeStyle = docStyle.getImSubset(name);
		
		//	collect match words
		ArrayList attributeValues = new ArrayList(isMultiValue ? 5 : 1);
		
		//	try extraction from document start first
		minFromStartPageId = attributeStyle.getIntProperty("minPageId", minFromStartPageId);
		maxFromStartPageId = attributeStyle.getIntProperty("maxPageId", maxFromStartPageId);
		for (int p = minFromStartPageId; (p <= maxFromStartPageId) && (p < pages.length); p++) {
			int dpi = pages[p].getImageDPI();
			if (extractAttribute(name, isIdentifier, pages[p], dpi, attributeStyle, isMultiValue, ref, attributeValues, System.out)) {
				attributesToValues.put(name, attributeValues);
				return true;
			}
		}
		
		//	try extraction from document end only second
		maxFromEndPageId = attributeStyle.getIntProperty("maxFromEndPageId", maxFromEndPageId);
		for (int p = Math.max(0, (pages.length - maxFromEndPageId)); p < pages.length; p++) {
			int dpi = pages[p].getImageDPI();
			if (extractAttribute(name, isIdentifier, pages[p], dpi, attributeStyle, isMultiValue, ref, attributeValues, System.out)) {
				attributesToValues.put(name, attributeValues);
				return true;
			}
		}
		
		//	remember any existing values
		if (attributeValues.size() != 0)
			attributesToValues.put(name, attributeValues);
		
		//	nothing helped ...
		return false;
	}
	
	private static boolean extractAttribute(String name, boolean isIdentifier, ImPage page, int dpi, ImDocumentStyle attributeStyle, boolean isMultiValue, RefData ref, ArrayList attributeValues, PrintStream log) {
		
		//	attribute already set, get annotations and we're done
		if ((ref != null) && (ref.getAttribute(name) != null)) {
			String annotType;
			if (isIdentifier)
				annotType = ("docId" + name.substring("ID-".length()));
			else annotType = ("doc" + Character.toUpperCase(name.charAt(0)) + name.substring(1));
			ImAnnotation[] annots = page.getDocument().getAnnotations(annotType, page.pageId);
			for (int a = 0; a < annots.length; a++) {
				String value = ImUtils.getString(annots[a].getFirstWord(), annots[a].getLastWord(), true);
				if (isIdentifier)
					value = value.replaceAll("\\s", "");
				log.println("Got existing " + name + " from " + annotType + ": " + value);
				String sValue = sanitizeAttributeValue(name, value, isIdentifier);
				if (sValue == null)
					sValue = ref.getAttribute(name);
				attributeValues.add(new ExtractedAttributeValue(name, value, StringUtils.normalizeString(value), sValue, getSpannedWords(annots[a].getFirstWord(), annots[a].getLastWord())));
			}
			if (attributeValues.isEmpty())
				log.println("Existing " + name + " not found from " + annotType);
			return false;
		}
		log.println("Extracting " + name + ":");
		
		//	check for fixed value
		String fixedValue = (fixableFieldNames.contains(name) ? attributeStyle.getStringProperty("fixedValue", null) : null);
		if (fixedValue != null) {
			log.println(" ==> fixed to " + fixedValue);
			if (ref != null)
				ref.setAttribute(name, fixedValue);
			return false;
		}
		
		//	get extraction area
		BoundingBox area = attributeStyle.getBoxProperty("area", null, dpi);
		if (area == null) {
			log.println(" ==> deactivated");
			return false;
		}
		
		//	get words from area
		ImWord[] words = page.getWordsInside(area);
		log.println(" - got " + words.length + " words in area");
		if (words.length == 0)
			return false;
		
		//	extract based on identified words
		return extractAttributeValue(name, isIdentifier, words, attributeStyle, isMultiValue, ref, attributeValues, log);
	}
	
	private static boolean extractAttributeValue(String name, boolean isIdentifier, ImWord[] words, ImDocumentStyle attributeStyle, boolean isMultiValue, RefData ref, ArrayList extractedValues, PrintStream log) {
		
		/* TODO Maybe compile styles parameter sets into MetaDataAttributeExtractor objects:
- akin to defined heading styles in document structure detection
  ==> ALSO, add extraction logging PrintStream argument to matching method of latter ...
  ==> ... and provide log output dialog as in document metadata parameter group descriptions
- saves reading and parsing parameters multiple times
- can be cached by document style and attribute name
- bundles parameters in single method arguments
- can provide instance methods for value extraction ...
- ... removing need to pass extraction parameters as method arguments
  ==> allow specifying custom values via arguments, though
    ==> facilitates passing computed author detail bounding box for proximity association
      ==> maybe intersect that with instance stored bounding box
- should not take up all too much memory
		 */
		
		//	get extraction parameters
		boolean isBold = attributeStyle.getBooleanProperty("isBold", false);
		boolean isItalics = attributeStyle.getBooleanProperty("isItalics", false);
		boolean isAllCaps = attributeStyle.getBooleanProperty("isAllCaps", false);
		int fontSize = attributeStyle.getIntProperty("fontSize", -1);
		int minFontSize = attributeStyle.getIntProperty("minFontSize", ((fontSize == -1) ? 0 : fontSize));
		int maxFontSize = attributeStyle.getIntProperty("maxFontSize", ((fontSize == -1) ? 72 : fontSize));
		boolean singleLineValues = (isMultiValue && attributeStyle.getBooleanProperty("singleLineValues", false));
		String[] filterPatternStrings = attributeStyle.getStringListProperty("filterPatterns", null, " ");
		Pattern[] filterPatterns = ((filterPatternStrings == null) ? null : new Pattern[filterPatternStrings.length]);
		String contextPattern = attributeStyle.getStringProperty("contextPattern", null);
		String valuePattern = attributeStyle.getStringProperty("valuePattern", null);
		
		//	filter words based on font size and style
		ArrayList wordList = new ArrayList();
		for (int w = 0; w < words.length; w++) {
			if (isBold && !words[w].hasAttribute(ImWord.BOLD_ATTRIBUTE))
				continue;
			if (isItalics && !words[w].hasAttribute(ImWord.ITALICS_ATTRIBUTE))
				continue;
			if (isAllCaps && !words[w].getString().equals(words[w].getString().toUpperCase()))
				continue;
			if ((0 < minFontSize) || (maxFontSize < 72)) try {
				int wfs = words[w].getFontSize();
				if ((wfs < minFontSize) || (maxFontSize < wfs))
					continue;
			}
			catch (Exception e) {
				continue;
			}
			wordList.add(words[w]);
		}
		if (wordList.size() < words.length)
			words = ((ImWord[]) wordList.toArray(new ImWord[wordList.size()]));
		log.println(" - got " + words.length + " matching style and font size");
		if (words.length == 0)
			return false;
		
		//	order and concatenate words
		//	TODO_not use sortIntoLines() instead if we have many individual words (graphics label false positive, etc.)
		//	==> better use page header areas to get such stuff into one stream, don't belong to main text anyway
		Arrays.sort(words, ImUtils.textStreamOrder);
		StringBuffer wordStringBuilder = new StringBuffer();
		ArrayList wordAtChar = new ArrayList();
		for (int w = 0; w < words.length; w++) {
			wordStringBuilder.append(words[w].getString());
			for (int c = 0; c < words[w].getString().length(); c++)
				wordAtChar.add(words[w]);
			if ((w+1) == words.length)
				break;
			if (words[w].getNextWord() != words[w+1]) {
				wordStringBuilder.append(singleLineValues ? "\n" : " ");
				wordAtChar.add(words[w]);
			}
			else if (words[w].getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END) {
				wordStringBuilder.append(singleLineValues ? "\n" : " ");
				wordAtChar.add(words[w]);
			}
			else if ((words[w].getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED) && (wordStringBuilder.length() != 0)) {
				wordStringBuilder.deleteCharAt(wordStringBuilder.length()-1);
				wordAtChar.remove(wordAtChar.size()-1);
			}
			else if (words[w].getNextRelation() == ImWord.NEXT_RELATION_SEPARATE) {
				if (words[w].bounds.bottom <= words[w+1].centerY) {
					wordStringBuilder.append(singleLineValues ? "\n" : " ");
					wordAtChar.add(words[w]);
				}
				else if (words[w].bounds.top > words[w+1].centerY) {
					wordStringBuilder.append(singleLineValues ? "\n" : " ");
					wordAtChar.add(words[w]);
				}
//				else if ((words[w].bounds.right < words[w+1].bounds.left) && Gamta.insertSpace(words[w].getString(), words[w+1].getString())) {
				else if (isSpace(words[w], words[w+1], false) && Gamta.insertSpace(words[w].getString(), words[w+1].getString())) {
					wordStringBuilder.append(" ");
					wordAtChar.add(words[w]);
				}
//				else if (StringUtils.DASHES.contains(words[w].getString()) != StringUtils.DASHES.contains(words[w+1].getString())) {
				else if (isSpace(words[w], words[w+1], true) && (StringUtils.DASHES.contains(words[w].getString()) != StringUtils.DASHES.contains(words[w+1].getString()))) {
					wordStringBuilder.append(" ");
					wordAtChar.add(words[w]);
				}
			}
		}
		String wordString = wordStringBuilder.toString();
		log.println(" - word string is " + wordString);
		
		//	normalize string for pattern matching
		String nWordString;
		if (singleLineValues && (wordString.indexOf('\n') != -1)) {
			StringVector wordLines = new StringVector();
			wordLines.parseAndAddElements(wordString, "\n");
			for (int l = 0; l < wordLines.size(); l++)
				wordLines.setElementAt(StringUtils.normalizeString(wordLines.get(l)), l);
			nWordString = wordLines.concatStrings("\n");
		}
		else nWordString = StringUtils.normalizeString(wordString);
		log.println(" - normalized word string is " + nWordString);
		
		//	nothing further to extract, we're done
		if ((contextPattern == null) && (valuePattern == null)) {
			if (singleLineValues && (wordString.indexOf('\n') != -1)) {
				if (isMultiValue) {
					String[] wordStringParts = wordString.split("\\n");
					int wordStringPartStart = 0;
					for (int p = 0; p < wordStringParts.length; p++) {
						log.println(" ==> single-line added " + wordStringParts[p]);
						String singleValue = wordStringParts[p];
						String nSingleValue = StringUtils.normalizeString(wordStringParts[p]);
						String sSingleValue = sanitizeAttributeValue(name, singleValue, isIdentifier);
						if (useAttributeValue(nSingleValue, filterPatternStrings, filterPatterns, log)) {
							if (ref != null)
								ref.addAttribute(name, ((sSingleValue == null) ? singleValue : sSingleValue));
							extractedValues.add(new ExtractedAttributeValue(name, singleValue, nSingleValue, sSingleValue, getAttributeWords(wordAtChar, wordStringPartStart, (wordStringPartStart + wordStringParts[p].length()))));
						}
						wordStringPartStart += wordStringParts[p].length();
						wordStringPartStart += "\n".length();
					}
				}
				else {
					wordString = wordString.substring(0, wordString.indexOf('\n')).trim();
					log.println(" - single-line cut to " + wordString);
					nWordString = StringUtils.normalizeString(wordString);
					log.println(" - normalized word now string is " + nWordString);
					log.println(" ==> set to " + wordString);
					String sWordString = sanitizeAttributeValue(name, wordString, isIdentifier);
					if (useAttributeValue(nWordString, filterPatternStrings, filterPatterns, log)) {
						if (ref != null)
							ref.setAttribute(name, ((sWordString == null) ? wordString : sWordString));
						extractedValues.add(new ExtractedAttributeValue(name, wordString, nWordString, sWordString, getAttributeWords(wordAtChar, 0, wordAtChar.size())));
					}
				}
			}
			else {
				log.println(" ==> set to " + wordString);
				String sWordString = sanitizeAttributeValue(name, wordString, isIdentifier);
				if (useAttributeValue(nWordString, filterPatternStrings, filterPatterns, log)) {
					if (ref != null)
						ref.setAttribute(name, ((sWordString == null) ? wordString : sWordString));
					extractedValues.add(new ExtractedAttributeValue(name, wordString, nWordString, sWordString, getAttributeWords(wordAtChar, 0, wordAtChar.size())));
				}
			}
			return (extractedValues.size() != 0);
		}
		
		//	adjust patterns for single-line values
		if (singleLineValues) {
			contextPattern = adjustWHitespaceMatchers(contextPattern, "context", log);
			valuePattern = adjustWHitespaceMatchers(valuePattern, "value", log);
		}
		
		//	extract multi value attributes
		if (isMultiValue) {
			
			//	use context pattern
			if (contextPattern != null) try {
				Matcher contextMatcher = Pattern.compile(contextPattern).matcher(nWordString);
				int contextMatches = 0;
				while (contextMatcher.find()) {
					contextMatches++;
					String cWordString = wordString.substring(contextMatcher.start(), contextMatcher.end());
					String cnWordString = contextMatcher.group();
					ArrayList cWordAtChar = new ArrayList(wordAtChar.subList(contextMatcher.start(), contextMatcher.end()));
					log.println(" - context pattern matched to " + cnWordString);
					addSingleAttributeValue(name, isIdentifier, isMultiValue, ref, extractedValues, cWordString, cnWordString, cWordAtChar, valuePattern, filterPatternStrings, filterPatterns, log);
				}
				if (extractedValues.isEmpty()) {
					if (contextMatches == 0) // value patern problems are reported individually
						log.println(" ==> context pattern mismatch");
					return false;
				}
				else return true;
			}
			catch (Exception e) {
				log.println(" - context pattern error: " + e.getMessage());
//				e.printStackTrace(System.out);
				return false;
			}
			
			//	use value pattern alone
			try {
				//	TODO_not maybe automatically add boundary matchers for single-line values ???
				//	==> but then, the '.' wildcard doesn't match line breaks by default, so their presence should do
				//	TODOne replace '\s' matchers (which _do_ match line breaks) with '\x20' matchers for single-line values 
				Matcher valueMatcher = Pattern.compile(valuePattern).matcher(nWordString);
				while (valueMatcher.find())
					addSinglePatternMatch(name, isIdentifier, isMultiValue, ref, extractedValues, wordString, nWordString, wordAtChar, valueMatcher, filterPatternStrings, filterPatterns, log);
				if ((ref != null) && ref.hasAttribute(name))
					return true;
				else if (extractedValues.size() != 0)
					return true;
				else {
					log.println(" ==> value pattern mismatch");
					return false;
				}
			}
			catch (Exception e) {
				log.println(" - value pattern error: " + e.getMessage());
//				e.printStackTrace(System.out);
				return false;
			}
		}
			
		//	use context narrowing pattern
		if (contextPattern != null) try {
			Matcher contextMatcher = Pattern.compile(contextPattern).matcher(nWordString);
			if (contextMatcher.find()) {
				wordString = wordString.substring(contextMatcher.start(), contextMatcher.end());
				nWordString = contextMatcher.group();
				wordAtChar = new ArrayList(wordAtChar.subList(contextMatcher.start(), contextMatcher.end()));
				log.println(" - context pattern cut to " + wordString);
			}
			else {
				log.println(" ==> context pattern mismatch");
				return false;
			}
		}
		catch (Exception e) {
			log.println(" - context pattern error: " + e.getMessage());
//			e.printStackTrace(System.out);
			return false;
		}
		
		//	extract single value from whatever we have
		return addSingleAttributeValue(name, isIdentifier, isMultiValue, ref, extractedValues, wordString, nWordString, wordAtChar, valuePattern, filterPatternStrings, filterPatterns, log);
	}
	
	private static boolean isSpace(ImWord word1, ImWord word2, boolean forDash) {
		int wordDist = (word2.bounds.left - word1.bounds.right);
		if (wordDist <= 0)
			return false;
		if (forDash && (wordDist > 1))
			return true;
		int wordHeight = ((word1.bounds.getHeight() + word2.bounds.getHeight()) / 2);
		return ((wordHeight * 1) <= (wordDist * 10)); // 10% should be OK as an estimate lower bound, and with some safety margin, at least for born-digital text (0.25 is smallest defaulting space width)
	}
	
	private static boolean useAttributeValue(String value, String[] filterPatternStrings, Pattern[] filterPatterns, PrintStream log) {
		if (filterPatternStrings == null)
			return true;
		for (int p = 0; p < filterPatterns.length; p++) {
			if (filterPatterns[p] == null) try {
				filterPatterns[p] = Pattern.compile(filterPatternStrings[p]);
			}
			catch (Exception e) {
				log.println(" - ignoring erroneous filter pattern: " + e.getMessage());
//				e.printStackTrace(System.out);
			}
			if (filterPatterns[p] != null) {
				Matcher m = filterPatterns[p].matcher(value);
				if (m.find() && (m.start() == 0)) {
					log.println(" ==> filtered by pattern " + filterPatternStrings[p]);
					return false;
				}
			}
		}
		return true;
	}
	
	private static String adjustWHitespaceMatchers(String pattern, String type, PrintStream log) {
		if (pattern == null)
			return pattern;
		boolean escaped = false;
		StringBuffer slPattern = new StringBuffer();
		for (int c = 0; c < pattern.length(); c++) {
			char ch = pattern.charAt(c);
			if (escaped) {
				escaped = false;
				if (ch == 's')
					slPattern.append("x20");
				else slPattern.append(ch);
			}
			else if (ch == '\\') {
				escaped = true;
				slPattern.append(ch);
			}
			else slPattern.append(ch);
		}
		pattern = slPattern.toString();
		log.println(" - " + type + " pattern adjusted to " + pattern);
		return pattern;
	}
	
	private static boolean addSingleAttributeValue(String name, boolean isIdentifier, boolean isMultiValue, RefData ref, ArrayList attributeValues, String wordString, String nWordString, ArrayList wordAtChar, String valuePattern, String[] filterPatternStrings, Pattern[] filterPatterns, PrintStream log) {
		
		//	no value pattern, use plain string
		if (valuePattern == null) {
			log.println(" ==> " + (isMultiValue? "added" : "set to") +  " " + wordString);
			String sWordString = sanitizeAttributeValue(name, wordString, isIdentifier);
			if (useAttributeValue(nWordString, filterPatternStrings, filterPatterns, log)) {
				if (ref != null)
					ref.setAttribute(name, ((sWordString == null) ? wordString : sWordString));
				attributeValues.add(new ExtractedAttributeValue(name, wordString, nWordString, sWordString, getAttributeWords(wordAtChar, 0, wordAtChar.size())));
				return true;
			}
			else return false;
		}
		
		//	use value pattern
		else try {
			Matcher valueMatcher = Pattern.compile(valuePattern).matcher(nWordString);
			if (valueMatcher.find())
				return addSinglePatternMatch(name, isIdentifier, isMultiValue, ref, attributeValues, wordString, nWordString, wordAtChar, valueMatcher, filterPatternStrings, filterPatterns, log);
			else {
				log.println(" ==> value pattern mismatch");
				return false;
			}
		}
		catch (Exception e) {
			log.println(" - value pattern error: " + e.getMessage());
//			e.printStackTrace(System.out);
			return false;
		}
	}
	
	private static boolean addSinglePatternMatch(String name, boolean isIdentifier, boolean isMultiValue, RefData ref, ArrayList attributeValues, String wordString, String nWordString, ArrayList wordAtChar, Matcher valueMatcher, String[] filterPatternStrings, Pattern[] filterPatterns, PrintStream log) {
		log.println(" ==> value pattern matched " + valueMatcher.group());
		log.println("        ==> actually " + (isMultiValue? "added" : "set to") +  " " + wordString.substring(valueMatcher.start(), valueMatcher.end()));
		String singleValue = wordString.substring(valueMatcher.start(), valueMatcher.end());
		String nSingleValue = valueMatcher.group();
		String sSingleValue = sanitizeAttributeValue(name, singleValue, isIdentifier);
		if (useAttributeValue(nWordString, filterPatternStrings, filterPatterns, log)) {
			if (ref != null)
				ref.addAttribute(name, ((sSingleValue == null) ? singleValue : sSingleValue));
			attributeValues.add(new ExtractedAttributeValue(name, singleValue, nSingleValue, sSingleValue, getAttributeWords(wordAtChar, valueMatcher.start(), valueMatcher.end())));
			return true;
		}
		return false;
	}
	
	private static ImWord[] getAttributeWords(ArrayList wordAtChar, int from, int to) {
		ArrayList attributeWords = new ArrayList();
		for (int w = from; w < to; w++) {
			if ((w == from) || (wordAtChar.get(w) != wordAtChar.get(w-1)))
				attributeWords.add(wordAtChar.get(w));
		}
		return ((ImWord[]) attributeWords.toArray(new ImWord[attributeWords.size()]));
	}
	
	private static ImWord[] getSpannedWords(ImWord fromWord, ImWord toWord) {
		ArrayList spannedWords = new ArrayList();
		for (ImWord imw = fromWord; imw != null; imw = imw.getNextWord()) {
			spannedWords.add(imw);
			if (imw == toWord)
				break;
		}
		return ((ImWord[]) spannedWords.toArray(new ImWord[spannedWords.size()]));
	}
	
	private static class ExtractedAttributeValue extends AbstractAttributed implements Comparable, ImAnnotation {
		final String name;
		final String rValue;
		final String nValue;
		final String sValue;
		final ImWord[] words;
		final ImWord firstWord;
		final ImWord lastWord;
		final TreeSet indexChars = new TreeSet();
		ExtractedAttributeValue(String name, String rValue, String nValue, String sValue, ImWord[] words) {
			this.name = name;
			this.rValue = trim(rValue);
			this.nValue = trim(nValue);
			this.sValue = trim(sValue);
			this.words = words;
			this.firstWord = this.words[0];
			this.lastWord = this.words[this.words.length - 1];
		}
		private static String trim(String str) {
			return ((str == null) ? null : str.trim());
		}
		boolean conflictsWith(ExtractedAttributeValue eav) {
			if (eav == this)
				return false;
			if (!this.name.equals(eav.name))
				return false;
			if (ImUtils.textStreamOrder.compare(this.lastWord, eav.firstWord) < 0)
				return false;
			if (ImUtils.textStreamOrder.compare(eav.lastWord, this.firstWord) < 0)
				return false;
			return true;
		}
		public String getType() {
			return this.name;
		}
		public void setType(String type) {}
		public String getLocalID() {
			return (this.name + "@" + this.firstWord.getLocalID() + "-" + this.lastWord.getLocalID());
		}
		public String getLocalUID() {
			return AnnotationUuidHelper.getLocalUID(this);
		}
		public String getUUID() {
			return AnnotationUuidHelper.getUUID(this);
		}
		public ImDocument getDocument() {
			return this.firstWord.getDocument();
		}
		public String getDocumentProperty(String propertyName) {
			return this.firstWord.getDocumentProperty(propertyName);
		}
		public String getDocumentProperty(String propertyName, String defaultValue) {
			return this.firstWord.getDocumentProperty(propertyName, defaultValue);
		}
		public String[] getDocumentPropertyNames() {
			return this.firstWord.getDocumentPropertyNames();
		}
		public ImWord getFirstWord() {
			return this.firstWord;
		}
		public void setFirstWord(ImWord firstWord) {}
		public ImWord getLastWord() {
			return this.lastWord;
		}
		public void setLastWord(ImWord lastWord) {}
		public int compareTo(Object obj) {
			ExtractedAttributeValue eav = ((ExtractedAttributeValue) obj);
			if (eav == this)
				return 0;
			int c;
			c = this.name.compareTo(eav.name);
			if (c != 0)
				return c;
			c = ImUtils.textStreamOrder.compare(this.firstWord, eav.firstWord);
			if (c != 0)
				return c;
			c = ImUtils.textStreamOrder.compare(eav.lastWord, this.lastWord);
			if (c != 0)
				return c;
			return 0;
		}
	}
	
	private static boolean extractAuthorDetail(ImAnnotation[] docAuthors, String name, boolean isIdentifier, ImDocumentStyle allDetailsStyle, RefData ref, HashMap attributesToValues, PrintStream log) {
		
		//	get attribute specific parameters
		ImDocumentStyle detailStyle = allDetailsStyle.getImSubset(name);
		
		//	collect match words
		ArrayList attributeValues = new ArrayList(docAuthors.length);
		
		//	extract matches from page authors are annotated in
		ImPage page = docAuthors[0].getDocument().getPage(docAuthors[0].getFirstWord().pageId);
		if (extractAuthorDetail(docAuthors, name, isIdentifier, page, page.getImageDPI(), detailStyle, allDetailsStyle, ref, attributeValues, log)) {
			attributesToValues.put((AUTHOR_ANNOTATION_TYPE + "-" + name), attributeValues);
			return true;
		}
//		
//		//	try extraction from document start first
//		maxFromStartPageId = detailStyle.getIntProperty("maxPageId", maxFromStartPageId);
//		for (int p = 0; (p <= maxFromStartPageId) && (p < pages.length); p++) {
//			int dpi = pages[p].getImageDPI();
//			if (extractAuthorDetail(docAuthors, name, isIdentifier, pages[p], dpi, detailStyle, allDetailsStyle, ref, attributeValues, log)) {
//				attributesToValues.put(name, attributeValues);
//				return true;
//			}
//		}
//		
//		//	try extraction from document end only second
//		maxFromEndPageId = detailStyle.getIntProperty("maxFromEndPageId", maxFromEndPageId);
//		for (int p = (pages.length - maxFromEndPageId); p < pages.length; p++) {
//			int dpi = pages[p].getImageDPI();
//			if (extractAuthorDetail(docAuthors, name, isIdentifier, pages[p], dpi, detailStyle, allDetailsStyle, ref, attributeValues, log)) {
//				attributesToValues.put(name, attributeValues);
//				return true;
//			}
//		}
		
		//	remember any existing values
		if (attributeValues.size() != 0)
			attributesToValues.put((AUTHOR_ANNOTATION_TYPE + "-" + name), attributeValues);
		
		//	nothing helped ...
		return false;
	}
	
	private static boolean extractAuthorDetail(ImAnnotation[] authorAnnots, String name, boolean isIdentifier, ImPage page, int dpi, ImDocumentStyle detailStyle, ImDocumentStyle allDetailsStyle, RefData ref, ArrayList extractedValues, PrintStream log) {
		
		//	get document authors
		if (authorAnnots == null)
			authorAnnots = page.getDocument().getAnnotations("docAuthor", page.pageId);
		if (authorAnnots.length == 0) {
			log.println("No document author annotations found");
			return false;
		}
		
		//	check for fixed value (this _can_ be the case for house journals)
		String fixedValue = (fixableFieldNames.contains(name) ? detailStyle.getStringProperty("fixedValue", null) : null);
		if (fixedValue != null) {
			log.println(" ==> fixed to " + fixedValue);
			if (ref != null)
				for (int a = 0; a < authorAnnots.length; a++) {
					String authorName;
					String sAuthorName;
					if (authorAnnots[a] instanceof ExtractedAttributeValue) {
						authorName = ((ExtractedAttributeValue) authorAnnots[a]).rValue;
						sAuthorName = ((ExtractedAttributeValue) authorAnnots[a]).sValue;
					}
					else {
						authorName = ImUtils.getString(authorAnnots[a].getFirstWord(), authorAnnots[a].getLastWord(), true);
						sAuthorName = sanitizeAttributeValue(AUTHOR_ANNOTATION_TYPE, authorName, false);
					}
					AuthorData authorData = ref.getAuthorData(sAuthorName);
					if (authorData != null)
						authorData.setAttribute(name, fixedValue);
				}
			return false;
		}
		
//		//	author detail already set, get annotations and we're done
//		//	NOT POSSIBLE HERE, WE ALSO NEED ASSOCIATIONS
//		if ((ref != null) && (ref.getAttribute(name) != null)) {
//			String annotType = ("docAuthor" + Character.toUpperCase(name.charAt(0)) + name.substring(1));
//			ImAnnotation[] annots = page.getDocument().getAnnotations(annotType, page.pageId);
//			for (int a = 0; a < annots.length; a++) {
//				String value = ImUtils.getString(annots[a].getFirstWord(), annots[a].getLastWord(), true);
//				if (isIdentifier)
//					value = value.replaceAll("\\s", "");
//				log.println("Got existing " + name + " from " + annotType + ": " + value);
//				String sValue = sanitizeAttributeValue(name, value, isIdentifier);
//				if (sValue == null)
//					sValue = ref.getAttribute(name);
//				attributeValues.add(new ExtractedAttributeValue(name, value, StringUtils.normalizeString(value), sValue, getSpannedWords(annots[a].getFirstWord(), annots[a].getLastWord())));
//			}
//			if (attributeValues.isEmpty())
//				log.println("Existing " + name + " not found from " + annotType);
//			return false;
//		}
		log.println("Extracting author " + name + ":");
		
		/* TODOne get association mechanism
Specify maximum block or paragraph distance for proximity association
- horizontally, measure for first word (rightward) or last word (leftward)
- vertically, measure for all words (at least in topmost line)
==> visualize accordingly in parameter group descriptions for template editor (most likely akin to block and column margins)
		 */
		String associationData = detailStyle.getStringProperty("indexChars", "");
		
		//	work author by author for proximity association
		if ("BELOW".equals(associationData) || "RIGHT".equals(associationData) || "ABOVE".equals(associationData) || "LEFT".equals(associationData)) {
			log.println(" - using proximity association " + associationData.toLowerCase() + " author names");
			
			//	get extraction area (default to page boundaries) and position
			BoundingBox detailValueArea = detailStyle.getBoxProperty("area", page.bounds, dpi);
			log.println(" - general extraction area is " + detailValueArea);
			String detailValueAreaPositionHorizontal = ((detailValueArea == page.bounds) ? "ABSOLUTE" : detailStyle.getStringProperty("areaPositionHorizontal", null));
			String detailValueAreaPositionVertical = ((detailValueArea == page.bounds) ? "ABSOLUTE" : detailStyle.getStringProperty("areaPositionVertical", null));
			if ((detailValueAreaPositionHorizontal == null) && (detailValueAreaPositionVertical == null)) {
				log.println(" - extraction area horizontal position is " + detailValueAreaPositionHorizontal);
				detailValueAreaPositionHorizontal = "ABSOLUTE";
				log.println("   ==> defaulted to " + detailValueAreaPositionHorizontal);
				log.println(" - extraction area vertical position is " + detailValueAreaPositionVertical);
				detailValueAreaPositionVertical = "ABSOLUTE";
				log.println("   ==> defaulted to " + detailValueAreaPositionVertical);
			}
			else {
				log.println(" - extraction area horizontal position is " + detailValueAreaPositionHorizontal);
				if (detailValueAreaPositionHorizontal == null) {
					if ("RIGHT".equals(associationData))
						detailValueAreaPositionHorizontal = "RIGHT";
					else if ("LEFT".equals(associationData))
						detailValueAreaPositionHorizontal = "LEFT";
					else detailValueAreaPositionHorizontal = "ABSOLUTE";
					log.println("   ==> defaulted to " + detailValueAreaPositionHorizontal);
				}
				log.println(" - extraction area vertical position is " + detailValueAreaPositionVertical);
				if (detailValueAreaPositionVertical == null) {
					if ("BELOW".equals(associationData))
						detailValueAreaPositionVertical = "DOWN";
					else if ("ABOVE".equals(associationData))
						detailValueAreaPositionVertical = "UP";
					else detailValueAreaPositionVertical = "ABSOLUTE";
					log.println("   ==> defaulted to " + detailValueAreaPositionVertical);
				}
			}
			
			//	get maximum distance (default to page height or width, depending on direction of association)
			int detailValueMaxDistance = detailStyle.getIntProperty("maxDistanceFromAuthorAnnots", (("RIGHT".equals(associationData) || "LEFT".equals(associationData)) ? page.bounds.getWidth() : page.bounds.getHeight()), dpi);
			log.println(" - maximum distance is " + detailValueMaxDistance);
			
			//	extract author name areas
			BoundingBox[] authorBoxes = new BoundingBox[authorAnnots.length];
			for (int a = 0; a < authorAnnots.length; a++) {
				ArrayList authorWords = new ArrayList();
				for (ImWord imw = authorAnnots[a].getFirstWord(); imw != null; imw = imw.getNextWord()) {
					authorWords.add(imw);
					if (imw == authorAnnots[a].getLastWord())
						break;
				}
				authorBoxes[a] = ImLayoutObject.getAggregateBox((ImWord[]) authorWords.toArray(new ImWord[authorWords.size()]));
				log.println(" - got bounds for " + ImUtils.getString(authorAnnots[a].getFirstWord(), authorAnnots[a].getLastWord(), true) + ": " + authorBoxes[a]);
			}
			
			//	work author name by author name
			BoundingBox[] vaBounds = getAuthorDetailValueAreas(authorAnnots, page, associationData, detailStyle, dpi, log);
			int addedDetailCount = 0;
			for (int a = 0; a < authorAnnots.length; a++) {
				String authorName = ImUtils.getString(authorAnnots[a].getFirstWord(), authorAnnots[a].getLastWord(), true);
				log.println(" - handling " + authorName + ":");
				String sAuthorName = sanitizeAttributeValue(AUTHOR_ANNOTATION_TYPE, authorName, false);
				log.println("   - normalized to " + sAuthorName);
				AuthorData authorData = ((ref == null) ? null : ref.getAuthorData(sAuthorName));
				
				//	anything to work with?
				if (vaBounds[a] == null)
					continue;
				
				//	collect words
				ImWord[] detailWords = page.getWordsInside(vaBounds[a]);
				log.println("   - got " + detailWords.length + " words in area");
				if (detailWords.length == 0)
					continue;
				
				//	extract attribute value
				ExtractedAttributeValue detailValue = extractAuthorDetailValue(name, detailWords, 0, detailWords.length, isIdentifier, detailStyle, log);
				if (detailValue == null)
					continue;
				
				//	store value
				addedDetailCount++;
				if (authorData != null)
					authorData.setAttribute(name, (isIdentifier ? detailValue.sValue.replaceAll("\\s", "") : detailValue.sValue));
				extractedValues.add(detailValue);
			}
			
			//	indicate whether or not we actually found something
			return (addedDetailCount != 0);
		}
		
		//	work author by author for in-line association
		else if ("INLINE-RIGHT".equals(associationData) || "INLINE-LEFT".equals(associationData)) {
			log.println(" - using in-line association to " + associationData.substring("INLINE-".length()).toLowerCase() + " of author names");
			
			HashSet authorAnnotStartEndWords = new HashSet();
			for (int a = 0; a < authorAnnots.length; a++) {
				authorAnnotStartEndWords.add(authorAnnots[a].getFirstWord());
				authorAnnotStartEndWords.add(authorAnnots[a].getLastWord());
			}
			
			int addedDetailCount = 0;
			for (int a = 0; a < authorAnnots.length; a++) {
				String authorName = ImUtils.getString(authorAnnots[a].getFirstWord(), authorAnnots[a].getLastWord(), true);
				log.println(" - handling " + authorName + ":");
				String sAuthorName = sanitizeAttributeValue(AUTHOR_ANNOTATION_TYPE, authorName, false);
				log.println("   - normalized to " + sAuthorName);
				AuthorData authorData = ((ref == null) ? null : ref.getAuthorData(sAuthorName));
				
				ArrayList detailWords = new ArrayList();
				if ("INLINE-RIGHT".equals(associationData)) {
					for (ImWord imw = authorAnnots[a].getLastWord().getNextWord(); imw != null; imw = imw.getNextWord()) {
						if (authorAnnotStartEndWords.contains(imw))
							break;
						detailWords.add(imw);
						if (imw.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
							break;
					}
				}
				else if ("INLINE-LEFT".equals(associationData)) {
					for (ImWord imw = authorAnnots[a].getFirstWord().getPreviousWord(); imw != null; imw = imw.getPreviousWord()) {
						if (authorAnnotStartEndWords.contains(imw))
							break;
						detailWords.add(0, imw);
						if (imw.getPreviousRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
							break;
					}
				}
				log.println("   - got " + detailWords.size() + " words in area");
				if (detailWords.isEmpty())
					continue;
				
				ExtractedAttributeValue detailValue = extractAuthorDetailValue(name, ((ImWord[]) detailWords.toArray(new ImWord[detailWords.size()])), 0, detailWords.size(), isIdentifier, detailStyle, log);
				if (detailValue == null)
					continue;
				
				addedDetailCount++;
				if (authorData != null)
					authorData.setAttribute(name, (isIdentifier ? detailValue.sValue.replaceAll("\\s", "") : detailValue.sValue));
				extractedValues.add(detailValue);
			}
			
			//	indicate whether or not we actually found something
			return (addedDetailCount != 0);
		}
		
		//	work cumulatively for indexed association
		else {
			log.println(" - using index based association");
			TreeSet indexChars = parseCharList(associationData, log);
			log.println(" - index characters are " + indexChars);
			TreeSet allIndexChars = parseCharList(allDetailsStyle.getStringProperty("indexChars", associationData), log);
			log.println(" - accepted index characters are " + allIndexChars);
			
			String indexCharPositionOnAuthorAnnots = detailStyle.getStringProperty("indexCharPositionOnAuthorAnnots", null);
			log.println(" - position of index characters on authors is " + indexCharPositionOnAuthorAnnots);
			if (indexCharPositionOnAuthorAnnots == null)
				return false;
			
			//	get author font size for superscript check
			int authorFontSize = -1;
			if (indexCharPositionOnAuthorAnnots.endsWith("-superscript")) {
				int authorFontSizeSum = 0;
				int authorFontSizeCount = 0;
				for (int a = 0; a < authorAnnots.length; a++) {
					int afs = getAverageFontSize(authorAnnots[a].getFirstWord(), authorAnnots[a].getLastWord(), true);
					if (afs != -1) {
						authorFontSizeSum += afs;
						authorFontSizeCount++;
					}
				}
				if (authorFontSizeCount == 0) {
					log.println(" ==> could not determine author font size");
					return false;
				}
				authorFontSize = ((authorFontSizeSum + (authorFontSizeCount / 2)) / authorFontSizeCount);
				log.println(" - average font size of authors is " + authorFontSize + " (" + authorFontSizeSum + "/" + authorFontSizeCount + ")");
			}
			
			//	index starts and ends of authors
			HashSet authorAnnotStartEndWords = new HashSet();
			for (int a = 0; a < authorAnnots.length; a++) {
				authorAnnotStartEndWords.add(authorAnnots[a].getFirstWord());
				authorAnnotStartEndWords.add(authorAnnots[a].getLastWord());
			}
			
			//	find (superscript) index characters for authors (need to preserve order here)
			LinkedHashSet[] authorIndexChars = new LinkedHashSet[authorAnnots.length];
			log.println(" - getting index characters for authors:");
			for (int a = 0; a < authorAnnots.length; a++) {
				authorIndexChars[a] = new LinkedHashSet(2);
				if (indexCharPositionOnAuthorAnnots.startsWith("leading")) {
					for (ImWord imw = authorAnnots[a].getFirstWord().getPreviousWord(); imw != null; imw = imw.getPreviousWord()) {
						if (imw.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
							break; // need to stop only _after_ paragraph break
						if (authorAnnotStartEndWords.contains(imw))
							break;
						if ((authorFontSize != -1) && (authorFontSize <= imw.getFontSize()))
							continue;
						String imwStr = imw.getString();
						if (imwStr == null)
							continue;
						//	TODOne split word string at any embedded commas (decimal dot vs. decimal comma _might_ go wrong sometimes)
						String[] ics = imwStr.split("[\\,\\;]");
						if (ics.length > 1)
							for (int i = 0; i < ics.length; i++) {
								if (indexChars.contains(ics[i]))
									authorIndexChars[a].add(ics[i]);
							}
						if (indexChars.contains(imwStr))
							authorIndexChars[a].add(imwStr);
						else if (!allIndexChars.contains(imwStr) && (",;".indexOf(imwStr) == -1))
							break;
					}
				}
				else if (indexCharPositionOnAuthorAnnots.startsWith("tailing")) {
					for (ImWord imw = authorAnnots[a].getLastWord().getNextWord(); imw != null; imw = imw.getNextWord()) {
						if (imw.getPreviousRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
							break; // need to stop only _after_ paragraph break
						if (authorAnnotStartEndWords.contains(imw))
							break;
						if ((authorFontSize != -1) && (authorFontSize <= imw.getFontSize()))
							continue;
						String imwStr = imw.getString();
						if (imwStr == null)
							continue;
						//	TODOne split word string at any embedded commas (decimal dot vs. decimal comma _might_ go wrong sometimes)
						String[] ics = imwStr.split("[\\,\\;]");
						if (ics.length > 1)
							for (int i = 0; i < ics.length; i++) {
								if (indexChars.contains(ics[i]))
									authorIndexChars[a].add(ics[i]);
							}
						if (indexChars.contains(imwStr))
							authorIndexChars[a].add(imwStr);
						else if (!allIndexChars.contains(imwStr) && (",;".indexOf(imwStr) == -1))
							break;
					}
				}
				log.println("   - for " + ImUtils.getString(authorAnnots[a].getFirstWord(), authorAnnots[a].getLastWord(), true) + ": " + authorIndexChars[a]);
			}
			
			/* Add parameter "indexPositionOnDetailValues":
- leading superscript
- leading
- tailing superscript
- tailing
			 */
			String indexCharPositionOnDetailValues = detailStyle.getStringProperty("indexCharPositionOnDetailValues", null);
			log.println(" - position of index characters on values is " + indexCharPositionOnDetailValues);
			if (indexCharPositionOnDetailValues == null)
				return false;
			
			/* Add (and handle) parameter "detailValueSeparator":
- "PARAGRAPH": detail value is whole paragraph, with potential line wraps
- "LINE": detail value is single line, ending at line wraps (ImUtils.isTextFlowBreak())
- "INDEX": detail value ends at (superscript) character in index character set (as well as paragraph break)
- other characters specify separator(s) directly (separate with spaces)
			 */
			String detailValueSeparator = detailStyle.getStringProperty("detailValueSeparator", null);
			log.println(" - value separator is " + detailValueSeparator);
			if (detailValueSeparator == null)
				return false;
			
			/*
- add set of index chars to ExtractedAttributeValue class
  - populate that set in former extractValues() method that extracts the index chars
  - use that set in assigning detail values to authors
			 */
			BoundingBox detailValueArea = detailStyle.getBoxProperty("area", null, dpi);
			log.println(" - value area is " + detailValueArea);
			if (detailValueArea == null)
				return false;
			ArrayList detailValues = new ArrayList() {
				public boolean add(Object e) {
					return ((e == null) ? false : super.add(e));
				}
			};
			if ("PARAGRAPH".equals(detailValueSeparator)) {
				ImRegion[] paragraphs = page.getRegionsInside(ImRegion.PARAGRAPH_TYPE, detailValueArea, false);
				Arrays.sort(paragraphs, ImUtils.topDownOrder);
				for (int p = 0; p < paragraphs.length; p++) {
					log.println(" - seeking paragraph " + paragraphs[p].bounds);
					ImWord[] pWords = paragraphs[p].getWords();
					Arrays.sort(pWords, ImUtils.textStreamOrder);
					log.println("   - got " + pWords.length + " words");
					boolean[] isSuperscript = null;
					if (indexCharPositionOnDetailValues.endsWith("-superscript")) {
						isSuperscript = getSuperscriptMarkers(pWords, true);
						if (isSuperscript == null) {
							log.println("   ==> could not determine paragraph font size");
							continue;
						}
						log.println("   - superscript is " + Arrays.toString(isSuperscript));
					}
					detailValues.add(extractAuthorDetailValue(name, pWords, isSuperscript, 0, pWords.length, isIdentifier, indexCharPositionOnDetailValues, indexChars, detailStyle, log));
				}
			}
			else if ("LINE".equals(detailValueSeparator)) {
				ImRegion[] lines = page.getRegionsInside(ImRegion.LINE_ANNOTATION_TYPE, detailValueArea, false);
				Arrays.sort(lines, ImUtils.topDownOrder);
				for (int l = 0; l < lines.length; l++) {
					log.println(" - seeking line " + lines[l].bounds);
					ImWord[] lWords = lines[l].getWords();
					Arrays.sort(lWords, ImUtils.textStreamOrder);
					log.println("   - got " + lWords.length + " words");
					boolean[] isSuperscript = null;
					if (indexCharPositionOnDetailValues.endsWith("-superscript")) {
						isSuperscript = getSuperscriptMarkers(lWords, false);
						if (isSuperscript == null) {
							log.println("   ==> could not determine line font size");
							continue;
						}
						log.println("   - superscript is " + Arrays.toString(isSuperscript));
					}
					detailValues.add(extractAuthorDetailValue(name, lWords, isSuperscript, 0, lWords.length, isIdentifier, indexCharPositionOnDetailValues, indexChars, detailStyle, log));
				}
			}
			else if ("INDEX".equals(detailValueSeparator)) {
				ImRegion[] paragraphs = page.getRegionsInside(ImRegion.PARAGRAPH_TYPE, detailValueArea, false);
				Arrays.sort(paragraphs, ImUtils.topDownOrder);
				for (int p = 0; p < paragraphs.length; p++) {
					log.println(" - seeking paragraph " + paragraphs[p].bounds);
					ImWord[] pWords = paragraphs[p].getWords();
					Arrays.sort(pWords, ImUtils.textStreamOrder);
					log.println("   - got " + pWords.length + " words overall");
					boolean[] isSuperscript = null;
					if (indexCharPositionOnDetailValues.endsWith("-superscript")) {
						isSuperscript = getSuperscriptMarkers(pWords, true);
						if (isSuperscript == null) {
							log.println("   ==> could not determine paragraph font size");
							continue;
						}
						log.println("   - superscript is " + Arrays.toString(isSuperscript));
					}
					boolean[] isIndexChar = new boolean[pWords.length];
					Arrays.fill(isIndexChar, false);
					for (int w = 0; w < pWords.length; w++) {
						if ((isSuperscript != null) && !isSuperscript[w])
							continue;
						String imwStr = pWords[w].getString();
						if (imwStr == null)
							continue;
						//	TODOne split word string at any embedded commas (decimal dot vs. decimal comma _might_ go wrong sometimes)
						String[] ics = imwStr.split("[\\,\\;]");
						if (ics.length > 1)
							for (int i = 0; i < ics.length; i++) {
								if (indexChars.contains(ics[i]))
									isIndexChar[w] = true;
							}
						if (indexChars.contains(imwStr))
							isIndexChar[w] = true;
						else if ((",;".indexOf(imwStr) != -1) && (w != 0) && isIndexChar[w-1])
							isIndexChar[w] = true;
					}
					if (indexCharPositionOnDetailValues.startsWith("leading")) {
						for (int w = 0; w < pWords.length;) {
							if (isIndexChar[w]) {
								int sw = w;
								while ((w < pWords.length) && isIndexChar[w])
									w++; // consume (block of) index chars
								while ((w < pWords.length) && !isIndexChar[w])
									w++; // consume value
								log.println("   - extracting value from " + sw + " to " + w);
								detailValues.add(extractAuthorDetailValue(name, pWords, isSuperscript, sw, w, isIdentifier, indexCharPositionOnDetailValues, indexChars, detailStyle, log));
							}
							else w++;
						}
					}
					else if (indexCharPositionOnDetailValues.startsWith("tailing")) {
						for (int w = 0; w < pWords.length;) {
							if (!isIndexChar[w]) {
								int sw = w;
								while ((w < pWords.length) && !isIndexChar[w])
									w++; // consume value
								while ((w < pWords.length) && isIndexChar[w])
									w++; // consume (block of) index chars
								log.println("   - extracting value from " + sw + " to " + w);
								detailValues.add(extractAuthorDetailValue(name, pWords, isSuperscript, sw, w, isIdentifier, indexCharPositionOnDetailValues, indexChars, detailStyle, log));
							}
							else w++;
						}
					}
				}
			}
			else {
				TreeSet valueSeparators = parseCharList(detailValueSeparator, log);
				log.println(" - value separators " + valueSeparators);
				
				ImRegion[] paragraphs = page.getRegionsInside(ImRegion.PARAGRAPH_TYPE, detailValueArea, false);
				Arrays.sort(paragraphs, ImUtils.topDownOrder);
				for (int p = 0; p < paragraphs.length; p++) {
					log.println(" - seeking paragraph " + paragraphs[p].bounds);
					ImWord[] pWords = paragraphs[p].getWords();
					Arrays.sort(pWords, ImUtils.textStreamOrder);
					log.println("   - got " + pWords.length + " words");
					boolean[] isSuperscript = null;
					if (indexCharPositionOnDetailValues.endsWith("-superscript")) {
						isSuperscript = getSuperscriptMarkers(pWords, true);
						if (isSuperscript == null) {
							log.println("   ==> could not determine paragraph font size");
							continue;
						}
						log.println("   - superscript is " + Arrays.toString(isSuperscript));
					}
					int sw = -1;
					for (int w = 0; w < pWords.length; w++) {
						String imwStr = pWords[w].getString();
						if (imwStr == null)
							continue;
						if (valueSeparators.contains(imwStr)) {
							if (sw != -1) {
								log.println("   - extracting value from " + sw + " to " + w);
								detailValues.add(extractAuthorDetailValue(name, pWords, isSuperscript, sw, w, isIdentifier, indexCharPositionOnDetailValues, indexChars, detailStyle, log));
							}
							sw = -1; // start over after separator
						}
						else if (sw == -1)
							sw = w;
					}
					if (sw != -1) {
						log.println("   - extracting value from " + sw + " to " + pWords.length);
						detailValues.add(extractAuthorDetailValue(name, pWords, isSuperscript, sw, pWords.length, isIdentifier, indexCharPositionOnDetailValues, indexChars, detailStyle, log));
					}
				}
			}
			
			//	index values by index chars
			log.println(" - indexing extracted values");
			TreeMap detailValuesByIndexChars = new TreeMap();
			for (int v = 0; v < detailValues.size(); v++) {
				ExtractedAttributeValue detailValue = ((ExtractedAttributeValue) detailValues.get(v));
				log.println("   - " + detailValue.rValue + ": " + detailValue.indexChars);
				for (Iterator icit = detailValue.indexChars.iterator(); icit.hasNext();) {
					String indexChar = ((String) icit.next());
					ArrayList indexCharValues = ((ArrayList) detailValuesByIndexChars.get(indexChar));
					if (indexCharValues == null) {
						indexCharValues = new ArrayList(2);
						detailValuesByIndexChars.put(indexChar, indexCharValues);
					}
					indexCharValues.add(detailValue);
				}
			}
			
			boolean useFirstDetailValue = detailStyle.getBooleanProperty("useFirstDetailValue", false);
			log.println(" - first value restriction per index characters is " + useFirstDetailValue);
			boolean useFirstAuthorValue = detailStyle.getBooleanProperty("useFirstAuthorValue", false);
			log.println(" - first value restriction per author is " + useFirstAuthorValue);
			
			//	assign details to authors
			log.println(" - assigning values to authors");
			LinkedHashSet addedDetailValues = new LinkedHashSet();
			for (int a = 0; a < authorAnnots.length; a++) {
				if (authorIndexChars[a].isEmpty())
					continue;
				String authorName = ImUtils.getString(authorAnnots[a].getFirstWord(), authorAnnots[a].getLastWord(), true);
				log.println(" - handling " + authorName + ":");
				String sAuthorName = sanitizeAttributeValue(AUTHOR_ANNOTATION_TYPE, authorName, false);
				log.println("   - normalized to " + sAuthorName);
				AuthorData authorData = ((ref == null) ? null : ref.getAuthorData(sAuthorName));
				log.println("   - index characters are " + authorIndexChars[a]);
				ArrayList authorValues = new ArrayList();
				for (Iterator icit = authorIndexChars[a].iterator(); icit.hasNext();) {
					String indexChar = ((String) icit.next());
					ArrayList indexCharValues = ((ArrayList) detailValuesByIndexChars.get(indexChar));
					if (indexCharValues == null)
						continue;
//					String value;
//					if ((indexCharValues.size() == 1) || useFirstDetailValue)
//						value = ((ExtractedAttributeValue) indexCharValues.get(0)).sValue;
//					else {
//						StringBuffer valueStr = new StringBuffer(((ExtractedAttributeValue) indexCharValues.get(0)).sValue);
//						for (int v = 1; v < indexCharValues.size(); v++) {
//							valueStr.append(" & ");
//							valueStr.append(((ExtractedAttributeValue) indexCharValues.get(v)).sValue);
//						}
//						value = valueStr.toString();
//					}
//					if (isIdentifier)
//						value = value.replaceAll("\\s", "");
//					log.println("   - assigning " + value);
//					if (authorData != null)
//						authorData.setAttribute(name, value);
//					addedDetailValues.addAll(indexCharValues);
					if (useFirstDetailValue) {
						authorValues.add(indexCharValues.get(0));
						log.println("     - adding " + ((ExtractedAttributeValue) indexCharValues.get(0)).sValue);
						addedDetailValues.add(indexCharValues.get(0));
					}
					else {
						authorValues.addAll(indexCharValues);
						for (int v = 0; v < indexCharValues.size(); v++)
							log.println("     - adding " + ((ExtractedAttributeValue) indexCharValues.get(v)).sValue);
						addedDetailValues.addAll(indexCharValues);
					}
				}
				if (authorValues.isEmpty()) {
					log.println("   - no values found for index characters " + authorIndexChars[a]);
					continue;
				}
				String value;
				if ((authorValues.size() == 1) || useFirstAuthorValue)
					value = ((ExtractedAttributeValue) authorValues.get(0)).sValue;
				else {
					StringBuffer valueStr = new StringBuffer(((ExtractedAttributeValue) authorValues.get(0)).sValue);
					for (int v = 1; v < authorValues.size(); v++) {
						valueStr.append(" & ");
						valueStr.append(((ExtractedAttributeValue) authorValues.get(v)).sValue);
					}
					value = valueStr.toString();
				}
				if (isIdentifier)
					value = value.replaceAll("\\s", "");
				log.println("   - assigning " + value);
				if (authorData != null)
					authorData.setAttribute(name, value);
			}
			extractedValues.addAll(addedDetailValues);
			
			//	indicate whether or not we actually found something
			return (addedDetailValues.size() != 0);
			
			/*
ALSO, add (optional) QC rule sets checking author details ...
... and add respective parameters to style templates

ALSO, most likely create new version of Default.imagine configuration before finishing the above things
==> makes current progress (fixes in RefParse, reference tagger, TreeFAT, etc.) available
==> makes at least manual affiliation extraction available
			 */
			
			/*
- "PARAGRAPH": detail value is whole paragraph, with potential line wraps
  - TEST: EJT (affiliations and LSIDs, also for distinguishing the two, mainly for telling affiliations (that are far too diverse for a good value pattern) from LSIDs)
    ==> maybe hard code fact that affiliation has many title case words to help with weighting (detail name is constant, anyway, after all)
- "LINE": detail value is single line, ending at line wraps (ImUtils.isTextFlowBreak())
  - TEST: EJT author LSID block merged to single paragraph for test
- "INDEX": detail value ends at (superscript) character in index character set (as well as paragraph break)
  - TEST: affiliations in Journal of Virology-2014-Corman-11297.full.pdf.imf (that corona virus paper)
- other characters specify separator(s) directly (separate with spaces)
  - TEST: semicolon for affiliations in Journal of Virology-2014-Corman-11297.full.pdf.imf (that corona virus paper)
			 */
		}
	}
	
	private static TreeSet parseCharList(String charString, PrintStream log) {
		String[] chars = charString.trim().split("\\s+");
		TreeSet charSet = new TreeSet();
		for (int c = 0; c < chars.length; c++) {
			if (chars[c].contains("\\")) {
				String uChar = unescapeString(chars[c]);
				if (chars[c].equals(uChar))
					log.println("Could not parse character " + chars[c]);
				else chars[c] = uChar;
			}
			charSet.add(chars[c]);
		}
		return charSet;
	}
	
	private static String unescapeString(String str) {
		StringBuffer unescaped = new StringBuffer(str.length());
		for (int c = 0; c < str.length(); c++) {
			char ch = str.charAt(c);
			if (ch != '\\') {
				unescaped.append(ch);
				continue;
			}
			if (str.startsWith("\\u", c) && ((c + "uFFFF".length()) < str.length())) try {
				unescaped.append((char) Integer.parseInt(str.substring((c + "\\u".length()), (c + "\\uFFFF".length())), 16));
				c += "uFFFF".length(); // loop increment handles backslash proper
				continue;
			} catch (RuntimeException re) {}
			if (str.startsWith("\\x", c) && ((c + "xFF".length()) < str.length())) try {
				unescaped.append((char) Integer.parseInt(str.substring((c + "\\x".length()), (c + "\\xFF".length())), 16));
				c += "xFF".length(); // loop increment handles backslash proper
				continue;
			} catch (RuntimeException re) {}
			unescaped.append(ch);
		}
		return unescaped.toString();
	}
	
	private static BoundingBox[] getAuthorDetailValueAreas(ImAnnotation[] authorAnnots, ImPage page, String associationData, ImDocumentStyle detailStyle, int dpi, PrintStream log) {
		BoundingBox[] vaBounds = new BoundingBox[authorAnnots.length];
		
		//	get extraction area (default to page boundaries) and position
		BoundingBox detailValueArea = detailStyle.getBoxProperty("area", page.bounds, dpi);
		if (log != null) log.println(" - general extraction area is " + detailValueArea);
		String detailValueAreaPositionHorizontal = ((detailValueArea == page.bounds) ? "ABSOLUTE" : detailStyle.getStringProperty("areaPositionHorizontal", null));
		String detailValueAreaPositionVertical = ((detailValueArea == page.bounds) ? "ABSOLUTE" : detailStyle.getStringProperty("areaPositionVertical", null));
		if ((detailValueAreaPositionHorizontal == null) && (detailValueAreaPositionVertical == null)) {
			if (log != null) log.println(" - extraction area horizontal position is " + detailValueAreaPositionHorizontal);
			detailValueAreaPositionHorizontal = "ABSOLUTE";
			if (log != null) log.println("   ==> defaulted to " + detailValueAreaPositionHorizontal);
			if (log != null) log.println(" - extraction area vertical position is " + detailValueAreaPositionVertical);
			detailValueAreaPositionVertical = "ABSOLUTE";
			if (log != null) log.println("   ==> defaulted to " + detailValueAreaPositionVertical);
		}
		else {
			if (log != null) log.println(" - extraction area horizontal position is " + detailValueAreaPositionHorizontal);
			if (detailValueAreaPositionHorizontal == null) {
				if ("RIGHT".equals(associationData))
					detailValueAreaPositionHorizontal = "RIGHT";
				else if ("LEFT".equals(associationData))
					detailValueAreaPositionHorizontal = "LEFT";
				else detailValueAreaPositionHorizontal = "ABSOLUTE";
				if (log != null) log.println("   ==> defaulted to " + detailValueAreaPositionHorizontal);
			}
			if (log != null) log.println(" - extraction area vertical position is " + detailValueAreaPositionVertical);
			if (detailValueAreaPositionVertical == null) {
				if ("BELOW".equals(associationData))
					detailValueAreaPositionVertical = "DOWN";
				else if ("ABOVE".equals(associationData))
					detailValueAreaPositionVertical = "UP";
				else detailValueAreaPositionVertical = "ABSOLUTE";
				if (log != null) log.println("   ==> defaulted to " + detailValueAreaPositionVertical);
			}
		}
		
		//	get maximum distance (default to page height or width, depending on direction of association)
		int detailValueMaxDistance = detailStyle.getIntProperty("maxDistanceFromAuthorAnnots", (("RIGHT".equals(associationData) || "LEFT".equals(associationData)) ? page.bounds.getWidth() : page.bounds.getHeight()), dpi);
		if (log != null) log.println(" - maximum distance is " + detailValueMaxDistance);
		
		//	extract author name areas
		BoundingBox[] authorBoxes = new BoundingBox[authorAnnots.length];
		for (int a = 0; a < authorAnnots.length; a++) {
			ArrayList authorWords = new ArrayList();
			for (ImWord imw = authorAnnots[a].getFirstWord(); imw != null; imw = imw.getNextWord()) {
				authorWords.add(imw);
				if (imw == authorAnnots[a].getLastWord())
					break;
			}
			authorBoxes[a] = ImLayoutObject.getAggregateBox((ImWord[]) authorWords.toArray(new ImWord[authorWords.size()]));
			if (log != null) log.println(" - got bounds for " + ImUtils.getString(authorAnnots[a].getFirstWord(), authorAnnots[a].getLastWord(), true) + ": " + authorBoxes[a]);
		}
		
		//	work author name by author name
		for (int a = 0; a < authorAnnots.length; a++) {
			String authorName = ImUtils.getString(authorAnnots[a].getFirstWord(), authorAnnots[a].getLastWord(), true);
			if (log != null) log.println(" - handling " + authorName + ":");
			String sAuthorName = sanitizeAttributeValue(AUTHOR_ANNOTATION_TYPE, authorName, false);
			if (log != null) log.println("   - normalized to " + sAuthorName);
			
			//	initialize extraction boundaries depending on direction of association
			int vaLeft;
			int vaRight;
			int vaTop;
			int vaBottom;
			if ("BELOW".equals(associationData)) {
				if ("RIGHT".equals(detailValueAreaPositionHorizontal))
					vaLeft = authorBoxes[a].left;
				else if ("CENTER".equals(detailValueAreaPositionHorizontal))
					vaLeft = (((authorBoxes[a].left + authorBoxes[a].right) / 2) - (detailValueArea.getWidth() / 2));
				else if ("LEFT".equals(detailValueAreaPositionHorizontal))
					vaLeft = (authorBoxes[a].right - detailValueArea.getWidth());
				else vaLeft = detailValueArea.left;
				if ("RIGHT".equals(detailValueAreaPositionHorizontal))
					vaRight = (authorBoxes[a].left + detailValueArea.getWidth());
				else if ("CENTER".equals(detailValueAreaPositionHorizontal))
					vaRight = (((authorBoxes[a].left + authorBoxes[a].right) / 2) + (detailValueArea.getWidth() / 2));
				else if ("LEFT".equals(detailValueAreaPositionHorizontal))
					vaRight = authorBoxes[a].right;
				else vaRight = detailValueArea.right;
				if ("ABSOLUTE".equals(detailValueAreaPositionVertical)) {
					vaTop = Math.max(detailValueArea.top, authorBoxes[a].bottom);
					vaBottom = Math.min(detailValueArea.bottom, (authorBoxes[a].bottom + detailValueMaxDistance));
				}
				else {
					vaTop = authorBoxes[a].bottom;
					vaBottom = (authorBoxes[a].bottom + detailValueArea.getHeight());
				}
			}
			else if ("RIGHT".equals(associationData)) {
				if ("ABSOLUTE".equals(detailValueAreaPositionHorizontal)) {
					vaLeft = Math.max(detailValueArea.right, authorBoxes[a].right);
					vaRight = Math.min(detailValueArea.right, (authorBoxes[a].right + detailValueMaxDistance));
				}
				else {
					vaLeft = authorBoxes[a].right;
					vaRight = (authorBoxes[a].right + detailValueArea.getWidth());
				}
				if ("DOWN".equals(detailValueAreaPositionVertical))
					vaTop = authorBoxes[a].top;
				else if ("CENTER".equals(detailValueAreaPositionVertical))
					vaTop = (((authorBoxes[a].top + authorBoxes[a].bottom) / 2) - (detailValueArea.getHeight() / 2));
				else if ("UP".equals(detailValueAreaPositionVertical))
					vaTop = (authorBoxes[a].bottom - detailValueArea.getHeight());
				else vaTop = authorBoxes[a].top;
				if ("DOWN".equals(detailValueAreaPositionVertical))
					vaBottom = (authorBoxes[a].top + detailValueArea.getHeight());
				else if ("CENTER".equals(detailValueAreaPositionVertical))
					vaBottom = (((authorBoxes[a].top + authorBoxes[a].bottom) / 2) + (detailValueArea.getHeight() / 2));
				else if ("UP".equals(detailValueAreaPositionVertical))
					vaBottom = authorBoxes[a].bottom;
				else vaBottom = detailValueArea.bottom;
			}
			else if ("ABOVE".equals(associationData)) {
				if ("RIGHT".equals(detailValueAreaPositionHorizontal))
					vaLeft = authorBoxes[a].left;
				else if ("CENTER".equals(detailValueAreaPositionHorizontal))
					vaLeft = (((authorBoxes[a].left + authorBoxes[a].right) / 2) - (detailValueArea.getWidth() / 2));
				else if ("LEFT".equals(detailValueAreaPositionHorizontal))
					vaLeft = (authorBoxes[a].right - detailValueArea.getWidth());
				else vaLeft = detailValueArea.left;
				if ("RIGHT".equals(detailValueAreaPositionHorizontal))
					vaRight = (authorBoxes[a].left + detailValueArea.getWidth());
				else if ("CENTER".equals(detailValueAreaPositionHorizontal))
					vaRight = (((authorBoxes[a].left + authorBoxes[a].right) / 2) + (detailValueArea.getWidth() / 2));
				else if ("LEFT".equals(detailValueAreaPositionHorizontal))
					vaRight = authorBoxes[a].right;
				else vaRight = detailValueArea.right;
				if ("ABSOLUTE".equals(detailValueAreaPositionVertical)) {
					vaTop = Math.max(detailValueArea.top, (authorBoxes[a].top - detailValueMaxDistance));
					vaBottom = Math.min(detailValueArea.bottom, authorBoxes[a].top);
				}
				else {
					vaTop = (authorBoxes[a].top - detailValueArea.getHeight());
					vaBottom = authorBoxes[a].top;
				}
			}
			else if ("LEFT".equals(associationData)) {
				if ("ABSOLUTE".equals(detailValueAreaPositionHorizontal)) {
					vaLeft = Math.max(detailValueArea.left, (authorBoxes[a].left - detailValueMaxDistance));
					vaRight = Math.min(detailValueArea.right, authorBoxes[a].left);
				}
				else {
					vaLeft = (authorBoxes[a].right - detailValueArea.getWidth());
					vaRight = authorBoxes[a].right;
				}
				if ("DOWN".equals(detailValueAreaPositionVertical))
					vaTop = authorBoxes[a].top;
				else if ("CENTER".equals(detailValueAreaPositionVertical))
					vaTop = (((authorBoxes[a].top + authorBoxes[a].bottom) / 2) - (detailValueArea.getHeight() / 2));
				else if ("UP".equals(detailValueAreaPositionVertical))
					vaTop = (authorBoxes[a].bottom - detailValueArea.getHeight());
				else vaTop = authorBoxes[a].top;
				if ("DOWN".equals(detailValueAreaPositionVertical))
					vaBottom = (authorBoxes[a].top + detailValueArea.getHeight());
				else if ("CENTER".equals(detailValueAreaPositionVertical))
					vaBottom = (((authorBoxes[a].top + authorBoxes[a].bottom) / 2) + (detailValueArea.getHeight() / 2));
				else if ("UP".equals(detailValueAreaPositionVertical))
					vaBottom = authorBoxes[a].bottom;
				else vaBottom = detailValueArea.bottom;
			}
			else continue; // never gonna happen, but Java don't know
			if (log != null) log.println("   - value area initialized to [" + vaLeft + "," + vaRight + "," + vaTop + "," + vaBottom + "]");
			
			//	use other authors as limits
			for (int ca = 0; ca < authorBoxes.length; ca++) {
				if (ca == a)
					continue;
				if (authorBoxes[ca].bottom < authorBoxes[a].top) // above current author
					vaTop = Math.max(vaTop, authorBoxes[ca].bottom);
				else if (authorBoxes[ca].top > authorBoxes[a].bottom) // below current author
					vaBottom = Math.min(vaBottom, authorBoxes[ca].top);
				else if (authorBoxes[ca].right < authorBoxes[a].left) // left of current author
					vaLeft = Math.max(vaLeft, authorBoxes[ca].right);
				else if (authorBoxes[ca].left > authorBoxes[a].right) // right of current author
					vaRight = Math.min(vaRight, authorBoxes[ca].left);
			}
			if (log != null) log.println("   - value area intersection reduced to [" + vaLeft + "," + vaRight + "," + vaTop + "," + vaBottom + "]");
			
			//	create final bounding box
			vaBounds[a] = new BoundingBox(vaLeft, vaRight, vaTop, vaBottom);
		}
		
		//	finally ...
		return vaBounds;
	}
	
	private static boolean[] getSuperscriptMarkers(ImWord[] words, boolean multiLine) {
		int fontSize = getAverageFontSize(words, true);
		if (fontSize == -1)
			return null;
		boolean[] isSuperscript = new boolean[words.length];
		Arrays.fill(isSuperscript, false);
		boolean gotSuperscript = false;
		for (int w = 0; w < words.length; w++) {
			int fs = words[w].getFontSize();
			if ((fs != -1) && (fs < fontSize)) {
				isSuperscript[w] = true;
				gotSuperscript = true;
				continue;
			}
			String str = words[w].getString();
			if (str == null)
				continue;
			for (int c = 0; c < str.length(); c++) {
				char ch = str.charAt(c);
				if (((ch < '\u2070') || (ch > '\u207F')) /* outside superscript Unicode block */ && ("*^°\u00B2\u00B3\u00B9".indexOf(ch) == -1) /* not a superscript character */) {
					str = null;
					break;
				}
			}
			if (str != null) {
				isSuperscript[w] = true;
				gotSuperscript = true;
			}
		}
		return (gotSuperscript ? isSuperscript : null);
		/* TODO add alternative baseline check:
  - compute weighted average baseline and middle line of author name proper ...
  - ... and count sufficiently pronounced elevation above baseline (bottom closer to middle line than baseline) as superscript as well
  ==> have to do this line by line
		 */
	}
	
	private static int getAverageFontSize(ImWord from, ImWord to, boolean weighted) {
		int fontSizeSum = 0;
		int fontSizeWeightSum = 0;
		for (ImWord imw = from; imw != null; imw = imw.getNextWord()) {
			int fs = imw.getFontSize();
			if (fs != -1) {
				int fsw = (weighted ? imw.getString().length() : 1);
				fontSizeSum += (fs * fsw);
				fontSizeWeightSum += fsw;
			}
			if (imw == to)
				break;
		}
		return ((fontSizeWeightSum == 0) ? -1 : ((fontSizeSum + (fontSizeWeightSum / 2)) / fontSizeWeightSum));
	}
	
	private static int getAverageFontSize(ImWord[] words, boolean weighted) {
		int fontSizeSum = 0;
		int fontSizeWeightSum = 0;
		for (int w = 0; w < words.length; w++) {
			int fs = words[w].getFontSize();
			if (fs != -1) {
				int fsw = (weighted ? words[w].getString().length() : 1);
				fontSizeSum += (fs * fsw);
				fontSizeWeightSum += fsw;
			}
		}
		return ((fontSizeWeightSum == 0) ? -1 : ((fontSizeSum + (fontSizeWeightSum / 2)) / fontSizeWeightSum));
	}
	
	private static ExtractedAttributeValue extractAuthorDetailValue(String name, ImWord[] words, boolean[] isSuperscript, int from, int to, boolean isIdentifier, String indexCharPosition, TreeSet indexChars, ImDocumentStyle detailStyle, PrintStream log) {
		
		//	find index chars (leading or tailing)
		TreeSet valueIndexChars = new TreeSet();
		if (indexCharPosition.startsWith("leading")) {
			for (; from < to; from++) {
				if ((isSuperscript != null) && !isSuperscript[from])
					break;
				String imwStr = words[from].getString();
				if (imwStr == null)
					continue;
				//	TODOne split word string at any embedded commas (decimal dot vs. decimal comma _might_ go wrong sometimes)
				String[] ics = imwStr.split("[\\,\\;]");
				if (ics.length > 1)
					for (int i = 0; i < ics.length; i++) {
						if (indexChars.contains(ics[i]))
							valueIndexChars.add(ics[i]);
					}
				if (indexChars.contains(imwStr))
					valueIndexChars.add(imwStr);
				else if (",;".indexOf(imwStr) == -1)
					break;
			}
		}
		else if (indexCharPosition.startsWith("tailing")) {
			for (; from < to; to--) {
				if ((isSuperscript != null) && !isSuperscript[to - 1])
					break;
				String imwStr = words[to - 1].getString();
				if (imwStr == null)
					continue;
				//	TODOne split word string at any embedded commas (decimal dot vs. decimal comma _might_ go wrong sometimes)
				String[] ics = imwStr.split("[\\,\\;]");
				if (ics.length > 1)
					for (int i = 0; i < ics.length; i++) {
						if (indexChars.contains(ics[i]))
							valueIndexChars.add(ics[i]);
					}
				if (indexChars.contains(imwStr))
					valueIndexChars.add(imwStr);
				else if (",;".indexOf(imwStr) == -1)
					break;
			}
		}
		log.println("   - index chars are " + valueIndexChars);
		if (valueIndexChars.isEmpty())
			return null;
		
		//	extract actual value
		ExtractedAttributeValue value = extractAuthorDetailValue(name, words, from, to, isIdentifier, detailStyle, log);
		if (value != null)
			value.indexChars.addAll(valueIndexChars);
		return value;
	}
	
	private static ExtractedAttributeValue extractAuthorDetailValue(String name, ImWord[] words, int from, int to, boolean isIdentifier, ImDocumentStyle detailStyle, PrintStream log) {
		if (to <= from)
			return null;
		if ((from != 0) || (to < words.length)) {
			ImWord[] valueWords = new ImWord[to - from];
			System.arraycopy(words, from, valueWords, 0, valueWords.length);
			words = valueWords;
		}
		log.println(" - got " + words.length + " words in area");
		
		ArrayList detailValues = new ArrayList(1);
		extractAttributeValue(name, isIdentifier, words, detailStyle, false, null, detailValues, log);
		return (detailValues.isEmpty() ? null : ((ExtractedAttributeValue) detailValues.get(0)));
	}
	
	void setDocumentAttributes(ImDocument doc, RefData ref) {
		
		//	store reference data proper
		BibRefUtils.toModsAttributes(ref, doc);
		this.refDataByDocId.remove(doc.docId);
		
		//	collect generalized attributes
		HashSet spuriousDocAttributeNames = new HashSet();
		spuriousDocAttributeNames.add(DOCUMENT_ORIGIN_ATTRIBUTE);
		spuriousDocAttributeNames.add(DOCUMENT_AUTHOR_ATTRIBUTE);
		spuriousDocAttributeNames.add(DOCUMENT_TITLE_ATTRIBUTE);
		spuriousDocAttributeNames.add(DOCUMENT_DATE_ATTRIBUTE);
		spuriousDocAttributeNames.add(DOCUMENT_SOURCE_LINK_ATTRIBUTE);
		spuriousDocAttributeNames.add(PAGE_NUMBER_ATTRIBUTE);
		spuriousDocAttributeNames.add(LAST_PAGE_NUMBER_ATTRIBUTE);
		
		//	handle pagination
		if (ref.hasAttribute(PAGINATION_ANNOTATION_TYPE)) {
			String[] pageNumbers = ref.getAttribute(PAGINATION_ANNOTATION_TYPE).trim().split("[^0-9]+");
			if (pageNumbers.length == 1) {
				doc.setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumbers[0]);
				spuriousDocAttributeNames.remove(PAGE_NUMBER_ATTRIBUTE);
			}
			else if (pageNumbers.length == 2) {
				doc.setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumbers[0]);
				spuriousDocAttributeNames.remove(PAGE_NUMBER_ATTRIBUTE);
				doc.setAttribute(LAST_PAGE_NUMBER_ATTRIBUTE, pageNumbers[1]);
				spuriousDocAttributeNames.remove(LAST_PAGE_NUMBER_ATTRIBUTE);
			}
		}
		
		//	set other generalized attributes
		if (setAttribute(doc, DOCUMENT_AUTHOR_ATTRIBUTE, ref, AUTHOR_ANNOTATION_TYPE, false))
			spuriousDocAttributeNames.remove(DOCUMENT_AUTHOR_ATTRIBUTE);
		if (setAttribute(doc, DOCUMENT_TITLE_ATTRIBUTE, ref, TITLE_ANNOTATION_TYPE, true))
			spuriousDocAttributeNames.remove(DOCUMENT_TITLE_ATTRIBUTE);
		if (setAttribute(doc, DOCUMENT_DATE_ATTRIBUTE, ref, YEAR_ANNOTATION_TYPE, true))
			spuriousDocAttributeNames.remove(DOCUMENT_DATE_ATTRIBUTE);
		if (setAttribute(doc, DOCUMENT_SOURCE_LINK_ATTRIBUTE, ref, PUBLICATION_URL_ANNOTATION_TYPE, true))
			spuriousDocAttributeNames.remove(DOCUMENT_SOURCE_LINK_ATTRIBUTE);
		
		//	add origin if applicable
		if (ref.hasAttribute(PAGINATION_ANNOTATION_TYPE) || ref.hasAttribute(VOLUME_TITLE_ANNOTATION_TYPE)) {
			String origin = refTypeSystem.getOrigin(ref);
			if ((origin != null) && (origin.trim().length() != 0)) {
				doc.setAttribute(DOCUMENT_ORIGIN_ATTRIBUTE, origin.trim());
				spuriousDocAttributeNames.remove(DOCUMENT_ORIGIN_ATTRIBUTE);
			}
		}
		
		//	remove spurious generalized attributes
		for (Iterator sanit = spuriousDocAttributeNames.iterator(); sanit.hasNext();) {
			String spuriousDocAttributeName = ((String) sanit.next());
			doc.removeAttribute(spuriousDocAttributeName);
		}
	}
	
	private static boolean setAttribute(ImDocument doc, String docAttributeName, RefData ref, String refAttributeName, boolean onlyFirst) {
		String[] values = ref.getAttributeValues(refAttributeName);
		if (values == null)
			return false;
		String value;
		if (onlyFirst || (values.length == 1))
			value = values[0];
		else {
			StringBuffer valueBuilder = new StringBuffer();
			for (int v = 0; v < values.length; v++) {
				if (v != 0)
					valueBuilder.append(" & ");
				valueBuilder.append(values[v]);
			}
			value = valueBuilder.toString();
		}
		doc.setAttribute(docAttributeName, value);
		return true;
	}
	
	private void annotateAttributeValues(RefData ref, String[] refAttributeNames, String[] allAttributeNames, boolean isIdentifierType, ImDocument doc, HashMap attributesToValues, boolean auto) {
		HashSet cleanupAttributeNames = new HashSet(Arrays.asList(allAttributeNames));
		ImDocumentRoot wrappedDoc = null;
		for (int an = 0; an < refAttributeNames.length; an++) {
			if (isIdentifierType ? "".equals(refAttributeNames[an]) : "ID".equals(refAttributeNames[an]))
				continue; // have to skip over wildcard ID type
			String refAttributeName = ((isIdentifierType ? "ID-" : "") + refAttributeNames[an]);
			String[] refAttributeValues = ref.getAttributeValues(refAttributeName);
			String valueAnnotType = ((isIdentifierType ? "docId" : "doc") + Character.toUpperCase(refAttributeNames[an].charAt(0)) + refAttributeNames[an].substring(1));
			wrappedDoc = this.annotateAttributeValues(refAttributeName, refAttributeValues, isIdentifierType, valueAnnotType, attributesToValues, cleanupAttributeNames, doc, wrappedDoc);
		}
		
		//	nothing to clean up (we're strictly adding in automated mode)
		if (auto || cleanupAttributeNames.isEmpty())
			return;
		
		//	clean up any annotations of removed attributes
		for (Iterator canit = cleanupAttributeNames.iterator(); canit.hasNext();) {
			String cleanupAttributeName = ((String) canit.next());
			if (isIdentifierType && "".equals(cleanupAttributeName))
				continue; // have to skip over wildcard ID type
			this.logInMasterConfiguration("Cleaning up " + cleanupAttributeName + ":");
			
			//	get existing annotations
			String valueAnnotType = ((isIdentifierType ? "docId" : "doc") + Character.toUpperCase(cleanupAttributeName.charAt(0)) + cleanupAttributeName.substring(1));
			ImAnnotation[] valueAnnots = doc.getAnnotations(valueAnnotType);
			this.logInMasterConfiguration(" - got " + valueAnnots.length + " existing " + valueAnnotType);
			
			//	remove spurious annotations
			for (int a = 0; a < valueAnnots.length; a++) {
				doc.removeAnnotation(valueAnnots[a]);
				wrappedDoc = null; // invalidate XML wrapper
			}
			this.logInMasterConfiguration("   ==> done");
		}
	}
	
	private void annotateAuthorDetailValues(RefData ref, String[] refAuthorNames, String[] refAttributeNames, String[] allAttributeNames, ImDocument doc, HashMap attributesToValues, boolean auto) {
		HashSet cleanupAttributeNames = new HashSet(Arrays.asList(allAttributeNames));
		ImDocumentRoot wrappedDoc = null;
		for (int an = 0; an < refAttributeNames.length; an++) {
			if (AuthorData.AUTHOR_NAME_ATTRIBUTE.equals(refAttributeNames[an]))
				continue;
			cleanupAttributeNames.remove(refAttributeNames[an]);
			String refAttributeName = (AUTHOR_ANNOTATION_TYPE + "-" + refAttributeNames[an]);
//			String[] refAttributeValues = new String[refAuthorNames.length];
			ArrayList refAttribValues = new ArrayList();
			for (int a = 0; a < refAuthorNames.length; a++) {
				String adv = ref.getAuthorAttribute(refAuthorNames[a], refAttributeNames[an]);
//				refAttributeValues[a] = ((adv == null) ? "" : adv);
				if (adv == null)
					continue;
				if (adv.indexOf(" & ") == -1)
					refAttribValues.add(adv);
				else refAttribValues.addAll(Arrays.asList(adv.split("\\s\\&\\s")));
			}
			String[] refAttributeValues = ((String[]) refAttribValues.toArray(new String[refAttribValues.size()]));
			this.logInMasterConfiguration(refAttributeName + ": " + Arrays.toString(refAttributeValues));
			String valueAnnotType = ("docAuthor" + Character.toUpperCase(refAttributeNames[an].charAt(0)) + refAttributeNames[an].substring(1));
			wrappedDoc = this.annotateAttributeValues(refAttributeName, refAttributeValues, !AuthorData.AUTHOR_AFFILIATION_ATTRIBUTE.equals(refAttributeNames[an]), valueAnnotType, attributesToValues, cleanupAttributeNames, doc, wrappedDoc);
		}
		
		//	nothing to clean up (we're strictly adding in automated mode)
		if (auto || cleanupAttributeNames.isEmpty())
			return;
		
		//	clean up any annotations of removed attributes
		for (Iterator canit = cleanupAttributeNames.iterator(); canit.hasNext();) {
			String cleanupAttributeName = ((String) canit.next());
			if (AuthorData.AUTHOR_NAME_ATTRIBUTE.equals(cleanupAttributeName))
				continue;
			this.logInMasterConfiguration("Cleaning up " + cleanupAttributeName + ":");
			
			//	get existing annotations
			String valueAnnotType = ("docAuthor" + Character.toUpperCase(cleanupAttributeName.charAt(0)) + cleanupAttributeName.substring(1));
			ImAnnotation[] valueAnnots = doc.getAnnotations(valueAnnotType);
			this.logInMasterConfiguration(" - got " + valueAnnots.length + " existing " + valueAnnotType);
			
			//	remove spurious annotations
			for (int a = 0; a < valueAnnots.length; a++) {
				doc.removeAnnotation(valueAnnots[a]);
				wrappedDoc = null; // invalidate XML wrapper
			}
			this.logInMasterConfiguration("   ==> done");
		}
	}
	
	private ImDocumentRoot annotateAttributeValues(String refAttributeName, String[] refAttributeValues, boolean isIdentifierType, String valueAnnotType, HashMap attributesToValues, HashSet cleanupAttributeNames, ImDocument doc, ImDocumentRoot wrappedDoc) {
		cleanupAttributeNames.remove(refAttributeName); // no cleanup required here
		this.logInMasterConfiguration("Annotating " + refAttributeName + ":");
		
		//	get existing annotations
		ArrayList valueAnnots = new ArrayList(Arrays.asList(doc.getAnnotations(valueAnnotType)));
		this.logInMasterConfiguration(" - got " + valueAnnots.size() + " existing " + valueAnnotType);
		
		//	get extracted values (creating working copy)
		ArrayList extractedValues = new ArrayList();
		if (attributesToValues.containsKey(refAttributeName))
			extractedValues.addAll((ArrayList) attributesToValues.get(refAttributeName));
		this.logInMasterConfiguration(" - got " + extractedValues.size() + " extracted values");
		
		//	process reference values
		ArrayList refValues = new ArrayList(Arrays.asList(refAttributeValues));
		this.logInMasterConfiguration(" - got " + refValues.size() + " values from reference");
		for (int v = 0; v < refValues.size(); v++) {
			String refValue = ((String) refValues.get(v));
			this.logInMasterConfiguration("   - handling " + refValue + ":");
			ImAnnotation refValueAnnot = null;
			
			//	if we have an existing annotation, we're all set
			for (int a = 0; a < valueAnnots.size(); a++) {
				ImAnnotation valueAnnot = ((ImAnnotation) valueAnnots.get(a));
				String annotValue = ImUtils.getString(valueAnnot.getFirstWord(), valueAnnot.getLastWord(), true);
				if (isIdentifierType)
					annotValue = annotValue.replaceAll("\\s", "");
				if (annotValue.equalsIgnoreCase(refValue)) {
					refValueAnnot = valueAnnot;
					valueAnnots.remove(a--); // no need to remove this one later on
					this.logInMasterConfiguration("     ==> found raw value matching annotation");
					break; // we're done here
				}
				String nAnnotValue = StringUtils.normalizeString(annotValue);
				if (nAnnotValue.equalsIgnoreCase(refValue)) {
					refValueAnnot = valueAnnot;
					valueAnnots.remove(a--); // no need to remove this one later on
					this.logInMasterConfiguration("     ==> found normalized value matching annotation");
					break; // we're done here
				}
				String sAnnotValue = sanitizeAttributeValue(refAttributeName, annotValue, false);
				if (sAnnotValue == null)
					sAnnotValue = sanitizeString(annotValue, (AUTHOR_ANNOTATION_TYPE.equals(refAttributeName) || EDITOR_ANNOTATION_TYPE.equals(refAttributeName)), false);
				if (sAnnotValue.equalsIgnoreCase(refValue)) {
					refValueAnnot = valueAnnot;
					valueAnnots.remove(a--); // no need to remove this one later on
					this.logInMasterConfiguration("     ==> found sanitized value matching annotation");
					break; // we're done here
				}
			}
			if (refValueAnnot != null) {
				refValues.remove(v--); // no need to go for this value any further
				wrappedDoc = null; // invalidate XML wrapper
				for (int e = 0; e < extractedValues.size(); e++) {
					ExtractedAttributeValue extractedValue = ((ExtractedAttributeValue) extractedValues.get(e));
					if (extractedValue.firstWord != refValueAnnot.getFirstWord())
						continue;
					if (extractedValue.lastWord != refValueAnnot.getLastWord())
						continue;
					extractedValues.remove(e--); // no need to hold on to this one if we already have the annotation
				}
				continue; // we're done with this one
			}
			this.logInMasterConfiguration("     --> matching annotation not found");
			
			//	if we have an extracted value, we can annotate from there
			for (int e = 0; e < extractedValues.size(); e++) {
				ExtractedAttributeValue extractedValue = ((ExtractedAttributeValue) extractedValues.get(e));
				if (extractedValue.rValue.equalsIgnoreCase(refValue)) {}
				else if (extractedValue.nValue.equalsIgnoreCase(refValue)) {}
				else if ((extractedValue.sValue != null) && extractedValue.sValue.equalsIgnoreCase(refValue)) {}
				else continue;
				refValueAnnot = doc.addAnnotation(extractedValue.firstWord, extractedValue.lastWord, valueAnnotType); // annotate
				this.logInMasterConfiguration("     ==> annotated matching extracted value as " + valueAnnotType);
				extractedValues.remove(e--); // no need to hold on to this one if we already have the annotation
				break; // we're done here
			}
			if (refValueAnnot != null) {
				refValues.remove(v--); // no need to go for this value any further
				wrappedDoc = null; // invalidate XML wrapper
				continue; // we're done with this one
			}
			this.logInMasterConfiguration("     --> matching extracted value not found");
		}
		
		//	remove spurious annotations
		for (int n = 0; n < valueAnnots.size(); n++) {
			doc.removeAnnotation((ImAnnotation) valueAnnots.get(n));
			wrappedDoc = null; // invalidate XML wrapper
		}
		this.logInMasterConfiguration(" - cleaned up " + valueAnnots.size() + " spurious " + valueAnnotType);
		
		//	anything left to annotate?
		if (refValues.isEmpty())
			return wrappedDoc; // we're done with this attribute
		this.logInMasterConfiguration(" - annotating " + refValues.size() + " remaining values from reference");
		
		//	(re)create wrapper only on demand
		if (wrappedDoc == null)
			wrappedDoc = new ImDocumentRoot(doc.getPage(doc.getFirstPageId()), (ImDocumentRoot.NORMALIZATION_LEVEL_WORDS | ImDocumentRoot.NORMALIZE_CHARACTERS));
		
		//	annotate any remaining values
		this.annotateAttributeValues(refAttributeName, refValues, valueAnnotType, wrappedDoc);
		return wrappedDoc;
	}
	
	private void annotateAttributeValues(String attributeName, ArrayList values, String valueAnnotType, ImDocumentRoot wrappedDoc) {
		
		//	create attribute value dictionary
		StringVector valueDict = new StringVector(false);
		for (int v = 0; v < values.size(); v++) {
			String value = StringUtils.normalizeString((String) values.get(v));
			if (PAGINATION_ANNOTATION_TYPE.equals(attributeName))
				valueDict.addElementIgnoreDuplicates(value.replaceAll("\\-", " - "));
			else {
				valueDict.addElementIgnoreDuplicates(value);
				if (AUTHOR_ANNOTATION_TYPE.equals(attributeName) || EDITOR_ANNOTATION_TYPE.equals(attributeName)) {
					int commaPos = value.indexOf(",");
					if (commaPos != -1)
						valueDict.addElementIgnoreDuplicates(value.substring(commaPos + ",".length()).trim() + " " + value.substring(0, commaPos).trim());
				}
			}
		}
		this.logInMasterConfiguration("Annotating " + values.size() + " " + attributeName + " as " + valueAnnotType);
		for (int v = 0; v < valueDict.size(); v++) {
			String value = valueDict.get(v);
			TokenSequence valueTokens = Gamta.newTokenSequence(value, wrappedDoc.getTokenizer());
			valueDict.addElementIgnoreDuplicates(TokenSequenceUtils.concatTokens(valueTokens, true, true));
		}
		this.logInMasterConfiguration(" - normalized to " + valueDict.size() + " values");
		
		//	annotate attribute values (each one only once, though)
		Annotation[] valueAnnots = Gamta.extractAllContained(wrappedDoc, valueDict, true, false, true);
		TreeSet annotatedValues = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		for (int a = 0; a < valueAnnots.length; a++) 
			if (annotatedValues.add(valueAnnots[a].getValue())) {
				valueAnnots[a].changeTypeTo(valueAnnotType);
				wrappedDoc.addAnnotation(valueAnnots[a]);
				this.logInMasterConfiguration(" - annotated " + valueAnnots[a].getValue() + " as " + valueAnnotType);
			}
	}
	
	private RefData editRefData(final ImDocument doc, RefData ref, final DocumentStyle metaDataStyle, final HashMap attributesToValues) {
		final String docName = ((String) doc.getAttribute(DOCUMENT_NAME_ATTRIBUTE));
		final JDialog refEditorDialog = DialogFactory.produceDialog(("Get Metadata for Document" + ((docName == null) ? "" : (" " + docName))), true);
		String refType = refTypeSystem.classify(ref);
		if (refType == null)
			refType = refTypeDefault;
		if (refType != null)
			ref.setAttribute(PUBLICATION_TYPE_ATTRIBUTE, refType);
		final BibRefEditorPanel refEditorPanel = new BibRefEditorPanel(this.refTypeSystem, this.refIdTypes, this.authorDetails, ref);
		final boolean[] cancelled = {false};
		
		JButton extract = new JButton("Extract");
		extract.setToolTipText("Extract metadata from document");
		extract.addActionListener(new ActionListener() {
			ImDocumentRoot wrappedDoc = null;
			public void actionPerformed(ActionEvent ae) {
				if (this.wrappedDoc == null)
					for (int p = 0; p < doc.getPageCount(); p++) {
						ImPage page = doc.getPage(doc.getFirstPageId() + p);
						ImWord[] pageWords = page.getWords();
						if (pageWords.length == 0)
							continue;
						this.wrappedDoc = new ImDocumentRoot(doc.getPage(doc.getFirstPageId() + p), ImDocumentRoot.NORMALIZATION_LEVEL_WORDS);
						break;
					}
				RefData ref = refEditorPanel.getRefData();
				if (fillFromDocument(refEditorDialog, docName, this.wrappedDoc, ref, attributesToValues)) {
					if (doc.hasAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE) && (doc.getAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE) instanceof String) && !ref.hasAttribute(PUBLICATION_URL_ANNOTATION_TYPE))
						ref.setAttribute(PUBLICATION_URL_ANNOTATION_TYPE, ((String) doc.getAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE)));
					refEditorPanel.setRefData(ref);
				}
			}
		});
		
		JButton search = null;
		checkRefDataSources();
		if (this.refDataSources != null) {
			search = new JButton("Search");
			search.setToolTipText("Search RefBank for metadata, using current input as query");
			search.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					RefData queryRef = refEditorPanel.getRefData();
					RefData resultRef = searchRefData(refEditorDialog, queryRef, docName);
					if (resultRef == null)
						return;
					if (doc.hasAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE) && (doc.getAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE) instanceof String) && !resultRef.hasAttribute(PUBLICATION_URL_ANNOTATION_TYPE))
						resultRef.setAttribute(PUBLICATION_URL_ANNOTATION_TYPE, ((String) doc.getAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE)));
					boolean preferQueryValues;
					if (resultRef.removeAttribute("selectedDetails"))
						preferQueryValues = false; // attributes hand picked by user
					else if (refEditorPanel.getErrors() == null)
						preferQueryValues = true; // this one looks too complete to overwrite
					else if ((metaDataStyle == null) || (metaDataStyle.getPropertyNames().length == 0))
						preferQueryValues = false; // no document style to extract sensible values
					else preferQueryValues = true; // hold on to values extracted by template
					transferSearchResultAttributes(resultRef, queryRef, (preferQueryValues ? preferTransferTarget : preferTransferSource));
					refEditorPanel.setRefData(queryRef);
				}
			});
		}
		
		JButton validate = new JButton("Validate");
		validate.setToolTipText("Check if the metadata is complete");
		validate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String[] errors = refEditorPanel.getErrors();
				if (errors == null)
					DialogFactory.alert("The document metadata is valid.", "Validation Report", JOptionPane.INFORMATION_MESSAGE);
				else displayErrors(errors, refEditorPanel);
			}
		});
		
		JButton ok = new JButton("OK");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String[] errors = refEditorPanel.getErrors();
				if (errors == null)
					refEditorDialog.dispose();
				else displayErrors(errors, refEditorPanel);
			}
		});
		
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				cancelled[0] = true;
				refEditorDialog.dispose();
			}
		});
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
		buttonPanel.add(extract);
		if (search != null)
			buttonPanel.add(search);
		buttonPanel.add(validate);
		buttonPanel.add(ok);
		buttonPanel.add(cancel);
		
		refEditorDialog.getContentPane().setLayout(new BorderLayout());
		refEditorDialog.getContentPane().add(refEditorPanel, BorderLayout.CENTER);
		refEditorDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		refEditorDialog.setSize(600, 600);
		refEditorDialog.setLocationRelativeTo(DialogFactory.getTopWindow());
		refEditorDialog.setVisible(true);
		
		return (cancelled[0] ? null : refEditorPanel.getRefData());
	}
	
	private boolean fillFromDocument(JDialog dialog, String docName, ImDocumentRoot doc, RefData ref, HashMap attributesToValues) {
		DocumentView docView = new DocumentView(dialog, doc, ref, attributesToValues);
		docView.setSize(900, 500);
		docView.setLocationRelativeTo(dialog);
		docView.setVisible(true);
		return docView.refModified;
	}
	
	private class DocumentView extends JDialog {
		private ImDocumentRoot doc;
		private int displayLength = 200;
		private RefData ref;
		private HashMap attributesToValues;
		private JTextArea textArea = new JTextArea();
		boolean refModified = false;
		DocumentView(JDialog owner, ImDocumentRoot doc, RefData rd, HashMap attributesToValues) {
			super(owner, "Document View", true);
			this.doc = doc;
			this.ref = rd;
			this.attributesToValues = attributesToValues;
			
			this.textArea.setFont(new Font("Verdana", Font.PLAIN, 12));
			this.textArea.setLineWrap(true);
			this.textArea.setWrapStyleWord(true);
			this.textArea.setText(TokenSequenceUtils.concatTokens(this.doc, 0, Math.min(this.doc.size(), this.displayLength), false, false));
			JScrollPane xmlAreaBox = new JScrollPane(this.textArea);
			
			JPanel extractButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
			final JButton more = new JButton("More ...");
			more.setToolTipText("Show more document text");
			more.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					DocumentView.this.displayLength += 100;
					DocumentView.this.textArea.setText(TokenSequenceUtils.concatTokens(DocumentView.this.doc, 0, Math.min(DocumentView.this.doc.size(), DocumentView.this.displayLength), false, false));
					if (DocumentView.this.doc.size() < DocumentView.this.displayLength)
						more.setEnabled(false);
				}
			});
			extractButtonPanel.add(more);
			final ExtractButton[] extractButtons = new ExtractButton[extractableFieldNames.length];
			for (int f = 0; f < extractableFieldNames.length; f++) {
				extractButtons[f] = new ExtractButton(extractableFieldNames[f]);
				extractButtonPanel.add(extractButtons[f]);
			}
			for (int t = 0; t < refIdTypes.length; t++) {
				if (!"".equals(refIdTypes[t].trim()))
					extractButtonPanel.add(new ExtractButton("ID-" + refIdTypes[t]));
			}
			
			//	add reference type selector to this panel
			BibRefType[] brts = refTypeSystem.getBibRefTypes();
			SelectableBibRefType[] sbrts = new SelectableBibRefType[brts.length];
			for (int t = 0; t < brts.length; t++)
				sbrts[t] = new SelectableBibRefType(brts[t]);
			final JComboBox typeSelector = new JComboBox(sbrts);
			
			//	adjust extractor buttons when reference type changes
			typeSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					if (ie.getStateChange() != ItemEvent.SELECTED)
						return;
					for (int f = 0; f < extractButtons.length; f++)
						extractButtons[f].setBibRefType(((SelectableBibRefType) ie.getItem()).brt);
				}
			});
			
			//	initialize buttons
			String type = this.ref.getAttribute(PUBLICATION_TYPE_ATTRIBUTE);
			if (type == null)
				type = refTypeSystem.classify(this.ref);
			BibRefType brt = refTypeSystem.getBibRefType(type);
			if ((brt == null) && (brts.length != 0))
				brt = brts[0];
			if (brt != null) {
				typeSelector.setSelectedItem(new SelectableBibRefType(brt));
				for (int f = 0; f < extractButtons.length; f++)
					extractButtons[f].setBibRefType(brt);
			}
			
			//	TODO_not add button 'Reference' to select whole reference at once, run it through RefParse, and then use all attributes found
			//	WOULD DRAG THIS WHOLE THING UNDER GPL ...
			
			JPanel functionPanel = new JPanel(new BorderLayout(), true);
			functionPanel.add(new JLabel("Publication Type: ", JLabel.RIGHT), BorderLayout.WEST);
			functionPanel.add(typeSelector, BorderLayout.CENTER);
			functionPanel.add(extractButtonPanel, BorderLayout.SOUTH);
			
			JButton closeButton = new JButton("Close");
			closeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (refModified)
						ref.setAttribute(PUBLICATION_TYPE_ATTRIBUTE, ((SelectableBibRefType) typeSelector.getSelectedItem()).brt.name);
					dispose();
				}
			});
			
			this.setLayout(new BorderLayout());
			this.add(functionPanel, BorderLayout.NORTH);
			this.add(xmlAreaBox, BorderLayout.CENTER);
			this.add(closeButton, BorderLayout.SOUTH);
		}
		
		private class ExtractButton extends JButton implements ActionListener {
			String attribute;
			ExtractButton(String attribute) {
				this.setToolTipText("Use selected string as " + attribute);
				this.attribute = attribute;
				if (attribute.startsWith("ID-")) {
					attribute = attribute.substring("ID-".length());
					if ("DOI Handle ISBN ISSN".indexOf(attribute) == -1)
						this.setText(attribute + " ID");
					else this.setText(attribute);
				}
				else if (PUBLICATION_URL_ANNOTATION_TYPE.equals(attribute))
					this.setText("URL");
				else {
					StringBuffer label = new StringBuffer();
					for (int c = 0; c < this.attribute.length(); c++) {
						char ch = this.attribute.charAt(c);
						if (c == 0)
							label.append(Character.toUpperCase(ch));
						else {
							if (Character.isUpperCase(ch))
								label.append(' ');
							label.append(ch);
						}
					}
					this.setText(label.toString());
				}
				if (ref.getAttribute(this.attribute) == null) {
					this.setToolTipText("Click to extract " + this.getText());
					this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createLineBorder(this.attribute.startsWith("ID-") ? this.getBackground() : Color.RED)));
				}
				else {
					this.setToolTipText(this.getText() + ": " + ref.getAttributeValueString(this.attribute, " & "));
					this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createLineBorder(this.getBackground())));
				}
				this.addActionListener(this);
			}
			public void actionPerformed(ActionEvent ae) {
				
				//	get raw selection
				int valueStart = Math.min(textArea.getCaret().getDot(), textArea.getCaret().getMark());
				int valueEnd = Math.max(textArea.getCaret().getDot(), textArea.getCaret().getMark());
				String value = doc.subSequence(valueStart, valueEnd).toString().trim();
				if (value.length() == 0)
					return;
				
				//	get underlying words
				ImWord startWord = doc.wordAtOffset(valueStart);
				while ((startWord == null) && (valueStart < valueEnd)) {
					valueStart++;
					startWord = doc.wordAtOffset(valueStart);
				}
				ImWord endWord = doc.wordAtOffset(valueEnd-1);
				while ((endWord == null) && (valueStart < valueEnd)) {
					valueEnd--;
					endWord = doc.wordAtOffset(valueEnd-1);
				}
				if ((startWord == null) || (endWord == null))
					return;
				
				//	index extracted match
				ArrayList attributeValues = ((ArrayList) attributesToValues.get(this.attribute));
				if (attributeValues == null) {
					attributeValues = new ArrayList();
					attributesToValues.put(this.attribute, attributeValues);
				}
				
				//	sanitize attribute value, and confirm with user if invalid
				String sValue = sanitizeAttributeValue(this.attribute, value, this.attribute.startsWith("ID-"));
				if (sValue == null) {
					int choice = DialogFactory.confirm(("'" + value + "' is not a valid value for document " + this.attribute + ". Use anyway?"), ("Invalid Document " + this.getText()), JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
					if (choice != JOptionPane.YES_OPTION)
						return;
					sValue = sanitizeString(value, (AUTHOR_ANNOTATION_TYPE.equals(this.attribute) || EDITOR_ANNOTATION_TYPE.equals(this.attribute)), this.attribute.startsWith("ID-"));
					if (AUTHOR_ANNOTATION_TYPE.equals(this.attribute) || EDITOR_ANNOTATION_TYPE.equals(this.attribute))
						sValue = flipNameParts(sValue, value);
				}
				
				//	store attribute value and keep track of words
				if (ref.hasAttribute(this.attribute) && (AUTHOR_ANNOTATION_TYPE.equals(this.attribute) || EDITOR_ANNOTATION_TYPE.equals(this.attribute))) {
					LinkedHashSet sValues = new LinkedHashSet(Arrays.asList(ref.getAttributeValues(this.attribute)));
					if (startWord.getTextStreamId().equals(endWord.getTextStreamId())) {
						ExtractedAttributeValue eav = new ExtractedAttributeValue(this.attribute, value, StringUtils.normalizeString(value), sValue, getSpannedWords(startWord, endWord));
						
						//	remove conflicting values from existing ones
						for (int v = 0; v < attributeValues.size(); v++) {
							ExtractedAttributeValue cEav = ((ExtractedAttributeValue) attributeValues.get(v));
							if (cEav.conflictsWith(eav)) {
								sValues.remove(cEav.rValue);
								sValues.remove(cEav.nValue);
								sValues.remove(cEav.sValue);
								attributeValues.remove(v--);
							}
						}
						
						//	add new extracted value, and ensure sort order
						attributeValues.add(eav);
						Collections.sort(attributeValues);
						
						//	add extracted values first (we know the sort order for those)
						for (int v = 0; v < attributeValues.size(); v++) {
							eav = ((ExtractedAttributeValue) attributeValues.get(v));
							if (v == 0)
								ref.setAttribute(this.attribute, eav.sValue);
							else ref.addAttribute(this.attribute, eav.sValue);
							sValues.remove(eav.rValue);
							sValues.remove(eav.nValue);
							sValues.remove(eav.sValue);
						}
						
						//	add any remaining values that existed before (might have been entered manually)
						for (Iterator svit = sValues.iterator(); svit.hasNext();)
							ref.addAttribute(this.attribute, ((String) svit.next()));
					}
					else if (sValues.add(sValue))
						ref.addAttribute(this.attribute, sValue);
				}
				else {
					ref.setAttribute(this.attribute, sValue);
					attributeValues.clear();
					if (startWord.getTextStreamId().equals(endWord.getTextStreamId()))
						attributeValues.add(new ExtractedAttributeValue(this.attribute, value, StringUtils.normalizeString(value), sValue, getSpannedWords(startWord, endWord)));
				}
				refModified = true;
				
				//	visualize changes
				this.setToolTipText(this.getText() + ": " + ref.getAttributeValueString(this.attribute, " & "));
				this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createLineBorder(Color.GREEN)));
			}
			void setBibRefType(BibRefType brt) {
				if (this.attribute.startsWith("ID-")) {
					if (ref.hasAttribute(this.attribute))
						this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createLineBorder(Color.GREEN)));
					else this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createLineBorder(this.getBackground())));
					this.setEnabled(true);
				}
				else if (brt.requiresAttribute(this.attribute)) {
					if (ref.hasAttribute(this.attribute))
						this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createLineBorder(Color.GREEN)));
					else this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createLineBorder(Color.RED)));
					this.setEnabled(true);
				}
				else if (brt.canHaveAttribute(this.attribute)) {
					if (ref.hasAttribute(this.attribute))
						this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createLineBorder(Color.GREEN)));
					else this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createLineBorder(this.getBackground())));
					this.setEnabled(true);
				}
				else {
					this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createLineBorder(this.getBackground())));
					this.setEnabled(false);
				}
			}
		}
		
		private class SelectableBibRefType {
			private BibRefType brt;
			SelectableBibRefType(BibRefType brt) {
				this.brt = brt;
			}
			public String toString() {
				return this.brt.getLabel();
			}
			public boolean equals(Object obj) {
				return ((obj instanceof SelectableBibRefType) && ((SelectableBibRefType) obj).brt.name.equals(this.brt.name));
			}
			public int hashCode() {
				return this.brt.name.hashCode();
			}
		}
	}
	
	private final void displayErrors(String[] errors, JPanel parent) {
		StringVector errorMessageBuilder = new StringVector();
		errorMessageBuilder.addContent(errors);
		DialogFactory.alert(("The document metadata is not valid. In particular, there are the following errors:\n" + errorMessageBuilder.concatStrings("\n")), "Validation Report", JOptionPane.ERROR_MESSAGE);
	}
	
	private RefData searchRefData(JDialog dialog, RefData query, String docName) {
		
		//	perform search
		Vector refs = this.searchRefData(query);
		if (refs == null) {
			if (dialog != null)
				DialogFactory.alert("Please enter some data to search for.\nYou can also use 'View Doc' to copy some data from the document text.", "Cannot Search Document Metadata", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		
		//	did we find anything?
		if (refs.isEmpty()) {
			if (dialog != null)
				DialogFactory.alert("Your search did not return any results.\nYou can still enter the document metadata manually.", "Document Metadata Not Fount", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		
		//	not allowed to show list dialog, return data only if we have an unambiguous match
		if (dialog == null)
			return ((refs.size() == 1) ? ((RefDataListElement) refs.get(0)).ref : null);
		
		//	display whatever is found in list dialog
		RefDataList refDataList = new RefDataList(dialog, ("Select Metadata for Document" + ((docName == null) ? "" : (" " + docName))), refs, query);
		refDataList.setVisible(true);
		return refDataList.ref;
	}
	
	Vector searchRefData(RefData query) {
		
		//	can we search?
		this.checkRefDataSources();
		if (this.refDataSources == null)
			return null;
		
		//	get search data
		String author = query.getAttribute(AUTHOR_ANNOTATION_TYPE);
		String title = query.getAttribute(TITLE_ANNOTATION_TYPE);
		String origin = this.refTypeSystem.getOrigin(query);
		String year = query.getAttribute(YEAR_ANNOTATION_TYPE);
		
		//	test year
		if (year != null) try {
			Integer.parseInt(year);
		}
		catch (NumberFormatException nfe) {
			year = null;
		}
		
		//	get identifiers (preferring DOI if present)
		String extIdType = null;
		String extId = null;
		if (query.getIdentifier("DOI") != null) {
			extIdType = "DOI";
			extId = query.getIdentifier("DOI");
		}
		else {
			String[] extIdTypes = query.getIdentifierTypes();
			for (int t = 0; t < extIdTypes.length; t++) {
				if (!this.searchRefIdTypes.contains(extIdTypes[t]))
					continue;
				extId = query.getIdentifier(extIdTypes[t]);
				if (extId == null)
					continue;
				extIdType = extIdTypes[t];
				break;
			}
		}
		
		//	got something to search for?
		if ((extId == null) && (author == null) && (title == null) && (year == null) && (origin == null))
			return null;
		
		//	perform search
		Vector refs = new Vector();
		Properties searchData = new Properties();
		if ((extId != null) && (extIdType != null))
			searchData.setProperty(("ID-" + extIdType), extId);
		else {
			if (author != null) {
				if (author.indexOf(',') != -1)
					author = author.substring(0, author.indexOf(','));
				searchData.setProperty(AUTHOR_ANNOTATION_TYPE, author);
			}
			if (year != null)
				searchData.setProperty(YEAR_ANNOTATION_TYPE, year);
			if (title != null)
				searchData.setProperty(TITLE_ANNOTATION_TYPE, title);
			if (origin != null)
				searchData.setProperty(JOURNAL_NAME_OR_PUBLISHER_ANNOTATION_TYPE, origin);
		}
		this.logInMasterConfiguration("Searching " + searchData);
		for (int s = 0; s < this.refDataSources.length; s++) try {
			RefData[] sRefs = this.refDataSources[s].findRefData(searchData);
			if (sRefs == null)
				continue;
			for (int r = 0; r < sRefs.length; r++) {
				if (sRefs[r] != null)
					refs.add(new RefDataListElement(sRefs[r]));
			}
		} catch (IOException ioe) { /* let's not bother with exceptions for now, just ignore them ... */ }
		
		//	finally ...
		return refs;
	}
	
	private static class RefDataList extends JDialog {
		private JList refList;
		RefData ref = new RefData();
		RefDataList(JDialog owner, String title, final Vector refData, final RefData queryData) {
			super(owner, title, true);
			
			this.refList = new JList(refData);
			this.refList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			JScrollPane refListBox = new JScrollPane(this.refList);
			refListBox.getVerticalScrollBar().setUnitIncrement(50);
			
			JButton ok = new JButton("OK");
			ok.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					int sri = refList.getSelectedIndex();
					if (sri == -1)
						return;
					ref = ((RefDataListElement) refData.get(sri)).ref;
					dispose();
				}
			});
			JButton details = new JButton("Details");
			details.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					int sri = refList.getSelectedIndex();
					if (sri == -1)
						return;
					RefData dRef = selectRefDetails(((RefDataListElement) refData.get(sri)).ref, queryData);
					if (dRef == null)
						return;
					ref = dRef;
					dispose();
				}
			});
			JButton cancel = new JButton("Cancel");
			cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					dispose();
				}
			});
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
			buttonPanel.add(ok);
			buttonPanel.add(details);
			buttonPanel.add(cancel);
			
			this.getContentPane().setLayout(new BorderLayout());
			this.getContentPane().add(refListBox, BorderLayout.CENTER);
			this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
			this.setSize(600, 370);
			this.setLocationRelativeTo(owner);
		}
	}
	
	static RefData selectRefDetails(RefData ref, RefData query) {
		JPanel sdp = new JPanel(new GridLayout(0, 1), true);
		ArrayList useAttributeList = new ArrayList();
		for (int n = 0; n < lookupTransferAttributeNames.length; n++)
			if (ref.hasAttribute(lookupTransferAttributeNames[n])) {
				UseRefDetailCheckbox useAttribute = new UseRefDetailCheckbox(lookupTransferAttributeNames[n], false, ref.getAttributeValueString(lookupTransferAttributeNames[n], " & "), query.getAttributeValueString(lookupTransferAttributeNames[n], " & "));
				useAttributeList.add(useAttribute);
				sdp.add(useAttribute);
			}
		UseRefDetailCheckbox[] useAttribute = ((UseRefDetailCheckbox[]) useAttributeList.toArray(new UseRefDetailCheckbox[useAttributeList.size()]));
		String[] identifierTypes = ref.getIdentifierTypes();
		UseRefDetailCheckbox[] useIdentifier = new UseRefDetailCheckbox[identifierTypes.length];
		for (int t = 0; t < identifierTypes.length; t++) {
			useIdentifier[t] = new UseRefDetailCheckbox(identifierTypes[t], true, ref.getIdentifier(identifierTypes[t]), query.getIdentifier(identifierTypes[t]));
			sdp.add(useIdentifier[t]);
		}
		int choice = DialogFactory.confirm(sdp, "Select Reference Details To Use", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (choice != JOptionPane.OK_OPTION)
			return null;
		
		RefData dRef = new RefData();
		dRef.setAttribute("selectedDetails", "true");
		for (int a = 0; a < useAttribute.length; a++)
			if (useAttribute[a].isSelected()) {
				dRef.setAttribute(useAttribute[a].name, ref.getAttribute(useAttribute[a].name));
				if (AUTHOR_ANNOTATION_TYPE.equals(useAttribute[a].name) || EDITOR_ANNOTATION_TYPE.equals(useAttribute[a].name)) {
					String[] values = ref.getAttributeValues(useAttribute[a].name);
					for (int v = 1; v < values.length; v++)
						dRef.addAttribute(useAttribute[a].name, values[v]);
				}
			}
		for (int i = 0; i < useIdentifier.length; i++) {
			if (useIdentifier[i].isSelected())
				dRef.setIdentifier(useIdentifier[i].name, ref.getIdentifier(useIdentifier[i].name));
		}
		return dRef;
	}
	
	private static class UseRefDetailCheckbox extends JCheckBox {
		final String name;
		UseRefDetailCheckbox(String name, boolean isIdentifier, String resValue, String queryValue) {
			super("", (queryValue == null));
			this.name = name;
			StringBuffer text = new StringBuffer("<HTML><B>");
			if (isIdentifier) {
				text.append(this.name);
				text.append(" Identifier");
			}
			else for (int c = 0; c < this.name.length(); c++) {
				char ch = this.name.charAt(c);
				if (c == 0)
					ch = Character.toUpperCase(ch);
				else if (Character.isUpperCase(ch))
					text.append(' ');
				text.append(ch);
			}
			text.append(":</B> ");
			text.append(AnnotationUtils.escapeForXml(resValue));
			if (queryValue == null) {}
			else if (resValue.equals(queryValue))
				text.append(" <B>(identical to existing)</B>");
			else {
				text.append(" <B>(replacing</B> ");
				text.append(AnnotationUtils.escapeForXml(queryValue));
				text.append("<B>)</B>");
			}
			text.append("</HTML>");
			this.setText(text.toString());
		}
	}
	
	private static class RefDataListElement {
		final RefData ref;
		final String displayString;
		final String sourceString;
		RefDataListElement(RefData ref) {
			this.ref = ref;
			this.displayString = BibRefUtils.toRefString(this.ref);
			this.sourceString = ref.getAttribute(BibRefDataSource.REFERENCE_DATA_SOURCE_ATTRIBUTE);
		}
		public String toString() {
			return (((this.sourceString == null) ? "" : ("[" + this.sourceString + "]: ")) + this.displayString);
		}
	}
	
	private static final String sanitizeString(String str, boolean isPersonName, boolean isIdentifier) {
		
		//	check all-caps (part-wise for person names), and also for vowels
		boolean[] inAllCaps = new boolean[str.length()];
		boolean noSpace = true;
		boolean hasVowel = false;
		if (isPersonName) {
			noSpace = false;
			hasVowel = true;
			for (int c = 0; c < str.length(); c++) {
				char ch = str.charAt(c);
				if (!Character.isLetter(ch)) {
					inAllCaps[c] = false;
					continue;
				}
				int tec;
				boolean tAllCaps = true;
				for (tec = c; tec < str.length(); tec++) {
					char tch = str.charAt(tec);
					if (Character.isLetter(tch)) {
						if (tch != Character.toUpperCase(tch))
							tAllCaps = false;
					}
					else break;
				}
				Arrays.fill(inAllCaps, c, tec, tAllCaps);
				c = tec; // jump to end of token
				c--; // compensate loop increment
			}
		}
		else {
			boolean allCaps = true;
			for (int c = 0; c < str.length(); c++) {
				char ch = str.charAt(c);
				if (ch < 33)
					noSpace = false;
				else if (Character.isLetter(ch)) {
					if ("aeiouyAEIOUY".indexOf(StringUtils.getBaseChar(ch)) != -1)
						hasVowel = true;
					if (ch != Character.toUpperCase(ch)) {
						allCaps = false;
						break;
					}
				}
			}
			Arrays.fill(inAllCaps, allCaps);
		}
		
		//	normalize characters
		StringBuffer sStr = new StringBuffer();
		char lCh = ' ';
		for (int c = 0; c < str.length(); c++) {
			char ch = str.charAt(c);
			if (ch < 33) {
				if (lCh > 32)
					sStr.append(' ');
			}
			else if (Character.isLetter(ch)) {
				if (isIdentifier) // no case normalization on identifiers
					sStr.append(ch);
				else if (noSpace && (str.length() <= (hasVowel ? 4 : 5))) // no case normalization on acronyms
					sStr.append(ch);
				else if (inAllCaps[c] && Character.isLetter(lCh)) // convert all-caps to title case
					sStr.append(Character.toLowerCase(ch));
				else sStr.append(ch);
			}
			else if (StringUtils.DASHES.indexOf(ch) != -1) // normalize dashes
				sStr.append('-');
			else sStr.append(ch);
			lCh = ch;
		}
		
		//	finally
		return sStr.toString();
	}
	
	private static final String flipNameParts(String name, String oName) {
		name = name.trim();
		oName = oName.trim();
		
		//	this one's in the appropriate order
		if (name.indexOf(',') != -1)
			return name;
		
		//	find possible splits
		int fSplit = name.indexOf(' ');
		int lSplit = name.lastIndexOf(' ');
		if ((fSplit == -1) || (lSplit == -1))
			return name;
		
		//	this one's unambiguous
		if (fSplit == lSplit) {
			String firstName = name.substring(0, lSplit).trim();
			String lastName = name.substring(lSplit + 1).trim();
			return (lastName + ", " + firstName);
		}
		
		//	try to split after (last) middle initial
		int lInitialEnd = name.lastIndexOf(". ");
		if ((lInitialEnd > fSplit) && (lInitialEnd < lSplit)) {
			int split = (name.lastIndexOf(". ") + ".".length());
			String firstName = name.substring(0, split).trim();
			String lastName = name.substring(split + 1).trim();
			return (lastName + ", " + firstName);
		}
		
		/* TODO make this less speculative:
		 * - if we have a lastname prefix like 'de', 'van', 'von', split before the first one
		 *   ==> consider Spanish names, though, which might have a prefix only before the second lastname !!!
		 * - if we have partial all-caps, use it to identify lastnames (be careful about initials blocks, though)
		 * 
		 * - THINK OF FURTHER CRITERIA
		 */
		
		//	TODO use ProperNameUtils here, and get parse from attributes
		//	TODO add author name style parameter to template to help resolve ambiguous cases
		
		//	TODO when annotating author names, also consider token ring match (we'll soon need these annotations to add affiliations to them ...)
		
		//	split at spaces as a general fallback
		String firstName = name.substring(0, lSplit).trim();
		String lastName = name.substring(lSplit + 1).trim();
		return (lastName + ", " + firstName);
	}
	
	//	!!! TEST ONLY !!!
	public static void main(String[] args) throws Exception {
//		if (true) {
//			System.out.println(parseCharList("\\u0043 \\x61 t", System.out));
//			System.out.println(parseCharList("\\u004G \\x61 t", System.out));
//			System.out.println(parseCharList("\\u0043\\x61t", System.out));
//			return;
//		}
//		
//		InputStream docIn = new BufferedInputStream(new FileInputStream(new File("E:/Testdaten/PdfExtract/zt00109.o.pdf.imf")));
		InputStream docIn = new BufferedInputStream(new FileInputStream(new File("E:/Testdaten/PdfExtract/zt00619.o.pdf.imf")));
//		InputStream docIn = new BufferedInputStream(new FileInputStream(new File("E:/Testdaten/PdfExtract/zt00872.pdf.imf")));
//		InputStream docIn = new BufferedInputStream(new FileInputStream(new File("E:/Testdaten/PdfExtract/zt00904.pdf.imf")));
//		InputStream docIn = new BufferedInputStream(new FileInputStream(new File("E:/Testdaten/PdfExtract/zt01826p058.pdf.imf")));
//		InputStream docIn = new BufferedInputStream(new FileInputStream(new File("E:/Testdaten/PdfExtract/zt03456p035.pdf.imf")));
		ImDocument doc = ImDocumentIO.loadDocument(docIn);
		docIn.close();
		doc.clearAttributes();
		
		DocumentMetaDataEditorProvider dmdep = new DocumentMetaDataEditorProvider();
		dmdep.setDataProvider(new PluginDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/DocumentMetaDataEditorData/")));
		dmdep.init();
		
		BufferedReader dsBr = new BufferedReader(new InputStreamReader(new FileInputStream(new File("E:/GoldenGATEv3/Plugins/DocumentStyleManagerData/zootaxa.0000.journal_article.docStyle")), "UTF-8"));
		DocumentStyle.Data dsData = DocumentStyle.readDocumentStyleData(dsBr);
		dsBr.close();
		final DocumentStyle docStyle = new ImDocumentStyle(dsData);
		DocumentStyle.addProvider(new DocumentStyle.Provider() {
			public DocumentStyle getStyleFor(Attributed doc) {
//				Settings ds = Settings.loadSettings(new File("E:/GoldenGATEv3/Plugins/DocumentStyleManagerData/zootaxa.2007.journal_article.docStyle"));
//				Settings ds = Settings.loadSettings(new File("E:/GoldenGATEv3/Plugins/DocumentStyleManagerData/zootaxa.0000.journal_article.docStyle"));
//				return new SettingsDocumentStyle(ds);
				return docStyle;
			}
			public void documentStyleAssigned(DocumentStyle docStyle, Attributed doc) {}
		});
		
		dmdep.metaDataAdder.process(doc, null, null, ProgressMonitor.dummy);
		String[] params = DocumentStyle.getParameterNames();
		Arrays.sort(params);
		for (int p = 0; p < params.length; p++)
			System.out.println(params[p] + " = \"" + DocumentStyle.getParameterValueClass(params[p]).getName() + "\";");
	}
}
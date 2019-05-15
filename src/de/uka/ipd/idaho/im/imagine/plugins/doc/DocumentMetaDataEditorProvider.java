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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
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
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.BorderFactory;
import javax.swing.JButton;
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

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.BufferedLineWriter;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.DocumentStyle;
import de.uka.ipd.idaho.gamta.util.imaging.DocumentStyle.ParameterGroupDescription;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.goldenGate.plugins.PluginDataProviderFileBased;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder.HtmlPageBuilderHost;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.gamta.ImDocumentRoot;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.DisplayExtensionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider;
import de.uka.ipd.idaho.im.imagine.web.plugins.WebDocumentViewer;
import de.uka.ipd.idaho.im.util.ImDocumentIO;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.DisplayExtension;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.DisplayExtensionGraphics;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefConstants;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefEditorFormHandler;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefEditorPanel;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefTypeSystem;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefTypeSystem.BibRefType;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils.RefData;
import de.uka.ipd.idaho.plugins.bibRefs.refBank.RefBankClient;
import de.uka.ipd.idaho.plugins.bibRefs.refBank.RefBankClient.BibRef;
import de.uka.ipd.idaho.plugins.bibRefs.refBank.RefBankClient.BibRefIterator;
import de.uka.ipd.idaho.plugins.dateTime.DateTimeUtils;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * This plugin provides an editor for the bibliographic metadata of a document.
 * 
 * @author sautter
 */
public class DocumentMetaDataEditorProvider extends AbstractImageMarkupToolProvider implements SelectionActionProvider, BibRefConstants {
	
	private static final String META_DATA_EDITOR_IMT_NAME = "MetaDataEditor";
	private ImageMarkupTool metaDataEditor = new MetaDataEditor(META_DATA_EDITOR_IMT_NAME);
	private static final String META_DATA_ADDER_IMT_NAME = "MetaDataAdder";
	private ImageMarkupTool metaDataAdder = new MetaDataEditor(META_DATA_ADDER_IMT_NAME);
	
	private BibRefTypeSystem refTypeSystem = BibRefTypeSystem.getDefaultInstance();
	private String[] refIdTypes = {};
	private RefBankClient refBankClient;
	
	/** public zero-argument constructor for class loading */
	public DocumentMetaDataEditorProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Document Meta Data Editor";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		
		//	load configuration
		Settings set = new Settings();
		try {
			set = Settings.loadSettings(new InputStreamReader(this.dataProvider.getInputStream("config.cnfg"), "UTF-8"));
		} catch (IOException ioe) {}
		
		//	connect to RefBank
		String refBankUrl = set.getSetting("refBankUrl");
		if (refBankUrl != null) try {
			this.refBankClient = new RefBankClient(this.dataProvider.getURL(refBankUrl).toString());
		} catch (IOException ioe) {}
		
		//	get publication ID types
		LinkedHashSet refIdTypes = new LinkedHashSet();
		refIdTypes.addAll(Arrays.asList((" " + set.getSetting("refIdTypes", "DOI Handle ISBN ISSN")).split("\\s+")));
		this.refIdTypes = ((String[]) refIdTypes.toArray(new String[refIdTypes.size()]));
		
		//	add style parameter group descriptions for metadata in general ...
		ParameterGroupDescription pgd = new ParameterGroupDescription("docMeta");
		pgd.setLabel("Document Metadata");
		pgd.setDescription("General parameters for locating document metadata as a whole. Not all metadata attributes need to be used in all types of publications: For instance, a template tailored to articles published in a specific journal will likely not extract any publisher or location of publishing.");
		pgd.setParamLabel("maxPageId", "Maximum Page From Start");
		pgd.setParamDescription("maxPageId", "Maximum page from document start to search for metadata attributes, treating first page as 0 (aimed at front matter data)");
		pgd.setParamLabel("maxFromEndPageId", "Maximum Page From End");
		pgd.setParamDescription("maxFromEndPageId", "Maximum page from document end to search for metadata attributes, treating last page as 1 (aimed at back matter data)");
		pgd.setParamLabel("docRefPresent", "Extract Document Reference?");
		pgd.setParamDescription("docRefPresent", "Extract a self-document reference, i.e., a bibliographic reference to the document proper?");
		DocumentStyle.addParameterGroupDescription(pgd);
		
		//	... and individual attributes ...
		for (int f = 0; f < extractableFieldNames.length; f++) {
			if (YEAR_ANNOTATION_TYPE.equals(extractableFieldNames[f]))
				addParameterGroupDescription(extractableFieldNames[f], "Year of Publication", "Parameters for automated extraction of the year a document was published");
			else if (PUBLICATION_DATE_ANNOTATION_TYPE.equals(extractableFieldNames[f]))
				addParameterGroupDescription(extractableFieldNames[f], "Exact Date of Publication", "Parameters for automated extraction of the exact date a document was published");
			else if (VOLUME_DESIGNATOR_ANNOTATION_TYPE.equals(extractableFieldNames[f]) || ISSUE_DESIGNATOR_ANNOTATION_TYPE.equals(extractableFieldNames[f]))
				addParameterGroupDescription(extractableFieldNames[f], (extractableFieldNames[f].substring(0, 1).toUpperCase() + extractableFieldNames[f].substring(1) + " Number"), ("Parameters for automated extraction of the " + extractableFieldNames[f] + " number of a document"));
			else if (VOLUME_TITLE_ANNOTATION_TYPE.equals(extractableFieldNames[f]))
				addParameterGroupDescription(extractableFieldNames[f], "Volume Title", "Parameters for automated extraction of the volume title, i.e., the title of a publication target documents are a part of (mostly for book chapters, the overall book title)");
			else if (AUTHOR_ANNOTATION_TYPE.equals(extractableFieldNames[f]) || EDITOR_ANNOTATION_TYPE.equals(extractableFieldNames[f])) {
				pgd = addParameterGroupDescription(extractableFieldNames[f], (extractableFieldNames[f].substring(0, 1).toUpperCase() + extractableFieldNames[f].substring(1) + "s"), ("Parameters for automated extraction of the " + extractableFieldNames[f] + "s of a document"));
				pgd.setParamLabel("isLastNameLast", "Names are Last Name Last");
				pgd.setParamDescription("isLastNameLast", "The order of author name parts, i.e., last name before ('Kennedy, J.F.') or after ('J.F. Kennedy') first name.");
			}
			else addParameterGroupDescription(extractableFieldNames[f], (extractableFieldNames[f].substring(0, 1).toUpperCase() + extractableFieldNames[f].substring(1)), ("Parameters for automated extraction of the " + extractableFieldNames[f] + " of a document"));
		}
		
		//	... as well as configured identifier types ...
		for (int t = 0; t < this.refIdTypes.length; t++) {
			if (this.refIdTypes[t].length() != 0)
				addParameterGroupDescription(("ID-" + this.refIdTypes[t]), (this.refIdTypes[t] + " Identifier"), ("Parameters for automated extraction of the identifier for a document as issued by " + this.refIdTypes[t] + "."));
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
	
	private static ParameterGroupDescription addParameterGroupDescription(String name, String label, String description) {
		ParameterGroupDescription pgd = new TestableParameterGroupDescription("docMeta" + "." + name);
		pgd.setLabel(label);
		pgd.setDescription(description);
		
		pgd.setParamLabel("maxPageId", "Maximum Page From Start");
		pgd.setParamDescription("maxPageId", "Maximum page from document start to search for the " + pgd.getLabel() + ", treating first page as 0 (aimed at front matter data).");
		pgd.setParamLabel("maxFromEndPageId", "Maximum Page From End");
		pgd.setParamDescription("maxFromEndPageId", "Maximum page from document end to search for the " + pgd.getLabel() + ", treating last page as 1 (aimed at back matter data).");
		
		pgd.setParamLabel("fixedValue", "Fixed Value");
		pgd.setParamDescription("fixedValue", ("A fixed value to use as the " + pgd.getLabel() + " (mostly for journal names)."));
		
		pgd.setParamLabel("area", "Extraction Area");
		pgd.setParamDescription("area", ("A bounding box restricting the area to search the " + pgd.getLabel() + " in (normalized to 72 DPI, leaving this parameter empty deactivates extraction of the " + pgd.getLabel() + ")."));
		
		pgd.setParamLabel("isBold", "Value is Bold");
		pgd.setParamDescription("isBold", ("Include only words in Bold for extraction of " + pgd.getLabel() + " (for filtering)."));
		pgd.setParamLabel("isItalics", "Value is in Italics");
		pgd.setParamDescription("isItalics", ("Include only words in Italics for extraction of " + pgd.getLabel() + " (for filtering)."));
		pgd.setParamLabel("isAllCaps", "Value is in All Caps");
		pgd.setParamDescription("isAllCaps", ("Include only words in All Caps for extraction of " + pgd.getLabel() + " (for filtering)."));
		
		pgd.setParamLabel("fontSize", "Font Size");
		pgd.setParamDescription("fontSize", ("The exact font size of the " + pgd.getLabel() + " (use minimum and maximum font size if variation present or to be expected)."));
		pgd.setParamLabel("minFontSize", "Minimum Font Size");
		pgd.setParamDescription("minFontSize", ("The minimum font size of the " + pgd.getLabel() + " (use only if variation present or to be expected, otherwise use exact font size)."));
		pgd.setParamLabel("maxFontSize", "Maximum Font Size");
		pgd.setParamDescription("maxFontSize", ("The maximum font size of the " + pgd.getLabel() + " (use only if variation present or to be expected, otherwise use exact font size)."));
		
		pgd.setParamLabel("contextPattern", "Context Pattern");
		pgd.setParamDescription("contextPattern", ("A regular expression pattern extracting the " + pgd.getLabel() + " plus some context (helpful especially for numeric attributes like the volume number, e.g. to disambiguate based upon surrounding punctuation)."));
		pgd.setParamLabel("valuePattern", "Value Pattern");
		pgd.setParamDescription("valuePattern", ("A regular expression pattern extracting the actual " + pgd.getLabel() + " (if a context pattern is in use, the value pattern is only applied to what the latter matches)."));
		
		DocumentStyle.addParameterGroupDescription(pgd);
		return pgd;
	}
	
	private static class TestableParameterGroupDescription extends ParameterGroupDescription implements DisplayExtension {
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
				return new DisplayExtensionGraphics[0];
			DocumentStyle docStyle = DocumentStyle.getStyleFor(idmp.document);
			DocumentStyle metaDataStyle = docStyle.getSubset("docMeta");
			DocumentStyle attributeStyle = metaDataStyle.getSubset(this.localPnp);
			if (attributeStyle.isEmpty())
				return DisplayExtensionProvider.NO_DISPLAY_EXTENSION_GRAPHICS;
			
			//	observe minimum and maximum page
			int maxFromStartPageId = attributeStyle.getIntProperty("maxPageId", metaDataStyle.getIntProperty("maxPageId", 0));
			int maxFromEndPageId = attributeStyle.getIntProperty("maxFromEndPageId", metaDataStyle.getIntProperty("maxFromEndPageId", 0));
			if (page.pageId <= maxFromStartPageId) { /* in range at start */ }
			else if (page.pageId >= (idmp.document.getPageCount() - maxFromEndPageId)) { /* in range at end */ }
			else return DisplayExtensionProvider.NO_DISPLAY_EXTENSION_GRAPHICS; // page out of range
			
			//	extract current matches from page and highlight them
			ArrayList attributeValues = new ArrayList();
			if (extractAttribute(this.localPnp, this.localPnp.startsWith("ID-"), page, page.getImageDPI(), attributeStyle, (AUTHOR_ANNOTATION_TYPE.equals(this.localPnp) || EDITOR_ANNOTATION_TYPE.equals(this.localPnp)), null, attributeValues)) {
				ArrayList shapes = new ArrayList();
				for (int v = 0; v < attributeValues.size(); v++) {
					ExtractedAttributeValue eav = ((ExtractedAttributeValue) attributeValues.get(v));
					for (int w = 0; w < eav.words.length; w++)
						shapes.add(new Rectangle2D.Float(eav.words[w].bounds.left, eav.words[w].bounds.top, eav.words[w].bounds.getWidth(), eav.words[w].bounds.getHeight()));
				}
				DisplayExtensionGraphics[] degs = {new DisplayExtensionGraphics(this, null, page, ((Shape[]) shapes.toArray(new Shape[shapes.size()])), attributeMatchLineColor, attributeMatchLineStroke, attributeMatchFillColor) {
					public boolean isActive() {
						return true;
					}
				}};
				return degs;
			}
			
			//	nothing to show right now
			return DisplayExtensionProvider.NO_DISPLAY_EXTENSION_GRAPHICS;
		}
		private static final Color attributeMatchLineColor = Color.BLUE;
		private static final BasicStroke attributeMatchLineStroke = new BasicStroke(3);
		private static final Color attributeMatchFillColor = new Color(attributeMatchLineColor.getRed(), attributeMatchLineColor.getGreen(), attributeMatchLineColor.getBlue(), 64);
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
						m.add(mi);
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
			annotateDocReference(idmp.document, ref);
		
		//	finally ...
		idmp.endAtomicAction();
		
		//	make changes show
		idmp.setAnnotationsPainted(annotType, true);
		idmp.validate();
		idmp.repaint();
	}
	
	private static String sanitizeAttributeValue(String type, String value, boolean isIdentifier) {
		
		//	normalize dashes and whitespace
		String sValue = sanitizeString(value, (AUTHOR_ANNOTATION_TYPE.equals(type) || EDITOR_ANNOTATION_TYPE.equals(type)), isIdentifier);
		
		//	check numeric attributes, translating Roman numbers in the process
		if (YEAR_ANNOTATION_TYPE.equals(type)) {
			sValue = sValue.replaceAll("\\s", "");
			sValue = removeLeadingZeros(sValue);
			if (StringUtils.isRomanNumber(sValue))
				sValue = ("" + StringUtils.parseRomanNumber(sValue));
			return (sValue.matches("[12][0-9]{3}") ? sValue : null);
		}
		else if (PUBLICATION_DATE_ANNOTATION_TYPE.equals(type))
			return DateTimeUtils.parseTextDate(sValue);
		else if (VOLUME_DESIGNATOR_ANNOTATION_TYPE.equals(type) || ISSUE_DESIGNATOR_ANNOTATION_TYPE.equals(type)) {
			sValue = sValue.replaceAll("\\s", "");
			sValue = removeLeadingZeros(sValue);
			if (StringUtils.isRomanNumber(sValue))
				sValue = ("" + StringUtils.parseRomanNumber(sValue));
			return (sValue.matches("[1-9][0-9]*") ? sValue : null);
		}
		else if (PAGINATION_ANNOTATION_TYPE.equals(type)) {
			sValue = sValue.replaceAll("\\s", "");
			if (sValue.indexOf('-') == -1) {
				sValue = removeLeadingZeros(sValue);
				if (StringUtils.isRomanNumber(sValue))
					sValue = ("" + StringUtils.parseRomanNumber(sValue));
				return (sValue.matches("[1-9][0-9]*") ? sValue : null);
			}
			else if (sValue.indexOf('-') == sValue.lastIndexOf('-')) {
				String[] valueParts = sValue.split("\\s*\\-\\s*");
				if (valueParts.length != 2)
					return null;
				String fpn = sanitizeAttributeValue(type, valueParts[0], isIdentifier);
				if (fpn == null)
					return null;
				String lpn = sanitizeAttributeValue(type, valueParts[1], isIdentifier);
				if (lpn == null)
					return null;
				if (lpn.length() < fpn.length())
					lpn = (fpn.substring(0, (fpn.length() - lpn.length())) + lpn);
				return (fpn + "-" + lpn);
			}
			else return null;
		}
		
		//	for person name attributes (author and editor), try and convert to <lastName>, <firstName>
		else if (AUTHOR_ANNOTATION_TYPE.equals(type) || EDITOR_ANNOTATION_TYPE.equals(type))
			return flipNameParts(sValue, value);
		
		//	return normalized value for all other attributes
		else return sValue;
	}
	
	private static String removeLeadingZeros(String value) {
		if (value == null)
			return null;
		value = value.trim();
		while (value.startsWith("0"))
			value = value.substring("0".length()).trim();
		return value;
	}
	
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
		VOLUME_DESIGNATOR_ANNOTATION_TYPE,
		ISSUE_DESIGNATOR_ANNOTATION_TYPE,
		PUBLISHER_ANNOTATION_TYPE,
		LOCATION_ANNOTATION_TYPE,
		EDITOR_ANNOTATION_TYPE,
		VOLUME_TITLE_ANNOTATION_TYPE,
		PUBLICATION_URL_ANNOTATION_TYPE,
	};
	
	private class MetaDataEditor implements ImageMarkupTool, WebDocumentViewer {
		private String name;
		MetaDataEditor(String name) {
			this.name = name;
		}
		public String getLabel() {
			if (META_DATA_EDITOR_IMT_NAME.equals(this.name))
				return "Edit Document Meta Data";
			else if (META_DATA_ADDER_IMT_NAME.equals(this.name))
				return "Add Document Meta Data";
			else return null;
		}
		public String getTooltip() {
			if (META_DATA_EDITOR_IMT_NAME.equals(this.name))
				return "Edit the bibliographic meta data of the document";
			else if (META_DATA_ADDER_IMT_NAME.equals(this.name))
				return "Import or extract the bibliographic meta data of the document";
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
			return new WebDocumentView(this, baseUrl) {
				private RefData ref;
				private HashMap attributesToValues = new HashMap();
				protected void preProcess(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp) {
					
					//	get bibliographic data from document
					this.ref = getRefData(doc, this.attributesToValues);
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
								if (refBankClient != null)
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
							if (refBankClient != null) {
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
	
	private void extractDocumentMetaData(ImDocument doc, boolean allowPrompt, ProgressMonitor pm) {
		
		//	get bibliographic data from document
		HashMap attributesToValues = new HashMap();
		RefData ref = this.getRefData(doc, attributesToValues);
		
		//	open meta data for editing if allowed to
		if (allowPrompt) {
			ref = this.editRefData(doc, ref, attributesToValues);
			
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
	
	private RefData getRefData(ImDocument doc, HashMap attributesToValues) {
		
		//	put attributes from document in BibRef object
		RefData ref = BibRefUtils.modsAttributesToRefData(doc);
		
		//	use document style template if given
		DocumentStyle docStyle = DocumentStyle.getStyleFor(doc);
		DocumentStyle metaDataStyle = docStyle.getSubset("docMeta");
		int metaDataMaxFromStartPageId = metaDataStyle.getIntProperty("maxPageId", 0);
		int metaDataMaxFromEndPageId = metaDataStyle.getIntProperty("maxFromEndPageId", 0);
		ImPage[] pages = doc.getPages();
		if (extractAttribute(AUTHOR_ANNOTATION_TYPE, false, pages, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, true, ref, attributesToValues)) {
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
		extractAttribute(YEAR_ANNOTATION_TYPE, false, pages, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues);
		extractAttribute(PUBLICATION_DATE_ANNOTATION_TYPE, false, pages, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues);
		extractAttribute(TITLE_ANNOTATION_TYPE, false, pages, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues);
		
		extractAttribute(JOURNAL_NAME_ANNOTATION_TYPE, false, pages, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues);
		extractAttribute(VOLUME_DESIGNATOR_ANNOTATION_TYPE, false, pages, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues);
		extractAttribute(ISSUE_DESIGNATOR_ANNOTATION_TYPE, false, pages, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues);
		extractAttribute(NUMERO_DESIGNATOR_ANNOTATION_TYPE, false, pages, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues);
		
		extractAttribute(PUBLISHER_ANNOTATION_TYPE, false, pages, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues);
		extractAttribute(LOCATION_ANNOTATION_TYPE, false, pages, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues);
		
		extractAttribute(VOLUME_TITLE_ANNOTATION_TYPE, false, pages, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues);
		if (extractAttribute(EDITOR_ANNOTATION_TYPE, false, pages, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, true, ref, attributesToValues)) {
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
		
		if (extractAttribute(PAGINATION_ANNOTATION_TYPE, false, pages, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues))
			ref.setAttribute(PAGINATION_ANNOTATION_TYPE, ref.getAttribute(PAGINATION_ANNOTATION_TYPE).trim().replaceAll("[^0-9]+", "-"));
		
		for (int t = 0; t < this.refIdTypes.length; t++) {
			if (this.refIdTypes[t].length() == 0)
				continue; // skip over wildcard ID type
			if (extractAttribute(("ID-" + this.refIdTypes[t]), true, pages, metaDataMaxFromStartPageId, metaDataMaxFromEndPageId, metaDataStyle, false, ref, attributesToValues))
				ref.setIdentifier(this.refIdTypes[t], ref.getIdentifier(this.refIdTypes[t]).replaceAll("\\s", ""));
		}
		
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
		
		//	transfer PDF URL is given
		if (doc.hasAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE) && (doc.getAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE) instanceof String) && !ref.hasAttribute(PUBLICATION_URL_ANNOTATION_TYPE))
			ref.setAttribute(PUBLICATION_URL_ANNOTATION_TYPE, ((String) doc.getAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE)));
		
		//	try to find title in document head if not already given
		if (!ref.hasAttribute(TITLE_ANNOTATION_TYPE)) {
			String title = findTitle(doc);
			if (title != null)
				ref.setAttribute(TITLE_ANNOTATION_TYPE, title);
		}
		
		//	try and add pagination from document if not already given
		if ((pages.length != 0) && pages[0].hasAttribute(PAGE_NUMBER_ATTRIBUTE) && pages[pages.length-1].hasAttribute(PAGE_NUMBER_ATTRIBUTE) && !ref.hasAttribute(PAGINATION_ANNOTATION_TYPE)) try {
			int firstPageNumber = Integer.parseInt((String) pages[0].getAttribute(PAGE_NUMBER_ATTRIBUTE));
			int lastPageNumber = Integer.parseInt((String) pages[pages.length-1].getAttribute(PAGE_NUMBER_ATTRIBUTE));
			if ((lastPageNumber - firstPageNumber + 1) == pages.length)
				ref.setAttribute(PAGINATION_ANNOTATION_TYPE, (firstPageNumber + "-" + lastPageNumber));
		} catch (NumberFormatException nfe) {}
		
		//	finally ...
		return ref;
	}
	
	private void storeRefData(ImDocument doc, RefData ref, HashMap attributesToValues, boolean auto) {
		
		//	store meta data in respective attributes
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
		annotateAttributeValues(ref, ref.getAttributeNames(), extractableFieldNames, false, doc, attributesToValues, auto);
		annotateAttributeValues(ref, ref.getIdentifierTypes(), this.refIdTypes, true, doc, attributesToValues, auto);
		
		//	annotate document reference
		annotateDocReference(doc, ref);
	}
	
	private static void annotateDocReference(ImDocument doc, RefData ref) {
		
		//	check for existing annotations
		ImAnnotation[] docRefAnnots = doc.getAnnotations("docRef");
		if (docRefAnnots.length != 0)
			return;
		
		//	use document style template if given
		DocumentStyle docStyle = DocumentStyle.getStyleFor(doc);
		DocumentStyle metaDataStyle = docStyle.getSubset("docMeta");
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
		int metaDataMaxFromStartPageId = metaDataStyle.getIntProperty("maxPageId", 0);
		int metaDataMaxFromEndPageId = metaDataStyle.getIntProperty("maxFromEndPageId", 0);
		DocumentStyle docRefStyle = metaDataStyle.getSubset("docRef");
		int fontSize = docRefStyle.getIntProperty("fontSize", -1);
		int minFontSize = docRefStyle.getIntProperty("minFontSize", ((fontSize == -1) ? 0 : fontSize));
		int maxFontSize = docRefStyle.getIntProperty("maxFontSize", ((fontSize == -1) ? 72 : fontSize));
		
		//	collect candidates through pages
		ImPage[] pages = doc.getPages();
		ArrayList docRefCandidates = new ArrayList();
		
		//	try extraction from document start first
		int maxFromStartPageId = docRefStyle.getIntProperty("maxPageId", metaDataMaxFromStartPageId);
		for (int p = 0; (p <= maxFromStartPageId) && (p < pages.length); p++)
			addDocReferenceCandidates(doc, pages[p], minFontSize, maxFontSize, refDataTokens, refIdTokens, docRefCandidates);
		
		//	try extraction from document end only second
		int maxFromEndPageId = docRefStyle.getIntProperty("maxFromEndPageId", metaDataMaxFromEndPageId);
		for (int p = (pages.length - maxFromEndPageId); p < pages.length; p++)
			addDocReferenceCandidates(doc, pages[p], minFontSize, maxFontSize, refDataTokens, refIdTokens, docRefCandidates);
		
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
	
	private static void addDocReferenceCandidates(ImDocument doc, ImPage page, int minFontSize, int maxFontSize, CountingSet refDataTokens, CountingSet refIdTokens, ArrayList docRefCandidates) {
		
		//	work through paragraphs
		ImRegion[] paragraphs = page.getRegions(ImRegion.PARAGRAPH_TYPE);
		for (int p = 0; p < paragraphs.length; p++) {
			ImWord[] words = paragraphs[p].getWords();
			System.out.println("Checking paragraph " + page.pageId + "." + paragraphs[p].bounds + " for document reference");
			
			//	check length first
			if ((refDataTokens.size() * 3) < words.length) {
				System.out.println(" ==> too large (" + words.length + " for " + refDataTokens.size() + " reference tokens)");
				continue; // this one is just too large, even accounting for punctuation and identifiers
			}
			if ((words.length * 2) < refDataTokens.size()) {
				System.out.println(" ==> too small (" + words.length + " for " + refDataTokens.size() + " reference tokens)");
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
					System.out.println(" ==> too many words outside font size range (" + nonFsWordWidth + " against " + fsWordWidth + " in range [" + minFontSize + "," + maxFontSize + "])");
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
			System.out.println(" - checking " + tokens.size() + " tokens:");
			for (int t = 0; t < tokens.size(); t++) {
				String token = StringUtils.normalizeString(tokens.valueAt(t)).trim();
				if (Gamta.isPunctuation(token))
					continue;
				tokenCount++;
				if (unmatchedRefDataTokens.contains(token)) {
					unmatchedRefDataTokens.remove(token);
					refTokenCount++;
					System.out.println("   - got matching token '" + token + "'");
				}
				else if (refIdTokens.contains(token)) {
					idTokenCount++;
					System.out.println("   - got identifier token '" + token + "'");
				}
				else {
					spuriousTokens.add(token);
					System.out.println("   - got spurious token '" + token + "'");
				}
			}
			System.out.println(" --> got " + tokenCount + " tokens");
			System.out.println(" --> got " + refTokenCount + " matching tokens");
			System.out.println(" --> got " + idTokenCount + " identifier tokens");
			System.out.println(" --> got " + spuriousTokens.size() + " spurious tokens");
			System.out.println(" --> got " + unmatchedRefDataTokens.size() + " unmatched reference tokens:");
			int abbrevMatchTokenCount = 0;
			for (Iterator umtit = unmatchedRefDataTokens.iterator(); umtit.hasNext();) {
				String unmatchedToken = ((String) umtit.next());
				System.out.println("   - '" + unmatchedToken + "' (" + unmatchedRefDataTokens.getCount(unmatchedToken) + ")");
				for (Iterator stit = spuriousTokens.iterator(); stit.hasNext();) {
					String spuriousToken = ((String) stit.next());
					if (StringUtils.isAbbreviationOf(unmatchedToken, spuriousToken, true)) {
						abbrevMatchTokenCount += Math.min(unmatchedRefDataTokens.getCount(unmatchedToken), spuriousTokens.getCount(spuriousToken));
						System.out.println("     --> abbreviation matched to '" + spuriousToken + "' (" + spuriousTokens.getCount(spuriousToken) + ")");
						break;
					}
				}
			}
			System.out.println(" --> got " + abbrevMatchTokenCount + " abbeviation matching tokens");
			float precision = (((float) (refTokenCount + idTokenCount + ((abbrevMatchTokenCount + 1) / 2))) / tokenCount);
			float recall = (((float) ((refDataTokens.size() - unmatchedRefDataTokens.size()) + ((abbrevMatchTokenCount + 1) / 2))) / refDataTokens.size());
			float accuracy = (precision * recall);
			System.out.println(" ==> match accuracy is " + accuracy + " (P" + precision + "/R" + recall + ")");
			
			//	store candidate if match good enough
			if (accuracy >= 0.75) {
				docRefCandidates.add(new DocReferenceCandidate(paragraphs[p], words, accuracy));
				System.out.println(" ==> good candidate");
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
	
	private static boolean extractAttribute(String name, boolean isIdentifier, ImPage[] pages, int maxFromStartPageId, int maxFromEndPageId, DocumentStyle docStyle, boolean isMultiValue, RefData ref, HashMap attributesToValues) {
		
		//	get attribute specific parameters
		DocumentStyle attributeStyle = docStyle.getSubset(name);
		
		//	collect match words
		ArrayList attributeValues = new ArrayList(isMultiValue ? 5 : 1);
		
		//	try extraction from document start first
		maxFromStartPageId = attributeStyle.getIntProperty("maxPageId", maxFromStartPageId);
		for (int p = 0; (p <= maxFromStartPageId) && (p < pages.length); p++) {
			int dpi = pages[p].getImageDPI();
			if (extractAttribute(name, isIdentifier, pages[p], dpi, attributeStyle, isMultiValue, ref, attributeValues)) {
				attributesToValues.put(name, attributeValues);
				return true;
			}
		}
		
		//	try extraction from document end only second
		maxFromEndPageId = attributeStyle.getIntProperty("maxFromEndPageId", maxFromEndPageId);
		for (int p = (pages.length - maxFromEndPageId); p < pages.length; p++) {
			int dpi = pages[p].getImageDPI();
			if (extractAttribute(name, isIdentifier, pages[p], dpi, attributeStyle, isMultiValue, ref, attributeValues)) {
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
	
	private static boolean extractAttribute(String name, boolean isIdentifier, ImPage page, int dpi, DocumentStyle attributeStyle, boolean isMultiValue, RefData ref, ArrayList attributeValues) {
		
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
				System.out.println("Got existing " + name + " from " + annotType + ": " + value);
				String sValue = sanitizeAttributeValue(name, value, isIdentifier);
				if (sValue == null)
					sValue = ref.getAttribute(name);
				attributeValues.add(new ExtractedAttributeValue(name, value, StringUtils.normalizeString(value), sValue, getSpannedWords(annots[a].getFirstWord(), annots[a].getLastWord())));
			}
			if (attributeValues.isEmpty())
				System.out.println("Existing " + name + " not found from " + annotType);
			return false;
		}
		System.out.println("Extracting " + name + ":");
		
		//	check for fixed value
		String fixedValue = attributeStyle.getProperty("fixedValue");
		if (fixedValue != null) {
			System.out.println(" ==> fixed to " + fixedValue);
			if (ref != null)
				ref.setAttribute(name, fixedValue);
			return false;
		}
		
		//	get extraction parameters
		BoundingBox area = attributeStyle.getBoxProperty("area", null, dpi);
		boolean isBold = attributeStyle.getBooleanProperty("isBold", false);
		boolean isItalics = attributeStyle.getBooleanProperty("isItalics", false);
		boolean isAllCaps = attributeStyle.getBooleanProperty("isAllCaps", false);
		int fontSize = attributeStyle.getIntProperty("fontSize", -1);
		int minFontSize = attributeStyle.getIntProperty("minFontSize", ((fontSize == -1) ? 0 : fontSize));
		int maxFontSize = attributeStyle.getIntProperty("maxFontSize", ((fontSize == -1) ? 72 : fontSize));
		String contextPattern = attributeStyle.getProperty("contextPattern", null);
		String valuePattern = attributeStyle.getProperty("valuePattern", null);
		if (area == null) {
			System.out.println(" ==> deactivated");
			return false;
		}
		
		//	get words from area
		ImWord[] words = page.getWordsInside(area);
		System.out.println(" - got " + words.length + " words in area");
		if (words.length == 0)
			return false;
		
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
		System.out.println(" - got " + words.length + " matching style and font size");
		if (words.length == 0)
			return false;
		
		//	order and concatenate words
		//	TODO use sortIntoLines() instead if we have many individual words (graphics label false positive, etc.)
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
				wordStringBuilder.append(" ");
				wordAtChar.add(words[w]);
			}
			else if (words[w].getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END) {
				wordStringBuilder.append(" ");
				wordAtChar.add(words[w]);
			}
			else if ((words[w].getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED) && (wordStringBuilder.length() != 0)) {
				wordStringBuilder.deleteCharAt(wordStringBuilder.length()-1);
				wordAtChar.remove(wordAtChar.size()-1);
			}
			else if (words[w].getNextRelation() == ImWord.NEXT_RELATION_SEPARATE) {
				if (words[w].bounds.bottom <= words[w+1].centerY) {
					wordStringBuilder.append(" ");
					wordAtChar.add(words[w]);
				}
				else if (words[w].bounds.top > words[w+1].centerY) {
					wordStringBuilder.append(" ");
					wordAtChar.add(words[w]);
				}
				else if ((words[w].bounds.right < words[w+1].bounds.left) && Gamta.insertSpace(words[w].getString(), words[w+1].getString())) {
					wordStringBuilder.append(" ");
					wordAtChar.add(words[w]);
				}
				else if (StringUtils.DASHES.contains(words[w].getString()) != StringUtils.DASHES.contains(words[w+1].getString())) {
					wordStringBuilder.append(" ");
					wordAtChar.add(words[w]);
				}
			}
		}
		String wordString = wordStringBuilder.toString();
		System.out.println(" - word string is " + wordString);
		
		//	normalize string for pattern matching
		String nWordString = StringUtils.normalizeString(wordString);
		System.out.println(" - normalized word string is " + nWordString);
		
		//	nothing further to extract, we're done
		if ((contextPattern == null) && (valuePattern == null)) {
			System.out.println(" ==> set to " + wordString);
			String sWordString = sanitizeAttributeValue(name, wordString, isIdentifier);
			if (ref != null)
				ref.setAttribute(name, ((sWordString == null) ? wordString : sWordString));
			attributeValues.add(new ExtractedAttributeValue(name, wordString, nWordString, sWordString, getAttributeWords(wordAtChar, 0, wordAtChar.size())));
			return true;
		}
		
		//	use context narrowing pattern
		if (contextPattern != null) try {
			Matcher matcher = Pattern.compile(contextPattern).matcher(nWordString);
			if (matcher.find()) {
				wordString = wordString.substring(matcher.start(), matcher.end());
				nWordString = matcher.group();
				wordAtChar = new ArrayList(wordAtChar.subList(matcher.start(), matcher.end()));
				System.out.println(" - context pattern cut to " + wordString);
			}
			else {
				System.out.println(" ==> context pattern mismatch");
				return false;
			}
		}
		catch (Exception e) {
			System.out.println(" - context pattern error: " + e.getMessage());
			return false;
		}
		
		//	set plain attribute
		if (valuePattern == null) {
			System.out.println(" ==> set to " + wordString);
			String sWordString = sanitizeAttributeValue(name, wordString, isIdentifier);
			if (ref != null)
				ref.setAttribute(name, ((sWordString == null) ? wordString : sWordString));
			attributeValues.add(new ExtractedAttributeValue(name, wordString, nWordString, sWordString, getAttributeWords(wordAtChar, 0, wordAtChar.size())));
			return true;
		}
		
		//	extract attribute value(s) via pattern
		else try {
			Matcher matcher = Pattern.compile(valuePattern).matcher(nWordString);
			while (matcher.find()) {
				if (isMultiValue) {
					System.out.println(" ==> value pattern added " + wordString.substring(matcher.start(), matcher.end()));
					String singleValue = wordString.substring(matcher.start(), matcher.end());
					String nSingleValue = matcher.group();
					String sSingleValue = sanitizeAttributeValue(name, singleValue, isIdentifier);
					if (ref != null)
						ref.addAttribute(name, ((sSingleValue == null) ? singleValue : sSingleValue));
					attributeValues.add(new ExtractedAttributeValue(name, singleValue, nSingleValue, sSingleValue, getAttributeWords(wordAtChar, matcher.start(), matcher.end())));
				}
				else {
					System.out.println(" ==> value pattern set to " + wordString.substring(matcher.start(), matcher.end()));
					String value = wordString.substring(matcher.start(), matcher.end());
					String nValue = matcher.group();
					String sValue = sanitizeAttributeValue(name, value, isIdentifier);
					if (ref != null)
						ref.setAttribute(name, ((sValue == null) ? value : sValue));
					attributeValues.add(new ExtractedAttributeValue(name, value, nValue, sValue, getAttributeWords(wordAtChar, matcher.start(), matcher.end())));
					return true;
				}
			}
			if ((ref != null) && ref.hasAttribute(name))
				return true;
			else if (attributeValues.size() != 0)
				return true;
			else {
				System.out.println(" ==> value pattern mismatch");
				return false;
			}
		}
		catch (Exception e) {
			System.out.println(" - value pattern error: " + e.getMessage());
			return false;
		}
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
	
	private static class ExtractedAttributeValue implements Comparable {
		final String name;
		final String rValue;
		final String nValue;
		final String sValue;
		final ImWord[] words;
		final ImWord firstWord;
		final ImWord lastWord;
		ExtractedAttributeValue(String name, String rValue, String nValue, String sValue, ImWord[] words) {
			this.name = name;
			this.rValue = rValue;
			this.nValue = nValue;
			this.sValue = sValue;
			this.words = words;
			this.firstWord = this.words[0];
			this.lastWord = this.words[this.words.length - 1];
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
	
	private void setDocumentAttributes(ImDocument doc, RefData ref) {
		
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
	
	private static void annotateAttributeValues(RefData ref, String[] refAttributeNames, String[] allAttributeNames, boolean isIdentifierTypes, ImDocument doc, HashMap attributesToValues, boolean auto) {
		HashSet cleanupAttributeNames = new HashSet(Arrays.asList(allAttributeNames));
		ImDocumentRoot wrappedDoc = null;
		for (int an = 0; an < refAttributeNames.length; an++) {
			if (isIdentifierTypes && "".equals(refAttributeNames[an]))
				continue; // have to skip over wildcard ID type
			String refAttributeName = ((isIdentifierTypes ? "ID-" : "") + refAttributeNames[an]);
			cleanupAttributeNames.remove(refAttributeNames[an]); // no cleanup required here
			System.out.println("Annotating " + refAttributeName + ":");
			
			//	get existing annotations
			String valueAnnotType = ((isIdentifierTypes ? "docId" : "doc") + Character.toUpperCase(refAttributeNames[an].charAt(0)) + refAttributeNames[an].substring(1));
			ArrayList valueAnnots = new ArrayList(Arrays.asList(doc.getAnnotations(valueAnnotType)));
			System.out.println(" - got " + valueAnnots.size() + " existing " + valueAnnotType);
			
			//	get extracted values (creating working copy)
			ArrayList extractedValues = new ArrayList();
			if (attributesToValues.containsKey(refAttributeName))
				extractedValues.addAll((ArrayList) attributesToValues.get(refAttributeName));
			System.out.println(" - got " + extractedValues.size() + " extracted values");
			
			//	process reference values
			ArrayList refValues = new ArrayList(Arrays.asList(ref.getAttributeValues(refAttributeName)));
			System.out.println(" - got " + refValues.size() + " values from reference");
			for (int v = 0; v < refValues.size(); v++) {
				String refValue = ((String) refValues.get(v));
				System.out.println("   - handling " + refValue + ":");
				ImAnnotation refValueAnnot = null;
				
				//	if we have an existing annotation, we're all set
				for (int a = 0; a < valueAnnots.size(); a++) {
					ImAnnotation valueAnnot = ((ImAnnotation) valueAnnots.get(a));
					String annotValue = ImUtils.getString(valueAnnot.getFirstWord(), valueAnnot.getLastWord(), true);
					if (isIdentifierTypes)
						annotValue = annotValue.replaceAll("\\s", "");
					if (annotValue.equalsIgnoreCase(refValue)) {
						refValueAnnot = valueAnnot;
						valueAnnots.remove(a--); // no need to remove this one later on
						System.out.println("     ==> found raw value matching annotation");
						break; // we're done here
					}
					String nAnnotValue = StringUtils.normalizeString(annotValue);
					if (nAnnotValue.equalsIgnoreCase(refValue)) {
						refValueAnnot = valueAnnot;
						valueAnnots.remove(a--); // no need to remove this one later on
						System.out.println("     ==> found normalized value matching annotation");
						break; // we're done here
					}
					String sAnnotValue = sanitizeAttributeValue(refAttributeName, annotValue, false);
					if (sAnnotValue == null)
						sAnnotValue = sanitizeString(annotValue, (AUTHOR_ANNOTATION_TYPE.equals(refAttributeName) || EDITOR_ANNOTATION_TYPE.equals(refAttributeName)), false);
					if (sAnnotValue.equalsIgnoreCase(refValue)) {
						refValueAnnot = valueAnnot;
						valueAnnots.remove(a--); // no need to remove this one later on
						System.out.println("     ==> found sanitized value matching annotation");
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
				System.out.println("     --> matching annotation not found");
				
				//	if we have an extracted value, we can annotate from there
				for (int e = 0; e < extractedValues.size(); e++) {
					ExtractedAttributeValue extractedValue = ((ExtractedAttributeValue) extractedValues.get(e));
					if (extractedValue.rValue.equalsIgnoreCase(refValue)) {}
					else if (extractedValue.nValue.equalsIgnoreCase(refValue)) {}
					else if ((extractedValue.sValue != null) && extractedValue.sValue.equalsIgnoreCase(refValue)) {}
					else continue;
					refValueAnnot = doc.addAnnotation(extractedValue.firstWord, extractedValue.lastWord, valueAnnotType); // annotate
					System.out.println("     ==> annotated matching extracted value as " + valueAnnotType);
					break; // we're done here
				}
				if (refValueAnnot != null) {
					refValues.remove(v--); // no need to go for this value any further
					wrappedDoc = null; // invalidate XML wrapper
					continue; // we're done with this one
				}
				System.out.println("     --> matching extracted value not found");
			}
			
			//	remove spurious annotations
			for (int n = 0; n < valueAnnots.size(); n++) {
				doc.removeAnnotation((ImAnnotation) valueAnnots.get(n));
				wrappedDoc = null; // invalidate XML wrapper
			}
			System.out.println(" - cleaned up " + valueAnnots.size() + " spurious " + valueAnnotType);
			
			//	anything left to annotate?
			if (refValues.isEmpty())
				continue; // we're done with this attribute
			System.out.println(" - annotating " + refValues.size() + " remaining values from reference");
			
			//	(re)create wrapper only on demand
			if (wrappedDoc == null)
				wrappedDoc = new ImDocumentRoot(doc.getPage(doc.getFirstPageId()), (ImDocumentRoot.NORMALIZATION_LEVEL_WORDS | ImDocumentRoot.NORMALIZE_CHARACTERS));
			
			//	annotate any remaining values
			annotateAttributeValues(refAttributeName, refValues, valueAnnotType, wrappedDoc);
		}
		
		//	nothing to clean up (we're strictly adding in automated mode)
		if (auto || cleanupAttributeNames.isEmpty())
			return;
		
		//	clean up any annotations of removed attributes
		for (Iterator canit = cleanupAttributeNames.iterator(); canit.hasNext();) {
			String cleanupAttributeName = ((String) canit.next());
			if (isIdentifierTypes && "".equals(cleanupAttributeName))
				continue; // have to skip over wildcard ID type
			System.out.println("Cleaning up " + cleanupAttributeName + ":");
			
			//	get existing annotations
			String valueAnnotType = ((isIdentifierTypes ? "docId" : "doc") + Character.toUpperCase(cleanupAttributeName.charAt(0)) + cleanupAttributeName.substring(1));
			ImAnnotation[] valueAnnots = doc.getAnnotations(valueAnnotType);
			System.out.println(" - got " + valueAnnots.length + " existing " + valueAnnotType);
			
			//	remove spurious annotations
			for (int a = 0; a < valueAnnots.length; a++) {
				doc.removeAnnotation(valueAnnots[a]);
				wrappedDoc = null; // invalidate XML wrapper
			}
			System.out.println("   ==> done");
		}
	}
	
	private static void annotateAttributeValues(String attributeName, ArrayList values, String valueAnnotType, ImDocumentRoot wrappedDoc) {
		
		//	create attribute value dictionary
		StringVector valueDict = new StringVector(false);
		for (int v = 0; v < values.size(); v++) {
			String value = ((String) values.get(v));
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
		System.out.println("Annotating " + values.size() + " " + attributeName + " as " + valueAnnotType);
		
		//	annotate attribute values (each one only once, though)
		Annotation[] valueAnnots = Gamta.extractAllContained(wrappedDoc, valueDict);
		TreeSet annotatedValues = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		for (int a = 0; a < valueAnnots.length; a++) 
			if (annotatedValues.add(valueAnnots[a].getValue())) {
				valueAnnots[a].changeTypeTo(valueAnnotType);
				wrappedDoc.addAnnotation(valueAnnots[a]);
				System.out.println(" - annotated " + valueAnnots[a].getValue() + " as " + valueAnnotType);
			}
	}
	
	private RefData editRefData(final ImDocument doc, RefData ref, final HashMap attributesToValues) {
		final String docName = ((String) doc.getAttribute(DOCUMENT_NAME_ATTRIBUTE));
		final JDialog refEditDialog = DialogFactory.produceDialog(("Get Meta Data for Document" + ((docName == null) ? "" : (" " + docName))), true);
		final BibRefEditorPanel refEditorPanel = new BibRefEditorPanel(refTypeSystem, refIdTypes, ref);
		final boolean[] cancelled = {false};
		
		JButton extract = new JButton("Extract");
		extract.setToolTipText("Extract meta data from document");
		extract.addActionListener(new ActionListener() {
			ImDocumentRoot wrappedDoc = null;
			public void actionPerformed(ActionEvent ae) {
				if (this.wrappedDoc == null)
					this.wrappedDoc = new ImDocumentRoot(doc.getPage(doc.getFirstPageId()), ImDocumentRoot.NORMALIZATION_LEVEL_WORDS);
				RefData ref = refEditorPanel.getRefData();
				if (fillFromDocument(refEditDialog, docName, this.wrappedDoc, ref, attributesToValues)) {
					if (doc.hasAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE) && (doc.getAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE) instanceof String) && !ref.hasAttribute(PUBLICATION_URL_ANNOTATION_TYPE))
						ref.setAttribute(PUBLICATION_URL_ANNOTATION_TYPE, ((String) doc.getAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE)));
					refEditorPanel.setRefData(ref);
				}
			}
		});
		
		JButton search = new JButton("Search");
		search.setToolTipText("Search RefBank for meta data, using current input as query");
		if (refBankClient == null)
			search = null;
		else search.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				RefData ref = refEditorPanel.getRefData();
				ref = searchRefData(refEditDialog, ref, docName);
				if (ref != null) {
					if (doc.hasAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE) && (doc.getAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE) instanceof String) && !ref.hasAttribute(PUBLICATION_URL_ANNOTATION_TYPE))
						ref.setAttribute(PUBLICATION_URL_ANNOTATION_TYPE, ((String) doc.getAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE)));
					refEditorPanel.setRefData(ref);
				}
			}
		});
		
		JButton validate = new JButton("Validate");
		validate.setToolTipText("Check if the meta data is complete");
		validate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String[] errors = refEditorPanel.getErrors();
				if (errors == null)
					DialogFactory.alert("The document meta data is valid.", "Validation Report", JOptionPane.INFORMATION_MESSAGE);
				else displayErrors(errors, refEditorPanel);
			}
		});
		
		JButton ok = new JButton("OK");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String[] errors = refEditorPanel.getErrors();
				if (errors == null)
					refEditDialog.dispose();
				else displayErrors(errors, refEditorPanel);
			}
		});
		
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				cancelled[0] = true;
				refEditDialog.dispose();
			}
		});
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
		buttonPanel.add(extract);
		if (search != null)
			buttonPanel.add(search);
		buttonPanel.add(validate);
		buttonPanel.add(ok);
		buttonPanel.add(cancel);
		
		refEditDialog.getContentPane().setLayout(new BorderLayout());
		refEditDialog.getContentPane().add(refEditorPanel, BorderLayout.CENTER);
		refEditDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		refEditDialog.setSize(600, 600);
		refEditDialog.setLocationRelativeTo(DialogFactory.getTopWindow());
		refEditDialog.setVisible(true);
		
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
			
			//	TODO add button 'Reference' to select whole reference at once, run it through RefParse, and then use all attributes found
			
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
//				String value = textArea.getSelectedText().trim();
//				if (value.length() != 0) {
//					String sValue = sanitizeString(value, (AUTHOR_ANNOTATION_TYPE.equals(this.attribute) || EDITOR_ANNOTATION_TYPE.equals(this.attribute)), this.attribute.startsWith("ID-"));
//					if (AUTHOR_ANNOTATION_TYPE.equals(this.attribute) || EDITOR_ANNOTATION_TYPE.equals(this.attribute)) {
//						flipNameParts(sValue, value);
//						ref.addAttribute(this.attribute, sValue);
//					}
//					else ref.setAttribute(this.attribute, sValue);
//					refModified = true;
//					this.setToolTipText(this.getText() + ": " + ref.getAttributeValueString(this.attribute, " & "));
//					this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createLineBorder(Color.GREEN)));
//				}
				
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
		DialogFactory.alert(("The document meta data is not valid. In particular, there are the following errors:\n" + errorMessageBuilder.concatStrings("\n")), "Validation Report", JOptionPane.ERROR_MESSAGE);
	}
	
	private final RefData searchRefData(JDialog dialog, RefData query, String docName) {
		
		//	perform search
		Vector refs = this.searchRefData(query);
		if (refs == null) {
			if (dialog != null)
				DialogFactory.alert("Please enter some data to search for.\nYou can also use 'View Doc' to copy some data from the document text.", "Cannot Search Document Meta Data", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		
		//	did we find anything?
		if (refs.isEmpty()) {
			if (dialog != null)
				DialogFactory.alert("Your search did not return any results.\nYou can still enter the document meta data manually.", "Document Meta Data Not Fount", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		
		//	not allowed to show list dialog, return data only if we have an unambiguous match
		if (dialog == null)
			return ((refs.size() == 1) ? ((RefDataListElement) refs.get(0)).ref : null);
		
		//	display whatever is found in list dialog
		RefDataList refDataList = new RefDataList(dialog, ("Select Meta Data for Document" + ((docName == null) ? "" : (" " + docName))), refs);
		refDataList.setVisible(true);
		return refDataList.ref;
	}
	
	private Vector searchRefData(RefData query) {
		
		//	can we search?
		if (this.refBankClient == null)
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
		
		//	get identifiers
		String[] extIdTypes = query.getIdentifierTypes();
		String extIdType = (((extIdTypes == null) || (extIdTypes.length == 0)) ? null : extIdTypes[0]);
		String extId = ((extIdType == null) ? null : query.getIdentifier(extIdType));
		
		//	got something to search for?
		if ((extId == null) && (author == null) && (title == null) && (year == null) && (origin == null))
			return null;
		
		//	perform search
		Vector refs = new Vector();
		try {
			BibRefIterator brit = this.refBankClient.findRefs(null, author, title, ((year == null) ? -1 : Integer.parseInt(year)), origin, extId, extIdType, 0, false);
			while (brit.hasNextRef()) {
				BibRef ps = brit.getNextRef();
				String rs = ps.getRefParsed();
				if (rs == null)
					continue;
				try {
					refs.add(new RefDataListElement(BibRefUtils.modsXmlToRefData(SgmlDocumentReader.readDocument(new StringReader(rs)))));
				} catch (IOException ioe) { /* never gonna happen, but Java don't know ... */ }
			}
		} catch (IOException ioe) { /* let's not bother with exceptions for now, just return null ... */ }
		
		//	finally ...
		return refs;
	}
	
	private class RefDataList extends JDialog {
		private JList refList;
		RefData ref = new RefData();
		RefDataList(JDialog owner, String title, final Vector refData) {
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
			JButton cancel = new JButton("Cancel");
			cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					dispose();
				}
			});
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
			buttonPanel.add(ok);
			buttonPanel.add(cancel);
			
			this.getContentPane().setLayout(new BorderLayout());
			this.getContentPane().add(refListBox, BorderLayout.CENTER);
			this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
			this.setSize(600, 370);
			this.setLocationRelativeTo(owner);
		}
	}
	
	private class RefDataListElement {
		final RefData ref;
		final String displayString;
		RefDataListElement(RefData ref) {
			this.ref = ref;
			this.displayString = BibRefUtils.toRefString(this.ref);
		}
		public String toString() {
			return this.displayString;
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
		
		DocumentStyle.addProvider(new DocumentStyle.Provider() {
			public Properties getStyleFor(Attributed doc) {
//				Settings ds = Settings.loadSettings(new File("E:/GoldenGATEv3/Plugins/DocumentStyleManagerData/zootaxa.2007.journal_article.docStyle"));
				Settings ds = Settings.loadSettings(new File("E:/GoldenGATEv3/Plugins/DocumentStyleManagerData/zootaxa.0000.journal_article.docStyle"));
				return ds.toProperties();
			}
		});
		
		dmdep.metaDataAdder.process(doc, null, null, ProgressMonitor.dummy);
		String[] params = DocumentStyle.getParameterNames();
		Arrays.sort(params);
		for (int p = 0; p < params.length; p++)
			System.out.println(params[p] + " = \"" + DocumentStyle.getParameterValueClass(params[p]).getName() + "\";");
	}
}
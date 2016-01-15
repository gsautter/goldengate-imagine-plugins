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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.BufferedLineWriter;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.DocumentStyle;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.goldenGate.plugins.PluginDataProviderFileBased;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder.HtmlPageBuilderHost;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.gamta.ImDocumentRoot;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider;
import de.uka.ipd.idaho.im.imagine.web.plugins.WebDocumentViewer;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.im.util.ImfIO;
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
		
		//	provide actions only in the first couple of pages
		if ((idmp.document.getPageCount() < 15) && (start.pageId > 0))
			return null;
		if ((idmp.document.getPageCount() < 100) && (start.pageId > 1))
			return null;
		if (start.pageId > 2)
			return null;
		
		//	allow extraction only in single text streams
		if (!start.getTextStreamId().equals(end.getTextStreamId()))
			return null;
		
		//	get existing bibliographic metadata
		final RefData ref = BibRefUtils.modsAttributesToRefData(idmp.document);
		
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
		String sValue = this.sanitizeAttributeValue(type, value);
		if (sValue == null) {
			DialogFactory.alert(("'" + value + "' is not a valid value for document " + type), ("Invalid Document " + Character.toUpperCase(type.charAt(0)) + type.substring(1)), JOptionPane.ERROR_MESSAGE);
			return;
		}
		else value = sValue;
		
		//	set attribute
		if (isIdentifier)
			ref.setIdentifier(type, value.replaceAll("\\s", ""));
		else if (AUTHOR_ANNOTATION_TYPE.equals(type) || EDITOR_ANNOTATION_TYPE.equals(type))
			ref.addAttribute(type, value);
		else ref.setAttribute(type, value);
		
		//	store attributes
		BibRefUtils.toModsAttributes(ref, idmp.document);
		
		//	generate annotation type
		String annotType;
		if (isIdentifier)
			annotType = ("doc" + type);
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
		
		//	make changes show
		idmp.setAnnotationsPainted(annotType, true);
		
		//	finally ...
		idmp.endAtomicAction();
	}
	
	private String sanitizeAttributeValue(String type, String value) {
		
		//	normalize dashes and whitespace
		value = sanitizeString(value);
		
		//	check numeric attributes, translating Roman numbers in the process
		if (YEAR_ANNOTATION_TYPE.equals(type)) {
			if (StringUtils.isRomanNumber(value))
				value = ("" + StringUtils.parseRomanNumber(value));
			return (value.matches("[12][0-9]{3}") ? value : null);
		}
		else if (VOLUME_DESIGNATOR_ANNOTATION_TYPE.equals(type) || ISSUE_DESIGNATOR_ANNOTATION_TYPE.equals(type)) {
			if (StringUtils.isRomanNumber(value))
				value = ("" + StringUtils.parseRomanNumber(value));
			return (value.matches("[1-9][0-9]*") ? value : null);
		}
		else if (PAGINATION_ANNOTATION_TYPE.equals(type)) {
			if (value.indexOf('-') == -1) {
				if (StringUtils.isRomanNumber(value))
					value = ("" + StringUtils.parseRomanNumber(value));
				return (value.matches("[1-9][0-9]*") ? value : null);
			}
			else if (value.indexOf('-') == value.lastIndexOf('-')) {
				String[] valueParts = value.split("\\s*\\-\\s*");
				if (valueParts.length != 2)
					return null;
				String fpn = this.sanitizeAttributeValue(type, valueParts[0]);
				if (fpn == null)
					return null;
				String lpn = this.sanitizeAttributeValue(type, valueParts[1]);
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
			return flipNameParts(value);
		
		//	return normalized value for all other attributes
		else return value;
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
				protected void preProcess(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp) {
					
					//	get bibliographic data from document
					this.ref = getRefData(doc);
				}
				protected void postProcess(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp) {
					
					//	store bibliographic data if it's valid and not canceled
					if (this.ref != null)
						storeRefData(doc, this.ref);
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
						return new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("webView.html"), "UTF-8"));
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
		RefData ref = this.getRefData(doc);
		
		//	open meta data for editing if allowed to
		if (allowPrompt) {
			ref = this.editRefData(doc, ref);
			
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
		this.storeRefData(doc, ref);
	}
	
	private RefData getRefData(ImDocument doc) {
		
		//	put attributes from document in BibRef object
		RefData ref = BibRefUtils.modsAttributesToRefData(doc);
		
		//	use document style template if given
		DocumentStyle docStyle = DocumentStyle.getStyleFor(doc);
		DocumentStyle metaDataStyle = docStyle.getSubset("docMeta");
		int metaDataMaxPageId = metaDataStyle.getIntProperty("maxPageId", 0);
		ImPage[] pages = doc.getPages();
		for (int p = 0; (p <= metaDataMaxPageId) && (p < pages.length); p++) {
			int dpi = pages[p].getPageImage().currentDpi;
			if (this.extractAttribute(AUTHOR_ANNOTATION_TYPE, pages[p], dpi, metaDataStyle, true, ref)) {
				String[] authors = ref.getAttributeValues(AUTHOR_ANNOTATION_TYPE);
				for (int a = 0; a < authors.length; a++) {
					if (metaDataStyle.getBooleanProperty((AUTHOR_ANNOTATION_TYPE + ".isAllCaps"), false))
						authors[a] = sanitizeString(authors[a]);
					if (metaDataStyle.getBooleanProperty((AUTHOR_ANNOTATION_TYPE + ".isLastNameLast"), false))
						authors[a] = flipNameParts(authors[a]);
					if (a == 0)
						ref.setAttribute(AUTHOR_ANNOTATION_TYPE, authors[a]);
					else ref.addAttribute(AUTHOR_ANNOTATION_TYPE, authors[a]);
				}
			}
			this.extractAttribute(YEAR_ANNOTATION_TYPE, pages[p], dpi, metaDataStyle, false, ref);
			this.extractAttribute(TITLE_ANNOTATION_TYPE, pages[p], dpi, metaDataStyle, false, ref);
			
			this.extractAttribute(JOURNAL_NAME_ANNOTATION_TYPE, pages[p], dpi, metaDataStyle, false, ref);
			this.extractAttribute(VOLUME_DESIGNATOR_ANNOTATION_TYPE, pages[p], dpi, metaDataStyle, false, ref);
			this.extractAttribute(ISSUE_DESIGNATOR_ANNOTATION_TYPE, pages[p], dpi, metaDataStyle, false, ref);
			this.extractAttribute(NUMERO_DESIGNATOR_ANNOTATION_TYPE, pages[p], dpi, metaDataStyle, false, ref);
			
			this.extractAttribute(PUBLISHER_ANNOTATION_TYPE, pages[p], dpi, metaDataStyle, false, ref);
			this.extractAttribute(LOCATION_ANNOTATION_TYPE, pages[p], dpi, metaDataStyle, false, ref);
			
			this.extractAttribute(VOLUME_TITLE_ANNOTATION_TYPE, pages[p], dpi, metaDataStyle, false, ref);
			if (this.extractAttribute(EDITOR_ANNOTATION_TYPE, pages[p], dpi, metaDataStyle, true, ref)) {
				String[] editors = ref.getAttributeValues(EDITOR_ANNOTATION_TYPE);
				for (int e = 0; e < editors.length; e++) {
					if (metaDataStyle.getBooleanProperty((EDITOR_ANNOTATION_TYPE + ".isAllCaps"), false))
						editors[e] = sanitizeString(editors[e]);
					if (metaDataStyle.getBooleanProperty((EDITOR_ANNOTATION_TYPE + ".isLastNameLast"), false))
						editors[e] = flipNameParts(editors[e]);
					if (e == 0)
						ref.setAttribute(EDITOR_ANNOTATION_TYPE, editors[e]);
					else ref.addAttribute(EDITOR_ANNOTATION_TYPE, editors[e]);
				}
			}
			
			if (this.extractAttribute(PAGINATION_ANNOTATION_TYPE, pages[p], dpi, metaDataStyle, false, ref))
				ref.setAttribute(PAGINATION_ANNOTATION_TYPE, ref.getAttribute(PAGINATION_ANNOTATION_TYPE).trim().replaceAll("[^0-9]+", "-"));
			
			for (int t = 0; t < this.refIdTypes.length; t++) {
				if ((this.refIdTypes[t].length() != 0) && (ref.getIdentifier(this.refIdTypes[t]) == null) && this.extractAttribute(("ID-" + this.refIdTypes[t]), pages[p], dpi, metaDataStyle, false, ref))
					ref.setIdentifier(this.refIdTypes[t], ref.getIdentifier(this.refIdTypes[t]).replaceAll("\\s", ""));
			}
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
			String title = this.findTitle(doc);
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
	
	private void storeRefData(ImDocument doc, RefData ref) {
		
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
		this.annotateAttributeValues(ref, doc);
	}
	
	private String findTitle(ImDocument doc) {
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
	
	private boolean extractAttribute(String name, ImPage page, int dpi, DocumentStyle docStyle, boolean isMultiValue, RefData ref) {
		
		//	attribute already set
		if (ref.hasAttribute(name))
			return false;
		System.out.println("Extracting " + name + ":");
		
		//	get attribute specific parameters
		DocumentStyle attributeStyle = docStyle.getSubset(name);
		
		//	check for fixed value
		String fixedValue = attributeStyle.getProperty("fixedValue");
		if (fixedValue != null) {
			System.out.println(" ==> fixed to " + fixedValue);
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
				int wfs = Integer.parseInt((String) words[w].getAttribute(ImWord.FONT_SIZE_ATTRIBUTE, "-1"));
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
		
		//	order and concatenate words
		Arrays.sort(words, ImUtils.textStreamOrder);
		StringBuffer wordStringBuilder = new StringBuffer();
		for (int w = 0; w < words.length; w++) {
			wordStringBuilder.append(words[w].getString());
			if ((w+1) == words.length)
				break;
			if (words[w].getNextWord() != words[w+1])
				wordStringBuilder.append(" ");
			else if ((words[w].getNextRelation() == ImWord.NEXT_RELATION_SEPARATE) && Gamta.insertSpace(words[w].getString(), words[w+1].getString()))
				wordStringBuilder.append(" ");
			else if (words[w].getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
				wordStringBuilder.append(" ");
			else if ((words[w].getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED) && (wordStringBuilder.length() != 0))
				wordStringBuilder.deleteCharAt(wordStringBuilder.length()-1);
		}
		String wordString = wordStringBuilder.toString();
		System.out.println(" - word string is " + wordString);
		
		//	nothing further to extract, we're done
		if ((contextPattern == null) && (valuePattern == null)) {
			System.out.println(" ==> set to " + wordString);
			ref.setAttribute(name, wordString);
			return true;
		}
		
		//	normalize string for pattern matching
		StringBuffer nWordStringBuilder = new StringBuffer();
		for (int c = 0; c < wordString.length(); c++)
			nWordStringBuilder.append(StringUtils.getBaseChar(wordString.charAt(c)));
		String nWordString = nWordStringBuilder.toString();
		System.out.println(" - normalized word string is " + nWordString);
		
		//	use context narrowing pattern
		if (contextPattern != null) try {
			Matcher matcher = Pattern.compile(contextPattern).matcher(nWordString);
			if (matcher.find()) {
				wordString = wordString.substring(matcher.start(), matcher.end());
				nWordString = matcher.group();
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
			ref.setAttribute(name, wordString);
			return true;
		}
		
		//	extract attribute value(s) via pattern
		else try {
			Matcher matcher = Pattern.compile(valuePattern).matcher(nWordString);
			while (matcher.find()) {
				if (isMultiValue) {
					System.out.println(" ==> value pattern added " + wordString.substring(matcher.start(), matcher.end()));
					ref.addAttribute(name, wordString.substring(matcher.start(), matcher.end()));
				}
				else {
					System.out.println(" ==> value pattern set to " + wordString.substring(matcher.start(), matcher.end()));
					ref.setAttribute(name, wordString.substring(matcher.start(), matcher.end()));
					return true;
				}
			}
			if (ref.hasAttribute(name))
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
	
	private void setDocumentAttributes(ImDocument doc, RefData ref) {
		
		//	store reference data proper
		BibRefUtils.toModsAttributes(ref, doc);
		
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
		if (this.setAttribute(doc, DOCUMENT_AUTHOR_ATTRIBUTE, ref, AUTHOR_ANNOTATION_TYPE, false))
			spuriousDocAttributeNames.remove(DOCUMENT_AUTHOR_ATTRIBUTE);
		if (this.setAttribute(doc, DOCUMENT_TITLE_ATTRIBUTE, ref, TITLE_ANNOTATION_TYPE, true))
			spuriousDocAttributeNames.remove(DOCUMENT_TITLE_ATTRIBUTE);
		if (this.setAttribute(doc, DOCUMENT_DATE_ATTRIBUTE, ref, YEAR_ANNOTATION_TYPE, true))
			spuriousDocAttributeNames.remove(DOCUMENT_DATE_ATTRIBUTE);
		if (this.setAttribute(doc, DOCUMENT_SOURCE_LINK_ATTRIBUTE, ref, PUBLICATION_URL_ANNOTATION_TYPE, true))
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
	
	private boolean setAttribute(ImDocument doc, String docAttributeName, RefData ref, String refAttributeName, boolean onlyFirst) {
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
	
	private void annotateAttributeValues(RefData ref, ImDocument doc) {
		ImDocumentRoot wrappedDoc = new ImDocumentRoot(doc.getPage(0), (ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS | ImDocumentRoot.NORMALIZE_CHARACTERS));
		String[] attributeNames = ref.getAttributeNames();
		for (int a = 0; a < attributeNames.length; a++)
			this.annotateAttributeValues(attributeNames[a], ref, wrappedDoc);
	}
	
	private void annotateAttributeValues(String attributeName, RefData ref, ImDocumentRoot wrappedDoc) {
		
		//	index existing annotations
		String valueAnnotType = ("doc" + Character.toUpperCase(attributeName.charAt(0)) + attributeName.substring(1));
		TreeSet valueAnnotStrings = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		TreeMap valueStringsToAnnots = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		Annotation[] exValueAnnots = wrappedDoc.getAnnotations(valueAnnotType);
		for (int a = 0; a < exValueAnnots.length; a++) try {
			valueAnnotStrings.add(exValueAnnots[a].getValue());
			valueStringsToAnnots.put(exValueAnnots[a].getValue(), exValueAnnots[a]);
		} catch (RuntimeException re) { /* sometimes, the wrapper does funny things when headers have been cut off */ }
		
		//	create attribute value dictionary
		String[] values = ref.getAttributeValues(attributeName);
		StringVector valueDict = new StringVector(false);
		if (PAGINATION_ANNOTATION_TYPE.equals(attributeName) && (values.length == 1))
			valueDict.addElementIgnoreDuplicates(values[0].replaceAll("\\-", " - "));
		else valueDict.addContent(values);
		
		//	annotate attribute values
		Annotation[] valueAnnots = Gamta.extractAllContained(wrappedDoc, valueDict);
		for (int a = 0; a < valueAnnots.length; a++) {
			if (!valueAnnotStrings.add(valueAnnots[a].getValue())) {
				valueStringsToAnnots.remove(valueAnnots[a].getValue());
				continue;
			}
			valueAnnots[a].changeTypeTo(valueAnnotType);
			wrappedDoc.addAnnotation(valueAnnots[a]);
		}
		
		//	clean up spurious annotations
		for (Iterator vasit = valueStringsToAnnots.keySet().iterator(); vasit.hasNext();) {
			String valueAnnotString = ((String) vasit.next());
			Annotation valueAnnot = ((Annotation) valueStringsToAnnots.get(valueAnnotString));
			if (valueAnnot != null)
				wrappedDoc.removeAnnotation(valueAnnot);
		}
	}
	
	private RefData editRefData(final ImDocument doc, RefData ref) {
		final String docName = ((String) doc.getAttribute(DOCUMENT_NAME_ATTRIBUTE));
		final JDialog refEditDialog = DialogFactory.produceDialog(("Get Meta Data for Document" + ((docName == null) ? "" : (" " + docName))), true);
		final BibRefEditorPanel refEditorPanel = new BibRefEditorPanel(refTypeSystem, refIdTypes, ref);
		final boolean[] cancelled = {false};
		
		JButton extract = new JButton("Extract");
		extract.setToolTipText("Extract meta data from document");
		extract.addActionListener(new ActionListener() {
			QueriableAnnotation wrappedDoc = null;
			public void actionPerformed(ActionEvent ae) {
				if (this.wrappedDoc == null)
					this.wrappedDoc = new ImDocumentRoot(doc.getPage(0), ImDocumentRoot.NORMALIZATION_LEVEL_WORDS);
				RefData ref = refEditorPanel.getRefData();
				if (fillFromDocument(refEditDialog, docName, this.wrappedDoc, ref)) {
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
		refEditDialog.setSize(600, 500);
		refEditDialog.setLocationRelativeTo(DialogFactory.getTopWindow());
		refEditDialog.setVisible(true);
		
		return (cancelled[0] ? null : refEditorPanel.getRefData());
	}
	
	private boolean fillFromDocument(JDialog dialog, String docName, QueriableAnnotation doc, RefData ref) {
		DocumentView docView = new DocumentView(dialog, doc, ref);
		docView.setSize(900, 500);
		docView.setLocationRelativeTo(dialog);
		docView.setVisible(true);
		return docView.refModified;
	}
	
	private class DocumentView extends JDialog {
		private QueriableAnnotation doc;
		private int displayLength = 200;
		private RefData ref;
		private JTextArea textArea = new JTextArea();
		boolean refModified = false;
		DocumentView(JDialog owner, QueriableAnnotation doc, RefData rd) {
			super(owner, "Document View", true);
			this.doc = doc;
			this.ref = rd;
			
			this.textArea.setFont(new Font("Verdana", Font.PLAIN, 12));
			this.textArea.setLineWrap(true);
			this.textArea.setWrapStyleWord(true);
			this.textArea.setText(TokenSequenceUtils.concatTokens(this.doc, 0, this.displayLength, false, false));
			JScrollPane xmlAreaBox = new JScrollPane(this.textArea);
			
			JPanel extractButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
			JButton more = new JButton("More ...");
			more.setToolTipText("Show more document text");
			more.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					DocumentView.this.displayLength += 100;
					DocumentView.this.textArea.setText(TokenSequenceUtils.concatTokens(DocumentView.this.doc, 0, DocumentView.this.displayLength, false, false));
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
				String value = textArea.getSelectedText().trim();
				if (value.length() != 0) {
					value = sanitizeString(value);
					if (AUTHOR_ANNOTATION_TYPE.equals(this.attribute) || EDITOR_ANNOTATION_TYPE.equals(this.attribute))
						ref.addAttribute(this.attribute, value);
					else ref.setAttribute(this.attribute, value);
					refModified = true;
					this.setToolTipText(this.getText() + ": " + ref.getAttributeValueString(this.attribute, " & "));
					this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createLineBorder(Color.GREEN)));
				}
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
	
	private static final String sanitizeString(String str) {
		
		//	check all-caps
		boolean allCaps = true;
		for (int c = 0; c < str.length(); c++) {
			char ch = str.charAt(c);
			if (ch != Character.toUpperCase(ch)) {
				allCaps = false;
				break;
			}
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
				if (allCaps && Character.isLetter(lCh))
					sStr.append(Character.toLowerCase(ch));
				else sStr.append(ch);
			}
			else if ("-\u00AD\u2010\u2011\u2012\u2013\u2014\u2015\u2212".indexOf(ch) != -1)
				sStr.append('-');
			else sStr.append(ch);
			lCh = ch;
		}
		
		//	finally
		return sStr.toString();
	}
	
	private static final String flipNameParts(String name) {
		name = name.trim();
		if (name.indexOf(',') != -1)
			return name;
		int split = name.lastIndexOf(' ');
		if (split == -1)
			return name;
		String firstName = name.substring(0, split).trim();
		String lastName = name.substring(split + 1).trim();
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
		ImDocument doc = ImfIO.loadDocument(docIn);
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
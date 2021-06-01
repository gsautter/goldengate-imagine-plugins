///*
// * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
// * All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without
// * modification, are permitted provided that the following conditions are met:
// *
// *     * Redistributions of source code must retain the above copyright
// *       notice, this list of conditions and the following disclaimer.
// *     * Redistributions in binary form must reproduce the above copyright
// *       notice, this list of conditions and the following disclaimer in the
// *       documentation and/or other materials provided with the distribution.
// *     * Neither the name of the Universitaet Karlsruhe (TH) / KIT nor the
// *       names of its contributors may be used to endorse or promote products
// *       derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY UNIVERSITAET KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
// * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
// * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
// * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//package de.uka.ipd.idaho.im.imagine.plugins.doc;
//
//import java.awt.BasicStroke;
//import java.awt.BorderLayout;
//import java.awt.Color;
//import java.awt.Component;
//import java.awt.Dimension;
//import java.awt.EventQueue;
//import java.awt.Font;
//import java.awt.GridLayout;
//import java.awt.Point;
//import java.awt.Shape;
//import java.awt.Window;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.event.FocusEvent;
//import java.awt.event.FocusListener;
//import java.awt.event.ItemEvent;
//import java.awt.event.ItemListener;
//import java.awt.event.WindowAdapter;
//import java.awt.event.WindowEvent;
//import java.awt.geom.Rectangle2D;
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.OutputStreamWriter;
//import java.io.Reader;
//import java.io.Writer;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.Map;
//import java.util.Properties;
//import java.util.TreeMap;
//import java.util.TreeSet;
//import java.util.regex.Pattern;
//import java.util.regex.PatternSyntaxException;
//
//import javax.imageio.ImageIO;
//import javax.swing.BorderFactory;
//import javax.swing.BoxLayout;
//import javax.swing.Icon;
//import javax.swing.ImageIcon;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
//import javax.swing.JComboBox;
//import javax.swing.JComponent;
//import javax.swing.JDialog;
//import javax.swing.JFrame;
//import javax.swing.JLabel;
//import javax.swing.JMenu;
//import javax.swing.JMenuItem;
//import javax.swing.JOptionPane;
//import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//import javax.swing.JSplitPane;
//import javax.swing.JTextArea;
//import javax.swing.JTextField;
//import javax.swing.JTree;
//import javax.swing.ToolTipManager;
//import javax.swing.UIManager;
//import javax.swing.event.DocumentEvent;
//import javax.swing.event.DocumentListener;
//import javax.swing.event.TreeModelEvent;
//import javax.swing.event.TreeModelListener;
//import javax.swing.event.TreeSelectionEvent;
//import javax.swing.event.TreeSelectionListener;
//import javax.swing.text.JTextComponent;
//import javax.swing.tree.DefaultTreeCellRenderer;
//import javax.swing.tree.DefaultTreeSelectionModel;
//import javax.swing.tree.TreeModel;
//import javax.swing.tree.TreeNode;
//import javax.swing.tree.TreePath;
//import javax.swing.tree.TreeSelectionModel;
//
//import de.uka.ipd.idaho.easyIO.settings.Settings;
//import de.uka.ipd.idaho.gamta.Annotation;
//import de.uka.ipd.idaho.gamta.AnnotationUtils;
//import de.uka.ipd.idaho.gamta.Gamta;
//import de.uka.ipd.idaho.gamta.Tokenizer;
//import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
//import de.uka.ipd.idaho.gamta.util.imaging.DocumentStyle;
//import de.uka.ipd.idaho.gamta.util.imaging.DocumentStyle.ParameterDescription;
//import de.uka.ipd.idaho.gamta.util.imaging.DocumentStyle.ParameterGroupDescription;
//import de.uka.ipd.idaho.gamta.util.imaging.DocumentStyle.TestableElement;
//import de.uka.ipd.idaho.gamta.util.swing.AnnotationDisplayDialog;
//import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
//import de.uka.ipd.idaho.htmlXmlUtil.Parser;
//import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
//import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
//import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
//import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;
//import de.uka.ipd.idaho.im.ImDocument;
//import de.uka.ipd.idaho.im.ImPage;
//import de.uka.ipd.idaho.im.ImRegion;
//import de.uka.ipd.idaho.im.ImWord;
//import de.uka.ipd.idaho.im.gamta.ImDocumentRoot;
//import de.uka.ipd.idaho.im.gamta.ImTokenSequence;
//import de.uka.ipd.idaho.im.imagine.GoldenGateImagine;
//import de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider;
//import de.uka.ipd.idaho.im.imagine.plugins.DisplayExtensionProvider;
//import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
//import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.DisplayExtension;
//import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.DisplayExtensionGraphics;
//import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
//import de.uka.ipd.idaho.im.util.ImUtils;
//import de.uka.ipd.idaho.im.util.LinePattern;
//import de.uka.ipd.idaho.plugins.bibRefs.BibRefTypeSystem;
//import de.uka.ipd.idaho.stringUtils.StringUtils;
//
///**
// * This plug-in manages document style parameter lists and helps users with
// * creating and editing them.
// * 
// * @author sautter
// */
//public class DocumentStyleManager extends AbstractSelectionActionProvider implements DisplayExtensionProvider {
//	private static final ParameterGroupDescription anchorRootDescription = new ParameterGroupDescription("anchor");
//	static {
//		anchorRootDescription.setLabel("Anchors");
//		anchorRootDescription.setDescription("Anchors automate the assignment of document styles to individual documents. In particular, anchors match on distinctive landmark features on the first few pages of documents, e.g. a journal name in a specific position and font size.");
//		anchorRootDescription.setParamLabel("maxPageId", "Maximum Pages After First");
//		anchorRootDescription.setParamDescription("maxPageId", "The maximum number of pages to serach for anchor targets after the very first page.");
//	}
//	private static final ParameterGroupDescription anchorDescription = new ParameterGroupDescription("anchor");
//	static {
//		anchorDescription.setLabel("Anchors");
//		anchorDescription.setDescription("Anchors automate the assignment of document styles to individual documents. In particular, anchors match on distinctive landmark features on the very first few pages of documents, e.g. a journal name in a specific position and font size.");
//		anchorDescription.setParamLabel("minFontSize", "Minimum Font Size");
//		anchorDescription.setParamDescription("minFontSize", "The minimum font size of the anchor target (use only if variation present or to be expected, otherwise use exact font size)");
//		anchorDescription.setParamLabel("maxFontSize", "Maximum Font Size");
//		anchorDescription.setParamDescription("maxFontSize", "The maximum font size of the anchor target (use only if variation present or to be expected, otherwise use exact font size)");
//		anchorDescription.setParamLabel("fontSize", "Exact Font Size");
//		anchorDescription.setParamDescription("fontSize", "The exact font size of the anchor target (use minimum and maximum font size if variation present or to be expected)");
//		anchorDescription.setParamLabel("isBold", "Is the Anchor Target Bold?");
//		anchorDescription.setParamDescription("isBold", "Is the anchor target set in bold face?");
//		anchorDescription.setParamLabel("isItalics", "Is the Anchor Target in Italics?");
//		anchorDescription.setParamDescription("isItalics", "Is the anchor target set in italics?");
//		anchorDescription.setParamLabel("isAllCaps", "Is the Anchor Target in All-Caps?");
//		anchorDescription.setParamDescription("isAllCaps", "Is the anchor target set in all-caps?");
//		anchorDescription.setParamLabel("pattern", "Pattern Matching Anchor Target");
//		anchorDescription.setParamDescription("pattern", "A pattern matching the anchor target; should be as restrictive as possible to avoid ambiguity.");
//		anchorDescription.setParamRequired("pattern");
//		anchorDescription.setParamLabel("area", "Anchor Target Area");
//		anchorDescription.setParamDescription("area", "A bounding box locating the anchor target; should be as precise as possible to avoid ambiguity.");
//		anchorDescription.setParamRequired("area");
//	}
//	
//	private GoldenGateImagine ggImagine;
//	private Settings parameterValueClassNames;
//	private Map parameterValueClasses = Collections.synchronizedMap(new HashMap());
//	private BibRefTypeSystem refTypeSystem;
//	private DocumentStyleProvider styleProvider;
//	
//	/** zero-argument constructor for class loading */
//	public DocumentStyleManager() {}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
//	 */
//	public String getPluginName() {
//		return "IM Document Style Manager";
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
//	 */
//	public String getResourceTypeLabel() {
//		return "Document Style";
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
//	 */
//	public void init() {
//		
//		//	connect to document style provider for storage
//		this.styleProvider = ((DocumentStyleProvider) this.parent.getResourceProvider(DocumentStyleProvider.class.getName()));
//		
//		//	get reference type system for publication types
//		this.refTypeSystem = BibRefTypeSystem.getDefaultInstance();
//		
//		//	read existing style parameters
//		try {
//			Reader spr = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream("styleParameters.cnfg"), "UTF-8"));
//			this.parameterValueClassNames = Settings.loadSettings(spr);
//			spr.close();
//			String[] spns = this.parameterValueClassNames.getKeys();
//			for (int p = 0; p < spns.length; p++) try {
//				this.parameterValueClasses.put(spns[p], Class.forName(this.parameterValueClassNames.getSetting(spns[p])));
//			} catch (Exception e) {}
//		}
//		catch (IOException ioe) {
//			ioe.printStackTrace(System.out);
//		}
//		
//		//	read and hash available parameter group descriptions
//		String[] dataNames = this.dataProvider.getDataNames();
//		for (int d = 0; d < dataNames.length; d++) {
//			if (dataNames[d].endsWith(".pgd.xml"))
//				this.loadParameterGroupDescription(dataNames[d].substring(0, (dataNames[d].length() - ".pgd.xml".length())));
//		}
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#exit()
//	 */
//	public void exit() {
//		
//		//	close style editor if open
//		if (this.docStyleEditor != null)
//			this.docStyleEditor.dispose();
//		
//		//	store all (known) style parameters and their value classes
//		String[] params = DocumentStyle.getParameterNames();
//		Arrays.sort(params);
//		boolean paramsDirty = false;
//		for (int p = 0; p < params.length; p++) {
//			Class paramClass = DocumentStyle.getParameterValueClass(params[p]);
//			Class eParamClass = ((Class) this.parameterValueClasses.get(params[p]));
//			if ((eParamClass == null) || !paramClass.getName().equals(eParamClass.getName())) {
//				this.parameterValueClassNames.setSetting(params[p], paramClass.getName());
//				paramsDirty = true;
//			}
//		}
//		if (paramsDirty) try {
//			Writer spw = new BufferedWriter(new OutputStreamWriter(this.dataProvider.getOutputStream("styleParameters.cnfg"), "UTF-8"));
//			this.parameterValueClassNames.storeAsText(spw);
//			spw.close();
//		}
//		catch (IOException ioe) {
//			ioe.printStackTrace(System.out);
//		}
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.im.imagine.plugins.DisplayExtensionProvider#getDisplayExtensions()
//	 */
//	public DisplayExtension[] getDisplayExtensions() {
//		return ((this.docStyleEditor == null) ? NO_DISPLAY_EXTENSIONS : this.docStyleEditorDisplayExtension);
//	}
//	
//	private ParameterGroupDescription getParameterGroupDescription(String pnp) {
//		if (pnp.equals("anchor"))
//			return anchorRootDescription;
//		if (pnp.startsWith("anchor."))
//			return anchorDescription;
//		ParameterGroupDescription pgd = DocumentStyle.getParameterGroupDescription(pnp);
//		if (pgd == null)
//			return this.loadParameterGroupDescription(pnp);
//		else {
//			this.storeParameterGroupDescription(pgd);
//			return pgd;
//		}
//	}
//	
//	private static final Grammar xmlGrammar = new StandardGrammar();
//	private static final Parser xmlParser = new Parser(xmlGrammar);
//	private HashMap paramGroupDescriptionsByName = new HashMap();
//	private HashSet paramGroupDescriptionsStored = new HashSet();
//	private HashMap paramGroupDescriptionHashes = new HashMap();
//	
//	private ParameterGroupDescription loadParameterGroupDescription(String pnp) {
//		
//		//	check cache first
//		if (this.paramGroupDescriptionsByName.containsKey(pnp))
//			return ((ParameterGroupDescription) this.paramGroupDescriptionsByName.get(pnp));
//		
//		//	resort to previously persisted parameter group description
//		if (this.dataProvider.isDataAvailable(pnp + ".pgd.xml")) try {
//			final StringBuffer pgdSb = new StringBuffer();
//			BufferedReader pgdBr = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream(pnp + ".pgd.xml"), "UTF-8") {
//				public int read() throws IOException {
//					int r = super.read();
//					if (r != -1)
//						pgdSb.append((char) r);
//					return r;
//				}
//				public int read(char[] cbuf, int offset, int length) throws IOException {
//					int r = super.read(cbuf, offset, length);
//					if (r != -1)
//						pgdSb.append(cbuf, offset, r);
//					return r;
//				}
//			});
//			final ParameterGroupDescription pgd = new ParameterGroupDescription(pnp);
//			xmlParser.parse(pgdBr, new TokenReceiver() {
//				private ParameterDescription pd = null;
//				private ArrayList pvs = null;
//				public void storeToken(String token, int treeDepth) throws IOException {
//					if (!xmlGrammar.isTag(token))
//						return;
//					String type = xmlGrammar.getType(token);
//					if ("paramGroup".equals(type)) {
//						if (!xmlGrammar.isEndTag(token)) {
//							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, xmlGrammar);
//							pgd.setLabel(tnas.getAttribute("label", ""));
//							pgd.setDescription(tnas.getAttribute("description", ""));
//						}
//					}
//					else if ("param".equals(type)) {
//						if (xmlGrammar.isEndTag(token)) {
//							this.pd.setValues((String[]) this.pvs.toArray(new String[this.pvs.size()]));
//							this.pvs = null;
//							this.pd = null;
//						}
//						else {
//							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, xmlGrammar);
//							String pn = tnas.getAttribute("name");
//							pgd.setParamLabel(pn, tnas.getAttribute("label", ""));
//							pgd.setParamDescription(pn, tnas.getAttribute("description", ""));
//							pgd.setParamDefaultValue(pn, tnas.getAttribute("default"));
//							if ("true".equals(tnas.getAttribute("required", "false")))
//								pgd.setParamRequired(pn);
//							this.readDependencyAttribute(pgd.getParameterDescription(pn), null, tnas.getAttribute("requires"), true);
//							this.readDependencyAttribute(pgd.getParameterDescription(pn), null, tnas.getAttribute("excludes"), false);
//							if (!xmlGrammar.isSingularTag(token)) {
//								this.pd = pgd.getParameterDescription(pn);
//								this.pvs = new ArrayList(2);
//							}
//						}
//					}
//					else if ("value".equals(type)) {
//						TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, xmlGrammar);
//						String pvn = tnas.getAttribute("name");
//						this.pvs.add(pvn);
//						String pvl = tnas.getAttribute("label");
//						this.readDependencyAttribute(this.pd, pvn, tnas.getAttribute("requires"), true);
//						this.readDependencyAttribute(this.pd, pvn, tnas.getAttribute("excludes"), false);
//						if (pvl != null)
//							this.pd.setValueLabel(pvn, pvl);
//					}
//				}
//				public void close() throws IOException {}
//				private void readDependencyAttribute(ParameterDescription pd, String value, String names, boolean isRequired) {
//					if (names == null)
//						return;
//					String[] ns = names.split("\\,");
//					for (int n = 0; n < ns.length; n++) {
//						if (isRequired)
//							pd.addRequiredParameter(value, ns[n]);
//						else pd.addExcludedParameter(value, ns[n]);
//					}
//				}
//			});
//			pgdBr.close();
//			
//			//	cache parameter group description for reuse
//			this.paramGroupDescriptionsByName.put(pnp, pgd);
//			
//			//	remember hash of what we read
//			this.paramGroupDescriptionHashes.put(pnp, Integer.valueOf(computeHashCode(pgdSb)));
//			
//			//	finally ...
//			return pgd;
//		}
//		catch (IOException ioe) {
//			ioe.printStackTrace(System.out);
//		}
//		
//		//	little we can do about this one ...
//		return null;
//	}
//	
//	private void storeParameterGroupDescription(ParameterGroupDescription pgd) {
//		
//		//	check if we saved this one before in current life cycle
//		if (this.paramGroupDescriptionsStored.contains(pgd.parameterNamePrefix))
//			return;
//		
//		//	little we can do about this one ...
//		if (!this.dataProvider.isDataEditable(pgd.parameterNamePrefix + ".pgd.xml"))
//			return;
//		
//		//	get parameter names
//		String[] pgdPns = pgd.getParameterNames();
//		
//		//	write XML to buffer
//		StringBuffer pgdSb = new StringBuffer();
//		pgdSb.append("<paramGroup");
//		pgdSb.append(" name=\"" + pgd.parameterNamePrefix + "\"");
//		if (pgd.getLabel() != null)
//			pgdSb.append(" label=\"" + xmlGrammar.escape(pgd.getLabel()) + "\"");
//		if (pgd.getDescription() != null)
//			pgdSb.append(" description=\"" + xmlGrammar.escape(pgd.getDescription()) + "\"");
//		if (pgdPns.length == 0)
//			pgdSb.append("/>\r\n");
//		else {
//			pgdSb.append(">\r\n");
//			for (int p = 0; p < pgdPns.length; p++) {
//				ParameterDescription pd = pgd.getParameterDescription(pgdPns[p]);
//				if (pd == null)
//					continue;
//				pgdSb.append("  <param");
//				pgdSb.append(" name=\"" + pgdPns[p] + "\"");
//				if (pd.getLabel() != null)
//					pgdSb.append(" label=\"" + xmlGrammar.escape(pd.getLabel()) + "\"");
//				if (pd.getDescription() != null)
//					pgdSb.append(" description=\"" + xmlGrammar.escape(pd.getDescription()) + "\"");
//				if (pd.getDefaultValue() != null)
//					pgdSb.append(" default=\"" + xmlGrammar.escape(pd.getDefaultValue()) + "\"");
//				if (pd.isRequired())
//					pgdSb.append(" required=\"true\"");
//				appendParameterNameListAttribute(pgdSb, "requires", pd.getRequiredParameters());
//				appendParameterNameListAttribute(pgdSb, "excludes", pd.getExcludedParameters());
//				String[] pvs = pd.getValues();
//				if ((pvs == null) || (pvs.length == 0))
//					pgdSb.append("/>\r\n");
//				else {
//					pgdSb.append(">\r\n");
//					for (int v = 0; v < pvs.length; v++) {
//						pgdSb.append("    <value");
//						pgdSb.append(" name=\"" + xmlGrammar.escape(pvs[v]) + "\"");
//						if (pd.getValueLabel(pvs[v]) != null)
//							pgdSb.append(" label=\"" + xmlGrammar.escape(pd.getValueLabel(pvs[v])) + "\"");
//						appendParameterNameListAttribute(pgdSb, "requires", pd.getRequiredParameters(pvs[v]));
//						appendParameterNameListAttribute(pgdSb, "excludes", pd.getExcludedParameters(pvs[v]));
//						pgdSb.append("/>\r\n");
//					}
//					pgdSb.append("  </param>\r\n");
//				}
//			}
//			pgdSb.append("</paramGroup>\r\n");
//		}
//		
//		//	check for changes via hash
//		Integer pgdHash = Integer.valueOf(computeHashCode(pgdSb));
//		if (this.paramGroupDescriptionHashes.containsKey(pgd.parameterNamePrefix) && this.paramGroupDescriptionHashes.get(pgd.parameterNamePrefix).equals(pgdHash))
//			return;
//		
//		//	persist any changes
//		try {
//			
//			//	write data
//			BufferedWriter pgdBw = new BufferedWriter(new OutputStreamWriter(this.dataProvider.getOutputStream(pgd.parameterNamePrefix + ".pgd.xml"), "UTF-8"));
//			pgdBw.write(pgdSb.toString());
//			pgdBw.flush();
//			pgdBw.close();
//			
//			//	remember data written ...
//			this.paramGroupDescriptionsStored.add(pgd.parameterNamePrefix);
//			
//			//	... as well as current status hash
//			this.paramGroupDescriptionHashes.put(pgd.parameterNamePrefix, pgdHash);
//		}
//		catch (IOException ioe) {
//			ioe.printStackTrace(System.out);
//		}
//	}
//	
//	private static void appendParameterNameListAttribute(StringBuffer pgdSb, String name, String[] pns) {
//		if (pns == null)
//			return;
//		pgdSb.append(" " + name + "=\"");
//		for (int n = 0; n < pns.length; n++) {
//			if (n != 0)
//				pgdSb.append(",");
//			pgdSb.append(xmlGrammar.escape(pns[n]));
//		}
//		pgdSb.append("\"");
//	}
//	
//	//	courtesy of java.lang.String
//	private static int computeHashCode(CharSequence chars) {
//		if (chars.length() == 0)
//			return 0;
//		int h = 0;
//		for (int c = 0; c < chars.length(); c++)
//			h = 31*h + chars.charAt(c);
//		return h;
//	}
//	
//	private boolean checkParamValueClass(String docStyleParamName, Class cls, boolean includeArray) {
//		Class paramValueClass = ((Class) this.parameterValueClasses.get(docStyleParamName));
//		if (paramValueClass != null) {
//			if (paramValueClass.getName().equals(cls.getName()))
//				return true;
//			else if (includeArray && DocumentStyle.getListElementClass(paramValueClass).getName().equals(cls.getName()))
//				return true;
//		}
//		if (docStyleParamName.startsWith("anchor.")) {
//			if (docStyleParamName.endsWith(".minFontSize") || docStyleParamName.endsWith(".maxFontSize") || docStyleParamName.endsWith(".fontSize"))
//				return Integer.class.getName().equals(cls.getName());
//			else if (docStyleParamName.endsWith(".isBold") || docStyleParamName.endsWith(".isItalics") || docStyleParamName.endsWith(".isAllCaps"))
//				return Boolean.class.getName().equals(cls.getName());
//			else if (docStyleParamName.endsWith(".pattern"))
//				return String.class.getName().equals(cls.getName());
//			else if (docStyleParamName.endsWith(".area"))
//				return BoundingBox.class.getName().equals(cls.getName());
//		}
//		return false;
//	}
//	
//	private boolean hasFixedValueList(String docStyleParamName) {
//		String pgn = docStyleParamName.substring(0, docStyleParamName.lastIndexOf('.'));
//		ParameterGroupDescription pgd = this.getParameterGroupDescription(pgn);
//		if (pgd == null)
//			return false;
//		String pn = docStyleParamName.substring(docStyleParamName.lastIndexOf('.') + ".".length());
//		return (pgd.getParamValues(pn) != null);
//	}
//	
//	private Class getParamValueClass(String docStyleParamName) {
//		Class paramValueClass = ((Class) this.parameterValueClasses.get(docStyleParamName));
//		if (paramValueClass != null)
//			return paramValueClass;
//		if (docStyleParamName.startsWith("anchor.")) {
//			if (docStyleParamName.endsWith(".minFontSize") || docStyleParamName.endsWith(".maxFontSize") || docStyleParamName.endsWith(".fontSize"))
//				return Integer.class;
//			else if (docStyleParamName.endsWith(".isBold") || docStyleParamName.endsWith(".isItalics") || docStyleParamName.endsWith(".isAllCaps"))
//				return Boolean.class;
//			else if (docStyleParamName.endsWith(".pattern"))
//				return String.class;
//			else if (docStyleParamName.endsWith(".area"))
//				return BoundingBox.class;
//		}
//		return String.class;
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImaginePlugin#setImagineParent(de.uka.ipd.idaho.im.imagine.GoldenGateImagine)
//	 */
//	public void setImagineParent(GoldenGateImagine ggImagine) {
//		this.ggImagine = ggImagine; // we need this to issue display extension change notifications
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImaginePlugin#initImagine()
//	 */
//	public void initImagine() { /* nothing to initialize */ }
////	
////	/* (non-Javadoc)
////	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentOpened(de.uka.ipd.idaho.im.ImDocument, java.lang.Object, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
////	 */
////	public void documentOpened(ImDocument doc, Object source, ProgressMonitor pm) { /* we only react to documents being closed */ }
////	
////	/* (non-Javadoc)
////	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentSelected(de.uka.ipd.idaho.im.ImDocument)
////	 */
////	public void documentSelected(ImDocument doc) { /* we only react to documents being closed */ }
////	TODO do change document style in editor if document selected that matches other style
////	/* (non-Javadoc)
////	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentSaving(de.uka.ipd.idaho.im.ImDocument, java.lang.Object, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
////	 */
////	public void documentSaving(ImDocument doc, Object dest, ProgressMonitor pm) { /* we only react to documents being closed */ }
////
////	/* (non-Javadoc)
////	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentSaved(de.uka.ipd.idaho.im.ImDocument, java.lang.Object, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
////	 */
////	public void documentSaved(ImDocument doc, Object dest, ProgressMonitor pm) { /* we only react to documents being closed */ }
////	
////	/* (non-Javadoc)
////	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentClosed(java.lang.String)
////	 */
////	public void documentClosed(String docId) { /* we only react to documents being closed */ }
////	
////	private Map docStylesByDocId = Collections.synchronizedMap(new HashMap());
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
//	 */
//	public SelectionAction[] getActions(final ImWord start, final ImWord end, final ImDocumentMarkupPanel idmp) {
//		
//		//	cross page selection, unlikely a style edit
//		if (start.pageId != end.pageId)
//			return null;
//		
//		//	get document style name and style
//		String docStyleName = ((String) idmp.document.getAttribute(DocumentStyle.DOCUMENT_STYLE_NAME_ATTRIBUTE));
//		
//		//	no document style assigned
//		if (docStyleName == null) {
//			
//			//	no document style editing, offer adding or creating one
//			if ((this.docStyleEditor == null) || (this.docStyleEditor.docStyleName == null))
//				return this.getAssignDocStyleAction(idmp.document);
//			
//			//	use editing document style
//			else docStyleName = this.docStyleEditor.docStyleName;
//		}
//		
//		//	style already assigned, offer extending or modifying it
//		final Settings docStyle;
//		if (this.docStyleEditor == null)
//			docStyle = this.getDocStyle(docStyleName);
//		else if (docStyleName.equals(this.docStyleEditor.docStyleName))
//			docStyle = this.docStyleEditor.docStyle;
//		else {
//			docStyle = this.getDocStyle(docStyleName);
//			this.docStyleEditor.setDocStyle(idmp.document, docStyleName, docStyle);
//		}
//		
//		//	assess font style and size, and collect word string
//		boolean isBold = true;
//		boolean isItalics = true;
//		boolean isAllCaps = true;
//		boolean hasCaps = false;
//		int minFontSize = 72;
//		int maxFontSize = 0;
//		final String fWordString;
//		int left = Integer.MAX_VALUE;
//		int right = 0;
//		int top = Integer.MAX_VALUE;
//		int bottom = 0;
//		
//		//	single text stream, assess word sequence
//		if (start.getTextStreamId().equals(end.getTextStreamId())) {
//			
//			//	make sure start does not lie after end (would run for loop to end of text stream)
//			if (ImUtils.textStreamOrder.compare(start, end) > 0)
//				return this.getActions(end, start, idmp);
//			
//			//	assess single word
//			if (start == end) {
//				isBold = (isBold && start.hasAttribute(ImWord.BOLD_ATTRIBUTE));
//				isItalics = (isItalics && start.hasAttribute(ImWord.ITALICS_ATTRIBUTE));
//				isAllCaps = (isAllCaps && start.getString().equals(start.getString().toUpperCase()));
//				hasCaps = (hasCaps || !start.getString().equals(start.getString().toLowerCase()));
//				try {
//					int fs = start.getFontSize();
//					minFontSize = Math.min(minFontSize, fs);
//					maxFontSize = Math.max(maxFontSize, fs);
//				} catch (RuntimeException re) {}
//				fWordString = start.getString();
//				left = Math.min(left, start.bounds.left);
//				right = Math.max(right, start.bounds.right);
//				top = Math.min(top, start.bounds.top);
//				bottom = Math.max(bottom, start.bounds.bottom);
//			}
//			
//			//	assess word sequence
//			else {
//				for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
//					isBold = (isBold && imw.hasAttribute(ImWord.BOLD_ATTRIBUTE));
//					isItalics = (isItalics && imw.hasAttribute(ImWord.ITALICS_ATTRIBUTE));
//					isAllCaps = (isAllCaps && imw.getString().equals(imw.getString().toUpperCase()));
//					hasCaps = (hasCaps || !imw.getString().equals(imw.getString().toLowerCase()));
//					try {
//						int fs = imw.getFontSize();
//						minFontSize = Math.min(minFontSize, fs);
//						maxFontSize = Math.max(maxFontSize, fs);
//					} catch (RuntimeException re) {}
//					left = Math.min(left, imw.bounds.left);
//					right = Math.max(right, imw.bounds.right);
//					top = Math.min(top, imw.bounds.top);
//					bottom = Math.max(bottom, imw.bounds.bottom);
//					if (imw == end)
//						break;
//				}
//				
//				//	get word string (allowing more words on first couple of pages for anchors and metadata extraction)
////				fWordString = (((end.getTextStreamPos() - start.getTextStreamPos()) < ((start.pageId == 0) ? 50 : 15)) ? ImUtils.getString(start, end, true) : null);
//				fWordString = (((end.getTextStreamPos() - start.getTextStreamPos()) < (((start.pageId - idmp.document.getFirstPageId()) < 4) ? 50 : 15)) ? ImUtils.getString(start, end, true) : null);
//			}
//		}
//		
//		//	different text streams, only use argument words proper
//		else {
//			isBold = (isBold && start.hasAttribute(ImWord.BOLD_ATTRIBUTE));
//			isItalics = (isItalics && start.hasAttribute(ImWord.ITALICS_ATTRIBUTE));
//			isAllCaps = (isAllCaps && start.getString().equals(start.getString().toUpperCase()));
//			hasCaps = (hasCaps || !start.getString().equals(start.getString().toLowerCase()));
//			try {
//				int fs = start.getFontSize();
//				minFontSize = Math.min(minFontSize, fs);
//				maxFontSize = Math.max(maxFontSize, fs);
//			} catch (RuntimeException re) {}
//			left = Math.min(left, start.bounds.left);
//			right = Math.max(right, start.bounds.right);
//			top = Math.min(top, start.bounds.top);
//			bottom = Math.max(bottom, start.bounds.bottom);
//			
//			isBold = (isBold && end.hasAttribute(ImWord.BOLD_ATTRIBUTE));
//			isItalics = (isItalics && end.hasAttribute(ImWord.ITALICS_ATTRIBUTE));
//			isAllCaps = (isAllCaps && end.getString().equals(end.getString().toUpperCase()));
//			hasCaps = (hasCaps || !end.getString().equals(end.getString().toLowerCase()));
//			try {
//				int fs = end.getFontSize();
//				minFontSize = Math.min(minFontSize, fs);
//				maxFontSize = Math.max(maxFontSize, fs);
//			} catch (RuntimeException re) {}
//			left = Math.min(left, end.bounds.left);
//			right = Math.max(right, end.bounds.right);
//			top = Math.min(top, end.bounds.top);
//			bottom = Math.max(bottom, end.bounds.bottom);
//			
//			fWordString = (start.getString() + " " + end.getString());
//		}
//		
//		//	measure margins
//		int horiMargin = (end.bounds.left - start.bounds.right);
//		int vertMargin = (end.bounds.top - start.bounds.bottom);
//		
//		//	fix parameter values, scaling bounds and margins to default 72 DPI
//		final boolean fIsBold = isBold;
//		final boolean fIsItalics = isItalics;
//		final boolean fIsAllCaps = (isAllCaps && hasCaps);
//		final int fMinFontSize = minFontSize;
//		final int fMaxFontSize = maxFontSize;
//		int pageDpi = idmp.document.getPage(start.pageId).getImageDPI();
//		/* cut word based bounding boxes a little slack, adding some pixels in
//		 * each direction, maybe (DPI / 12), a.k.a. some 2 millimeters, to help
//		 * with slight word placement variations */
//		final BoundingBox fWordBounds = DocumentStyle.scaleBox(new BoundingBox((left - (pageDpi / 12)), (right + (pageDpi / 12)), (top - (pageDpi / 12)), (bottom + (pageDpi / 12))), pageDpi, 72, 'O');
//		final int fHoriMargin = ((horiMargin < 0) ? 0 : DocumentStyle.scaleInt(horiMargin, pageDpi, 72, 'F'));
//		final int cHoriMargin = ((horiMargin < 0) ? 0 : DocumentStyle.scaleInt(horiMargin, pageDpi, 72, 'C'));
//		final int fVertMargin = ((vertMargin < 0) ? 0 : DocumentStyle.scaleInt(vertMargin, pageDpi, 72, 'F'));
//		final int cVertMargin = ((vertMargin < 0) ? 0 : DocumentStyle.scaleInt(vertMargin, pageDpi, 72, 'C'));
//		
//		//	get available parameter names, including ones from style proper (anchors !!!)
//		TreeSet docStyleParamNameSet = new TreeSet(Arrays.asList(this.parameterValueClassNames.getKeys()));
//		String[] dsDocStyleParamNames = docStyle.getKeys();
//		for (int p = 0; p < dsDocStyleParamNames.length; p++) {
//			if (dsDocStyleParamNames[p].startsWith("anchor."))
//				docStyleParamNameSet.add(dsDocStyleParamNames[p]);
//		}
//		final String[] docStyleParamNames = ((String[]) docStyleParamNameSet.toArray(new String[docStyleParamNameSet.size()]));
//		
//		//	collect actions
//		ArrayList actions = new ArrayList();
//		
//		//	collect style parameter group names that use font properties
//		final TreeSet fpDocStyleParamGroupNames = new TreeSet();
//		for (int p = 0; p < docStyleParamNames.length; p++) {
//			if ((fMinFontSize <= fMaxFontSize) && docStyleParamNames[p].endsWith(".fontSize") && checkParamValueClass(docStyleParamNames[p], Integer.class, false))
//				fpDocStyleParamGroupNames.add(docStyleParamNames[p].substring(0, docStyleParamNames[p].lastIndexOf('.')));
//			else if ((fMinFontSize < 72) && docStyleParamNames[p].endsWith(".minFontSize") && checkParamValueClass(docStyleParamNames[p], Integer.class, false))
//				fpDocStyleParamGroupNames.add(docStyleParamNames[p].substring(0, docStyleParamNames[p].lastIndexOf('.')));
//			else if ((0 < fMaxFontSize) && docStyleParamNames[p].endsWith(".maxFontSize") && checkParamValueClass(docStyleParamNames[p], Integer.class, false))
//				fpDocStyleParamGroupNames.add(docStyleParamNames[p].substring(0, docStyleParamNames[p].lastIndexOf('.')));
//			else if (fIsBold && (docStyleParamNames[p].endsWith(".isBold") || docStyleParamNames[p].endsWith(".startIsBold")) && checkParamValueClass(docStyleParamNames[p], Boolean.class, false))
//				fpDocStyleParamGroupNames.add(docStyleParamNames[p].substring(0, docStyleParamNames[p].lastIndexOf('.')));
//			else if (fIsItalics && (docStyleParamNames[p].endsWith(".isItalics") || docStyleParamNames[p].endsWith(".startIsItalics")) && checkParamValueClass(docStyleParamNames[p], Boolean.class, false))
//				fpDocStyleParamGroupNames.add(docStyleParamNames[p].substring(0, docStyleParamNames[p].lastIndexOf('.')));
//			else if (fIsAllCaps && (docStyleParamNames[p].endsWith(".isAllCaps") || docStyleParamNames[p].endsWith(".startIsAllCaps")) && checkParamValueClass(docStyleParamNames[p], Boolean.class, false))
//				fpDocStyleParamGroupNames.add(docStyleParamNames[p].substring(0, docStyleParamNames[p].lastIndexOf('.')));
//		}
//		
//		//	get currently selected parameter group from editor (if we have one open)
//		final String editParamGroupName = ((this.docStyleEditor == null) ? "<NONE>" : this.docStyleEditor.paramGroupName);
//		final String editTopParamGroupName;
//		if (editParamGroupName == null)
//			editTopParamGroupName = "<NONE>";
//		else if (editParamGroupName.indexOf('.') == -1)
//			editTopParamGroupName = editParamGroupName;
//		else editTopParamGroupName = editParamGroupName.substring(0, editParamGroupName.indexOf('.'));
//		
//		//	add actions using font style and size
//		if (((fMinFontSize <= fMaxFontSize) || fIsBold || fIsItalics || fIsAllCaps) && (fpDocStyleParamGroupNames.size() != 0))
//			actions.add(new SelectionAction("styleUseFont", "Use Font Properties", "Use font properties of selected words in document style") {
//				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
//					if (fpDocStyleParamGroupNames.size() == 1)
//						return super.getMenuItem(invoker);
//					
//					//	populate sub menu
//					JMenu m = new JMenu(this.label + " ...");
//					m.setToolTipText(this.tooltip);
//					JMenuItem mi;
//					String smTpgn = null;
//					JMenu sm = null;
//					for (Iterator pnit = fpDocStyleParamGroupNames.iterator(); pnit.hasNext();) {
//						final String pgn = ((String) pnit.next());
////						mi = new JMenuItem(pgn);
////						if ((docStyleEditor != null) && pgn.equals(docStyleEditor.paramGroupName))
////							mi.setText("<HTML><B>" + pgn + "</B></HTML>");
//						mi = createParameterGroupMenuItem(pgn);
//						mi.addActionListener(new ActionListener() {
//							public void actionPerformed(ActionEvent ae) {
//								useFontProperties(idmp.document, start.pageId, docStyle, pgn, docStyleParamNames, fMinFontSize, fMaxFontSize, fIsBold, fIsItalics, fIsAllCaps);
//							}
//						});
////						m.add(mi);
//						String tpgn = ((pgn.indexOf('.') == -1) ? pgn : pgn.substring(0, pgn.indexOf('.')));
//						if (tpgn.equals(editTopParamGroupName))
//							m.add(mi);
//						else {
//							if (!tpgn.equals(smTpgn)) {
//								smTpgn = tpgn;
//								sm = new JMenu(getParameterGroupLabel(smTpgn, false));
//								m.add(sm);
//							}
//							sm.add(mi);
//						}
//					}
//					
//					//	finally ...
//					return m;
//				}
//				public boolean performAction(ImDocumentMarkupPanel invoker) {
//					useFontProperties(idmp.document, start.pageId, docStyle, ((String) fpDocStyleParamGroupNames.first()), docStyleParamNames, fMinFontSize, fMaxFontSize, fIsBold, fIsItalics, fIsAllCaps);
//					return false;
//				}
//			});
//		
//		//	collect style parameter group names that use string properties
//		final TreeSet sDocStyleParamGroupNames = new TreeSet();
//		for (int p = 0; p < docStyleParamNames.length; p++) {
//			if (hasFixedValueList(docStyleParamNames[p]))
//				continue;
//			if (checkParamValueClass(docStyleParamNames[p], String.class, true))
//				sDocStyleParamGroupNames.add(docStyleParamNames[p].substring(0, docStyleParamNames[p].lastIndexOf('.')));
//		}
//		
//		//	add actions using word string (patterns, first and foremost, but also fixed values)
//		if ((fWordString != null) && (sDocStyleParamGroupNames.size() != 0))
//			actions.add(new SelectionAction("styleUseString", "Use String / Pattern", "Use string or pattern based on selected words in document style") {
//				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
//					if (sDocStyleParamGroupNames.size() == 1)
//						return super.getMenuItem(invoker);
//					
//					//	populate sub menu
//					JMenu m = new JMenu(this.label + " ...");
//					m.setToolTipText(this.tooltip);
//					JMenuItem mi;
//					String smTpgn = null;
//					JMenu sm = null;
//					for (Iterator pnit = sDocStyleParamGroupNames.iterator(); pnit.hasNext();) {
//						final String pgn = ((String) pnit.next());
////						mi = new JMenuItem(pgn);
////						if ((docStyleEditor != null) && pgn.equals(docStyleEditor.paramGroupName))
////							mi.setText("<HTML><B>" + pgn + "</B></HTML>");
//						mi = createParameterGroupMenuItem(pgn);
//						mi.addActionListener(new ActionListener() {
//							public void actionPerformed(ActionEvent ae) {
//								useString(idmp.document, start.pageId, docStyle, pgn, docStyleParamNames, fWordString);
//							}
//						});
////						m.add(mi);
//						String tpgn = ((pgn.indexOf('.') == -1) ? pgn : pgn.substring(0, pgn.indexOf('.')));
//						if (tpgn.equals(editTopParamGroupName))
//							m.add(mi);
//						else {
//							if (!tpgn.equals(smTpgn)) {
//								smTpgn = tpgn;
//								sm = new JMenu(getParameterGroupLabel(smTpgn, false));
//								m.add(sm);
//							}
//							sm.add(mi);
//						}
//					}
//					
//					//	finally ...
//					return m;
//				}
//				public boolean performAction(ImDocumentMarkupPanel invoker) {
//					useString(idmp.document, start.pageId, docStyle, ((String) sDocStyleParamGroupNames.first()), docStyleParamNames, fWordString);
//					return false;
//				}
//			});
//		
//		//	collect style parameter names that use bounding box properties
//		final TreeSet bbDocStyleParamNames = new TreeSet();
//		for (int p = 0; p < docStyleParamNames.length; p++) {
//			if (checkParamValueClass(docStyleParamNames[p], BoundingBox.class, true))
//				bbDocStyleParamNames.add(docStyleParamNames[p]);
//		}
//		
//		//	add actions using bounding box
//		if (bbDocStyleParamNames.size() != 0)
//			actions.add(new SelectionAction("styleUseBox", "Use Bounding Box", "Use bounding box (rectangular hull) of selected words in document style") {
//				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
//					if (bbDocStyleParamNames.size() == 1)
//						return super.getMenuItem(invoker);
//					
//					//	populate sub menu
//					JMenu m = new JMenu(this.label + " ...");
//					m.setToolTipText(this.tooltip);
//					JMenuItem mi;
//					String smTpgn = null;
//					JMenu sm = null;
//					for (Iterator pnit = bbDocStyleParamNames.iterator(); pnit.hasNext();) {
//						final String pn = ((String) pnit.next());
////						mi = new JMenuItem(pn);
////						if ((docStyleEditor != null) && (docStyleEditor.paramGroupName != null) && pn.startsWith(docStyleEditor.paramGroupName) && (docStyleEditor.paramGroupName.length() == pn.lastIndexOf('.')))
////							mi.setText("<HTML><B>" + pn + "</B></HTML>");
//						mi = createParameterMenuItem(pn);
//						mi.addActionListener(new ActionListener() {
//							public void actionPerformed(ActionEvent ae) {
//								useBoundingBox(idmp.document, start.pageId, docStyle, pn, fWordBounds);
//							}
//						});
////						m.add(mi);
//						String pgn = ((pn.lastIndexOf('.') == -1) ? pn : pn.substring(0, pn.lastIndexOf('.')));
//						String tpgn = ((pgn.indexOf('.') == -1) ? pgn : pgn.substring(0, pgn.indexOf('.')));
//						if (tpgn.equals(editTopParamGroupName))
//							m.add(mi);
//						else {
//							if (!tpgn.equals(smTpgn)) {
//								smTpgn = tpgn;
//								sm = new JMenu(getParameterGroupLabel(smTpgn, false));
//								m.add(sm);
//							}
//							sm.add(mi);
//						}
//					}
//					
//					//	finally ...
//					return m;
//				}
//				public boolean performAction(ImDocumentMarkupPanel invoker) {
//					useBoundingBox(idmp.document, start.pageId, docStyle, ((String) bbDocStyleParamNames.first()), fWordBounds);
//					return false;
//				}
//			});
//		
//		//	collect style parameter names that use integer properties (apart from font sizes)
//		final TreeSet mDocStyleParamNames = new TreeSet();
//		for (int p = 0; p < docStyleParamNames.length; p++) {
//			if (true
//					&& !docStyleParamNames[p].endsWith(".margin") && !docStyleParamNames[p].endsWith("Margin")
//					&& !docStyleParamNames[p].endsWith(".width") && !docStyleParamNames[p].endsWith("Width")
//					&& !docStyleParamNames[p].endsWith(".height") && !docStyleParamNames[p].endsWith("Height")
//					&& !docStyleParamNames[p].endsWith(".distance") && !docStyleParamNames[p].endsWith("Distance")
//					&& !docStyleParamNames[p].endsWith(".dist") && !docStyleParamNames[p].endsWith("Dist")
//					&& !docStyleParamNames[p].endsWith(".gap") && !docStyleParamNames[p].endsWith("Gap")
//				) continue;
//			if (checkParamValueClass(docStyleParamNames[p], Integer.class, false))
//				mDocStyleParamNames.add(docStyleParamNames[p]);
//		}
//		
//		//	if two words on same line, offer using horizontal distance between first and last (e.g. for minimum column margin)
//		if ((fHoriMargin != 0) && (mDocStyleParamNames.size() != 0))
//			actions.add(new SelectionAction("styleUseMargin", "Use Horizontal Margin", "Use horizontal margin between first and last seleted words in document style") {
//				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
//					if (mDocStyleParamNames.size() == 1)
//						return super.getMenuItem(invoker);
//					
//					//	populate sub menu
//					JMenu m = new JMenu(this.label + " ...");
//					m.setToolTipText(this.tooltip);
//					JMenuItem mi;
//					String smTpgn = null;
//					JMenu sm = null;
//					for (Iterator pnit = mDocStyleParamNames.iterator(); pnit.hasNext();) {
//						final String pn = ((String) pnit.next());
////						mi = new JMenuItem(pn);
////						if ((docStyleEditor != null) && (docStyleEditor.paramGroupName != null) && pn.startsWith(docStyleEditor.paramGroupName) && (docStyleEditor.paramGroupName.length() == pn.lastIndexOf('.')))
////							mi.setText("<HTML><B>" + pn + "</B></HTML>");
//						mi = createParameterMenuItem(pn);
//						mi.addActionListener(new ActionListener() {
//							public void actionPerformed(ActionEvent ae) {
//								useMargin(idmp.document.docId, docStyle, pn, fHoriMargin, cHoriMargin);
//							}
//						});
////						m.add(mi);
//						String pgn = ((pn.lastIndexOf('.') == -1) ? pn : pn.substring(0, pn.lastIndexOf('.')));
//						String tpgn = ((pgn.indexOf('.') == -1) ? pgn : pgn.substring(0, pgn.indexOf('.')));
//						if (tpgn.equals(editTopParamGroupName))
//							m.add(mi);
//						else {
//							if (!tpgn.equals(smTpgn)) {
//								smTpgn = tpgn;
//								sm = new JMenu(getParameterGroupLabel(smTpgn, false));
//								m.add(sm);
//							}
//							sm.add(mi);
//						}
//					}
//					
//					//	finally ...
//					return m;
//				}
//				public boolean performAction(ImDocumentMarkupPanel invoker) {
//					useMargin(idmp.document.docId, docStyle, ((String) mDocStyleParamNames.first()), fHoriMargin, cHoriMargin);
//					return false;
//				}
//			});
//		
//		//	if two or more words not on same line, offer using vertical distance between first and last (e.g. for minimum block margin)
//		if ((fVertMargin != 0) && (mDocStyleParamNames.size() != 0))
//			actions.add(new SelectionAction("styleUseMargin", "Use Vertical Margin", "Use vertical margin between first and last seleted words in document style") {
//				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
//					if (mDocStyleParamNames.size() == 1)
//						return super.getMenuItem(invoker);
//					
//					//	populate sub menu
//					JMenu m = new JMenu(this.label + " ...");
//					m.setToolTipText(this.tooltip);
//					JMenuItem mi;
//					String smTpgn = null;
//					JMenu sm = null;
//					for (Iterator pnit = mDocStyleParamNames.iterator(); pnit.hasNext();) {
//						final String pn = ((String) pnit.next());
////						mi = new JMenuItem(pn);
////						if ((docStyleEditor != null) && (docStyleEditor.paramGroupName != null) && pn.startsWith(docStyleEditor.paramGroupName) && (docStyleEditor.paramGroupName.length() == pn.lastIndexOf('.')))
////							mi.setText("<HTML><B>" + pn + "</B></HTML>");
//						mi = createParameterMenuItem(pn);
//						mi.addActionListener(new ActionListener() {
//							public void actionPerformed(ActionEvent ae) {
//								useMargin(idmp.document.docId, docStyle, pn, fVertMargin, cVertMargin);
//							}
//						});
////						m.add(mi);
//						String pgn = ((pn.lastIndexOf('.') == -1) ? pn : pn.substring(0, pn.lastIndexOf('.')));
//						String tpgn = ((pgn.indexOf('.') == -1) ? pgn : pgn.substring(0, pgn.indexOf('.')));
//						if (tpgn.equals(editTopParamGroupName))
//							m.add(mi);
//						else {
//							if (!tpgn.equals(smTpgn)) {
//								smTpgn = tpgn;
//								sm = new JMenu(getParameterGroupLabel(smTpgn, false));
//								m.add(sm);
//							}
//							sm.add(mi);
//						}
//					}
//					
//					//	finally ...
//					return m;
//				}
//				public boolean performAction(ImDocumentMarkupPanel invoker) {
//					useMargin(idmp.document.docId, docStyle, ((String) mDocStyleParamNames.first()), fVertMargin, cVertMargin);
//					return false;
//				}
//			});
//		
//		//	collect style parameter group names that use bounding box properties
//		final TreeSet bbDocStyleParamGroupNames = new TreeSet();
//		for (int p = 0; p < docStyleParamNames.length; p++) {
//			if (checkParamValueClass(docStyleParamNames[p], BoundingBox.class, true))
//				bbDocStyleParamGroupNames.add(docStyleParamNames[p].substring(0, docStyleParamNames[p].lastIndexOf('.')));
//		}
//		
//		//	combine style parameter names
//		final TreeSet selDocStyleParamGroupNames = new TreeSet();
//		selDocStyleParamGroupNames.addAll(fpDocStyleParamGroupNames);
//		selDocStyleParamGroupNames.addAll(sDocStyleParamGroupNames);
//		selDocStyleParamGroupNames.addAll(bbDocStyleParamGroupNames);
//		
//		//	add prefix for creating anchor (only if string given)
//		if (fWordString != null)
//			selDocStyleParamGroupNames.add("anchor.<create>");
//		
//		//	add actions using all properties of selection
//		if (selDocStyleParamGroupNames.size() != 0)
//			actions.add(new SelectionAction("styleUseAll", "Use Selection", "Use properties and bounds of selected words in document style") {
//				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
//					if (selDocStyleParamGroupNames.size() == 1)
//						return super.getMenuItem(invoker);
//					
//					//	populate sub menu
//					JMenu m = new JMenu(this.label + " ...");
//					m.setToolTipText(this.tooltip);
//					JMenuItem mi;
//					String smTpgn = null;
//					JMenu sm = null;
//					for (Iterator pnit = selDocStyleParamGroupNames.iterator(); pnit.hasNext();) {
//						final String pgn = ((String) pnit.next());
////						mi = new JMenuItem(pgn);
////						if ((docStyleEditor != null) && pgn.equals(docStyleEditor.paramGroupName))
////							mi.setText("<HTML><B>" + ("anchor.<create>".equals(pgn) ? "anchor.&lt;create&gt;" : pgn) + "</B></HTML>");
//						mi = createParameterGroupMenuItem(pgn);
//						mi.addActionListener(new ActionListener() {
//							public void actionPerformed(ActionEvent ae) {
//								useSelection(idmp.document, start.pageId, docStyle, pgn, docStyleParamNames, fMinFontSize, fMaxFontSize, fIsBold, fIsItalics, fIsAllCaps, fWordString, fWordBounds);
//							}
//						});
////						m.add(mi);
//						String tpgn = ((pgn.indexOf('.') == -1) ? pgn : pgn.substring(0, pgn.indexOf('.')));
//						if (tpgn.equals(editTopParamGroupName))
//							m.add(mi);
//						else {
//							if (!tpgn.equals(smTpgn)) {
//								smTpgn = tpgn;
//								sm = new JMenu(getParameterGroupLabel(smTpgn, false));
//								m.add(sm);
//							}
//							sm.add(mi);
//						}
//					}
//					
//					//	finally ...
//					return m;
//				}
//				public boolean performAction(ImDocumentMarkupPanel invoker) {
//					useSelection(idmp.document, start.pageId, docStyle, ((String) selDocStyleParamGroupNames.first()), docStyleParamNames, fMinFontSize, fMaxFontSize, fIsBold, fIsItalics, fIsAllCaps, fWordString, fWordBounds);
//					return false;
//				}
//			});
//		
//		//	add action editing document style (open dialog with tree based access to all style parameters)
//		if ((this.docStyleEditor == null) || !this.docStyleEditor.isVisible() || !docStyleName.equals(this.docStyleEditor.docStyleName))
//			actions.add(this.getEditDocStyleAction(idmp.document, docStyleName, docStyle));
//		
//		//	finally ...
//		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
//	 */
//	public SelectionAction[] getActions(Point start, Point end, final ImPage page, final ImDocumentMarkupPanel idmp) {
//		
//		//	get document style name and style
//		String docStyleName = ((String) idmp.document.getAttribute(DocumentStyle.DOCUMENT_STYLE_NAME_ATTRIBUTE));
//		
//		//	no document style assigned, offer adding or creating one
//		if (docStyleName == null) {
//			
//			//	no document style editing, offer adding or creating one
//			if ((this.docStyleEditor == null) || (this.docStyleEditor.docStyleName == null))
//				return this.getAssignDocStyleAction(idmp.document);
//			
//			//	use editing document style
//			else docStyleName = this.docStyleEditor.docStyleName;
//		}
//		
//		//	style already assigned, offer extending or modifying it
////		final Settings docStyle = this.getDocStyle(idmp.document.docId, docStyleName);
//		final Settings docStyle = this.getDocStyle(docStyleName);
//		if (this.docStyleEditor != null)
//			this.docStyleEditor.setDocStyle(idmp.document, docStyleName, docStyle);
//		
//		//	measure selection, and crop to fit in page bounds (editor panel adds a bit extra space around actual page)
//		int left = Math.max(page.bounds.left, Math.min(start.x, end.x));
//		int right = Math.min(page.bounds.right, Math.max(start.x, end.x));
//		int top = Math.max(page.bounds.top, Math.min(start.y, end.y));
//		int bottom = Math.min(page.bounds.bottom, Math.max(start.y, end.y));
//		
//		//	fix parameter values, scaling bounds and margins to default 72 DPI
//		final BoundingBox fWordBounds = DocumentStyle.scaleBox(new BoundingBox(left, right, top, bottom), page.getImageDPI(), 72, 'O');
//		final int fHoriMargin = DocumentStyle.scaleInt((right - left), page.getImageDPI(), 72, 'F');
//		final int cHoriMargin = DocumentStyle.scaleInt((right - left), page.getImageDPI(), 72, 'C');
//		final int fVertMargin = DocumentStyle.scaleInt((bottom - top), page.getImageDPI(), 72, 'F');
//		final int cVertMargin = DocumentStyle.scaleInt((bottom - top), page.getImageDPI(), 72, 'C');
//		
//		//	get available parameter names, including ones from style proper (anchors !!!)
//		TreeSet docStyleParamNameSet = new TreeSet(Arrays.asList(this.parameterValueClassNames.getKeys()));
//		String[] dsDocStyleParamNames = docStyle.getKeys();
//		for (int p = 0; p < dsDocStyleParamNames.length; p++) {
//			if (dsDocStyleParamNames[p].startsWith("anchor."))
//				docStyleParamNameSet.add(dsDocStyleParamNames[p]);
//		}
//		final String[] docStyleParamNames = ((String[]) docStyleParamNameSet.toArray(new String[docStyleParamNameSet.size()]));
//		
//		//	collect actions
//		ArrayList actions = new ArrayList();
//		
//		//	collect style parameter group names that use bounding box properties
//		final TreeSet bbDocStyleParamNames = new TreeSet();
//		for (int p = 0; p < docStyleParamNames.length; p++) {
//			if (checkParamValueClass(docStyleParamNames[p], BoundingBox.class, true))
//				bbDocStyleParamNames.add(docStyleParamNames[p]);
//		}
//		
//		//	get currently selected parameter group from editor (if we have one open)
//		final String editParamGroupName = ((this.docStyleEditor == null) ? "<NONE>" : this.docStyleEditor.paramGroupName);
//		final String editTopParamGroupName;
//		if (editParamGroupName == null)
//			editTopParamGroupName = "<NONE>";
//		else if (editParamGroupName.indexOf('.') == -1)
//			editTopParamGroupName = editParamGroupName;
//		else editTopParamGroupName = editParamGroupName.substring(0, editParamGroupName.indexOf('.'));
//		
//		//	add actions using bounding box
//		if (bbDocStyleParamNames.size() != 0)
//			actions.add(new SelectionAction("styleUseBox", "Use Bounding Box", "Use selected bounding box in document style") {
//				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
//					if (bbDocStyleParamNames.size() == 1)
//						return super.getMenuItem(invoker);
//					
//					//	populate sub menu
//					JMenu m = new JMenu(this.label + " ...");
//					m.setToolTipText(this.tooltip);
//					JMenuItem mi;
//					String smTpgn = null;
//					JMenu sm = null;
//					for (Iterator pnit = bbDocStyleParamNames.iterator(); pnit.hasNext();) {
//						final String pn = ((String) pnit.next());
////						mi = new JMenuItem(pn);
////						if ((docStyleEditor != null) && (docStyleEditor.paramGroupName != null) && pn.startsWith(docStyleEditor.paramGroupName) && (docStyleEditor.paramGroupName.length() == pn.lastIndexOf('.')))
////							mi.setText("<HTML><B>" + pn + "</B></HTML>");
//						mi = createParameterMenuItem(pn);
//						mi.addActionListener(new ActionListener() {
//							public void actionPerformed(ActionEvent ae) {
//								useBoundingBox(idmp.document, page.pageId, docStyle, pn, fWordBounds);
//							}
//						});
////						m.add(mi);
//						String pgn = ((pn.lastIndexOf('.') == -1) ? pn : pn.substring(0, pn.lastIndexOf('.')));
//						String tpgn = ((pgn.indexOf('.') == -1) ? pgn : pgn.substring(0, pgn.indexOf('.')));
//						if (tpgn.equals(editTopParamGroupName))
//							m.add(mi);
//						else {
//							if (!tpgn.equals(smTpgn)) {
//								smTpgn = tpgn;
//								sm = new JMenu(getParameterGroupLabel(smTpgn, false));
//								m.add(sm);
//							}
//							sm.add(mi);
//						}
//					}
//					
//					//	finally ...
//					return m;
//				}
//				public boolean performAction(ImDocumentMarkupPanel invoker) {
//					useBoundingBox(idmp.document, page.pageId, docStyle, ((String) bbDocStyleParamNames.first()), fWordBounds);
//					return false;
//				}
//			});
//		
//		//	collect style parameter names that use integer properties (apart from font sizes)
//		final TreeSet mDocStyleParamNames = new TreeSet();
//		for (int p = 0; p < docStyleParamNames.length; p++) {
//			if (true
//					&& !docStyleParamNames[p].endsWith(".margin") && !docStyleParamNames[p].endsWith("Margin")
//					&& !docStyleParamNames[p].endsWith(".width") && !docStyleParamNames[p].endsWith("Width")
//					&& !docStyleParamNames[p].endsWith(".height") && !docStyleParamNames[p].endsWith("Height")
//					&& !docStyleParamNames[p].endsWith(".distance") && !docStyleParamNames[p].endsWith("Distance")
//					&& !docStyleParamNames[p].endsWith(".dist") && !docStyleParamNames[p].endsWith("Dist")
//					&& !docStyleParamNames[p].endsWith(".gap") && !docStyleParamNames[p].endsWith("Gap")
//				) continue;
//			if (checkParamValueClass(docStyleParamNames[p], Integer.class, false))
//				mDocStyleParamNames.add(docStyleParamNames[p]);
//		}
//		
//		//	if two words on same line, offer using horizontal distance between first and last (e.g. for minimum column margin)
//		if ((fHoriMargin != 0) && (mDocStyleParamNames.size() != 0))
//			actions.add(new SelectionAction("styleUseMargin", "Use Horizontal Margin", "Use width of selection in document style") {
//				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
//					if (mDocStyleParamNames.size() == 1)
//						return super.getMenuItem(invoker);
//					
//					//	populate sub menu
//					JMenu m = new JMenu(this.label + " ...");
//					m.setToolTipText(this.tooltip);
//					JMenuItem mi;
//					String smTpgn = null;
//					JMenu sm = null;
//					for (Iterator pnit = mDocStyleParamNames.iterator(); pnit.hasNext();) {
//						final String pn = ((String) pnit.next());
////						mi = new JMenuItem(pn);
////						if ((docStyleEditor != null) && (docStyleEditor.paramGroupName != null) && pn.startsWith(docStyleEditor.paramGroupName) && (docStyleEditor.paramGroupName.length() == pn.lastIndexOf('.')))
////							mi.setText("<HTML><B>" + pn + "</B></HTML>");
//						mi = createParameterMenuItem(pn);
//						mi.addActionListener(new ActionListener() {
//							public void actionPerformed(ActionEvent ae) {
//								useMargin(idmp.document.docId, docStyle, pn, fHoriMargin, cHoriMargin);
//							}
//						});
////						m.add(mi);
//						String pgn = ((pn.lastIndexOf('.') == -1) ? pn : pn.substring(0, pn.lastIndexOf('.')));
//						String tpgn = ((pgn.indexOf('.') == -1) ? pgn : pgn.substring(0, pgn.indexOf('.')));
//						if (tpgn.equals(editTopParamGroupName))
//							m.add(mi);
//						else {
//							if (!tpgn.equals(smTpgn)) {
//								smTpgn = tpgn;
//								sm = new JMenu(getParameterGroupLabel(smTpgn, false));
//								m.add(sm);
//							}
//							sm.add(mi);
//						}
//					}
//					
//					//	finally ...
//					return m;
//				}
//				public boolean performAction(ImDocumentMarkupPanel invoker) {
//					useMargin(idmp.document.docId, docStyle, ((String) mDocStyleParamNames.first()), fHoriMargin, cHoriMargin);
//					return false;
//				}
//			});
//		
//		//	if two or more words not on same line, offer using vertical distance between first and last (e.g. for minimum block margin)
//		if ((fVertMargin != 0) && (mDocStyleParamNames.size() != 0))
//			actions.add(new SelectionAction("styleUseMargin", "Use Vertical Margin", "Use height of selection in document style") {
//				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
//					if (mDocStyleParamNames.size() == 1)
//						return super.getMenuItem(invoker);
//					
//					//	populate sub menu
//					JMenu m = new JMenu(this.label + " ...");
//					m.setToolTipText(this.tooltip);
//					JMenuItem mi;
//					String smTpgn = null;
//					JMenu sm = null;
//					for (Iterator pnit = mDocStyleParamNames.iterator(); pnit.hasNext();) {
//						final String pn = ((String) pnit.next());
////						mi = new JMenuItem(pn);
////						if ((docStyleEditor != null) && (docStyleEditor.paramGroupName != null) && pn.startsWith(docStyleEditor.paramGroupName) && (docStyleEditor.paramGroupName.length() == pn.lastIndexOf('.')))
////							mi.setText("<HTML><B>" + pn + "</B></HTML>");
//						mi = createParameterMenuItem(pn);
//						mi.addActionListener(new ActionListener() {
//							public void actionPerformed(ActionEvent ae) {
//								useMargin(idmp.document.docId, docStyle, pn, fVertMargin, cVertMargin);
//							}
//						});
////						m.add(mi);
//						String pgn = ((pn.lastIndexOf('.') == -1) ? pn : pn.substring(0, pn.lastIndexOf('.')));
//						String tpgn = ((pgn.indexOf('.') == -1) ? pgn : pgn.substring(0, pgn.indexOf('.')));
//						if (tpgn.equals(editTopParamGroupName))
//							m.add(mi);
//						else {
//							if (!tpgn.equals(smTpgn)) {
//								smTpgn = tpgn;
//								sm = new JMenu(getParameterGroupLabel(smTpgn, false));
//								m.add(sm);
//							}
//							sm.add(mi);
//						}
//					}
//					
//					//	finally ...
//					return m;
//				}
//				public boolean performAction(ImDocumentMarkupPanel invoker) {
//					useMargin(idmp.document.docId, docStyle, ((String) mDocStyleParamNames.first()), fVertMargin, cVertMargin);
//					return false;
//				}
//			});
//		
//		//	add action editing document style (open dialog with tree based access to all style parameters)
//		if ((this.docStyleEditor == null) || !this.docStyleEditor.isVisible() || !docStyleName.equals(this.docStyleEditor.docStyleName))
//			actions.add(this.getEditDocStyleAction(idmp.document, docStyleName, docStyle));
//		
//		//	finally ...
//		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
//	}
//	
//	private JMenuItem createParameterGroupMenuItem(String pgn) {
//		ParameterGroupDescription pgd = this.getParameterGroupDescription(pgn);
//		String pgl = this.getParameterGroupLabel(pgn, pgd, false);
//		if ((this.docStyleEditor != null) && pgn.equals(this.docStyleEditor.paramGroupName))
//			pgl = ("<HTML><B>" + AnnotationUtils.escapeForXml(pgl) + "</B></HTML>");
//		JMenuItem mi = new JMenuItem(pgl);
//		if (pgd != null)
//			mi.setToolTipText(pgd.getDescription());
//		return mi;
//	}
//	
//	private JMenuItem createParameterMenuItem(String pn) {
//		String pgn = pn.substring(0, pn.lastIndexOf('.'));
//		String glpn = pn.substring(pn.lastIndexOf('.') + ".".length());
//		ParameterGroupDescription pgd = this.getParameterGroupDescription(pgn);
//		String pgl = this.getParameterGroupLabel(pgn, pgd, false);
//		String pl = ((pgd == null) ? pn : pgd.getParamLabel(glpn));
//		if ((pl == null) || (pl.length() == 0))
//			pl = pn;
//		else pl = (pgl + " / " + pl);
//		if ((this.docStyleEditor != null) && (this.docStyleEditor.paramGroupName != null) && pn.startsWith(this.docStyleEditor.paramGroupName) && (this.docStyleEditor.paramGroupName.length() == pn.lastIndexOf('.')))
//			pl = ("<HTML><B>" + AnnotationUtils.escapeForXml(pl) + "</B></HTML>");
//		JMenuItem mi = new JMenuItem(pl);
//		if (pgd != null)
//			mi.setToolTipText(pgd.getParamDescription(glpn));
//		return mi;
//	}
//	
//	private String getParameterGroupLabel(String pgn, boolean forTree) {
//		return this.getParameterGroupLabel(pgn, this.getParameterGroupDescription(pgn), forTree);
//	}
//	
//	private String getParameterGroupLabel(String pgn, ParameterGroupDescription pgd, boolean forTree) {
//		if (pgn.startsWith("anchor.")) {
//			if (forTree)
//				return pgn.substring("anchor.".length());
//			else return ("Anchor '" + pgn.substring("anchor.".length()) + "'");
//		}
//		String pgl = ((pgd == null) ? null : pgd.getLabel());
//		if ((pgl == null) || (pgl.length() == 0))
//			return StringUtils.capitalize(pgn);
//		else return pgl;
//	}
//	
//	private static final boolean DEBUG_STYLE_UPDATES = true;
//	
//	private void useFontProperties(ImDocument doc, int pageId, Settings docStyle, String docStyleParamGroupName, String[] docStyleParamNames, int minFontSize, int maxFontSize, boolean isBold, boolean isItalics, boolean isAllCaps) {
//		
//		//	get parameter group description and group label
//		ParameterGroupDescription pgd = this.getParameterGroupDescription(docStyleParamGroupName);
//		String pgl = ((pgd == null) ? null : pgd.getLabel());
//		
//		//	ask for properties to use
//		JPanel fpPanel = new JPanel(new GridLayout(0, 1, 0, 0), true);
//		UseBooleanPanel useMinFontSize = null;
//		UseBooleanPanel useMaxFontSize = null;
//		if (minFontSize <= maxFontSize) {
//			int eMinFontSize = 72;
//			try {
//				eMinFontSize = Integer.parseInt(docStyle.getSetting((docStyleParamGroupName + ".minFontSize"), "72"));
//			} catch (NumberFormatException nfe) {}
//			if (minFontSize < eMinFontSize) {
//				String pl;
//				if (pgl == null)
//					pl = ("Use " + minFontSize + " as Minimum Font Size (currently " + eMinFontSize + ")");
//				else pl = ("Use " + minFontSize + " as Minimum Font Size for " + pgl + " (currently " + eMinFontSize + ")");
//				String pd = ((pgd == null) ? null : pgd.getParamDescription("minFontSize"));
//				useMinFontSize = new UseBooleanPanel(this.docStyleEditor, (docStyleParamGroupName + ".minFontSize"), pl, pd, true);
//				fpPanel.add(useMinFontSize);
//			}
//			int eMaxFontSize = 0;
//			try {
//				eMaxFontSize = Integer.parseInt(docStyle.getSetting((docStyleParamGroupName + ".maxFontSize"), "0"));
//			} catch (NumberFormatException nfe) {}
//			if (eMaxFontSize < maxFontSize) {
//				String pl;
//				if (pgl == null)
//					pl = ("Use " + maxFontSize + " as Maximum Font Size (currently " + eMaxFontSize + ")");
//				else pl = ("Use " + maxFontSize + " as Maximum Font Size for " + pgl + " (currently " + eMaxFontSize + ")");
//				String pd = ((pgd == null) ? null : pgd.getParamDescription("minFontSize"));
//				useMaxFontSize = new UseBooleanPanel(this.docStyleEditor, (docStyleParamGroupName + ".maxFontSize"), pl, pd, true);
//				fpPanel.add(useMaxFontSize);
//			}
//		}
//		UseBooleanPanel useIsBold = null;
//		if (isBold) {
//			for (int p = 0; p < docStyleParamNames.length; p++) {
//				if (docStyleParamNames[p].equals(docStyleParamGroupName + ".isBold")) {
//					String pl;
//					if (pgl == null)
//						pl = "Require Values to be Bold";
//					else pl = ("Require Values for " + pgl + " to be Bold");
//					String pd = ((pgd == null) ? null : pgd.getParamDescription("isBold"));
//					useIsBold = new UseBooleanPanel(this.docStyleEditor, (docStyleParamGroupName + ".isBold"), pl, pd, true);
//					break;
//				}
//				else if (docStyleParamNames[p].equals(docStyleParamGroupName + ".startIsBold")) {
//					String pl;
//					if (pgl == null)
//						pl = "Require Value Starts to be Bold";
//					else pl = ("Require Values for " + pgl + " to Start in Bold");
//					String pd = ((pgd == null) ? null : pgd.getParamDescription("startIsBold"));
//					useIsBold = new UseBooleanPanel(this.docStyleEditor, (docStyleParamGroupName + ".startIsBold"), pl, pd, true);
//					break;
//				}
//			}
//			if (useIsBold != null)
//				fpPanel.add(useIsBold);
//		}
//		UseBooleanPanel useIsItalics = null;
//		if (isItalics) {
//			for (int p = 0; p < docStyleParamNames.length; p++) {
//				if (docStyleParamNames[p].equals(docStyleParamGroupName + ".isItalics")) {
//					String pl;
//					if (pgl == null)
//						pl = "Require Values to be in Italics";
//					else pl = ("Require Values for " + pgl + " to be in Italics");
//					String pd = ((pgd == null) ? null : pgd.getParamDescription("isItalics"));
//					useIsItalics = new UseBooleanPanel(this.docStyleEditor, (docStyleParamGroupName + ".isItalics"), pl, pd, true);
//					break;
//				}
//				else if (docStyleParamNames[p].equals(docStyleParamGroupName + ".startIsItalics")) {
//					String pl;
//					if (pgl == null)
//						pl = "Require Values Starts to be in Italics";
//					else pl = ("Require Values for " + pgl + " to Start in Italics");
//					String pd = ((pgd == null) ? null : pgd.getParamDescription("startIsItalics"));
//					useIsItalics = new UseBooleanPanel(this.docStyleEditor, (docStyleParamGroupName + ".startIsItalics"), pl, pd, true);
//					break;
//				}
//			}
//			if (useIsItalics != null)
//				fpPanel.add(useIsItalics);
//		}
//		UseBooleanPanel useIsAllCaps = null;
//		if (isAllCaps) {
//			for (int p = 0; p < docStyleParamNames.length; p++) {
//				if (docStyleParamNames[p].equals(docStyleParamGroupName + ".isAllCaps")) {
//					String pl;
//					if (pgl == null)
//						pl = "Require Values to be in All Caps";
//					else pl = ("Require Values for " + pgl + " to be in All Caps");
//					String pd = ((pgd == null) ? null : pgd.getParamDescription("isAllCaps"));
//					useIsAllCaps = new UseBooleanPanel(this.docStyleEditor, (docStyleParamGroupName + ".isAllCaps"), pl, pd, true);
//					break;
//				}
//				else if (docStyleParamNames[p].equals(docStyleParamGroupName + ".startIsAllCaps")) {
//					String pl;
//					if (pgl == null)
//						pl = "Require Values Starts to be in All Caps";
//					else pl = ("Require Values for " + pgl + " to Start in All Caps");
//					String pd = ((pgd == null) ? null : pgd.getParamDescription("startIsAllCaps"));
//					useIsAllCaps = new UseBooleanPanel(this.docStyleEditor, (docStyleParamGroupName + ".startIsAllCaps"), pl, pd, true);
//					break;
//				}
//			}
//			if (useIsAllCaps != null)
//				fpPanel.add(useIsAllCaps);
//		}
//		
//		//	prompt
//		int choice = JOptionPane.showConfirmDialog(null, fpPanel, ("Select Font Properties to Use" + ((pgl == null) ? "" : (" in " + pgl))), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
//		if (choice != JOptionPane.OK_OPTION)
//			return;
//		
//		//	we have an anchor, adjust minimum page ID
//		if (docStyleParamGroupName.startsWith("anchor.")) {
//			int maxPageId = Integer.parseInt(docStyle.getSetting("anchor.maxPageId", "0"));
//			if (maxPageId < pageId)
//				docStyle.setSetting("anchor.maxPageId", ("" + (pageId + doc.getFirstPageId())));
//		}
//		
//		//	set properties
//		if ((useMinFontSize != null) && useMinFontSize.useParam.isSelected()) {
//			docStyle.setSetting((docStyleParamGroupName + ".minFontSize"), ("" + minFontSize));
//			if (DEBUG_STYLE_UPDATES) System.out.println(docStyleParamGroupName + ".minFontSize set to " + minFontSize);
//		}
//		if ((useMaxFontSize != null) && useMaxFontSize.useParam.isSelected()) {
//			docStyle.setSetting((docStyleParamGroupName + ".maxFontSize"), ("" + maxFontSize));
//			if (DEBUG_STYLE_UPDATES) System.out.println(docStyleParamGroupName + ".maxFontSize set to " + maxFontSize);
//		}
//		if ((minFontSize == maxFontSize) && (useMinFontSize != null) && useMinFontSize.useParam.isSelected() && (useMaxFontSize != null) && useMaxFontSize.useParam.isSelected()) {
//			docStyle.setSetting((docStyleParamGroupName + ".fontSize"), ("" + minFontSize));
//			if (DEBUG_STYLE_UPDATES) System.out.println(docStyleParamGroupName + ".fontSize set to " + minFontSize);
//		}
//		if ((useIsBold != null) && useIsBold.useParam.isSelected()) {
//			docStyle.setSetting(useIsBold.docStyleParamName, "true");
//			if (DEBUG_STYLE_UPDATES) System.out.println(useIsBold.docStyleParamName + " set to true");
//		}
//		if ((useIsItalics != null) && useIsItalics.useParam.isSelected()) {
//			docStyle.setSetting(useIsItalics.docStyleParamName, "true");
//			if (DEBUG_STYLE_UPDATES) System.out.println(useIsItalics.docStyleParamName + " set to true");
//		}
//		if ((useIsAllCaps != null) && useIsAllCaps.useParam.isSelected()) {
//			docStyle.setSetting(useIsAllCaps.docStyleParamName, "true");
//			if (DEBUG_STYLE_UPDATES) System.out.println(useIsAllCaps.docStyleParamName + " set to true");
//		}
//		
//		//	if style editor open, adjust tree path
//		if (this.docStyleEditor != null) {
//			this.docStyleEditor.setParamGroupName(docStyleParamGroupName);
//			this.docStyleEditor.setDocStyleDirty(true);
//		}
////		
////		//	index document style for saving
////		this.docStylesByDocId.put(doc.docId, docStyle);
//	}
//	
//	private void useMargin(String docId, Settings docStyle, String docStyleParamName, int fMargin, int cMargin) {
//		Class paramValueClass = this.getParamValueClass(docStyleParamName);
//		
//		//	single integer, expand or overwrite
//		if (Integer.class.getName().equals(paramValueClass.getName())) {
//			int eMargin = Integer.parseInt(docStyle.getSetting(docStyleParamName, "-1"));
//			if (eMargin == -1) {
//				docStyle.setSetting(docStyleParamName, ("" + fMargin));
//				if (DEBUG_STYLE_UPDATES) System.out.println(docStyleParamName + " set to " + fMargin);
//			}
//			else if (docStyleParamName.startsWith(".min", docStyleParamName.lastIndexOf('.'))) {
//				docStyle.setSetting(docStyleParamName, ("" + Math.min(((eMargin == -1) ? fMargin : eMargin),  fMargin)));
//				if (DEBUG_STYLE_UPDATES) System.out.println(docStyleParamName + " set to " + Math.min(eMargin,  Math.min(((eMargin == -1) ? cMargin : eMargin),  fMargin)));
//			}
//			else if (docStyleParamName.startsWith(".max", docStyleParamName.lastIndexOf('.'))) {
//				docStyle.setSetting(docStyleParamName, ("" + Math.max(((eMargin == -1) ? fMargin : eMargin),  cMargin)));
//				if (DEBUG_STYLE_UPDATES) System.out.println(docStyleParamName + " set to " + Math.max(eMargin,  Math.max(((eMargin == -1) ? cMargin : eMargin),  cMargin)));
//			}
//			else {
//				docStyle.setSetting(docStyleParamName, ("" + fMargin));
//				if (DEBUG_STYLE_UPDATES) System.out.println(docStyleParamName + " set to " + fMargin);
//			}
//		}
//		
//		//	list of integers, add new one and eliminate duplicates
//		else if (Integer.class.getName().equals(DocumentStyle.getListElementClass(paramValueClass).getName())) {
//			String eMarginStr = docStyle.getSetting(docStyleParamName);
//			if ((eMarginStr == null) || (eMarginStr.trim().length() == 0)) {
//				docStyle.setSetting(docStyleParamName, ("" + fMargin));
//				if (DEBUG_STYLE_UPDATES) System.out.println(docStyleParamName + " set to " + fMargin);
//			}
//			else {
//				String[] eMarginStrs = eMarginStr.split("[^0-9]+");
//				for (int e = 0; e < eMarginStrs.length; e++) {
//					if (fMargin == Integer.parseInt(eMarginStrs[e]))
//						return;
//				}
//				docStyle.setSetting(docStyleParamName, (eMarginStr + " " + fMargin));
//				if (DEBUG_STYLE_UPDATES) System.out.println(docStyleParamName + " set to " + (eMarginStr + " " + fMargin));
//			}
//		}
//		
//		//	if style editor open, adjust tree path
//		if (this.docStyleEditor != null) {
//			this.docStyleEditor.setParamGroupName(docStyleParamName.substring(0, docStyleParamName.lastIndexOf('.')));
//			this.docStyleEditor.setDocStyleDirty(true);
//		}
////		
////		//	index document style for saving
////		this.docStylesByDocId.put(docId, docStyle);
//	}
//	
//	private void useString(final ImDocument doc, int pageId, Settings docStyle, String docStyleParamGroupName, String[] docStyleParamNames, String string) {
//		
//		//	get parameter group description and group label
//		ParameterGroupDescription pgd = this.getParameterGroupDescription(docStyleParamGroupName);
//		String pgl = ((pgd == null) ? null : pgd.getLabel());
//		
//		//	collect style parameter names in argument group that use string properties, constructing string usage panels on the fly
//		TreeSet sDocStyleParamPanels = new TreeSet();
//		final ImTokenSequence[] docTokens = {null}; // using array facilitates sharing tokens and still generating them on demand
//		for (int p = 0; p < docStyleParamNames.length; p++) {
//			if (!docStyleParamNames[p].startsWith(docStyleParamGroupName + "."))
//				continue;
//			if (!checkParamValueClass(docStyleParamNames[p], String.class, true))
//				continue;
//			String localDspn = docStyleParamNames[p].substring(docStyleParamNames[p].lastIndexOf('.') + ".".length());
//			String pl = ((pgd == null) ? null : pgd.getParamLabel(localDspn));
//			if (pl == null)
//				pl = (" Use as " + localDspn);
//			else pl = (" Use as " + pl);
//			String pd = ((pgd == null) ? null : pgd.getParamDescription(localDspn));
//			sDocStyleParamPanels.add(new UseStringPanel(this.docStyleEditor, docStyleParamNames[p], pl, pd, true, string, true, true) {
//				ImTokenSequence getTestDocTokens() {
//					if (docTokens[0] == null)
//						docTokens[0] = new ImTokenSequence(((Tokenizer) doc.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER)), doc.getTextStreamHeads(), (ImTokenSequence.NORMALIZATION_LEVEL_PARAGRAPHS | ImTokenSequence.NORMALIZE_CHARACTERS));
//					return docTokens[0];
//				}
//				ImDocument getTestDoc() {
//					return doc;
//				}
//				DisplayExtensionGraphics[] getDisplayExtensionGraphics(ImPage page) {
//					if (this.docStyleParamName.endsWith(".linePattern") || this.docStyleParamName.endsWith("LinePattern"))
//						return getLinePatternVisualizationGraphics(this.parent, page, this.getValue());
//					if (this.docStyleParamName.endsWith(".pattern") || this.docStyleParamName.endsWith("Pattern"))
//						return getPatternVisualizationGraphics(this.parent, page, this.getValue());
//					//	TODO think of more
//					return NO_DISPLAY_EXTENSION_GRAPHICS;
//				}
//			});
//		}
//		
//		//	nothing to work with
//		if (sDocStyleParamPanels.isEmpty())
//			return;
//		
//		//	assemble panel
//		JPanel sPanel = new JPanel(new GridLayout(0, 1, 0, 3), true);
//		for (Iterator ppit = sDocStyleParamPanels.iterator(); ppit.hasNext();)
//			sPanel.add((UseStringPanel) ppit.next());
//		
//		//	prompt
//		int choice = JOptionPane.showConfirmDialog(null, sPanel, ("Select how to Use '" + string + "'" + ((pgl == null) ? "" : (" in " + pgl))), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
//		if (choice != JOptionPane.OK_OPTION)
//			return;
//		
//		//	we have an anchor, adjust minimum page ID
//		if (docStyleParamGroupName.startsWith("anchor.")) {
//			int maxPageId = Integer.parseInt(docStyle.getSetting("anchor.maxPageId", "0"));
//			if (maxPageId < pageId)
//				docStyle.setSetting("anchor.maxPageId", ("" + (pageId + doc.getFirstPageId())));
//		}
//		
//		//	write parameters
//		for (Iterator ppit = sDocStyleParamPanels.iterator(); ppit.hasNext();) {
//			UseStringPanel usp = ((UseStringPanel) ppit.next());
//			if (!usp.useParam.isSelected())
//				continue;
//			string = usp.string.getText().trim();
//			if (string.length() == 0)
//				continue;
//			Class paramValueClass = this.getParamValueClass(usp.docStyleParamName);
//			if (String.class.getName().equals(paramValueClass.getName())) {
//				docStyle.setSetting(usp.docStyleParamName, string);
//				if (DEBUG_STYLE_UPDATES) System.out.println(usp.docStyleParamName + " set to " + string);
//			}
//			else if (String.class.getName().equals(DocumentStyle.getListElementClass(paramValueClass).getName())) {
//				String eString = docStyle.getSetting(usp.docStyleParamName, "").trim();
//				if (eString.length() == 0) {
//					docStyle.setSetting(usp.docStyleParamName, string);
//					if (DEBUG_STYLE_UPDATES) System.out.println(usp.docStyleParamName + " set to " + string);
//				}
//				else {
//					TreeSet eStringSet = new TreeSet(Arrays.asList(eString.split("\\s+")));
//					eStringSet.add(string);
//					StringBuffer eStringsStr = new StringBuffer();
//					for (Iterator sit = eStringSet.iterator(); sit.hasNext();) {
//						eStringsStr.append((String) sit.next());
//						if (sit.hasNext())
//							eStringsStr.append(' ');
//					}
//					docStyle.setSetting(usp.docStyleParamName, eStringsStr.toString());
//					if (DEBUG_STYLE_UPDATES) System.out.println(usp.docStyleParamName + " set to " + eStringsStr.toString());
//				}
//			}
//		}
//		
//		//	if style editor open, adjust tree path
//		if (this.docStyleEditor != null) {
//			this.docStyleEditor.setParamGroupName(docStyleParamGroupName);
//			this.docStyleEditor.setDocStyleDirty(true);
//		}
////		
////		//	index document style for saving
////		this.docStylesByDocId.put(doc.docId, docStyle);
//	}
//	
//	private static abstract class UseParamPanel extends JPanel implements Comparable {
//		final DocStyleEditor parent;
//		final String docStyleParamName;
//		final JCheckBox useParam;
//		UseParamPanel(DocStyleEditor parent, String docStyleParamName, String label, String description, boolean use) {
//			super(new BorderLayout(), true);
//			this.parent = parent;
//			this.docStyleParamName = docStyleParamName;
//			this.useParam = new JCheckBox(label, use);
//			if (description != null)
//				this.useParam.setToolTipText(description);
//			this.useParam.addItemListener(new ItemListener() {
//				public void itemStateChanged(ItemEvent ie) {
//					notifyUsageChanged();
//				}
//			});
//		}
//		public int compareTo(Object obj) {
//			return this.docStyleParamName.compareTo(((UseParamPanel) obj).docStyleParamName);
//		}
//		void setRequired(boolean required) {
//			if (required)
//				this.setExcluded(false);
//			this.useParam.setFont(this.useParam.getFont().deriveFont(required ? Font.BOLD : Font.PLAIN));
//		}
//		void setExcluded(boolean excluded) {
//			if (excluded)
//				this.setRequired(false);
//			this.setInputEnabled(!excluded);
//		}
//		abstract void setInputEnabled(boolean enabled);
//		abstract boolean isInputEnabled();
//		abstract String getValue();
//		abstract void setValue(String value);
//		boolean verifyValue(String value) {
//			return true;
//		}
//		void notifyUsageChanged() {
//			if (this.parent != null)
//				this.parent.paramUsageChanged(this);
//		}
//		void notifyActivated() {
////			System.out.println("Field '" + this.docStyleParamName + "' activated");
//			if (this.parent != null)
//				this.parent.setActiveParamPanel(this);
//		}
//		void notifyModified() {
//			if (this.parent != null) {
//				this.parent.setActiveParamPanel(this);
//				this.parent.paramValueChanged(this);
//			}
//		}
//		void notifyDeactivated() {
////			System.out.println("Field '" + this.docStyleParamName + "' deactivated");
////			BETTER STAY ON FOR VISUALIZATION, USER MIGHT WANT TO SCROLL THROUGH MAIN WINDOW, AND WE LOSE FOCUS FROM THAT
////			if (this.parent != null)
////				this.parent.setActiveParamPanel(null);
//		}
//		abstract DisplayExtensionGraphics[] getDisplayExtensionGraphics(ImPage page);
//	}
//	
//	private static class UseBooleanPanel extends UseParamPanel {
//		UseBooleanPanel(DocStyleEditor parent, String docStyleParamName, String label, String description, boolean selected) {
//			super(parent, docStyleParamName, label, description, selected);
//			this.add(this.useParam, BorderLayout.CENTER);
//			
//			this.useParam.addFocusListener(new FocusListener() {
//				public void focusGained(FocusEvent fe) {
//					notifyActivated();
//				}
//				public void focusLost(FocusEvent fe) {
//					notifyDeactivated();
//				}
//			});
//			this.useParam.addItemListener(new ItemListener() {
//				public void itemStateChanged(ItemEvent ie) {
//					notifyModified();
//				}
//			});
//		}
//		String getValue() {
//			return (this.useParam.isSelected() ? "true" : "false");
//		}
//		void setValue(String value) {
//			this.useParam.setSelected("true".equals(value));
//		}
//		void setInputEnabled(boolean enabled) {
//			this.useParam.setEnabled(enabled);
//		}
//		boolean isInputEnabled() {
//			return this.useParam.isEnabled();
//		}
//		DisplayExtensionGraphics[] getDisplayExtensionGraphics(ImPage page) {
//			if (!this.useParam.isSelected())
//				return NO_DISPLAY_EXTENSION_GRAPHICS;
//			if (this.docStyleParamName.endsWith(".bold") || this.docStyleParamName.endsWith("Bold"))
//				return getFontStyleVisualizationGraphics(this.parent, page, ImWord.BOLD_ATTRIBUTE);
//			if (this.docStyleParamName.endsWith(".italics") || this.docStyleParamName.endsWith("Italics"))
//				return getFontStyleVisualizationGraphics(this.parent, page, ImWord.ITALICS_ATTRIBUTE);
//			if (this.docStyleParamName.endsWith(".allCaps") || this.docStyleParamName.endsWith("AllCaps"))
//				return getFontStyleVisualizationGraphics(this.parent, page, "allCaps");
//			return NO_DISPLAY_EXTENSION_GRAPHICS;
//		}
//	}
//	
//	private static abstract class UseStringPanel extends UseParamPanel {
//		JTextField string;
//		UseStringPanel(DocStyleEditor parent, String docStyleParamName, String label, String description, boolean selected, String string, boolean escapePattern, boolean scroll) {
//			super(parent, docStyleParamName, label, description, selected);
//			final String localDspn = this.docStyleParamName.substring(this.docStyleParamName.lastIndexOf('.') + ".".length());
//			this.add(this.useParam, BorderLayout.WEST);
//			
//			if (localDspn.equals("pattern") || localDspn.endsWith("Pattern")) {
//				if (escapePattern)
//					string = buildPattern(string);
//				JButton testButton = new JButton("Test");
//				testButton.setBorder(BorderFactory.createRaisedBevelBorder());
//				testButton.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent ae) {
//						String pattern = UseStringPanel.this.string.getText().trim();
//						if (pattern.length() == 0)
//							return;
//						if (localDspn.equals("linePattern") || localDspn.endsWith("LinePattern"))
//							testLinePattern(pattern, getTestDoc());
//						else testPattern(pattern, getTestDocTokens());
//					}
//				});
//				this.add(testButton, BorderLayout.EAST);
//				//	TODO add button opening GGE pattern editor in sub dialog (helps understand them suckers)
//			}
//			
//			this.string = new JTextField(string) {
//				private int colWidth = -1;
//				private int rowHeight = -1;
//				public Dimension getPreferredSize() {
//					if (this.colWidth == -1)
//						this.colWidth = this.getFontMetrics(this.getFont()).charWidth('m');
//					if (this.rowHeight == -1)
//						this.rowHeight = this.getFontMetrics(this.getFont()).getHeight();
//					Dimension ps = super.getPreferredSize();
//					ps.width = ((this.getDocument().getLength() * this.colWidth) + this.getInsets().left + this.getInsets().right);
//					ps.height = Math.max(ps.height, (this.rowHeight + this.getInsets().top + this.getInsets().bottom));
//					return ps;
//				}
//				public void setFont(Font f) {
//					super.setFont(f);
//					this.colWidth = -1;
//					this.rowHeight = -1;
//				}
//			};
//			this.string.setFont(UIManager.getFont("TextArea.font")); // we want these to be the same ...
//			this.string.setBorder(BorderFactory.createLoweredBevelBorder());
//			this.string.setPreferredSize(new Dimension(Math.max(this.string.getWidth(), (this.string.getFont().getSize() * string.length())), this.string.getHeight()));
//			this.string.addFocusListener(new FocusListener() {
//				private String oldValue = null;
//				public void focusGained(FocusEvent fe) {
//					this.oldValue = UseStringPanel.this.string.getText().trim();
//					notifyActivated();
//				}
//				public void focusLost(FocusEvent fe) {
//					String value = UseStringPanel.this.string.getText().trim();
//					if (!value.equals(this.oldValue))
//						stringChanged(value);
//					this.oldValue = null;
//					notifyDeactivated();
//				}
//			});
//			this.string.getDocument().addDocumentListener(new DocumentListener() {
//				public void insertUpdate(DocumentEvent de) {
//					notifyModified();
//				}
//				public void removeUpdate(DocumentEvent de) {
//					notifyModified();
//				}
//				public void changedUpdate(DocumentEvent de) {}
//			});
//			
//			JComponent stringField = this.string;
//			if (scroll) {
//				JScrollPane stringBox = new JScrollPane(this.string) {
//					public Dimension getPreferredSize() {
//						Dimension ps = super.getPreferredSize();
//						ps.height = (UseStringPanel.this.string.getPreferredSize().height + this.getHorizontalScrollBar().getPreferredSize().height + 5);
//						return ps;
//					}
//				};
//				stringBox.setViewportBorder(null);
//				stringBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
//				stringBox.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
//				stringField = stringBox;
//			}
//			this.add(stringField, BorderLayout.CENTER);
//		}
//		ImTokenSequence getTestDocTokens() {
//			return null;
//		}
//		ImDocument getTestDoc() {
//			return null;
//		}
//		void setInputEnabled(boolean enabled) {
//			this.useParam.setEnabled(enabled);
//			this.string.setEnabled(enabled);
//		}
//		boolean isInputEnabled() {
//			return this.useParam.isEnabled();
//		}
//		String getValue() {
//			return this.string.getText().trim();
//		}
//		void setValue(String value) {
//			this.string.setText(value);
//			if (value.length() != 0)
//				this.useParam.setSelected(true);
//		}
//		void stringChanged(String string) {}
//	}
//	
//	private static String buildPattern(String string) {
//		StringBuffer pString = new StringBuffer();
//		for (int c = 0; c < string.length(); c++) {
//			char ch = string.charAt(c);
//			if ((ch < 33) || (ch == 160))
//				pString.append("\\s*"); // turn all control characters into spaces, along with non-breaking space
//			else if (ch < 127)
//				pString.append((Character.isLetterOrDigit(ch) ? "" : "\\") + ch); // no need to normalize basic ASCII characters, nor escaping letters and digits
//			else if ("-\u00AD\u2010\u2011\u2012\u2013\u2014\u2015\u2212".indexOf(ch) != -1)
//				pString.append("\\-"); // normalize dashes right here
//			else pString.append(StringUtils.getNormalForm(ch));
//		}
//		replace(pString, "\\s**", "\\s*");
//		replace(pString, "\\s*\\s*", "\\s*");
//		string = pString.toString();
//		string = string.replaceAll("[1-9][0-9]*", "[1-9][0-9]*");
//		return string;
//	}
//	
//	private static void replace(StringBuffer sb, String toReplace, String replacement) {
//		for (int s; (s = sb.indexOf(toReplace)) != -1;)
//			sb.replace(s, (s + toReplace.length()), replacement);
//	}
//	
//	private static void testPattern(String pattern, ImTokenSequence docTokens) {
//		try {
//			Annotation[] annotations = Gamta.extractAllMatches(docTokens, pattern, 64, false, false, false);
//			if (annotations != null) {
//				Window topWindow = DialogPanel.getTopWindow();
//				AnnotationDisplayDialog add;
//				if (topWindow instanceof JFrame)
//					add = new AnnotationDisplayDialog(((JFrame) topWindow), "Matches of Pattern", annotations, true);
//				else if (topWindow instanceof JDialog)
//					add = new AnnotationDisplayDialog(((JDialog) topWindow), "Matches of Pattern", annotations, true);
//				else add = new AnnotationDisplayDialog(((JFrame) null), "Matches of Pattern", annotations, true);
//				add.setLocationRelativeTo(topWindow);
//				add.setVisible(true);
//			}
//		}
//		catch (PatternSyntaxException pse) {
//			JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), ("The pattern is not valid:\n" + pse.getMessage()), "Pattern Validation Error", JOptionPane.ERROR_MESSAGE);
//		}
//	}
//	
//	private static void testLinePattern(String pattern, ImDocument doc) {
//		try {
//			LinePattern lp = LinePattern.parsePattern(pattern);
//			ImPage[] pages = doc.getPages();
//			ArrayList matchLineAnnots = new ArrayList();
//			for (int p = 0; p < pages.length; p++) {
//				ImRegion[] matchLines = lp.getMatches(pages[p]);
//				for (int l = 0; l < matchLines.length; l++) {
//					ImDocumentRoot matchLineDoc = new ImDocumentRoot(matchLines[l], (ImDocumentRoot.NORMALIZATION_LEVEL_RAW | ImDocumentRoot.NORMALIZE_CHARACTERS));
//					matchLineAnnots.add(matchLineDoc.addAnnotation(ImRegion.LINE_ANNOTATION_TYPE, 0, matchLineDoc.size()));
//				}
//			}
//			Annotation[] annotations = ((Annotation[]) matchLineAnnots.toArray(new Annotation[matchLineAnnots.size()]));
//			Window topWindow = DialogPanel.getTopWindow();
//			AnnotationDisplayDialog add;
//			if (topWindow instanceof JFrame)
//				add = new AnnotationDisplayDialog(((JFrame) topWindow), "Matches of Line Pattern", annotations, true);
//			else if (topWindow instanceof JDialog)
//				add = new AnnotationDisplayDialog(((JDialog) topWindow), "Matches of Line Pattern", annotations, true);
//			else add = new AnnotationDisplayDialog(((JFrame) null), "Matches of Line Pattern", annotations, true);
//			add.setLocationRelativeTo(topWindow);
//			add.setVisible(true);
//		}
//		catch (PatternSyntaxException pse) {
//			JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), ("The pattern is not valid:\n" + pse.getMessage()), "Pattern Validation Error", JOptionPane.ERROR_MESSAGE);
//		}
//	}
//	
//	private static class UseStringOptionPanel extends UseParamPanel {
//		JComboBox string;
//		UseStringOptionPanel(DocStyleEditor parent, String docStyleParamName, String[] values, String[] valueLabels, String label, String description, boolean selected, String string, boolean escapePattern) {
//			super(parent, docStyleParamName, label, description, selected);
//			this.add(this.useParam, BorderLayout.WEST);
//			
////			StringOption[] options = new StringOption[values.length];
////			for (int v = 0; v < values.length; v++)
////				options[v] = new StringOption(values[v], valueLabels[v]);
////			
////			this.string = new JComboBox(options);
////			this.string.setEditable(false);
//			
//			StringOption[] options;
//			if ((values.length != 0) && "".equals(values[0])) {
//				options = new StringOption[values.length - 1];
//				for (int v = 1; v < values.length; v++)
//					options[v-1] = new StringOption(values[v], valueLabels[v]);
//			}
//			else {
//				options = new StringOption[values.length];
//				for (int v = 0; v < values.length; v++)
//					options[v] = new StringOption(values[v], valueLabels[v]);
//			}
//			
//			this.string = new JComboBox(options);
//			this.string.setEditable(options.length < values.length);
//			this.string.setBorder(BorderFactory.createLoweredBevelBorder());
//			this.string.setPreferredSize(new Dimension(Math.max(this.string.getWidth(), (this.string.getFont().getSize() * string.length())), this.string.getHeight()));
//			this.string.setSelectedItem(new StringOption(string, null));
//			this.string.addFocusListener(new FocusListener() {
//				private String oldValue = null;
//				public void focusGained(FocusEvent fe) {
//					this.oldValue = UseStringOptionPanel.this.getValue();
//					notifyActivated();
//				}
//				public void focusLost(FocusEvent fe) {
//					String value = UseStringOptionPanel.this.getValue();
//					if (!value.equals(this.oldValue))
//						stringChanged(value);
//					this.oldValue = null;
//					notifyDeactivated();
//				}
//			});
//			this.string.addItemListener(new ItemListener() {
//				public void itemStateChanged(ItemEvent ie) {
//					stringChanged(UseStringOptionPanel.this.getValue());
//					notifyModified();
//				}
//			});
//			if (this.string.isEditable()) try {
//				final JTextComponent str = ((JTextComponent) this.string.getEditor().getEditorComponent());
//				str.getDocument().addDocumentListener(new DocumentListener() {
//					public void insertUpdate(DocumentEvent de) {
//						this.fireActionEventUnlessEmpty();
//					}
//					public void removeUpdate(DocumentEvent de) {
//						this.fireActionEventUnlessEmpty();
//					}
//					private void fireActionEventUnlessEmpty() {
//						String text = str.getText();
//						if (text.length() != 0)
//							UseStringOptionPanel.this.string.actionPerformed(new ActionEvent(str, ActionEvent.ACTION_PERFORMED, text, EventQueue.getMostRecentEventTime(), 0));
//					}
//					public void changedUpdate(DocumentEvent de) {}
//				});
//			}
//			catch (Exception e) {
//				System.out.println("Error wiring combo box editor: " + e.getMessage());
//				e.printStackTrace(System.out);
//			}
//			
//			this.add(this.string, BorderLayout.CENTER);
//		}
//		void setInputEnabled(boolean enabled) {
//			this.useParam.setEnabled(enabled);
//			this.string.setEnabled(enabled);
//		}
//		boolean isInputEnabled() {
//			return this.useParam.isEnabled();
//		}
//		String getValue() {
//			Object vObj = this.string.getSelectedItem();
////			return ((vObj == null) ? "" : ((StringOption) vObj).value);
//			if (vObj == null)
//				return "";
//			else if (vObj instanceof StringOption)
//				return ((StringOption) vObj).value;
//			else return vObj.toString();
//		}
//		void setValue(String value) {
////			this.string.setSelectedItem(new StringOption(value, null));
//			if (this.string.isEditable()) {
//				int vIndex = -1;
//				for (int i = 0; i < this.string.getItemCount(); i++) {
//					Object vObj = this.string.getItemAt(i);
//					if ((vObj instanceof StringOption) && ((StringOption) vObj).value.equals(value)) {
//						vIndex = i;
//						break;
//					}
//				}
//				if (vIndex == -1)
//					this.string.setSelectedItem(value);
//				else this.string.setSelectedIndex(vIndex);
//			}
//			else this.string.setSelectedItem(new StringOption(value, null));
//			if (value.length() != 0)
//				this.useParam.setSelected(true);
//		}
//		void stringChanged(String string) {}
//		DisplayExtensionGraphics[] getDisplayExtensionGraphics(ImPage page) {
//			return NO_DISPLAY_EXTENSION_GRAPHICS; // no way of visualizing choice generically
//		}
//		private static class StringOption {
//			final String value;
//			final String label;
//			StringOption(String value, String label) {
//				this.value = value;
//				this.label = ((label == null) ? value : label);
//			}
//			public boolean equals(Object obj) {
//				return ((obj instanceof StringOption) && this.value.equals(((StringOption) obj).value));
//			}
//			public String toString() {
//				return this.label; // need to show the label
//			}
//		}
//	}
//	
//	private static abstract class UseListPanel extends UseParamPanel {
//		JTextArea list;
//		UseListPanel(DocStyleEditor parent, String docStyleParamName, String label, String description, boolean selected, String string, boolean scroll) {
//			super(parent, docStyleParamName, label, description, selected);
//			final String localDspn = this.docStyleParamName.substring(this.docStyleParamName.lastIndexOf('.') + ".".length());
//			this.add(this.useParam, BorderLayout.WEST);
//			
//			if (localDspn.equals("patterns") || localDspn.endsWith("Patterns")) {
//				JButton testButton = new JButton("Test");
//				testButton.setBorder(BorderFactory.createRaisedBevelBorder());
//				testButton.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent ae) {
//						String pattern = UseListPanel.this.list.getSelectedText();
//						if (pattern == null)
//							return;
//						pattern = pattern.trim();
//						if (pattern.length() == 0)
//							return;
//						if (localDspn.equals("linePatterns") || localDspn.endsWith("LinePatterns"))
//							testLinePattern(pattern, getTestDoc());
//						else testPattern(pattern, getTestDocTokens());
//					}
//				});
//				this.add(testButton, BorderLayout.EAST);
//				//	TODO add button opening GGE pattern editor in sub dialog (helps understand them suckers)
//			}
//			
//			this.list = new JTextArea((string == null) ? "" : string.trim().replaceAll("\\s+", "\r\n"));
//			this.list.setBorder(BorderFactory.createLoweredBevelBorder());
//			this.list.addFocusListener(new FocusListener() {
//				private String oldValue = null;
//				public void focusGained(FocusEvent fe) {
//					this.oldValue = UseListPanel.this.list.getText().trim();
//					notifyActivated();
//				}
//				public void focusLost(FocusEvent fe) {
//					String value = UseListPanel.this.list.getText().trim();
//					if (!value.equals(this.oldValue))
//						stringChanged(value);
//					this.oldValue = null;
//					notifyDeactivated();
//				}
//			});
//			this.list.getDocument().addDocumentListener(new DocumentListener() {
//				int listLineCount = UseListPanel.this.list.getLineCount();
//				public void insertUpdate(DocumentEvent de) {
//					int listLineCount = UseListPanel.this.list.getLineCount();
//					if (listLineCount != this.listLineCount) {
//						this.listLineCount = listLineCount;
//						if (UseListPanel.this.parent != null) {
//							UseListPanel.this.parent.paramPanel.validate();
//							UseListPanel.this.parent.paramPanel.repaint();
//						}
//					}
//					notifyModified();
//				}
//				public void removeUpdate(DocumentEvent de) {
//					int listLineCount = UseListPanel.this.list.getLineCount();
//					if (listLineCount != this.listLineCount) {
//						this.listLineCount = listLineCount;
//						if (UseListPanel.this.parent != null) {
//							UseListPanel.this.parent.paramPanel.validate();
//							UseListPanel.this.parent.paramPanel.repaint();
//						}
//					}
//					notifyModified();
//				}
//				public void changedUpdate(DocumentEvent de) {}
//			});
//			
//			JComponent listField = this.list;
//			if (scroll) {
//				JScrollPane listBox = new JScrollPane(this.list) {
//					public Dimension getPreferredSize() {
//						Dimension ps = super.getPreferredSize();
//						ps.height = (UseListPanel.this.list.getPreferredSize().height + this.getHorizontalScrollBar().getPreferredSize().height + 5);
//						return ps;
//					}
//				};
//				listBox.setViewportBorder(null);
//				listBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
//				listBox.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
//				listField = listBox;
//			}
//			this.add(listField, BorderLayout.CENTER);
//		}
//		ImTokenSequence getTestDocTokens() {
//			return null;
//		}
//		ImDocument getTestDoc() {
//			return null;
//		}
//		void setInputEnabled(boolean enabled) {
//			this.useParam.setEnabled(enabled);
//			this.list.setEnabled(enabled);
//		}
//		boolean isInputEnabled() {
//			return this.useParam.isEnabled();
//		}
//		String getValue() {
//			return this.list.getText().trim().replaceAll("\\s+", " ");
//		}
//		void setValue(String value) {
//			this.list.setText(value.replaceAll("\\s+", "\r\n"));
//			if (value.length() != 0)
//				this.useParam.setSelected(true);
//		}
//		void stringChanged(String string) {}
//	}
//	
//	private void useBoundingBox(ImDocument doc, int pageId, Settings docStyle, String docStyleParamName, BoundingBox bounds) {
//		Class paramValueClass = this.getParamValueClass(docStyleParamName);
//		
//		//	single bounding box, expand
//		if (BoundingBox.class.getName().equals(paramValueClass.getName())) {
//			BoundingBox eBounds = BoundingBox.parse(docStyle.getSetting(docStyleParamName));
//			if (eBounds == null) {
//				docStyle.setSetting(docStyleParamName, bounds.toString());
//				if (DEBUG_STYLE_UPDATES) System.out.println(docStyleParamName + " set to " + bounds.toString());
//			}
//			else {
//				docStyle.setSetting(docStyleParamName, this.aggregateBoxes(eBounds, bounds).toString());
//				if (DEBUG_STYLE_UPDATES) System.out.println(docStyleParamName + " set to " + this.aggregateBoxes(eBounds, bounds).toString());
//			}
//		}
//		
//		//	list of bounding boxes, add new one and merge ones overlapping at least 90%
//		else if (BoundingBox.class.getName().equals(DocumentStyle.getListElementClass(paramValueClass).getName())) {
//			String boundsStr = this.getBoxListString(docStyle.getSetting(docStyleParamName), bounds);
//			docStyle.setSetting(docStyleParamName, boundsStr);
//			if (DEBUG_STYLE_UPDATES) System.out.println(docStyleParamName + " set to " + boundsStr);
//		}
//		
//		//	we have an anchor, adjust minimum page ID
//		if (docStyleParamName.startsWith("anchor.")) {
//			int maxPageId = Integer.parseInt(docStyle.getSetting("anchor.maxPageId", "0"));
//			if (maxPageId < pageId)
//				docStyle.setSetting("anchor.maxPageId", ("" + (pageId + doc.getFirstPageId())));
//		}
//		
//		//	if style editor open, adjust tree path
//		if (this.docStyleEditor != null) {
//			this.docStyleEditor.setParamGroupName(docStyleParamName.substring(0, docStyleParamName.lastIndexOf('.')));
//			this.docStyleEditor.setDocStyleDirty(true);
//		}
////		
////		//	index document style for saving
////		this.docStylesByDocId.put(doc.docId, docStyle);
//	}
//	
//	private double getBoxOverlap(BoundingBox bb1, BoundingBox bb2) {
//		if (bb1.includes(bb2, false) || bb2.includes(bb1, false))
//			return 1;
//		if (!bb1.overlaps(bb2))
//			return 0;
//		int iLeft = Math.max(bb1.left, bb2.left);
//		int iRight = Math.min(bb1.right, bb2.right);
//		int iTop = Math.max(bb1.top, bb2.top);
//		int iBottom = Math.min(bb1.bottom, bb2.bottom);
//		int iArea = ((iRight - iLeft) * (iBottom - iTop));
//		int minBbArea = Math.min(((bb1.right - bb1.left) * (bb1.bottom - bb1.top)), ((bb2.right - bb2.left) * (bb2.bottom - bb2.top)));
//		return (((double) iArea) / minBbArea);
//	}
//	
//	private BoundingBox aggregateBoxes(BoundingBox bb1, BoundingBox bb2) {
//		int left = Math.min(bb1.left, bb2.left);
//		int right = Math.max(bb1.right, bb2.right);
//		int top = Math.min(bb1.top, bb2.top);
//		int bottom = Math.max(bb1.bottom, bb2.bottom);
//		return new BoundingBox(left, right, top, bottom);
//	}
//	
//	private String getBoxListString(String eBoundsStr, BoundingBox bounds) {
//		if ((eBoundsStr == null) || (eBoundsStr.trim().length() == 0))
//			return bounds.toString();
//		
//		ArrayList boundsList = new ArrayList();
//		boundsList.add(bounds);
//		String[] eBoundsStrs = eBoundsStr.split("[^0-9\\,\\[\\]]+");
//		for (int b = 0; b < eBoundsStrs.length; b++)
//			boundsList.add(BoundingBox.parse(eBoundsStrs[b]));
//		
//		int boundsCount;
//		do {
//			boundsCount = boundsList.size();
//			BoundingBox bb1 = null;
//			BoundingBox bb2 = null;
//			double bbOverlap = 0.9; // 90% is minimum overlap for merging
//			for (int b = 0; b < boundsList.size(); b++) {
//				BoundingBox tbb1 = ((BoundingBox) boundsList.get(b));
//				if (DEBUG_STYLE_UPDATES) System.out.println("Testing for merger: " + tbb1);
//				for (int c = (b+1); c < boundsList.size(); c++) {
//					BoundingBox tbb2 = ((BoundingBox) boundsList.get(c));
//					double tbbOverlap = this.getBoxOverlap(tbb1, tbb2);
//					if (DEBUG_STYLE_UPDATES) System.out.println(" - overlap with " + tbb2 + " is " + tbbOverlap);
//					if (bbOverlap < tbbOverlap) {
//						bbOverlap = tbbOverlap;
//						bb1 = tbb1;
//						bb2 = tbb2;
//						if (DEBUG_STYLE_UPDATES) System.out.println(" ==> new best merger");
//					}
//				}
//			}
//			if ((bb1 != null) && (bb2 != null)) {
//				boundsList.remove(bb1);
//				boundsList.remove(bb2);
//				boundsList.add(this.aggregateBoxes(bb1, bb2));
//			}
//		}
//		while (boundsList.size() < boundsCount);
//		
//		StringBuffer boundsStr = new StringBuffer();
//		for (int b = 0; b < boundsList.size(); b++) {
//			if (b != 0)
//				boundsStr.append(' ');
//			boundsStr.append(((BoundingBox) boundsList.get(b)).toString());
//		}
//		return boundsStr.toString();
//	}
//	
//	private void useSelection(final ImDocument doc, int pageId, Settings docStyle, String docStyleParamGroupName, String[] docStyleParamNames, int minFontSize, int maxFontSize, boolean isBold, boolean isItalics, boolean isAllCaps, String string, BoundingBox bounds) {
//		JPanel sPanel = new JPanel(new GridLayout(0, 1, 0, 0), true);
//		
//		//	get parameter group description and group label
//		ParameterGroupDescription pgd = this.getParameterGroupDescription(docStyleParamGroupName);
//		String pgl = ((pgd == null) ? null : pgd.getLabel());
//		
//		//	if creating anchor, add name field at top, pre-filled with string less all non-letters
//		JTextField createAnchorName = null;
//		if ("anchor.<create>".equals(docStyleParamGroupName)) {
//			createAnchorName = new JTextField(DocumentStyleProvider.normalizeString(string).replaceAll("[^A-Za-z]", ""));
//			JPanel caPanel = new JPanel(new BorderLayout(), true);
//			caPanel.add(new JLabel(" Anchor Name: "), BorderLayout.WEST);
//			caPanel.add(createAnchorName, BorderLayout.CENTER);
//			sPanel.add(caPanel);
//		}
//		
//		//	ask for font properties to use
//		UseBooleanPanel useMinFontSize = null;
//		UseBooleanPanel useMaxFontSize = null;
//		if (minFontSize <= maxFontSize) {
//			int eMinFontSize = 72;
//			try {
//				eMinFontSize = Integer.parseInt(docStyle.getSetting((docStyleParamGroupName + ".minFontSize"), "72"));
//			} catch (NumberFormatException nfe) {}
//			if (minFontSize < eMinFontSize) {
//				String pl;
//				if (pgl == null)
//					pl = ("Use " + minFontSize + " as Minimum Font Size (currently " + eMinFontSize + ")");
//				else pl = ("Use " + minFontSize + " as Minimum Font Size for " + pgl + " (currently " + eMinFontSize + ")");
//				String pd = ((pgd == null) ? null : pgd.getParamDescription("minFontSize"));
//				useMinFontSize = new UseBooleanPanel(this.docStyleEditor, (docStyleParamGroupName + ".minFontSize"), pl, pd, true);
//				sPanel.add(useMinFontSize);
//			}
//			int eMaxFontSize = 0;
//			try {
//				eMaxFontSize = Integer.parseInt(docStyle.getSetting((docStyleParamGroupName + ".maxFontSize"), "0"));
//			} catch (NumberFormatException nfe) {}
//			if (eMaxFontSize < maxFontSize) {
//				String pl;
//				if (pgl == null)
//					pl = ("Use " + maxFontSize + " as Maximum Font Size (currently " + eMaxFontSize + ")");
//				else pl = ("Use " + maxFontSize + " as Maximum Font Size for " + pgl + " (currently " + eMaxFontSize + ")");
//				String pd = ((pgd == null) ? null : pgd.getParamDescription("minFontSize"));
//				useMaxFontSize = new UseBooleanPanel(this.docStyleEditor, (docStyleParamGroupName + ".maxFontSize"), pl, pd, true);
//				sPanel.add(useMaxFontSize);
//			}
//		}
//		UseBooleanPanel useIsBold = null;
//		if (isBold) {
//			for (int p = 0; p < docStyleParamNames.length; p++) {
//				if (docStyleParamGroupName.startsWith("anchor.")) {
//					useIsBold = new UseBooleanPanel(this.docStyleEditor, (docStyleParamGroupName + ".isBold"), "Require Anchor Value to be Bold", null, true);
//					break;
//				}
//				else if (docStyleParamNames[p].equals(docStyleParamGroupName + ".isBold")) {
//					String pl;
//					if (pgl == null)
//						pl = "Require Values to be Bold";
//					else pl = ("Require Values for " + pgl + " to be Bold");
//					String pd = ((pgd == null) ? null : pgd.getParamDescription("isBold"));
//					useIsBold = new UseBooleanPanel(this.docStyleEditor, (docStyleParamGroupName + ".isBold"), pl, pd, true);
//					break;
//				}
//				else if (docStyleParamNames[p].equals(docStyleParamGroupName + ".startIsBold")) {
//					String pl;
//					if (pgl == null)
//						pl = "Require Value Starts to be Bold";
//					else pl = ("Require Values for " + pgl + " to Start in Bold");
//					String pd = ((pgd == null) ? null : pgd.getParamDescription("startIsBold"));
//					useIsBold = new UseBooleanPanel(this.docStyleEditor, (docStyleParamGroupName + ".startIsBold"), pl, pd, true);
//					break;
//				}
//			}
//			if (useIsBold != null)
//				sPanel.add(useIsBold);
//		}
//		UseBooleanPanel useIsItalics = null;
//		if (isItalics) {
//			for (int p = 0; p < docStyleParamNames.length; p++) {
//				if (docStyleParamGroupName.startsWith("anchor.")) {
//					useIsItalics = new UseBooleanPanel(this.docStyleEditor, (docStyleParamGroupName + ".isItalics"), "Require Anchor Value to be in Italics", null, true);
//					break;
//				}
//				else if (docStyleParamNames[p].equals(docStyleParamGroupName + ".isItalics")) {
//					String pl;
//					if (pgl == null)
//						pl = "Require Values to be in Italics";
//					else pl = ("Require Values for " + pgl + " to be in Italics");
//					String pd = ((pgd == null) ? null : pgd.getParamDescription("isItalics"));
//					useIsItalics = new UseBooleanPanel(this.docStyleEditor, (docStyleParamGroupName + ".isItalics"), pl, pd, true);
//					break;
//				}
//				else if (docStyleParamNames[p].equals(docStyleParamGroupName + ".startIsItalics")) {
//					String pl;
//					if (pgl == null)
//						pl = "Require Values Starts to be in Italics";
//					else pl = ("Require Values for " + pgl + " to Start in Italics");
//					String pd = ((pgd == null) ? null : pgd.getParamDescription("startIsItalics"));
//					useIsItalics = new UseBooleanPanel(this.docStyleEditor, (docStyleParamGroupName + ".startIsItalics"), pl, pd, true);
//					break;
//				}
//			}
//			if (useIsItalics != null)
//				sPanel.add(useIsItalics);
//		}
//		UseBooleanPanel useIsAllCaps = null;
//		if (isAllCaps) {
//			for (int p = 0; p < docStyleParamNames.length; p++) {
//				if (docStyleParamGroupName.startsWith("anchor.")) {
//					useIsAllCaps = new UseBooleanPanel(this.docStyleEditor, (docStyleParamGroupName + ".isAllCaps"), "Require Anchor Value to be in All Caps", null, true);
//					break;
//				}
//				if (docStyleParamNames[p].equals(docStyleParamGroupName + ".isAllCaps")) {
//					String pl;
//					if (pgl == null)
//						pl = "Require Values to be in All Caps";
//					else pl = ("Require Values for " + pgl + " to be in All Caps");
//					String pd = ((pgd == null) ? null : pgd.getParamDescription("isAllCaps"));
//					useIsAllCaps = new UseBooleanPanel(this.docStyleEditor, (docStyleParamGroupName + ".isAllCaps"), pl, pd, true);
//					break;
//				}
//				else if (docStyleParamNames[p].equals(docStyleParamGroupName + ".startIsAllCaps")) {
//					String pl;
//					if (pgl == null)
//						pl = "Require Values Starts to be in All Caps";
//					else pl = ("Require Values for " + pgl + " to Start in All Caps");
//					String pd = ((pgd == null) ? null : pgd.getParamDescription("startIsAllCaps"));
//					useIsAllCaps = new UseBooleanPanel(this.docStyleEditor, (docStyleParamGroupName + ".startIsAllCaps"), pl, pd, true);
//					break;
//				}
//			}
//			if (useIsAllCaps != null)
//				sPanel.add(useIsAllCaps);
//		}
//		
//		//	collect style parameter names in argument group that use string properties, constructing string usage panels on the fly
//		TreeSet sDocStyleParamPanels = new TreeSet();
//		final ImTokenSequence[] docTokens = {null}; // using array facilitates sharing tokens and still generating them on demand
//		for (int p = 0; p < docStyleParamNames.length; p++) {
//			if (!docStyleParamNames[p].startsWith(docStyleParamGroupName + "."))
//				continue;
//			if (!checkParamValueClass(docStyleParamNames[p], String.class, true))
//				continue;
//			String localDspn = docStyleParamNames[p].substring(docStyleParamNames[p].lastIndexOf('.') + ".".length());
//			String pl = ((pgd == null) ? null : pgd.getParamLabel(localDspn));
//			if (pl == null)
//				pl = (" Use as " + localDspn);
//			else pl = (" Use as " + pl);
//			String pd = ((pgd == null) ? null : pgd.getParamDescription(localDspn));
//			sDocStyleParamPanels.add(new UseStringPanel(this.docStyleEditor, docStyleParamNames[p], pl, pd, true, string, true, true) {
//				ImTokenSequence getTestDocTokens() {
//					if (docTokens[0] == null)
//						docTokens[0] = new ImTokenSequence(((Tokenizer) doc.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER)), doc.getTextStreamHeads(), (ImTokenSequence.NORMALIZATION_LEVEL_PARAGRAPHS | ImTokenSequence.NORMALIZE_CHARACTERS));
//					return docTokens[0];
//				}
//				ImDocument getTestDoc() {
//					return doc;
//				}
//				DisplayExtensionGraphics[] getDisplayExtensionGraphics(ImPage page) {
//					if (this.docStyleParamName.endsWith(".linePattern") || this.docStyleParamName.endsWith("LinePattern"))
//						return getLinePatternVisualizationGraphics(this.parent, page, this.getValue());
//					if (this.docStyleParamName.endsWith(".pattern") || this.docStyleParamName.endsWith("Pattern"))
//						return getPatternVisualizationGraphics(this.parent, page, this.getValue());
//					//	TODO think of more
//					return NO_DISPLAY_EXTENSION_GRAPHICS;
//				}
//			});
//		}
//		if (docStyleParamGroupName.equals("anchor.<create>") && sDocStyleParamPanels.isEmpty()) {
//			sDocStyleParamPanels.add(new UseStringPanel(this.docStyleEditor, (docStyleParamGroupName + ".pattern"), "Use as Anchor Value Pattern", null, true, string, true, false) {
//				ImTokenSequence getTestDocTokens() {
//					if (docTokens[0] == null)
//						docTokens[0] = new ImTokenSequence(((Tokenizer) doc.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER)), doc.getTextStreamHeads(), (ImTokenSequence.NORMALIZATION_LEVEL_PARAGRAPHS | ImTokenSequence.NORMALIZE_CHARACTERS));
//					return docTokens[0];
//				}
//				ImDocument getTestDoc() {
//					return doc;
//				}
//				DisplayExtensionGraphics[] getDisplayExtensionGraphics(ImPage page) {
//					return getPatternVisualizationGraphics(this.parent, page, this.getValue());
//				}
//			});
//		}
//		for (Iterator ppit = sDocStyleParamPanels.iterator(); ppit.hasNext();)
//			sPanel.add((UseStringPanel) ppit.next());
//		
//		//	collect style parameter names in argument group that use bounding box properties, constructing checkboxes on the fly
//		TreeSet bbDocStyleParamPanels = new TreeSet();
//		for (int p = 0; p < docStyleParamNames.length; p++)
//			if (docStyleParamNames[p].startsWith(docStyleParamGroupName + ".")) {
//				String localDspn = docStyleParamNames[p].substring(docStyleParamNames[p].lastIndexOf('.') + ".".length());
//				if (!docStyleParamNames[p].equals(docStyleParamGroupName + "." + localDspn))
//					continue;
//				if (!checkParamValueClass(docStyleParamNames[p], BoundingBox.class, true))
//					continue;
//				String pl;
//				if (pgl == null)
//					pl = ("Use Bounding Box as " + localDspn);
//				else pl = ("Use Bounding Box as " + pgl);
//				String pd = ((pgd == null) ? null : pgd.getParamDescription(localDspn));
//				bbDocStyleParamPanels.add(new UseBooleanPanel(this.docStyleEditor, docStyleParamNames[p], pl, pd, true));
//			}
//		for (Iterator pnit = bbDocStyleParamPanels.iterator(); pnit.hasNext();)
//			sPanel.add((UseParamPanel) pnit.next());
//		if (docStyleParamGroupName.equals("anchor.<create>") && bbDocStyleParamPanels.isEmpty()) // an anchor always requires a bounding box, so we don't display the checkbox, but simply use it
//			bbDocStyleParamPanels.add(new UseBooleanPanel(this.docStyleEditor, (docStyleParamGroupName + ".area"), "Require Anchor Value to be inside Bounding Box", null, true));
//		
//		//	prompt
//		int choice = JOptionPane.showConfirmDialog(null, sPanel, ("Select Properties to Use" + ((pgl == null) ? "" : (" in " + pgl))), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
//		if (choice != JOptionPane.OK_OPTION)
//			return;
//		
//		//	if we're creating an anchor, determine lowest non-used anchor number and use that as parameter group name
//		if ("anchor.<create>".equals(docStyleParamGroupName)) {
//			String can = createAnchorName.getText().replaceAll("[^A-Za-z0-9]", "");
//			if (can.length() == 0)
//				return;
//			docStyleParamGroupName = ("anchor." + can);
//			int maxPageId = Integer.parseInt(docStyle.getSetting("anchor.maxPageId", "0"));
//			if (maxPageId < pageId)
//				docStyle.setSetting("anchor.maxPageId", ("" + (pageId + doc.getFirstPageId())));
//		}
//		
//		//	we have an anchor, adjust minimum page ID
//		else if (docStyleParamGroupName.startsWith("anchor.")) {
//			int maxPageId = Integer.parseInt(docStyle.getSetting("anchor.maxPageId", "0"));
//			if (maxPageId < pageId)
//				docStyle.setSetting("anchor.maxPageId", ("" + (pageId + doc.getFirstPageId())));
//		}
//		
//		//	set font properties
//		if ((useMinFontSize != null) && useMinFontSize.useParam.isSelected()) {
//			docStyle.setSetting((docStyleParamGroupName + ".minFontSize"), ("" + minFontSize));
//			if (DEBUG_STYLE_UPDATES) System.out.println(docStyleParamGroupName + ".minFontSize set to " + minFontSize);
//		}
//		if ((useMaxFontSize != null) && useMaxFontSize.useParam.isSelected()) {
//			docStyle.setSetting((docStyleParamGroupName + ".maxFontSize"), ("" + maxFontSize));
//			if (DEBUG_STYLE_UPDATES) System.out.println(docStyleParamGroupName + ".maxFontSize set to " + maxFontSize);
//		}
//		if ((minFontSize == maxFontSize) && (useMinFontSize != null) && useMinFontSize.useParam.isSelected() && (useMaxFontSize != null) && useMaxFontSize.useParam.isSelected()) {
//			docStyle.setSetting((docStyleParamGroupName + ".fontSize"), ("" + minFontSize));
//			if (DEBUG_STYLE_UPDATES) System.out.println(docStyleParamGroupName + ".fontSize set to " + minFontSize);
//		}
//		if ((useIsBold != null) && useIsBold.useParam.isSelected()) {
//			docStyle.setSetting((docStyleParamGroupName + useIsBold.docStyleParamName.substring(useIsBold.docStyleParamName.lastIndexOf('.'))), "true");
//			if (DEBUG_STYLE_UPDATES) System.out.println((docStyleParamGroupName + useIsBold.docStyleParamName.substring(useIsBold.docStyleParamName.lastIndexOf('.'))) + " set to true");
//		}
//		if ((useIsItalics != null) && useIsItalics.useParam.isSelected()) {
//			docStyle.setSetting((docStyleParamGroupName + useIsItalics.docStyleParamName.substring(useIsItalics.docStyleParamName.lastIndexOf('.'))), "true");
//			if (DEBUG_STYLE_UPDATES) System.out.println((docStyleParamGroupName + useIsItalics.docStyleParamName.substring(useIsItalics.docStyleParamName.lastIndexOf('.'))) + " set to true");
//		}
//		if ((useIsAllCaps != null) && useIsAllCaps.useParam.isSelected()) {
//			docStyle.setSetting((docStyleParamGroupName + useIsAllCaps.docStyleParamName.substring(useIsAllCaps.docStyleParamName.lastIndexOf('.'))), "true");
//			if (DEBUG_STYLE_UPDATES) System.out.println((docStyleParamGroupName + useIsAllCaps.docStyleParamName.substring(useIsAllCaps.docStyleParamName.lastIndexOf('.'))) + " set to true");
//		}
//		
//		//	set string parameters
//		for (Iterator ppit = sDocStyleParamPanels.iterator(); ppit.hasNext();) {
//			UseStringPanel usp = ((UseStringPanel) ppit.next());
//			if (!usp.useParam.isSelected())
//				continue;
//			string = usp.string.getText().trim();
//			if (string.length() == 0)
//				continue;
//			
//			String docStyleParamName = usp.docStyleParamName;
//			if ("anchor.<create>.pattern".equals(usp.docStyleParamName))
//				docStyleParamName = (docStyleParamGroupName + ".pattern");
//			Class paramValueClass = this.getParamValueClass(docStyleParamName);
//			
//			if (String.class.getName().equals(paramValueClass.getName())) {
//				docStyle.setSetting(docStyleParamName, string);
//				if (DEBUG_STYLE_UPDATES) System.out.println(docStyleParamName + " set to " + string);
//			}
//			else if (String.class.getName().equals(DocumentStyle.getListElementClass(paramValueClass).getName())) {
//				String eString = docStyle.getSetting(docStyleParamName, "").trim();
//				if (eString.length() == 0) {
//					docStyle.setSetting(docStyleParamName, string);
//					if (DEBUG_STYLE_UPDATES) System.out.println(docStyleParamName + " set to " + string);
//				}
//				else {
//					TreeSet eStringSet = new TreeSet(Arrays.asList(eString.split("\\s+")));
//					eStringSet.add(string);
//					StringBuffer eStringsStr = new StringBuffer();
//					for (Iterator sit = eStringSet.iterator(); sit.hasNext();) {
//						eStringsStr.append((String) sit.next());
//						if (sit.hasNext())
//							eStringsStr.append(' ');
//					}
//					docStyle.setSetting(docStyleParamName, eStringsStr.toString());
//					if (DEBUG_STYLE_UPDATES) System.out.println(docStyleParamName + " set to " + eStringsStr.toString());
//				}
//			}
//		}
//		
//		//	set bounding box properties
//		for (Iterator bbdspnit = bbDocStyleParamPanels.iterator(); bbdspnit.hasNext();) {
//			UseBooleanPanel useBbDsp = ((UseBooleanPanel) bbdspnit.next());
//			if (!useBbDsp.useParam.isSelected())
//				continue;
//			
//			String docStyleParamName = useBbDsp.docStyleParamName;
//			if ("anchor.<create>.area".equals(docStyleParamName))
//				docStyleParamName = (docStyleParamGroupName + ".area");
//			Class paramValueClass = this.getParamValueClass(docStyleParamName);
//			
//			//	single bounding box, expand
//			if (BoundingBox.class.getName().equals(paramValueClass.getName())) {
//				BoundingBox eBounds = BoundingBox.parse(docStyle.getSetting(docStyleParamName));
//				if (eBounds == null) {
//					docStyle.setSetting(docStyleParamName, bounds.toString());
//					if (DEBUG_STYLE_UPDATES) System.out.println(docStyleParamName + " set to " + bounds.toString());
//				}
//				else {
//					docStyle.setSetting(docStyleParamName, this.aggregateBoxes(eBounds, bounds).toString());
//					if (DEBUG_STYLE_UPDATES) System.out.println(docStyleParamName + " set to " + this.aggregateBoxes(eBounds, bounds).toString());
//				}
//			}
//			
//			//	list of bounding boxes, add new one and merge ones overlapping at least 90%
//			else if (BoundingBox.class.getName().equals(DocumentStyle.getListElementClass(paramValueClass).getName())) {
//				String boundsStr = this.getBoxListString(docStyle.getSetting(docStyleParamName), bounds);
//				docStyle.setSetting(docStyleParamName, boundsStr);
//				if (DEBUG_STYLE_UPDATES) System.out.println(docStyleParamName + " set to " + boundsStr);
//			}
//		}
//		
//		//	if style editor open, adjust tree path
//		if (this.docStyleEditor != null) {
//			this.docStyleEditor.setParamGroupName(docStyleParamGroupName);
//			this.docStyleEditor.setDocStyleDirty(true);
//		}
//	}
//	
//	private Settings getDocStyle(String docStyleName) {
//		Settings docStyle = this.styleProvider.getStyle(docStyleName);
//		if (docStyle == null) {
//			docStyle = new Settings();
//			docStyle.setSetting(DocumentStyle.DOCUMENT_STYLE_NAME_ATTRIBUTE, docStyleName);
//		}
//		return docStyle;
//	}
//	
//	private static final String CREATE_DOC_STYLE = "<Create Document Style>";
//	
//	private SelectionAction[] getAssignDocStyleAction(final ImDocument doc) {
//		SelectionAction[] adsa = {new SelectionAction("styleAssign", "Assign Document Style", "Assign a style template to, or create one for this document to help with markup automation") {
//			public boolean performAction(ImDocumentMarkupPanel invoker) {
//				
//				final JTextField cDocStyleOrigin = new JTextField();
//				final JTextField cDocStyleYear = new JTextField();
//				final JComboBox cDocStylePubType = new JComboBox(refTypeSystem.getBibRefTypeNames());
//				cDocStylePubType.setEditable(false);
//				
//				final JPanel cDocStylePanel = new JPanel(new GridLayout(0, 2, 3, 3), true);
//				cDocStylePanel.setBorder(BorderFactory.createEtchedBorder());
//				cDocStylePanel.add(new JLabel(" Journal / Publisher: "));
//				cDocStylePanel.add(cDocStyleOrigin);
//				cDocStylePanel.add(new JLabel(" From Year: "));
//				cDocStylePanel.add(cDocStyleYear);
//				cDocStylePanel.add(new JLabel(" Publication Type: "));
//				cDocStylePanel.add(cDocStylePubType);
//				
//				String[] docStyleNames = styleProvider.getResourceNames();
//				for (int s = 0; s < docStyleNames.length; s++)
//					docStyleNames[s] = docStyleNames[s].substring(0, docStyleNames[s].lastIndexOf('.'));
//				
//				final JComboBox docStyleSelector = new JComboBox(docStyleNames);
//				docStyleSelector.insertItemAt(CREATE_DOC_STYLE, 0);
//				docStyleSelector.setSelectedItem(CREATE_DOC_STYLE);
//				docStyleSelector.setEditable(false);
//				docStyleSelector.addItemListener(new ItemListener() {
//					public void itemStateChanged(ItemEvent ie) {
//						cDocStylePanel.setEnabled(CREATE_DOC_STYLE.equals(docStyleSelector.getSelectedItem()));
//						cDocStyleOrigin.setEnabled(CREATE_DOC_STYLE.equals(docStyleSelector.getSelectedItem()));
//						cDocStyleYear.setEnabled(CREATE_DOC_STYLE.equals(docStyleSelector.getSelectedItem()));
//						cDocStylePubType.setEnabled(CREATE_DOC_STYLE.equals(docStyleSelector.getSelectedItem()));
//					}
//				});
//				
//				JPanel docStyleSelectorPanel = new JPanel(new BorderLayout(), true);
//				docStyleSelectorPanel.add(new JLabel("Select Document Style: "), BorderLayout.WEST);
//				docStyleSelectorPanel.add(docStyleSelector, BorderLayout.CENTER);
//				docStyleSelectorPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 4, 0, docStyleSelectorPanel.getBackground()));
//				
//				JPanel docStylePanel = new JPanel(new BorderLayout(), true);
//				docStylePanel.add(docStyleSelectorPanel, BorderLayout.NORTH);
//				docStylePanel.add(cDocStylePanel, BorderLayout.CENTER);
//				
//				int choice = JOptionPane.showConfirmDialog(DialogPanel.getTopWindow(), docStylePanel, "Select or Create Document Style", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
//				if (choice != JOptionPane.OK_OPTION)
//					return false;
//				
//				String selectedDocStyleName = ((String) docStyleSelector.getSelectedItem());
//				if (CREATE_DOC_STYLE.equals(selectedDocStyleName)) {
//					String origin = cDocStyleOrigin.getText().trim();
//					if (origin.length() == 0)
//						return false;
//					origin = origin.toLowerCase().replaceAll("[^A-Za-z0-9\\-]+", "_");
//					String year = cDocStyleYear.getText().trim();
//					if ((year.length() == 0) || !year.matches("[0-9]{4}"))
//						year = "0000";
//					String pubType = ((String) cDocStylePubType.getSelectedItem());
//					pubType = pubType.toLowerCase().replaceAll("\\s+", "_");
//					selectedDocStyleName = (origin + "." + year + "." + pubType);
//				}
//				doc.setAttribute(DocumentStyle.DOCUMENT_STYLE_NAME_ATTRIBUTE, selectedDocStyleName);
//				DocumentStyle.getStyleFor(doc);
//				
//				return true; // we _did_ change the document, if only by assigning attributes
//			}
//		}};
//		return adsa;
//	}
//	
//	private SelectionAction getEditDocStyleAction(final ImDocument doc, final String docStyleName, final Settings docStyle) {
//		return new SelectionAction("styleEdit", "Edit Document Style", "Open the style template assigned to this document for editing") {
//			public boolean performAction(ImDocumentMarkupPanel invoker) {
//				if (docStyleEditor == null) {
//					docStyleEditor = new DocStyleEditor();
//					docStyleEditorDisplayExtension = new DisplayExtension[1];
//					docStyleEditorDisplayExtension[0] = docStyleEditor;
//				}
//				docStyleEditor.setDocStyle(doc, docStyleName, docStyle);
//				ggImagine.notifyDisplayExtensionsModified(null);
//				docStyleEditor.setVisible(true);
//				return false;
//			}
//		};
//	}
//	
//	private DocStyleEditor docStyleEditor = null;
//	private DisplayExtension[] docStyleEditorDisplayExtension = null;
//	
//	/**
//	 * @author sautter
//	 */
//	private class DocStyleEditor extends DialogPanel implements DisplayExtension {
//		private JTree paramTree = new JTree();
//		private JPanel paramPanel = new JPanel(new BorderLayout(), true);
//		private JButton saveDocStyleButton = new JButton("Save Document Style Template");
//		private int toolTipCloseDelay = ToolTipManager.sharedInstance().getDismissDelay();
//		DocStyleEditor() {
//			super("Edit Document Style Template", false);
//			
//			this.addWindowListener(new WindowAdapter() {
//				public void windowClosing(WindowEvent we) {
//					askSaveIfDirty();
//					docStyleEditor = null; // make way on closing
//					docStyleEditorDisplayExtension = null;
//					ggImagine.notifyDisplayExtensionsModified(null);
//					ToolTipManager.sharedInstance().setDismissDelay(toolTipCloseDelay); // revert tooltip behavior to normal
//				}
//			});
//			ToolTipManager.sharedInstance().setDismissDelay(10 * 60 * 1000); // make sure tooltips remain open long enough for reading explanations
//			
//			TreeSelectionModel ptsm = new DefaultTreeSelectionModel();
//			ptsm.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
//			this.paramTree.setSelectionModel(ptsm);
//			this.paramTree.addTreeSelectionListener(new TreeSelectionListener() {
//				public void valueChanged(TreeSelectionEvent tse) {
//					paramTreeNodeSelected((ParamTreeNode) tse.getPath().getLastPathComponent());
//				}
//			});
//			this.paramTree.setModel(this.paramTreeModel);
//			this.paramTree.setRootVisible(true);
//			this.paramTree.setCellRenderer(new ParamTreeCellRenderer());
//			
//			this.saveDocStyleButton.setBorder(BorderFactory.createRaisedBevelBorder());
//			this.saveDocStyleButton.setEnabled(false);
//			this.saveDocStyleButton.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					saveDocStyle();
//				}
//			});
//			
//			JScrollPane paramTreeBox = new JScrollPane(this.paramTree);
//			paramTreeBox.getHorizontalScrollBar().setBlockIncrement(20);
//			paramTreeBox.getHorizontalScrollBar().setUnitIncrement(20);
//			paramTreeBox.getVerticalScrollBar().setBlockIncrement(20);
//			paramTreeBox.getVerticalScrollBar().setUnitIncrement(20);
//			
//			JSplitPane paramSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, paramTreeBox, this.paramPanel);
//			
//			this.add(paramSplit, BorderLayout.CENTER);
//			this.add(this.saveDocStyleButton, BorderLayout.SOUTH);
//			this.setSize(400, 600);
//			this.setLocationRelativeTo(this.getOwner());
//		}
//		
//		String docStyleName = null;
//		private Settings docStyle = null;
//		private boolean docStyleDirty = false;
//		
//		private ImDocument testDoc;
//		private ImTokenSequence testDocTokens;
//		
//		void setDocStyle(ImDocument doc, String docStyleName, Settings docStyle) {
//			
//			//	update test document
//			if (this.testDoc != doc) {
//				this.testDoc = doc;
//				this.testDocTokens = null;
//			}
//			
//			//	document style remains, we're done here
//			if (docStyleName.equals(this.docStyleName))
//				return;
//			
//			//	save any modifications to previously open document style
//			this.askSaveIfDirty();
//			
//			//	update data fields
//			this.docStyleName = docStyleName;
//			this.docStyle = docStyle;
//			this.setDocStyleDirty(false);
//			
//			//	clear index fields
//			this.paramGroupName = null;
//			this.paramValueFields.clear();
//			
//			//	update window title
//			this.setTitle("Edit Document Style Template '" + this.docStyleName + "'");
//			
//			//	get available parameter names, including ones from style proper (anchors !!!)
//			TreeSet dsParamNameSet = new TreeSet(Arrays.asList(parameterValueClassNames.getKeys()));
//			String[] dDsParamNames = docStyle.getKeys();
//			for (int p = 0; p < dDsParamNames.length; p++) {
//				if (dDsParamNames[p].startsWith("anchor."))
//					dsParamNameSet.add(dDsParamNames[p]);
//			}
//			
//			//	make sure we can create anchors
//			dsParamNameSet.add("anchor.<create>.dummy");
//			dsParamNameSet.add("anchor.maxPageId");
//			
//			//	line up parameter names
//			String[] dsParamNames = ((String[]) dsParamNameSet.toArray(new String[dsParamNameSet.size()]));
//			Arrays.sort(dsParamNames);
//			
//			//	update parameter tree
//			this.paramTreeRoot.clearChildren();
//			this.paramTreeNodesByPrefix.clear();
//			this.paramTreeNodesByPrefix.put(this.paramTreeRoot.prefix, this.paramTreeRoot);
//			LinkedList ptnStack = new LinkedList();
//			ptnStack.add(this.paramTreeRoot);
//			for (int p = 0; p < dsParamNames.length; p++) {
//				
//				//	get current parent
//				ParamTreeNode pptn = ((ParamTreeNode) ptnStack.getLast());
//				
//				//	ascend until prefix matches
//				while ((pptn != this.paramTreeRoot) && !dsParamNames[p].startsWith(pptn.prefix + ".")) {
//					ptnStack.removeLast();
//					pptn = ((ParamTreeNode) ptnStack.getLast());
//				}
//				
//				//	add more intermediate nodes for steps of current parameter
//				while (pptn.prefix.length() < dsParamNames[p].lastIndexOf('.')) {
//					ParamTreeNode ptn = new ParamTreeNode(dsParamNames[p].substring(0, dsParamNames[p].indexOf('.', (pptn.prefix.length() + 1))), pptn);
//					pptn.addChild(ptn);
//					pptn = ptn;
//					ptnStack.addLast(pptn);
//				}
//				
//				//	add parameter to parent tree node
//				if (!"anchor.<create>.dummy".equals(dsParamNames[p]))
//					pptn.addParamName(dsParamNames[p]);
//			}
//			
//			//	update display
//			this.updateParamTree();
//		}
//		
//		void setDocStyleDirty(boolean dirty) {
//			this.docStyleDirty = dirty;
//			this.saveDocStyleButton.setEnabled(dirty);
//		}
//		
//		void askSaveIfDirty() {
//			if ((this.docStyleName == null) || (this.docStyle == null))
//				return;
//			if (!this.docStyleDirty)
//				return;
//			int choice = JOptionPane.showConfirmDialog(this, ("Document style template '" + docStyleName + "' has been modified. Save Changes?"), "Save Document Style?", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
//			if (choice == JOptionPane.YES_OPTION)
//				this.saveDocStyle();
//		}
//		
//		void saveDocStyle() {
//			if ((this.docStyleName == null) || (this.docStyle == null))
//				return;
//			if (!this.docStyleDirty)
//				return;
//			styleProvider.storeStyle(this.docStyleName, this.docStyle);
//			this.setDocStyleDirty(false);
//		}
//		
//		/* (non-Javadoc)
//		 * @see de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.DisplayExtension#isActive()
//		 */
//		public boolean isActive() {
//			return true;
//		}
//		
//		/* (non-Javadoc)
//		 * @see de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.DisplayExtension#getExtensionGraphics(de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
//		 */
//		public DisplayExtensionGraphics[] getExtensionGraphics(ImPage page, ImDocumentMarkupPanel idmp) {
//			if (idmp.document == this.testDoc) { /* we're working with this one */ }
//			else if ((this.testDoc != null) && this.testDoc.docId.equals(idmp.document.docId)) { /* we're still working with this one */ }
//			else if ((this.docStyleName != null) && this.docStyleName.equals(idmp.document.getAttribute(DocumentStyle.DOCUMENT_STYLE_NAME_ATTRIBUTE))) { /* this one fits the template we're working on */ }
//			else return NO_DISPLAY_EXTENSION_GRAPHICS; // this one is none of our business
////			System.out.println("Getting display '" + this.paramGroupName + "' extension graphics for page " + page.pageId);
////			System.out.println(" - active parameter description is " + this.activeParamDescription);
////			System.out.println(" - active parameter panel is " + this.activeParamPanel);
////			System.out.println(" - parameter group description is " + this.paramGroupDesciption);
//			
//			//	highlight current field in any custom way available
//			if (this.activeParamDescription instanceof DisplayExtension)
//				return ((DisplayExtension) this.activeParamDescription).getExtensionGraphics(page, idmp);
//			
//			//	highlight content of current field, or what it represents or matches
//			else if (this.activeParamPanel != null)
//				return this.activeParamPanel.getDisplayExtensionGraphics(page);
//			
//			//	highlight match of group as a whole (anchors, document metadata, etc.) if parameter description exists and represents a display extension
//			else if (this.paramGroupDescription instanceof DisplayExtension)
//				return ((DisplayExtension) this.paramGroupDescription).getExtensionGraphics(page, idmp);
//			
//			//	nothing to show right now
//			else return NO_DISPLAY_EXTENSION_GRAPHICS;
//		}
//		
//		private UseParamPanel activeParamPanel = null;
//		private ParameterDescription activeParamDescription = null;
//		
//		void setActiveParamPanel(UseParamPanel activeParamPanel) {
//			this.activeParamPanel = activeParamPanel;
//			if (this.activeParamPanel == null)
//				this.activeParamDescription = null;
//			else this.activeParamDescription = ((this.paramGroupDescription == null) ? null : this.paramGroupDescription.getParameterDescription(this.activeParamPanel.docStyleParamName));
//			ggImagine.notifyDisplayExtensionsModified(null);
//		}
//		
//		void paramUsageChanged(UseParamPanel paramPanel) {
//			this.updateParamStates(paramPanel);
//		}
//		
//		void paramValueChanged(UseParamPanel paramPanel) {
//			this.updateParamStates(paramPanel);
//		}
//		
//		private void updateParamStates(UseParamPanel paramPanel) {
//			if (this.paramGroupDescription == null)
//				return;
//			HashSet requiredParamNames = new HashSet();
//			HashSet excludedParamNames = new HashSet();
//			for (int p = 0; p < this.paramPanels.size(); p++) {
//				UseParamPanel upp = ((UseParamPanel) this.paramPanels.get(p));
//				String lpn = upp.docStyleParamName.substring(upp.docStyleParamName.lastIndexOf(".") + ".".length());
//				ParameterDescription pd = this.paramGroupDescription.getParameterDescription(lpn);
//				if (pd == null)
//					continue;
//				if (pd.isRequired())
//					requiredParamNames.add(lpn);
//				if (!upp.useParam.isSelected())
//					continue;
//				String[] rpns = pd.getRequiredParameters();
//				if (rpns != null)
//					requiredParamNames.addAll(Arrays.asList(rpns));
//				String[] epns = pd.getExcludedParameters();
//				if (epns != null)
//					excludedParamNames.addAll(Arrays.asList(epns));
//			}
//			HashSet valueRequiredParamNames = new HashSet();
//			HashSet valueExcludedParamNames = new HashSet();
//			for (int p = 0; p < this.paramPanels.size(); p++) {
//				UseParamPanel upp = ((UseParamPanel) this.paramPanels.get(p));
//				String lpn = upp.docStyleParamName.substring(upp.docStyleParamName.lastIndexOf(".") + ".".length());
//				if (excludedParamNames.contains(lpn))
//					continue;
//				ParameterDescription pd = this.paramGroupDescription.getParameterDescription(lpn);
//				if (pd == null)
//					continue;
//				String pv = upp.getValue();
//				String[] rpns = pd.getRequiredParameters(pv);
//				if (rpns != null)
//					valueRequiredParamNames.addAll(Arrays.asList(rpns));
//				String[] epns = pd.getExcludedParameters(pv);
//				if (epns != null)
//					valueExcludedParamNames.addAll(Arrays.asList(epns));
//			}
//			for (int p = 0; p < this.paramPanels.size(); p++) {
//				UseParamPanel upp = ((UseParamPanel) this.paramPanels.get(p));
//				if (upp == paramPanel)
//					continue;
//				String lpn = upp.docStyleParamName.substring(upp.docStyleParamName.lastIndexOf(".") + ".".length());
//				if (valueExcludedParamNames.contains(lpn))
//					upp.setExcluded(true);
//				else if (valueRequiredParamNames.contains(lpn))
//					upp.setRequired(true);
//				else if (excludedParamNames.contains(lpn))
//					upp.setExcluded(true);
//				else if (requiredParamNames.contains(lpn))
//					upp.setRequired(true);
//				else upp.setExcluded(false);
//			}
//			this.validate();
//			this.repaint();
//		}
//		
//		private class ParamTreeNode implements Comparable {
//			final String prefix;
//			final ParamTreeNode parent;
//			private ArrayList children = null;
//			private TreeSet paramNames = null;
//			ParamTreeNode(String prefix, ParamTreeNode parent) {
//				this.prefix = prefix;
//				this.parent = parent;
//				paramTreeNodesByPrefix.put(this.prefix, this);
//			}
//			int getChildCount() {
//				return ((this.children == null) ? 0 : this.children.size());
//			}
//			int getChildIndex(ParamTreeNode child) {
//				return ((this.children == null) ? -1 : this.children.indexOf(child));
//			}
//			void addChild(ParamTreeNode child) {
//				if (this.children == null)
//					this.children = new ArrayList(3);
//				this.children.add(child);
//			}
//			void removeChild(ParamTreeNode child) {
//				if (this.children != null)
//					this.children.remove(child);
//			}
//			void sortChildren() {
//				if (this.children == null)
//					return;
//				Collections.sort(this.children);
//				for (int c = 0; c < this.children.size(); c++)
//					((ParamTreeNode) this.children.get(c)).sortChildren();
//			}
//			void clearChildren() {
//				if (this.children != null)
//					this.children.clear();
//			}
//			void addParamName(String paramName) {
//				if (this.paramNames == null)
//					this.paramNames = new TreeSet();
//				this.paramNames.add(paramName);
//			}
//			
//			public String toString() {
////				return this.prefix.substring(this.prefix.lastIndexOf('.') + ".".length());
//				return getParameterGroupLabel(this.prefix, true);
//			}
//			
//			public int compareTo(Object obj) {
//				ParamTreeNode ptn = ((ParamTreeNode) obj);
//				if ((this.children == null) != (ptn.children == null))
//					return ((this.children == null) ? -1 : 1);
//				return this.prefix.compareTo(ptn.prefix);
//			}
//		}
//		
//		private TreeMap paramTreeNodesByPrefix = new TreeMap();
//		private ParamTreeNode paramTreeRoot = new ParamTreeNode("", null);
//		private ArrayList paramTreeModelListeners = new ArrayList(2);
//		private TreeModel paramTreeModel = new TreeModel() {
//			public Object getRoot() {
//				return paramTreeRoot;
//			}
//			public boolean isLeaf(Object node) {
//				return (((ParamTreeNode) node).getChildCount() == 0);
//			}
//			public int getChildCount(Object parent) {
//				return ((ParamTreeNode) parent).getChildCount();
//			}
//			public Object getChild(Object parent, int index) {
//				return ((ParamTreeNode) parent).children.get(index);
//			}
//			public int getIndexOfChild(Object parent, Object child) {
//				return ((ParamTreeNode) parent).getChildIndex((ParamTreeNode) child);
//			}
//			public void valueForPathChanged(TreePath path, Object newValue) { /* we're not changing the tree */ }
//			public void addTreeModelListener(TreeModelListener tml) {
//				paramTreeModelListeners.add(tml);
//			}
//			public void removeTreeModelListener(TreeModelListener tml) {
//				paramTreeModelListeners.remove(tml);
//			}
//		};
//		private void updateParamTree() {
//			this.paramTreeRoot.sortChildren();
//			ArrayList expandedPaths = new ArrayList();
//			for (int r = 0; r < this.paramTree.getRowCount(); r++) {
//				if (this.paramTree.isExpanded(r))
//					expandedPaths.add(this.paramTree.getPathForRow(r));
//			}
//			TreeModelEvent tme = new TreeModelEvent(this, new TreePath(this.paramTreeRoot));
//			for (int l = 0; l < paramTreeModelListeners.size(); l++)
//				((TreeModelListener) paramTreeModelListeners.get(l)).treeStructureChanged(tme);
//			for (int r = 0; r < expandedPaths.size(); r++)
//				this.paramTree.expandPath((TreePath) expandedPaths.get(r));
//			this.paramTree.validate();
//			this.paramTree.repaint();
//		}
//		
//		void paramTreeNodeSelected(final ParamTreeNode ptn) {
//			
//			//	remember selected param group
//			this.paramGroupName = ptn.prefix;
//			this.paramGroupDescription = getParameterGroupDescription(this.paramGroupName);
//			this.paramPanels.clear();
//			
//			//	clear param panel
//			this.paramPanel.removeAll();
//			
//			//	update param panel
//			if (ptn.paramNames != null) {
//				
//				//	add group label and description if group description present
//				if (this.paramGroupDescription != null) {
//					JPanel titlePanel = new JPanel(new BorderLayout(), true);
//					if (this.paramGroupDescription.getLabel() != null) {
//						JLabel title = new JLabel("<HTML><B>" + this.paramGroupDescription.getLabel() + "</B></HTML>");
//						title.setOpaque(true);
//						title.setBackground(Color.WHITE);
//						titlePanel.add(title, BorderLayout.NORTH);
//					}
//					if (this.paramGroupDescription.getDescription() != null) {
//						JLabel description = new JLabel("<HTML>" + this.paramGroupDescription.getDescription() + "</HTML>");
//						description.setOpaque(true);
//						description.setBackground(Color.WHITE);
//						titlePanel.add(description, BorderLayout.CENTER);
//					}
//					titlePanel.setBackground(Color.WHITE);
//					titlePanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 3), BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.RED, 1), BorderFactory.createLineBorder(Color.WHITE, 5))));
//					this.paramPanel.add(titlePanel, BorderLayout.NORTH);
//				}
//				
//				//	add parameter fields
//				JPanel paramPanel = new JPanel(true) {
//					public Dimension getPreferredSize() {
//						Dimension ps = super.getPreferredSize();
//						ps.width = (DocStyleEditor.this.paramPanel.getWidth() - 50);
//						return ps;
//					}
//				};
//				paramPanel.setLayout(new BoxLayout(paramPanel, BoxLayout.Y_AXIS));
//				
//				//	for anchors, make damn sure to show all the parameters (anchor names are custom and thus cannot be learned)
//				if (ptn.prefix.startsWith("anchor.")) {
//					ptn.addParamName(this.paramGroupName + ".fontSize");
//					ptn.addParamName(this.paramGroupName + ".minFontSize");
//					ptn.addParamName(this.paramGroupName + ".maxFontSize");
//					ptn.addParamName(this.paramGroupName + ".isBold");
//					ptn.addParamName(this.paramGroupName + ".isItalics");
//					ptn.addParamName(this.paramGroupName + ".isAllCaps");
//					ptn.addParamName(this.paramGroupName + ".pattern");
//					ptn.addParamName(this.paramGroupName + ".area");
//				}
//				for (Iterator pnit = ptn.paramNames.iterator(); pnit.hasNext();) {
//					String pn = ((String) pnit.next());
//					UseParamPanel upp = this.getParamValueField(pn);
//					paramPanel.add(upp);
//					this.paramPanels.add(upp);
//				}
//				JPanel paramPanelTray = new JPanel(new BorderLayout(), true);
//				paramPanelTray.add(paramPanel, BorderLayout.NORTH);
//				
//				//	make the whole thing scroll
//				JScrollPane paramPanelBox = new JScrollPane(paramPanelTray);
//				paramPanelBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//				paramPanelBox.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
//				paramPanelBox.getVerticalScrollBar().setBlockIncrement(50);
//				paramPanelBox.getVerticalScrollBar().setUnitIncrement(50);
//				this.paramPanel.add(paramPanelBox, BorderLayout.CENTER);
//				
//				//	add anchor test facilities
//				if (this.paramGroupName.startsWith("anchor.")) {
//					JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 0, 5), true);
//					JButton testButton = new JButton("Test Anchor");
//					testButton.setBorder(BorderFactory.createRaisedBevelBorder());
//					testButton.addActionListener(new ActionListener() {
//						public void actionPerformed(ActionEvent ae) {
//							testAnchor(ptn);
//						}
//					});
//					buttonPanel.add(testButton);
//					JButton removeButton = new JButton("Remove Anchor");
//					removeButton.setBorder(BorderFactory.createRaisedBevelBorder());
//					removeButton.addActionListener(new ActionListener() {
//						public void actionPerformed(ActionEvent ae) {
//							removeAnchor(ptn);
//						}
//					});
//					buttonPanel.add(removeButton);
//					buttonPanel.setBorder(BorderFactory.createLineBorder(buttonPanel.getBackground(), 5));
//					this.paramPanel.add(buttonPanel, BorderLayout.SOUTH);
//				}
//				
//				//	add group test button if group description is testable or can visualize its content
//				else if ((this.paramGroupDescription instanceof DisplayExtension) || (this.paramGroupDescription instanceof DisplayExtension)) {
//					JButton testButton = new JButton("Test");
//					testButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(testButton.getBackground(), 5), BorderFactory.createRaisedBevelBorder()));
//					testButton.addActionListener(new ActionListener() {
//						public void actionPerformed(ActionEvent ae) {
//							if (paramGroupDescription instanceof TestableElement) {
//								Properties dspg = docStyle.getSubset(paramGroupName).toProperties();
//								((TestableElement) paramGroupDescription).test(dspg); // test the parameter group description
//							}
//							if (paramGroupDescription instanceof DisplayExtension)
//								setActiveParamPanel(null); // this triggers update of display extensions
//						}
//					});
//					this.paramPanel.add(testButton, BorderLayout.SOUTH);
//				}
//			}
//			
//			//	make changes show
//			this.paramPanel.validate();
//			this.paramPanel.repaint();
//			this.updateParamStates(null);
//			
//			//	update display extensions
//			this.setActiveParamPanel(null);
//		}
//		
//		void testAnchor(ParamTreeNode ptn) {
//			
//			//	get anchor settings
//			Settings anchorParamList = this.docStyle.getSubset(ptn.prefix);
//			
//			//	get bounding box
//			BoundingBox area = BoundingBox.parse(anchorParamList.getSetting("area"));
//			if (area == null)
//				return;
//			
//			//	get pattern
//			String pattern = anchorParamList.getSetting("pattern");
//			if (pattern == null)
//				return;
//			
//			//	get font sizes and perform test
//			try {
//				ArrayList matchLog = new ArrayList();
//				int anchorMaxPageId = Integer.parseInt(this.docStyle.getSetting("anchor.maxPageId", this.docStyle.getSetting("layout.coverPageCount", "0")));
//				boolean anchorMatch = false;
//				for (int p = 0; p <= anchorMaxPageId; p++) {
//					matchLog.add("Testing page " + p + ":");
//					anchorMatch = DocumentStyleProvider.anchorMatches(this.testDoc,
//							(this.testDoc.getFirstPageId() + p),
//							area,
//							Integer.parseInt(anchorParamList.getSetting("minFontSize", anchorParamList.getSetting("fontSize", "0"))),
//							Integer.parseInt(anchorParamList.getSetting("maxFontSize", anchorParamList.getSetting("fontSize", "72"))),
//							"true".equals(anchorParamList.getSetting("isBold")),
//							"true".equals(anchorParamList.getSetting("isItalics")),
//							"true".equals(anchorParamList.getSetting("isAllCaps")),
//							pattern,
//							matchLog);
//					if (anchorMatch)
//						break;
//				}
//				String anchorName = ptn.prefix.substring(ptn.prefix.lastIndexOf('.') + ".".length());
//				StringBuffer anchorMatchLog = new StringBuffer();
//				for (int l = 0; l < matchLog.size(); l++) {
//					anchorMatchLog.append("\r\n");
//					anchorMatchLog.append((String) matchLog.get(l));
//				}
//				JOptionPane.showMessageDialog(this, ("This document " + (anchorMatch ? " matches " : " does not match ") + " anchor '" + anchorName + "':" + anchorMatchLog.toString()), "Anchor Match Test", (anchorMatch ? JOptionPane.PLAIN_MESSAGE : JOptionPane.ERROR_MESSAGE));
//			} catch (NumberFormatException nfe) {}
//		}
//		
//		void removeAnchor(ParamTreeNode ptn) {
//			
//			//	remove settings
//			for (Iterator pnit = ptn.paramNames.iterator(); pnit.hasNext();)
//				this.docStyle.removeSetting((String) pnit.next());
//			
//			//	remove node
//			ParamTreeNode pptn = ptn.parent;
//			pptn.removeChild(ptn);
//			
//			//	update param tree
//			this.updateParamTree();
//			
//			//	select path of current tree node
//			ArrayList pptnPath = new ArrayList();
//			for (;pptn != null; pptn = pptn.parent)
//				pptnPath.add(0, pptn);
//			if (pptnPath.size() != 0)
//				this.paramTree.setSelectionPath(new TreePath(pptnPath.toArray()));
//		}
//		
//		String paramGroupName;
//		ParameterGroupDescription paramGroupDescription;
//		ArrayList paramPanels = new ArrayList();
//		
//		void setParamGroupName(String pgn) {
//			
//			//	update fields for any parameters in group
//			for (Iterator pnit = this.paramValueFields.keySet().iterator(); pnit.hasNext();) {
//				String pn = ((String) pnit.next());
//				if (pn.lastIndexOf('.') != pgn.length())
//					continue;
//				if (!pn.startsWith(pgn))
//					continue;
//				UseParamPanel pvf = ((UseParamPanel) this.paramValueFields.get(pn));
//				String pv = this.docStyle.getSetting(pn);
//				pvf.setValue((pv == null) ? "" : pv);
//				pvf.useParam.setSelected(pv != null);
//			}
//			
//			//	no further updates required
//			if (pgn.equals(this.paramGroupName))
//				return;
//			
//			//	set param group name and get corresponding tree node
//			this.paramGroupName = pgn;
//			this.paramGroupDescription = getParameterGroupDescription(this.paramGroupName);
//			ParamTreeNode ptn = ((ParamTreeNode) this.paramTreeNodesByPrefix.get(this.paramGroupName));
//			
//			//	if we're creating an anchor, and only then, the tree node is null
//			if (ptn == null) {
//				ParamTreeNode pptn = ((ParamTreeNode) this.paramTreeNodesByPrefix.get("anchor"));
//				ptn = new ParamTreeNode(pgn, pptn);
//				ptn.addParamName(this.paramGroupName + ".fontSize");
//				ptn.addParamName(this.paramGroupName + ".minFontSize");
//				ptn.addParamName(this.paramGroupName + ".maxFontSize");
//				ptn.addParamName(this.paramGroupName + ".isBold");
//				ptn.addParamName(this.paramGroupName + ".isItalics");
//				ptn.addParamName(this.paramGroupName + ".isAllCaps");
//				ptn.addParamName(this.paramGroupName + ".pattern");
//				ptn.addParamName(this.paramGroupName + ".area");
//				pptn.addChild(ptn);
//				this.updateParamTree();
//			}
//			
//			//	select path of current tree node
//			ArrayList ptnPath = new ArrayList();
//			for (;ptn != null; ptn = ptn.parent)
//				ptnPath.add(0, ptn);
//			if (ptnPath.size() != 0)
//				this.paramTree.setSelectionPath(new TreePath(ptnPath.toArray()));
//		}
//		
//		private TreeMap paramValueFields = new TreeMap();
//		
//		private UseParamPanel getParamValueField(String pn) {
//			UseParamPanel pvf = ((UseParamPanel) this.paramValueFields.get(pn));
//			if (pvf == null) {
//				pvf = this.createParamValueField(this, pn);
//				this.paramValueFields.put(pn, pvf);
//			}
//			return pvf;
//		}
//		
//		private class ParamToggleListener implements ItemListener {
//			private UseParamPanel upp;
//			ParamToggleListener(UseParamPanel upp) {
//				this.upp = upp;
//			}
//			public void itemStateChanged(ItemEvent ie) {
//				if (this.upp.useParam.isSelected()) {
//					String dspv = this.upp.getValue();
//					if (this.upp.verifyValue(dspv))
//						docStyle.setSetting(this.upp.docStyleParamName, dspv);
//				}
//				else docStyle.removeSetting(this.upp.docStyleParamName);
//			}
//		}
//		
//		private UseParamPanel createParamValueField(DocStyleEditor parent, final String pn) {
//			final Class pvc = getParamValueClass(pn);
//			String pv = ((this.docStyle == null) ? null : this.docStyle.getSetting(pn));
//			boolean ps = (pv != null);
//			
//			String pl = null;
//			String pd = null;
//			String[] pvs = null;
//			String[] pvls = null;
//			if (this.paramGroupDescription != null) {
//				String lpn = pn.substring(pn.lastIndexOf('.') + ".".length());
//				pl = this.paramGroupDescription.getParamLabel(lpn);
//				pd = this.paramGroupDescription.getParamDescription(lpn);
//				if (pv == null)
//					pv = this.paramGroupDescription.getParamDefaultValue(lpn);
//				pvs = this.paramGroupDescription.getParamValues(lpn);
//				if (pvs != null) {
//					pvls = new String[pvs.length];
//					for (int v = 0; v < pvs.length; v++)
//						pvls[v] = this.paramGroupDescription.getParamValueLabel(lpn, pvs[v]);
//				}
//			}
//			if (pl == null)
//				pl = paramNamesLabels.getProperty(pn, pn);
//			
//			//	boolean, use plain checkbox
//			if (Boolean.class.getName().equals(pvc.getName())) {
//				UseBooleanPanel pvf = new UseBooleanPanel(parent, pn, pl, pd, "true".equals(pv)) {
//					void notifyModified() {
//						setDocStyleDirty(true);
//						super.notifyModified();
//					}
//				};
//				pvf.useParam.addItemListener(new ParamToggleListener(pvf));
//				return pvf;
//			}
//			
//			//	number, use string field
//			else if (Integer.class.getName().equals(pvc.getName())) {
//				if ((pvs != null) && (pvs.length != 0)) {
//					for (int v = 0; v < pvs.length; v++) try {
//						Integer.parseInt(pvs[v]);
//					}
//					catch (RuntimeException re) {
//						pvs = null;
//						break;
//					}
//				}
//				UseParamPanel pvf;
//				if (pvs == null)
//					pvf = new UseStringPanel(parent, pn, pl, pd, ps, ((pv == null) ? "" : pv), false, false) {
//						boolean verifyValue(String value) {
//							try {
//								Integer.parseInt(this.getValue());
//								return true;
//							}
//							catch (NumberFormatException nfe) {
//								return false;
//							}
//						}
//						void stringChanged(String string) {
//							if (string.length() == 0)
//								this.useParam.setSelected(false);
//							else if (this.useParam.isSelected() && this.verifyValue(string))
//								docStyle.setSetting(this.docStyleParamName, string);
//							setDocStyleDirty(true);
//						}
//						DisplayExtensionGraphics[] getDisplayExtensionGraphics(ImPage page) {
//							if (this.docStyleParamName.endsWith(".fontSize") || this.docStyleParamName.endsWith("FontSize"))
//								return getFontSizeVisualizationGraphics(this.parent, page, this.getValue());
//							if (this.docStyleParamName.endsWith(".margin") || this.docStyleParamName.endsWith("Margin"))
//								return getMarginVisualizationGraphics(this.parent, page, this.getValue());
//							//	TODO think of more
//							return NO_DISPLAY_EXTENSION_GRAPHICS;
//						}
//					};
//				else pvf = new UseStringOptionPanel(parent, pn, pvs, pvls, pl, pd, ps, ((pv == null) ? "" : pv), false) {
//					void stringChanged(String string) {
//						if (string.length() == 0)
//							this.useParam.setSelected(false);
//						else if (this.useParam.isSelected() && this.verifyValue(string))
//							docStyle.setSetting(this.docStyleParamName, string);
//						setDocStyleDirty(true);
//					}
//				};
//				pvf.useParam.addItemListener(new ParamToggleListener(pvf));
//				return pvf;
//			}
//			else if (Float.class.getName().equals(pvc.getName())) {
//				if ((pvs != null) && (pvs.length != 0)) {
//					for (int v = 0; v < pvs.length; v++) try {
//						Float.parseFloat(pvs[v]);
//					}
//					catch (RuntimeException re) {
//						pvs = null;
//						break;
//					}
//				}
//				UseParamPanel pvf;
//				if (pvs == null)
//					pvf = new UseStringPanel(parent, pn, pl, pd, ps, ((pv == null) ? "" : pv), false, false) {
//						boolean verifyValue(String value) {
//							try {
//								Float.parseFloat(this.getValue());
//								return true;
//							}
//							catch (NumberFormatException nfe) {
//								return false;
//							}
//						}
//						void stringChanged(String string) {
//							if (string.length() == 0)
//								this.useParam.setSelected(false);
//							else if (this.useParam.isSelected() && this.verifyValue(string))
//								docStyle.setSetting(this.docStyleParamName, string);
//							setDocStyleDirty(true);
//						}
//						DisplayExtensionGraphics[] getDisplayExtensionGraphics(ImPage page) {
//							return NO_DISPLAY_EXTENSION_GRAPHICS;
//						}
//					};
//				else pvf = new UseStringOptionPanel(parent, pn, pvs, pvls, pl, pd, ps, ((pv == null) ? "" : pv), false) {
//					void stringChanged(String string) {
//						if (string.length() == 0)
//							this.useParam.setSelected(false);
//						else if (this.useParam.isSelected() && this.verifyValue(string))
//							docStyle.setSetting(this.docStyleParamName, string);
//						setDocStyleDirty(true);
//					}
//				};
//				pvf.useParam.addItemListener(new ParamToggleListener(pvf));
//				return pvf;
//			}
//			else if (Double.class.getName().equals(pvc.getName())) {
//				if ((pvs != null) && (pvs.length != 0)) {
//					for (int v = 0; v < pvs.length; v++) try {
//						Double.parseDouble(pvs[v]);
//					}
//					catch (RuntimeException re) {
//						pvs = null;
//						break;
//					}
//				}
//				UseParamPanel pvf;
//				if (pvs == null)
//					pvf = new UseStringPanel(parent, pn, pl, pd, ps, ((pv == null) ? "" : pv), false, false) {
//						boolean verifyValue(String value) {
//							try {
//								Double.parseDouble(this.getValue());
//								return true;
//							}
//							catch (NumberFormatException nfe) {
//								return false;
//							}
//						}
//						void stringChanged(String string) {
//							if (string.length() == 0)
//								this.useParam.setSelected(false);
//							else if (this.useParam.isSelected() && this.verifyValue(string))
//								docStyle.setSetting(this.docStyleParamName, string);
//							setDocStyleDirty(true);
//						}
//						DisplayExtensionGraphics[] getDisplayExtensionGraphics(ImPage page) {
//							return NO_DISPLAY_EXTENSION_GRAPHICS;
//						}
//					};
//				else pvf = new UseStringOptionPanel(parent, pn, pvs, pvls, pl, pd, ps, ((pv == null) ? "" : pv), false) {
//					void stringChanged(String string) {
//						if (string.length() == 0)
//							this.useParam.setSelected(false);
//						else if (this.useParam.isSelected() && this.verifyValue(string))
//							docStyle.setSetting(this.docStyleParamName, string);
//						setDocStyleDirty(true);
//					}
//				};
//				pvf.useParam.addItemListener(new ParamToggleListener(pvf));
//				return pvf;
//			}
//			
//			//	bounding box, use string field
//			else if (BoundingBox.class.getName().equals(pvc.getName())) {
//				UseStringPanel pvf = new UseStringPanel(parent, pn, pl, pd, ps, ((pv == null) ? "" : pv), false, false) {
//					boolean verifyValue(String value) {
//						try {
//							return (BoundingBox.parse(this.getValue()) != null);
//						}
//						catch (RuntimeException re) {
//							return false;
//						}
//					}
//					void stringChanged(String string) {
//						if (string.length() == 0)
//							this.useParam.setSelected(false);
//						else if (this.useParam.isSelected() && this.verifyValue(string))
//							docStyle.setSetting(this.docStyleParamName, string);
//						setDocStyleDirty(true);
//					}
//					DisplayExtensionGraphics[] getDisplayExtensionGraphics(ImPage page) {
//						return getBoundingBoxVisualizationGraphics(this.parent, page, this.getValue());
//					}
//				};
//				pvf.useParam.addItemListener(new ParamToggleListener(pvf));
//				return pvf;
//			}
//			
//			//	string, use string field
//			else if (String.class.getName().equals(pvc.getName())) {
//				UseParamPanel pvf;
//				if (pvs == null)
//					pvf = new UseStringPanel(parent, pn, pl, pd, ps, ((pv == null) ? "" : pv), false, (pn.endsWith(".pattern") || pn.endsWith("Pattern"))) {
//						ImTokenSequence getTestDocTokens() {
//							if (testDocTokens == null)
//								testDocTokens = new ImTokenSequence(((Tokenizer) testDoc.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER)), testDoc.getTextStreamHeads(), (ImTokenSequence.NORMALIZATION_LEVEL_PARAGRAPHS | ImTokenSequence.NORMALIZE_CHARACTERS));
//							return testDocTokens;
//						}
//						ImDocument getTestDoc() {
//							return testDoc;
//						}
//						boolean verifyValue(String value) {
//							if (pn.endsWith(".linePattern") || pn.endsWith("LinePattern")) {
//								try {
//									LinePattern.parsePattern(value);
//									return true;
//								}
//								catch (IllegalArgumentException iae) {
//									return false;
//								}
//							}
//							else if (pn.endsWith(".pattern") || pn.endsWith("Pattern")) {
//								try {
//									Pattern.compile(value);
//									return true;
//								}
//								catch (PatternSyntaxException pse) {
//									return false;
//								}
//							}
//							else return true;
//						}
//						void stringChanged(String string) {
//							if (string.length() == 0)
//								this.useParam.setSelected(false);
//							else if (this.useParam.isSelected() && this.verifyValue(string))
//								docStyle.setSetting(this.docStyleParamName, string);
//							setDocStyleDirty(true);
//						}
//						DisplayExtensionGraphics[] getDisplayExtensionGraphics(ImPage page) {
//							if (this.docStyleParamName.endsWith(".linePattern") || this.docStyleParamName.endsWith("LinePattern"))
//								return getLinePatternVisualizationGraphics(this.parent, page, this.getValue());
//							if (pn.endsWith(".pattern") || pn.endsWith("Pattern"))
//								return getPatternVisualizationGraphics(this.parent, page, this.getValue());
//							return NO_DISPLAY_EXTENSION_GRAPHICS;
//						}
//					};
//				else pvf = new UseStringOptionPanel(parent, pn, pvs, pvls, pl, pd, ps, ((pv == null) ? "" : pv), false) {
//					void stringChanged(String string) {
//						if (string.length() == 0)
//							this.useParam.setSelected(false);
//						else if (this.useParam.isSelected() && this.verifyValue(string))
//							docStyle.setSetting(this.docStyleParamName, string);
//						setDocStyleDirty(true);
//					}
//				};
//				pvf.useParam.addItemListener(new ParamToggleListener(pvf));
//				return pvf;
//			}
//			
//			//	list, use list field
//			else if (DocumentStyle.getListElementClass(pvc) != pvc) {
//				final Class pvlec = DocumentStyle.getListElementClass(pvc);
//				UseListPanel pvf = new UseListPanel(parent, pn, pl, pd, ps, ((pv == null) ? "" : pv), (pn.endsWith(".patterns") || pn.endsWith("Patterns"))) {
//					ImTokenSequence getTestDocTokens() {
//						if (testDocTokens == null)
//							testDocTokens = new ImTokenSequence(((Tokenizer) testDoc.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER)), testDoc.getTextStreamHeads(), (ImTokenSequence.NORMALIZATION_LEVEL_PARAGRAPHS | ImTokenSequence.NORMALIZE_CHARACTERS));
//						return testDocTokens;
//					}
//					ImDocument getTestDoc() {
//						return testDoc;
//					}
//					boolean verifyValue(String value) {
//						String[] valueParts = value.split("\\s+");
//						for (int p = 0; p < valueParts.length; p++) {
//							if (!this.verifyValuePart(valueParts[p]))
//								return false;
//						}
//						return true;
//					}
//					boolean verifyValuePart(String valuePart) {
//						if (Boolean.class.getName().equals(pvlec.getName()))
//							return ("true".equals(valuePart) || "false".equals(valuePart));
//						else if (Integer.class.getName().equals(pvlec.getName())) {
//							try {
//								Integer.parseInt(valuePart);
//								return true;
//							}
//							catch (NumberFormatException nfe) {
//								return false;
//							}
//						}
//						else if (Float.class.getName().equals(pvlec.getName())) {
//							try {
//								Float.parseFloat(valuePart);
//								return true;
//							}
//							catch (NumberFormatException nfe) {
//								return false;
//							}
//						}
//						else if (Double.class.getName().equals(pvlec.getName())) {
//							try {
//								Double.parseDouble(valuePart);
//								return true;
//							}
//							catch (NumberFormatException nfe) {
//								return false;
//							}
//						}
//						else if (BoundingBox.class.getName().equals(pvlec.getName())) {
//							try {
//								return (BoundingBox.parse(this.getValue()) != null);
//							}
//							catch (RuntimeException re) {
//								return false;
//							}
//						}
//						else if (String.class.getName().equals(pvlec.getName())) {
//							if (pn.endsWith(".linePatterns") || pn.endsWith("LinePatterns")) {
//								try {
//									LinePattern.parsePattern(valuePart);
//									return true;
//								}
//								catch (IllegalArgumentException iae) {
//									return false;
//								}
//							}
//							else if (pn.endsWith(".patterns") || pn.endsWith("Patterns")) {
//								try {
//									Pattern.compile(valuePart);
//									return true;
//								}
//								catch (PatternSyntaxException pse) {
//									return false;
//								}
//							}
//							else return true;
//						}
//						else return true;
//					}
//					void stringChanged(String string) {
//						if (string.length() == 0)
//							this.useParam.setSelected(false);
//						else if (this.useParam.isSelected() && this.verifyValue(string))
//							docStyle.setSetting(this.docStyleParamName, string.replaceAll("\\s+", " "));
//						setDocStyleDirty(true);
//					}
//					DisplayExtensionGraphics[] getDisplayExtensionGraphics(ImPage page) {
//						String[] valueParts = this.getValue().split("\\s+");
//						ArrayList degList = new ArrayList();
//						for (int p = 0; p < valueParts.length; p++)
//							degList.addAll(Arrays.asList(this.getDisplayExtensionGraphicsPart(page, valueParts[p])));
//						return ((DisplayExtensionGraphics[]) degList.toArray(new DisplayExtensionGraphics[degList.size()]));
//					}
//					DisplayExtensionGraphics[] getDisplayExtensionGraphicsPart(ImPage page, String valuePart) {
//						if (Integer.class.getName().equals(pvlec.getName())) {
//							if (this.docStyleParamName.endsWith(".fontSize") || this.docStyleParamName.endsWith("FontSize"))
//								return getFontSizeVisualizationGraphics(this.parent, page, valuePart);
//							if (this.docStyleParamName.endsWith(".margin") || this.docStyleParamName.endsWith("Margin"))
//								return getMarginVisualizationGraphics(this.parent, page, valuePart);
//							//	TODO think of more
//						}
//						else if (BoundingBox.class.getName().equals(pvlec.getName()))
//							return getBoundingBoxVisualizationGraphics(this.parent, page, valuePart);
//						else if (String.class.getName().equals(pvlec.getName())) {
//							if (this.docStyleParamName.endsWith(".linePatterns") || this.docStyleParamName.endsWith("LinePatterns"))
//								return getLinePatternVisualizationGraphics(this.parent, page, valuePart);
//							if (pn.endsWith(".patterns") || pn.endsWith("Patterns"))
//								return getPatternVisualizationGraphics(this.parent, page, valuePart);
//						}
//						return NO_DISPLAY_EXTENSION_GRAPHICS;
//					}
//				};
//				pvf.useParam.addItemListener(new ParamToggleListener(pvf));
//				return pvf;
//			}
//			
//			//	as the ultimate fallback, use string field
//			else {
//				UseStringPanel pvf = new UseStringPanel(parent, pn, pl, pd, ps, ((pv == null) ? "" : pv), false, false) {
//					void stringChanged(String string) {
//						if (string.length() == 0)
//							this.useParam.setSelected(false);
//						else if (this.useParam.isSelected() && this.verifyValue(string))
//							docStyle.setSetting(this.docStyleParamName, string);
//						setDocStyleDirty(true);
//					}
//					DisplayExtensionGraphics[] getDisplayExtensionGraphics(ImPage page) {
//						return NO_DISPLAY_EXTENSION_GRAPHICS;
//					}
//				};
//				pvf.useParam.addItemListener(new ParamToggleListener(pvf));
//				return pvf;
//			}
//		}
//	}
//	
//	private static class ParamTreeCellRenderer extends DefaultTreeCellRenderer {
//		private Icon rootIcon = null;
//		ParamTreeCellRenderer() {
//			String packageName = DocumentStyleManager.class.getName();
//			packageName = packageName.replace('.', '/');
//			try {
//				this.setClosedIcon(new ImageIcon(ImageIO.read(DocumentStyleManager.class.getClassLoader().getResourceAsStream(packageName + ".paramTree.closed.png"))));
//				this.setOpenIcon(new ImageIcon(ImageIO.read(DocumentStyleManager.class.getClassLoader().getResourceAsStream(packageName + ".paramTree.open.png"))));
//				this.setLeafIcon(new ImageIcon(ImageIO.read(DocumentStyleManager.class.getClassLoader().getResourceAsStream(packageName + ".paramTree.leaf.png"))));
//				this.rootIcon = new ImageIcon(ImageIO.read(DocumentStyleManager.class.getClassLoader().getResourceAsStream(packageName + ".paramTree.root.png")));
//			} catch (IOException ioe) { /* never gonna happen, but Java don't know */ }
//		}
//		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
//			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
//			if ((this.rootIcon != null) && (value != null) && (value instanceof TreeNode) && (((TreeNode) value).getParent() == null))
//				this.setIcon(this.rootIcon);
//			return this;
//		}
//	}
//	
//	private static DisplayExtensionGraphics[] getLinePatternVisualizationGraphics(DocStyleEditor parent, ImPage page, String pattern) {
//		try {
//			if (pattern.trim().length() == 0)
//				return NO_DISPLAY_EXTENSION_GRAPHICS;
//			LinePattern lp = LinePattern.parsePattern(pattern);
//			ImRegion[] lines = lp.getMatches(page);
//			if (lines.length == 0)
//				return NO_DISPLAY_EXTENSION_GRAPHICS;
//			DisplayExtensionGraphics[] degs = new DisplayExtensionGraphics[lines.length];
//			for (int l = 0; l < lines.length; l++)
//				degs[l] = getBoundingBoxVisualizationGraphics(parent, page, lines[l].bounds);
//			return degs;
//		}
//		catch (IllegalArgumentException iae) {
//			return NO_DISPLAY_EXTENSION_GRAPHICS;
//		}
//		catch (Exception e) {
//			e.printStackTrace(System.out);
//			return NO_DISPLAY_EXTENSION_GRAPHICS;
//		}
//	}
//	
//	private static DisplayExtensionGraphics[] getPatternVisualizationGraphics(DocStyleEditor parent, ImPage page, String pattern) {
//		try {
//			if (pattern.trim().length() == 0)
//				return NO_DISPLAY_EXTENSION_GRAPHICS;
//			ImDocumentRoot pageTokens = new ImDocumentRoot(page, (ImTokenSequence.NORMALIZATION_LEVEL_PARAGRAPHS | ImTokenSequence.NORMALIZE_CHARACTERS | ImDocumentRoot.INCLUDE_PAGE_TITLES));
//			Annotation[] patternMatches = Gamta.extractAllMatches(pageTokens, pattern, 32, false, false, false);
//			ArrayList pmWords = new ArrayList();
//			for (int m = 0; m < patternMatches.length; m++) {
//				ImWord pmStartWord = pageTokens.wordAtIndex(patternMatches[m].getStartIndex());
//				ImWord pmEndWord = pageTokens.wordAtIndex(patternMatches[m].getEndIndex() - 1);
//				for (ImWord pmWord = pmStartWord; pmWord != null; pmWord = pmWord.getNextWord()) {
//					if (pmWord.pageId != page.pageId)
//						break;
//					pmWords.add(pmWord);
//					if (pmWord == pmEndWord)
//						break;
//				}
//			}
//			return getWordVisualizationGraphics(parent, page, ((ImWord[]) pmWords.toArray(new ImWord[pmWords.size()])));
//		}
//		catch (IllegalArgumentException iae) {
//			return NO_DISPLAY_EXTENSION_GRAPHICS;
//		}
//		catch (Exception e) {
//			e.printStackTrace(System.out);
//			return NO_DISPLAY_EXTENSION_GRAPHICS;
//		}
//	}
//	
//	private static DisplayExtensionGraphics[] getFontSizeVisualizationGraphics(DocStyleEditor parent, ImPage page, String fontSize) {
//		if (fontSize == null)
//			return NO_DISPLAY_EXTENSION_GRAPHICS;
//		fontSize = fontSize.trim();
//		if (fontSize.length() == 0)
//			return NO_DISPLAY_EXTENSION_GRAPHICS;
//		try {
//			ArrayList fsWords = new ArrayList();
//			ImWord[] pWords = page.getWords();
//			for (int w = 0; w < pWords.length; w++) {
//				if (fontSize.equals(pWords[w].getAttribute(ImWord.FONT_SIZE_ATTRIBUTE)))
//					fsWords.add(pWords[w]);
//			}
//			return getWordVisualizationGraphics(parent, page, ((ImWord[]) fsWords.toArray(new ImWord[fsWords.size()])));
//		}
//		catch (Exception e) {
//			e.printStackTrace(System.out);
//			return NO_DISPLAY_EXTENSION_GRAPHICS;
//		}
//	}
//	
//	private static DisplayExtensionGraphics[] getFontStyleVisualizationGraphics(DocStyleEditor parent, ImPage page, String attributeName) {
//		if (attributeName == null)
//			return NO_DISPLAY_EXTENSION_GRAPHICS;
//		attributeName = attributeName.trim();
//		if (attributeName.length() == 0)
//			return NO_DISPLAY_EXTENSION_GRAPHICS;
//		try {
//			ArrayList fsWords = new ArrayList();
//			ImWord[] pWords = page.getWords();
//			for (int w = 0; w < pWords.length; w++) {
//				if (pWords[w].hasAttribute(attributeName))
//					fsWords.add(pWords[w]);
//				else if ("allCaps".equals(attributeName) && isAllCaps(pWords[w].getString()))
//					fsWords.add(pWords[w]);
//			}
//			return getWordVisualizationGraphics(parent, page, ((ImWord[]) fsWords.toArray(new ImWord[fsWords.size()])));
//		}
//		catch (Exception e) {
//			e.printStackTrace(System.out);
//			return NO_DISPLAY_EXTENSION_GRAPHICS;
//		}
//	}
//	private static boolean isAllCaps(String str) {
//		return (str.equals(str.toUpperCase()) && !str.equals(str.toLowerCase()));
//	}
//	
//	private static final Color wordLineColor = Color.ORANGE;
//	private static final BasicStroke wordLineStroke = new BasicStroke(1);
//	private static final Color wordFillColor = new Color(wordLineColor.getRed(), wordLineColor.getGreen(), wordLineColor.getBlue(), 64);
//	private static DisplayExtensionGraphics[] getWordVisualizationGraphics(DocStyleEditor parent, ImPage page, ImWord[] words) {
//		Shape[] shapes = new Shape[words.length];
//		for (int w = 0; w < words.length; w++)
//			shapes[w] = new Rectangle2D.Float(words[w].bounds.left, words[w].bounds.top, words[w].bounds.getWidth(), words[w].bounds.getHeight());
//		DisplayExtensionGraphics[] degs = {new DisplayExtensionGraphics(parent, null, page, shapes, wordLineColor, wordLineStroke, wordFillColor) {
//			public boolean isActive() {
//				return true;
//			}
//		}};
//		return degs;
//	}
//	
//	private static DisplayExtensionGraphics[] getMarginVisualizationGraphics(DocStyleEditor parent, ImPage page, String margin) {
//		try {
//			/* TODO figure out how to implement this:
//			 * - based upon lines?
//			 * - based upon paragraphs?
//			 * - based upon blocks?
//			 */
//			return NO_DISPLAY_EXTENSION_GRAPHICS;
//		}
//		catch (IllegalArgumentException iae) {
//			return NO_DISPLAY_EXTENSION_GRAPHICS;
//		}
//		catch (Exception e) {
//			e.printStackTrace(System.out);
//			return NO_DISPLAY_EXTENSION_GRAPHICS;
//		}
//	}
//	
//	private static final Color boundingBoxLineColor = Color.GREEN;
//	private static final BasicStroke boundingBoxLineStroke = new BasicStroke(3);
//	private static final Color boundingBoxFillColor = new Color(boundingBoxLineColor.getRed(), boundingBoxLineColor.getGreen(), boundingBoxLineColor.getBlue(), 64);
//	private static DisplayExtensionGraphics[] getBoundingBoxVisualizationGraphics(DocStyleEditor parent, ImPage page, String box) {
//		try {
//			BoundingBox bb = BoundingBox.parse(box);
//			if (bb == null)
//				return NO_DISPLAY_EXTENSION_GRAPHICS;
//			bb = bb.scale(((float) page.getImageDPI()) / DocumentStyle.DEFAULT_DPI);
//			DisplayExtensionGraphics[] degs = {getBoundingBoxVisualizationGraphics(parent, page, bb)};
//			return degs;
//		}
//		catch (IllegalArgumentException iae) {
//			return NO_DISPLAY_EXTENSION_GRAPHICS;
//		}
//		catch (Exception e) {
//			e.printStackTrace(System.out);
//			return NO_DISPLAY_EXTENSION_GRAPHICS;
//		}
//	}
//	private static DisplayExtensionGraphics getBoundingBoxVisualizationGraphics(DocStyleEditor parent, ImPage page, BoundingBox bb) {
//		Shape[] shapes = {new Rectangle2D.Float(bb.left, bb.top, bb.getWidth(), bb.getHeight())};
//		return new DisplayExtensionGraphics(parent, null, page, shapes, boundingBoxLineColor, boundingBoxLineStroke, boundingBoxFillColor) {
//			public boolean isActive() {
//				return true;
//			}
//		};
//	}
//	
//	private static final Properties paramNamesLabels = new Properties() {
//		public String getProperty(String key, String defaultValue) {
//			return super.getProperty(key.substring(key.lastIndexOf('.') + 1), defaultValue);
//		}
//	};
//	static {
//		paramNamesLabels.setProperty("isBold", "Require Values to be Bold");
//		paramNamesLabels.setProperty("startIsBold", "Require Value Starts to be Bold");
//		paramNamesLabels.setProperty("isItalics", "Require Values to be in Italics");
//		paramNamesLabels.setProperty("startIsItalics", "Require Value Starts to be in Italics");
//		paramNamesLabels.setProperty("isAllCaps", "Require Values to be All Caps");
//		paramNamesLabels.setProperty("startIsAllCaps", "Require Value Starts to be All Caps");
//		paramNamesLabels.setProperty("minFontSize", "Use Minimum Font Size");
//		paramNamesLabels.setProperty("maxFontSize", "Use Maximum Font Size");
//	}
//	
//	/* TODO 
//- when loading document style parameter lists, copy them into genuine Properties object via putAll() instead of handing out view on Settings
//- introduce "parent" parameter
//  - load parent parameter list ...
//  - ... and use it as default in handed out Properties
//  ==> facilitates parameter value inheritance, vastly reducing maintenance effort
//  	 */
//	public static void main(String[] args) throws Exception {
//		System.out.println(buildPattern("ZOOTAXA  109:"));
//	}
//}
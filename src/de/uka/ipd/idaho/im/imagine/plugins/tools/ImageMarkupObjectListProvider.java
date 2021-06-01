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
package de.uka.ipd.idaho.im.imagine.plugins.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.AttributeUtils;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.gamta.defaultImplementation.PlainTokenSequence;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.BufferedLineWriter;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.GPathParser;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.GPathException;
import de.uka.ipd.idaho.gamta.util.swing.AnnotationDisplayDialog;
import de.uka.ipd.idaho.gamta.util.swing.AttributeEditor;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationFilter;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.goldenGate.util.HelpEditorDialog;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder.HtmlPageBuilderHost;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.gamta.ImDocumentRoot;
import de.uka.ipd.idaho.im.imagine.GoldenGateImagine;
import de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.basic.AttributeToolProvider;
import de.uka.ipd.idaho.im.imagine.web.GoldenGateImagineWebUtils;
import de.uka.ipd.idaho.im.imagine.web.GoldenGateImagineWebUtils.AttributeEditorPageBuilder;
import de.uka.ipd.idaho.im.imagine.web.plugins.WebDocumentViewer;
import de.uka.ipd.idaho.im.imagine.web.plugins.WebDocumentViewer.WebDocumentView;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Annotation viewer listing annotations in a table. The displayed annotations
 * can be filtered through custom filters that may be changed in an ad-hoc
 * fashion. The listing allows for merging annotations and editing their
 * attributes, among others. This view is both helpful for sorting out
 * annotations, and for finding ones with specific error conditions, e.g. a
 * lacking attribute.<br>
 * To simplify matters for users, administrative mode allows to pre-configure
 * listings, which are then readily accessible through the menu.
 * If none of these parameters is specified, the user can select or enter
 * filters manually in an extra panel at the top of the dialog.
 * 
 * @author sautter
 */
public class ImageMarkupObjectListProvider extends AbstractResourceManager implements ImageMarkupToolProvider, SelectionActionProvider {
	
	private static final String FILE_EXTENSION = ".imObjectList";
	
	private static final String ANNOT_LISTER_IMT_NAME = "ListAnnots";
	private static final String REGION_LISTER_IMT_NAME = "ListRegions";
	
	private ImageMarkupTool annotLister = new AnnotLister();
	private ImageMarkupTool regionLister = new RegionLister();
	
	private AttributeToolProvider attributeToolProvider;
	
	/** zero-argument constructor for class loading */
	public ImageMarkupObjectListProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "IM Object List Provider";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		
		//	load custom filter history
		try {
			InputStream is = this.dataProvider.getInputStream("customFilterHistory.cnfg");
			this.customFilterHistory.addContent(StringVector.loadList(is));
			is.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImaginePlugin#setImagineParent(de.uka.ipd.idaho.im.imagine.GoldenGateImagine)
	 */
	public void setImagineParent(GoldenGateImagine ggImagine) { /* we don't seem to need this one here */ }
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractGoldenGateImaginePlugin#initImagine()
	 */
	public void initImagine() {
		
		//	connect to attribute tool provider
		this.attributeToolProvider = ((AttributeToolProvider) this.parent.getPlugin(AttributeToolProvider.class.getName()));
		if (this.attributeToolProvider == null)
			throw new RuntimeException("Cannot work without attribute tools");
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#exit()
	 */
	public void exit() {
		
		//	store custom filter history if possible
		if (this.dataProvider.isDataEditable("customFilterHistory.cnfg")) try {
			OutputStream os = this.dataProvider.getOutputStream("customFilterHistory.cnfg");
			this.customFilterHistory.storeContent(os);
			os.flush();
			os.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#isOperational()
	 */
	public boolean isOperational() {
		return !GraphicsEnvironment.isHeadless();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "IM Object List";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getFileExtension()
	 */
	protected String getFileExtension() {
		return FILE_EXTENSION;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getDataNamesForResource(java.lang.String)
	 */
	public String[] getDataNamesForResource(String name) {
		if (this.dataProvider.isDataAvailable(name + ".help.html")) {
			String[] dns = {(name + "@" + this.getClass().getName()), (name + ".help.html" + "@" + this.getClass().getName())};
			return dns;
		}
		else return super.getDataNamesForResource(name);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider#getActions(de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(ImWord start, ImWord end, ImDocumentMarkupPanel idmp) {

		//	we're single word selection only
		if (start != end)
			return null;
		
		//	collect painted annotations spanning or overlapping whole selection
		ImAnnotation[] spanningAnnots = idmp.document.getAnnotationsSpanning(start, end);
		final TreeSet spanningAnnotTypes = new TreeSet();
		for (int a = 0; a < spanningAnnots.length; a++) {
			if (idmp.areAnnotationsPainted(spanningAnnots[a].getType()))
				spanningAnnotTypes.add(spanningAnnots[a].getType());
		}
		
		//	nothing to work with
		if (spanningAnnotTypes.size() == 0)
			return null;
		
		//	collect available actions
		LinkedList actions = new LinkedList();
		
		//	a single annotation to work with
		if (spanningAnnotTypes.size() == 1) {
			final String annotType = ((String) spanningAnnotTypes.first());
			actions.add(new SelectionAction("listAnnots", ("List " + annotType + " Annotations"), ("Open a list of all " + annotType + " annotations")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					invoker.applyMarkupTool(new AnnotTypeLister(annotType), null);
					return true;
				}
			});
		}
		
		//	multiple annotation types to work with
		else actions.add(new SelectionAction("listAnnots", "List Annotations ...", ("Open a list of annotations from the document")) {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				return false;
			}
			public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
				JMenu pm = new JMenu("List Annotations");
				for (Iterator atit = spanningAnnotTypes.iterator(); atit.hasNext();) {
					final String annotType = ((String) atit.next());
					JMenuItem mi = new JMenuItem("- " + annotType);
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							invoker.applyMarkupTool(new AnnotTypeLister(annotType), null);
						}
					});
					pm.add(mi);
				}
				return pm;
			}
		});
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, ImPage page, ImDocumentMarkupPanel idmp) {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider#getEditMenuItemNames()
	 */
	public String[] getEditMenuItemNames() {
		String[] emins = {ANNOT_LISTER_IMT_NAME, REGION_LISTER_IMT_NAME};
		return emins;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getToolsMenuItemNames()
	 */
	public String[] getToolsMenuItemNames() {
		return this.getResourceNames();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		if (ANNOT_LISTER_IMT_NAME.equals(name))
			return this.annotLister;
		else if (REGION_LISTER_IMT_NAME.equals(name))
			return this.regionLister;
		else return this.getCustomLister(name);
	}
	
	private class AnnotLister implements ImageMarkupTool, WebDocumentViewer {
		public String getLabel() {
			return "List Annotations";
		}
		public String getTooltip() {
			return "List annotations from document";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	we're only processing the document as a whole
			if (annot != null)
				return;
			
			//	list annotations on paragraph level normalized document
			listImObjects("Annotation List", doc, ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS, idmp, null);
		}
		public WebDocumentView getWebDocumentView(String baseUrl) {
			return new WebObjectList(this, baseUrl, "Annotation List", ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS, null);
		}
	}
	
	private class AnnotTypeLister implements ImageMarkupTool, WebDocumentViewer {
		private String annotType;
		AnnotTypeLister(String annotType) {
			this.annotType = annotType;
		}
		public String getLabel() {
			return ("List " + this.annotType + " Annotations");
		}
		public String getTooltip() {
			return ("List " + this.annotType + " annotations present in document");
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	we're only processing the document as a whole
			if (annot != null)
				return;
			
			//	list annotations of matching type on paragraph level normalized document
			listImObjects("Annotation List", doc, ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS, idmp, getGPathFilter(this.annotType));
		}
		public WebDocumentView getWebDocumentView(String baseUrl) {
			return new WebObjectList(this, baseUrl, "Annotation List", ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS, getGPathFilter(this.annotType));
		}
	}
	
	private class RegionLister implements ImageMarkupTool, WebDocumentViewer {
		public String getLabel() {
			return "List Regions";
		}
		public String getTooltip() {
			return "List regions from document";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	we're only processing the document as a whole
			if (annot != null)
				return;
			
			//	list regions on un-normalized normalized document
			listImObjects("Region List", doc, ImDocumentRoot.NORMALIZATION_LEVEL_RAW, idmp, null);
		}
		public WebDocumentView getWebDocumentView(String baseUrl) {
//			return createImObjectListView(this, baseUrl, "Region List", ImDocumentRoot.NORMALIZATION_LEVEL_RAW, null);
			return new WebObjectList(this, baseUrl, "Region List", ImDocumentRoot.NORMALIZATION_LEVEL_RAW, null);
		}
	}
	
	private CustomImObjectLister getCustomLister(String name) {
		Settings cl = this.loadSettingsResource(name);
		if ((cl == null) || cl.isEmpty())
			return null;
		int normalizationLevel;
		try {
			normalizationLevel = Integer.parseInt(cl.getSetting(NORMALIZATION_LEVEL_ATTRIBUTE));
		} catch (RuntimeException re) { return null; }
		String label = cl.getSetting(LABEL_ATTRIBUTE);
		String tooltip = cl.getSetting(TOOLTIP_ATTRIBUTE);
		String filter = cl.getSetting(FILTER_ATTRIBUTE);
		return new CustomImObjectLister(name, label, tooltip, normalizationLevel, filter);
	}
	
	private class CustomImObjectLister implements ImageMarkupTool, WebDocumentViewer {
		private String name;
		private String label;
		private String tooltip;
		private int normalizationLevel;
		private String filterString;
		private AnnotationFilter filter;
		CustomImObjectLister(String name, String label, String tooltip, int normalizationLevel, String filterString) {
			this.name = name;
			this.label = label;
			this.tooltip = tooltip;
			this.normalizationLevel = normalizationLevel;
			this.filterString = filterString;
		}
		public String getLabel() {
			return this.label;
		}
		public String getTooltip() {
			return this.tooltip;
		}
		public String getHelpText() {
			return loadStringResource(this.name + ".help.html");
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	we're only processing the document as a whole
			if (annot != null)
				return;
			
			//	get filter
			if (this.filter == null)
				this.filter = getGPathFilter(this.filterString);
			if (this.filter == null)
				return;
			
			//	offer editing annotations on paragraph level normalized document
			listImObjects(this.label, doc, this.normalizationLevel, idmp, this.filter);
		}
		public WebDocumentView getWebDocumentView(String baseUrl) {
//			return createImObjectListView(this, baseUrl, this.label, this.normalizationLevel, this.filter);
			return new WebObjectList(this, baseUrl, this.label, this.normalizationLevel, this.filter);
		}
	}
	
	private static AnnotationFilter getGPathFilter(String filterString) {
		
		//	validate & compile path expression
		String error = GPathParser.validatePath(filterString);
		GPath filterPath = null;
		if (error == null) try {
			filterPath = new GPath(filterString);
		}
		catch (Exception e) {
			error = e.getMessage();
		}
		
		//	validation successful
		if (error == null)
			return new GPathAnnotationFilter(filterString, filterPath);
		
		//	validation error
		else {
			DialogFactory.alert(("The filter expression is not valid:\n" + error), "GPath Validation", JOptionPane.ERROR_MESSAGE);
			return null;
		}
	}
	
	private static class GPathAnnotationFilter implements AnnotationFilter {
		private String filterString;
		private GPath filterPath;
		GPathAnnotationFilter(String filterString, GPath filterPath) {
			this.filterString = filterString;
			this.filterPath = filterPath;
		}
		public boolean accept(Annotation annotation) {
			return false;
		}
		public QueriableAnnotation[] getMatches(QueriableAnnotation data) {
			try {
				return this.filterPath.evaluate(data, GPath.getDummyVariableResolver());
			}
			catch (GPathException gpe) {
				DialogFactory.alert(gpe.getMessage(), "GPath Error", JOptionPane.ERROR_MESSAGE);
				return new QueriableAnnotation[0];
			}
		}
		public MutableAnnotation[] getMutableMatches(MutableAnnotation data) {
			QueriableAnnotation[] matches = this.getMatches(data);
			Set matchIDs = new HashSet();
			for (int m = 0; m < matches.length; m++)
				matchIDs.add(matches[m].getAnnotationID());
			MutableAnnotation[] mutableAnnotations = data.getMutableAnnotations();
			ArrayList mutableMatches = new ArrayList();
			for (int m = 0; m < mutableAnnotations.length; m++)
				if (matchIDs.contains(mutableAnnotations[m].getAnnotationID()))
					mutableMatches.add(mutableAnnotations[m]);
			return ((MutableAnnotation[]) mutableMatches.toArray(new MutableAnnotation[mutableMatches.size()]));
		}
		public String getName() {
			return this.filterString;
		}
		public String getProviderClassName() {
			return "Homegrown";
		}
		public String getTypeLabel() {
			return "Custom Filter";
		}
		public boolean equals(Object obj) {
			return ((obj != null) && this.filterString.equals(obj.toString()));
		}
		public String toString() {
			return this.filterString;
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Image Markup Object Lists";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuItems()
	 */
	public JMenuItem[] getMainMenuItems() {
		if (!this.dataProvider.isDataEditable())
			return new JMenuItem[0];
		
		ArrayList collector = new ArrayList();
		JMenuItem mi;
		
		mi = new JMenuItem("Create");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				createCustomLister();
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Edit");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				editCustomListers();
			}
		});
		collector.add(mi);
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	private boolean createCustomLister() {
		return (this.createCustomLister(null, null) != null);
	}
	
	private boolean cloneCustomLister() {
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			return this.createCustomLister();
		else {
			String name = "New " + selectedName;
			return (this.createCustomLister(this.getCustomLister(selectedName), name) != null);
		}
	}
	
	private String createCustomLister(CustomImObjectLister modelCol, String name) {
		CreateCustomListerDialog ccld = new CreateCustomListerDialog(modelCol, name);
		ccld.setVisible(true);
		
		if (ccld.isCommitted()) {
			Settings col = ccld.getCustomLister();
			String colName = ccld.getImageMarkupToolName();
			if (!colName.endsWith(FILE_EXTENSION)) colName += FILE_EXTENSION;
			try {
				if (this.storeSettingsResource(colName, col)) {
					this.resourceNameList.refresh();
					return colName;
				}
			} catch (IOException ioe) {}
		}
		return null;
	}
	
	private void editCustomListers() {
		final CustomListerEditorPanel[] editor = new CustomListerEditorPanel[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit Image Markup Object Lists", true);
		editDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		editDialog.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent we) {
				this.closeDialog();
			}
			public void windowClosing(WindowEvent we) {
				this.closeDialog();
			}
			private void closeDialog() {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeSettingsResource(editor[0].name, editor[0].getSettings());
					} catch (IOException ioe) {}
				}
				if (editDialog.isVisible()) editDialog.dispose();
			}
		});
		
		editDialog.setLayout(new BorderLayout());
		
		JPanel editButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton button;
		button = new JButton("Create");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				createCustomLister();
			}
		});
		editButtons.add(button);
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				cloneCustomLister();
			}
		});
		editButtons.add(button);
		button = new JButton("Delete");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (deleteResource(resourceNameList.getSelectedName()))
					resourceNameList.refresh();
			}
		});
		editButtons.add(button);
		
		editDialog.add(editButtons, BorderLayout.NORTH);
		
		final JPanel editorPanel = new JPanel(new BorderLayout());
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
		else {
			CustomImObjectLister cl = this.getCustomLister(selectedName);
			if (cl == null)
				editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
			else {
				editor[0] = new CustomListerEditorPanel(selectedName, cl);
				editorPanel.add(editor[0], BorderLayout.CENTER);
			}
		}
		editDialog.add(editorPanel, BorderLayout.CENTER);
		
		editDialog.add(this.resourceNameList, BorderLayout.EAST);
		DataListListener dll = new DataListListener() {
			public void selected(String dataName) {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeSettingsResource(editor[0].name, editor[0].getSettings());
					}
					catch (IOException ioe) {
						if (DialogFactory.confirm((ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving file to " + editor[0].name + "\nProceed?"), "Could Not Save Object List", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
							resourceNameList.setSelectedName(editor[0].name);
							editorPanel.validate();
							return;
						}
					}
				}
				editorPanel.removeAll();
				
				if (dataName == null)
					editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
				else {
					CustomImObjectLister cl = getCustomLister(dataName);
					if (cl == null)
						editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
					else {
						editor[0] = new CustomListerEditorPanel(dataName, cl);
						editorPanel.add(editor[0], BorderLayout.CENTER);
					}
				}
				editorPanel.validate();
			}
		};
		this.resourceNameList.addDataListListener(dll);
		
		editDialog.setSize(DEFAULT_EDIT_DIALOG_SIZE);
		editDialog.setLocationRelativeTo(editDialog.getOwner());
		editDialog.setVisible(true);
		
		this.resourceNameList.removeDataListListener(dll);
	}
	
	private static final String LABEL_ATTRIBUTE = "LABEL";
	private static final String TOOLTIP_ATTRIBUTE = "TOOLTIP";
	
	private static final String NORMALIZATION_LEVEL_ATTRIBUTE = "NORMALIZATION_LEVEL";
	private static final String FILTER_ATTRIBUTE = "FILTER";
	
	private static final String RAW_NORMALIZATION_LEVEL = "Raw (words strictly in layout order)";
	private static final String WORD_NORMALIZATION_LEVEL = "Words (words in layout order, but de-hyphenated)";
	private static final String PARAGRAPH_NORMALIZATION_LEVEL = "Paragraphs (logical paragraphs kept together)";
	private static final String STREAM_NORMALIZATION_LEVEL = "Text Streams (logical text streams one after another)";
	private static final String[] NORMALIZATION_LEVELS = {
		RAW_NORMALIZATION_LEVEL,
		WORD_NORMALIZATION_LEVEL,
		PARAGRAPH_NORMALIZATION_LEVEL,
		STREAM_NORMALIZATION_LEVEL,
	};
	
	private class CreateCustomListerDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private CustomListerEditorPanel editor;
		private String clName = null;
		
		CreateCustomListerDialog(CustomImObjectLister col, String name) {
			super("Create Image Markup Object Lists", true);
			
			this.nameField = new JTextField((name == null) ? "New Image Markup Object Lists" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					clName = nameField.getText();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					clName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new CustomListerEditorPanel(name, col);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(this.nameField, BorderLayout.NORTH);
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 600));
			this.setLocationRelativeTo(DialogPanel.getTopWindow());
		}
		
		boolean isCommitted() {
			return (this.clName != null);
		}
		
		Settings getCustomLister() {
			return this.editor.getSettings();
		}
		
		String getImageMarkupToolName() {
			return this.clName;
		}
	}
	
	private class CustomListerEditorPanel extends JPanel implements DocumentListener, ItemListener {
		
		private String name;
		
		private boolean dirty = false;
		private boolean helpTextDirty = false;
		
		private JTextField label = new JTextField();
		private JTextField toolTip = new JTextField();
		private String helpText;
		private JButton editHelpText = new JButton("Edit Help Text");
		
		private JComboBox normalizationLevel = new JComboBox(NORMALIZATION_LEVELS);
		private JTextField filter = new JTextField();
		
		CustomListerEditorPanel(String name, CustomImObjectLister col) {
			super(new BorderLayout(), true);
			this.name = name;
			this.add(getExplanationLabel(), BorderLayout.CENTER);
			
			this.normalizationLevel.setEditable(false);
			this.normalizationLevel.setSelectedItem(PARAGRAPH_NORMALIZATION_LEVEL);
			
			if (col != null) {
				this.label.setText(col.label);
				this.toolTip.setText(col.tooltip);
				this.helpText = col.getHelpText();
				if (col.normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_RAW)
					this.normalizationLevel.setSelectedItem(RAW_NORMALIZATION_LEVEL);
				else if (col.normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_WORDS)
					this.normalizationLevel.setSelectedItem(WORD_NORMALIZATION_LEVEL);
				else if (col.normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_STREAMS)
					this.normalizationLevel.setSelectedItem(STREAM_NORMALIZATION_LEVEL);
				else this.normalizationLevel.setSelectedItem(PARAGRAPH_NORMALIZATION_LEVEL);
				this.filter.setText(col.filterString);
			}
			
			this.label.getDocument().addDocumentListener(this);
			this.toolTip.getDocument().addDocumentListener(this);
			this.editHelpText.setBorder(BorderFactory.createRaisedBevelBorder());
			this.editHelpText.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					editHelpText();
				}
			});
			
			this.normalizationLevel.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			this.filter.getDocument().addDocumentListener(this);
			
			JButton testFilter = new JButton("Test");
			testFilter.setBorder(BorderFactory.createRaisedBevelBorder());
			testFilter.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					testFilter(filter.getText());
				}
			});
			JPanel filterPanel = new JPanel(new BorderLayout(), true);
			filterPanel.add(this.filter, BorderLayout.CENTER);
			filterPanel.add(testFilter, BorderLayout.EAST);
			
			JPanel functionPanel = new JPanel(new GridBagLayout(), true);
			functionPanel.setBorder(BorderFactory.createEtchedBorder());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 2;
			gbc.insets.bottom = 2;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.weighty = 1;
			gbc.gridheight = 1;
			gbc.fill = GridBagConstraints.BOTH;
			
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			gbc.weightx = 0;
			functionPanel.add(new JLabel("Label in 'Tools' Menu", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			functionPanel.add(this.label, gbc.clone());
			gbc.gridx = 2;
			gbc.weightx = 0;
			functionPanel.add(this.editHelpText, gbc.clone());
			
			gbc.gridy ++;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			gbc.weightx = 0;
			functionPanel.add(new JLabel("Object List Explanation", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.gridwidth = 2;
			gbc.weightx = 2;
			functionPanel.add(this.toolTip, gbc.clone());
			
			gbc.gridy ++;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			gbc.weightx = 0;
			functionPanel.add(new JLabel("Use Document Normalization Level ...", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 2;
			functionPanel.add(this.normalizationLevel, gbc.clone());
			
			gbc.gridy ++;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			gbc.weightx = 0;
			functionPanel.add(new JLabel("Use Filter ...", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 2;
			functionPanel.add(filterPanel, gbc.clone());
			
			this.add(functionPanel, BorderLayout.SOUTH);
		}
		
		void testFilter(String filter) {
			QueriableAnnotation testDoc = Gamta.getTestDocument();
			if (testDoc == null)
				return;
			
			AnnotationFilter af = getGPathFilter(filter);
			if (af == null)
				return;
			
			Annotation[] data = af.getMatches(testDoc);
			
			AnnotationDisplayDialog add;
			Window top = DialogPanel.getTopWindow();
			if (top instanceof JDialog)
				add = new AnnotationDisplayDialog(((JDialog) top), "Matches of Filter", data, true);
			else if (top instanceof JFrame)
				add = new AnnotationDisplayDialog(((JFrame) top), "Matches of Filter", data, true);
			else add = new AnnotationDisplayDialog(((JFrame) null), "Matches of Filter", data, true);
			add.setLocationRelativeTo(top);
			add.setVisible(true);
		}
		
		public void changedUpdate(DocumentEvent de) {}
		
		public void insertUpdate(DocumentEvent de) {
			this.dirty = true;
		}
		
		public void removeUpdate(DocumentEvent de) {
			this.dirty = true;
		}
		
		public void itemStateChanged(ItemEvent ie) {
			this.dirty = true;
		}
		
		boolean isDirty() {
			return (this.dirty || this.helpTextDirty);
		}
		
		private void editHelpText() {
			HelpEditorDialog hed = new HelpEditorDialog(("Edit Help Text for '" + this.name + "'"), this.helpText);
			hed.setVisible(true);
			if (hed.isCommitted()) {
				this.helpText = hed.getHelpText();
				this.helpTextDirty = true;
			}
		}
		
		Settings getSettings() {
			Settings set = new Settings();
			
			String label = this.label.getText();
			if (label.trim().length() != 0)
				set.setSetting(LABEL_ATTRIBUTE, label);
			
			String toolTip = this.toolTip.getText();
			if (toolTip.trim().length() != 0)
				set.setSetting(TOOLTIP_ATTRIBUTE, toolTip);
			
			if (RAW_NORMALIZATION_LEVEL.equals(this.normalizationLevel.getSelectedItem()))
				set.setSetting(NORMALIZATION_LEVEL_ATTRIBUTE, ("" + ImDocumentRoot.NORMALIZATION_LEVEL_RAW));
			else if (WORD_NORMALIZATION_LEVEL.equals(this.normalizationLevel.getSelectedItem()))
				set.setSetting(NORMALIZATION_LEVEL_ATTRIBUTE, ("" + ImDocumentRoot.NORMALIZATION_LEVEL_WORDS));
			else if (STREAM_NORMALIZATION_LEVEL.equals(this.normalizationLevel.getSelectedItem()))
				set.setSetting(NORMALIZATION_LEVEL_ATTRIBUTE, ("" + ImDocumentRoot.NORMALIZATION_LEVEL_STREAMS));
			else set.setSetting(NORMALIZATION_LEVEL_ATTRIBUTE, ("" + ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS));
			
			String filter = this.filter.getText();
			if (filter.trim().length() != 0)
				set.setSetting(FILTER_ATTRIBUTE, filter);
			
			if (this.helpTextDirty && (this.helpText != null)) try {
				storeStringResource((this.name + ".help.html"), this.helpText);
			} catch (IOException ioe) {}
			
			return set;
		}
	}
	
	private class WebObjectList extends WebDocumentView {
		private ImDocumentMarkupPanel idmp;
		private boolean showingLayoutObjects;
		private String layoutObjectName;
		private String objectListTitle;
		
		private int normalizationLevel;
		private ImDocumentRoot wrappedDoc;
		
		private String objectFilterString = null;
		private AnnotationFilter objectFilter;
		private boolean isFixedObjectFilter;
		
		private ObjectTray[] objectTrays;
		private HashMap objectTraysByID = new HashMap();
		
		private HtmlPageBuilderHost host;
		
		private String editingAttributesId = null;
		private boolean editingAttributesDirty = false;
		
		WebObjectList(ImageMarkupTool parentImt, String baseUrl, String objectListTitle, int normalizationLevel, AnnotationFilter objectFilter) {
			super(parentImt, baseUrl);
			this.normalizationLevel = normalizationLevel;
			this.objectListTitle = objectListTitle;
			this.objectFilter = objectFilter;
			this.isFixedObjectFilter = (objectFilter != null);
		}
		
		protected void preProcess(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp) {
			this.idmp = idmp;
			
			//	are we showing layout objects?
			this.showingLayoutObjects = ((this.normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_RAW) || (this.normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_WORDS));
			this.layoutObjectName = (this.showingLayoutObjects ? "Region" : "Annotation");
			
			//	wrap document
			this.wrappedDoc = new ImDocumentRoot(doc, (this.normalizationLevel | ImDocumentRoot.SHOW_TOKENS_AS_WORD_ANNOTATIONS | ImDocumentRoot.USE_RANDOM_ANNOTATION_IDS));
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
			this.host = host;
			
			//	wait for pre-processing to finish (we can do that without locking, as pre-processing is quick)
			while (this.wrappedDoc == null) try {
				Thread.sleep(10);
			} catch (InterruptedException ie) {}
			
			//	return page builder
			return new HtmlPageBuilder(host, request, response) {
				protected boolean includeJavaScriptDomHelpers() {
					return true;
				}
				protected void include(String type, String tag) throws IOException {
					if ("includeBody".equals(type)) {
						
						//	use this for style development !!!
						if (this.host.findFile("WebDocumentView.dev.html") != null)
							this.includeFile("WebDocumentView.dev.html");
						
						//	open dialog content
						this.writeLine("<div class=\"objectListDialog\">");
						
						//	add title
						if (objectListTitle != null)
							this.writeLine("<div id=\"objectListTitle\" class=\"objectListTitle\">" + html.escape(objectListTitle) + "</div>");
						
						//	add filter panel only if filter is not fixed
						if (!isFixedObjectFilter) {
							this.writeLine("<div class=\"objectListFilter\">");
							this.writeLine("<input type=\"text\" id=\"objectListFilterField\" value=\"" + ((objectFilterString == null) ? "" : html.escape(objectFilterString)) + "\" placeholder=\"&lt;Enter Filter (XPath)&gt;\" list=\"objectListFilterHistory\" onkeyup=\"return catchReturnKeyInFilter(event);\" />");
							this.writeLine("<datalist id=\"objectListFilterHistory\">");
							String[] objectTypes = wrappedDoc.getAnnotationTypes();
							for (int t = 0; t < objectTypes.length; t++)
								this.writeLine("<option value=\"" + html.escape(objectTypes[t]) + "\" />");
							this.writeLine("</datalist>");
							this.writeLine("<button id=\"objectListFilterButton\" onclick=\"return setObjectListFilter(null);\">Apply</button>");
							this.writeLine("</div>");
						}
						
						//	add 'Show Matches Only' vs. 'Highlight Matches' toggle
						this.writeLine("<div id=\"objectListModeBox\" class=\"objectListModes\">");
						this.write("<span class=\"objectListMode\">");
						this.write("<input type=\"radio\" name=\"objectListMode\" value=\"highlight\" id=\"objectListModeHighlight\" onchange=\"return setObjectListMode('h');\"/>");
						this.write("Highlight Matches");
						this.writeLine("</span>");
						this.write("<span class=\"objectListMode\">");
						this.write("<input type=\"radio\" name=\"objectListMode\" value=\"restrict\" id=\"objectListModeRestrict\" onchange=\"return setObjectListMode('r');\"/>");
						this.write("Show Matches Only");
						this.writeLine("</span>");
						this.writeLine("</div>");
						
						//	add function buttons, calling dynamic scripts with selection and parameters
						this.writeLine("<div class=\"objectListFunctions\">");
						this.writeLine("<button id=\"mergeObjectsButton\" class=\"objectListFunctionsButton\" onclick=\"return mergeSelectedObjects();\" disabled=\"true\">Merge</button>");
						this.writeLine("<button id=\"renameObjectsButton\" class=\"objectListFunctionsButton\" onclick=\"return renameSelectedObjects();\" disabled=\"true\">Rename</button>");
						this.writeLine("<button id=\"removeObjectsButton\" class=\"objectListFunctionsButton\" onclick=\"return removeSelectedObjects();\" disabled=\"true\">Remove</button>");
						this.writeLine("<button id=\"deleteObjectsButton\" class=\"objectListFunctionsButton\" onclick=\"return deleteSelectedObjects();\" disabled=\"true\">Delete</button>");
						this.writeLine("<button id=\"editAttributesButton\" class=\"objectListFunctionsButton\" onclick=\"return editObjectAttributes(null);\" disabled=\"true\">Edit Attributes</button>");
						this.writeLine("<button id=\"renameAttributeButton\" class=\"objectListFunctionsButton\" onclick=\"return renameObjectAttribute();\" disabled=\"true\">Rename Attribute</button>");
						this.writeLine("<button id=\"modifyAttributeButton\" class=\"objectListFunctionsButton\" onclick=\"return modifyObjectAttribute();\" disabled=\"true\">Modify Attributes</button>");
						this.writeLine("<button id=\"removeAttributeButton\" class=\"objectListFunctionsButton\" onclick=\"return removeObjectAttribute();\" disabled=\"true\">Remove Attribute</button>");
						this.writeLine("</div>");
						
						//	add object table, allowing to select objects via checkbox (and blue highlight)
						this.writeLine("<table id=\"objectListTable\">");
						
						//	add column headers, including onclick sorting
						this.writeLine("<thead id=\"objectListTableHead\">");
						this.writeLine("<td id=\"objectListColumnHeadSelect\" class=\"objectListColumnHead objectListSelect\">&nbsp;</td>");
						this.writeLine("<td id=\"objectListColumnHeadType\" class=\"objectListColumnHead objectListType\" onclick=\"return setObjectListSortField('type');\">Type</td>");
						this.writeLine("<td id=\"objectListColumnHeadStart\" class=\"objectListColumnHead objectListStart\" onclick=\"return setObjectListSortField('start');\">Start</td>");
						this.writeLine("<td id=\"objectListColumnHeadEnd\" class=\"objectListColumnHead objectListEnd\" onclick=\"return setObjectListSortField('end');\">End</td>");
						this.writeLine("<td id=\"objectListColumnHeadValue\" class=\"objectListColumnHead objectListValue\" onclick=\"return setObjectListSortField('value');\">Value</td>");
						this.writeLine("<td id=\"objectListColumnHeadSpacer\" class=\"objectListColumnHead\">&nbsp</td>");
						this.writeLine("</thead>");
						
						//	add table body (content follows via JavaScript)
						this.writeLine("<tbody id=\"objectListTableBody\">");
						this.writeLine("<tr id=\"objectListTableSpacer\">");
						this.writeLine("<td>&nbsp;</td>");
						this.writeLine("<td>&nbsp;</td>");
						this.writeLine("<td>&nbsp;</td>");
						this.writeLine("<td>&nbsp;</td>");
						this.writeLine("<td>&nbsp;</td>");
						this.writeLine("</tr>");
						this.writeLine("</tbody>");
						this.writeLine("</table>");
						
						//	add 'Select All' and 'Select None' buttons
						this.writeLine("<div class=\"objectListButtons\">");
						this.writeLine("<button class=\"objectListButton\" onclick=\"return globalSelectObjects(true);\">Select All</button>");
						this.writeLine("<button class=\"objectListButton\" onclick=\"return globalSelectObjects(false);\">Select None</button>");
						this.writeLine("</div>");
						
						//	add Close button (simply set URL in window.location.href)
						this.writeLine("<div class=\"objectListMainButtons\">");
						this.writeLine("<button class=\"objectListMainButton\" onclick=\"return closeObjectList();\">Close</button>");
						this.writeLine("</div>");
						
						//	close object list area
						this.writeLine("</div>");
						
						//	add script with initialization calls
						this.writeLine("<script type=\"text/javascript\">");
						
						//	if filter is fixed, include script setting filter to that value (to use default list loading facilities)
						if (isFixedObjectFilter)
							this.writeLine("setObjectListFilter('" + GoldenGateImagineWebUtils.escapeForJavaScript(objectFilter.toString()) + "');");
						
						//	if we have a filter string (may happen on page reload), apply it
						else if (objectFilterString != null)
							this.writeLine("setObjectListFilter(null);");
						
						//	if editing attributes in a refresh, re-open attribute editor
						if (editingAttributesId != null)
							this.writeLine("editObjectAttributes('" + GoldenGateImagineWebUtils.escapeForJavaScript(editingAttributesId) + "');");
						
						//	close initialization calls and add dynamic script node
						this.writeLine("</script>");
						this.writeLine("<script id=\"dynamicActionScript\" type=\"text/javascript\" src=\"toBeSetDynamically\"></script>");
					}
					else super.include(type, tag);
				}
				protected void writePageHeadExtensions() throws IOException {
					this.writeLine("<script type=\"text/javascript\">");
					
					//	perform some dynamic action via script tag replacement
					this.writeLine("function getDynamicActionScript(name, params) {");
					this.writeLine("  var das = getById('dynamicActionScript');");
					this.writeLine("  var dasp = das.parentNode;");
					this.writeLine("  removeElement(das);");
					this.writeLine("  var dasSrc = ('" + baseUrl + "' + name + '?time=' + (new Date()).getTime() + params);");
					this.writeLine("  das = newElement('script', 'dynamicActionScript');");
					this.writeLine("  das.type = 'text/javascript';");
					this.writeLine("  das.src = dasSrc;");
					this.writeLine("  dasp.appendChild(das);");
					this.writeLine("  return false;");
					this.writeLine("}");
					
					//	catch return key in filter field to move to attribute value input
					this.writeLine("function catchReturnKeyInFilter(event) {");
					this.writeLine("  if (event.keyCode == 13) {");
					this.writeLine("    event.stopPropagation();");
					this.writeLine("    return setObjectListFilter(null);");
					this.writeLine("  }");
					this.writeLine("  else return true;");
					this.writeLine("}");
					
					//	TODO put all these URL independent functions in 'webView.html' for easier maintenance
					
					//	move table spacer to end
					this.writeLine("function addObjectListTableSpacer() {");
					this.writeLine("  var olTb = getById('objectListTableBody');");
					this.writeLine("  var olTs = getById('objectListTableSpacer');");
					this.writeLine("  olTb.removeChild(olTs);");
					this.writeLine("  olTb.appendChild(olTs);");
					this.writeLine("}");
					
					//	call to filter.js
					this.writeLine("function setObjectListFilter(filter) {");
					this.writeLine("  if (filter == null)");
					this.writeLine("    filter = getById('objectListFilterField').value;");
					this.writeLine("  if ((filter == null) || (filter.trim() == '')) {");
					this.writeLine("    showAlertDialog('Please enter a non-empty filter to use.', 'Invalid Filter', " + JOptionPane.ERROR_MESSAGE + ");");
					this.writeLine("    return false;");
					this.writeLine("  }");
					this.writeLine("  return getDynamicActionScript('filter.js', ('&filter=' + encodeURIComponent(filter)));");
					this.writeLine("}");
					
					//	add filter string to history
					this.writeLine("function addToFilterHistory(filterString) {");
					this.writeLine("  var fh = getById('objectListFilterHistory');");
					this.writeLine("  var fhChildren = fh.children;");
					this.writeLine("  var fsOpt = null;");
					this.writeLine("  for (var c = 0; c < fhChildren.length; c++)");
					this.writeLine("    if (fhChildren[c].value == filterString) {");
					this.writeLine("      if (c == 0)");
					this.writeLine("        return; // no need to move anything around");
					this.writeLine("      fsOpt = fhChildren[c];");
					this.writeLine("      removeElement(fsOpt);");
					this.writeLine("    }");
					this.writeLine("  if (fsOpt == null) {");
					this.writeLine("    fsOpt = newElement('option', null, null, null);");
					this.writeLine("    fsOpt.value = filterString;");
					this.writeLine("  }");
					this.writeLine("  fh.insertBefore(fsOpt, fh.firstElementChild);");
					this.writeLine("}");
					
					//	call to 'merge.js'
					this.writeLine("function mergeSelectedObjects() {");
					this.writeLine("  getDynamicActionScript('merge.js', ('&selection=' + getSelectedObjectIDs()));");
					this.writeLine("}");
					
					//	call to 'rename.js'
					this.writeLine("function renameSelectedObjects() {");
					this.writeLine("  getDynamicActionScript('rename.js', ('&selection=' + getSelectedObjectIDs()));");
					this.writeLine("}");
					
					//	call to 'remove.js'
					this.writeLine("function removeSelectedObjects() {");
					this.writeLine("  getDynamicActionScript('remove.js', ('&selection=' + getSelectedObjectIDs()));");
					this.writeLine("}");
					
					//	call to 'delete.js'
					this.writeLine("function deleteSelectedObjects() {");
					this.writeLine("  getDynamicActionScript('delete.js', ('selection=' + getSelectedObjectIDs()));");
					this.writeLine("}");
					
					//	open 'editAttributes'
					this.writeLine("function editObjectAttributes(objectId) {");
					this.writeLine("  if (objectId == null)");
					this.writeLine("    objectId = getSelectedObjectIDs();");
					this.writeLine("  if ((objectId == '') || (objectId.indexOf(';') != -1))");
					this.writeLine("    return;");
					this.writeLine("  window.open(('" + baseUrl + "editAttributes?id=' + objectId), 'editAttributesWindow', 'width=50,height=50,top=0,left=0,resizable=yes,scrollbar=yes,scrollbars=yes');");
					this.writeLine("}");
					
					//	get previous or next object ID to 'Previous' and 'Next' up and down object list with attribute editor
					this.writeLine("function getPreviousObjectId(objId) {");
					this.writeLine("  var prevObjId = null;");
					this.writeLine("  for (var o = 0; o < objectList.length; o++) {");
					this.writeLine("    if (objectList[o].tr.style.display == 'none')");
					this.writeLine("      continue;");
					this.writeLine("    if (objectList[o].id == objId)");
					this.writeLine("      break;");
					this.writeLine("    else prevObjId = objectList[o].id;");
					this.writeLine("  }");
					this.writeLine("  return prevObjId;");
					this.writeLine("}");
					this.writeLine("function getNextObjectId(objId) {");
					this.writeLine("  var gotObjId = false;");
					this.writeLine("  for (var o = 0; o < objectList.length; o++) {");
					this.writeLine("    if (objectList[o].tr.style.display == 'none')");
					this.writeLine("      continue;");
					this.writeLine("    if (objectList[o].id == objId)");
					this.writeLine("      gotObjId = true;");
					this.writeLine("    else if (gotObjId)");
					this.writeLine("      return objectList[o].id;");
					this.writeLine("  }");
					this.writeLine("  return null;");
					this.writeLine("}");
					
					//	call to 'renameAttribute.js'
					this.writeLine("function renameObjectAttribute() {");
					this.writeLine("  getDynamicActionScript('renameAttribute.js', ('&selection=' + getSelectedObjectIDs()));");
					this.writeLine("}");
					
					//	call to 'modifyAttribute.js'
					this.writeLine("function modifyObjectAttribute() {");
					this.writeLine("  getDynamicActionScript('modifyAttribute.js', ('&selection=' + getSelectedObjectIDs()));");
					this.writeLine("}");
					
					//	call to 'removeAttribute.js'
					this.writeLine("function removeObjectAttribute() {");
					this.writeLine("  getDynamicActionScript('removeAttribute.js', ('&selection=' + getSelectedObjectIDs()));");
					this.writeLine("}");
					
					//	background list object store and sort order
					this.writeLine("var objectsById = new Object();");
					this.writeLine("var objectList = new Array();");
					this.writeLine("var objectListMatchCount = 0;");
					this.writeLine("var objectListMatchesOnly = true;");
					this.writeLine("var objectListSortField = 'start';");
					this.writeLine("var objectListSortedDescending = false;");
					
					//	get IDs of selected objects
					this.writeLine("function getSelectedObjectIDs() {");
					this.writeLine("  var selIDs = '';");
					this.writeLine("  for (var o = 0; o < objectList.length; o++)");
					this.writeLine("    if (objectList[o].isSelected) {");
					this.writeLine("      if (selIDs != '')");
					this.writeLine("        selIDs += ';'");
					this.writeLine("      selIDs += objectList[o].id;");
					this.writeLine("    }");
					this.writeLine("  return selIDs;");
					this.writeLine("}");
					
					//	sort object list, reversing sort order for second click on same column
					this.writeLine("function setObjectListSortField(sortField) {");
					this.writeLine("  if (sortField == objectListSortField)");
					this.writeLine("    objectListSortedDescending = !objectListSortedDescending;");
					this.writeLine("  else objectListSortedDescending = false;");
					this.writeLine("  objectListSortField = sortField;");
					this.writeLine("  sortObjectList(objectListSortField, objectListSortedDescending);");
					this.writeLine("}");
					this.writeLine("function sortObjectList(sortField, descending) {");
					this.writeLine("  objectList.sort(function(lo1, lo2) {");
					this.writeLine("    return (compareListObjects(lo1, lo2, sortField) * (descending ? -1 : 1));");
					this.writeLine("  });");
					this.writeLine("  var sortHeaderSuffix = (descending ? ' (d)' : ' (a)');");
					this.writeLine("  getById('objectListColumnHeadType').innerHTML = ('Type' + ((sortField == 'type') ? sortHeaderSuffix : ''));");
					this.writeLine("  getById('objectListColumnHeadStart').innerHTML = ('Start' + ((sortField == 'start') ? sortHeaderSuffix : ''));");
					this.writeLine("  getById('objectListColumnHeadEnd').innerHTML = ('End' + ((sortField == 'end') ? sortHeaderSuffix : ''));");
					this.writeLine("  getById('objectListColumnHeadValue').innerHTML = ('Value' + ((sortField == 'value') ? sortHeaderSuffix : ''));");
					this.writeLine("  var olTb = getById('objectListTableBody');");
					this.writeLine("  for (var o = 0; o < objectList.length; o++) {");
					this.writeLine("    removeElement(objectList[o].tr);");
					this.writeLine("    olTb.appendChild(objectList[o].tr);");
					this.writeLine("  }");
					this.writeLine("  addObjectListTableSpacer();");
					this.writeLine("}");
					this.writeLine("function compareListObjects(lo1, lo2, compareField) {");
					this.writeLine("  var val1 = lo1[compareField];");
					this.writeLine("  var val2 = lo2[compareField];");
					this.writeLine("  if ((compareField == 'start') || (compareField == 'end'))");
					this.writeLine("    return (val1 - val2);");
					this.writeLine("  else if (val1 && val2)");
					this.writeLine("    return val1.localeCompare(val2);");
					this.writeLine("  else if (val1)");
					this.writeLine("    return -1;");
					this.writeLine("  else if (val2)");
					this.writeLine("    return 1;");
					this.writeLine("  else return 0;");
					this.writeLine("}");
					
					//	update match status of list objects
					this.writeLine("function setNonMatch(nonMatchIDs) {");
					this.writeLine("  setListObjectMatchStatus(nonMatchIDs, false);");
					this.writeLine("}");
					this.writeLine("function setMatch(matchIDs) {");
					this.writeLine("  setListObjectMatchStatus(matchIDs, true);");
					this.writeLine("}");
					this.writeLine("function setListObjectMatchStatus(listObjectIDs, isMatch) {");
					this.writeLine("  for (var i = 0; i < listObjectIDs.length; i++) {");
					this.writeLine("    var lo = objectsById[listObjectIDs[i]];");
					this.writeLine("    if (lo != null)");
					this.writeLine("      lo.isMatch = isMatch;");
					this.writeLine("  }");
					this.writeLine("}");
					
					//	remove list objects
					this.writeLine("function removeListObjects(listObjectIDs) {");
					this.writeLine("  for (var i = 0; i < listObjectIDs.length; i++) {");
					this.writeLine("    var lo = objectsById[listObjectIDs[i]];");
					this.writeLine("    if (lo == null)");
					this.writeLine("      continue;");
					this.writeLine("    removeElement(lo.tr);");
					this.writeLine("    delete objectsById[listObjectIDs[i]];");
					this.writeLine("  }");
					this.writeLine("  for (var o = 0; o < objectList.length; o++) {");
					this.writeLine("    if (!objectsById[objectList[o].id])");
					this.writeLine("      objectList.splice(o--, 1);");
					this.writeLine("  }");
					this.writeLine("}");
					
					//	add an object to the list
					this.writeLine("function addListObject(listObject) {");
					this.writeLine("  objectsById[listObject.id] = listObject;");
					this.writeLine("  objectList[objectList.length] = listObject;");
					this.writeLine("  listObject.isSelected = false;");
					this.writeLine("  var olTr = newElement('tr', null, 'objectListRow', null);");
					this.writeLine("  var olTd;");
					this.writeLine("  olTd = newElement('td', null, 'objectListField objectListSelect', null);");
					this.writeLine("  var olCb = newElement('input', null, null, null);");
					this.writeLine("  olCb.type = 'checkbox';");
					this.writeLine("  olCb.name = 'selected';");
					this.writeLine("  olCb.value = 'true';");
					this.writeLine("  olCb.onchange = function() {");
					this.writeLine("    selectListObject(listObject, olCb.checked, true);");
					this.writeLine("  };");
					this.writeLine("  olTd.appendChild(olCb);");
					this.writeLine("  olTr.appendChild(olTd);");
					this.writeLine("  olTd = newElement('td', null, 'objectListField objectListType', listObject.type);");
					this.writeLine("  olTr.appendChild(olTd);");
					this.writeLine("  olTd = newElement('td', null, 'objectListField objectListStart', ('' + listObject.start));");
					this.writeLine("  olTr.appendChild(olTd);");
					this.writeLine("  olTd = newElement('td', null, 'objectListField objectListEnd', ('' + listObject.end));");
					this.writeLine("  olTr.appendChild(olTd);");
					this.writeLine("  olTd = newElement('td', null, 'objectListField objectListValue', listObject.value);");
					this.writeLine("  olTr.appendChild(olTd);");
					this.writeLine("  olTr.onclick = function(event) {");
					this.writeLine("    selectListObject(listObject, !listObject.isSelected, true);");
					this.writeLine("    event.stopPropagation();");
					this.writeLine("  };");
					this.writeLine("  listObject.tr = olTr;");
					this.writeLine("  listObject.cb = olCb;");
					this.writeLine("  getById('objectListTableBody').appendChild(olTr);");
					this.writeLine("  addObjectListTableSpacer();");
					this.writeLine("}");
					
					//	select specific object, or all / no objects
					this.writeLine("function selectListObject(listObject, selected, adjustButtons) {");
					this.writeLine("  if (selected == listObject.isSelected)");
					this.writeLine("    return;");
					this.writeLine("  if (selected) {");
					this.writeLine("    listObject.isSelected = true;");
					this.writeLine("    listObject.tr.className = 'objectListRow objectListRowSelected';");
					this.writeLine("    listObject.cb.checked = 'checked';");
					this.writeLine("  }");
					this.writeLine("  else {");
					this.writeLine("    listObject.isSelected = false;");
					this.writeLine("    listObject.tr.className = 'objectListRow';");
					this.writeLine("    listObject.cb.checked = null;");
					this.writeLine("  }");
					this.writeLine("  if (adjustButtons)");
					this.writeLine("    adjustObjectListFunctionButtons();");
					this.writeLine("}");
					this.writeLine("function globalSelectObjects(all) {");
					this.writeLine("  for (var o = 0; o < objectList.length; o++) {");
					this.writeLine("    if (objectList[o].isSelected != all)");
					this.writeLine("      selectListObject(objectList[o], all, false);");
					this.writeLine("  }");
					this.writeLine("  adjustObjectListFunctionButtons();");
					this.writeLine("}");
					
					//	adjust availability of functions
					this.writeLine("function adjustObjectListFunctionButtons() {");
					this.writeLine("  var selCount = 0;");
					this.writeLine("  for (var o = 0; o < objectList.length; o++) {");
					this.writeLine("    if (objectList[o].isSelected)");
					this.writeLine("      selCount++;");
					this.writeLine("  }");
					this.writeLine("  var singleType = ((selCount == 0) ? null : getSingleSelectedObjectType());");
					if (showingLayoutObjects)
						this.writeLine("  var mergeable = false;");
					else this.writeLine("  var mergeable = ((selCount > 1) && (singleType != null) && isMergeableSelection());");
					this.writeLine("  getById('mergeObjectsButton').disabled = (mergeable ? false : true);");
					this.writeLine("  getById('renameObjectsButton').disabled = ((singleType != null) ? false : true);");
					this.writeLine("  getById('removeObjectsButton').disabled = ((selCount != 0) ? false : true);");
					this.writeLine("  getById('deleteObjectsButton').disabled = ((selCount != 0) ? false : true);");
					this.writeLine("  getById('editAttributesButton').disabled = ((selCount == 1) ? false : true);");
					this.writeLine("  getById('renameAttributeButton').disabled = ((selCount != 0) ? false : true);");
					this.writeLine("  getById('modifyAttributeButton').disabled = ((selCount != 0) ? false : true);");
					this.writeLine("  getById('removeAttributeButton').disabled = ((selCount != 0) ? false : true);");
					this.writeLine("}");
					this.writeLine("function getSingleSelectedObjectType() {");
					this.writeLine("  var singleType = null;");
					this.writeLine("  for (var o = 0; o < objectList.length; o++) {");
					this.writeLine("    if (!objectList[o].isSelected)");
					this.writeLine("      continue;");
					this.writeLine("    if (singleType == null)");
					this.writeLine("      singleType = objectList[o].type;");
					this.writeLine("    else if (singleType != objectList[o].type)");
					this.writeLine("      return null;");
					this.writeLine("  }");
					this.writeLine("  return singleType;");
					this.writeLine("}");
					
					//	assess mergeability of selection
					this.writeLine("function isMergeableSelection() {");
					this.writeLine("  var minSelIndex = -1;");
					this.writeLine("  var maxSelIndex = -1;");
					this.writeLine("  var minSelStart = -1;");
					this.writeLine("  var maxSelEnd = -1;");
					this.writeLine("  for (var o = 0; o < objectList.length; o++) {");
					this.writeLine("    if (!objectList[o].isSelected)");
					this.writeLine("      continue;");
					this.writeLine("    if (objectList[o].tr.style.display == 'none')");
					this.writeLine("      continue;");
					this.writeLine("    if (minSelIndex == -1)");
					this.writeLine("      minSelIndex = o;");
					this.writeLine("    maxSelIndex = o;");
					this.writeLine("    if (minSelStart == -1)");
					this.writeLine("      minSelStart = objectList[o].start;");
					this.writeLine("    else minSelStart = Math.min(minSelStart, objectList[o].start);");
					this.writeLine("    maxSelEnd = Math.max(maxSelEnd, objectList[o].end);");
					this.writeLine("  }");
					this.writeLine("  if (minSelIndex == -1)");
					this.writeLine("    return false;");
					this.writeLine("  else if (minSelIndex == maxSelIndex)");
					this.writeLine("    return false;");
					this.writeLine("  for (var o = 0; o < objectList.length; o++) {");
					this.writeLine("    if (objectList[o].isSelected)");
					this.writeLine("      continue;");
					this.writeLine("    if ((minSelIndex < o) && (o < maxSelIndex))");
					this.writeLine("      return false;");
					this.writeLine("    if ((minSelStart < objectList[o].start) && (objectList[o].start < maxSelEnd))");
					this.writeLine("      return false;");
					this.writeLine("    if ((minSelStart < objectList[o].end) && (objectList[o].end < maxSelEnd))");
					this.writeLine("      return false;");
					this.writeLine("  }");
					this.writeLine("  return true;");
					this.writeLine("}");
					
					//	toggle list mode ('highlight matches' vs. 'show matches only')
					this.writeLine("function setObjectListMode(mode) {");
					this.writeLine("  for (var o = 0; o < objectList.length; o++) {");
					this.writeLine("    if (mode == 'r') {");
					this.writeLine("      objectList[o].tr.style.fontWeight = 'normal';");
					this.writeLine("      objectList[o].tr.style.display = (objectList[o].isMatch ? null : 'none');");
					this.writeLine("      if (!objectList[o].isMatch && objectList[o].isSelected)");
					this.writeLine("        selectListObject(objectList[o], false, false);");
					this.writeLine("    }");
					this.writeLine("    else {");
					this.writeLine("      objectList[o].tr.style.fontWeight = ((!objectListMatchesOnly && objectList[o].isMatch) ? 'bold' : 'normal');");
					this.writeLine("      objectList[o].tr.style.display = null;");
					this.writeLine("    }");
					this.writeLine("  }");
					this.writeLine("  if ((mode == 'r') || objectListMatchesOnly)");
					this.writeLine("    getById('objectListTitle').innerHTML = ('" + GoldenGateImagineWebUtils.escapeForJavaScript(objectListTitle) + " (' + objectListMatchCount + ' matches)');");
					this.writeLine("  else getById('objectListTitle').innerHTML = ('" + GoldenGateImagineWebUtils.escapeForJavaScript(objectListTitle) + " (' + objectListMatchCount + ' matches out of ' + objectList.length + ')');");
					this.writeLine("}");
					
					//	clear object list, setting all status fields back to initial values
					this.writeLine("function clearObjectList() {");
					this.writeLine("  var olTs = getById('objectListTableSpacer');");
					this.writeLine("  var olTb = getById('objectListTableBody');");
					this.writeLine("  while (olTb.firstElementChild)");
					this.writeLine("    removeElement(olTb.firstElementChild);");
					this.writeLine("  olTb.appendChild(olTs);");
					this.writeLine("  objectsById = new Object();");
					this.writeLine("  objectList.splice(0, objectList.length);");
					this.writeLine("  objectListMatchesOnly = true;");
					this.writeLine("  objectListSortField = 'start';");
					this.writeLine("  objectListSortedDescending = false;");
					this.writeLine("}");
					
					//	finish object list update (restore sort order, adjust match and non-match styles, etc.)
					this.writeLine("function finishObjectListUpdate() {");
					this.writeLine("  if (objectList.length == 0)");
					this.writeLine("    return;");
					this.writeLine("  objectListMatchCount = 0;");
					this.writeLine("  var type = objectList[0].type;");
					this.writeLine("  for (var o = 0; o < objectList.length; o++) {");
					this.writeLine("    if (objectList[o].isMatch)");
					this.writeLine("      objectListMatchCount++;");
					this.writeLine("    else objectListMatchesOnly = false;");
					this.writeLine("    objectList[o].isSelected = false;");
					this.writeLine("    objectList[o].tr.className = 'objectListRow';");
					this.writeLine("    objectList[o].cb.checked = null;");
					this.writeLine("  }");
					this.writeLine("  sortObjectList(objectListSortField, objectListSortedDescending);");
					this.writeLine("  for (var o = 0; o < objectList.length; o++) {");
					this.writeLine("    if (!objectListMatchesOnly && objectList[o].isMatch)");
					this.writeLine("      objectList[o].tr.style.fontWeight = 'bold';");
					this.writeLine("    else objectList[o].tr.style.fontWeight = 'normal';");
					this.writeLine("  }");
					this.writeLine("  if (objectListMatchesOnly) {");
					this.writeLine("    getById('objectListModeHighlight').disabled = true;");
					this.writeLine("    getById('objectListModeRestrict').disabled = true;");
					this.writeLine("  }");
					this.writeLine("  else {");
					this.writeLine("    getById('objectListModeHighlight').disabled = false;");
					this.writeLine("    getById('objectListModeRestrict').disabled = false;");
					this.writeLine("  }");
					this.writeLine("  getById('objectListModeHighlight').checked = null;");
					this.writeLine("  getById('objectListModeRestrict').checked = 'checked';");
					this.writeLine("  setObjectListMode('r');");
					this.writeLine("  adjustObjectListFunctionButtons();");
					this.writeLine("  addObjectListTableSpacer();");
					this.writeLine("}");
					
					//	call to close (not a JavaScript !!!)
					this.writeLine("function closeObjectList() {");
					this.writeLine("  window.location.href = '" + baseUrl + "close';");
					this.writeLine("}");
					
					this.writeLine("</script>");
				}
			};
		}
		public boolean handleRequest(HttpServletRequest request, HttpServletResponse response, String pathInfo) throws IOException {
			
			//	catch 'filter.js' (filter is in 'filter' parameter)
			if ("/filter.js".equals(pathInfo)) {
				
				//	read filter
				String filterString = request.getParameter("filter");
				AnnotationFilter filter = ((filterString == null) ? null : getGPathFilter(filterString));
				
				//	prepare response
				response.setContentType("text/plain");
				response.setCharacterEncoding("UTF-8");
				Writer out = new OutputStreamWriter(response.getOutputStream(), "UTF-8");
				BufferedLineWriter blw = new BufferedLineWriter(out);
				
				//	filter is good (filter error is sent by alert() calls in filtering code)
				if (filter != null) {
					
					//	apply filter to update object list (always include type matches, to facilitate filtering in browser)
					this.objectFilterString = filterString;
					this.objectFilter = filter;
					this.objectTrays = getObjects(this.objectFilter, this.wrappedDoc, this.objectTraysByID, true);
					
					//	add filter to history
					blw.writeLine("addToFilterHistory('" + GoldenGateImagineWebUtils.escapeForJavaScript(filterString) + "');");
					
					//	update object list
					blw.writeLine("clearObjectList();");
					for (int o = 0; o < this.objectTrays.length; o++)
						this.writeAddListObjectCall(blw, this.objectTrays[o]);
					blw.writeLine("finishObjectListUpdate();");
				}
				
				//	finish response
				blw.flush();
				out.flush();
				blw.close();
				return true;
			}
			
			//	call to 'merge.js'
			if ("/merge.js".equals(pathInfo)) {
				if (this.showingLayoutObjects)
					return this.sendEmptyScript(response);
				Annotation[] selectedObjects = this.getSelectedObjects(request);
				if (selectedObjects.length < 2)
					return this.sendEmptyScript(response);
				
				//	check feasibility of merger
				ImWord fImw = ((ImWord) selectedObjects[0].getAttribute(ImAnnotation.FIRST_WORD_ATTRIBUTE));
				if ((fImw == null) || (fImw.getTextStreamId() == null))
					return this.sendEmptyScript(response);
				for (int o = 1; o < selectedObjects.length; o++) {
					ImWord rImw = ((ImWord) selectedObjects[o].getAttribute(ImAnnotation.FIRST_WORD_ATTRIBUTE));
					if ((rImw == null) || !fImw.getTextStreamId().equals(rImw.getTextStreamId()))
						return this.sendEmptyScript(response);
				}
				
				//	get merge parameters
				int start = selectedObjects[0].getStartIndex();
				int end = selectedObjects[0].getEndIndex();
				for (int o = 1; o < selectedObjects.length; o++) {
					start = Math.min(start, selectedObjects[o].getStartIndex());
					end = Math.max(end, selectedObjects[o].getEndIndex());
				}
				
				//	perform merger
				Annotation mObject = null;
				for (int o = 0; o < selectedObjects.length; o++)
					if ((selectedObjects[o].getStartIndex() == start) && (selectedObjects[o].getEndIndex() == end)) {
						mObject = selectedObjects[o];
						break;
					}
				if (mObject == null)
					mObject = wrappedDoc.addAnnotation(selectedObjects[0].getType(), start, (end - start));
				for (int o = 0; o < selectedObjects.length; o++) {
					AttributeUtils.copyAttributes(selectedObjects[o], mObject, AttributeUtils.ADD_ATTRIBUTE_COPY_MODE);
					if (selectedObjects[o] != mObject)
						this.wrappedDoc.removeAnnotation(selectedObjects[o]);
				}
				
				//	send update
				this.refreshObjectListAndSendUpdates(response);
				return true;
			}
			
			//	call to 'rename.js'
			if ("/rename.js".equals(pathInfo)) {
				Annotation[] selectedObjects = this.getSelectedObjects(request);
				if (selectedObjects.length == 0)
					return this.sendEmptyScript(response);
				
				String[] types = ((this.idmp == null) ? wrappedDoc.getAnnotationTypes() : this.idmp.getAnnotationTypes());
				Arrays.sort(types, ANNOTATION_TYPE_ORDER);
				String newType = ImUtils.promptForObjectType(("Enter New " + this.layoutObjectName + " Type"), ("Enter or select new " + this.layoutObjectName.toLowerCase() + " type"), types, selectedObjects[0].getType(), true);
				if (newType == null)
					return this.sendEmptyScript(response);
				
				for (int o = 0; o < selectedObjects.length; o++)
					selectedObjects[o].changeTypeTo(newType);
				if (this.showingLayoutObjects)
					this.idmp.setRegionsPainted(newType, true);
				else this.idmp.setAnnotationsPainted(newType, true);
				
				//	send update
				this.refreshObjectListAndSendUpdates(response);
				return true;
			}
			
			//	call to 'remove.js'
			if ("/remove.js".equals(pathInfo)) {
				Annotation[] selectedObjects = this.getSelectedObjects(request);
				if (selectedObjects.length == 0)
					return this.sendEmptyScript(response);
				
				for (int o = 0; o < selectedObjects.length; o++)
					this.wrappedDoc.removeAnnotation(selectedObjects[o]);
				
				//	send update
				this.refreshObjectListAndSendUpdates(response);
				return true;
			}
			
			//	call to 'delete.js'
			if ("/delete.js".equals(pathInfo)) {
				Annotation[] selectedObjects = this.getSelectedObjects(request);
				if (selectedObjects.length == 0)
					return this.sendEmptyScript(response);
				
				for (int o = 0; o < selectedObjects.length; o++)
					this.wrappedDoc.removeTokens(selectedObjects[o]);
				
				//	send update
				this.refreshObjectListAndSendUpdates(response);
				return true;
			}
			
			//	call to 'editAttributes'
			if ("/editAttributes".equals(pathInfo)) {
				
				//	write any committed changes
				if ("POST".equalsIgnoreCase(request.getMethod())) {
					Annotation target = this.getSelectedObject(request.getParameter("id"));
					if (target != null)
						this.editingAttributesDirty = (GoldenGateImagineWebUtils.processAttributeEditorSubmission(target, request) | this.editingAttributesDirty);
					String nextTargetId = request.getParameter("nextId");
					Annotation nextTarget = this.getSelectedObject(nextTargetId);
					if (nextTarget == null) {
						this.editingAttributesId = null;
						this.sendAttributeEditorClosePage(response);
					}
					else {
						this.editingAttributesId = nextTargetId;
						this.sendAttributeEditorPage(request, response, nextTarget, nextTargetId);
					}
				}
				
				//	initial call for attribute editor page
				else {
					String targetId = request.getParameter("id");
					Annotation target = this.getSelectedObject(targetId);
					if (target == null) {
						this.editingAttributesId = null;
						this.sendAttributeEditorClosePage(response);
					}
					else {
						this.editingAttributesId = targetId;
						this.sendAttributeEditorPage(request, response, target, targetId);
					}
				}
				
				//	finally ...
				return true;
			}
			
			//	call to 'renameAttribute.js'
			if ("/renameAttribute.js".equals(pathInfo)) {
				Annotation[] selectedObjects = this.getSelectedObjects(request);
				if ((selectedObjects.length != 0) && attributeToolProvider.renameAttribute(null, selectedObjects)) {
					this.refreshObjectListAndSendUpdates(response);
					return true;
				}
				else return this.sendEmptyScript(response);
			}
			
			//	call to 'modifyAttribute.js'
			if ("/modifyAttribute.js".equals(pathInfo)) {
				Annotation[] selectedObjects = this.getSelectedObjects(request);
				if ((selectedObjects.length != 0) && attributeToolProvider.modifyAttribute(null, selectedObjects)) {
					this.refreshObjectListAndSendUpdates(response);
					return true;
				}
				else return this.sendEmptyScript(response);
			}
			
			//	call to 'removeAttribute.js'
			if ("/removeAttribute.js".equals(pathInfo)) {
				Annotation[] selectedObjects = this.getSelectedObjects(request);
				if ((selectedObjects.length != 0) && attributeToolProvider.removeAttribute(null, selectedObjects)) {
					this.refreshObjectListAndSendUpdates(response);
					return true;
				}
				else return this.sendEmptyScript(response);
			}
			
			//	none of our business
			return false;
		}
		private void refreshObjectListAndSendUpdates(HttpServletResponse response) throws IOException {
			
			//	prepare response
			response.setContentType("text/plain");
			response.setCharacterEncoding("UTF-8");
			Writer out = new OutputStreamWriter(response.getOutputStream(), "UTF-8");
			BufferedLineWriter blw = new BufferedLineWriter(out);
			
			//	do actual update and write script
			this.refreshObjectListAndWriteUpdates("", blw);
			
			//	finish response
			blw.flush();
			out.flush();
			blw.close();
		}
		private void refreshObjectListAndWriteUpdates(String prefix, BufferedLineWriter blw) throws IOException {
			
			//	re-apply filter and diff and switch objects
			ObjectTray[] objectTrays = getObjects(this.objectFilter, this.wrappedDoc, this.objectTraysByID, true);
			HashSet toRemove = new HashSet();
			for (int o = 0; o < this.objectTrays.length; o++)
				toRemove.add(this.objectTrays[o]);
			HashSet toAdd = new HashSet();
			for (int o = 0; o < objectTrays.length; o++) {
				if (!toRemove.remove(objectTrays[o]))
					toAdd.add(objectTrays[o]);
			}
			this.objectTrays = objectTrays;
			
			//	write update script
			if (toRemove.size() != 0) {
				blw.write(prefix + "removeListObjects([");
				for (Iterator rloit = toRemove.iterator(); rloit.hasNext();) {
					ObjectTray rlo = ((ObjectTray) rloit.next());
					blw.write("'" + rlo.hashCode() + "'");
					if (rloit.hasNext())
						blw.write(",");
				}
				blw.writeLine("]);");
			}
			for (Iterator aloit = toAdd.iterator(); aloit.hasNext();)
				this.writeAddListObjectCall(blw, ((ObjectTray) aloit.next()));
			StringBuffer matchIDs = new StringBuffer();
			StringBuffer nonMatchIDs = new StringBuffer();
			for (int o = 0; o < this.objectTrays.length; o++) {
				StringBuffer ids = (this.objectTrays[o].isMatch ? matchIDs : nonMatchIDs);
				if (ids.length() != 0)
					ids.append(",");
				ids.append("'" + this.objectTrays[o].hashCode() + "'");
			}
			if (matchIDs.length() != 0)
				blw.writeLine(prefix + "setMatch([" + matchIDs.toString() + "]);");
			if (nonMatchIDs.length() != 0)
				blw.writeLine(prefix + "setNonMatch([" + nonMatchIDs.toString() + "]);");
			blw.writeLine(prefix + "finishObjectListUpdate();");
		}
		private void writeAddListObjectCall(BufferedLineWriter blw, ObjectTray ot) throws IOException {
			blw.write("addListObject({");
			blw.write("  \"type\": \"" + GoldenGateImagineWebUtils.escapeForJavaScript(ot.wrappedObject.getType()) + "\",");
			blw.write("  \"start\": " + ot.wrappedObject.getStartIndex() + ",");
			blw.write("  \"end\": " + ot.wrappedObject.getEndIndex() + ",");
			blw.write("  \"value\": \"" + GoldenGateImagineWebUtils.escapeForJavaScript(ot.getObjectString()) + "\",");
			blw.write("  \"id\": \"" + ot.hashCode() + "\",");
			blw.write("  \"isMatch\": " + ot.isMatch + "");
			blw.writeLine("});");
		}
		private Annotation getSelectedObject(String objectId) {
			if (objectId == null)
				return null;
			for (int o = 0; o < this.objectTrays.length; o++) {
				if (objectId.equals("" + this.objectTrays[o].hashCode()))
					return this.objectTrays[o].wrappedObject;
			}
			return null;
		}
		private Annotation[] getSelectedObjects(HttpServletRequest request) {
			Set selectedObjectIDs = this.readSelectedObjectIDs(request);
			ArrayList selectedObjectList = new ArrayList(selectedObjectIDs.size());
			for (int o = 0; o < this.objectTrays.length; o++) {
				if (selectedObjectIDs.contains("" + this.objectTrays[o].hashCode()))
					selectedObjectList.add(this.objectTrays[o].wrappedObject);
			}
			return ((Annotation[]) selectedObjectList.toArray(new Annotation[selectedObjectList.size()]));
		}
		private Set readSelectedObjectIDs(HttpServletRequest request) {
			Set selectedObjectIDs = new HashSet();
			String selectionStr = request.getParameter("selection");
			if (selectionStr != null)
				selectedObjectIDs.addAll(Arrays.asList(selectionStr.trim().split("\\s*\\;\\s*")));
			return selectedObjectIDs;
		}
		private boolean sendEmptyScript(HttpServletResponse response) throws IOException {
			response.setContentType("text/plain");
			response.setCharacterEncoding("UTF-8");
			response.getOutputStream().flush();
			return true;
		}
		
		public boolean isCloseRequest(HttpServletRequest request, String pathInfo) {
			return ("/close".equals(pathInfo));
		}
		
		private boolean sendAttributeEditorPage(HttpServletRequest request, HttpServletResponse response, Attributed target, final String targetId) throws IOException {
			response.setContentType("text/html");
			response.setCharacterEncoding("UTF-8");
			Reader eaPageReader;
			File eaPageFile = this.host.findFile("editAttributes.html");
			if (eaPageFile == null)
				eaPageReader = new BufferedReader(new InputStreamReader(dataProvider.getInputStream("editAttributes.html")));
			else eaPageReader = new BufferedReader(new InputStreamReader(new FileInputStream(eaPageFile)));
			GoldenGateImagineWebUtils.sendHtmlPage(eaPageReader, new AttributeEditorPageBuilder(this.host, request, response, target, targetId, (this.baseUrl + "editAttributes")) {
				protected void writePageHeadExtensions() throws IOException {
					super.writePageHeadExtensions();
					
					//	submit attribute form with mode
					this.writeLine("<script type=\"text/javascript\">");
					this.writeLine("function doSubmitDataAttributes(mode) {");
					this.writeLine("  var attrForm = getById('attributeForm');");
					this.writeLine("  if (mode == 'C') {");
					this.writeLine("    while (attrForm.firstElementChild)");
					this.writeLine("      removeElement(attrForm.firstElementChild);");
					this.writeLine("    attrForm.submit();");
					this.writeLine("    return;");
					this.writeLine("  }");
					this.writeLine("  var nextId = null;");
					this.writeLine("  if (mode == 'P')");
					this.writeLine("    nextId = window.opener.getPreviousObjectId('" + targetId + "');");
					this.writeLine("  else if (mode == 'N')");
					this.writeLine("    nextId = window.opener.getNextObjectId('" + targetId + "');");
					this.writeLine("  if (nextId != null) {");
					this.writeLine("    var nextIdField = newElement('input', null, null, null);");
					this.writeLine("    nextIdField.type = 'hidden';");
					this.writeLine("    nextIdField.name = 'nextId';");
					this.writeLine("    nextIdField.value = nextId;");
					this.writeLine("    attrForm.appendChild(nextIdField);");
					this.writeLine("  }");
					this.writeLine("  submitDataAttributes();");
					this.writeLine("}");
					this.writeLine("</script>");
				}
				protected Attributed[] getContext(Attributed target) {
					Attributed[] context = new Attributed[objectTrays.length];
					for (int o = 0; o < objectTrays.length; o++)
						context[o] = objectTrays[o].wrappedObject;
					return context;
				}
				protected String getTitle(Attributed target) {
					return ("Edit " + layoutObjectName + " Attributes");
				}
				protected SubmitButton[] getButtons() {
					SubmitButton[] sbs = {
						new SubmitButton("Previous", "doSubmitDataAttributes('P');"),
						new SubmitButton("OK", "doSubmitDataAttributes('O');"),
						new SubmitButton("Cancel", "doSubmitDataAttributes('C');"),
						new SubmitButton("Next", "doSubmitDataAttributes('N');"),
						new SubmitButton("Reset", "window.location.reload();")
					};
					return sbs;
				}
			});
			eaPageReader.close();
			return true;
		}
		private void sendAttributeEditorClosePage(HttpServletResponse response) throws IOException {
			
			//	prepare response
			response.setContentType("text/html");
			response.setCharacterEncoding("UTF-8");
			Writer out = new OutputStreamWriter(response.getOutputStream(), "UTF-8");
			BufferedLineWriter blw = new BufferedLineWriter(out);
			blw.writeLine("<html><body>");
			blw.writeLine("<script type=\"text/javascript\">");
			
			//	do actual update and write script
			blw.writeLine("function doExecuteCalls() {");
			if (this.editingAttributesDirty)
				this.refreshObjectListAndWriteUpdates("window.opener.", blw);
			this.editingAttributesDirty = false;
			blw.writeLine("}");
			
			//	execute argument calls and close status window (we need to wait until pop-in parent has set window.opener and replaced close() function)
			blw.writeLine("function executeCalls() {");
			blw.writeLine("  if (window.opener && (window.opener != null)) {");
			blw.writeLine("    doExecuteCalls();");
			blw.writeLine("    window.close();");
			blw.writeLine("  }");
			blw.writeLine("  else window.setTimeout('executeCalls()', 100);");
			blw.writeLine("}");
			blw.writeLine("window.setTimeout('executeCalls()', 100);");
			
			//	finish response
			blw.writeLine("</script>");
			blw.writeLine("</body></html>");
			blw.flush();
			out.flush();
			blw.close();
		}
	}
	
	/**
	 * Open a list of Image Markup objects. Depending on the argument normalization
	 * level, the listed objects are regions or annotations. If the argument
	 * filter is null, users can ad-hoc enter custom filter expressions in the
	 * list dialog, which is helpful for exploring a document.
	 * @param title the title for the list dialog
	 * @param doc the document to list objects from
	 * @param normalizationLevel the normalization level to use
	 * @param idmp the markup panel hosting the document
	 * @param filter the filter to use
	 */
	public void listImObjects(String title, ImDocument doc, int normalizationLevel, ImDocumentMarkupPanel idmp, AnnotationFilter filter) {
		
		//	wrap document
		ImDocumentRoot wrappedDoc = new ImDocumentRoot(doc, (normalizationLevel | ImDocumentRoot.SHOW_TOKENS_AS_WORD_ANNOTATIONS | ImDocumentRoot.USE_RANDOM_ANNOTATION_IDS));
		
		//	are we showing layout objects?
		boolean showingLayoutObjects = ((normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_RAW) || (normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_WORDS));
		
		//	open list viewer around wrapper, and work on wrapper (it all goes through to document anyway)
		ObjectListDialog ond = new ObjectListDialog(title, wrappedDoc, showingLayoutObjects, idmp, filter);
		ond.setVisible(true);
		
		//	show any changes
		idmp.validate();
		idmp.repaint();
	}
	
	private StringVector customFilterHistory = new StringVector();
	private HashMap customFiltersByName = new HashMap();
	private int customFilterHistorySize = 10;
	
	private class ObjectListDialog extends DialogPanel {
		
		private ObjectListPanel objectDisplay;
		
		private String originalTitle;
		private boolean showingLayoutObjects;
		
		private JComboBox filterSelector = new JComboBox();
		private boolean customFilterSelectorKeyPressed = false;
		
		ObjectListDialog(String title, ImDocumentRoot wrappedDoc, boolean showingLayoutObjects, ImDocumentMarkupPanel target, AnnotationFilter filter) {
			super(title, true);
			this.originalTitle = title;
			this.showingLayoutObjects = showingLayoutObjects;
			
			JButton closeButton = new JButton("Close");
			closeButton.setBorder(BorderFactory.createRaisedBevelBorder());
			closeButton.setPreferredSize(new Dimension(100, 21));
			closeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					ObjectListDialog.this.dispose();
				}
			});
			
			//	create editor
			this.objectDisplay = new ObjectListPanel(wrappedDoc, showingLayoutObjects, target);
			
			//	set filter if given
			if (filter != null)
				this.objectDisplay.setFilter(filter);
			
			//	put the whole stuff together
			if (filter == null)
				this.add(this.buildFilterPanel(wrappedDoc, target), BorderLayout.NORTH);
			this.add(this.objectDisplay, BorderLayout.CENTER);
			this.add(closeButton, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(500, 650));
			this.setLocationRelativeTo(this.getOwner());
		}
		
		private JPanel buildFilterPanel(MutableAnnotation data, ImDocumentMarkupPanel target) {
			
			StringVector filters = new StringVector();
			TreeSet filterTypes = new TreeSet();
			for (int f = 0; f < customFilterHistory.size(); f++) {
				String cf = customFilterHistory.get(f);
				if ((cf.indexOf('[') == -1) && (cf.indexOf('/') == -1))
					filters.addElementIgnoreDuplicates(cf);
				else filterTypes.add(cf);
			}
			filters.addContentIgnoreDuplicates(customFilterHistory);
			if (this.showingLayoutObjects) {
				String[] layoutObjectTypes = target.getLayoutObjectTypes();
				for (int t = 0; t < layoutObjectTypes.length; t++) {
					if (target.areRegionsPainted(layoutObjectTypes[t]))
						filterTypes.add(layoutObjectTypes[t]);
				}
			}
			else {
				String[] annotTypes = target.getAnnotationTypes();
				for (int t = 0; t < annotTypes.length; t++) {
					if (target.areAnnotationsPainted(annotTypes[t]))
						filterTypes.add(annotTypes[t]);
				}
			}
			for (Iterator ftit = filterTypes.iterator(); ftit.hasNext();)
				filters.addElementIgnoreDuplicates((String) ftit.next());
			
			this.filterSelector.setModel(new DefaultComboBoxModel(filters.toStringArray()));
			this.filterSelector.setEditable(true);
			this.filterSelector.setSelectedItem("");
			this.filterSelector.setBorder(BorderFactory.createLoweredBevelBorder());
			this.filterSelector.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (!filterSelector.isVisible())
						return;
					else if (customFilterSelectorKeyPressed && !filterSelector.isPopupVisible())
						ObjectListDialog.this.applyFilter();
				}
			});
			((JTextComponent) this.filterSelector.getEditor().getEditorComponent()).addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent ke) {
					customFilterSelectorKeyPressed = true;
				}
				public void keyReleased(KeyEvent ke) {
					customFilterSelectorKeyPressed = false;
				}
			});
			
			JButton applyFilterButton = new JButton("Apply");
			applyFilterButton.setBorder(BorderFactory.createRaisedBevelBorder());
			applyFilterButton.setPreferredSize(new Dimension(50, 21));
			applyFilterButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					ObjectListDialog.this.applyFilter();
				}
			});
			
			JPanel filterPanel = new JPanel(new BorderLayout());
			filterPanel.add(this.filterSelector, BorderLayout.CENTER);
			filterPanel.add(applyFilterButton, BorderLayout.EAST);
			return filterPanel;
		}
		
		void applyFilter() {
			Object filterObject = this.filterSelector.getSelectedItem();
			if (filterObject != null) {
				final String filterString = filterObject.toString().trim();
				
				//	filter from history selected
				if (customFiltersByName.containsKey(filterString))
					this.objectDisplay.setFilter((AnnotationFilter) customFiltersByName.get(filterString));
				
				//	new filter entered
				else {
					
					//	validate & compile path expression
					String error = GPathParser.validatePath(filterString);
					GPath filterPath = null;
					if (error == null) try {
						filterPath = new GPath(filterString);
					}
					catch (Exception e) {
						error = e.getMessage();
					}
					
					//	validation successful
					if (error == null) {
						
						//	create & cache filter
						AnnotationFilter filter = new GPathAnnotationFilter(filterString, filterPath);
						customFiltersByName.put(filterString, filter);
						
						//	make way
						customFilterHistory.removeAll(filterString);
						this.filterSelector.removeItem(filterString);
						
						//	store new filter in history
						customFilterHistory.insertElementAt(filterString, 0);
						this.filterSelector.insertItemAt(filterString, 0);
						this.filterSelector.setSelectedIndex(0);
						
						//	shrink history
						while (customFilterHistory.size() > customFilterHistorySize) {
							customFiltersByName.remove(customFilterHistory.get(customFilterHistorySize));
							customFilterHistory.remove(customFilterHistorySize);
							filterSelector.removeItemAt(customFilterHistorySize);
						}
						
						//	apply filter
						this.objectDisplay.setFilter(filter);
					}
					
					//	path validation error
					else {
						DialogFactory.alert(("The expression is not valid:\n" + error), "GPath Validation", JOptionPane.ERROR_MESSAGE);
						this.filterSelector.requestFocusInWindow();
					}
				}
			}
		}
		
		private class ObjectListPanel extends JPanel {
			
			private Dimension attributeDialogSize = new Dimension(400, 300);
			private Point attributeDialogLocation = null;
			
			private JTable objectTable;
			
			private ObjectTray[] objectTrays;
			private HashMap objectTraysByID = new HashMap();
			
			private ImDocumentRoot wrappedDoc;
			private ImDocumentMarkupPanel target;
			private boolean showingLayoutObjects;
			private String layoutObjectName;
			
			private AnnotationFilter filter = null;
			private int matchObjectCount = 0;
			private boolean singleTypeMatch = false;
			
			private JRadioButton showMatches = new JRadioButton("Show Matches Only", true);
			private JRadioButton highlightMatches = new JRadioButton("Highlight Matches", false);
			
			private JButton mergeButton;
			private JButton renameButton;
			private JButton removeButton;
			private JButton deleteButton;
			private JButton editAttributesButton;
			private JButton renameAttributeButton;
			private JButton modifyAttributeButton;
			private JButton removeAttributeButton;
			
			private int sortColumn = -1;
			private boolean sortDescending = false;
			
			ObjectListPanel(ImDocumentRoot data, boolean showingLayoutObjects, ImDocumentMarkupPanel target) {
				super(new BorderLayout(), true);
				this.setBorder(BorderFactory.createEtchedBorder());
				this.wrappedDoc = data;
				this.target = target;
				this.showingLayoutObjects = showingLayoutObjects;
				this.layoutObjectName = (this.showingLayoutObjects ? "Region" : "Annotation");
				
				this.objectTable = new JTable();
				this.objectTable.setDefaultRenderer(Object.class, new TooltipAwareComponentRenderer(5, data.getTokenizer()));
				this.objectTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
				this.objectTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent lse) {
						adjustMenu();
					}
				});
				
				final JTableHeader header = this.objectTable.getTableHeader();
				header.setReorderingAllowed(false);
				header.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
		                int newSortColumn = header.columnAtPoint(me.getPoint());
		                if (newSortColumn == sortColumn)
		                	sortDescending = !sortDescending;
		                else {
		                	sortDescending = false;
		                	sortColumn = newSortColumn;
		                }
		                sortObjects(true);
					}
				});
				
				this.refreshObjectList(true);
				
				JScrollPane annotationTableBox = new JScrollPane(this.objectTable);
				
				this.showMatches.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						if (showMatches.isSelected())
							refreshObjectList(true);
					}
				});
				this.highlightMatches.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						if (highlightMatches.isSelected())
							refreshObjectList(true);
					}
				});
				
				ButtonGroup displayModeButtonGroup = new ButtonGroup();
				displayModeButtonGroup.add(this.showMatches);
				displayModeButtonGroup.add(this.highlightMatches);
				
				JPanel displayModePanel = new JPanel(new GridLayout(1, 2));
				displayModePanel.add(this.showMatches);
				displayModePanel.add(this.highlightMatches);
				
				JPanel functionPanel = new JPanel(new BorderLayout());
				functionPanel.add(this.buildMenu(), BorderLayout.NORTH);
				functionPanel.add(displayModePanel, BorderLayout.SOUTH);
				
				this.add(functionPanel, BorderLayout.NORTH);
				this.add(annotationTableBox, BorderLayout.CENTER);
			}
			
			private JPanel buildMenu() {
				this.mergeButton = new JButton("Merge");
				this.mergeButton.setToolTipText("Merge " + layoutObjectName.toLowerCase() + "s");
				this.mergeButton.setBorder(BorderFactory.createRaisedBevelBorder());
				this.mergeButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						Annotation[] annotations = getSelectedObjects();
						if (annotations.length < 2)
							return;
						int start = annotations[0].getStartIndex();
						int end = annotations[0].getEndIndex();
						for (int a = 1; a < annotations.length; a++) {
							start = Math.min(start, annotations[a].getStartIndex());
							end = Math.max(end, annotations[a].getEndIndex());
						}
						Annotation mAnnotation = null;
						for (int a = 0; a < annotations.length; a++)
							if ((annotations[a].getStartIndex() == start) && (annotations[a].getEndIndex() == end)) {
								mAnnotation = annotations[a];
								break;
							}
						if (mAnnotation == null)
							mAnnotation = wrappedDoc.addAnnotation(annotations[0].getType(), start, (end - start));
						for (int a = 0; a < annotations.length; a++) {
							AttributeUtils.copyAttributes(annotations[a], mAnnotation, AttributeUtils.ADD_ATTRIBUTE_COPY_MODE);
							if (annotations[a] != mAnnotation)
								wrappedDoc.removeAnnotation(annotations[a]);
						}
						refreshObjectList(false);
					}
				});
				
				this.renameButton = new JButton("Rename");
				this.renameButton.setToolTipText("Rename " + layoutObjectName.toLowerCase() + "s, i.e., change their type");
				this.renameButton.setBorder(BorderFactory.createRaisedBevelBorder());
				this.renameButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						Annotation[] annotations = getSelectedObjects();
						if (annotations.length == 0)
							return;
						String[] types = ((target == null) ? wrappedDoc.getAnnotationTypes() : target.getAnnotationTypes());
						Arrays.sort(types, ANNOTATION_TYPE_ORDER);
						String newType = ImUtils.promptForObjectType(("Enter New " + layoutObjectName + " Type"), ("Enter or select new " + layoutObjectName.toLowerCase() + " type"), types, annotations[0].getType(), true);
						if (newType == null)
							return;
						for (int a = 0; a < annotations.length; a++)
							annotations[a].changeTypeTo(newType);
						refreshObjectList(false);
						if (showingLayoutObjects)
							target.setRegionsPainted(newType, true);
						else target.setAnnotationsPainted(newType, true);
					}
				});
				
				this.removeButton = new JButton("Remove");
				this.removeButton.setToolTipText("Remove " + layoutObjectName.toLowerCase() + "s from the document");
				this.removeButton.setBorder(BorderFactory.createRaisedBevelBorder());
				this.removeButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						Annotation[] annotations = getSelectedObjects();
						if (annotations.length != 0) {
							for (int a = 0; a < annotations.length; a++)
								wrappedDoc.removeAnnotation(annotations[a]);
							refreshObjectList(false);
						}
					}
				});
				
				this.deleteButton = new JButton("Delete");
				this.deleteButton.setToolTipText("Delete " + layoutObjectName.toLowerCase() + "s, including their content");
				this.deleteButton.setBorder(BorderFactory.createRaisedBevelBorder());
				this.deleteButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						Annotation[] annotations = getSelectedObjects();
						if (annotations.length != 0) {
							for (int a = 0; a < annotations.length; a++)
								wrappedDoc.removeTokens(annotations[a]);
							refreshObjectList(false);
						}
					}
				});
				
				this.editAttributesButton = new JButton("Edit Attributes");
				this.editAttributesButton.setToolTipText("Edit " + layoutObjectName.toLowerCase() + " attributes");
				this.editAttributesButton.setBorder(BorderFactory.createRaisedBevelBorder());
				this.editAttributesButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						int[] selectedRows = objectTable.getSelectedRows();
						if (selectedRows.length == 1)
							editObjectAttributes(selectedRows[0]);
						else if (selectedRows.length > 1)
							editObjectAttributes(selectedRows);
					}
				});
				
				this.renameAttributeButton = new JButton("Rename Attribute");
				this.renameAttributeButton.setToolTipText("Rename an attribute of " + layoutObjectName.toLowerCase() + "s");
				this.renameAttributeButton.setBorder(BorderFactory.createRaisedBevelBorder());
				this.renameAttributeButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						renameObjectAttribute();
					}
				});
				
				this.modifyAttributeButton = new JButton("Modify Attribute");
				this.modifyAttributeButton.setToolTipText("Modify an attribute of " + layoutObjectName.toLowerCase() + "s");
				this.modifyAttributeButton.setBorder(BorderFactory.createRaisedBevelBorder());
				this.modifyAttributeButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						modifyObjectAttribute();
					}
				});
				
				this.removeAttributeButton = new JButton("Remove Attribute");
				this.removeAttributeButton.setToolTipText("Remove an attribute from " + layoutObjectName.toLowerCase() + "s");
				this.removeAttributeButton.setBorder(BorderFactory.createRaisedBevelBorder());
				this.removeAttributeButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						removeObjectAttribute();
					}
				});
				
				JPanel menuPanel = new JPanel(new GridLayout(1, 0, 2, 2));
				menuPanel.add(this.mergeButton);
				menuPanel.add(this.renameButton);
				menuPanel.add(this.removeButton);
				menuPanel.add(this.deleteButton);
				menuPanel.add(this.editAttributesButton);
				menuPanel.add(this.renameAttributeButton);
				menuPanel.add(this.modifyAttributeButton);
				menuPanel.add(this.removeAttributeButton);
				return menuPanel;
			}
			
			void adjustMenu() {
				int[] rows = this.objectTable.getSelectedRows();
				if (rows.length == 0) {
					this.mergeButton.setEnabled(false);
					this.renameButton.setEnabled(this.singleTypeMatch);
					this.removeButton.setEnabled(0 < this.matchObjectCount);
					this.deleteButton.setEnabled(false);
					this.editAttributesButton.setEnabled(this.matchObjectCount == 1);
					this.renameAttributeButton.setEnabled(0 < this.matchObjectCount);
					this.modifyAttributeButton.setEnabled(0 < this.matchObjectCount);
					this.removeAttributeButton.setEnabled(0 < this.matchObjectCount);
				}
				else {
					Arrays.sort(rows);
//					int fRow = rows[0];
//					int lRow = rows[rows.length-1];
					boolean isSingleTypeSelection = this.isSingleTypeSelection(rows);
					this.mergeButton.setEnabled(this.isMergeableSelection(rows));
//					this.renameButton.setEnabled(this.isSingleTypeSelection(rows));
					this.renameButton.setEnabled(isSingleTypeSelection);
					this.removeButton.setEnabled(true);
					this.deleteButton.setEnabled(true);
//					this.editAttributesButton.setEnabled(fRow == lRow);
					this.editAttributesButton.setEnabled(isSingleTypeSelection);
					this.renameAttributeButton.setEnabled(true);
					this.modifyAttributeButton.setEnabled(true);
					this.removeAttributeButton.setEnabled(true);
				}
			}
			
			boolean isSingleTypeSelection(int[] rows) {
				if (rows.length <= 1)
					return true;
				if (this.singleTypeMatch && this.showMatches.isSelected())
					return true;
				String type = this.objectTrays[rows[0]].wrappedObject.getType();
				for (int r = 1; r < rows.length; r++) {
					if (!type.equals(this.objectTrays[rows[r]].wrappedObject.getType()))
						return false;
				}
				return true;
			}
			
			boolean isMergeableSelection(int[] rows) {
				if (rows.length <= 1)
					return false;
				if (((this.sortColumn != 1) && (this.sortColumn != -1)) || this.sortDescending)
					return false;
				if (this.showingLayoutObjects)
					return false;
				for (int r = 1; r < rows.length; r++) {
					if ((rows[r-1] + 1) != rows[r])
						return false;
				}
				if (!this.isSingleTypeSelection(rows))
					return false;
				ImWord fImw = ((ImWord) this.objectTrays[rows[0]].wrappedObject.getAttribute(ImAnnotation.FIRST_WORD_ATTRIBUTE));
				if ((fImw == null) || (fImw.getTextStreamId() == null))
					return false;
				for (int r = 1; r < rows.length; r++) {
					ImWord rImw = ((ImWord) this.objectTrays[rows[r]].wrappedObject.getAttribute(ImAnnotation.FIRST_WORD_ATTRIBUTE));
					if ((rImw == null) || !fImw.getTextStreamId().equals(rImw.getTextStreamId()))
						return false;
				}
				return true;
			}
			
			void setFilter(AnnotationFilter filter) {
				this.filter = filter;
				this.refreshObjectList(true);
			}
			
			void refreshObjectList(boolean clearSortOrder) {
				
				//	remember selection
				int[] selectedRows = (clearSortOrder ? null : this.objectTable.getSelectedRows());
				int listSize = ((this.objectTrays == null) ? -1 : this.objectTrays.length);
				
				//	apply filter
				this.objectTrays = getObjects(this.filter, this.wrappedDoc, this.objectTraysByID, this.highlightMatches.isSelected());
				
				//	clear selection if list size changed
				if (this.objectTrays.length != listSize)
					selectedRows = null;
				
				//	set up statistics
				String matchObjectType = ((this.objectTrays.length == 0) ? "" : this.objectTrays[0].wrappedObject.getType());
				this.matchObjectCount = 0;
				
				//	check matching annotations
				for (int o = 0; o < this.objectTrays.length; o++) {
					if (!matchObjectType.equals(this.objectTrays[0].wrappedObject.getType()))
						matchObjectType = "";
					if (this.objectTrays[o].isMatch)
						this.matchObjectCount++;
				}
				
				//	more than one type
				if (matchObjectType.length() == 0) {
					this.singleTypeMatch = false;
					this.showMatches.setSelected(true);
					this.showMatches.setEnabled(false);
					this.highlightMatches.setEnabled(false);
					setTitle(originalTitle + " - " + this.objectTrays.length + " " + this.layoutObjectName + "s");
				}
				
				//	all of same type, do match highlight display if required
				else {
					this.singleTypeMatch = true;
					this.showMatches.setEnabled(true);
					this.highlightMatches.setEnabled(true);
					
					//	highlight matches
					if (this.highlightMatches.isSelected())
						setTitle(originalTitle + " - " + this.objectTrays.length + " " + this.layoutObjectName + "s, " + this.matchObjectCount + " matches");
					else setTitle(originalTitle + " - " + this.objectTrays.length + " " + this.layoutObjectName + "s");
				}
				
				this.objectTable.setModel(new ObjectListTableModel(this.objectTrays));
				this.objectTable.getColumnModel().getColumn(0).setMaxWidth(120);
				this.objectTable.getColumnModel().getColumn(1).setMaxWidth(50);
				this.objectTable.getColumnModel().getColumn(2).setMaxWidth(50);
				
				if (clearSortOrder) {
					this.sortColumn = -1;
					this.sortDescending = false;
				}
				
				if (this.sortColumn == -1)
					this.refreshDisplay();
				else this.sortObjects(selectedRows == null);
				
				if (selectedRows != null)
					for (int r = 0; r < selectedRows.length; r++) {
						if (r == 0)
							this.objectTable.setRowSelectionInterval(selectedRows[r], selectedRows[r]);
						else this.objectTable.addRowSelectionInterval(selectedRows[r], selectedRows[r]);
					}
			}
			
			void sortObjects(boolean clearSelection) {
				Arrays.sort(this.objectTrays, new Comparator() {
					public int compare(Object o1, Object o2) {
						ObjectTray at1 = ((ObjectTray) o1);
						ObjectTray at2 = ((ObjectTray) o2);
						int c;
						if (sortColumn == 0)
							c = at1.wrappedObject.getType().compareToIgnoreCase(at2.wrappedObject.getType());
						else if (sortColumn == 1)
							c = (at1.wrappedObject.getStartIndex() - at2.wrappedObject.getStartIndex());
						else if (sortColumn == 2)
							c = (at1.wrappedObject.size() - at2.wrappedObject.size());
						else if (sortColumn == 3)
							c = String.CASE_INSENSITIVE_ORDER.compare(at1.wrappedObject.getValue(), at2.wrappedObject.getValue());
						else c = 0;
						
						return ((sortDescending ? -1 : 1) * ((c == 0) ? AnnotationUtils.compare(at1.wrappedObject, at2.wrappedObject) : c));
					}
				});
				if (clearSelection)
					this.objectTable.clearSelection();
				this.refreshDisplay();
			}
			
			void refreshDisplay() {
				for(int i = 0; i < this.objectTable.getColumnCount();i++)
					this.objectTable.getColumnModel().getColumn(i).setHeaderValue(this.objectTable.getModel().getColumnName(i));
				this.objectTable.getTableHeader().revalidate();
				this.objectTable.getTableHeader().repaint();
				this.objectTable.revalidate();
				this.objectTable.repaint();
				ObjectListDialog.this.validate();
			}
			
			void editObjectAttributes(int objectIndex) {
				Annotation[] objects = new Annotation[this.objectTrays.length];
				for (int o = 0; o < this.objectTrays.length; o++)
					objects[o] = this.objectTrays[o].wrappedObject;
				
				//	keep going while user skips up and down
				boolean dirty = false;
				while (objectIndex != -1) {
					
					//	create dialog
					AttributeEditorDialog aed = new AttributeEditorDialog(ObjectListDialog.this.getDialog(), objects, objectIndex);
					aed.setVisible(true);
					
					//	read editing result
					objectIndex = aed.getSelectedObjectIndex();
					dirty = (dirty | aed.isDirty());
				}
				
				//	finish
				if (dirty)
					refreshObjectList(false);
			}
			
			void editObjectAttributes(int[] objectIndexes) {
				Annotation[] allObjects = new Annotation[this.objectTrays.length];
				for (int o = 0; o < this.objectTrays.length; o++)
					allObjects[o] = this.objectTrays[o].wrappedObject;
				Annotation[] selObjects = new Annotation[objectIndexes.length];
				for (int oi = 0; oi < objectIndexes.length; oi++)
					selObjects[oi] = this.objectTrays[objectIndexes[oi]].wrappedObject;
				
				//	create dialog
				AttributeEditorDialog aed = new AttributeEditorDialog(ObjectListDialog.this.getDialog(), selObjects, allObjects);
				aed.setVisible(true);
				
				//	finish
				if (aed.isDirty())
					refreshObjectList(false);
			}
			
			private class AttributeEditorDialog extends DialogPanel {
				private int objectIndex;
				private boolean dirty = false;
				private AttributeEditor attributeEditor;
				AttributeEditorDialog(Window owner, Annotation[] objects, int objectIndex) {
					super(owner, ("Edit " + layoutObjectName + " Attributes"), true);
					this.objectIndex = objectIndex;
					
					//	set size and location
					this.setSize(attributeDialogSize);
					if (attributeDialogLocation == null)
						this.setLocationRelativeTo(owner);
					else this.setLocation(attributeDialogLocation);
					
					//	create attribute editor
					this.attributeEditor = new AttributeEditor(objects[objectIndex], objects[objectIndex].getType(), objects[objectIndex].getValue(), objects);
					
					//	create buttons
					JButton previous = new JButton("Previous");
					previous.setBorder(BorderFactory.createRaisedBevelBorder());
					previous.setPreferredSize(new Dimension(80, 21));
					previous.setEnabled(this.objectIndex > 0);
					previous.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							AttributeEditorDialog.this.objectIndex--;
							AttributeEditorDialog.this.dirty = attributeEditor.writeChanges();
							AttributeEditorDialog.this.dispose();
						}
					});
					JButton ok = new JButton("OK");
					ok.setBorder(BorderFactory.createRaisedBevelBorder());
					ok.setPreferredSize(new Dimension(80, 21));
					ok.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							AttributeEditorDialog.this.objectIndex = -1;
							AttributeEditorDialog.this.dirty = attributeEditor.writeChanges();
							AttributeEditorDialog.this.dispose();
						}
					});
					JButton cancel = new JButton("Cancel");
					cancel.setBorder(BorderFactory.createRaisedBevelBorder());
					cancel.setPreferredSize(new Dimension(80, 21));
					cancel.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							AttributeEditorDialog.this.objectIndex = -1;
							AttributeEditorDialog.this.dispose();
						}
					});
					JButton next = new JButton("Next");
					next.setBorder(BorderFactory.createRaisedBevelBorder());
					next.setPreferredSize(new Dimension(80, 21));
					next.setEnabled((this.objectIndex + 1) < objects.length);
					next.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							AttributeEditorDialog.this.objectIndex++;
							AttributeEditorDialog.this.dirty = attributeEditor.writeChanges();
							AttributeEditorDialog.this.dispose();
						}
					});
					
					//	tray up buttons
					JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
					buttons.add(previous);
					buttons.add(ok);
					buttons.add(cancel);
					buttons.add(next);
					
					//	assemble the whole thing
					this.add(this.attributeEditor, BorderLayout.CENTER);
					this.add(buttons, BorderLayout.SOUTH);
				}
				
				AttributeEditorDialog(Window owner, Annotation[] selObjects, Annotation[] allObjects) {
					super(owner, ("Edit " + layoutObjectName + " Attributes [" + selObjects.length + "]"), true);
					this.objectIndex = -1;
					
					//	set size and location
					this.setSize(attributeDialogSize);
					if (attributeDialogLocation == null)
						this.setLocationRelativeTo(owner);
					else this.setLocation(attributeDialogLocation);
					
					//	create attribute editor
					this.attributeEditor = new AttributeEditor(selObjects, allObjects);
					
					//	create buttons
					JButton ok = new JButton("OK");
					ok.setBorder(BorderFactory.createRaisedBevelBorder());
					ok.setPreferredSize(new Dimension(80, 21));
					ok.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							AttributeEditorDialog.this.objectIndex = -1;
							AttributeEditorDialog.this.dirty = attributeEditor.writeChanges();
							AttributeEditorDialog.this.dispose();
						}
					});
					JButton cancel = new JButton("Cancel");
					cancel.setBorder(BorderFactory.createRaisedBevelBorder());
					cancel.setPreferredSize(new Dimension(80, 21));
					cancel.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							AttributeEditorDialog.this.objectIndex = -1;
							AttributeEditorDialog.this.dispose();
						}
					});
					
					//	tray up buttons
					JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
					buttons.add(ok);
					buttons.add(cancel);
					
					//	assemble the whole thing
					this.add(this.attributeEditor, BorderLayout.CENTER);
					this.add(buttons, BorderLayout.SOUTH);
				}
				
				//	remember size and location on closing
				public void dispose() {
					attributeDialogSize = this.getSize();
					attributeDialogLocation = this.getLocation(attributeDialogLocation);
					super.dispose();
				}
				
				int getSelectedObjectIndex() {
					return this.objectIndex;
				}
				
				boolean isDirty() {
					return this.dirty;
				}
			}
			
			void renameObjectAttribute() {
				Annotation[] annotations = this.getSelectedObjects();
				if (annotations.length == 0)
					return;
				if (attributeToolProvider.renameAttribute(null, annotations))
					refreshObjectList(false);
			}
			
			void modifyObjectAttribute() {
				Annotation[] annotations = this.getSelectedObjects();
				if (annotations.length == 0)
					return;
				if (attributeToolProvider.modifyAttribute(null, annotations))
					refreshObjectList(false);
			}
			
			void removeObjectAttribute() {
				Annotation[] annotations = this.getSelectedObjects();
				if (annotations.length == 0)
					return;
				if (attributeToolProvider.removeAttribute(null, annotations))
					refreshObjectList(false);
			}
			
			Annotation[] getSelectedObjects() {
				int[] rows = this.objectTable.getSelectedRows();
				if (rows.length == 0) {
					ArrayList objectList = new ArrayList();
					for (int t = 0; t < this.objectTrays.length; t++) {
						if (this.objectTrays[t].isMatch)
							objectList.add(this.objectTrays[t].wrappedObject);
					}
					return ((Annotation[]) objectList.toArray(new Annotation[objectList.size()]));
				}
				else {
					Annotation[] objects = new Annotation[rows.length];
					for (int r = 0; r < rows.length; r++)
						objects[r] = this.objectTrays[rows[r]].wrappedObject;
					return objects;
				}
			}
			
			private class ObjectListTableModel implements TableModel {
				private ObjectTray[] objectTrays;
				private boolean isMatchesOnly = true;
				ObjectListTableModel(ObjectTray[] annotations) {
					this.objectTrays = annotations;
					for (int o = 0; o < this.objectTrays.length; o++)
						this.isMatchesOnly = (this.isMatchesOnly && this.objectTrays[o].isMatch);
				}
				
				public int getColumnCount() {
					return 4;
				}
				public Class getColumnClass(int columnIndex) {
					return String.class;
				}
				public String getColumnName(int columnIndex) {
					String sortExtension = ((columnIndex == sortColumn) ? (sortDescending ? " (d)" : " (a)") : "");
					if (columnIndex == 0) return ("Type" + sortExtension);
					if (columnIndex == 1) return ("Start" + sortExtension);
					if (columnIndex == 2) return ("End" + sortExtension);
					if (columnIndex == 3) return ((showingLayoutObjects ? "Words" : "Value") + sortExtension);
					return null;
				}
				
				public int getRowCount() {
					return this.objectTrays.length;
				}
				public Object getValueAt(int rowIndex, int columnIndex) {
					if (this.isMatchesOnly || !this.objectTrays[rowIndex].isMatch) {
						if (columnIndex == 0) return this.objectTrays[rowIndex].wrappedObject.getType();
						if (columnIndex == 1) return "" + this.objectTrays[rowIndex].wrappedObject.getStartIndex();
						if (columnIndex == 2) return "" + this.objectTrays[rowIndex].wrappedObject.getEndIndex();
						if (columnIndex == 3) return this.objectTrays[rowIndex].getObjectString();
						return null;
					}
					else {
						String value = null;
						if (columnIndex == 0) value = this.objectTrays[rowIndex].wrappedObject.getType();
						if (columnIndex == 1) value = "" + this.objectTrays[rowIndex].wrappedObject.getStartIndex();
						if (columnIndex == 2) value = "" + this.objectTrays[rowIndex].wrappedObject.getEndIndex();
						if (columnIndex == 3) value = this.objectTrays[rowIndex].getObjectString();
						return ((value == null) ? null : ("<HTML><B>" + AnnotationUtils.escapeForXml(value) + "</B></HTML>"));
					}
				}
				
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return false;
				}
				public void setValueAt(Object newValue, int rowIndex, int columnIndex) {}
				
				public void addTableModelListener(TableModelListener l) {}
				public void removeTableModelListener(TableModelListener l) {}
			}
			
			private class TooltipAwareComponentRenderer extends DefaultTableCellRenderer {
				private HashSet tooltipColumns = new HashSet();
				private Tokenizer tokenizer;
				TooltipAwareComponentRenderer(int tooltipColumn, Tokenizer tokenizer) {
					this.tooltipColumns.add("" + tooltipColumn);
					this.tokenizer = tokenizer;
				}
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
					JComponent component = ((value instanceof JComponent) ? ((JComponent) value) : (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column));
					if (this.tooltipColumns.contains("" + row) && (component instanceof JComponent))
						((JComponent) component).setToolTipText(this.produceTooltipText(new PlainTokenSequence(value.toString(), this.tokenizer)));
					return component;
				}
				private String produceTooltipText(TokenSequence tokens) {
					if (tokens.size() < 100) return TokenSequenceUtils.concatTokens(tokens);
					
					StringVector lines = new StringVector();
					int startToken = 0;
					int lineLength = 0;
					Token lastToken = null;
					
					for (int t = 0; t < tokens.size(); t++) {
						Token token = tokens.tokenAt(t);
						lineLength += token.length();
						if (lineLength > 100) {
							lines.addElement(TokenSequenceUtils.concatTokens(tokens, startToken, (t - startToken + 1)));
							startToken = (t + 1);
							lineLength = 0;
						} else if (Gamta.insertSpace(lastToken, token)) lineLength++;
					}
					if (startToken < tokens.size())
						lines.addElement(TokenSequenceUtils.concatTokens(tokens, startToken, (tokens.size() - startToken)));
					
					return ("<HTML>" + lines.concatStrings("<BR>") + "</HTML>");
				}
			}
		}
	}
	
	private static ObjectTray[] getObjects(AnnotationFilter filter, MutableAnnotation wrappedDoc, HashMap objectTraysByID, boolean includeTypeMatches) {
		
		//	apply filter
		MutableAnnotation[] objects = ((filter == null) ? new MutableAnnotation[0] : filter.getMutableMatches(wrappedDoc));
		ObjectTray[] objectTrays = new ObjectTray[objects.length];
		if (objectTraysByID == null)
			objectTraysByID = new HashMap();
		
		//	set up statistics
		String type = ((objects.length == 0) ? "" : objects[0].getType());
		Set matchIDs = new HashSet();
		
		//	check matching annotations
		for (int o = 0; o < objects.length; o++) {
			if (!type.equals(objects[o].getType()))
				type = "";
			matchIDs.add(objects[o].getAnnotationID());
			if (objectTraysByID.containsKey(objects[o].getAnnotationID()))
				objectTrays[o] = ((ObjectTray) objectTraysByID.get(objects[o].getAnnotationID()));
			else {
				objectTrays[o] = new ObjectTray(objects[o]);
				objectTraysByID.put(objects[o].getAnnotationID(), objectTrays[o]);
			}
			objectTrays[o].isMatch = true;
		}
		
		//	include type matches if requested
		if (includeTypeMatches) {
			objects = wrappedDoc.getMutableAnnotations(type);
			objectTrays = new ObjectTray[objects.length];
			for (int o = 0; o < objects.length; o++) {
				if (objectTraysByID.containsKey(objects[o].getAnnotationID()))
					objectTrays[o] = ((ObjectTray) objectTraysByID.get(objects[o].getAnnotationID()));
				else {
					objectTrays[o] = new ObjectTray(objects[o]);
					objectTraysByID.put(objects[o].getAnnotationID(), objectTrays[o]);
				}
				objectTrays[o].isMatch = matchIDs.contains(objects[o].getAnnotationID());
			}
		}
		
		//	finally ...
		return objectTrays;
	}
	
	private static class ObjectTray {
		final MutableAnnotation wrappedObject;
		boolean isMatch = false;
		ObjectTray(MutableAnnotation annotation) {
			this.wrappedObject = annotation;
		}
		private static final int objectStringHalfMinTokens = 3;
		private static final int objectStringHalfMinChars = 20;
		private String objectString = null;
		String getObjectString() {
			if (this.objectString != null)
				return this.objectString;
			
			//	we only have a few tokens, concatenate them right away
			if ((this.wrappedObject.size() <= (objectStringHalfMinTokens * 2)) || (this.wrappedObject.length() <= (objectStringHalfMinChars * 2))) {
				this.objectString = TokenSequenceUtils.concatTokens(this.wrappedObject, false, true);
				System.out.println("(" + this.wrappedObject.size() + " <= 6) or (" + this.wrappedObject.length() + " <= 40)");
				System.out.println(" ==> " + this.objectString);
				return this.objectString;
			}
			
			//	get first 2 or 3 tokens or 20 characters, whichever is exceeded last
			StringBuffer osHead = new StringBuffer(this.wrappedObject.firstValue());
			int osHeadEndIndex = 1;
			int osHeadEndOffset = this.wrappedObject.firstToken().getEndOffset();
			for (int t = 1; t < this.wrappedObject.size(); t++) {
				if (this.wrappedObject.getWhitespaceAfter(t - 1).length() != 0)
					osHead.append(' ');
				osHead.append(this.wrappedObject.valueAt(t));
				osHeadEndIndex = (t + 1);
				osHeadEndOffset = this.wrappedObject.tokenAt(t).getEndOffset();
				if ((osHeadEndIndex >= objectStringHalfMinTokens) && (osHead.length() >= objectStringHalfMinChars)) {
					break;
				}
			}
			
			//	get last 2 or 3 tokens or 20 characters, whichever is exceeded last
			StringBuffer osTail = new StringBuffer(this.wrappedObject.lastValue());
			int osTailStartIndex = (this.wrappedObject.size() - 1);
			int osTailStartOffset = this.wrappedObject.lastToken().getStartOffset();
			for (int t = (this.wrappedObject.size() - 1 - 1); t >= 0; t--) {
				if ((t < osHeadEndIndex) || (this.wrappedObject.tokenAt(t).getStartOffset() < osHeadEndOffset))
					break; // stop mid-string collisions before they even happen
				if (this.wrappedObject.getWhitespaceAfter(t).length() != 0)
					osTail.insert(0, ' ');
				osTail.insert(0, this.wrappedObject.valueAt(t));
				osTailStartIndex = t;
				osTailStartOffset = this.wrappedObject.tokenAt(t).getStartOffset();
				if (((this.wrappedObject.size() - osTailStartIndex) >= objectStringHalfMinTokens) && (osTail.length() >= objectStringHalfMinChars))
					break;
			}
			
			//	get middle of object string ...
			String osMiddle;
			
			//	if we meet in the middle, we have to add a space at most
			if (osHeadEndIndex == osTailStartIndex)
				osMiddle = ((this.wrappedObject.getWhitespaceAfter(osHeadEndIndex - 1).length() == 0) ? "" : " ");
			
			//	add remainder in the middle if not longer than ' ... '
			else if ((osTailStartOffset - osHeadEndOffset) <= " ... ".length()) {
				osMiddle = (
						((this.wrappedObject.getWhitespaceAfter(osHeadEndIndex - 1).length() == 0) ? "" : " ") +
						TokenSequenceUtils.concatTokens(this.wrappedObject, osHeadEndIndex, (osTailStartIndex - osHeadEndIndex), false, true) +
						((this.wrappedObject.getWhitespaceAfter(osTailStartIndex - 1).length() == 0) ? "" : " ")
					);
			}
			
			//	add ' ... ' ellipsis sign in the middle otherwise
			else osMiddle = " ... ";
			
			//	finally ...
			this.objectString = (osHead.toString() + osMiddle + osTail.toString());
			return this.objectString;
		}
	}
}
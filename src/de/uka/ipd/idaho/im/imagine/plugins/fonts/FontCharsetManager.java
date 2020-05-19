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
package de.uka.ipd.idaho.im.imagine.plugins.fonts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.uka.ipd.idaho.easyIO.streams.CharSequenceReader;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.goldenGate.util.EditFontsButton;
import de.uka.ipd.idaho.goldenGate.util.FontEditable;
import de.uka.ipd.idaho.goldenGate.util.FontEditorDialog;
import de.uka.ipd.idaho.goldenGate.util.FontEditorPanel;
import de.uka.ipd.idaho.im.pdf.PdfFontDecoder.CustomFontDecoderCharset;
import de.uka.ipd.idaho.im.pdf.PdfFontDecoder.CustomFontDecoderCharset.FontDecoderCharsetProvider;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Manager and provider of custom font decoder character sets.
 * 
 * @author sautter
 */
public class FontCharsetManager extends AbstractResourceManager {
	
	private static final String FILE_EXTENSION = ".imCharSet";
	
	private FontDecoderCharsetProvider charsetProvider;
	
	/** public zero-argument constructor for class loading */
	public FontCharsetManager() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Font Decoder Charset Manager";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "Font Decoder Charset";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		this.charsetProvider = new FontDecoderCharsetProvider() {
			public String[] getCharsetNames() {
				String[] names = getResourceNames();
				for (int n = 0; n < names.length; n++)
					names[n] = names[n].substring(0, (names[n].length() - FILE_EXTENSION.length()));
				return names;
			}
			public Reader getNamedCharset(String name) {
				CharSequence charset = getResolvedCharset(name);
				return ((charset == null) ? null : new CharSequenceReader(charset));
			}
		};
		CustomFontDecoderCharset.addCharsetProvider(this.charsetProvider);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#exit()
	 */
	public void exit() {
		CustomFontDecoderCharset.removeCharsetProvider(this.charsetProvider);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getFileExtension()
	 */
	protected String getFileExtension() {
		return FILE_EXTENSION;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Font Decoder Charsets";
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
				createFontDecoderCharset();
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Edit");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				editFontDecoderCharsets();
			}
		});
		collector.add(mi);
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	private boolean createFontDecoderCharset() {
		return (this.createFontDecoderCharset(new StringVector(), null) != null);
	}
	
	private boolean cloneFontDecoderCharset() {
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			return this.createFontDecoderCharset();
		else {
			String name = "New " + selectedName;
			return (this.createFontDecoderCharset(this.loadListResource(selectedName), name) != null);
		}
	}
	
	private String createFontDecoderCharset(StringVector modelFdc, String name) {
		CreateFontDecoderCharsetDialog ccd = new CreateFontDecoderCharsetDialog(name, modelFdc);
		ccd.setVisible(true);
		
		if (ccd.isCommitted()) {
			StringVector fdc = ccd.getFontDecoderCharset();
			String fdcName = ccd.getFontDecoderCharsetName();
			if (!fdcName.endsWith(FILE_EXTENSION)) fdcName += FILE_EXTENSION;
			try {
				if (this.storeListResource(fdcName, fdc)) {
					this.resourceNameList.refresh();
					return fdcName;
				}
			} catch (IOException ioe) {}
		}
		return null;
	}
	
	private void editFontDecoderCharsets() {
		final FontDecoderCharsetEditorPanel[] editor = new FontDecoderCharsetEditorPanel[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit Font Decoder Charsets", true);
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
						storeListResource(editor[0].name, editor[0].getContent());
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
				createFontDecoderCharset();
			}
		});
		editButtons.add(button);
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				cloneFontDecoderCharset();
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
			StringVector list = this.loadListResource(selectedName);
			if (list == null)
				editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
			else {
				editor[0] = new FontDecoderCharsetEditorPanel(selectedName, list);
				editorPanel.add(editor[0], BorderLayout.CENTER);
			}
		}
		editDialog.add(editorPanel, BorderLayout.CENTER);
		
		editDialog.add(this.resourceNameList, BorderLayout.EAST);
		DataListListener dll = new DataListListener() {
			public void selected(String dataName) {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeListResource(editor[0].name, editor[0].getContent());
					}
					catch (IOException ioe) {
						if (DialogFactory.confirm((ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving file to " + editor[0].name + "\nProceed?"), "Could Not Save Font Decoder Charset", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
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
					StringVector list = loadListResource(dataName);
					if (list == null)
						editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
					else {
						editor[0] = new FontDecoderCharsetEditorPanel(dataName, list);
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
	
	/* (non-JavaDoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getDataNamesForResource(java.lang.String)
	 */
	public String[] getDataNamesForResource(String name) {
		StringVector nameCollector = new StringVector();
		nameCollector.addElementIgnoreDuplicates(name);
		for (int n = 0; n < nameCollector.size(); n++)
			nameCollector.addContentIgnoreDuplicates(this.getReferencedCharsetResourceNames(nameCollector.get(n)));
		for (int n = 0; n < nameCollector.size(); n++)
			nameCollector.set(n, (nameCollector.get(n) + "@" + this.getClass().getName()));
		return nameCollector.toStringArray();
	}

	/* (non-JavaDoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getRequiredResourceNames(java.lang.String, boolean)
	 */
	public String[] getRequiredResourceNames(String name, boolean recourse) {
		StringVector nameCollector = new StringVector();
		nameCollector.addContentIgnoreDuplicates(this.getReferencedCharsetResourceNames(name));
		if (recourse) {
			for (int n = 0; n < nameCollector.size(); n++)
				nameCollector.addContentIgnoreDuplicates(this.getReferencedCharsetResourceNames(nameCollector.get(n)));
		}			
		for (int n = 0; n < nameCollector.size(); n++)
			nameCollector.set(n, (nameCollector.get(n) + "@" + this.getClass().getName()));
		return nameCollector.toStringArray();
	}
	
	private CharSequence getResolvedCharset(String name) {
		StringVector charset = this.loadListResource(name + FILE_EXTENSION);
		if (charset == null)
			return null;
		
		//	distribute content of root charset to references and rest
		StringVector toResolveReferences = new StringVector();
		toResolveReferences.addElementIgnoreDuplicates(name); // make sure not to resolve root again
		StringBuffer resolvedCharset = new StringBuffer("// resolved transitive hull of " + name);
		this.addCharsetAndReferences(charset, toResolveReferences, resolvedCharset);
		
		//	load and add referenced charsets recursively (rolled out for breaking reference cycles)
		for (int r = 1 /* no need to resolve root charset again */; r < toResolveReferences.size(); r++) {
			String toResolveReference = toResolveReferences.get(r);
			StringVector referencedCharset = this.loadListResource(toResolveReference + FILE_EXTENSION);
			//	retain this one for external resolution
			if (referencedCharset == null) {
				resolvedCharset.append("\r\n");
				resolvedCharset.append("@" + toResolveReference);
			}
			
			//	distribute content of referenced charset to references and rest
			else this.addCharsetAndReferences(referencedCharset, toResolveReferences, resolvedCharset);
		}
		
		//	finally ...
		return resolvedCharset;
	}
	
	private void addCharsetAndReferences(StringVector charset, StringVector toResolveReferences, StringBuffer resolvedCharset) {
		if (charset == null)
			return;
		for (int l = 0; l < charset.size(); l++) {
			String csl = charset.get(l).trim();
			if (csl.startsWith("@")) {
				csl = csl.substring("@".length()).trim();
				if (csl.indexOf("//") != -1)
					csl = csl.substring(0, csl.indexOf("//")).trim();
				if (csl.length() != 0)
					toResolveReferences.addElementIgnoreDuplicates(csl);
			}
			else {
				resolvedCharset.append("\r\n");
				resolvedCharset.append(csl);
			}
		}
	}
	
	private String[] getReferencedCharsetResourceNames(String name) {
		StringVector list = this.loadListResource(name); // allocates a new StringVector on each call, no caching, so we can safely destroy it
		if (list == null)
			return new String[0];
		for (int l = 0; l < list.size(); l++) {
			String ll = list.get(l).trim();
			if (ll.startsWith("@")) {
				ll = ll.substring("@".length()).trim();
				if (ll.indexOf("//") != -1)
					ll = ll.substring(0, ll.indexOf("//")).trim();
				if (ll.length() == 0)
					list.remove(l--);
				else list.set(l, (ll + FILE_EXTENSION));
			}
			else list.remove(l--);
		}
		list.removeDuplicateElements();
		return list.toStringArray();
	}
	
	private class CreateFontDecoderCharsetDialog extends DialogPanel {
		private String fdcName = null;
		
		private JTextField nameField;
		private FontDecoderCharsetEditorPanel editor;
		
		CreateFontDecoderCharsetDialog(String name, StringVector fdc) {
			super("Create Font Decoder Charset", true);
			
			this.nameField = new JTextField((name == null) ? "New Font Decoder Charset" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					fdcName = nameField.getText();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					fdcName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new FontDecoderCharsetEditorPanel(name, fdc);
			
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
			return (this.fdcName != null);
		}
		
		StringVector getFontDecoderCharset() {
			return this.editor.getContent();
		}
		
		String getFontDecoderCharsetName() {
			return this.fdcName;
		}
	}
	
	private class FontDecoderCharsetEditorPanel extends JPanel implements DocumentListener, FontEditable {
		final String name;
		private StringVector content = new StringVector();
		
		private JTextArea editor;
		private JScrollPane editorBox;
		
		private String fontName = "Verdana";
		private int fontSize = 12;
		private Color fontColor = Color.BLACK;
		
		private boolean dirty = false;
		
		FontDecoderCharsetEditorPanel(String name, StringVector fdc) {
			super(new BorderLayout(), true);
			this.name = name;
			
			//	initialize editor
			this.editor = new JTextArea();
			this.editor.setEditable(true);
			
			//	wrap editor in scroll pane
			this.editorBox = new JScrollPane(this.editor);
			
			//	put the whole stuff together
			this.add(this.editorBox, BorderLayout.CENTER);
			this.add(this.getEditFontsButton(new Dimension(100, 21)), BorderLayout.SOUTH);
			this.setContent((fdc == null) ? new StringVector() : fdc);
		}
		
		StringVector getContent() {
			if (this.isDirty()) try {
				this.content.clear();
				BufferedReader cbr = new BufferedReader(new StringReader(this.editor.getText()));
				for (String cl; (cl = cbr.readLine()) != null;)
					this.content.addElement(cl);
			} catch (IOException ioe) { /* never gonna happen, but Java don't know */ }
			return this.content;
		}
		
		void setContent(StringVector list) {
			this.content = list;
			this.refreshDisplay();
			this.dirty = false;
		}
		
		boolean isDirty() {
			return this.dirty;
		}
		
		void refreshDisplay() {
			this.editor.setFont(new Font(this.fontName, Font.PLAIN, this.fontSize));
			this.editor.setText(this.content.concatStrings("\r\n"));
			this.editor.getDocument().addDocumentListener(this);
		}
		
		public void changedUpdate(DocumentEvent de) { /* attribute changes are not of interest */ }
		public void insertUpdate(DocumentEvent de) {
			this.dirty = true;
		}
		public void removeUpdate(DocumentEvent de) {
			this.dirty = true;
		}
		
		public JButton getEditFontsButton() {
			return this.getEditFontsButton(null, null, null);
		}
		public JButton getEditFontsButton(String text) {
			return this.getEditFontsButton(text, null, null);
		}
		public JButton getEditFontsButton(Dimension dimension) {
			return this.getEditFontsButton(null, dimension, null);
		}
		public JButton getEditFontsButton(Border border) {
			return this.getEditFontsButton(null, null, border);
		}
		public JButton getEditFontsButton(String text, Dimension dimension, Border border) {
			return new EditFontsButton(this, text, dimension, border);
		}
		public boolean editFonts() {
			FontEditorDialog fed = new FontEditorDialog(((JFrame) null), this.fontName, this.fontSize, this.fontColor);
			fed.setVisible(true);
			if (fed.isCommitted()) {
				FontEditorPanel font = fed.getFontEditor();
				if (font.isDirty()) {
					this.fontName = font.getFontName();
					this.fontSize = font.getFontSize();
					this.fontColor = font.getFontColor();
					dirty = true;
				}
				this.refreshDisplay();
				return true;
			}
			return false;
		}
	}
}
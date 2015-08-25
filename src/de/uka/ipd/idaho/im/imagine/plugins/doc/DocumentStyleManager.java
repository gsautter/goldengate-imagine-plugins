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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager;
import de.uka.ipd.idaho.goldenGate.util.ResourceDialog;
import de.uka.ipd.idaho.im.imagine.GoldenGateImagine;
import de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImaginePlugin;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefConstants;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils.RefData;
import de.uka.ipd.idaho.plugins.docStyle.DocumentStyle;

/**
 * This plug-in manages and provides document style parameter lists.
 * 
 * @author sautter
 */
public class DocumentStyleManager extends AbstractResourceManager implements GoldenGateImaginePlugin, DocumentStyle.Provider, BibRefConstants {
	private Settings parameterValueClasses;
	
	/** zero-argument constructor for class loading */
	public DocumentStyleManager() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Document Style Manager";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "Document Style";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		DocumentStyle.addProvider(this);
		this.parameterValueClasses = this.loadSettingsResource("styleParameters.cnfg");
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#exit()
	 */
	public void exit() {
		
		//	store all (known) style parameters and their value classes
		String[] params = DocumentStyle.getParameterNames();
		Arrays.sort(params);
		boolean paramsDirty = false;
		for (int p = 0; p < params.length; p++) {
			String paramClassName = DocumentStyle.getParameterValueClass(params[p]).getName();
			if (!paramClassName.equals(this.parameterValueClasses.getSetting(params[p]))) {
				this.parameterValueClasses.setSetting(params[p], paramClassName);
				paramsDirty = true;
			}
		}
		if (paramsDirty) try {
			this.storeSettingsResource("styleParameters.cnfg", this.parameterValueClasses);
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImaginePlugin#setImagineParent(de.uka.ipd.idaho.im.imagine.GoldenGateImagine)
	 */
	public void setImagineParent(GoldenGateImagine ggImagine) { /* we don't really need GG Imagine, at least for now */ }
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImaginePlugin#initImagine()
	 */
	public void initImagine() { /* nothing to initialize */ }
	
	/* TODO add selection actions offering to extract some style parameter value
	 * - box selection with words: extract style parameter values for some parameter group (prefix of parameter names to first dot)
	 * - box selection in general: use selection as value some box valued style parameter
	 * - word selection: extract style parameter values for some parameter group (prefix of parameter names to first dot)
	 * 
	 * - for parameter extraction, open dialog allowing to select and edit what to extract
	 * 
	 * - offer all style parameter names ever requested for selection
	 */
	
	/* TODO offer editor dialogs for document style parameter lists
	 * - show tree of parameter name prefixes, each step one tree level
	 * - show editor displaying leaves for currently selected tree branch (blank for inner tree levels)
	 * - allow flagging parameters with fixed values as anchors
	 */
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getFileExtension()
	 */
	protected String getFileExtension() {
		return ".docStyle";
	}
	
	/* TODO 
- when loading document style parameter lists, copy them into genuine Properties object via putAll() instead of handing out view on Settings
- introduce "parent" parameter
  - load parent parameter list ...
  - ... and use it as default in handed out Properties
  ==> facilitates parameter value inheritance, vastly reducing maintenance effort
  	 */
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.docStyle.DocumentStyle.Provider#getStyleFor(de.uka.ipd.idaho.gamta.Attributed)
	 */
	public Properties getStyleFor(Attributed doc) {
		String docStyleName = ((String) doc.getAttribute(DocumentStyle.DOCUMENT_STYLE_NAME_ATTRIBUTE));
		
		//	no style clues at all, have to match
		if (docStyleName == null) {
			
			//	TODO remove this after tests
			ResourceDialog dssd = ResourceDialog.getResourceDialog(this, "Select Document Style", "OK");
			dssd.setLocationRelativeTo(dssd.getOwner());
			dssd.setVisible(true);
			if (dssd.isCommitted()) {
				docStyleName = dssd.getSelectedResourceName();
				if (docStyleName == null)
					return null;
				Settings docStyle = this.loadSettingsResource(docStyleName);
				if (docStyle != null) {
					docStyle.setSetting(DocumentStyle.DOCUMENT_STYLE_NAME_ATTRIBUTE, docStyleName.substring(0, docStyleName.lastIndexOf('.')));
					return docStyle.toProperties();
				}
			}
			
			return null;
			/* TODO find style for document:
			 * - match document against (to-create) anchor style features of all styles we have
			 *   - e.g. journal name in some position
			 *   - simply add 'anchors' entry in parameter list, listing prefixes of distinctive properties (likely from meta data)
			 *   - test against fixedValue
			 * - load all style parameter lists on startup ...
			 * - ... and hold list of anchors, mapping to parameter lists
			 */
		}
		
		//	try loading style directly by name first
		Settings docStyle = this.loadSettingsResource(docStyleName + ".docStyle");
		if (docStyle != null) {
			docStyle.setSetting(DocumentStyle.DOCUMENT_STYLE_NAME_ATTRIBUTE, docStyleName);
			return docStyle.toProperties();
		}
		
		//	get bibliographic meta data if available
		RefData ref = BibRefUtils.modsAttributesToRefData(doc);
		String docYear = ref.getAttribute(YEAR_ANNOTATION_TYPE);
		if ((docYear != null) && (docStyleName.indexOf(docYear) != -1))
			docYear = null;
		String docType = ref.getAttribute(PUBLICATION_TYPE_ATTRIBUTE);
		if (docType != null)
			docType = docType.replaceAll("[^a-zA-Z]+", "_");
		if ((docType != null) && (docStyleName.indexOf(docType) != -1))
			docType = null;
		
		//	try and match name and year against available styles (sort descending to find closest style on year match)
		String[] docStyleNames = this.dataProvider.getDataNames();
		String bestDocStyleName = null;
		Arrays.sort(docStyleNames, Collections.reverseOrder());
		for (int n = 0; n < docStyleNames.length; n++) {
			if (!docStyleNames[n].startsWith(docStyleName + "."))
				continue;
			String dsn = docStyleNames[n].substring((docStyleName + ".").length());
			
			if (dsn.matches("[0-9]{4}\\..+")) {
				if ((docYear != null) && (docYear.compareTo(dsn.substring(0, 4)) < 0))
					continue;
				dsn = dsn.substring("0000.".length());
			}
			
			if ((docType != null) && dsn.startsWith(docType + ".")) {
				docStyle = this.loadSettingsResource(docStyleNames[n]);
				if (docStyle != null) {
					docStyle.setSetting(DocumentStyle.DOCUMENT_STYLE_NAME_ATTRIBUTE, docStyleNames[n].substring(0, docStyleNames[n].lastIndexOf('.')));
					return docStyle.toProperties();
				}
			}
			
			if ((bestDocStyleName == null) || (docStyleNames[n].length() < bestDocStyleName.length()))
				bestDocStyleName = docStyleNames[n];
		}
		
		//	do we have a match?
		if (bestDocStyleName != null) {
			docStyle = this.loadSettingsResource(bestDocStyleName);
			if (docStyle != null) {
				docStyle.setSetting(DocumentStyle.DOCUMENT_STYLE_NAME_ATTRIBUTE, bestDocStyleName.substring(0, bestDocStyleName.lastIndexOf('.')));
				return docStyle.toProperties();
			}
		}
		
		//	no style found
		return null;
	}
}
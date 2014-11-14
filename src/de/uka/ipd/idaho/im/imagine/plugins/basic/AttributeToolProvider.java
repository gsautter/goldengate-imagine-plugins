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
package de.uka.ipd.idaho.im.imagine.plugins.basic;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;

import javax.swing.JOptionPane;

import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImUtils.StringSelectorLine;
import de.uka.ipd.idaho.im.util.ImUtils.StringSelectorPanel;

/**
 * This class provides 'Edit' menu entries for handling attributes of both
 * regions and annotations in an Image Markup document.
 * 
 * @author sautter
 */
public class AttributeToolProvider extends AbstractImageMarkupToolProvider {
	
	private static final String ALL_OBJECTS = "<all objects>";
	private static final String ALL_ANNOTATIONS = "<all annotations>";
	private static final String ALL_REGIONS = "<all regions>";
	private static final String ALL_VALUES = "<all values>";
	
	private static final String ADD_MODE = "Add where not set";
	private static final String REPLACE_MODE = "Replace value above";
	private static final String SET_MODE = "Set everywhere";
	
	private static final String ANNOTATIONS_TYPE_SUFFIX = " (annotations)";
	private static final String REGIONS_TYPE_SUFFIX = " (regions)";
	
	private static final String DOCUMENT_ATTRIBUTE_EDITOR_IMT_NAME = "EditDocumentAttributes";
	private static final String ATTRIBUTE_RENAMER_IMT_NAME = "RenameAttribute";
	private static final String ATTRIBUTE_CHANGER_IMT_NAME = "ChangeAttribute";
	private static final String ATTRIBUTE_REMOVER_IMT_NAME = "RemoveAttribute";
	
	private ImageMarkupTool documentAttributeEditor = new DocumentAttributeEditor();
	private ImageMarkupTool attributeRenamer = new AttributeRenamer();
	private ImageMarkupTool attributeChanger = new AttributeChanger();
	private ImageMarkupTool attributeRemover = new AttributeRemover();
	
	/** zero-argument constructor for class loading */
	public AttributeToolProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Attribute Actions";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider#getEditMenuItemNames()
	 */
	public String[] getEditMenuItemNames() {
		String[] emins = {DOCUMENT_ATTRIBUTE_EDITOR_IMT_NAME, ATTRIBUTE_RENAMER_IMT_NAME, ATTRIBUTE_CHANGER_IMT_NAME, ATTRIBUTE_REMOVER_IMT_NAME};
		return emins;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		if (DOCUMENT_ATTRIBUTE_EDITOR_IMT_NAME.equals(name))
			return this.documentAttributeEditor;
		else if (ATTRIBUTE_RENAMER_IMT_NAME.equals(name))
			return this.attributeRenamer;
		else if (ATTRIBUTE_CHANGER_IMT_NAME.equals(name))
			return this.attributeChanger;
		else if (ATTRIBUTE_REMOVER_IMT_NAME.equals(name))
			return this.attributeRemover;
		else return null;
	}
	
	private class DocumentAttributeEditor implements ImageMarkupTool {
		public String getLabel() {
			return "Edit Document Attributes";
		}
		public String getTooltip() {
			return "Open the attributes of the document for editing";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			idmp.editAttributes(doc, DocumentRoot.DOCUMENT_TYPE, "");
		}
	}
	
	private class AttributeRenamer implements ImageMarkupTool {
		public String getLabel() {
			return "Rename Attribute";
		}
		public String getTooltip() {
			return "Rename an attribute of annotations or regions";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(final ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	get annotation and region types
			String[] types = getObjectTypes(idmp);
			if (types.length == 0) {
				JOptionPane.showMessageDialog(DialogFactory.getTopWindow(), "There are no attributed objects in this document.", "No Attributed Objects", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			
			//	set up cache (we don't want to collect objects or attribute names more than once)
			final HashMap cache = new HashMap();
			
			//	get all objects
			ImObject[] objects = getObjects(doc, null, ALL_OBJECTS);
			cache.put(ALL_OBJECTS, objects);
			
			//	get attribute names
			String[] attributeNames = getAttributeNames(objects);
			cache.put((ALL_OBJECTS + "@"), attributeNames);
			
			//	build input panel
			StringSelectorPanel ssp = new StringSelectorPanel("Rename Attribute");
			
			//	add type selector
			final StringSelectorLine typeSsl = ssp.addSelector("Object type", types, null, false);
			typeSsl.selector.insertItemAt(ALL_OBJECTS, 0);
			typeSsl.selector.insertItemAt(ALL_ANNOTATIONS, 1);
			typeSsl.selector.insertItemAt(ALL_REGIONS, 2);
			typeSsl.selector.setSelectedIndex(0);
			
			//	add attribute selectors
			final StringSelectorLine oldNameSsl = ssp.addSelector("Attribute to rename", attributeNames, null, false);
			final StringSelectorLine newNameSsl = ssp.addSelector("New attribute name", attributeNames, null, true);
			
			//	change attribute list when object type changes
			typeSsl.selector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					String type = typeSsl.getSelectedString();
					String[] typeAttributeNames = ((String[]) cache.get(type + "@"));
					if (typeAttributeNames == null) {
						ImObject[] typeObjects = ((ImObject[]) cache.get(type));
						if (typeObjects == null) {
							typeObjects = getObjects(doc, (type.startsWith("<") ? null : type.substring(0, type.indexOf(" ("))), (type.startsWith("<") ? type : (type.endsWith(ANNOTATIONS_TYPE_SUFFIX) ? ALL_ANNOTATIONS : ALL_REGIONS)));
							cache.put(type, typeObjects);
						}
						typeAttributeNames = getAttributeNames(typeObjects);
						cache.put((type + "@"), typeAttributeNames);
					}
					oldNameSsl.setSelectableStrings(typeAttributeNames, null, false);
					newNameSsl.setSelectableStrings(typeAttributeNames, null, true);
				}
			});
			
			//	prompt user
			if (ssp.prompt(DialogFactory.getTopWindow()) != JOptionPane.OK_OPTION)
				return;
			
			//	get data
			String type = typeSsl.getSelectedString();
			String oldName = oldNameSsl.getSelectedString();
			String newName = newNameSsl.getSelectedTypeOrName(true);
			if (newName == null)
				return;
			
			//	do the renaming, finally
			ImObject[] typeObjects = ((ImObject[]) cache.get(type));
			if (typeObjects == null)
				typeObjects = getObjects(doc, (type.startsWith("<") ? null : type.substring(0, type.indexOf(" ("))), (type.startsWith("<") ? type : (type.endsWith(ANNOTATIONS_TYPE_SUFFIX) ? ALL_ANNOTATIONS : ALL_REGIONS)));
			for (int o = 0; o < typeObjects.length; o++) {
				Object value = typeObjects[o].removeAttribute(oldName);
				if (value != null)
					typeObjects[o].setAttribute(newName, value);
			}
		}
	}
	
	private class AttributeChanger implements ImageMarkupTool {
		public String getLabel() {
			return "Modify Attribute";
		}
		public String getTooltip() {
			return "Modify an attribute of annotations or regions";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(final ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	get annotation and region types
			String[] types = getObjectTypes(idmp);
			if (types.length == 0) {
				JOptionPane.showMessageDialog(DialogFactory.getTopWindow(), "There are no attributed objects in this document.", "No Attributed Objects", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			
			//	set up cache (we don't want to collect objects, attribute names, or existing attribute values more than once)
			final HashMap cache = new HashMap();
			
			//	get all objects
			ImObject[] objects = getObjects(doc, null, ALL_OBJECTS);
			cache.put(ALL_OBJECTS, objects);
			
			//	get attribute names
			String[] attributeNames = getAttributeNames(objects);
			cache.put((ALL_OBJECTS + "@"), attributeNames);
			
			//	get attribute values
			for (int n = 0; n < attributeNames.length; n++) {
				String[] attributeValues = getAttributeValues(objects, attributeNames[n]);
				cache.put((ALL_OBJECTS + "@" + attributeNames[n]), attributeValues);
			}
			
			//	build input panel
			StringSelectorPanel ssp = new StringSelectorPanel("Modify Attribute Value");
			
			//	add type selector
			final StringSelectorLine typeSsl = ssp.addSelector("Object type", types, null, false);
			typeSsl.selector.insertItemAt(ALL_OBJECTS, 0);
			typeSsl.selector.insertItemAt(ALL_ANNOTATIONS, 1);
			typeSsl.selector.insertItemAt(ALL_REGIONS, 2);
			typeSsl.selector.setSelectedIndex(0);
			
			//	add attribute selectors
			final StringSelectorLine nameSsl = ssp.addSelector("Attribute name", attributeNames, null, true);
			
			//	change attribute list when object type changes
			typeSsl.selector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					String type = typeSsl.getSelectedString();
					String[] typeAttributeNames = ((String[]) cache.get(type + "@"));
					if (typeAttributeNames == null) {
						ImObject[] typeObjects = ((ImObject[]) cache.get(type));
						if (typeObjects == null) {
							typeObjects = getObjects(doc, (type.startsWith("<") ? null : type.substring(0, type.indexOf(" ("))), (type.startsWith("<") ? type : (type.endsWith(ANNOTATIONS_TYPE_SUFFIX) ? ALL_ANNOTATIONS : ALL_REGIONS)));
							cache.put(type, typeObjects);
						}
						typeAttributeNames = getAttributeNames(typeObjects);
						cache.put((type + "@"), typeAttributeNames);
					}
					nameSsl.setSelectableStrings(typeAttributeNames, null, true);
				}
			});
			
			//	add existing value selector
			final StringSelectorLine oldValueSsl = ssp.addSelector("Attribute value to change", new String[0], null, false);
			oldValueSsl.selector.insertItemAt(ALL_VALUES, 0);
			oldValueSsl.selector.setSelectedIndex(0);
			
			//	add new value selector
			final StringSelectorLine newValueSsl = ssp.addSelector("New attribute value", attributeNames, null, true);
			
			//	change value selection if attribute name changes
			nameSsl.selector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					String type = typeSsl.getSelectedString();
					String name = nameSsl.getSelectedString();
					String[] typeNameValues = ((String[]) cache.get(type + "@" + name));
					if (typeNameValues == null) {
						ImObject[] typeObjects = ((ImObject[]) cache.get(type));
						if (typeObjects == null) {
							typeObjects = getObjects(doc, (type.startsWith("<") ? null : type.substring(0, type.indexOf(" ("))), (type.startsWith("<") ? type : (type.endsWith(ANNOTATIONS_TYPE_SUFFIX) ? ALL_ANNOTATIONS : ALL_REGIONS)));
							cache.put(type, typeObjects);
						}
						typeNameValues = getAttributeValues(typeObjects, name);
						cache.put((type + "@" + name), typeNameValues);
					}
					oldValueSsl.setSelectableStrings(typeNameValues, null, false);
					oldValueSsl.selector.insertItemAt(ALL_VALUES, 0);
					newValueSsl.setSelectableStrings(typeNameValues, null, true);
				}
			});
			
			//	change new value input if old value changes
			oldValueSsl.selector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					String oldValue = oldValueSsl.getSelectedString();
					if (!ALL_VALUES.equals(oldValue))
						newValueSsl.selector.setSelectedItem(oldValue);
				}
			});
			
			//	add mode selector
			final StringSelectorLine modeSsl = ssp.addSelector("How to change attribute", new String[0], null, false);
			modeSsl.selector.insertItemAt(ADD_MODE, 0);
			modeSsl.selector.insertItemAt(REPLACE_MODE, 1);
			modeSsl.selector.insertItemAt(SET_MODE, 2);
			modeSsl.selector.setSelectedIndex(0);
			
			//	prompt user
			if (ssp.prompt(DialogFactory.getTopWindow()) != JOptionPane.OK_OPTION)
				return;
			
			//	get object type and attribute name
			String type = typeSsl.getSelectedString();
			String name = nameSsl.getSelectedTypeOrName(true);
			if (name == null)
				return;
			
			//	get old and new values
			String oldValue = oldValueSsl.getSelectedString();
			if (ALL_VALUES.equals(oldValue))
				oldValue = null;
			String newValue = newValueSsl.getSelectedString();
			
			//	get mode
			String mode = modeSsl.getSelectedString();
			
			//	do the change, finally
			ImObject[] typeObjects = ((ImObject[]) cache.get(type));
			if (typeObjects == null)
				typeObjects = getObjects(doc, (type.startsWith("<") ? null : type.substring(0, type.indexOf(" ("))), (type.startsWith("<") ? type : (type.endsWith(ANNOTATIONS_TYPE_SUFFIX) ? ALL_ANNOTATIONS : ALL_REGIONS)));
			for (int o = 0; o < typeObjects.length; o++) {
				Object value = typeObjects[o].getAttribute(name);
				if (ADD_MODE.equals(mode) && (value == null) && (newValue != null))
					typeObjects[o].setAttribute(name, newValue);
				else if (REPLACE_MODE.equals(mode) && ((oldValue == null) || oldValue.equals(value))) {
					if (newValue == null)
						typeObjects[o].removeAttribute(name);
					else typeObjects[o].setAttribute(name, newValue);
				}
				else if (SET_MODE.equals(mode)) {
					if (newValue == null)
						typeObjects[o].removeAttribute(name);
					else typeObjects[o].setAttribute(name, newValue);
				}
			}
		}
	}
	
	private class AttributeRemover implements ImageMarkupTool {
		public String getLabel() {
			return "Remove Attribute";
		}
		public String getTooltip() {
			return "Remove an attribute from annotations or regions";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(final ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	get annotation and region types
			String[] types = getObjectTypes(idmp);
			if (types.length == 0) {
				JOptionPane.showMessageDialog(DialogFactory.getTopWindow(), "There are no attributed objects in this document.", "No Attributed Objects", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			
			//	set up cache (we don't want to collect objects or attribute names more than once)
			final HashMap cache = new HashMap();
			
			//	get all objects
			ImObject[] objects = getObjects(doc, null, ALL_OBJECTS);
			cache.put(ALL_OBJECTS, objects);
			
			//	get attribute names
			String[] attributeNames = getAttributeNames(objects);
			cache.put((ALL_OBJECTS + "@"), attributeNames);
			
			//	build input panel
			StringSelectorPanel ssp = new StringSelectorPanel("Rename Attribute");
			
			//	add type selector
			final StringSelectorLine typeSsl = ssp.addSelector("Object type", types, null, false);
			typeSsl.selector.insertItemAt(ALL_OBJECTS, 0);
			typeSsl.selector.insertItemAt(ALL_ANNOTATIONS, 1);
			typeSsl.selector.insertItemAt(ALL_REGIONS, 2);
			typeSsl.selector.setSelectedIndex(0);
			
			//	add attribute selectors
			final StringSelectorLine nameSsl = ssp.addSelector("Attribute to remove", attributeNames, null, false);
			
			//	change attribute list when object type changes
			typeSsl.selector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					String type = typeSsl.getSelectedString();
					String[] typeAttributeNames = ((String[]) cache.get(type + "@"));
					if (typeAttributeNames == null) {
						ImObject[] typeObjects = ((ImObject[]) cache.get(type));
						if (typeObjects == null) {
							typeObjects = getObjects(doc, (type.startsWith("<") ? null : type.substring(0, type.indexOf(" ("))), (type.startsWith("<") ? type : (type.endsWith(ANNOTATIONS_TYPE_SUFFIX) ? ALL_ANNOTATIONS : ALL_REGIONS)));
							cache.put(type, typeObjects);
						}
						typeAttributeNames = getAttributeNames(typeObjects);
						cache.put((type + "@"), typeAttributeNames);
					}
					nameSsl.setSelectableStrings(typeAttributeNames, null, false);
				}
			});
			
			//	prompt user
			if (ssp.prompt(DialogFactory.getTopWindow()) != JOptionPane.OK_OPTION)
				return;
			
			//	get data
			String type = typeSsl.getSelectedString();
			String name = nameSsl.getSelectedString();
			
			//	do the removal, finally
			ImObject[] typeObjects = ((ImObject[]) cache.get(type));
			if (typeObjects == null)
				typeObjects = getObjects(doc, (type.startsWith("<") ? null : type.substring(0, type.indexOf(" ("))), (type.startsWith("<") ? type : (type.endsWith(ANNOTATIONS_TYPE_SUFFIX) ? ALL_ANNOTATIONS : ALL_REGIONS)));
			for (int o = 0; o < typeObjects.length; o++)
				typeObjects[o].removeAttribute(name);
		}
	}
	
	private String[] getObjectTypes(ImDocumentMarkupPanel idmp) {
		TreeSet types = new TreeSet();
		String[] annotTypes = idmp.document.getAnnotationTypes();
		for (int t = 0; t < annotTypes.length; t++)
			types.add(annotTypes[t] + ANNOTATIONS_TYPE_SUFFIX);
		ImPage[] pages = idmp.document.getPages();
		for (int p = 0; p < pages.length; p++) {
			String[] regionTypes = pages[p].getRegionTypes();
			for (int t = 0; t < regionTypes.length; t++)
				types.add(regionTypes[t] + REGIONS_TYPE_SUFFIX);
		}
		return ((String[]) types.toArray(new String[types.size()]));
	}
	
	private ImObject[] getObjects(ImDocument doc, String type, String objType) {
		LinkedList objects = new LinkedList();
		if (ALL_OBJECTS.equals(objType) || ALL_REGIONS.equals(objType)) {
			ImPage[] pages = doc.getPages();
			for (int p = 0; p < pages.length; p++)
				objects.addAll(Arrays.asList(pages[p].getRegions(type)));
		}
		if (ALL_OBJECTS.equals(objType) || ALL_ANNOTATIONS.equals(objType))
			objects.addAll(Arrays.asList(doc.getAnnotations(type)));
		return ((ImObject[]) objects.toArray(new ImObject[objects.size()]));
	}
	
	private String[] getAttributeNames(ImObject[] objects) {
		TreeSet names = new TreeSet();
		for (int o = 0; o < objects.length; o++)
			names.addAll(Arrays.asList(objects[o].getAttributeNames()));
		return ((String[]) names.toArray(new String[names.size()]));
	}
	
	private String[] getAttributeValues(ImObject[] objects, String name) {
		TreeSet values = new TreeSet();
		for (int o = 0; o < objects.length; o++) {
			Object valueObj = objects[o].getAttribute(name);
			if (valueObj == null)
				continue;
			String value = valueObj.toString().trim();
			if (value.length() != 0)
				values.add(value);
		}
		return ((String[]) values.toArray(new String[values.size()]));
	}
}
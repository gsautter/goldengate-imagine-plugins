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
package de.uka.ipd.idaho.im.imagine.plugins.basic;

import java.awt.Color;
import java.awt.Point;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.AttributeUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.GPathException;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.gamta.ImDocumentRoot;
import de.uka.ipd.idaho.im.gamta.ImTokenSequence;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.TwoClickSelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.im.util.ImUtils.StringPair;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * This class provides basic actions for editing annotations.
 * 
 * @author sautter
 */
public class AnnotationActionProvider extends AbstractSelectionActionProvider implements ImageMarkupToolProvider {
	private static final String ANNOT_RETYPER_IMT_NAME = "RetypeAnnots";
	private static final String ANNOT_REMOVER_IMT_NAME = "RemoveAnnots";
	private static final String ANNOT_DUPLICATE_REMOVER_IMT_NAME = "RemoveDuplicateAnnots";
	private static final String ANNOT_NESTING_CHECKER_IMT_NAME = "CheckAnnotNesting";
	
	private ImageMarkupTool annotRetyper = new AnnotRetyper();
	private ImageMarkupTool annotRemover = new AnnotRemover();
	private ImageMarkupTool annotDuplicateRemover = new AnnotDuplicateRemover();
	private ImageMarkupTool annotNestingChecker = new AnnotNestingEnforcer();
	
	private static final String SPLIT_OPERATION = "split";
	private static final String REMOVE_OPERATION = "remove";
	private static final String CUT_TO_START_OPERATION = "cutToStart";
	private static final String CUT_TO_END_OPERATION = "cutToEnd";
	
	private static final String EXTEND_FIRST_OPERATION = "extendFirst";
	private static final String SPLIT_FIRST_OPERATION = "splitFirst";
	private static final String REMOVE_FIRST_OPERATION = "removeFirst";
	private static final String CUT_FIRST_TO_START_OPERATION = "cutFirstToStart";
	private static final String CUT_FIRST_TO_END_OPERATION = "cutFirstToEnd";
	private static final String EXTEND_SECOND_OPERATION = "extendSecond";
	private static final String SPLIT_SECOND_OPERATION = "splitSecond";
	private static final String REMOVE_SECOND_OPERATION = "removeSecond";
	private static final String CUT_SECOND_TO_START_OPERATION = "cutSecondToStart";
	private static final String CUT_SECOND_TO_END_OPERATION = "cutSecondToEnd";
	
	private Properties annotTypeOperations = new Properties();
	private LinkedHashMap annotPathOperations = new LinkedHashMap();
	
	private TreeSet annotTypes = new TreeSet(String.CASE_INSENSITIVE_ORDER);
	
	/** public zero-argument constructor for class loading */
	public AnnotationActionProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Annotation Actions";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		
		//	load pre-configured annotation types from config file
		try {
			StringVector annotTypes = StringVector.loadList(new InputStreamReader(this.dataProvider.getInputStream("annotationTypes.cnfg"), "UTF-8"));
			for (int t = 0; t < annotTypes.size(); t++) {
				String annotType = annotTypes.get(t).trim();
				if ((annotType.length() != 0) && !annotType.startsWith("//"))
					this.annotTypes.add(annotType);
			}
		} catch (IOException ioe) {}
		
		//	read cleanup operations for individual annotation types
		try {
			Reader tor = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream("annotationCleanupOptions.cnfg")));
			StringVector typeOperationList = StringVector.loadList(tor);
			tor.close();
			for (int t = 0; t < typeOperationList.size(); t++) {
				String typeOperation = typeOperationList.get(t).trim();
				if (typeOperation.length() == 0)
					continue;
				if (typeOperation.startsWith("//"))
					continue;
				String[] typeAndOperation = typeOperation.split("\\t");
				if (typeAndOperation.length != 2)
					continue;
				typeAndOperation[0] = typeAndOperation[0].trim();
				if (typeAndOperation[0].startsWith("/"))
					this.annotPathOperations.put(typeAndOperation[0], typeAndOperation[1].trim());
				else this.annotTypeOperations.setProperty(typeAndOperation[0], typeAndOperation[1].trim());
			}
		} catch (IOException ioe) {}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getEditMenuItemNames()
	 */
	public String[] getEditMenuItemNames() {
		String[] emins = {ANNOT_RETYPER_IMT_NAME, ANNOT_REMOVER_IMT_NAME, ANNOT_DUPLICATE_REMOVER_IMT_NAME};
		return emins;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getToolsMenuItemNames()
	 */
	public String[] getToolsMenuItemNames() {
		String[] tmins = {ANNOT_NESTING_CHECKER_IMT_NAME};
		return tmins;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		if (ANNOT_RETYPER_IMT_NAME.equals(name))
			return this.annotRetyper;
		else if (ANNOT_REMOVER_IMT_NAME.equals(name))
			return this.annotRemover;
		else if (ANNOT_DUPLICATE_REMOVER_IMT_NAME.equals(name))
			return this.annotDuplicateRemover;
		else if (ANNOT_NESTING_CHECKER_IMT_NAME.equals(name))
			return this.annotNestingChecker;
		else return null;
	}
	
	private class AnnotRetyper implements ImageMarkupTool {
		public String getLabel() {
			return "Change Annotation Type";
		}
		public String getTooltip() {
			return "Replace an annotation type with another one";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	get region types
			TreeSet allAnnotTypes = new TreeSet();
			TreeSet paintedAnnotTypes = new TreeSet();
			String[] annotTypes = doc.getAnnotationTypes();
			for (int t = 0; t < annotTypes.length; t++) {
				allAnnotTypes.add(annotTypes[t]);
				if (idmp.areAnnotationsPainted(annotTypes[t]))
					paintedAnnotTypes.add(annotTypes[t]);
			}
			
			//	nothing to work with
			if (allAnnotTypes.isEmpty()) {
				DialogFactory.alert("There are no annotations in this document.", "No Annotations", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			
			//	get annotation types
			annotTypes = (paintedAnnotTypes.isEmpty() ? ((String[]) allAnnotTypes.toArray(new String[allAnnotTypes.size()])) : ((String[]) paintedAnnotTypes.toArray(new String[paintedAnnotTypes.size()])));
			StringPair annotTypeChange = ImUtils.promptForObjectTypeChange("Select Annotation Type", "Select type of annotation to change", "Select or enter new annotation type", annotTypes, annotTypes[0], true);
			if (annotTypeChange == null)
				return;
			
			//	process annotations of selected type
			ImAnnotation[] annots = doc.getAnnotations(annotTypeChange.strOld);
			for (int a = 0; a < annots.length; a++)
				annots[a].setType(annotTypeChange.strNew);
			
			//	perform document annotation cleanup to deal with whatever duplicates we might have incurred
			doc.cleanupAnnotations();
		}
	}
	
	private class AnnotRemover implements ImageMarkupTool {
		public String getLabel() {
			return "Remove Annotations";
		}
		public String getTooltip() {
			return "Remove annotations from document";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	get annotations types
			TreeSet allAnnotTypes = new TreeSet();
			TreeSet paintedAnnotTypes = new TreeSet();
			String[] annotTypes = doc.getAnnotationTypes();
			for (int t = 0; t < annotTypes.length; t++) {
				allAnnotTypes.add(annotTypes[t]);
				if (idmp.areAnnotationsPainted(annotTypes[t]))
					paintedAnnotTypes.add(annotTypes[t]);
			}
			
			//	nothing to work with
			if (allAnnotTypes.isEmpty()) {
				DialogFactory.alert("There are no annotations in this document.", "No Annotations", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			
			//	get annotation types
			annotTypes = (paintedAnnotTypes.isEmpty() ? ((String[]) allAnnotTypes.toArray(new String[allAnnotTypes.size()])) : ((String[]) paintedAnnotTypes.toArray(new String[paintedAnnotTypes.size()])));
			String annotType = ImUtils.promptForObjectType("Select Annotation Type", "Select type of annotations", annotTypes, annotTypes[0], false);
			if (annotType == null)
				return;
			
			//	remove annotations of selected type
			ImAnnotation[] annots = doc.getAnnotations(annotType);
			for (int a = 0; a < annots.length; a++)
				doc.removeAnnotation(annots[a]);
		}
	}
	
	private class AnnotDuplicateRemover implements ImageMarkupTool {
		public String getLabel() {
			return "Remove Duplicate Annotations";
		}
		public String getTooltip() {
			return "Remove duplicate annotations from document";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			doc.cleanupAnnotations();
		}
	}
	
	private class AnnotNestingEnforcer implements ImageMarkupTool {
		public String getLabel() {
			return "Check Annotation Nesting";
		}
		public String getTooltip() {
			return "Check the nesting of annotations in the document, and cut interleaving ones.";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	we only process the document as a whole
			if (annot != null)
				return;
			
			//	clean up document data structures so we have a clean basis to start from 
			doc.cleanupAnnotations();
			
			//	get document annotations
			ImAnnotation[] annots = doc.getAnnotations();
			
			//	investigate annotations
			pm.setBaseProgress(0);
			pm.setMaxProgress(10);
			pm.setStep("Indexing Annotations by Text Stream");
			HashMap annotsByTextStream = new LinkedHashMap();
			for (int a = 0; a < annots.length; a++) {
				pm.setProgress((a * 100) / annots.length);
				
				//	sort annotations by text stream ID ...
				String annotTextStreamId = annots[a].getFirstWord().getTextStreamId();
				if (annotTextStreamId.equals(annots[a].getLastWord().getTextStreamId())) {
					ArrayList textStreamAnnots = ((ArrayList) annotsByTextStream.get(annotTextStreamId));
					if (textStreamAnnots == null) {
						textStreamAnnots = new ArrayList();
						annotsByTextStream.put(annotTextStreamId, textStreamAnnots);
					}
					textStreamAnnots.add(annots[a]);
				}
				
				//	... and remove annotations whose start and end word belong to different logical text streams
				else {
					doc.removeAnnotation(annots[a]);
					annots[a] = null;
				}
			}
			
			//	process annotations of each text stream
			pm.setBaseProgress(10);
			pm.setMaxProgress(100);
			pm.setStep("Checking Annotations");
			int annotsChecked = 0;
			for (Iterator tsidit = annotsByTextStream.keySet().iterator(); tsidit.hasNext();) {
				String textStreamId = ((String) tsidit.next());
				ArrayList textStreamAnnots = ((ArrayList) annotsByTextStream.get(textStreamId));
				pm.setBaseProgress(10 + ((annotsChecked * 90) / annots.length));
				pm.setMaxProgress(10 + (((annotsChecked + textStreamAnnots.size()) * 90) / annots.length));
				this.processAnnots(doc, ((ImAnnotation[]) textStreamAnnots.toArray(new ImAnnotation[textStreamAnnots.size()])), pm);
				annotsChecked += textStreamAnnots.size();
			}
			
			//	clean up document data structures
			doc.cleanupAnnotations();
		}
		
		private void processAnnots(ImDocument doc, ImAnnotation[] annots, ProgressMonitor pm) {
			
			//	anything to do?
			if (annots.length < 2)
				return;
			
			//	sort annotations
			Arrays.sort(annots, annotationOrder);
			
			//	resolve annotation vs. paragraph break issues first
			pm.setInfo(" - checking structure annotations against paragraphs");
			for (int a = 0; a < annots.length; a++) {
				pm.setProgress((a * 20) / annots.length);
				
				//	check if annotation spans paragraphs
				ImWord firstWord = annots[a].getFirstWord();
				boolean firstIsParaStart = ((firstWord.getPreviousWord() == null) || (firstWord.getPreviousWord().getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END));
				ImWord lastWord = annots[a].getLastWord();
				boolean lastIsParaEnd = ((lastWord.getNextWord() == null) || (lastWord.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END));
				
				//	nothing to do in this step if so (splitting at internal paragraph breaks happens below)
				if (firstIsParaStart && lastIsParaEnd)
					continue;
				
				//	take care of annotation start if required
				String startOperation = (firstIsParaStart ? null : annotTypeOperations.getProperty(ImAnnotation.PARAGRAPH_TYPE + ">" + annots[a].getType()));
				if (EXTEND_SECOND_OPERATION.equals(startOperation))
					for (ImWord imw = annots[a].getFirstWord(); imw != null; imw = imw.getPreviousWord()) {
						boolean imwIsParaStart = ((imw.getPreviousWord() == null) || (imw.getPreviousWord().getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END));
						if (imwIsParaStart) {
							annots[a].setFirstWord(imw);
							break;
						}
					}
				
				//	take care of annotation end if required
				String endOperation = (lastIsParaEnd ? null : annotTypeOperations.getProperty(annots[a].getType() + ">" + ImAnnotation.PARAGRAPH_TYPE));
				if (EXTEND_FIRST_OPERATION.equals(endOperation))
					for (ImWord imw = annots[a].getLastWord(); imw != null; imw = imw.getNextWord()) {
						boolean imwIsParaEnd = ((imw.getNextWord() == null) || (imw.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END));
						if (imwIsParaEnd) {
							annots[a].setLastWord(imw);
							break;
						}
					}
			}
			
			//	re-sort annotations
			Arrays.sort(annots, annotationOrder);
			
			//	group annotations into structure (comprising whole paragraphs) and details
			//	chop up (or cut) detail level annotations inconsistent with paragraph breaks
			pm.setInfo(" - checking detail annotations against paragraphs");
			ArrayList structAnnotList = new ArrayList();
			ArrayList detailAnnotList = new ArrayList();
			for (int a = 0; a < annots.length; a++) {
				pm.setProgress(20 + ((a * 40) / annots.length));
				
				//	check if annotation spans paragraphs
				ImWord firstWord = annots[a].getFirstWord();
				boolean firstIsParaStart = ((firstWord.getPreviousWord() == null) || (firstWord.getPreviousWord().getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END));
				ImWord lastWord = annots[a].getLastWord();
				boolean lastIsParaEnd = ((lastWord.getNextWord() == null) || (lastWord.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END));
				
				//	paragraph spanning, and no explicit operation defined for line breaks
				if (firstIsParaStart && lastIsParaEnd && (annotTypeOperations.getProperty(annots[a].getType()) == null)) {
					structAnnotList.add(annots[a]);
					continue;
				}
				
				//	check annotation for inner paragraph breaks
				for (ImWord imw = firstWord; imw != null; imw = imw.getNextWord()) {
					if (imw == lastWord)
						break;
					boolean isParaEnd = ((imw.getNextWord() == null) || (imw.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END));
					if (!isParaEnd)
						continue;
					
					//	perform cleanup operation
					String operation = annotTypeOperations.getProperty(annots[a].getType(), CUT_TO_START_OPERATION);
					if (CUT_TO_START_OPERATION.equals(operation)) {
						annots[a].setLastWord(imw);
						break; // we're done, no more cuts to come
					}
					else if (CUT_TO_END_OPERATION.equals(operation)) {
						if (imw.getNextWord() != null)
							annots[a].setFirstWord(imw.getNextWord());
						// need to keep on looking for further paragraph breaks
					}
					else if (SPLIT_OPERATION.equals(operation)) {
						ImAnnotation splitAnnot = doc.addAnnotation(annots[a].getFirstWord(), imw, annots[a].getType());
						splitAnnot.copyAttributes(annots[a]);
						detailAnnotList.add(splitAnnot); // store first part
						if (imw.getNextWord() != null)
							annots[a].setFirstWord(imw.getNextWord()); // proceed with remainder
					}
					else if (REMOVE_OPERATION.equals(operation)) {
						doc.removeAnnotation(annots[a]);
						annots[a] = null;
						break; // we're done, this one's out altogether
					}
				}
				
				//	this one came all the way through, store it
				if (annots[a] != null)
					detailAnnotList.add(annots[a]);
			}
			
			//	check nesting on structural annotations
			pm.setProgress(60);
			pm.setInfo(" - checking structural annotation nesting");
			this.cleanUpInterleavingAnnots(doc, structAnnotList);
			
			//	handle detail annotations
			pm.setProgress(annotPathOperations.isEmpty() ? 80 : 73);
			pm.setInfo(" - checking detail annotation nesting");
			
			//	extend annotations to cover entirety of joined (or hyphenated) words
			for (int a = 0; a < detailAnnotList.size(); a++) {
				ImAnnotation annot = ((ImAnnotation) detailAnnotList.get(a));
				
				//	seek potential new start word further upstream
				ImWord first = annot.getFirstWord();
				while (first != null) {
					if (first.getPreviousRelation() == ImWord.NEXT_RELATION_CONTINUE) {}
					else if (first.getPreviousRelation() == ImWord.NEXT_RELATION_HYPHENATED) {}
					else break;
					first = first.getPreviousWord();
				}
				
				//	seek potential new end word further downstream
				ImWord last = annot.getLastWord();
				while (last != null) {
					if (last.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE) {}
					else if (last.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED) {}
					else break;
					last = last.getNextWord();
				}
				
				//	adjust annotation if anything changed
				if ((first != null) && (first != annot.getFirstWord()))
					annot.setFirstWord(first);
				if ((last != null) && (last != annot.getLastWord()))
					annot.setLastWord(last);
			}
			
			//	re-sort detail annotations in case we changed anything
			Collections.sort(detailAnnotList, annotationOrder);
			
			//	clean up nesting
			this.cleanUpInterleavingAnnots(doc, detailAnnotList);
			
			//	merge adjacent equal detail annotations (type and attributes)
			//	TEST Zootaxa/zootaxa.4772.2.1.pdf.imd (version on server is fixed)
			ArrayList openAnnots = new ArrayList();
			for (int a = 0; a < detailAnnotList.size(); a++) {
				ImAnnotation annot = ((ImAnnotation) detailAnnotList.get(a));
				
				//	make sure not to merge across table cell boundaries
				if (ImWord.TEXT_STREAM_TYPE_TABLE.equals(annot.getFirstWord().getTextStreamType()))
					continue;
				
				//	find closest end of annotations starting before current one
				int minOpenEndPos = Integer.MAX_VALUE;
				if (openAnnots.size() != 0)
					for (int oa = 0; oa < openAnnots.size(); oa++) {
						ImAnnotation oAnnot = ((ImAnnotation) openAnnots.get(oa));
						if (oAnnot.getLastWord().getTextStreamPos() < annot.getFirstWord().getTextStreamPos())
							openAnnots.remove(oa--); // we've proceeded beyond the end of this one
						else if (oAnnot.getFirstWord().getTextStreamPos() == annot.getFirstWord().getTextStreamPos())
							break; // anything to come can just as well be nested in current annotation
						else minOpenEndPos = Math.min(minOpenEndPos, oAnnot.getLastWord().getTextStreamPos());
					}
				
				//	find successor to merge with, as well as closest end of adjacent annotations
				ImAnnotation mergeAnnot = null;
				int minNonMergeEnd = Integer.MAX_VALUE;
				for (int ma = (a+1); ma < detailAnnotList.size(); ma++) {
					ImAnnotation mAnnot = ((ImAnnotation) detailAnnotList.get(ma));
					if (mAnnot.getFirstWord().getTextStreamPos() > (annot.getLastWord().getTextStreamPos() + 1))
						break; // no use seeking beyond gap
					
					//	nested inside current annotation
					if (mAnnot.getLastWord().getTextStreamPos() <= annot.getLastWord().getTextStreamPos()) {
						
						//	"merge" on the fly by removing nested equal annotation
						if (annot.getType().equals(mAnnot.getType()) && AttributeUtils.hasEqualAttributes(annot, mAnnot)) {
							doc.removeAnnotation(mAnnot);
							detailAnnotList.remove(ma--);
						}
						
						//	no need to worry about this one with regard to nesting
						continue;
					}
					
					//	this one looks good, also regarding open annotations
					if (annot.getType().equals(mAnnot.getType()) && (mAnnot.getLastWord().getTextStreamPos() <= minOpenEndPos) && AttributeUtils.hasEqualAttributes(annot, mAnnot)) {
						mergeAnnot = mAnnot;
						doc.removeAnnotation(mAnnot);
						detailAnnotList.remove(ma);
						break;
					}
					
					//	this one is adjacent to current annotation, need to observe nesting
					if (mAnnot.getFirstWord().getTextStreamPos() == (annot.getLastWord().getTextStreamPos() + 1)) {
						if (mAnnot.getLastWord().getTextStreamPos() < minNonMergeEnd)
							break; // there is some further reaching annotation in the way
						else minNonMergeEnd = Math.min(minNonMergeEnd, mAnnot.getLastWord().getTextStreamPos()); // no merging across start of this one
					}
				}
				
				//	anything to merge with?
				if (mergeAnnot != null) {
					annot.setLastWord(mergeAnnot.getLastWord());
					a--; // start over with new end position
				}
				
				//	mark ourselves as open for downstream annotations
				else openAnnots.add(annot);
			}
			
			//	any custom cleanup rules to apply? (we can save the XML wrapper hassle if not)
			if (annotPathOperations.isEmpty())
				return;
			
			//	create XML wrapper to execute path expressions
			ArrayList annotList = (structAnnotList.isEmpty() ? detailAnnotList : structAnnotList);
			ImWord firstWord = ((ImAnnotation) annotList.get(0)).getFirstWord();
			while (firstWord.getPreviousWord() != null)
				firstWord = firstWord.getPreviousWord();
			ImWord lastWord = ((ImAnnotation) annotList.get(annotList.size()-1)).getLastWord();
			while (lastWord.getNextWord() != null)
				lastWord = lastWord.getNextWord();
			ImDocumentRoot xmlDoc = new ImDocumentRoot(firstWord, lastWord, ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS);
			
			//	apply custom cleanup rules
			pm.setProgress(87);
			pm.setInfo(" - applying custom cleanup rules");
			for (Iterator pit = annotPathOperations.keySet().iterator(); pit.hasNext();) try {
				String path = ((String) pit.next());
				String operation = ((String) annotPathOperations.get(path));
				Annotation[] pAnnots = GPath.evaluatePath(xmlDoc, path, null);
				for (int a = 0; a < pAnnots.length; a++) {
					if (REMOVE_OPERATION.equals(operation))
						xmlDoc.removeAnnotation(pAnnots[a]);
					else pAnnots[a].changeTypeTo(operation);
				}
			} catch (GPathException gpe) {}
		}
		
		private void cleanUpInterleavingAnnots(ImDocument doc, ArrayList annotList) {
			boolean annotsChanged;
			int minFirstAnnotIndex = 0;
			do {
				annotsChanged = false;
				
				//	(re-)sort annotations
				Collections.sort(annotList, annotationOrder);
				
				//	go annotation by annotation ...
				for (int fa = minFirstAnnotIndex; fa < annotList.size(); fa++) {
					ImAnnotation fAnnot = ((ImAnnotation) annotList.get(fa));
					if (DEBUG_CHECK_NESTING) System.out.println("Checking nesting on '" + fAnnot.getType() + "' " + getAnnotationValue(fAnnot));
					
					//	... looking for the next conflicts
					for (int sa = (fa+1); sa < annotList.size(); sa++) {
						ImAnnotation sAnnot = ((ImAnnotation) annotList.get(sa));
						if (DEBUG_CHECK_NESTING) System.out.println(" - comparing to '" + sAnnot.getType() + "' " + getAnnotationValue(sAnnot));
						
						//	this one starts after first annotation ends, we're done
						if (ImUtils.textStreamOrder.compare(fAnnot.getLastWord(), sAnnot.getFirstWord()) < 0) {
							if (DEBUG_CHECK_NESTING) System.out.println(" ==> start after end, we're done");
							break;
						}
						
						//	this one ends before or where first annotation ends, not a nesting problem
						if (ImUtils.textStreamOrder.compare(fAnnot.getLastWord(), sAnnot.getLastWord()) >= 0) {
							if (DEBUG_CHECK_NESTING) System.out.println(" ==> completely inside, we're OK");
							continue;
						}
						
						//	get and perform cleanup operation
						String operation = annotTypeOperations.getProperty((fAnnot.getType() + ">" + sAnnot.getType()), CUT_SECOND_TO_START_OPERATION);
						if (DEBUG_CHECK_NESTING) System.out.println(" ==> conflict, operation is " + operation + " for " + (fAnnot.getType() + ">" + sAnnot.getType()));
						if (CUT_FIRST_TO_START_OPERATION.equals(operation)) {
							fAnnot.setLastWord(sAnnot.getFirstWord().getPreviousWord());
							if (DEBUG_CHECK_NESTING) System.out.println("   - cut to " + getAnnotationValue(fAnnot));
							annotsChanged = true;
							fa = annotList.size(); // start all over, sort order likely broken
							sa = annotList.size();
						}
						else if (CUT_FIRST_TO_END_OPERATION.equals(operation)) {
							fAnnot.setFirstWord(sAnnot.getFirstWord());
							if (DEBUG_CHECK_NESTING) System.out.println("   - cut to " + getAnnotationValue(fAnnot));
							annotsChanged = true;
							fa = annotList.size(); // start all over, sort order likely broken
							sa = annotList.size();
						}
						else if (EXTEND_FIRST_OPERATION.equals(operation)) {
							fAnnot.setLastWord(sAnnot.getLastWord());
							if (DEBUG_CHECK_NESTING) System.out.println("   - extended to " + getAnnotationValue(fAnnot));
							annotsChanged = true;
							fa = annotList.size(); // start all over, sort order likely broken
							sa = annotList.size();
						}
						else if (SPLIT_FIRST_OPERATION.equals(operation)) {
							ImAnnotation splitAnnot = doc.addAnnotation(fAnnot.getFirstWord(), sAnnot.getFirstWord().getPreviousWord(), fAnnot.getType());
							splitAnnot.copyAttributes(fAnnot);
							annotList.add(splitAnnot);
							if (DEBUG_CHECK_NESTING) System.out.println("   - first part of split is " + getAnnotationValue(splitAnnot));
							fAnnot.setFirstWord(sAnnot.getFirstWord());
							if (DEBUG_CHECK_NESTING) System.out.println("   - second part of split is " + getAnnotationValue(fAnnot));
							annotsChanged = true;
							fa = annotList.size(); // start all over, sort order likely broken
							sa = annotList.size();
						}
						else if (REMOVE_FIRST_OPERATION.equals(operation)) {
							doc.removeAnnotation(fAnnot);
							if (DEBUG_CHECK_NESTING) System.out.println("   - removed");
							annotList.remove(fa--);
							annotsChanged = true;
							sa = annotList.size(); // no use testing against this one any further
						}
						else if (CUT_SECOND_TO_START_OPERATION.equals(operation)) {
							sAnnot.setLastWord(fAnnot.getLastWord());
							if (DEBUG_CHECK_NESTING) System.out.println("   - cut to " + getAnnotationValue(sAnnot));
							annotsChanged = true;
							fa = annotList.size(); // start all over, sort order likely broken
							sa = annotList.size();
						}
						else if (CUT_SECOND_TO_END_OPERATION.equals(operation)) {
							sAnnot.setFirstWord(fAnnot.getLastWord().getNextWord());
							if (DEBUG_CHECK_NESTING) System.out.println("   - cut to " + getAnnotationValue(sAnnot));
							annotsChanged = true;
							fa = annotList.size(); // start all over, sort order likely broken
							sa = annotList.size();
						}
						else if (EXTEND_SECOND_OPERATION.equals(operation)) {
							sAnnot.setFirstWord(fAnnot.getFirstWord());
							if (DEBUG_CHECK_NESTING) System.out.println("   - extended to " + getAnnotationValue(sAnnot));
							annotsChanged = true;
							fa = annotList.size(); // start all over, sort order likely broken
							sa = annotList.size();
						}
						else if (SPLIT_SECOND_OPERATION.equals(operation)) {
							ImAnnotation splitAnnot = doc.addAnnotation(sAnnot.getFirstWord(), fAnnot.getLastWord(), sAnnot.getType());
							splitAnnot.copyAttributes(sAnnot);
							annotList.add(splitAnnot);
							if (DEBUG_CHECK_NESTING) System.out.println("   - first part of split is " + getAnnotationValue(splitAnnot));
							sAnnot.setFirstWord(fAnnot.getLastWord().getNextWord());
							if (DEBUG_CHECK_NESTING) System.out.println("   - second part of split is " + getAnnotationValue(sAnnot));
							annotsChanged = true;
							fa = annotList.size(); // start all over, sort order likely broken
							sa = annotList.size();
						}
						else if (REMOVE_SECOND_OPERATION.equals(operation)) {
							doc.removeAnnotation(sAnnot);
							if (DEBUG_CHECK_NESTING) System.out.println("   - removed");
							annotList.remove(sa--);
							annotsChanged = true;
						}
					}
					
					//	no changes thus far, we can start here when we start over
					if (!annotsChanged)
						minFirstAnnotIndex = fa;
				}
			}
			while (annotsChanged);
		}
	}
	
	private static final boolean DEBUG_CHECK_NESTING = false;
	
	private static String getAnnotationValue(ImAnnotation annot) {
		
		//	count out annotation length
		int annotChars = 0;
		for (ImWord imw = annot.getFirstWord(); imw != null; imw = imw.getNextWord()) {
			annotChars += imw.getString().length();
			if (imw == annot.getLastWord())
				break;
		}
		
		//	this one's short enough
		if (annotChars <= 40)
			return ImUtils.getString(annot.getFirstWord(), annot.getLastWord(), true);
		
		//	get end of head
		ImWord headEnd = annot.getFirstWord();
		int headChars = 0;
		for (; headEnd != null; headEnd = headEnd.getNextWord()) {
			headChars += headEnd.getString().length();
			if (headChars >= 20)
				break;
			if (headEnd == annot.getLastWord())
				break;
		}
		
		//	get start of tail
		ImWord tailStart = annot.getLastWord();
		int tailChars = 0;
		for (; tailStart != null; tailStart = tailStart.getPreviousWord()) {
			tailChars += tailStart.getString().length();
			if (tailChars >= 20)
				break;
			if (tailStart == annot.getFirstWord())
				break;
			if (tailStart == headEnd)
				break;
		}
		
		//	met in the middle, use whole string
		if ((headEnd == tailStart) || (headEnd.getNextWord() == tailStart) || (headEnd.getNextWord() == tailStart.getPreviousWord()))
			return ImUtils.getString(annot.getFirstWord(), annot.getLastWord(), true);
		
		//	give head and tail only if annotation too long
		else return (ImUtils.getString(annot.getFirstWord(), headEnd, true) + " ... " + ImUtils.getString(tailStart, annot.getLastWord(), true));
	}
	
	private static final Comparator annotationOrder = new Comparator() {
		public int compare(Object obj1, Object obj2) {
			ImAnnotation annot1 = ((ImAnnotation) obj1);
			ImAnnotation annot2 = ((ImAnnotation) obj2);
			int c = ImUtils.textStreamOrder.compare(annot1.getFirstWord(), annot2.getFirstWord());
			return ((c == 0) ? ImUtils.textStreamOrder.compare(annot2.getLastWord(), annot1.getLastWord()) : c);
		}
	};
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(final ImWord start, final ImWord end, final ImDocumentMarkupPanel idmp) {
		
		//	we only work on individual text streams
		if (!start.getTextStreamId().equals(end.getTextStreamId()))
			return null;
		
		//	collect painted annotations spanning or overlapping whole selection
		ImAnnotation[] allOverlappingAnnots = idmp.document.getAnnotationsOverlapping(start, end);
		LinkedList spanningAnnotList = new LinkedList();
		LinkedList overlappingAnnotList = new LinkedList();
		for (int a = 0; a < allOverlappingAnnots.length; a++) {
			if (!idmp.areAnnotationsPainted(allOverlappingAnnots[a].getType()))
				continue;
			overlappingAnnotList.add(allOverlappingAnnots[a]);
			if (ImUtils.textStreamOrder.compare(start, allOverlappingAnnots[a].getFirstWord()) < 0)
				continue;
			if (ImUtils.textStreamOrder.compare(allOverlappingAnnots[a].getLastWord(), end) < 0)
				continue;
			spanningAnnotList.add(allOverlappingAnnots[a]);
		}
		final ImAnnotation[] spanningAnnots = ((ImAnnotation[]) spanningAnnotList.toArray(new ImAnnotation[spanningAnnotList.size()]));
		final ImAnnotation[] overlappingAnnots = ((ImAnnotation[]) overlappingAnnotList.toArray(new ImAnnotation[overlappingAnnotList.size()]));
		
		//	sort annotations to reflect nesting order
		Arrays.sort(spanningAnnots, annotationOrder);
		Arrays.sort(overlappingAnnots, annotationOrder);
		
		//	index overlapping annotations by type to identify merger groups
		final TreeMap annotMergerGroups = new TreeMap();
		for (int a = 0; a < overlappingAnnots.length; a++) {
			LinkedList annotMergerGroup = ((LinkedList) annotMergerGroups.get(overlappingAnnots[a].getType()));
			if (annotMergerGroup == null) {
				annotMergerGroup = new LinkedList();
				annotMergerGroups.put(overlappingAnnots[a].getType(), annotMergerGroup);
			}
			annotMergerGroup.add(overlappingAnnots[a]);
		}
		for (Iterator atit = annotMergerGroups.keySet().iterator(); atit.hasNext();) {
			LinkedList annotMergerGroup = ((LinkedList) annotMergerGroups.get(atit.next()));
			if (annotMergerGroup.size() < 2)
				atit.remove();
		}
		
		//	get available annotation types
		final String[] annotTypes = idmp.document.getAnnotationTypes();
		final TreeSet paintedAnnotTypes = new TreeSet();
		for (int t = 0; t < annotTypes.length; t++) {
			if (idmp.areAnnotationsPainted(annotTypes[t]))
				paintedAnnotTypes.add(annotTypes[t]);
		}
		
		//	collect annotation types
		final TreeSet createAnnotTypes = new TreeSet();
		createAnnotTypes.addAll(paintedAnnotTypes);
		createAnnotTypes.addAll(this.annotTypes);
		for (int a = 0; a < overlappingAnnots.length; a++)
			createAnnotTypes.remove(overlappingAnnots[a].getType());
		
		//	collect available actions
		LinkedList actions = new LinkedList();
		
		//	create an annotation
		if (createAnnotTypes.isEmpty()) {
			actions.add(new SelectionAction("annotate", "Annotate", "Annotate selected words") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					String annotType = ImUtils.promptForObjectType("Enter Annotation Type", "Enter or select type of annotation to create", annotTypes, null, true);
					if (annotType == null)
						return false;
					ImWord fw = ((ImUtils.textStreamOrder.compare(start, end) < 0) ? start : end);
					ImWord lw = ((ImUtils.textStreamOrder.compare(start, end) < 0) ? end : start);
					idmp.document.addAnnotation(fw, lw, annotType);
					idmp.setAnnotationsPainted(annotType, true);
					return true;
				}
			});
			actions.add(new SelectionAction("annotateAll", "Annotate All", "Annotate all occurrences of selected words") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					String annotType = ImUtils.promptForObjectType("Enter Annotation Type", "Enter or select type of annotation to create", annotTypes, null, true);
					if (annotType == null)
						return false;
					if (annotateAll(idmp.document, start, end, annotType)) {
						idmp.setAnnotationsPainted(annotType, true);
						return true;
					}
					else return false;
				}
			});
		}
		else {
			actions.add(new SelectionAction("annotate", "Annotate", "Annotate selected words") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Annotate");
					JMenuItem mi = new JMenuItem("Annotate ...");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							String annotType = ImUtils.promptForObjectType("Enter Annotation Type", "Enter or select type of annotation to create", annotTypes, null, true);
							if (annotType != null) {
								invoker.beginAtomicAction("Add '" + annotType + "' Annotation");
								ImWord fw = ((ImUtils.textStreamOrder.compare(start, end) < 0) ? start : end);
								ImWord lw = ((ImUtils.textStreamOrder.compare(start, end) < 0) ? end : start);
								idmp.document.addAnnotation(fw, lw, annotType);
								invoker.endAtomicAction();
								invoker.setAnnotationsPainted(annotType, true);
								invoker.validate();
								invoker.repaint();
							}
						}
					});
					pm.add(mi);
					for (Iterator atit = createAnnotTypes.iterator(); atit.hasNext();) {
						final String annotType = ((String) atit.next());
						mi = new JMenuItem("- " + annotType);
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								invoker.beginAtomicAction("Add '" + annotType + "' Annotation");
								ImWord fw = ((ImUtils.textStreamOrder.compare(start, end) < 0) ? start : end);
								ImWord lw = ((ImUtils.textStreamOrder.compare(start, end) < 0) ? end : start);
								ImAnnotation imAnnot = idmp.document.addAnnotation(fw, lw, annotType);
								invoker.endAtomicAction();
								invoker.setAnnotationsPainted(imAnnot.getType(), true);
								invoker.validate();
								invoker.repaint();
							}
						});
						Color annotTypeColor = idmp.getAnnotationColor(annotType);
						if (annotTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(annotTypeColor);
						}
						pm.add(mi);
					}
					return pm;
				}
			});
			actions.add(new SelectionAction("annotateAll", "Annotate All", "Annotate all occurrences of selected words") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Annotate All");
					JMenuItem mi = new JMenuItem("Annotate All ...");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							String annotType = ImUtils.promptForObjectType("Enter Annotation Type", "Enter or select type of annotations to create", annotTypes, null, true);
							if (annotType != null) {
								invoker.beginAtomicAction("Annotate All");
								if (annotateAll(idmp.document, start, end, annotType)) {
									invoker.endAtomicAction();
									invoker.setAnnotationsPainted(annotType, true);
									invoker.validate();
									invoker.repaint();
								}
							}
						}
					});
					pm.add(mi);
					for (Iterator atit = createAnnotTypes.iterator(); atit.hasNext();) {
						final String annotType = ((String) atit.next());
						mi = new JMenuItem("- " + annotType);
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								invoker.beginAtomicAction("Annotate All");
								if (annotateAll(idmp.document, start, end, annotType)) {
									invoker.setAnnotationsPainted(annotType, true);
									invoker.endAtomicAction();
									invoker.validate();
									invoker.repaint();
								}
							}
						});
						Color annotTypeColor = idmp.getAnnotationColor(annotType);
						if (annotTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(annotTypeColor);
						}
						pm.add(mi);
					}
					return pm;
				}
			});
		}
		
		//	single word selection (offer editing word attributes above same for other annotations)
		if (start == end)
			actions.add(new SelectionAction("editAttributesWord", "Edit Word Attributes", ("Edit attributes of '" + start.getString() + "'.")) {
				protected boolean isAtomicAction() {
					return false;
				}
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					idmp.editAttributes(start, start.getType(), start.getString());
					return true;
				}
			});
		
		//	single annotation selected
		if (spanningAnnots.length == 1) {
			
			//	edit attributes of existing annotations
			actions.add(new SelectionAction("editAttributesAnnot", ("Edit " + spanningAnnots[0].getType() + " Attributes"), ("Edit attributes of '" + spanningAnnots[0].getType() + "' annotation.")) {
				protected boolean isAtomicAction() {
					return false;
				}
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					StringBuffer spanningAnnotValue = new StringBuffer();
					for (ImWord imw = spanningAnnots[0].getFirstWord(); imw != null; imw = imw.getNextWord()) {
						if (imw.pageId != spanningAnnots[0].getFirstWord().pageId)
							break;
						spanningAnnotValue.append(imw.toString());
						if (imw == spanningAnnots[0].getLastWord())
							break;
					}
					idmp.editAttributes(spanningAnnots[0], spanningAnnots[0].getType(), spanningAnnotValue.toString());
					return true;
				}
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					JMenuItem mi = super.getMenuItem(invoker);
					Color annotTypeColor = idmp.getAnnotationColor(spanningAnnots[0].getType());
					if (annotTypeColor != null) {
						mi.setOpaque(true);
						mi.setBackground(annotTypeColor);
					}
					return mi;
				}
			});
			
			//	offer copying attributes to other annotation
			if (start == end)
				actions.add(new TwoClickSelectionAction("copyAttributesAnnot", "Copy " + spanningAnnots[0].getType() + " Attributes", "Copy attributes of '" + spanningAnnots[0].getType() + "' annotation.") {
					protected boolean isAtomicAction() {
						return false;
					}
					public ImWord getFirstWord() {
						return start;
					}
					public boolean performAction(ImWord secondWord) {
						return copyAnnotationAttributes(spanningAnnots[0], idmp, secondWord);
					}
					public String getActiveLabel() {
						return ("Copy attributes of " + spanningAnnots[0].getType() + " annotation at '" + start.getString() + "'");
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color annotTypeColor = idmp.getAnnotationColor(spanningAnnots[0].getType());
						if (annotTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(annotTypeColor);
						}
						return mi;
					}
				});
			
			//	remove existing annotation
			actions.add(new SelectionAction("removeAnnot", ("Remove " + spanningAnnots[0].getType() + " Annotation"), ("Remove '" + spanningAnnots[0].getType() + "' annotation.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					idmp.document.removeAnnotation(spanningAnnots[0]);
					return true;
				}
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					JMenuItem mi = super.getMenuItem(invoker);
					Color annotTypeColor = idmp.getAnnotationColor(spanningAnnots[0].getType());
					if (annotTypeColor != null) {
						mi.setOpaque(true);
						mi.setBackground(annotTypeColor);
					}
					return mi;
				}
			});
			
			//	remove all existing annotation with selected value
			actions.add(new SelectionAction("removeAllAnnot", ("Remove All '" + getAnnotationShortValue(start, end) + "' " + spanningAnnots[0].getType() + " Annotations"), ("Remove all '" + spanningAnnots[0].getType() + "' annotations with value '" + getAnnotationShortValue(start, end) + "'.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return removeAll(idmp.document, spanningAnnots[0]);
				}
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					JMenuItem mi = super.getMenuItem(invoker);
					Color annotTypeColor = idmp.getAnnotationColor(spanningAnnots[0].getType());
					if (annotTypeColor != null) {
						mi.setOpaque(true);
						mi.setBackground(annotTypeColor);
					}
					return mi;
				}
			});
			
			//	change type of existing annotation
			actions.add(new SelectionAction("changeTypeAnnot", ("Change Annotation Type"), ("Change type of '" + spanningAnnots[0].getType() + "' annotation.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					String annotType = ImUtils.promptForObjectType("Enter Annotation Type", "Enter or select new annotation type", annotTypes, spanningAnnots[0].getType(), true);
					if (annotType == null)
						return false;
					spanningAnnots[0].setType(annotType);
					idmp.setAnnotationsPainted(annotType, true);
					return true;
				}
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					JMenuItem mi = super.getMenuItem(invoker);
					Color annotTypeColor = idmp.getAnnotationColor(spanningAnnots[0].getType());
					if (annotTypeColor != null) {
						mi.setOpaque(true);
						mi.setBackground(annotTypeColor);
					}
					return mi;
				}
			});
		}
		
		/* multiple annotations selected; offering attribute editing, removal,
		 * and type change only if we have a single word selection or if there
		 * are no merger groups, i.e., if selection purpose is unlikely to be
		 * merging annotations
		 */
		else if ((spanningAnnots.length > 1) && (annotMergerGroups.isEmpty() || (start == end))) {
			
			//	edit attributes of existing annotations
			actions.add(new SelectionAction("editAttributesAnnot", "Edit Annotation Attributes ...", "Edit attributes of selected annotations.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Edit Annotation Attributes ...");
					JMenuItem mi;
					for (int a = 0; a < spanningAnnots.length; a++) {
						final ImAnnotation spanningAnnot = spanningAnnots[a];
						mi = new JMenuItem("- " + spanningAnnot.getType() + " '" + getAnnotationShortValue(spanningAnnot.getFirstWord(), spanningAnnot.getLastWord()) + "'");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								StringBuffer spanningAnnotValue = new StringBuffer();
								for (ImWord imw = spanningAnnot.getFirstWord(); imw != null; imw = imw.getNextWord()) {
									if (imw.pageId != spanningAnnot.getFirstWord().pageId)
										break;
									spanningAnnotValue.append(imw.toString());
									if (imw == spanningAnnot.getLastWord())
										break;
								}
								idmp.editAttributes(spanningAnnot, spanningAnnot.getType(), spanningAnnotValue.toString());
								invoker.validate();
								invoker.repaint();
							}
						});
						Color annotTypeColor = idmp.getAnnotationColor(spanningAnnot.getType());
						if (annotTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(annotTypeColor);
						}
						pm.add(mi);
					}
					return pm;
				}
			});
			
			//	offer copying attributes to other annotation
			if (start == end)
				actions.add(new TwoClickSelectionAction("copyAttributesAnnot", "Copy Annotation Attributes ...", "Copy attributes of selected annotations.") {
					private ImAnnotation sourceAnnot = null;
					protected boolean isAtomicAction() {
						return false;
					}
					public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
						JMenu pm = new JMenu("Copy Annotation Attributes ...");
						JMenuItem mi;
						for (int a = 0; a < spanningAnnots.length; a++) {
							final ImAnnotation spanningAnnot = spanningAnnots[a];
							mi = new JMenuItem("- " + spanningAnnot.getType() + " '" + getAnnotationShortValue(spanningAnnot.getFirstWord(), spanningAnnot.getLastWord()) + "'");
							mi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									sourceAnnot = spanningAnnot;
									performAction(invoker);
								}
							});
							Color annotTypeColor = idmp.getAnnotationColor(spanningAnnot.getType());
							if (annotTypeColor != null) {
								mi.setOpaque(true);
								mi.setBackground(annotTypeColor);
							}
							pm.add(mi);
						}
						return pm;
					}
					public ImWord getFirstWord() {
						return start;
					}
					public boolean performAction(ImWord secondWord) {
						return copyAnnotationAttributes(this.sourceAnnot, idmp, secondWord);
					}
					public String getActiveLabel() {
						return ("Copy attributes of " + this.sourceAnnot.getType() + " annotation at '" + start.getString() + "'");
					}
				});
			
			//	remove existing annotation
			actions.add(new SelectionAction("removeAnnot", "Remove Annotation ...", "Remove selected annotations.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Remove Annotation ...");
					JMenuItem mi;
					for (int a = 0; a < spanningAnnots.length; a++) {
						final ImAnnotation spanningAnnot = spanningAnnots[a];
						mi = new JMenuItem("- " + spanningAnnot.getType() + " '" + getAnnotationShortValue(spanningAnnot.getFirstWord(), spanningAnnot.getLastWord()) + "'");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								invoker.beginAtomicAction("Remove Annotation");
								idmp.document.removeAnnotation(spanningAnnot);
								invoker.endAtomicAction();
								invoker.validate();
								invoker.repaint();
							}
						});
						Color annotTypeColor = idmp.getAnnotationColor(spanningAnnot.getType());
						if (annotTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(annotTypeColor);
						}
						pm.add(mi);
					}
					return pm;
				}
			});
			
			//	remove all existing annotation with selected value
			actions.add(new SelectionAction("removeAllAnnot", "Remove All Annotations ...", "Remove all annotations with selected value.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Remove All Annotations ...");
					JMenuItem mi;
					for (int a = 0; a < spanningAnnots.length; a++) {
						final ImAnnotation selectedAnnot = spanningAnnots[a];
						mi = new JMenuItem("- " + selectedAnnot.getType() + " '" + getAnnotationShortValue(start, end) + "'");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								invoker.beginAtomicAction("Remove All");
								if (removeAll(idmp.document, selectedAnnot)) {
									invoker.endAtomicAction();
									invoker.validate();
									invoker.repaint();
								}
							}
						});
						Color annotTypeColor = idmp.getAnnotationColor(selectedAnnot.getType());
						if (annotTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(annotTypeColor);
						}
						pm.add(mi);
					}
					return pm;
				}
			});
			
			//	change type of existing annotation
			actions.add(new SelectionAction("changeTypeAnnot", "Change Annotation Type ...", "Change the type of selected annotations.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Change Annotation Type ...");
					JMenuItem mi;
					for (int a = 0; a < spanningAnnots.length; a++) {
						final ImAnnotation spanningAnnot = spanningAnnots[a];
						mi = new JMenuItem("- " + spanningAnnot.getType() + " '" + spanningAnnot.getFirstWord().getString() + "'");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								String annotType = ImUtils.promptForObjectType("Enter Annotation Type", "Enter or select new annotation type", annotTypes, spanningAnnot.getType(), true);
								if (annotType != null) {
									invoker.beginAtomicAction("Change Annotation Type");
									spanningAnnot.setType(annotType);
									idmp.setAnnotationsPainted(annotType, true);
									invoker.endAtomicAction();
									invoker.validate();
									invoker.repaint();
								}
							}
						});
						Color annotTypeColor = idmp.getAnnotationColor(spanningAnnot.getType());
						if (annotTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(annotTypeColor);
						}
						pm.add(mi);
					}
					return pm;
				}
			});
		}
		
		//	single word selection
		if (start == end) {
			
			//	start annotation
			actions.add(new TwoClickSelectionAction("annotateTwoClick", "Start Annotation", ("Start annotation from '" + start.getString() + "'")) {
				public boolean performAction(ImWord secondWord) {
					if (!start.getTextStreamId().equals(secondWord.getTextStreamId())) {
						DialogFactory.alert(("Cannot annotate from '" + start.getString() + "' to '" + secondWord.getString() + "', they belong to different text streams:\r\n- '" + start.getString() + "': " + start.getTextStreamId() + ", type '" + start.getTextStreamType() + "'\r\n- '" + secondWord.getString() + "': " + secondWord.getTextStreamId() + ", type '" + secondWord.getTextStreamType() + "'"), "Cannot Annotate Across Text Streams", JOptionPane.ERROR_MESSAGE);
						return false;
					}
					String annotType = ImUtils.promptForObjectType("Enter Annotation Type", "Enter or select type of annotation to create", ((String[]) createAnnotTypes.toArray(new String[createAnnotTypes.size()])), null, true);
					if (annotType == null)
						return false;
					ImWord fw = ((ImUtils.textStreamOrder.compare(start, secondWord) < 0) ? start : secondWord);
					ImWord lw = ((ImUtils.textStreamOrder.compare(start, secondWord) < 0) ? secondWord : start);
					ImAnnotation imAnnot = idmp.document.addAnnotation(fw, lw, annotType.toString().trim());
					idmp.setAnnotationsPainted(imAnnot.getType(), true);
					return true;
				}
				public ImWord getFirstWord() {
					return start;
				}
				public String getActiveLabel() {
					return ("Add annotation starting from '" + start.getString() + "'");
				}
			});
			
			//	split annotation before selected word
			for (int a = 0; a < spanningAnnots.length; a++) {
				if ((spanningAnnots[a].getFirstWord() == start) || (start.getPreviousWord() == null))
					continue;
				final ImAnnotation spanningAnnot = spanningAnnots[a];
				actions.add(new SelectionAction("splitAnnotBefore", ("Split " + spanningAnnot.getType() + " Before"), ("Split the '" + spanningAnnot.getType() + "' anotation before '" + start.getString() + "'")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						ImAnnotation imAnnot = idmp.document.addAnnotation(start, spanningAnnot.getLastWord(), spanningAnnot.getType());
						imAnnot.copyAttributes(spanningAnnot);
						spanningAnnot.setLastWord(start.getPreviousWord());
						idmp.document.cleanupAnnotations();
						return true;
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color annotTypeColor = idmp.getAnnotationColor(spanningAnnot.getType());
						if (annotTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(annotTypeColor);
						}
						return mi;
					}
				});
			}
			
			//	split annotation after selected word
			for (int a = 0; a < spanningAnnots.length; a++) {
				if ((spanningAnnots[a].getLastWord() == start) || (start.getNextWord() == null))
					continue;
				final ImAnnotation spanningAnnot = spanningAnnots[a];
				actions.add(new SelectionAction("splitAnnotAfter", ("Split " + spanningAnnot.getType() + " After"), ("Split the '" + spanningAnnot.getType() + "' anotation after '" + start.getString() + "'")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						ImAnnotation imAnnot = idmp.document.addAnnotation(start.getNextWord(), spanningAnnot.getLastWord(), spanningAnnot.getType());
						imAnnot.copyAttributes(spanningAnnot);
						spanningAnnot.setLastWord(start);
						idmp.document.cleanupAnnotations();
						return true;
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color annotTypeColor = idmp.getAnnotationColor(spanningAnnot.getType());
						if (annotTypeColor != null)
							mi.setBackground(annotTypeColor);
						return mi;
					}
				});
			}
		}
		
		//	merge annotations (can also occur with single word selection if there are overlapping annotations)
		for (Iterator atit = annotMergerGroups.keySet().iterator(); atit.hasNext();) {
			final String type = ((String) atit.next());
			final LinkedList annotMergerGroup = ((LinkedList) annotMergerGroups.get(type));
			actions.add(new SelectionAction("mergeAnnots", ("Merge " + type + "s"), ("Merge selected '" + type + "' annotations")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					ImAnnotation mergedAnnot = ((ImAnnotation) annotMergerGroup.removeFirst());
					ImWord mergedLastWord = mergedAnnot.getLastWord();
					for (Iterator ait = annotMergerGroup.iterator(); ait.hasNext();) {
						ImAnnotation annot = ((ImAnnotation) ait.next());
						ImWord lastWord = annot.getLastWord();
						if (ImUtils.textStreamOrder.compare(mergedLastWord, lastWord) < 0)
							mergedLastWord = lastWord;
						AttributeUtils.copyAttributes(annot, mergedAnnot, AttributeUtils.ADD_ATTRIBUTE_COPY_MODE);
						idmp.document.removeAnnotation(annot);
					}
					if (mergedLastWord != mergedAnnot.getLastWord())
						mergedAnnot.setLastWord(mergedLastWord);
					idmp.document.cleanupAnnotations();
					return true;
				}
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					JMenuItem mi = super.getMenuItem(invoker);
					Color annotTypeColor = idmp.getAnnotationColor(type);
					if (annotTypeColor != null) {
						mi.setOpaque(true);
						mi.setBackground(annotTypeColor);
					}
					return mi;
				}
			});
		}
		
		//	offer extending overlapping annotations that are not in a merger group
		for (int a = 0; a < overlappingAnnots.length; a++) {
			if (annotMergerGroups.containsKey(overlappingAnnots[a].getType()))
				continue;
			final boolean extendStart = (ImUtils.textStreamOrder.compare(start, overlappingAnnots[a].getFirstWord()) < 0);
			final boolean extendEnd = (ImUtils.textStreamOrder.compare(overlappingAnnots[a].getLastWord(), end) < 0);
			if (!extendStart && !extendEnd)
				continue;
			final ImAnnotation overlappingAnnot = overlappingAnnots[a];
			actions.add(new SelectionAction("extendAnnot", ("Extend " + overlappingAnnot.getType()), ("Extend selected '" + overlappingAnnot.getType() + "' annotation to " + (extendStart ? ("'" + start.getString() + "'") : "") + (extendEnd ? ((extendStart ? " and " : "") + ("'" + end.getString() + "'")) : ""))) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					if (extendStart)
						overlappingAnnot.setFirstWord(start);
					if (extendEnd)
						overlappingAnnot.setLastWord(end);
					idmp.document.cleanupAnnotations();
					return true;
				}
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					JMenuItem mi = super.getMenuItem(invoker);
					Color annotTypeColor = idmp.getAnnotationColor(overlappingAnnot.getType());
					if (annotTypeColor != null) {
						mi.setOpaque(true);
						mi.setBackground(annotTypeColor);
					}
					return mi;
				}
			});
		}
		
		//	copy spanning annotations (a) as plain text and (b) as XML
		if (spanningAnnots.length == 1) {
			actions.add(new SelectionAction("copyAnnotTXT", ("Copy " + spanningAnnots[0].getType() + " Text"), ("Copy the text annotated as '" + spanningAnnots[0].getType() + "' to the system clipboard")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					ImUtils.copy(new StringSelection(ImUtils.getString(spanningAnnots[0].getFirstWord(), spanningAnnots[0].getLastWord(), true)));
					return false;
				}
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					JMenuItem mi = super.getMenuItem(invoker);
					Color annotTypeColor = idmp.getAnnotationColor(spanningAnnots[0].getType());
					if (annotTypeColor != null) {
						mi.setOpaque(true);
						mi.setBackground(annotTypeColor);
					}
					return mi;
				}
			});
			actions.add(new SelectionAction("copyAnnotXML", ("Copy " + spanningAnnots[0].getType() + " XML"), ("Copy the '" + spanningAnnots[0].getType() + "' annotation to the system clipboard as XML")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					copyAnnotationXML(spanningAnnots[0]);
					return false;
				}
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					JMenuItem mi = super.getMenuItem(invoker);
					Color annotTypeColor = idmp.getAnnotationColor(spanningAnnots[0].getType());
					if (annotTypeColor != null) {
						mi.setOpaque(true);
						mi.setBackground(annotTypeColor);
					}
					return mi;
				}
			});
		}
		else if (spanningAnnots.length > 1) {
			actions.add(new SelectionAction("copyAnnotTXT", "Copy Annotation Text", "Copy the text of annnotations to the system clipboard") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Copy Annotation Text ...");
					JMenuItem mi;
					for (int a = 0; a < spanningAnnots.length; a++) {
						final ImAnnotation spanningAnnot = spanningAnnots[a];
						mi = new JMenuItem("- " + spanningAnnot.getType() + " '" + spanningAnnot.getFirstWord().getString() + "'");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								ImUtils.copy(new StringSelection(ImUtils.getString(spanningAnnot.getFirstWord(), spanningAnnot.getLastWord(), true)));
							}
						});
						Color annotTypeColor = idmp.getAnnotationColor(spanningAnnot.getType());
						if (annotTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(annotTypeColor);
						}
						pm.add(mi);
					}
					return pm;
				}
			});
			actions.add(new SelectionAction("copyAnnotXML", "Copy Annotation XML", "Copy annnotations to the system clipboard as XML") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Copy Annotation XML ...");
					JMenuItem mi;
					for (int a = 0; a < spanningAnnots.length; a++) {
						final ImAnnotation spanningAnnot = spanningAnnots[a];
						mi = new JMenuItem("- " + spanningAnnot.getType() + " '" + spanningAnnot.getFirstWord().getString() + "'");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								copyAnnotationXML(spanningAnnot);
							}
						});
						Color annotTypeColor = idmp.getAnnotationColor(spanningAnnot.getType());
						if (annotTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(annotTypeColor);
						}
						pm.add(mi);
					}
					return pm;
				}
			});
		}
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	private boolean copyAnnotationAttributes(ImAnnotation sourceAnnot, ImDocumentMarkupPanel idmp, ImWord secondWord) {
		
		//	anything to copy from?
		if (sourceAnnot == null)
			return false;
		
		//	collect painted annotations spanning or overlapping second word
		ImAnnotation[] allSpanningAnnots = idmp.document.getAnnotationsOverlapping(secondWord);
		LinkedList spanningAnnotList = new LinkedList();
		for (int a = 0; a < allSpanningAnnots.length; a++) {
			if (!idmp.areAnnotationsPainted(allSpanningAnnots[a].getType()))
				continue;
			if (ImUtils.textStreamOrder.compare(secondWord, allSpanningAnnots[a].getFirstWord()) < 0)
				continue;
			if (ImUtils.textStreamOrder.compare(allSpanningAnnots[a].getLastWord(), secondWord) < 0)
				continue;
			spanningAnnotList.add(allSpanningAnnots[a]);
		}
		ImAnnotation[] spanningAnnots = ((ImAnnotation[]) spanningAnnotList.toArray(new ImAnnotation[spanningAnnotList.size()]));
		if (spanningAnnots.length == 0)
			return false;
		
		//	find target annotation
		ImAnnotation targetAnnot = null;
		
		//	select target via exact match to source annotation type
		if (targetAnnot == null) {
			for (int a = 0; a < spanningAnnots.length; a++)
				if (spanningAnnots[a].getType().equals(sourceAnnot.getType())) {
					targetAnnot = spanningAnnots[a];
					break;
				}
		}
		
		//	select target via prefix match to source annotation type
		if (targetAnnot == null) {
			for (int a = 0; a < spanningAnnots.length; a++)
				if (spanningAnnots[a].getType().startsWith(sourceAnnot.getType())) {
					targetAnnot = spanningAnnots[a];
					break;
				}
		}
		if (targetAnnot == null) {
			for (int a = 0; a < spanningAnnots.length; a++)
				if (sourceAnnot.getType().startsWith(spanningAnnots[a].getType())) {
					targetAnnot = spanningAnnots[a];
					break;
				}
		}
		
		//	select only spanning annot as target
		if ((targetAnnot == null) && (spanningAnnots.length == 1))
			targetAnnot = spanningAnnots[0];
		
		//	anything to work with?
		if (targetAnnot == null)
			return false;
		
		//	prepare target attribute editing
		StringBuffer targetAnnotValue = new StringBuffer();
		for (ImWord imw = targetAnnot.getFirstWord(); imw != null; imw = imw.getNextWord()) {
			if (imw.pageId != targetAnnot.getFirstWord().pageId)
				break;
			targetAnnotValue.append(imw.toString());
			if (imw == targetAnnot.getLastWord())
				break;
		}
		
		//	copy attributes (using 'add' mode by default) and open them for editing
		try {
			idmp.beginAtomicAction("Copy " + sourceAnnot.getType() + " Attributes");
			AttributeUtils.copyAttributes(sourceAnnot, targetAnnot, AttributeUtils.ADD_ATTRIBUTE_COPY_MODE);
			idmp.editAttributes(targetAnnot, targetAnnot.getType(), targetAnnotValue.toString());
		}
		finally {
			idmp.endAtomicAction();
		}
		
		//	indicate changes
		return true;
	}
	
	private static String getAnnotationShortValue(ImWord start, ImWord end) {
		if (start == end)
			return start.getString();
		else if (start.getNextWord() == end)
			return (start.getString() + (Gamta.insertSpace(start.getString(), end.getString()) ? " " : "") + end.getString());
		else return (start.getString() + " ... " + end.getString());
	}
	
	private void copyAnnotationXML(final ImAnnotation annot) {
		
		//	wrap annotation
		ImDocumentRoot wrappedAnnot = new ImDocumentRoot(annot, ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS);
		
		//	write annotation as XML (filter out implicit paragraphs, though) TODO do we really need this filter ???
		StringWriter annotData = new StringWriter();
		try {
			AnnotationUtils.writeXML(wrappedAnnot, annotData, true, new HashSet() {
				public boolean contains(Object obj) {
					return (MutableAnnotation.PARAGRAPH_TYPE.equals(annot.getType()) || !MutableAnnotation.PARAGRAPH_TYPE.equals(obj));
				}
			});
		} catch (IOException ioe) {}
		
		//	put data in clipboard
		ImUtils.copy(new StringSelection(annotData.toString()));
	}
	
	private boolean annotateAll(ImDocument doc, ImWord start, ImWord end, String annotType) {
		
		//	annotate selected occurrence
		ImWord fw = ((ImUtils.textStreamOrder.compare(start, end) < 0) ? start : end);
		ImWord lw = ((ImUtils.textStreamOrder.compare(start, end) < 0) ? end : start);
		ImAnnotation annot = doc.addAnnotation(fw, lw, annotType);
		if (annot == null)
			return false;
		
		//	wrap document (we can normalize all the way, as this is fastest, and annotations refer to a single text stream anyway)
		ImDocumentRoot wrapperDoc = new ImDocumentRoot(doc, ImTokenSequence.NORMALIZATION_LEVEL_STREAMS);
		
		//	extract all occurrences of selected text
		StringVector dictionary = new StringVector();
		dictionary.addElement(TokenSequenceUtils.concatTokens(new ImTokenSequence(annot.getFirstWord(), annot.getLastWord()), true, true));
		Annotation[] annots = Gamta.extractAllContained(wrapperDoc, dictionary);
		
		//	add annotations (we can simply add them all, as ImDocument prevents duplicates internally)
		for (int a = 0; a < annots.length; a++)
			wrapperDoc.addAnnotation(annotType, annots[a].getStartIndex(), annots[a].size());
		
		//	we added at least one annotation ...
		return true;
	}
	
	private boolean removeAll(ImDocument doc, ImAnnotation annot) {
		
		//	get all annotations of selected type
		ImAnnotation[] annots = doc.getAnnotations(annot.getType());
		
		//	get annotation value
		String removeValue = TokenSequenceUtils.concatTokens(new ImTokenSequence(annot.getFirstWord(), annot.getLastWord()), true, true);
		
		//	remove annotations
		boolean removed = false;
		for (int a = 0; a < annots.length; a++) {
			String annotValue = TokenSequenceUtils.concatTokens(new ImTokenSequence(annots[a].getFirstWord(), annots[a].getLastWord()), true, true);
			if (annotValue.equals(removeValue)) {
				doc.removeAnnotation(annots[a]);
				removed = true;
			}
		}
		
		//	report any changes
		return removed;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, ImPage page, final ImDocumentMarkupPanel idmp) {
		
		//	mark selection
		BoundingBox selectedBox = new BoundingBox(Math.min(start.x, end.x), Math.max(start.x, end.x), Math.min(start.y, end.y), Math.max(start.y, end.y));
		
		//	get selected words
		final ImWord[] selectedWords = page.getWordsInside(selectedBox);
		if (selectedWords.length == 0)
			return null;
		
		//	order words
		Arrays.sort(selectedWords, ImUtils.textStreamOrder);
		
		//	check if we have a single continuous stream
		boolean singleContinuousStream = true;
		for (int w = 1; w < selectedWords.length; w++)
			if (selectedWords[w].getPreviousWord() != selectedWords[w-1]) {
				singleContinuousStream = false;
				break;
			}
		
		//	if we have a single continuous selection, we can handle it like a word selection
		return (singleContinuousStream ? this.getActions(selectedWords[0], selectedWords[selectedWords.length - 1], idmp) : null);
	}
}
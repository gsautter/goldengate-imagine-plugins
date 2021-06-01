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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed;
import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImDocument.ImDocumentListener;
import de.uka.ipd.idaho.im.ImFont;
import de.uka.ipd.idaho.im.ImLayoutObject.LayoutObjectUuidHelper;
import de.uka.ipd.idaho.im.ImObject;
import de.uka.ipd.idaho.im.ImObject.UuidHelper;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImSupplement;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.imagine.GoldenGateImagine;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider;
import de.uka.ipd.idaho.im.imagine.swing.ImageDocumentMarkupDialog;
import de.uka.ipd.idaho.im.imagine.swing.ImageDocumentMarkupUI.ImageDocumentEditorTab;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.AtomicActionListener;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImObjectTransformer;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * Plugin providing editing facilities for pages or parts thereof in dedicated
 * sub dialog, optionally rotating document content 90° clockwise or counter
 * clockwise to help with situations where flipped page content could not be
 * transformed back to an upright position during document creation, be it for
 * lack of space on the page or for lack of text direction indicators.
 * 
 * @author sautter
 */
public class DocumentPartEditorProvider extends AbstractSelectionActionProvider {
	/* TODO Use new sub document editing and write-through capability to offer GGE like "Edit" option in ImObject list views:
- open spanned page(s) for annotations (excluding words):
  - omit text streams unrelated to annotation
  - maybe also omit paragraphs (or at least blocks) not overlapping with annotation
  - in case of multiple spanned pages, use dedicated transformer for each page ...
  - ... wrapped in custom multiplexing transformer to dispatch between page specific ones
- open parent page for regions (including words):
  - should work pretty much like implemented now
  - maybe omit content outside selected region (akin to editing block in sub document) ...
  - ... but do include wiggling room margin outside selected region (as currently drafted in code)
- in any case, use most likely rotation angle right on click
- move dialog and write-through implementation to GGI proper ...
- ... or provide document part editor interface in GGI proper ...
- ... to implement in current gizmo and inject (or ingest) via GGI core in object list provider
	 */
	
	/** default zero-argument constructor for class loading */
	public DocumentPartEditorProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "IM Editor Dialog Provider";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, final ImPage page, final ImDocumentMarkupPanel idmp) {
		
		//	set up selection bounds
		final BoundingBox selectedBounds = new BoundingBox(
				Math.min(start.x, end.x),
				Math.max(start.x, end.x),
				Math.min(start.y, end.y),
				Math.max(start.y, end.y)
			);
		
		//	get visible regions involved with selection
		final ArrayList selectedRegions = new ArrayList();
		CountingSet selectedRegionTypes = new CountingSet();
		final ArrayList contextRegions = new ArrayList();
		ArrayList overlappingRegions = new ArrayList();
		ImRegion[] regions = page.getRegions();
		for (int r = 0; r < regions.length; r++) {
			if (!regions[r].bounds.overlaps(selectedBounds))
				continue;
			if (!idmp.areRegionsPainted(regions[r].getType()))
				continue;
			if (regions[r].bounds.liesIn(selectedBounds, false)) {
				selectedRegions.add(regions[r]);
				selectedRegionTypes.add(regions[r].getType());
			}
			else if (regions[r].bounds.includes(selectedBounds, false))
				contextRegions.add(regions[r]);
			else overlappingRegions.add(regions[r]);
		}
		
		//	discard regions presumably not targeted specifically (at least not for us)
		ImRegion largestSelectedRegion = null;
		boolean selectedRegionsNested = false;
		for (int r = 0; r < selectedRegions.size(); r++) {
			ImRegion selectedRegion = ((ImRegion) selectedRegions.get(r));
			if (selectedRegionTypes.getCount(selectedRegion.getType()) > 1)
				selectedRegions.remove(r--);
			else if (largestSelectedRegion == null) {
				largestSelectedRegion = selectedRegion;
				selectedRegionsNested = true;
			}
			else if (!largestSelectedRegion.bounds.includes(selectedRegion.bounds, false))
				selectedRegionsNested = false;
		}
		
		//	get selected words for context
		ImWord[] selWords = page.getWordsInside(selectedBounds);
		
		//	collect available actions
		LinkedList actions = new LinkedList();
		
		//	no words selected (page margin click or figure ???)
		if (selWords.length == 0) {
			
			//	offer editing page if nothing selected at all
			if (selectedRegions.isEmpty() && contextRegions.isEmpty() && overlappingRegions.isEmpty())
				actions.add(new SelectionAction("showImView", "Edit Page in Sub Window", "Open page in sub window, potentially rotated to upright orientation.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return editBoxSelection(page, "page", page.bounds, false, idmp);
					}
				});
			
			//	only context regions available
			else if (selectedRegions.isEmpty() && overlappingRegions.isEmpty()) {
				if (contextRegions.size() == 1) {
					final ImRegion contextRegion = ((ImRegion) contextRegions.get(0));
					actions.add(new SelectionAction("showImView", ("Edit '" + contextRegion.getType() + "' in Sub Window"), ("Open '" + contextRegion.getType() + "' in sub window, potentially rotated to upright orientation.")) {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return editBoxSelection(page, contextRegion.getType(), contextRegion.bounds, true, idmp);
						}
						public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
							JMenuItem mi = super.getMenuItem(invoker);
							Color regionTypeColor = idmp.getLayoutObjectColor(contextRegion.getType());
							if ((mi != null) && (regionTypeColor != null)) {
								mi.setOpaque(true);
								mi.setBackground(regionTypeColor);
							}
							return mi;
						}
					});
				}
				else {
					actions.add(new SelectionAction("showImView", "Edit in Sub Window ...", "Open selected region in sub window, potentially rotated to upright orientation.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return false;
						}
						public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
							JMenu pm = new JMenu("Edit in Sub Window ...");
							JMenuItem mi;
							for (int r = 0; r < contextRegions.size(); r++) {
								final ImRegion contextRegion = ((ImRegion) contextRegions.get(r));
								mi = new JMenuItem("- " + contextRegion.getType() + " " + contextRegion.bounds.toString());
								mi.addActionListener(new ActionListener() {
									public void actionPerformed(ActionEvent ae) {
										try {
											idmp.beginAtomicAction("Edit '" + contextRegion.getType() + "' in Sub Window");
											if (editBoxSelection(page, contextRegion.getType(), contextRegion.bounds, true, idmp)) {
												invoker.validate();
												invoker.repaint();
											}
										}
										finally {
											idmp.endAtomicAction();
										}
									}
								});
								Color regionTypeColor = idmp.getLayoutObjectColor(contextRegion.getType());
								if (regionTypeColor != null) {
									mi.setOpaque(true);
									mi.setBackground(regionTypeColor);
								}
								pm.add(mi);
							}
							return pm;
						}
					});
				}
			}
			
			//	coherent selection of regions (only offer outmost one)
			else if (contextRegions.isEmpty() && overlappingRegions.isEmpty() && selectedRegionsNested) {
				final ImRegion selectedRegion = ((ImRegion) selectedRegions.get(0));
				actions.add(new SelectionAction("showImView", ("Edit '" + selectedRegion.getType() + "' in Sub Window"), ("Open '" + selectedRegion.getType() + "' in sub window, potentially rotated to upright orientation.")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return editBoxSelection(page, selectedRegion.getType(), selectedRegion.bounds, true, idmp);
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(selectedRegion.getType());
						if ((mi != null) && (regionTypeColor != null)) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
			}
			
			//	offer selection proper in all other cases
			else actions.add(new SelectionAction("showImView", "Edit Selection in Sub Window", "Open selection in sub window, potentially rotated to upright orientation.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return editBoxSelection(page, "selection", selectedBounds, false, idmp);
				}
			});
		}
		
		//	words selected inside coherent selection of regions (only offer outmost one)
		else if (contextRegions.isEmpty() && overlappingRegions.isEmpty() && selectedRegionsNested) {
			final ImRegion selectedRegion = ((ImRegion) selectedRegions.get(0));
			actions.add(new SelectionAction("showImView", ("Edit '" + selectedRegion.getType() + "' in Sub Window"), ("Open '" + selectedRegion.getType() + "' in sub window, potentially rotated to upright orientation.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return editBoxSelection(page, selectedRegion.getType(), selectedRegion.bounds, true, idmp);
				}
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					JMenuItem mi = super.getMenuItem(invoker);
					Color regionTypeColor = idmp.getLayoutObjectColor(selectedRegion.getType());
					if ((mi != null) && (regionTypeColor != null)) {
						mi.setOpaque(true);
						mi.setBackground(regionTypeColor);
					}
					return mi;
				}
			});
		}
		
		//	offer selection proper in all other cases
		else actions.add(new SelectionAction("showImView", "Edit Selection in Sub Window", "Open selection in sub window, potentially rotated to upright orientation.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				return editBoxSelection(page, "selection", selectedBounds, false, idmp);
			}
		});
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	private static class SelectableRotation {
		final int turnDegrees;
		final String label;
		SelectableRotation(int turnDegrees, String label) {
			this.turnDegrees = turnDegrees;
			this.label = label;
		}
		public String toString() {
			return this.label;
		}
	}
	private static final SelectableRotation rotateCounterClockwise = new SelectableRotation(ImObjectTransformer.COUNTER_CLOCKWISE_ROTATION, "Rotate 90° counter-clockwise");
	private static final SelectableRotation doNotRotate = new SelectableRotation(ImObjectTransformer.NO_ROTATION, "Do not rotate");
	private static final SelectableRotation rotateClockwise = new SelectableRotation(ImObjectTransformer.CLOCKWISE_ROTATION, "Rotate 90° clockwise");
	private static final SelectableRotation[] rotations = {
		rotateCounterClockwise,
		doNotRotate,
		rotateClockwise
	};
	
	boolean editBoxSelection(ImPage page, String label, BoundingBox bounds, boolean isRegionBounds, ImDocumentMarkupPanel idmp) {
		
		//	pre-select most logical rotation angle from content orientation
		ImWord[] words = page.getWordsInside(bounds);
		int buCount = 0;
		int lrCount = 0;
		int tdCount = 0;
		for (int w = 0; w < words.length; w++) {
			String wordDirection = ((String) words[w].getAttribute(ImWord.TEXT_DIRECTION_ATTRIBUTE, ImWord.TEXT_DIRECTION_LEFT_RIGHT));
			if (ImWord.TEXT_DIRECTION_BOTTOM_UP.equals(wordDirection))
				buCount++;
			else if (ImWord.TEXT_DIRECTION_LEFT_RIGHT.equals(wordDirection))
				lrCount++;
			else if (ImWord.TEXT_DIRECTION_TOP_DOWN.equals(wordDirection))
				tdCount++;
		}
		SelectableRotation selectedRotation = doNotRotate;
		if (Math.max(lrCount, tdCount) < buCount)
			selectedRotation = rotateClockwise;
		else if (Math.max(lrCount, buCount) < tdCount)
			selectedRotation = rotateCounterClockwise;
		
		//	prompt for rotation angle
		Object choice = JOptionPane.showInputDialog(DialogFactory.getTopWindow(), ("Select how to rotate " + label + " for editing"), "Select Rotation", JOptionPane.PLAIN_MESSAGE, null, rotations, selectedRotation);
		if (choice instanceof SelectableRotation)
			return this.editBoxSelection(page, label, bounds, isRegionBounds, ((SelectableRotation) choice).turnDegrees, idmp);
		else return false;
	}
	
	boolean editBoxSelection(ImPage page, String label, BoundingBox bounds, boolean isRegionBounds, int turnDegrees, ImDocumentMarkupPanel idmp) {
		
		//	create document
		ImSubDocument subDoc = createSubDocument(page, bounds, isRegionBounds, turnDegrees);
		subDoc.copyAttributes(idmp.document);
		System.out.println("Sub document created");
		
		//	find main window (we don't want to size after splash screen ...)
		Window topWindow = DialogPanel.getTopWindow();
		while (topWindow != null) {
			if (topWindow instanceof JFrame)
				break;
			Window topWindowOwner = topWindow.getOwner();
			if (topWindowOwner == null)
				break;
			topWindow = topWindowOwner;
		}
		System.out.println("Got top window");
		
		//	create and configure dialog TODOne anything else ??? doesn't seem so
		ImageDocumentPartMarkupDialog idpmd = new ImageDocumentPartMarkupDialog(this.imagineParent, subDoc, ("Edit " + StringUtils.capitalize(label)), idmp.document, page);
		ImageDocumentEditorTab subIdet = idpmd.getMarkupUi().getActiveDocument();
		ImDocumentMarkupPanel subIdmp = subIdet.getMarkupPanel();
		System.out.println("Edit dialog");
		String[] annotTypes = idmp.document.getAnnotationTypes();
		for (int t = 0; t < annotTypes.length; t++) {
			subIdmp.setAnnotationColor(annotTypes[t], idmp.getAnnotationColor(annotTypes[t]));
			if (idmp.areAnnotationsPainted(annotTypes[t]))
				subIdmp.setAnnotationsPainted(annotTypes[t], true);
		}
		System.out.println("Annotation settings transferred");
		String[] objectTypes = idmp.getLayoutObjectTypes();
		for (int t = 0; t < objectTypes.length; t++) {
			subIdmp.setLayoutObjectColor(objectTypes[t], idmp.getLayoutObjectColor(objectTypes[t]));
			if (idmp.areRegionsPainted(objectTypes[t]))
				subIdmp.setRegionsPainted(objectTypes[t], true);
		}
		System.out.println("Region settings transferred");
		String[] textStreamTypes = idmp.getTextStreamTypes();
		for (int t = 0; t < textStreamTypes.length; t++)
			subIdmp.setTextStreamTypeColor(textStreamTypes[t], idmp.getTextStreamTypeColor(textStreamTypes[t]));
		if (idmp.areTextStreamsPainted())
			subIdmp.setTextStreamsPainted(true);
		System.out.println("Text stream settings transferred");
		if (!idmp.documentBornDigital)
			subIdmp.setTextStringPercentage(idmp.getTextStringPercentage());
		subIdet.setRenderingDpi(idmp.getRenderingDpi()); // need to do this on editor tab to also adjust view control
		System.out.println("Display settings transferred");
		
		//	open dialog
		idpmd.setSize(topWindow.getSize());
		idpmd.setLocationRelativeTo(topWindow);
		System.out.println("Dialog configured");
		idpmd.setVisible(true);
		
		//	indicate changes if any happen
		return idpmd.writeChangesIfCommitted(idmp);
	}
	
	private static class ImageDocumentPartMarkupDialog extends ImageDocumentMarkupDialog implements ImDocumentListener, AtomicActionListener, ImageMarkupTool {
		ImSubDocument subDoc;
		ImObjectTransformer revTransformer;
		ImDocument doc;
		ImPage page;
		boolean canceled = false;
		ImageDocumentPartMarkupDialog(GoldenGateImagine ggImagine, ImSubDocument subDoc, String docName, ImDocument doc, ImPage page) {
			super(ggImagine, ggImagine.getConfiguration().getSettings(), subDoc, docName, true);
			this.subDoc = subDoc;
			this.doc = doc;
			this.page = page;
			
			//	set up write-through mapping of edits
			this.revTransformer = this.subDoc.transformer.invert();
			
			//	add attribute transformers (required for supplements)
			this.revTransformer.addAttributeTransformer(ImObjectTransformer.PAGE_ID_TRANSFORMER);
			this.revTransformer.addAttributeTransformer(ImObjectTransformer.BOUNDING_BOX_TRANSFORMER);
			
			//	start recording edits
			this.getMarkupUi().getActiveDocument().getMarkupPanel().addAtomicActionListener(this);
			this.subDoc.addDocumentListener(this);
			
			//	add OK and Cancel buttons
			JButton ok = new JButton("OK");
			ok.setBorder(BorderFactory.createRaisedBevelBorder());
			ok.setPreferredSize(new Dimension(80, 21));
			ok.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					dispose();
				}
			});
			JButton cancel = new JButton("Cancel");
			cancel.setBorder(BorderFactory.createRaisedBevelBorder());
			cancel.setPreferredSize(new Dimension(80, 21));
			cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					canceled = true;
					dispose();
				}
			});
			JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
			buttons.add(ok);
			buttons.add(cancel);
			this.add(buttons, BorderLayout.SOUTH);
		}
		protected boolean saveDocument(ImageDocumentEditorTab idet) {
			return false;
		}
		
		boolean writeChangesIfCommitted(ImDocumentMarkupPanel idmp) {
			if (this.canceled)
				return false;
			if (this.writeThroughActions.isEmpty())
				return false;
			idmp.applyMarkupTool(this, null); // need to inject ourselves as IMT so reaction providers don't interfere with write-through
			return true;
		}
		public String getLabel() {
			return this.getTitle();
		}
		public String getTooltip() {
			return null;
		}
		public String getHelpText() {
			return null;
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			pm.setBaseProgress(0);
			pm.setProgress(0);
			pm.setMaxProgress(90);
			pm.setStep("Replaying edits");
			for (int a = 0; a < this.writeThroughActions.size(); a++) {
				WriteThroughAction wta = ((WriteThroughAction) this.writeThroughActions.get(a));
				pm.setProgress((a * 100) / this.writeThroughActions.size());
				if (wta instanceof MultipartWriteThroughAction)
					pm.setInfo(((MultipartWriteThroughAction) wta).label);
				wta.execute();
			}
			
			pm.setBaseProgress(90);
			pm.setProgress(0);
			pm.setMaxProgress(100);
			pm.setStep("Connecting inbound and outbound text streams");
			ExternalImWordProxy[] eimwps = this.subDoc.getExternalImWordProxies();
			for (int p = 0; p < eimwps.length; p++) {
				pm.setProgress((p * 100) / eimwps.length);
				if (eimwps[p] instanceof ExternalPredecessorProxy) {
					if (eimwps[p].getNextWord() instanceof ExternalSuccessorProxy)
						eimwps[p].externalImWord.setNextWord(((ExternalImWordProxy) eimwps[p].getNextWord()).externalImWord);
					else if (eimwps[p].getNextWord() == null)
						eimwps[p].externalImWord.setNextWord(null);
				}
				if (eimwps[p] instanceof ExternalSuccessorProxy) {
					if (eimwps[p].getPreviousWord() instanceof ExternalPredecessorProxy)
						eimwps[p].externalImWord.setPreviousWord(((ExternalImWordProxy) eimwps[p].getPreviousWord()).externalImWord);
					else if (eimwps[p].getPreviousWord() == null)
						eimwps[p].externalImWord.setPreviousWord(null);
				}
			}
			
			pm.setProgress(100);
		}
		
		private ArrayList writeThroughActions = new ArrayList();
		private CountingSet writeThroughActionIDs = new CountingSet(new HashMap());
		private abstract class WriteThroughAction {
			final long actionId;
			WriteThroughAction(long actionId) {
				this.actionId = actionId;
			}
			abstract void execute();
		}
		private class MultipartWriteThroughAction extends WriteThroughAction {
			final ArrayList parts = new ArrayList();
			final String label;
			MultipartWriteThroughAction(long actionId, String label) {
				super(actionId);
				this.label = label;
			}
			synchronized void addWriteThroughAction(WriteThroughAction wta) {
				this.parts.add(wta);
			}
			void execute() {
				for (int p = 0; p < this.parts.size(); p++)
					((WriteThroughAction) this.parts.get(p)).execute();
			}
		}
		private MultipartWriteThroughAction multipartWriteThroughAction = null;
		private boolean inUndoAction = false;
		public void atomicActionStarted(long id, String label, ImageMarkupTool imt, ImAnnotation annot, ProgressMonitor pm) {
			synchronized (this.writeThroughActions) {
				this.atomicActionStarted(id, label, imt, annot);
			}
		}
		private void atomicActionStarted(long id, String label, ImageMarkupTool imt, ImAnnotation annot) {
			//	mark beginning of UNDO action if we know what is being reverted
			if ((id < 0) && this.writeThroughActionIDs.contains(new Long(-id)))
				this.inUndoAction = true;
			//	start recording write-through action if ID positive or we don't know inverse ID
			else this.multipartWriteThroughAction = new MultipartWriteThroughAction(id, label);
		}
		public void atomicActionFinishing(long id, ProgressMonitor pm) {}
		public void atomicActionFinished(long id, ProgressMonitor pm) {
			synchronized (this.writeThroughActions) {
				this.atomicActionFinished(id);
			}
		}
		private void atomicActionFinished(long id) {
			//	remove un-done write-through action
			if (this.inUndoAction) {
				for (int a = (this.writeThroughActions.size() - 1); a >= 0; a--) {
					WriteThroughAction wta = ((WriteThroughAction) this.writeThroughActions.get(a));
					if (wta.actionId == -id) {
						this.writeThroughActions.remove(a);
						this.writeThroughActionIDs.remove(new Long(wta.actionId));
						break;
					}
					//	TODO this should always remove last write-through action (let's just see what happens ...)
				}
				this.inUndoAction = false;
			}
			//	store write-through action if any open
			else if (this.multipartWriteThroughAction != null) {
				this.writeThroughActions.add(this.multipartWriteThroughAction);
				this.writeThroughActionIDs.add(new Long(this.multipartWriteThroughAction.actionId));
				this.multipartWriteThroughAction = null;
			}
		}
		
		private void storeWriteThroughAction(WriteThroughAction wta) {
			synchronized (this.writeThroughActions) {
				if (this.multipartWriteThroughAction == null) {
					this.writeThroughActions.add(wta);
					this.writeThroughActionIDs.add(new Long(wta.actionId));
				}
				else this.multipartWriteThroughAction.addWriteThroughAction(wta);
			}
		}
		
		public void typeChanged(ImObject sdObject, String oldType) {
			if (this.inUndoAction)
				return;
			final String objectLuid; // LUID of main document object _before_ edit we need to replay
			if (sdObject instanceof ImRegion) {
				int objectPageId = this.page.pageId;
				BoundingBox objectBounds = this.revTransformer.transformBounds(((ImRegion) sdObject).bounds);
				objectLuid = LayoutObjectUuidHelper.getLocalUID(oldType, objectPageId, objectBounds);
			}
			else if (sdObject instanceof ImAnnotation) {
				ImWord sdFirstWord = ((ImAnnotation) sdObject).getFirstWord();
				ImWord sdLastWord = ((ImAnnotation) sdObject).getLastWord();
				int firstWordPageId = this.page.pageId;
				BoundingBox firstWordBounds = this.revTransformer.transformBounds(sdFirstWord.bounds);
				int lastWordPageId = this.page.pageId;
				BoundingBox lastWordBounds = this.revTransformer.transformBounds(sdLastWord.bounds);
				objectLuid = UuidHelper.getLocalUID(oldType, firstWordPageId, lastWordPageId, firstWordBounds.left, firstWordBounds.top, lastWordBounds.right, lastWordBounds.bottom);
			}
			else objectLuid = sdObject.getLocalUID();
			final String newType = sdObject.getType();
			this.storeWriteThroughAction(new WriteThroughAction(1/* complementing -1 ID of single-edit undo */) {
				void execute() {
					ImObject object = doc.getObjectByLocalUID(objectLuid);
					if (object != null)
						object.setType(newType);
				}
			});
		}
		public void attributeChanged(ImObject sdObject, final String attributeName, Object oldValue) {
			if (this.inUndoAction)
				return;
			final String objectLuid; // LUID of main document object _before_ edit we need to replay
			if (sdObject instanceof ExternalImWordProxy)
				objectLuid = ((ExternalImWordProxy) sdObject).externalImWord.getLocalUID();
			else if (sdObject instanceof ImRegion) {
				int objectPageId = this.page.pageId;
				BoundingBox objectBounds = this.revTransformer.transformBounds(((ImRegion) sdObject).bounds);
				objectLuid = LayoutObjectUuidHelper.getLocalUID(sdObject.getType(), objectPageId, objectBounds);
			}
			else if (sdObject instanceof ImAnnotation) {
				ImWord sdFirstWord = ((ImAnnotation) sdObject).getFirstWord();
				if (ImAnnotation.FIRST_WORD_ATTRIBUTE.equals(attributeName) && (oldValue instanceof ImWord))
					sdFirstWord = ((ImWord) oldValue);
				ImWord sdLastWord = ((ImAnnotation) sdObject).getLastWord();
				if (ImAnnotation.LAST_WORD_ATTRIBUTE.equals(attributeName) && (oldValue instanceof ImWord))
					sdLastWord = ((ImWord) oldValue);
				int firstWordPageId;
				BoundingBox firstWordBounds;
				if (sdFirstWord instanceof ExternalImWordProxy) {
					firstWordPageId = ((ExternalImWordProxy) sdFirstWord).externalImWord.pageId;
					firstWordBounds = ((ExternalImWordProxy) sdFirstWord).externalImWord.bounds;
				}
				else {
					firstWordPageId = this.page.pageId;
					firstWordBounds = this.revTransformer.transformBounds(sdFirstWord.bounds);
				}
				int lastWordPageId;
				BoundingBox lastWordBounds;
				if (sdLastWord instanceof ExternalImWordProxy) {
					lastWordPageId = ((ExternalImWordProxy) sdLastWord).externalImWord.pageId;
					lastWordBounds = ((ExternalImWordProxy) sdLastWord).externalImWord.bounds;
				}
				else {
					lastWordPageId = this.page.pageId;
					lastWordBounds = this.revTransformer.transformBounds(sdLastWord.bounds);
				}
				objectLuid = UuidHelper.getLocalUID(sdObject.getType(), firstWordPageId, lastWordPageId, firstWordBounds.left, firstWordBounds.top, lastWordBounds.right, lastWordBounds.bottom);
			}
			else objectLuid = sdObject.getLocalUID(); // font, supplement, etc. LUIDs remain the same
			Object sdNewValue = sdObject.getAttribute(attributeName);
			final String newValueLuid;
			final Object newValue;
			if (sdNewValue instanceof ExternalImWordProxy) {
				newValueLuid = null;
				newValue = ((ExternalImWordProxy) sdNewValue).externalImWord;
			}
			else if (sdNewValue instanceof ImRegion) {
				int newValuePageId = this.page.pageId;
				BoundingBox newValueBounds = this.revTransformer.transformBounds(((ImRegion) sdNewValue).bounds);
				newValueLuid = LayoutObjectUuidHelper.getLocalUID(((ImRegion) sdNewValue).getType(), newValuePageId, newValueBounds);
				newValue = null;
			}
			else if (sdNewValue instanceof ImAnnotation) {
				ImWord sdNewValueFirstWord = ((ImAnnotation) sdNewValue).getFirstWord();
				ImWord sdNewValueLastWord = ((ImAnnotation) sdNewValue).getLastWord();
				int newValueFirstWordPageId = this.page.pageId;
				BoundingBox newValueFirstWordBounds = this.revTransformer.transformBounds(sdNewValueFirstWord.bounds);
				int newValueLastWordPageId = this.page.pageId;
				BoundingBox newValueLastWordBounds = this.revTransformer.transformBounds(sdNewValueLastWord.bounds);
				newValueLuid = UuidHelper.getLocalUID(((ImAnnotation) sdNewValue).getType(), newValueFirstWordPageId, newValueLastWordPageId, newValueFirstWordBounds.left, newValueFirstWordBounds.top, newValueLastWordBounds.right, newValueLastWordBounds.bottom);
				newValue = null;
			}
			else if (sdNewValue instanceof BoundingBox) {
				newValueLuid = null;
				newValue = this.revTransformer.transformBounds((BoundingBox) sdNewValue);
			}
//			else if (ImWord.TEXT_DIRECTION_ATTRIBUTE.equals(attributeName) && (sdObject instanceof ImWord)) {
//				String sdWordDirection = ((String) sdObject.getAttribute(ImWord.TEXT_DIRECTION_ATTRIBUTE, ImWord.TEXT_DIRECTION_LEFT_RIGHT));
//				String wordDirection = this.revTransformer.transformTextDirection(sdWordDirection);
//				newValueLuid = null;
//				newValue = wordDirection;
//			}
//			else if (ImWord.BASELINE_ATTRIBUTE.equals(attributeName) && (sdObject instanceof ImWord) && (sdNewValue != null)) {
//				int sdBaseline = Integer.parseInt(sdNewValue.toString());
//				String sdWordDirection = ((String) sdObject.getAttribute(ImWord.TEXT_DIRECTION_ATTRIBUTE, ImWord.TEXT_DIRECTION_LEFT_RIGHT));
//				int baseline = this.revTransformer.trasnformBaseline(sdBaseline, sdWordDirection);
//				newValueLuid = null;
//				newValue = ("" + baseline);
//			}
			else {
				newValueLuid = null;
				newValue = this.revTransformer.transformAttributeValue(sdObject, attributeName, sdNewValue);
			}
			this.storeWriteThroughAction(new WriteThroughAction(1/* complementing -1 ID of single-edit undo */) {
				void execute() {
					ImObject object = doc.getObjectByLocalUID(objectLuid);
					if (object != null)
						object.setAttribute(attributeName, ((newValueLuid == null) ? newValue : doc.getObjectByLocalUID(newValueLuid)));
				}
			});
		}
		public void supplementChanged(String sdSupplementId, ImSupplement oldValue) {
			if (this.inUndoAction)
				return;
			ImSupplement sdSupplement = this.subDoc.getSupplement(sdSupplementId);
			final ImSupplement supplement;
			if (sdSupplement instanceof ImSupplement.Figure)
				supplement = ImSupplement.Figure.transformFigure(((ImSupplement.Figure) sdSupplement), this.revTransformer);
			else if (sdSupplement instanceof ImSupplement.Graphics)
				supplement = ImSupplement.Graphics.transformGraphics(((ImSupplement.Graphics) sdSupplement), this.revTransformer);
			else if (sdSupplement instanceof ImSupplement.Scan)
				supplement = null; // we never edit the original scan proper !!!
			else supplement = null; // make sure not to replace arbitrary global supplements (error protocols, etc.) !!!
			final Attributed attributes = this.getTransformedAttributes(sdSupplement);
			this.storeWriteThroughAction(new WriteThroughAction(1/* complementing -1 ID of single-edit undo */) {
				void execute() {
					if (supplement != null) {
						supplement.copyAttributes(attributes);
						doc.addSupplement(supplement);
					}
				}
			});
		}
		public void fontChanged(String fontName, ImFont oldValue) {
			//	we'll keep font changes to main window for now ...
		}
		public void regionAdded(final ImRegion sdRegion) {
			if (this.inUndoAction)
				return;
			final String type = sdRegion.getType();
			//final int pageId = this.page.pageId;
			final BoundingBox bounds = this.revTransformer.transformBounds(sdRegion.bounds);
			final Attributed attributes = this.getTransformedAttributes(sdRegion);
			final Object textDirection = this.revTransformer.transformAttributeValue(sdRegion, ImRegion.TEXT_DIRECTION_ATTRIBUTE, sdRegion.getAttribute(ImRegion.TEXT_DIRECTION_ATTRIBUTE, ImRegion.TEXT_DIRECTION_LEFT_RIGHT));
			this.storeWriteThroughAction(new WriteThroughAction(1/* complementing -1 ID of single-edit undo */) {
				void execute() {
					ImRegion region;
					if (sdRegion instanceof ImWord)
						region = new ImWord(page, bounds, ((ImWord) sdRegion).getString());
					else region = new ImRegion(page, bounds, type);
					region.copyAttributes(attributes);
					region.setAttribute(ImRegion.TEXT_DIRECTION_ATTRIBUTE, textDirection);
				}
			});
		}
		public void regionRemoved(ImRegion sdRegion) {
			if (this.inUndoAction)
				return;
			final String objectLuid; // LUID of main document object _before_ edit we need to replay
			int objectPageId = this.page.pageId;
			BoundingBox objectBounds = this.revTransformer.transformBounds(sdRegion.bounds);
			objectLuid = LayoutObjectUuidHelper.getLocalUID(sdRegion.getType(), objectPageId, objectBounds);
			this.storeWriteThroughAction(new WriteThroughAction(1/* complementing -1 ID of single-edit undo */) {
				void execute() {
					ImObject object = doc.getObjectByLocalUID(objectLuid);
					if (object instanceof ImRegion) {
						ImRegion region = ((ImRegion) object);
						region.getPage().removeRegion(region);
					}
				}
			});
		}
		public void annotationAdded(final ImAnnotation sdAnnotation) {
			if (this.inUndoAction)
				return;
			ImWord sdFirstWord = sdAnnotation.getFirstWord();
			ImWord sdLastWord = sdAnnotation.getLastWord();
			int firstWordPageId;
			BoundingBox firstWordBounds;
			if (sdFirstWord instanceof ExternalImWordProxy) {
				firstWordPageId = ((ExternalImWordProxy) sdFirstWord).externalImWord.pageId;
				firstWordBounds = ((ExternalImWordProxy) sdFirstWord).externalImWord.bounds;
			}
			else {
				firstWordPageId = this.page.pageId;
				firstWordBounds = this.revTransformer.transformBounds(sdFirstWord.bounds);
			}
			int lastWordPageId;
			BoundingBox lastWordBounds;
			if (sdLastWord instanceof ExternalImWordProxy) {
				lastWordPageId = ((ExternalImWordProxy) sdLastWord).externalImWord.pageId;
				lastWordBounds = ((ExternalImWordProxy) sdLastWord).externalImWord.bounds;
			}
			else {
				lastWordPageId = this.page.pageId;
				lastWordBounds = this.revTransformer.transformBounds(sdLastWord.bounds);
			}
			final String type = sdAnnotation.getType();
			final String firstWordLuid = LayoutObjectUuidHelper.getLocalUID(ImWord.WORD_ANNOTATION_TYPE, firstWordPageId, firstWordBounds);
			final String lastWordLuid = LayoutObjectUuidHelper.getLocalUID(ImWord.WORD_ANNOTATION_TYPE, lastWordPageId, lastWordBounds);
			final Attributed attributes = this.getTransformedAttributes(sdAnnotation);
			this.storeWriteThroughAction(new WriteThroughAction(1/* complementing -1 ID of single-edit undo */) {
				void execute() {
					ImObject firstWord = doc.getObjectByLocalUID(firstWordLuid);
					ImObject lastWord = doc.getObjectByLocalUID(lastWordLuid);
					if ((firstWord instanceof ImWord) && (lastWord instanceof ImWord)) {
						ImAnnotation annot = doc.addAnnotation(((ImWord) firstWord), ((ImWord) lastWord), type);
						annot.copyAttributes(attributes);
					}
				}
			});
		}
		public void annotationRemoved(ImAnnotation sdAnnotation) {
			if (this.inUndoAction)
				return;
			final String objectLuid; // LUID of main document object _before_ edit we need to replay
			ImWord sdFirstWord = sdAnnotation.getFirstWord();
			ImWord sdLastWord = sdAnnotation.getLastWord();
			int firstWordPageId;
			BoundingBox firstWordBounds;
			if (sdFirstWord instanceof ExternalImWordProxy) {
				firstWordPageId = ((ExternalImWordProxy) sdFirstWord).externalImWord.pageId;
				firstWordBounds = ((ExternalImWordProxy) sdFirstWord).externalImWord.bounds;
			}
			else {
				firstWordPageId = this.page.pageId;
				firstWordBounds = this.revTransformer.transformBounds(sdFirstWord.bounds);
			}
			int lastWordPageId;
			BoundingBox lastWordBounds;
			if (sdLastWord instanceof ExternalImWordProxy) {
				lastWordPageId = ((ExternalImWordProxy) sdLastWord).externalImWord.pageId;
				lastWordBounds = ((ExternalImWordProxy) sdLastWord).externalImWord.bounds;
			}
			else {
				lastWordPageId = this.page.pageId;
				lastWordBounds = this.revTransformer.transformBounds(sdLastWord.bounds);
			}
			objectLuid = UuidHelper.getLocalUID(sdAnnotation.getType(), firstWordPageId, lastWordPageId, firstWordBounds.left, firstWordBounds.top, lastWordBounds.right, lastWordBounds.bottom);
			this.storeWriteThroughAction(new WriteThroughAction(1/* complementing -1 ID of single-edit undo */) {
				void execute() {
					ImObject object = doc.getObjectByLocalUID(objectLuid);
					if (object instanceof ImAnnotation)
						doc.removeAnnotation((ImAnnotation) object);
				}
			});
		}
		
		private Attributed getTransformedAttributes(ImObject sdObject) {
			Attributed attributes = new AbstractAttributed();
			transformAttributes(sdObject, attributes, this.revTransformer);
			return attributes;
		}
	}
	
	private static class ExternalImWordProxy extends ImWord {
		final ImWord externalImWord;
		final ImPage detachedPage;
		ExternalImWordProxy(ImSubDocument doc, ImPage page, BoundingBox bounds, String string, ImWord externalImWord) {
			super(doc, page.pageId, bounds, string);
			this.externalImWord = externalImWord;
			this.detachedPage = page;
			doc.addExternalImWordProxy(this);
		}
		public ImPage getPage() {
			return this.detachedPage; // need to hold this available for attributes, XML wrapper, etc.
		}
	}
	
	private static class ExternalPredecessorProxy extends ExternalImWordProxy {
		ExternalPredecessorProxy(ImSubDocument doc, ImPage page, ImWord predecessor) {
			super(doc, page, new BoundingBox(0, 1, 0, 1), predecessor.getString(), predecessor);
			this.setTextStreamType(this.externalImWord.getTextStreamType());
		}
		public void setNextWord(ImWord nextWord) {
			ImWord oldNext = this.getNextWord();
			if (oldNext != null) // since we're detached, we won't notify about the change, so we need to invert operation
				oldNext.setPreviousWord(null);
			ImWord nextOldPrev = ((nextWord == null) ? null : nextWord.getPreviousWord());
			if (nextOldPrev != null) // since we're detached, we won't notify about the change, so we need to invert operation
				nextOldPrev.setNextWord(null);
			if (nextWord instanceof ExternalSuccessorProxy)
				super.setNextWord(nextWord); // this will be the result of some other edit whose roll-back will also revert us
			else if (nextWord != null) // since we're detached, we won't notify about the change, so we need to invert operation
				nextWord.setPreviousWord(this);
		}
		public Object setAttribute(String name, Object value) {
			if (ImWord.NEXT_WORD_ATTRIBUTE.equals(value) && ((value == null) || (value instanceof ImWord))) {
				ImWord oldNext = this.getNextWord();
				this.setNextWord((ImWord) value);
				return oldNext;
			}
			else return super.setAttribute(name, value);
		}
	}
	
	private static class ExternalSuccessorProxy extends ExternalImWordProxy {
		ExternalSuccessorProxy(ImSubDocument doc, ImPage page, ImWord successor) {
			super(doc, page, new BoundingBox((page.bounds.right-1), page.bounds.right, (page.bounds.bottom - 1), page.bounds.bottom), successor.getString(), successor);
		}
		public void setPreviousWord(ImWord prevWord) {
			ImWord oldPrev = this.getPreviousWord();
			if (oldPrev != null) // since we're detached, we won't notify about the change, so we need to invert operation
				oldPrev.setNextWord(null);
			ImWord prevOldNext = ((prevWord == null) ? null : prevWord.getNextWord());
			if (prevOldNext != null) // since we're detached, we won't notify about the change, so we need to invert operation
				prevOldNext.setPreviousWord(null);
			if (prevWord instanceof ExternalPredecessorProxy)
				super.setPreviousWord(prevWord); // this will be the result of some other edit whose roll-back will also revert us
			else if (prevWord != null) // since we're detached, we won't notify about the change, so we need to invert operation
				prevWord.setNextWord(this);
		}
		public Object setAttribute(String name, Object value) {
			if (ImWord.PREVIOUS_WORD_ATTRIBUTE.equals(value) && ((value == null) || (value instanceof ImWord))) {
				ImWord oldPrev = this.getPreviousWord();
				this.setPreviousWord((ImWord) value);
				return oldPrev;
			}
			else return super.setAttribute(name, value);
		}
	}
	
	private static ImSubDocument createSubDocument(ImPage page, BoundingBox selBounds, boolean isRegionBounds, int turnDegrees) {
		ImDocument doc = page.getDocument();
		PageImage pageImage = page.getImage();
		
		//	create document ID
		ImRegion subDocIdRegion = new ImRegion(doc, page.pageId, selBounds, "subDoc");
		String subDocId = subDocIdRegion.getUUID();
		
		//	add some margin around regions to give logic some wiggling room against sub document page edges
		BoundingBox cutBounds;
		if (isRegionBounds) {
			int cutMargin = (page.getImageDPI() / 4);
			cutBounds = new BoundingBox(
					Math.max(0, (selBounds.left - cutMargin)),
					Math.min(page.bounds.right, (selBounds.right + cutMargin)),
					Math.max(0, (selBounds.top - cutMargin)),
					Math.min(page.bounds.bottom, (selBounds.bottom + cutMargin))
				);
		}
		else cutBounds = selBounds;
		
		//	add page bounds and page image
		BoundingBox sdPageBounds;
		BufferedImage sdPageBi;
		Graphics2D sdPageBiGr;
		if (turnDegrees == ImObjectTransformer.NO_ROTATION) {
			sdPageBounds = new BoundingBox(0, cutBounds.getWidth(), 0, cutBounds.getHeight());
			sdPageBi = new BufferedImage(cutBounds.getWidth(), cutBounds.getHeight(), pageImage.image.getType());
			sdPageBiGr = sdPageBi.createGraphics();
			sdPageBiGr.setColor(Color.WHITE);
			sdPageBiGr.fillRect(0, 0, sdPageBi.getWidth(), sdPageBi.getHeight());
		}
		else if (turnDegrees == ImObjectTransformer.CLOCKWISE_ROTATION) {
			sdPageBounds = new BoundingBox(0, cutBounds.getHeight(), 0, cutBounds.getWidth());
			sdPageBi = new BufferedImage(cutBounds.getHeight(), cutBounds.getWidth(), pageImage.image.getType());
			sdPageBiGr = sdPageBi.createGraphics();
			sdPageBiGr.setColor(Color.WHITE);
			sdPageBiGr.fillRect(0, 0, sdPageBi.getWidth(), sdPageBi.getHeight());
			sdPageBiGr.translate(((sdPageBi.getWidth() - cutBounds.getWidth()) / 2), ((sdPageBi.getHeight() - cutBounds.getHeight()) / 2));
			sdPageBiGr.rotate((Math.PI / 2), (cutBounds.getWidth() / 2), (cutBounds.getHeight() / 2));
		}
		else if (turnDegrees == ImObjectTransformer.COUNTER_CLOCKWISE_ROTATION) {
			sdPageBounds = new BoundingBox(0, cutBounds.getHeight(), 0, cutBounds.getWidth());
			sdPageBi = new BufferedImage(cutBounds.getHeight(), cutBounds.getWidth(), pageImage.image.getType());
			sdPageBiGr = sdPageBi.createGraphics();
			sdPageBiGr.setColor(Color.WHITE);
			sdPageBiGr.fillRect(0, 0, sdPageBi.getWidth(), sdPageBi.getHeight());
			sdPageBiGr.translate(((sdPageBi.getWidth() - cutBounds.getWidth()) / 2), ((sdPageBi.getHeight() - cutBounds.getHeight()) / 2));
			sdPageBiGr.rotate(-(Math.PI / 2), (cutBounds.getWidth() / 2), (cutBounds.getHeight() / 2));
		}
		else throw new IllegalArgumentException("Can only rotate by 90° (clockwise) or -90° (counter-clockwise), not by " + turnDegrees);
		sdPageBiGr.drawImage(
				pageImage.image.getSubimage(selBounds.left, selBounds.top, selBounds.getWidth(), selBounds.getHeight()),
				(selBounds.left - cutBounds.left),
				(selBounds.top - cutBounds.top),
				null);
		PageImage sdPageImage = new PageImage(sdPageBi, pageImage.currentDpi, pageImage.source);
		
		//	set up boundary transformation
		final ImObjectTransformer transformer = new ImObjectTransformer(page.pageId, cutBounds, 0, sdPageBounds, turnDegrees);
		
		//	add attribute transformers (required for supplements)
		transformer.addAttributeTransformer(ImObjectTransformer.PAGE_ID_TRANSFORMER);
		transformer.addAttributeTransformer(ImObjectTransformer.BOUNDING_BOX_TRANSFORMER);
		
		//	create document and page proper
		ImSubDocument subDoc = new ImSubDocument(subDocId, sdPageImage, transformer);
		ImPage sdPage = new ImPage(subDoc, 0, sdPageBounds);
		sdPage.copyAttributes(page);
		sdPage.setImage(sdPageImage);
		
		//	add words
		ImWord[] words = page.getWords();
		HashMap wordMapping = new HashMap();
		LinkedHashSet sdWordFontNames = new LinkedHashSet();
		for (int w = 0; w < words.length; w++) {
			if (!selBounds.includes(words[w].bounds, false))
				continue;
			
			BoundingBox sdWordBounds = transformer.transformBounds(words[w].bounds);
			ImWord sdWord = new ImWord(sdPage, sdWordBounds, words[w].getString());
			wordMapping.put(words[w], sdWord);
			wordMapping.put(sdWord, words[w]);
			
			transformAttributes(words[w], sdWord, transformer);
			sdWord.setAttribute(ImWord.TEXT_DIRECTION_ATTRIBUTE, transformer.transformAttributeValue(words[w], ImWord.TEXT_DIRECTION_ATTRIBUTE, words[w].getAttribute(ImWord.TEXT_DIRECTION_ATTRIBUTE, ImWord.TEXT_DIRECTION_LEFT_RIGHT)));
			sdWord.setAttribute(ImWord.BASELINE_ATTRIBUTE, transformer.transformAttributeValue(words[w], ImWord.BASELINE_ATTRIBUTE, words[w].getAttribute(ImWord.BASELINE_ATTRIBUTE)));
			
			sdWordFontNames.add(sdWord.getAttribute(ImWord.FONT_NAME_ATTRIBUTE));
		}
		
		//	chain text streams
		ImWord[] sdWords = sdPage.getWords();
		for (int w = 0; w < sdWords.length; w++) {
			ImWord word = ((ImWord) wordMapping.get(sdWords[w]));
			ImWord pWord = word.getPreviousWord();
			if (pWord == null)
				sdWords[w].setTextStreamType(word.getTextStreamType());
			else {
				ImWord pSdWord = ((ImWord) wordMapping.get(pWord));
				if (pSdWord == null)
					pSdWord = new ExternalPredecessorProxy(subDoc, sdPage, pWord);
				sdWords[w].setPreviousWord(pSdWord);
			}
			ImWord nWord = word.getNextWord();
			if (nWord != null) {
				ImWord nSdWord = ((ImWord) wordMapping.get(nWord));
				if (nSdWord == null)
					nSdWord = new ExternalSuccessorProxy(subDoc, sdPage, nWord);
				sdWords[w].setNextWord(nSdWord);
				sdWords[w].setNextRelation(word.getNextRelation());
			}
		}
		
		//	add regions
		ImRegion[] regions = page.getRegions();
		for (int r = 0; r < regions.length; r++) {
			if (!selBounds.includes(regions[r].bounds, false))
				continue;
			
			BoundingBox sdRegionBounds = transformer.transformBounds(regions[r].bounds);
			ImRegion sdRegion = new ImRegion(sdPage, sdRegionBounds, regions[r].getType());
			transformAttributes(regions[r], sdRegion, transformer);
			sdRegion.setAttribute(ImRegion.TEXT_DIRECTION_ATTRIBUTE, transformer.transformAttributeValue(regions[r], ImRegion.TEXT_DIRECTION_ATTRIBUTE, regions[r].getAttribute(ImRegion.TEXT_DIRECTION_ATTRIBUTE, ImRegion.TEXT_DIRECTION_LEFT_RIGHT)));
		}
		
		//	add annotations
		/* TODO Keep track of annotations starting Xor ending inside sub document selection:
		 * ==> helps replace start or end word with whatever word ends up in that position after edits replayed
		 * - reduce to closest extant predecessor if end word removed
		 * - reduce to closest extant successor if start word removed */
		ImAnnotation[] annots = doc.getAnnotations(page.pageId);
		for (int a = 0; a < annots.length; a++) {
			ImWord sdFirstWord = ((ImWord) wordMapping.get(annots[a].getFirstWord()));
			ImWord sdLastWord = ((ImWord) wordMapping.get(annots[a].getLastWord()));
			if ((sdFirstWord == null) && (sdLastWord == null))
				continue;
//			if (sdFirstWord == null) {
//				for (ImWord imw = sdLastWord; imw != null; imw = imw.getPreviousWord())
//					if (imw instanceof ExternalImWordProxy) {
//						sdFirstWord = imw;
//						break;
//					}
//				//	TODO figure out how to proxy words further out
//			}
//			if (sdLastWord == null) {
//				for (ImWord imw = sdFirstWord; imw != null; imw = imw.getNextWord())
//					if (imw instanceof ExternalImWordProxy) {
//						sdLastWord = imw;
//						break;
//					}
//				//	TODO figure out how to proxy words further out
//			}
			if ((sdFirstWord == null) || (sdLastWord == null))
				continue;
			
			ImAnnotation sdAnnot = subDoc.addAnnotation(sdFirstWord, sdLastWord, annots[a].getType());
			if (sdAnnot != null) // can happen if text streams broken in main document
				transformAttributes(annots[a], sdAnnot, transformer);
		}
		
		//	add fonts
		for (Iterator fnit = sdWordFontNames.iterator(); fnit.hasNext();) {
			String fontName = ((String) fnit.next());
			ImFont font = doc.getFont(fontName);
			if (font != null)
				subDoc.addFont(font);
		}
		
		//	add supplements (page bound ones only, though, as global ones make little sense to tag along)
		ImSupplement[] supplements = page.getSupplements();
		for (int s = 0; s < supplements.length; s++) {
			if (supplements[s] instanceof ImSupplement.Scan) {
				ImSupplement.Scan scan = ((ImSupplement.Scan) supplements[s]);
				ImSupplement.Scan sdScan = ImSupplement.Scan.transformScan(scan, transformer);
				transformAttributes(scan, sdScan, transformer);
				subDoc.addSupplement(sdScan);
			}
			else if (supplements[s] instanceof ImSupplement.Figure) {
				ImSupplement.Figure figure = ((ImSupplement.Figure) supplements[s]);
				BoundingBox figureBounds = figure.getBounds();
				if (!selBounds.includes(figureBounds, false))
					continue;
				ImSupplement.Figure sdFigure = ImSupplement.Figure.transformFigure(figure, transformer);
				transformAttributes(figure, sdFigure, transformer);
				subDoc.addSupplement(sdFigure);
			}
			else if (supplements[s] instanceof ImSupplement.Graphics) {
				ImSupplement.Graphics graphics = ((ImSupplement.Graphics) supplements[s]);
				BoundingBox graphicsBounds = graphics.getBounds();
				if (!selBounds.includes(graphicsBounds, false))
					continue;
				ImSupplement.Graphics sdGraphics = ImSupplement.Graphics.transformGraphics(graphics, transformer);
				transformAttributes(graphics, sdGraphics, transformer);
				subDoc.addSupplement(sdGraphics);
			}
		}
		
		//	finally ...
		return subDoc;
	}
	
	static void transformAttributes(ImObject object, Attributed tObject, ImObjectTransformer transformer) {
		String[] ans = object.getAttributeNames();
		for (int n = 0; n < ans.length; n++) {
			Object value = object.getAttribute(ans[n]);
			Object tValue = transformer.transformAttributeValue(object, ans[n], value);
			tObject.setAttribute(ans[n], tValue);
		}
	}
	
	private static class ImSubDocument extends ImDocument {
		private PageImage pageImage;
		ImObjectTransformer transformer;
		ArrayList externalImWordProxies = new ArrayList();
		ImSubDocument(String docId, PageImage pageImage, ImObjectTransformer transformer) {
			super(docId);
			this.pageImage = pageImage;
			this.transformer = transformer;
		}
		public PageImage getPageImage(int pageId) throws IOException {
			return this.pageImage; // we only have this single page ...
		}
		void addExternalImWordProxy(ExternalImWordProxy eimwp) {
			this.externalImWordProxies.add(eimwp);
		}
		ExternalImWordProxy[] getExternalImWordProxies() {
			return ((ExternalImWordProxy[]) this.externalImWordProxies.toArray(new ExternalImWordProxy[this.externalImWordProxies.size()]));
		}
	}
}

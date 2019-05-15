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
package de.uka.ipd.idaho.im.imagine.plugins.test;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.DisplayExtensionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.DisplayExtension;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.DisplayExtensionGraphics;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;

/**
 * @author sautter
 *
 */
public class PageStructureHistogramDisplayer extends AbstractImageMarkupToolProvider implements DisplayExtensionProvider, GoldenGateImagineDocumentListener {
	private static final String HISTOGRAM_OPTION_IMT_NAME = "HistogramOptionDisplayer";
	
	private HistogramOptionDisplayer histogramOptions = new HistogramOptionDisplayer();
	private PageStructureHistograms histograms = new PageStructureHistograms();
	
	/** zero-argument constructor for class loading */
	public PageStructureHistogramDisplayer() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider#getToolsMenuItemNames()
	 */
	public String[] getToolsMenuItemNames() {
		String[] tmins = {HISTOGRAM_OPTION_IMT_NAME};
		return tmins;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		if (HISTOGRAM_OPTION_IMT_NAME.equals(name))
			return this.histogramOptions;
		else return null;
	}
	
	private class HistogramOptionDisplayer implements ImageMarkupTool {
		final JCheckBox showHistograms = new JCheckBox("Show Structure Histograms");
		final JCheckBox overlayHistograms = new JCheckBox("Overlay Histograms", true);
		final JCheckBox considerWords = new JCheckBox("Count Words", true);
		final JCheckBox considerFigures = new JCheckBox("Count Figures", true);
		final JCheckBox smoothDistribution = new JCheckBox("Smooth Distribution", true);
		final JComboBox smoothRadiusSelect;
		int smoothRadius = 1;
		
		final JCheckBox pageHistogramsHorizontal = new JCheckBox("Individual Page, Horizontal");
		final ColorButton pageHistogramHorizontalColor = new ColorButton(Color.BLUE);
		final JCheckBox pageHistogramsVertical = new JCheckBox("Individual Page, Vertical");
		final ColorButton pageHistogramVerticalColor = new ColorButton(Color.RED);
		
		final JCheckBox docHistogramsHorizontal = new JCheckBox("Document Average, Horizontal");
		final ColorButton docHistogramHorizontalColor = new ColorButton(Color.GREEN);
		final JCheckBox docHistogramsVertical = new JCheckBox("Document Average, Vertical");
		final ColorButton docHistogramVerticalColor = new ColorButton(Color.YELLOW);
		
		HistogramOptionDisplayer() {
			Integer[] smoothRadiuses = {
					Integer.valueOf(1),
					Integer.valueOf(2),
					Integer.valueOf(3),
					Integer.valueOf(5),
					Integer.valueOf(7),
					Integer.valueOf(10),
					Integer.valueOf(15)
				};
			this.smoothRadiusSelect = new JComboBox(smoothRadiuses);
			this.smoothRadiusSelect.setEditable(false);
		}
		int getSmoothRadius() {
			return (this.smoothDistribution.isSelected() ? this.smoothRadius : 0);
		}
		
		public String getLabel() {
			return "Page Structure Histogram Options";
		}
		public String getTooltip() {
			return "Options for page structure histograms";
		}
		public String getHelpText() {
			return null;
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	we're only visible in UI mode
			if (idmp == null)
				return;
			
			//	show options for page structure histograms
			JPanel optionPanel = new JPanel(new GridLayout(0, 2));
			optionPanel.add(this.showHistograms);
			optionPanel.add(this.overlayHistograms);
			optionPanel.add(this.considerWords);
			optionPanel.add(this.considerFigures);
			optionPanel.add(this.smoothDistribution);
			optionPanel.add(this.smoothRadiusSelect);
			optionPanel.add(this.pageHistogramsHorizontal);
			optionPanel.add(this.pageHistogramHorizontalColor);
			optionPanel.add(this.pageHistogramsVertical);
			optionPanel.add(this.pageHistogramVerticalColor);
			optionPanel.add(this.docHistogramsHorizontal);
			optionPanel.add(this.docHistogramHorizontalColor);
			optionPanel.add(this.docHistogramsVertical);
			optionPanel.add(this.docHistogramVerticalColor);
			DialogFactory.alert(optionPanel, "Page Structure Histogram Options", JOptionPane.PLAIN_MESSAGE);
			this.smoothRadius = ((Integer) this.smoothRadiusSelect.getSelectedItem()).intValue();
			
			//	make sure changes will show
			imagineParent.notifyDisplayExtensionsModified(idmp);
		}
	}
	private static class ColorButton extends JButton implements ActionListener {
		Color fullColor;
		Color color;
		ColorButton(Color color) {
			super("Change Color");
			this.setOpaque(true);
			this.fullColor = color;
			this.setBackground(this.fullColor);
			this.color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 64);
			this.addActionListener(this);
		}
		public void actionPerformed(ActionEvent ae) {
			Color color = JColorChooser.showDialog(this, "Change Color", this.fullColor);
			if (color != null) {
				this.fullColor = color;
				this.setBackground(this.fullColor);
				this.color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 64);
			}
		}
	}
	
	public void documentOpened(ImDocument doc, Object source, ProgressMonitor pm) {}
	public void documentSelected(ImDocument doc) {}
	public void documentSaving(ImDocument doc, Object dest, ProgressMonitor pm) {}
	public void documentSaved(ImDocument doc, Object dest, ProgressMonitor pm) {}
	public void documentClosed(String docId) {
		synchronized (this.docPagePixelDistributionCache) {
			this.docPagePixelDistributionCache.remove(docId);
		}
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.DisplayExtensionProvider#getDisplayExtensions()
	 */
	public DisplayExtension[] getDisplayExtensions() {
		DisplayExtension[] des = {this.histograms};
		return des;
	}
	
	private class PageStructureHistograms implements DisplayExtension {
		public boolean isActive() {
			return histogramOptions.showHistograms.isSelected();
		}
		public DisplayExtensionGraphics[] getExtensionGraphics(ImPage page, ImDocumentMarkupPanel idmp) {
			DocPagePixelDistribution dppd = getDocPagePixelDistribution(page);
			PagePixelDistribution ppd = dppd.getPagePixelDistribution(page);
			ArrayList degs = new ArrayList();
			if (histogramOptions.docHistogramsHorizontal.isSelected())
				degs.add(new DisplayExtensionGraphics(this, idmp, page, dppd.getRowShapes(page, !histogramOptions.overlayHistograms.isSelected(), histogramOptions.getSmoothRadius(), histogramOptions.considerWords.isSelected(), histogramOptions.considerFigures.isSelected()), histogramOptions.docHistogramHorizontalColor.color, null) {
					public boolean isActive() {
						return histogramOptions.docHistogramsHorizontal.isSelected();
					}
				});
			if (histogramOptions.docHistogramsVertical.isSelected())
				degs.add(new DisplayExtensionGraphics(this, idmp, page, dppd.getColShapes(page, !histogramOptions.overlayHistograms.isSelected(), histogramOptions.getSmoothRadius(), histogramOptions.considerWords.isSelected(), histogramOptions.considerFigures.isSelected()), histogramOptions.docHistogramVerticalColor.color, null) {
					public boolean isActive() {
						return histogramOptions.docHistogramsVertical.isSelected();
					}
				});
			degs.add(new DisplayExtensionGraphics(this, idmp, page, ppd.getRowShapes(false, histogramOptions.getSmoothRadius(), histogramOptions.considerWords.isSelected(), histogramOptions.considerFigures.isSelected()), histogramOptions.pageHistogramHorizontalColor.color, null) {
				public boolean isActive() {
					return histogramOptions.pageHistogramsHorizontal.isSelected();
				}
			});
			degs.add(new DisplayExtensionGraphics(this, idmp, page, ppd.getColShapes(false, histogramOptions.getSmoothRadius(), histogramOptions.considerWords.isSelected(), histogramOptions.considerFigures.isSelected()), histogramOptions.pageHistogramVerticalColor.color, null) {
				public boolean isActive() {
					return histogramOptions.pageHistogramsVertical.isSelected();
				}
			});
			return ((DisplayExtensionGraphics[]) degs.toArray(new DisplayExtensionGraphics[degs.size()]));
		}
	}
	
	private static class DocPagePixelDistribution {
		private int firstPageId;
		private PagePixelDistribution[] pagePixelDistributions;
		private int[] colPixelCounts = null;
		private int[] colWordPixelCounts = null;
		private int[] colFigPixelCounts = null;
		private int[] colPageCounts = null;
		private int[] rowPixelCounts = null;
		private int[] rowWordPixelCounts = null;
		private int[] rowFigPixelCounts = null;
		private int[] rowPageCounts = null;
		DocPagePixelDistribution(ImDocument doc) {
			this.firstPageId = doc.getFirstPageId();
			this.pagePixelDistributions = new PagePixelDistribution[doc.getPageCount()];
		}
		
		PagePixelDistribution getPagePixelDistribution(ImPage page) {
			int ppdIndex = (page.pageId - this.firstPageId);
			if (this.pagePixelDistributions[ppdIndex] == null)
				this.pagePixelDistributions[ppdIndex] = new PagePixelDistribution(page);
			return this.pagePixelDistributions[ppdIndex];
		}
		
		private void fillPagePixelDistributions(ImDocument doc) {
			ImPage[] pages = doc.getPages();
			int maxPageCols = 0;
			int maxPageRows = 0;
			for (int p = 0; p < pages.length; p++) {
				maxPageCols = Math.max(maxPageCols, pages[p].bounds.getWidth());
				maxPageRows = Math.max(maxPageRows, pages[p].bounds.getHeight());
			}
			this.colPixelCounts = new int[maxPageCols];
			Arrays.fill(this.colPixelCounts, 0);
			this.colWordPixelCounts = new int[maxPageCols];
			Arrays.fill(this.colWordPixelCounts, 0);
			this.colFigPixelCounts = new int[maxPageCols];
			Arrays.fill(this.colFigPixelCounts, 0);
			this.colPageCounts = new int[maxPageCols];
			Arrays.fill(this.colPageCounts, 0);
			this.rowPixelCounts = new int[maxPageRows];
			Arrays.fill(this.rowPixelCounts, 0);
			this.rowWordPixelCounts = new int[maxPageRows];
			Arrays.fill(this.rowWordPixelCounts, 0);
			this.rowFigPixelCounts = new int[maxPageRows];
			Arrays.fill(this.rowFigPixelCounts, 0);
			this.rowPageCounts = new int[maxPageRows];
			Arrays.fill(this.rowPageCounts, 0);
			for (int p = 0; p < pages.length; p++) {
				PagePixelDistribution ppd = this.getPagePixelDistribution(pages[p]);
				addArrays(this.colPixelCounts, ppd.colPixelCounts);
				addArrays(this.colWordPixelCounts, ppd.colWordPixelCounts);
				addArrays(this.colFigPixelCounts, ppd.colFigPixelCounts);
				addArrays(this.rowPixelCounts, ppd.rowPixelCounts);
				addArrays(this.rowWordPixelCounts, ppd.rowWordPixelCounts);
				addArrays(this.rowFigPixelCounts, ppd.rowFigPixelCounts);
				for (int c = 0; c < ppd.pageBounds.getWidth(); c++)
					this.colPageCounts[c]++;
				for (int r = 0; r < ppd.pageBounds.getHeight(); r++)
					this.rowPageCounts[r]++;
			}
			for (int c = 0; c < this.colPageCounts.length; c++) {
				this.colPixelCounts[c] = ((this.colPixelCounts[c] + (this.colPageCounts[c] / 2)) / this.colPageCounts[c]);
				this.colWordPixelCounts[c] = ((this.colWordPixelCounts[c] + (this.colPageCounts[c] / 2)) / this.colPageCounts[c]);
				this.colFigPixelCounts[c] = ((this.colFigPixelCounts[c] + (this.colPageCounts[c] / 2)) / this.colPageCounts[c]);
			}
			for (int r = 0; r < this.rowPageCounts.length; r++) {
				this.rowPixelCounts[r] = ((this.rowPixelCounts[r] + (this.rowPageCounts[r] / 2)) / this.rowPageCounts[r]);
				this.rowWordPixelCounts[r] = ((this.rowWordPixelCounts[r] + (this.rowPageCounts[r] / 2)) / this.rowPageCounts[r]);
				this.rowFigPixelCounts[r] = ((this.rowFigPixelCounts[r] + (this.rowPageCounts[r] / 2)) / this.rowPageCounts[r]);
			}
		}
		private static void addArrays(int[] addTo, int[] toAdd) {
			for (int i = 0; i < Math.min(addTo.length, toAdd.length); i++)
				addTo[i] += toAdd[i];
		}
		
		Shape[] getColShapes(ImPage page, boolean fromTop, int smoothRadius, boolean words, boolean figures) {
			if (this.colPixelCounts == null)
				this.fillPagePixelDistributions(page.getDocument());
			int[] pixelCounts;
			if (words && figures)
				pixelCounts = this.colPixelCounts;
			else if (words)
				pixelCounts = this.colWordPixelCounts;
			else if (figures)
				pixelCounts = this.colFigPixelCounts;
			else return new Shape[0];
			if (smoothRadius != 0)
				pixelCounts = smoothDistribution(pixelCounts, smoothRadius);
			ArrayList shapes = new ArrayList();
			int pageBottom = (page.bounds.getHeight() - 1);
			for (int c = 0; c < Math.min(page.bounds.getWidth(), pixelCounts.length); c++) {
				if (fromTop)
					shapes.add(new Line2D.Float(c, 0, c, pixelCounts[c]));
				else shapes.add(new Line2D.Float(c, pageBottom, c, (pageBottom - pixelCounts[c])));
			}
			return ((Shape[]) shapes.toArray(new Shape[shapes.size()]));
		}
		
		Shape[] getRowShapes(ImPage page, boolean fromRight, int smoothRadius, boolean words, boolean figures) {
			if (this.rowPixelCounts == null)
				this.fillPagePixelDistributions(page.getDocument());
			int[] pixelCounts;
			if (words && figures)
				pixelCounts = this.rowPixelCounts;
			else if (words)
				pixelCounts = this.rowWordPixelCounts;
			else if (figures)
				pixelCounts = this.rowFigPixelCounts;
			else return new Shape[0];
			if (smoothRadius != 0)
				pixelCounts = smoothDistribution(pixelCounts, smoothRadius);
			ArrayList shapes = new ArrayList();
			int pageRight = (page.bounds.getWidth() - 1);
			for (int r = 0; r < Math.min(page.bounds.getHeight(), pixelCounts.length); r++) {
				if (fromRight)
					shapes.add(new Line2D.Float(pageRight, r, (pageRight - pixelCounts[r]), r));
				else shapes.add(new Line2D.Float(0, r, pixelCounts[r], r));
			}
			return ((Shape[]) shapes.toArray(new Shape[shapes.size()]));
		}
	}
	
	private static class PagePixelDistribution {
		final BoundingBox pageBounds;
		final int[] colPixelCounts;
		final int[] colWordPixelCounts;
		final int[] colFigPixelCounts;
		final int[] rowPixelCounts;
		final int[] rowWordPixelCounts;
		final int[] rowFigPixelCounts;
		PagePixelDistribution(ImPage page) {
			this.pageBounds = page.bounds;
			this.colPixelCounts = new int[page.bounds.getWidth()];
			Arrays.fill(this.colPixelCounts, 0);
			this.colWordPixelCounts = new int[page.bounds.getWidth()];
			Arrays.fill(this.colWordPixelCounts, 0);
			this.colFigPixelCounts = new int[page.bounds.getWidth()];
			Arrays.fill(this.colFigPixelCounts, 0);
			this.rowPixelCounts = new int[page.bounds.getHeight()];
			Arrays.fill(this.rowPixelCounts, 0);
			this.rowWordPixelCounts = new int[page.bounds.getHeight()];
			Arrays.fill(this.rowWordPixelCounts, 0);
			this.rowFigPixelCounts = new int[page.bounds.getHeight()];
			Arrays.fill(this.rowFigPixelCounts, 0);
			
			//	count words
			ImWord[] words = page.getWords();
			for (int w = 0; w < words.length; w++) {
				BoundingBox bounds = words[w].bounds;
				bounds = bounds.intersect(this.pageBounds);
				if (bounds == null)
					continue;
				for (int c = Math.max(bounds.left, 0); c < Math.min(bounds.right, this.colPixelCounts.length); c++) {
					this.colPixelCounts[c] += bounds.getHeight();
					this.colWordPixelCounts[c] += bounds.getHeight();
				}
				for (int r = Math.max(bounds.top, 0); r < Math.min(bounds.bottom, this.rowPixelCounts.length); r++) {
					this.rowPixelCounts[r] += bounds.getWidth();
					this.rowWordPixelCounts[r] += bounds.getWidth();
				}
			}
//			
//			//	count figures ...
//			ImSupplement[] suppls = page.getSupplements();
//			for (int s = 0; s < suppls.length; s++) {
//				BoundingBox bounds;
//				if (suppls[s] instanceof Figure)
//					bounds = ((Figure) suppls[s]).getBounds();
//				else continue;
//				//	TODO shrink bounds to remove non-white edges
//				bounds = bounds.intersect(this.pageBounds);
//				if (bounds == null)
//					continue;
//				for (int c = Math.max(bounds.left, 0); c < Math.min(bounds.right, this.colPixelCounts.length); c++) {
//					this.colPixelCounts[c] += bounds.getHeight();
//					this.colFigPixelCounts[c] += bounds.getHeight();
//				}
//				for (int r = Math.max(bounds.top, 0); r < Math.min(bounds.bottom, this.rowPixelCounts.length); r++) {
//					this.rowPixelCounts[r] += bounds.getWidth();
//					this.rowFigPixelCounts[r] += bounds.getWidth();
//				}
//			}
//			
//			//	... and graphics
//			for (int s = 0; s < suppls.length; s++) {
//				Graphics graph;
//				if (suppls[s] instanceof Graphics)
//					graph = ((Graphics) suppls[s]);
//				else continue;
//				Path[] paths = graph.getPaths();
//				for (int p = 0; p < paths.length; p++) {
//					if (isInvisible(paths[p].getStrokeColor()) && isInvisible(paths[p].getFillColor()))
//						continue;
//					SubPath[] subPaths = paths[p].getSubPaths();
//					for (int sp = 0; sp < subPaths.length; sp++) {
//						BoundingBox bounds = subPaths[sp].bounds.intersect(this.pageBounds);
//						if (bounds == null)
//							continue;
//						for (int c = Math.max(bounds.left, 0); c < Math.min(bounds.right, this.colPixelCounts.length); c++) {
//							this.colPixelCounts[c] += bounds.getHeight();
//							this.colFigPixelCounts[c] += bounds.getHeight();
//						}
//						for (int r = Math.max(bounds.top, 0); r < Math.min(bounds.bottom, this.rowPixelCounts.length); r++) {
//							this.rowPixelCounts[r] += bounds.getWidth();
//							this.rowFigPixelCounts[r] += bounds.getWidth();
//						}
//					}
//				}
//			}
			
			//	count figures ...
			ImRegion[] figures = page.getRegions(ImRegion.IMAGE_TYPE);
			for (int f = 0; f < figures.length; f++) {
				BoundingBox bounds = figures[f].bounds;
				bounds = bounds.intersect(this.pageBounds);
				if (bounds == null)
					continue;
				for (int c = Math.max(bounds.left, 0); c < Math.min(bounds.right, this.colPixelCounts.length); c++) {
					this.colPixelCounts[c] += bounds.getHeight();
					this.colFigPixelCounts[c] += bounds.getHeight();
				}
				for (int r = Math.max(bounds.top, 0); r < Math.min(bounds.bottom, this.rowPixelCounts.length); r++) {
					this.rowPixelCounts[r] += bounds.getWidth();
					this.rowFigPixelCounts[r] += bounds.getWidth();
				}
			}
			
			//	... and graphics
			ImRegion[] graphics = page.getRegions(ImRegion.GRAPHICS_TYPE);
			for (int g = 0; g < graphics.length; g++) {
				BoundingBox bounds = graphics[g].bounds;
				bounds = bounds.intersect(this.pageBounds);
				if (bounds == null)
					continue;
				for (int c = Math.max(bounds.left, 0); c < Math.min(bounds.right, this.colPixelCounts.length); c++) {
					this.colPixelCounts[c] += bounds.getHeight();
					this.colFigPixelCounts[c] += bounds.getHeight();
				}
				for (int r = Math.max(bounds.top, 0); r < Math.min(bounds.bottom, this.rowPixelCounts.length); r++) {
					this.rowPixelCounts[r] += bounds.getWidth();
					this.rowFigPixelCounts[r] += bounds.getWidth();
				}
			}
		}
//		private static boolean isInvisible(Color color) {
//			if (color == null)
//				return true;
//			if (color.getAlpha() < 32)
//				return true;
//			return ((color.getRed() > 224) && (color.getGreen() > 224) && (color.getBlue() > 224));
//		}
		
		Shape[] getColShapes(boolean fromTop, int smoothRadius, boolean words, boolean figures) {
			int[] pixelCounts;
			if (words && figures)
				pixelCounts = this.colPixelCounts;
			else if (words)
				pixelCounts = this.colWordPixelCounts;
			else if (figures)
				pixelCounts = this.colFigPixelCounts;
			else return new Shape[0];
			if (smoothRadius != 0)
				pixelCounts = smoothDistribution(pixelCounts, smoothRadius);
			ArrayList shapes = new ArrayList();
			int pageBottom = (this.pageBounds.getHeight() - 1);
			for (int c = 0; c < pixelCounts.length; c++) {
				if (fromTop)
					shapes.add(new Line2D.Float(c, 0, c, pixelCounts[c]));
				else shapes.add(new Line2D.Float(c, pageBottom, c, (pageBottom - pixelCounts[c])));
			}
			return ((Shape[]) shapes.toArray(new Shape[shapes.size()]));
		}
		
		Shape[] getRowShapes(boolean fromRight, int smoothRadius, boolean words, boolean figures) {
			int[] pixelCounts;
			if (words && figures)
				pixelCounts = this.rowPixelCounts;
			else if (words)
				pixelCounts = this.rowWordPixelCounts;
			else if (figures)
				pixelCounts = this.rowFigPixelCounts;
			else return new Shape[0];
			if (smoothRadius != 0)
				pixelCounts = smoothDistribution(pixelCounts, smoothRadius);
			ArrayList shapes = new ArrayList();
			int pageRight = (this.pageBounds.getWidth() - 1);
			for (int r = 0; r < pixelCounts.length; r++) {
				if (fromRight)
					shapes.add(new Line2D.Float(pageRight, r, (pageRight - pixelCounts[r]), r));
				else shapes.add(new Line2D.Float(0, r, pixelCounts[r], r));
			}
			return ((Shape[]) shapes.toArray(new Shape[shapes.size()]));
		}
	}
	
	static int[] smoothDistribution(int[] pixelCounts, int radius) {
		int[] sPixelCounts = new int[pixelCounts.length];
		for (int i = 0; i < pixelCounts.length; i++) {
			int pixelCountSum = 0;
			for (int s = (i - radius); s <= (i + radius); s++)
				pixelCountSum += ((s < 0) ? 0 : ((s < pixelCounts.length) ? pixelCounts[s] : 0));
			sPixelCounts[i] = ((pixelCountSum + radius) / (radius + 1 + radius));
		}
		return sPixelCounts;
	}
	
	private HashMap docPagePixelDistributionCache = new HashMap();
	private DocPagePixelDistribution getDocPagePixelDistribution(ImPage page) {
		ImDocument doc = page.getDocument();
		DocPagePixelDistribution dppd;
		synchronized (this.docPagePixelDistributionCache) {
			dppd = ((DocPagePixelDistribution) this.docPagePixelDistributionCache.get(doc.docId));
		}
		if (dppd == null) {
			dppd = new DocPagePixelDistribution(page.getDocument());
			synchronized (this.docPagePixelDistributionCache) {
				this.docPagePixelDistributionCache.put(doc.docId, dppd);
			}
		}
		return dppd;
	}
}

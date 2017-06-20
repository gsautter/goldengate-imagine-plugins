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
package de.uka.ipd.idaho.im.imagine.plugins.ocr;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import javax.imageio.ImageIO;

import de.uka.ipd.idaho.gamta.AttributeUtils;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.gamta.util.swing.ProgressMonitorDialog;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImLayoutObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.analysis.Imaging;
import de.uka.ipd.idaho.im.analysis.Imaging.AnalysisImage;
import de.uka.ipd.idaho.im.analysis.Imaging.ImagePartRectangle;
import de.uka.ipd.idaho.im.analysis.PageImageAnalysis;
import de.uka.ipd.idaho.im.analysis.PageImageAnalysis.Line;
import de.uka.ipd.idaho.im.analysis.WordImageAnalysis;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ImageEditToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.basic.RegionActionProvider;
import de.uka.ipd.idaho.im.ocr.OcrEngine;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImImageEditorPanel;
import de.uka.ipd.idaho.im.util.ImImageEditorPanel.ImImageEditTool;
import de.uka.ipd.idaho.im.util.ImImageEditorPanel.SelectionImageEditTool;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.im.utilities.ImageDisplayDialog;

/**
 * This plugin provides functionality for running OCR on selected blocks of a
 * page image.
 * 
 * @author sautter
 */
public class BlockOcrProvider extends AbstractSelectionActionProvider implements ImageEditToolProvider {
	
	private static final boolean DEBUG_LINE_SPLITTING = false;
	
	private static final int minLineDistance = 5;
	private static final int whiteRgb = Color.WHITE.getRGB();
	
	private OcrEngine ocrEngine = null;
	
	private ImageBlockOCR imageBlockOcr = null;
	
	private RegionActionProvider regionActions = null;
	
	/** public zero-argument constructor for class loading */
	public BlockOcrProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "IM Block OCR Provider";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractGoldenGateImaginePlugin#initImagine()
	 */
	public void initImagine() {
		
		//	get OCR engine
		this.ocrEngine = this.imagineParent.getOcrEngine();
		if (this.ocrEngine != null)
			this.imageBlockOcr = new ImageBlockOCR();
		
		//	get region action provider
		this.regionActions = ((RegionActionProvider) this.imagineParent.getPlugin(RegionActionProvider.class.getName()));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(final Point start, final Point end, final ImPage page, ImDocumentMarkupPanel idmp) {
		
		//	little we'd have to offer on born-digital documents ...
		if (idmp.documentBornDigital)
			return null;
		
		//	little we can do without an OCR engine ...
		if (this.ocrEngine == null)
			return null;
		
		//	collect actions (there might be more in the future ...)
		LinkedList actions = new LinkedList();
		
		//	offer block OCR in main window for scanned documents
		actions.add(new SelectionAction("textBlock", "Mark OCR Text Block", "Apply OCR to the selected page region and mark the resulting words as a text block") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				return doBlockOcr(start, end, page, ProgressMonitor.dummy);
			}
		});
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	private boolean doBlockOcr(Point start, Point end, ImPage page, ProgressMonitor pm) {
		
		//	get page image
		PageImage pi = page.getPageImage();
		
		//	compute bounds
		int left = Math.min(start.x, end.x);
		int right = Math.max(start.x, end.x);
		int top = Math.min(start.y, end.y);
		int bottom = Math.max(start.y, end.y);
		BoundingBox blockBounds = new BoundingBox(left, right, top, bottom);
		
		//	OCR detect words
		ImWord[] dBlockWords = this.detectBlockWords(page.getDocument(), page.pageId, pi.image, pi.currentDpi, blockBounds, pm);
		if ((dBlockWords == null) || (dBlockWords.length == 0))
			return false;
		
		//	get and index existing words
		ImWord[] eBlockWords = page.getWordsInside(blockBounds);
		HashMap eBlockWordsByBounds = new HashMap();
		for (int w = 0; w < eBlockWords.length; w++)
			eBlockWordsByBounds.put(eBlockWords[w].bounds, eBlockWords[w]);
		
		//	order existing words, and find predecessor and successor
		ImWord blockPredecessor = null;
		ImWord blockSuccessor = null;
		if (eBlockWords.length != 0) {
			ImUtils.orderStream(eBlockWords, ImUtils.leftRightTopDownOrder);
			blockPredecessor = eBlockWords[0].getPreviousWord();
			blockSuccessor = eBlockWords[eBlockWords.length - 1].getNextWord();
		}
		
		//	cut existing words out of any larger text stream
		if (blockPredecessor != null)
			blockPredecessor.setNextWord(blockSuccessor);
		else if (blockSuccessor != null)
			blockSuccessor.setPreviousWord(blockPredecessor);
		
		//	dissolve existing text stream (back to front to save effort for propagating text stream ID)
		for (int w = (eBlockWords.length-1); w > 0; w--)
			eBlockWords[w].setPreviousWord(null);
		
		//	delta with existing words
		pm.setStep("Adding detected words");
		for (int w = 0; w < dBlockWords.length; w++) {
			ImWord eBlockWord = ((ImWord) eBlockWordsByBounds.remove(dBlockWords[w].bounds));
			if (eBlockWord == null) {
				pm.setInfo("Adding word '" + dBlockWords[w].getString() + "' at " + dBlockWords[w].bounds.toString());
				page.addWord(dBlockWords[w]);
			}
			else {
				pm.setInfo("Retaining word '" + eBlockWord.getString() + "' at " + eBlockWord.bounds.toString());
				AttributeUtils.copyAttributes(dBlockWords[w], eBlockWord, AttributeUtils.ADD_ATTRIBUTE_COPY_MODE);
			}
		}
		pm.setStep("Removing spurious words");
		for (Iterator wit = eBlockWordsByBounds.keySet().iterator(); wit.hasNext();) {
			ImWord imw = ((ImWord) eBlockWordsByBounds.get(wit.next()));
			pm.setInfo("Removing word '" + imw.getString() + "' at " + imw.bounds.toString());
			page.removeWord(imw, true);
		}
		
		//	make block words into stream
		ImWord[] blockWords = page.getWordsInside(blockBounds);
		ImUtils.orderStream(blockWords, ImUtils.leftRightTopDownOrder);
		
		//	connect first and last word
		if (blockWords.length != 0) {
			if (blockPredecessor != null)
				blockWords[0].setPreviousWord(blockPredecessor);
			if (blockSuccessor != null)
				blockWords[blockWords.length-1].setNextWord(blockSuccessor);
		}
		
		//	clean up any blocks, paragraphs, and lines
		blockBounds = ImLayoutObject.getAggregateBox(blockWords);
		ImRegion[] pageRegions = page.getRegions();
		for (int r = 0; r < pageRegions.length; r++) {
			if (!blockBounds.overlaps(pageRegions[r].bounds))
				continue;
			if (ImRegion.LINE_ANNOTATION_TYPE.equals(pageRegions[r].getType()) || ImRegion.PARAGRAPH_TYPE.equals(pageRegions[r].getType()) || ImRegion.BLOCK_ANNOTATION_TYPE.equals(pageRegions[r].getType()))
				page.removeRegion(pageRegions[r]);
		}
		
		//	mark block (region action provider will take care of inner block structure)
		if (this.regionActions == null)
			new ImRegion(page, blockBounds, ImRegion.BLOCK_ANNOTATION_TYPE);
		else this.regionActions.markBlock(page, blockBounds);
		
		//	indicate something changed
		return true;
	}
	
	private ImWord[] detectBlockWords(ImDocument doc, int pageId, BufferedImage pageBi, int pageBiDpi, BoundingBox blockBounds, ProgressMonitor pm) {
		
		//	get selected image block
		BufferedImage bi = pageBi.getSubimage(blockBounds.left, blockBounds.top, blockBounds.getWidth(), blockBounds.getHeight());
		
		//	TODO apply skew detection here, ...
		//	TODO ... compute column shifts, and ...
		//	TODO ... shift detected words into place
		
		//	detect lines in block image
		pm.setStep("Computing line split");
		AnalysisImage bai = Imaging.wrapImage(bi, null);
		byte[][] baiBrightness = bai.getBrightness();
		for (int c = 0; c < baiBrightness.length; c++)
			for (int r = 0; r < baiBrightness[c].length; r++) {
				if (baiBrightness[c][r] > 120)
					baiBrightness[c][r] = ((byte) 127);
			}
		ImagePartRectangle bRect = Imaging.getContentBox(bai);
//		BlockData bData = analyzeBlock(bRect, pageBiDpi, pm);
//		ImagePartRectangle[] bLines = bData.getBlockLines();
		Line[] bLines = PageImageAnalysis.findBlockLines(bRect, pageBiDpi, pm);
		
		//	compute line offsets
		int[] bLineOffsets = new int[bLines.length];
		bLineOffsets[0] = 0;
		for (int l = 1; l < bLines.length; l++) {
//			if ((bLines[l].getTopRow() - bLines[l-1].getBottomRow()) >= minLineDistance)
			if ((bLines[l].bounds.getTopRow() - bLines[l-1].bounds.getBottomRow()) >= minLineDistance)
				bLineOffsets[l] = bLineOffsets[l-1];
//			else bLineOffsets[l] = bLineOffsets[l-1] + (minLineDistance - (bLines[l].getTopRow() - bLines[l-1].getBottomRow()));
			else bLineOffsets[l] = bLineOffsets[l-1] + (minLineDistance - (bLines[l].bounds.getTopRow() - bLines[l-1].bounds.getBottomRow()));
		}
		
		//	compute region coloring
		pm.setStep("Computing region coloring");
		int[][] bRegionColors = Imaging.getRegionColoring(bai, Imaging.computeAverageBrightness(bai), false);
		int bMaxRegionColor = getMaxRegionColor(bRegionColors);
		
		//	compute presence of each region color in each line
		pm.setStep("Computing region color distribution");
		int[] bRegionColorFrequencies = new int[bMaxRegionColor + 1];
		Arrays.fill(bRegionColorFrequencies, 0);
		int[] bRegionColorMinRows = new int[bMaxRegionColor + 1];
		Arrays.fill(bRegionColorMinRows, Integer.MAX_VALUE);
		int[] bRegionColorMaxRows = new int[bMaxRegionColor + 1];
		Arrays.fill(bRegionColorMaxRows, 0);
		for (int c = 0; c < bRegionColors.length; c++)
			for (int r = 0; r < bRegionColors[c].length; r++) {
				bRegionColorFrequencies[bRegionColors[c][r]]++;
				bRegionColorMinRows[bRegionColors[c][r]] = Math.min(bRegionColorMinRows[bRegionColors[c][r]], r);
				bRegionColorMaxRows[bRegionColors[c][r]] = Math.max(bRegionColorMaxRows[bRegionColors[c][r]], r);
			}
		int[][] bLineRegionColorFrequencies = new int[bLines.length][];
		for (int l = 0; l < bLines.length; l++) {
			bLineRegionColorFrequencies[l] = new int[bMaxRegionColor + 1];
			Arrays.fill(bLineRegionColorFrequencies[l], 0);
//			for (int c = bLines[l].getLeftCol(); c < bLines[l].getRightCol(); c++) {
			for (int c = bLines[l].bounds.getLeftCol(); c < bLines[l].bounds.getRightCol(); c++) {
//				for (int r = bLines[l].getTopRow(); r < bLines[l].getBottomRow(); r++)
				for (int r = bLines[l].bounds.getTopRow(); r < bLines[l].bounds.getBottomRow(); r++)
					bLineRegionColorFrequencies[l][bRegionColors[c][r]]++;
			}
		}
		
		//	generate block image, with line offsets
		pm.setStep("Generating expanded block image");
		BufferedImage ocrBi = new BufferedImage(bRect.getWidth(), (bRect.getHeight() + (bLineOffsets[bLineOffsets.length-1])), BufferedImage.TYPE_BYTE_GRAY);
		Graphics ocrg = ocrBi.createGraphics();
		ocrg.setColor(Color.WHITE);
		ocrg.fillRect(0, 0, ocrBi.getWidth(), ocrBi.getHeight());
		for (int l = 0; l < bLines.length; l++) {
//			ocrg.drawImage(bi.getSubimage(bLines[l].getLeftCol(), bLines[l].getTopRow(), bLines[l].getWidth(), bLines[l].getHeight()), (bLines[l].getLeftCol() - bRect.getLeftCol()), (bLines[l].getTopRow() - bRect.getTopRow() + bLineOffsets[l]), null);
			ocrg.drawImage(bi.getSubimage(bLines[l].bounds.getLeftCol(), bLines[l].bounds.getTopRow(), bLines[l].bounds.getWidth(), bLines[l].bounds.getHeight()), (bLines[l].bounds.getLeftCol() - bRect.getLeftCol()), (bLines[l].bounds.getTopRow() - bRect.getTopRow() + bLineOffsets[l]), null);
//			for (int c = bLines[l].getLeftCol(); c < bLines[l].getRightCol(); c++)
			for (int c = bLines[l].bounds.getLeftCol(); c < bLines[l].bounds.getRightCol(); c++)
//				for (int r = bLines[l].getTopRow(); r < bLines[l].getBottomRow(); r++) {
				for (int r = bLines[l].bounds.getTopRow(); r < bLines[l].bounds.getBottomRow(); r++) {
					int bLineRegionColorFrequency = bLineRegionColorFrequencies[l][bRegionColors[c][r]];
					
					//	check upward shift of lower descender tips if region color does not occur below line center
//					if ((l != 0) && (bRegionColorMaxRows[bRegionColors[c][r]] < ((bLines[l].getTopRow() + bLines[l].getBottomRow()) / 2)) && (bLineRegionColorFrequencies[l-1][bRegionColors[c][r]] > (3 * bLineRegionColorFrequency))) {
					if ((l != 0) && (bRegionColorMaxRows[bRegionColors[c][r]] < ((bLines[l].bounds.getTopRow() + bLines[l].bounds.getBottomRow()) / 2)) && (bLineRegionColorFrequencies[l-1][bRegionColors[c][r]] > (3 * bLineRegionColorFrequency))) {
						int rgb = ocrBi.getRGB((c - bRect.getLeftCol()), (r - bRect.getTopRow() + bLineOffsets[l]));
						ocrBi.setRGB((c - bRect.getLeftCol()), (r - bRect.getTopRow() + bLineOffsets[l-1]), rgb);
						ocrBi.setRGB((c - bRect.getLeftCol()), (r - bRect.getTopRow() + bLineOffsets[l]), whiteRgb);
					}
					
					//	check downward shift of upper ascender tips if region color does not occur above line center
//					if (((l+1) < bLines.length) && bRegionColorMinRows[bRegionColors[c][r]] > ((bLines[l].getTopRow() + bLines[l].getBottomRow()) / 2) && (bLineRegionColorFrequencies[l+1][bRegionColors[c][r]] > (3 * bLineRegionColorFrequency))) {
					if (((l+1) < bLines.length) && bRegionColorMinRows[bRegionColors[c][r]] > ((bLines[l].bounds.getTopRow() + bLines[l].bounds.getBottomRow()) / 2) && (bLineRegionColorFrequencies[l+1][bRegionColors[c][r]] > (3 * bLineRegionColorFrequency))) {
						int rgb = ocrBi.getRGB((c - bRect.getLeftCol()), (r - bRect.getTopRow() + bLineOffsets[l]));
						ocrBi.setRGB((c - bRect.getLeftCol()), (r - bRect.getTopRow() + bLineOffsets[l+1]), rgb);
						ocrBi.setRGB((c - bRect.getLeftCol()), (r - bRect.getTopRow() + bLineOffsets[l]), whiteRgb);
					}
				}
		}
		
		//	wrap region
		pm.setStep("Wrapping block image");
		final PageImage ocrPi = new PageImage(ocrBi, pageBiDpi, null);
		ImDocument ocrDoc = new ImDocument("OcrTemp") {
			public PageImage getPageImage(int pageId) throws IOException {
				return ocrPi;
			}
		};
		ImPage ocrPage = new ImPage(ocrDoc, 0, new BoundingBox(0, ocrBi.getWidth(), 0, ocrBi.getHeight())) {
			public PageImage getImage() {
				return ocrPi;
			}
		};
		ImRegion ocrBlock = new ImRegion(ocrPage, ocrPage.bounds, ImRegion.BLOCK_ANNOTATION_TYPE);
		
		if (DEBUG_LINE_SPLITTING)
			showImage(ocrBi, null, 0, null, null, 127, null);
		
		//	run block OCR
		else try {
			pm.setStep("Doing OCR");
			this.ocrEngine.doBlockOcr(ocrBlock, ocrPi, pm);
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
			return null;
		}
		ImWord[] ocrWords = ocrBlock.getWords();
		if (ocrWords.length == 0)
			return null;
		
		//	detect word properties (font size, bold, italics)
		pm.setStep("Detecting word properties");
		
		//	add word baselines from block image analysis (OCR image starts only at top of first line, as do row pixel count drops)
		for (int l = 0; l < bLines.length; l++) {
			if (bLines[l].getBaseline() == -1)
				continue;
			for (int w = 0; w < ocrWords.length; w++) {
//				if ((ocrWords[w].centerY > (bLines[l].getTopRow() - bRect.getTopRow() + bLineOffsets[l])) && (ocrWords[w].centerY < (bLines[l].getBottomRow() - bRect.getTopRow() + bLineOffsets[l])))
				if ((ocrWords[w].centerY > (bLines[l].bounds.getTopRow() - bRect.getTopRow() + bLineOffsets[l])) && (ocrWords[w].centerY < (bLines[l].bounds.getBottomRow() - bRect.getTopRow() + bLineOffsets[l])))
//					ocrWords[w].setAttribute(ImWord.BASELINE_ATTRIBUTE, ("" + (bLineOffsets[l] + bData.rowPixelCountDrops[l])));
//					ocrWords[w].setAttribute(ImWord.BASELINE_ATTRIBUTE, ("" + (bLineOffsets[l] + bData.rowPixelCountDrops[l].row)));
					ocrWords[w].setAttribute(ImWord.BASELINE_ATTRIBUTE, ("" + (bLineOffsets[l] + bLines[l].getBaseline())));
			}
		}
		
		//	analyze font metrics
		WordImageAnalysis.analyzeFontMetrics(ocrBlock, pm);
		
		//	translate words to page coordinates, subtracting vertical offsets
		pm.setStep("Translating detected words");
		ArrayList blockWords = new ArrayList();
		for (int l = 0; l < bLines.length; l++)
			for (int w = 0; w < ocrWords.length; w++) {
				if (ocrWords[w] == null)
					continue;
//				if ((ocrWords[w].centerY > (bLines[l].getTopRow() - bRect.getTopRow() + bLineOffsets[l])) && (ocrWords[w].centerY < (bLines[l].getBottomRow() - bRect.getTopRow() + bLineOffsets[l]))) {
				if ((ocrWords[w].centerY > (bLines[l].bounds.getTopRow() - bRect.getTopRow() + bLineOffsets[l])) && (ocrWords[w].centerY < (bLines[l].bounds.getBottomRow() - bRect.getTopRow() + bLineOffsets[l]))) {
					//	TODO observe page image edges
					ImWord bWord = new ImWord(doc, pageId, new BoundingBox(
							(ocrWords[w].bounds.left + blockBounds.left + bRect.getLeftCol()),
							(ocrWords[w].bounds.right + blockBounds.left + bRect.getLeftCol()),
							(ocrWords[w].bounds.top + blockBounds.top + bRect.getTopRow() - bLineOffsets[l]),
							(ocrWords[w].bounds.bottom + blockBounds.top + bRect.getTopRow() - bLineOffsets[l])
						), ocrWords[w].getString());
					bWord.copyAttributes(ocrWords[w]);
					if (ocrWords[w].hasAttribute(ImWord.BASELINE_ATTRIBUTE)) {
						int ocrWordBl = Integer.parseInt((String) ocrWords[w].getAttribute(ImWord.BASELINE_ATTRIBUTE));
						bWord.setAttribute(ImWord.BASELINE_ATTRIBUTE, ("" + (ocrWordBl + blockBounds.top + bRect.getTopRow() - bLineOffsets[l])));
					}
					blockWords.add(bWord);
					ocrWords[w] = null;
				}
			}
		
		//	finally ...
		return ((ImWord[]) blockWords.toArray(new ImWord[blockWords.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageEditToolProvider#getImageEditTools()
	 */
	public ImImageEditTool[] getImageEditTools() {
		if (this.imageBlockOcr == null)
			return new ImImageEditTool[0];
		ImImageEditTool[] iiets = {this.imageBlockOcr};
		return iiets;
	}
	
	private class ImageBlockOCR extends SelectionImageEditTool {
		ImageBlockOCR() {
			super("Detect Words", "Run OCR on a text block to detect words", null, true);
		}
		
		protected void doEdit(final ImImageEditorPanel iiep, final int sx, final int sy, final int ex, final int ey) {
			
			//	use progress monitor splash screen
			final ProgressMonitorDialog pmd = new ProgressMonitorDialog(DialogFactory.getTopWindow(), "Detecting Words ...");
			pmd.setSize(400, 130);
			pmd.setLocationRelativeTo(DialogFactory.getTopWindow());
			
			//	do the job
			Thread dwt = new Thread() {
				public void run() {
					try {
						while (!pmd.getWindow().isVisible()) try {
							Thread.sleep(10);
						} catch (InterruptedException ie) {}
						detectWords(iiep, sx, sy, ex, ey, pmd);
					}
					
					//	make sure to close progress monitor
					finally {
						pmd.close();
					}
				}
			};
			dwt.start();
			pmd.popUp(true);
		}
		
		protected void detectWords(ImImageEditorPanel iiep, int sx, int sy, int ex, int ey, ProgressMonitor pm) {
			
			//	compute bounds
			int left = Math.min(sx, ex);
			int right = Math.max(sx, ex);
			int top = Math.min(sy, ey);
			int bottom = Math.max(sy, ey);
			BoundingBox blockBounds = new BoundingBox(left, right, top, bottom);
			
			//	get words
			ImWord[] blockWords = detectBlockWords(iiep.getPage().getDocument(), iiep.getPage().pageId, iiep.getImage(), iiep.getPageImage().currentDpi, blockBounds, pm);
			if ((blockWords == null) || (blockWords.length == 0))
				return;
			
			//	remove existing words
			pm.setStep("Removing spurious words");
			ImWord[] eBlockWords = iiep.getWords();
			for (int w = 0; w < eBlockWords.length; w++) {
				if (eBlockWords[w].bounds.liesIn(blockBounds, true))
					iiep.removeWord(eBlockWords[w]);
			}
			
			//	add words to document
			pm.setStep("Adding detected words");
			for (int w = 0; w < blockWords.length; w++)
				iiep.addWord(blockWords[w]);
		}
	}
	
	private static int getMaxRegionColor(int[][] regionColors) {
		int maxRegionColor = 0;
		for (int c = 0; c < regionColors.length; c++) {
			for (int r = 0; r < regionColors[c].length; r++)
				maxRegionColor = Math.max(maxRegionColor, regionColors[c][r]);
		}
		return maxRegionColor;
	}
//	
//	private static BlockData analyzeBlock(ImagePartRectangle block, int dpi, ProgressMonitor pm) {
//		
//		//	compute row brightness
//		byte[][] brightness = block.getImage().getBrightness();
//		int[] rowBrightness = getRowBrightness(block, brightness);
//		int[] rowPixelCount = getRowPixelCount(block, brightness);
//		
//		//	wrap brightness and pixel row occupancy histograms, computing basic analyses along the way
//		BlockData blockData = new BlockData(block, rowBrightness, rowPixelCount);
//		
//		//	check for last line
//		blockData.checkForLastLine();
//		
//		//	finally ...
//		return blockData;
//	}
//	
//	private static class BlockData {
//		ImagePartRectangle block;
//		
//		int[] rowBrightness;
//		int avgRowBrightness;
//		
//		int[] rowPixelCount;
//		int avgRowPixelCount;
//		
////		int[] rowPixelCountRises; // x-height rows of lines
//		HistogramRiseDrop[] rowPixelCountRises; // x-height rows of lines
////		int[] rowPixelCountDrops; // baseline rows of lines
//		HistogramRiseDrop[] rowPixelCountDrops; // baseline rows of lines
//		
//		int[] highRowPixelCenters; // line centers, i.e., middle of x-height
//		int[] highRowPixelWidths; // x-heights of lines
//		int avgHighRowPixelWidth; // average x-height
//		
//		int[] lowRowPixelCenters; // line boundaries
//		
//		BlockData(ImagePartRectangle block, int[] rowBrightness, int[] rowPixelCount) {
//			this.block = block;
//			
//			this.rowBrightness = rowBrightness;
//			System.out.println("Row brightness:");
//			for (int r = 0; r < rowBrightness.length; r++)
//				System.out.println(r + ": " + rowBrightness[r]);
//			this.avgRowBrightness = getHistogramAverage(this.rowBrightness);
//			System.out.println("avg brightness: " + avgRowBrightness);
//			
//			this.rowPixelCount = rowPixelCount;
//			System.out.println("Row occupation:");
//			for (int r = 0; r < rowBrightness.length; r++)
//				System.out.println(r + ": " + rowPixelCount[r]);
//			this.avgRowPixelCount = getHistogramAverage(rowPixelCount);
//			System.out.println("avg row pixels: " + avgRowPixelCount);
//			
//			//	assess row pixel count distribution
//			this.rowPixelCountRises = getHistgramMaxRises(this.rowPixelCount, this.avgRowPixelCount);
////			System.out.println("Row height from x-heights: " + getDistanceAverage(rowPixelCountRises) + " (min " + getDistanceMin(rowPixelCountRises) + ", max " + getDistanceMax(rowPixelCountRises) + ")");
//			this.rowPixelCountDrops = getHistgramMaxDrops(this.rowPixelCount, this.avgRowPixelCount);
////			System.out.println("Row height from baselines: " + getDistanceAverage(rowPixelCountDrops) + " (min " + getDistanceMin(rowPixelCountDrops) + ", max " + getDistanceMax(rowPixelCountDrops) + ")");
//			
//			//	filter very small rises and drops immediately adjacent to larger drops and rises !!!
//			this.filterRowPixelCountRisesAndDrops();
//			
//			//	get centers of high and low areas
//			this.highRowPixelCenters = getHighCenters(this.rowPixelCountRises, this.rowPixelCountDrops);
////			System.out.println("Row height from line centers: " + getDistanceAverage(highRowPixelCenters) + " (min " + getDistanceMin(highRowPixelCenters) + ", max " + getDistanceMax(highRowPixelCenters) + ")");
//			this.lowRowPixelCenters = getLowCenters(this.rowPixelCountRises, this.rowPixelCountDrops);
////			System.out.println("Row height from line gap centers: " + getDistanceAverage(lowRowPixelCenters) + " (min " + getDistanceMin(lowRowPixelCenters) + ", max " + getDistanceMax(lowRowPixelCenters) + ")");
//			
//			this.highRowPixelWidths = getHighWidths(this.rowPixelCountRises, this.rowPixelCountDrops);
//			this.avgHighRowPixelWidth = getHistogramAverage(this.highRowPixelWidths);
//		}
//		
//		private void filterRowPixelCountRisesAndDrops() {
//			ArrayList rowPixelCountRises = new ArrayList();
//			for (int r = 0; r < this.rowPixelCountRises.length; r++) {
//				for (int d = 0; d < this.rowPixelCountDrops.length; d++) {
//					int dist = Math.abs(this.rowPixelCountRises[r].row - this.rowPixelCountDrops[d].row);
//					if (dist > 2) // TODO_not maybe only allow 1? ==> TODO_not make this dependent on DPI !!!
//						continue;
//					if (this.rowPixelCountRises[r].delta < this.rowPixelCountDrops[d].delta) {
//						System.out.println("Eliminated rise of " + this.rowPixelCountRises[r].delta + " at " + this.rowPixelCountRises[r].row + " as adjacent to drop of " + this.rowPixelCountDrops[d].delta + " at " + this.rowPixelCountDrops[d].row);
//						this.rowPixelCountRises[r] = null;
//						break;
//					}
//				}
//				if (this.rowPixelCountRises[r] != null)
//					rowPixelCountRises.add(this.rowPixelCountRises[r]);
//			}
//			if (rowPixelCountRises.size() < this.rowPixelCountRises.length)
//				this.rowPixelCountRises = ((HistogramRiseDrop[]) rowPixelCountRises.toArray(new HistogramRiseDrop[rowPixelCountRises.size()]));
//			
//			ArrayList rowPixelCountDrops = new ArrayList();
//			for (int d = 0; d < this.rowPixelCountDrops.length; d++) {
//				for (int r = 0; r < this.rowPixelCountRises.length; r++) {
//					int dist = Math.abs(this.rowPixelCountDrops[d].row - this.rowPixelCountRises[r].row);
//					if (dist > 2) // TODO_not maybe only allow 1? ==> TODO_not make this dependent on DPI !!!
//						continue;
//					if (this.rowPixelCountDrops[d].delta < this.rowPixelCountRises[r].delta) {
//						System.out.println("Eliminated drop of " + this.rowPixelCountDrops[d].delta + " at " + this.rowPixelCountDrops[d].row + " as adjacent to rise of " + this.rowPixelCountRises[r].delta + " at " + this.rowPixelCountRises[r].row);
//						this.rowPixelCountDrops[d] = null;
//						break;
//					}
//				}
//				if (this.rowPixelCountDrops[d] != null)
//					rowPixelCountDrops.add(this.rowPixelCountDrops[d]);
//			}
//			if (rowPixelCountDrops.size() < this.rowPixelCountDrops.length)
//				this.rowPixelCountDrops = ((HistogramRiseDrop[]) rowPixelCountDrops.toArray(new HistogramRiseDrop[rowPixelCountDrops.size()]));
//		}
//		
//		void checkForLastLine() {
//			
//			//	if we have only one distinctive line, use x-height to see if we have one or two lines (x-height should never be smaller than 1/3 of line height ...)
//			if ((this.highRowPixelCenters.length < 2) || (this.lowRowPixelCenters.length < 2))
//				this.checkForLastLineXHeight();
//			
//			//	otherwise still check if we have a short last line (based on average line height, though, which should be more reliable)
//			else this.checkForLastLineLineHeight();
//		}
//		
//		private void checkForLastLineXHeight() {
//			System.out.println("x-height is " + this.avgHighRowPixelWidth + " from " + Arrays.toString(this.highRowPixelWidths));
//			
//			//	this x-height looks plausible (usual x-height is 40-50% of line height)
//			if ((this.avgHighRowPixelWidth * 3) > this.block.getHeight()) {
//				System.out.println(" ==> looking like a single line");
//				return;
//			}
//			
//			//	cut row pixel occupancy histogram in half (split off last line), and re-compute average
//			int[] lastLineRowPixelCount = new int[this.rowPixelCount.length / 2];
//			System.arraycopy(this.rowPixelCount, (this.rowPixelCount.length - lastLineRowPixelCount.length), lastLineRowPixelCount, 0, lastLineRowPixelCount.length);
//			int avgLastLineRowPixelCount = getHistogramAverage(lastLineRowPixelCount);
//			System.out.println("last line row pixel average is " + avgLastLineRowPixelCount);
//			
//			//	compute rises and drops of lower half (to allow for local averages to kick in)
////			int[] lastLineRowPixelCountRises = getHistgramMaxRises(lastLineRowPixelCount, avgLastLineRowPixelCount);
//			HistogramRiseDrop[] lastLineRowPixelCountRises = getHistgramMaxRises(lastLineRowPixelCount, avgLastLineRowPixelCount);
////			System.out.println("Row height from x-heights: " + getDistanceAverage(rowPixelCountRises) + " (min " + getDistanceMin(rowPixelCountRises) + ", max " + getDistanceMax(rowPixelCountRises) + ")");
////			int[] lastLineRowPixelCountDrops = getHistgramMaxDrops(lastLineRowPixelCount, avgLastLineRowPixelCount);
//			HistogramRiseDrop[] lastLineRowPixelCountDrops = getHistgramMaxDrops(lastLineRowPixelCount, avgLastLineRowPixelCount);
////			System.out.println("Row height from baselines: " + getDistanceAverage(rowPixelCountDrops) + " (min " + getDistanceMin(rowPixelCountDrops) + ", max " + getDistanceMax(rowPixelCountDrops) + ")");
//			System.out.println("last line row pixel rises are " + Arrays.toString(lastLineRowPixelCountRises));
//			System.out.println("last line row pixel drops are " + Arrays.toString(lastLineRowPixelCountDrops));
//			
//			//	compute center and x-height of second line
//			int[] lastLineHighRowPixelCenters = getHighCenters(lastLineRowPixelCountRises, lastLineRowPixelCountDrops);
////			System.out.println("Row height from line centers: " + getDistanceAverage(highRowPixelCenters) + " (min " + getDistanceMin(highRowPixelCenters) + ", max " + getDistanceMax(highRowPixelCenters) + ")");
//			int[] lastLineHighRowPixelWidths = getHighWidths(lastLineRowPixelCountRises, lastLineRowPixelCountDrops);
//			int avgLastLineHighRowPixelWidth = getHistogramAverage(lastLineHighRowPixelWidths);
//			System.out.println("last line x-height is " + avgLastLineHighRowPixelWidth + " from " + Arrays.toString(lastLineHighRowPixelWidths));
//			
//			//	x-height differs more than 20% from block average, too unreliable
//			if (((avgLastLineHighRowPixelWidth * 10) < (this.avgHighRowPixelWidth * 8)) && ((this.avgHighRowPixelWidth * 8) < (avgLastLineHighRowPixelWidth * 8))) {
//				System.out.println(" ==> x-height of last line too far off");
//				return;
//			}
//			
//			//	append rises and drops for last line
//			for (int r = 0; r < lastLineRowPixelCountRises.length; r++)
////				lastLineRowPixelCountRises[r] += (this.rowPixelCount.length - lastLineRowPixelCount.length);
//				lastLineRowPixelCountRises[r] = new HistogramRiseDrop((lastLineRowPixelCountRises[r].row + this.rowPixelCount.length - lastLineRowPixelCount.length), lastLineRowPixelCountRises[r].delta);
//			this.rowPixelCountRises = merge(this.rowPixelCountRises, lastLineRowPixelCountRises);
//			for (int d = 0; d < lastLineRowPixelCountDrops.length; d++)
////				lastLineRowPixelCountDrops[d] += (this.rowPixelCount.length - lastLineRowPixelCount.length);
//				lastLineRowPixelCountDrops[d] = new HistogramRiseDrop((lastLineRowPixelCountDrops[d].row + this.rowPixelCount.length - lastLineRowPixelCount.length), lastLineRowPixelCountDrops[d].delta);
//			this.rowPixelCountDrops = merge(this.rowPixelCountDrops, lastLineRowPixelCountDrops);
//			this.highRowPixelCenters = getHighCenters(this.rowPixelCountRises, this.rowPixelCountDrops);
//			this.lowRowPixelCenters = getLowCenters(this.rowPixelCountRises, this.rowPixelCountDrops);
//			
//			//	re-compute x-heights and average
//			this.highRowPixelWidths = getHighWidths(this.rowPixelCountRises, this.rowPixelCountDrops);
//			this.avgHighRowPixelWidth = getHistogramAverage(this.highRowPixelWidths);
//			
//			//	TODO_elsewhere figure out what to do if x-height of last line too far off
//		}
//		
//		private void checkForLastLineLineHeight() {
//			
//			//	compute average line height (from both line centers and line boundaries)
//			float avgLineCenterDist = getDistanceAverage(this.highRowPixelCenters);
//			float avgLineGapDist = getDistanceAverage(this.lowRowPixelCenters);
//			float avgLineHeight = ((avgLineCenterDist + avgLineGapDist) / 2);
//			System.out.println("line height is " + avgLineHeight + " (line centers: min " + getDistanceMin(this.highRowPixelCenters) + ", max " + getDistanceMax(this.highRowPixelCenters) + ", avg " + avgLineCenterDist + ", line gaps: min " + getDistanceMin(this.lowRowPixelCenters) + ", max " + getDistanceMax(this.lowRowPixelCenters) + ", avg " + avgLineGapDist + ")");
//			System.out.println("x-height is " + this.avgHighRowPixelWidth + " from " + Arrays.toString(this.highRowPixelWidths));
//			
//			//	check distance from block bottom
//			int lineCenterBlockBottomDist = (this.rowPixelCount.length - this.highRowPixelCenters[this.highRowPixelCenters.length-1]);
//			System.out.println("last line center is " + lineCenterBlockBottomDist + " from block bottom");
//			int lineGapBlockBottomDist = (this.rowPixelCount.length - this.lowRowPixelCenters[this.lowRowPixelCenters.length-1]);
//			System.out.println("last line boundary is " + lineGapBlockBottomDist + " from block bottom");
//			
//			//	are we close enough to block bottom, or are we missing a line?
//			if ((lineCenterBlockBottomDist < avgLineHeight) && ((lineGapBlockBottomDist * 2) < (avgLineHeight * 3))) {
//				System.out.println(" ==> looks like we have all the line boundaries");
//				return;
//			}
//			
//			//	extrapolate and merge last rise and drop
////			int[] lastLineRowPixelCountRises = {Math.round(this.rowPixelCountRises[this.rowPixelCountRises.length-1] + avgLineHeight)};
//			HistogramRiseDrop[] lastLineRowPixelCountRises = {new HistogramRiseDrop(Math.round(this.rowPixelCountRises[this.rowPixelCountRises.length-1].row + avgLineHeight), 0)};
//			this.rowPixelCountRises = merge(this.rowPixelCountRises, lastLineRowPixelCountRises);
////			int[] lastLineRowPixelCountDrops = {Math.round(this.rowPixelCountDrops[this.rowPixelCountDrops.length-1] + avgLineHeight)};
//			HistogramRiseDrop[] lastLineRowPixelCountDrops = {new HistogramRiseDrop(Math.round(this.rowPixelCountDrops[this.rowPixelCountDrops.length-1].row + avgLineHeight), 0)};
//			this.rowPixelCountDrops = merge(this.rowPixelCountDrops, lastLineRowPixelCountDrops);
//			
//			//	re-compute line centers and boundaries
//			this.highRowPixelCenters = getHighCenters(this.rowPixelCountRises, this.rowPixelCountDrops);
//			this.lowRowPixelCenters = getLowCenters(this.rowPixelCountRises, this.rowPixelCountDrops);
//			
//			//	re-compute individual and average x-heights
//			this.highRowPixelWidths = getHighWidths(this.rowPixelCountRises, this.rowPixelCountDrops);
//			this.avgHighRowPixelWidth = getHistogramAverage(this.highRowPixelWidths);
//		}
//		
//		ImagePartRectangle[] getBlockLines() {
//			
//			//	use line boundaries to split up block
//			ImagePartRectangle[] lines = new ImagePartRectangle[this.lowRowPixelCenters.length+1];
//			for (int l = 0; l <= this.lowRowPixelCenters.length; l++) {
//				int lineTop = ((l == 0) ? 0 : this.lowRowPixelCenters[l-1]);
//				int lineBottom = ((l == this.lowRowPixelCenters.length) ? block.getHeight() : this.lowRowPixelCenters[l]);
//				lines[l] = block.getSubRectangle(block.getLeftCol(), block.getRightCol(), (lineTop + block.getTopRow()), (lineBottom + block.getTopRow()));
//				Imaging.narrowTopAndBottom(lines[l]);
//				Imaging.narrowLeftAndRight(lines[l]);
//				System.out.println("line: " + lines[l].getId());
//			}
//			
//			//	finally ...
//			return lines;
//		}
//	}
//	
//	private static int[] getRowBrightness(ImagePartRectangle block, byte[][] brightness) {
//		int[] rowBrightness = new int[block.getHeight()];
//		Arrays.fill(rowBrightness, 0);
//		for (int c = block.getLeftCol(); c < block.getRightCol(); c++) {
//			for (int r = block.getTopRow(); r < block.getBottomRow(); r++)
//				rowBrightness[r - block.getTopRow()] += brightness[c][r];
//		}
//		for (int r = 0; r < rowBrightness.length; r++)
//			rowBrightness[r] /= block.getWidth();
//		return rowBrightness;
//	}
//	
//	private static int[] getRowPixelCount(ImagePartRectangle block, byte[][] brightness) {
//		int[] rowPixelCount = new int[block.getHeight()];
//		Arrays.fill(rowPixelCount, 0);
//		for (int c = block.getLeftCol(); c < block.getRightCol(); c++)
//			for (int r = block.getTopRow(); r < block.getBottomRow(); r++) {
//				if (brightness[c][r] < 120)
//					rowPixelCount[r - block.getTopRow()]++;
//			}
//		return rowPixelCount;
//	}
//	
//	private static int getHistogramAverage(int[] histogram) {
//		int histogramSum = 0;
//		for (int h = 0; h < histogram.length; h++)
//			histogramSum += histogram[h];
//		return ((histogramSum + (histogram.length / 2)) / histogram.length);
//	}
//	
////	private static int[] getHistgramMaxRises(int[] histogram, int histogramAvg) {
//	private static HistogramRiseDrop[] getHistgramMaxRises(int[] histogram, int histogramAvg) {
//		List maxRiseRowList = new ArrayList();
//		for (int r = 1; r < histogram.length; r++) {
//			if ((histogram[r] >= histogramAvg) && (histogram[r-1] < histogramAvg)) {
//				int maxRise = 0;
//				int maxRiseRow = -1;
//				for (int rr = Math.max((r-1), 1); rr < Math.min((r+2), histogram.length); rr++) {
//					int rise = (histogram[rr] - histogram[rr-1]);
//					System.out.println(rr + ": Got rise of " + rise + " from " + histogram[rr-1] + " to " + histogram[rr] + " through average of " + histogramAvg);
//					if (rise > maxRise) {
//						maxRise = rise;
//						maxRiseRow = rr;
//					}
//				}
//				if (maxRiseRow != -1) {
////					maxRiseRowList.add(new Integer(maxRiseRow));
//					maxRiseRowList.add(new HistogramRiseDrop(maxRiseRow, maxRise));
//					System.out.println(" ==> Got maximum rise of " + maxRise + " at " + maxRiseRow);
//				}
//			}
//		}
////		int[] maxRiseRows = new int[maxRiseRowList.size()];
////		for (int r = 0; r < maxRiseRowList.size(); r++)
////			maxRiseRows[r] = ((Integer) maxRiseRowList.get(r)).intValue();
////		return maxRiseRows;
//		return ((HistogramRiseDrop[]) maxRiseRowList.toArray(new HistogramRiseDrop[maxRiseRowList.size()]));
//	}
//	
////	private static int[] getHistgramMaxDrops(int[] histogram, int histogramAvg) {
//	private static HistogramRiseDrop[] getHistgramMaxDrops(int[] histogram, int histogramAvg) {
//		List maxDropRowList = new ArrayList();
//		for (int r = 1; r < histogram.length; r++) {
//			if ((histogram[r] < histogramAvg) && (histogram[r-1] >= histogramAvg)) {
//				int maxDrop = 0;
//				int maxDropRow = -1;
//				for (int dr = Math.max((r-1), 1); dr < Math.min((r+2), histogram.length); dr++) {
//					int drop = (histogram[dr-1] - histogram[dr]);
//					System.out.println(dr + ": Got drop of " + drop + " from " + histogram[dr-1] + " to " + histogram[dr] + " through average of " + histogramAvg);
//					if (drop > maxDrop) {
//						maxDrop = drop;
//						maxDropRow = (dr-1);
//					}
//				}
//				if (maxDropRow != -1) {
////					maxDropRowList.add(new Integer(maxDropRow));
//					maxDropRowList.add(new HistogramRiseDrop(maxDropRow, maxDrop));
//					System.out.println(" ==> Got maximum drop of " + maxDrop + " at " + maxDropRow);
//				}
//			}
//		}
////		int[] maxDropRows = new int[maxDropRowList.size()];
////		for (int r = 0; r < maxDropRowList.size(); r++)
////			maxDropRows[r] = ((Integer) maxDropRowList.get(r)).intValue();
////		return maxDropRows;
//		return ((HistogramRiseDrop[]) maxDropRowList.toArray(new HistogramRiseDrop[maxDropRowList.size()]));
//	}
//	
//	private static class HistogramRiseDrop {
//		final int row;
//		final int delta;
//		HistogramRiseDrop(int row, int delta) {
//			this.row = row;
//			this.delta = delta;
//		}
//	}
//	
////	private static int[] getHighCenters(int[] rises, int[] drops) {
//	private static int[] getHighCenters(HistogramRiseDrop[] rises, HistogramRiseDrop[] drops) {
//		ArrayList highCenterList = new ArrayList();
//		int lastDrop = -1;
//		for (int r = 0; r < rises.length; r++) {
//			for (int d = 0; d < drops.length; d++)
////				if (drops[d] > rises[r]) {
//				if (drops[d].row > rises[r].row) {
////					if (drops[d] == lastDrop)
//					if (drops[d].row == lastDrop)
//						highCenterList.remove(highCenterList.size()-1);
////					lastDrop = drops[d];
//					lastDrop = drops[d].row;
////					highCenterList.add(new Integer((rises[r] + drops[d] + 1) / 2));
//					highCenterList.add(new Integer((rises[r].row + drops[d].row + 1) / 2));
//					break;
//				}
//		}
//		int[] highCenters = new int[highCenterList.size()];
//		for (int c = 0; c < highCenterList.size(); c++)
//			highCenters[c] = ((Integer) highCenterList.get(c)).intValue();
//		return highCenters;
//	}
//	
////	private static int[] getHighWidths(int[] rises, int[] drops) {
//	private static int[] getHighWidths(HistogramRiseDrop[] rises, HistogramRiseDrop[] drops) {
//		ArrayList highWidthList = new ArrayList();
//		int lastDrop = -1;
//		for (int r = 0; r < rises.length; r++) {
//			for (int d = 0; d < drops.length; d++)
////				if (drops[d] > rises[r]) {
//				if (drops[d].row > rises[r].row) {
////					if (drops[d] == lastDrop)
//					if (drops[d].row == lastDrop)
//						highWidthList.remove(highWidthList.size()-1);
////					lastDrop = drops[d];
//					lastDrop = drops[d].row;
////					highWidthList.add(new Integer(drops[d] - rises[r]));
//					highWidthList.add(new Integer(drops[d].row - rises[r].row));
//					break;
//				}
//		}
//		int[] highWidths = new int[highWidthList.size()];
//		for (int c = 0; c < highWidthList.size(); c++)
//			highWidths[c] = ((Integer) highWidthList.get(c)).intValue();
//		return highWidths;
//	}
//	
////	private static int[] getLowCenters(int[] rises, int[] drops) {
//	private static int[] getLowCenters(HistogramRiseDrop[] rises, HistogramRiseDrop[] drops) {
//		ArrayList lowCenterList = new ArrayList();
//		int lastRise = -1;
//		for (int d = 0; d < drops.length; d++) {
//			for (int r = 0; r < rises.length; r++)
////				if (rises[r] > drops[d]) {
//				if (rises[r].row > drops[d].row) {
////					if (rises[r] == lastRise)
//					if (rises[r].row == lastRise)
//						lowCenterList.remove(lowCenterList.size()-1);
////					lastRise = rises[r];
//					lastRise = rises[r].row;
////					lowCenterList.add(new Integer((drops[d] + rises[r] + 1) / 2));
//					lowCenterList.add(new Integer((drops[d].row + rises[r].row + 1) / 2));
//					break;
//				}
//		}
//		int[] lowCenters = new int[lowCenterList.size()];
//		for (int c = 0; c < lowCenterList.size(); c++)
//			lowCenters[c] = ((Integer) lowCenterList.get(c)).intValue();
//		return lowCenters;
//	}
//	
//	private static int getDistanceMin(int[] rows) {
//		int rowDistanceMin = Integer.MAX_VALUE;
//		for (int r = 1; r < rows.length; r++)
//			rowDistanceMin = Math.min((rows[r] - rows[r-1]), rowDistanceMin);
//		return rowDistanceMin;
//	}
//	
//	private static int getDistanceMax(int[] rows) {
//		int rowDistanceMax = 0;
//		for (int r = 1; r < rows.length; r++)
//			rowDistanceMax = Math.max((rows[r] - rows[r-1]), rowDistanceMax);
//		return rowDistanceMax;
//	}
//	
//	private static float getDistanceAverage(int[] rows) {
//		float rowDistanceSum = 0;
//		for (int r = 1; r < rows.length; r++)
//			rowDistanceSum += (rows[r] - rows[r-1]);
//		return (rowDistanceSum / (rows.length - 1));
//	}
//	
//	private static void smoothHistogram(int[] histogram, int radius, int zero) {
//		int[] sHistogram = new int[histogram.length + (radius * 2)];
//		Arrays.fill(sHistogram, 0, radius, zero);
//		System.arraycopy(histogram, 0, sHistogram, radius, histogram.length);
//		Arrays.fill(sHistogram, (histogram.length + radius), sHistogram.length, zero);
//		int hSum = 0;
//		for (int h = 0; h < sHistogram.length; h++) {
//			hSum += sHistogram[h];
//			if (h >= ((radius * 2) + 1))
//				hSum -= sHistogram[h - ((radius * 2) + 1)];
//			if (h >= (2 * radius))
//				histogram[h - (2 * radius)] = ((hSum + radius) / ((radius * 2) + 1));
//		}
//	}
	
	private static void showImage(BufferedImage bi, int[] rowPixelCount, int avgRowPixelCount, int[] rowPixelCountHighlights, int[] rowBrightness, int avgRowBrightness, int[] rowBrightnessHighlighs) {
		BufferedImage sBi = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics sGr = sBi.createGraphics();
		sGr.drawImage(bi, 0, 0, null);
		if (rowPixelCount != null)
			drawHistogram(sBi, sGr, rowPixelCount, avgRowPixelCount, rowPixelCountHighlights, new Color(255, 0, 0, 86), true);
		if (rowBrightness != null)
			drawHistogram(sBi, sGr, rowBrightness, avgRowBrightness, rowBrightnessHighlighs, new Color(0, 0, 255, 86), (rowPixelCount == null));
		ImageDisplayDialog idd = new ImageDisplayDialog("");
		idd.setSize((bi.getWidth() + 100), (bi.getHeight() + 100));
		idd.setLocationRelativeTo(null);
		idd.addImage(sBi, "");
		idd.setVisible(true);
	}
	
	private static void drawHistogram(BufferedImage bi, Graphics gr, int[] histogram, int histogramAvg, int[] histogramHighlights, Color color, boolean left) {
		gr.setColor(color);
		for (int r = 0; r < Math.min(histogram.length, bi.getHeight()); r++) {
			int start;
			int end;
			if (left) {
				start = 0;
				end = histogram[r];
			}
			else {
				start = (bi.getWidth() - histogram[r]);
				end = bi.getWidth();
			}
			if (start < end)
				gr.drawLine(start, r, (end-1), r); // _both_ coordinates are inclusive in drawing ...
		}
		gr.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 255));
		if (left)
			gr.drawLine(histogramAvg, 0, histogramAvg, bi.getHeight());
		else gr.drawLine((bi.getWidth() - histogramAvg), 0, (bi.getWidth() - histogramAvg), bi.getHeight());
		if (histogramHighlights != null)
			for (int h = 0; h < histogramHighlights.length; h++) {
				if (left)
					gr.drawLine(0, histogramHighlights[h], (bi.getWidth() / 2), histogramHighlights[h]);
//				else gr.drawLine((bi.getWidth() / 2), histogramHighlights[h], bi.getWidth(), histogramHighlights[h]);
				else gr.drawLine((bi.getWidth() / 10), histogramHighlights[h], bi.getWidth(), histogramHighlights[h]);
			}
	}
	
	//	!!! TEST ONLY !!!
	public static void main(String[] args) throws Exception {
//		int dpi = 150;
//		BufferedImage bi = ImageIO.read(new File("E:/Testdaten/PdfExtract/OcrTestBlock.png"));
		
		int dpi = 200;
//		BufferedImage bi = ImageIO.read(new File("E:/Testdaten/PdfExtract/OcrTestBlock2.png")); // splits OK to 8 lines
//		BufferedImage bi = ImageIO.read(new File("E:/Testdaten/PdfExtract/OcrTestBlock3.png")); // splits OK to 6 lines
//		BufferedImage bi = ImageIO.read(new File("E:/Testdaten/PdfExtract/OcrTestBlock4.png")); // TODOne splits to 6 lines, but should split to 5 ==> no more ...
//		BufferedImage bi = ImageIO.read(new File("E:/Testdaten/PdfExtract/OcrTestBlock5.png")); // TODOne 2 lines only (cut for test purposes) ==> extrapolates nicely based on x-height
		BufferedImage bi = ImageIO.read(new File("E:/Testdaten/PdfExtract/OcrTestBlock6.png")); // TODOne 2 lines only (cut for test purposes) ==> extrapolates nicely based on x-height
		
		AnalysisImage bai = Imaging.wrapImage(bi, null);
//		Imaging.correctPageRotation(bai, dpi, 0.1, Imaging.ADJUST_MODE_SQUARE_ROOT);
		byte[][] baiBrightness = bai.getBrightness();
		for (int c = 0; c < baiBrightness.length; c++)
			for (int r = 0; r < baiBrightness[c].length; r++) {
				if (baiBrightness[c][r] > 120)
					baiBrightness[c][r] = ((byte) 127);
			}
		ImagePartRectangle bRect = Imaging.getContentBox(bai);
//		BlockData bData = analyzeBlock(bRect, dpi, ProgressMonitor.dummy);
//		ImagePartRectangle[] bLines = bData.getBlockLines();
		Line[] bLines = PageImageAnalysis.findBlockLines(bRect, dpi, ProgressMonitor.dummy);
		
		//	compute line offsets
		int[] bLineOffsets = new int[bLines.length];
		bLineOffsets[0] = 0;
		for (int l = 1; l < bLines.length; l++) {
//			bLineOffsets[l] = 0;
//			if ((bLines[l].getTopRow() - bLines[l-1].getBottomRow()) >= minLineDistance)
			if ((bLines[l].bounds.getTopRow() - bLines[l-1].bounds.getBottomRow()) >= minLineDistance)
				bLineOffsets[l] = bLineOffsets[l-1];
//			else bLineOffsets[l] = bLineOffsets[l-1] + (minLineDistance - (bLines[l].getTopRow() - bLines[l-1].getBottomRow()));
			else bLineOffsets[l] = bLineOffsets[l-1] + (minLineDistance - (bLines[l].bounds.getTopRow() - bLines[l-1].bounds.getBottomRow()));
		}
		
		//	compute region coloring
		System.out.println("Computing region coloring");
		int[][] bRegionColors = Imaging.getRegionColoring(bai, Imaging.computeAverageBrightness(bai), false);
		int bMaxRegionColor = getMaxRegionColor(bRegionColors);
		
		//	compute presence of each region color in each line
		System.out.println("Computing region color distribution");
		int[] bRegionColorFrequencies = new int[bMaxRegionColor + 1];
		Arrays.fill(bRegionColorFrequencies, 0);
		int[] bRegionColorMinRows = new int[bMaxRegionColor + 1];
		Arrays.fill(bRegionColorMinRows, Integer.MAX_VALUE);
		int[] bRegionColorMaxRows = new int[bMaxRegionColor + 1];
		Arrays.fill(bRegionColorMaxRows, 0);
		for (int c = 0; c < bRegionColors.length; c++)
			for (int r = 0; r < bRegionColors[c].length; r++) {
				bRegionColorFrequencies[bRegionColors[c][r]]++;
				bRegionColorMinRows[bRegionColors[c][r]] = Math.min(bRegionColorMinRows[bRegionColors[c][r]], r);
				bRegionColorMaxRows[bRegionColors[c][r]] = Math.max(bRegionColorMaxRows[bRegionColors[c][r]], r);
			}
		int[][] bLineRegionColorFrequencies = new int[bLines.length][];
		for (int l = 0; l < bLines.length; l++) {
			bLineRegionColorFrequencies[l] = new int[bMaxRegionColor + 1];
			Arrays.fill(bLineRegionColorFrequencies[l], 0);
//			for (int c = bLines[l].getLeftCol(); c < bLines[l].getRightCol(); c++) {
			for (int c = bLines[l].bounds.getLeftCol(); c < bLines[l].bounds.getRightCol(); c++) {
//				for (int r = bLines[l].getTopRow(); r < bLines[l].getBottomRow(); r++)
				for (int r = bLines[l].bounds.getTopRow(); r < bLines[l].bounds.getBottomRow(); r++)
					bLineRegionColorFrequencies[l][bRegionColors[c][r]]++;
			}
		}
		
		//	generate block image, with line offsets
		System.out.println("Generating expanded block image");
		System.out.println(" - line offsets are " + Arrays.toString(bLineOffsets));
		BufferedImage ocrBi = new BufferedImage(bRect.getWidth(), (bRect.getHeight() + (bLineOffsets[bLineOffsets.length-1])), BufferedImage.TYPE_BYTE_GRAY);
		Graphics ocrg = ocrBi.createGraphics();
		ocrg.setColor(Color.WHITE);
		ocrg.fillRect(0, 0, ocrBi.getWidth(), ocrBi.getHeight());
		for (int l = 0; l < bLines.length; l++) {
			System.out.println(" - adding line " + bLines[l].toString());
//			ocrg.drawImage(bi.getSubimage(bLines[l].getLeftCol(), bLines[l].getTopRow(), bLines[l].getWidth(), bLines[l].getHeight()), (bLines[l].getLeftCol() - bRect.getLeftCol()), (bLines[l].getTopRow() - bRect.getTopRow() + bLineOffsets[l]), null);
			ocrg.drawImage(bi.getSubimage(bLines[l].bounds.getLeftCol(), bLines[l].bounds.getTopRow(), bLines[l].bounds.getWidth(), bLines[l].bounds.getHeight()), (bLines[l].bounds.getLeftCol() - bRect.getLeftCol()), (bLines[l].bounds.getTopRow() - bRect.getTopRow() + bLineOffsets[l]), null);
//			for (int c = bLines[l].getLeftCol(); c < bLines[l].getRightCol(); c++)
			for (int c = bLines[l].bounds.getLeftCol(); c < bLines[l].bounds.getRightCol(); c++)
//				for (int r = bLines[l].getTopRow(); r < bLines[l].getBottomRow(); r++) {
				for (int r = bLines[l].bounds.getTopRow(); r < bLines[l].bounds.getBottomRow(); r++) {
					int bLineRegionColorFrequency = bLineRegionColorFrequencies[l][bRegionColors[c][r]];
					
					//	check upward shift of lower descender tips if region color does not occur below line center
//					if ((l != 0) && (bRegionColorMaxRows[bRegionColors[c][r]] < ((bLines[l].getTopRow() + bLines[l].getBottomRow()) / 2)) && (bLineRegionColorFrequencies[l-1][bRegionColors[c][r]] > (3 * bLineRegionColorFrequency))) {
					if ((l != 0) && (bRegionColorMaxRows[bRegionColors[c][r]] < ((bLines[l].bounds.getTopRow() + bLines[l].bounds.getBottomRow()) / 2)) && (bLineRegionColorFrequencies[l-1][bRegionColors[c][r]] > (3 * bLineRegionColorFrequency))) {
						int rgb = ocrBi.getRGB((c - bRect.getLeftCol()), (r - bRect.getTopRow() + bLineOffsets[l]));
						ocrBi.setRGB((c - bRect.getLeftCol()), (r - bRect.getTopRow() + bLineOffsets[l-1]), rgb);
						ocrBi.setRGB((c - bRect.getLeftCol()), (r - bRect.getTopRow() + bLineOffsets[l]), whiteRgb);
					}
					
					//	check downward shift of upper ascender tips if region color does not occur above line center
//					if (((l+1) < bLines.length) && bRegionColorMinRows[bRegionColors[c][r]] > ((bLines[l].getTopRow() + bLines[l].getBottomRow()) / 2) && (bLineRegionColorFrequencies[l+1][bRegionColors[c][r]] > (3 * bLineRegionColorFrequency))) {
					if (((l+1) < bLines.length) && bRegionColorMinRows[bRegionColors[c][r]] > ((bLines[l].bounds.getTopRow() + bLines[l].bounds.getBottomRow()) / 2) && (bLineRegionColorFrequencies[l+1][bRegionColors[c][r]] > (3 * bLineRegionColorFrequency))) {
						int rgb = ocrBi.getRGB((c - bRect.getLeftCol()), (r - bRect.getTopRow() + bLineOffsets[l]));
						ocrBi.setRGB((c - bRect.getLeftCol()), (r - bRect.getTopRow() + bLineOffsets[l+1]), rgb);
						ocrBi.setRGB((c - bRect.getLeftCol()), (r - bRect.getTopRow() + bLineOffsets[l]), whiteRgb);
					}
				}
		}
//		
//		//	generate block image, with line offsets
//		BufferedImage ocrBi = new BufferedImage(bRect.getWidth(), (bRect.getHeight() + (bLineOffsets[bLineOffsets.length-1])), BufferedImage.TYPE_BYTE_GRAY);
//		Graphics ocrg = ocrBi.createGraphics();
//		ocrg.setColor(Color.WHITE);
//		ocrg.fillRect(0, 0, ocrBi.getWidth(), ocrBi.getHeight());
//		for (int l = 0; l < bLines.length; l++)
//			ocrg.drawImage(bi.getSubimage(bLines[l].getLeftCol(), bLines[l].getTopRow(), bLines[l].getWidth(), bLines[l].getHeight()), (bLines[l].getLeftCol() - bRect.getLeftCol()), (bLines[l].getTopRow() - bRect.getTopRow() + bLineOffsets[l]), null);
//		
//		int[] rowPixelCount = getRowPixelCount(bRect, baiBrightness);
//		int[] rowBrightness = getRowBrightness(bRect, baiBrightness);
////		BlockData bdi = new BlockData(bRect, rowBrightness, rowPixelCount);
////		bdi.checkForLastLine();
//		showImage(ocrBi,
//				expandHistogram(bData.rowPixelCount, bLines, bLineOffsets, 0),
//				bData.avgRowPixelCount,
//				expandHistogramHighlighs(getRows(merge(bData.rowPixelCountRises, bData.rowPixelCountDrops)), bLines, bLineOffsets),
//				expandHistogram(bData.rowBrightness, bLines, bLineOffsets, 127),
//				bData.avgRowBrightness,
////				expandHistogramHighlighs(merge(bdi.rowBrightnessRises, bdi.rowBrightnessDrops), bLines, bLineOffsets)
////				expandHistogramHighlighs(getLowCenters(bdi.rowPixelCountRises, bdi.rowPixelCountDrops), bLines, bLineOffsets)
////				expandHistogramHighlighs(getHighCenters(bdi.rowPixelCountRises, bdi.rowPixelCountDrops), bLines, bLineOffsets)
//				expandHistogramHighlighs(merge(bData.highRowPixelCenters, bData.lowRowPixelCenters), bLines, bLineOffsets)
//			);
	}
//	
//	private static int[] getRows(HistogramRiseDrop[] histogramHighlights) {
//		int[] histogramHighlightRows = new int[histogramHighlights.length];
//		for (int h = 0; h < histogramHighlights.length; h++)
//			histogramHighlightRows[h] = histogramHighlights[h].row;
//		return histogramHighlightRows;
//	}
//	
//	private static int[] expandHistogram(int[] histogram, ImagePartRectangle[] lines, int[] lineOffsets, int fill) {
//		int[] eHistogram = new int[histogram.length + lineOffsets[lineOffsets.length-1]];
//		Arrays.fill(eHistogram, fill);
//		for (int l = 0; l < lines.length; l++)
//			System.arraycopy(histogram, (lines[l].getTopRow() - lines[0].getTopRow()), eHistogram, (lines[l].getTopRow() - lines[0].getTopRow() + lineOffsets[l]), lines[l].getHeight());
//		return eHistogram;
//	}
//	
//	private static int[] expandHistogramHighlighs(int[] histogramHighlights, ImagePartRectangle[] lines, int[] lineOffsets) {
//		int[] eHistogramHighlights = new int[histogramHighlights.length];
//		Arrays.fill(eHistogramHighlights, Integer.MIN_VALUE);
////		System.out.println("Expanding histogram highlights " + Arrays.toString(histogramHighlights));
//		for (int h = 0; h < histogramHighlights.length; h++) {
//			for (int l = (lines.length-1); l >= 0; l--)
//				if (histogramHighlights[h] >= (lines[l].getTopRow() - lines[0].getTopRow() - minLineDistance)) {
//					eHistogramHighlights[h] = (histogramHighlights[h] + lineOffsets[l]);
//					break;
//				}
//			if (eHistogramHighlights[h] == Integer.MIN_VALUE)
//				eHistogramHighlights[h] = histogramHighlights[h];
//		}
////		System.out.println(" ==> " + Arrays.toString(eHistogramHighlights));
//		return eHistogramHighlights;
//	}
//	
//	private static int[] merge(int[] h1, int[] h2) {
////		System.out.println("Merging " + Arrays.toString(h1) + " and " + Arrays.toString(h2));
//		int[] h = new int[h1.length + h2.length];
//		System.arraycopy(h1, 0, h, 0, h1.length);
//		System.arraycopy(h2, 0, h, h1.length, h2.length);
////		System.out.println(" ==> " + Arrays.toString(h));
//		return h;
//	}
//	
//	private static HistogramRiseDrop[] merge(HistogramRiseDrop[] h1, HistogramRiseDrop[] h2) {
////		System.out.println("Merging " + Arrays.toString(h1) + " and " + Arrays.toString(h2));
//		HistogramRiseDrop[] h = new HistogramRiseDrop[h1.length + h2.length];
//		System.arraycopy(h1, 0, h, 0, h1.length);
//		System.arraycopy(h2, 0, h, h1.length, h2.length);
////		System.out.println(" ==> " + Arrays.toString(h));
//		return h;
//	}
}
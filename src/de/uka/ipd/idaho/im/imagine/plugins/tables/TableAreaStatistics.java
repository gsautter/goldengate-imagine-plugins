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
package de.uka.ipd.idaho.im.imagine.plugins.tables;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.im.ImLayoutObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImSupplement;
import de.uka.ipd.idaho.im.ImSupplement.Graphics;
import de.uka.ipd.idaho.im.ImSupplement.Graphics.Path;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.analysis.Imaging;
import de.uka.ipd.idaho.im.analysis.Imaging.AnalysisImage;
import de.uka.ipd.idaho.im.util.ImUtils;


/**
 * Statistics describing the distribution of words in a table area, i.e.:
 * <br/>- pixel rows occupied with words
 * <br/>- occupation of pixel rows with words
 * <br/>- pixel columns occupied with words
 * <br/>- occupation of pixel columns with words
 * <br/>- lows and gaps in occupation of pixel columns with words, to help
 *        detect column gaps
 * 
 * @author sautter
 */
class TableAreaStatistics {
	private static final float defaultNormSpaceWidth = 0.33f;
	
	final int pageImageDpi;
	
	final int grCount;
	final BoundingBox grBounds;
	final BoundingBox inWordGrBounds;
	final BufferedImage grImage;
	final byte[][] grBrightness;
	final int[] grRowOccupations;
	final int[] grColOccupations;
	
	final ImWord[] words;
	final BoundingBox wordBounds;
	final int avgWordHeight;
	final float normSpaceWidth;
	final float avgSpaceWidth;
	final int wordsInGraphics;
	
	final int[] rowOccupations;
	final int wordPixelRows;
	
	final int[] colOccupations;
	final int wordPixelCols;
	final int[] bColOccupations;
	final int bWordPixelCols;
	
	TableAreaStatistics(ImPage page, ImWord[] words) {
		this(page, null, words, defaultNormSpaceWidth); // rough estimate for word bridging, still way below usual relative width of column gap 
	}
	
	TableAreaStatistics(ImPage page, BoundingBox bounds, ImWord[] words) {
		this(page, bounds, words, defaultNormSpaceWidth); // rough estimate for word bridging, still way below usual relative width of column gap 
	}
	
	TableAreaStatistics(ImPage page, ImWord[] words, float normSpaceWidth) {
		this(page, null, words, normSpaceWidth);
	}
	
	TableAreaStatistics(ImPage page, BoundingBox bounds, ImWord[] words, float normSpaceWidth) {
		this.words = new ImWord[words.length];
		System.arraycopy(words, 0, this.words, 0, words.length);
		ImUtils.sortLeftRightTopDown(this.words);
		this.normSpaceWidth = normSpaceWidth;
		
		//	wrap bounds around words if none given
		this.wordBounds = ((bounds == null) ? ImLayoutObject.getAggregateBox(this.words) : bounds);
		this.pageImageDpi = this.words[0].getPage().getImageDPI();
		
		//	compute row and column pixel stats, as well as average word height
		this.rowOccupations = new int[this.wordBounds.getHeight()];
		Arrays.fill(this.rowOccupations, 0);
		this.colOccupations = new int[this.wordBounds.getWidth()];
		Arrays.fill(this.colOccupations, 0);
		this.bColOccupations = new int[this.wordBounds.getWidth()];
		Arrays.fill(this.bColOccupations, 0);
		int wordHeightSum = 0;
		for (int w = 0; w < this.words.length; w++) {
			for (int y = Math.max(this.words[w].bounds.top, this.wordBounds.top); y < Math.min(this.words[w].bounds.bottom, this.wordBounds.bottom); y++)
				this.rowOccupations[y - this.wordBounds.top] += this.words[w].bounds.getWidth();
			for (int x = Math.max(this.words[w].bounds.left, this.wordBounds.left); x < Math.min(this.words[w].bounds.right, this.wordBounds.right); x++) {
				this.colOccupations[x - this.wordBounds.left] += this.words[w].bounds.getHeight();
				this.bColOccupations[x - this.wordBounds.left] += this.words[w].bounds.getHeight();
			}
			if (((w+1) < this.words.length) && areTextFlowSpaced(this.words[w], this.words[w+1], this.normSpaceWidth)) {
				for (int x = Math.max(this.words[w].bounds.right, this.wordBounds.left); x < Math.min(this.words[w+1].bounds.left, this.wordBounds.right); x++)
					this.bColOccupations[x - this.wordBounds.left] += this.words[w].bounds.getHeight();
			}
			wordHeightSum += this.words[w].bounds.getHeight();
		}
		
		//	compute plain occupied row and column counts
		this.wordPixelRows = countNonZeroElements(this.rowOccupations);
		this.wordPixelCols = countNonZeroElements(this.colOccupations);
		this.bWordPixelCols = countNonZeroElements(this.bColOccupations);
		
		//	compute average word height and space width
		this.avgWordHeight = ((wordHeightSum + (this.words.length / 2)) / this.words.length);
		this.avgSpaceWidth = (this.avgWordHeight * this.normSpaceWidth);
		
		//	get graphics objects overlapping with table area, and compute rendering bounds along the way
		ArrayList graphics = new ArrayList();
		int grLeft = this.wordBounds.left;
		int grRight = this.wordBounds.right;
		int grTop = this.wordBounds.top;
		int grBottom = this.wordBounds.bottom;
		int grSpanLeft = Integer.MAX_VALUE;
		int grSpanRight = Integer.MIN_VALUE;
		int grSpanTop = Integer.MAX_VALUE;
		int grSpanBottom = Integer.MIN_VALUE;
		ImSupplement[] supplements = page.getSupplements();
		for (int s = 0; s < supplements.length; s++)
			if (supplements[s] instanceof ImSupplement.Graphics) {
				ImSupplement.Graphics gr = ((ImSupplement.Graphics) supplements[s]);
				BoundingBox grBounds = gr.getBounds();
				if ((grBounds == null) || !grBounds.overlaps(this.wordBounds))
					continue; // this one didn't load properly, or doesn't overlap with table area
				graphics.add(gr);
				grLeft = Math.min(grLeft, grBounds.left);
				grRight = Math.max(grRight, grBounds.right);
				grTop = Math.min(grTop, grBounds.top);
				grBottom = Math.max(grBottom, grBounds.bottom);
				grSpanLeft = Math.min(grSpanLeft, grBounds.left);
				grSpanRight = Math.max(grSpanRight, grBounds.right);
				grSpanTop = Math.min(grSpanTop, grBounds.top);
				grSpanBottom = Math.max(grSpanBottom, grBounds.bottom);
				supplements[s] = null; // mark as done
			}
		
		//	add graphics objects overlapping with potentially extended table graphics area
		int grCount = graphics.size();
		BoundingBox grSpanBounds = null;
		while (grCount != 0) {
			grCount = graphics.size();
			BoundingBox aGrBounds = new BoundingBox(grLeft, grRight, grTop, grBottom);
			for (int s = 0; s < supplements.length; s++)
				if (supplements[s] instanceof ImSupplement.Graphics) {
					ImSupplement.Graphics gr = ((ImSupplement.Graphics) supplements[s]);
					BoundingBox grBounds = gr.getBounds();
					if ((grBounds == null) || !grBounds.overlaps(aGrBounds))
						continue; // this one didn't load properly, or doesn't overlap with table area
					graphics.add(gr);
					grLeft = Math.min(grLeft, grBounds.left);
					grRight = Math.max(grRight, grBounds.right);
					grTop = Math.min(grTop, grBounds.top);
					grBottom = Math.max(grBottom, grBounds.bottom);
					grSpanLeft = Math.min(grSpanLeft, grBounds.left);
					grSpanRight = Math.max(grSpanRight, grBounds.right);
					grSpanTop = Math.min(grSpanTop, grBounds.top);
					grSpanBottom = Math.max(grSpanBottom, grBounds.bottom);
					supplements[s] = null; // mark as done
				}
			if (grCount == graphics.size()) {
				grSpanBounds = new BoundingBox(grSpanLeft, grSpanRight, grSpanTop, grSpanBottom);
				break; // nothing new added this round, we're done here
			}
		}
		this.grCount = grCount;
		this.inWordGrBounds = grSpanBounds;
		
		//	render graphics objects on grayscale image (if any)
		if (graphics.size() == 0) {
			this.grBounds = null;
			this.grImage = null;
			this.grBrightness = null;
			this.grRowOccupations = null;
			this.grColOccupations = null;
			this.wordsInGraphics = 0;
		}
		else {
			this.grBounds = new BoundingBox(grLeft, grRight, grTop, grBottom);
			this.grImage = new BufferedImage(this.grBounds.getWidth(), this.grBounds.getHeight(), BufferedImage.TYPE_BYTE_GRAY);//ImSupplement.getCompositeImage(null, page.getImageDPI(), ((ImSupplement.Graphics[]) graphics.toArray(new ImSupplement.Graphics[graphics.size()])), null, page, BufferedImage.TYPE_BYTE_GRAY);
			
			//	set up rendering (cannot use ImSupplement.getCompoundImage() here because we need exact control over boundaries, and graphics might be short of aggregate word bounds)
			Graphics2D giGr = this.grImage.createGraphics();
			giGr.setColor(Color.WHITE);
			giGr.fillRect(0, 0, this.grImage.getWidth(), this.grImage.getHeight());
			giGr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			float renderScale = (((float) page.getImageDPI()) / ImSupplement.Graphics.RESOLUTION);
			
			//	render graphics
			for (int g = 0; g < graphics.size(); g++) {
				ImSupplement.Graphics gr = ((ImSupplement.Graphics) graphics.get(g));
				
				//	translate and scale to table area bounds
				AffineTransform preAt = giGr.getTransform();
				BoundingBox grBounds = gr.getBounds();
				giGr.translate((grBounds.left - this.grBounds.left), (grBounds.top - this.grBounds.top));
				giGr.scale(renderScale, renderScale);
				
				//	render paths
				Path[] paths = gr.getPaths();
				for (int p = 0; p < paths.length; p++) {
					Color preColor = giGr.getColor();
					Stroke preStroke = giGr.getStroke();
					Color strokeColor = paths[p].getStrokeColor();
					Stroke stroke = paths[p].getStroke();
					Color fillColor = paths[p].getFillColor();
					if ((strokeColor == null) && (fillColor == null))
						continue;
					
					Path2D path = new Path2D.Float();
					Graphics.SubPath[] subPaths = paths[p].getSubPaths();
					for (int s = 0; s < subPaths.length; s++) {
						Path2D subPath = subPaths[s].getPath();
						path.append(subPath, false);
					}
					
					if (fillColor != null) {
						giGr.setColor(fillColor);
						giGr.fill(path);
					}
					if (strokeColor != null) {
						giGr.setColor(strokeColor);
						giGr.setStroke(stroke);
						giGr.draw(path);
					}
					
					giGr.setColor(preColor);
					giGr.setStroke(preStroke);
				}
				
				//	reset renderer
				giGr.setTransform(preAt);
			}
//			ImageDisplayDialog grIdd = new ImageDisplayDialog("Table Area Graphics in Page " + page.pageId + " at " + this.grBounds);
//			grIdd.addImage(this.grImage, "");
//			grIdd.setSize(600, 800);
//			grIdd.setLocationRelativeTo(null);
//			grIdd.setVisible(true);
			
			//	get brightness, and compute distribution
			AnalysisImage ai = Imaging.wrapImage(this.grImage, null);
			this.grBrightness = ai.getBrightness();
			CountingSet brightnessDist = new CountingSet(new TreeMap());
			int minBrightness = Byte.MAX_VALUE;
			int maxBrightness = Byte.MIN_VALUE;
			for (int x = 0; x < this.grBrightness.length; x++)
				for (int y = 0; y < this.grBrightness[x].length; y++) {
					brightnessDist.add(Byte.valueOf(this.grBrightness[x][y]));
					minBrightness = Math.min(minBrightness, this.grBrightness[x][y]);
					maxBrightness = Math.max(maxBrightness, this.grBrightness[x][y]);
				}
			
			//	find maximum brightness covering at least 10% of pixels TODO use 20% ???
			int maxBrightnessAt10 = maxBrightness;
			int maxBrightnessCountAt10 = brightnessDist.getCount(Byte.valueOf((byte) maxBrightnessAt10));
			while ((maxBrightnessCountAt10 * 10) < this.grBounds.getArea()) {
				maxBrightnessAt10--;
				maxBrightnessCountAt10 += brightnessDist.getCount(Byte.valueOf((byte) maxBrightnessAt10));
			}
			
			//	make sure at least the 10% brightest pixels are white (some tables might have a completely non-white background ...)
			if (maxBrightnessAt10 < Byte.MAX_VALUE) {
				int whiteRgb = Color.WHITE.getRGB();
				for (int x = 0; x < this.grBrightness.length; x++) {
					for (int y = 0; y < this.grBrightness[x].length; y++)
						if ((this.grBrightness[x][y] < Byte.MAX_VALUE) && (maxBrightnessAt10 <= this.grBrightness[x][y])) {
							this.grImage.setRGB(x, y, whiteRgb);
							this.grBrightness[x][y] = Byte.MAX_VALUE;
						}
				}
			}
			
			//	count out non-white share of each row and column of pixels
			this.grRowOccupations = new int[this.grBounds.getHeight()];
			Arrays.fill(this.grRowOccupations, 0);
			this.grColOccupations = new int[this.grBounds.getWidth()];
			Arrays.fill(this.grColOccupations, 0);
			for (int x = 0; x < this.grBrightness.length; x++) {
				for (int y = 0; y < this.grBrightness[x].length; y++)
					if (this.grBrightness[x][y] < Byte.MAX_VALUE) {
						this.grRowOccupations[y]++;
						this.grColOccupations[x]++;
					}
			}
			
			//	find minimum, maximum, and distribution of coloration
			int minGrRowOccupation = this.grBounds.getHeight();
			int maxGrRowOccupation = 0;
			CountingSet grRowOccupationDist = new CountingSet(new TreeMap());
			for (int y = 0; y < this.grRowOccupations.length; y++) {
				minGrRowOccupation = Math.min(minGrRowOccupation, this.grRowOccupations[y]);
				maxGrRowOccupation = Math.max(maxGrRowOccupation, this.grRowOccupations[y]);
				grRowOccupationDist.add(new Integer(this.grRowOccupations[y]));
			}
			int minGrColOccupation = this.grBounds.getHeight();
			int maxGrColOccupation = 0;
			CountingSet grColOccupationDist = new CountingSet(new TreeMap());
			for (int x = 0; x < this.grColOccupations.length; x++) {
				minGrColOccupation = Math.min(minGrColOccupation, this.grColOccupations[x]);
				maxGrColOccupation = Math.max(maxGrColOccupation, this.grColOccupations[x]);
				grColOccupationDist.add(new Integer(this.grColOccupations[x]));
			}
			
			//	count out number of words in non-white graphics areas
			int wordsInGraphics = 0;
			for (int w = 0; w < this.words.length; w++) {
				if (this.grBrightness[this.words[w].centerX - this.grBounds.left][this.words[w].centerY - this.grBounds.top] < Byte.MAX_VALUE)
					wordsInGraphics++;
			}
			this.wordsInGraphics = wordsInGraphics;
		}
		
		/* TODO determine table grid type:
		 * - wide peaks, and words (word center points) inside colored area ==> stripe grid (horizontally or vertically)
		 *   - otherwise ==> line grid
		 * - block line grid peaks for column and especially row merging
		 * - block stripe grid peak _flanks_ for column and especially row merging
		 * - use grayscale bitmap proper to assess whether or not some cells _can_ be merged
		 * - if we have a full grid, use grayscale bitmap to determine which cells _should_ be merged (there won't be any grid lines separating them, just check area between them)
		 */
	}
	
	private static boolean areTextFlowSpaced(ImWord w1, ImWord w2, float normSpaceWidth) {
		if (w1.bounds.bottom < w2.centerY)
			return false; // second word (mostly) below first
		if (w1.bounds.top > w2.centerY)
			return false; // second word (mostly) above first (in whichever way)
		if (w1.centerX > w2.centerX)
			return false; // second word to left of first (in whichever way)
		if (w1.bounds.right >= w2.bounds.left)
			return false; // no space between the two
		int maxWordHeigth = Math.max(w1.bounds.getHeight(), w2.bounds.getHeight());
		return ((w2.bounds.left - w1.bounds.right) < (maxWordHeigth * normSpaceWidth));
	}
	
	private static int countNonZeroElements(int[] array) {
		int nonZeroEntries = 0;
		for (int c = 0; c < array.length; c++) {
			if (array[c] != 0)
				nonZeroEntries++;
		}
		return nonZeroEntries;
	}
	
	
	byte getMinGrBrightness(BoundingBox bounds) {
		if (this.grBrightness == null)
			return Byte.MAX_VALUE;
		byte minGrBrightness = Byte.MAX_VALUE;
		for (int x = Math.max((bounds.left - this.grBounds.left), 0); x < Math.min((bounds.right - this.grBounds.left), this.grBounds.getWidth()); x++)
			for (int y = Math.max((bounds.top - this.grBounds.top), 0); y < Math.min((bounds.bottom - this.grBounds.top), this.grBounds.getHeight()); y++) {
				if (this.grBrightness[x][y] < minGrBrightness)
					minGrBrightness = this.grBrightness[x][y];
			}
		return minGrBrightness;
	}
	
	static class GraphicsAreaBrightness {
		final BoundingBox bounds;
		final byte[][] brightness;
		final byte minBrightness;
		final byte maxBrightness;
		final byte avgBrightness;
		GraphicsAreaBrightness(BoundingBox bounds, byte[][] brightness) {
			this.bounds = bounds;
			this.brightness = brightness;
			int minBrightness = Byte.MAX_VALUE;
			int maxBrightness = 0;
			int brightnessSum = 0;
			for (int x = 0; x < brightness.length; x++)
				for (int y = 0; y < brightness[x].length; y++) {
					minBrightness = Math.min(minBrightness, brightness[x][y]);
					maxBrightness = Math.max(maxBrightness, brightness[x][y]);
					brightnessSum += brightness[x][y];
				}
			
			this.minBrightness = ((byte) minBrightness);
			this.maxBrightness = ((byte) maxBrightness);
			this.avgBrightness = ((byte) ((brightnessSum + (this.bounds.getArea() / 2)) / this.bounds.getArea()));
		}
	}
	
	GraphicsAreaBrightness getGrBrightness(BoundingBox bounds) {
		if (this.grBrightness == null)
			return null;
		
		int left = Math.max(bounds.left, this.grBounds.left);
		int right = Math.min(bounds.right, this.grBounds.right);
		int top = Math.max(bounds.top, this.grBounds.top);
		int bottom = Math.min(bounds.bottom, this.grBounds.bottom);
		bounds = new BoundingBox(left, right, top, bottom);
		
		byte[][] areaBrightness = new byte[bounds.getWidth()][bounds.getHeight()];
		for (int x = 0; x < bounds.getWidth(); x++)
			System.arraycopy(this.grBrightness[x + (bounds.left - this.grBounds.left)], (bounds.top - this.grBounds.top), areaBrightness[x], 0, areaBrightness[x].length);
		return new GraphicsAreaBrightness(bounds, areaBrightness);
	}
	
	
	static class GraphicsSlice {
		final int min;
		final int max;
		final int pixelCount;
		final int[] pixelOccupation;
		final int minOccupiedPixels;
		final int maxOccupiedPixels;
		final int avgOccupiedPixels;
		GraphicsSlice(int min, int pixelCount, int[] pixelOccupation) {
			this.min = min;
			this.pixelCount = pixelCount;
			this.pixelOccupation = pixelOccupation;
			this.max = (this.min + this.pixelOccupation.length);
			int minOccupiedPixels = Integer.MAX_VALUE;
			int maxOccupiedPixels = 0;
			int occupiedPixelSum = 0;
			for (int p = 0; p < this.pixelOccupation.length; p++) {
				minOccupiedPixels = Math.min(minOccupiedPixels, pixelOccupation[p]);
				maxOccupiedPixels = Math.max(maxOccupiedPixels, pixelOccupation[p]);
				occupiedPixelSum += pixelOccupation[p];
			}
			this.minOccupiedPixels = minOccupiedPixels;
			this.maxOccupiedPixels = maxOccupiedPixels;
			this.avgOccupiedPixels = ((occupiedPixelSum + (this.pixelOccupation.length / 2)) / this.pixelOccupation.length);
		}
		
		boolean hasPeak() {
			if (((this.maxOccupiedPixels - this.minOccupiedPixels) * 2) < this.pixelCount)
				return false; // no sufficiently sharp contrast
			//	low on edges ==> maximum must be in middle and form peak
			return ((this.pixelOccupation[0] < this.avgOccupiedPixels) && (this.pixelOccupation[this.pixelOccupation.length - 1] < this.avgOccupiedPixels));
		}
		
		boolean hasFlank() {
			if (((this.maxOccupiedPixels - this.minOccupiedPixels) * 2) < this.pixelCount)
				return false; // no sufficiently sharp contrast
			//	low on one edge, high on other ==> maximum must be on either side
			return ((this.pixelOccupation[0] < this.avgOccupiedPixels) != (this.pixelOccupation[this.pixelOccupation.length - 1] < this.avgOccupiedPixels));
		}
	}
	
	GraphicsSlice getColGraphicsStats(int left, int right) {
		return ((this.grBounds == null) ? null : getSliceGraphicsStats(left, right, this.grBounds.left, this.grBounds.getHeight(), this.grColOccupations));
	}
	
	GraphicsSlice getRowGraphicsStats(int top, int bottom) {
		return ((this.grBounds == null) ? null : getSliceGraphicsStats(top, bottom, this.grBounds.top, this.grBounds.getWidth(), this.grRowOccupations));
	}
	
	private static GraphicsSlice getSliceGraphicsStats(int min, int max, int offset, int pixelCount, int[] pixelOccupations) {
		if (min < offset)
			min = offset;
		if ((offset + pixelOccupations.length) < max)
			max = (offset + pixelOccupations.length);
		return new GraphicsSlice(min, pixelCount, Arrays.copyOfRange(pixelOccupations, (min - offset), (max - offset)));
	}
	
	
	LinkedHashSet getColOccupationFlanks() {
		if (this.colOccupationFlanks == null)
			this.colOccupationFlanks = getColumnOccupationFlanks(this.pageImageDpi, this.wordBounds.left, this.colOccupations, this.wordPixelRows, this.wordBounds.getHeight());
		return this.colOccupationFlanks;
	}
	private LinkedHashSet colOccupationFlanks = null;
	
	LinkedHashSet getBrgColOccupationFlanks() {
		if (this.bColOccupationFlanks == null)
			this.bColOccupationFlanks = getColumnOccupationFlanks(this.pageImageDpi, this.wordBounds.left, this.bColOccupations, this.wordPixelRows, this.wordBounds.getHeight());
		return this.bColOccupationFlanks;
	}
	private LinkedHashSet bColOccupationFlanks = null;
	
	//	TODO make fraction getters static
	//	TODO add static getters for minimum, average, and maximum low width
	
	TreeSet getColOccupationLows() {
		if (this.colOccupationLows == null)
			this.colOccupationLows = getColumnOccupationLows(this, this.wordBounds.left, this.colOccupations, this.wordPixelRows);
		return this.colOccupationLows;
	}
	private TreeSet colOccupationLows = null;
	
	float getColOccupationLowFract() {
		if (this.colOccupationLowFract == -1)
			this.colOccupationLowFract = countLowOccupationColumns(this.getColOccupationLows(), this.wordBounds.getWidth());
		return this.colOccupationLowFract;
	}
	private float colOccupationLowFract = -1;
	
	TreeSet getColOccupationLowsFiltered() {
		if (this.colOccupationLowsFiltered == null)
			this.colOccupationLowsFiltered = filterColumnOccupationLows(this.getColOccupationLows(), this.wordBounds, this.wordPixelRows, this.avgWordHeight, this.normSpaceWidth);
		return this.colOccupationLowsFiltered;
	}
	private TreeSet colOccupationLowsFiltered = null;
	
	float getColOccupationLowFractFiltered() {
		if (this.colOccupationLowFractFiltered == -1)
			this.colOccupationLowFractFiltered = countLowOccupationColumns(this.getColOccupationLowsFiltered(), this.wordBounds.getWidth());
		return this.colOccupationLowFractFiltered;
	}
	private float colOccupationLowFractFiltered = -1;
	
	TreeSet getColOccupationLowsCleaned() {
		if (this.colOccupationLowsCleaned == null)
			this.colOccupationLowsCleaned = cleanColumnOccupationLows(this, this.getColOccupationLowsFiltered(), this.avgWordHeight);
		return this.colOccupationLowsCleaned;
	}
	private TreeSet colOccupationLowsCleaned = null;
	
	float getColOccupationLowFractCleaned() {
		if (this.colOccupationLowFractCleaned == -1)
			this.colOccupationLowFractCleaned = countLowOccupationColumns(this.getColOccupationLowsCleaned(), this.wordBounds.getWidth());
		return this.colOccupationLowFractCleaned;
	}
	private float colOccupationLowFractCleaned = -1;
	
	TreeSet getColOccupationLowsReduced() {
		if (this.colOccupationLowsReduced == null)
			this.colOccupationLowsReduced = reduceColumnOccupationLows(this.getColOccupationLowsCleaned(), this.avgWordHeight, this.normSpaceWidth);
		return this.colOccupationLowsReduced;
	}
	private TreeSet colOccupationLowsReduced = null;
	
	float getColOccupationLowFractReduced() {
		if (this.colOccupationLowFractReduced == -1)
			this.colOccupationLowFractReduced = countLowOccupationColumns(this.getColOccupationLowsReduced(), this.wordBounds.getWidth());
		return this.colOccupationLowFractReduced;
	}
	private float colOccupationLowFractReduced = -1;
	
	//	TODO make fraction getters static
	//	TODO add static getters for minimum, average, and maximum low width
	
	TreeSet getBrgColOccupationLows() {
		if (this.bColOccupationLows == null)
			this.bColOccupationLows = getColumnOccupationLows(this, this.wordBounds.left, this.bColOccupations, this.wordPixelRows);
		return this.bColOccupationLows;
	}
	private TreeSet bColOccupationLows = null;
	
	float getBrgColOccupationLowFract() {
		if (this.bColOccupationLowFract == -1)
			this.bColOccupationLowFract = countLowOccupationColumns(this.getBrgColOccupationLows(), this.wordBounds.getWidth());
		return this.bColOccupationLowFract;
	}
	private float bColOccupationLowFract = -1;
	
	TreeSet getBrgColOccupationLowsFiltered() {
		if (this.bColOccupationLowsFiltered == null)
			this.bColOccupationLowsFiltered = filterColumnOccupationLows(this.getBrgColOccupationLows(), this.wordBounds, this.wordPixelRows, this.avgWordHeight, this.normSpaceWidth);
		return this.bColOccupationLowsFiltered;
	}
	private TreeSet bColOccupationLowsFiltered = null;
	
	float getBrgColOccupationLowFractFiltered() {
		if (this.bColOccupationLowFractFiltered == -1)
			this.bColOccupationLowFractFiltered = countLowOccupationColumns(this.getBrgColOccupationLowsFiltered(), this.wordBounds.getWidth());
		return this.bColOccupationLowFractFiltered;
	}
	private float bColOccupationLowFractFiltered = -1;
	
	TreeSet getBrgColOccupationLowsCleaned() {
		if (this.bColOccupationLowsCleaned == null)
			this.bColOccupationLowsCleaned = cleanColumnOccupationLows(this, this.getBrgColOccupationLowsFiltered(), this.avgWordHeight);
		return this.bColOccupationLowsCleaned;
	}
	private TreeSet bColOccupationLowsCleaned = null;
	
	float getBrgColOccupationLowFractCleaned() {
		if (this.bColOccupationLowFractCleaned == -1)
			this.bColOccupationLowFractCleaned = countLowOccupationColumns(this.getBrgColOccupationLowsCleaned(), this.wordBounds.getWidth());
		return this.bColOccupationLowFractCleaned;
	}
	private float bColOccupationLowFractCleaned = -1;
	
	TreeSet getBrgColOccupationLowsReduced() {
		if (this.bColOccupationLowsReduced == null)
			this.bColOccupationLowsReduced = reduceColumnOccupationLows(this.getBrgColOccupationLowsCleaned(), this.avgWordHeight, this.normSpaceWidth);
		return this.bColOccupationLowsReduced;
	}
	private TreeSet bColOccupationLowsReduced = null;
	
	float getBrgColOccupationLowFractReduced() {
		if (this.bColOccupationLowFractReduced == -1)
			this.bColOccupationLowFractReduced = countLowOccupationColumns(this.getBrgColOccupationLowsReduced(), this.wordBounds.getWidth());
		return this.bColOccupationLowFractReduced;
	}
	private float bColOccupationLowFractReduced = -1;
	
	
	TreeSet getRowOccupationGaps() {
		if (this.rowOccupationGaps == null)
			this.rowOccupationGaps = getRowOccupationGaps(this, this.wordBounds.top, this.rowOccupations);
		return this.rowOccupationGaps;
	}
	private TreeSet rowOccupationGaps = null;
	
	float getRowOccupationGapFract() {
		if (this.rowOccupationGapFract == -1)
			this.rowOccupationGapFract = (((float) this.wordPixelRows) / this.wordBounds.getHeight());
		return this.rowOccupationGapFract;
	}
	private float rowOccupationGapFract = -1;
	
	
	/**
	 * A flank (i.e., drastic rise or drop) in the number of pixel rows that
	 * are occupied with words, within a narrow window of pixel columns.
	 * 
	 * @author sautter
	 */
	static class ColumnOccupationFlank {
		final int center;
		final int height;
		final int width;
		final int min;
		final int max;
		final boolean isRise;
		ColumnOccupationFlank(int center, int min, int max, int width, boolean isRise) {
			this.center = center;
			this.height = (max - min);
			this.width = width;
			this.min = min;
			this.max = max;
			this.isRise = isRise;
		}
		public boolean equals(Object obj) {
			if (obj instanceof ColumnOccupationFlank) {
				ColumnOccupationFlank cof = ((ColumnOccupationFlank) obj);
				return ((this.center == cof.center) && (this.height == cof.height) && (this.width == cof.width) && (this.isRise == cof.isRise));
			}
			else return false;
		}
		public int hashCode() {
			if (this.hash == 0) {
				int hash = 0;
				hash |= (this.center & 0xFFFF);
				hash <<= 16;
				hash |= (this.height & 0xFFFF);
				this.hash = (this.isRise ? hash : (hash ^ 0xFFFFFFFF));
			}
			return this.hash;
		}
		private int hash = 0;
		public String toString() {
			if (this.string == null)
				this.string = ((this.isRise ? "Rise" : "Drop") + " by " + this.height + "(" + (this.isRise ? (this.min + "-" + this.max) : (this.max + "-" + this.min)) + ") at " + this.center + "(" + this.width + ")");
			return this.string;
		}
		private String string = null;
	}
	
	private static LinkedHashSet getColumnOccupationFlanks(int pageImageDpi, int areaLeft, int[] areaColPixelRowCounts, int maxAreaColPixelRowCount, int areaHeight) {
		
		//	collect all sharp flanks (both up and down) across pixel column occupation
		int radius = Math.round(((float) pageImageDpi) / (50 + 50));
		LinkedHashSet colPixelCountFlankSet = new LinkedHashSet();
		for (int c = 0; c < areaColPixelRowCounts.length; c++) {
			int[] colPixelRowCounts = new int[radius + 1 + radius];
			for (int x = 0; x < colPixelRowCounts.length; x++) {
				if ((c + x - radius) < 0)
					colPixelRowCounts[x] = 0;
				else if ((c + x - radius) < areaColPixelRowCounts.length)
					colPixelRowCounts[x] = areaColPixelRowCounts[c + x - radius];
				else colPixelRowCounts[x] = 0;
			}
			int colPixelMin = areaHeight;
			int minMinX = 0;
			int maxMinX = 0;
			int colPixelMax = 0;
			int minMaxX = 0;
			int maxMaxX = 0;
			for (int x = 0; x < colPixelRowCounts.length; x++) {
				if (colPixelRowCounts[x] < colPixelMin) {
					colPixelMin = colPixelRowCounts[x];
					minMinX = x;
					maxMinX = x;
				}
				else if (colPixelRowCounts[x] == colPixelMin)
					maxMinX = x;
				if (colPixelMax < colPixelRowCounts[x]) {
					colPixelMax = colPixelRowCounts[x];
					minMaxX = x;
					maxMaxX = x;
				}
				else if (colPixelMax == colPixelRowCounts[x])
					maxMaxX = x;
			}
			if (((colPixelMax - colPixelMin) * 2) > (maxAreaColPixelRowCount * 1)) {
//				System.out.println("     --> Got sharp flank at " + c + ": (" + colPixelMin + "/" + colPixelMax + ") in " + Arrays.toString(colPixelRowCounts));
				//	last minimum before first maximum ==> rise
				if (maxMinX < minMaxX) {
					int width = (minMaxX - maxMinX);
					int center = (c + maxMinX - radius + (width / 2));
					colPixelCountFlankSet.add(new ColumnOccupationFlank((areaLeft + center), colPixelMin, colPixelMax, width, true));
				}
				//	last maximum before first minimum ==> drop
				if (maxMaxX < minMinX) {
					int width = (minMinX - maxMaxX);
					int center = (c + maxMaxX - radius + (width / 2));
					colPixelCountFlankSet.add(new ColumnOccupationFlank((areaLeft + center), colPixelMin, colPixelMax, width, false));
				}
			}
		}
		
		//	eliminate neighboring duplicates (edges of sliding window might yield such if only part of flank inside)
		ArrayList colPixelCountFlankList = new ArrayList(colPixelCountFlankSet);
		for (int f = 0; f < colPixelCountFlankList.size(); f++) {
			ColumnOccupationFlank cof = ((ColumnOccupationFlank) colPixelCountFlankList.get(f));
			for (int cf = (f+1); cf < colPixelCountFlankList.size(); cf++) {
				ColumnOccupationFlank cCof = ((ColumnOccupationFlank) colPixelCountFlankList.get(cf));
				if ((radius + 1 + radius) < (cCof.center - cof.center))
					break; // too far apart, we're done here
				if (cCof.isRise != cof.isRise)
					continue; // those two don't compare at all
				if (cCof.height <= cof.height)
					colPixelCountFlankList.remove(cf--); // remove right duplicate of sub-flank
				else {
					colPixelCountFlankList.remove(f--); // remove left duplicate of sub-flank
					break; // need to start over in outer loop
				}
			}
		}
		colPixelCountFlankSet.clear();
		colPixelCountFlankSet.addAll(colPixelCountFlankList);
		
		//	finally ...
		return colPixelCountFlankSet;
	}
	
	/**
	 * A local low in the number of pixel rows that are occupied with words, in
	 * comparison to surrounding pixel columns.
	 * 
	 * @author sautter
	 */
	static class ColumnOccupationLow implements Comparable {
		private final TableAreaStatistics tas;
		final int left;
		final int right;
		final int min;
		final int max;
		boolean enforced;
		ColumnOccupationLow(TableAreaStatistics tas, int left, int right, int min, int max) {
			this(tas, left, right, min, max, false);
		}
		ColumnOccupationLow(TableAreaStatistics tas, int left, int right, int min, int max, boolean enforced) {
			this.tas = tas;
			this.left = left;
			this.right = right;
			this.min = min;
			this.max = max;
			this.enforced = enforced;
		}
		int getWidth() {
			return (this.right - this.left);
		}
		public boolean equals(Object obj) {
			if (obj instanceof ColumnOccupationLow) {
				ColumnOccupationLow col = ((ColumnOccupationLow) obj);
				return ((this.left == col.left) && (this.right == col.right));
			}
			else return false;
		}
		public int hashCode() {
			if (this.hash == 0) {
				int hash = 0;
				hash |= (this.left & 0xFFFF);
				hash <<= 16;
				hash |= (this.right & 0xFFFF);
				this.hash = hash;
			}
			return this.hash;
		}
		private int hash = 0;
		public String toString() {
			if (this.string == null)
				this.string = ("Low with maximum of " + this.max + " at " + this.left + "-" + this.right);
			return this.string;
		}
		private String string = null;
		public int compareTo(Object obj) {
			ColumnOccupationLow col = ((ColumnOccupationLow) obj);
			return ((this.left == col.left) ? (col.right - this.right) : (this.left - col.left));
		}
		boolean isBlockedByPeak() {
			if (this.grStats == null)
				this.grStats = this.tas.getColGraphicsStats(this.left, this.right);
			return ((this.grStats != null) && this.grStats.hasPeak());
		}
		boolean isBlockedByFlank() {
			if (this.grStats == null)
				this.grStats = this.tas.getColGraphicsStats(this.left, this.right);
			return ((this.grStats != null) && this.grStats.hasFlank());
		}
		boolean isBlockedByGraphics() {
			if (this.grStats == null)
				this.grStats = this.tas.getColGraphicsStats(this.left, this.right);
			return ((this.grStats != null) && (this.grStats.hasPeak() || this.grStats.hasFlank()));
		}
		private GraphicsSlice grStats = null;
	}
	
	private static TreeSet getColumnOccupationLows(TableAreaStatistics tas, int areaLeft, int[] areaColPixelRowCounts, int maxAreaColPixelRowCount) {
		TreeSet colPixelCountLowSet = new TreeSet();
		
		//	no actual column gap should be obstructed in more than one third of rows (there might be pathological cases, but then ...)
		int maxLowColPixelCount = (maxAreaColPixelRowCount / 3);
		
		//	collect all lows across pixel column occupation
		for (int c = 0; c < areaColPixelRowCounts.length; c++) {
			if (maxLowColPixelCount < areaColPixelRowCounts[c])
				continue; // this one is too dense
			
			//	we have a jump, measure gap (we cannot only start at drops, as some wider non-zero lows might have might have zero lows on both ends)
			if ((c == 0) || (areaColPixelRowCounts[c-1] != areaColPixelRowCounts[c])) {
				int min = maxLowColPixelCount;
				int max = 0;
				int left = c;
				int right = c;
				for (; right < areaColPixelRowCounts.length; right++) {
					if (areaColPixelRowCounts[right] <= areaColPixelRowCounts[c]) {
						min = Math.min(min, areaColPixelRowCounts[right]);
						max = Math.max(max, areaColPixelRowCounts[right]);
					}
					else break;
				}
				for (; left != 0; left--) {
					if (areaColPixelRowCounts[left - 1] <= areaColPixelRowCounts[c]) {
						min = Math.min(min, areaColPixelRowCounts[left - 1]);
						max = Math.max(max, areaColPixelRowCounts[left - 1]);
					}
					else break;
				}
				colPixelCountLowSet.add(new ColumnOccupationLow(tas, (areaLeft + left), (areaLeft + right), min, max));
			}
			
			//	we have a jump, measure gap (we cannot only start at rises, as some wider non-zero lows might have might have zero lows on both ends)
			if (((c+1) == areaColPixelRowCounts.length) || areaColPixelRowCounts[c] != areaColPixelRowCounts[c+1]) {
				int min = maxLowColPixelCount;
				int max = 0;
				int left = c;
				int right = c;
				for (; right < areaColPixelRowCounts.length; right++) {
					if (areaColPixelRowCounts[right] <= areaColPixelRowCounts[c]) {
						min = Math.min(min, areaColPixelRowCounts[right]);
						max = Math.max(max, areaColPixelRowCounts[right]);
					}
					else break;
				}
				for (; left != 0; left--) {
					if (areaColPixelRowCounts[left - 1] <= areaColPixelRowCounts[c]) {
						min = Math.min(min, areaColPixelRowCounts[left - 1]);
						max = Math.max(max, areaColPixelRowCounts[left - 1]);
					}
					else break;
				}
				colPixelCountLowSet.add(new ColumnOccupationLow(tas, (areaLeft + left), (areaLeft + right), min, max));
			}
		}
		
		//	finally ...
		return colPixelCountLowSet;
	}
	
	private static TreeSet filterColumnOccupationLows(TreeSet columnOccupationLows, BoundingBox bounds, int maxAreaColPixelRowCount, int avgAreaWordHeigth, float normSpaceWidth) {
		TreeSet fColumnOccupationLows = new TreeSet();
		for (Iterator colit = columnOccupationLows.iterator(); colit.hasNext();) {
			ColumnOccupationLow col = ((ColumnOccupationLow) colit.next());
			if ((col.left == bounds.left) || (col.right == bounds.right))
				continue; // there are no column gaps on the edge of a table !!!
			if (col.getWidth() <= (avgAreaWordHeigth * normSpaceWidth))
				continue; // as narrow as an average space, hardly a column gap
			if (((col.max * 4) > maxAreaColPixelRowCount) && (col.getWidth() < avgAreaWordHeigth))
				continue; // more than one quarter obstructed, we need some width here
			if (((col.max * 6) > maxAreaColPixelRowCount) && (col.getWidth() < (avgAreaWordHeigth * Math.min(1, (normSpaceWidth * 2)))))
				continue; // more than one sixth obstructed, we need at least a double space here
			if (col.getWidth() < (avgAreaWordHeigth * normSpaceWidth))
				continue; // unobstructed, but we still need more than a normal space for a column gap
			fColumnOccupationLows.add(col); // this one looks good
		}
		return fColumnOccupationLows;
	}
	
	private static TreeSet cleanColumnOccupationLows(TableAreaStatistics tas, TreeSet columnOccupationLows, int avgAreaWordHeigth) {
		TreeSet cColumnOccupationLows = new TreeSet();
		
		//	clean lows in groups separated by non-low areas
		ArrayList columnOccupationLowGroup = null;
		int lastColGroupRight = Integer.MIN_VALUE;
		for (Iterator colit = columnOccupationLows.iterator(); colit.hasNext();) {
			ColumnOccupationLow col = ((ColumnOccupationLow) colit.next());
			
			//	this one doesn't overlap with current group, clean the latter and store the result
			if (lastColGroupRight < col.left) {
				if (columnOccupationLowGroup != null)
					cColumnOccupationLows.addAll(cleanColumnOccupationLows(tas, columnOccupationLowGroup, avgAreaWordHeigth));
				
				//	start next group
				columnOccupationLowGroup = new ArrayList();
				lastColGroupRight = col.right;
			}
			
			//	add current low to group
			columnOccupationLowGroup.add(col);
		}
		
		//	clean the last group and store the result
		if (columnOccupationLowGroup != null)
			cColumnOccupationLows.addAll(cleanColumnOccupationLows(tas, columnOccupationLowGroup, avgAreaWordHeigth));
		
		//	return cleaned lows
		return cColumnOccupationLows;
	}
	
	/* This method will only ever merge up two lows if their separating relative
	 * high qualifies as a low as well, as without a spanning (higher) low they
	 * would be in different groups */
	private static ArrayList cleanColumnOccupationLows(TableAreaStatistics tas, ArrayList columnOccupationLowGroup, int avgAreaWordHeigth) {
		for (int l = 0; l < columnOccupationLowGroup.size(); l++) {
			ColumnOccupationLow col = ((ColumnOccupationLow) columnOccupationLowGroup.get(l));
			for (int cl = (l+1); cl < columnOccupationLowGroup.size(); cl++) {
				ColumnOccupationLow cCol = ((ColumnOccupationLow) columnOccupationLowGroup.get(cl));
				if (avgAreaWordHeigth < (4 * (cCol.left - col.right)))
					continue; // separating high wider than one quarter of average word hight
				if (avgAreaWordHeigth < (2 * Math.abs(col.max - cCol.max)))
					continue; // height differs by more than half the average word height
				ColumnOccupationLow mCol = new ColumnOccupationLow(tas, col.left, cCol.right, Math.min(col.min, cCol.min), Math.max(col.max, cCol.max));
				columnOccupationLowGroup.set(l, mCol); // replace left merged low with merge result
				columnOccupationLowGroup.remove(cl--); // remove right merged low
				col = cCol; // continue with merge result
			}
		}
		return columnOccupationLowGroup;
	}
	
	private static TreeSet reduceColumnOccupationLows(TreeSet columnOccupationLows, int avgAreaWordHeigth, float normSpaceWidth) {
		TreeSet rColumnOccupationLows = new TreeSet();
		
		//	reduce lows in groups separated by non-low areas
		ArrayList columnOccupationLowGroup = null;
		int lastColGroupRight = Integer.MIN_VALUE;
		for (Iterator colit = columnOccupationLows.iterator(); colit.hasNext();) {
			ColumnOccupationLow col = ((ColumnOccupationLow) colit.next());
			
			//	this one doesn't overlap with last group, reduce the latter and store the result
			if (lastColGroupRight < col.left) {
				if (columnOccupationLowGroup != null)
					rColumnOccupationLows.addAll(reduceColumnOccupationLows(columnOccupationLowGroup, avgAreaWordHeigth, normSpaceWidth));
				columnOccupationLowGroup = new ArrayList();
				lastColGroupRight = col.right;
			}
			columnOccupationLowGroup.add(col);
		}
		
		//	reduce the last group and store the result
		if (columnOccupationLowGroup != null)
			rColumnOccupationLows.addAll(reduceColumnOccupationLows(columnOccupationLowGroup, avgAreaWordHeigth, normSpaceWidth));
		
		//	return reduced lows
		return rColumnOccupationLows;
	}
	
	private static ArrayList reduceColumnOccupationLows(ArrayList columnOccupationLowGroup, int avgAreaWordHeigth, float normSpaceWidth) {
		for (int l = 0; l < columnOccupationLowGroup.size(); l++) {
			ColumnOccupationLow col = ((ColumnOccupationLow) columnOccupationLowGroup.get(l));
			for (int cl = (l+1); cl < columnOccupationLowGroup.size(); cl++) {
				ColumnOccupationLow cCol = ((ColumnOccupationLow) columnOccupationLowGroup.get(cl));
				if (col.right < cCol.left)
					break; // cannot reduce to disjoint low (can end up in same group as part of wider low spanning through sparse columns)
				if ((avgAreaWordHeigth * 3) < (4 * cCol.getWidth())) {/* wider than three quarters of average word height */}
				else if ((cCol.max == 0) && ((avgAreaWordHeigth * 2) < (3 * cCol.getWidth()))) {/* zero low wider than two thirds of average word height */}
				else if ((col.getWidth() * 9) < (10 * cCol.getWidth())) {/* wider than 90% of parent low */}
				else if ((cCol.max == 0) && ((col.getWidth() * 4) < (5 * cCol.getWidth()))) {/* zero low wider than 80% of parent low */}
				else if ((avgAreaWordHeigth * normSpaceWidth) < cCol.getWidth()) {/* need to allow reducing to everything we refuse to bridge TODO test this */}
				else continue; // this one is just too narrow overall to reduce to
				columnOccupationLowGroup.remove(l--);
				break;
			}
		}
		return columnOccupationLowGroup;
	}
	
	private static float countLowOccupationColumns(TreeSet columnOccupationLows, int tableAreaWidth) {
		int lowOccupationColumnCount = 0;
		
		//	count lows in groups separated by non-low areas
		int lastColGroupRight = Integer.MIN_VALUE;
		for (Iterator colit = columnOccupationLows.iterator(); colit.hasNext();) {
			ColumnOccupationLow col = ((ColumnOccupationLow) colit.next());
			if (col.right <= lastColGroupRight)
				continue; // included in preceding (higher) low
			lastColGroupRight = col.right;
			lowOccupationColumnCount += col.getWidth();
		}
		
		//	return width fraction of lows
		return (((float) lowOccupationColumnCount) / tableAreaWidth);
	}
	
	/**
	 * A local low in the number of pixel rows that are occupied with words, in
	 * comparison to surrounding pixel columns.
	 * 
	 * @author sautter
	 */
	static class RowOccupationGap implements Comparable {
		private final TableAreaStatistics tas;
		final int top;
		final int bottom;
		boolean enforced;
		RowOccupationGap(TableAreaStatistics tas, int top, int bottom) {
			this(tas, top, bottom, false);
		}
		RowOccupationGap(TableAreaStatistics tas, int top, int bottom, boolean enforced) {
			this.tas = tas;
			this.top = top;
			this.bottom = bottom;
			this.enforced = enforced;
		}
		int getHeight() {
			return (this.bottom - this.top);
		}
		public boolean equals(Object obj) {
			if (obj instanceof RowOccupationGap) {
				RowOccupationGap rog = ((RowOccupationGap) obj);
				return ((this.top == rog.top) && (this.bottom == rog.bottom));
			}
			else return false;
		}
		public int hashCode() {
			if (this.hash == 0) {
				int hash = 0;
				hash |= (this.top & 0xFFFF);
				hash <<= 16;
				hash |= (this.bottom & 0xFFFF);
				this.hash = hash;
			}
			return this.hash;
		}
		private int hash = 0;
		public String toString() {
			if (this.string == null)
				this.string = ("Gap at " + this.top + "-" + this.bottom);
			return this.string;
		}
		private String string = null;
		public int compareTo(Object obj) {
			RowOccupationGap rog = ((RowOccupationGap) obj);
			return ((this.top == rog.top) ? (rog.bottom - this.bottom) : (this.top - rog.bottom));
		}
		boolean isBlockedByPeak() {
			if (this.grStats == null)
				this.grStats = this.tas.getRowGraphicsStats(this.top, this.bottom);
			return ((this.grStats != null) && this.grStats.hasPeak());
		}
		boolean isBlockedByFlank() {
			if (this.grStats == null)
				this.grStats = this.tas.getRowGraphicsStats(this.top, this.bottom);
			return ((this.grStats != null) && this.grStats.hasFlank());
		}
		boolean isBlockedByGraphics() {
			if (this.grStats == null)
				this.grStats = this.tas.getRowGraphicsStats(this.top, this.bottom);
			return ((this.grStats != null) && (this.grStats.hasPeak() || this.grStats.hasFlank()));
		}
		private GraphicsSlice grStats = null;
	}
	
	private static TreeSet getRowOccupationGaps(TableAreaStatistics tas, int areaTop, int[] areaRowPixelColCounts) {
		TreeSet rowPixelCountGapSet = new TreeSet();
		
		//	collect all gaps across pixel row occupation
		for (int r = 0; r < areaRowPixelColCounts.length; r++) {
			if (areaRowPixelColCounts[r] != 0)
				continue; // not a gap
			int rowPixelCountGapStart = r++; // seek gap end from next pixel row
			for (; r < areaRowPixelColCounts.length; r++)
				if (areaRowPixelColCounts[r] != 0) {
					rowPixelCountGapSet.add(new RowOccupationGap(tas, (areaTop + rowPixelCountGapStart), (areaTop + r))); // no decrement required, no need to investigate this one again in outer loop
					break;
				}
		}
		
		//	finally ...
		return rowPixelCountGapSet;
	}
	
	static TableAreaStatistics getTableAreaStatistics(ImPage page, ImWord[] words) {
		return getTableAreaStatistics(page, words, defaultNormSpaceWidth);
	}
	
	static TableAreaStatistics getTableAreaStatistics(ImPage page, ImWord[] words, float normSpaceWidth) {
		
		//	compute initial statistics
		TableAreaStatistics tas = new TableAreaStatistics(page, words, normSpaceWidth);
		
		//	progressively increase normalized space width if column gap distribution all too heterogeneous
		while (true) {
			
			//	compute average, minimum, and maximum column margin, as well as distribution
			System.out.println("Raw column occupation lows are " + tas.getBrgColOccupationLows());
			System.out.println("Filtered column occupation lows are " + tas.getBrgColOccupationLowsFiltered());
			System.out.println("Cleaned column occupation lows are " + tas.getBrgColOccupationLowsCleaned());
			TreeSet colOccupationLows = tas.getBrgColOccupationLowsReduced();
			System.out.println("Column occupation lows are " + colOccupationLows);
			if (colOccupationLows.isEmpty())
				break; // catch total absence of lows (it _can_ happen if we increase the minimum margin all too far)
			int colMarginSum = 0;
			int minColMargin = Integer.MAX_VALUE;
			int maxColMargin = 0;
			CountingSet colOccupationLowWidths = new CountingSet(new TreeMap());
			for (Iterator colit = colOccupationLows.iterator(); colit.hasNext();) {
				ColumnOccupationLow col = ((ColumnOccupationLow) colit.next());
				colMarginSum += col.getWidth();
				minColMargin = Math.min(minColMargin, col.getWidth());
				maxColMargin = Math.max(maxColMargin, col.getWidth());
				colOccupationLowWidths.add(new Integer(col.getWidth()));
			}
			System.out.println(" - average word height is " + tas.avgWordHeight);
			float colGapFract = tas.getBrgColOccupationLowFractReduced();
			System.out.println(" - column gap fraction is " + colGapFract);
			int avgColMargin = ((colMarginSum + (colOccupationLows.size() / 2)) / colOccupationLows.size());
			System.out.println(" - average column occupation low width is " + avgColMargin + " (" + minColMargin + "/" + maxColMargin + "):");
			for (Iterator cmit = colOccupationLowWidths.iterator(); cmit.hasNext();) {
				Integer colMargin = ((Integer) cmit.next());
				System.out.println("   - " + colMargin + ": " + colOccupationLowWidths.getCount(colMargin));
			}
			
			//	minimum column margin has reached average word height, that's but enough in any case
			//	TODO figure out if we really need this cutoff, uninhibited iteration seems to converge just fine
			if (tas.avgWordHeight < minColMargin)
				break;
			
			//	separate column gaps at average
			CountingSet lowColOccupationLowWidths = new CountingSet(new TreeMap());
			CountingSet highColOccupationLowWidths = new CountingSet(new TreeMap());
			for (Iterator colit = colOccupationLows.iterator(); colit.hasNext();) {
				ColumnOccupationLow col = ((ColumnOccupationLow) colit.next());
				if (col.getWidth() <= avgColMargin)
					lowColOccupationLowWidths.add(new Integer(col.getWidth()));
				else highColOccupationLowWidths.add(new Integer(col.getWidth()));
			}
			
			//	assess low fraction
			int lowColMarginSum = 0;
			int maxLowColMargin = 0;
			for (Iterator lcolit = lowColOccupationLowWidths.iterator(); lcolit.hasNext();) {
				Integer lColMargin = ((Integer) lcolit.next());
				lowColMarginSum += (lColMargin.intValue() * lowColOccupationLowWidths.getCount(lColMargin));
				maxLowColMargin = Math.max(maxLowColMargin, lColMargin.intValue());
			}
			int avgLowColMargin = (lowColOccupationLowWidths.isEmpty() ? 0 : ((lowColMarginSum + (lowColOccupationLowWidths.size() / 2)) / lowColOccupationLowWidths.size()));
			System.out.println(" - average low column occupation low width is " + avgLowColMargin + " (" + minColMargin + "/" + maxLowColMargin + "):");
			for (Iterator cmit = lowColOccupationLowWidths.iterator(); cmit.hasNext();) {
				Integer colMargin = ((Integer) cmit.next());
				System.out.println("   - " + colMargin + ": " + lowColOccupationLowWidths.getCount(colMargin));
			}
			
			//	assess high fraction
			int highColMarginSum = 0;
			int minHighColMargin = Integer.MAX_VALUE;
			for (Iterator hcolit = highColOccupationLowWidths.iterator(); hcolit.hasNext();) {
				Integer hColMargin = ((Integer) hcolit.next());
				highColMarginSum += (hColMargin.intValue() * highColOccupationLowWidths.getCount(hColMargin));
				minHighColMargin = Math.min(minHighColMargin, hColMargin.intValue());
			}
			int avgHighColMargin = (highColOccupationLowWidths.isEmpty() ? 0 : ((highColMarginSum + (highColOccupationLowWidths.size() / 2)) / highColOccupationLowWidths.size()));
			System.out.println(" - average high column occupation low width is " + avgHighColMargin + " (" + minHighColMargin + "/" + maxColMargin + "):");
			for (Iterator cmit = highColOccupationLowWidths.iterator(); cmit.hasNext();) {
				Integer colMargin = ((Integer) cmit.next());
				System.out.println("   - " + colMargin + ": " + highColOccupationLowWidths.getCount(colMargin));
			}
			
			//	high fraction all too far from low fraction, and low fraction well in range of in-cell word spacing (assuming actual column gaps are somewhat regular)
			if ((highColOccupationLowWidths.size() > 1) && ((maxLowColMargin * 2) < minHighColMargin) && ((maxLowColMargin * 2) < tas.avgWordHeight)) {
				float rNormSpaceWidth = (((float) avgColMargin) / tas.avgWordHeight);
				if (rNormSpaceWidth <= tas.normSpaceWidth)
					break; // no use reducing normalized space width in this hill climbing approach
				System.out.println(" - testing increased normalized space width of " + rNormSpaceWidth + " (up from " + tas.normSpaceWidth + ")");
				TableAreaStatistics rTas = new TableAreaStatistics(page, words, rNormSpaceWidth);
				float rColGapFract = rTas.getBrgColOccupationLowFractReduced();
				System.out.println(" --> column gap fraction is " + rColGapFract + " (was " + colGapFract + " before)");
				tas = rTas;
			}
			
			//	minimum all too far below average, cut to half average (assuming actual column gaps are somewhat regular at least towards lower end)
			else if ((minColMargin * 2) < avgColMargin) {
//				float rNormSpaceWidth = Math.min((tas.normSpaceWidth * 1.33f), (((float) avgColMargin) / (tas.avgWordHeight * 2)));
				float rNormSpaceWidth = Math.min((((float) avgColMargin) / (tas.avgWordHeight * 2)), 1); // cap off at 1 (no in-cell space is wider than average word height)
				if (rNormSpaceWidth <= tas.normSpaceWidth)
					break; // no use reducing normalized space width in this hill climbing approach
				System.out.println(" - testing increased normalized space width of " + rNormSpaceWidth + " (up from " + tas.normSpaceWidth + ")");
				TableAreaStatistics rTas = new TableAreaStatistics(page, words, rNormSpaceWidth);
				float rColGapFract = rTas.getBrgColOccupationLowFractReduced();
				System.out.println(" --> column gap fraction is " + rColGapFract + " (was " + colGapFract + " before)");
				if (rColGapFract <= colGapFract)
					break; // we're trying to close spaces in multi-column cells to bring the gaps they obstruct to bear in full
				tas = rTas;
			}
			
			//	looks like we simply have a pretty dense table ...
			else break;
		}
		
		//	finally ...
		return tas;
	}}

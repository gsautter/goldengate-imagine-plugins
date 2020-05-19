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

import java.awt.Shape;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.DocumentStyle;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImLayoutObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImSupplement;
import de.uka.ipd.idaho.im.ImSupplement.Graphics;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractGoldenGateImaginePlugin;
import de.uka.ipd.idaho.im.imagine.plugins.tables.TableAreaStatistics.ColumnOccupationLow;
import de.uka.ipd.idaho.im.imagine.plugins.tables.TableAreaStatistics.RowOccupationGap;
import de.uka.ipd.idaho.im.util.ImDocumentIO;
import de.uka.ipd.idaho.im.util.ImUtils;

/**
 * @author sautter
 *
 */
public class TableDetectorProviderTest extends AbstractGoldenGateImaginePlugin {
	
	/** public zero-argument constructor for class loading */
	public TableDetectorProviderTest() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Table Detector";
	}
	
	private static RowStripe[] getRowStripes(ImPage page, BoundingBox regionBounds) {
		
		//	get and sort words (top-down order assures proper row list carrying in stripe identification)
		ImWord[] words = page.getWordsInside(regionBounds);
		Arrays.sort(words, ImUtils.topDownOrder);
		
		//	find horizontal stripes (can comprise multiple lines)
		ArrayList[] stripeWordLists = new ArrayList[regionBounds.bottom - regionBounds.top];
		Arrays.fill(stripeWordLists, null);
		for (int w = 0; w < words.length; w++) {
			if (ImWord.TEXT_STREAM_TYPE_DELETED.equals(words[w].getTextStreamType())) {}
			else if (ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(words[w].getTextStreamType())) {}
			else if (ImWord.TEXT_STREAM_TYPE_PAGE_TITLE.equals(words[w].getTextStreamType())) {}
			else for (int r = Math.max(regionBounds.top, words[w].bounds.top); r < Math.min(regionBounds.bottom, words[w].bounds.bottom); r++) {
				if ((r == regionBounds.top) || (stripeWordLists[r - regionBounds.top - 1] == null))
					stripeWordLists[r - regionBounds.top] = new ArrayList();
				else stripeWordLists[r - regionBounds.top] = stripeWordLists[r - regionBounds.top - 1];
			}
		}
		
		//	sort words into stripes
		for (int w = 0; w < words.length; w++) {
			if (ImWord.TEXT_STREAM_TYPE_DELETED.equals(words[w].getTextStreamType())) {}
			else if (ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(words[w].getTextStreamType())) {}
			else if (ImWord.TEXT_STREAM_TYPE_PAGE_TITLE.equals(words[w].getTextStreamType())) {}
			else stripeWordLists[Math.max(regionBounds.top, words[w].bounds.top) - regionBounds.top].add(words[w]);
		}
		
		//	generate stripes
		ArrayList stripeList = new ArrayList();
		for (int r = 0; r < stripeWordLists.length; r++) {
			if (stripeWordLists[r] == null)
				continue;
			if (stripeWordLists[r].isEmpty())
				continue;
			if ((r != 0) && (stripeWordLists[r] == stripeWordLists[r-1]))
				continue;
			stripeList.add(new RowStripe((ImWord[]) stripeWordLists[r].toArray(new ImWord[stripeWordLists[r].size()])));
		}
		return ((RowStripe[]) stripeList.toArray(new RowStripe[stripeList.size()]));
	}
	
	private static class RowStripe {
		final ImWord[] words;
		final int wordWidthSum;
		final int wordHeightSum;
		final int wordArea;
		final int wordDistanceCount;
		final int minWordDistance;
		final int maxWordDistance;
		final int wordDistanceSum;
		final int wordDistanceSquareSum;
		final float nMinWordDistance;
		final float nMaxWordDistance;
		final float nWordDistanceSum;
		final float nWordDistanceSquareSum;
		final BoundingBox bounds;
		final int area;
		final boolean[] colHasWord;
		final char[] colType;
		ColumnAnchor[] colAnchors;
		ColumnAnchor[] compatibleColAnchors;
		int colCount = 0;
//		float aSharedWordCols = 0;
//		float aSharedNonWordCols = 0;
//		float bSharedWordCols = 0;
//		float bSharedNonWordCols = 0;
		ColumnCompatibility aColCompatibility;
		ColumnCompatibility bColCompatibility;
		float sharedWordCols = 0;
		float sharedNonWordCols = 0;
//		float aFwSharedWordCols = 0;
//		float aFwSharedNonWordCols = 0;
		ColumnCompatibility aFwColCompatibility;
		int aFwMaxSeqSharedNonWordCols = 0;
		float aFwAvgSeqSharedNonWordCols = 0;
		float aFwMatchTypeCols = 0;
//		float bFwSharedWordCols = 0;
//		float bFwSharedNonWordCols = 0;
		ColumnCompatibility bFwColCompatibility;
		int bFwMaxSeqSharedNonWordCols = 0;
		float bFwAvgSeqSharedNonWordCols = 0;
		float bFwMatchTypeCols = 0;
		int aSpace = Short.MAX_VALUE; // not using Integer.MAX_VALUE to prevent having to handle arithmetic overflows
		int bSpace = Short.MAX_VALUE; // not using Integer.MAX_VALUE to prevent having to handle arithmetic overflows
		boolean isCaptionOrNote = false;
		RowStripe(ImWord[] words) {
			Arrays.sort(words, ImUtils.leftRightOrder);
			this.words = words;
			int wordArea = 0;
			int wordWidthSum = 0;
			int wordHeightSum = 0; // using average word height as proxy for both font size and DPI
			int wordDistanceCount = 0;
			int minWordDistance = Integer.MAX_VALUE;
			int maxWordDistance = 0;
			int wordDistanceSum = 0;
			int wordDistanceSquareSum = 0;
			for (int w = 0; w < this.words.length; w++) {
				wordArea += getArea(this.words[w]);
				wordWidthSum += (this.words[w].bounds.right - this.words[w].bounds.left);
				wordHeightSum += (this.words[w].bounds.bottom - this.words[w].bounds.top);
				if (w != 0) {
					int wordDistance = (this.words[w].bounds.left - this.words[w-1].bounds.right);
					if (wordDistance > 0) {
						wordDistanceCount++;
						minWordDistance = Math.min(minWordDistance, wordDistance);
						maxWordDistance = Math.max(maxWordDistance, wordDistance);
						wordDistanceSum += wordDistance;
						wordDistanceSquareSum += (wordDistance * wordDistance);
					}
				}
			}
			
			//	compute overall word stats
			this.wordWidthSum = wordWidthSum;
			this.wordHeightSum = wordHeightSum;
			this.wordArea = wordArea;
			this.wordDistanceCount = wordDistanceCount;
			this.minWordDistance = ((minWordDistance < Integer.MAX_VALUE) ? minWordDistance : -1);
			this.maxWordDistance = ((maxWordDistance > 0) ? maxWordDistance : -1);
			this.wordDistanceSum = wordDistanceSum;
			this.wordDistanceSquareSum = wordDistanceSquareSum;
			this.nMinWordDistance = ((minWordDistance < Integer.MAX_VALUE) ? (((float) (minWordDistance * this.words.length)) / wordHeightSum) : -1);
			this.nMaxWordDistance = ((maxWordDistance > 0) ? (((float) (maxWordDistance * this.words.length)) / wordHeightSum) : -1);
			this.nWordDistanceSum = (((float) (wordDistanceSum * this.words.length)) / wordHeightSum);
			this.nWordDistanceSquareSum = (((float) (wordDistanceSquareSum * this.words.length * this.words.length)) / (wordHeightSum * wordHeightSum));
			
			//	compute own stats
			this.bounds = ImLayoutObject.getAggregateBox(this.words);
			this.area = getArea(this.bounds);
			
			//	check where words are
			this.colHasWord = new boolean[this.bounds.right - this.bounds.left];
			Arrays.fill(this.colHasWord, false);
			this.colType = new char[this.bounds.right - this.bounds.left];
			Arrays.fill(this.colType, ' '); // initialize to space
			for (int w = 0; w < this.words.length; w++) {
				final char wordType;
				if (Gamta.isRomanNumber(this.words[w].getString()))
					wordType = 'R'; // Roman numeral
				else if (Gamta.isNumber(this.words[w].getString()))
					wordType = 'A'; // Arabic numeral
				else if (this.words[w].getString().length() == 1) {
					if (":.;,/|".indexOf(this.words[w].getString()) != -1)
						wordType = 'S'; // separating punctuation (more common _outside_ tables !!!)
					else if ("?".indexOf(this.words[w].getString()) != -1)
						wordType = 'W'; // wildcard (can occur to indicate an unknown value)
					else if ("-".indexOf(this.words[w].getString()) != -1)
						wordType = 'B'; // blank (can occur to indicate a non-existing value)
					else if ("+".indexOf(this.words[w].getString()) != -1)
						wordType = 'B'; // boolean (need to conflate with 'blank' for +/- columns)
					else if (!Character.isLetter(this.words[w].getString().charAt(0)))
						wordType = 'P'; // general punctuation mark (only need exclude letters, as numbers would have been caught above)
					else wordType = 'T'; // default to text
				}
				else wordType = 'T'; // default to text
				for (int c = this.words[w].bounds.left; c < this.words[w].bounds.right; c++) {
					this.colHasWord[c - this.bounds.left] = true;
					this.colType[c - this.bounds.left] = wordType;
				}
			}
		}
		float getWordDensity() {
//			return ((this.area < 1) ? 0 : (((float) this.wordArea) / this.area));
			//	use horizontal density only to counter effect of superscript or subscript characters expanding row height
			return ((this.bounds.left < this.bounds.right) ? (((float) this.wordWidthSum) / this.bounds.getWidth()) : 0);
		}
		float getWordSpacing() {
//			return ((this.words.length < 2) ? 0 : (((float) this.wordDistanceSum) / (this.words.length - 1)));
			return ((this.wordDistanceCount < 1) ? 0 : (((float) this.wordDistanceSum) / this.wordDistanceCount));
		}
		float getSquareWordSpacing() {
//			return ((this.words.length < 2) ? 0 : (((float) this.wordDistanceSquareSum) / (this.words.length - 1)));
			return ((this.wordDistanceCount < 1) ? 0 : (((float) this.wordDistanceSquareSum) / this.wordDistanceCount));
		}
		float getNormWordSpacing() {
//			return ((this.words.length < 2) ? 0 : (((float) this.nWordDistanceSum) / (this.words.length - 1)));
			return ((this.wordDistanceCount < 1) ? 0 : (((float) this.nWordDistanceSum) / this.wordDistanceCount));
		}
		float getNormSquareWordSpacing() {
//			return ((this.words.length < 2) ? 0 : (((float) this.nWordDistanceSquareSum) / (this.words.length - 1)));
			return ((this.wordDistanceCount < 1) ? 0 : (((float) this.nWordDistanceSquareSum) / this.wordDistanceCount));
		}
		float getWordSpacingQuotient() {
			return ((this.wordDistanceSum < 1) ? 0 : (((float) this.wordDistanceSquareSum) / this.wordDistanceSum));
		}
		int findWordMatching(String pattern) {
			for (int w = 0; w < this.words.length; w++) {
				if (this.words[w].getString().matches(pattern))
					return w;
			}
			return -1;
		}
	}
	
	private static int getArea(ImLayoutObject ilo) {
		return getArea(ilo.bounds);
	}
	
	private static int getArea(BoundingBox bounds) {
		return ((bounds.right - bounds.left) * (bounds.bottom - bounds.top));
	}
	
	private static float getWordDensity(RowStripe[] rowStripes) {
		if (rowStripes.length == 0)
			return 0;
		else if (rowStripes.length == 1)
			return rowStripes[0].getWordDensity();
//		int stripeArea = 0;
//		int wordArea = 0;
		int stripeWidthTotal = 0;
		int wordWidthTotal = 0;
		for (int rs = 0; rs < rowStripes.length; rs++) {
//			stripeArea += rowStripes[rs].area;
//			wordArea += rowStripes[rs].wordArea;
			stripeWidthTotal += rowStripes[rs].bounds.getWidth();
			wordWidthTotal += rowStripes[rs].wordWidthSum;
		}
//		return ((stripeArea < 1) ? 0 : (((float) wordArea) / stripeArea));
		return ((stripeWidthTotal < 1) ? 0 : (((float) wordWidthTotal) / stripeWidthTotal));
	}
	
	private static class ColumnAnchor implements Comparable {
		final char orientation;
		final int col;
		final int score;
		ColumnAnchor(char orientation, int col, int score) {
			this.orientation = orientation;
			this.col = col;
			this.score = score;
		}
		public int compareTo(Object obj) {
			if (obj instanceof ColumnAnchor) {
				ColumnAnchor ca = ((ColumnAnchor) obj);
				if (this.col == ca.col)
					return (this.orientation - ca.orientation);
				else return (this.col - ca.col);
			}
			else return -1;
		}
		public String toString() {
			return (this.orientation + "-" + this.col + " (" + this.score + ")");
		}
		public boolean equals(Object obj) {
			return (this.compareTo(obj) == 0);
		}
	}
	
	private static ColumnAnchor[] getColumnAnchors(RowStripe stripe) {
		
		//	get DPI to cap score and avoid runaway in spare tables
		int dpi = stripe.words[0].getPage().getImageDPI();
		
		//	find and score column anchor points
		ArrayList colAnchorList = new ArrayList();
		for (int c = 0; c < stripe.colHasWord.length; c++) {
			if (!stripe.colHasWord[c])
				continue;
			
			//	check for left anchor
			if (c == 0)
				colAnchorList.add(new ColumnAnchor('L', (c + stripe.bounds.left), (dpi / 2))); // assume half an inch of whitespace, which makes for a good average outer margin
			else if (stripe.colHasWord[c-1]) {}
			else for (int s = 1; s <= c; s++) {
				if (s == c)
					colAnchorList.add(new ColumnAnchor('L', (c + stripe.bounds.left), Math.min(s, dpi))); // cap score to one inch of whitespace to avoid runaway in sparse tables
				else if (stripe.colHasWord[c - s]) {
					colAnchorList.add(new ColumnAnchor('L', (c + stripe.bounds.left), Math.min((s-1), dpi))); // cap score to one inch of whitespace to avoid runaway in sparse tables
					break;
				}
			}
			
			//	check for right anchor
			if ((c+1) == stripe.colHasWord.length)
				colAnchorList.add(new ColumnAnchor('R', (c + stripe.bounds.left), (dpi / 2))); // assume half an inch of whitespace, which makes for a good average outer margin
			else if (stripe.colHasWord[c+1]) {}
			else for (int s = 1; (s + c) <= stripe.colHasWord.length; s++) {
				if ((s + c) == stripe.colHasWord.length)
					colAnchorList.add(new ColumnAnchor('R', (c + stripe.bounds.left), Math.min(s, dpi))); // cap score to one inch of whitespace to avoid runaway in sparse tables
				else if (stripe.colHasWord[c + s]) {
					colAnchorList.add(new ColumnAnchor('R', (c + stripe.bounds.left), Math.min((s-1), dpi))); // cap score to one inch of whitespace to avoid runaway in sparse tables
					break;
				}
			}
		}
		
		//	build middle anchors from left and right ones
		for (int a = 0; a < colAnchorList.size(); a++) {
			ColumnAnchor lColAnchor = ((ColumnAnchor) colAnchorList.get(a));
			if (lColAnchor.orientation == 'M')
				break; // we've reached the added anchors
			if (lColAnchor.orientation == 'R')
				continue; // left end of middle anchor has to be a left anchor
			int spannedColAnchorScores = 0;
			for (int ca = (a+1); ca < colAnchorList.size(); ca++) {
				ColumnAnchor cColAnchor = ((ColumnAnchor) colAnchorList.get(ca));
				if (cColAnchor.orientation == 'M')
					break; // we've reached the added anchors
				if (cColAnchor.orientation == 'L') {
					spannedColAnchorScores += cColAnchor.score; // punish large gaps in columns (more likely to be column margins themselves)
					continue; // right end of middle anchor has to be a right anchor
				}
				int mColAnchorScore = (((lColAnchor.score + cColAnchor.score) / 2) - spannedColAnchorScores);
				if (mColAnchorScore <= 0)
					continue; // spanning all too much whitespace for too small outer margins
				colAnchorList.add(new ColumnAnchor('M', ((lColAnchor.col + cColAnchor.col) / 2), mColAnchorScore));
			}
		}
		
		//	sort anchors
		Collections.sort(colAnchorList);
		
		//	remove duplicates (can happen for middle anchors)
		for (int a = 1; a < colAnchorList.size(); a++) {
			ColumnAnchor lColAnchor = ((ColumnAnchor) colAnchorList.get(a-1));
			if (lColAnchor.orientation != 'M')
				continue;
			ColumnAnchor rColAnchor = ((ColumnAnchor) colAnchorList.get(a));
			if (rColAnchor.orientation != 'M')
				continue;
			if (lColAnchor.col != rColAnchor.col)
				continue;
			if (lColAnchor.score < rColAnchor.score)
				colAnchorList.set((a-1), rColAnchor);
			colAnchorList.remove(a--);
		}
		
		//	finally ...
		return ((ColumnAnchor[]) colAnchorList.toArray(new ColumnAnchor[colAnchorList.size()]));
	}
	
	private static class ColStripe extends CountingSet {
		final int centerCol;
		final int radius;
		int minCol = Integer.MAX_VALUE;
		int maxCol = 0;
		TreeSet cols = new TreeSet();
		int rawScore = 0;
		TreeSet orientations = new TreeSet();
		ArrayList content = new ArrayList();
		double score = -1;
		ColStripe(int centerCol, int radius) {
			super(new TreeMap());
			this.centerCol = centerCol;
			this.radius = radius;
		}
		public boolean add(Object obj) {
			if (obj instanceof ColumnAnchor) {
				ColumnAnchor ca = ((ColumnAnchor) obj);
				if (ca.col < (this.centerCol - this.radius))
					return false; // outside our stripe
				else if (ca.col >= (this.centerCol + this.radius))
					return false; // outside our stripe
				else if (ca.score < this.radius)
					return false; // less than 1mm of space
				else {
					this.minCol = Math.min(this.minCol, ca.col);
					this.maxCol = Math.max(this.maxCol, ca.col);
					this.cols.add(new Integer(ca.col));
					this.rawScore += ca.score;
					this.orientations.add(new Character(ca.orientation));
					this.content.add(ca);
					return super.add(ca);
				}
			}
			else return false;
		}
		int getWidth() {
			return ((this.maxCol < this.minCol) ? 0 : (this.maxCol - this.minCol));
		}
		public String toString() {
			StringBuffer ts = new StringBuffer("[");
			for (Iterator it = this.iterator(); it.hasNext();) {
				Object obj = it.next();
				ts.append(obj + ": " + this.getCount(obj));
				if (it.hasNext())
					ts.append(", ");
			}
			ts.append("]");
			return ts.toString();
		}
	}
	
	private static double scoreColStripe(ColStripe colStripe, RowStripe[] rowStripes) {
		
		//	nothing to score here
		if (colStripe.isEmpty())
			return 0;
		
		//	start with raw score
		double score = colStripe.rawScore;
		
		//	penalize scatter
		score /= Math.sqrt(colStripe.cols.size());
		
		//	penalize spread
		score /= Math.sqrt(colStripe.getWidth() + 1);
		
		//	normalize by row stripes intersected
		BoundingBox colStripeBounds = new BoundingBox((colStripe.centerCol - colStripe.radius), (colStripe.centerCol + colStripe.radius), rowStripes[0].bounds.top, rowStripes[rowStripes.length-1].bounds.bottom);
		int rowStripesIntersected = 0;
		for (int rs = 0; rs < rowStripes.length; rs++) {
			if (colStripeBounds.overlaps(rowStripes[rs].bounds))
				rowStripesIntersected++;
		}
		score /= rowStripesIntersected;
		
		//	reward row stripe coverage (avoid over-rewarding duplicates, though, which can result from middle anchors created slightly off each other from multiple left-right anchor pairings)
//		score *= colStripe.size();
		score *= Math.min(colStripe.size(), rowStripes.length);
		score /= rowStripesIntersected;
		
		//	TODO anything else?
		
		//	finally ...
		return score;
	}
	
	private static class RowStripeGroup {
		final BoundingBox bounds;
		
		final RowStripe[] rowStripes;
		final RowStripe[] fullWidthRowStripes;
		final RowStripe topFullWidthRowStripe;
		final RowStripe bottomFullWidthRowStripe;
		final int fullWidthRowStripeCount;
		
		final boolean[] colHasWord;
		final float wordColDensity;
		
		final byte[] colWordCounts;
		final float avgColWordCount;
		final int minColWordCount;
		final int maxColWordCount;
		
		final byte[] sColWordCounts;
		final float sAvgColWordCount;
		final int sMinColWordCount;
		final int sMaxColWordCount;
//		float aSharedWordCols = 0;
//		float aSharedNonWordCols = 0;
//		float bSharedWordCols = 0;
//		float bSharedNonWordCols = 0;
		ColumnCompatibility aColCompatibility;
		ColumnCompatibility bColCompatibility;
		
		final boolean isCaptionOrNote;
		
		boolean isTable = false;
		boolean isTablePart = false;
		
		RowStripeGroup(RowStripe[] rowStripes, RowStripe[] fullWidthRowStripes) {
			this.rowStripes = rowStripes;
			this.fullWidthRowStripes = fullWidthRowStripes;
			this.topFullWidthRowStripe = this.fullWidthRowStripes[0];
			this.bottomFullWidthRowStripe = this.fullWidthRowStripes[this.fullWidthRowStripes.length-1];
			
			int fullWidthRowStripeCount = 1;
			for (int rs = 1; rs < this.fullWidthRowStripes.length; rs++) {
				if (this.fullWidthRowStripes[rs-1] != this.fullWidthRowStripes[rs])
					fullWidthRowStripeCount++;
			}
			this.fullWidthRowStripeCount = fullWidthRowStripeCount;
			
			int left = Integer.MAX_VALUE;
			int right = 0;
			int top = Integer.MAX_VALUE;
			int bottom = 0;
			for (int rs = 0; rs < this.rowStripes.length; rs++) {
				left = Math.min(left, this.rowStripes[rs].bounds.left);
				right = Math.max(right, this.rowStripes[rs].bounds.right);
				top = Math.min(top, this.rowStripes[rs].bounds.top);
				bottom = Math.max(bottom, this.rowStripes[rs].bounds.bottom);
			}
			this.bounds = new BoundingBox(left, right, top, bottom);
			
			this.colHasWord = new boolean[right - left];
			Arrays.fill(this.colHasWord, false);
			this.colWordCounts = new byte[right - left];
			Arrays.fill(this.colWordCounts, ((byte) 0));
			for (int rs = 0; rs < this.rowStripes.length; rs++) {
				for (int c = 0; c < this.rowStripes[rs].colHasWord.length; c++)
					if (this.rowStripes[rs].colHasWord[c]) {
						this.colHasWord[c + this.rowStripes[rs].bounds.left - this.bounds.left] = true;
						this.colWordCounts[c + this.rowStripes[rs].bounds.left - this.bounds.left]++;
					}
			}
			
			int wordColCount = 0;
			for (int c = 0; c < this.colHasWord.length; c++) {
				if (this.colHasWord[c])
					wordColCount++;
			}
			this.wordColDensity = (((float) wordColCount) / this.colHasWord.length);
			
			int minColWordCount = Integer.MAX_VALUE;
			int maxColWordCount = 0;
			int colWordCountSum = 0;
			for (int c = 0; c < this.colWordCounts.length; c++) {
				minColWordCount = Math.min(minColWordCount, this.colWordCounts[c]);
				maxColWordCount = Math.max(maxColWordCount, this.colWordCounts[c]);
				colWordCountSum += this.colWordCounts[c];
			}
			this.minColWordCount = minColWordCount;
			this.maxColWordCount = maxColWordCount;
			this.avgColWordCount = (((float) colWordCountSum) / this.colWordCounts.length);
			
			//	TODO produce smoothed version of histogram:
			//	TODO - ignore word gaps whose width is below 1/3 of word height (row stripe height should do as well)
			//	TODO - average out columns in a range of DPI/25 
			byte[] sColWordCounts = new byte[right - left];
			Arrays.fill(sColWordCounts, ((byte) 0));
			for (int rs = 0; rs < this.rowStripes.length; rs++) {
				boolean[] sColHasWord = new boolean[this.rowStripes[rs].colHasWord.length];
				System.arraycopy(this.rowStripes[rs].colHasWord, 0, sColHasWord, 0, this.rowStripes[rs].colHasWord.length);
				for (int c = 1; c < sColHasWord.length; c++) {
					if (sColHasWord[c])
						continue;
					if (!sColHasWord[c-1])
						continue;
					int wordGapWidth = 0;
					for (int l = c; l < sColHasWord.length; l++) {
						if (sColHasWord[l])
							break;
						else wordGapWidth++;
					}
					if ((wordGapWidth * 3) < (this.rowStripes[rs].bounds.bottom - this.rowStripes[rs].bounds.top))
						for (int l = c; l < sColHasWord.length; l++) {
							if (sColHasWord[l])
								break;
							else sColHasWord[l] = true;
						}
					c += (wordGapWidth - 1);
				}
				for (int c = 0; c < sColHasWord.length; c++) {
					if (sColHasWord[c])
						sColWordCounts[c + this.rowStripes[rs].bounds.left - this.bounds.left]++;
				}
			}
			
			int sRadius = ((this.rowStripes[0].words[0].getPage().getImageDPI() + 25) / 50);
			this.sColWordCounts = new byte[right - left];
			Arrays.fill(this.sColWordCounts, ((byte) 0));
			for (int c = 0; c < this.sColWordCounts.length; c++) {
				int sLeft = Math.max((c - sRadius), 0);
				int sRight = Math.min((c + sRadius), (this.sColWordCounts.length - 1));
				int sColWordCountSum = 0;
				for (int sc = sLeft; sc <= sRight; sc++)
					sColWordCountSum += sColWordCounts[sc];
				this.sColWordCounts[c] = ((byte) ((sColWordCountSum + ((sRight - sLeft) / 2)) / (sRight - sLeft)));
			}
			
			int sMinColWordCount = Integer.MAX_VALUE;
			int sMaxColWordCount = 0;
			int sColWordCountSum = 0;
			for (int c = 0; c < this.colWordCounts.length; c++) {
				sMinColWordCount = Math.min(sMinColWordCount, this.sColWordCounts[c]);
				sMaxColWordCount = Math.max(sMaxColWordCount, this.sColWordCounts[c]);
				sColWordCountSum += this.sColWordCounts[c];
			}
			this.sMinColWordCount = sMinColWordCount;
			this.sMaxColWordCount = sMaxColWordCount;
			this.sAvgColWordCount = (((float) sColWordCountSum) / this.sColWordCounts.length);
			
			boolean isCaptionOrNote = false;
			for (int rs = 0; rs < this.rowStripes.length; rs++)
				if (this.rowStripes[rs].isCaptionOrNote) {
					isCaptionOrNote = true;
					break;
				}
			this.isCaptionOrNote = isCaptionOrNote;
		}
		float getDensity() {
			return getWordDensity(this.rowStripes);
		}
		float getColWordContrast(int width) {
			return computeColWordContrast(this.colWordCounts, width);
		}
		float getSmoothColWordContrast(int width) {
			return computeColWordContrast(this.sColWordCounts, width);
		}
	}
	
	private static float computeColWordContrast(byte[] colWordCounts, int width) {
		if (width == 0)
			return 0;
		int cwcDiffSquareSum = 0;
		for (int c = 0; c < (colWordCounts.length - width); c++) {
			int cwcDiff = Math.abs(colWordCounts[c] - colWordCounts[c + width]);
			cwcDiffSquareSum += (cwcDiff * cwcDiff);
		}
		return (((float) cwcDiffSquareSum) / colWordCounts.length);
	}
	
	private static float computeColWordContrast(int left1, byte[] colWordCounts1, int left2, byte[] colWordCounts2, int width) {
		if (width == 0)
			return 0;
		int left = Math.min(left1, left2);
		int right = Math.max((left1 + colWordCounts1.length), (left2 + colWordCounts2.length));
		byte[] colWordCounts = new byte[right - left];
		Arrays.fill(colWordCounts, ((byte) 0));
		for (int c = 0; c < colWordCounts1.length; c++)
			colWordCounts[c + (left1 - left)] += colWordCounts1[c];
		for (int c = 0; c < colWordCounts2.length; c++)
			colWordCounts[c + (left2 - left)] += colWordCounts2[c];
		return computeColWordContrast(colWordCounts, width);
	}
	
	private static RowStripeGroup[] getRowStripeGroups(ImPage page, BoundingBox blockBounds, RowStripe[] blockStripes) {
		System.out.println("Assessing block " + page.pageId + "." + blockBounds);
//		RowStripe[] blockStripes = getRowStripes(page, blockBounds);
		if (blockStripes == null)
			blockStripes = getRowStripes(page, blockBounds);
		System.out.println(" - got " + blockStripes.length + " stripes");
//		CountingSet colAnchors = new CountingSet(new TreeMap());
//		CountingSet colAnchorScores = new CountingSet(new TreeMap());
		
		//	get column anchors and sort them into vertical stripes
		int colStripeRadius = (page.getImageDPI() / 25); // little over 1mm, resulting in 2mm stripes
		ColStripe[] colStripes = new ColStripe[(blockBounds.right - blockBounds.left + colStripeRadius - 1) / colStripeRadius];
		for (int cs = 0; cs < colStripes.length; cs++)
			colStripes[cs] = new ColStripe((blockBounds.left + (cs * colStripeRadius)), colStripeRadius);
		for (int rs = 0; rs < blockStripes.length; rs++) {
			blockStripes[rs].colAnchors = getColumnAnchors(blockStripes[rs]);
			for (int cs = 0; cs < colStripes.length; cs++)
				colStripes[cs].addAll(Arrays.asList(blockStripes[rs].colAnchors));
//			for (int a = 0; a < sColAnchors.length; a++) {
//				String colAnchorStr = (sColAnchors[a].orientation + "-" + sColAnchors[a].col);
//				colAnchors.add(colAnchorStr);
//				colAnchorScores.add(colAnchorStr, sColAnchors[a].score);
//			}
		}
//		System.out.println(" - got " + colAnchors.size() + " column anchors, " + colAnchors.elementCount() + " distinct ones:");
//		for (Iterator casit = colAnchors.iterator(); casit.hasNext();) {
//			String colAnchorStr = ((String) casit.next());
//			int colAnchorTimes = colAnchors.getCount(colAnchorStr);
//			int colAnchorScore = colAnchorScores.getCount(colAnchorStr);
//			System.out.println("   - " + colAnchorStr + " (" + colAnchorTimes + " times): " + colAnchorScore + " --> " + (((float) colAnchorScore) / colAnchorTimes));
//		}
		System.out.println(" - got " + colStripes.length + " column anchor stripes of radius " + colStripeRadius +":");
		ArrayList colStripeList = new ArrayList();
		for (int cs = 0; cs < colStripes.length; cs++) {
			
			//	use single-orientation stripe directly
			if (colStripes[cs].orientations.size() < 2) {
//				System.out.println("   - " + colStripes[cs].centerCol + " (" + colStripes[cs].getWidth() + "): " + colStripes[cs].size() + " anchors with total score of " + colStripes[cs].rawScore + " " + colStripes[cs]);
				colStripes[cs].score = scoreColStripe(colStripes[cs], blockStripes);
//				System.out.println("     --> " + colStripes[cs].score);
				colStripeList.add(colStripes[cs]);
			}
			
			//	break up mixed-orientation stripes
			else for (Iterator oit = colStripes[cs].orientations.iterator(); oit.hasNext();) {
				Character orientation = ((Character) oit.next());
				ColStripe oColStripe = new ColStripe(colStripes[cs].centerCol, colStripes[cs].radius);
				for (int a = 0; a < colStripes[cs].content.size(); a++) {
					ColumnAnchor colAnchor = ((ColumnAnchor) colStripes[cs].content.get(a));
					if (colAnchor.orientation == orientation.charValue())
						oColStripe.add(colAnchor);
				}
//				System.out.println("   - " + oColStripe.centerCol + " (" + oColStripe.getWidth() + "): " + oColStripe.size() + " anchors with total score of " + oColStripe.rawScore + " " + oColStripe);
				oColStripe.score = scoreColStripe(oColStripe, blockStripes);
//				System.out.println("     --> " + oColStripe.score);
				colStripeList.add(oColStripe);
			}
		}
		
		//	eliminate duplicates (anchor stripes that contain the very same column anchors, which can happen due to overlap)
		for (int s = 1; s < colStripeList.size(); s++) {
			ColStripe lColStripe = ((ColStripe) colStripeList.get(s-1));
			ColStripe rColStripe = ((ColStripe) colStripeList.get(s));
			if (lColStripe.cols.equals(rColStripe.cols))
				colStripeList.remove(s--);
		}
		
		//	TODO maybe check what an anchor stripe's score would be if broken down to individual anchors or narrower stripes
		
		/* find top score (excluding leftmost left anchor and rightmost right
		 * anchor, which always exist in plain text as well, and get get an
		 * artificial score), averaging out top 3 or top 5 anchors instead of
		 * using single maximum to avoid runaway in situations of one extremely
		 * high scoring anchor caused by largely blank preceding or following
		 * column */
		double maxColStripeScore = 0;
		ArrayList maxColStripeScores = new ArrayList(colStripeList.size());
		for (int s = 0; s < colStripeList.size(); s++) {
			ColStripe colStripe = ((ColStripe) colStripeList.get(s));
			if (colStripe.orientations.contains(new Character('L')) && (colStripe.centerCol <= (blockBounds.left + (page.getImageDPI() / 4))))
				continue;
			if (colStripe.orientations.contains(new Character('R')) && (colStripe.centerCol >= (blockBounds.right - (page.getImageDPI() / 4))))
				continue;
			maxColStripeScore = Math.max(maxColStripeScore, colStripe.score);
			maxColStripeScores.add(new Double(colStripe.score));
		}
		Collections.sort(maxColStripeScores, Collections.reverseOrder());
		while (maxColStripeScores.size() > 5) // TODO edit threshold here, and only here
			maxColStripeScores.remove(maxColStripeScores.size()-1);
		double maxColStripeScoreSum = 0;
		for (int s = 0; s < maxColStripeScores.size(); s++)
			maxColStripeScoreSum += ((Double) maxColStripeScores.get(s)).doubleValue();
		maxColStripeScore = (maxColStripeScoreSum / maxColStripeScores.size());
		
		//	extract top scoring column anchor stripes, and compute their average number of rows
		System.out.println(" - top column anchor stripe score is " + maxColStripeScore + ", good anchor stripes are:");
		ArrayList topColStripeList = new ArrayList();
		int topColStripeRowCountSum = 0;
		for (int s = 0; s < colStripeList.size(); s++) {
			ColStripe colStripe = ((ColStripe) colStripeList.get(s));
			if (colStripe.size() < 2)
				continue; // let's only consider column anchors that exist at least twice
			if ((colStripe.score * 5) > maxColStripeScore) { // TODO assess 20% threshold
				topColStripeList.add(colStripe);
				topColStripeRowCountSum += colStripe.size();
				System.out.println("   - " + colStripe.centerCol + " (" + colStripe.getWidth() + "): " + colStripe.size() + " anchors with total score of " + colStripe.score + " (" + colStripe.rawScore + ") " + colStripe);
			}
		}
		int topColStripeRowCount = (topColStripeRowCountSum / topColStripeList.size());
		
		//	sort out column anchor stripes whose multiplicity is less than one third of average (those anchors just exist in too few rows)
		System.out.println(" - top column anchor stripe row count is " + topColStripeRowCount + ", good anchor stripes left are:");
		for (int s = 0; s < topColStripeList.size(); s++) {
			ColStripe colStripe = ((ColStripe) topColStripeList.get(s));
			if ((colStripe.size() * 3) < topColStripeRowCount) // TODO assess 33% threshold
				topColStripeList.remove(s--);
			else System.out.println("   - " + colStripe.centerCol + " (" + colStripe.getWidth() + "): " + colStripe.size() + " anchors with total score of " + colStripe.score + " (" + colStripe.rawScore + ") " + colStripe);
		}
		
		//	eliminate duplicates again (anchor stripes that contain the very same column anchors, which can happen due to overlap)
		for (int s = 1; s < topColStripeList.size(); s++) {
			ColStripe lColStripe = ((ColStripe) topColStripeList.get(s-1));
			ColStripe rColStripe = ((ColStripe) topColStripeList.get(s));
			if (lColStripe.cols.equals(rColStripe.cols))
				topColStripeList.remove(s--);
		}
		
		/* re-compute average score (still excluding leftmost left anchor and
		 * rightmost right anchor, which always exist in plain text as well, and
		 * get get an artificial score), averaging out top 3 or top 5 anchors
		 * instead of using single maximum to avoid runaway in situations of one
		 * extremely high scoring anchor caused by largely blank preceding or
		 * following column */
		maxColStripeScore = 0;
		maxColStripeScores.clear();
		for (int s = 0; s < topColStripeList.size(); s++) {
			ColStripe colStripe = ((ColStripe) topColStripeList.get(s));
			if (colStripe.orientations.contains(new Character('L')) && (colStripe.centerCol <= (blockBounds.left + (page.getImageDPI() / 4))))
				continue;
			if (colStripe.orientations.contains(new Character('R')) && (colStripe.centerCol >= (blockBounds.right - (page.getImageDPI() / 4))))
				continue;
			maxColStripeScore = Math.max(maxColStripeScore, colStripe.score);
			maxColStripeScores.add(new Double(colStripe.score));
		}
		Collections.sort(maxColStripeScores, Collections.reverseOrder());
		while (maxColStripeScores.size() > 5) // TODO edit threshold here, and only here
			maxColStripeScores.remove(maxColStripeScores.size()-1);
		maxColStripeScoreSum = 0;
		for (int s = 0; s < maxColStripeScores.size(); s++)
			maxColStripeScoreSum += ((Double) maxColStripeScores.get(s)).doubleValue();
		maxColStripeScore = (maxColStripeScoreSum / maxColStripeScores.size());
		
		//	sort out column anchor stripes based on new average, and re-compute average row count
		System.out.println(" - top column anchor stripe score re-computed as " + maxColStripeScore + ", good anchor stripes left are:");
		topColStripeRowCountSum = 0;
		for (int s = 0; s < topColStripeList.size(); s++) {
			ColStripe colStripe = ((ColStripe) topColStripeList.get(s));
			if ((colStripe.score * 3) < maxColStripeScore) // TODO assess 33% threshold
				topColStripeList.remove(s--);
			else {
				topColStripeRowCountSum += colStripe.size();
				System.out.println("   - " + colStripe.centerCol + " (" + colStripe.getWidth() + "): " + colStripe.size() + " anchors with total score of " + colStripe.score + " (" + colStripe.rawScore + ") " + colStripe);
			}
		}
		topColStripeRowCount = (topColStripeRowCountSum / topColStripeList.size());
		
		//	eliminate duplicates again (anchor stripes that contain the very same column anchors, which can happen due to overlap)
		for (int s = 1; s < topColStripeList.size(); s++) {
			ColStripe lColStripe = ((ColStripe) topColStripeList.get(s-1));
			ColStripe rColStripe = ((ColStripe) topColStripeList.get(s));
			if (lColStripe.cols.equals(rColStripe.cols))
				topColStripeList.remove(s--);
		}
		
		//	sort out column anchor stripes whose multiplicity is less than one third of new average (those anchors just exist in too few rows)
		System.out.println(" - top column anchor stripe row count re-computed as " + topColStripeRowCount + ", good anchor stripes left are:");
		TreeSet topColAnchors = new TreeSet();
		for (int s = 0; s < topColStripeList.size(); s++) {
			ColStripe colStripe = ((ColStripe) topColStripeList.get(s));
			if ((colStripe.size() * 2) < topColStripeRowCount) // TODO assess 50% threshold
				topColStripeList.remove(s--);
			else {
				topColAnchors.addAll(colStripe.content);
				System.out.println("   - " + colStripe.centerCol + " (" + colStripe.getWidth() + "): " + colStripe.size() + " anchors with total score of " + colStripe.score + " (" + colStripe.rawScore + ") " + colStripe);
			}
		}
		
		//	for each row stripe, assess fraction of whitespace and non-whitespace shared with neighbors (between full extent of either stripe, implying respective whitespace padding to account for leading and tailing empty cells)
		for (int rs = 0; rs < blockStripes.length; rs++) {
			RowStripe aRowStripe = ((rs == 0) ? null : blockStripes[rs-1]);
			RowStripe rowStripe = blockStripes[rs];
			RowStripe bRowStripe = (((rs+1) == blockStripes.length) ? null : blockStripes[rs+1]);
			if (aRowStripe != null)
				rowStripe.aColCompatibility = getColumnCompatibility(aRowStripe.bounds.left, aRowStripe.colHasWord, rowStripe.bounds.left, rowStripe.colHasWord, true);
			if (bRowStripe != null)
				rowStripe.bColCompatibility = getColumnCompatibility(rowStripe.bounds.left, rowStripe.colHasWord, bRowStripe.bounds.left, bRowStripe.colHasWord, true);
		}
		
		//	compute pixel column occupation for whole block
		int bMinCol = Integer.MAX_VALUE;
		int bMaxCol = 0;
		for (int rs = 0; rs < blockStripes.length; rs++) {
			bMinCol = Math.min(bMinCol, blockStripes[rs].bounds.left);
			bMaxCol = Math.max(bMaxCol, blockStripes[rs].bounds.right);
		}
		int[] colWordCount = new int[bMaxCol - bMinCol];
		Arrays.fill(colWordCount, 0);
		for (int rs = 0; rs < blockStripes.length; rs++)
			for (int c = 0; c < blockStripes[rs].colHasWord.length; c++) {
				if (blockStripes[rs].colHasWord[c])
					colWordCount[c + blockStripes[rs].bounds.left - bMinCol]++;
			}
		
		//	compare each row stripe to overall pixel column occupation
		for (int rs = 0; rs < blockStripes.length; rs++) {
			int wordColScore = 0;
			int nonWordColScore = 0;
			for (int c = 0; c < blockStripes[rs].colHasWord.length; c++) {
				if (blockStripes[rs].colHasWord[c])
					wordColScore += colWordCount[c + blockStripes[rs].bounds.left - bMinCol];
				else nonWordColScore += (blockStripes.length - colWordCount[c + blockStripes[rs].bounds.left - bMinCol]);
			}
			blockStripes[rs].sharedWordCols = (((float) wordColScore) / (blockStripes[rs].colHasWord.length * blockStripes.length));
			blockStripes[rs].sharedNonWordCols = (((float) nonWordColScore) / (blockStripes[rs].colHasWord.length * blockStripes.length));
		}
		
		//	for each row stripe, compute space above and below, and also compute average TODO consider normalizing to DPI
		int blockStripeSpaceSum = 0;
		for (int rs = 0; rs < blockStripes.length; rs++) {
			RowStripe aRowStripe = ((rs == 0) ? null : blockStripes[rs-1]);
			RowStripe rowStripe = blockStripes[rs];
			RowStripe bRowStripe = (((rs+1) == blockStripes.length) ? null : blockStripes[rs+1]);
			if (aRowStripe != null) {
				rowStripe.aSpace = (rowStripe.bounds.top - aRowStripe.bounds.bottom);
				blockStripeSpaceSum += (rowStripe.bounds.top - aRowStripe.bounds.bottom);
			}
			if (bRowStripe != null)
				rowStripe.bSpace = (bRowStripe.bounds.top - rowStripe.bounds.bottom);
		}
		
		//	generously round up average, as we'll mainly use it on property inheritance to short rows below
		int avgBlockStripeSpace = ((blockStripes.length == 1) ? 0 : ((blockStripeSpaceSum + (blockStripes.length - 2)) / (blockStripes.length - 1)));
		System.out.println(" - average row stripe spacing computed as " + avgBlockStripeSpace);
		
		/* For dealing with short lines of attached captions and table notes in
		 * detection of table areas (as opposed to row stripes only filled in a
		 * single table cell):
		 * - inherit properties (as well as probabilities, and ultimately
		 *   features) from preceding row stripes if row stripe at hand is
		 *   shorter than 1/3 or 1/2 of block width ...
		 * - ... or better from row stripe below or above,depending on which
		 *   distance is (significantly) smaller (by some 10% or 25%, maybe,
		 *   use average otherwise) */
		
		//	identify actual full-width row stripes first TODO maybe use 1/2 of block width for a threshold rather than 1/3
		System.out.println(" - assigning full-width row stripes for area feature inheritance:");
		RowStripe[] fullWidthBlockStripes = new RowStripe[blockStripes.length];
		Arrays.fill(fullWidthBlockStripes, null);
		for (int rs = 0; rs < blockStripes.length; rs++) {
			RowStripe rowStripe = blockStripes[rs];
			if (((rowStripe.bounds.right - rowStripe.bounds.left) * 2) > (blockBounds.right - blockBounds.left)) {
				fullWidthBlockStripes[rs] = rowStripe;
				System.out.println("   - " + rowStripe.words[0].getString() + " " + rowStripe.bounds + " assigned to itself");
			}
		}
		
		//	outwards assign full-width row stripes first (based on significantly lower distance) to prevent just-made assignments from rippling downwards
		for (boolean newFullWidthBlockStripesAssigned = true; newFullWidthBlockStripesAssigned;) {
			newFullWidthBlockStripesAssigned = false;
			for (int rs = 0; rs < blockStripes.length; rs++) {
				if (fullWidthBlockStripes[rs] != null)
					continue;
				RowStripe rowStripe = blockStripes[rs];
				
				//	need to restrict downward assignments to cases where space above current row is at most block average to prevent assigning to short, single-line above-table notes or captions
				if (rowStripe.aSpace <= avgBlockStripeSpace) {
					
					//	compute maximum space before next full-width row
					int maxBelowFullWidthRowSpace = -1;
					for (int crs = (rs+1); crs < blockStripes.length; crs++) {
						maxBelowFullWidthRowSpace = Math.max(maxBelowFullWidthRowSpace, blockStripes[crs].aSpace);
						if (fullWidthBlockStripes[crs] != null) {
							System.out.println("   - found next full-width row stripe below " + rowStripe.words[0].getString() + " " + rowStripe.bounds + ": " + fullWidthBlockStripes[crs].words[0] + " " + fullWidthBlockStripes[crs].bounds + " at maximum downward distance " + maxBelowFullWidthRowSpace);
							break;
						}
						else if ((crs + 1) == blockStripes.length) {
							maxBelowFullWidthRowSpace = -1;
							System.out.println("   - next full-width row stripe below " + rowStripe.words[0].getString() + " " + rowStripe.bounds + " not found");
							break;
						}
					}
					
					//	need to restrict downward assignments to cases where space above current row is less than space before next full-width row to prevent rippling
					if ((maxBelowFullWidthRowSpace == -1) || (rowStripe.aSpace < maxBelowFullWidthRowSpace)) {
						fullWidthBlockStripes[rs] = ((rs == 0) ? null : fullWidthBlockStripes[rs-1]);
						if (fullWidthBlockStripes[rs] != null) {
							newFullWidthBlockStripesAssigned = true;
							System.out.println("     ==> " + rowStripe.words[0].getString() + " " + rowStripe.bounds + " backward assigned to " + fullWidthBlockStripes[rs].words[0] + " " + fullWidthBlockStripes[rs].bounds);
						}
					}
					else System.out.println("     ==> " + rowStripe.words[0].getString() + " " + rowStripe.bounds + " too close to full-width bottom peer at maximum downward distance " + maxBelowFullWidthRowSpace + " and upward distance " + rowStripe.aSpace);
				}
				
				//	need to restrict upward assignments to cases where space below current row is at most block average to prevent assigning to short, single-line below-table notes
				else if (rowStripe.bSpace <= avgBlockStripeSpace) {
					fullWidthBlockStripes[rs] = (((rs+1) == fullWidthBlockStripes.length) ? null : fullWidthBlockStripes[rs+1]);
					if (fullWidthBlockStripes[rs] != null) {
						newFullWidthBlockStripesAssigned = true;
						System.out.println("   - " + rowStripe.words[0].getString() + " " + rowStripe.bounds + " forward assigned to " + fullWidthBlockStripes[rs].words[0] + " " + fullWidthBlockStripes[rs].bounds);
					}
				}
				
				//	this one is out in the boondocks ...
				else System.out.println("   - " + rowStripe.words[0].getString() + " " + rowStripe.bounds + " too far away from peers");
			}
		}
		
		//	self-assign remaining full-width row stripes (should be only those further than block average away from any actual full-width row stripe)
		for (int rs = 0; rs < blockStripes.length; rs++) {
			if (fullWidthBlockStripes[rs] == null) {
				fullWidthBlockStripes[rs] = blockStripes[rs];
				System.out.println("   - " + blockStripes[rs].words[0].getString() + " " + blockStripes[rs].bounds + " reserve assigned to itself");
			}
		}
		
		//	try and compute column occupation similarity for full-width row stripes alone (this time only counting the overlap of both)
		for (int rs = 0; rs < fullWidthBlockStripes.length; rs++) {
			RowStripe aRowStripe = ((rs == 0) ? null : fullWidthBlockStripes[rs-1]);
			RowStripe rowStripe = fullWidthBlockStripes[rs];
			RowStripe bRowStripe = (((rs+1) == fullWidthBlockStripes.length) ? null : fullWidthBlockStripes[rs+1]);
			if ((aRowStripe != null) && (aRowStripe != rowStripe)) {
				rowStripe.aFwColCompatibility = getColumnCompatibility(aRowStripe.bounds.left, aRowStripe.colHasWord, rowStripe.bounds.left, rowStripe.colHasWord, true);
//				int minCol = Math.min(aRowStripe.bounds.left, rowStripe.bounds.left);
//				int maxCol = Math.max(aRowStripe.bounds.right, rowStripe.bounds.right);
				int minCol = Math.max(aRowStripe.bounds.left, rowStripe.bounds.left);
				int maxCol = Math.min(aRowStripe.bounds.right, rowStripe.bounds.right);
				int sWordCols = 0;
				int sNonWordCols = 0;
				int sNonWordColBlockCount = 0;
				int sNonWordColBlockLength = 0;
				int sNonWordColBlockMaxLength = 0;
				int mTypeCols = 0;
				for (int c = minCol; c < maxCol; c++) {
					boolean aWordCol = (((c < aRowStripe.bounds.left) || (c >= aRowStripe.bounds.right)) ? false : aRowStripe.colHasWord[c - aRowStripe.bounds.left]);
					boolean wordCol = (((c < rowStripe.bounds.left) || (c >= rowStripe.bounds.right)) ? false : rowStripe.colHasWord[c - rowStripe.bounds.left]);
					if (aWordCol && wordCol) {
						sWordCols++;
						sNonWordColBlockLength = 0;
					}
					else if (!aWordCol && !wordCol) {
						sNonWordCols++;
						if (sNonWordColBlockLength == 0)
							sNonWordColBlockCount++;
						sNonWordColBlockLength++;
						sNonWordColBlockMaxLength = Math.max(sNonWordColBlockMaxLength, sNonWordColBlockLength);
						continue; // no use checking type on two spaces
					}
					else {
						sNonWordColBlockLength = 0;
						continue; // no use checking if either row has space
					}
					char aColType = (((c < aRowStripe.bounds.left) || (c >= aRowStripe.bounds.right)) ? ' ' : aRowStripe.colType[c - aRowStripe.bounds.left]);
					char colType = (((c < rowStripe.bounds.left) || (c >= rowStripe.bounds.right)) ? ' ' : rowStripe.colType[c - rowStripe.bounds.left]);
					if (aColType == colType)
						mTypeCols++;
					else if ("WB".indexOf(aColType) != -1)
						mTypeCols++; // wildcard or blank always matches
					else if ("WB".indexOf(colType) != -1)
						mTypeCols++; // wildcard or blank always matches
				}
				if (minCol < maxCol) {
					rowStripe.aFwMaxSeqSharedNonWordCols = sNonWordColBlockMaxLength;
					if (sNonWordColBlockCount != 0)
						rowStripe.aFwAvgSeqSharedNonWordCols = (((float) sNonWordCols) / sNonWordColBlockCount);
					rowStripe.aFwMatchTypeCols = (((float) mTypeCols) / (maxCol - minCol));
				}
			}
			if ((bRowStripe != null) && (bRowStripe != rowStripe)) {
				rowStripe.bFwColCompatibility = getColumnCompatibility(rowStripe.bounds.left, rowStripe.colHasWord, bRowStripe.bounds.left, bRowStripe.colHasWord, true);
//				int minCol = Math.min(rowStripe.bounds.left, bRowStripe.bounds.left);
//				int maxCol = Math.max(rowStripe.bounds.right, bRowStripe.bounds.right);
				int minCol = Math.max(rowStripe.bounds.left, bRowStripe.bounds.left);
				int maxCol = Math.min(rowStripe.bounds.right, bRowStripe.bounds.right);
				int sWordCols = 0;
				int sNonWordCols = 0;
				int sNonWordColBlockCount = 0;
				int sNonWordColBlockLength = 0;
				int sNonWordColBlockMaxLength = 0;
				int mTypeCols = 0;
				for (int c = minCol; c < maxCol; c++) {
					boolean wordCol = (((c < rowStripe.bounds.left) || (c >= rowStripe.bounds.right)) ? false : rowStripe.colHasWord[c - rowStripe.bounds.left]);
					boolean bWordCol = (((c < bRowStripe.bounds.left) || (c >= bRowStripe.bounds.right)) ? false : bRowStripe.colHasWord[c - bRowStripe.bounds.left]);
					if (wordCol && bWordCol) {
						sWordCols++;
						sNonWordColBlockLength = 0;
					}
					else if (!wordCol && !bWordCol) {
						sNonWordCols++;
						if (sNonWordColBlockLength == 0)
							sNonWordColBlockCount++;
						sNonWordColBlockLength++;
						sNonWordColBlockMaxLength = Math.max(sNonWordColBlockMaxLength, sNonWordColBlockLength);
						continue; // no use checking type on two spaces
					}
					else {
						sNonWordColBlockLength = 0;
						continue; // no use checking if either row has space
					}
					char colType = (((c < rowStripe.bounds.left) || (c >= rowStripe.bounds.right)) ? ' ' : rowStripe.colType[c - rowStripe.bounds.left]);
					char bColType = (((c < bRowStripe.bounds.left) || (c >= bRowStripe.bounds.right)) ? ' ' : bRowStripe.colType[c - bRowStripe.bounds.left]);
					if (bColType == colType)
						mTypeCols++;
					else if ("WB".indexOf(colType) != -1)
						mTypeCols++; // wildcard or blank always matches
					else if ("WB".indexOf(bColType) != -1)
						mTypeCols++; // wildcard or blank always matches
				}
				if (minCol < maxCol) {
					rowStripe.bFwMaxSeqSharedNonWordCols = sNonWordColBlockMaxLength;
					if (sNonWordColBlockCount != 0)
						rowStripe.bFwAvgSeqSharedNonWordCols = (((float) sNonWordCols) / sNonWordColBlockCount);
					rowStripe.bFwMatchTypeCols = (((float) mTypeCols) / (maxCol - minCol));
				}
			}
		}
		
		//	clean up row stripes' column anchors
		for (int rs = 0; rs < blockStripes.length; rs++) {
			if (blockStripes[rs].colAnchors == null)
				continue;
			
			//	clean up
			ArrayList rowStripeColAnchors = new ArrayList();
			for (int a = 0; a < blockStripes[rs].colAnchors.length; a++) {
				if (topColAnchors.contains(blockStripes[rs].colAnchors[a]))
					rowStripeColAnchors.add(blockStripes[rs].colAnchors[a]);
			}
			blockStripes[rs].colAnchors = ((ColumnAnchor[]) rowStripeColAnchors.toArray(new ColumnAnchor[rowStripeColAnchors.size()]));
			
			//	get compatible top column anchors
			ArrayList compRowStripeColAnchors = new ArrayList();
			for (Iterator tcait = topColAnchors.iterator(); tcait.hasNext();) {
				ColumnAnchor topColAnchor = ((ColumnAnchor) tcait.next());
				if (topColAnchor.col < blockStripes[rs].bounds.left)
					compRowStripeColAnchors.add(topColAnchor);
				else if (topColAnchor.col > blockStripes[rs].bounds.right)
					compRowStripeColAnchors.add(topColAnchor);
				else if (rowStripeColAnchors.contains(topColAnchor))
					compRowStripeColAnchors.add(topColAnchor);
			}
			blockStripes[rs].compatibleColAnchors = ((ColumnAnchor[]) compRowStripeColAnchors.toArray(new ColumnAnchor[compRowStripeColAnchors.size()]));
			
			//	try and establish table column count from sequences of L, M, and R column anchors
			int colCount = 0;
			int lastColAnchorOrientation = 'R';
			int lastMiddleColAnchorPos = -page.getImageDPI();
			for (int a = 0; a < blockStripes[rs].colAnchors.length; a++) {
				ColumnAnchor colAnchor = blockStripes[rs].colAnchors[a];
				if ((colAnchor.orientation == 'M') && (colAnchor.col < (lastMiddleColAnchorPos + (page.getImageDPI() / 6))))
					continue; // assuming minimum column+gap width of 1/6 inch, let's not count middle anchors if within that range (similar middle anchors can emerge from multiple pairings of left and right anchors, especially in tables with many and evenly distributed columns)
				if (colAnchor.orientation <= lastColAnchorOrientation) // conveniently, L, M, and R also have this order in the alphabet
					colCount++;
				lastColAnchorOrientation = colAnchor.orientation;
				if (colAnchor.orientation == 'M')
					lastMiddleColAnchorPos = colAnchor.col;
			}
			blockStripes[rs].colCount = colCount;
		}
		
		//	TODO try and establish table row count from maximum row count in each column anchor group
		
		//	what do we have?
		System.out.println(" - got " + blockStripes.length + " row stripes:");
		for (int rs = 0; rs < blockStripes.length; rs++) {
			System.out.println("   - " + blockStripes[rs].bounds + ": " + blockStripes[rs].words[0].getString());
			System.out.println("     - vertical spacing: " + blockStripes[rs].aSpace + " above, " + blockStripes[rs].bSpace + " below");
			System.out.println("     - density: " + blockStripes[rs].getWordDensity() + ", " + fullWidthBlockStripes[rs].getWordDensity() + " inherited");
			System.out.println("     - space width: " + blockStripes[rs].getWordSpacing() + " (min " + blockStripes[rs].minWordDistance + ", max " + blockStripes[rs].maxWordDistance + ")");
			System.out.println("     - normalized space width: " + blockStripes[rs].getNormWordSpacing() + " (min " + blockStripes[rs].nMinWordDistance + ", max " + blockStripes[rs].nMaxWordDistance + ")");
			System.out.println("     - square space width: " + blockStripes[rs].getSquareWordSpacing() + " absolute, " + blockStripes[rs].getNormSquareWordSpacing() + " normalized");
			System.out.println("     - space width quotient: " + blockStripes[rs].getWordSpacingQuotient());
			System.out.println("     - column occupation similarity overall: " + (blockStripes[rs].sharedWordCols + blockStripes[rs].sharedNonWordCols) + " (" + blockStripes[rs].sharedWordCols + " words, " + blockStripes[rs].sharedNonWordCols + " space)");
			System.out.println("     - column occupation similarity above: " + blockStripes[rs].aColCompatibility);
			System.out.println("     - column occupation similarity below: " + blockStripes[rs].bColCompatibility);
			System.out.println("     - assigned full-width sibling: " + ((fullWidthBlockStripes[rs] == blockStripes[rs]) ? "self" : fullWidthBlockStripes[rs].bounds.toString()));
			System.out.println("     - column occupation similarity above for full width: " + fullWidthBlockStripes[rs].aFwColCompatibility + " (matching word type " + fullWidthBlockStripes[rs].aFwMatchTypeCols + "), (max space length " + fullWidthBlockStripes[rs].aFwMaxSeqSharedNonWordCols + ", avg " + fullWidthBlockStripes[rs].aFwAvgSeqSharedNonWordCols + "))");
			System.out.println("     - column occupation similarity below for full width: " + fullWidthBlockStripes[rs].bFwColCompatibility + " (matching word type " + fullWidthBlockStripes[rs].bFwMatchTypeCols + "), (max space length " + fullWidthBlockStripes[rs].bFwMaxSeqSharedNonWordCols + ", avg " + fullWidthBlockStripes[rs].bFwAvgSeqSharedNonWordCols + "))");
			if (blockStripes[rs].colAnchors != null) {
				System.out.println("     - column anchors: " + Arrays.toString(blockStripes[rs].colAnchors));
				System.out.println("     - column count: " + blockStripes[rs].colCount);
				System.out.println("     - column anchors per word: " + (((float) blockStripes[rs].colAnchors.length) / blockStripes[rs].words.length));
				System.out.println("     - column anchors per inch: " + (((float) (blockStripes[rs].colAnchors.length * page.getImageDPI())) / (blockStripes[rs].bounds.right - blockStripes[rs].bounds.left)));
			}
			if (blockStripes[rs].compatibleColAnchors != null)
				System.out.println("     - compatible column anchors: " + Arrays.toString(blockStripes[rs].compatibleColAnchors));
		}
		
		//	sort row stripes into groups, separating at row gaps of above-average height
		ArrayList blockStripeGroups = new ArrayList();
		ArrayList groupRowStripes = new ArrayList();
		ArrayList groupFullWidthRowStripes = new ArrayList();
		for (int rs = 0; rs < blockStripes.length; rs++) {
			groupRowStripes.add(blockStripes[rs]);
			groupFullWidthRowStripes.add(fullWidthBlockStripes[rs]);
			
			//	test if stripe group ends
			boolean stripeGroupEnds = false;
			
			//	cut at above average distance
			if (blockStripes[rs].bSpace > avgBlockStripeSpace) {
				stripeGroupEnds = true;
				
				//	make sure to mark left-aligned single-line table caption (multi-line captions will have a distinctive word column density)
				if ((rs == 0) && ((blockStripes[rs].bounds.left - blockBounds.left) < (page.getImageDPI() / 8)) && (blockStripes[rs].findWordMatching("T[Aa][Bb].*") == 0))
					blockStripes[rs].isCaptionOrNote = true;
				
				//	make sure to mark table bottom notes
				else if (((rs + 2) == blockStripes.length) && ((blockBounds.right - blockStripes[rs+1].bounds.right) < (page.getImageDPI() / 8)) && (blockStripes[rs+1].findWordMatching("[Cc]ontinu.*") != -1))
					blockStripes[rs+1].isCaptionOrNote = true;
			}
			
			//	cut after short left-aligned single-line table caption (longer or multi-line captions will have a distinctive space below them)
			if ((rs == 0) && (fullWidthBlockStripes[rs] != blockStripes[rs]) && ((blockStripes[rs].bounds.left - blockBounds.left) < (page.getImageDPI() / 8)) && (blockStripes[rs].findWordMatching("T[Aa][Bb].*") == 0)) {
				groupFullWidthRowStripes.set((groupFullWidthRowStripes.size() - 1), blockStripes[rs]);
				blockStripes[rs].isCaptionOrNote = true;
				stripeGroupEnds = true;
			}
			
			//	cut before short right-aligned single-line table bottom note (longer or multi-line notes will have a distinctive space above them)
			if (((rs + 2) == blockStripes.length) && (fullWidthBlockStripes[rs+1] != blockStripes[rs+1]) && ((blockBounds.right - blockStripes[rs+1].bounds.right) < (page.getImageDPI() / 8)) && (blockStripes[rs+1].findWordMatching("[Cc]ontinu.*") != -1))
				stripeGroupEnds = true;
			
			//	make sure table bottom notes are self-assigned full width row stripes
			else if (((rs + 1) == blockStripes.length) && (fullWidthBlockStripes[rs] != blockStripes[rs]) && ((blockBounds.right - blockStripes[rs].bounds.right) < (page.getImageDPI() / 8)) && (blockStripes[rs].findWordMatching("[Cc]ontinu.*") != -1)) {
				groupFullWidthRowStripes.set((groupFullWidthRowStripes.size() - 1), blockStripes[rs]);
				blockStripes[rs].isCaptionOrNote = true;
			}
			
			//	TODO consider cutting at large column word density jumps between full-width row stripes
			
			//	store stripe group and clear collector lists
			if (stripeGroupEnds) {
				blockStripeGroups.add(new RowStripeGroup(
						((RowStripe[]) groupRowStripes.toArray(new RowStripe[groupRowStripes.size()])),
						((RowStripe[]) groupFullWidthRowStripes.toArray(new RowStripe[groupFullWidthRowStripes.size()]))
					));
				groupRowStripes.clear();
				groupFullWidthRowStripes.clear();
			}
		}
		
		//	just for now ...
		return ((RowStripeGroup[]) blockStripeGroups.toArray(new RowStripeGroup[blockStripeGroups.size()]));
	}
	
	/*
When marking a table, keep using current approach of optimizing column count and column gaps ...
... but add second step:
- compute word density and average space widths in columns ...
- ... and split up columns whose values are way above average and have internal low-whitespace areas
==> should also help with column detection in the presence of multi-column cells
- also split up columns that have internal whitespace gaps at least 67% as wide as table average
==> should help with wide (as well as misplaced, see that Zootaxa with the five-page table appendix) column headers
- ultimately, word density in table _cells_ should be in vicinity (most likely north of) document average
==> use the latter for splitting up columns, also in the presence of multi-column cells
	 */
	
	private static class BlockData {
		final ImRegion block;
		final float density;
		final RowStripe[] rowStripes;
		RowStripeGroup[] rowStripeGroups;
		BlockData(ImRegion block, float density, RowStripe[] rowStripes) {
			this.block = block;
			this.density = density;
			this.rowStripes = rowStripes;
		}
	}
	
	private static void assessBlockData(ImPage page, BlockData blockData, float docWordDensity) {
		System.out.println("Assessing block " + blockData.block.pageId + "." + blockData.block.bounds + " with density " + blockData.density + " below document average of " + docWordDensity);
		blockData.rowStripeGroups = getRowStripeGroups(page, blockData.block.bounds, blockData.rowStripes);
		
		System.out.println("Got " + blockData.rowStripeGroups.length + " horizontal stripe groups in block " + blockData.block.pageId + "." + blockData.block.bounds + " (density " + blockData.density + " below document average of " + docWordDensity + "):");
		for (int sg = 0; sg < blockData.rowStripeGroups.length; sg++) {
			System.out.println(" - " + blockData.rowStripeGroups[sg].bounds + " with density " + blockData.rowStripeGroups[sg].getDensity() + ", word column density " + blockData.rowStripeGroups[sg].wordColDensity);
			System.out.print("   - rows: ");
			for (int rs = 0; rs < blockData.rowStripeGroups[sg].rowStripes.length; rs++)
				System.out.print(((rs == 0) ? "" : ", ") + blockData.rowStripeGroups[sg].rowStripes[rs].words[0].getString());
			System.out.println();
			
			//	assess column occupation histogram
			System.out.println("   - column word details: " + Arrays.toString(blockData.rowStripeGroups[sg].colWordCounts));
			System.out.println("   - average words per column and row: " + (blockData.rowStripeGroups[sg].avgColWordCount / blockData.rowStripeGroups[sg].rowStripes.length) + " (avg " + blockData.rowStripeGroups[sg].avgColWordCount + ", min " + blockData.rowStripeGroups[sg].minColWordCount + ", max " + blockData.rowStripeGroups[sg].maxColWordCount + ")");
			for (int w = (page.getImageDPI() / 4); w > 0; w /= 2) {
				float cwcContrast = blockData.rowStripeGroups[sg].getColWordContrast(w);
//				System.out.println("     - column occupation contrast W" + w + ": " + (cwcContrast / (blockData.rowStripeGroups[sg].rowStripes.length * blockData.rowStripeGroups[sg].rowStripes.length)) + " (" + cwcContrast + ")");
				System.out.println("     - contrast W" + w + ": " + (cwcContrast / (blockData.rowStripeGroups[sg].maxColWordCount * blockData.rowStripeGroups[sg].maxColWordCount)) + " (" + cwcContrast + ")");
			}
			System.out.println("   - average smooth words per column and row: " + (blockData.rowStripeGroups[sg].sAvgColWordCount / blockData.rowStripeGroups[sg].rowStripes.length) + " (avg " + blockData.rowStripeGroups[sg].sAvgColWordCount + ", min " + blockData.rowStripeGroups[sg].sMinColWordCount + ", max " + blockData.rowStripeGroups[sg].sMaxColWordCount + ")");
			for (int w = (page.getImageDPI() / 4); w > 0; w /= 2) {
				float cwcContrast = blockData.rowStripeGroups[sg].getSmoothColWordContrast(w);
//				System.out.println("     - column occupation contrast W" + w + ": " + (cwcContrast / (blockData.rowStripeGroups[sg].rowStripes.length * blockData.rowStripeGroups[sg].rowStripes.length)) + " (" + cwcContrast + ")");
				System.out.println("     - contrast W" + w + ": " + (cwcContrast / (blockData.rowStripeGroups[sg].sMaxColWordCount * blockData.rowStripeGroups[sg].sMaxColWordCount)) + " (" + cwcContrast + ")");
			}
			
			//	assess upward any downward compatibility, especially regarding whitespace
			RowStripeGroup aStripeGroup = ((sg == 0) ? null : blockData.rowStripeGroups[sg-1]);
			RowStripeGroup stripeGroup = blockData.rowStripeGroups[sg];
			RowStripeGroup bStripeGroup = (((sg+1) == blockData.rowStripeGroups.length) ? null : blockData.rowStripeGroups[sg+1]);
			if (aStripeGroup != null) {
				stripeGroup.aColCompatibility = getColumnCompatibility(aStripeGroup.bounds.left, aStripeGroup.colHasWord, stripeGroup.bounds.left, stripeGroup.colHasWord, false);
				System.out.println("   - column occupation similarity above: " + blockData.rowStripeGroups[sg].aColCompatibility);
				int cMaxColWordCount = (aStripeGroup.maxColWordCount + stripeGroup.maxColWordCount);
				for (int w = (page.getImageDPI() / 4); w > 0; w /= 2) {
					float cwcContrast = computeColWordContrast(aStripeGroup.bounds.left, aStripeGroup.colWordCounts, stripeGroup.bounds.left, stripeGroup.colWordCounts, w);
//					System.out.println("     - column occupation contrast W" + w + ": " + (cwcContrast / (blockData.rowStripeGroups[sg].rowStripes.length * blockData.rowStripeGroups[sg].rowStripes.length)) + " (" + cwcContrast + ")");
					System.out.println("     - contrast W" + w + " with above: " + (cwcContrast / (cMaxColWordCount * cMaxColWordCount)) + " (" + cwcContrast + ")");
				}
				int cMaxSmoothColWordCount = (aStripeGroup.sMaxColWordCount + stripeGroup.sMaxColWordCount);
				for (int w = (page.getImageDPI() / 4); w > 0; w /= 2) {
					float sCwcContrast = computeColWordContrast(aStripeGroup.bounds.left, aStripeGroup.sColWordCounts, stripeGroup.bounds.left, stripeGroup.sColWordCounts, w);
//					System.out.println("     - column occupation contrast W" + w + ": " + (cwcContrast / (blockData.rowStripeGroups[sg].rowStripes.length * blockData.rowStripeGroups[sg].rowStripes.length)) + " (" + cwcContrast + ")");
					System.out.println("     - smooth contrast W" + w + " with above: " + (sCwcContrast / (cMaxSmoothColWordCount * cMaxSmoothColWordCount)) + " (" + sCwcContrast + ")");
				}
			}
			if (bStripeGroup != null) {
				stripeGroup.bColCompatibility = getColumnCompatibility(stripeGroup.bounds.left, stripeGroup.colHasWord, bStripeGroup.bounds.left, bStripeGroup.colHasWord, false);
				System.out.println("   - column occupation similarity below: " + blockData.rowStripeGroups[sg].bColCompatibility);
				int cMaxColWordCount = (stripeGroup.maxColWordCount + bStripeGroup.maxColWordCount);
				for (int w = (page.getImageDPI() / 4); w > 0; w /= 2) {
					float cwcContrast = computeColWordContrast(stripeGroup.bounds.left, stripeGroup.colWordCounts, bStripeGroup.bounds.left, bStripeGroup.colWordCounts, w);
//					System.out.println("     - column occupation contrast W" + w + ": " + (cwcContrast / (blockData.rowStripeGroups[sg].rowStripes.length * blockData.rowStripeGroups[sg].rowStripes.length)) + " (" + cwcContrast + ")");
					System.out.println("     - contrast W" + w + " with below: " + (cwcContrast / (cMaxColWordCount * cMaxColWordCount)) + " (" + cwcContrast + ")");
				}
				int cMaxSmoothColWordCount = (stripeGroup.sMaxColWordCount + bStripeGroup.sMaxColWordCount);
				for (int w = (page.getImageDPI() / 4); w > 0; w /= 2) {
					float sCwcContrast = computeColWordContrast(stripeGroup.bounds.left, stripeGroup.sColWordCounts, bStripeGroup.bounds.left, bStripeGroup.sColWordCounts, w);
//					System.out.println("     - column occupation contrast W" + w + ": " + (cwcContrast / (blockData.rowStripeGroups[sg].rowStripes.length * blockData.rowStripeGroups[sg].rowStripes.length)) + " (" + cwcContrast + ")");
					System.out.println("     - smooth contrast W" + w + " with below: " + (sCwcContrast / (cMaxSmoothColWordCount * cMaxSmoothColWordCount)) + " (" + sCwcContrast + ")");
				}
			}
			
			//	discard short captions and table bottom notes marked as such if they are sufficiently dense
			if ((blockData.rowStripeGroups[sg].wordColDensity > docWordDensity) && blockData.rowStripeGroups[sg].isCaptionOrNote) {
				System.out.println("   ==> marked as caption or note at edge of table");
				blockData.rowStripeGroups[sg].isTable = false;
				blockData.rowStripeGroups[sg].isTablePart = false;
				continue;
			}
			
			//	discard row stripe groups whose word column density is closer to 100% than to document average
			if ((1 - blockData.rowStripeGroups[sg].wordColDensity) < (blockData.rowStripeGroups[sg].wordColDensity - docWordDensity)) {
				System.out.println("   ==> columns too dense for table");
				blockData.rowStripeGroups[sg].isTable = false;
				blockData.rowStripeGroups[sg].isTablePart = false;
				continue;
			}
			
			//	require at least 3 full width rows for a full table
			if (blockData.rowStripeGroups[sg].fullWidthRowStripeCount < 3) {
				System.out.println("   ==> too few full width rows for full table, needs merger");
				blockData.rowStripeGroups[sg].isTable = false;
				blockData.rowStripeGroups[sg].isTablePart = true;
				continue;
			}
			
			//	TODO need any further checks?
			//	TODO add more checks as need arises
			
			//	this one looks good
			System.out.println("   ==> likely table");
			blockData.rowStripeGroups[sg].isTable = true;
			blockData.rowStripeGroups[sg].isTablePart = true;
		}
	}
	
	private static class TableCandidate implements Comparable {
		final BoundingBox bounds;
		final RowStripeGroup[] rowStripeGroups;
		final boolean[] colHasWord;
		final float wordColDensity;
		final int fullWidthRowStripeCount;
		TableCandidate(BoundingBox bounds, RowStripeGroup[] rowStripeGroups, boolean[] colHasWord, float wordColDensity, int fullWidthRowStripeCount) {
			this.bounds = bounds;
			this.rowStripeGroups = rowStripeGroups;
			this.colHasWord = colHasWord;
			this.wordColDensity = wordColDensity;
			this.fullWidthRowStripeCount = fullWidthRowStripeCount;
		}
		public int compareTo(Object obj) {
			if (obj instanceof TableCandidate)
				return (getArea(((TableCandidate) obj).bounds) - getArea(this.bounds));
			else return 0;
		}
	}
	
	//	TODO return some TableCandidate object or the like, with probabilities, etc.
	private static TableCandidate checkForTable(ArrayList rowStripeGroups, float docWordDensity) {
		
		//	compute overall extent
		int left = Integer.MAX_VALUE;
		int right = 0;
		int top = Integer.MAX_VALUE;
		int bottom = 0;
		for (int rs = 0; rs < rowStripeGroups.size(); rs++) {
			RowStripeGroup stripeGroup = ((RowStripeGroup) rowStripeGroups.get(rs));
			left = Math.min(left, stripeGroup.bounds.left);
			right = Math.max(right, stripeGroup.bounds.right);
			top = Math.min(top, stripeGroup.bounds.top);
			bottom = Math.max(bottom, stripeGroup.bounds.bottom);
		}
		BoundingBox aggregateBox = new BoundingBox(left, right, top, bottom);
		System.out.println("Checking " + rowStripeGroups.size() + " row stripe groups in " + aggregateBox + " for table ...");
		
		//	compute overall column occupation and full width row count
		int fullWidthRowStripeCount = 0;
		float maxPartWordColDensity = 0;
		boolean[] colHasWord = new boolean[right - left];
		Arrays.fill(colHasWord, false);
		for (int rs = 0; rs < rowStripeGroups.size(); rs++) {
			RowStripeGroup stripeGroup = ((RowStripeGroup) rowStripeGroups.get(rs));
			fullWidthRowStripeCount += stripeGroup.fullWidthRowStripeCount;
			maxPartWordColDensity = Math.max(maxPartWordColDensity, stripeGroup.wordColDensity);
			for (int c = 0; c < stripeGroup.colHasWord.length; c++) {
				if (stripeGroup.colHasWord[c])
					colHasWord[c + (stripeGroup.bounds.left - left)] = true;
			}
		}
		System.out.println(" - got " + fullWidthRowStripeCount + " full with row stripes");
		System.out.println(" - maximum row stripe group density is " + maxPartWordColDensity);
		
		//	compute column occupation
		int wordCols = 0;
		for (int c = 0; c < colHasWord.length; c++) {
			if (colHasWord[c])
				wordCols++;
		}
		float wordColDensity = (((float) wordCols) / (right - left));
		System.out.println(" - aggregate density is " + wordColDensity);
		
		//	overall word column density closer to 100% than to maximum of argument row stripe groups, reject table
		if ((1 - wordColDensity) < (wordColDensity - maxPartWordColDensity)) {
			System.out.println(" ==> aggregate columns too dense for table");
			return null;
		}
		
		//	finally ...
		System.out.println(" ==> looks good for table or part thereof");
		return new TableCandidate(aggregateBox, ((RowStripeGroup[]) rowStripeGroups.toArray(new RowStripeGroup[rowStripeGroups.size()])), colHasWord, wordColDensity, fullWidthRowStripeCount);
	}
	
	/* TODO also assess Graphics supplements (paths that form long horizontal or
	 * vertical lines, in particular), which occur with the very most tables. */
	
	public static void main(String[] args) throws Exception {
		final ImDocument doc;
//		
//		//	example with multi-line cells (in-cell line margin about 5 pixels, row margin about 15 pixels): zt00904.pdf, page 6
//		doc = ImDocumentIO.loadDocument(new File("E:/Testdaten/PdfExtract/TableTest/zt00904.pdf.clean.imf"));
//		
//		//	test for second step in column detection (see above, in 'zt00904.pdf.clean.imf', page ID 6, table at [267, 1313, 908, 1870])
//		if (true) {
//			TableActionProvider tap = new TableActionProvider();
//			tap.setDataProvider(new PluginDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/TableActionProviderData")));
//			tap.init();
//			ImPage page = doc.getPage(6);
//			BoundingBox tbb = new BoundingBox(267, 1313, 908, 1870);
//			tap.markTable(page, tbb, null);
//			ImRegion[] tCols = page.getRegions(ImRegion.TABLE_COL_TYPE);
//			Arrays.sort(tCols, ImUtils.leftRightOrder);
//			for (int c = 0; c < tCols.length; c++)
//				System.out.println("Table column: " + tCols[c].bounds);
//			return;
//		}
//		
//		//	example without multi-line cells, very tight row margin (2 pixels): Milleretal2014Anthracotheres.pdf, page 4
//		doc = ImDocumentIO.loadDocument(new File("E:/Testdaten/PdfExtract/TableTest/Milleretal2014Anthracotheres.pdf.clean.imf"));
//		
//		//	example without multi-line cells, normal row margin: zt00619.o.pdf, page 3
//		doc = ImDocumentIO.loadDocument(new File("E:/Testdaten/PdfExtract/TableTest/zt00619.pdf.clean.imf"));
//		
//		//	example with very widely spaced table rows and narrow column gaps: zt00872.pdf, page 8
//		doc = ImDocumentIO.loadDocument(new File("E:/Testdaten/PdfExtract/TableTest/zt00872.pdf.clean.imf"));
//		
//		//	example with sparse 2-page table on page 1, and large 5-page table on page 30, without multi-line cells: zt03652p155.pdf.clean.imf
//		doc = ImDocumentIO.loadDocument(new File("E:/Testdaten/PdfExtract/TableTest/zt03652p155.pdf.clean.imf"));
//		
//		//	simple, clean example with twice two smallish tables
//		doc = ImDocumentIO.loadDocument(new File("E:/Testdaten/PdfExtract/TableTest/zt01826p058.pdf.clean.imf"));
//		
//		//	example with multi-page tables from pages 4 to 6 (protruding column header on page 9), and very sparse tables on pages 9 and 11
//		doc = ImDocumentIO.loadDocument(new File("E:/Testdaten/PdfExtract/TableTest/zt03881p227.pdf.clean.imf"));
//		
//		//	example with multiple tables, and key mistaken for table candidate on page 8 (gets sorted out on too few columns, though)
//		doc = ImDocumentIO.loadDocument(new File("E:/Testdaten/PdfExtract/TableTest/zt03980p278.pdf.clean.imf"));
//		
//		//	artificial example with multi-column cells
//		doc = ImDocumentIO.loadDocument(new File("E:/Testdaten/PdfExtract/TableTest/Artificial1.pdf.imf"));
//		
//		//	artificial example with full-grid table and multi-column cells
//		doc = ImDocumentIO.loadDocument(new File("E:/Testdaten/PdfExtract/TableTest/Artificial2.pdf.imf"));
//		
//		//	nice example with multi-column header cells on pages 2 (with extensive caption), 3, and 4
//		doc = ImDocumentIO.loadDocument(new File("E:/Testdaten/PdfExtract/Zootaxa/zootaxa.4085.3.3.pdf.imf"));
//		
//		//	some serrated-column table on Page 9 that fails to detect TODO figure out how to detect it
//		//	TODO maybe cut some slack on column spacing if overall word density is well below document average
//		//	TODO maybe try and compute actual word spacing in row stripes where spaces overlap with core column gaps
//		doc = ImDocumentIO.loadDocument(new File("E:/Testdaten/PdfExtract/EJT/ejt-394_enghoff.pdf.imf"));
//		
//		//	real-world example of a fully enclosing grid table on page 13 TODO test this
//		doc = ImDocumentIO.loadDocument(new File("E:/Testdaten/PdfExtract/EJT/73-363-1-PB.pdf.imf"));
		
		//	example with gigantic table making up half of document
		//	TODO make sure to find tables in Pages 4, 5, 11
		//	TODO allow less than 5% column gap if overall density low enough
		doc = ImDocumentIO.loadDocument(new File("E:/Testdaten/PdfExtract/Zootaxa/zootaxa.4369.4.4.pdf.imf"));
		
		//	TODO check table in page 5 of American_Journal_of_Primatology.10.1002_ajp.22631.pdf.imd
		
		//	get document pages
		ImPage[] pages = doc.getPages();
		TableDetectorProvider tdp = new TableDetectorProvider();
		tdp.init();
		tdp.detectTables(doc, DocumentStyle.getStyleFor(doc), null, null);
		if (true)
			return;
		
		/* TODO
	- investigate vector graphics objects in the area:
	  - horizontal lines split table rows
	    ==> helps with full-grid tables
	  - render graphics in area, and use row occupation histogram to find horizontal lines
	- in general, use presence of text line separating (horizontal, first and foremost) lines as indicator of table
	- try and find table border lines, encircling table block
	- keep on using word distance as a measure of table cell coherence (==> helps ignore in-cell word gaps)
	- use peaks in column whitespace (or lows in column occupation) as indicators of column gaps
	- detect multi-column cells as local obstruction of column gaps
	etc. ...
		 */
		
		/* TODO use public methods of table action provider to mark tables if graphics framed block found to be one
		 * After initial marking, correct markup:
		 * - investigate columns:
		 *   - split ones with large internal gaps (multi-column cells stay together now !!!)
		 *   - merge ones all too close together
		 * - investigate rows:
		 *   - split ones with large internal gaps (multi-row cells stay together now !!!)
		 *   - merge ones all too close together
		 */
		
		/* TODO
In after-markup stage of table _detection_, check for column and row gaps that have lots of whitespace on either side (likely choked by narrow word gap in multi-column cell or narrow line gap in multi-row cell, respectively), merge respective cells.
==> columns or rows snap back to correct bounds

When detecting choked column boundaries, also check if right side of narrow gap is straight aligned with rest of right column, and if so, waive cell merger as choking value in left column might just be comparatively long, but not crossing column boundary.
In same assessment, also check left alignment in left column - an indent in an otherwise left aligned column might point to a multi-column cell or row group heading.
		 */
		
		/*
4. Presence of row labels on left and/or column headers at top
- if columns are type coherent (and specific), we need words at column top
- if rows are type coherent (and specific), we need words to left of rows
- use this one only as a secondary criterion, as there might well be tables that use number or letter codes in both roles

In candidate generation
- quit if both (1) and (2) come back negative (either one needs to be sufficient, (2) needs to catch likely failure of (1) in Zootaxa 872, for instance)
- do not quit if (4) comes back negative, as there might well be tables using numeric codes as row labels and/or column headers (Zootaxa 35xx, the one Ross initially asked about in Leiden)
- discard candidates containing (area-wise) two or more higher scoring candidates (table conflation problem in Zootaxa 872)
- make sure to not score table body (type coherent, etc.) over whole table including column labels

In general, be more strict the smaller the table
- require at least three rows (splittable lines) and two columns, or vice versa
- after all, a false positive on column gaps is more likely the fewer rows are in a block

In post-markup correction
- check for column gaps with lots of whitespace to left and/or right, and mark cross column cells choking split
  - require column gap below half word height (upper bound for word space width in any reasonable font)
  - require whitespace adjacent to column gap to be wider than column gap proper in total (sum of left and right)
- do similar for table rows
  - use average line distance as baseline (from whole document)

In style templates
- specify presence of vertical grid lines
  - full grid (lines between all columns)
  - row labels and table body separated (internal line only between row labels and table body)
  - boundary (only left and right)
  - none
- specify presence of horizontal grid lines
  - full grid (line between all rows)
  - partial grid (line between some rows, e.g. after every third row)
  - column heads and table body separated (internal line only between column headers and table body)
  - none
- encode as 'F'  (full grid), 'P' (implying 'L', got to see that one case where this doesn't hold), 'L' (separator after row labels / column headers only), and 'N' (none)
==> try and introduce enumeration based style parameters in some kind of way
		 */
		
		//	compute average density of document blocks
		int stripeWidthSum = 0;
		int stripeArea = 0;
		int wordWidthSum = 0;
		int wordHeightSum = 0;
		int wordCount = 0;
		int wordArea = 0;
		int wordDistanceSum = 0;
		float nWordDistanceSum = 0;
		int wordDistanceCount = 0;
		for (int pg = 0; pg < pages.length; pg++) {
			
			//	assess page blocks
			ImRegion[] pgBlocks = pages[pg].getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
			Arrays.sort(pgBlocks, ImUtils.topDownOrder);
			for (int b = 0; b < pgBlocks.length; b++) {
				System.out.println(" - assessing block " + pgBlocks[b].bounds);
				
				RowStripe[] bStripes = getRowStripes(pages[pg], pgBlocks[b].bounds);
				if (bStripes.length == 0) {
//					System.out.println("   --> empty block");
					continue; // empty block, image or graphics with no labels
				}
//				System.out.println("   - got " + bStripes.length + " block stripes");
//				float bWordDensity = getWordDensity(bStripes);
//				System.out.println("   --> block word density is " + bWordDensity);
				
				for (int s = 0; s < bStripes.length; s++) {
					stripeWidthSum += bStripes[s].bounds.getWidth();
					stripeArea += bStripes[s].area;
					wordWidthSum += bStripes[s].wordWidthSum;
					wordHeightSum += bStripes[s].wordHeightSum;
					wordCount += bStripes[s].words.length;
					wordArea += bStripes[s].wordArea;
					wordDistanceSum += bStripes[s].wordDistanceSum;
					nWordDistanceSum += bStripes[s].nWordDistanceSum;
					wordDistanceCount += bStripes[s].wordDistanceCount;
				}
			}
		}
		float docWordDensity1d = ((stripeWidthSum < 1) ? 0 : (((float) wordWidthSum) / stripeWidthSum));
		float docWordDensity2d = ((stripeArea < 1) ? 0 : (((float) wordArea) / stripeArea));
		System.out.println("Average document word density is " + docWordDensity1d + " horizonatally, " + docWordDensity2d + " by area");
		float docWordSpacing = ((wordDistanceCount < 1) ? 0 : (((float) wordDistanceSum) / wordDistanceCount));
		System.out.println("Average document word spacing " + docWordSpacing);
		float docNormSpaceWidth = ((wordDistanceCount < 1) ? 0 : (((float) nWordDistanceSum) / wordDistanceCount));
		System.out.println("Average normalized document word spacing " + docNormSpaceWidth);
		float docWordHeight = ((wordCount < 1) ? 0 : (((float) wordHeightSum) / wordCount));
		System.out.println("Average document word height " + docWordHeight);
//		System.out.println("Block density distripution:");
//		for (int d = 20; d >= 0; d--)
//			System.out.println(" - " + (d * 5) + ": " + docBlockDensities.getCount(new Integer(d)));
//		System.out.println("Stripe density distripution:");
//		for (int d = 20; d >= 0; d--)
//			System.out.println(" - " + (d * 5) + ": " + docStripeDensities.getCount(new Integer(d)));
		//	as it seems (at least from 3 test documents of rather different layout), document average block density makes for a good threshold above which a block is surely NOT a table
		
		//	get vector graphics supplements for each page, and count frequencies
		Graphics[][] pageGraphics = new Graphics[pages.length][];
		CountingSet graphicsPageCounts = new CountingSet();
		CountingSet graphicsEvenPageCounts = new CountingSet();
		CountingSet graphicsOddPageCounts = new CountingSet();
		for (int pg = 0; pg < pages.length; pg++) {
			System.out.println("Collecting graphics in page " + pages[pg].pageId);
			ImSupplement[] pageSupplements = pages[pg].getSupplements();
			ArrayList pageGraphicsList = new ArrayList();
			for (int s = 0; s < pageSupplements.length; s++) {
				if (pageSupplements[s] instanceof Graphics)
					pageGraphicsList.add(pageSupplements[s]);
			}
			pageGraphics[pg] = ((Graphics[]) pageGraphicsList.toArray(new Graphics[pageGraphicsList.size()]));
			System.out.println(" - found " + pageGraphics[pg].length + " individual graphics:");
			for (int g = 0; g < pageGraphics[pg].length; g++) {
				BoundingBox graphBounds = pageGraphics[pg][g].getBounds();
				System.out.println("   - " + graphBounds);
				graphicsPageCounts.add(graphBounds);
				if ((pg % 2) == 0)
					graphicsEvenPageCounts.add(graphBounds);
				else graphicsOddPageCounts.add(graphBounds);
			}
		}
		
		//	assess graphics
		for (int pg = 0; pg < pages.length; pg++) {
			System.out.println("Assessing graphics in page " + pages[pg].pageId);
			ArrayList pageGraphicsList = new ArrayList();
			for (int g = 0; g < pageGraphics[pg].length; g++) {
				BoundingBox graphBounds = pageGraphics[pg][g].getBounds();
				System.out.println(" - " + graphBounds + ":");
				
				//	graphics existing in majority (maybe 2/3) of pages, likely page decoration
				int graphPageCount = graphicsPageCounts.getCount(graphBounds);
				int graphEvenPageCount = graphicsEvenPageCounts.getCount(graphBounds);
				int graphOddPageCount = graphicsOddPageCounts.getCount(graphBounds);
				System.out.println("   - present in " + graphPageCount + " of " + pages.length + " pages (" + graphEvenPageCount + " even, " + graphOddPageCount + " odd)");
				if ((graphPageCount * 3) > (pages.length * 2)) {
					System.out.println("     ==> removed as page decoration");
					continue;
				}
				else if ((graphOddPageCount == 0) && ((graphEvenPageCount * 3) > pages.length)) {
					System.out.println("     ==> removed as even page decoration");
					continue;
				}
				else if ((graphEvenPageCount == 0) && ((graphOddPageCount * 3) > pages.length)) {
					System.out.println("     ==> removed as odd page decoration");
					continue;
				}
				
				//	sort out graphics too narrow for a table (less than 1 inch)
				int graphWidth = graphBounds.getWidth();
				System.out.println("   - width is " + graphWidth);
				if (graphWidth < pages[pg].getImageDPI()) {
					System.out.println("     ==> removed as too narrow for (part of) table grid line");
					continue;
				}
				
				//	remove graphics that include curves, as we're out for straight lines only
				Graphics.Path[] graphPaths = pageGraphics[pg][g].getPaths();
				int graphSubPathCount = 0;
				int graphShapeCount = 0;
				int graphLineCount = 0;
				int graphCurveCount = 0;
				for (int p = 0; p < graphPaths.length; p++) {
					Graphics.SubPath[] subPaths = graphPaths[p].getSubPaths();
					graphSubPathCount += subPaths.length;
					for (int s = 0; s < subPaths.length; s++) {
						Shape[] subPathShapes = subPaths[s].getShapes();
						graphShapeCount += subPathShapes.length;
						for (int h = 0; h < subPathShapes.length; h++) {
							if (subPathShapes[h] instanceof Line2D)
								graphLineCount++;
							else if (subPathShapes[h] instanceof CubicCurve2D)
								graphCurveCount++;
							else if (subPathShapes[h] instanceof QuadCurve2D)
								graphCurveCount++;
						}
					}
				}
				System.out.println("   - got " + graphShapeCount + " shapes in " + graphPaths.length + " paths and " + graphSubPathCount + " sub paths: " + graphLineCount + " lines and " + graphCurveCount + " curves");
				if (graphCurveCount > graphLineCount) {
					System.out.println("     ==> removed as comprising too many curves and too few lines for (part of) table grid");
					continue;
				}
				
				//	this one looks like it might be part of a table grid
				pageGraphicsList.add(pageGraphics[pg][g]);
				System.out.println("     ==> retained for further inspection");
			}
			
			//	update main array
			if (pageGraphicsList.size() < pageGraphics[pg].length)
				pageGraphics[pg] = ((Graphics[]) pageGraphicsList.toArray(new Graphics[pageGraphicsList.size()]));
			
			System.out.println(" ==> retained " + pageGraphics[pg].length + " graphics for further inspection");
		}
		
		//	assess potential table grid graphics in each page
		for (int pg = 0; pg < pages.length; pg++) {
			findHorizontalLineGridTables(pages[pg], pageGraphics[pg], docWordHeight, docNormSpaceWidth);
			if (true)
				continue;
			
			if (pageGraphics[pg].length == 0)
				continue; // nothing to work with
			System.out.println("Assessing potential table grids in page " + pages[pg].pageId);
			
			//	sort graphics top-down
			Arrays.sort(pageGraphics[pg], topDownGraphicsOrder);
			System.out.println(" - " + pageGraphics[pg].length + " graphics sorted");
			
			/* mark region as non-table area if
			 * (a) only two lines contained
			 * (b) lines more than half average word height apart (to account for double lines)
			 * (c) no words contained at all */
			ArrayList pageNonTableAreas = new ArrayList();
			for (int g = 0; g < pageGraphics[pg].length; g++) {
				BoundingBox graphBounds = pageGraphics[pg][g].getBounds();
				System.out.println(" - seeking non-table areas starting with " + graphBounds);
				
				//	combine with other graphics in same column
				for (int cg = (g+1); cg < pageGraphics[pg].length; cg++) {
					BoundingBox cGraphBounds = pageGraphics[pg][cg].getBounds();
//					System.out.println("     - checking against " + cGraphBounds + " ...");
					
					//	check column overlap (require 97% to accommodate sloppy layout)
					int uWidth = (Math.max(graphBounds.right, cGraphBounds.right) - Math.min(graphBounds.left, cGraphBounds.left));
					int iWidth = (Math.min(graphBounds.right, cGraphBounds.right) - Math.max(graphBounds.left, cGraphBounds.left));
					if ((iWidth * 100) < (uWidth * 97)) {
//						System.out.println("       ==> alignment mismatch");
						continue;
					}
					
					//	check combined height
					int uHeight = (Math.max(graphBounds.bottom, cGraphBounds.bottom) - Math.min(graphBounds.top, cGraphBounds.top));
					if ((uHeight * 2) < docWordHeight) {
//						System.out.println("       ==> lower than half a word, potential double line");
						continue;
					}
					
					//	mark potential non-table area
					BoundingBox uGraphBounds = new BoundingBox(
							Math.min(graphBounds.left, cGraphBounds.left),
							Math.max(graphBounds.right, cGraphBounds.right),
							Math.min(graphBounds.top, cGraphBounds.top),
							Math.max(graphBounds.bottom, cGraphBounds.bottom)
						);
					
					//	check for any contained graphics
					for (int og = (g+1); og < pageGraphics[pg].length; og++) {
						if (og == cg)
							break; // no need to seek any further thanks to top-down sorting
						BoundingBox oGraphBounds = pageGraphics[pg][og].getBounds();
						
						//	check width (require 97% to accommodate sloppy layout)
						if ((oGraphBounds.getWidth() * 100) < (uGraphBounds.getWidth() * 97))
							continue;
						
						//	check overlap (having checked width before, fuzzy inclusion should do)
						if (!uGraphBounds.includes(oGraphBounds, true))
							continue;
						
						//	we can exclude this one
						uGraphBounds = null;
//						System.out.println("       ==> contains " + oGraphBounds);
						break;
					}
					if (uGraphBounds == null)
						continue;
					
					//	check for contained words
					ImWord[] uGraphWords = pages[pg].getWordsInside(uGraphBounds);
					
					//	mark empty non-table area
					if (uGraphWords.length == 0) {
						pageNonTableAreas.add(uGraphBounds);
						System.out.println("   - got non-table area together with " + cGraphBounds + ": " + uGraphBounds);
						continue;
					}
					
					//	check for table caption
					ImUtils.sortLeftRightTopDown(uGraphWords);
					String uGraphStartWordString = uGraphWords[0].getString().toLowerCase();
					if (uGraphStartWordString.equals("t") || uGraphStartWordString.startsWith("tab")) {// need to catch word boundary due to font size change emulating small caps in some layouts
						String uGraphStartString = ImUtils.getString(uGraphWords[0], uGraphWords[Math.min(uGraphWords.length, 5) - 1], true);
						if (uGraphStartString.matches("[Tab|TAB][^\\s]*\\.?\\s*[1-9][0-9]*.*")) {
							pageNonTableAreas.add(uGraphBounds);
							System.out.println("   - got caption non-table area together with " + cGraphBounds + ": " + uGraphBounds);
							continue;
						}
					}
					
					//	TODO anything else to check?
				}
			}
			
			//	group graphics into potential table grids with nested loop
			ArrayList pageTableAreas = new ArrayList();
			for (int g = 0; g < pageGraphics[pg].length; g++) {
				BoundingBox graphBounds = pageGraphics[pg][g].getBounds();
				System.out.println(" - seeking table grids starting with " + graphBounds);
				
				//	graphics is tall enough to be table grid in its own right (full-grid tables !!!)
				if (graphBounds.getHeight() > pages[pg].getImageDPI()) {
					pageTableAreas.add(graphBounds);
					System.out.println("   - tall enough for one-piece full table grid");
				}
				
				//	combine with other graphics in same column
				for (int cg = (g+1); cg < pageGraphics[pg].length; cg++) {
					BoundingBox cGraphBounds = pageGraphics[pg][cg].getBounds();
					
					//	check column overlap (require 97% to accommodate sloppy layout)
					int uWidth = (Math.max(graphBounds.right, cGraphBounds.right) - Math.min(graphBounds.left, cGraphBounds.left));
					int iWidth = (Math.min(graphBounds.right, cGraphBounds.right) - Math.max(graphBounds.left, cGraphBounds.left));
					if ((iWidth * 100) < (uWidth * 97))
						continue;
					
					//	check combined height TODO figure out sensible minimum
					int uHeight = (Math.max(graphBounds.bottom, cGraphBounds.bottom) - Math.min(graphBounds.top, cGraphBounds.top));
					if ((uHeight * 2) < pages[pg].getImageDPI())
						continue;
					
					//	mark potential table bounds
					BoundingBox uGraphBounds = new BoundingBox(
							Math.min(graphBounds.left, cGraphBounds.left),
							Math.max(graphBounds.right, cGraphBounds.right),
							Math.min(graphBounds.top, cGraphBounds.top),
							Math.max(graphBounds.bottom, cGraphBounds.bottom)
						);
					
					//	check if area includes any non-table areas
					for (int nta = 0; nta < pageNonTableAreas.size(); nta++) {
						BoundingBox ntaBounds = ((BoundingBox) pageNonTableAreas.get(nta));
						if (uGraphBounds.includes(ntaBounds, true)) {
							uGraphBounds = null;
							break;
						}
					}
					if (uGraphBounds == null)
						continue;
					
					//	store potential table area
					pageTableAreas.add(uGraphBounds);
					System.out.println("   - got potential partial table grid together with " + cGraphBounds + ": " + uGraphBounds);
				}
			}
			
			//	anything left to work with?
			if (pageTableAreas.size() == 0)
				continue;
			
			//	order potential table areas by descending size (makes sure to find whole tables before partial ones)
			Collections.sort(pageTableAreas, descendingBoxSizeOrder);
			
			//	assess potential table areas
			System.out.println(" - assessing " + pageTableAreas.size() + " potential table grids");
			ArrayList pageTableCandidates = new ArrayList();
			for (int t = 0; t < pageTableAreas.size(); t++) {
				BoundingBox tableArea = ((BoundingBox) pageTableAreas.get(t));
				System.out.println("   - assessing " + tableArea);
				ImWord[] tableAreaWords;
				BoundingBox wTableArea;
				while (true) {
					tableAreaWords = pages[pg].getWordsInside(tableArea);
					wTableArea = ImLayoutObject.getAggregateBox(tableAreaWords);
					if (tableArea.equals(wTableArea))
						break;
					else {
						tableArea = wTableArea;
						System.out.println("   - reduced to " + tableArea);
					}
				}
				int tableAreaWordHeightSum = 0;
				for (int w = 0; w < tableAreaWords.length; w++)
					tableAreaWordHeightSum += tableAreaWords[w].bounds.getHeight();
				int tableAreaWordHeight = ((tableAreaWords.length == 0) ? 0 : ((tableAreaWordHeightSum + (tableAreaWords.length / 2)) / tableAreaWords.length));
				System.out.println("   - average word height is " + tableAreaWordHeight);
				
				/* TODO
1. Low density in comparison to document main text
- detect as currently implemented
				 */
				//	get row stripes and density
				RowStripe[] tableAreaStripes = getRowStripes(pages[pg], tableArea);
				float tableAreaWordDensity = getWordDensity(tableAreaStripes);
				System.out.println("   - word density is " + tableAreaWordDensity);
				System.out.println("   - got " + tableAreaStripes.length + " row stripes:");
				for (int r = 0; r < tableAreaStripes.length; r++)
					System.out.println("     - " + tableAreaStripes[r].bounds + ": " + tableAreaStripes[r].words[0].getString());
				
				//	check individual row stripes for table caption (non-table area detection only catches captions boxed in by themselves)
				for (int r = 0; r < tableAreaStripes.length; r++) {
					String stripeStartWordString = tableAreaStripes[r].words[0].getString().toLowerCase();
					if (!stripeStartWordString.equals("t") && !stripeStartWordString.startsWith("tab")) // need to catch word boundary due to font size change emulating small caps in some layouts
						continue;
					String stripeStartString = ImUtils.getString(tableAreaStripes[r].words[0], tableAreaStripes[r].words[Math.min(tableAreaStripes[r].words.length, 5) - 1], true);
					if (stripeStartString.matches("[Tab|TAB][^\\s]*\\.?\\s*[1-9][0-9]*.*")) {
						tableArea = null;
						break;
					}
				}
				if (tableArea == null) {
					System.out.println("   ==> cannot span table across caption");
					continue;
				}
				
				/* TODO
2. Steep flanks in column occupation histogram
- detect using sliding window of, say, DPI/50 pixels width (some half millimeter)
- record both height and width (the higher and narrower the better)
  ==> maybe score as ((height * height) / width), also to pick best one from multiple flanks in same sliding window
  ==> and then DON'T, as that would value a height 4+2 width 2 flank over the contained height 4 width 1 flank
  ==> TODO figure out a sensible scoring formula
- super dense tables will have very pronounced flanks (otherwise even a pair of human eyeballs wouldn't be able to recognize the structure ...)
- watch out for comparatively narrow column gap peaks with wide "shoulders" on both sides (table conflation problem in Zootaxa 872):
  - check if left "shoulder" whitespace completely above or below right "shoulder" whitespace, and if so call a negative
  - check for any lines separating left and right whitespace making up "shoulders", and if any call a negative
				 */
				TableAreaStatistics areaStats = new TableAreaStatistics(pages[pg], tableAreaWords, docNormSpaceWidth);
//				HashSet areaPixelRows = new HashSet();
//				int[] areaColPixelRowCounts = new int[tableArea.getWidth()];
//				Arrays.fill(areaColPixelRowCounts, 0);
//				int[] bAreaColPixelRowCounts = new int[tableArea.getWidth()];
//				Arrays.fill(bAreaColPixelRowCounts, 0);
//				for (int r = 0; r < tableAreaStripes.length; r++) {
////					if ((skipStripes != null) && skipStripes[r])
////						continue;
//					for (int y = tableAreaStripes[r].bounds.top; y < tableAreaStripes[r].bounds.bottom; y++)
//						areaPixelRows.add(new Integer(y));
//					for (int w = 0; w < tableAreaStripes[r].words.length; w++) {
//						for (int x = tableAreaStripes[r].words[w].bounds.left; x < tableAreaStripes[r].words[w].bounds.right; x++) {
//							areaColPixelRowCounts[x - tableArea.left] += tableAreaStripes[r].words[w].bounds.getHeight();
//							bAreaColPixelRowCounts[x - tableArea.left] += tableAreaStripes[r].words[w].bounds.getHeight();
//						}
//						if (((w+1) < tableAreaStripes[r].words.length) && ((tableAreaStripes[r].words[w+1].bounds.left - tableAreaStripes[r].words[w].bounds.right) < (tableAreaStripes[r].words[w].bounds.getHeight() * docNormSpaceWidth))) {
//							for (int x = tableAreaStripes[r].words[w].bounds.right; x < tableAreaStripes[r].words[w+1].bounds.left; x++)
//								bAreaColPixelRowCounts[x - tableArea.left] += tableAreaStripes[r].words[w].bounds.getHeight();
//						}
//					}
//				}
//				System.out.println("   - got " + areaPixelRows.size() + " occupied pixel rows in area of height " + tableArea.getHeight());
//				System.out.println("   - pixel column occupation distribution is " + Arrays.toString(areaColPixelRowCounts));
//				System.out.println("   - bridged pixel column occupation distribution is " + Arrays.toString(bAreaColPixelRowCounts));
//				System.out.println("   - got " + areaStats.wordPixelRows + " occupied pixel rows in area of height " + tableArea.getHeight());
//				System.out.println("   - pixel column occupation distribution is " + Arrays.toString(areaStats.colWordPixelCounts));
//				System.out.println("   - bridged pixel column occupation distribution is " + Arrays.toString(areaStats.bColWordPixelCounts));
//				
//				//	collect all sharp flanks (both up and down) across pixel column occupation
////				LinkedHashSet colPixelCountFlankSet = getColumnOccupationFlanks(pages[pg], areaColPixelRowCounts, areaPixelRows.size(), tableArea.getHeight());
////				System.out.println("   - rises and drops are " + colPixelCountFlankSet);
//				System.out.println("   - rises and drops are " + areaStats.getColPixelCountFlankSet());
////				LinkedHashSet bColPixelCountFlankSet = getColumnOccupationFlanks(pages[pg], bAreaColPixelRowCounts, areaPixelRows.size(), tableArea.getHeight());
////				System.out.println("   - bridged rises and drops are " + bColPixelCountFlankSet);
//				System.out.println("   - bridged rises and drops are " + areaStats.getBrgColPixelCountFlankSet());
				
				//	collect areas with low pixel column occupation
////				TreeSet colPixelCountLowSet = getColumnOccupationLows(areaColPixelRowCounts, areaPixelRows.size());
////				System.out.println("   - lows are " + colPixelCountLowSet);
////				float colPixelLowFract = countLowOccupationColumns(colPixelCountLowSet, tableArea.getWidth());
////				TreeSet bColPixelCountLowSet = getColumnOccupationLows(bAreaColPixelRowCounts, areaPixelRows.size());
////				System.out.println("   - bridged lows are " + bColPixelCountLowSet);
////				float bColPixelLowFract = countLowOccupationColumns(bColPixelCountLowSet, tableArea.getWidth());
//				System.out.println("   - lows are " + areaStats.getColPixelCountLowSet());
//				float colPixelLowFract = areaStats.getColPixelLowFract();
//				System.out.println("   - bridged lows are " + areaStats.getBrgColPixelCountLowSet());
//				float bColPixelLowFract = areaStats.getBrgColPixelLowFract();
				
////				TreeSet colPixelCountLowSetFiltered = filterColumnOccupationLows(colPixelCountLowSet, areaColPixelRowCounts.length, areaPixelRows.size(), tableAreaWordHeight, docNormSpaceWidth);
////				System.out.println("   - filtered lows are " + colPixelCountLowSetFiltered);
////				float colPixelLowFractFiltered = countLowOccupationColumns(colPixelCountLowSetFiltered, tableArea.getWidth());
////				TreeSet bColPixelCountLowSetFiltered = filterColumnOccupationLows(bColPixelCountLowSet, bAreaColPixelRowCounts.length, areaPixelRows.size(), tableAreaWordHeight, docNormSpaceWidth);
////				System.out.println("   - filtered bridged lows are " + bColPixelCountLowSetFiltered);
////				float bColPixelLowFractFiltered = countLowOccupationColumns(bColPixelCountLowSetFiltered, tableArea.getWidth());
//				System.out.println("   - filtered lows are " + areaStats.getColPixelCountLowSetFiltered());
//				float colPixelLowFractFiltered = areaStats.getColPixelLowFractFiltered();
				System.out.println("   - filtered bridged lows are " + areaStats.getBrgColOccupationLowsFiltered());
				float bColPixelLowFractFiltered = areaStats.getBrgColOccupationLowFractFiltered();
				
				//	we need a good few core lows wider than average word spacing (i.e., filtered) to have any chance of a good column split
				if (bColPixelLowFractFiltered < 0.05) {
					System.out.println("   ==> cannot build table with less than 5% column gaps");
					continue;
				}
				
////				TreeSet colPixelCountLowSetCleaned = cleanColumnOccupationLows(colPixelCountLowSetFiltered, tableAreaWordHeight);
////				System.out.println("   - cleaned lows are " + colPixelCountLowSetCleaned);
////				float colPixelLowFractCleaned = countLowOccupationColumns(colPixelCountLowSetCleaned, tableArea.getWidth());
////				TreeSet bColPixelCountLowSetCleaned = cleanColumnOccupationLows(bColPixelCountLowSetFiltered, tableAreaWordHeight);
////				System.out.println("   - cleaned bridged lows are " + bColPixelCountLowSetCleaned);
////				float bColPixelLowFractCleaned = countLowOccupationColumns(bColPixelCountLowSetCleaned, tableArea.getWidth());
//				System.out.println("   - cleaned lows are " + areaStats.getColPixelCountLowSetCleaned());
//				float colPixelLowFractCleaned = areaStats.getColPixelLowFractCleaned();
//				System.out.println("   - cleaned bridged lows are " + areaStats.getBrgColPixelCountLowSetCleaned());
//				float bColPixelLowFractCleaned = areaStats.getBrgColPixelLowFractCleaned();
				
////				TreeSet colPixelCountLowSetReduced = reduceColumnOccupationLows(colPixelCountLowSetCleaned, tableAreaWordHeight);
////				System.out.println("   - reduced lows are " + colPixelCountLowSetReduced);
////				float colPixelLowFractReduced = countLowOccupationColumns(colPixelCountLowSetReduced, tableArea.getWidth());
////				TreeSet bColPixelCountLowSetReduced = reduceColumnOccupationLows(bColPixelCountLowSetCleaned, tableAreaWordHeight);
////				System.out.println("   - reduced bridged lows are " + bColPixelCountLowSetReduced);
////				float bColPixelLowFractReduced = countLowOccupationColumns(bColPixelCountLowSetReduced, tableArea.getWidth());
//				System.out.println("   - reduced lows are " + areaStats.getColPixelCountLowSetReduced());
//				float colPixelLowFractReduced = areaStats.getColPixelLowFractReduced();
				System.out.println("   - reduced bridged lows are " + areaStats.getBrgColOccupationLowsReduced());
				float bColPixelLowFractReduced = areaStats.getBrgColOccupationLowFractReduced();
				
				//	we need a good few core lows wider than average word spacing (i.e., filtered) to have any chance of a good column split
				if (bColPixelLowFractReduced < 0.05) {
					System.out.println("   ==> cannot build table with less than 5% core column gaps");
					continue;
				}
				
				/* TODO
3. Data type coherence (in simulated table markup)
- count through characters in cells:
  - upper case letters
  - lower case letters
  - letters in general
  - digits
  - hexadecimal digits
  - punctuation marks
  - frequency of each character
  - cell-frequency of each character (especially helpful with punctuation marks)
  - frequency of each type
  - cell-frequency of each type
- distinguish types:
  - numbers
  - numbers with math punctuation (e.g. "+/-")
  - numbers with punctuation (e.g. ISO dates)
  - letter codes (i.e., (mostly) upper case letter sequences)
  - letter codes with punctuation
  - hexadecimal codes (count of letters plus digits equals count of hexadecimal digits, e.g UUIDs)
  - hexadecimal codes with punctuation
  - alphanumeric codes (i.e., (mostly) upper case letter and digit sequences)
  - alphanumeric codes with punctuation
  - words
  - word sequences
  - empty cell markers (all sorts of dashes, "n/a", etc., acting as sort of wildcard)
  - arbitrary text (most general type)
- abstract values:
  - upper case letter -> 'A'
  - lower case letter -> 'a'
  - digit -> '0'
  - hexadecimal digit -> 'X'
  - retain punctuation initially (just all too different semantics)
- abstract value patterns:
  - 'A+' -> 'A'
  - 'a+' -> 'a'
  - '0+' -> '0'
  - 'X+' -> 'X'
  - still retain punctuation
- check and score compatibility of types in columns
  - observe type inheritance (TODO figure out how to do that efficiently, maybe set bits in some type code constants)
  - compare by means of equality of abstracted letters and digits as well as overlap of punctuation marks
- do same in rows (semantics might be flipped ...)
- check and score compatibility of value patterns in columns
- do same in rows (semantics might be flipped ...)
				 */
				
				//	collect words overlapping with lows to ignore them on column splitting
				HashSet multiColumnCellWords = new HashSet();
//				for (Iterator colit = bColPixelCountLowSetReduced.iterator(); colit.hasNext();) {
				for (Iterator colit = areaStats.getBrgColOccupationLowsReduced().iterator(); colit.hasNext();) {
					ColumnOccupationLow col = ((ColumnOccupationLow) colit.next());
					for (int w = 0; w < tableAreaWords.length; w++) {
//						if (tableAreaWords[w].bounds.right <= (col.left + tableArea.left))
						if (tableAreaWords[w].bounds.right <= col.left)
							continue;
//						if ((col.right + tableArea.left) <= tableAreaWords[w].bounds.left)
						if (col.right <= tableAreaWords[w].bounds.left)
							continue;
						multiColumnCellWords.add(tableAreaWords[w]);
					}
				}
				
				//	get speculative columns
				//	rationale: if we actually have a table, we should be able to distinguish at least _several_ columns
				ImRegion tableAreaTable = new ImRegion(doc, pages[pg].pageId, tableArea, ImRegion.TABLE_TYPE);
				ImRegion[] tableAreaCols = ImUtils.createTableColumns(tableAreaTable, multiColumnCellWords);
				if (tableAreaCols == null)
					continue;
				System.out.println("   - potential cells are:");
				for (int c = 0; c < tableAreaCols.length; c++) {
					System.out.println("     - in " + tableAreaCols[c].bounds);
					for (int r = 0; r < tableAreaStripes.length; r++) {
						BoundingBox tacBounds = new BoundingBox(tableAreaCols[c].bounds.left, tableAreaCols[c].bounds.right, tableAreaStripes[r].bounds.top, tableAreaStripes[r].bounds.bottom);
						ImWord[] tacWords = pages[pg].getWordsInside(tacBounds);
						String tacString = ImUtils.getString(tacWords, ImUtils.leftRightTopDownOrder, true);
						System.out.println("       - " + tacString);
					}
				}
				if (tableAreaCols.length < 2)
					continue;
				//	TODO mark region as dead if (a) only two lines contained and (b) column split fails
				
				//	TODOne use lows to identify overlapping column split ignore words (from multi-column cells) ...
				//	TODO ... and use that (a) to refine column identification and (b) to identify to-merge multi-column cells
				
				//	compute average column spacing ...
				//	TODO maybe also compute mean (by means of CountingSet and elimination of extremes)
				int columnGapSum = 0;
				for (int c = 1; c < tableAreaCols.length; c++)
					columnGapSum += (tableAreaCols[c].bounds.left - tableAreaCols[c-1].bounds.right);
				int avgColumnGap = (columnGapSum / (tableAreaCols.length - 1));
				System.out.println("   - average column gap is " + avgColumnGap);
				
				//	TODO ... and compare in-cell word spacing to that
				for (int c = 0; c < tableAreaCols.length; c++) {
					System.out.println("   - checking words distances in " + tableAreaCols[c].bounds);
					for (int r = 0; r < tableAreaStripes.length; r++) {
						BoundingBox tacBounds = new BoundingBox(tableAreaCols[c].bounds.left, tableAreaCols[c].bounds.right, tableAreaStripes[r].bounds.top, tableAreaStripes[r].bounds.bottom);
						ImWord[] tacWords = pages[pg].getWordsInside(tacBounds);
						ImUtils.sortLeftRightTopDown(tacWords);
						for (int w = 1; w < tacWords.length; w++) {
							if (tacWords[w-1].bounds.bottom < tacWords[w].centerY)
								continue; // no use comparing across line break
							int tacWordDist = (tacWords[w].bounds.left - tacWords[w-1].bounds.right);
							if ((avgColumnGap * 3) < (tacWordDist * 4)) {
								System.out.println("     - column gap grade word distance of " + tacWordDist + " in " + ImUtils.getString(tacWords, ImUtils.leftRightTopDownOrder, true) + " between '" + tacWords[w-1].getString() + "' and '" + tacWords[w].getString() + "'");
								//	TODO make this count
							}
						}
					}
				}
				
				//	(between-word spaces in the vicinity of average column spacing might well indicate a missed column split)
				//	==> TODO use that in scoring candidate tables
				
				//	TODO column data consistency as described above (cells as intersections of columns and row stripes)
				
				//	TODO also analyze row content (table might be semantically flipped)
				
				//	TODO accept either
				
				//	for now ...
				if (true)
					continue;
				
				/* TODO evaluate individual row stripe's compatibility with block column structure:
				 * - first, compute occupation of each pixel column in table area
				 * - then, score each row stripe, adding occupied rows if row stripe has a word in a column, and unoccupied rows if row stripe has a space
				 * - compute this score for full width, and normalize it
				 * - also compute this score for row stripe width, and normalize it
				 * 
				 * - maybe also compute this regarding spaces of width below document average as occupied
				 * 
				 * ==> should help identify row stripes to ignore in column re-computation
				 * 
				 * ==> looks pretty good as additional feature (significantly higher in actual tables, and distinctively low there for column spanning rows)
				 * ==> not sure if bridging below-document-average spaces is helpful or harmful, or makes a difference at all
				 * 
				 * ==> when omitting row stripes in column computation, start with those whose full-width score is lowest ...
				 * ==> ... and omit more row stripes one by one until
				 *   - columns look good (success)
				 *   - next to-omit row stripe has score above average (failure)
				 *   - more than one third of row stripes omitted (failure)
				 * ==> DO NOT strictly skip by score, but factor in column gap obstruction as well (row stripe segmentation might help here ...)
				 */
				RowStripeScore tableAreaScore = new RowStripeScore(tableArea, tableAreaStripes, null, docNormSpaceWidth, true);
				
				/* TODOne evaluate how many segments row stripes have at different gap widths:
				 * - compute sort of a "segment coloring" at different gap width thresholds
				 * - assess how many other row stripes have distinct segments (different colors) where given row stripe has different segments
				 * 
				 * ==> should help identify row stripes to ignore in column re-computation
				 * ==> should also help with column assessment below
				 * ==> simply use pixel column index as color, propagating it rightward across non-splitting spaces and words
				 */
//				int[][] stripeSegmentColors50 = new int[tableAreaStripes.length][tableArea.getWidth()];
//				int[][] stripeSegmentColors25 = new int[tableAreaStripes.length][tableArea.getWidth()];
//				int[][] stripeSegmentColors13 = new int[tableAreaStripes.length][tableArea.getWidth()];
//				int[][] stripeSegmentColors7 = new int[tableAreaStripes.length][tableArea.getWidth()];
//				int[][] stripeSegmentColorsDNS = new int[tableAreaStripes.length][tableArea.getWidth()];
//				System.out.println("     - segmenting " + tableAreaStripes.length + " row stripes:");
//				for (int r = 0; r < tableAreaStripes.length; r++) {
//					stripeSegmentColors50[r] = getRowStripeSegmentColors(tableArea, tableAreaStripes[r], (pages[pg].getImageDPI() / 50));
//					stripeSegmentColors25[r] = getRowStripeSegmentColors(tableArea, tableAreaStripes[r], (pages[pg].getImageDPI() / 25));
//					stripeSegmentColors13[r] = getRowStripeSegmentColors(tableArea, tableAreaStripes[r], (pages[pg].getImageDPI() / 13));
//					stripeSegmentColors7[r] = getRowStripeSegmentColors(tableArea, tableAreaStripes[r], (pages[pg].getImageDPI() / 7));
//					stripeSegmentColorsDNS[r] = getRowStripeSegmentColors(tableArea, tableAreaStripes[r], ((int) (tableAreaStripes[r].bounds.getHeight() * docNormWordSpacing)));
//					System.out.println("       - " + tableAreaStripes[r].bounds + ": " + tableAreaStripes[r].words[0].getString());
//					System.out.println("         " + countSegments(stripeSegmentColors50[r]) + " at gap " + (pages[pg].getImageDPI() / 50) + ", " + countSegments(stripeSegmentColors25[r]) + " at gap " + (pages[pg].getImageDPI() / 25) + ", " + countSegments(stripeSegmentColors13[r]) + " at gap " + (pages[pg].getImageDPI() / 13) + ", " + countSegments(stripeSegmentColors7[r]) + " at gap " + (pages[pg].getImageDPI() / 7) + ", " + countSegments(stripeSegmentColorsDNS[r]) + " at gap " + ((int) (tableAreaStripes[r].bounds.getHeight() * docNormWordSpacing)));
//				}
				
				/* TODO evaluate segment compatibility or interference of row stripes:
				 * - segment row stripes at minimum column margin (DPI/25 or 4/3 of normalized space, whichever is larger)
				 * - count out which segment of each row overlaps with how many segments in how many other rows ...
				 * - ... and use maximum of (overlapping segments / overlapping rows) to determine skip row stripes
				 */
				int[][] stripeSegmentColors = new int[tableAreaStripes.length][tableArea.getWidth()];
				int minSegmentGap = Math.max(Math.round(docNormSpaceWidth * tableAreaWordHeight), (pages[pg].getImageDPI() / 25));
				for (int r = 0; r < tableAreaStripes.length; r++)
					stripeSegmentColors[r] = getRowStripeSegmentColors(tableArea, tableAreaStripes[r], minSegmentGap);
				
				RowStripeSegmentation[] stripeSegments = new RowStripeSegmentation[tableAreaStripes.length];
				System.out.println("     - segmenting " + tableAreaStripes.length + " row stripes:");
				for (int r = 0; r < tableAreaStripes.length; r++) {
					System.out.println("       - " + tableAreaStripes[r].bounds + ": " + tableAreaStripes[r].words[0].getString());
					stripeSegments[r] = getRowStripeSegmentation(stripeSegmentColors[r], r, stripeSegmentColors, null);
					System.out.println("         got " + stripeSegments[r].segments.length + " segments at threshold gap " + minSegmentGap + " (" + stripeSegments[r].minGap + " min, " + stripeSegments[r].maxGap + " max, " + stripeSegments[r].avgGap + " avg)");
					for (int s = 0; s < stripeSegments[r].segments.length; s++) {
						if (s != 0)
							System.out.println("             gap also existing in " + stripeSegments[r].gaps[s-1].overlappingRowCount + " rows");
						System.out.println("           - " + stripeSegments[r].segments[s].color + " overlapping with " + stripeSegments[r].segments[s].overlappingSegmentCount + " segments in " + stripeSegments[r].segments[s].overlappingRowCount + " rows");
					}
					System.out.println("           ==> row obstructs " + stripeSegments[r].getObstructedColumnGaps() + " column gaps");
				}
				
				//	collect gaps existing in less than half of row stripes, and compute min, max, and average width of others
				int minGap = Integer.MAX_VALUE;
				int maxGap = 0;
				int gapSum = 0;
				int gapCount = 0;
				HashSet lowSupportSegments = new HashSet();
				HashSet lowSupportGaps = new HashSet();
				for (int r = 0; r < tableAreaStripes.length; r++) {
					for (int s = 0; s < stripeSegments[r].segments.length; s++) {
						if (stripeSegments[r].segments[s].overlappingRowCount < (tableAreaStripes.length / 2))
							lowSupportSegments.add(stripeSegments[r].segments[s]);
					}
					for (int g = 0; g < stripeSegments[r].gaps.length; g++) {
						if (stripeSegments[r].gaps[g].overlappingRowCount < (tableAreaStripes.length / 2))
							lowSupportGaps.add(stripeSegments[r].gaps[g]);
						else {
							int gap = (stripeSegments[r].gaps[g].end - stripeSegments[r].gaps[g].start);
							minGap = Math.min(minGap, gap);
							maxGap = Math.max(maxGap, gap);
							gapSum += gap;
							gapCount++;
						}
					}
				}
				int avgGap = ((gapCount == 0) ? 0 : ((gapSum + (gapCount / 2)) / gapCount));
				System.out.println("     - found "  + lowSupportSegments.size() + " rare segments and " + lowSupportGaps.size() + " rare gaps, others are " + avgGap + " on average (" + minGap + " min, " + maxGap + " max)");
				
				//	close below-average width gaps existing in less than half of row stripes
				if ((lowSupportSegments.size() != 0) || (lowSupportGaps.size() != 0)) {
					
					//	close below-average width gaps existing in less than half of row stripes
					for (int r = 0; r < tableAreaStripes.length; r++)
						for (int g = 0; g < stripeSegments[r].gaps.length; g++) {
							if (stripeSegments[r].gaps[g].overlappingRowCount >= (tableAreaStripes.length / 2))
								continue;
							if ((stripeSegments[r].gaps[g].end - stripeSegments[r].gaps[g].start) >= avgGap)
								continue;
							for (int x = stripeSegments[r].gaps[g].start; x < stripeSegments[r].gaps[g].end; x++)
								stripeSegmentColors[r][x] = stripeSegmentColors[r][x-1];
							for (int x = stripeSegments[r].gaps[g].end; (x < stripeSegmentColors[r].length) && (stripeSegmentColors[r][x] != 0); x++)
								stripeSegmentColors[r][x] = stripeSegmentColors[r][x-1];
						}
					
					//	close narrower gap to left or right of low-support segments
					for (int r = 0; r < tableAreaStripes.length; r++) {
						if (stripeSegments[r].segments.length == 1)
							continue;
						for (int s = 0; s < stripeSegments[r].segments.length; s++) {
							if (stripeSegments[r].segments[s].overlappingRowCount >= (tableAreaStripes.length / 2))
								continue;
							int narrowerAdjacentGapIndex;
							if (s == 0)
								narrowerAdjacentGapIndex = 0;
							else if ((s+1) == stripeSegments[r].segments.length)
								narrowerAdjacentGapIndex = (s-1);
							else if ((stripeSegments[r].gaps[s-1].end - stripeSegments[r].gaps[s-1].start) > (stripeSegments[r].gaps[s].end - stripeSegments[r].gaps[s].start))
								narrowerAdjacentGapIndex = s;
							else narrowerAdjacentGapIndex = (s-1);
							if ((stripeSegments[r].gaps[narrowerAdjacentGapIndex].end - stripeSegments[r].gaps[narrowerAdjacentGapIndex].start) >= avgGap)
								continue;
							for (int x = stripeSegments[r].gaps[narrowerAdjacentGapIndex].start; x < stripeSegments[r].gaps[narrowerAdjacentGapIndex].end; x++)
								stripeSegmentColors[r][x] = stripeSegmentColors[r][x-1];
							for (int x = stripeSegments[r].gaps[narrowerAdjacentGapIndex].end; (x < stripeSegmentColors[r].length) && (stripeSegmentColors[r][x] != 0); x++)
								stripeSegmentColors[r][x] = stripeSegmentColors[r][x-1];
						}
					}
					
					//	re-compute segmentation with closed gaps
					System.out.println("     - re-segmenting " + tableAreaStripes.length + " row stripes:");
					for (int r = 0; r < tableAreaStripes.length; r++) {
						System.out.println("       - " + tableAreaStripes[r].bounds + ": " + tableAreaStripes[r].words[0].getString());
						stripeSegments[r] = getRowStripeSegmentation(stripeSegmentColors[r], r, stripeSegmentColors, null);
						System.out.println("         got " + stripeSegments[r].segments.length + " segments at threshold gap " + minSegmentGap + " (" + stripeSegments[r].minGap + " min, " + stripeSegments[r].maxGap + " max, " + stripeSegments[r].avgGap + " avg)");
						for (int s = 0; s < stripeSegments[r].segments.length; s++) {
							if (s != 0)
								System.out.println("             gap also existing in " + stripeSegments[r].gaps[s-1].overlappingRowCount + " rows");
							System.out.println("           - " + stripeSegments[r].segments[s].color + " overlapping with " + stripeSegments[r].segments[s].overlappingSegmentCount + " segments in " + stripeSegments[r].segments[s].overlappingRowCount + " rows");
						}
						System.out.println("           ==> row obstructs " + stripeSegments[r].getObstructedColumnGaps() + " column gaps");
					}
				}
				
				//	exclude table with more than one third column gap obstructing rows
				int gapObstructingRowCount = 0;
				for (int r = 0; r < tableAreaStripes.length; r++) {
					if (stripeSegments[r].getObstructedColumnGaps() != 0)
						gapObstructingRowCount++;
				}
				System.out.println("     - got " + gapObstructingRowCount + " out of " + tableAreaStripes.length + " column gap obstructing row stripes");
				if ((gapObstructingRowCount * 3) > tableAreaStripes.length) {
					System.out.println("     ==> column gaps too inconsistent");
					continue;
				}
				
				//	get potential columns (there have to be some, at least, even in presence of multi-column cells)
				ImRegion tableAreaRegion = new ImRegion(doc, pages[pg].pageId, tableArea, ImRegion.TABLE_TYPE);
				ImRegion[] tableAreaColumns = ImUtils.createTableColumns(tableAreaRegion, (pages[pg].getImageDPI() / 25), 0); // use 1 mm as minimum gap here, as we have the graphics for insurance
				if (checkColumns(pages[pg], tableAreaColumns, tableAreaStripes)) {
					System.out.println("     ==> looking good with all rows");
				}
				
				//	get potential columns omitting any column gap obstructing rows
				ImRegion[] ouTableAreaColumns;
				if (gapObstructingRowCount == 0)
					ouTableAreaColumns = tableAreaColumns;
				else {
					HashSet skipRowWords = new HashSet();
					for (int r = 0; r < tableAreaStripes.length; r++) {
						if (stripeSegments[r].getObstructedColumnGaps() != 0)
							skipRowWords.addAll(Arrays.asList(tableAreaStripes[r].words));
					}
					ouTableAreaColumns = ImUtils.createTableColumns(tableAreaRegion, skipRowWords, (pages[pg].getImageDPI() / 25), 0); // use 1 mm as minimum gap here, as we have the graphics for insurance
					if (checkColumns(pages[pg], ouTableAreaColumns, tableAreaStripes)) {
						System.out.println("     ==> looking good with omitting rows");
					}
				}
				
				
				
				//	TODO_not check row stripe density distribution as well (there has to be some significant outliers for row skipping to make sense)
				//	==> might be true for intermediate labels, but not for multi-column cells in general
				//	==> let row stripe segmentation based column split assessment take care of this
				
				//	check columns, and try leaving out high-density stripes if columns no good
				if (!checkColumns(pages[pg], tableAreaColumns, tableAreaStripes)) {
					boolean[] skipRow = new boolean[tableAreaStripes.length];
					Arrays.fill(skipRow, false);
					int skipRowCount = 0;
					RowStripeScore skipTableAreaScore = tableAreaScore;
					HashSet skipRowWords = new HashSet();
					
					//	skip more and more rows one by one
					//	TODO make sure not to stop all too early, though, as that might mess up table
					//	==> TODO keep skipping a little more even when columns check out, collecting results, and pick best columns in the end
					/*	==> TODO find column scoring scheme:
					 * - few rows skipped
					 * - gaps consistent (low difference between minimum and maximum, or low oddity)
					 * - columns internally consistent, and well-occupied
					 * 
					 * ==> TODO_not consider re-scoring solely based on non-skipped row stripes after each round (alleviates mutual scoring of skipped rows)
					 * ==> tried it, doesn't seem to work all that well ...
					 */
					while ((skipRowCount * 3) <= tableAreaStripes.length) {
						
						//	find next row to skip
						int skipRowIndex = -1;
						float skipRowScore = 1;
						for (int r = 0; r < tableAreaStripes.length; r++) {
							if (skipRow[r])
								continue; // already skipped
//							if (areaWidthColOccupationScores[r] < skipRowScore) {
//								skipRowIndex = r;
//								skipRowScore = areaWidthColOccupationScores[r];
//							}
							if (skipTableAreaScore.areaWidthColOccupationScores[r] < skipRowScore) {
								skipRowIndex = r;
								skipRowScore = skipTableAreaScore.areaWidthColOccupationScores[r];
							}
						}
						
						//	nothing left to skip, so we don't have a table
						if (skipRowIndex == -1) {
							tableAreaColumns = null;
							System.out.println("     ==> no more rows to skip");
							break;
						}
						
						//	mark and count row as skipped
						skipRow[skipRowIndex] = true;
						skipRowCount++;
						System.out.println("     - omitting row " + tableAreaStripes[skipRowIndex].words[0].getString());
						
						//	skipped too many row stripes already, so we don't have a table
						if ((skipRowCount * 3) > tableAreaStripes.length) {
							tableAreaColumns = null;
							System.out.println("     ==> skipped too many rows already");
							break;
						}
						
						//	skip threshold came up above average, so we don't have a table
//						if (skipRowScore >= areaWidthColOccupationScoreAvg) {
//							tableAreaColumns = null;
//							System.out.println("     ==> no use skipping row scored above average");
//							break;
//						}
						if (skipRowScore >= tableAreaScore.areaWidthColOccupationScoreAvg) {
							tableAreaColumns = null;
							System.out.println("     ==> no use skipping row scored above average");
							break;
						}
						
						//	add row words to skip set, and re-compute columns
						skipRowWords.addAll(Arrays.asList(tableAreaStripes[skipRowIndex].words));
						tableAreaColumns = ImUtils.createTableColumns(tableAreaRegion, skipRowWords, (pages[pg].getImageDPI() / 25), 0); // use 1 mm as minimum gap here, as we have the graphics for insurance
						System.out.println("     - columns re-computed omitting " + skipRowWords.size() + " words");
						
						//	re-score remaining row stripes
						//	==> doesn't seem to work all that well ...
						skipTableAreaScore = new RowStripeScore(tableArea, tableAreaStripes, skipRow, docNormSpaceWidth, true);
//						
//						//	check result
//						if (checkColumns(pages[pg], tableAreaColumns, tableAreaStripes))
//							break;
					}
//					for (int r = 0; r < tableAreaStripes.length; r++) {
//						if (tableAreaStripes[r].getWordDensity() > docWordDensity1d)
//							skipRowWords.addAll(Arrays.asList(tableAreaStripes[r].words));
//					}
//					tableAreaColumns = ImUtils.createTableColumns(tableAreaRegion, skipRowWords, (pages[pg].getImageDPI() / 25), 0); // use 1 mm as minimum gap here, as we have the graphics for insurance
//					System.out.println("     - columns re-computed omitting " + skipRowWords.size() + " words");
//					if (!checkColumns(pages[pg], tableAreaColumns, tableAreaStripes))
//						continue;
					if (tableAreaColumns == null)
						continue;
				}
				
				//	mark table candidate for further assessment
				GraphTableCandiate tableCand = new GraphTableCandiate(tableArea, tableAreaStripes, tableAreaColumns);
				
				//	finally ...
				System.out.println("     ==> looking good for a table");
				pageTableCandidates.add(tableCand);
			}
			
			//	anything left to work with?
			if (pageTableCandidates.size() == 0)
				continue;
			if (true)
				continue;
			
			//	filter table candidates contained in others and having no more columns than containing one
			System.out.println(" - filtering " + pageTableCandidates.size() + " candidate table grids");
			for (int t = 0; t < pageTableCandidates.size(); t++) {
				GraphTableCandiate tableCand = ((GraphTableCandiate) pageTableCandidates.get(t));
				for (int ct = (t+1); ct < pageTableCandidates.size(); ct++) {
					GraphTableCandiate cTableCand = ((GraphTableCandiate) pageTableCandidates.get(ct));
					if ((cTableCand.columns.length <= tableCand.columns.length) && cTableCand.bounds.liesIn(tableCand.bounds, false)) {
						pageTableCandidates.remove(ct--);
						System.out.println("   - removed " + cTableCand.bounds + " as contained in " + tableCand.bounds);
					}
				}
			}
			
			//	filter table candidates containing two or more others with more columns than they have themselves (helps sort out stacked tables with partially compatible column gaps marked as one)
			for (int t = 0; t < pageTableCandidates.size(); t++) {
				GraphTableCandiate tableCand = ((GraphTableCandiate) pageTableCandidates.get(t));
				ArrayList subTableCands = new ArrayList();
				for (int ct = (t+1); ct < pageTableCandidates.size(); ct++) {
					GraphTableCandiate cTableCand = ((GraphTableCandiate) pageTableCandidates.get(ct));
					if ((cTableCand.columns.length > tableCand.columns.length) && cTableCand.bounds.liesIn(tableCand.bounds, false))
						subTableCands.add(cTableCand);
				}
				if (subTableCands.size() > 1) {
					pageTableCandidates.remove(t--);
					System.out.println("   - removed " + tableCand.bounds + " as conflation of " + subTableCands.size() + " better candidates:");
					for (int s = 0; s < subTableCands.size(); s++) {
						GraphTableCandiate subTableCand = ((GraphTableCandiate) subTableCands.get(s));
						System.out.println("     - " + subTableCand.bounds);
					}
				}
			}
			
			//	filter table candidates overlapping two or more others with at least as many columns as they have themselves, and lower column gap oddity (helps sort out stacked tables with partially compatible column gaps marked as one)
			for (int t = 0; t < pageTableCandidates.size(); t++) {
				GraphTableCandiate tableCand = ((GraphTableCandiate) pageTableCandidates.get(t));
				ArrayList subTableCands = new ArrayList();
				for (int ct = (t+1); ct < pageTableCandidates.size(); ct++) {
					GraphTableCandiate cTableCand = ((GraphTableCandiate) pageTableCandidates.get(ct));
					if ((cTableCand.columns.length >= tableCand.columns.length) && (cTableCand.getColumnGapOddity() < tableCand.getColumnGapOddity()) && cTableCand.bounds.overlaps(tableCand.bounds))
						subTableCands.add(cTableCand);
				}
				if (subTableCands.size() > 1) {
					pageTableCandidates.remove(t--);
					System.out.println("   - removed " + tableCand.bounds + " as overlapping with " + subTableCands.size() + " better candidates:");
					for (int s = 0; s < subTableCands.size(); s++) {
						GraphTableCandiate subTableCand = ((GraphTableCandiate) subTableCands.get(s));
						System.out.println("     - " + subTableCand.bounds);
					}
				}
			}
			
			//	shrink tables to contained words, and show what we have
			System.out.println(" - retained " + pageTableCandidates.size() + " candidate table grids:");
			for (int t = 0; t < pageTableCandidates.size(); t++) {
				GraphTableCandiate tableCand = ((GraphTableCandiate) pageTableCandidates.get(t));
				System.out.println("   - " + tableCand.bounds + ":");
				BoundingBox tableWordBounds = ImLayoutObject.getAggregateBox(pages[pg].getWordsInside(tableCand.bounds));
				if (tableWordBounds != null) {
					tableCand = new GraphTableCandiate(tableWordBounds, tableCand.rowStripes, tableCand.columns);
					pageTableCandidates.set(t, tableCand);
					System.out.println("     - bounding box reduced to words: " + tableWordBounds);
				}
				System.out.println("     - " + tableCand.columns.length + " columns, gaps are " + tableCand.avgColumnGap + " on average (min " + tableCand.minColumnGap + ", max " + tableCand.maxColumnGap + "):");
				for (int c = 0; c < tableCand.columns.length; c++) {
					ImWord[] colWords = pages[pg].getWordsInside(tableCand.columns[c].bounds);
					ImUtils.sortLeftRightTopDown(colWords);
					System.out.println("       - " + tableCand.columns[c].bounds + ": " + ((colWords.length == 0) ? "<empty>" : colWords[0].getString()));
				}
				System.out.println("     - column gap oddity quotient is " + tableCand.getColumnGapOddity());
				System.out.println("     - " + tableCand.rowStripes.length + " rows:");
				for (int r = 0; r < tableCand.rowStripes.length; r++)
					System.out.println("       - " + tableCand.rowStripes[r].bounds + ": " + tableCand.rowStripes[r].words[0].getString());
				RowStripeGroup[] rowStripeGroups = getRowStripeGroups(pages[pg], tableCand.bounds, tableCand.rowStripes);
				System.out.println("     - row stripes grouped into " + rowStripeGroups.length + " table rows:");
				for (int g = 0; g < rowStripeGroups.length; g++) {
					System.out.println("       - " + rowStripeGroups[g].bounds);
					for (int r = 0; r < rowStripeGroups[g].rowStripes.length; r++)
						System.out.println("         - " + rowStripeGroups[g].rowStripes[r].bounds + ": " + rowStripeGroups[g].rowStripes[r].words[0].getString() + " (spaces are " + rowStripeGroups[g].rowStripes[r].aSpace + " above, " + rowStripeGroups[g].rowStripes[r].bSpace + " below)");
				}
			}
		}
		
		if (true)
			return;
		
		//	assess block density for individual pages
		BlockData[][] pageBlockData = new BlockData[pages.length][];
		CountingSet docBlockDensities = new CountingSet(new TreeMap());
		CountingSet docStripeDensities = new CountingSet(new TreeMap());
		for (int pg = 0; pg < pages.length; pg++) {
			System.out.println("Assessing page " + pages[pg].pageId);
			
			RowStripe[] pgStripes = getRowStripes(pages[pg], pages[pg].bounds);
			System.out.println(" - got " + pgStripes.length + " overall stripes");
			float pgWordDensity = getWordDensity(pgStripes);
			System.out.println(" --> overall word density is " + pgWordDensity);
			
			//	assess page blocks
			ImRegion[] pgBlocks = pages[pg].getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
			Arrays.sort(pgBlocks, ImUtils.topDownOrder);
			ArrayList pgBlockData = new ArrayList();
			for (int b = 0; b < pgBlocks.length; b++) {
				System.out.println(" - assessing block " + pgBlocks[b].bounds);
				
				RowStripe[] bStripes = getRowStripes(pages[pg], pgBlocks[b].bounds);
				if (bStripes.length == 0) {
					System.out.println("   --> empty block");
					continue;
				}
				System.out.println("   - got " + bStripes.length + " block stripes");
				float bWordDensity = getWordDensity(bStripes);
				System.out.println("   --> block word density is " + bWordDensity);
				
				docBlockDensities.add(new Integer((int) (20 * bWordDensity)));
				for (int s = 0; s < bStripes.length; s++)
					docStripeDensities.add(new Integer((int) (20 * bStripes[s].getWordDensity())));
				
				pgBlockData.add(new BlockData(pgBlocks[b], bWordDensity, bStripes));
			}
			
			//	store blocks and stripes
			pageBlockData[pg] = ((BlockData[]) pgBlockData.toArray(new BlockData[pgBlockData.size()]));
//			
//			ImRegion[] paragraphs = pages[pg].getRegions(ImRegion.PARAGRAPH_TYPE);
//			for (int p = 0; p < paragraphs.length; p++) {
//				System.out.println(" - assessing paragra " + paragraphs[p].bounds);
//				
//				float pWordDensity = assessWordDensity(paragraphs[p]);
//				System.out.println("   --> paragraph word density is " + pWordDensity);
//			}
		}
//		
//		//	compute average density of document blocks
//		int stripeWidthSum = 0;
//		int wordWidthSum = 0;
//		int stripeArea = 0;
//		int wordArea = 0;
//		int wordDistanceSum = 0;
//		float nWordDistanceSum = 0;
//		int wordDistanceCount = 0;
//		for (int pg = 0; pg < pages.length; pg++) {
//			for (int b = 0; b < pageBlockData[pg].length; b++)
//				for (int s = 0; s < pageBlockData[pg][b].rowStripes.length; s++) {
//					stripeWidthSum += (pageBlockData[pg][b].rowStripes[s].bounds.right - pageBlockData[pg][b].rowStripes[s].bounds.left);
//					wordWidthSum += pageBlockData[pg][b].rowStripes[s].wordWidthSum;
//					stripeArea += pageBlockData[pg][b].rowStripes[s].area;
//					wordArea += pageBlockData[pg][b].rowStripes[s].wordArea;
//					wordDistanceSum += pageBlockData[pg][b].rowStripes[s].wordDistanceSum;
//					nWordDistanceSum += pageBlockData[pg][b].rowStripes[s].nWordDistanceSum;
//					wordDistanceCount += pageBlockData[pg][b].rowStripes[s].wordDistanceCount;
//				}
//		}
//		float docWordDensity1d = ((stripeWidthSum < 1) ? 0 : (((float) wordWidthSum) / stripeWidthSum));
//		float docWordDensity2d = ((stripeArea < 1) ? 0 : (((float) wordArea) / stripeArea));
//		System.out.println("Average document word density is " + docWordDensity1d + " horizonatally, " + docWordDensity2d + " by area");
//		float docWordSpacing = ((wordDistanceCount < 1) ? 0 : (((float) wordDistanceSum) / wordDistanceCount));
//		System.out.println("Average document word spacing " + docWordSpacing);
//		float docNormWordSpacing = ((wordDistanceCount < 1) ? 0 : (((float) nWordDistanceSum) / wordDistanceCount));
//		System.out.println("Average normalized document word spacing " + docNormWordSpacing);
//		System.out.println("Block density distripution:");
//		for (int d = 20; d >= 0; d--)
//			System.out.println(" - " + (d * 5) + ": " + docBlockDensities.getCount(new Integer(d)));
//		System.out.println("Stripe density distripution:");
//		for (int d = 20; d >= 0; d--)
//			System.out.println(" - " + (d * 5) + ": " + docStripeDensities.getCount(new Integer(d)));
//		//	as it seems (at least from 3 test documents of rather different layout), document average block density makes for a good threshold above which a block is surely NOT a table
		
		//	assess page blocks
		for (int pg = 0; pg < pages.length; pg++) {
			
			//	assess individual blocks
			for (int b = 0; b < pageBlockData[pg].length; b++) {
				
				//	this one is too dense to be a table
				if (pageBlockData[pg][b].density >= docWordDensity1d)
					continue;
				
				//	assess block
				assessBlockData(pages[pg], pageBlockData[pg][b], docWordDensity1d);
			}
			
			//	try and assemble row stripe groups into tables
			//	TODO do this _per_column_, so top-down sort order doesn't alternate left-right in two-column layouts
			ArrayList tableRowStripeGroups = new ArrayList();
			ArrayList tableCandidates = new ArrayList();
			for (int b = 0; b < pageBlockData[pg].length; b++) {
				
				//	this one is too dense to be a table
				if (pageBlockData[pg][b].density >= docWordDensity1d) {
					tableRowStripeGroups.clear(); // cannot continue table across this block
					continue;
				}
				
				//	this one was excluded as a table
				if (pageBlockData[pg][b].rowStripeGroups == null) {
					tableRowStripeGroups.clear(); // cannot continue table across this block
					continue;
				}
				
				//	assess individual row stripe groups
				for (int sg = 0; sg < pageBlockData[pg][b].rowStripeGroups.length; sg++) {
					
					//	table ends here
					if (!pageBlockData[pg][b].rowStripeGroups[sg].isTablePart) {
						tableRowStripeGroups.clear(); // cannot continue table across this row stripe group
						continue;
					}
					
					//	add current row stripe group ...
					tableRowStripeGroups.add(pageBlockData[pg][b].rowStripeGroups[sg]);
					
					//	... and check for table
					TableCandidate tabCand = checkForTable(tableRowStripeGroups, docWordDensity1d);
					
					//	row stripes incompatible, start over with current one alone
					if (tabCand == null) {
						tableRowStripeGroups.clear();
						sg--;
						continue;
					}
					
					//	too few full width row stripes
					if (tabCand.fullWidthRowStripeCount < 3) {
						System.out.println(" ==> too few full-width rows for fully blown table, need mergers");
						continue;
					}
					
					//	this one looks good, keep it
					tableCandidates.add(tabCand);
					System.out.println(" ==> table candidate retained for further assessment");
				}
			}
			
			//	sort table candidates ()
			Collections.sort(tableCandidates);
			
			//	remove table candidates nested in others
			//	TODO maybe add some comparative checks
			for (int tc = 0; tc < tableCandidates.size(); tc++) {
				TableCandidate tabCand = ((TableCandidate) tableCandidates.get(tc));
				for (int ctc = (tc+1); ctc < tableCandidates.size(); ctc++) {
					TableCandidate cTabCand = ((TableCandidate) tableCandidates.get(ctc));
					if (tabCand.bounds.includes(cTabCand.bounds, false))
						tableCandidates.remove(ctc--);
				}
			}
			
			//	what do we have?
			System.out.println("Found " + tableCandidates.size() + " likely tables in page " + pg + ":");
			for (int tc = 0; tc < tableCandidates.size(); tc++) {
				TableCandidate tabCand = ((TableCandidate) tableCandidates.get(tc));
				System.out.println(" - " + tabCand.bounds + ", rows groups are:");
				for (int sg = 0; sg < tabCand.rowStripeGroups.length; sg++) {
					System.out.print("   - " + tabCand.rowStripeGroups[sg].bounds + ": ");
					for (int rs = 0; rs < tabCand.rowStripeGroups[sg].rowStripes.length; rs++)
						System.out.print(((rs == 0) ? "" : ", ") + tabCand.rowStripeGroups[sg].rowStripes[rs].words[0].getString());
					System.out.println();
				}
				
				//	try column split, requiring at least 3 columns
				ImRegion tabReg = new ImRegion(doc, pages[pg].pageId, tabCand.bounds, ImRegion.TABLE_TYPE);
				ImRegion[] tabCols = ImUtils.getTableColumns(tabReg, (pages[pg].getImageDPI() / 13), 0);
				if (tabCols == null) {
					System.out.println(" ==> could not identify columns");
					tableCandidates.remove(tc--);
					continue;
				}
				if (tabCols.length < 3) {
					System.out.println(" ==> got only " + tabCols.length + " columns, too few");
					tableCandidates.remove(tc--);
					continue;
				}
				System.out.println(" - " + tabCols.length + " columns");
				for (int c = 0; c < tabCols.length; c++) {
					ImWord[] colWords = pages[pg].getWordsInside(tabCols[c].bounds);
					Arrays.sort(colWords, ImUtils.topDownOrder);
					System.out.println("   - " + tabCols[c].bounds + ": " + colWords[0].getString());
				}
				
				//	TODO merge columns with single words into nearest more strongly occupied column
				
				//	TODO assess column symmetry
			}
		}
	}
	
	private static TableAreaCandidate[] findHorizontalLineGridTables(ImPage page, Graphics[] pageGraphics, float docWordHeight, float normSpaceWidth) {
		if (pageGraphics.length == 0)
			return null; // nothing to work with
		System.out.println("Assessing potential table grids in page " + page.pageId);
		
		//	sort graphics top-down
		Arrays.sort(pageGraphics, topDownGraphicsOrder);
		System.out.println(" - " + pageGraphics.length + " graphics sorted");
		
		/* mark region as non-table area if
		 * (a) only two lines contained
		 * (b) lines more than half average word height apart (to account for double lines)
		 * (c) no words contained at all */
		ArrayList pageNonTableAreas = new ArrayList();
		for (int g = 0; g < pageGraphics.length; g++) {
			BoundingBox graphBounds = pageGraphics[g].getBounds();
			System.out.println(" - seeking non-table areas starting with " + graphBounds);
			
			//	combine with other graphics in same column
			for (int cg = (g+1); cg < pageGraphics.length; cg++) {
				BoundingBox cGraphBounds = pageGraphics[cg].getBounds();
//				System.out.println("     - checking against " + cGraphBounds + " ...");
				
				//	check column overlap (require 97% to accommodate sloppy layout)
				int uWidth = (Math.max(graphBounds.right, cGraphBounds.right) - Math.min(graphBounds.left, cGraphBounds.left));
				int iWidth = (Math.min(graphBounds.right, cGraphBounds.right) - Math.max(graphBounds.left, cGraphBounds.left));
				if ((iWidth * 100) < (uWidth * 97)) {
//					System.out.println("       ==> alignment mismatch");
					continue;
				}
				
				//	check combined height
				int uHeight = (Math.max(graphBounds.bottom, cGraphBounds.bottom) - Math.min(graphBounds.top, cGraphBounds.top));
				if ((uHeight * 2) < docWordHeight) {
//					System.out.println("       ==> lower than half a word, potential double line");
					continue;
				}
				
				//	mark potential non-table area
				BoundingBox uGraphBounds = new BoundingBox(
						Math.min(graphBounds.left, cGraphBounds.left),
						Math.max(graphBounds.right, cGraphBounds.right),
						Math.min(graphBounds.top, cGraphBounds.top),
						Math.max(graphBounds.bottom, cGraphBounds.bottom)
					);
				
				//	check for any contained graphics
				for (int og = (g+1); og < pageGraphics.length; og++) {
					if (og == cg)
						break; // no need to seek any further thanks to top-down sorting
					BoundingBox oGraphBounds = pageGraphics[og].getBounds();
					
					//	check width (require 97% to accommodate sloppy layout)
					if ((oGraphBounds.getWidth() * 100) < (uGraphBounds.getWidth() * 97))
						continue;
					
					//	check overlap (having checked width before, fuzzy inclusion should do)
					if (!uGraphBounds.includes(oGraphBounds, true))
						continue;
					
					//	we can exclude this one
					uGraphBounds = null;
//					System.out.println("       ==> contains " + oGraphBounds);
					break;
				}
				if (uGraphBounds == null)
					continue;
				
				//	check for contained words
				ImWord[] uGraphWords = page.getWordsInside(uGraphBounds);
				
				//	mark empty non-table area
				if (uGraphWords.length == 0) {
					pageNonTableAreas.add(uGraphBounds);
					System.out.println("   - got non-table area together with " + cGraphBounds + ": " + uGraphBounds);
					continue;
				}
				
				//	check for table caption
				ImUtils.sortLeftRightTopDown(uGraphWords);
				String uGraphStartWordString = uGraphWords[0].getString().toLowerCase();
				if (uGraphStartWordString.equals("t") || uGraphStartWordString.startsWith("tab")) {// need to catch word boundary due to font size change emulating small caps in some layouts
					String uGraphStartString = ImUtils.getString(uGraphWords[0], uGraphWords[Math.min(uGraphWords.length, 5) - 1], true);
					if (uGraphStartString.matches("[Tab|TAB][^\\s]*\\.?\\s*[1-9][0-9]*.*")) {
						pageNonTableAreas.add(uGraphBounds);
						System.out.println("   - got caption non-table area together with " + cGraphBounds + ": " + uGraphBounds);
						continue;
					}
				}
				
				//	TODO anything else to check?
			}
		}
		
		//	group graphics into potential table grids
		ArrayList pageTableAreas = new ArrayList();
		for (int g = 0; g < pageGraphics.length; g++) {
			BoundingBox graphBounds = pageGraphics[g].getBounds();
			System.out.println(" - seeking table grids starting with " + graphBounds);
			
			//	graphics is tall enough to be table grid in its own right (full-grid tables !!!)
			//	TODO move this part to dedicated seeker method for full grid tables (distinction via document style parameters)
			if (graphBounds.getHeight() > page.getImageDPI()) {
//				pageTableAreas.add(graphBounds);
				System.out.println("   - tall enough for one-piece full table grid");
			}
			
			//	combine with other graphics in same column
			for (int cg = (g+1); cg < pageGraphics.length; cg++) {
				BoundingBox cGraphBounds = pageGraphics[cg].getBounds();
				
				//	check column overlap (require 97% to accommodate sloppy layout)
				int uWidth = (Math.max(graphBounds.right, cGraphBounds.right) - Math.min(graphBounds.left, cGraphBounds.left));
				int iWidth = (Math.min(graphBounds.right, cGraphBounds.right) - Math.max(graphBounds.left, cGraphBounds.left));
				if ((iWidth * 100) < (uWidth * 97))
					continue;
				
				//	check combined height TODO figure out sensible minimum
				int uHeight = (Math.max(graphBounds.bottom, cGraphBounds.bottom) - Math.min(graphBounds.top, cGraphBounds.top));
				if ((uHeight * 2) < page.getImageDPI())
					continue;
				
				//	mark potential table bounds
				BoundingBox uGraphBounds = new BoundingBox(
						Math.min(graphBounds.left, cGraphBounds.left),
						Math.max(graphBounds.right, cGraphBounds.right),
						Math.min(graphBounds.top, cGraphBounds.top),
						Math.max(graphBounds.bottom, cGraphBounds.bottom)
					);
				
				//	check if area includes any non-table areas
				for (int nta = 0; nta < pageNonTableAreas.size(); nta++) {
					BoundingBox ntaBounds = ((BoundingBox) pageNonTableAreas.get(nta));
					if (uGraphBounds.includes(ntaBounds, true)) {
						uGraphBounds = null;
						break;
					}
				}
				if (uGraphBounds == null)
					continue;
				
				//	store potential table area
				pageTableAreas.add(uGraphBounds);
				System.out.println("   - got potential partial table grid together with " + cGraphBounds + ": " + uGraphBounds);
			}
		}
		
		//	anything left to work with?
		if (pageTableAreas.size() == 0)
			return null;
		
		//	order potential table areas by descending size (makes sure to find whole tables before partial ones)
		Collections.sort(pageTableAreas, descendingBoxSizeOrder);
		
		//	assess potential table areas
		System.out.println(" - assessing " + pageTableAreas.size() + " potential table grids");
		ArrayList pageTableAreaCandidates = new ArrayList();
		for (int t = 0; t < pageTableAreas.size(); t++) {
			BoundingBox tableArea = ((BoundingBox) pageTableAreas.get(t));
			System.out.println("   - assessing " + tableArea);
			TableAreaCandidate tac = getTableAreaCandidate(page, tableArea, normSpaceWidth);
			if (tac != null)
				pageTableAreaCandidates.add(tac);
		}
		
		//	anything to work with?
		if (pageTableAreaCandidates.size() == 0)
			return null;
		
		//	filter table candidates contained in others and having no more columns than containing one
		System.out.println(" - filtering " + pageTableAreaCandidates.size() + " candidate table grids");
		for (int t = 0; t < pageTableAreaCandidates.size(); t++) {
			TableAreaCandidate tac = ((TableAreaCandidate) pageTableAreaCandidates.get(t));
			for (int ct = (t+1); ct < pageTableAreaCandidates.size(); ct++) {
				TableAreaCandidate cTac = ((TableAreaCandidate) pageTableAreaCandidates.get(ct));
				if ((cTac.cols.length <= tac.cols.length) && cTac.bounds.liesIn(tac.bounds, false)) {
					pageTableAreaCandidates.remove(ct--);
					System.out.println("   - removed " + cTac.bounds + " as contained in " + tac.bounds);
				}
			}
		}
		
		//	filter table candidates containing two or more others with more columns than they have themselves (helps sort out stacked tables with partially compatible column gaps marked as one)
		for (int t = 0; t < pageTableAreaCandidates.size(); t++) {
			TableAreaCandidate tac = ((TableAreaCandidate) pageTableAreaCandidates.get(t));
			ArrayList subTableAreaCandidates = new ArrayList();
			for (int ct = (t+1); ct < pageTableAreaCandidates.size(); ct++) {
				TableAreaCandidate cTac = ((TableAreaCandidate) pageTableAreaCandidates.get(ct));
				if ((cTac.cols.length > tac.cols.length) && cTac.bounds.liesIn(tac.bounds, false))
					subTableAreaCandidates.add(cTac);
			}
			if (subTableAreaCandidates.size() > 1) {
				pageTableAreaCandidates.remove(t--);
				System.out.println("   - removed " + tac.bounds + " as likely conflation of " + subTableAreaCandidates.size() + " better candidates:");
				for (int s = 0; s < subTableAreaCandidates.size(); s++) {
					TableAreaCandidate sTac = ((TableAreaCandidate) subTableAreaCandidates.get(s));
					System.out.println("     - " + sTac.bounds);
				}
			}
		}
//		
//		//	filter table candidates overlapping two or more others with at least as many columns as they have themselves, and lower column gap oddity (helps sort out stacked tables with partially compatible column gaps marked as one)
//		for (int t = 0; t < pageTableAreaCandidates.size(); t++) {
//			TableAreaCandidate tac = ((TableAreaCandidate) pageTableAreaCandidates.get(t));
//			ArrayList subTableCands = new ArrayList();
//			for (int ct = (t+1); ct < pageTableAreaCandidates.size(); ct++) {
//				TableAreaCandidate cTac = ((TableAreaCandidate) pageTableAreaCandidates.get(ct));
//				if ((cTac.cols.length >= tac.cols.length) && (cTac.getColumnGapOddity() < tac.getColumnGapOddity()) && cTac.bounds.overlaps(tac.bounds))
//					subTableCands.add(cTac);
//			}
//			if (subTableCands.size() > 1) {
//				pageTableAreaCandidates.remove(t--);
//				System.out.println("   - removed " + tac.bounds + " as overlapping with " + subTableCands.size() + " better candidates:");
//				for (int s = 0; s < subTableCands.size(); s++) {
//					TableAreaCandidate subTableCand = ((TableAreaCandidate) subTableCands.get(s));
//					System.out.println("     - " + subTableCand.bounds);
//				}
//			}
//		}
		
		//	show what we have
		System.out.println(" - retained " + pageTableAreaCandidates.size() + " candidate table grids:");
		for (int t = 0; t < pageTableAreaCandidates.size(); t++) {
			TableAreaCandidate tac = ((TableAreaCandidate) pageTableAreaCandidates.get(t));
			System.out.println("   - " + tac.bounds + ":");
		}
		
		//	finally ...
		return ((TableAreaCandidate[]) pageTableAreaCandidates.toArray(new TableAreaCandidate[pageTableAreaCandidates.size()]));
	}
	
	private static class TableAreaCandidate {
		final BoundingBox bounds;
		final ImWord[] words;
		final TableAreaStripe[] rows;
		final ImRegion[] cols;
		final float colGapFract;
		final TableAreaStatistics stats;
		TableAreaCandidate(TableAreaStripe[] rows, ImRegion[] cols, TableAreaStatistics stats) {
			this.rows = rows;
			this.cols = cols;
			this.stats = stats;
			this.bounds = this.stats.wordBounds;
			this.words = this.stats.words;
			this.colGapFract = this.stats.getBrgColOccupationLowFractReduced();
		}
	}
	
	private static TableAreaCandidate getTableAreaCandidate(ImPage page, BoundingBox tableArea, float normSpaceWidth) {
		System.out.println("   - assessing " + tableArea);
		
		//	make sure to (a) wrap table area tightly around contained words, but (b) not to cut off any words
		ImWord[] areaWords;
		BoundingBox wTableArea;
		while (true) {
			areaWords = page.getWordsInside(tableArea);
			wTableArea = ImLayoutObject.getAggregateBox(areaWords);
			if (tableArea.equals(wTableArea))
				break;
			else {
				tableArea = wTableArea;
				System.out.println("   - reduced to " + tableArea);
			}
		}
		
		//	compute average word height
		int tableAreaWordHeightSum = 0;
		for (int w = 0; w < areaWords.length; w++)
			tableAreaWordHeightSum += areaWords[w].bounds.getHeight();
		int tableAreaWordHeight = ((areaWords.length == 0) ? 0 : ((tableAreaWordHeightSum + (areaWords.length / 2)) / areaWords.length));
		System.out.println("   - average word height is " + tableAreaWordHeight);
		
		//	compute table area statistics
//		TableAreaStatistics areaStats = new TableAreaStatistics(areaWords, normSpaceWidth);
		TableAreaStatistics areaStats = getTableAreaStatistics(page, areaWords, normSpaceWidth);
		
		//	we need a good few core lows wider than average word spacing (i.e., filtered) to have any chance of a good column split
		System.out.println("   - filtered bridged lows are " + areaStats.getBrgColOccupationLowsFiltered());
		float bColPixelLowFractFiltered = areaStats.getBrgColOccupationLowFractFiltered();
		if (bColPixelLowFractFiltered < 0.05) {
			System.out.println("   ==> cannot build table with less than 5% column gaps");
			return null;
		}
		
		//	we still need a good few core lows wider than average word spacing (i.e., filtered) to have any chance of a good column split
		System.out.println("   - reduced bridged lows are " + areaStats.getBrgColOccupationLowsReduced());
		float bColPixelLowFractReduced = areaStats.getBrgColOccupationLowFractReduced();
		if (bColPixelLowFractReduced < 0.05) {
			System.out.println("   ==> cannot build table with less than 5% core column gaps");
			return null;
		}
		
		/* TODO also call mis-match is maximum gap is more than, say, five times average gap
		 * ==> super large gap most likely due to short headers over long-valued column 
		 * ==> likely got largish heading section only, no good for table by itself */
		
		//	get row stripes and density
		TableAreaStripe[] areaStripes = getTableAreaStripes(page, areaStats);
		System.out.println("   - got " + areaStripes.length + " stripes:");
		for (int r = 0; r < areaStripes.length; r++)
			System.out.println("     - " + areaStripes[r].bounds + ": " + areaStripes[r].words[0].getString());
		if (areaStripes.length < 3) {
			System.out.println("   ==> cannot build table with less than three rows");
			return null;
		}
		
		//	check individual row stripes for table caption (non-table area detection only catches captions boxed in by themselves)
		for (int r = 0; r < areaStripes.length; r++) {
			String stripeStartWordString = areaStripes[r].words[0].getString().toLowerCase();
			if (!stripeStartWordString.equals("t") && !stripeStartWordString.startsWith("tab")) // need to catch word boundary due to font size change emulating small caps in some layouts
				continue;
			String stripeStartString = ImUtils.getString(areaStripes[r].words[0], areaStripes[r].words[Math.min(areaStripes[r].words.length, 5) - 1], true);
			if (stripeStartString.matches("[Tab|TAB][^\\s]*\\.?\\s*[1-9][0-9]*.*")) {
				tableArea = null;
				break;
			}
		}
		if (tableArea == null) {
			System.out.println("   ==> cannot span table across caption");
			return null;
		}
		
		//	collect words overlapping with lows to ignore them on column splitting
		HashSet multiColumnCellWords = new HashSet();
//		for (Iterator colit = bColPixelCountLowSetReduced.iterator(); colit.hasNext();) {
		for (Iterator colit = areaStats.getBrgColOccupationLowsReduced().iterator(); colit.hasNext();) {
			ColumnOccupationLow col = ((ColumnOccupationLow) colit.next());
			for (int w = 0; w < areaWords.length; w++) {
//				if (tableAreaWords[w].bounds.right <= (col.left + tableArea.left))
				if (areaWords[w].bounds.right <= col.left)
					continue;
//				if ((col.right + tableArea.left) <= tableAreaWords[w].bounds.left)
				if (col.right <= areaWords[w].bounds.left)
					continue;
				multiColumnCellWords.add(areaWords[w]);
			}
		}
		
		//	get speculative columns
		//	rationale: if we actually have a table, we should be able to distinguish at least _several_ columns
		ImRegion areaTestTable = new ImRegion(page.getDocument(), page.pageId, tableArea, ImRegion.TABLE_TYPE);
		ImRegion[] areaTestCols = ImUtils.createTableColumns(areaTestTable, multiColumnCellWords);
		if (areaTestCols == null)
			return null;
		if (areaTestCols.length < 2) {
			System.out.println("   ==> cannot build table with less two columns");
			return null;
		}
		System.out.println("   - potential cells are:");
		for (int c = 0; c < areaTestCols.length; c++) {
			System.out.println("     - in " + areaTestCols[c].bounds);
			for (int r = 0; r < areaStripes.length; r++) {
				BoundingBox tacBounds = new BoundingBox(areaTestCols[c].bounds.left, areaTestCols[c].bounds.right, areaStripes[r].bounds.top, areaStripes[r].bounds.bottom);
				ImWord[] tacWords = page.getWordsInside(tacBounds);
				String tacString = ImUtils.getString(tacWords, ImUtils.leftRightTopDownOrder, true);
				System.out.println("       - " + tacString);
			}
		}
		
		//	TODO mark region as dead if (a) only two lines contained and (b) column split fails
		
		//	compute average column spacing ...
		//	TODO maybe also compute mean (by means of CountingSet and elimination of extremes)
		int columnGapSum = 0;
		for (int c = 1; c < areaTestCols.length; c++)
			columnGapSum += (areaTestCols[c].bounds.left - areaTestCols[c-1].bounds.right);
		int avgColumnGap = (columnGapSum / (areaTestCols.length - 1));
		System.out.println("   - average column gap is " + avgColumnGap);
		
		//	TODO ... and compare in-cell word spacing to that
		for (int c = 0; c < areaTestCols.length; c++) {
			System.out.println("   - checking words distances in " + areaTestCols[c].bounds);
			for (int r = 0; r < areaStripes.length; r++) {
				BoundingBox tacBounds = new BoundingBox(areaTestCols[c].bounds.left, areaTestCols[c].bounds.right, areaStripes[r].bounds.top, areaStripes[r].bounds.bottom);
				ImWord[] tacWords = page.getWordsInside(tacBounds);
				ImUtils.sortLeftRightTopDown(tacWords);
				for (int w = 1; w < tacWords.length; w++) {
					if (tacWords[w-1].bounds.bottom < tacWords[w].centerY)
						continue; // no use comparing across line break
					int tacWordDist = (tacWords[w].bounds.left - tacWords[w-1].bounds.right);
					if ((avgColumnGap * 3) < (tacWordDist * 4)) {
						System.out.println("     - column gap grade word distance of " + tacWordDist + " in " + ImUtils.getString(tacWords, ImUtils.leftRightTopDownOrder, true) + " between '" + tacWords[w-1].getString() + "' and '" + tacWords[w].getString() + "'");
						//	TODO make this count
					}
				}
			}
		}
		
		//	finally ...
		return new TableAreaCandidate(areaStripes, areaTestCols, areaStats);
	}
	
	private static TableAreaStatistics getTableAreaStatistics(ImPage page, ImWord[] words, float normSpaceWidth) {
		
		//	compute initial statistics
		TableAreaStatistics tas = new TableAreaStatistics(page, words, normSpaceWidth);
		
		//	progressively increase normalized space width if column gap distribution all too heterogeneous
		while (true) {
			
			//	compute average, minimum, and maximum column margin, as well as distribution
			TreeSet colOccupationLows = tas.getBrgColOccupationLowsReduced();
			System.out.println("Column occupation lows are " + colOccupationLows);
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
			int avgColMargin = (colOccupationLows.isEmpty() ? 0 : ((colMarginSum + (colOccupationLows.size() / 2)) / colOccupationLows.size()));
			System.out.println(" - average column occupation low width is " + avgColMargin + " (" + minColMargin + "/" + maxColMargin + "):");
			for (Iterator cmit = colOccupationLowWidths.iterator(); cmit.hasNext();) {
				Integer colMargin = ((Integer) cmit.next());
				System.out.println("   - " + colMargin + ": " + colOccupationLowWidths.getCount(colMargin));
			}
			
			//	get row gaps
			TreeSet rowOccupationGaps = tas.getRowOccupationGaps();
			System.out.println("Row occupation gaps are " + rowOccupationGaps);
			int rowMarginSum = 0;
			int minRowMargin = Integer.MAX_VALUE;
			int maxRowMargin = 0;
			CountingSet rowOccupationGapWidths = new CountingSet(new TreeMap());
			for (Iterator rogit = rowOccupationGaps.iterator(); rogit.hasNext();) {
				RowOccupationGap rog = ((RowOccupationGap) rogit.next());
				rowMarginSum += rog.getHeight();
				minRowMargin = Math.min(minRowMargin, rog.getHeight());
				maxRowMargin = Math.max(maxRowMargin, rog.getHeight());
				rowOccupationGapWidths.add(new Integer(rog.getHeight()));
			}
			int avgRowMargin = ((rowMarginSum + (rowOccupationGaps.size() / 2)) / rowOccupationGaps.size());
			System.out.println(" - average row occupation gap heigth is " + avgRowMargin + " (" + minRowMargin + "/" + maxRowMargin + "):");
			for (Iterator rmit = rowOccupationGapWidths.iterator(); rmit.hasNext();) {
				Integer rowMargin = ((Integer) rmit.next());
				System.out.println("   - " + rowMargin + ": " + rowOccupationGapWidths.getCount(rowMargin));
			}
			System.out.println(" ==> minimum row margin is " + minRowMargin);
			
			//	minimum column margin has reached average word height, that's but enough in any case
			//	TODO figure out if we really need this cutoff, uninhibited iteration seems to converge just fine
			if (tas.avgWordHeight < minColMargin)
				break;
			
			//	minimum all too far below average, cut to half average (assuming actual column gaps are somewhat regular at least towards lower end)
			if ((minColMargin * 2) < avgColMargin) {
				float rNormSpaceWidth = (((float) avgColMargin) / (tas.avgWordHeight * 2));
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
	}
	
	private static class TableAreaStripe {
		final ImWord[] words;
		final BoundingBox bounds;
		TableAreaStripe(ImWord[] words) {
			Arrays.sort(words, ImUtils.leftRightOrder);
			this.words = words;
			this.bounds = ImLayoutObject.getAggregateBox(this.words);
		}
	}
	
	private static TableAreaStripe[] getTableAreaStripes(ImPage page, TableAreaStatistics stats) {
		ArrayList stripeList = new ArrayList();
		int lastNonEmptyRow = -1;
		for (int r = 0; r < stats.rowOccupations.length; r++) {
			if (stats.rowOccupations[r] == 0) {
				if (lastNonEmptyRow != -1) {
					BoundingBox stipeBounds = new BoundingBox(stats.wordBounds.left, stats.wordBounds.right, (stats.wordBounds.top + lastNonEmptyRow), (stats.wordBounds.top + r));
					ImWord[] stripeWords = page.getWordsInside(stipeBounds);
					if (stripeWords.length != 0)
						stripeList.add(new TableAreaStripe(stripeWords));
				}
				lastNonEmptyRow = -1;
			}
			else if (lastNonEmptyRow == -1)
				lastNonEmptyRow = r;
		}
		if (lastNonEmptyRow != -1) {
			BoundingBox stipeBounds = new BoundingBox(stats.wordBounds.left, stats.wordBounds.right, (stats.wordBounds.top + lastNonEmptyRow), (stats.wordBounds.top + stats.rowOccupations.length));
			ImWord[] stripeWords = page.getWordsInside(stipeBounds);
			if (stripeWords.length != 0)
				stripeList.add(new TableAreaStripe(stripeWords));
		}
		return ((TableAreaStripe[]) stripeList.toArray(new TableAreaStripe[stripeList.size()]));
	}
	
	
	
	private static class RowStripeScore {
		final float[] areaWidthColOccupationScores;
		final float[] stripeWidthColOccupationScores;
		final float[] bAreaWidthColOccupationScores;
		final float[] bStripeWidthColOccupationScores;
		final float areaWidthColOccupationScoreAvg;
		final float stripeWidthColOccupationScoreAvg;
		final float bAreaWidthColOccupationScoreAvg;
		final float bStripeWidthColOccupationScoreAvg;
		RowStripeScore(BoundingBox tableArea, RowStripe[] rowStripes, boolean[] skipStripes, float docNormWordSpacing, boolean verbose) {
			HashSet areaPixelRows = new HashSet();
			int[] areaColPixelRowCounts = new int[tableArea.getWidth()];
			Arrays.fill(areaColPixelRowCounts, 0);
			int[] bAreaColPixelRowCounts = new int[tableArea.getWidth()];
			Arrays.fill(bAreaColPixelRowCounts, 0);
			for (int r = 0; r < rowStripes.length; r++) {
				if ((skipStripes != null) && skipStripes[r])
					continue;
				for (int y = rowStripes[r].bounds.top; y < rowStripes[r].bounds.bottom; y++)
					areaPixelRows.add(new Integer(y));
				for (int w = 0; w < rowStripes[r].words.length; w++) {
					for (int x = rowStripes[r].words[w].bounds.left; x < rowStripes[r].words[w].bounds.right; x++) {
						areaColPixelRowCounts[x - tableArea.left] += rowStripes[r].words[w].bounds.getHeight();
						bAreaColPixelRowCounts[x - tableArea.left] += rowStripes[r].words[w].bounds.getHeight();
					}
					if (((w+1) < rowStripes[r].words.length) && ((rowStripes[r].words[w+1].bounds.left - rowStripes[r].words[w].bounds.right) < (rowStripes[r].words[w].bounds.getHeight() * docNormWordSpacing))) {
						for (int x = rowStripes[r].words[w].bounds.right; x < rowStripes[r].words[w+1].bounds.left; x++)
							bAreaColPixelRowCounts[x - tableArea.left] += rowStripes[r].words[w].bounds.getHeight();
					}
				}
			}
			if (verbose) System.out.println("     - got " + areaPixelRows.size() + " occupied pixel rows in area of height " + tableArea.getHeight());
			if (verbose) System.out.println("     - pixel column occupation distribution is " + Arrays.toString(areaColPixelRowCounts));
			if (verbose) System.out.println("     - bridged pixel column occupation distribution is " + Arrays.toString(bAreaColPixelRowCounts));
			this.areaWidthColOccupationScores = new float[rowStripes.length];
			float areaWidthColOccupationScoreSum = 0;
			this.stripeWidthColOccupationScores = new float[rowStripes.length];
			float stripeWidthColOccupationScoreSum = 0;
			this.bAreaWidthColOccupationScores = new float[rowStripes.length];
			float bAreaWidthColOccupationScoreSum = 0;
			this.bStripeWidthColOccupationScores = new float[rowStripes.length];
			float bStripeWidthColOccupationScoreSum = 0;
			//	TODO normalize these guys by density as well ???
			if (verbose) System.out.println("     - assessing " + rowStripes.length + " row stripes:");
			int colOccupationScoreCount = 0;
			for (int r = 0; r < rowStripes.length; r++) {
				if ((skipStripes != null) && skipStripes[r])
					continue;
				if (verbose) System.out.println("       - " + rowStripes[r].bounds + ": " + rowStripes[r].words[0].getString());
				if (verbose) System.out.println("         density is " + rowStripes[r].getWordDensity());
				boolean[] colHasPixel = new boolean[tableArea.getWidth()];
				Arrays.fill(colHasPixel, false);
				boolean[] bColHasPixel = new boolean[tableArea.getWidth()];
				Arrays.fill(bColHasPixel, false);
				for (int w = 0; w < rowStripes[r].words.length; w++) {
					for (int x = rowStripes[r].words[w].bounds.left; x < rowStripes[r].words[w].bounds.right; x++) {
						colHasPixel[x - tableArea.left] = true;
						bColHasPixel[x - tableArea.left] = true;
					}
					if (((w+1) < rowStripes[r].words.length) && ((rowStripes[r].words[w+1].bounds.left - rowStripes[r].words[w].bounds.right) < (rowStripes[r].words[w].bounds.getHeight() * docNormWordSpacing))) {
						for (int x = rowStripes[r].words[w].bounds.right; x < rowStripes[r].words[w+1].bounds.left; x++)
							bColHasPixel[x - tableArea.left] = true;
					}
				}
				int areaWidthColOccupationScoreSumRs = 0;
				int stripeWidthColOccupationScoreSumRs = 0;
				int bAreaWidthColOccupationScoreSumRs = 0;
				int bStripeWidthColOccupationScoreSumRs = 0;
				for (int x = 0; x < colHasPixel.length; x++) {
					if (colHasPixel[x])
						areaWidthColOccupationScoreSumRs += areaColPixelRowCounts[x];
					else areaWidthColOccupationScoreSumRs += (areaPixelRows.size() - areaColPixelRowCounts[x]);
					if (bColHasPixel[x])
						bAreaWidthColOccupationScoreSumRs += bAreaColPixelRowCounts[x];
					else bAreaWidthColOccupationScoreSumRs += (areaPixelRows.size() - bAreaColPixelRowCounts[x]);
					if ((x >= (rowStripes[r].bounds.left - tableArea.left)) && (x < (rowStripes[r].bounds.right - tableArea.left))) {
						if (colHasPixel[x])
							stripeWidthColOccupationScoreSumRs += areaColPixelRowCounts[x];
						else stripeWidthColOccupationScoreSumRs += (areaPixelRows.size() - areaColPixelRowCounts[x]);
						if (bColHasPixel[x])
							bStripeWidthColOccupationScoreSumRs += bAreaColPixelRowCounts[x];
						else bStripeWidthColOccupationScoreSumRs += (areaPixelRows.size() - bAreaColPixelRowCounts[x]);
					}
				}
				this.areaWidthColOccupationScores[r] = (((float) areaWidthColOccupationScoreSumRs) / (tableArea.getWidth() * areaPixelRows.size()));
				areaWidthColOccupationScoreSum += this.areaWidthColOccupationScores[r];
				this.stripeWidthColOccupationScores[r] = (((float) stripeWidthColOccupationScoreSumRs) / (rowStripes[r].bounds.getWidth() * areaPixelRows.size()));
				stripeWidthColOccupationScoreSum += this.stripeWidthColOccupationScores[r];
				if (verbose) System.out.println("         column occupation score is " + this.areaWidthColOccupationScores[r] + " whole width, " + this.stripeWidthColOccupationScores[r] + " stripe width");
				this.bAreaWidthColOccupationScores[r] = (((float) bAreaWidthColOccupationScoreSumRs) / (tableArea.getWidth() * areaPixelRows.size()));
				bAreaWidthColOccupationScoreSum += this.bAreaWidthColOccupationScores[r];
				this.bStripeWidthColOccupationScores[r] = (((float) bStripeWidthColOccupationScoreSumRs) / (rowStripes[r].bounds.getWidth() * areaPixelRows.size()));
				bStripeWidthColOccupationScoreSum += this.bStripeWidthColOccupationScores[r];
				if (verbose) System.out.println("         bridged column occupation score is " + this.bAreaWidthColOccupationScores[r] + " whole width, " + this.bStripeWidthColOccupationScores[r] + " stripe width");
				//	TODO normalize these guys by density as well ???
				colOccupationScoreCount++;
			}
			this.areaWidthColOccupationScoreAvg = (areaWidthColOccupationScoreSum / colOccupationScoreCount);
			this.stripeWidthColOccupationScoreAvg = (stripeWidthColOccupationScoreSum / colOccupationScoreCount);
			if (verbose) System.out.println("     - avg column occupation score is " + this.areaWidthColOccupationScoreAvg + " whole width, " + this.stripeWidthColOccupationScoreAvg + " stripe width");
			this.bAreaWidthColOccupationScoreAvg = (bAreaWidthColOccupationScoreSum / colOccupationScoreCount);
			this.bStripeWidthColOccupationScoreAvg = (bStripeWidthColOccupationScoreSum / colOccupationScoreCount);
			if (verbose) System.out.println("     - avg bridged column occupation score is " + this.bAreaWidthColOccupationScoreAvg + " whole width, " + this.bStripeWidthColOccupationScoreAvg + " stripe width");
		}
	}
	
	private static class RowStripeSegmentation {
		final RowStripeSegment[] segments;
		final int minSeg;
		final int maxSeg;
		final int avgSeg;
		final RowStripeGap[] gaps;
		final int minGap;
		final int maxGap;
		final int avgGap;
		RowStripeSegmentation(RowStripeSegment[] segments, RowStripeGap[] gaps) {
			this.segments = segments;
			int minSeg = Integer.MAX_VALUE;
			int maxSeg = 0;
			int segSum = 0;
			
			for (int s = 0; s < this.segments.length; s++) {
				int seg = (this.segments[s].end - this.segments[s].start);
				minSeg = Math.min(seg, minSeg);
				maxSeg = Math.max(seg, maxSeg);
				segSum += seg;
			}
			if (this.segments.length == 0) {
				this.minSeg = -1;
				this.maxSeg = -1;
				this.avgSeg = -1;
			}
			else {
				this.minSeg = minSeg;
				this.maxSeg = maxSeg;
				this.avgSeg = ((segSum + (this.segments.length / 2)) / this.segments.length);
			}
			
			this.gaps = gaps;
			int minGap = Integer.MAX_VALUE;
			int maxGap = 0;
			int gapSum = 0;
			for (int g = 0; g < this.gaps.length; g++) {
				int gap = (this.gaps[g].end - this.gaps[g].start);
				minGap = Math.min(gap, minGap);
				maxGap = Math.max(gap, maxGap);
				gapSum += gap;
			}
			if (this.gaps.length == 0) {
				this.minGap = -1;
				this.maxGap = -1;
				this.avgGap = -1;
			}
			else {
				this.minGap = minGap;
				this.maxGap = maxGap;
				this.avgGap = ((gapSum + (this.gaps.length / 2)) / this.gaps.length);
			}
		}
		//	TODO add any evaluation method we might need ...
		boolean obstructsColumnGap() {
			for (int s = 0; s < this.segments.length; s++) {
				if (this.segments[s].obstructsSegmentGap())
					return true;
			}
			return false;
		}
		int getObstructedColumnGaps() {
			int ocgs = 0;
			for (int s = 0; s < this.segments.length; s++) {
				int osgs = this.segments[s].getObstructedSegmentGaps();
				if (osgs > 0)
					ocgs += ((osgs + this.segments[s].overlappingRowCount - 1) / this.segments[s].overlappingRowCount);
			}
			return ocgs;
		}
	}
	
	private static class RowStripeSegment {
		final int start;
		final int end;
		final int color;
		final int overlappingRowCount;
		final int overlappingSegmentCount;
		RowStripeSegment(int start, int end, int color, int overlappingRowCount, int overlappingSegmentCount) {
			this.start = start;
			this.end = end;
			this.color = color;
			this.overlappingRowCount = overlappingRowCount;
			this.overlappingSegmentCount = overlappingSegmentCount;
		}
		boolean obstructsSegmentGap() {
			return (this.overlappingSegmentCount > this.overlappingRowCount);
		}
		int getObstructedSegmentGaps() {
			return Math.max((this.overlappingSegmentCount - this.overlappingRowCount), 0);
		}
	}
	
	private static class RowStripeGap {
		final int start;
		final int end;
		final int overlappingRowCount;
		RowStripeGap(int start, int end, int overlappingRowCount) {
			this.start = start;
			this.end = end;
			this.overlappingRowCount = overlappingRowCount;
		}
	}
	
	private static RowStripeSegmentation getRowStripeSegmentation(int[] rowSegmentColors, int rowIndex, int[][] areaRowSegmentColors, boolean[] skipRows) {
		ArrayList segs = new ArrayList();
		HashSet segOverlappingRows = new HashSet();
		HashSet segOverlappingSegs = new HashSet();
		int segStart = -1;
		int segColor = -1;
		
		ArrayList gaps = new ArrayList();
		HashSet gapOverlappingRows = new HashSet();
		int gapStart = -1;
		
		for (int x = 0; x < rowSegmentColors.length; x++) {
			if (rowSegmentColors[x] == 0) {
				if (segStart != -1) {
					segs.add(new RowStripeSegment(segStart, x, segColor, segOverlappingRows.size(), segOverlappingSegs.size()));
					segStart = -1;
				}
				if ((x != 0) && (segs.size() != 0) && (rowSegmentColors[x] != rowSegmentColors[x-1])) {
					gapOverlappingRows.clear();
					gapStart = x;
				}
				for (int r = 0; r < areaRowSegmentColors.length; r++) {
					if (r == rowIndex)
						continue;
					if ((skipRows != null) && skipRows[r])
						continue;
					if (areaRowSegmentColors[r][x] == 0)
						gapOverlappingRows.add(new Integer(r));
				}
			}
			
			else {
				if (gapStart != -1) {
					gaps.add(new RowStripeGap(gapStart, x, gapOverlappingRows.size()));
					gapStart = -1;
				}
				if ((x == 0) || (rowSegmentColors[x] != rowSegmentColors[x-1])) {
					segOverlappingRows.clear();
					segOverlappingSegs.clear();
					segStart = x;
					segColor = rowSegmentColors[x];
				}
				for (int r = 0; r < areaRowSegmentColors.length; r++) {
					if (r == rowIndex)
						continue;
					if ((skipRows != null) && skipRows[r])
						continue;
					if (areaRowSegmentColors[r][x] != 0) {
						segOverlappingRows.add(new Integer(r));
//						overlappingSegs.add(r + "-" + areaRowSegmentColors[r][x]);
						segOverlappingSegs.add(new Integer((r << 16) + areaRowSegmentColors[r][x]));
						//	TODOne use Integer instead of String, with row number shifted left by 16 bits (way faster ...)
					}
				}
			}
		}
		if (segStart != -1) {
			segs.add(new RowStripeSegment(segStart, rowSegmentColors.length, segColor, segOverlappingRows.size(), segOverlappingSegs.size()));
			segStart = -1;
		}
		return new RowStripeSegmentation(((RowStripeSegment[]) segs.toArray(new RowStripeSegment[segs.size()])), ((RowStripeGap[]) gaps.toArray(new RowStripeGap[gaps.size()])));
	}
	
	private static boolean checkColumns(ImPage page, ImRegion[] columns, RowStripe[] rowStripes) {
		
		//	check columns
		if (columns == null) {
			System.out.println("     ==> could not identify columns");
			return false;
		}
		
		//	check column count
		System.out.println("     - got " + columns.length + " columns");
		if (columns.length < 3) {
			System.out.println("     ==> too few columns");
			return false;
		}
		
		//	assess column gaps, and show table column header starts
		int minColGap = Integer.MAX_VALUE;
		int maxColGap = 0;
		int colGapCount = 0;
		int colGapSum = 0;
		for (int c = 0; c < columns.length; c++) {
			ImWord[] colWords = page.getWordsInside(columns[c].bounds);
			ImUtils.sortLeftRightTopDown(colWords);
			System.out.println("       - " + columns[c].bounds + ": " + ((colWords.length == 0) ? "<empty>" : colWords[0].getString()));
			if (c == 0)
				continue;
			int colGap = (columns[c].bounds.left - columns[c-1].bounds.right);
			minColGap = Math.min(minColGap, colGap);
			maxColGap = Math.max(maxColGap, colGap);
			colGapCount++;
			colGapSum += colGap;
		}
		int minColumnGap = minColGap;
		int maxColumnGap = maxColGap;
		int avgColumnGap = ((colGapCount == 0) ? 0 : ((colGapSum + (colGapCount / 2)) / colGapCount));
		System.out.println("       - column gaps are " + avgColumnGap + " on average (min " + minColumnGap + ", max " + maxColumnGap + ")");
		
		//	check average column gap (should be 2 mm at least below 6 columns or 4 rows, and at least 1 mm above both)
		if (avgColumnGap < (((columns.length < 6) || (rowStripes.length < 4)) ? (page.getImageDPI() / 13) : (page.getImageDPI() / 25))) {
			System.out.println("     ==> columns gaps too narrow on average, below " + (page.getImageDPI() / 13) + "/"  + (page.getImageDPI() / 25));
			return false;
		}
		
		//	this looks OK
		return true;
	}
	
	private static int[] getRowStripeSegmentColors(BoundingBox tableArea, RowStripe rowStripe, int minSegmentGap) {
		int[] rowStripeSegmentColors = new int[tableArea.getWidth()];
		Arrays.fill(rowStripeSegmentColors, 0);
		int segColor = -1;
		for (int w = 0; w < rowStripe.words.length; w++) {
			if (segColor == -1)
				segColor = rowStripe.words[w].bounds.left;
			for (int x = rowStripe.words[w].bounds.left; x < rowStripe.words[w].bounds.right; x++)
				rowStripeSegmentColors[x - tableArea.left] = segColor;
			if (((w+1) < rowStripe.words.length) && ((rowStripe.words[w+1].bounds.left - rowStripe.words[w].bounds.right) < minSegmentGap)) {
				for (int x = rowStripe.words[w].bounds.right; x < rowStripe.words[w+1].bounds.left; x++)
					rowStripeSegmentColors[x - tableArea.left] = segColor;
			}
			else segColor = -1;
		}
		return rowStripeSegmentColors;
	}
	
	private static int countSegments(int[] segmentColors) {
		int segCount = 0;
		for (int x = 0; x < segmentColors.length; x++) {
			if (segmentColors[x] == 0)
				continue;
			if ((x == 0) || (segmentColors[x] != segmentColors[x-1]))
				segCount++;
		}
		return segCount;
	}
	
	private static class GraphTableCandiate {
		final BoundingBox bounds;
		final RowStripe[] rowStripes;
		final ImRegion[] columns;
		final int minColumnGap;
		final int maxColumnGap;
		final int avgColumnGap;
		GraphTableCandiate(BoundingBox bounds, RowStripe[] rowStripes, ImRegion[] columns) {
			this.bounds = bounds;
			this.rowStripes = rowStripes;
			this.columns = columns;
			int minColGap = Integer.MAX_VALUE;
			int maxColGap = 0;
			int colGapCount = 0;
			int colGapSum = 0;
			for (int c = 1; c < this.columns.length; c++) {
				int colGap = (this.columns[c].bounds.left - this.columns[c-1].bounds.right);
				minColGap = Math.min(minColGap, colGap);
				maxColGap = Math.max(maxColGap, colGap);
				colGapCount++;
				colGapSum += colGap;
			}
			this.minColumnGap = minColGap;
			this.maxColumnGap = maxColGap;
			this.avgColumnGap = ((colGapCount == 0) ? 0 : ((colGapSum + (colGapCount / 2)) / colGapCount));
		}
		float getColumnGapOddity() {
			return (((float) this.maxColumnGap) / this.minColumnGap);
		}
	}
	
	/*
When seeking tables split up into multiple blocks:
- compare column gaps and anchors between adjacent blocks ...
- ... as well as general word / whitespace compatibility
==> build analyzeArea(ImPage, BoundingBox) method (or the like named, maybe isLikelyTable):
- analyze row stripe word density, ...
- ... whitespace distribution, ...
- ... potential column splits and gaps (gap width in particular), ...
- ... etc.
	 */
	private static class ColumnCompatibility {
		final float sharedWordCols;
		final float sharedNonWordCols;
		ColumnCompatibility(float sharedWordCols, float sharedNonWordCols) {
			this.sharedWordCols = sharedWordCols;
			this.sharedNonWordCols = sharedNonWordCols;
		}
		public String toString() {
			return ((this.sharedWordCols + this.sharedNonWordCols) + " (" + this.sharedWordCols + " words, " + this.sharedNonWordCols + " space)");
		}
	}
	
	private static ColumnCompatibility getColumnCompatibility(int left1, boolean[] colHasWord1, int left2, boolean[] colHasWord2, boolean expand) {
		int minCol = (expand ? Math.min(left1, left2) : Math.max(left1, left2));
		int maxCol = (expand ? Math.max((left1 + colHasWord1.length), (left2 + colHasWord2.length)) : Math.min((left1 + colHasWord1.length), (left2 + colHasWord2.length)));
		if (maxCol <= minCol)
			return new ColumnCompatibility(0, 0);
		int sWordCols = 0;
		int sNonWordCols = 0;
		for (int c = minCol; c < maxCol; c++) {
			boolean isWordCol1 = (((c < left1) || (c >= (left1 + colHasWord1.length))) ? false : colHasWord1[c - left1]);
			boolean isWordCol2 = (((c < left2) || (c >= (left2 + colHasWord2.length))) ? false : colHasWord2[c - left2]);
			if (isWordCol1 && isWordCol2)
				sWordCols++;
			else if (!isWordCol1 && !isWordCol2)
				sNonWordCols++;
		}
		return new ColumnCompatibility((((float) sWordCols) / (maxCol - minCol)), (((float) sNonWordCols) / (maxCol - minCol)));
	}
	
	private static final Comparator topDownGraphicsOrder = new Comparator() {
		public int compare(Object obj1, Object obj2) {
			BoundingBox bb1 = ((Graphics) obj1).getBounds();
			BoundingBox bb2 = ((Graphics) obj2).getBounds();
			if (bb1.top == bb2.top)
				return (bb1.left - bb2.left);
			else return (bb1.top - bb2.top);
		}
	};
	
	private static final Comparator descendingBoxSizeOrder = new Comparator() {
		public int compare(Object obj1, Object obj2) {
			BoundingBox bb1 = ((BoundingBox) obj1);
			BoundingBox bb2 = ((BoundingBox) obj2);
			return ((bb2.getArea() - bb1.getArea()));
		}
	};
}
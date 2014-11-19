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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;

/**
 * This plugin provides a detector for the structure of a document. The detector
 * identified page headers, page numbers, captions, and footnotes. It can also
 * be used as a component outside of GoldenGATE Imagine.
 * 
 * @author sautter
 */
public class DocumentStructureDetectorProvider extends AbstractImageMarkupToolProvider implements ImagingConstants {
	
	private static final String STRUCTURE_DETECTOR_IMT_NAME = "StructureDetector";
	
	private String pageNumberPattern = "[1-9][0-9]*";
	private String ocrPageNumberPartPattern = "[0-9]+";
	private HashMap ocrPageNumberCharacterTranslations = new HashMap();
	
	private Pattern[] captionStartPatterns = new Pattern[0];
	
	private Pattern[] footnoteStartPatterns = new Pattern[0];
	
	private ImageMarkupTool structureDetector = new StructureDetector();
	
	/** public zero-argument constructor for class loading */
	public DocumentStructureDetectorProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Document Structure Detector";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		
		//	load character mappings
		try {
			InputStream is = this.dataProvider.getInputStream("pageNumberCharacters.txt");
			StringVector characterLines = StringVector.loadList(is);
			is.close();
			
			TreeSet pageNumberCharacters = new TreeSet();
			for (int l = 0; l < characterLines.size(); l++) {
				String characterLine = characterLines.get(l).trim();
				if ((characterLine.length() == 0) || characterLine.startsWith("//") || (characterLine.indexOf(' ') == -1))
					continue;
				
				String digit = characterLine.substring(0, characterLine.indexOf(' ')).trim();
				if (!Gamta.isNumber(digit))
					continue;
				
				pageNumberCharacters.add(digit);
				ArrayList characterTranslations = ((ArrayList) this.ocrPageNumberCharacterTranslations.get(digit));
				if (characterTranslations == null) {
					characterTranslations = new ArrayList(2);
					this.ocrPageNumberCharacterTranslations.put(digit, characterTranslations);
				}
				characterTranslations.add(digit);
				
				String characters = characterLine.substring(characterLine.indexOf(' ')).trim();
				for (int c = 0; c < characters.length(); c++) {
					String character = characters.substring(c, (c+1));
					pageNumberCharacters.add(character);
					characterTranslations = ((ArrayList) this.ocrPageNumberCharacterTranslations.get(character));
					if (characterTranslations == null) {
						characterTranslations = new ArrayList(2);
						this.ocrPageNumberCharacterTranslations.put(character, characterTranslations);
					}
					characterTranslations.add(digit);
				}
			}
			
			for (int d = 0; d < Gamta.DIGITS.length(); d++)
				pageNumberCharacters.add(Gamta.DIGITS.substring(d, (d+1)));
			
			StringBuffer ocrPageNumberCharaterPatternBuilder = new StringBuffer();
			for (Iterator cit = pageNumberCharacters.iterator(); cit.hasNext();) {
				String ocrPageNumberCharacter = ((String) cit.next());
				ocrPageNumberCharaterPatternBuilder.append(ocrPageNumberCharacter);
				ArrayList characterTranslations = ((ArrayList) this.ocrPageNumberCharacterTranslations.get(ocrPageNumberCharacter));
				if (characterTranslations == null)
					continue;
				int[] cts = new int[characterTranslations.size()];
				for (int t = 0; t < characterTranslations.size(); t++)
					cts[t] = Integer.parseInt((String) characterTranslations.get(t));
				this.ocrPageNumberCharacterTranslations.put(ocrPageNumberCharacter, cts);
			}
			
			String ocrPageNumberCharacterPattern = ("[" + RegExUtils.escapeForRegEx(ocrPageNumberCharaterPatternBuilder.toString()) + "]");
			this.ocrPageNumberPartPattern = (ocrPageNumberCharacterPattern + "+(\\s?" + ocrPageNumberCharacterPattern + "+)*");
		} catch (IOException ioe) {}
		
		//	load caption indicators
		try {
			InputStream is = this.dataProvider.getInputStream("captionStartPatterns.txt");
			StringVector captionStartPatternStrings = StringVector.loadList(is);
			is.close();
			
			ArrayList captionStartPatterns = new ArrayList();
			for (int l = 0; l < captionStartPatternStrings.size(); l++) {
				String captionStartPattern = captionStartPatternStrings.get(l).trim();
				if ((captionStartPattern.length() == 0) || captionStartPattern.startsWith("//"))
					continue;
				try {
					captionStartPatterns.add(Pattern.compile(captionStartPattern, Pattern.CASE_INSENSITIVE));
				} catch (PatternSyntaxException pse) {}
			}
			
			this.captionStartPatterns = ((Pattern[]) captionStartPatterns.toArray(new Pattern[captionStartPatterns.size()]));
		} catch (IOException ioe) {}
		
		//	load footnote indicators
		try {
			InputStream is = this.dataProvider.getInputStream("footnoteStartPatterns.txt");
			StringVector footnoteStartPatternStrings = StringVector.loadList(is);
			is.close();
			
			ArrayList footnoteStartPatterns = new ArrayList();
			for (int l = 0; l < footnoteStartPatternStrings.size(); l++) {
				String footnoteStartPattern = footnoteStartPatternStrings.get(l).trim();
				if ((footnoteStartPattern.length() == 0) || footnoteStartPattern.startsWith("//"))
					continue;
				try {
					footnoteStartPatterns.add(Pattern.compile(footnoteStartPattern, Pattern.CASE_INSENSITIVE));
				} catch (PatternSyntaxException pse) {}
			}
			
			this.footnoteStartPatterns = ((Pattern[]) footnoteStartPatterns.toArray(new Pattern[footnoteStartPatterns.size()]));
		} catch (IOException ioe) {}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider#getToolsMenuItemNames()
	 */
	public String[] getToolsMenuItemNames() {
		String[] tmins = {STRUCTURE_DETECTOR_IMT_NAME};
		return tmins;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		if (STRUCTURE_DETECTOR_IMT_NAME.equals(name))
			return this.structureDetector;
		else return null;
	}
//	
//	//	!!! TEST ONLY !!!
//	public static void main(String[] args) throws Exception {
////		InputStream docIn = new BufferedInputStream(new FileInputStream(new File("E:/Testdaten/PdfExtract/zt00872.pdf.imf")));
//		InputStream docIn = new BufferedInputStream(new FileInputStream(new File("E:/Testdaten/PdfExtract/3868.pdf.imf")));
//		ImDocument doc = ImfIO.loadDocument(docIn);
//		docIn.close();
//		
//		DocumentStructureDetectorProvider dsdp = new DocumentStructureDetectorProvider();
//		dsdp.setDataProvider(new PluginDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/DocumentStructureDetectorData")));
//		dsdp.init();
//		dsdp.detectDocumentStructure(doc, false, ProgressMonitor.dummy);
//	}
	
	private class StructureDetector implements ImageMarkupTool {
		public String getLabel() {
			return "Detect Document Structure";
		}
		public String getTooltip() {
			return "Detect page headers, page numbers, captions, and footnotes";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			if (annot == null)
				detectDocumentStructure(doc, idmp.documentBornDigital, pm);
			else pm.setStep("Cannot detect document structure on single annotation");
		}
	}
	
	/**
	 * Detect and mark the structure of a document. In particular, this method
	 * identifies page numbers, page headers, captions, and footnotes. If the
	 * <code>documentBornDigital</code> argument is set to false, page number
	 * detection considers a wide range of characters to potentially be page
	 * number digits that have been mis-recognized by OCR.
	 * @param doc the document whose structure to detect
	 * @param documentBornDigital is the document born digital or scanned?
	 * @param pm a progress monitor observing document processing
	 */
	public void detectDocumentStructure(ImDocument doc, boolean documentBornDigital, ProgressMonitor pm) {
		
		//	get pages
		ImPage[] pages = doc.getPages();
		pm.setStep("Detecting document structure from " + pages.length + " pages");
		
		//	compute average resolution
		int pageImageDpiSum = 0;
		for (int p = 0; p < pages.length; p++)
			pageImageDpiSum += pages[p].getPageImage().currentDpi;
		int pageImageDpi = ((pageImageDpiSum + (pages.length / 2)) / pages.length);
		pm.setInfo(" - resolution is " + pageImageDpi);
		
		//	collect blocks adjacent to page edge (top or bottom) from each page
		pm.setStep(" - gathering data");
		PageData[] pageData = new PageData[pages.length];
		for (int p = 0; p < pages.length; p++) {
			pageData[p] = new PageData(pages[p]);
			
			//	get regions
			ImRegion[] regions = pages[p].getRegions();
			pm.setInfo(" - got " + regions.length + " regions in page " + p);
			
			//	collect regions of interest
			for (int r = 0; r < regions.length; r++) {
				pm.setInfo(" - assessing region " + regions[r].getType() + "@" + regions[r].bounds.toString());
				
				//	lines are not of interest here, as we are out for standalone blocks
				if (LINE_ANNOTATION_TYPE.equals(regions[r].getType())) {
					pm.setInfo(" --> ignoring line");
					continue;
				}
				
				//	this one's too large (higher than an inch, wider than three inches) to be a page header (would just waste too much space)
				if (((regions[r].bounds.bottom - regions[r].bounds.top) > pageImageDpi) && ((regions[r].bounds.right - regions[r].bounds.left) > (pageImageDpi * 3))) {
					pm.setInfo(" --> too large");
					continue;
				}
				
				//	get words
				ImWord[] regionWords = regions[r].getWords();
				if (regionWords.length == 0)
					continue;
				Arrays.sort(regionWords, ImUtils.textStreamOrder);
				
				//	too many words for a page header
				if (regionWords.length > 30) {
					pm.setInfo(" --> too many words (" + regionWords.length + ")");
					continue;
				}
				
				//	this one has been worked on before (we still need page headers for page number detection, though)
				if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(regionWords[0].getTextStreamType()) && !ImWord.TEXT_STREAM_TYPE_PAGE_TITLE.equals(regionWords[0].getTextStreamType()))
					continue;
				
				//	check if any regions above or below
				boolean regionsAbove = false;
				boolean regionsBelow = false;
				for (int cr = 0; cr < regions.length; cr++) {
					if (cr == r)
						continue;
					if (LINE_ANNOTATION_TYPE.equals(regions[cr].getType()))
						continue;
					
					//	test if regions are really above/below one another
					if ((regions[r].bounds.right <= regions[cr].bounds.left) || (regions[cr].bounds.right <= regions[r].bounds.left))
						continue;
					
					//	test relative position
					if (regions[r].bounds.bottom <= regions[cr].bounds.top)
						regionsBelow = true;
					if (regions[cr].bounds.bottom <= regions[r].bounds.top)
						regionsAbove = true;
					
					//	do we need to check any further?
					if (regionsAbove && regionsBelow)
						break;
				}
				
				//	remember region of interest
				if (!regionsAbove && (regions[r].bounds.bottom < (pages[p].bounds.bottom / 2))) {
					pageData[p].topRegions.add(regions[r]);
					pm.setInfo(" --> found top region");
				}
				if (!regionsBelow && (regions[r].bounds.top > (pages[p].bounds.bottom / 2))) {
					pageData[p].bottomRegions.add(regions[r]);
					pm.setInfo(" --> found bottom region");
				}
			}
		}
		
		//	collect words from top and bottom regions, separate for even and odd pages
		CountingSet topWordsOdd = new CountingSet(String.CASE_INSENSITIVE_ORDER);
		CountingSet topWordsEven = new CountingSet(String.CASE_INSENSITIVE_ORDER);
		CountingSet bottomWordsOdd = new CountingSet(String.CASE_INSENSITIVE_ORDER);
		CountingSet bottomWordsEven = new CountingSet(String.CASE_INSENSITIVE_ORDER);
		CountingSet allWordsOdd = new CountingSet(String.CASE_INSENSITIVE_ORDER);
		CountingSet allWordsEven = new CountingSet(String.CASE_INSENSITIVE_ORDER);
		
		//	collect words that might be (parts of) page numbers
		String pageNumberPartPattern = (documentBornDigital ? pageNumberPattern : ocrPageNumberPartPattern);
		
		//	collect the words, and find possible page numbers
		for (int p = 0; p < pages.length; p++) {
			ArrayList pageNumberParts = new ArrayList();
			HashSet pageBorderWordIDs = new HashSet();
			
			//	words from page top candidates
			TreeSet topWords = new TreeSet(String.CASE_INSENSITIVE_ORDER);
			for (int r = 0; r < pageData[p].topRegions.size(); r++) {
				ImWord[] words = ((ImRegion) pageData[p].topRegions.get(r)).getWords();
				Arrays.sort(words, ImUtils.leftRightTopDownOrder);
				for (int w = 0; w < words.length; w++)
					if (pageBorderWordIDs.add(words[w].getLocalID())) {
						topWords.add(words[w].getString());
						if (words[w].getString().matches(pageNumberPartPattern))
							pageNumberParts.add(words[w]);
					}
			}
			(((p % 2) == 0) ? topWordsEven : topWordsOdd).addAll(topWords);
			
			//	words from page bottom candidates
			TreeSet bottomWords = new TreeSet(String.CASE_INSENSITIVE_ORDER);
			for (int r = 0; r < pageData[p].bottomRegions.size(); r++) {
				ImWord[] words = ((ImRegion) pageData[p].bottomRegions.get(r)).getWords();
				Arrays.sort(words, ImUtils.leftRightTopDownOrder);
				for (int w = 0; w < words.length; w++)
					if (pageBorderWordIDs.add(words[w].getLocalID())) {
						bottomWords.add(words[w].getString());
						if (words[w].getString().matches(pageNumberPartPattern))
							pageNumberParts.add(words[w]);
					}
			}
			(((p % 2) == 0) ? bottomWordsEven : bottomWordsOdd).addAll(bottomWords);
			
			//	overall words for comparison
			TreeSet allWords = new TreeSet(String.CASE_INSENSITIVE_ORDER);
			ImWord[] words = pages[p].getWords();
			for (int w = 0; w < words.length; w++)
				allWords.add(words[w].getString());
			(((p % 2) == 0) ? allWordsEven : allWordsOdd).addAll(allWords);
			
			//	build candidate page numbers from collected parts
			Collections.sort(pageNumberParts, ImUtils.leftRightTopDownOrder);
			if (documentBornDigital) {
				pm.setInfo(" - got " + pageNumberParts.size() + " possible page numbers for page " + p + ":");
				for (int w = 0; w < pageNumberParts.size(); w++) {
					pageData[p].pageNumberCandidates.add(new PageNumber((ImWord) pageNumberParts.get(w)));
					pm.setInfo("   - " + ((ImWord) pageNumberParts.get(w)).getString());
				}
			}
			else {
				pm.setInfo(" - got " + pageNumberParts.size() + " parts of possible page numbers:");
				HashSet pageNumberPartIDs = new HashSet();
				for (int w = 0; w < pageNumberParts.size(); w++)
					pageNumberPartIDs.add(((ImWord) pageNumberParts.get(w)).getLocalID());
				for (int w = 0; w < pageNumberParts.size(); w++) {
					pm.setInfo("   - " + ((ImWord) pageNumberParts.get(w)).getString());
					addPageNumberCandidates(pageData[p].pageNumberCandidates, ((ImWord) pageNumberParts.get(w)), pageNumberPartIDs, pageImageDpi, pm);
					Collections.sort(pageData[p].pageNumberCandidates, pageNumberValueOrder);
				}
			}
		}
		
		//	score and select page numbers for each page
		pm.setStep(" - scoring page numbers:");
		this.scoreAndSelectPageNumbers(pageData, pm);
		
		//	check page number sequence
		pm.setStep(" - checking page number sequence:");
		this.checkPageNumberSequence(pageData, pm);
		
		//	fill in missing page numbers
		pm.setStep(" - filling in missing page numbers:");
		this.fillInMissingPageNumbers(pageData, pm);
		
		//	annotate page numbers, collecting page number words
		HashSet pageNumberWordIDs = new HashSet();
		for (int p = 0; p < pageData.length; p++)
			this.annotatePageNumbers(pageData[p], pageNumberWordIDs);
		
		//	judge on page headers based on frequent words and on page numbers
		pm.setStep(" - detectinr page headers");
		for (int p = 0; p < pageData.length; p++) {
			for (int r = 0; r < pageData[p].topRegions.size(); r++) {
				ImRegion topRegion = ((ImRegion) pageData[p].topRegions.get(r));
				ImWord[] regionWords = topRegion.getWords();
				Arrays.sort(regionWords, ImUtils.textStreamOrder);
				if ((regionWords.length == 0) || ImWord.TEXT_STREAM_TYPE_PAGE_TITLE.equals(regionWords[0].getTextStreamType()))
					continue;
				if (this.isPageTitle(topRegion, regionWords, (pages.length / 2), (((p % 2) == 0) ? topWordsEven : topWordsOdd), (((p % 2) == 0) ? allWordsEven : allWordsOdd), pageNumberWordIDs)) {
					ImUtils.makeStream(regionWords, ImWord.TEXT_STREAM_TYPE_PAGE_TITLE, PAGE_TITLE_TYPE);
					ImUtils.orderStream(regionWords, ImUtils.leftRightTopDownOrder);
				}
			}
			for (int r = 0; r < pageData[p].bottomRegions.size(); r++) {
				ImRegion bottomRegion = ((ImRegion) pageData[p].bottomRegions.get(r));
				ImWord[] regionWords = bottomRegion.getWords();
				Arrays.sort(regionWords, ImUtils.textStreamOrder);
				if ((regionWords.length == 0) || ImWord.TEXT_STREAM_TYPE_PAGE_TITLE.equals(regionWords[0].getTextStreamType()))
					continue;
				if (this.isPageTitle(bottomRegion, regionWords, (pages.length / 2), (((p % 2) == 0) ? bottomWordsEven : bottomWordsOdd), (((p % 2) == 0) ? allWordsEven : allWordsOdd), pageNumberWordIDs)) {
					ImUtils.makeStream(regionWords, ImWord.TEXT_STREAM_TYPE_PAGE_TITLE, PAGE_TITLE_TYPE);
					ImUtils.orderStream(regionWords, ImUtils.leftRightTopDownOrder);
				}
			}
		}
		
		//	detect caption paragraphs
		pm.setStep(" - detecting captions");
		for (int p = 0; p < pages.length; p++) {
			ImRegion[] paragraphRegions = pages[p].getRegions(PARAGRAPH_TYPE);
			for (int r = 0; r < paragraphRegions.length; r++) {
				ImWord[] regionWords = paragraphRegions[r].getWords();
				if (regionWords.length == 0)
					continue;
				Arrays.sort(regionWords, ImUtils.textStreamOrder);
				if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(regionWords[0].getTextStreamType()))
					continue;
				if (this.isCaption(paragraphRegions[r], regionWords)) {
					ImUtils.makeStream(regionWords, ImWord.TEXT_STREAM_TYPE_CAPTION, CAPTION_TYPE);
					ImUtils.orderStream(regionWords, ImUtils.leftRightTopDownOrder);
				}
			}
		}
		
		//	detect footnote paragraphs
		pm.setStep(" - detecting footnotes");
		for (int p = 0; p < pages.length; p++) {
			ImRegion[] paragraphRegions = pages[p].getRegions(PARAGRAPH_TYPE);
			boolean newFootnoteFound;
			do {
				newFootnoteFound = false;
				for (int r = 0; r < paragraphRegions.length; r++) {
					ImWord[] regionWords = paragraphRegions[r].getWords();
					if (regionWords.length == 0)
						continue;
					Arrays.sort(regionWords, ImUtils.textStreamOrder);
					if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(regionWords[0].getTextStreamType()))
						continue;
					boolean nonFootnoteBelow = false;
					for (int cr = 0; cr < paragraphRegions.length; cr++) {
						if (cr == r)
							continue;
						if ((paragraphRegions[r].bounds.right < paragraphRegions[cr].bounds.left) || (paragraphRegions[cr].bounds.right < paragraphRegions[r].bounds.left))
							continue;
						if (paragraphRegions[cr].bounds.bottom < paragraphRegions[r].bounds.top)
							continue;
						ImWord[] cRegionWords = paragraphRegions[cr].getWords();
						if (cRegionWords.length == 0)
							continue;
						Arrays.sort(cRegionWords, ImUtils.textStreamOrder);
						if (!ImWord.TEXT_STREAM_TYPE_FOOTNOTE.equals(cRegionWords[0].getTextStreamType()) && !ImWord.TEXT_STREAM_TYPE_PAGE_TITLE.equals(cRegionWords[0].getTextStreamType()) && !ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(cRegionWords[0].getTextStreamType())) {
							nonFootnoteBelow = true;
							break;
						}
					}
					if (nonFootnoteBelow)
						continue;
					if (this.isFootnote(paragraphRegions[r], regionWords)) {
						ImUtils.makeStream(regionWords, ImWord.TEXT_STREAM_TYPE_FOOTNOTE, FOOTNOTE_TYPE);
						ImUtils.orderStream(regionWords, ImUtils.leftRightTopDownOrder);
						newFootnoteFound = true;
					}
				}
			} while (newFootnoteFound);
		}
		
		//	TODO consider detecting tables using result of column and row split attempts on regions in comparison to average of other regions in document or page
	}
	
	private boolean isFootnote(ImRegion paragraph, ImWord[] paragraphWords) {
		
		//	TODO use font size as additional evidence
		
		//	concatenate first ten or so words
		String paragraphStart = ImUtils.getString(paragraphWords[0], paragraphWords[(paragraphWords.length < 10) ? (paragraphWords.length - 1) : 9], true);
		
		//	test paragraph start against detector patterns
		for (int p = 0; p < this.footnoteStartPatterns.length; p++) {
			if (this.footnoteStartPatterns[p].matcher(paragraphStart).matches())
				return true;
		}
		
		//	no match found
		return false;
	}
	
	private boolean isCaption(ImRegion paragraph, ImWord[] paragraphWords) {
		
		//	concatenate first ten or so words
		String paragraphStart = ImUtils.getString(paragraphWords[0], paragraphWords[(paragraphWords.length < 10) ? (paragraphWords.length - 1) : 9], true);
		
		//	test paragraph start against detector patterns
		for (int p = 0; p < this.captionStartPatterns.length; p++) {
			if (this.captionStartPatterns[p].matcher(paragraphStart).matches())
				return true;
		}
		
		//	no match found
		return false;
	}
	
	private boolean isPageTitle(ImRegion region, ImWord[] regionWords, int pageCount, CountingSet edgeWords, CountingSet allWords, HashSet pageNumberWordIDs) {
		
		//	do we have a page number?
		for (int w = 0; w < regionWords.length; w++) {
			if (pageNumberWordIDs.contains(regionWords[w].getLocalID()))
				return true;
		}
		
		//	check words
		TreeSet edgeWordStrings = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		for (int w = 0; w < regionWords.length; w++) {
			String regionWordString = regionWords[w].getString();
			if (Gamta.isWord(regionWordString) || Gamta.isNumber(regionWordString))
				edgeWordStrings.add(regionWordString);
		}
		int edgeWordFrequencySum = 0;
		for (Iterator wit = edgeWordStrings.iterator(); wit.hasNext();)
			edgeWordFrequencySum += edgeWords.getCount((String) wit.next());
		if ((edgeWordFrequencySum * 2) > (pageCount * edgeWordStrings.size()))
			return true;
		
		//	check case as last resort
		int charCount = 0;
		int capCharCount = 0;
		for (int w = 0; w < regionWords.length; w++) {
			String wordStr = regionWords[w].getString();
			if (!Gamta.isWord(wordStr))
				continue;
			charCount += wordStr.length();
			for (int c = 0; c < wordStr.length(); c++) {
				if (wordStr.charAt(c) == Character.toUpperCase(wordStr.charAt(c)))
					capCharCount++;
			}
		}
		return ((capCharCount * 2) > charCount);
	}
	
	private void addPageNumberCandidates(List pageNumberCandidates, ImWord pageNumberStartPart, HashSet pageNumberPartIDs, int dpi, ProgressMonitor pm) {
		String pageNumberString = pageNumberStartPart.getString();
		this.addPageNumberCandidates(pageNumberCandidates, pageNumberStartPart, pageNumberStartPart, pageNumberString, pm);
		for (ImWord imw = pageNumberStartPart.getNextWord(); imw != null; imw = imw.getNextWord()) {
			if (imw.pageId != pageNumberStartPart.pageId)
				break; // off page
			if ((imw.centerY < pageNumberStartPart.bounds.top) || (pageNumberStartPart.bounds.bottom < imw.centerY))
				break; // out off line
			if (imw.bounds.left < pageNumberStartPart.bounds.right)
				break; // out of left-to-right order
			if ((dpi / 13) < (imw.bounds.left - pageNumberStartPart.bounds.right))
				break; // too far away (more than 2 mm)
			if (!pageNumberPartIDs.contains(imw.getLocalID()))
				break;
			pageNumberString += imw.getString();
			boolean newPageNumbers = this.addPageNumberCandidates(pageNumberCandidates, pageNumberStartPart, imw, pageNumberString, pm);
			if (!newPageNumbers)
				break; // no matches found
		}
	}
	
	private boolean addPageNumberCandidates(List pageNumberCandidates, ImWord firstWord, ImWord lastWord, String pageNumberString, ProgressMonitor pm) {
		
		//	page numbers with six or more digits are rather improbable ...
		if (pageNumberString.length() > 5)
			return false;
		
		//	extract possible interpretations, computing ambiguity along the way
		int [][] pageNumberDigits = new int[pageNumberString.length()][];
		int ambiguity = 1;
		for (int c = 0; c < pageNumberString.length(); c++) {
			String pageNumberCharacter = pageNumberString.substring(c, (c+1));
			pageNumberDigits[c] = ((int[]) this.ocrPageNumberCharacterTranslations.get(pageNumberCharacter));
			if (pageNumberDigits[c] == null)
				return false; // invalid character
			ambiguity *= pageNumberDigits[c].length;
		}
		
		//	the first digit is never zero ...
		if ((pageNumberDigits[0].length == 1) && (pageNumberDigits[0][0] == 0))
			return false;
		
		//	create all candidate values
		TreeSet values = this.getAllPossibleValues(pageNumberDigits);
		if (values.size() == 0)
			return false;
		pm.setInfo("   - got " + values.size() + " possible values");
		
		//	compute fuzzyness for each value
		for (Iterator vit = values.iterator(); vit.hasNext();) {
			String pageNumberValue = ((String) vit.next());
			pm.setInfo("     - " + pageNumberValue);
			int fuzzyness = 0;
			for (int d = 0; d < pageNumberValue.length(); d++) {
				if (pageNumberValue.charAt(d) != pageNumberString.charAt(d))
					fuzzyness++;
			}
			pageNumberCandidates.add(new PageNumber(pageNumberString, Integer.parseInt(pageNumberValue), fuzzyness, ambiguity, firstWord, lastWord));
		}
		
		//	did we add anything?
		return true;
	}
	
	private TreeSet getAllPossibleValues(int[][] baseDigits) {
		TreeSet allPossibleValues = new TreeSet();
		int[] digits = new int[baseDigits.length];
		addAllPossibleValues(baseDigits, digits, 0, allPossibleValues);
		return allPossibleValues;
	}
	
	private void addAllPossibleValues(int[][] baseDigits, int[] digits, int pos, TreeSet values) {
		if (pos == digits.length)
			values.add(this.getValueString(digits));
		else for (int d = 0; d < baseDigits[pos].length; d++) {
			digits[pos] = baseDigits[pos][d];
			addAllPossibleValues(baseDigits, digits, (pos+1), values);
		}
	}
	
	private String getValueString(int[] digits) {
		StringBuffer valueString = new StringBuffer();
		for (int d = 0; d < digits.length; d++)
			valueString.append("" + digits[d]);
		return valueString.toString();
	}
	
	private void scoreAndSelectPageNumbers(PageData[] pageData, ProgressMonitor pm) {
		
		//	work page by page
		for (int p = 0; p < pageData.length; p++) {
			
			//	score page numbers
			for (int n = 0; n < pageData[p].pageNumberCandidates.size(); n++) {
				PageNumber pn = ((PageNumber) pageData[p].pageNumberCandidates.get(n));
//				
//				//	penalize own fuzzyness
//				pn.score -=pn.fuzzyness;
				
				//	look forward
				int fMisses = 0;
				for (int l = 1; (p+l) < pageData.length; l++) {
					PageNumber cpn = null;
					for (int c = 0; c < pageData[p+l].pageNumberCandidates.size(); c++) {
						cpn = ((PageNumber) pageData[p+l].pageNumberCandidates.get(c));
						if (cpn.isConsistentWith(pn)) {
							pn.score += (((float) l) / (1 + cpn.fuzzyness));
//							pn.score -= cpn.fuzzyness;
							break;
						}
						else cpn = null;
					}
					if (cpn == null) {
						fMisses++;
						if (fMisses == 3)
							break;
					}
				}
				
				//	look backward
				int bMisses = 0;
				for (int l = 1; l <= p; l++) {
					PageNumber cpn = null;
					for (int c = 0; c < pageData[p-l].pageNumberCandidates.size(); c++) {
						cpn = ((PageNumber) pageData[p-l].pageNumberCandidates.get(c));
						if (cpn.isConsistentWith(pn)) {
							pn.score += (((float) l) / (1 + cpn.fuzzyness));
//							pn.score -= cpn.fuzzyness;
							break;
						}
						else cpn = null;
					}
					if (cpn == null) {
						bMisses++;
						if (bMisses == 3)
							break;
					}
				}
				
				//	penalize fuzzyness
				pn.score /= (pn.fuzzyness+1);
				
				pm.setInfo("   - " + pn.valueStr + " (as " + pn.value + ", fuzzyness " + pn.fuzzyness + ", ambiguity " + pn.ambiguity + ") on page " + p + " ==> " + pn.score);
			}
			
			//	select page number
			for (int n = 0; n < pageData[p].pageNumberCandidates.size(); n++) {
				PageNumber pn = ((PageNumber) pageData[p].pageNumberCandidates.get(n));
				if (pn.score < 1)
					continue;
				if ((pageData[p].pageNumber == null) || (pageData[p].pageNumber.score < pn.score))
					pageData[p].pageNumber = pn;
			}
			if (pageData[p].pageNumber == null)
				pm.setInfo(" --> could not determine page number of page " + p + ".");
			else pm.setInfo(" --> page number of " + p + " identified as " + pageData[p].pageNumber.value + " (score " + pageData[p].pageNumber.score + ")");
		}
	}
	
	private void checkPageNumberSequence(PageData[] pageData, ProgressMonitor pm) {
		for (int p = 0; p < pageData.length; p++) {
			if (pageData[p].pageNumber == null)
				continue;
			
			//	get adjacent page numbers
			PageNumber bpn = ((p == 0) ? null : pageData[p-1].pageNumber);
			PageNumber fpn = (((p+1) == pageData.length) ? null : pageData[p+1].pageNumber);
			if ((bpn == null) && (fpn == null))
				continue;
			
			//	check sequence consistent with what we can check against
			if (((bpn == null) || bpn.isConsistentWith(pageData[p].pageNumber)) && ((fpn == null) || fpn.isConsistentWith(pageData[p].pageNumber)))
				continue;
			
			//	correct that one oddjob in the middle
			if ((bpn != null) && (fpn != null) && bpn.isConsistentWith(fpn)) {
				pm.setInfo("   - eliminated page number " + pageData[p].pageNumber.value + " in page " + p + " for sequence inconsistency.");
				pageData[p].pageNumber = null;
				for (int n = 0; n < pageData[p].pageNumberCandidates.size(); n++) {
					PageNumber pn = ((PageNumber) pageData[p].pageNumberCandidates.get(n));
					if (pn.isConsistentWith(bpn) && pn.isConsistentWith(fpn)) {
						pageData[p].pageNumber = pn;
						pm.setInfo("   --> re-assigned to " + pageData[p].pageNumber.value);
						break;
					}
				}
				continue;
			}
			
			//	one side not set, other inconsistent and far more secure
			if ((bpn == null) && !fpn.isConsistentWith(pageData[p].pageNumber) && ((pageData[p].pageNumber.score * 2) < fpn.score)) {
				pm.setInfo("   - eliminated page number " + pageData[p].pageNumber.value + " in page " + p + " for sequence front edge inconsistency.");
				pageData[p].pageNumber = null;
				continue;
			}
			else if ((fpn == null) && !bpn.isConsistentWith(pageData[p].pageNumber) && ((pageData[p].pageNumber.score * 2) < bpn.score)) {
				pm.setInfo("   - eliminated page number " + pageData[p].pageNumber.value + " in page " + p + " for sequence back edge inconsistency.");
				pageData[p].pageNumber = null;
				continue;
			}
		}
	}
	
	private void fillInMissingPageNumbers(PageData[] pageData, ProgressMonitor pm) {
		
		//	make sure not to extrapolate to page numbers that are already taken
		HashSet existingPageNumbers = new HashSet();
		for (int p = 0; p < pageData.length; p++) {
			if (pageData[p].pageNumber != null)
				existingPageNumbers.add("" + pageData[p].pageNumber.value);
		}
		
		//	do backward sequence extrapolation
		PageNumber bPageNumber = null;
		for (int p = (pageData.length - 2); p >= 0; p--) {
			if (pageData[p].pageNumber != null) {
				bPageNumber = pageData[p].pageNumber;
				continue;
			}
			if (bPageNumber == null)
				continue;
			PageNumber ePageNumber = new PageNumber(p, (bPageNumber.value - bPageNumber.pageId + p));
			if (existingPageNumbers.add("" + ePageNumber.value)) {
				pageData[p].pageNumber = ePageNumber;
				pageData[p].page.setAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + ePageNumber.value));
				bPageNumber = ePageNumber;
				pm.setInfo(" --> backward-extrapolated page number of page " + p + " to " + pageData[p].pageNumber.value);
			}
			else pm.setInfo(" --> could not backward-extrapolated page number of page " + p + ", page number " + ePageNumber.value + " already assigned");
		}
		
		//	do forward sequence extrapolation
		PageNumber fPageNumber = null;
		for (int p = 1; p < pageData.length; p++) {
			if (pageData[p].pageNumber != null) {
				fPageNumber = pageData[p].pageNumber;
				continue;
			}
			if (fPageNumber == null)
				continue;
			PageNumber ePageNumber = new PageNumber(p, (bPageNumber.value - bPageNumber.pageId + p));
			if (existingPageNumbers.add("" + ePageNumber.value)) {
				pageData[p].pageNumber = ePageNumber;
				pageData[p].page.setAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + ePageNumber.value));
				fPageNumber = ePageNumber;
				pm.setInfo(" --> forward-extrapolated page number of page " + p + " to " + pageData[p].pageNumber.value);
			}
			else pm.setInfo(" --> could not forward-extrapolated page number of page " + p + ", page number " + ePageNumber.value + " already assigned");
		}
	}
	
	private void annotatePageNumbers(PageData pageData, HashSet pageNumberWordIDs) {
		if (pageData.pageNumber == null)
			return;
		pageData.page.setAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + pageData.pageNumber.value));
		if (pageData.pageNumber.firstWord == null)
			return;
		ImAnnotation pageNumber = pageData.page.getDocument().addAnnotation(pageData.pageNumber.firstWord, pageData.pageNumber.lastWord, PAGE_NUMBER_TYPE);
		pageNumber.setAttribute("value", ("" + pageData.pageNumber.value));
		pageNumber.setAttribute("score", ("" + pageData.pageNumber.score));
		pageNumber.setAttribute("ambiguity", ("" + pageData.pageNumber.ambiguity));
		pageNumber.setAttribute("fuzzyness", ("" + pageData.pageNumber.fuzzyness));
		for (ImWord imw = pageData.pageNumber.firstWord; imw != null; imw = imw.getNextWord()) {
			pageNumberWordIDs.add(imw.getLocalID());
			if (imw == pageData.pageNumber.lastWord)
				break;
		}
	}
	
	private static class PageData {
		final ImPage page;
		
		List topRegions = new ArrayList();
		List bottomRegions = new ArrayList();
		
		List pageNumberCandidates = new ArrayList(1);
		PageNumber pageNumber = null;
		
		PageData(ImPage page) {
			this.page = page;
		}
	}
	
	private class PageNumber {
		final int pageId;
		final String valueStr;
		final int value; // the integer value
		final int fuzzyness; // how many characters do not match our integer value
		final int ambiguity; // how many possible values are there for our word (sequence), including this one
		final ImWord firstWord;
		final ImWord lastWord;
		float score = 0;
		
		//	used for born-digital documents, where we have no need for interpretation
		PageNumber(ImWord word) {
			this.pageId = word.pageId;
			this.valueStr = word.getString();
			this.value = Integer.parseInt(word.getString());
			this.fuzzyness = 0;
			this.ambiguity = 1;
			this.firstWord = word;
			this.lastWord = word;
		}
		
		//	used for scanned documents
		PageNumber(String valueStr, int value, int fuzzyness, int ambiguity, ImWord firstWord, ImWord lastWord) {
			this.pageId = firstWord.pageId;
			this.valueStr = valueStr;
			this.value = value;
			this.fuzzyness = fuzzyness;
			this.ambiguity = ambiguity;
			this.firstWord = firstWord;
			this.lastWord = lastWord;
		}
		
		//	used for extrapolation
		PageNumber(int pageId, int value) {
			this.pageId = pageId;
			this.valueStr = null;
			this.value = value;
			this.fuzzyness = Integer.MAX_VALUE;
			this.ambiguity = Integer.MAX_VALUE;
			this.firstWord = null;
			this.lastWord = null;
		}
		
		public boolean equals(Object obj) {
			return (((PageNumber) obj).value == this.value);
		}
		public int hashCode() {
			return this.value;
		}
		public String toString() {
			return ("" + this.value);
		}
		boolean isConsistentWith(PageNumber pn) {
			return ((this.value - pn.value) == (this.pageId - pn.pageId));
		}
//		float getAdjustedScore() {
//			return (((this.fuzzyness == Integer.MAX_VALUE) || (this.ambiguity == Integer.MAX_VALUE)) ? 0 : (this.score / (this.fuzzyness + this.ambiguity)));
//		}
	}
	
	private static final Comparator pageNumberValueOrder = new Comparator() {
		public int compare(Object obj1, Object obj2) {
			PageNumber pn1 = ((PageNumber) obj1);
			PageNumber pn2 = ((PageNumber) obj2);
			return ((pn1.value == pn2.value) ? (pn1.fuzzyness - pn2.fuzzyness) : (pn1.value - pn2.value));
		}
	};
	
	private static class CountingSet {
		private static class Counter {
			int c = 0;
			Counter(int c) {
				this.c = c;
			}
		}
		private Map content;
		private int size = 0;
		CountingSet() {
			this(null);
		}
		CountingSet(Comparator contentOrder) {
			this.content = new TreeMap(contentOrder);
		}
		boolean add(Object obj) {
			return this.add(obj, 1);
		}
		boolean add(Object obj, int count) {
			if (count <= 0)
				return false;
			this.size += count;
			Counter cnt = ((Counter) this.content.get(obj));
			if (cnt == null) {
				cnt = new Counter(count);
				this.content.put(obj, cnt);
				return true;
			}
			else {
				cnt.c += count;
				return false;
			}
		}
		boolean addAll(Collection c) {
			boolean changed = false;
			for (Iterator oit = c.iterator(); oit.hasNext();) {
				Object obj = oit.next();
				changed = (changed | this.add(obj));
			}
			return changed;
		}
		boolean addAll(CountingSet cs) {
			boolean changed = false;
			if (cs == this)
				return changed;
			for (Iterator oit = cs.iterator(); oit.hasNext();) {
				Object obj = oit.next();
				changed = (changed | this.add(obj, cs.getCount(obj)));
			}
			return changed;
		}
		boolean remove(Object obj) {
			return this.remove(obj, 1);
		}
		boolean remove(Object obj, int count) {
			if (count <= 0)
				return false;
			Counter cnt = ((Counter) this.content.get(obj));
			if (cnt == null)
				return false;
			if (cnt.c <= count) {
				this.size -= cnt.c;
				this.content.remove(obj);
				return true;
			}
			else {
				cnt.c -= count;
				this.size -= count;
				return false;
			}
		}
		boolean removeAll(Collection c) {
			boolean changed = false;
			for (Iterator oit = c.iterator(); oit.hasNext();) {
				Object obj = oit.next();
				changed = (changed | this.remove(obj));
			}
			return changed;
		}
		boolean removeAll(CountingSet cs) {
			boolean changed = false;
			if (cs == this)
				return changed;
			for (Iterator oit = cs.iterator(); oit.hasNext();) {
				Object obj = oit.next();
				changed = (changed | this.remove(obj, cs.getCount(obj)));
			}
			return changed;
		}
		int removeAll(Object obj) {
			Counter cnt = ((Counter) this.content.get(obj));
			if (cnt == null)
				return 0;
			this.size -= cnt.c;
			this.content.remove(obj);
			return cnt.c;
		}
		boolean contains(Object obj) {
			return this.content.containsKey(obj);
		}
		int getCount(Object obj) {
			Counter cnt = ((Counter) this.content.get(obj));
			return ((cnt == null) ? 0 : cnt.c);
		}
		int size() {
			return this.size;
		}
		int elementCount() {
			return this.content.size();
		}
		Iterator iterator() {
			return this.content.keySet().iterator();
		}
	}
}
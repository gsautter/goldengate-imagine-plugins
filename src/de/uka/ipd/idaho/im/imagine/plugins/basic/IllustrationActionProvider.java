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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicFileChooserUI;

import de.uka.ipd.idaho.easyIO.streams.CharSequenceReader;
import de.uka.ipd.idaho.gamta.util.DocumentStyle.PropertiesData;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.gamta.util.swing.ProgressMonitorWindow;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImLayoutObject;
import de.uka.ipd.idaho.im.ImObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImSupplement;
import de.uka.ipd.idaho.im.ImSupplement.Figure;
import de.uka.ipd.idaho.im.ImSupplement.Graphics;
import de.uka.ipd.idaho.im.ImSupplement.Scan;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.analysis.Imaging;
import de.uka.ipd.idaho.im.analysis.Imaging.AnalysisImage;
import de.uka.ipd.idaho.im.analysis.Imaging.ImagePartRectangle;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ImageDocumentDropHandler;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.TwoClickSelectionAction;
import de.uka.ipd.idaho.im.util.ImDocumentStyle;
import de.uka.ipd.idaho.im.util.ImIllustrationUtils;
import de.uka.ipd.idaho.im.util.ImIllustrationUtils.RenderingOptions;
import de.uka.ipd.idaho.im.util.ImObjectTransformer;
import de.uka.ipd.idaho.im.util.ImObjectTransformer.AttributeTransformer;
import de.uka.ipd.idaho.im.util.ImUtils;

/**
 * This class provides basic actions for handling illustrations, namely figures
 * and graphics.
 * 
 * @author sautter
 */
public class IllustrationActionProvider extends AbstractSelectionActionProvider implements LiteratureConstants, ImageDocumentDropHandler {
	private RegionActionProvider regionActions = null;
	
	/** public zero-argument constructor for class loading */
	public IllustrationActionProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Illustration Actions";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractGoldenGateImaginePlugin#initImagine()
	 */
	public void initImagine() {
		ImObjectTransformer.addGlobalAttributeTransformer(new AttributeTransformer() {
			public boolean canTransformAttribute(String name) {
				return ("rightContinuesFrom".equals(name) || "rightContinuesIn".equals(name) || "bottomContinuesFrom".equals(name) || "bottomContinuesIn".equals(name));
			}
			public Object transformAttributeValue(ImObject object, String name, Object value, ImObjectTransformer transformer) {
				if (value == null)
					return null;
				String wordId = value.toString();
				int split = wordId.indexOf(".");
				if (split == -1)
					return value;
				try {
					int pageId = Integer.parseInt(wordId.substring(0, split));
					if (pageId != transformer.fromPageId)
						return value;
					BoundingBox bounds = BoundingBox.parse(wordId.substring(split + ".".length()));
					return (transformer.toPageId + "." + transformer.transformBounds(bounds));
				}
				catch (RuntimeException re) {
					return value;
				}
			}
		});
		
		//	get region action provider
		this.regionActions = ((RegionActionProvider) this.imagineParent.getPlugin(RegionActionProvider.class.getName()));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageDocumentDropHandler#handleDrop(de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, de.uka.ipd.idaho.im.ImPage, int, int, java.awt.dnd.DropTargetDropEvent)
	 */
	public boolean handleDrop(ImDocumentMarkupPanel idmp, ImPage page, int pageX, int pageY, DropTargetDropEvent dtde) {
		return this.handleDrop(idmp, page, pageX, pageY, dtde.getTransferable());
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageDocumentDropHandler#handleDrop(de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, de.uka.ipd.idaho.im.ImPage, int, int, java.awt.datatransfer.Transferable)
	 */
	public boolean handleDrop(ImDocumentMarkupPanel idmp, ImPage page, int pageX, int pageY, Transferable transfer) {
		
		//	we're only doing this for scanned documents (for now)
		if (idmp.documentBornDigital)
			return false;
		
		//	images not showing, this one isn't for us
		if (!idmp.areRegionsPainted(ImRegion.IMAGE_TYPE))
			return false;
		
		//	do we have any images to work with?
		ImRegion[] images = page.getRegions(ImRegion.IMAGE_TYPE);
		if (images.length == 0)
			return false;
		
		//	find target image
		ImRegion targetImage = null;
		for (int i = 0; i < images.length; i++) {
			if (pageX < images[i].bounds.left)
				continue;
			if (images[i].bounds.right <= pageX)
				continue;
			if (pageY < images[i].bounds.top)
				continue;
			if (images[i].bounds.bottom <= pageY)
				continue;
			targetImage = images[i];
			break;
		}
		if (targetImage == null)
			return false;
		
		//	check if we have some data we can work with
		DataFlavor[] dataFlavors = transfer.getTransferDataFlavors();
		for (int f = 0; f < dataFlavors.length; f++) try {
			System.out.println("Trying data flavor " + dataFlavors[f].toString());
			System.out.println(" - MIME type is " + dataFlavors[f].getMimeType());
			System.out.println(" - representation class is " + dataFlavors[f].getRepresentationClass());
			
			//	get basic data
			String mimeType = dataFlavors[f].getMimeType();
			Class representationClass = dataFlavors[f].getRepresentationClass();
			
			//	file (list) drop
			if (("application/x-java-file-list".equalsIgnoreCase(mimeType) || mimeType.toLowerCase().startsWith("application/x-java-file-list; class=")) && List.class.isAssignableFrom(representationClass)) {
				List files = ((List) transfer.getTransferData(dataFlavors[f]));
				System.out.println(" - got " + files.size() + " files");
				if (files.size() != 1)
					continue;
				if (this.addAlternativeRendition(idmp.document, targetImage, page.getImageDPI(), ((File) files.get(0)), idmp))
					return true;
			}
			if (("image/x-java-image".equalsIgnoreCase(mimeType) || mimeType.toLowerCase().startsWith("image/x-java-image; class=")) && Image.class.isAssignableFrom(dataFlavors[f].getRepresentationClass())) {
				Image image = ((Image) transfer.getTransferData(dataFlavors[f]));
				System.out.println(" - got image sized " + image.getWidth(null) + "x" + image.getHeight(null));
				if (this.addAlternativeRendition(idmp.document, targetImage, page.getImageDPI(), image, idmp))
					return true;
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.out);
		}
		
		//	failed to find anything to work with
		return false;
	}
	
	private boolean addAlternativeRendition(ImDocument doc, ImRegion imageRegion, int pageDpi, File imageData, ImDocumentMarkupPanel idmp) throws IOException {
		System.out.println(" - loading image from " + imageData.getAbsolutePath().toString());
		BufferedImage bi = ImageIO.read(imageData);
		if (bi == null)
			return false;
		System.out.println(" - got image sized " + bi.getWidth(null) + "x" + bi.getHeight(null));
		
		//	store image in atomic action on drop
		try {
			if (idmp != null)
				idmp.beginAtomicAction("Set Alternative Rendition of Image");
			return this.addAlternativeRendition(doc, imageRegion, pageDpi, bi);
		}
		finally {
			if (idmp != null)
				idmp.endAtomicAction();
		}
	}
	
	private boolean addAlternativeRendition(ImDocument doc, ImRegion imageRegion, int pageDpi, Image image, ImDocumentMarkupPanel idmp) {
		
		//	transfer to raster image
		BufferedImage bi = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D gr = bi.createGraphics();
		gr.setColor(Color.WHITE);
		gr.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		gr.drawImage(image, 0, 0, null);
		gr.dispose();
		System.out.println(" - image internalized");
		
		//	store image in atomic action on drop
		try {
			if (idmp != null)
				idmp.beginAtomicAction("Set Alternative Rendition of Image");
			return this.addAlternativeRendition(doc, imageRegion, pageDpi, bi);
		}
		finally {
			if (idmp != null)
				idmp.endAtomicAction();
		}
	}
	
	private boolean addAlternativeRendition(ImDocument doc, ImRegion imageRegion, int pageDpi, BufferedImage bi) {
		
		//	counter rotation of rendering (assuming image proper is upright)
		if (ImLayoutObject.TEXT_DIRECTION_BOTTOM_UP.equals(imageRegion.getAttribute(ImLayoutObject.TEXT_DIRECTION_ATTRIBUTE)))
			bi = ImObjectTransformer.transformImage(bi, ImObjectTransformer.COUNTER_CLOCKWISE_ROTATION);
		else if (ImLayoutObject.TEXT_DIRECTION_TOP_DOWN.equals(imageRegion.getAttribute(ImLayoutObject.TEXT_DIRECTION_ATTRIBUTE)))
			bi = ImObjectTransformer.transformImage(bi, ImObjectTransformer.CLOCKWISE_ROTATION);
		
		//	compute DPI figure resolution
		float dpiRatioX = (((float) bi.getWidth()) / imageRegion.bounds.getWidth());
		float dpiRatioY = (((float) bi.getHeight()) / imageRegion.bounds.getHeight());
		float rawDpi = (pageDpi * ((dpiRatioX + dpiRatioY) / 2));
		int imageDpi = (Math.round(rawDpi / 10) * 10);
		System.out.println(" - resolution computed as " + imageDpi + " DPI (" + rawDpi + ", X " + (dpiRatioX * pageDpi) + ", Y " + (dpiRatioY * pageDpi) + ")");
		
		//	check DPI ratios and prompt for confirmation if too different
		float dpiRatioDiff = Math.abs(dpiRatioX - dpiRatioY);
		if ((dpiRatioDiff * 10) > ((dpiRatioX + dpiRatioY) / 2)) {
			float rawDpiX = (pageDpi * dpiRatioX);
			int imageDpiX = (Math.round(rawDpiX / 10) * 10);
			float rawDpiY = (pageDpi * dpiRatioY);
			int imageDpiY = (Math.round(rawDpiY / 10) * 10);
			int choice = DialogFactory.confirm(("The size of the image you selected as an alternate rendition of the image at " + imageRegion.bounds + " does not match.\r\nResolution computes to " + imageDpiX + " DPI (" + rawDpiX + ") along the horizontal axis and " + imageDpiY + " DPI (" + rawDpiY + ") along the vertical axis.\r\nAdd the image as the alternative rendition anyway?."), "Image Format Out Of Proportion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (choice != JOptionPane.YES_OPTION)
				return false;
		}
		
		//	finally ...
		Figure.createFigure(doc, imageRegion.pageId, 0 /* indicating 'underneath everything else' */, imageDpi, bi, imageRegion.bounds);
		DialogFactory.alert(("The alternative rendition of the image at " + imageRegion.bounds + "\r\nhas been set to a new image sized " + bi.getWidth() + "x" + bi.getHeight() + " for resolution of " + imageDpi + " DPI.\r\nThe alternative rendition will be preferred on exports and copying operations."), "Alternative Rendition Set", JOptionPane.INFORMATION_MESSAGE);
		return true;
	}
	
	boolean addAlternativeRendition(ImDocument doc, ImRegion imageRegion, int pageDpi) {
		final JFileChooser fc = this.getFileChooser();
		
		fc.addChoosableFileFilter(pngFileFilter);
		fc.addChoosableFileFilter(bmpFileFilter);
		fc.addChoosableFileFilter(jpgFileFilter);
		fc.addChoosableFileFilter(gifFileFilter);
		fc.setFileFilter(pngFileFilter);
		
		if (fc.showOpenDialog(DialogFactory.getTopWindow()) != JFileChooser.APPROVE_OPTION)
			return false;
		
		File imageFile = fc.getSelectedFile();
		try {
			return this.addAlternativeRendition(doc, imageRegion, pageDpi, imageFile, null);
		}
		catch (IOException ioe) {
			DialogFactory.alert(("Failed to add image from " + imageFile.getAbsolutePath() + ":\r\n" + ioe.getMessage()), "Could Not Load Image", JOptionPane.ERROR_MESSAGE);
			ioe.printStackTrace(System.out);
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, final ImPage page, final ImDocumentMarkupPanel idmp) {
		
		//	mark selection
		BoundingBox selectedBox = new BoundingBox(Math.min(start.x, end.x), Math.max(start.x, end.x), Math.min(start.y, end.y), Math.max(start.y, end.y));
		
		//	get selected words
		final ImWord[] selectedWords = page.getWordsInside(selectedBox);
//		
//		//	anything to work with?
//		if (selectedWords.length == 0) {}
//		else if (idmp.areRegionsPainted(ImRegion.IMAGE_TYPE)) {}
//		else if (idmp.areRegionsPainted(ImRegion.GRAPHICS_TYPE)) {}
//		else return null;
		
		//	get selected and context regions
		ImRegion[] pageRegions = page.getRegions();
		ArrayList overlappingRegionList = new ArrayList();
		ArrayList selectedImages = new ArrayList();
		ArrayList selectedGraphics = new ArrayList();
		for (int r = 0; r < pageRegions.length; r++) {
			if (pageRegions[r].bounds.right < selectedBox.left)
				continue;
			if (selectedBox.right < pageRegions[r].bounds.left)
				continue;
			if (pageRegions[r].bounds.bottom < selectedBox.top)
				continue;
			if (selectedBox.bottom < pageRegions[r].bounds.top)
				continue;
			overlappingRegionList.add(pageRegions[r]);
			if (!pageRegions[r].bounds.includes(selectedBox, true) && !selectedBox.includes(pageRegions[r].bounds, true))
				continue;
			if (!idmp.areRegionsPainted(pageRegions[r].getType()))
				continue;
			if (ImRegion.IMAGE_TYPE.equals(pageRegions[r].getType()))
				selectedImages.add(pageRegions[r]);
			else if (ImRegion.GRAPHICS_TYPE.equals(pageRegions[r].getType()))
				selectedGraphics.add(pageRegions[r]);
		}
		final ImRegion[] overlappingRegions = ((ImRegion[]) overlappingRegionList.toArray(new ImRegion[overlappingRegionList.size()]));
		final ImRegion selImage = ((selectedImages.size() == 1) ? ((ImRegion) selectedImages.get(0)) : null);
		final ImRegion selGraphics = ((selectedGraphics.size() == 1) ? ((ImRegion) selectedGraphics.get(0)) : null);
		
		//	collect actions
		LinkedList actions = new LinkedList();
		
		//	no words selected (text block action handle cases where there are, for now, including all the splitting, etc.)
		boolean offerMarkIllustration = false;
		if (offerMarkIllustration) { /* we just want everything under 'else if' ... */ }
		else if (selImage != null) {
			if (idmp.documentBornDigital)
				offerMarkIllustration = true; // there will be supplements for additional checks
			else if (selectedBox.getWidth() < page.getImageDPI())
				offerMarkIllustration = false;
			else if (selectedBox.getHeight() < page.getImageDPI())
				offerMarkIllustration = false;
			else offerMarkIllustration = true;
		}
		else if (selGraphics != null) {
			if (idmp.documentBornDigital)
				offerMarkIllustration = true; // there will be supplements for additional checks
			else if (selectedBox.getWidth() < page.getImageDPI())
				offerMarkIllustration = false;
			else if (selectedBox.getHeight() < page.getImageDPI())
				offerMarkIllustration = false;
			else offerMarkIllustration = true;
		}
		else if (idmp.documentBornDigital)
			offerMarkIllustration = true; // there will be supplements for additional checks
		else if (selectedBox.getWidth() < (page.getImageDPI() / 4))
			offerMarkIllustration = false;
		else if (selectedBox.getHeight() < (page.getImageDPI() / 4))
			offerMarkIllustration = false;
		else if (selectedBox.getWidth() < (page.getImageDPI() / 2) && (selectedBox.getHeight() < (page.getImageDPI() / 2)))
			offerMarkIllustration = false;
		else offerMarkIllustration = true;
		if (offerMarkIllustration) {
			int sLeft = Math.max(page.bounds.left, selectedBox.left);
			int sRight = Math.min(page.bounds.right, selectedBox.right);
			int sTop = Math.max(page.bounds.top, selectedBox.top);
			int sBottom = Math.min(page.bounds.bottom, selectedBox.bottom);
			final BoundingBox selectedBounds = new BoundingBox(sLeft, sRight, sTop, sBottom);
			
			//	check supplements to tell image from graphics if document born-digital
			boolean canBeImage;
			boolean canBeGraphics;
			if (idmp.documentBornDigital) {
				ImSupplement[] pageSupplements = page.getSupplements();
				Figure[] figures = ImSupplement.getFiguresIn(pageSupplements, selectedBox);
				int figureArea = 0;
				for (int f = 0; f < figures.length; f++)
					figureArea += figures[f].getBounds().getArea();
				canBeImage = (figureArea != 0);
				Graphics[] graphics = ImSupplement.getGraphicsIn(pageSupplements, selectedBox);
				int graphicsArea = 0;
				for (int g = 0; g < graphics.length; g++)
					graphicsArea += graphics[g].getBounds().getArea();
				canBeGraphics = (graphicsArea != 0);
			}
			else {
				canBeImage = true;
				canBeGraphics = false;
			}
			
			//	mark selected non-white area as image (also if we have an image selected that might require resizing)
			if (canBeImage || (selImage != null))
				actions.add(new SelectionAction("markRegionImage", "Mark Image", "Mark selected region as an image.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return markIllustration(selectedWords, page, selectedBounds, overlappingRegions, ImRegion.IMAGE_TYPE, idmp);
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.IMAGE_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
			
			//	mark selected non-white area as graphics (also if we have a graphics selected that might require resizing)
			if (canBeGraphics || (selGraphics != null))
				actions.add(new SelectionAction("markRegionGraphics", "Mark Graphics", "Mark selected region as a vector based graphics.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return markIllustration(selectedWords, page, selectedBounds, overlappingRegions, ImRegion.GRAPHICS_TYPE, idmp);
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.GRAPHICS_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
		}
		
		//	any single image or graphics to work with?
		if ((selImage == null) && (selGraphics == null))
			return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
		
		//	get supplements if we might need them for images or graphics
		final ImSupplement[] supplements = ((idmp.areRegionsPainted(ImRegion.IMAGE_TYPE) || idmp.areRegionsPainted(ImRegion.GRAPHICS_TYPE)) ? page.getSupplements() : null);
		
		//	get clicked figure and graphics supplements
		final Figure[] clickedFigures = ImSupplement.getFiguresAt(supplements, selectedBox);
		final Graphics[] clickedGraphics = ImSupplement.getGraphicsAt(supplements, selectedBox);
		
		//	offer copying selected figure to clipboard, as well as assigning caption
		if (selImage != null) {
			actions.add(new TwoClickSelectionAction("assignCaptionImage", "Assign Caption", "Assign a caption to this image with a second click.") {
				public boolean performAction(ImWord secondWord) {
					
					//	find affected caption
					ImAnnotation[] wordAnnots = idmp.document.getAnnotationsSpanning(secondWord);
					ArrayList wordCaptions = new ArrayList(2);
					for (int a = 0; a < wordAnnots.length; a++) {
						if (!ImAnnotation.CAPTION_TYPE.equals(wordAnnots[a].getType()))
							continue;
						if (ImWord.TEXT_STREAM_TYPE_CAPTION.equals(secondWord.getTextStreamType()))
							wordCaptions.add(wordAnnots[a]);
						else if (wordAnnots[a].hasAttribute(IN_LINE_OBJECT_MARKER_ATTRIBUTE))
							wordCaptions.add(wordAnnots[a]);
					}
					if (wordCaptions.size() != 1)
						return false;
					ImAnnotation wordCaption = ((ImAnnotation) wordCaptions.get(0));
					
					//	does this caption match?
					String firstWordStr = ImUtils.getStringFrom(wordCaption.getFirstWord());
					if (firstWordStr.toLowerCase().startsWith("tab")) {
						int choice = DialogFactory.confirm("This caption appears to belong to a table rather than an image.\r\nAre you sure you want to assign it to the image?", "Assign Table Caption to Image?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
						if (choice != JOptionPane.YES_OPTION)
							return false;
					}
					
					//	set attributes
					wordCaption.setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, selImage.bounds.toString());
					wordCaption.setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + selImage.pageId));
					return true;
				}
				public boolean performAction(ImPage secondPage, Point secondPoint) {
					return false;
				}
				public ImRegion getFirstRegion() {
					return selImage;
				}
				public String getActiveLabel() {
					return ("Click on a caption to assign it to the image at " + selImage.bounds.toString() + " on page " + (selImage.pageId + 1));
				}
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					JMenuItem mi = super.getMenuItem(invoker);
					Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.IMAGE_TYPE);
					if (regionTypeColor != null) {
						mi.setOpaque(true);
						mi.setBackground(regionTypeColor);
					}
					return mi;
				}
			});
			actions.add(new SelectionAction("textDirectionImage", "Set Image Text Direction", "Adjust the text direction of the selected image.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					String imageTextDirection = ((String) selImage.getAttribute(ImRegion.TEXT_DIRECTION_ATTRIBUTE, ImRegion.TEXT_DIRECTION_LEFT_RIGHT));
					JMenu pm = new JMenu("Image Text Direction");
					ButtonGroup bg = new ButtonGroup();
					final JMenuItem lrmi = new JRadioButtonMenuItem("Left-Right", ImRegion.TEXT_DIRECTION_LEFT_RIGHT.equals(imageTextDirection));
					lrmi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (lrmi.isSelected())
								setTextDirection(ImRegion.TEXT_DIRECTION_LEFT_RIGHT, invoker);
						}
					});
					pm.add(lrmi);
					bg.add(lrmi);
					final JMenuItem bumi = new JRadioButtonMenuItem("Bottom-Up", ImRegion.TEXT_DIRECTION_BOTTOM_UP.equals(imageTextDirection));
					bumi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (bumi.isSelected())
								setTextDirection(ImRegion.TEXT_DIRECTION_BOTTOM_UP, invoker);
						}
					});
					pm.add(bumi);
					bg.add(bumi);
					final JMenuItem tdmi = new JRadioButtonMenuItem("Top-Down", ImRegion.TEXT_DIRECTION_TOP_DOWN.equals(imageTextDirection));
					tdmi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (tdmi.isSelected())
								setTextDirection(ImRegion.TEXT_DIRECTION_TOP_DOWN, invoker);
						}
					});
					pm.add(tdmi);
					bg.add(tdmi);
					final JMenuItem udmi = new JRadioButtonMenuItem("Right-Left & Upside-Down", ImRegion.TEXT_DIRECTION_RIGHT_LEFT_UPSIDE_DOWN.equals(imageTextDirection));
					udmi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (udmi.isSelected())
								setTextDirection(ImRegion.TEXT_DIRECTION_RIGHT_LEFT_UPSIDE_DOWN, invoker);
						}
					});
					pm.add(udmi);
					bg.add(udmi);
					pm.setToolTipText(this.tooltip);
					Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.IMAGE_TYPE);
					if (regionTypeColor != null) {
						pm.setOpaque(true);
						pm.setBackground(regionTypeColor);
					}
					return pm;
				}
				private void setTextDirection(String textDirection, ImDocumentMarkupPanel invoker) {
					invoker.beginAtomicAction(this.label);
					if (ImWord.TEXT_DIRECTION_LEFT_RIGHT.equals(textDirection))
						selImage.removeAttribute(ImWord.TEXT_DIRECTION_ATTRIBUTE);
					else selImage.setAttribute(ImWord.TEXT_DIRECTION_ATTRIBUTE, textDirection);
					invoker.endAtomicAction();
					invoker.validate();
					invoker.repaint();
				}
			});
			
			if (selImage.hasAttribute("rightContinuesFrom")) {
				actions.add(new SelectionAction("imageCutRight", "Disconnect Images Horizontally", "Disconnect the image from the one it extends to the right.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return disconnectImagesLeftRight(idmp.document, selImage);
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.IMAGE_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
			}
			else actions.add(new TwoClickSelectionAction("imageExtendRight", "Attach Image Right", "Attach this image to the right of another image.") {
				public boolean performAction(ImWord secondWord) {
					return connectImagesLeftRight(selImage, secondWord);
				}
				public boolean performAction(ImPage secondPage, Point secondPoint) {
					return connectImagesLeftRight(selImage, secondPage, secondPoint);
				}
				public ImRegion getFirstRegion() {
					return selImage;
				}
				public String getActiveLabel() {
					return ("Click into the image to extend rightwards");
				}
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					JMenuItem mi = super.getMenuItem(invoker);
					Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.IMAGE_TYPE);
					if (regionTypeColor != null) {
						mi.setOpaque(true);
						mi.setBackground(regionTypeColor);
					}
					return mi;
				}
			});
			if (selImage.hasAttribute("bottomContinuesFrom")) {
				actions.add(new SelectionAction("imageCutBottom", "Disconnect Images Vertically", "Disconnect the image from the one it extends to bottom.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return disconnectImagesTopBottom(idmp.document, selImage);
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.IMAGE_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
			}
			else actions.add(new TwoClickSelectionAction("imageExtendBottom", "Attach Image Bottom", "Attach this image to the bottom of another image.") {
				public boolean performAction(ImWord secondWord) {
					return connectImagesTopBottom(selImage, secondWord);
				}
				public boolean performAction(ImPage secondPage, Point secondPoint) {
					return connectImagesTopBottom(selImage, secondPage, secondPoint);
				}
				public ImRegion getFirstRegion() {
					return selImage;
				}
				public String getActiveLabel() {
					return ("Click into the image to extend downwards");
				}
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					JMenuItem mi = super.getMenuItem(invoker);
					Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.IMAGE_TYPE);
					if (regionTypeColor != null) {
						mi.setOpaque(true);
						mi.setBackground(regionTypeColor);
					}
					return mi;
				}
			});
			
			if (!idmp.documentBornDigital) {
				if (clickedFigures.length == 0)
					actions.add(new SelectionAction("setAlternateRendition", "Add Alternative Rendition", "Add an image from a local file as an alternative/improved rendition of the image.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return addAlternativeRendition(idmp.document, selImage, page.getImageDPI());
						}
						public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
							JMenuItem mi = super.getMenuItem(invoker);
							Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.IMAGE_TYPE);
							if (regionTypeColor != null) {
								mi.setOpaque(true);
								mi.setBackground(regionTypeColor);
							}
							return mi;
						}
					});
				else if (clickedFigures.length == 1) {
					actions.add(new SelectionAction("setAlternateRendition", "Change Alternative Rendition", "Replace the alternative/improved rendition of the image with an image from a local file.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return addAlternativeRendition(idmp.document, selImage, page.getImageDPI());
						}
						public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
							JMenuItem mi = super.getMenuItem(invoker);
							Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.IMAGE_TYPE);
							if (regionTypeColor != null) {
								mi.setOpaque(true);
								mi.setBackground(regionTypeColor);
							}
							return mi;
						}
					});
					actions.add(new SelectionAction("setAlternateRendition", "Remove Alternative Rendition", "Remove the alternative/improved rendition of the image.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							idmp.document.removeSupplement(clickedFigures[0]);
							return true;
						}
						public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
							JMenuItem mi = super.getMenuItem(invoker);
							Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.IMAGE_TYPE);
							if (regionTypeColor != null) {
								mi.setOpaque(true);
								mi.setBackground(regionTypeColor);
							}
							return mi;
						}
					});
				}
			}
			
			final Scan contextScan;
			if (idmp.documentBornDigital)
				contextScan = null;
			else {
				Scan scan = null;
				for (int s = 0; s < supplements.length; s++)
					if (supplements[s] instanceof Scan) {
						scan = ((Scan) supplements[s]);
						break;
					}
				contextScan = scan;
			}
			if ((clickedFigures.length == 1) || (contextScan != null)) {// clicked single figure, or we have a scan and no figures at all
				actions.add(new SelectionAction("copyImage", "Copy Image", "Copy the selected image to the system clipboard.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						extractImageOrGrid(null, ((clickedFigures.length == 1) ? clickedFigures[0] : null), ((clickedFigures.length == 1) ? null : contextScan), selImage, false, idmp);
						return false;
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.IMAGE_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
				actions.add(new SelectionAction("saveImage", "Save Image", "Save the selected image to disk.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						String imageFileName = getExportFileName(idmp.document, page, selImage);
						extractImageOrGrid(imageFileName, ((clickedFigures.length == 1) ? clickedFigures[0] : null), ((clickedFigures.length == 1) ? null : contextScan), selImage, false, idmp);
						return false;
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.IMAGE_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
			}
			
			//	offer saving graphics if clicked
			if ((clickedGraphics.length == 1) && (selGraphics == null)) {
				actions.add(new SelectionAction("copyGraphics", "Copy Graphics", "Copy the selected graphics to the system clipboard.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						extractGraphics(null, clickedGraphics[0], true, selImage, page, supplements, idmp);
						return false;
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.IMAGE_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
				actions.add(new SelectionAction("saveGraphics", "Save Graphics", "Save the selected graphics to disk.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						String graphicsFileName = getExportFileName(idmp.document, page, selGraphics);
						extractGraphics(graphicsFileName, clickedGraphics[0], true, selImage, page, supplements, idmp);
						return false;
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.IMAGE_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
			}
			
			// we have an image group to copy or save, or decorative vector graphics or label words to go with it
			final Figure[] contextFigures = ImSupplement.getFiguresIn(supplements, selImage.bounds);
			final Graphics[] contextGraphics = ImSupplement.getGraphicsIn(supplements, selImage.bounds);
			final ImWord[] contextWords = getLabelWords(page.getWordsInside(selImage.bounds));
			if ((contextFigures.length > 1) || ((contextWords.length + contextGraphics.length) != 0)) {
				actions.add(new SelectionAction("copyImageCustom", "Copy Image or Group ...", "Copy the selected image or group to the system clipboard, with custom options.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						extractImageOrGrid(null, null, null, selImage, false, idmp);
						return false;
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.IMAGE_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
				actions.add(new SelectionAction("saveImageCustom", "Save Image or Group ...", "Save the selected image or group to disk, with custom options.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						String imageFileName = getExportFileName(idmp.document, page, selImage);
						extractImageOrGrid(imageFileName, null, null, selImage, false, idmp);
						return false;
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.IMAGE_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
			}
			
			//	if we have a figure grid, offer copying and saving that as well
			if (selImage.hasAttribute("rightContinuesIn") || selImage.hasAttribute("rightContinuesFrom") || selImage.hasAttribute("bottomContinuesIn") || selImage.hasAttribute("bottomContinuesFrom")) {
				actions.add(new SelectionAction("copyImageGrid", "Copy Image Grid", "Copy the selected image and its attached images to the system clipboard.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						extractImageOrGrid(null, null, null, selImage, true, idmp);
						return false;
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.IMAGE_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
				actions.add(new SelectionAction("saveImageGrid", "Save Image Grid", "Save the selected image and its attached images to disk.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						String imageFileName = getExportFileName(idmp.document, page, selImage);
						extractImageOrGrid(imageFileName, null, null, selImage, true, idmp);
						return false;
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.IMAGE_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
			}
		}
		
		//	offer copying selected graphics to clipboard, as well as assigning caption
		if (selGraphics != null) {
			actions.add(new TwoClickSelectionAction("assignCaptionGraphics", "Assign Caption", "Assign a caption to this graphics with a second click.") {
				public boolean performAction(ImWord secondWord) {
					
					//	find affected caption
					ImAnnotation[] wordAnnots = idmp.document.getAnnotationsSpanning(secondWord);
					ArrayList wordCaptions = new ArrayList(2);
					for (int a = 0; a < wordAnnots.length; a++) {
						if (!ImAnnotation.CAPTION_TYPE.equals(wordAnnots[a].getType()))
							continue;
						if (ImWord.TEXT_STREAM_TYPE_CAPTION.equals(secondWord.getTextStreamType()))
							wordCaptions.add(wordAnnots[a]);
						else if (wordAnnots[a].hasAttribute(IN_LINE_OBJECT_MARKER_ATTRIBUTE))
							wordCaptions.add(wordAnnots[a]);
					}
					if (wordCaptions.size() != 1)
						return false;
					ImAnnotation wordCaption = ((ImAnnotation) wordCaptions.get(0));
					
					//	does this caption match?
					String firstWordStr = ImUtils.getStringFrom(wordCaption.getFirstWord());
					if (firstWordStr.toLowerCase().startsWith("tab")) {
						int choice = DialogFactory.confirm("This caption appears to belong to a table rather than a graphics.\r\nAre you sure you want to assign it to the graphics?", "Assign Table Caption to Graphics?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
						if (choice != JOptionPane.YES_OPTION)
							return false;
					}
					
					//	set attributes
					wordCaption.setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, selGraphics.bounds.toString());
					wordCaption.setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + selGraphics.pageId));
					return true;
				}
				public boolean performAction(ImPage secondPage, Point secondPoint) {
					return false;
				}
				public ImRegion getFirstRegion() {
					return selGraphics;
				}
				public String getActiveLabel() {
					return ("Click on a caption to assign it to the graphics at " + selGraphics.bounds.toString() + " on page " + (selGraphics.pageId + 1));
				}
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					JMenuItem mi = super.getMenuItem(invoker);
					Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.GRAPHICS_TYPE);
					if (regionTypeColor != null) {
						mi.setOpaque(true);
						mi.setBackground(regionTypeColor);
					}
					return mi;
				}
			});
			actions.add(new SelectionAction("textDirectionImage", "Set Graphics Text Direction", "Adjust the text direction of the selected graphics.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					String graphicsTextDirection = ((String) selGraphics.getAttribute(ImRegion.TEXT_DIRECTION_ATTRIBUTE, ImRegion.TEXT_DIRECTION_LEFT_RIGHT));
					JMenu pm = new JMenu("Graphics Text Direction");
					ButtonGroup bg = new ButtonGroup();
					final JMenuItem lrmi = new JRadioButtonMenuItem("Left-Right", ImRegion.TEXT_DIRECTION_LEFT_RIGHT.equals(graphicsTextDirection));
					lrmi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (lrmi.isSelected())
								setTextDirection(ImRegion.TEXT_DIRECTION_LEFT_RIGHT, invoker);
						}
					});
					pm.add(lrmi);
					bg.add(lrmi);
					final JMenuItem bumi = new JRadioButtonMenuItem("Bottom-Up", ImRegion.TEXT_DIRECTION_BOTTOM_UP.equals(graphicsTextDirection));
					bumi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (bumi.isSelected())
								setTextDirection(ImRegion.TEXT_DIRECTION_BOTTOM_UP, invoker);
						}
					});
					pm.add(bumi);
					bg.add(bumi);
					final JMenuItem tdmi = new JRadioButtonMenuItem("Top-Down", ImRegion.TEXT_DIRECTION_TOP_DOWN.equals(graphicsTextDirection));
					tdmi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (tdmi.isSelected())
								setTextDirection(ImRegion.TEXT_DIRECTION_TOP_DOWN, invoker);
						}
					});
					pm.add(tdmi);
					bg.add(tdmi);
					final JMenuItem udmi = new JRadioButtonMenuItem("Right-Left & Upside-Down", ImRegion.TEXT_DIRECTION_RIGHT_LEFT_UPSIDE_DOWN.equals(graphicsTextDirection));
					udmi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (udmi.isSelected())
								setTextDirection(ImRegion.TEXT_DIRECTION_RIGHT_LEFT_UPSIDE_DOWN, invoker);
						}
					});
					pm.add(udmi);
					bg.add(udmi);
					pm.setToolTipText(this.tooltip);
					Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.GRAPHICS_TYPE);
					if (regionTypeColor != null) {
						pm.setOpaque(true);
						pm.setBackground(regionTypeColor);
					}
					return pm;
				}
				private void setTextDirection(String textDirection, ImDocumentMarkupPanel invoker) {
					invoker.beginAtomicAction(this.label);
					if (ImWord.TEXT_DIRECTION_LEFT_RIGHT.equals(textDirection))
						selGraphics.removeAttribute(ImWord.TEXT_DIRECTION_ATTRIBUTE);
					else selGraphics.setAttribute(ImWord.TEXT_DIRECTION_ATTRIBUTE, textDirection);
					invoker.endAtomicAction();
					invoker.validate();
					invoker.repaint();
				}
			});
			
			if (clickedGraphics.length == 1) {
				actions.add(new SelectionAction("copyGraphics", "Copy Graphics", "Copy the selected graphics to the system clipboard.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						extractGraphics(null, clickedGraphics[0], false, selGraphics, page, supplements, idmp);
						return false;
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.GRAPHICS_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
				actions.add(new SelectionAction("saveGraphics", "Save Graphics", "Save the selected graphics to disk.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						String graphicsFileName = getExportFileName(idmp.document, page, selGraphics);
						extractGraphics(graphicsFileName, clickedGraphics[0], false, selGraphics, page, supplements, idmp);
						return false;
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.GRAPHICS_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
			}
			
			if ((clickedFigures.length == 1) && (selImage == null)) {
				actions.add(new SelectionAction("copyImage", "Copy Image", "Copy the selected image to the system clipboard.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						extractImageOrGrid(null, clickedFigures[0], null, selGraphics, false, idmp);
						return false;
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.GRAPHICS_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
				actions.add(new SelectionAction("saveImage", "Save Image", "Save the selected image to disk.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						String imageFileName = getExportFileName(idmp.document, page, selImage);
						extractImageOrGrid(imageFileName, clickedFigures[0], null, selGraphics, false, idmp);
						return false;
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.GRAPHICS_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
			}
			
			final Figure[] contextFigures = ImSupplement.getFiguresIn(supplements, selGraphics.bounds);
			final Graphics[] contextGraphics = ImSupplement.getGraphicsIn(supplements, selGraphics.bounds);
			final ImWord[] contextWords = page.getWordsInside(selGraphics.bounds);
			if ((contextGraphics.length > 1) || ((contextWords.length + contextFigures.length) != 0)) {
				actions.add(new SelectionAction("copyGraphicsCustom", "Copy Graphics or Group ...", "Copy the selected graphics or group to the system clipboard, with custom options.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						extractGraphics(null, null, false, selGraphics, page, supplements, idmp);
						return false;
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.GRAPHICS_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
				actions.add(new SelectionAction("saveGraphicsCustom", "Save Graphics or Group ...", "Save the selected graphics or group to disk, with custom options.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						String graphicsFileName = getExportFileName(idmp.document, page, selGraphics);
						extractGraphics(graphicsFileName, null, false, selGraphics, page, supplements, idmp);
						return false;
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenuItem mi = super.getMenuItem(invoker);
						Color regionTypeColor = idmp.getLayoutObjectColor(ImRegion.GRAPHICS_TYPE);
						if (regionTypeColor != null) {
							mi.setOpaque(true);
							mi.setBackground(regionTypeColor);
						}
						return mi;
					}
				});
			}
		}
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	private static ImWord[] getLabelWords(ImWord[] words) {
		ArrayList wordList = new ArrayList();
		for (int w = 0; w < words.length; w++) {
			if (ImWord.TEXT_STREAM_TYPE_LABEL.equals(words[w].getTextStreamType()))
				wordList.add(words[w]);
		}
		return ((wordList.size() < words.length) ? ((ImWord[]) wordList.toArray(new ImWord[wordList.size()])) : words);
	}
	
	private boolean markIllustration(ImWord[] words, ImPage page, BoundingBox selectedBounds, ImRegion[] overlappingRegions, String type, ImDocumentMarkupPanel idmp) {
		
		//	shrink selection
		BoundingBox iogBounds;
		
		//	shrink to supplements (figures, graphics) and words in born-digital document
		if (idmp.documentBornDigital) {
			int iogLeft = selectedBounds.right;
			int iogRight = selectedBounds.left;
			int iogTop = selectedBounds.bottom;
			int iogBottom = selectedBounds.top;
			
			//	wrap around words
			for (int w = 0; w < words.length; w++) {
				iogLeft = Math.min(iogLeft, words[w].bounds.left);
				iogRight = Math.max(iogRight, words[w].bounds.right);
				iogTop = Math.min(iogTop, words[w].bounds.top);
				iogBottom = Math.max(iogBottom, words[w].bounds.bottom);
			}
			
			//	wrap around clipping area of figures and graphics
			ImSupplement[] pageSupplements = page.getSupplements();
			for (int s = 0; s < pageSupplements.length; s++) {
				BoundingBox psBounds;
				if (pageSupplements[s] instanceof Figure)
					psBounds = ((Figure) pageSupplements[s]).getClipBounds();
				else if (pageSupplements[s] instanceof Graphics)
					psBounds = ((Graphics) pageSupplements[s]).getClipBounds();
				else continue;
				//	only shrink for supplements to prevent white figure margins from inflating selection
				iogLeft = Math.min(iogLeft, Math.max(psBounds.left, selectedBounds.left));
				iogRight = Math.max(iogRight, Math.min(psBounds.right, selectedBounds.right));
				iogTop = Math.min(iogTop, Math.max(psBounds.top, selectedBounds.top));
				iogBottom = Math.max(iogBottom, Math.min(psBounds.bottom, selectedBounds.bottom));
			}
			
			//	anything selected at all (too much effort to check in advance)
			if ((iogRight <= iogLeft) || (iogBottom <= iogTop))
				iogBounds = null;
			
			//	determine bounding box
			else iogBounds = new BoundingBox(iogLeft, iogRight, iogTop, iogBottom);
		}
		
		//	shrink to populated area of page in scanned documents
		else {
			PageImage pi = page.getImage();
			AnalysisImage ai = Imaging.wrapImage(pi.image, null);
			ImagePartRectangle ipr = Imaging.getContentBox(ai);
			ImagePartRectangle selectedIpr = ipr.getSubRectangle(selectedBounds.left, selectedBounds.right, selectedBounds.top, selectedBounds.bottom);
			selectedIpr = Imaging.narrowLeftAndRight(selectedIpr);
			selectedIpr = Imaging.narrowTopAndBottom(selectedIpr);
			
			//	anything selected at all (too much effort to check in advance)
			if ((selectedIpr.getWidth() == 0) || (selectedIpr.getHeight() == 0) || (Imaging.computeAverageBrightness(selectedIpr) > 125))
				iogBounds = null;
			
			//	determine bounding box
			else iogBounds = new BoundingBox(selectedIpr.getLeftCol(), selectedIpr.getRightCol(), selectedIpr.getTopRow(), selectedIpr.getBottomRow());
		}
		
		//	anything selected at all (too much effort to check in advance)
		if (iogBounds == null) {
			DialogFactory.alert(("The selection appears to be completely empty and thus cannot be marked as " + (ImRegion.IMAGE_TYPE.equals(type) ? "an image" : "a vector based graphics") + "."), ("Cannot Mark " + (ImRegion.IMAGE_TYPE.equals(type) ? "Image" : "Graphics")), JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		//	handle words (if any)
		ArrayList labelWords = null;
		ArrayList captionWords = null;
		HashSet captionRegions = null;
		ImWord blockSplitWord = null;
		
		//	no words to handle at all, add substitute word to facilitate block split
		if (words.length == 0)
			blockSplitWord = new ImWord(page, iogBounds, "ILLUSTRATION");
		
		//	make words into 'label' text stream if document is born-digital
		else if (idmp.documentBornDigital) {
			ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_LABEL, null);
			ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
			labelWords = new ArrayList(Arrays.asList(words));
			words = new ImWord[0];
		}
		
		//	remove words if document is scanned (odds are most of them are artifacts), but retain ones marked as captions or labels
		else {
			ArrayList removeWordList = new ArrayList();
			captionRegions = new HashSet();
			HashSet nonCaptionRegions = new HashSet();
			for (int w = 0; w < words.length; w++) {
				if (ImWord.TEXT_STREAM_TYPE_CAPTION.equals(words[w].getTextStreamType())) {
					if (captionWords == null)
						captionWords = new ArrayList();
					captionWords.add(words[w]);
					for (int r = 0; r < overlappingRegions.length; r++) {
						if (!words[w].bounds.liesIn(overlappingRegions[r].bounds, false))
							continue;
						if (ImRegion.BLOCK_ANNOTATION_TYPE.equals(overlappingRegions[r].getType()) || ImRegion.PARAGRAPH_TYPE.equals(overlappingRegions[r].getType()) || ImRegion.LINE_ANNOTATION_TYPE.equals(overlappingRegions[r].getType()))
							captionRegions.add(overlappingRegions[r]);
					}
				}
				else if (ImWord.TEXT_STREAM_TYPE_LABEL.equals(words[w].getTextStreamType())) {
					if (labelWords == null)
						labelWords = new ArrayList();
					labelWords.add(words[w]);
					for (int r = 0; r < overlappingRegions.length; r++) {
						if (words[w].bounds.liesIn(overlappingRegions[r].bounds, false))
							nonCaptionRegions.add(overlappingRegions[r]);
					}
				}
				else {
					removeWordList.add(words[w]);
					for (int r = 0; r < overlappingRegions.length; r++) {
						if (words[w].bounds.liesIn(overlappingRegions[r].bounds, false))
							nonCaptionRegions.add(overlappingRegions[r]);
					}
				}
			}
			words = ((ImWord[]) removeWordList.toArray(new ImWord[removeWordList.size()]));
			
			//	retain only regions that belong exclusively to caption
			captionRegions.removeAll(nonCaptionRegions);
			
			//	dissolve remaining words out of text streams (helps prevent cycles on block splitting)
			ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_ARTIFACT, null);
			ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
			for (ImWord imw = words[words.length-1]; imw != null; imw = imw.getPreviousWord()) // going backwards saves us propagating changes of text stream ID
				imw.setNextWord(null);
		}
		
		//	clean up nested regions (except any adjacent illustrations, as we might be marking multiple parts)
		for (int r = 0; r < overlappingRegions.length; r++) {
			if (type.equals(overlappingRegions[r].getType()) && !iogBounds.includes(overlappingRegions[r].bounds, true))
				continue; // keep peripherally overlapping tiles of images marked in multiple parts (non-convex figures, first and foremost)
			if ((captionRegions != null) && captionRegions.contains(overlappingRegions[r]))
				continue; // keep regions marking structure of in-figure caption
			if (iogBounds.includes(overlappingRegions[r].bounds, false)) {
				page.removeRegion(overlappingRegions[r]);
				continue;
			}
			ImWord[] regionWords = overlappingRegions[r].getWords();
			if (regionWords.length == 0) {
				page.removeRegion(overlappingRegions[r]);
				continue;
			}
			if (ImRegion.BLOCK_ANNOTATION_TYPE.equals(overlappingRegions[r].getType())) {
				if (this.regionActions == null)
					page.removeRegion(overlappingRegions[r]);
				else this.regionActions.splitBlock(page, iogBounds); // also takes care of paragraphs and lines
			}
			else if (ImRegion.COLUMN_ANNOTATION_TYPE.equals(overlappingRegions[r].getType())) { /* leave columns in place */ }
			else if (ImRegion.REGION_ANNOTATION_TYPE.equals(overlappingRegions[r].getType())) { /* leave generic regions in place */ }
//			else if (ImRegion.PARAGRAPH_TYPE.equals(selectedRegions[r].getType())) { /* taken care of by block split */ }
//			else if (ImRegion.LINE_ANNOTATION_TYPE.equals(selectedRegions[r].getType())) { /* taken care of by block split */ }
			else page.removeRegion(overlappingRegions[r]);
		}
		
		/* re-get and clean up nested regions once again (only with center point containment this time, as we've done the splitting above):
		 * - some region removals trigger adding new regions via reactions, e.g. tables
		 * - split blocks will now be contained completely
		 */
		overlappingRegions = page.getRegionsInside(iogBounds, true);
		for (int r = 0; r < overlappingRegions.length; r++) {
			if (type.equals(overlappingRegions[r].getType()) && !iogBounds.includes(overlappingRegions[r].bounds, true))
				continue; // keep peripherally overlapping tiles of images marked in multiple parts (non-convex figures, first and foremost)
			if ((captionRegions != null) && captionRegions.contains(overlappingRegions[r]))
				continue; // keep regions marking structure of in-figure caption
			if (ImRegion.COLUMN_ANNOTATION_TYPE.equals(overlappingRegions[r].getType()))
				continue; // leave columns in place
			if (ImRegion.REGION_ANNOTATION_TYPE.equals(overlappingRegions[r].getType()))
				continue; // leave generic regions in place
			page.removeRegion(overlappingRegions[r]);
		}
		
		//	restore text streams preserved above
		if (labelWords != null) {
			ImWord[] lWords = ((ImWord[]) labelWords.toArray(new ImWord[labelWords.size()]));
			ImUtils.makeStream(lWords, ImWord.TEXT_STREAM_TYPE_LABEL, null);
			ImUtils.orderStream(lWords, ImUtils.leftRightTopDownOrder);
		}
		if (captionWords != null) {
			ImWord[] cWords = ((ImWord[]) captionWords.toArray(new ImWord[captionWords.size()]));
			ImUtils.makeStream(cWords, ImWord.TEXT_STREAM_TYPE_CAPTION, null);
			ImUtils.orderStream(cWords, ImUtils.leftRightTopDownOrder);
		}
		
		//	remove any artificial word we might have added for block splitting
		if (blockSplitWord != null)
			page.removeWord(blockSplitWord, true);
		
		//	cut to-remove words out of text streams again (in case block splitting merged them back in some way), and clean up
		else if ((words != null) && (words.length != 0)) {
			ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_ARTIFACT, null);
			ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
			for (ImWord imw = words[words.length-1]; imw != null; imw = imw.getPreviousWord()) // going backwards saves us propagating changes of text stream ID
				imw.setNextWord(null);
			for (int w = 0; w < words.length; w++)
				page.removeWord(words[w], true);
		}
		
		//	mark image or graphics
		ImRegion iog = new ImRegion(page, iogBounds, type);
		idmp.setRegionsPainted(type, true);
		
		//	consult document style regarding where captions might be located
		ImDocumentStyle docStyle = ImDocumentStyle.getStyleFor(idmp.document);
		if (docStyle == null)
			docStyle = new ImDocumentStyle(new PropertiesData(new Properties()));
		final ImDocumentStyle docLayout = docStyle.getImSubset("layout");
		boolean captionAbove = docLayout.getBooleanProperty("caption.aboveFigure", false);
		boolean captionBelow = docLayout.getBooleanProperty("caption.belowFigure", true);
		boolean captionBeside = docLayout.getBooleanProperty("caption.besideFigure", true);
		boolean captionInside = docLayout.getBooleanProperty("caption.insideFigure", false);
		
		//	get potential captions
		ImAnnotation[] captionAnnots = ImUtils.findCaptions(iog, captionAbove, captionBelow, captionBeside, captionInside, true);
		
		//	try setting attributes in unassigned captions first
		for (int a = 0; a < captionAnnots.length; a++) {
			if (captionAnnots[a].hasAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE) || captionAnnots[a].hasAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE))
				continue;
			captionAnnots[a].setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + iog.pageId));
			captionAnnots[a].setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, iog.bounds.toString());
			return true;
		}
		
		//	set attributes in any caption (happens if user corrects, for instance)
		if (captionAnnots.length != 0) {
			captionAnnots[0].setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + iog.pageId));
			captionAnnots[0].setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, iog.bounds.toString());
		}
		
		//	finally ...
		return true;
	}
	
	private static class ImageSelection implements Transferable {
		private Image image;
		ImageSelection(Image image) {
			this.image = image;
		}
		public DataFlavor[] getTransferDataFlavors() {
			DataFlavor[] dfs = {DataFlavor.imageFlavor};
			return dfs;
		}
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return DataFlavor.imageFlavor.equals(flavor);
		}
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (DataFlavor.imageFlavor.equals(flavor))
				return image;
			else throw new UnsupportedFlavorException(flavor);
		}
	}
	
	private static String getExportFileName(ImDocument doc, ImPage page, ImRegion exportRegion) {
		
		//	start with document name
		String docName = ((String) doc.getAttribute(DOCUMENT_NAME_ATTRIBUTE, doc.docId));
		if (docName.toLowerCase().indexOf(".pdf") != -1) // TODO observe more common file name suffixes, if only as we add more decoders ...
			docName = docName.substring(0, (docName.toLowerCase().indexOf(".pdf") + ".pdf".length()));
		
		//	find caption
		String regionBoundsStr = exportRegion.bounds.toString();
		ImAnnotation[] captions = doc.getAnnotations(ImAnnotation.CAPTION_TYPE, page.pageId);
		String captionStart = null;
		for (int c = 0; c < captions.length; c++)
			if (regionBoundsStr.equals(captions[c].getAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE))) {
				captionStart = getCaptionStart(captions[c]);
				break;
			}
		
		//	no caption start to work with, default to generic naming
		if (captionStart == null)
			return (docName + "." + exportRegion.getType() + "@" + page.pageId + "." + exportRegion.bounds.toString());
		
		//	use (normalized) caption start
		captionStart = captionStart.replaceAll("\\s+", ""); // eliminate spaces
		captionStart = captionStart.replaceAll("\\.", ""); // eliminate dots
		captionStart = captionStart.replaceAll("[\\-\\u00AD\\u2010\\u2011\\u2012\\u2013\\u2014\\u2015\\u2212]+", "-"); // normalize dashes
		captionStart = captionStart.replaceAll("[^a-zA-Z0-9\\-\\_]+", "_"); // replace anything that might cause trouble with an underscore
		return (docName + "." + captionStart);
	}
	
	private static String getCaptionStart(ImAnnotation caption) {
		ImWord captionStartEnd = null;
		char indexType = ((char) 0);
		boolean lastWasIndex = false;
		for (ImWord imw = caption.getFirstWord().getNextWord(); imw != null; imw = imw.getNextWord()) {
			String imwString = imw.getString();
			if ((imwString == null) || (imwString.trim().length() == 0))
				continue;
			
			//	Arabic index number
			if (imwString.matches("[0-9]+(\\s*[a-z])?")) {
				if (indexType == 0)
					indexType = 'A';
				else if (indexType != 'A')
					break;
				captionStartEnd = imw;
				lastWasIndex = true;
			}
			
			//	lower case Roman index number
			else if (imwString.matches("[ivxl]+")) {
				if (indexType == 0)
					indexType = 'r';
				else if ((indexType == 'l') && (imwString.length() == 1)) { /* need to allow 'a-i', etc. ... */ }
				else if (indexType != 'r')
					break;
				captionStartEnd = imw;
				lastWasIndex = true;
			}
			
			//	upper case Roman index number
			else if (imwString.matches("[IVXL]+")) {
				if (indexType == 0)
					indexType = 'R';
				else if ((indexType == 'L') && (imwString.length() == 1)) { /* need to allow 'A-I', etc. ... */ }
				else if (indexType != 'R')
					break;
				captionStartEnd = imw;
				lastWasIndex = true;
			}
			
			//	lower case index letter
			else if (imwString.matches("[a-z]")) {
				if (indexType == 0)
					indexType = 'l';
				else if (indexType != 'l')
					break;
				captionStartEnd = imw;
				lastWasIndex = true;
			}
			
			//	upper case index letter
			else if (imwString.matches("[A-Z]")) {
				if (indexType == 0)
					indexType = 'L';
				else if (indexType != 'L')
					break;
				captionStartEnd = imw;
				lastWasIndex = true;
			}
			
			//	enumeration separator or range marker
			else if (imwString.equals(",") || imwString.equals("&") || (";and;und;et;y;".indexOf(";" + imwString.toLowerCase() + ";") != -1) || imwString.matches("[\\-\\u00AD\\u2010-\\u2015\\u2212]+")) {
				if (!lastWasIndex) // no enumeration or range open, we're done here
					break;
				lastWasIndex = false;
			}
			
			//	ignore dots
			else if (!".".equals(imwString))
				break;
		}
		
		//	finally ...
		return ImUtils.getString(caption.getFirstWord(), ((captionStartEnd == null) ? caption.getLastWord() : captionStartEnd), true);
	}
	
	private File chooseExportFile(final String fileName, final ExportParameterPanel paramPanel, boolean forGraphics) {
		final JFileChooser fc = this.getFileChooser();
		
		if (paramPanel != null) {
			paramPanel.setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, paramPanel.getBackground()));
			JPanel paramPanelTray = new JPanel(new BorderLayout(), true);
			paramPanelTray.add(paramPanel, BorderLayout.SOUTH);
			fc.setAccessory(paramPanelTray);
		}
		
		fc.addChoosableFileFilter(pngFileFilter);
		if (forGraphics) {
			fc.addChoosableFileFilter(svgFileFilter);
			fc.setFileFilter(svgFileFilter);
			fc.setSelectedFile(new File(fc.getCurrentDirectory(), fileName + ".svg"));
		}
		else {
			fc.setFileFilter(pngFileFilter);
			fc.setSelectedFile(new File(fc.getCurrentDirectory(), fileName + ".png"));
		}
		
		if (forGraphics && (paramPanel != null))
			paramPanel.dpiSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					int dpi = paramPanel.getDpi();
					String selFileName;
					try {
						BasicFileChooserUI fileChooserUI = (BasicFileChooserUI) fc.getUI();
						selFileName = fileChooserUI.getFileName();
					}
					catch (Exception e) {
						File selFile = fc.getSelectedFile();
						selFileName = ((selFile == null) ? fileName : selFile.getName());
					}
//					System.out.println("Selected file is " + selFileName);
					fc.setFileFilter((dpi < 0) ? svgFileFilter : pngFileFilter);
					if (selFileName.toLowerCase().endsWith(".png") || selFileName.toLowerCase().endsWith(".svg"))
						selFileName = selFileName.substring(0, selFileName.lastIndexOf('.'));
					fc.setSelectedFile(new File(fc.getCurrentDirectory(), (selFileName + ((dpi < 0) ? ".svg" : ".png"))));
				}
			});
		
		File exportFile = null;
		if (fc.showSaveDialog(DialogFactory.getTopWindow()) == JFileChooser.APPROVE_OPTION)
			exportFile = fc.getSelectedFile();
		
		return exportFile;
	}
	
	private JFileChooser getFileChooser() {
		if (this.fileChooser == null) {
			this.fileChooser = new JFileChooser();
			this.fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			this.fileChooser.setAcceptAllFileFilterUsed(false);
		}
		else {
			this.fileChooser.resetChoosableFileFilters();
			this.fileChooser.setAccessory(null);
		}
		return this.fileChooser;
	}
	private JFileChooser fileChooser = null;
	
	//	JPEG, PNG, BMP, WEBMP, GIF
	private static FileFilter svgFileFilter = new FileFilter() {
		public String getDescription() {
			return "Scalable Vector Graphics (SVG)";
		}
		public boolean accept(File file) {
			return (file.isDirectory() || file.getName().toLowerCase().endsWith(".svg"));
		}
	};
	private static FileFilter pngFileFilter = new FileFilter() {
		public String getDescription() {
			return "Portable Network Graphics (PNG)";
		}
		public boolean accept(File file) {
			return (file.isDirectory() || file.getName().toLowerCase().endsWith(".png"));
		}
	};
	private static FileFilter bmpFileFilter = new FileFilter() {
		public String getDescription() {
			return "Bitmap Images (BMP)";
		}
		public boolean accept(File file) {
			return (file.isDirectory() || file.getName().toLowerCase().endsWith(".bmp"));
		}
	};
	private static FileFilter jpgFileFilter = new FileFilter() {
		public String getDescription() {
			return "JPEG Compressed Images (JPG)";
		}
		public boolean accept(File file) {
			return (file.isDirectory() || file.getName().toLowerCase().endsWith(".jpg"));
		}
	};
	private static FileFilter gifFileFilter = new FileFilter() {
		public String getDescription() {
			return "Graphics Interchange Format (GIF)";
		}
		public boolean accept(File file) {
			return (file.isDirectory() || file.getName().toLowerCase().endsWith(".gif"));
		}
	};
	
	private static class ExportParameterPanel extends JPanel {
		final JComboBox dpiSelector;
		private JCheckBox exportFigures;
		private JCheckBox exportGraphics;
		private JCheckBox exportWords;
//		ExportParameterPanel(Figure[] figures, Graphics[] graphics, ImWord[] words, boolean forGraphics, boolean forFileChooser) {
//			this(getLength(figures), getLength(graphics), getLength(words), getDpiSet(figures), forGraphics, forFileChooser);
//		}
		ExportParameterPanel(int figureCount, int graphicsCount, int wordCount, SortedSet figureDpiSet, boolean forGraphics, boolean forFileChooser) {
			super(new GridLayout(0, 1), true);
			
			if (forFileChooser)
				this.add(new JLabel(("<HTML><B>" + (forGraphics ? "Graphics" : "Image") + " Export Options</B></HTML>"), JLabel.CENTER));
			
			Integer maxFigureDpi = (figureDpiSet.isEmpty() ? null : ((Integer) figureDpiSet.last()));
			TreeSet dpiSet = new TreeSet();
			if (forGraphics)
				dpiSet.add(new SelectableDPI(-1, ("Copy as SVG" + ((figureCount == 0) ? "" : " (excludes figures)"))));
			for (Iterator dpiit = figureDpiSet.iterator(); dpiit.hasNext();) {
				Integer dpi = ((Integer) dpiit.next());
				dpiSet.add(new SelectableDPI(dpi, ("render at " + dpi + " DPI")));
			}
			if (!figureDpiSet.contains(new Integer(300)))
				dpiSet.add(new SelectableDPI(300, ("render at 300 DPI")));
			if (!figureDpiSet.contains(new Integer(200)))
				dpiSet.add(new SelectableDPI(200, ("render at 200 DPI")));
			if (!figureDpiSet.contains(new Integer(150)))
				dpiSet.add(new SelectableDPI(150, ("render at 150 DPI")));
			SelectableDPI[] dpiOptions = ((SelectableDPI[]) dpiSet.toArray(new SelectableDPI[dpiSet.size()]));
			this.dpiSelector = new JComboBox(dpiOptions);
			if (maxFigureDpi == null)
				this.dpiSelector.setSelectedItem(new SelectableDPI(-1, ""));
			else if (maxFigureDpi.intValue() <= 600)
				this.dpiSelector.setSelectedItem(new SelectableDPI(maxFigureDpi.intValue(), ""));
			else this.dpiSelector.setSelectedItem(new SelectableDPI(300, ""));
			
			JPanel dpiPanel = new JPanel(new BorderLayout(), true);
			dpiPanel.add(new JLabel("Export Resolution ", JLabel.LEFT), BorderLayout.WEST);
			dpiPanel.add(this.dpiSelector, BorderLayout.CENTER);
			this.add(dpiPanel);
			
			if (forGraphics) {
				this.exportFigures = new JCheckBox("Include Bitmap Images", false);
				this.exportGraphics = null;
				if (figureCount != 0) {
					this.add(this.exportFigures);
					this.dpiSelector.addItemListener(new ItemListener() {
						public void itemStateChanged(ItemEvent ie) {
							SelectableDPI selDpi = ((SelectableDPI) dpiSelector.getSelectedItem());
							exportFigures.setEnabled(selDpi.dpi != -1);
						}
					});
				}
			}
			else {
				this.exportFigures = null;
				this.exportGraphics = new JCheckBox("Include Line Graphics", (graphicsCount != 0));
				if (graphicsCount != 0)
					this.add(this.exportGraphics);
			}
			
			this.exportWords = new JCheckBox("Include Label Text", (wordCount != 0));
			if (wordCount != 0)
				this.add(this.exportWords);
		}
		int getDpi() {
			return ((SelectableDPI) this.dpiSelector.getSelectedItem()).dpi;
		}
		boolean includeFigures() {
			return ((this.getDpi() > 0) && ((this.exportFigures == null) || this.exportFigures.isSelected()));
		}
		boolean includeGraphics() {
			return ((this.exportGraphics == null) || this.exportGraphics.isSelected());
		}
		boolean includeWords() {
			return ((this.exportWords == null) || this.exportWords.isSelected());
		}
//		private static int getLength(ImObject[] objects) {
//			return ((objects == null) ? 0 : objects.length);
//		}
//		private static SortedSet getDpiSet(Figure[] figures) {
//			TreeSet dpiSet = new TreeSet();
//			if (figures != null) {
//				for (int f = 0; f < figures.length; f++)
//					dpiSet.add(new Integer(figures[f].getDpi()));
//			}
//			return dpiSet;
//		}
	}
	
	private static class SelectableDPI implements Comparable {
		final int dpi;
		final String label;
		SelectableDPI(int dpi, String label) {
			this.dpi = dpi;
			this.label = label;
		}
		public String toString() {
			return this.label;
		}
		public boolean equals(Object obj) {
			if (obj instanceof SelectableDPI)
				return (this.compareTo(obj) == 0);
			else if (obj instanceof Number)
				return (this.dpi == ((Number) obj).intValue());
			else return false;
		}
		public int compareTo(Object obj) {
			return (this.dpi - ((SelectableDPI) obj).dpi);
		}
	}
	
	private static CharSequence rotateSvg(CharSequence svg, String textDirection) {
		if (ImRegion.TEXT_DIRECTION_BOTTOM_UP.equals(textDirection))
			return spliceTranslateRotate(svg, ImObjectTransformer.CLOCKWISE_ROTATION);
		else if (ImRegion.TEXT_DIRECTION_TOP_DOWN.equals(textDirection))
			return spliceTranslateRotate(svg, ImObjectTransformer.COUNTER_CLOCKWISE_ROTATION);
		else if (ImRegion.TEXT_DIRECTION_RIGHT_LEFT_UPSIDE_DOWN.equals(textDirection))
			return spliceTranslateRotate(svg, (ImObjectTransformer.CLOCKWISE_ROTATION + ImObjectTransformer.CLOCKWISE_ROTATION));
		else return svg;
	}
	private static final Pattern svgStartTagPattern = Pattern.compile("\\<svg[^\\>]*\\>");
	private static CharSequence spliceTranslateRotate(CharSequence svg, int rotation) {
		StringBuffer svgBuf = ((svg instanceof StringBuffer) ? ((StringBuffer) svg) : new StringBuffer(svg));
		Matcher sstm = svgStartTagPattern.matcher(svgBuf);
		if (!sstm.find())
			return svg;
		String sst = sstm.group();
		
		//	translate to keep top left corner for +/-90° rotations
		String translate = "";
		if ((rotation == ImObjectTransformer.CLOCKWISE_ROTATION) || (rotation == ImObjectTransformer.COUNTER_CLOCKWISE_ROTATION)) try {
			if (sst.indexOf("width=\"") == -1)
				return svg;
			String swStr = sst.substring(sst.indexOf("width=\"") + "width=\"".length()).trim();
			if (swStr.indexOf("\"") == -1)
				return svg;
			swStr = swStr.substring(0, swStr.indexOf("\"")).trim();
			if (swStr.indexOf("px") != -1)
				swStr = swStr.substring(0, swStr.indexOf("px")).trim();
			int svgWidth = Integer.parseInt(swStr);
			if (sst.indexOf("height=\"") == -1)
				return svg;
			String shStr = sst.substring(sst.indexOf("height=\"") + "height=\"".length()).trim();
			if (shStr.indexOf("\"") == -1)
				return svg;
			shStr = shStr.substring(0, shStr.indexOf("\"")).trim();
			if (shStr.indexOf("px") != -1)
				shStr = shStr.substring(0, shStr.indexOf("px")).trim();
			int svgHeight = Integer.parseInt(shStr);
			translate = "translate(" + ((svgHeight + 1 - svgWidth) / 2) + "," + ((svgWidth + 1 - svgHeight) / 2) + ") ";
		}
		catch (NumberFormatException nfe) {
			return svg;
		}
		
		//	splice in transformation ...
		if (sst.indexOf("transform=\"") == -1) {
			int spliceOffset = (sstm.start() + sst.indexOf(">"));
			svgBuf.insert(spliceOffset, (" transform=\"" + translate + "rotate(" + rotation + ")\""));
		}
		
		//	... or amend existing transformation
		else {
			int spliceOffset = (sstm.start() + sst.indexOf("transform=\"") + "transform=\"".length());
			svgBuf.insert(spliceOffset, (translate + "rotate(" + rotation + ") "));
		}
		
		//	finally ...
		return svgBuf;
	}
	
	private void extractGraphics(final String graphicsFileName, final Graphics clickedGraphics, final boolean excludeFigures, final ImRegion selGraphics, final ImPage page, final ImSupplement[] supplements, final ImDocumentMarkupPanel idmp) {
		runImageRenderingJob(new ImageRenderingJob() {
			private int graphicsDpi;
			private Graphics[] graphicsParts;
			private ImWord[] graphicsWords;
			private BufferedImage graphicsImage;
			private CharSequence graphicsSvg;
			private File graphicsFile;
			boolean render(ProgressMonitor pm) {
				this.graphicsImage = ImIllustrationUtils.renderImage(selGraphics, this, idmp.document, pm);
				if ((this.graphicsImage == null) && (this.graphicsParts != null)) {
					if (this.graphicsDpi == -1) {
						this.graphicsSvg = ImIllustrationUtils.renderSvg(this.graphicsParts, this.graphicsWords, page);
						if (this.graphicsSvg != null)
							this.graphicsSvg = rotateSvg(this.graphicsSvg, ((String) selGraphics.getAttribute(ImRegion.TEXT_DIRECTION_ATTRIBUTE)));
					}
					else {
						this.graphicsImage = ImIllustrationUtils.renderCompositeImage(null, this.graphicsDpi, this.graphicsParts, this.graphicsWords, page, pm);
						if (this.graphicsImage != null)
							this.graphicsImage = ImIllustrationUtils.rotateImageToUpright(this.graphicsImage, ((String) selGraphics.getAttribute(ImRegion.TEXT_DIRECTION_ATTRIBUTE)));
					}
				}
				return ((this.graphicsImage != null) || (this.graphicsSvg != null));
			}
			void handleResult(ProgressMonitor pm) {
				if (this.graphicsFile == null) {
					if (this.graphicsImage == null)
						ImUtils.copy(new StringSelection(this.graphicsSvg.toString()));
					else ImUtils.copy(new ImageSelection(this.graphicsImage));
				}
				else try {
					OutputStream out = new BufferedOutputStream(new FileOutputStream(this.graphicsFile));
					if (this.graphicsImage == null) {
						Reader svgIn = new CharSequenceReader(this.graphicsSvg);
						Writer svgOut = new OutputStreamWriter(out, "UTF-8");
						char[] svgBuffer = new char[1024];
						for (int r; (r = svgIn.read(svgBuffer, 0, svgBuffer.length)) != -1;)
							svgOut.write(svgBuffer, 0, r);
						svgIn.close();
						svgOut.flush();
						svgOut.close();
					}
					else {
						ImageIO.write(this.graphicsImage, "PNG", out);
						out.flush();
						out.close();
					}
				}
				catch (IOException ioe) {
					ioe.printStackTrace(System.out);
					DialogFactory.alert(("The selected image grid could not be saved:\r\n" + ioe.getMessage()), "Error Saving Image Grid", JOptionPane.ERROR_MESSAGE);
				}
			}
			public RenderingOptions getRenderingOptions(int figureCount, int graphicsCount, int wordCount, SortedSet dpis) {
				ExportParameterPanel exportParamPanel = new ExportParameterPanel(((excludeFigures && (clickedGraphics != null)) ? 0 : figureCount), graphicsCount, wordCount, dpis, true, (graphicsFileName != null));
				if (graphicsFileName == null) {
					int choice = DialogFactory.confirm(exportParamPanel, "Graphics Export Options", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
					if (choice != JOptionPane.OK_OPTION)
						return null;
					this.graphicsFile = null;
				}
				else {
					this.graphicsFile = chooseExportFile(graphicsFileName, exportParamPanel, true);
					if (this.graphicsFile == null)
						return null;
				}
				if (clickedGraphics != null) {
					this.graphicsDpi = exportParamPanel.getDpi();
					this.graphicsParts = new Graphics[1];
					this.graphicsParts[0] = clickedGraphics;
					this.graphicsWords = (exportParamPanel.includeWords() ? page.getWordsInside(clickedGraphics.getBounds()) : null);
					return null;
				}
				else if (exportParamPanel.getDpi() == -1) {
					this.graphicsDpi = exportParamPanel.getDpi();
					this.graphicsParts = ImSupplement.getGraphicsIn(supplements, selGraphics.bounds);
					this.graphicsWords = (exportParamPanel.includeWords() ? page.getWordsInside(selGraphics.bounds) : null);
					return null;
				}
				else return new RenderingOptions(exportParamPanel.includeFigures(), true, exportParamPanel.includeWords(), exportParamPanel.getDpi());
			}
		}, idmp);
	}
	
	private void extractImageOrGrid(final String imageFileName, final Figure clickedFigure, final Scan clickedFigureScan, final ImRegion selImage, final boolean extractGrid, final ImDocumentMarkupPanel idmp) {
		runImageRenderingJob(new ImageRenderingJob() {
			private BufferedImage image;
			private File imageFile;
			boolean render(ProgressMonitor pm) {
				if ((clickedFigure != null) || (clickedFigureScan != null)) {
					if (imageFileName != null) {
						this.imageFile = chooseExportFile(imageFileName, null, false);
						if (this.imageFile == null)
							return false;
					}
					try {
						if (clickedFigure != null)
							this.image = ImageIO.read(clickedFigure.getInputStream());
						else if (clickedFigureScan != null) {
							BufferedImage scan = ImageIO.read(clickedFigureScan.getInputStream());
							this.image = scan.getSubimage(selImage.bounds.left, selImage.bounds.top, selImage.bounds.getWidth(), selImage.bounds.getHeight());
						}
					}
					catch (Exception e) {
						System.out.println("Exception extracting figure image: " + e.getMessage());
						e.printStackTrace(System.out);
						DialogFactory.alert(("The selected image could not be extracted:\r\n" + e.getMessage()), "Error Extracting Image", JOptionPane.ERROR_MESSAGE);
					}
					if (image != null)
						this.image = ImIllustrationUtils.rotateImageToUpright(this.image, ((String) selImage.getAttribute(ImRegion.TEXT_DIRECTION_ATTRIBUTE)));
				}
				else if (extractGrid)
					this.image = ImIllustrationUtils.renderImageGrid(selImage, this, idmp.document, pm);
				else this.image = ImIllustrationUtils.renderImage(selImage, this, idmp.document, pm);
				return (this.image != null);
			}
			void handleResult(ProgressMonitor pm) {
				if (this.imageFile == null)
					ImUtils.copy(new ImageSelection(this.image));
				else try {
					OutputStream out = new BufferedOutputStream(new FileOutputStream(this.imageFile));
					ImageIO.write(this.image, "PNG", out);
					out.flush();
					out.close();
				}
				catch (IOException ioe) {
					ioe.printStackTrace(System.out);
					DialogFactory.alert(("The selected image grid could not be saved:\r\n" + ioe.getMessage()), "Error Saving Image Grid", JOptionPane.ERROR_MESSAGE);
				}
			}
			public RenderingOptions getRenderingOptions(int figureCount, int graphicsCount, int wordCount, SortedSet dpis) {
				ExportParameterPanel exportParamPanel = new ExportParameterPanel(figureCount, graphicsCount, wordCount, dpis, false, (imageFileName != null));
				if (imageFileName == null) {
					int choice = DialogFactory.confirm(exportParamPanel, "Image Export Options", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
					if (choice != JOptionPane.OK_OPTION)
						return null;
					this.imageFile = null;
				}
				else {
					this.imageFile = chooseExportFile(imageFileName, exportParamPanel, false);
					if (this.imageFile == null)
						return null;
				}
				return new RenderingOptions(true, exportParamPanel.includeGraphics(), exportParamPanel.includeWords(), exportParamPanel.getDpi());
			}
		}, idmp);
	}
	
	private static abstract class ImageRenderingJob implements RenderingOptions.Provider {
		void execute(ProgressMonitor pm) {
			if (this.render(pm))
				this.handleResult(pm);
		}
		abstract boolean render(ProgressMonitor pm);
		abstract void handleResult(ProgressMonitor pm);
	}
	
	private static void runImageRenderingJob(final ImageRenderingJob irj, ImDocumentMarkupPanel idmp) {
		
		//	get progress monitor
		final ProgressMonitor pm = idmp.getProgressMonitor("Rendering Image, Please Wait", "", false, false);
		final ProgressMonitorWindow pmw = ((pm instanceof ProgressMonitorWindow) ? ((ProgressMonitorWindow) pm) : null);
		
		//	apply document processor, in separate thread
		Thread irjThread = new Thread() {
			public void run() {
				try {
					
					//	wait for splash screen progress monitor to come up (we must not reach the dispose() line before the splash screen even comes up)
					while ((pmw != null) && !pmw.getWindow().isVisible()) try {
						Thread.sleep(10);
					} catch (InterruptedException ie) {}
					
					//	apply image markup tool
					irj.execute(pm);
				}
				
				//	catch whatever might happen
				catch (Throwable t) {
					t.printStackTrace(System.out);
					DialogFactory.alert(("Error rendering image:\n" + t.getMessage()), "Error Rendering Image", JOptionPane.ERROR_MESSAGE, null);
				}
				
				//	dispose splash screen progress monitor
				finally {
					if (pmw != null)
						pmw.close();
				}
			}
		};
		irjThread.start();
		
		//	open splash screen progress monitor (this waits)
		if (pmw != null)
			pmw.popUp(true);
	}
	
	private ImRegion getImageAt(ImPage page, Point point) {
		ImRegion[] imwPageTables = page.getRegions(ImRegion.IMAGE_TYPE);
		if (imwPageTables.length == 0)
			return null;
		for (int t = 0; t < imwPageTables.length; t++) {
			if (imwPageTables[t].bounds.includes(point))
				return imwPageTables[t];
		}
		return null;
	}
	
	private boolean connectImagesLeftRight(ImRegion startImage, ImWord secondWord) {
		ImRegion secondWordImage = this.getImageAt(secondWord.getPage(), secondWord.bounds.getCenterPoint());
		if (!ImWord.TEXT_STREAM_TYPE_LABEL.equals(secondWord.getTextStreamType()) || (secondWordImage == null)) {
			DialogFactory.alert(("'" + secondWord.getString() + "' is not an image label"), "Cannot Connect Images", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return this.connectImagesLeftRight(startImage, secondWordImage);
	}
	private boolean connectImagesLeftRight(ImRegion startImage, ImPage secondPage, Point secondPoint) {
		ImRegion secondImage = this.getImageAt(secondPage, secondPoint);
		if (secondImage == null)
			return false;
		return this.connectImagesLeftRight(startImage, secondImage);
	}
	private boolean connectImagesLeftRight(ImRegion startImage, ImRegion secondImage) {
		if ((startImage.pageId == secondImage.pageId) && startImage.bounds.equals(secondImage.bounds)) {
			DialogFactory.alert("An image cannot extend itself", "Cannot Connect Images", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		secondImage.setAttribute("rightContinuesIn", (startImage.pageId + "." + startImage.bounds));
		startImage.setAttribute("rightContinuesFrom", (secondImage.pageId + "." + secondImage.bounds));
		DialogFactory.alert("Images merged successfully; you can copy the whole grid of connected images via 'Copy Image Grid '", "Images Connected", JOptionPane.INFORMATION_MESSAGE);
		return true;
	}
	
	private boolean disconnectImagesLeftRight(ImDocument doc, ImRegion rightImage) {
		ImRegion leftImage = ImUtils.getRegionForId(doc, ImRegion.IMAGE_TYPE, ((String) rightImage.getAttribute("rightContinuesFrom")));
		if (leftImage != null)
			leftImage.removeAttribute("rightContinuesIn");
		rightImage.removeAttribute("rightContinuesFrom");
		DialogFactory.alert("Images dissected successfully", "Images Dissected", JOptionPane.INFORMATION_MESSAGE);
		return true;
	}
	
	private boolean connectImagesTopBottom(ImRegion startImage, ImWord secondWord) {
		ImRegion secondWordImage = getImageAt(secondWord.getPage(), secondWord.bounds.getCenterPoint());
		if (!ImWord.TEXT_STREAM_TYPE_LABEL.equals(secondWord.getTextStreamType()) || (secondWordImage == null)) {
			DialogFactory.alert(("'" + secondWord.getString() + "' is not an image label"), "Cannot Connect Images", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return this.connectImagesTopBottom(startImage, secondWordImage);
	}
	private boolean connectImagesTopBottom(ImRegion startImage, ImPage secondPage, Point secondPoint) {
		ImRegion secondImage = getImageAt(secondPage, secondPoint);
		if (secondImage == null)
			return false;
		return this.connectImagesTopBottom(startImage, secondImage);
	}
	private boolean connectImagesTopBottom(ImRegion startImage, ImRegion secondImage) {
		if ((startImage.pageId == secondImage.pageId) && startImage.bounds.equals(secondImage.bounds)) {
			DialogFactory.alert("An image cannot extend itself", "Cannot Connect Images", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		secondImage.setAttribute("bottomContinuesIn", (startImage.pageId + "." + startImage.bounds));
		startImage.setAttribute("bottomContinuesFrom", (secondImage.pageId + "." + secondImage.bounds));
		DialogFactory.alert("Images merged successfully; you can copy the whole grid of connected images via 'Copy Image Grid '", "Images Connected", JOptionPane.INFORMATION_MESSAGE);
		return true;
	}
	
	private boolean disconnectImagesTopBottom(ImDocument doc, ImRegion bottomImage) {
		ImRegion topImage = ImUtils.getRegionForId(doc, ImRegion.IMAGE_TYPE, ((String) bottomImage.getAttribute("bottomContinuesFrom")));
		if (topImage != null)
			topImage.removeAttribute("bottomContinuesIn");
		bottomImage.removeAttribute("bottomContinuesFrom");
		DialogFactory.alert("Images dissected successfully", "Images Dissected", JOptionPane.INFORMATION_MESSAGE);
		return true;
	}
}
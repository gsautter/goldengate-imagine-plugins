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
package de.uka.ipd.idaho.im.imagine.plugins.fonts;

import de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;

/**
 * This plugin provides functionality for manual editing of font mapping in
 * born-digital PDFs.
 * 
 * @author sautter
 */
public class FontEditorProvider extends AbstractImageMarkupToolProvider {
	
	/* TODO to facilitate this:
	 * - enable IMF archives to store fonts
	 *   - store in 'Fonts.csv'
	 *   - columns: fontId (id of font object from PDF), charId (char ID in font), mappedChar (Unicode char that char ID is mapped to, in HEX), charImage (PNG image of rendered char, in HEX)
	 * - add addFont(), getFont(), and getFonts() methods to ImDocument
	 *   ==> also further indicator of born-digital documents, and a safe one
	 * - add font ID as attribute to words
	 * - cache fonts across pages in PDF parsing (checked ==> already happening)
	 * - load page fonts in separate step, before page rendering
	 *   ==> avoids loading same font multiple times in parallel page rendering
	 *   ==> allows for parallel font decoding
	 * - put fonts in IMF archive from PDF objects map (using 'instanceof PdfFont')
	 * - add rendered char images to fonts while loading if image comparison decoding involved
	 * 
	 * - offer editing individual fonts in dedicated dialog:
	 *   - checkboxes for bold and italics
	 *   - table of characters with
	 *     - char ID
	 *     - char image thumbnail icon
	 *     - text field for mapped Unicode char (with link to symbol table)
	 *       ==> make Symbol table separate class of its own to facilitate this use
	 * - if font changed in editor
	 *   - run through document and replace respective characters
	 *   - update font in IMF file
	 * - however, DO NOT offer editing basic built-in fonts
	 */
	
	/** public zero-argument constructor for class loading */
	public FontEditorProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
	}
}
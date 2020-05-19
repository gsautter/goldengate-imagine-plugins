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
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider;
import de.uka.ipd.idaho.im.util.ImDocumentIO;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.TwoClickSelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * This class provides basic actions for editing logical text streams.
 * 
 * @author sautter
 */
public class TextStreamActionProvider extends AbstractSelectionActionProvider implements ImageMarkupToolProvider {
	private static final String WORD_JOINER_IMT_NAME = "WordJoiner";
	private static final String WORD_SPLITTER_IMT_NAME = "WordSplitter";
	private static final String TEXT_FLOW_BREAK_CHECKER_IMT_NAME = "TextFlowBreakChecker";
	
	private ImageMarkupTool wordJoiner = new WordJoiner();
	private ImageMarkupTool wordSplitter = new WordSplitter();
	private ImageMarkupTool textFlowBreakChecker = new TextFlowBreakChecker();
	
	/** public zero-argument constructor for class loading */
	public TextStreamActionProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Text Stream Actions";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(final ImWord start, final ImWord end, final ImDocumentMarkupPanel idmp) {
		
		//	get selection actions
		LinkedList actions = new LinkedList();
		
		//	test if paragraphs to merge
		boolean paragraphsToMerge = false;
		if (start.getTextStreamId().equals(end.getTextStreamId()) && (start != end))
			for (ImWord imw = start.getNextWord(); imw != null; imw = imw.getNextWord()) {
				if (imw.getPreviousWord().getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END) {
					paragraphsToMerge = true;
					break;
				}
				if (imw == end)
					break;
			}
		
		//	we're not offering text stream editing if text streams are not visualized, only word relations
		if (!idmp.areTextStreamsPainted()) {
			if (start == end) {
				if (start.getNextWord() != null)
					actions.add(new SelectionAction("streamWordRelation", "Set Next Word Relation", ("Set Relation between '" + start.getString() + "' and its Successor '" + start.getNextWord().getString() + "'")) {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return false;
						}
						public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
							JMenu pm = new JMenu("Set Next Word Relation");
							ButtonGroup bg = new ButtonGroup();
							final JMenuItem smi = new JRadioButtonMenuItem("Separate Word", (start.getNextRelation() == ImWord.NEXT_RELATION_SEPARATE));
							smi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									if (smi.isSelected()) {
										setNextRelation(ImWord.NEXT_RELATION_SEPARATE, invoker);
									}
								}
							});
							pm.add(smi);
							bg.add(smi);
							final JMenuItem pmi = new JRadioButtonMenuItem("Separate Word with Pararaph Break After", (start.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END));
							pmi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									if (pmi.isSelected())
										setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END, invoker);
								}
							});
							pm.add(pmi);
							bg.add(pmi);
							final JMenuItem hmi = new JRadioButtonMenuItem("First Part of Hyphenated Word", (start.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED));
							hmi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									if (hmi.isSelected())
										setNextRelation(ImWord.NEXT_RELATION_HYPHENATED, invoker);
								}
							});
							pm.add(hmi);
							bg.add(hmi);
							final JMenuItem cmi = new JRadioButtonMenuItem(((start.bounds.left <= start.getNextWord().bounds.left) ? "First Part of Split Word" : "First Part of Line-Broken Word"), (start.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE));
							cmi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									if (cmi.isSelected())
										setNextRelation(ImWord.NEXT_RELATION_CONTINUE, invoker);
								}
							});
							pm.add(cmi);
							bg.add(cmi);
							return pm;
						}
						private void setNextRelation(char nextRelation, ImDocumentMarkupPanel invoker) {
							invoker.beginAtomicAction("Set Next Word Relation");
							start.setNextRelation(nextRelation);
							invoker.endAtomicAction();
							invoker.validate();
							invoker.repaint();
						}
					});
				if (start.getPreviousWord() != null)
					actions.add(new SelectionAction("streamWordRelation", "Set Previous Word Relation", ("Set Relation between '" + start.getString() + "' and its Predecessor '" + start.getPreviousWord().getString() + "'")) {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return false;
						}
						public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
							JMenu pm = new JMenu("Set Previous Word Relation");
							ButtonGroup bg = new ButtonGroup();
							final JMenuItem smi = new JRadioButtonMenuItem("Separate Word", (start.getPreviousRelation() == ImWord.NEXT_RELATION_SEPARATE));
							smi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									if (smi.isSelected())
										setPreviousRelation(ImWord.NEXT_RELATION_SEPARATE, invoker);
								}
							});
							pm.add(smi);
							bg.add(smi);
							final JMenuItem pmi = new JRadioButtonMenuItem("Separate Word with Pararaph Break Before", (start.getPreviousRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END));
							pmi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									if (pmi.isSelected())
										setPreviousRelation(ImWord.NEXT_RELATION_PARAGRAPH_END, invoker);
								}
							});
							pm.add(pmi);
							bg.add(pmi);
							final JMenuItem hmi = new JRadioButtonMenuItem("Second Part of Hyphenated Word", (start.getPreviousRelation() == ImWord.NEXT_RELATION_HYPHENATED));
							hmi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									if (hmi.isSelected())
										setPreviousRelation(ImWord.NEXT_RELATION_HYPHENATED, invoker);
								}
							});
							pm.add(hmi);
							bg.add(hmi);
							final JMenuItem cmi = new JRadioButtonMenuItem(((start.bounds.left >= start.getPreviousWord().bounds.left) ? "Second Part of Split Word" : "Second Part of Line-Broken Word"), (start.getPreviousRelation() == ImWord.NEXT_RELATION_CONTINUE));
							cmi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									if (cmi.isSelected())
										setPreviousRelation(ImWord.NEXT_RELATION_CONTINUE, invoker);
								}
							});
							pm.add(cmi);
							bg.add(cmi);
							return pm;
						}
						private void setPreviousRelation(char previousRelation, ImDocumentMarkupPanel invoker) {
							invoker.beginAtomicAction("Set Previous Word Relation");
							start.getPreviousWord().setNextRelation(previousRelation);
							invoker.endAtomicAction();
							invoker.validate();
							invoker.repaint();
						}
					});
			}
			else if (start.getNextWord() == end)
				actions.add(new SelectionAction("streamWordRelation", "Set Word Relation", ("Set word relation between '" + start.getString() + "' and '" + end.getString() + "'")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return false;
					}
					public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
						JMenu pm = new JMenu("Set Word Relation");
						ButtonGroup bg = new ButtonGroup();
						final JMenuItem smi = new JRadioButtonMenuItem("Two Words", (start.getNextRelation() == ImWord.NEXT_RELATION_SEPARATE));
						smi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (smi.isSelected())
									setNextRelation(ImWord.NEXT_RELATION_SEPARATE, invoker);
							}
						});
						pm.add(smi);
						bg.add(smi);
						final JMenuItem pmi = new JRadioButtonMenuItem("Two Words with Pararaph Break", (start.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END));
						pmi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (pmi.isSelected())
									setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END, invoker);
							}
						});
						pm.add(pmi);
						bg.add(pmi);
						final JMenuItem hmi = new JRadioButtonMenuItem("Hyphenated Word", (start.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED));
						hmi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (hmi.isSelected())
									setNextRelation(ImWord.NEXT_RELATION_HYPHENATED, invoker);
							}
						});
						pm.add(hmi);
						bg.add(hmi);
						final JMenuItem cmi = new JRadioButtonMenuItem(((start.bounds.left <= start.getNextWord().bounds.left) ? "Same Word" : "Line-Broken Word"), (start.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE));
						cmi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (cmi.isSelected())
									setNextRelation(ImWord.NEXT_RELATION_CONTINUE, invoker);
							}
						});
						pm.add(cmi);
						bg.add(cmi);
						return pm;
					}
					private void setNextRelation(char nextRelation, ImDocumentMarkupPanel invoker) {
						invoker.beginAtomicAction("Set Word Relation");
						start.setNextRelation(nextRelation);
						invoker.endAtomicAction();
						invoker.validate();
						invoker.repaint();
					}
				});
			if ((start != end) && start.getTextStreamId().equals(end.getTextStreamId()) && (start.pageId == end.pageId) && (start.bounds.top < end.centerY) && (end.centerY < start.bounds.bottom) && (end.getTextStreamPos() <= (start.getTextStreamPos() + 20))) {
				Tokenizer tokenizer = ((Tokenizer) idmp.document.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER));
				if (this.isJoinableSelection(start, end, tokenizer)) {
					StringBuffer before = new StringBuffer();
					StringBuffer after = new StringBuffer("'");
					for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
						if (imw == end) {
							if (start.getNextWord() == end)
								before.append(" and ");
							else before.append(", and ");
							before.append("'" + imw.getString() + "'");
							after.append(imw.getString() + "'");
							break;
						}
						else {
							if (imw != start)
								before.append(", ");
							before.append("'" + imw.getString() + "'");
							after.append(imw.getString());
						}
					}
					actions.add(new SelectionAction("streamJoinWords", "Join Words", ("Logically join " + before.toString() + " into " + after.toString() + ".")) {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return joinWords(start, end, null);
						}
					});
					actions.add(new SelectionAction("streamJoinWords", "Join All", ("Logically join all occurrences of " + before.toString() + " into " + after.toString() + ".")) {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return joinWords(start, end, idmp.document);
						}
					});
				}
			}
			
			//	merge paragraphs
			if (paragraphsToMerge)
				actions.add(new SelectionAction("streamMergeParas", "Merge Paragraphs", "Merge selected paragraphs into one.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
							if (imw == end)
								break;
							if (imw.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
								imw.setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
						}
						return true;
					}
				});
			
			//	copy selected text
			if (start.getTextStreamId().equals(end.getTextStreamId()))
				actions.add(new SelectionAction("copyWordsTXT", "Copy Text", "Copy the selected words to the system clipboard.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						ImUtils.copy(new StringSelection(ImUtils.getString(start, end, false)));
						return false;
					}
				});
			
			//	finally ...
			return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
		}
		
		//	single word selection
		if (start == end) {
			if (start.getNextWord() != null)
				actions.add(new SelectionAction("streamWordRelation", "Set Next Word Relation", ("Set Relation between '" + start.getString() + "' and its Successor '" + start.getNextWord().getString() + "'")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return false;
					}
					public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
						JMenu pm = new JMenu("Set Next Word Relation");
						ButtonGroup bg = new ButtonGroup();
						final JMenuItem smi = new JRadioButtonMenuItem("Separate Word", (start.getNextRelation() == ImWord.NEXT_RELATION_SEPARATE));
						smi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (smi.isSelected())
									setNextRelation(ImWord.NEXT_RELATION_SEPARATE, invoker);
							}
						});
						pm.add(smi);
						bg.add(smi);
						final JMenuItem pmi = new JRadioButtonMenuItem("Separate Word with Pararaph Break After", (start.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END));
						pmi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (pmi.isSelected())
									setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END, invoker);
							}
						});
						pm.add(pmi);
						bg.add(pmi);
						final JMenuItem hmi = new JRadioButtonMenuItem("First Part of Hyphenated Word", (start.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED));
						hmi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (hmi.isSelected())
									setNextRelation(ImWord.NEXT_RELATION_HYPHENATED, invoker);
							}
						});
						pm.add(hmi);
						bg.add(hmi);
						final JMenuItem cmi = new JRadioButtonMenuItem(((start.bounds.left <= start.getNextWord().bounds.left) ? "First Part of Split Word" : "First Part of Line-Broken Word"), (start.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE));
						cmi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (cmi.isSelected())
									setNextRelation(ImWord.NEXT_RELATION_CONTINUE, invoker);
							}
						});
						pm.add(cmi);
						bg.add(cmi);
						return pm;
					}
					private void setNextRelation(char nextRelation, ImDocumentMarkupPanel invoker) {
						invoker.beginAtomicAction("Set Next Word Relation");
						start.setNextRelation(nextRelation);
						invoker.endAtomicAction();
						invoker.validate();
						invoker.repaint();
					}
				});
			if (start.getPreviousWord() != null)
				actions.add(new SelectionAction("streamWordRelation", "Set Previous Word Relation", ("Set Relation between '" + start.getString() + "' and its Predecessor '" + start.getPreviousWord().getString() + "'")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return false;
					}
					public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
						JMenu pm = new JMenu("Set Previous Word Relation");
						ButtonGroup bg = new ButtonGroup();
						final JMenuItem smi = new JRadioButtonMenuItem("Separate Word", (start.getPreviousRelation() == ImWord.NEXT_RELATION_SEPARATE));
						smi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (smi.isSelected())
									setPreviousRelation(ImWord.NEXT_RELATION_SEPARATE, invoker);
							}
						});
						pm.add(smi);
						bg.add(smi);
						final JMenuItem pmi = new JRadioButtonMenuItem("Separate Word with Pararaph Break Before", (start.getPreviousRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END));
						pmi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (pmi.isSelected())
									setPreviousRelation(ImWord.NEXT_RELATION_PARAGRAPH_END, invoker);
							}
						});
						pm.add(pmi);
						bg.add(pmi);
						final JMenuItem hmi = new JRadioButtonMenuItem("Second Part of Hyphenated Word", (start.getPreviousRelation() == ImWord.NEXT_RELATION_HYPHENATED));
						hmi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (hmi.isSelected())
									setPreviousRelation(ImWord.NEXT_RELATION_HYPHENATED, invoker);
							}
						});
						pm.add(hmi);
						bg.add(hmi);
						final JMenuItem cmi = new JRadioButtonMenuItem(((start.bounds.left >= start.getPreviousWord().bounds.left) ? "Second Part of Split Word" : "Second Part of Line-Broken Word"), (start.getPreviousRelation() == ImWord.NEXT_RELATION_CONTINUE));
						cmi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (cmi.isSelected())
									setPreviousRelation(ImWord.NEXT_RELATION_CONTINUE, invoker);
							}
						});
						pm.add(cmi);
						bg.add(cmi);
						return pm;
					}
					private void setPreviousRelation(char previousRelation, ImDocumentMarkupPanel invoker) {
						invoker.beginAtomicAction("Set Previous Word Relation");
						start.getPreviousWord().setNextRelation(previousRelation);
						invoker.endAtomicAction();
						invoker.validate();
						invoker.repaint();
					}
				});
			if (start.getPreviousWord() != null)
				actions.add(new SelectionAction("streamCutBefore", "Cut Stream Before", ("Cut text stream before '" + start.getString() + "'")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						start.setPreviousWord(null);
						return true;
					}
				});
			if (start.getNextWord() != null)
				actions.add(new SelectionAction("streamCutAfter", "Cut Stream After", ("Cut text stream after '" + start.getString() + "'")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						start.setNextWord(null);
						return true;
					}
				});
			actions.add(new TwoClickSelectionAction("streamMergeBackward", "Click Predeccessor", "Mark words, and set its predeccessor by clicking another word") {
				public boolean performAction(ImWord secondWord) {
//					if (start.getTextStreamId().equals(secondWord.getTextStreamId()) && ((secondWord.pageId > start.pageId) || ((secondWord.pageId == start.pageId) && (secondWord.getTextStreamPos() >= start.getTextStreamPos())))) {
					if (start.getTextStreamId().equals(secondWord.getTextStreamId()) && (secondWord.getTextStreamPos() >= start.getTextStreamPos())) {
						DialogFactory.alert(("'" + secondWord.getString() + "' cannot be the predecessor of '" + start.getString() + "'\nThey belong to the same logical text stream,\nand '" + start.getString() + "' is a treansitive predecessor of '" + secondWord.getString() + "'"), "Cannot Set Predecessor", JOptionPane.ERROR_MESSAGE);
						return false;
					}
					else if (start.getPreviousWord() == secondWord)
						return false;
					else {
						start.setPreviousWord(secondWord);
						return true;
					}
				}
				public ImWord getFirstWord() {
					return start;
				}
				public String getActiveLabel() {
					return ("Click predecessor of '" + start.getString() + "'");
				}
			});
			actions.add(new TwoClickSelectionAction("streamMergeForward", "Click Successor", "Mark words, and set its successor by clicking another word") {
				public boolean performAction(ImWord secondWord) {
//					if (start.getTextStreamId().equals(secondWord.getTextStreamId()) && ((secondWord.pageId < start.pageId) || ((secondWord.pageId == start.pageId) && (secondWord.getTextStreamPos() <= start.getTextStreamPos())))) {
					if (start.getTextStreamId().equals(secondWord.getTextStreamId()) && (secondWord.getTextStreamPos() <= start.getTextStreamPos())) {
						DialogFactory.alert(("'" + secondWord.getString() + "' cannot be the successor of '" + start.getString() + "'\nThey belong to the same logical text stream,\nand '" + start.getString() + "' is a treansitive successor of '" + secondWord.getString() + "'"), "Cannot Set Successor", JOptionPane.ERROR_MESSAGE);
						return false;
					}
					else if (start.getNextWord() == secondWord)
						return false;
					else {
						start.setNextWord(secondWord);
						return true;
					}
				}
				public ImWord getFirstWord() {
					return start;
				}
				public String getActiveLabel() {
					return ("Click successor of '" + start.getString() + "'");
				}
			});
		}
		
		//	two words, different streams
		else if (!start.getTextStreamId().equals(end.getTextStreamId())) {
			actions.add(new SelectionAction("streamMergeForward", "Make Successor", ("Make '" + end.getString() + "' successor of '" + start.getString() + "'")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					System.out.println("Setting successor of " + start + " to " + end);
					ImWord oldStartNext = start.getNextWord();
					System.out.println("Old start next is " + oldStartNext);
					start.setNextWord(end);
					if (oldStartNext == null)
						return true;
					ImWord imw = end;
					while (imw != null) {
						if (imw.getNextWord() == null) {
							oldStartNext.setPreviousWord(imw);
							System.out.println("Setting predecessor of " + oldStartNext + " to " + imw + " at " + imw.getLocalID());
							break;
						}
						imw = imw.getNextWord();
					}
					return true;
				}
			});
		}
		
		//	two words, same stream
		else if (start.getNextWord() == end) {
			actions.add(new SelectionAction("streamWordRelation", "Set Word Relation", ("Set word relation between '" + start.getString() + "' and '" + end.getString() + "'")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Set Word Relation");
					ButtonGroup bg = new ButtonGroup();
					final JMenuItem smi = new JRadioButtonMenuItem("Two Words", (start.getNextRelation() == ImWord.NEXT_RELATION_SEPARATE));
					smi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (smi.isSelected())
								setNextRelation(ImWord.NEXT_RELATION_SEPARATE, invoker);
						}
					});
					pm.add(smi);
					bg.add(smi);
					final JMenuItem pmi = new JRadioButtonMenuItem("Two Words with Pararaph Break", (start.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END));
					pmi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (pmi.isSelected())
								setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END, invoker);
						}
					});
					pm.add(pmi);
					bg.add(pmi);
					final JMenuItem hmi = new JRadioButtonMenuItem("Hyphenated Word", (start.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED));
					hmi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (hmi.isSelected())
								setNextRelation(ImWord.NEXT_RELATION_HYPHENATED, invoker);
						}
					});
					pm.add(hmi);
					bg.add(hmi);
					final JMenuItem cmi = new JRadioButtonMenuItem(((start.bounds.left <= end.bounds.left) ? "Same Word" : "Line-Broken Word"), (start.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE));
					cmi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (cmi.isSelected())
								setNextRelation(ImWord.NEXT_RELATION_CONTINUE, invoker);
						}
					});
					pm.add(cmi);
					bg.add(cmi);
					return pm;
				}
				private void setNextRelation(char nextRelation, ImDocumentMarkupPanel invoker) {
					invoker.beginAtomicAction("Set Word Relation");
					start.setNextRelation(nextRelation);
					invoker.endAtomicAction();
					invoker.validate();
					invoker.repaint();
				}
			});
			actions.add(new SelectionAction("streamCut", "Cut Stream", ("Cut text stream between '" + start.getString() + "' and '" + end.getString() + "'")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					start.setNextWord(null);
					return true;
				}
			});
		}
		
		//	multiple words, same stream
		if ((start != end) && start.getTextStreamId().equals(end.getTextStreamId()) && (start.pageId == end.pageId) && (start.bounds.top < end.centerY) && (end.centerY < start.bounds.bottom) && (end.getTextStreamPos() <= (start.getTextStreamPos() + 20))) {
			Tokenizer tokenizer = ((Tokenizer) idmp.document.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER));
			if (this.isJoinableSelection(start, end, tokenizer)) {
				StringBuffer before = new StringBuffer();
				StringBuffer after = new StringBuffer("'");
				for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
					if (imw == end) {
						if (start.getNextWord() == end)
							before.append(" and ");
						else before.append(", and ");
						before.append("'" + imw.getString() + "'");
						after.append(imw.getString() + "'");
						break;
					}
					else {
						if (imw != start)
							before.append(", ");
						before.append("'" + imw.getString() + "'");
						after.append(imw.getString());
					}
				}
				actions.add(new SelectionAction("streamJoinWords", "Join Words", ("Logically join " + before.toString() + " into " + after.toString() + ".")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return joinWords(start, end, null);
					}
				});
				actions.add(new SelectionAction("streamJoinWords", "Join All", ("Logically join all occurrences of " + before.toString() + " into " + after.toString() + ".")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return joinWords(start, end, idmp.document);
					}
				});
			}
		}
		
		//	merge paragraphs
		if (paragraphsToMerge)
			actions.add(new SelectionAction("streamMergeParas", "Merge Paragraphs", "Merge selected paragraphs into one.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
						if (imw == end)
							break;
						if (imw.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
							imw.setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
					}
					return true;
				}
			});
		
		//	one or more words from same stream
		if (start.getTextStreamId().equals(end.getTextStreamId())) {
			actions.add(new SelectionAction("streamCutOut", "Make Stream", "Make selected words a separate text stream.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					ImWord oldStartPrev = start.getPreviousWord();
					ImWord oldEndNext = end.getNextWord();
					if ((oldStartPrev != null) && (oldEndNext != null))
						oldStartPrev.setNextWord(oldEndNext);
					else {
						start.setPreviousWord(null);
						end.setNextWord(null);
					}
					return true;
				}
			});
			actions.add(new SelectionAction("streamSetType", "Set Text Stream Type", ("Set Type of Text Stream '" + start.getString() + "' Belongs to")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Set Text Stream Type");
					ButtonGroup bg = new ButtonGroup();
					String[] textStreamTypes = idmp.getTextStreamTypes();
					for (int t = 0; t < textStreamTypes.length; t++) {
						final String textStreamType = textStreamTypes[t];
						final JMenuItem smi = new JRadioButtonMenuItem(textStreamType, textStreamType.equals(start.getTextStreamType()));
						smi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								invoker.beginAtomicAction("Set Text Stream Type");
								if (smi.isSelected())
									start.setTextStreamType(textStreamType);
								invoker.endAtomicAction();
								invoker.validate();
								invoker.repaint();
							}
						});
						Color textStreamTypeColor = idmp.getTextStreamTypeColor(textStreamTypes[t]);
						if (textStreamTypeColor != null)
							smi.setBackground(textStreamTypeColor);
						pm.add(smi);
						bg.add(smi);
					}
					return pm;
				}
			});
			actions.add(new SelectionAction("copyWordsTXT", "Copy Text", "Copy the selected words to the system clipboard.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					ImUtils.copy(new StringSelection(ImUtils.getString(start, end, false)));
					return false;
				}
			});
		}
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, final ImPage page, final ImDocumentMarkupPanel idmp) {
		
		//	we're not offering text stream editing if text streams are not visualized
		if (!idmp.areTextStreamsPainted())
			return null;
		
		//	mark selection
		BoundingBox selectedBox = new BoundingBox(Math.min(start.x, end.x), Math.max(start.x, end.x), Math.min(start.y, end.y), Math.max(start.y, end.y));
		
		//	prepare collecting actions
		LinkedList actions = new LinkedList();
		
		//	get selected words
		final ImWord[] selectedWords = page.getWordsInside(selectedBox);
		if (selectedWords.length == 0) {
			
			//	get selected and context regions
			ImRegion[] pageRegions = page.getRegions();
			ImRegion contextParagraph = null;
			ImRegion contextBlock = null;
			for (int r = 0; r < pageRegions.length; r++) {
				if (!idmp.areRegionsPainted(pageRegions[r].getType()))
					continue;
				if (pageRegions[r].bounds.includes(selectedBox, true)) {
					if (ImRegion.PARAGRAPH_TYPE.equals(pageRegions[r].getType()))
						contextParagraph = pageRegions[r];
					else if (ImRegion.BLOCK_ANNOTATION_TYPE.equals(pageRegions[r].getType()))
						contextBlock = pageRegions[r];
				}
			}
			
			//	add ordering options for paragraph and block if displaying
			if (contextParagraph != null) {
				final ImRegion paragraph = contextParagraph;
				actions.add(new SelectionAction("streamOrderPara", "Order Paragraph Words", "Order paragraphs words in a text stream left to right and top to bottom.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return orderStream(paragraph.getWords(), page);
					}
				});
			}
			if (contextBlock != null) {
				final ImRegion block = contextBlock;
				actions.add(new SelectionAction("streamOrderBlock", "Order Block Words", "Order block words in a text stream left to right and top to bottom.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return orderStream(block.getWords(), page);
					}
				});
			}
			
			//	little else to do without words being selected
			return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
		}
		
		//	order words
		Arrays.sort(selectedWords, ImUtils.textStreamOrder);
		
		//	check if we have a single continuous stream
		boolean singleContinuousStream = true;
		for (int w = 1; w < selectedWords.length; w++) {
			
			//	not continuous
			if (selectedWords[w].getPreviousWord() != selectedWords[w-1]) {
				singleContinuousStream = false;
				break;
			}
			
			//	above predecessor
			if (selectedWords[w].centerY < selectedWords[w-1].bounds.top) {
				singleContinuousStream = false;
				break;
			}
			
			//	same line as predecessor, and to the left
			if ((selectedWords[w].centerY < selectedWords[w-1].bounds.bottom) && (selectedWords[w].centerX < selectedWords[w-1].bounds.right)) {
				singleContinuousStream = false;
				break;
			}
		}
		
		//	if we have a single continuous selection, we can handle it like a word selection
		if (singleContinuousStream)
			actions.addAll(Arrays.asList(this.getActions(selectedWords[0], selectedWords[selectedWords.length - 1], idmp)));
		
		//	generically make selected words a separate text stream
		actions.add(new SelectionAction("streamCutOut", "Make Stream", "Make selected words a separate text stream.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				ImUtils.makeStream(selectedWords, null, null);
				return true;
			}
		});
		
		//	order selected words into a text stream
		actions.add(new SelectionAction("streamOrder", "Order Stream", "Order selected words in a text stream left to right and top to bottom.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				return orderStream(selectedWords, page);
			}
		});
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	private boolean isJoinableSelection(ImWord start, ImWord end, Tokenizer tokenizer) {
		StringBuffer joinResult = ((tokenizer == null) ? null : new StringBuffer());
		boolean gotNonContinue = false;
		for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
			if ((imw.centerY < start.bounds.top) || (start.bounds.bottom < imw.centerY))
				return false;
			if (joinResult != null)
				joinResult.append(imw.getString());
			if (imw == end)
				break;
			if (imw.getNextRelation() != ImWord.NEXT_RELATION_CONTINUE)
				gotNonContinue = true;
		}
		return (gotNonContinue && ((tokenizer == null) || (tokenizer.tokenize(joinResult).size() == 1)));
	}
	
	private boolean joinWords(ImWord start, ImWord end, ImDocument docForAll) {
		boolean modified = false;
		
		//	find all occurrences of arguments, and recurse
		if (docForAll != null) {
			ImWord[] tshs = docForAll.getTextStreamHeads();
			for (int h = 0; h < tshs.length; h++)
				for (ImWord imw = tshs[h]; imw != null; imw = imw.getNextWord()) {
					if (imw == start)
						continue; // we do that in the end
					if (!start.getString().equals(imw.getString()))
						continue;
					ImWord matchPos = imw.getNextWord();
					ImWord matchEnd = null;
					ImWord compPos = start.getNextWord();
					while ((matchPos != null) && (compPos != null)) {
						if (!compPos.getString().equals(matchPos.getString()))
							break;
						if (compPos == end) {
							matchEnd = matchPos;
							break;
						}
						matchPos = matchPos.getNextWord();
						compPos = compPos.getNextWord();
					}
					if ((matchEnd != null) && (this.isJoinableSelection(imw, matchEnd, null))) {
						modified = (this.joinWords(imw, matchEnd, null) || modified);
						imw = matchEnd;
					}
				}
			modified = (this.joinWords(start, end, null) || modified);
		}
		
		//	join argument words
		else for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
			if (imw == end)
				break;
			if (imw.getNextRelation() != ImWord.NEXT_RELATION_CONTINUE) {
				imw.setNextRelation(ImWord.NEXT_RELATION_CONTINUE);
				modified = true;
			}
		}
		
		//	indicate changes
		return modified;
	}
	
	/* TODO put this text orientation aware ordering in ImUtils, need to be used in:
	 * - document structure detection
	 * - paragraph merging and splitting
	 * - block merging and splitting
	 * - block paragraph revisiting
	 * 
	 * OR BETTER TODO add function for manually flipping box selections, most likely in region actions, to facilitate manual cleanup right after decoding
	 */
	
	/* TODO Prevent in-line separator dashes from getting in way of markup (especially pattern matching and lexicon lookups):
- split tailing dashes off words unless successor
  - follows after text flow break (potential hyphenation) ...
  - ... or is "and" or "or" (in any target language)
    ==> centrally provide respective dictionaries, most likely in StringUtils
- also split if dash-tailed word lower case and successor capitalized ...
- ... unless coming in from capitalized joined word sequence
==> should get reference group figure dashes (tend to attach to species) out of FAT's way
==> centralize to WordAnalysis
==> call from PDF decoder after establishing text streams
==> provide "Tools > Check Tokenization" to retro-apply functionality
==> use rendering extent based measurements (centralize, see previous mail) for splitting
	 */
	
	private static boolean orderStream(ImWord[] words, ImPage page) {
		
		//	anything to do at all?
		if (words.length < 2)
			return false;
		
		//	collect writing directions
		HashSet textDirections = new HashSet();
		for (int w = 0; w < words.length; w++)
			textDirections.add(words[w].getAttribute(ImWord.TEXT_DIRECTION_ATTRIBUTE, ImWord.TEXT_DIRECTION_LEFT_RIGHT));
		
		//	check writing directions
		if (textDirections.size() > 1)
			return false; // TODO figure out how to handle mixed orientations
		String textDirection = (textDirections.isEmpty() ? ImWord.TEXT_DIRECTION_LEFT_RIGHT : ((String) textDirections.iterator().next()));
		
		//	the default case
		if (ImWord.TEXT_DIRECTION_LEFT_RIGHT.equals(textDirection)) {
			ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
			return true;
		}
		
		//	use proxy words flipped to left-right for other writing directions
		ImWord[] lrWords = new ImWord[words.length];
		HashMap lrWordsToWords = new HashMap();
		for (int w = 0; w < words.length; w++) {
			lrWords[w] = getLeftRightWord(words[w], textDirection, page);
			lrWordsToWords.put(lrWords[w], words[w]);
		}
		
		//	order proxy word stream
		ImUtils.sortLeftRightTopDown(lrWords);
		
		//	collect argument words in sorted proxy order
		for (int w = 0; w < lrWords.length; w++)
			words[w] = ((ImWord) lrWordsToWords.get(lrWords[w]));
		
		//	order stream with inert comparator (we have our desired order already)
		ImUtils.orderStream(words, inertOrder);
		return true;
	}
	
	private static ImWord getLeftRightWord(ImWord word, String textDirection, ImPage page) {
		if (ImWord.TEXT_DIRECTION_BOTTOM_UP.equals(textDirection)) {
			return new ImWord(page.getDocument(), page.pageId, new BoundingBox(
				(page.bounds.bottom - word.bounds.bottom),
				(page.bounds.bottom - word.bounds.top),
				word.bounds.left,
				word.bounds.right
			), word.getString());
		}
		else if (ImWord.TEXT_DIRECTION_TOP_DOWN.equals(textDirection)) {
			return new ImWord(page.getDocument(), page.pageId, new BoundingBox(
				word.bounds.top,
				word.bounds.bottom,
				(page.bounds.right - word.bounds.right),
				(page.bounds.right - word.bounds.left)
			), word.getString());
		}
		else return word; // never gonna happen, but Java don't know
	}
	
	private static final Comparator inertOrder = new Comparator() {
		public int compare(Object obj1, Object obj2) {
			return 0;
		}
	};
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getEditMenuItemNames()
	 */
	public String[] getEditMenuItemNames() {
		return new String[0];
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getToolsMenuItemNames()
	 */
	public String[] getToolsMenuItemNames() {
		String[] tmins = {WORD_JOINER_IMT_NAME, WORD_SPLITTER_IMT_NAME, TEXT_FLOW_BREAK_CHECKER_IMT_NAME};
		return tmins;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		if (WORD_JOINER_IMT_NAME.equals(name))
			return this.wordJoiner;
		else if (WORD_SPLITTER_IMT_NAME.equals(name))
			return this.wordSplitter;
		else if (TEXT_FLOW_BREAK_CHECKER_IMT_NAME.equals(name))
			return this.textFlowBreakChecker;
		else return null;
	}
	
	private static class WordJoiner implements ImageMarkupTool {
		public String getLabel() {
			return "Join Shattered Words";
		}
		public String getTooltip() {
			return "Analyze line-internal word gaps, and join words shattered into multiple pieces due to font decoding errors - USE WITH CARE";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	we only process documents as a whole
			if (annot != null)
				return;
			
			//	little use processing scanned documents
			if (!idmp.documentBornDigital)
				return;
			
			//	get lines
			ArrayList docLines = new ArrayList();
			ImPage[] pages = doc.getPages();
			for (int p = 0; p < pages.length; p++) {
				ImRegion[] pageLines = pages[p].getRegions(ImagingConstants.LINE_ANNOTATION_TYPE);
				Arrays.sort(pageLines, ImUtils.topDownOrder);
				docLines.addAll(Arrays.asList(pageLines));
			}
			ImRegion[] lines = ((ImRegion[]) docLines.toArray(new ImRegion[docLines.size()]));
			
			//	get tokenizer
			Tokenizer tokenizer = ((Tokenizer) doc.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.NO_INNER_PUNCTUATION_TOKENIZER));
			
			//	process lines
			joinWords(lines, tokenizer, pm);
		}
	}
	
	private static class WordSplitter implements ImageMarkupTool {
		public String getLabel() {
			return "Split Conflated Words";
		}
		public String getTooltip() {
			return "Analyze line-internal word gaps, and split words conflated into one due to very narrow spaces - USE WITH CARE";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	we only process documents as a whole
			if (annot != null)
				return;
			
			//	little use processing scanned documents
			if (!idmp.documentBornDigital)
				return;
			
			//	get lines
			ArrayList docLines = new ArrayList();
			ImPage[] pages = doc.getPages();
			for (int p = 0; p < pages.length; p++) {
				ImRegion[] pageLines = pages[p].getRegions(ImagingConstants.LINE_ANNOTATION_TYPE);
				Arrays.sort(pageLines, ImUtils.topDownOrder);
				docLines.addAll(Arrays.asList(pageLines));
			}
			ImRegion[] lines = ((ImRegion[]) docLines.toArray(new ImRegion[docLines.size()]));
			
			//	get tokenizer
			Tokenizer tokenizer = ((Tokenizer) doc.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.NO_INNER_PUNCTUATION_TOKENIZER));
			
			//	process lines
			splitWords(lines, tokenizer, pm);
		}
	}
	
	private static class TextFlowBreakChecker implements ImageMarkupTool {
		public String getLabel() {
			return "Check Text Flow Breaks";
		}
		public String getTooltip() {
			return "Check word relations at text flow breaks (line, column, and page breaks) for hyphenations missed due to font decoding or page blocking errors";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	we only process documents as a whole
			if (annot != null)
				return;
			
			//	get text stream heads
			ImWord[] textStreamHeads = doc.getTextStreamHeads();
			
			//	get tokenizer
			Tokenizer tokenizer = ((Tokenizer) doc.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.NO_INNER_PUNCTUATION_TOKENIZER));
			
			//	process lines
			checkTextFlowBreaks(textStreamHeads, tokenizer, pm);
		}
	}
	
	private static abstract class LineData {
		ImWord[] words;
		int lineHeight;
		int fontSize;
		LineData(ImWord[] words, int lineHeight, int fontSize) {
			this.words = words;
			this.lineHeight = lineHeight;
			this.fontSize = fontSize;
		}
	}
	
	private static final boolean DEBUG_WORD_MERGING = false;
	
	private static class WordJoiningLineData extends LineData {
		int maxWordGapJump;
		int minWordGap;
		int maxNonWordGap;
		int avgWordGap;
		int minSpaceWordGap;
		int maxNonSpaceWordGap;
		int mergeMinWordGap = -1;
		WordJoiningLineData(ImWord[] words, int lineHeight, int fontSize) {
			super(words, lineHeight, fontSize);
		}
	}
	
	private static void joinWords(ImRegion[] lines, Tokenizer tokenizer, ProgressMonitor pm) {
		WordJoiningLineData[] lineData = new WordJoiningLineData[lines.length];
		
		//	get line words, and measure font size
		pm.setStep("Collecting line words");
		pm.setBaseProgress(0);
		pm.setMaxProgress(5);
		int minFontSize = Integer.MAX_VALUE;
		int maxFontSize = 0;
		for (int l = 0; l < lines.length; l++) {
			pm.setProgress((l * 100) / lines.length);
			
			ImWord[] lWords = lines[l].getWords();
			int lFontSizeSum = 0;
			int lFontSizeCount = 0;
			for (int w = 1; w < lWords.length; w++) try {
				int wfs = lWords[w].getFontSize();
				minFontSize = Math.min(minFontSize, wfs);
				maxFontSize = Math.max(maxFontSize, wfs);
				lFontSizeSum += wfs;
				lFontSizeCount++;
			} catch (RuntimeException re) {}
			int lFontSize = ((lFontSizeCount == 0) ? -1 : ((lFontSizeSum + (lFontSizeCount / 2)) / lFontSizeCount));
			if (1 < lWords.length)
				Arrays.sort(lWords, ImUtils.leftRightOrder);
			lineData[l] = new WordJoiningLineData(lWords, (lines[l].bounds.bottom - lines[l].bounds.top), lFontSize);
		}
		
		//	split lines up at gaps larger than twice line height TODO threshold low enough?
		pm.setStep("Splitting lines at large gaps");
		pm.setBaseProgress(5);
		pm.setMaxProgress(10);
		ArrayList lineDataList = new ArrayList();
		for (int l = 0; l < lineData.length; l++) {
			pm.setProgress((l * 100) / lines.length);
			pm.setInfo("Line " + l + " at " + lineData[l].words[0].getLocalID() + " (font size " + lineData[l].fontSize + ", height " + lineData[l].lineHeight + ")");
			
			if (DEBUG_WORD_MERGING) {
				System.out.println("Line " + l + " of " + lineData.length + " at " + lineData[l].words[0].getLocalID() + " (font size " + lineData[l].fontSize + ", height " + lineData[l].lineHeight + ")");
				System.out.print("  words: " + lineData[l].words[0].getString());
				for (int w = 1; w < lineData[l].words.length; w++) {
					int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
					System.out.print(((gap < 100) ? "" : " ") + ((gap < 10) ? "" : " ") + " " + lineData[l].words[w].getString());
				}
				System.out.println();
				System.out.print("   gaps: " + lineData[l].words[0].getString().replaceAll(".", " "));
				for (int w = 1; w < lineData[l].words.length; w++) {
					int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
					System.out.print(gap + "" + lineData[l].words[w].getString().replaceAll(".", " "));
				}
				System.out.println();
			}
			for (int w = 1; w < lineData[l].words.length; w++) {
				int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
				if ((lineData[l].lineHeight * 2) < gap) {
					ImWord[] lWords = new ImWord[w];
					System.arraycopy(lineData[l].words, 0, lWords, 0, lWords.length);
					lineDataList.add(new WordJoiningLineData(lWords, lineData[l].lineHeight, lineData[l].fontSize));
					ImWord[] rWords = new ImWord[lineData[l].words.length - w];
					System.arraycopy(lineData[l].words, w, rWords, 0, rWords.length);
					lineData[l] = new WordJoiningLineData(rWords, lineData[l].lineHeight, lineData[l].fontSize);
					if (DEBUG_WORD_MERGING) System.out.println("  split at " + w + " for " + gap + " gap");
					w = 0;
				}
			}
			lineDataList.add(lineData[l]);
		}
		if (lineData.length < lineDataList.size())
			lineData = ((WordJoiningLineData[]) lineDataList.toArray(new WordJoiningLineData[lineDataList.size()]));
		
		//	analyze word gap structure for all lines together
		pm.setStep("Computing word gap distributions");
		pm.setBaseProgress(10);
		pm.setMaxProgress(15);
		CountingSet pageWordGaps = new CountingSet(new TreeMap());
		CountingSet[] fsPageWordGaps = ((minFontSize < maxFontSize) ? new CountingSet[maxFontSize - minFontSize + 1] : null);
		CountingSet pageWordGapQuots = new CountingSet(new TreeMap());
		for (int l = 0; l < lineData.length; l++) {
			pm.setProgress((l * 100) / lines.length);
			pm.setInfo("Line " + l + " at " + lineData[l].words[0].getLocalID() + " (font size " + lineData[l].fontSize + ", height " + lineData[l].lineHeight + ")");
			
			CountingSet lWordGaps = new CountingSet(new TreeMap());
			CountingSet lWordGapQuots = new CountingSet(new TreeMap());
			for (int w = 1; w < lineData[l].words.length; w++) {
				int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
				lWordGaps.add(new Integer(gap));
				lWordGapQuots.add(new Integer(((gap * 10) + (lineData[l].fontSize / 2)) / lineData[l].fontSize));
			}
			
			pageWordGaps.addAll(lWordGaps);
			if ((0 < lineData[l].fontSize) && (fsPageWordGaps != null)) {
				if (fsPageWordGaps[lineData[l].fontSize - minFontSize] == null)
					fsPageWordGaps[lineData[l].fontSize - minFontSize] = new CountingSet(new TreeMap());
				fsPageWordGaps[lineData[l].fontSize - minFontSize].addAll(lWordGaps);
			}
			pageWordGapQuots.addAll(lWordGapQuots);
		}
		
		pm.setStep("Computing word gap accumulation points");
		pm.setBaseProgress(15);
		pm.setMaxProgress(20);
		int[] allPageWordGaps = getElementArray(pageWordGaps);
		if (DEBUG_WORD_MERGING) {
			System.out.println("Computing word gap accumulation points from " + Arrays.toString(allPageWordGaps));
			printCountingSet(pageWordGaps);
		}
		int pageInWordGap = -1;
		for (int g = 0; g < allPageWordGaps.length; g++) {
			int gc = pageWordGaps.getCount(new Integer(allPageWordGaps[g]));
			if ((g != 0) && (gc < pageWordGaps.getCount(new Integer(allPageWordGaps[g-1]))))
				continue;
			if (((g+1) < allPageWordGaps.length) && (gc < pageWordGaps.getCount(new Integer(allPageWordGaps[g+1]))))
				continue;
			if (pageWordGaps.getCount(new Integer(pageInWordGap)) < gc)
				pageInWordGap = allPageWordGaps[g];
		}
		int pageWordGap = -1;
		for (int g = 0; g < allPageWordGaps.length; g++) {
			if (allPageWordGaps[g] <= pageInWordGap)
				continue;
			int gc = pageWordGaps.getCount(new Integer(allPageWordGaps[g]));
			if ((g != 0) && (gc < pageWordGaps.getCount(new Integer(allPageWordGaps[g-1]))))
				continue;
			if (((g+1) < allPageWordGaps.length) && (gc < pageWordGaps.getCount(new Integer(allPageWordGaps[g+1]))))
				continue;
			if (pageWordGaps.getCount(new Integer(pageWordGap)) < gc)
				pageWordGap = allPageWordGaps[g];
		}
		pm.setInfo("Found best gap pair " + pageInWordGap + " (" + pageWordGaps.getCount(new Integer(pageInWordGap)) + ") / " + pageWordGap + " (" + pageWordGaps.getCount(new Integer(pageWordGap)) + ")");
		
		int[] allPageWordGapQuots = getElementArray(pageWordGapQuots);
		if (DEBUG_WORD_MERGING) {
			System.out.println("Computing word gap quotient accumulation points from " + Arrays.toString(allPageWordGapQuots));
			printCountingSet(pageWordGapQuots);
		}
		int pageInWordGapQuot = -1;
		for (int g = 0; g < allPageWordGapQuots.length; g++) {
			int gc = pageWordGapQuots.getCount(new Integer(allPageWordGapQuots[g]));
			if ((g != 0) && (gc < pageWordGapQuots.getCount(new Integer(allPageWordGapQuots[g-1]))))
				continue;
			if (((g+1) < allPageWordGapQuots.length) && (gc < pageWordGapQuots.getCount(new Integer(allPageWordGapQuots[g+1]))))
				continue;
			if (pageWordGapQuots.getCount(new Integer(pageInWordGapQuot)) < gc)
				pageInWordGapQuot = allPageWordGapQuots[g];
		}
		int pageWordGapQuot = -1;
		for (int g = 0; g < allPageWordGapQuots.length; g++) {
			if (allPageWordGapQuots[g] <= pageInWordGapQuot)
				continue;
			int gc = pageWordGapQuots.getCount(new Integer(allPageWordGapQuots[g]));
			if ((g != 0) && (gc < pageWordGapQuots.getCount(new Integer(allPageWordGapQuots[g-1]))))
				continue;
			if (((g+1) < allPageWordGapQuots.length) && (gc < pageWordGapQuots.getCount(new Integer(allPageWordGapQuots[g+1]))))
				continue;
			if (pageWordGapQuots.getCount(new Integer(pageWordGapQuot)) < gc)
				pageWordGapQuot = allPageWordGapQuots[g];
		}
		pm.setInfo("Found best gap quotient pair " + pageInWordGapQuot + " (" + pageWordGapQuots.getCount(new Integer(pageInWordGapQuot)) + ") / " + pageWordGapQuot + " (" + pageWordGapQuots.getCount(new Integer(pageWordGapQuot)) + ")");
		
		//	analyze word gap structure for each line
		pm.setStep("Analyzing word gaps for individual lines");
		pm.setBaseProgress(20);
		pm.setMaxProgress(25);
		CountingSet maxWordGapJumps = new CountingSet(new TreeMap());
		CountingSet[] fsMaxWordGapJumps = ((minFontSize < maxFontSize) ? new CountingSet[maxFontSize - minFontSize + 1] : null);
		CountingSet minWordGaps = new CountingSet(new TreeMap());
		CountingSet[] fsMinWordGaps = ((minFontSize < maxFontSize) ? new CountingSet[maxFontSize - minFontSize + 1] : null);
		CountingSet maxNonWordGaps = new CountingSet(new TreeMap());
		CountingSet[] fsMaxNonWordGaps = ((minFontSize < maxFontSize) ? new CountingSet[maxFontSize - minFontSize + 1] : null);
		CountingSet minSpaceWordGaps = new CountingSet(new TreeMap());
		CountingSet[] fsMinSpaceWordGaps = ((minFontSize < maxFontSize) ? new CountingSet[maxFontSize - minFontSize + 1] : null);
		CountingSet maxNonSpaceWordGaps = new CountingSet(new TreeMap());
		CountingSet[] fsMaxNonSpaceWordGaps = ((minFontSize < maxFontSize) ? new CountingSet[maxFontSize - minFontSize + 1] : null);
		for (int l = 0; l < lineData.length; l++) {
			pm.setProgress((l * 100) / lines.length);
			
			CountingSet lWordGaps = new CountingSet(new TreeMap());
			int lWordGapSum = 0;
			for (int w = 1; w < lineData[l].words.length; w++) {
				int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
				lWordGaps.add(new Integer(gap));
				lWordGapSum += gap;
			}
			lineData[l].avgWordGap = ((lineData[l].words.length < 2) ? 0 : ((lWordGapSum + ((lineData[l].words.length - 1) / 2)) / (lineData[l].words.length - 1)));
			if (DEBUG_WORD_MERGING) {
				System.out.println("Word gaps in line " + l + " (" + lWordGaps.size() + " in total, font size " + lineData[l].fontSize + ")");
				printCountingSet(lWordGaps);
			}
			if (DEBUG_WORD_MERGING) System.out.println(" - line at 0 gap threshold: " + getWordString(lineData[l].words, 1));
			
			//	search largest jump in gap size 
			int lastGap = -1;
			for (Iterator git = lWordGaps.iterator(); git.hasNext();) {
				Integer gap = ((Integer) git.next());
				if (lastGap == -1) {
					lastGap = gap.intValue();
					continue;
				}
				if ((gap.intValue() - lastGap) >= lineData[l].maxWordGapJump) {
					lineData[l].maxWordGapJump = (gap.intValue() - lastGap);
					lineData[l].minWordGap = gap.intValue();
					lineData[l].maxNonWordGap = lastGap;
				}
				
				//	break if lower edge closer to larger accumulation point (likely caused by extremely large outlier, e.g. table column gap)
				int lGapDist = Math.abs(pageInWordGap - gap.intValue());
				int rGapDist = Math.abs(pageWordGap - gap.intValue());
				int gapQuot = ((gap * 10) / lineData[l].fontSize);
				int lGapQuotDist = Math.abs(pageInWordGapQuot - gapQuot);
				int rGapQuotDist = Math.abs(pageWordGapQuot - gapQuot);
				if ((rGapDist < lGapDist) && (rGapQuotDist < lGapQuotDist))
					break;
				lastGap = Math.max(gap.intValue(), 1);
			}
			
			if (DEBUG_WORD_MERGING) System.out.println(" - line at max jump gap threshold (" + lineData[l].minWordGap + "): " + getWordString(lineData[l].words, lineData[l].minWordGap));
			maxWordGapJumps.add(new Integer(lineData[l].maxWordGapJump));
			if ((0 < lineData[l].fontSize) && (fsMaxWordGapJumps != null)) {
				if (fsMaxWordGapJumps[lineData[l].fontSize - minFontSize] == null)
					fsMaxWordGapJumps[lineData[l].fontSize - minFontSize] = new CountingSet(new TreeMap());
				fsMaxWordGapJumps[lineData[l].fontSize - minFontSize].add(new Integer(lineData[l].maxWordGapJump));
			}
			
			minWordGaps.add(new Integer(lineData[l].minWordGap), lineData[l].maxWordGapJump);
			if ((0 < lineData[l].fontSize) && (fsMinWordGaps != null)) {
				if (fsMinWordGaps[lineData[l].fontSize - minFontSize] == null)
					fsMinWordGaps[lineData[l].fontSize - minFontSize] = new CountingSet(new TreeMap());
				fsMinWordGaps[lineData[l].fontSize - minFontSize].add(new Integer(lineData[l].minWordGap), lineData[l].maxWordGapJump);
			}
			maxNonWordGaps.add(new Integer(lineData[l].maxNonWordGap), lineData[l].maxWordGapJump);
			if ((0 < lineData[l].fontSize) && (fsMaxNonWordGaps != null)) {
				if (fsMaxNonWordGaps[lineData[l].fontSize - minFontSize] == null)
					fsMaxNonWordGaps[lineData[l].fontSize - minFontSize] = new CountingSet(new TreeMap());
				fsMaxNonWordGaps[lineData[l].fontSize - minFontSize].add(new Integer(lineData[l].maxNonWordGap), lineData[l].maxWordGapJump);
			}
			
			
			lineData[l].minSpaceWordGap = getMinSpaceGap(lineData[l].words, tokenizer);
			if (lineData[l].minSpaceWordGap < Integer.MAX_VALUE) {
				minSpaceWordGaps.add(new Integer(lineData[l].minSpaceWordGap));
				if ((0 < lineData[l].fontSize) && (fsMinSpaceWordGaps != null)) {
					if (fsMinSpaceWordGaps[lineData[l].fontSize - minFontSize] == null)
						fsMinSpaceWordGaps[lineData[l].fontSize - minFontSize] = new CountingSet(new TreeMap());
					fsMinSpaceWordGaps[lineData[l].fontSize - minFontSize].add(new Integer(lineData[l].minSpaceWordGap));
				}
			}
			lineData[l].maxNonSpaceWordGap = getMaxNonSpaceGap(lineData[l].words);
			if (lineData[l].maxNonSpaceWordGap > 0) {
				maxNonSpaceWordGaps.add(new Integer(lineData[l].maxNonSpaceWordGap));
				if ((0 < lineData[l].fontSize) && (fsMaxNonSpaceWordGaps != null)) {
					if (fsMaxNonSpaceWordGaps[lineData[l].fontSize - minFontSize] == null)
						fsMaxNonSpaceWordGaps[lineData[l].fontSize - minFontSize] = new CountingSet(new TreeMap());
					fsMaxNonSpaceWordGaps[lineData[l].fontSize - minFontSize].add(new Integer(lineData[l].maxNonSpaceWordGap));
				}
			}
		}
		
		//	perform word mergers starting from most certain lines
		pm.setStep("Merging words in lines with clear gap structure");
		pm.setBaseProgress(25);
		pm.setMaxProgress(40);
		TreeSet dictionary = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		for (int l = 0; l < lineData.length; l++) {
			pm.setProgress((l * 100) / lines.length);
			pm.setInfo("Line " + l + " at " + lineData[l].words[0].getLocalID() + " (font size " + lineData[l].fontSize + ", height " + lineData[l].lineHeight + ", max word gap jump " + lineData[l].maxWordGapJump + ")");
			
			if (DEBUG_WORD_MERGING) System.out.println("Line " + l + " of " + lineData.length + " at " + lineData[l].words[0].getLocalID() + " (font size " + lineData[l].fontSize + ", height " + lineData[l].lineHeight + ", max word gap jump " + lineData[l].maxWordGapJump + ")");
			if (DEBUG_WORD_MERGING) {
				int leftGapSum = 0;
				int rightGapSum = 0;
				for (int w = 1; w < lineData[l].words.length; w++) {
					int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
					if (w < (lineData[l].words.length / 2))
						leftGapSum += gap;
					else rightGapSum += gap;
				}
				System.out.print("  words: " + lineData[l].words[0].getString());
				for (int w = 1; w < lineData[l].words.length; w++) {
					int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
					System.out.print(((gap < 100) ? "" : " ") + ((gap < 10) ? "" : " ") + " " + lineData[l].words[w].getString());
				}
				System.out.println();
				System.out.print("   gaps: " + lineData[l].words[0].getString().replaceAll(".", " "));
				for (int w = 1; w < lineData[l].words.length; w++) {
					int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
					System.out.print(gap + "" + lineData[l].words[w].getString().replaceAll(".", " "));
				}
				System.out.println();
				System.out.println("  left gap sum is " + leftGapSum + ", right gap sum is " + rightGapSum);
				System.out.println("  at 0 gap threshold: " + getWordString(lineData[l].words, 1));
				
				//	show word gap distribution based thresholds
				System.out.println("  at local max non-gap + 1 threshold (" + (lineData[l].maxNonWordGap + 1) + "): " + getWordString(lineData[l].words, (lineData[l].maxNonWordGap + 1)));
				System.out.println("  at local min gap threshold (" + lineData[l].minWordGap + "): " + getWordString(lineData[l].words, lineData[l].minWordGap));
				System.out.println("  at local avg gap threshold (" + lineData[l].avgWordGap + "): " + getWordString(lineData[l].words, lineData[l].avgWordGap));
			}
			
			//	this one's clear from gap jumps, can use word gap
			boolean gapJumpSecure = ((lineData[l].maxWordGapJump * 5) > lineData[l].lineHeight);
			if (gapJumpSecure) {
				lineData[l].mergeMinWordGap = lineData[l].minWordGap;
				lineData[l].words = performWordJoins(lineData[l].words, lineData[l].mergeMinWordGap, tokenizer, dictionary);
				pm.setInfo("  securely merged at local min word gap (" + lineData[l].mergeMinWordGap + "): " + ImUtils.getString(lineData[l].words[0], lineData[l].words[lineData[l].words.length-1], true));
				continue;
			}
			
			//	show space and tokenization based thresholds
			if (DEBUG_WORD_MERGING) {
				if (lineData[l].maxNonSpaceWordGap > 0)
					System.out.println("  at maximum local non-space gap + 1 threshold (" + (lineData[l].maxNonSpaceWordGap + 1) + "): " + getWordString(lineData[l].words, (lineData[l].maxNonSpaceWordGap + 1)));
				if (lineData[l].minSpaceWordGap < Integer.MAX_VALUE)
					System.out.println("  at minimum local space gap threshold (" + lineData[l].minSpaceWordGap + "): " + getWordString(lineData[l].words, lineData[l].minSpaceWordGap));
				if ((lineData[l].maxNonSpaceWordGap > 0) && (lineData[l].minSpaceWordGap < Integer.MAX_VALUE)) {
					int spaceWordGap = ((lineData[l].minSpaceWordGap + lineData[l].maxNonSpaceWordGap + 1) / 2);
					System.out.println("  at avg local space gap threshold (" + spaceWordGap + "): " + getWordString(lineData[l].words, spaceWordGap));
				}
			}
			
			//	this one's clear from spaces and tokenization, can use space based word gap
			boolean nonSpaceSecure = ((lineData[l].maxNonSpaceWordGap * 5) > lineData[l].lineHeight);
			if (nonSpaceSecure) {
				lineData[l].mergeMinWordGap = (lineData[l].maxNonSpaceWordGap + 1);
				lineData[l].words = performWordJoins(lineData[l].words, lineData[l].mergeMinWordGap, tokenizer, dictionary);
				pm.setInfo("  securely merged at local non-space word gap + 1 (" + lineData[l].mergeMinWordGap + "): " + ImUtils.getString(lineData[l].words[0], lineData[l].words[lineData[l].words.length-1], true));
				continue;
			}
		}
		
		//	try and increase min word gap from one fifth of line height upward, or from previous threshold, and use gap with most dictionary hits
		pm.setStep("Merging words in remaining lines");
		pm.setBaseProgress(40);
		pm.setMaxProgress(100);
		for (int r = 0, nml = 0;; r++, nml = 0) {
			pm.setStep("Merging words in remaining lines, round " + (r+1));
			pm.setBaseProgress(40 + ((60 * r) / (r+1)));
			for (int l = 0; l < lineData.length; l++) {
				pm.setProgress((l * 100) / lines.length);
				pm.setInfo("Line " + l + " at " + lineData[l].words[0].getLocalID() + " (font size " + lineData[l].fontSize + ", height " + lineData[l].lineHeight + ", max word gap jump " + lineData[l].maxWordGapJump + ")");
				
				if (DEBUG_WORD_MERGING) System.out.println("Round " + r + ": Line " + l + " of " + lineData.length + " at " + lineData[l].words[0].getLocalID() + " (font size " + lineData[l].fontSize + ", height " + lineData[l].lineHeight + ", max word gap jump " + lineData[l].maxWordGapJump + ")");
				if (DEBUG_WORD_MERGING) {
					System.out.print("  words: " + lineData[l].words[0].getString());
					for (int w = 1; w < lineData[l].words.length; w++) {
						int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
						System.out.print(((gap < 100) ? "" : " ") + ((gap < 10) ? "" : " ") + " " + lineData[l].words[w].getString());
					}
					System.out.println();
					System.out.print("   gaps: " + lineData[l].words[0].getString().replaceAll(".", " "));
					for (int w = 1; w < lineData[l].words.length; w++) {
						int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
						System.out.print(gap + "" + lineData[l].words[w].getString().replaceAll(".", " "));
					}
					System.out.println();
					System.out.println("  at 0 gap threshold: " + getWordString(lineData[l].words, 1));
				}
				
				//	keep track of dictionary hits for each char
				int lineCharCount = 0;
				for (int w = 0; w < lineData[l].words.length; w++)
					lineCharCount += lineData[l].words[w].getString().length();
				char[] lineChars = new char[lineCharCount];
				char[] lineCharDictHitStatus = new char[lineCharCount];
				ImWord[] lineCharWords = new ImWord[lineCharCount];
				for (int lco = 0, w = 0; w < lineData[l].words.length; w++)
					for (int c = 0; c < lineData[l].words[w].getString().length(); c++) {
						lineChars[lco] = lineData[l].words[w].getString().charAt(c);
						lineCharDictHitStatus[lco] = 'O';
						lineCharWords[lco] = lineData[l].words[w];
						lco++;
					}
				
				//	count dictionary hits, starting at one fifth of line height
				int maxDictHitCount = -1;
				int maxDictHitCharCount = -1;
//				int maxDictHitTokenCount = 0;
//				int maxDictHitLength = Integer.MIN_VALUE;
//				int minKillSpaceCount = 0;
				int minNumberTokenCount = 0;
				int minNonNumberTokenCount = 0;
				for (int twg = ((lineData[l].mergeMinWordGap == -1) ? ((lineData[l].lineHeight + 2) / 5) : lineData[l].mergeMinWordGap); twg < lineData[l].lineHeight; twg++) {
					String twgString = getWordString(lineData[l].words, twg);
					if (DEBUG_WORD_MERGING) System.out.println("  at test word gap threshold (" + twg + "): " + twgString);
					TokenSequence twgTokens = tokenizer.tokenize(twgString);
					int twgDictHitCount = 0;
					int twgDictHitCharCount = 0;
					int twgKillSpaceCount = 0;
					int twgNonNumberTokenCount = 0;
					int twgNumberTokenCount = 0;
					for (int lco = 0, t = 0; t < twgTokens.size(); lco += twgTokens.valueAt(t++).length()) {
						String twgToken = twgTokens.valueAt(t);
						if (twgToken.matches("[0-9]+"))
							twgNumberTokenCount++;
						else twgNonNumberTokenCount++;
						if (twgToken.length() < 2)
							continue;
						if (dictionary.contains(twgToken) && (twgToken.matches("[12][0-9]{3}") || !Gamta.isNumber(twgToken))) {
							twgDictHitCount++;
							twgDictHitCharCount += twgToken.length();
							lineCharDictHitStatus[lco] = 'S';
							for (int c = 1; c < twgToken.length(); c++)
								lineCharDictHitStatus[lco + c] = 'C';
						}
						if ((t != 0) && Gamta.insertSpace(twgTokens.valueAt(t-1), twgToken) && (twgTokens.getWhitespaceAfter(t-1).length() == 0))
							twgKillSpaceCount++;
					}
					if (DEBUG_WORD_MERGING) {
						System.out.println("  ==> " + twgTokens.size() + " tokens, " + twgDictHitCount + " in dictionary (" + twgDictHitCharCount + " chars), " + twgKillSpaceCount + " spaces killed, " + twgNumberTokenCount + " number tokens, " + twgNonNumberTokenCount + " other tokens");
						System.out.println("  ==> dict hit status: " + Arrays.toString(lineChars));
						System.out.println("                       " + Arrays.toString(lineCharDictHitStatus));
					}
					
					//	do we have a new optimum?
					if ((maxDictHitCharCount < twgDictHitCharCount) || ((maxDictHitCharCount == twgDictHitCharCount) && (twgDictHitCount < maxDictHitCount))) {
						pm.setInfo("  ==> new best word gap " + twg + " for dictionary hits");
						maxDictHitCount = twgDictHitCount;
						maxDictHitCharCount = twgDictHitCharCount;
//						maxDictHitTokenCount = twgTokens.size();
//						maxDictHitLength = twgString.length();
//						minKillSpaceCount = twgKillSpaceCount;
						minNumberTokenCount = twgNumberTokenCount;
						minNonNumberTokenCount = twgNonNumberTokenCount;
						if (lineData[l].mergeMinWordGap < twg)
							nml++;
						lineData[l].mergeMinWordGap = twg;
						if (twgString.indexOf(' ') == -1)
							break;
						else continue;
					}
					
					if ((twgDictHitCount + twgDictHitCharCount) < (maxDictHitCount + maxDictHitCharCount)) {
						if (twgString.indexOf(' ') == -1)
							break;
						else continue;
					}
//					
//					if ((maxDictHitTokenCount <= twgTokens.size()) && (twgString.length() < maxDictHitLength)) {
//						pm.setInfo("  ==> new best word gap " + twg + " for overall length");
//						maxDictHitCount = twgDictHitCount;
//						maxDictHitCharCount = twgDictHitCharCount;
//						maxDictHitTokenCount = twgTokens.size();
//						maxDictHitLength = twgString.length();
//						minKillSpaceCount = twgKillSpaceCount;
//						minNumberTokenCount = twgNumberTokenCount;
//						minNonNumberTokenCount = twgNonNumberTokenCount;
//						if (lineData[l].mergeMinWordGap < twg)
//							nml++;
//						lineData[l].mergeMinWordGap = twg;
//						continue;
//					}
					
					if ((minNonNumberTokenCount <= twgNonNumberTokenCount) && (twgNumberTokenCount < minNumberTokenCount)) {
						pm.setInfo("  ==> new best word gap " + twg + " for fewer number tokens");
						maxDictHitCount = twgDictHitCount;
						maxDictHitCharCount = twgDictHitCharCount;
//						maxDictHitTokenCount = twgTokens.size();
//						maxDictHitLength = twgString.length();
//						minKillSpaceCount = twgKillSpaceCount;
						minNumberTokenCount = twgNumberTokenCount;
						minNonNumberTokenCount = twgNonNumberTokenCount;
						if (lineData[l].mergeMinWordGap < twg)
							nml++;
						lineData[l].mergeMinWordGap = twg;
						continue;
					}
					
					//	no use increasing minimum gap any further ...
					if (twgString.indexOf(' ') == -1)
						break;
				}
//				
//				//	no dictionary hits at all, be safe
//				if (maxDictHitCount == 0)
//					lineData[l].mergeMinWordGap = -1;
				
				//	nothing found on this one, at least merge at minimum one fifth 
				if (lineData[l].mergeMinWordGap == -1)
					lineData[l].mergeMinWordGap = ((lineData[l].lineHeight + 2) / 5);
				
				//	perform mergers (we need to amend dictionary after round only, so to not reinforce partial mergers)
				lineData[l].words = performWordJoins(lineData[l].words, lineData[l].mergeMinWordGap, tokenizer, null);
				pm.setInfo("  merged at dictionary hit optimized min word gap (" + lineData[l].mergeMinWordGap + "): " + ImUtils.getString(lineData[l].words[0], lineData[l].words[lineData[l].words.length-1], true));
				if (DEBUG_WORD_MERGING) System.out.println("  ==> " + getWordString(lineData[l].words, lineData[l].mergeMinWordGap));
//				
//				//	recover dictionary hits that disappeared due to mergers
//				boolean dictHitResplit = false;
//				for (int c = 1; c < lineCharDictHitStatus.length; c++) {
//					if (lineCharWords[c-1] == lineCharWords[c])
//						continue;
//					if (lineCharWords[c-1].getNextRelation() != ImWord.NEXT_RELATION_CONTINUE)
//						continue;
//					if ((lineCharDictHitStatus[c-1] != 'O') && (lineCharDictHitStatus[c] == 'S')) {
//						lineCharWords[c-1].setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
//						dictHitResplit = true;
//					}
////					TOO AGGRESSIVE WITH FREQUENT WORD PREFIXES, INFIXES, AND SUFFIXES
////					else if ((lineCharDictHitStatus[c-1] != 'O') && (lineCharDictHitStatus[c] == 'O')) {
////						lineCharWords[c-1].setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
////						dictHitResplit = true;
////					}
//				}
//				if (dictHitResplit)
//					System.out.println("  re-split merged-away dictionary hits: " + ImUtils.getString(lineData[l].words[0], lineData[l].words[lineData[l].words.length-1], true));
			}
			
			//	nothing new this round, we're done
			if (nml == 0)
				break;
			
			/* extend dictionary (we need to amend dictionary after round only,
			 * so to not reinforce partial mergers)
			 * - only add words to dictionary that don't merge away if min word
			 *   gap increases by 2 or 3 to prevent adding results of incomplete
			 *   mergers to dictionary (which would reinforce these mergers)
			 * - collect line tokens at each threshold, retaining only those
			 *   that remain at min word gap increase, and adding only the
			 *   latter to the main dictionary */
			for (int l = 0; l < lineData.length; l++) {
				TreeSet lDictionary = getWordTokens(lineData[l].words, lineData[l].mergeMinWordGap, tokenizer);
				for (int g = (lineData[l].mergeMinWordGap + 1); g < (lineData[l].mergeMinWordGap + 3); g++)
					lDictionary.retainAll(getWordTokens(lineData[l].words, g, tokenizer));
				dictionary.addAll(lDictionary);
				if (DEBUG_WORD_MERGING && (lDictionary.size() != 0))
					System.out.println("Line " + l + " of " + lineData.length + " at (" + lineData[l].mergeMinWordGap + "): dictionary extended by " + lDictionary);
			}
			
			//	TODO consider case sensitive dictionary
		}
		
		if (DEBUG_WORD_MERGING) {
			for (int l = 0; l < lineData.length; l++)
				System.out.println("Line " + l + " of " + lineData.length + " at " + lineData[l].words[0].getLocalID() + " (" + lineData[l].mergeMinWordGap + "): " + ImUtils.getString(lineData[l].words[0], lineData[l].words[lineData[l].words.length-1], true));
		}
	}
	
	private static ImWord[] performWordJoins(ImWord[] words, int minWordGap, Tokenizer tokenizer, Set dictionary) {
		StringBuffer wStr = new StringBuffer(words[0].getString());
		ArrayList wStrCharWords = new ArrayList();
		for (int c = 0; c < words[0].getString().length(); c++)
			wStrCharWords.add(words[0]);
		for (int w = 1; w < words.length; w++) {
			
			//	append space if required
			if (space(words, w, minWordGap)) {
				wStr.append(' ');
				wStrCharWords.add(null);
			}
			
			//	append word proper
			wStr.append(words[w].getString());
			for (int c = 0; c < words[w].getString().length(); c++)
				wStrCharWords.add(words[w]);
		}
		
		//	merge away combinable accents
		for (int c = 0; c < wStr.length(); c++) {
			char ch = wStr.charAt(c);
			if (COMBINABLE_ACCENTS.indexOf(ch) == -1)
				continue;
//			System.out.println("Char is " + COMBINABLE_ACCENT_MAPPINGS.get(new Character(ch)).toString());
			if (c != 0) {
				char lCh = wStr.charAt(c-1);
				char cCh = StringUtils.getCharForName(lCh + COMBINABLE_ACCENT_MAPPINGS.get(new Character(ch)).toString());
				if (cCh != 0) {
					wStr.deleteCharAt(c);
					wStrCharWords.remove(c);
					wStr.setCharAt(--c, cCh);
					continue;
				}
			}
			if ((c+1) < wStr.length()) {
				char rCh = wStr.charAt(c+1);
				char cCh = StringUtils.getCharForName(rCh + COMBINABLE_ACCENT_MAPPINGS.get(new Character(ch)).toString());
				if (cCh != 0) {
					wStr.deleteCharAt(c);
					wStrCharWords.remove(c);
					wStr.setCharAt(c, cCh);
					continue;
				}
			}
		}
		
		//	tokenize string
		TokenSequence wStrTokens = tokenizer.tokenize(wStr);
		
		//	update word strings
		ArrayList mWords = new ArrayList();
		for (int t = 0; t < wStrTokens.size(); t++) {
			Token wStrToken = wStrTokens.tokenAt(t);
			for (int c = wStrToken.getStartOffset(); c < wStrToken.getEndOffset(); c++) {
				ImWord imw1 = ((c == 0) ? null : ((ImWord) wStrCharWords.get(c-1)));
				ImWord imw2 = ((ImWord) wStrCharWords.get(c));
				if (imw1 == imw2)
					imw2.setString(imw2.getString() + wStr.charAt(c));
				else imw2.setString("" + wStr.charAt(c));
			}
		}
		
		//	cut out empty words
		for (int w = 0; w < words.length; w++) {
			if (wStrCharWords.contains(words[w]))
				mWords.add(words[w]);
			else {
				ImWord[] dWord = {words[w]};
				ImUtils.makeStream(dWord, ImWord.TEXT_STREAM_TYPE_DELETED, null);
			}
		}
		
		//	set word relations
		for (int t = 0; t < wStrTokens.size(); t++) {
			Token wStrToken = wStrTokens.tokenAt(t);
			for (int c = wStrToken.getStartOffset(); c < (wStrToken.getEndOffset() - 1); c++) {
				ImWord imw1 = ((ImWord) wStrCharWords.get(c));
				ImWord imw2 = ((ImWord) wStrCharWords.get(c+1));
				if (imw1 != imw2)
					imw1.setNextRelation(ImWord.NEXT_RELATION_CONTINUE);
			}
			if (dictionary != null)
				dictionary.add(wStrToken.getValue());
		}
		
		//	indicate whether or not words have changed
		return ((mWords.size() < words.length) ? ((ImWord[]) mWords.toArray(new ImWord[mWords.size()])) : words);
	}
	
	private static TreeSet getWordTokens(ImWord[] words, int minWordGap, Tokenizer tokenizer) {
		TreeSet dictionary = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		String wStr = getWordString(words, minWordGap);
		TokenSequence wStrTokens = tokenizer.tokenize(wStr);
		for (int t = 0; t < wStrTokens.size(); t++)
			dictionary.add(wStrTokens.valueAt(t));
		return dictionary;
	}
	
	private static int getMinSpaceGap(ImWord[] words, Tokenizer tokenizer) {
		int minSpaceGap = Integer.MAX_VALUE;
		for (int w = 1; w < words.length; w++) {
			if (Gamta.insertSpace(words[w-1].getString(), words[w].getString()) && (tokenizer.tokenize(words[w-1].getString() + words[w].getString()).size() > 1))
				minSpaceGap = Math.min(minSpaceGap, getGap(words[w-1], words[w]));
		}
		return Math.max(minSpaceGap, 0);
	}
	
	private static int getMaxNonSpaceGap(ImWord[] words) {
		int maxNonSpaceGap = -1;
		for (int w = 1; w < words.length; w++) {
			if (!Gamta.insertSpace(words[w-1].getString(), words[w].getString()))
				maxNonSpaceGap = Math.max(maxNonSpaceGap, getGap(words[w-1], words[w]));
		}
		return maxNonSpaceGap;
	}
	
	private static void printCountingSet(CountingSet cs) {
		for (Iterator iit = cs.iterator(); iit.hasNext();) {
			Integer i = ((Integer) iit.next());
			System.out.println(" - " + i.intValue() + " (" + cs.getCount(i) + " times)");
		}
	}
	
	private static String getWordString(ImWord[] words, int minWordGap) {
		if (words.length == 0)
			return "";
		StringBuffer wStr = new StringBuffer(words[0].getString());
		for (int w = 1; w < words.length; w++) {
			
			//	append space if required
			if (space(words, w, minWordGap))
				wStr.append(' ');
			
			//	append word proper
			wStr.append(words[w].getString());
		}
		
		//	merge away combinable accents
		for (int c = 0; c < wStr.length(); c++) {
			char ch = wStr.charAt(c);
			if (COMBINABLE_ACCENTS.indexOf(ch) == -1)
				continue;
//			System.out.println("Char is " + COMBINABLE_ACCENT_MAPPINGS.get(new Character(ch)).toString());
			if (c != 0) {
				char lCh = wStr.charAt(c-1);
				char cCh = StringUtils.getCharForName(lCh + COMBINABLE_ACCENT_MAPPINGS.get(new Character(ch)).toString());
				if (cCh != 0) {
					wStr.deleteCharAt(c);
					wStr.setCharAt(--c, cCh);
					continue;
				}
			}
			if ((c+1) < wStr.length()) {
				char rCh = wStr.charAt(c+1);
				char cCh = StringUtils.getCharForName(rCh + COMBINABLE_ACCENT_MAPPINGS.get(new Character(ch)).toString());
				if (cCh != 0) {
					wStr.deleteCharAt(c);
					wStr.setCharAt(c, cCh);
					continue;
				}
			}
		}
		
		//	finally ...
		return wStr.toString();
	}
	
	private static int getGap(ImWord lWord, ImWord rWord) {
		int wordGap = Math.max(0, (rWord.bounds.left - lWord.bounds.right));
		
		//	cap gap at line height to deal with extremely large (column) gaps in tables
		int lineHeight = ((lWord.bounds.bottom - lWord.bounds.top + rWord.bounds.bottom - rWord.bounds.top) / 2);
		
		//	investigate left tail char and right lead char
		String lStr = lWord.getString();
		char lTailCh = lStr.charAt(lStr.length()-1);
		String rStr = rWord.getString();
		char rLeadCh = rStr.charAt(0);
		
		//	add some anti-kerning for left tailing 'f' if right lead char doesn't have left ascender (maybe 10% of line height)
		boolean antiKern = false;
		if (("fFPTVWY7'".indexOf(lTailCh) != -1) && ("acdegijmnopqrstuvwxyzAJ.,;:+/-".indexOf(rLeadCh) != -1))
			antiKern = true;
		else if (("rvwyDOSU09".indexOf(lTailCh) != -1) && ("A.,".indexOf(rLeadCh) != -1))
			antiKern = true;
		else if ((".,".indexOf(lTailCh) != -1) && ("vwyCOQTVW01".indexOf(rLeadCh) != -1))
			antiKern = true;
		
		//	subtract some kerning for left tailing or right leading 'i', 'l', '1', etc. (maybe 5% of line height)
		boolean kern = false;
		if (("([{".indexOf(lTailCh) != -1) || (")]}".indexOf(rLeadCh) != -1))
			kern = true;
		
		if (antiKern)
			wordGap += (lineHeight / 10);
		else if (kern)
			wordGap -= (lineHeight / 20);
		wordGap = Math.max(wordGap, 0);
		
		//	finally ...
		return wordGap;
	}
	
	private static boolean space(ImWord[] words, int w, int minWordGap) {
		int gap = getGap(words[w-1], words[w]);
		
		//	font mismatch ==> don't merge
		if (!words[w-1].getAttribute(ImWord.FONT_NAME_ATTRIBUTE, "").equals(words[w].getAttribute(ImWord.FONT_NAME_ATTRIBUTE, "")))
			return true;
		
		//	compute adjusted gap for case to left and right
		int aGap = gap;
		String lStr = words[w-1].getString();
		char lTailCh = lStr.charAt(lStr.length()-1);
		String rStr = words[w].getString();
		char rLeadCh = rStr.charAt(0);
		if (Character.isLetter(lTailCh) && Character.isLetter(rLeadCh)) {
			
			//	left side ends in lower case and right side starts in upper case ==> only merge if gap at most half the threshold
			if ((lTailCh == Character.toLowerCase(lTailCh)) && (rLeadCh == Character.toUpperCase(rLeadCh)))
				aGap = (aGap * 2);
			
			//	left is single upper case letter, right side starts in lower case ==> even merge if gap is a little above threshold
			else if ((lStr.length() == 1) && (lTailCh == Character.toUpperCase(lTailCh)) && (rLeadCh == Character.toLowerCase(rLeadCh)))
				aGap = ((aGap * 2) / 3);
		}
		
		//	gap above threshold ==> don't merge
		if (minWordGap <= aGap)
			return true;
		
		//	gap at least four times as large as smaller of adjacent gaps (looks like we're in a dense area), and closer to threshold than to average of adjacent gaps ==> don't merge
		int lGap = Math.max(1, ((w < 2) ? gap : getGap(words[w-2], words[w-1])));
		int rGap = Math.max(1, (((w+1) == words.length) ? gap : getGap(words[w], words[w+1])));
		int avgLrGap = ((lGap + rGap + 1) / 2);
		if (false) {}
		else if ((gap - avgLrGap) < (minWordGap - gap)) {}
		else if ((Math.min(lGap, rGap) * 4) <= gap)
			return true;
		else if ((avgLrGap * 3) <= gap)
			return true;
		
		//	found no reason for a space
		return false;
	}
	
	private static final boolean DEBUG_WORD_SPLITTING = true;
	
	private static class WordSplittingLineData extends LineData {
		CountingSet all = new CountingSet(new TreeMap());
		CountingSet separating = new CountingSet(new TreeMap());
		CountingSet nonSpace = new CountingSet(new TreeMap());
		CountingSet relevant;
		float maxJump = -1;
		float maxJumpLow = -1;
		float maxJumpHigh = -1;
		WordSplittingLineData(ImWord[] words, int lineHeight, int fontSize) {
			super(words, lineHeight, fontSize);
		}
		void computeRelevantWordGaps(float avgNonSpaceGap) {
			this.relevant = new CountingSet(new TreeMap());
			if (this.nonSpace.isEmpty()) {
				if (DEBUG_WORD_SPLITTING) System.out.println(" - added average non-space gap for lack of local ones");
				this.relevant.add(new Float(round(avgNonSpaceGap, 1)));
			}
			else this.relevant.addAll(this.nonSpace);
			this.relevant.addAll(this.separating);
			if (DEBUG_WORD_SPLITTING) System.out.println(" - relevant gaps: " + this.relevant);
			
			float lastGap = Float.NaN;
			for (Iterator git = this.relevant.iterator(); git.hasNext();) {
				Number gap = ((Number) git.next());
				if (!Float.isNaN(lastGap)) {
					float gapJump = round((gap.floatValue() - lastGap), 1);
					if (this.maxJump < gapJump) {
						this.maxJump = gapJump;
						this.maxJumpLow = lastGap;
						this.maxJumpHigh = gap.floatValue();
					}
				}
				lastGap = gap.floatValue();
			}
			if (DEBUG_WORD_SPLITTING) System.out.println(" - maximum gap jump is " + this.maxJump + " (" + this.maxJumpLow + "/" + this.maxJumpHigh + ")");
		}
	}
	
	private static void splitWords(ImRegion[] lines, Tokenizer tokenizer, ProgressMonitor pm) {
		WordSplittingLineData[] lineData = new WordSplittingLineData[lines.length];
		
		//	get line words, and measure font size and line height
		pm.setStep("Collecting line words");
		pm.setBaseProgress(0);
		pm.setMaxProgress(10);
		int minFontSize = Integer.MAX_VALUE;
		int maxFontSize = 0;
		int lineHeightSum = 0;
		for (int l = 0; l < lines.length; l++) {
			pm.setProgress((l * 100) / lines.length);
			
			ImWord[] lWords = lines[l].getWords();
			int lFontSizeSum = 0;
			int lFontSizeCount = 0;
			for (int w = 1; w < lWords.length; w++) try {
				int wfs = lWords[w].getFontSize();
				minFontSize = Math.min(minFontSize, wfs);
				maxFontSize = Math.max(maxFontSize, wfs);
				lFontSizeSum += wfs;
				lFontSizeCount++;
			} catch (RuntimeException re) {}
			int lFontSize = ((lFontSizeCount == 0) ? -1 : ((lFontSizeSum + (lFontSizeCount / 2)) / lFontSizeCount));
			if (1 < lWords.length)
				Arrays.sort(lWords, ImUtils.leftRightOrder);
			lineData[l] = new WordSplittingLineData(lWords, (lines[l].bounds.bottom - lines[l].bounds.top), lFontSize);
			lineHeightSum += lines[l].bounds.getHeight();
		}
		int avgLineHeight = ((lineHeightSum + (lines.length / 2)) / lines.length);
		
		//	split lines up at gaps larger than twice line height TODO threshold low enough?
		pm.setStep("Splitting lines at large gaps");
		pm.setBaseProgress(10);
		pm.setMaxProgress(20);
		ArrayList lineDataList = new ArrayList();
		for (int l = 0; l < lineData.length; l++) {
			pm.setProgress((l * 100) / lines.length);
			pm.setInfo("Line " + l + " at " + lineData[l].words[0].getLocalID() + " (font size " + lineData[l].fontSize + ", height " + lineData[l].lineHeight + ")");
			
			if (DEBUG_WORD_SPLITTING) {
				System.out.println("Line " + l + " of " + lineData.length + " at " + lineData[l].words[0].getLocalID() + " (font size " + lineData[l].fontSize + ", height " + lineData[l].lineHeight + ")");
				System.out.print("  words: " + lineData[l].words[0].getString());
				for (int w = 1; w < lineData[l].words.length; w++) {
					int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
					System.out.print(((gap < 100) ? "" : " ") + ((gap < 10) ? "" : " ") + " " + lineData[l].words[w].getString());
				}
				System.out.println();
				System.out.print("   gaps: " + lineData[l].words[0].getString().replaceAll(".", " "));
				for (int w = 1; w < lineData[l].words.length; w++) {
					int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
					System.out.print(gap + "" + lineData[l].words[w].getString().replaceAll(".", " "));
				}
				System.out.println();
			}
			for (int w = 1; w < lineData[l].words.length; w++) {
				int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
				if ((lineData[l].lineHeight * 2) < gap) {
					ImWord[] lWords = new ImWord[w];
					System.arraycopy(lineData[l].words, 0, lWords, 0, lWords.length);
					lineDataList.add(new WordSplittingLineData(lWords, lineData[l].lineHeight, lineData[l].fontSize));
					ImWord[] rWords = new ImWord[lineData[l].words.length - w];
					System.arraycopy(lineData[l].words, w, rWords, 0, rWords.length);
					lineData[l] = new WordSplittingLineData(rWords, lineData[l].lineHeight, lineData[l].fontSize);
					if (DEBUG_WORD_SPLITTING) System.out.println("  split at " + w + " for " + gap + " gap");
					w = 0;
				}
			}
			lineDataList.add(lineData[l]);
		}
		if (lineData.length < lineDataList.size())
			lineData = ((WordSplittingLineData[]) lineDataList.toArray(new WordSplittingLineData[lineDataList.size()]));
		pm.setProgress(100);
		
		/* TODOne Build WordSplitter as reverse of WordJoiner:
		 * - https://github.com/gsautter/goldengate-imagine/issues/864
		 * - TEST: Nauplius.26.e2018015.imd at FFA6FFE8FF85CB7DB102C5500823306A
		 */
		
		//	analyze word gap structure for all lines together
		CountingSet allSeparatingWordGaps = new CountingSet(new TreeMap());
		CountingSet allNonSpaceWordGaps = new CountingSet(new TreeMap());
		
		//	collect all words for dictionary-based scoring in problematic lines
		CountingSet allWords = new CountingSet(new TreeMap(String.CASE_INSENSITIVE_ORDER));
		
		//	compute word gaps for all lines, and collect separating ones as well as ones that do _non_ represent spaces
		pm.setStep("Computing word gap distributions");
		pm.setBaseProgress(20);
		pm.setMaxProgress(50);
		for (int l = 0; l < lineData.length; l++) {
			pm.setProgress((l * 100) / lines.length);
			pm.setInfo("Line " + l + " at " + lineData[l].words[0].getLocalID() + " (font size " + lineData[l].fontSize + ", height " + lineData[l].lineHeight + "): " + ImUtils.getString(lineData[l].words, true, 2));
			
			for (int w = 1; w < lineData[l].words.length; w++) {
				int gap = (lineData[l].words[w].bounds.left - lineData[l].words[w-1].bounds.right);
				lineData[l].all.add(new Float(normalize(gap, lineData[l].lineHeight, avgLineHeight, 1)));
				if (lineData[l].words[w-1].getFontSize() != lineData[l].words[w].getFontSize())
					continue; // skip over emulated small-caps
				String lwStr = StringUtils.normalizeString(lineData[l].words[w-1].getString());
				if (lwStr.startsWith("<") || lwStr.startsWith(">"))
					continue;
				String rwStr = StringUtils.normalizeString(lineData[l].words[w].getString());
				if (rwStr.startsWith("<") || rwStr.startsWith(">"))
					continue;
				String wStr = (lwStr + rwStr);
				if (wStr.matches("\\.{2,}"))
					continue;
				TokenSequence wTs = tokenizer.tokenize(wStr);
				
				boolean isSpace;
				if (".:,;!?".indexOf(rwStr) != -1)
					isSpace = false;
				else if (StringUtils.isClosingBracket(rwStr.substring(0, 1)))
					isSpace = false;
				else if (StringUtils.isOpeningBracket(lwStr.substring(0, 1)))
					isSpace = false;
				else if (StringUtils.isClosingBracket(lwStr.substring(0, 1)))
					isSpace = true;
				else if (StringUtils.isOpeningBracket(rwStr.substring(0, 1)))
					isSpace = true;
				else if (":,;!?".indexOf(lwStr) != -1)
					isSpace = true;
				else if (StringUtils.isWord(lwStr) && StringUtils.isWord(rwStr))
					isSpace = true;
				else if (StringUtils.isNumber(lwStr) && StringUtils.isNumber(rwStr))
					isSpace = true;
				else continue;
				/* NON-SPACES: count
				 * - 	before periods
				 * - 	before closing brackets
				 * - 	before colons, commas, semicolons
				 * - 	before question and exclamation marks
				 * - 	after opening brackets
				 * 
				 * SPACES: count
				 * - between words (both if second starts or does not start with cap)
				 * - 	after colons, semicolons, and commas
				 * - 	before opening brackets
				 * - 	after closing brackets (unless followed by any of above space blockers along lines of period, colon, etc.)
				 */
				
				if (wTs.size() == 1)
					lineData[l].separating.add(new Float(normalize(gap, lineData[l].lineHeight, avgLineHeight, 1)));
				else if (!isSpace)
					lineData[l].nonSpace.add(new Float(normalize(gap, lineData[l].lineHeight, avgLineHeight, 1)));
			}
			
			if (DEBUG_WORD_SPLITTING) {
				System.out.println(" - separator gaps: " + lineData[l].separating);
				System.out.println(" - non-space gaps: " + lineData[l].nonSpace);
			}
			
			allSeparatingWordGaps.addAll(lineData[l].separating);
			allNonSpaceWordGaps.addAll(lineData[l].nonSpace);
			
			if (lineData[l].separating.isEmpty()) {
				collectWords(lineData[l], allWords);
				lineData[l] = null; // no need to bother with this one any longer
			}
		}
		pm.setProgress(100);
		
		MedianAverage allAvgNonSpaceGap = getMedianAverage(allNonSpaceWordGaps);
		MedianAverage allAvgSeparatingGap = getMedianAverage(allSeparatingWordGaps);
		if (DEBUG_WORD_SPLITTING) {
			System.out.println("Overall word gaps:");
			System.out.println(" - tokenizing apart without required space: " + allNonSpaceWordGaps);
			System.out.println("   ==>  " + allAvgNonSpaceGap);
			System.out.println(" - separating tokens: " + allSeparatingWordGaps);
			System.out.println("   ==>  " + allAvgSeparatingGap);
		}
		
		//	handle lines whose gap distribution is unambiguous
		pm.setStep("Handling unproblematic lines");
		pm.setBaseProgress(50);
		pm.setMaxProgress(75);
		for (int l = 0; l < lineData.length; l++) {
			pm.setProgress((l * 100) / lines.length);
			if (lineData[l] == null)
				continue; // no relevant spaces in this one at all ...
			pm.setInfo("Line " + l + " at " + lineData[l].words[0].getLocalID() + " (font size " + lineData[l].fontSize + ", height " + lineData[l].lineHeight + "): " + ImUtils.getString(lineData[l].words, true, 2));
			
			if (DEBUG_WORD_SPLITTING) {
				System.out.println(" - all gaps: " + lineData[l]);
				System.out.println(" - separator gaps: " + lineData[l].separating);
				System.out.println(" - non-space gaps: " + lineData[l].nonSpace);
				System.out.print("  words: " + lineData[l].words[0].getString());
				for (int w = 1; w < lineData[l].words.length; w++) {
					int gap = (lineData[l].words[w].bounds.left - lineData[l].words[w-1].bounds.right);
					System.out.print(((gap < 100) ? "" : " ") + (((gap < 10) && (-1 < gap)) ? "" : " ") + " " + lineData[l].words[w].getString());
				}
				System.out.println();
				System.out.print("   gaps: " + lineData[l].words[0].getString().replaceAll(".", " "));
				for (int w = 1; w < lineData[l].words.length; w++) {
					int gap = (lineData[l].words[w].bounds.left - lineData[l].words[w-1].bounds.right);
					System.out.print(gap + "" + lineData[l].words[w].getString().replaceAll(".", " "));
				}
				System.out.println();
			}
			
			lineData[l].computeRelevantWordGaps(allAvgNonSpaceGap.average);
			int minSpaceWidth = -1;
			
			if (Math.abs(lineData[l].maxJumpLow - allAvgNonSpaceGap.average) > Math.abs(lineData[l].maxJumpLow - allAvgSeparatingGap.average))
				pm.setInfo(" ==> looking problematic, low end of maximum jump closer to separating gap: " + Math.abs(lineData[l].maxJumpLow - allAvgNonSpaceGap.average) + " > " + Math.abs(lineData[l].maxJumpLow - allAvgSeparatingGap.average));
			else if (Math.abs(lineData[l].maxJumpHigh - allAvgNonSpaceGap.average) < Math.abs(lineData[l].maxJumpHigh - allAvgSeparatingGap.average)) {
				pm.setInfo(" ==> looking problematic, high end of maximum jump closer to non-space gap: " + Math.abs(lineData[l].maxJumpHigh - allAvgNonSpaceGap.average) + " < " + Math.abs(lineData[l].maxJumpHigh - allAvgSeparatingGap.average));
				float lWordGapMax = ((Float) lineData[l].all.last()).floatValue();
				pm.setInfo(" - maximum gap is " + lWordGapMax + " against global average of " + allAvgSeparatingGap.average);
				
				//	proceed as normal if _maximum_ gap below over-all average as well (extremely dense line)
				if (lWordGapMax < allAvgSeparatingGap.average) {
					pm.setInfo(" ==> looking OK at last, just very dense, joining down from " + lineData[l].maxJumpLow + " and splitting up from " + lineData[l].maxJumpHigh);
					minSpaceWidth = ((int) normalize(lineData[l].maxJumpHigh, avgLineHeight, lineData[l].lineHeight, 0));
				}
				else pm.setInfo(" ==> still looking problematic");
			}
			else {
				pm.setInfo(" ==> looking OK, joining down from " + lineData[l].maxJumpLow + " and splitting up from " + lineData[l].maxJumpHigh);
				minSpaceWidth = ((int) normalize(lineData[l].maxJumpHigh, avgLineHeight, lineData[l].lineHeight, 0));
			}
			if (minSpaceWidth == -1)
				continue; // let's get back to this trouble maker later
			
			pm.setInfo(" ==> minimum space width: " + minSpaceWidth + " = " + (((float) minSpaceWidth) / lineData[l].lineHeight));
			applyMinSpaceWidth(lineData[l], tokenizer, minSpaceWidth, allWords);
			lineData[l] = null; // no need to bother with this one any longer
		}
		pm.setProgress(100);
		
		//	handle remaining lines, now that we have a dictionary for sensibility checks
		pm.setStep("Handling remaining lines");
		pm.setBaseProgress(75);
		pm.setMaxProgress(100);
		for (int l = 0; l < lineData.length; l++) {
			pm.setProgress((l * 100) / lines.length);
			if (lineData[l] == null)
				continue; // no relevant spaces in this one at all ...
			pm.setInfo("Line " + l + " at " + lineData[l].words[0].getLocalID() + " (font size " + lineData[l].fontSize + ", height " + lineData[l].lineHeight + "): " + ImUtils.getString(lineData[l].words, true, 2));
			
			if (DEBUG_WORD_SPLITTING) {
				System.out.println(" - separator gaps: " + lineData[l].separating);
				System.out.println(" - non-space gaps: " + lineData[l].nonSpace);
				System.out.print("  words: " + lineData[l].words[0].getString());
				for (int w = 1; w < lineData[l].words.length; w++) {
					int gap = (lineData[l].words[w].bounds.left - lineData[l].words[w-1].bounds.right);
					System.out.print(((gap < 100) ? "" : " ") + (((gap < 10) && (-1 < gap)) ? "" : " ") + " " + lineData[l].words[w].getString());
				}
				System.out.println();
				System.out.print("   gaps: " + lineData[l].words[0].getString().replaceAll(".", " "));
				for (int w = 1; w < lineData[l].words.length; w++) {
					int gap = (lineData[l].words[w].bounds.left - lineData[l].words[w-1].bounds.right);
					System.out.print(gap + "" + lineData[l].words[w].getString().replaceAll(".", " "));
				}
				System.out.println();
			}
			
			if (Math.abs(lineData[l].maxJumpLow - allAvgNonSpaceGap.average) > Math.abs(lineData[l].maxJumpLow - allAvgSeparatingGap.average))
				pm.setInfo(" ==> looking problematic, low end of maximum jump closer to separating gap: " + Math.abs(lineData[l].maxJumpLow - allAvgNonSpaceGap.average) + " > " + Math.abs(lineData[l].maxJumpLow - allAvgSeparatingGap.average));
			else if (Math.abs(lineData[l].maxJumpHigh - allAvgNonSpaceGap.average) < Math.abs(lineData[l].maxJumpHigh - allAvgSeparatingGap.average))
				pm.setInfo(" ==> looking problematic, high end of maximum jump closer to non-space gap: " + Math.abs(lineData[l].maxJumpHigh - allAvgNonSpaceGap.average) + " < " + Math.abs(lineData[l].maxJumpHigh - allAvgSeparatingGap.average));
			
			pm.setInfo(" ==> scoring all word gaps:");
			int bestGapScore = 0;
			int bestMinSpaceWidth = -1;
			for (Iterator git = lineData[l].relevant.iterator(); git.hasNext();) {
				Number tGap = ((Number) git.next());
				if (tGap.floatValue() <= 0)
					continue;
				if (DEBUG_WORD_SPLITTING) System.out.println(" - testing " + tGap + " minimum for separating word gaps:");
				int gMinSpaceWidth = ((int) normalize(tGap.floatValue(), avgLineHeight, lineData[l].lineHeight, 0));
				if (DEBUG_WORD_SPLITTING) System.out.println("   - minimum space width is " + gMinSpaceWidth);
				StringBuffer glStr = new StringBuffer(StringUtils.normalizeString(lineData[l].words[0].getString()));
				for (int w = 1; w < lineData[l].words.length; w++) {
					int gap = (lineData[l].words[w].bounds.left - lineData[l].words[w-1].bounds.right);
					if (gap >= gMinSpaceWidth)
						glStr.append(" ");
					glStr.append(StringUtils.normalizeString(lineData[l].words[w].getString()));
				}
				if (DEBUG_WORD_SPLITTING) System.out.println("   - line is " + glStr);
				TokenSequence gTs = tokenizer.tokenize(glStr);
				int gWordCount = 0;
				int gCharCount = 0;
				int gHitWordCount = 0;
				int gHitCharCount = 0;
				int gHitWordPoints = 0;
				for (int t = 0; t < gTs.size(); t++) {
					String wStr = gTs.valueAt(t);
					if (!StringUtils.isWord(wStr))
						continue;
					gWordCount++;
					gCharCount += wStr.length();
					int wCount = allWords.getCount(wStr);
					if (wCount == 0)
						continue;
					gHitWordCount++;
					gHitCharCount += wStr.length();
					gHitWordPoints += (wCount * wStr.length());
				}
				if (DEBUG_WORD_SPLITTING) {
					System.out.println("   ==> got " + gHitWordCount + " out of " + gWordCount + " words in dictionary");
					System.out.println("   ==> got " + gHitCharCount + " out of " + gCharCount + " word chars in dictionary");
				}
				//	int gScore = ((gHitWordPoints * gHitCharCount * gMinSpaceWidth) / gCharCount);
				//	CANNOT FACTOR IN SPACE WIDTH: line with only few _adjacent_ words would get larger gap selected despite lower dictionary hit rate
				int gScore = (((gHitWordPoints * gHitCharCount) / gCharCount) + gMinSpaceWidth);
				//	BETTER: just ensures that larger gap wins for same dictionary hit rate
				if (DEBUG_WORD_SPLITTING) System.out.println("   ==> score is " + gScore);
				if (bestGapScore < gScore) {
					bestGapScore = gScore;
					bestMinSpaceWidth = gMinSpaceWidth;
					if (DEBUG_WORD_SPLITTING) System.out.println("   ==> new best score");
				}
			}
			if (bestMinSpaceWidth == -1)
				continue;
			
			pm.setInfo(" ==> minimum space width: " + bestMinSpaceWidth + " = " + (((float) bestMinSpaceWidth) / lineData[l].lineHeight));
			applyMinSpaceWidth(lineData[l], tokenizer, bestMinSpaceWidth, null);
		}
		pm.setProgress(100);
	}
	
	private static float normalize(float val, int fromDenom, int toDenom, int toSigDigits) {
		return round((val * toDenom), fromDenom, toSigDigits);
	}
	
	private static float round(float nom, int denom, int toSigDigits) {
		return round((nom / denom), toSigDigits);
	}
	
	private static float round(float value, int toSigDigits) {
		int mul = 1;
		if (toSigDigits == 1)
			mul = 10;
		else if (toSigDigits == 2)
			mul = 100;
		else for (int d = 0; d < toSigDigits; d++)
			mul *= 10;
		return (((float) Math.round(value * mul)) / mul);
	}
	
	private static class MedianAverage {
		final int valTotal;
		final int valMed;
		final float medMin;
		final float medMax;
		final float average;
		MedianAverage(int valTotal, int valMed, float medMin, float medMax, float average) {
			this.valTotal = valTotal;
			this.valMed = valMed;
			this.medMin = medMin;
			this.medMax = medMax;
			this.average = average;
		}
		public String toString() {
			return ("" + this.average + " (" + this.valMed + "/" + this.valTotal + ":[" + this.medMin + "," + this.medMax + "])");
		}
	}
	
	private static MedianAverage getMedianAverage(CountingSet values) {
		if (values.isEmpty())
			return new MedianAverage(0, 0, 0, 0, 0);
		int total = values.size();
		int seenValueCount = 0;
		float inSumMin = Float.NaN;
		float inSumMax = 0;
		float valueSum = 0;
		int inSumValueCount = 0;
		for (Iterator vit = values.iterator(); vit.hasNext();) {
			Number value = ((Number) vit.next());
			int count = values.getCount(value);
			seenValueCount += count;
			if ((seenValueCount * 5) < (total * 1))
				continue; // still below 20%, disregard minimum 20% values
			if (Float.isNaN(inSumMin))
				inSumMin = value.floatValue();
			inSumMax = value.floatValue();
			valueSum += (count * value.floatValue());
			inSumValueCount += count;
			if ((seenValueCount * 5) >= (total * 4))
				break; // made it above 80%, disregard maximum 20% values
		}
		return new MedianAverage(values.size(), inSumValueCount, inSumMin, inSumMax, (valueSum / inSumValueCount));
	}
	
	private static boolean applyMinSpaceWidth(LineData lineData, Tokenizer tokenizer, int minSpaceWidth, CountingSet allWords) {
		boolean modified = false;
		for (int w = 1; w < lineData.words.length; w++) {
			int gap = (lineData.words[w].bounds.left - lineData.words[w-1].bounds.right);
			String lwStr = StringUtils.normalizeString(lineData.words[w-1].getString());
			String rwStr = StringUtils.normalizeString(lineData.words[w].getString());
			String wStr = (lwStr + rwStr);
			TokenSequence wTs = tokenizer.tokenize(wStr);
			if (wTs.size() != 1)
				continue;
			if (gap < minSpaceWidth) {
				lineData.words[w-1].setNextRelation(ImWord.NEXT_RELATION_CONTINUE);
				modified = true;
			}
			else if (lineData.words[w-1].getNextRelation() == ImWord.NEXT_RELATION_CONTINUE) {
				lineData.words[w-1].setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
				modified = true;
			}
		}
		if (allWords != null)
			collectWords(lineData, allWords);
		return modified;
	}
	
	private static void collectWords(LineData lineData, CountingSet allWords) {
		for (int w = 0; w < lineData.words.length; w++) {
			String wStr = StringUtils.normalizeString(lineData.words[w].getString());
			if (!StringUtils.isWord(wStr))
				continue;
			if (lineData.words[w].getPreviousRelation() == ImWord.NEXT_RELATION_CONTINUE)
				continue;
			if ((w == 0) && (lineData.words[w].getPreviousRelation() == ImWord.NEXT_RELATION_HYPHENATED))
				continue;
			wStr = StringUtils.normalizeString(ImUtils.getStringFrom(lineData.words[w]));
			if (StringUtils.isWord(wStr))
				allWords.add(wStr);
		}
	}
	
	private static void checkTextFlowBreaks(ImWord[] textStreamHeads, Tokenizer tokenizer, ProgressMonitor pm) {
		
		//	collect all document words to form lookup list, and count them out along the way
		TreeSet docWords = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		int docWordCount = 0;
		pm.setStep("Indexing document words");
		pm.setBaseProgress(0);
		pm.setProgress(0);
		pm.setMaxProgress(10);
		for (int h = 0; h < textStreamHeads.length; h++) {
			
			//	update progress indicator
			pm.setProgress((h * 100) / textStreamHeads.length);
			
			//	do not count tables, labels, artifacts, and deleted text
			if (ImWord.TEXT_STREAM_TYPE_TABLE.equals(textStreamHeads[h].getTextStreamType()))
				continue;
			if (ImWord.TEXT_STREAM_TYPE_LABEL.equals(textStreamHeads[h].getTextStreamType()))
				continue;
			if (ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(textStreamHeads[h].getTextStreamType()))
				continue;
			if (ImWord.TEXT_STREAM_TYPE_DELETED.equals(textStreamHeads[h].getTextStreamType()))
				continue;
			
			//	process text stream
			for (ImWord imw = textStreamHeads[h]; imw != null; imw = imw.getNextWord()) {
				docWordCount++;
				
				//	check and index word
				String imwStr = imw.getString();
				if ((imwStr == null) || (imwStr.trim().length() == 0))
					continue; // nothing to collect here
				imwStr = StringUtils.normalizeString(imwStr).trim();
				if (imwStr.endsWith("-")) {
					imw = imw.getNextWord(); // jump over successor as well, not a full word, either
					if (imw == null)
						break;
					else continue; // not a full word
				}
				if (Gamta.isWord(imwStr))
					docWords.add(imwStr);
			}
		}
		
		//	scan through text streams one by one
		int scanWordCount = 0;
		pm.setStep("Scanning document text streams");
		pm.setBaseProgress(10);
		pm.setProgress(0);
		pm.setMaxProgress(100);
		for (int h = 0; h < textStreamHeads.length; h++) {
			
			//	leave alone tables, labels, artifacts, and deleted text
			if (ImWord.TEXT_STREAM_TYPE_TABLE.equals(textStreamHeads[h].getTextStreamType()))
				continue;
			if (ImWord.TEXT_STREAM_TYPE_LABEL.equals(textStreamHeads[h].getTextStreamType()))
				continue;
			if (ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(textStreamHeads[h].getTextStreamType()))
				continue;
			if (ImWord.TEXT_STREAM_TYPE_DELETED.equals(textStreamHeads[h].getTextStreamType()))
				continue;
			
			//	scan text stream
			for (ImWord imw = textStreamHeads[h]; imw != null; imw = imw.getNextWord()) {
				
				//	update stats and progress indicator
				pm.setProgress((scanWordCount * 100) / docWordCount);
				scanWordCount++;
				
				//	check relationship to successor (we're only after text flow breaks here, no use checking anything inside lines)
				if (!hasTextFlowBreakAfter(imw))
					continue; // nothing to check here
				if (imw.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED)
					continue; // already recognized as hyphenated TODO maybe also double-check these guys ???
				if (imw.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE)
					continue; // compound word (double last name) broken after hyphen
				
				//	check string (we need a dash at the end)
				String imwStr = imw.getString();
				if ((imwStr == null) || (imwStr.trim().length() == 0))
					continue;
				imwStr = StringUtils.normalizeString(imwStr).trim();
				
				//	check for potentially missed hyphenation
				if (imwStr.endsWith("-"))
					checkForHyphenation(imw, imwStr, tokenizer, docWords, pm);
				
				//	also catch plain sentence continuations (two adjacent lower case words)
				else if (imw.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
					checkForSentenceContinuation(imw, imwStr, tokenizer, pm);
			}
		}
	}
	
	private static boolean hasTextFlowBreakAfter(ImWord imw) {
		ImWord nImw = imw.getNextWord();
		if (nImw == null)
			return false;
		if (imw.pageId != nImw.pageId)
			return true; // page break
		if (nImw.bounds.bottom < imw.bounds.top)
			return true; // successor further up, column break
		if (imw.bounds.bottom < nImw.bounds.top)
			return true; // successor further down, line break
		return false; // this one is in line
	}
	
	private static String normalizeString(ImWord fromImw, ImWord toImw) {
		StringBuffer sb = new StringBuffer();
		for (ImWord imw = fromImw; imw != null; imw = imw.getNextWord()) {
			sb.append(StringUtils.normalizeString(imw.getString()));
			if (imw == toImw)
				break;
		}
		return sb.toString();
	}
	
	private static void checkForHyphenation(ImWord imw, String imwStr, Tokenizer tokenizer, TreeSet docWords, ProgressMonitor pm) {
		
		//	check next word for potential continuation
		ImWord nextImw = imw.getNextWord();
		if (nextImw == null)
			return;
		String nextImwStr = nextImw.getString();
		if ((nextImwStr == null) || (nextImwStr.trim().length() == 0))
			return;
		nextImwStr = StringUtils.normalizeString(nextImwStr);
		if (nextImwStr.charAt(0) == Character.toUpperCase(nextImwStr.charAt(0)))
			return; // starting with capital letter, not a word continued
		
		//	attach any joined following words
		ImWord endImw = findConnectedWordEnd(nextImw);
		nextImwStr = normalizeString(nextImw, endImw);
		
		//	do we have a word to continue with?
		if (!Gamta.isWord(nextImwStr))
			return;
		
		//	check for enumeration prepositions (western European languages for now)
		if ("and;or;und;oder;et;ou;y;e;o;u;ed".indexOf(nextImwStr.toLowerCase()) != -1)
			return;
		
		//	add any connected preceding words
		ImWord startImw = findConnectedWordStart(imw, tokenizer);
		imwStr = normalizeString(startImw, imw);
		
		//	do we have a word?
		if (!Gamta.isWord(imwStr))
			return;
		
		//	check de-hyphenation result
		String fullStr = (imwStr.substring(0, (imwStr.length() - "-".length())) + nextImwStr);
		TokenSequence fullTokens = Gamta.newTokenSequence(fullStr, tokenizer);
		if (fullTokens.size() != 1)
			return;
		
		//	do we have a word occurring as a whole?
		if (!docWords.contains(fullStr))
			return;
		
		//	smooth out text stream
		for (ImWord smoothImw = startImw; smoothImw != endImw; smoothImw = smoothImw.getNextWord())
			smoothImw.setNextRelation((smoothImw == imw) ? ImWord.NEXT_RELATION_HYPHENATED : ImWord.NEXT_RELATION_CONTINUE);
		pm.setInfo("De-hyphenated " + imwStr + " and " + nextImwStr + " to " + fullStr + " at " + startImw.getLocalID() + " through " + endImw.getLocalID());
	}
	
	private static void checkForSentenceContinuation(ImWord imw, String imwStr, Tokenizer tokenizer, ProgressMonitor pm) {
		
		//	add any connected preceding words
		ImWord startImw = findConnectedWordStart(imw, tokenizer);
		imwStr = normalizeString(startImw, imw);
		
		//	do we have a word?
		if (!Gamta.isWord(imwStr))
			return;
		if (!StringUtils.containsVowel(imwStr))
			return;
		
		//	check next word for potential sentence continuation
		ImWord nextImw = imw.getNextWord();
		if (nextImw == null)
			return;
		String nextImwStr = nextImw.getString();
		if ((nextImwStr == null) || (nextImwStr.trim().length() == 0))
			return;
		nextImwStr = StringUtils.normalizeString(nextImwStr);
		if (nextImwStr.charAt(0) == Character.toUpperCase(nextImwStr.charAt(0)))
			return; // starting with capital letter, not as safely a sentence continued
		if (!StringUtils.containsVowel(nextImwStr))
			return; // not a real word (be careful with abbreviations)
		
		//	attach any joined following words
		ImWord endImw = findConnectedWordEnd(nextImw);
		nextImwStr = normalizeString(nextImw, endImw);
		
		//	do we have a word to continue with?
		if (!Gamta.isWord(nextImwStr))
			return;
		
		//	connect logical paragraphs
		imw.setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
		pm.setInfo("Continued sentence between " + imwStr + " and " + nextImwStr + " at " + startImw.getLocalID() + " through " + endImw.getLocalID());
	}
	
	private static ImWord findConnectedWordEnd(ImWord imw) {
		ImWord endImw = imw;
		while (endImw.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE) {
			if (endImw.getNextWord() == null)
				break;
			endImw = endImw.getNextWord();
		}
		return endImw;
	}
	
	private static ImWord findConnectedWordStart(ImWord imw, Tokenizer tokenizer) {
		
		//	add any connected preceding words
		ImWord startImw = imw;
		for (ImWord prevImw = imw.getPreviousWord(); prevImw != null; prevImw = prevImw.getPreviousWord()) {
			
			//	check relationship to what we already have
			if (prevImw.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
				break; // let's not bridge into preceding paragraphs
			if (prevImw.getNextWord().bounds.bottom < prevImw.bounds.top)
				break; // let's not bridge upward
			if (prevImw.bounds.bottom < prevImw.getNextWord().bounds.top)
				break; // let's not bridge downward
			if (prevImw.getString() == null)
				break; // little use going beyond this one
			if (prevImw.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE) {}
			else if (((startImw.bounds.left - prevImw.bounds.right) * 10 * 2) < (prevImw.bounds.getHeight() + startImw.bounds.getHeight())) {}
			else break; // too far away, and not explicitly linked
			
			//	check string
			String prevImwStr = StringUtils.normalizeString(prevImw.getString());
			if (prevImw.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE) {}
			else if (Gamta.isWord(prevImwStr)) {}
			else if ("-".equals(prevImwStr)) {}
			else break; // not a word or stray hyphen, and not explicitly linked
			
			//	include current word
			startImw = prevImw;
		}
		
		//	remove preceding words until we have a single token (or are back down to the word we started out with)
		if (startImw != imw) do {
			String imwStr = normalizeString(startImw, imw);
			TokenSequence imwTokens = Gamta.newTokenSequence(imwStr, tokenizer);
			if (imwTokens.size() == 1)
				break; // we have a single consistent token
			startImw = startImw.getNextWord();
		} while (startImw != imw);
		
		//	finally ...
		return startImw;
	}
	
	private static final String COMBINABLE_ACCENTS;
	private static final HashMap COMBINABLE_ACCENT_MAPPINGS = new HashMap();
	static {
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0300'), "grave");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0301'), "acute");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0302'), "circumflex");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0303'), "tilde");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0304'), "macron");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0306'), "breve");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0307'), "dot");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0308'), "dieresis");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0309'), "hook");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u030A'), "ring");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u030B'), "dblacute");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u030F'), "dblgrave");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u030C'), "caron");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0323'), "dotbelow");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0327'), "cedilla");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0328'), "ogonek");
		
		StringBuffer combinableAccentCollector = new StringBuffer();
		ArrayList combinableAccents = new ArrayList(COMBINABLE_ACCENT_MAPPINGS.keySet());
		for (int c = 0; c < combinableAccents.size(); c++) {
			Character combiningChar = ((Character) combinableAccents.get(c));
			combinableAccentCollector.append(combiningChar.charValue());
			String charName = ((String) COMBINABLE_ACCENT_MAPPINGS.get(combiningChar));
			char baseChar = StringUtils.getCharForName(charName);
			if ((baseChar > 0) && (baseChar != combiningChar.charValue())) {
				combinableAccentCollector.append(baseChar);
				COMBINABLE_ACCENT_MAPPINGS.put(new Character(baseChar), charName);
			}
		}
		COMBINABLE_ACCENTS = combinableAccentCollector.toString();
	}
	
	private static int[] getElementArray(CountingSet cs) {
		int[] csElements = new int[cs.elementCount()];
		int csIndex = 0;
		for (Iterator git = cs.iterator(); git.hasNext();)
			csElements[csIndex++] = ((Integer) git.next()).intValue();
		return csElements;
	}
	
	/* Tooling for re-assessing words
	 * - helps merging single-letter "words" into actual words ...
	 * - ... vastly mitigating effort for correcting adverse effects of nasty obfuscation fonts
	 * 
	 * - mechanism:
	 *   - analyze distribution of word gap widths for each line (rationale: justification expands actual inter-word whitespace equally)
	 *   - set word relation to "same word" for below-average word gaps
	 *   - use absolute upper gap width bound for merging as well, for safety ...
	 *   - ... and only merge if words don't tokenize apart
	 *   ==> perform this test for potential merger blocks as a whole
	 * 
	 * - provide for:
	 *   - individual text blocks / regions via context menu
	 *   - whole document via "Tools" menu
	 * 
	 * TEST WITH Lopez_et_al.pdf (words torn into individual letters due to combination of char decoding problems and obfuscation-aimed word rendering)
	 */
//	public static void main(String[] args) throws Exception {
//		InputStream docIn = new BufferedInputStream(new FileInputStream(new File("E:/Eigene Daten/Plazi Workshop - Heraklion 2015/Lopez et al.pdf.charsOk.imf")));
//		ImDocument doc = ImDocumentIO.loadDocument(docIn);
//		docIn.close();
//		Tokenizer tokenizer = ((Tokenizer) doc.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.NO_INNER_PUNCTUATION_TOKENIZER));
//		
//		//	get lines
//		ArrayList docLines = new ArrayList();
//		ImPage[] pages = doc.getPages();
//		for (int p = 0; p < pages.length; p++) {
//			ImRegion[] pageLines = pages[p].getRegions(ImagingConstants.LINE_ANNOTATION_TYPE);
//			Arrays.sort(pageLines, ImUtils.topDownOrder);
////			if (p == 0)
//			docLines.addAll(Arrays.asList(pageLines));
//		}
//		ImRegion[] lines = ((ImRegion[]) docLines.toArray(new ImRegion[docLines.size()]));
//		joinWords(lines, tokenizer, ProgressMonitor.dummy);
//	}
	
	public static void main(String[] args) throws Exception {
		ImDocument doc = ImDocumentIO.loadDocument(new File("E:/Testdaten/PdfExtract/Nauplius.26.e2018015.imdir"));
		Tokenizer tokenizer = ((Tokenizer) doc.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.NO_INNER_PUNCTUATION_TOKENIZER));
		
		//	get lines
		ArrayList docLines = new ArrayList();
		ImPage[] pages = doc.getPages();
		for (int p = 0; p < pages.length; p++) {
			ImRegion[] pageLines = pages[p].getRegions(ImagingConstants.LINE_ANNOTATION_TYPE);
			Arrays.sort(pageLines, ImUtils.topDownOrder);
//			if (p == 0)
			docLines.addAll(Arrays.asList(pageLines));
		}
		ImRegion[] lines = ((ImRegion[]) docLines.toArray(new ImRegion[docLines.size()]));
		splitWords(lines, tokenizer, ProgressMonitor.dummy);
	}
}
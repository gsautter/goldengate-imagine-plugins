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
package de.uka.ipd.idaho.im.imagine.plugins.basic;

import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.LinkedList;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;

import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.TwoClickSelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;

/**
 * This class provides basic actions for editing logical text streams.
 * 
 * @author sautter
 */
public class TextStreamActionProvider extends AbstractSelectionActionProvider {
	
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
			if ((start == end) && (start.getNextWord() != null))
				actions.add(new SelectionAction("Set Next Word Relation", ("Set Relation between '" + start.getString() + "' and its Successor '" + start.getNextWord().getString() + "'")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return false;
					}
					public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
						JMenu pm = new JMenu("Set Next Word Relation");
						ButtonGroup bg = new ButtonGroup();
						final JMenuItem smi = new JRadioButtonMenuItem("Full Word", (start.getNextRelation() == ImWord.NEXT_RELATION_SEPARATE));
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
						if (start.bounds.right <= start.getNextWord().bounds.left) {
							final JMenuItem cmi = new JRadioButtonMenuItem("First Part of Split Word", (start.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE));
							cmi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									if (cmi.isSelected())
										setNextRelation(ImWord.NEXT_RELATION_CONTINUE, invoker);
								}
							});
							pm.add(cmi);
							bg.add(cmi);
						}
						return pm;
					}
					private void setNextRelation(char nextRelation, ImDocumentMarkupPanel invoker) {
						invoker.beginAtomicAction("Set Next Word Relation");
						start.setNextRelation(nextRelation);
						invoker.validate();
						invoker.repaint();
						invoker.endAtomicAction();
					}
				});
			else if (start.getNextWord() == end)
				actions.add(new SelectionAction("Set Word Relation", ("Set word relation between '" + start.getString() + "' and '" + end.getString() + "'")) {
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
						if (start.bounds.right < end.bounds.left) {
							final JMenuItem cmi = new JRadioButtonMenuItem("Same Word", (start.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE));
							cmi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									if (cmi.isSelected())
										setNextRelation(ImWord.NEXT_RELATION_CONTINUE, invoker);
								}
							});
							pm.add(cmi);
							bg.add(cmi);
						}
						return pm;
					}
					private void setNextRelation(char nextRelation, ImDocumentMarkupPanel invoker) {
						invoker.beginAtomicAction("Set Word Relation");
						start.setNextRelation(nextRelation);
						invoker.validate();
						invoker.repaint();
						invoker.endAtomicAction();
					}
				});
			else if (start.getTextStreamId().equals(end.getTextStreamId()) && (start.pageId == end.pageId) && (start.bounds.top < end.centerY) && (end.centerY < start.bounds.bottom) && (end.getTextStreamPos() <= (start.getTextStreamPos() + 20))) {
				boolean singleLine = true;
				for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
					if ((imw.centerY < start.bounds.top) || (start.bounds.bottom < imw.centerY)) {
						singleLine = false;
						break;
					}
					if (imw == end)
						break;
				}
				if (singleLine)
					actions.add(new SelectionAction("Merge Words", "Merge selected words into one.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
								if (imw == end)
									break;
								imw.setNextRelation(ImWord.NEXT_RELATION_CONTINUE);
							}
							return true;
						}
					});
			}
			
			//	merge paragraphs
			if (paragraphsToMerge)
				actions.add(new SelectionAction("Merge Paragraphs", "Merge selected paragraphs into one.") {
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
				actions.add(new SelectionAction("Copy Text", "Copy the selected words to the system clipboard.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(ImUtils.getString(start, end, true)), null);
						return false;
					}
				});
			
			//	finally ...
			return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
		}
		
		//	single word selection
		if (start == end) {
			if (start.getNextWord() != null) {
				actions.add(new SelectionAction("Set Next Word Relation", ("Set Relation between '" + start.getString() + "' and its Successor '" + start.getNextWord().getString() + "'")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return false;
					}
					public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
						JMenu pm = new JMenu("Set Next Word Relation");
						ButtonGroup bg = new ButtonGroup();
						final JMenuItem smi = new JRadioButtonMenuItem("Full Word", (start.getNextRelation() == ImWord.NEXT_RELATION_SEPARATE));
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
						if (start.bounds.right <= start.getNextWord().bounds.left) {
							final JMenuItem cmi = new JRadioButtonMenuItem("First Part of Split Word", (start.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE));
							cmi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									if (cmi.isSelected())
										setNextRelation(ImWord.NEXT_RELATION_CONTINUE, invoker);
								}
							});
							pm.add(cmi);
							bg.add(cmi);
						}
						return pm;
					}
					private void setNextRelation(char nextRelation, ImDocumentMarkupPanel invoker) {
						invoker.beginAtomicAction("Set Next Word Relation");
						start.setNextRelation(nextRelation);
						invoker.validate();
						invoker.repaint();
						invoker.endAtomicAction();
					}
				});
			}
			if (start.getPreviousWord() != null)
				actions.add(new SelectionAction("Cut Stream Before", ("Cut text stream before '" + start.getString() + "'")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						start.setPreviousWord(null);
						return true;
					}
				});
			if (start.getNextWord() != null)
				actions.add(new SelectionAction("Cut Stream After", ("Cut text stream after '" + start.getString() + "'")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						start.setNextWord(null);
						return true;
					}
				});
			actions.add(new TwoClickSelectionAction("Click Predeccessor", "Mark words, and set its predeccessor by clicking another word") {
				public boolean performAction(ImWord secondWord) {
					if (start.getTextStreamId().equals(secondWord.getTextStreamId()) && ((secondWord.pageId > start.pageId) || ((secondWord.pageId == start.pageId) && (secondWord.getTextStreamPos() >= start.getTextStreamPos())))) {
						JOptionPane.showMessageDialog(DialogFactory.getTopWindow(), ("'" + secondWord.getString() + "' cannot be the predecessor of '" + start.getString() + "'\nThey belong to the same logical text stream,\nand '" + start.getString() + "' is a treansitive predecessor of '" + secondWord.getString() + "'"), "Cannot Set Predecessor", JOptionPane.ERROR_MESSAGE);
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
			actions.add(new TwoClickSelectionAction("Click Successor", "Mark words, and set its successor by clicking another word") {
				public boolean performAction(ImWord secondWord) {
					if (start.getTextStreamId().equals(secondWord.getTextStreamId()) && ((secondWord.pageId < start.pageId) || ((secondWord.pageId == start.pageId) && (secondWord.getTextStreamPos() <= start.getTextStreamPos())))) {
						JOptionPane.showMessageDialog(DialogFactory.getTopWindow(), ("'" + secondWord.getString() + "' cannot be the successor of '" + start.getString() + "'\nThey belong to the same logical text stream,\nand '" + start.getString() + "' is a treansitive successor of '" + secondWord.getString() + "'"), "Cannot Set Successor", JOptionPane.ERROR_MESSAGE);
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
			actions.add(new SelectionAction("Make Successor", ("Make '" + end.getString() + "' successor of '" + start.getString() + "'")) {
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
			actions.add(new SelectionAction("Set Word Relation", ("Set word relation between '" + start.getString() + "' and '" + end.getString() + "'")) {
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
					if (start.bounds.right < end.bounds.left) {
						final JMenuItem cmi = new JRadioButtonMenuItem("Same Word", (start.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE));
						cmi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (cmi.isSelected())
									setNextRelation(ImWord.NEXT_RELATION_CONTINUE, invoker);
							}
						});
						pm.add(cmi);
						bg.add(cmi);
					}
					return pm;
				}
				private void setNextRelation(char nextRelation, ImDocumentMarkupPanel invoker) {
					invoker.beginAtomicAction("Set Word Relation");
					start.setNextRelation(nextRelation);
					invoker.validate();
					invoker.repaint();
					invoker.endAtomicAction();
				}
			});
			actions.add(new SelectionAction("Cut Stream", ("Cut text stream between '" + start.getString() + "' and '" + end.getString() + "'")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					start.setNextWord(null);
					return true;
				}
			});
		}
		
		//	multiple words, same stream
		else if (start.getTextStreamId().equals(end.getTextStreamId()) && (start.pageId == end.pageId) && (start.bounds.top < end.centerY) && (end.centerY < start.bounds.bottom) && (end.getTextStreamPos() <= (start.getTextStreamPos() + 20))) {
			boolean singleLine = true;
			for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
				if ((imw.centerY < start.bounds.top) || (start.bounds.bottom < imw.centerY)) {
					singleLine = false;
					break;
				}
				if (imw == end)
					break;
			}
			if (singleLine)
				actions.add(new SelectionAction("Merge Words", "Merge selected words into one.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
							if (imw == end)
								break;
							imw.setNextRelation(ImWord.NEXT_RELATION_CONTINUE);
						}
						return true;
					}
				});
		}
		
		//	merge paragraphs
		if (paragraphsToMerge)
			actions.add(new SelectionAction("Merge Paragraphs", "Merge selected paragraphs into one.") {
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
			actions.add(new SelectionAction("Make Stream", "Make selected words a separate text stream.") {
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
			actions.add(new SelectionAction("Set Text Stream Type", ("Set Type of Text Stream '" + start.getString() + "' Belongs to")) {
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
								invoker.validate();
								invoker.repaint();
								invoker.endAtomicAction();
							}
						});
						pm.add(smi);
						bg.add(smi);
					}
					return pm;
				}
			});
			actions.add(new SelectionAction("Copy Text", "Copy the selected words to the system clipboard.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(ImUtils.getString(start, end, true)), null);
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
	public SelectionAction[] getActions(Point start, Point end, ImPage page, final ImDocumentMarkupPanel idmp) {
		
		//	we're not offering text stream editing if text streams are not visualized
		if (!idmp.areTextStreamsPainted())
			return null;
		
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
		for (int w = 1; w < selectedWords.length; w++) {
			if (selectedWords[w].getPreviousWord() != selectedWords[w-1]) {
				singleContinuousStream = false;
				break;
			}
			if (ImUtils.leftRightTopDownOrder.compare(selectedWords[w], selectedWords[w-1]) < 0) {
				singleContinuousStream = false;
				break;
			}
		}
		
		//	if we have a single continuous selection, we can handle it like a word selection
		if (singleContinuousStream)
			return this.getActions(selectedWords[0], selectedWords[selectedWords.length - 1], idmp);
		
		LinkedList actions = new LinkedList();
		
		//	generically make selected words a separate text stream
		actions.add(new SelectionAction("Make Stream", "Make selected words a separate text stream.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				ImUtils.makeStream(selectedWords, null, null);
				return true;
			}
		});
		
		//	order selected words into a text stream
		actions.add(new SelectionAction("Order Stream", "Order selected words in a text stream left to right and top to bottom.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				ImUtils.orderStream(selectedWords, ImUtils.leftRightTopDownOrder);
				return true;
			}
		});
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
}
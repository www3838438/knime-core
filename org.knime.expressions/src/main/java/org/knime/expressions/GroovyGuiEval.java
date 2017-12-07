package org.knime.expressions;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import org.scijava.Context;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;

public class GroovyGuiEval<T> extends JFrame {
	
	JTextArea textArea;
	JTextArea resultArea;
	JButton okButton;
	JList<String> inputList;
	JList<String> outputList;
	JComboBox<Language> dropDown;
	
	LinkedList<T> data;
	LinkedList<T> result;
	
	enum Language {
			GROOVY,
			JAVASCRIPT,
			RUBY,
			JYTHON,
			CLOJURE
	};
	
	public GroovyGuiEval() {
		textArea = new JTextArea();
		textArea.setPreferredSize(new Dimension(300, 100));
		
		inputList = new JList<>();
		outputList = new JList<>();
		
		okButton = new JButton("enter");
		okButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				perfomComputation();
			}
		});
		
		dropDown = new JComboBox<>(Language.values());

		resultArea = new JTextArea();
		resultArea.setPreferredSize(new Dimension(300, 100));
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		this.setTitle("Eval Testing");
	}
	
	public void setUp() {
		JPanel superPanel = new JPanel(new GridBagLayout());
		
		GridBagConstraints constraint = new GridBagConstraints();
		constraint.gridx=1;
		constraint.gridy=1;
		constraint.gridx = 1;
		constraint.gridy = 1;
		constraint.fill = constraint.BOTH;
		
		superPanel.add(new JScrollPane(inputList), constraint);
		constraint.gridx++;
		constraint.fill = GridBagConstraints.NONE;
		
		JPanel panel = new JPanel(new GridBagLayout());
		
		GridBagConstraints subConstraint = new GridBagConstraints();
		subConstraint.gridx = 1;
		subConstraint.gridy = 1;
		subConstraint.weightx = 1;
		subConstraint.weighty = 1;
		
		panel.add(dropDown, subConstraint);
		
		subConstraint.gridy++;
		subConstraint.fill = GridBagConstraints.BOTH;
		
		panel.add(textArea, subConstraint);
		
		subConstraint.gridy++;
		panel.add(okButton, subConstraint);
		
		subConstraint.gridy++;
		panel.add(resultArea, subConstraint);
		
		superPanel.add(panel,constraint);
		
		constraint.gridx++;
		constraint.fill = GridBagConstraints.VERTICAL;
		
		superPanel.add(new JScrollPane(outputList), constraint);
		
		this.setLayout(new BorderLayout());
		this.add(superPanel, BorderLayout.CENTER);
//		this.setPreferredSize(new Dimension(500, 300));
		this.pack();
	}
	
	
	
	private void perfomComputation() {
		final Context context = new Context(ScriptService.class);
		final ScriptService scriptService = context.getService(ScriptService.class);
		final String script = textArea.getText();
		ScriptEngine engine = null;
		
		String path = "groovy";
		
		switch((Language) dropDown.getSelectedItem()) {
			case GROOVY: path = "groovy";
				break;
			case JAVASCRIPT: path = "js";
				break;
			case RUBY: path = "rb";
				break;
			case JYTHON: path = "py";
				break;
			case CLOJURE: path = "clj";
		}
		
		
		try {
			result = new LinkedList<>();
			Iterator<T> iter = data.iterator();
			
			ScriptLanguage language = scriptService.getLanguageByExtension(path);
			engine = language.getScriptEngine();
			
			while(iter.hasNext()) {
				engine.put("data", iter.next());
				engine.eval(script);
					
				result.add((T) engine.get("res").toString());
			}
			
			fillOutput();
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		final Object result = engine.get("res");
		
		if(result != null) {
		resultArea.setText(result.toString());}
		else {
			resultArea.setText("");
		}
	}
	
	private void fillInput() {
		Iterator<T> iter = data.iterator();
		
		String[] input = new String[data.size()];
		
		int i = 0;
		
		while(iter.hasNext()) {
			input[i++] = iter.next().toString();
		}
		
		inputList.setListData(input);
	}
	
	private void fillOutput() {
		Iterator<T> iter = result.iterator();
		
		String[] input = new String[data.size()];
		
		int i = 0;
		
		while(iter.hasNext()) {
			input[i++] = iter.next().toString();
		}
		
		outputList.setListData(input);
	}
	
	public static void main(String[] args) {
		GroovyGuiEval<Double> gui = new GroovyGuiEval<Double>();
		
		Random ran = new Random();
		
		gui.data = new LinkedList<Double>();
		
		for(int i = 0; i < 100; i++) {
			gui.data.add((double) ran.nextInt(500));
		}
		
		gui.setUp();
		gui.fillInput();
		gui.setVisible(true);
		
		
		
	}
}

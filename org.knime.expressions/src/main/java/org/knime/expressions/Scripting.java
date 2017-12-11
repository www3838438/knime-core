package org.knime.expressions;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

public abstract class Scripting extends JFrame {
	JTextArea textArea;

	JButton okButton;
	JTable inputTable;
	JTable resultTable;
	JScrollPane resultPane;
	HashMap<String, Integer> nameColumnMap;

	Object[][] data;
	Object[][] result;
	
	JComboBox<Language> dropDown;
	String[] inputNames;
	String[] outputNames;
	
	enum Language {
			GROOVY,
			JAVASCRIPT,
			BEANSHELL
	};
	
	public Scripting() {
		textArea = new JTextArea();
		textArea.setPreferredSize(new Dimension(500, 500));
		
		okButton = new JButton("enter");
		okButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				perfomComputation();
			}
		});
		
		dropDown = new JComboBox<>(Language.values());
		
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
		
		superPanel.add(new JScrollPane(inputTable), constraint);
		inputTable.setFillsViewportHeight(true);
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
		
		superPanel.add(panel,constraint);
		
		constraint.gridx++;
		constraint.fill = GridBagConstraints.VERTICAL;
		
		resultTable = new JTable(new Object[0][0], new String[0]);
		resultPane = new JScrollPane(resultTable);
		superPanel.add(resultPane, constraint);
		
		this.setLayout(new BorderLayout());
		this.add(superPanel, BorderLayout.CENTER);
		this.pack();
	}
	
	
	
	protected abstract void perfomComputation();
	
	protected void fillInput() {
		String filePath = new File("").getAbsolutePath().concat("/data/testdata.csv");
		
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
		    String line = "";
		    String seperator = ",";
		    
		    // Skip header
		    inputNames = reader.readLine().split(seperator);
		    nameColumnMap = new HashMap<>(inputNames.length);
		    
		    for(int i = 0; i < inputNames.length; i++) {
		    	nameColumnMap.put(inputNames[i], i);
		    }
		    
		    for(int i = 0; i < inputNames.length; i++) {
		    	
		    }
		    
		    ArrayList<Object[]> rows = new ArrayList<>();
		    
		    while((line = reader.readLine()) != null) {
		    	String[] dat = line.split(seperator);
		    	
		    	Object[] temp = new Object[6];
		    	
		    	temp[0] = dat[0];
		    	temp[1] = dat[1];
		    	temp[2] = dat[2];
		    	temp[3] = Integer.parseInt(dat[3]);
		    	temp[4] = Double.parseDouble(dat[4]);
		    	temp[5] = Boolean.parseBoolean(dat[5]);
		    	
		    	rows.add(temp);
		    }
		    
		    data = new Object[rows.size()][];
		    for(int i = 0; i < rows.size(); i++) {
		    	data[i] = rows.get(i);
		    }
		} catch (IOException x) {
		    System.err.format("IOException: %s%n", x);
		}
		
		inputTable = new JTable(data, inputNames);
		inputTable.setVisible(true);
	}
	
	protected void fillOutput() {
		resultPane.remove(resultTable);
		
		resultTable = new JTable(result, outputNames);
		resultPane.add(resultTable);
		resultPane.setViewportView(resultTable);
		resultPane.revalidate();
		resultPane.repaint();
		this.pack();
	}
}

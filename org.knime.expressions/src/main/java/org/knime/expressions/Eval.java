package org.knime.expressions;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.scijava.Context;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptService;

public class Eval extends Scripting {
	
	public Eval() {
		super();
		this.setTitle("Eval Testing");
	}
	
	protected void perfomComputation() {
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
			case BEANSHELL: path = "bsh";
		}
		
		/* Search for input and output variables. */
		String[] tempInput = script.split("\n");
		String[] inputVar = null;
		String[] outputVar = null;
		
		boolean oneSet = false;
			
		for(String t : tempInput) {
			if(t.contains("@INPUT")) {
				inputVar = t.split(" ");
				
				if(oneSet) {
					break;
				}
				
				oneSet = true;
			} else if(t.contains("@OUTPUT")) {
				outputVar = t.split(" ");
				
				
				if(oneSet) {
					break;
				}
				
				oneSet = true;
			}
		}
		
		/* Run the given script. */
		try {
			if(outputVar != null) {
				result = new Object[data.length][outputVar.length-1];
			} else {
				result = new Object[data.length][1];
			}
			
			ScriptLanguage language = scriptService.getLanguageByExtension(path);
			engine = language.getScriptEngine();
			
			
			for(int i = 0; i < data.length; i++) {
				Object[] row = data[i];
				
				/* Add input to script. */
				if(inputVar != null) {
					for(String temp : inputVar) {
						if(temp.contains("@INPUT")) {
							continue;
						}
					
						engine.put(temp, row[nameColumnMap.get(temp)]);
					}
				}
				
				Object tempResult = engine.eval(script);
				
				if(outputVar == null) {
					result[i][0] = tempResult;
					outputNames = new String[]{"result"};
				} else {
					outputNames = new String[outputVar.length-1];
					for(int j = 0; j < outputVar.length-1; j++) {
						result[i][j] = engine.get(outputVar[j+1]);
						outputNames[j] = outputVar[j+1];
					}
				}
			}
			
			fillOutput();
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static void main(String[] args) {
		Eval eval = new Eval();
		
		eval.fillInput();
		eval.setUp();
		eval.setVisible(true);
	}
}

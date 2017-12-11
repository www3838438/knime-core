package org.knime.expressions;

import java.util.concurrent.ExecutionException;

import org.scijava.Context;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;

public class Run extends Scripting {
	
	public Run() {
		super();
		this.setTitle("Run Testing");
	}
	
	protected void perfomComputation() {
		final Context context = new Context(ScriptService.class);
		final ScriptService scriptService = context.getService(ScriptService.class);
		final String script = textArea.getText();
		ScriptModule module = null;
		
		String path = "add.groovy";
		
		switch((Language) dropDown.getSelectedItem()) {
			case GROOVY: path = "add.groovy";
				break;
			case JAVASCRIPT: path = "add.js";
				break;
			case BEANSHELL: path = "add.bsh";
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
		
		
		try {
			if(outputVar != null) {
				result = new Object[data.length][outputVar.length/2];
			} else {
				result = new Object[data.length][1];
			}
			
			Object[] tempIn = null;
			
			for(int i = 0; i < data.length; i++) {
				Object[] row = data[i];
				
				/* Add input to script. */
				if(inputVar != null) {
					tempIn = new Object[inputVar.length-1];
					
					for(int k = 0; k < (inputVar.length-1)/2; k++) {
						tempIn[2*k] = inputVar[(k+1)*2];
						tempIn[2*k+1] = row[nameColumnMap.get(inputVar[(k+1)*2])];
					}
				}
				
				module = scriptService.run(path, script, true, tempIn).get();
					
				if(outputVar == null) {
					result[i][0] = module.getReturnValue();
					outputNames = new String[]{"result"};
				} else {
					outputNames = new String[outputVar.length/2];
					for(int j = 0; j < (outputVar.length-1)/2; j++) {
						result[i][j] = module.getOutput(outputVar[(j+1)*2]);
						outputNames[j] = outputVar[(j+1)*2];
					}
				}
			}
			
			fillOutput();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static void main(String[] args) {
		Run run = new Run();
		
		run.fillInput();
		run.setUp();
		run.setVisible(true);	
	}
}

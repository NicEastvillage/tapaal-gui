package dk.aau.cs.gui.components;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

import dk.aau.cs.verification.batchProcessing.BatchProcessingVerificationResult;

public class BatchProcessingResultsTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;
	private final String[] HEADINGS = new String[]{ "Model", "Query", "Result", "Verification Time" }; // TODO: add expected result and maybe error columns
	private List<BatchProcessingVerificationResult> results;
	
	public BatchProcessingResultsTableModel(){
		results = new ArrayList<BatchProcessingVerificationResult>();
	}
	
	public void AddResult(BatchProcessingVerificationResult result){
		int lastRow = results.size();
		results.add(result);
		fireTableRowsInserted(lastRow, lastRow);
	}
		

	public String getColumnName(int column) {
		return HEADINGS[column];
	}
	
	
	public int getColumnCount() {
		return HEADINGS.length;
	}

	
	public int getRowCount() {
		return results.size();
	}


	public Object getValueAt(int row, int col) {
		BatchProcessingVerificationResult result = results.get(row);
		
		switch(col){
		case 0: return result.modelFile();
		case 1: return result.query();
		case 2: return result.verificationResult();
		case 3: return (result.verificationTimeInMs() / 1000.0) + " s";
		default:
			return null;
		}
	}

	public void clear() {
		results.clear();
		fireTableDataChanged();
	}
	
	public Class getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

	public Iterable<BatchProcessingVerificationResult> getResults() {
		return results;
	}

}
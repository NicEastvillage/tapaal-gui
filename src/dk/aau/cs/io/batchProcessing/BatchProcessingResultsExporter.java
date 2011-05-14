package dk.aau.cs.io.batchProcessing;

import java.io.File;
import java.io.PrintStream;

import pipe.dataLayer.TAPNQuery;
import pipe.dataLayer.TAPNQuery.SearchOption;

import dk.aau.cs.translations.ReductionOption;
import dk.aau.cs.verification.batchProcessing.BatchProcessingVerificationResult;

public class BatchProcessingResultsExporter {
	private static final String name_verifyTAPN = "VerifyTAPN";
	private static final String name_OPTIMIZEDSTANDARD = "UPPAAL: Optimised Standard Reduction";
	private static final String name_STANDARD = "UPPAAL: Standard Reduction";
	private static final String name_BROADCAST = "UPPAAL: Broadcast Reduction";
	private static final String name_BROADCASTDEG2 = "UPPAAL: Broadcast Degree 2 Reduction";
	private static final String name_BFS = "Breadth First Search";
	private static final String name_DFS = "Depth First Search";
	private static final String name_RandomDFS = "Random Depth First Search";
	private static final String DELIMITER = ";";

	public void exportToCSV(Iterable<BatchProcessingVerificationResult> results, File outputFile) throws Exception {
		PrintStream writer = new PrintStream(outputFile);
		
		writer.println("Model" + DELIMITER + "Query" + DELIMITER + "Result" + DELIMITER + "Verification Time" + DELIMITER + "Query Property" + DELIMITER + "Extra Tokens" + DELIMITER + "Search Order" + DELIMITER + "Symmetry" + DELIMITER + "Verification Method");
		
		for(BatchProcessingVerificationResult result : results) {
			TAPNQuery query = result.query();
			
			StringBuilder s = new StringBuilder();
			
			
			
			s.append(result.modelFile());
			s.append(DELIMITER);
			s.append(query != null ? query.getName() : "");
			s.append(DELIMITER);
			s.append(result.verificationResult());
			s.append(DELIMITER);
			s.append((result.verificationTimeInMs() / 1000.0) + " s");
			s.append(DELIMITER);
			s.append(query != null ? query.getProperty().toString() : "");
			s.append(DELIMITER);
			s.append(query != null ? query.getCapacity() : "");
			s.append(DELIMITER);
			s.append(query != null ? getSearchOrder(query) : "");
			s.append(DELIMITER);
			s.append(query != null ? (query.useSymmetry() ? "Yes" : "No") : "");
			s.append(DELIMITER);
			s.append(query != null ? getVerificationMethod(query) : "");
			
			writer.println(s.toString());
		}
	}

	private String getVerificationMethod(TAPNQuery query) {
		SearchOption search = query.getSearchOption();
		
		if(search == SearchOption.DFS)
			return name_DFS;
		else if(search == SearchOption.RDFS)
			return name_RandomDFS;
		else 
			return name_BFS;
		
	}

	private Object getSearchOrder(TAPNQuery query) {
		ReductionOption reduction = query.getReductionOption();
		
		if(reduction == ReductionOption.STANDARD)
			return name_STANDARD;
		else if(reduction == ReductionOption.OPTIMIZEDSTANDARD)
			return name_OPTIMIZEDSTANDARD;
		else if(reduction == ReductionOption.DEGREE2BROADCAST)
			return name_BROADCASTDEG2;
		else if(reduction == ReductionOption.VerifyTAPN)
			return name_verifyTAPN;
		else
			return name_BROADCAST;
	}
}
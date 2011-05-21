package dk.aau.cs.model.tapn;

import dk.aau.cs.TCTL.TCTLAFNode;
import dk.aau.cs.TCTL.TCTLAbstractProperty;
import dk.aau.cs.TCTL.TCTLEFNode;
import dk.aau.cs.TCTL.TCTLEGNode;
import dk.aau.cs.verification.QueryType;

public class TAPNQuery {
	private TCTLAbstractProperty property;
	private int extraTokens = 0;

	public TCTLAbstractProperty getProperty() {
		return property;
	}

	public TAPNQuery(TCTLAbstractProperty inputProperty, int extraTokens) {
		this.property = inputProperty;
		this.extraTokens = extraTokens;
	}

	public int getExtraTokens() {
		return extraTokens;
	}
	
	public QueryType queryType(){
		if(property instanceof TCTLEFNode) return QueryType.EF;
		else if(property instanceof TCTLEGNode) return QueryType.EG;
		else if(property instanceof TCTLAFNode) return QueryType.AF;
		else return QueryType.AG;
	}

	@Override
	public String toString() {
		return property.toString();
	}
}

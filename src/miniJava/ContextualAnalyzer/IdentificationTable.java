package miniJava.ContextualAnalyzer;

import java.util.HashMap;
import java.util.Stack;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.Identifier;

public class IdentificationTable {

	private ErrorReporter errorReporter;
	
	public static final int PREDEFINED_SCOPE = 0;
	public static final int CLASS_NAME_SCOPE = 1;
	public static final int MEMBER_NAME_SCOPE = 2;
	public static final int PARAMETER_NAME_SCOPE = 3;
	public static final int LOCAL_SCOPE = 4;
	
	Stack<HashMap<String, Declaration>> scopes = new Stack<HashMap<String, Declaration>>();
	
	public IdentificationTable(ErrorReporter reporter) {
		errorReporter = reporter;
		//Open predefined scope
		openScope();
	}

	// Opens a new level in the identification table, 1 higher than the
	// current topmost level.

	public void openScope() {
		scopes.add(new HashMap<String, Declaration>());
	}

	// Closes the topmost level in the identification table, discarding
	// all entries belonging to that level.

	public void closeScope() {
		if(scopes.size() > 1)
			scopes.pop();
		else
			errorReporter.reportError("Cannot close predefined scope.");
	}

	// Makes a new entry in the identification table for the given identifier
	// and attribute. The new entry belongs to the current highest scope
	// Results in an error if the identifier has already been declared

	public void enter(String id, Declaration decl) {
		
		// Declarations at level 4 or higher may not hide declarations at levels 3 or higher.
		for(int i = PARAMETER_NAME_SCOPE; i < scopes.size(); i++){
			if(scopes.get(i).containsKey(id)){
				errorReporter.reportError(id + " has already been declared.");
				return;
			}
		}
		HashMap<String, Declaration> currentScope = scopes.peek();
		// Checks that declaration has not been made in currentScope if currentScope is level 2 or below
		if(!currentScope.containsKey(id)){
			currentScope.put(id, decl);
		}
		else{
			errorReporter.reportError(id + " has already been declared.");
		}
	}

	// Finds an entry for the given identifier in the identification table,
	// if any. If there are several entries for that identifier, finds the
	// entry at the highest level, in accordance with the scope rules.
	// Returns null iff no entry is found.
	// otherwise returns the attribute field of the entry found.

	public Declaration retrieve(String id) {
		HashMap<String, Declaration> currentScope = scopes.peek();
		int currentScopeLevel = scopes.indexOf(currentScope);
		while(currentScopeLevel >= 0){
			Declaration decl = currentScope.get(id);
			if(decl != null){
				return decl;
			} else if (currentScopeLevel > 0) {
				currentScopeLevel--;
				currentScope = scopes.get(currentScopeLevel);
			} else
				break;
		}
		return null;
	}
	
	public Declaration checkMemberDecls(Declaration decl){
		HashMap<String, Declaration> classDecls = scopes.get(1);
		Declaration matchingDecl = classDecls.get(decl.name);
		return matchingDecl;
	}

}

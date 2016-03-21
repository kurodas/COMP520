package miniJava.SyntacticAnalyzer;

public class SourcePosition {

	  public int line, column;

	  public SourcePosition () {
	    line = 1;
	    column = 1;
	  }

	  public SourcePosition (int l, int c) {
	    line = l;
	    column = c;
	  }
	  
	  public SourcePosition(SourcePosition position){
		  line = position.line;
		  column = position.column;
				  
	  }

	  public String toString() {
	    return "line: " + line + " column: " + column + ")";
	  }
}

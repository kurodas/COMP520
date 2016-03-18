package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.Declaration;

public class IdEntry {
	  protected String id;
	  protected Declaration attr;
	  protected int level;
	  protected IdEntry previous;

	  IdEntry (String id, Declaration attr, int level, IdEntry previous) {
	    this.id = id;
	    this.attr = attr;
	    this.level = level;
	    this.previous = previous;
	  }
}

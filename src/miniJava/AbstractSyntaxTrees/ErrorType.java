package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ErrorType extends Type {

	public ErrorType(TypeKind typ, SourcePosition posn) {
		super(TypeKind.ERROR, posn);
	}

	@Override
	public <A, R> R visit(Visitor<A, R> v, A o) {
		return v.visitErrorType(this, o);
	}

}

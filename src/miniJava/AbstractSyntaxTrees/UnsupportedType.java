package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

public class UnsupportedType extends ClassType{
	public UnsupportedType(Identifier cn, SourcePosition posn){
		super(new Identifier(new Token(TokenKind.ID, "String")), null);
		super.typeKind = TypeKind.UNSUPPORTED;
    }
            
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitUnsupportedType(this, o);
    }

}

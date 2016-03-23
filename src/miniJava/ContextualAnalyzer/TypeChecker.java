package miniJava.ContextualAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ArrayType;
import miniJava.AbstractSyntaxTrees.AssignStmt;
import miniJava.AbstractSyntaxTrees.BaseType;
import miniJava.AbstractSyntaxTrees.BinaryExpr;
import miniJava.AbstractSyntaxTrees.BlockStmt;
import miniJava.AbstractSyntaxTrees.BooleanLiteral;
import miniJava.AbstractSyntaxTrees.CallExpr;
import miniJava.AbstractSyntaxTrees.CallStmt;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassDeclList;
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.ErrorType;
import miniJava.AbstractSyntaxTrees.ExprList;
import miniJava.AbstractSyntaxTrees.Expression;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IndexedRef;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.IxAssignStmt;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.NewArrayExpr;
import miniJava.AbstractSyntaxTrees.NewObjectExpr;
import miniJava.AbstractSyntaxTrees.NullLiteral;
import miniJava.AbstractSyntaxTrees.Operator;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.ParameterDeclList;
import miniJava.AbstractSyntaxTrees.QualifiedRef;
import miniJava.AbstractSyntaxTrees.RefExpr;
import miniJava.AbstractSyntaxTrees.ReturnStmt;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.StatementList;
import miniJava.AbstractSyntaxTrees.Terminal;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.Type;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.UnsupportedType;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class TypeChecker implements Visitor<Object, Type> {

	ErrorReporter reporter;
	
	public TypeChecker(ErrorReporter reporter){
		this.reporter = reporter;
	}
	
	public void check(AST ast){
		ast.visit(this, null);
	}
	
	public boolean checkTypeEquality(AST node1, AST node2) {
		Type type1 = node1.visit(this, new Object());
		Type type2 = node2.visit(this, new Object());
		if (type1.typeKind == TypeKind.CLASS
				&& type2.typeKind == TypeKind.CLASS) {
			return ((ClassType) type1).className
					.equals(((ClassType) type2).className);
		} else if (type1.typeKind == TypeKind.ERROR
				|| type2.typeKind == TypeKind.ERROR) {
			return true;
		} else
			return type1.typeKind == type2.typeKind;
	}

	// /////////////////////////////////////////////////////////////////////////////
	//
	// PACKAGE
	//
	// /////////////////////////////////////////////////////////////////////////////

	public Type visitPackage(Package prog, Object arg) {
		ClassDeclList cl = prog.classDeclList;
		for (ClassDecl c : prog.classDeclList) {
			c.visit(this, arg);
		}
		return null;
	}

	// /////////////////////////////////////////////////////////////////////////////
	//
	// DECLARATIONS
	//
	// /////////////////////////////////////////////////////////////////////////////

	public Type visitClassDecl(ClassDecl clas, Object arg) {
		for (FieldDecl f : clas.fieldDeclList)
			f.visit(this, arg);
		for (MethodDecl m : clas.methodDeclList)
			m.visit(this, arg);
		return null;
	}

	public Type visitFieldDecl(FieldDecl f, Object arg) {
		f.type.visit(this, arg);
		return null;
	}

	public Type visitMethodDecl(MethodDecl m, Object arg) {
		m.type.visit(this, arg);
		ParameterDeclList pdl = m.parameterDeclList;
		for (ParameterDecl pd : pdl) {
			pd.visit(this, arg);
		}
		StatementList sl = m.statementList;
		for (Statement s : sl) {
			s.visit(this, arg);
		}
		return null;
	}

	public Type visitParameterDecl(ParameterDecl pd, Object arg) {
		pd.type.visit(this, arg);
		return null;
	}

	public Type visitVarDecl(VarDecl vd, Object arg) {
		vd.type.visit(this, arg);
		return null;
	}

	// /////////////////////////////////////////////////////////////////////////////
	//
	// TYPES
	//
	// /////////////////////////////////////////////////////////////////////////////

	public Type visitBaseType(BaseType type, Object arg) {
		return type;
	}

	public Type visitClassType(ClassType type, Object arg) {
		return type;
	}

	public Type visitArrayType(ArrayType type, Object arg) {
		return type.eltType.visit(this, arg);
	}

	public Type visitUnsupportedType(UnsupportedType type, Object arg) {
		return type;
	}

	public Type visitErrorType(ErrorType type, Object arg) {
		return type;
	}

	// /////////////////////////////////////////////////////////////////////////////
	//
	// STATEMENTS
	//
	// /////////////////////////////////////////////////////////////////////////////

	public Type visitBlockStmt(BlockStmt stmt, Object arg) {
		StatementList sl = stmt.sl;
		for (Statement s : sl) {
			s.visit(this, arg);
		}
		return null;
	}

	public Type visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		stmt.varDecl.visit(this, arg);
		stmt.initExp.visit(this, arg);
		return null;
	}

	public Type visitAssignStmt(AssignStmt stmt, Object arg) {
		stmt.ref.visit(this, arg);
		stmt.val.visit(this, arg);
		return null;
	}

	public Type visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		stmt.ixRef.visit(this, arg);
		stmt.val.visit(this, arg);
		return null;
	}

	public Type visitCallStmt(CallStmt stmt, Object arg) {
		stmt.methodRef.visit(this, arg);
		ExprList al = stmt.argList;
		for (Expression e : al) {
			e.visit(this, arg);
		}
		return null;
	}

	public Type visitReturnStmt(ReturnStmt stmt, Object arg) {
		if (stmt.returnExpr != null)
			stmt.returnExpr.visit(this, arg);
		return null;
	}

	public Type visitIfStmt(IfStmt stmt, Object arg) {
		stmt.cond.visit(this, arg);
		stmt.thenStmt.visit(this, arg);
		if (stmt.elseStmt != null)
			stmt.elseStmt.visit(this, arg);
		return null;
	}

	public Type visitWhileStmt(WhileStmt stmt, Object arg) {
		if(stmt.cond.visit(this, arg).typeKind != TypeKind.BOOLEAN)
			
		stmt.cond.visit(this, arg);
		stmt.body.visit(this, arg);
		return null;
	}

	// /////////////////////////////////////////////////////////////////////////////
	//
	// EXPRESSIONS
	//
	// /////////////////////////////////////////////////////////////////////////////

	public Type visitUnaryExpr(UnaryExpr expr, Object arg) {
		if(expr.expr.visit(this, arg).typeKind == TypeKind.BOOLEAN
				&& expr.operator.spelling.equals("!"))
			return new BaseType(TypeKind.BOOLEAN,expr.posn);
		else if(expr.expr.visit(this, arg).typeKind == TypeKind.INT
				&& expr.operator.spelling.equals("-"))
			return new BaseType(TypeKind.INT, expr.posn);
		else
			return new ErrorType(TypeKind.ERROR, expr.posn);
	}

	public Type visitBinaryExpr(BinaryExpr expr, Object arg) {
		expr.operator.visit(this, arg);
		Type leftType = expr.left.visit(this, arg);
//		Type rightType = expr.right.visit(this, arg);
		if (leftType.typeKind == TypeKind.INT
				&& checkTypeEquality(expr.left, expr.right)
				&& (expr.operator.spelling.equals(">")
						|| expr.operator.spelling.equals("<")
						|| expr.operator.spelling.equals(">")
						|| expr.operator.spelling.equals("<=")
						|| expr.operator.spelling.equals(">=")
						|| expr.operator.spelling.equals("+")
						|| expr.operator.spelling.equals("-")
						|| expr.operator.spelling.equals("*") || expr.operator.spelling
							.equals("/"))) {
			return new BaseType(TypeKind.INT, expr.posn);
		} else if (leftType.typeKind == TypeKind.BOOLEAN
				&& checkTypeEquality(expr.left, expr.right)
				&& (expr.operator.spelling.equals("||") 
						|| expr.operator.spelling.equals("&&"))) {
			return new BaseType(TypeKind.BOOLEAN, expr.posn);
		}
		else if(checkTypeEquality(expr.left, expr.right)
				&& (expr.operator.spelling.equals("=="))
				|| expr.operator.spelling.equals("!=")){
			return new BaseType(TypeKind.BOOLEAN, expr.posn);
		}
		else{
			return new ErrorType(TypeKind.BOOLEAN, expr.posn);
		}
	}

	public Type visitRefExpr(RefExpr expr, Object arg) {
		return expr.ref.visit(this, arg);
	}

	public Type visitCallExpr(CallExpr expr, Object arg) {
		expr.functionRef.visit(this, arg);
		ExprList al = expr.argList;
		ParameterDeclList pdl = ((MethodDecl)expr.functionRef.decl).parameterDeclList;
		Boolean typesMatch = true;
		int i = 0;
		SourcePosition mismatchPos = null;
		//Check that each argument matches the parameter type
		for (Expression e : al) {
			typesMatch = checkTypeEquality(e.visit(this, arg), pdl.get(i));
			if(!typesMatch){
				mismatchPos = e.posn;
				break;
			}
			i++;
		}
		if (typesMatch)
			return expr.functionRef.visit(this, arg);
		else
			return new ErrorType(TypeKind.ERROR, mismatchPos);
	}

	public Type visitLiteralExpr(LiteralExpr expr, Object arg) {
		return expr.lit.visit(this, arg);
	}

	public Type visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		if(expr.sizeExpr.visit(this, arg).typeKind == TypeKind.INT){
			return expr.eltType.visit(this, arg);
		}
		else{
			return new ErrorType(TypeKind.ERROR, expr.sizeExpr.posn);
		}
	}

	public Type visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		return expr.classtype.visit(this, arg);
	}

	// /////////////////////////////////////////////////////////////////////////////
	//
	// REFERENCES
	//
	// /////////////////////////////////////////////////////////////////////////////

	public Type visitQualifiedRef(QualifiedRef qr, Object arg) {
//		qr.ref.visit(this, arg);
//		qr.id.visit(this, arg);
		return qr.id.visit(this, arg);
	}

	public Type visitIndexedRef(IndexedRef ir, Object arg) {
		if(ir.indexExpr.visit(this, arg).typeKind == TypeKind.INT){
			return ir.idRef.visit(this, arg);
		}
		else{
			return new ErrorType(TypeKind.ERROR, ir.indexExpr.posn);
		}
	}

	public Type visitIdRef(IdRef ref, Object arg) {
		return ref.id.visit(this, arg);
	}

	public Type visitThisRef(ThisRef ref, Object arg) {
		return ref.decl.type;
	}

	// /////////////////////////////////////////////////////////////////////////////
	//
	// TERMINALS
	//
	// /////////////////////////////////////////////////////////////////////////////

	public Type visitIdentifier(Identifier id, Object arg) {
		return id.decl.type;
	}

	public Type visitOperator(Operator op, Object arg) {
		return null;
	}

	public Type visitIntLiteral(IntLiteral num, Object arg) {
		return new BaseType(TypeKind.INT, num.posn);
	}

	public Type visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		return new BaseType(TypeKind.BOOLEAN,bool.posn);
	}

	@Override
	public Type visitNullLiteral(NullLiteral nullLit, Object arg) {
		return new BaseType(TypeKind.NULL, nullLit.posn);
	}
	
	private void reportTypeError(Terminal leaf) {
		reporter.reportError("***At "+ leaf.posn.toString() + ", \"" + leaf.spelling + "\" is not declared");
	}
}

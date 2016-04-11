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
	
	public boolean checkTypeEquality(Type type1, Type type2) {
		// Type type1 = node1.visit(this, new Object());
		// Type type2 = node2.visit(this, new Object());
		if (type1.typeKind == TypeKind.CLASS
				&& type2.typeKind == TypeKind.CLASS) {
			return ((ClassType) type1).className
					.equals(((ClassType) type2).className);
		} else if ((type1.typeKind == TypeKind.CLASS && type2.typeKind == TypeKind.NULL)
				|| (type1.typeKind == TypeKind.NULL && type2.typeKind == TypeKind.CLASS)
				|| (type1.typeKind == TypeKind.NULL && type2.typeKind == TypeKind.NULL)
				|| (type1.typeKind == TypeKind.ERROR || type2.typeKind == TypeKind.ERROR)) {
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
//		ClassDeclList cl = prog.classDeclList;
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
		return clas.type;
	}

	public Type visitFieldDecl(FieldDecl f, Object arg) {
		f.type.visit(this, arg);
		return f.type;
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
		return m.type;
	}

	public Type visitParameterDecl(ParameterDecl pd, Object arg) {
		pd.type.visit(this, arg);
		return pd.type;
	}

	public Type visitVarDecl(VarDecl vd, Object arg) {
		vd.type.visit(this, arg);
		return vd.type;
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
		Type varDeclType = stmt.varDecl.visit(this, arg);
		Type expType = stmt.initExp.visit(this, arg);
		if(!checkTypeEquality(varDeclType, expType)){
			reportTypeError(varDeclType, expType);
		}
		return null;
	}

	public Type visitAssignStmt(AssignStmt stmt, Object arg) {
		Type refType = stmt.ref.visit(this, arg);
		Type valType = stmt.val.visit(this, arg);
		if(!checkTypeEquality(refType, valType)){
			reportTypeError(valType, refType);
		}
		return null;
	}

	public Type visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		Type ixRefType = stmt.ixRef.visit(this, arg);
		Type valType = stmt.val.visit(this, arg);
		
		if(!checkTypeEquality(ixRefType, valType)){
			reportTypeError(valType, ixRefType);
		}
		return null;
	}

	public Type visitCallStmt(CallStmt stmt, Object arg) {
		stmt.methodRef.type = stmt.methodRef.visit(this, arg);
		ParameterDeclList pdl;
		//stmt.methodRef must be an IdRef or a qualifiedRef
		if(stmt.methodRef instanceof IdRef){
			pdl = ((MethodDecl)((IdRef)stmt.methodRef).id.decl).parameterDeclList;
		}
		else{
			pdl = ((MethodDecl)((QualifiedRef)stmt.methodRef).id.decl).parameterDeclList;
		}
		ExprList al = stmt.argList;
		int i = 0;
		for (Expression e : al) {
			e.type = e.visit(this, arg);
			if(!checkTypeEquality(e.type, pdl.get(i).type)){
				reportTypeError(e.type, pdl.get(i).type);
				break;
			}
			i++;
		}
		return null;
	}

	public Type visitReturnStmt(ReturnStmt stmt, Object arg) {
		if (stmt.returnExpr != null){
			stmt.returnExpr.type = stmt.returnExpr.visit(this, arg);
			if(!checkTypeEquality(stmt.returnExpr.type, stmt.returnType)){
				reportTypeError(stmt.returnExpr.type, stmt.returnType);
			}
		}
		return null;
	}

	public Type visitIfStmt(IfStmt stmt, Object arg) {
		Type conditionType = stmt.cond.visit(this, arg);
		if(conditionType.typeKind != TypeKind.BOOLEAN){
			reportTypeError(conditionType, new BaseType(TypeKind.BOOLEAN, new SourcePosition()));
			return null;
		}
		stmt.thenStmt.visit(this, arg);
		if (stmt.elseStmt != null)
			stmt.elseStmt.visit(this, arg);
		return null;
	}

	public Type visitWhileStmt(WhileStmt stmt, Object arg) {
		Type conditionType = stmt.cond.visit(this, arg);
		if(conditionType.typeKind != TypeKind.BOOLEAN){
			reportTypeError(conditionType, new BaseType(TypeKind.BOOLEAN, new SourcePosition()));
			return null;
		}
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
				&& expr.operator.spelling.equals("!")){
			expr.type = new BaseType(TypeKind.BOOLEAN,expr.posn);
			return expr.type;
		}
		else if(expr.expr.visit(this, arg).typeKind == TypeKind.INT
				&& expr.operator.spelling.equals("-")){
			expr.type = new BaseType(TypeKind.INT, expr.posn);
			return expr.type;
		}
		else if(expr.operator.spelling.equals("-")){
			reportTypeError(expr.expr.type, new BaseType(TypeKind.INT, new SourcePosition()));
			return new ErrorType(TypeKind.ERROR, expr.posn);
		}
		else {//expr.operator.spelling.equals("!")
			reportTypeError(expr.expr.type, new BaseType(TypeKind.BOOLEAN, new SourcePosition()));
			return new ErrorType(TypeKind.ERROR, expr.posn);
		}
	}

	public Type visitBinaryExpr(BinaryExpr expr, Object arg) {
		expr.operator.visit(this, arg);
		Type leftType = expr.left.visit(this, arg);
		Type rightType = expr.right.visit(this, arg);
		if (leftType.typeKind == TypeKind.INT
				&& checkTypeEquality(leftType, rightType)
				&& (expr.operator.spelling.equals(">")
						|| expr.operator.spelling.equals("<")
						|| expr.operator.spelling.equals(">")
						|| expr.operator.spelling.equals("<=")
						|| expr.operator.spelling.equals(">=")
						|| expr.operator.spelling.equals("+")
						|| expr.operator.spelling.equals("-")
						|| expr.operator.spelling.equals("*") || expr.operator.spelling
							.equals("/"))) {
			expr.type = new BaseType(TypeKind.INT, expr.posn);
			return expr.type;
		} else if (leftType.typeKind == TypeKind.BOOLEAN
				&& checkTypeEquality(leftType, rightType)
				&& (expr.operator.spelling.equals("||") 
						|| expr.operator.spelling.equals("&&"))) {
			expr.type = new BaseType(TypeKind.BOOLEAN, expr.posn);
			return expr.type;
		}
		else if(checkTypeEquality(leftType, rightType)
				&& (expr.operator.spelling.equals("=="))
				|| expr.operator.spelling.equals("!=")){
			expr.type = new BaseType(TypeKind.BOOLEAN, expr.posn);
			return expr.type;
		}
		else{
			reportTypeError(leftType, rightType);
			expr.type = new ErrorType(TypeKind.ERROR, expr.posn);
			return expr.type;
		}
	}

	public Type visitRefExpr(RefExpr expr, Object arg) {
		return expr.ref.visit(this, arg);
	}

	public Type visitCallExpr(CallExpr expr, Object arg) {
		expr.functionRef.visit(this, arg);
		ExprList al = expr.argList;
		ParameterDeclList pdl;
		//stmt.methodRef must be an IdRef or a qualifiedRef
		if(expr.functionRef instanceof IdRef){
			pdl = ((MethodDecl)((IdRef)expr.functionRef).id.decl).parameterDeclList;
		}
		else{
			pdl = ((MethodDecl)((QualifiedRef)expr.functionRef).id.decl).parameterDeclList;
		}
		Boolean typesMatch = true;
		int i = 0;
		SourcePosition mismatchPos = null;
		//Check that each argument matches the parameter type
		for (Expression e : al) {
			Type exprType = e.visit(this, arg);
			Type parameterType = pdl.get(i).type;
			typesMatch = checkTypeEquality(exprType, parameterType);
			if(!typesMatch){
				mismatchPos = e.posn;
				reportTypeError(exprType, parameterType);
				break;
			}
			i++;
		}
		if (typesMatch)
			return expr.functionRef.visit(this, arg);
		else{
			return new ErrorType(TypeKind.ERROR, mismatchPos);
		}
	}

	public Type visitLiteralExpr(LiteralExpr expr, Object arg) {
		return expr.lit.visit(this, arg);
	}

	public Type visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		expr.sizeExpr.type = expr.sizeExpr.visit(this, arg);
		if(expr.sizeExpr.type.typeKind == TypeKind.INT){
			expr.eltType = expr.eltType.visit(this, arg);
			return expr.eltType;
		}
		else{
			reportTypeError(expr.sizeExpr.type, new BaseType(TypeKind.INT, new SourcePosition()));
			return new ErrorType(TypeKind.ERROR, expr.sizeExpr.posn);
		}
	}

	public Type visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		expr.type = expr.classtype.visit(this, arg);
		return expr.type;
	}

	// /////////////////////////////////////////////////////////////////////////////
	//
	// REFERENCES
	//
	// /////////////////////////////////////////////////////////////////////////////

	public Type visitQualifiedRef(QualifiedRef qr, Object arg) {
//		qr.ref.visit(this, arg);
//		qr.id.visit(this, arg);
		qr.type = qr.id.visit(this, arg);
		return qr.type;
	}

	public Type visitIndexedRef(IndexedRef ir, Object arg) {
		ir.indexExpr.type = ir.indexExpr.visit(this, arg);
		if(ir.indexExpr.type.typeKind == TypeKind.INT){
			ir.type = ir.idRef.visit(this, arg);
			return ir.type;
		}
		else{
			reportTypeError(ir.indexExpr.type, new BaseType(TypeKind.INT, new SourcePosition()));
			return new ErrorType(TypeKind.ERROR, ir.indexExpr.posn);
		}
	}

	public Type visitIdRef(IdRef ref, Object arg) {
		ref.type = ref.id.visit(this, arg);
		return ref.type;
	}

	public Type visitThisRef(ThisRef ref, Object arg) {
		ref.type = ref.decl.type;
		return ref.type;
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
	
	private void reportTypeError(Type type1, Type type2) {
		if(type1.typeKind == TypeKind.CLASS && type2.typeKind == TypeKind.CLASS){
			reporter.reportError("***At " + type1.posn + ", cannot convert from "+ ((ClassType)type1).className + " to " + ((ClassType)type2).className);
		}
		else if(type1.typeKind == TypeKind.CLASS && type2.typeKind != TypeKind.CLASS){
			reporter.reportError("***At " + type1.posn + ", cannot convert from "+ ((ClassType)type1).className + " to " + type2.typeKind.toString());
		}
		else if(type2.typeKind == TypeKind.CLASS && type1.typeKind != TypeKind.CLASS){
			reporter.reportError("***At " + type1.posn + ", cannot convert from "+ type1.typeKind.toString() + " to " + ((ClassType)type2).className);
		}
	}
}

package miniJava.ContextualAnalyzer;


import miniJava.ErrorReporter;
import miniJava.StandardEnvironment;
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
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.ErrorType;
import miniJava.AbstractSyntaxTrees.ExprList;
import miniJava.AbstractSyntaxTrees.Expression;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.FieldDeclList;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IndexedRef;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.IxAssignStmt;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
import miniJava.AbstractSyntaxTrees.LocalDecl;
import miniJava.AbstractSyntaxTrees.MemberDecl;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.MethodDeclList;
import miniJava.AbstractSyntaxTrees.NewArrayExpr;
import miniJava.AbstractSyntaxTrees.NewObjectExpr;
import miniJava.AbstractSyntaxTrees.NullLiteral;
import miniJava.AbstractSyntaxTrees.Operator;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.ParameterDeclList;
import miniJava.AbstractSyntaxTrees.QualifiedRef;
import miniJava.AbstractSyntaxTrees.RefExpr;
import miniJava.AbstractSyntaxTrees.Reference;
import miniJava.AbstractSyntaxTrees.ReturnStmt;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.StatementList;
import miniJava.AbstractSyntaxTrees.Terminal;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.UnsupportedType;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

public class Identification implements Visitor<Object, Object> {
	private IdentificationTable idTable;
	private ErrorReporter reporter;

	@Override
	public Object visitPackage(Package prog, Object arg) {
		//Open class name scope
		idTable.openScope();
		//Enter all class declarations into idTable first
		for (ClassDecl c : prog.classDeclList) {
			idTable.enter(c.name, c);
		}
		//Open class member scope
		idTable.openScope();
//		for (ClassDecl c : prog.classDeclList) {
//			//Enter all class members and methods into idTable
//			//with c.name prepended
//			for(FieldDecl f: c.fieldDeclList){
//				idTable.enter(c.name + "." + f.name, f);
//			}
//			for(MethodDecl m: c.methodDeclList){
//				idTable.enter(c.name + "." + m.name, m);
//			}
//		}
		//Visit each class
		for (ClassDecl c : prog.classDeclList) {
			c.visit(this, arg);
		}
		//Close class member scope
		idTable.closeScope();
		//Close class name scope
		idTable.closeScope();
		return null;
	}
	///////////////////////////////////////////////////////////////////////////////
	//
	// DECLARATIONS
	//
	///////////////////////////////////////////////////////////////////////////////
    
	@Override
	public Object visitClassDecl(ClassDecl clas, Object arg) {
//		idTable.openScope();
		for (FieldDecl f : clas.fieldDeclList) {
			f.visit(this, arg);
		}
		for (MethodDecl m : clas.methodDeclList) {
			m.visit(this, arg);
		}
//		idTable.closeScope();
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl f, Object arg) {
		idTable.enter(f.name, f);
		f.type.visit(this, arg);
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl m, Object arg) {
		idTable.enter(m.name, m);
		m.type.visit(this, arg);
		ParameterDeclList pdl = m.parameterDeclList;
		//Open parameter name scope
		idTable.openScope();
		for (ParameterDecl pd : pdl) {
			pd.visit(this, arg);
		}
		StatementList sl = m.statementList;
		for (Statement s : sl) {
			s.visit(this, arg);
		}
		idTable.closeScope();
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		idTable.enter(pd.name, pd);
		pd.type.visit(this, arg);
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl vd, Object arg) {
		idTable.enter(vd.name, vd);
		vd.type.visit(this, arg);
		return null;
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// TYPES
	//
	///////////////////////////////////////////////////////////////////////////////

	@Override
	public Object visitBaseType(BaseType type, Object arg) {
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Object arg) {
		Declaration classDecl = idTable.retrieve(type.className.spelling);
		if(classDecl != null){
			type.className.decl = classDecl;
		}
		else{
			reportUndeclared(type.className);
		}
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		type.eltType.visit(this, arg);
		return null;
	}
	
	public Object visitUnsupportedType(UnsupportedType type, Object arg){
		Declaration stringDecl = idTable.retrieve(type.className.spelling);
		if(stringDecl != null){
			type.className.decl = stringDecl;
		}
		else
			reportUndeclared(type.className);
		return null;
	}
	
	public Object visitErrorType(ErrorType type, Object arg){
		return null;
	}

	///////////////////////////////////////////////////////////////////////////////
	//
	// STATEMENTS
	//
	///////////////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		StatementList sl = stmt.sl;
		idTable.openScope();
		for (Statement s : sl) {
			s.visit(this, arg);
		}
		idTable.closeScope();
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		//Visit initExp first to make sure that the variable itself
		//is not being used in the declaration
		stmt.initExp.visit(this, arg);
		stmt.varDecl.visit(this, arg);
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		//Visit value being assigned to first to make sure it has been declared
		stmt.val.visit(this, arg);
		stmt.ref.visit(this, arg);
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		// show(arg,stmt);
		stmt.val.visit(this, arg);
		stmt.ixRef.visit(this, arg);
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		stmt.methodRef.visit(this, arg);
		ExprList al = stmt.argList;
		for (Expression e : al) {
			e.visit(this, arg);
		}
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		if (stmt.returnExpr != null)
			stmt.returnExpr.visit(this, arg);
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		stmt.cond.visit(this, arg);
		stmt.thenStmt.visit(this, arg);
		if (stmt.elseStmt != null)
			stmt.elseStmt.visit(this, arg);
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		stmt.cond.visit(this, arg);
		stmt.body.visit(this, arg);
		return null;
	}

	///////////////////////////////////////////////////////////////////////////////
	//
	// EXPRESSIONS
	//
	///////////////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		expr.operator.visit(this, arg);
		expr.expr.visit(this, arg);
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		expr.operator.visit(this, arg);
		expr.left.visit(this, arg);
		expr.right.visit(this, arg);
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		expr.ref.visit(this, arg);
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		expr.functionRef.visit(this, arg);
		ExprList al = expr.argList;
		for (Expression e : al) {
			e.visit(this, arg);
		}
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		expr.lit.visit(this, arg);
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		expr.classtype.visit(this, arg);
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		expr.eltType.visit(this, arg);
		expr.sizeExpr.visit(this, arg);
		return null;
	}

	@Override
	public Object visitQualifiedRef(QualifiedRef qr, Object arg) {
		qr.ref.visit(this, arg);
		if(qr.ref instanceof ThisRef){
			qr.ref.decl = idTable.retrieve(qr.id.spelling);
			if(qr.ref.decl == null){
				reportUndeclared(((ClassType)qr.ref.type).className);
			}
		}
		else if(qr.ref instanceof IdRef){
			qr.ref.decl = idTable.retrieve(((IdRef)qr.ref).id.spelling);
			if(qr.ref.decl == null){
				reportUndeclared((((IdRef)qr.ref).id));
			}
		}
		qr.id.visit(this, arg);
		return null;
	}

	@Override
	public Object visitIndexedRef(IndexedRef ir, Object arg) {
		ir.indexExpr.visit(this, arg);
		ir.idRef.visit(this, arg);
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		ref.id.visit(this, arg);
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		return null;
	}

	///////////////////////////////////////////////////////////////////////////////
	//
	// TERMINALS
	//
	///////////////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		Declaration decl = idTable.retrieve(id.spelling);
		if(decl != null){
			id.decl = decl;
		}
		else{
			reportUndeclared(id);
		}
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Object arg) {
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral nullLit, Object arg) {
		return null;
	}

	private void reportUndeclared(Terminal leaf) {
		reporter.reportError("***At "+ leaf.posn.toString() + ", \"" + leaf.spelling + "\" is not declared");
	}

	public void check(AST ast){
		ast.visit(this, idTable);
	}
	
	public Identification(ErrorReporter reporter) {
		this.reporter = reporter;
		this.idTable = new IdentificationTable(reporter);
		establishStdEnvironment();
	}

	SourcePosition filler = new SourcePosition(-1, -1);
	
	private ClassDecl declareStdClass(String id, FieldDeclList fieldDeclList,
			MethodDeclList methodDeclList) {
		ClassDecl binding;
		binding = new ClassDecl(id, fieldDeclList, methodDeclList, filler);
		idTable.enter(id, binding);
		return binding;
	}
	
	private void establishStdEnvironment(){	
		FieldDeclList systemFieldDeclList = new FieldDeclList();
		FieldDecl systemMember = new FieldDecl(false,true,new ClassType(new Identifier(new Token(TokenKind.ID, "_PrintStream")),filler),"out",filler);
		systemFieldDeclList.add(systemMember);
		StandardEnvironment.SystemDecl = declareStdClass("System", systemFieldDeclList, new MethodDeclList());
		
		MethodDeclList _PrintStreamMethodDeclList = new MethodDeclList();
		ParameterDeclList printlnParameterList = new ParameterDeclList();
		printlnParameterList.add(new ParameterDecl(new BaseType(TypeKind.INT,filler), "n", filler));
		MethodDecl _PrintStreamMethodDecl = new MethodDecl(new FieldDecl(false,false, new BaseType(TypeKind.VOID,filler),"println",null),printlnParameterList, new StatementList(),filler);
		_PrintStreamMethodDeclList.add(_PrintStreamMethodDecl);
		StandardEnvironment._PrintStreamDecl = declareStdClass("_PrintStream", new FieldDeclList(), _PrintStreamMethodDeclList);
		
		StandardEnvironment.StringDecl = declareStdClass("String",new FieldDeclList(),new MethodDeclList());
		StandardEnvironment.StringDecl.type = new UnsupportedType(new Identifier(new Token(TokenKind.ID,"String")), filler);
	}
}

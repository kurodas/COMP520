package miniJava.ContextualAnalyzer;


import miniJava.ErrorReporter;
import miniJava.StandardEnvironment;
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
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

public class Identification implements Visitor<String, Object> {
	private IdentificationTable idTable;
	private ErrorReporter reporter;
    
//    /**
//     * print text representation of AST to stdout
//     * @param ast root node of AST 
//     */
//    public void showTree(AST ast){
//        System.out.println("======= AST Display =========================");
//        ast.visit(this, "");
//        System.out.println("=============================================");
//    }   
//    
//    // methods to format output
//    
//    /**
//     * display arbitrary text for a node
//     * @param prefix  spaced indent to indicate depth in AST
//     * @param text    preformatted node display
//     */
//    private void show(String prefix, String text) {
//        System.out.println(prefix + text);
//    }
//    
//    /**
//     * display AST node by name
//     * @param prefix  spaced indent to indicate depth in AST
//     * @param node    AST node, will be shown by name
//     */
//    private void show(String prefix, AST node) {
//    	System.out.println(prefix + node.toString());
//    }
    
	/**
	 * quote a string
	 * 
	 * @param text
	 *            string to quote
	 */
	private String quote(String text) {
		return ("\"" + text + "\"");
	}

	/**
	 * increase depth in AST
	 * 
	 * @param prefix
	 *            current spacing to indicate depth in AST
	 * @return new spacing
	 */
	private String indent(String prefix) {
		return prefix + "  ";
	}

	@Override
	public Object visitPackage(Package prog, String arg) {
		//Open class name scope
		idTable.openScope();
		//Enter all class declarations into idTable first
		for (ClassDecl c : prog.classDeclList) {
			idTable.enter(c.name, c);
		}
		//Open class member scope
		idTable.openScope();
		for (ClassDecl c : prog.classDeclList) {
			//Enter all class members and methods into idTable
			//with c.name prepended
			for(FieldDecl f: c.fieldDeclList){
				idTable.enter(c.name + "." + f.name, f);
			}
			for(MethodDecl m: c.methodDeclList){
				idTable.enter(c.name + "." + m.name, m);
			}
		}
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
	public Object visitClassDecl(ClassDecl clas, String arg) {
//		idTable.enter(clas.name, clas);
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
	public Object visitFieldDecl(FieldDecl f, String arg) {
//		idTable.enter(f.name, f);
		f.type.visit(this, indent(arg));
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl m, String arg) {
//		idTable.enter(m.name, m);
		// show(arg, "(" + (m.isPrivate ? "private": "public")
		// + (m.isStatic ? " static) " :") ") + m.toString());
		m.type.visit(this, indent(arg));
		// show(indent(arg), quote(m.name) + " methodname");
		ParameterDeclList pdl = m.parameterDeclList;
		// show(arg, "  ParameterDeclList [" + pdl.size() + "]");
		String pfx = ((String) arg) + "  . ";
		idTable.openScope();
		for (ParameterDecl pd : pdl) {
			pd.visit(this, pfx);
		}
		StatementList sl = m.statementList;
		// show(arg, "  StmtList [" + sl.size() + "]");
		for (Statement s : sl) {
			s.visit(this, pfx);
		}
		idTable.closeScope();
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, String arg) {
		idTable.enter(pd.name, pd);
		pd.type.visit(this, indent(arg));
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl vd, String arg) {
		idTable.enter(vd.name, vd);
		// show(arg, vd);
		vd.type.visit(this, indent(arg));
		// show(indent(arg), quote(vd.name) + " varname");
		return null;
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// TYPES
	//
	///////////////////////////////////////////////////////////////////////////////

	@Override
	public Object visitBaseType(BaseType type, String arg) {
//		show(arg, type.typeKind + " " + type.toString());
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, String arg) {
//		show(arg, type);
		Declaration classDecl = idTable.retrieve(type.className.spelling);
		//Check to see if type has been declared
		if(classDecl != null){
			type.className.decl = classDecl;
		}
		else{
			reportUndeclared(type.className);
		}
//		show(indent(arg), quote(type.className.spelling) + " classname");
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, String arg) {
//		show(arg, type);
		type.eltType.visit(this, indent(arg));
		return null;
	}
	
	public Object visitUnsupportedType(UnsupportedType type, String arg){
		Declaration stringDecl = idTable.retrieve(type.className.spelling);
		if(stringDecl != null){
			type.className.decl = stringDecl;
		}
		else
			reportUndeclared(type.className);
		return null;
	}
	
	public Object visitErrorType(ErrorType type, String arg){
		return null;
	}

	///////////////////////////////////////////////////////////////////////////////
	//
	// STATEMENTS
	//
	///////////////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitBlockStmt(BlockStmt stmt, String arg) {
//		show(arg, stmt);
		StatementList sl = stmt.sl;
//		show(arg, "  StatementList [" + sl.size() + "]");
		String pfx = arg + "  . ";
		idTable.openScope();
		for (Statement s : sl) {
			s.visit(this, pfx);
		}
		idTable.closeScope();
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, String arg) {
		// show(arg, stmt);
		stmt.varDecl.visit(this, indent(arg));
		stmt.initExp.visit(this, indent(arg));
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, String arg) {
		// show(arg,stmt);
		stmt.ref.visit(this, indent(arg));
		stmt.val.visit(this, indent(arg));
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, String arg) {
		// show(arg,stmt);
		stmt.ixRef.visit(this, indent(arg));
		stmt.val.visit(this, indent(arg));
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, String arg) {
		// show(arg,stmt);
		stmt.methodRef.visit(this, indent(arg));
		ExprList al = stmt.argList;
		// show(arg,"  ExprList [" + al.size() + "]");
		String pfx = arg + "  . ";
		for (Expression e : al) {
			e.visit(this, pfx);
		}
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, String arg) {
//		show(arg, stmt);
		if (stmt.returnExpr != null)
			stmt.returnExpr.visit(this, indent(arg));
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, String arg) {
//		show(arg, stmt);
		stmt.cond.visit(this, indent(arg));
		stmt.thenStmt.visit(this, indent(arg));
		if (stmt.elseStmt != null)
			stmt.elseStmt.visit(this, indent(arg));
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, String arg) {
//		show(arg, stmt);
		stmt.cond.visit(this, indent(arg));
		stmt.body.visit(this, indent(arg));
		return null;
	}

	///////////////////////////////////////////////////////////////////////////////
	//
	// EXPRESSIONS
	//
	///////////////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitUnaryExpr(UnaryExpr expr, String arg) {
//		show(arg, expr);
		expr.operator.visit(this, indent(arg));
		expr.expr.visit(this, indent(indent(arg)));
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, String arg) {
//		show(arg, expr);
		expr.operator.visit(this, indent(arg));
		expr.left.visit(this, indent(indent(arg)));
		expr.right.visit(this, indent(indent(arg)));
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, String arg) {
//		show(arg, expr);
		expr.ref.visit(this, indent(arg));
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, String arg) {
//		show(arg, expr);
		expr.functionRef.visit(this, indent(arg));
		ExprList al = expr.argList;
//		show(arg, "  ExprList + [" + al.size() + "]");
		String pfx = arg + "  . ";
		for (Expression e : al) {
			e.visit(this, pfx);
		}
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, String arg) {
//		show(arg, expr);
		expr.lit.visit(this, indent(arg));
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, String arg) {
//		show(arg, expr);
		expr.classtype.visit(this, indent(arg));
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, String arg) {
//		show(arg, expr);
		expr.eltType.visit(this, indent(arg));
		expr.sizeExpr.visit(this, indent(arg));
		return null;
	}

	@Override
	public Object visitQualifiedRef(QualifiedRef qr, String arg) {
//		show(arg, qr);
		qr.id.visit(this, indent(arg));
		qr.ref.visit(this, indent(arg));
		return null;
	}

	@Override
	public Object visitIndexedRef(IndexedRef ir, String arg) {
//		show(arg, ir);
		ir.indexExpr.visit(this, indent(arg));
		ir.idRef.visit(this, indent(arg));
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, String arg) {
//		show(arg, ref);
		ref.id.visit(this, indent(arg));
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, String arg) {
//		show(arg, ref);
		return null;
	}

	///////////////////////////////////////////////////////////////////////////////
	//
	// TERMINALS
	//
	///////////////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitIdentifier(Identifier id, String arg) {
//		show(arg, quote(id.spelling) + " " + id.toString());
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
	public Object visitOperator(Operator op, String arg) {
//		show(arg, quote(op.spelling) + " " + op.toString());
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, String arg) {
//		show(arg, quote(num.spelling) + " " + num.toString());
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, String arg) {
//		show(arg, quote(bool.spelling) + " " + bool.toString());
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral nullLit, String arg) {
//		show(arg, quote(nullLit.spelling) + " " + nullLit.toString());
		return null;
	}

	private void reportUndeclared(Terminal leaf) {
		reporter.reportError("\"" + leaf.spelling + "\" is not declared");
	}

	public void check(Package ast){
		ast.visit(this, null);
	}
	
	public Identification(ErrorReporter reporter) {
		this.reporter = reporter;
		this.idTable = new IdentificationTable(reporter);
		establishStdEnvironment();
	}

	private ClassDecl declareStdClass(String id, FieldDeclList fieldDeclList,
			MethodDeclList methodDeclList) {
		ClassDecl binding;
		binding = new ClassDecl(id, fieldDeclList, methodDeclList, null);
		idTable.enter(id, binding);
		return binding;

	}
	
	private void establishStdEnvironment(){	
		FieldDeclList systemFieldDeclList = new FieldDeclList();
		FieldDecl systemMember = new FieldDecl(false,true,new ClassType(new Identifier(new Token(TokenKind.ID, "_PrintStream")),null),"out",null);
		systemFieldDeclList.add(systemMember);
		StandardEnvironment.SystemDecl = declareStdClass("System", systemFieldDeclList, new MethodDeclList());
		
		MethodDeclList _PrintStreamMethodDeclList = new MethodDeclList();
		ParameterDeclList printlnParameterList = new ParameterDeclList();
		printlnParameterList.add(new ParameterDecl(new BaseType(TypeKind.INT,null), "n", null));
		MethodDecl _PrintStreamMethodDecl = new MethodDecl(new FieldDecl(false,false, new BaseType(TypeKind.VOID,null),"println",null),printlnParameterList, new StatementList(),null);
		_PrintStreamMethodDeclList.add(_PrintStreamMethodDecl);
		StandardEnvironment._PrintStreamDecl = declareStdClass("_PrintStream", new FieldDeclList(), _PrintStreamMethodDeclList);
		
		StandardEnvironment.StringDecl = declareStdClass("String",new FieldDeclList(),new MethodDeclList()); 
	}
}

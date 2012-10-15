//package program.e2c;

public class Parser {

    // need a symbol table
    private Symtab symtab = new Symtab();

    // the first sets.
    // note: we cheat sometimes:
    // when there is only a single token in the set,
    // we generally just compare tkrep with the first token.
    TK f_declaration[] = {TK.VAR, TK.CONST, TK.none};
    TK f_var_decl[] = {TK.VAR, TK.none};
    TK f_const_decl[] = {TK.CONST, TK.none};
    TK f_statement[] = {TK.ID, TK.PRINT, TK.IF, TK.WHILE, TK.REPEAT, TK.FOR, TK.none};
    TK f_print[] = {TK.PRINT, TK.none};
    TK f_assignment[] = {TK.ID, TK.none};
    TK f_if[] = {TK.IF, TK.none};
    TK f_while[] = {TK.WHILE, TK.none};
    TK f_repeat[] = {TK.REPEAT, TK.none};
    TK f_for[] = {TK.FOR, TK.none};
    TK f_expression[] = {TK.ID, TK.NUM, TK.STR, TK.COMMA, TK.LPAREN, TK.none};


    // tok is global to all these parsing methods;
    // scan just calls the scanner's scan method and saves the result in tok.
    private Token tok; // the current token
    private void scan() {
        tok = scanner.scan();
        //System.out.println(tok);
    }
    
    private Scan scanner;
    Parser(Scan scanner) {
        this.scanner = scanner;
        scan();
        program();
        if( tok.kind != TK.EOF )
            parse_error("junk after logical end of program");
    }

    // for code generation
    private static final int initialValueEVariable = 8888;

    // print something in the generated code
    private void gcprint(String str) {
        System.out.println(str);
    }
    // print identifier in the generated code
    // it prefixes x_ in case id conflicts with C keyword.
    private void gcprintid(String str) {
        System.out.println("x_"+str);
    }

    private void program() {
        gcprint("#include <stdio.h>");
        //array bounds checking function
        gcprint("int boundsChecker(int size) {}");
        gcprint("main() ");
        block();
    }

    private void block() {
        gcprint("{");
        symtab.begin_st_block();
        while( first(f_declaration) ) {
            declaration();
        }
        while( first(f_statement) ) {
            statement();
        }
        symtab.end_st_block();
        gcprint("}");
     }

    private void declaration() {
        if (first(f_var_decl)) {
            var_decl();
        }
        else if (first(f_const_decl)) {
            const_decl();
        }
        else
            parse_error("oops -- declaration bad first");
    }

    private void var_decl() {
        mustbe(TK.VAR);
        var_decl_id();
        while( is(TK.COMMA) ) {
            scan();
            var_decl_id();
        }
    }

    private void var_decl_id() {
        if( is(TK.ID) ) {
			Token ID_Token = tok;
			
			scan();
			if ( is(TK.LBRACKET)) {
				gcprint("int ");
				gcprintid(ID_Token.string);
				
				int array_size;
				gcprint("[");
				array_size = array_decl_bound(ID_Token);
				gcprint("];");
				
				gcprint("int "+ID_Token.string+"_size="+array_size+";");
				
			}
            else if (symtab.add_entry(ID_Token.string, ID_Token.lineNumber, TK.VAR)) {
                gcprint("int ");
				gcprintid(ID_Token.string);
                
                gcprint("="+initialValueEVariable+";");
            }
        }
        else {
            parse_error("expected id in var declaration, got " + tok);
        }
    }

	private int array_decl_bound(Token token) {
		scan();
		boolean negative_flag = false;
		int lower_bound, upper_bound, array_size;
		
		if ( is(TK.RBRACKET)) {
			System.err.println("No bounds given for array\n");
			System.exit(1);
		}
		
		//get lower bound
		if ( is(TK.MINUS)) {
			negative_flag = true;
			scan();
		}
		
		Token lower_bound_token = tok;
		mustbe(TK.NUM);
		lower_bound = Integer.parseInt(lower_bound_token.string);
		if (negative_flag) {
			lower_bound -= 2 * lower_bound;
			negative_flag = false;
		}
		
		//get upper bound
		mustbe(TK.COLON);
		if ( is(TK.MINUS)) {
			negative_flag = true;
			scan();
		}
		
		Token upper_bound_token = tok;
		mustbe(TK.NUM);
		upper_bound = Integer.parseInt(upper_bound_token.string);
		if (negative_flag) {
			upper_bound -= 2 * upper_bound;
			negative_flag = false;
		}
		
		array_size = upper_bound - lower_bound + 1;
		if (array_size <= 0) {
			System.err.println("declared size of " + token.string + " is <= 0 (" + array_size + ") on line " + token.lineNumber);
			System.exit(1);
		}
		
		mustbe(TK.RBRACKET);
		if (symtab.add_entry(token.string, tok.lineNumber, TK.ARR))
			gcprint(" "+array_size);
		
		return array_size;
	}

    private void const_decl() {
        mustbe(TK.CONST);
        boolean newConst = const_decl_id();
        mustbe(TK.EQ);
        if (newConst) {
            gcprint("=");
            gcprint(tok.string);
            gcprint(";");
        }
        mustbe(TK.NUM);
    }

    private boolean const_decl_id() {
        if( is(TK.ID) ) {
            boolean ret;
            if (ret = symtab.add_entry(tok.string, tok.lineNumber, TK.CONST)) {
                gcprint("int ");
                gcprintid(tok.string);
            }
            scan();
            return ret;
        }
        else {
            parse_error("expected id in const declaration, got " + tok);
            return false; // meaningless since parse_error doesn't return
        }
    }

    private void statement(){
        if( first(f_assignment) )
            assignment();
        else if( first(f_print) )
            print();
        else if( first(f_if) )
            ifproc();
        else if( first(f_while) )
            whileproc();
        else if ( first(f_repeat))
			repeatproc();
        else if( first(f_for) )
            forproc();
        else
            parse_error("oops -- statement bad first");
    }

    private void assignment(){
		Token id_token = tok;
        if( is(TK.ID) )
            lvalue_id(tok.string, tok.lineNumber);
        else
            parse_error("missing id on left-hand-side of assignment");
        
        if (isArray("missing subscript for array "+id_token.string+" on line "+id_token.lineNumber, "subscripting non-array "+id_token.string+" on line "+id_token.lineNumber)) {
			;
		}
        else {
			mustbe(TK.ASSIGN);
			gcprint("=");
			expression();
			gcprint(";");
		}
    }

    private void print(){
        mustbe(TK.PRINT);
        /////////////// need to distinguish between string or expression 
        if( is(TK.STR))  {
        	gcprint("printf(\"%s\\n\", ");
        	gcprint(tok.string);
        	scan();
        }
        else {
        	gcprint("printf(\"%d\\n\", ");
        	System.out.println("here");
        	expression();
        }
        gcprint(");");
    }

    private void ifproc(){
        mustbe(TK.IF);
        gcprint("if(");
        expression();
        gcprint(")");
        mustbe(TK.THEN);
        block();
        while( is(TK.ELSIF) ) {
            scan();
            gcprint("else if(");
            expression();
            gcprint(")");
            mustbe(TK.THEN);
            block();
        }
        if( is(TK.ELSE) ) {
            scan();
            gcprint("else");
            block();
        }
        mustbe(TK.END);
    }

    private void whileproc(){
        mustbe(TK.WHILE);
        gcprint("while(");
        expression();
        gcprint(")");
        mustbe(TK.DO);
        block();
        mustbe(TK.END);
    }
    
    private void repeatproc() {
		mustbe(TK.REPEAT);
		gcprint("do");
		block();
		mustbe(TK.UNTIL);
		gcprint("while(!(");
		expression();
		gcprint("));");
	}

    private void forproc(){
        mustbe(TK.FOR);
        gcprint("for(");
        String id = tok.string;
        Token id_token = tok;
        Entry iv = null; // index variable in symtab
        if( is(TK.ID) ) {
            iv = lvalue_id(tok.string, tok.lineNumber);
            
            
            //rvalue_id(tok.string, tok.lineNumber);
            
			/*Entry e = symtab.search(tok.string);
            if ( e.isArr()) {
				scan();
				if (is(TK.LBRACKET)) scan();
				else {
					System.err.println("array on left-hand-side of assignment (used as index variable) "+id_token.string+" on line "+id_token.lineNumber);
					System.exit(1);
				}
			}
            else scan();
            */
            
            iv.setIsIV(true); // mark Entry as IV
            //scan();
        }
        else {
            parse_error("missing id on left-hand-side of assignment in for");
        }
        
       if ( isArray("array on left-hand-side of assignment (used as index variable) "+id_token.string+" on line "+id_token.lineNumber, "subscripting non-array "+id_token.string+" on line "+id_token.lineNumber)) {
			;
		}
        mustbe(TK.ASSIGN);
        gcprint("=");
        expression();
        gcprint(";");
        boolean up = true;
        if( is(TK.TO) ) {
            up = true;
        }
        else if( is(TK.DOWNTO) ) {
            up = false;
        }
        else
            parse_error("for statement is missing to/downto");
        scan();
        gcprintid(id);
        gcprint(up?"<=":">=");
        expression();
        mustbe(TK.DO);
        gcprint(";");
        gcprintid(id);
        gcprint(up?"++)":"--)");
        block();
        mustbe(TK.END);
        iv.setIsIV(false); // mark Entry as no longer IV
    }

    private void expression(){
        simple();
        while( is(TK.EQ) || is(TK.LT) || is(TK.GT) ||
               is(TK.NE) || is(TK.LE) || is(TK.GE)) {
            if( is(TK.EQ) ) gcprint("==");
            else if( is(TK.NE) ) gcprint("!=");
            else gcprint(tok.string);
            scan();
            simple();
        }
    }
    
    private void simple(){
        term();
        while( is(TK.PLUS) || is(TK.MINUS) ) {
            gcprint(tok.string);
            scan();
            term();
        }
    }

    private void term(){
        factor();
        while(  is(TK.TIMES) || is(TK.DIVIDE) ) {
            gcprint(tok.string);
            scan();
            factor();
        }
    }

    private void factor(){
    	//System.out.println("in factor");
        if( is(TK.LPAREN) ) {
            gcprint("(");
            scan();
            expression();
            mustbe(TK.RPAREN);
            gcprint(")");
        }
        else if( is(TK.ID) ) {
			Token id_token = tok;
            rvalue_id(tok.string, tok.lineNumber);
            
            if (isArray("missing subscript for array "+id_token.string+" on line "+id_token.lineNumber, "subscripting non-array "+id_token.string+" on line "+id_token.lineNumber)) {
				//empty
				;
			}
        }
        else if( is(TK.NUM) ) {
            gcprint(tok.string);
            scan();
        }
        else if ( is(TK.COMMA) ) {
        	gcprint(tok.string);
        	scan();
        }
        else
            parse_error("factor");
    }
    
    private boolean isArray(String error_missing_brackets, String error_extra_brackets) {
		Token id_token = tok;
		Entry e = symtab.search(tok.string);
		//is array, but checks if missing bracket
        if ( e.isArr()) {
			scan();
			if (is(TK.LBRACKET)) scan();
			else {
				System.err.println(error_missing_brackets);
				System.exit(1);
			}
			return true;
		}
		//not array, but checks if it has erroneous brackets
        else {
			scan();
			if (is(TK.LBRACKET)) {
				System.err.println(error_extra_brackets);
				System.exit(1);
			}
			return false;
		}
	}

    private Entry lvalue_id(String id, int lno) {
        Entry e = symtab.search(id);
        if( e == null) {
            System.err.println("undeclared variable "+ id + " on line "
                               + lno);
            System.exit(1);
        }
        if( !e.isVar() && !e.isArr()) {
            System.err.println("constant on left-hand-side of assignment "+ id + " on line "
                               + lno);
            System.exit(1);
        }
        if( e.getIsIV()) {
            System.err.println("index variable on left-hand-side of assignment "+ id + " on line "
                               + lno);
            System.exit(1);
        }
        gcprintid(id);
        return e;
    }

    private void rvalue_id(String id, int lno) {
        Entry e = symtab.search(id);
        if( e == null) {
            System.err.println("undeclared variable "+ id + " on line "
                               + lno);
            System.exit(1);
        }
        gcprintid(id);
    }


    // is current token what we want?
    private boolean is(TK tk) {
        return tk == tok.kind;
    }

    // ensure current token is tk and skip over it.
    private void mustbe(TK tk) {
        if( ! is(tk) ) {
            System.err.println( "mustbe: want " + tk + ", got " +
                                    tok);
            parse_error( "missing token (mustbe)" );
        }
        scan();
    }
    
    boolean first(TK [] set) {
        int k = 0;
        while(set[k] != TK.none && set[k] != tok.kind) {
            k++;
        }
        return set[k] != TK.none;
    }

    private void parse_error(String msg) {
        System.err.println( "can't parse: line "
                            + tok.lineNumber + " " + msg );
        System.exit(1);
    }
}

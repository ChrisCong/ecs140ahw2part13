//package program.e2c;

import java.util.*;

class Entry {
    private String name;        // name of symbol.
    private int lineNumber, lowerBound, upperBound;     // line on which declared
    private TK varOrConst;      // variable or const?
    private boolean isIV ;      // is presently index variable?

    public Entry(String name, int lineNumber, TK varOrConst) {
        this.name = name;
        this.lineNumber = lineNumber;
        this.varOrConst = varOrConst;
        this.isIV = false;
    }
    public Entry(String name, int lineNumber, TK varOrConst, int lowerBound, int upperBound) {
		this.name = name;
		this.lineNumber = lineNumber;
		this.varOrConst = varOrConst;
		this.isIV = false;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}
    String getName() {
        return name;
    }
    int getLineNumber() {
        return lineNumber;
    }
    int getLowerBound() {
		return lowerBound;
	}
	int getUpperBound() {
		return upperBound;
	}
    boolean isVar() {
        return (varOrConst == TK.VAR);
    }
    boolean isArr() {
		return (varOrConst == TK.ARR);
	}
    boolean isConst() {
        return (varOrConst == TK.CONST);
    }
    boolean getIsIV() {
        return isIV;
    }
    void setIsIV(boolean is) {
        isIV = is;
    }
    public String whatAreYou() {
        return  isVar()?"variable"
               :isConst()?"constant"
               :isArr()?"array"
               :"OOPS Entry whatAreYou()";
    }
    public String toString() {
        return name + "  declared on line " + lineNumber + " as "+ varOrConst;
    }
}

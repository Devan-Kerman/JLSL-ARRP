package org.jglrxavpok.jlsl.java;

import java.io.*;
import java.util.*;

import org.jglrxavpok.jlsl.*;
import org.jglrxavpok.jlsl.fragments.*;
import org.jglrxavpok.jlsl.fragments.MethodCallFragment.InvokeTypes;

public class JavaEncoder extends CodeEncoder {
	public static boolean DEBUG = true;

	private int indentation;
	private NewClassFragment currentClass;
	private int currentLine;
	private Stack<String> stack;
	private final Stack<String> typesStack;
	private final HashMap<String, String> name2type;
	private final HashMap<Object, String> constants;
	private final ArrayList<String> initialized;
	private StartOfMethodFragment currentMethod;
	private boolean allowedToPrint;
	private PrintWriter output;
	private final Stack<CodeFragment> waiting;
	private final Stack<String> newInstances;
	public boolean interpreting = false;
	private final HashMap<String, String> imports;

	private String classPackage;

	private String className;

	public JavaEncoder(int glslversion) {
		this.imports = new HashMap<>();
		this.stack = new Stack<String>();
		this.typesStack = new Stack<String>();
		this.initialized = new ArrayList<String>();
		this.name2type = new HashMap<String, String>();
		this.constants = new HashMap<Object, String>();
		this.waiting = new Stack<CodeFragment>();
		this.newInstances = new Stack<String>();

		this.init();
	}

	public void init() {

	}

	private String toJava(String type) {
		if (type == null) {
			return "";
		}
		String copy = type;
		String end = "";
		while (copy.contains("[]")) {
			copy = copy.replaceFirst("\\[\\]", "");
			end += "[]";
		}
		type = copy;
		if (type.startsWith("java.lang.")) {
			type = type.replaceFirst("java.lang.", "");
		}
		if (type.contains(".") && !type.startsWith("this.") && !this.name2type.containsKey(type)) {
			String withoutPackage = type.substring(type.lastIndexOf(".") + 1);
			if (this.imports.containsKey(withoutPackage)) {
				String fullName = this.imports.get(withoutPackage);
				if (fullName.equals(type)) {
					return withoutPackage + end;
				}
			} else {
				this.imports.put(withoutPackage, type);
				return withoutPackage + end;
			}
		}
		return type + end;
	}

	private String getEndOfLine(int currentLine) {
		String s = "";
		// if(currentLine % 2 == 0)
		{
			s = " //Line #" + currentLine;
		}
		return s;
	}

	@Override
	public void onRequestResult(ArrayList<CodeFragment> fragments) {
	}

	@Override
	public void createSourceCode(List<CodeFragment> in, PrintWriter out) {
		this.interpreting = true;
		this.interpret(in);
		this.interpreting = false;
		this.output = out;
		this.allowedToPrint = true;
		this.println("package " + this.classPackage + ";\n");
		Iterator<String> it = this.imports.values().iterator();
		while (it.hasNext()) {
			String importName = it.next();
			this.println("import " + importName + ";");
		}
		for (int index = 0; index < in.size(); index++) {
			CodeFragment fragment = in.get(index);
			this.output = out;
			this.allowedToPrint = !fragment.forbiddenToPrint;
			if (!this.waiting.isEmpty()) {
				this.handleCodeFragment(this.waiting.pop(), index, in, out);
			}
			this.handleCodeFragment(fragment, index, in, out);
		}
		this.println("}");
		out.flush();
	}

	private void handleCodeFragment(CodeFragment fragment, int index, List<CodeFragment> in, PrintWriter out) {
		if (fragment.getClass() == NewClassFragment.class) {
			this.handleClassFragment((NewClassFragment) fragment, in, index, out);
			this.currentClass = (NewClassFragment) fragment;
		} else if (fragment.getClass() == FieldFragment.class) {
			this.handleFieldFragment((FieldFragment) fragment, in, index, out);
		} else if (fragment.getClass() == StartOfMethodFragment.class) {
			this.handleStartOfMethodFragment((StartOfMethodFragment) fragment, in, index, out);
			this.currentMethod = (StartOfMethodFragment) fragment;
		} else if (fragment.getClass() == EndOfMethodFragment.class) {
			this.handleEndOfMethodFragment((EndOfMethodFragment) fragment, in, index, out);
		} else if (fragment.getClass() == LineNumberFragment.class) {
			this.currentLine = ((LineNumberFragment) fragment).line;
		} else if (fragment.getClass() == NewArrayFragment.class) {
			this.handleNewArrayFragment((NewArrayFragment) fragment, in, index, out);
		} else if (fragment.getClass() == NewMultiArrayFragment.class) {
			this.handleNewMultiArrayFragment((NewMultiArrayFragment) fragment, in, index, out);
		} else if (fragment.getClass() == PutFieldFragment.class) {
			this.handlePutFieldFragment((PutFieldFragment) fragment, in, index, out);
		} else if (fragment.getClass() == GetFieldFragment.class) {
			this.handleGetFieldFragment((GetFieldFragment) fragment, in, index, out);
		} else if (fragment.getClass() == IntPushFragment.class) {
			this.handleBiPushFragment((IntPushFragment) fragment, in, index, out);
		} else if (fragment.getClass() == NewPrimitiveArrayFragment.class) {
			this.handleNewPrimitiveArrayFragment((NewPrimitiveArrayFragment) fragment, in, index, out);
		} else if (fragment.getClass() == LoadVariableFragment.class) {
			this.handleLoadVariableFragment((LoadVariableFragment) fragment, in, index, out);
		} else if (fragment.getClass() == StoreVariableFragment.class) {
			this.handleStoreVariableFragment((StoreVariableFragment) fragment, in, index, out);
		} else if (fragment.getClass() == LdcFragment.class) {
			this.handleLdcFragment((LdcFragment) fragment, in, index, out);
		} else if (fragment.getClass() == LoadConstantFragment.class) {
			this.handleLoadConstantFragment((LoadConstantFragment) fragment, in, index, out);
		} else if (fragment.getClass() == ReturnValueFragment.class) {
			this.handleReturnValueFragment((ReturnValueFragment) fragment, in, index, out);
		} else if (fragment.getClass() == AddFragment.class) {
			this.handleAddFragment((AddFragment) fragment, in, index, out);
		} else if (fragment.getClass() == SubFragment.class) {
			this.handleSubFragment((SubFragment) fragment, in, index, out);
		} else if (fragment.getClass() == MulFragment.class) {
			this.handleMulFragment((MulFragment) fragment, in, index, out);
		} else if (fragment.getClass() == DivFragment.class) {
			this.handleDivFragment((DivFragment) fragment, in, index, out);
		} else if (fragment.getClass() == ArrayOfArrayLoadFragment.class) {
			this.handleArrayOfArrayLoadFragment((ArrayOfArrayLoadFragment) fragment, in, index, out);
		} else if (fragment.getClass() == ArrayStoreFragment.class) {
			this.handleArrayStoreFragment((ArrayStoreFragment) fragment, in, index, out);
		} else if (fragment.getClass() == IfStatementFragment.class) {
			this.handleIfStatementFragment((IfStatementFragment) fragment, in, index, out);
		} else if (fragment.getClass() == EndOfBlockFragment.class) {
			this.handleEndOfBlockFragment((EndOfBlockFragment) fragment, in, index, out);
		} else if (fragment.getClass() == ElseStatementFragment.class) {
			this.handleElseStatementFragment((ElseStatementFragment) fragment, in, index, out);
		} else if (fragment.getClass() == MethodCallFragment.class) {
			this.handleMethodCallFragment((MethodCallFragment) fragment, in, index, out);
		} else if (fragment.getClass() == ModFragment.class) {
			this.handleModFragment((ModFragment) fragment, in, index, out);
		} else if (fragment.getClass() == CastFragment.class) {
			this.handleCastFragment((CastFragment) fragment, in, index, out);
		} else if (fragment.getClass() == LeftShiftFragment.class) {
			this.handleLeftShiftFragment((LeftShiftFragment) fragment, in, index, out);
		} else if (fragment.getClass() == RightShiftFragment.class) {
			this.handleRightShiftFragment((RightShiftFragment) fragment, in, index, out);
		} else if (fragment.getClass() == AndFragment.class) {
			this.handleAndFragment((AndFragment) fragment, in, index, out);
		} else if (fragment.getClass() == OrFragment.class) {
			this.handleOrFragment((OrFragment) fragment, in, index, out);
		} else if (fragment.getClass() == XorFragment.class) {
			this.handleXorFragment((XorFragment) fragment, in, index, out);
		} else if (fragment.getClass() == IfNotStatementFragment.class) {
			this.handleIfNotStatementFragment((IfNotStatementFragment) fragment, in, index, out);
		} else if (fragment.getClass() == PopFragment.class) {
			this.handlePopFragment((PopFragment) fragment, in, index, out);
		} else if (fragment.getClass() == ReturnFragment.class) {
			this.handleReturnFragment((ReturnFragment) fragment, in, index, out);
		} else if (fragment.getClass() == DuplicateFragment.class) {
			this.handleDuplicateFragment((DuplicateFragment) fragment, in, index, out);
		} else if (fragment.getClass() == NewInstanceFragment.class) {
			this.handleNewInstanceFragment((NewInstanceFragment) fragment, in, index, out);
		} else if (fragment.getClass() == EqualCheckFragment.class) {
			this.handleEqualCheckFragment((EqualCheckFragment) fragment, in, index, out);
		} else if (fragment.getClass() == NotEqualCheckFragment.class) {
			this.handleNotEqualCheckFragment((NotEqualCheckFragment) fragment, in, index, out);
		} else if (fragment.getClass() == CompareFragment.class) {
			this.handleCompareFragment((CompareFragment) fragment, in, index, out);
		}
	}

	private void handleCompareFragment(CompareFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String right = this.stack.pop();
		String left = this.stack.pop();
		this.stack.push(left + " " + (fragment.inferior ? "<" : ">") + " " + right);
	}

	private void handleNotEqualCheckFragment(NotEqualCheckFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String right = this.stack.pop();
		String left = this.stack.pop();
		this.stack.push("(" + left + " " + "!=" + " " + right + ")");
	}

	private void handleEqualCheckFragment(EqualCheckFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String right = this.stack.pop();
		String left = this.stack.pop();
		this.stack.push("(" + left + " " + "==" + " " + right + ")");
	}

	private void handleNewInstanceFragment(NewInstanceFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		this.newInstances.push(fragment.type);
	}

	private void handleDuplicateFragment(DuplicateFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		if (!this.newInstances.isEmpty()) {
			return;
		}
		if (fragment.wait > 0) {
			this.waiting.add(fragment);
			fragment.wait--;
		} else {
			String a = this.stack.pop();
			this.stack.push(a);
			this.stack.push(a);
		}
	}

	private void interpret(List<CodeFragment> in) {
		Stack<String> copy = this.stack;
		Stack<String> tmpstack = new Stack<String>();
		this.stack = tmpstack;
		StartOfMethodFragment currentMethod = null;
		PrintWriter nullPrinter = new PrintWriter(new StringWriter());
		for (int i = 0; i < in.size(); i++) {
			boolean dontHandle = false;
			CodeFragment fragment = in.get(i);
			if (fragment.getClass() == StartOfMethodFragment.class) {
				currentMethod = (StartOfMethodFragment) fragment;
			} else if (fragment.getClass() == FieldFragment.class) {
				FieldFragment fieldFrag = (FieldFragment) fragment;
			} else if (fragment.getClass() == StoreVariableFragment.class) {
				StoreVariableFragment storeFrag = (StoreVariableFragment) fragment;
				String type = storeFrag.variableType;
			} else if (fragment.getClass() == PutFieldFragment.class) {
				PutFieldFragment storeFrag = (PutFieldFragment) fragment;
				if (currentMethod != null && currentMethod.name.equals("<init>")) {
					for (int ii = 0; ii < in.size(); ii++) {
						CodeFragment fragment1 = in.get(ii);
						if (fragment1.getClass() == FieldFragment.class) {
							FieldFragment fieldFrag = (FieldFragment) fragment1;
							if (fieldFrag.name.equals(storeFrag.fieldName) && fieldFrag.type.equals(storeFrag.fieldType) && !(fieldFrag.access.isFinal() && fieldFrag.initialValue != null)) {
								fieldFrag.initialValue = this.stack.peek();
								dontHandle = true;
								storeFrag.forbiddenToPrint = true;
								break;
							}
						}
					}
				}
			}
			if (!dontHandle) {
				this.output = nullPrinter;
				this.allowedToPrint = !fragment.forbiddenToPrint;
				if (!this.waiting.isEmpty()) {
					this.handleCodeFragment(this.waiting.pop(), i, in, nullPrinter);
				}
				this.handleCodeFragment(fragment, i, in, nullPrinter);
			}
		}

		this.waiting.clear();
		this.currentLine = 0;
		this.indentation = 0;
		this.initialized.clear();
		this.name2type.clear();
		this.currentClass = null;
		this.typesStack.clear();
		this.constants.clear();
		this.stack = copy;
	}

	private void println() {
		this.println("");
	}

	private void println(String s) {
		if (this.allowedToPrint) {
			this.output.println(s);
		}
	}

	private void handleReturnFragment(ReturnFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		if (in.size() <= index + 1 || in.get(index + 1).getClass() == EndOfMethodFragment.class) {
		} else {
			this.println(this.getIndent() + "return;" + this.getEndOfLine(this.currentLine));
		}
	}

	private void handlePopFragment(PopFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		this.println(this.getIndent() + this.stack.pop() + ";" + this.getEndOfLine(this.currentLine));
	}

	private int countChar(String str, char c) {
		int nbr = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == c) {
				nbr++;
			}
		}
		return nbr;
	}

	private void handleIfNotStatementFragment(IfNotStatementFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String condition = this.stack.pop();
		this.println(this.getIndent() + "if(!" + condition + ")" + this.getEndOfLine(this.currentLine));
		this.println(this.getIndent() + "{");
		this.indentation++;
	}

	private void handleXorFragment(XorFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String b = this.stack.pop();
		String a = this.stack.pop();
		this.stack.push("(" + a + " || " + b + ")");
	}

	private void handleOrFragment(OrFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String b = this.stack.pop();
		String a = this.stack.pop();
		this.stack.push("(" + a + " " + (fragment.isDouble ? "||" : "|") + " " + b + ")");
	}

	private void handleAndFragment(AndFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String b = this.stack.pop();
		String a = this.stack.pop();
		this.stack.push("(" + a + " " + (fragment.isDouble ? "&&" : "&") + " " + b + ")");
	}

	private void handleRightShiftFragment(RightShiftFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String b = this.stack.pop();
		String a = this.stack.pop();
		this.stack.push(a + ">>" + (!fragment.signed ? ">" : "") + b);
	}

	private void handleLeftShiftFragment(LeftShiftFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String b = this.stack.pop();
		String a = this.stack.pop();
		this.stack.push(a + "<<" + (!fragment.signed ? "<" : "") + b);
	}

	private void handleCastFragment(CastFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String toCast = this.stack.pop();

		String previousType;
		if (toCast.startsWith("(")) {
			previousType = toCast.substring(1, toCast.indexOf(")") - 1);
		} else {
			previousType = this.toJava(this.currentMethod.varName2TypeMap.get(toCast));
		}
		if (previousType.equals(this.toJava(fragment.to))) {
			if (DEBUG) {
				System.out.println("GLSLEncoder > Cancelling cast for " + toCast);
			}
		} else {
			this.stack.push("(" + this.toJava(fragment.to) + ")" + toCast);
		}
	}

	private void handleModFragment(ModFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String a = this.stack.pop();
		String b = this.stack.pop();
		this.stack.push("(" + b + " % " + a + ")");
	}

	private void handleMethodCallFragment(MethodCallFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String s = "";
		String n = fragment.methodName;
		boolean isConstructor = false;
		if (n.equals("<init>")) {
			n = "new " + this.toJava(fragment.methodOwner);
			isConstructor = true;
			if (!this.newInstances.isEmpty()) {
				this.newInstances.pop();
			}
		}
		String key = fragment.methodName;
		if (this.toJava(fragment.methodOwner) != null && !this.toJava(fragment.methodOwner).equals("null") && !this.toJava(fragment.methodOwner).trim().equals("")) {
			key = this.toJava(fragment.methodOwner) + "." + key;
		}
		if (fragment.invokeType == InvokeTypes.SPECIAL && this.currentMethod.name.equals("<init>") && fragment.methodOwner.equals(this.currentClass.superclass)) {
			this.allowedToPrint = false;
		}

		s += n + "(";
		ArrayList<String> args = new ArrayList<String>();
		for (@SuppressWarnings ("unused") String type : fragment.argumentsTypes) {
			String arg = this.stack.pop();
			if (arg.startsWith("(") && arg.endsWith(")") && this.countChar(arg, '(') == this.countChar(arg, ')')) {
				arg = arg.substring(1, arg.length() - 1);
			}
			args.add(arg);
		}
		String argsStr = "";
		for (int i = 0; i < args.size(); i++) {
			if (i != 0) {
				argsStr += ", ";
			}
			argsStr += args.get(args.size() - 1 - i);
		}
		s += argsStr;
		s += ")";
		boolean ownerBefore = false;
		boolean parenthesis = true;
		int ownerPosition = 0;
		boolean actsAsField = false;
		if (fragment.invokeType == InvokeTypes.VIRTUAL) {
			String owner = this.stack.pop();
			if (owner.equals(this.currentClass.className) || owner.equals("this")) {
				owner = null;
			} else {
				if (owner.startsWith("(") && owner.endsWith(")") && this.countChar(owner, '(') == this.countChar(owner, ')')) {
					owner = owner.substring(1, owner.length() - 1);
				}
			}
			if (!ownerBefore) {
				if (actsAsField) {
					if (n.length() >= 1) {
						s = (owner != null ? owner : "") + "." + n;
					} else {
						s = (owner != null ? owner : "");
					}
					if (argsStr.length() > 0) {
						s += " = " + argsStr;
					}
				} else {
					s = (owner != null ? owner + "." : "") + n + (parenthesis ? "(" : "") + argsStr + (parenthesis ? ")" : "");
				}
			} else {
				s = (owner != null ? owner : "") + n + (parenthesis ? "(" : "") + argsStr + (parenthesis ? ")" : "");
			}
			if (fragment.returnType.equals("void")) {
				this.println(this.getIndent() + s + ";" + this.getEndOfLine(this.currentLine));
			} else {
				this.stack.push("(" + s + ")");
			}
		} else if (fragment.invokeType == InvokeTypes.STATIC) {
			String ownership = "";
			String owner = this.toJava(fragment.methodOwner);
			if (owner != null && !owner.trim().equals("") && !owner.equals("null")) {
				ownership = owner + (n.length() > 0 ? "." : "");
			}
			this.stack.push(ownership + n + (parenthesis ? "(" : "") + argsStr + (parenthesis ? ")" : ""));
		} else {
			this.stack.push(n + (parenthesis ? "(" : "") + argsStr + (parenthesis ? ")" : ""));

			if (fragment.returnType.equals("void") && !fragment.methodName.equals("<init>")) {
				this.println(this.getIndent() + this.stack.pop() + ";");
			}
		}

	}

	private void handleElseStatementFragment(ElseStatementFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		this.println(this.getIndent() + "else" + this.getEndOfLine(this.currentLine));
		this.println(this.getIndent() + "{");
		this.indentation++;
	}

	private void handleEndOfBlockFragment(EndOfBlockFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		this.indentation--;
		this.println(this.getIndent() + "}");
	}

	private void handleIfStatementFragment(IfStatementFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String condition = this.stack.pop();
		this.println(this.getIndent() + "if(" + condition + ")" + this.getEndOfLine(this.currentLine));
		this.println(this.getIndent() + "{");
		this.indentation++;
	}

	private void handleArrayStoreFragment(ArrayStoreFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String result = "";
		String toAdd = "";
		for (int i = 0; i < 2; i++) {
			String lastType = this.typesStack.pop();
			String copy = lastType;
			int dimensions = 0;
			if (copy != null) {
				while (copy.indexOf("[]") >= 0) {
					copy = copy.substring(copy.indexOf("[]") + 2);
					dimensions++;
				}
			}
			String val = this.stack.pop();
			String arrayIndex = "";
			for (int dim = 0; dim < dimensions; dim++) {
				arrayIndex = "[" + this.stack.pop() + "]" + arrayIndex;
			}
			String name = this.stack.pop();
			if (i == 1) {
				result = val + toAdd + " = " + result;
			} else if (i == 0) {
				result = val + result;
				toAdd = "[" + name + "]";
			}
		}
		this.println(this.getIndent() + result + ";" + this.getEndOfLine(this.currentLine));
	}

	private void handleArrayOfArrayLoadFragment(ArrayOfArrayLoadFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String value = this.stack.pop();
		String name = this.stack.pop();
		this.stack.push(name + "[" + value + "]");
		if (this.name2type.containsKey(name + "[" + value + "]")) {
			this.name2type.put(name + "[" + value + "]", name.substring(0, name.indexOf("[")));
		}
		this.typesStack.push(this.name2type.get(name + "[" + value + "]"));
	}

	private void handleDivFragment(DivFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String a = this.stack.pop();
		String b = this.stack.pop();
		this.stack.push("(" + b + "/" + a + ")");
	}

	private void handleMulFragment(MulFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String a = this.stack.pop();
		String b = this.stack.pop();
		this.stack.push("(" + b + "*" + a + ")");
	}

	private void handleSubFragment(SubFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String a = this.stack.pop();
		String b = this.stack.pop();
		this.stack.push("(" + b + "-" + a + ")");
	}

	private void handleAddFragment(AddFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String a = this.stack.pop();
		String b = this.stack.pop();
		this.stack.push("(" + b + "+" + a + ")");
	}

	private void handleReturnValueFragment(ReturnValueFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		this.println(this.getIndent() + "return" + " " + this.stack.pop() + ";" + this.getEndOfLine(this.currentLine));
	}

	private void handleLoadConstantFragment(LoadConstantFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		this.stack.push(fragment.value + "");
	}

	private void handleLdcFragment(LdcFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		if (this.constants.containsKey(fragment.value)) {
			this.stack.push("" + this.constants.get(fragment.value));
		} else if (fragment.value instanceof String) {
			this.stack.push("\"" + fragment.value + "\"");
		} else if (fragment.value instanceof Number) {
			this.stack.push("" + fragment.value);
		} else if (DEBUG) {
			System.out.println("GLSLEncoder > Invalid value: " + fragment.value + " of type " + fragment.value.getClass().getCanonicalName());
		}
	}

	private void handleStoreVariableFragment(StoreVariableFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String value = this.stack.pop();
		if (value.startsWith("(") && value.endsWith(")") && this.countChar(value, '(') == this.countChar(value, ')')) {
			value = value.substring(1, value.length() - 1);
		}
		if (value.equals(fragment.variableName + "+1")) {
			this.println(this.getIndent() + fragment.variableName + "++;" + this.getEndOfLine(this.currentLine));
			return;
		} else if (value.equals(fragment.variableName + "-1")) {
			this.println(this.getIndent() + fragment.variableName + "--;" + this.getEndOfLine(this.currentLine));
			return;
		}
		String javaType = this.toJava(this.currentMethod.varName2TypeMap.get(fragment.variableName));
		if (javaType.equals("boolean")) {
			if (value.equals("0")) {
				value = "false";
			} else if (value.equals("1")) {
				value = "true";
			}
		} else if (javaType.equals("char")) {
			try {
				value = "'" + Character.valueOf((char) Integer.parseInt(value)) + "'";
			} catch (Exception e) {
			}
		}
		if (this.initialized.contains(fragment.variableName)) {
			this.println(this.getIndent() + fragment.variableName + " = " + value + ";" + this.getEndOfLine(this.currentLine));
		} else {
			this.initialized.add(fragment.variableName);
			this.println(this.getIndent() + this.toJava(this.currentMethod.varName2TypeMap.get(fragment.variableName)) + " " + fragment.variableName + " = " + value +
			             ";" + this.getEndOfLine(this.currentLine));
		}
	}

	private void handleLoadVariableFragment(LoadVariableFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		this.stack.push(fragment.variableName);
	}

	private void handleNewPrimitiveArrayFragment(NewPrimitiveArrayFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String dimension = "[" + this.stack.pop() + "]";
		this.stack.push(fragment.type + dimension);
	}

	private void handleBiPushFragment(IntPushFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		this.stack.push(fragment.value + "");
	}

	private void handleGetFieldFragment(GetFieldFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String owner = this.toJava(this.stack.pop());
		String ownership = owner + ".";
		this.stack.push(ownership + fragment.fieldName);
		this.typesStack.push(fragment.fieldType);
	}

	private void handlePutFieldFragment(PutFieldFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String value = this.stack.pop();
		if (value.startsWith("(") && value.endsWith(")") && this.countChar(value, '(') == this.countChar(value, ')')) {
			value = value.substring(1, value.length() - 1);
		}
		if (value.equals(fragment.fieldName + "+1")) {
			this.println(this.getIndent() + fragment.fieldName + "++;" + this.getEndOfLine(this.currentLine));
			return;
		} else if (value.equals(fragment.fieldName + "-1")) {
			this.println(this.getIndent() + fragment.fieldName + "--;" + this.getEndOfLine(this.currentLine));
			return;
		}
		String javaType = this.toJava(this.currentMethod.varName2TypeMap.get(fragment.fieldName));
		if (javaType.equals("boolean")) {
			if (value.equals("0")) {
				value = "false";
			} else if (value.equals("1")) {
				value = "true";
			}
		}
		String owner = this.stack.pop();
		String ownership = owner + ".";
		for (int i = 0; i < index; i++) {
			CodeFragment frag = in.get(i);
			if (frag.getClass() == FieldFragment.class) {
				FieldFragment fieldFrag = (FieldFragment) frag;
				if (fieldFrag.access.isFinal() && fieldFrag.name.equals(fragment.fieldName)) {
					return;
				}
			}
		}
		this.println(this.getIndent() + ownership + fragment.fieldName + " " + "=" + " " + value + ";" + this.getEndOfLine(this.currentLine));
	}

	private void handleNewMultiArrayFragment(NewMultiArrayFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String s = "";
		ArrayList<String> list = new ArrayList<String>();
		for (int dim = 0; dim < fragment.dimensions; dim++) {
			list.add(this.stack.pop());
		}
		for (int dim = 0; dim < fragment.dimensions; dim++) {
			s += "[" + list.get(list.size() - dim - 1) + "]";
		}
		this.stack.push(this.toJava(fragment.type) + s);
	}

	private void handleNewArrayFragment(NewArrayFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String s = "[" + this.stack.pop() + "]";
		this.stack.push(this.toJava(fragment.type) + s);
	}

	private void handleEndOfMethodFragment(EndOfMethodFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		this.indentation--;
		this.println(this.getIndent() + "}");
	}

	private void handleStartOfMethodFragment(StartOfMethodFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		if (fragment.name.equals("<init>")) {
			String n = this.className;
			this.initialized.clear();
			this.println();
			String args = "";
			for (int i = 0; i < fragment.argumentsNames.size(); i++) {
				String s = this.toJava(fragment.argumentsTypes.get(i)) + " " + fragment.argumentsNames.get(i);
				if (i != 0) {
					args += ", ";
				}
				args += s;
			}
			String accessStr = "";
			if (fragment.access.isPublic()) {
				accessStr = "public";
			} else if (fragment.access.isProtected()) {
				accessStr = "protected";
			} else if (fragment.access.isPrivate()) {
				accessStr = "private";
			}
			this.println(this.getIndent() + accessStr + " " + n + "(" + args + ")\n" + this.getIndent() + "{");
		} else {
			this.initialized.clear();
			this.println();
			String args = "";
			for (int i = 0; i < fragment.argumentsNames.size(); i++) {
				String s = this.toJava(fragment.argumentsTypes.get(i)) + " " + fragment.argumentsNames.get(i);
				if (i != 0) {
					args += ", ";
				}
				args += s;
			}
			this.println(this.getIndent() + this.toJava(fragment.returnType) + " " + fragment.name + "(" + args + ")\n" + this.getIndent() + "{");
		}
		this.indentation++;
	}

	private void handleFieldFragment(FieldFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String storageType = null;
		for (CodeFragment child : fragment.getChildren()) {
			if (child instanceof AnnotationFragment) {
				AnnotationFragment annot = (AnnotationFragment) child;
				this.println(this.getIndent() + "@" + this.toJava(annot.name));
			}
		}
		String str = "";
		if (fragment.access.isPublic()) {
			str += "public ";
		} else if (fragment.access.isPrivate()) {
			str += "private ";
		} else if (fragment.access.isProtected()) {
			str += "protected ";
		}
		if (fragment.access.isStatic()) {
			str += "static ";
		}
		if (fragment.access.isFinal()) {
			str += "final ";
		}
		str += this.toJava(fragment.type) + " ";
		str += fragment.name;
		if (fragment.initialValue != null) {
			str += " = " + fragment.initialValue;
		}
		this.println(this.getIndent() + str + ";");
	}

	@SuppressWarnings ("unchecked")
	private void handleClassFragment(NewClassFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		this.println("// Original class name: " + fragment.className + " compiled from " + fragment.sourceFile + " and of version " + fragment.classVersion);
		this.classPackage = fragment.className.substring(0, fragment.className.lastIndexOf("."));
		this.className = fragment.className.substring(fragment.className.lastIndexOf(".") + 1);
		for (CodeFragment child : fragment.getChildren()) {
			if (child instanceof AnnotationFragment) {
				AnnotationFragment annotFragment = (AnnotationFragment) child;
				this.println("@" + this.toJava(annotFragment.name));
			}
		}
		String hierarchy = "";
		if (fragment.superclass != null && !fragment.superclass.equals(Object.class.getCanonicalName())) {
			hierarchy += " extends " + this.toJava(fragment.superclass);
		}
		String access = "";
		if (fragment.access.isPublic()) {
			access += "public ";
		} else if (fragment.access.isProtected()) {
			access += "protected ";
		} else if (fragment.access.isPrivate()) {
			access += "private ";
		}
		this.println(access + "class " + this.className + hierarchy);
		this.println("{");
		this.indentation++;
	}

	private String getIndent() {
		String s = "";
		for (int i = 0; i < this.indentation; i++) {
			s += "    ";
		}
		return s;
	}

}

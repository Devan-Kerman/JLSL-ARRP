package org.jglrxavpok.jlsl.glsl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.jglrxavpok.jlsl.CodeEncoder;
import org.jglrxavpok.jlsl.JLSLException;
import org.jglrxavpok.jlsl.fragments.AddFragment;
import org.jglrxavpok.jlsl.fragments.AndFragment;
import org.jglrxavpok.jlsl.fragments.AnnotationFragment;
import org.jglrxavpok.jlsl.fragments.ArrayOfArrayLoadFragment;
import org.jglrxavpok.jlsl.fragments.ArrayStoreFragment;
import org.jglrxavpok.jlsl.fragments.CastFragment;
import org.jglrxavpok.jlsl.fragments.CodeFragment;
import org.jglrxavpok.jlsl.fragments.CompareFragment;
import org.jglrxavpok.jlsl.fragments.DivFragment;
import org.jglrxavpok.jlsl.fragments.DuplicateFragment;
import org.jglrxavpok.jlsl.fragments.ElseStatementFragment;
import org.jglrxavpok.jlsl.fragments.EndOfBlockFragment;
import org.jglrxavpok.jlsl.fragments.EndOfMethodFragment;
import org.jglrxavpok.jlsl.fragments.EqualCheckFragment;
import org.jglrxavpok.jlsl.fragments.FieldFragment;
import org.jglrxavpok.jlsl.fragments.GetFieldFragment;
import org.jglrxavpok.jlsl.fragments.IfNotStatementFragment;
import org.jglrxavpok.jlsl.fragments.IfStatementFragment;
import org.jglrxavpok.jlsl.fragments.IntPushFragment;
import org.jglrxavpok.jlsl.fragments.LdcFragment;
import org.jglrxavpok.jlsl.fragments.LeftShiftFragment;
import org.jglrxavpok.jlsl.fragments.LineNumberFragment;
import org.jglrxavpok.jlsl.fragments.LoadConstantFragment;
import org.jglrxavpok.jlsl.fragments.LoadVariableFragment;
import org.jglrxavpok.jlsl.fragments.MethodCallFragment;
import org.jglrxavpok.jlsl.fragments.MethodCallFragment.InvokeTypes;
import org.jglrxavpok.jlsl.fragments.ModFragment;
import org.jglrxavpok.jlsl.fragments.MulFragment;
import org.jglrxavpok.jlsl.fragments.NewArrayFragment;
import org.jglrxavpok.jlsl.fragments.NewClassFragment;
import org.jglrxavpok.jlsl.fragments.NewInstanceFragment;
import org.jglrxavpok.jlsl.fragments.NewMultiArrayFragment;
import org.jglrxavpok.jlsl.fragments.NewPrimitiveArrayFragment;
import org.jglrxavpok.jlsl.fragments.NotEqualCheckFragment;
import org.jglrxavpok.jlsl.fragments.OrFragment;
import org.jglrxavpok.jlsl.fragments.PopFragment;
import org.jglrxavpok.jlsl.fragments.PutFieldFragment;
import org.jglrxavpok.jlsl.fragments.ReturnFragment;
import org.jglrxavpok.jlsl.fragments.ReturnValueFragment;
import org.jglrxavpok.jlsl.fragments.RightShiftFragment;
import org.jglrxavpok.jlsl.fragments.StartOfMethodFragment;
import org.jglrxavpok.jlsl.fragments.StoreVariableFragment;
import org.jglrxavpok.jlsl.fragments.SubFragment;
import org.jglrxavpok.jlsl.fragments.XorFragment;
import org.jglrxavpok.jlsl.glsl.GLSL.Attribute;
import org.jglrxavpok.jlsl.glsl.GLSL.Extensions;
import org.jglrxavpok.jlsl.glsl.GLSL.In;
import org.jglrxavpok.jlsl.glsl.GLSL.Layout;
import org.jglrxavpok.jlsl.glsl.GLSL.Out;
import org.jglrxavpok.jlsl.glsl.GLSL.Substitute;
import org.jglrxavpok.jlsl.glsl.GLSL.Uniform;
import org.jglrxavpok.jlsl.glsl.GLSL.Varying;
import org.jglrxavpok.jlsl.glsl.fragments.StructFragment;

public class GLSLEncoder extends CodeEncoder {
	private static final int STRUCT = 1;
	public static boolean DEBUG = true;
	private int indentation;
	private final int glslversion;
	private NewClassFragment currentClass;
	private final ArrayList<String> extensions = new ArrayList<String>();
	private final String space = " ";
	private final String tab = "    ";
	private int currentLine;
	private Stack<String> stack;
	private final Stack<String> typesStack;
	private final HashMap<String, String> name2type;
	private final HashMap<Object, String> constants;
	private final HashMap<String, String> methodReplacements;
	private final ArrayList<String> initialized;
	private StartOfMethodFragment currentMethod;
	private boolean convertNumbersToChars;
	private final ArrayList<String> loadedStructs = new ArrayList<String>();
	private int currentRequestType;
	private Object requestData;
	private boolean allowedToPrint;
	private PrintWriter output;
	private final Stack<CodeFragment> waiting;
	private final Stack<String> newInstances;
	private final String structOwnerMethodSeparator;
	private final HashMap<String, String> translations = new HashMap<String, String>();
	private final HashMap<String, String> conversionsToStructs = new HashMap<String, String>();

	public GLSLEncoder(int glslversion) {
		this.convertNumbersToChars = true;
		this.glslversion = glslversion;
		this.stack = new Stack<String>();
		this.typesStack = new Stack<String>();
		this.initialized = new ArrayList<String>();
		this.name2type = new HashMap<String, String>();
		this.constants = new HashMap<Object, String>();
		this.methodReplacements = new HashMap<String, String>();
		this.waiting = new Stack<CodeFragment>();
		this.newInstances = new Stack<String>();
		this.structOwnerMethodSeparator = "__";

		this.init();
	}

	public void init() {
		this.setGLSLTranslation("boolean", "bool");
		this.setGLSLTranslation("double", "float"); // not every GPU has double
		// precision;
		this.setGLSLTranslation(Vec2.class.getCanonicalName(), "vec2");
		this.setGLSLTranslation(Vec3.class.getCanonicalName(), "vec3");
		this.setGLSLTranslation(Vec4.class.getCanonicalName(), "vec4");
		this.setGLSLTranslation(Mat2.class.getCanonicalName(), "mat2");
		this.setGLSLTranslation(Mat3.class.getCanonicalName(), "mat3");
		this.setGLSLTranslation(Mat4.class.getCanonicalName(), "mat4");
		this.setGLSLTranslation(Integer.class.getCanonicalName(), "int");

		this.setGLSLTranslation(Math.class.getCanonicalName(), "");

		this.setGLSLTranslation(Sampler2D.class.getCanonicalName(), "sampler2D");

	}

	public void setGLSLTranslation(String javaType, String glslType) {
		this.translations.put(javaType, glslType);
	}

	public void addToStructConversion(String javaType, String structName) {
		this.conversionsToStructs.put(javaType, structName);
	}

	public boolean hasStructAttached(String javaType) {
		return this.conversionsToStructs.containsKey(javaType);
	}

	public void removeGLSLTranslation(String javaType) {
		this.translations.remove(javaType);
	}

	private String toGLSL(String type) {
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
		if (this.conversionsToStructs.containsKey(type)) {
			return this.conversionsToStructs.get(type) + end;
		}
		if (this.translations.containsKey(type)) {
			return this.translations.get(type) + end;
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

	public void convertNumbersToChar(boolean convert) {
		this.convertNumbersToChars = convert;
	}

	@Override
	public void createSourceCode(List<CodeFragment> in, PrintWriter out) {
		this.interpret(in);
		this.output = out;
		this.allowedToPrint = true;
		this.println("#version " + this.glslversion);
		for (int index = 0; index < in.size(); index++) {
			CodeFragment fragment = in.get(index);
			this.output = out;
			this.allowedToPrint = !fragment.forbiddenToPrint;
			if (!this.waiting.isEmpty()) {
				this.handleCodeFragment(this.waiting.pop(), index, in, out);
			}
			this.handleCodeFragment(fragment, index, in, out);
		}
		out.flush();
	}

	@Override
	public void onRequestResult(ArrayList<CodeFragment> fragments) {
		if (this.currentRequestType == STRUCT) {
			StructFragment currentStruct = (StructFragment) this.requestData;
			HashMap<String, String> fields = currentStruct.fields;
			for (int i = 0; i < fragments.size(); i++) {
				CodeFragment fragment = fragments.get(i);
				if (fragment.getClass() == FieldFragment.class) {
					FieldFragment fieldFrag = (FieldFragment) fragment;
					fields.put(fieldFrag.name, fieldFrag.type);
					fragment.forbiddenToPrint = true;
				}

				currentStruct.addChild(fragment);
			}
		}
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
		} else if (fragment.getClass() == StructFragment.class) {
			this.handleStructFragment((StructFragment) fragment, in, index, out);
		}
	}

	private void handleCompareFragment(CompareFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String right = this.stack.pop();
		String left = this.stack.pop();
		this.stack.push(left + this.space + (fragment.inferior ? "<" : ">") + this.space + right);
	}

	private void handleNotEqualCheckFragment(NotEqualCheckFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String right = this.stack.pop();
		String left = this.stack.pop();
		this.stack.push("(" + left + this.space + "!=" + this.space + right + ")");
	}

	private void handleEqualCheckFragment(EqualCheckFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String right = this.stack.pop();
		String left = this.stack.pop();
		this.stack.push("(" + left + this.space + "==" + this.space + right + ")");
	}

	private void handleNewInstanceFragment(NewInstanceFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		this.newInstances.push(fragment.type);
	}

	private void handleStructFragment(StructFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		this.println(this.getIndent() + "struct " + fragment.name);
		this.println(this.getIndent() + "{");
		this.indentation++;
		Iterator<String> it = fragment.fields.keySet().iterator();
		while (it.hasNext()) {
			String name = it.next();
			String type = this.toGLSL(fragment.fields.get(name));
			this.println(this.getIndent() + type + this.space + name + ";");
		}
		this.indentation--;
		this.println(this.getIndent() + "};");

		StartOfMethodFragment currentMethod = null;
		String instanceName = ("" + fragment.name.charAt(0)).toLowerCase() + fragment.name.substring(1) + "Instance";
		for (int i = 0; i < fragment.getChildren().size(); i++) {
			CodeFragment fragment1 = fragment.getChildren().get(i);
			if (fragment1 == null) {
				continue;
			}
			this.output = out;
			this.allowedToPrint = !fragment1.forbiddenToPrint;
			if (fragment1 instanceof StartOfMethodFragment) {
				StartOfMethodFragment method = (StartOfMethodFragment) fragment1;
				currentMethod = method;
				String oldName = currentMethod.name;
				method.varNameMap.put(0, instanceName);
				boolean isConstructor = false;
				if (currentMethod.name.equals("<init>") || currentMethod.name.equals(fragment.name + this.structOwnerMethodSeparator + "new")) {
					currentMethod.name = "new";
					method.returnType = fragment.name;
					isConstructor = true;
				} else if (!method.argumentsNames.contains(instanceName)) {
					method.argumentsNames.add(0, instanceName);
					method.argumentsTypes.add(0, fragment.name);
				}
				if (!currentMethod.name.startsWith(fragment.name + this.structOwnerMethodSeparator)) {
					currentMethod.name = fragment.name + this.structOwnerMethodSeparator + currentMethod.name;
				}
				String key = this.toGLSL(currentMethod.owner) + "." + oldName;
				this.methodReplacements.put(key, currentMethod.name);

				if (DEBUG && fragment1.getClass() == StartOfMethodFragment.class) {
					System.out.println("GLSLEncoder > Mapped " + key + " to " + currentMethod.name);
				}
			}
			if (fragment1 instanceof LoadVariableFragment) {
				LoadVariableFragment var = (LoadVariableFragment) fragment1;
				var.variableName = currentMethod.varNameMap.get(var.variableIndex);
			} else if (fragment1 instanceof StoreVariableFragment) {
				StoreVariableFragment var = (StoreVariableFragment) fragment1;
				var.variableName = currentMethod.varNameMap.get(var.variableIndex);
			}
			if (!this.waiting.isEmpty()) {
				this.handleCodeFragment(this.waiting.pop(), index, in, out);
			}

			this.allowedToPrint = !fragment1.forbiddenToPrint;
			if (fragment1.getClass() == EndOfMethodFragment.class && currentMethod.name.equals(fragment.name + this.structOwnerMethodSeparator + "new")) {
				this.println(this.getIndent() + "return " + instanceName + ";");
			}

			this.handleCodeFragment(fragment1, i, fragment.getChildren(), out);
			if (fragment1.getClass() == StartOfMethodFragment.class && currentMethod.name.equals(fragment.name + this.structOwnerMethodSeparator + "new")) {
				this.println(this.getIndent() + fragment.name + this.space + instanceName + ";");
			}
		}
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
				if (this.hasStructAttached(fieldFrag.type) && !this.loadedStructs.contains(this.toGLSL(fieldFrag.type))) {
					this.loadedStructs.add(this.toGLSL(fieldFrag.type));
					StructFragment struct = new StructFragment();
					struct.name = this.conversionsToStructs.get(fieldFrag.type);
					HashMap<String, String> fields = new HashMap<String, String>();
					struct.fields = fields;
					this.currentRequestType = STRUCT;
					this.requestData = struct;
					this.context.requestAnalysisForEncoder(this.context.provider.getBytes(fieldFrag.type.replace(".", "/")));
					in.add(i, struct);
					this.currentRequestType = 0;
					i--;
				}
			} else if (fragment.getClass() == StoreVariableFragment.class) {
				StoreVariableFragment storeFrag = (StoreVariableFragment) fragment;
				String type = storeFrag.variableType;
				if (this.hasStructAttached(type) && !this.loadedStructs.contains(this.toGLSL(type))) {
					this.loadedStructs.add(this.toGLSL(type));
					StructFragment struct = new StructFragment();
					struct.name = this.conversionsToStructs.get(type);
					HashMap<String, String> fields = new HashMap<String, String>();
					struct.fields = fields;
					this.currentRequestType = STRUCT;
					this.requestData = struct;
					this.context.requestAnalysisForEncoder(this.context.provider.getBytes(type.replace(".", "/")));
					in.add(i, struct);
					this.currentRequestType = 0;
					i--;
				}
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
		this.extensions.clear();
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
		this.stack.push("(" + a + this.space + (fragment.isDouble ? "||" : "|") + this.space + b + ")");
	}

	private void handleAndFragment(AndFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String b = this.stack.pop();
		String a = this.stack.pop();
		this.stack.push("(" + a + this.space + (fragment.isDouble ? "&&" : "&") + this.space + b + ")");
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
		String withoutPreviousCast = toCast;

		String previousType = null;
		if (withoutPreviousCast.startsWith("(")) {
			previousType = withoutPreviousCast.substring(1, withoutPreviousCast.indexOf(")") - 1);
		} else {
			previousType = this.toGLSL(this.currentMethod.varName2TypeMap.get(withoutPreviousCast));
		}
		if (previousType.equals(this.toGLSL(fragment.to))) {
			if (DEBUG) {
				System.out.println("GLSLEncoder > Cancelling cast for " + toCast);
			}
		} else {
			this.stack.push("(" + this.toGLSL(fragment.to) + ")" + toCast);
		}
	}

	private void handleModFragment(ModFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String a = this.stack.pop();
		String b = this.stack.pop();
		this.stack.push("mod(" + b + ", " + a + ")");
	}

	private void handleMethodCallFragment(MethodCallFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String s = "";
		String n = fragment.methodName;
		boolean isConstructor = false;
		if (n.equals("<init>")) {
			n = this.toGLSL(fragment.methodOwner);
			isConstructor = true;
			if (!this.newInstances.isEmpty()) {
				this.newInstances.pop();
			}
		}
		String key = fragment.methodName;
		if (this.toGLSL(fragment.methodOwner) != null && !this.toGLSL(fragment.methodOwner).equals("null") && !this.toGLSL(fragment.methodOwner).trim().equals("")) {
			key = this.toGLSL(fragment.methodOwner) + "." + key;
		}
		if (this.methodReplacements.containsKey(key)) {
			String nold = key;
			n = this.methodReplacements.get(key);
			if (DEBUG) {
				System.out.println("GLSLEncoder > Replacing " + nold + " by " + n);
			}
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
		for (CodeFragment child : fragment.getChildren()) {
			if (child.getClass() == AnnotationFragment.class) {
				AnnotationFragment annot = (AnnotationFragment) child;
				if (annot.name.equals(Substitute.class.getCanonicalName())) {
					if (!annot.values.get("value").equals("$")) {
						n = (String) annot.values.get("value");
					}
					if (annot.values.containsKey("ownerBefore")) {
						ownerBefore = (Boolean) annot.values.get("ownerBefore");
					}
					if (annot.values.containsKey("ownerPosition")) {
						ownerPosition = (Integer) annot.values.get("ownerPosition");
					}
					if (annot.values.containsKey("actsAsField")) {
						actsAsField = (Boolean) annot.values.get("actsAsField");
					}
					if (annot.values.containsKey("usesParenthesis")) {
						parenthesis = (Boolean) annot.values.get("usesParenthesis");
					}
				}
			}
		}
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
					s = n + (parenthesis ? "(" : "") + (owner != null ? owner + (argsStr.length() > 0 ? ", " : "") : "") + argsStr + (parenthesis ?
					                                                                                                                  ")" : "");
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
			String owner = this.toGLSL(fragment.methodOwner);
			if (owner != null && !owner.trim().equals("") && !owner.equals("null")) {
				ownership = owner + (n.length() > 0 ? "." : "");
			}
			this.stack.push(ownership + n + (parenthesis ? "(" : "") + argsStr + (parenthesis ? ")" : ""));
		} else {
			this.stack.push(n + (parenthesis ? "(" : "") + argsStr + (parenthesis ? ")" : ""));
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
		this.stack.push(b + "/" + a);
	}

	private void handleMulFragment(MulFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String a = this.stack.pop();
		String b = this.stack.pop();
		this.stack.push(b + "*" + a);
	}

	private void handleSubFragment(SubFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String a = this.stack.pop();
		String b = this.stack.pop();
		this.stack.push(b + "-" + a);
	}

	private void handleAddFragment(AddFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String a = this.stack.pop();
		String b = this.stack.pop();
		this.stack.push(b + "+" + a);
	}

	private void handleReturnValueFragment(ReturnValueFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		this.println(this.getIndent() + "return" + this.space + this.stack.pop() + ";" + this.getEndOfLine(this.currentLine));
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
		String glslType = this.toGLSL(this.currentMethod.varName2TypeMap.get(fragment.variableName));
		if (glslType.equals("bool")) {
			if (value.equals("0")) {
				value = "false";
			} else if (value.equals("1")) {
				value = "true";
			}
		} else if (glslType.equals("char")) {
			if (this.convertNumbersToChars) {
				try {
					value = "'" + Character.valueOf((char) Integer.parseInt(value)) + "'";
				} catch (Exception e) {
				}
			}
		}
		if (this.initialized.contains(fragment.variableName)) {
			this.println(this.getIndent() + fragment.variableName + " = " + value + ";" + this.getEndOfLine(this.currentLine));
		} else {
			this.initialized.add(fragment.variableName);
			this.println(this.getIndent() + this.toGLSL(this.currentMethod.varName2TypeMap.get(fragment.variableName)) + this.space + fragment.variableName + " = " + value + ";" + this.getEndOfLine(
					this.currentLine));
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
		String owner = this.toGLSL(this.stack.pop());
		String ownership = owner + ".";
		if (owner.equals("this")) {
			ownership = "";
		}
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
		String glslType = this.toGLSL(this.currentMethod.varName2TypeMap.get(fragment.fieldName));
		if (glslType.equals("bool")) {
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
		if (owner.equals("this")) {
			ownership = "";
		}
		this.println(this.getIndent() + ownership + fragment.fieldName + this.space + "=" + this.space + value + ";" + this.getEndOfLine(this.currentLine));
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
		this.stack.push(this.toGLSL(fragment.type) + s);
	}

	private void handleNewArrayFragment(NewArrayFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String s = "[" + this.stack.pop() + "]";
		this.stack.push(this.toGLSL(fragment.type) + s);
	}

	private void handleEndOfMethodFragment(EndOfMethodFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		if (fragment.name.equals("<init>")) {
			return;
		}
		this.println("}");
		this.indentation--;
	}

	private void handleStartOfMethodFragment(StartOfMethodFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		if (fragment.name.equals("<init>")) {
			return;
		}
		this.initialized.clear();
		this.println();
		String args = "";
		for (int i = 0; i < fragment.argumentsNames.size(); i++) {
			String s = this.toGLSL(fragment.argumentsTypes.get(i)) + this.space + fragment.argumentsNames.get(i);
			if (i != 0) {
				args += ", ";
			}
			args += s;
		}
		this.println(this.toGLSL(fragment.returnType) + this.space + fragment.name + "(" + args + ")\n{");
		this.indentation++;
	}

	private void handleFieldFragment(FieldFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		String storageType = null;
		for (CodeFragment child : fragment.getChildren()) {
			if (child instanceof AnnotationFragment) {
				AnnotationFragment annot = (AnnotationFragment) child;
				if (annot.name.equals(Uniform.class.getCanonicalName())) {
					storageType = "uniform";
				} else if (annot.name.equals(Attribute.class.getCanonicalName())) {
					storageType = "attribute";
					if (this.currentClass.superclass.equals(FragmentShader.class.getCanonicalName())) {
						throw new JLSLException("Attributes are not allowed in fragment shaders");
					}
				} else if (annot.name.equals(In.class.getCanonicalName())) {
					storageType = "in";
				} else if (annot.name.equals(Out.class.getCanonicalName())) {
					storageType = "out";
				} else if (annot.name.equals(Varying.class.getCanonicalName())) {
					storageType = "varying";
				} else if (annot.name.equals(Layout.class.getCanonicalName())) {
					int location = (Integer) annot.values.get("location");

					if (this.glslversion > 430 || this.extensions.contains("GL_ARB_explicit_uniform_location")) {
						out.print("layout(location = " + location + ") ");
					}
				}
			}
		}
		if (storageType == null) {
			storageType = "uniform";
		}
		if (fragment.access.isFinal()) {
			if (fragment.access.isStatic()) {
				this.println("#define" + this.space + fragment.name + this.space + fragment.initialValue);
				this.constants.put(fragment.initialValue, fragment.name);
			} else {
				storageType = "const";
				this.println(storageType + this.space + this.toGLSL(fragment.type) + this.space + fragment.name + this.space + "=" + this.space + fragment.initialValue + ";");
				this.constants.put(fragment.initialValue, fragment.name);
			}
		} else {
			if (fragment.initialValue != null) {
				this.println(storageType + this.space + this.toGLSL(fragment.type) + this.space + fragment.name + this.space + "=" + this.space + fragment.initialValue + ";");
			} else {
				this.println(storageType + this.space + this.toGLSL(fragment.type) + this.space + fragment.name + ";");
			}
		}
	}

	@SuppressWarnings ("unchecked")
	private void handleClassFragment(NewClassFragment fragment, List<CodeFragment> in, int index, PrintWriter out) {
		this.println("// Original class name: " + fragment.className + " compiled from " + fragment.sourceFile + " and of version " + fragment.classVersion);
		for (CodeFragment child : fragment.getChildren()) {
			if (child instanceof AnnotationFragment) {
				AnnotationFragment annotFragment = (AnnotationFragment) child;
				this.println();
				if (annotFragment.name.equals(Extensions.class.getCanonicalName())) {
					ArrayList<String> values = (ArrayList<String>) annotFragment.values.get("value");
					for (String extension : values) {
						this.println("#extension " + extension + " : enable" + this.getEndOfLine(this.currentLine));
					}
				}
			}
		}
	}

	private String getIndent() {
		String s = "";
		for (int i = 0; i < this.indentation; i++) {
			s += this.tab;
		}
		return s;
	}

}

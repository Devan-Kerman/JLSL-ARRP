package test;

import java.io.PrintWriter;

import classes.TestShader;
import net.devtech.jlsl.v0.api.ClasspathBytecodeProvider;
import org.jglrxavpok.jlsl.BytecodeDecoder;
import org.jglrxavpok.jlsl.JLSLContext;
import org.jglrxavpok.jlsl.glsl.GLSLEncoder;

public class NewTest {
	public static void main(String[] args) {
		BytecodeDecoder decoder = new BytecodeDecoder();
		GLSLEncoder encoder = new GLSLEncoder(120);
		JLSLContext context = new JLSLContext(decoder, encoder, ClasspathBytecodeProvider.forLocal(TestShader.class));
		PrintWriter writer = new PrintWriter(System.out);
		context.execute(TestShader.class, writer);
	}
}

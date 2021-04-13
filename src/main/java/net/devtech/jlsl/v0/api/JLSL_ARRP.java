package net.devtech.jlsl.v0.api;

import java.io.PrintWriter;

import net.devtech.arrp.api.RuntimeResourcePack;
import net.devtech.arrp.util.UnsafeByteArrayOutputStream;
import org.jglrxavpok.jlsl.BytecodeDecoder;
import org.jglrxavpok.jlsl.JLSLContext;
import org.jglrxavpok.jlsl.glsl.GLSLEncoder;
import org.jglrxavpok.jlsl.glsl.ShaderBase;

import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

public class JLSL_ARRP {
	private static final BytecodeDecoder DECODER = new BytecodeDecoder();
	private static final GLSLEncoder ENCODER = new GLSLEncoder(120);
	private static final JLSLContext CONTEXT = new JLSLContext(DECODER, ENCODER, ClasspathBytecodeProvider.forLocal(JLSL_ARRP.class));

	/**
	 * @param path where to stick the shader (this should include the extension eg. mymod:myshader.frag)
	 */
	public static void addShader(RuntimeResourcePack rrp, Identifier path, Class<? extends ShaderBase> shaderClass) {
		rrp.addAsyncResource(ResourceType.CLIENT_RESOURCES, new Identifier(path.getNamespace(), "shaders/" + path.getPath()), identifier -> {
			UnsafeByteArrayOutputStream output = new UnsafeByteArrayOutputStream(256);
			PrintWriter writer = new PrintWriter(output);
			CONTEXT.execute(shaderClass, writer);
			return output.getBytes();
		});
	}
}

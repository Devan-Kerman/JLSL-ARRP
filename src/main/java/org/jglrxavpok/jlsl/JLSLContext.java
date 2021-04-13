package org.jglrxavpok.jlsl;

import java.io.PrintWriter;
import java.util.ArrayList;

import net.devtech.jlsl.v0.api.ClasspathBytecodeProvider;
import org.jglrxavpok.jlsl.fragments.CodeFragment;

public class JLSLContext {

	public static JLSLContext currentInstance;
	private final CodeDecoder decoder;
	private final CodeEncoder encoder;
	private final ArrayList<CodeFilter> filters;
	private Object object;
	public final ClasspathBytecodeProvider provider;

	public JLSLContext(CodeDecoder decoder, CodeEncoder encoder, ClasspathBytecodeProvider provider) {
		this.provider = provider;
		JLSLContext.currentInstance = this;
		this.filters = new ArrayList<CodeFilter>();
		this.decoder = decoder;
		this.decoder.context = this;
		this.encoder = encoder;
		this.encoder.context = this;
	}

	public void addFilters(CodeFilter... filters) {
		for (CodeFilter filter : filters) {
			this.filters.add(filter);
		}
	}

	public void requestAnalysisForEncoder(Object data) {
		this.object = data;
		ArrayList<CodeFragment> fragments = new ArrayList<CodeFragment>();
		this.decoder.handleClass(data, fragments);
		ArrayList<CodeFragment> finalFragments = new ArrayList<CodeFragment>();
		for (CodeFragment frag : fragments) {
			if (frag != null) {
				finalFragments.add(this.filter(frag));
			}
		}
		this.encoder.onRequestResult(finalFragments);
	}

	private CodeFragment filter(CodeFragment fragment) {
		for (CodeFilter filter : this.filters) {
			fragment = filter.filter(fragment);
		}
		return fragment;
	}

	public void execute(Object data, PrintWriter out) {
		this.object = data;
		ArrayList<CodeFragment> fragments = new ArrayList<CodeFragment>();
		this.decoder.handleClass(data, fragments);
		ArrayList<CodeFragment> finalFragments = new ArrayList<CodeFragment>();
		for (CodeFragment frag : fragments) {
			if (frag != null) {
				finalFragments.add(this.filter(frag));
			}
		}
		this.encoder.createSourceCode(finalFragments, out);
	}

	public Object getCurrentObject() {
		return this.object;
	}
}

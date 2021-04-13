package net.devtech.jlsl.internal;

import java.io.InputStream;

import net.devtech.jlsl.v0.api.ClasspathBytecodeProvider;

public class ClassloaderClassFileResourceBytecodeProvider implements ClasspathBytecodeProvider {
	public final Iterable<ClassLoader> classLoaders;

	public ClassloaderClassFileResourceBytecodeProvider(Iterable<ClassLoader> loaders) {
		this.classLoaders = loaders;
	}

	@Override
	public InputStream getBytes(String internalName) {
		for (ClassLoader loader : this.classLoaders) {
			InputStream stream = loader.getResourceAsStream("/" + internalName + ".class");
			if(stream != null) {
				return stream;
			}
			stream = loader.getResourceAsStream(internalName + ".class");
			if(stream != null) {
				return stream;
			}
		}
		throw new IllegalArgumentException("Class File Not Found: " + internalName + "\n\tPossible Reasons:\n\t\t- generated class\n\t\t- incomplete classpath");
	}
}

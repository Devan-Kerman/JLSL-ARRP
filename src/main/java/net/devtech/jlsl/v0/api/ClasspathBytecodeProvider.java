package net.devtech.jlsl.v0.api;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import com.google.common.collect.ImmutableSet;
import net.devtech.jlsl.internal.ClassloaderClassFileResourceBytecodeProvider;

/**
 * an api interface for getting class files from the classpath
 */
public interface ClasspathBytecodeProvider {
	/**
	 * This is probably what you want. Grabs class files from the current context class loader, and the classloader that loaded
	 * ClasspathBytecodeProvider.
	 */
	static ClasspathBytecodeProvider forLocal(Class<?> currentClass) {
		return new ClassloaderClassFileResourceBytecodeProvider(ImmutableSet.of(Thread.currentThread().getContextClassLoader(),
				ClasspathBytecodeProvider.class.getClassLoader(), currentClass.getClassLoader()));
	}

	static ClasspathBytecodeProvider forClassLoaders(ClassLoader... classLoader) {
		return new ClassloaderClassFileResourceBytecodeProvider(Arrays.asList(classLoader));
	}

	static ClasspathBytecodeProvider forClassLoaders(Iterable<ClassLoader> classLoaders) {
		return new ClassloaderClassFileResourceBytecodeProvider(classLoaders);
	}

	/**
	 * @deprecated not recommended
	 */
	@Deprecated
	static ClasspathBytecodeProvider forSystemClassLoader() {
		return new ClassloaderClassFileResourceBytecodeProvider(Collections.singleton(ClassLoader.getSystemClassLoader()));
	}

	InputStream getBytes(String internalName);
}

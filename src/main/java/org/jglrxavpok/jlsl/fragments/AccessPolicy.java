package org.jglrxavpok.jlsl.fragments;

import org.objectweb.asm.*;

public class AccessPolicy implements Opcodes
{

	private final boolean isPublic;
	private final boolean isProtected;
	private final boolean isPrivate;
	private final boolean isStatic;
	private final boolean isAbstract;
	private final boolean isFinal;

	public AccessPolicy(int access)
	{
		this.isPrivate = this.hasModifier(access, ACC_PRIVATE);
		this.isProtected = this.hasModifier(access, ACC_PROTECTED);
		this.isPublic = this.hasModifier(access, ACC_PUBLIC);
		this.isStatic = this.hasModifier(access, ACC_STATIC);
		this.isAbstract = this.hasModifier(access, ACC_ABSTRACT);
		this.isFinal = this.hasModifier(access, ACC_FINAL);
	}

	public boolean isAbstract()
	{
		return this.isAbstract;
	}

	public boolean isFinal()
	{
		return this.isFinal;
	}

	public boolean isPublic()
	{
		return this.isPublic;
	}

	public boolean isProtected()
	{
		return this.isProtected;
	}

	public boolean isPrivate()
	{
		return this.isPrivate;
	}

	public boolean isStatic()
	{
		return this.isStatic;
	}

	private boolean hasModifier(int i, int modifier)
	{
		return (i | modifier) == i;
	}
}

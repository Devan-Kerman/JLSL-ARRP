package org.jglrxavpok.jlsl;

import org.jglrxavpok.jlsl.fragments.*;

@FunctionalInterface
public interface CodeFilter
{
	CodeFragment filter(CodeFragment fragment);
}

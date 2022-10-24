package args4j.spi;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import args4j.CmdLineException;
import args4j.CmdLineParser;
import args4j.IllegalAnnotationError;

/**
 * {@link Setter} that sets to a {@link Method}.
 *
 * @author Kohsuke Kawaguchi
 */
public final class MethodSetter implements Setter {
	private final CmdLineParser parser;
	private final Object bean;
	private final Method m;

	public MethodSetter(CmdLineParser parser, Object bean, Method m) {
		this.parser = parser;
		this.bean = bean;
		this.m = m;
		if (m.getParameterTypes().length != 1) {
			throw new IllegalAnnotationError(Messages.ILLEGAL_METHOD_SIGNATURE.format(m));
		}
	}

	@Override
	public Class getType() {
		return m.getParameterTypes()[0];
	}

	@Override
	public boolean isMultiValued() {
		// multiple values can be handled by calling methods repeatedly
		return true;
	}

	@Override
	public FieldSetter asFieldSetter() {
		return null;
	}

	@Override
	public AnnotatedElement asAnnotatedElement() {
		return m;
	}

	@Override
	public void addValue(Object value) throws CmdLineException {
		try {
			try {
				m.invoke(bean, value);
			} catch (IllegalAccessException ex) {
				// try again
				m.setAccessible(true);
				try {
					m.invoke(bean, value);
				} catch (IllegalAccessException e) {
					throw new IllegalAccessError(e.getMessage());
				}
			}
		} catch (InvocationTargetException e) {
			Throwable t = e.getTargetException();
			if (t instanceof RuntimeException) {
				throw (RuntimeException) t;
			}
			if (t instanceof Error) {
				throw (Error) t;
			}
			if (t instanceof CmdLineException) {
				throw (CmdLineException) t;
			}

			// otherwise wrap
			if (t != null) {
				throw new CmdLineException(parser, t);
			} else {
				throw new CmdLineException(parser, e);
			}
		}
	}
}

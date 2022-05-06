package args4j.spi;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import args4j.IllegalAnnotationError;

/**
 * {@link Setter} that sets multiple values to a collection {@link Field}.
 *
 * @author Kohsuke Kawaguchi
 */
final class MultiValueFieldSetter implements Setter {
	private final Object bean;
	private final Field f;

	public MultiValueFieldSetter(Object bean, Field f) {
		this.bean = bean;
		this.f = f;

		if (!List.class.isAssignableFrom(f.getType())) {
			throw new IllegalAnnotationError(Messages.ILLEGAL_FIELD_SIGNATURE.format(f.getType()));
		}
	}

	@Override
	public boolean isMultiValued() {
		return true;
	}

	@Override
	public FieldSetter asFieldSetter() {
		return new FieldSetter(bean, f);
	}

	@Override
	public AnnotatedElement asAnnotatedElement() {
		return f;
	}

	@Override
	public Class getType() {
		// TODO: compute this correctly
		Type t = f.getGenericType();
		if (t instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) t;
			t = pt.getActualTypeArguments()[0];
			if (t instanceof Class) {
				return (Class) t;
			}
		}
		return Object.class;
	}

	@Override
	public void addValue(Object value) {
		try {
			doAddValue(bean, value);
		} catch (IllegalAccessException ex) {
			// try again
			f.setAccessible(true);
			try {
				doAddValue(bean, value);
			} catch (IllegalAccessException e) {
				throw new IllegalAccessError(e.getMessage());
			}
		}
	}

	private void doAddValue(Object bean, Object value) throws IllegalAccessException {
		Object o = f.get(bean);
		if (o == null) {
			o = new ArrayList();
			f.set(bean, o);
		}
		if (!(o instanceof List)) {
			throw new IllegalAnnotationError(Messages.ILLEGAL_LIST.format(f));
		}

		((List) o).add(value);
	}
}

package args4j.spi;

import args4j.Option;

/**
 * Implementation of @Option so we can instantiate it.
 *
 * @author Jan Materne
 */
public class OptionImpl extends AnnotationImpl implements Option {
	public OptionImpl(ConfigElement ce) throws ClassNotFoundException {
		super(Option.class, ce);
		name = ce.name;
	}

	public String name;

	@Override
	public String name() {
		return name;
	}

	public String[] depends;

	@Override
	public String[] depends() {
		return depends;
	}

	public String[] forbids;

	@Override
	public String[] forbids() {
		assert false; // FIXME
		return depends; // is this return value correct?
	}

	public String[] groups;

	@Override
	public String[] groups() {
		return groups;
	}

	public boolean showDefault;

	@Override
	public boolean showDefault() {
		return showDefault;
	}
}

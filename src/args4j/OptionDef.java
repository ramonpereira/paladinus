package args4j;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import args4j.spi.OptionHandler;

/**
 * Run-time copy of the Option or Argument annotation. By definition, unnamed
 * options are arguments (and instances of this class). Named options are
 * actually a subclass.
 *
 * @author Mark Sinke
 */
public class OptionDef {
	private final String usage;
	private final String metaVar;
	private final boolean required;
	private final boolean help;
	private final boolean hidden;
	private final boolean multiValued;
	private final Class<? extends OptionHandler> handler;

	// myND adaptions
	private final Set<Set<String>> groups;
	private final boolean showDefault;
	private Object defaultValue;

	public OptionDef(Argument a, boolean forceMultiValued) {
		this(a.usage(), a.metaVar(), a.required(), false, a.hidden(), a.handler(), a.multiValued() || forceMultiValued,
				new String[0], false);
	}

	protected OptionDef(String usage, String metaVar, boolean required, boolean help, boolean hidden,
			Class<? extends OptionHandler> handler, boolean multiValued, String[] group, boolean showDefault) {
		this.usage = usage;
		this.metaVar = metaVar;
		this.required = required;
		this.help = help;
		this.hidden = hidden;
		this.handler = handler;
		this.multiValued = multiValued;
		// myND adaption
		Set<Set<String>> groups_ = new HashSet<Set<String>>();
		for (String string : group) {
			HashSet<String> set = new HashSet<String>();
			for (String g : string.split(",")) {
				set.add(g);
			}
			groups_.add(set);
		}
		groups = Collections.unmodifiableSet(groups_); // myND adaption
		this.showDefault = showDefault; // myND Adaption
	}

	public String usage() {
		return usage;
	}

	public String metaVar() {
		return metaVar;
	}

	public boolean required() {
		return required;
	}

	public boolean help() {
		return help;
	}

	/**
	 * Value from {@link Option#hidden()} or {@link Argument#hidden()}
	 */
	public boolean hidden() {
		return hidden;
	}

	public Class<? extends OptionHandler> handler() {
		return handler;
	}

	public boolean isMultiValued() {
		return multiValued;
	}

	public boolean isArgument() {
		return true;
	}

	@Override
	public String toString() {
		return metaVar() != null ? metaVar() : "ARG";
	}

	// myND adaption
	public Set<Set<String>> getGroups() {
		return groups;
	}

	// myND adaption
	public void setDefault(Object o) {
		if (o instanceof Boolean) {
			if ((Boolean) o) {
				assert false : "Boolean value has to be false by default.";
			} else {
				defaultValue = "false";
			}
		} else if (o instanceof Integer) {
			if (((Integer) o).equals(Integer.MAX_VALUE)) {
				defaultValue = "inf";
			} else {
				defaultValue = o;
			}
		} else {
			defaultValue = o;
		}
	}

	// myND adaption
	public boolean showDefault() {
		return showDefault;
	}

	// myND adaption
	public Object getDefault() {
		return defaultValue;
	}
}

package args4j.spi;

import args4j.CmdLineException;
import args4j.CmdLineParser;
import args4j.OptionDef;

/**
 * String {@link OptionHandler}.
 *
 * @author Kohsuke Kawaguchi
 */
public class StringOptionHandler extends OptionHandler<String> {
	public StringOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super String> setter) {
		super(parser, option, setter);
	}

	@Override
	public int parseArguments(Parameters params) throws CmdLineException {
		setter.addValue(params.getParameter(0));
		return 1;
	}

	@Override
	public String getDefaultMetaVariable() {
		return Messages.DEFAULT_META_STRING_OPTION_HANDLER.format();
	}
}

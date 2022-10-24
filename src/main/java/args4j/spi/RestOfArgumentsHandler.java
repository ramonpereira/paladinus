package args4j.spi;

import args4j.Argument;
import args4j.CmdLineException;
import args4j.CmdLineParser;
import args4j.OptionDef;

/**
 * Eagerly grabs all the arguments.
 *
 * <p>
 * Used with {@link Argument}, this implements a semantics where non-option
 * token causes the option parsing to terminate. An example of this is
 * <tt>ssh(1)</tt>, where <samp>ssh -p 222 abc</samp> will treat <samp>-p</samp>
 * as an option to <tt>ssh</tt>, but <samp>ssh abc -p 222</samp> is considered
 * to have no option for <tt>ssh</tt>.
 *
 * @author Kohsuke Kawaguchi
 */
public class RestOfArgumentsHandler extends OptionHandler<String> {
	public RestOfArgumentsHandler(CmdLineParser cmdLineParser, OptionDef optionDef, Setter<String> setter) {
		super(cmdLineParser, optionDef, setter);
	}

	@Override
	public int parseArguments(Parameters parameters) throws CmdLineException {
		for (int i = 0; i < parameters.size(); i++) {
			setter.addValue(parameters.getParameter(i));
		}
		return parameters.size();
	}

	@Override
	public String getDefaultMetaVariable() {
		return Messages.DEFAULT_META_REST_OF_ARGUMENTS_HANDLER.format();
	}
}

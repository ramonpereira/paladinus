package args4j.spi;

import java.net.MalformedURLException;
import java.net.URL;

import args4j.CmdLineException;
import args4j.CmdLineParser;
import args4j.OptionDef;

/**
 * {@link URL} {@link OptionHandler}.
 *
 * @author Kohsuke Kawaguchi
 */
public class URLOptionHandler extends OptionHandler<URL> {
	public URLOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super URL> setter) {
		super(parser, option, setter);
	}

	@Override
	public int parseArguments(Parameters params) throws CmdLineException {
		String param = params.getParameter(0);
		try {
			setter.addValue(new URL(param));
			return 1;
		} catch (MalformedURLException e) {
			throw new CmdLineException(owner, Messages.ILLEGAL_OPERAND, params.getParameter(-1), param);
		}
	}

	@Override
	public String getDefaultMetaVariable() {
		return Messages.DEFAULT_META_URL_OPTION_HANDLER.format();
	}
}

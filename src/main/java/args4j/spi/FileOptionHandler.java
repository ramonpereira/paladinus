package args4j.spi;

import java.io.File;

import args4j.CmdLineException;
import args4j.CmdLineParser;
import args4j.OptionDef;

/**
 * {@link File} {@link OptionHandler}.
 *
 * @author Kohsuke Kawaguchi
 */
public class FileOptionHandler extends OneArgumentOptionHandler<File> {
	public FileOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super File> setter) {
		super(parser, option, setter);
	}

	@Override
	protected File parse(String argument) throws CmdLineException {
		return new File(argument);
	}

	@Override
	public String getDefaultMetaVariable() {
		return Messages.DEFAULT_META_FILE_OPTION_HANDLER.format();
	}
}

package args4j.spi;

import java.io.File;

import args4j.CmdLineException;
import args4j.CmdLineParser;
import args4j.OptionDef;

// import java.nio.file.Path; Note: requires Java 1.7
// import java.nio.file.Paths;

/**
 * Takes a single argument to the option and maps that to {@link Path}.
 *
 * @author kmahoney
 */
public class PathOptionHandler extends OneArgumentOptionHandler<File> {
	public PathOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super File> setter) {
		super(parser, option, setter);
	}

	@Override
	protected File parse(String argument) throws NumberFormatException, CmdLineException {
		try {
			return new File(argument);
			// return File.get(argument);
		} catch (Exception e) {
			throw new CmdLineException(owner, Messages.ILLEGAL_PATH, argument);
		}
	}

	@Override
	public String getDefaultMetaVariable() {
		return Messages.DEFAULT_META_PATH_OPTION_HANDLER.format();
	}
}

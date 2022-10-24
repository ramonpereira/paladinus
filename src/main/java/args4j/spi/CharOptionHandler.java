package args4j.spi;

import args4j.CmdLineException;
import args4j.CmdLineParser;
import args4j.OptionDef;

/**
 * {@link Char} {@link OptionHandler} {@link OneArgumentOptionHandler}
 *
 * @author Jan Materne
 * @since 2.0.9
 */
public class CharOptionHandler extends OneArgumentOptionHandler<Character> {

	public CharOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super Character> setter) {
		super(parser, option, setter);
	}

	@Override
	protected Character parse(String argument) throws NumberFormatException, CmdLineException {
		if (argument.length() != 1) {
			throw new CmdLineException(owner, Messages.ILLEGAL_CHAR, argument);
		}
		return argument.charAt(0);
	}
}

package paladinus.util;

/**
 * Pair parameterized by the types of the two elements.
 *
 * @author Robert Mattmueller
 *
 * @param <A> Type of first entry.
 * @param <B> Type of second entry.
 */
public class Pair<A, B> {

	/**
	 * First entry
	 */
	public A first;

	/**
	 * Second entry
	 */
	public B second;

	/**
	 * Construct an ordered pair consisting of two entries <tt>first</tt> and
	 * <tt>second</tt>.
	 *
	 * @param first  First entry
	 * @param second Second entry
	 */
	public Pair(A first, B second) {
		this.first = first;
		this.second = second;
	}

	/**
	 * Test equality of two pairs.
	 *
	 * @return true iff given object is equal to this pair.
	 */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Pair<?, ?>)) {
			return false;
		}
		Pair<?, ?> p = (Pair<?, ?>) o;
		return p.first.equals(first) && p.second.equals(second);
	}

	/**
	 * Get hash code of this pair.
	 *
	 * @return hash code
	 */
	@Override
	public int hashCode() {
		return 2 * first.hashCode() + 3 * second.hashCode();
	}

	/**
	 * Get string representation of this pair.
	 *
	 * @return string representation
	 */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("(");
		buffer.append(first.toString());
		buffer.append(",");
		buffer.append(second.toString());
		buffer.append(")");
		return buffer.toString();
	}
}

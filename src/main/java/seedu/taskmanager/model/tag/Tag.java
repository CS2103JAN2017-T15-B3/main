
package seedu.taskmanager.model.tag;

import seedu.taskmanager.commons.exceptions.IllegalValueException;

/**
 * Represents a Tag in the task manager. Guarantees: immutable; name is valid as declared in
 * {@link #isValidTagName(String)}
 */
public class Tag {

    public static final String MESSAGE_TAG_CONSTRAINTS = "Tags names should be alphanumeric";
    public static final String TAG_VALIDATION_REGEX = "\\p{Alnum}+";

    public final String tagName;

    /**
     * Validates given tag name.
     * @throws IllegalValueException
     *         if the given tag name string is invalid.
     */
    public Tag(String argName) throws IllegalValueException {
        assert argName != null;
        String trimmedName = getTagNameFromArgName(argName);
        if (!isValidTagName(trimmedName)) {
            throw new IllegalValueException(MESSAGE_TAG_CONSTRAINTS);
        }
        this.tagName = trimmedName;
    }

    /**
     * Hot-fix for tags as tags stored have a '/' character
     * @param argName
     * @return trimmed argName without the '/' character
     */
    private String getTagNameFromArgName(String argName) {
        if (argName.startsWith("/")) {
            return argName.substring(1).trim();
        }

        return argName.trim();
    }

    /**
     * Returns true if a given string is a valid tag name.
     */
    public static boolean isValidTagName(String test) {
        return test.matches(TAG_VALIDATION_REGEX);
    }

    @Override
    public boolean equals(Object other) {
        return other == this // short circuit if same object
                || (other instanceof Tag // instanceof handles nulls
                        && this.tagName.equals(((Tag) other).tagName)); // state
                                                                        // check
    }

    @Override
    public int hashCode() {
        return tagName.hashCode();
    }

    /**
     * Format state as text for viewing.
     */
    @Override
    public String toString() {
        return '[' + tagName + ']';
    }

}

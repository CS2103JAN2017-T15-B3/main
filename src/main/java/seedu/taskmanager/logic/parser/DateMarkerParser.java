package seedu.taskmanager.logic.parser;

import static seedu.taskmanager.commons.core.Messages.MESSAGE_REPEATED_MARKERS_FOUND;
import static seedu.taskmanager.logic.parser.CliSyntax.ACCEPTED_END_DATE_MARKER;
import static seedu.taskmanager.logic.parser.CliSyntax.ACCEPTED_START_DATE_MARKER;
import static seedu.taskmanager.logic.parser.CliSyntax.PREFIX_ENDDATE;
import static seedu.taskmanager.logic.parser.CliSyntax.PREFIX_STARTDATE;

import java.util.HashMap;

import seedu.taskmanager.commons.exceptions.IllegalValueException;
import seedu.taskmanager.logic.parser.ArgumentTokenizer.Prefix;

// @@author A0140417R
/**
 * Markers are words that indicates that the following argument is a date. e.g some name <from> [some date] <to>
 * [another date] In this case, from and to are markers and will be replaced with the relevant prefixes. Accepted start
 * date markers and end date markers can be found at {@code CliSyntax.class}
 * @author zeticous
 */
public class DateMarkerParser {
    private static final String EMPTY_SPACE = "\\s+";
    private static final String WHITE_SPACE = " ";

    /**
     * Replaces the markers with the respective start dates and end date prefixes.
     * @param argString
     * @return String with markers replaced with prefixes
     * @throws IllegalValueException
     *         when markers from the same group is detected
     */
    public static String replaceMarkersWithPrefix(String argString) throws IllegalValueException {
        assert argString != null;
        DateMarkerMap markerMap = new DateMarkerMap();
        String[] splittedArgs = argString.split(EMPTY_SPACE);
        StringBuilder builder = new StringBuilder();

        int currentIndex = 0;
        for (String string : splittedArgs) {
            if (markerMap.contains(string)) {
                if (currentIndex != splittedArgs.length
                        && DateTimeUtil.isValidDateString(splittedArgs[currentIndex + 1])) {
                    Prefix assignedPrefix = markerMap.get(string);
                    if (markerMap.hasRepeatedMarker(assignedPrefix)) {
                        throw new IllegalValueException(MESSAGE_REPEATED_MARKERS_FOUND);
                    }
                    string = markerMap.get(string).getPrefix();
                }
            }
            builder.append(string + WHITE_SPACE);
            currentIndex++;
        }

        return builder.toString().trim();
    }

    /**
     * Wrapper class to store 2 hashmaps. One with the marker string and its respective prefix. One with the prefix and
     * its respective amount of appearance.
     * @author zeticous
     */
    public static class DateMarkerMap {
        final HashMap<String, Prefix> markerMap;
        final HashMap<Prefix, Integer> markerCountMap;

        /**
         * Creates the wrapper class with the hashmaps assigned
         */
        DateMarkerMap() {
            this.markerMap = new HashMap<String, Prefix>();
            this.markerCountMap = new HashMap<Prefix, Integer>();

            markerCountMap.put(PREFIX_STARTDATE, 0);
            markerCountMap.put(PREFIX_ENDDATE, 0);

            for (String string : ACCEPTED_START_DATE_MARKER) {
                markerMap.put(string, PREFIX_STARTDATE);
            }
            for (String string : ACCEPTED_END_DATE_MARKER) {
                markerMap.put(string, PREFIX_ENDDATE);
            }
        }

        /**
         * Checks if a particular string is a marker.
         * @param argument
         * @return true if it is a marker
         */
        public boolean contains(String argument) {
            return markerMap.containsKey(argument);
        }

        /**
         * Get the prefix assigned to the argument
         * @param argument
         * @return prefix respective to the argument, null if argument is not marker
         */
        public Prefix get(String argument) {
            Prefix outputPrefix = markerMap.get(argument);
            int currentCount = markerCountMap.get(outputPrefix);
            markerCountMap.put(outputPrefix, ++currentCount);
            return outputPrefix;
        }

        /**
         * @param prefix
         * @return true if there is more than 1 instance of the marker of the same group.
         */
        public boolean hasRepeatedMarker(Prefix prefix) {
            return (markerCountMap.get(prefix) > 1);
        }
    }
}

package org.workcraft.plugins.stg;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.workcraft.exceptions.ArgumentException;
import org.workcraft.plugins.stg.SignalTransition.Direction;
import org.workcraft.util.Pair;
import org.workcraft.util.Triple;

public class LabelParser {
	private static final Pattern signalTransitionPattern = Pattern
			.compile("([_A-Za-z][_A-Za-z0-9]*)([\\+\\-\\~])(\\/([0-9]+))?");

	private static final Pattern signalTransitionPattern2 = Pattern
		.compile("([_A-Za-z][_A-Za-z0-9]*[\\+\\-\\~])(\\/([0-9]+))?");
	
	public static Triple<String, SignalTransition.Direction, Integer> parseFull(String s) {
		final Matcher matcher = signalTransitionPattern.matcher(s);
		
		if (!matcher.find())
			throw new ArgumentException("\"" + s
					+ "\" is not a valid STG label.");
 
		if (! (matcher.end() == s.length()))
			throw new ArgumentException("\"" + s
					+ "\" is not a valid STG label.");
			
		final String instanceGroup = matcher.group(4);
		
		final Direction second;
		
		if (matcher.group(2).equals("+"))
			second = SignalTransition.Direction.PLUS;
		else if (matcher.group(2).equals("-"))
			second = SignalTransition.Direction.MINUS;
		else
			second = SignalTransition.Direction.TOGGLE;
		
		return Triple.of(matcher.group(1),
				second, instanceGroup == null? null : Integer
						.parseInt(instanceGroup));
	}
	
	public static Pair<String, Integer> parse(String s) {
		final Matcher matcher = signalTransitionPattern2.matcher(s);
		
		if (!matcher.find())
			throw new ArgumentException("\"" + s
					+ "\" is not a valid STG label.");
 
		if (! (matcher.end() == s.length()))
			throw new ArgumentException("\"" + s
					+ "\" is not a valid STG label.");
			
		final String instanceGroup = matcher.group(3);
		
		return Pair.of(matcher.group(1), instanceGroup == null? 0 : Integer.parseInt(instanceGroup));
	}
}

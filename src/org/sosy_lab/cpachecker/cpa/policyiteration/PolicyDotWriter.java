package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.rationals.Rational;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

/**
 * Converts a set of invariants to the pretty text representation.
 */
public class PolicyDotWriter {
  public String toDOTLabel(Map<Template, PolicyBound> data) {
    StringBuilder b = new StringBuilder();
    b.append("\n");

    // TODO: might want to do it as a pre-processing rather than
    // only for printing.

    // Convert to the readable format.
    Map<LinearExpression, PolicyBound> toSort = new HashMap<>();

    // Pretty-printing is tricky.
    Map<LinearExpression, Rational> lessThan = new HashMap<>();
    Map<LinearExpression, Pair<Rational, Rational>> bounded
        = new HashMap<>();
    Map<LinearExpression, Rational> equal = new HashMap<>();

    for (Map.Entry<Template, PolicyBound> e : data.entrySet()) {
      toSort.put(e.getKey().linearExpression, e.getValue());
    }
    while (toSort.size() > 0) {
      LinearExpression template, negTemplate;
      template = toSort.keySet().iterator().next();
      Rational upperBound = toSort.get(template).bound;

      negTemplate = template.negate();

      toSort.remove(template);

      if (toSort.containsKey(negTemplate)) {
        Rational lowerBound = toSort.get(negTemplate).bound.negate();
        toSort.remove(negTemplate);

        // Rotate the pair if necessary.
        boolean negated = false;
        if (template.toString().startsWith("-")) {
          negated = true;
        }

        if (lowerBound.equals(upperBound)) {
          if (negated) {
            equal.put(template.negate(), lowerBound.negate());
          } else {
            equal.put(template, lowerBound);
          }
        } else {
          if (negated) {
            bounded.put(
                template.negate(), Pair.of(upperBound.negate(), lowerBound.negate()));
          } else {
            bounded.put(
                template, Pair.of(lowerBound, upperBound));
          }
        }
      } else {
        lessThan.put(template, upperBound);
      }
    }

    // Print equals.
    for (Map.Entry<LinearExpression, Rational> entry : equal.entrySet()) {
      b.append(entry.getKey())
          .append("=")
          .append(entry.getValue())
          .append("\n");
    }

    // Print bounded.
    for (Map.Entry<LinearExpression, Pair<Rational, Rational>> entry
        : bounded.entrySet()) {
      b
          .append(entry.getValue().getFirst())
          .append("≤")
          .append(entry.getKey())
          .append("≤")
          .append(entry.getValue().getSecond())
          .append("\n");
    }

    // Print less-than.
    for (Map.Entry<LinearExpression, Rational> entry : lessThan.entrySet()) {
      b.append(entry.getKey())
          .append("≤")
          .append(entry.getValue())
          .append("\n");
    }

    return b.toString();

  }
}

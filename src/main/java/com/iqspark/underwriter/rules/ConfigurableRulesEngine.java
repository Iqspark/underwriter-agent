package com.iqspark.underwriter.rules;

import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.model.LineOfBusiness;
import com.iqspark.underwriter.domain.model.Submission;
import com.iqspark.underwriter.rules.config.Condition;
import com.iqspark.underwriter.rules.config.RuleConfigLoader;
import com.iqspark.underwriter.rules.config.RuleDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates the YAML rule packs against the facts produced by {@link FactExtractor}, emitting a
 * {@link Finding} for every rule whose line matches the submission and whose conditions all hold.
 * Deterministic and pure: the same submission always yields the same findings.
 */
@Component
public class ConfigurableRulesEngine {

    private static final Pattern TOKEN = Pattern.compile("\\{(\\w+)}");

    private final RuleConfigLoader loader;
    private final FactExtractor factExtractor;

    public ConfigurableRulesEngine(RuleConfigLoader loader, FactExtractor factExtractor) {
        this.loader = loader;
        this.factExtractor = factExtractor;
    }

    public List<Finding> evaluate(Submission submission) {
        Map<String, Object> facts = factExtractor.extract(submission);
        LineOfBusiness line = submission.effectiveLine();

        List<Finding> findings = new ArrayList<>();
        for (RuleDefinition rule : loader.rules()) {
            if (!rule.appliesTo(line)) {
                continue;
            }
            if (allMatch(rule, facts)) {
                findings.add(new Finding(
                        rule.code(),
                        rule.severity(),
                        rule.category(),
                        interpolate(rule.message(), facts),
                        interpolate(rule.rationale(), facts),
                        rule.id()));
            }
        }
        return findings;
    }

    private boolean allMatch(RuleDefinition rule, Map<String, Object> facts) {
        for (Condition c : rule.all()) {
            if (!evaluate(c, facts)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluate(Condition c, Map<String, Object> facts) {
        Object fact = facts.get(c.fact());
        Object value = c.value();
        return switch (c.op()) {
            case "isTrue" -> Boolean.TRUE.equals(fact);
            case "isFalse" -> Boolean.FALSE.equals(fact);
            case "isNull" -> fact == null;
            case "isNotNull" -> fact != null;
            case "eq" -> equals(fact, value);
            case "ne" -> !equals(fact, value);
            case "eqIgnoreCase" -> fact != null && value != null
                    && fact.toString().equalsIgnoreCase(value.toString());
            case "gt" -> compare(fact, value) > 0;
            case "gte" -> compare(fact, value) >= 0;
            case "lt" -> compare(fact, value) < 0;
            case "lte" -> compare(fact, value) <= 0;
            default -> false;
        };
    }

    private static boolean equals(Object fact, Object value) {
        if (fact == null || value == null) {
            return fact == value;
        }
        Double a = toDouble(fact);
        Double b = toDouble(value);
        if (a != null && b != null) {
            return a.doubleValue() == b.doubleValue();
        }
        return fact.toString().equals(value.toString());
    }

    /** Numeric comparison; returns 0 (no match for gt/lt) when either side isn't numeric. */
    private static int compare(Object fact, Object value) {
        Double a = toDouble(fact);
        Double b = toDouble(value);
        if (a == null || b == null) {
            return 0;
        }
        return Double.compare(a, b);
    }

    private static Double toDouble(Object o) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        if (o instanceof Boolean b) {
            return b ? 1.0 : 0.0;
        }
        if (o instanceof String s) {
            try {
                return Double.valueOf(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static String interpolate(String template, Map<String, Object> facts) {
        if (template == null || template.indexOf('{') < 0) {
            return template;
        }
        Matcher m = TOKEN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            Object val = facts.get(m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(val == null ? "" : val.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}

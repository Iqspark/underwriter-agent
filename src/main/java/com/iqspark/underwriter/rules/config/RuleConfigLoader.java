package com.iqspark.underwriter.rules.config;

import com.iqspark.underwriter.domain.decision.Severity;
import com.iqspark.underwriter.domain.model.LineOfBusiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads the rule packs (rules as data, not code — ADR-0018). Reads {@code rules/*.yml} from the
 * classpath by default, or an external directory via {@code underwriter.rules.dir}. Every rule in a
 * line file (e.g. {@code vacant-home.yml}) is auto-scoped to that line; {@code shared.yml} rules
 * apply to all lines unless they declare an explicit {@code lines:} restriction.
 */
@Component
public class RuleConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(RuleConfigLoader.class);

    private final List<RuleDefinition> rules;

    public RuleConfigLoader(@Value("${underwriter.rules.dir:}") String externalDir) {
        this.rules = externalDir == null || externalDir.isBlank()
                ? loadFromClasspath()
                : loadFromDirectory(externalDir);
        log.info("Loaded {} underwriting rules", rules.size());
    }

    public List<RuleDefinition> rules() {
        return rules;
    }

    private List<RuleDefinition> loadFromClasspath() {
        List<RuleDefinition> all = new ArrayList<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:rules/*.yml");
            for (Resource r : resources) {
                String filename = r.getFilename();
                try (InputStream in = r.getInputStream()) {
                    all.addAll(parse(in, defaultLineFor(filename)));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load rule packs from classpath", e);
        }
        return List.copyOf(all);
    }

    private List<RuleDefinition> loadFromDirectory(String dir) {
        List<RuleDefinition> all = new ArrayList<>();
        try (var stream = Files.list(Path.of(dir))) {
            List<Path> files = stream.filter(p -> p.toString().endsWith(".yml")).sorted().toList();
            for (Path p : files) {
                try (InputStream in = Files.newInputStream(p)) {
                    all.addAll(parse(in, defaultLineFor(p.getFileName().toString())));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load rule packs from " + dir, e);
        }
        return List.copyOf(all);
    }

    @SuppressWarnings("unchecked")
    private List<RuleDefinition> parse(InputStream in, LineOfBusiness defaultLine) {
        Map<String, Object> doc = new Yaml().load(in);
        List<RuleDefinition> out = new ArrayList<>();
        if (doc == null || doc.get("rules") == null) {
            return out;
        }
        List<Map<String, Object>> ruleMaps = (List<Map<String, Object>>) doc.get("rules");
        for (Map<String, Object> m : ruleMaps) {
            out.add(toRule(m, defaultLine));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private RuleDefinition toRule(Map<String, Object> m, LineOfBusiness defaultLine) {
        LineOfBusiness line = m.containsKey("line") ? lineOf((String) m.get("line")) : defaultLine;
        List<LineOfBusiness> lines = null;
        if (m.get("lines") instanceof List<?> rawLines) {
            lines = rawLines.stream().map(o -> lineOf(o.toString())).toList();
        }

        List<Condition> conditions = new ArrayList<>();
        if (m.get("all") instanceof List<?> rawConds) {
            for (Object o : rawConds) {
                Map<String, Object> cm = (Map<String, Object>) o;
                conditions.add(new Condition(
                        (String) cm.get("fact"),
                        (String) cm.get("op"),
                        cm.get("value")));
            }
        }

        return new RuleDefinition(
                (String) m.get("id"),
                line,
                lines,
                (String) m.get("code"),
                (String) m.get("category"),
                Severity.valueOf((String) m.get("severity")),
                conditions,
                (String) m.get("message"),
                (String) m.get("rationale"));
    }

    private static LineOfBusiness defaultLineFor(String filename) {
        if (filename == null) {
            return null;
        }
        String base = filename.toLowerCase().replace(".yml", "");
        if (base.equals("shared")) {
            return null;
        }
        return lineOf(base);
    }

    private static LineOfBusiness lineOf(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return LineOfBusiness.valueOf(raw.trim().toUpperCase().replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

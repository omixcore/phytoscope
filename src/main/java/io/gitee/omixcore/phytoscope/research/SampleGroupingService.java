package io.gitee.omixcore.phytoscope.research;

import io.gitee.omixcore.phytoscope.model.ExpressionProfile;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import java.util.*;

/** Reversible sample-to-group assignments and explicit summary statistics. */
public final class SampleGroupingService {
    public enum Statistic { MEAN, MEDIAN }
    public record GroupSummary(String group, int n, double value) {}
    private final LinkedHashMap<String, String> assignments = new LinkedHashMap<>();

    public void assign(String sample, String group) {
        if (sample == null || sample.isBlank() || group == null || group.isBlank())
            throw new IllegalArgumentException("Sample and group names are required");
        assignments.put(sample, group.trim());
    }
    public void assignAll(Map<String, String> mapping) { mapping.forEach(this::assign); }
    public Map<String, String> assignments() { return Collections.unmodifiableMap(new LinkedHashMap<>(assignments)); }
    public void clear() { assignments.clear(); }

    /** Summarizes only present measurements; missing values are excluded and zero is retained. */
    public List<GroupSummary> summarize(ExpressionDataService data, String annotationId, Statistic statistic) {
        Objects.requireNonNull(data); Objects.requireNonNull(statistic);
        ExpressionProfile profile = data.getProfileForAnnotation(annotationId);
        if (profile == null) return List.of();
        Map<String, List<Double>> values = new LinkedHashMap<>();
        for (String sample : data.getTissues()) {
            String group = assignments.getOrDefault(sample, sample);
            double v = profile.getTpm(sample);
            if (Double.isFinite(v)) values.computeIfAbsent(group, k -> new ArrayList<>()).add(v);
        }
        return values.entrySet().stream().map(e -> {
            List<Double> xs = e.getValue().stream().sorted().toList();
            double value = statistic == Statistic.MEAN ? xs.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN)
                    : xs.size() % 2 == 1 ? xs.get(xs.size()/2) : (xs.get(xs.size()/2-1)+xs.get(xs.size()/2))/2;
            return new GroupSummary(e.getKey(), xs.size(), value);
        }).toList();
    }
}

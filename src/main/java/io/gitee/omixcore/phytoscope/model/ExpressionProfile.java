package io.gitee.omixcore.phytoscope.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A profile in the input measurement unit; missing samples remain absent. */
public class ExpressionProfile {

    /** Gene identifier this profile belongs to (e.g. AT1G01010). */
    private final String geneId;

    /** Tissue name -> expression value, insertion-ordered. */
    private final Map<String, TissueExpression> byTissue = new LinkedHashMap<>();

    public ExpressionProfile(String geneId) {
        this.geneId = geneId;
    }

    public String getGeneId() {
        return geneId;
    }

    public void put(String tissue, double tpm, int sampleCount) {
        byTissue.put(tissue, new TissueExpression(tissue, tpm, sampleCount));
    }

    /** All measurements in the canonical tissue order. */
    public List<TissueExpression> getValues() {
        return new ArrayList<>(byTissue.values());
    }

    public double getTpm(String tissue) {
        TissueExpression e = byTissue.get(tissue);
        return e == null ? Double.NaN : e.tpm();
    }

    public boolean isEmpty() {
        return byTissue.isEmpty();
    }

    /** Highest TPM across all tissues (0 if empty). */
    public double maxTpm() {
        return byTissue.values().stream()
                .mapToDouble(TissueExpression::tpm)
                .max()
                .orElse(0.0);
    }

    /** Tissue with the highest expression, or null when empty. */
    public TissueExpression topTissue() {
        return byTissue.values().stream()
                .max(Comparator.comparingDouble(TissueExpression::tpm))
                .orElse(null);
    }

    /** Sum of TPM across tissues, used as a rough "overall activity" score. */
    public double sumTpm() {
        return byTissue.values().stream()
                .mapToDouble(TissueExpression::tpm)
                .sum();
    }
}

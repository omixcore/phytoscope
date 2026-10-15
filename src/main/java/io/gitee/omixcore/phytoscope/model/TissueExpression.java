package io.gitee.omixcore.phytoscope.model;

/**
 * A single tissue's expression measurement for one gene.
 *
 * @param tissue     tissue / organ name, e.g. "Root"
 * @param tpm        expression level in TPM (Transcripts Per Million)
 * @param sampleCount number of biological samples aggregated, 0 if unknown
 */
public record TissueExpression(String tissue, double tpm, int sampleCount) {

    /** Expression level expressed in log2(TPM + 1), used for colour scaling. */
    public double log2Tpm() {
        return Math.log(tpm + 1.0) / Math.log(2.0);
    }

    @Override
    public String toString() {
        return String.format("%s: %.2f TPM", tissue, tpm);
    }
}

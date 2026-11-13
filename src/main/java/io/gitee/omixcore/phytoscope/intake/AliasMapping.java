package io.gitee.omixcore.phytoscope.intake;

/**
 * A confirmed sequence-name alias mapping.
 *
 * <p>Schema-name boundary (v1): this resolves only <em>sequence-name</em>
 * differences, never coordinate-system conversion. {@code from} is the name as it
 * appears in the annotation (GFF3 side); {@code to} is the FASTA sequence id it
 * maps to; {@code confirmedBy} records how the mapping was established.</p>
 *
 * @param from        annotation-side sequence name (e.g. {@code 1})
 * @param to          FASTA sequence id it resolves to (e.g. {@code Chr1})
 * @param confirmedBy {@code "auto-normalize"} (discovered via normalisation) or
 *                    {@code "user"} (declared manually in the wizard)
 */
public record AliasMapping(String from, String to, String confirmedBy) {
}

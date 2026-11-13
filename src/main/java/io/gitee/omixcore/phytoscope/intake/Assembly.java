package io.gitee.omixcore.phytoscope.intake;

import java.util.List;

/**
 * Reference assembly identity recorded in manifest v2.
 *
 * <p>Carries the assembly id, its source label and the list of FASTA sequence
 * names so a restored project can confirm that the genome still matches.</p>
 *
 * @param id         assembly identifier (e.g. {@code TAIR10} or a user label)
 * @param source     where the assembly came from (file name or user-imported)
 * @param sequences  FASTA sequence names in file order
 */
public record Assembly(String id, String source, List<String> sequences) {
    public Assembly {
        sequences = sequences == null ? List.of() : List.copyOf(sequences);
    }
}

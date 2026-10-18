package io.gitee.omixcore.phytoscope.research;

import io.gitee.omixcore.phytoscope.service.GeneSequenceService;
import java.util.*;

/** Transparent sequence metrics and annotation checks; findings are flags, not biological diagnoses. */
public final class SequenceQualityService {
    public record Report(String sequenceId, int length, double gcPercent, int ambiguousBases,
                         boolean lengthMultipleOfThree, List<String> flags) {
        public Report { flags = List.copyOf(flags); }
    }
    public Report assess(String id, String sequence, boolean coding) {
        Objects.requireNonNull(sequence);
        int gc = 0, ambiguous = 0;
        for (char c : sequence.toUpperCase(Locale.ROOT).toCharArray()) {
            if (c == 'G' || c == 'C') gc++;
            if ("ACGTU".indexOf(c) < 0) ambiguous++;
        }
        boolean frame = sequence.length() % 3 == 0;
        List<String> flags = new ArrayList<>();
        if (sequence.isEmpty()) flags.add("empty sequence");
        if (ambiguous > 0) flags.add(ambiguous + " ambiguous/non-ACGT bases");
        if (coding && !frame) flags.add("CDS length is not divisible by three");
        String s = sequence.toUpperCase(Locale.ROOT).replace('U', 'T');
        if (coding && s.length() >= 3 && !s.startsWith("ATG")) flags.add("CDS does not start with ATG (may be partial or use a non-standard code)");
        if (coding && s.length() >= 3 && !Set.of("TAA", "TAG", "TGA").contains(s.substring(s.length()-3))) flags.add("CDS has no terminal stop codon (may be partial)");
        return new Report(id, sequence.length(), sequence.isEmpty() ? Double.NaN : 100.0 * gc / sequence.length(), ambiguous, frame, flags);
    }
    public Report assessPromoter(String id, String seq) { return assess(id, seq, false); }
    public Report assessCds(String id, String seq) { return assess(id, seq, true); }
}

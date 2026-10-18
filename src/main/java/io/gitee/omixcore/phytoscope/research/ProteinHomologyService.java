package io.gitee.omixcore.phytoscope.research;

import java.util.*;

/** Pairwise protein similarity ranking with explicit reciprocal-best-hit reporting. */
public final class ProteinHomologyService {
    public record Protein(String species, String id, String sequence) {
        public Protein {
            if (species == null || species.isBlank() || id == null || id.isBlank())
                throw new IllegalArgumentException("Protein species and identifier are required");
        }
    }
    public record Hit(String querySpecies, String queryId, String targetSpecies, String targetId,
                      double identity, int alignedLength, double queryCoverage, double targetCoverage,
                      boolean reciprocalBestHit) {}

    /** Local alignment (Smith-Waterman); identities/coverage are descriptive, not an orthology proof. */
    public List<Hit> compare(List<Protein> queries, List<Protein> targets, double minimumIdentity) {
        if (minimumIdentity < 0 || minimumIdentity > 100) throw new IllegalArgumentException("Identity cutoff must be 0–100");
        requireUniqueKeys(queries); requireUniqueKeys(targets);
        Map<String, Protein> qBest = new HashMap<>(), tBest = new HashMap<>();
        Map<String, Integer> qScores = new HashMap<>(), tScores = new HashMap<>();
        Map<String, double[]> score = new HashMap<>();
        for (Protein q : queries) for (Protein t : targets) {
            String qSequence = clean(q.sequence()), tSequence = clean(t.sequence());
            Alignment a = align(qSequence, tSequence);
            double identity = a.length == 0 ? 0 : 100.0 * a.matches / a.length;
            double qc = qSequence.isEmpty() ? 0 : (double)a.qSpan / qSequence.length();
            double tc = tSequence.isEmpty() ? 0 : (double)a.tSpan / tSequence.length();
            String queryKey = key(q), targetKey = key(t);
            score.put(pairKey(queryKey, targetKey), new double[]{identity,a.length,qc,tc,a.score});
            choose(qBest, qScores, queryKey, t, a.score);
            choose(tBest, tScores, targetKey, q, a.score);
        }
        List<Hit> hits = new ArrayList<>();
        for (Protein q : queries) for (Protein t : targets) {
            String queryKey = key(q), targetKey = key(t);
            double[] x = score.get(pairKey(queryKey, targetKey));
            if (x[0] >= minimumIdentity && x[1] > 0) hits.add(new Hit(q.species(),q.id(),t.species(),t.id(),x[0],(int)x[1],x[2],x[3],
                    Objects.equals(qBest.get(queryKey) == null ? null : key(qBest.get(queryKey)), targetKey)
                    && Objects.equals(tBest.get(targetKey) == null ? null : key(tBest.get(targetKey)), queryKey)));
        }
        hits.sort(Comparator.comparing(Hit::reciprocalBestHit).reversed().thenComparing(Hit::identity, Comparator.reverseOrder()));
        return List.copyOf(hits);
    }
    private record Alignment(int matches, int length, int qSpan, int tSpan, int score) {}
    private static String clean(String s) { return s == null ? "" : s.toUpperCase(Locale.ROOT).replaceAll("[^A-Z*]", ""); }
    private static String key(Protein p) { return p.species() + "\0" + p.id(); }
    private static String pairKey(String q, String t) { return q + "\1" + t; }
    private static void requireUniqueKeys(List<Protein> proteins) {
        Set<String> keys = new HashSet<>();
        for (Protein protein : proteins) if (!keys.add(key(protein)))
            throw new IllegalArgumentException("Duplicate protein identifier within species: " + protein.species() + ":" + protein.id());
    }
    private static void choose(Map<String,Protein> best, Map<String,Integer> scores, String id, Protein candidate, int score) {
        if (!scores.containsKey(id) || score > scores.get(id)) { best.put(id,candidate); scores.put(id,score); }
    }
    private static Alignment align(String q, String t) {
        int n=q.length(), m=t.length();
        if (n == 0 || m == 0) return new Alignment(0, 0, 0, 0, 0);
        if ((n + 1L) * (m + 1L) > 4_000_000L) throw new IllegalArgumentException("Protein pair exceeds 4 million alignment cells");
        int[][] d=new int[n+1][m+1]; int best=0, bi=0,bj=0;
        for(int i=1;i<=n;i++) for(int j=1;j<=m;j++) { d[i][j]=Math.max(0,Math.max(d[i-1][j-1]+(q.charAt(i-1)==t.charAt(j-1)?2:-1),Math.max(d[i-1][j]-2,d[i][j-1]-2))); if(d[i][j]>best){best=d[i][j];bi=i;bj=j;} }
        int i=bi,j=bj,matches=0,len=0,qs=0,ts=0;
        while(i>0&&j>0&&d[i][j]>0){int v=d[i][j]; if(v==d[i-1][j-1]+(q.charAt(i-1)==t.charAt(j-1)?2:-1)){if(q.charAt(i-1)==t.charAt(j-1))matches++;i--;j--;qs++;ts++;} else if(v==d[i-1][j]-2){i--;qs++;} else {j--;ts++;} len++;}
        return new Alignment(matches,len,qs,ts,best);
    }
}

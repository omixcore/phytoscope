package io.gitee.omixcore.phytoscope.research;

import java.util.*;

/** Pairwise protein similarity ranking with explicit reciprocal-best-hit reporting. */
public final class ProteinHomologyService {
    public record Protein(String species, String id, String sequence) {}
    public record Hit(String querySpecies, String queryId, String targetSpecies, String targetId,
                      double identity, int alignedLength, double queryCoverage, double targetCoverage,
                      boolean reciprocalBestHit) {}

    /** Local alignment (Smith-Waterman); identities/coverage are descriptive, not an orthology proof. */
    public List<Hit> compare(List<Protein> queries, List<Protein> targets, double minimumIdentity) {
        if (minimumIdentity < 0 || minimumIdentity > 100) throw new IllegalArgumentException("Identity cutoff must be 0–100");
        Map<String, Protein> qBest = new HashMap<>(), tBest = new HashMap<>();
        Map<String, Integer> qScores = new HashMap<>(), tScores = new HashMap<>();
        Map<String, double[]> score = new HashMap<>();
        for (Protein q : queries) for (Protein t : targets) {
            Alignment a = align(clean(q.sequence()), clean(t.sequence()));
            double identity = a.length == 0 ? 0 : 100.0 * a.matches / a.length;
            double qc = q.sequence().isEmpty() ? 0 : (double)a.qSpan / q.sequence().length();
            double tc = t.sequence().isEmpty() ? 0 : (double)a.tSpan / t.sequence().length();
            String key = q.id()+"\0"+t.id(); score.put(key, new double[]{identity,a.length,qc,tc,a.score});
            choose(qBest, qScores, q.id(), t, a.score);
            choose(tBest, tScores, t.id(), q, a.score);
        }
        List<Hit> hits = new ArrayList<>();
        for (Protein q : queries) for (Protein t : targets) {
            double[] x = score.get(q.id()+"\0"+t.id());
            if (x[0] >= minimumIdentity && x[1] > 0) hits.add(new Hit(q.species(),q.id(),t.species(),t.id(),x[0],(int)x[1],x[2],x[3],
                    Objects.equals(qBest.get(q.id()) == null ? null : qBest.get(q.id()).id(), t.id())
                    && Objects.equals(tBest.get(t.id()) == null ? null : tBest.get(t.id()).id(), q.id())));
        }
        hits.sort(Comparator.comparing(Hit::reciprocalBestHit).reversed().thenComparing(Hit::identity, Comparator.reverseOrder()));
        return List.copyOf(hits);
    }
    private record Alignment(int matches, int length, int qSpan, int tSpan, int score) {}
    private static String clean(String s) { return s == null ? "" : s.toUpperCase(Locale.ROOT).replaceAll("[^A-Z*]", ""); }
    private static void choose(Map<String,Protein> best, Map<String,Integer> scores, String id, Protein candidate, int score) {
        if (!scores.containsKey(id) || score > scores.get(id)) { best.put(id,candidate); scores.put(id,score); }
    }
    private static Alignment align(String q, String t) {
        int n=q.length(), m=t.length(); if ((long)n*m > 4_000_000L) throw new IllegalArgumentException("Protein pair exceeds 4 million alignment cells");
        int[][] d=new int[n+1][m+1]; int best=0, bi=0,bj=0;
        for(int i=1;i<=n;i++) for(int j=1;j<=m;j++) { d[i][j]=Math.max(0,Math.max(d[i-1][j-1]+(q.charAt(i-1)==t.charAt(j-1)?2:-1),Math.max(d[i-1][j]-2,d[i][j-1]-2))); if(d[i][j]>best){best=d[i][j];bi=i;bj=j;} }
        int i=bi,j=bj,matches=0,len=0,qs=0,ts=0;
        while(i>0&&j>0&&d[i][j]>0){int v=d[i][j]; if(v==d[i-1][j-1]+(q.charAt(i-1)==t.charAt(j-1)?2:-1)){if(q.charAt(i-1)==t.charAt(j-1))matches++;i--;j--;qs++;ts++;} else if(v==d[i-1][j]-2){i--;qs++;} else {j--;ts++;} len++;}
        return new Alignment(matches,len,qs,ts,best);
    }
}

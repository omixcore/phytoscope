#!/usr/bin/env python3
"""Compare coverage-filtered PhytoScope family hits with an independent BLASTP run."""
from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import tempfile
import time
from pathlib import Path


def write_fasta(path: Path, records):
    with path.open("wt", encoding="ascii") as handle:
        for record in records:
            handle.write(f">{record['gene_id']}\n")
            sequence = record["sequence"].replace("*", "")
            for offset in range(0, len(sequence), 60):
                handle.write(sequence[offset:offset + 60] + "\n")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", type=Path, required=True,
                        help="research-module-gold-set.json emitted by ResearchModuleGoldSetTest")
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()

    blastp = shutil.which("blastp")
    if not blastp:
        raise SystemExit("blastp is required; install ncbi-blast+ before running this comparison")
    report = json.loads(args.report.read_text(encoding="utf-8"))
    similarity = report["protein_similarity"]
    records = similarity["reference_proteins"]
    queries = [record for record in records if record["family"] == "RBCS"]
    if len(queries) != 4 or len(records) != 6:
        raise SystemExit(f"Expected 4 RBCS queries and 2 unrelated controls; got {len(queries)} and {len(records)}")
    if len({record["gene_id"] for record in records}) != len(records):
        raise SystemExit("Duplicate gene IDs in the protein reference set")

    positive_pairs = {
        (q["gene_id"], t["gene_id"])
        for q in queries for t in records
        if t["family"] == "RBCS" and q["gene_id"] != t["gene_id"]
    }
    negative_pairs = {(q["gene_id"], t["gene_id"]) for q in queries for t in records if t["family"] != "RBCS"}
    filtered_search = similarity["filtered_search"]
    phytoscope_pairs = {
        (hit["query_gene"], hit["target_gene"])
        for hit in filtered_search["hits"]
        if hit["query_gene"] != hit["target_gene"]
    }
    self_pairs_excluded = len(queries)

    with tempfile.TemporaryDirectory(prefix="phytoscope-blastp-") as temporary:
        temp = Path(temporary)
        query_fasta = temp / "rbcS-query.fa"
        subject_fasta = temp / "six-protein-subject.fa"
        raw_output = temp / "blastp.tsv"
        write_fasta(query_fasta, queries)
        write_fasta(subject_fasta, records)
        version_result = subprocess.run([blastp, "-version"], check=True, capture_output=True, text=True)
        version = (version_result.stdout or version_result.stderr).splitlines()[0].strip()
        command = [
            blastp, "-query", str(query_fasta), "-subject", str(subject_fasta),
            "-evalue", "1e-5", "-max_hsps", "1",
            "-outfmt", "6 qseqid sseqid pident length qcovs sstart send slen evalue bitscore",
            "-out", str(raw_output),
        ]
        started = time.perf_counter()
        subprocess.run(command, check=True, capture_output=True, text=True)
        elapsed = time.perf_counter() - started
        blast_rows = []
        blast_pairs = set()
        if raw_output.exists():
            for line in raw_output.read_text(encoding="utf-8").splitlines():
                fields = line.split("\t")
                if len(fields) != 10:
                    raise SystemExit(f"Unexpected BLASTP output row: {line}")
                qid, sid = fields[0], fields[1]
                identity = float(fields[2])
                aligned_length = int(fields[3])
                query_coverage = float(fields[4])
                subject_start, subject_end, subject_length = int(fields[5]), int(fields[6]), int(fields[7])
                subject_coverage = 100.0 * (abs(subject_end - subject_start) + 1) / subject_length
                evalue = float(fields[8])
                # Apply the same acceptance thresholds as PhytoScope after BLASTP
                # has generated the local alignment; only the alignment algorithm differs.
                passes_shared_filter = (
                    identity >= 30.0
                    and aligned_length >= 20
                    and query_coverage >= 50.0
                    and subject_coverage >= 50.0
                    and evalue <= 1e-5
                )
                if passes_shared_filter:
                    blast_pairs.add((qid, sid))
                blast_rows.append({
                    "query_gene": qid,
                    "target_gene": sid,
                    "is_self_hit": qid == sid,
                    "identity_percent": identity,
                    "aligned_length": aligned_length,
                    "query_coverage_percent": query_coverage,
                    "subject_coverage_percent": subject_coverage,
                    "evalue": fields[8],
                    "bit_score": float(fields[9]),
                    "passes_shared_filter": passes_shared_filter,
                })

    missing_blast_positives = sorted(positive_pairs - blast_pairs)
    blast_negative_hits = sorted(negative_pairs & blast_pairs)
    missing_phyto_positives = sorted(positive_pairs - phytoscope_pairs)
    phyto_negative_hits = sorted(negative_pairs & phytoscope_pairs)
    pairwise_agreement = sum(
        ((pair in blast_pairs) == (pair in phytoscope_pairs))
        for pair in positive_pairs | negative_pairs
    )
    total_pairs = len(positive_pairs) + len(negative_pairs)
    result = {
        "external_tool": "NCBI BLASTP",
        "version": version,
        "shared_classification_thresholds": {
            "minimum_identity_percent": 30.0,
            "minimum_aligned_length_aa": 20,
            "minimum_query_coverage_percent": 50.0,
            "minimum_subject_coverage_percent": 50.0,
            "maximum_evalue": "1e-5",
            "maximum_hsps_per_pair": 1,
        },
        "reference_set": "4 annotated Arabidopsis RBCS-family proteins plus NAC001 and histone H3.1 negative controls",
        "self_pairs_excluded_from_classification": self_pairs_excluded,
        "positive_family_pairs_expected": len(positive_pairs),
        "positive_pairs_reported_by_blastp": len(positive_pairs & blast_pairs),
        "negative_control_pairs_expected": len(negative_pairs),
        "negative_pairs_reported_by_blastp": len(blast_negative_hits),
        "positive_pairs_reported_by_phytoscope": len(positive_pairs & phytoscope_pairs),
        "negative_pairs_reported_by_phytoscope": len(phyto_negative_hits),
        "pairwise_classification_agreement": pairwise_agreement / total_pairs if total_pairs else 1.0,
        "pairwise_classification_count": total_pairs,
        "blastp_elapsed_seconds": round(elapsed, 6),
        "missing_blastp_positive_pairs": missing_blast_positives,
        "blastp_negative_control_hits": blast_negative_hits,
        "missing_phytoscope_positive_pairs": missing_phyto_positives,
        "phytoscope_negative_control_hits": phyto_negative_hits,
        "blastp_records": blast_rows,
        "scope_limit": "Small same-species family discrimination benchmark; this is not an orthology benchmark or a general sensitivity/specificity estimate.",
    }
    # A second, independently sourced panel tests family similarity across four
    # plant species. It is deliberately reported separately from the Arabidopsis
    # panel and must not be interpreted as an orthology benchmark.
    cross = report.get("cross_species_rbcS_family")
    if not isinstance(cross, dict):
        raise SystemExit("Missing cross_species_rbcS_family section in research-module report")
    cross_records = cross.get("reference_proteins", [])
    cross_queries = [record for record in cross_records if record["family"] == "RBCS"]
    cross_species = {record.get("species") for record in cross_queries}
    if len(cross_queries) != 4 or len(cross_species) != 4 or len(cross_records) != 6:
        raise SystemExit(
            "Expected 4 cross-species RBCS proteins from 4 species plus 2 controls; "
            f"got {len(cross_queries)} RBCS records, {len(cross_species)} species, {len(cross_records)} total records"
        )
    if len({record["gene_id"] for record in cross_records}) != len(cross_records):
        raise SystemExit("Duplicate gene IDs in the cross-species protein reference set")
    cross_positive_pairs = {
        (q["gene_id"], t["gene_id"])
        for q in cross_queries for t in cross_queries
        if q["gene_id"] != t["gene_id"]
    }
    cross_negative_pairs = {
        (q["gene_id"], t["gene_id"])
        for q in cross_queries for t in cross_records
        if t["family"] != "RBCS"
    }
    cross_phyto_pairs = {
        (hit["query_gene"], hit["target_gene"])
        for hit in cross["filtered_search"]["hits"]
        if hit["query_gene"] != hit["target_gene"]
    }
    with tempfile.TemporaryDirectory(prefix="phytoscope-cross-species-blastp-") as temporary:
        temp = Path(temporary)
        query_fasta = temp / "four-species-rbcS-query.fa"
        subject_fasta = temp / "four-species-six-protein-subject.fa"
        raw_output = temp / "cross-species-blastp.tsv"
        write_fasta(query_fasta, cross_queries)
        write_fasta(subject_fasta, cross_records)
        command = [
            blastp, "-query", str(query_fasta), "-subject", str(subject_fasta),
            "-evalue", "1e-5", "-max_hsps", "1",
            "-outfmt", "6 qseqid sseqid pident length qcovs sstart send slen evalue bitscore",
            "-out", str(raw_output),
        ]
        started = time.perf_counter()
        subprocess.run(command, check=True, capture_output=True, text=True)
        cross_elapsed = time.perf_counter() - started
        cross_blast_rows = []
        cross_blast_pairs = set()
        if raw_output.exists():
            for line in raw_output.read_text(encoding="utf-8").splitlines():
                fields = line.split("\\t")
                if len(fields) != 10:
                    raise SystemExit(f"Unexpected cross-species BLASTP output row: {line}")
                qid, sid = fields[0], fields[1]
                identity = float(fields[2])
                aligned_length = int(fields[3])
                query_coverage = float(fields[4])
                subject_start, subject_end, subject_length = int(fields[5]), int(fields[6]), int(fields[7])
                subject_coverage = 100.0 * (abs(subject_end - subject_start) + 1) / subject_length
                evalue = float(fields[8])
                passes_shared_filter = (
                    identity >= 30.0
                    and aligned_length >= 20
                    and query_coverage >= 50.0
                    and subject_coverage >= 50.0
                    and evalue <= 1e-5
                )
                if passes_shared_filter:
                    cross_blast_pairs.add((qid, sid))
                cross_blast_rows.append({
                    "query_gene": qid,
                    "target_gene": sid,
                    "is_self_hit": qid == sid,
                    "identity_percent": identity,
                    "aligned_length": aligned_length,
                    "query_coverage_percent": query_coverage,
                    "subject_coverage_percent": subject_coverage,
                    "evalue": fields[8],
                    "bit_score": float(fields[9]),
                    "passes_shared_filter": passes_shared_filter,
                })
    cross_missing_blast_positives = sorted(cross_positive_pairs - cross_blast_pairs)
    cross_blast_negative_hits = sorted(cross_negative_pairs & cross_blast_pairs)
    cross_missing_phyto_positives = sorted(cross_positive_pairs - cross_phyto_pairs)
    cross_phyto_negative_hits = sorted(cross_negative_pairs & cross_phyto_pairs)
    cross_pairwise_agreement = sum(
        ((pair in cross_blast_pairs) == (pair in cross_phyto_pairs))
        for pair in cross_positive_pairs | cross_negative_pairs
    )
    cross_total_pairs = len(cross_positive_pairs) + len(cross_negative_pairs)
    result["cross_species_comparison"] = {
        "external_tool": "NCBI BLASTP",
        "version": version,
        "reference_set": "Mature RBCS family sequences from Arabidopsis, rice, maize and soybean plus two Arabidopsis unrelated-family controls",
        "species_count": len(cross_species),
        "self_pairs_excluded_from_classification": len(cross_queries),
        "positive_family_pairs_expected": len(cross_positive_pairs),
        "positive_pairs_reported_by_blastp": len(cross_positive_pairs & cross_blast_pairs),
        "negative_control_pairs_expected": len(cross_negative_pairs),
        "negative_pairs_reported_by_blastp": len(cross_blast_negative_hits),
        "positive_pairs_reported_by_phytoscope": len(cross_positive_pairs & cross_phyto_pairs),
        "negative_pairs_reported_by_phytoscope": len(cross_phyto_negative_hits),
        "pairwise_classification_agreement": cross_pairwise_agreement / cross_total_pairs if cross_total_pairs else 1.0,
        "pairwise_classification_count": cross_total_pairs,
        "phytoscope_elapsed_ms": cross.get("phytoscope_elapsed_ms"),
        "blastp_elapsed_seconds": round(cross_elapsed, 6),
        "missing_blastp_positive_pairs": cross_missing_blast_positives,
        "blastp_negative_control_hits": cross_blast_negative_hits,
        "missing_phytoscope_positive_pairs": cross_missing_phyto_positives,
        "phytoscope_negative_control_hits": cross_phyto_negative_hits,
        "blastp_records": cross_blast_rows,
        "shared_classification_thresholds": result["shared_classification_thresholds"],
        "scope_limit": "Four-species, single-family similarity smoke benchmark; no one-to-one orthology, proteome-wide accuracy or speed-superiority claim.",
    }
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({
        "same_species_comparison": {k: result[k] for k in (
            "external_tool", "version", "self_pairs_excluded_from_classification",
            "positive_family_pairs_expected", "positive_pairs_reported_by_blastp", "negative_control_pairs_expected",
            "negative_pairs_reported_by_blastp", "positive_pairs_reported_by_phytoscope",
            "negative_pairs_reported_by_phytoscope", "pairwise_classification_agreement")},
        "cross_species_comparison": {k: result["cross_species_comparison"][k] for k in (
            "species_count", "positive_family_pairs_expected", "positive_pairs_reported_by_blastp",
            "negative_control_pairs_expected", "negative_pairs_reported_by_blastp",
            "positive_pairs_reported_by_phytoscope", "negative_pairs_reported_by_phytoscope",
            "pairwise_classification_agreement", "pairwise_classification_count",
            "phytoscope_elapsed_ms", "blastp_elapsed_seconds")},
    }, indent=2))
    cross_result = result["cross_species_comparison"]
    if (missing_blast_positives or blast_negative_hits or missing_phyto_positives or phyto_negative_hits
            or cross_missing_blast_positives or cross_blast_negative_hits
            or cross_missing_phyto_positives or cross_phyto_negative_hits
            or cross_pairwise_agreement != cross_total_pairs):
        print(json.dumps({
            "same_species": {
                "missing_blastp_positive_pairs": missing_blast_positives,
                "blastp_negative_control_hits": blast_negative_hits,
                "missing_phytoscope_positive_pairs": missing_phyto_positives,
                "phytoscope_negative_control_hits": phyto_negative_hits,
            },
            "cross_species": {
                "missing_blastp_positive_pairs": cross_missing_blast_positives,
                "blastp_negative_control_hits": cross_blast_negative_hits,
                "missing_phytoscope_positive_pairs": cross_missing_phyto_positives,
                "phytoscope_negative_control_hits": cross_phyto_negative_hits,
                "pairwise_classification_agreement": cross_pairwise_agreement / cross_total_pairs if cross_total_pairs else 1.0,
            },
        }, indent=2))
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

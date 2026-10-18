#!/usr/bin/env python3
"""Summarize PastDB event PSI by mapped original study for frozen candidates.

This joins exact candidate deletion blocks to PastDB exon events, then maps
condition-level PSI summaries to the SRA runs and GEO source labels in PastDB's
sample-information table. It does not treat aggregated condition PSI as raw
replicate evidence or validate complete transcript chains.
"""
from __future__ import annotations

import argparse
import csv
import gzip
import hashlib
import json
import re
from collections import defaultdict
from pathlib import Path


def read_tsv(path: Path):
    with gzip.open(path, "rt", encoding="utf-8", errors="replace", newline="") as handle:
        yield from csv.DictReader(handle, delimiter="\t")


def sha256_file(path: Path):
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def load_candidates(path: Path):
    rows = []
    with path.open(encoding="utf-8-sig", newline="") as handle:
        for row in csv.DictReader(handle, delimiter="\t"):
            row["blocks"] = json.loads(row["deletion_genomic_blocks"])
            rows.append(row)
    return rows


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--audit-dir", type=Path, required=True,
                    help="Directory containing PastDB downloads from audit_public_splicing_evidence.py")
    ap.add_argument("--candidates", type=Path,
                    default=Path("examples/arabidopsis-isoform-validation/candidates.tsv"))
    ap.add_argument("--out", type=Path, required=True)
    args = ap.parse_args()

    candidates = load_candidates(args.candidates)
    by_gene = defaultdict(list)
    for candidate in candidates:
        by_gene[candidate["gene"].upper()].append(candidate)

    event_to_candidates = defaultdict(list)
    event_coords = {}
    event_gene_ids = {
        row.get("EventID", ""): row.get("GeneID", "").upper()
        for row in read_tsv(args.audit_dir / "pastdb_event_gene_map.gz")
    }
    for row in read_tsv(args.audit_dir / "pastdb_event_information.gz"):
        event_id = row.get("EVENT", "")
        m = re.fullmatch(r"chr([^:]+):(\d+)-(\d+)", row.get("COORD_o", "").lower())
        if not m:
            continue
        chromosome, start, end = m.group(1), int(m.group(2)), int(m.group(3))
        # EVENT_INFO.GENE is a display symbol (for example, PIF6), whereas
        # frozen candidate records use stable TAIR locus IDs. Join by the
        # resource's explicit EventID -> GeneID mapping.
        gene = event_gene_ids.get(event_id, "")
        for candidate in by_gene.get(gene, []):
            for block_start, block_end in candidate["blocks"]:
                if (chromosome, start, end) == (
                    candidate["chromosome"].lower().removeprefix("chr"),
                    int(block_start), int(block_end),
                ):
                    event_to_candidates[event_id].append(candidate)
                    event_coords[event_id] = row

    source_by_group = defaultdict(list)
    for sample in read_tsv(args.audit_dir / "pastdb_sample_information.gz"):
        source_by_group[sample.get("Sample_ID", "")].append(sample)

    event_rows = {}
    condition_headers = []
    main_psi = args.audit_dir / "pastdb_main_psi.gz"
    with gzip.open(main_psi, "rt", encoding="utf-8", errors="replace", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        condition_headers = [
            h for h in (reader.fieldnames or [])
            if h not in {"GENE", "EVENT", "COORD", "LENGTH", "FullCO", "COMPLEX"}
            and not h.endswith("-Q")
        ]
        wanted = set(event_to_candidates)
        for row in reader:
            if row.get("EVENT") in wanted:
                event_rows[row["EVENT"]] = row

    details = []
    summaries = []
    for event_id in sorted(event_to_candidates):
        event = event_rows.get(event_id)
        info = event_coords[event_id]
        candidates_for_event = event_to_candidates[event_id]
        study_values = defaultdict(list)
        no_source_groups = 0
        condition_count = 0
        run_ids_all = set()
        for group in condition_headers:
            psi = (event or {}).get(group, "")
            if not psi or psi.upper() in {"NA", "NAN"}:
                continue
            try:
                psi_value = float(psi)
            except ValueError:
                continue
            condition_count += 1
            samples = source_by_group.get(group, [])
            source_ids = sorted({s.get("Source", "").strip() for s in samples if s.get("Source", "").strip()})
            run_ids = sorted({s.get("SRA_ID", "").strip() for s in samples if s.get("SRA_ID", "").strip()})
            run_ids_all.update(run_ids)
            study = ";".join(source_ids) if source_ids else "source_unassigned"
            if study == "source_unassigned":
                no_source_groups += 1
            study_values[study].append(psi_value)
            details.append({
                "event_id": event_id,
                "gene": info.get("GENE", ""),
                "event_type": info.get("COMPLEX", ""),
                "event_coordinate": info.get("COORD_o", ""),
                "condition_group": group,
                "study_source": study,
                "study_group_is_explicit": study != "source_unassigned",
                "sra_run_count": len(run_ids),
                "sra_run_accessions": ";".join(run_ids),
                "psi_inclusion_percent": psi_value,
                "quality_counts_raw": (event or {}).get(group + "-Q", ""),
            })

        unique_studies = sorted(k for k in study_values if k != "source_unassigned")
        # For each original GEO source, preserve the observed condition range;
        # do not pool PSI values across source groups or infer significance.
        per_study_ranges = {
            study: {"condition_group_count": len(values), "min_psi": min(values), "max_psi": max(values)}
            for study, values in sorted(study_values.items()) if study != "source_unassigned"
        }
        for candidate in candidates_for_event:
            full_block_match = len(candidate["blocks"]) == 1
            summaries.append({
                "candidate_id": candidate["candidate_id"],
                "gene": candidate["gene"],
                "event_id": event_id,
                "event_coordinate": info.get("COORD_o", ""),
                "event_type": info.get("COMPLEX", ""),
                "candidate_deletion_block_count": len(candidate["blocks"]),
                "matched_block_covers_entire_candidate_deletion": full_block_match,
                "condition_groups_with_psi": condition_count,
                "unique_explicit_GEO_study_groups": len(unique_studies),
                "GEO_study_groups": ";".join(unique_studies),
                "mapped_sra_run_count": len(run_ids_all),
                "condition_groups_without_source_mapping": no_source_groups,
                "per_study_condition_psi_ranges_json": json.dumps(per_study_ranges, sort_keys=True),
                "interpretation": "coordinate-matched event evidence only; PSI is condition-aggregated and does not validate the complete candidate transcript chain",
            })

    args.out.mkdir(parents=True, exist_ok=True)
    detail_fields = ["event_id", "gene", "event_type", "event_coordinate", "condition_group",
                     "study_source", "study_group_is_explicit", "sra_run_count", "sra_run_accessions",
                     "psi_inclusion_percent", "quality_counts_raw"]
    summary_fields = ["candidate_id", "gene", "event_id", "event_coordinate", "event_type",
                      "candidate_deletion_block_count", "matched_block_covers_entire_candidate_deletion",
                      "condition_groups_with_psi", "unique_explicit_GEO_study_groups", "GEO_study_groups",
                      "mapped_sra_run_count", "condition_groups_without_source_mapping",
                      "per_study_condition_psi_ranges_json", "interpretation"]
    for name, fields, rows in (("pastdb-candidate-event-condition-study.tsv", detail_fields, details),
                               ("pastdb-candidate-study-summary.tsv", summary_fields, summaries)):
        with (args.out / name).open("w", encoding="utf-8", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=fields, delimiter="\t", extrasaction="ignore")
            writer.writeheader()
            writer.writerows(rows)

    report = {
        "analysis": "Exact candidate-deletion-block coordinate match to PastDB event, with condition PSI mapped through sample information to GEO source groups and SRA runs.",
        "event_count": len(event_to_candidates),
        "candidate_event_rows": len(summaries),
        "condition_observation_rows": len(details),
        "limitations": [
            "PastDB main PSI values are condition-group aggregates, not per-run replicate values.",
            "GEO source IDs are used only as source-study labels; groups without source labels are retained but not counted as independent studies.",
            "An exact exon/block coordinate match does not establish the complete candidate exon chain, translation, function, or novelty.",
            "No raw reads were realigned and no new statistical test was performed.",
        ],
        "input_sha256": {
            name: sha256_file(args.audit_dir / filename)
            for name, filename in {
                "event_information": "pastdb_event_information.gz",
                "event_gene_map": "pastdb_event_gene_map.gz",
                "main_psi": "pastdb_main_psi.gz",
                "sample_information": "pastdb_sample_information.gz",
            }.items()
        },
        "candidate_event_summaries": summaries,
    }
    (args.out / "pastdb-candidate-study-summary.json").write_text(
        json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps({"events": len(event_to_candidates), "candidate_event_rows": len(summaries),
                      "condition_observation_rows": len(details)}, indent=2))


if __name__ == "__main__":
    main()

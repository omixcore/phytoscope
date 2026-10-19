"""Compare exact locus extraction from a full public annotation in PhytoScope and gffutils.

This compares one reproducible scripted task, not GUI usability or a whole-browser ranking.
Both tools ingest the same complete annotation and are checked against source tuples.
"""
import argparse
import collections
import csv
import datetime
import gzip
import hashlib
import json
import platform
import statistics
import subprocess
import sys
from pathlib import Path
from urllib.parse import unquote

from validate_structure_inventory import GENE_TYPES, TX_TYPES, UTR_TYPES


def read_tuples(path, allowed_ids=None):
    result = collections.Counter()
    with path.open(encoding="utf-8", newline="") as stream:
        for row in csv.DictReader(stream, delimiter="\t"):
            if allowed_ids is not None and row["GeneID"] not in allowed_ids:
                continue
            result[(row["GeneID"], row["TranscriptID"], row["Sequence"], row["Strand"],
                    row["Feature"], int(row["Start"]), int(row["End"]))] += 1
    return result


def run(name, command, root):
    result = subprocess.run(command, cwd=root, text=True, capture_output=True)
    if result.returncode:
        raise RuntimeError(f"{name} failed ({result.returncode}): {' '.join(map(str, command))}\n"
                           f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}")
    return result


def source_locus_tuples(path, wanted_gene_ids):
    """Read only selected explicit GFF3 hierarchies; unrelated source gaps do not mask them."""
    opener = gzip.open if path.suffix == ".gz" else open

    def rows():
        with opener(path, "rt", encoding="utf-8") as stream:
            for line in stream:
                if line.startswith("##FASTA"):
                    break
                if not line.strip() or line.startswith("#"):
                    continue
                fields = line.rstrip("\n").split("\t")
                if len(fields) != 9:
                    raise ValueError(f"Malformed GFF3 row in {path}")
                attrs = {}
                for token in fields[8].split(";"):
                    if "=" in token:
                        key, value = token.split("=", 1)
                        attrs[key] = unquote(value)
                yield fields, attrs

    expected = collections.Counter()
    gene_to_id = {}
    for fields, attrs in rows():
        if fields[2].lower() in GENE_TYPES and attrs.get("ID") in wanted_gene_ids:
            gid = attrs["ID"]
            gene_to_id[gid] = gid
            expected[(gid, "", fields[0], fields[6], "gene", int(fields[3]), int(fields[4]))] += 1
    if not gene_to_id:
        raise ValueError(f"No matching source gene IDs: {sorted(wanted_gene_ids)}")

    tx_to_gene = {}
    for fields, attrs in rows():
        kind = fields[2].lower()
        if kind not in TX_TYPES:
            continue
        parents = [parent for parent in attrs.get("Parent", "").split(",") if parent]
        linked = [parent for parent in parents if parent in gene_to_id]
        if not linked:
            continue
        tid = attrs.get("ID")
        if not tid:
            raise ValueError(f"Source transcript lacks an ID for selected gene: {fields[8]}")
        tx_to_gene[tid] = linked
        for gid in linked:
            expected[(gid, tid, fields[0], fields[6], "transcript", int(fields[3]), int(fields[4]))] += 1

    for fields, attrs in rows():
        kind = fields[2].lower()
        if kind not in {"exon", "pseudogenic_exon", "cds"} | UTR_TYPES:
            continue
        normalized = "CDS" if kind == "cds" else "UTR" if kind in UTR_TYPES else "exon"
        for tid in (parent for parent in attrs.get("Parent", "").split(",") if parent):
            for gid in tx_to_gene.get(tid, []):
                expected[(gid, tid, fields[0], fields[6], normalized, int(fields[3]), int(fields[4]))] += 1
    return expected


def main():
    import gffutils

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--annotation", type=Path, required=True)
    parser.add_argument("--gene-ids", nargs="+", required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--trials", type=int, default=3)
    parser.add_argument("--java", default="java")
    args = parser.parse_args()
    if args.trials < 3:
        parser.error("--trials must be at least 3")
    if gffutils.__version__ != "0.13":
        raise RuntimeError(f"Expected gffutils 0.13; found {gffutils.__version__}")

    root = Path(__file__).resolve().parents[1]
    jar, source, out_root = args.jar.resolve(), args.annotation.resolve(), args.out.resolve()
    if not jar.is_file() or not source.is_file():
        raise FileNotFoundError(jar if not jar.is_file() else source)
    gene_ids = set(args.gene_ids)
    expected = source_locus_tuples(source, gene_ids)
    out_root.mkdir(parents=True, exist_ok=True)
    trials = []

    for trial in range(args.trials):
        trial_dir = out_root / f"trial-{trial + 1}"
        java_dir, baseline_dir = trial_dir / "phytoscope", trial_dir / "gffutils"
        java_dir.mkdir(parents=True, exist_ok=False)
        baseline_dir.mkdir(parents=True, exist_ok=False)
        commands = [
            ("phytoscope", [args.java, "-Xmx3g", "-jar", str(jar), "--benchmark",
                             "--annotation", str(source), "--query-ids", ",".join(sorted(gene_ids)),
                             "--out", str(java_dir)]),
            ("gffutils", [sys.executable, str(root / "tools/compare_gffutils.py"),
                           "--annotation", str(source), "--loci", *sorted(gene_ids),
                           "--out", str(baseline_dir)])
        ]
        if trial % 2:
            commands.reverse()
        for name, command in commands:
            run(name, command, root)

        java_full = java_dir / "parsed-structure.tsv"
        java_selected = java_dir / "selected-structure.tsv"
        with java_full.open(encoding="utf-8", newline="") as inp, java_selected.open("w", encoding="utf-8", newline="") as out:
            reader = csv.DictReader(inp, delimiter="\t")
            writer = csv.DictWriter(out, fieldnames=reader.fieldnames, delimiter="\t", lineterminator="\n")
            writer.writeheader()
            for row in reader:
                if row["GeneID"] in gene_ids:
                    writer.writerow(row)

        java_tuples = read_tuples(java_selected)
        baseline_tuples = read_tuples(baseline_dir / "parsed-structure.tsv")
        if java_tuples != baseline_tuples or java_tuples != expected:
            raise AssertionError(
                f"trial {trial + 1}: source/alternative mismatch; "
                f"PhytoScope missing={list((expected-java_tuples).items())[:3]} extra={list((java_tuples-expected).items())[:3]}; "
                f"gffutils missing={list((expected-baseline_tuples).items())[:3]} extra={list((baseline_tuples-expected).items())[:3]}"
            )
        java_perf = json.loads((java_dir / "performance.json").read_text())
        baseline_perf = json.loads((baseline_dir / "performance.json").read_text())
        trials.append({
            "trial": trial + 1,
            "execution_order": [name for name, _ in commands],
            "exact_tuple_agreement_with_each_other_and_source": True,
            "selected_tuple_count": expected.total(),
            "phytoscope": {
                "cold_full_import_ms": java_perf["import_ms"],
                "selected_gene_search_median_ms": java_perf["search_median_ms"],
                "selected_gene_search_p95_ms": java_perf["search_p95_ms"],
                "query_gene_ids": java_perf["search_query_ids"],
                "feature_count": java_perf["features"],
                "sequence_count": java_perf["sequences"],
                "max_heap_bytes": java_perf["max_heap_bytes"]
            },
            "gffutils": {
                "cold_full_index_build_ms": baseline_perf["index_build_ms"],
                "selected_gene_query_100_median_ms": baseline_perf["query_100_repetitions_median_ms"],
                "selected_gene_query_p95_ms": baseline_perf["query_p95_ms"],
                "version": baseline_perf["version"],
                "max_rss_platform_units": baseline_perf["max_rss_platform_units"],
                "rss_units": baseline_perf["rss_units"]
            }
        })

    report = {
        "status": "PASS",
        "comparison": "Full rice-bean GFF3 import plus exact structure query for fixed locus IDs",
        "date_utc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "assembly": "GCF_029865625.1",
        "annotation_sha256": hashlib.sha256(source.read_bytes()).hexdigest(),
        "gene_ids": sorted(gene_ids),
        "source_gold_tuple_count": expected.total(),
        "trials": trials,
        "median_phytoscope_full_import_ms": statistics.median(t["phytoscope"]["cold_full_import_ms"] for t in trials),
        "median_gffutils_full_index_build_ms": statistics.median(t["gffutils"]["cold_full_index_build_ms"] for t in trials),
        "python": platform.python_version(),
        "platform": platform.platform(),
        "gffutils_version": gffutils.__version__,
        "interpretation": (
            "This is an objective scripted import/search/structure-extraction comparison on identical full input, "
            "with outputs checked against source GFF3 tuples. The tools build different in-memory/on-disk indexes, "
            "so cold import and index-build times are reported separately. It does not compare GUI interaction, "
            "manual workflows, user effort, or general usability and supports no speed/usability claim."
        )
    }
    (out_root / "summary.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"status": report["status"], "tuple_count": expected.total(),
                      "summary": str(out_root / "summary.json")}, indent=2))


if __name__ == "__main__":
    main()

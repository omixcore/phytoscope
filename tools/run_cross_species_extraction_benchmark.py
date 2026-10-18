"""Cross-species, same-input structure extraction benchmark.

This is a parser/query correctness benchmark, not a whole-browser ranking.
It compares the exact GFF hierarchy tuples emitted by PhytoScope and gffutils
0.13 for source-verified release-62 excerpts from four plant species. Index/load
times are reported separately because the tools use different storage designs.
"""
import argparse
import collections
import csv
import hashlib
import json
import platform
import statistics
import subprocess
import sys
from pathlib import Path
from urllib.parse import unquote

import gffutils
from validate_structure_inventory import GENE_TYPES, validate

SPECIES = ("arabidopsis", "rice", "maize", "soybean")


def gene_ids_from_gff3(path):
    ids = []
    with path.open(encoding="utf-8") as stream:
        for line in stream:
            if line.startswith("##FASTA"):
                break
            if not line.strip() or line.startswith("#"):
                continue
            fields = line.rstrip("\n").split("\t")
            if len(fields) != 9:
                raise ValueError(f"Malformed GFF3 row in {path}")
            if fields[2].lower() not in GENE_TYPES:
                continue
            identifier = None
            for token in fields[8].split(";"):
                if token.startswith("ID="):
                    identifier = unquote(token[3:])
                    break
            if identifier:
                ids.append(identifier)
    if not ids:
        raise ValueError(f"No gene IDs found in {path}")
    if len(ids) != len(set(ids)):
        raise ValueError(f"Duplicate gene IDs in benchmark source {path}")
    return sorted(ids)


def record_counter(path):
    with path.open(encoding="utf-8", newline="") as stream:
        reader = csv.reader(stream, delimiter="\t")
        next(reader, None)
        return collections.Counter(tuple(row) for row in reader if row)


def run_checked(name, command, root):
    completed = subprocess.run(
        command, cwd=root, text=True, stdout=subprocess.PIPE,
        stderr=subprocess.PIPE, check=False
    )
    if completed.returncode:
        raise RuntimeError(
            f"{name} failed (exit {completed.returncode):\n"
            f"COMMAND: {' '.join(map(str, command))}\n"
            f"STDOUT:\n{completed.stdout}\nSTDERR:\n{completed.stderr}"
        )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--java", default="java")
    parser.add_argument("--out", type=Path, default=Path("target/cross-species-extraction"))
    parser.add_argument("--trials", type=int, default=3)
    parser.add_argument("--species", nargs="+", choices=SPECIES, default=list(SPECIES))
    args = parser.parse_args()
    if args.trials < 3:
        parser.error("--trials must be at least 3")
    if gffutils.__version__ != "0.13":
        raise RuntimeError(f"Expected gffutils 0.13, found {gffutils.__version__}")

    root = Path(__file__).resolve().parents[1]
    jar = args.jar.resolve()
    if not jar.is_file():
        raise FileNotFoundError(f"PhytoScope JAR not found: {jar}")
    source_dir = root / "src/test/resources/chromosome-validation"
    manifest = json.loads((source_dir / "manifest.json").read_text(encoding="utf-8"))
    out_root = args.out.resolve()
    out_root.mkdir(parents=True, exist_ok=True)
    results = []

    for species_index, species in enumerate(args.species):
        source = source_dir / f"{species}.gff3"
        if not source.is_file():
            raise FileNotFoundError(source)
        loci = gene_ids_from_gff3(source)
        source_sha = hashlib.sha256(source.read_bytes()).hexdigest()
        trials = []

        for trial in range(args.trials):
            trial_dir = out_root / species / f"trial-{trial + 1}"
            java_dir = trial_dir / "phytoscope"
            baseline_dir = trial_dir / "gffutils"
            java_dir.mkdir(parents=True, exist_ok=False)
            baseline_dir.mkdir(parents=True, exist_ok=False)

            commands = [
                ("phytoscope", [
                    args.java, "-Xmx1g", "-jar", str(jar),
                    "--benchmark", "--annotation", str(source),
                    "--out", str(java_dir)
                ]),
                ("gffutils", [
                    sys.executable, str(root / "tools/compare_gffutils.py"),
                    "--annotation", str(source), "--loci", *loci,
                    "--out", str(baseline_dir)
                ])
            ]
            # Alternate execution order to reduce systematic order bias.
            if (trial + species_index) % 2:
                commands.reverse()
            for name, command in commands:
                run_checked(name, command, root)

            java_parsed = java_dir / "parsed-structure.tsv"
            baseline_parsed = baseline_dir / "parsed-structure.tsv"
            java_records = record_counter(java_parsed)
            baseline_records = record_counter(baseline_parsed)
            if java_records != baseline_records:
                missing = list((baseline_records - java_records).items())[:5]
                extra = list((java_records - baseline_records).items())[:5]
                raise AssertionError(
                    f"{species} trial {trial + 1}: output mismatch; "
                    f"missing examples={missing}; extra examples={extra}"
                )

            java_check = validate([source], java_parsed)
            baseline_check = validate([source], baseline_parsed)
            (java_dir / "correctness.json").write_text(
                json.dumps(java_check, indent=2) + "\n", encoding="utf-8"
            )
            (baseline_dir / "correctness.json").write_text(
                json.dumps(baseline_check, indent=2) + "\n", encoding="utf-8"
            )
            if java_check["status"] != "PASS" or baseline_check["status"] != "PASS":
                raise AssertionError(
                    f"{species} trial {trial + 1}: source validation failed: "
                    f"PhytoScope={java_check['status']}, gffutils={baseline_check['status']}"
                )

            java_perf = json.loads((java_dir / "performance.json").read_text(encoding="utf-8"))
            baseline_perf = json.loads((baseline_dir / "performance.json").read_text(encoding="utf-8"))
            trials.append({
                "trial": trial + 1,
                "execution_order": [name for name, _ in commands],
                "exact_tuple_agreement": True,
                "tuple_count": sum(java_records.values()),
                "phytoscope": {
                    "import_ms": java_perf.get("import_ms"),
                    "genes": java_perf.get("genes"),
                    "features": java_perf.get("features"),
                    "sequences": java_perf.get("sequences"),
                    "java": java_perf.get("java"),
                    "os": java_perf.get("os"),
                    "max_heap_bytes": java_perf.get("max_heap_bytes")
                },
                "gffutils": {
                    "index_build_ms": baseline_perf.get("index_build_ms"),
                    "query_100_repetitions_median_ms": baseline_perf.get("query_100_repetitions_median_ms"),
                    "query_p95_ms": baseline_perf.get("query_p95_ms"),
                    "records": baseline_perf.get("records"),
                    "version": baseline_perf.get("version"),
                    "python": baseline_perf.get("python"),
                    "os": baseline_perf.get("os"),
                    "max_rss_platform_units": baseline_perf.get("max_rss_platform_units"),
                    "rss_units": baseline_perf.get("rss_units")
                },
                "phytoscope_source_validation": java_check,
                "gffutils_source_validation": baseline_check
            })

        results.append({
            "species_key": species,
            "species": manifest[species]["species"],
            "assembly": manifest[species]["assembly"],
            "release": 62,
            "source_url": manifest[species]["source"],
            "source_excerpt_sha256": source_sha,
            "manifest_fixture_sha256": manifest[species]["fixture_sha256"],
            "gene_loci": len(loci),
            "trials": trials,
            "median_phytoscope_import_ms": statistics.median(
                item["phytoscope"]["import_ms"] for item in trials
            ),
            "median_gffutils_index_build_ms": statistics.median(
                item["gffutils"]["index_build_ms"] for item in trials
            ),
            "exact_tuple_agreement_all_trials": all(
                item["exact_tuple_agreement"] for item in trials
            )
        })

    summary = {
        "benchmark": "cross-species annotation hierarchy extraction",
        "status": "PASS" if all(x["exact_tuple_agreement_all_trials"] for x in results) else "FAIL",
        "date_utc": __import__("datetime").datetime.now(__import__("datetime").timezone.utc).isoformat(),
        "phytoScope_jar": str(jar),
        "python": platform.python_version(),
        "platform": platform.platform(),
        "gffutils_version": gffutils.__version__,
        "trials_per_species": args.trials,
        "species": results,
        "interpretation": (
            "Exact source-to-output tuple agreement is the correctness endpoint. "
            "PhytoScope in-memory import time and gffutils SQLite index construction "
            "are reported separately because they are not identical storage operations. "
            "These source-verified excerpts sample two annotated genes per nuclear "
            "chromosome; they are not whole-genome biological gold standards, do not "
            "benchmark homology/domain/promoter accuracy, and do not establish a "
            "general speed or usability ranking."
        )
    }
    summary_path = out_root / "summary.json"
    summary_path.write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    (out_root / "README.md").write_text(
        "# Cross-species extraction benchmark\n\n"
        "Generated by `tools/run_cross_species_extraction_benchmark.py`. "
        "The summary records source hashes, assembly/release, exact tuple agreement, "
        "three or more alternating-order trials, and environment metadata. "
        "Per-tool raw timings and correctness JSON are retained by trial. "
        "Import and SQLite index-construction times are not interpreted as an "
        "overall speed ranking. The fixtures are source-verified excerpts, not "
        "whole-genome functional gold sets.\n",
        encoding="utf-8"
    )
    print(json.dumps({
        "status": summary["status"],
        "species": [{
            "species": x["species"],
            "gene_loci": x["gene_loci"],
            "exact_tuple_agreement_all_trials": x["exact_tuple_agreement_all_trials"],
            "median_phytoscope_import_ms": x["median_phytoscope_import_ms"],
            "median_gffutils_index_build_ms": x["median_gffutils_index_build_ms"]
        } for x in results],
        "summary": str(summary_path)
    }, indent=2))
    if summary["status"] != "PASS":
        raise SystemExit(1)


if __name__ == "__main__":
    main()

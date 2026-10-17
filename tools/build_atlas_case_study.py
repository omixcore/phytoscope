"""Build a traceable full-gene Arabidopsis TPM panel from Expression Atlas E-MTAB-7978.

The selected six groups are the same pinned single-assay conditions used by the
original four-locus case study. For every gene/group cell, all five Expression
Atlas summary components must agree before the first value is retained. No
averaging, imputation, or transcript-to-gene aggregation is performed.
"""
import argparse
import csv
import hashlib
import json
import math
import shutil
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

BASE = "https://ftp.ebi.ac.uk/pub/databases/microarray/data/atlas/experiments/E-MTAB-7978/"
GENES = ["AT1G67090", "AT5G38410", "AT5G38420", "AT5G38430"]
GROUPS = ["g7", "g8", "g20", "g2", "g26", "g40"]
NAMES = ["Root", "Rosette_leaf_1", "Rosette_leaf_8", "Cauline_leaf", "Dry_seed", "Pollen"]
ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT = ROOT / "examples/arabidopsis-atlas"
DEFAULT_RESOURCE_OUTPUT = ROOT / "src/main/resources/data/case-studies/arabidopsis-atlas"
MISSING = {"", "NA", "N/A", "NULL", "."}


def download(url, path):
    if path.exists() and path.stat().st_size:
        return
    request = urllib.request.Request(url, headers={"User-Agent": "PhytoScope/0.2.0 expression-atlas builder"})
    last_error = None
    for attempt in range(1, 4):
        try:
            with urllib.request.urlopen(request, timeout=180) as response:
                payload = response.read()
            if not payload or payload.lstrip().lower().startswith((b"<!doctype html", b"<html")):
                raise ValueError(f"Expected a data file, received an empty or HTML response from {url}")
            path.write_bytes(payload)
            return
        except Exception as error:
            last_error = error
            if attempt < 3:
                import time
                time.sleep(attempt * 2)
    raise RuntimeError(f"Could not download {url}: {last_error}") from last_error


def read_source_value(raw, gene, group):
    parts = [part.strip() for part in raw.split(",")]
    if len(parts) != 5:
        raise ValueError(f"{gene}/{group}: expected five source summary components, found {len(parts)}: {raw!r}")
    if len(set(parts)) != 1:
        raise ValueError(f"{gene}/{group}: source components differ; refusing to average or guess: {raw!r}")
    value = parts[0]
    if value.upper() in MISSING:
        return "NA"
    try:
        number = float(value)
    except ValueError as error:
        raise ValueError(f"{gene}/{group}: non-numeric TPM value {value!r}") from error
    if not math.isfinite(number) or number < 0:
        raise ValueError(f"{gene}/{group}: invalid TPM value {value!r}")
    return value


def build(cache, output, resource_output):
    cache.mkdir(parents=True, exist_ok=True)
    output.mkdir(parents=True, exist_ok=True)
    resource_output.mkdir(parents=True, exist_ok=True)
    manifest_path = output / "manifest.json"
    previous_path = manifest_path if manifest_path.exists() else resource_output / "manifest.json"
    previous = json.loads(previous_path.read_text(encoding="utf-8")) if previous_path.exists() else None

    inputs = {}
    for suffix in ["tpms.tsv", "configuration.xml"]:
        name = "E-MTAB-7978-" + suffix
        path = cache / name
        download(BASE + name, path)
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        if previous and digest != previous["inputs"][name]["sha256"]:
            raise ValueError(f"Pinned source hash changed for {name}: {digest}")
        inputs[name] = {"url": BASE + name, "sha256": digest, "bytes": path.stat().st_size}

    config = ET.parse(cache / "E-MTAB-7978-configuration.xml")
    groups = {group.attrib["id"]: group for group in config.findall(".//assay_group")}
    metadata = []
    for group_id, name in zip(GROUPS, NAMES):
        group = groups.get(group_id)
        if group is None:
            raise ValueError(f"Source configuration is missing expected Atlas group {group_id}")
        assays = [assay.text for assay in group.findall("assay")]
        if len(assays) != 1:
            raise ValueError(f"{group_id} must remain a single-assay group, found {len(assays)}")
        metadata.append([name, group_id, group.attrib["label"], assays[0], "1", "TPM"])

    source_path = cache / "E-MTAB-7978-tpms.tsv"
    required = {"GeneID", *GROUPS}
    gene_rows = {}
    ordered_genes = []
    anchor_values = {}
    missing_cells = 0
    checked_cells = 0
    with source_path.open(encoding="utf-8-sig", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        fields = set(reader.fieldnames or [])
        if not required.issubset(fields):
            raise ValueError(f"TPM source lacks required columns {sorted(required - fields)}; found {reader.fieldnames}")
        for row in reader:
            gene = (row.get("GeneID") or "").strip()
            if not gene:
                continue
            if gene in gene_rows:
                raise ValueError(f"Duplicate gene ID in source matrix: {gene}")
            values = []
            for group_id in GROUPS:
                value = read_source_value(row[group_id], gene, group_id)
                values.append(value)
                checked_cells += 1
                if value == "NA":
                    missing_cells += 1
            gene_rows[gene] = values
            ordered_genes.append(gene)
            if gene in GENES:
                anchor_values[gene] = values

    if len(ordered_genes) < 20000:
        raise ValueError(f"Expected a full-gene expression matrix, found only {len(ordered_genes)} gene rows")
    if set(anchor_values) != set(GENES):
        raise ValueError(f"Missing one or more pinned case-study genes: {sorted(set(GENES) - set(anchor_values))}")
    if "AT1G01860" not in gene_rows:
        raise ValueError("Ordinary-gene regression target AT1G01860 is absent from the source matrix")
    ordinary_values = gene_rows["AT1G01860"]
    if any(value == "NA" for value in ordinary_values):
        raise ValueError(f"AT1G01860 has missing values in selected single-assay groups: {ordinary_values}")

    matrix = (
        "# unit=TPM\n"
        "# level=gene\n"
        "# source=Expression Atlas E-MTAB-7978; selected single-assay groups; full gene matrix\n"
        "# provenance=manifest.json; source summary components verified identical before extraction\n"
        + "\t".join(["GeneID"] + NAMES) + "\n"
    )
    matrix += "".join("\t".join([gene] + gene_rows[gene]) + "\n" for gene in ordered_genes)
    (output / "expression.tsv").write_text(matrix, encoding="utf-8", newline="")

    raw = []
    for gene in GENES:
        # Preserve the original source cells as an auditable set of pinned examples.
        with source_path.open(encoding="utf-8-sig", newline="") as stream:
            for row in csv.DictReader(stream, delimiter="\t"):
                if row.get("GeneID") == gene:
                    for group_id in GROUPS:
                        raw.append([gene, group_id, row[group_id]])
                    break
    for name, header, rows in [
        ("samples.tsv", ["Sample", "Atlas_group", "Atlas_label", "ENA_run", "Assays_in_group", "Unit"], metadata),
        ("source-values.tsv", ["GeneID", "Atlas_group", "Unmodified_source_cell"], raw),
    ]:
        with (output / name).open("w", encoding="utf-8", newline="") as stream:
            writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
            writer.writerow(header)
            writer.writerows(rows)

    # Keep the same pinned Araport11 descendant closure for the four comparison genes.
    reference = ROOT / "src/main/resources/data/species/arabidopsis"
    annotation_manifest = json.loads((reference / "manifest.json").read_text(encoding="utf-8"))
    selected = set(GENES)
    annotation_rows = []
    for chromosome in ["Chr1", "Chr5"]:
        path = reference / (chromosome + ".gff3")
        if hashlib.sha256(path.read_bytes()).hexdigest() != annotation_manifest["chromosomes"][chromosome]["sha256"]:
            raise ValueError(f"Bundled annotation hash changed: {chromosome}")
        with path.open(encoding="utf-8") as stream:
            for line in stream:
                if line.startswith("#"):
                    continue
                fields = line.rstrip("\n").split("\t")
                attrs = dict(token.split("=", 1) for token in fields[8].split(";") if "=" in token)
                if attrs.get("ID") in selected or any(parent in selected for parent in attrs.get("Parent", "").split(",")):
                    annotation_rows.append(line.rstrip("\n"))
                    if "ID" in attrs:
                        selected.add(attrs["ID"])
    if not all(any(("ID=" + gene + ";") in row for row in annotation_rows) for gene in GENES):
        raise ValueError("Could not reconstruct the pinned annotation closure for all four comparison genes")
    annotation_header = [
        "##gff-version 3",
        "##species=Arabidopsis thaliana",
        "##assembly=TAIR10 / Araport11",
    ]
    annotation_header += [
        f"##sequence-region {chromosome} 1 {annotation_manifest['chromosomes'][chromosome]['length']}"
        for chromosome in ["Chr1", "Chr5"]
    ]
    (output / "annotation.gff3").write_text("\n".join(annotation_header + annotation_rows) + "\n", encoding="utf-8")

    output_hashes = {
        name: hashlib.sha256((output / name).read_bytes()).hexdigest()
        for name in ["expression.tsv", "samples.tsv", "source-values.tsv", "annotation.gff3"]
    }
    manifest = {
        "accession": "E-MTAB-7978",
        "title": "Arabidopsis tissue atlas",
        "publication_doi": "10.1038/s41586-020-2094-2",
        "assembly": "TAIR10",
        "annotation_join": "Exact AGI gene IDs to bundled Araport11; no transcript-level inference",
        "inputs": inputs,
        "genes": GENES,
        "groups": GROUPS,
        "expression_matrix_gene_count": len(ordered_genes),
        "expression_matrix_sample_count": len(NAMES),
        "expression_matrix_missing_cells": missing_cells,
        "expression_matrix_checked_cells": checked_cells,
        "ordinary_gene_validation": {
            "gene_id": "AT1G01860",
            "values_tpm": {name: value for name, value in zip(NAMES, ordinary_values)},
        },
        "extraction": "All source cells in the six selected single-assay groups were required to contain five identical summary components; the first component was retained unchanged. No averaging, imputation, or transcript-to-gene aggregation.",
        "limitations": "Descriptive gene-level atlas values from one assay per selected condition; no differential-expression statistics, biological replication within these six conditions, or causal inference.",
        "annotation_sources": {chromosome: annotation_manifest["chromosomes"][chromosome]["sha256"] for chromosome in ["Chr1", "Chr5"]},
        "outputs": output_hashes,
    }
    (output / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")

    # Keep the runnable example and bundled desktop resource byte-identical.
    for name in ["expression.tsv", "samples.tsv", "source-values.tsv", "manifest.json"]:
        shutil.copyfile(output / name, resource_output / name)

    print(f"PASS: {len(ordered_genes):,} genes x {len(NAMES)} single-assay TPM conditions")
    print(f"PASS: verified {checked_cells:,} source cells; {missing_cells:,} missing cells preserved as NA")
    print("AT1G01860 TPM: " + ", ".join(f"{name}={value}" for name, value in zip(NAMES, ordinary_values)))
    print(f"TPM source SHA-256: {inputs['E-MTAB-7978-tpms.tsv']['sha256']}")
    print(f"Bundled matrix SHA-256: {output_hashes['expression.tsv']}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--cache", type=Path, default=Path("target/atlas-cache"))
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--resource-output", type=Path, default=DEFAULT_RESOURCE_OUTPUT)
    args = parser.parse_args()
    build(args.cache, args.output, args.resource_output)

#!/usr/bin/env python3
"""Reconcile the frozen AT3G62090.2/.4 candidate with published PIF6-beta.

This is a biological identity gate, not a software feature or a new-discovery
claim. It intentionally fails if the frozen candidate changes or if the
published sequence fixture is not the hash-pinned record.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CASE = ROOT / "examples" / "arabidopsis-isoform-validation"
DEFAULT_BETA_FASTA = DEFAULT_CASE / "published-pif6-beta.fasta"
DEFAULT_BETA_META = DEFAULT_CASE / "published-pif6-beta.json"
DEFAULT_ANNOTATION = DEFAULT_CASE / "annotation.gff3"
DEFAULT_OUT = ROOT / "target" / "independent-isoforms" / "pif6-identity-validation.json"


def read_fasta(path: Path) -> str:
    lines = path.read_text(encoding="utf-8").splitlines()
    sequence = "".join(line.strip() for line in lines if line and not line.startswith(">"))
    sequence = "".join(sequence.split()).upper()
    if not sequence or any(residue not in "ACDEFGHIKLMNPQRSTVWY" for residue in sequence):
        raise ValueError(f"Invalid protein FASTA: {path}")
    return sequence


def transcript_protein_lengths(gff_path: Path, gene_id: str) -> dict[str, dict[str, int]]:
    """Calculate protein lengths from the CDS coordinates in the frozen GFF3."""
    cds_lengths: dict[str, int] = {}
    for line in gff_path.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        fields = line.split("\t")
        if len(fields) != 9 or fields[2] != "CDS":
            continue
        attributes = {}
        for item in fields[8].split(";"):
            key, sep, value = item.partition("=")
            if sep:
                attributes[key] = value
        start, end = int(fields[3]), int(fields[4])
        for parent in attributes.get("Parent", "").split(","):
            if parent.startswith(gene_id + "."):
                cds_lengths[parent] = cds_lengths.get(parent, 0) + end - start + 1

    result: dict[str, dict[str, int]] = {}
    for transcript, cds_nt in sorted(cds_lengths.items()):
        if cds_nt % 3:
            raise SystemExit(f"CDS length is not divisible by three for {transcript}: {cds_nt}")
        result[transcript] = {"cds_length_nt_including_stop": cds_nt, "protein_length_aa": cds_nt // 3 - 1}
    return result


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--case", type=Path, default=DEFAULT_CASE)
    parser.add_argument("--beta-fasta", type=Path, default=DEFAULT_BETA_FASTA)
    parser.add_argument("--beta-metadata", type=Path, default=DEFAULT_BETA_META)
    parser.add_argument("--annotation", type=Path, default=DEFAULT_ANNOTATION)
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    args = parser.parse_args()

    candidates = json.loads((args.case / "candidates.json").read_text(encoding="utf-8"))
    candidate = next((row for row in candidates if row.get("candidate_id") == "DI0006"), None)
    if candidate is None or candidate.get("gene") != "AT3G62090":
        raise SystemExit("DI0006/AT3G62090 is missing or changed; review the biological identity gate.")

    canonical = candidate["canonical_sequence"].upper()
    short = candidate["short_sequence"].upper()
    start = int(candidate["deletion_start_aa"])
    end = int(candidate["deletion_end_aa"])
    beta = read_fasta(args.beta_fasta)
    beta_meta = json.loads(args.beta_metadata.read_text(encoding="utf-8"))
    beta_sha256 = hashlib.sha256(beta.encode("ascii")).hexdigest()
    annotated_models = transcript_protein_lengths(args.annotation, "AT3G62090")
    expected_model_lengths = {
        "AT3G62090.1": 346,
        "AT3G62090.2": 363,
        "AT3G62090.3": 324,
        "AT3G62090.4": 341,
    }
    observed_model_lengths = {tx: item["protein_length_aa"] for tx, item in annotated_models.items()}
    if observed_model_lengths != expected_model_lengths:
        raise SystemExit(
            "The frozen PIF6 transcript inventory/lengths changed; review the identity assessment: "
            + repr(observed_model_lengths)
        )
    if observed_model_lengths["AT3G62090.2"] != len(canonical) or observed_model_lengths["AT3G62090.4"] != len(short):
        raise SystemExit("GFF3-derived lengths disagree with the frozen canonical/short candidate sequences.")

    if len(canonical) != int(candidate["canonical_length_aa"]):
        raise SystemExit("Canonical PIF6 length does not match the frozen candidate metadata.")
    if len(short) != int(candidate["short_length_aa"]):
        raise SystemExit("Short PIF6 length does not match the frozen candidate metadata.")
    if len(beta) != int(beta_meta["sequence_length_aa"]) or beta_sha256 != beta_meta["sequence_sha256"]:
        raise SystemExit("Published PIF6-beta sequence fixture failed its length/SHA-256 check.")
    if not (1 <= start <= end <= len(canonical)):
        raise SystemExit("Candidate deletion coordinates are invalid.")

    # Deletion coordinates are 1-based and inclusive.
    reconstructed_short = canonical[: start - 1] + canonical[end:]
    deletion_reconstruction_matches = reconstructed_short == short
    downstream_c_terminal_region_retained = short[start - 1 :] == canonical[end:]
    exact_published_beta_match = short == beta
    length_difference = len(short) - len(beta)

    report = {
        "protocol": "pif6-published-isoform-identity-v1",
        "candidate_id": "DI0006",
        "gene": "AT3G62090",
        "symbol": "PIL2/PIF6",
        "current_annotation": {
            "canonical_transcript": candidate["canonical_transcript"],
            "short_transcript": candidate["short_transcript"],
            "canonical_length_aa": len(canonical),
            "short_length_aa": len(short),
            "deletion_start_aa_1based_inclusive": start,
            "deletion_end_aa_1based_inclusive": end,
            "deletion_length_aa": end - start + 1,
            "annotated_domain": candidate["domain"],
            "domain_start_aa": candidate["domain_start_aa"],
            "domain_end_aa": candidate["domain_end_aa"],
        },
        "published_reference": {
            "name": beta_meta["record_name"],
            "protein_accession": beta_meta["protein_accession"],
            "genbank_protein_accession": beta_meta["genbank_protein_accession"],
            "genbank_transcript_accession": beta_meta["genbank_transcript_accession"],
            "sequence_length_aa": len(beta),
            "sequence_sha256": beta_sha256,
            "literature_reported_construct_lengths_aa": beta_meta["reported_construct_lengths_aa"],
            "sources": beta_meta["sources"],
        },
        "sequence_reconciliation": {
            "candidate_pair_reconstructs_as_the_frozen_in_frame_deletion": deletion_reconstruction_matches,
            "current_short_exactly_matches_published_pif6_beta": exact_published_beta_match,
            "length_difference_current_short_minus_reference_aa": length_difference,
            "downstream_c_terminal_region_after_internal_deletion_is_retained": downstream_c_terminal_region_retained,
            "all_annotated_pif6_transcript_protein_lengths": annotated_models,
            "no_current_model_matches_either_published_beta_length": not any(
                model["protein_length_aa"] in set(beta_meta["reported_construct_lengths_aa"] + [len(beta)])
                for model in annotated_models.values()
            ),
        },
        "decision": "reject_as_isoform_specific_known_positive",
        "interpretation": (
            "The current AT3G62090.4 model is a 341-aa protein with an internal "
            "22-aa deletion and retained downstream C-terminal sequence. The published "
            "PIF6-beta construct is reported as 182 aa in the literature; the pinned "
            "UniProtKB D0EDQ7 sequence is 188 aa. The current model is not sequence-identical "
            "to that published beta variant, so the published seed-dormancy phenotype "
            "cannot be assigned to AT3G62090.4. PIF6 remains locus-level context only; "
            "this result is not a new biological discovery."
        ),
        "new_biological_discovery_established": False,
    }

    if not deletion_reconstruction_matches:
        raise SystemExit("Frozen PIF6 candidate no longer reconstructs from its stated deletion.")
    if exact_published_beta_match or length_difference <= 0 or not downstream_c_terminal_region_retained:
        raise SystemExit("PIF6 identity decision changed; review before making any isoform-specific claim.")
    published_lengths = set(beta_meta["reported_construct_lengths_aa"] + [len(beta)])
    if any(model["protein_length_aa"] in published_lengths for model in annotated_models.values()):
        raise SystemExit("A current Araport11 model has a published-beta length; review sequence identity before concluding.")

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(
        "PIF6 identity gate passed: current Araport11 protein models are "
        f"{observed_model_lengths}; none matches the published 182/188-aa beta length. "
        "No phenotype transfer or novelty claim."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

"""Fetch two public RefSeq annotations and verify each against NCBI's MD5 listing."""
import argparse
import datetime
import hashlib
import json
import os
import tempfile
import urllib.request
from pathlib import Path


CASES = {
    "andrographis-paniculata": {
        "species": "Andrographis paniculata",
        "assembly": "GCF_009805555.1",
        "assembly_name": "ASM980555v1",
        "annotation_release": "NCBI Annotation Release 100",
        "gene_count": 27712,
        "target_gene": "LOC127243311",
        "target_sequence": "NW_026137634.1",
        "target_start": 2929092,
        "target_end": 2934822,
        "annotation_report": "https://www.ncbi.nlm.nih.gov/refseq/annotation_euk/Andrographis_paniculata/100/",
    },
    "hibiscus-syriacus": {
        "species": "Hibiscus syriacus",
        "assembly": "GCF_006381635.1",
        "assembly_name": "ASM638163v2",
        "annotation_release": "NCBI Annotation Release 100",
        "gene_count": 106303,
        "target_gene": "LOC120212642",
        "target_sequence": "NW_024062564.1",
        "target_start": 758339,
        "target_end": 760358,
        "target_strand": "-",
        "annotation_report": "https://www.ncbi.nlm.nih.gov/refseq/annotation_euk/Hibiscus_syriacus/100/",
    },
}


def fetch(url: str, destination: Path) -> None:
    request = urllib.request.Request(url, headers={"User-Agent": "PhytoScope-validation/1.0"})
    with urllib.request.urlopen(request, timeout=180) as response, destination.open("wb") as output:
        while chunk := response.read(1024 * 1024):
            output.write(chunk)


def digests(path: Path) -> tuple[str, str]:
    md5, sha256 = hashlib.md5(), hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            md5.update(chunk)
            sha256.update(chunk)
    return md5.hexdigest(), sha256.hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--directory", type=Path, required=True)
    args = parser.parse_args()
    args.directory.mkdir(parents=True, exist_ok=True)
    manifest = {"retrieved_utc_date": datetime.datetime.now(datetime.timezone.utc).date().isoformat(),
                "checksum_authority": "NCBI assembly md5checksums.txt", "datasets": {}}
    for key, case in CASES.items():
        prefix = case["assembly"].split("_")
        digits = prefix[1].split(".")[0]
        path_part = "/".join((prefix[0], digits[:3], digits[3:6], digits[6:]))
        assembly_dir = f"https://ftp.ncbi.nlm.nih.gov/genomes/all/{path_part}/{case['assembly']}_{case['assembly_name']}"
        filename = f"{case['assembly']}_{case['assembly_name']}_genomic.gff.gz"
        source_url = f"{assembly_dir}/{filename}"
        with tempfile.TemporaryDirectory(dir=args.directory) as tmp:
            tmp_path = Path(tmp)
            checksums = tmp_path / "md5checksums.txt"
            fetch(f"{assembly_dir}/md5checksums.txt", checksums)
            official_md5 = None
            for line in checksums.read_text(encoding="ascii").splitlines():
                fields = line.split()
                if len(fields) == 2 and Path(fields[1]).name == filename:
                    official_md5 = fields[0].lower()
                    break
            if official_md5 is None:
                raise RuntimeError(f"NCBI md5checksums.txt has no entry for {filename}")
            temporary = tmp_path / filename
            fetch(source_url, temporary)
            actual_md5, actual_sha = digests(temporary)
            if actual_md5 != official_md5:
                raise RuntimeError(f"NCBI MD5 mismatch for {filename}: {actual_md5} != {official_md5}")
            destination = args.directory / filename
            os.replace(temporary, destination)
        case_manifest = dict(case)
        case_manifest.update({"format": "GFF3", "source_url": source_url, "source_md5": official_md5,
                              "source_sha256": actual_sha, "bytes": destination.stat().st_size,
                              "local_file": filename})
        manifest["datasets"][key] = case_manifest
        print(f"VERIFIED {key} {filename} MD5={official_md5} SHA256={actual_sha}")
    (args.directory / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()

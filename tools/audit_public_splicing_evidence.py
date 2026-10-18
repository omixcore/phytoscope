#!/usr/bin/env python3
"""Audit public Arabidopsis alternative-splicing and proteomics resources.

This is a public-data-only screen. Missing hits are not evidence of absence.
"""
from __future__ import annotations

import argparse
import csv
import gzip
import hashlib
import io
import json
import re
import sys
import time
import urllib.parse
import urllib.request
import zipfile
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path

GENE_RE = re.compile(r"\bAT[1-5CM]G\d{5}\b", re.IGNORECASE)
EVENT_RE = re.compile(r"\bAth[A-Z]{2,}\d+\b")
PEPTIDE = "ESVEEETEEEMR"
PASTDB_DOWNLOADS = "https://pastdb.crg.eu/wiki/Downloads"
ENA_API = (
    "https://www.ebi.ac.uk/ena/portal/api/filereport?"
    "accession=PRJNA1418670&result=read_run&"
    "fields=run_accession,experiment_accession,study_accession,sample_accession,"
    "scientific_name,library_strategy,instrument_platform,library_layout,"
    "read_count,base_count,fastq_ftp,fastq_bytes,submitted_ftp,submitted_bytes&format=tsv"
)
FIGSHARE_API = "https://api.figshare.com/v2/articles/9959036"
PMC_SUPPLEMENTS = {
    "pastdb_table_s1": [
        "https://pmc.ncbi.nlm.nih.gov/articles/instance/7807721/bin/13059_2020_2258_MOESM1_ESM.xlsx",
        "https://static-content.springer.com/esm/art%3A10.1186%2Fs13059-020-02258-y/MediaObjects/13059_2020_2258_MOESM1_ESM.xlsx",
    ],
    "pastdb_table_s3": [
        "https://pmc.ncbi.nlm.nih.gov/articles/instance/7807721/bin/13059_2020_2258_MOESM4_ESM.xlsx",
        "https://static-content.springer.com/esm/art%3A10.1186%2Fs13059-020-02258-y/MediaObjects/13059_2020_2258_MOESM4_ESM.xlsx",
    ],
}
MAX_TABLE_BYTES = 180 * 1024 * 1024
MAX_LIBRARY_FILE_BYTES = 200 * 1024 * 1024
MAX_LIBRARY_TOTAL_BYTES = 450 * 1024 * 1024


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def fetch_bytes(url: str, max_bytes: int = MAX_TABLE_BYTES) -> bytes:
    last_error = None
    for attempt in range(3):
        try:
            request = urllib.request.Request(
                url,
                headers={
                    "User-Agent": "PhytoScope-public-data-audit/1.0 (reproducible research)",
                    "Accept": "*/*",
                },
            )
            with urllib.request.urlopen(request, timeout=120) as response:
                declared = response.headers.get("Content-Length")
                if declared and int(declared) > max_bytes:
                    raise ValueError(f"remote file exceeds byte limit ({declared} > {max_bytes})")
                chunks, total = [], 0
                while True:
                    chunk = response.read(1024 * 1024)
                    if not chunk:
                        break
                    total += len(chunk)
                    if total > max_bytes:
                        raise ValueError(f"download exceeded byte limit ({total} > {max_bytes})")
                    chunks.append(chunk)
                return b"".join(chunks)
        except Exception as exc:
            last_error = f"{type(exc).__name__}: {exc}"
            if attempt < 2:
                time.sleep(2 ** attempt)
    raise RuntimeError(f"could not fetch {url}: {last_error}")


def fetch_first(name: str, urls: list[str], max_bytes: int = MAX_TABLE_BYTES):
    failures = []
    for url in urls:
        try:
            data = fetch_bytes(url, max_bytes=max_bytes)
            return {"name": name, "url": url, "sha256": sha256(data), "bytes": len(data)}, data
        except Exception as exc:
            failures.append(f"{url}: {exc}")
    return {"name": name, "status": "unavailable", "errors": failures}, None


class AnchorParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.links = []
        self.current = None

    def handle_starttag(self, tag, attrs):
        if tag.lower() == "a":
            attrs = dict(attrs)
            self.current = {"href": attrs.get("href", ""), "title": attrs.get("title", ""), "text": []}

    def handle_data(self, data):
        if self.current is not None:
            self.current["text"].append(data)

    def handle_endtag(self, tag):
        if tag.lower() == "a" and self.current is not None:
            self.current["text"] = " ".join(self.current["text"]).strip()
            self.links.append(self.current)
            self.current = None


def candidate_genes(path: Path):
    with path.open(newline="", encoding="utf-8-sig") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    result = {}
    for row in rows:
        gene = (row.get("gene") or row.get("Gene") or "").strip().upper()
        if GENE_RE.fullmatch(gene):
            result.setdefault(gene, []).append(row.get("candidate_id", ""))
    return result


def open_workbook(data: bytes):
    import openpyxl
    book = openpyxl.load_workbook(io.BytesIO(data), read_only=True, data_only=True)
    sheets = {}
    for sheet in book.worksheets:
        rows = []
        for row in sheet.iter_rows(values_only=True):
            values = ["" if value is None else str(value).strip() for value in row]
            if any(values):
                rows.append(values)
        sheets[sheet.title] = rows
    book.close()
    return sheets


def parse_text_table(text: str):
    sample = text[:8192]
    delimiter = "\t" if sample.count("\t") >= sample.count(",") else ","
    return [row for row in csv.reader(io.StringIO(text), delimiter=delimiter) if any(cell.strip() for cell in row)]


def decode_table(data: bytes, name: str):
    if data[:2] == b"\x1f\x8b":
        data = gzip.decompress(data)
    if data[:4] == b"PK\x03\x04":
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            members = [
                n for n in archive.namelist()
                if n.lower().endswith((".tsv", ".txt", ".csv", ".tab", ".tsv.gz", ".csv.gz"))
            ]
            if not members:
                raise ValueError(f"no text table member in archive {name}")
            member = sorted(members, key=lambda n: (len(n), n))[0]
            payload = archive.read(member)
            if payload[:2] == b"\x1f\x8b":
                payload = gzip.decompress(payload)
            return parse_text_table(payload.decode("utf-8-sig", errors="replace"))
    return parse_text_table(data.decode("utf-8-sig", errors="replace"))


def gene_hits_in_rows(rows, genes):
    hits = []
    for row_number, row in enumerate(rows, start=1):
        joined = " | ".join(str(x) for x in row)
        matches = sorted(gene for gene in genes if gene in joined.upper())
        if not matches:
            continue
        digits = {int(x) for x in re.findall(r"(?<![A-Za-z])\d{5,9}(?![A-Za-z])", joined)}
        hits.append({
            "row_number": row_number,
            "genes": matches,
            "event_ids": sorted(set(EVENT_RE.findall(joined))),
            "exact_cipk15_boundaries_both_present": 311308 in digits and 311444 in digits,
            "contains_311308": 311308 in digits,
            "contains_311444": 311444 in digits,
            "row": [str(value).strip()[:300] for value in row],
        })
    return hits


def parse_xlsx_hits(data, genes):
    sheets = open_workbook(data)
    all_hits = []
    sheet_summary = {}
    for sheet_name, rows in sheets.items():
        hits = gene_hits_in_rows(rows, genes)
        sheet_summary[sheet_name] = {"nonempty_rows": len(rows), "candidate_rows": len(hits)}
        for hit in hits:
            hit["sheet"] = sheet_name
            all_hits.append(hit)
    return sheet_summary, all_hits


def scrape_pastdb_tables(outdir: Path, genes):
    manifest = {"page_url": PASTDB_DOWNLOADS, "status": "not_checked", "tables": {}, "errors": []}
    try:
        page_bytes = fetch_bytes(PASTDB_DOWNLOADS, max_bytes=20 * 1024 * 1024)
        page = page_bytes.decode("utf-8", errors="replace")
        manifest["page_sha256"] = sha256(page_bytes)
        parser = AnchorParser()
        parser.feed(page)
        manifest["status"] = "download_page_retrieved"
        wanted = {
            "event_information": ("event information",),
            "event_metrics": ("event metrics",),
            "event_gene_map": ("event_id-to-gene_id", "event id-to-gene id", "event_id to gene_id"),
            "main_psi": ("main psi table",),
            "abiotic_psi": ("abiotic psi table",),
            "sample_information": ("sample information",),
        }
        for key, labels in wanted.items():
            urls = []
            for link in parser.links:
                label = " ".join([link.get("text", ""), link.get("title", "")]).lower()
                if link.get("href") and any(term in label for term in labels):
                    urls.append(urllib.parse.urljoin(PASTDB_DOWNLOADS, link["href"]))
            if not urls:
                manifest["tables"][key] = {"status": "link_not_found"}
                continue
            last_error = None
            for url in urls:
                try:
                    data = fetch_bytes(url, max_bytes=MAX_TABLE_BYTES)
                    suffix = Path(urllib.parse.urlparse(url).path).suffix or ".dat"
                    (outdir / f"pastdb_{key}{suffix}").write_bytes(data)
                    if data[:4] == b"PK\x03\x04" and suffix.lower() == ".xlsx":
                        sheets, hits = parse_xlsx_hits(data, genes)
                        rows_examined = sum(v["nonempty_rows"] for v in sheets.values())
                    else:
                        if data[:2] == b"\x1f\x8b":
                            payload = gzip.decompress(data)
                        else:
                            payload = data
                        rows = decode_table(payload, key)
                        hits = gene_hits_in_rows(rows, genes)
                        rows_examined = len(rows)
                    manifest["tables"][key] = {
                        "status": "downloaded",
                        "url": url,
                        "bytes": len(data),
                        "sha256": sha256(data),
                        "candidate_rows": len(hits),
                        "rows_examined": rows_examined,
                        "hits": hits[:100],
                    }
                    break
                except Exception as exc:
                    last_error = f"{type(exc).__name__}: {exc}"
            else:
                manifest["tables"][key] = {"status": "unavailable", "error": last_error}
    except Exception as exc:
        manifest["status"] = "unavailable"
        manifest["errors"].append(f"{type(exc).__name__}: {exc}")
    return manifest


def fetch_ena_metadata():
    info, data = fetch_first("ENA run metadata", [ENA_API], max_bytes=20 * 1024 * 1024)
    if data is None:
        return info, []
    rows = list(csv.DictReader(io.StringIO(data.decode("utf-8-sig", errors="replace")), delimiter="\t"))
    info["run_count"] = len(rows)
    info["runs"] = rows
    return info, rows


def fetch_supplement(name: str, urls: list[str], outdir: Path, genes):
    info, data = fetch_first(name, urls)
    if data is None:
        return info, []
    dest = outdir / f"{name}.xlsx"
    dest.write_bytes(data)
    sheets, hits = parse_xlsx_hits(data, genes)
    info["local_file"] = dest.name
    info["sheets"] = sheets
    info["candidate_rows"] = len(hits)
    info["hits"] = hits[:200]
    return info, hits


def scan_zip_for_peptide(data: bytes):
    hits = []
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        for name in archive.namelist():
            if name.endswith("/") or not name.lower().endswith(
                (".txt", ".tsv", ".csv", ".msp", ".sptxt", ".splib", ".pep", ".fasta", ".fa")
            ):
                continue
            try:
                with archive.open(name) as handle:
                    carry = b""
                    while True:
                        chunk = handle.read(1024 * 1024)
                        if not chunk:
                            break
                        block = carry + chunk
                        if PEPTIDE.encode() in block:
                            hits.append(name)
                            break
                        carry = block[-len(PEPTIDE):]
            except Exception:
                continue
    return sorted(set(hits))


def audit_figshare_spectral_library():
    info, raw = fetch_first("Figshare spectral library metadata", [FIGSHARE_API], max_bytes=10 * 1024 * 1024)
    if raw is None:
        return info, []
    try:
        article = json.loads(raw)
    except Exception as exc:
        return {**info, "status": "invalid_json", "error": str(exc)}, []
    files = article.get("files", [])
    info["article_title"] = article.get("title")
    info["article_doi"] = article.get("doi")
    info["files"] = [
        {"name": f.get("name"), "size": f.get("size"), "download_url": f.get("download_url"),
         "computed_md5": f.get("computed_md5")}
        for f in files
    ]
    likely = []
    for item in files:
        name = (item.get("name") or "").lower()
        if any(token in name for token in ("spectral", "library", "assay")) or name.endswith(
            (".tsv", ".txt", ".msp", ".sptxt", ".splib", ".zip", ".gz")
        ):
            likely.append(item)
    total_downloaded = 0
    searched, skipped, peptide_hits = [], [], []
    for item in likely:
        name = item.get("name") or "unnamed"
        size = int(item.get("size") or 0)
        if size > MAX_LIBRARY_FILE_BYTES or total_downloaded + size > MAX_LIBRARY_TOTAL_BYTES:
            skipped.append({"name": name, "size": size, "reason": "download cap"})
            continue
        url = item.get("download_url")
        if not url:
            skipped.append({"name": name, "size": size, "reason": "no download URL"})
            continue
        try:
            data = fetch_bytes(url, max_bytes=MAX_LIBRARY_FILE_BYTES)
            total_downloaded += len(data)
            hits = []
            if data[:4] == b"PK\x03\x04":
                hits = scan_zip_for_peptide(data)
            else:
                search_data = data
                if data[:2] == b"\x1f\x8b":
                    try:
                        search_data = gzip.decompress(data)
                    except Exception:
                        search_data = data
                if PEPTIDE.encode() in search_data:
                    hits = [name]
            searched.append({"name": name, "size": len(data), "sha256": sha256(data), "peptide_hit": bool(hits)})
            for hit in hits:
                peptide_hits.append({"file": name, "member": hit, "peptide": PEPTIDE})
        except Exception as exc:
            skipped.append({"name": name, "size": size, "reason": f"{type(exc).__name__}: {exc}"})
    info["files_considered"] = len(likely)
    info["files_searched"] = searched
    info["files_skipped"] = skipped
    info["total_bytes_searched"] = total_downloaded
    info["peptide_hits"] = peptide_hits
    info["search_interpretation"] = (
        "Exact sequence text found in a downloaded public spectral-library file; inspect the source spectrum and source-study metadata before counting it as independent protein evidence."
        if peptide_hits else
        "No exact sequence text was found in the eligible downloaded library files. This is not evidence of absence; skipped files and raw spectra remain unresolved."
    )
    return info, peptide_hits


def write_tsv(path: Path, headers, rows):
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=headers, delimiter="\t", extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--candidates", type=Path, default=Path("examples/arabidopsis-isoform-validation/validation.tsv"))
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    genes = candidate_genes(args.candidates)
    if not genes:
        raise SystemExit(f"No candidate genes read from {args.candidates}")

    report = {
        "generated_at_utc": datetime.now(timezone.utc).isoformat(),
        "scope": "public-data-only; no wet-lab work",
        "candidate_genes": {gene: ids for gene, ids in genes.items()},
        "sources": {},
        "claim_boundary": {
            "novel_biological_discovery_established": False,
            "missing_detection_is_not_absence": True,
            "annotation_is_not_independent_evidence": True,
            "same_study_accessions_must_not_be_counted_as_independent_studies": True,
        },
    }

    for name, urls in PMC_SUPPLEMENTS.items():
        info, hits = fetch_supplement(name, urls, args.out, genes)
        report["sources"][name] = info
        if hits:
            write_tsv(
                args.out / f"{name}-candidate-hits.tsv",
                ["sheet", "row_number", "genes", "event_ids", "exact_cipk15_boundaries_both_present",
                 "contains_311308", "contains_311444", "row"],
                [{
                    "sheet": hit.get("sheet", ""),
                    "row_number": hit.get("row_number", ""),
                    "genes": ";".join(hit.get("genes", [])),
                    "event_ids": ";".join(hit.get("event_ids", [])),
                    "exact_cipk15_boundaries_both_present": hit.get("exact_cipk15_boundaries_both_present", False),
                    "contains_311308": hit.get("contains_311308", False),
                    "contains_311444": hit.get("contains_311444", False),
                    "row": json.dumps(hit.get("row", []), ensure_ascii=False),
                } for hit in hits],
            )

    report["sources"]["PastDB_downloads"] = scrape_pastdb_tables(args.out, genes)
    ena_info, ena_rows = fetch_ena_metadata()
    report["sources"]["ENA_PRJNA1418670"] = ena_info
    if ena_rows:
        write_tsv(args.out / "PRJNA1418670-run-metadata.tsv", list(ena_rows[0].keys()), ena_rows)

    figshare_info, peptide_hits = audit_figshare_spectral_library()
    report["sources"]["Zhang2019_Figshare_spectral_libraries"] = figshare_info

    summary = []
    for gene, candidate_ids in sorted(genes.items()):
        gene_hits = []
        for key in PMC_SUPPLEMENTS:
            for hit in report["sources"].get(key, {}).get("hits", []):
                if gene in hit.get("genes", []):
                    gene_hits.append({
                        "source": key,
                        "sheet": hit.get("sheet", ""),
                        "row_number": hit.get("row_number", ""),
                        "exact_cipk15_boundaries_both_present": hit.get("exact_cipk15_boundaries_both_present", False),
                    })
        summary.append({
            "gene": gene,
            "candidate_ids": ";".join(candidate_ids),
            "published_supplementary_AS_rows": len(gene_hits),
            "published_supplementary_rows_json": json.dumps(gene_hits, ensure_ascii=False),
            "independent_proteomics_library_exact_peptide_hit": bool(peptide_hits),
            "interpretation": (
                "Gene occurs in a published differential-AS table; exact isoform support requires coordinate- and sample-level review."
                if gene_hits else
                "Not listed in the screened differential-AS supplementary tables; this does not establish absence of splicing."
            ),
        })
    write_tsv(
        args.out / "candidate-public-splicing-summary.tsv",
        ["gene", "candidate_ids", "published_supplementary_AS_rows",
         "published_supplementary_rows_json", "independent_proteomics_library_exact_peptide_hit", "interpretation"],
        summary,
    )

    report["summary"] = {
        "candidate_gene_count": len(genes),
        "candidate_genes_in_screened_supplementary_tables": [
            row["gene"] for row in summary if row["published_supplementary_AS_rows"]
        ],
        "exact_junction_coordinate_match_in_supplementary_rows": [
            hit for key in PMC_SUPPLEMENTS
            for hit in report["sources"].get(key, {}).get("hits", [])
            if hit.get("exact_cipk15_boundaries_both_present")
        ],
        "independent_public_spectral_library_exact_peptide_hit": bool(peptide_hits),
        "raw_iso_seq_alignment_performed": False,
        "novel_biological_discovery_established": False,
    }
    (args.out / "report.json").write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    lines = [
        "# Public-data validation audit",
        "",
        f"- Generated UTC: {report['generated_at_utc']}",
        f"- Candidate genes screened: {len(genes)}",
        f"- Candidate genes listed in screened differential-AS supplementary tables: {len(report['summary']['candidate_genes_in_screened_supplementary_tables'])}",
        f"- Exact CIPK15 junction boundaries found in supplementary rows: {len(report['summary']['exact_junction_coordinate_match_in_supplementary_rows'])}",
        f"- Exact CIPK15 peptide text found in eligible public spectral-library files: {bool(peptide_hits)}",
        f"- PRJNA1418670 SRA runs described by metadata: {len(ena_rows)}",
        "- Raw Iso-Seq reads have not been aligned by this audit.",
        "- Missing table or peptide-library hits are not evidence that an isoform is absent.",
        "- This audit does not establish a novel biological discovery.",
        "",
        "## Source status",
        "",
    ]
    for key, value in report["sources"].items():
        status = value.get("status", "retrieved" if value.get("sha256") else "unknown")
        lines.append(f"- {key}: {status}")
        if value.get("url"):
            lines.append(f"  - URL: {value['url']}")
        if value.get("sha256"):
            lines.append(f"  - SHA-256: {value['sha256']}")
        if value.get("error"):
            lines.append(f"  - Error: {value['error']}")
    lines.extend([
        "",
        "## Interpretation boundary",
        "",
        "This report distinguishes source discovery, public processed-data screening,",
        "and independent biological evidence. Public accession metadata or a gene-level",
        "alternative-splicing hit is not by itself evidence for the exact CIPK15 isoform.",
        "Exact junction support requires coordinate-level and sample-level evidence.",
        "Public protein evidence requires sequence-specific spectra, uniqueness and",
        "source-study deduplication. No wet-lab work is required or planned.",
        "",
    ])
    (args.out / "README.md").write_text("\n".join(lines), encoding="utf-8")

    processed_available = any(
        report["sources"].get(key, {}).get("candidate_rows") is not None
        for key in PMC_SUPPLEMENTS
        if report["sources"].get(key, {}).get("status") != "unavailable"
    ) or any(
        table.get("status") == "downloaded"
        for table in report["sources"]["PastDB_downloads"].get("tables", {}).values()
    )
    print(json.dumps(report["summary"], indent=2), flush=True)
    if not processed_available:
        print("No public processed splicing table could be retrieved; see report.json", file=sys.stderr)
        raise SystemExit(2)


if __name__ == "__main__":
    main()

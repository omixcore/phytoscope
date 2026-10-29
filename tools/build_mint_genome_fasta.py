"""Extract the haplotype-A chromosomes from the published Long3_HAP_v2 genome
and rename the records so they match the Phytoscope GFF3 seqids (Chr1..Chr12).

The published FASTA uses `Chr01_A_Long3HAP` style headers; the annotation uses
`Chr1`. Phytoscope matches FASTA records to annotation sequences by exact id or
by a `chromosome N` phrase in the description, so neither matches out of the
box. Renaming is coordinate-neutral: sequence content is copied verbatim.
"""

from __future__ import annotations

import argparse
import gzip
import re
import sys
import time
from pathlib import Path

LINE_WIDTH = 60


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--fasta", type=Path, required=True, help="published genome FASTA (.gz)")
    ap.add_argument("--out", type=Path, required=True, help="output FASTA (.gz)")
    ap.add_argument("--haplotype", default="A")
    args = ap.parse_args()

    pattern = re.compile(r"^Chr0*(\d+)_" + re.escape(args.haplotype) + r"_Long3HAP$")
    started = time.time()
    written: list[tuple[str, int]] = []
    keep = False
    out = gzip.open(args.out, "wt", encoding="ascii", compresslevel=6, newline="\n")
    try:
        with gzip.open(args.fasta, "rt", errors="replace") as src:
            for line in src:
                if line.startswith(">"):
                    header = line[1:].strip()
                    match = pattern.match(header)
                    keep = match is not None
                    if keep:
                        name = "Chr" + str(int(match.group(1)))
                        out.write(">" + name + " chromosome " + match.group(1) + " Mentha longifolia\n")
                        written.append((name, 0))
                    continue
                if keep:
                    seq = line.strip()
                    if not seq:
                        continue
                    for i in range(0, len(seq), LINE_WIDTH):
                        out.write(seq[i:i + LINE_WIDTH] + "\n")
                    name, count = written[-1]
                    written[-1] = (name, count + len(seq))
    finally:
        out.close()

    total = sum(count for _, count in written)
    print(f"records={len(written)} bases={total:,} in {time.time() - started:.0f}s")
    for name, count in written:
        print(f"  {name:6s} {count:>12,}")
    return 0 if written else 1


if __name__ == "__main__":
    sys.exit(main())

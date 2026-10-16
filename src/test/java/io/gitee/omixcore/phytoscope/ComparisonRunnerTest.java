package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.cli.ComparisonRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ComparisonRunnerTest {
    @TempDir Path temp;

    @Test
    void comparisonLociUseExactGeneIds() throws Exception {
        Path annotation = temp.resolve("collision.gff3");
        Files.writeString(annotation, """
                ##gff-version 3
                c\ts\tgene\t1\t100\t.\t+\t.\tID=gene-A;Name=gene-B
                c\ts\tgene\t200\t300\t.\t+\t.\tID=gene-B;Name=other
                """);
        Path out = temp.resolve("out");
        ComparisonRunner.run(new String[]{"--compare", "--gff3", annotation.toString(),
                "--loci", "gene-B", "--out", out.toString(), "--no-expression"});
        String table = Files.readString(out.resolve("comparison.tsv"));
        String[] lines = table.split("\\R");
        assertTrue(lines.length >= 2, table);
        assertTrue(lines[1].startsWith("\"gene-B\"\t"), table);
    }
}

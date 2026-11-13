package io.gitee.omixcore.phytoscope.intake;

/**
 * Result of checking a single input file: its path, kind, SHA-256, byte count and
 * whether it parsed successfully.
 *
 * @param path    absolute or display path of the file
 * @param kind    {@link IntakeFileKind} of the file
 * @param sha256  lowercase 64-char hex SHA-256 of the raw file bytes
 * @param bytes   size of the raw file in bytes
 * @param ok      true when the file parsed without throwing
 * @param error   the parse error message when {@code ok == false}, otherwise null
 */
public record IntakeFileResult(String path, IntakeFileKind kind, String sha256, long bytes,
                              boolean ok, String error) {
}

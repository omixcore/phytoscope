package io.gitee.omixcore.phytoscope.intake;

import java.util.List;

/**
 * Carrier for the manifest v2 fields produced by the import wizard.
 *
 * <p>Holds the specification version, assembly identity, annotation metadata, the
 * per-file integrity results and the user-confirmed alias map. {@link Assembly}
 * and {@link AnnotationMeta} are the structured sub-records. The wizard builds
 * one of these when the user accepts an import; {@code ProjectStore} serialises
 * it into a schemaVersion-4 manifest and restores it on open.</p>
 *
 * @param specVersion     import-health-check specification version string
 * @param assembly        reference assembly identity, nullable
 * @param annotationMeta  annotation metadata, nullable
 * @param files           per-file integrity results, nullable
 * @param aliasMap        user-confirmed sequence-name aliases, nullable
 */
public record IntakeManifestMeta(String specVersion, Assembly assembly, AnnotationMeta annotationMeta,
                                List<IntakeFileResult> files, List<AliasMapping> aliasMap) {
    public IntakeManifestMeta {
        files = files == null ? List.of() : List.copyOf(files);
        aliasMap = aliasMap == null ? List.of() : List.copyOf(aliasMap);
    }
}

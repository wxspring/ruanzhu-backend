package com.company.ruanzhu.file.enums;

/**
 * File-type constants for the file_record table.
 */
public final class FileType {

    private FileType() {
    }

    /** Seed source-code archive (ZIP) uploaded by the user. */
    public static final String SEED_CODE = "SEED_CODE";

    /** Generated source code (txt) stored for inline editing. */
    public static final String GENERATED_CODE = "GENERATED_CODE";

    /** Generated operation manual (html) stored for inline editing. */
    public static final String MANUAL = "MANUAL";

    /** Exported ZIP package (program docs + manual + summary) uploaded after async export. */
    public static final String EXPORT_ZIP = "EXPORT_ZIP";
}

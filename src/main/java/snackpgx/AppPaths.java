package snackpgx;

import java.io.File;

/**
 * Resolves the location of the bundled read-only data directories
 * ({@code resources/} and {@code gene_tables/}).
 *
 * <p>When launched as a jpackage installed app, the launcher passes
 * {@code -Dapp.dir=$APPDIR} so the data files are found next to the
 * application image regardless of the process working directory.
 * When run from an IDE / Gradle ({@code app.dir} unset) the base
 * defaults to {@code "."}, preserving the original behaviour
 * (paths resolve relative to the project root).
 */
public final class AppPaths {
	private AppPaths() {}

	private static final String BASE = System.getProperty("app.dir", ".");

	/** Absolute or relative path to the {@code resources} directory. */
	public static String resourcesDir() {
		return new File(BASE, "resources").getPath();
	}

	/** Absolute or relative path to the {@code gene_tables} directory. */
	public static String geneTablesDir() {
		return new File(BASE, "gene_tables").getPath();
	}
}

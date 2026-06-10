package snackpgx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import javafx.application.Application;

/**
 * Thin entry point that does NOT extend {@link javafx.application.Application}.
 *
 * <p>Two jobs:
 *
 * <ol>
 *   <li><b>Class-path JavaFX:</b> when JavaFX is delivered on the class-path
 *   (as in the jpackage image), launching a class that directly extends
 *   {@code Application} fails with "JavaFX runtime components are missing".
 *   Delegating through this non-Application wrapper avoids that check.</li>
 *
 *   <li><b>Console redirection:</b> the packaged Windows launcher is a
 *   <em>windowed</em> (no-console) process. Its inherited stdout/stderr handles
 *   are not drained by anyone, so once the OS pipe buffer fills, the next
 *   {@code System.out}/{@code System.err} write blocks forever — and because
 *   JavaFX/POI emit messages on the FX thread during {@code FXMLLoader.load()}
 *   (e.g. the "Unsupported JavaFX configuration: classes loaded from unnamed
 *   module" warning), startup intermittently dead-locks. Redirecting both
 *   streams to a file (file writes never block like a full pipe) eliminates
 *   the hang and preserves the output as a diagnostic log.</li>
 * </ol>
 */
public class Launcher {
	public static void main(String[] args) {
		redirectConsoleToLog();
		Application.launch(MainStage.class, args);
	}

	private static void redirectConsoleToLog() {
		try {
			File logDir = new File(System.getProperty("user.home"), ".snackpgx");
			logDir.mkdirs();
			PrintStream out = new PrintStream(
					new FileOutputStream(new File(logDir, "snackpgx.log"), true), true, "UTF-8");
			System.setOut(out);
			System.setErr(out);
		} catch (Exception ignore) {
			// If redirection fails, fall through — worst case the original
			// (possibly blocking) streams remain, but startup is not aborted.
		}
	}
}

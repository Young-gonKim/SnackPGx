package snackpgx;

import javafx.scene.Parent;
import javafx.scene.Scene;

/**
 * Small UI helper so every window shares the same stylesheet (global font).
 * Use {@link #scene(Parent)} instead of {@code new Scene(...)} when building
 * any window/dialog.
 */
public final class Ui {
	private Ui() {}

	/** Resource URL of the global stylesheet (snackpgx/app.css); null if absent. */
	public static final String STYLESHEET = resolveStylesheet();

	private static String resolveStylesheet() {
		java.net.URL u = Ui.class.getResource("app.css");
		return (u == null) ? null : u.toExternalForm();
	}

	/** Creates a Scene with the global stylesheet applied (if available). */
	public static Scene scene(Parent root) {
		Scene s = new Scene(root);
		if (STYLESHEET != null)
			s.getStylesheets().add(STYLESHEET);
		return s;
	}
}

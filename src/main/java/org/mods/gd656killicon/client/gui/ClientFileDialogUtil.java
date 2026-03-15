package org.mods.gd656killicon.client.gui;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public final class ClientFileDialogUtil {
    private ClientFileDialogUtil() {
    }

    public static boolean isNativeDialogAvailable() {
        return !GraphicsEnvironment.isHeadless();
    }

    public static Path chooseOpenFile(String title, Path initialDir, String description, String... extensions) {
        return runChooser(false, title, initialDir, null, description, extensions);
    }

    public static Path chooseSaveFile(String title, Path initialDir, String suggestedFileName, String description, String... extensions) {
        return runChooser(true, title, initialDir, suggestedFileName, description, extensions);
    }

    public static Path tryParsePath(String rawInput) {
        if (rawInput == null) {
            return null;
        }
        String trimmed = rawInput.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Path.of(trimmed);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean hasExtension(Path path, String... extensions) {
        if (path == null || extensions == null || extensions.length == 0) {
            return true;
        }
        Path fileName = path.getFileName();
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toString().toLowerCase(Locale.ROOT);
        for (String extension : extensions) {
            if (extension != null && !extension.isBlank() && lower.endsWith("." + extension.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isExistingFileWithExtension(String rawInput, String... extensions) {
        Path path = tryParsePath(rawInput);
        return path != null && Files.isRegularFile(path) && hasExtension(path, extensions);
    }

    public static boolean isWritablePath(String rawInput) {
        Path path = tryParsePath(rawInput);
        if (path == null) {
            return false;
        }
        Path parent = path.toAbsolutePath().getParent();
        return parent == null || Files.isDirectory(parent);
    }

    public static Path ensureExtension(Path path, String extension) {
        if (path == null || extension == null || extension.isBlank() || hasExtension(path, extension)) {
            return path;
        }
        Path fileName = path.getFileName();
        if (fileName == null) {
            return path;
        }
        String updatedName = fileName + "." + extension;
        Path parent = path.getParent();
        return parent == null ? Path.of(updatedName) : parent.resolve(updatedName);
    }

    private static Path runChooser(
        boolean saveMode,
        String title,
        Path initialDir,
        String suggestedFileName,
        String description,
        String... extensions
    ) {
        if (!isNativeDialogAvailable()) {
            return null;
        }

        AtomicReference<Path> result = new AtomicReference<>(null);
        Runnable task = () -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle(title);
            if (initialDir != null) {
                chooser.setCurrentDirectory(initialDir.toFile());
            }
            if (suggestedFileName != null && !suggestedFileName.isBlank()) {
                chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), suggestedFileName));
            }
            if (extensions != null && extensions.length > 0) {
                chooser.setFileFilter(new FileNameExtensionFilter(description, extensions));
            }

            int chooserResult = saveMode ? chooser.showSaveDialog(null) : chooser.showOpenDialog(null);
            if (chooserResult != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) {
                return;
            }

            File selected = chooser.getSelectedFile();
            if (saveMode && extensions != null && extensions.length > 0) {
                String lower = selected.getName().toLowerCase();
                boolean matches = false;
                for (String extension : extensions) {
                    if (lower.endsWith("." + extension.toLowerCase())) {
                        matches = true;
                        break;
                    }
                }
                if (!matches) {
                    selected = new File(selected.getParentFile(), selected.getName() + "." + extensions[0]);
                }
            }
            result.set(selected.toPath());
        };

        try {
            if (SwingUtilities.isEventDispatchThread()) {
                task.run();
            } else {
                SwingUtilities.invokeAndWait(task);
            }
        } catch (Exception ignored) {
            return null;
        }
        return result.get();
    }
}

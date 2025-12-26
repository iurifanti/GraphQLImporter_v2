package graphql;

import java.awt.*;
import javax.swing.*;

/**
 * Area di testo personalizzata per visualizzare log in tempo reale, con
 * gestione del colore per gli errori e pulizia automatica quando il testo
 * supera una soglia.
 */
public class LogArea extends JTextArea {

    private static final int LOG_MAX_CHARS = 500_000;

    /**
     * Inizializza l'area rendendola di sola lettura e con font monospaziato.
     */
    public LogArea() {
        setEditable(false);
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    }

    /**
     * Log di una riga, thread-safe e realtime.
     */
    public void log(String msg) {
        log(msg, false);
    }

    /**
     * Log di un errore, thread-safe e realtime.
     */
    public void logError(String msg) {
        log(msg, true);
    }

    private void log(String msg, boolean isError) {
        Runnable r = () -> {
            if (isError) {
                setForeground(Color.RED);
            } else {
                setForeground(Color.BLACK);
            }
            append(msg + "\n");
            enforceLogLimit();
            setCaretPosition(getDocument().getLength());
        };
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    private void enforceLogLimit() {
        int len = getDocument().getLength();
        if (len > LOG_MAX_CHARS) {
            try {
                String text = getText();
                int extra = len - LOG_MAX_CHARS;
                int cutIdx = text.indexOf('\n', extra);
                if (cutIdx < 0) {
                    cutIdx = extra;
                }
                getDocument().remove(0, cutIdx + 1);
            } catch (Exception ignored) {
            }
        }
    }

    public void clear() {
        Runnable r = () -> setText("");
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }
}

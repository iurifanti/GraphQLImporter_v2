package graphql;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import static javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Interfaccia Swing per configurare endpoint GraphQL, credenziali e file Excel
 * da importare o eliminare. Gestisce preset salvati su disco e offre un log in
 * tempo reale delle operazioni svolte.
 */
public class GraphQLExcelForm extends JFrame {

    private static final String PROPERTIES_FILE = "graphql_excel_form.properties";
    private static final String PRESETS_PREFIX = "preset.";

    private JTextField jtfEndpoint;
    private JTextField jtfSecondaryEndpoint;
    private JTextField jtfUsername;
    private JPasswordField jtfPassword;
    private JTextField jtfExcelPath;
    private LogArea logArea;

    private JButton jbImporta;
    private JButton jbElimina;
    private JButton jbSfoglia;
    private JButton jbChiudi;

    private final Properties formProperties = new Properties();

    private Integer loadedFrameWidth = null, loadedFrameHeight = null;

    private JMenu presetsMenu;
    private Map<String, Preset> loadedPresets = new LinkedHashMap<>();

    private static final String HELP_TEXT
            = "Guida alla compilazione del file Excel per l'importazione:\n"
            + "\n"
            + "- Generale\n"
            + "    - Tutti i nomi di campi e fogli sono case sensitive\n"
            + "    - La prima riga di ogni foglio contiene l'intestazione\n"
            + "    - Se si vuole forzare l'uso delle virgolette nella mutation per il valore di un determinato campo,\n"
            + "      bisogna usare come prefisso il carattere '§' nel nome del campo (e.g. §latitudine)\n"
            + "    - I riferimenti esterni (associazioni o whole) hanno come prefisso il carattere '*' (e.g. *EDSS_area.nome)\n"
            + "    - Se il nome del ruolo del riferimento esterno ha un formato diverso da quello \n"
            + "      di default (ossia il nome della classe con la prima lettera minuscola e underscore \n"
            + "      finale), il nome del ruolo va specificato all'interno di parentesi quadre immediatamente\n"
            + "      prima del carattere di separazione tra classe e nome campo (e.g. Patologia[patologia_epatica].nome)\n"
            + "    - L'ordine dei fogli deve rispettare le dipendenze delle classi, quindi le\n"
            + "      classi con dipendenze esterne vanno messe dopo\n"
            + "    - Se il nome della classe eccede la lunghezza massima supportata da Excel per i nomi dei fogli, si può creare\n"
            + "      un foglio chiamato \"_mapping\" con due colonne: vecchio nome foglio, nuovo nome foglio. Il nome delle\n"
            + "      colonne non conta, basta che siano nell'ordine vecchio-nuovo, e che i valori contengano anche eventuali\n"
            + "      simboli per le composizioni, se il caso.\n"
            + "    - Se si vuole fare riferimento all'ID di un oggetto (come nel caso degli enum), il nome da usare è _id, con un\n"
            + "      solo undersocre (e.g. *Probe._id)\n"
            + "\n"
            + "- Classi main\n"
            + "    - Il nome del foglio è il nome della classe (case sensitive), senza alcun prefisso (e.g. Tipo_visita)\n"
            + "\n"
            + "- Composizioni\n"
            + "    - Il nome del foglio è il nome del ruolo (case sensitive), con il carattere '#' come prefisso (e.g. #localizzazione_CCA_)\n"
            + "    - Il riferimento esterno al whole deve essere la prima colonna del foglio";

    public GraphQLExcelForm() {
        super("Importa/Elimina dati da Excel su GraphQL");

        setSystemLookAndFeel();

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (confirmExit()) {
                    saveProperties();
                    dispose();
                }
            }
        });

        initializeComponents();

        createMenuBar();

        // Carica le preferenze prima del pack così da ripristinare correttamente le dimensioni
        loadProperties();
        pack();
        applyFrameSizeFromProperties();

        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(600, 400));
        setResizable(true);
    }

    private void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            // Se il look&feel non è disponibile si prosegue con quello di default
        }
    }

    private boolean confirmExit() {
        return JOptionPane.showConfirmDialog(
                this,
                "Sei sicuro di voler uscire?",
                "Conferma uscita",
                JOptionPane.YES_NO_OPTION
        ) == JOptionPane.YES_OPTION;
    }

    private void initializeComponents() {
        JPanel jpMainPanel = new JPanel(new GridBagLayout());
        jpMainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Campi di connessione agli endpoint GraphQL
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        jpMainPanel.add(new JLabel("Endpoint GraphQL:"), gbc);

        jtfEndpoint = new JTextField();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        jpMainPanel.add(jtfEndpoint, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        jpMainPanel.add(new JLabel("Endpoint secondario:"), gbc);

        jtfSecondaryEndpoint = new JTextField();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        jpMainPanel.add(jtfSecondaryEndpoint, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        jpMainPanel.add(new JLabel("Username:"), gbc);

        jtfUsername = new JTextField();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        jpMainPanel.add(jtfUsername, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        jpMainPanel.add(new JLabel("Password:"), gbc);

        jtfPassword = new JPasswordField();
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        jpMainPanel.add(jtfPassword, gbc);

        // Sezione per selezionare il file Excel da processare
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        jpMainPanel.add(new JLabel("File Excel:"), gbc);

        jtfExcelPath = new JTextField();
        jtfExcelPath.setEditable(false);
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        jpMainPanel.add(jtfExcelPath, gbc);

        jbSfoglia = new JButton("Sfoglia...");
        gbc.gridx = 2;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        jpMainPanel.add(jbSfoglia, gbc);

        // Area di log scrollabile per mostrare output e errori runtime
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        logArea = new LogArea();
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jpMainPanel.add(scrollPane, gbc);

        // Pannello comandi: importazione, eliminazione e chiusura
        JPanel jpButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        jbImporta = new JButton("Importa");
        jbElimina = new JButton("Elimina");
        jbChiudi = new JButton("Chiudi");
        jpButtonPanel.add(jbImporta);
        jpButtonPanel.add(jbElimina);
        jpButtonPanel.add(jbChiudi);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        jpMainPanel.add(jpButtonPanel, gbc);

        setContentPane(jpMainPanel);

        FocusAdapter selectAllOnFocus = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                // invokeLater evita conflitti con la selezione automatica gestita da Swing
                SwingUtilities.invokeLater(() -> {
                    JTextField source = (JTextField) e.getSource();
                    source.selectAll();
                });
            }
        };

        // Associazione dei listener ai componenti interattivi
        jbSfoglia.addActionListener(this::onSfoglia);
        jbImporta.addActionListener(this::onImporta);
        jbElimina.addActionListener(this::onElimina);
        jbChiudi.addActionListener(e -> {
            saveProperties();
            dispose();
        });
        jtfPassword.addFocusListener(selectAllOnFocus);
        jtfEndpoint.addFocusListener(selectAllOnFocus);
        jtfSecondaryEndpoint.addFocusListener(selectAllOnFocus);
        jtfUsername.addFocusListener(selectAllOnFocus);
    }

    private void onSfoglia(ActionEvent e) {
        String currentPath = jtfExcelPath.getText();
        JFileChooser chooser;
        if (!currentPath.isEmpty()) {
            File currentFile = new File(currentPath);
            chooser = new JFileChooser(currentFile.getParentFile());
        } else {
            chooser = new JFileChooser();
        }
        chooser.setFileFilter(new FileNameExtensionFilter("Excel Files", "xls", "xlsx"));
        int ret = chooser.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            jtfExcelPath.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    // Callback dei pulsanti principali
    private void onImporta(ActionEvent e) {
        logArea.clear();
        logArea.log("Premuto IMPORTA");
        new Thread(()
                -> Application.run(getEndpoint(), getSecondaryEndpoint(), getUsername(), new String(getPassword()), getExcelPath(), false)
        ).start();

    }

    private void onElimina(ActionEvent e) {
        logArea.clear();
        logArea.log("Premuto ELIMINA");
        int res = JOptionPane.showConfirmDialog(
                this,
                "ATTENZIONE!"
                + "\n\nLe tabelle MAIN elencate nel file Excel verranno svuotate completamente nel DB."
                + "\n\nVuoi proseguire con l'operazione?",
                "Conferma eliminazione",
                JOptionPane.YES_NO_OPTION);
        if (res == JOptionPane.YES_OPTION) {
            new Thread(()
                    -> Application.run(getEndpoint(), getSecondaryEndpoint(), getUsername(), new String(getPassword()), getExcelPath(), true)
            ).start();
        }
    }
    // -----------------------------------------------------------------------

    /**
     * Log info.
     */
    public void logInfo(String msg) {
        logArea.log(msg);
    }

    /**
     * Log errore con eccezione.
     */
    public void logError(String msg, Throwable t) {
        logArea.logError(msg + "\n" + getStackTrace(t));
    }

    private String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    // Accessor per i valori inseriti dall'utente
    public String getEndpoint() {
        return jtfEndpoint.getText().trim();
    }

    public String getSecondaryEndpoint() {
        return jtfSecondaryEndpoint.getText().trim();
    }

    public String getUsername() {
        return jtfUsername.getText().trim();
    }

    public char[] getPassword() {
        return jtfPassword.getPassword();
    }

    public String getExcelPath() {
        return jtfExcelPath.getText().trim();
    }

    // --- Gestione delle properties di configurazione ---
    private void loadProperties() {
        File propFile = new File(PROPERTIES_FILE);
        if (!propFile.exists()) {
            return;
        }
        try (FileInputStream fis = new FileInputStream(propFile)) {
            formProperties.load(fis);
            jtfEndpoint.setText(formProperties.getProperty("endpoint", ""));
            jtfSecondaryEndpoint.setText(formProperties.getProperty("secondaryEndpoint", ""));
            jtfUsername.setText(formProperties.getProperty("username", ""));
            jtfPassword.setText(formProperties.getProperty("password", ""));
            jtfExcelPath.setText(formProperties.getProperty("excelPath", ""));
            String widthStr = formProperties.getProperty("frameWidth");
            String heightStr = formProperties.getProperty("frameHeight");
            if (widthStr != null && heightStr != null) {
                try {
                    loadedFrameWidth = Integer.parseInt(widthStr);
                    loadedFrameHeight = Integer.parseInt(heightStr);
                } catch (NumberFormatException ignored) {
                }
            }
            // I preset vanno caricati dopo aver ripristinato i valori base
            loadPresetsFromProperties();
            logArea.log("Parametri caricati da " + PROPERTIES_FILE);
        } catch (IOException ex) {
            logArea.logError("Errore nel caricamento delle impostazioni: " + ex);
        }
    }

    private void applyFrameSizeFromProperties() {
        if (loadedFrameWidth != null && loadedFrameHeight != null) {
            setSize(loadedFrameWidth, loadedFrameHeight);
        }
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Menu di supporto con la guida all'uso
        JMenu helpMenu = new JMenu("Help");
        JMenuItem guidaItem = new JMenuItem("Guida all'utilizzo...");
        guidaItem.addActionListener(e -> showHelpDialog());
        helpMenu.add(guidaItem);

        // Menu per salvare e richiamare preset di configurazione
        presetsMenu = new JMenu("Presets");
        JMenuItem savePresetItem = new JMenuItem("Salva preset...");
        savePresetItem.addActionListener(e -> onSavePreset());
        presetsMenu.add(savePresetItem);
        presetsMenu.addSeparator();
        reloadPresetsMenu();

        menuBar.add(presetsMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void showHelpDialog() {
        JDialog dialog = new JDialog(this, "Guida alla compilazione", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JTextArea textArea = new JTextArea(HELP_TEXT);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        dialog.getContentPane().add(scrollPane, BorderLayout.CENTER);
        dialog.setSize(750, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(true);
        dialog.setVisible(true);
    }

    private void saveProperties() {
        formProperties.setProperty("endpoint", jtfEndpoint.getText());
        formProperties.setProperty("secondaryEndpoint", jtfSecondaryEndpoint.getText());
        formProperties.setProperty("username", jtfUsername.getText());
        formProperties.setProperty("password", new String(jtfPassword.getPassword()));
        formProperties.setProperty("excelPath", jtfExcelPath.getText());
        Dimension size = getSize();
        formProperties.setProperty("frameWidth", Integer.toString(size.width));
        formProperties.setProperty("frameHeight", Integer.toString(size.height));
        // Salva anche le configurazioni preimpostate
        savePresetsToProperties();
        try (FileOutputStream fos = new FileOutputStream(PROPERTIES_FILE)) {
            formProperties.store(fos, "Parametri GraphQLExcelForm");
            logArea.log("Parametri salvati su " + PROPERTIES_FILE);
        } catch (IOException ex) {
            logArea.logError("Errore nel salvataggio delle impostazioni: " + ex);
        }
    }

    // --- Gestione Preset salvati ---
    private static class Preset {

        String name;
        String endpoint;
        String secondaryEndpoint;
        String username;
        String encodedPassword; // password codificata in Base64

        Preset(String name, String endpoint, String secondaryEndpoint, String username, String encodedPassword) {
            this.name = name;
            this.endpoint = endpoint;
            this.secondaryEndpoint = secondaryEndpoint;
            this.username = username;
            this.encodedPassword = encodedPassword;
        }

        String getDecodedPassword() {
            try {
                return new String(Base64.getDecoder().decode(encodedPassword));
            } catch (Exception e) {
                return "";
            }
        }
    }

    private void loadPresetsFromProperties() {
        loadedPresets.clear();
        for (String key : formProperties.stringPropertyNames()) {
            if (key.startsWith(PRESETS_PREFIX) && key.endsWith(".endpoint")) {
                String presetName = key.substring(PRESETS_PREFIX.length(), key.length() - ".endpoint".length());
                String endpoint = formProperties.getProperty(PRESETS_PREFIX + presetName + ".endpoint", "");
                String secondary = formProperties.getProperty(PRESETS_PREFIX + presetName + ".secondary", "");
                String username = formProperties.getProperty(PRESETS_PREFIX + presetName + ".username", "");
                String encodedPassword = formProperties.getProperty(PRESETS_PREFIX + presetName + ".password", "");
                loadedPresets.put(presetName, new Preset(presetName, endpoint, secondary, username, encodedPassword));
            }
        }
        reloadPresetsMenu();
    }

    private void savePresetsToProperties() {
        // Rimuove le chiavi esistenti prima di rigenerare la sezione dedicata ai preset
        List<String> toRemove = new ArrayList<>();
        for (String key : formProperties.stringPropertyNames()) {
            if (key.startsWith(PRESETS_PREFIX)) {
                toRemove.add(key);
            }
        }
        for (String key : toRemove) {
            formProperties.remove(key);
        }
        // Inserisce nuovamente tutti i preset correnti
        for (Preset preset : loadedPresets.values()) {
            formProperties.setProperty(PRESETS_PREFIX + preset.name + ".endpoint", preset.endpoint);
            formProperties.setProperty(PRESETS_PREFIX + preset.name + ".secondary", preset.secondaryEndpoint);
            formProperties.setProperty(PRESETS_PREFIX + preset.name + ".username", preset.username);
            formProperties.setProperty(PRESETS_PREFIX + preset.name + ".password", preset.encodedPassword);
        }
    }

    private void reloadPresetsMenu() {
        presetsMenu.removeAll();

        JMenuItem savePresetItem = new JMenuItem("Salva preset...");
        savePresetItem.addActionListener(e -> onSavePreset());
        presetsMenu.add(savePresetItem);
        presetsMenu.addSeparator();

        if (loadedPresets.isEmpty()) {
            JMenuItem noneItem = new JMenuItem("(Nessun preset)");
            noneItem.setEnabled(false);
            presetsMenu.add(noneItem);
        } else {
            for (Preset preset : loadedPresets.values()) {
                JMenu presetSubMenu = new JMenu(preset.name);

                // Opzione per applicare subito il preset
                JMenuItem useItem = new JMenuItem("Usa");
                useItem.addActionListener(e -> applyPreset(preset));
                presetSubMenu.add(useItem);

                // Voce "Aggiorna"
                JMenuItem updateItem = new JMenuItem("Aggiorna");
                updateItem.addActionListener(e -> onUpdatePreset(preset));
                presetSubMenu.add(updateItem);

                // Voce "Rinomina"
                JMenuItem renameItem = new JMenuItem("Rinomina");
                renameItem.addActionListener(e -> {
                    onRenamePreset(preset);
                });
                presetSubMenu.add(renameItem);

                // Voce "Elimina"
                JMenuItem deleteItem = new JMenuItem("Elimina");
                deleteItem.addActionListener(e -> {
                    onDeletePreset(preset);
                });
                presetSubMenu.add(deleteItem);

                presetsMenu.add(presetSubMenu);
            }
        }
    }

    private void onSavePreset() {
        String presetName = JOptionPane.showInputDialog(
                this,
                "Inserisci il nome del preset:",
                "Salva Preset",
                JOptionPane.PLAIN_MESSAGE);
        if (presetName == null || presetName.trim().isEmpty()) {
            return;
        }
        presetName = presetName.trim();
        if (loadedPresets.containsKey(presetName)) {
            JOptionPane.showMessageDialog(this, "Nome già esistente.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Codifica la password per evitarne il salvataggio in chiaro
        String encodedPwd = Base64.getEncoder().encodeToString(new String(getPassword()).getBytes());
        loadedPresets.put(presetName, new Preset(
                presetName,
                getEndpoint(),
                getSecondaryEndpoint(),
                getUsername(),
                encodedPwd
        ));
        saveProperties(); // Persistenza immediata sul file di properties
        reloadPresetsMenu();
        logArea.log("Preset \"" + presetName + "\" salvato.");
    }

    private void applyPreset(Preset preset) {
        int res = JOptionPane.showConfirmDialog(
                this,
                "Vuoi usare il preset \"" + preset.name + "\"?",
                "Conferma utilizzo",
                JOptionPane.YES_NO_OPTION);
        if (res == JOptionPane.YES_OPTION) {
            jtfEndpoint.setText(preset.endpoint);
            jtfSecondaryEndpoint.setText(preset.secondaryEndpoint);
            jtfUsername.setText(preset.username);
            jtfPassword.setText(preset.getDecodedPassword());
            logArea.log("Preset \"" + preset.name + "\" caricato.");
        }
    }

    private void onDeletePreset(Preset preset) {
        int res = JOptionPane.showConfirmDialog(
                this,
                "Vuoi eliminare il preset \"" + preset.name + "\"?",
                "Conferma eliminazione",
                JOptionPane.YES_NO_OPTION);
        if (res == JOptionPane.YES_OPTION) {
            loadedPresets.remove(preset.name);
            saveProperties();
            reloadPresetsMenu();
            logArea.log("Preset \"" + preset.name + "\" eliminato.");
        }
    }

    private void onRenamePreset(Preset preset) {
        String newName = (String) JOptionPane.showInputDialog(
                this,
                "Nuovo nome per il preset \"" + preset.name + "\":",
                "Rinomina Preset",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                preset.name
        );
        if (newName == null || newName.trim().isEmpty() || loadedPresets.containsKey(newName.trim())) {
            if (newName != null && !newName.trim().equals(preset.name)) {
                JOptionPane.showMessageDialog(this, "Nome non valido o già esistente.", "Errore", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }
        newName = newName.trim();
        Preset renamed = new Preset(newName, preset.endpoint, preset.secondaryEndpoint, preset.username, preset.encodedPassword);
        loadedPresets.remove(preset.name);
        loadedPresets.put(newName, renamed);
        saveProperties();
        reloadPresetsMenu();
        logArea.log("Preset \"" + preset.name + "\" rinominato in \"" + newName + "\".");
    }

    // Aggiorna i valori del preset con quelli attualmente presenti nella form
    private void onUpdatePreset(Preset preset) {
        int res = JOptionPane.showConfirmDialog(
                this,
                "Vuoi aggiornare il preset \"" + preset.name + "\" con i valori correnti della form?",
                "Conferma aggiornamento",
                JOptionPane.YES_NO_OPTION);
        if (res == JOptionPane.YES_OPTION) {
            // Codifica la password (NON in chiaro)
            String encodedPwd = Base64.getEncoder().encodeToString(new String(getPassword()).getBytes());
            Preset updated = new Preset(
                    preset.name,
                    getEndpoint(),
                    getSecondaryEndpoint(),
                    getUsername(),
                    encodedPwd
            );
            loadedPresets.put(preset.name, updated);
            saveProperties();
            reloadPresetsMenu();
            logArea.log("Preset \"" + preset.name + "\" aggiornato.");
        }
    }

//    // Main di test standalone
//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> {
//            GraphQLExcelForm form = new GraphQLExcelForm();
//            form.setVisible(true);
//            form.logInfo("Applicazione pronta.");
//        });
//    }
}

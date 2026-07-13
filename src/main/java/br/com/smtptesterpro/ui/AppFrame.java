package br.com.smtptesterpro.ui;

import br.com.smtptesterpro.application.DiagnosticListener;
import br.com.smtptesterpro.application.SmtpDiagnosticService;
import br.com.smtptesterpro.domain.AuthMode;
import br.com.smtptesterpro.domain.DiagnosticStep;
import br.com.smtptesterpro.domain.SecurityMode;
import br.com.smtptesterpro.domain.SmtpTestRequest;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;

public final class AppFrame extends JFrame {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final JTextField hostField = new JTextField("smtp.gmail.com", 22);
    private final JTextField portField = new JTextField("587", 6);
    private final JComboBox<SecurityMode> securityField = new JComboBox<>(SecurityMode.values());
    private final JTextField ehloField = new JTextField("localhost", 16);
    private final JCheckBox authCheck = new JCheckBox("Autenticar");
    private final JComboBox<AuthMode> authModeField = new JComboBox<>(AuthMode.values());
    private final JTextField userField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JTextArea oauthTokenField = new JTextArea(4, 20);
    private final JCheckBox sendCheck = new JCheckBox("Enviar e-mail de teste");
    private final JTextField fromField = new JTextField(22);
    private final JTextField toField = new JTextField(22);
    private final JTextField subjectField = new JTextField("SMTP Tester Pro - teste", 22);
    private final JTextArea bodyField = new JTextArea("Mensagem de diagnostico enviada pelo SMTP Tester Pro.", 4, 22);
    private final JTextField timeoutField = new JTextField("10000", 7);
    private final JButton runButton = new JButton("Iniciar diagnostico");
    private final JButton clearButton = new JButton("Limpar");
    private final DefaultTableModel stepsModel = new DefaultTableModel(new String[]{"Hora", "Etapa", "Status", "Duracao", "Detalhe"}, 0);
    private final JTextArea protocolArea = new JTextArea();

    public AppFrame() {
        super("SMTP Tester Pro");
        configureWindow();
        setContentPane(buildContent());
        bindActions();
    }

    private void configureWindow() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 760));
        setLocationByPlatform(true);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildWorkspace(), BorderLayout.CENTER);
        return root;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("SMTP Tester Pro");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        JLabel subtitle = new JLabel("Diagnostico SMTP, TLS, DNS, autenticacao e envio em tempo real");
        subtitle.setForeground(new Color(90, 90, 90));
        JPanel text = new JPanel(new BorderLayout());
        text.add(title, BorderLayout.NORTH);
        text.add(subtitle, BorderLayout.SOUTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(clearButton);
        actions.add(runButton);
        header.add(text, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JSplitPane buildWorkspace() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildForm(), buildResults());
        split.setResizeWeight(0.30);
        split.setBorder(BorderFactory.createEmptyBorder());
        return split;
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Configuracao"));
        oauthTokenField.setLineWrap(true);
        oauthTokenField.setWrapStyleWord(false);
        bodyField.setLineWrap(true);
        bodyField.setWrapStyleWord(true);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 6, 5, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        addField(panel, c, 0, "Servidor", hostField);
        addField(panel, c, 1, "Porta", portField);
        addField(panel, c, 2, "Seguranca", securityField);
        addField(panel, c, 3, "EHLO", ehloField);
        addField(panel, c, 4, "Timeout ms", timeoutField);
        addFull(panel, c, 5, authCheck);
        addField(panel, c, 6, "Tipo auth", authModeField);
        addField(panel, c, 7, "Usuario", userField);
        addField(panel, c, 8, "Senha", passwordField);
        addField(panel, c, 9, "Token OAuth2", new JScrollPane(oauthTokenField));
        addFull(panel, c, 10, sendCheck);
        addField(panel, c, 11, "Remetente", fromField);
        addField(panel, c, 12, "Destinatario", toField);
        addField(panel, c, 13, "Assunto", subjectField);
        addField(panel, c, 14, "Corpo", new JScrollPane(bodyField));

        JPanel filler = new JPanel();
        c.gridy = 15;
        c.weighty = 1;
        panel.add(filler, c);
        return panel;
    }

    private JPanel buildResults() {
        JTable stepsTable = new JTable(stepsModel);
        stepsTable.setFillsViewportHeight(true);
        stepsTable.setRowHeight(26);
        stepsTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        stepsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        stepsTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        stepsTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        stepsTable.getColumnModel().getColumn(4).setPreferredWidth(520);

        protocolArea.setEditable(false);
        protocolArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        protocolArea.setLineWrap(false);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(stepsTable), new JScrollPane(protocolArea));
        split.setResizeWeight(0.48);
        split.setBorder(BorderFactory.createTitledBorder("Resultados em tempo real"));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private void addField(JPanel panel, GridBagConstraints c, int row, String label, java.awt.Component field) {
        c.gridy = row;
        c.gridx = 0;
        c.weightx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        panel.add(field, c);
    }

    private void addFull(JPanel panel, GridBagConstraints c, int row, java.awt.Component field) {
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 2;
        panel.add(field, c);
        c.gridwidth = 1;
    }

    private void bindActions() {
        clearButton.addActionListener(event -> clearResults());
        runButton.addActionListener(event -> startDiagnostic());
        authCheck.addActionListener(event -> updateEnabledFields());
        authModeField.addActionListener(event -> updateEnabledFields());
        sendCheck.addActionListener(event -> updateEnabledFields());
        securityField.setSelectedItem(SecurityMode.STARTTLS);
        updateEnabledFields();
    }

    private void updateEnabledFields() {
        boolean authenticate = authCheck.isSelected();
        boolean xoauth2 = authModeField.getSelectedItem() == AuthMode.XOAUTH2;
        authModeField.setEnabled(authenticate);
        userField.setEnabled(authenticate);
        passwordField.setEnabled(authenticate && !xoauth2);
        oauthTokenField.setEnabled(authenticate && xoauth2);
        fromField.setEnabled(sendCheck.isSelected() || authCheck.isSelected());
        toField.setEnabled(sendCheck.isSelected());
        subjectField.setEnabled(sendCheck.isSelected());
        bodyField.setEnabled(sendCheck.isSelected());
    }

    private void startDiagnostic() {
        SmtpTestRequest request;
        try {
            request = buildRequest();
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Configuracao invalida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        clearResults();
        runButton.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                new SmtpDiagnosticService().run(request, new SwingDiagnosticListener());
                return null;
            }

            @Override
            protected void done() {
                runButton.setEnabled(true);
                try {
                    get();
                    appendProtocol("APP", "Diagnostico concluido.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    appendProtocol("APP", "Diagnostico interrompido.");
                } catch (ExecutionException exception) {
                    appendProtocol("APP", "Diagnostico finalizado com erro: " + exception.getCause().getMessage());
                }
            }
        };
        worker.execute();
    }

    private SmtpTestRequest buildRequest() {
        return new SmtpTestRequest(
                hostField.getText().trim(),
                Integer.parseInt(portField.getText().trim()),
                (SecurityMode) securityField.getSelectedItem(),
                (AuthMode) authModeField.getSelectedItem(),
                ehloField.getText().trim().isBlank() ? "localhost" : ehloField.getText().trim(),
                userField.getText().trim(),
                passwordField.getPassword(),
                oauthTokenField.getText().replaceAll("\\s+", "").toCharArray(),
                fromField.getText().trim(),
                toField.getText().trim(),
                subjectField.getText().trim(),
                bodyField.getText(),
                authCheck.isSelected(),
                sendCheck.isSelected(),
                Duration.ofMillis(Long.parseLong(timeoutField.getText().trim()))
        );
    }

    private void clearResults() {
        stepsModel.setRowCount(0);
        protocolArea.setText("");
    }

    private void addStep(DiagnosticStep step) {
        stepsModel.addRow(new Object[]{
                TIME_FORMAT.format(step.timestamp()),
                step.name(),
                step.status(),
                step.durationMillis() == 0 ? "" : step.durationMillis() + " ms",
                step.detail()
        });
    }

    private void appendProtocol(String direction, String line) {
        protocolArea.append("[%s] %s%n".formatted(direction, line));
        protocolArea.setCaretPosition(protocolArea.getDocument().getLength());
    }

    private final class SwingDiagnosticListener implements DiagnosticListener {
        @Override
        public void onStep(DiagnosticStep step) {
            SwingUtilities.invokeLater(() -> addStep(step));
        }

        @Override
        public void onProtocol(String direction, String line) {
            SwingUtilities.invokeLater(() -> appendProtocol(direction, line));
        }
    }
}

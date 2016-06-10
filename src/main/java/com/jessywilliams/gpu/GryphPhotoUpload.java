package com.jessywilliams.gpu;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;

public class GryphPhotoUpload extends JFrame {

    /* GUI Vars */
    private JButton dirButton;
    private JButton loginButton;
    protected JButton uploadButton;
    private JLabel dirLabel;
    private JLabel passwordLabel;
    protected JLabel progressLabel;
    private JLabel usernameLabel;
    private JPanel mainPanel;
    private JPanel progressPanel;
    private JPasswordField passwordField;
    protected JProgressBar progressBar;
    private JTextField dirField;
    private JTextField usernameField;

    /* GPU Vars */
    private ActiveNet connection = new ActiveNet();
    private BatchUpload batchUpload = null;
    private boolean started = false;

    public GryphPhotoUpload(String title) {

         /* Set up main window */
        super(title);
        this.setContentPane(this.mainPanel);
        this.setMinimumSize(new Dimension(350, 250));
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (batchUpload != null) batchUpload.cancel(true);
                connection.kill();
                System.exit(0);
            }
        });

        /* Display GUI */
        this.pack();
        this.setVisible(true);
        loginButton.setEnabled(false);
        uploadButton.setEnabled(false);
        usernameField.setEnabled(false);
        passwordField.setEnabled(false);

        /* Create directory add button listener */
        dirButton.addActionListener(new UploadButtonListener(this));

        /* Create Login Button Listener */
        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                connection.authenticate(usernameField.getText(), passwordField.getText());
                if (connection.isAuthenticated()) {
                    loginButton.setEnabled(false);
                    usernameField.setEnabled(false);
                    passwordField.setEnabled(false);
                    uploadButton.setEnabled(true);

                    /* Create BatchUpload Listener to update progress bar */
                    batchUpload.addPropertyChangeListener(new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            if (evt.getPropertyName().equals("progress")) {
                                progressBar.setIndeterminate(false);
                                progressBar.setValue((Integer) evt.getNewValue());
                            }
                        }
                    });
                }
            }
        });

        /* Create start button listener */
        uploadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!started) {
                    batchUpload.execute();
                    batchUpload.resume();
                    started = true;
                    uploadButton.setText("Pause");
                } else if (batchUpload.isPaused()) {
                    batchUpload.resume();
                    uploadButton.setText("Pause");
                } else {
                    batchUpload.pause();
                    uploadButton.setText("Resume");
                }
            }
        });
    }

    public static void main(String[] args) {
        if (!extractResources()) {
            System.out.println("Could not extract required resources. Exiting.");
            return;
        }
        new GryphPhotoUpload("Gryph Photo Uploader");
    }

    protected static String execPath() throws UnsupportedEncodingException {
        String execPath;
        execPath = GryphPhotoUpload.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        execPath = URLDecoder.decode(execPath, "UTF-8");
        execPath = new File(execPath).getParentFile().getPath();
        return execPath;
    }

    protected static boolean extractResources() {

        String[] toCopy = {
                "done_button.png",
                "entry_field.png",
                "IEDriverServer.exe",
                "upload_button.png",
                "yes_button.png"
        };

        try {
            String execPath = execPath();
            File resource_dir = new File(execPath + "/resources");
            InputStream link;
            if (!resource_dir.exists()) {
                Files.createDirectory(resource_dir.getAbsoluteFile().toPath());
                System.out.println("Created resource directory: " + execPath + "/resources");
                for (String item : toCopy) {
                    link = (Thread.currentThread().getContextClassLoader().getResourceAsStream("files/" + item));
                    File dest = new File(execPath + "/resources/" + item);
                    Files.copy(link, dest.getAbsoluteFile().toPath());
                    System.out.println("Extracted file: " + dest.toString());
                }
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            return false;
        }
        return true;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(4, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(5, 5, 5, 5), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel1.setBorder(BorderFactory.createTitledBorder("Select Image Directory"));
        dirField = new JTextField();
        panel1.add(dirField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        dirButton = new JButton();
        dirButton.setText("Select");
        panel1.add(dirButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dirLabel = new JLabel();
        dirLabel.setText("Image Dir");
        panel1.add(dirLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 3, new Insets(5, 5, 5, 5), -1, -1));
        mainPanel.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel2.setBorder(BorderFactory.createTitledBorder("Login to ActiveNet"));
        usernameField = new JTextField();
        panel2.add(usernameField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        passwordField = new JPasswordField();
        panel2.add(passwordField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        usernameLabel = new JLabel();
        usernameLabel.setText("Username");
        panel2.add(usernameLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        passwordLabel = new JLabel();
        passwordLabel.setText("Password");
        panel2.add(passwordLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        loginButton = new JButton();
        loginButton.setText("Login");
        panel2.add(loginButton, new GridConstraints(0, 2, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        progressPanel = new JPanel();
        progressPanel.setLayout(new GridLayoutManager(2, 2, new Insets(5, 5, 5, 5), -1, -1));
        mainPanel.add(progressPanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        progressPanel.setBorder(BorderFactory.createTitledBorder(null, "Progress", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, new Color(-16777216)));
        progressBar = new JProgressBar();
        progressPanel.add(progressBar, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        progressLabel = new JLabel();
        progressLabel.setEnabled(false);
        progressLabel.setText("-- of --");
        progressPanel.add(progressLabel, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        uploadButton = new JButton();
        uploadButton.setEnabled(false);
        uploadButton.setText("Upload");
        mainPanel.add(uploadButton, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    private class UploadButtonListener implements ActionListener {
        GryphPhotoUpload gpu;

        public UploadButtonListener(GryphPhotoUpload gpu) {
            this.gpu = gpu;
        }

        public void actionPerformed(ActionEvent e) {
            JFileChooser dirChooser = new JFileChooser();
            try {
                dirChooser.setCurrentDirectory(new File(execPath()));
            } catch (UnsupportedEncodingException e1) {
                return;
            }
            dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            dirChooser.setAcceptAllFileFilterUsed(false);
            if (dirChooser.showOpenDialog(dirButton) == JFileChooser.APPROVE_OPTION) {

                /* Check if any suitable images were found */
                String imagePath = dirChooser.getSelectedFile().getPath();
                batchUpload = new BatchUpload(connection, gpu);
                batchUpload.createCustomerList(imagePath);

                /* If found.. */
                if (batchUpload.customerCount() > 0) {

                    /* Fill path into path field */
                    dirField.setText(imagePath);

                    /* Lock directory add pane, unlock login pane*/
                    dirButton.setEnabled(false);
                    dirField.setEnabled(false);
                    loginButton.setEnabled(true);
                    usernameField.setEnabled(true);
                    passwordField.setEnabled(true);

                    /* Set progress label */
                    progressLabel.setText("0 of " + batchUpload.customerCount());
                }

                /* Otherwise notify w/ a popup if not */
                else {
                    JOptionPane.showMessageDialog(null,
                            "No suitable image files found.",
                            "Selection error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

}
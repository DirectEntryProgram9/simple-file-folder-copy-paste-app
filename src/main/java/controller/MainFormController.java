package controller;

import com.jfoenix.controls.JFXButton;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import java.io.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Optional;

public class MainFormController {
    public AnchorPane pneContainer;
    public JFXButton btnSelectFile;
    public JFXButton btnSelectDirectory;
    public JFXButton btnBrowseDirectory;
    public Label lblSelectedFile;
    public Label lblBrowsedDirectory;
    public Button btnCopy;
    public Rectangle pgbContainer;
    public Rectangle pgbBar;
    public Label lblSize;
    public Label lblProgress;

    private List<File> srcFiles;
    private File srcDirectory;
    private File destDir;

    public void initialize() {
        btnCopy.setDisable(true);
    }
    public void btnSelectFile_OnAction(ActionEvent actionEvent) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.setTitle("Select files to copy");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files (*.*)", "*.*"));
        srcFiles = fileChooser.showOpenMultipleDialog(btnSelectFile.getScene().getWindow());
        if (srcFiles != null) {
            srcDirectory = null;
            double size = 0;
            if (srcFiles.size() > 1) {
                for (File file : srcFiles) {
                    size += file.length();
                }
                lblSelectedFile.setText("Multiple files have been selected, size: " + formatNumber(size/1024.0) + " Kb");
            }
            else if (srcFiles.size() == 1) {
                lblSelectedFile.setText("File name: " + srcFiles.get(0).getName() + ", size: " + formatNumber(srcFiles.get(0).length()/1024.0) + " Kb");
            }
        }
        else lblSelectedFile.setText("No file/directory selected");
        btnCopy.setDisable(srcFiles == null || destDir == null);
    }
    public void btnSelectDirectory_OnAction(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        directoryChooser.setTitle("Select a directory to copy");
        srcDirectory = directoryChooser.showDialog(btnBrowseDirectory.getScene().getWindow());
        if (srcDirectory != null) {
            srcFiles = null;
            double size = 0;
            File[] files = srcDirectory.listFiles();
            for (File file : files) {
                size += file.length();
            }
            lblSelectedFile.setText("File name: " + srcDirectory.getName() + ", inner file size: " + formatNumber(size/1024.0) + " Kb");
        }
        else lblSelectedFile.setText("No file/directory selected");
        btnCopy.setDisable(srcDirectory == null || destDir == null);
    }
    public void btnBrowseDirectory_OnAction(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select a destination directory");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        destDir = directoryChooser.showDialog(btnBrowseDirectory.getScene().getWindow());
        if (destDir != null) lblBrowsedDirectory.setText("Browse path: " + destDir.getAbsolutePath());
        else lblBrowsedDirectory.setText("No directory selected");

        if (srcFiles == null) btnCopy.setDisable(srcDirectory == null || destDir == null);
        if (srcDirectory == null) btnCopy.setDisable(srcFiles == null || destDir == null);
    }
    public void btnCopy_OnAction(ActionEvent actionEvent) throws IOException {
        if (srcFiles != null && srcDirectory == null) {
            for (File file : srcFiles) {
                File destFile = new File(destDir.getAbsolutePath(),file.getName());
                if (destFile.exists()) {
                    Optional<ButtonType> result = new Alert(Alert.AlertType.INFORMATION,
                            destFile.getName()+ " file is already exists. Do you want to overwrite?",
                            ButtonType.YES, ButtonType.NO).showAndWait();
                    if (result.get() == ButtonType.NO) {
                        continue;
                    }
                }
                readAndWrite(file,destFile);
            }
            new Alert(Alert.AlertType.INFORMATION,"Files has been copied successfully").showAndWait();
            srcFiles = null;
            destDir = null;
        }
        else if (srcFiles == null && srcDirectory != null) {
            File destDirectory = new File(destDir.getAbsolutePath(), srcDirectory.getName());
            if (!destDirectory.exists()) {
                destDirectory.mkdir();
            }
            else {
                Optional<ButtonType> result = new Alert(Alert.AlertType.INFORMATION,
                        "Directory already exists. Do you want to overwrite?",
                        ButtonType.YES, ButtonType.NO).showAndWait();
                if (result.get() == ButtonType.NO) {
                    return;
                }
            }
            File[] files = srcDirectory.listFiles();
            for (File file : files) {
                if (!file.isDirectory()) {
                    File destFile = new File(destDirectory.getAbsolutePath(),file.getName());
                    readAndWrite(file,destFile);
                }
            }
            new Alert(Alert.AlertType.INFORMATION, "All files in the directory have been successfully copied.").showAndWait();
            srcFiles = null;
            destDir = null;
        }
    }
    private void readAndWrite(File src, File dest) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                FileInputStream fis = new FileInputStream(src);
                BufferedInputStream bis = new BufferedInputStream(fis);
                FileOutputStream fos = new FileOutputStream(dest);
                BufferedOutputStream bos = new BufferedOutputStream(fos);

                long fileSize = src.length();
                int totalRead = 0;
                while (true) {
                    byte[] buffer = new byte[1024 * 10];
                    int read = bis.read(buffer);
                    totalRead += read;
                    if (read == -1) break;
                    bos.write(buffer,0,read);
                    updateProgress(totalRead,fileSize);
                }
                updateProgress(fileSize,fileSize);
                bis.close();
                bos.close();
                return null;
            }
        };
        task.workDoneProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number prevWork, Number curWork) {
                pgbBar.setWidth(pgbContainer.getWidth() / task.getTotalWork() * task.getWorkDone());
                lblSize.setText(formatNumber(task.getWorkDone() / 1024.0) + " / " + formatNumber(task.getTotalWork() / 1024.0) + " Kb");
                lblProgress.setText("Progress: " + formatNumber(task.getWorkDone() * 1.0 / task.getTotalWork() * 100) + " %");
            }
        });
        task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                pgbBar.setWidth(pgbContainer.getWidth());
                lblBrowsedDirectory.setText("No directory selected");
                lblSelectedFile.setText("No file/directory selected");
                btnCopy.setDisable(true);
            }
        });
        new Thread(task).start();
    }
    private String formatNumber(double input) {
        NumberFormat ni = NumberFormat.getNumberInstance();
        ni.setGroupingUsed(true);
        ni.setMinimumFractionDigits(2);
        ni.setMaximumFractionDigits(2);
        return ni.format(input);
    }
}
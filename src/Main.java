import Windows.AdministratorWindow;
import Windows.AuthentifiactionWindow;
import Windows.MainWindow;

import javax.swing.*;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main extends JPanel {
    public static void main(String[] args) {

        try {
            // Set system look and feel for the JOptionPane
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); // Set the pane to look like my system layout
        } catch (Exception e) {
            e.printStackTrace();
        }

          MainWindow mainWindow = new MainWindow();
//          AuthentifiactionWindow aw = new AuthentifiactionWindow();
//        AdministratorWindow adminWindow = new AdministratorWindow();
    }
}
import Models.HTMLobject;
import org.apache.pdfbox.pdmodel.PDDocument;

import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.filechooser.*;

import static java.awt.Color.*;

/** Authors: Jui, Aditya **/
public class mainClass extends JFrame implements ActionListener {
    private static final long serialVersionUID = 1L;
    private String pdfPath = "";
    private String xmlPath = "";
    private JTextField pdfFilePath;
    private JTextField xmlFilePath;
    private JButton choosePdf = new JButton("CHOOSE PDF");
    private JButton chooseXml = new JButton("CHOOSE XML Path");
    private JButton convert = new JButton("CONVERT");

    //gui creation -
    //3 buttons- 2 for choosing filepaths and 1 for actual conversion
    //2 textfields for showing the paths chosen
    public mainClass() throws IOException {
        //Background colour -gray
        this.getContentPane().setBackground(lightGray);
        this.getContentPane().setLayout(new FlowLayout());

        Box box = Box.createVerticalBox();

        //created Label for project name & changed its some default properties
        JLabel label = new JLabel();
        label.setFont(new Font("Verdana", Font.BOLD, 18));
        label.setBounds(50, 100, 250, 20);
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setText("PDF TO XML CONVERTER");
        label.setForeground(blue);
        box.add(label);

        //Created table for Team name & changed the required default properties
        JLabel label1 = new JLabel();
        label1.setFont(new Font("Verdana", Font.BOLD, 14));
        label1.setBounds(50, 100, 250, 20);
        label1.setText("Team VJTI");
        label1.setHorizontalAlignment(JLabel.CENTER);
        label1.setForeground(black);
        box.add(label1);

        //for displaying selected path
        pdfFilePath = new JTextField("");
        pdfFilePath.setForeground(gray);
        pdfFilePath.setBounds(50, 100, 200, 30);
        box.add(pdfFilePath);

        //Button created for choosing the path
        choosePdf.setText("CHOOSE PDF");
        choosePdf.addActionListener(this);
        choosePdf.setBackground(BLACK);
        choosePdf.setForeground(white);
        choosePdf.setBounds(50, 100, 200, 30);
        choosePdf.setFont(new Font("Verdana", Font.BOLD, 14));
        choosePdf.setActionCommand("choosepdf");
        box.add(choosePdf);

        // for displaying xml path
        xmlFilePath = new JTextField("");
        xmlFilePath.setForeground(gray);
        xmlFilePath.setBounds(50, 100, 200, 30);
        box.add(xmlFilePath);

        //for choosing xml filepath
        chooseXml.setText("CHOOSE XML");
        chooseXml.addActionListener(this);
        chooseXml.setBackground(BLACK);
        chooseXml.setForeground(white);
        chooseXml.setFont(new Font("Verdana", Font.BOLD, 14));
        chooseXml.setBounds(50, 100, 200, 30);
        chooseXml.setActionCommand("choosexml");
        box.add(chooseXml);


        //Conversion task
        convert.setBackground(RED);
        convert.setForeground(white);
        convert.setFont(new Font("Verdana", Font.BOLD, 14));
        convert.setBounds(50, 100, 200, 30);
        box.add(convert);
        convert.addActionListener(this);
        convert.setActionCommand("convert");

        //by box keeping it vertically aligned
        add(box);


    }


    @Override
    public void actionPerformed(ActionEvent ae) {
        String action = ae.getActionCommand();
        if (action.equals("convert")) {
            try {

                PDDocument pdf = PDDocument.load(new File(pdfPath));

                List<String[][]> tables = PDFTableStripper.getDetails(pdf).getTables();
                String XMLTable = Table2XML.convertToXML(tables);

                //generates HTMLString
                String htmlString = HTMLformatter.generateHTMLFromPDF(pdf);

                //generates List of HTML objects.
                List<HTMLobject> htmlObjectList = HTMLformatter.parseHTML(htmlString);

                //concats characters of same line
                List<HTMLobject> formattedHTMLList = HTMLformatter.formatHTMLList(htmlObjectList);
                htmlObjectList.clear();

                //removes tables from text
                List<HTMLobject> textList = removeTable(formattedHTMLList);
                formattedHTMLList.clear();

                //generates XML from unstructured data
                Text2XML.XMLGenerationCombined(textList, xmlPath, XMLTable);

                //changes color & text of button when done
                convert.setBounds(50, 100, 200, 30);
                convert.setText("Done!!");
                convert.setFont(new Font("Verdana", Font.BOLD, 14));
                convert.setBackground(green);


            } catch (ParserConfigurationException | IOException e) {
                e.printStackTrace();
            }


        }
        if (action.equals("choosepdf")) {
            JFileChooser jFileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());

            // allow multiple file selection
            jFileChooser.setMultiSelectionEnabled(true);

            // invoke the showsOpenDialog function to show the path
            int openStatus = jFileChooser.showOpenDialog(null);

            if (openStatus == JFileChooser.APPROVE_OPTION) {
                // get the selected files
                pdfPath = jFileChooser.getSelectedFile().getAbsolutePath();
                pdfPath.replace("\\", "/");
                pdfFilePath.setText(pdfPath);

            }
        }
        if (action.equals("choosexml")) {
            JFileChooser jFilechooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());

            // invoke the showsOpenDialog function to show the path
            int openStatus = jFilechooser.showOpenDialog(null);

            if (openStatus == JFileChooser.APPROVE_OPTION) {
                // get the selected filepath
                xmlPath = jFilechooser.getSelectedFile().getAbsolutePath();
                xmlPath.replace("\\", "/");
                xmlFilePath.setText(xmlPath);

            }
        }
    }

    private static void createAndShowGUI() throws IOException {

        //creation of swing frame
        JFrame frame = new mainClass();
        frame.setSize(400, 400);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }

    public static void main(String[] args) {

        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            public void run() {

                try {
                    createAndShowGUI();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        });

    }

    //Removes table from data to be processed
    public static List<HTMLobject> removeTable(List<HTMLobject> formattedHTMLList) {
        List<HTMLobject> textList = new ArrayList<>();
        boolean flag = false;

        for (HTMLobject htmLobject : formattedHTMLList) {

            if (htmLobject.getValue().equals("Sl."))
                flag = true;
            else if (htmLobject.getValue().equals("TOTAL:")) {
                flag = false;
            }

            if (!flag) {
                textList.add(htmLobject);
            }
        }
        return textList;
    }


}
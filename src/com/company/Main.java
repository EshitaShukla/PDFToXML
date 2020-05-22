package com.company;


import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;

public class Main {

    public static void main(String args[]) {

        PDFParser parser = null;
        PDDocument pdDoc = null;
        COSDocument cosDoc = null;
        PDFTextStripper pdfStripper;

        String parsedText;
        String fileName = "/home/theperson/IdeaProjects/proj1/src/com/company/Invoice.pdf";
        File file = new File(fileName);
        try {
            parser = new PDFParser(new org.apache.pdfbox.io.RandomAccessFile(file, "r"));
            parser.parse();
            cosDoc = parser.getDocument();
            pdfStripper = new PDFTextStripper();
            pdDoc = new PDDocument(cosDoc);
            parsedText = pdfStripper.getText(pdDoc);
            System.out.println(parsedText);

            String[] array = parsedText.split("\n", -1);
//            int[] array = {1, 2, 3, 4, 5};
            for (String element: array) {
                System.out.println(element);

            }

//            int bname_i = parsedText.indexOf("Business Number")+15+1;
//            int inv_i = parsedText.indexOf("INVOICE")+7+1;
////            int[] arrayStartOfKey =
//            System.out.println(parsedText.charAt(bname_i));
//            System.out.println(parsedText.charAt(inv_i));
//            System.out.println(bname_i);


        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (cosDoc != null)
                    cosDoc.close();
                if (pdDoc != null)
                    pdDoc.close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }

        }
    }
}

import Models.HTMLobject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**Author-Jui**/

class Text2XML {
    //variables
    private static List<String> Headings = new ArrayList<>();
    private static List<String> textData = new ArrayList<>();

    // joining the relevant fields together which are separated on different lines
    public static List<HTMLobject> joinHTMLObjectList(List<HTMLobject> textList) {

        List<HTMLobject> shrinkedList = new ArrayList<>();
        shrinkedList.add(textList.get(0));
        for (int index = 1; index < textList.size(); index++) {
            /*checking some font parameters & distance between two lines
               If the font is equal and they are not distant then will get appended
               Else if font is equal but they are distant then will not get appended
               and if fonts are different will be considered as different valued components*/
            boolean isFontequal = checkFont(textList.get(index - 1), textList.get(index));
            if (isFontequal) {
                boolean appendable = checkDistanceBetween(textList.get(index - 1), textList.get(index));
                if (appendable) {
                    HTMLobject object = shrinkedList.get(shrinkedList.size() - 1);
                    object.setValue(object.getValue() + " " + textList.get(index).getValue());
                } else {
                    shrinkedList.add(textList.get(index));
                }
            } else {
                shrinkedList.add(textList.get(index));
            }
        }
        return shrinkedList;
    }

    //Generates key value pairs required for xml file generation
    public static void getKeyValuePairs(List<HTMLobject> shrinkedList) {
        //Initialization of array for keeping track of visited elements
        List<Integer> ProcessedElement = new ArrayList<>();
        for (int index = 0; index < shrinkedList.size(); index++) {
            ProcessedElement.add(-1);
        }
        /*
        Logic behind key value pair generation:
         If the element is not visited then process it as follows:
         1. If current element and next element are having same y coordinates & current element is bold then it will result into key value pair.
         2. Else if line spacing between current and next element is between 15 & 14.34
                 if font is same then -----key value
                 else key will be text-entry
         3. Else if line spacing is greater than 15 then key will be text-entry
         4.If line spacing is less than 14.34 then ------key value
         */
        for (int index= 0; index < shrinkedList.size() - 1; index++) {
            if (ProcessedElement.get(index) == -1) {
                if (shrinkedList.get(index).getTop() == shrinkedList.get(index + 1).getTop()) {
                    if (shrinkedList.get(index).getFont_weight().equals("bold")) {
                        Headings.add(shrinkedList.get(index).getValue());
                        textData.add(shrinkedList.get(index + 1).getValue());
                        ProcessedElement.set(index, 1);
                        ProcessedElement.set(index + 1, 1);
                    }
                } else if (Math.abs(shrinkedList.get(index).getTop() - shrinkedList.get(index + 1).getTop()) > 14.34
                        && Math.abs(shrinkedList.get(index).getTop() - shrinkedList.get(index + 1).getTop()) < 15.00) {
                    if (shrinkedList.get(index).getFont_weight().equals(shrinkedList.get(index + 1).getFont_weight())) {
                        Headings.add(shrinkedList.get(index).getValue());
                        textData.add(shrinkedList.get(index + 1).getValue());
                        ProcessedElement.set(index, 1);
                        ProcessedElement.set(index + 1, 1);
                    } else {
                        Headings.add("text-entry");
                        textData.add(shrinkedList.get(index).getValue());
                        ProcessedElement.set(index, 1);
                    }
                } else if (Math.abs(shrinkedList.get(index).getTop() - shrinkedList.get(index+ 1).getTop()) > 15.00) {
                    Headings.add("text-entry");
                    textData.add(shrinkedList.get(index).getValue());
                    ProcessedElement.set(index, 1);
                } else if (Math.abs(shrinkedList.get(index).getTop() - shrinkedList.get(index + 1).getTop()) < 14.34) {
                    if (shrinkedList.get(index).getFont_weight().equals("bold")) {
                        Headings.add(shrinkedList.get(index).getValue());
                        textData.add(shrinkedList.get(index + 1).getValue());
                        ProcessedElement.set(index, 1);
                        ProcessedElement.set(index + 1, 1);
                    }
                }
            }
        }
    }

    //Generates XML file & stores it at xmlFilePath location
    public static void XMLGenerator(String xmlFilePath,String tableString) {
        //Enables  to obtain DOM parser
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        //For obtaining document from XML
        DocumentBuilder documentBuilder = null;
        try {
            documentBuilder = documentFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        Document document = documentBuilder.newDocument();
        //root elemnt creation
        Element root = document.createElement("non-tabular-data");
        document.appendChild(root);
        //Creates temp node with required values and attributes and appends them as a child to root
        for (int index = 0; index < Headings.size(); index++) {
            Headings.set(index, Headings.get(index).replace(":", "").trim());

                Element toBeProcessed = document.createElement("text");
                //for attribute creation
                if (!Headings.get(index).equals("text-entry")) {
                    Attr attr = document.createAttribute("key");
                    attr.setValue(Headings.get(index));
                    toBeProcessed.setAttributeNode(attr);
                }
                //gives entry value
                toBeProcessed.appendChild(document.createTextNode(textData.get(index)));
                root.appendChild(toBeProcessed);

        }
        //transform DOM Document to string
        String doc= convertDocumentToString(document).replaceFirst(">","><document>")+tableString+"</document>";

        Document mergedDocument=convertStringToDocument(doc);
        //Transform DOM document to XML
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
        DOMSource domSource = new DOMSource(mergedDocument);
        StreamResult streamResult = new StreamResult(new File(xmlFilePath));
        try {
            transformer.transform(domSource, streamResult);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        //check statement
        System.out.println("Done creating XML File");
    }

    //main function which will call all the required functions & will be called in main
    public static void XMLGenerationCombined(List<HTMLobject> textList, String XMLPath,String tableString) {
        List<HTMLobject> shrinkedList = Text2XML.joinHTMLObjectList(textList);
        Text2XML.getKeyValuePairs(shrinkedList);
        XMLGenerator(XMLPath,tableString);
    }

    //This method checks the color, fontfamily, fontweight of Elements
    public static boolean checkFont(HTMLobject element1, HTMLobject element2) {
        if (element1.getColor().equals(element2.getColor()) & element1.getFont_family().equals(element2.getFont_family())
                & element1.getFont_weight() == element2.getFont_weight()) {
            return true;
        }
        return false;
    }

    //This method checks the distance between two elements: if less distance returns false else true
    public static boolean checkDistanceBetween(HTMLobject element1, HTMLobject element2) {
        if (Math.abs(element1.getTop() - element2.getTop()) > 14.34 ||
                (Math.abs(element1.getTop() - element2.getTop()) < 7 && (Math.abs(element1.getTop() - element2.getTop()) > 3))) {
            return false;
        }
        return true;
    }

    //This method converts DOM Document to string
    private static String convertDocumentToString(Document doc) {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = tf.newTransformer();
            // below code to remove XML declaration
            // transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            String output = writer.getBuffer().toString();
            return output;
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        return null;
    }


    //This method converts string to DOM document
    private static Document convertStringToDocument(String xmlStr) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try
        {
            builder = factory.newDocumentBuilder();
            Document doc = builder.parse( new InputSource( new StringReader( xmlStr ) ) );
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
